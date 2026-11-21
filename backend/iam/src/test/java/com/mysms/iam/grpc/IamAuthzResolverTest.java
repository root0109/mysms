package com.mysms.iam.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.json.JsonMappers;
import com.mysms.core.validation.DtoValidator;
import com.mysms.iam.business.AuthzService;
import com.mysms.iam.v1.GetRolePermissionsRequest;
import com.mysms.iam.v1.GetRolePermissionsResponse;
import io.grpc.stub.StreamObserver;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The resolver only decodes ids and shapes the answer; a malformed id never reaches the service. */
class IamAuthzResolverTest {

    private static final UUID TENANT = UUID.fromString("018f2a2a-0000-7000-8000-000000000001");
    private static final UUID ROLE = UUID.fromString("018f2a2a-0000-7000-8000-000000000002");

    private final AuthzService service = mock(AuthzService.class);
    private final IamAuthzResolver resolver = new IamAuthzResolver(service, bridge());

    private static ProtoBridge bridge() {
        try (jakarta.validation.ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            return new ProtoBridge(JsonMappers.create(), new DtoValidator(validator), List.of());
        }
    }

    /** Collects what the resolver answers, as the gRPC runtime would. */
    private static final class Answer implements StreamObserver<GetRolePermissionsResponse> {

        private final List<GetRolePermissionsResponse> answers = new ArrayList<>();

        @Override
        public void onNext(GetRolePermissionsResponse value) {
            answers.add(value);
        }

        @Override
        public void onError(Throwable failure) {
            throw new IllegalStateException("the resolver must not answer an error itself", failure);
        }

        @Override
        public void onCompleted() {
            // nothing to collect
        }
    }

    @Test
    void answersEveryResolvedRoleWithItsCodesTheCommonCodesAndTheVersion() {
        when(service.callerRolePermissions(eq(TENANT), eq(List.of(ROLE)))).thenReturn(new AuthzService.RoleAuthz(7,
                List.of(new AuthzService.RoleCodes(ROLE, List.of("iam.role.read", "sis.student.read"))),
                List.of("iam.user_account.accept_terms")));
        Answer answer = new Answer();

        resolver.getRolePermissions(GetRolePermissionsRequest.newBuilder().setTenantId(TENANT.toString())
                .addRoleIds(ROLE.toString()).build(), answer);

        assertThat(answer.answers).singleElement().satisfies(reply -> {
            assertThat(reply.getAuthzVersion()).isEqualTo(7);
            assertThat(reply.getCommonPermissionCodesList()).containsExactly("iam.user_account.accept_terms");
            assertThat(reply.getRolesList()).singleElement().satisfies(role -> {
                assertThat(role.getRoleId()).isEqualTo(ROLE.toString());
                assertThat(role.getPermissionCodesList()).containsExactly("iam.role.read", "sis.student.read");
            });
        });
    }

    @Test
    void refusesAnIdThatIsNoUuid() {
        GetRolePermissionsRequest request = GetRolePermissionsRequest.newBuilder()
                .setTenantId(TENANT.toString()).addRoleIds("not-a-uuid").build();

        assertThatThrownBy(() -> resolver.getRolePermissions(request, new Answer()))
                .isInstanceOf(InvalidArgumentException.class);
    }
}
