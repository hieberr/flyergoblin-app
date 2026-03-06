package com.hologrampacific.flyergoblin.presentation.util

import androidx.compose.runtime.Composable

/**
 * Returns a function that opens [deepLinkUri] if the platform can handle it (e.g. the target app
 * is installed), falling back to [fallbackUrl] in the browser otherwise.
 */
@Composable
expect fun rememberDeepLinkOpener(): (deepLinkUri: String, fallbackUrl: String) -> Unit
