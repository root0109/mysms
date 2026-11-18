import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { lms } from '@mysms/sdk';
import { CohortApi } from '@mysms/sdk/lms/apis/CohortApi';
import { ForumPostVoteApi } from '@mysms/sdk/lms/apis/ForumPostVoteApi';
import { ForumSectionApi } from '@mysms/sdk/lms/apis/ForumSectionApi';
import { ForumTopicApi } from '@mysms/sdk/lms/apis/ForumTopicApi';
import { allPages, pageQuery } from '../api/pages';
import { call, callWithEtag } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';

export type Forum = lms.Forum;
export type ForumTopic = lms.ForumTopic;
export type ForumPost = lms.ForumPost;
/** The moderation steps on one post (api/state-machines.md "lms.ForumPost"). */
export type ForumPostModeration = 'hide' | 'unhide' | 'confirmAbuse';

const FORUMS = (cohortId: string | null) => queryKey('lms', 'forums', cohortId);
const TOPICS = (filter: string | null) => queryKey('lms', 'forum-topics', filter);
const TOPIC = (topicId: string | null) => queryKey('lms', 'forum-topic', topicId);
const POSTS = (topicId: string | null) => queryKey('lms', 'forum-posts', topicId);

/** lms.ListForums of a cohort. */
export function useForums(cohortId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: FORUMS(cohortId),
    queryFn: () =>
      allPages((page) =>
        call(api.get(CohortApi).listForums({ cohortId: cohortId as string, size: 200, ...pageQuery(page) })),
      ),
    enabled: cohortId !== null,
    staleTime: STALE_TIME.list,
  });
}

/** The cohort's topics: its forums, their sections, and every topic under them, newest first. */
export function useCohortForumTopics(cohortId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey('lms', 'cohort-forum-topics', cohortId),
    queryFn: async () => {
      const forums = await allPages((page) =>
        call(api.get(CohortApi).listForums({ cohortId: cohortId as string, size: 200, ...pageQuery(page) })),
      );
      if (forums.items.length === 0) return { forums: [], topics: [] as ForumTopic[] };
      const forumFilter = forums.items.map((forum) => `forumId = "${forum.id}"`).join(' OR ');
      const sections = await allPages((page) =>
        call(
          api.get(ForumSectionApi).listForumSections({ size: 500, filter: forumFilter, ...pageQuery(page) }),
        ),
      );
      if (sections.items.length === 0) return { forums: forums.items, topics: [] as ForumTopic[] };
      const sectionFilter = sections.items.map((section) => `forumSectionId = "${section.id}"`).join(' OR ');
      const topics = await allPages((page) =>
        call(
          api.get(ForumTopicApi).listForumTopics({
            size: 500,
            sort: ['createTime desc'],
            filter: sectionFilter,
            ...pageQuery(page),
          }),
        ),
      );
      return { forums: forums.items, topics: topics.items };
    },
    enabled: cohortId !== null,
    staleTime: STALE_TIME.list,
  });
}

/** lms.ListForumTopics, newest first; narrowed by the filter when given. */
export function useForumTopics(filter?: string) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: TOPICS(filter ?? null),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(ForumTopicApi).listForumTopics({
            size: 200,
            sort: ['createTime desc'],
            ...(filter ? { filter } : {}),
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** lms.GetForumTopic with its ETag. */
export function useForumTopic(topicId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: TOPIC(topicId),
    queryFn: () => callWithEtag(api.get(ForumTopicApi).getForumTopicRaw({ forumTopicId: topicId as string })),
    enabled: topicId !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** lms.ListForumPosts of a topic, oldest first, so the thread reads top to bottom. */
export function useForumPosts(topicId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: POSTS(topicId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(ForumTopicApi).listForumPosts({
            forumTopicId: topicId as string,
            size: 500,
            sort: ['createTime'],
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: topicId !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** lms.CreateForumPost: the caller's reply in the topic (under the named parent when answering a post). */
export function useCreateForumPost() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ topicId, body, parentPostId }: { topicId: string; body: string; parentPostId?: string }) =>
      call(
        api.get(ForumTopicApi).createForumPost({
          forumTopicId: topicId,
          forumPost: {
            forumTopicId: topicId,
            body,
            ...(parentPostId ? { parentPostId } : {}),
          },
        }),
      ),
    onSettled: (_r, _e, variables) => qc.invalidateQueries({ queryKey: POSTS(variables.topicId) }),
  });
}

/** lms.MarkTopicRead: the topic (and the posts given) read by the caller. */
export function useMarkTopicRead() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: ({ topicId, postIds }: { topicId: string; postIds?: string[] }) =>
      call(
        api.get(ForumTopicApi).markTopicRead({
          forumTopicId: topicId,
          markTopicReadRequest: postIds?.length ? { forumPostIds: postIds } : {},
        }),
      ),
  });
}

/** lms.FlagForumPost: sets or clears the caller's own flag on a post. */
export function useFlagForumPost() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: ({ topicId, postId, flagged }: { topicId: string; postId: string; flagged: boolean }) =>
      call(
        api.get(ForumTopicApi).flagForumPost({
          forumTopicId: topicId,
          forumPostId: postId,
          flagForumPostRequest: { flagged },
        }),
      ),
  });
}

/** lms.CreateForumPostVote: the caller's like on a post. */
export function useVoteForumPost() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ postId }: { topicId: string; postId: string }) =>
      call(
        api
          .get(ForumPostVoteApi)
          .createForumPostVote({ forumPostVote: { forumPostId: postId, isLike: true } }),
      ),
    onSettled: (_r, _e, variables) => qc.invalidateQueries({ queryKey: POSTS(variables.topicId) }),
  });
}

/** The moderation steps: hiding, showing and confirming a reported post, under the post's etag. */
export function useModerateForumPost() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      topicId,
      postId,
      step,
      etag,
      reason,
    }: {
      topicId: string;
      postId: string;
      step: ForumPostModeration;
      etag: string | null;
      reason?: string;
    }) => {
      const posts = api.get(ForumTopicApi);
      const body = {
        ...(etag ? { etag } : {}),
        ...(reason?.trim() ? { reason: reason.trim() } : {}),
      };
      const at = { forumTopicId: topicId, forumPostId: postId };
      if (step === 'hide') return call(posts.hideForumPost({ ...at, hideForumPostRequest: body }));
      if (step === 'unhide') return call(posts.unhideForumPost({ ...at, unhideForumPostRequest: body }));
      return call(posts.confirmPostAbuse({ ...at, confirmPostAbuseRequest: body }));
    },
    onSettled: (_r, _e, variables) => qc.invalidateQueries({ queryKey: POSTS(variables.topicId) }),
  });
}

/** lms.MarkPostSubstantive: counts (or uncounts) a post for discussion grading. */
export function useMarkPostSubstantive() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      topicId,
      postId,
      substantive,
    }: {
      topicId: string;
      postId: string;
      substantive: boolean;
    }) =>
      call(
        api.get(ForumTopicApi).markPostSubstantive({
          forumTopicId: topicId,
          forumPostId: postId,
          markPostSubstantiveRequest: { substantive },
        }),
      ),
    onSettled: (_r, _e, variables) => qc.invalidateQueries({ queryKey: POSTS(variables.topicId) }),
  });
}
