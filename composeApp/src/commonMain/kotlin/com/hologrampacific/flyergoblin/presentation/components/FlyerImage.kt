package com.hologrampacific.flyergoblin.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.hologrampacific.flyergoblin.presentation.Ui
import com.hologrampacific.flyergoblin.presentation.util.decodeImageBitmap

@Composable
fun FlyerImage(imageBytes: ByteArray, modifier: Modifier = Modifier) {
  val imageBitmap = remember(imageBytes) { decodeImageBitmap(imageBytes) }

  if (imageBitmap != null) {
    Image(
      bitmap = imageBitmap,
      contentDescription = "Event flyer",
      modifier = modifier,
      contentScale = ContentScale.FillWidth,
    )
  } else {
    // Show error state when image decoding fails
    Surface(
      modifier = modifier.height(Ui.unit * 12),
      color = MaterialTheme.colorScheme.errorContainer,
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(Ui.unit),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text("Unable to display flyer image", color = MaterialTheme.colorScheme.onErrorContainer)
      }
    }
  }
}
