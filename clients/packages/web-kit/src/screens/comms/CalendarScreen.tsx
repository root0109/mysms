import {
  calendarDays,
  calendarRange,
  localDay,
  shiftCalendar,
  shiftDay,
  useCan,
  useExportCalendar,
  useI18n,
  useMyCalendar,
  type CalendarItem,
  type CalendarProvider,
  type CalendarView,
} from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import { Banner, Button, Card, SegmentedControl, Skeleton } from '@mysms/ui-web';
import { useState } from 'react';

const VIEWS: readonly CalendarView[] = ['month', 'week', 'day'];
const PROVIDERS: readonly CalendarProvider[] = ['google', 'microsoft'];

/**
 * COM-03 My calendar: what the signed-in account's calendar holds — school, class and cohort events it is invited
 * to, its personal events, due dates and holidays, merged by the server (comms.GetMyCalendar) — for a month, a week
 * or a day, each item on its day in the viewer's time zone. The range can be sent to the account's Google or
 * Microsoft calendar (comms.ExportCalendar, which runs as a background operation).
 */
export function CalendarScreen() {
  const { t, format, timeZone } = useI18n();
  const mayRead = useCan('comms.event.get_my_calendar');
  const mayExport = useCan('comms.calendar_export.export_calendar');
  const [view, setView] = useState<CalendarView>('month');
  const [day, setDay] = useState(() => localDay(new Date(), timeZone));
  const days = calendarDays(view, day);
  const range = calendarRange(days);
  const calendar = useMyCalendar(mayRead ? range : null);
  const exportTo = useExportCalendar();
  const byDay = new Map<string, CalendarItem[]>();
  for (const item of calendar.data?.items ?? []) {
    const on = localDay(item.startAt, timeZone);
    if (on < days.from || on > days.to) continue;
    byDay.set(on, [...(byDay.get(on) ?? []), item]);
  }
  const shown = [...byDay.keys()].sort();
  const heading =
    view === 'month'
      ? format.month(days.from)
      : view === 'week'
        ? t('comms.calendar.week', { from: format.day(days.from), to: format.day(days.to) })
        : format.day(day);
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.COM03.title')}</h1>
        <p className="page__subtitle">{t('comms.calendar.intro')}</p>
      </header>
      {!mayRead ? (
        <Banner intent="info">{t('adm.resource.noAccess')}</Banner>
      ) : (
        <>
          <SegmentedControl
            label={t('comms.calendar.view')}
            options={VIEWS.map((value) => ({ value, label: t(`comms.calendar.view.${value}`) }))}
            value={view}
            onChange={setView}
          />
          <div className="chip-row">
            <Button variant="secondary" onClick={() => setDay(shiftCalendar(view, day, -1))}>
              {t('comms.calendar.previous')}
            </Button>
            <Button variant="secondary" onClick={() => setDay(localDay(new Date(), timeZone))}>
              {t('comms.calendar.today')}
            </Button>
            <Button variant="secondary" onClick={() => setDay(shiftCalendar(view, day, 1))}>
              {t('comms.calendar.next')}
            </Button>
          </div>
          <h2 aria-live="polite">{heading}</h2>
          {calendar.isPending ? <Skeleton label={t('common.state.loading')} lines={5} /> : null}
          {calendar.isError ? (
            <Banner
              intent="danger"
              action={
                <Button variant="secondary" onClick={() => void calendar.refetch()}>
                  {t('common.action.retry')}
                </Button>
              }
            >
              {t('common.error.generic')}
            </Banner>
          ) : null}
          {calendar.isSuccess && shown.length === 0 ? (
            <p className="hint">{t('comms.calendar.empty')}</p>
          ) : null}
          {shown.map((on) => (
            <Card key={on} title={`${format.weekday(on)}, ${format.day(on)}`}>
              <ul className="leave-list">
                {(byDay.get(on) ?? []).map((item) => (
                  <li key={`${item.itemType}-${item.id}`} className="leave-list__item">
                    <div className="leave-list__what">
                      <span>{item.title}</span>
                      <span className="hint">
                        {[
                          enumLabel(t, 'calendarItem', item.itemType),
                          item.allDay
                            ? t('comms.calendar.allDay')
                            : `${format.time(item.startAt)}${item.endAt ? ` – ${format.time(item.endAt)}` : ''}`,
                          item.isCancelled ? t('comms.calendar.cancelled') : '',
                          item.isCompleted ? t('comms.calendar.completed') : '',
                        ]
                          .filter(Boolean)
                          .join(' · ')}
                      </span>
                    </div>
                  </li>
                ))}
              </ul>
            </Card>
          ))}
          {mayExport ? (
            <Card title={t('comms.calendar.export')}>
              <p className="hint">
                {t('comms.calendar.exportHint', { from: format.day(days.from), to: format.day(days.to) })}
              </p>
              {exportTo.isSuccess ? <Banner intent="success">{t('comms.calendar.exported')}</Banner> : null}
              {exportTo.isError ? <Banner intent="danger">{t('common.error.generic')}</Banner> : null}
              <div className="chip-row">
                {PROVIDERS.map((provider) => (
                  <Button
                    key={provider}
                    variant="secondary"
                    loading={exportTo.isPending && exportTo.variables?.provider === provider}
                    onClick={() =>
                      exportTo.mutate({
                        provider,
                        from: `${days.from}T00:00:00Z`,
                        to: `${shiftDay(days.to, 1)}T00:00:00Z`,
                      })
                    }
                  >
                    {t(`comms.calendar.exportTo.${provider}`)}
                  </Button>
                ))}
              </div>
            </Card>
          ) : null}
        </>
      )}
    </div>
  );
}
