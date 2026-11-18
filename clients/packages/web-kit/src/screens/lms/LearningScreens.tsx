import {
  parseLessonBlocks,
  percent,
  useCan,
  useCourseOutline,
  useEnrollInLearningProgram,
  useI18n,
  useLearningPrograms,
  useLessonContent,
  useLessonNoteActions,
  useLessonNotes,
  useMarkLessonComplete,
  useSaveScormProgress,
  useScormLaunch,
  useSession,
  useStudentDashboard,
  useTrackVideoWatch,
} from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import { Banner, Button, Card, ProgressBar, SelectField, StatusChip, TextArea } from '@mysms/ui-web';
import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { LessonBlocksView } from './LessonBlocks';
import { ActionError, LmsPage, LmsState } from './lmsUi';

const NOTE_COLORS = ['yellow', 'green', 'blue', 'red', 'purple'] as const;

/**
 * LMS-04 Student dashboard: where to continue, the reader's courses with their progress, the coming live classes of
 * their cohorts, and their learning streak.
 */
export function StudentDashboardScreen() {
  const { t, format } = useI18n();
  const allowed = useCan('lms.enrollment.get_student_dashboard');
  const dashboard = useStudentDashboard();
  const title = t('screen.LMS04.title');
  const state = LmsState({
    title,
    allowed,
    loading: allowed && dashboard.isPending,
    error: dashboard.error,
    retry: () => void dashboard.refetch(),
  });
  if (state) return state;
  const data = dashboard.data;
  if (!data) return null;
  return (
    <LmsPage
      title={title}
      subtitle={t('lms.dashboard.streak', { days: data.streakDays, longest: data.longestStreakDays })}
    >
      {data.continueLessonId ? (
        <Card title={t('lms.dashboard.continue')}>
          <Link to={`/app/lms/lessons/${data.continueLessonId}`}>{t('lms.dashboard.resume')}</Link>
        </Card>
      ) : null}
      <Card title={t('lms.dashboard.courses')}>
        {(data.enrollments ?? []).length === 0 ? (
          <p>
            {t('lms.dashboard.noCourses')} <Link to="/app/lms/catalog">{t('screen.LMS01.title')}</Link>
          </p>
        ) : (
          <ul>
            {(data.enrollments ?? []).map((enrolment) => (
              <li key={enrolment.id}>
                <Link to={`/app/lms/courses/${enrolment.courseId}`}>{t('lms.dashboard.openCourse')}</Link>
                <ProgressBar label={t('lms.course.progress')} value={percent(enrolment.progressPercent)} />
              </li>
            ))}
          </ul>
        )}
      </Card>
      {(data.upcomingLiveClasses ?? []).length > 0 ? (
        <Card title={t('lms.dashboard.liveClasses')}>
          <ul>
            {(data.upcomingLiveClasses ?? []).map((live) => (
              <li key={live.id}>
                {t('lms.dashboard.liveClass', { title: live.title, day: format.day(live.classDate) })}
              </li>
            ))}
          </ul>
        </Card>
      ) : null}
      <Card title={t('lms.dashboard.activity')}>
        <p>{t('lms.dashboard.activeDays', { days: (data.activityHeatmap ?? []).length })}</p>
      </Card>
    </LmsPage>
  );
}

/** LMS-05 Course home: the outline with the reader's progress per lesson, and where each lesson or package opens. */
export function CourseHomeScreen() {
  const { courseId = '' } = useParams();
  const { t } = useI18n();
  const outline = useCourseOutline(courseId);
  const title = outline.data?.course.title ?? t('screen.LMS05.title');
  const state = LmsState({
    title,
    allowed: true,
    loading: outline.isPending,
    error: outline.error,
    retry: () => void outline.refetch(),
  });
  if (state) return state;
  const data = outline.data;
  if (!data) return null;
  return (
    <LmsPage title={title}>
      {data.enrollmentId ? (
        <ProgressBar label={t('lms.course.progress')} value={percent(data.progressPercent)} />
      ) : (
        <Banner intent="info">
          {t('lms.course.notEnrolled')}{' '}
          <Link to={`/app/lms/courses/${courseId}/catalog`}>{t('screen.LMS02.title')}</Link>
        </Banner>
      )}
      {(data.chapters ?? []).length === 0 ? (
        <Banner intent="info">{t('lms.course.emptyOutline')}</Banner>
      ) : null}
      {(data.chapters ?? []).map((chapter) => (
        <Card key={chapter.chapterId} title={chapter.title}>
          {chapter.isScormPackage ? (
            <Link to={`/app/lms/chapters/${chapter.chapterId}/scorm`}>{t('lms.course.openPackage')}</Link>
          ) : (
            <ul>
              {chapter.lessons.map((lesson) => (
                <li key={lesson.lessonId} className="lms-outline__lesson">
                  <Link to={`/app/lms/lessons/${lesson.lessonId}`}>{lesson.title}</Link>
                  {lesson.progressStatus ? (
                    <StatusChip
                      intent={lesson.progressStatus === 'complete' ? 'success' : 'neutral'}
                      label={enumLabel(t, 'lmsProgress', lesson.progressStatus)}
                    />
                  ) : null}
                </li>
              ))}
            </ul>
          )}
        </Card>
      ))}
    </LmsPage>
  );
}

/** The reader's own notes beside a lesson: written and removed by them alone. */
function LessonNotesPanel({ lessonId }: { lessonId: string }) {
  const { t } = useI18n();
  const mayWrite = useCan('lms.lesson_note.create');
  const mayRead = useCan('lms.lesson_note.read');
  const notes = useLessonNotes(lessonId, mayRead);
  const { create, remove } = useLessonNoteActions(lessonId);
  const [text, setText] = useState('');
  const [color, setColor] = useState<(typeof NOTE_COLORS)[number]>('yellow');
  if (!mayRead) return null;
  return (
    <Card title={t('lms.notes.title')}>
      <ul>
        {(notes.data?.items ?? []).map((note) => (
          <li key={note.id}>
            <StatusChip
              intent="neutral"
              label={t(`lms.notes.color.${note.color}` as 'lms.notes.color.yellow')}
            />{' '}
            {note.note ?? note.highlightedText}
            <Button variant="ghost" onClick={() => remove.mutate(note)}>
              {t('common.action.delete')}
            </Button>
          </li>
        ))}
      </ul>
      {mayWrite ? (
        <div className="form">
          <TextArea label={t('lms.notes.new')} value={text} onChange={(e) => setText(e.target.value)} />
          <SelectField
            label={t('lms.notes.colour')}
            value={color}
            options={NOTE_COLORS.map((c) => ({ value: c, label: t(`lms.notes.color.${c}`) }))}
            onChange={(e) => setColor(e.target.value as (typeof NOTE_COLORS)[number])}
          />
          <Button
            disabled={!text.trim()}
            loading={create.isPending}
            onClick={() => create.mutate({ note: text, color }, { onSuccess: () => setText('') })}
          >
            {t('lms.notes.add')}
          </Button>
          <ActionError error={create.error ?? remove.error} />
        </div>
      ) : null}
    </Card>
  );
}

/**
 * LMS-06 Lesson player: the lesson's blocks, text and video, its neighbours, completing it and the reader's notes. A lesson
 * the reader cannot open yet (not enrolled, not a preview) shows its title and where to enrol.
 */
export function LessonPlayerScreen() {
  const { lessonId = '' } = useParams();
  const { t } = useI18n();
  const content = useLessonContent(lessonId);
  const complete = useMarkLessonComplete();
  const watch = useTrackVideoWatch();
  const signedIn = useSession((s) => s.status === 'signedIn');
  const mayComplete = useCan('lms.lesson.mark_lesson_complete');
  const mayTrack = useCan('lms.lesson.track_video_watch');
  const [watchedFrom] = useState(() => Date.now());
  const title = content.data?.lesson.title ?? t('screen.LMS06.title');
  const state = LmsState({
    title,
    allowed: true,
    loading: content.isPending,
    error: content.error,
    retry: () => void content.refetch(),
  });
  if (state) return state;
  const data = content.data;
  if (!data) return null;
  const video = data.lesson.youtubeVideoId;
  const blocks = parseLessonBlocks(data.lesson.content);
  return (
    <LmsPage
      title={title}
      subtitle={data.progressStatus ? enumLabel(t, 'lmsProgress', data.progressStatus) : undefined}
    >
      <div className="lms-player">
        <div>
          {!data.canAccess ? <Banner intent="info">{t('lms.lesson.locked')}</Banner> : null}
          {video ? (
            <Card title={t('lms.lesson.video')}>
              <iframe
                className="lms-frame"
                title={t('lms.lesson.video')}
                src={`https://www.youtube-nocookie.com/embed/${encodeURIComponent(video)}`}
                allow="encrypted-media; picture-in-picture"
              />
              {mayTrack && signedIn ? (
                <Button
                  variant="secondary"
                  loading={watch.isPending}
                  onClick={() =>
                    watch.mutate({ lessonId, videoSource: video, seconds: (Date.now() - watchedFrom) / 1000 })
                  }
                >
                  {t('lms.lesson.watched')}
                </Button>
              ) : null}
              <ActionError error={watch.error} />
            </Card>
          ) : null}
          {blocks.length > 0 ? (
            <Card>
              <LessonBlocksView blocks={blocks} />
            </Card>
          ) : null}
          {data.lesson.body ? <Card>{data.lesson.body}</Card> : null}
          <div className="lms-toolbar">
            {data.previousLessonId ? (
              <Link to={`/app/lms/lessons/${data.previousLessonId}`}>{t('lms.lesson.previous')}</Link>
            ) : null}
            {data.nextLessonId ? (
              <Link to={`/app/lms/lessons/${data.nextLessonId}`}>{t('lms.lesson.next')}</Link>
            ) : null}
            {mayComplete && signedIn && data.canAccess && data.progressStatus !== 'complete' ? (
              <Button loading={complete.isPending} onClick={() => complete.mutate(lessonId)}>
                {t('lms.lesson.complete')}
              </Button>
            ) : null}
          </div>
          <ActionError error={complete.error} />
        </div>
        {signedIn && data.canAccess ? <LessonNotesPanel lessonId={lessonId} /> : null}
      </div>
    </LmsPage>
  );
}

/**
 * LMS-07 SCORM player: the chapter's package in a frame, its saved state shown, and saving the reader's progress. A
 * deployment that serves no packages says so instead of an empty frame.
 */
export function ScormPlayerScreen() {
  const { chapterId = '' } = useParams();
  const { t } = useI18n();
  const allowed = useCan('lms.chapter.get_scorm_launch');
  const launch = useScormLaunch(chapterId);
  const save = useSaveScormProgress(chapterId);
  const mayAccess = allowed;
  const title = t('screen.LMS07.title');
  const state = LmsState({
    title,
    allowed: mayAccess,
    loading: mayAccess && launch.isPending,
    error: launch.error,
    retry: () => void launch.refetch(),
  });
  if (state) return state;
  const data = launch.data;
  if (!data) return null;
  const saved = (data.runtimeState ?? {}) as Record<string, unknown>;
  return (
    <LmsPage title={title}>
      <iframe
        className="lms-frame"
        title={title}
        src={data.launchUrl}
        sandbox="allow-scripts allow-same-origin"
      />
      <div className="lms-toolbar">
        <Button
          variant="secondary"
          loading={save.isPending}
          onClick={() => save.mutate({ status: 'partially_complete', runtimeState: saved })}
        >
          {t('lms.scorm.saveProgress')}
        </Button>
        <Button
          loading={save.isPending}
          onClick={() => save.mutate({ status: 'complete', runtimeState: saved })}
        >
          {t('lms.scorm.finish')}
        </Button>
      </div>
      <ActionError error={save.error} />
    </LmsPage>
  );
}

/** LMS-17 Learning programs: the published programs, each joined once. */
export function LearningProgramsScreen() {
  const { t, format } = useI18n();
  const allowed = useCan('lms.learning_program.read');
  const programs = useLearningPrograms();
  const mayJoin = useCan('lms.learning_program.enroll_in_learning_program');
  const join = useEnrollInLearningProgram();
  const title = t('screen.LMS17.title');
  const state = LmsState({
    title,
    allowed: allowed || mayJoin,
    loading: programs.isPending,
    error: programs.error,
    retry: () => void programs.refetch(),
  });
  if (state) return state;
  const rows = programs.data?.items ?? [];
  return (
    <LmsPage title={title}>
      {rows.length === 0 ? <Banner intent="info">{t('lms.programs.empty')}</Banner> : null}
      <div className="lms-cards">
        {rows.map((program) => (
          <Card key={program.id} title={program.title}>
            <p>
              {t('lms.programs.counts', {
                courses: format.number(program.courseCount ?? 0),
                members: format.number(program.memberCount ?? 0),
              })}
            </p>
            {mayJoin ? (
              <Button
                loading={join.isPending && join.variables === program.id}
                onClick={() => join.mutate(program.id ?? '')}
              >
                {t('lms.programs.join')}
              </Button>
            ) : null}
          </Card>
        ))}
      </div>
      <ActionError error={join.error} />
    </LmsPage>
  );
}
