# BladeClient Presence API

Runs a tiny presence service for BladeClient.

## Endpoints
- `POST /presence/heartbeat`  
  Body: `{ "uuid": "...", "name": "...", "server": "mc.example.net" }`

- `GET /presence/list?server=mc.example.net`  
  Response: `[{ "uuid": "...", "name": "..." }, ...]`

- `GET /health`
- `GET /client/version`  
  Response: `{ "version": "...", "url": "..." }`

## Run
```bash
cd presence-server
npm install
npm start
```

Default port: `5000`

## Config (.env)
Create `presence-server/.env` (or copy from `.env.example`):
```
PORT=5000
CLIENT_VERSION=0.10.4-alpha
CLIENT_URL=http://example.com/BladeClient.jar
```
If `CLIENT_VERSION` or `CLIENT_URL` is set, `/client/version` will use those values.

## Notes
- Records expire automatically after 30s with cleanup every 15s.
- Only in-memory storage (no DB).

