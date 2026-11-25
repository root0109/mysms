package com.mysms.comms.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Timestamp;
import com.mysms.comms.integration.IamGrpcClient;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.iam.v1.GetMeResponse;
import com.mysms.iam.v1.ListUserAccountsRequest;
import com.mysms.iam.v1.ListUserAccountsResponse;
import com.mysms.iam.v1.UserAccount;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RecipientDirectoryTest {

    private final IamGrpcClient iam = mock(IamGrpcClient.class);
    private final RecipientDirectory directory = new RecipientDirectory(iam);

    private static UserAccount account(UUID id, UserAccount.Status status, String email, boolean verified) {
        UserAccount.Builder account = UserAccount.newBuilder().setId(id.toString()).setStatus(status)
                .setMobileNo("9800000000");
        if (email != null) {
            account.setEmail(email);
        }
        if (verified) {
            account.setEmailVerifiedAt(Timestamp.newBuilder().setSeconds(1));
        }
        return account.build();
    }

    private static IntegrationGrpcClientException refused(Status status) {
        try {
            IntegrationGrpcClientException.call("iam", () -> {
                throw new StatusRuntimeException(status);
            });
        } catch (IntegrationGrpcClientException refusal) {
            return refusal;
        }
        throw new AssertionError("the call was not refused");
    }

    @Test
    void answersTheAddressOfEachActiveAccountOnTheChannel() {
        UUID active = UUID.randomUUID();
        UUID inactive = UUID.randomUUID();
        UUID withoutEmail = UUID.randomUUID();
        when(iam.listUserAccounts(any())).thenReturn(ListUserAccountsResponse.newBuilder()
                .addItems(account(active, UserAccount.Status.STATUS_ACTIVE, "a@example.org", false))
                .addItems(account(inactive, UserAccount.Status.STATUS_INACTIVE, "b@example.org", true))
                .addItems(account(withoutEmail, UserAccount.Status.STATUS_ACTIVE, null, false)).build());

        assertThat(directory.addresses(List.of(active, inactive, withoutEmail, active), "email"))
                .isEqualTo(Map.of(active, "a@example.org"));
        assertThat(directory.addresses(List.of(active, inactive, withoutEmail), "sms"))
                .isEqualTo(Map.of(active, "9800000000", withoutEmail, "9800000000"));
    }

    @Test
    void asksIamInBatchesAndNeverForPush() {
        List<UUID> many = IntStream.range(0, RecipientDirectory.BATCH + 1).mapToObj(i -> UUID.randomUUID()).toList();
        when(iam.listUserAccounts(any())).thenReturn(ListUserAccountsResponse.getDefaultInstance());

        directory.addresses(many, "email");

        ArgumentCaptor<ListUserAccountsRequest> asked = ArgumentCaptor.forClass(ListUserAccountsRequest.class);
        verify(iam, times(2)).listUserAccounts(asked.capture());
        assertThat(asked.getAllValues().get(0).getPageable().getSize()).isEqualTo(RecipientDirectory.BATCH);
        assertThat(asked.getAllValues().get(1).getFilter()).isEqualTo("id = \"" + many.get(many.size() - 1) + "\"");
        UUID device = UUID.randomUUID();
        assertThat(directory.addresses(List.of(device), "push")).isEqualTo(Map.of(device, device.toString()));
        verify(iam, times(2)).listUserAccounts(any());
    }

    @Test
    void answersTheCallersOwnAddressOnlyWhenVerified() {
        UUID me = UUID.randomUUID();
        when(iam.getMe(any())).thenReturn(meWith(account(me, UserAccount.Status.STATUS_ACTIVE, "me@example.org", true)),
                meWith(account(me, UserAccount.Status.STATUS_ACTIVE, "me@example.org", false)));

        assertThat(directory.ownVerified("email")).contains("me@example.org");
        assertThat(directory.ownVerified("email")).isEmpty();
        verify(iam, never()).listUserAccounts(any());
    }

    private static GetMeResponse meWith(UserAccount account) {
        GetMeResponse.Builder me = GetMeResponse.newBuilder();
        me.getResultBuilder().setUser(account);
        return me.build();
    }

    @Test
    void mapsIamsRefusalTo403AndItsSilenceTo503() {
        when(iam.listUserAccounts(any())).thenThrow(refused(Status.PERMISSION_DENIED));
        assertThatThrownBy(() -> directory.addresses(List.of(UUID.randomUUID()), "email"))
                .isInstanceOf(PermissionDeniedException.class).hasMessageContaining(RecipientDirectory.IAM_PERMISSION);
        when(iam.getMe(any())).thenThrow(refused(Status.UNAVAILABLE));
        assertThatThrownBy(() -> directory.ownVerified("sms")).isInstanceOf(UnavailableException.class);
    }
}
