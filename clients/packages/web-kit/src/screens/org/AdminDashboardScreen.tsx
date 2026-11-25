import {
  can,
  localDay,
  resourceDef,
  useI18n,
  useRecordTotal,
  useSession,
  type PermissionCode,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Banner, Card, Skeleton } from '@mysms/ui-web';
import { Link } from 'react-router';

/** One count of the dashboard: what is counted, how, and the screen that works on those rows. */
interface Tile {
  label: MessageKey;
  resource: string;
  filter: (today: string) => string;
  to: string;
}

/** ui/02 §2.1.1 Administrator: the work waiting for the school, each count opening the screen that does it. */
const TILES: readonly Tile[] = [
  {
    label: 'org.overview.applications',
    resource: 'admission.Application',
    filter: () => 'status = "submitted" OR status = "pending" OR status = "confirmed"',
    to: '/app/admission/applications',
  },
  {
    label: 'org.overview.leave',
    resource: 'hr.LeaveApplication',
    filter: () => 'status = "pending"',
    to: '/app/hr/leave-applications',
  },
  {
    label: 'org.overview.sheets',
    resource: 'attendance.AttendanceSheet',
    filter: (today) => `attendanceDate = "${today}" AND (status = "draft" OR status = "in_progress")`,
    to: '/app/attendance/attendance-sheets',
  },
  {
    label: 'org.overview.feedback',
    resource: 'comms.FeedbackThread',
    filter: () => 'status = "open"',
    to: '/app/comms/feedback-threads?status=open',
  },
  {
    label: 'org.overview.events',
    resource: 'comms.Event',
    filter: (today) => `startAt >= "${today}T00:00:00Z" AND status = "active"`,
    to: '/app/comms/events',
  },
];

function Count({ tile, today }: { tile: Tile; today: string }) {
  const { t, format } = useI18n();
  const total = useRecordTotal(tile.resource, tile.filter(today));
  const label = t(tile.label);
  return (
    <Card title={label}>
      {total.isPending ? (
        <Skeleton label={t('common.state.loading')} lines={1} />
      ) : total.isError ? (
        <p className="hint">{t('common.error.generic')}</p>
      ) : (
        <p className="stat-tile__value">
          {total.data.more
            ? t('org.overview.countMore', { count: format.number(total.data.count) })
            : format.number(total.data.count)}
        </p>
      )}
      <Link to={tile.to} aria-label={t('org.overview.openLabel', { what: label })}>
        {t('org.overview.open')}
      </Link>
    </Card>
  );
}

/**
 * ADM-01 Admin dashboard: what waits for the school's administrators — admission applications to process, leave to
 * decide, today's attendance sheets not yet complete, open feedback threads and upcoming events — each as a count
 * that opens the screen doing that work. A count is shown only to a role that may list its records, and the server
 * counts within the caller's institutions; a role with none of them is told so.
 */
export function AdminDashboardScreen() {
  const { t, timeZone } = useI18n();
  const permissions = useSession((s) => s.me?.permissions);
  const today = localDay(new Date(), timeZone);
  const shown = TILES.filter((tile) => {
    const code = resourceDef(tile.resource)?.ops.List?.permission;
    return code !== undefined && can(permissions, code as PermissionCode);
  });
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.ADM01.title')}</h1>
        <p className="page__subtitle">{t('org.overview.intro')}</p>
      </header>
      {shown.length === 0 ? (
        <Banner intent="info">{t('org.overview.noAccess')}</Banner>
      ) : (
        <div className="stat-tiles">
          {shown.map((tile) => (
            <Count key={tile.label} tile={tile} today={today} />
          ))}
        </div>
      )}
    </div>
  );
}
