package com.prototype.gradusp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.prototype.gradusp.ui.theme.GRADUSPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GRADUSPTheme {
                GraduspApp()
            }
        }
    }
}
