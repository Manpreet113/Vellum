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
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
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
import androidx.compose.ui.unit.em
import androidx.compose.material3.HorizontalDivider
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.reader.vellum.data.local.CollectionInfo
import com.reader.vellum.domain.model.Book
import com.reader.vellum.ui.components.GlassmorphicSurface
import com.reader.vellum.ui.components.ShimmerBox
import com.reader.vellum.ui.components.indigoGlow
import com.reader.vellum.ui.theme.ElectricIndigo
import com.reader.vellum.ui.theme.GlassWhite
import com.reader.vellum.ui.theme.InkBlack
import com.reader.vellum.ui.theme.SurfaceElevated1
import com.reader.vellum.ui.theme.SurfaceElevated2
import com.reader.vellum.ui.theme.EpilogueFontFamily

enum class LibraryScreenState {
    MAIN,
    SEARCH,
    ADD_CONTENT,
    SETTINGS
}

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
    var screenState by rememberSaveable { mutableStateOf(LibraryScreenState.MAIN) }

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
    if (screenState != LibraryScreenState.MAIN) {
        BackHandler { screenState = LibraryScreenState.MAIN }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedCollection != null) {
                        IconButton(
                            onClick = {
                                viewModel.onCollectionSelected(null)
                            },
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }
                    Text(
                        text = "VELLUM",
                        fontFamily = EpilogueFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 48.sp,
                        lineHeight = 52.sp,
                        letterSpacing = (-0.02).em,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        bottomBar = {
            if (selectedCollection == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* block clicks from passing through */ }
                        .navigationBarsPadding()
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        thickness = 0.5.dp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val booksActive = screenState == LibraryScreenState.MAIN && selectedTab == LibraryTab.BOOKS
                        NavItem(
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            label = "Books",
                            active = booksActive,
                            onClick = {
                                screenState = LibraryScreenState.MAIN
                                viewModel.onTabSelected(LibraryTab.BOOKS)
                            }
                        )

                        val searchActive = screenState == LibraryScreenState.SEARCH
                        NavItem(
                            icon = Icons.Default.Search,
                            label = "Search",
                            active = searchActive,
                            onClick = {
                                screenState = LibraryScreenState.SEARCH
                            }
                        )

                        val importActive = screenState == LibraryScreenState.ADD_CONTENT
                        NavItem(
                            icon = Icons.Default.Add,
                            label = "Import",
                            active = importActive,
                            onClick = {
                                screenState = LibraryScreenState.ADD_CONTENT
                            }
                        )

                        val libraryActive = screenState == LibraryScreenState.MAIN && selectedTab == LibraryTab.COLLECTIONS
                        NavItem(
                            icon = Icons.AutoMirrored.Filled.LibraryBooks,
                            label = "Library",
                            active = libraryActive,
                            onClick = {
                                screenState = LibraryScreenState.MAIN
                                viewModel.onTabSelected(LibraryTab.COLLECTIONS)
                            }
                        )

                        val settingsActive = screenState == LibraryScreenState.SETTINGS
                        NavItem(
                            icon = Icons.Default.Settings,
                            label = "Settings",
                            active = settingsActive,
                            onClick = {
                                screenState = LibraryScreenState.SETTINGS
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            when {
                selectedCollection != null -> {
                    val collItems = viewModel.booksInCollection.collectAsLazyPagingItems()
                    BooksGrid(collItems, onBookClick, isScanning, scanProgress)
                }
                screenState == LibraryScreenState.SEARCH -> {
                    val searchBooks = viewModel.books.collectAsLazyPagingItems()
                    SearchPage(
                        query = searchQuery,
                        onQueryChange = viewModel::onSearchQueryChanged,
                        books = searchBooks,
                        onBookClick = onBookClick
                    )
                }
                screenState == LibraryScreenState.ADD_CONTENT -> {
                    AddContentPage(
                        isLanServerRunning = isLanServerRunning,
                        lanServerAddress = lanServerAddress,
                        lanServerPin = lanServerPin,
                        onToggleLan = viewModel::toggleLanServer,
                        onImportFile = {
                            filePicker.launch(arrayOf(
                                "application/pdf",
                                "application/epub+zip",
                                "application/zip",
                                "application/x-cbz"
                            ))
                        },
                        onScanFolder = {
                            directoryPicker.launch(null)
                        },
                        onBackup = {
                            backupLauncher.launch("vellum_backup.json")
                        },
                        onRestore = {
                            restoreLauncher.launch(arrayOf("application/json"))
                        }
                    )
                }
                screenState == LibraryScreenState.SETTINGS -> {
                    SettingsPage(viewModel = viewModel)
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
}

@Composable
fun CollectionsGrid(collections: List<CollectionInfo>, onCollectionClick: (String?) -> Unit) {
    if (collections.isEmpty()) {
        EmptyState("No collections found", showAddHint = false)
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 120.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "COLLECTIONS",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${collections.size} FOLDERS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
            items(collections) { info ->
                CollectionItem(info, onClick = { onCollectionClick(info.collectionName) })
            }
        }
    }
}@Composable
fun MangaCoverStack(
    covers: List<String>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.833f)
    ) {
        val parentWidth = maxWidth
        val parentHeight = maxHeight

        val childWidth = parentWidth * 0.7f
        val coverCount = covers.size

        if (coverCount == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.02f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = ElectricIndigo
                )
            }
        } else {
            if (coverCount >= 3) {
                MangaCoverItem(
                    model = covers[2],
                    modifier = Modifier
                        .size(childWidth, childWidth / 0.7f)
                        .graphicsLayer {
                            translationX = (parentWidth * 0.25f).toPx()
                            translationY = (parentHeight * 0.15f).toPx()
                            rotationZ = 5f
                            shadowElevation = 8.dp.toPx()
                        }
                )
            }

            if (coverCount >= 2) {
                MangaCoverItem(
                    model = covers[1],
                    modifier = Modifier
                        .size(childWidth, childWidth / 0.7f)
                        .graphicsLayer {
                            translationX = (parentWidth * 0.05f).toPx()
                            translationY = (parentHeight * 0.05f).toPx()
                            rotationZ = -5f
                            shadowElevation = 12.dp.toPx()
                        }
                )
            }

            val frontTranslationX = when (coverCount) {
                1 -> parentWidth * 0.15f
                2 -> parentWidth * 0.18f
                else -> parentWidth * 0.15f
            }
            val frontTranslationY = when (coverCount) {
                1 -> parentHeight * 0.1f
                2 -> parentHeight * 0.12f
                else -> parentHeight * 0.1f
            }

            MangaCoverItem(
                model = covers[0],
                modifier = Modifier
                    .size(childWidth, childWidth / 0.7f)
                    .graphicsLayer {
                        translationX = frontTranslationX.toPx()
                        translationY = frontTranslationY.toPx()
                        rotationZ = 0f
                        shadowElevation = 16.dp.toPx()
                    }
            )
        }
    }
}

@Composable
fun MangaCoverItem(
    model: String,
    modifier: Modifier = Modifier
) {
    SubcomposeAsyncImage(
        model = model,
        contentDescription = null,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(6.dp)),
        contentScale = ContentScale.Crop,
        loading = {
            ShimmerBox(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(6.dp)
            )
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1A1A2E), Color(0xFF0F0F1A))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = ElectricIndigo.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    )
}

@Composable
fun CollectionItem(info: CollectionInfo, onClick: () -> Unit) {
    val covers = remember(info.coverPaths) {
        info.coverPaths?.split(",")?.filter { it.isNotBlank() }?.take(3) ?: emptyList()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MangaCoverStack(
                covers = covers,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = info.collectionName?.uppercase() ?: "UNCATEGORIZED",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "(${info.bookCount} ITEMS)",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}@Composable
fun LastReadHeroBanner(
    book: Book,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        SubcomposeAsyncImage(
            model = book.coverPath,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            loading = {
                ShimmerBox(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(16.dp))
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1A1A2E), Color(0xFF0F0F1A))
                            )
                        )
                )
            }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = "LAST READ",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = book.title.uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                val progressPercent = (book.progress * 100).toInt()
                Text(
                    text = "${book.author?.uppercase() ?: "UNKNOWN AUTHOR"} • ${progressPercent}% COMPLETE",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricIndigo,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricIndigo,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "RESUME",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
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
        EmptyState("Your library is silent.", showAddHint = showSections)
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
                val lastReadBook = continueReading.first()
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LastReadHeroBanner(lastReadBook, onClick = { onBookClick(lastReadBook.id) })
                }

                val remainingInProgress = continueReading.drop(1)
                if (remainingInProgress.isNotEmpty()) {
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
                        val background = MaterialTheme.colorScheme.background
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Transparent,
                                                background.copy(alpha = 0.0f),
                                                background.copy(alpha = 0.85f)
                                            ),
                                            startX = 0f,
                                            endX = size.width
                                        )
                                    )
                                }
                        ) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                contentPadding = PaddingValues(bottom = 40.dp, end = 32.dp)
                            ) {
                                items(remainingInProgress) { book ->
                                    ContinueReadingCard(book, onClick = { onBookClick(book.id) })
                                }
                            }
                        }
                    }
                }
            }

            if (showSections) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "LIBRARY",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.3f),
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    )
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
fun EmptyState(message: String, showAddHint: Boolean = false) {
    // 1g: Better empty state with icon, raised alpha, and CTA
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.White.copy(alpha = 0.18f)
            )
            Text(
                message,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White.copy(alpha = 0.45f),
                textAlign = TextAlign.Center
            )
            if (showAddHint) {
                Text(
                    "Tap + to add your first book",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.25f),
                    textAlign = TextAlign.Center
                )
            }
        }
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
                // 1f: Author name
                if (!book.author.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.45f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
                .aspectRatio(0.667f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
        ) {
            SubcomposeAsyncImage(
                model = book.coverPath,
                contentDescription = book.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    ShimmerBox(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(8.dp)
                    )
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF1A1A2E),
                                        Color(0xFF0F0F1A)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = ElectricIndigo.copy(alpha = 0.4f),
                                modifier = Modifier.size(32.dp)
                            )
                            val initials = book.title
                                .split(" ")
                                .filter { it.isNotBlank() }
                                .take(2)
                                .joinToString("") { it.first().uppercaseChar().toString() }
                            Text(
                                text = initials.ifBlank { "?" },
                                style = MaterialTheme.typography.headlineMedium,
                                color = ElectricIndigo.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            )

            if (book.progress > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color(0xFF1E1E1E))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(book.progress.toFloat())
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = book.title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        if (!book.author.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = book.author.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
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
fun SettingsItem(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                    color = Color(0xFFE2E2E2),
                    fontWeight = FontWeight.SemiBold
                )
                if (description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                        color = Color(0xFFC7C4D7)
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF121414),
                    checkedTrackColor = ElectricIndigo,
                    uncheckedThumbColor = Color(0xFFE2E2E2),
                    uncheckedTrackColor = Color(0xFF1E2020),
                    uncheckedBorderColor = Color(0xFF464554)
                )
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.05f))
            )
        }
    }
}


@Composable
fun SearchPage(
    query: String,
    onQueryChange: (String) -> Unit,
    books: androidx.paging.compose.LazyPagingItems<Book>,
    onBookClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
            placeholder = { Text("FILTER ARCHIVES...", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.3f)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = ElectricIndigo) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        if (books.itemCount == 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (query.isEmpty()) "TYPE TO SEARCH ARCHIVES" else "NO ARCHIVES FOUND",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.3f),
                    letterSpacing = 2.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(40.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(books.itemCount) { index ->
                    val book = books[index]
                    book?.let { BookItem(it, onClick = { onBookClick(it.id) }) }
                }
            }
        }
    }
}

@Composable
fun AddContentPage(
    isLanServerRunning: Boolean,
    lanServerAddress: String?,
    lanServerPin: String?,
    onToggleLan: () -> Unit,
    onImportFile: () -> Unit,
    onScanFolder: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit
) {
    val parsedIp = remember(lanServerAddress) {
        if (lanServerAddress == null) "127.0.0.1" else {
            val cleanAddr = lanServerAddress.replace("http://", "").replace("https://", "")
            val parts = cleanAddr.split(":")
            parts.firstOrNull() ?: cleanAddr
        }
    }
    val parsedPort = remember(lanServerAddress) {
        if (lanServerAddress == null) "8080" else {
            val cleanAddr = lanServerAddress.replace("http://", "").replace("https://", "")
            val parts = cleanAddr.split(":")
            if (parts.size > 1) parts[1] else "8080"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 120.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = "Integrations",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = EpilogueFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = Color.White
        )
        
        Spacer(Modifier.height(24.dp))

        // Wireless Integration Section
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "WIRELESS INTEGRATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFC7C4D7),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Switch(
                    checked = isLanServerRunning,
                    onCheckedChange = { onToggleLan() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF121414),
                        checkedTrackColor = ElectricIndigo,
                        uncheckedThumbColor = Color(0xFFE2E2E2),
                        uncheckedTrackColor = Color(0xFF1E2020),
                        uncheckedBorderColor = Color(0xFF464554)
                    )
                )
            }
            Spacer(Modifier.height(16.dp))
            
            // Server State Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0C0F0F))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isLanServerRunning) ElectricIndigo else Color.White.copy(alpha = 0.3f))
                        )
                        Text(
                            text = if (isLanServerRunning) "Archive Server: Online" else "Archive Server: Offline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    AnimatedVisibility(
                        visible = isLanServerRunning,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // IP
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "IP ADDRESS",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Color.White.copy(alpha = 0.45f)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF121414), RoundedCornerShape(4.dp))
                                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Wifi,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.45f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = parsedIp,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                
                                // Port
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "PORT",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Color.White.copy(alpha = 0.45f)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF121414), RoundedCornerShape(4.dp))
                                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Dns,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.45f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = parsedPort,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            
                            if (lanServerPin != null) {
                                Spacer(Modifier.height(12.dp))
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "PIN",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Color.White.copy(alpha = 0.45f)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(ElectricIndigo.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .border(1.dp, ElectricIndigo.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = ElectricIndigo,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = lanServerPin,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 2.sp,
                                                fontSize = 11.sp
                                            ),
                                            color = ElectricIndigo
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))

        // Local Archive Section
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "LOCAL ARCHIVES",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFC7C4D7),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LocalArchiveButton(
                    title = "Import File",
                    description = "Single archive ingestion",
                    icon = Icons.AutoMirrored.Filled.NoteAdd,
                    onClick = onImportFile,
                    modifier = Modifier.weight(1f)
                )
                LocalArchiveButton(
                    title = "Scan Directory",
                    description = "Batch import from folder",
                    icon = Icons.Default.Folder,
                    onClick = onScanFolder,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))

        // System State Section
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "SYSTEM STATE",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFC7C4D7),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(16.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0C0F0F))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onBackup)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = Color(0xFFC7C4D7),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Backup Library Data",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFFC7C4D7),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.05f))
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onRestore)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = Color(0xFFC7C4D7),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Restore from Backup",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFFC7C4D7),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LocalArchiveButton(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0C0F0F))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E2020)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFC7C4D7),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp, lineHeight = 14.sp),
                    color = Color(0xFFC7C4D7)
                )
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = ElectricIndigo,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.1f))
        )
    }
}

@Composable
fun SettingsPage(viewModel: LibraryViewModel) {
    val mangaMode by viewModel.mangaMode.collectAsState(false)
    val volumeKeys by viewModel.volumeKeys.collectAsState(false)
    val tapToTurn by viewModel.tapToTurn.collectAsState(true)
    val hideCompleted by viewModel.hideCompleted.collectAsState(true)
    val keepScreenOn by viewModel.keepScreenOn.collectAsState(true)
    val hqScaling by viewModel.hqScaling.collectAsState(false)
    val longPressMenu by viewModel.longPressMenu.collectAsState(false)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 120.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = EpilogueFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = Color.White
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Manage your reading experience and library preferences.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFC7C4D7)
        )
        Spacer(Modifier.height(24.dp))

        // Library Group
        SettingsSectionHeader(title = "Library")
        SettingsItem(
            label = "Hide Completed",
            description = "Remove finished volumes from the main library view to keep it uncluttered.",
            checked = hideCompleted,
            onCheckedChange = viewModel::setHideCompleted,
            showDivider = false
        )

        Spacer(Modifier.height(16.dp))

        // Reading Group
        SettingsSectionHeader(title = "Reading")
        SettingsItem(
            label = "Manga Mode",
            description = "Enable right-to-left reading orientation for an authentic manga experience.",
            checked = mangaMode,
            onCheckedChange = viewModel::setMangaMode,
            showDivider = true
        )
        SettingsItem(
            label = "Keep Screen On",
            description = "Prevent the device from sleeping while you are actively reading a chapter.",
            checked = keepScreenOn,
            onCheckedChange = viewModel::setKeepScreenOn,
            showDivider = true
        )
        SettingsItem(
            label = "High-Quality Scaling",
            description = "Use advanced interpolation for sharper image rendering. May consume more battery.",
            checked = hqScaling,
            onCheckedChange = viewModel::setHqScaling,
            showDivider = false
        )

        Spacer(Modifier.height(16.dp))

        // Navigation Group
        SettingsSectionHeader(title = "Navigation")
        SettingsItem(
            label = "Volume Button Navigation",
            description = "Use your device's hardware volume keys to turn pages.",
            checked = volumeKeys,
            onCheckedChange = viewModel::setVolumeKeys,
            showDivider = true
        )
        SettingsItem(
            label = "Tap Zone Navigation",
            description = "Divide the screen into invisible tap regions for intuitive page turns.",
            checked = tapToTurn,
            onCheckedChange = viewModel::setTapToTurn,
            showDivider = true
        )
        SettingsItem(
            label = "Long Press Menu",
            description = "Access reader controls and overlays with a long press instead of a single tap.",
            checked = longPressMenu,
            onCheckedChange = viewModel::setLongPressMenu,
            showDivider = false
        )
    }
}

@Composable
fun RowScope.NavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) ElectricIndigo else Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (active) ElectricIndigo else Color.White.copy(alpha = 0.4f),
            maxLines = 1
        )
    }
}

