package com.mysms.platform.business;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.platform.api.model.Branding;
import com.mysms.platform.api.model.Theme;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.domain.TenantEntity;
import com.mysms.platform.repository.TenantRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * platform.GetBranding, public: what a sign-in page and the PWA manifest show before anyone signs in — the tenant's
 * name, locale and time zone, and from its default theme the brand colour ({@code color.primary}) and a short-lived
 * logo URL. Nothing else: no ids, contacts or settings. A closed or unknown tenant code is 404. The institution's own
 * name and theme live in org (org.institution), which the platform may not read and whose events carry only ids, so an
 * {@code institutionId} is answered with the tenant's branding.
 */
@Service
public class BrandingService extends AppTransactionManager {

    static final String PRIMARY_COLOUR = "color.primary";

    private final TenantRepository tenants;
    private final ThemeService themes;
    private final FileObjectService files;

    public BrandingService(TenantRepository tenants, ThemeService themes, FileObjectService files,
            @Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PlatformDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions) {
        super(database, transactions);
        this.tenants = tenants;
        this.themes = themes;
        this.files = files;
    }

    public Branding getBranding(String tenantCode, UUID institutionId) {
        TenantEntity tenant = doInGlobalReadTransaction(() -> tenants.findByCode(tenantCode))
                .filter(t -> !"closed".equals(t.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException(TenantService.AGGREGATE, tenantCode));
        Branding branding = new Branding(tenant.getName()).locale(tenant.getLocale()).timezone(tenant.getTimezone());
        String trace = CallContextHolder.current().map(CallContext::traceId).orElseThrow();
        return CallContextHolder.callAs(CallContext.anonymous(trace).withIdentity(tenant.getId(), null), () -> {
            Optional<Theme> theme = themes.defaultTheme();
            theme.map(Theme::getTokens).filter(Map.class::isInstance).map(tokens -> ((Map<?, ?>) tokens)
                    .get(PRIMARY_COLOUR)).map(String::valueOf).ifPresent(branding::setPrimaryColor);
            theme.map(Theme::getLogoFileId).flatMap(files::publicUrl).ifPresent(branding::setLogoFileUrl);
            return branding;
        });
    }
}
