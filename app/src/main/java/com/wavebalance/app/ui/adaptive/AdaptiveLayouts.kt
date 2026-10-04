package com.wavebalance.app.ui.adaptive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Widest a page's content grows on large or maximised windows. Beyond this,
 * cards would stretch past comfortable reading width, so the page centres.
 */
val MaxContentWidth = 1600.dp

/**
 * Desktop page with a primary and a secondary column that scroll together.
 * Used on expanded windows in place of the single phone column.
 */
@Composable
fun TwoColumnPage(
    modifier: Modifier = Modifier,
    primaryWeight: Float = 1.4f,
    secondaryWeight: Float = 1f,
    columnSpacing: Dp = 20.dp,
    itemSpacing: Dp = 16.dp,
    contentPadding: PaddingValues = PaddingValues(start = 28.dp, end = 28.dp, top = 4.dp, bottom = 28.dp),
    primary: @Composable ColumnScope.() -> Unit,
    secondary: @Composable ColumnScope.() -> Unit
) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val scrollState = rememberScrollState()
        val pageModifier = Modifier
            .widthIn(max = MaxContentWidth)
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(contentPadding)
        // Navigation and system insets have already consumed space here.
        if (AdaptiveLayoutPolicy.useTwoPanes(maxWidth.value, maxHeight.value, fontScale)) {
            Row(modifier = pageModifier, horizontalArrangement = Arrangement.spacedBy(columnSpacing)) {
                Column(
                    modifier = Modifier.weight(primaryWeight),
                    verticalArrangement = Arrangement.spacedBy(itemSpacing),
                    content = primary
                )
                Column(
                    modifier = Modifier.weight(secondaryWeight),
                    verticalArrangement = Arrangement.spacedBy(itemSpacing),
                    content = secondary
                )
            }
        } else {
            Column(modifier = pageModifier, verticalArrangement = Arrangement.spacedBy(itemSpacing)) {
                primary()
                secondary()
            }
        }
    }
}
