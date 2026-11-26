package com.mysms.campus.library.integration;

import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.finance.v1.InvoiceServiceGrpc;
import com.mysms.finance.v1.RaiseChargeInvoiceRequest;
import com.mysms.finance.v1.RaiseChargeInvoiceResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/** Calls the finance service as the caller; a failed call throws {@link IntegrationGrpcClientException}. */
@Service
public class FinanceGrpcClient {

    /** The starter's client of the finance service. */
    public static final String FINANCE_SERVICE_KEY = "finance-service";
    private static final String SERVICE = "finance";

    private InvoiceServiceGrpc.InvoiceServiceBlockingStub invoices;

    @GrpcClient(FINANCE_SERVICE_KEY)
    public void initInvoiceStub(InvoiceServiceGrpc.InvoiceServiceBlockingStub stub) {
        this.invoices = stub;
    }

    public RaiseChargeInvoiceResponse raiseChargeInvoice(RaiseChargeInvoiceRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> invoices.raiseChargeInvoice(request));
    }
}
