package com.mysms.lms.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.lms.api.model.MemberCard;
import com.mysms.lms.api.model.MemberProfile;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.mapper.BadgeAwardMapper;
import com.mysms.lms.mapper.CertificateMapper;
import com.mysms.lms.mapper.EnrollmentMapper;
import com.mysms.lms.repository.BadgeAwardRepository;
import com.mysms.lms.repository.CertificateRepository;
import com.mysms.lms.repository.EnrollmentRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * A member's profile (lms.GetMemberProfile, signed-in callers only): their card, published certificates, badges and
 * course enrolments, at most {@value #LIMIT} of each. The card names the caller from their own person record and anybody
 * else through iam (which asks iam.user_account.read of the caller).
 */
@Service
public class MemberProfileService extends AppTransactionManager {

    static final int LIMIT = 100;
    static final String PERMISSION = "lms.enrollment.get_member_profile";

    private final CertificateRepository certificates;
    private final CertificateMapper certificateMapper;
    private final BadgeAwardRepository awards;
    private final BadgeAwardMapper awardMapper;
    private final EnrollmentRepository enrollments;
    private final EnrollmentMapper enrollmentMapper;
    private final MemberDirectory directory;
    private final Clock clock;

    public MemberProfileService(CertificateRepository certificates, CertificateMapper certificateMapper,
            BadgeAwardRepository awards, BadgeAwardMapper awardMapper, EnrollmentRepository enrollments,
            EnrollmentMapper enrollmentMapper, MemberDirectory directory,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions, Clock clock) {
        super(database, transactions);
        this.certificates = certificates;
        this.certificateMapper = certificateMapper;
        this.awards = awards;
        this.awardMapper = awardMapper;
        this.enrollments = enrollments;
        this.enrollmentMapper = enrollmentMapper;
        this.directory = directory;
        this.clock = clock;
    }

    /**
     * lms.GetMemberProfile: a member's profile as the caller may see it — certificates and enrolments within the
     * caller's data scope for this operation's permission, and badges (tenant-level). A member iam does not show the
     * caller is 404.
     */
    public MemberProfile getMemberProfile(UUID userId) {
        AuthContext caller = AuthContext.require();
        DataScope scope = DataScope.current(PERMISSION);
        String purpose = "Reading a member profile";
        MemberDirectory.Member member = caller.userId().equals(userId) ? directory.self(purpose)
                : Optional.ofNullable(directory.lookup(List.of(userId), purpose).get(userId))
                        .orElseThrow(() -> new ResourceNotFoundException("lms.member", userId));
        MemberProfile profile = doInReadTransaction(() -> {
            MemberProfile read = new MemberProfile();
            read.setCertificates(certificates.list(page(CertificateRepository.SORT_FIELDS,
                    CertificateRepository.DEFAULT_ORDER, CertificateRepository.ID_COLUMN, userId),
                    owned(userId, "r.is_published").and(CertificateRepository.scope(scope))).items().stream()
                    .map(certificateMapper::toDto).toList());
            read.setBadges(awards.list(page(BadgeAwardRepository.SORT_FIELDS, BadgeAwardRepository.DEFAULT_ORDER,
                    BadgeAwardRepository.ID_COLUMN, userId), owned(userId, "TRUE")).items().stream()
                    .map(awardMapper::toDto).toList());
            read.setEnrollments(enrollments.list(page(EnrollmentRepository.SORT_FIELDS,
                    EnrollmentRepository.DEFAULT_ORDER, EnrollmentRepository.ID_COLUMN, userId),
                    owned(userId, "TRUE").and(EnrollmentRepository.scope(scope))).items().stream()
                    .map(enrollmentMapper::toDto).toList());
            return read;
        });
        profile.setMember(new MemberCard(userId, member.displayName()));
        return profile;
    }

    private PageRequest page(Map<String, FieldSpec> sortFields, String order, String id,
            UUID userId) {
        return PageRequest.of(1, LIMIT, null, SortSpec.parse(null, sortFields, order, id), "profile|" + userId, clock);
    }

    private static SqlFragment owned(UUID userId, String condition) {
        return new SqlFragment("r.user_id = :profileUserId AND " + condition, Map.of("profileUserId", userId));
    }
}
