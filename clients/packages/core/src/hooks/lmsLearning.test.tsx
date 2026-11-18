import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import {
  fakeApi,
  fakeLms,
  json,
  lmsFixture,
  pageOf,
  problem,
  schoolAdmin,
  type Handler,
} from '@mysms/fixtures';
import { MySmsProvider } from '../react/context';
import { createRuntime } from '../runtime';
import {
  COURSE_STEP_PERMISSIONS,
  courseSteps,
  percent,
  useAddFromRepository,
  useAnnotationActions,
  useAnnotations,
  useAuthoredCourses,
  useBookmarkActions,
  useBookmarks,
  useCatalogHome,
  useCourseDependencies,
  useCourseOutline,
  useCoursePage,
  useCourseStep,
  useCreateCourse,
  useDeleteCourse,
  useEbook,
  useEbookActions,
  useEbooks,
  useEnrollInCourse,
  useEnrollInLearningProgram,
  useLearningPrograms,
  useLesson,
  useLessonContent,
  useLessonNoteActions,
  useLessonNotes,
  useLmsCategories,
  useLmsSettings,
  useLmsStatistics,
  useMarkLessonComplete,
  useMyRelatedMaterials,
  usePublicLmsSettings,
  useRecordItemView,
  useRepositoryItems,
  useSaveScormProgress,
  useScormLaunch,
  useSettingsSidebarItems,
  useStructureActions,
  useCatalogItemActions,
  useCatalogItems,
  useChapterActions,
  useCourseChapters,
  useCreateCourseVersion,
  useDeleteLesson,
  useStudentDashboard,
  useTeacherDashboard,
  useTrackVideoWatch,
  useUnreadCounts,
  useUpdateLesson,
  useUpdateLmsSettings,
  useWebResourceActions,
  useWebResourceLaunch,
  useWebResources,
  type LmsCourse,
} from './lmsLearning';

const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresInSeconds: 900, tokenType: 'Bearer' };
const f = lmsFixture;
const uuid = (suffix: string) => `01970000-0000-7000-8000-${suffix.padStart(12, '0')}`;

/** The reads fakeLms does not model whose paths carry an action suffix; they must come before the plain reads. */
function extras(): Record<string, Handler> {
  return {
    'GET /v1/lms/me/dashboard': () =>
      json({
        enrollments: [{ id: uuid('1'), courseId: f.courseId, userId: uuid('9'), progressPercent: '50.00' }],
        continueLessonId: f.textLessonId,
        upcomingLiveClasses: [],
        streakDays: 2,
        longestStreakDays: 5,
        activityHeatmap: [],
      }),
    'GET /v1/lms/me/teacher-dashboard': () =>
      json({ cohorts: [], toGrade: 3, unreadPosts: 1, upcomingDue: [], upcomingSessions: [], alerts: [] }),
    'GET /v1/lms/me/unread-counts': () => json({ forumPosts: 1, notifications: 0, mailbox: 0 }),
    'GET /v1/lms/learning-programs': (req) => pageOf(req, [{ id: uuid('2'), title: 'Path', etag: '"1"' }]),
    'POST /v1/lms/learning-programs/{learningProgramId}:enroll': () => json({ id: uuid('3') }),
    'GET /v1/lms/courses/{courseId}:dependencies': () => json({ blockers: {}, deletable: true }),
    'DELETE /v1/lms/courses/{courseId}': () => new Response(null, { status: 204 }),
    'POST /v1/lms/courses/{courseId}:reorderChapters': () => json({ items: [] }),
    'POST /v1/lms/chapters/{chapterId}:move': (_req, { chapterId }) => json({ id: chapterId }),
    'DELETE /v1/lms/courses/{courseId}/chapters/{chapterId}': () => new Response(null, { status: 204 }),
    'DELETE /v1/lms/lessons/{lessonId}': () => new Response(null, { status: 204 }),
    'POST /v1/lms/lessons/{lessonId}:move': () => json({ id: f.videoLessonId }),
    'POST /v1/lms/courses/{courseId}/chapters': async (req) => json({ ...(await req.json()), id: uuid('4') }),
    'POST /v1/lms/lessons': async (req) => json({ ...(await req.json()), id: uuid('5') }, { status: 201 }),
    'GET /v1/lms/chapters/{chapterId}:scormLaunch': () =>
      json({
        launchUrl: 'https://content.example.com/pkg/index.html',
        runtimeState: { status: 'incomplete' },
      }),
    'POST /v1/lms/chapters/{chapterId}:saveScormProgress': () => json({ id: uuid('6'), status: 'completed' }),
    'GET /v1/lms/ebooks/{ebookId}:myRelatedMaterials': () => problem(501, 'NOT_IMPLEMENTED'),
    'GET /v1/lms/ebooks/{ebookId}': (_req, { ebookId }) =>
      json({ id: ebookId, title: 'Handbook', etag: '"1"' }),
    'GET /v1/lms/ebooks': (req) =>
      pageOf(req, [{ id: uuid('7'), title: 'Handbook', isInRepository: true, etag: '"1"' }]),
    'POST /v1/lms/ebooks': async (req) =>
      json({ ...(await req.json()), id: uuid('8'), etag: '"1"' }, { status: 201 }),
    'DELETE /v1/lms/ebooks/{ebookId}': () => new Response(null, { status: 204 }),
    'POST /v1/lms/ebooks/{ebookId}:convert': () => problem(501, 'NOT_IMPLEMENTED'),
    'POST /v1/lms/annotations': async (req) =>
      json({ ...(await req.json()), id: uuid('a'), etag: '"1"' }, { status: 201 }),
    'DELETE /v1/lms/annotations/{annotationId}': () => new Response(null, { status: 204 }),
    'POST /v1/lms/content-items/{contentItemId}:recordView': (_req, { contentItemId }) =>
      json({ id: contentItemId, viewCount: '1' }),
    'GET /v1/lms/content-items/{contentItemId}:launch': () =>
      json({ url: 'https://files.example.com/signed' }),
    'GET /v1/lms/web-resources': (req) =>
      pageOf(req, [{ id: uuid('b'), title: 'Reference site', resourceType: 'url', etag: '"1"' }]),
    'POST /v1/lms/web-resources': async (req) =>
      json({ ...(await req.json()), id: uuid('c'), etag: '"1"' }, { status: 201 }),
    'DELETE /v1/lms/web-resources/{webResourceId}': () => new Response(null, { status: 204 }),
    'GET /v1/lms/assignments': (req) => pageOf(req, []),
    'GET /v1/lms/discussion-prompts': (req) => pageOf(req, []),
    'GET /v1/lms/participation-activities': (req) => pageOf(req, []),
  };
}

async function setup() {
  const api = fakeApi({
    'POST /v1/iam/auth:login': () => json({ tokens, user: schoolAdmin.user }),
    'GET /v1/iam/me': () => json(schoolAdmin),
    ...extras(),
    ...fakeLms(),
    'GET /v1/lms/lessons/{lessonId}': (_req, { lessonId }) =>
      json({ id: lessonId, chapterId: f.chapterId, title: 'Editable', etag: '"1"' }),
  });
  const runtime = createRuntime({
    baseUrl: 'https://school.example.com',
    clientId: 'test/0/jsdom',
    fetch: api.fetch,
  });
  await runtime.session.signIn({ tenantCode: 'demo', identifier: 'ravi', password: 'pw' });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <MySmsProvider runtime={runtime}>{children}</MySmsProvider>
  );
  return { api, wrapper };
}

describe('course steps and figures', () => {
  it('offers the steps each state allows, as the server does', () => {
    expect(courseSteps({ status: 'in_progress' } as LmsCourse)).toEqual(['submitForReview']);
    expect(courseSteps({ status: 'under_review' } as LmsCourse)).toEqual(['approve']);
    expect(courseSteps({ status: 'approved', lifecycleStatus: 'draft' } as LmsCourse)).toEqual(['publish']);
    expect(courseSteps({ lifecycleStatus: 'published', catalogStatus: 'draft' } as LmsCourse)).toEqual([
      'unpublish',
      'deactivate',
      'publishCatalogListing',
    ]);
    expect(courseSteps({ lifecycleStatus: 'deactivated', catalogStatus: 'published' } as LmsCourse)).toEqual([
      'reactivate',
      'hideCatalogListing',
    ]);
    expect(Object.keys(COURSE_STEP_PERMISSIONS)).toHaveLength(8);
  });

  it('rounds and clamps a server percentage for a ring', () => {
    expect(percent('50.00')).toBe(50);
    expect(percent('120.00')).toBe(100);
    expect(percent(undefined)).toBe(0);
    expect(percent('nonsense')).toBe(0);
  });
});

describe('the catalogue and the course page', () => {
  it('reads the home, categories, figures and settings', async () => {
    const { wrapper } = await setup();
    const home = renderHook(() => useCatalogHome({ query: 'alg' }), { wrapper });
    const categories = renderHook(() => useLmsCategories(), { wrapper });
    const statistics = renderHook(() => useLmsStatistics(), { wrapper });
    const settings = renderHook(() => usePublicLmsSettings(), { wrapper });
    await waitFor(() => expect(home.result.current.data?.courses?.[0]?.title).toBe('Algebra basics'));
    await waitFor(() => expect(categories.result.current.data?.items[0]?.name).toBe('Mathematics'));
    await waitFor(() => expect(statistics.result.current.data?.students).toBe(12));
    await waitFor(() => expect(settings.result.current.data?.showCourses).toBe(true));
  });

  it('reads one course page with its outline and enrols', async () => {
    const { api, wrapper } = await setup();
    const page = renderHook(() => useCoursePage(f.courseId), { wrapper });
    const outline = renderHook(() => useCourseOutline(f.courseId), { wrapper });
    await waitFor(() => expect(page.result.current.data?.course.title).toBe('Algebra basics'));
    await waitFor(() => expect(outline.result.current.data?.chapters?.[0]?.lessons).toHaveLength(2));
    const enrol = renderHook(() => useEnrollInCourse(), { wrapper });
    act(() => enrol.result.current.mutate(f.courseId));
    await waitFor(() => expect(enrol.result.current.isSuccess).toBe(true));
    expect(api.calls.some((c) => c.path.endsWith(':enroll'))).toBe(true);
  });
});

describe('learning', () => {
  it('plays a lesson: content, watch, completion and notes', async () => {
    const { wrapper } = await setup();
    const enrol = renderHook(() => useEnrollInCourse(), { wrapper });
    act(() => enrol.result.current.mutate(f.courseId));
    await waitFor(() => expect(enrol.result.current.isSuccess).toBe(true));
    const content = renderHook(() => useLessonContent(f.videoLessonId), { wrapper });
    await waitFor(() => expect(content.result.current.data?.lesson.title).toBe('Solving for x'));
    const watch = renderHook(() => useTrackVideoWatch(), { wrapper });
    act(() =>
      watch.result.current.mutate({ lessonId: f.videoLessonId, videoSource: 'youtube', seconds: 90 }),
    );
    await waitFor(() => expect(watch.result.current.isSuccess).toBe(true));
    const complete = renderHook(() => useMarkLessonComplete(), { wrapper });
    act(() => complete.result.current.mutate(f.videoLessonId));
    await waitFor(() => expect(complete.result.current.isSuccess).toBe(true));
    const notes = renderHook(() => useLessonNotes(f.videoLessonId), { wrapper });
    const actions = renderHook(() => useLessonNoteActions(f.videoLessonId), { wrapper });
    act(() => actions.result.current.create.mutate({ note: 'Check signs.', color: 'yellow' }));
    await waitFor(() => expect(actions.result.current.create.isSuccess).toBe(true));
    await waitFor(() => expect(notes.result.current.data?.items).toHaveLength(1));
  });

  it('shows the dashboard, the badges and the programs, and joins one', async () => {
    const { wrapper } = await setup();
    const dashboard = renderHook(() => useStudentDashboard(), { wrapper });
    const teacher = renderHook(() => useTeacherDashboard(), { wrapper });
    const unread = renderHook(() => useUnreadCounts(), { wrapper });
    const programs = renderHook(() => useLearningPrograms(), { wrapper });
    await waitFor(() => expect(dashboard.result.current.data?.continueLessonId).toBe(f.textLessonId));
    await waitFor(() => expect(teacher.result.current.data?.toGrade).toBe(3));
    await waitFor(() => expect(unread.result.current.data?.forumPosts).toBe(1));
    await waitFor(() => expect(programs.result.current.data?.items).toHaveLength(1));
    const join = renderHook(() => useEnrollInLearningProgram(), { wrapper });
    act(() => join.result.current.mutate(uuid('2')));
    await waitFor(() => expect(join.result.current.isSuccess).toBe(true));
  });

  it('launches a SCORM package and saves its progress', async () => {
    const { wrapper } = await setup();
    const launch = renderHook(() => useScormLaunch(f.chapterId), { wrapper });
    await waitFor(() => expect(launch.result.current.data?.launchUrl).toContain('index.html'));
    const save = renderHook(() => useSaveScormProgress(f.chapterId), { wrapper });
    act(() => save.result.current.mutate({ status: 'complete', runtimeState: { location: '3' } }));
    await waitFor(() => expect(save.result.current.isSuccess).toBe(true));
  });
});

describe('authoring', () => {
  it('lists, creates, steps, checks dependencies and deletes a course', async () => {
    const { api, wrapper } = await setup();
    const authored = renderHook(() => useAuthoredCourses(), { wrapper });
    await waitFor(() => expect(authored.result.current.data?.items).toHaveLength(2));
    const create = renderHook(() => useCreateCourse(), { wrapper });
    act(() => create.result.current.mutate({ title: 'New course' }));
    await waitFor(() => expect(create.result.current.isError).toBe(true)); // the fake has no create
    const step = renderHook(() => useCourseStep(), { wrapper });
    const draft = { id: f.draftCourseId, etag: '"1"' } as LmsCourse;
    act(() => step.result.current.mutate({ course: draft, step: 'submitForReview' }));
    await waitFor(() => expect(step.result.current.isSuccess).toBe(true));
    const dependencies = renderHook(() => useCourseDependencies(f.courseId), { wrapper });
    await waitFor(() => expect(dependencies.result.current.isSuccess).toBe(true));
    const remove = renderHook(() => useDeleteCourse(), { wrapper });
    act(() => remove.result.current.mutate({ id: f.draftCourseId, etag: '"2"' }));
    await waitFor(() => expect(remove.result.current.isSuccess).toBe(true));
    expect(api.calls.some((c) => c.method === 'DELETE' && c.path.includes(f.draftCourseId))).toBe(true);
  });

  it('makes a new version, renames, nests and deletes chapters, and deletes a lesson, each under its etag', async () => {
    const { api, wrapper } = await setup();
    const version = renderHook(() => useCreateCourseVersion(), { wrapper });
    act(() =>
      version.result.current.mutate({ courseId: f.courseId, versionKind: 'minor', authorComments: 'Typos' }),
    );
    await waitFor(() => expect(version.result.current.data?.versionMinor).toBe(1));
    const chapters = renderHook(() => useCourseChapters(f.courseId), { wrapper });
    await waitFor(() => expect(chapters.result.current.data?.items).toHaveLength(1));
    const chapter = chapters.result.current.data?.items[0];
    if (!chapter) throw new Error('no chapter');
    const actions = renderHook(() => useChapterActions(f.courseId), { wrapper });
    act(() => actions.result.current.rename.mutate({ chapter, title: ' Week 1 ' }));
    await waitFor(() => expect(actions.result.current.rename.data?.title).toBe('Week 1'));
    await waitFor(() => expect(chapters.result.current.data?.items[0]?.etag).toBe('"2"'));
    act(() =>
      actions.result.current.move.mutate({ chapterId: f.chapterId, parentChapterId: null, position: 0 }),
    );
    await waitFor(() => expect(actions.result.current.move.isSuccess).toBe(true));
    act(() => actions.result.current.remove.mutate(chapter));
    await waitFor(() => expect(actions.result.current.remove.isSuccess).toBe(true));
    act(() => actions.result.current.remove.mutate({ ...chapter, etag: undefined }));
    await waitFor(() => expect(actions.result.current.remove.isError).toBe(true));
    const lesson = renderHook(() => useDeleteLesson(), { wrapper });
    act(() =>
      lesson.result.current.mutate({
        id: f.videoLessonId,
        chapterId: f.chapterId,
        title: 'x',
        sortOrder: 0,
        etag: '"5"',
      }),
    );
    await waitFor(() => expect(lesson.result.current.isSuccess).toBe(true));
    const sent = (method: string, part: string) =>
      api.calls.find((c) => c.method === method && c.path.includes(part));
    expect(sent('POST', ':createVersion')).toBeDefined();
    expect(sent('PATCH', '/chapters/')?.headers.get('If-Match')).toBe('"1"');
    expect(sent('DELETE', `/lessons/${f.videoLessonId}`)?.headers.get('If-Match')).toBe('"5"');
    expect(api.calls.filter((c) => c.method === 'DELETE' && c.path.includes('/chapters/'))).toHaveLength(1);
  });

  it('lists the catalogue page items, adds an author and removes it under its etag', async () => {
    const { api, wrapper } = await setup();
    const items = renderHook(() => useCatalogItems(f.courseId), { wrapper });
    await waitFor(() => expect(items.result.current.data?.authors).toEqual([]));
    const actions = renderHook(() => useCatalogItemActions(f.courseId), { wrapper });
    act(() => actions.result.current.addAuthor.mutate({ name: 'A. Teacher', sortOrder: 0 }));
    await waitFor(() => expect(actions.result.current.addAuthor.data?.courseId).toBe(f.courseId));
    await waitFor(() => expect(items.result.current.data?.authors).toHaveLength(1));
    const author = items.result.current.data?.authors[0];
    if (!author) throw new Error('no author');
    act(() => actions.result.current.remove.mutate({ kind: 'author', item: author }));
    await waitFor(() => expect(actions.result.current.remove.isSuccess).toBe(true));
    expect(
      api.calls
        .find((c) => c.method === 'DELETE' && c.path.includes('/author-profiles/'))
        ?.headers.get('If-Match'),
    ).toBe('"1"');
    await waitFor(() => expect(items.result.current.data?.authors).toEqual([]));
  });

  it('moves the structure and edits a lesson under its etag', async () => {
    const { wrapper } = await setup();
    const structure = renderHook(() => useStructureActions(f.courseId), { wrapper });
    act(() => structure.result.current.reorderChapters.mutate([f.chapterId]));
    await waitFor(() => expect(structure.result.current.reorderChapters.isSuccess).toBe(true));
    act(() => structure.result.current.addChapter.mutate({ title: 'Week two', sortOrder: 1 }));
    await waitFor(() => expect(structure.result.current.addChapter.isSuccess).toBe(true));
    act(() =>
      structure.result.current.addLesson.mutate({ chapterId: f.chapterId, title: 'More', sortOrder: 2 }),
    );
    await waitFor(() => expect(structure.result.current.addLesson.isSuccess).toBe(true));
    act(() =>
      structure.result.current.moveLesson.mutate({
        lessonId: f.videoLessonId,
        targetChapterId: f.chapterId,
        position: 1,
      }),
    );
    await waitFor(() => expect(structure.result.current.moveLesson.isSuccess).toBe(true));
    const lesson = renderHook(() => useLesson(f.videoLessonId), { wrapper });
    await waitFor(() => expect(lesson.result.current.data?.title).toBe('Editable'));
    const update = renderHook(() => useUpdateLesson(), { wrapper });
    act(() =>
      update.result.current.mutate({
        lesson: { id: f.videoLessonId, chapterId: f.chapterId, title: 'Editable', sortOrder: 0, etag: '"1"' },
        changes: { title: 'Edited' },
      }),
    );
    await waitFor(() => expect(update.result.current.isSuccess).toBe(true));
  });
});

describe('the settings singleton', () => {
  it('reads the defaults and saves under the ETag it read', async () => {
    const { api, wrapper } = await setup();
    const settings = renderHook(() => useLmsSettings(), { wrapper });
    await waitFor(() => expect(settings.result.current.data?.etag).toBe('"0"'));
    const update = renderHook(() => useUpdateLmsSettings(), { wrapper });
    act(() => update.result.current.mutate({ etag: '"0"', changes: { disableSignup: false } }));
    await waitFor(() => expect(update.result.current.isSuccess).toBe(true));
    expect(api.calls.find((c) => c.method === 'PATCH')?.headers.get('If-Match')).toBe('"0"');
    const sidebar = renderHook(() => useSettingsSidebarItems(uuid('d')), { wrapper });
    await waitFor(() => expect(sidebar.result.current.data?.items).toEqual([]));
    const unsaved = renderHook(() => useSettingsSidebarItems(null), { wrapper });
    expect(unsaved.result.current.isPending).toBe(true);
  });
});

describe('e-books, annotations and bookmarks', () => {
  it('reads an e-book, treats unbuilt related materials as absent and keeps annotations', async () => {
    const { api, wrapper } = await setup();
    const ebook = renderHook(() => useEbook(uuid('7')), { wrapper });
    const ebooks = renderHook(() => useEbooks(), { wrapper });
    const materials = renderHook(() => useMyRelatedMaterials(uuid('7')), { wrapper });
    await waitFor(() => expect(ebook.result.current.data?.title).toBe('Handbook'));
    await waitFor(() => expect(ebooks.result.current.data?.items).toHaveLength(1));
    await waitFor(() => expect(materials.result.current.isSuccess).toBe(true));
    expect(materials.result.current.data).toBeNull();
    const annotations = renderHook(() => useAnnotations(uuid('7')), { wrapper });
    await waitFor(() => expect(annotations.result.current.data?.items).toEqual([]));
    const actions = renderHook(() => useAnnotationActions(), { wrapper });
    act(() =>
      actions.result.current.create.mutate({ ebookId: uuid('7'), annotationType: 'note', description: 'Hm' }),
    );
    await waitFor(() => expect(actions.result.current.create.isSuccess).toBe(true));
    // the owner is the caller, taken from the token: the client never names one
    const sent = api.calls.find((c) => c.method === 'POST' && c.path.endsWith('/v1/lms/annotations'));
    expect(JSON.parse(sent?.body ?? '{}')).not.toHaveProperty('userId');
    act(() => actions.result.current.remove.mutate({ id: uuid('a'), etag: '"1"' }));
    await waitFor(() => expect(actions.result.current.remove.isSuccess).toBe(true));
  });

  it('keeps bookmarks and counts an item view with a like', async () => {
    const { api, wrapper } = await setup();
    const bookmarks = renderHook(() => useBookmarks(), { wrapper });
    await waitFor(() => expect(bookmarks.result.current.data?.items).toEqual([]));
    const actions = renderHook(() => useBookmarkActions(), { wrapper });
    act(() => actions.result.current.create.mutate({ title: 'Khan', url: 'https://k.example.org' }));
    await waitFor(() => expect(actions.result.current.create.isSuccess).toBe(true));
    expect(
      JSON.parse(
        api.calls.find((c) => c.method === 'POST' && c.path.endsWith('/v1/lms/bookmarks'))?.body ?? '{}',
      ),
    ).toEqual({
      title: 'Khan',
      url: 'https://k.example.org',
    });
    act(() => actions.result.current.remove.mutate({ id: uuid('90'), etag: '"1"' }));
    await waitFor(() => expect(actions.result.current.remove.isError).toBe(true)); // unknown id is 404
    const view = renderHook(() => useRecordItemView(), { wrapper });
    act(() => view.result.current.mutate({ contentItemId: uuid('e'), liked: true }));
    await waitFor(() => expect(view.result.current.isSuccess).toBe(true));
    expect(JSON.parse(api.calls.find((c) => c.path.endsWith(':recordView'))?.body ?? '{}')).toEqual({
      liked: true,
    });
    const launch = renderHook(() => useWebResourceLaunch(), { wrapper });
    act(() => launch.result.current.mutate(uuid('e')));
    await waitFor(() => expect(launch.result.current.data?.url).toContain('signed'));
  });
});

describe('the repository and resources', () => {
  it('lists each kind of shared item and places a pick', async () => {
    const { api, wrapper } = await setup();
    const quizzes = renderHook(() => useRepositoryItems('quiz'), { wrapper });
    await waitFor(() => expect(quizzes.result.current.data?.[0]?.title).toBe('Fractions check'));
    const ebooks = renderHook(() => useRepositoryItems('ebook'), { wrapper });
    await waitFor(() => expect(ebooks.result.current.data).toHaveLength(1));
    const empty = renderHook(() => useRepositoryItems('assignment'), { wrapper });
    await waitFor(() => expect(empty.result.current.data).toEqual([]));
    const discussions = renderHook(() => useRepositoryItems('discussion'), { wrapper });
    await waitFor(() => expect(discussions.result.current.data).toEqual([]));
    const participations = renderHook(() => useRepositoryItems('participation'), { wrapper });
    await waitFor(() => expect(participations.result.current.data).toEqual([]));
    const web = renderHook(() => useRepositoryItems('web_resource'), { wrapper });
    await waitFor(() => expect(web.result.current.data).toHaveLength(1));
    const place = renderHook(() => useAddFromRepository(), { wrapper });
    act(() =>
      place.result.current.mutate({
        chapterId: f.chapterId,
        itemKind: 'quiz',
        sourceIds: [f.repositoryQuizId],
      }),
    );
    await waitFor(() => expect(place.result.current.isSuccess).toBe(true));
    expect(api.calls.some((c) => c.path.endsWith(':addFromRepository'))).toBe(true);
  });

  it('manages web resources and e-books, conversion included', async () => {
    const { wrapper } = await setup();
    const resources = renderHook(() => useWebResources(), { wrapper });
    await waitFor(() => expect(resources.result.current.data?.items).toHaveLength(1));
    const actions = renderHook(() => useWebResourceActions(), { wrapper });
    act(() =>
      actions.result.current.create.mutate({ title: 'Site', resourceType: 'url', url: 'https://x.example' }),
    );
    await waitFor(() => expect(actions.result.current.create.isSuccess).toBe(true));
    act(() => actions.result.current.remove.mutate({ id: uuid('b'), etag: '"1"' }));
    await waitFor(() => expect(actions.result.current.remove.isSuccess).toBe(true));
    const ebookActions = renderHook(() => useEbookActions(), { wrapper });
    act(() => ebookActions.result.current.create.mutate({ title: 'New handbook' }));
    await waitFor(() => expect(ebookActions.result.current.create.isSuccess).toBe(true));
    act(() => ebookActions.result.current.remove.mutate({ id: uuid('7'), etag: '"1"' }));
    await waitFor(() => expect(ebookActions.result.current.remove.isSuccess).toBe(true));
    act(() => ebookActions.result.current.convert.mutate(uuid('7')));
    await waitFor(() => expect(ebookActions.result.current.convert.isError).toBe(true)); // 501 until it ships
  });
});
