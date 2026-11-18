import {
  RESOURCES,
  can,
  useInfiniteRecords,
  useRecord,
  useI18n,
  useSession,
  type ClientKind,
  type PermissionCode,
  type RecordRow,
  type ResourceDef,
} from '@mysms/core';
import { enumLabel, type MessageKey } from '@mysms/i18n';
import { Banner, Button, Screen, Text, useTheme } from '@mysms/ui-native';
import { useMemo, useState } from 'react';
import { View } from 'react-native';
import type { AssessmentRoute, AssessmentScreenId } from './assessmentRoutes';
import {
  AssignmentWorkflow,
  AssignmentSubmissionsPanel,
  AssignmentSubmission,
} from './assessment/AssignmentScreens';
import {
  ExamSessionWorkflow,
  ExamSessionExamsPanel,
  ExamWorkflow,
  ExamMarks,
} from './assessment/ExamScreens';
import { HomeworkReview, HomeworkSubmission } from './assessment/HomeworkScreens';
import {
  MarksheetWorkflow,
  ResultTemplatePanel,
  StudentResults,
  MarksheetDetails,
  GradingSchemeBands,
} from './assessment/ResultScreens';
import {
  RecordFields,
  WorkflowActions,
  QueryFeedback,
  RecordEditor,
  RecordDelete,
  LoadingRows,
} from './records';
import { firstText } from './recordText';

const RECORD_OF_SCREEN: Partial<Record<AssessmentScreenId, string>> = {
  'ASM-01': 'assessment.Homework',
  'ASM-02': 'assessment.Homework',
  'ASM-05': 'assessment.Assignment',
  'ASM-08': 'assessment.ExamSession',
  'ASM-09': 'assessment.Exam',
  'ASM-11': 'assessment.Marksheet',
  'ASM-13': 'assessment.GradingScheme',
};

const LIST_DETAIL_PARAM: Partial<Record<AssessmentScreenId, string>> = {
  'ASM-03': 'homeworkId',
  'ASM-04': 'homeworkId',
  'ASM-06': 'assignmentId',
  'ASM-07': 'assignmentId',
  'ASM-08': 'examSessionId',
  'ASM-09': 'examId',
  'ASM-10': 'examId',
  'ASM-11': 'marksheetId',
  'ASM-12': 'studentId',
  'ASM-13': 'gradingSchemeId',
};

/** The permissions that make a caller a reviewer of assignment submissions (a teacher). */
const ASSIGNMENT_REVIEW = [
  'assessment.assignment_submission.grade_assignment_submission',
  'assessment.assignment_submission.accept_assignment_submission',
  'assessment.assignment_submission.reject_assignment_submission',
  'assessment.assignment_submission.request_submission_changes',
] as PermissionCode[];

const titleKey = (id: AssessmentScreenId) => `screen.${id.replace('-', '')}.title` as MessageKey;

/** One platform-neutral assessment workspace, selected by the shared screen id and deep-link parameters. */
export function AssessmentScreen({
  route,
  client,
  tablet = false,
  onBack,
  onNavigate,
}: {
  route: AssessmentRoute;
  client: ClientKind;
  tablet?: boolean;
  onBack: () => void;
  onNavigate: (route: AssessmentRoute) => void;
}) {
  const { t } = useI18n();
  const screenTitle = t(titleKey(route.screenId));
  return (
    <Screen tablet={tablet}>
      <Text variant="h1" accessibilityRole="header">
        {screenTitle}
      </Text>
      <Button variant="secondary" onPress={onBack}>
        {t('common.action.back')}
      </Button>
      <AssessmentContent route={route} client={client} tablet={tablet} onNavigate={onNavigate} />
    </Screen>
  );
}

function AssessmentContent({
  route,
  client,
  tablet,
  onNavigate,
}: {
  route: AssessmentRoute;
  client: ClientKind;
  tablet: boolean;
  onNavigate: (route: AssessmentRoute) => void;
}) {
  switch (route.screenId) {
    case 'ASM-01':
    case 'ASM-02':
    case 'ASM-05':
    case 'ASM-08':
    case 'ASM-09':
    case 'ASM-11':
    case 'ASM-13':
      return <AssessmentResourceList route={route} client={client} tablet={tablet} onNavigate={onNavigate} />;
    case 'ASM-03':
      return <HomeworkReview route={route} tablet={tablet} />;
    case 'ASM-04':
      return <HomeworkSubmission route={route} tablet={tablet} />;
    case 'ASM-06':
      return <AssignmentReview route={route} tablet={tablet} />;
    case 'ASM-07':
      return <AssignmentSubmission route={route} tablet={tablet} />;
    case 'ASM-10':
      return <ExamMarks route={route} tablet={tablet} />;
    case 'ASM-12':
      return <StudentResults route={route} tablet={tablet} />;
  }
}

function AssessmentResourceList({
  route,
  client,
  tablet,
  onNavigate,
}: {
  route: AssessmentRoute;
  client: ClientKind;
  tablet: boolean;
  onNavigate: (route: AssessmentRoute) => void;
}) {
  const { t } = useI18n();
  const permissions = useSession((state) => state.me?.permissions);
  const key = RECORD_OF_SCREEN[route.screenId];
  const def = useMemo(() => RESOURCES.find((resource) => resource.key === key), [key]);
  const listPermission = def?.ops.List?.permission;
  const hasRead = Boolean(listPermission && can(permissions, listPermission as PermissionCode));
  const detailParam = def ? LIST_DETAIL_PARAM[route.screenId] : undefined;
  const detailValue = detailParam ? route.params[detailParam] : undefined;
  const filter = route.screenId === 'ASM-02' ? 'status = "pending"' : undefined;
  const list = useInfiniteRecords(hasRead ? def : undefined, {
    size: tablet ? 50 : 25,
    ...(filter ? { filter } : {}),
  });
  const [creating, setCreating] = useState(false);
  const rows = list.data?.pages.flatMap((page) => page.data) ?? [];
  const student = (useSession((state) => state.me?.studentIds?.length) ?? 0) > 0;
  const target = detailTarget(route.screenId, client, permissions, student);
  const mayCreate = Boolean(def?.ops.Create && can(permissions, def.ops.Create.permission as PermissionCode));

  if (!def || !hasRead) return <Banner>{t('adm.resource.noAccess')}</Banner>;

  if (detailValue)
    return <RecordDetail def={def} id={detailValue} route={route} tablet={tablet} onNavigate={onNavigate} />;

  return (
    <>
      {mayCreate && !creating ? (
        <Button variant="secondary" onPress={() => setCreating(true)}>
          {t('adm.records.new')}
        </Button>
      ) : null}
      {creating && def ? (
        <RecordEditor
          def={def}
          tablet={tablet}
          onDone={() => {
            setCreating(false);
            void list.refetch();
          }}
          onCancel={() => setCreating(false)}
        />
      ) : null}
      <QueryFeedback query={list} hasCachedRows={rows.length > 0} onRetry={() => void list.refetch()} />
      {list.isPending ? <LoadingRows tablet={tablet} /> : null}
      {!list.isPending && !list.isError && rows.length === 0 ? (
        <Banner>{t('common.state.empty')}</Banner>
      ) : null}
      {rows.map((row) => (
        <ResourceCard
          key={row.id}
          row={row}
          tablet={tablet}
          onOpen={() => {
            const param = detailParam ?? defaultDetailParam(route.screenId);
            if (target && param) onNavigate({ screenId: target, params: { [param]: row.id } });
          }}
          openLabel={t('asm.native.open')}
        />
      ))}
      {list.hasNextPage ? (
        <Button
          variant="secondary"
          loading={list.isFetchingNextPage}
          onPress={() => void list.fetchNextPage()}
        >
          {t('common.list.more')}
        </Button>
      ) : null}
      {route.screenId === 'ASM-11' ? <ResultTemplatePanel tablet={tablet} onNavigate={onNavigate} /> : null}
    </>
  );
}

/**
 * Where a row of a list opens. A student (the signed-in person has a student record) who does not review opens their
 * own submission screens; the read grants alone do not tell a student from a teacher.
 */
function detailTarget(
  screenId: AssessmentScreenId,
  client: ClientKind,
  permissions: readonly string[] | undefined,
  student: boolean,
): AssessmentScreenId | null {
  const checks = can(
    permissions,
    'assessment.homework_submission.check_homework_submission' as PermissionCode,
  );
  const reviews = ASSIGNMENT_REVIEW.some((code) => can(permissions, code));
  if (screenId === 'ASM-01') {
    if (student && !checks) return 'ASM-04';
    if (
      client === 'T' ||
      can(permissions, 'assessment.homework_submission.check_homework_submission' as PermissionCode) ||
      can(permissions, 'assessment.homework.approve_homework' as PermissionCode)
    )
      return 'ASM-03';
    return 'ASM-01';
  }
  if (screenId === 'ASM-02') return 'ASM-03';
  if (screenId === 'ASM-05') return student && !reviews ? 'ASM-07' : 'ASM-06';
  if (screenId === 'ASM-08') return 'ASM-08';
  if (screenId === 'ASM-09') return 'ASM-09';
  if (screenId === 'ASM-11') return 'ASM-11';
  if (screenId === 'ASM-13') return 'ASM-13';
  return null;
}

function defaultDetailParam(screenId: AssessmentScreenId): string | undefined {
  if (screenId === 'ASM-01' || screenId === 'ASM-02') return 'homeworkId';
  if (screenId === 'ASM-05') return 'assignmentId';
  return LIST_DETAIL_PARAM[screenId];
}

function ResourceCard({
  row,
  tablet,
  onOpen,
  openLabel,
}: {
  row: RecordRow;
  tablet: boolean;
  onOpen: () => void;
  openLabel: string;
}) {
  const { t } = useI18n();
  const theme = useTheme();
  const heading = firstText(row, ['title', 'name', 'code', 'subject', 'label']) ?? row.id;
  const subtitle = firstText(row, ['description', 'examDate', 'dueAt', 'startAt', 'issuedOn']);
  const status = typeof row.status === 'string' ? row.status : undefined;
  return (
    <View
      style={{
        padding: theme.space[5],
        marginTop: theme.space[4],
        borderWidth: 1,
        borderRadius: theme.radius.md,
        borderColor: theme.colors.border,
        backgroundColor: theme.colors.surface,
        width: tablet ? '48%' : '100%',
      }}
    >
      <Text variant="h3">{heading}</Text>
      {subtitle ? <Text muted>{subtitle}</Text> : null}
      {status ? <Text variant="caption">{enumLabel(t, 'assessment.status', status)}</Text> : null}
      <Button variant="secondary" onPress={onOpen}>
        {openLabel}
      </Button>
    </View>
  );
}

function RecordDetail({
  def,
  id,
  route,
  tablet,
  onNavigate,
}: {
  def: ResourceDef;
  id: string;
  route: AssessmentRoute;
  tablet: boolean;
  onNavigate: (route: AssessmentRoute) => void;
}) {
  const { t } = useI18n();
  const permissions = useSession((state) => state.me?.permissions);
  const [editing, setEditing] = useState(false);
  const record = useRecord(def, id);
  const name = record.data ? (firstText(record.data.data, ['title', 'name', 'code', 'subject']) ?? id) : id;
  const mayUpdate = Boolean(def.ops.Update && can(permissions, def.ops.Update.permission as PermissionCode));
  const mayDelete = Boolean(def.ops.Delete && can(permissions, def.ops.Delete.permission as PermissionCode));
  return (
    <>
      <QueryFeedback
        query={record}
        hasCachedRows={Boolean(record.data)}
        onRetry={() => void record.refetch()}
      />
      {record.isPending ? <LoadingRows tablet={tablet} /> : null}
      {record.data ? (
        <>
          <Text variant="h2">{name}</Text>
          {editing ? (
            <RecordEditor
              def={def}
              row={record.data.data}
              etag={record.data.etag}
              tablet={tablet}
              onDone={() => {
                setEditing(false);
                void record.refetch();
              }}
              onCancel={() => setEditing(false)}
            />
          ) : (
            <RecordFields row={record.data.data} def={def} />
          )}
          {mayUpdate && !editing ? (
            <Button variant="secondary" onPress={() => setEditing(true)}>
              {t('adm.records.edit')}
            </Button>
          ) : null}
          {mayDelete ? (
            <RecordDelete
              def={def}
              row={record.data.data}
              etag={record.data.etag}
              onDeleted={() => onNavigate({ screenId: route.screenId, params: {} })}
            />
          ) : null}
          {def.key === 'assessment.Assignment' ? (
            <AssignmentWorkflow row={record.data.data} etag={record.data.etag ?? undefined} tablet={tablet} />
          ) : def.key === 'assessment.ExamSession' ? (
            <ExamSessionWorkflow row={record.data.data} etag={record.data.etag ?? undefined} />
          ) : def.key === 'assessment.Exam' ? (
            <ExamWorkflow row={record.data.data} etag={record.data.etag ?? undefined} />
          ) : def.key === 'assessment.Marksheet' ? (
            <MarksheetWorkflow row={record.data.data} etag={record.data.etag ?? undefined} />
          ) : (
            <WorkflowActions
              def={def}
              row={record.data.data}
              etag={record.data.etag}
              onReload={() => void record.refetch()}
            />
          )}
          {route.screenId === 'ASM-09' ? (
            <Button
              variant="secondary"
              onPress={() => onNavigate({ screenId: 'ASM-10', params: { examId: id } })}
            >
              {t('screen.ASM10.title')}
            </Button>
          ) : null}
          {route.screenId === 'ASM-08' ? (
            <ExamSessionExamsPanel examSessionId={id} onNavigate={onNavigate} />
          ) : null}
          {route.screenId === 'ASM-06' ? (
            <AssignmentSubmissionsPanel assignmentId={id} tablet={tablet} />
          ) : null}
          {route.screenId === 'ASM-11' ? <MarksheetDetails marksheetId={id} tablet={tablet} /> : null}
          {route.screenId === 'ASM-13' ? <GradingSchemeBands gradingSchemeId={id} tablet={tablet} /> : null}
        </>
      ) : null}
    </>
  );
}

function AssignmentReview({ route, tablet }: { route: AssessmentRoute; tablet: boolean }) {
  return (
    <RecordDetailRoute recordKey="assessment.Assignment" param="assignmentId" route={route} tablet={tablet} />
  );
}

function RecordDetailRoute({
  recordKey,
  param,
  route,
  tablet,
}: {
  recordKey: string;
  param: string;
  route: AssessmentRoute;
  tablet: boolean;
}) {
  const { t } = useI18n();
  const permissions = useSession((state) => state.me?.permissions);
  const def = RESOURCES.find((resource) => resource.key === recordKey);
  const id = route.params[param];
  if (!def || !id) return <Banner>{t('common.state.notAvailable')}</Banner>;
  if (!def.ops.Get || !can(permissions, def.ops.Get.permission as PermissionCode))
    return <Banner>{t('adm.resource.noAccess')}</Banner>;
  return <RecordDetail def={def} id={id} route={route} tablet={tablet} onNavigate={() => undefined} />;
}
