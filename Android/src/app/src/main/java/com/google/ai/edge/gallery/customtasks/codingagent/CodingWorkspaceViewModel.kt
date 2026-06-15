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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.DataStoreRepository
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.codingagent.AgentMemory
import com.google.ai.edge.gallery.data.codingagent.AgentMemoryDao
import com.google.ai.edge.gallery.data.codingagent.Project
import com.google.ai.edge.gallery.network.GitHubService
import com.google.ai.edge.gallery.network.GitHubTreeEntry
import com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper
import com.google.ai.edge.gallery.runtime.ResultListener
import com.google.ai.edge.litertlm.Contents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CodingWorkspaceViewModel @Inject constructor(
  private val agentMemoryDao: AgentMemoryDao,
  private val gitHubService: GitHubService,
  private val dataStoreRepository: DataStoreRepository
) : ViewModel() {

  private val _uiState = MutableStateFlow(CodingWorkspaceUiState())
  val uiState = _uiState.asStateFlow()

  fun initProject(project: Project) {
    _uiState.value = _uiState.value.copy(project = project)
    loadMemory(project.id)
    loadFiles(project)
  }

  private fun loadMemory(projectId: Long) {
    viewModelScope.launch {
      val memory = agentMemoryDao.getMemoryForProject(projectId)
      if (memory != null) {
        // Load history and plan
        _uiState.value = _uiState.value.copy(
          messages = emptyList(), // Need to deserialize
          plan = memory.planJson
        )
      }
    }
  }

  fun saveMemory() {
    val project = _uiState.value.project ?: return
    viewModelScope.launch {
      agentMemoryDao.insertMemory(
        AgentMemory(
          projectId = project.id,
          conversationJson = "", // Serialize messages
          planJson = _uiState.value.plan
        )
      )
    }
  }

  private fun loadFiles(project: Project) {
    viewModelScope.launch {
      val token = dataStoreRepository.readSecret("github_token") ?: return@launch
      val parts = project.githubRepoUrl.split("/")
      val response = gitHubService.getTree("token $token", parts[0], parts[1], project.branch)
      if (response.isSuccessful) {
        _uiState.value = _uiState.value.copy(fileTree = response.body()?.tree ?: emptyList())
      }
    }
  }

  fun sendMessage(model: Model, text: String) {
    val currentMessages = _uiState.value.messages.toMutableList()
    currentMessages.add(ChatMessage(text, true))
    _uiState.value = _uiState.value.copy(messages = currentMessages, isGenerating = true)

    viewModelScope.launch {
      LlmChatModelHelper.runInference(
        model = model,
        input = text,
        resultListener = { result, done, thought ->
          if (done) {
            _uiState.value = _uiState.value.copy(isGenerating = false)
            saveMemory()
          } else {
            val updatedMessages = _uiState.value.messages.toMutableList()
            if (updatedMessages.isNotEmpty() && !updatedMessages.last().isUser) {
                updatedMessages[updatedMessages.size - 1] = ChatMessage(updatedMessages.last().text + result, false)
            } else {
                updatedMessages.add(ChatMessage(result, false))
            }
            _uiState.value = _uiState.value.copy(messages = updatedMessages)

            thought?.let {
                _uiState.value = _uiState.value.copy(plan = it)
            }
          }
        },
        cleanUpListener = {},
        onError = { error ->
          _uiState.value = _uiState.value.copy(isGenerating = false)
          val updatedMessages = _uiState.value.messages.toMutableList()
          updatedMessages.add(ChatMessage("Error: $error", false))
          _uiState.value = _uiState.value.copy(messages = updatedMessages)
        }
      )
    }
  }

  fun requestWriteApproval(diff: DiffData) {
      _uiState.value = _uiState.value.copy(currentDiff = diff)
  }

  fun confirmWrite(diff: DiffData, codingTools: CodingTools) {
      viewModelScope.launch {
          val result = codingTools.write_file(diff.path, diff.newContent, diff.commitMessage)
          _uiState.value = _uiState.value.copy(currentDiff = null)
          // Result should be fed back to model...
      }
  }

  fun rejectWrite() {
      _uiState.value = _uiState.value.copy(currentDiff = null)
  }

  fun stopGeneration(model: Model) {
    LlmChatModelHelper.stopResponse(model)
    _uiState.value = _uiState.value.copy(isGenerating = false)
  }
}

data class CodingWorkspaceUiState(
  val project: Project? = null,
  val messages: List<ChatMessage> = emptyList(),
  val fileTree: List<GitHubTreeEntry> = emptyList(),
  val plan: String = "",
  val isGenerating: Boolean = false,
  val currentDiff: DiffData? = null
)

data class ChatMessage(val text: String, val isUser: Boolean)
data class DiffData(val path: String, val oldContent: String, val newContent: String, val commitMessage: String)
