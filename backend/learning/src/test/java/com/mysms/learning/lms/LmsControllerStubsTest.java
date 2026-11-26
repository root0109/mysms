package com.mysms.learning.lms;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ControllerStubs;
import com.mysms.learning.lms.controller.AccessLogController;
import com.mysms.learning.lms.controller.AnnotationController;
import com.mysms.learning.lms.controller.AssignmentGroupMemberController;
import com.mysms.learning.lms.controller.BadgeController;
import com.mysms.learning.lms.controller.BadgeAwardController;
import com.mysms.learning.lms.controller.BookmarkController;
import com.mysms.learning.lms.controller.CategoryController;
import com.mysms.learning.lms.controller.CertificateController;
import com.mysms.learning.lms.controller.CertificateEvaluationController;
import com.mysms.learning.lms.controller.CertificateRequestController;
import com.mysms.learning.lms.controller.CohortController;
import com.mysms.learning.lms.controller.CohortEnrollmentController;
import com.mysms.learning.lms.controller.ConferencingAccountController;
import com.mysms.learning.lms.controller.ContentAuthorController;
import com.mysms.learning.lms.controller.ContentItemController;
import com.mysms.learning.lms.controller.ContentViewSessionController;
import com.mysms.learning.lms.controller.DiscussionPromptController;
import com.mysms.learning.lms.controller.DriveItemController;
import com.mysms.learning.lms.controller.EbookController;
import com.mysms.learning.lms.controller.EnrollmentController;
import com.mysms.learning.lms.controller.EvaluatorController;
import com.mysms.learning.lms.controller.ExerciseSubmissionController;
import com.mysms.learning.lms.controller.ExternalToolController;
import com.mysms.learning.lms.controller.ExternalToolResourceController;
import com.mysms.learning.lms.controller.ForumPostReportController;
import com.mysms.learning.lms.controller.ForumPostVoteController;
import com.mysms.learning.lms.controller.ForumReportCategoryController;
import com.mysms.learning.lms.controller.ForumSectionController;
import com.mysms.learning.lms.controller.ForumTopicController;
import com.mysms.learning.lms.controller.GradeChangeLogController;
import com.mysms.learning.lms.controller.GradebookEntryController;
import com.mysms.learning.lms.controller.GradingCategoryController;
import com.mysms.learning.lms.controller.ImportExportJobController;
import com.mysms.learning.lms.controller.LearningObjectiveController;
import com.mysms.learning.lms.controller.LearningProgramController;
import com.mysms.learning.lms.controller.LessonController;
import com.mysms.learning.lms.controller.LessonProgressController;
import com.mysms.learning.lms.controller.LiveClassController;
import com.mysms.learning.lms.controller.LmsAssignmentController;
import com.mysms.learning.lms.controller.LmsAssignmentSubmissionController;
import com.mysms.learning.lms.controller.LmsCourseController;
import com.mysms.learning.lms.controller.MentorshipController;
import com.mysms.learning.lms.controller.ParticipationActivityController;
import com.mysms.learning.lms.controller.PeerReviewController;
import com.mysms.learning.lms.controller.PlagiarismAccountController;
import com.mysms.learning.lms.controller.PlagiarismReportController;
import com.mysms.learning.lms.controller.ProgrammingExerciseController;
import com.mysms.learning.lms.controller.QuestionController;
import com.mysms.learning.lms.controller.QuestionBlankAnswerController;
import com.mysms.learning.lms.controller.QuestionHotspotRegionController;
import com.mysms.learning.lms.controller.QuestionPoolController;
import com.mysms.learning.lms.controller.QuizController;
import com.mysms.learning.lms.controller.QuizSubmissionController;
import com.mysms.learning.lms.controller.ReassignmentLogController;
import com.mysms.learning.lms.controller.ReleaseRuleController;
import com.mysms.learning.lms.controller.ResourceCategoryController;
import com.mysms.learning.lms.controller.RubricController;
import com.mysms.learning.lms.controller.RubricAssessmentController;
import com.mysms.learning.lms.controller.SettingsController;
import com.mysms.learning.lms.controller.StudentAlertController;
import com.mysms.learning.lms.controller.TimetableEntryController;
import com.mysms.learning.lms.controller.TimetableLegendController;
import com.mysms.learning.lms.controller.TimetableTemplateController;
import com.mysms.learning.lms.controller.WebResourceController;
import org.junit.jupiter.api.Test;

/**
 * Every lms contract operation is served: no controller answers 501 NOT_IMPLEMENTED, and a stub put back changes a
 * count on purpose.
 */
class LmsControllerStubsTest {

    @Test
    void everyOperationIsServed() {
        assertThat(ControllerStubs.count(AccessLogController.class)).as("AccessLog").isEqualTo(0);
        assertThat(ControllerStubs.count(AnnotationController.class)).as("Annotation").isEqualTo(0);
        assertThat(ControllerStubs.count(AssignmentGroupMemberController.class)).as("AssignmentGroupMember")
                .isEqualTo(0);
        assertThat(ControllerStubs.count(BadgeController.class)).as("Badge").isEqualTo(0);
        assertThat(ControllerStubs.count(BadgeAwardController.class)).as("BadgeAward").isEqualTo(0);
        assertThat(ControllerStubs.count(BookmarkController.class)).as("Bookmark").isEqualTo(0);
        assertThat(ControllerStubs.count(CategoryController.class)).as("Category").isEqualTo(0);
        assertThat(ControllerStubs.count(CertificateController.class)).as("Certificate").isEqualTo(0);
        assertThat(ControllerStubs.count(CertificateEvaluationController.class)).as("CertificateEvaluation")
                .isEqualTo(0);
        assertThat(ControllerStubs.count(CertificateRequestController.class)).as("CertificateRequest").isEqualTo(0);
        assertThat(ControllerStubs.count(CohortController.class)).as("Cohort").isEqualTo(0);
        assertThat(ControllerStubs.count(CohortEnrollmentController.class)).as("CohortEnrollment").isEqualTo(0);
        assertThat(ControllerStubs.count(ConferencingAccountController.class)).as("ConferencingAccount").isEqualTo(0);
        assertThat(ControllerStubs.count(ContentAuthorController.class)).as("ContentAuthor").isEqualTo(0);
        assertThat(ControllerStubs.count(ContentItemController.class)).as("ContentItem").isEqualTo(0);
        assertThat(ControllerStubs.count(ContentViewSessionController.class)).as("ContentViewSession").isEqualTo(0);
        assertThat(ControllerStubs.count(DiscussionPromptController.class)).as("DiscussionPrompt").isEqualTo(0);
        assertThat(ControllerStubs.count(DriveItemController.class)).as("DriveItem").isEqualTo(0);
        assertThat(ControllerStubs.count(EbookController.class)).as("Ebook").isEqualTo(0);
        assertThat(ControllerStubs.count(EnrollmentController.class)).as("Enrollment").isEqualTo(0);
        assertThat(ControllerStubs.count(EvaluatorController.class)).as("Evaluator").isEqualTo(0);
        assertThat(ControllerStubs.count(ExerciseSubmissionController.class)).as("ExerciseSubmission").isEqualTo(0);
        assertThat(ControllerStubs.count(ExternalToolController.class)).as("ExternalTool").isEqualTo(0);
        assertThat(ControllerStubs.count(ExternalToolResourceController.class)).as("ExternalToolResource").isEqualTo(0);
        assertThat(ControllerStubs.count(ForumPostReportController.class)).as("ForumPostReport").isEqualTo(0);
        assertThat(ControllerStubs.count(ForumPostVoteController.class)).as("ForumPostVote").isEqualTo(0);
        assertThat(ControllerStubs.count(ForumReportCategoryController.class)).as("ForumReportCategory").isEqualTo(0);
        assertThat(ControllerStubs.count(ForumSectionController.class)).as("ForumSection").isEqualTo(0);
        assertThat(ControllerStubs.count(ForumTopicController.class)).as("ForumTopic").isEqualTo(0);
        assertThat(ControllerStubs.count(GradeChangeLogController.class)).as("GradeChangeLog").isEqualTo(0);
        assertThat(ControllerStubs.count(GradebookEntryController.class)).as("GradebookEntry").isEqualTo(0);
        assertThat(ControllerStubs.count(GradingCategoryController.class)).as("GradingCategory").isEqualTo(0);
        assertThat(ControllerStubs.count(ImportExportJobController.class)).as("ImportExportJob").isEqualTo(0);
        assertThat(ControllerStubs.count(LearningObjectiveController.class)).as("LearningObjective").isEqualTo(0);
        assertThat(ControllerStubs.count(LearningProgramController.class)).as("LearningProgram").isEqualTo(0);
        assertThat(ControllerStubs.count(LessonController.class)).as("Lesson").isEqualTo(0);
        assertThat(ControllerStubs.count(LessonProgressController.class)).as("LessonProgress").isEqualTo(0);
        assertThat(ControllerStubs.count(LiveClassController.class)).as("LiveClass").isEqualTo(0);
        assertThat(ControllerStubs.count(LmsAssignmentController.class)).as("LmsAssignment").isEqualTo(0);
        assertThat(ControllerStubs.count(LmsAssignmentSubmissionController.class)).as("LmsAssignmentSubmission")
                .isEqualTo(0);
        assertThat(ControllerStubs.count(LmsCourseController.class)).as("LmsCourse").isEqualTo(0);
        assertThat(ControllerStubs.count(MentorshipController.class)).as("Mentorship").isEqualTo(0);
        assertThat(ControllerStubs.count(ParticipationActivityController.class)).as("ParticipationActivity")
                .isEqualTo(0);
        assertThat(ControllerStubs.count(PeerReviewController.class)).as("PeerReview").isEqualTo(0);
        assertThat(ControllerStubs.count(PlagiarismAccountController.class)).as("PlagiarismAccount").isEqualTo(0);
        assertThat(ControllerStubs.count(PlagiarismReportController.class)).as("PlagiarismReport").isEqualTo(0);
        assertThat(ControllerStubs.count(ProgrammingExerciseController.class)).as("ProgrammingExercise").isEqualTo(0);
        assertThat(ControllerStubs.count(QuestionController.class)).as("Question").isEqualTo(0);
        assertThat(ControllerStubs.count(QuestionBlankAnswerController.class)).as("QuestionBlankAnswer").isEqualTo(0);
        assertThat(ControllerStubs.count(QuestionHotspotRegionController.class)).as("QuestionHotspotRegion")
                .isEqualTo(0);
        assertThat(ControllerStubs.count(QuestionPoolController.class)).as("QuestionPool").isEqualTo(0);
        assertThat(ControllerStubs.count(QuizController.class)).as("Quiz").isEqualTo(0);
        assertThat(ControllerStubs.count(QuizSubmissionController.class)).as("QuizSubmission").isEqualTo(0);
        assertThat(ControllerStubs.count(ReassignmentLogController.class)).as("ReassignmentLog").isEqualTo(0);
        assertThat(ControllerStubs.count(ReleaseRuleController.class)).as("ReleaseRule").isEqualTo(0);
        assertThat(ControllerStubs.count(ResourceCategoryController.class)).as("ResourceCategory").isEqualTo(0);
        assertThat(ControllerStubs.count(RubricController.class)).as("Rubric").isEqualTo(0);
        assertThat(ControllerStubs.count(RubricAssessmentController.class)).as("RubricAssessment").isEqualTo(0);
        assertThat(ControllerStubs.count(SettingsController.class)).as("Settings").isEqualTo(0);
        assertThat(ControllerStubs.count(StudentAlertController.class)).as("StudentAlert").isEqualTo(0);
        assertThat(ControllerStubs.count(TimetableEntryController.class)).as("TimetableEntry").isEqualTo(0);
        assertThat(ControllerStubs.count(TimetableLegendController.class)).as("TimetableLegend").isEqualTo(0);
        assertThat(ControllerStubs.count(TimetableTemplateController.class)).as("TimetableTemplate").isEqualTo(0);
        assertThat(ControllerStubs.count(WebResourceController.class)).as("WebResource").isEqualTo(0);
    }
}
