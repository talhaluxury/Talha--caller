# TALHA CALLER ID backend

Zero-dependency Node.js (18+) service. It authenticates users, stores caller IDs, checks that a caller ID is verified and owned by the signed-in user before any call, rate-limits requests, logs security events, and keeps provider credentials off the phone.

## Run
```
export JWT_SECRET=$(openssl rand -hex 32)
export PROVIDER=mock        # test mode. Leave unset for "not configured".
npm start                   # listens on :8080
npm test
```

## Provider modes
| PROVIDER | Behaviour |
|---|---|
| unset | Every provider action returns 503 "Calling service is not configured yet." |
| `mock` | TEST MODE. Simulated verification (code `123456`), caller name approval and call progress. No real call is placed. Numbers ending `0000` simulate a failed call. |

## Adding a real provider
Create `src/providers/<name>.js` with the same methods as `MockProvider`
(`requestCallerIdVerification`, `verifyCallerId`, `setApprovedCallerName`, `getCallerNameStatus`, `makeCall`, `getCallStatus`, `endCall`, `getAccountBalance`) and register it in `src/providers/index.js`.
Do this only with the provider's real API documentation and credentials. Before going live confirm in writing: Pakistan mobile termination, authorized outbound caller ID rules, caller name support, pricing, and regulatory compliance (PTA). Put keys in environment variables on the server only.

## Security notes
- Passwords: scrypt with per-user salt. Tokens: HMAC-SHA256, 7-day expiry.
- Rate limits: general per IP, stricter on `/auth`, per-user on `/calls`.
- Security events go to `data/security.log` as JSON lines (numbers are masked).
- Call records keep only id, owner, time and a masked number.
- The JSON-file store is fine for development. Use a real database and HTTPS (reverse proxy) in production.
