package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MediaCategory
import com.example.model.MediaItem
import com.example.model.SharedFolder
import com.example.ui.theme.*

@Composable
fun FoldersAndMediaView(
    sharedFolders: List<SharedFolder>,
    mediaItems: List<MediaItem>,
    selectedCategory: MediaCategory?,
    searchQuery: String,
    isLoading: Boolean,
    onAddFolder: (Uri) -> Unit,
    onRemoveFolder: (String) -> Unit,
    onSelectCategory: (MediaCategory?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onPlayOnTv: (MediaItem) -> Unit
) {
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            onAddFolder(uri)
        }
    }

    val filteredItems = mediaItems.filter { item ->
        val matchesCategory = selectedCategory == null || item.category == selectedCategory
        val matchesSearch = searchQuery.isEmpty() ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.folderName.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("folders_and_media_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Folder Selection Header & Button
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CinemaSurface),
                border = BorderStroke(1.dp, CinemaOutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Hosted Storage Folders",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CinemaTextPrimary
                            )
                            Text(
                                text = "${sharedFolders.size} folder(s) exposed to TV",
                                fontSize = 12.sp,
                                color = CinemaTextSecondary
                            )
                        }

                        Button(
                            onClick = { folderPickerLauncher.launch(null) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CinemaPrimary,
                                contentColor = CinemaOnPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("add_folder_button")
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select Folder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (sharedFolders.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(sharedFolders) { folder ->
                                Surface(
                                    color = CinemaSurfaceContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, CinemaOutline)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = CinemaPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = folder.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CinemaTextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "(${folder.itemCount})",
                                            fontSize = 11.sp,
                                            color = CinemaTextMuted
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove folder",
                                            tint = CinemaTextMuted,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { onRemoveFolder(folder.uriString) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Search Bar & Filter Pills
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search indexed movies & media...", color = CinemaTextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CinemaTextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = CinemaTextSecondary)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CinemaPrimary,
                        unfocusedBorderColor = CinemaOutline,
                        focusedTextColor = CinemaTextPrimary,
                        unfocusedTextColor = CinemaTextPrimary,
                        focusedContainerColor = CinemaSurface,
                        unfocusedContainerColor = CinemaSurface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("media_search_field")
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { onSelectCategory(null) },
                            label = { Text("All (${mediaItems.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CinemaPrimary,
                                selectedLabelColor = CinemaOnPrimary,
                                containerColor = CinemaSurface,
                                labelColor = CinemaTextSecondary
                            )
                        )
                    }
                    items(MediaCategory.values()) { cat ->
                        val count = mediaItems.count { it.category == cat }
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { onSelectCategory(cat) },
                            label = { Text("${cat.label} ($count)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CinemaPrimary,
                                selectedLabelColor = CinemaOnPrimary,
                                containerColor = CinemaSurface,
                                labelColor = CinemaTextSecondary
                            )
                        )
                    }
                }
            }
        }

        // Loading or Empty State or Media Items List
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CinemaPrimary)
                }
            }
        } else if (filteredItems.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CinemaSurface),
                    border = BorderStroke(1.dp, CinemaOutlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = CinemaTextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No media matches your search" else "No files found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CinemaTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap 'Select Folder' to expose downloaded movies, videos, or music to your TV.",
                            fontSize = 13.sp,
                            color = CinemaTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredItems, key = { it.id }) { item ->
                MediaItemRow(item = item, onPlay = { onPlayOnTv(item) })
            }
        }
    }
}

@Composable
private fun MediaItemRow(
    item: MediaItem,
    onPlay: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CinemaSurface),
        border = BorderStroke(1.dp, CinemaOutlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("media_row_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CinemaSurfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIconVector(item.category),
                    contentDescription = null,
                    tint = CinemaPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CinemaTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.formattedSize,
                        fontSize = 12.sp,
                        color = CinemaTextMuted
                    )
                    Text("•", fontSize = 10.sp, color = CinemaTextMuted)
                    Text(
                        text = item.folderName,
                        fontSize = 12.sp,
                        color = CinemaTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.subtitleUri != null) {
                        Surface(
                            color = CinemaSecondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "CC",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CinemaOnSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CinemaPrimary,
                    contentColor = CinemaOnPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("play_button_${item.id}")
            ) {
                Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cast", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun getCategoryIconVector(category: MediaCategory): ImageVector {
    return when (category) {
        MediaCategory.VIDEO -> Icons.Default.Movie
        MediaCategory.AUDIO -> Icons.Default.MusicNote
        MediaCategory.IMAGE -> Icons.Default.Image
        MediaCategory.DOCUMENT -> Icons.Default.Description
        MediaCategory.OTHER -> Icons.Default.InsertDriveFile
    }
}
