package com.mysms.platform.org.business;

import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.master.v1.GetCityRequest;
import com.mysms.master.v1.ListCountriesRequest;
import com.mysms.platform.org.api.model.InstitutionCard;
import com.mysms.platform.org.config.OrgDatabaseConfig;
import com.mysms.platform.org.integration.MasterGrpcClient;
import com.mysms.platform.org.repository.DirectoryRepository;
import com.mysms.platform.org.repository.DirectoryRepository.Listing;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * org.FindInstitutions: the public school finder of the mobile apps, before anyone has signed in. It searches every
 * tenant's schools, so it reads only org's global directories — a school's name, code, city and its tenant's sign-in
 * code — and finds only active schools of active tenants whose administrators opted in (the iam setting
 * {@code directory.listedInSchoolFinder}, off by default). Pages follow the name, then the id. The query is three
 * characters at least; a country narrows it (by ISO code, through master). City names come from master and are left
 * out when master cannot answer.
 */
@Service
public class SchoolFinder extends AppTransactionManager {

    static final int MIN_QUERY = 3;
    private static final Pattern ISO2 = Pattern.compile("^[A-Za-z]{2}$");
    private static final Logger LOG = LoggerFactory.getLogger(SchoolFinder.class);

    private final DirectoryRepository directory;
    private final MasterGrpcClient master;
    private final Clock clock;

    public SchoolFinder(DirectoryRepository directory, MasterGrpcClient master,
            @Qualifier(OrgDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(OrgDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions, Clock clock) {
        super(database, transactions);
        this.directory = directory;
        this.master = master;
        this.clock = clock;
    }

    public Page<InstitutionCard> findInstitutions(String query, String countryCode, Integer pageNumber,
            Integer pageSize, String cursor) {
        String text = query == null ? "" : query.trim();
        if (text.length() < MIN_QUERY || text.length() > 200) {
            throw new ValidationFailedException(List.of(new FieldError("query",
                    "give between " + MIN_QUERY + " and 200 characters of the name or code", "Size")));
        }
        if (countryCode != null && !countryCode.isBlank() && !ISO2.matcher(countryCode.trim()).matches()) {
            throw new ValidationFailedException(List.of(new FieldError("countryCode",
                    "must be an ISO 3166-1 alpha-2 code", "Pattern")));
        }
        String country = countryCode == null || countryCode.isBlank() ? null
                : countryCode.trim().toUpperCase(Locale.ROOT);
        SortSpec sort = SortSpec.parse(null, DirectoryRepository.SORT_FIELDS, DirectoryRepository.DEFAULT_ORDER,
                DirectoryRepository.ID_COLUMN);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort,
                text.toLowerCase(Locale.ROOT) + "|" + (country == null ? "" : country), clock);
        Optional<UUID> countryId = country == null ? Optional.empty() : countryOf(country);
        if (country != null && countryId.isEmpty()) {
            return new Page<>(List.of(), page.page(), page.size(), "");
        }
        Page<Listing> found = doInGlobalReadTransaction(() -> directory.find(text, countryId.orElse(null), page));
        Map<UUID, Optional<String>> cities = new HashMap<>();
        return found.map(listing -> {
            InstitutionCard card = new InstitutionCard(listing.institutionId(), listing.tenantCode(), listing.name());
            if (listing.cityId() != null) {
                cities.computeIfAbsent(listing.cityId(), this::cityName).ifPresent(card::setCity);
            }
            return card;
        });
    }

    /** The country of an ISO code; empty when master knows none. Without master the finder cannot narrow: 503. */
    private Optional<UUID> countryOf(String iso2) {
        try {
            return master.listCountries(ListCountriesRequest.newBuilder().setFilter("iso2 = \"" + iso2 + "\"")
                    .build()).getItemsList().stream().findFirst().map(country -> UUID.fromString(country.getId()));
        } catch (IntegrationGrpcClientException e) {
            LOG.warn("the master service answered {} ({})", e.httpStatus(), e.statusCode());
            throw new UnavailableException("The master service could not answer.");
        }
    }

    /** A city's name, or nothing when master cannot say (a display nicety, never a guess). */
    private Optional<String> cityName(UUID cityId) {
        try {
            return Optional.of(master.getCity(GetCityRequest.newBuilder().setId(cityId.toString()).build()).getCity()
                    .getName()).filter(name -> !name.isEmpty());
        } catch (IntegrationGrpcClientException e) {
            return Optional.empty();
        }
    }
}
