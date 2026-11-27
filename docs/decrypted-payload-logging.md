# Decrypted request / response logging (authentication-service)

Optional, off by default. When on, the decrypted request and the plaintext response (before it is signed) of each API call are written to `ida.nira_decrypted_payload_log`, one row per call.

> Note: this service does not encrypt responses; it signs them (`keyManager.signResponse`). "Response before encryption" is therefore the plaintext JSON before signing.

## Configuration

| Property | Default | Meaning |
|---|---|---|
| `nira.logging.decrypted-payload.enabled` | `false` | Master switch. Read at startup; restart to change. |
| `nira.logging.decrypted-payload.masked-fields` | `bioValue,requestSessionKey,sessionKey,requestHMAC,otp,pin,password` | JSON field names (any depth, case-insensitive) replaced with `****`. |
| `nira.logging.decrypted-payload.partially-masked-fields` | `individualId` | Fields that keep only their last 4 characters (`****7890`). |
| `nira.logging.decrypted-payload.async.pool-size` | `2` | Writer threads. |
| `nira.logging.decrypted-payload.async.queue-capacity` | `1000` | Pending writes; when full, new records are dropped with a warning. |

Production / default:
```yaml
nira:
  logging:
    decrypted-payload:
      enabled: false
```
Troubleshooting environment:
```yaml
nira:
  logging:
    decrypted-payload:
      enabled: true
      masked-fields: bioValue,requestSessionKey,sessionKey,requestHMAC,otp,pin,password
      partially-masked-fields: individualId
```

## Database
- New installs: `db_scripts/mosip_ida/ddl/ida-nira_decrypted_payload_log.sql` (included from `ddl.sql`).
- Existing DBs: `db_upgrade_scripts/mosip_ida/sql/NIRA_to_NIRA-B1_upgrade.sql`; rollback `NIRA-B1_to_NIRA_rollback.sql`.
- There is no purge job. Delete old rows on a schedule (`DELETE ... WHERE created_date < now() - interval '7 days'`; `created_date` is indexed).

## Sample record
| column | value |
|---|---|
| id | `5c1b0f0e-3a54-4f0a-9d54-0d1b6f6a9a11` |
| endpoint | `/idauthentication/v1/auth/{partnerId}/{apiKey}` |
| http_method | `POST` |
| decrypted_request | `{"id":"mosip.identity.auth","version":"1.0","transactionID":"1234567890","individualId":"****7890","requestHMAC":"****","request":{"otp":"****","timestamp":"2026-11-26T15:00:00.000Z"}}` |
| decrypted_response | `{"id":"mosip.identity.auth","version":"1.0","transactionID":"1234567890","response":{"authStatus":true,"authToken":"..."},"errors":null}` |
| response_status | `200/SUCCESS` (also `200/FAILED`, `200/ERROR`, `200/UNKNOWN`) |
| error_message | `null` (e.g. `IDA-MPA-001: ...` for exceptions) |
| request_timestamp / response_timestamp | `2026-11-26 15:00:00.101` / `2026-11-26 15:00:00.437` |
| processing_time_ms | `336` |

If decryption fails, `decrypted_request` is `null` (the still-encrypted body is never stored).

## Security and performance
- The table holds personal data (names, dates of birth, etc. from demographics are not masked by default). Enable only where needed, restrict access to the table, and purge regularly. Add field names to `masked-fields` for anything else that must not be stored.
- Disabled: one boolean check per request; no extra serialization, decryption or DB access.
- Enabled: no extra decryption; the request string already built by the filter is reused. Masking and the INSERT run on a separate bounded thread pool, after the response is written. A full queue or a DB error drops the record and logs; authentication is not affected.
