package com.mysms.finance.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.error.UnavailableException;
import com.mysms.finance.domain.PaymentEntity;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** A refund in the gateway order's own minor units, exactly (no I/O). */
class PaymentServiceTest {

    @Test
    void aRefundIsCountedInTheMinorUnitsTheOrderWasOpenedIn() {
        PaymentEntity rupees = PaymentEntity.builder().amount(new BigDecimal("1000.00")).gatewayAmountMinor(100000L)
                .build();
        assertThat(PaymentService.minorOf(new BigDecimal("400.50"), rupees)).isEqualTo(40050L);
        PaymentEntity yen = PaymentEntity.builder().amount(new BigDecimal("1500")).gatewayAmountMinor(1500L).build();
        assertThat(PaymentService.minorOf(new BigDecimal("700"), yen)).isEqualTo(700L);
        PaymentEntity counter = PaymentEntity.builder().amount(new BigDecimal("10.00")).build();
        assertThatThrownBy(() -> PaymentService.minorOf(BigDecimal.ONE, counter))
                .isInstanceOf(UnavailableException.class);
        assertThatThrownBy(() -> PaymentService.minorOf(new BigDecimal("0.001"), rupees))
                .isInstanceOf(ArithmeticException.class);
    }
}
