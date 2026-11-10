import {
  buildPath,
  isApiError,
  screenById,
  useCan,
  useI18n,
  useTakeAttendanceForSession,
  type RecordRow,
} from '@mysms/core';
import { Button } from '@mysms/ui-web';
import { useNavigate } from 'react-router';

/**
 * Flow F3 from the timetable: opens the attendance sheet of a session — the existing one, or a new sheet with every
 * expected student marked present — and goes to it (ATT-03). Offered to roles that may take attendance for a
 * session and open sheets; the server decides whether this caller may for this session.
 */
export function TakeAttendanceButton({ row }: { row: RecordRow }) {
  const { t } = useI18n();
  const navigate = useNavigate();
  const mayTake = useCan('attendance.attendance_sheet.take_attendance_for_session');
  const mayOpen = useCan('attendance.attendance_sheet.read');
  const take = useTakeAttendanceForSession();
  const route = screenById('ATT-03')?.routes[0];
  if (!mayTake || !mayOpen || !route) return null;
  const error = take.error;
  return (
    <>
      <Button
        variant="secondary"
        loading={take.isPending}
        onClick={() =>
          take.mutate(row.id, {
            onSuccess: (sheet) => {
              if (sheet.id) void navigate(`/app${buildPath(route, { attendanceSheetId: sheet.id })}`);
            },
          })
        }
      >
        {t('att.take.forSession')}
      </Button>
      {error ? (
        <p className="sms-field__error" role="alert">
          {isApiError(error) && error.status === 409
            ? (error.detail ?? t('common.error.generic'))
            : isApiError(error) && error.status === 403
              ? t('att.take.forbidden')
              : isApiError(error) && error.status === 503
                ? t('att.take.sessionUnavailable')
                : t('common.error.generic')}
        </p>
      ) : null}
    </>
  );
}
