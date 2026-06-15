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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.network.GitHubTreeEntry
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodingWorkspaceScreen(
  task: Task,
  modelManagerViewModel: ModelManagerViewModel,
  bottomPadding: Dp,
  setAppBarControlsDisabled: (Boolean) -> Unit,
  setTopBarVisible: (Boolean) -> Unit,
  setCustomNavigateUpCallback: (() -> Unit) -> Unit,
  viewModel: CodingWorkspaceViewModel = hiltViewModel()
) {
  val uiState by viewModel.uiState.collectAsState()
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  var inputText by remember { mutableStateOf("") }

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      ModalDrawerSheet {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("File Tree", style = MaterialTheme.typography.titleLarge)
          Spacer(modifier = Modifier.height(8.dp))
          LazyColumn {
            items(uiState.fileTree) { entry ->
              FileTreeItem(entry)
            }
          }
        }
      }
    }
  ) {
    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text(uiState.project?.name ?: "Coding Workspace") },
          navigationIcon = {
            IconButton(onClick = { scope.launch { drawerState.open() } }) {
              Icon(Icons.Default.Menu, contentDescription = "Open File Tree")
            }
          }
        )
      }
    ) { padding ->
      Column(
        modifier = Modifier
          .padding(padding)
          .fillMaxSize()
          .padding(bottom = bottomPadding)
      ) {
        // Chat Area
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
          items(uiState.messages) { msg ->
            ChatBubble(msg)
          }
        }

        // Plan Panel (Collapsible)
        if (uiState.plan.isNotEmpty()) {
            PlanPanel(uiState.plan)
        }

        uiState.currentDiff?.let { diffData ->
            DiffViewerDialog(
                diffData = diffData,
                onConfirm = { /* viewModel.confirmWrite(diffData) */ },
                onDismiss = { /* viewModel.rejectWrite() */ }
            )
        }

        // Input Area
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Ask the coding agent...") }
          )
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(onClick = {
            // viewModel.sendMessage(...)
            inputText = ""
          }) {
            Icon(Icons.Default.Send, contentDescription = "Send")
          }
        }
      }
    }
  }
}

@Composable
fun FileTreeItem(entry: GitHubTreeEntry) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
    Icon(
      if (entry.type == "tree") Icons.Default.Folder else Icons.Default.Description,
      contentDescription = null,
      modifier = Modifier.padding(end = 8.dp)
    )
    Text(entry.path, style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable
fun ChatBubble(message: ChatMessage) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    contentAlignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
  ) {
    Card(
      modifier = Modifier.width(IntrinsicSize.Min).padding(horizontal = 8.dp)
    ) {
      Text(message.text, modifier = Modifier.padding(8.dp))
    }
  }
}

@Composable
fun PlanPanel(plan: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Current Plan", style = MaterialTheme.typography.titleSmall)
            Text(plan, style = MaterialTheme.typography.bodySmall)
        }
    }
}
