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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
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
import coil3.compose.AsyncImage
import com.hologrampacific.flyergoblin.PlatformType
import com.hologrampacific.flyergoblin.flyer.domain.model.Artist
import com.hologrampacific.flyergoblin.flyer.domain.model.MixcloudShow
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudInfo
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudProfile
import com.hologrampacific.flyergoblin.flyer.domain.model.SoundCloudTrack
import com.hologrampacific.flyergoblin.flyer.presentation.MixcloudProfileSelection
import com.hologrampacific.flyergoblin.flyer.presentation.SoundCloudProfileSelection
import com.hologrampacific.flyergoblin.flyer.presentation.artist.components.MixcloudMultiTrackPlayer
import com.hologrampacific.flyergoblin.flyer.presentation.artist.components.SoundCloudMultiTrackPlayer
import com.hologrampacific.flyergoblin.getPlatform
import com.hologrampacific.flyergoblin.presentation.Navigator
import com.hologrampacific.flyergoblin.presentation.Ui
import com.hologrampacific.flyergoblin.presentation.components.DevMenu
import com.hologrampacific.flyergoblin.presentation.components.DevMenuTestSnackbarErrorText
import com.hologrampacific.flyergoblin.presentation.components.TopAppBarScreen
import com.hologrampacific.flyergoblin.presentation.theme.AppTheme
import com.hologrampacific.flyergoblin.presentation.util.formattedString
import com.hologrampacific.flyergoblin.presentation.util.rememberDeepLinkOpener
import flyergoblin.composeapp.generated.resources.Res
import flyergoblin.composeapp.generated.resources.chevron_right_24px
import flyergoblin.composeapp.generated.resources.music_note_24px
import flyergoblin.composeapp.generated.resources.soundcloud_cloudmark_transparent_white
import kotlin.time.Instant
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Tabs shown on the Artist Detail screen. */
enum class ArtistTab {
  SoundCloud,
  Mixcloud,
}

/**
 * Screen displaying detailed information about an artist including their SoundCloud and Mixcloud
 * profiles.
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

  val isFetching = uiState.isFetchingSoundCloud || uiState.isFetchingMixcloud
  val fetchingLabel =
    when {
      uiState.isFetchingSoundCloud -> "Fetching SoundCloud profile..."
      uiState.isFetchingMixcloud -> "Fetching Mixcloud profile..."
      else -> ""
    }

  TopAppBarScreen(
    appBarTitle = artistName,
    onBackClicked = { navigator.goBack() },
    snackbarHostState = snackbarHostState,
    navBarActions = {
      DevMenu {
        DropdownMenuItem(
          text = { Text("Clear artist data") },
          onClick = {
            dismiss()
            viewModel.deleteArtist()
          },
        )
        DropdownMenuItem(
          text = { DevMenuTestSnackbarErrorText() },
          onClick = {
            dismiss()
            viewModel.triggerTestError()
          },
        )
      }
    },
    // isFetching: user-triggered network call; artist data is already on screen,
    // so an overlay preserves the content while showing progress.
    overlay =
      if (isFetching) {
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
              Text(fetchingLabel, style = MaterialTheme.typography.bodyMedium)
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
          selectedTab = uiState.selectedTab,
          onTabSelected = { viewModel.selectTab(it) },
          rateLimitBlockedUntil = uiState.rateLimitBlockedUntil,
          onFetchSoundCloud = { viewModel.fetchSoundCloudInfo() },
          onFetchMixcloud = { viewModel.fetchMixcloudInfo() },
          onSoundCloudProfileClick = { navigator.goTo(SoundCloudProfileSelection(artistName)) },
          onMixcloudProfileClick = { navigator.goTo(MixcloudProfileSelection(artistName)) },
        )
      }
    }
  }
}

/** Content section of the Artist Detail screen. */
@Composable
private fun ArtistDetailContent(
  artist: Artist?,
  selectedTab: ArtistTab,
  onTabSelected: (ArtistTab) -> Unit,
  rateLimitBlockedUntil: Instant?,
  onFetchSoundCloud: () -> Unit,
  onFetchMixcloud: () -> Unit,
  onSoundCloudProfileClick: () -> Unit,
  onMixcloudProfileClick: () -> Unit,
) {
  val isDesktop = remember { getPlatform().type == PlatformType.DESKTOP }
  val scrollState = rememberScrollState()

  Column(modifier = Modifier.fillMaxSize()) {
    PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
      ArtistTab.entries.forEach { tab ->
        Tab(
          selected = selectedTab == tab,
          onClick = { onTabSelected(tab) },
          text = { Text(tab.name) },
        )
      }
    }

    Column(
      modifier =
        Modifier.fillMaxSize()
          .let { if (!isDesktop) it.verticalScroll(scrollState) else it }
          // Add a bunch of extra bottom padding so we can scroll up past the bottom item.
          .padding(top = 0.dp, bottom = Ui.unit * 4, start = Ui.unit, end = Ui.unit),
      verticalArrangement = Arrangement.spacedBy(Ui.unit),
    ) {
      when (selectedTab) {
        ArtistTab.SoundCloud ->
          SoundCloudTabContent(
            artist = artist,
            rateLimitBlockedUntil = rateLimitBlockedUntil,
            onFetchSoundCloud = onFetchSoundCloud,
            onProfileClick = onSoundCloudProfileClick,
          )

        ArtistTab.Mixcloud ->
          MixcloudTabContent(
            artist = artist,
            onFetchMixcloud = onFetchMixcloud,
            onProfileClick = onMixcloudProfileClick,
          )
      }
    }
  }
}

@Composable
private fun SoundCloudTabContent(
  artist: Artist?,
  rateLimitBlockedUntil: Instant?,
  onFetchSoundCloud: () -> Unit,
  onProfileClick: () -> Unit,
) {
  val hasProfile = artist?.soundCloudInfo?.profile != null
  val hasProfiles = artist?.soundCloudInfo?.profileSearchResults?.results?.isNotEmpty() == true

  if (hasProfile) {
    PlatformProfileSection(
      profileCard = { modifier ->
        ArtistProfileCard(
          profileUsername = artist.soundCloudInfo.profile.username,
          avatarUrl = artist.soundCloudInfo.profile.avatarUrl,
          fullName = artist.soundCloudInfo.profile.fullName,
          city = artist.soundCloudInfo.profile.city,
          countryCode = artist.soundCloudInfo.profile.countryCode,
          onClick = onProfileClick,
          modifier = modifier,
        )
      },
      viewCard = { modifier ->
        ViewPlatformCard(
          profileUrl = artist.soundCloudInfo.profile.profileUrl,
          deepLinkUri = "soundcloud://users/${artist.soundCloudInfo.profile.id}",
          brandColor = Color(0xFFFF5500),
          platformName = "SoundCloud",
          brandIcon = {
            Image(
              painter = painterResource(Res.drawable.soundcloud_cloudmark_transparent_white),
              contentDescription = "SoundCloud",
              modifier = Modifier.size(Ui.unit * 2),
            )
          },
          modifier = modifier,
        )
      },
    )
  } else if (hasProfiles) {
    SelectProfileCard(platformName = "SoundCloud", onProfileClick = onProfileClick)
  }

  if (artist?.soundCloudInfo?.profile?.lastUpdated != null) {
    Text(
      text = "Last updated: ${artist.soundCloudInfo.profile.lastUpdated.formattedString()}",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }

  val hasTracks = artist?.soundCloudInfo?.profile?.tracks?.isNotEmpty() == true
  if (hasTracks) {
    TopTracksSection(tracks = artist.soundCloudInfo.profile.tracks)
  }

  if (!hasProfile && !hasProfiles) {
    Button(
      onClick = onFetchSoundCloud,
      modifier = Modifier.fillMaxWidth(),
      enabled = rateLimitBlockedUntil == null,
    ) {
      Text(
        if (rateLimitBlockedUntil != null) "Rate Limited - Try Later" else "Fetch SoundCloud Info"
      )
    }
  }
}

@Composable
private fun MixcloudTabContent(
  artist: Artist?,
  onFetchMixcloud: () -> Unit,
  onProfileClick: () -> Unit,
) {
  val hasProfile = artist?.mixcloudInfo?.profile != null
  val hasProfiles = artist?.mixcloudInfo?.profileSearchResults?.results?.isNotEmpty() == true

  if (hasProfile) {
    PlatformProfileSection(
      profileCard = { modifier ->
        ArtistProfileCard(
          profileUsername = artist.mixcloudInfo.profile.username,
          avatarUrl = artist.mixcloudInfo.profile.avatarUrl,
          fullName = artist.mixcloudInfo.profile.name,
          city = artist.mixcloudInfo.profile.city,
          countryCode = artist.mixcloudInfo.profile.countryCode,
          onClick = onProfileClick,
          modifier = modifier,
        )
      },
      viewCard = { modifier ->
        ViewPlatformCard(
          profileUrl = artist.mixcloudInfo.profile.profileUrl,
          deepLinkUri = "mixcloud://profile/${artist.mixcloudInfo.profile.username}",
          brandColor = Color(0xFF52AAD8),
          platformName = "Mixcloud",
          brandIcon = {
            Image(
              painter = painterResource(Res.drawable.music_note_24px),
              contentDescription = "Mixcloud",
              modifier = Modifier.size(Ui.unit * 2),
              colorFilter = ColorFilter.tint(Color.White),
            )
          },
          modifier = modifier,
        )
      },
    )
  } else if (hasProfiles) {
    SelectProfileCard(platformName = "Mixcloud", onProfileClick = onProfileClick)
  }

  if (artist?.mixcloudInfo?.profile?.lastUpdated != null) {
    Text(
      text = "Last updated: ${artist.mixcloudInfo.profile.lastUpdated.formattedString()}",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }

  val hasShows = artist?.mixcloudInfo?.profile?.shows?.isNotEmpty() == true
  if (hasShows) {
    MixcloudShowsSection(shows = artist.mixcloudInfo.profile.shows)
  }

  if (!hasProfile && !hasProfiles) {
    Button(onClick = onFetchMixcloud, modifier = Modifier.fillMaxWidth()) {
      Text("Fetch Mixcloud Info")
    }
  }
}

/**
 * Generic platform profile section handling responsive narrow/wide layout.
 *
 * @param profileCard Composable for the profile card, receives a Modifier
 * @param viewCard Composable for the view-on-platform card, receives a Modifier
 */
@Composable
private fun PlatformProfileSection(
  profileCard: @Composable (Modifier) -> Unit,
  viewCard: @Composable (Modifier) -> Unit,
) {
  BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    val isNarrowScreen = maxWidth < 600.dp

    if (isNarrowScreen) {
      // Vertical layout for narrow screens (phones)
      Column(
        modifier = Modifier.height(IntrinsicSize.Max).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Ui.halfUnit),
      ) {
        profileCard(Modifier.fillMaxWidth())
        viewCard(Modifier.fillMaxWidth())
      }
    } else {
      // Horizontal layout for wider screens
      Row(
        modifier = Modifier.height(IntrinsicSize.Max).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Ui.halfUnit),
      ) {
        profileCard(Modifier.fillMaxWidth().weight(2f))
        viewCard(Modifier.fillMaxWidth().fillMaxHeight().weight(1f))
      }
    }
  }
}

/** Card displayed when no profile is selected but profiles are available. */
@Composable
private fun SelectProfileCard(platformName: String, onProfileClick: () -> Unit) {
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
        text = "No $platformName profile selected",
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

/** Card displaying the artist's platform username with "tap to change" action. */
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

/** Card with button to view the artist profile on a platform. */
@Composable
private fun ViewPlatformCard(
  profileUrl: String,
  brandColor: Color,
  platformName: String,
  brandIcon: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  deepLinkUri: String? = null,
) {
  val uriHandler = LocalUriHandler.current
  val openDeepLink = rememberDeepLinkOpener()

  Card(
    onClick = {
      if (deepLinkUri != null) openDeepLink(deepLinkUri, profileUrl)
      else uriHandler.openUri(profileUrl)
    },
    modifier = modifier.semantics { role = Role.Button },
    colors = CardDefaults.cardColors(containerColor = brandColor),
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
        brandIcon()
        Text(
          text = "View on $platformName",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = Color.White,
        )
      }
      Image(
        painter = painterResource(Res.drawable.chevron_right_24px),
        contentDescription = null,
        modifier = Modifier.size(Ui.unit * 2),
        colorFilter = ColorFilter.tint(Color.White),
      )
    }
  }
}

/** Section displaying the list of popular tracks from SoundCloud with embedded players. */
@Composable
private fun TopTracksSection(tracks: List<SoundCloudTrack>, modifier: Modifier = Modifier) {
  val isDesktop = remember { getPlatform().type == PlatformType.DESKTOP }
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Ui.halfUnit)) {
    Text(
      text = "Popular Tracks",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
    )
    SoundCloudMultiTrackPlayer(
      tracks = tracks,
      modifier = if (isDesktop) Modifier.weight(1f) else Modifier,
    )
  }
}

/** Section displaying Mixcloud shows with embedded players. */
@Composable
private fun MixcloudShowsSection(shows: List<MixcloudShow>, modifier: Modifier = Modifier) {
  val isDesktop = remember { getPlatform().type == PlatformType.DESKTOP }
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Ui.halfUnit)) {
    Text(
      text = "Shows",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
    )
    MixcloudMultiTrackPlayer(
      shows = shows,
      modifier = if (isDesktop) Modifier.weight(1f) else Modifier,
    )
  }
}

@Composable
@Preview
private fun ArtistDetailContentWithSoundCloudProfilePreview() {
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
      selectedTab = ArtistTab.SoundCloud,
      onTabSelected = {},
      rateLimitBlockedUntil = null,
      onFetchSoundCloud = {},
      onFetchMixcloud = {},
      onSoundCloudProfileClick = {},
      onMixcloudProfileClick = {},
    )
  }
}

@Composable
@Preview
private fun ArtistDetailContentNoProfilePreview() {
  AppTheme {
    ArtistDetailContent(
      artist = Artist(name = "DJ Horizon"),
      selectedTab = ArtistTab.SoundCloud,
      onTabSelected = {},
      rateLimitBlockedUntil = null,
      onFetchSoundCloud = {},
      onFetchMixcloud = {},
      onSoundCloudProfileClick = {},
      onMixcloudProfileClick = {},
    )
  }
}
