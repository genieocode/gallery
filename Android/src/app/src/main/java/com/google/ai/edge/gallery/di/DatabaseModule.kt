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

package com.google.ai.edge.gallery.di

import com.google.ai.edge.gallery.data.codingagent.ProjectDao
import com.google.ai.edge.gallery.data.codingagent.AgentMemoryDao
import com.google.ai.edge.gallery.data.codingagent.MockProjectDao
import com.google.ai.edge.gallery.data.codingagent.MockAgentMemoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

  @Provides
  @Singleton
  fun provideProjectDao(): ProjectDao {
    return MockProjectDao()
  }

  @Provides
  @Singleton
  fun provideAgentMemoryDao(): AgentMemoryDao {
    return MockAgentMemoryDao()
  }
}
