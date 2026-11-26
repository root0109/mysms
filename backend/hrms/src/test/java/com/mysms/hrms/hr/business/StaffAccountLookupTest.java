package com.mysms.hrms.hr.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.testkit.GrpcRefusals;
import com.mysms.hrms.hr.integration.IamGrpcClient;
import com.mysms.iam.v1.CreateUserAccountRequest;
import com.mysms.iam.v1.CreateUserAccountResponse;
import com.mysms.iam.v1.ListUserAccountsRequest;
import com.mysms.iam.v1.ListUserAccountsResponse;
import com.mysms.iam.v1.UserAccount;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

/**
 * The staff account gateway against a stand-in of iam's gRPC client: which account a person is given, and how iam's
 * refusals become this operation's errors.
 */
class StaffAccountLookupTest {

    private static final UUID PERSON = UUID.randomUUID();
    private static final UUID ACCOUNT = UUID.randomUUID();
    private static final LocalDate JOINED = LocalDate.of(2026, 4, 1);

    private final IamGrpcClient iam = mock(IamGrpcClient.class);
    private final StaffAccountLookup accounts = new StaffAccountLookup(iam);

    private static ListUserAccountsResponse page(UserAccount... found) {
        return ListUserAccountsResponse.newBuilder().addAllItems(java.util.List.of(found)).build();
    }

    private static UserAccount stored(UUID id) {
        return UserAccount.newBuilder().setId(id.toString()).build();
    }

    private void givenThePersonHas(UserAccount... found) {
        doReturn(page(found)).when(iam).listUserAccounts(any());
    }

    private void givenCreateAnswers(HttpStatus status) {
        doThrow(GrpcRefusals.refused("iam", status, "REFUSED")).when(iam).createUserAccount(any());
    }

    @Test
    void aPersonWithoutAnAccountGetsAStaffAccountThatSignsInWithTheEmployeeNumber() {
        givenThePersonHas();
        doReturn(CreateUserAccountResponse.newBuilder().setUserAccount(stored(ACCOUNT)).build()).when(iam)
                .createUserAccount(any());

        assertThat(accounts.accountFor(PERSON, "E-17", JOINED)).isEqualTo(ACCOUNT);

        ArgumentCaptor<ListUserAccountsRequest> asked = ArgumentCaptor.forClass(ListUserAccountsRequest.class);
        verify(iam).listUserAccounts(asked.capture());
        assertThat(asked.getValue().getFilter()).isEqualTo("personId = \"" + PERSON + "\"");
        ArgumentCaptor<CreateUserAccountRequest> sent = ArgumentCaptor.forClass(CreateUserAccountRequest.class);
        verify(iam).createUserAccount(sent.capture());
        assertThat(sent.getValue().getUserAccount().getPersonId()).isEqualTo(PERSON.toString());
        assertThat(sent.getValue().getUserAccount().getUsername()).isEqualTo("E-17");
        assertThat(sent.getValue().getUserAccount().getUserType()).isEqualTo("staff");
        assertThat(sent.getValue().getUserAccount().getJoinedOn()).isEqualTo("2026-04-01");
    }

    @Test
    void thePersonsExistingAccountIsTakenSoARepeatedCallCreatesNothing() {
        givenThePersonHas(stored(ACCOUNT));

        assertThat(accounts.accountFor(PERSON, "E-17", JOINED)).isEqualTo(ACCOUNT);

        verify(iam, never()).createUserAccount(any());
    }

    @Test
    void aConflictIsTheAccountAConcurrentCallMadeOrElseATakenLoginName() {
        doReturn(page()).doReturn(page(stored(ACCOUNT))).when(iam).listUserAccounts(any());
        givenCreateAnswers(HttpStatus.CONFLICT);
        assertThat(accounts.accountFor(PERSON, "E-17", JOINED)).as("made meanwhile for the same person")
                .isEqualTo(ACCOUNT);

        givenThePersonHas();
        assertThatThrownBy(() -> accounts.accountFor(PERSON, "E-17", JOINED))
                .isInstanceOf(DuplicateResourceException.class).hasMessageContaining("employee number");
    }

    @Test
    void iamsRefusalsBecomeThisOperationsErrors() {
        givenThePersonHas();
        givenCreateAnswers(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> accounts.accountFor(PERSON, "E-17", JOINED))
                .isInstanceOf(PermissionDeniedException.class).hasMessageContaining("iam.user_account.create");

        givenCreateAnswers(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThatThrownBy(() -> accounts.accountFor(PERSON, "E-17", JOINED))
                .isInstanceOfSatisfying(ValidationFailedException.class,
                        e -> assertThat(e.errors()).singleElement()
                                .satisfies(error -> assertThat(error.field()).isEqualTo("employeeCode")));

        givenCreateAnswers(HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> accounts.accountFor(PERSON, "E-17", JOINED))
                .isInstanceOf(UnavailableException.class);

        doThrow(GrpcRefusals.refused("iam", HttpStatus.FORBIDDEN, "PERMISSION_DENIED")).when(iam)
                .listUserAccounts(any());
        assertThatThrownBy(() -> accounts.accountFor(PERSON, "E-17", JOINED))
                .isInstanceOf(PermissionDeniedException.class).hasMessageContaining("iam.user_account.read");
    }

    @Test
    void aFailingOrUnreachableIamIsUnavailableAndSaysNothingOfTheCause() {
        doThrow(GrpcRefusals.refused("iam", HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL")).when(iam)
                .listUserAccounts(any());
        assertThatThrownBy(() -> accounts.accountFor(PERSON, "E-17", JOINED))
                .isInstanceOf(UnavailableException.class).hasMessageNotContaining("own sentence");

        doThrow(GrpcRefusals.unreachable("iam")).when(iam).listUserAccounts(any());
        assertThatThrownBy(() -> accounts.accountFor(PERSON, "E-17", JOINED))
                .isInstanceOf(UnavailableException.class);

        givenThePersonHas();
        doReturn(CreateUserAccountResponse.getDefaultInstance()).when(iam).createUserAccount(any());
        assertThatThrownBy(() -> accounts.accountFor(PERSON, "E-17", JOINED))
                .as("an account without its id").isInstanceOf(UnavailableException.class);
        doThrow(GrpcRefusals.unreachable("iam")).when(iam).createUserAccount(any());
        assertThatThrownBy(() -> accounts.accountFor(PERSON, "E-17", JOINED))
                .isInstanceOf(UnavailableException.class);
    }
}
