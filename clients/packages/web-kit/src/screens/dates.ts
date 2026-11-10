/** Today plus `offset` days as `YYYY-MM-DD`, in the viewer's own calendar. */
export function isoDay(offset: number, now: Date = new Date()): string {
  const day = new Date(now.getFullYear(), now.getMonth(), now.getDate() + offset);
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${day.getFullYear()}-${pad(day.getMonth() + 1)}-${pad(day.getDate())}`;
}
