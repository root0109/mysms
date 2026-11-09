import type { ComponentPropsWithoutRef, ReactNode } from 'react';

export interface CardProps extends ComponentPropsWithoutRef<'section'> {
  title?: string;
  actions?: ReactNode;
}

export function Card({ title, actions, children, className, ...rest }: CardProps) {
  return (
    <section {...rest} className={['sms-card', className ?? ''].filter(Boolean).join(' ')}>
      {title || actions ? (
        <header className="sms-card__header">
          {title ? <h2 className="sms-card__title">{title}</h2> : null}
          {actions ? <div className="sms-card__actions">{actions}</div> : null}
        </header>
      ) : null}
      {children}
    </section>
  );
}
