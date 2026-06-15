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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.DataStoreRepository
import com.google.ai.edge.gallery.data.codingagent.Project
import com.google.ai.edge.gallery.data.codingagent.ProjectDao
import com.google.ai.edge.gallery.network.GitHubService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectManagementViewModel @Inject constructor(
  private val projectDao: ProjectDao,
  private val gitHubService: GitHubService,
  private val dataStoreRepository: DataStoreRepository
) : ViewModel() {

  val projects: StateFlow<List<Project>> = projectDao.getAllProjects()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun createProject(name: String, description: String, repoUrl: String, branch: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
    viewModelScope.launch {
      val token = dataStoreRepository.readSecret("github_token")
      if (token == null) {
        onError("GitHub token missing. Please log in again.")
        return@launch
      }

      try {
        val parts = repoUrl.split("/")
        if (parts.size != 2) {
          onError("Invalid repo URL format. Use owner/repo.")
          return@launch
        }
        val owner = parts[0]
        val repo = parts[1]

        val response = gitHubService.getRepo("token $token", owner, repo)
        if (response.isSuccessful) {
          val newProject = Project(
            name = name,
            description = description,
            githubRepoUrl = repoUrl,
            branch = branch
          )
          projectDao.insertProject(newProject)
          onSuccess()
        } else {
          onError("Repository not found or access denied: ${response.message()}")
        }
      } catch (e: Exception) {
        onError(e.message ?: "Unknown error")
      }
    }
  }

  fun deleteProject(project: Project) {
    viewModelScope.launch {
      projectDao.deleteProject(project)
    }
  }

  fun logoutGitHub() {
      dataStoreRepository.deleteSecret("github_token")
  }
}
