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

import android.util.Base64
import android.util.Log
import com.google.ai.edge.gallery.data.DataStoreRepository
import com.google.ai.edge.gallery.network.GitHubCreatePullRequest
import com.google.ai.edge.gallery.network.GitHubService
import com.google.ai.edge.gallery.network.GitHubUpdateFileRequest
import com.google.ai.edge.gallery.data.codingagent.Project
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

private const val TAG = "AGCodingTools"

class CodingTools @Inject constructor(
  private val gitHubService: GitHubService,
  private val dataStoreRepository: DataStoreRepository
) : ToolSet {
  var currentProject: Project? = null

  @Tool(description = "Reads the content of a file from the current repository.")
  fun read_file(@ToolParam(description = "The path to the file.") path: String): String {
    val project = currentProject ?: return "Error: No project selected."
    val token = dataStoreRepository.readSecret("github_token") ?: return "Error: Not logged in to GitHub."
    val parts = project.githubRepoUrl.split("/")

    return runBlocking(Dispatchers.IO) {
      try {
        val response = gitHubService.getFileContent("token $token", parts[0], parts[1], path, project.branch)
        if (response.isSuccessful) {
          val fileContent = response.body()
          if (fileContent?.encoding == "base64" && fileContent.content != null) {
            String(Base64.decode(fileContent.content.replace("\n", ""), Base64.DEFAULT))
          } else {
            fileContent?.content ?: "Error: File content is empty."
          }
        } else {
          "Error: Failed to read file: ${response.message()}"
        }
      } catch (e: Exception) {
        "Error: ${e.message}"
      }
    }
  }

  @Tool(description = "Writes content to a file in the current repository. This creates a commit.")
  fun write_file(
    @ToolParam(description = "The path to the file.") path: String,
    @ToolParam(description = "The new content of the file.") content: String,
    @ToolParam(description = "The commit message.") commit_message: String
  ): String {
    val project = currentProject ?: return "Error: No project selected."
    val token = dataStoreRepository.readSecret("github_token") ?: return "Error: Not logged in to GitHub."
    val parts = project.githubRepoUrl.split("/")

    return runBlocking(Dispatchers.IO) {
      try {
        // First get the SHA of the file if it exists
        val getFileResponse = gitHubService.getFileContent("token $token", parts[0], parts[1], path, project.branch)
        val sha = if (getFileResponse.isSuccessful) getFileResponse.body()?.sha else null

        val base64Content = Base64.encodeToString(content.toByteArray(), Base64.NO_WRAP)
        val request = GitHubUpdateFileRequest(
          message = commit_message,
          content = base64Content,
          sha = sha,
          branch = project.branch
        )

        val response = gitHubService.updateFile("token $token", parts[0], parts[1], path, request)
        if (response.isSuccessful) {
          "Success: File updated. Commit SHA: ${response.body()?.commit?.sha}"
        } else {
          "Error: Failed to write file: ${response.message()}"
        }
      } catch (e: Exception) {
        "Error: ${e.message}"
      }
    }
  }

  @Tool(description = "Lists files and directories in a given path.")
  fun list_directory(@ToolParam(description = "The path to list.") path: String): String {
    val project = currentProject ?: return "Error: No project selected."
    val token = dataStoreRepository.readSecret("github_token") ?: return "Error: Not logged in to GitHub."
    val parts = project.githubRepoUrl.split("/")

    return runBlocking(Dispatchers.IO) {
      try {
        // GitHub's trees API is better for recursive listing, but for a single directory we can use contents API
        val response = gitHubService.getTree("token $token", parts[0], parts[1], project.branch)
        if (response.isSuccessful) {
          val tree = response.body()?.tree ?: emptyList()
          val filtered = tree.filter { it.path.startsWith(path) && it.path.removePrefix(path).removePrefix("/").split("/").size == 1 }
          filtered.joinToString("\n") { "${it.type}: ${it.path}" }
        } else {
          "Error: Failed to list directory: ${response.message()}"
        }
      } catch (e: Exception) {
        "Error: ${e.message}"
      }
    }
  }

  @Tool(description = "Creates a pull request.")
  fun create_pull_request(
    @ToolParam(description = "PR Title.") title: String,
    @ToolParam(description = "PR Body.") body: String,
    @ToolParam(description = "Head branch (source).") head_branch: String,
    @ToolParam(description = "Base branch (target).") base_branch: String
  ): String {
    val project = currentProject ?: return "Error: No project selected."
    val token = dataStoreRepository.readSecret("github_token") ?: return "Error: Not logged in to GitHub."
    val parts = project.githubRepoUrl.split("/")

    return runBlocking(Dispatchers.IO) {
      try {
        val request = GitHubCreatePullRequest(title, body, head_branch, base_branch)
        val response = gitHubService.createPullRequest("token $token", parts[0], parts[1], request)
        if (response.isSuccessful) {
          "Success: PR created at ${response.body()?.html_url}"
        } else {
          "Error: Failed to create PR: ${response.message()}"
        }
      } catch (e: Exception) {
        "Error: ${e.message}"
      }
    }
  }

  @Tool(description = "Runs a shell command (Simulated).")
  fun run_command(@ToolParam(description = "Command to run.") command: String): String {
    return "Simulation: Command '$command' executed successfully (mock output). Note: Real command execution is not supported in this environment yet."
  }
}
