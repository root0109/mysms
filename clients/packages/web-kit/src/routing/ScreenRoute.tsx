import { matchRoute, type ClientKind } from '@mysms/core';
import type { ReactNode } from 'react';
import { useLocation } from 'react-router';
import { NotFoundScreen } from '../screens/NotFoundScreen';
import { ScreenPlaceholder } from '../screens/ScreenPlaceholder';

/**
 * Resolves the current path and query to its inventory screen (query variants such as `?mine` or
 * `?view=room` share a path) and renders the built screen or its placeholder. `built` is keyed by screen
 * id, or by "<id> <route>" for a screen whose routes are built one at a time.
 */
export function ScreenRoute({
  client,
  built,
}: {
  client: ClientKind;
  built: Readonly<Record<string, ReactNode>>;
}) {
  const { pathname, search } = useLocation();
  const match = matchRoute(
    pathname.replace(/^\/app/, '') || '/',
    client,
    undefined,
    new URLSearchParams(search),
  );
  if (!match) return <NotFoundScreen />;
  // a screen with several routes can be built route by route: "<ID> <route>" wins over "<ID>"
  return (
    built[`${match.screen.id} ${match.route}`] ??
    built[match.screen.id] ?? <ScreenPlaceholder screenId={match.screen.id} title={match.screen.title} />
  );
}
