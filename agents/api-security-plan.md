# API Security Plan

## Current State

- `POST https://api.uedo.net/llm` accepts **raw Gemini requests** — anyone can use it as a free LLM proxy
- **Zero authentication** — no API keys, no tokens, nothing
- The flyer extraction prompt is built **client-side** in `GeminiApiClientImpl.kt` and sent as-is
- The lambda just forwards to Gemini and returns the response

## Options for Securing the API

### 1. Move prompt logic server-side

Convert from a generic LLM proxy to a **purpose-built flyer processing endpoint**:

```
POST /flyer/process
Body: { "imageData": "<base64>", "mimeType": "image/jpeg" }
Response: { "name": "...", "venue": "...", "artists": [...] }
```

**Why this matters:** Even without any auth, this dramatically reduces abuse surface. An attacker gets a flyer-extraction API, not a general-purpose LLM. You also control the prompt, temperature, model, and max tokens server-side — no one can override them.

**Cost:** Moderate refactoring. Move the prompt from `GeminiApiClientImpl.kt` to the lambda, change the app to send just image + mimeType, parse the LLM response server-side, and return structured `ExtractedFlyerData` JSON.

### 2. API Gateway API Key (simple, not bulletproof)

Add an API Gateway usage plan + API key. Ship the key in the app binary.

- **Pros:** Trivial to set up, stops casual abuse, enables rate limiting per key
- **Cons:** Key can be extracted from the binary. It's security by obscurity.
- **Verdict:** Worth doing as a **layer**, but not sufficient alone.

### 3. AWS WAF on API Gateway

Attach WAF rules to your API Gateway:

- **Rate limiting** per IP (e.g., 10 requests/minute)
- **Geographic restrictions** if your user base is regional
- **Request body size limits** (you already compress to 50KB on the client)
- **Bot detection** rules

**Pros:** No app-side changes needed. Stops volumetric abuse.
**Cons:** Doesn't authenticate the app itself.

### 4. App Attestation (strongest without user accounts)

Use platform-native attestation to prove requests come from your real app:

| Platform | Service |
|----------|---------|
| iOS | [App Attest](https://developer.apple.com/documentation/devicecheck/establishing-your-app-s-integrity) (DeviceCheck framework) |
| Android | [Play Integrity API](https://developer.android.com/google/play/integrity) |

**Flow:**
1. App requests an attestation token from Apple/Google
2. App sends token with each API request (or on first launch to get a session)
3. Lambda authorizer verifies the token with Apple/Google servers
4. Only requests from verified app installs are allowed

**Pros:** Cannot be faked without a jailbroken/rooted device. No secrets in the binary.
**Cons:** Platform-specific implementation needed per platform. Desktop (JVM) has no equivalent — you'd need a fallback strategy. More complex to implement.

### 5. Device Registration + Short-Lived Tokens

A lighter-weight custom scheme:

1. On first launch, app generates a UUID device ID and registers with a `/register` endpoint
2. Backend issues a short-lived JWT (e.g., 1 hour)
3. App uses JWT for API calls, refreshes when expired
4. Backend tracks per-device rate limits

**Pros:** Works cross-platform including desktop. Enables per-device throttling.
**Cons:** Registration endpoint itself needs protection (chicken-and-egg), though you can combine with API key + rate limiting to make abuse impractical.

### 6. Amazon Cognito (future path)

Cognito supports **unauthenticated identities** — no user login required. Each device gets a unique Cognito identity and temporary AWS credentials.

**Pros:** AWS-native, integrates with API Gateway authorizers, per-identity rate limiting, easy upgrade path to authenticated users later.
**Cons:** Adds SDK dependency, slightly more complex setup.

## Recommended Approach (layered)

Combine these in layers, prioritized by effort vs. impact:

| Priority | Action | Effort | Impact |
|----------|--------|--------|--------|
| **1** | Move prompt server-side (purpose-built endpoint) | Medium | High — eliminates general LLM abuse entirely |
| **2** | API Gateway API key + usage plan with rate limits | Low | Medium — stops casual abuse, enables throttling |
| **3** | AWS WAF rate limiting per IP | Low | Medium — stops volumetric abuse |
| **4** | App Attestation (iOS/Android) or Cognito unauthenticated identities | High | High — cryptographic proof of app identity |

Layers 1-3 can be done quickly and get you to a reasonable security posture. Layer 4 is the "proper" solution for when/if you see real abuse.

**Recommendation:** Start with **1 + 2 + 3**. The purpose-built endpoint is the single highest-impact change — it makes the API fundamentally less valuable to attackers. The API key + WAF rate limiting on top makes opportunistic abuse impractical. Save app attestation or Cognito for later when you have a real need.
