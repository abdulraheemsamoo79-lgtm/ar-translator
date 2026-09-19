// AR Translator — update-check backend.
// Deploy this folder to Railway. The Android app calls GET /version on
// every launch and shows an "update available" dialog when the server's
// versionCode is higher than the installed app's.
//
// To ship an update: bump versionCode/versionName in app/build.gradle.kts,
// build a new APK, upload it wherever you host downloads (Railway static
// file, GitHub Releases, your own server), then update version.json below
// (or POST to /admin/version with your ADMIN_KEY) with the new numbers and
// the download link. Every phone with the app installed will see the
// update prompt next time they open it — no app-store review needed.

const fastify = require('fastify')({ logger: true });
const fs = require('fs');
const path = require('path');

const VERSION_FILE = path.join(__dirname, 'version.json');
const ADMIN_KEY = process.env.ADMIN_KEY || 'change-this-secret';

fastify.register(require('@fastify/cors'), { origin: true });

function readVersion() {
  return JSON.parse(fs.readFileSync(VERSION_FILE, 'utf-8'));
}

function writeVersion(data) {
  fs.writeFileSync(VERSION_FILE, JSON.stringify(data, null, 2));
}

// Public — the app polls this on every launch.
fastify.get('/version', async (request, reply) => {
  return readVersion();
});

// Simple health check for Railway.
fastify.get('/', async (request, reply) => {
  return { status: 'ok', service: 'ar-translator-backend' };
});

// Protected — call this (e.g. from curl or Postman) whenever you push a
// new release, instead of hand-editing version.json on the server.
fastify.post('/admin/version', async (request, reply) => {
  const key = request.headers['x-admin-key'];
  if (key !== ADMIN_KEY) {
    reply.code(401);
    return { error: 'Unauthorized' };
  }
  const { versionCode, versionName, downloadUrl, changelog, forceUpdate } = request.body;
  if (!versionCode || !versionName || !downloadUrl) {
    reply.code(400);
    return { error: 'versionCode, versionName and downloadUrl are required' };
  }
  const data = { versionCode, versionName, downloadUrl, changelog: changelog || '', forceUpdate: !!forceUpdate };
  writeVersion(data);
  return { updated: true, data };
});

const start = async () => {
  try {
    const port = process.env.PORT || 3000;
    await fastify.listen({ port, host: '0.0.0.0' });
  } catch (err) {
    fastify.log.error(err);
    process.exit(1);
  }
};

start();
