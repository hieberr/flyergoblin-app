package com.hologrampacific.flyergoblin.presentation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.hologrampacific.flyergoblin.email.presentation.EmailRoutes.Companion.emailEntryBuilder
import com.hologrampacific.flyergoblin.email.presentation.EmailScreen
import com.hologrampacific.flyergoblin.flyer.presentation.events.EventsScreen
import com.hologrampacific.flyergoblin.flyer.presentation.flyerEntryBuilder
import com.hologrampacific.flyergoblin.presentation.theme.AppTheme

// Below this screen width we switch to a compact layout.
private val COMPACT_WIDTH_BREAKPOINT = 600.dp

private interface TopLevelNavigationItem {
  val title: String
  val iconLabel: String
  val route: NavKey

  data object About : TopLevelNavigationItem {
    override val title = "About"
    override val iconLabel = "ℹ️" // Info emoji
    override val route = TopLevelRoutes.About
  }

  data object EmailList : TopLevelNavigationItem {
    override val title = "Emails"
    override val iconLabel = "✉️" // Envelope emoji
    override val route = TopLevelRoutes.Email
  }

  data object Flyer : TopLevelNavigationItem {
    override val title = "Flyer Goblin"
    override val iconLabel = "📜" // Page emoji
    override val route = TopLevelRoutes.Flyer
  }

  companion object Companion {
    /** The top level routes in order that they appear in the main screen nav bar */
    val entries: List<TopLevelNavigationItem> = listOf(Flyer, EmailList, About)
  }
}

@Composable
@Preview
fun MainScreen(modifier: Modifier = Modifier) {
  val navConfig = SavedStateConfiguration { serializersModule = appSerializersModule }
  val backStack =
    rememberNavBackStack(configuration = navConfig, elements = arrayOf(TopLevelRoutes.home))
  val navTransition = remember { mutableStateOf(NavTransition.SlideOutLeft) }
  val navigator = remember(backStack) { AppNavigator(backStack) { navTransition.value = it } }

  AppTheme {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
      val currentRoute = remember { derivedStateOf { backStack.lastOrNull() } }
      val isTopLevelRoute = remember {
        derivedStateOf { currentRoute.value in TopLevelRoutes.entries }
      }

      val isCompact = maxWidth < COMPACT_WIDTH_BREAKPOINT
      if (isCompact) {
        Scaffold(
          contentWindowInsets = WindowInsets.safeDrawing,
          bottomBar = {
            if (isTopLevelRoute.value) {
              NavigationBar(windowInsets = WindowInsets(bottom = 0.dp)) {
                TopLevelNavigationItem.entries.forEach { navItem ->
                  NavigationBarItem(
                    icon = { Text(navItem.iconLabel, style = MaterialTheme.typography.titleLarge) },
                    label = { Text(navItem.title) },
                    selected = currentRoute.value == navItem.route,
                    onClick = {
                      val newTopLevelIndex = TopLevelNavigationItem.entries.indexOf(navItem)
                      val currentTopLevelIndex =
                        TopLevelNavigationItem.entries.indexOfFirst {
                          it.route == currentRoute.value
                        }
                      val transition =
                        if (newTopLevelIndex > currentTopLevelIndex) {
                          // Moving right on the bottom bar: current screen slides out to the left
                          NavTransition.SlideOutLeft
                        } else {
                          // Moving left on the bottom bar: current screen slides out to the right
                          NavTransition.SlideOutRight
                        }
                      navigator.goTo(navItem.route, transition)
                    },
                  )
                }
              }
            }
          },
        ) { padding ->
          val layoutDirection = LocalLayoutDirection.current
          AppNavDisplay(
            backStack,
            navigator,
            { navTransition.value },
            modifier =
              Modifier.padding(
                top = padding.calculateTopPadding(),
                start = padding.calculateStartPadding(layoutDirection),
                end = padding.calculateEndPadding(layoutDirection),
                // On top-level routes the NavigationBar is visible, so apply the full bottom
                // padding to keep content above it. On sub-screens we omit bottom padding so
                // content can extend to the bottom edge of the screen.
                bottom = if (isTopLevelRoute.value) padding.calculateBottomPadding() else 0.dp,
              ),
          )
        }
      } else {
        Row(modifier = Modifier.fillMaxSize()) {
          if (isTopLevelRoute.value) {
            NavigationRail {
              TopLevelNavigationItem.entries.forEach {
                NavigationRailItem(
                  icon = { Text(it.iconLabel, style = MaterialTheme.typography.titleLarge) },
                  label = { Text(it.title) },
                  selected = currentRoute.value == it.route,
                  onClick = { navigator.goTo(it.route, NavTransition.Fade) },
                )
              }
            }
          }
          AppNavDisplay(backStack, navigator, { navTransition.value })
        }
      }
    }
  }
}

@Composable
fun AppNavDisplay(
  backStack: NavBackStack<NavKey>,
  navigator: AppNavigator,
  navTransition: () -> NavTransition,
  modifier: Modifier = Modifier,
) {
  NavDisplay(
    backStack = backStack,
    onBack = { navigator.goBack() },
    modifier = modifier,
    transitionSpec = {
      // navTransition is set by AppNavigator before every backstack change, so it reliably
      // reflects direction even when NavDisplay mistakenly calls transitionSpec during
      // a rapid second back press (a known alpha-06 issue).
      when (navTransition()) {
        NavTransition.SlideOutRight ->
          slideInHorizontally { -it } togetherWith slideOutHorizontally { it }

        NavTransition.SlideOutLeft ->
          slideInHorizontally { it } togetherWith slideOutHorizontally { -it }

        NavTransition.Fade -> fadeIn() togetherWith fadeOut()
      }
    },
    popTransitionSpec = {
      if (navTransition() == NavTransition.Fade) {
        fadeIn() togetherWith fadeOut()
      } else {
        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
      }
    },
    entryDecorators =
      listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
      ),
    entryProvider =
      entryProvider {
        entry<TopLevelRoutes.About> { AboutScreen() }
        entry<TopLevelRoutes.Flyer> { EventsScreen(navigator) }
        entry<TopLevelRoutes.Email> { EmailScreen(navigator) }

        emailEntryBuilder(navigator)
        flyerEntryBuilder(navigator)
      },
  )
}
