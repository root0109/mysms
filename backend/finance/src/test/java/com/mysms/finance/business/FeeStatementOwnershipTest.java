package com.mysms.finance.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mysms.authclient.AuthContext;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.validation.DtoValidator;
import com.mysms.finance.api.model.FeeStatement;
import com.mysms.finance.api.model.Payment;
import com.mysms.finance.domain.PaymentEntity;
import com.mysms.finance.domain.StudentFeeDueEntity;
import com.mysms.finance.mapper.InvoiceLineMapper;
import com.mysms.finance.mapper.InvoiceMapper;
import com.mysms.finance.mapper.StudentFeeDueMapper;
import com.mysms.finance.repository.InvoiceLineRepository;
import com.mysms.finance.repository.InvoiceRepository;
import com.mysms.finance.repository.LedgerRepository;
import com.mysms.finance.repository.NumberSequenceRepository;
import com.mysms.finance.repository.PaymentRepository;
import com.mysms.finance.repository.StudentFeeDueRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Who reads which fee statement, and what of the payments on it (stand-ins for the repositories and for sis; the HTTP
 * paths are FeeWorkflowComponentTest's): the fee office reads what lies in its reach without asking sis; a clerk's own
 * child at another school is theirs to read whole; and a payment someone else made, outside the caller's fee-office
 * reach, shows only its amount, date, status and receipt.
 */
class FeeStatementOwnershipTest {

    private static final UUID INR = UUID.randomUUID();

    private final StandInTransactions tx = new StandInTransactions();
    private final InvoiceRepository invoices = mock(InvoiceRepository.class);
    private final StudentFeeDueRepository dues = mock(StudentFeeDueRepository.class);
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final PaymentViewFactory views = mock(PaymentViewFactory.class);
    private final LedgerRepository ledger = mock(LedgerRepository.class);
    private final StudentOwnershipLookup ownership = mock(StudentOwnershipLookup.class);
    private final InvoiceService service = new InvoiceService(tx.database, tx.transactions, invoices,
            mock(InvoiceMapper.class), mock(InvoiceLineRepository.class), mock(InvoiceLineMapper.class), dues,
            mock(StudentFeeDueMapper.class), payments, views, ledger, mock(NumberSequenceRepository.class), ownership,
            mock(CurrencyLookup.class), mock(OutboxWriter.class), mock(AuditWriter.class), mock(DtoValidator.class),
            Clock.systemUTC());
    private final UUID schoolA = UUID.randomUUID();
    private final UUID schoolB = UUID.randomUUID();
    private final UUID student = UUID.randomUUID();
    private final UUID clerk = UUID.randomUUID();
    private final UUID otherPayer = UUID.randomUUID();
    private final StudentFeeDueEntity due = StudentFeeDueEntity.builder().id(UUID.randomUUID()).institutionId(schoolB)
            .studentId(student).currencyId(INR).status("draft").amount(new BigDecimal("500.00"))
            .discountAmount(BigDecimal.ZERO).netAmount(new BigDecimal("500.00")).build();
    private final PaymentEntity byClerk = payment(clerk);
    private final PaymentEntity byOther = payment(otherPayer);

    @BeforeEach
    void theStudentsRecordsAtSchoolB() {
        when(dues.ofStudent(student, null)).thenReturn(List.of(due));
        when(invoices.ofStudent(student)).thenReturn(List.of());
        when(payments.ofStudent(List.of(due.getId()), List.of())).thenReturn(List.of(byClerk, byOther));
        when(ledger.allocationsTouching(any())).thenReturn(List.of());
        when(ledger.refunded(any())).thenReturn(Map.of());
        when(views.views(List.of(byClerk, byOther))).thenAnswer(call -> List.of(full(byClerk), full(byOther)));
    }

    @Test
    void aClerksOwnChildAtAnotherSchoolIsTheirsToReadWholeAndOtherPayersStayPrivate() {
        AuthContext clerkOfA = caller(clerk, Set.of(schoolA), "finance.payment.record",
                "finance.invoice.get_fee_statement");
        when(ownership.relationTo(clerkOfA, student)).thenReturn(Optional.of(StudentOwnershipLookup.Relation.GUARDIAN));

        FeeStatement statement = service.feeStatement(clerkOfA, student, null);

        assertThat(statement.getDues()).hasSize(1);
        assertThat(statement.getTotalDue()).isEqualTo("500.00");
        Payment own = statement.getPayments().get(0);
        assertThat(own.getPayerUserId()).as("the clerk's own payment, whole").isEqualTo(clerk);
        assertThat(own.getBillingName()).isEqualTo("Asha Rao");
        Payment other = statement.getPayments().get(1);
        assertThat(other.getAmount()).isEqualTo("100.00");
        assertThat(other.getReceiptNumber()).isEqualTo("RCT-000007");
        assertThat(other.getStatus()).isEqualTo(Payment.StatusEnum.RECEIVED);
        assertThat(other.getReceivedAt()).isNotNull();
        assertThat(other.getPayerUserId()).isNull();
        assertThat(other.getPayerName()).isNull();
        assertThat(other.getBillingName()).isNull();
        assertThat(other.getTaxRegistrationNumber()).isNull();
        assertThat(other.getTaxIdentityMasked()).isNull();
        assertThat(other.getInstrumentReference()).isNull();
        assertThat(other.getBankName()).isNull();
        assertThat(other.getGatewayPaymentId()).isNull();
    }

    @Test
    void aClerkReadsNoStatementOfAStudentElsewhereThatIsNotTheirs() {
        AuthContext clerkOfA = caller(clerk, Set.of(schoolA), "finance.payment.record");
        when(ownership.relationTo(clerkOfA, student)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.feeStatement(clerkOfA, student, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void theOfficeOfTheStudentsSchoolReadsEveryPaymentWholeWithoutAskingSis() {
        AuthContext officeOfB = caller(clerk, Set.of(schoolB), "finance.payment.record");
        FeeStatement statement = service.feeStatement(officeOfB, student, null);
        assertThat(statement.getPayments()).extracting(Payment::getPayerUserId).containsExactly(clerk, otherPayer);
        verify(ownership, never()).relationTo(any(), any());
    }

    @Test
    void aGuardianSeesTheirOwnPaymentWholeAndAnotherPayersOnlyInPart() {
        AuthContext guardian = caller(otherPayer, Set.of(schoolB), "finance.invoice.get_fee_statement");
        when(ownership.relationTo(guardian, student)).thenReturn(Optional.of(StudentOwnershipLookup.Relation.GUARDIAN));
        FeeStatement statement = service.feeStatement(guardian, student, null);
        assertThat(statement.getPayments()).extracting(Payment::getPayerUserId).containsExactly(null, otherPayer);
        assertThat(statement.getPayments().get(0).getBillingName()).isNull();
        assertThat(statement.getPayments().get(1).getBillingName()).isEqualTo("Asha Rao");
    }

    private PaymentEntity payment(UUID payer) {
        return PaymentEntity.builder().id(UUID.randomUUID()).institutionId(schoolB).payerUserId(payer)
                .amount(new BigDecimal("100.00")).currencyId(INR).status("received").build();
    }

    /** The payment as the payment views give it to the fee office: every field. */
    private static Payment full(PaymentEntity payment) {
        return new Payment(Payment.PurposeEnum.FEES, "100.00", INR).id(payment.getId())
                .payerUserId(payment.getPayerUserId()).payerName("Parent").billingName("Asha Rao")
                .taxRegistrationNumber("GSTIN-1").taxIdentityMasked("••••1234").instrumentReference("CHQ-1")
                .bankName("Bank").gatewayPaymentId("pay_1").status(Payment.StatusEnum.RECEIVED)
                .receivedAt(OffsetDateTime.parse("2026-04-02T10:00:00Z")).receiptNumber("RCT-000007");
    }

    /** A caller of the tenant holding the permissions in these institutions. */
    private static AuthContext caller(UUID user, Set<UUID> institutions, String... permissions) {
        return new AuthContext(user, Optional.of(UUID.randomUUID()), institutions, Optional.empty(),
                Set.of(permissions), "jti", Optional.empty(), false, Optional.empty(), Optional.empty(), false,
                Set.of());
    }
}
