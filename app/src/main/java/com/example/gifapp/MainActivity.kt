package com.example.gifapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.example.gifapp.ui.composable.RenderBackground
import com.example.gifapp.ui.composable.theme.GifAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GifAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val configuration = LocalConfiguration.current
                    val assetContainerHeight = remember {
                        (configuration.screenHeightDp * 0.6).toInt()
                    }
                    RenderBackground(assetContainerHeightDp = assetContainerHeight)
                }
            }
        }
    }
}
