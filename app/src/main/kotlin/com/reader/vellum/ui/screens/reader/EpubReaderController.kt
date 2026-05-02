package com.reader.vellum.ui.screens.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.reader.vellum.util.EpubHrefTarget
import com.reader.vellum.util.EpubManifest
import com.reader.vellum.util.EpubPublication
import java.util.Locale
import kotlin.math.roundToInt

private data class EpubHistoryEntry(
    val chapterIndex: Int,
    val chapterProgress: Float,
    val anchor: String?
)

class EpubReaderController(
    private val bookId: String,
    private val manifest: EpubManifest?,
    private val requests: List<EpubPageRequest>,
    initialChapterIndex: Int,
    initialLocator: EpubLocator
) {
    private val historyBack = ArrayDeque<EpubHistoryEntry>()
    private val historyForward = ArrayDeque<EpubHistoryEntry>()

    var currentChapterIndex by mutableIntStateOf(initialChapterIndex.coerceIn(0, requests.lastIndex.coerceAtLeast(0)))
        private set
    var chapterProgress by mutableFloatStateOf(0f)
        private set
    var pendingRestoreProgress by mutableFloatStateOf(
        if (initialLocator.chapterIndex == currentChapterIndex) {
            initialLocator.chapterProgress
        } else {
            0f
        }
    )
        private set
    var pendingAnchor by mutableStateOf<String?>(null)
        private set

    val currentRequest: EpubPageRequest?
        get() = requests.getOrNull(currentChapterIndex)

    val canGoBack: Boolean
        get() = historyBack.isNotEmpty()

    val canGoForward: Boolean
        get() = historyForward.isNotEmpty()

    val chapterTitle: String
        get() = resolveChapterTitle(manifest, currentRequest?.chapterPath.orEmpty(), currentChapterIndex)

    val overallProgress: Float
        get() = computeOverallProgress(currentChapterIndex, chapterProgress, requests.size)

    val readingLabel: String
        get() = "${chapterTitle.uppercase(Locale.getDefault())}  ${formatPercent(overallProgress)}"

    fun onProgressUpdate(progress: Float) {
        chapterProgress = progress.coerceIn(0f, 1f)
    }

    fun onExternalLocator(locator: EpubLocator) {
        if (locator.chapterIndex == currentChapterIndex) {
            pendingRestoreProgress = locator.chapterProgress.coerceIn(0f, 1f)
        }
    }

    fun seekTo(targetChapterIndex: Int) {
        pushCurrentToBackStack(anchor = null)
        currentChapterIndex = targetChapterIndex.coerceIn(0, requests.lastIndex.coerceAtLeast(0))
        chapterProgress = 0f
        pendingRestoreProgress = 0f
        pendingAnchor = null
        historyForward.clear()
    }

    fun nextChapter() {
        if (currentChapterIndex < requests.lastIndex) {
            pushCurrentToBackStack(anchor = null)
            currentChapterIndex += 1
            chapterProgress = 0f
            pendingRestoreProgress = 0f
            pendingAnchor = null
            historyForward.clear()
        }
    }

    fun previousChapter() {
        if (currentChapterIndex > 0) {
            pushCurrentToBackStack(anchor = null)
            currentChapterIndex -= 1
            chapterProgress = 1f
            pendingRestoreProgress = 1f
            pendingAnchor = null
            historyForward.clear()
        }
    }

    fun consumePendingAnchor(): String? {
        val anchor = pendingAnchor
        pendingAnchor = null
        return anchor
    }

    fun navigateToHref(currentChapterPath: String, href: String): EpubHrefTarget? {
        val target = EpubPublication.resolveNavigationTarget(currentChapterPath, href) ?: return null
        val currentArchivePath = currentChapterPath.substringBefore('#')

        if (target.archivePath == currentArchivePath) {
            pendingAnchor = target.fragment
            return target
        }

        val targetIndex = requests.indexOfFirst { request ->
            normalizeHref(request.chapterPath) == normalizeHref(target.archivePath)
        }
        if (targetIndex < 0) return null

        pushCurrentToBackStack(anchor = pendingAnchor)
        currentChapterIndex = targetIndex
        chapterProgress = 0f
        pendingRestoreProgress = 0f
        pendingAnchor = target.fragment
        historyForward.clear()
        return target
    }

    fun goBackInHistory(): Boolean {
        val entry = historyBack.removeLastOrNull() ?: return false
        historyForward.addLast(currentHistoryEntry())
        restoreEntry(entry)
        return true
    }

    fun goForwardInHistory(): Boolean {
        val entry = historyForward.removeLastOrNull() ?: return false
        historyBack.addLast(currentHistoryEntry())
        restoreEntry(entry)
        return true
    }

    private fun pushCurrentToBackStack(anchor: String?) {
        historyBack.addLast(
            EpubHistoryEntry(
                chapterIndex = currentChapterIndex,
                chapterProgress = chapterProgress,
                anchor = anchor
            )
        )
        if (historyBack.size > 100) {
            historyBack.removeFirst()
        }
    }

    private fun currentHistoryEntry(): EpubHistoryEntry {
        return EpubHistoryEntry(
            chapterIndex = currentChapterIndex,
            chapterProgress = chapterProgress,
            anchor = pendingAnchor
        )
    }

    private fun restoreEntry(entry: EpubHistoryEntry) {
        currentChapterIndex = entry.chapterIndex.coerceIn(0, requests.lastIndex.coerceAtLeast(0))
        chapterProgress = entry.chapterProgress.coerceIn(0f, 1f)
        pendingRestoreProgress = chapterProgress
        pendingAnchor = entry.anchor
    }
}

private fun resolveChapterTitle(manifest: EpubManifest?, chapterPath: String, chapterIndex: Int): String {
    val normalizedChapterPath = normalizeHref(chapterPath)
    return manifest?.toc
        ?.firstOrNull { tocItem ->
            val tocPath = normalizeHref(tocItem.href)
            tocPath == normalizedChapterPath ||
                normalizedChapterPath.startsWith(tocPath) ||
                tocPath.startsWith(normalizedChapterPath)
        }
        ?.title
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: resolveFallbackChapterTitle(chapterPath, chapterIndex)
}

private fun resolveFallbackChapterTitle(chapterPath: String, chapterIndex: Int): String {
    val base = chapterPath.substringAfterLast('/').substringBeforeLast('.')
    val cleaned = base.replace('-', ' ').replace('_', ' ').trim()
    return cleaned.ifBlank { "Chapter ${chapterIndex + 1}" }
}

private fun normalizeHref(href: String): String = href.substringBefore('#').substringBefore('?')

private fun computeOverallProgress(chapterIndex: Int, chapterProgress: Float, chapterCount: Int): Float {
    if (chapterCount <= 0) return 0f
    if (chapterCount == 1) return chapterProgress.coerceIn(0f, 1f)
    return ((chapterIndex.toFloat() + chapterProgress.coerceIn(0f, 1f)) / chapterCount.toFloat()).coerceIn(0f, 1f)
}

private fun formatPercent(progress: Float): String = "${(progress * 100).roundToInt()}%"
