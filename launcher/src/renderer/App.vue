<template>
  <div class="shell" :class="themeClass">
    <div class="titlebar">
      <img class="titlebar-icon" src="/icon.png" alt="BladeClient" />
      <div class="titlebar-center">
        <div class="titlebar-main">BladeClient</div>
        <div class="titlebar-sub">Launcher v{{ appVersion }}</div>
      </div>
    </div>
    <div v-if="updateVisible" class="update-overlay">
      <div class="update-card">
        <div class="update-title">Updating Launcher</div>
        <div class="update-label">{{ updateLabel }}</div>
        <div class="update-percent" v-if="updatePercentVisible">{{ updatePercent }}%</div>
        <div class="update-track">
          <div v-if="updatePercentVisible" class="update-fill" :style="{ width: updatePercent + '%' }"></div>
          <div v-else class="update-fill update-fill--indeterminate"></div>
        </div>
      </div>
    </div>
    <div v-if="showOnboarding" class="update-overlay">
      <div class="update-card">
        <div class="update-title">Welcome To BladeClient Launcher</div>
        <div class="update-label">First setup: Name, Java, Theme, Memory</div>
        <div class="setting-row">
          <label>Player Name</label>
          <input class="text-input" v-model="onboardingName" maxlength="16" placeholder="Your username" />
        </div>
        <div class="setting-row">
          <label>Java</label>
          <select class="text-input" v-model="onboardingJavaPath">
            <option :value="RECOMMENDED_JAVA_TOKEN">Launcher Recommended Java</option>
          </select>
          <div class="auth-actions">
            <button v-if="!javaStatus.bundledVersion" class="auth-btn auth-btn--offline" @click="installBundledJava" :disabled="javaInstallBusy">
              {{ javaInstallBusy ? 'Installing...' : 'Install Recommended Java' }}
            </button>
            <div v-else class="setting-value">Recommended Java is already installed.</div>
          </div>
          <div class="setting-value" v-if="onboardingJavaNote">{{ onboardingJavaNote }}</div>
          <div v-if="javaInstallBusy" class="progress onboarding-progress">
            <div class="progress-label">{{ progressLabel || 'Installing Java...' }}</div>
            <div class="progress-track">
              <div class="progress-fill" :style="{ width: progress + '%' }"></div>
            </div>
            <div class="setting-value">{{ progress }}%</div>
          </div>
        </div>
        <div class="setting-row">
          <label>Theme</label>
          <select class="text-input" v-model="config.themePreset">
            <option value="dark">Dark</option>
            <option value="glass">Glass</option>
            <option value="minimal">Minimal</option>
          </select>
        </div>
        <div class="setting-row">
          <label>Memory</label>
          <input class="range" type="range" min="2048" :max="memoryMaxMb" step="256" v-model.number="config.memoryMb" />
          <div class="setting-value">{{ config.memoryMb }} MB</div>
        </div>
        <div class="setting-value onboarding-error" v-if="onboardingError">{{ onboardingError }}</div>
        <button class="launch" @click="finishOnboarding" :disabled="!onboardingName.trim() || onboardingBusy">
          {{ onboardingBusy ? 'Applying...' : 'Continue' }}
        </button>
      </div>
    </div>
    <div class="panel">
      <div class="brand">
        <div class="logo" />
        <div>
          <h1>BladeClient</h1>
          <p class="client-version">v{{ clientVersion }}</p>
          <p>Official Launcher</p>
        </div>
        <div class="tabs">
          <button class="tab-btn" :class="currentTab === 'play' ? 'tab-btn--active' : ''" @click="currentTab = 'play'">Play</button>
          <button class="tab-btn" :class="currentTab === 'settings' ? 'tab-btn--active' : ''" @click="currentTab = 'settings'">Settings</button>
        </div>
      </div>

      <div class="presence-inline" v-if="presenceUrl">
        <span v-if="!presenceError" class="presence-dot presence-dot--ok" />
        <span v-else class="presence-dot presence-dot--bad" />
        <span v-if="!presenceError">{{ presenceTotal }} Online</span>
        <span v-else>Server offline</span>
        <span v-if="!config.richPresenceEnabled" class="presence-note">Rich Presence Off</span>
      </div>

      <template v-if="currentTab === 'play'">
        <button class="launch" @click="handleClick" :disabled="busy && !canCancel">
          {{ busy ? 'Cancel Launch' : 'Launch BladeClient' }}
        </button>

        <div v-if="progressVisible" class="progress">
          <div class="progress-label">{{ progressLabel || 'Preparing downloads...' }}</div>
          <div class="progress-track">
            <div class="progress-fill" :style="{ width: progress + '%' }"></div>
          </div>
          <div class="progress-footer">
            <div class="progress-dot" :style="{ '--progress': progress }"></div>
            <div class="progress-footer__text">
              <span v-if="progressSpeed">{{ progressSpeed }}/s</span>
              <span v-if="progressBytes">• {{ progressBytes }}</span>
            </div>
          </div>
        </div>

        <div class="server-strip-wrap" v-if="playServers.length">
          <div class="server-strip-title">Saved Servers</div>
          <div class="server-strip">
            <button v-for="srv in playServers" :key="srv.address" class="server-card" @click="launchServer(srv.address)">
              <div class="server-ip">{{ srv.address }}</div>
              <div class="server-icon-wrap">
                <img class="server-icon" :src="srv.icon || '/icon.png'" :alt="srv.name" />
              </div>
            </button>
          </div>
        </div>

        <div class="log" ref="logEl">{{ log }}</div>
        <div class="auth-actions">
          <button class="auth-btn auth-btn--offline" @click="copyLogs">Copy Logs</button>
        </div>
      </template>

      <template v-else>
        <div class="settings">
          <div class="settings-title">Settings</div>
          <div class="setting-row">
            <label>Memory</label>
            <input class="range" type="range" min="2048" :max="memoryMaxMb" step="256" v-model.number="config.memoryMb" />
            <div class="setting-value">{{ config.memoryMb }} MB / Max {{ memoryMaxMb }} MB</div>
          </div>
          <div class="setting-row">
            <label>Java Args</label>
            <input class="text-input" type="text" v-model="config.javaArgs" placeholder="-XX:+UseG1GC -Dfile.encoding=UTF-8" />
          </div>
          <div class="setting-row">
            <label>Theme Preset</label>
            <select class="text-input" v-model="config.themePreset">
              <option value="dark">Dark</option>
              <option value="glass">Glass</option>
              <option value="minimal">Minimal</option>
            </select>
          </div>
          <div class="setting-row">
            <label>Game Resolution</label>
            <div class="resolution-row">
              <input class="number-input" type="number" min="640" max="7680" step="1" v-model.number="config.windowWidth" />
              <span>x</span>
              <input class="number-input" type="number" min="360" max="4320" step="1" v-model.number="config.windowHeight" />
              <label class="fullscreen-inline">
                <span>Fullscreen</span>
                <input class="fullscreen-check" type="checkbox" v-model="config.launchFullscreen" />
              </label>
            </div>
          </div>
          <div class="setting-row">
            <label>After Game Launch</label>
            <select class="text-input" v-model="config.postLaunchAction">
              <option value="none">Do nothing</option>
              <option value="tray">Minimize to tray</option>
              <option value="close">Close launcher</option>
            </select>
          </div>
          <div class="setting-row">
            <label>Java Manager</label>
            <div class="setting-value">Mode: Launcher Recommended Java only</div>
            <div class="setting-value">Bundled: {{ javaStatus.bundledVersion ? ('Java ' + javaStatus.bundledVersion) : 'Not installed' }}</div>
            <button v-if="!javaStatus.bundledVersion" class="auth-btn auth-btn--offline" @click="installBundledJava" :disabled="javaInstallBusy">
              {{ javaInstallBusy ? 'Installing...' : 'Install Launcher Java' }}
            </button>
            <div v-else class="setting-value">Launcher Java is already installed.</div>
            <div v-if="javaInstallBusy" class="progress onboarding-progress">
              <div class="progress-label">{{ progressLabel || 'Installing Java...' }}</div>
              <div class="progress-track">
                <div class="progress-fill" :style="{ width: progress + '%' }"></div>
              </div>
              <div class="setting-value">{{ progress }}%</div>
            </div>
            <div class="setting-value" v-if="javaSettingNote">{{ javaSettingNote }}</div>
          </div>
          <div class="setting-row">
            <label>Discord Rich Presence</label>
            <button class="toggle" :class="config.richPresenceEnabled ? 'toggle--on' : 'toggle--off'" @click="togglePresence">
              {{ config.richPresenceEnabled ? 'On' : 'Off' }}
            </button>
          </div>
          <div class="setting-row">
            <label>Logs</label>
            <div class="auth-actions">
              <button class="auth-btn auth-btn--offline" @click="showLogs = true">Open Logs</button>
              <button class="auth-btn auth-btn--offline" @click="copyLogs">Copy Logs</button>
            </div>
          </div>
        </div>
      </template>

    </div>
    <div v-if="showLogs" class="update-overlay">
      <div class="update-card logs-card">
        <div class="update-title">Launcher Logs</div>
        <div class="update-label" v-if="latestError">Last Error: {{ latestError }}</div>
        <div class="auth-actions">
          <button class="auth-btn auth-btn--offline" @click="logFilter='all'">All</button>
          <button class="auth-btn auth-btn--offline" @click="logFilter='warn'">Warn</button>
          <button class="auth-btn auth-btn--offline" @click="logFilter='error'">Error</button>
          <button class="auth-btn auth-btn--offline" @click="copyLastError" :disabled="!latestError">Copy Error</button>
          <button class="auth-btn auth-btn--offline" @click="copyLogs">Copy Logs</button>
          <button class="auth-btn auth-btn--premium" @click="showLogs=false">Close</button>
        </div>
        <div class="log log-modal">{{ filteredLog }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, nextTick, computed, watch } from 'vue';

const log = ref('');
const logEl = ref(null);
const busy = ref(false);
const canCancel = ref(false);
const progress = ref(0);
const progressLabel = ref('');
const progressSpeedBps = ref(0);
const progressDownloaded = ref(0);
const progressTotal = ref(0);
const progressVisible = computed(() => busy.value);
const clientVersion = ref('0.0.0');
const appVersion = ref('0.0.0');
const DEFAULT_PRESENCE_URL = 'https://blade.runflare.run';
const presenceUrl = ref('');
const presenceTotal = ref(0);
const presenceVersions = ref({});
const presenceError = ref(false);
const saveTimer = ref(null);
const currentTab = ref('play');
const updateState = ref({ status: 'checking', label: 'Checking for updates...' });
const memoryMaxMb = ref(8192);
const showOnboarding = ref(false);
const showLogs = ref(false);
const logFilter = ref('all');
const playServers = ref([]);
const onboardingName = ref('');
const onboardingJavaPath = ref('__recommended_java__');
const onboardingBusy = ref(false);
const onboardingError = ref('');
const onboardingJavaNote = ref('');
const javaInstallBusy = ref(false);
const javaSettingNote = ref('');
const RECOMMENDED_JAVA_TOKEN = '__recommended_java__';
const latestError = ref('');
const javaStatus = ref({
  configuredPath: '',
  configuredVersion: 0,
  systemPath: '',
  systemVersion: 0,
  bundledPath: '',
  bundledVersion: 0,
  requiredMajor: 21
});
const config = ref({
  memoryMb: 4096,
  windowWidth: 1280,
  windowHeight: 720,
  launchFullscreen: false,
  themePreset: 'dark',
  onboardingDone: false,
  quickServers: [],
  selectedQuickServer: '',
  autoJoinQuickServer: false,
  postLaunchAction: 'tray',
  javaArgs: '',
  presenceApiUrl: '',
  richPresenceEnabled: true
});

function appendLog(msg) {
  log.value += msg;
  nextTick(() => {
    if (logEl.value) {
      logEl.value.scrollTop = logEl.value.scrollHeight;
    }
  });
}

function formatSpeed(bps) {
  if (!bps || bps <= 0) return '';
  const units = ['B', 'KB', 'MB', 'GB'];
  let value = bps;
  let idx = 0;
  while (value >= 1024 && idx < units.length - 1) {
    value /= 1024;
    idx += 1;
  }
  return `${value.toFixed(value >= 10 || idx === 0 ? 0 : 1)} ${units[idx]}`;
}

const progressSpeed = computed(() => formatSpeed(progressSpeedBps.value));

function formatBytes(bytes) {
  if (!bytes || bytes <= 0) return '';
  const units = ['B', 'KB', 'MB', 'GB'];
  let value = bytes;
  let idx = 0;
  while (value >= 1024 && idx < units.length - 1) {
    value /= 1024;
    idx += 1;
  }
  return `${value.toFixed(value >= 10 || idx === 0 ? 0 : 1)} ${units[idx]}`;
}

const progressBytes = computed(() => {
  if (!progressDownloaded.value) return '';
  if (progressTotal.value && progressTotal.value > 0) {
    return `${formatBytes(progressDownloaded.value)} / ${formatBytes(progressTotal.value)}`;
  }
  return formatBytes(progressDownloaded.value);
});

async function launch(launchRequest = null) {
  if (!window.bladeLauncher || busy.value) return;
  busy.value = true;
  canCancel.value = true;
  appendLog('\n[Launcher] Starting...\n');
  await window.bladeLauncher.launch(launchRequest || undefined);
}

async function cancelLaunch() {
  if (!window.bladeLauncher || !busy.value) return;
  canCancel.value = false;
  appendLog('[Launcher] Cancel requested...\n');
  await window.bladeLauncher.cancel();
}

function handleClick() {
  if (busy.value) {
    cancelLaunch();
  } else {
    launch();
  }
}

function launchServer(address) {
  if (!address || busy.value) return;
  launch({ serverAddress: String(address).trim() });
}

async function refreshJavaStatus() {
  if (!window.bladeLauncher?.getJavaStatus) return;
  try {
    javaStatus.value = await window.bladeLauncher.getJavaStatus();
  } catch {}
}

async function refreshJavaChoices() {
  onboardingJavaPath.value = RECOMMENDED_JAVA_TOKEN;
}

async function installBundledJava() {
  if (!window.bladeLauncher?.installBundledJava) return;
  onboardingJavaNote.value = '';
  javaInstallBusy.value = true;
  progress.value = 0;
  progressLabel.value = 'Preparing Java install...';
  try {
    javaStatus.value = await window.bladeLauncher.installBundledJava();
    await refreshJavaChoices();
    onboardingJavaPath.value = RECOMMENDED_JAVA_TOKEN;
    onboardingJavaNote.value = 'Recommended Java installed and selected.';
    javaSettingNote.value = 'Recommended Java installed and selected.';
  } catch (err) {
    onboardingJavaNote.value = `Java install failed: ${err?.message || err}`;
    javaSettingNote.value = onboardingJavaNote.value;
    appendLog(`[Launcher] Java install failed: ${err?.message || err}\n`);
  } finally {
    javaInstallBusy.value = false;
  }
}

async function finishOnboarding() {
  onboardingError.value = '';
  if (!(onboardingJavaPath.value || '').trim()) {
    onboardingError.value = 'Java selection is required.';
    return;
  }
  if (!onboardingName.value.trim()) {
    onboardingError.value = 'Name is required.';
    return;
  }

  onboardingBusy.value = true;
  try {
    await window.bladeLauncher.selectJavaPath?.(onboardingJavaPath.value);
    config.value.onboardingDone = true;
    const onboardingPayload = JSON.parse(JSON.stringify({ ...config.value, onboardingDone: true }));
    await window.bladeLauncher.saveConfig?.(onboardingPayload);
    showOnboarding.value = false;
    await refreshJavaStatus();
  } catch (err) {
    onboardingError.value = err?.message || 'Setup failed.';
  } finally {
    onboardingBusy.value = false;
  }
}

async function copyToClipboard(text) {
  const value = String(text || '');
  if (!value) return;
  try {
    await navigator.clipboard.writeText(value);
    return true;
  } catch {
    try {
      const ta = document.createElement('textarea');
      ta.value = value;
      document.body.appendChild(ta);
      ta.select();
      document.execCommand('copy');
      document.body.removeChild(ta);
      return true;
    } catch {
      return false;
    }
  }
}

async function copyLastError() {
  if (!latestError.value) return;
  const ok = await copyToClipboard(latestError.value);
  if (ok) appendLog('[Launcher] Error copied to clipboard.\n');
}

async function copyLogs() {
  const ok = await copyToClipboard(log.value);
  if (ok) appendLog('[Launcher] Logs copied to clipboard.\n');
}

async function refreshServersList() {
  if (!window.bladeLauncher?.getServersList) return;
  try {
    const list = await window.bladeLauncher.getServersList();
    playServers.value = Array.isArray(list) ? list.slice(0, 5) : [];
  } catch {
    playServers.value = [];
  }
}

function scheduleSave() {
  if (!window.bladeLauncher?.saveConfig) return;
  if (saveTimer.value) clearTimeout(saveTimer.value);
  saveTimer.value = setTimeout(() => {
    const payload = JSON.parse(JSON.stringify(config.value));
    window.bladeLauncher.saveConfig(payload);
  }, 400);
}

function togglePresence() {
  config.value.richPresenceEnabled = !config.value.richPresenceEnabled;
}

const topVersionLabel = computed(() => {
  const versions = presenceVersions.value || {};
  const entries = Object.entries(versions);
  if (entries.length === 0) return '';
  entries.sort((a, b) => b[1] - a[1]);
  const [ver, count] = entries[0];
  return `${ver} (${count})`;
});

const updateVisible = computed(() => updateState.value.status !== 'ready');
const updateLabel = computed(() => updateState.value.label || 'Checking for updates...');
const updatePercent = computed(() => {
  const p = Number(updateState.value?.percent ?? 0);
  return Math.max(0, Math.min(100, Math.floor(p)));
});
const updatePercentVisible = computed(() => {
  return updateState.value?.status === 'downloading';
});
const themeClass = computed(() => `theme-${config.value.themePreset || 'dark'}`);
const filteredLog = computed(() => {
  const content = log.value || '';
  if (logFilter.value === 'all') return content;
  const lines = content.split('\n');
  const token = logFilter.value === 'warn' ? 'warn' : 'error';
  return lines.filter((l) => l.toLowerCase().includes(token)).join('\n');
});

onMounted(() => {
  if (window.bladeLauncher) {
    window.bladeLauncher.uiReady?.();
    window.bladeLauncher.loadConfig?.().then((cfg) => {
      if (cfg && cfg.clientVersion) clientVersion.value = cfg.clientVersion;
      if (cfg) {
        memoryMaxMb.value = Number.isFinite(cfg.maxMemoryMb) ? cfg.maxMemoryMb : 8192;
        config.value = {
          memoryMb: Math.min(cfg.memoryMb ?? 4096, memoryMaxMb.value),
          windowWidth: cfg.windowWidth ?? 1280,
          windowHeight: cfg.windowHeight ?? 720,
          launchFullscreen: !!cfg.launchFullscreen,
          themePreset: cfg.themePreset || 'dark',
          onboardingDone: !!cfg.onboardingDone,
          quickServers: Array.isArray(cfg.quickServers) ? cfg.quickServers : [],
          selectedQuickServer: cfg.selectedQuickServer || '',
          autoJoinQuickServer: !!cfg.autoJoinQuickServer,
          postLaunchAction: ['none', 'tray', 'close'].includes(cfg.postLaunchAction) ? cfg.postLaunchAction : 'tray',
          javaArgs: typeof cfg.javaArgs === 'string' ? cfg.javaArgs : '',
          presenceApiUrl: cfg.presenceApiUrl ?? '',
          richPresenceEnabled: typeof cfg.richPresenceEnabled === 'boolean' ? cfg.richPresenceEnabled : true
        };
        showOnboarding.value = !config.value.onboardingDone;
        onboardingName.value = '';
        onboardingJavaPath.value = RECOMMENDED_JAVA_TOKEN;
        presenceUrl.value = (config.value.presenceApiUrl || DEFAULT_PRESENCE_URL).trim();
      }
      if (presenceUrl.value) {
        fetchPresence();
        fetchClientVersion();
        setInterval(fetchPresence, 15000);
        setInterval(fetchClientVersion, 30000);
      }
    });
    window.bladeLauncher.getAppVersion?.().then((ver) => {
      if (ver) appVersion.value = ver;
    });
    refreshServersList();
    setInterval(refreshServersList, 15000);
    window.bladeLauncher.onLog((msg) => {
      appendLog(msg);
      const text = String(msg || '').trim();
      const isLauncherError = /\[Launcher\]\s+Error:/i.test(text);
      const isUserCancel = /launch cancelled/i.test(text) || /cancel requested/i.test(text);
      if (isLauncherError && !isUserCancel) {
        latestError.value = text;
        showLogs.value = true;
      }
    });
    window.bladeLauncher.onState((state) => {
      busy.value = state === 'running';
      if (!busy.value) {
        progress.value = 0;
        progressLabel.value = '';
        progressSpeedBps.value = 0;
        progressDownloaded.value = 0;
        progressTotal.value = 0;
        canCancel.value = false;
      }
    });
    window.bladeLauncher.onProgress((payload) => {
      if (!payload) return;
      if (typeof payload.percent === 'number') progress.value = payload.percent;
      if (typeof payload.label === 'string') progressLabel.value = payload.label;
      if (typeof payload.speedBps === 'number') progressSpeedBps.value = payload.speedBps;
      if (typeof payload.downloadedBytes === 'number') progressDownloaded.value = payload.downloadedBytes;
      if (typeof payload.totalBytes === 'number') progressTotal.value = payload.totalBytes;
    });
    window.bladeLauncher.onUpdate((payload) => {
      if (!payload) return;
      updateState.value = { ...updateState.value, ...payload };
    });
    refreshJavaStatus();
    refreshJavaChoices();
    setInterval(refreshJavaChoices, 10000);
  }
});

watch(
  config,
  (next) => {
    const nextUrl = typeof next.presenceApiUrl === 'string' ? next.presenceApiUrl.trim() : '';
    if (nextUrl.length > 0) {
      presenceUrl.value = nextUrl;
    }
    scheduleSave();
  },
  { deep: true }
);

async function fetchPresence() {
  if (!presenceUrl.value) return;
  try {
    const res = await fetch(`${presenceUrl.value}/presence/stats`);
    if (!res.ok) throw new Error('bad status');
    const data = await res.json();
    presenceTotal.value = data.total || 0;
    presenceVersions.value = data.versions || {};
    presenceError.value = false;
  } catch {
    presenceError.value = true;
  }
}

async function fetchClientVersion() {
  if (!presenceUrl.value) return;
  try {
    const res = await fetch(`${presenceUrl.value}/client/version`);
    if (!res.ok) throw new Error('bad status');
    const data = await res.json();
    if (data && typeof data.version === 'string' && data.version.trim().length > 0) {
      clientVersion.value = data.version.trim();
    }
  } catch {}
}
</script>

<style scoped>
.shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
  position: relative;
  background: radial-gradient(circle at top, rgba(74, 163, 255, 0.2), transparent 45%),
    linear-gradient(180deg, #090b10 0%, #0b0d12 100%);
  color: #e8edf5;
}

.theme-glass {
  background: radial-gradient(circle at top, rgba(74, 163, 255, 0.28), transparent 45%),
    linear-gradient(180deg, #0a1118 0%, #0b0f14 100%);
}

.theme-glass .panel {
  background: rgba(12, 18, 28, 0.64);
  border-color: rgba(124, 192, 255, 0.2);
}

.theme-minimal {
  background: #0b0d12;
}

.theme-minimal .panel {
  background: rgba(10, 12, 16, 0.96);
  border-color: rgba(255, 255, 255, 0.05);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.45);
}

.panel {
  scrollbar-width: thin;
}

.panel::-webkit-scrollbar { width: 10px; }
.panel::-webkit-scrollbar-track { border-radius: 999px; }
.panel::-webkit-scrollbar-thumb { border-radius: 999px; }

.theme-dark .panel { scrollbar-color: rgba(124, 192, 255, 0.62) rgba(12, 18, 30, 0.74); }
.theme-dark .panel::-webkit-scrollbar-track { background: rgba(12, 18, 30, 0.74); }
.theme-dark .panel::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, rgba(124, 192, 255, 0.88), rgba(74, 163, 255, 0.72));
  border: 2px solid rgba(8, 12, 22, 0.8);
}
.theme-dark .panel::-webkit-scrollbar-thumb:hover { background: linear-gradient(180deg, rgba(154, 208, 255, 0.95), rgba(96, 175, 255, 0.82)); }

.theme-glass .panel { scrollbar-color: rgba(148, 214, 255, 0.75) rgba(18, 28, 42, 0.55); }
.theme-glass .panel::-webkit-scrollbar-track { background: rgba(18, 28, 42, 0.55); }
.theme-glass .panel::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, rgba(190, 232, 255, 0.85), rgba(128, 196, 255, 0.72));
  border: 2px solid rgba(10, 16, 28, 0.55);
}
.theme-glass .panel::-webkit-scrollbar-thumb:hover { background: linear-gradient(180deg, rgba(213, 241, 255, 0.92), rgba(152, 208, 255, 0.78)); }

.theme-minimal .panel { scrollbar-color: rgba(165, 172, 184, 0.72) rgba(22, 24, 28, 0.88); }
.theme-minimal .panel::-webkit-scrollbar-track { background: rgba(22, 24, 28, 0.88); }
.theme-minimal .panel::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, rgba(178, 184, 195, 0.86), rgba(141, 148, 161, 0.72));
  border: 2px solid rgba(12, 13, 16, 0.8);
}
.theme-minimal .panel::-webkit-scrollbar-thumb:hover { background: linear-gradient(180deg, rgba(192, 198, 208, 0.92), rgba(156, 163, 176, 0.82)); }

.titlebar {
  position: fixed; top: 0; left: 0; right: 0; height: 34px;
  -webkit-app-region: drag; z-index: 200;
  display: flex; align-items: center; padding: 0 10px;
}

.titlebar-icon { width: 18px; height: 18px; object-fit: contain; }

.titlebar-center {
  position: absolute; left: 50%; transform: translateX(-50%);
  display: grid; justify-items: center; line-height: 1.05;
}

.titlebar-main { font-size: 11px; color: rgba(232, 237, 245, 0.92); }
.titlebar-sub { font-size: 10px; color: rgba(232, 237, 245, 0.62); }

.update-overlay {
  position: fixed; inset: 0;
  background: rgba(5, 7, 12, 0.85);
  display: grid; place-items: center; z-index: 100;
  backdrop-filter: blur(6px);
}

.update-card {
  background: rgba(13, 17, 26, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px; padding: 26px 28px;
  width: min(520px, 88vw); text-align: center;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.45);
}

.update-title { font-size: 15px; font-weight: 600; margin-bottom: 8px; }
.update-label { font-size: 12px; color: rgba(255, 255, 255, 0.7); }
.onboarding-error { color: #ff7a7a; }
.onboarding-progress { margin-top: 6px; }
.logs-card { width: min(900px, 88vw); }
.update-percent { margin-top: 8px; font-size: 12px; color: rgba(255, 255, 255, 0.85); }

.update-track {
  margin-top: 8px; width: 100%; height: 8px;
  border-radius: 999px; background: rgba(255, 255, 255, 0.14);
  overflow: hidden;
}

.update-fill {
  height: 100%; border-radius: 999px;
  background: linear-gradient(90deg, #4aa3ff, #7cc0ff);
  transition: width 0.15s linear;
}

.update-fill--indeterminate { width: 35%; animation: update-indeterminate 1.1s ease-in-out infinite; }

@keyframes update-indeterminate {
  0% { transform: translateX(-120%); }
  100% { transform: translateX(310%); }
}

.panel {
  width: min(640px, 90vw);
  max-height: calc(100vh - 40px);
  overflow: auto;
  background: rgba(13, 17, 26, 0.86);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 22px; padding: 32px;
  box-shadow: 0 30px 80px rgba(4, 8, 16, 0.6);
  display: grid; gap: 22px; position: relative;
}

.panel { scrollbar-width: thin; }

.brand { display: flex; align-items: center; gap: 16px; }

.logo {
  width: 56px; height: 56px; border-radius: 16px;
  background: url('/logo.png') center/cover no-repeat,
    linear-gradient(145deg, rgba(74, 163, 255, 0.7), rgba(74, 163, 255, 0.2));
  box-shadow: inset 0 0 20px rgba(74, 163, 255, 0.5);
}

h1 { font-size: 24px; margin: 0; }
p { color: rgba(255, 255, 255, 0.65); margin: 4px 0 0; }
.client-version { color: rgba(124, 192, 255, 0.85); font-size: 13px; }

.presence-inline {
  position: absolute; right: 20px; top: 18px;
  display: flex; gap: 8px; align-items: center;
  font-size: 12px; color: rgba(255, 255, 255, 0.75);
}

.presence-note { color: rgba(255, 255, 255, 0.5); }

.presence-dot { width: 10px; height: 10px; border-radius: 999px; display: inline-block; }
.presence-dot--ok { background: #35d27f; box-shadow: 0 0 12px rgba(53, 210, 127, 0.7); }
.presence-dot--bad { background: #ff4d4d; box-shadow: 0 0 10px rgba(255, 77, 77, 0.55); }

.tabs { margin-left: auto; display: flex; gap: 14px; align-items: center; }

.tab-btn {
  background: transparent; border: none; color: rgba(255, 255, 255, 0.7);
  font-size: 13px; cursor: pointer; padding: 6px 12px;
  border-radius: 999px; transition: 0.2s ease;
}

.tab-btn:hover { color: #e8edf5; background: rgba(255, 255, 255, 0.08); }
.tab-btn--active { color: #e8edf5; }

.launch {
  border: none; border-radius: 14px; padding: 14px 18px;
  font-size: 16px; font-weight: 600; color: #081018;
  background: linear-gradient(135deg, #4aa3ff, #7cc0ff);
  cursor: pointer; transition: 0.2s ease;
}

.launch:disabled { opacity: 0.6; cursor: not-allowed; }
.launch:not(:disabled):hover { transform: translateY(-1px); box-shadow: 0 14px 32px rgba(74, 163, 255, 0.4); }

.auth-actions { display: flex; gap: 8px; }

.auth-btn {
  border: none; border-radius: 10px; padding: 8px 12px;
  font-size: 12px; font-weight: 600; cursor: pointer;
}

.auth-btn:disabled { opacity: 0.55; cursor: not-allowed; }

.auth-btn--premium { color: #081018; background: linear-gradient(135deg, #4aa3ff, #7cc0ff); }
.auth-btn--offline { color: #e8edf5; background: rgba(255, 255, 255, 0.1); }

.progress { display: grid; gap: 12px; }
.progress-label { font-size: 13px; color: rgba(255, 255, 255, 0.75); }

.progress-track {
  position: relative; height: 10px; border-radius: 999px;
  background: rgba(255, 255, 255, 0.1); overflow: hidden;
}

.progress-fill {
  height: 100%; border-radius: 999px;
  background: linear-gradient(90deg, #4aa3ff, #7cc0ff);
  transition: width 0.2s ease;
}

.progress-footer {
  display: flex; align-items: center; gap: 8px;
  font-size: 12px; color: rgba(255, 255, 255, 0.55);
}

.progress-dot {
  width: 14px; height: 14px; border-radius: 50%;
  background: conic-gradient(#4aa3ff calc(var(--progress) * 1%), rgba(255, 255, 255, 0.15) 0);
  position: relative;
}

.progress-dot::after {
  content: ''; position: absolute; inset: 3px;
  border-radius: 50%; background: rgba(13, 17, 26, 0.95);
}

.progress-footer__text { display: flex; gap: 8px; }

.settings {
  display: grid; gap: 12px; padding: 14px;
  border-radius: 14px; background: rgba(8, 10, 16, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.settings-title { font-weight: 600; font-size: 14px; color: rgba(255, 255, 255, 0.8); }
.setting-row { display: grid; gap: 10px; }
.setting-row label { font-size: 12px; color: rgba(255, 255, 255, 0.6); }

.resolution-row { display: flex; align-items: center; gap: 8px; }
.resolution-row span { color: rgba(255, 255, 255, 0.65); font-size: 12px; }

.fullscreen-inline {
  margin-left: auto; display: inline-flex; align-items: center;
  gap: 8px; font-size: 12px; color: rgba(255, 255, 255, 0.75);
}

.fullscreen-check { width: 16px; height: 16px; accent-color: #4aa3ff; cursor: pointer; }
.range { width: 100%; }

.setting-value { font-size: 12px; color: rgba(255, 255, 255, 0.75); }
.setting-value--danger { color: rgba(255, 90, 90, 0.95); }

.text-input, .number-input {
  background: rgba(15, 18, 26, 0.9); color: #e8edf5;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px; padding: 8px 10px; font-size: 12px;
}

.text-input:focus, .number-input:focus {
  outline: none; border-color: rgba(124, 192, 255, 0.7);
  box-shadow: 0 0 0 2px rgba(74, 163, 255, 0.2);
}

.toggle {
  width: 90px; border-radius: 999px; padding: 6px 10px;
  border: none; font-size: 12px; font-weight: 600;
  cursor: pointer; justify-self: start;
}

.toggle--on { background: rgba(53, 210, 127, 0.9); color: #0b1a12; box-shadow: 0 6px 14px rgba(53, 210, 127, 0.35); }
.toggle--off { background: rgba(255, 77, 77, 0.9); color: #1b0b0b; box-shadow: 0 6px 14px rgba(255, 77, 77, 0.35); }

.log {
  background: rgba(8, 10, 16, 0.9); border-radius: 14px;
  padding: 14px; height: 150px; overflow: auto;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px; color: #9fb6cf;
  border: 1px solid rgba(255, 255, 255, 0.05);
  white-space: pre-wrap;
}

.log-modal { height: 360px; }

.server-strip-wrap { display: grid; gap: 8px; }
.server-strip-title { font-size: 12px; color: rgba(255, 255, 255, 0.72); }
.server-strip { display: flex; gap: 10px; flex-wrap: wrap; justify-content: center; }

.server-card {
  width: 56px; height: 56px; border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 14px; background: rgba(0, 0, 0, 0.28);
  padding: 0; position: relative; overflow: visible; cursor: pointer;
}

.server-icon-wrap {
  position: absolute; inset: 0; border-radius: 14px;
  overflow: hidden; z-index: 1;
}

.server-ip {
  position: absolute; left: 50%; transform: translateX(-50%); top: -24px;
  background: rgba(0, 0, 0, 0.72); border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 8px; font-size: 9px; color: #e8edf5;
  white-space: nowrap; padding: 2px 5px; opacity: 0;
  pointer-events: none; transition: opacity 0.16s ease; z-index: 2;
}

.server-icon { width: 100%; height: 100%; object-fit: cover; transition: transform 0.16s ease; }
.server-card:hover .server-icon { transform: scale(1.28); }
.server-card:hover .server-ip { opacity: 1; }
</style>
