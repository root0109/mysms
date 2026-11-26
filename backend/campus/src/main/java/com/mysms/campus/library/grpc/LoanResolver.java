package com.mysms.campus.library.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.campus.library.api.model.Invoice;
import com.mysms.campus.library.api.model.Loan;
import com.mysms.campus.library.api.model.LoanUpdate;
import com.mysms.campus.library.business.LoanService;
import com.mysms.library.v1.CreateLoanRequest;
import com.mysms.library.v1.CreateLoanResponse;
import com.mysms.library.v1.DeleteLoanRequest;
import com.mysms.library.v1.DeleteLoanResponse;
import com.mysms.library.v1.GetLoanRequest;
import com.mysms.library.v1.GetLoanResponse;
import com.mysms.library.v1.InvoiceLoanFineRequest;
import com.mysms.library.v1.InvoiceLoanFineResponse;
import com.mysms.library.v1.ListLoansRequest;
import com.mysms.library.v1.ListLoansResponse;
import com.mysms.library.v1.LoanServiceGrpc;
import com.mysms.library.v1.MarkLoanLostRequest;
import com.mysms.library.v1.MarkLoanLostResponse;
import com.mysms.library.v1.RequestLoanReturnRequest;
import com.mysms.library.v1.RequestLoanReturnResponse;
import com.mysms.library.v1.ReturnLoanRequest;
import com.mysms.library.v1.ReturnLoanResponse;
import com.mysms.library.v1.UpdateLoanRequest;
import com.mysms.library.v1.UpdateLoanResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of library.LoanService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class LoanResolver extends LoanServiceGrpc.LoanServiceImplBase {

    private final LoanService service;
    private final ProtoBridge bridge;

    public LoanResolver(LoanService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listLoans(ListLoansRequest request, StreamObserver<ListLoansResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Loan> rows = service.listLoans(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListLoansResponse.newBuilder(), rows).build());
    }

    @Override
    public void getLoan(GetLoanRequest request, StreamObserver<GetLoanResponse> out) {
        Loan found = service.getLoan(
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetLoanResponse.newBuilder(), "loan", found).build());
    }

    @Override
    public void createLoan(CreateLoanRequest request, StreamObserver<CreateLoanResponse> out) {
        Loan created = service.createLoan(
                bridge.body(request.getLoan(), Loan.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateLoanResponse.newBuilder(), "loan", created).build());
    }

    @Override
    public void updateLoan(UpdateLoanRequest request, StreamObserver<UpdateLoanResponse> out) {
        Loan updated = service.updateLoan(
                bridge.id(request.getLoan().getId(), "id"),
                bridge.requiredIfMatch(request.getLoan().getEtag()),
                bridge.changes(request.getLoan(), request.getUpdateMask(), LoanUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateLoanResponse.newBuilder(), "loan", updated).build());
    }

    @Override
    public void deleteLoan(DeleteLoanRequest request, StreamObserver<DeleteLoanResponse> out) {
        service.deleteLoan(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteLoanResponse.getDefaultInstance());
    }

    @Override
    public void requestLoanReturn(RequestLoanReturnRequest request, StreamObserver<RequestLoanReturnResponse> out) {
        Loan changed = service.requestLoanReturn(bridge.id(request.getLoanId(), "loanId"),
                bridge.body(request, com.mysms.campus.library.api.model.RequestLoanReturnRequest.class, "loan_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(RequestLoanReturnResponse.newBuilder(), "loan", changed).build());
    }

    @Override
    public void returnLoan(ReturnLoanRequest request, StreamObserver<ReturnLoanResponse> out) {
        Loan changed = service.returnLoan(bridge.id(request.getLoanId(), "loanId"),
                bridge.body(request, com.mysms.campus.library.api.model.ReturnLoanRequest.class, "loan_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ReturnLoanResponse.newBuilder(), "loan", changed).build());
    }

    @Override
    public void markLoanLost(MarkLoanLostRequest request, StreamObserver<MarkLoanLostResponse> out) {
        Loan changed = service.markLoanLost(bridge.id(request.getLoanId(), "loanId"),
                bridge.body(request, com.mysms.campus.library.api.model.MarkLoanLostRequest.class, "loan_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(MarkLoanLostResponse.newBuilder(), "loan", changed).build());
    }

    @Override
    public void invoiceLoanFine(InvoiceLoanFineRequest request, StreamObserver<InvoiceLoanFineResponse> out) {
        Invoice invoice = service.invoiceLoanFine(bridge.id(request.getLoanId(), "loanId"));
        ProtoBridge.send(out, bridge.reply(InvoiceLoanFineResponse.newBuilder(), "result", invoice).build());
    }
}
