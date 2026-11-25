package com.mysms.comms;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ControllerStubs;
import com.mysms.comms.controller.AudienceGroupController;
import com.mysms.comms.controller.CalendarExportController;
import com.mysms.comms.controller.ClassPageController;
import com.mysms.comms.controller.CommentReactionController;
import com.mysms.comms.controller.ContentViewController;
import com.mysms.comms.controller.ConversationController;
import com.mysms.comms.controller.EventController;
import com.mysms.comms.controller.FeedbackThreadController;
import com.mysms.comms.controller.InAppNotificationController;
import com.mysms.comms.controller.MagazineController;
import com.mysms.comms.controller.MailboxMessageController;
import com.mysms.comms.controller.MessageTemplateController;
import com.mysms.comms.controller.NoticeController;
import com.mysms.comms.controller.NoticeBackgroundController;
import com.mysms.comms.controller.NotificationChannelController;
import com.mysms.comms.controller.NotificationPreferenceSetController;
import com.mysms.comms.controller.NotificationTypeController;
import com.mysms.comms.controller.OutboundMessageController;
import com.mysms.comms.controller.PageCategoryController;
import com.mysms.comms.controller.PostController;
import com.mysms.comms.controller.TaskController;
import org.junit.jupiter.api.Test;

/**
 * Every contract operation of comms is served: no controller answers 501, and a stub added back fails this test.
 */
class CommsControllerStubsTest {

    @Test
    void noOperationAnswersNotImplemented() {
        assertThat(ControllerStubs.count(AudienceGroupController.class)).as("AudienceGroup").isEqualTo(0);
        assertThat(ControllerStubs.count(CalendarExportController.class)).as("CalendarExport").isEqualTo(0);
        assertThat(ControllerStubs.count(ClassPageController.class)).as("ClassPage").isEqualTo(0);
        assertThat(ControllerStubs.count(CommentReactionController.class)).as("CommentReaction").isEqualTo(0);
        assertThat(ControllerStubs.count(ContentViewController.class)).as("ContentView").isEqualTo(0);
        assertThat(ControllerStubs.count(ConversationController.class)).as("Conversation").isEqualTo(0);
        assertThat(ControllerStubs.count(EventController.class)).as("Event").isEqualTo(0);
        assertThat(ControllerStubs.count(FeedbackThreadController.class)).as("FeedbackThread").isEqualTo(0);
        assertThat(ControllerStubs.count(InAppNotificationController.class)).as("InAppNotification").isEqualTo(0);
        assertThat(ControllerStubs.count(MagazineController.class)).as("Magazine").isEqualTo(0);
        assertThat(ControllerStubs.count(MailboxMessageController.class)).as("MailboxMessage").isEqualTo(0);
        assertThat(ControllerStubs.count(MessageTemplateController.class)).as("MessageTemplate").isEqualTo(0);
        assertThat(ControllerStubs.count(NoticeController.class)).as("Notice").isEqualTo(0);
        assertThat(ControllerStubs.count(NoticeBackgroundController.class)).as("NoticeBackground").isEqualTo(0);
        assertThat(ControllerStubs.count(NotificationChannelController.class)).as("NotificationChannel").isEqualTo(0);
        assertThat(ControllerStubs.count(NotificationPreferenceSetController.class)).as("NotificationPreferenceSet")
                .isEqualTo(0);
        assertThat(ControllerStubs.count(NotificationTypeController.class)).as("NotificationType").isEqualTo(0);
        assertThat(ControllerStubs.count(OutboundMessageController.class)).as("OutboundMessage").isEqualTo(0);
        assertThat(ControllerStubs.count(PageCategoryController.class)).as("PageCategory").isEqualTo(0);
        assertThat(ControllerStubs.count(PostController.class)).as("Post").isEqualTo(0);
        assertThat(ControllerStubs.count(TaskController.class)).as("Task").isEqualTo(0);
    }
}
