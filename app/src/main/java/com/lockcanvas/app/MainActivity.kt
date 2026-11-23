package com.lockcanvas.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lockcanvas.app.ui.LockCanvasViewModel
import com.lockcanvas.app.ui.ScreenRoute
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
    val designs by viewModel.designs.collectAsStateWithLifecycle()
    val activeDesignId by viewModel.activeDesignId.collectAsStateWithLifecycle()
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val liveSnapshot by viewModel.liveSnapshot.collectAsStateWithLifecycle()
    val isWallpaperActive by viewModel.isWallpaperActive.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

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
                viewModel.launchLiveWallpaperPicker()
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshWallpaperStatus()
    }

    when (route) {
        ScreenRoute.Home -> HomeScreen(
            designs = designs,
            activeDesignId = activeDesignId,
            snapshot = liveSnapshot,
            isWallpaperActive = isWallpaperActive,
            snackbarHostState = snackbarHostState,
            onApplyWallpaperClick = { viewModel.launchLiveWallpaperPicker() },
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
            onLaunchWallpaperPicker = { viewModel.launchLiveWallpaperPicker() },
            onResetToPresets = { viewModel.resetToPresets() },
        )
    }
}
