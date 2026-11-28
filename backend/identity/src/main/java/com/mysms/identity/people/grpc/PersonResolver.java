package com.mysms.identity.people.grpc;

import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.identity.people.api.model.BirthdayEntry;
import com.mysms.identity.people.api.model.Certification;
import com.mysms.identity.people.api.model.EducationHistory;
import com.mysms.identity.people.api.model.EmergencyContact;
import com.mysms.identity.people.api.model.Person;
import com.mysms.identity.people.api.model.PersonAddress;
import com.mysms.identity.people.api.model.PersonDocument;
import com.mysms.identity.people.api.model.PersonSkill;
import com.mysms.identity.people.api.model.PersonSocialLink;
import com.mysms.identity.people.api.model.PublicProfile;
import com.mysms.identity.people.api.model.SendResult;
import com.mysms.identity.people.api.model.WorkExperience;
import com.mysms.identity.people.business.GreetingService;
import com.mysms.identity.people.business.MyPersonService;
import com.mysms.identity.people.business.PersonService;
import com.mysms.people.v1.CertificationUpdate;
import com.mysms.people.v1.CreateCertificationRequest;
import com.mysms.people.v1.CreateCertificationResponse;
import com.mysms.people.v1.CreateEducationHistoryRequest;
import com.mysms.people.v1.CreateEducationHistoryResponse;
import com.mysms.people.v1.CreateEmergencyContactRequest;
import com.mysms.people.v1.CreateEmergencyContactResponse;
import com.mysms.people.v1.CreatePersonAddressRequest;
import com.mysms.people.v1.CreatePersonAddressResponse;
import com.mysms.people.v1.CreatePersonDocumentRequest;
import com.mysms.people.v1.CreatePersonDocumentResponse;
import com.mysms.people.v1.CreatePersonRequest;
import com.mysms.people.v1.CreatePersonResponse;
import com.mysms.people.v1.CreatePersonSkillRequest;
import com.mysms.people.v1.CreatePersonSkillResponse;
import com.mysms.people.v1.CreatePersonSocialLinkRequest;
import com.mysms.people.v1.CreatePersonSocialLinkResponse;
import com.mysms.people.v1.CreatePublicProfileRequest;
import com.mysms.people.v1.CreatePublicProfileResponse;
import com.mysms.people.v1.CreateWorkExperienceRequest;
import com.mysms.people.v1.CreateWorkExperienceResponse;
import com.mysms.people.v1.DeleteCertificationRequest;
import com.mysms.people.v1.DeleteCertificationResponse;
import com.mysms.people.v1.DeleteEducationHistoryRequest;
import com.mysms.people.v1.DeleteEducationHistoryResponse;
import com.mysms.people.v1.DeleteEmergencyContactRequest;
import com.mysms.people.v1.DeleteEmergencyContactResponse;
import com.mysms.people.v1.DeletePersonAddressRequest;
import com.mysms.people.v1.DeletePersonAddressResponse;
import com.mysms.people.v1.DeletePersonDocumentRequest;
import com.mysms.people.v1.DeletePersonDocumentResponse;
import com.mysms.people.v1.DeletePersonRequest;
import com.mysms.people.v1.DeletePersonResponse;
import com.mysms.people.v1.DeletePersonSkillRequest;
import com.mysms.people.v1.DeletePersonSkillResponse;
import com.mysms.people.v1.DeletePersonSocialLinkRequest;
import com.mysms.people.v1.DeletePersonSocialLinkResponse;
import com.mysms.people.v1.DeletePublicProfileRequest;
import com.mysms.people.v1.DeletePublicProfileResponse;
import com.mysms.people.v1.DeleteWorkExperienceRequest;
import com.mysms.people.v1.DeleteWorkExperienceResponse;
import com.mysms.people.v1.EducationHistoryUpdate;
import com.mysms.people.v1.EmergencyContactUpdate;
import com.mysms.people.v1.GetCertificationRequest;
import com.mysms.people.v1.GetCertificationResponse;
import com.mysms.people.v1.GetEducationHistoryRequest;
import com.mysms.people.v1.GetEducationHistoryResponse;
import com.mysms.people.v1.GetEmergencyContactRequest;
import com.mysms.people.v1.GetEmergencyContactResponse;
import com.mysms.people.v1.GetMyPersonRequest;
import com.mysms.people.v1.GetMyPersonResponse;
import com.mysms.people.v1.GetPersonAddressRequest;
import com.mysms.people.v1.GetPersonAddressResponse;
import com.mysms.people.v1.GetPersonDocumentRequest;
import com.mysms.people.v1.GetPersonDocumentResponse;
import com.mysms.people.v1.GetPersonRequest;
import com.mysms.people.v1.GetPersonResponse;
import com.mysms.people.v1.GetPersonSkillRequest;
import com.mysms.people.v1.GetPersonSkillResponse;
import com.mysms.people.v1.GetPersonSocialLinkRequest;
import com.mysms.people.v1.GetPersonSocialLinkResponse;
import com.mysms.people.v1.GetPublicProfileRequest;
import com.mysms.people.v1.GetPublicProfileResponse;
import com.mysms.people.v1.GetWorkExperienceRequest;
import com.mysms.people.v1.GetWorkExperienceResponse;
import com.mysms.people.v1.ListBirthdaysRequest;
import com.mysms.people.v1.ListBirthdaysResponse;
import com.mysms.people.v1.ListCertificationsRequest;
import com.mysms.people.v1.ListCertificationsResponse;
import com.mysms.people.v1.ListEducationHistoryRequest;
import com.mysms.people.v1.ListEducationHistoryResponse;
import com.mysms.people.v1.ListEmergencyContactsRequest;
import com.mysms.people.v1.ListEmergencyContactsResponse;
import com.mysms.people.v1.ListPersonAddressesRequest;
import com.mysms.people.v1.ListPersonAddressesResponse;
import com.mysms.people.v1.ListPersonDocumentsRequest;
import com.mysms.people.v1.ListPersonDocumentsResponse;
import com.mysms.people.v1.ListPersonSkillsRequest;
import com.mysms.people.v1.ListPersonSkillsResponse;
import com.mysms.people.v1.ListPersonSocialLinksRequest;
import com.mysms.people.v1.ListPersonSocialLinksResponse;
import com.mysms.people.v1.ListPersonsRequest;
import com.mysms.people.v1.ListPersonsResponse;
import com.mysms.people.v1.ListWorkExperiencesRequest;
import com.mysms.people.v1.ListWorkExperiencesResponse;
import com.mysms.people.v1.MyPersonUpdate;
import com.mysms.people.v1.PersonAddressUpdate;
import com.mysms.people.v1.PersonDocumentUpdate;
import com.mysms.people.v1.PersonServiceGrpc;
import com.mysms.people.v1.PersonSkillUpdate;
import com.mysms.people.v1.PersonSocialLinkUpdate;
import com.mysms.people.v1.PersonUpdate;
import com.mysms.people.v1.PublicProfileUpdate;
import com.mysms.people.v1.SendGreetingsRequest;
import com.mysms.people.v1.SendGreetingsResponse;
import com.mysms.people.v1.UpdateCertificationResponse;
import com.mysms.people.v1.UpdateEducationHistoryResponse;
import com.mysms.people.v1.UpdateEmergencyContactResponse;
import com.mysms.people.v1.UpdateMyPersonResponse;
import com.mysms.people.v1.UpdatePersonAddressResponse;
import com.mysms.people.v1.UpdatePersonDocumentResponse;
import com.mysms.people.v1.UpdatePersonResponse;
import com.mysms.people.v1.UpdatePersonSkillResponse;
import com.mysms.people.v1.UpdatePersonSocialLinkResponse;
import com.mysms.people.v1.UpdatePublicProfileResponse;
import com.mysms.people.v1.UpdateWorkExperienceResponse;
import com.mysms.people.v1.WorkExperienceUpdate;
import io.grpc.stub.StreamObserver;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of people.PersonService: delegates every RPC to the business service REST uses, so the rules exist
 * once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class PersonResolver extends PersonServiceGrpc.PersonServiceImplBase {

    private final MyPersonService myPerson;
    private final PersonService personService;
    private final GreetingService greetingService;
    private final ProtoBridge bridge;

    public PersonResolver(MyPersonService myPerson, PersonService personService, GreetingService greetingService,
            ProtoBridge bridge) {
        this.myPerson = myPerson;
        this.personService = personService;
        this.greetingService = greetingService;
        this.bridge = bridge;
    }

    @Override
    public void getMyPerson(GetMyPersonRequest request, StreamObserver<GetMyPersonResponse> out) {
        Person changed = myPerson.get();
        ProtoBridge.send(out, bridge.reply(GetMyPersonResponse.newBuilder(), "person", changed).build());
    }

    @Override
    public void updateMyPerson(MyPersonUpdate request, StreamObserver<UpdateMyPersonResponse> out) {
        Person updated = myPerson.update(bridge.requiredIfMatch(request.getEtag()),
                MyPersonProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateMyPersonResponse.newBuilder(), "person", updated).build());
    }

    @Override
    public void listPersons(ListPersonsRequest request, StreamObserver<ListPersonsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Person> rows = personService.listPersons(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPersonsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPerson(GetPersonRequest request, StreamObserver<GetPersonResponse> out) {
        Person found = personService.getPerson(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPersonResponse.newBuilder(), "person", found).build());
    }

    @Override
    public void createPerson(CreatePersonRequest request, StreamObserver<CreatePersonResponse> out) {
        Person created = personService.createPerson(bridge.body(request.getPerson(), Person.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreatePersonResponse.newBuilder(), "person", created).build());
    }

    @Override
    public void updatePerson(PersonUpdate request, StreamObserver<UpdatePersonResponse> out) {
        Person updated = personService.updatePerson(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()),
                PersonProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdatePersonResponse.newBuilder(), "person", updated).build());
    }

    @Override
    public void deletePerson(DeletePersonRequest request, StreamObserver<DeletePersonResponse> out) {
        personService.deletePerson(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePersonResponse.getDefaultInstance());
    }

    @Override
    public void listPersonAddresses(ListPersonAddressesRequest request, StreamObserver<ListPersonAddressesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<PersonAddress> rows = personService.listPersonAddresss(
                bridge.id(request.getPersonId(), "personId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPersonAddressesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPersonAddress(GetPersonAddressRequest request, StreamObserver<GetPersonAddressResponse> out) {
        PersonAddress found = personService.getPersonAddress(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPersonAddressResponse.newBuilder(), "person_address", found).build());
    }

    @Override
    public void createPersonAddress(CreatePersonAddressRequest request, StreamObserver<CreatePersonAddressResponse> out) {
        PersonAddress created = personService.createPersonAddress(
                bridge.id(request.getPersonId(), "personId"), bridge.body(request.getPersonAddress(), PersonAddress.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreatePersonAddressResponse.newBuilder(), "person_address", created).build());
    }

    @Override
    public void updatePersonAddress(PersonAddressUpdate request, StreamObserver<UpdatePersonAddressResponse> out) {
        PersonAddress updated = personService.updatePersonAddress(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                PersonAddressProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdatePersonAddressResponse.newBuilder(), "person_address", updated).build());
    }

    @Override
    public void deletePersonAddress(DeletePersonAddressRequest request, StreamObserver<DeletePersonAddressResponse> out) {
        personService.deletePersonAddress(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePersonAddressResponse.getDefaultInstance());
    }

    @Override
    public void listEmergencyContacts(ListEmergencyContactsRequest request, StreamObserver<ListEmergencyContactsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<EmergencyContact> rows = personService.listEmergencyContacts(
                bridge.id(request.getPersonId(), "personId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListEmergencyContactsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getEmergencyContact(GetEmergencyContactRequest request, StreamObserver<GetEmergencyContactResponse> out) {
        EmergencyContact found = personService.getEmergencyContact(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetEmergencyContactResponse.newBuilder(), "emergency_contact", found).build());
    }

    @Override
    public void createEmergencyContact(CreateEmergencyContactRequest request,
            StreamObserver<CreateEmergencyContactResponse> out) {
        EmergencyContact created = personService.createEmergencyContact(
                bridge.id(request.getPersonId(), "personId"),
                bridge.body(request.getEmergencyContact(), EmergencyContact.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateEmergencyContactResponse.newBuilder(), "emergency_contact", created).build());
    }

    @Override
    public void updateEmergencyContact(EmergencyContactUpdate request,
            StreamObserver<UpdateEmergencyContactResponse> out) {
        EmergencyContact updated = personService.updateEmergencyContact(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                EmergencyContactProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateEmergencyContactResponse.newBuilder(), "emergency_contact", updated).build());
    }

    @Override
    public void deleteEmergencyContact(DeleteEmergencyContactRequest request,
            StreamObserver<DeleteEmergencyContactResponse> out) {
        personService.deleteEmergencyContact(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteEmergencyContactResponse.getDefaultInstance());
    }

    @Override
    public void listEducationHistory(ListEducationHistoryRequest request, StreamObserver<ListEducationHistoryResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<EducationHistory> rows = personService.listEducationHistorys(
                bridge.id(request.getPersonId(), "personId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListEducationHistoryResponse.newBuilder(), rows).build());
    }

    @Override
    public void getEducationHistory(GetEducationHistoryRequest request, StreamObserver<GetEducationHistoryResponse> out) {
        EducationHistory found = personService.getEducationHistory(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetEducationHistoryResponse.newBuilder(), "education_history", found).build());
    }

    @Override
    public void createEducationHistory(CreateEducationHistoryRequest request,
            StreamObserver<CreateEducationHistoryResponse> out) {
        EducationHistory created = personService.createEducationHistory(
                bridge.id(request.getPersonId(), "personId"),
                bridge.body(request.getEducationHistory(), EducationHistory.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateEducationHistoryResponse.newBuilder(), "education_history", created).build());
    }

    @Override
    public void updateEducationHistory(EducationHistoryUpdate request,
            StreamObserver<UpdateEducationHistoryResponse> out) {
        EducationHistory updated = personService.updateEducationHistory(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                EducationHistoryProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateEducationHistoryResponse.newBuilder(), "education_history", updated).build());
    }

    @Override
    public void deleteEducationHistory(DeleteEducationHistoryRequest request,
            StreamObserver<DeleteEducationHistoryResponse> out) {
        personService.deleteEducationHistory(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteEducationHistoryResponse.getDefaultInstance());
    }

    @Override
    public void listWorkExperiences(ListWorkExperiencesRequest request, StreamObserver<ListWorkExperiencesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<WorkExperience> rows = personService.listWorkExperiences(
                bridge.id(request.getPersonId(), "personId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListWorkExperiencesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getWorkExperience(GetWorkExperienceRequest request, StreamObserver<GetWorkExperienceResponse> out) {
        WorkExperience found = personService.getWorkExperience(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetWorkExperienceResponse.newBuilder(), "work_experience", found).build());
    }

    @Override
    public void createWorkExperience(CreateWorkExperienceRequest request, StreamObserver<CreateWorkExperienceResponse> out) {
        WorkExperience created = personService.createWorkExperience(
                bridge.id(request.getPersonId(), "personId"), bridge.body(request.getWorkExperience(), WorkExperience.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateWorkExperienceResponse.newBuilder(), "work_experience", created).build());
    }

    @Override
    public void updateWorkExperience(WorkExperienceUpdate request, StreamObserver<UpdateWorkExperienceResponse> out) {
        WorkExperience updated = personService.updateWorkExperience(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                WorkExperienceProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateWorkExperienceResponse.newBuilder(), "work_experience", updated).build());
    }

    @Override
    public void deleteWorkExperience(DeleteWorkExperienceRequest request, StreamObserver<DeleteWorkExperienceResponse> out) {
        personService.deleteWorkExperience(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteWorkExperienceResponse.getDefaultInstance());
    }

    @Override
    public void listCertifications(ListCertificationsRequest request, StreamObserver<ListCertificationsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Certification> rows = personService.listCertifications(
                bridge.id(request.getPersonId(), "personId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCertificationsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCertification(GetCertificationRequest request, StreamObserver<GetCertificationResponse> out) {
        Certification found = personService.getCertification(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCertificationResponse.newBuilder(), "certification", found).build());
    }

    @Override
    public void createCertification(CreateCertificationRequest request, StreamObserver<CreateCertificationResponse> out) {
        Certification created = personService.createCertification(
                bridge.id(request.getPersonId(), "personId"), bridge.body(request.getCertification(), Certification.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCertificationResponse.newBuilder(), "certification", created).build());
    }

    @Override
    public void updateCertification(CertificationUpdate request, StreamObserver<UpdateCertificationResponse> out) {
        Certification updated = personService.updateCertification(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                CertificationProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCertificationResponse.newBuilder(), "certification", updated).build());
    }

    @Override
    public void deleteCertification(DeleteCertificationRequest request, StreamObserver<DeleteCertificationResponse> out) {
        personService.deleteCertification(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCertificationResponse.getDefaultInstance());
    }

    @Override
    public void listPersonSkills(ListPersonSkillsRequest request, StreamObserver<ListPersonSkillsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<PersonSkill> rows = personService.listPersonSkills(
                bridge.id(request.getPersonId(), "personId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPersonSkillsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPersonSkill(GetPersonSkillRequest request, StreamObserver<GetPersonSkillResponse> out) {
        PersonSkill found = personService.getPersonSkill(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPersonSkillResponse.newBuilder(), "person_skill", found).build());
    }

    @Override
    public void createPersonSkill(CreatePersonSkillRequest request, StreamObserver<CreatePersonSkillResponse> out) {
        PersonSkill created = personService.createPersonSkill(
                bridge.id(request.getPersonId(), "personId"), bridge.body(request.getPersonSkill(), PersonSkill.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreatePersonSkillResponse.newBuilder(), "person_skill", created).build());
    }

    @Override
    public void updatePersonSkill(PersonSkillUpdate request, StreamObserver<UpdatePersonSkillResponse> out) {
        PersonSkill updated = personService.updatePersonSkill(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                PersonSkillProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdatePersonSkillResponse.newBuilder(), "person_skill", updated).build());
    }

    @Override
    public void deletePersonSkill(DeletePersonSkillRequest request, StreamObserver<DeletePersonSkillResponse> out) {
        personService.deletePersonSkill(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePersonSkillResponse.getDefaultInstance());
    }

    @Override
    public void listPersonDocuments(ListPersonDocumentsRequest request, StreamObserver<ListPersonDocumentsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<PersonDocument> rows = personService.listPersonDocuments(
                bridge.id(request.getPersonId(), "personId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPersonDocumentsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPersonDocument(GetPersonDocumentRequest request, StreamObserver<GetPersonDocumentResponse> out) {
        PersonDocument found = personService.getPersonDocument(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPersonDocumentResponse.newBuilder(), "person_document", found).build());
    }

    @Override
    public void createPersonDocument(CreatePersonDocumentRequest request, StreamObserver<CreatePersonDocumentResponse> out) {
        PersonDocument created = personService.createPersonDocument(
                bridge.id(request.getPersonId(), "personId"), bridge.body(request.getPersonDocument(), PersonDocument.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreatePersonDocumentResponse.newBuilder(), "person_document", created).build());
    }

    @Override
    public void updatePersonDocument(PersonDocumentUpdate request, StreamObserver<UpdatePersonDocumentResponse> out) {
        PersonDocument updated = personService.updatePersonDocument(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                PersonDocumentProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdatePersonDocumentResponse.newBuilder(), "person_document", updated).build());
    }

    @Override
    public void deletePersonDocument(DeletePersonDocumentRequest request, StreamObserver<DeletePersonDocumentResponse> out) {
        personService.deletePersonDocument(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePersonDocumentResponse.getDefaultInstance());
    }

    @Override
    public void getPublicProfile(GetPublicProfileRequest request, StreamObserver<GetPublicProfileResponse> out) {
        PublicProfile found = personService.getPublicProfile(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPublicProfileResponse.newBuilder(), "public_profile", found).build());
    }

    @Override
    public void createPublicProfile(CreatePublicProfileRequest request, StreamObserver<CreatePublicProfileResponse> out) {
        PublicProfile created = personService.createPublicProfile(
                bridge.id(request.getPersonId(), "personId"), bridge.body(request.getPublicProfile(), PublicProfile.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreatePublicProfileResponse.newBuilder(), "public_profile", created).build());
    }

    @Override
    public void updatePublicProfile(PublicProfileUpdate request, StreamObserver<UpdatePublicProfileResponse> out) {
        PublicProfile updated = personService.updatePublicProfile(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                PublicProfileProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdatePublicProfileResponse.newBuilder(), "public_profile", updated).build());
    }

    @Override
    public void deletePublicProfile(DeletePublicProfileRequest request, StreamObserver<DeletePublicProfileResponse> out) {
        personService.deletePublicProfile(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePublicProfileResponse.getDefaultInstance());
    }

    @Override
    public void listPersonSocialLinks(ListPersonSocialLinksRequest request, StreamObserver<ListPersonSocialLinksResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<PersonSocialLink> rows = personService.listPersonSocialLinks(
                bridge.id(request.getPersonId(), "personId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPersonSocialLinksResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPersonSocialLink(GetPersonSocialLinkRequest request, StreamObserver<GetPersonSocialLinkResponse> out) {
        PersonSocialLink found = personService.getPersonSocialLink(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPersonSocialLinkResponse.newBuilder(), "person_social_link", found).build());
    }

    @Override
    public void createPersonSocialLink(CreatePersonSocialLinkRequest request,
            StreamObserver<CreatePersonSocialLinkResponse> out) {
        PersonSocialLink created = personService.createPersonSocialLink(
                bridge.id(request.getPersonId(), "personId"),
                bridge.body(request.getPersonSocialLink(), PersonSocialLink.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreatePersonSocialLinkResponse.newBuilder(), "person_social_link", created).build());
    }

    @Override
    public void updatePersonSocialLink(PersonSocialLinkUpdate request,
            StreamObserver<UpdatePersonSocialLinkResponse> out) {
        PersonSocialLink updated = personService.updatePersonSocialLink(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                PersonSocialLinkProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdatePersonSocialLinkResponse.newBuilder(), "person_social_link", updated).build());
    }

    @Override
    public void deletePersonSocialLink(DeletePersonSocialLinkRequest request,
            StreamObserver<DeletePersonSocialLinkResponse> out) {
        personService.deletePersonSocialLink(
                bridge.id(request.getPersonId(), "personId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePersonSocialLinkResponse.getDefaultInstance());
    }

    /** REST's query string as fields: the institution and both days are required; kinds and roles narrow. */
    @Override
    public void listBirthdays(ListBirthdaysRequest request, StreamObserver<ListBirthdaysResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<BirthdayEntry> rows = greetingService.listBirthdays(bridge.id(request.getInstitutionId(),
                        "institutionId"), date(request.getFromDate(), "fromDate"), date(request.getToDate(), "toDate"),
                request.getKindsList(), request.getRolesList(), page.page(), page.size(), page.cursor());
        ProtoBridge.send(out, bridge.page(ListBirthdaysResponse.newBuilder(), rows).build());
    }

    @Override
    public void sendGreetings(SendGreetingsRequest request, StreamObserver<SendGreetingsResponse> out) {
        SendResult sent = greetingService.sendGreetings(bridge.body(request,
                com.mysms.identity.people.api.model.SendGreetingsRequest.class, "request_id"));
        ProtoBridge.send(out, bridge.reply(SendGreetingsResponse.newBuilder(), "result", sent).build());
    }

    /** A required ISO date of REST's query string: 400 when absent or malformed, like REST's parameter binding. */
    private static LocalDate date(String value, String name) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new InvalidArgumentException("Invalid value for " + name + ".");
        }
    }
}
