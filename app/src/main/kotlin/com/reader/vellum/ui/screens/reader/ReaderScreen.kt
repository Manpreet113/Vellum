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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.reader.vellum.ui.components.indigoGlow
import com.reader.vellum.ui.theme.ElectricIndigo
import com.reader.vellum.ui.theme.InkBlack
import com.reader.vellum.util.EpubManifest
import com.reader.vellum.util.EpubStyle
import com.reader.vellum.util.EpubStyleGenerator
import com.reader.vellum.util.EpubParser
import com.reader.vellum.util.HardwareEvent
import java.io.ByteArrayInputStream
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

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

    var showUi by rememberSaveable { mutableStateOf(false) }
    var showReaderSettings by remember { mutableStateOf(false) }
    var showChapterSheet by remember { mutableStateOf(false) }
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
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Error, null, tint = Color.White, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = state.message,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
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
                val accentColor = state.accentColor ?: ElectricIndigo
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
                            onSeekConsumed = { pendingSeekTarget = null }
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

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .align(Alignment.CenterEnd)
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

                AnimatedVisibility(
                    visible = showUi,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .statusBarsPadding()
                                .clip(RoundedCornerShape(24.dp))
                                .background(InkBlack.copy(alpha = 0.82f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                                .indigoGlow(color = accentColor, alpha = 0.12f, borderRadius = 24.dp)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = state.book.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = Color.White
                                    )
                                    Text(
                                        text = progressLabel.ifBlank {
                                            if (isEpub) {
                                                "CHAPTER ${currentPage + 1} OF ${state.pages.size}"
                                            } else {
                                                "PAGE ${currentPage + 1} OF ${state.pages.size}"
                                            }
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accentColor.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (isEpub) {
                                    IconButton(
                                        onClick = { epubHistoryBackRequest += 1 },
                                        enabled = epubCanGoBack
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            "Previous location",
                                            tint = if (epubCanGoBack) Color.White else Color.White.copy(alpha = 0.35f)
                                        )
                                    }
                                    IconButton(
                                        onClick = { epubHistoryForwardRequest += 1 },
                                        enabled = epubCanGoForward
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForward,
                                            "Next location",
                                            tint = if (epubCanGoForward) Color.White else Color.White.copy(alpha = 0.35f)
                                        )
                                    }
                                }
                                if (isEpub && tocEntries.isNotEmpty()) {
                                    IconButton(onClick = { showChapterSheet = true }) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.FormatListBulleted,
                                            "Chapters",
                                            tint = Color.White
                                        )
                                    }
                                }
                                IconButton(onClick = { showReaderSettings = true }) {
                                    Icon(Icons.Default.Tune, "Settings", tint = Color.White)
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showUi,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.84f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(InkBlack.copy(alpha = 0.78f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                                .indigoGlow(color = accentColor, alpha = 0.08f, borderRadius = 24.dp)
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
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
                                    activeTrackColor = accentColor,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }
                }

                if (showReaderSettings) {
                    ReaderControlSheet(
                        viewModel = viewModel,
                        accentColor = accentColor,
                        onDismiss = { showReaderSettings = false }
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
fun ReaderControlSheet(
    viewModel: ReaderViewModel,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isMangaMode by viewModel.mangaMode.collectAsState(false)
    val isTapToTurn by viewModel.tapToTurn.collectAsState(true)
    val isAdaptiveChroma by viewModel.adaptiveChroma.collectAsState(true)
    val isVolumeKeys by viewModel.volumeKeys.collectAsState(false)
    val uiState by viewModel.uiState.collectAsState()
    val isEpub = (uiState as? ReaderUiState.Success)?.book?.format == "epub"
    val epubStyle by viewModel.epubStyle.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F0F0F),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.2f)) },
        tonalElevation = 8.dp,
        scrimColor = Color.Black.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Text(
                "READER CONFIG",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = Color.White
            )

            if (isEpub) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "TYPOGRAPHY",
                        style = MaterialTheme.typography.labelLarge,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )

                    ControlRow("FONT SIZE") {
                        Slider(
                            value = epubStyle.fontSize,
                            onValueChange = { viewModel.setEpubFontSize(it) },
                            valueRange = 12f..32f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = accentColor
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
                                activeTrackColor = accentColor
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
                                activeTrackColor = accentColor
                            )
                        )
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FontButton("SERIF", epubStyle.fontFamily == "serif", accentColor) {
                            viewModel.setEpubFontFamily("serif")
                        }
                        FontButton("SANS", epubStyle.fontFamily == "sans-serif", accentColor) {
                            viewModel.setEpubFontFamily("sans-serif")
                        }
                        FontButton("MONO", epubStyle.fontFamily == "monospace", accentColor) {
                            viewModel.setEpubFontFamily("monospace")
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "APPEARANCE",
                        style = MaterialTheme.typography.labelLarge,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ThemeCircle("DARK", Color(0xFF0A0A0A), epubStyle.theme == "dark", accentColor) {
                            viewModel.setEpubTheme("dark")
                        }
                        ThemeCircle("SEP", Color(0xFFF4ECD8), epubStyle.theme == "sepia", accentColor) {
                            viewModel.setEpubTheme("sepia")
                        }
                        ThemeCircle("LIT", Color.White, epubStyle.theme == "light", accentColor) {
                            viewModel.setEpubTheme("light")
                        }
                        ThemeCircle("NIT", Color.Black, epubStyle.theme == "night", accentColor) {
                            viewModel.setEpubTheme("night")
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "NAVIGATION",
                    style = MaterialTheme.typography.labelLarge,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
                if (!isEpub) {
                    SettingsToggleNeo("TAP TO TURN", isTapToTurn, accentColor, viewModel::setTapToTurn)
                }
                SettingsToggleNeo("MANGA MODE", isMangaMode, accentColor, viewModel::setMangaMode)
                SettingsToggleNeo("VOLUME KEYS", isVolumeKeys, accentColor, viewModel::setVolumeKeys)
                SettingsToggleNeo("CHROMA ENGINE", isAdaptiveChroma, accentColor, viewModel::setAdaptiveChroma)
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
        containerColor = Color(0xFF0F0F0F),
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
            entries.forEach { entry ->
                val isCurrent = entry.chapterIndex == currentChapterIndex
                Box(
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
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
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
fun RowScope.FontButton(label: String, selected: Boolean, accentColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accentColor else Color.White.copy(alpha = 0.05f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
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
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, if (selected) accentColor else Color.White.copy(alpha = 0.1f), CircleShape)
                .clickable { onClick() }
        )
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
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
            val documentHtml = remember(html, epubStyle, accentColor) {
                buildStyledDocument(html.orEmpty(), epubStyle, accentColor.toArgb())
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

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .height(4.dp)
                .fillMaxWidth(0.4f)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(controller.overallProgress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(accentColor)
                    .indigoGlow(color = accentColor, alpha = 0.4f, blurRadius = 6.dp)
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

private fun buildStyledDocument(rawHtml: String, style: EpubStyle, accentColor: Int): String {
    val bridgeScript = """
        <script>
            (function() {
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
                            window.pageLeft();
                        } else if (endX > width * 0.7) {
                            window.pageRight();
                        } else if (window.VellumBridge) {
                            window.VellumBridge.toggleUi();
                        }
                        return;
                    }

                    if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > swipeThreshold) {
                        if (diffX > 0) {
                            window.pageRight();
                        } else {
                            window.pageLeft();
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
