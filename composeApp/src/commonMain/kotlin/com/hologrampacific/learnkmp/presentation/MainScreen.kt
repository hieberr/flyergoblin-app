package com.hologrampacific.learnkmp.presentation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import com.hologrampacific.learnkmp.email.presentation.EmailRoutes.Companion.emailEntryBuilder
import com.hologrampacific.learnkmp.email.presentation.EmailScreen
import com.hologrampacific.learnkmp.flyer.presentation.flyer.FlyerScreen
import com.hologrampacific.learnkmp.flyer.presentation.flyerEntryBuilder

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
  val navigator = AppNavigator(backStack)

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val currentRoute = remember { derivedStateOf { backStack.lastOrNull() } }
    val isTopLevelRoute = remember { derivedStateOf { currentRoute.value in TopLevelRoutes.entries } }

    val isCompact = maxWidth < COMPACT_WIDTH_BREAKPOINT
    if (isCompact) {
      Scaffold(
        bottomBar = {
          if (isTopLevelRoute.value) {
            NavigationBar {
              TopLevelNavigationItem.entries.forEach {
                NavigationBarItem(
                  icon = { Text(it.iconLabel, style = MaterialTheme.typography.titleLarge) },
                  label = { Text(it.title) },
                  selected = currentRoute.value == it.route,
                  onClick = { navigator.goTo(it.route) },
                )
              }
            }
          }
        }
      ) { padding ->
        AppNavDisplay(backStack = backStack, modifier = Modifier.padding(padding))
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
                onClick = { navigator.goTo(it.route) },
              )
            }
          }
        }
        AppNavDisplay(backStack = backStack)
      }
    }
  }
}

@Composable
fun AppNavDisplay(backStack: NavBackStack<NavKey>, modifier: Modifier = Modifier) {
  val navigator = AppNavigator(backStack)
  NavDisplay(
    backStack = backStack,
    onBack = { navigator.goBack() },
    modifier = modifier,
    entryDecorators =
      listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
      ),
    entryProvider =
      entryProvider {
        entry<TopLevelRoutes.About> { AboutScreen() }
        entry<TopLevelRoutes.Flyer> { FlyerScreen(navigator) }
        entry<TopLevelRoutes.Email> { EmailScreen(navigator) }

        emailEntryBuilder(navigator)
        flyerEntryBuilder(navigator)
      },
  )
}
