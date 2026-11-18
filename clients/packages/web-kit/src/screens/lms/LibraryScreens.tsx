import {
  useAddFromRepository,
  useAnnotationActions,
  useAnnotations,
  useBookmarkActions,
  useBookmarks,
  useCan,
  useEbook,
  useEbookActions,
  useEbooks,
  useI18n,
  useLmsSettings,
  useMyRelatedMaterials,
  useRecordItemView,
  useRepositoryItems,
  useSettingsSidebarItems,
  type LmsSettings,
  type LmsSettingsUpdate,
  useUpdateLmsSettings,
  useWebResourceActions,
  useWebResources,
  type RepositoryKind,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Banner, Button, Card, SelectField, StatusChip, Switch, TextArea, TextInput } from '@mysms/ui-web';
import { useState } from 'react';
import { useParams, useSearchParams } from 'react-router';
import { ActionError, LmsPage, LmsState } from './lmsUi';

/**
 * LMS-13 E-book reader: the e-book, the reader's highlights and notes on it, and the curated related materials
 * (absent until their curation ships). Opening the reader counts a view when the address names the placement.
 */
export function EbookReaderScreen() {
  const { t } = useI18n();
  const { ebookId = '' } = useParams();
  const [search] = useSearchParams();
  const ebook = useEbook(ebookId);
  const annotations = useAnnotations(ebookId);
  const materials = useMyRelatedMaterials(ebookId);
  const { create, remove } = useAnnotationActions();
  const view = useRecordItemView();
  const [note, setNote] = useState('');
  const [viewCounted, setViewCounted] = useState(false);
  const contentItemId = search.get('item');
  if (contentItemId && !viewCounted) {
    setViewCounted(true);
    view.mutate({ contentItemId });
  }
  const title = ebook.data?.title ?? t('screen.LMS13.title');
  const state = LmsState({
    title,
    allowed: true,
    loading: ebook.isPending,
    error: ebook.error,
    retry: () => void ebook.refetch(),
  });
  if (state) return state;
  return (
    <LmsPage title={title} subtitle={ebook.data?.description ?? undefined}>
      {ebook.data?.fileId ? null : <Banner intent="info">{t('lms.ebook.notConverted')}</Banner>}
      <Card title={t('lms.ebook.annotations')}>
        {(annotations.data?.items ?? []).length === 0 ? <p>{t('lms.ebook.noAnnotations')}</p> : null}
        <ul>
          {(annotations.data?.items ?? []).map((annotation) => (
            <li key={annotation.id}>
              <StatusChip label={annotation.annotationType} />{' '}
              {annotation.annotatedText ?? annotation.description ?? ''}
              <Button variant="ghost" onClick={() => remove.mutate(annotation)}>
                {t('common.action.delete')}
              </Button>
            </li>
          ))}
        </ul>
        <div className="form">
          <TextArea label={t('lms.ebook.newNote')} value={note} onChange={(e) => setNote(e.target.value)} />
          <Button
            disabled={!note.trim() || create.isPending}
            onClick={() =>
              create.mutate(
                { ebookId, annotationType: 'note', description: note.trim() },
                { onSuccess: () => setNote('') },
              )
            }
          >
            {t('lms.ebook.addNote')}
          </Button>
          <ActionError error={create.error ?? remove.error} />
        </div>
      </Card>
      {materials.data?.length ? (
        <Card title={t('lms.ebook.related')}>
          <ul>
            {materials.data.map((material) => (
              <li key={material.id}>
                <a href={material.url ?? '#'} target="_blank" rel="noreferrer">
                  {material.title ?? material.url ?? ''}
                </a>
              </li>
            ))}
          </ul>
        </Card>
      ) : null}
    </LmsPage>
  );
}

/** LMS-14 My notes & bookmarks: the reader's annotations across content, and their saved links. */
export function NotesBookmarksScreen() {
  const { t } = useI18n();
  const annotations = useAnnotations();
  const bookmarks = useBookmarks();
  const { create, remove } = useBookmarkActions();
  const [draft, setDraft] = useState({ title: '', url: '' });
  const title = t('screen.LMS14.title');
  const state = LmsState({
    title,
    allowed: true,
    loading: annotations.isPending || bookmarks.isPending,
    error: annotations.error ?? bookmarks.error,
    retry: () => {
      void annotations.refetch();
      void bookmarks.refetch();
    },
  });
  if (state) return state;
  return (
    <LmsPage title={title}>
      <Card title={t('lms.notes.title')}>
        {(annotations.data?.items ?? []).length === 0 ? <p>{t('lms.notes.none')}</p> : null}
        <ul>
          {(annotations.data?.items ?? []).map((annotation) => (
            <li key={annotation.id}>
              <StatusChip label={annotation.annotationType} />{' '}
              {annotation.annotatedText ?? annotation.description ?? annotation.title ?? ''}
            </li>
          ))}
        </ul>
      </Card>
      <Card title={t('lms.bookmarks.title')}>
        <ul>
          {(bookmarks.data?.items ?? []).map((bookmark) => (
            <li key={bookmark.id}>
              <a href={bookmark.url} target="_blank" rel="noreferrer">
                {bookmark.title}
              </a>
              <Button variant="ghost" onClick={() => remove.mutate(bookmark)}>
                {t('common.action.delete')}
              </Button>
            </li>
          ))}
        </ul>
        <div className="form">
          <TextInput
            label={t('lms.bookmarks.titleField')}
            value={draft.title}
            onChange={(e) => setDraft({ ...draft, title: e.target.value })}
          />
          <TextInput
            label={t('lms.bookmarks.url')}
            value={draft.url}
            onChange={(e) => setDraft({ ...draft, url: e.target.value })}
          />
          <Button
            disabled={!draft.title.trim() || !draft.url.trim() || create.isPending}
            onClick={() =>
              create.mutate(
                { title: draft.title.trim(), url: draft.url.trim() },
                { onSuccess: () => setDraft({ title: '', url: '' }) },
              )
            }
          >
            {t('lms.bookmarks.add')}
          </Button>
          <ActionError error={create.error ?? remove.error} />
        </div>
      </Card>
    </LmsPage>
  );
}

const REPOSITORY_KINDS: Record<RepositoryKind, MessageKey> = {
  quiz: 'lms.repository.kind.quiz',
  assignment: 'lms.repository.kind.assignment',
  discussion: 'lms.repository.kind.discussion',
  participation: 'lms.repository.kind.participation',
  web_resource: 'lms.repository.kind.webResource',
  ebook: 'lms.repository.kind.ebook',
};

/**
 * LMS-47 Repository: the shared items of each kind, picked and placed into a chapter by its id. The chapter comes
 * from the structure editor's address bar; the server refuses one the author may not write.
 */
export function RepositoryScreen() {
  const { t } = useI18n();
  const [kind, setKind] = useState<RepositoryKind>('quiz');
  const items = useRepositoryItems(kind);
  const place = useAddFromRepository();
  const canPlace = useCan('lms.content_item.add_from_repository');
  const [chapterId, setChapterId] = useState('');
  const [picked, setPicked] = useState<string[]>([]);
  const title = t('screen.LMS47.title');
  const state = LmsState({
    title,
    allowed: true,
    loading: items.isPending,
    error: items.error,
    retry: () => void items.refetch(),
  });
  if (state) return state;
  return (
    <LmsPage title={title} subtitle={t('lms.repository.subtitle')}>
      <Card>
        <SelectField
          label={t('lms.repository.kind')}
          value={kind}
          options={(Object.keys(REPOSITORY_KINDS) as RepositoryKind[]).map((value) => ({
            value,
            label: t(REPOSITORY_KINDS[value]),
          }))}
          onChange={(event) => {
            setKind(event.target.value as RepositoryKind);
            setPicked([]);
          }}
        />
        {(items.data ?? []).length === 0 ? <p>{t('lms.repository.none')}</p> : null}
        <ul>
          {(items.data ?? []).map((item) => (
            <li key={item.id}>
              <label>
                <input
                  type="checkbox"
                  checked={picked.includes(item.id)}
                  onChange={(event) =>
                    setPicked(
                      event.target.checked ? [...picked, item.id] : picked.filter((id) => id !== item.id),
                    )
                  }
                />{' '}
                {item.title}
              </label>
            </li>
          ))}
        </ul>
        {canPlace ? (
          <div className="form">
            <TextInput
              label={t('lms.repository.chapter')}
              value={chapterId}
              onChange={(e) => setChapterId(e.target.value)}
            />
            <Button
              disabled={picked.length === 0 || !chapterId.trim() || place.isPending}
              onClick={() =>
                place.mutate(
                  { chapterId: chapterId.trim(), itemKind: kind, sourceIds: picked },
                  { onSuccess: () => setPicked([]) },
                )
              }
            >
              {t('lms.repository.place', { count: String(picked.length) })}
            </Button>
            {place.isSuccess ? <Banner intent="success">{t('lms.repository.placed')}</Banner> : null}
            <ActionError error={place.error} />
          </div>
        ) : null}
      </Card>
    </LmsPage>
  );
}

/**
 * LMS-48 Web resources & e-books: the admin's lists with create, delete, and the e-book conversion that answers
 * 503/501 until its provider is configured.
 */
export function ResourcesAdminScreen() {
  const { t } = useI18n();
  const resources = useWebResources();
  const ebooks = useEbooks();
  const resourceActions = useWebResourceActions();
  const ebookActions = useEbookActions();
  const canWrite = useCan('lms.web_resource.create');
  const [resourceDraft, setResourceDraft] = useState({ title: '', url: '' });
  const [ebookTitle, setEbookTitle] = useState('');
  const title = t('screen.LMS48.title');
  const state = LmsState({
    title,
    allowed: true,
    loading: resources.isPending || ebooks.isPending,
    error: resources.error ?? ebooks.error,
    retry: () => {
      void resources.refetch();
      void ebooks.refetch();
    },
  });
  if (state) return state;
  return (
    <LmsPage title={title}>
      <Card title={t('lms.resources.web')}>
        <ul>
          {(resources.data?.items ?? []).map((resource) => (
            <li key={resource.id}>
              {resource.title} <StatusChip label={resource.resourceType} />
              {resource.isInRepository ? <StatusChip label={t('lms.repository.shared')} /> : null}
              {canWrite ? (
                <Button variant="ghost" onClick={() => resourceActions.remove.mutate(resource)}>
                  {t('common.action.delete')}
                </Button>
              ) : null}
            </li>
          ))}
        </ul>
        {canWrite ? (
          <div className="form">
            <TextInput
              label={t('lms.resources.titleField')}
              value={resourceDraft.title}
              onChange={(e) => setResourceDraft({ ...resourceDraft, title: e.target.value })}
            />
            <TextInput
              label={t('lms.resources.url')}
              value={resourceDraft.url}
              onChange={(e) => setResourceDraft({ ...resourceDraft, url: e.target.value })}
            />
            <Button
              disabled={
                !resourceDraft.title.trim() || !resourceDraft.url.trim() || resourceActions.create.isPending
              }
              onClick={() =>
                resourceActions.create.mutate(
                  { title: resourceDraft.title.trim(), resourceType: 'url', url: resourceDraft.url.trim() },
                  { onSuccess: () => setResourceDraft({ title: '', url: '' }) },
                )
              }
            >
              {t('lms.resources.addWeb')}
            </Button>
            <ActionError error={resourceActions.create.error ?? resourceActions.remove.error} />
          </div>
        ) : null}
      </Card>
      <Card title={t('lms.resources.ebooks')}>
        <ul>
          {(ebooks.data?.items ?? []).map((ebook) => (
            <li key={ebook.id}>
              {ebook.title}{' '}
              <StatusChip label={ebook.fileId ? t('lms.ebook.ready') : t('lms.ebook.pending')} />
              {canWrite ? (
                <>
                  <Button variant="ghost" onClick={() => ebookActions.convert.mutate(ebook.id ?? '')}>
                    {t('lms.resources.convert')}
                  </Button>
                  <Button variant="ghost" onClick={() => ebookActions.remove.mutate(ebook)}>
                    {t('common.action.delete')}
                  </Button>
                </>
              ) : null}
            </li>
          ))}
        </ul>
        {canWrite ? (
          <div className="form">
            <TextInput
              label={t('lms.resources.titleField')}
              value={ebookTitle}
              onChange={(e) => setEbookTitle(e.target.value)}
            />
            <Button
              disabled={!ebookTitle.trim() || ebookActions.create.isPending}
              onClick={() =>
                ebookActions.create.mutate(
                  { title: ebookTitle.trim() },
                  { onSuccess: () => setEbookTitle('') },
                )
              }
            >
              {t('lms.resources.addEbook')}
            </Button>
            <ActionError
              error={ebookActions.create.error ?? ebookActions.remove.error ?? ebookActions.convert.error}
            />
          </div>
        ) : null}
      </Card>
    </LmsPage>
  );
}

/** The feature toggles LMS-53 offers; each is a SettingsUpdate field the admin may flip. */
const SETTINGS_TOGGLES: [keyof LmsSettingsUpdate & keyof LmsSettings & string, MessageKey][] = [
  ['allowGuestAccess', 'lms.settings.allowGuestAccess'],
  ['disableSignup', 'lms.settings.disableSignup'],
  ['showCourses', 'lms.settings.showCourses'],
  ['showLiveClass', 'lms.settings.showLiveClass'],
  ['showDiscussions', 'lms.settings.showDiscussions'],
  ['showDashboard', 'lms.settings.showDashboard'],
  ['enforceVideoCompletion', 'lms.settings.enforceVideoCompletion'],
  ['enforceQuizCompletion', 'lms.settings.enforceQuizCompletion'],
  ['enforceQuizPassing', 'lms.settings.enforceQuizPassing'],
  ['preventSkippingVideos', 'lms.settings.preventSkippingVideos'],
  ['lmsIsDefaultHome', 'lms.settings.lmsIsDefaultHome'],
  ['allowJobPosting', 'lms.settings.allowJobPosting'],
];

/**
 * LMS-53 LMS settings: the singleton's feature toggles and contact details under its ETag (the first save creates
 * the row), and the sidebar items that drive the phone and tablet menus. Tenant administrators only.
 */
export function LmsSettingsScreen() {
  const { t } = useI18n();
  const settings = useLmsSettings();
  const update = useUpdateLmsSettings();
  const sidebar = useSettingsSidebarItems(settings.data?.id ?? null);
  const canWrite = useCan('lms.settings.update');
  const [contact, setContact] = useState<string | null>(null);
  const title = t('screen.LMS53.title');
  const state = LmsState({
    title,
    allowed: useCan('lms.settings.read'),
    loading: settings.isPending,
    error: settings.error,
    retry: () => void settings.refetch(),
  });
  if (state) return state;
  const data = settings.data;
  const etag = data?.etag ?? '"0"';
  return (
    <LmsPage title={title} subtitle={t('lms.settings.subtitle')}>
      <Card title={t('lms.settings.toggles')}>
        {SETTINGS_TOGGLES.map(([field, label]) => (
          <Switch
            key={field}
            label={t(label)}
            checked={Boolean(data?.[field])}
            disabled={!canWrite || update.isPending}
            onChange={(checked) => update.mutate({ etag, changes: { [field]: checked } })}
          />
        ))}
        <ActionError error={update.error} />
      </Card>
      <Card title={t('lms.settings.contact')}>
        <TextInput
          label={t('lms.settings.contactEmail')}
          value={contact ?? data?.contactUsEmail ?? ''}
          disabled={!canWrite}
          onChange={(event) => setContact(event.target.value)}
        />
        <Button
          disabled={!canWrite || contact === null || update.isPending}
          onClick={() => {
            update.mutate({ etag, changes: { contactUsEmail: contact?.trim() || undefined } });
            setContact(null);
          }}
        >
          {t('common.action.save')}
        </Button>
      </Card>
      <Card title={t('lms.settings.sidebar')}>
        {data?.id ? null : <p>{t('lms.settings.sidebarUnsaved')}</p>}
        <ul>
          {(sidebar.data?.items ?? []).map((item) => (
            <li key={item.id}>
              {item.title} {item.isHidden ? <StatusChip label={t('lms.settings.hidden')} /> : null}
            </li>
          ))}
        </ul>
      </Card>
    </LmsPage>
  );
}
