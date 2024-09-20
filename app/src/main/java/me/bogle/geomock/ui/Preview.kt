package me.bogle.geomock.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import me.bogle.geomock.ui.theme.GeoMockTheme

@Preview(showBackground = true)
annotation class GeoMockPreview

@Composable
fun GeoMockThemedPreview(content: @Composable () -> Unit) = GeoMockTheme { content() }