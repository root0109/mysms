import type { lms } from '@mysms/sdk';
import { schoolAdmin } from './data';
import { json, problem, type Handler } from './fakeApi';
import { onePage, pageOf } from './paging';

/** Ids of the cohort area (cohorts, forums, live classes, certificates). */
const cid = (suffix: string) => `01970000-0000-7000-8000-${suffix.padStart(12, '0')}`;
/** Ids of the coursework area (quizzes, assignments, grading, the drive). */
const id = (suffix: string) => `0196a000-0000-7000-8000-${suffix.padStart(12, '0')}`;
/** Ids of the learning area (the catalogue, courses, lessons, the repository). */
const lid = (suffix: string) => `01960000-0000-7000-8000-${suffix.padStart(12, '0')}`;

/**
 * The LMS the screen tests use: one cohort with a teacher, two students, an upcoming live class, a forum topic and a
 * certificate; in it one quiz placement with two questions, one open assignment, a two-student gradebook and a drive
 * item; and in the catalogue one published course with one chapter of two lessons (the first has a video and is a
 * preview) beside a draft.
 */
export const lmsFixture = {
  cohortId: cid('c1'),
  teacherUserId: cid('11'),
  studentUserIds: [cid('21'), cid('22')],
  liveClassId: cid('d1'),
  topicId: cid('e1'),
  certificateId: cid('f1'),
  quizItemId: id('11'),
  quizId: id('12'),
  quizSubmissionId: id('13'),
  attemptToken: 'token-1',
  choiceQuestionId: id('21'),
  openQuestionId: id('22'),
  optionRight: id('31'),
  optionWrong: id('32'),
  assignmentItemId: id('41'),
  assignmentId: id('42'),
  submissionId: id('43'),
  studentId: id('51'),
  otherStudentId: id('52'),
  driveItemId: id('61'),
  questionFileId: id('71'),
  courseId: lid('e1'),
  draftCourseId: lid('e2'),
  chapterId: lid('e11'),
  videoLessonId: lid('e21'),
  textLessonId: lid('e22'),
  categoryId: lid('e31'),
  repositoryQuizId: lid('e32'),
} as const;

/** What a student needs to learn (lessons, video, notes) and on the quiz, assignment and drive screens. */
export const LMS_STUDENT = [
  'lms.content_item.read',
  'lms.quiz.start_quiz_attempt',
  'lms.quiz.check_quiz_answer',
  'lms.quiz.submit_quiz',
  'lms.quiz_submission.read',
  'lms.quiz_submission.save_quiz_responses',
  'lms.quiz_submission.pause_quiz_attempt',
  'lms.quiz_submission.resume_quiz_attempt',
  'lms.quiz_answer.read',
  'lms.assignment.read',
  'lms.assignment_submission.read',
  'lms.assignment_submission.create',
  'lms.assignment_submission.update',
  'lms.assignment_submission.delete',
  'lms.assignment_submission.finalize_submission',
  'lms.cohort.get_gradebook',
  'lms.drive_item.read',
  'lms.drive_item.share_drive_item',
  'lms.course.enroll_in_course',
  'lms.lesson.mark_lesson_complete',
  'lms.lesson.track_video_watch',
  'lms.lesson_note.read',
  'lms.lesson_note.create',
  'lms.lesson_note.delete',
  'lms.course.get_course_syllabus',
] as const;

/** What a teacher holds: the cohort, its live classes, forums and certificates, the gradebook, grading and the bank. */
export const LMS_TEACHER = [
  'lms.cohort.read',
  'lms.cohort.update',
  'lms.cohort.get_cohort_roster',
  'lms.cohort.set_cohort_members',
  'lms.cohort.send_cohort_announcement',
  'lms.cohort.publish_cohort',
  'lms.cohort.get_cohort_progress',
  'lms.live_class.read',
  'lms.live_class.join_live_class',
  'lms.live_class.retry_live_class_invites',
  'lms.forum_topic.read',
  'lms.forum_post.read',
  'lms.forum_post.create',
  'lms.forum_topic.mark_topic_read',
  'lms.certificate.read',
  'lms.certificate.download_certificate',
  'lms.badge_award.read',
  'lms.cohort.get_gradebook',
  'lms.cohort.export_grades',
  'lms.cohort.publish_final_grades',
  'lms.content_item.read',
  'lms.content_item.publish_item_grades',
  'lms.gradebook_entry.grade_entry',
  'lms.gradebook_entry.batch_grade_entries',
  'lms.grade_change_log.read',
  'lms.reassignment_log.read',
  'lms.quiz_submission.read',
  'lms.quiz_submission.grade_quiz_answers',
  'lms.quiz_answer.read',
  'lms.assignment_submission.read',
  'lms.assignment_submission.grade_learning_submission',
  'lms.question.search_question_bank',
  'lms.question.import_questions',
  'lms.rubric.read',
] as const;

/** What a course author needs for their courses and the review step. */
export const LMS_AUTHOR = [
  'lms.course.read',
  'lms.course.create',
  'lms.course.submit_course_for_review',
  'lms.course.create_course_version',
  'lms.chapter.update',
  'lms.lesson.read',
  'lms.lesson.update',
  'lms.course_author_profile.create',
  'lms.course_author_profile.delete',
] as const;

/** What the LMS administrator needs: the settings singleton and the repository. */
export const LMS_ADMIN = [
  'lms.settings.read',
  'lms.settings.update',
  'lms.quiz.read',
  'lms.content_item.add_from_repository',
  'lms.annotation.read',
  'lms.bookmark.read',
  'lms.bookmark.create',
  'lms.bookmark.delete',
] as const;

type Mutable<T> = { -readonly [K in keyof T]: T[K] };

export interface FakeLmsOptions {
  /** The grade the gradebook entry stands at; a regrade under another etag is 412. */
  entryEtag?: string;
}

/** The server's LMS as the screen tests see it: every area's slice over its own in-memory state. */
export function fakeLms(options: FakeLmsOptions = {}): Record<string, Handler> {
  return { ...cohortHandlers(), ...courseworkHandlers(options), ...learningHandlers() };
}

/**
 * The cohort slice of the server the LMS screens use, over one in-memory cohort: the read with its ETag, the member
 * lifecycle steps under the body etag (409 off their state, 412 stale), the roster, announcements that count the
 * active students, self-enrolment with its gate codes, the forum thread with read marks, one live class with its
 * join link and invite retries, and the member's certificates with a signed download link.
 */
function cohortHandlers(): Record<string, Handler> {
  const fixture = lmsFixture;
  let version = 1;
  let lifecycle = 'active';
  let published = true;
  let inviteAttempts = 0;
  const posts: lms.ForumPost[] = [];
  const etag = () => `"${version}"`;
  const cohort = (): lms.Cohort =>
    ({
      id: fixture.cohortId,
      title: 'Algebra 2026',
      startDate: '2026-01-10',
      endDate: '2026-06-20',
      timezone: 'Asia/Kolkata',
      lifecycleStatus: lifecycle,
      isPublished: published,
      allowSelfEnrollment: true,
      seatCount: 10,
      etag: etag(),
    }) as lms.Cohort;
  const step = (allowed: boolean, apply: () => void): Handler => {
    return async (req) => {
      const body = (await req.json().catch(() => ({}))) as { etag?: string };
      if (body.etag && body.etag !== etag()) return problem(412, 'PRECONDITION_FAILED', 'Stale etag.');
      if (!allowed) return problem(409, 'INVALID_STATE_TRANSITION', 'Not allowed from this state.');
      apply();
      version += 1;
      return json(cohort(), { headers: { ETag: etag() } });
    };
  };
  return {
    [`GET /v1/lms/cohorts/${fixture.cohortId}`]: () => json(cohort(), { headers: { ETag: etag() } }),
    'GET /v1/lms/cohorts': (req) => onePage(req, [cohort()]),
    [`POST /v1/lms/cohorts/${fixture.cohortId}:publish`]: (req) =>
      step(!published && lifecycle !== 'archived', () => {
        published = true;
      })(req, {}),
    [`POST /v1/lms/cohorts/${fixture.cohortId}:unpublish`]: (req) =>
      step(published, () => {
        published = false;
      })(req, {}),
    [`POST /v1/lms/cohorts/${fixture.cohortId}:deactivate`]: (req) =>
      step(lifecycle === 'active', () => {
        lifecycle = 'suspended';
      })(req, {}),
    [`POST /v1/lms/cohorts/${fixture.cohortId}:sendAnnouncement`]: async (req) => {
      const body = (await req.json()) as { subject?: string; content?: string };
      if (!body.subject?.trim() || !body.content?.trim())
        return problem(422, 'VALIDATION_FAILED', 'subject and content are required.');
      return json({ queued: fixture.studentUserIds.length, skipped: 0 });
    },
    [`POST /v1/lms/cohorts/${fixture.cohortId}:enroll`]: () =>
      published && lifecycle === 'active'
        ? json({
            id: cid('31'),
            cohortId: fixture.cohortId,
            userId: fixture.studentUserIds[0],
            isActive: true,
            isSelfEnrolled: true,
            etag: '"1"',
          })
        : problem(409, 'COHORT_NOT_OPEN', 'The cohort is not open for enrolment.'),
    [`GET /v1/lms/cohorts/${fixture.cohortId}:roster`]: (req) =>
      onePage(req, [
        {
          userId: fixture.teacherUserId,
          displayName: 'Meera Iyer',
          roleCode: 'teacher',
          isActive: true,
        },
        ...fixture.studentUserIds.map((userId, at) => ({
          userId,
          displayName: at === 0 ? 'Asha Rao' : 'Ravi Kumar',
          roleCode: 'student',
          isActive: true,
        })),
      ]),
    [`GET /v1/lms/cohorts/${fixture.cohortId}:progress`]: (req) =>
      onePage(
        req,
        fixture.studentUserIds.map((userId) => ({ userId, assessmentsCompleted: 1, assessmentsTotal: 2 })),
      ),
    [`GET /v1/lms/cohorts/${fixture.cohortId}/forums`]: (req) =>
      onePage(req, [{ id: cid('a1'), cohortId: fixture.cohortId, title: 'General', etag: '"1"' }]),
    'GET /v1/lms/forum-sections': (req) =>
      onePage(req, [{ id: cid('a2'), forumId: cid('a1'), title: 'Week 1', etag: '"1"' }]),
    'GET /v1/lms/forum-topics': (req) =>
      onePage(req, [{ id: fixture.topicId, title: 'Introductions', isHidden: false, etag: '"1"' }]),
    [`GET /v1/lms/forum-topics/${fixture.topicId}/posts`]: (req) => onePage(req, posts),
    [`POST /v1/lms/forum-topics/${fixture.topicId}/posts`]: async (req) => {
      const body = (await req.json()) as lms.ForumPost;
      const post = {
        ...body,
        id: cid(`p${posts.length + 1}`),
        postedAt: '2026-11-16T09:00:00Z',
        likeCount: 0,
        etag: '"1"',
      };
      posts.push(post);
      return json(post, { status: 201 });
    },
    [`POST /v1/lms/forum-topics/${fixture.topicId}:markRead`]: () => new Response(null, { status: 204 }),
    'GET /v1/lms/live-classes': (req) =>
      onePage(req, [
        {
          id: fixture.liveClassId,
          cohortId: fixture.cohortId,
          title: 'Live 1',
          classDate: '2026-11-20',
          startTime: '10:00',
          durationMinutes: 45,
          timezone: 'Asia/Kolkata',
          inviteStatus: inviteAttempts > 0 ? 'retrying' : 'sent',
          inviteAttempts,
          etag: `"${1 + inviteAttempts}"`,
        },
      ]),
    [`POST /v1/lms/live-classes/${fixture.liveClassId}:join`]: () => json({ url: 'https://meet.example/j' }),
    [`POST /v1/lms/live-classes/${fixture.liveClassId}:retryInvites`]: () => {
      inviteAttempts += 1;
      return json({
        id: fixture.liveClassId,
        title: 'Live 1',
        inviteStatus: 'retrying',
        inviteAttempts,
        etag: `"${1 + inviteAttempts}"`,
      });
    },
    'GET /v1/lms/certificates': (req) =>
      onePage(req, [
        {
          id: fixture.certificateId,
          userId: fixture.studentUserIds[0],
          cohortId: fixture.cohortId,
          certificateNumber: 'CRT-1',
          issueDate: '2026-06-21',
          templateCode: 'default',
          isPublished: true,
          etag: '"1"',
        },
      ]),
    [`GET /v1/lms/certificates/${fixture.certificateId}:download`]: () =>
      json({ url: 'https://files.example/crt.pdf', expiresAt: '2026-11-16T10:00:00Z' }),
    'GET /v1/lms/badge-awards': (req) => onePage(req, []),
  };
}

/**
 * The coursework slice, with the server's rules for it: an attempt started once with its token
 * (a save or submission with another token is 409), hand-worked marks on submission (the right option earns 6 of
 * 10), grading under the entry's etag (412 when stale), the bank searched by text, and a drive item's share list
 * replaced as a whole.
 */
function courseworkHandlers(options: FakeLmsOptions): Record<string, Handler> {
  const fx = lmsFixture;
  let entryEtag = options.entryEtag ?? '"1"';
  let entryPoints = '12.00';
  let shares: string[] = [];
  let submissionFinal = false;
  const attempt = {
    quiz: {
      id: fx.quizId,
      title: 'Algebra check',
      passingPercentage: 50,
      totalMarks: '10',
      allowPause: true,
      showFeedback: true,
      showAnswers: true,
    },
    questions: [
      {
        quizQuestionId: id('81'),
        questionId: fx.choiceQuestionId,
        questionText: 'Pick the prime number.',
        questionType: 'choices',
        allowsMultiple: false,
        marks: '6',
        options: [
          { optionId: fx.optionRight, optionText: '7' },
          { optionId: fx.optionWrong, optionText: '8' },
        ],
      },
      {
        quizQuestionId: id('82'),
        questionId: fx.openQuestionId,
        questionText: 'Why is it prime?',
        questionType: 'open_ended',
        marks: '4',
        options: [],
      },
    ],
    attemptsUsed: 1,
    attemptToken: fx.attemptToken,
    quizSubmissionId: fx.quizSubmissionId,
    attemptNumber: 1,
    serverTime: new Date().toISOString(),
  };
  const submission = () => ({
    id: fx.submissionId,
    assignmentId: fx.assignmentId,
    userId: fx.studentId,
    answer: 'My essay.',
    status: 'not_graded',
    isFinal: submissionFinal,
    ...(submissionFinal ? { finalizedAt: '2026-11-14T10:00:00Z' } : {}),
    etag: '"1"',
  });
  return {
    [`GET /v1/lms/content-items/{contentItemId}`]: (_req, p) =>
      p['contentItemId'] === fx.quizItemId
        ? json({
            id: fx.quizItemId,
            itemKind: 'quiz',
            quizId: fx.quizId,
            cohortId: fx.cohortId,
            title: 'Quiz',
            points: '20',
            isGraded: true,
          })
        : json({
            id: fx.assignmentItemId,
            itemKind: 'assignment',
            assignmentId: fx.assignmentId,
            cohortId: fx.cohortId,
            title: 'Essay',
            points: '20',
            isGraded: true,
          }),
    [`POST /v1/lms/content-items/{contentItemId}:startQuizAttempt`]: () => json(attempt),
    [`POST /v1/lms/quiz-submissions/{quizSubmissionId}:saveResponses`]: async (req) => {
      const body = (await req.json()) as { attemptToken?: string; answers?: unknown[] };
      if (body.attemptToken !== fx.attemptToken)
        return problem(409, 'ATTEMPT_TOKEN_MISMATCH', 'This answer sheet belongs to another delivery.');
      return json({
        savedCount: body.answers?.length ?? 0,
        ignoredStale: 0,
        serverTime: new Date().toISOString(),
      });
    },
    [`POST /v1/lms/quiz-submissions/{quizSubmissionId}:submit`]: async (req) => {
      const body = (await req.json()) as {
        attemptToken?: string;
        answers?: { selectedOptionIds?: string[] }[];
      };
      if (body.attemptToken !== fx.attemptToken)
        return problem(409, 'ATTEMPT_TOKEN_MISMATCH', 'This answer sheet belongs to another delivery.');
      const right = body.answers?.some((answer) => answer.selectedOptionIds?.includes(fx.optionRight));
      return json({
        id: fx.quizSubmissionId,
        quizId: fx.quizId,
        userId: fx.studentId,
        status: 'submitted',
        score: right ? '6.00' : '0.00',
        scoreOutOf: '10.00',
        percentage: right ? '60.00' : '0.00',
        attemptNumber: 1,
        etag: '"2"',
      });
    },
    [`GET /v1/lms/quiz-submissions/{quizSubmissionId}`]: () =>
      json({
        id: fx.quizSubmissionId,
        quizId: fx.quizId,
        userId: fx.studentId,
        status: 'graded',
        score: '6.00',
        scoreOutOf: '10.00',
        percentage: '60.00',
        correctCount: 1,
        incorrectCount: 1,
        timeTakenSeconds: 300,
        attemptNumber: 1,
        etag: '"3"',
      }),
    [`GET /v1/lms/quiz-submissions/{quizSubmissionId}/answers`]: (req) =>
      onePage(req, [
        {
          id: id('91'),
          questionId: fx.choiceQuestionId,
          questionText: 'Pick the prime number.',
          isCorrect: true,
          marksAwarded: '6.00',
          marksPossible: '6.00',
        },
        {
          id: id('92'),
          questionId: fx.openQuestionId,
          questionText: 'Why is it prime?',
          answer: 'Because only 1 and 7 divide it.',
          marksPossible: '4.00',
        },
      ]),
    [`GET /v1/lms/quiz-submissions`]: (req) =>
      onePage(req, [
        {
          id: fx.quizSubmissionId,
          quizId: fx.quizId,
          userId: fx.studentId,
          status: 'submitted',
          contentItemId: fx.quizItemId,
        },
      ]),
    [`GET /v1/lms/assignments/{assignmentId}`]: () =>
      json(
        {
          id: fx.assignmentId,
          title: 'Prime essay',
          question: 'Write about primes.',
          submissionType: 'text',
          etag: '"1"',
        },
        { headers: { ETag: '"1"' } },
      ),
    [`GET /v1/lms/assignment-submissions`]: (req) => onePage(req, [submission()]),
    [`POST /v1/lms/assignment-submissions/{assignmentSubmissionId}:finalize`]: () => {
      submissionFinal = true;
      return json(submission());
    },
    [`POST /v1/lms/assignment-submissions/{assignmentSubmissionId}:grade`]: async (req) => {
      const body = (await req.json()) as { status?: string };
      if (!submissionFinal) return problem(409, 'INVALID_STATE', 'A draft is evaluated once it is final.');
      return json({ ...submission(), status: body.status });
    },
    [`GET /v1/lms/cohorts/{cohortId}:gradebook`]: () =>
      json({
        cohortId: fx.cohortId,
        gradeDisplay: 'points',
        columns: [{ contentItemId: fx.quizItemId, title: 'Quiz', itemKind: 'quiz', points: '20' }],
        rows: [
          {
            userId: fx.studentId,
            studentName: 'Asha Verma',
            totalPercent: '60.00',
            cells: [
              { contentItemId: fx.quizItemId, entryId: id('a1'), points: entryPoints, isPublished: false },
            ],
          },
          { userId: fx.otherStudentId, studentName: 'Rohan Gupta', cells: [] },
        ],
        etag: '"7"',
      }),
    [`POST /v1/lms/gradebook-entries:grade`]: async (req) => {
      const body = (await req.json()) as { etag?: string; grade?: { points?: string } };
      if (body.etag !== undefined && body.etag !== null && body.etag !== entryEtag)
        return problem(
          412,
          'PRECONDITION_FAILED',
          'The gradebook entry changed since it was read; read it again.',
        );
      entryPoints = body.grade?.points ?? entryPoints;
      entryEtag = '"2"';
      return json({
        id: id('a1'),
        contentItemId: fx.quizItemId,
        userId: fx.studentId,
        points: entryPoints,
        manualPoints: entryPoints,
        status: 'graded',
        etag: entryEtag,
      });
    },
    [`GET /v1/lms/grade-change-logs`]: (req) =>
      onePage(req, [
        { id: id('b1'), points: '12.00', changedAt: '2026-11-14T09:00:00Z', comment: 'Rechecked' },
      ]),
    [`GET /v1/lms/reassignment-logs`]: (req) => onePage(req, []),
    [`GET /v1/lms/questions:search`]: (req) => {
      const query = new URL(req.url).searchParams.get('query') ?? '';
      const all = [
        {
          id: fx.choiceQuestionId,
          questionText: 'Pick the prime number.',
          questionType: 'choices',
          marks: '6',
          etag: '"1"',
        },
        {
          id: fx.openQuestionId,
          questionText: 'Why is it prime?',
          questionType: 'open_ended',
          marks: '4',
          etag: '"1"',
        },
      ];
      return onePage(
        req,
        all.filter((question) => question.questionText.toLowerCase().includes(query.toLowerCase())),
      );
    },
    [`POST /v1/lms/questions:import`]: async (req) => {
      const body = (await req.json()) as { fileId?: string; format?: string };
      if (body.format !== 'gift' && body.format !== 'csv')
        return problem(422, 'VALIDATION_FAILED', 'only gift and csv files are imported by this release');
      return json({ id: id('c9'), status: 'succeeded', result: { imported: 3 } }, { status: 202 });
    },
    [`GET /v1/lms/drive-items`]: (req) =>
      onePage(req, [
        {
          id: fx.driveItemId,
          ownerUserId: schoolAdmin.user?.id ?? fx.studentId,
          title: 'Notes.pdf',
          itemType: 'file',
          isShared: shares.length > 0,
          etag: '"1"',
        },
      ]),
    [`GET /v1/lms/drive-items/{driveItemId}/shares`]: (req) =>
      onePage(
        req,
        shares.map((userId, index) => ({ id: id(`d${index}`), driveItemId: fx.driveItemId, userId })),
      ),
    [`POST /v1/lms/drive-items/{driveItemId}:share`]: async (req) => {
      const body = (await req.json()) as { userIds?: string[] };
      shares = body.userIds ?? [];
      return json({
        items: shares.map((userId, index) => ({ id: id(`d${index}`), driveItemId: fx.driveItemId, userId })),
      });
    },
    [`GET /v1/platform/me/storage`]: () => json({ quotaBytes: '104857600', usedBytes: '52428800' }),
  };
}

/**
 * The learning side over in-memory state with the server's rules for what the screens use: a lesson with a video is
 * refused completion (409 VIDEO_NOT_WATCHED) until its watch time is reported, a repeated enrolment is 409 DUPLICATE,
 * and a course step needs the state it starts from (409) and the course's etag (412).
 */
function learningHandlers(): Record<string, Handler> {
  const f = lmsFixture;
  const published: Mutable<lms.LmsCourse> = {
    id: f.courseId,
    title: 'Algebra basics',
    shortIntroduction: 'Equations from the ground up.',
    status: 'approved',
    lifecycleStatus: 'published',
    catalogStatus: 'published',
    isPaid: false,
    enrollmentCount: 12,
    etag: '"3"',
  } as Mutable<lms.LmsCourse>;
  const draft: Mutable<lms.LmsCourse> = {
    id: f.draftCourseId,
    title: 'Geometry draft',
    status: 'in_progress',
    lifecycleStatus: 'draft',
    catalogStatus: 'draft',
    etag: '"1"',
  } as Mutable<lms.LmsCourse>;
  const courses = [published, draft];
  let enrolled = false;
  let watched = false;
  const progress = new Map<string, string>();
  const notes: Mutable<lms.LessonNote>[] = [];
  const bookmarks: Mutable<lms.Bookmark>[] = [];
  const chapter: Mutable<lms.Chapter> = {
    id: f.chapterId,
    courseId: f.courseId,
    title: 'Linear equations',
    sortOrder: 0,
    etag: '"1"',
  } as Mutable<lms.Chapter>;
  const authors: Mutable<lms.CourseAuthorProfile>[] = [];
  const edits = new Map<string, Partial<lms.Lesson>>();
  const settings: Mutable<lms.Settings> = {
    allowGuestAccess: true,
    disableSignup: true,
    etag: '"0"',
  } as Mutable<lms.Settings>;

  const lesson = (lessonId: string): lms.Lesson =>
    (lessonId === f.videoLessonId
      ? {
          id: lessonId,
          chapterId: f.chapterId,
          title: 'Solving for x',
          youtubeVideoId: 'abc123',
          includeInPreview: true,
          etag: '"1"',
          ...edits.get(lessonId),
        }
      : {
          id: lessonId,
          chapterId: f.chapterId,
          title: 'Two unknowns',
          body: 'Substitute one equation into the other.',
          content: {
            blocks: [
              { type: 'list', data: { style: 'ordered', items: ['Isolate y', 'Substitute'] } },
              { type: 'embed', data: { source: 'https://graphs.example.org/plot' } },
            ],
          },
          includeInPreview: false,
          etag: '"1"',
          ...edits.get(lessonId),
        }) as lms.Lesson;
  const outline = (): lms.CourseOutline =>
    ({
      course: published,
      chapters: [
        {
          chapterId: f.chapterId,
          title: chapter.title,
          sortOrder: 0,
          isScormPackage: false,
          lessons: [f.videoLessonId, f.textLessonId].map((lessonId, sortOrder) => ({
            lessonId,
            title: lesson(lessonId).title,
            sortOrder,
            includeInPreview: lesson(lessonId).includeInPreview,
            ...(enrolled ? { progressStatus: progress.get(lessonId) ?? 'incomplete' } : {}),
          })),
        },
      ],
      ...(enrolled ? { enrollmentId: lid('e41'), progressPercent: `${progress.size * 50}.00` } : {}),
    }) as lms.CourseOutline;
  const step =
    (from: Partial<lms.LmsCourse>, to: Partial<lms.LmsCourse>): Handler =>
    async (req, { courseId }) => {
      const course = courses.find((c) => c.id === courseId);
      if (!course) return problem(404, 'NOT_FOUND');
      const body = (await req.json()) as { etag?: string };
      if (body.etag && body.etag !== course.etag) return problem(412, 'ETAG_MISMATCH');
      if (Object.entries(from).some(([k, v]) => course[k as keyof lms.LmsCourse] !== v)) {
        return problem(409, 'INVALID_STATE_TRANSITION');
      }
      Object.assign(course, to, { etag: `"${Number(course.etag?.replace(/"/g, '') ?? 0) + 1}"` });
      return json(course);
    };

  return {
    'GET /v1/lms/catalog': async (req) => {
      const page = (await pageOf(
        req,
        courses.filter((c) => c.catalogStatus === 'published'),
      ).json()) as { data: lms.LmsCourse[]; paging: unknown };
      return json({
        featured: [],
        showcase: [],
        courses: page.data,
        categories: [],
        openCohorts: [],
        paging: page.paging,
      });
    },
    'GET /v1/lms/public-settings': () =>
      json({ showCourses: true, allowGuestAccess: true, contactUsEmail: 'learn@school.example.com' }),
    'GET /v1/lms/statistics': () =>
      json({
        publishedCourses: 1,
        students: 12,
        enrollments: 12,
        certificatesIssued: 0,
        completionPercent: '0.00',
      }),
    'GET /v1/lms/categories': (req) => pageOf(req, [{ id: f.categoryId, name: 'Mathematics' }]),
    'GET /v1/lms/courses/{courseId}:outline': () => json(outline()),
    'POST /v1/lms/courses/{courseId}:enroll': (_req, { courseId }) => {
      if (courseId !== f.courseId) return problem(404, 'NOT_FOUND');
      if (enrolled) return problem(409, 'DUPLICATE');
      enrolled = true;
      return json({ id: lid('e41'), courseId, progress: '0.00' });
    },
    'POST /v1/lms/courses/{courseId}:submitForReview': step(
      { status: 'in_progress' },
      { status: 'under_review' },
    ),
    'GET /v1/lms/courses/{courseId}:syllabus': (_req, { courseId }) =>
      json({ url: `https://files.example.com/syllabus/${courseId}.html`, expiresAt: '2026-01-01T00:05:00Z' }),
    'GET /v1/lms/courses/{courseId}/reviews': (req) => pageOf(req, []),
    'GET /v1/lms/courses/{courseId}/previews': (req) => pageOf(req, []),
    'GET /v1/lms/courses/{courseId}/testimonials': (req) => pageOf(req, []),
    'GET /v1/lms/courses/{courseId}/author-profiles': (req) => pageOf(req, authors),
    'GET /v1/lms/courses/{courseId}': (_req, { courseId }) => {
      const course = courses.find((c) => c.id === courseId);
      return course ? json(course, { headers: { ETag: course.etag ?? '' } }) : problem(404, 'NOT_FOUND');
    },
    'GET /v1/lms/courses': (req) => pageOf(req, courses),
    'GET /v1/lms/lessons/{lessonId}:content': (_req, { lessonId = '' }) => {
      const found = lesson(lessonId);
      const canAccess = enrolled || Boolean(found.includeInPreview);
      return json({
        lesson: canAccess ? found : { id: found.id, title: found.title, chapterId: found.chapterId },
        activities: [],
        ...(lessonId === f.videoLessonId
          ? { nextLessonId: f.textLessonId }
          : { previousLessonId: f.videoLessonId }),
        ...(enrolled ? { progressStatus: progress.get(lessonId) ?? 'incomplete' } : {}),
        canAccess,
      });
    },
    'POST /v1/lms/lessons/{lessonId}:trackVideoWatch': (_req, { lessonId }) => {
      if (!enrolled) return problem(409, 'NOT_ENROLLED');
      watched = lessonId === f.videoLessonId || watched;
      return json({ id: lid('e51'), lessonId, watchSeconds: '60.00' });
    },
    'POST /v1/lms/lessons/{lessonId}:markComplete': (_req, { lessonId = '' }) => {
      if (!enrolled) return problem(409, 'NOT_ENROLLED');
      if (lessonId === f.videoLessonId && !watched) return problem(409, 'VIDEO_NOT_WATCHED');
      progress.set(lessonId, 'complete');
      return json({ id: lid('e61'), lessonId, status: 'complete' });
    },
    'GET /v1/lms/lessons/{lessonId}/notes': (req, { lessonId }) =>
      pageOf(
        req,
        notes.filter((n) => n.lessonId === lessonId),
      ),
    'GET /v1/lms/settings': () => json(settings, { headers: { ETag: settings.etag ?? '"0"' } }),
    'PATCH /v1/lms/settings': async (req) => {
      if (req.headers.get('If-Match') !== settings.etag) return problem(412, 'ETAG_MISMATCH');
      Object.assign(settings, (await req.json()) as object, {
        id: settings.id ?? lid('e81'),
        etag: `"${Number(settings.etag?.replace(/"/g, '') ?? 0) + 1}"`,
      });
      return json(settings, { headers: { ETag: settings.etag ?? '' } });
    },
    'GET /v1/lms/settings/{settingsId}/sidebar-items': (req) => pageOf(req, []),
    'GET /v1/lms/annotations': (req) => pageOf(req, []),
    'GET /v1/lms/bookmarks': (req) => pageOf(req, bookmarks),
    'POST /v1/lms/bookmarks': async (req) => {
      const body = (await req.json()) as lms.Bookmark;
      const bookmark = { ...body, id: lid(`e9${bookmarks.length}`), etag: '"1"' } as Mutable<lms.Bookmark>;
      bookmarks.push(bookmark);
      return json(bookmark, { status: 201 });
    },
    'DELETE /v1/lms/bookmarks/{bookmarkId}': (_req, { bookmarkId }) => {
      const at = bookmarks.findIndex((b) => b.id === bookmarkId);
      if (at < 0) return problem(404, 'NOT_FOUND');
      bookmarks.splice(at, 1);
      return new Response(null, { status: 204 });
    },
    'GET /v1/lms/quizzes': (req) =>
      pageOf(req, [{ id: f.repositoryQuizId, title: 'Fractions check', isInRepository: true }]),
    'POST /v1/lms/content-items:addFromRepository': async (req) => {
      const body = (await req.json()) as lms.AddFromRepositoryRequest;
      return json({
        items: body.sourceIds.map((sourceId, at) => ({
          id: lid(`ea${at}`),
          chapterId: body.chapterId,
          courseId: f.courseId,
          itemKind: body.itemKind,
          quizId: sourceId,
          title: 'Fractions check',
        })),
      });
    },
    'POST /v1/lms/courses/{courseId}:createVersion': async (req, { courseId }) => {
      const source = courses.find((c) => c.id === courseId);
      if (!source) return problem(404, 'NOT_FOUND');
      const { versionKind } = (await req.json()) as lms.CreateCourseVersionRequest;
      const major = versionKind === 'major';
      const copy = {
        ...source,
        id: lid(`eb${courses.length}`),
        rootCourseId: source.rootCourseId ?? source.id,
        previousVersionId: source.id,
        versionMajor: (source.versionMajor ?? 1) + (major ? 1 : 0),
        versionMinor: major ? 0 : (source.versionMinor ?? 0) + 1,
        status: 'in_progress',
        lifecycleStatus: 'draft',
        etag: '"1"',
      } as Mutable<lms.LmsCourse>;
      courses.push(copy);
      return json(copy, { status: 201 });
    },
    'GET /v1/lms/courses/{courseId}/chapters': (req) => pageOf(req, [chapter]),
    'PATCH /v1/lms/courses/{courseId}/chapters/{chapterId}': async (req) => {
      if (req.headers.get('If-Match') !== chapter.etag) return problem(412, 'ETAG_MISMATCH');
      Object.assign(chapter, (await req.json()) as object, {
        etag: `"${Number(chapter.etag?.replace(/"/g, '') ?? 0) + 1}"`,
      });
      return json(chapter);
    },
    'GET /v1/lms/courses/{courseId}/banners': (req) => pageOf(req, []),
    'POST /v1/lms/courses/{courseId}/author-profiles': async (req) => {
      const author = {
        ...((await req.json()) as lms.CourseAuthorProfile),
        id: lid(`ec${authors.length}`),
        etag: '"1"',
      } as Mutable<lms.CourseAuthorProfile>;
      authors.push(author);
      return json(author, { status: 201 });
    },
    'DELETE /v1/lms/courses/{courseId}/author-profiles/{courseAuthorProfileId}': (
      req,
      { courseAuthorProfileId },
    ) => {
      const at = authors.findIndex((a) => a.id === courseAuthorProfileId);
      if (at < 0) return problem(404, 'NOT_FOUND');
      if (req.headers.get('If-Match') !== authors[at]?.etag) return problem(412, 'ETAG_MISMATCH');
      authors.splice(at, 1);
      return new Response(null, { status: 204 });
    },
    'GET /v1/lms/lessons/{lessonId}': (_req, { lessonId = '' }) => json(lesson(lessonId)),
    'PATCH /v1/lms/lessons/{lessonId}': async (req, { lessonId = '' }) => {
      if (req.headers.get('If-Match') !== lesson(lessonId).etag) return problem(412, 'ETAG_MISMATCH');
      edits.set(lessonId, { ...edits.get(lessonId), ...((await req.json()) as object), etag: '"2"' });
      return json(lesson(lessonId));
    },
    'POST /v1/lms/lessons/{lessonId}/notes': async (req, { lessonId }) => {
      const body = (await req.json()) as lms.LessonNote;
      const note = {
        ...body,
        id: lid(`e7${notes.length}`),
        lessonId,
        etag: '"1"',
      } as Mutable<lms.LessonNote>;
      notes.push(note);
      return json(note, { status: 201 });
    },
  };
}
