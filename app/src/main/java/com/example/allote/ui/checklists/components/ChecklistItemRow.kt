package com.example.allote.ui.checklists.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.allote.data.ChecklistItem
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistItemRow(
    item: ChecklistItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStartDrag: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 100.dp.toPx() }
    val scope = rememberCoroutineScope()
    
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offset"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (item.isDone) 0.95f else 1f,
        animationSpec = tween(200),
        label = "scale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            offsetX < -swipeThreshold -> MaterialTheme.colorScheme.errorContainer
            offsetX > swipeThreshold -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(300),
        label = "backgroundColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
    ) {
        // Background actions
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left action - Edit
            AnimatedVisibility(
                visible = offsetX > swipeThreshold / 2,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Right action - Delete
            AnimatedVisibility(
                visible = offsetX < -swipeThreshold / 2,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Main content
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .scale(scale)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                when {
                                    offsetX < -swipeThreshold -> {
                                        onDelete()
                                        offsetX = 0f
                                    }
                                    offsetX > swipeThreshold -> {
                                        onEdit()
                                        offsetX = 0f
                                    }
                                    else -> offsetX = 0f
                                }
                            }
                        }
                    ) { _, dragAmount ->
                        offsetX = (offsetX + dragAmount).coerceIn(-swipeThreshold * 1.5f, swipeThreshold * 1.5f)
                    }
                },
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Checkbox with animation
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Checkbox(
                        checked = item.isDone,
                        onCheckedChange = { onToggle() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                
                // Text with strikethrough animation
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
                    color = if (item.isDone) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // Drag handle
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Reordenar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { alpha = 0.6f }
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { onStartDrag() },
                                onDragEnd = { onDragEnd() }
                            ) { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.y)
                            }
                        }
                )
            }
        }
    }
}
