import {
  lmsFailureText,
  isApiError,
  useAssignmentGroup,
  useAssignmentGroupMembers,
  useCan,
  useGroupWorkspaceSteps,
  useI18n,
  useSession,
  type AssignmentGroupMember,
} from '@mysms/core';
import { Banner, Button, Card, Skeleton, StatusChip, TextInput } from '@mysms/ui-web';
import { useState } from 'react';
import { useParams, useSearchParams } from 'react-router';
import { UUID } from '../ids';

/**
 * LMS-11 Group workspace: one assignment group — who is in it, who asked to join, the steps the role holds
 * (join, accept or reject a request, set the members outright) and the peer review at the end. The link into the
 * workspace carries the placement's id, which the group's own operations need.
 */
export function GroupWorkspaceScreen() {
  const { t } = useI18n();
  const { assignmentGroupId: param = '' } = useParams();
  const [search] = useSearchParams();
  const itemParam = search.get('contentItemId') ?? '';
  const mayRead = useCan('lms.assignment_group_member.read');
  const assignmentGroupId = mayRead && UUID.test(param) ? param : null;
  const contentItemId = UUID.test(itemParam) ? itemParam : null;
  const mayReadGroup = useCan('lms.assignment_group.read');
  const group = useAssignmentGroup(mayReadGroup ? contentItemId : null, assignmentGroupId);
  const members = useAssignmentGroupMembers(assignmentGroupId);
  const title = t('screen.LMS11.title');
  if (!mayRead)
    return (
      <Card title={title}>
        <p className="hint">{t('lms.common.forbidden')}</p>
      </Card>
    );
  if (assignmentGroupId === null || (isApiError(members.error) && members.error.isNotAvailable))
    return (
      <Card title={title}>
        <p>{t('common.state.notAvailable')}</p>
      </Card>
    );
  if (members.isPending) return <Skeleton label={t('common.state.loading')} lines={5} />;
  if (members.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void members.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    );
  return (
    <WorkspacePanel
      assignmentGroupId={assignmentGroupId}
      contentItemId={contentItemId ?? members.data.items[0]?.contentItemId ?? null}
      groupName={group.data?.name ?? title}
      members={members.data.items}
    />
  );
}

function WorkspacePanel({
  assignmentGroupId,
  contentItemId,
  groupName,
  members,
}: {
  assignmentGroupId: string;
  contentItemId: string | null;
  groupName: string;
  members: AssignmentGroupMember[];
}) {
  const { t } = useI18n();
  const me = useSession((s) => s.me?.user?.id);
  const steps = useGroupWorkspaceSteps();
  const mayJoin = useCan('lms.assignment_group.request_to_join_group');
  const mayDecide = useCan('lms.assignment_group_member.accept_group_request');
  const maySetMembers = useCan('lms.assignment_group.set_group_members');
  const mayReview = useCan('lms.assignment_group.submit_peer_review');
  const [reviewee, setReviewee] = useState('');
  const [points, setPoints] = useState('');
  const mine = members.find((member) => member.userId === me);
  const failure =
    steps.requestToJoin.error ?? steps.decide.error ?? steps.setMembers.error ?? steps.submitPeerReview.error;
  return (
    <Card title={groupName}>
      {contentItemId === null && <Banner intent="info">{t('lms.group.noItemLink')}</Banner>}
      {failure !== undefined && failure !== null && (
        <Banner intent="danger">{lmsFailureText(t, failure)}</Banner>
      )}
      <ul aria-label={t('lms.group.members')}>
        {members.map((member) => (
          <li key={member.id}>
            {member.userId}{' '}
            <StatusChip
              intent={member.status === 'active' ? 'success' : 'neutral'}
              label={member.status ?? ''}
            />
            {mayDecide && member.status === 'requested' && member.id && (
              <span role="group" aria-label={t('lms.group.decide')}>
                <Button
                  variant="secondary"
                  onClick={() =>
                    steps.decide.mutate({
                      assignmentGroupMemberId: member.id as string,
                      assignmentGroupId,
                      accept: true,
                    })
                  }
                  disabled={steps.decide.isPending}
                >
                  {t('lms.group.accept')}
                </Button>
                <Button
                  variant="secondary"
                  onClick={() =>
                    steps.decide.mutate({
                      assignmentGroupMemberId: member.id as string,
                      assignmentGroupId,
                      accept: false,
                    })
                  }
                  disabled={steps.decide.isPending}
                >
                  {t('lms.group.reject')}
                </Button>
              </span>
            )}
          </li>
        ))}
        {members.length === 0 && <li className="hint">{t('common.state.empty')}</li>}
      </ul>
      {mayJoin &&
        (mine === undefined || mine === null) &&
        contentItemId !== undefined &&
        contentItemId !== null && (
          <Button
            onClick={() => steps.requestToJoin.mutate({ contentItemId, assignmentGroupId })}
            disabled={steps.requestToJoin.isPending}
          >
            {t('lms.group.join')}
          </Button>
        )}
      {maySetMembers && contentItemId !== undefined && contentItemId !== null && (
        <p className="hint">{t('lms.group.setMembersHint')}</p>
      )}
      {mayReview && contentItemId !== undefined && contentItemId !== null && mine?.status === 'active' && (
        <section aria-label={t('lms.group.peerReview')}>
          <h2>{t('lms.group.peerReview')}</h2>
          <TextInput
            label={t('lms.group.reviewee')}
            value={reviewee}
            onChange={(event) => setReviewee(event.target.value)}
            error={reviewee !== '' && !UUID.test(reviewee) ? t('common.validation.pattern') : undefined}
          />
          <TextInput
            label={t('lms.group.points')}
            inputMode="decimal"
            value={points}
            onChange={(event) => setPoints(event.target.value)}
          />
          <Button
            onClick={() =>
              steps.submitPeerReview.mutate({
                contentItemId,
                assignmentGroupId,
                request: { reviews: [{ userId: reviewee, points: points.trim() }] },
              })
            }
            disabled={steps.submitPeerReview.isPending || !UUID.test(reviewee) || points.trim() === ''}
          >
            {t('lms.group.sendReview')}
          </Button>
        </section>
      )}
    </Card>
  );
}
