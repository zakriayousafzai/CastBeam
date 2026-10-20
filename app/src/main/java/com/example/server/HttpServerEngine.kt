package com.example.server

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.model.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

class HttpServerEngine(
    private val context: Context,
    private val getMediaList: () -> List<MediaItem>,
    private val getFoldersList: () -> List<SharedFolder>,
    private val onLogEntry: (ServerLogEntry) -> Unit,
    private val onClientConnected: (ServerClientSession) -> Unit
) {
    private val tag = "HttpServerEngine"

    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private var serverJob: Job? = null
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val bytesSent = AtomicLong(0)
    val bytesReceived = AtomicLong(0)

    // Remote playback synchronization
    private val pendingCommands = CopyOnWriteArrayList<RemoteCommand>()
    private var currentRemoteState = RemotePlaybackState()
    private val activeClients = ConcurrentHashMap<String, ServerClientSession>()

    fun start(port: Int = 8080): Boolean {
        if (isRunning) return true
        return try {
            val socket = ServerSocket()
            socket.reuseAddress = true
            socket.bind(InetSocketAddress(port))
            serverSocket = socket
            isRunning = true

            serverJob = serverScope.launch {
                while (isActive && isRunning) {
                    try {
                        val client = socket.accept()
                        launch(Dispatchers.IO) {
                            handleConnection(client)
                        }
                    } catch (e: Exception) {
                        if (!isRunning) break
                        Log.e(tag, "Accept error: ${e.message}")
                    }
                }
            }
            Log.i(tag, "CastBeam HTTP Server started successfully on port $port")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to start server: ${e.message}")
            isRunning = false
            false
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(tag, "Error closing server socket: ${e.message}")
        }
        serverJob?.cancel()
        serverSocket = null
    }

    fun isServerRunning(): Boolean = isRunning

    fun getPort(): Int = serverSocket?.localPort ?: 8080

    fun sendRemoteCommand(action: String, value: String? = null) {
        val command = RemoteCommand(action = action, value = value)
        pendingCommands.add(command)
        // Clean old commands if queue is bloated
        while (pendingCommands.size > 20) {
            pendingCommands.removeAt(0)
        }
    }

    fun getRemotePlaybackState(): RemotePlaybackState = currentRemoteState

    private fun handleConnection(socket: Socket) {
        val clientIp = socket.inetAddress?.hostAddress ?: "Unknown"
        var requestPath = "/"
        var method = "GET"
        var statusCode = 200

        try {
            socket.soTimeout = 30000
            val input = BufferedInputStream(socket.getInputStream())
            val output = BufferedOutputStream(socket.getOutputStream())

            val reader = BufferedReader(InputStreamReader(input))
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            method = parts[0].uppercase()
            val rawUri = parts[1]
            val uri = URI(rawUri)
            requestPath = uri.path ?: "/"
            val queryParams = parseQueryParams(uri.rawQuery)

            // Read request headers
            val headers = mutableMapOf<String, String>()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrEmpty()) break
                val colonIdx = line!!.indexOf(':')
                if (colonIdx > 0) {
                    val key = line!!.substring(0, colonIdx).trim().lowercase()
                    val value = line!!.substring(colonIdx + 1).trim()
                    headers[key] = value
                }
            }

            // Track client session
            val userAgent = headers["user-agent"] ?: "Unknown"
            val isTv = userAgent.contains("TV", ignoreCase = true) ||
                    userAgent.contains("webOS", ignoreCase = true) ||
                    userAgent.contains("Tizen", ignoreCase = true) ||
                    userAgent.contains("SMART-TV", ignoreCase = true) ||
                    userAgent.contains("CrKey", ignoreCase = true) ||
                    userAgent.contains("AppleTV", ignoreCase = true)

            val session = ServerClientSession(
                id = clientIp,
                ipAddress = clientIp,
                userAgent = userAgent,
                lastActive = System.currentTimeMillis(),
                isTv = isTv
            )
            activeClients[clientIp] = session
            onClientConnected(session)

            // Route dispatch
            when {
                requestPath == "/" || requestPath == "/index.html" -> {
                    serveHtml(output)
                    statusCode = 200
                }
                requestPath == "/api/media" -> {
                    serveMediaListJson(output)
                    statusCode = 200
                }
                requestPath == "/api/folders" -> {
                    serveFoldersListJson(output)
                    statusCode = 200
                }
                requestPath == "/api/status" -> {
                    serveStatusJson(output)
                    statusCode = 200
                }
                requestPath == "/stream" -> {
                    val mediaId = queryParams["id"]
                    statusCode = serveMediaStream(mediaId, headers, output, asDownload = false)
                }
                requestPath == "/download" -> {
                    val mediaId = queryParams["id"]
                    statusCode = serveMediaStream(mediaId, headers, output, asDownload = true)
                }
                requestPath == "/subtitles" -> {
                    val mediaId = queryParams["id"]
                    serveSubtitles(mediaId, output)
                    statusCode = 200
                }
                requestPath == "/api/remote/state" -> {
                    serveRemoteState(output)
                    statusCode = 200
                }
                requestPath == "/api/remote/status" && method == "POST" -> {
                    handleRemoteStatusPost(reader, headers)
                    sendJsonResponse(output, 200, "{\"success\":true}")
                    statusCode = 200
                }
                requestPath == "/api/remote/command" && method == "POST" -> {
                    handleRemoteCommandPost(reader, headers)
                    sendJsonResponse(output, 200, "{\"success\":true}")
                    statusCode = 200
                }
                requestPath == "/api/upload" && method == "POST" -> {
                    handleFileUpload(input, headers, output)
                    statusCode = 200
                }
                else -> {
                    sendNotFound(output)
                    statusCode = 404
                }
            }

            output.flush()
        } catch (e: Exception) {
            // Socket errors (e.g. client disconnect while seeking video) are normal for streaming
            statusCode = 499
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {}
            onLogEntry(
                ServerLogEntry(
                    method = method,
                    path = requestPath,
                    status = statusCode,
                    clientIp = clientIp
                )
            )
        }
    }

    private fun serveHtml(output: OutputStream) {
        val html = WebDashboardHtml.generateHtml()
        val bytes = html.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        bytesSent.addAndGet(bytes.size.toLong())
    }

    private fun serveMediaListJson(output: OutputStream) {
        val list = getMediaList()
        val jsonArray = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("mimeType", item.mimeType)
                put("size", item.size)
                put("formattedSize", item.formattedSize)
                put("category", item.category.name)
                put("folderName", item.folderName)
                put("durationMs", item.durationMs)
                put("formattedDuration", item.formattedDuration)
                put("hasSubtitle", item.subtitleUri != null)
            }
            jsonArray.put(obj)
        }
        sendJsonResponse(output, 200, jsonArray.toString())
    }

    private fun serveFoldersListJson(output: OutputStream) {
        val list = getFoldersList()
        val jsonArray = JSONArray()
        list.forEach { folder ->
            val obj = JSONObject().apply {
                put("uri", folder.uriString)
                put("name", folder.name)
                put("count", folder.itemCount)
            }
            jsonArray.put(obj)
        }
        sendJsonResponse(output, 200, jsonArray.toString())
    }

    private fun serveStatusJson(output: OutputStream) {
        val obj = JSONObject().apply {
            put("running", isRunning)
            put("port", getPort())
            put("activeClients", activeClients.size)
            put("bytesSent", bytesSent.get())
            put("bytesReceived", bytesReceived.get())
            put("currentTitle", currentRemoteState.title)
            put("isPlaying", currentRemoteState.isPlaying)
        }
        sendJsonResponse(output, 200, obj.toString())
    }

    private fun serveRemoteState(output: OutputStream) {
        val commandsArray = JSONArray()
        while (pendingCommands.isNotEmpty()) {
            val cmd = pendingCommands.removeAt(0)
            val obj = JSONObject().apply {
                put("id", cmd.id)
                put("action", cmd.action)
                put("value", cmd.value)
                put("timestamp", cmd.timestamp)
            }
            commandsArray.put(obj)
        }

        val res = JSONObject().apply {
            put("commands", commandsArray)
            put("state", JSONObject().apply {
                put("title", currentRemoteState.title)
                put("currentTime", currentRemoteState.currentTime)
                put("duration", currentRemoteState.duration)
                put("isPlaying", currentRemoteState.isPlaying)
                put("volume", currentRemoteState.volume)
                put("isMuted", currentRemoteState.isMuted)
            })
        }
        sendJsonResponse(output, 200, res.toString())
    }

    private fun handleRemoteStatusPost(reader: BufferedReader, headers: Map<String, String>) {
        val length = headers["content-length"]?.toIntOrNull() ?: 0
        if (length > 0) {
            val charBuffer = CharArray(length)
            reader.read(charBuffer, 0, length)
            val body = String(charBuffer)
            try {
                val json = JSONObject(body)
                currentRemoteState = currentRemoteState.copy(
                    mediaId = if (json.has("mediaId") && !json.isNull("mediaId")) json.getString("mediaId") else null,
                    title = json.optString("title", "CastBeam Media"),
                    currentTime = json.optDouble("currentTime", 0.0),
                    duration = json.optDouble("duration", 0.0),
                    isPlaying = json.optBoolean("isPlaying", false),
                    volume = json.optDouble("volume", 1.0),
                    isMuted = json.optBoolean("isMuted", false),
                    lastUpdated = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e(tag, "Error parsing remote status: ${e.message}")
            }
        }
    }

    private fun handleRemoteCommandPost(reader: BufferedReader, headers: Map<String, String>) {
        val length = headers["content-length"]?.toIntOrNull() ?: 0
        if (length > 0) {
            val charBuffer = CharArray(length)
            reader.read(charBuffer, 0, length)
            val body = String(charBuffer)
            try {
                val json = JSONObject(body)
                val action = json.optString("action")
                val value = if (json.has("value") && !json.isNull("value")) json.getString("value") else null
                if (action.isNotEmpty()) {
                    sendRemoteCommand(action, value)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error parsing remote command: ${e.message}")
            }
        }
    }

    /**
     * Streams media files with RFC 7233 HTTP Byte-Range support (206 Partial Content).
     * This is essential for Smart TVs and web browsers to seek and scrub seamlessly.
     */
    private fun serveMediaStream(
        mediaId: String?,
        headers: Map<String, String>,
        output: OutputStream,
        asDownload: Boolean
    ): Int {
        if (mediaId.isNullOrEmpty()) {
            sendNotFound(output)
            return 404
        }

        val item = getMediaList().find { it.id == mediaId }
        if (item == null) {
            sendNotFound(output)
            return 404
        }

        val uri = Uri.parse(item.uriString)
        val fileLength: Long = if (item.size > 0) item.size else {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
            } catch (_: Exception) {
                -1L
            }
        }

        val rangeHeader = headers["range"]
        var startRange: Long = 0
        var endRange: Long = if (fileLength > 0) fileLength - 1 else -1L

        var isPartial = false
        if (!rangeHeader.isNullOrEmpty() && rangeHeader.startsWith("bytes=")) {
            isPartial = true
            val rangeVal = rangeHeader.substring(6).trim()
            val dashIndex = rangeVal.indexOf('-')
            if (dashIndex != -1) {
                val startStr = rangeVal.substring(0, dashIndex).trim()
                val endStr = rangeVal.substring(dashIndex + 1).trim()
                if (startStr.isNotEmpty()) {
                    startRange = startStr.toLongOrNull() ?: 0L
                }
                if (endStr.isNotEmpty()) {
                    endRange = endStr.toLongOrNull() ?: endRange
                }
            }
        }

        if (fileLength > 0 && endRange >= fileLength) {
            endRange = fileLength - 1
        }
        if (endRange < startRange && fileLength > 0) {
            endRange = fileLength - 1
        }

        val contentLength = if (fileLength > 0) (endRange - startRange + 1) else -1L

        val statusLine = if (isPartial && fileLength > 0) "HTTP/1.1 206 Partial Content\r\n" else "HTTP/1.1 200 OK\r\n"
        val headerBuilder = StringBuilder(statusLine)
        headerBuilder.append("Content-Type: ${item.mimeType}\r\n")
        headerBuilder.append("Accept-Ranges: bytes\r\n")
        headerBuilder.append("Access-Control-Allow-Origin: *\r\n")
        headerBuilder.append("Access-Control-Allow-Headers: Range, Content-Type\r\n")

        if (asDownload) {
            val encodedName = URLEncoder.encode(item.name, "UTF-8").replace("+", "%20")
            headerBuilder.append("Content-Disposition: attachment; filename=\"$encodedName\"; filename*=UTF-8''$encodedName\r\n")
        }

        if (fileLength > 0) {
            headerBuilder.append("Content-Length: $contentLength\r\n")
            if (isPartial) {
                headerBuilder.append("Content-Range: bytes $startRange-$endRange/$fileLength\r\n")
            }
        }
        headerBuilder.append("Connection: keep-alive\r\n\r\n")

        output.write(headerBuilder.toString().toByteArray(Charsets.UTF_8))
        output.flush()

        // Stream binary data
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return 500
            inputStream.use { input ->
                if (startRange > 0) {
                    var skipped = 0L
                    while (skipped < startRange) {
                        val s = input.skip(startRange - skipped)
                        if (s <= 0) break
                        skipped += s
                    }
                }

                val buffer = ByteArray(65536) // 64 KB streaming buffer
                var remaining = contentLength
                while (remaining > 0 || contentLength < 0) {
                    val readLimit = if (contentLength > 0) minOf(buffer.size.toLong(), remaining).toInt() else buffer.size
                    val read = input.read(buffer, 0, readLimit)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    bytesSent.addAndGet(read.toLong())
                    if (contentLength > 0) {
                        remaining -= read
                    }
                }
            }
            return if (isPartial) 206 else 200
        } catch (_: SocketException) {
            // Client seeking or pausing video stream; normal socket closure
            return 206
        } catch (e: Exception) {
            Log.e(tag, "Stream error: ${e.message}")
            return 500
        }
    }

    private fun serveSubtitles(mediaId: String?, output: OutputStream) {
        val item = getMediaList().find { it.id == mediaId }
        val vttContent = if (item?.subtitleUri != null) {
            try {
                val subUri = Uri.parse(item.subtitleUri)
                val rawText = context.contentResolver.openInputStream(subUri)?.bufferedReader()?.use { it.readText() } ?: ""
                convertSrtToVtt(rawText)
            } catch (_: Exception) {
                "WEBVTT\n\n1\n00:00:00.000 --> 00:00:05.000\n[Subtitles active]\n"
            }
        } else {
            "WEBVTT\n\n"
        }

        val bytes = vttContent.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/vtt; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.write(bytes)
    }

    private fun convertSrtToVtt(srt: String): String {
        if (srt.startsWith("WEBVTT")) return srt
        val vtt = StringBuilder("WEBVTT\n\n")
        val lines = srt.lines()
        for (line in lines) {
            if (line.contains("-->")) {
                // Convert SRT comma format 00:01:20,500 to VTT period format 00:01:20.500
                vtt.append(line.replace(",", ".")).append("\n")
            } else {
                vtt.append(line).append("\n")
            }
        }
        return vtt.toString()
    }

    private fun handleFileUpload(input: InputStream, headers: Map<String, String>, output: OutputStream) {
        try {
            val filename = headers["x-filename"]?.let { URLDecoder.decode(it, "UTF-8") } ?: "uploaded_media_${System.currentTimeMillis()}"
            val targetDir = File(context.cacheDir, "CastBeam_Uploads").apply { mkdirs() }
            val targetFile = File(targetDir, filename)

            val contentLength = headers["content-length"]?.toLongOrNull() ?: 0L
            FileOutputStream(targetFile).use { fos ->
                val buffer = ByteArray(32768)
                var bytesRemaining = contentLength
                while (bytesRemaining > 0) {
                    val read = input.read(buffer, 0, minOf(buffer.size.toLong(), bytesRemaining).toInt())
                    if (read == -1) break
                    fos.write(buffer, 0, read)
                    bytesRemaining -= read
                    bytesReceived.addAndGet(read.toLong())
                }
            }
            sendJsonResponse(output, 200, "{\"success\":true,\"file\":\"${targetFile.name}\"}")
        } catch (e: Exception) {
            sendJsonResponse(output, 500, "{\"error\":\"${e.message}\"}")
        }
    }

    private fun sendJsonResponse(output: OutputStream, status: Int, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val statusText = if (status == 200) "OK" else "Error"
        val header = "HTTP/1.1 $status $statusText\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        bytesSent.addAndGet(bytes.size.toLong())
    }

    private fun sendNotFound(output: OutputStream) {
        val msg = "404 Not Found"
        val header = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: ${msg.length}\r\n" +
                "Connection: close\r\n\r\n" +
                msg
        output.write(header.toByteArray(Charsets.UTF_8))
    }

    private fun parseQueryParams(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrEmpty()) return emptyMap()
        val map = mutableMapOf<String, String>()
        rawQuery.split("&").forEach { pair ->
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                map[key] = value
            }
        }
        return map
    }
}
