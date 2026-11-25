import { fieldIn } from '../records/api';
import { useAllRecords } from '../records/hooks';
import type { RecordRow } from '../records/types';
import { useCan, useSession } from './session';

const NONE: readonly RecordRow[] = [];
const idsOf = (rows: readonly RecordRow[], field: string) => [
  ...new Set(rows.flatMap((row) => (typeof row[field] === 'string' ? [row[field] as string] : []))),
];

/**
 * LIB-07 "my loans": the loans on the signed-in member's own library cards (their person's), newest first, with each
 * title's name. The server answers a member only their own cards and loans; the person filter keeps a librarian's
 * view, who reads the whole school's, to their own cards. A guardian reads no child's loans (library knows no
 * guardian link).
 */
export function useMyLoans() {
  const personId = useSession((s) => s.me?.user?.personId ?? null);
  const mayCards = useCan('library.library_card.read');
  const mayLoans = useCan('library.loan.read');
  const mayTitles = useCan('library.media.read');
  const whose = personId ? `personId = "${personId}"` : undefined;
  const cards = useAllRecords(
    'library.LibraryCard',
    { ...(whose ? { filter: whose } : {}) },
    mayCards && !!whose,
  );
  const cardIds = idsOf(cards.data?.items ?? NONE, 'id');
  const loans = useAllRecords(
    'library.Loan',
    { ...(cardIds.length ? { filter: fieldIn('libraryCardId', cardIds) } : {}), sort: ['createTime,desc'] },
    mayLoans && cardIds.length > 0,
  );
  const mediaIds = idsOf(loans.data?.items ?? NONE, 'mediaId');
  const titles = useAllRecords(
    'library.Media',
    { ...(mediaIds.length ? { filter: fieldIn('id', mediaIds) } : {}) },
    mayTitles && mediaIds.length > 0,
  );
  return {
    may: mayCards && mayLoans,
    cards: cards.data?.items ?? NONE,
    loans: loans.data?.items ?? NONE,
    titleOf: new Map((titles.data?.items ?? NONE).map((row) => [row.id, String(row.title ?? '')])),
    isPending:
      (mayCards && !!whose && cards.isPending) || (cardIds.length > 0 && mayLoans && loans.isPending),
    isError: cards.isError || loans.isError,
    refetch: () => void (cards.isError ? cards.refetch() : loans.refetch()),
  };
}

/**
 * TRN-03 "my route": how one student of the account's gets to school — their transport records, and for a school bus
 * its route (vehicle and times) and stops in order, with their own stop marked by its id.
 */
export function useMyTransport(studentId: string | null) {
  const mayTransport = useCan('transport.student_transport.read');
  const mayRoutes = useCan('transport.route.read');
  const mayStops = useCan('transport.route_stop.read');
  const transports = useAllRecords(
    'transport.StudentTransport',
    { ...(studentId ? { filter: `studentId = "${studentId}"` } : {}) },
    mayTransport && studentId !== null,
  );
  const routeIds = idsOf(transports.data?.items ?? NONE, 'routeId');
  const routeId = routeIds[0] ?? '';
  const routes = useAllRecords(
    'transport.Route',
    { ...(routeIds.length ? { filter: fieldIn('id', routeIds) } : {}) },
    mayRoutes && routeIds.length > 0,
  );
  const stops = useAllRecords(
    'transport.RouteStop',
    { parentId: routeId, sort: ['sequence'] },
    mayStops && !!routeId,
  );
  return {
    may: mayTransport,
    transports: transports.data?.items ?? NONE,
    routes: routes.data?.items ?? NONE,
    stops: stops.data?.items ?? NONE,
    isPending: mayTransport && studentId !== null && transports.isPending,
    isError: transports.isError || routes.isError || stops.isError,
    refetch: () => void transports.refetch(),
  };
}
