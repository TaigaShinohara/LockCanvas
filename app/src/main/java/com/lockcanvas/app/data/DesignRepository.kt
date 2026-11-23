package com.lockcanvas.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lockcanvas.app.model.CanvasDesign
import com.lockcanvas.app.model.PresetDesigns
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.lockCanvasDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "lock_canvas_designs",
)

val LockCanvasJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = false
}

class DesignRepository(context: Context) {

    private val dataStore: DataStore<Preferences> = context.applicationContext.lockCanvasDataStore

    private val safePreferencesFlow: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) {
            emit(emptyPreferences())
        } else {
            throw error
        }
    }

    val designsFlow: Flow<List<CanvasDesign>> = safePreferencesFlow
        .map { prefs -> decodeDesigns(prefs[DESIGNS_JSON_KEY]) }
        .distinctUntilChanged()

    val activeDesignIdFlow: Flow<String> = safePreferencesFlow
        .map { prefs -> prefs[ACTIVE_DESIGN_ID_KEY] ?: PresetDesigns.defaultDesign().id }
        .distinctUntilChanged()

    val activeDesignFlow: Flow<CanvasDesign> = safePreferencesFlow
        .map { prefs ->
            val designs = decodeDesigns(prefs[DESIGNS_JSON_KEY])
            val activeId = prefs[ACTIVE_DESIGN_ID_KEY]
            designs.firstOrNull { it.id == activeId } ?: designs.first()
        }
        .distinctUntilChanged()

    val onboardingCompletedVersionFlow: Flow<Int> = safePreferencesFlow
        .map { prefs -> prefs[ONBOARDING_COMPLETED_VERSION_KEY] ?: 0 }
        .distinctUntilChanged()

    val onboardingGoalKeyFlow: Flow<String?> = safePreferencesFlow
        .map { prefs -> prefs[ONBOARDING_GOAL_KEY] }
        .distinctUntilChanged()

    val firstSuccessAchievedFlow: Flow<Boolean> = safePreferencesFlow
        .map { prefs -> prefs[FIRST_SUCCESS_ACHIEVED_KEY] ?: false }
        .distinctUntilChanged()

    suspend fun saveOnboardingCompleted(
        version: Int,
        goalKey: String,
        firstSuccessAchieved: Boolean,
    ) {
        dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED_VERSION_KEY] = version
            prefs[ONBOARDING_GOAL_KEY] = goalKey
            if (firstSuccessAchieved) {
                prefs[FIRST_SUCCESS_ACHIEVED_KEY] = true
            }
        }
    }

    suspend fun markFirstSuccessAchieved() {
        dataStore.edit { prefs ->
            prefs[FIRST_SUCCESS_ACHIEVED_KEY] = true
        }
    }

    suspend fun saveDesign(design: CanvasDesign) {
        val stamped = design.copy(updatedAtEpochMs = System.currentTimeMillis())
        dataStore.edit { prefs ->
            val current = decodeDesigns(prefs[DESIGNS_JSON_KEY]).toMutableList()
            val idx = current.indexOfFirst { it.id == stamped.id }
            if (idx >= 0) {
                current[idx] = stamped
            } else {
                current.add(0, stamped)
            }
            prefs[DESIGNS_JSON_KEY] = LockCanvasJson.encodeToString(current)
            if (prefs[ACTIVE_DESIGN_ID_KEY].isNullOrBlank()) {
                prefs[ACTIVE_DESIGN_ID_KEY] = stamped.id
            }
        }
    }

    suspend fun deleteDesign(id: String) {
        dataStore.edit { prefs ->
            val current = decodeDesigns(prefs[DESIGNS_JSON_KEY]).toMutableList()
            val updated = current.filterNot { it.id == id }
            val nonEmpty = updated.ifEmpty { listOf(PresetDesigns.defaultDesign()) }
            prefs[DESIGNS_JSON_KEY] = LockCanvasJson.encodeToString(nonEmpty)
            val activeId = prefs[ACTIVE_DESIGN_ID_KEY]
            if (activeId == id || nonEmpty.none { it.id == activeId }) {
                prefs[ACTIVE_DESIGN_ID_KEY] = nonEmpty.first().id
            }
        }
    }

    suspend fun setActiveDesign(id: String) {
        dataStore.edit { prefs ->
            val current = decodeDesigns(prefs[DESIGNS_JSON_KEY])
            if (prefs[DESIGNS_JSON_KEY] == null) {
                prefs[DESIGNS_JSON_KEY] = LockCanvasJson.encodeToString(current)
            }
            val resolvedId = if (current.any { it.id == id }) id else current.first().id
            prefs[ACTIVE_DESIGN_ID_KEY] = resolvedId
        }
    }

    suspend fun duplicateDesign(id: String): CanvasDesign? {
        var duplicated: CanvasDesign? = null
        dataStore.edit { prefs ->
            val current = decodeDesigns(prefs[DESIGNS_JSON_KEY]).toMutableList()
            val source = current.firstOrNull { it.id == id } ?: return@edit
            val copy = source.copy(
                id = "design_${UUID.randomUUID().toString().take(8)}",
                name = "${source.name} Copy",
                updatedAtEpochMs = System.currentTimeMillis(),
            )
            val sourceIdx = current.indexOfFirst { it.id == id }
            current.add(sourceIdx + 1, copy)
            prefs[DESIGNS_JSON_KEY] = LockCanvasJson.encodeToString(current)
            duplicated = copy
        }
        return duplicated
    }

    suspend fun resetToPresets() {
        val presets = PresetDesigns.all()
        dataStore.edit { prefs ->
            prefs[DESIGNS_JSON_KEY] = LockCanvasJson.encodeToString(presets)
            prefs[ACTIVE_DESIGN_ID_KEY] = presets.first().id
        }
    }

    private fun decodeDesigns(rawJson: String?): List<CanvasDesign> {
        if (rawJson.isNullOrBlank()) return PresetDesigns.all()
        return runCatching {
            val parsed = LockCanvasJson.decodeFromString<List<CanvasDesign>>(rawJson)
            parsed.ifEmpty { PresetDesigns.all() }
        }.getOrElse {
            PresetDesigns.all()
        }
    }

    companion object {
        private val DESIGNS_JSON_KEY = stringPreferencesKey("designs_json")
        private val ACTIVE_DESIGN_ID_KEY = stringPreferencesKey("active_design_id")
        private val ONBOARDING_COMPLETED_VERSION_KEY = intPreferencesKey("onboarding_completed_version")
        private val ONBOARDING_GOAL_KEY = stringPreferencesKey("onboarding_goal_key")
        private val FIRST_SUCCESS_ACHIEVED_KEY = booleanPreferencesKey("first_success_achieved")

        @Volatile
        private var instance: DesignRepository? = null

        fun getInstance(context: Context): DesignRepository =
            instance ?: synchronized(this) {
                instance ?: DesignRepository(context.applicationContext).also { instance = it }
            }
    }
}
