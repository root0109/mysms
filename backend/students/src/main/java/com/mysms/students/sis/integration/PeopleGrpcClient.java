package com.mysms.students.sis.integration;

import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.people.v1.ListPersonsRequest;
import com.mysms.people.v1.ListPersonsResponse;
import com.mysms.people.v1.PersonServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/**
 * Calls the people service with the caller's own identity; a failed call throws {@link IntegrationGrpcClientException}.
 */
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

    public ListPersonsResponse listPersons(ListPersonsRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> persons.listPersons(request));
    }
}
