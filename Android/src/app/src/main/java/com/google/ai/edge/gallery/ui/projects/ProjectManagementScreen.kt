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

package com.google.ai.edge.gallery.ui.projects

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.data.codingagent.Project
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectManagementScreen(
  viewModel: ProjectManagementViewModel,
  onProjectClick: (Project) -> Unit,
  onLogout: () -> Unit,
  onNavigateUp: () -> Unit
) {
  val projects by viewModel.projects.collectAsState()
  var showCreateDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Your Coding Projects") },
        actions = {
          IconButton(onClick = {
              viewModel.logoutGitHub()
              onLogout()
          }) {
            Icon(Icons.Default.Logout, contentDescription = "Logout GitHub")
          }
        }
      )
    },
    floatingActionButton = {
      FloatingActionButton(onClick = { showCreateDialog = true }) {
        Icon(Icons.Default.Add, contentDescription = "Create Project")
      }
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .padding(padding)
        .fillMaxSize()
        .padding(16.dp)
    ) {
      items(projects) { project ->
        ProjectCard(
          project = project,
          onClick = { onProjectClick(project) },
          onDelete = { viewModel.deleteProject(project) }
        )
        Spacer(modifier = Modifier.height(8.dp))
      }
    }
  }

  if (showCreateDialog) {
    CreateProjectDialog(
      onDismiss = { showCreateDialog = false },
      onCreate = { name, desc, repo, branch ->
        viewModel.createProject(name, desc, repo, branch,
          onSuccess = { showCreateDialog = false },
          onError = { /* Handle error, maybe show snackbar */ }
        )
      }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectCard(project: Project, onClick: () -> Unit, onDelete: () -> Unit) {
  val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
  Card(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(modifier = Modifier.fillMaxWidth()) {
        Text(project.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onDelete) {
          Icon(Icons.Default.Delete, contentDescription = "Delete Project")
        }
      }
      if (project.description.isNotEmpty()) {
        Text(project.description, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
      }
      Text("Repo: ${project.githubRepoUrl}", style = MaterialTheme.typography.bodySmall)
      Text("Branch: ${project.branch}", style = MaterialTheme.typography.bodySmall)
      Text("Last modified: ${dateFormat.format(Date(project.lastModified))}", style = MaterialTheme.typography.bodySmall)
    }
  }
}

@Composable
fun CreateProjectDialog(onDismiss: () -> Unit, onCreate: (String, String, String, String) -> Unit) {
  var name by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var repoUrl by remember { mutableStateOf("") }
  var branch by remember { mutableStateOf("main") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Create New Project") },
    text = {
      Column {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Project Name") })
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
        OutlinedTextField(value = repoUrl, onValueChange = { repoUrl = it }, label = { Text("GitHub Repo (owner/repo)") })
        OutlinedTextField(value = branch, onValueChange = { branch = it }, label = { Text("Branch") })
      }
    },
    confirmButton = {
      TextButton(onClick = { onCreate(name, description, repoUrl, branch) }, enabled = name.isNotBlank() && repoUrl.isNotBlank()) {
        Text("Create")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
