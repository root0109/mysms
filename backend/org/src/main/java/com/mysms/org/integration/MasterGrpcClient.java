package com.mysms.org.integration;

import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.master.v1.CityServiceGrpc;
import com.mysms.master.v1.CountryServiceGrpc;
import com.mysms.master.v1.GetCityRequest;
import com.mysms.master.v1.GetCityResponse;
import com.mysms.master.v1.ListCountriesRequest;
import com.mysms.master.v1.ListCountriesResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/** Calls the master service (its reads are public); a failed call throws {@link IntegrationGrpcClientException}. */
@Service
public class MasterGrpcClient {

    /** The starter's client of the master service. */
    public static final String MASTER_SERVICE_KEY = "master-service";
    private static final String SERVICE = "master";

    private CountryServiceGrpc.CountryServiceBlockingStub countries;
    private CityServiceGrpc.CityServiceBlockingStub cities;

    @GrpcClient(MASTER_SERVICE_KEY)
    public void initCountryStub(CountryServiceGrpc.CountryServiceBlockingStub stub) {
        this.countries = stub;
    }

    @GrpcClient(MASTER_SERVICE_KEY)
    public void initCityStub(CityServiceGrpc.CityServiceBlockingStub stub) {
        this.cities = stub;
    }

    public ListCountriesResponse listCountries(ListCountriesRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> countries.listCountries(request));
    }

    public GetCityResponse getCity(GetCityRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> cities.getCity(request));
    }
}
