import {
  blocksAreValid,
  COURSE_STEP_PERMISSIONS,
  courseSteps,
  lessonContentOf,
  parseLessonBlocks,
  useAuthoredCourses,
  useCan,
  useCatalogItemActions,
  useCatalogItems,
  useChapterActions,
  useCourseChapters,
  useCourseDependencies,
  useCourseOutline,
  useCoursePage,
  useCourseStep,
  useCreateCourse,
  useCreateCourseVersion,
  useDeleteCourse,
  useDeleteLesson,
  useI18n,
  useLesson,
  useStructureActions,
  useUpdateLesson,
  type CatalogItemKind,
  type CourseStep,
  type LmsChapter,
  type LmsCourse,
  type LmsLesson,
} from '@mysms/core';
import { enumLabel, type MessageKey } from '@mysms/i18n';
import {
  Banner,
  Button,
  Card,
  DataTable,
  Dialog,
  SelectField,
  StatusChip,
  Switch,
  TextArea,
  TextInput,
} from '@mysms/ui-web';
import { useState, type ReactNode } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { FileUploadButton } from '../../files/FileTransfer';
import { LessonBlocksEditor } from './LessonBlocks';
import { ActionError, LmsPage, LmsState } from './lmsUi';

/** The step buttons of one course: only those its state allows and the reader may take. */
function CourseStepButtons({ course }: { course: LmsCourse }) {
  const { t } = useI18n();
  const step = useCourseStep();
  const permitted = {
    submitForReview: useCan(COURSE_STEP_PERMISSIONS.submitForReview),
    approve: useCan(COURSE_STEP_PERMISSIONS.approve),
    publish: useCan(COURSE_STEP_PERMISSIONS.publish),
    unpublish: useCan(COURSE_STEP_PERMISSIONS.unpublish),
    deactivate: useCan(COURSE_STEP_PERMISSIONS.deactivate),
    reactivate: useCan(COURSE_STEP_PERMISSIONS.reactivate),
    publishCatalogListing: useCan(COURSE_STEP_PERMISSIONS.publishCatalogListing),
    hideCatalogListing: useCan(COURSE_STEP_PERMISSIONS.hideCatalogListing),
  } satisfies Record<CourseStep, boolean>;
  const steps = courseSteps(course).filter((s) => permitted[s]);
  return (
    <span className="lms-toolbar">
      {steps.map((s) => (
        <Button
          key={s}
          variant="secondary"
          loading={step.isPending && step.variables?.step === s}
          onClick={() => step.mutate({ course, step: s })}
        >
          {t(`lms.authoring.step.${s}` as MessageKey)}
        </Button>
      ))}
      <ActionError error={step.error} />
    </span>
  );
}

/** Deleting a course: what still holds it is shown first; the delete is offered only when nothing does. */
function DeleteCourseDialog({ course, onClose }: { course: LmsCourse; onClose: () => void }) {
  const { t } = useI18n();
  const dependencies = useCourseDependencies(course.id ?? null);
  const remove = useDeleteCourse();
  const blocking = Object.keys((dependencies.data?.blocking ?? {}) as Record<string, unknown>);
  return (
    <Dialog
      open
      title={t('lms.authoring.deleteTitle', { title: course.title })}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button
            variant="danger"
            disabled={!dependencies.data?.canDelete}
            loading={remove.isPending}
            onClick={() => remove.mutate(course, { onSuccess: onClose })}
          >
            {t('common.action.delete')}
          </Button>
        </>
      }
    >
      {dependencies.isPending ? <p>{t('common.state.loading')}</p> : null}
      {blocking.length > 0 ? (
        <>
          <p>{t('lms.authoring.blocked')}</p>
          <ul>
            {blocking.map((reason) => (
              <li key={reason}>{reason === 'published' ? t('lms.authoring.blockedPublished') : reason}</li>
            ))}
          </ul>
        </>
      ) : null}
      <ActionError error={remove.error ?? dependencies.error} />
    </Dialog>
  );
}

/** A new edition of a course: major or minor, with the author's comments; the copy opens in its own lane. */
function NewVersionDialog({ course, onClose }: { course: LmsCourse; onClose: () => void }) {
  const { t } = useI18n();
  const create = useCreateCourseVersion();
  const [versionKind, setVersionKind] = useState<'major' | 'minor'>('minor');
  const [authorComments, setAuthorComments] = useState('');
  return (
    <Dialog
      open
      title={t('lms.authoring.newVersionTitle', { title: course.title })}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button
            loading={create.isPending}
            onClick={() =>
              create.mutate(
                { courseId: course.id ?? '', versionKind, authorComments: authorComments.trim() },
                { onSuccess: onClose },
              )
            }
          >
            {t('lms.authoring.createVersion')}
          </Button>
        </>
      }
    >
      <div className="form">
        <SelectField
          label={t('lms.authoring.versionKind')}
          value={versionKind}
          options={[
            { value: 'minor', label: t('lms.authoring.versionMinor') },
            { value: 'major', label: t('lms.authoring.versionMajor') },
          ]}
          onChange={(e) => setVersionKind(e.target.value === 'major' ? 'major' : 'minor')}
        />
        <TextArea
          label={t('lms.authoring.authorComments')}
          value={authorComments}
          onChange={(e) => setAuthorComments(e.target.value)}
        />
      </div>
      <ActionError error={create.error} />
    </Dialog>
  );
}

/** A course's version as authors read it: major.minor. */
const versionOf = (course: LmsCourse) => `${course.versionMajor ?? 1}.${course.versionMinor ?? 0}`;

/**
 * LMS-40 My courses: the author's courses in version lanes (each edition beside the one it came from) with their
 * review, lifecycle and catalogue states, their steps, a new edition and the guarded delete.
 */
export function MyCoursesScreen() {
  const { t } = useI18n();
  const allowed = useCan('lms.course.read');
  const mayCreate = useCan('lms.course.create');
  const mayDelete = useCan('lms.course.delete');
  const mayVersion = useCan('lms.course.create_course_version');
  const courses = useAuthoredCourses();
  const create = useCreateCourse();
  const [title, setTitle] = useState('');
  const [deleting, setDeleting] = useState<LmsCourse | null>(null);
  const [versioning, setVersioning] = useState<LmsCourse | null>(null);
  // a lane is every edition of one course (same root), newest version first
  const lane = (course: LmsCourse) => course.rootCourseId ?? course.id ?? '';
  const rows = [...(courses.data?.items ?? [])].sort(
    (a, b) =>
      lane(a).localeCompare(lane(b)) ||
      (b.versionMajor ?? 1) - (a.versionMajor ?? 1) ||
      (b.versionMinor ?? 0) - (a.versionMinor ?? 0),
  );
  const screenTitle = t('screen.LMS40.title');
  const state = LmsState({
    title: screenTitle,
    allowed,
    loading: allowed && courses.isPending,
    error: courses.error,
    retry: () => void courses.refetch(),
  });
  if (state) return state;
  return (
    <LmsPage title={screenTitle}>
      {mayCreate ? (
        <Card title={t('lms.authoring.newCourse')}>
          <div className="form">
            <TextInput
              label={t('lms.authoring.courseTitle')}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
            <Button
              disabled={!title.trim()}
              loading={create.isPending}
              onClick={() => create.mutate({ title: title.trim() }, { onSuccess: () => setTitle('') })}
            >
              {t('lms.authoring.create')}
            </Button>
            <ActionError error={create.error} />
          </div>
        </Card>
      ) : null}
      <DataTable<LmsCourse>
        caption={screenTitle}
        rows={courses.data ? rows : undefined}
        rowKey={(course) => course.id ?? course.title}
        loadingLabel={t('common.state.loading')}
        emptyText={t('lms.authoring.noCourses')}
        columns={[
          {
            key: 'title',
            header: t('lms.authoring.courseTitle'),
            render: (course) => <Link to={`/app/lms/courses/${course.id ?? ''}/edit`}>{course.title}</Link>,
          },
          {
            key: 'version',
            header: t('lms.authoring.version'),
            render: (course) => versionOf(course),
          },
          {
            key: 'state',
            header: t('lms.authoring.state'),
            render: (course) => (
              <>
                <StatusChip label={enumLabel(t, 'lmsCourseReview', course.status ?? 'in_progress')} />{' '}
                <StatusChip
                  intent={course.lifecycleStatus === 'published' ? 'success' : 'neutral'}
                  label={enumLabel(t, 'lmsCourseLifecycle', course.lifecycleStatus ?? 'draft')}
                />
              </>
            ),
          },
          {
            key: 'steps',
            header: t('lms.authoring.steps'),
            render: (course) => <CourseStepButtons course={course} />,
          },
          {
            key: 'delete',
            header: '',
            render: (course) => (
              <span className="lms-toolbar">
                {mayVersion ? (
                  <Button variant="ghost" onClick={() => setVersioning(course)}>
                    {t('lms.authoring.newVersion')}
                  </Button>
                ) : null}
                {mayDelete ? (
                  <Button variant="ghost" onClick={() => setDeleting(course)}>
                    {t('lms.authoring.deleteCourse')}
                  </Button>
                ) : null}
              </span>
            ),
          },
        ]}
      />
      {deleting ? <DeleteCourseDialog course={deleting} onClose={() => setDeleting(null)} /> : null}
      {versioning ? <NewVersionDialog course={versioning} onClose={() => setVersioning(null)} /> : null}
    </LmsPage>
  );
}

/**
 * LMS-41 Course structure editor: the course's chapters as a tree with their lessons. Chapters are renamed, moved up
 * and down among their siblings, nested under the chapter above or lifted out, and deleted; lessons move within and
 * between chapters; new chapters and lessons are added at the end.
 */
export function StructureEditorScreen() {
  const { courseId = '' } = useParams();
  const { t } = useI18n();
  const allowed = useCan('lms.course.read');
  const mayReorder = useCan('lms.course.reorder_chapters');
  const mayMoveChapter = useCan('lms.chapter.move_chapter');
  const mayRename = useCan('lms.chapter.update');
  const mayDeleteChapter = useCan('lms.chapter.delete');
  const mayMoveLesson = useCan('lms.lesson.move_lesson');
  const mayAddChapter = useCan('lms.chapter.create');
  const mayAddLesson = useCan('lms.lesson.create');
  const outline = useCourseOutline(courseId);
  const rows = useCourseChapters(courseId);
  const { reorderChapters, moveLesson, addChapter, addLesson } = useStructureActions(courseId);
  const chapterActions = useChapterActions(courseId);
  const [chapterTitle, setChapterTitle] = useState('');
  const [lessonTitles, setLessonTitles] = useState<Record<string, string>>({});
  const [renaming, setRenaming] = useState<{ chapter: LmsChapter; title: string } | null>(null);
  const [deleting, setDeleting] = useState<LmsChapter | null>(null);
  const title = outline.data?.course.title ?? t('screen.LMS41.title');
  const state = LmsState({
    title,
    allowed,
    loading: allowed && (outline.isPending || rows.isPending),
    error: outline.error ?? rows.error,
    retry: () => {
      void outline.refetch();
      void rows.refetch();
    },
  });
  if (state) return state;
  const chapters = outline.data?.chapters ?? [];
  const byId = new Map((rows.data?.items ?? []).map((row) => [row.id ?? '', row]));
  const parentOf = (chapterId: string) => byId.get(chapterId)?.parentChapterId ?? null;
  const depthOf = (chapterId: string) => {
    let depth = 0;
    for (let at = parentOf(chapterId); at && depth < chapters.length; at = parentOf(at)) depth++;
    return depth;
  };
  // the outline lists the tree depth-first, so siblings keep their order when filtered by parent
  const siblingsOf = (chapterId: string) =>
    chapters.map((c) => c.chapterId).filter((id) => parentOf(id) === parentOf(chapterId));
  const shift = (chapterId: string, by: -1 | 1) => {
    const siblings = siblingsOf(chapterId);
    const at = siblings.indexOf(chapterId);
    const parent = parentOf(chapterId);
    if (parent === null) {
      const next = [...siblings];
      [next[at], next[at + by]] = [next[at + by] ?? '', next[at] ?? ''];
      reorderChapters.mutate(next);
    } else {
      chapterActions.move.mutate({ chapterId, parentChapterId: parent, position: at + by });
    }
  };
  const indent = (chapterId: string) => {
    const siblings = siblingsOf(chapterId);
    const above = siblings[siblings.indexOf(chapterId) - 1];
    if (!above) return;
    chapterActions.move.mutate({ chapterId, parentChapterId: above, position: siblingsOf(above).length });
  };
  const outdent = (chapterId: string) => {
    const parent = parentOf(chapterId);
    if (!parent) return;
    chapterActions.move.mutate({
      chapterId,
      parentChapterId: parentOf(parent),
      position: siblingsOf(parent).indexOf(parent) + 1,
    });
  };
  return (
    <LmsPage title={title} subtitle={t('screen.LMS41.title')}>
      <ActionError
        error={
          reorderChapters.error ??
          moveLesson.error ??
          addChapter.error ??
          addLesson.error ??
          chapterActions.move.error ??
          chapterActions.rename.error
        }
      />
      {chapters.length === 0 ? <Banner intent="info">{t('lms.course.emptyOutline')}</Banner> : null}
      {chapters.map((chapter, index) => {
        const row = byId.get(chapter.chapterId);
        const siblings = siblingsOf(chapter.chapterId);
        const at = siblings.indexOf(chapter.chapterId);
        const nested = parentOf(chapter.chapterId) !== null;
        const mayShift = nested ? mayMoveChapter : mayReorder;
        return (
          <Card
            key={chapter.chapterId}
            style={{ marginInlineStart: `calc(var(--sms-space-6) * ${depthOf(chapter.chapterId)})` }}
            title={chapter.title}
            actions={
              <>
                {mayShift ? (
                  <>
                    <Button variant="ghost" disabled={at <= 0} onClick={() => shift(chapter.chapterId, -1)}>
                      {t('lms.structure.up')}
                    </Button>
                    <Button
                      variant="ghost"
                      disabled={at === siblings.length - 1}
                      onClick={() => shift(chapter.chapterId, 1)}
                    >
                      {t('lms.structure.down')}
                    </Button>
                  </>
                ) : null}
                {mayMoveChapter && at > 0 ? (
                  <Button variant="ghost" onClick={() => indent(chapter.chapterId)}>
                    {t('lms.structure.indent')}
                  </Button>
                ) : null}
                {mayMoveChapter && nested ? (
                  <Button variant="ghost" onClick={() => outdent(chapter.chapterId)}>
                    {t('lms.structure.outdent')}
                  </Button>
                ) : null}
                {mayRename && row ? (
                  <Button variant="ghost" onClick={() => setRenaming({ chapter: row, title: row.title })}>
                    {t('lms.structure.rename')}
                  </Button>
                ) : null}
                {mayDeleteChapter && row ? (
                  <Button variant="ghost" onClick={() => setDeleting(row)}>
                    {t('lms.structure.deleteChapter')}
                  </Button>
                ) : null}
              </>
            }
          >
            <ul>
              {chapter.lessons.map((lesson, position) => (
                <li key={lesson.lessonId} className="lms-outline__lesson">
                  <Link to={`/app/lms/lessons/${lesson.lessonId}/edit`}>{lesson.title}</Link>
                  {mayMoveLesson && position > 0 ? (
                    <Button
                      variant="ghost"
                      onClick={() =>
                        moveLesson.mutate({
                          lessonId: lesson.lessonId,
                          targetChapterId: chapter.chapterId,
                          position: position - 1,
                        })
                      }
                    >
                      {t('lms.structure.up')}
                    </Button>
                  ) : null}
                  {mayMoveLesson && index + 1 < chapters.length ? (
                    <Button
                      variant="ghost"
                      onClick={() =>
                        moveLesson.mutate({
                          lessonId: lesson.lessonId,
                          targetChapterId: chapters[index + 1]?.chapterId ?? chapter.chapterId,
                          position: 0,
                        })
                      }
                    >
                      {t('lms.structure.toNextChapter')}
                    </Button>
                  ) : null}
                </li>
              ))}
            </ul>
            {mayAddLesson ? (
              <div className="lms-toolbar">
                <TextInput
                  label={t('lms.structure.lessonTitle')}
                  value={lessonTitles[chapter.chapterId] ?? ''}
                  onChange={(e) => setLessonTitles({ ...lessonTitles, [chapter.chapterId]: e.target.value })}
                />
                <Button
                  disabled={!(lessonTitles[chapter.chapterId] ?? '').trim()}
                  loading={addLesson.isPending}
                  onClick={() =>
                    addLesson.mutate(
                      {
                        chapterId: chapter.chapterId,
                        title: lessonTitles[chapter.chapterId] ?? '',
                        sortOrder: chapter.lessons.length,
                      },
                      { onSuccess: () => setLessonTitles({ ...lessonTitles, [chapter.chapterId]: '' }) },
                    )
                  }
                >
                  {t('lms.structure.addLesson')}
                </Button>
              </div>
            ) : null}
          </Card>
        );
      })}
      {mayAddChapter ? (
        <Card title={t('lms.structure.newChapter')}>
          <div className="lms-toolbar">
            <TextInput
              label={t('lms.structure.chapterTitle')}
              value={chapterTitle}
              onChange={(e) => setChapterTitle(e.target.value)}
            />
            <Button
              disabled={!chapterTitle.trim()}
              loading={addChapter.isPending}
              onClick={() =>
                addChapter.mutate(
                  { title: chapterTitle, sortOrder: chapters.filter((c) => !parentOf(c.chapterId)).length },
                  { onSuccess: () => setChapterTitle('') },
                )
              }
            >
              {t('lms.structure.addChapter')}
            </Button>
          </div>
        </Card>
      ) : null}
      {renaming ? (
        <Dialog
          open
          title={t('lms.structure.rename')}
          onClose={() => setRenaming(null)}
          footer={
            <>
              <Button variant="secondary" onClick={() => setRenaming(null)}>
                {t('common.action.cancel')}
              </Button>
              <Button
                disabled={!renaming.title.trim()}
                loading={chapterActions.rename.isPending}
                onClick={() =>
                  chapterActions.rename.mutate(
                    { chapter: renaming.chapter, title: renaming.title },
                    { onSuccess: () => setRenaming(null) },
                  )
                }
              >
                {t('common.action.save')}
              </Button>
            </>
          }
        >
          <TextInput
            label={t('lms.structure.chapterTitle')}
            value={renaming.title}
            onChange={(e) => setRenaming({ ...renaming, title: e.target.value })}
          />
          <ActionError error={chapterActions.rename.error} />
        </Dialog>
      ) : null}
      {deleting ? (
        <Dialog
          open
          title={t('lms.structure.deleteChapterTitle', { title: deleting.title })}
          onClose={() => setDeleting(null)}
          footer={
            <>
              <Button variant="secondary" onClick={() => setDeleting(null)}>
                {t('common.action.cancel')}
              </Button>
              <Button
                variant="danger"
                loading={chapterActions.remove.isPending}
                onClick={() => chapterActions.remove.mutate(deleting, { onSuccess: () => setDeleting(null) })}
              >
                {t('lms.structure.deleteChapter')}
              </Button>
            </>
          }
        >
          <p>{t('lms.structure.deleteChapterBody')}</p>
          <ActionError error={chapterActions.remove.error} />
        </Dialog>
      ) : null}
    </LmsPage>
  );
}

/**
 * The lesson form: title, content blocks, the plain text and video of older lessons, preview flag and the teacher's
 * notes, saved under the lesson's etag; a draft lesson can be deleted.
 */
function LessonForm({ lesson }: { lesson: LmsLesson }) {
  const { t } = useI18n();
  const mayUpdate = useCan('lms.lesson.update');
  const mayDelete = useCan('lms.lesson.delete');
  const update = useUpdateLesson();
  const remove = useDeleteLesson();
  const navigate = useNavigate();
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [blocks, setBlocks] = useState(() => parseLessonBlocks(lesson.content));
  const [draft, setDraft] = useState({
    title: lesson.title,
    body: lesson.body ?? '',
    youtubeVideoId: lesson.youtubeVideoId ?? '',
    teacherNotes: lesson.teacherNotes ?? '',
    includeInPreview: lesson.includeInPreview ?? false,
  });
  const published = Boolean(lesson.publishedAt);
  return (
    <Card>
      {published ? <Banner intent="info">{t('lms.lessonEditor.published')}</Banner> : null}
      <div className="form">
        <TextInput
          label={t('lms.lessonEditor.title')}
          value={draft.title}
          disabled={!mayUpdate || published}
          onChange={(e) => setDraft({ ...draft, title: e.target.value })}
        />
        <LessonBlocksEditor
          blocks={blocks}
          lessonId={lesson.id ?? ''}
          disabled={!mayUpdate || published}
          onChange={setBlocks}
        />
        <TextArea
          label={t('lms.lessonEditor.body')}
          rows={12}
          value={draft.body}
          disabled={!mayUpdate || published}
          onChange={(e) => setDraft({ ...draft, body: e.target.value })}
        />
        <TextInput
          label={t('lms.lessonEditor.video')}
          value={draft.youtubeVideoId}
          maxLength={60}
          disabled={!mayUpdate || published}
          onChange={(e) => setDraft({ ...draft, youtubeVideoId: e.target.value })}
        />
        <TextArea
          label={t('lms.lessonEditor.teacherNotes')}
          value={draft.teacherNotes}
          disabled={!mayUpdate || published}
          onChange={(e) => setDraft({ ...draft, teacherNotes: e.target.value })}
        />
        <Switch
          label={t('lms.lessonEditor.preview')}
          checked={draft.includeInPreview}
          disabled={!mayUpdate || published}
          onChange={(checked) => setDraft({ ...draft, includeInPreview: checked })}
        />
        {mayUpdate && !published ? (
          <Button
            disabled={!draft.title.trim() || !blocksAreValid(blocks)}
            loading={update.isPending}
            onClick={() =>
              update.mutate({
                lesson,
                changes: {
                  title: draft.title.trim(),
                  content: lessonContentOf(blocks),
                  body: draft.body,
                  youtubeVideoId: draft.youtubeVideoId.trim() || undefined,
                  teacherNotes: draft.teacherNotes,
                  includeInPreview: draft.includeInPreview,
                },
              })
            }
          >
            {t('common.action.save')}
          </Button>
        ) : null}
        {mayDelete && !published ? (
          <Button variant="danger" onClick={() => setConfirmDelete(true)}>
            {t('lms.lessonEditor.delete')}
          </Button>
        ) : null}
        <ActionError error={update.error} />
      </div>
      {confirmDelete ? (
        <Dialog
          open
          title={t('lms.lessonEditor.deleteTitle', { title: lesson.title })}
          onClose={() => setConfirmDelete(false)}
          footer={
            <>
              <Button variant="secondary" onClick={() => setConfirmDelete(false)}>
                {t('common.action.cancel')}
              </Button>
              <Button
                variant="danger"
                loading={remove.isPending}
                onClick={() => remove.mutate(lesson, { onSuccess: () => void navigate(-1) })}
              >
                {t('lms.lessonEditor.delete')}
              </Button>
            </>
          }
        >
          <p>{t('lms.lessonEditor.deleteBody')}</p>
          <ActionError error={remove.error} />
        </Dialog>
      ) : null}
    </Card>
  );
}

/** LMS-42 Lesson editor: the lesson's text, video, preview flag and teacher notes. */
export function LessonEditorScreen() {
  const { lessonId = '' } = useParams();
  const { t } = useI18n();
  const allowed = useCan('lms.lesson.read');
  const lesson = useLesson(lessonId);
  const title = lesson.data?.title ?? t('screen.LMS42.title');
  const state = LmsState({
    title,
    allowed,
    loading: allowed && lesson.isPending,
    error: lesson.error,
    retry: () => void lesson.refetch(),
  });
  if (state) return state;
  if (!lesson.data) return null;
  return (
    <LmsPage
      title={title}
      actions={<Link to={`/app/lms/lessons/${lessonId}`}>{t('lms.lessonEditor.view')}</Link>}
    >
      <LessonForm key={lesson.data.etag} lesson={lesson.data} />
    </LmsPage>
  );
}

/** One list of the catalogue page with each item's delete; the item's form comes below it. */
function CatalogItemList<T extends { id?: string; etag?: string }>({
  title,
  kind,
  items,
  label,
  mayDelete,
  onDelete,
  children,
}: {
  title: string;
  kind: CatalogItemKind;
  items: readonly T[];
  label: (item: T) => string;
  mayDelete: boolean;
  onDelete: (kind: CatalogItemKind, item: T) => void;
  children: ReactNode;
}) {
  const { t } = useI18n();
  return (
    <Card title={title}>
      {items.length === 0 ? <p>{t('lms.catalogEditor.none')}</p> : null}
      <ul>
        {items.map((item) => (
          <li key={item.id} className="lms-outline__lesson">
            {label(item)}
            {mayDelete ? (
              <Button variant="ghost" onClick={() => onDelete(kind, item)}>
                {t('lms.catalogEditor.remove')}
              </Button>
            ) : null}
          </li>
        ))}
      </ul>
      {children}
    </Card>
  );
}

/**
 * LMS-50 Catalogue page editor: what the course's public page shows — previews, authors, testimonials and the
 * catalogue banners — added and removed, and the listing put into the catalogue or taken out.
 */
export function CatalogPageEditorScreen() {
  const { courseId = '' } = useParams();
  const { t } = useI18n();
  const allowed = useCan('lms.course.read');
  const may = {
    preview: { add: useCan('lms.course_preview.create'), remove: useCan('lms.course_preview.delete') },
    author: {
      add: useCan('lms.course_author_profile.create'),
      remove: useCan('lms.course_author_profile.delete'),
    },
    testimonial: {
      add: useCan('lms.course_testimonial.create'),
      remove: useCan('lms.course_testimonial.delete'),
    },
    banner: { add: useCan('lms.catalog_banner.create'), remove: useCan('lms.catalog_banner.delete') },
  } satisfies Record<CatalogItemKind, { add: boolean; remove: boolean }>;
  const page = useCoursePage(courseId);
  const items = useCatalogItems(courseId);
  const actions = useCatalogItemActions(courseId);
  const [preview, setPreview] = useState({ title: '', previewKind: 'material' as 'material' | 'video' });
  const [author, setAuthor] = useState({ name: '', qualification: '', bio: '' });
  const [testimonial, setTestimonial] = useState({ title: '', quote: '', currentPosition: '' });
  const [placement, setPlacement] = useState<'featured' | 'showcase'>('featured');
  const title = page.data?.course.title ?? t('screen.LMS50.title');
  const state = LmsState({
    title,
    allowed,
    loading: allowed && (page.isPending || items.isPending),
    error: page.error ?? items.error,
    retry: () => {
      void page.refetch();
      void items.refetch();
    },
  });
  if (state) return state;
  const data = items.data;
  if (!page.data || !data) return null;
  const remove = (kind: CatalogItemKind, item: { id?: string; etag?: string }) =>
    actions.remove.mutate({ kind, item });
  return (
    <LmsPage
      title={title}
      subtitle={enumLabel(t, 'lmsCatalogStatus', page.data.course.catalogStatus ?? 'draft')}
      actions={<CourseStepButtons course={page.data.course} />}
    >
      <ActionError
        error={
          actions.remove.error ??
          actions.addPreview.error ??
          actions.addAuthor.error ??
          actions.addTestimonial.error ??
          actions.addBanner.error
        }
      />
      <CatalogItemList
        title={t('lms.catalogEditor.previews')}
        kind="preview"
        items={data.previews}
        label={(item) => item.title ?? enumLabel(t, 'lmsPreviewKind', item.previewKind)}
        mayDelete={may.preview.remove}
        onDelete={remove}
      >
        {may.preview.add ? (
          <div className="lms-toolbar">
            <TextInput
              label={t('lms.catalogEditor.previewTitle')}
              value={preview.title}
              onChange={(e) => setPreview({ ...preview, title: e.target.value })}
            />
            <SelectField
              label={t('lms.catalogEditor.previewKindLabel')}
              value={preview.previewKind}
              options={[
                { value: 'material', label: t('enum.lmsPreviewKind.material') },
                { value: 'video', label: t('enum.lmsPreviewKind.video') },
              ]}
              onChange={(e) =>
                setPreview({ ...preview, previewKind: e.target.value === 'video' ? 'video' : 'material' })
              }
            />
            <FileUploadButton
              label={t('lms.catalogEditor.uploadPreview')}
              owner={{ ownerEntityType: 'lms.course', ownerEntityId: courseId }}
              onUploaded={(file) =>
                actions.addPreview.mutate(
                  {
                    fileId: file.id ?? '',
                    previewKind: preview.previewKind,
                    title: preview.title.trim() || file.fileName,
                    sortOrder: data.previews.length,
                  },
                  { onSuccess: () => setPreview({ ...preview, title: '' }) },
                )
              }
            />
          </div>
        ) : null}
      </CatalogItemList>
      <CatalogItemList
        title={t('lms.course.authors')}
        kind="author"
        items={data.authors}
        label={(item) => item.name + (item.qualification ? ` · ${item.qualification}` : '')}
        mayDelete={may.author.remove}
        onDelete={remove}
      >
        {may.author.add ? (
          <div className="form">
            <TextInput
              label={t('lms.catalogEditor.authorName')}
              value={author.name}
              onChange={(e) => setAuthor({ ...author, name: e.target.value })}
            />
            <TextInput
              label={t('lms.catalogEditor.qualification')}
              value={author.qualification}
              onChange={(e) => setAuthor({ ...author, qualification: e.target.value })}
            />
            <TextArea
              label={t('lms.catalogEditor.bio')}
              value={author.bio}
              onChange={(e) => setAuthor({ ...author, bio: e.target.value })}
            />
            <Button
              disabled={!author.name.trim()}
              loading={actions.addAuthor.isPending}
              onClick={() =>
                actions.addAuthor.mutate(
                  {
                    name: author.name.trim(),
                    sortOrder: data.authors.length,
                    ...(author.qualification.trim() ? { qualification: author.qualification.trim() } : {}),
                    ...(author.bio.trim() ? { bio: author.bio.trim() } : {}),
                  },
                  { onSuccess: () => setAuthor({ name: '', qualification: '', bio: '' }) },
                )
              }
            >
              {t('lms.catalogEditor.addAuthor')}
            </Button>
          </div>
        ) : null}
      </CatalogItemList>
      <CatalogItemList
        title={t('lms.course.testimonials')}
        kind="testimonial"
        items={data.testimonials}
        label={(item) => [item.title, item.quote].filter(Boolean).join(': ')}
        mayDelete={may.testimonial.remove}
        onDelete={remove}
      >
        {may.testimonial.add ? (
          <div className="form">
            <TextInput
              label={t('lms.catalogEditor.testimonialBy')}
              value={testimonial.title}
              onChange={(e) => setTestimonial({ ...testimonial, title: e.target.value })}
            />
            <TextInput
              label={t('lms.catalogEditor.position')}
              value={testimonial.currentPosition}
              onChange={(e) => setTestimonial({ ...testimonial, currentPosition: e.target.value })}
            />
            <TextArea
              label={t('lms.catalogEditor.quote')}
              value={testimonial.quote}
              onChange={(e) => setTestimonial({ ...testimonial, quote: e.target.value })}
            />
            <Button
              disabled={!testimonial.quote.trim()}
              loading={actions.addTestimonial.isPending}
              onClick={() =>
                actions.addTestimonial.mutate(
                  {
                    quote: testimonial.quote.trim(),
                    sortOrder: data.testimonials.length,
                    ...(testimonial.title.trim() ? { title: testimonial.title.trim() } : {}),
                    ...(testimonial.currentPosition.trim()
                      ? { currentPosition: testimonial.currentPosition.trim() }
                      : {}),
                  },
                  { onSuccess: () => setTestimonial({ title: '', quote: '', currentPosition: '' }) },
                )
              }
            >
              {t('lms.catalogEditor.addTestimonial')}
            </Button>
          </div>
        ) : null}
      </CatalogItemList>
      <CatalogItemList
        title={t('lms.catalogEditor.banners')}
        kind="banner"
        items={data.banners}
        label={(item) => enumLabel(t, 'lmsBannerPlacement', item.placement)}
        mayDelete={may.banner.remove}
        onDelete={remove}
      >
        {may.banner.add ? (
          <div className="lms-toolbar">
            <SelectField
              label={t('lms.catalogEditor.placementLabel')}
              value={placement}
              options={[
                { value: 'featured', label: t('enum.lmsBannerPlacement.featured') },
                { value: 'showcase', label: t('enum.lmsBannerPlacement.showcase') },
              ]}
              onChange={(e) => setPlacement(e.target.value === 'showcase' ? 'showcase' : 'featured')}
            />
            <FileUploadButton
              label={t('lms.catalogEditor.uploadBanner')}
              accept="image/png,image/jpeg,image/webp"
              owner={{ ownerEntityType: 'lms.course', ownerEntityId: courseId }}
              onUploaded={(file) =>
                actions.addBanner.mutate({
                  placement,
                  imageFileId: file.id ?? '',
                  sortOrder: data.banners.length,
                })
              }
            />
          </div>
        ) : null}
      </CatalogItemList>
      <Link to={`/app/lms/courses/${courseId}/catalog`}>{t('lms.catalogEditor.viewPublic')}</Link>
    </LmsPage>
  );
}
