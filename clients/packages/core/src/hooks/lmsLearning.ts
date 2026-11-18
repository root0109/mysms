import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query';
import type { lms } from '@mysms/sdk';
import { AnnotationApi } from '@mysms/sdk/lms/apis/AnnotationApi';
import { BookmarkApi } from '@mysms/sdk/lms/apis/BookmarkApi';
import { CategoryApi } from '@mysms/sdk/lms/apis/CategoryApi';
import { CohortApi } from '@mysms/sdk/lms/apis/CohortApi';
import { ContentItemApi } from '@mysms/sdk/lms/apis/ContentItemApi';
import { DiscussionPromptApi } from '@mysms/sdk/lms/apis/DiscussionPromptApi';
import { EbookApi } from '@mysms/sdk/lms/apis/EbookApi';
import { LmsAssignmentApi } from '@mysms/sdk/lms/apis/LmsAssignmentApi';
import { ParticipationActivityApi } from '@mysms/sdk/lms/apis/ParticipationActivityApi';
import { QuizApi } from '@mysms/sdk/lms/apis/QuizApi';
import { WebResourceApi } from '@mysms/sdk/lms/apis/WebResourceApi';
import { EnrollmentApi } from '@mysms/sdk/lms/apis/EnrollmentApi';
import { LearningProgramApi } from '@mysms/sdk/lms/apis/LearningProgramApi';
import { LessonApi } from '@mysms/sdk/lms/apis/LessonApi';
import { LmsCourseApi } from '@mysms/sdk/lms/apis/LmsCourseApi';
import { SettingsApi } from '@mysms/sdk/lms/apis/SettingsApi';
import { allPages, pageQuery } from '../api/pages';
import { ApiError } from '../api/problem';
import { call } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';

export type LmsCourse = lms.LmsCourse;
export type CatalogHome = lms.CatalogHome;
export type CourseOutline = lms.CourseOutline;
export type CourseOutlineChapter = lms.CourseOutlineChapter;
export type LessonContent = lms.LessonContent;
export type LmsLesson = lms.Lesson;
export type LessonNote = lms.LessonNote;
export type StudentDashboard = lms.StudentDashboard;
export type LearningProgram = lms.LearningProgram;
export type LmsChapter = lms.Chapter;
export type ScormLaunch = lms.ScormLaunch;
export type LmsSettings = lms.Settings;
export type LmsSettingsUpdate = lms.SettingsUpdate;
export type LmsAnnotation = lms.Annotation;
export type LmsBookmark = lms.Bookmark;
export type LmsEbook = lms.Ebook;
export type LmsWebResource = lms.WebResource;
export type TeacherDashboard = lms.TeacherDashboard;
export type UnreadCounts = lms.UnreadCounts;
export type CourseStep =
  | 'submitForReview'
  | 'approve'
  | 'publish'
  | 'unpublish'
  | 'deactivate'
  | 'reactivate'
  | 'publishCatalogListing'
  | 'hideCatalogListing';

/** The permission each course step needs, so a screen offers only the steps the reader may take. */
export const COURSE_STEP_PERMISSIONS = {
  submitForReview: 'lms.course.submit_course_for_review',
  approve: 'lms.course.approve_course',
  publish: 'lms.course.publish_course',
  unpublish: 'lms.course.unpublish_course',
  deactivate: 'lms.course.deactivate_course',
  reactivate: 'lms.course.reactivate_course',
  publishCatalogListing: 'lms.course.publish_catalog_listing',
  hideCatalogListing: 'lms.course.hide_catalog_listing',
} as const;

/** The steps a course's state allows (api/state-machines.md "lms.LmsCourse"); the server checks them again. */
export function courseSteps(
  course: Pick<LmsCourse, 'status' | 'lifecycleStatus' | 'catalogStatus'>,
): CourseStep[] {
  const steps: CourseStep[] = [];
  if (course.status === 'in_progress') steps.push('submitForReview');
  if (course.status === 'under_review') steps.push('approve');
  if (course.lifecycleStatus === 'draft' && course.status === 'approved') steps.push('publish');
  if (course.lifecycleStatus === 'published') steps.push('unpublish', 'deactivate');
  if (course.lifecycleStatus === 'deactivated') steps.push('reactivate');
  if (course.lifecycleStatus === 'published' && course.catalogStatus !== 'published') {
    steps.push('publishCatalogListing');
  }
  if (course.catalogStatus === 'published') steps.push('hideCatalogListing');
  return steps;
}

/** A progress percentage as the server writes it ("50.00") as a whole number for a ring or bar. */
export function percent(value: string | undefined | null): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? Math.max(0, Math.min(100, Math.round(parsed))) : 0;
}

const LMS = 'lms';

async function refreshCourse(qc: QueryClient, courseId?: string): Promise<void> {
  void qc.invalidateQueries({ queryKey: queryKey('records', 'lms.LmsCourse') });
  await Promise.all([
    qc.invalidateQueries({ queryKey: queryKey(LMS, 'courses') }),
    qc.invalidateQueries({ queryKey: queryKey(LMS, 'catalog') }),
    courseId ? qc.invalidateQueries({ queryKey: queryKey(LMS, 'outline', courseId) }) : Promise.resolve(),
    courseId ? qc.invalidateQueries({ queryKey: queryKey(LMS, 'course', courseId) }) : Promise.resolve(),
  ]);
}

// ---------------------------------------------------------------- the public catalogue

/** lms.GetCatalogHome: banners, a page of listed courses (one category, or words), categories, open cohorts. */
export function useCatalogHome(filter: { categoryId?: string; query?: string; page?: number }) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey(LMS, 'catalog', filter),
    queryFn: () =>
      call(
        api.get(LmsCourseApi).getCatalogHome({
          ...(filter.categoryId ? { categoryId: filter.categoryId } : {}),
          ...(filter.query?.trim() ? { query: filter.query.trim() } : {}),
          page: filter.page ?? 1,
          size: 24,
        }),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** lms.GetPublicSettings: what the public catalogue shows (sections, contact, text direction). */
export function usePublicLmsSettings() {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey(LMS, 'public-settings'),
    queryFn: () => call(api.get(SettingsApi).getPublicSettings()),
    staleTime: STALE_TIME.reference,
  });
}

/** lms.GetLmsStatistics: the catalogue's public figures. */
export function useLmsStatistics() {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey(LMS, 'statistics'),
    queryFn: () => call(api.get(EnrollmentApi).getLmsStatistics()),
    staleTime: STALE_TIME.reference,
  });
}

/** lms.ListCategories: the categories the reader may see (active ones for the public). */
export function useLmsCategories() {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey(LMS, 'categories'),
    queryFn: () =>
      allPages((page) => call(api.get(CategoryApi).listCategories({ size: 500, ...pageQuery(page) }))),
    staleTime: STALE_TIME.reference,
  });
}

/** One course with what its public page shows: the course, its outline, reviews, previews and testimonials. */
export function useCoursePage(courseId: string) {
  const { api } = useRuntime();
  const courses = api.get(LmsCourseApi);
  return useQuery({
    queryKey: queryKey(LMS, 'course', courseId),
    queryFn: async () => {
      const [course, reviews, previews, testimonials, authors] = await Promise.all([
        call(courses.getLmsCourse({ courseId })),
        call(courses.listCourseReviews({ courseId, size: 20 })),
        call(courses.listCoursePreviews({ courseId, size: 50 })),
        call(courses.listCourseTestimonials({ courseId, size: 20 })),
        call(courses.listCourseAuthorProfiles({ courseId, size: 20 })),
      ]);
      return {
        course,
        reviews: reviews.data,
        previews: previews.data,
        testimonials: testimonials.data,
        authors: authors.data,
      };
    },
    staleTime: STALE_TIME.list,
  });
}

/** lms.GetCourseSyllabus at the moment of use: the generated syllabus's signed, short-lived address. */
export function useCourseSyllabus() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: (courseId: string) =>
      call(api.get(LmsCourseApi).getCourseSyllabus({ courseId, format: 'html' })),
  });
}

/** lms.GetCourseOutline: chapters and lessons; for an enrolled student, their progress too. */
export function useCourseOutline(courseId: string) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey(LMS, 'outline', courseId),
    queryFn: () => call(api.get(LmsCourseApi).getCourseOutline({ courseId })),
    staleTime: STALE_TIME.list,
  });
}

/** lms.EnrollInCourse: the reader enrols in a free, published course. */
export function useEnrollInCourse() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (courseId: string) =>
      call(api.get(LmsCourseApi).enrollInCourse({ courseId, enrollInCourseRequest: {} })),
    onSettled: async (_data, _error, courseId) => {
      await Promise.all([
        qc.invalidateQueries({ queryKey: queryKey(LMS, 'outline', courseId) }),
        qc.invalidateQueries({ queryKey: queryKey(LMS, 'dashboard') }),
      ]);
    },
  });
}

// ---------------------------------------------------------------- learning

/** lms.GetStudentDashboard: the reader's enrolments, where to continue, live classes and activity. */
export function useStudentDashboard() {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey(LMS, 'dashboard'),
    queryFn: () => call(api.get(EnrollmentApi).getStudentDashboard()),
    staleTime: STALE_TIME.list,
  });
}

/** lms.GetLessonContent: the lesson, its neighbours and the reader's progress on it. */
export function useLessonContent(lessonId: string) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey(LMS, 'lesson-content', lessonId),
    queryFn: () => call(api.get(LessonApi).getLessonContent({ lessonId })),
    staleTime: STALE_TIME.list,
  });
}

/** lms.MarkLessonComplete: refused (409) until the lesson's video is watched and its quizzes done. */
export function useMarkLessonComplete() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (lessonId: string) => call(api.get(LessonApi).markLessonComplete({ lessonId, body: {} })),
    onSettled: async (_data, _error, lessonId) => {
      await Promise.all([
        qc.invalidateQueries({ queryKey: queryKey(LMS, 'lesson-content', lessonId) }),
        qc.invalidateQueries({ queryKey: queryKey(LMS, 'outline') }),
        qc.invalidateQueries({ queryKey: queryKey(LMS, 'dashboard') }),
      ]);
    },
  });
}

/** lms.TrackVideoWatch: reports how long the reader has watched a lesson video. */
export function useTrackVideoWatch() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: ({
      lessonId,
      videoSource,
      seconds,
    }: {
      lessonId: string;
      videoSource: string;
      seconds: number;
    }) =>
      call(
        api.get(LessonApi).trackVideoWatch({
          lessonId,
          trackVideoWatchRequest: { videoSource, watchSeconds: Math.max(0, seconds).toFixed(2) },
        }),
      ),
  });
}

/** lms.ListLessonNotes: the reader's own notes on a lesson. */
export function useLessonNotes(lessonId: string, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: queryKey(LMS, 'lesson-notes', lessonId),
    queryFn: () =>
      allPages((page) =>
        call(api.get(LessonApi).listLessonNotes({ lessonId, size: 500, ...pageQuery(page) })),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** lms.CreateLessonNote / DeleteLessonNote: the reader writes or removes their own note. */
export function useLessonNoteActions(lessonId: string) {
  const { api } = useRuntime();
  const qc = useQueryClient();
  const refresh = () => qc.invalidateQueries({ queryKey: queryKey(LMS, 'lesson-notes', lessonId) });
  const create = useMutation({
    mutationFn: (note: { note: string; color: NonNullable<LessonNote['color']> }) =>
      call(
        api.get(LessonApi).createLessonNote({
          lessonId,
          lessonNote: { color: note.color, note: note.note.trim() },
        }),
      ),
    onSettled: refresh,
  });
  const remove = useMutation({
    mutationFn: (note: Pick<LessonNote, 'id' | 'etag'>) =>
      call(
        api
          .get(LessonApi)
          .deleteLessonNote({ lessonId, lessonNoteId: note.id ?? '', ifMatch: note.etag ?? '' }),
      ),
    onSettled: refresh,
  });
  return { create, remove };
}

/** lms.GetScormLaunch: where the package plays and the reader's saved state. */
export function useScormLaunch(chapterId: string) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey(LMS, 'scorm', chapterId),
    queryFn: () => call(api.get(LmsCourseApi).getScormLaunch({ chapterId })),
    staleTime: STALE_TIME.list,
  });
}

/** lms.SaveScormProgress: the player's runtime state and status. */
export function useSaveScormProgress(chapterId: string) {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (save: {
      status: lms.SaveScormProgressRequestStatusEnum;
      runtimeState: Record<string, unknown>;
    }) => call(api.get(LmsCourseApi).saveScormProgress({ chapterId, saveScormProgressRequest: save })),
    onSettled: () => qc.invalidateQueries({ queryKey: queryKey(LMS, 'outline') }),
  });
}

/** lms.ListLearningPrograms: published programs (all of them for a holder of the read permission). */
export function useLearningPrograms() {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey(LMS, 'learning-programs'),
    queryFn: () =>
      allPages((page) =>
        call(api.get(LearningProgramApi).listLearningPrograms({ size: 500, ...pageQuery(page) })),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** lms.EnrollInLearningProgram: the reader joins a program (409 when already a member). */
export function useEnrollInLearningProgram() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (learningProgramId: string) =>
      call(api.get(LearningProgramApi).enrollInLearningProgram({ learningProgramId, body: {} })),
    onSettled: () => qc.invalidateQueries({ queryKey: queryKey(LMS, 'learning-programs') }),
  });
}

// ---------------------------------------------------------------- authoring

/** lms.ListLmsCourses: the courses the author sees in their scope, newest first. */
export function useAuthoredCourses() {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey(LMS, 'courses', { authoring: true }),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(LmsCourseApi).listLmsCourses({ size: 500, sort: ['createTime,desc'], ...pageQuery(page) }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** lms.CreateLmsCourse: a new draft course. */
export function useCreateCourse() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (course: { title: string; institutionId?: string }) =>
      call(api.get(LmsCourseApi).createLmsCourse({ lmsCourse: course as LmsCourse })),
    onSettled: () => refreshCourse(qc),
  });
}

/** A course step under the etag the screen last read (409 when the course moved on, 412 when it changed). */
export function useCourseStep() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ course, step, reason }: { course: LmsCourse; step: CourseStep; reason?: string }) => {
      const courses = api.get(LmsCourseApi);
      const courseId = course.id ?? '';
      const etag = course.etag ? { etag: course.etag } : {};
      switch (step) {
        case 'submitForReview':
          return call(courses.submitCourseForReview({ courseId, submitCourseForReviewRequest: etag }));
        case 'approve':
          return call(courses.approveCourse({ courseId, approveCourseRequest: etag }));
        case 'publish':
          return call(courses.publishCourse({ courseId, publishCourseRequest: etag }));
        case 'unpublish':
          return call(courses.unpublishCourse({ courseId, unpublishCourseRequest: etag }));
        case 'deactivate':
          return call(
            courses.deactivateCourse({
              courseId,
              deactivateCourseRequest: { ...etag, ...(reason?.trim() ? { reason: reason.trim() } : {}) },
            }),
          );
        case 'reactivate':
          return call(courses.reactivateCourse({ courseId, reactivateCourseRequest: etag }));
        case 'publishCatalogListing':
          return call(courses.publishCatalogListing({ courseId, publishCatalogListingRequest: etag }));
        case 'hideCatalogListing':
          return call(courses.hideCatalogListing({ courseId, hideCatalogListingRequest: etag }));
      }
    },
    onSettled: (_data, _error, { course }) => refreshCourse(qc, course.id),
  });
}

/** lms.CheckCourseDependencies: what keeps a course from being deleted. */
export function useCourseDependencies(courseId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    enabled: courseId !== null,
    queryKey: queryKey(LMS, 'course-dependencies', courseId),
    queryFn: () => call(api.get(LmsCourseApi).checkCourseDependencies({ courseId: courseId ?? '' })),
  });
}

/** lms.DeleteLmsCourse under the course's etag. */
export function useDeleteCourse() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (course: Pick<LmsCourse, 'id' | 'etag'>) =>
      call(api.get(LmsCourseApi).deleteLmsCourse({ courseId: course.id ?? '', ifMatch: course.etag ?? '' })),
    onSettled: () => refreshCourse(qc),
  });
}

/** The structure editor's moves: chapters in a new order, a chapter or lesson to another place. */
export function useStructureActions(courseId: string) {
  const { api } = useRuntime();
  const qc = useQueryClient();
  const refresh = () => qc.invalidateQueries({ queryKey: queryKey(LMS, 'outline', courseId) });
  const reorderChapters = useMutation({
    mutationFn: (chapterIds: string[]) =>
      call(api.get(LmsCourseApi).reorderChapters({ courseId, reorderChaptersRequest: { chapterIds } })),
    onSettled: refresh,
  });
  const moveLesson = useMutation({
    mutationFn: ({
      lessonId,
      targetChapterId,
      position,
    }: {
      lessonId: string;
      targetChapterId: string;
      position: number;
    }) => call(api.get(LessonApi).moveLesson({ lessonId, moveLessonRequest: { targetChapterId, position } })),
    onSettled: refresh,
  });
  const addChapter = useMutation({
    mutationFn: ({ title, sortOrder }: { title: string; sortOrder: number }) =>
      call(
        api
          .get(LmsCourseApi)
          .createChapter({ courseId, chapter: { courseId, title: title.trim(), sortOrder } }),
      ),
    onSettled: refresh,
  });
  const addLesson = useMutation({
    mutationFn: ({ chapterId, title, sortOrder }: { chapterId: string; title: string; sortOrder: number }) =>
      call(api.get(LessonApi).createLesson({ lesson: { chapterId, title: title.trim(), sortOrder } })),
    onSettled: refresh,
  });
  return { reorderChapters, moveLesson, addChapter, addLesson };
}

/** lms.GetLesson for the lesson editor (teacher material included). */
export function useLesson(lessonId: string) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey(LMS, 'lesson', lessonId),
    queryFn: () => call(api.get(LessonApi).getLesson({ lessonId })),
  });
}

/** lms.UpdateLesson under the lesson's etag (409 for a published version, 412 when it changed meanwhile). */
export function useUpdateLesson() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ lesson, changes }: { lesson: LmsLesson; changes: lms.LessonUpdate }) =>
      call(
        api
          .get(LessonApi)
          .updateLesson({ lessonId: lesson.id ?? '', ifMatch: lesson.etag ?? '', lessonUpdate: changes }),
      ),
    onSettled: async (_data, _error, { lesson }) => {
      await Promise.all([
        qc.invalidateQueries({ queryKey: queryKey(LMS, 'lesson', lesson.id) }),
        qc.invalidateQueries({ queryKey: queryKey(LMS, 'lesson-content', lesson.id) }),
        qc.invalidateQueries({ queryKey: queryKey(LMS, 'outline') }),
      ]);
    },
  });
}

/** The etag a write sends; a row read without one cannot be written safely, so the write is not attempted. */
function etagOf(row: { etag?: string }): string {
  if (!row.etag) throw new Error('the row was read without its etag');
  return row.etag;
}

/** lms.CreateCourseVersion: the next major or minor edition of a course, its structure copied. */
export function useCreateCourseVersion() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      courseId,
      versionKind,
      authorComments,
    }: {
      courseId: string;
      versionKind: lms.CreateCourseVersionRequestVersionKindEnum;
      authorComments?: string;
    }) =>
      call(
        api.get(LmsCourseApi).createCourseVersion({
          courseId,
          createCourseVersionRequest: { versionKind, ...(authorComments ? { authorComments } : {}) },
        }),
      ),
    onSettled: () => refreshCourse(qc),
  });
}

/** lms.ListChapters of a course: every chapter with its parent and etag (the outline flattens the tree). */
export function useCourseChapters(courseId: string) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey(LMS, 'chapters', courseId),
    queryFn: () =>
      allPages((page) =>
        call(api.get(LmsCourseApi).listChapters({ courseId, size: 500, ...pageQuery(page) })),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** Chapter rename, nesting and delete under the chapter's etag; the outline and chapter list are read again. */
export function useChapterActions(courseId: string) {
  const { api } = useRuntime();
  const qc = useQueryClient();
  const refresh = () =>
    Promise.all([
      qc.invalidateQueries({ queryKey: queryKey(LMS, 'outline', courseId) }),
      qc.invalidateQueries({ queryKey: queryKey(LMS, 'chapters', courseId) }),
    ]);
  const rename = useMutation({
    mutationFn: ({ chapter, title }: { chapter: LmsChapter; title: string }) =>
      call(
        api.get(LmsCourseApi).updateChapter({
          courseId,
          chapterId: chapter.id ?? '',
          ifMatch: etagOf(chapter),
          chapterUpdate: { title: title.trim() },
        }),
      ),
    onSettled: refresh,
  });
  const move = useMutation({
    mutationFn: ({
      chapterId,
      parentChapterId,
      position,
    }: {
      chapterId: string;
      parentChapterId: string | null;
      position: number;
    }) =>
      call(
        api.get(LmsCourseApi).moveChapter({
          chapterId,
          moveChapterRequest: { position, ...(parentChapterId ? { parentChapterId } : {}) },
        }),
      ),
    onSettled: refresh,
  });
  const remove = useMutation({
    mutationFn: (chapter: LmsChapter) =>
      call(
        api
          .get(LmsCourseApi)
          .deleteChapter({ courseId, chapterId: chapter.id ?? '', ifMatch: etagOf(chapter) }),
      ),
    onSettled: refresh,
  });
  return { rename, move, remove };
}

/** lms.DeleteLesson under the lesson's etag (409 once it is published or has progress). */
export function useDeleteLesson() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (lesson: LmsLesson) =>
      call(api.get(LessonApi).deleteLesson({ lessonId: lesson.id ?? '', ifMatch: etagOf(lesson) })),
    onSettled: () => qc.invalidateQueries({ queryKey: queryKey(LMS, 'outline') }),
  });
}

export type CatalogItemKind = 'preview' | 'author' | 'testimonial' | 'banner';

/** What the catalogue page editor (LMS-50) lists: previews, authors, testimonials and banners of one course. */
export function useCatalogItems(courseId: string) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey(LMS, 'catalog-items', courseId),
    queryFn: async () => {
      const courses = api.get(LmsCourseApi);
      const [previews, authors, testimonials, banners] = await Promise.all([
        allPages((page) => call(courses.listCoursePreviews({ courseId, size: 500, ...pageQuery(page) }))),
        allPages((page) =>
          call(courses.listCourseAuthorProfiles({ courseId, size: 500, ...pageQuery(page) })),
        ),
        allPages((page) => call(courses.listCourseTestimonials({ courseId, size: 500, ...pageQuery(page) }))),
        allPages((page) => call(courses.listCatalogBanners({ courseId, size: 500, ...pageQuery(page) }))),
      ]);
      return {
        previews: previews.items,
        authors: authors.items,
        testimonials: testimonials.items,
        banners: banners.items,
      };
    },
    staleTime: STALE_TIME.list,
  });
}

/** Adding and removing the catalogue page's items; each delete sends the item's etag. */
export function useCatalogItemActions(courseId: string) {
  const { api } = useRuntime();
  const qc = useQueryClient();
  const refresh = () =>
    Promise.all([
      qc.invalidateQueries({ queryKey: queryKey(LMS, 'catalog-items', courseId) }),
      qc.invalidateQueries({ queryKey: queryKey(LMS, 'course', courseId) }),
      qc.invalidateQueries({ queryKey: queryKey(LMS, 'catalog') }),
    ]);
  const courses = () => api.get(LmsCourseApi);
  const addPreview = useMutation({
    mutationFn: (preview: Omit<lms.CoursePreview, 'courseId'>) =>
      call(courses().createCoursePreview({ courseId, coursePreview: { ...preview, courseId } })),
    onSettled: refresh,
  });
  const addAuthor = useMutation({
    mutationFn: (author: Omit<lms.CourseAuthorProfile, 'courseId'>) =>
      call(courses().createCourseAuthorProfile({ courseId, courseAuthorProfile: { ...author, courseId } })),
    onSettled: refresh,
  });
  const addTestimonial = useMutation({
    mutationFn: (testimonial: Omit<lms.CourseTestimonial, 'courseId'>) =>
      call(courses().createCourseTestimonial({ courseId, courseTestimonial: { ...testimonial, courseId } })),
    onSettled: refresh,
  });
  const addBanner = useMutation({
    mutationFn: (banner: Omit<lms.CatalogBanner, 'courseId'>) =>
      call(courses().createCatalogBanner({ courseId, catalogBanner: { ...banner, courseId } })),
    onSettled: refresh,
  });
  const remove = useMutation({
    mutationFn: ({ kind, item }: { kind: CatalogItemKind; item: { id?: string; etag?: string } }) => {
      const id = item.id ?? '';
      const ifMatch = etagOf(item);
      switch (kind) {
        case 'preview':
          return call(courses().deleteCoursePreview({ courseId, coursePreviewId: id, ifMatch }));
        case 'author':
          return call(courses().deleteCourseAuthorProfile({ courseId, courseAuthorProfileId: id, ifMatch }));
        case 'testimonial':
          return call(courses().deleteCourseTestimonial({ courseId, courseTestimonialId: id, ifMatch }));
        case 'banner':
          return call(courses().deleteCatalogBanner({ courseId, catalogBannerId: id, ifMatch }));
      }
    },
    onSettled: refresh,
  });
  return { addPreview, addAuthor, addTestimonial, addBanner, remove };
}

// ---------------------------------------------------------------- the teacher's home and badges

/** lms.GetTeacherDashboard: the teacher's cohorts, grading queue, unread posts, coming items and alerts. */
export function useTeacherDashboard() {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey(LMS, 'teacher-dashboard'),
    queryFn: () => call(api.get(LmsCourseApi).getTeacherDashboard()),
    staleTime: STALE_TIME.list,
  });
}

/** lms.GetUnreadCounts: the reader's unread forum posts for the apps' badges, in all and per cohort. */
export function useUnreadCounts(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: queryKey(LMS, 'unread-counts'),
    queryFn: () => call(api.get(CohortApi).getUnreadCounts()),
    staleTime: STALE_TIME.list,
  });
}

// ---------------------------------------------------------------- the settings singleton (LMS-53)

/** lms.GetSettings: the tenant's LMS settings, or the defaults (ETag "0") while nothing is saved. */
export function useLmsSettings() {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey(LMS, 'settings'),
    queryFn: () => call(api.get(SettingsApi).getSettings()),
    staleTime: STALE_TIME.list,
  });
}

/** lms.UpdateSettings under the ETag the screen last read; the first save creates the row. */
export function useUpdateLmsSettings() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ etag, changes }: { etag: string; changes: lms.SettingsUpdate }) =>
      call(api.get(SettingsApi).updateSettings({ ifMatch: etag, settingsUpdate: changes })),
    onSettled: async () => {
      await Promise.all([
        qc.invalidateQueries({ queryKey: queryKey(LMS, 'settings') }),
        qc.invalidateQueries({ queryKey: queryKey(LMS, 'public-settings') }),
      ]);
    },
  });
}

/** lms.ListSettingsSidebarItems: the S/B menu entries the settings drive. */
export function useSettingsSidebarItems(settingsId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    enabled: settingsId !== null,
    queryKey: queryKey(LMS, 'settings-sidebar', settingsId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(SettingsApi).listSettingsSidebarItems({
            settingsId: settingsId ?? '',
            size: 500,
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

// ---------------------------------------------------------------- e-books, annotations, bookmarks (LMS-13/14)

/** lms.GetEbook: one e-book of the reader's scope. */
export function useEbook(ebookId: string) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey(LMS, 'ebook', ebookId),
    queryFn: () => call(api.get(EbookApi).getEbook({ ebookId })),
    staleTime: STALE_TIME.list,
  });
}

/** lms.ListEbooks: the e-books of the reader's scope, newest first. */
export function useEbooks() {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey(LMS, 'ebooks'),
    queryFn: () =>
      allPages((page) =>
        call(api.get(EbookApi).listEbooks({ size: 500, sort: ['createTime,desc'], ...pageQuery(page) })),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** lms.GetMyRelatedMaterials: the reader's curated materials of an e-book (absent until that ships: null). */
export function useMyRelatedMaterials(ebookId: string) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey(LMS, 'related-materials', ebookId),
    queryFn: async () => {
      try {
        const page = await call(api.get(EbookApi).getMyRelatedMaterials({ ebookId }));
        return page.data ?? [];
      } catch (error) {
        if (error instanceof ApiError && error.status === 501) return null;
        throw error;
      }
    },
    staleTime: STALE_TIME.list,
  });
}

/** lms.ListAnnotations of one e-book, or every one of the reader for the notes screen. */
export function useAnnotations(ebookId?: string) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey(LMS, 'annotations', ebookId ?? 'all'),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(AnnotationApi).listAnnotations({
            size: 500,
            sort: ['createTime,desc'],
            ...(ebookId ? { filter: `ebookId = "${ebookId}"` } : {}),
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** Annotation create and delete for the reader's highlights and notes. */
export function useAnnotationActions() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  const refresh = () => qc.invalidateQueries({ queryKey: queryKey(LMS, 'annotations') });
  const create = useMutation({
    mutationFn: (annotation: Omit<lms.Annotation, 'userId'>) =>
      call(api.get(AnnotationApi).createAnnotation({ annotation })),
    onSettled: refresh,
  });
  const remove = useMutation({
    mutationFn: (annotation: Pick<lms.Annotation, 'id' | 'etag'>) =>
      call(
        api.get(AnnotationApi).deleteAnnotation({
          annotationId: annotation.id ?? '',
          ifMatch: annotation.etag ?? '',
        }),
      ),
    onSettled: refresh,
  });
  return { create, remove };
}

/** lms.ListBookmarks: the reader's saved links, newest first. */
export function useBookmarks(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: queryKey(LMS, 'bookmarks'),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(BookmarkApi).listBookmarks({ size: 500, sort: ['createTime,desc'], ...pageQuery(page) }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** Bookmark create and delete. */
export function useBookmarkActions() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  const refresh = () => qc.invalidateQueries({ queryKey: queryKey(LMS, 'bookmarks') });
  const create = useMutation({
    mutationFn: (bookmark: { title: string; url: string }) =>
      call(api.get(BookmarkApi).createBookmark({ bookmark })),
    onSettled: refresh,
  });
  const remove = useMutation({
    mutationFn: (bookmark: Pick<lms.Bookmark, 'id' | 'etag'>) =>
      call(
        api.get(BookmarkApi).deleteBookmark({ bookmarkId: bookmark.id ?? '', ifMatch: bookmark.etag ?? '' }),
      ),
    onSettled: refresh,
  });
  return { create, remove };
}

/** lms.RecordItemView: counts the reader's view of an item (and their like, once). */
export function useRecordItemView() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: ({ contentItemId, liked }: { contentItemId: string; liked?: boolean }) =>
      call(
        api.get(ContentItemApi).recordItemView({
          contentItemId,
          recordItemViewRequest: liked === undefined ? {} : { liked },
        }),
      ),
  });
}

/** lms.GetWebResourceLaunch: where a link, file, e-book or web-resource item opens (signed when stored). */
export function useWebResourceLaunch() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: (contentItemId: string) =>
      call(api.get(ContentItemApi).getWebResourceLaunch({ contentItemId })),
  });
}

// ---------------------------------------------------------------- the repository (LMS-47) and resources (LMS-48)

export type RepositoryKind =
  'quiz' | 'assignment' | 'discussion' | 'participation' | 'web_resource' | 'ebook';

/** The shared repository items of one kind: title and id, read from the kind's own list. */
export function useRepositoryItems(kind: RepositoryKind) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey(LMS, 'repository', kind),
    queryFn: async (): Promise<{ id: string; title: string }[]> => {
      const filter = 'isInRepository = true';
      const named = (result: { items: { id?: string; title: string }[] }) =>
        result.items.map((row) => ({ id: row.id ?? '', title: row.title }));
      switch (kind) {
        case 'quiz':
          return named(
            await allPages((page) =>
              call(api.get(QuizApi).listQuizzes({ size: 500, filter, ...pageQuery(page) })),
            ),
          );
        case 'assignment':
          return named(
            await allPages((page) =>
              call(api.get(LmsAssignmentApi).listLmsAssignments({ size: 500, filter, ...pageQuery(page) })),
            ),
          );
        case 'discussion':
          return named(
            await allPages((page) =>
              call(
                api.get(DiscussionPromptApi).listDiscussionPrompts({ size: 500, filter, ...pageQuery(page) }),
              ),
            ),
          );
        case 'participation':
          return named(
            await allPages((page) =>
              call(
                api
                  .get(ParticipationActivityApi)
                  .listParticipationActivities({ size: 500, filter, ...pageQuery(page) }),
              ),
            ),
          );
        case 'web_resource':
          return named(
            await allPages((page) =>
              call(api.get(WebResourceApi).listWebResources({ size: 500, filter, ...pageQuery(page) })),
            ),
          );
        case 'ebook':
          return named(
            await allPages((page) =>
              call(api.get(EbookApi).listEbooks({ size: 500, filter, ...pageQuery(page) })),
            ),
          );
      }
    },
    staleTime: STALE_TIME.list,
  });
}

/** lms.AddFromRepository: places shared repository items in a chapter. */
export function useAddFromRepository() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      chapterId,
      itemKind,
      sourceIds,
    }: {
      chapterId: string;
      itemKind: RepositoryKind;
      sourceIds: string[];
    }) =>
      call(
        api
          .get(ContentItemApi)
          .addFromRepository({ addFromRepositoryRequest: { chapterId, itemKind, sourceIds } }),
      ),
    onSettled: () => qc.invalidateQueries({ queryKey: queryKey(LMS, 'outline') }),
  });
}

/** lms.ListWebResources: the web resources of the reader's scope. */
export function useWebResources() {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey(LMS, 'web-resources'),
    queryFn: () =>
      allPages((page) =>
        call(
          api
            .get(WebResourceApi)
            .listWebResources({ size: 500, sort: ['createTime,desc'], ...pageQuery(page) }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** Web resource create and delete for the admin screen. */
export function useWebResourceActions() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  const refresh = () => qc.invalidateQueries({ queryKey: queryKey(LMS, 'web-resources') });
  const create = useMutation({
    mutationFn: (resource: lms.WebResource) =>
      call(api.get(WebResourceApi).createWebResource({ webResource: resource })),
    onSettled: refresh,
  });
  const remove = useMutation({
    mutationFn: (resource: Pick<lms.WebResource, 'id' | 'etag'>) =>
      call(
        api.get(WebResourceApi).deleteWebResource({
          webResourceId: resource.id ?? '',
          ifMatch: resource.etag ?? '',
        }),
      ),
    onSettled: refresh,
  });
  return { create, remove };
}

/** E-book create and delete, and lms.ConvertEbook (501 until the conversion provider ships). */
export function useEbookActions() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  const refresh = () => qc.invalidateQueries({ queryKey: queryKey(LMS, 'ebooks') });
  const create = useMutation({
    mutationFn: (ebook: lms.Ebook) => call(api.get(EbookApi).createEbook({ ebook })),
    onSettled: refresh,
  });
  const remove = useMutation({
    mutationFn: (ebook: Pick<lms.Ebook, 'id' | 'etag'>) =>
      call(api.get(EbookApi).deleteEbook({ ebookId: ebook.id ?? '', ifMatch: ebook.etag ?? '' })),
    onSettled: refresh,
  });
  const convert = useMutation({
    mutationFn: (ebookId: string) => call(api.get(EbookApi).convertEbook({ ebookId, body: {} })),
    onSettled: refresh,
  });
  return { create, remove, convert };
}
