package com.lockcanvas.app.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.lockcanvas.app.engine.CanvasSceneRenderer
import com.lockcanvas.app.engine.RuleEvaluator
import com.lockcanvas.app.model.AnimationStyle
import com.lockcanvas.app.model.BatteryGaugeElement
import com.lockcanvas.app.model.BatteryGaugeStyle
import com.lockcanvas.app.model.CanvasElement
import com.lockcanvas.app.model.ConditionalRule
import com.lockcanvas.app.model.DynamicDataSnapshot
import com.lockcanvas.app.model.ImageElement
import com.lockcanvas.app.model.RuleAction
import com.lockcanvas.app.model.RuleCondition
import com.lockcanvas.app.model.ShapeElement
import com.lockcanvas.app.model.ShapeType
import com.lockcanvas.app.model.TextElement
import com.lockcanvas.app.ui.EditorToolTab
import com.lockcanvas.app.ui.EditorUiState
import com.lockcanvas.app.ui.theme.EditorColorSwatches
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    state: EditorUiState,
    liveSnapshot: DynamicDataSnapshot,
    snackbarHostState: SnackbarHostState,
    isWallpaperActive: Boolean,
    onApplyWallpaperClick: () -> Unit,
    onBack: () -> Unit,
    onSaveAndApply: () -> Unit,
    onRenameDesign: (String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleSafeAreaGuides: () -> Unit,
    onSelectElement: (String?) -> Unit,
    onSelectToolTab: (EditorToolTab?) -> Unit,
    onBeginGesture: () -> Unit,
    onTransformGesture: (String, Float, Float, Float, Float) -> Unit,
    onEndGesture: () -> Unit,
    onAddText: () -> Unit,
    onAddDynamicClock: () -> Unit,
    onAddBatteryBadge: () -> Unit,
    onAddBatteryGauge: (BatteryGaugeStyle) -> Unit,
    onAddShape: (ShapeType) -> Unit,
    onImportImage: (Uri, String?) -> Unit,
    onUpdateElement: (String, Boolean, (CanvasElement) -> CanvasElement) -> Unit,
    onDuplicateElement: (String) -> Unit,
    onDeleteElement: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onBringForward: (String) -> Unit,
    onSendBackward: (String) -> Unit,
    onUpdateBackground: (Long, Long, Boolean, AnimationStyle) -> Unit,
    onAddRule: (ConditionalRule) -> Unit,
    onToggleRule: (String) -> Unit,
    onDeleteRule: (String) -> Unit,
    onSetSimulatorEnabled: (Boolean) -> Unit,
    onUpdateSimulatorValues: (Int, Boolean, Int) -> Unit,
) {
    val context = LocalContext.current
    val renderer = remember(context) { CanvasSceneRenderer(context.applicationContext) }
    DisposableEffect(renderer) {
        onDispose { renderer.clearImageCache() }
    }

    val effectiveSnapshot = remember(
        liveSnapshot,
        state.simulatorEnabled,
        state.simulatedBatteryPercent,
        state.simulatedIsCharging,
        state.simulatedHourOfDay,
    ) {
        state.effectiveSnapshot(liveSnapshot)
    }

    val matchedRuleIds = remember(state.design, effectiveSnapshot) {
        RuleEvaluator.evaluate(state.design, effectiveSnapshot).matchedRuleIds
    }

    var animationTimeSec by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(state.design.animationStyle) {
        if (state.design.animationStyle == AnimationStyle.NONE) {
            animationTimeSec = 0f
            return@LaunchedEffect
        }
        var startNanos = -1L
        while (true) {
            withFrameNanos { frameNanos ->
                if (startNanos < 0L) startNanos = frameNanos
                animationTimeSec = (frameNanos - startNanos) / 1_000_000_000f
            }
        }
    }

    var pendingReplaceImageElementId by remember { mutableStateOf<String?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            onImportImage(uri, pendingReplaceImageElementId)
        }
        pendingReplaceImageElementId = null
    }

    var guideHintDismissed by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showAddRuleDialog by remember { mutableStateOf(false) }

    val handleBackRequest: () -> Unit = {
        if (state.activeTab != null) {
            onSelectToolTab(null)
        } else if (state.hasUnsavedChanges) {
            showUnsavedDialog = true
        } else {
            onBack()
        }
    }

    BackHandler(onBack = handleBackRequest)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = handleBackRequest) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "戻る",
                        )
                    }
                },
                title = {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showRenameDialog = true }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = state.design.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "名前を変更",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (state.hasUnsavedChanges) {
                            Text(
                                text = "未保存の変更あり",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onUndo, enabled = state.canUndo) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Undo,
                            contentDescription = "元に戻す",
                        )
                    }
                    IconButton(onClick = onRedo, enabled = state.canRedo) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Redo,
                            contentDescription = "やり直す",
                        )
                    }
                    IconButton(onClick = onToggleSafeAreaGuides) {
                        Icon(
                            imageVector = Icons.Outlined.GridOn,
                            contentDescription = "セーフエリア表示切替",
                            tint = if (state.showSafeAreaGuides) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    if (!isWallpaperActive) {
                        Button(
                            onClick = onApplyWallpaperClick,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .minimumInteractiveComponentSize()
                                .heightIn(min = 38.dp),
                        ) {
                            Text("ロック画面に設定", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Button(
                        onClick = onSaveAndApply,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .minimumInteractiveComponentSize()
                            .heightIn(min = 38.dp),
                    ) {
                        Text("保存", style = MaterialTheme.typography.labelLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding(),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Center Interactive Phone Preview
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                InteractiveCanvasStage(
                    state = state,
                    snapshot = effectiveSnapshot,
                    animationTimeSec = animationTimeSec,
                    renderer = renderer,
                    onSelectElement = onSelectElement,
                    onBeginGesture = onBeginGesture,
                    onTransformGesture = onTransformGesture,
                    onEndGesture = onEndGesture,
                )

                if (state.simulatorEnabled) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(99.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    ) {
                        val chargeText = if (state.simulatedIsCharging) "充電中" else "放電中"
                        Text(
                            text = "テスト表示: ${state.simulatedBatteryPercent}%・$chargeText・${state.simulatedHourOfDay}時",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            if (state.showSafeAreaGuides && !guideHintDismissed) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "色のついた帯は、時計や通知が重なる場所です",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    TextButton(onClick = { guideHintDismissed = true }) {
                        Text("閉じる", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Beginner-friendly 1-line operation hint (when panel is closed)
            if (state.activeTab == null) {
                val hintText = if (state.selectedElement != null) {
                    "ドラッグで移動・2本指で拡縮・回転・「編集」で調整"
                } else {
                    "タップで選択・ドラッグで移動・「追加」でパーツ追加"
                }
                Text(
                    text = hintText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }

            // Expandable Tool Panel above Dock (Progressive Disclosure)
            AnimatedVisibility(
                visible = state.activeTab != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                state.activeTab?.let { tab ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            IconButton(
                                onClick = { onSelectToolTab(null) },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "パネルを閉じる",
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            when (tab) {
                                EditorToolTab.ADD -> AddElementPanel(
                                    onAddText = onAddText,
                                    onAddDynamicClock = onAddDynamicClock,
                                    onAddBatteryBadge = onAddBatteryBadge,
                                    onAddBatteryGauge = onAddBatteryGauge,
                                    onAddShape = onAddShape,
                                    onPickImage = {
                                        pendingReplaceImageElementId = null
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                                            ),
                                        )
                                    },
                                )
                                EditorToolTab.EDIT -> ElementInspectorPanel(
                                    element = state.selectedElement,
                                    onUpdateElement = onUpdateElement,
                                    onReplaceImage = { elementId ->
                                        pendingReplaceImageElementId = elementId
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                                            ),
                                        )
                                    },
                                    onBringForward = onBringForward,
                                    onSendBackward = onSendBackward,
                                    onDuplicate = onDuplicateElement,
                                    onDelete = onDeleteElement,
                                )
                                EditorToolTab.BACKGROUND -> BackgroundAnimationPanel(
                                    state = state,
                                    onUpdateBackground = onUpdateBackground,
                                )
                                EditorToolTab.RULES -> ConditionalRulesPanel(
                                    state = state,
                                    matchedRuleIds = matchedRuleIds,
                                    onSetSimulatorEnabled = onSetSimulatorEnabled,
                                    onUpdateSimulatorValues = onUpdateSimulatorValues,
                                    onToggleRule = onToggleRule,
                                    onDeleteRule = onDeleteRule,
                                    onOpenAddRule = { showAddRuleDialog = true },
                                )
                                EditorToolTab.LAYERS -> LayersPanel(
                                    elements = state.design.elements,
                                    selectedElementId = state.selectedElementId,
                                    onSelect = onSelectElement,
                                    onToggleVisibility = onToggleVisibility,
                                    onBringForward = onBringForward,
                                    onSendBackward = onSendBackward,
                                    onDelete = onDeleteElement,
                                )
                            }
                        }
                    }
                }
                }
            }

            // Bottom Tool Dock (Edge-to-Edge to screen bottom, zero black gap)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DockTabItem(
                        icon = Icons.Outlined.AddBox,
                        label = "追加",
                        selected = state.activeTab == EditorToolTab.ADD,
                        onClick = { onSelectToolTab(EditorToolTab.ADD) },
                    )
                    DockTabItem(
                        icon = Icons.Outlined.Edit,
                        label = "編集",
                        selected = state.activeTab == EditorToolTab.EDIT,
                        onClick = { onSelectToolTab(EditorToolTab.EDIT) },
                    )
                    DockTabItem(
                        icon = Icons.Outlined.Palette,
                        label = "背景/動き",
                        selected = state.activeTab == EditorToolTab.BACKGROUND,
                        onClick = { onSelectToolTab(EditorToolTab.BACKGROUND) },
                    )
                    DockTabItem(
                        icon = Icons.Outlined.Tune,
                        label = "自動切替",
                        selected = state.activeTab == EditorToolTab.RULES,
                        hasIndicator = matchedRuleIds.isNotEmpty() || state.simulatorEnabled,
                        onClick = { onSelectToolTab(EditorToolTab.RULES) },
                    )
                    DockTabItem(
                        icon = Icons.Outlined.Layers,
                        label = "重なり順",
                        selected = state.activeTab == EditorToolTab.LAYERS,
                        onClick = { onSelectToolTab(EditorToolTab.LAYERS) },
                    )
                }
            }
        }
    }

    if (showRenameDialog) {
        var nameInput by remember(state.design.name) { mutableStateOf(state.design.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("デザイン名を変更") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    singleLine = true,
                    label = { Text("デザイン名") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenameDesign(nameInput)
                        showRenameDialog = false
                    },
                ) {
                    Text("変更")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("キャンセル")
                }
            },
        )
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("変更を保存しますか？") },
            text = { Text("保存していない編集内容があります。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnsavedDialog = false
                        onSaveAndApply()
                        onBack()
                    },
                ) {
                    Text("保存して戻る")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showUnsavedDialog = false
                            onBack()
                        },
                    ) {
                        Text("破棄", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { showUnsavedDialog = false }) {
                        Text("キャンセル")
                    }
                }
            },
        )
    }

    if (showAddRuleDialog) {
        AddRuleDialog(
            elements = state.design.elements,
            onDismiss = { showAddRuleDialog = false },
            onAddRule = { rule ->
                onAddRule(rule)
                showAddRuleDialog = false
            },
        )
    }
}

@Composable
private fun InteractiveCanvasStage(
    state: EditorUiState,
    snapshot: DynamicDataSnapshot,
    animationTimeSec: Float,
    renderer: CanvasSceneRenderer,
    onSelectElement: (String?) -> Unit,
    onBeginGesture: () -> Unit,
    onTransformGesture: (String, Float, Float, Float, Float) -> Unit,
    onEndGesture: () -> Unit,
) {
    val latestState by rememberUpdatedState(state)
    val latestSnapshot by rememberUpdatedState(snapshot)
    var stageSize by remember { mutableStateOf(IntSize.Zero) }

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(9f / 19.5f),
        shape = RoundedCornerShape(28.dp),
        color = Color.Black,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 8.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { stageSize = it }
                .pointerInput(renderer) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downPos = down.position
                        val w = stageSize.width
                        val h = stageSize.height
                        if (w <= 0 || h <= 0) return@awaitEachGesture

                        val hit = renderer.hitTestElement(
                            touchX = downPos.x,
                            touchY = downPos.y,
                            width = w,
                            height = h,
                            design = latestState.design,
                            snapshot = latestSnapshot,
                        )
                        var activeId = hit?.id ?: latestState.selectedElementId
                        var gestureStarted = false
                        var totalMovement = 0f

                        while (true) {
                            val event = awaitPointerEvent()
                            val pressedCount = event.changes.count { it.pressed }
                            if (pressedCount == 0) break

                            val pan = event.calculatePan()
                            val zoom = event.calculateZoom()
                            val rotation = event.calculateRotation()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            totalMovement += pan.getDistance()

                            val hasTransform = pressedCount >= 2 &&
                                (abs(zoom - 1f) > 0.005f || abs(rotation) > 0.3f)
                            val hasDrag = totalMovement > viewConfiguration.touchSlop

                            if (!gestureStarted && (hasDrag || hasTransform)) {
                                if (hit != null) {
                                    activeId = hit.id
                                    onSelectElement(hit.id)
                                }
                                if (activeId != null) {
                                    gestureStarted = true
                                    onBeginGesture()
                                }
                            }

                            if (gestureStarted && activeId != null) {
                                val dxFrac = pan.x / w.toFloat()
                                val dyFrac = pan.y / h.toFloat()
                                val appliedZoom = if (pressedCount >= 2) zoom else 1f
                                val appliedRot = if (pressedCount >= 2) rotation else 0f
                                if (dxFrac != 0f || dyFrac != 0f || appliedZoom != 1f || appliedRot != 0f) {
                                    onTransformGesture(
                                        activeId,
                                        dxFrac,
                                        dyFrac,
                                        appliedZoom,
                                        appliedRot,
                                    )
                                }
                                event.changes.forEach { it.consume() }
                            } else if (centroid != Offset.Unspecified && pressedCount >= 2) {
                                event.changes.forEach { it.consume() }
                            }
                        }

                        if (gestureStarted) {
                            onEndGesture()
                        } else {
                            onSelectElement(hit?.id)
                        }
                    }
                },
        ) {
            drawIntoCanvas { canvas ->
                renderer.drawScene(
                    canvas = canvas.nativeCanvas,
                    width = size.width.toInt(),
                    height = size.height.toInt(),
                    design = state.design,
                    snapshot = snapshot,
                    animationTimeSec = animationTimeSec,
                    selectedElementId = state.selectedElementId,
                    showSafeAreaGuides = state.showSafeAreaGuides,
                )
            }

            // Center Snap Guide Lines
            val snapColor = Color(0xCC64D2FF)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            if (state.snapGuideX) {
                drawLine(
                    color = snapColor,
                    start = Offset(size.width * 0.5f, 0f),
                    end = Offset(size.width * 0.5f, size.height),
                    strokeWidth = 2f,
                    pathEffect = dashEffect,
                )
            }
            if (state.snapGuideY) {
                drawLine(
                    color = snapColor,
                    start = Offset(0f, size.height * 0.5f),
                    end = Offset(size.width, size.height * 0.5f),
                    strokeWidth = 2f,
                    pathEffect = dashEffect,
                )
            }
        }

            if (state.selectedElementId == null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp, start = 12.dp, end = 12.dp),
                    shape = RoundedCornerShape(99.dp),
                    color = Color.Black.copy(alpha = 0.58f),
                ) {
                    Text(
                        text = "ドラッグで移動・2本指で拡大/回転",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DockTabItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    hasIndicator: Boolean = false,
    onClick: () -> Unit,
) {
    val iconTint = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val labelTint = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = Modifier
            .widthIn(min = 56.dp)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(99.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.secondaryContainer
                    else Color.Transparent,
                )
                .padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
            if (hasIndicator) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = labelTint,
            maxLines = 1,
        )
    }
}

@Composable
private fun AddElementPanel(
    onAddText: () -> Unit,
    onAddDynamicClock: () -> Unit,
    onAddBatteryBadge: () -> Unit,
    onAddBatteryGauge: (BatteryGaugeStyle) -> Unit,
    onAddShape: (ShapeType) -> Unit,
    onPickImage: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "文字と、自動で変わるパーツ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickAddTile(Icons.Outlined.TextFields, "テキスト", onAddText)
            QuickAddTile(Icons.Outlined.Schedule, "時計・日付", onAddDynamicClock)
            QuickAddTile(Icons.Outlined.BatteryChargingFull, "電池バッジ", onAddBatteryBadge)
            QuickAddTile(Icons.Outlined.Image, "画像を追加", onPickImage)
        }

        Text(
            text = "電池のバーと図形",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickAddTile(Icons.Outlined.BatteryChargingFull, "電池バー") {
                onAddBatteryGauge(BatteryGaugeStyle.BAR)
            }
            QuickAddTile(Icons.Outlined.AutoAwesome, "円形ゲージ") {
                onAddBatteryGauge(BatteryGaugeStyle.CIRCULAR)
            }
            QuickAddTile(Icons.Outlined.AddBox, "角丸四角") {
                onAddShape(ShapeType.RECTANGLE)
            }
            QuickAddTile(Icons.Outlined.AddBox, "サークル") {
                onAddShape(ShapeType.CIRCLE)
            }
            QuickAddTile(Icons.Outlined.AddBox, "ピル型") {
                onAddShape(ShapeType.PILL)
            }
        }
    }
}

@Composable
private fun QuickAddTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .width(86.dp)
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ElementInspectorPanel(
    element: CanvasElement?,
    onUpdateElement: (String, Boolean, (CanvasElement) -> CanvasElement) -> Unit,
    onReplaceImage: (String) -> Unit,
    onBringForward: (String) -> Unit,
    onSendBackward: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    if (element == null) {
        Text(
            text = "キャンバス上の要素をタップして選択すると、ここで色・サイズ・テキストなどを編集できます。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        return
    }

    // Quick Action Strip
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SmallActionChip(Icons.Rounded.ArrowUpward, "前面へ") { onBringForward(element.id) }
        SmallActionChip(Icons.Rounded.ArrowDownward, "背面へ") { onSendBackward(element.id) }
        SmallActionChip(Icons.Outlined.ContentCopy, "複製") { onDuplicate(element.id) }
        SmallActionChip(
            icon = Icons.Outlined.DeleteOutline,
            label = "削除",
            tint = MaterialTheme.colorScheme.error,
        ) { onDelete(element.id) }
    }

    // Type-specific properties
    when (element) {
        is TextElement -> {
            OutlinedTextField(
                value = element.text,
                onValueChange = { newText ->
                    onUpdateElement(element.id, true) { el ->
                        if (el is TextElement) el.copy(text = newText) else el
                    }
                },
                label = { Text("文字（時刻や電池残量も入れられます）") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
            )

            // Dynamic Token Insert Chips (display names in plain words; the token is inserted)
            Text(
                text = "下のボタンを押すと、ロック画面で自動的に変わる文字を入れられます。上の欄に {battery} のような記号が見えたら、それが自動で置き換わる合図です",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(
                    "{time}" to "今の時刻",
                    "{date}" to "今日の日付",
                    "{battery}" to "電池残量（%）",
                    "{charging_status}" to "充電中かどうか",
                ).forEach { (token, displayName) ->
                    FilterChip(
                        selected = element.text.contains(token),
                        onClick = {
                            onUpdateElement(element.id, true) { el ->
                                if (el is TextElement) {
                                    val nextText = if (el.text.isBlank()) token else "${el.text} $token"
                                    el.copy(text = nextText, dynamicTemplate = true)
                                } else {
                                    el
                                }
                            }
                        },
                        label = { Text(displayName, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Outlined.FormatBold, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("太字", style = MaterialTheme.typography.bodyMedium)
                }
                Switch(
                    checked = element.bold,
                    onCheckedChange = { bold ->
                        onUpdateElement(element.id, true) { el ->
                            if (el is TextElement) el.copy(bold = bold) else el
                        }
                    },
                )
            }

            LabeledSlider(
                label = "文字サイズ",
                valueText = "${element.fontSizeSp.roundToInt()}sp",
                value = element.fontSizeSp,
                valueRange = 10f..72f,
                onValueChange = { sp ->
                    onUpdateElement(element.id, false) { el ->
                        if (el is TextElement) el.copy(fontSizeSp = sp) else el
                    }
                },
            )

            ColorSwatchRow(
                label = "文字色",
                colors = EditorColorSwatches.foregroundColors,
                selectedArgb = element.colorArgb,
                onSelect = { argb ->
                    onUpdateElement(element.id, true) { el ->
                        if (el is TextElement) el.copy(colorArgb = argb) else el
                    }
                },
            )
        }

        is BatteryGaugeElement -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = element.style == BatteryGaugeStyle.BAR,
                    onClick = {
                        onUpdateElement(element.id, true) { el ->
                            if (el is BatteryGaugeElement) {
                                el.copy(
                                    style = BatteryGaugeStyle.BAR,
                                    widthFraction = 0.60f,
                                    heightFraction = 0.04f,
                                )
                            } else el
                        }
                    },
                    label = { Text("バー型") },
                )
                FilterChip(
                    selected = element.style == BatteryGaugeStyle.CIRCULAR,
                    onClick = {
                        onUpdateElement(element.id, true) { el ->
                            if (el is BatteryGaugeElement) {
                                el.copy(
                                    style = BatteryGaugeStyle.CIRCULAR,
                                    widthFraction = 0.40f,
                                    heightFraction = 0.20f,
                                )
                            } else el
                        }
                    },
                    label = { Text("円形ゲージ") },
                )
                FilterChip(
                    selected = element.showLabel,
                    onClick = {
                        onUpdateElement(element.id, true) { el ->
                            if (el is BatteryGaugeElement) el.copy(showLabel = !el.showLabel) else el
                        }
                    },
                    label = { Text("%表示") },
                )
            }

            ColorSwatchRow(
                label = "ゲージ色",
                colors = EditorColorSwatches.foregroundColors,
                selectedArgb = element.activeColorArgb,
                onSelect = { argb ->
                    onUpdateElement(element.id, true) { el ->
                        if (el is BatteryGaugeElement) el.copy(activeColorArgb = argb) else el
                    }
                },
            )
        }

        is ShapeElement -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ShapeType.RECTANGLE to "四角",
                    ShapeType.CIRCLE to "円",
                    ShapeType.PILL to "ピル",
                ).forEach { (shapeType, label) ->
                    FilterChip(
                        selected = element.shapeType == shapeType,
                        onClick = {
                            onUpdateElement(element.id, true) { el ->
                                if (el is ShapeElement) el.copy(shapeType = shapeType) else el
                            }
                        },
                        label = { Text(label) },
                    )
                }
            }

            LabeledSlider(
                label = "幅",
                valueText = "${(element.widthFraction * 100).roundToInt()}%",
                value = element.widthFraction,
                valueRange = 0.12f..0.95f,
                onValueChange = { w ->
                    onUpdateElement(element.id, false) { el ->
                        if (el is ShapeElement) el.copy(widthFraction = w) else el
                    }
                },
            )
            LabeledSlider(
                label = "高さ",
                valueText = "${(element.heightFraction * 100).roundToInt()}%",
                value = element.heightFraction,
                valueRange = 0.03f..0.60f,
                onValueChange = { h ->
                    onUpdateElement(element.id, false) { el ->
                        if (el is ShapeElement) el.copy(heightFraction = h) else el
                    }
                },
            )
            ColorSwatchRow(
                label = "塗りつぶし",
                colors = EditorColorSwatches.shapeFillColors,
                selectedArgb = element.fillColorArgb,
                onSelect = { argb ->
                    onUpdateElement(element.id, true) { el ->
                        if (el is ShapeElement) el.copy(fillColorArgb = argb) else el
                    }
                },
            )
            ColorSwatchRow(
                label = "枠線色",
                colors = EditorColorSwatches.foregroundColors,
                selectedArgb = element.strokeColorArgb,
                onSelect = { argb ->
                    onUpdateElement(element.id, true) { el ->
                        if (el is ShapeElement) el.copy(strokeColorArgb = argb) else el
                    }
                },
            )
        }

        is ImageElement -> {
            OutlinedButton(
                onClick = { onReplaceImage(element.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("別の画像を選ぶ")
            }
            LabeledSlider(
                label = "角丸",
                valueText = "${element.cornerRadiusDp.roundToInt()}dp",
                value = element.cornerRadiusDp,
                valueRange = 0f..48f,
                onValueChange = { r ->
                    onUpdateElement(element.id, false) { el ->
                        if (el is ImageElement) el.copy(cornerRadiusDp = r) else el
                    }
                },
            )
        }
    }

    // Common Transform Controls
    LabeledSlider(
        label = "スケール",
        valueText = "${(element.scale * 100).roundToInt()}%",
        value = element.scale,
        valueRange = 0.4f..2.5f,
        onValueChange = { sc ->
            onUpdateElement(element.id, false) { it.withTransform(scale = sc) }
        },
    )
    LabeledSlider(
        label = "回転",
        valueText = "${element.rotationDegrees.roundToInt()}°",
        value = element.rotationDegrees,
        valueRange = -180f..180f,
        onValueChange = { rot ->
            onUpdateElement(element.id, false) { it.withTransform(rotationDegrees = rot) }
        },
    )
    LabeledSlider(
        label = "不透明度",
        valueText = "${(element.alpha * 100).roundToInt()}%",
        value = element.alpha,
        valueRange = 0.1f..1.0f,
        onValueChange = { a ->
            onUpdateElement(element.id, false) { it.withTransform(alpha = a) }
        },
    )
}

@Composable
private fun BackgroundAnimationPanel(
    state: EditorUiState,
    onUpdateBackground: (Long, Long, Boolean, AnimationStyle) -> Unit,
) {
    val design = state.design
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("グラデーション背景", style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = design.backgroundGradientEnabled,
            onCheckedChange = { enabled ->
                onUpdateBackground(
                    design.backgroundColorArgb,
                    design.secondaryBackgroundColorArgb,
                    enabled,
                    design.animationStyle,
                )
            },
        )
    }

    ColorSwatchRow(
        label = if (design.backgroundGradientEnabled) "上部カラー" else "背景カラー",
        colors = EditorColorSwatches.backgroundColors,
        selectedArgb = design.backgroundColorArgb,
        onSelect = { argb ->
            onUpdateBackground(
                argb,
                design.secondaryBackgroundColorArgb,
                design.backgroundGradientEnabled,
                design.animationStyle,
            )
        },
    )

    if (design.backgroundGradientEnabled) {
        ColorSwatchRow(
            label = "下部カラー",
            colors = EditorColorSwatches.backgroundColors,
            selectedArgb = design.secondaryBackgroundColorArgb,
            onSelect = { argb ->
                onUpdateBackground(
                    design.backgroundColorArgb,
                    argb,
                    true,
                    design.animationStyle,
                )
            },
        )
    }

    Text(
        text = "アニメーション",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            AnimationStyle.NONE to "なし（最省電力）",
            AnimationStyle.FLOATING_PARTICLES to "パーティクル",
            AnimationStyle.PULSE_GLOW to "パルスグロー",
        ).forEach { (style, label) ->
            FilterChip(
                selected = design.animationStyle == style,
                onClick = {
                    onUpdateBackground(
                        design.backgroundColorArgb,
                        design.secondaryBackgroundColorArgb,
                        design.backgroundGradientEnabled,
                        style,
                    )
                },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}

@Composable
private fun ConditionalRulesPanel(
    state: EditorUiState,
    matchedRuleIds: List<String>,
    onSetSimulatorEnabled: (Boolean) -> Unit,
    onUpdateSimulatorValues: (Int, Boolean, Int) -> Unit,
    onToggleRule: (String) -> Unit,
    onDeleteRule: (String) -> Unit,
    onOpenAddRule: () -> Unit,
) {
    // Live Preview Simulator Card
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("条件のテスト表示", style = MaterialTheme.typography.titleSmall)
                Switch(
                    checked = state.simulatorEnabled,
                    onCheckedChange = onSetSimulatorEnabled,
                )
            }

            if (state.simulatorEnabled) {
                LabeledSlider(
                    label = "バッテリー残量",
                    valueText = "${state.simulatedBatteryPercent}%",
                    value = state.simulatedBatteryPercent.toFloat(),
                    valueRange = 0f..100f,
                    onValueChange = { v ->
                        onUpdateSimulatorValues(
                            v.roundToInt(),
                            state.simulatedIsCharging,
                            state.simulatedHourOfDay,
                        )
                    },
                )
                LabeledSlider(
                    label = "時間帯",
                    valueText = "${state.simulatedHourOfDay}時",
                    value = state.simulatedHourOfDay.toFloat(),
                    valueRange = 0f..23f,
                    onValueChange = { v ->
                        onUpdateSimulatorValues(
                            state.simulatedBatteryPercent,
                            state.simulatedIsCharging,
                            v.roundToInt(),
                        )
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("充電中としてプレビュー", style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = state.simulatedIsCharging,
                        onCheckedChange = { charging ->
                            onUpdateSimulatorValues(
                                state.simulatedBatteryPercent,
                                charging,
                                state.simulatedHourOfDay,
                            )
                        },
                    )
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "自動切替のルール (${state.design.rules.size})",
            style = MaterialTheme.typography.titleSmall,
        )
        TextButton(onClick = onOpenAddRule) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("ルールを追加")
        }
    }

    if (state.design.rules.isEmpty()) {
        Text(
            text = "ルールがありません。「ルールを追加」からバッテリー低下や夜間の自動切り替えを設定できます。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        state.design.rules.forEach { rule ->
            val matched = matchedRuleIds.contains(rule.id)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = if (matched) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                } else {
                    null
                },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(rule.name, style = MaterialTheme.typography.titleSmall)
                            if (matched) {
                                Text(
                                    text = "一致中",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Text(
                            text = "${describeCondition(rule.condition)} → ${describeAction(rule.action)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = rule.enabled,
                        onCheckedChange = { onToggleRule(rule.id) },
                    )
                    IconButton(
                        onClick = { onDeleteRule(rule.id) },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "ルールを削除",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LayersPanel(
    elements: List<CanvasElement>,
    selectedElementId: String?,
    onSelect: (String?) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onBringForward: (String) -> Unit,
    onSendBackward: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    if (elements.isEmpty()) {
        Text(
            text = "パーツがありません。「追加」タブからパーツを追加してください。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val topFirst = remember(elements) { elements.sortedByDescending { it.zIndex } }
    topFirst.forEach { el ->
        val isSelected = el.id == selectedElementId
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(el.id) },
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            border = if (isSelected) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            } else {
                null
            },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { onToggleVisibility(el.id) },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = if (el.visible) {
                            Icons.Outlined.Visibility
                        } else {
                            Icons.Outlined.VisibilityOff
                        },
                        contentDescription = "表示切替",
                        modifier = Modifier.size(18.dp),
                        tint = if (el.visible) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }

                Text(
                    text = describeElement(el),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                )

                IconButton(
                    onClick = { onBringForward(el.id) },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "上へ", modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = { onSendBackward(el.id) },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "下へ", modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = { onDelete(el.id) },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "削除",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AddRuleDialog(
    elements: List<CanvasElement>,
    onDismiss: () -> Unit,
    onAddRule: (ConditionalRule) -> Unit,
) {
    var conditionType by remember { mutableIntStateOf(0) } // 0: BatteryBelow, 1: IsCharging, 2: NightTime
    var actionType by remember { mutableIntStateOf(0) } // 0: OverrideBg, 1: OverrideText, 2: ToggleVis
    var threshold by remember { mutableIntStateOf(20) }
    var bgColor by remember { mutableStateOf(0xFF2B0E17L) }
    val textElements = remember(elements) { elements.filterIsInstance<TextElement>() }
    var targetElementId by remember(elements) { mutableStateOf(elements.firstOrNull()?.id ?: "") }
    var overrideText by remember { mutableStateOf("電池わずか · {battery}") }
    var targetVisible by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自動切替ルールを追加") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("どんなとき", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = conditionType == 0,
                        onClick = { conditionType = 0 },
                        label = { Text("電池低下") },
                    )
                    FilterChip(
                        selected = conditionType == 1,
                        onClick = { conditionType = 1 },
                        label = { Text("充電中") },
                    )
                    FilterChip(
                        selected = conditionType == 2,
                        onClick = { conditionType = 2 },
                        label = { Text("夜間") },
                    )
                }

                if (conditionType == 0) {
                    LabeledSlider(
                        label = "しきい値",
                        valueText = "${threshold}%未満",
                        value = threshold.toFloat(),
                        valueRange = 5f..50f,
                        onValueChange = { threshold = it.roundToInt() },
                    )
                }

                Text("アクション (THEN)", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = actionType == 0,
                        onClick = { actionType = 0 },
                        label = { Text("背景色を変更") },
                    )
                    if (textElements.isNotEmpty()) {
                        FilterChip(
                            selected = actionType == 1,
                            onClick = {
                                actionType = 1
                                targetElementId = textElements.first().id
                            },
                            label = { Text("文字を変更") },
                        )
                    }
                    if (elements.isNotEmpty()) {
                        FilterChip(
                            selected = actionType == 2,
                            onClick = {
                                actionType = 2
                                targetElementId = elements.first().id
                            },
                            label = { Text("表示切替") },
                        )
                    }
                }

                when (actionType) {
                    0 -> ColorSwatchRow(
                        label = "変更後の背景色",
                        colors = EditorColorSwatches.backgroundColors,
                        selectedArgb = bgColor,
                        onSelect = { bgColor = it },
                    )
                    1 -> {
                        OutlinedTextField(
                            value = overrideText,
                            onValueChange = { overrideText = it },
                            label = { Text("置き換えるテキスト") },
                            supportingText = { Text("{battery} は電池残量に自動で置き換わります") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    2 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("条件一致時に表示する", style = MaterialTheme.typography.bodySmall)
                            Switch(checked = targetVisible, onCheckedChange = { targetVisible = it })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val condition = when (conditionType) {
                        0 -> RuleCondition.BatteryBelow(threshold)
                        1 -> RuleCondition.IsCharging
                        else -> RuleCondition.NightTime(startHour = 21, endHour = 6)
                    }
                    val action = when (actionType) {
                        1 -> RuleAction.OverrideElementText(
                            elementId = targetElementId.ifBlank { textElements.firstOrNull()?.id ?: "" },
                            text = overrideText,
                        )
                        2 -> RuleAction.ToggleElementVisibility(
                            elementId = targetElementId.ifBlank { elements.firstOrNull()?.id ?: "" },
                            visible = targetVisible,
                        )
                        else -> RuleAction.OverrideBackground(bgColor)
                    }
                    val ruleName = when (conditionType) {
                        0 -> "バッテリー${threshold}%未満"
                        1 -> "充電中モード"
                        else -> "夜間モード (21:00-06:00)"
                    }
                    onAddRule(
                        ConditionalRule(
                            id = "rule_${UUID.randomUUID().toString().take(8)}",
                            name = ruleName,
                            enabled = true,
                            condition = condition,
                            action = action,
                        ),
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("追加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        },
    )
}

@Composable
private fun SmallActionChip(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valueText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.height(30.dp),
        )
    }
}

@Composable
private fun ColorSwatchRow(
    label: String,
    colors: List<Long>,
    selectedArgb: Long,
    onSelect: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            colors.forEach { argb ->
                val isSelected = argb == selectedArgb
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(argb.toInt()))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            shape = CircleShape,
                        )
                        .clickable { onSelect(argb) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun describeElement(element: CanvasElement): String = when (element) {
    is TextElement -> "テキスト: ${element.text.replace('\n', ' ').take(18)}"
    is BatteryGaugeElement -> when (element.style) {
        BatteryGaugeStyle.BAR -> "バッテリーバー"
        BatteryGaugeStyle.CIRCULAR -> "円形バッテリーゲージ"
    }
    is ShapeElement -> when (element.shapeType) {
        ShapeType.RECTANGLE -> "図形（四角）"
        ShapeType.CIRCLE -> "図形（円）"
        ShapeType.PILL -> "図形（ピル）"
    }
    is ImageElement -> "画像パーツ"
}

private fun describeCondition(condition: RuleCondition): String = when (condition) {
    is RuleCondition.BatteryBelow -> "電池${condition.thresholdPercent}%未満"
    is RuleCondition.IsCharging -> "充電中"
    is RuleCondition.NightTime -> "夜間(${condition.startHour}時-${condition.endHour}時)"
    is RuleCondition.DateRange ->
        "期間(${condition.startMonth}/${condition.startDay}-${condition.endMonth}/${condition.endDay})"
}

private fun describeAction(action: RuleAction): String = when (action) {
    is RuleAction.OverrideBackground -> "背景色変更"
    is RuleAction.OverrideElementText -> "テキスト変更"
    is RuleAction.ToggleElementVisibility -> if (action.visible) "要素を表示" else "要素を非表示"
}
