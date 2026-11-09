import { can, useI18n, useSession, useSignOut, type PermissionCode } from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { AppShell, Button } from '@mysms/ui-web';
import { useEffect, useRef, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router';

export interface NavItem {
  to: string;
  label: MessageKey;
  /** Match only the exact path (for the home link). */
  end?: boolean;
  /** Shown only when the user holds this permission (the server enforces it either way). */
  permission?: PermissionCode;
  /** Shown when the user holds at least one of these (a screen serving many resources). */
  anyPermission?: readonly string[];
}

/** A titled group of navigation entries (ui/02 §2.1); shown only when the role may open one of them. */
export interface NavGroup {
  label: MessageKey;
  items: readonly NavItem[];
}

/** Whether a navigation entry is the page being shown (or a page below it). */
const isActive = (item: NavItem, path: string) =>
  item.end ? path === item.to : path === item.to || path.startsWith(item.to + '/');

/**
 * The signed-in web shell: navigation, user, sign-out and an optional environment band (ui/02 §1.2). A group opens
 * on its own when it holds the page being shown, so the last group (Setup) stays out of the way otherwise; the
 * "find a screen" box (`/` or Ctrl/⌘-K) searches every entry the role may open (ui/02 §2.1.2).
 */
export function NavShell({
  items,
  groups = [],
  environmentBand,
}: {
  items: readonly NavItem[];
  groups?: readonly NavGroup[];
  environmentBand?: MessageKey;
}) {
  const { t } = useI18n();
  const username = useSession((s) => s.me?.user?.username ?? '');
  const permissions = useSession((s) => s.me?.permissions);
  const allowed = (item: NavItem) =>
    (!item.permission || can(permissions, item.permission)) &&
    (!item.anyPermission || item.anyPermission.some((code) => can(permissions, code as PermissionCode)));
  const visible = items.filter(allowed);
  const visibleGroups = groups
    .map((group) => ({ label: group.label, items: group.items.filter(allowed) }))
    .filter((group) => group.items.length > 0);
  const signOut = useSignOut();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const [query, setQuery] = useState('');
  const search = useRef<HTMLInputElement>(null);
  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      const target = event.target;
      const typing =
        target instanceof Element &&
        target.closest('input, textarea, select, [contenteditable="true"]') !== null;
      if ((event.key === 'k' && (event.metaKey || event.ctrlKey)) || (event.key === '/' && !typing)) {
        event.preventDefault();
        search.current?.focus();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);
  const needle = query.trim().toLowerCase();
  const matches = needle
    ? [...visible, ...visibleGroups.flatMap((group) => group.items)]
        .filter((item) => t(item.label).toLowerCase().includes(needle))
        .slice(0, 12)
    : [];
  return (
    <AppShell
      brand={t('app.brand.name')}
      environmentBand={environmentBand ? t(environmentBand) : undefined}
      navLabel={t('shell.nav.label')}
      skipLinkLabel={t('shell.nav.skip')}
      nav={
        <>
          <div className="shell-find" role="search">
            <input
              ref={search}
              type="search"
              className="shell-find-input"
              placeholder={t('shell.find.placeholder')}
              aria-label={t('shell.find.label')}
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter' && matches[0]) {
                  navigate(matches[0].to);
                  setQuery('');
                } else if (event.key === 'Escape') {
                  setQuery('');
                }
              }}
            />
            {needle && (
              <ul className="shell-nav shell-find-results" aria-label={t('shell.find.results')}>
                {matches.length === 0 && <li className="hint">{t('shell.find.none')}</li>}
                {matches.map((item) => (
                  <li key={item.to}>
                    <NavLink to={item.to} end={item.end ?? false} onClick={() => setQuery('')}>
                      {t(item.label)}
                    </NavLink>
                  </li>
                ))}
              </ul>
            )}
          </div>
          <ul className="shell-nav">
            {visible.map((item) => (
              <li key={item.to}>
                <NavLink to={item.to} end={item.end ?? false}>
                  {t(item.label)}
                </NavLink>
              </li>
            ))}
          </ul>
          {visibleGroups.map((group) => (
            <details
              key={group.label}
              className="shell-nav-group"
              open={group.items.some((item) => isActive(item, pathname)) || undefined}
            >
              <summary>{t(group.label)}</summary>
              <ul className="shell-nav">
                {group.items.map((item) => (
                  <li key={item.to}>
                    <NavLink to={item.to} end={item.end ?? false}>
                      {t(item.label)}
                    </NavLink>
                  </li>
                ))}
              </ul>
            </details>
          ))}
        </>
      }
      actions={
        <>
          <span className="shell-user">{username}</span>
          <Button
            variant="secondary"
            loading={signOut.isPending}
            onClick={() =>
              signOut.mutate(undefined, { onSettled: () => navigate('/app/auth/login', { replace: true }) })
            }
          >
            {t('shell.user.signOut')}
          </Button>
        </>
      }
    >
      <Outlet />
    </AppShell>
  );
}
