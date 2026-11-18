import {
  can,
  parseQuery,
  pathOf,
  searchOf,
  useI18n,
  useSession,
  useSignOut,
  type PermissionCode,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { AppShell, Button } from '@mysms/ui-web';
import { useEffect, useRef, useState } from 'react';
import { Link, Outlet, useLocation, useNavigate } from 'react-router';
import { TermsGate } from '../screens/account/TermsScreen';
import { ImpersonationBanner, PersonaSwitcher } from './AccountBar';

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

/** Whether a navigation entry leads to the page being shown (or a page below it), by path alone. */
const isActive = (item: NavItem, path: string) => {
  const to = pathOf(item.to);
  return item.end ? path === to : path === to || path.startsWith(to + '/');
};

/**
 * The one entry that is the page being shown. Two screens can share a path and differ by query (the homework board
 * and its approval queue): the entry whose query the address carries wins, then the longest path.
 */
function currentEntry(items: readonly NavItem[], path: string, search: string): string | null {
  const query = parseQuery(search);
  let best: { to: string; rank: number } | null = null;
  for (const item of items) {
    if (!isActive(item, path)) continue;
    const wanted = [...new URLSearchParams(searchOf(item.to))];
    if (!wanted.every(([key, value]) => query.get(key) === value)) continue;
    const rank = wanted.length * 10_000 + pathOf(item.to).length;
    if (!best || rank > best.rank) best = { to: item.to, rank };
  }
  return best?.to ?? null;
}

/**
 * The signed-in web shell: navigation, the persona switcher (ACC-06), user, sign-out, the impersonation banner, the
 * terms gate (ACC-04) and an optional environment band (ui/02 §1.2). A group opens
 * on its own when it holds the page being shown, so the last group (Setup) stays out of the way otherwise; the
 * "find a screen" box (`/` or Ctrl/⌘-K) searches every entry the role may open (ui/02 §2.1.2).
 */
export function NavShell({
  items,
  groups = [],
  environmentBand,
  termsGate = false,
}: {
  items: readonly NavItem[];
  groups?: readonly NavGroup[];
  environmentBand?: MessageKey;
  /** Tenant apps ask for the terms (ACC-04) before any page; the platform console does not. */
  termsGate?: boolean;
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
  const { pathname, search: locationSearch } = useLocation();
  const current = currentEntry(
    [...visible, ...visibleGroups.flatMap((group) => group.items)],
    pathname,
    locationSearch,
  );
  const mark = (item: NavItem) => (item.to === current ? ('page' as const) : undefined);
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
                    <Link to={item.to} aria-current={mark(item)} onClick={() => setQuery('')}>
                      {t(item.label)}
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </div>
          <ul className="shell-nav">
            {visible.map((item) => (
              <li key={item.to}>
                <Link to={item.to} aria-current={mark(item)}>
                  {t(item.label)}
                </Link>
              </li>
            ))}
          </ul>
          {visibleGroups.map((group) => (
            <details
              key={group.label}
              className="shell-nav-group"
              open={group.items.some((item) => item.to === current) || undefined}
            >
              <summary>{t(group.label)}</summary>
              <ul className="shell-nav">
                {group.items.map((item) => (
                  <li key={item.to}>
                    <Link to={item.to} aria-current={mark(item)}>
                      {t(item.label)}
                    </Link>
                  </li>
                ))}
              </ul>
            </details>
          ))}
        </>
      }
      actions={
        <>
          <PersonaSwitcher />
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
      <ImpersonationBanner />
      {termsGate ? (
        <TermsGate>
          <Outlet />
        </TermsGate>
      ) : (
        <Outlet />
      )}
    </AppShell>
  );
}
