package com.ritsu.ai_assistant

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

@Composable
fun AppDrawer(apps: List<AppInfo>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Card(modifier = modifier) {
        LazyColumn {
            items(apps) { app ->
                AppIcon(app = app, context = context)
            }
        }
    }
}

@Composable
fun AppIcon(app: AppInfo, context: Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                }
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = app.icon),
            contentDescription = app.name,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = app.name)
    }
}