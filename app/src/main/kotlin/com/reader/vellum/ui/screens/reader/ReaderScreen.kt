package com.reader.vellum.ui.screens.reader

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.reader.vellum.ui.components.ShimmerBox
import com.reader.vellum.ui.components.indigoGlow
import com.reader.vellum.ui.theme.ElectricIndigo
import com.reader.vellum.ui.theme.InkBlack
import com.reader.vellum.ui.theme.SurfaceElevated1
import com.reader.vellum.util.EpubManifest
import com.reader.vellum.util.EpubStyle
import com.reader.vellum.util.EpubStyleGenerator
import com.reader.vellum.util.EpubParser
import com.reader.vellum.util.HardwareEvent
import java.io.ByteArrayInputStream
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.WidthFull
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.automirrored.filled.FormatTextdirectionLToR
import androidx.compose.material.icons.automirrored.filled.FormatTextdirectionRToL


private data class ReaderTocEntry(
    val title: String,
    val chapterIndex: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    id: String,
    viewModel: ReaderViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val epubManifest by viewModel.epubManifest.collectAsState()
    val isMangaMode by viewModel.mangaMode.collectAsState(false)
    val isTapToTurn by viewModel.tapToTurn.collectAsState(true)
    val isVolumeKeys by viewModel.volumeKeys.collectAsState(false)
    val epubStyle by viewModel.epubStyle.collectAsState()
    val isAdaptiveChroma by viewModel.adaptiveChroma.collectAsState(true)

    var showUi by rememberSaveable { mutableStateOf(false) }
    var showReaderSettings by remember { mutableStateOf(false) }
    var showChapterSheet by remember { mutableStateOf(false) }
    var isFitWidth by rememberSaveable { mutableStateOf(false) }
    var isBookmarked by rememberSaveable { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var currentPage by rememberSaveable { mutableIntStateOf(0) }
    var progressFraction by rememberSaveable { mutableFloatStateOf(0f) }
    var progressLabel by rememberSaveable { mutableStateOf("") }
    var scrubberPosition by rememberSaveable { mutableFloatStateOf(0f) }
    var scrubberDraft by rememberSaveable { mutableFloatStateOf(0f) }
    var scrubberDragging by remember { mutableStateOf(false) }
    var pendingSeekTarget by remember { mutableStateOf<Int?>(null) }
    var epubCanGoBack by remember { mutableStateOf(false) }
    var epubCanGoForward by remember { mutableStateOf(false) }
    var epubHistoryBackRequest by remember { mutableIntStateOf(0) }
    var epubHistoryForwardRequest by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

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
        DisposableEffect(Unit) {
            onDispose {
                controller.show(WindowInsetsCompat.Type.systemBars())
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
                // 2f: Skeleton loading state with blurred cover if available
                val lastBook = (uiState as? ReaderUiState.Success)?.book
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (lastBook?.coverPath != null) {
                        AsyncImage(
                            model = lastBook.coverPath,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().blur(32.dp),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.7f))
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ShimmerBox(
                                modifier = Modifier
                                    .size(120.dp, 180.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            ShimmerBox(
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            ShimmerBox(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        }
                    } else {
                        CircularProgressIndicator(color = ElectricIndigo)
                    }
                }
            }

            is ReaderUiState.Error -> {
                // 2g: Friendlier error state with on-brand icon
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        null,
                        tint = ElectricIndigo.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "Couldn't open this book",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.45f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                        shape = CircleShape
                    ) {
                        Text("GO BACK", fontWeight = FontWeight.Bold)
                    }
                }
            }

            is ReaderUiState.Success -> {
                val accentColor = ElectricIndigo
                val isEpub = state.book.format == "epub"
                val maxPage = state.pages.lastIndex.coerceAtLeast(0)
                val tocEntries = remember(state.pages, epubManifest) {
                    if (!isEpub || epubManifest == null) {
                        emptyList()
                    } else {
                        buildTocEntries(epubManifest!!, state.pages.filterIsInstance<EpubPageRequest>())
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (isEpub) {
                        EpubEngine(
                            state = state,
                            viewModel = viewModel,
                            onToggleUi = { showUi = !showUi },
                            onReadingStateChanged = { chapterIndex, overallProgress, label ->
                                currentPage = chapterIndex
                                progressFraction = overallProgress
                                progressLabel = label
                                if (!scrubberDragging) {
                                    scrubberPosition = chapterIndex.toFloat()
                                    scrubberDraft = scrubberPosition
                                }
                            },
                            historyBackRequest = epubHistoryBackRequest,
                            historyForwardRequest = epubHistoryForwardRequest,
                            onHistoryStateChanged = { canGoBack, canGoForward ->
                                epubCanGoBack = canGoBack
                                epubCanGoForward = canGoForward
                            },
                            seekRequest = pendingSeekTarget,
                            onSeekConsumed = { pendingSeekTarget = null },
                            isMangaMode = isMangaMode,
                            isVolumeKeys = isVolumeKeys
                        )
                    } else {
                        PaginatedEngine(
                            state = state,
                            viewModel = viewModel,
                            onToggleUi = { showUi = !showUi },
                            isMangaMode = isMangaMode,
                            isTapToTurn = isTapToTurn,
                            isVolumeKeys = isVolumeKeys,
                            seekRequest = pendingSeekTarget,
                            onSeekConsumed = { pendingSeekTarget = null },
                            contentScale = if (isFitWidth) ContentScale.FillWidth else ContentScale.Fit
                        ) { pageIndex ->
                            currentPage = pageIndex
                            progressFraction = if (maxPage > 0) {
                                pageIndex.toFloat() / maxPage.toFloat()
                            } else {
                                1f
                            }
                            progressLabel = "PAGE ${pageIndex + 1} OF ${state.pages.size}"
                            if (!scrubberDragging) {
                                scrubberPosition = pageIndex.toFloat()
                                scrubberDraft = scrubberPosition
                            }
                        }
                    }
                }

                // 2d: Right-edge strip — only visible during immersive reading (inverse of showUi)
                AnimatedVisibility(
                    visible = !showUi,
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(6.dp)
                            .padding(vertical = 100.dp, horizontal = 1.dp)
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(progressFraction.coerceIn(0f, 1f))
                                .background(accentColor.copy(alpha = 0.5f), CircleShape)
                                .indigoGlow(color = accentColor, alpha = 0.2f, blurRadius = 8.dp)
                        )
                    }
                }

                // Unified overlay: Combine top and bottom overlays with a semi-transparent dark backdrop
                AnimatedVisibility(
                    visible = showUi,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f))
                    ) {
                        // Gesture area for tap to close
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { showUi = false }
                        )

                        // Top App Bar
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .animateEnterExit(
                                    enter = slideInVertically { -it },
                                    exit = slideOutVertically { -it }
                                )
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White
                                    )
                                }

                                val titleText = if (isEpub) {
                                    progressLabel.ifBlank { state.book.title }
                                } else {
                                    state.book.title
                                }
                                Text(
                                    text = titleText,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 18.sp
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 16.dp)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isEpub && tocEntries.isNotEmpty()) {
                                        IconButton(
                                            onClick = { showChapterSheet = true },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                                                contentDescription = "Chapters",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { isBookmarked = !isBookmarked },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "Bookmark",
                                            tint = if (isBookmarked) Color(0xFFC0C1FF) else Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Controls Panel
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .animateEnterExit(
                                    enter = slideInVertically { it },
                                    exit = slideOutVertically { it }
                                )
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    enabled = false
                                ) {} // prevent clicks propagating to gesture area
                                .background(
                                    color = Color(0xFF1A1C1C).copy(alpha = 0.95f),
                                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF464554),
                                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                )
                                .navigationBarsPadding()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                // Scrubber Section
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 16.dp)
                                ) {
                                    val prevLabel = if (isEpub) {
                                        if (currentPage > 0) "CH $currentPage" else ""
                                    } else {
                                        if (currentPage > 0) "PG $currentPage" else ""
                                    }
                                    val centerLabel = if (isEpub) {
                                        progressLabel.ifBlank { "CHAPTER ${currentPage + 1} OF ${state.pages.size}" }
                                    } else {
                                        "PAGE ${currentPage + 1} OF ${state.pages.size}"
                                    }
                                    val nextLabel = if (isEpub) {
                                        if (currentPage < maxPage) "CH ${currentPage + 2}" else ""
                                    } else {
                                        if (currentPage < maxPage) "PG ${currentPage + 2}" else ""
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = prevLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFC7C4D7)
                                        )
                                        Text(
                                            text = centerLabel.uppercase(Locale.ROOT),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 2.sp
                                            ),
                                            color = Color(0xFFC0C1FF)
                                        )
                                        Text(
                                            text = nextLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFC7C4D7)
                                        )
                                    }

                                    Spacer(Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val target = (currentPage - 1).coerceAtLeast(0)
                                                pendingSeekTarget = target
                                                scrubberPosition = target.toFloat()
                                            },
                                            enabled = currentPage > 0,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SkipPrevious,
                                                contentDescription = "Previous",
                                                tint = if (currentPage > 0) Color(0xFFC7C4D7) else Color(0xFF464554)
                                            )
                                        }

                                        Slider(
                                            value = if (scrubberDragging) scrubberDraft else scrubberPosition,
                                            onValueChange = {
                                                scrubberDragging = true
                                                scrubberDraft = it.coerceIn(0f, maxPage.toFloat())
                                            },
                                            onValueChangeFinished = {
                                                pendingSeekTarget = scrubberDraft.roundToInt().coerceIn(0, maxPage)
                                                scrubberPosition = pendingSeekTarget?.toFloat() ?: scrubberPosition
                                                scrubberDragging = false
                                            },
                                            valueRange = 0f..maxPage.toFloat().coerceAtLeast(0f),
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color.White,
                                                activeTrackColor = Color(0xFFC0C1FF),
                                                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )

                                        IconButton(
                                            onClick = {
                                                val target = (currentPage + 1).coerceAtMost(maxPage)
                                                pendingSeekTarget = target
                                                scrubberPosition = target.toFloat()
                                            },
                                            enabled = currentPage < maxPage,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SkipNext,
                                                contentDescription = "Next",
                                                tint = if (currentPage < maxPage) Color(0xFFC7C4D7) else Color(0xFF464554)
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = Color(0xFF464554), thickness = 0.5.dp)

                                // Scrollable middle options panel
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    if (isEpub) {
                                        // Typography Section
                                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(3.dp)
                                                        .height(14.dp)
                                                        .background(Color(0xFFC0C1FF), CircleShape)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    "TYPOGRAPHY",
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFFC0C1FF)
                                                )
                                            }

                                            ControlRow("FONT SIZE") {
                                                Slider(
                                                    value = epubStyle.fontSize,
                                                    onValueChange = { viewModel.setEpubFontSize(it) },
                                                    valueRange = 12f..32f,
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = Color.White,
                                                        activeTrackColor = Color(0xFFC0C1FF)
                                                    )
                                                )
                                            }

                                            ControlRow("LINE HEIGHT") {
                                                Slider(
                                                    value = epubStyle.lineHeight,
                                                    onValueChange = { viewModel.setEpubLineHeight(it) },
                                                    valueRange = 1.0f..2.3f,
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = Color.White,
                                                        activeTrackColor = Color(0xFFC0C1FF)
                                                    )
                                                )
                                            }

                                            ControlRow("PAGE PADDING") {
                                                Slider(
                                                    value = epubStyle.margin.toFloat(),
                                                    onValueChange = { viewModel.setEpubMargin(it.roundToInt()) },
                                                    valueRange = 12f..40f,
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = Color.White,
                                                        activeTrackColor = Color(0xFFC0C1FF)
                                                    )
                                                )
                                            }

                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                FontButton("Serif", "serif", epubStyle.fontFamily == "serif", Color(0xFFC0C1FF)) {
                                                    viewModel.setEpubFontFamily("serif")
                                                }
                                                FontButton("Sans", "sans-serif", epubStyle.fontFamily == "sans-serif", Color(0xFFC0C1FF)) {
                                                    viewModel.setEpubFontFamily("sans-serif")
                                                }
                                                FontButton("Mono", "monospace", epubStyle.fontFamily == "monospace", Color(0xFFC0C1FF)) {
                                                    viewModel.setEpubFontFamily("monospace")
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                                        // Appearance Section
                                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(3.dp)
                                                        .height(14.dp)
                                                        .background(Color(0xFFC0C1FF), CircleShape)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    "APPEARANCE",
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFFC0C1FF)
                                                )
                                            }
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                ThemeCircle("Dark", Color(0xFF1A1A2E), epubStyle.theme == "dark", Color(0xFFC0C1FF)) {
                                                    viewModel.setEpubTheme("dark")
                                                }
                                                ThemeCircle("Sepia", Color(0xFFF4ECD8), epubStyle.theme == "sepia", Color(0xFFC0C1FF)) {
                                                    viewModel.setEpubTheme("sepia")
                                                }
                                                ThemeCircle("Light", Color.White, epubStyle.theme == "light", Color(0xFFC0C1FF)) {
                                                    viewModel.setEpubTheme("light")
                                                }
                                                ThemeCircle("Night", Color(0xFF000000), epubStyle.theme == "night", Color(0xFFC0C1FF)) {
                                                    viewModel.setEpubTheme("night")
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                    }

                                    // Display / Navigation Section
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .width(3.dp)
                                                    .height(14.dp)
                                                    .background(Color(0xFFC0C1FF), CircleShape)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "READING DIRECTION",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFFC0C1FF)
                                            )
                                        }

                                        // 3 Direction Toggles
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                DirectionToggle(
                                                    icon = Icons.AutoMirrored.Filled.FormatTextdirectionRToL,
                                                    label = "R to L",
                                                    selected = isMangaMode,
                                                    onClick = { viewModel.setMangaMode(true) }
                                                )
                                            }
                                            Box(modifier = Modifier.weight(1f)) {
                                                DirectionToggle(
                                                    icon = Icons.AutoMirrored.Filled.FormatTextdirectionLToR,
                                                    label = "L to R",
                                                    selected = !isMangaMode,
                                                    onClick = { viewModel.setMangaMode(false) }
                                                )
                                            }
                                            Box(modifier = Modifier.weight(1f)) {
                                                DirectionToggle(
                                                    icon = Icons.Default.SwapVert,
                                                    label = "Vertical",
                                                    selected = false,
                                                    onClick = {
                                                        Toast.makeText(context, "Vertical scrolling is not supported for this format", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            }
                                        }

                                        // Fit Options (only if CBZ/PDF)
                                        if (!isEpub) {
                                            Spacer(Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .clickable { isFitWidth = true }
                                                        .padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.WidthFull,
                                                        contentDescription = "Fit Width",
                                                        tint = if (isFitWidth) Color(0xFFC0C1FF) else Color(0xFFC7C4D7),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Text(
                                                        text = "FIT WIDTH",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = if (isFitWidth) Color(0xFFC0C1FF) else Color(0xFFC7C4D7)
                                                    )
                                                }
                                                Spacer(Modifier.width(24.dp))
                                                Row(
                                                    modifier = Modifier
                                                        .clickable { isFitWidth = false }
                                                        .padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Height,
                                                        contentDescription = "Fit Height",
                                                        tint = if (!isFitWidth) Color(0xFFC0C1FF) else Color(0xFFC7C4D7),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Text(
                                                        text = "FIT HEIGHT",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = if (!isFitWidth) Color(0xFFC0C1FF) else Color(0xFFC7C4D7)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                                    // Other settings
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .width(3.dp)
                                                    .height(14.dp)
                                                    .background(Color(0xFFC0C1FF), CircleShape)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "NAVIGATION SETTINGS",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFFC0C1FF)
                                            )
                                        }
                                        if (!isEpub) {
                                            SettingsToggleNeo("TAP TO TURN", isTapToTurn, Color(0xFFC0C1FF), viewModel::setTapToTurn)
                                        }
                                        SettingsToggleNeo("VOLUME KEYS", isVolumeKeys, Color(0xFFC0C1FF), viewModel::setVolumeKeys)
                                    }
                                }

                                HorizontalDivider(color = Color(0xFF464554), thickness = 0.5.dp)

                                // Book Info Footer
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 48.dp, height = 64.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White.copy(alpha = 0.05f))
                                                .border(1.dp, Color(0xFF464554), RoundedCornerShape(8.dp))
                                        ) {
                                            if (state.book.coverPath != null) {
                                                AsyncImage(
                                                    model = state.book.coverPath,
                                                    contentDescription = "Cover",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }

                                        Spacer(Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = state.book.title,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = state.book.author ?: "Unknown Author",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFC7C4D7),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(16.dp))

                                    Button(
                                        onClick = { showDetailsDialog = true },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC0C1FF)),
                                        border = BorderStroke(1.dp, Color(0xFFC0C1FF)),
                                        shape = RoundedCornerShape(50),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "DETAILS",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (showDetailsDialog) {
                    AlertDialog(
                        onDismissRequest = { showDetailsDialog = false },
                        title = {
                            Text(
                                text = "Book Details",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Title: ${state.book.title}",
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "Author: ${state.book.author ?: "Unknown"}",
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "Format: ${state.book.format.uppercase(Locale.ROOT)}",
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "File Path: ${state.book.filePath}",
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { showDetailsDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0C1FF), contentColor = Color(0xFF1000A9))
                            ) {
                                Text("CLOSE", fontWeight = FontWeight.Bold)
                            }
                        },
                        containerColor = Color(0xFF1E2020),
                        textContentColor = Color.White,
                        titleContentColor = Color.White
                    )
                }

                if (showChapterSheet && tocEntries.isNotEmpty()) {
                    EpubTocSheet(
                        currentChapterIndex = currentPage,
                        entries = tocEntries,
                        accentColor = accentColor,
                        onDismiss = { showChapterSheet = false },
                        onChapterSelected = { index ->
                            pendingSeekTarget = index
                            showChapterSheet = false
                            showUi = false
                        }
                    )
                }
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpubTocSheet(
    currentChapterIndex: Int,
    entries: List<ReaderTocEntry>,
    accentColor: Color,
    onDismiss: () -> Unit,
    onChapterSelected: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceElevated1,  // 3c: token
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.2f)) },
        tonalElevation = 8.dp,
        scrimColor = Color.Black.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "CONTENTS",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            // 2k: LazyColumn for scrollable TOC (handles 50+ chapters)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(entries) { entry ->
                    val isCurrent = entry.chapterIndex == currentChapterIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (isCurrent) accentColor.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isCurrent) accentColor.copy(alpha = 0.45f) else Color.Transparent,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { onChapterSelected(entry.chapterIndex) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Chapter index counter
                        Text(
                            text = String.format("%02d", entry.chapterIndex + 1),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrent) accentColor else Color.White.copy(alpha = 0.25f),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(32.dp)
                        )
                        Text(
                            text = entry.title,
                            color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.82f),
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ControlRow(label: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

@Composable
fun RowScope.FontButton(
    label: String,
    fontFamily: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    // 2i: Font button with "Aa" preview in the actual CSS font stack
    Box(
        modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accentColor else Color.White.copy(alpha = 0.05f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                "Aa",
                fontWeight = FontWeight.Bold,
                fontStyle = if (fontFamily == "serif") FontStyle.Italic else FontStyle.Normal,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
                fontSize = 16.sp
            )
            Text(
                label,
                fontWeight = FontWeight.Medium,
                color = if (selected) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.35f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun RowScope.ThemeCircle(
    label: String,
    color: Color,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    // 2h: Larger 56dp tap target for theme circles
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color)
                .border(2.5.dp, if (selected) accentColor else Color.White.copy(alpha = 0.1f), CircleShape)
                .clickable { onClick() }
        )
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.45f),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun SettingsToggleNeo(
    label: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Medium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
fun DirectionToggle(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFFC0C1FF).copy(alpha = 0.1f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) Color(0xFFC0C1FF) else Color(0xFF464554),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color(0xFFC0C1FF) else Color(0xFFC7C4D7),
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            ),
            color = if (selected) Color(0xFFC0C1FF) else Color(0xFFC7C4D7),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EpubEngine(
    state: ReaderUiState.Success,
    viewModel: ReaderViewModel,
    onToggleUi: () -> Unit,
    onReadingStateChanged: (chapterIndex: Int, overallProgress: Float, label: String) -> Unit,
    historyBackRequest: Int,
    historyForwardRequest: Int,
    onHistoryStateChanged: (canGoBack: Boolean, canGoForward: Boolean) -> Unit,
    seekRequest: Int?,
    onSeekConsumed: () -> Unit,
    isMangaMode: Boolean,
    isVolumeKeys: Boolean
) {
    val epubManifest by viewModel.epubManifest.collectAsState()
    val epubLocator by viewModel.epubLocator.collectAsState()
    val epubStyle by viewModel.epubStyle.collectAsState()
    val accentColor = state.accentColor ?: ElectricIndigo
    val requests = remember(state.pages) { state.pages.filterIsInstance<EpubPageRequest>() }
    val controller = remember(state.book.id, requests, epubManifest) {
        EpubReaderController(
            bookId = state.book.id,
            manifest = epubManifest,
            requests = requests,
            initialChapterIndex = state.initialPage,
            initialLocator = epubLocator
        )
    }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentChapterPath by remember { mutableStateOf(requests.getOrNull(state.initialPage)?.chapterPath.orEmpty()) }

    val currentRequest = controller.currentRequest
    if (currentRequest == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Invalid Chapter", color = Color.White)
        }
        return
    }

    LaunchedEffect(controller.currentChapterIndex, controller.chapterProgress, controller.readingLabel, controller.overallProgress) {
        viewModel.updateEpubLocation(controller.currentChapterIndex, controller.chapterProgress)
        onReadingStateChanged(
            controller.currentChapterIndex,
            controller.overallProgress,
            controller.readingLabel
        )
    }

    LaunchedEffect(epubLocator.chapterIndex, epubLocator.chapterProgress, state.book.id) {
        controller.onExternalLocator(epubLocator)
    }

    LaunchedEffect(seekRequest, requests.size) {
        val target = seekRequest ?: return@LaunchedEffect
        controller.seekTo(target)
        onSeekConsumed()
    }

    LaunchedEffect(historyBackRequest) {
        if (historyBackRequest > 0) {
            controller.goBackInHistory()
        }
    }

    LaunchedEffect(historyForwardRequest) {
        if (historyForwardRequest > 0) {
            controller.goForwardInHistory()
        }
    }

    LaunchedEffect(isVolumeKeys, isMangaMode, webViewRef) {
        if (webViewRef == null) return@LaunchedEffect
        viewModel.hardwareEvents.collect { event ->
            if (!isVolumeKeys) return@collect
            val isUp = event == HardwareEvent.VOLUME_UP
            val goNext = if (isMangaMode) isUp else !isUp
            webViewRef?.evaluateJavascript(if (goNext) "pageRight()" else "pageLeft()", null)
        }
    }

    LaunchedEffect(currentRequest.chapterPath) {
        currentChapterPath = currentRequest.chapterPath
    }

    LaunchedEffect(controller.canGoBack, controller.canGoForward) {
        onHistoryStateChanged(controller.canGoBack, controller.canGoForward)
    }

    LaunchedEffect(controller.currentChapterIndex, currentRequest.uriString, requests) {
        val chapterPaths = buildList {
            requests.getOrNull(controller.currentChapterIndex - 1)?.chapterPath?.let(::add)
            requests.getOrNull(controller.currentChapterIndex + 1)?.chapterPath?.let(::add)
            requests.getOrNull(controller.currentChapterIndex + 2)?.chapterPath?.let(::add)
        }
        viewModel.prefetchEpubChapters(currentRequest.uriString, chapterPaths)
    }

    val html by produceState<String?>(initialValue = null, currentRequest.uriString, currentRequest.chapterPath) {
        value = viewModel.getEpubChapter(currentRequest.uriString, currentRequest.chapterPath)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (html == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accentColor)
            }
        } else {
            val documentHtml = remember(html, epubStyle, accentColor, isMangaMode) {
                buildStyledDocument(html.orEmpty(), epubStyle, accentColor.toArgb(), isMangaMode)
            }

            AndroidView(
                factory = { ctx ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        WebView.setWebContentsDebuggingEnabled(true)
                    }
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        isVerticalScrollBarEnabled = false
                        isHorizontalScrollBarEnabled = false
                        overScrollMode = android.view.View.OVER_SCROLL_NEVER

                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun nextChapter() {
                                post { controller.nextChapter() }
                            }

                            @JavascriptInterface
                            fun prevChapter() {
                                post { controller.previousChapter() }
                            }

                            @JavascriptInterface
                            fun toggleUi() {
                                post { onToggleUi() }
                            }

                            @JavascriptInterface
                            fun onProgressUpdate(progress: Float) {
                                post { controller.onProgressUpdate(progress) }
                            }

                            @JavascriptInterface
                            fun openLink(href: String?) {
                                val targetHref = href?.trim().orEmpty()
                                if (targetHref.isEmpty()) return
                                post {
                                    val target = controller.navigateToHref(currentChapterPath, targetHref) ?: return@post
                                    if (target.archivePath == currentChapterPath.substringBefore('#')) {
                                        val anchor = target.fragment ?: return@post
                                        webViewRef?.evaluateJavascript(
                                            "window.__vellumGoToAnchor(${anchor.jsQuoted()});",
                                            null
                                        )
                                    }
                                }
                            }

                            @JavascriptInterface
                            fun historyBack() {
                                post { controller.goBackInHistory() }
                            }

                            @JavascriptInterface
                            fun historyForward() {
                                post { controller.goForwardInHistory() }
                            }
                        }, "VellumBridge")

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val pendingAnchor = controller.consumePendingAnchor()
                                if (pendingAnchor != null) {
                                    view?.evaluateJavascript(
                                        "window.__vellumGoToAnchor(${pendingAnchor.jsQuoted()});",
                                        null
                                    )
                                } else {
                                    view?.evaluateJavascript(
                                        "window.__vellumRestore(${controller.pendingRestoreProgress.coerceIn(0f, 1f)});",
                                        null
                                    )
                                }
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val url = request?.url ?: return null
                                if (url.scheme != "epub") return super.shouldInterceptRequest(view, request)
                                val pathSegments = url.pathSegments
                                if (pathSegments.size < 2) return super.shouldInterceptRequest(view, request)

                                val bookUri = pathSegments.firstOrNull().orEmpty()
                                val archivePath = pathSegments.drop(1).joinToString("/")
                                if (bookUri.isBlank() || archivePath.isBlank()) {
                                    return super.shouldInterceptRequest(view, request)
                                }
                                return loadEpubResource(ctx, bookUri, archivePath)
                                    ?: super.shouldInterceptRequest(view, request)
                            }
                        }
                    }.also { webViewRef = it }
                },
                update = { webView ->
                    val baseUrl = buildEpubBaseUrl(currentRequest.uriString, currentRequest.chapterPath)
                    if (webView.url != baseUrl) {
                        webView.loadDataWithBaseURL(baseUrl, documentHtml, "text/html", "UTF-8", null)
                    } else {
                        webView.evaluateJavascript(
                            "window.__vellumRefreshTheme(${controller.pendingRestoreProgress.coerceIn(0f, 1f)});",
                            null
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
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
    seekRequest: Int?,
    onSeekConsumed: () -> Unit,
    contentScale: ContentScale = ContentScale.Fit,
    onPageChanged: (Int) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = state.initialPage, pageCount = { state.pages.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        viewModel.updateProgress(pagerState.currentPage)
        onPageChanged(pagerState.currentPage)
    }

    LaunchedEffect(seekRequest, state.pages.size) {
        val target = seekRequest ?: return@LaunchedEffect
        pagerState.animateScrollToPage(target.coerceIn(0, state.pages.lastIndex.coerceAtLeast(0)))
        onSeekConsumed()
    }

    LaunchedEffect(isVolumeKeys, isMangaMode) {
        viewModel.hardwareEvents.collect { event ->
            if (!isVolumeKeys) return@collect
            val target = if (isMangaMode) {
                if (event == HardwareEvent.VOLUME_UP) pagerState.currentPage + 1 else pagerState.currentPage - 1
            } else {
                if (event == HardwareEvent.VOLUME_UP) pagerState.currentPage - 1 else pagerState.currentPage + 1
            }
            pagerState.animateScrollToPage(target.coerceIn(0, state.pages.lastIndex.coerceAtLeast(0)))
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        reverseLayout = isMangaMode,
        beyondViewportPageCount = 1
    ) { pageIndex ->
        val scrollState = rememberScrollState()
        LaunchedEffect(pageIndex) {
            scrollState.scrollTo(0)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (contentScale == ContentScale.FillWidth) {
                            Modifier.verticalScroll(scrollState)
                        } else {
                            Modifier
                        }
                    )
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(state.pages[pageIndex])
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .then(
                            if (contentScale == ContentScale.FillWidth) {
                                Modifier.fillMaxWidth()
                            } else {
                                Modifier.fillMaxSize()
                            }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onToggleUi() },
                    contentScale = contentScale
                )
            }

            if (isTapToTurn) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                scope.launch {
                                    val target = if (isMangaMode) pagerState.currentPage + 1 else pagerState.currentPage - 1
                                    if (target in 0 until state.pages.size) {
                                        pagerState.animateScrollToPage(target)
                                    }
                                }
                            }
                    )
                    Spacer(modifier = Modifier.weight(2f))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                scope.launch {
                                    val target = if (isMangaMode) pagerState.currentPage - 1 else pagerState.currentPage + 1
                                    if (target in 0 until state.pages.size) {
                                        pagerState.animateScrollToPage(target)
                                    }
                                }
                            }
                    )
                }
            }
        }
    }
}

private fun buildTocEntries(manifest: EpubManifest, pages: List<EpubPageRequest>): List<ReaderTocEntry> {
    if (pages.isEmpty()) return emptyList()

    val chapterIndexByPath = pages.mapIndexed { index, page ->
        normalizeHref(page.chapterPath) to index
    }.toMap()

    val entries = manifest.toc.mapNotNull { tocItem ->
        val normalizedHref = normalizeHref(tocItem.href)
        val chapterIndex = chapterIndexByPath[normalizedHref]
            ?: chapterIndexByPath.entries.firstOrNull { (path, _) ->
                normalizedHref.startsWith(path) || path.startsWith(normalizedHref)
            }?.value

        val title = tocItem.title.trim().ifBlank {
            normalizedHref.substringAfterLast('/').substringBeforeLast('.').replace('-', ' ')
        }

        chapterIndex?.let { ReaderTocEntry(title = title, chapterIndex = it) }
    }

    return entries
        .distinctBy { it.chapterIndex }
        .ifEmpty {
            pages.mapIndexed { index, page ->
                ReaderTocEntry(resolveFallbackChapterTitle(page.chapterPath, index), index)
            }
        }
}

private fun normalizeHref(href: String): String = href.substringBefore('#').substringBefore('?')

private fun resolveFallbackChapterTitle(chapterPath: String, chapterIndex: Int): String {
    val base = chapterPath.substringAfterLast('/').substringBeforeLast('.')
    val cleaned = base.replace('-', ' ').replace('_', ' ').trim()
    return cleaned.ifBlank { "Chapter ${chapterIndex + 1}" }
}

private fun buildStyledDocument(rawHtml: String, style: EpubStyle, accentColor: Int, isMangaMode: Boolean): String {
    val bridgeScript = """
        <script>
            (function() {
                const isMangaMode = $isMangaMode;
                function rootEl() {
                    return document.scrollingElement || document.documentElement || document.body;
                }

                function maxScrollX() {
                    const root = rootEl();
                    return Math.max(0, root.scrollWidth - window.innerWidth);
                }

                function reportProgress() {
                    const total = maxScrollX();
                    const progress = total > 0 ? window.scrollX / total : 0;
                    if (window.VellumBridge) {
                        window.VellumBridge.onProgressUpdate(progress);
                    }
                }

                function snapTo(progress, immediate) {
                    const target = Math.max(0, Math.min(maxScrollX(), maxScrollX() * Math.max(0, Math.min(1, progress || 0))));
                    window.scrollTo({ left: target, top: 0, behavior: immediate ? 'auto' : 'smooth' });
                    window.setTimeout(reportProgress, immediate ? 40 : 260);
                }

                window.pageRight = function() {
                    const next = window.scrollX + window.innerWidth;
                    if (next >= maxScrollX() - 4) {
                        if (window.VellumBridge) window.VellumBridge.nextChapter();
                    } else {
                        window.scrollTo({ left: next, top: 0, behavior: 'smooth' });
                        window.setTimeout(reportProgress, 260);
                    }
                };

                window.pageLeft = function() {
                    const prev = window.scrollX - window.innerWidth;
                    if (prev <= 4) {
                        if (window.scrollX <= 4 && window.VellumBridge) {
                            window.VellumBridge.prevChapter();
                        } else {
                            window.scrollTo({ left: 0, top: 0, behavior: 'smooth' });
                            window.setTimeout(reportProgress, 260);
                        }
                    } else {
                        window.scrollTo({ left: prev, top: 0, behavior: 'smooth' });
                        window.setTimeout(reportProgress, 260);
                    }
                };

                window.__vellumRestore = function(progress) {
                    snapTo(progress, true);
                };

                window.__vellumRefreshTheme = function(progress) {
                    reportProgress();
                    if (typeof progress === 'number') {
                        snapTo(progress, true);
                    }
                };

                window.__vellumGoToAnchor = function(anchorId) {
                    if (!anchorId) return;
                    const target = document.getElementById(anchorId) || document.querySelector('[name="' + anchorId + '"]');
                    if (!target) return;
                    target.scrollIntoView({ behavior: 'smooth', block: 'start', inline: 'start' });
                    window.setTimeout(reportProgress, 260);
                };

                document.addEventListener('click', function(event) {
                    const link = event.target.closest('a[href]');
                    if (!link) return;
                    const href = link.getAttribute('href');
                    if (!href) return;
                    if (/^(https?:|mailto:|tel:)/i.test(href)) return;
                    if (window.VellumBridge) {
                        event.preventDefault();
                        window.VellumBridge.openLink(href);
                    }
                }, true);

                let startX = 0;
                let startY = 0;
                const swipeThreshold = 50;

                document.addEventListener('touchstart', function(e) {
                    startX = e.changedTouches[0].screenX;
                    startY = e.changedTouches[0].screenY;
                }, { passive: true });

                document.addEventListener('touchend', function(e) {
                    const endX = e.changedTouches[0].screenX;
                    const endY = e.changedTouches[0].screenY;
                    const diffX = startX - endX;
                    const diffY = startY - endY;

                    if (Math.abs(diffX) < 10 && Math.abs(diffY) < 10) {
                        const width = window.innerWidth;
                        if (endX < width * 0.3) {
                            if (isMangaMode) window.pageRight(); else window.pageLeft();
                        } else if (endX > width * 0.7) {
                            if (isMangaMode) window.pageLeft(); else window.pageRight();
                        } else if (window.VellumBridge) {
                            window.VellumBridge.toggleUi();
                        }
                        return;
                    }

                    if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > swipeThreshold) {
                        if (diffX > 0) {
                            if (isMangaMode) window.pageLeft(); else window.pageRight();
                        } else {
                            if (isMangaMode) window.pageRight(); else window.pageLeft();
                        }
                    }
                }, { passive: true });

                window.addEventListener('resize', function() { reportProgress(); });
                window.addEventListener('load', function() { reportProgress(); });
                document.addEventListener('scroll', reportProgress, { passive: true });
            })();
        </script>
    """.trimIndent()

    val chapterHtml = rawHtml
        .replace(Regex("<\\?xml[^>]*\\?>", RegexOption.IGNORE_CASE), "")
        .trim()

    val overrideCss = "<style>${EpubStyleGenerator.generateCss(style, accentColor)}</style>"
    val documentParts = extractDocumentParts(chapterHtml)
    val headContent = buildString {
        append("<meta charset=\"UTF-8\">")
        append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
        documentParts.headContent?.takeIf { it.isNotBlank() }?.let { append(it) }
        append(overrideCss)
        append(bridgeScript)
    }
    val bodyContent = documentParts.bodyContent.ifBlank { chapterHtml }
    val bodyOpenTag = documentParts.bodyOpenTag ?: "<body>"

    return """
        <!DOCTYPE html>
        <html>
        <head>
            $headContent
        </head>
        $bodyOpenTag
            <article id="vellum-content">
                $bodyContent
            </article>
        </body>
        </html>
    """.trimIndent()
}

private data class HtmlDocumentParts(
    val headContent: String?,
    val bodyOpenTag: String?,
    val bodyContent: String
)

private fun extractDocumentParts(rawHtml: String): HtmlDocumentParts {
    val headMatch = Regex("<head[^>]*>(.*?)</head>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        .find(rawHtml)
    val bodyMatch = Regex("(<body[^>]*>)(.*)</body>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        .find(rawHtml)

    return if (bodyMatch != null) {
        HtmlDocumentParts(
            headContent = headMatch?.groupValues?.getOrNull(1)?.trim(),
            bodyOpenTag = bodyMatch.groupValues.getOrNull(1)?.trim(),
            bodyContent = bodyMatch.groupValues.getOrNull(2)?.trim().orEmpty()
        )
    } else {
        HtmlDocumentParts(
            headContent = headMatch?.groupValues?.getOrNull(1)?.trim(),
            bodyOpenTag = null,
            bodyContent = rawHtml
                .replace(Regex("<!DOCTYPE[^>]*>", RegexOption.IGNORE_CASE), "")
                .replace(Regex("<html[^>]*>", RegexOption.IGNORE_CASE), "")
                .replace(Regex("</html>", RegexOption.IGNORE_CASE), "")
                .replace(Regex("<head[^>]*>.*?</head>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
                .trim()
        )
    }
}

private fun buildEpubBaseUrl(uriString: String, chapterPath: String): String {
    return "epub://local/${Uri.encode(uriString)}/$chapterPath"
}

private fun loadEpubResource(context: android.content.Context, uriString: String, archivePath: String): WebResourceResponse? {
    return try {
        val resource = EpubParser(context).loadResource(Uri.parse(uriString), archivePath) ?: return null
        WebResourceResponse(resource.mimeType, resource.encoding, ByteArrayInputStream(resource.bytes))
    } catch (_: Exception) {
        null
    }
}

private fun String.jsQuoted(): String {
    return buildString(length + 2) {
        append('"')
        for (ch in this@jsQuoted) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
        append('"')
    }
}
