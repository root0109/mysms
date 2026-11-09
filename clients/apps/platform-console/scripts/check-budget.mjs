// ui/05 §10: the initial route's JavaScript must stay within 300 KB gzip. Fails the build when it does not.
import { readFileSync, readdirSync } from 'node:fs';
import { resolve } from 'node:path';
import { gzipSync } from 'node:zlib';

const BUDGET_BYTES = 300 * 1024;
const dist = resolve(import.meta.dirname, '../dist');
const html = readFileSync(resolve(dist, 'index.html'), 'utf8');
const entries = [...html.matchAll(/<(?:script|link)[^>]+(?:src|href)="\/(assets\/[^"]+\.js)"/g)].map(
  (m) => m[1],
);
if (entries.length === 0) throw new Error('no entry scripts found in dist/index.html');
let total = 0;
for (const file of entries) total += gzipSync(readFileSync(resolve(dist, file))).length;
const all = readdirSync(resolve(dist, 'assets')).filter((f) => f.endsWith('.js')).length;
const kb = (total / 1024).toFixed(1);
if (total > BUDGET_BYTES) {
  console.error(`budget: initial JS ${kb} KB gzip exceeds 300 KB`);
  process.exit(1);
}
console.warn(
  `budget: initial JS ${kb} KB gzip (limit 300 KB) across ${entries.length} entry file(s), ${all} chunk(s) total`,
);
