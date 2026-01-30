package com.hologrampacific.learnkmp.presentation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

interface Navigator {
  fun goBack() {}

  fun goTo(route: NavKey) {}

  fun popAndGoTo(route: NavKey) {}

  companion object {
    val noOpNavigator = object : Navigator {}
  }
}

class AppNavigator(val backStack: NavBackStack<NavKey>) : Navigator {

  override fun goBack() {
    backStack.removeLastOrNull()
  }

  override fun goTo(route: NavKey) {

    // Don't navigate if already on the screen
    if (backStack.lastOrNull() == route) return

    // For bottom navigation pattern: clear back stack to home and add new destination
    // This prevents building up a large back stack when switching between tabs
    if (route in TopLevelRoutes.entries) {
      // Remove all items except Home
      while (backStack.size > 1) {
        backStack.removeLast()
      }
      // Add the new screen if it's not Home
      if (route != TopLevelRoutes.home) {
        backStack.add(route)
      }
    } else {
      // For other screens, just add to the back stack
      backStack.add(route)
    }
  }

  override fun popAndGoTo(route: NavKey) {
    // Remove the current screen from the backstack
    backStack.removeLastOrNull()
    // Navigate to the new screen
    backStack.add(route)
  }
}
