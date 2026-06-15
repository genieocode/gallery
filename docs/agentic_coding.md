# Agentic Coding Feature

The Agentic Coding feature extends the Google AI Edge Gallery with an on-device AI-assisted software development environment. It allows users to manage coding projects, interact with a local LLM agent to perform repository tasks, and integrate directly with GitHub.

## Setup Instructions

### GitHub Personal Access Token (PAT)
Currently, the app requires a classic Personal Access Token to interact with GitHub.
1. Go to [GitHub Settings > Developer settings > Personal access tokens > Tokens (classic)](https://github.com/settings/tokens).
2. Generate a new token with the `repo` scope.
3. Use this token to log in within the app.

### GitHub OAuth (Future Support)
To set up OAuth in the future:
1. Register a new OAuth App on GitHub.
2. Update `ProjectConfig.kt` with your `clientId` and `clientSecret` (if handled via a secure proxy).
3. Set the redirect URI to match the app's manifest scheme.

## Agent Loop and Tools
The agent uses a "Plan & Execute" loop:
1. User provides a task via chat.
2. Agent proposes a step-by-step plan.
3. Agent uses tools to gather information and propose changes.
4. User approves file modifications via a unified diff viewer.
5. Changes are committed directly to GitHub.

### Available Tools
- `read_file`: Fetches content of a specific file.
- `write_file`: Proposes changes to a file (requires user approval).
- `list_directory`: Lists files in a directory.
- `create_pull_request`: Creates a PR on GitHub.
- `run_command`: Mocked simulation of shell command execution.

## Persistence
- **Projects**: Stored in a local Room database.
- **Agent Memory**: Conversation history and current plan are saved per project in Room.
- **Tokens**: Stored securely in `EncryptedSharedPreferences` via `DataStoreRepository`.

## Model Switching
Users can switch local models (e.g., Gemma 2b vs 7b) at any time. The agent memory is preserved, allowing the new model to catch up and continue the task.
