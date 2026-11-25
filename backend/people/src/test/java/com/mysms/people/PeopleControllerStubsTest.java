package com.mysms.people;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ControllerStubs;
import com.mysms.people.controller.HonorificController;
import com.mysms.people.controller.PersonController;
import com.mysms.people.controller.QualificationController;
import com.mysms.people.controller.SkillController;
import org.junit.jupiter.api.Test;

/**
 * The contract operations people does not serve yet answer 501 NOT_IMPLEMENTED explicitly — none falls through, and a
 * newly served operation changes the count on purpose.
 */
class PeopleControllerStubsTest {

    @Test
    void unservedOperationsAnswerNotImplemented() {
        assertThat(ControllerStubs.count(HonorificController.class)).as("Honorific").isEqualTo(0);
        assertThat(ControllerStubs.count(PersonController.class)).as("Person").isEqualTo(0);
        assertThat(ControllerStubs.count(QualificationController.class)).as("Qualification").isEqualTo(0);
        assertThat(ControllerStubs.count(SkillController.class)).as("Skill").isEqualTo(0);
    }
}
