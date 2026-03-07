package com.hologrampacific.flyergoblin.flyer.presentation.artist.profileselection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.hologrampacific.flyergoblin.flyer.presentation.artist.buildLocationString
import com.hologrampacific.flyergoblin.presentation.Navigator
import com.hologrampacific.flyergoblin.presentation.Ui
import com.hologrampacific.flyergoblin.presentation.components.ScreenButtonConfig
import com.hologrampacific.flyergoblin.presentation.components.TopAppBarScreenWithCenteredContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileSelectionScreenLayout(
  appBarTitle: String,
  artistName: String,
  isLoading: Boolean,
  isConfirming: Boolean,
  hasNoProfiles: Boolean,
  isNoneSelected: Boolean,
  onSelectNone: () -> Unit,
  navigator: Navigator,
  snackbarHostState: SnackbarHostState,
  primaryButtonConfig: ScreenButtonConfig,
  noneDescription: String,
  searchResultsAvailable: Boolean,
  isSearching: Boolean,
  onSearchProfiles: () -> Unit,
  navBarActions: @Composable RowScope.() -> Unit = {},
  profileCards: @Composable () -> Unit,
) {
  TopAppBarScreenWithCenteredContent(
    appBarTitle = appBarTitle,
    onBackClicked = { navigator.goBack() },
    snackbarHostState = snackbarHostState,
    navBarActions = navBarActions,
    primaryButtonConfig = primaryButtonConfig,
    overlay =
      if (isConfirming) {
        {
          Box(
            modifier =
              Modifier.matchParentSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator()
          }
        }
      } else null,
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      if (isLoading || isSearching) {
        CircularProgressIndicator(
          modifier = Modifier.align(Alignment.CenterHorizontally).padding(Ui.unit * 2)
        )
      } else if (!searchResultsAvailable) {
        Button(
          onClick = onSearchProfiles,
          modifier = Modifier.fillMaxWidth().padding(top = Ui.unit),
        ) {
          Text("Search Profiles")
        }
      } else {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(Ui.halfUnit),
        ) {
          if (hasNoProfiles) {
            Text(
              text = "No search results found for $artistName",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = Ui.halfUnit),
            )
          }
          NoneCard(
            isSelected = isNoneSelected,
            onClick = onSelectNone,
            description = noneDescription,
          )
          profileCards()
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileCardBase(
  isSelected: Boolean,
  onClick: () -> Unit,
  content: @Composable () -> Unit,
) {
  Card(
    onClick = onClick,
    modifier =
      Modifier.fillMaxWidth().semantics {
        stateDescription = if (isSelected) "Selected" else "Not selected"
      },
    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (isSelected) MaterialTheme.colorScheme.primaryContainer
          else MaterialTheme.colorScheme.surfaceVariant
      ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(Ui.unit),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(Ui.unit / 4),
      ) {
        content()
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

@Composable
internal fun ProfileCard(
  avatarUrl: String?,
  username: String,
  displayName: String?,
  city: String?,
  countryCode: String?,
  followerCount: Int?,
  contentCount: Int?,
  contentLabel: String,
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  ProfileCardBase(isSelected = isSelected, onClick = onClick) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(Ui.unit),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (avatarUrl != null) {
        val placeholderColor = MaterialTheme.colorScheme.surfaceContainerHighest
        AsyncImage(
          model = avatarUrl,
          contentDescription = null,
          contentScale = ContentScale.Crop,
          placeholder = remember(placeholderColor) { ColorPainter(placeholderColor) },
          modifier = Modifier.size(Ui.unit * 3).clip(CircleShape),
        )
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(Ui.unit / 4),
      ) {
        Text(
          text = username,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
        )

        val location = buildLocationString(city, countryCode)
        val nameAndLocation = listOfNotNull(displayName?.takeIf { it.isNotBlank() }, location)
        if (nameAndLocation.isNotEmpty()) {
          Text(
            text = nameAndLocation.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
          )
        }

        val details = buildList {
          followerCount?.let { add("$it followers") }
          contentCount?.let { add("$it $contentLabel") }
        }
        if (details.isNotEmpty()) {
          Text(
            text = details.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
internal fun NoneCard(isSelected: Boolean, onClick: () -> Unit, description: String) {
  ProfileCardBase(isSelected = isSelected, onClick = onClick) {
    Text(
      text = "None",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
    )

    Text(
      text = description,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
