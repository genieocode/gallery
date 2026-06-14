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

package com.google.ai.edge.gallery.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubService {
  @GET("user")
  suspend fun getCurrentUser(
    @Header("Authorization") token: String
  ): Response<GitHubUser>

  @GET("repos/{owner}/{repo}")
  suspend fun getRepo(
    @Header("Authorization") token: String,
    @Path("owner") owner: String,
    @Path("repo") repo: String
  ): Response<GitHubRepo>

  @GET("repos/{owner}/{repo}/git/trees/{branch}")
  suspend fun getTree(
    @Header("Authorization") token: String,
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("branch") branch: String,
    @Query("recursive") recursive: Int = 1
  ): Response<GitHubTree>

  @GET("repos/{owner}/{repo}/contents/{path}")
  suspend fun getFileContent(
    @Header("Authorization") token: String,
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("path") path: String,
    @Query("ref") ref: String? = null
  ): Response<GitHubFileContent>

  @PUT("repos/{owner}/{repo}/contents/{path}")
  suspend fun updateFile(
    @Header("Authorization") token: String,
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("path") path: String,
    @Body request: GitHubUpdateFileRequest
  ): Response<GitHubUpdateFileResponse>

  @POST("repos/{owner}/{repo}/pulls")
  suspend fun createPullRequest(
    @Header("Authorization") token: String,
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Body request: GitHubCreatePullRequest
  ): Response<GitHubPullRequestResponse>

  @GET("repos/{owner}/{repo}/branches")
  suspend fun getBranches(
    @Header("Authorization") token: String,
    @Path("owner") owner: String,
    @Path("repo") repo: String
  ): Response<List<GitHubBranch>>
}

data class GitHubUser(val login: String, val id: Long)
data class GitHubRepo(val name: String, val full_name: String, val default_branch: String)
data class GitHubTree(val sha: String, val tree: List<GitHubTreeEntry>, val truncated: Boolean)
data class GitHubTreeEntry(val path: String, val mode: String, val type: String, val sha: String, val size: Long?)
data class GitHubFileContent(val name: String, val path: String, val sha: String, val size: Long, val content: String?, val encoding: String?)
data class GitHubUpdateFileRequest(val message: String, val content: String, val sha: String? = null, val branch: String? = null)
data class GitHubUpdateFileResponse(val content: GitHubFileContent?, val commit: GitHubCommit?)
data class GitHubCommit(val sha: String, val message: String)
data class GitHubCreatePullRequest(val title: String, val body: String, val head: String, val base: String)
data class GitHubPullRequestResponse(val id: Long, val number: Int, val html_url: String)
data class GitHubBranch(val name: String)
