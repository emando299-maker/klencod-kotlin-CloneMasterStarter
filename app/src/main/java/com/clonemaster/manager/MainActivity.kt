package com.clonemaster.manager

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class InstalledApp(
    val label: String,
    val packageName: String,
    val isSystem: Boolean
)

class MainActivity : ComponentActivity() {
    private var importedApkName by mutableStateOf<String?>(null)
    private val apkPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            importedApkName = uri.lastPathSegment ?: "APK selected"
            // Starter stage: we deliberately do not install, modify, or re-sign the APK.
            // Next stage will inspect package metadata and validate clone feasibility.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val apps = loadLaunchableApps()
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    CloneMasterScreen(
                        apps = apps,
                        importedApkName = importedApkName,
                        onImportApk = { apkPicker.launch(arrayOf("application/vnd.android.package-archive", "*/*")) },
                        onAppClick = { app ->
                            // Selecting an app only opens its details in Android settings for this starter.
                            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.parse("package:${app.packageName}"))
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }

    private fun loadLaunchableApps(): List<InstalledApp> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { resolve ->
                val info = resolve.activityInfo?.applicationInfo ?: return@mapNotNull null
                val label = pm.getApplicationLabel(info).toString()
                InstalledApp(label, info.packageName,
                    (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloneMasterScreen(
    apps: List<InstalledApp>,
    importedApkName: String?,
    onImportApk: () -> Unit,
    onAppClick: (InstalledApp) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var showSystem by remember { mutableStateOf(false) }
    val filtered = apps.filter {
        (showSystem || !it.isSystem) &&
        (it.label.contains(query, ignoreCase = true) ||
         it.packageName.contains(query, ignoreCase = true))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clone Master") },
                navigationIcon = { Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.padding(start = 16.dp)) }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("مدير التطبيقات", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                "نموذج أولي: يعرض التطبيقات المثبتة ويتيح اختيار ملف APK للفحص في المرحلة التالية.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ابحث عن تطبيق") },
                singleLine = true
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = showSystem, onCheckedChange = { showSystem = it })
                Text("إظهار تطبيقات النظام")
            }
            Button(onClick = onImportApk, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.FileOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("اختيار ملف APK")
            }
            if (importedApkName != null) {
                Text("تم اختيار الملف: $importedApkName", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Apps, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("التطبيقات (${filtered.size})", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filtered, key = { it.packageName }) { app ->
                    Card(onClick = { onAppClick(app) }, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Android, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(app.label, style = MaterialTheme.typography.titleSmall)
                                Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                                if (app.isSystem) Text("تطبيق نظام", style = MaterialTheme.typography.labelSmall)
                            }
                            Icon(Icons.Default.Info, contentDescription = "تفاصيل")
                        }
                    }
                }
            }
        }
    }
}