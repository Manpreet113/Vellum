package com.reader.vellum.ui.screens.reader

import android.app.Activity
import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.reader.vellum.ui.components.GlassmorphicSurface
import com.reader.vellum.ui.components.indigoGlow
import com.reader.vellum.ui.theme.ElectricIndigo
import com.reader.vellum.ui.theme.InkBlack
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
            .background(InkBlack)
    ) {
        when (val state = uiState) {
            is ReaderUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ElectricIndigo)
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
                    Button(
                        onClick = onBack, 
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                        shape = CircleShape
                    ) { Text("GO BACK", fontWeight = FontWeight.Bold) }
                }
            }
            is ReaderUiState.Success -> {
                // Content
                Box(modifier = Modifier.fillMaxSize()) {
                    if (state.book.format == "epub") {
                        EpubEngine(state, viewModel, { showUi = !showUi }, { currentPage = it }, isMangaMode, isVolumeKeys)
                    } else {
                        PaginatedEngine(state, viewModel, { showUi = !showUi }, isMangaMode, isTapToTurn, isVolumeKeys) { currentPage = it }
                    }
                }

                // Floating Vertical Progress (Neo-Reader Spec)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .align(Alignment.CenterEnd)
                        .padding(vertical = 100.dp, horizontal = 1.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    val progress = if (state.pages.isNotEmpty()) (currentPage.toFloat() / (state.pages.size - 1).coerceAtLeast(1)) else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(progress)
                            .background(ElectricIndigo.copy(alpha = 0.5f), CircleShape)
                            .indigoGlow(alpha = 0.2f, blurRadius = 8.dp)
                    )
                }

                // Floating Top Dock
                AnimatedVisibility(
                    visible = showUi,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .statusBarsPadding()
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f)) // Darker background for contrast
                                .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                .indigoGlow(alpha = 0.15f, borderRadius = 40.dp)
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = state.book.title.uppercase(),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        letterSpacing = 1.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "PAGE ${currentPage + 1} OF ${state.pages.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.7f), // Increased opacity
                                        letterSpacing = 1.sp
                                    )
                                }
                                IconButton(onClick = { showReaderSettings = true }) {
                                    Icon(Icons.Default.Tune, "Settings", tint = Color.White)
                                }
                            }
                        }
                    }
                }

                // Bottom Floating Scrubber
                AnimatedVisibility(
                    visible = showUi,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f)) // Darker background
                                .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                .indigoGlow(alpha = 0.1f, borderRadius = 40.dp)
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Slider(
                                value = currentPage.toFloat(),
                                onValueChange = { /* handled via engines internally */ },
                                valueRange = 0f..(state.pages.size - 1).toFloat().coerceAtLeast(0f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = ElectricIndigo,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                )
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
    val isAdaptiveChroma by viewModel.adaptiveChroma.collectAsState(true)
    val isTapToTurn by viewModel.tapToTurn.collectAsState(true)
    val isVolumeKeys by viewModel.volumeKeys.collectAsState(false)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F0F0F),
        modifier = Modifier.border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp)),
        title = { 
            Text(
                "READER CONFIG", 
                style = MaterialTheme.typography.headlineSmall, 
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                SettingsToggle("MANGA MODE (RTL)", isMangaMode, viewModel::setMangaMode)
                SettingsToggle("ADAPTIVE CHROMATICITY", isAdaptiveChroma, viewModel::setAdaptiveChroma)
                SettingsToggle("VOLUME NAVIGATION", isVolumeKeys, viewModel::setVolumeKeys)
                SettingsToggle("TAP ZONE NAVIGATION", isTapToTurn, viewModel::setTapToTurn)
            }
        },
        confirmButton = { 
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                shape = CircleShape
            ) { 
                Text("DONE", fontWeight = FontWeight.Bold) 
            } 
        }
    )
}

@Composable
fun EpubEngine(
    state: ReaderUiState.Success,
    viewModel: ReaderViewModel,
    onToggleUi: () -> Unit,
    onPageChanged: (Int) -> Unit,
    isMangaMode: Boolean,
    isVolumeKeys: Boolean
) {
    val pagerState = rememberPagerState(initialPage = state.initialPage, pageCount = { state.pages.size })
    val bgHex = "#0A0A0A" // Neo Ink Black
    val textHex = "#FFFFFF" // Neo Pure White
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        viewModel.updateProgress(pagerState.currentPage)
        onPageChanged(pagerState.currentPage)
    }

    // Handle Volume Keys
    LaunchedEffect(isVolumeKeys, isMangaMode) {
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

    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
        val request = state.pages[pageIndex] as? com.reader.vellum.ui.screens.reader.EpubPageRequest
        if (request != null) {
            val html by produceState<String?>(initialValue = null, request.uriString, request.chapterPath) {
                value = viewModel.getEpubChapter(request.uriString, request.chapterPath)
            }

            if (html == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ElectricIndigo)
                }
            } else {
                // Newsreader font injection
                val styledHtml = """
                    <html>
                    <head>
                        <link rel="preconnect" href="https://fonts.googleapis.com">
                        <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                        <link href="https://fonts.googleapis.com/css2?family=Newsreader:opsz,wght@6..72,400;6..72,500&display=swap" rel="stylesheet">
                        <style>
                            @font-face {
                                font-family: 'Newsreader';
                                font-style: normal;
                                font-weight: 400;
                                font-display: swap;
                                src: url(https://fonts.gstatic.com/s/newsreader/v31/f0X6028_9o95f0j0.woff2) format('woff2');
                            }
                            body {
                                background: $bgHex;
                                color: $textHex;
                                padding: 48px 32px;
                                font-family: 'Newsreader', serif;
                                font-size: 22px;
                                line-height: 1.7;
                                text-align: justify;
                            }
                            img { max-width: 100%; height: auto; border-radius: 12px; border: 1px solid rgba(255,255,255,0.1); }
                            h1, h2, h3 { font-weight: 800; letter-spacing: -0.02em; }
                        </style>
                    </head>
                    <body>$html</body>
                    </html>
                """.trimIndent()
                
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
    LaunchedEffect(isVolumeKeys, isMangaMode) {
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
        modifier = Modifier.fillMaxSize().background(Color.Black),
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
