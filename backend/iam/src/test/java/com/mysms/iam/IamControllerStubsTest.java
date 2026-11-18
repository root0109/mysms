package com.mysms.iam;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ControllerStubs;
import com.mysms.iam.controller.RoleController;
import com.mysms.iam.controller.UserAccountController;
import org.junit.jupiter.api.Test;

/**
 * Every contract operation of iam is served: no controller method answers 501, and a stub added back would change the
 * count on purpose.
 */
class IamControllerStubsTest {

    @Test
    void unservedOperationsAnswerNotImplemented() {
        assertThat(ControllerStubs.count(RoleController.class)).as("Role").isEqualTo(0);
        assertThat(ControllerStubs.count(UserAccountController.class)).as("UserAccount").isEqualTo(0);
    }
}
