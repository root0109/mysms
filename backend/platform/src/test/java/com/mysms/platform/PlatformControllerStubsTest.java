package com.mysms.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ControllerStubs;
import com.mysms.platform.controller.DeletionLogController;
import com.mysms.platform.controller.EntityAttachmentController;
import com.mysms.platform.controller.EntityNoteController;
import com.mysms.platform.controller.ReservedKeywordController;
import com.mysms.platform.controller.SaasPlanController;
import com.mysms.platform.controller.SalesEnquiryController;
import com.mysms.platform.controller.StorageQuotaController;
import com.mysms.platform.controller.SystemSettingController;
import com.mysms.platform.controller.TagAssignmentController;
import com.mysms.platform.controller.TenantController;
import com.mysms.platform.controller.TenantSubscriptionController;
import com.mysms.platform.controller.UploadPolicyController;
import org.junit.jupiter.api.Test;

/**
 * The contract operations platform does not serve yet answer 501 NOT_IMPLEMENTED explicitly — none falls through, and a
 * newly served operation changes the count on purpose.
 */
class PlatformControllerStubsTest {

    @Test
    void unservedOperationsAnswerNotImplemented() {
        assertThat(ControllerStubs.count(DeletionLogController.class)).as("DeletionLog").isEqualTo(0);
        assertThat(ControllerStubs.count(EntityAttachmentController.class)).as("EntityAttachment").isEqualTo(0);
        assertThat(ControllerStubs.count(EntityNoteController.class)).as("EntityNote").isEqualTo(0);
        assertThat(ControllerStubs.count(ReservedKeywordController.class)).as("ReservedKeyword").isEqualTo(0);
        assertThat(ControllerStubs.count(SaasPlanController.class)).as("SaasPlan").isEqualTo(0);
        assertThat(ControllerStubs.count(SalesEnquiryController.class)).as("SalesEnquiry").isEqualTo(1);
        assertThat(ControllerStubs.count(StorageQuotaController.class)).as("StorageQuota").isEqualTo(1);
        assertThat(ControllerStubs.count(SystemSettingController.class)).as("SystemSetting").isEqualTo(0);
        assertThat(ControllerStubs.count(TagAssignmentController.class)).as("TagAssignment").isEqualTo(0);
        assertThat(ControllerStubs.count(TenantController.class)).as("Tenant").isEqualTo(8);
        assertThat(ControllerStubs.count(TenantSubscriptionController.class)).as("TenantSubscription").isEqualTo(2);
        assertThat(ControllerStubs.count(UploadPolicyController.class)).as("UploadPolicy").isEqualTo(0);
    }
}
