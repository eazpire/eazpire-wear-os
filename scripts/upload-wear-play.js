#!/usr/bin/env node
/**
 * Upload Wear AAB only to wear:* track (never phone internal/beta/production).
 */
const fs = require('fs');

function parseArgs() {
  const args = process.argv.slice(2);
  const out = {
    package: 'com.eazpire.wear.os',
    aab: 'app/build/outputs/bundle/release/app-release.aab',
    track: 'wear:qa',
    symbols: '',
    status: 'completed',
  };
  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--package') out.package = args[++i];
    else if (args[i] === '--aab') out.aab = args[++i];
    else if (args[i] === '--track') out.track = args[++i];
    else if (args[i] === '--symbols') out.symbols = args[++i];
    else if (args[i] === '--status') out.status = args[++i];
  }
  if (out.track === 'wear:internal' || out.track === 'internal') out.track = 'wear:qa';
  if (!out.track.startsWith('wear:')) {
    console.error(`::error::Refusing phone track "${out.track}" — use wear:qa (internal testing)`);
    process.exit(1);
  }
  return out;
}

async function getPublisher() {
  const raw = process.env.PLAY_SERVICE_ACCOUNT_JSON;
  if (!raw) throw new Error('PLAY_SERVICE_ACCOUNT_JSON missing');
  const { google } = await import('googleapis');
  const auth = new google.auth.GoogleAuth({
    credentials: JSON.parse(raw),
    scopes: ['https://www.googleapis.com/auth/androidpublisher'],
  });
  return google.androidpublisher({ version: 'v3', auth });
}

async function getTrackReleases(publisher, packageName, editId, track) {
  const res = await publisher.edits.tracks.list({ packageName, editId });
  const row = (res.data.tracks || []).find((t) => t.track === track);
  return row?.releases || [];
}

async function assignVersionToTrack(publisher, packageName, editId, track, versionCode, status) {
  const existing = await getTrackReleases(publisher, packageName, editId, track);
  const codeStr = String(versionCode);
  const already = existing.some((rel) =>
    (rel.versionCodes || []).map(String).includes(codeStr)
  );
  if (already) {
    console.log(`versionCode ${versionCode} already assigned to ${track}`);
    return;
  }

  const releases = [{ status, versionCodes: [codeStr] }];

  await publisher.edits.tracks.update({
    packageName,
    editId,
    track,
    requestBody: { track, releases },
  });
  console.log(
    `Assigned versionCode ${versionCode} to ${track} (one ${status} release; prior codes removed from track)`
  );
}

function formatPlayApiError(e) {
  const parts = [e.message || String(e)];
  const data = e.response?.data;
  if (data) {
    if (typeof data === 'string') parts.push(data);
    else parts.push(JSON.stringify(data));
  }
  return parts.join(' — ');
}

async function main() {
  const opts = parseArgs();
  if (!fs.existsSync(opts.aab)) throw new Error(`AAB not found: ${opts.aab}`);

  const publisher = await getPublisher();
  const insert = await publisher.edits.insert({ packageName: opts.package });
  const editId = insert.data.id;
  console.log(`Edit ${editId} — upload to track ${opts.track} only`);

  try {
    const bundle = await publisher.edits.bundles.upload({
      packageName: opts.package,
      editId,
      media: {
        mimeType: 'application/octet-stream',
        body: fs.createReadStream(opts.aab),
      },
    });
    const versionCode = Number(bundle.data.versionCode);
    if (!versionCode) throw new Error('Bundle upload returned no versionCode');
    console.log(`Uploaded AAB versionCode=${versionCode}`);

    await assignVersionToTrack(
      publisher,
      opts.package,
      editId,
      opts.track,
      versionCode,
      opts.status
    );

    await publisher.edits.commit({
      packageName: opts.package,
      editId,
      changesNotSentForReview: true,
    });
    console.log('Edit committed (changesNotSentForReview=true — promote in Play Console if needed).');
  } catch (e) {
    try {
      await publisher.edits.delete({ packageName: opts.package, editId });
    } catch {
      /* ignore */
    }
    throw e;
  }
}

main().catch((e) => {
  const msg = formatPlayApiError(e);
  console.error(msg);
  console.error(`::error::${msg.replace(/[\r\n]+/g, ' ').slice(0, 500)}`);
  process.exit(1);
});
