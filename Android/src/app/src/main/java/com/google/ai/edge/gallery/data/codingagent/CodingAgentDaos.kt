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

package com.google.ai.edge.gallery.data.codingagent

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface ProjectDao {
  fun getAllProjects(): Flow<List<Project>>
  suspend fun getProjectById(id: Long): Project?
  suspend fun insertProject(project: Project): Long
  suspend fun updateProject(project: Project)
  suspend fun deleteProject(project: Project)
}

interface AgentMemoryDao {
  suspend fun getMemoryForProject(projectId: Long): AgentMemory?
  suspend fun insertMemory(memory: AgentMemory)
  suspend fun deleteMemoryForProject(projectId: Long)
}
