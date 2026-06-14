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

package com.google.ai.edge.gallery.ui.github

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.DataStoreRepository
import com.google.ai.edge.gallery.network.GitHubService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GitHubLoginViewModel @Inject constructor(
  private val gitHubService: GitHubService,
  private val dataStoreRepository: DataStoreRepository
) : ViewModel() {

  private val _uiState = MutableStateFlow<GitHubLoginUiState>(GitHubLoginUiState.Idle)
  val uiState = _uiState.asStateFlow()

  fun loginWithToken(token: String) {
    viewModelScope.launch {
      _uiState.value = GitHubLoginUiState.Loading
      try {
        val response = gitHubService.getCurrentUser("token $token")
        if (response.isSuccessful) {
          dataStoreRepository.saveSecret("github_token", token)
          _uiState.value = GitHubLoginUiState.Success
        } else {
          _uiState.value = GitHubLoginUiState.Error("Invalid token: ${response.message()}")
        }
      } catch (e: Exception) {
        _uiState.value = GitHubLoginUiState.Error(e.message ?: "Unknown error")
      }
    }
  }

  fun resetState() {
    _uiState.value = GitHubLoginUiState.Idle
  }
}

sealed class GitHubLoginUiState {
  object Idle : GitHubLoginUiState()
  object Loading : GitHubLoginUiState()
  object Success : GitHubLoginUiState()
  data class Error(val message: String) : GitHubLoginUiState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitHubLoginScreen(
  viewModel: GitHubLoginViewModel,
  onLoginSuccess: () -> Unit,
  onNavigateUp: () -> Unit
) {
  var token by remember { mutableStateOf("") }
  val uiState by viewModel.uiState.collectAsState()

  if (uiState is GitHubLoginUiState.Success) {
    onLoginSuccess()
    viewModel.resetState()
  }

  Scaffold(
    topBar = {
      TopAppBar(title = { Text("GitHub Login") })
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .padding(padding)
        .fillMaxSize()
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        "To use the Agentic Coding feature, please provide a GitHub Personal Access Token (classic) with 'repo' scope.",
        style = MaterialTheme.typography.bodyMedium
      )
      Spacer(modifier = Modifier.height(16.dp))
      OutlinedTextField(
        value = token,
        onValueChange = { token = it },
        label = { Text("Personal Access Token") },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = PasswordVisualTransformation()
      )
      Spacer(modifier = Modifier.height(16.dp))
      if (uiState is GitHubLoginUiState.Error) {
        Text(
          (uiState as GitHubLoginUiState.Error).message,
          color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
      }
      Button(
        onClick = { viewModel.loginWithToken(token) },
        enabled = token.isNotBlank() && uiState !is GitHubLoginUiState.Loading,
        modifier = Modifier.fillMaxWidth()
      ) {
        if (uiState is GitHubLoginUiState.Loading) {
          CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
        }
        Text("Login")
      }

      Spacer(modifier = Modifier.height(24.dp))
      Text(
        "OAuth support coming soon. Please use a PAT for now.",
        style = MaterialTheme.typography.bodySmall
      )
    }
  }
}
