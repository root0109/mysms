import { useBranding, useI18n } from '@mysms/core';
import { Card, TextInput } from '@mysms/ui-web';
import { useState } from 'react';
import { RetryBanner } from './shared';

/**
 * What the tenant's sign-in page shows (platform.GetBranding, public): name, brand colour, logo, locale and time zone.
 * The school code comes from the app's configuration when it serves one school; otherwise the user types it.
 */
export function BrandingCard({ tenantCode }: { tenantCode: string }) {
  const { t } = useI18n();
  const [code, setCode] = useState(tenantCode);
  const branding = useBranding(code);
  return (
    <Card title={t('plt.branding.title')}>
      <TextInput
        label={t('plt.branding.code')}
        hint={t('plt.branding.codeHint')}
        value={code}
        onChange={(event) => setCode(event.target.value)}
      />
      {code.trim().length === 0 ? null : branding.isError ? (
        <RetryBanner onRetry={() => void branding.refetch()} />
      ) : branding.isPending ? (
        <p className="hint">{t('common.state.loading')}</p>
      ) : (
        <dl className="facts">
          <dt>{t('plt.branding.name')}</dt>
          <dd>{branding.data.tenantName}</dd>
          <dt>{t('plt.branding.colour')}</dt>
          <dd>{branding.data.primaryColor ?? t('plt.theme.platformDefault')}</dd>
          <dt>{t('plt.branding.locale')}</dt>
          <dd>{branding.data.locale ?? ''}</dd>
          <dt>{t('plt.branding.timezone')}</dt>
          <dd>{branding.data.timezone ?? ''}</dd>
          <dt>{t('plt.branding.logo')}</dt>
          <dd>
            {branding.data.logoFileUrl ? (
              <img
                className="branding-logo"
                src={branding.data.logoFileUrl}
                alt={t('plt.branding.logoAlt')}
              />
            ) : (
              t('plt.theme.noLogo')
            )}
          </dd>
        </dl>
      )}
    </Card>
  );
}
