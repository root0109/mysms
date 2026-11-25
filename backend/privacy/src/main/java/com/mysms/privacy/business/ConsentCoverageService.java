package com.mysms.privacy.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.privacy.api.model.ConsentCoverage;
import com.mysms.privacy.api.model.ConsentCoverageDenial;
import com.mysms.privacy.api.model.ConsentCoverageScope;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.model.ConsentReason;
import com.mysms.privacy.model.ConsentScope;
import com.mysms.privacy.repository.ConsentCoverageRepository;
import com.mysms.privacy.repository.PurposeRepository;
import com.mysms.privacy.repository.PurposeView;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Consent coverage (design §6.1): how many of the tenant's data principals — or of one institution's students — a
 * purpose is allowed for at each width, how many are denied and why, and how many have no decision that counts.
 * Counts only, never who: a coverage report is for planning (which classes still need forms), not for finding people.
 * The whole tenant needs a tenant-wide grant; an institution must be within the caller's grant (403).
 */
@Service
public class ConsentCoverageService extends AppTransactionManager {

    static final String PERMISSION_READ = "privacy.coverage.read";
    static final String PURPOSE = "privacy.purpose";
    /** The reasons a stored answer denies a width for, other than silence, in the order the contract lists them. */
    private static final List<ConsentReason> DENIALS = List.of(ConsentReason.REFUSED, ConsentReason.NARROWER_GRANT,
            ConsentReason.EXPIRED, ConsentReason.RECONSENT_REQUIRED, ConsentReason.FORBIDDEN_FOR_MINOR,
            ConsentReason.PURPOSE_DISABLED);

    private final ConsentCoverageRepository repository;
    private final PurposeRepository purposes;

    public ConsentCoverageService(ConsentCoverageRepository repository, PurposeRepository purposes,
            @Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PrivacyDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions) {
        super(database, transactions);
        this.repository = repository;
        this.purposes = purposes;
    }

    /**
     * privacy.GetConsentCoverage: the counts of the purpose (404 when the catalogue has none) at every width it takes,
     * or at the one asked for (422 when it takes no such width). A subject with no stored answer has decided nothing
     * that counts, so it is counted as not answered.
     */
    public ConsentCoverage getConsentCoverage(String purposeCode, String scope, UUID institutionId) {
        requireReach(institutionId);
        Optional<ConsentScope> asked = Optional.ofNullable(scope).filter(s -> !s.isBlank())
                .map(ConsentCoverageService::width);
        return doInReadTransaction(() -> {
            PurposeView purpose = purposes.findByCode(purposeCode)
                    .orElseThrow(() -> new ResourceNotFoundException(PURPOSE, purposeCode));
            List<ConsentScope> widths = ConsentScope.forKind(purpose.scopeKind());
            if (asked.isPresent() && !widths.contains(asked.get())) {
                throw new ValidationFailedException(List.of(new FieldError("scope",
                        "is not a width the purpose takes", "SCOPE_NOT_APPLICABLE")));
            }
            long subjects = repository.subjects(institutionId);
            List<ConsentCoverageRepository.Count> counts = repository.counts(purposeCode, institutionId);
            List<ConsentCoverageScope> rows = widths.stream()
                    .filter(w -> asked.isEmpty() || asked.get() == w)
                    .map(w -> row(w, subjects, counts))
                    .toList();
            return new ConsentCoverage().purposeCode(purposeCode).institutionId(institutionId)
                    .subjects(Math.toIntExact(subjects)).scopes(rows);
        });
    }

    /** One width's counts: allowed, each denial, and the rest of the subjects as not answered. */
    private static ConsentCoverageScope row(ConsentScope width, long subjects,
            List<ConsentCoverageRepository.Count> counts) {
        long allowed = count(counts, width, ConsentReason.CONSENT);
        List<ConsentCoverageDenial> denied = DENIALS.stream()
                .map(reason -> new ConsentCoverageDenial()
                        .reason(ConsentCoverageDenial.ReasonEnum.fromValue(reason.name().toLowerCase(Locale.ROOT)))
                        .subjects(Math.toIntExact(count(counts, width, reason))))
                .toList();
        long deniedTotal = denied.stream().mapToLong(ConsentCoverageDenial::getSubjects).sum();
        return new ConsentCoverageScope()
                .scope(ConsentCoverageScope.ScopeEnum.fromValue(width.value()))
                .allowed(Math.toIntExact(allowed))
                .notAnswered(Math.toIntExact(subjects - allowed - deniedTotal))
                .denied(denied);
    }

    private static long count(List<ConsentCoverageRepository.Count> counts, ConsentScope width, ConsentReason reason) {
        return counts.stream().filter(c -> width.value().equals(c.scope()) && reason.name().equals(c.reason()))
                .mapToLong(ConsentCoverageRepository.Count::subjects).sum();
    }

    /** The whole tenant needs a tenant-wide grant of the permission, an institution one that reaches it (403). */
    private static void requireReach(UUID institutionId) {
        DataScope reach = DataScope.current(PERMISSION_READ);
        boolean allowed = institutionId == null ? reach.all() : reach.canRead(Optional.of(institutionId));
        if (!allowed) {
            throw new PermissionDeniedException(institutionId == null
                    ? "Counts over the whole tenant need a tenant-wide grant; name an institution."
                    : "The institution is outside the caller's scope.");
        }
    }

    private static ConsentScope width(String value) {
        try {
            return ConsentScope.of(value);
        } catch (IllegalArgumentException e) {
            throw new ValidationFailedException(List.of(new FieldError("scope",
                    "must be one of class, institution, inter_school, public, tenant", "INVALID_ENUM")));
        }
    }
}
