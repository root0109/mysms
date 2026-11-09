import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { tokensCss } from '@mysms/tokens';
import { describe, expect, it } from 'vitest';

/** Every token a stylesheet uses must exist in the token build (a typo would silently fall back). */
describe('stylesheets only use defined tokens', () => {
  const defined = new Set([...tokensCss().matchAll(/(--sms-[a-z0-9-]+):/g)].map((m) => m[1]));
  const sheets = ['src/styles.css', '../web-kit/src/kit.css'];
  for (const sheet of sheets) {
    it(sheet, () => {
      const css = readFileSync(resolve(import.meta.dirname, '..', sheet), 'utf8');
      const used = [...new Set([...css.matchAll(/var\((--sms-[a-z0-9-]+)\)/g)].map((m) => m[1]))];
      expect(used.length).toBeGreaterThan(5);
      expect(used.filter((u) => !defined.has(u))).toEqual([]);
    });
  }
});
