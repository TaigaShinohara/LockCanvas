package com.lockcanvas.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lockcanvas.app.ui.LockCanvasViewModel
import com.lockcanvas.app.ui.ScreenRoute
import com.lockcanvas.app.ui.onboarding.OnboardingScreen
import com.lockcanvas.app.ui.screens.EditorScreen
import com.lockcanvas.app.ui.screens.HomeScreen
import com.lockcanvas.app.ui.screens.SettingsInfoScreen
import com.lockcanvas.app.ui.theme.LockCanvasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LockCanvasTheme {
                LockCanvasApp()
            }
        }
    }
}

@Composable
fun LockCanvasApp(
    viewModel: LockCanvasViewModel = viewModel(),
) {
    val route by viewModel.route.collectAsStateWithLifecycle()
    val onboardingChecked by viewModel.onboardingChecked.collectAsStateWithLifecycle()
    val onboardingGoal by viewModel.onboardingGoal.collectAsStateWithLifecycle()
    val designs by viewModel.designs.collectAsStateWithLifecycle()
    val activeDesignId by viewModel.activeDesignId.collectAsStateWithLifecycle()
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val liveSnapshot by viewModel.liveSnapshot.collectAsStateWithLifecycle()
    val isWallpaperActive by viewModel.isWallpaperActive.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showWallpaperGuideDialog by remember { mutableStateOf(false) }

    val requestWallpaperPicker: () -> Unit = {
        if (isWallpaperActive) {
            viewModel.launchLiveWallpaperPicker()
        } else {
            showWallpaperGuideDialog = true
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.snackbarMessages.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = event.actionLabel,
                duration = if (event.actionLabel != null) {
                    SnackbarDuration.Long
                } else {
                    SnackbarDuration.Short
                },
            )
            if (result == SnackbarResult.ActionPerformed && event.openWallpaperPickerOnAction) {
                requestWallpaperPicker()
            }
            if (result == SnackbarResult.ActionPerformed && event.undoOnAction) {
                viewModel.undo()
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshWallpaperStatus()
    }

    if (!onboardingChecked) return

    when (route) {
        ScreenRoute.Onboarding -> OnboardingScreen(
            initialGoal = onboardingGoal,
            liveSnapshot = liveSnapshot,
            onCompleteToWallpaper = { goal ->
                viewModel.completeOnboardingToWallpaper(goal)
                requestWallpaperPicker()
            },
            onTryLater = { goal ->
                viewModel.completeOnboardingLater(goal, isSkip = false)
            },
            onSkip = { goal ->
                viewModel.completeOnboardingLater(goal, isSkip = true)
            },
        )

        ScreenRoute.Home -> HomeScreen(
            designs = designs,
            activeDesignId = activeDesignId,
            snapshot = liveSnapshot,
            isWallpaperActive = isWallpaperActive,
            snackbarHostState = snackbarHostState,
            onApplyWallpaperClick = requestWallpaperPicker,
            onCreateNewClick = { viewModel.createNewDesignAndEdit() },
            onOpenEditor = { design -> viewModel.openEditor(design) },
            onSetActiveDesign = { id -> viewModel.applyDesign(id) },
            onDuplicateDesign = { id -> viewModel.duplicateSavedDesign(id) },
            onDeleteDesign = { id -> viewModel.deleteSavedDesign(id) },
            onOpenSettingsInfo = { viewModel.openSettingsInfo() },
        )

        ScreenRoute.Editor -> EditorScreen(
            state = editorState,
            liveSnapshot = liveSnapshot,
            snackbarHostState = snackbarHostState,
            isWallpaperActive = isWallpaperActive,
            onApplyWallpaperClick = requestWallpaperPicker,
            onBack = { viewModel.openHome() },
            onSaveAndApply = { viewModel.saveAndApplyCurrentDesign() },
            onRenameDesign = { name -> viewModel.renameDesign(name) },
            onUndo = { viewModel.undo() },
            onRedo = { viewModel.redo() },
            onToggleSafeAreaGuides = { viewModel.toggleSafeAreaGuides() },
            onSelectElement = { id -> viewModel.selectElement(id) },
            onSelectToolTab = { tab -> viewModel.selectToolTab(tab) },
            onBeginGesture = { viewModel.beginGestureTransform() },
            onTransformGesture = { id, dx, dy, zoom, rot ->
                viewModel.transformElementGesture(id, dx, dy, zoom, rot)
            },
            onEndGesture = { viewModel.endGestureTransform() },
            onAddText = { viewModel.addTextElement() },
            onAddDynamicClock = { viewModel.addDynamicClockElement() },
            onAddBatteryBadge = { viewModel.addBatteryBadgeElement() },
            onAddBatteryGauge = { style -> viewModel.addBatteryGaugeElement(style) },
            onAddShape = { shapeType -> viewModel.addShapeElement(shapeType) },
            onImportImage = { uri, replaceId -> viewModel.importImageFromUri(uri, replaceId) },
            onUpdateElement = { id, recordHistory, transform ->
                viewModel.updateSelectedElement(id, recordHistory, transform)
            },
            onDuplicateElement = { id -> viewModel.duplicateElement(id) },
            onDeleteElement = { id -> viewModel.deleteElement(id) },
            onToggleVisibility = { id -> viewModel.toggleElementVisibility(id) },
            onBringForward = { id -> viewModel.bringForward(id) },
            onSendBackward = { id -> viewModel.sendBackward(id) },
            onUpdateBackground = { primary, secondary, gradient, anim ->
                viewModel.updateBackground(primary, secondary, gradient, anim)
            },
            onAddRule = { rule -> viewModel.addRule(rule) },
            onToggleRule = { id -> viewModel.toggleRule(id) },
            onDeleteRule = { id -> viewModel.deleteRule(id) },
            onSetSimulatorEnabled = { enabled -> viewModel.setSimulatorEnabled(enabled) },
            onUpdateSimulatorValues = { battery, charging, hour ->
                viewModel.updateSimulatorValues(battery, charging, hour)
            },
        )

        ScreenRoute.SettingsInfo -> SettingsInfoScreen(
            snackbarHostState = snackbarHostState,
            onBack = { viewModel.openHome() },
            onLaunchWallpaperPicker = requestWallpaperPicker,
            onReplayOnboarding = { viewModel.openOnboardingReplay() },
            onResetToPresets = { viewModel.resetToPresets() },
        )
    }

    if (showWallpaperGuideDialog) {
        AlertDialog(
            onDismissRequest = { showWallpaperGuideDialog = false },
            title = { Text("ロック画面への設定方法") },
            text = {
                Text("次の画面で「壁紙に設定」を押し、「ロック画面」（または「ホームとロック画面」）を選ぶと反映されます。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWallpaperGuideDialog = false
                        viewModel.launchLiveWallpaperPicker()
                    },
                ) {
                    Text("設定画面を開く")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWallpaperGuideDialog = false }) {
                    Text("あとで")
                }
            },
        )
    }
}
