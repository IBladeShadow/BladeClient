export {}

interface ElectronAPI {
  ipcRenderer: {
    send(channel: string, ...args: unknown[]): void
    on(channel: string, handler: (...args: unknown[]) => void): () => void
    invoke(channel: string, ...args: unknown[]): Promise<unknown>
  }
}

interface BladeLauncherAPI {
  uiReady: () => void
  loadConfig: () => Promise<unknown>
  saveConfig: (cfg: unknown) => Promise<void>
  launch: (launchRequest?: unknown) => Promise<void>
  cancel: () => Promise<void>
  getAuthStatus: () => Promise<unknown>
  premiumLogin: () => Promise<unknown>
  useOfflineAuth: () => Promise<unknown>
  addOfflineAccount: (username: string) => Promise<void>
  selectOfflineAccount: (username: string) => Promise<void>
  removeOfflineAccount: (username: string) => Promise<void>
  selectPremiumAccount: (uuid: string) => Promise<void>
  removePremiumAccount: (uuid: string) => Promise<void>
  getJavaStatus: () => Promise<unknown>
  listJavaPaths: () => Promise<string[]>
  selectJavaPath: (javaPath: string) => Promise<void>
  installBundledJava: () => Promise<void>
  useSystemJava: () => Promise<void>
  useBundledJava: () => Promise<void>
  pauseDownloads: () => Promise<void>
  resumeDownloads: () => Promise<void>
  getServersList: () => Promise<unknown[]>
  getAppVersion: () => Promise<string>
  resolveAvatar: (url: string, key: string) => Promise<string | null>
  onLog: (handler: (msg: string) => void) => void
  onState: (handler: (state: unknown) => void) => void
  onProgress: (handler: (payload: unknown) => void) => void
  onUpdate: (handler: (payload: unknown) => void) => void
  onDownloadsState: (handler: (payload: unknown) => void) => void
}

declare global {
  interface Window {
    electron: ElectronAPI
    bladeLauncher: BladeLauncherAPI
  }
}
