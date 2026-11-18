import {
  lmsFailureText,
  linkHost,
  parseLessonBlocks,
  percent,
  type LessonBlock,
  type PermissionCode,
  useAnnotationActions,
  useAnnotations,
  useBookmarkActions,
  useBookmarks,
  useCan,
  useCatalogHome,
  useCourseOutline,
  useCoursePage,
  useDownloadUrl,
  useEbook,
  useEnrollInCourse,
  useI18n,
  useLessonContent,
  useLessonNoteActions,
  useLessonNotes,
  useLmsCategories,
  useMarkLessonComplete,
  useMyRelatedMaterials,
  usePublicLmsSettings,
  useRecordItemView,
  useSaveScormProgress,
  useScormLaunch,
  useSession,
  useStudentDashboard,
  useTrackVideoWatch,
  useUnreadCounts,
} from '@mysms/core';
import { enumLabel, type MessageKey } from '@mysms/i18n';
import { Banner, Button, ChoiceGroup, Text, TextInput, Screen } from '@mysms/ui-native';
import { useEffect, useState, type ReactNode } from 'react';
import { Linking, View } from 'react-native';
import type { LearningScreenId, LmsRoute } from './lmsRoutes';

const TITLE_OF: Record<LearningScreenId, MessageKey> = {
  'LMS-01': 'screen.LMS01.title',
  'LMS-02': 'screen.LMS02.title',
  'LMS-04': 'screen.LMS04.title',
  'LMS-05': 'screen.LMS05.title',
  'LMS-06': 'screen.LMS06.title',
  'LMS-07': 'screen.LMS07.title',
  'LMS-13': 'screen.LMS13.title',
  'LMS-14': 'screen.LMS14.title',
};

/** The tablet-first screens of the inventory (W B): the phone offers the rest. */
const TABLET_ONLY: ReadonlySet<LearningScreenId> = new Set(['LMS-07', 'LMS-13']);

/** One platform-neutral learning workspace, selected by the shared screen id and deep-link parameters. */
export function LearningScreen({
  route,
  tablet = false,
  onBack,
  onNavigate,
  onSignIn,
}: {
  route: LmsRoute<LearningScreenId>;
  tablet?: boolean;
  onBack: () => void;
  onNavigate: (route: LmsRoute) => void;
  /** For a guest: the way to sign in, offered where enrolling needs a session. */
  onSignIn?: () => void;
}) {
  const { t } = useI18n();
  return (
    <Screen tablet={tablet}>
      <Text variant="h1" accessibilityRole="header">
        {t(TITLE_OF[route.screenId])}
      </Text>
      <Button variant="secondary" onPress={onBack}>
        {t('common.action.back')}
      </Button>
      {!tablet && TABLET_ONLY.has(route.screenId) ? (
        <Banner>{t('lms.native.tabletOnly')}</Banner>
      ) : (
        <LearningContent route={route} tablet={tablet} onNavigate={onNavigate} onSignIn={onSignIn} />
      )}
    </Screen>
  );
}

function LearningContent({
  route,
  tablet,
  onNavigate,
  onSignIn,
}: {
  route: LmsRoute<LearningScreenId>;
  tablet: boolean;
  onNavigate: (route: LmsRoute) => void;
  onSignIn: (() => void) | undefined;
}) {
  switch (route.screenId) {
    case 'LMS-01':
      return <CatalogHome onNavigate={onNavigate} />;
    case 'LMS-02':
      return (
        <CoursePage courseId={route.params.courseId ?? ''} onNavigate={onNavigate} onSignIn={onSignIn} />
      );
    case 'LMS-04':
      return (
        <Allowed permission="lms.enrollment.get_student_dashboard">
          <MyLearning onNavigate={onNavigate} />
        </Allowed>
      );
    case 'LMS-05':
      return <CourseHome courseId={route.params.courseId ?? ''} tablet={tablet} onNavigate={onNavigate} />;
    case 'LMS-06':
      return <LessonPlayer lessonId={route.params.lessonId ?? ''} onNavigate={onNavigate} />;
    case 'LMS-07':
      return (
        <Allowed permission="lms.chapter.get_scorm_launch">
          <ScormPlayer chapterId={route.params.chapterId ?? ''} />
        </Allowed>
      );
    case 'LMS-13':
      return (
        <Allowed permission="lms.ebook.read">
          <EbookReader ebookId={route.params.ebookId ?? ''} contentItemId={route.params.item ?? null} />
        </Allowed>
      );
    case 'LMS-14':
      return (
        <Allowed permission="lms.annotation.read">
          <NotesAndBookmarks />
        </Allowed>
      );
  }
}

/** Renders the panel only for a reader holding the permission, so a refused reader causes no calls. */
function Allowed({ permission, children }: { permission: PermissionCode; children: ReactNode }) {
  const { t } = useI18n();
  return useCan(permission) ? children : <Banner>{t('lms.access.denied')}</Banner>;
}

/** Loading and error states shared by the learning panels. */
function QueryState({
  pending,
  error,
  retry,
}: {
  pending: boolean;
  error: unknown;
  retry: () => void;
}): ReactNode {
  const { t } = useI18n();
  if (pending) return <Text muted>{t('common.state.loading')}</Text>;
  if (error)
    return (
      <Banner intent="danger">
        <Text>{lmsFailureText(t, error)}</Text>
        <Button variant="secondary" onPress={retry}>
          {t('common.action.retry')}
        </Button>
      </Banner>
    );
  return null;
}

/** A failed write shown under its action, in the words lmsFailureText gives it. */
function ActionError({ error }: { error: unknown }) {
  const { t } = useI18n();
  return error ? <Banner intent="danger">{lmsFailureText(t, error)}</Banner> : null;
}

/**
 * LMS-01: the catalogue's courses, searched by words and narrowed to a category a page at a time, with the featured
 * courses, the cohorts open for enrolment and the school's contact address; each course opens its page.
 */
function CatalogHome({ onNavigate }: { onNavigate: (route: LmsRoute) => void }) {
  const { t, format } = useI18n();
  const [words, setWords] = useState('');
  const [filter, setFilter] = useState({ query: '', categoryId: '', page: 1 });
  const home = useCatalogHome(filter);
  const categories = useLmsCategories();
  const settings = usePublicLmsSettings();
  if (settings.data?.showCourses === false) return <Banner>{t('lms.access.denied')}</Banner>;
  const state = QueryState({ pending: home.isPending, error: home.error, retry: () => void home.refetch() });
  const open = (courseId: string) => onNavigate({ screenId: 'LMS-02', params: { courseId } });
  const courses = home.data?.courses ?? [];
  return (
    <View>
      <TextInput
        label={t('lms.catalog.search')}
        value={words}
        onChangeText={setWords}
        returnKeyType="search"
        onSubmitEditing={() => setFilter({ ...filter, query: words.trim(), page: 1 })}
      />
      <ChoiceGroup
        label={t('lms.catalog.category')}
        value={filter.categoryId}
        choices={[
          { value: '', label: t('lms.catalog.allCategories') },
          ...(categories.data?.items ?? []).map((category) => ({
            value: category.id ?? '',
            label: category.name,
          })),
        ]}
        onChange={(categoryId) => setFilter({ ...filter, categoryId, page: 1 })}
      />
      {state ?? (
        <View>
          {(home.data?.featured ?? []).length > 0 ? (
            <Text variant="h3">{t('lms.catalog.featured')}</Text>
          ) : null}
          {(home.data?.featured ?? []).map((banner) => (
            <Button key={banner.id} variant="secondary" onPress={() => open(banner.courseId ?? '')}>
              {t('lms.catalog.open')}
            </Button>
          ))}
          {courses.length === 0 ? <Banner>{t('lms.catalog.empty')}</Banner> : null}
          {courses.map((course) => (
            <View key={course.id}>
              <Text variant="h3">{course.title}</Text>
              <Text muted>
                {t('lms.catalog.enrolled', { count: format.number(course.enrollmentCount ?? 0) })}
              </Text>
              <Button variant="secondary" onPress={() => open(course.id ?? '')}>
                {t('lms.catalog.open')}
              </Button>
            </View>
          ))}
          {filter.page > 1 ? (
            <Button variant="ghost" onPress={() => setFilter({ ...filter, page: filter.page - 1 })}>
              {t('lms.catalog.previous')}
            </Button>
          ) : null}
          {home.data?.paging?.next ? (
            <Button variant="ghost" onPress={() => setFilter({ ...filter, page: filter.page + 1 })}>
              {t('lms.catalog.next')}
            </Button>
          ) : null}
          {(home.data?.openCohorts ?? []).length > 0 ? (
            <Text variant="h3">{t('lms.catalog.openCohorts')}</Text>
          ) : null}
          {(home.data?.openCohorts ?? []).map((cohort) => (
            <Text key={cohort.id} muted>
              {t('lms.catalog.cohort', { title: cohort.title, from: format.day(cohort.startDate) })}
            </Text>
          ))}
        </View>
      )}
      {settings.data?.contactUsEmail ? (
        <Text muted>{t('lms.catalog.contact', { email: settings.data.contactUsEmail })}</Text>
      ) : null}
    </View>
  );
}

/**
 * LMS-02: the course as the public sees it — introduction, outline with its preview lessons, authors, testimonials
 * and reviews — with free enrolment for a reader who may, the way on for one already enrolled, or signing in for a
 * guest.
 */
function CoursePage({
  courseId,
  onNavigate,
  onSignIn,
}: {
  courseId: string;
  onNavigate: (route: LmsRoute) => void;
  onSignIn: (() => void) | undefined;
}) {
  const { t } = useI18n();
  const page = useCoursePage(courseId);
  const outline = useCourseOutline(courseId);
  const enrol = useEnrollInCourse();
  const mayEnrol = useCan('lms.course.enroll_in_course');
  const state = QueryState({ pending: page.isPending, error: page.error, retry: () => void page.refetch() });
  if (state) return state;
  const data = page.data;
  if (!data) return null;
  const courseHome = () => onNavigate({ screenId: 'LMS-05', params: { courseId } });
  const enrolled = Boolean(outline.data?.enrollmentId);
  return (
    <View>
      <Text variant="h2">{data.course.title}</Text>
      {data.course.shortIntroduction ? <Text>{data.course.shortIntroduction}</Text> : null}
      {data.course.isPaid ? <Banner>{t('lms.course.paid')}</Banner> : null}
      {enrolled ? (
        <Button onPress={courseHome}>{t('lms.course.continue')}</Button>
      ) : mayEnrol && !data.course.isPaid ? (
        <Button disabled={enrol.isPending} onPress={() => enrol.mutate(courseId, { onSuccess: courseHome })}>
          {t('lms.course.enrol')}
        </Button>
      ) : onSignIn ? (
        <Button onPress={onSignIn}>{t('lms.course.signInToEnrol')}</Button>
      ) : null}
      <ActionError error={enrol.error} />
      {data.course.description ? <Text>{data.course.description}</Text> : null}
      <Text variant="h3">{t('lms.course.outline')}</Text>
      {(outline.data?.chapters ?? []).map((chapter) => (
        <View key={chapter.chapterId}>
          <Text>{chapter.title}</Text>
          {chapter.lessons.map((lesson) =>
            lesson.includeInPreview ? (
              <Button
                key={lesson.lessonId}
                variant="ghost"
                onPress={() => onNavigate({ screenId: 'LMS-06', params: { lessonId: lesson.lessonId } })}
              >
                {lesson.title}
              </Button>
            ) : (
              <Text key={lesson.lessonId} muted>
                {lesson.title}
              </Text>
            ),
          )}
        </View>
      ))}
      {data.authors.length > 0 ? <Text variant="h3">{t('lms.course.authors')}</Text> : null}
      {data.authors.map((author) => (
        <Text key={author.id}>
          {author.name + (author.qualification ? ` · ${author.qualification}` : '')}
        </Text>
      ))}
      {data.testimonials.length > 0 ? <Text variant="h3">{t('lms.course.testimonials')}</Text> : null}
      {data.testimonials.map((testimonial) => (
        <Text key={testimonial.id} muted>
          {testimonial.quote}
        </Text>
      ))}
      <Text variant="h3">{t('lms.course.reviews')}</Text>
      {data.reviews.length === 0 ? <Text muted>{t('lms.course.noReviews')}</Text> : null}
      {data.reviews.map((review) => (
        <Text key={review.id} muted>
          {t('lms.course.rating', { rating: review.rating }) + ' ' + (review.review ?? '')}
        </Text>
      ))}
    </View>
  );
}

/** LMS-04: where to continue, the reader's courses with progress, unread posts, live classes and their streak. */
function MyLearning({ onNavigate }: { onNavigate: (route: LmsRoute) => void }) {
  const { t, format } = useI18n();
  const dashboard = useStudentDashboard();
  const unread = useUnreadCounts();
  const state = QueryState({
    pending: dashboard.isPending,
    error: dashboard.error,
    retry: () => void dashboard.refetch(),
  });
  if (state) return state;
  const data = dashboard.data;
  if (!data) return null;
  const enrollments = data.enrollments ?? [];
  return (
    <View>
      <Text muted>
        {t('lms.dashboard.streak', { days: data.streakDays, longest: data.longestStreakDays })}
      </Text>
      {(unread.data?.forumPosts ?? 0) > 0 ? (
        <Banner>{t('lms.teacher.unread', { count: format.number(unread.data?.forumPosts ?? 0) })}</Banner>
      ) : null}
      {data.continueLessonId ? (
        <Button
          onPress={() =>
            onNavigate({ screenId: 'LMS-06', params: { lessonId: data.continueLessonId ?? '' } })
          }
        >
          {t('lms.dashboard.continue')}
        </Button>
      ) : null}
      <Text variant="h3">{t('lms.dashboard.courses')}</Text>
      {enrollments.length === 0 ? (
        <View>
          <Banner>{t('lms.dashboard.noCourses')}</Banner>
          <Button variant="secondary" onPress={() => onNavigate({ screenId: 'LMS-01', params: {} })}>
            {t('screen.LMS01.title')}
          </Button>
        </View>
      ) : null}
      {enrollments.map((enrollment) => (
        <View key={enrollment.id}>
          <Text>{t('lms.course.progress') + ': ' + String(percent(enrollment.progressPercent)) + '%'}</Text>
          <Button
            variant="secondary"
            onPress={() => onNavigate({ screenId: 'LMS-05', params: { courseId: enrollment.courseId } })}
          >
            {t('lms.dashboard.openCourse')}
          </Button>
        </View>
      ))}
      {(data.upcomingLiveClasses ?? []).length > 0 ? (
        <Text variant="h3">{t('lms.dashboard.liveClasses')}</Text>
      ) : null}
      {(data.upcomingLiveClasses ?? []).map((liveClass) => (
        <Text key={liveClass.id} muted>
          {t('lms.dashboard.liveClass', { title: liveClass.title, day: format.day(liveClass.classDate) })}
        </Text>
      ))}
    </View>
  );
}

/** LMS-05: the outline with the reader's progress; lessons open the player, SCORM chapters the package (tablet). */
function CourseHome({
  courseId,
  tablet,
  onNavigate,
}: {
  courseId: string;
  tablet: boolean;
  onNavigate: (route: LmsRoute) => void;
}) {
  const { t } = useI18n();
  const outline = useCourseOutline(courseId);
  const state = QueryState({
    pending: outline.isPending,
    error: outline.error,
    retry: () => void outline.refetch(),
  });
  if (state) return state;
  const data = outline.data;
  if (!data) return null;
  return (
    <View>
      <Text variant="h2">{data.course.title}</Text>
      {data.enrollmentId ? (
        <Text muted>{t('lms.course.progress') + ': ' + String(percent(data.progressPercent)) + '%'}</Text>
      ) : (
        <Banner>
          <Text>{t('lms.course.notEnrolled')}</Text>
          <Button
            variant="secondary"
            onPress={() => onNavigate({ screenId: 'LMS-02', params: { courseId } })}
          >
            {t('screen.LMS02.title')}
          </Button>
        </Banner>
      )}
      {(data.chapters ?? []).length === 0 ? <Banner>{t('lms.course.emptyOutline')}</Banner> : null}
      {(data.chapters ?? []).map((chapter) => (
        <View key={chapter.chapterId}>
          <Text variant="h3">{chapter.title}</Text>
          {chapter.isScormPackage ? (
            tablet ? (
              <Button
                variant="secondary"
                onPress={() => onNavigate({ screenId: 'LMS-07', params: { chapterId: chapter.chapterId } })}
              >
                {t('lms.course.openPackage')}
              </Button>
            ) : (
              <Text muted>{t('lms.native.tabletOnly')}</Text>
            )
          ) : (
            chapter.lessons.map((lesson) => (
              <Button
                key={lesson.lessonId}
                variant="ghost"
                onPress={() => onNavigate({ screenId: 'LMS-06', params: { lessonId: lesson.lessonId } })}
              >
                {lesson.title +
                  (lesson.progressStatus ? ' · ' + enumLabel(t, 'lmsProgress', lesson.progressStatus) : '')}
              </Button>
            ))
          )}
        </View>
      ))}
    </View>
  );
}

/** LMS-06: the lesson's blocks, text and video, completing it, its neighbours and the reader's own notes. */
function LessonPlayer({ lessonId, onNavigate }: { lessonId: string; onNavigate: (route: LmsRoute) => void }) {
  const { t } = useI18n();
  const content = useLessonContent(lessonId);
  const complete = useMarkLessonComplete();
  const watch = useTrackVideoWatch();
  const mayComplete = useCan('lms.lesson.mark_lesson_complete');
  const mayTrack = useCan('lms.lesson.track_video_watch');
  const mayReadNotes = useCan('lms.lesson_note.read');
  const [watchedFrom, setWatchedFrom] = useState<number | null>(null);
  const state = QueryState({
    pending: content.isPending,
    error: content.error,
    retry: () => void content.refetch(),
  });
  if (state) return state;
  const data = content.data;
  if (!data) return null;
  const video = data.lesson.youtubeVideoId;
  const go = (id: string) => onNavigate({ screenId: 'LMS-06', params: { lessonId: id } });
  return (
    <View>
      <Text variant="h2">{data.lesson.title}</Text>
      {data.progressStatus ? <Text muted>{enumLabel(t, 'lmsProgress', data.progressStatus)}</Text> : null}
      {!data.canAccess ? <Banner>{t('lms.lesson.locked')}</Banner> : null}
      {video ? (
        <View>
          <Button
            variant="secondary"
            onPress={() => {
              setWatchedFrom(Date.now());
              void Linking.openURL(`https://www.youtube.com/watch?v=${encodeURIComponent(video)}`);
            }}
          >
            {t('lms.lesson.video')}
          </Button>
          {mayTrack && watchedFrom !== null ? (
            <Button
              variant="secondary"
              disabled={watch.isPending}
              onPress={() =>
                watch.mutate({ lessonId, videoSource: video, seconds: (Date.now() - watchedFrom) / 1000 })
              }
            >
              {t('lms.lesson.watched')}
            </Button>
          ) : null}
          <ActionError error={watch.error} />
        </View>
      ) : null}
      <LessonBlocks blocks={parseLessonBlocks(data.lesson.content)} />
      {data.lesson.body ? <Text>{data.lesson.body}</Text> : null}
      {mayComplete && data.canAccess && data.progressStatus !== 'complete' ? (
        <Button disabled={complete.isPending} onPress={() => complete.mutate(lessonId)}>
          {t('lms.lesson.complete')}
        </Button>
      ) : null}
      {data.progressStatus === 'complete' ? (
        <Banner intent="success">{t('lms.lesson.completedBanner')}</Banner>
      ) : null}
      <ActionError error={complete.error} />
      {data.previousLessonId ? (
        <Button variant="secondary" onPress={() => go(data.previousLessonId ?? '')}>
          {t('lms.lesson.previous')}
        </Button>
      ) : null}
      {data.nextLessonId ? (
        <Button variant="secondary" onPress={() => go(data.nextLessonId ?? '')}>
          {t('lms.lesson.next')}
        </Button>
      ) : null}
      {mayReadNotes && data.canAccess ? <LessonNotes lessonId={lessonId} /> : null}
    </View>
  );
}

/** A lesson's blocks as text, lists, files (signed at the tap) and outside links; markup is never interpreted. */
function LessonBlocks({ blocks }: { blocks: readonly LessonBlock[] }) {
  const { t } = useI18n();
  const mayDownload = useCan('platform.file_object.get_download_url');
  const download = useDownloadUrl();
  return (
    <View>
      {blocks.map((block, index) => {
        switch (block.type) {
          case 'paragraph':
            return <Text key={index}>{block.text}</Text>;
          case 'header':
            return (
              <Text key={index} variant={block.level === 3 ? 'h3' : 'h2'}>
                {block.text}
              </Text>
            );
          case 'list':
            return (
              <View key={index}>
                {block.items.map((item, i) => (
                  <Text key={i}>{(block.ordered ? `${i + 1}. ` : '• ') + item}</Text>
                ))}
              </View>
            );
          case 'upload':
            return mayDownload ? (
              <Button
                key={index}
                variant="secondary"
                disabled={download.isPending}
                onPress={() =>
                  download.mutate(block.fileId, { onSuccess: (link) => void Linking.openURL(link.url) })
                }
              >
                {block.name}
              </Button>
            ) : null;
          case 'embed':
            return (
              <Button key={index} variant="ghost" onPress={() => void Linking.openURL(block.url)}>
                {t('lms.blocks.openEmbed', { address: linkHost(block.url) })}
              </Button>
            );
        }
      })}
      <ActionError error={download.error} />
    </View>
  );
}

/** The reader's notes on a lesson: theirs only, written and removed with the note's ETag. */
function LessonNotes({ lessonId }: { lessonId: string }) {
  const { t } = useI18n();
  const userId = useSession((s) => s.me?.user?.id ?? null);
  const notes = useLessonNotes(lessonId, Boolean(userId));
  const { create, remove } = useLessonNoteActions(lessonId);
  const mayWrite = useCan('lms.lesson_note.create');
  const mayDelete = useCan('lms.lesson_note.delete');
  const [note, setNote] = useState('');
  return (
    <View>
      <Text variant="h3">{t('lms.notes.title')}</Text>
      {(notes.data?.items ?? []).length === 0 ? <Text muted>{t('lms.notes.none')}</Text> : null}
      {(notes.data?.items ?? []).map((lessonNote) => (
        <View key={lessonNote.id}>
          <Text muted>{lessonNote.note ?? ''}</Text>
          {mayDelete ? (
            <Button variant="ghost" disabled={remove.isPending} onPress={() => remove.mutate(lessonNote)}>
              {t('common.action.delete')}
            </Button>
          ) : null}
        </View>
      ))}
      {mayWrite ? (
        <View>
          <TextInput label={t('lms.ebook.newNote')} value={note} onChangeText={setNote} multiline />
          <Button
            variant="secondary"
            disabled={!note.trim() || create.isPending}
            onPress={() => create.mutate({ note, color: 'yellow' }, { onSuccess: () => setNote('') })}
          >
            {t('lms.ebook.addNote')}
          </Button>
        </View>
      ) : null}
      <ActionError error={create.error ?? remove.error} />
    </View>
  );
}

/**
 * LMS-07 (tablet): the package opens in the browser (the app embeds no web view); the reader's saved state is shown
 * and their progress is saved or finished from here.
 */
function ScormPlayer({ chapterId }: { chapterId: string }) {
  const { t } = useI18n();
  const launch = useScormLaunch(chapterId);
  const save = useSaveScormProgress(chapterId);
  const maySave = useCan('lms.chapter.save_scorm_progress');
  const state = QueryState({
    pending: launch.isPending,
    error: launch.error,
    retry: () => void launch.refetch(),
  });
  if (state) return state;
  const data = launch.data;
  if (!data) return null;
  const saved = (data.runtimeState ?? {}) as Record<string, unknown>;
  const status = typeof saved.status === 'string' ? saved.status : null;
  return (
    <View>
      {status ? <Banner>{t('lms.scorm.resumed', { status })}</Banner> : null}
      <Button onPress={() => void Linking.openURL(data.launchUrl)}>{t('lms.scorm.open')}</Button>
      {maySave ? (
        <View>
          <Button
            variant="secondary"
            disabled={save.isPending}
            onPress={() => save.mutate({ status: 'partially_complete', runtimeState: saved })}
          >
            {t('lms.scorm.saveProgress')}
          </Button>
          <Button
            disabled={save.isPending}
            onPress={() => save.mutate({ status: 'complete', runtimeState: saved })}
          >
            {t('lms.scorm.finish')}
          </Button>
        </View>
      ) : null}
      <ActionError error={save.error} />
    </View>
  );
}

/** LMS-13 (tablet): the e-book, the reader's highlights and notes on it, and their related materials. */
function EbookReader({ ebookId, contentItemId }: { ebookId: string; contentItemId: string | null }) {
  const { t } = useI18n();
  const ebook = useEbook(ebookId);
  const annotations = useAnnotations(ebookId);
  const materials = useMyRelatedMaterials(ebookId);
  const { create, remove } = useAnnotationActions();
  const view = useRecordItemView();
  const mayWrite = useCan('lms.annotation.create');
  const mayDelete = useCan('lms.annotation.delete');
  const [note, setNote] = useState('');
  const record = view.mutate;
  // an e-book opened from a course counts one view of that course item
  useEffect(() => {
    if (contentItemId) record({ contentItemId });
  }, [contentItemId, record]);
  const state = QueryState({
    pending: ebook.isPending,
    error: ebook.error,
    retry: () => void ebook.refetch(),
  });
  if (state) return state;
  return (
    <View>
      <Text variant="h2">{ebook.data?.title ?? ''}</Text>
      {ebook.data?.description ? <Text>{ebook.data.description}</Text> : null}
      {ebook.data?.fileId ? null : <Banner>{t('lms.ebook.notConverted')}</Banner>}
      <Text variant="h3">{t('lms.ebook.annotations')}</Text>
      {(annotations.data?.items ?? []).length === 0 ? (
        <Text muted>{t('lms.ebook.noAnnotations')}</Text>
      ) : null}
      {(annotations.data?.items ?? []).map((annotation) => (
        <View key={annotation.id}>
          <Text muted>{annotation.annotatedText ?? annotation.description ?? ''}</Text>
          {mayDelete ? (
            <Button variant="ghost" disabled={remove.isPending} onPress={() => remove.mutate(annotation)}>
              {t('common.action.delete')}
            </Button>
          ) : null}
        </View>
      ))}
      {mayWrite ? (
        <View>
          <TextInput label={t('lms.ebook.newNote')} value={note} onChangeText={setNote} multiline />
          <Button
            variant="secondary"
            disabled={!note.trim() || create.isPending}
            onPress={() =>
              create.mutate(
                { ebookId, annotationType: 'note', description: note.trim() },
                { onSuccess: () => setNote('') },
              )
            }
          >
            {t('lms.ebook.addNote')}
          </Button>
        </View>
      ) : null}
      <ActionError error={create.error ?? remove.error} />
      {materials.data?.length ? <Text variant="h3">{t('lms.ebook.related')}</Text> : null}
      {(materials.data ?? []).map((material) => (
        <Button
          key={material.id}
          variant="ghost"
          disabled={!material.url}
          onPress={() => void Linking.openURL(material.url ?? '')}
        >
          {material.title ?? material.url ?? ''}
        </Button>
      ))}
    </View>
  );
}

/** LMS-14: the reader's notes across content and their bookmarks, with save and delete. */
function NotesAndBookmarks() {
  const { t } = useI18n();
  const annotations = useAnnotations();
  const mayBookmarks = useCan('lms.bookmark.read');
  const mayAdd = useCan('lms.bookmark.create');
  const mayDelete = useCan('lms.bookmark.delete');
  const bookmarks = useBookmarks(mayBookmarks);
  const { create, remove } = useBookmarkActions();
  const [draft, setDraft] = useState({ title: '', url: '' });
  const state = QueryState({
    pending: annotations.isPending || (mayBookmarks && bookmarks.isPending),
    error: annotations.error ?? bookmarks.error,
    retry: () => {
      void annotations.refetch();
      if (mayBookmarks) void bookmarks.refetch();
    },
  });
  if (state) return state;
  return (
    <View>
      <Text variant="h3">{t('lms.notes.title')}</Text>
      {(annotations.data?.items ?? []).length === 0 ? <Text muted>{t('lms.notes.none')}</Text> : null}
      {(annotations.data?.items ?? []).map((annotation) => (
        <Text key={annotation.id} muted>
          {annotation.annotatedText ?? annotation.description ?? annotation.title ?? ''}
        </Text>
      ))}
      {mayBookmarks ? <Text variant="h3">{t('lms.bookmarks.title')}</Text> : null}
      {(bookmarks.data?.items ?? []).map((bookmark) => (
        <View key={bookmark.id}>
          <Button variant="ghost" onPress={() => void Linking.openURL(bookmark.url)}>
            {bookmark.title}
          </Button>
          {mayDelete ? (
            <Button variant="secondary" disabled={remove.isPending} onPress={() => remove.mutate(bookmark)}>
              {t('common.action.delete')}
            </Button>
          ) : null}
        </View>
      ))}
      {mayAdd ? (
        <View>
          <TextInput
            label={t('lms.bookmarks.titleField')}
            value={draft.title}
            onChangeText={(title) => setDraft({ ...draft, title })}
          />
          <TextInput
            label={t('lms.bookmarks.url')}
            value={draft.url}
            autoCapitalize="none"
            keyboardType="url"
            onChangeText={(url) => setDraft({ ...draft, url })}
          />
          <Button
            disabled={!draft.title.trim() || !draft.url.trim() || create.isPending}
            onPress={() =>
              create.mutate(
                { title: draft.title.trim(), url: draft.url.trim() },
                { onSuccess: () => setDraft({ title: '', url: '' }) },
              )
            }
          >
            {t('lms.bookmarks.add')}
          </Button>
        </View>
      ) : null}
      <ActionError error={create.error ?? remove.error} />
    </View>
  );
}
