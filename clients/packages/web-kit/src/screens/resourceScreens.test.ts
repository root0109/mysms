import { RESOURCES, SCREENS } from '@mysms/core';
import { en } from '@mysms/i18n';
import { describe, expect, it } from 'vitest';
import { defaultColumns, defaultFilter, screenTitleKey } from './resourceColumns';
import { NAV_GROUPS, RESOURCE_SCREENS } from './resourceScreens';

describe('the list-manager screens', () => {
  it('each names a served record type, a web screen of the inventory, a heading and a navigation group', () => {
    const ids = new Set<string>();
    for (const screen of RESOURCE_SCREENS) {
      expect(ids.has(screen.id), screen.id).toBe(false);
      ids.add(screen.id);
      const def = RESOURCES.find((r) => r.key === screen.resource);
      expect(def, screen.resource).toBeDefined();
      expect(def?.ops.List, `${screen.resource} List`).toBeDefined();
      expect(SCREENS.find((s) => s.id === screen.id)?.clients, screen.id).toContain('W');
      expect(en[screenTitleKey(screen.id)], screen.id).toBeTruthy();
      expect(NAV_GROUPS).toContain(screen.group);
      expect(en[`shell.nav.group.${screen.group}` as keyof typeof en]).toBeTruthy();
      const columns = screen.columns ?? defaultColumns(def!);
      expect(columns.length, screen.id).toBeGreaterThan(0);
      for (const name of columns)
        expect(
          def?.fields.map((f) => f.name),
          screen.id,
        ).toContain(name);
      const filter = screen.filter ?? defaultFilter(def!);
      if (filter) expect(def?.fields.find((f) => f.name === filter)?.kind).toBe('enum');
    }
  });

  it('chooses naming fields first and at most five columns', () => {
    const institution = RESOURCES.find((r) => r.key === 'org.Institution')!;
    const columns = defaultColumns(institution);
    expect(columns.slice(0, 2)).toEqual(['code', 'name']);
    expect(columns.length).toBeLessThanOrEqual(5);
    expect(defaultFilter(institution)).toBe('status');
  });
});
