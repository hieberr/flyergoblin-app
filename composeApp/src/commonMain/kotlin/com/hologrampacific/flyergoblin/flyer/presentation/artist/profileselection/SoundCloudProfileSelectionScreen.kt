package com.hologrampacific.flyergoblin.flyer.presentation.artist.profileselection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfileInfo
import com.hologrampacific.flyergoblin.presentation.BackIcon
import com.hologrampacific.flyergoblin.presentation.Navigator
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundCloudProfileSelectionScreen(navigator: Navigator, artistName: String) {
  val viewModel: SoundCloudProfileSelectionViewModel = koinViewModel { parametersOf(artistName) }
  val uiState by viewModel.uiState.collectAsState()

  LaunchedEffect(Unit) {
    viewModel.effects.collect { effect ->
      when (effect) {
        is SoundCloudProfileSelectionEffect.NavigateBack -> navigator.goBack()
      }
    }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    TopAppBar(
      title = { Text("Select SoundCloud Profile") },
      navigationIcon = { IconButton(onClick = { navigator.goBack() }) { BackIcon() } },
      windowInsets = WindowInsets(0, 0, 0, 0),
    )

    if (uiState.isLoading) {
      CircularProgressIndicator(
        modifier = Modifier.align(Alignment.CenterHorizontally).padding(32.dp)
      )
    } else {
      LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(uiState.profiles, key = { it.profileUrl }) { profile ->
          ProfileCard(
            profile = profile,
            isSelected = profile.profileUrl == uiState.selectedProfileUrl,
            onClick = { viewModel.selectProfile(profile.profileUrl) },
          )
        }
      }

      Button(
        onClick = { viewModel.confirmSelection() },
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        enabled = uiState.selectedProfileUrl != null,
      ) {
        Text("Use this profile")
      }
    }
  }
}

@Composable
private fun ProfileCard(profile: SoundCloudProfileInfo, isSelected: Boolean, onClick: () -> Unit) {
  Card(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (isSelected) MaterialTheme.colorScheme.primaryContainer
          else MaterialTheme.colorScheme.surfaceVariant
      ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = profile.username,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
        )

        val details = buildList {
          profile.followersCount?.let { add("$it followers") }
          profile.trackCount?.let { add("$it tracks") }
          val location = buildLocationString(profile.city, profile.countryCode)
          if (location != null) add(location)
        }
        if (details.isNotEmpty()) {
          Text(
            text = details.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (isSelected) {
        Text(
          text = "\u2713",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

private fun buildLocationString(city: String?, countryCode: String?): String? {
  return when {
    city != null && countryCode != null -> "$city, $countryCode"
    city != null -> city
    countryCode != null -> countryCode
    else -> null
  }
}
