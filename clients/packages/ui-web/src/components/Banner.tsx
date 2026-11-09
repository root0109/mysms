import type { ReactNode } from 'react';
import type { Intent } from '@mysms/tokens';

export interface BannerProps {
  intent?: Intent;
  title?: string;
  children?: ReactNode;
  /** Optional action area (e.g. a retry button). */
  action?: ReactNode;
}

/** Banner / Callout. Danger and warning banners are announced (role=alert), others are polite status. */
export function Banner({ intent = 'info', title, children, action }: BannerProps) {
  const urgent = intent === 'danger' || intent === 'warning';
  return (
    <div className={`sms-banner sms-banner--${intent}`} role={urgent ? 'alert' : 'status'}>
      <div className="sms-banner__body">
        {title ? <p className="sms-banner__title">{title}</p> : null}
        {children}
      </div>
      {action ? <div className="sms-banner__action">{action}</div> : null}
    </div>
  );
}
