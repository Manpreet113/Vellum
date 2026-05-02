package com.reader.vellum.util

data class EpubStyle(
    val fontSize: Float,
    val fontFamily: String,
    val lineHeight: Float,
    val theme: String,
    val margin: Int
)

object EpubStyleGenerator {
    fun generateCss(style: EpubStyle, accentColor: Int = 0xFF6366F1.toInt()): String {
        val (backgroundColor, textColor) = when (style.theme) {
            "light" -> "#FFFFFF" to "#1A1A1A"
            "sepia" -> "#F4ECD8" to "#3D2E24"
            "night" -> "#000000" to "#A0A0A0"
            else -> "#0A0A0A" to "#E0E0E0"
        }

        val accent = String.format("#%06X", 0xFFFFFF and accentColor)
        val fontStack = when (style.fontFamily) {
            "serif" -> "Georgia, 'Times New Roman', serif"
            "sans-serif" -> "'Segoe UI', Roboto, Helvetica, Arial, sans-serif"
            "monospace" -> "'JetBrains Mono', 'SFMono-Regular', Consolas, monospace"
            else -> "serif"
        }

        return """
            :root {
                --vellum-font-size: ${style.fontSize}px;
                --vellum-line-height: ${style.lineHeight};
                --vellum-text-color: $textColor;
                --vellum-bg-color: $backgroundColor;
                --vellum-side-padding: ${style.margin}px;
                --vellum-accent-color: $accent;
                color-scheme: ${if (style.theme == "light" || style.theme == "sepia") "light" else "dark"};
            }

            html, body {
                background: var(--vellum-bg-color) !important;
                color: var(--vellum-text-color) !important;
            }

            body, #vellum-content {
                font-family: $fontStack !important;
                font-size: var(--vellum-font-size) !important;
                line-height: var(--vellum-line-height) !important;
                color: var(--vellum-text-color) !important;
                background: transparent !important;
            }

            body {
                margin: 0 !important;
                padding-left: var(--vellum-side-padding) !important;
                padding-right: var(--vellum-side-padding) !important;
                padding-top: 16px !important;
                padding-bottom: 16px !important;
            }

            #vellum-content {
                max-width: 100% !important;
            }

            img, svg, video, audio, canvas {
                max-width: 100% !important;
                height: auto !important;
            }

            a {
                color: var(--vellum-accent-color) !important;
            }
        """.trimIndent()
    }
}
