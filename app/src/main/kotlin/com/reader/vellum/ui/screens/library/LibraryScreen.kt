package com.reader.vellum.ui.screens.library

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.reader.vellum.data.local.CollectionInfo
import com.reader.vellum.data.repository.SortOrder
import com.reader.vellum.domain.model.Book

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onBookClick: (String) -> Unit
) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val totalBookCount by viewModel.bookCount.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentSort by viewModel.sortOrder.collectAsState()
    val selectedCollection by viewModel.selectedCollection.collectAsState()
    
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var isSearchActive by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val directoryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.scanDirectory(it.toString())
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? -> uri?.let { viewModel.backupProgress(it) } }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.restoreProgress(it) } }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importFile(it, onBookClick) }
    }

    if (selectedCollection != null) {
        BackHandler { viewModel.onCollectionSelected(null) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (isSearchActive) {
                SearchTopBar(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    onClose = {
                        isSearchActive = false
                        viewModel.onSearchQueryChanged("")
                    }
                )
            } else {
                Column {
                    TopAppBar(
                        title = { 
                            Text(
                                if (selectedCollection != null) selectedCollection!! else "Library",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            ) 
                        },
                        navigationIcon = {
                            if (selectedCollection != null) {
                                IconButton(onClick = { viewModel.onCollectionSelected(null) }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, "Search")
                            }
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, "Sort")
                                SortDropdownMenu(
                                    expanded = showSortMenu,
                                    currentSort = currentSort,
                                    onSortChange = viewModel::onSortOrderChanged,
                                    onDismiss = { showSortMenu = false }
                                )
                            }
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Default.Settings, "Settings")
                            }
                            IconButton(onClick = {
                                filePicker.launch(arrayOf(
                                    "application/pdf",
                                    "application/epub+zip",
                                    "application/zip",
                                    "application/x-cbz"
                                ))
                            }) {
                                Icon(Icons.AutoMirrored.Filled.NoteAdd, "Open File")
                            }
                            IconButton(onClick = { directoryPicker.launch(null) }) {
                                Icon(Icons.Default.Add, "Add Folder")
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                    
                    if (selectedCollection == null) {
                        PrimaryTabRow(
                            selectedTabIndex = selectedTab.ordinal,
                            divider = {}
                        ) {
                            LibraryTab.values().forEach { tab ->
                                Tab(
                                    selected = selectedTab == tab,
                                    onClick = { viewModel.onTabSelected(tab) },
                                    text = { 
                                        Text(
                                            tab.name.lowercase().replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.labelLarge
                                        ) 
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                selectedCollection != null -> {
                    val collItems = viewModel.booksInCollection.collectAsLazyPagingItems()
                    BooksGrid(collItems, onBookClick, isScanning, scanProgress)
                }
                selectedTab == LibraryTab.COLLECTIONS -> {
                    val collections by viewModel.collections.collectAsState(initial = emptyList())
                    CollectionsGrid(collections, onCollectionClick = viewModel::onCollectionSelected)
                }
                selectedTab == LibraryTab.BOOKS -> {
                    val books = viewModel.books.collectAsLazyPagingItems()
                    val continueReading by viewModel.continueReadingBooks.collectAsState(initial = emptyList())
                    BooksGrid(books, onBookClick, isScanning, scanProgress, continueReading, searchQuery.isEmpty())
                }
                selectedTab == LibraryTab.COMPLETED -> {
                    val completed = viewModel.completedBooks.collectAsLazyPagingItems()
                    BooksGrid(completed, onBookClick, isScanning, scanProgress)
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            viewModel = viewModel,
            onBackup = { backupLauncher.launch("vellum_backup.json") },
            onRestore = { restoreLauncher.launch(arrayOf("application/json")) },
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
fun CollectionsGrid(collections: List<CollectionInfo>, onCollectionClick: (String?) -> Unit) {
    if (collections.isEmpty()) {
        EmptyState("No collections found")
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(collections) { info ->
                CollectionItem(info, onClick = { onCollectionClick(info.collectionName) })
            }
        }
    }
}

@Composable
fun CollectionItem(info: CollectionInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Folder, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(
                text = info.collectionName ?: "Uncategorized",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${info.bookCount} items",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BooksGrid(
    pagingItems: androidx.paging.compose.LazyPagingItems<Book>,
    onBookClick: (String) -> Unit,
    isScanning: Boolean,
    scanProgress: com.reader.vellum.data.repository.ScanProgress?,
    continueReading: List<Book> = emptyList(),
    showSections: Boolean = true
) {
    val isInitialLoading = pagingItems.loadState.refresh is LoadState.Loading

    if (pagingItems.itemCount == 0 && !isScanning && !isInitialLoading) {
        EmptyState("No books found")
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(130.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isScanning) {
                item(span = { GridItemSpan(maxLineSpan) }) { ScanningProgressCard(scanProgress) }
            }

            if (showSections && continueReading.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Continue Reading",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(continueReading) { book ->
                            ContinueReadingCard(book, onClick = { onBookClick(book.id) })
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                }
            }

            items(pagingItems.itemCount) { index ->
                val book = pagingItems[index]
                book?.let { BookItem(it, onClick = { onBookClick(it.id) }) }
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ScanningProgressCard(progress: com.reader.vellum.data.repository.ScanProgress?) {
    progress?.let { p ->
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            LinearProgressIndicator(
                progress = { if (p.total > 0) p.current.toFloat() / p.total else 0f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (p.total > 0) "Importing: ${p.current}/${p.total}" else "Scanning for files...",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ContinueReadingCard(book: Book, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(240.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = book.coverPath,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp, 90.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { book.progress.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${(book.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun BookItem(book: Book, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.aspectRatio(0.7f).fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box {
                AsyncImage(
                    model = book.coverPath,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (book.progress > 0) {
                    LinearProgressIndicator(
                        progress = { book.progress.toFloat() },
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = book.title,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (book.collectionName != null) {
            Text(
                text = book.collectionName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SearchTopBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().statusBarsPadding(),
        placeholder = { Text("Search your library...") },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurface) },
        trailingIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface) } },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        singleLine = true
    )
}

@Composable
fun SortDropdownMenu(expanded: Boolean, currentSort: SortOrder, onSortChange: (SortOrder) -> Unit, onDismiss: () -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        SortOrder.values().forEach { order ->
            DropdownMenuItem(
                text = { Text(order.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                onClick = { onSortChange(order); onDismiss() },
                leadingIcon = { if (currentSort == order) Icon(Icons.Default.Check, null) }
            )
        }
    }
}

@Composable
fun SettingsDialog(viewModel: LibraryViewModel, onBackup: () -> Unit, onRestore: () -> Unit, onDismiss: () -> Unit) {
    val mangaMode by viewModel.mangaMode.collectAsState(false)
    val tapToTurn by viewModel.tapToTurn.collectAsState(true)
    val volumeKeys by viewModel.volumeKeys.collectAsState(false)
    val hideCompleted by viewModel.hideCompleted.collectAsState(true)
    val adaptiveChroma by viewModel.adaptiveChroma.collectAsState(true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SettingsToggle("Manga Mode", mangaMode, viewModel::setMangaMode)
                SettingsToggle("Adaptive Colors", adaptiveChroma, viewModel::setAdaptiveChroma)
                SettingsToggle("Volume Navigation", volumeKeys, viewModel::setVolumeKeys)
                SettingsToggle("Tap Navigation", tapToTurn, viewModel::setTapToTurn)
                SettingsToggle("Hide Completed", hideCompleted, viewModel::setHideCompleted)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onBackup, modifier = Modifier.weight(1f)) { Text("Backup") }
                    Button(onClick = onRestore, modifier = Modifier.weight(1f)) { Text("Restore") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
