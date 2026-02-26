package com.hologrampacific.flyergoblin.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import com.hologrampacific.flyergoblin.presentation.Ui
import flyergoblin.composeapp.generated.resources.Res
import flyergoblin.composeapp.generated.resources.remove_24px
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/**
 * A container that adds a left-swipe gesture to reveal a red delete button beneath [content].
 *
 * Swiping left past the midpoint snaps the item open. Tapping the revealed delete button calls
 * [onDelete]. The parent coordinates "only one item open at a time" by tracking which item is open
 * and passing that down via [isOpen] / [onSwipeOpen]:
 * ```
 * var openItemId by remember { mutableStateOf<Long?>(null) }
 *
 * items(list, key = { it.id }) { item ->
 *     val isOpen = openItemId == item.id
 *     SwipeToDeleteBox(
 *         isOpen = isOpen,
 *         onSwipeOpen = { openItemId = item.id },
 *         onDelete = { openItemId = null; deleteItem(item.id) },
 *         modifier = Modifier.animateItem(),
 *     ) {
 *         MyItemCard(
 *             onClick = { if (isOpen) openItemId = null else navigate(item.id) },
 *         )
 *     }
 * }
 * ```
 */
@Composable
fun SwipeToDeleteBox(
  isOpen: Boolean,
  onSwipeOpen: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  val deleteButtonWidth = Ui.unit * 5
  val density = LocalDensity.current
  val deleteButtonWidthPx = with(density) { deleteButtonWidth.toPx() }
  val offsetX = remember { Animatable(0f) }
  val scope = rememberCoroutineScope()
  val currentOnSwipeOpen by rememberUpdatedState(onSwipeOpen)
  val currentOnDelete by rememberUpdatedState(onDelete)

  // When the parent clears the open item, animate back to closed.
  LaunchedEffect(isOpen) {
    if (!isOpen && offsetX.value != 0f) {
      offsetX.animateTo(0f, spring())
    }
  }

  Box(modifier = modifier.fillMaxWidth()) {
    // Delete button — sits behind the content and is revealed as the content slides left.
    Box(
      modifier =
        Modifier.matchParentSize()
          .clip(CardDefaults.shape)
          .background(MaterialTheme.colorScheme.error)
          .clickable(enabled = isOpen) { currentOnDelete() },
      contentAlignment = Alignment.CenterEnd,
    ) {
      Icon(
        painter = painterResource(Res.drawable.remove_24px),
        contentDescription = "Delete",
        modifier = Modifier.padding(end = Ui.unit).size(Ui.unit * 2),
        tint = MaterialTheme.colorScheme.onError,
      )
    }

    // Content foreground — offset left by the drag state to reveal the delete button underneath.
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .offset { IntOffset(offsetX.value.roundToInt(), 0) }
          .semantics {
            customActions =
              listOf(
                CustomAccessibilityAction(label = "Delete") {
                  currentOnDelete()
                  true
                }
              )
          }
          .pointerInput(Unit) {
            detectHorizontalDragGestures(
              onHorizontalDrag = { _, dragAmount ->
                scope.launch {
                  val newOffset = (offsetX.value + dragAmount).coerceIn(-deleteButtonWidthPx, 0f)
                  offsetX.snapTo(newOffset)
                }
              },
              onDragEnd = {
                scope.launch {
                  if (offsetX.value < -deleteButtonWidthPx * 0.5f) {
                    currentOnSwipeOpen()
                    offsetX.animateTo(-deleteButtonWidthPx, spring())
                  } else {
                    offsetX.animateTo(0f, spring())
                  }
                }
              },
              onDragCancel = { scope.launch { offsetX.animateTo(0f, spring()) } },
            )
          }
    ) {
      content()
    }
  }
}
