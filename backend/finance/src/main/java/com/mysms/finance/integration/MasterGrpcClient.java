package com.mysms.finance.integration;

import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.master.v1.CurrencyServiceGrpc;
import com.mysms.master.v1.GetCurrencyRequest;
import com.mysms.master.v1.GetCurrencyResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/**
 * Calls the master service with the caller's own identity; a failed call throws {@link IntegrationGrpcClientException}.
 */
@Service
public class MasterGrpcClient {

    /** The starter's client of the master service. */
    public static final String MASTER_SERVICE_KEY = "master-service";
    private static final String SERVICE = "master";

    private CurrencyServiceGrpc.CurrencyServiceBlockingStub currencys;

    @GrpcClient(MASTER_SERVICE_KEY)
    public void initCurrencyStub(CurrencyServiceGrpc.CurrencyServiceBlockingStub stub) {
        this.currencys = stub;
    }

    public GetCurrencyResponse getCurrency(GetCurrencyRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> currencys.getCurrency(request));
    }
}
