package com.mysms.org;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ControllerStubs;
import com.mysms.org.controller.AcademicYearController;
import com.mysms.org.controller.AttributeDefinitionController;
import com.mysms.org.controller.DepartmentController;
import com.mysms.org.controller.HolidayController;
import com.mysms.org.controller.InstitutionController;
import com.mysms.org.controller.InstitutionTypeController;
import org.junit.jupiter.api.Test;

/**
 * The contract operations org does not serve yet answer 501 NOT_IMPLEMENTED explicitly — none falls through, and a
 * newly served operation changes the count on purpose.
 */
class OrgControllerStubsTest {

    @Test
    void unservedOperationsAnswerNotImplemented() {
        assertThat(ControllerStubs.count(AcademicYearController.class)).as("AcademicYear").isEqualTo(0);
        assertThat(ControllerStubs.count(AttributeDefinitionController.class)).as("AttributeDefinition").isEqualTo(0);
        assertThat(ControllerStubs.count(DepartmentController.class)).as("Department").isEqualTo(0);
        assertThat(ControllerStubs.count(HolidayController.class)).as("Holiday").isEqualTo(0);
        assertThat(ControllerStubs.count(InstitutionController.class)).as("Institution").isEqualTo(0);
        assertThat(ControllerStubs.count(InstitutionTypeController.class)).as("InstitutionType").isEqualTo(0);
    }
}
