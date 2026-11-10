/** Intl-based formatting for one locale (ui/04 §7: numbers, dates, currency with an explicit code). */
export function createFormatters(locale: string, timeZone?: string) {
  const tz = timeZone ? { timeZone } : {};
  const date = new Intl.DateTimeFormat(locale, { dateStyle: 'medium', ...tz });
  const dateTime = new Intl.DateTimeFormat(locale, { dateStyle: 'medium', timeStyle: 'short', ...tz });
  const time = new Intl.DateTimeFormat(locale, { timeStyle: 'short', ...tz });
  const weekday = new Intl.DateTimeFormat(locale, { weekday: 'long', timeZone: 'UTC' });
  // a calendar date (YYYY-MM-DD) has no time zone: shown as that same day everywhere
  const day = new Intl.DateTimeFormat(locale, { dateStyle: 'medium', timeZone: 'UTC' });
  const number = new Intl.NumberFormat(locale);
  const percent = new Intl.NumberFormat(locale, { style: 'percent', maximumFractionDigits: 0 });
  const percentExact = new Intl.NumberFormat(locale, { style: 'percent', maximumFractionDigits: 2 });
  const money = new Map<string, Intl.NumberFormat>();
  const toDate = (v: Date | string) => (typeof v === 'string' ? new Date(v) : v);
  return {
    date: (v: Date | string) => date.format(toDate(v)),
    dateTime: (v: Date | string) => dateTime.format(toDate(v)),
    /** The time of day of an instant, e.g. `8:45 am`, in the viewer's time zone. */
    time: (v: Date | string) => time.format(toDate(v)),
    /** The weekday of a contract `date` value, e.g. `Monday` for `2026-03-02`. */
    weekday: (v: string) => weekday.format(new Date(`${v.slice(0, 10)}T00:00:00Z`)),
    /** A contract `date` value, e.g. `2026-03-04`, without shifting it into the viewer's time zone. */
    day: (v: string) => day.format(new Date(`${v.slice(0, 10)}T00:00:00Z`)),
    number: (v: number) => number.format(v),
    percent: (fraction: number) => percent.format(fraction),
    /** A percentage that is compared with a limit: up to two decimals, so 74.6% never reads as 75%. */
    percentExact: (fraction: number) => percentExact.format(fraction),
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
