import { NoticeList } from './CommsScreens';
import {
  lmsFailureText,
  useBadgeAwards,
  useCan,
  useCertificates,
  useCohort,
  useCohortForumTopics,
  useCohortOutline,
  useCohortRoster,
  useCohortSchedule,
  useCohorts,
  useCohortTimetable,
  useCreateForumPost,
  useDownloadCertificate,
  useForumPosts,
  useI18n,
  useJoinLiveClass,
  useLiveClasses,
  useLiveClassStartUrl,
  useLiveClassStep,
  useMarkTopicRead,
  useSendCohortAnnouncement,
  useSession,
  useVoteForumPost,
  type LiveClass,
} from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import { Banner, Button, Screen, Text, TextInput, useTheme } from '@mysms/ui-native';
import { useEffect, useState } from 'react';
import { Linking, Pressable, View } from 'react-native';
import type { CohortScreenId, LmsRoute } from './lmsRoutes';

/**
 * The LMS on phones (S) and the tablet (B), the LMS's main device: the member's cohorts (LMS-20/21), the roster
 * (LMS-23, tablet), schedule (LMS-25, tablet), forums with replies and likes (LMS-26), live classes with
 * provider-agnostic join (LMS-27), the outline (LMS-22, tablet) and the member's certificates and badges (LMS-15).
 * Announcements to the cohort (LMS-28) are sent from the cohort page and read as the cohort's notices.
 */
export function CohortScreen({
  route,
  tablet = false,
  onRoute,
  onBack,
}: {
  route: LmsRoute<CohortScreenId>;
  tablet?: boolean;
  onRoute: (route: LmsRoute) => void;
  onBack: () => void;
}) {
  const { t } = useI18n();
  const cohortId = route.params.cohortId ?? '';
  return (
    <Screen tablet={tablet}>
      <Button variant="secondary" onPress={onBack}>
        {t('common.action.back')}
      </Button>
      {route.screenId === 'LMS-20' ? (
        <CohortList
          onOpen={(id) => onRoute({ screenId: 'LMS-21', params: { cohortId: id } })}
          onCertificates={() => onRoute({ screenId: 'LMS-15', params: {} })}
          onLive={() => onRoute({ screenId: 'LMS-27', params: {} })}
        />
      ) : null}
      {route.screenId === 'LMS-21' ? (
        <CohortHome cohortId={cohortId} tablet={tablet} onRoute={onRoute} />
      ) : null}
      {route.screenId === 'LMS-23' ? <Roster cohortId={cohortId} /> : null}
      {route.screenId === 'LMS-25' ? <Schedule cohortId={cohortId} /> : null}
      {route.screenId === 'LMS-26' ? <Forums cohortId={cohortId} /> : null}
      {route.screenId === 'LMS-27' ? <LiveClasses cohortId={cohortId || undefined} /> : null}
      {route.screenId === 'LMS-22' ? <Outline cohortId={cohortId} /> : null}
      {route.screenId === 'LMS-15' ? <Certificates /> : null}
      {route.screenId === 'LMS-28' ? <NoticeList cohortId={cohortId} tablet={tablet} /> : null}
    </Screen>
  );
}

function CohortList({
  onOpen,
  onCertificates,
  onLive,
}: {
  onOpen: (cohortId: string) => void;
  onCertificates: () => void;
  onLive: () => void;
}) {
  const { t, format } = useI18n();
  const theme = useTheme();
  const cohorts = useCohorts();
  return (
    <View>
      <Text variant="h1" accessibilityRole="header">
        {t('lms.teaching.cohorts')}
      </Text>
      <View style={{ flexDirection: 'row', gap: theme.space[3] }}>
        <Button variant="secondary" onPress={onCertificates}>
          {t('screen.LMS15.title')}
        </Button>
        <Button variant="secondary" onPress={onLive}>
          {t('screen.LMS27.title')}
        </Button>
      </View>
      {cohorts.isError ? <Banner intent="danger">{t('common.error.generic')}</Banner> : null}
      {cohorts.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
      {(cohorts.data?.items ?? []).map((cohort) => (
        <Pressable
          key={String(cohort.id)}
          accessibilityRole="button"
          onPress={() => onOpen(String(cohort.id))}
          style={{ paddingVertical: theme.space[4] }}
        >
          <Text>{cohort.title}</Text>
          <Text muted>
            {t('lms.cohort.dates', { from: format.day(cohort.startDate), to: format.day(cohort.endDate) })}
          </Text>
        </Pressable>
      ))}
      {cohorts.data && cohorts.data.items.length === 0 ? (
        <Text muted>{t('lms.teaching.noCohorts')}</Text>
      ) : null}
    </View>
  );
}

function CohortHome({
  cohortId,
  tablet,
  onRoute,
}: {
  cohortId: string;
  tablet: boolean;
  onRoute: (route: LmsRoute) => void;
}) {
  const { t, format } = useI18n();
  const theme = useTheme();
  const cohort = useCohort(cohortId || null);
  const announce = useSendCohortAnnouncement();
  const mayAnnounce = useCan('lms.cohort.send_cohort_announcement');
  const [subject, setSubject] = useState('');
  const [content, setContent] = useState('');
  const [note, setNote] = useState<string | null>(null);
  const [problem, setProblem] = useState<string | null>(null);
  if (cohort.isPending) return <Text muted>{t('common.state.loading')}</Text>;
  if (cohort.isError || !cohort.data) return <Banner intent="danger">{t('common.error.generic')}</Banner>;
  const row = cohort.data.data;
  const open = (screenId: LmsRoute['screenId']) => () => onRoute({ screenId, params: { cohortId } });
  return (
    <View>
      <Text variant="h1" accessibilityRole="header">
        {row.title}
      </Text>
      <Text muted>
        {t('lms.cohort.dates', { from: format.day(row.startDate), to: format.day(row.endDate) })} ·{' '}
        {enumLabel(t, 'lmsCohort', row.lifecycleStatus ?? 'draft')}
      </Text>
      {problem ? <Banner intent="danger">{problem}</Banner> : null}
      {note ? <Banner intent="success">{note}</Banner> : null}
      <View style={{ gap: theme.space[3] }}>
        <Button onPress={open('LMS-26')}>{t('screen.LMS26.title')}</Button>
        <Button onPress={open('LMS-27')}>{t('screen.LMS27.title')}</Button>
        <Button onPress={open('LMS-28')}>{t('screen.LMS28.title')}</Button>
        {tablet ? <Button onPress={open('LMS-23')}>{t('screen.LMS23.title')}</Button> : null}
        {tablet ? <Button onPress={open('LMS-25')}>{t('screen.LMS25.title')}</Button> : null}
        {tablet ? <Button onPress={open('LMS-22')}>{t('screen.LMS22.title')}</Button> : null}
      </View>
      {mayAnnounce ? (
        <View style={{ gap: theme.space[3] }}>
          <Text variant="h2" accessibilityRole="header">
            {t('lms.cohort.announce')}
          </Text>
          <TextInput label={t('lms.cohort.announceSubject')} value={subject} onChangeText={setSubject} />
          <TextInput
            label={t('lms.cohort.announceContent')}
            value={content}
            onChangeText={setContent}
            multiline
          />
          <Button
            disabled={announce.isPending || !subject.trim() || !content.trim()}
            onPress={() => {
              setProblem(null);
              announce.mutate(
                { cohortId, subject, content },
                {
                  onSuccess: (result) => {
                    setNote(t('lms.cohort.announcementSent', { queued: result.queued ?? 0 }));
                    setSubject('');
                    setContent('');
                  },
                  onError: (error) => setProblem(lmsFailureText(t, error)),
                },
              );
            }}
          >
            {t('lms.cohort.announceSend')}
          </Button>
        </View>
      ) : null}
    </View>
  );
}

function Roster({ cohortId }: { cohortId: string }) {
  const { t } = useI18n();
  const roster = useCohortRoster(cohortId || null);
  return (
    <View>
      <Text variant="h1" accessibilityRole="header">
        {t('screen.LMS23.title')}
      </Text>
      {roster.isError ? <Banner intent="danger">{t('common.error.generic')}</Banner> : null}
      {roster.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
      {(roster.data?.items ?? []).map((member) => (
        <Text key={`${member.userId}-${member.roleCode}`}>
          {member.displayName} —{' '}
          {t(member.roleCode === 'teacher' ? 'lms.roster.teacher' : 'lms.roster.student')}
          {member.isActive ? '' : ` (${t('enum.status.inactive')})`}
        </Text>
      ))}
      {roster.data && roster.data.items.length === 0 ? <Text muted>{t('lms.roster.empty')}</Text> : null}
    </View>
  );
}

function Schedule({ cohortId }: { cohortId: string }) {
  const { t, format } = useI18n();
  const timetable = useCohortTimetable(cohortId || null);
  const schedule = useCohortSchedule(cohortId || null);
  return (
    <View>
      <Text variant="h1" accessibilityRole="header">
        {t('screen.LMS25.title')}
      </Text>
      {timetable.isError || schedule.isError ? (
        <Banner intent="danger">{t('common.error.generic')}</Banner>
      ) : null}
      {timetable.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
      <Text variant="h2" accessibilityRole="header">
        {t('lms.schedule.timetable')}
      </Text>
      {(timetable.data?.items ?? []).map((entry) => (
        <Text key={String(entry.id)}>
          {entry.entryDate
            ? format.day(entry.entryDate)
            : t('lms.schedule.dayNumber', { day: entry.dayNumber ?? 0 })}{' '}
          · {entry.startTime ?? ''}–{entry.endTime ?? ''}
        </Text>
      ))}
      {timetable.data && timetable.data.items.length === 0 ? (
        <Text muted>{t('lms.schedule.noTimetable')}</Text>
      ) : null}
      <Text variant="h2" accessibilityRole="header">
        {t('lms.schedule.sessions')}
      </Text>
      {(schedule.data?.sessions ?? []).map((session) => (
        <Text key={String(session.id)}>
          {session.startsAt ? format.day(session.startsAt) : ''} {session.description ?? session.venue ?? ''}
        </Text>
      ))}
      {schedule.data && schedule.data.sessions.length === 0 ? (
        <Text muted>{t('lms.schedule.noSessions')}</Text>
      ) : null}
    </View>
  );
}

function Forums({ cohortId }: { cohortId: string }) {
  const { t, format } = useI18n();
  const theme = useTheme();
  const topics = useCohortForumTopics(cohortId || null);
  const [topicId, setTopicId] = useState<string | null>(null);
  const posts = useForumPosts(topicId);
  const reply = useCreateForumPost();
  const vote = useVoteForumPost();
  const markRead = useMarkTopicRead();
  const mayPost = useCan('lms.forum_post.create');
  const [text, setText] = useState('');
  const [problem, setProblem] = useState<string | null>(null);
  useEffect(() => {
    if (topicId) markRead.mutate({ topicId });
    // opening the topic is what marks it read
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [topicId]);
  if (topicId === null) {
    return (
      <View>
        <Text variant="h1" accessibilityRole="header">
          {t('lms.forum.topics')}
        </Text>
        {topics.isError ? <Banner intent="danger">{t('common.error.generic')}</Banner> : null}
        {topics.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
        {(topics.data?.topics ?? []).map((topic) => (
          <Pressable
            key={String(topic.id)}
            accessibilityRole="button"
            onPress={() => setTopicId(String(topic.id))}
            style={{ paddingVertical: theme.space[4] }}
          >
            <Text>{topic.title}</Text>
          </Pressable>
        ))}
        {topics.data && topics.data.topics.length === 0 ? <Text muted>{t('lms.forum.noTopics')}</Text> : null}
      </View>
    );
  }
  return (
    <View>
      <Button variant="secondary" onPress={() => setTopicId(null)}>
        {t('lms.forum.topics')}
      </Button>
      {problem ? <Banner intent="danger">{problem}</Banner> : null}
      {posts.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
      {(posts.data?.items ?? []).map((post) => (
        <View
          key={String(post.id)}
          style={{ paddingVertical: theme.space[3], paddingLeft: post.parentPostId ? theme.space[5] : 0 }}
        >
          <Text muted>{t('lms.forum.postedAt', { at: post.postedAt ? format.day(post.postedAt) : '' })}</Text>
          <Text>
            {post.isHidden || post.isHiddenByParent ? t('lms.forum.hiddenBody') : (post.body ?? '')}
          </Text>
          <Button variant="secondary" onPress={() => vote.mutate({ topicId, postId: String(post.id) })}>
            {t('lms.forum.like', { count: post.likeCount ?? 0 })}
          </Button>
        </View>
      ))}
      {mayPost ? (
        <View style={{ gap: theme.space[3] }}>
          <TextInput label={t('lms.forum.yourReply')} value={text} onChangeText={setText} multiline />
          <Button
            disabled={reply.isPending || !text.trim()}
            onPress={() =>
              reply.mutate(
                { topicId, body: text.trim() },
                {
                  onSuccess: () => setText(''),
                  onError: (error) => setProblem(lmsFailureText(t, error)),
                },
              )
            }
          >
            {t('lms.forum.send')}
          </Button>
        </View>
      ) : null}
    </View>
  );
}

function LiveClasses({ cohortId }: { cohortId?: string }) {
  const { t, format } = useI18n();
  const theme = useTheme();
  const classes = useLiveClasses(cohortId ?? null);
  const join = useJoinLiveClass();
  const startUrl = useLiveClassStartUrl();
  const step = useLiveClassStep();
  const mayJoin = useCan('lms.live_class.join_live_class');
  const mayStart = useCan('lms.live_class.get_live_class_start_url');
  const mayEnd = useCan('lms.live_class.end_live_class');
  const [problem, setProblem] = useState<string | null>(null);
  const fail = (error: unknown) => setProblem(lmsFailureText(t, error));
  const openLink = (link: { url?: string }) => {
    if (link.url) void Linking.openURL(link.url);
  };
  const line = (liveClass: LiveClass) =>
    `${liveClass.classDate ? format.day(liveClass.classDate) : ''} ${liveClass.startTime ?? ''}`;
  return (
    <View>
      <Text variant="h1" accessibilityRole="header">
        {t('screen.LMS27.title')}
      </Text>
      {problem ? <Banner intent="danger">{problem}</Banner> : null}
      {classes.isError ? <Banner intent="danger">{t('common.error.generic')}</Banner> : null}
      {classes.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
      {(classes.data?.items ?? []).map((liveClass) => (
        <View key={String(liveClass.id)} style={{ paddingVertical: theme.space[3], gap: theme.space[3] }}>
          <Text>
            {liveClass.title} — {line(liveClass)}
            {liveClass.endedAt ? ` (${t('lms.live.ended')})` : ''}
          </Text>
          <View style={{ flexDirection: 'row', gap: theme.space[3], flexWrap: 'wrap' }}>
            {mayJoin && !liveClass.endedAt ? (
              <Button
                onPress={() =>
                  join.mutate({ liveClassId: String(liveClass.id) }, { onSuccess: openLink, onError: fail })
                }
                disabled={join.isPending}
              >
                {t('lms.live.join')}
              </Button>
            ) : null}
            {mayStart && !liveClass.endedAt ? (
              <Button
                variant="secondary"
                onPress={() =>
                  startUrl.mutate(
                    { liveClassId: String(liveClass.id) },
                    { onSuccess: openLink, onError: fail },
                  )
                }
                disabled={startUrl.isPending}
              >
                {t('lms.live.start')}
              </Button>
            ) : null}
            {mayEnd && !liveClass.endedAt ? (
              <Button
                variant="secondary"
                onPress={() =>
                  step.mutate({ liveClassId: String(liveClass.id), step: 'end' }, { onError: fail })
                }
                disabled={step.isPending}
              >
                {t('lms.live.end')}
              </Button>
            ) : null}
          </View>
        </View>
      ))}
      {classes.data && classes.data.items.length === 0 ? <Text muted>{t('lms.live.none')}</Text> : null}
    </View>
  );
}

function Outline({ cohortId }: { cohortId: string }) {
  const { t, format } = useI18n();
  const outline = useCohortOutline(cohortId || null);
  return (
    <View>
      <Text variant="h1" accessibilityRole="header">
        {t('screen.LMS22.title')}
      </Text>
      {outline.isError ? <Banner intent="danger">{t('common.error.generic')}</Banner> : null}
      {outline.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
      {(outline.data?.items ?? []).map((item) => (
        <Text key={String(item.id)}>
          {item.title ?? String(item.itemKind)} ({String(item.itemKind)})
          {item.dueAt ? ` · ${t('lms.outline.due')} ${format.day(item.dueAt)}` : ''}
        </Text>
      ))}
      {outline.data && outline.data.items.length === 0 ? <Text muted>{t('lms.outline.empty')}</Text> : null}
    </View>
  );
}

function Certificates() {
  const { t, format } = useI18n();
  const theme = useTheme();
  const me = useSession((s) => s.me?.user?.id ?? '');
  const certificates = useCertificates(me || undefined);
  const badges = useBadgeAwards(me || undefined);
  const download = useDownloadCertificate();
  const mayDownload = useCan('lms.certificate.download_certificate');
  const [problem, setProblem] = useState<string | null>(null);
  return (
    <View>
      <Text variant="h1" accessibilityRole="header">
        {t('screen.LMS15.title')}
      </Text>
      {problem ? <Banner intent="danger">{problem}</Banner> : null}
      {certificates.isError ? <Banner intent="danger">{t('common.error.generic')}</Banner> : null}
      {certificates.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
      {(certificates.data?.items ?? []).map((certificate) => (
        <View key={String(certificate.id)} style={{ paddingVertical: theme.space[3] }}>
          <Text>
            {certificate.certificateNumber ?? String(certificate.id)} ·{' '}
            {certificate.issueDate ? format.day(certificate.issueDate) : ''}
          </Text>
          {mayDownload ? (
            <Button
              variant="secondary"
              disabled={download.isPending}
              onPress={() =>
                download.mutate(
                  { certificateId: String(certificate.id) },
                  {
                    onSuccess: (link) => {
                      if (link.url) void Linking.openURL(link.url);
                    },
                    onError: (error) => setProblem(lmsFailureText(t, error)),
                  },
                )
              }
            >
              {t('lms.certificates.download')}
            </Button>
          ) : null}
        </View>
      ))}
      {certificates.data && certificates.data.items.length === 0 ? (
        <Text muted>{t('lms.certificates.none')}</Text>
      ) : null}
      <Text variant="h2" accessibilityRole="header">
        {t('lms.certificates.badges')}
      </Text>
      {(badges.data?.items ?? []).map((award) => (
        <Text key={String(award.id)}>
          {String(award.badgeId)} · {award.issuedOn ? format.day(award.issuedOn) : ''}
        </Text>
      ))}
      {badges.data && badges.data.items.length === 0 ? (
        <Text muted>{t('lms.certificates.noBadges')}</Text>
      ) : null}
    </View>
  );
}
