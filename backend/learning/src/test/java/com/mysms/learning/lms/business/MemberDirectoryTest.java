package com.mysms.learning.lms.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mysms.iam.v1.ListUserAccountsResponse;
import com.mysms.iam.v1.UserAccount;
import com.mysms.learning.lms.integration.IamGrpcClient;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The directory fails closed: an account iam does not show is absent, and no member is ever named by an id. */
class MemberDirectoryTest {

    private final IamGrpcClient iam = mock(IamGrpcClient.class);
    private final MemberDirectory directory = new MemberDirectory(iam);

    @Test
    void anAccountIamDoesNotShowIsAbsentNotNamedByItsId() {
        UUID known = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();
        when(iam.listUserAccounts(any())).thenReturn(ListUserAccountsResponse.newBuilder()
                .addItems(UserAccount.newBuilder().setId(known.toString()).setUsername("asha")).build());
        Map<UUID, MemberDirectory.Member> found = directory.lookup(List.of(known, unknown), "Reading");
        assertThat(found).containsOnlyKeys(known);
        assertThat(found.get(known).displayName()).isEqualTo("asha");
        assertThat(MemberDirectory.Member.unknown(unknown).displayName()).isEqualTo(MemberDirectory.UNKNOWN);
    }

    @Test
    void anAccountWithoutUsernameOrEmailIsNotNamedByItsId() {
        UUID bare = UUID.randomUUID();
        when(iam.listUserAccounts(any())).thenReturn(ListUserAccountsResponse.newBuilder()
                .addItems(UserAccount.newBuilder().setId(bare.toString())).build());
        assertThat(directory.lookup(List.of(bare), "Reading").get(bare).displayName())
                .isEqualTo(MemberDirectory.UNKNOWN).doesNotContain(bare.toString());
    }
}
