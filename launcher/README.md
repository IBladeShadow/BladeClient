# BladeClient Launcher

A minimal Vue + Electron launcher for running the dev client (`gradlew runClient`).

## Setup

```bash
cd launcher
npm install
```

## Dev

```bash
npm run dev
```

## Build renderer

```bash
npm run build
npm run start
```

## Notes
- This launcher downloads Minecraft assets + Fabric and runs the client directly (no Gradle).
- `clientZipUrl` can point to a `.jar` (mod) or a `.zip` (full client files).
- Config is stored at Electron userData (`bladeclient-launcher.json`).
