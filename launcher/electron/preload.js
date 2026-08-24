const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electron', {
  ipcRenderer: {
    send: (channel, ...args) => ipcRenderer.send(channel, ...args),
    on: (channel, handler) => {
      const listener = (_event, ...args) => handler(...args);
      ipcRenderer.on(channel, listener);
      return () => ipcRenderer.removeListener(channel, listener);
    },
    invoke: (channel, ...args) => ipcRenderer.invoke(channel, ...args)
  }
});

contextBridge.exposeInMainWorld('bladeLauncher', {
  uiReady: () => ipcRenderer.send('ui:ready'),
  loadConfig: () => ipcRenderer.invoke('config:load'),
  saveConfig: (cfg) => ipcRenderer.invoke('config:save', cfg),
  launch: (launchRequest) => ipcRenderer.invoke('launcher:launch', launchRequest),
  cancel: () => ipcRenderer.invoke('launcher:cancel'),
  getAuthStatus: () => ipcRenderer.invoke('auth:status'),
  premiumLogin: () => ipcRenderer.invoke('auth:premium-login'),
  useOfflineAuth: () => ipcRenderer.invoke('auth:use-offline'),
  addOfflineAccount: (username) => ipcRenderer.invoke('auth:add-offline', username),
  selectOfflineAccount: (username) => ipcRenderer.invoke('auth:select-offline', username),
  removeOfflineAccount: (username) => ipcRenderer.invoke('auth:remove-offline', username),
  selectPremiumAccount: (uuid) => ipcRenderer.invoke('auth:select-premium', uuid),
  removePremiumAccount: (uuid) => ipcRenderer.invoke('auth:remove-premium', uuid),
  getJavaStatus: () => ipcRenderer.invoke('java:status'),
  listJavaPaths: () => ipcRenderer.invoke('java:list'),
  selectJavaPath: (javaPath) => ipcRenderer.invoke('java:select', javaPath),
  installBundledJava: () => ipcRenderer.invoke('java:install-bundled'),
  useSystemJava: () => ipcRenderer.invoke('java:use-system'),
  useBundledJava: () => ipcRenderer.invoke('java:use-bundled'),
  pauseDownloads: () => ipcRenderer.invoke('launcher:downloads-pause'),
  resumeDownloads: () => ipcRenderer.invoke('launcher:downloads-resume'),
  getServersList: () => ipcRenderer.invoke('servers:list'),
  getAppVersion: () => ipcRenderer.invoke('app:version'),
  resolveAvatar: (url, key) => ipcRenderer.invoke('avatar:resolve', url, key),
  onLog: (handler) => ipcRenderer.on('launcher:log', (_, msg) => handler(msg)),
  onState: (handler) => ipcRenderer.on('launcher:state', (_, state) => handler(state)),
  onProgress: (handler) => ipcRenderer.on('launcher:progress', (_, payload) => handler(payload)),
  onUpdate: (handler) => ipcRenderer.on('launcher:update', (_, payload) => handler(payload)),
  onDownloadsState: (handler) => ipcRenderer.on('launcher:downloads-state', (_, payload) => handler(payload)),
  sendNotification: (title, body) => ipcRenderer.invoke('notify:show', { title, body })
});

contextBridge.exposeInMainWorld('bladeLog', {
  flush: (batch) => ipcRenderer.invoke('log:flush', batch)
});
