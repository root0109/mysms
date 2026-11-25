import {
  resourceDef,
  useCan,
  useCreateRecord,
  useHideMessageForMe,
  useI18n,
  useInfiniteRecords,
  useMarkConversationRead,
  useSession,
  type RecordRow,
} from '@mysms/core';
import { Banner, Button, Card, SelectField, Skeleton, TextArea, TextInput } from '@mysms/ui-web';
import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router';
import { UUID } from '../ids';
import { problemText } from '../recordText';
import { RelatedRecords } from '../RelatedRecords';

const CONVERSATIONS = resourceDef('comms.Conversation');
const MESSAGES = resourceDef('comms.ConversationMessage');

/** Starts a conversation (comms.CreateConversation); its people are added on its participants tab. */
function NewConversation({ onCreated }: { onCreated: (id: string) => void }) {
  const { t } = useI18n();
  const create = useCreateRecord(CONVERSATIONS as NonNullable<typeof CONVERSATIONS>);
  const [title, setTitle] = useState('');
  const [kind, setKind] = useState<'direct' | 'group'>('direct');
  return (
    <Card title={t('comms.chat.new')}>
      {create.error ? <Banner intent="danger">{problemText(t, create.error)}</Banner> : null}
      <form
        className="form"
        onSubmit={(event) => {
          event.preventDefault();
          create.mutate(
            { values: { conversationType: kind, ...(title.trim() ? { title: title.trim() } : {}) } },
            { onSuccess: (row) => onCreated(row.id) },
          );
        }}
      >
        <SelectField
          label={t('comms.chat.kind')}
          value={kind}
          options={[
            { value: 'direct', label: t('comms.chat.kind.direct') },
            { value: 'group', label: t('comms.chat.kind.group') },
          ]}
          onChange={(e) => setKind(e.target.value as 'direct' | 'group')}
        />
        <TextInput
          label={t('comms.chat.title')}
          value={title}
          maxLength={200}
          onChange={(e) => setTitle(e.target.value)}
        />
        <div className="form__actions">
          <Button type="submit" loading={create.isPending}>
            {t('comms.chat.start')}
          </Button>
        </div>
      </form>
    </Card>
  );
}

/**
 * One conversation: its messages oldest first (the newest page read first, earlier ones on request), the caller's
 * own marked as theirs; sending a message; hiding one from the caller's own view; and, on open, the caller's read
 * mark moved to now. Messages are refreshed on request (no socket in v1, ui/02 COM-05).
 */
function Thread({ conversationId }: { conversationId: string }) {
  const { t, format } = useI18n();
  const me = useSession((s) => s.me?.user?.id ?? '');
  const mayRead = useCan('comms.conversation_message.read');
  const maySend = useCan('comms.conversation_message.create');
  const mayHide = useCan('comms.conversation_message.hide_message_for_me');
  const mayMark = useCan('comms.conversation.mark_conversation_read');
  const messages = useInfiniteRecords(mayRead ? MESSAGES : undefined, {
    parentId: conversationId,
    sort: ['createTime,desc'],
    size: 50,
  });
  const send = useCreateRecord(MESSAGES as NonNullable<typeof MESSAGES>);
  const hide = useHideMessageForMe();
  const markRead = useMarkConversationRead();
  const [text, setText] = useState('');
  const { mutate: mark } = markRead;
  useEffect(() => {
    if (mayMark) mark(conversationId);
  }, [conversationId, mayMark, mark]);
  const rows: RecordRow[] = [...(messages.data?.pages.flatMap((page) => page.data) ?? [])].reverse();
  const failure = send.error ?? hide.error;
  return (
    <Card title={t('comms.chat.messages')}>
      {!mayRead ? <Banner intent="info">{t('adm.resource.noAccess')}</Banner> : null}
      {messages.isPending && mayRead ? <Skeleton label={t('common.state.loading')} lines={4} /> : null}
      {messages.isError ? <Banner intent="danger">{problemText(t, messages.error)}</Banner> : null}
      {messages.hasNextPage ? (
        <Button
          variant="secondary"
          loading={messages.isFetchingNextPage}
          onClick={() => void messages.fetchNextPage()}
        >
          {t('comms.chat.earlier')}
        </Button>
      ) : null}
      {messages.isSuccess && rows.length === 0 ? <p className="hint">{t('comms.chat.noMessages')}</p> : null}
      <ol className="thread" aria-label={t('comms.chat.messages')} aria-live="polite">
        {rows.map((row) => {
          const mine = row.senderUserId === me;
          return (
            <li key={row.id} className={`thread__post${mine ? ' thread__post--mine' : ''}`}>
              <p className="hint">
                {[
                  mine ? t('comms.chat.you') : t('comms.chat.them'),
                  row.createTime ? format.dateTime(String(row.createTime)) : '',
                ]
                  .filter(Boolean)
                  .join(' · ')}
              </p>
              <p className="consent-notice">{String(row.body ?? '')}</p>
              {mayHide ? (
                <Button variant="secondary" onClick={() => hide.mutate(row.id)}>
                  {t('comms.chat.hide')}
                </Button>
              ) : null}
            </li>
          );
        })}
      </ol>
      {failure ? <Banner intent="danger">{problemText(t, failure)}</Banner> : null}
      {maySend ? (
        <form
          className="form"
          onSubmit={(event) => {
            event.preventDefault();
            if (!text.trim()) return;
            send.mutate(
              { parentId: conversationId, values: { body: text.trim() } },
              { onSuccess: () => setText('') },
            );
          }}
        >
          <TextArea
            label={t('comms.chat.write')}
            value={text}
            rows={3}
            onChange={(e) => setText(e.target.value)}
          />
          <div className="form__actions">
            <Button type="submit" disabled={!text.trim()} loading={send.isPending}>
              {t('comms.chat.send')}
            </Button>
            <Button variant="secondary" onClick={() => void messages.refetch()}>
              {t('comms.chat.refresh')}
            </Button>
          </div>
        </form>
      ) : null}
    </Card>
  );
}

/**
 * COM-05 Conversations: the caller's conversations (the server lists those they take part in), most recently active
 * first; one opens in place (`?conversation=`) with its messages and its participants. Starting a conversation
 * makes it, then its people are added.
 */
export function ConversationsScreen() {
  const { t, format } = useI18n();
  const [search, setSearch] = useSearchParams();
  const asked = search.get('conversation') ?? '';
  const open = UUID.test(asked) ? asked : '';
  const mayRead = useCan('comms.conversation.read');
  const mayCreate = useCan('comms.conversation.create');
  const conversations = useInfiniteRecords(mayRead ? CONVERSATIONS : undefined, {
    filter: 'isActive = true',
    sort: ['updateTime,desc'],
    size: 30,
  });
  const rows = conversations.data?.pages.flatMap((page) => page.data) ?? [];
  const choose = (id: string) => setSearch(id ? { conversation: id } : {});
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.COM05.title')}</h1>
        <p className="page__subtitle">{t('comms.chat.intro')}</p>
      </header>
      {!mayRead ? <Banner intent="info">{t('adm.resource.noAccess')}</Banner> : null}
      <div className="page__grid">
        <Card title={t('comms.chat.list')}>
          {conversations.isPending && mayRead ? (
            <Skeleton label={t('common.state.loading')} lines={4} />
          ) : null}
          {conversations.isError ? (
            <Banner intent="danger">{problemText(t, conversations.error)}</Banner>
          ) : null}
          {conversations.isSuccess && rows.length === 0 ? (
            <p className="hint">{t('comms.chat.none')}</p>
          ) : null}
          <ul className="leave-list">
            {rows.map((row) => (
              <li key={row.id} className="leave-list__item">
                <div className="leave-list__what">
                  <span>
                    {typeof row.title === 'string' && row.title ? row.title : t('comms.chat.untitled')}
                  </span>
                  <span className="hint">
                    {typeof row.lastMessageAt === 'string'
                      ? t('comms.chat.lastMessage', { when: format.dateTime(row.lastMessageAt) })
                      : t('comms.chat.noMessages')}
                  </span>
                </div>
                <Button
                  variant={row.id === open ? 'primary' : 'secondary'}
                  aria-pressed={row.id === open}
                  onClick={() => choose(row.id)}
                >
                  {t('comms.chat.open')}
                </Button>
              </li>
            ))}
          </ul>
          {conversations.hasNextPage ? (
            <Button
              variant="secondary"
              loading={conversations.isFetchingNextPage}
              onClick={() => void conversations.fetchNextPage()}
            >
              {t('common.list.more')}
            </Button>
          ) : null}
        </Card>
        {open ? (
          <div>
            <Thread conversationId={open} />
            <RelatedRecords
              resource="comms.ConversationParticipant"
              title={t('comms.chat.participants')}
              parentId={open}
              columns={['userId', 'lastReadAt']}
            />
          </div>
        ) : mayCreate ? (
          <NewConversation onCreated={choose} />
        ) : null}
      </div>
    </div>
  );
}
