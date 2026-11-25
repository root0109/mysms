package com.mysms.library.integration;

import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.people.v1.GetPersonRequest;
import com.mysms.people.v1.GetPersonResponse;
import com.mysms.people.v1.PersonServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/** Calls the people service as the caller; a failed call throws {@link IntegrationGrpcClientException}. */
@Service
public class PeopleGrpcClient {

    /** The starter's client of the people service. */
    public static final String PEOPLE_SERVICE_KEY = "people-service";
    private static final String SERVICE = "people";

    private PersonServiceGrpc.PersonServiceBlockingStub persons;

    @GrpcClient(PEOPLE_SERVICE_KEY)
    public void initPersonStub(PersonServiceGrpc.PersonServiceBlockingStub stub) {
        this.persons = stub;
    }

    public GetPersonResponse getPerson(GetPersonRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> persons.getPerson(request));
    }
}
