package com.prototype.gradusp.ui.components.base

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraduspTitleBar(screenTitle: String) {
    // Store animation state in the ViewModel to persist across screens
    // Check if this is the first time opening the app in this session
    val sharedPrefs = LocalContext.current.getSharedPreferences("gradusp_prefs", 0)
    var showAnimation by rememberSaveable { mutableStateOf(sharedPrefs.getBoolean("first_open", true)) }
    var showSlash by remember { mutableStateOf(false) }
    var showScreenTitle by remember { mutableStateOf(false) }

    // Set up animation timings
    LaunchedEffect(showAnimation) {
        if (showAnimation) {
            // Sequence the animations
            delay(500) // Initial delay
            showSlash = true
            delay(700) // Delay before showing screen title
            showScreenTitle = true
            delay(1000) // Keep animation state for future reference
            showAnimation = false
            // Save state to prevent animation on subsequent navigation
            sharedPrefs.edit().putBoolean("first_open", false).apply()
        } else {
            // If not animating, just show everything immediately
            showSlash = true
            showScreenTitle = true
        }
    }

    // Animate the slash movement
    val slashPadding by animateDpAsState(
        targetValue = if (showSlash && !showScreenTitle) 0.dp else 4.dp,
        animationSpec = tween(500, easing = LinearEasing),
        label = "slash padding animation"
    )

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // App name is always visible
                Text(
                    text = "GRADUSP",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF27F22)
                )

                // Animated slash
                AnimatedVisibility(
                    visible = showSlash,
                    enter = fadeIn(animationSpec = tween(300))
                ) {
                    Text(
                        text = " / ",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = slashPadding)
                    )
                }

                // Animated screen title
                AnimatedVisibility(
                    visible = showScreenTitle,
                    enter = expandHorizontally(
                        animationSpec = tween(500, easing = LinearEasing)
                    ) + fadeIn(animationSpec = tween(400))
                ) {
                    Text(
                        text = screenTitle,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    )
}