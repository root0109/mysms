package com.mysms.students.sis.business;

import com.mysms.common.v1.PageableRequest;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.org.v1.GetInstitutionRequest;
import com.mysms.org.v1.Institution;
import com.mysms.people.v1.ListPersonsRequest;
import com.mysms.students.sis.api.model.Person;
import com.mysms.students.sis.integration.OrgGrpcClient;
import com.mysms.students.sis.integration.PeopleGrpcClient;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Gateway to what other modules know about a student, for the student summary and the child cards (an Adapter over
 * their gRPC clients): the person from people and the institution's name from org. Every call carries the caller's own
 * identity, and every part is optional: a service that cannot answer, or a caller who may not read the data there,
 * leaves the part out — the summary of a student never fails because of another module. (Attendance, library and
 * finance depend on sis, so sis never calls them.)
 */
@Component
public class StudentDetailsLookup {

    /** Ids per person list call: the list filter takes 50 terms and 2,000 characters; 40 ids fit both. */
    static final int ID_CHUNK = 40;

    private final PeopleGrpcClient people;
    private final OrgGrpcClient org;
    private final ProtoBridge bridge;

    public StudentDetailsLookup(PeopleGrpcClient people, OrgGrpcClient org, ProtoBridge bridge) {
        this.people = people;
        this.org = org;
        this.bridge = bridge;
    }

    /**
     * The person records of several students at once (people.ListPersons by id, {@value #ID_CHUNK} ids a call), as
     * sis's Person: person id → person; the ones people does not show this caller are absent.
     */
    public Map<UUID, Person> persons(Collection<UUID> personIds) {
        Map<UUID, Person> found = new HashMap<>();
        List<UUID> ids = personIds.stream().filter(Objects::nonNull).distinct().toList();
        for (int start = 0; start < ids.size(); start += ID_CHUNK) {
            String filter = ids.subList(start, Math.min(start + ID_CHUNK, ids.size())).stream()
                    .map(id -> "id = \"" + id + "\"").collect(Collectors.joining(" OR "));
            Downstream.optional("people", () -> people.listPersons(ListPersonsRequest.newBuilder().setFilter(filter)
                            .setPageable(PageableRequest.newBuilder().setPage(1).setSize(ID_CHUNK)).build()))
                    .ifPresent(rows -> rows.getItemsList().forEach(person ->
                            found.put(UUID.fromString(person.getId()), bridge.answer(person, Person.class))));
        }
        return found;
    }

    /** The institution's name (needs org.institution.read in org). */
    public Optional<String> institutionName(UUID institutionId) {
        if (institutionId == null) {
            return Optional.empty();
        }
        return Downstream.optional("org", () -> org.getInstitution(GetInstitutionRequest.newBuilder()
                .setId(institutionId.toString()).build()).getInstitution()).map(Institution::getName);
    }
}
