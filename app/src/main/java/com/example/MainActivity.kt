package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chess.AppLang
import com.example.ui.ChessViewModel
import com.example.ui.ChessVisionScreen
import com.example.ui.theme.ChessVisionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            val viewModel: ChessViewModel = viewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            ChessVisionTheme(isArabic = state.lang == AppLang.AR) {
                ChessVisionScreen(viewModel = viewModel)
            }
        }
    }
}
