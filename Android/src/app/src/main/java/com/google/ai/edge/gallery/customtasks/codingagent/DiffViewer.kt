/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.customtasks.codingagent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DiffViewerDialog(
  diffData: DiffData,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Approve Changes: ${diffData.path}") },
    text = {
      Column {
        Text("Commit Message: ${diffData.commitMessage}", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.height(300.dp).fillMaxWidth().background(Color.Black.copy(alpha = 0.05f))) {
            LazyColumn(modifier = Modifier.padding(8.dp)) {
                val lines = generateUnifiedDiff(diffData.oldContent, diffData.newContent)
                items(lines) { line ->
                    DiffLine(line)
                }
            }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onConfirm) { Text("Approve & Commit") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Reject") }
    }
  )
}

@Composable
fun DiffLine(line: String) {
    val backgroundColor = when {
        line.startsWith("+") -> Color.Green.copy(alpha = 0.1f)
        line.startsWith("-") -> Color.Red.copy(alpha = 0.1f)
        else -> Color.Transparent
    }
    val textColor = when {
        line.startsWith("+") -> Color(0xFF006400)
        line.startsWith("-") -> Color.Red
        else -> MaterialTheme.colorScheme.onSurface
    }
    Text(
        text = line,
        modifier = Modifier.fillMaxWidth().background(backgroundColor).padding(horizontal = 4.dp),
        color = textColor,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp
    )
}

fun generateUnifiedDiff(old: String, new: String): List<String> {
    val oldLines = old.lines()
    val newLines = new.lines()
    // Simple diff algorithm for demonstration
    val diff = mutableListOf<String>()
    // In a real implementation, use a library like java-diff-utils
    // This is a naive line-by-line comparison
    val maxLines = maxOf(oldLines.size, newLines.size)
    for (i in 0 until maxLines) {
        val oldLine = oldLines.getOrNull(i)
        val newLine = newLines.getOrNull(i)
        if (oldLine != newLine) {
            if (oldLine != null) diff.add("- $oldLine")
            if (newLine != null) diff.add("+ $newLine")
        } else {
            if (oldLine != null) diff.add("  $oldLine")
        }
    }
    return diff
}
