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

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.runtime.Composable
import com.google.ai.edge.gallery.customtasks.common.CustomTask
import com.google.ai.edge.gallery.customtasks.common.CustomTaskData
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.Category
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.tool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

private const val CODING_AGENT_SYSTEM_PROMPT = """
You are an expert software engineering agent. You help the user with coding tasks in their GitHub repository.
You have access to tools to read files, write files, list directories, and create pull requests.

CRITICAL RULES:
1. ALWAYS start by proposing a step-by-step plan.
2. User MUST approve any file modifications (write_file) via the diff viewer.
3. Be concise and professional.
4. If a tool fails, analyze the error and propose an alternative.
5. Before editing, check if you are on the correct branch (avoid main).

Your available tools are:
- read_file(path: String)
- write_file(path: String, content: String, commit_message: String)
- list_directory(path: String)
- create_pull_request(title: String, body: String, head_branch: String, base_branch: String)
- run_command(command: String)
"""

class CodingAgentTask @Inject constructor(
  val codingTools: CodingTools
) : CustomTask {

  override val task = Task(
    id = BuiltInTaskId.LLM_CODING_AGENT,
    label = "Agentic Coding",
    description = "AI-assisted software development on-device.",
    shortDescription = "Expert coding assistant",
    category = Category.LLM,
    icon = Icons.Outlined.Code,
    models = mutableListOf(),
    handleModelConfigChangesInTask = true,
    defaultSystemPrompt = CODING_AGENT_SYSTEM_PROMPT
  )

  override fun initializeModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    systemInstruction: Contents?,
    onDone: (String) -> Unit
  ) {
    LlmChatModelHelper.initialize(
      context = context,
      model = model,
      taskId = task.id,
      supportImage = false,
      supportAudio = false,
      onDone = onDone,
      systemInstruction = systemInstruction ?: Contents.of(task.defaultSystemPrompt),
      tools = listOf(tool(codingTools)),
      enableConversationConstrainedDecoding = true
    )
  }

  override fun cleanUpModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: () -> Unit
  ) {
    LlmChatModelHelper.cleanUp(model = model, onDone = onDone)
  }

  @Composable
  override fun MainScreen(data: Any) {
    val customTaskData = data as CustomTaskData
    CodingWorkspaceScreen(
      task = task,
      modelManagerViewModel = customTaskData.modelManagerViewModel,
      bottomPadding = customTaskData.bottomPadding,
      setAppBarControlsDisabled = customTaskData.setAppBarControlsDisabled,
      setTopBarVisible = customTaskData.setTopBarVisible,
      setCustomNavigateUpCallback = customTaskData.setCustomNavigateUpCallback
    )
  }
}

@Module
@InstallIn(SingletonComponent::class)
object CodingAgentTaskModule {
  @Provides
  @IntoSet
  fun provideCodingAgentTask(codingTools: CodingTools): CustomTask {
    return CodingAgentTask(codingTools)
  }
}
