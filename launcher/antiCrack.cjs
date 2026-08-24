const crypto = require('crypto');

const sessions = new Map();

function generateSession() {
  const id = crypto.randomBytes(16).toString('hex');
  const createdAt = Date.now();
  sessions.set(id, { createdAt });
  return id;
}

function verifySession(id) {
  if (!id || typeof id !== 'string') return false;
  const session = sessions.get(id);
  if (!session) return false;
  if (Date.now() - session.createdAt > 30_000) {
    sessions.delete(id);
    return false;
  }
  return true;
}

function getSessionJvmArg() {
  return `-Dbladeclient.session_id=${generateSession()}`;
}

function getVerifyEndpoint() {
  return '/launcher/verify';
}

function handleVerifyRequest(url, res) {
  const id = url.searchParams.get('id');
  const valid = verifySession(id);
  res.writeHead(200, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ valid }));
}

module.exports = { generateSession, verifySession, getSessionJvmArg, getVerifyEndpoint, handleVerifyRequest };
