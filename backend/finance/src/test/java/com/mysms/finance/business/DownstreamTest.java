package com.mysms.finance.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.testkit.GrpcRefusals;
import com.mysms.finance.integration.MasterGrpcClient;
import com.mysms.master.v1.Currency;
import com.mysms.master.v1.GetCurrencyRequest;
import com.mysms.master.v1.GetCurrencyResponse;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

/** How another module's answers become finance's (Downstream), seen through the currency lookup. */
class DownstreamTest {

    @Test
    void aRequiredReadMapsTheOtherModulesStatus() {
        assertThat(Downstream.required("org", "org.academic_year.read", "Generating fee dues", () -> "room"))
                .contains("room");
        assertThat(Downstream.required("org", "org.academic_year.read", "Generating fee dues",
                failing(HttpStatus.NOT_FOUND))).isEmpty();
        assertThatThrownBy(() -> Downstream.required("org", "org.academic_year.read", "Generating fee dues",
                failing(HttpStatus.FORBIDDEN)))
                .isInstanceOf(PermissionDeniedException.class)
                .hasMessage("Generating fee dues also needs org.academic_year.read.");
        assertThatThrownBy(() -> Downstream.required("org", "org.academic_year.read", "Generating fee dues",
                failing(HttpStatus.INTERNAL_SERVER_ERROR)))
                .isInstanceOf(UnavailableException.class).hasMessage("The org service could not answer.");
        assertThatThrownBy(() -> Downstream.required("org", "org.academic_year.read", "Generating fee dues",
                () -> {
                    throw GrpcRefusals.unreachable("org");
                })).isInstanceOf(UnavailableException.class);
    }

    @Test
    void anOwnerCheckTreatsARefusalAsNotTheCallers() {
        assertThat(Downstream.ownerCheck("sis", failing(HttpStatus.FORBIDDEN))).isEmpty();
        assertThat(Downstream.ownerCheck("sis", failing(HttpStatus.NOT_FOUND))).isEmpty();
        assertThatThrownBy(() -> Downstream.ownerCheck("sis", failing(HttpStatus.TOO_MANY_REQUESTS)))
                .isInstanceOf(UnavailableException.class);
    }

    @Test
    void theCurrencyLookupAnswersTheCodeOrUnavailableNeverAGuess() {
        MasterGrpcClient master = Mockito.mock(MasterGrpcClient.class);
        UUID known = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();
        UUID partial = UUID.randomUUID();
        Mockito.when(master.getCurrency(request(known))).thenReturn(GetCurrencyResponse.newBuilder()
                .setCurrency(Currency.newBuilder().setCode("INR").setMinorUnits(2)).build());
        Mockito.when(master.getCurrency(request(unknown)))
                .thenThrow(GrpcRefusals.refused("master", HttpStatus.NOT_FOUND, "NOT_FOUND"));
        Mockito.when(master.getCurrency(request(partial))).thenReturn(GetCurrencyResponse.getDefaultInstance());
        CurrencyLookup lookup = new CurrencyLookup(master);

        assertThat(lookup.require(known)).isEqualTo(new CurrencyLookup.Money("INR", 2));
        assertThat(lookup.code(known)).contains("INR");
        assertThatThrownBy(() -> lookup.require(unknown)).isInstanceOf(UnavailableException.class);
        assertThatThrownBy(() -> lookup.require(partial)).isInstanceOf(UnavailableException.class);
        assertThat(lookup.code(unknown)).isEmpty();
        assertThat(lookup.code(null)).isEmpty();
    }

    private static GetCurrencyRequest request(UUID id) {
        return GetCurrencyRequest.newBuilder().setId(id.toString()).build();
    }

    /** A call that fails the way the gRPC client does when the callee refuses with this status. */
    private static Supplier<String> failing(HttpStatus status) {
        return () -> {
            throw GrpcRefusals.refused("org", status, "REFUSED");
        };
    }
}
