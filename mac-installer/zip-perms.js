#!/usr/bin/env node
/*
 * zip-perms.js - create .zip or .tar.gz archives that preserve Unix
 * executable bits.
 *
 * PowerShell's Compress-Archive and Git Bash tar both strip the +x bits on
 * Windows, which would make the packaged .app unopenable on macOS (the
 * Contents/MacOS executable would have mode 0644). This writer stores the
 * modes by hand, which macOS's Archive Utility / unzip honor on extraction.
 *
 * Usage:
 *   node zip-perms.js --out out.zip  --app "Haval Installer.app" [--pdf file.pdf]
 *   node zip-perms.js --tgz --out out.tar.gz --app "Haval Installer.app" [--pdf file.pdf]
 *   (--app is a directory: walked recursively; --pdf is a file added at archive root)
 */
'use strict';

const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const args = process.argv.slice(2);
const get = (flag) => args[args.indexOf(flag) + 1];
const OUT = get('--out');
const APP = get('--app');
const PDF = get('--pdf');
const TGZ = args.includes('--tgz');
if (!OUT || !APP) { console.error('usage: node zip-perms.js --out out.zip --app <dir> [--pdf <file>] [--tgz]'); process.exit(1); }

// ----------------------------------------------------------------- crc32
const CRC_TABLE = (() => {
  const t = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = (c & 1) ? (0xedb88320 ^ (c >>> 1)) : (c >>> 1);
    t[n] = c >>> 0;
  }
  return t;
})();
function crc32(buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) c = CRC_TABLE[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}

// ----------------------------------------------------------------- walk
// archive paths live under the .app root, e.g. "Haval Installer.app/Contents/..."
const APP_NAME = path.basename(APP);
const EXEC_NAMES = new Set([
  'Contents/MacOS/HavalInstaller',
  'Contents/Resources/menu.sh',
  'Contents/Resources/install-macos.sh',
  'Contents/Resources/fetch-payloads.sh',
  'Contents/Resources/car.py',
  'Contents/Resources/car.pl',
  'Contents/Resources/serve.pl',
  'Contents/Resources/mock-car.pl',
].map(n => APP_NAME + '/' + n));
const items = []; // { name, data, mode, isDir }

function walk(dir, prefix) {
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, ent.name);
    const rel = (prefix ? prefix + '/' : '') + ent.name;
    if (ent.isDirectory()) {
      items.push({ name: rel + '/', data: Buffer.alloc(0), mode: 0o40755, isDir: true });
      walk(full, rel);
    } else if (ent.isFile()) {
      const mode = EXEC_NAMES.has(rel) ? 0o100755 : 0o100644;
      items.push({ name: rel, data: fs.readFileSync(full), mode, isDir: false });
    }
  }
}
// root dir entry named after the .app folder, so unzipping recreates
// "Haval Installer.app/" with everything inside it
items.push({ name: APP_NAME + '/', data: Buffer.alloc(0), mode: 0o40755, isDir: true });
walk(APP, APP_NAME);
if (PDF) {
  items.push({ name: path.basename(PDF), data: fs.readFileSync(PDF), mode: 0o100644, isDir: false });
}

// ----------------------------------------------------------------- zip
const DOS_TIME = (12 << 11) | (0 << 5) | 0;            // 12:00
const DOS_DATE = ((2026 - 1980) << 9) | (8 << 5) | 25; // 2026-08-25
const locals = [];
const centrals = [];
const out = [];
let offset = 0;

for (const it of items) {
  const nameBuf = Buffer.from(it.name, 'utf8');
  const data = it.data;
  const crc = crc32(data);
  const comp = it.isDir ? data : zlib.deflateRawSync(data, { level: 9 });
  const method = it.isDir ? 0 : 8;

  const local = Buffer.alloc(30);
  local.writeUInt32LE(0x04034b50, 0);
  local.writeUInt16LE(20, 4);
  local.writeUInt16LE(0x0000, 6); // no flags: names are pure ASCII (Archive Utility is strictest with no flags set)
  local.writeUInt16LE(method, 8);
  local.writeUInt16LE(DOS_TIME, 10);
  local.writeUInt16LE(DOS_DATE, 12);
  local.writeUInt32LE(crc, 14);
  local.writeUInt32LE(comp.length, 18);
  local.writeUInt32LE(data.length, 22);
  local.writeUInt16LE(nameBuf.length, 26);
  local.writeUInt16LE(0, 28);

  const central = Buffer.alloc(46);
  central.writeUInt32LE(0x02014b50, 0);
  central.writeUInt16LE(0x0314, 4); // version made by: UNIX (3) << 8 | 20
  central.writeUInt16LE(20, 6);
  central.writeUInt16LE(0x0000, 8);
  central.writeUInt16LE(method, 10);
  central.writeUInt16LE(DOS_TIME, 12);
  central.writeUInt16LE(DOS_DATE, 14);
  central.writeUInt32LE(crc, 16);
  central.writeUInt32LE(comp.length, 20);
  central.writeUInt32LE(data.length, 24);
  central.writeUInt16LE(nameBuf.length, 28);
  central.writeUInt16LE(0, 30); // extra len
  central.writeUInt16LE(0, 32); // comment len
  central.writeUInt16LE(0, 34); // disk start
  central.writeUInt16LE(0, 36); // internal attrs
  // note: `<<` is signed 32-bit in JS and wraps negative for mode bits >= 0x8000
  const extAttrs = ((it.mode & 0xffff) * 0x10000) | (it.isDir ? 0x10 : 0);
  central.writeUInt32LE(extAttrs >>> 0, 38); // external attrs
  central.writeUInt32LE(offset, 42);

  locals.push(local);
  centrals.push(central);
  out.push(local, nameBuf, comp);
  offset += local.length + nameBuf.length + comp.length;
}

const cdStart = offset;
let cdSize = 0;
for (let i = 0; i < items.length; i++) {
  const c = centrals[i];
  out.push(c, Buffer.from(items[i].name, 'utf8'));
  cdSize += c.length + Buffer.byteLength(items[i].name);
}

const eocd = Buffer.alloc(22);
eocd.writeUInt32LE(0x06054b50, 0);
eocd.writeUInt16LE(0, 4);
eocd.writeUInt16LE(0, 6);
eocd.writeUInt16LE(items.length, 8);
eocd.writeUInt16LE(items.length, 10);
eocd.writeUInt32LE(cdSize, 12);
eocd.writeUInt32LE(cdStart, 16);
eocd.writeUInt16LE(0, 20);
out.push(eocd);

fs.writeFileSync(OUT, Buffer.concat(out));
console.log(`zip written: ${OUT} (${items.length} entries, ${Math.round(fs.statSync(OUT).size / 1024 / 1024)} MB)`);

// ----------------------------------------------------------------- tar.gz
if (TGZ) {
  const MTIME = 1787184000; // 2026-08-25T00:00:00Z
  const chunks = [];
  const octal = (val, len) => (val.toString(8).padStart(len - 1, '0') + '\0');
  for (const it of items) {
    const name = Buffer.from(it.name, 'utf8');
    if (name.length > 100) throw new Error('tar name too long: ' + it.name);
    const h = Buffer.alloc(512);
    name.copy(h, 0);
    h.write(octal(it.mode, 8), 100);          // mode: 7 octal digits + NUL
    h.write(octal(0, 8), 108);                // uid
    h.write(octal(0, 8), 116);                // gid
    h.write(octal(it.data.length, 12), 124);  // size: 11 octal digits + NUL
    h.write(octal(MTIME, 12), 136);           // mtime
    h.write('        ', 148);                 // chksum placeholder (8 spaces)
    h.write(it.isDir ? '5' : '0', 156);       // typeflag
    h.write('ustar\0', 257);                  // magic
    h.write('00', 263);                       // version
    h.write('macos', 265);                    // uname
    h.write('staff', 297);                    // gname
    // checksum: sum of all 512 bytes with chksum field as spaces
    let sum = 0;
    for (let i = 0; i < 512; i++) sum += h[i];
    h.write(octal(sum, 7) + ' ', 148);        // 6 octal digits + NUL + space
    chunks.push(h);
    if (it.data.length) {
      const d = Buffer.alloc(Math.ceil(it.data.length / 512) * 512);
      it.data.copy(d, 0);
      chunks.push(d);
    }
  }
  chunks.push(Buffer.alloc(1024)); // two zero blocks = end of archive
  const tar = Buffer.concat(chunks);
  fs.writeFileSync(OUT, zlib.gzipSync(tar, { level: 9 }));
  console.log(`tar.gz written: ${OUT} (${items.length} entries, ${Math.round(fs.statSync(OUT).size / 1024 / 1024)} MB)`);
  process.exit(0);
}

