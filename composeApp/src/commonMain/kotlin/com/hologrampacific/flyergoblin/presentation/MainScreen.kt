package com.hologrampacific.flyergoblin.presentation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.hologrampacific.flyergoblin.flyer.presentation.EditEvent
import com.hologrampacific.flyergoblin.flyer.presentation.events.EventsScreen
import com.hologrampacific.flyergoblin.flyer.presentation.flyerEntryBuilder
import com.hologrampacific.flyergoblin.presentation.theme.AppTheme
import com.hologrampacific.flyergoblin.sharing.SharedImageProvider
import flyergoblin.composeapp.generated.resources.Res
import flyergoblin.composeapp.generated.resources.event_list_24px
import flyergoblin.composeapp.generated.resources.settings_24px
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

// Below this screen width we switch to a compact layout.
private val COMPACT_WIDTH_BREAKPOINT = 600.dp

private interface TopLevelNavigationItem {
  val title: String
  val icon: @Composable () -> Unit
  val route: NavKey

  data object Settings : TopLevelNavigationItem {
    override val title = "Settings"
    override val icon: @Composable () -> Unit = {
      Icon(
        painter = painterResource(Res.drawable.settings_24px),
        contentDescription = "Settings",
        modifier = Modifier.size(Ui.standardIconSize),
      )
    }
    override val route = TopLevelRoutes.Settings
  }

  data object EmailList : TopLevelNavigationItem {
    override val title = "Emails"
    override val icon: @Composable () -> Unit = {
      Text("✉️", style = MaterialTheme.typography.titleLarge)
    }
    override val route = TopLevelRoutes.Email
  }

  data object Flyer : TopLevelNavigationItem {
    override val title = "Events"
    override val icon: @Composable () -> Unit = {
      Icon(
        painter = painterResource(Res.drawable.event_list_24px),
        contentDescription = "Events",
        modifier = Modifier.size(Ui.standardIconSize),
      )
    }
    override val route = TopLevelRoutes.Flyer
  }

  companion object Companion {
    /** The top level routes in order that they appear in the main screen nav bar */
    val entries: List<TopLevelNavigationItem> = listOf(Flyer, EmailList, Settings)
  }
}

@Composable
@Preview
fun MainScreen(modifier: Modifier = Modifier) {
  val sharedImageProvider: SharedImageProvider = koinInject()
  val navConfig = SavedStateConfiguration { serializersModule = appSerializersModule }
  val backStack =
    rememberNavBackStack(configuration = navConfig, elements = arrayOf(TopLevelRoutes.home))
  val navTransition = remember { mutableStateOf(NavTransition.SlideOutLeft) }
  val navigator = remember(backStack) { AppNavigator(backStack) { navTransition.value = it } }

  LaunchedEffect(Unit) {
    sharedImageProvider.navigationEvent.collect {
      navigator.goTo(EditEvent(null), NavTransition.SlideOutLeft)
    }
  }

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
              // Set the bottom inset to give just a little padding. It almost looks good at 0.dp,
              // but it's a little close to the bottom line on ios and android.
              NavigationBar(windowInsets = WindowInsets(bottom = Ui.halfUnit)) {
                TopLevelNavigationItem.entries.forEach { navItem ->
                  NavigationBarItem(
                    icon = { navItem.icon() },
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
          // On top-level routes the NavigationBar is visible, so apply the full bottom
          // padding to keep content above it. On sub-screens we omit bottom padding so
          // sub-screens can apply safeDrawing bottom inset themselves (e.g. around buttons).
          val appliedBottom = if (isTopLevelRoute.value) padding.calculateBottomPadding() else 0.dp
          val appliedPadding =
            PaddingValues(
              top = padding.calculateTopPadding(),
              start = padding.calculateStartPadding(layoutDirection),
              end = padding.calculateEndPadding(layoutDirection),
              bottom = appliedBottom,
            )
          AppNavDisplay(
            backStack,
            navigator,
            { navTransition.value },
            modifier = Modifier.padding(appliedPadding).consumeWindowInsets(appliedPadding),
          )
        }
      } else {
        Row(modifier = Modifier.fillMaxSize()) {
          if (isTopLevelRoute.value) {
            NavigationRail {
              TopLevelNavigationItem.entries.forEach {
                NavigationRailItem(
                  icon = { it.icon() },
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
    modifier = modifier.imePadding(),
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
        entry<TopLevelRoutes.Settings> { SettingsScreen() }
        entry<TopLevelRoutes.Flyer> { EventsScreen(navigator) }
        entry<TopLevelRoutes.Email> { EmailScreen(navigator) }

        emailEntryBuilder(navigator)
        flyerEntryBuilder(navigator)
      },
  )
}
