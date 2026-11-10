package com.mysms.lms;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ControllerStubs;
import com.mysms.lms.controller.AnnotationController;
import com.mysms.lms.controller.AssignmentGroupMemberController;
import com.mysms.lms.controller.BadgeController;
import com.mysms.lms.controller.BadgeAwardController;
import com.mysms.lms.controller.BookmarkController;
import com.mysms.lms.controller.CategoryController;
import com.mysms.lms.controller.CertificateController;
import com.mysms.lms.controller.CertificateEvaluationController;
import com.mysms.lms.controller.CertificateRequestController;
import com.mysms.lms.controller.CohortController;
import com.mysms.lms.controller.CohortEnrollmentController;
import com.mysms.lms.controller.ConferencingAccountController;
import com.mysms.lms.controller.ContentAuthorController;
import com.mysms.lms.controller.ContentItemController;
import com.mysms.lms.controller.ContentViewSessionController;
import com.mysms.lms.controller.DiscussionPromptController;
import com.mysms.lms.controller.DriveItemController;
import com.mysms.lms.controller.EbookController;
import com.mysms.lms.controller.EnrollmentController;
import com.mysms.lms.controller.EvaluatorController;
import com.mysms.lms.controller.ExerciseSubmissionController;
import com.mysms.lms.controller.ExternalToolController;
import com.mysms.lms.controller.ExternalToolResourceController;
import com.mysms.lms.controller.ForumPostReportController;
import com.mysms.lms.controller.ForumPostVoteController;
import com.mysms.lms.controller.ForumReportCategoryController;
import com.mysms.lms.controller.ForumSectionController;
import com.mysms.lms.controller.ForumTopicController;
import com.mysms.lms.controller.GradeChangeLogController;
import com.mysms.lms.controller.GradebookEntryController;
import com.mysms.lms.controller.GradingCategoryController;
import com.mysms.lms.controller.ImportExportJobController;
import com.mysms.lms.controller.LearningObjectiveController;
import com.mysms.lms.controller.LearningProgramController;
import com.mysms.lms.controller.LessonController;
import com.mysms.lms.controller.LessonProgressController;
import com.mysms.lms.controller.LiveClassController;
import com.mysms.lms.controller.LmsAssignmentController;
import com.mysms.lms.controller.LmsAssignmentSubmissionController;
import com.mysms.lms.controller.LmsCourseController;
import com.mysms.lms.controller.MentorshipController;
import com.mysms.lms.controller.ParticipationActivityController;
import com.mysms.lms.controller.PeerReviewController;
import com.mysms.lms.controller.PlagiarismAccountController;
import com.mysms.lms.controller.PlagiarismReportController;
import com.mysms.lms.controller.ProgrammingExerciseController;
import com.mysms.lms.controller.QuestionController;
import com.mysms.lms.controller.QuestionBlankAnswerController;
import com.mysms.lms.controller.QuestionHotspotRegionController;
import com.mysms.lms.controller.QuestionPoolController;
import com.mysms.lms.controller.QuizController;
import com.mysms.lms.controller.QuizSubmissionController;
import com.mysms.lms.controller.ReassignmentLogController;
import com.mysms.lms.controller.ReleaseRuleController;
import com.mysms.lms.controller.ResourceCategoryController;
import com.mysms.lms.controller.RubricController;
import com.mysms.lms.controller.RubricAssessmentController;
import com.mysms.lms.controller.SettingsController;
import com.mysms.lms.controller.StudentAlertController;
import com.mysms.lms.controller.TimetableEntryController;
import com.mysms.lms.controller.TimetableLegendController;
import com.mysms.lms.controller.TimetableTemplateController;
import com.mysms.lms.controller.WebResourceController;
import org.junit.jupiter.api.Test;

/**
 * The contract operations lms does not serve yet answer 501 NOT_IMPLEMENTED explicitly — none falls through, and a
 * newly served operation changes the count on purpose.
 */
class LmsControllerStubsTest {

    @Test
    void unservedOperationsAnswerNotImplemented() {
        assertThat(ControllerStubs.count(AnnotationController.class)).as("Annotation").isEqualTo(0);
        assertThat(ControllerStubs.count(AssignmentGroupMemberController.class)).as("AssignmentGroupMember")
                .isEqualTo(0);
        assertThat(ControllerStubs.count(BadgeController.class)).as("Badge").isEqualTo(1);
        assertThat(ControllerStubs.count(BadgeAwardController.class)).as("BadgeAward").isEqualTo(0);
        assertThat(ControllerStubs.count(BookmarkController.class)).as("Bookmark").isEqualTo(0);
        assertThat(ControllerStubs.count(CategoryController.class)).as("Category").isEqualTo(2);
        assertThat(ControllerStubs.count(CertificateController.class)).as("Certificate").isEqualTo(2);
        assertThat(ControllerStubs.count(CertificateEvaluationController.class)).as("CertificateEvaluation")
                .isEqualTo(1);
        assertThat(ControllerStubs.count(CertificateRequestController.class)).as("CertificateRequest").isEqualTo(2);
        assertThat(ControllerStubs.count(CohortController.class)).as("Cohort").isEqualTo(32);
        assertThat(ControllerStubs.count(CohortEnrollmentController.class)).as("CohortEnrollment").isEqualTo(0);
        assertThat(ControllerStubs.count(ConferencingAccountController.class)).as("ConferencingAccount").isEqualTo(1);
        assertThat(ControllerStubs.count(ContentAuthorController.class)).as("ContentAuthor").isEqualTo(0);
        assertThat(ControllerStubs.count(ContentItemController.class)).as("ContentItem").isEqualTo(17);
        assertThat(ControllerStubs.count(ContentViewSessionController.class)).as("ContentViewSession").isEqualTo(0);
        assertThat(ControllerStubs.count(DiscussionPromptController.class)).as("DiscussionPrompt").isEqualTo(0);
        assertThat(ControllerStubs.count(DriveItemController.class)).as("DriveItem").isEqualTo(1);
        assertThat(ControllerStubs.count(EbookController.class)).as("Ebook").isEqualTo(6);
        assertThat(ControllerStubs.count(EnrollmentController.class)).as("Enrollment").isEqualTo(3);
        assertThat(ControllerStubs.count(EvaluatorController.class)).as("Evaluator").isEqualTo(2);
        assertThat(ControllerStubs.count(ExerciseSubmissionController.class)).as("ExerciseSubmission").isEqualTo(0);
        assertThat(ControllerStubs.count(ExternalToolController.class)).as("ExternalTool").isEqualTo(1);
        assertThat(ControllerStubs.count(ExternalToolResourceController.class)).as("ExternalToolResource").isEqualTo(2);
        assertThat(ControllerStubs.count(ForumPostReportController.class)).as("ForumPostReport").isEqualTo(0);
        assertThat(ControllerStubs.count(ForumPostVoteController.class)).as("ForumPostVote").isEqualTo(0);
        assertThat(ControllerStubs.count(ForumReportCategoryController.class)).as("ForumReportCategory").isEqualTo(0);
        assertThat(ControllerStubs.count(ForumSectionController.class)).as("ForumSection").isEqualTo(0);
        assertThat(ControllerStubs.count(ForumTopicController.class)).as("ForumTopic").isEqualTo(6);
        assertThat(ControllerStubs.count(GradeChangeLogController.class)).as("GradeChangeLog").isEqualTo(0);
        assertThat(ControllerStubs.count(GradebookEntryController.class)).as("GradebookEntry").isEqualTo(2);
        assertThat(ControllerStubs.count(GradingCategoryController.class)).as("GradingCategory").isEqualTo(0);
        assertThat(ControllerStubs.count(ImportExportJobController.class)).as("ImportExportJob").isEqualTo(2);
        assertThat(ControllerStubs.count(LearningObjectiveController.class)).as("LearningObjective").isEqualTo(0);
        assertThat(ControllerStubs.count(LearningProgramController.class)).as("LearningProgram").isEqualTo(3);
        assertThat(ControllerStubs.count(LessonController.class)).as("Lesson").isEqualTo(14);
        assertThat(ControllerStubs.count(LessonProgressController.class)).as("LessonProgress").isEqualTo(0);
        assertThat(ControllerStubs.count(LiveClassController.class)).as("LiveClass").isEqualTo(6);
        assertThat(ControllerStubs.count(LmsAssignmentController.class)).as("LmsAssignment").isEqualTo(1);
        assertThat(ControllerStubs.count(LmsAssignmentSubmissionController.class)).as("LmsAssignmentSubmission")
                .isEqualTo(3);
        assertThat(ControllerStubs.count(LmsCourseController.class)).as("LmsCourse").isEqualTo(35);
        assertThat(ControllerStubs.count(MentorshipController.class)).as("Mentorship").isEqualTo(0);
        assertThat(ControllerStubs.count(ParticipationActivityController.class)).as("ParticipationActivity")
                .isEqualTo(0);
        assertThat(ControllerStubs.count(PeerReviewController.class)).as("PeerReview").isEqualTo(0);
        assertThat(ControllerStubs.count(PlagiarismAccountController.class)).as("PlagiarismAccount").isEqualTo(0);
        assertThat(ControllerStubs.count(PlagiarismReportController.class)).as("PlagiarismReport").isEqualTo(3);
        assertThat(ControllerStubs.count(ProgrammingExerciseController.class)).as("ProgrammingExercise").isEqualTo(1);
        assertThat(ControllerStubs.count(QuestionController.class)).as("Question").isEqualTo(2);
        assertThat(ControllerStubs.count(QuestionBlankAnswerController.class)).as("QuestionBlankAnswer").isEqualTo(0);
        assertThat(ControllerStubs.count(QuestionHotspotRegionController.class)).as("QuestionHotspotRegion")
                .isEqualTo(0);
        assertThat(ControllerStubs.count(QuestionPoolController.class)).as("QuestionPool").isEqualTo(0);
        assertThat(ControllerStubs.count(QuizController.class)).as("Quiz").isEqualTo(4);
        assertThat(ControllerStubs.count(QuizSubmissionController.class)).as("QuizSubmission").isEqualTo(4);
        assertThat(ControllerStubs.count(ReassignmentLogController.class)).as("ReassignmentLog").isEqualTo(0);
        assertThat(ControllerStubs.count(ReleaseRuleController.class)).as("ReleaseRule").isEqualTo(3);
        assertThat(ControllerStubs.count(ResourceCategoryController.class)).as("ResourceCategory").isEqualTo(0);
        assertThat(ControllerStubs.count(RubricController.class)).as("Rubric").isEqualTo(0);
        assertThat(ControllerStubs.count(RubricAssessmentController.class)).as("RubricAssessment").isEqualTo(0);
        assertThat(ControllerStubs.count(SettingsController.class)).as("Settings").isEqualTo(4);
        assertThat(ControllerStubs.count(StudentAlertController.class)).as("StudentAlert").isEqualTo(0);
        assertThat(ControllerStubs.count(TimetableEntryController.class)).as("TimetableEntry").isEqualTo(0);
        assertThat(ControllerStubs.count(TimetableLegendController.class)).as("TimetableLegend").isEqualTo(0);
        assertThat(ControllerStubs.count(TimetableTemplateController.class)).as("TimetableTemplate").isEqualTo(0);
        assertThat(ControllerStubs.count(WebResourceController.class)).as("WebResource").isEqualTo(0);
    }
}
