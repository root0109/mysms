import { useI18n, useSession, useStopImpersonation, useSwitchRole, type RoleGrant } from '@mysms/core';
import { Banner, Button, Dialog } from '@mysms/ui-web';
import { useState } from 'react';

const grantKey = (g: Pick<RoleGrant, 'roleId' | 'scopeId'>) => `${g.roleId ?? ''}/${g.scopeId ?? ''}`;

/** A grant as the switcher names it: the role, and the unit it is limited to unless it is tenant-wide. */
function useGrantLabel() {
  const { t } = useI18n();
  return (g: RoleGrant) =>
    g.scopeType === 'tenant'
      ? t('acc.persona.tenantWide', { role: g.roleCode })
      : t('acc.persona.scoped', { role: g.roleCode, scope: g.scopeType });
}

/**
 * ACC-06 Persona switcher (a sheet): for a user with more than one role grant, picks the one to work in
 * (iam.SwitchRole). The answer's tokens and Me replace the session's, so navigation and every check follow at once.
 */
export function PersonaSwitcher() {
  const { t } = useI18n();
  const roles = useSession((s) => s.me?.roles ?? []);
  const active = useSession((s) => s.me?.activeRole);
  const impersonating = useSession((s) => Boolean(s.impersonation));
  const label = useGrantLabel();
  const switchRole = useSwitchRole();
  const [open, setOpen] = useState(false);
  if (roles.length < 2 || impersonating) return null;
  return (
    <>
      <Button variant="secondary" onClick={() => setOpen(true)}>
        {active ? label(active) : t('acc.persona.allRoles')}
      </Button>
      <Dialog open={open} title={t('acc.persona.title')} onClose={() => setOpen(false)}>
        <p className="hint">{t('acc.persona.intro')}</p>
        {switchRole.isError ? <Banner intent="danger">{t('acc.persona.failed')}</Banner> : null}
        <ul className="persona-list">
          {roles.map((grant) => {
            const current = Boolean(active) && grantKey(grant) === grantKey(active ?? {});
            return (
              <li key={grantKey(grant)}>
                <Button
                  variant={current ? 'primary' : 'ghost'}
                  aria-pressed={current}
                  disabled={!grant.roleId}
                  loading={switchRole.isPending && switchRole.variables?.roleId === grant.roleId}
                  onClick={() =>
                    switchRole.mutate(
                      {
                        roleId: grant.roleId as string,
                        ...(grant.scopeId ? { scopeId: grant.scopeId } : {}),
                      },
                      { onSuccess: () => setOpen(false) },
                    )
                  }
                >
                  {label(grant)}
                </Button>
              </li>
            );
          })}
        </ul>
      </Dialog>
    </>
  );
}

/**
 * While an administrator acts as another user (ADM-09): a banner that stays on every page, naming the user, with the
 * way back (iam.StopImpersonation).
 */
export function ImpersonationBanner() {
  const { t } = useI18n();
  const impersonating = useSession((s) => Boolean(s.impersonation));
  const username = useSession((s) => s.me?.user?.username ?? '');
  const stop = useStopImpersonation();
  if (!impersonating) return null;
  return (
    <Banner
      intent="warning"
      action={
        <Button variant="secondary" loading={stop.isPending} onClick={() => stop.mutate()}>
          {t('acc.impersonation.stop')}
        </Button>
      }
    >
      {t('acc.impersonation.banner', { username })}
    </Banner>
  );
}
