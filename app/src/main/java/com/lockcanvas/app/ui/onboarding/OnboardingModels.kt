package com.lockcanvas.app.ui.onboarding

import com.lockcanvas.app.model.PresetDesigns

const val CURRENT_ONBOARDING_VERSION = 1

enum class OnboardingGoal(
    val storageKey: String,
    val choiceLabel: String,
    val echoPhrase: String,
    val matchStrength: String,
    val recommendedPresetId: String,
    val ctaSubtitle: String,
) {
    FREE_LAYOUT(
        storageKey = "free_layout",
        choiceLabel = "自分好みに自由に配置したい",
        echoPhrase = "自分好みに自由に配置したいあなたには",
        matchStrength = "ドラッグと2本指操作で、ロック画面の好きな場所へ思い通りに置けます。",
        recommendedPresetId = PresetDesigns.ID_MINIMAL_CLOCK,
        ctaSubtitle = "「ミニマル時計」をエディターで開きます",
    ),
    DYNAMIC_INFO(
        storageKey = "dynamic_info",
        choiceLabel = "電池や時刻をかっこよく見たい",
        echoPhrase = "電池や時刻をかっこよく見たいあなたには",
        matchStrength = "バッテリー残量や充電状態に応じて、リアルタイムに表情が変わります。",
        recommendedPresetId = PresetDesigns.ID_CYBER_NEON,
        ctaSubtitle = "「サイバーネオン」をエディターで開きます",
    ),
    UNDECIDED(
        storageKey = "undecided",
        choiceLabel = "まだわからない・いろいろ試したい",
        echoPhrase = "まずは気軽に試してみたいあなたには",
        matchStrength = "用意されたテンプレートを少し動かすだけで、雰囲気の違いをすぐ体感できます。",
        recommendedPresetId = PresetDesigns.ID_NIGHT_ZEN,
        ctaSubtitle = "「ナイト禅」をエディターで開きます",
    ),
    ;

    companion object {
        fun fromKey(key: String?): OnboardingGoal =
            entries.firstOrNull { it.storageKey == key } ?: UNDECIDED
    }
}

enum class OnboardingPageRole(val chapterLabel: String) {
    HOOK("はじめに"),
    GOAL_CHOICE("あなたの希望"),
    MATCH_RESULT("おすすめの理由"),
    IDENTITY("LockCanvasとは"),
    VALUE_DYNAMIC("リアルタイム表示"),
    VALUE_SAFE_AREA("きれいな配置"),
    STEP_1("使い方 ステップ1"),
    STEP_2("使い方 ステップ2"),
    STEP_3("使い方 ステップ3"),
    TRY_DRAG("かんたん体験"),
    TRY_RESULT("体験の結果"),
    CONNECT("はじめる準備"),
}

data class OnboardingState(
    val completedVersion: Int = 0,
    val selectedGoal: OnboardingGoal = OnboardingGoal.UNDECIDED,
    val firstSuccessAchieved: Boolean = false,
) {
    val shouldShowOnboarding: Boolean
        get() = completedVersion < CURRENT_ONBOARDING_VERSION
}

object OnboardingPageOrder {
    fun buildPages(goal: OnboardingGoal): List<OnboardingPageRole> {
        val valuePages = when (goal) {
            OnboardingGoal.FREE_LAYOUT -> listOf(
                OnboardingPageRole.VALUE_SAFE_AREA,
                OnboardingPageRole.VALUE_DYNAMIC,
            )
            OnboardingGoal.DYNAMIC_INFO,
            OnboardingGoal.UNDECIDED,
            -> listOf(
                OnboardingPageRole.VALUE_DYNAMIC,
                OnboardingPageRole.VALUE_SAFE_AREA,
            )
        }
        return listOf(
            OnboardingPageRole.HOOK,
            OnboardingPageRole.GOAL_CHOICE,
            OnboardingPageRole.MATCH_RESULT,
            OnboardingPageRole.IDENTITY,
        ) + valuePages + listOf(
            OnboardingPageRole.STEP_1,
            OnboardingPageRole.STEP_2,
            OnboardingPageRole.STEP_3,
            OnboardingPageRole.TRY_DRAG,
            OnboardingPageRole.TRY_RESULT,
            OnboardingPageRole.CONNECT,
        )
    }
}

class PraiseRotator {
    private var lastMediumIndex = -1
    private var lastLargeIndex = -1

    private val mediumGoalPraises = listOf(
        "教えてくれてありがとう。あなたに合う流れを用意します。",
        "いい選び方です。その目的に合わせて案内します。",
        "なるほど、そこにぴったり合う使い方をご紹介します。",
    )

    private val mediumTryPraises = listOf(
        "きれいに動かせました。その感覚で自由に配置できます。",
        "ばっちりです。指先ひとつで位置を決められます。",
        "いい動きです。このまま自分のレイアウトを作れます。",
    )

    private val largeCompletionPraises = listOf(
        "準備完了です。あなただけのキャンバスを始めましょう。",
        "おつかれさまでした。さっそく最初の1枚を作ってみましょう。",
        "さあ、ロック画面を自分らしく彩りましょう。",
    )

    fun nextGoalPraise(): String {
        val idx = (lastMediumIndex + 1) % mediumGoalPraises.size
        lastMediumIndex = idx
        return mediumGoalPraises[idx]
    }

    fun nextTryPraise(): String {
        val idx = (lastMediumIndex + 1) % mediumTryPraises.size
        lastMediumIndex = idx
        return mediumTryPraises[idx]
    }

    fun nextCompletionPraise(): String {
        val idx = (lastLargeIndex + 1) % largeCompletionPraises.size
        lastLargeIndex = idx
        return largeCompletionPraises[idx]
    }
}

data class WindowDot(
    val pageIndex: Int,
    val isCurrent: Boolean,
    val isEdgeSmall: Boolean,
)

object WindowDotCalculator {
    const val MAX_VISIBLE_DOTS = 5

    fun compute(currentPage: Int, totalPages: Int): List<WindowDot> {
        if (totalPages <= 0) return emptyList()
        val clamped = currentPage.coerceIn(0, totalPages - 1)
        if (totalPages <= MAX_VISIBLE_DOTS) {
            return (0 until totalPages).map { idx ->
                WindowDot(pageIndex = idx, isCurrent = idx == clamped, isEdgeSmall = false)
            }
        }
        val half = MAX_VISIBLE_DOTS / 2
        val start = (clamped - half).coerceIn(0, totalPages - MAX_VISIBLE_DOTS)
        val end = start + MAX_VISIBLE_DOTS - 1
        return (start..end).map { idx ->
            val isLeftOverflowEdge = idx == start && start > 0
            val isRightOverflowEdge = idx == end && end < totalPages - 1
            WindowDot(
                pageIndex = idx,
                isCurrent = idx == clamped,
                isEdgeSmall = isLeftOverflowEdge || isRightOverflowEdge,
            )
        }
    }
}
