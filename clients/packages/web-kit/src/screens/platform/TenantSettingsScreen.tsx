import { useI18n } from '@mysms/core';
import { Banner } from '@mysms/ui-web';
import { BrandingCard } from './BrandingCard';

/**
 * ADM-15 Tenant settings: read-only, from the tenant's public branding (ui/07 D-12: the tenant self-service resource
 * is pending P1-28; the values are changed by platform staff in the console, the brand on ADM-07).
 */
export function TenantSettingsScreen({ tenantCode = '' }: { tenantCode?: string }) {
  const { t } = useI18n();
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.ADM15.title')}</h1>
        <p className="page__subtitle">{t('plt.tenant.intro')}</p>
      </header>
      <Banner intent="info">{t('plt.tenant.readOnly')}</Banner>
      <BrandingCard tenantCode={tenantCode} />
    </div>
  );
}
