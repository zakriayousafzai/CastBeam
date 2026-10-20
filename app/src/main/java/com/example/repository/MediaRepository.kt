package com.example.repository

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.example.model.MediaCategory
import com.example.model.MediaItem
import com.example.model.SharedFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MediaRepository(private val context: Context) {
    private val tag = "MediaRepository"
    private val prefs: SharedPreferences = context.getSharedPreferences("castbeam_prefs", Context.MODE_PRIVATE)

    private val cachedItems = mutableListOf<MediaItem>()
    private val foldersList = mutableListOf<SharedFolder>()

    init {
        loadPersistedFolders()
        ensureDemoSampleFiles()
    }

    private fun loadPersistedFolders() {
        foldersList.clear()
        val json = prefs.getString("shared_folders", null)
        if (!json.isNullOrEmpty()) {
            try {
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    foldersList.add(
                        SharedFolder(
                            uriString = obj.getString("uri"),
                            name = obj.getString("name"),
                            itemCount = obj.optInt("count", 0),
                            addedTimestamp = obj.optLong("time", System.currentTimeMillis())
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to load folders: ${e.message}")
            }
        }
    }

    private fun savePersistedFolders() {
        val array = JSONArray()
        foldersList.forEach { f ->
            val obj = JSONObject().apply {
                put("uri", f.uriString)
                put("name", f.name)
                put("count", f.itemCount)
                put("time", f.addedTimestamp)
            }
            array.put(obj)
        }
        prefs.edit().putString("shared_folders", array.toString()).apply()
    }

    fun getSharedFolders(): List<SharedFolder> = foldersList.toList()

    suspend fun addFolder(treeUri: Uri): SharedFolder = withContext(Dispatchers.IO) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            Log.w(tag, "Could not take persistable permission: ${e.message}")
        }

        var displayName = "Shared Folder"
        try {
            val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
            context.contentResolver.query(
                rootDocUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    displayName = cursor.getString(0) ?: displayName
                }
            }
        } catch (_: Exception) {}

        // Remove if exists
        foldersList.removeAll { it.uriString == treeUri.toString() }

        val newFolder = SharedFolder(
            uriString = treeUri.toString(),
            name = displayName,
            itemCount = 0
        )
        foldersList.add(0, newFolder)
        savePersistedFolders()

        // Scan immediately
        scanAllFolders()

        // Return updated folder with actual count
        foldersList.find { it.uriString == treeUri.toString() } ?: newFolder
    }

    fun removeFolder(uriString: String) {
        foldersList.removeAll { it.uriString == uriString }
        savePersistedFolders()
        cachedItems.removeAll { it.folderName == uriString }
    }

    suspend fun scanAllFolders(): List<MediaItem> = withContext(Dispatchers.IO) {
        val allItems = mutableListOf<MediaItem>()

        // 1. Scan user shared folders via DocumentsContract
        val updatedFolders = mutableListOf<SharedFolder>()
        for (folder in foldersList) {
            try {
                val treeUri = Uri.parse(folder.uriString)
                val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId)

                val projection = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED
                )

                val folderMedia = mutableListOf<MediaItem>()
                val subtitleMap = mutableMapOf<String, String>()
                val fileList = mutableListOf<RawDocFile>()

                context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                    val modIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                    while (cursor.moveToNext()) {
                        val docId = cursor.getString(idIdx)
                        val name = cursor.getString(nameIdx) ?: continue
                        val mime = cursor.getString(mimeIdx) ?: "application/octet-stream"
                        val size = if (!cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else 0L
                        val lastModified = if (!cursor.isNull(modIdx)) cursor.getLong(modIdx) else System.currentTimeMillis()
                        val itemUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId).toString()

                        fileList.add(RawDocFile(docId, name, mime, size, lastModified, itemUri))
                    }
                }

                // First pass: subtitles
                for (file in fileList) {
                    val lower = file.name.lowercase()
                    if (lower.endsWith(".srt") || lower.endsWith(".vtt")) {
                        val baseName = file.name.substringBeforeLast(".")
                        subtitleMap[baseName] = file.uriString
                    }
                }

                // Second pass: media files
                for (file in fileList) {
                    val lower = file.name.lowercase()
                    if (lower.endsWith(".srt") || lower.endsWith(".vtt")) continue
                    if (file.mime == DocumentsContract.Document.MIME_TYPE_DIR) continue

                    val mime = if (file.mime == "application/octet-stream") getMimeTypeFromExtension(lower) else file.mime
                    val category = determineCategory(mime, lower)
                    val baseName = file.name.substringBeforeLast(".")
                    val matchingSubtitle = subtitleMap[baseName]

                    val item = MediaItem(
                        id = UUID.nameUUIDFromBytes("${folder.uriString}/${file.name}".toByteArray()).toString(),
                        name = file.name,
                        uriString = file.uriString,
                        mimeType = mime,
                        size = file.size,
                        category = category,
                        folderName = folder.name,
                        subtitleUri = matchingSubtitle,
                        lastModified = file.lastModified
                    )
                    folderMedia.add(item)
                }

                allItems.addAll(folderMedia)
                updatedFolders.add(folder.copy(itemCount = folderMedia.size))
            } catch (e: Exception) {
                Log.e(tag, "Error scanning folder ${folder.name}: ${e.message}")
                updatedFolders.add(folder)
            }
        }

        foldersList.clear()
        foldersList.addAll(updatedFolders)
        savePersistedFolders()

        // 2. Scan internal app cache / Uploads folder
        val uploadsDir = File(context.cacheDir, "CastBeam_Uploads")
        if (uploadsDir.exists() && uploadsDir.isDirectory) {
            uploadsDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val lower = file.name.lowercase()
                    val mime = getMimeTypeFromExtension(lower)
                    val item = MediaItem(
                        id = UUID.nameUUIDFromBytes(file.absolutePath.toByteArray()).toString(),
                        name = file.name,
                        uriString = Uri.fromFile(file).toString(),
                        mimeType = mime,
                        size = file.length(),
                        category = determineCategory(mime, lower),
                        folderName = "Web Uploads",
                        lastModified = file.lastModified()
                    )
                    allItems.add(item)
                }
            }
        }

        // 3. Include Demo Media Samples if library is empty
        val demoDir = File(context.cacheDir, "CastBeam_Demos")
        if (demoDir.exists() && demoDir.isDirectory) {
            demoDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val lower = file.name.lowercase()
                    val mime = getMimeTypeFromExtension(lower)
                    val item = MediaItem(
                        id = UUID.nameUUIDFromBytes(file.absolutePath.toByteArray()).toString(),
                        name = file.name,
                        uriString = Uri.fromFile(file).toString(),
                        mimeType = mime,
                        size = file.length(),
                        category = determineCategory(mime, lower),
                        folderName = "Demo Samples",
                        durationMs = if (lower.contains("video")) 45000L else 30000L,
                        lastModified = file.lastModified()
                    )
                    allItems.add(item)
                }
            }
        }

        cachedItems.clear()
        cachedItems.addAll(allItems)
        allItems
    }

    fun getCachedItems(): List<MediaItem> = cachedItems.toList()

    private fun determineCategory(mime: String, filename: String): MediaCategory {
        return when {
            mime.startsWith("video/") || filename.endsWith(".mp4") || filename.endsWith(".mkv") ||
                    filename.endsWith(".webm") || filename.endsWith(".avi") || filename.endsWith(".mov") ||
                    filename.endsWith(".m4v") || filename.endsWith(".ts") -> MediaCategory.VIDEO

            mime.startsWith("audio/") || filename.endsWith(".mp3") || filename.endsWith(".m4a") ||
                    filename.endsWith(".flac") || filename.endsWith(".wav") || filename.endsWith(".aac") ||
                    filename.endsWith(".ogg") -> MediaCategory.AUDIO

            mime.startsWith("image/") || filename.endsWith(".jpg") || filename.endsWith(".jpeg") ||
                    filename.endsWith(".png") || filename.endsWith(".webp") || filename.endsWith(".gif") -> MediaCategory.IMAGE

            mime.contains("pdf") || mime.contains("text") || filename.endsWith(".pdf") ||
                    filename.endsWith(".txt") || filename.endsWith(".doc") || filename.endsWith(".docx") -> MediaCategory.DOCUMENT

            else -> MediaCategory.OTHER
        }
    }

    private fun getMimeTypeFromExtension(lowerName: String): String {
        return when {
            lowerName.endsWith(".mp4") -> "video/mp4"
            lowerName.endsWith(".mkv") -> "video/x-matroska"
            lowerName.endsWith(".webm") -> "video/webm"
            lowerName.endsWith(".avi") -> "video/x-msvideo"
            lowerName.endsWith(".mov") -> "video/quicktime"
            lowerName.endsWith(".mp3") -> "audio/mpeg"
            lowerName.endsWith(".m4a") -> "audio/mp4"
            lowerName.endsWith(".flac") -> "audio/flac"
            lowerName.endsWith(".wav") -> "audio/wav"
            lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") -> "image/jpeg"
            lowerName.endsWith(".png") -> "image/png"
            lowerName.endsWith(".webp") -> "image/webp"
            lowerName.endsWith(".pdf") -> "application/pdf"
            lowerName.endsWith(".txt") -> "text/plain"
            else -> "application/octet-stream"
        }
    }

    private fun ensureDemoSampleFiles() {
        try {
            val demoDir = File(context.cacheDir, "CastBeam_Demos")
            if (!demoDir.exists()) demoDir.mkdirs()

            val demoVideo = File(demoDir, "CastBeam_Intro_Cinema_1080p.mp4")
            if (!demoVideo.exists()) {
                FileOutputStream(demoVideo).use { fos ->
                    val dummyMp4Bytes = byteArrayOf(
                        0x00, 0x00, 0x00, 0x18, 'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte(),
                        'i'.code.toByte(), 's'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(),
                        0x00, 0x00, 0x02, 0x00, 'i'.code.toByte(), 's'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(),
                        'i'.code.toByte(), 's'.code.toByte(), 'o'.code.toByte(), '2'.code.toByte()
                    )
                    fos.write(dummyMp4Bytes)
                    fos.write(ByteArray(64 * 1024))
                }
            }

            val demoAudio = File(demoDir, "Synthwave_Local_Lounge.mp3")
            if (!demoAudio.exists()) {
                FileOutputStream(demoAudio).use { fos ->
                    val id3Header = byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x20)
                    fos.write(id3Header)
                    fos.write(ByteArray(32 * 1024))
                }
            }

            val demoSub = File(demoDir, "CastBeam_Intro_Cinema_1080p.srt")
            if (!demoSub.exists()) {
                demoSub.writeText(
                    "1\n00:00:01,000 --> 00:00:06,000\nWelcome to CastBeam TV Streaming!\n\n2\n00:00:07,000 --> 00:00:14,000\nStream local movies straight from your phone.\n"
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error creating demo files: ${e.message}")
        }
    }

    private data class RawDocFile(
        val docId: String,
        val name: String,
        val mime: String,
        val size: Long,
        val lastModified: Long,
        val uriString: String
    )
}
