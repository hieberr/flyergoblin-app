package com.hologrampacific.learnkmp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.hologrampacific.learnkmp.util.InitUrlOpener

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    setContent {
      InitUrlOpener()
      App()
    }
  }
}

@Preview
@Composable
fun AppAndroidPreview() {
  App()
}
