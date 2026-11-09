/** Intl-based formatting for one locale (ui/04 §7: numbers, dates, currency with an explicit code). */
export function createFormatters(locale: string, timeZone?: string) {
  const tz = timeZone ? { timeZone } : {};
  const date = new Intl.DateTimeFormat(locale, { dateStyle: 'medium', ...tz });
  const dateTime = new Intl.DateTimeFormat(locale, { dateStyle: 'medium', timeStyle: 'short', ...tz });
  // a calendar date (YYYY-MM-DD) has no time zone: shown as that same day everywhere
  const day = new Intl.DateTimeFormat(locale, { dateStyle: 'medium', timeZone: 'UTC' });
  const number = new Intl.NumberFormat(locale);
  const percent = new Intl.NumberFormat(locale, { style: 'percent', maximumFractionDigits: 0 });
  const money = new Map<string, Intl.NumberFormat>();
  const toDate = (v: Date | string) => (typeof v === 'string' ? new Date(v) : v);
  return {
    date: (v: Date | string) => date.format(toDate(v)),
    dateTime: (v: Date | string) => dateTime.format(toDate(v)),
    /** A contract `date` value, e.g. `2026-03-04`, without shifting it into the viewer's time zone. */
    day: (v: string) => day.format(new Date(`${v.slice(0, 10)}T00:00:00Z`)),
    number: (v: number) => number.format(v),
    percent: (fraction: number) => percent.format(fraction),
    /** Money always carries its ISO 4217 code from master.currency. */
    money: (amount: number | string, currencyCode: string) => {
      let f = money.get(currencyCode);
      if (!f) {
        f = new Intl.NumberFormat(locale, { style: 'currency', currency: currencyCode });
        money.set(currencyCode, f);
      }
      return f.format(typeof amount === 'string' ? Number(amount) : amount);
    },
  };
}

export type Formatters = ReturnType<typeof createFormatters>;
