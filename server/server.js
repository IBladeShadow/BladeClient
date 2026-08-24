import 'dotenv/config';
import express from 'express';
import cors from 'cors';
import fs from 'fs';
import path from 'path';
import crypto from 'crypto';
import bcrypt from 'bcryptjs';

const app = express();
app.use(cors());
app.use(express.json({ limit: '64kb' }));

const PUBLIC_DIR = path.join(process.cwd(), 'public');
app.use('/public', express.static(PUBLIC_DIR, { fallthrough: true }));
app.use(express.static(PUBLIC_DIR, { fallthrough: true }));

const PORT = process.env.PORT ? Number(process.env.PORT) : 5000;
const TTL_MS = 30 * 1000;
const CLEANUP_MS = 15 * 1000;
const CLIENT_VERSION_PATH = path.join(process.cwd(), 'client-version.json');
const LAUNCHER_VERSION_PATH = path.join(process.cwd(), 'launcher-version.json');
const CHAT_TTL_MS = 30 * 60 * 1000;
const CHAT_MAX = 500;
const FRIENDS_PATH = path.join(process.cwd(), 'friends.json');
const ACCOUNTS_PATH = path.join(process.cwd(), 'accounts_v2.json');
const SESSIONS_PATH = path.join(process.cwd(), 'sessions.json');
const SESSION_TTL_MS = 30 * 24 * 60 * 60 * 1000;
const GEO_CACHE_TTL_MS = 6 * 60 * 60 * 1000;

// Map<serverId, Map<uuid, { uuid, name, server, version, ts }>>
const presence = new Map();
// Map<key, Array<{ from, to, message, ts }>>
const chat = new Map();
// { users: { [username]: string[] }, requests: { [username]: string[] } }
let friendsStore = { users: {}, requests: {} };
// { users: { [username]: { username, passHash, premiumUuid, premiumName, gameName, createdAt } } }
let accountsStore = { users: {} };
// Map<token, { username, ts }>
const sessions = new Map();
// Map<ip, { country, ts }>
const geoCache = new Map();

function loadFriendsStore() {
  try {
    if (!fs.existsSync(FRIENDS_PATH)) return { users: {}, requests: {} };
    const raw = fs.readFileSync(FRIENDS_PATH, 'utf8');
    const json = JSON.parse(raw);
    if (!json || typeof json !== 'object' || !json.users) return { users: {}, requests: {} };
    if (!json.requests || typeof json.requests !== 'object') json.requests = {};
    return json;
  } catch {
    return { users: {}, requests: {} };
  }
}

function saveFriendsStore() {
  try {
    fs.writeFileSync(FRIENDS_PATH, JSON.stringify(friendsStore, null, 2), 'utf8');
  } catch {}
}

friendsStore = loadFriendsStore();
accountsStore = loadAccountsStore();
hydrateSessions();

function now() { return Date.now(); }

function getServerMap(server) {
  let map = presence.get(server);
  if (!map) {
    map = new Map();
    presence.set(server, map);
  }
  return map;
}

function normalizeServer(raw) {
  if (!raw || typeof raw !== 'string') return 'unknown';
  return raw.trim().toLowerCase().slice(0, 128) || 'unknown';
}

function normalizeName(raw) {
  if (!raw || typeof raw !== 'string') return '';
  return raw.trim().slice(0, 32);
}

function normalizeUuid(raw) {
  if (!raw || typeof raw !== 'string') return '';
  return raw.trim().toLowerCase();
}

function normalizeVersion(raw) {
  if (!raw || typeof raw !== 'string') return 'unknown';
  const cleaned = raw.trim().slice(0, 32);
  return cleaned || 'unknown';
}

function normalizeMessage(raw) {
  if (!raw || typeof raw !== 'string') return '';
  return raw.trim().slice(0, 500);
}

function normalizeAccountName(raw) {
  if (!raw || typeof raw !== 'string') return '';
  return raw.trim().toLowerCase().slice(0, 24);
}

function getRequestIp(req) {
  const xfwd = String(req.headers['x-forwarded-for'] || '').split(',')[0].trim();
  const rip = String(req.ip || req.socket?.remoteAddress || '').trim();
  const raw = xfwd || rip;
  if (!raw) return '';
  if (raw.startsWith('::ffff:')) return raw.slice(7);
  return raw;
}

function isPrivateIp(ip) {
  if (!ip) return true;
  if (ip === '127.0.0.1' || ip === '::1') return true;
  if (ip.startsWith('10.') || ip.startsWith('192.168.')) return true;
  if (ip.startsWith('172.')) {
    const parts = ip.split('.');
    const second = Number(parts[1] || 0);
    if (second >= 16 && second <= 31) return true;
  }
  return false;
}

async function resolveCountryByIp(ip) {
  if (!ip || isPrivateIp(ip)) return '';
  const cached = geoCache.get(ip);
  if (cached && (now() - cached.ts) < GEO_CACHE_TTL_MS) return cached.country || '';
  try {
    const ctl = new AbortController();
    const timer = setTimeout(() => ctl.abort(), 4000);
    const res = await fetch(`https://ipwho.is/${encodeURIComponent(ip)}?fields=success,country_code`, { signal: ctl.signal });
    clearTimeout(timer);
    if (!res.ok) return '';
    const data = await res.json();
    if (!data?.success) return '';
    const cc = String(data.country_code || '').toUpperCase().slice(0, 2);
    geoCache.set(ip, { country: cc, ts: now() });
    return cc;
  } catch {
    return '';
  }
}

function loadAccountsStore() {
  try {
    if (!fs.existsSync(ACCOUNTS_PATH)) return { users: {} };
    const raw = fs.readFileSync(ACCOUNTS_PATH, 'utf8');
    const json = JSON.parse(raw);
    if (!json || typeof json !== 'object' || !json.users) return { users: {} };
    return json;
  } catch {
    return { users: {} };
  }
}

function saveAccountsStore() {
  try {
    fs.writeFileSync(ACCOUNTS_PATH, JSON.stringify(accountsStore, null, 2), 'utf8');
  } catch {}
}

function loadSessionsStore() {
  try {
    if (!fs.existsSync(SESSIONS_PATH)) return {};
    const raw = fs.readFileSync(SESSIONS_PATH, 'utf8');
    const json = JSON.parse(raw);
    if (!json || typeof json !== 'object') return {};
    return json;
  } catch {
    return {};
  }
}

function saveSessionsStore() {
  try {
    const out = {};
    for (const [token, rec] of sessions.entries()) {
      if (!token || !rec?.username || !Number.isFinite(rec?.ts)) continue;
      out[token] = { username: rec.username, ts: rec.ts };
    }
    fs.writeFileSync(SESSIONS_PATH, JSON.stringify(out, null, 2), 'utf8');
  } catch {}
}

function hydrateSessions() {
  const store = loadSessionsStore();
  const cutoff = now() - SESSION_TTL_MS;
  for (const [token, rec] of Object.entries(store)) {
    const username = normalizeAccountName(rec?.username);
    const ts = Number(rec?.ts);
    if (!username || !Number.isFinite(ts) || ts < cutoff) continue;
    sessions.set(token, { username, ts });
  }
}

function createSession(username) {
  const token = crypto.randomBytes(24).toString('hex');
  sessions.set(token, { username, ts: now() });
  saveSessionsStore();
  return token;
}

function getSessionUser(token) {
  if (!token) return '';
  const rec = sessions.get(token);
  if (!rec) return '';
  if (now() - rec.ts > SESSION_TTL_MS) {
    sessions.delete(token);
    saveSessionsStore();
    return '';
  }
  rec.ts = now();
  sessions.set(token, rec);
  saveSessionsStore();
  return rec.username;
}

function pruneServer(server) {
  const map = presence.get(server);
  if (!map) return;
  const cutoff = now() - TTL_MS;
  for (const [uuid, rec] of map.entries()) {
    if (!rec || rec.ts < cutoff) map.delete(uuid);
  }
  if (map.size === 0) presence.delete(server);
}

function chatKey(a, b) {
  const x = normalizeName(a);
  const y = normalizeName(b);
  return [x, y].sort().join('|');
}

function pruneChat() {
  const cutoff = now() - CHAT_TTL_MS;
  for (const [key, list] of chat.entries()) {
    const filtered = list.filter((m) => m && m.ts >= cutoff);
    if (filtered.length === 0) chat.delete(key);
    else chat.set(key, filtered.slice(-CHAT_MAX));
  }
}

function getFriends(user) {
  const name = normalizeAccountName(user);
  if (!name) return [];
  const list = friendsStore.users?.[name];
  if (!Array.isArray(list)) return [];
  return list.filter((n) => typeof n === 'string' && n.length > 0).slice(0, 200);
}

function setFriends(user, list) {
  const name = normalizeAccountName(user);
  if (!name) return [];
  const clean = Array.from(new Set(list.map(normalizeAccountName).filter((n) => n.length > 0))).slice(0, 200);
  friendsStore.users[name] = clean;
  saveFriendsStore();
  return clean;
}

function getRequests(user) {
  const name = normalizeAccountName(user);
  if (!name) return [];
  const list = friendsStore.requests?.[name];
  if (!Array.isArray(list)) return [];
  return list.filter((n) => typeof n === 'string' && n.length > 0).slice(0, 200);
}

function setRequests(user, list) {
  const name = normalizeAccountName(user);
  if (!name) return [];
  const clean = Array.from(new Set(list.map(normalizeAccountName).filter((n) => n.length > 0))).slice(0, 200);
  if (!friendsStore.requests) friendsStore.requests = {};
  friendsStore.requests[name] = clean;
  saveFriendsStore();
  return clean;
}

function loadClientVersion() {
  const envVersion = (process.env.CLIENT_VERSION || '').trim();
  const envUrl = (process.env.CLIENT_URL || '').trim();
  if (envVersion || envUrl) {
    return {
      version: envVersion || 'unknown',
      url: envUrl
    };
  }
  try {
    if (!fs.existsSync(CLIENT_VERSION_PATH)) {
      return { version: 'unknown', url: '' };
    }
    const raw = fs.readFileSync(CLIENT_VERSION_PATH, 'utf8');
    const json = JSON.parse(raw);
    return {
      version: typeof json.version === 'string' ? json.version : 'unknown',
      url: typeof json.url === 'string' ? json.url : ''
    };
  } catch {
    return { version: 'unknown', url: '' };
  }
}

function loadLauncherVersion() {
  const envVersion = (process.env.LAUNCHER_VERSION || '').trim();
  const envUrl = (process.env.LAUNCHER_URL || '').trim();
  if (envVersion || envUrl) {
    return {
      version: envVersion || 'unknown',
      url: envUrl
    };
  }
  try {
    if (!fs.existsSync(LAUNCHER_VERSION_PATH)) {
      return { version: 'unknown', url: '' };
    }
    const raw = fs.readFileSync(LAUNCHER_VERSION_PATH, 'utf8');
    const json = JSON.parse(raw);
    return {
      version: typeof json.version === 'string' ? json.version : 'unknown',
      url: typeof json.url === 'string' ? json.url : ''
    };
  } catch {
    return { version: 'unknown', url: '' };
  }
}

setInterval(() => {
  for (const server of presence.keys()) pruneServer(server);
  pruneChat();
}, CLEANUP_MS);

app.get('/health', (req, res) => {
  res.json({ ok: true, servers: presence.size });
});

app.post('/presence/heartbeat', (req, res) => {
  const body = req.body || {};
  const uuid = normalizeUuid(body.uuid);
  const name = normalizeName(body.name);
  const server = normalizeServer(body.server);
  const version = normalizeVersion(body.version);

  if (!uuid || !name) {
    return res.status(400).json({ ok: false, error: 'uuid and name required' });
  }

  const map = getServerMap(server);
  map.set(uuid, { uuid, name, server, version, ts: now() });
  res.json({ ok: true });
});

app.get('/presence/list', (req, res) => {
  const server = normalizeServer(req.query.server);
  pruneServer(server);
  const map = presence.get(server);
  if (!map) return res.json([]);

  const arr = Array.from(map.values()).map(({ uuid, name, version }) => ({ uuid, name, version }));
  res.json(arr);
});

function buildStats(map) {
  const stats = { count: 0, versions: {} };
  if (!map) return stats;
  for (const rec of map.values()) {
    stats.count += 1;
    const ver = rec.version || 'unknown';
    stats.versions[ver] = (stats.versions[ver] || 0) + 1;
  }
  return stats;
}

app.get('/presence/stats', (req, res) => {
  const rawServer = req.query.server;
  if (rawServer) {
    const server = normalizeServer(rawServer);
    pruneServer(server);
    const map = presence.get(server);
    return res.json({ server, ...buildStats(map) });
  }

  for (const server of presence.keys()) pruneServer(server);
  const servers = [];
  const versions = {};
  let total = 0;
  for (const [server, map] of presence.entries()) {
    const stats = buildStats(map);
    total += stats.count;
    for (const [ver, count] of Object.entries(stats.versions)) {
      versions[ver] = (versions[ver] || 0) + count;
    }
    servers.push({ server, ...stats });
  }
  res.json({ total, versions, servers });
});

app.post('/bc/register', async (req, res) => {
  const username = normalizeAccountName(req.body?.username);
  const password = String(req.body?.password || '');
  const gameName = normalizeName(req.body?.gameName);
  if (!username || password.length < 4) {
    return res.status(400).json({ ok: false, error: 'username and password required' });
  }
  if (accountsStore.users[username]) {
    return res.status(409).json({ ok: false, error: 'user exists' });
  }
  const passHash = await bcrypt.hash(password, 10);
  accountsStore.users[username] = {
    username,
    passHash,
    premiumUuid: '',
    premiumName: '',
    gameName: gameName || '',
    createdAt: now(),
    lastIp: '',
    lastCountry: '',
    lastSeenAt: now()
  };
  const ip = getRequestIp(req);
  const country = await resolveCountryByIp(ip);
  accountsStore.users[username].lastIp = ip;
  accountsStore.users[username].lastCountry = country;
  accountsStore.users[username].lastSeenAt = now();
  accountsStore.users[username].lastStatus = 'launcher';
  accountsStore.users[username].lastServerAddress = '';
  accountsStore.users[username].lastServerName = '';
  saveAccountsStore();
  const token = createSession(username);
  res.json({ ok: true, token, username, gameName: gameName || '' });
});

app.post('/bc/login', async (req, res) => {
  const username = normalizeAccountName(req.body?.username);
  const password = String(req.body?.password || '');
  const rec = accountsStore.users[username];
  if (!rec) return res.status(401).json({ ok: false, error: 'invalid credentials' });
  const ok = await bcrypt.compare(password, rec.passHash);
  if (!ok) return res.status(401).json({ ok: false, error: 'invalid credentials' });
  const ip = getRequestIp(req);
  const country = await resolveCountryByIp(ip);
  rec.lastIp = ip;
  rec.lastCountry = country;
  rec.lastSeenAt = now();
  rec.lastStatus = 'launcher';
  rec.lastServerAddress = '';
  rec.lastServerName = '';
  accountsStore.users[username] = rec;
  saveAccountsStore();
  const token = createSession(username);
  res.json({ ok: true, token, username, premiumUuid: rec.premiumUuid || '', premiumName: rec.premiumName || '' });
});

app.post('/bc/link-premium', (req, res) => {
  const token = String(req.body?.token || '');
  const username = getSessionUser(token);
  if (!username) return res.status(401).json({ ok: false, error: 'not authorized' });
  const premiumUuid = normalizeUuid(req.body?.premiumUuid);
  const premiumName = normalizeName(req.body?.premiumName);
  const rec = accountsStore.users[username];
  if (!rec) return res.status(404).json({ ok: false, error: 'account not found' });
  rec.premiumUuid = premiumUuid;
  rec.premiumName = premiumName;
  accountsStore.users[username] = rec;
  saveAccountsStore();
  res.json({ ok: true, premiumUuid, premiumName });
});

app.get('/bc/profile', (req, res) => {
  const token = String(req.query.token || '');
  const username = getSessionUser(token);
  if (!username) return res.status(401).json({ ok: false, error: 'not authorized' });
  const rec = accountsStore.users[username];
  if (!rec) return res.status(404).json({ ok: false, error: 'account not found' });
  res.json({
    ok: true,
    username,
    premiumUuid: rec.premiumUuid || '',
    premiumName: rec.premiumName || '',
    gameName: rec.gameName || '',
    lastCountry: rec.lastCountry || '',
    lastSeenAt: Number(rec.lastSeenAt || 0)
  });
});

app.post('/bc/touch', async (req, res) => {
  const token = String(req.body?.token || '');
  const username = getSessionUser(token);
  if (!username) return res.status(401).json({ ok: false, error: 'not authorized' });
  const rec = accountsStore.users[username];
  if (!rec) return res.status(404).json({ ok: false, error: 'account not found' });
  const ip = getRequestIp(req);
  const country = await resolveCountryByIp(ip);
  rec.lastIp = ip;
  rec.lastCountry = country;
  rec.lastSeenAt = now();
  const rawStatus = String(req.body?.status || '').trim().toLowerCase();
  rec.lastStatus = rawStatus || 'launcher';
  rec.lastServerAddress = String(req.body?.serverAddress || '').trim();
  rec.lastServerName = String(req.body?.serverName || '').trim();
  accountsStore.users[username] = rec;
  saveAccountsStore();
  res.json({
    ok: true,
    country: rec.lastCountry || '',
    status: rec.lastStatus || 'launcher',
    serverAddress: rec.lastServerAddress || '',
    serverName: rec.lastServerName || ''
  });
});

app.get('/friends/list', (req, res) => {
  const token = String(req.query.token || '');
  const username = getSessionUser(token);
  if (!username) return res.status(401).json([]);
  const names = getFriends(username);
  const out = names.map((n) => {
    const rec = accountsStore.users?.[n];
    return {
      name: n,
      country: rec?.lastCountry || '',
      lastSeenAt: Number(rec?.lastSeenAt || 0),
      status: rec?.lastStatus || '',
      serverAddress: rec?.lastServerAddress || '',
      serverName: rec?.lastServerName || ''
    };
  });
  return res.json(out);
});

app.post('/friends/add', (req, res) => {
  const token = String(req.body?.token || '');
  const username = getSessionUser(token);
  const friend = normalizeName(req.body?.friend);
  if (!username || !friend) return res.status(400).json({ ok: false, error: 'auth and friend required' });
  if (!accountsStore.users?.[friend]) {
    return res.status(404).json({ ok: false, error: 'friend not found' });
  }
  const list = getFriends(username);
  list.push(friend);
  const updated = setFriends(username, list);
  res.json({ ok: true, friends: updated });
});

app.post('/friends/request', (req, res) => {
  const token = String(req.body?.token || '');
  const username = getSessionUser(token);
  const friend = normalizeAccountName(req.body?.friend);
  if (!username) return res.status(401).json({ ok: false, error: 'not authorized' });
  if (!friend) return res.status(400).json({ ok: false, error: 'friend required' });
  if (username === friend) return res.status(400).json({ ok: false, error: 'cannot add self' });
  if (!accountsStore.users?.[friend]) return res.status(404).json({ ok: false, error: 'friend not found' });
  const list = new Set(getFriends(username));
  if (list.has(friend)) return res.json({ ok: true, status: 'already_friends' });
  const pending = getRequests(friend);
  if (!pending.includes(username)) pending.push(username);
  setRequests(friend, pending);
  res.json({ ok: true, status: 'requested' });
});

app.get('/friends/requests', (req, res) => {
  const token = String(req.query.token || '');
  const username = getSessionUser(token);
  if (!username) return res.status(401).json([]);
  return res.json(getRequests(username));
});

app.post('/friends/accept', (req, res) => {
  const token = String(req.body?.token || '');
  const username = getSessionUser(token);
  const from = normalizeAccountName(req.body?.from);
  if (!username) return res.status(401).json({ ok: false, error: 'not authorized' });
  if (!from) return res.status(400).json({ ok: false, error: 'from required' });
  const pending = getRequests(username).filter((n) => n !== from);
  setRequests(username, pending);
  const a = new Set(getFriends(username));
  a.add(from);
  setFriends(username, Array.from(a));
  const b = new Set(getFriends(from));
  b.add(username);
  setFriends(from, Array.from(b));
  res.json({ ok: true });
});

app.post('/friends/decline', (req, res) => {
  const token = String(req.body?.token || '');
  const username = getSessionUser(token);
  const from = normalizeAccountName(req.body?.from);
  if (!username) return res.status(401).json({ ok: false, error: 'not authorized' });
  if (!from) return res.status(400).json({ ok: false, error: 'from required' });
  const pending = getRequests(username).filter((n) => n !== from);
  setRequests(username, pending);
  res.json({ ok: true });
});

app.post('/friends/remove', (req, res) => {
  const token = String(req.body?.token || '');
  const username = getSessionUser(token);
  const friend = normalizeName(req.body?.friend);
  if (!username || !friend) return res.status(400).json({ ok: false, error: 'auth and friend required' });
  const list = getFriends(username).filter((n) => n !== friend);
  const updated = setFriends(username, list);
  res.json({ ok: true, friends: updated });
});

app.post('/friends/online', (req, res) => {
  const names = Array.isArray(req.body?.names) ? req.body.names : [];
  const targets = names.map(normalizeAccountName).filter((n) => n.length > 0);
  if (targets.length === 0) return res.json([]);

  // Build map from BC username -> possible in-game names
  const lookup = new Map();
  for (const user of targets) {
    const rec = accountsStore.users?.[user];
    const aliases = new Set();
    aliases.add(user.toLowerCase());
    if (rec?.premiumName) aliases.add(String(rec.premiumName).trim().toLowerCase());
    if (rec?.gameName) aliases.add(String(rec.gameName).trim().toLowerCase());
    lookup.set(user, aliases);
  }

  for (const server of presence.keys()) pruneServer(server);
  const best = new Map();
  for (const map of presence.values()) {
    for (const rec of map.values()) {
      if (!rec?.name) continue;
      const onlineName = String(rec.name).trim().toLowerCase();
      for (const [bcUser, aliases] of lookup.entries()) {
        if (!aliases.has(onlineName)) continue;
        const prev = best.get(bcUser);
        if (!prev || rec.ts > prev.ts) {
          best.set(bcUser, { name: bcUser, server: rec.server, version: rec.version, ts: rec.ts });
        }
      }
    }
  }
  res.json(Array.from(best.values()));
});

app.post('/chat/send', (req, res) => {
  const token = String(req.body?.token || '');
  const from = getSessionUser(token);
  const to = normalizeName(req.body?.to);
  const message = normalizeMessage(req.body?.message);
  if (!from || !to || !message) {
    return res.status(400).json({ ok: false, error: 'auth, to, message required' });
  }
  const key = chatKey(from, to);
  const list = chat.get(key) || [];
  list.push({ from, to, message, ts: now() });
  chat.set(key, list.slice(-CHAT_MAX));
  res.json({ ok: true });
});

app.get('/chat/poll', (req, res) => {
  const token = String(req.query.token || '');
  const user = getSessionUser(token);
  const peer = normalizeName(req.query.peer);
  const since = Number.isFinite(Number(req.query.since)) ? Number(req.query.since) : 0;
  if (!user) return res.status(401).json([]);
  if (!peer) return res.json([]);
  const key = chatKey(user, peer);
  const list = chat.get(key) || [];
  const result = list.filter((m) => m.ts > since && (m.from === user || m.to === user));
  res.json(result.slice(-200));
});

app.get('/client/version', (req, res) => {
  res.json(loadClientVersion());
});

app.get('/launcher/version', (req, res) => {
  res.json(loadLauncherVersion());
});

// ---------------------------------------------------------------------------
// Anti-crack
// ---------------------------------------------------------------------------

function buildAntiCrackSecret() {
  const a = Buffer.from('4A6F686E6E7942726F776E', 'hex');
  const b = Buffer.from('5869616F4C6F6E67', 'hex');
  const c = Buffer.from('426C616465436C69656E74', 'hex');
  const d = Buffer.from('536563726574', 'hex');
  return crypto.createHash('sha256').update(Buffer.concat([a, b, c, d])).digest('hex');
}

const antiCrackSecret = buildAntiCrackSecret();

app.get('/api/anti-crack/secret', (req, res) => {
  res.json({ secret: antiCrackSecret });
});

app.post('/api/anti-crack/validate', (req, res) => {
  const token = String(req.body?.token || '');
  if (!token) return res.json({ valid: false });

  const parts = token.split(':');

  // v1: hexTs:hmac
  if (parts.length === 2) {
    const [hexTs, hmac] = parts;
    const timestamp = parseInt(hexTs, 16);
    if (!Number.isFinite(timestamp)) return res.json({ valid: false });
    if (Date.now() - timestamp > 60000) return res.json({ valid: false });
    const expected = crypto.createHmac('sha256', antiCrackSecret)
      .update(`bladeclient_launch_v1:${hexTs}`)
      .digest('hex');
    try {
      const valid = crypto.timingSafeEqual(Buffer.from(hmac, 'hex'), Buffer.from(expected, 'hex'));
      return res.json({ valid });
    } catch {
      return res.json({ valid: false });
    }
  }

  // v2: hexTs:nonce:hmac
  if (parts.length === 3) {
    const [hexTs, nonce, hmac] = parts;
    const timestamp = parseInt(hexTs, 16);
    if (!Number.isFinite(timestamp)) return res.json({ valid: false });
    if (Date.now() - timestamp > 60000) return res.json({ valid: false });
    const expected = crypto.createHmac('sha256', antiCrackSecret)
      .update(`bladeclient_launch_v2:${hexTs}:${nonce}`)
      .digest('hex');
    try {
      const valid = crypto.timingSafeEqual(Buffer.from(hmac, 'hex'), Buffer.from(expected, 'hex'));
      return res.json({ valid });
    } catch {
      return res.json({ valid: false });
    }
  }

  return res.json({ valid: false });
});

// Error reporting endpoint
const ERROR_REPORT_PATH = path.join(process.cwd(), 'error-reports.json');

app.post('/api/error-report', (req, res) => {
  const { errors } = req.body || {};
  if (!Array.isArray(errors) || errors.length === 0) {
    return res.json({ ok: false, reason: 'no errors' });
  }
  try {
    const existing = [];
    try {
      if (fs.existsSync(ERROR_REPORT_PATH)) {
        existing.push(...JSON.parse(fs.readFileSync(ERROR_REPORT_PATH, 'utf8')));
      }
    } catch {}
    const now = Date.now();
    for (const e of errors) {
      existing.push({
        ts: e.ts || now,
        level: e.level || 'error',
        source: e.source || 'unknown',
        message: String(e.message || ''),
        stack: e.stack || ''
      });
    }
    if (existing.length > 5000) existing.splice(0, existing.length - 5000);
    fs.writeFileSync(ERROR_REPORT_PATH, JSON.stringify(existing, null, 2));
  } catch {}
  res.json({ ok: true });
});

app.listen(PORT, () => {
  console.log(`[BladeClient Presence] listening on :${PORT}`);
});
