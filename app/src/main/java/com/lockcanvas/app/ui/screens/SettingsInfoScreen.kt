package com.lockcanvas.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsInfoScreen(
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onLaunchWallpaperPicker: () -> Unit,
    onReplayOnboarding: () -> Unit,
    onResetToPresets: () -> Unit,
) {
    var showResetDialog by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("設定とアプリについて", style = MaterialTheme.typography.titleLarge) },
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
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    FilledTonalButton(
                        onClick = onLaunchWallpaperPicker,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Wallpaper,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("壁紙の設定画面を開く")
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    TextButton(
                        onClick = onReplayOnboarding,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Text("初回ガイドをもう一度見る")
                    }

                    TextButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Restore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "デザインを初期状態に戻す",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SpecListItem(
                icon = Icons.Outlined.Wallpaper,
                headline = "壁紙の設定と反映",
                supporting = "壁紙に設定したあとは、エディターで「保存」するだけでロック画面にすぐ反映されます",
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            SpecListItem(
                icon = Icons.Outlined.Layers,
                headline = "時計や通知と重なる場所",
                supporting = "ロック画面の時計・通知・指紋認証のマークは、スマホ側が手前に表示します。大事なパーツは中央あたりに置くのがおすすめです",
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            SpecListItem(
                icon = Icons.Outlined.LockClock,
                headline = "画面が暗いとき（いつも表示）",
                supporting = "いつも表示（AOD）のときは動きを止めて暗く表示します。ロック画面の上では、タップなどの操作はスマホ側が受け付けます",
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            SpecListItem(
                icon = Icons.Outlined.BatteryChargingFull,
                headline = "電池にやさしい設計",
                supporting = "画面が消えているときは描画を止めます。動きのないデザインでは、表示が変わるときだけ更新します",
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            WidgetPermissionSection()
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("初期状態に戻しますか？") },
            text = { Text("保存したデザインはすべて削除され、最初のプリセット6種類だけが残ります。あなたが作ったデザインもすべて削除されます。この操作は取り消せません。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetToPresets()
                    },
                ) {
                    Text("すべて削除して初期化", color = MaterialTheme.colorScheme.error)
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
private fun WidgetPermissionSection() {
    val context = LocalContext.current
    val widgetPermissions = arrayOf(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    fun allGranted(): Boolean = widgetPermissions.all { permission ->
        ContextCompat.checkSelfPermission(
            context,
            permission,
        ) == PackageManager.PERMISSION_GRANTED
    }

    var granted by remember { mutableStateOf(allGranted()) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        granted = result.values.all { it } || allGranted()
    }

    SpecListItem(
        icon = Icons.Outlined.Event,
        headline = "ロック画面ウィジェットの表示",
        supporting = if (granted) {
            "許可済みです。ウィジェットに次の予定と天気が表示されます"
        } else {
            "ウィジェットに「次の予定」と「天気」を出すには、カレンダーと位置情報の許可が必要です"
        },
    )
    if (!granted) {
        FilledTonalButton(
            onClick = { launcher.launch(widgetPermissions) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .height(44.dp),
        ) {
            Text("予定と天気の表示を許可する")
        }
    }
}

@Composable
private fun SpecListItem(
    icon: ImageVector,
    headline: String,
    supporting: String,
) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        },
        headlineContent = {
            Text(
                text = headline,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        supportingContent = {
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
