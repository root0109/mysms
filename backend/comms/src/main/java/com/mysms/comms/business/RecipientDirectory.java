package com.mysms.comms.business;

import com.mysms.common.v1.PageableRequest;
import com.mysms.comms.integration.IamGrpcClient;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.iam.v1.GetMeRequest;
import com.mysms.iam.v1.ListUserAccountsRequest;
import com.mysms.iam.v1.UserAccount;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Where a message to an account goes (an Adapter over iam's user accounts, asked with the caller's own identity): the
 * account's e-mail for e-mail, its mobile number for SMS, the account itself for push (the provider finds its devices).
 * Only active accounts receive messages. iam refusing the caller is 403, naming the permission; iam not answering is
 * 503. Never call this inside a database transaction.
 */
@Component
public class RecipientDirectory {

    /** The iam permission the lookup runs under (the caller's own). */
    public static final String IAM_PERMISSION = "iam.user_account.read";
    static final int BATCH = 40;

    private final IamGrpcClient iam;

    public RecipientDirectory(IamGrpcClient iam) {
        this.iam = iam;
    }

    /** The address of each account on the channel; accounts without one, or not shown to the caller, are absent. */
    public Map<UUID, String> addresses(Collection<UUID> userIds, String channel) {
        List<UUID> ids = userIds.stream().distinct().toList();
        Map<UUID, String> found = new LinkedHashMap<>();
        if ("push".equals(channel) || "web_push".equals(channel)) {
            ids.forEach(id -> found.put(id, id.toString()));
            return found;
        }
        for (int from = 0; from < ids.size(); from += BATCH) {
            List<UUID> batch = ids.subList(from, Math.min(ids.size(), from + BATCH));
            String filter = batch.stream().map(id -> "id = \"" + id + "\"").collect(Collectors.joining(" OR "));
            ListUserAccountsRequest request = ListUserAccountsRequest.newBuilder().setFilter(filter)
                    .setPageable(PageableRequest.newBuilder().setPage(1).setSize(batch.size())).build();
            for (UserAccount account : call(() -> iam.listUserAccounts(request).getItemsList())) {
                address(account, channel, false)
                        .ifPresent(address -> found.put(UUID.fromString(account.getId()), address));
            }
        }
        return found;
    }

    /** The caller's own verified address on the channel (e-mail or mobile number), when they have one. */
    public Optional<String> ownVerified(String channel) {
        UserAccount me = call(() -> iam.getMe(GetMeRequest.getDefaultInstance()).getResult().getUser());
        return address(me, channel, true);
    }

    private static Optional<String> address(UserAccount account, String channel, boolean verified) {
        if (account.getStatus() != UserAccount.Status.STATUS_ACTIVE) {
            return Optional.empty();
        }
        if (EmailChannel.NAME.equals(channel) && account.hasEmail() && !account.getEmail().isBlank()
                && (!verified || account.hasEmailVerifiedAt())) {
            return Optional.of(account.getEmail());
        }
        if ("sms".equals(channel) && account.hasMobileNo() && !account.getMobileNo().isBlank()
                && (!verified || account.hasMobileVerifiedAt())) {
            return Optional.of(account.getMobileNo());
        }
        return Optional.empty();
    }

    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (IntegrationGrpcClientException e) {
            if (e.httpStatus() == HttpStatus.FORBIDDEN.value()) {
                throw new PermissionDeniedException("Sending to these accounts also needs " + IAM_PERMISSION + ".");
            }
            throw new UnavailableException("The iam service could not answer.");
        }
    }
}
