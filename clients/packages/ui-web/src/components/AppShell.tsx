import type { ReactNode } from 'react';

export interface AppShellProps {
  brand: ReactNode;
  /** Navigation links (rendered by the app's router). */
  nav: ReactNode;
  navLabel: string;
  /** Top-bar trailing area: switchers, notifications, user menu. */
  actions?: ReactNode;
  skipLinkLabel: string;
  /** Environment band text, e.g. the platform console's red band (ui/02 §1.2). */
  environmentBand?: string | undefined;
  children: ReactNode;
}

/** Web shell (ui/04 §3): 56 px top bar, 264 px left navigation, skip-to-content, single <main>. */
export function AppShell({
  brand,
  nav,
  navLabel,
  actions,
  skipLinkLabel,
  environmentBand,
  children,
}: AppShellProps) {
  return (
    <div className="sms-shell">
      <a className="sms-shell__skip" href="#main">
        {skipLinkLabel}
      </a>
      {environmentBand ? <div className="sms-shell__band">{environmentBand}</div> : null}
      <header className="sms-shell__top">
        <div className="sms-shell__brand">{brand}</div>
        <div className="sms-shell__actions">{actions}</div>
      </header>
      <div className="sms-shell__body">
        <nav className="sms-shell__nav" aria-label={navLabel}>
          {nav}
        </nav>
        <main id="main" className="sms-shell__main" tabIndex={-1}>
          {children}
        </main>
      </div>
    </div>
  );
}
