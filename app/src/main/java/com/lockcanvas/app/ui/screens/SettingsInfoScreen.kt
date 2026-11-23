package com.lockcanvas.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsInfoScreen(
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onLaunchWallpaperPicker: () -> Unit,
    onResetToPresets: () -> Unit,
) {
    var showResetDialog by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("仕様と設定ガイド", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "戻る",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoSectionCard(
                icon = Icons.Outlined.Wallpaper,
                title = "ロック画面への適用方法",
                body = "「ロック画面に設定」ボタンからAndroid標準のライブ壁紙ピッカーが開きます。プレビュー確認後、「壁紙に設定」→「ロック画面」または「ホーム画面とロック画面」を選択してください。設定後はエディタの「保存」ボタンで即時に反映されます。",
            )

            InfoSectionCard(
                icon = Icons.Outlined.Layers,
                title = "Androidロック画面の表示領域",
                body = "OS標準の時計・日付・通知カード・指紋認証アイコン・下部ショートカットはシステムが最前面に描画するため、ライブ壁紙で消去・上書きすることはできません。エディタのセーフエリアガイド（上部28%・下部20%）を参考に、中央エリアへ配置すると重なりを防げます。",
            )

            InfoSectionCard(
                icon = Icons.Outlined.LockClock,
                title = "AOD（常時表示）とタッチ操作",
                body = "常時表示ディスプレイ（AOD）はOSが管理するため、画面消灯・AOD中はライブ壁紙の描画が停止または減光されます。また、ロック画面上のタッチ操作はOSが優先処理するため、ライブ壁紙自体は表示・動的情報反映に特化しています。",
            )

            InfoSectionCard(
                icon = Icons.Outlined.BatteryChargingFull,
                title = "省電力・ゼロバッテリー設計",
                body = "画面がオフまたは非表示の間は描画ループを完全に停止します。アニメーション「なし」のデザインでは、時刻・バッテリー変化・デザイン保存時のみ再描画するイベント駆動方式のため、待機電力への影響を最小限に抑えます。",
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onLaunchWallpaperPicker,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Wallpaper,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("ライブ壁紙ピッカーを開く")
            }

            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Restore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("プリセットデザインを初期状態に戻す")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("プリセットを初期化") },
            text = { Text("保存済みのデザインを初期プリセット3種類に戻しますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetToPresets()
                    },
                ) {
                    Text("初期化する", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("キャンセル")
                }
            },
        )
    }
}

@Composable
private fun InfoSectionCard(
    icon: ImageVector,
    title: String,
    body: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
