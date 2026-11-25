import { describe, expect, it, vi } from 'vitest';
import { en } from './catalogs/en';
import { hi } from './catalogs/hi';
import { createFormatters } from './format';
import {
  catalogVersion,
  createTranslator,
  enumLabel,
  loadCatalog,
  onCatalogLoaded,
  resolveLocale,
} from './translate';

describe('catalogues', () => {
  it('use module.screen.element keys and only keys that exist in en', () => {
    for (const key of Object.keys(en)) expect(key).toMatch(/^[a-z]+(\.[a-zA-Z0-9]+){2,}$/);
    for (const key of Object.keys(hi)) expect(Object.keys(en)).toContain(key);
  });

  it('parse as ICU messages', () => {
    const t = createTranslator('en');
    for (const key of Object.keys(en) as (keyof typeof en)[]) {
      // supply every argument the message declares ({name}, {count, plural, ...})
      const args = Object.fromEntries([...en[key].matchAll(/\{(\w+)/g)].map((m) => [m[1] as string, 1]));
      expect(() => t(key, args), key).not.toThrow();
    }
  });
});

describe('translation', () => {
  it('formats ICU plurals and interpolation', () => {
    const t = createTranslator('en');
    expect(t('common.list.count', { count: 0 })).toBe('No items');
    expect(t('common.list.count', { count: 1 })).toBe('1 item');
    expect(t('common.list.count', { count: 12 })).toBe('12 items');
    expect(t('master.countries.saved', { name: 'India' })).toBe('India saved.');
  });

  it('loads a catalogue on first use, in English until then, and tells its listeners', async () => {
    const heard = vi.fn();
    const stop = onCatalogLoaded(heard);
    const before = catalogVersion();
    expect(createTranslator('hi')('acc.signIn.title')).toBe('Sign in');
    await loadCatalog('hi');
    await loadCatalog('hi');
    await loadCatalog('xx');
    expect(heard).toHaveBeenCalledTimes(1);
    expect(catalogVersion()).toBe(before + 1);
    stop();
  });

  it('falls back to en for missing translations and unknown locales', async () => {
    await loadCatalog('hi');
    const t = createTranslator('hi');
    expect(t('acc.signIn.title')).toBe('साइन इन करें');
    expect(t('acc.signIn.tenant')).toBe('School code');
    expect(createTranslator('xx')('app.brand.name')).toBe('MySMS');
  });

  it('resolves user → tenant → en', () => {
    expect(resolveLocale('hi-IN', 'en')).toBe('hi');
    expect(resolveLocale(null, 'hi')).toBe('hi');
    expect(resolveLocale('fr', 'de')).toBe('en');
    expect(resolveLocale()).toBe('en');
  });

  it('never shows a raw enum value', () => {
    const t = createTranslator('en');
    expect(enumLabel(t, 'operation', 'succeeded')).toBe('Done');
    expect(enumLabel(t, 'operation', 'paused_by_admin')).toBe('Other');
    expect(enumLabel(t, 'assessment.assignment_submission_status', 'change_requested')).toBe(
      'Changes requested',
    );
    expect(enumLabel(t, 'assessment.gradingMethod', 'percentage_band')).toBe('Percentage bands');
    expect(enumLabel(t, 'operation', null)).toBe('Other');
  });
});

describe('formatting', () => {
  it('formats money with an explicit currency and dates in a time zone', () => {
    const f = createFormatters('en-IN', 'Asia/Kolkata');
    expect(f.day('2026-03-04')).toBe(
      new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeZone: 'UTC' }).format(
        new Date('2026-03-04T00:00:00Z'),
      ),
    );
    expect(createFormatters('en-US', 'America/Los_Angeles').day('2026-03-04')).toContain('4');
    expect(f.money(125000.5, 'INR')).toBe('₹1,25,000.50');
    expect(f.money('10', 'USD')).toBe('$10.00');
    expect(f.number(1234567)).toBe('12,34,567');
    expect(f.percent(0.42)).toBe('42%');
    expect(f.percentExact(0.7456)).toBe('74.56%');
    expect(f.percentExact(1)).toBe('100%');
    expect(f.date('2026-11-06T20:00:00Z')).toBe('7 Nov 2026');
    expect(f.dateTime(new Date('2026-11-06T04:30:00Z'))).toBe('6 Nov 2026, 10:00 am');
    expect(f.time('2026-11-06T04:30:00Z')).toBe('10:00 am');
    // a calendar day keeps its weekday whatever the viewer's zone
    expect(f.weekday('2026-03-02')).toBe('Monday');
    // a month is that calendar month everywhere, from a day or a month value
    expect(createFormatters('en-US', 'America/Los_Angeles').month('2026-03-01')).toBe('March 2026');
    expect(f.month('2026-03')).toBe('March 2026');
    expect(createFormatters('en-US').date('2026-01-15T12:00:00Z')).toBe('Jan 15, 2026');
  });
});
