package com.movtery.zalithlauncher.ui.theme

import androidx.compose.material3.ColorScheme

/**
 * Прозрачность карточек и панелей.
 * 1.0 - обычные непрозрачные, 0.5 - очень прозрачные.
 * Если хочется сильнее или слабее "стекла", меняйте это число.
 */
const val GLASS_ALPHA = 0.7f

/**
 * Делает фоны карточек полупрозрачными,
 * чтобы через них был виден фон лаунчера.
 * Диалоги (surfaceContainerHigh) остаются непрозрачными, чтобы текст читался.
 */
fun ColorScheme.withGlass(alpha: Float = GLASS_ALPHA): ColorScheme = copy(
    surfaceContainerLow = surfaceContainerLow.copy(alpha = alpha),
    surfaceContainer = surfaceContainer.copy(alpha = alpha),
    surfaceContainerHighest = surfaceContainerHighest.copy(alpha = alpha),
)