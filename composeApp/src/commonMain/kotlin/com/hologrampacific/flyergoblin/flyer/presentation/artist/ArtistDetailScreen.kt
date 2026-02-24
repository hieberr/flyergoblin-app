package com.hologrampacific.flyergoblin.flyer.presentation.artist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.hologrampacific.flyergoblin.PlatformType
import com.hologrampacific.flyergoblin.flyer.domain.model.Artist
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfile
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudTrack
import com.hologrampacific.flyergoblin.flyer.presentation.SoundCloudProfileSelection
import com.hologrampacific.flyergoblin.flyer.presentation.artist.components.SoundCloudMultiTrackPlayer
import com.hologrampacific.flyergoblin.getPlatform
import com.hologrampacific.flyergoblin.presentation.Navigator
import com.hologrampacific.flyergoblin.presentation.Ui
import com.hologrampacific.flyergoblin.presentation.components.TopAppBarScreen
import com.hologrampacific.flyergoblin.presentation.theme.AppTheme
import flyergoblin.composeapp.generated.resources.Res
import flyergoblin.composeapp.generated.resources.chevron_right_24px
import flyergoblin.composeapp.generated.resources.soundcloud_cloudmark_transparent_white
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Screen displaying detailed information about an artist including their SoundCloud profile and top
 * tracks.
 *
 * @param navigator Navigator for handling navigation events
 * @param artistName The name of the artist to display
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(navigator: Navigator, artistName: String) {
  val viewModel: ArtistDetailViewModel = koinViewModel { parametersOf(artistName) }
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  // Show error messages in snackbar
  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let { errorMessage ->
      snackbarHostState.showSnackbar(message = errorMessage, withDismissAction = true)
      viewModel.clearError()
    }
  }

  TopAppBarScreen(
    appBarTitle = artistName,
    onBackClicked = { navigator.goBack() },
    snackbarHostState = snackbarHostState,
    // isFetchingSoundCloud: user-triggered network call; artist data is already on screen,
    // so an overlay preserves the content while showing progress.
    overlay =
      if (uiState.isFetchingSoundCloud) {
        {
          Box(
            modifier =
              Modifier.matchParentSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center,
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(Ui.halfUnit),
            ) {
              CircularProgressIndicator()
              Text("Fetching SoundCloud profile...", style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      } else null,
  ) {
    when {
      // isLoading: initial load from local DB; no content yet, so a centered spinner fills the
      // empty screen.
      uiState.isLoading -> {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
      }

      else -> {
        ArtistDetailContent(
          artist = uiState.artist,
          rateLimitResetTime = uiState.rateLimitResetTime,
          onFetchSoundCloud = { viewModel.fetchSoundCloudInfo() },
          onProfileClick = { navigator.goTo(SoundCloudProfileSelection(artistName)) },
        )
      }
    }
  }
}

/** Content section of the Artist Detail screen. */
@Composable
private fun ArtistDetailContent(
  artist: Artist?,
  rateLimitResetTime: String?,
  onFetchSoundCloud: () -> Unit,
  onProfileClick: () -> Unit,
) {
  val isDesktop = remember { getPlatform().type == PlatformType.DESKTOP }
  val scrollState = rememberScrollState()
  Column(
    modifier =
      Modifier.fillMaxSize()
        .let { if (!isDesktop) it.verticalScroll(scrollState) else it }
        .padding(horizontal = Ui.unit),
    verticalArrangement = Arrangement.spacedBy(Ui.unit),
  ) {
    HorizontalDivider()

    // SoundCloud Profile Section
    val hasProfile = artist?.soundCloudInfo?.profile != null
    val hasProfiles = artist?.soundCloudInfo?.profileSearchResults?.results?.isNotEmpty() == true

    if (hasProfile) {
      SoundCloudProfileSection(
        profileUsername = artist.soundCloudInfo.profile.username,
        profileUrl = artist.soundCloudInfo.profile.profileUrl,
        avatarUrl = artist.soundCloudInfo.profile.avatarUrl,
        fullName = artist.soundCloudInfo.profile.fullName,
        city = artist.soundCloudInfo.profile.city,
        countryCode = artist.soundCloudInfo.profile.countryCode,
        onProfileClick = onProfileClick,
      )
    } else if (hasProfiles) {
      // Show "tap to select" button when no profile is selected but profiles are available
      SelectProfileCard(onProfileClick = onProfileClick)
    }

    // Last fetched timestamp (before tracks so it's not after a weight() item on desktop)
    if (artist?.soundCloudInfo?.profile?.lastUpdated != null) {
      Text(
        text = "Last updated: ${artist.soundCloudInfo.profile.lastUpdated}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    // Top Tracks Section
    val hasTracks = artist?.soundCloudInfo?.profile?.tracks?.isNotEmpty() == true
    if (hasTracks) {
      TopTracksSection(
        tracks = artist.soundCloudInfo.profile.tracks,
        modifier = if (isDesktop) Modifier.weight(1f) else Modifier,
      )
    }

    // Fetch button
    if (!hasProfile && !hasProfiles) {
      Button(
        onClick = onFetchSoundCloud,
        modifier = Modifier.fillMaxWidth(),
        enabled = rateLimitResetTime == null,
      ) {
        Text(
          if (rateLimitResetTime != null) "Rate Limited - Try Later" else "Fetch SoundCloud Info"
        )
      }
    }
  }
}

/** Section displaying the SoundCloud profile link with official branding. */
@Composable
private fun SoundCloudProfileSection(
  profileUsername: String,
  profileUrl: String,
  avatarUrl: String?,
  fullName: String?,
  city: String?,
  countryCode: String?,
  onProfileClick: () -> Unit,
) {
  BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    val isNarrowScreen = maxWidth < 600.dp

    if (isNarrowScreen) {
      // Vertical layout for narrow screens (phones)
      Column(
        modifier = Modifier.height(IntrinsicSize.Max).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Ui.halfUnit),
      ) {
        ArtistProfileCard(
          profileUsername,
          avatarUrl = avatarUrl,
          fullName = fullName,
          city = city,
          countryCode = countryCode,
          onClick = onProfileClick,
          modifier = Modifier.fillMaxWidth(),
        )
        ViewProfileCard(profileUrl = profileUrl, modifier = Modifier.fillMaxWidth())
      }
    } else {
      // Horizontal layout for wider screens
      Row(
        modifier = Modifier.height(IntrinsicSize.Max).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Ui.halfUnit),
      ) {
        ArtistProfileCard(
          profileUsername,
          avatarUrl = avatarUrl,
          fullName = fullName,
          city = city,
          countryCode = countryCode,
          onClick = onProfileClick,
          modifier = Modifier.fillMaxWidth().weight(2f),
        )
        ViewProfileCard(
          profileUrl = profileUrl,
          modifier = Modifier.fillMaxWidth().fillMaxHeight().weight(1f),
        )
      }
    }
  }
}

/** Card displayed when no profile is selected but profiles are available. */
@Composable
private fun SelectProfileCard(onProfileClick: () -> Unit) {
  Card(
    onClick = onProfileClick,
    modifier = Modifier.fillMaxWidth().semantics { role = Role.Button },
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(
      modifier = Modifier.padding(Ui.unit),
      verticalArrangement = Arrangement.spacedBy(Ui.unit / 4),
    ) {
      Text(
        text = "No SoundCloud profile selected",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = "(tap to select)",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

/** Card displaying the SoundCloud username with "tap to change" action. */
@Composable
private fun ArtistProfileCard(
  profileUsername: String,
  avatarUrl: String?,
  fullName: String?,
  city: String?,
  countryCode: String?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = onClick,
    modifier = modifier.semantics { role = Role.Button },
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Row(
      modifier = Modifier.padding(Ui.halfUnit),
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
      Column(verticalArrangement = Arrangement.spacedBy(Ui.unit / 4)) {
        Text(
          text = profileUsername,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
        val location = buildLocationString(city, countryCode)
        val subtitle = listOfNotNull(fullName?.takeIf { it.isNotBlank() }, location)
        if (subtitle.isNotEmpty()) {
          Text(
            text = subtitle.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          text = "(tap to change)",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

/** Card with button to view SoundCloud profile. */
@Composable
private fun ViewProfileCard(profileUrl: String, modifier: Modifier = Modifier) {
  val uriHandler = LocalUriHandler.current

  Card(
    onClick = { uriHandler.openUri(profileUrl) },
    modifier = modifier.semantics { role = Role.Button },
    colors =
      CardDefaults.cardColors(
        containerColor = Color(0xFFFF5500) // SoundCloud orange
      ),
  ) {
    Row(
      modifier = Modifier.fillMaxSize().padding(Ui.halfUnit),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(Ui.unit),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
      ) {
        Image(
          painter = painterResource(Res.drawable.soundcloud_cloudmark_transparent_white),
          contentDescription = "SoundCloud",
          modifier = Modifier.size(Ui.unit * 2),
        )
        Text(
          text = "View on SoundCloud",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = Color.White,
        )
      }
      Image(
        painter = painterResource(Res.drawable.chevron_right_24px),
        contentDescription = "Back",
        modifier = Modifier.size(Ui.unit * 2),
        colorFilter = ColorFilter.tint(Color.White),
      )
    }
  }
}

@Composable
@Preview
private fun ArtistDetailContentWithProfilePreview() {
  // Tracks are intentionally omitted: SoundCloudMultiTrackPlayer uses AndroidView (WebView)
  // which cannot render in the Compose preview engine.
  AppTheme {
    ArtistDetailContent(
      artist =
        Artist(
          name = "DJ Horizon",
          soundCloudInfo =
            SoundCloudInfo(
              profile =
                SoundCloudProfile(
                  id = 1L,
                  username = "djhorizon",
                  profileUrl = "https://soundcloud.com/djhorizon",
                  fullName = "Alex Horizon",
                  city = "Berlin",
                  countryCode = "DE",
                  followersCount = 12400,
                )
            ),
        ),
      rateLimitResetTime = null,
      onFetchSoundCloud = {},
      onProfileClick = {},
    )
  }
}

@Composable
@Preview
private fun ArtistDetailContentNoProfilePreview() {
  AppTheme {
    ArtistDetailContent(
      artist = Artist(name = "DJ Horizon"),
      rateLimitResetTime = null,
      onFetchSoundCloud = {},
      onProfileClick = {},
    )
  }
}

/** Section displaying the list of popular tracks from SoundCloud with embedded players. */
@Composable
private fun TopTracksSection(tracks: List<SoundCloudTrack>, modifier: Modifier = Modifier) {
  val isDesktop = remember { getPlatform().type == PlatformType.DESKTOP }
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Ui.halfUnit)) {
    Text(text = "Popular Tracks", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    SoundCloudMultiTrackPlayer(
      tracks = tracks,
      modifier = if (isDesktop) Modifier.weight(1f) else Modifier,
    )
  }
}
