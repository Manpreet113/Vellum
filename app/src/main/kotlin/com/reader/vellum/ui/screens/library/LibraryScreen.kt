package com.reader.vellum.ui.screens.library

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.reader.vellum.data.local.CollectionInfo
import com.reader.vellum.domain.model.Book
import com.reader.vellum.ui.components.GlassmorphicSurface
import com.reader.vellum.ui.components.indigoGlow
import com.reader.vellum.ui.theme.ElectricIndigo
import com.reader.vellum.ui.theme.GlassWhite
import com.reader.vellum.ui.theme.InkBlack

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
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCollection by viewModel.selectedCollection.collectAsState()
    val isLanServerRunning by viewModel.isLanServerRunning.collectAsState()
    val lanServerAddress by viewModel.lanServerAddress.collectAsState()
    val lanServerPin by viewModel.lanServerPin.collectAsState()
    
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var isSearchActive by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }

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

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importFile(it, onBookClick) }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? -> uri?.let { viewModel.backupProgress(it) } }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.restoreProgress(it) } }

    if (selectedCollection != null) {
        BackHandler { viewModel.onCollectionSelected(null) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                if (isSearchActive) {
                    GlassmorphicSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = CircleShape
                    ) {
                        SearchTopBar(
                            query = searchQuery,
                            onQueryChange = viewModel::onSearchQueryChanged,
                            onClose = {
                                isSearchActive = false
                                viewModel.onSearchQueryChanged("")
                            }
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedCollection != null) selectedCollection!!.uppercase() else "VELLUM",
                            style = MaterialTheme.typography.displaySmall, // Epilogue bold
                            letterSpacing = 4.sp,
                            color = Color.White
                        )
                        Row {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, "Search", tint = Color.White)
                            }
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Default.Tune, "Settings", tint = Color.White)
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (selectedCollection == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlassmorphicSurface(
                        modifier = Modifier.indigoGlow(alpha = 0.1f),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LibraryTab.entries.forEach { tab ->
                                val selected = selectedTab == tab
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (selected) ElectricIndigo else Color.Transparent)
                                        .clickable { viewModel.onTabSelected(tab) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = tab.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            // Add Button
                            IconButton(
                                onClick = { showAddMenu = true },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.05f))
                            ) {
                                Icon(Icons.Default.Add, "Add Content", tint = ElectricIndigo)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
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

    if (showAddMenu) {
        AddContentDialog(
            isLanServerRunning = isLanServerRunning,
            lanServerAddress = lanServerAddress,
            lanServerPin = lanServerPin,
            onToggleLan = viewModel::toggleLanServer,
            onImportFile = {
                showAddMenu = false
                filePicker.launch(arrayOf(
                    "application/pdf",
                    "application/epub+zip",
                    "application/zip",
                    "application/x-cbz"
                ))
            },
            onScanFolder = {
                showAddMenu = false
                directoryPicker.launch(null)
            },
            onBackup = {
                showAddMenu = false
                backupLauncher.launch("vellum_backup.json")
            },
            onRestore = {
                showAddMenu = false
                restoreLauncher.launch(arrayOf("application/json"))
            },
            onDismiss = { showAddMenu = false }
        )
    }

    if (showSettings) {
        SettingsDialog(
            viewModel = viewModel,
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
            columns = GridCells.Adaptive(160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(collections) { info ->
                CollectionItem(info, onClick = { onCollectionClick(info.collectionName) })
            }
        }
    }
}

@Composable
fun CollectionItem(info: CollectionInfo, onClick: () -> Unit) {
    GlassmorphicSurface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(32.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Folder, null, modifier = Modifier.size(56.dp), tint = ElectricIndigo)
            Spacer(Modifier.height(16.dp))
            Text(
                text = info.collectionName ?: "UNCATEGORIZED",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${info.bookCount} ITEMS",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
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
        EmptyState("Your library is silent.")
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 120.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(40.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (isScanning) {
                item(span = { GridItemSpan(maxLineSpan) }) { ScanningProgressCard(scanProgress) }
            }

            if (showSections && continueReading.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "IN PROGRESS",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricIndigo,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(bottom = 40.dp)
                    ) {
                        items(continueReading) { book ->
                            ContinueReadingCard(book, onClick = { onBookClick(book.id) })
                        }
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "COLLECTION",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.3f),
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
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
        Text(
            message, 
            style = MaterialTheme.typography.headlineMedium, 
            color = Color.White.copy(alpha = 0.2f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ScanningProgressCard(progress: com.reader.vellum.data.repository.ScanProgress?) {
    progress?.let { p ->
        GlassmorphicSurface(
            modifier = Modifier.padding(bottom = 24.dp).indigoGlow(alpha = 0.1f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                LinearProgressIndicator(
                    progress = { if (p.total > 0) p.current.toFloat() / p.total else 0f },
                    modifier = Modifier.fillMaxWidth().height(2.dp).clip(CircleShape),
                    color = ElectricIndigo,
                    trackColor = Color.White.copy(alpha = 0.05f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    if (p.total > 0) "INTEGRATING CONTENT: ${p.current}/${p.total}" else "SCANNING ARCHIVES...",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricIndigo,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun ContinueReadingCard(
    book: Book, 
    onClick: () -> Unit
) {
    GlassmorphicSurface(
        modifier = Modifier
            .width(300.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(32.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = book.coverPath,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp, 120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .indigoGlow(alpha = 0.1f, borderRadius = 16.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { book.progress.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = ElectricIndigo,
                        trackColor = Color.White.copy(alpha = 0.05f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${(book.progress * 100).toInt()}% COMPLETE",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricIndigo,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun BookItem(
    book: Book, 
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(0.7f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                .indigoGlow(alpha = 0.05f, borderRadius = 24.dp)
        ) {
            AsyncImage(
                model = book.coverPath,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            if (book.progress > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(book.progress.toFloat())
                            .background(ElectricIndigo)
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = book.title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun SearchTopBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("FILTER ARCHIVES...", style = MaterialTheme.typography.labelLarge) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = ElectricIndigo) },
        trailingIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = Color.White) } },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        singleLine = true
    )
}

@Composable
fun AddContentDialog(
    isLanServerRunning: Boolean,
    lanServerAddress: String?,
    lanServerPin: String?,
    onToggleLan: () -> Unit,
    onImportFile: () -> Unit,
    onScanFolder: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF080808), // Darker Ink Black
        modifier = Modifier
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(40.dp))
            .indigoGlow(alpha = 0.1f, borderRadius = 40.dp, blurRadius = 60.dp),
        title = { 
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(ElectricIndigo.copy(alpha = 0.4f), CircleShape)
                        .padding(bottom = 12.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "INTEGRATION", 
                    style = MaterialTheme.typography.headlineSmall, 
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 8.sp, 
                    color = Color.White
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(28.dp), 
                modifier = Modifier.padding(top = 16.dp)
            ) {
                // Section 1: Wireless Transfer (New)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "WIRELESS INTEGRATION", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = ElectricIndigo, 
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (isLanServerRunning) ElectricIndigo.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.02f))
                            .border(0.5.dp, if (isLanServerRunning) ElectricIndigo.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isLanServerRunning) Icons.Default.Wifi else Icons.Default.WifiOff,
                                        null,
                                        tint = if (isLanServerRunning) ElectricIndigo else Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        if (isLanServerRunning) "SERVER ACTIVE" else "START ARCHIVE SERVER",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Switch(
                                    checked = isLanServerRunning,
                                    onCheckedChange = { onToggleLan() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = ElectricIndigo
                                    )
                                )
                            }
                            if (isLanServerRunning && lanServerAddress != null) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Access from PC browser:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                Text(
                                    lanServerAddress,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ElectricIndigo,
                                    letterSpacing = 1.sp
                                )
                                if (lanServerPin != null) {
                                    Spacer(Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "SERVER PIN: ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                        Text(
                                            lanServerPin,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            letterSpacing = 2.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Local Sources
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "LOCAL ARCHIVES", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = Color.White.copy(alpha = 0.3f), 
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            AddOption(
                                title = "IMPORT",
                                subtitle = "Single file",
                                icon = Icons.AutoMirrored.Filled.NoteAdd,
                                onClick = onImportFile,
                                compact = true
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            AddOption(
                                title = "SCAN",
                                subtitle = "Folder",
                                icon = Icons.Default.CreateNewFolder,
                                onClick = onScanFolder,
                                compact = true
                            )
                        }
                    }
                }

                // Section 3: System State
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "SYSTEM STATE", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = Color.White.copy(alpha = 0.2f), 
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            AddOption(
                                title = "BACKUP",
                                subtitle = "Export",
                                icon = Icons.Default.CloudUpload,
                                onClick = onBackup,
                                compact = true
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            AddOption(
                                title = "RESTORE",
                                subtitle = "Import",
                                icon = Icons.Default.CloudDownload,
                                onClick = onRestore,
                                compact = true
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { 
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp), 
                contentAlignment = Alignment.Center
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = 24.dp)
                ) { 
                    Text(
                        "DISMISS", 
                        color = Color.White.copy(alpha = 0.6f), 
                        fontWeight = FontWeight.Bold, 
                        letterSpacing = 2.sp,
                        style = MaterialTheme.typography.labelMedium
                    ) 
                }
            }
        }
    )
}

@Composable
fun AddOption(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    compact: Boolean = false,
    accent: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = if (accent) 0.04f else 0.02f))
            .border(
                width = 0.5.dp, 
                color = if (accent) ElectricIndigo.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f), 
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
            .padding(if (compact) 16.dp else 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (compact) Arrangement.Center else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                icon, 
                null, 
                tint = if (accent) ElectricIndigo else Color.White.copy(alpha = 0.6f), 
                modifier = Modifier
                    .size(if (compact) 24.dp else 28.dp)
                    .then(if (accent) Modifier.indigoGlow(alpha = 0.4f, blurRadius = 16.dp) else Modifier)
            )
            if (!compact) {
                Spacer(Modifier.width(20.dp))
                Column {
                    Text(
                        title, 
                        style = MaterialTheme.typography.labelLarge, 
                        fontWeight = FontWeight.ExtraBold, 
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        subtitle.uppercase(), 
                        style = MaterialTheme.typography.labelSmall, 
                        color = Color.White.copy(alpha = 0.3f),
                        letterSpacing = 1.sp
                    )
                }
            } else {
                Spacer(Modifier.width(12.dp))
                Text(
                    title, 
                    style = MaterialTheme.typography.labelSmall, 
                    fontWeight = FontWeight.Bold, 
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun SettingsDialog(viewModel: LibraryViewModel, onDismiss: () -> Unit) {
    val mangaMode by viewModel.mangaMode.collectAsState(false)
    val adaptiveChroma by viewModel.adaptiveChroma.collectAsState(true)
    val volumeKeys by viewModel.volumeKeys.collectAsState(false)
    val tapToTurn by viewModel.tapToTurn.collectAsState(true)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F0F0F),
        modifier = Modifier.border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp)),
        title = { 
            Text(
                "SYSTEM SETTINGS", 
                style = MaterialTheme.typography.headlineSmall, 
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                SettingsToggle("MANGA MODE (RTL)", mangaMode, viewModel::setMangaMode)
                SettingsToggle("ADAPTIVE CHROMATICITY", adaptiveChroma, viewModel::setAdaptiveChroma)
                SettingsToggle("VOLUME NAVIGATION", volumeKeys, viewModel::setVolumeKeys)
                SettingsToggle("TAP ZONE NAVIGATION", tapToTurn, viewModel::setTapToTurn)
            }
        },
        confirmButton = { 
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                shape = CircleShape
            ) { 
                Text("DISMISS", fontWeight = FontWeight.Bold) 
            } 
        }
    )
}

@Composable
fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label, 
            style = MaterialTheme.typography.labelLarge, 
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ElectricIndigo,
                uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
