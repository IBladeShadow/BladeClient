const { app, BrowserWindow, ipcMain, Menu, Tray, Notification } = require('electron');
const path = require('path');
const fs = require('fs');
const os = require('os');
const zlib = require('zlib');
const http = require('http');
const https = require('https');
const { pathToFileURL } = require('url');
const crypto = require('crypto');
const { spawnSync, spawn } = require('child_process');
const { getSessionJvmArg, getVerifyEndpoint, handleVerifyRequest } = require(path.join(__dirname, '..', 'antiCrack.cjs'));
const extract = require('extract-zip');
const archiver = require('archiver');
const { Client, Authenticator } = require('minecraft-launcher-core');
const MclcHandler = require('minecraft-launcher-core/components/handler');
let MSMCAuth = null;
let autoUpdater = null;
let DiscordRPC = null;
let path7za = '';
try {
  ({ Auth: MSMCAuth } = require('msmc'));
} catch {
  MSMCAuth = null;
}
try {
  ({ autoUpdater } = require('electron-updater'));
} catch {
  autoUpdater = null;
}

try {
  DiscordRPC = require('discord-rpc');
} catch {
  DiscordRPC = null;
}
try {
  ({ path7za } = require('7zip-bin'));
} catch {
  path7za = '';
}

const isDev = process.argv.includes('--dev');
const userData = app.getPath('userData');
const configPath = path.join(userData, 'bladeclient-launcher.json');
const DISCORD_CLIENT_ID = '1453725609531281419';
const ZULU_METADATA_URL = 'https://api.azul.com/metadata/v1/zulu/packages/?java_version=21&os=windows&arch=x86_64&archive_type=zip&java_package_type=jre&release_status=ga&javafx_bundled=false&latest=true';
const ADOPTIUM_FALLBACK_URL = 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse';

function readConfig() {
  const defaultClientUrl = 'https://fm-m1qk-blade.runflare.run/data/launcher/BladeClient.jar';
  const defaultPresenceUrl = 'https://blade.runflare.run';
  if (!fs.existsSync(configPath)) {
    return {
      gameDir: path.join(app.getPath('appData'), '.bladeclient'),
      javaPath: '',
      memoryMb: 4096,
      windowWidth: 1280,
      windowHeight: 720,
      launchFullscreen: false,
      javaArgs: '',
      minecraftVersion: '1.21.8',
      fabricLoader: '0.18.4',
      clientZipUrl: defaultClientUrl,
      assetsZipUrl: '',
      launcherUpdateUrl: 'https://fm-m1qk-blade.runflare.run/data/launcher/',
      presenceApiUrl: defaultPresenceUrl,
      clientVersion: '0.10.4-alpha',
      richPresenceEnabled: true,
      presenceHeartbeatSeconds: 15,
      presenceFetchSeconds: 10,
      authMode: 'offline',
      premiumAccounts: [],
      activePremiumUuid: '',
      offlineAccounts: [],
      activeOfflineName: '',
      bcAuthToken: '',
      bcAuthUser: '',
      bcPremiumName: '',
      bcPremiumUuid: '',
      bcAccounts: [],
      bcActiveUser: '',
      bcAccountsMigrated: false,
      themePreset: 'dark',
      onboardingDone: false,
      quickServers: [],
      selectedQuickServer: '',
      autoJoinQuickServer: false,
      postLaunchAction: 'tray',
      friends: []
    };
  }
  try {
    const cfg = JSON.parse(fs.readFileSync(configPath, 'utf8'));
    if (!cfg.clientZipUrl || cfg.clientZipUrl === 'http://modernshadow.ir/BladeClient.jar') {
      cfg.clientZipUrl = defaultClientUrl;
      writeConfig(cfg);
    }
    if (!cfg.presenceApiUrl) {
      cfg.presenceApiUrl = defaultPresenceUrl;
      writeConfig(cfg);
    }
    if (typeof cfg.launcherUpdateUrl !== 'string' || cfg.launcherUpdateUrl.trim().length === 0) {
      cfg.launcherUpdateUrl = 'https://fm-m1qk-blade.runflare.run/data/launcher/';
      writeConfig(cfg);
    }
    const NEW_HOST = 'https://fm-m1qk-blade.runflare.run/data/launcher/';
    if (typeof cfg.clientZipUrl === 'string' && !cfg.clientZipUrl.startsWith(NEW_HOST)) {
      cfg.clientZipUrl = NEW_HOST.replace(/\/+$/, '') + '/BladeClient.jar';
      writeConfig(cfg);
    }
    if (typeof cfg.launcherUpdateUrl === 'string' && !cfg.launcherUpdateUrl.startsWith(NEW_HOST)) {
      cfg.launcherUpdateUrl = NEW_HOST;
      writeConfig(cfg);
    }
    if (typeof cfg.presenceApiUrl === 'string' && cfg.presenceApiUrl.includes('5.57.37.248')) {
      cfg.presenceApiUrl = 'https://blade.runflare.run';
      writeConfig(cfg);
    }
    if (typeof cfg.presenceApiUrl === 'string' && /:\d+/.test(cfg.presenceApiUrl)) {
      cfg.presenceApiUrl = cfg.presenceApiUrl.replace(/:\d+/, '');
      writeConfig(cfg);
    }
    if (typeof cfg.presenceApiUrl === 'string' && cfg.presenceApiUrl.includes('fm-m1qk-')) {
      cfg.presenceApiUrl = 'https://blade.runflare.run';
      writeConfig(cfg);
    }
    if (typeof cfg.javaArgs !== 'string') {
      cfg.javaArgs = '';
      writeConfig(cfg);
    }
    if (!Number.isFinite(cfg.windowWidth)) {
      cfg.windowWidth = 1280;
      writeConfig(cfg);
    }
    if (!Number.isFinite(cfg.windowHeight)) {
      cfg.windowHeight = 720;
      writeConfig(cfg);
    }
    cfg.windowWidth = clampWindowWidth(cfg.windowWidth);
    cfg.windowHeight = clampWindowHeight(cfg.windowHeight);
    if (typeof cfg.launchFullscreen !== 'boolean') {
      cfg.launchFullscreen = false;
      writeConfig(cfg);
    }
    if (typeof cfg.richPresenceEnabled !== 'boolean') {
      cfg.richPresenceEnabled = true;
      writeConfig(cfg);
    }
    if (!Number.isFinite(cfg.presenceHeartbeatSeconds)) {
      cfg.presenceHeartbeatSeconds = 15;
      writeConfig(cfg);
    }
    if (!Number.isFinite(cfg.presenceFetchSeconds)) {
      cfg.presenceFetchSeconds = 10;
      writeConfig(cfg);
    }
    if (cfg.authMode !== 'premium' && cfg.authMode !== 'offline') {
      cfg.authMode = 'offline';
      writeConfig(cfg);
    }
    if (!Array.isArray(cfg.premiumAccounts)) {
      cfg.premiumAccounts = [];
      writeConfig(cfg);
    }
    if (typeof cfg.activePremiumUuid !== 'string') {
      cfg.activePremiumUuid = '';
      writeConfig(cfg);
    }
    if (!Array.isArray(cfg.offlineAccounts)) {
      cfg.offlineAccounts = [];
      writeConfig(cfg);
    }
    cfg.offlineAccounts = cfg.offlineAccounts
      .filter((v) => typeof v === 'string')
      .map((v) => v.trim())
      .filter((v) => v.length > 0);
    if (typeof cfg.activeOfflineName !== 'string') {
      cfg.activeOfflineName = '';
      writeConfig(cfg);
    }
    if (typeof cfg.bcAuthToken !== 'string') {
      cfg.bcAuthToken = '';
      writeConfig(cfg);
    }
    if (typeof cfg.bcAuthUser !== 'string') {
      cfg.bcAuthUser = '';
      writeConfig(cfg);
    }
    if (typeof cfg.bcPremiumName !== 'string') {
      cfg.bcPremiumName = '';
      writeConfig(cfg);
    }
    if (typeof cfg.bcPremiumUuid !== 'string') {
      cfg.bcPremiumUuid = '';
      writeConfig(cfg);
    }
    if (!Array.isArray(cfg.bcAccounts)) {
      cfg.bcAccounts = [];
      writeConfig(cfg);
    }
    cfg.bcAccounts = cfg.bcAccounts
      .filter((a) => a && typeof a.username === 'string')
      .map((a) => ({
        username: a.username.trim(),
        token: typeof a.token === 'string' ? a.token : '',
        premiumName: typeof a.premiumName === 'string' ? a.premiumName : '',
        premiumUuid: typeof a.premiumUuid === 'string' ? a.premiumUuid : ''
      }))
      .filter((a) => a.username.length > 0);
    if (typeof cfg.bcActiveUser !== 'string') {
      cfg.bcActiveUser = '';
      writeConfig(cfg);
    }
    if (!cfg.bcActiveUser && cfg.bcAccounts.length > 0) {
      cfg.bcActiveUser = cfg.bcAccounts[0].username;
      writeConfig(cfg);
    }
    if (typeof cfg.bcAccountsMigrated !== 'boolean') {
      cfg.bcAccountsMigrated = false;
      writeConfig(cfg);
    }
    if (!cfg.bcAccountsMigrated) {
      cfg.premiumAccounts = [];
      cfg.activePremiumUuid = '';
      cfg.offlineAccounts = [];
      cfg.activeOfflineName = '';
      cfg.authMode = 'offline';
      if (cfg.bcAuthUser && cfg.bcAuthToken) {
        cfg.bcAccounts = [{
          username: cfg.bcAuthUser,
          token: cfg.bcAuthToken,
          premiumName: cfg.bcPremiumName || '',
          premiumUuid: cfg.bcPremiumUuid || ''
        }];
        cfg.bcActiveUser = cfg.bcAuthUser;
      }
      cfg.bcAccountsMigrated = true;
      writeConfig(cfg);
    }
    if (typeof cfg.themePreset !== 'string' || cfg.themePreset.trim().length === 0) {
      cfg.themePreset = 'dark';
      writeConfig(cfg);
    }
    if (typeof cfg.onboardingDone !== 'boolean') {
      cfg.onboardingDone = false;
      writeConfig(cfg);
    }
    if (!Array.isArray(cfg.quickServers)) {
      cfg.quickServers = [];
      writeConfig(cfg);
    }
    cfg.quickServers = cfg.quickServers
      .map((s) => ({
        name: typeof s?.name === 'string' ? s.name.trim().slice(0, 32) : '',
        address: typeof s?.address === 'string' ? s.address.trim().slice(0, 128) : '',
        port: Number.isFinite(s?.port) ? s.port : 25565
      }))
      .filter((s) => s.name && s.address)
      .map((s) => ({ ...s, port: Math.max(1, Math.min(65535, Math.floor(s.port))) }));
    if (typeof cfg.selectedQuickServer !== 'string') {
      cfg.selectedQuickServer = '';
      writeConfig(cfg);
    }
    if (typeof cfg.autoJoinQuickServer !== 'boolean') {
      cfg.autoJoinQuickServer = false;
      writeConfig(cfg);
    }
    if (!Array.isArray(cfg.friends)) {
      cfg.friends = [];
      writeConfig(cfg);
    }
    const rawFriends = JSON.stringify(cfg.friends || []);
    const cleanedFriends = (cfg.friends || [])
      .map((f) => ({
        name: typeof f?.name === 'string' ? f.name.trim().slice(0, 32) : '',
        server: typeof f?.server === 'string' ? f.server.trim().slice(0, 128) : ''
      }))
      .filter((f) => f.name.length > 0);
    cfg.friends = cleanedFriends;
    if (JSON.stringify(cleanedFriends) !== rawFriends) {
      writeConfig(cfg);
    }
    if (!['none', 'tray', 'close'].includes(cfg.postLaunchAction)) {
      cfg.postLaunchAction = 'tray';
      writeConfig(cfg);
    }
    if (cfg.offlineAccounts.length === 0) cfg.activeOfflineName = '';
    if (cfg.offlineAccounts.length > 0 && !cfg.offlineAccounts.includes(cfg.activeOfflineName)) {
      cfg.activeOfflineName = cfg.offlineAccounts[0];
      writeConfig(cfg);
    }
    return cfg;
  } catch (err) {
    return {
      gameDir: path.join(app.getPath('appData'), '.bladeclient'),
      javaPath: '',
      memoryMb: 4096,
      windowWidth: 1280,
      windowHeight: 720,
      launchFullscreen: false,
      javaArgs: '',
      minecraftVersion: '1.21.8',
      fabricLoader: '0.18.4',
      clientZipUrl: defaultClientUrl,
      assetsZipUrl: '',
      launcherUpdateUrl: 'https://fm-m1qk-blade.runflare.run/data/launcher/',
      presenceApiUrl: defaultPresenceUrl,
      clientVersion: '0.10.5-alpha',
      richPresenceEnabled: true,
      presenceHeartbeatSeconds: 15,
      presenceFetchSeconds: 10,
      authMode: 'offline',
      premiumAccounts: [],
      activePremiumUuid: '',
      offlineAccounts: [],
      activeOfflineName: '',
      bcAuthToken: '',
      bcAuthUser: '',
      bcPremiumName: '',
      bcPremiumUuid: '',
      bcAccountsMigrated: true,
      themePreset: 'dark',
      onboardingDone: false,
      quickServers: [],
      selectedQuickServer: '',
      autoJoinQuickServer: false,
      postLaunchAction: 'tray',
      friends: []
    };
  }
}

function writeConfig(cfg) {
  fs.writeFileSync(configPath, JSON.stringify(cfg, null, 2), 'utf8');
}

function getMaxAllocatableMemoryMb() {
  const totalMb = Math.floor(os.totalmem() / (1024 * 1024));
  // Half of system RAM, with sane bounds for Minecraft.
  return Math.max(2048, Math.min(65536, Math.floor(totalMb / 2)));
}

function clampMemoryMb(value) {
  const num = Number.isFinite(value) ? value : Number(value);
  const max = getMaxAllocatableMemoryMb();
  if (!Number.isFinite(num)) return Math.min(4096, max);
  const stepped = Math.round(num / 256) * 256;
  return Math.max(2048, Math.min(max, stepped));
}

function clampWindowWidth(value) {
  const num = Number.isFinite(value) ? value : Number(value);
  if (!Number.isFinite(num)) return 1280;
  return Math.max(640, Math.min(7680, Math.floor(num)));
}

function clampWindowHeight(value) {
  const num = Number.isFinite(value) ? value : Number(value);
  if (!Number.isFinite(num)) return 720;
  return Math.max(360, Math.min(4320, Math.floor(num)));
}

let mainWindow = null;
let splashWindow = null;
let tray = null;
let isAppQuitting = false;
let rendererReady = false;
let updateFlowReady = false;
let lastBackgroundNoticeAt = 0;
let launching = false;
let progressState = { percent: 0, label: '' };
let lastDownloadName = '';
let currentProc = null;
let cancelRequested = false;
let downloadSpeedBps = 0;
let downloadSpeedBytes = 0;
let downloadSpeedLastAt = 0;
let currentDownloadBytes = 0;
let currentDownloadTotal = 0;
const ASSETS_ZIP_NAME = 'assets-cache.zip';
const MODS_MARKER = '.bladeclient-mods.json';
let assetsPatchApplied = false;
const activeDownloads = new Set();
const activeRequests = new Set();
const activeDownloadResponses = new Set();
let downloadsPaused = false;
let forceKillPending = false;
let updateCheckStarted = false;
let updateInstallTriggered = false;
let rpcClient = null;
let rpcReady = false;
let rpcStateSignature = '';
let rpcStartTimestamp = 0;
let rpcPollTimer = null;
let launcherStateTimer = null;
let launcherPresenceStatus = 'launcher';
let rpcLastStatus = '';
let rpcKeepAliveTimer = null;
let localPresenceServer = null;
let localPresenceBaseUrl = '';
let localLauncherState = { source: 'launcher', status: 'launcher', ts: 0 };
let localClientState = {
  source: 'client',
  status: 'client',
  ts: 0,
  serverName: '',
  serverAddress: '',
  serverIconUrl: '',
  iconBuffer: null,
  iconMime: 'image/png',
  iconUpdatedAt: 0
};
const LOCAL_PRESENCE_TTL_MS = 30 * 1000;
const LOCAL_PRESENCE_HOST = '127.0.0.1';
const LOCAL_PRESENCE_PORT = 57575;
let localStateLogSignature = '';

function sendLog(msg) {
  mainWindow?.webContents.send('launcher:log', msg);
  if (isDev) {
    try { console.log(msg.trimEnd()); } catch {}
  }
}

function throwIfCancelled() {
  if (cancelRequested) {
    throw new Error('Launch cancelled');
  }
}

function cancelActiveNetwork() {
  for (const entry of activeDownloads) {
    try { entry.req?.destroy(); } catch {}
    try { entry.file?.destroy(); } catch {}
    if (entry.dest && fs.existsSync(entry.dest)) {
      try { fs.unlinkSync(entry.dest); } catch {}
    }
  }
  activeDownloads.clear();
  for (const req of activeRequests) {
    try { req.destroy(); } catch {}
  }
  activeRequests.clear();
  activeDownloadResponses.clear();
  downloadsPaused = false;
  downloadSpeedBps = 0;
  downloadSpeedBytes = 0;
  downloadSpeedLastAt = 0;
  currentDownloadBytes = 0;
  currentDownloadTotal = 0;
}

function sendState(state) {
  mainWindow?.webContents.send('launcher:state', state);
}

function sendUpdate(payload) {
  mainWindow?.webContents.send('launcher:update', payload);
  updateSplash(payload);
  if (payload?.status === 'ready') {
    updateFlowReady = true;
    tryShowMainWindow();
  }
}

function killProcessTree(proc) {
  if (!proc || !proc.pid) return;
  try {
    if (process.platform === 'win32') {
      spawn('taskkill', ['/PID', String(proc.pid), '/T', '/F'], { detached: true, stdio: 'ignore' }).unref();
    } else {
      try { process.kill(-proc.pid, 'SIGKILL'); } catch {}
      try { proc.kill('SIGKILL'); } catch {}
    }
  } catch {}
}

function createSplashWindow() {
  splashWindow = new BrowserWindow({
    width: 520,
    height: 300,
    frame: false,
    resizable: false,
    transparent: false,
    alwaysOnTop: true,
    show: true,
    backgroundColor: '#0b0d12',
    webPreferences: {
      contextIsolation: true,
      sandbox: true
    }
  });
  const html = `
<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <style>
    html,body{margin:0;padding:0;background:#0b0d12;color:#e8edf5;font-family:Segoe UI,Arial,sans-serif;}
    .wrap{height:100vh;display:flex;align-items:center;justify-content:center;background:radial-gradient(circle at 20% 0%, rgba(74,163,255,.16), transparent 44%),#0b0d12;}
    .card{width:380px;background:rgba(13,17,26,.92);border:1px solid rgba(255,255,255,.08);border-radius:16px;padding:22px;box-shadow:0 24px 64px rgba(0,0,0,.45);}
    .title{font-size:16px;font-weight:600;margin-bottom:8px}
    .label{font-size:12px;opacity:.85;margin-bottom:10px}
    .percent{font-size:12px;opacity:.92;margin-bottom:8px;min-height:14px}
    .track{height:8px;background:rgba(255,255,255,.12);border-radius:999px;overflow:hidden}
    .fill{height:100%;background:linear-gradient(90deg,#4aa3ff,#7cc0ff);border-radius:999px;width:0%}
    .ind{width:35%;animation:ind 1.1s ease-in-out infinite}
    @keyframes ind{0%{transform:translateX(-120%)}100%{transform:translateX(310%)}}
  </style>
</head>
<body>
  <div class="wrap">
    <div class="card">
      <div class="title">BladeClient Launcher</div>
      <div id="label" class="label">Checking for launcher updates...</div>
      <div id="percent" class="percent"></div>
      <div class="track"><div id="fill" class="fill ind"></div></div>
    </div>
  </div>
</body>
</html>`;
  splashWindow.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(html)}`);
}

function updateSplash(payload) {
  if (!splashWindow || splashWindow.isDestroyed()) return;
  const status = payload?.status || 'checking';
  const label = payload?.label || (
    status === 'checking' ? 'Checking for launcher updates...' :
    status === 'downloading' ? 'Downloading update...' :
    status === 'installing' ? 'Installing update...' :
    'Preparing launcher...'
  );
  const hasPercent = status === 'downloading' && Number.isFinite(payload?.percent);
  const percent = hasPercent ? Math.max(0, Math.min(100, Math.floor(payload.percent))) : 0;
  const js = `
    (function(){
      var labelEl=document.getElementById('label');
      var percentEl=document.getElementById('percent');
      var fillEl=document.getElementById('fill');
      if(labelEl) labelEl.textContent=${JSON.stringify(label)};
      if(!fillEl) return;

      function applyWidth(v){
        fillEl.style.width = Math.max(0, Math.min(100, v)) + '%';
        if(percentEl) percentEl.textContent = Math.round(v) + '%';
      }

      if(${hasPercent ? 'true' : 'false'}){
        fillEl.classList.remove('ind');
        window.__bcTarget = ${percent};
        if(typeof window.__bcCurrent !== 'number'){
          window.__bcCurrent = ${percent};
        }
        if(window.__bcCurrent > window.__bcTarget){
          window.__bcCurrent = window.__bcTarget;
        }
        if(window.__bcAnim) return;
        window.__bcAnim = true;
        (function tick(){
          var target = window.__bcTarget;
          var current = window.__bcCurrent;
          var next = current + (target - current) * 0.22;
          if(Math.abs(target - next) < 0.12) next = target;
          window.__bcCurrent = next;
          applyWidth(next);
          if(next !== target){
            requestAnimationFrame(tick);
          } else {
            window.__bcAnim = false;
          }
        })();
      } else {
        window.__bcAnim = false;
        window.__bcCurrent = 0;
        window.__bcTarget = 0;
        if(percentEl) percentEl.textContent = '';
        fillEl.classList.add('ind');
        fillEl.style.width = '35%';
      }
    })();
  `;
  splashWindow.webContents.executeJavaScript(js).catch(() => {});
}

function tryShowMainWindow() {
  if (!rendererReady || !updateFlowReady || !mainWindow || mainWindow.isDestroyed()) return;
  if (splashWindow && !splashWindow.isDestroyed()) {
    splashWindow.close();
    splashWindow = null;
  }
  mainWindow.show();
  mainWindow.focus();
}

function getTrayIconPath() {
  return app.isPackaged
    ? path.join(process.resourcesPath, 'icon.png')
    : path.join(__dirname, '../public/icon.png');
}

function ensureTray() {
  if (tray) return tray;
  tray = new Tray(getTrayIconPath());
  tray.setToolTip('BladeClient Launcher');
  tray.setContextMenu(Menu.buildFromTemplate([
    {
      label: 'Open Launcher',
      click: () => {
        if (mainWindow) {
          mainWindow.show();
          mainWindow.focus();
        }
      }
    },
    { label: 'Exit', click: () => app.quit() }
  ]));
  tray.on('double-click', () => {
    if (mainWindow) {
      mainWindow.show();
      mainWindow.focus();
    }
  });
  return tray;
}

function notifyBackgroundRunning() {
  const now = Date.now();
  if (now - lastBackgroundNoticeAt < 4000) return;
  lastBackgroundNoticeAt = now;
  const title = 'BladeClient Launcher';
  const body = 'The program is still running in the background.';
  try {
    if (tray && process.platform === 'win32' && typeof tray.displayBalloon === 'function') {
      tray.displayBalloon({
        icon: getTrayIconPath(),
        title,
        content: body
      });
      return;
    }
  } catch {}
  try {
    if (Notification.isSupported()) {
      new Notification({ title, body, silent: true }).show();
    }
  } catch {}
}

function applyPostLaunchAction(cfg) {
  const action = cfg?.postLaunchAction || 'none';
  if (action === 'tray') {
    ensureTray();
    if (mainWindow) mainWindow.hide();
    sendLog('[Launcher] Launcher moved to tray.\n');
    notifyBackgroundRunning();
    return;
  }
  if (action === 'close') {
    sendLog('[Launcher] Launcher closing after game start.\n');
    setTimeout(() => app.quit(), 250);
  }
}

function setDownloadsPaused(paused) {
  downloadsPaused = !!paused;
  for (const res of activeDownloadResponses) {
    try {
      if (downloadsPaused) res.pause();
      else res.resume();
    } catch {}
  }
  mainWindow?.webContents.send('launcher:downloads-state', { paused: downloadsPaused });
}

function sendProgress(percent, label, speedBps) {
  progressState = {
    percent,
    label,
    speedBps: typeof speedBps === 'number' ? speedBps : downloadSpeedBps,
    downloadedBytes: currentDownloadBytes,
    totalBytes: currentDownloadTotal
  };
  mainWindow?.webContents.send('launcher:progress', progressState);
}

function updateDownloadSpeed(bytes) {
  if (!Number.isFinite(bytes) || bytes <= 0) return;
  const now = Date.now();
  if (!downloadSpeedLastAt) {
    downloadSpeedLastAt = now;
    downloadSpeedBytes = 0;
  }
  downloadSpeedBytes += bytes;
  if (now - downloadSpeedLastAt >= 500) {
    const elapsedSec = (now - downloadSpeedLastAt) / 1000;
    downloadSpeedBps = Math.max(0, Math.floor(downloadSpeedBytes / Math.max(elapsedSec, 0.001)));
    downloadSpeedBytes = 0;
    downloadSpeedLastAt = now;
    sendProgress(progressState.percent, progressState.label, downloadSpeedBps);
  }
}

function logMojangSources() {
  sendLog('[Launcher] Mojang download sources:\n');
  sendLog('  - Version manifest: https://launchermeta.mojang.com/mc/game/version_manifest.json\n');
  sendLog('  - Version jar base: https://piston-data.mojang.com/v1/objects/\n');
  sendLog('  - Libraries base: https://libraries.minecraft.net/\n');
  sendLog('  - Assets base: https://resources.download.minecraft.net/\n');
}

function createWindow() {
  const windowIcon = app.isPackaged
    ? path.join(process.resourcesPath, 'icon.png')
    : path.join(__dirname, '../public/icon.png');
  Menu.setApplicationMenu(null);
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 720,
    resizable: false,
    maximizable: false,
    fullscreenable: false,
    show: false,
    titleBarStyle: 'hidden',
    titleBarOverlay: {
      color: '#0b0d12',
      symbolColor: '#e8edf5',
      height: 34
    },
    backgroundColor: '#0b0d12',
    icon: windowIcon,
    webPreferences: {
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js'),
      devTools: isDev
    }
  });

  if (isDev) {
    mainWindow.loadURL('http://localhost:5173');
    mainWindow.webContents.openDevTools({ mode: 'detach' });
  } else {
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'));
    mainWindow.webContents.on('before-input-event', (event, input) => {
      const key = String(input.key || '').toUpperCase();
      const ctrlOrCmd = !!(input.control || input.meta);
      const shift = !!input.shift;
      const blocked =
        key === 'F12' ||
        (ctrlOrCmd && shift && (key === 'I' || key === 'J' || key === 'C'));
      if (blocked) event.preventDefault();
    });
  }

  mainWindow.webContents.on('did-finish-load', () => {
    (async () => {
      const cfg = readConfig();
      await fetchLauncherVersionFromApi(cfg);
      checkLauncherUpdate();
    })();
  });

  mainWindow.on('close', (event) => {
    const cfg = readConfig();
    if (!isAppQuitting && cfg.postLaunchAction === 'tray') {
      ensureTray();
      event.preventDefault();
      mainWindow.hide();
      sendLog('[Launcher] Launcher minimized to tray.\n');
      notifyBackgroundRunning();
    }
  });
}

function existsJava(javaPath) {
  try {
    const bin = javaPath && javaPath.trim().length > 0 ? javaPath : 'java';
    const res = spawnSync(bin, ['-version'], { stdio: 'pipe' });
    return res.status === 0 || res.stderr.toString().includes('version');
  } catch (err) {
    return false;
  }
}

function getJavaMajorVersion(javaPath) {
  try {
    const bin = javaPath && javaPath.trim().length > 0 ? javaPath : 'java';
    const res = spawnSync(bin, ['-version'], { stdio: 'pipe', encoding: 'utf8' });
    const output = `${res.stderr || ''}\n${res.stdout || ''}`;
    const line = output.split(/\r?\n/).find((v) => v.toLowerCase().includes('version')) || '';
    const match = line.match(/version\s+"([^"]+)"/i);
    if (!match) return 0;
    const ver = match[1];
    if (ver.startsWith('1.')) {
      const legacy = Number(ver.split('.')[1]);
      return Number.isFinite(legacy) ? legacy : 0;
    }
    const major = Number(ver.split('.')[0]);
    return Number.isFinite(major) ? major : 0;
  } catch {
    return 0;
  }
}

function isJavaCompatible(javaPath, minMajor = 21) {
  return getJavaMajorVersion(javaPath) >= minMajor;
}

function getJavaStatus() {
  const cfg = readConfig();
  const configured = cfg.javaPath && cfg.javaPath.trim().length > 0 ? cfg.javaPath.trim() : '';
  const bundledRoot = path.join(userData, 'jre');
  const bundledPath = findJavaBinInDir(bundledRoot);
  const systemPath = existsJava('java') ? 'java' : '';
  const configuredVersion = configured && existsJava(configured) ? getJavaMajorVersion(configured) : 0;
  const bundledVersion = bundledPath && existsJava(bundledPath) ? getJavaMajorVersion(bundledPath) : 0;
  const systemVersion = systemPath ? getJavaMajorVersion(systemPath) : 0;
  return {
    configuredPath: configured || '',
    configuredVersion,
    systemPath,
    systemVersion,
    bundledPath,
    bundledVersion,
    requiredMajor: 21
  };
}

function toJavaChoice(pathValue, major, source, recommended = false) {
  const safePath = String(pathValue || '').trim();
  const versionLabel = major > 0 ? `Java ${major}` : 'Unknown';
  let label = `${versionLabel} - ${safePath}`;
  if (source === 'bundled') label = `${versionLabel} - Launcher Recommended`;
  if (source === 'system') label = `${versionLabel} - System PATH`;
  return {
    id: `${source}:${safePath}`,
    path: safePath,
    source,
    major: major || 0,
    recommended: !!recommended,
    label
  };
}

function findJavaBinsInRoot(rootDir, maxDepth = 2, maxCount = 40) {
  const results = [];
  if (!rootDir || !fs.existsSync(rootDir)) return results;
  const exeName = process.platform === 'win32' ? 'java.exe' : 'java';
  const queue = [{ dir: rootDir, depth: 0 }];
  while (queue.length > 0 && results.length < maxCount) {
    const current = queue.shift();
    let entries = [];
    try {
      entries = fs.readdirSync(current.dir, { withFileTypes: true });
    } catch {
      continue;
    }
    for (const entry of entries) {
      if (!entry.isDirectory()) continue;
      const next = path.join(current.dir, entry.name);
      const bin = path.join(next, 'bin', exeName);
      if (fs.existsSync(bin)) {
        results.push(bin);
        if (results.length >= maxCount) break;
      }
      if (current.depth + 1 < maxDepth) queue.push({ dir: next, depth: current.depth + 1 });
    }
  }
  return results;
}

function listJavaChoices() {
  const cfg = readConfig();
  const choices = [];
  const seen = new Set();
  const push = (pathValue, source, recommended = false) => {
    const p = String(pathValue || '').trim();
    if (!p || seen.has(p)) return;
    if (!existsJava(p)) return;
    seen.add(p);
    const major = getJavaMajorVersion(p);
    choices.push(toJavaChoice(p, major, source, recommended));
  };

  const configured = cfg.javaPath && cfg.javaPath.trim().length > 0 ? cfg.javaPath.trim() : '';
  if (configured) push(configured, 'configured', false);

  if (existsJava('java')) push('java', 'system', false);

  const bundled = findJavaBinInDir(path.join(userData, 'jre'));
  if (bundled) push(bundled, 'bundled', true);

  const envJavaHome = process.env.JAVA_HOME || '';
  if (envJavaHome) {
    const exeName = process.platform === 'win32' ? 'java.exe' : 'java';
    push(path.join(envJavaHome, 'bin', exeName), 'env', false);
  }

  if (process.platform === 'win32') {
    const roots = [
      process.env['ProgramFiles'],
      process.env['ProgramFiles(x86)'],
      process.env['LOCALAPPDATA']
    ].filter(Boolean);
    const knownJavaDirs = [
      ['Java'],
      ['Eclipse Adoptium'],
      ['Microsoft'],
      ['Zulu'],
      ['BellSoft'],
      ['Amazon Corretto']
    ];
    for (const root of roots) {
      for (const parts of knownJavaDirs) {
        const dir = path.join(root, ...parts);
        const found = findJavaBinsInRoot(dir, 2, 20);
        for (const f of found) push(f, 'installed', false);
      }
    }
  }

  // recommended first, then compatible version, then major desc.
  choices.sort((a, b) => {
    if (a.recommended !== b.recommended) return a.recommended ? -1 : 1;
    const aCompat = a.major >= 21 ? 1 : 0;
    const bCompat = b.major >= 21 ? 1 : 0;
    if (aCompat !== bCompat) return bCompat - aCompat;
    if (a.major !== b.major) return b.major - a.major;
    return a.label.localeCompare(b.label);
  });

  return choices;
}

function findJavaBinInDir(rootDir) {
  if (!rootDir || !fs.existsSync(rootDir)) return '';
  const exeName = process.platform === 'win32' ? 'java.exe' : 'java';
  const direct = path.join(rootDir, 'bin', exeName);
  if (fs.existsSync(direct)) return direct;
  const stack = [rootDir];
  while (stack.length > 0) {
    const dir = stack.pop();
    let entries = [];
    try {
      entries = fs.readdirSync(dir, { withFileTypes: true });
    } catch {
      continue;
    }
    for (const entry of entries) {
      if (!entry.isDirectory()) continue;
      const next = path.join(dir, entry.name);
      const candidate = path.join(next, 'bin', exeName);
      if (fs.existsSync(candidate)) return candidate;
      stack.push(next);
    }
  }
  return '';
}

function resolveJavaForLaunch(javaPath) {
  if (!javaPath || typeof javaPath !== 'string') return 'javaw';
  const trimmed = javaPath.trim();
  if (process.platform !== 'win32') return trimmed;
  if (/javaw\.exe$/i.test(trimmed)) return trimmed;
  if (/java\.exe$/i.test(trimmed)) {
    const javaw = trimmed.replace(/java\.exe$/i, 'javaw.exe');
    if (fs.existsSync(javaw)) return javaw;
  }
  return trimmed;
}

function resolveRedirect(baseUrl, location) {
  try {
    return new URL(location, baseUrl).toString();
  } catch {
    return location;
  }
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function withTimeout(promise, ms) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('timeout')), ms);
    promise.then((val) => {
      clearTimeout(timer);
      resolve(val);
    }).catch((err) => {
      clearTimeout(timer);
      reject(err);
    });
  });
}

function parseJavaArgs(raw) {
  if (!raw || typeof raw !== 'string') return [];
  const args = [];
  const re = /([^\s"']+)|"([^"]*)"|'([^']*)'/g;
  let match;
  while ((match = re.exec(raw)) !== null) {
    args.push(match[1] ?? match[2] ?? match[3] ?? '');
  }
  return args.filter((v) => v.length > 0);
}

function sanitizeOfflineName(name) {
  if (typeof name !== 'string') return '';
  const trimmed = name.trim();
  return trimmed.replace(/[^A-Za-z0-9_]/g, '').slice(0, 16);
}

function maybeGunzip(buf) {
  if (!buf || buf.length < 2) return buf;
  // gzip magic
  if (buf[0] === 0x1f && buf[1] === 0x8b) {
    try {
      return zlib.gunzipSync(buf);
    } catch {
      return buf;
    }
  }
  return buf;
}

function parseNbtString(buf, offset) {
  const len = buf.readUInt16BE(offset);
  const start = offset + 2;
  const end = start + len;
  return { value: buf.toString('utf8', start, end), offset: end };
}

function parseNbtTagPayload(buf, type, offset) {
  switch (type) {
    case 0:
      return { value: null, offset };
    case 1:
      return { value: buf.readInt8(offset), offset: offset + 1 };
    case 2:
      return { value: buf.readInt16BE(offset), offset: offset + 2 };
    case 3:
      return { value: buf.readInt32BE(offset), offset: offset + 4 };
    case 4:
      return { value: Number(buf.readBigInt64BE(offset)), offset: offset + 8 };
    case 5:
      return { value: buf.readFloatBE(offset), offset: offset + 4 };
    case 6:
      return { value: buf.readDoubleBE(offset), offset: offset + 8 };
    case 7: {
      const len = buf.readInt32BE(offset);
      const start = offset + 4;
      return { value: buf.subarray(start, start + len), offset: start + len };
    }
    case 8:
      return parseNbtString(buf, offset);
    case 9: {
      const itemType = buf.readUInt8(offset);
      const len = buf.readInt32BE(offset + 1);
      let cursor = offset + 5;
      const arr = [];
      for (let i = 0; i < len; i += 1) {
        const parsed = parseNbtTagPayload(buf, itemType, cursor);
        arr.push(parsed.value);
        cursor = parsed.offset;
      }
      return { value: arr, offset: cursor };
    }
    case 10: {
      let cursor = offset;
      const obj = {};
      while (cursor < buf.length) {
        const t = buf.readUInt8(cursor);
        cursor += 1;
        if (t === 0) break;
        const nameParsed = parseNbtString(buf, cursor);
        cursor = nameParsed.offset;
        const payload = parseNbtTagPayload(buf, t, cursor);
        obj[nameParsed.value] = payload.value;
        cursor = payload.offset;
      }
      return { value: obj, offset: cursor };
    }
    case 11: {
      const len = buf.readInt32BE(offset);
      const start = offset + 4;
      const out = [];
      for (let i = 0; i < len; i += 1) out.push(buf.readInt32BE(start + i * 4));
      return { value: out, offset: start + len * 4 };
    }
    case 12: {
      const len = buf.readInt32BE(offset);
      const start = offset + 4;
      const out = [];
      for (let i = 0; i < len; i += 1) out.push(Number(buf.readBigInt64BE(start + i * 8)));
      return { value: out, offset: start + len * 8 };
    }
    default:
      throw new Error(`Unsupported NBT tag type: ${type}`);
  }
}

function parseNbtRoot(buf) {
  if (!buf || buf.length < 3) return null;
  const rootType = buf.readUInt8(0);
  if (rootType !== 10) return null;
  const rootName = parseNbtString(buf, 1);
  const rootPayload = parseNbtTagPayload(buf, 10, rootName.offset);
  return rootPayload.value;
}

function readServersFromDat(cfg) {
  try {
    const serversPath = path.join(cfg.gameDir, 'servers.dat');
    if (!fs.existsSync(serversPath)) return [];
    const raw = fs.readFileSync(serversPath);
    const data = maybeGunzip(raw);
    const root = parseNbtRoot(data);
    const list = Array.isArray(root?.servers) ? root.servers : [];
    return list
      .map((s) => {
        const name = typeof s?.name === 'string' ? s.name.trim() : '';
        const address = typeof s?.ip === 'string' ? s.ip.trim() : '';
        let icon = typeof s?.icon === 'string' ? s.icon.trim() : '';
        if (!icon) icon = '';
        if (icon && !icon.startsWith('data:image/')) icon = `data:image/png;base64,${icon}`;
        return {
          name: name || address,
          address,
          icon: icon || ''
        };
      })
      .filter((s) => s.address.length > 0)
      .slice(0, 5);
  } catch (err) {
    sendLog(`[Launcher] Failed to parse servers.dat: ${err.message}\n`);
    return [];
  }
}

function toPremiumView(account) {
  return {
    username: account?.username || '',
    uuid: account?.uuid || '',
    updatedAt: account?.updatedAt || 0
  };
}

function getActivePremium(cfg) {
  if (!Array.isArray(cfg.premiumAccounts) || cfg.premiumAccounts.length === 0) return null;
  const active = cfg.premiumAccounts.find((a) => a && a.uuid === cfg.activePremiumUuid);
  return active || cfg.premiumAccounts[0];
}

function upsertPremiumAccount(cfg, account) {
  if (!Array.isArray(cfg.premiumAccounts)) cfg.premiumAccounts = [];
  const idx = cfg.premiumAccounts.findIndex((a) => a && a.uuid === account.uuid);
  if (idx >= 0) cfg.premiumAccounts[idx] = account;
  else cfg.premiumAccounts.push(account);
  cfg.activePremiumUuid = account.uuid || '';
}

async function premiumLogin(cfg) {
  if (!MSMCAuth) {
    throw new Error('MSMC dependency is missing. Run npm install in launcher/');
  }
  const onWindowCreated = (event, win) => {
    win.focus();
    win.moveTop();
    app.removeListener('browser-window-created', onWindowCreated);
  };
  app.on('browser-window-created', onWindowCreated);
  const authManager = new MSMCAuth('select_account');
  const xbox = await authManager.launch('electron', {
    width: 520,
    height: 760,
    resizable: false
  });
  app.removeListener('browser-window-created', onWindowCreated);
  const mc = await xbox.getMinecraft();
  const mclc = mc.mclc();
  cfg.authMode = 'premium';
  const premiumAccount = {
    username: mc.profile?.name || mclc?.name || '',
    uuid: mc.profile?.id || mclc?.uuid || '',
    refreshToken: xbox.save(),
    xuid: mc.xuid || '',
    updatedAt: Date.now()
  };
  upsertPremiumAccount(cfg, premiumAccount);
  writeConfig(cfg);
  return {
    mode: cfg.authMode,
    premiumAccounts: cfg.premiumAccounts.map(toPremiumView),
    activePremiumUuid: cfg.activePremiumUuid,
    offlineAccounts: cfg.offlineAccounts || [],
    activeOfflineName: cfg.activeOfflineName || 'Player'
  };
}

async function getLaunchAuthorization(cfg) {
  const activePremium = getActivePremium(cfg);
  if (cfg.authMode === 'premium' && activePremium?.refreshToken && MSMCAuth) {
    try {
      const authManager = new MSMCAuth('select_account');
      const xbox = await authManager.refresh(activePremium.refreshToken);
      const mc = await xbox.getMinecraft();
      const mclc = mc.mclc();
      const refreshed = {
        username: mc.profile?.name || mclc?.name || activePremium.username || '',
        uuid: mc.profile?.id || mclc?.uuid || activePremium.uuid || '',
        refreshToken: xbox.save(),
        xuid: mc.xuid || activePremium.xuid || '',
        updatedAt: Date.now()
      };
      upsertPremiumAccount(cfg, refreshed);
      writeConfig(cfg);
      sendLog(`[Launcher] Premium auth ready for ${refreshed.username}.\n`);
      return mclc;
    } catch (err) {
      sendLog(`[Launcher] Premium auth failed, using offline fallback: ${err.message}\n`);
    }
  }
  const offlineName = sanitizeOfflineName(cfg.activeOfflineName);
  if (!offlineName) {
    throw new Error('No account selected. Add offline account or login premium first.');
  }
  return await Authenticator.getAuth(offlineName);
}

function applyBladeClientPresenceConfig(cfg) {
  try {
    const configDir = path.join(cfg.gameDir, 'config');
    const configFile = path.join(configDir, 'bladeclient.json');
    fs.mkdirSync(configDir, { recursive: true });
    let json = {};
    if (fs.existsSync(configFile)) {
      try {
        json = JSON.parse(fs.readFileSync(configFile, 'utf8')) || {};
      } catch {
        json = {};
      }
    }

    const launcherUrl = localPresenceBaseUrl || '';
    const presence = { ...(json.presence || {}) };
    presence.launcherApiUrl = launcherUrl;
    json.presence = presence;

    const activePremium = getActivePremium(cfg);
    const account = { ...(json.account || {}) };
    account.offlineAccounts = Array.isArray(cfg.offlineAccounts) ? cfg.offlineAccounts : [];
    account.offlineName = cfg.activeOfflineName || '';
    account.useOffline = cfg.authMode !== 'premium';
    account.launcherPremiumAccounts = (cfg.premiumAccounts || [])
      .map((a) => (a && a.username ? String(a.username).trim() : ''))
      .filter((v) => v.length > 0);
    account.launcherActivePremium = activePremium?.username || '';
    json.account = account;

    json.launchFullscreen = !!cfg.launchFullscreen;

    fs.writeFileSync(configFile, JSON.stringify(json, null, 2), 'utf8');
  } catch (err) {
    sendLog(`[Launcher] Failed to write BladeClient config: ${err.message}\n`);
  }
}

function parseRetryAfter(header) {
  if (!header) return null;
  const num = Number(header);
  if (!Number.isNaN(num)) {
    return Math.max(0, num * 1000);
  }
  const dateMs = Date.parse(header);
  if (!Number.isNaN(dateMs)) {
    return Math.max(0, dateMs - Date.now());
  }
  return null;
}

function normalizeStatus(raw) {
  if (!raw || typeof raw !== 'string') return 'launcher';
  const v = raw.trim().toLowerCase();
  if (v === 'server' || v === 'client' || v === 'launcher') return v;
  return 'launcher';
}

function normalizeServerName(raw) {
  if (!raw || typeof raw !== 'string') return '';
  return raw.trim().slice(0, 64);
}

function normalizeServerAddress(raw) {
  if (!raw || typeof raw !== 'string') return '';
  return raw.trim().toLowerCase().slice(0, 128);
}

function normalizeIconUrl(raw) {
  if (!raw || typeof raw !== 'string') return '';
  try {
    const url = new URL(raw.trim());
    if (url.protocol !== 'http:' && url.protocol !== 'https:') return '';
    return url.toString();
  } catch {
    return '';
  }
}

function decodeBase64Image(raw) {
  if (!raw || typeof raw !== 'string') return null;
  const trimmed = raw.trim();
  let mime = 'image/png';
  let data = trimmed;
  if (trimmed.startsWith('data:')) {
    const split = trimmed.indexOf(',');
    if (split > 0) {
      const header = trimmed.slice(5, split);
      const semi = header.indexOf(';');
      if (semi > 0) {
        mime = header.slice(0, semi);
      }
      data = trimmed.slice(split + 1);
    }
  }
  try {
    const buffer = Buffer.from(data, 'base64');
    if (!buffer || buffer.length === 0) return null;
    return { buffer, mime };
  } catch {
    return null;
  }
}

function requestJsonOnce(url) {
  return new Promise((resolve, reject) => {
    if (cancelRequested) return reject(new Error('Launch cancelled'));
    let client;
    try {
      const protocol = new URL(url).protocol;
      if (protocol === 'http:') client = http;
      else if (protocol === 'https:') client = https;
    } catch {
      client = null;
    }

    if (!client) {
      return reject(new Error(`Protocol not supported for URL: ${url}`));
    }

    const req = client.get(url, (res) => {
      if (res.statusCode && res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        const nextUrl = resolveRedirect(url, res.headers.location);
        activeRequests.delete(req);
        return requestJsonOnce(nextUrl).then(resolve).catch(reject);
      }
      if (res.statusCode !== 200) {
        const err = new Error(`Request failed: ${res.statusCode}`);
        err.status = res.statusCode;
        err.headers = res.headers || {};
        activeRequests.delete(req);
        return reject(err);
      }

      let raw = '';
      res.setEncoding('utf8');
      res.on('data', (chunk) => { raw += chunk; });
      res.on('end', () => {
        activeRequests.delete(req);
        try {
          resolve(JSON.parse(raw));
        } catch (err) {
          reject(err);
        }
      });
    });
    activeRequests.add(req);
    req.on('error', (err) => {
      activeRequests.delete(req);
      reject(err);
    });
  });
}

async function requestJson(url, options = {}) {
  const retries = Number.isFinite(options.retries) ? options.retries : 0;
  const retryOn = Array.isArray(options.retryOn) ? options.retryOn : [429];
  const baseDelay = Number.isFinite(options.retryDelayMs) ? options.retryDelayMs : 1500;

  for (let attempt = 0; attempt <= retries; attempt += 1) {
    try {
      return await requestJsonOnce(url);
    } catch (err) {
      const status = err && typeof err.status === 'number' ? err.status : null;
      if (status && retryOn.includes(status) && attempt < retries) {
        const retryAfterMs = parseRetryAfter(err.headers && err.headers['retry-after']);
        const backoff = baseDelay * Math.pow(2, attempt);
        const delay = Math.min(30000, retryAfterMs != null ? Math.max(retryAfterMs, backoff) : backoff);
        sendLog(`[Launcher] Request ${status}, retrying in ${Math.ceil(delay / 1000)}s...\n`);
        await sleep(delay);
        throwIfCancelled();
        continue;
      }
      throw err;
    }
  }
  throw new Error('Request failed');
}

function setLauncherPresenceStatus(status) {
  launcherPresenceStatus = status === 'client' ? 'client' : 'launcher';
  localLauncherState = { source: 'launcher', status: launcherPresenceStatus, ts: Date.now() };
  updateRpcFromLocalPresence();
}

function updateRpcFromLocalStatus(status) {
  localLauncherState = { source: 'launcher', status, ts: Date.now() };
  updateRpcFromLocalPresence();
}

function getLocalPresenceState() {
  const now = Date.now();
  const clientFresh = localClientState.ts && now - localClientState.ts < LOCAL_PRESENCE_TTL_MS;
  const launcherFresh = localLauncherState.ts && now - localLauncherState.ts < LOCAL_PRESENCE_TTL_MS;
  if (clientFresh) return localClientState;
  if (launcherFresh) return localLauncherState;
  return { source: 'launcher', status: 'launcher', ts: now };
}

function updateRpcFromLocalPresence() {
  if (!rpcReady) return;
  const state = getLocalPresenceState();
  const status = state.status === 'server' ? 'server' : (state.status === 'client' ? 'client' : 'launcher');
  const serverName = typeof state.serverName === 'string' ? state.serverName : '';
  const serverAddress = typeof state.serverAddress === 'string' ? state.serverAddress : '';
  const serverIconUrl = typeof state.serverIconUrl === 'string' ? state.serverIconUrl : '';
  const signature = `local|${status}|${serverName}|${serverAddress}|${serverIconUrl}`;
  if (rpcStateSignature === signature) return;
  rpcStateSignature = signature;
  if (rpcLastStatus !== status) {
    rpcStartTimestamp = Math.floor(Date.now() / 1000);
    rpcLastStatus = status;
  }

  let details = 'Active in launcher';
  let stateText = '';
  let largeImageKey = 'icon';
  let largeImageText = 'BladeClient';
  let smallImageKey = '';
  let smallImageText = '';

  if (status === 'client') {
    details = 'In client';
    stateText = serverName || '';
  } else if (status === 'server') {
    details = serverName && serverName.trim().length > 0 ? serverName : 'In server';
    stateText = serverAddress || '';
    if (serverIconUrl) largeImageKey = serverIconUrl;
    if (serverAddress && serverAddress.trim().length > 0) {
      largeImageText = serverAddress.trim();
    }
    smallImageKey = 'icon';
    smallImageText = 'BladeClient';
  }

  const activity = {
    details,
    largeImageKey,
    largeImageText,
    startTimestamp: rpcStartTimestamp
  };
  if (stateText && stateText.trim().length > 0) {
    activity.state = stateText;
  }
  if (smallImageKey) {
    activity.smallImageKey = smallImageKey;
    activity.smallImageText = smallImageText;
  }
  rpcClient?.setActivity(activity).catch((err) => {
    sendLog(`[Launcher] Discord RPC setActivity failed: ${err?.message || err}\n`);
  });
}

function initDiscordRpc() {
  if (!DiscordRPC) {
    sendLog('[Launcher] Discord RPC module not available.\n');
    return;
  }
  if (rpcClient) return;
  if (typeof DiscordRPC.register === 'function') {
    try {
      DiscordRPC.register(DISCORD_CLIENT_ID);
    } catch {}
  }
  rpcClient = new DiscordRPC.Client({ transport: 'ipc' });
  rpcClient.on('ready', () => {
    rpcReady = true;
    rpcStartTimestamp = Math.floor(Date.now() / 1000);
    sendLog('[Launcher] Discord RPC ready.\n');
    updateRpcFromLocalPresence();
    if (!rpcKeepAliveTimer) {
      rpcKeepAliveTimer = setInterval(() => {
        if (rpcReady) updateRpcFromLocalPresence();
      }, 15000);
    }
  });
  rpcClient.on('error', (err) => {
    sendLog(`[Launcher] Discord RPC error: ${err?.message || err}\n`);
  });
  rpcClient.on('disconnected', () => {
    rpcReady = false;
  });
  rpcClient.login({ clientId: DISCORD_CLIENT_ID }).catch((err) => {
    sendLog(`[Launcher] Discord RPC unavailable: ${err.message}\n`);
  });
}

function stopLauncherPresence() {
  if (rpcPollTimer) {
    clearInterval(rpcPollTimer);
    rpcPollTimer = null;
  }
  if (launcherStateTimer) {
    clearInterval(launcherStateTimer);
    launcherStateTimer = null;
  }
  if (rpcKeepAliveTimer) {
    clearInterval(rpcKeepAliveTimer);
    rpcKeepAliveTimer = null;
  }
  if (rpcClient) {
    try { rpcClient.clearActivity().catch(() => {}); } catch {}
    try { rpcClient.destroy(); } catch {}
  }
  rpcClient = null;
  rpcReady = false;
  rpcStateSignature = '';
  rpcLastStatus = '';
}

function startLauncherPresence(cfg) {
  stopLauncherPresence();
  if (!cfg || !cfg.richPresenceEnabled) return;
  startLocalPresenceServer();
  localLauncherState = { source: 'launcher', status: launcherPresenceStatus, ts: Date.now() };
  initDiscordRpc();
  updateRpcFromLocalPresence();
  launcherStateTimer = setInterval(() => {
    updateRpcFromLocalPresence();
  }, 5000);
}

function applyPresenceSettings(cfg) {
  startLauncherPresence(cfg);
}

function startLocalPresenceServer() {
  if (localPresenceServer) return;

  localPresenceServer = http.createServer((req, res) => {
    try {
      const url = new URL(req.url || '/', 'http://127.0.0.1');
      const pathName = url.pathname;
      const method = req.method || 'GET';

      if (method === 'GET' && pathName === '/launcher/state') {
        const state = getLocalPresenceState();
        const response = {
          source: state.source,
          status: state.status,
          serverName: state.serverName || '',
          serverAddress: state.serverAddress || '',
          serverIconUrl: state.serverIconUrl || ''
        };
        if (state.status === 'server' && localClientState.iconBuffer) {
          response.iconUrl = `/launcher/icon?ts=${localClientState.iconUpdatedAt}`;
        }
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(response));
        return;
      }

      if (method === 'GET' && pathName === '/launcher/icon') {
        if (!localClientState.iconBuffer) {
          res.writeHead(404);
          res.end();
          return;
        }
        res.writeHead(200, {
          'Content-Type': localClientState.iconMime || 'image/png',
          'Cache-Control': 'no-cache'
        });
        res.end(localClientState.iconBuffer);
        return;
      }

      if (method === 'GET' && pathName === getVerifyEndpoint()) {
        handleVerifyRequest(url, res);
        return;
      }

      if (method === 'POST' && pathName === '/launcher/state') {
        let raw = '';
        req.setEncoding('utf8');
        req.on('data', (chunk) => {
          raw += chunk;
          if (raw.length > 256 * 1024) {
            raw = '';
            req.destroy();
          }
        });
        req.on('end', () => {
          let body = {};
          try {
            body = raw ? JSON.parse(raw) : {};
          } catch {
            res.writeHead(400);
            res.end();
            return;
          }

          const source = body.source === 'client' ? 'client' : 'launcher';
          const status = normalizeStatus(body.status);
          const nowTs = Date.now();

          if (source === 'launcher') {
            localLauncherState = { source, status, ts: nowTs };
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ ok: true }));
            updateRpcFromLocalPresence();
            return;
          }

          const isServer = status === 'server';
          const serverName = normalizeServerName(body.serverName);
          const serverAddress = isServer ? normalizeServerAddress(body.serverAddress || body.server) : '';
          const icon = isServer ? decodeBase64Image(body.serverIcon || body.icon || '') : null;
          const serverIconUrl = isServer ? normalizeIconUrl(body.serverIconUrl || '') : '';

          localClientState = {
            ...localClientState,
            source,
            status,
            ts: nowTs,
            serverName,
            serverAddress,
            serverIconUrl
          };

          if (icon) {
            localClientState.iconBuffer = icon.buffer;
            localClientState.iconMime = icon.mime;
            localClientState.iconUpdatedAt = nowTs;
          } else if (!isServer) {
            localClientState.iconBuffer = null;
            localClientState.iconMime = 'image/png';
            localClientState.iconUpdatedAt = 0;
          }

          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ ok: true }));
          const nextSig = `${status}|${serverName}|${serverAddress}`;
          if (nextSig !== localStateLogSignature) {
            localStateLogSignature = nextSig;
            sendLog(`[Launcher] Client presence: ${status}${serverName ? ` (${serverName})` : ''}\n`);
          }
          updateRpcFromLocalPresence();
        });
        return;
      }

      if (method === 'POST' && pathName === '/launcher/auth/select') {
        let raw = '';
        req.setEncoding('utf8');
        req.on('data', (chunk) => {
          raw += chunk;
          if (raw.length > 32 * 1024) {
            raw = '';
            req.destroy();
          }
        });
        req.on('end', () => {
          let body = {};
          try {
            body = raw ? JSON.parse(raw) : {};
          } catch {
            res.writeHead(400, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ ok: false, error: 'invalid json' }));
            return;
          }

          const mode = String(body.mode || '').trim().toLowerCase();
          const name = String(body.name || '').trim();
          if (!name || (mode !== 'offline' && mode !== 'premium')) {
            res.writeHead(400, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ ok: false, error: 'mode and name required' }));
            return;
          }

          const cfg = loadConfig();
          if (mode === 'offline') {
            const existing = (cfg.offlineAccounts || []).find((n) => String(n || '').trim().toLowerCase() === name.toLowerCase());
            const finalName = existing || name;
            if (!existing) {
              cfg.offlineAccounts = Array.isArray(cfg.offlineAccounts) ? cfg.offlineAccounts : [];
              cfg.offlineAccounts.push(finalName);
            }
            cfg.activeOfflineName = finalName;
            cfg.authMode = 'offline';
            writeConfig(cfg);
            sendLog(`[Launcher] Account selected from client: offline (${finalName})\n`);
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ ok: true }));
            return;
          }

          const premium = (cfg.premiumAccounts || []).find((a) =>
            String(a?.username || '').trim().toLowerCase() === name.toLowerCase());
          if (!premium?.uuid) {
            res.writeHead(404, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ ok: false, error: 'premium account not found' }));
            return;
          }

          cfg.activePremiumUuid = premium.uuid;
          cfg.authMode = 'premium';
          writeConfig(cfg);
          sendLog(`[Launcher] Account selected from client: premium (${premium.username})\n`);
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ ok: true }));
        });
        return;
      }

      if (method === 'POST' && pathName === '/launcher/auth/microsoft') {
        handleMicrosoftAuth(res);
        return;
      }

      res.writeHead(404);
      res.end();
    } catch {
      res.writeHead(500);
      res.end();
    }
  });

  async function handleMicrosoftAuth(res) {
    try {
      if (!MSMCAuth) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ ok: false, error: 'MSMC dependency missing' }));
        return;
      }
      const onWindowCreated = (event, win) => {
        win.focus();
        win.moveTop();
        app.removeListener('browser-window-created', onWindowCreated);
      };
      app.on('browser-window-created', onWindowCreated);
      const authManager = new MSMCAuth('select_account');
      const xbox = await authManager.launch('electron', {
        width: 520,
        height: 760,
        resizable: false
      });
      app.removeListener('browser-window-created', onWindowCreated);
      const mc = await xbox.getMinecraft();
      const mclc = mc.mclc();
      const session = {
        ok: true,
        username: mc.profile?.name || mclc?.name || '',
        uuid: mc.profile?.id || mclc?.uuid || '',
        accessToken: mclc?.access_token || '',
        xuid: mc.xuid || ''
      };
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(session));
    } catch (err) {
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ ok: false, error: err.message || 'Microsoft auth failed' }));
    }
  }

  localPresenceServer.on('error', (err) => {
    sendLog(`[Launcher] Local presence server error: ${err.message}\n`);
  });

  const onListening = () => {
    const addr = localPresenceServer.address();
    const port = typeof addr === 'object' && addr ? addr.port : 0;
    if (!port) {
      sendLog('[Launcher] Local presence server failed to bind.\n');
      return;
    }
    localPresenceBaseUrl = `http://${LOCAL_PRESENCE_HOST}:${port}`;
    sendLog(`[Launcher] Local presence active on ${localPresenceBaseUrl}\n`);
    applyBladeClientPresenceConfig(readConfig());
    updateRpcFromLocalPresence();
  };

  localPresenceServer.listen(LOCAL_PRESENCE_PORT, LOCAL_PRESENCE_HOST, onListening);
  localPresenceServer.once('error', (err) => {
    if (err && err.code === 'EADDRINUSE') {
      sendLog(`[Launcher] Local presence port ${LOCAL_PRESENCE_PORT} busy, using random port.\n`);
      try {
        localPresenceServer.listen(0, LOCAL_PRESENCE_HOST, onListening);
      } catch (retryErr) {
        sendLog(`[Launcher] Local presence retry failed: ${retryErr.message}\n`);
      }
    }
  });
}

function safeUrlString(raw) {
  if (!raw || typeof raw !== 'string') return '';
  try {
    return new URL(raw).toString();
  } catch {
    return '';
  }
}

function normalizeVersionString(raw) {
  if (!raw) return '';
  let v = String(raw).trim();
  if (v.toLowerCase().startsWith('v')) v = v.slice(1).trim();
  return v.toLowerCase();
}

function isSameVersion(a, b) {
  const va = normalizeVersionString(a);
  const vb = normalizeVersionString(b);
  if (!va || !vb) return false;
  return va === vb;
}

function findFirstFile(dir, predicate) {
  if (!fs.existsSync(dir)) return '';
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      const child = findFirstFile(full, predicate);
      if (child) return child;
    } else if (predicate(full)) {
      return full;
    }
  }
  return '';
}

function getMarkerEntry(marker, slug) {
  const entry = marker[slug];
  if (!entry) return null;
  if (typeof entry === 'string') {
    return { id: entry, file: '' };
  }
  if (typeof entry === 'object') {
    return {
      id: typeof entry.id === 'string' ? entry.id : '',
      file: typeof entry.file === 'string' ? entry.file : ''
    };
  }
  return null;
}

function findExistingModFile(modsDir, slug) {
  if (!fs.existsSync(modsDir)) return '';
  const jars = fs.readdirSync(modsDir).filter((f) => f.toLowerCase().endsWith('.jar'));
  return jars.find((f) => modFileMatchesSlug(f, slug)) || '';
}

function getBundledModsDir() {
  return path.join(__dirname, '..', 'mods');
}

function modFileMatchesSlug(fileName, slug) {
  const lower = String(fileName || '').toLowerCase();
  if (!lower.endsWith('.jar')) return false;
  switch (String(slug || '').toLowerCase()) {
    case 'fabric-api': return lower.includes('fabric-api');
    case 'yacl': return lower.includes('yet_another_config_lib') || lower.includes('yacl');
    case 'owo-lib': return lower.includes('owo-lib');
    case 'simple-voice-chat': return lower.includes('voicechat');
    default: return lower.includes(String(slug || '').toLowerCase());
  }
}

function findBundledModFile(slug) {
  const bundledDir = getBundledModsDir();
  if (!fs.existsSync(bundledDir)) return '';
  const jars = fs.readdirSync(bundledDir).filter((f) => modFileMatchesSlug(f, slug));
  if (!jars.length) return '';
  return path.join(bundledDir, jars[0]);
}

function seedBundledRequiredMods(modsDir, required) {
  for (const mod of required) {
    const existing = findExistingModFile(modsDir, mod.slug);
    if (existing) continue;
    const bundled = findBundledModFile(mod.slug);
    if (!bundled) continue;
    const destName = path.basename(bundled);
    const dest = path.join(modsDir, destName);
    try {
      fs.copyFileSync(bundled, dest);
      sendLog(`[Launcher] Seeded local mod ${mod.name}: ${destName}\n`);
    } catch (err) {
      sendLog(`[Launcher] Failed to seed ${mod.name} from launcher/mods: ${err.message}\n`);
    }
  }
}

function downloadFile(url, dest, onProgress) {
  return new Promise((resolve, reject) => {
    let settled = false;
    const doneResolve = (value) => {
      if (settled) return;
      settled = true;
      resolve(value);
    };
    const doneReject = (err) => {
      if (settled) return;
      settled = true;
      reject(err);
    };
    if (cancelRequested) return reject(new Error('Launch cancelled'));
    let client;
    try {
      const protocol = new URL(url).protocol;
      if (protocol === 'http:') client = http;
      else if (protocol === 'https:') client = https;
    } catch {
      client = null;
    }

    if (!client) {
      return reject(new Error(`Protocol not supported for URL: ${url}`));
    }

    const file = fs.createWriteStream(dest);
    const entry = { req: null, file, dest };
    const req = client.get(url, (res) => {
      if (res.statusCode && res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        const nextUrl = resolveRedirect(url, res.headers.location);
        file.close();
        activeDownloads.delete(entry);
        if (cancelRequested) return doneReject(new Error('Launch cancelled'));
        return doneResolve(downloadFile(nextUrl, dest, onProgress));
      }
      if (res.statusCode !== 200) {
        file.close();
        activeDownloads.delete(entry);
        return doneReject(new Error(`Download failed: ${res.statusCode}`));
      }
      activeDownloadResponses.add(res);
      if (downloadsPaused) {
        try { res.pause(); } catch {}
      }
      currentDownloadBytes = 0;
      const totalHeader = res.headers['content-length'];
      currentDownloadTotal = totalHeader ? Number(totalHeader) : 0;
      res.on('data', (chunk) => {
        if (cancelRequested) {
          try { req.destroy(); } catch {}
        }
        if (chunk && chunk.length) {
          currentDownloadBytes += chunk.length;
          updateDownloadSpeed(chunk.length);
          if (typeof onProgress === 'function' && currentDownloadTotal > 0) {
            const percent = Math.max(0, Math.min(100, Math.floor((currentDownloadBytes / currentDownloadTotal) * 100)));
            onProgress(percent, currentDownloadBytes, currentDownloadTotal);
          }
        }
      });
      res.pipe(file);
      file.on('finish', () => file.close(() => {
        activeDownloadResponses.delete(res);
        if (typeof onProgress === 'function') onProgress(100, currentDownloadBytes, currentDownloadTotal);
        doneResolve();
      }));
    }).on('error', (err) => {
      file.close();
      activeDownloads.delete(entry);
      doneReject(err);
    });
    entry.req = req;
    activeDownloads.add(entry);
    req.on('close', () => {
      activeDownloads.delete(entry);
      if (cancelRequested && !settled) doneReject(new Error('Launch cancelled'));
    });
    req.on('error', () => {
      activeDownloads.delete(entry);
    });
    file.on('error', () => {
      activeDownloads.delete(entry);
      if (cancelRequested && !settled) doneReject(new Error('Launch cancelled'));
    });
    req.on('close', () => {
      for (const res of activeDownloadResponses) {
        if (res.destroyed || res.complete) activeDownloadResponses.delete(res);
      }
    });
  });
}

function patchMclcAssetsDownload() {
  if (assetsPatchApplied) return;
  if (!MclcHandler || !MclcHandler.prototype) return;
  const original = MclcHandler.prototype.getAssets;
  MclcHandler.prototype.getAssets = async function patchedGetAssets() {
    this.client.emit('debug', '[MCLC]: Skipping assets download (assets-cache.zip)');
    return;
  };
  MclcHandler.prototype.getAssets._bladeclientOriginal = original;
  assetsPatchApplied = true;
}

async function applyLauncherUpdate(filePath) {
  if (!filePath || !fs.existsSync(filePath)) {
    throw new Error('Update file missing');
  }
  const lower = filePath.toLowerCase();
  if (lower.endsWith('.zip')) {
    const extractDir = path.join(userData, 'launcher-update');
    fs.rmSync(extractDir, { recursive: true, force: true });
    fs.mkdirSync(extractDir, { recursive: true });
    await extract(filePath, { dir: extractDir });
    const exePath = findFirstFile(extractDir, (p) => p.toLowerCase().endsWith('.exe'));
    if (!exePath) {
      throw new Error('Updater exe not found');
    }
    const child = spawn(exePath, [], { detached: true, stdio: 'ignore', shell: true });
    child.unref();
    return;
  }
  if (lower.endsWith('.msi')) {
    const child = spawn('msiexec', ['/i', filePath], { detached: true, stdio: 'ignore', shell: true });
    child.unref();
    return;
  }
  if (lower.endsWith('.exe')) {
    const child = spawn(filePath, [], { detached: true, stdio: 'ignore', shell: true });
    child.unref();
    return;
  }
  throw new Error('Unknown update package');
}

async function checkLauncherUpdate() {
  if (updateCheckStarted) return;
  updateCheckStarted = true;
  updateInstallTriggered = false;

  if (!app.isPackaged) {
    sendUpdate({ status: 'ready' });
    return;
  }

  const cfg = readConfig();
  const updateUrl = cfg.launcherUpdateUrl && cfg.launcherUpdateUrl.trim().length > 0 ? cfg.launcherUpdateUrl.trim() : '';
  if (autoUpdater && updateUrl) {
    try {
      const triggerAutoInstall = () => {
        if (updateInstallTriggered) return;
        updateInstallTriggered = true;
        sendUpdate({ status: 'installing', label: 'Installing update...' });
        setTimeout(() => {
          try {
            // Close launcher and immediately hand off to installer.
            autoUpdater.quitAndInstall(false, true);
          } catch {
            try { app.quit(); } catch {}
          }
        }, 150);
      };

      autoUpdater.autoDownload = true;
      autoUpdater.autoInstallOnAppQuit = true;
      autoUpdater.setFeedURL({ provider: 'generic', url: updateUrl });

      autoUpdater.on('checking-for-update', () => {
        sendUpdate({ status: 'checking', label: 'Checking for launcher updates...' });
      });
      autoUpdater.on('update-available', (info) => {
        sendUpdate({ status: 'available', version: info?.version || '', label: `Update found: ${info?.version || 'latest'}` });
      });
      autoUpdater.on('download-progress', (progress) => {
        const percent = Math.max(0, Math.min(100, Math.floor(progress?.percent || 0)));
        sendUpdate({ status: 'downloading', percent, label: `Downloading update... ${percent}%` });
        if (percent >= 100) triggerAutoInstall();
      });
      autoUpdater.on('update-downloaded', (info) => {
        sendUpdate({ status: 'downloaded', version: info?.version || '', label: 'Update downloaded, installing...' });
        triggerAutoInstall();
      });
      autoUpdater.on('update-not-available', () => {
        sendUpdate({ status: 'ready' });
      });
      autoUpdater.on('error', (err) => {
        sendLog(`[Launcher] Update error: ${err?.message || err}
`);
        sendUpdate({ status: 'ready' });
      });

      withTimeout(autoUpdater.checkForUpdates(), 30000).catch((err) => {
        if (String(err?.message || '').toLowerCase().includes('timeout')) {
          sendLog('[Launcher] Update check timeout after 30 seconds.\n');
        } else {
          sendLog(`[Launcher] Update check failed: ${err?.message || err}
`);
        }
        sendUpdate({ status: 'ready' });
      });
      return;
    } catch (err) {
      sendLog(`[Launcher] Update init failed: ${err?.message || err}
`);
    }
  }

  const base = cfg.presenceApiUrl && cfg.presenceApiUrl.trim().length > 0 ? cfg.presenceApiUrl.trim() : '';
  if (!base) {
    sendUpdate({ status: 'ready' });
    return;
  }

  const url = base.replace(/\/+$/, '') + '/launcher/version';
  sendUpdate({ status: 'checking', label: 'Checking for updates...' });

  let info;
  try {
    info = await withTimeout(requestJson(url, { retries: 1, retryOn: [429, 502, 503], retryDelayMs: 1500 }), 30000);
  } catch (err) {
    sendLog(`[Launcher] Update check failed: ${err.message}\n`);
    sendUpdate({ status: 'ready' });
    return;
  }

  const latestVersion = info && typeof info.version === 'string' ? info.version.trim() : '';
  const updateDownloadUrl = safeUrlString(info && info.url);
  if (!latestVersion || !updateDownloadUrl) {
    sendUpdate({ status: 'ready' });
    return;
  }

  const current = app.getVersion();
  if (isSameVersion(current, latestVersion)) {
    sendUpdate({ status: 'ready' });
    return;
  }

  sendUpdate({ status: 'downloading', label: `Downloading ${latestVersion}...`, percent: 0 });
  sendLog(`[Launcher] Update available: ${latestVersion}\n`);
  const updatesDir = path.join(userData, 'updates');
  fs.mkdirSync(updatesDir, { recursive: true });
  const fileName = path.basename(new URL(updateDownloadUrl).pathname) || `launcher-${latestVersion}.bin`;
  const dest = path.join(updatesDir, fileName);

  try {
    await downloadFile(updateDownloadUrl, dest, (percent) => {
      sendUpdate({ status: 'downloading', label: `Downloading ${latestVersion}...`, percent });
    });
  } catch (err) {
    sendLog(`[Launcher] Update download failed: ${err.message}\n`);
    sendUpdate({ status: 'ready' });
    return;
  }

  sendUpdate({ status: 'installing', label: 'Installing update...' });
  try {
    await applyLauncherUpdate(dest);
    sendLog('[Launcher] Update installer launched. Closing...\n');
    setTimeout(() => app.quit(), 500);
  } catch (err) {
    sendLog(`[Launcher] Update install failed: ${err.message}\n`);
    sendUpdate({ status: 'ready' });
  }
}

async function requestModrinthVersions(slug, gameVersion) {
  const base = `https://api.modrinth.com/v2/project/${slug}/version`;
  const params = new URLSearchParams();
  params.set('loaders', JSON.stringify(['fabric']));
  if (gameVersion) {
    params.set('game_versions', JSON.stringify([gameVersion]));
  }
  const url = `${base}?${params.toString()}`;
  return requestJson(url, { retries: 3, retryOn: [429, 502, 503], retryDelayMs: 1500 });
}

function pickModrinthVersion(versions, gameVersion) {
  if (!Array.isArray(versions) || versions.length === 0) return null;
  const exact = versions.find((v) => Array.isArray(v.game_versions) && v.game_versions.includes(gameVersion));
  if (exact) return exact;
  const sameMajor = versions.find((v) => Array.isArray(v.game_versions) && v.game_versions.some((gv) => gv.startsWith('1.21')));
  return sameMajor || versions[0];
}

function getPrimaryFile(version) {
  if (!version || !Array.isArray(version.files)) return null;
  return version.files.find((f) => f.primary) || version.files[0];
}

async function ensureRequiredMods(cfg) {
  const modsDir = path.join(cfg.gameDir, 'mods');
  fs.mkdirSync(modsDir, { recursive: true });

  const markerPath = path.join(cfg.gameDir, MODS_MARKER);
  let marker = {};
  if (fs.existsSync(markerPath)) {
    try {
      marker = JSON.parse(fs.readFileSync(markerPath, 'utf8'));
    } catch {
      marker = {};
    }
  }

  const required = [
    { slug: 'fabric-api', name: 'Fabric API' },
    { slug: 'yacl', name: 'YetAnotherConfigLib' },
    { slug: 'owo-lib', name: 'owo-lib' },
    { slug: 'simple-voice-chat', name: 'Simple Voice Chat' }
  ];

  seedBundledRequiredMods(modsDir, required);

  for (const mod of required) {
    throwIfCancelled();
    const entry = getMarkerEntry(marker, mod.slug);
    const cachedEntryFile = entry && entry.file ? path.join(modsDir, entry.file) : '';
    const cachedFile = cachedEntryFile && fs.existsSync(cachedEntryFile) ? entry.file : '';
    const fallbackFile = cachedFile || findExistingModFile(modsDir, mod.slug);
    const index = required.indexOf(mod);
    const percent = Math.max(0, Math.min(100, Math.floor(((index + 1) / required.length) * 100)));
    sendProgress(percent, `Verifying mods (${index + 1}/${required.length})`);
    if (fallbackFile) {
      sendLog(`[Launcher] ${mod.name} verified locally (${fallbackFile}).\n`);
      if (!entry || !entry.file) {
        marker[mod.slug] = { id: entry?.id || '', file: fallbackFile };
      }
      continue;
    }
    // Try Modrinth download first
    sendLog(`[Launcher] ${mod.name} not found locally. Trying Modrinth...\n`);
    let downloaded = false;
    try {
      const versions = await requestModrinthVersions(mod.slug, cfg.minecraftVersion);
      const picked = pickModrinthVersion(versions, cfg.minecraftVersion);
      const file = getPrimaryFile(picked);
      if (file && file.url && file.filename) {
        const dest = path.join(modsDir, file.filename);
        sendLog(`[Launcher] Downloading ${mod.name} from Modrinth...\n`);
        await downloadFile(file.url, dest, (pct) => {
          sendProgress(percent, `Downloading ${mod.name} (${pct}%)`);
        });
        marker[mod.slug] = { id: picked?.id || '', file: file.filename };
        sendLog(`[Launcher] ${mod.name} downloaded from Modrinth.\n`);
        downloaded = true;
      }
    } catch (modrinthErr) {
      sendLog(`[Launcher] Modrinth download failed for ${mod.name}: ${modrinthErr.message}\n`);
    }
    if (!downloaded) {
      // Fallback to bundled mods
      const bundled = findBundledModFile(mod.slug);
      if (bundled) {
        const destName = path.basename(bundled);
        const dest = path.join(modsDir, destName);
        fs.copyFileSync(bundled, dest);
        marker[mod.slug] = { id: '', file: destName };
        sendLog(`[Launcher] ${mod.name} restored from bundled fallback.\n`);
        downloaded = true;
      }
    }
    if (!downloaded) {
      throw new Error(`Required mod missing: ${mod.name}. Could not download from Modrinth, and no bundled fallback found in launcher/mods/ or ${modsDir}`);
    }
  }

  fs.writeFileSync(markerPath, JSON.stringify(marker, null, 2), 'utf8');
}

async function fetchClientVersionFromApi(cfg) {
  const base = cfg.presenceApiUrl && cfg.presenceApiUrl.trim().length > 0 ? cfg.presenceApiUrl.trim() : '';
  if (!base) return;
  const url = base.replace(/\/+$/, '') + '/client/version';
  try {
    const data = await requestJson(url);
    if (!data || typeof data !== 'object') return;
    let changed = false;
    if (typeof data.version === 'string' && data.version.trim().length > 0) {
      cfg.clientVersion = data.version.trim();
      changed = true;
    }
    if (typeof data.url === 'string' && data.url.trim().length > 0) {
      cfg.clientZipUrl = data.url.trim();
      changed = true;
    }
    if (changed) writeConfig(cfg);
  } catch (err) {
    sendLog(`[Launcher] Client version API unavailable, using fallback URL.\n`);
  }
}

async function fetchLauncherVersionFromApi(cfg) {
  const base = cfg.presenceApiUrl && cfg.presenceApiUrl.trim().length > 0 ? cfg.presenceApiUrl.trim() : '';
  if (!base) return;
  const url = base.replace(/\/+$/, '') + '/launcher/version';
  try {
    const data = await requestJson(url);
    if (!data || typeof data !== 'object') return;
    let changed = false;
    if (typeof data.version === 'string' && data.version.trim().length > 0) {
      cfg.launcherUpdateUrl = data.url.trim();
      changed = true;
    }
    if (changed) writeConfig(cfg);
  } catch (err) {
    sendLog(`[Launcher] Launcher version API unavailable, using fallback URL.\n`);
  }
}

async function extractAssetsZip(cfg, assetId) {
  const assetRoot = path.join(cfg.gameDir, 'assets');
  const zipPath = path.join(cfg.gameDir, ASSETS_ZIP_NAME);
  const indexPath = path.join(assetRoot, 'indexes', `${assetId}.json`);
  if (!fs.existsSync(zipPath)) return;
  const objectsDir = path.join(assetRoot, 'objects');
  const needsExtract = !fs.existsSync(indexPath) || !fs.existsSync(objectsDir) || (fs.readdirSync(objectsDir).length === 0);
  if (!needsExtract) return;
  sendLog('[Launcher] Extracting assets cache...\n');
  sendProgress(25, 'Extracting assets cache');
  if (hasSplitAssetsParts(zipPath)) {
    await extractSplitZip(zipPath, assetRoot);
  } else {
    await extract(zipPath, { dir: assetRoot });
  }
  if (!fs.existsSync(indexPath)) {
    const fallbackIndex = path.join(assetRoot, 'indexes', `${cfg.minecraftVersion}.json`);
    if (fs.existsSync(fallbackIndex)) {
      fs.copyFileSync(fallbackIndex, indexPath);
      sendLog('[Launcher] Asset index aligned for Fabric.\n');
    }
  }
  sendLog('[Launcher] Assets extracted.\n');
}

async function ensureAssetsZipFromServer(cfg, assetId) {
  const url = cfg.assetsZipUrl && cfg.assetsZipUrl.trim().length > 0 ? cfg.assetsZipUrl.trim() : '';
  if (!url) return;
  const assetRoot = path.join(cfg.gameDir, 'assets');
  const objectsDir = path.join(assetRoot, 'objects');
  const zipPath = path.join(cfg.gameDir, ASSETS_ZIP_NAME);
  const indexPath = path.join(assetRoot, 'indexes', `${assetId}.json`);
  const needsAssets = !fs.existsSync(indexPath) || !fs.existsSync(objectsDir) || countAssetObjects(objectsDir) === 0;
  if (fs.existsSync(zipPath) && !needsAssets) return;

  sendLog('[Launcher] Downloading assets cache...\n');
  sendProgress(20, 'Downloading assets cache (zip)');
  await downloadFile(url, zipPath);
  throwIfCancelled();
  await downloadAssetsZipParts(url, zipPath);
  throwIfCancelled();
  await extractAssetsZip(cfg, assetId);
}

function pad2(value) {
  return String(value).padStart(2, '0');
}

function hasSplitAssetsParts(zipPath) {
  const baseName = path.basename(zipPath, '.zip');
  const partPath = path.join(path.dirname(zipPath), `${baseName}.z01`);
  return fs.existsSync(partPath);
}

async function downloadAssetsZipParts(zipUrl, zipPath) {
  if (!zipUrl.toLowerCase().endsWith('.zip')) return;
  const baseUrl = zipUrl.slice(0, -4);
  const baseName = path.basename(zipPath, '.zip');
  const dir = path.dirname(zipPath);

  const firstPart = path.join(dir, `${baseName}.z01`);
  if (!fs.existsSync(firstPart)) {
    sendLog('[Launcher] Downloading split assets parts...\n');
  }

  let hasParts = false;
  let downloadedCount = 0;
  let missingCount = 0;
  const existingParts = new Set();
  for (let i = 1; i <= 34; i += 1) {
    const suffix = `z${pad2(i)}`;
    const partPath = path.join(dir, `${baseName}.${suffix}`);
    if (fs.existsSync(partPath) && fs.statSync(partPath).size > 0) {
      existingParts.add(suffix);
      hasParts = true;
    }
  }
  for (let i = 1; i <= 34; i += 1) {
    const suffix = `z${pad2(i)}`;
    const partName = `${baseName}.${suffix}`;
    const partPath = path.join(dir, partName);
    if (fs.existsSync(partPath) && fs.statSync(partPath).size > 0) {
      continue;
    }
    const partUrl = `${baseUrl}.${suffix}`;
    try {
      sendProgress(20, `Downloading assets ${suffix}`);
      await downloadFile(partUrl, partPath);
      hasParts = true;
      downloadedCount += 1;
    } catch (err) {
      const msg = err && err.message ? err.message : String(err);
      if (msg.includes('404')) {
        missingCount += 1;
        if (!hasParts) {
          sendLog('[Launcher] No split assets parts found.\n');
          return;
        }
        throw new Error(`Missing assets part ${partName}`);
      }
      throw err;
    }
  }

  if (hasParts) {
    const existingCount = existingParts.size;
    const total = 34;
    const picked = existingCount + downloadedCount;
    if (picked >= total && missingCount === 0) {
      sendLog('[Launcher] All split assets parts ready.\n');
    } else {
      sendLog(`[Launcher] Split assets parts ready: ${picked}/${total}\n`);
    }
  }
}

async function extractSplitZip(zipPath, destDir) {
  if (!path7za) {
    throw new Error('7-Zip is required to extract split archives.');
  }
  await new Promise((resolve, reject) => {
    const child = spawn(path7za, ['x', '-y', `-o${destDir}`, zipPath], {
      windowsHide: true
    });
    child.on('error', reject);
    child.on('exit', (code) => {
      if (code === 0) resolve();
      else reject(new Error(`7-Zip extract failed (${code})`));
    });
  });
}

function countAssetObjects(objectsDir) {
  if (!fs.existsSync(objectsDir)) return 0;
  let total = 0;
  const buckets = fs.readdirSync(objectsDir);
  for (const bucket of buckets) {
    const bucketPath = path.join(objectsDir, bucket);
    if (!fs.statSync(bucketPath).isDirectory()) continue;
    total += fs.readdirSync(bucketPath).length;
  }
  return total;
}

function verifyRequiredFiles(cfg) {
  const missing = {
    java: false,
    vanilla: false,
    fabric: false,
    assets: false,
    client: false,
    mods: false
  };

  const javaOk = existsJava(cfg.javaPath) || existsJava('java');
  if (!javaOk) missing.java = true;

  const versionsDir = path.join(cfg.gameDir, 'versions', cfg.minecraftVersion);
  const jarPath = path.join(versionsDir, `${cfg.minecraftVersion}.jar`);
  const jsonPath = path.join(versionsDir, `${cfg.minecraftVersion}.json`);
  if (!fs.existsSync(jarPath) || !fs.existsSync(jsonPath)) missing.vanilla = true;

  const fabricId = `fabric-loader-${cfg.fabricLoader}-${cfg.minecraftVersion}`;
  const fabricDir = path.join(cfg.gameDir, 'versions', fabricId);
  const fabricJsonPath = path.join(fabricDir, `${fabricId}.json`);
  const fabricJarPath = path.join(fabricDir, `${fabricId}.jar`);
  if (!fs.existsSync(fabricJsonPath) || !fs.existsSync(fabricJarPath)) missing.fabric = true;

  const assetRoot = path.join(cfg.gameDir, 'assets');
  const objectsDir = path.join(assetRoot, 'objects');
  const indexPath = path.join(assetRoot, 'indexes', `${fabricId}.json`);
  const fallbackIndex = path.join(assetRoot, 'indexes', `${cfg.minecraftVersion}.json`);
  if (!fs.existsSync(indexPath) && !fs.existsSync(fallbackIndex)) missing.assets = true;
  if (!fs.existsSync(objectsDir) || countAssetObjects(objectsDir) === 0) missing.assets = true;

  const marker = path.join(cfg.gameDir, '.bladeclient-client-version');
  const current = fs.existsSync(marker) ? fs.readFileSync(marker, 'utf8').trim() : '';
  if (!current || current !== cfg.clientVersion) missing.client = true;
  if (cfg.clientZipUrl && cfg.clientZipUrl.trim().toLowerCase().endsWith('.jar')) {
    const clientJar = path.join(cfg.gameDir, 'mods', 'BladeClient.jar');
    if (!fs.existsSync(clientJar)) missing.client = true;
  }

  const modsDir = path.join(cfg.gameDir, 'mods');
  const required = [
    { slug: 'fabric-api' },
    { slug: 'yacl' },
    { slug: 'owo-lib' },
    { slug: 'simple-voice-chat' }
  ];
  let modsMissing = false;
  for (const mod of required) {
    const fallbackFile = findExistingModFile(modsDir, mod.slug);
    if (!fallbackFile) {
      modsMissing = true;
      break;
    }
  }
  if (modsMissing) missing.mods = true;

  return missing;
}
async function ensureAssetsIntegrity(cfg, assetId) {
  const assetRoot = path.join(cfg.gameDir, 'assets');
  const indexPath = path.join(assetRoot, 'indexes', `${assetId}.json`);
  const fallbackIndex = path.join(assetRoot, 'indexes', `${cfg.minecraftVersion}.json`);
  if (!fs.existsSync(indexPath) && fs.existsSync(fallbackIndex)) {
    fs.copyFileSync(fallbackIndex, indexPath);
    sendLog('[Launcher] Asset index aligned for Fabric.\n');
  }

  if (!fs.existsSync(indexPath)) return;
  const index = JSON.parse(fs.readFileSync(indexPath, 'utf8'));
  const expected = index.objects ? Object.keys(index.objects).length : 0;
  if (expected === 0) return;

  const objectsDir = path.join(assetRoot, 'objects');
  const actual = countAssetObjects(objectsDir);

  if (actual > 0 && actual < expected) {
    sendLog(`[Launcher] Assets incomplete (${actual}/${expected}). Rebuilding cache...\n`);
    fs.rmSync(objectsDir, { recursive: true, force: true });
    fs.rmSync(indexPath, { force: true });
    const zipPath = path.join(cfg.gameDir, ASSETS_ZIP_NAME);
    if (fs.existsSync(zipPath)) {
      await extractAssetsZip(cfg, assetId);
    }
  }
}

async function createAssetsZip(cfg, assetId) {
  const assetRoot = path.join(cfg.gameDir, 'assets');
  const zipPath = path.join(cfg.gameDir, ASSETS_ZIP_NAME);
  const indexPath = path.join(assetRoot, 'indexes', `${assetId}.json`);
  if (!fs.existsSync(indexPath)) return;
  if (fs.existsSync(zipPath)) return;

  sendLog('[Launcher] Packing assets to zip...\n');
  await new Promise((resolve, reject) => {
    const output = fs.createWriteStream(zipPath);
    const archive = archiver('zip', { zlib: { level: 9 } });
    output.on('close', resolve);
    output.on('error', reject);
    archive.on('error', reject);
    archive.pipe(output);
    archive.directory(assetRoot, false);
    archive.finalize();
  });
  sendLog('[Launcher] Assets zip created.\n');
}

async function ensureVanillaVersion(cfg) {
  const versionsDir = path.join(cfg.gameDir, 'versions', cfg.minecraftVersion);
  const jarPath = path.join(versionsDir, `${cfg.minecraftVersion}.jar`);
  const jsonPath = path.join(versionsDir, `${cfg.minecraftVersion}.json`);

  if (fs.existsSync(jarPath) && fs.existsSync(jsonPath)) {
    return { jarPath, jsonPath };
  }

  sendLog(`[Launcher] Downloading Minecraft ${cfg.minecraftVersion} base files...\n`);
  sendProgress(10, `Downloading Minecraft ${cfg.minecraftVersion}`);
  fs.mkdirSync(versionsDir, { recursive: true });

  const manifest = await requestJson('https://launchermeta.mojang.com/mc/game/version_manifest.json');
  throwIfCancelled();
  const entry = manifest.versions.find((v) => v.id === cfg.minecraftVersion);
  if (!entry) {
    throw new Error(`Version ${cfg.minecraftVersion} not found in manifest`);
  }

  const versionJson = await requestJson(entry.url);
  throwIfCancelled();
  fs.writeFileSync(jsonPath, JSON.stringify(versionJson, null, 2), 'utf8');

  if (versionJson.downloads && versionJson.downloads.client && versionJson.downloads.client.url) {
    await downloadFile(versionJson.downloads.client.url, jarPath);
    throwIfCancelled();
  } else {
    throw new Error(`Missing client download URL for ${cfg.minecraftVersion}`);
  }

  return { jarPath, jsonPath };
}

function mergeArguments(baseArgs, extraArgs) {
  if (!baseArgs && !extraArgs) return undefined;
  const result = { ...(baseArgs || {}) };
  if (extraArgs) {
    if (Array.isArray(extraArgs.game)) {
      result.game = [...(result.game || []), ...extraArgs.game];
    }
    if (Array.isArray(extraArgs.jvm)) {
      result.jvm = [...(result.jvm || []), ...extraArgs.jvm];
    }
  }
  return result;
}

function libraryParts(libName) {
  if (!libName) return { group: '', artifact: '', version: '', classifier: '' };
  const parts = libName.split(':');
  return {
    group: parts[0] || '',
    artifact: parts[1] || '',
    version: parts[2] || '',
    classifier: parts.slice(3).join(':') || ''
  };
}

function libraryKey(libName) {
  const parts = libraryParts(libName);
  if (!parts.group || !parts.artifact) return libName || '';
  return `${parts.group}:${parts.artifact}:${parts.classifier}`;
}

function hasDuplicateLibrariesByKey(libraries) {
  const seen = new Set();
  for (const lib of libraries || []) {
    if (!lib || !lib.name) continue;
    const key = libraryKey(lib.name);
    if (seen.has(key)) return true;
    seen.add(key);
  }
  return false;
}

function hasMissingLwjglBase(libraries) {
  const base = new Set();
  const withClassifier = new Set();
  for (const lib of libraries || []) {
    if (!lib || !lib.name) continue;
    const parts = libraryParts(lib.name);
    if (parts.group !== 'org.lwjgl' || !parts.artifact) continue;
    const ga = `${parts.group}:${parts.artifact}`;
    if (parts.classifier) {
      withClassifier.add(ga);
    } else {
      base.add(ga);
    }
  }
  for (const ga of withClassifier) {
    if (!base.has(ga)) return true;
  }
  return false;
}

async function ensureFabricVersion(cfg, baseJarPath, baseJsonPath) {
  const fabricId = `fabric-loader-${cfg.fabricLoader}-${cfg.minecraftVersion}`;
  const fabricDir = path.join(cfg.gameDir, 'versions', fabricId);
  const fabricJsonPath = path.join(fabricDir, `${fabricId}.json`);
  const fabricJarPath = path.join(fabricDir, `${fabricId}.jar`);

  fs.mkdirSync(fabricDir, { recursive: true });

  let shouldRewrite = !fs.existsSync(fabricJsonPath);
  if (!shouldRewrite) {
    try {
      const existing = JSON.parse(fs.readFileSync(fabricJsonPath, 'utf8'));
      if (hasDuplicateLibrariesByKey(existing.libraries) || hasMissingLwjglBase(existing.libraries)) {
        shouldRewrite = true;
      }
    } catch {
      shouldRewrite = true;
    }
  }

  if (shouldRewrite) {
    sendLog('[Launcher] Downloading Fabric loader profile...\n');
    sendProgress(15, 'Downloading Fabric loader');
    const profileUrl = `https://meta.fabricmc.net/v2/versions/loader/${cfg.minecraftVersion}/${cfg.fabricLoader}/profile/json`;
    const profile = await requestJson(profileUrl);
    throwIfCancelled();
    const baseJson = JSON.parse(fs.readFileSync(baseJsonPath, 'utf8'));
    const mergedMap = new Map();
    const addLib = (lib) => {
      if (!lib || !lib.name) return;
      const key = libraryKey(lib.name);
      if (!key) return;
      mergedMap.set(key, lib);
    };
    (baseJson.libraries || []).forEach(addLib);
    (profile.libraries || []).forEach(addLib);
    const mergedLibraries = Array.from(mergedMap.values());

    const merged = {
      ...baseJson,
      ...profile,
      id: fabricId,
      inheritsFrom: profile.inheritsFrom || cfg.minecraftVersion,
      mainClass: profile.mainClass || baseJson.mainClass,
      arguments: mergeArguments(baseJson.arguments, profile.arguments),
      libraries: mergedLibraries
    };
    fs.writeFileSync(fabricJsonPath, JSON.stringify(merged, null, 2), 'utf8');
  }

  if (!fs.existsSync(fabricJarPath)) {
    sendLog('[Launcher] Preparing Fabric version jar...\n');
    fs.copyFileSync(baseJarPath, fabricJarPath);
  }

  return { fabricId, fabricJsonPath, fabricJarPath };
}

function resolveAssetIndexPath(cfg, assetId) {
  const assetRoot = path.join(cfg.gameDir, 'assets');
  const primary = path.join(assetRoot, 'indexes', `${assetId}.json`);
  if (fs.existsSync(primary)) return primary;
  const fallback = path.join(assetRoot, 'indexes', `${cfg.minecraftVersion}.json`);
  if (fs.existsSync(fallback)) return fallback;
  return '';
}

function parseAssetIndex(indexPath) {
  try {
    const raw = fs.readFileSync(indexPath, 'utf8');
    const json = JSON.parse(raw);
    if (!json || !json.objects || typeof json.objects !== 'object') return null;
    return json.objects;
  } catch {
    return null;
  }
}

function getAssetIndexInfoFromBase(baseJsonPath) {
  try {
    const raw = fs.readFileSync(baseJsonPath, 'utf8');
    const json = JSON.parse(raw);
    if (!json || !json.assetIndex || !json.assetIndex.url) return null;
    return {
      id: json.assetIndex.id || '',
      url: json.assetIndex.url
    };
  } catch {
    return null;
  }
}

async function downloadAssetIndex(url, destPath) {
  if (!url) return;
  const json = await requestJson(url);
  fs.mkdirSync(path.dirname(destPath), { recursive: true });
  fs.writeFileSync(destPath, JSON.stringify(json, null, 2), 'utf8');
}

async function ensureAssetIndex(cfg, assetId, baseJsonPath) {
  const info = getAssetIndexInfoFromBase(baseJsonPath);
  if (!info) {
    sendLog('[Launcher] Asset index info missing from version json.\n');
    return;
  }

  const assetRoot = path.join(cfg.gameDir, 'assets');
  const indexDir = path.join(assetRoot, 'indexes');
  fs.mkdirSync(indexDir, { recursive: true });

  const baseId = info.id && info.id.trim().length > 0 ? info.id.trim() : cfg.minecraftVersion;
  const baseIndexPath = path.join(indexDir, `${baseId}.json`);
  const fabricIndexPath = path.join(indexDir, `${assetId}.json`);

  const baseValid = parseAssetIndex(baseIndexPath) !== null;
  const fabricValid = parseAssetIndex(fabricIndexPath) !== null;

  if (!baseValid && !fabricValid) {
    sendLog('[Launcher] Downloading assets index...\n');
    await downloadAssetIndex(info.url, baseIndexPath);
  }

  if (!parseAssetIndex(fabricIndexPath) && fs.existsSync(baseIndexPath)) {
    try {
      fs.copyFileSync(baseIndexPath, fabricIndexPath);
    } catch {}
  }
}


function prefillAssetsFromMinecraft(cfg, assetId) {
  const sourceRoot = path.join(app.getPath('appData'), '.minecraft', 'assets');
  const sourceObjects = path.join(sourceRoot, 'objects');
  const sourceIndex = path.join(sourceRoot, 'indexes', `${cfg.minecraftVersion}.json`);
  if (!fs.existsSync(sourceObjects) || !fs.existsSync(sourceIndex)) {
    return;
  }

  const targetRoot = path.join(cfg.gameDir, 'assets');
  const targetObjects = path.join(targetRoot, 'objects');
  const targetIndexes = path.join(targetRoot, 'indexes');
  fs.mkdirSync(targetObjects, { recursive: true });
  fs.mkdirSync(targetIndexes, { recursive: true });

  const targetIndex = path.join(targetIndexes, `${assetId}.json`);
  if (!fs.existsSync(targetIndex)) {
    try {
      fs.copyFileSync(sourceIndex, targetIndex);
    } catch {}
  }

  const indexPath = resolveAssetIndexPath(cfg, assetId);
  const objects = indexPath ? parseAssetIndex(indexPath) : null;
  if (!objects) return;

  let copied = 0;
  for (const entry of Object.values(objects)) {
    if (!entry || !entry.hash) continue;
    const hash = String(entry.hash);
    const sub = hash.substring(0, 2);
    const src = path.join(sourceObjects, sub, hash);
    const dstDir = path.join(targetObjects, sub);
    const dst = path.join(dstDir, hash);
    if (fs.existsSync(dst)) continue;
    if (!fs.existsSync(src)) continue;
    try {
      fs.mkdirSync(dstDir, { recursive: true });
      fs.copyFileSync(src, dst);
      copied += 1;
    } catch {}
  }

  if (copied > 0) {
    sendLog(`[Launcher] Prefilled ${copied} assets from .minecraft.\n`);
  }
}

async function verifyAssetsWithIndex(cfg, assetId, baseJsonPath) {
  const indexPath = resolveAssetIndexPath(cfg, assetId);
  if (!indexPath) {
    sendLog('[Launcher] Assets index missing, skip verify.\n');
    return;
  }

  const objects = parseAssetIndex(indexPath);
  if (!objects) {
    sendLog('[Launcher] Assets index invalid, re-downloading.\n');
    if (baseJsonPath) {
      await ensureAssetIndex(cfg, assetId, baseJsonPath);
    }
    const retryPath = resolveAssetIndexPath(cfg, assetId);
    const retryObjects = retryPath ? parseAssetIndex(retryPath) : null;
    if (!retryObjects) {
      sendLog('[Launcher] Assets index still invalid, skip verify.\n');
      return;
    }
    return await verifyAssetsWithIndex(cfg, assetId, null);
  }

  const assetRoot = path.join(cfg.gameDir, 'assets');
  const objectsDir = path.join(assetRoot, 'objects');
  if (!fs.existsSync(objectsDir)) {
    fs.mkdirSync(objectsDir, { recursive: true });
  }

  const expectedHashes = new Set();
  const expectedSizes = new Map();
  for (const entry of Object.values(objects)) {
    if (!entry || !entry.hash) continue;
    const hash = String(entry.hash);
    expectedHashes.add(hash);
    expectedSizes.set(hash, typeof entry.size === 'number' ? entry.size : null);
  }

  const missing = [];
  let removed = 0;
  const buckets = fs.existsSync(objectsDir) ? fs.readdirSync(objectsDir) : [];
  for (const bucket of buckets) {
    const bucketPath = path.join(objectsDir, bucket);
    if (!fs.statSync(bucketPath).isDirectory()) continue;
    const files = fs.readdirSync(bucketPath);
    for (const file of files) {
      const hash = file;
      const filePath = path.join(bucketPath, file);
      if (!expectedHashes.has(hash)) {
        fs.rmSync(filePath, { force: true });
        removed += 1;
        continue;
      }
      const size = expectedSizes.get(hash);
      if (typeof size === 'number') {
        const actual = fs.statSync(filePath).size;
        if (actual !== size) {
          fs.rmSync(filePath, { force: true });
          missing.push(hash);
          removed += 1;
        }
      }
    }
  }

  for (const hash of expectedHashes) {
    const filePath = path.join(objectsDir, hash.substring(0, 2), hash);
    if (!fs.existsSync(filePath)) missing.push(hash);
  }

  if (removed > 0) {
    sendLog(`[Launcher] Assets cleanup removed ${removed} extra files.\n`);
  }

  if (missing.length === 0) {
    sendLog('[Launcher] Assets verified (all present).\n');
    return;
  }

  sendLog(`[Launcher] Assets missing: ${missing.length}. Downloading...\n`);
  sendProgress(12, `Downloading ${missing.length} assets`);

  const concurrency = 8;
  let index = 0;
  const downloadOne = async (hash) => {
    const sub = hash.substring(0, 2);
    const url = `https://resources.download.minecraft.net/${sub}/${hash}`;
    const destDir = path.join(objectsDir, sub);
    fs.mkdirSync(destDir, { recursive: true });
    const dest = path.join(destDir, hash);
    await downloadFile(url, dest);
  };

  const workers = Array.from({ length: concurrency }, async () => {
    while (index < missing.length) {
      const i = index;
      index += 1;
      const hash = missing[i];
      try {
        await downloadOne(hash);
      } catch (err) {
        sendLog(`[Launcher] Asset download failed: ${hash} (${err.message})\n`);
        throw err;
      }
    }
  });

  await Promise.all(workers);
  sendLog('[Launcher] Assets download complete.\n');
}

async function ensureJava(cfg) {
  const requiredMajor = 21;
  const bundled = findJavaBinInDir(path.join(userData, 'jre'));
  if (bundled && existsJava(bundled) && isJavaCompatible(bundled, requiredMajor)) {
    cfg.javaPath = bundled;
    writeConfig(cfg);
    return resolveJavaForLaunch(bundled);
  }
  return installBundledJava(cfg);
}

async function installBundledJava(cfg) {
  const requiredMajor = 21;

  sendLog('[Launcher] Installing recommended Java (21)...\n');
  sendProgress(0, 'Preparing recommended Java install...');
  const jreDir = path.join(userData, 'jre');
  const existingJre = findJavaBinInDir(jreDir);
  if (existingJre && existsJava(existingJre) && isJavaCompatible(existingJre, requiredMajor)) {
    cfg.javaPath = existingJre;
    writeConfig(cfg);
    sendLog('[Launcher] Recommended Java already installed.\n');
    sendProgress(100, 'Recommended Java already installed');
    return resolveJavaForLaunch(existingJre);
  }

  fs.rmSync(jreDir, { recursive: true, force: true });
  fs.mkdirSync(jreDir, { recursive: true });
  const zipPath = path.join(userData, 'jre.zip');
  const url = await resolveRecommendedJavaDownloadUrl();
  sendProgress(1, 'Downloading recommended Java...');
  await downloadFile(url, zipPath, (percent) => {
    sendProgress(percent, `Downloading recommended Java... ${percent}%`);
  });
  sendProgress(96, 'Extracting Java...');
  await extract(zipPath, { dir: jreDir });
  fs.unlinkSync(zipPath);

  const jreBin = findJavaBinInDir(jreDir);
  if (!jreBin || !existsJava(jreBin) || !isJavaCompatible(jreBin, requiredMajor)) {
    throw new Error('Downloaded Java is invalid or incompatible');
  }
  cfg.javaPath = jreBin;
  writeConfig(cfg);
  sendLog('[Launcher] Recommended Java installed.\n');
  sendProgress(100, 'Recommended Java installed');
  return resolveJavaForLaunch(jreBin);
}

async function resolveRecommendedJavaDownloadUrl() {
  try {
    const payload = await withTimeout(
      requestJson(ZULU_METADATA_URL, { retries: 1, retryOn: [429, 502, 503], retryDelayMs: 1500 }),
      15000
    );
    const item = Array.isArray(payload) ? payload[0] : null;
    const url = item?.download_url || item?.url || '';
    if (typeof url === 'string' && /^https?:\/\//i.test(url)) {
      sendLog('[Launcher] Using Azul Zulu JRE 21 as recommended Java.\n');
      return url;
    }
  } catch (err) {
    sendLog(`[Launcher] Zulu lookup failed, using fallback Java source: ${err.message}\n`);
  }
  return ADOPTIUM_FALLBACK_URL;
}

async function ensureClientFiles(cfg) {
  await fetchClientVersionFromApi(cfg);
  if (!cfg.clientZipUrl || cfg.clientZipUrl.trim().length === 0) {
    sendLog('[Launcher] clientZipUrl not set. Skip client update.\n');
    return;
  }

  const gameDir = cfg.gameDir;
  const marker = path.join(gameDir, '.bladeclient-client-version');
  const url = cfg.clientZipUrl.trim();
  let current = '';
  if (fs.existsSync(marker)) {
    current = fs.readFileSync(marker, 'utf8').trim();
  }

  if (current === cfg.clientVersion) {
    return;
  }

  sendLog('[Launcher] Downloading client files...\n');
  sendProgress(40, 'Downloading client files');
  fs.mkdirSync(gameDir, { recursive: true });
  if (url.toLowerCase().endsWith('.jar')) {
    const modsDir = path.join(gameDir, 'mods');
    fs.mkdirSync(modsDir, { recursive: true });
    const jarPath = path.join(modsDir, 'BladeClient.jar');
    await downloadFile(url, jarPath);
    throwIfCancelled();
  } else {
    const zipPath = path.join(gameDir, 'client.zip');
    await downloadFile(url, zipPath);
    throwIfCancelled();
    await extract(zipPath, { dir: gameDir });
    fs.unlinkSync(zipPath);
  }
  fs.writeFileSync(marker, cfg.clientVersion, 'utf8');
  sendLog('[Launcher] Client updated.\n');
}

async function launchClient(cfg, launchRequest = null) {
  const gameDir = cfg.gameDir;
  fs.mkdirSync(gameDir, { recursive: true });

  if (cancelRequested) {
    cancelRequested = false;
    throw new Error('Launch cancelled');
  }

  sendProgress(2, 'Verifying files...');
  const missing = verifyRequiredFiles(cfg);
  const missingList = Object.entries(missing).filter(([, v]) => v).map(([k]) => k);
  if (missingList.length) {
    sendLog('[Launcher] Missing files: ' + missingList.join(', ') + '\n');
  } else {
    sendLog('[Launcher] All required files are present.\n');
  }
  throwIfCancelled();

  const javaPath = await ensureJava(cfg);
  const javaMajor = getJavaMajorVersion(javaPath);
  sendLog(`[Launcher] Using Java: ${javaPath} (major ${javaMajor || 'unknown'})\n`);
  if (javaMajor < 21) {
    throw new Error(`Selected Java is incompatible (found ${javaMajor || 'unknown'}, need 21+)`);
  }
  throwIfCancelled();

  if (cancelRequested) {
    cancelRequested = false;
    throw new Error('Launch cancelled');
  }

  const base = await ensureVanillaVersion(cfg);
  const fabric = await ensureFabricVersion(cfg, base.jarPath, base.jsonPath);
  await ensureAssetIndex(cfg, fabric.fabricId, base.jsonPath);
  patchMclcAssetsDownload();
  prefillAssetsFromMinecraft(cfg, fabric.fabricId);
  await verifyAssetsWithIndex(cfg, fabric.fabricId, base.jsonPath);
  await ensureClientFiles(cfg);
  await ensureRequiredMods(cfg);
  throwIfCancelled();

  applyBladeClientPresenceConfig(cfg);
  const launcher = new Client();
  const auth = await getLaunchAuthorization(cfg);

  logMojangSources();
  sendProgress(0, 'Preparing launch...');

  launcher.on('debug', (e) => {
    sendLog(`[MCLC] ${e}\n`);
    if (typeof e === 'string' && e.includes('Downloaded assets')) {
      createAssetsZip(cfg, fabric.fabricId).catch((err) => {
        sendLog(`[Launcher] Assets zip failed: ${err.message}\n`);
      });
    }
  });
  launcher.on('data', (e) => sendLog(`[MCLC] ${e}\n`));
  launcher.on('download-status', (e) => {
    if (!e || !e.name || !e.type) return;
    const label = `Downloading ${e.type}: ${e.name}`;
    sendProgress(progressState.percent, label);
    if (e.name !== lastDownloadName) {
      lastDownloadName = e.name;
      sendLog(`[Download] ${e.type}: ${e.name}\n`);
    }
  });
  launcher.on('progress', (e) => {
    if (!e || !e.total) return;
    const percent = Math.max(0, Math.min(100, Math.floor((e.task / e.total) * 100)));
    const labels = {
      assets: 'Downloading assets',
      'assets-copy': 'Preparing assets',
      natives: 'Downloading natives',
      classes: 'Downloading libraries',
      'classes-custom': 'Downloading libraries',
      'classes-maven-custom': 'Downloading libraries',
      log4j: 'Downloading log4j config',
      'version-jar': 'Downloading Minecraft client'
    };
    const label = labels[e.type] || `Processing ${e.type}`;
    sendProgress(percent, label);
  });
  launcher.on('close', (code) => {
    sendLog(`\n[Launcher] process exited: ${code}\n`);
    launching = false;
    currentProc = null;
    cancelRequested = false;
    sendState('idle');
    sendProgress(0, '');
    setLauncherPresenceStatus('launcher');
    if (cfg.postLaunchAction === 'tray' && mainWindow && !mainWindow.isDestroyed() && !isAppQuitting) {
      mainWindow.show();
      mainWindow.focus();
      sendLog('[Launcher] Launcher restored from tray.\n');
    }
  });

  const opts = {
    authorization: auth,
    javaPath,
    root: gameDir,
    version: {
      number: cfg.minecraftVersion,
      type: 'release',
      custom: fabric.fabricId
    },
    memory: {
      max: `${clampMemoryMb(cfg.memoryMb)}M`,
      min: '1024M'
    },
    window: {
      width: clampWindowWidth(cfg.windowWidth),
      height: clampWindowHeight(cfg.windowHeight),
      fullscreen: false
    },
    overrides: {
      executablePath: javaPath,
      versionJson: fabric.fabricJsonPath,
      minecraftJar: fabric.fabricJarPath
    },
    cache: path.join(gameDir, 'cache'),
    // Fabric handled by custom version json.
  };

  const directServer = typeof launchRequest?.serverAddress === 'string'
    ? launchRequest.serverAddress.trim()
    : '';
  if (directServer.length > 0) {
    opts.quickPlay = {
      type: 'multiplayer',
      identifier: directServer
    };
    sendLog(`[Launcher] QuickPlay server: ${directServer}\n`);
  }

  const sessionId = getSessionJvmArg();
  const customArgs = parseJavaArgs(cfg.javaArgs);
  customArgs.push(sessionId);
  if (localPresenceBaseUrl) {
    customArgs.push(`-Dbladeclient.launcher_url=${localPresenceBaseUrl}`);
  }
  opts.customArgs = customArgs;

  sendLog('[Launcher] Launching client...\n');
  currentProc = await launcher.launch(opts);
  if (currentProc) {
    if (cancelRequested || forceKillPending) {
      killProcessTree(currentProc);
      throw new Error('Launch cancelled');
    }
    sendProgress(100, 'Client started');
    createAssetsZip(cfg, fabric.fabricId).catch((err) => {
      sendLog(`[Launcher] Assets zip failed: ${err.message}\n`);
    });
    applyPostLaunchAction(cfg);
  }
}

ipcMain.handle('config:load', () => {
  const cfg = readConfig();
  const clampedMemory = clampMemoryMb(cfg.memoryMb);
  const clampedWidth = clampWindowWidth(cfg.windowWidth);
  const clampedHeight = clampWindowHeight(cfg.windowHeight);
  const fullscreen = !!cfg.launchFullscreen;
  if (clampedMemory !== cfg.memoryMb) {
    cfg.memoryMb = clampedMemory;
    writeConfig(cfg);
  }
  if (clampedWidth !== cfg.windowWidth || clampedHeight !== cfg.windowHeight || fullscreen !== cfg.launchFullscreen) {
    cfg.windowWidth = clampedWidth;
    cfg.windowHeight = clampedHeight;
    cfg.launchFullscreen = fullscreen;
    writeConfig(cfg);
  }
  return {
    ...cfg,
    maxMemoryMb: getMaxAllocatableMemoryMb()
  };
});
ipcMain.handle('config:save', (event, cfg) => {
  const current = readConfig();
  const merged = { ...current, ...(cfg || {}) };
  merged.memoryMb = clampMemoryMb(merged.memoryMb);
  merged.windowWidth = clampWindowWidth(merged.windowWidth);
  merged.windowHeight = clampWindowHeight(merged.windowHeight);
  merged.launchFullscreen = !!merged.launchFullscreen;
  if (typeof merged.presenceApiUrl !== 'string' || merged.presenceApiUrl.trim().length === 0) {
    merged.presenceApiUrl = current.presenceApiUrl;
  }
  writeConfig(merged);
  applyPresenceSettings(merged);
  return true;
});
ipcMain.handle('auth:status', () => {
  const cfg = readConfig();
  return {
    mode: cfg.authMode === 'premium' ? 'premium' : 'offline',
    premiumAccounts: (cfg.premiumAccounts || []).map(toPremiumView),
    activePremiumUuid: cfg.activePremiumUuid || '',
    offlineAccounts: cfg.offlineAccounts || [],
    activeOfflineName: cfg.activeOfflineName || ''
  };
});
ipcMain.handle('auth:premium-login', async () => {
  const cfg = readConfig();
  const result = await premiumLogin(cfg);
  const active = getActivePremium(cfg);
  sendLog(`[Launcher] Premium login successful: ${active?.username || 'Unknown'}\n`);
  applyBladeClientPresenceConfig(cfg);
  return result;
});
ipcMain.handle('auth:use-offline', () => {
  const cfg = readConfig();
  cfg.authMode = 'offline';
  writeConfig(cfg);
  applyBladeClientPresenceConfig(cfg);
  sendLog('[Launcher] Switched to offline launch mode.\n');
  return {
    mode: 'offline',
    premiumAccounts: (cfg.premiumAccounts || []).map(toPremiumView),
    activePremiumUuid: cfg.activePremiumUuid || '',
    offlineAccounts: cfg.offlineAccounts || [],
    activeOfflineName: cfg.activeOfflineName || ''
  };
});
ipcMain.handle('auth:add-offline', (event, username) => {
  const cfg = readConfig();
  const name = sanitizeOfflineName(username);
  if (!name) throw new Error('Invalid offline name');
  cfg.offlineAccounts = Array.isArray(cfg.offlineAccounts) ? cfg.offlineAccounts : [];
  if (!cfg.offlineAccounts.includes(name)) {
    cfg.offlineAccounts.push(name);
  }
  cfg.activeOfflineName = name;
  cfg.authMode = 'offline';
  writeConfig(cfg);
  applyBladeClientPresenceConfig(cfg);
  return {
    mode: 'offline',
    premiumAccounts: (cfg.premiumAccounts || []).map(toPremiumView),
    activePremiumUuid: cfg.activePremiumUuid || '',
    offlineAccounts: cfg.offlineAccounts || [],
    activeOfflineName: cfg.activeOfflineName
  };
});
ipcMain.handle('auth:select-offline', (event, username) => {
  const cfg = readConfig();
  const name = sanitizeOfflineName(username);
  const list = Array.isArray(cfg.offlineAccounts) ? cfg.offlineAccounts : [];
  if (!name || !list.includes(name)) throw new Error('Offline account not found');
  cfg.activeOfflineName = name;
  cfg.authMode = 'offline';
  writeConfig(cfg);
  applyBladeClientPresenceConfig(cfg);
  return {
    mode: 'offline',
    premiumAccounts: (cfg.premiumAccounts || []).map(toPremiumView),
    activePremiumUuid: cfg.activePremiumUuid || '',
    offlineAccounts: cfg.offlineAccounts || [],
    activeOfflineName: cfg.activeOfflineName || ''
  };
});
ipcMain.handle('auth:remove-offline', (event, username) => {
  const cfg = readConfig();
  const name = sanitizeOfflineName(username);
  cfg.offlineAccounts = (cfg.offlineAccounts || []).filter((n) => n !== name);
  if (cfg.activeOfflineName === name) {
    cfg.activeOfflineName = cfg.offlineAccounts[0] || '';
  }
  writeConfig(cfg);
  applyBladeClientPresenceConfig(cfg);
  return {
    mode: cfg.authMode === 'premium' ? 'premium' : 'offline',
    premiumAccounts: (cfg.premiumAccounts || []).map(toPremiumView),
    activePremiumUuid: cfg.activePremiumUuid || '',
    offlineAccounts: cfg.offlineAccounts || [],
    activeOfflineName: cfg.activeOfflineName || ''
  };
});

ipcMain.handle('avatar:resolve', async (event, url, key) => {
  try {
    const rawUrl = typeof url === 'string' ? url.trim() : '';
    if (!rawUrl) return '';
    if (!rawUrl.startsWith('http://') && !rawUrl.startsWith('https://')) return '';
    const cacheDir = path.join(app.getPath('userData'), 'avatar-cache');
    if (!fs.existsSync(cacheDir)) fs.mkdirSync(cacheDir, { recursive: true });
    const hash = crypto.createHash('sha256').update(rawUrl).digest('hex').slice(0, 24);
    const safeKey = typeof key === 'string' && key.trim().length > 0 ? key.trim().replace(/[^a-zA-Z0-9_-]/g, '_') : 'avatar';
    const fileName = `${safeKey}-${hash}.png`;
    const filePath = path.join(cacheDir, fileName);
    if (!fs.existsSync(filePath)) {
      const buf = await new Promise((resolve, reject) => {
        const client = rawUrl.startsWith('https://') ? https : http;
        const req = client.get(rawUrl, (res) => {
          if (res.statusCode !== 200) {
            res.resume();
            reject(new Error(`HTTP ${res.statusCode}`));
            return;
          }
          const chunks = [];
          res.on('data', (d) => chunks.push(d));
          res.on('end', () => resolve(Buffer.concat(chunks)));
        });
        req.on('error', reject);
        req.setTimeout(10000, () => {
          req.destroy(new Error('timeout'));
        });
      });
      fs.writeFileSync(filePath, buf);
    }
    const data = fs.readFileSync(filePath);
    return `data:image/png;base64,${data.toString('base64')}`;
  } catch {
    return '';
  }
});
ipcMain.handle('auth:select-premium', (event, uuid) => {
  const cfg = readConfig();
  if (!(cfg.premiumAccounts || []).some((a) => a && a.uuid === uuid)) throw new Error('Premium account not found');
  cfg.activePremiumUuid = uuid;
  cfg.authMode = 'premium';
  writeConfig(cfg);
  applyBladeClientPresenceConfig(cfg);
  return {
    mode: 'premium',
    premiumAccounts: (cfg.premiumAccounts || []).map(toPremiumView),
    activePremiumUuid: cfg.activePremiumUuid,
    offlineAccounts: cfg.offlineAccounts || [],
    activeOfflineName: cfg.activeOfflineName || ''
  };
});
ipcMain.handle('auth:remove-premium', (event, uuid) => {
  const cfg = readConfig();
  cfg.premiumAccounts = (cfg.premiumAccounts || []).filter((a) => a && a.uuid !== uuid);
  if (cfg.premiumAccounts.length === 0) {
    cfg.activePremiumUuid = '';
    cfg.authMode = 'offline';
  } else if (!cfg.premiumAccounts.some((a) => a.uuid === cfg.activePremiumUuid)) {
    cfg.activePremiumUuid = cfg.premiumAccounts[0].uuid;
  }
  writeConfig(cfg);
  applyBladeClientPresenceConfig(cfg);
  return {
    mode: cfg.authMode === 'premium' ? 'premium' : 'offline',
    premiumAccounts: (cfg.premiumAccounts || []).map(toPremiumView),
    activePremiumUuid: cfg.activePremiumUuid || '',
    offlineAccounts: cfg.offlineAccounts || [],
    activeOfflineName: cfg.activeOfflineName || ''
  };
});
ipcMain.handle('java:status', () => getJavaStatus());
ipcMain.handle('java:list', () => listJavaChoices());
ipcMain.handle('java:install-bundled', async () => {
  const cfg = readConfig();
  await installBundledJava(cfg);
  return getJavaStatus();
});
ipcMain.handle('java:select', (event, javaPath) => {
  const selected = String(javaPath || '').trim();
  if (selected === '__recommended_java__') {
    const cfg = readConfig();
    return installBundledJava(cfg).then(() => getJavaStatus());
  }
  throw new Error('Manual Java selection is disabled. Use Launcher Recommended Java.');
});
ipcMain.handle('java:use-system', () => {
  throw new Error('System Java is disabled. Use Launcher Recommended Java.');
});
ipcMain.handle('java:use-bundled', () => {
  const cfg = readConfig();
  const bundled = findJavaBinInDir(path.join(userData, 'jre'));
  if (!bundled || !existsJava(bundled)) throw new Error('Bundled Java not found');
  cfg.javaPath = bundled;
  writeConfig(cfg);
  return getJavaStatus();
});
ipcMain.handle('launcher:downloads-pause', () => {
  setDownloadsPaused(true);
  return { paused: true };
});
ipcMain.handle('launcher:downloads-resume', () => {
  setDownloadsPaused(false);
  return { paused: false };
});
ipcMain.handle('servers:list', () => {
  const cfg = readConfig();
  return readServersFromDat(cfg);
});
ipcMain.handle('app:version', () => app.getVersion());

ipcMain.handle('launcher:launch', async (event, launchRequest) => {
  if (launching) return false;
  forceKillPending = false;
  launching = true;
  const cfg = readConfig();
  try {
    sendState('running');
    setLauncherPresenceStatus('client');
    await launchClient(cfg, launchRequest || null);
    return true;
  } catch (err) {
    launching = false;
    currentProc = null;
    cancelRequested = false;
    sendState('idle');
    sendProgress(0, '');
    sendLog(`[Launcher] Error: ${err.message}\n`);
    setLauncherPresenceStatus('launcher');
    return false;
  }
});

ipcMain.handle('launcher:cancel', async () => {
  if (!launching) return false;
  cancelRequested = true;
  forceKillPending = true;
  sendLog('[Launcher] Cancel requested.\n');
  cancelActiveNetwork();
  if (currentProc && currentProc.pid) {
    killProcessTree(currentProc);
    sendLog('[Launcher] Launch process killed.\n');
  }
  launching = false;
  currentProc = null;
  sendState('idle');
  sendProgress(0, '');
  setLauncherPresenceStatus('launcher');
  return true;
});

app.whenReady().then(() => {
  createSplashWindow();
  createWindow();
  applyPresenceSettings(readConfig());
});

app.on('before-quit', () => {
  isAppQuitting = true;
  stopLauncherPresence();
  if (localPresenceServer) {
    try { localPresenceServer.close(); } catch {}
    localPresenceServer = null;
  }
  if (tray) {
    try { tray.destroy(); } catch {}
    tray = null;
  }
  if (splashWindow) {
    try { splashWindow.destroy(); } catch {}
    splashWindow = null;
  }
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) createWindow();
});

// Window controls
ipcMain.on('window-minimize', (event) => {
  const win = BrowserWindow.fromWebContents(event.sender);
  if (win) win.minimize();
});

ipcMain.on('window-maximize', (event) => {
  const win = BrowserWindow.fromWebContents(event.sender);
  if (win) {
    if (win.isMaximized()) {
      win.unmaximize();
    } else {
      win.maximize();
    }
  }
});

ipcMain.on('window-close', (event) => {
  const win = BrowserWindow.fromWebContents(event.sender);
  if (win) win.close();
});

// Microsoft authentication via launcher bridge
ipcMain.handle('microsoft-auth', async () => {
  try {
    const { Auth, Xbox } = require('msmc');
    if (!Auth || !Xbox) throw new Error('msmc not available');

    const xboxManager = new Auth('bladeclient-launcher', '00000000-0000-0000-0000-000000000000');
    const token = await xboxManager.launch('raw');
    if (!token || !token.data) throw new Error('Microsoft auth failed');

    const minecraft = await token.getMinecraft();
    if (!minecraft || !minecraft.data) throw new Error('Minecraft auth failed');

    return {
      ok: true,
      username: minecraft.data.username || '',
      uuid: minecraft.data.uuid || '',
      accessToken: minecraft.data.access_token || ''
    };
  } catch (err) {
    console.error('Microsoft auth failed:', err.message);
    return { ok: false };
  }
});

// Desktop notifications
ipcMain.handle('notify:show', (_, { title, body }) => {
  try {
    if (Notification.isSupported()) {
      const n = new Notification({
        title: String(title || 'BladeClient'),
        body: String(body || ''),
        silent: true,
        icon: path.join(__dirname, '..', 'assets', 'icon.png')
      });
      n.show();
      n.on('click', () => {
        const win = BrowserWindow.getAllWindows().find(w => !w.isDestroyed());
        if (win) { win.show(); win.focus(); }
      });
    }
  } catch {}
});

// Error reporting
const ERROR_LOG_PATH = path.join(app.getPath('userData'), 'error-log.json');

ipcMain.handle('log:flush', async (_, batch) => {
  try {
    const existing = [];
    try {
      if (fs.existsSync(ERROR_LOG_PATH)) {
        const raw = fs.readFileSync(ERROR_LOG_PATH, 'utf8');
        existing.push(...JSON.parse(raw));
      }
    } catch {}
    existing.push(...batch.map(e => ({ ...e, ts: e.ts || Date.now() })));
    // Keep last 1000 entries
    if (existing.length > 1000) existing.splice(0, existing.length - 1000);
    fs.writeFileSync(ERROR_LOG_PATH, JSON.stringify(existing, null, 2));

    // Send to server in background
    const serverUrl = getConfig()?.serverUrl || 'https://blade.runflare.run';
    const errorPayload = batch.filter(e => e.level === 'error' || e.level === 'warn').slice(0, 20);
    if (errorPayload.length > 0) {
      fetch(`${serverUrl}/api/error-report`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ errors: errorPayload }),
        signal: AbortSignal.timeout(5000)
      }).catch(() => {});
    }
  } catch {}
  return { ok: true };
});

ipcMain.on('ui:ready', () => {
  rendererReady = true;
  tryShowMainWindow();
});

