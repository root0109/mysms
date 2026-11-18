package com.mysms.finance.business;

import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.mapping.WireValues;
import com.mysms.finance.api.model.Payment;
import com.mysms.finance.api.model.PaymentAllocation;
import com.mysms.finance.domain.PaymentEntity;
import com.mysms.finance.mapper.PaymentMapper;
import com.mysms.finance.repository.PaymentRepository;
import com.mysms.finance.repository.PaymentRepository.AllocationView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Payments as the contract shows them, wherever finance answers one (payments, a checkout, a fee statement): the row,
 * its allocations read once for the whole list, and the payer's tax identity only in its masked form. Runs inside the
 * caller's transaction. A Factory of the contract's payment views.
 */
@Component
public class PaymentViewFactory {

    /** The column the payer's tax identity is encrypted for. */
    static final String TAX_IDENTITY = "finance.payment.tax_identity_enc";

    private final PaymentRepository repository;
    private final PaymentMapper mapper;
    private final FieldCipher cipher;

    public PaymentViewFactory(PaymentRepository repository, PaymentMapper mapper, FieldCipher cipher) {
        this.repository = repository;
        this.mapper = mapper;
        this.cipher = cipher;
    }

    /**
     * The payment as shown to someone who neither made it nor works on its fees: amount, date, status and receipt
     * only — no payer, billing, tax, instrument or gateway details.
     */
    static Payment ownerOnly(Payment full) {
        Payment shown = new Payment();
        shown.setId(full.getId());
        shown.setPurpose(full.getPurpose());
        shown.setAmount(full.getAmount());
        shown.setCurrencyId(full.getCurrencyId());
        shown.setStatus(full.getStatus());
        shown.setReceivedAt(full.getReceivedAt());
        shown.setReceiptNumber(full.getReceiptNumber());
        shown.setAllocations(null);
        return shown;
    }

    public Payment view(PaymentEntity payment) {
        return views(List.of(payment)).get(0);
    }

    public List<Payment> views(List<PaymentEntity> payments) {
        if (payments.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<PaymentAllocation>> slices = new HashMap<>();
        for (AllocationView slice : repository.allocations(payments.stream().map(PaymentEntity::getId).toList())) {
            slices.computeIfAbsent(slice.paymentId(), k -> new ArrayList<>()).add(new PaymentAllocation()
                    .id(slice.id()).invoiceId(slice.invoiceId()).studentFeeDueId(slice.studentFeeDueId())
                    .amount(WireValues.decimal(slice.amount())).currencyId(slice.currencyId()));
        }
        return payments.stream().map(payment -> {
            Payment dto = mapper.toDto(payment);
            dto.setAllocations(slices.getOrDefault(payment.getId(), List.of()));
            if (payment.getTaxIdentityEnc() != null) {
                dto.setTaxIdentityMasked(cipher.maskStored(payment.getTaxIdentityEnc(), payment.getTenantId(),
                        TAX_IDENTITY));
            }
            return dto;
        }).toList();
    }
}
