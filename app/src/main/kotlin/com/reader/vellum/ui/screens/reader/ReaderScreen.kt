package com.reader.vellum.ui.screens.reader

import android.app.Activity
import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.reader.vellum.util.HardwareEvent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    id: String,
    viewModel: ReaderViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isMangaMode by viewModel.mangaMode.collectAsState(false)
    val isTapToTurn by viewModel.tapToTurn.collectAsState(true)
    val isVolumeKeys by viewModel.volumeKeys.collectAsState(false)
    val isAdaptiveChroma by viewModel.adaptiveChroma.collectAsState(true)

    var showUi by rememberSaveable { mutableStateOf(false) }
    var showReaderSettings by remember { mutableStateOf(false) }
    var currentPage by rememberSaveable { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // Immersive Mode
    val window = (context as? Activity)?.window
    if (window != null) {
        val controller = remember { WindowInsetsControllerCompat(window, window.decorView) }
        LaunchedEffect(showUi) {
            if (showUi) {
                controller.show(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    LaunchedEffect(id) {
        val currentState = viewModel.uiState.value
        if (currentState !is ReaderUiState.Success || currentState.book.id != id) {
            viewModel.loadBook(id)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val state = uiState) {
            is ReaderUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is ReaderUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Error, null, tint = Color.White, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(text = state.message, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onBack) { Text("Go Back") }
                }
            }
            is ReaderUiState.Success -> {
                val accentColor = if (isAdaptiveChroma) state.accentColor ?: MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                
                // Background
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)))

                // Engine selection
                Box(modifier = Modifier.fillMaxSize()) {
                    if (state.book.format == "epub") {
                        EpubEngine(state, viewModel, { showUi = !showUi }) { currentPage = it }
                    } else {
                        PaginatedEngine(state, viewModel, { showUi = !showUi }, isMangaMode, isTapToTurn, isVolumeKeys) { currentPage = it }
                    }
                }

                // Overlays
                AnimatedVisibility(
                    visible = showUi,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                text = state.book.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showReaderSettings = true }) {
                                Icon(Icons.Default.Tune, "Settings", tint = accentColor)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    )
                }

                AnimatedVisibility(
                    visible = showUi,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        tonalElevation = 8.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Page ${currentPage + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                                Text("${state.pages.size} pages", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Slider(
                                value = currentPage.toFloat(),
                                onValueChange = { /* Engines should handle jumps */ },
                                valueRange = 0f..(state.pages.size - 1).toFloat().coerceAtLeast(0f),
                                colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                            )
                        }
                    }
                }

                if (showReaderSettings) {
                    ReaderSettingsDialog(viewModel) { showReaderSettings = false }
                }
            }
        }
    }
}

@Composable
fun ReaderSettingsDialog(viewModel: ReaderViewModel, onDismiss: () -> Unit) {
    val isMangaMode by viewModel.mangaMode.collectAsState(false)
    val isTapToTurn by viewModel.tapToTurn.collectAsState(true)
    val isAdaptiveChroma by viewModel.adaptiveChroma.collectAsState(true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SettingsToggle("Manga Mode (RTL)", isMangaMode, viewModel::setMangaMode)
                SettingsToggle("Adaptive Colors", isAdaptiveChroma, viewModel::setAdaptiveChroma)
                SettingsToggle("Tap Navigation", isTapToTurn, viewModel::setTapToTurn)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
fun EpubEngine(
    state: ReaderUiState.Success,
    viewModel: ReaderViewModel,
    onToggleUi: () -> Unit,
    onPageChanged: (Int) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = state.initialPage, pageCount = { state.pages.size })
    val bgHex = String.format("#%06X", (0xFFFFFF and MaterialTheme.colorScheme.surface.toArgb()))
    val textHex = String.format("#%06X", (0xFFFFFF and MaterialTheme.colorScheme.onSurface.toArgb()))

    LaunchedEffect(pagerState.currentPage) {
        viewModel.updateProgress(pagerState.currentPage)
        onPageChanged(pagerState.currentPage)
    }

    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
        val request = state.pages[pageIndex] as? com.reader.vellum.ui.screens.reader.EpubPageRequest
        if (request != null) {
            val html by produceState<String?>(initialValue = null, request.uriString, request.chapterPath) {
                value = viewModel.getEpubChapter(request.uriString, request.chapterPath)
            }

            if (html == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                val styledHtml = "<html><body style='background:$bgHex; color:$textHex; padding:20px; font-size:18px; line-height:1.6;'>$html</body></html>"
                AndroidView(
                    factory = { WebView(it).apply { setBackgroundColor(android.graphics.Color.TRANSPARENT) } },
                    update = { it.loadDataWithBaseURL(null, styledHtml, "text/html", "UTF-8", null) },
                    modifier = Modifier.fillMaxSize().clickable { onToggleUi() }
                )
            }
        }
    }
}

@Composable
fun PaginatedEngine(
    state: ReaderUiState.Success,
    viewModel: ReaderViewModel,
    onToggleUi: () -> Unit,
    isMangaMode: Boolean,
    isTapToTurn: Boolean,
    isVolumeKeys: Boolean,
    onPageChanged: (Int) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = state.initialPage, pageCount = { state.pages.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        viewModel.updateProgress(pagerState.currentPage)
        onPageChanged(pagerState.currentPage)
    }

    // Handle Volume Keys
    LaunchedEffect(Unit) {
        viewModel.hardwareEvents.collect { event ->
            if (isVolumeKeys) {
                val target = if (isMangaMode) {
                    if (event == HardwareEvent.VOLUME_UP) pagerState.currentPage + 1 else pagerState.currentPage - 1
                } else {
                    if (event == HardwareEvent.VOLUME_UP) pagerState.currentPage - 1 else pagerState.currentPage + 1
                }
                pagerState.animateScrollToPage(target.coerceIn(0, state.pages.size - 1))
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        reverseLayout = isMangaMode,
        pageSpacing = 0.dp,
        beyondViewportPageCount = 1
    ) { pageIndex ->
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(state.pages[pageIndex])
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggleUi() },
                contentScale = ContentScale.Fit
            )
            
            if (isTapToTurn) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        scope.launch {
                            val target = if (isMangaMode) pagerState.currentPage + 1 else pagerState.currentPage - 1
                            if (target in 0 until state.pages.size) pagerState.animateScrollToPage(target)
                        }
                    })
                    Spacer(modifier = Modifier.weight(2f))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        scope.launch {
                            val target = if (isMangaMode) pagerState.currentPage - 1 else pagerState.currentPage + 1
                            if (target in 0 until state.pages.size) pagerState.animateScrollToPage(target)
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
