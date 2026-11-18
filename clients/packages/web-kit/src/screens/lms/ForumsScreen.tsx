import {
  lmsFailureText,
  useCan,
  useCohortForumTopics,
  useCreateForumPost,
  useFlagForumPost,
  useForumPosts,
  useI18n,
  useMarkPostSubstantive,
  useMarkTopicRead,
  useModerateForumPost,
  useVoteForumPost,
  type ForumPost,
} from '@mysms/core';
import { Banner, Button, Card, DataTable, StatusChip, TextArea, type Column } from '@mysms/ui-web';
import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router';
import { LmsRetryBanner } from './lmsShared';

/**
 * LMS-26 Forums: the cohort's forums and topics, one topic's thread (replies indented under their parent), writing a
 * reply, likes, the reader's own flag, and moderation — hiding, showing, confirming a reported post and counting a
 * post for discussion grading. Opening a topic marks it read.
 */
export function ForumsScreen() {
  const { t, format } = useI18n();
  const { cohortId = '' } = useParams();
  const [topicId, setTopicId] = useState<string | null>(null);
  const topics = useCohortForumTopics(cohortId || null);
  const posts = useForumPosts(topicId);
  const reply = useCreateForumPost();
  const markRead = useMarkTopicRead();
  const flag = useFlagForumPost();
  const vote = useVoteForumPost();
  const moderate = useModerateForumPost();
  const substantive = useMarkPostSubstantive();
  const mayPost = useCan('lms.forum_post.create');
  const mayHide = useCan('lms.forum_post.hide_forum_post');
  const mayConfirm = useCan('lms.forum_post.confirm_post_abuse');
  const mayGrade = useCan('lms.forum_post.mark_post_substantive');
  const [text, setText] = useState('');
  const [replyTo, setReplyTo] = useState<string | null>(null);
  const [problem, setProblem] = useState<string | null>(null);

  useEffect(() => {
    if (topicId) markRead.mutate({ topicId });
    // the read mark is the side effect of opening the topic, once per choice
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [topicId]);

  type Topic = NonNullable<typeof topics.data>['topics'][number];
  const topicColumns: Column<Topic>[] = [
    { key: 'title', header: t('lms.forum.topic'), render: (row) => row.title },
    {
      key: 'state',
      header: t('lms.forum.state'),
      render: (row) => (row.isHidden ? <StatusChip intent="warning" label={t('lms.forum.hidden')} /> : null),
    },
  ];

  const send = () => {
    if (!topicId) return;
    setProblem(null);
    reply.mutate(
      { topicId, body: text.trim(), ...(replyTo ? { parentPostId: replyTo } : {}) },
      {
        onSuccess: () => {
          setText('');
          setReplyTo(null);
        },
        onError: (error) => setProblem(lmsFailureText(t, error)),
      },
    );
  };

  const postActions = (post: ForumPost) => (
    <span className="chip-row">
      <Button onClick={() => setReplyTo(String(post.id))}>{t('lms.forum.reply')}</Button>
      <Button
        onClick={() => vote.mutate({ topicId: topicId ?? '', postId: String(post.id) })}
        disabled={vote.isPending}
      >
        {t('lms.forum.like', { count: post.likeCount ?? 0 })}
      </Button>
      <Button
        onClick={() => flag.mutate({ topicId: topicId ?? '', postId: String(post.id), flagged: true })}
        disabled={flag.isPending}
      >
        {t('lms.forum.flag')}
      </Button>
      {mayHide ? (
        <Button
          onClick={() =>
            moderate.mutate(
              {
                topicId: topicId ?? '',
                postId: String(post.id),
                step: post.isHidden ? 'unhide' : 'hide',
                etag: post.etag ?? null,
              },
              { onError: (error) => setProblem(lmsFailureText(t, error)) },
            )
          }
          disabled={moderate.isPending}
        >
          {post.isHidden ? t('lms.forum.unhide') : t('lms.forum.hide')}
        </Button>
      ) : null}
      {mayConfirm && post.abuseReportCount ? (
        <Button
          onClick={() =>
            moderate.mutate(
              {
                topicId: topicId ?? '',
                postId: String(post.id),
                step: 'confirmAbuse',
                etag: post.etag ?? null,
              },
              { onError: (error) => setProblem(lmsFailureText(t, error)) },
            )
          }
          disabled={moderate.isPending}
        >
          {t('lms.forum.confirmAbuse')}
        </Button>
      ) : null}
      {mayGrade ? (
        <Button
          onClick={() =>
            substantive.mutate({
              topicId: topicId ?? '',
              postId: String(post.id),
              substantive: (post.substantiveFlag ?? 0) === 0,
            })
          }
          disabled={substantive.isPending}
        >
          {(post.substantiveFlag ?? 0) > 0 ? t('lms.forum.notSubstantive') : t('lms.forum.substantive')}
        </Button>
      ) : null}
    </span>
  );

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.LMS26.title')}</h1>
        <p className="page__subtitle">
          <Link to={`/app/lms/cohorts/${cohortId}`}>{t('lms.cohort.back')}</Link>
        </p>
      </header>
      {problem ? <Banner intent="danger">{problem}</Banner> : null}
      <div className="page__grid">
        <Card title={t('lms.forum.topics')}>
          {topics.isError ? (
            <LmsRetryBanner onRetry={() => void topics.refetch()} />
          ) : (
            <DataTable
              caption={t('lms.forum.topics')}
              columns={topicColumns}
              rows={topics.data?.topics}
              rowKey={(row) => String(row.id)}
              loading={topics.isPending}
              loadingLabel={t('common.state.loading')}
              emptyText={t('lms.forum.noTopics')}
              onRowClick={(row) => setTopicId(String(row.id))}
            />
          )}
        </Card>
        {topicId ? (
          <Card title={t('lms.forum.thread')}>
            {posts.isError ? (
              <LmsRetryBanner onRetry={() => void posts.refetch()} />
            ) : posts.isPending ? (
              <p>{t('common.state.loading')}</p>
            ) : (
              <ul className="thread">
                {(posts.data?.items ?? []).map((post) => (
                  <li
                    key={String(post.id)}
                    className={post.parentPostId ? 'thread__post thread__post--reply' : 'thread__post'}
                  >
                    <p className="hint">
                      {t('lms.forum.postedAt', {
                        at: post.postedAt ? format.day(post.postedAt) : '',
                      })}
                      {post.isHidden || post.isHiddenByParent ? ` · ${t('lms.forum.hidden')}` : ''}
                      {(post.substantiveFlag ?? 0) > 0 ? ` · ${t('lms.forum.counted')}` : ''}
                    </p>
                    <p>{post.isHidden || post.isHiddenByParent ? t('lms.forum.hiddenBody') : post.body}</p>
                    {postActions(post)}
                  </li>
                ))}
                {(posts.data?.items ?? []).length === 0 ? <li>{t('lms.forum.noPosts')}</li> : null}
              </ul>
            )}
            {mayPost ? (
              <>
                {replyTo ? <p className="hint">{t('lms.forum.replyingTo')}</p> : null}
                <TextArea
                  label={t('lms.forum.yourReply')}
                  value={text}
                  onChange={(e) => setText(e.target.value)}
                />
                <Button onClick={send} disabled={reply.isPending || !text.trim()}>
                  {t('lms.forum.send')}
                </Button>
              </>
            ) : null}
          </Card>
        ) : null}
      </div>
    </div>
  );
}
