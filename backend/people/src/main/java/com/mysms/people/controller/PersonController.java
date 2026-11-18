package com.mysms.people.controller;

import com.mysms.people.api.model.BirthdayEntry;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.people.api.PersonApi;
import com.mysms.people.api.model.Certification;
import com.mysms.people.api.model.CertificationUpdate;
import com.mysms.people.api.model.ConsentRecord;
import com.mysms.people.api.model.EducationHistory;
import com.mysms.people.api.model.EducationHistoryUpdate;
import com.mysms.people.api.model.EmergencyContact;
import com.mysms.people.api.model.EmergencyContactUpdate;
import com.mysms.people.api.model.MyPersonUpdate;
import com.mysms.people.api.model.Person;
import com.mysms.people.api.model.PersonAddress;
import com.mysms.people.api.model.PersonAddressUpdate;
import com.mysms.people.api.model.PersonDocument;
import com.mysms.people.api.model.PersonDocumentUpdate;
import com.mysms.people.api.model.PersonSkill;
import com.mysms.people.api.model.PersonSkillUpdate;
import com.mysms.people.api.model.PersonSocialLink;
import com.mysms.people.api.model.PersonSocialLinkUpdate;
import com.mysms.people.api.model.PersonUpdate;
import com.mysms.people.api.model.PublicProfile;
import com.mysms.people.api.model.PublicProfileUpdate;
import com.mysms.people.api.model.SendGreetingsRequest;
import com.mysms.people.api.model.SendResult;
import com.mysms.people.api.model.WithdrawConsentRequest;
import com.mysms.people.api.model.WorkExperience;
import com.mysms.people.api.model.WorkExperienceUpdate;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.people.business.MyPersonService;
import com.mysms.people.business.PersonService;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Person operations; authentication and the permission check already happened in the auth-client
 * filter; operations not served yet answer 501 NOT_IMPLEMENTED. getMyPerson and updateMyPerson carry their own rules.
 */
@RestController
public class PersonController implements PersonApi {

    private final MyPersonService myPerson;
    private final PersonService personService;

    public PersonController(MyPersonService myPerson, PersonService personService) {
        this.myPerson = myPerson;
        this.personService = personService;
    }

    @Override
    public ResponseEntity<Certification> createCertification(UUID personId, Certification certification,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Certification created = personService.createCertification(personId, certification, dryRun);
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
        EducationHistory created = personService.createEducationHistory(personId, educationHistory, dryRun);
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
        EmergencyContact created = personService.createEmergencyContact(personId, emergencyContact, dryRun);
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
        PersonAddress created = personService.createPersonAddress(personId, personAddress, dryRun);
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
        PersonDocument created = personService.createPersonDocument(personId, personDocument, dryRun);
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
        PersonSkill created = personService.createPersonSkill(personId, personSkill, dryRun);
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
        PersonSocialLink created = personService.createPersonSocialLink(personId, personSocialLink, dryRun);
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
        PublicProfile created = personService.createPublicProfile(personId, publicProfile, dryRun);
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
        WorkExperience created = personService.createWorkExperience(personId, workExperience, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/people/persons/" + personId + "/work-experience/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteCertification(UUID personId, UUID certificationId, String ifMatch) {
        personService.deleteCertification(personId, certificationId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteEducationHistory(UUID personId, UUID educationHistoryId,
            String ifMatch) {
        personService.deleteEducationHistory(personId, educationHistoryId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteEmergencyContact(UUID personId, UUID emergencyContactId,
            String ifMatch) {
        personService.deleteEmergencyContact(personId, emergencyContactId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deletePerson(UUID personId, String ifMatch) {
        personService.deletePerson(personId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deletePersonAddress(UUID personId, UUID personAddressId, String ifMatch) {
        personService.deletePersonAddress(personId, personAddressId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deletePersonDocument(UUID personId, UUID personDocumentId,
            String ifMatch) {
        personService.deletePersonDocument(personId, personDocumentId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deletePersonSkill(UUID personId, UUID personSkillId, String ifMatch) {
        personService.deletePersonSkill(personId, personSkillId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deletePersonSocialLink(UUID personId, UUID personSocialLinkId,
            String ifMatch) {
        personService.deletePersonSocialLink(personId, personSocialLinkId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deletePublicProfile(UUID personId, UUID publicProfileId, String ifMatch) {
        personService.deletePublicProfile(personId, publicProfileId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteWorkExperience(UUID personId, UUID workExperienceId,
            String ifMatch) {
        personService.deleteWorkExperience(personId, workExperienceId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Certification> getCertification(UUID personId, UUID certificationId,
            String fields) {
        Certification found = personService.getCertification(personId, certificationId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ConsentRecord> getConsentRecord(UUID personId, UUID consentRecordId,
            String fields) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<PagingInfo<ConsentRecord>> getConsentStatus(UUID personId, Integer page, Integer size,
            String cursor) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<EducationHistory> getEducationHistory(UUID personId, UUID educationHistoryId,
            String fields) {
        EducationHistory found = personService.getEducationHistory(personId, educationHistoryId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<EmergencyContact> getEmergencyContact(UUID personId, UUID emergencyContactId,
            String fields) {
        EmergencyContact found = personService.getEmergencyContact(personId, emergencyContactId);
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
        PersonAddress found = personService.getPersonAddress(personId, personAddressId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PersonDocument> getPersonDocument(UUID personId, UUID personDocumentId,
            String fields) {
        PersonDocument found = personService.getPersonDocument(personId, personDocumentId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PersonSkill> getPersonSkill(UUID personId, UUID personSkillId, String fields) {
        PersonSkill found = personService.getPersonSkill(personId, personSkillId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PersonSocialLink> getPersonSocialLink(UUID personId, UUID personSocialLinkId,
            String fields) {
        PersonSocialLink found = personService.getPersonSocialLink(personId, personSocialLinkId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PublicProfile> getPublicProfile(UUID personId, UUID publicProfileId,
            String fields) {
        PublicProfile found = personService.getPublicProfile(personId, publicProfileId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<WorkExperience> getWorkExperience(UUID personId, UUID workExperienceId,
            String fields) {
        WorkExperience found = personService.getWorkExperience(personId, workExperienceId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<BirthdayEntry>> listBirthdays(UUID institutionId, LocalDate fromDate,
            LocalDate toDate, List<String> kinds, List<String> roles, Integer page, Integer size, String cursor) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<PagingInfo<Certification>> listCertifications(UUID personId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<Certification> list =
                PagingUtil.pagingInfo(personService.listCertifications(personId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<ConsentRecord>> listConsentRecords(UUID personId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<PagingInfo<EducationHistory>> listEducationHistory(UUID personId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<EducationHistory> list =
                PagingUtil.pagingInfo(personService.listEducationHistorys(personId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<EmergencyContact>> listEmergencyContacts(UUID personId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<EmergencyContact> list =
                PagingUtil.pagingInfo(personService.listEmergencyContacts(personId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<PersonAddress>> listPersonAddresses(UUID personId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<PersonAddress> list =
                PagingUtil.pagingInfo(personService.listPersonAddresss(personId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<PersonDocument>> listPersonDocuments(UUID personId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<PersonDocument> list =
                PagingUtil.pagingInfo(personService.listPersonDocuments(personId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<PersonSkill>> listPersonSkills(UUID personId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<PersonSkill> list =
                PagingUtil.pagingInfo(personService.listPersonSkills(personId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<PersonSocialLink>> listPersonSocialLinks(UUID personId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<PersonSocialLink> list =
                PagingUtil.pagingInfo(personService.listPersonSocialLinks(personId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Person>> listPersons(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Person> list = PagingUtil.pagingInfo(personService.listPersons(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<WorkExperience>> listWorkExperiences(UUID personId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<WorkExperience> list =
                PagingUtil.pagingInfo(personService.listWorkExperiences(personId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
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
                personService.updateCertification(personId, certificationId, ifMatch, certificationUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<EducationHistory> updateEducationHistory(UUID personId, UUID educationHistoryId,
            String ifMatch, EducationHistoryUpdate educationHistoryUpdate, String updateMask, Boolean validateOnly) {
        EducationHistory updated =
                personService.updateEducationHistory(personId, educationHistoryId, ifMatch,
                        educationHistoryUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<EmergencyContact> updateEmergencyContact(UUID personId, UUID emergencyContactId,
            String ifMatch, EmergencyContactUpdate emergencyContactUpdate, String updateMask, Boolean validateOnly) {
        EmergencyContact updated =
                personService.updateEmergencyContact(personId, emergencyContactId, ifMatch,
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
                personService.updatePersonAddress(personId, personAddressId, ifMatch, personAddressUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<PersonDocument> updatePersonDocument(UUID personId, UUID personDocumentId,
            String ifMatch, PersonDocumentUpdate personDocumentUpdate, String updateMask, Boolean validateOnly) {
        PersonDocument updated =
                personService.updatePersonDocument(personId, personDocumentId, ifMatch, personDocumentUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<PersonSkill> updatePersonSkill(UUID personId, UUID personSkillId,
            String ifMatch, PersonSkillUpdate personSkillUpdate, String updateMask, Boolean validateOnly) {
        PersonSkill updated =
                personService.updatePersonSkill(personId, personSkillId, ifMatch, personSkillUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<PersonSocialLink> updatePersonSocialLink(UUID personId, UUID personSocialLinkId,
            String ifMatch, PersonSocialLinkUpdate personSocialLinkUpdate, String updateMask, Boolean validateOnly) {
        PersonSocialLink updated =
                personService.updatePersonSocialLink(personId, personSocialLinkId, ifMatch,
                        personSocialLinkUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<PublicProfile> updatePublicProfile(UUID personId, UUID publicProfileId,
            String ifMatch, PublicProfileUpdate publicProfileUpdate, String updateMask, Boolean validateOnly) {
        PublicProfile updated =
                personService.updatePublicProfile(personId, publicProfileId, ifMatch, publicProfileUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<WorkExperience> updateWorkExperience(UUID personId, UUID workExperienceId,
            String ifMatch, WorkExperienceUpdate workExperienceUpdate, String updateMask, Boolean validateOnly) {
        WorkExperience updated =
                personService.updateWorkExperience(personId, workExperienceId, ifMatch, workExperienceUpdate,
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
