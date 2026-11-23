package com.lockcanvas.app.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lockcanvas.app.data.DesignRepository
import com.lockcanvas.app.model.AnimationStyle
import com.lockcanvas.app.model.BatteryGaugeElement
import com.lockcanvas.app.model.BatteryGaugeStyle
import com.lockcanvas.app.model.CanvasDesign
import com.lockcanvas.app.model.CanvasElement
import com.lockcanvas.app.model.ConditionalRule
import com.lockcanvas.app.model.DynamicDataSnapshot
import com.lockcanvas.app.model.ImageElement
import com.lockcanvas.app.model.PresetDesigns
import com.lockcanvas.app.model.ShapeElement
import com.lockcanvas.app.model.ShapeType
import com.lockcanvas.app.model.TextElement
import com.lockcanvas.app.ui.onboarding.CURRENT_ONBOARDING_VERSION
import com.lockcanvas.app.ui.onboarding.OnboardingGoal
import com.lockcanvas.app.wallpaper.WallpaperLauncher
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SnackbarEvent(
    val message: String,
    val actionLabel: String? = null,
    val openWallpaperPickerOnAction: Boolean = false,
)

class LockCanvasViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DesignRepository.getInstance(application)

    val designs: StateFlow<List<CanvasDesign>> = repository.designsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PresetDesigns.all(),
        )

    val activeDesignId: StateFlow<String> = repository.activeDesignIdFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PresetDesigns.defaultDesign().id,
        )

    private val _route = MutableStateFlow<ScreenRoute>(ScreenRoute.Home)
    val route: StateFlow<ScreenRoute> = _route.asStateFlow()

    private val _onboardingChecked = MutableStateFlow(false)
    val onboardingChecked: StateFlow<Boolean> = _onboardingChecked.asStateFlow()

    private val _onboardingGoal = MutableStateFlow(OnboardingGoal.UNDECIDED)
    val onboardingGoal: StateFlow<OnboardingGoal> = _onboardingGoal.asStateFlow()

    private val _editorState = MutableStateFlow(EditorUiState())
    val editorState: StateFlow<EditorUiState> = _editorState.asStateFlow()

    private val _liveSnapshot = MutableStateFlow(readSystemSnapshot())
    val liveSnapshot: StateFlow<DynamicDataSnapshot> = _liveSnapshot.asStateFlow()

    private val _isWallpaperActive = MutableStateFlow(
        WallpaperLauncher.isLockCanvasActive(application),
    )
    val isWallpaperActive: StateFlow<Boolean> = _isWallpaperActive.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<SnackbarEvent>(extraBufferCapacity = 4)
    val snackbarMessages: SharedFlow<SnackbarEvent> = _snackbarMessages.asSharedFlow()

    private var gestureStartDesign: CanvasDesign? = null
    private var gestureElementId: String? = null
    private var gestureRawX: Float? = null
    private var gestureRawY: Float? = null

    private val systemStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            _liveSnapshot.value = readSystemSnapshot()
        }
    }
    private var receiverRegistered = false

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        runCatching {
            ContextCompat.registerReceiver(
                getApplication(),
                systemStateReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            receiverRegistered = true
        }
        viewModelScope.launch {
            val completedVersion = repository.onboardingCompletedVersionFlow.first()
            val savedGoal = OnboardingGoal.fromKey(repository.onboardingGoalKeyFlow.first())
            _onboardingGoal.value = savedGoal
            if (completedVersion < CURRENT_ONBOARDING_VERSION) {
                _route.value = ScreenRoute.Onboarding
            }
            _onboardingChecked.value = true
        }
        viewModelScope.launch {
            while (isActive) {
                _liveSnapshot.value = readSystemSnapshot()
                delay(30_000L)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (receiverRegistered) {
            runCatching { getApplication<Application>().unregisterReceiver(systemStateReceiver) }
            receiverRegistered = false
        }
    }

    fun refreshWallpaperStatus() {
        _isWallpaperActive.value = WallpaperLauncher.isLockCanvasActive(getApplication())
        _liveSnapshot.value = readSystemSnapshot()
    }

    fun openHome() {
        _route.value = ScreenRoute.Home
        refreshWallpaperStatus()
    }

    fun openSettingsInfo() {
        _route.value = ScreenRoute.SettingsInfo
    }

    fun openOnboardingReplay() {
        _route.value = ScreenRoute.Onboarding
    }

    fun completeOnboardingToEditor(goal: OnboardingGoal, trySucceeded: Boolean) {
        _onboardingGoal.value = goal
        viewModelScope.launch {
            repository.saveOnboardingCompleted(
                version = CURRENT_ONBOARDING_VERSION,
                goalKey = goal.storageKey,
                firstSuccessAchieved = trySucceeded,
            )
            val allDesigns = repository.designsFlow.first()
            val recommended = allDesigns.firstOrNull { it.id == goal.recommendedPresetId }
                ?: allDesigns.firstOrNull()
                ?: PresetDesigns.defaultDesign()
            repository.setActiveDesign(recommended.id)
            openEditor(recommended)
        }
    }

    fun completeOnboardingLater(goal: OnboardingGoal, isSkip: Boolean) {
        _onboardingGoal.value = goal
        viewModelScope.launch {
            repository.saveOnboardingCompleted(
                version = CURRENT_ONBOARDING_VERSION,
                goalKey = goal.storageKey,
                firstSuccessAchieved = false,
            )
            openHome()
            if (!isSkip) {
                _snackbarMessages.tryEmit(SnackbarEvent("あとで、エディターからいつでも試せます"))
            }
        }
    }

    fun openEditor(design: CanvasDesign) {
        _editorState.value = EditorUiState(
            design = design,
            selectedElementId = null,
            showSafeAreaGuides = true,
            activeTab = null,
            undoStack = emptyList(),
            redoStack = emptyList(),
            hasUnsavedChanges = false,
        )
        _route.value = ScreenRoute.Editor
    }

    fun createNewDesignAndEdit() {
        val nextNum = designs.value.size + 1
        val newDesign = CanvasDesign(
            id = "design_${UUID.randomUUID().toString().take(8)}",
            name = "マイデザイン $nextNum",
            backgroundColorArgb = 0xFF0B1020,
            secondaryBackgroundColorArgb = 0xFF1A2540,
            backgroundGradientEnabled = true,
            animationStyle = AnimationStyle.NONE,
            elements = listOf(
                TextElement(
                    id = newElementId(),
                    xFraction = 0.5f,
                    yFraction = 0.48f,
                    text = "{time}",
                    colorArgb = 0xFFF8FAFC,
                    fontSizeSp = 38f,
                    bold = true,
                    dynamicTemplate = true,
                    zIndex = 0,
                ),
                BatteryGaugeElement(
                    id = newElementId(),
                    xFraction = 0.5f,
                    yFraction = 0.57f,
                    style = BatteryGaugeStyle.BAR,
                    widthFraction = 0.56f,
                    heightFraction = 0.036f,
                    activeColorArgb = 0xFF64D2FF,
                    zIndex = 1,
                ),
            ),
            updatedAtEpochMs = System.currentTimeMillis(),
        )
        viewModelScope.launch {
            repository.saveDesign(newDesign)
            openEditor(newDesign)
        }
    }

    fun applyDesign(designId: String) {
        viewModelScope.launch {
            repository.setActiveDesign(designId)
            val active = WallpaperLauncher.isLockCanvasActive(getApplication())
            _isWallpaperActive.value = active
            if (active) {
                _snackbarMessages.tryEmit(SnackbarEvent("ロック画面のデザインを切り替えました"))
            } else {
                _snackbarMessages.tryEmit(
                    SnackbarEvent(
                        message = "デザインを選択しました",
                        actionLabel = "壁紙に設定",
                        openWallpaperPickerOnAction = true,
                    ),
                )
            }
        }
    }

    fun duplicateSavedDesign(designId: String) {
        viewModelScope.launch {
            val copy = repository.duplicateDesign(designId)
            if (copy != null) {
                _snackbarMessages.tryEmit(SnackbarEvent("「${copy.name}」を複製しました"))
            }
        }
    }

    fun deleteSavedDesign(designId: String) {
        viewModelScope.launch {
            repository.deleteDesign(designId)
            _snackbarMessages.tryEmit(SnackbarEvent("デザインを削除しました"))
        }
    }

    fun resetToPresets() {
        viewModelScope.launch {
            repository.resetToPresets()
            _snackbarMessages.tryEmit(SnackbarEvent("プリセットを初期状態に戻しました"))
        }
    }

    fun saveAndApplyCurrentDesign() {
        val current = _editorState.value.design
        viewModelScope.launch {
            repository.saveDesign(current)
            repository.setActiveDesign(current.id)
            repository.markFirstSuccessAchieved()
            _editorState.update { it.copy(hasUnsavedChanges = false) }
            val active = WallpaperLauncher.isLockCanvasActive(getApplication())
            _isWallpaperActive.value = active
            if (active) {
                _snackbarMessages.tryEmit(SnackbarEvent("保存してロック画面へ反映しました"))
            } else {
                _snackbarMessages.tryEmit(
                    SnackbarEvent(
                        message = "保存しました",
                        actionLabel = "ロック画面に設定",
                        openWallpaperPickerOnAction = true,
                    ),
                )
            }
        }
    }

    fun launchLiveWallpaperPicker(): Boolean {
        val launched = WallpaperLauncher.launchLiveWallpaperPicker(getApplication())
        if (!launched) {
            _snackbarMessages.tryEmit(
                SnackbarEvent("壁紙ピッカーを開けませんでした。設定 > 壁紙 から選択してください"),
            )
        }
        return launched
    }

    // --- Editor State Controls ---

    fun selectElement(elementId: String?) {
        _editorState.update { current ->
            val nextTab = when {
                elementId == null && current.activeTab == EditorToolTab.EDIT -> null
                elementId != null && current.activeTab == EditorToolTab.ADD -> EditorToolTab.EDIT
                else -> current.activeTab
            }
            current.copy(
                selectedElementId = elementId,
                activeTab = nextTab,
            )
        }
    }

    fun selectToolTab(tab: EditorToolTab?) {
        _editorState.update { current ->
            val resolved = if (current.activeTab == tab) null else tab
            current.copy(activeTab = resolved)
        }
    }

    fun toggleSafeAreaGuides() {
        _editorState.update { it.copy(showSafeAreaGuides = !it.showSafeAreaGuides) }
    }

    fun renameDesign(newName: String) {
        val trimmed = newName.trim().ifEmpty { "無題のデザイン" }
        _editorState.update { state ->
            EditorReducer.pushDesignChange(state, state.design.copy(name = trimmed))
        }
    }

    fun undo() {
        _editorState.update { EditorReducer.undo(it) }
    }

    fun redo() {
        _editorState.update { EditorReducer.redo(it) }
    }

    // --- Gesture Transforms (drag / pinch / rotate) ---

    fun beginGestureTransform() {
        gestureStartDesign = _editorState.value.design
        gestureElementId = null
        gestureRawX = null
        gestureRawY = null
    }

    fun dragElementByFraction(elementId: String, dxFraction: Float, dyFraction: Float) {
        _editorState.update { state ->
            val target = state.design.elements.firstOrNull { it.id == elementId } ?: return@update state
            if (gestureElementId != elementId || gestureRawX == null || gestureRawY == null) {
                gestureElementId = elementId
                gestureRawX = target.xFraction
                gestureRawY = target.yFraction
            }
            val rawX = ((gestureRawX ?: target.xFraction) + dxFraction).coerceIn(0.02f, 0.98f)
            val rawY = ((gestureRawY ?: target.yFraction) + dyFraction).coerceIn(0.02f, 0.98f)
            gestureRawX = rawX
            gestureRawY = rawY
            val snapped = EditorReducer.snapToCenter(rawX, rawY)
            val updatedState = EditorReducer.updateElement(
                state = state,
                elementId = elementId,
                recordHistory = false,
            ) { el ->
                el.withPosition(snapped.xFraction, snapped.yFraction)
            }
            updatedState.copy(
                snapGuideX = snapped.snappedX,
                snapGuideY = snapped.snappedY,
            )
        }
    }

    fun transformElementGesture(
        elementId: String,
        dxFraction: Float,
        dyFraction: Float,
        zoomFactor: Float,
        rotationDeltaDegrees: Float,
    ) {
        _editorState.update { state ->
            val target = state.design.elements.firstOrNull { it.id == elementId } ?: return@update state
            if (gestureElementId != elementId || gestureRawX == null || gestureRawY == null) {
                gestureElementId = elementId
                gestureRawX = target.xFraction
                gestureRawY = target.yFraction
            }
            val rawX = ((gestureRawX ?: target.xFraction) + dxFraction).coerceIn(0.02f, 0.98f)
            val rawY = ((gestureRawY ?: target.yFraction) + dyFraction).coerceIn(0.02f, 0.98f)
            gestureRawX = rawX
            gestureRawY = rawY
            val snapped = EditorReducer.snapToCenter(rawX, rawY)
            val nextScale = (target.scale * zoomFactor).coerceIn(0.35f, 3.0f)
            val nextRotation = normalizeDegrees(target.rotationDegrees + rotationDeltaDegrees)
            val updatedState = EditorReducer.updateElement(
                state = state,
                elementId = elementId,
                recordHistory = false,
            ) { el ->
                el.withPosition(snapped.xFraction, snapped.yFraction)
                    .withTransform(scale = nextScale, rotationDegrees = nextRotation)
            }
            updatedState.copy(
                snapGuideX = snapped.snappedX,
                snapGuideY = snapped.snappedY,
            )
        }
    }

    fun endGestureTransform() {
        val before = gestureStartDesign ?: return
        gestureStartDesign = null
        gestureElementId = null
        gestureRawX = null
        gestureRawY = null
        _editorState.update { EditorReducer.commitGestureSnapshot(it, before) }
    }

    // --- Element Creation ---

    fun addTextElement() {
        val el = TextElement(
            id = newElementId(),
            xFraction = 0.5f,
            yFraction = 0.52f,
            text = "LockCanvas",
            colorArgb = 0xFFF8FAFC,
            fontSizeSp = 24f,
            bold = true,
            dynamicTemplate = true,
        )
        _editorState.update {
            EditorReducer.addElement(it, el).copy(activeTab = EditorToolTab.EDIT)
        }
    }

    fun addDynamicClockElement() {
        val el = TextElement(
            id = newElementId(),
            xFraction = 0.5f,
            yFraction = 0.46f,
            text = "{time}\n{date}",
            colorArgb = 0xFFF8FAFC,
            fontSizeSp = 32f,
            bold = true,
            dynamicTemplate = true,
        )
        _editorState.update {
            EditorReducer.addElement(it, el).copy(activeTab = EditorToolTab.EDIT)
        }
    }

    fun addBatteryBadgeElement() {
        val el = TextElement(
            id = newElementId(),
            xFraction = 0.5f,
            yFraction = 0.62f,
            text = "🔋 {battery} · {charging_status}",
            colorArgb = 0xFF64D2FF,
            fontSizeSp = 15f,
            bold = true,
            dynamicTemplate = true,
        )
        _editorState.update {
            EditorReducer.addElement(it, el).copy(activeTab = EditorToolTab.EDIT)
        }
    }

    fun addBatteryGaugeElement(style: BatteryGaugeStyle) {
        val el = when (style) {
            BatteryGaugeStyle.BAR -> BatteryGaugeElement(
                id = newElementId(),
                xFraction = 0.5f,
                yFraction = 0.58f,
                style = BatteryGaugeStyle.BAR,
                widthFraction = 0.60f,
                heightFraction = 0.04f,
                activeColorArgb = 0xFF34D399,
            )
            BatteryGaugeStyle.CIRCULAR -> BatteryGaugeElement(
                id = newElementId(),
                xFraction = 0.5f,
                yFraction = 0.52f,
                style = BatteryGaugeStyle.CIRCULAR,
                widthFraction = 0.40f,
                heightFraction = 0.20f,
                activeColorArgb = 0xFF64D2FF,
            )
        }
        _editorState.update {
            EditorReducer.addElement(it, el).copy(activeTab = EditorToolTab.EDIT)
        }
    }

    fun addShapeElement(shapeType: ShapeType) {
        val el = when (shapeType) {
            ShapeType.RECTANGLE -> ShapeElement(
                id = newElementId(),
                xFraction = 0.5f,
                yFraction = 0.52f,
                shapeType = ShapeType.RECTANGLE,
                widthFraction = 0.72f,
                heightFraction = 0.16f,
                fillColorArgb = 0x22FFFFFF,
                strokeColorArgb = 0x5564D2FF,
                cornerRadiusDp = 24f,
            )
            ShapeType.CIRCLE -> ShapeElement(
                id = newElementId(),
                xFraction = 0.5f,
                yFraction = 0.52f,
                shapeType = ShapeType.CIRCLE,
                widthFraction = 0.46f,
                heightFraction = 0.22f,
                fillColorArgb = 0x2264D2FF,
                strokeColorArgb = 0x6664D2FF,
            )
            ShapeType.PILL -> ShapeElement(
                id = newElementId(),
                xFraction = 0.5f,
                yFraction = 0.64f,
                shapeType = ShapeType.PILL,
                widthFraction = 0.60f,
                heightFraction = 0.055f,
                fillColorArgb = 0x22A855F7,
                strokeColorArgb = 0x88C084FC,
            )
        }
        _editorState.update {
            EditorReducer.addElement(it, el).copy(activeTab = EditorToolTab.EDIT)
        }
    }

    fun importImageFromUri(sourceUri: Uri, replaceElementId: String? = null) {
        viewModelScope.launch {
            val savedFileUri = withContext(Dispatchers.IO) {
                copyUriToInternalStorage(sourceUri)
            }
            if (savedFileUri == null) {
                _snackbarMessages.tryEmit(SnackbarEvent("画像の読み込みに失敗しました"))
                return@launch
            }
            if (replaceElementId != null) {
                updateSelectedElement(replaceElementId) { el ->
                    if (el is ImageElement) el.copy(uriString = savedFileUri) else el
                }
            } else {
                val imageEl = ImageElement(
                    id = newElementId(),
                    xFraction = 0.5f,
                    yFraction = 0.50f,
                    uriString = savedFileUri,
                    widthFraction = 0.44f,
                    heightFraction = 0.24f,
                    cornerRadiusDp = 20f,
                )
                _editorState.update {
                    EditorReducer.addElement(it, imageEl).copy(activeTab = EditorToolTab.EDIT)
                }
            }
        }
    }

    // --- Element Updates ---

    fun updateSelectedElement(
        elementId: String,
        recordHistory: Boolean = true,
        transform: (CanvasElement) -> CanvasElement,
    ) {
        _editorState.update { state ->
            EditorReducer.updateElement(state, elementId, recordHistory, transform)
        }
    }

    fun duplicateElement(elementId: String) {
        _editorState.update { EditorReducer.duplicateElement(it, elementId) }
    }

    fun deleteElement(elementId: String) {
        _editorState.update { EditorReducer.deleteElement(it, elementId) }
    }

    fun toggleElementVisibility(elementId: String) {
        _editorState.update { EditorReducer.toggleVisibility(it, elementId) }
    }

    fun bringForward(elementId: String) {
        _editorState.update { EditorReducer.bringForward(it, elementId) }
    }

    fun sendBackward(elementId: String) {
        _editorState.update { EditorReducer.sendBackward(it, elementId) }
    }

    // --- Background & Animation ---

    fun updateBackground(
        primaryColorArgb: Long = _editorState.value.design.backgroundColorArgb,
        secondaryColorArgb: Long = _editorState.value.design.secondaryBackgroundColorArgb,
        gradientEnabled: Boolean = _editorState.value.design.backgroundGradientEnabled,
        animationStyle: AnimationStyle = _editorState.value.design.animationStyle,
    ) {
        _editorState.update { state ->
            val updated = state.design.copy(
                backgroundColorArgb = primaryColorArgb,
                secondaryBackgroundColorArgb = secondaryColorArgb,
                backgroundGradientEnabled = gradientEnabled,
                animationStyle = animationStyle,
            )
            EditorReducer.pushDesignChange(state, updated)
        }
    }

    // --- Conditional Rules & Simulator ---

    fun addRule(rule: ConditionalRule) {
        _editorState.update { EditorReducer.addRule(it, rule) }
    }

    fun toggleRule(ruleId: String) {
        _editorState.update { EditorReducer.toggleRule(it, ruleId) }
    }

    fun deleteRule(ruleId: String) {
        _editorState.update { EditorReducer.deleteRule(it, ruleId) }
    }

    fun setSimulatorEnabled(enabled: Boolean) {
        _editorState.update { it.copy(simulatorEnabled = enabled) }
    }

    fun updateSimulatorValues(
        batteryPercent: Int = _editorState.value.simulatedBatteryPercent,
        isCharging: Boolean = _editorState.value.simulatedIsCharging,
        hourOfDay: Int = _editorState.value.simulatedHourOfDay,
    ) {
        _editorState.update {
            it.copy(
                simulatedBatteryPercent = batteryPercent.coerceIn(0, 100),
                simulatedIsCharging = isCharging,
                simulatedHourOfDay = hourOfDay.coerceIn(0, 23),
            )
        }
    }

    // --- Helpers ---

    private fun copyUriToInternalStorage(sourceUri: Uri): String? = runCatching {
        val ctx = getApplication<Application>()
        val dir = File(ctx.filesDir, "canvas_images").apply { mkdirs() }
        val dest = File(dir, "img_${UUID.randomUUID().toString().take(8)}.jpg")
        ctx.contentResolver.openInputStream(sourceUri)?.use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return null
        Uri.fromFile(dest).toString()
    }.getOrNull()

    private fun readSystemSnapshot(): DynamicDataSnapshot {
        val ctx = getApplication<Application>()
        val intent = runCatching {
            ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) {
            ((level * 100f) / scale).toInt().coerceIn(0, 100)
        } else {
            82
        }
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return DynamicDataSnapshot.fromSystem(
            batteryPercent = percent,
            isCharging = charging,
        )
    }

    private fun newElementId(): String = "el_${UUID.randomUUID().toString().take(8)}"

    private fun normalizeDegrees(deg: Float): Float {
        var result = deg % 360f
        if (result > 180f) result -= 360f
        if (result < -180f) result += 360f
        return result
    }
}
