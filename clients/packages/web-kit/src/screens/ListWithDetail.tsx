import type { ReactNode } from 'react';
import { useLocation, useSearchParams } from 'react-router';
import { ResourceScreen } from './ResourceScreen';
import { RESOURCE_SCREENS } from './resourceScreens';

/**
 * A list manager whose rows open in place: the inventory gives the record type one route, so the chosen row is in
 * the address (`?<param>=<id>`) and the page shows its detail instead of the list; the detail's back link returns
 * to the list. Used where the inventory asks for a row's related records (a course's subjects, a route's stops, a
 * guardian's students) on a screen without a detail route.
 */
export function ListWithDetail({
  screenId,
  param,
  detail,
}: {
  screenId: string;
  param: string;
  detail: (id: string) => ReactNode;
}) {
  const [search] = useSearchParams();
  const { pathname } = useLocation();
  const id = search.get(param) ?? '';
  const screen = RESOURCE_SCREENS.find((each) => each.id === screenId);
  if (id) return detail(id);
  if (!screen) return null;
  return <ResourceScreen screen={screen} rowLink={(row) => `${pathname}?${param}=${row.id}`} />;
}
