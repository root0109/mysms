package com.mysms.campus.library.business;

import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.finance.v1.RaiseChargeInvoiceRequest;
import com.mysms.campus.library.api.model.Invoice;
import com.mysms.campus.library.integration.FinanceGrpcClient;
import com.mysms.campus.library.integration.PeopleGrpcClient;
import com.mysms.campus.library.repository.CardHolder;
import com.mysms.people.v1.GetPersonRequest;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Raises a loan's fine as a finance invoice (an Adapter over the finance and people gRPC clients, called with the
 * caller's token and never inside a transaction): the card holder's name comes from people, the invoice from finance,
 * which keeps one invoice per loan. A refusal names the permission the other service asks for (403); a charge finance
 * will not take is 409; anything else is 503. The other service's sentences never reach the client.
 */
@Component
public class FineInvoicing {

    /** The source finance keys the one invoice of a loan by. */
    static final String SOURCE = "library.loan";
    private static final Logger LOG = LoggerFactory.getLogger(FineInvoicing.class);
    private static final int UNPROCESSABLE = 422;

    private final FinanceGrpcClient finance;
    private final PeopleGrpcClient people;
    private final ProtoBridge bridge;

    public FineInvoicing(FinanceGrpcClient finance, PeopleGrpcClient people, ProtoBridge bridge) {
        this.finance = finance;
        this.people = people;
        this.bridge = bridge;
    }

    /** The fine's invoice: billed to the card's student, or to the borrower's account for a staff card. */
    public Invoice raise(UUID loanId, BigDecimal fine, CardHolder holder, UUID borrowerUserId) {
        String name = call("people", "people.person.read", () -> people.getPerson(GetPersonRequest.newBuilder()
                .setId(holder.personId().toString()).build()).getPerson().getDisplayName());
        RaiseChargeInvoiceRequest.Builder charge = RaiseChargeInvoiceRequest.newBuilder()
                .setInstitutionId(holder.institutionId().toString())
                .setBillToName(name)
                .setDescription("Library fine")
                .setAmount(fine.toPlainString())
                .setSourceType(SOURCE)
                .setSourceId(loanId.toString());
        if ("student".equals(holder.holderType())) {
            charge.setStudentId(holder.studentId().toString());
        } else if (borrowerUserId != null) {
            charge.setBillToUserId(borrowerUserId.toString());
        } else {
            throw new InvalidStateException("The borrower of a staff card has no account to bill.");
        }
        return bridge.answer(call("finance", "finance.invoice.create",
                () -> finance.raiseChargeInvoice(charge.build()).getInvoice()), Invoice.class);
    }

    private static <T> T call(String service, String permission, Supplier<T> request) {
        try {
            return request.get();
        } catch (IntegrationGrpcClientException e) {
            int status = e.httpStatus();
            if (status == HttpStatus.FORBIDDEN.value()) {
                throw new PermissionDeniedException("Invoicing a fine also needs " + permission + ".");
            }
            if (status == HttpStatus.NOT_FOUND.value() || status == HttpStatus.CONFLICT.value()
                    || status == UNPROCESSABLE) {
                throw new InvalidStateException("The " + service + " service did not accept the fine's invoice "
                        + "for this loan.");
            }
            LOG.warn("the {} service answered {} ({})", service, status, e.statusCode());
            throw new UnavailableException("The " + service + " service could not answer.");
        }
    }
}
