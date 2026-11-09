package com.mysms.people.controller;

import com.mysms.contracts.people.api.PersonApi;
import com.mysms.contracts.people.model.BirthdayEntryList;
import com.mysms.contracts.people.model.Certification;
import com.mysms.contracts.people.model.CertificationList;
import com.mysms.contracts.people.model.CertificationUpdate;
import com.mysms.contracts.people.model.ConsentRecord;
import com.mysms.contracts.people.model.ConsentRecordList;
import com.mysms.contracts.people.model.EducationHistory;
import com.mysms.contracts.people.model.EducationHistoryList;
import com.mysms.contracts.people.model.EducationHistoryUpdate;
import com.mysms.contracts.people.model.EmergencyContact;
import com.mysms.contracts.people.model.EmergencyContactList;
import com.mysms.contracts.people.model.EmergencyContactUpdate;
import com.mysms.contracts.people.model.MyPersonUpdate;
import com.mysms.contracts.people.model.Person;
import com.mysms.contracts.people.model.PersonAddress;
import com.mysms.contracts.people.model.PersonAddressList;
import com.mysms.contracts.people.model.PersonAddressUpdate;
import com.mysms.contracts.people.model.PersonDocument;
import com.mysms.contracts.people.model.PersonDocumentList;
import com.mysms.contracts.people.model.PersonDocumentUpdate;
import com.mysms.contracts.people.model.PersonList;
import com.mysms.contracts.people.model.PersonSkill;
import com.mysms.contracts.people.model.PersonSkillList;
import com.mysms.contracts.people.model.PersonSkillUpdate;
import com.mysms.contracts.people.model.PersonSocialLink;
import com.mysms.contracts.people.model.PersonSocialLinkList;
import com.mysms.contracts.people.model.PersonSocialLinkUpdate;
import com.mysms.contracts.people.model.PersonUpdate;
import com.mysms.contracts.people.model.PublicProfile;
import com.mysms.contracts.people.model.PublicProfileUpdate;
import com.mysms.contracts.people.model.SendGreetingsRequest;
import com.mysms.contracts.people.model.SendResult;
import com.mysms.contracts.people.model.WithdrawConsentRequest;
import com.mysms.contracts.people.model.WorkExperience;
import com.mysms.contracts.people.model.WorkExperienceList;
import com.mysms.contracts.people.model.WorkExperienceUpdate;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.people.business.CertificationService;
import com.mysms.people.business.EducationHistoryService;
import com.mysms.people.business.EmergencyContactService;
import com.mysms.people.business.MyPersonService;
import com.mysms.people.business.PersonAddressService;
import com.mysms.people.business.PersonDocumentService;
import com.mysms.people.business.PersonService;
import com.mysms.people.business.PersonSkillService;
import com.mysms.people.business.PersonSocialLinkService;
import com.mysms.people.business.PublicProfileService;
import com.mysms.people.business.WorkExperienceService;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Person operations: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/people.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED. Standard operations follow the generated pattern (tools/backend/crud, merged by
 * merge_controllers.py); getMyPerson, updateMyPerson are hand-written.
 */
@RestController
public class PersonController implements PersonApi {

    private final MyPersonService myPerson;
    private final CertificationService certificationService;
    private final EducationHistoryService educationHistoryService;
    private final EmergencyContactService emergencyContactService;
    private final PersonAddressService personAddressService;
    private final PersonDocumentService personDocumentService;
    private final PersonService personService;
    private final PersonSkillService personSkillService;
    private final PersonSocialLinkService personSocialLinkService;
    private final PublicProfileService publicProfileService;
    private final WorkExperienceService workExperienceService;

    public PersonController(MyPersonService myPerson, CertificationService certificationService,
            EducationHistoryService educationHistoryService,
            EmergencyContactService emergencyContactService, PersonAddressService personAddressService,
            PersonDocumentService personDocumentService, PersonService personService,
            PersonSkillService personSkillService, PersonSocialLinkService personSocialLinkService,
            PublicProfileService publicProfileService, WorkExperienceService workExperienceService) {
        this.myPerson = myPerson;
        this.certificationService = certificationService;
        this.educationHistoryService = educationHistoryService;
        this.emergencyContactService = emergencyContactService;
        this.personAddressService = personAddressService;
        this.personDocumentService = personDocumentService;
        this.personService = personService;
        this.personSkillService = personSkillService;
        this.personSocialLinkService = personSocialLinkService;
        this.publicProfileService = publicProfileService;
        this.workExperienceService = workExperienceService;
    }

    @Override
    public ResponseEntity<Certification> createCertification(UUID personId, Certification certification,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Certification created = certificationService.createCertification(personId, certification, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/people/persons/" + personId + "/certifications/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<ConsentRecord> createConsentRecord(UUID personId, ConsentRecord consentRecord,
            UUID idempotencyKey, Boolean validateOnly) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<EducationHistory> createEducationHistory(UUID personId,
            EducationHistory educationHistory, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        EducationHistory created = educationHistoryService.createEducationHistory(personId, educationHistory, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/people/persons/" + personId + "/education/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<EmergencyContact> createEmergencyContact(UUID personId,
            EmergencyContact emergencyContact, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        EmergencyContact created = emergencyContactService.createEmergencyContact(personId, emergencyContact, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/people/persons/" + personId + "/emergency-contacts/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Person> createPerson(Person person, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Person created = personService.createPerson(person, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/people/persons/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<PersonAddress> createPersonAddress(UUID personId, PersonAddress personAddress,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        PersonAddress created = personAddressService.createPersonAddress(personId, personAddress, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/people/persons/" + personId + "/addresses/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<PersonDocument> createPersonDocument(UUID personId,
            PersonDocument personDocument, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        PersonDocument created = personDocumentService.createPersonDocument(personId, personDocument, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/people/persons/" + personId + "/documents/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<PersonSkill> createPersonSkill(UUID personId, PersonSkill personSkill,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        PersonSkill created = personSkillService.createPersonSkill(personId, personSkill, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/people/persons/" + personId + "/skills/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<PersonSocialLink> createPersonSocialLink(UUID personId,
            PersonSocialLink personSocialLink, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        PersonSocialLink created = personSocialLinkService.createPersonSocialLink(personId, personSocialLink, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/people/persons/" + personId + "/social-links/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<PublicProfile> createPublicProfile(UUID personId, PublicProfile publicProfile,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        PublicProfile created = publicProfileService.createPublicProfile(personId, publicProfile, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/people/persons/" + personId + "/public-profile/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<WorkExperience> createWorkExperience(UUID personId,
            WorkExperience workExperience, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        WorkExperience created = workExperienceService.createWorkExperience(personId, workExperience, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/people/persons/" + personId + "/work-experience/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteCertification(UUID personId, UUID certificationId, String ifMatch) {
        certificationService.deleteCertification(personId, certificationId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteEducationHistory(UUID personId, UUID educationHistoryId,
            String ifMatch) {
        educationHistoryService.deleteEducationHistory(personId, educationHistoryId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteEmergencyContact(UUID personId, UUID emergencyContactId,
            String ifMatch) {
        emergencyContactService.deleteEmergencyContact(personId, emergencyContactId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deletePerson(UUID personId, String ifMatch) {
        personService.deletePerson(personId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deletePersonAddress(UUID personId, UUID personAddressId, String ifMatch) {
        personAddressService.deletePersonAddress(personId, personAddressId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deletePersonDocument(UUID personId, UUID personDocumentId,
            String ifMatch) {
        personDocumentService.deletePersonDocument(personId, personDocumentId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deletePersonSkill(UUID personId, UUID personSkillId, String ifMatch) {
        personSkillService.deletePersonSkill(personId, personSkillId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deletePersonSocialLink(UUID personId, UUID personSocialLinkId,
            String ifMatch) {
        personSocialLinkService.deletePersonSocialLink(personId, personSocialLinkId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deletePublicProfile(UUID personId, UUID publicProfileId, String ifMatch) {
        publicProfileService.deletePublicProfile(personId, publicProfileId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteWorkExperience(UUID personId, UUID workExperienceId,
            String ifMatch) {
        workExperienceService.deleteWorkExperience(personId, workExperienceId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Certification> getCertification(UUID personId, UUID certificationId,
            String fields) {
        Certification found = certificationService.getCertification(personId, certificationId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ConsentRecord> getConsentRecord(UUID personId, UUID consentRecordId,
            String fields) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<ConsentRecordList> getConsentStatus(UUID personId, Integer pageSize,
            String pageToken) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<EducationHistory> getEducationHistory(UUID personId, UUID educationHistoryId,
            String fields) {
        EducationHistory found = educationHistoryService.getEducationHistory(personId, educationHistoryId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<EmergencyContact> getEmergencyContact(UUID personId, UUID emergencyContactId,
            String fields) {
        EmergencyContact found = emergencyContactService.getEmergencyContact(personId, emergencyContactId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<Person> getMyPerson(String fields) {
        Person found = myPerson.get();
        return ResponseEntity.ok().eTag(found.getEtag()).cacheControl(CacheControl.noStore())
                .body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<Person> getPerson(UUID personId, String fields) {
        Person found = personService.getPerson(personId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PersonAddress> getPersonAddress(UUID personId, UUID personAddressId,
            String fields) {
        PersonAddress found = personAddressService.getPersonAddress(personId, personAddressId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PersonDocument> getPersonDocument(UUID personId, UUID personDocumentId,
            String fields) {
        PersonDocument found = personDocumentService.getPersonDocument(personId, personDocumentId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PersonSkill> getPersonSkill(UUID personId, UUID personSkillId, String fields) {
        PersonSkill found = personSkillService.getPersonSkill(personId, personSkillId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PersonSocialLink> getPersonSocialLink(UUID personId, UUID personSocialLinkId,
            String fields) {
        PersonSocialLink found = personSocialLinkService.getPersonSocialLink(personId, personSocialLinkId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PublicProfile> getPublicProfile(UUID personId, UUID publicProfileId,
            String fields) {
        PublicProfile found = publicProfileService.getPublicProfile(personId, publicProfileId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<WorkExperience> getWorkExperience(UUID personId, UUID workExperienceId,
            String fields) {
        WorkExperience found = workExperienceService.getWorkExperience(personId, workExperienceId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<BirthdayEntryList> listBirthdays(UUID institutionId, LocalDate fromDate,
            LocalDate toDate, List<String> kinds, List<String> roles, Integer pageSize, String pageToken) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<CertificationList> listCertifications(UUID personId, Integer pageSize,
            String pageToken, String filter, String orderBy, String fields) {
        CertificationList list =
                certificationService.listCertifications(personId, pageSize, pageToken, filter, orderBy);
        list.getItems().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ConsentRecordList> listConsentRecords(UUID personId, Integer pageSize,
            String pageToken, String filter, String orderBy, String fields) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<EducationHistoryList> listEducationHistory(UUID personId, Integer pageSize,
            String pageToken, String filter, String orderBy, String fields) {
        EducationHistoryList list =
                educationHistoryService.listEducationHistorys(personId, pageSize, pageToken, filter, orderBy);
        list.getItems().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<EmergencyContactList> listEmergencyContacts(UUID personId, Integer pageSize,
            String pageToken, String filter, String orderBy, String fields) {
        EmergencyContactList list =
                emergencyContactService.listEmergencyContacts(personId, pageSize, pageToken, filter, orderBy);
        list.getItems().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PersonAddressList> listPersonAddresses(UUID personId, Integer pageSize,
            String pageToken, String filter, String orderBy, String fields) {
        PersonAddressList list =
                personAddressService.listPersonAddresss(personId, pageSize, pageToken, filter, orderBy);
        list.getItems().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PersonDocumentList> listPersonDocuments(UUID personId, Integer pageSize,
            String pageToken, String filter, String orderBy, String fields) {
        PersonDocumentList list =
                personDocumentService.listPersonDocuments(personId, pageSize, pageToken, filter, orderBy);
        list.getItems().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PersonSkillList> listPersonSkills(UUID personId, Integer pageSize,
            String pageToken, String filter, String orderBy, String fields) {
        PersonSkillList list = personSkillService.listPersonSkills(personId, pageSize, pageToken, filter, orderBy);
        list.getItems().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PersonSocialLinkList> listPersonSocialLinks(UUID personId, Integer pageSize,
            String pageToken, String filter, String orderBy, String fields) {
        PersonSocialLinkList list =
                personSocialLinkService.listPersonSocialLinks(personId, pageSize, pageToken, filter, orderBy);
        list.getItems().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PersonList> listPersons(Integer pageSize, String pageToken, String filter,
            String orderBy, String fields) {
        PersonList list = personService.listPersons(pageSize, pageToken, filter, orderBy);
        list.getItems().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<WorkExperienceList> listWorkExperiences(UUID personId, Integer pageSize,
            String pageToken, String filter, String orderBy, String fields) {
        WorkExperienceList list =
                workExperienceService.listWorkExperiences(personId, pageSize, pageToken, filter, orderBy);
        list.getItems().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<SendResult> sendGreetings(SendGreetingsRequest sendGreetingsRequest,
            UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Certification> updateCertification(UUID personId, UUID certificationId,
            String ifMatch, CertificationUpdate certificationUpdate, String updateMask, Boolean validateOnly) {
        Certification updated =
                certificationService.updateCertification(personId, certificationId, ifMatch, certificationUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<EducationHistory> updateEducationHistory(UUID personId, UUID educationHistoryId,
            String ifMatch, EducationHistoryUpdate educationHistoryUpdate, String updateMask, Boolean validateOnly) {
        EducationHistory updated =
                educationHistoryService.updateEducationHistory(personId, educationHistoryId, ifMatch,
                        educationHistoryUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<EmergencyContact> updateEmergencyContact(UUID personId, UUID emergencyContactId,
            String ifMatch, EmergencyContactUpdate emergencyContactUpdate, String updateMask, Boolean validateOnly) {
        EmergencyContact updated =
                emergencyContactService.updateEmergencyContact(personId, emergencyContactId, ifMatch,
                        emergencyContactUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<Person> updateMyPerson(String ifMatch, MyPersonUpdate myPersonUpdate, String updateMask,
            Boolean validateOnly) {
        Person updated = myPerson.update(ifMatch, myPersonUpdate, updateMask, Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).cacheControl(CacheControl.noStore()).body(updated);
    }

    @Override
    public ResponseEntity<Person> updatePerson(UUID personId, String ifMatch, PersonUpdate personUpdate,
            String updateMask, Boolean validateOnly) {
        Person updated = personService.updatePerson(personId, ifMatch, personUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<PersonAddress> updatePersonAddress(UUID personId, UUID personAddressId,
            String ifMatch, PersonAddressUpdate personAddressUpdate, String updateMask, Boolean validateOnly) {
        PersonAddress updated =
                personAddressService.updatePersonAddress(personId, personAddressId, ifMatch, personAddressUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<PersonDocument> updatePersonDocument(UUID personId, UUID personDocumentId,
            String ifMatch, PersonDocumentUpdate personDocumentUpdate, String updateMask, Boolean validateOnly) {
        PersonDocument updated =
                personDocumentService.updatePersonDocument(personId, personDocumentId, ifMatch, personDocumentUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<PersonSkill> updatePersonSkill(UUID personId, UUID personSkillId,
            String ifMatch, PersonSkillUpdate personSkillUpdate, String updateMask, Boolean validateOnly) {
        PersonSkill updated =
                personSkillService.updatePersonSkill(personId, personSkillId, ifMatch, personSkillUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<PersonSocialLink> updatePersonSocialLink(UUID personId, UUID personSocialLinkId,
            String ifMatch, PersonSocialLinkUpdate personSocialLinkUpdate, String updateMask, Boolean validateOnly) {
        PersonSocialLink updated =
                personSocialLinkService.updatePersonSocialLink(personId, personSocialLinkId, ifMatch,
                        personSocialLinkUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<PublicProfile> updatePublicProfile(UUID personId, UUID publicProfileId,
            String ifMatch, PublicProfileUpdate publicProfileUpdate, String updateMask, Boolean validateOnly) {
        PublicProfile updated =
                publicProfileService.updatePublicProfile(personId, publicProfileId, ifMatch, publicProfileUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<WorkExperience> updateWorkExperience(UUID personId, UUID workExperienceId,
            String ifMatch, WorkExperienceUpdate workExperienceUpdate, String updateMask, Boolean validateOnly) {
        WorkExperience updated =
                workExperienceService.updateWorkExperience(personId, workExperienceId, ifMatch, workExperienceUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ConsentRecord> withdrawConsent(UUID personId, UUID consentRecordId,
            WithdrawConsentRequest withdrawConsentRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }
}
