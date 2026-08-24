# BladeClient

**Blade Client for modern PvP.**

BladeClient is a full ecosystem for Minecraft **1.21.8**: a Fabric mod client, an Electron launcher, and a presence/update server — all in one repository.

## Repository layout

| Folder | What it is | Tech |
|--------|------------|------|
| [`client/`](client) | The BladeClient Fabric mod (v0.17.3-alpha) | Java 21, Fabric, Gradle |
| [`launcher/`](launcher) | Custom Minecraft launcher with auto-updates | Electron, Vite, Vue |
| [`server/`](server) | Presence + version/update server | Node.js |

## Components

### 🎮 Client
Lightweight PvP-focused mod with custom HUD, click GUI (YACL + Owo-lib), freelook, zoom, chat enhancements and more.
```bash
cd client
./gradlew build        # jar lands in build/libs/
```

### 🚀 Launcher
```bash
cd launcher
npm install
npm start              # dev mode
```

### 🌐 Server
```bash
cd server
npm install
node server.js         # see .env.example for config
```

## Requirements

- **Client:** Minecraft 1.21.8, Fabric Loader >= 0.18.4, Java 21+
- **Launcher / Server:** Node.js LTS

## License

This project is licensed under the [BladeClient Non-Commercial License](LICENSE). Use, modification and forking are free — selling is prohibited.

---

Made by **IBladeShadow**
