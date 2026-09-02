package com.chani.reptap.ui.builder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chani.reptap.data.local.dao.WorkoutExerciseWithName
import com.chani.reptap.data.local.entity.ExerciseTrackingType
import com.chani.reptap.ui.components.RepTapTopAppBar
import kotlinx.coroutines.launch


@Composable
fun WorkoutBuilderScreen(
    viewModel: WorkoutBuilderViewModel,
    onBack: () -> Unit,
    onExerciseClick:
        (WorkoutExerciseWithName) -> Unit,
    onAddExercise: () -> Unit
) {
    val workoutDay by
    viewModel.workoutDay
        .collectAsStateWithLifecycle()

    val exercises by
    viewModel.exercises
        .collectAsStateWithLifecycle()

    val isSavingOrder by
    viewModel.isSavingOrder
        .collectAsStateWithLifecycle()

    val orderSaveError by
    viewModel.orderSaveError
        .collectAsStateWithLifecycle()

    val listState =
        rememberLazyListState()

    val coroutineScope =
        rememberCoroutineScope()

    /*
     * While Reorder mode is active this becomes our
     * temporary visual order.
     *
     * Room is updated only when Done is pressed.
     */
    var displayedExercises by remember {
        mutableStateOf<
                List<WorkoutExerciseWithName>
                >(emptyList())
    }

    var isReorderMode by remember {
        mutableStateOf(false)
    }

    var draggedExerciseId by remember {
        mutableStateOf<Long?>(null)
    }

    var draggedOffsetY by remember {
        mutableFloatStateOf(0f)
    }

    /*
     * Keeps the optimistic order on screen while waiting
     * for Room's reactive query to emit the saved order.
     */
    var pendingSavedOrderIds by remember {
        mutableStateOf<List<Long>?>(null)
    }

    var showWorkoutMenu by remember {
        mutableStateOf(false)
    }

    var showRenameDialog by remember {
        mutableStateOf(false)
    }

    var renameText by remember {
        mutableStateOf("")
    }

    LaunchedEffect(
        exercises,
        isReorderMode,
        pendingSavedOrderIds
    ) {
        if (!isReorderMode) {
            val pending =
                pendingSavedOrderIds

            val databaseIds =
                exercises.map { it.id }

            if (
                pending == null ||
                databaseIds == pending
            ) {
                displayedExercises =
                    exercises

                if (
                    pending != null &&
                    databaseIds == pending
                ) {
                    pendingSavedOrderIds =
                        null
                }
            }
        }
    }

    fun cancelReorder() {
        draggedExerciseId = null
        draggedOffsetY = 0f
        displayedExercises = exercises
        pendingSavedOrderIds = null
        isReorderMode = false
        viewModel.clearOrderSaveError()
    }

    BackHandler(
        enabled = isReorderMode
    ) {
        cancelReorder()
    }

    Scaffold(
        topBar = {
            RepTapTopAppBar(
                title =
                    workoutDay?.name
                        ?: "Workout Builder",

                onBack = {
                    if (isReorderMode) {
                        cancelReorder()
                    } else {
                        onBack()
                    }
                },

                actions = {
                    if (!isReorderMode) {
                        Box {
                            IconButton(
                                onClick = {
                                    showWorkoutMenu =
                                        true
                                }
                            ) {
                                Icon(
                                    imageVector =
                                        Icons.Default
                                            .MoreVert,
                                    contentDescription =
                                        "Workout options"
                                )
                            }

                            DropdownMenu(
                                expanded =
                                    showWorkoutMenu,
                                onDismissRequest = {
                                    showWorkoutMenu =
                                        false
                                }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Rename workout"
                                        )
                                    },
                                    onClick = {
                                        showWorkoutMenu =
                                            false

                                        renameText =
                                            workoutDay
                                                ?.name
                                                .orEmpty()

                                        showRenameDialog =
                                            true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->

        LazyColumn(
            state = listState,
            modifier =
                Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = 12.dp,
                    top =
                        innerPadding
                            .calculateTopPadding() +
                                8.dp,
                    end = 12.dp,
                    bottom = 12.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            /*
             * Header + Reorder/Done action.
             */
            item {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Text(
                        text =
                            "Workout Exercises",
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        fontWeight =
                            FontWeight.Bold
                    )

                    if (
                        displayedExercises.size > 1
                    ) {
                        TextButton(
                            onClick = {
                                if (
                                    !isReorderMode
                                ) {
                                    displayedExercises =
                                        exercises

                                    viewModel
                                        .clearOrderSaveError()

                                    isReorderMode =
                                        true
                                } else {
                                    val orderedIds =
                                        displayedExercises
                                            .map { it.id }

                                    coroutineScope.launch {
                                        val success =
                                            viewModel
                                                .saveExerciseOrder(
                                                    orderedIds
                                                )

                                        if (success) {
                                            /*
                                             * Keep showing
                                             * the locally
                                             * reordered list
                                             * until Room emits
                                             * the same order.
                                             */
                                            pendingSavedOrderIds =
                                                orderedIds

                                            draggedExerciseId =
                                                null

                                            draggedOffsetY =
                                                0f

                                            isReorderMode =
                                                false
                                        }
                                    }
                                }
                            },
                            enabled =
                                !isSavingOrder
                        ) {
                            if (
                                isSavingOrder &&
                                isReorderMode
                            ) {
                                CircularProgressIndicator(
                                    modifier =
                                        Modifier.size(
                                            18.dp
                                        ),
                                    strokeWidth =
                                        2.dp
                                )

                                Spacer(
                                    Modifier.width(
                                        8.dp
                                    )
                                )
                            }

                            Text(
                                if (isReorderMode) {
                                    "Done"
                                } else {
                                    "Reorder"
                                }
                            )
                        }
                    }
                }
            }

            /*
             * Structural changes are disabled while
             * ordering is being edited.
             */
            if (!isReorderMode) {
                item {
                    FilledTonalButton(
                        onClick =
                            onAddExercise,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.Add,
                            contentDescription =
                                null
                        )

                        Spacer(
                            modifier =
                                Modifier.width(
                                    8.dp
                                )
                        )

                        Text(
                            "Add Exercise"
                        )
                    }
                }
            }

            orderSaveError?.let {
                    error ->
                item {
                    Text(
                        text = error,
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }
            }

            itemsIndexed(
                items =
                    displayedExercises,
                key = {
                        _,
                        exercise ->
                    exercise.id
                }
            ) {
                    index,
                    exercise ->

                val isDragging =
                    draggedExerciseId ==
                            exercise.id

                val dragHandleModifier =
                    if (isReorderMode) {
                        Modifier
                            .size(48.dp)
                            .pointerInput(
                                exercise.id
                            ) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedExerciseId =
                                            exercise.id

                                        draggedOffsetY =
                                            0f
                                    },

                                    onDragCancel = {
                                        draggedExerciseId =
                                            null

                                        draggedOffsetY =
                                            0f
                                    },

                                    onDragEnd = {
                                        draggedExerciseId =
                                            null

                                        draggedOffsetY =
                                            0f
                                    },

                                    onDrag = {
                                            change,
                                            dragAmount ->

                                        change.consume()

                                        draggedOffsetY +=
                                            dragAmount.y

                                        val layoutInfo =
                                            listState
                                                .layoutInfo

                                        val draggedInfo =
                                            layoutInfo
                                                .visibleItemsInfo
                                                .firstOrNull {
                                                    it.key ==
                                                            exercise.id
                                                }

                                        if (
                                            draggedInfo !=
                                            null
                                        ) {
                                            /*
                                             * layoutInfo does
                                             * not include our
                                             * visual translation,
                                             * so add the drag
                                             * offset manually.
                                             */
                                            val draggedCenter =
                                                draggedInfo.offset +
                                                        draggedInfo.size /
                                                        2f +
                                                        draggedOffsetY

                                            val targetInfo =
                                                layoutInfo
                                                    .visibleItemsInfo
                                                    .firstOrNull {
                                                            info ->

                                                        info.key !=
                                                                exercise.id &&
                                                                draggedCenter >=
                                                                info.offset &&
                                                                draggedCenter <=
                                                                info.offset +
                                                                info.size
                                                    }

                                            if (
                                                targetInfo !=
                                                null
                                            ) {
                                                val fromIndex =
                                                    displayedExercises
                                                        .indexOfFirst {
                                                            it.id ==
                                                                    exercise.id
                                                        }

                                                val targetId =
                                                    targetInfo.key
                                                            as? Long

                                                val toIndex =
                                                    if (
                                                        targetId !=
                                                        null
                                                    ) {
                                                        displayedExercises
                                                            .indexOfFirst {
                                                                it.id ==
                                                                        targetId
                                                            }
                                                    } else {
                                                        -1
                                                    }

                                                if (
                                                    fromIndex >=
                                                    0 &&
                                                    toIndex >=
                                                    0 &&
                                                    fromIndex !=
                                                    toIndex
                                                ) {
                                                    /*
                                                     * Keep the
                                                     * dragged card
                                                     * approximately
                                                     * under the
                                                     * finger after
                                                     * its underlying
                                                     * list position
                                                     * changes.
                                                     */
                                                    val layoutDelta =
                                                        targetInfo.offset -
                                                                draggedInfo.offset

                                                    displayedExercises =
                                                        displayedExercises
                                                            .toMutableList()
                                                            .apply {
                                                                val moved =
                                                                    removeAt(
                                                                        fromIndex
                                                                    )

                                                                add(
                                                                    toIndex,
                                                                    moved
                                                                )
                                                            }

                                                    draggedOffsetY -=
                                                        layoutDelta
                                                            .toFloat()
                                                }
                                            }

                                            /*
                                             * Gentle automatic
                                             * scrolling near the
                                             * visible edges.
                                             */
                                            val edgeThreshold =
                                                72.dp.toPx()

                                            val draggedTop =
                                                draggedInfo.offset +
                                                        draggedOffsetY

                                            val draggedBottom =
                                                draggedTop +
                                                        draggedInfo.size

                                            val scrollAmount =
                                                when {
                                                    draggedTop <
                                                            layoutInfo
                                                                .viewportStartOffset +
                                                            edgeThreshold -> {
                                                        -18f
                                                    }

                                                    draggedBottom >
                                                            layoutInfo
                                                                .viewportEndOffset -
                                                            edgeThreshold -> {
                                                        18f
                                                    }

                                                    else -> {
                                                        0f
                                                    }
                                                }

                                            if (
                                                scrollAmount !=
                                                0f
                                            ) {
                                                coroutineScope
                                                    .launch {
                                                        listState
                                                            .scrollBy(
                                                                scrollAmount
                                                            )
                                                    }
                                            }
                                        }
                                    }
                                )
                            }
                    } else {
                        Modifier
                    }

                WorkoutBuilderExerciseCard(
                    exercise =
                        exercise,
                    displayOrder =
                        index + 1,
                    isReorderMode =
                        isReorderMode,
                    isDragging =
                        isDragging,
                    draggedOffsetY =
                        if (isDragging) {
                            draggedOffsetY
                        } else {
                            0f
                        },
                    dragHandleModifier =
                        dragHandleModifier,
                    onClick = {
                        if (
                            !isReorderMode
                        ) {
                            onExerciseClick(
                                exercise
                            )
                        }
                    }
                )
            }

            if (
                workoutDay != null &&
                displayedExercises.isEmpty()
            ) {
                item {
                    Text(
                        text =
                            "No exercises in this workout",
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = {
                showRenameDialog =
                    false
            },

            title = {
                Text("Rename workout")
            },

            text = {
                OutlinedTextField(
                    value =
                        renameText,
                    onValueChange = {
                        renameText =
                            it
                    },
                    label = {
                        Text(
                            "Workout name"
                        )
                    },
                    singleLine = true
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val success =
                                viewModel
                                    .renameWorkout(
                                        renameText
                                    )

                            if (success) {
                                showRenameDialog =
                                    false
                            }
                        }
                    },
                    enabled =
                        renameText
                            .trim()
                            .isNotEmpty()
                ) {
                    Text("Save")
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        showRenameDialog =
                            false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun WorkoutBuilderExerciseCard(
    exercise: WorkoutExerciseWithName,
    displayOrder: Int,
    isReorderMode: Boolean,
    isDragging: Boolean,
    draggedOffsetY: Float,
    dragHandleModifier: Modifier,
    onClick: () -> Unit
) {
    val targetText =
        when (exercise.trackingType) {
            ExerciseTrackingType.REPS -> {
                "${exercise.targetSets} sets · " +
                        "${exercise.targetReps} reps"
            }

            ExerciseTrackingType.TIME -> {
                "${exercise.targetSets} sets · " +
                        "${exercise.targetDurationSec ?: 0} sec"
            }
        }

    val loadText =
        exercise.defaultWeightKg
            ?.let { weight ->
                if (
                    weight ==
                    weight.toInt().toFloat()
                ) {
                    "${weight.toInt()}kg"
                } else {
                    "${weight}kg"
                }
            }
            ?: "Bodyweight"

    val sideText =
        if (exercise.isPerSide) {
            " · each side"
        } else {
            ""
        }

    val cardModifier =
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY =
                    draggedOffsetY

                if (isDragging) {
                    /*
                     * Very subtle visual lift without
                     * introducing a new colour.
                     */
                    scaleX = 1.01f
                    scaleY = 1.01f
                }
            }
            .zIndex(
                if (isDragging) {
                    1f
                } else {
                    0f
                }
            )
            .then(
                if (!isReorderMode) {
                    Modifier.clickable(
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )

    Card(
        modifier =
            cardModifier
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 14.dp
            )
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    text =
                        "$displayOrder. " +
                                exercise.exerciseName,
                    style =
                        MaterialTheme
                            .typography
                            .titleSmall,
                    fontWeight =
                        FontWeight.Bold,
                    modifier =
                        Modifier.weight(1f)
                )

                if (isReorderMode) {
                    /*
                     * Material Icons Core does not give us
                     * a dedicated drag handle reliably
                     * across all current dependency setups,
                     * so use a simple typographic grip
                     * rather than adding another library.
                     */
                    Box(
                        modifier =
                            dragHandleModifier,
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            text = "≡",
                            fontSize = 26.sp,
                            fontWeight =
                                FontWeight.Bold,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                } else {
                    Icon(
                        imageVector =
                            Icons.AutoMirrored
                                .Filled
                                .KeyboardArrowRight,
                        contentDescription =
                            null,
                        tint =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text =
                    "$targetText · " +
                            "$loadText$sideText",
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Text(
                text =
                    "Rest: " +
                            "${exercise.restBetweenSetsSec}s between sets" +
                            " · " +
                            "${exercise.restBeforeNextSec}s before next",
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}