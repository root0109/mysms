import { SCREENS, matchRoute, routerPaths, type ClientKind } from '@mysms/core';
import { createElement, type ComponentType, type ReactNode } from 'react';
import { Navigate, type RouteObject } from 'react-router';
import { NotFoundScreen } from '../screens/NotFoundScreen';
import { PUBLIC_SCREENS, toRouterPath } from './paths';
import { RequireSession } from './RequireSession';
import { ScreenRoute } from './ScreenRoute';

export interface AppRouteOptions {
  client: ClientKind;
  /** Built screens by inventory id, or by "<id> <route>" (must include ACC-01, the sign-in screen). */
  built: Readonly<Record<string, ReactNode>>;
  /** Signed-in layout; renders an <Outlet/>. */
  Shell: ComponentType;
  /** Index page under /app. */
  Home: ComponentType;
  publicScreens?: ReadonlySet<string>;
}

/**
 * Routes for a web client, generated from the screen inventory: every distinct path of every screen the
 * client carries is mounted exactly once (query variants share their path); public screens sit outside
 * the session guard; built screens render, the rest render their placeholder.
 */
export function buildAppRoutes(options: AppRouteOptions): RouteObject[] {
  const { client, built, Shell, Home } = options;
  const publicScreens = options.publicScreens ?? PUBLIC_SCREENS;
  if (!built['ACC-01']) throw new Error('the sign-in screen (ACC-01) must be built');
  const paths = routerPaths(client);
  const expected = new Set(
    SCREENS.filter((s) => s.clients.includes(client)).flatMap((s) => s.routes.map((r) => r.split('?')[0])),
  );
  if (paths.length !== expected.size) throw new Error(`router paths do not cover the ${client} inventory`);
  const isPublic = (path: string) => publicScreens.has(matchRoute(path, client)?.screen.id ?? '');
  const screen = createElement(ScreenRoute, { client, built });
  return [
    { path: '/', element: createElement(Navigate, { to: '/app', replace: true }) },
    ...paths.filter(isPublic).map((p) => ({ path: toRouterPath(p), element: screen })),
    {
      path: '/app',
      element: createElement(RequireSession, null, createElement(Shell)),
      children: [
        { index: true, Component: Home },
        ...paths.filter((p) => !isPublic(p)).map((p) => ({ path: toRouterPath(p), element: screen })),
        { path: '*', Component: NotFoundScreen },
      ],
    },
    { path: '*', Component: NotFoundScreen },
  ];
}
