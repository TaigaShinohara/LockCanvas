package com.lockcanvas.app.ui.onboarding

import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lockcanvas.app.engine.CanvasSceneRenderer
import com.lockcanvas.app.model.CanvasDesign
import com.lockcanvas.app.model.DynamicDataSnapshot
import com.lockcanvas.app.model.PresetDesigns
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    initialGoal: OnboardingGoal,
    liveSnapshot: DynamicDataSnapshot,
    onCompleteToEditor: (OnboardingGoal, Boolean) -> Unit,
    onTryLater: (OnboardingGoal) -> Unit,
    onSkip: (OnboardingGoal) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val reducedMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }

    var selectedGoal by remember { mutableStateOf(initialGoal) }
    var tryDragOffsetX by remember { mutableFloatStateOf(0f) }
    var tryDragOffsetY by remember { mutableFloatStateOf(0f) }
    var trySucceeded by remember { mutableStateOf(false) }

    val pages = remember(selectedGoal) { OnboardingPageOrder.buildPages(selectedGoal) }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val praiseRotator = remember { PraiseRotator() }

    var activePraise by remember { mutableStateOf<String?>(null) }
    var pendingAutoAdvanceJob by remember { mutableStateOf<Job?>(null) }
    var isCompleting by remember { mutableStateOf(false) }

    val renderer = remember(context) { CanvasSceneRenderer(context.applicationContext) }
    DisposableEffect(renderer) {
        onDispose { renderer.clearImageCache() }
    }

    val currentRole = pages.getOrElse(pagerState.currentPage) { OnboardingPageRole.HOOK }

    fun cancelAutoAdvance() {
        pendingAutoAdvanceJob?.cancel()
        pendingAutoAdvanceJob = null
    }

    fun navigateToPage(target: Int) {
        cancelAutoAdvance()
        activePraise = null
        val clamped = target.coerceIn(0, pages.lastIndex)
        scope.launch {
            if (reducedMotion) {
                pagerState.scrollToPage(clamped)
            } else {
                pagerState.animateScrollToPage(
                    page = clamped,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                )
            }
        }
    }

    fun triggerGoalSelection(goal: OnboardingGoal) {
        if (pendingAutoAdvanceJob != null) return
        selectedGoal = goal
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        activePraise = praiseRotator.nextGoalPraise()
        val nextIndex = (pagerState.currentPage + 1).coerceAtMost(pages.lastIndex)
        pendingAutoAdvanceJob = scope.launch {
            delay(1000L)
            activePraise = null
            if (reducedMotion) {
                pagerState.scrollToPage(nextIndex)
            } else {
                pagerState.animateScrollToPage(
                    page = nextIndex,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                )
            }
            pendingAutoAdvanceJob = null
        }
    }

    fun triggerTrySuccess() {
        if (trySucceeded || pendingAutoAdvanceJob != null) return
        trySucceeded = true
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        activePraise = praiseRotator.nextTryPraise()
        val nextIndex = (pagerState.currentPage + 1).coerceAtMost(pages.lastIndex)
        pendingAutoAdvanceJob = scope.launch {
            delay(1050L)
            activePraise = null
            if (reducedMotion) {
                pagerState.scrollToPage(nextIndex)
            } else {
                pagerState.animateScrollToPage(
                    page = nextIndex,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                )
            }
            pendingAutoAdvanceJob = null
        }
    }

    fun triggerComplete() {
        if (isCompleting) return
        isCompleting = true
        cancelAutoAdvance()
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        activePraise = praiseRotator.nextCompletionPraise()
        scope.launch {
            delay(650L)
            onCompleteToEditor(selectedGoal, trySucceeded)
        }
    }

    BackHandler(enabled = pagerState.currentPage > 0) {
        navigateToPage(pagerState.currentPage - 1)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding(),
        ) {
            // Top bar: Back (left), Window Dots (center), Skip (right) - always in the same spot
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(modifier = Modifier.width(96.dp), contentAlignment = Alignment.CenterStart) {
                    if (pagerState.currentPage > 0) {
                        IconButton(onClick = { navigateToPage(pagerState.currentPage - 1) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "前のページへ戻る",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                WindowDotIndicator(
                    currentPage = pagerState.currentPage,
                    totalPages = pages.size,
                    chapterLabel = currentRole.chapterLabel,
                )

                Box(modifier = Modifier.width(96.dp), contentAlignment = Alignment.CenterEnd) {
                    if (currentRole != OnboardingPageRole.CONNECT) {
                        TextButton(
                            onClick = {
                                cancelAutoAdvance()
                                onSkip(selectedGoal)
                            },
                        ) {
                            Text(
                                text = "スキップ",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }

            // Main Pager (standard HorizontalPager)
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = currentRole != OnboardingPageRole.TRY_DRAG,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { pageIndex ->
                val role = pages[pageIndex]
                val isCurrentPage = pagerState.currentPage == pageIndex

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .let { mod ->
                            if (role != OnboardingPageRole.GOAL_CHOICE &&
                                role != OnboardingPageRole.TRY_DRAG &&
                                role != OnboardingPageRole.CONNECT
                            ) {
                                mod.clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    navigateToPage(pageIndex + 1)
                                }
                            } else if (pendingAutoAdvanceJob != null) {
                                mod.clickable { navigateToPage(pageIndex + 1) }
                            } else {
                                mod
                            }
                        }
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when (role) {
                        OnboardingPageRole.HOOK -> HookPage(
                            isCurrentPage = isCurrentPage,
                            reducedMotion = reducedMotion,
                        )
                        OnboardingPageRole.GOAL_CHOICE -> GoalChoicePage(
                            selectedGoal = if (pendingAutoAdvanceJob != null) selectedGoal else null,
                            praiseText = activePraise,
                            reducedMotion = reducedMotion,
                            onSelectGoal = ::triggerGoalSelection,
                        )
                        OnboardingPageRole.MATCH_RESULT -> MatchResultPage(
                            goal = selectedGoal,
                            isCurrentPage = isCurrentPage,
                            reducedMotion = reducedMotion,
                            renderer = renderer,
                            snapshot = liveSnapshot,
                        )
                        OnboardingPageRole.IDENTITY -> IdentityPage(
                            renderer = renderer,
                            snapshot = liveSnapshot,
                            goal = selectedGoal,
                        )
                        OnboardingPageRole.VALUE_DYNAMIC -> ValueDynamicPage(
                            renderer = renderer,
                            snapshot = liveSnapshot,
                            isCurrentPage = isCurrentPage,
                            reducedMotion = reducedMotion,
                        )
                        OnboardingPageRole.VALUE_SAFE_AREA -> ValueSafeAreaPage(
                            renderer = renderer,
                            snapshot = liveSnapshot,
                        )
                        OnboardingPageRole.STEP_1 -> Step1Page(
                            isCurrentPage = isCurrentPage,
                            reducedMotion = reducedMotion,
                        )
                        OnboardingPageRole.STEP_2 -> Step2Page(
                            isCurrentPage = isCurrentPage,
                            reducedMotion = reducedMotion,
                        )
                        OnboardingPageRole.STEP_3 -> Step3Page()
                        OnboardingPageRole.TRY_DRAG -> TryDragPage(
                            offsetX = tryDragOffsetX,
                            offsetY = tryDragOffsetY,
                            succeeded = trySucceeded,
                            praiseText = activePraise,
                            reducedMotion = reducedMotion,
                            onDrag = { dx, dy ->
                                tryDragOffsetX = (tryDragOffsetX + dx).coerceIn(-220f, 220f)
                                tryDragOffsetY = (tryDragOffsetY + dy).coerceIn(-220f, 220f)
                            },
                            onDragFinished = {
                                if (hypot(tryDragOffsetX.toDouble(), tryDragOffsetY.toDouble()) >= 48.0) {
                                    triggerTrySuccess()
                                }
                            },
                        )
                        OnboardingPageRole.TRY_RESULT -> TryResultPage(
                            offsetX = tryDragOffsetX,
                            offsetY = tryDragOffsetY,
                            isCurrentPage = isCurrentPage,
                            reducedMotion = reducedMotion,
                        )
                        OnboardingPageRole.CONNECT -> ConnectPage(
                            goal = selectedGoal,
                            renderer = renderer,
                            snapshot = liveSnapshot,
                            praiseText = activePraise,
                            isCompleting = isCompleting,
                            reducedMotion = reducedMotion,
                        )
                    }
                }
            }

            // Bottom action area (Edge-to-Edge to screen bottom, zero gap)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (currentRole) {
                        OnboardingPageRole.GOAL_CHOICE -> {
                            // No "Next" button on choice page per mobile_onboarding rule
                            Text(
                                text = "ひとつ選ぶと、次へ進みます",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 14.dp),
                            )
                        }
                        OnboardingPageRole.TRY_DRAG -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TextButton(
                                    onClick = {
                                        cancelAutoAdvance()
                                        onTryLater(selectedGoal)
                                    },
                                    modifier = Modifier.height(48.dp),
                                ) {
                                    Text(
                                        text = "あとで試す",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        navigateToPage(pagerState.currentPage + 1)
                                    },
                                    modifier = Modifier.height(48.dp),
                                ) {
                                    Text("次へ進む")
                                }
                            }
                        }
                        OnboardingPageRole.CONNECT -> {
                            Text(
                                text = selectedGoal.ctaSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            Button(
                                onClick = ::triggerComplete,
                                enabled = !isCompleting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text(
                                    text = "デザインを編集してみる",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                        else -> {
                            BounceNextButton(
                                label = "次へ",
                                reducedMotion = reducedMotion,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    navigateToPage(pagerState.currentPage + 1)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WindowDotIndicator(
    currentPage: Int,
    totalPages: Int,
    chapterLabel: String,
) {
    val dots = remember(currentPage, totalPages) {
        WindowDotCalculator.compute(currentPage, totalPages)
    }
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = chapterLabel
        },
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        dots.forEach { dot ->
            val width by animateDpAsState(
                targetValue = when {
                    dot.isCurrent -> 22.dp
                    dot.isEdgeSmall -> 5.dp
                    else -> 8.dp
                },
                animationSpec = tween(220),
                label = "dotWidth",
            )
            val height by animateDpAsState(
                targetValue = if (dot.isEdgeSmall) 5.dp else 8.dp,
                animationSpec = tween(220),
                label = "dotHeight",
            )
            Box(
                modifier = Modifier
                    .size(width = width, height = height)
                    .clip(CircleShape)
                    .background(
                        if (dot.isCurrent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = if (dot.isEdgeSmall) 0.45f else 0.7f)
                        },
                    ),
            )
        }
    }
}

@Composable
private fun BounceNextButton(
    label: String,
    reducedMotion: Boolean,
    onClick: () -> Unit,
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    Button(
        onClick = {
            if (!reducedMotion) {
                scope.launch {
                    scale.snapTo(0.96f)
                    scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
            }
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale.value),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun HookPage(
    isCurrentPage: Boolean,
    reducedMotion: Boolean,
) {
    val alpha by animateFloatAsState(
        targetValue = if (isCurrentPage) 1f else 0.3f,
        animationSpec = tween(if (reducedMotion) 120 else 420),
        label = "hookAlpha",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Surface(
            modifier = Modifier.size(144.dp),
            shape = RoundedCornerShape(36.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Smartphone,
                    contentDescription = "スマートフォンのロック画面",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp),
                )
            }
        }
        Text(
            text = "ロック画面、そのままになっていませんか",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "1日に何十回も目にする画面を、自分だけの心地よい場所に。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GoalChoicePage(
    selectedGoal: OnboardingGoal?,
    praiseText: String?,
    reducedMotion: Boolean,
    onSelectGoal: (OnboardingGoal) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "ロック画面で、いちばん叶えたいことは？",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        OnboardingGoal.entries.forEach { goal ->
            val isChosen = selectedGoal == goal
            val scale by animateFloatAsState(
                targetValue = if (isChosen && !reducedMotion) 1.03f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "choiceScale",
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = selectedGoal == null) { onSelectGoal(goal) },
                shape = RoundedCornerShape(16.dp),
                color = if (isChosen) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                border = BorderStroke(
                    width = if (isChosen) 1.5.dp else 1.dp,
                    color = if (isChosen) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = goal.choiceLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isChosen) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f),
                    )
                    if (isChosen) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = "選択済み",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = praiseText != null,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(150)),
            ) {
                Text(
                    text = praiseText.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun MatchResultPage(
    goal: OnboardingGoal,
    isCurrentPage: Boolean,
    reducedMotion: Boolean,
    renderer: CanvasSceneRenderer,
    snapshot: DynamicDataSnapshot,
) {
    // Staged entrance: Echo -> Visual -> Declaration (150ms stagger)
    var stage by remember(isCurrentPage) { mutableStateOf(0) }
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            stage = 0
            return@LaunchedEffect
        }
        stage = 1
        delay(150L)
        stage = 2
        delay(150L)
        stage = 3
    }

    val previewDesign = remember(goal) {
        PresetDesigns.all().firstOrNull { it.id == goal.recommendedPresetId }
            ?: PresetDesigns.defaultDesign()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        AnimatedVisibility(
            visible = stage >= 1,
            enter = fadeIn(tween(if (reducedMotion) 100 else 240)),
        ) {
            Text(
                text = goal.echoPhrase,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }

        AnimatedVisibility(
            visible = stage >= 2,
            enter = fadeIn(tween(if (reducedMotion) 100 else 260)),
        ) {
            MiniCanvasPreview(
                design = previewDesign,
                snapshot = snapshot,
                renderer = renderer,
                showSafeAreaGuides = false,
                heightDp = 230,
            )
        }

        AnimatedVisibility(
            visible = stage >= 3,
            enter = fadeIn(tween(if (reducedMotion) 100 else 260)),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "LockCanvas がぴったりです",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = goal.matchStrength,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun IdentityPage(
    renderer: CanvasSceneRenderer,
    snapshot: DynamicDataSnapshot,
    goal: OnboardingGoal,
) {
    val design = remember(goal) {
        PresetDesigns.all().firstOrNull { it.id == goal.recommendedPresetId }
            ?: PresetDesigns.defaultDesign()
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        MiniCanvasPreview(
            design = design,
            snapshot = snapshot,
            renderer = renderer,
            showSafeAreaGuides = false,
            heightDp = 260,
        )
        Text(
            text = "ロック画面を、自由なキャンバスに",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "文字も画像もバッテリーゲージも、好きな場所へ配置できるライブ壁紙アプリです。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ValueDynamicPage(
    renderer: CanvasSceneRenderer,
    snapshot: DynamicDataSnapshot,
    isCurrentPage: Boolean,
    reducedMotion: Boolean,
) {
    var demoCharging by remember { mutableStateOf(false) }
    LaunchedEffect(isCurrentPage, reducedMotion) {
        if (!isCurrentPage || reducedMotion) {
            demoCharging = true
            return@LaunchedEffect
        }
        while (true) {
            demoCharging = false
            delay(1800L)
            demoCharging = true
            delay(1800L)
        }
    }
    val demoSnapshot = remember(snapshot, demoCharging) {
        snapshot.copy(
            batteryPercent = if (demoCharging) 84 else 18,
            isCharging = demoCharging,
            chargingLabel = if (demoCharging) "充電中" else "バッテリー駆動",
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        MiniCanvasPreview(
            design = PresetDesigns.cyberNeonGauge(),
            snapshot = demoSnapshot,
            renderer = renderer,
            showSafeAreaGuides = false,
            heightDp = 260,
        )
        Text(
            text = "バッテリーや時刻が、リアルタイムに動く",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "充電中や残量低下など、条件に応じてデザインを自動で切り替えられます。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ValueSafeAreaPage(
    renderer: CanvasSceneRenderer,
    snapshot: DynamicDataSnapshot,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        MiniCanvasPreview(
            design = PresetDesigns.minimalClockAndBattery(),
            snapshot = snapshot,
            renderer = renderer,
            showSafeAreaGuides = true,
            heightDp = 260,
        )
        Text(
            text = "OSの時計や指紋と、重ならず綺麗に収まる",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "セーフエリアガイドを見ながら、ロック画面の見やすい位置へ配置できます。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Step1Page(
    isCurrentPage: Boolean,
    reducedMotion: Boolean,
) {
    val alpha by animateFloatAsState(
        targetValue = if (isCurrentPage) 1f else 0.4f,
        animationSpec = tween(if (reducedMotion) 100 else 320),
        label = "step1Alpha",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        StepBadge("ステップ 1 / 3")
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("ミニマル時計", "サイバーネオン", "ナイト禅").forEachIndexed { idx, name ->
                Surface(
                    modifier = Modifier
                        .width(86.dp)
                        .height(132.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (idx == 0) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    border = BorderStroke(
                        1.dp,
                        if (idx == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GridView,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        Text(
            text = "1. ベースのデザインを選ぶ",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "まずはテンプレートから選ぶだけで、すぐに編集を始められます。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Step2Page(
    isCurrentPage: Boolean,
    reducedMotion: Boolean,
) {
    val shiftX = remember { Animatable(-28f) }
    LaunchedEffect(isCurrentPage, reducedMotion) {
        if (!isCurrentPage || reducedMotion) {
            shiftX.snapTo(0f)
            return@LaunchedEffect
        }
        shiftX.snapTo(-28f)
        delay(220L)
        shiftX.animateTo(0f, tween(durationMillis = 500, easing = FastOutSlowInEasing))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        StepBadge("ステップ 2 / 3")
        Surface(
            modifier = Modifier
                .width(196.dp)
                .height(140.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.offset { IntOffset(shiftX.value.roundToInt(), 0) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TouchApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "12:00",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
        Text(
            text = "2. 指で動かして、色やサイズを整える",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "ドラッグで移動、2本指で拡縮・回転。真ん中でピタッと揃います。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Step3Page() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        StepBadge("ステップ 3 / 3")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Save,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Text(
                text = "→",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Surface(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Wallpaper,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
        Text(
            text = "3. 保存して、ロック画面に設定する",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "一度ライブ壁紙に設定すれば、次からは保存するだけで即時反映されます。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TryDragPage(
    offsetX: Float,
    offsetY: Float,
    succeeded: Boolean,
    praiseText: String?,
    reducedMotion: Boolean,
    onDrag: (Float, Float) -> Unit,
    onDragFinished: () -> Unit,
) {
    val badgeScale by animateFloatAsState(
        targetValue = if (succeeded && !reducedMotion) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tryBadgeScale",
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = "パーツを1回、好きな場所へ動かしてみよう",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(
                width = if (succeeded) 2.dp else 1.dp,
                color = if (succeeded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                        .scale(badgeScale)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = { onDragFinished() },
                                onDragCancel = { onDragFinished() },
                            ) { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.x, dragAmount.y)
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BatteryChargingFull,
                            contentDescription = "動かせるバッテリーパーツ",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = "ドラッグして動かす",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.animation.AnimatedVisibility(visible = praiseText != null) {
                Text(
                    text = praiseText.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun TryResultPage(
    offsetX: Float,
    offsetY: Float,
    isCurrentPage: Boolean,
    reducedMotion: Boolean,
) {
    val alpha by animateFloatAsState(
        targetValue = if (isCurrentPage) 1f else 0.4f,
        animationSpec = tween(if (reducedMotion) 100 else 300),
        label = "tryResultAlpha",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Surface(
            modifier = Modifier
                .width(180.dp)
                .height(220.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.offset {
                        IntOffset((offsetX * 0.45f).roundToInt(), (offsetY * 0.45f).roundToInt())
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "配置完了",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
        Text(
            text = "思い通りに動かせましたね",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "あなたのロック画面は、もう自由に変えられます。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ConnectPage(
    goal: OnboardingGoal,
    renderer: CanvasSceneRenderer,
    snapshot: DynamicDataSnapshot,
    praiseText: String?,
    isCompleting: Boolean,
    reducedMotion: Boolean,
) {
    val previewDesign = remember(goal) {
        PresetDesigns.all().firstOrNull { it.id == goal.recommendedPresetId }
            ?: PresetDesigns.defaultDesign()
    }
    val scale by animateFloatAsState(
        targetValue = if (isCompleting && !reducedMotion) 1.04f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "completeScale",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        MiniCanvasPreview(
            design = previewDesign,
            snapshot = snapshot,
            renderer = renderer,
            showSafeAreaGuides = false,
            heightDp = 240,
        )
        Text(
            text = "さあ、最初の1枚を仕上げよう",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = praiseText ?: "選んだスタイルをベースに、そのまま編集を始められます。",
            style = MaterialTheme.typography.bodyMedium,
            color = if (praiseText != null) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StepBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun MiniCanvasPreview(
    design: CanvasDesign,
    snapshot: DynamicDataSnapshot,
    renderer: CanvasSceneRenderer,
    showSafeAreaGuides: Boolean,
    heightDp: Int,
) {
    Surface(
        modifier = Modifier
            .height(heightDp.dp)
            .aspectRatio(9f / 19.5f),
        shape = RoundedCornerShape(22.dp),
        color = Color.Black,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 6.dp,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                renderer.drawScene(
                    canvas = canvas.nativeCanvas,
                    width = size.width.toInt(),
                    height = size.height.toInt(),
                    design = design,
                    snapshot = snapshot,
                    animationTimeSec = 1.4f,
                    selectedElementId = null,
                    showSafeAreaGuides = showSafeAreaGuides,
                )
            }
        }
    }
}
