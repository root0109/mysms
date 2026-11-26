package com.mysms.learning.lms.business;

import com.mysms.common.v1.PageableRequest;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.iam.v1.GetMeRequest;
import com.mysms.iam.v1.ListUserAccountsRequest;
import com.mysms.iam.v1.Me;
import com.mysms.iam.v1.UserAccount;
import com.mysms.learning.lms.integration.IamGrpcClient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * The names and e-mail addresses lms shows for its members (an Adapter over iam's user accounts, asked with the
 * caller's own identity): the roster, the gradebook and member cards name people by the account's username, or its
 * e-mail when it has none. lms keeps no personal data of its own and does not call the people service, so the
 * account is the only name it can show.
 *
 * <p>The accounts are read in batches of {@value #BATCH} ids (iam's filter allows 50 terms). iam refusing the caller is
 * this operation's 403, naming the permission; iam not answering is 503 — a roster without names would be a guess.
 * Never call this inside a database transaction.
 */
@Component
public class MemberDirectory {

    /** The iam permission the lookup runs under (the caller's own). */
    public static final String IAM_PERMISSION = "iam.user_account.read";
    static final int BATCH = 40;
    private static final Logger LOG = LoggerFactory.getLogger(MemberDirectory.class);

    private final IamGrpcClient iam;

    public MemberDirectory(IamGrpcClient iam) {
        this.iam = iam;
    }

    /** One member as lms shows them. */
    public record Member(UUID userId, String displayName, String email) {

        /** A row's member iam does not show the caller: named as unknown, never by the account id. */
        static Member unknown(UUID userId) {
            return new Member(userId, UNKNOWN, null);
        }
    }

    /** The name shown for an account iam does not show the caller. */
    static final String UNKNOWN = "Unknown member";

    /**
     * The caller themself, named as their own person record shows them (iam.GetMe, which every signed-in user may
     * ask); their account's name when no person is linked.
     */
    public Member self(String purpose) {
        Me me = call(() -> iam.getMe(GetMeRequest.getDefaultInstance()).getResult(), purpose);
        UserAccount account = me.getUser();
        String name = me.hasPerson() && !me.getPerson().getDisplayName().isBlank() ? me.getPerson().getDisplayName()
                : name(account);
        return new Member(UUID.fromString(account.getId()), name, account.hasEmail() ? account.getEmail() : null);
    }

    /**
     * The members of the given accounts iam shows the caller, keyed by user id; an account it does not know or does
     * not show is absent (rosters name it {@link Member#unknown}).
     */
    public Map<UUID, Member> lookup(Collection<UUID> userIds, String purpose) {
        List<UUID> ids = userIds.stream().distinct().toList();
        Map<UUID, Member> found = new LinkedHashMap<>();
        for (int from = 0; from < ids.size(); from += BATCH) {
            List<UUID> batch = ids.subList(from, Math.min(ids.size(), from + BATCH));
            String filter = batch.stream().map(id -> "id = \"" + id + "\"").collect(Collectors.joining(" OR "));
            ListUserAccountsRequest request = ListUserAccountsRequest.newBuilder().setFilter(filter)
                    .setPageable(PageableRequest.newBuilder().setPage(1).setSize(batch.size())).build();
            for (UserAccount account : accounts(request, purpose)) {
                UUID id = UUID.fromString(account.getId());
                found.put(id, new Member(id, name(account), account.hasEmail() ? account.getEmail() : null));
            }
        }
        return found;
    }

    /** The accounts of these e-mail addresses (matched case-insensitively), keyed by the lower-cased address. */
    public Map<String, UUID> byEmail(Collection<String> emails, String purpose) {
        List<String> wanted = emails.stream().filter(Objects::nonNull).map(email -> email.toLowerCase(Locale.ROOT)).distinct()
                .toList();
        Map<String, UUID> found = new LinkedHashMap<>();
        for (int from = 0; from < wanted.size(); from += BATCH) {
            List<String> batch = wanted.subList(from, Math.min(wanted.size(), from + BATCH));
            String filter = batch.stream().map(email -> "email = \"" + email.replace("\\", "").replace("\"", "")
                    + "\"").collect(Collectors.joining(" OR "));
            ListUserAccountsRequest request = ListUserAccountsRequest.newBuilder().setFilter(filter)
                    .setPageable(PageableRequest.newBuilder().setPage(1).setSize(batch.size())).build();
            for (UserAccount account : accounts(request, purpose)) {
                if (account.hasEmail()) {
                    found.put(account.getEmail().toLowerCase(Locale.ROOT), UUID.fromString(account.getId()));
                }
            }
        }
        return found;
    }

    private List<UserAccount> accounts(ListUserAccountsRequest request, String purpose) {
        return call(() -> new ArrayList<>(iam.listUserAccounts(request).getItemsList()), purpose);
    }

    private static <T> T call(Supplier<T> request, String purpose) {
        try {
            return request.get();
        } catch (IntegrationGrpcClientException e) {
            if (e.httpStatus() == HttpStatus.FORBIDDEN.value()) {
                throw new PermissionDeniedException(purpose + " also needs " + IAM_PERMISSION + ".");
            }
            LOG.warn("the iam service answered {} ({})", e.httpStatus(), e.statusCode());
            throw new UnavailableException("The iam service could not answer.");
        }
    }

    private static String name(UserAccount account) {
        if (account.hasUsername() && !account.getUsername().isBlank()) {
            return account.getUsername();
        }
        return account.hasEmail() && !account.getEmail().isBlank() ? account.getEmail() : UNKNOWN;
    }
}
