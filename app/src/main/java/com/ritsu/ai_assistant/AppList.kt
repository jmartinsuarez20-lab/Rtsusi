package com.ritsu.ai_assistant

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

data class AppInfo(
    val label: CharSequence,
    val packageName: CharSequence,
    val icon: Drawable
)

@Composable
fun AppDrawer(apps: List<AppInfo>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 80.dp),
        modifier = modifier
    ) {
        items(apps) { app ->
            AppIcon(app = app, context = context)
        }
    }
}

@Composable
fun AppIcon(app: AppInfo, context: Context) {
    Column(
        modifier = Modifier
            .clickable {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName.toString())
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                }
            }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = app.icon),
            contentDescription = app.label.toString(),
            modifier = Modifier.size(50.dp)
        )
        Text(
            text = app.label.toString(),
            textAlign = TextAlign.Center
        )
    }
}
