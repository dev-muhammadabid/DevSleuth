# AI Analysis Engine

## Pipeline

```
AnalysisInput
    |
    v
ReviewContextBuilder (extract relevant code, limit tokens)
    |
    v
AiInputSanitizer (redact secrets, neutralize injections, enforce limits)
    |
    v
AiPromptService (build system + user prompts)
    |
    v
LLM API call (OpenAI GPT-4o or Anthropic Claude)
    |
    v
AiResponseParser (extract JSON from response)
    |
    v
AiResponseValidator (validate enums, ranges, file paths)
    |
    v
List<RawFinding>
```

## Providers

- **OpenAI**: GPT-4o, `response_format: json_object`, temperature 0
- **Anthropic**: Claude, max_tokens 4096

Configured via `AI_PROVIDER` and `AI_API_KEY` env vars.

## Prompt Design

System prompt:
- Role: expert Java code reviewer
- Instructions: find bugs, security, performance, quality issues
- Exclusions: no style opinions, no unsupported claims, no duplicates
- Output format: strict JSON schema with findings array

User prompt:
- PR title + repository name
- Per-file: diff patch + surrounding code context

## Safety

- Max 200K chars total sent to LLM
- Max 30K chars per file
- Max 20 files
- Secrets redacted (12 regex patterns)
- Prompt injection patterns neutralized
- Invalid responses retried up to 2 times

## Retry Logic

- Timeout/5xx/rate-limit: retryable (exponential backoff)
- Auth failure/malformed request: not retryable
- After max retries: returns empty findings (review continues with static-only)
