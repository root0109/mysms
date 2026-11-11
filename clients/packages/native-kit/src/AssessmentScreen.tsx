import {
  RESOURCES,
  actionAllowed,
  can,
  useAllocateExamRooms,
  useAssignmentStep,
  useAssignmentSubmissionStep,
  useAssignmentSubmissions,
  useCheckHomeworkSubmission,
  useCreateHomeworkSubmission,
  useCreateRecord,
  useDeleteRecord,
  useExamResults,
  useExamSessionStep,
  useExamStep,
  useGradingBands,
  useHomework,
  useHomeworkStep,
  useHomeworkSubmissions,
  useInfiniteRecords,
  useMarksheetLines,
  useMarksheetStep,
  usePopulateExamResults,
  useMyStudent,
  useRecord,
  useRecordAction,
  useRecordExamMarks,
  useReplyToHomeworkFeedback,
  useResultTemplateGenerateResults,
  useStudentNames,
  useStudentResults,
  useUpdateRecord,
  useOperation,
  useCan,
  isApiError,
  useI18n,
  useSession,
  type ClientKind,
  type Assignment,
  type AssignmentStep,
  type AssignmentSubmissionStep,
  type Exam,
  type ExamSessionStep,
  type ExamStep,
  type ExamResult,
  type GradingBand,
  type MarkEntry,
  type MarksheetLine,
  type ResultCard,
  type HomeworkSubmission as HomeworkSubmissionData,
  type AssignmentSubmission as AssignmentSubmissionData,
  type PermissionCode,
  type Operation,
  type RecordRow,
  type ResourceDef,
} from '@mysms/core';
import { enumLabel, type MessageKey } from '@mysms/i18n';
import { Banner, Button, ChoiceGroup, Screen, SwitchRow, Text, TextInput, useTheme } from '@mysms/ui-native';
import { useMemo, useState } from 'react';
import { View } from 'react-native';
import type { AssessmentRoute, AssessmentScreenId } from './assessmentRoutes';

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
  const target = detailTarget(route.screenId, client, permissions);
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

function detailTarget(
  screenId: AssessmentScreenId,
  client: ClientKind,
  permissions: readonly string[] | undefined,
): AssessmentScreenId | null {
  if (screenId === 'ASM-01') {
    if (can(permissions, 'assessment.homework_submission.create' as PermissionCode)) return 'ASM-04';
    if (
      client === 'T' ||
      can(permissions, 'assessment.homework_submission.check_homework_submission' as PermissionCode) ||
      can(permissions, 'assessment.homework.approve_homework' as PermissionCode)
    )
      return 'ASM-03';
    return 'ASM-01';
  }
  if (screenId === 'ASM-02') return 'ASM-03';
  if (screenId === 'ASM-05') {
    return can(permissions, 'assessment.assignment_submission.create' as PermissionCode)
      ? 'ASM-07'
      : 'ASM-06';
  }
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
            <AssignmentWorkflow row={record.data.data} etag={record.data.etag} tablet={tablet} />
          ) : def.key === 'assessment.ExamSession' ? (
            <ExamSessionWorkflow row={record.data.data} etag={record.data.etag} />
          ) : def.key === 'assessment.Exam' ? (
            <ExamWorkflow row={record.data.data} etag={record.data.etag} />
          ) : def.key === 'assessment.Marksheet' ? (
            <MarksheetWorkflow row={record.data.data} etag={record.data.etag} />
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

function RecordFields({ row, def }: { row: RecordRow; def: ResourceDef }) {
  const { t } = useI18n();
  return (
    <View>
      {def.fields
        .filter((field) => !field.writeOnly && field.name in row && field.kind !== 'id')
        .map((field) => {
          const value = row[field.name];
          if (value === null || value === undefined || typeof value === 'object') return null;
          const display =
            field.kind === 'enum'
              ? (field.valueLabels?.[field.values?.indexOf(String(value)) ?? -1] ??
                enumLabel(t, `${def.key}.${field.name}`, String(value)))
              : field.name === 'status' && def.module === 'assessment'
                ? enumLabel(t, 'assessment.status', String(value))
                : String(value);
          return (
            <Text key={field.name}>
              {field.label}: {display}
            </Text>
          );
        })}
    </View>
  );
}

function WorkflowActions({
  def,
  row,
  etag,
  onReload,
}: {
  def: ResourceDef;
  row: RecordRow;
  etag: string | null;
  onReload: () => void;
}) {
  const { t } = useI18n();
  const permissions = useSession((state) => state.me?.permissions);
  const theme = useTheme();
  const actionMutation = useRecordAction(def);
  const [reason, setReason] = useState('');
  const available = (def.actions ?? []).filter(
    (action) => can(permissions, action.permission as PermissionCode) && actionAllowed(action, row),
  );
  const conflict = isApiError(actionMutation.error) && actionMutation.error.isConflict;
  return (
    <View style={{ marginTop: theme.space[5], flexDirection: 'row', flexWrap: 'wrap', gap: theme.space[3] }}>
      {available.some((action) => action.reason) ? (
        <TextInput
          label={t('adm.records.reason')}
          value={reason}
          onChangeText={setReason}
          multiline
          style={{ minHeight: 88, width: '100%' }}
        />
      ) : null}
      {available.map((action) => (
        <Button
          key={action.operation}
          variant="secondary"
          loading={actionMutation.isPending}
          disabled={action.ifMatch && !etag}
          onPress={() =>
            actionMutation.mutate(
              {
                action,
                id: row.id,
                etag,
                ...(action.reason && reason.trim() ? { reason: reason.trim() } : {}),
              },
              { onSuccess: () => setReason('') },
            )
          }
        >
          {action.label}
        </Button>
      ))}
      {actionMutation.isError ? (
        <>
          <Banner intent="danger">
            {conflict
              ? t('common.conflict.body')
              : isApiError(actionMutation.error)
                ? actionMutation.error.title
                : t('common.error.generic')}
          </Banner>
          <Button
            variant="secondary"
            onPress={() => {
              actionMutation.reset();
              onReload();
            }}
          >
            {conflict ? t('common.conflict.reload') : t('common.action.retry')}
          </Button>
        </>
      ) : null}
    </View>
  );
}

function QueryFeedback({
  query,
  hasCachedRows,
  onRetry,
}: {
  query: { isPending: boolean; isError: boolean; error: unknown };
  hasCachedRows: boolean;
  onRetry: () => void;
}) {
  const { t } = useI18n();
  if (!query.isError) return null;
  const offline = isNetworkFailure(query.error);
  const error = query.error;
  return (
    <>
      {offline ? (
        <Banner>{hasCachedRows ? t('common.state.offline') : t('common.error.generic')}</Banner>
      ) : null}
      {!offline ? (
        <Banner intent="danger">
          {isApiError(error) ? `${error.title} (${error.code})` : t('common.error.generic')}
        </Banner>
      ) : null}
      <Button variant="secondary" onPress={onRetry}>
        {t('common.action.retry')}
      </Button>
    </>
  );
}

function WorkflowNotice({ mutation }: { mutation: MutationFeedback }) {
  const { t } = useI18n();
  if (!mutation.isError) return null;
  const conflict = isApiError(mutation.error) && mutation.error.isConflict;
  return (
    <View>
      <Banner intent="danger">
        {conflict
          ? t('common.conflict.body')
          : isApiError(mutation.error)
            ? `${mutation.error.title} (${mutation.error.code})`
            : t('common.error.generic')}
      </Banner>
      <Button variant="secondary" onPress={mutation.reset}>
        {t('common.action.close')}
      </Button>
    </View>
  );
}

function examMarkRowErrors(error: unknown, studentIds: readonly string[]): Record<string, string[]> {
  if (!isApiError(error)) return {};
  const byStudent: Record<string, string[]> = {};
  for (const fieldError of error.errors) {
    const match = /^entries\[(\d+)\](?:\.|$)/.exec(fieldError.field);
    const studentId = match ? studentIds[Number(match[1])] : undefined;
    if (!studentId) continue;
    (byStudent[studentId] ??= []).push(fieldError.message);
  }
  return byStudent;
}

interface MutationFeedback {
  isError: boolean;
  isPending: boolean;
  error: unknown;
  reset: () => void;
}

function RecordEditor({
  def,
  row,
  etag,
  initialValues,
  tablet = false,
  onDone,
  onCancel,
}: {
  def: ResourceDef;
  row?: RecordRow;
  etag?: string | null;
  initialValues?: Record<string, unknown>;
  tablet?: boolean;
  onDone: () => void;
  onCancel: () => void;
}) {
  const { t } = useI18n();
  const theme = useTheme();
  const create = useCreateRecord(def);
  const update = useUpdateRecord(def);
  const [values, setValues] = useState<Record<string, unknown>>(() =>
    Object.fromEntries(
      def.fields.map((field) => [
        field.name,
        row?.[field.name] ??
          initialValues?.[field.name] ??
          (field.kind === 'boolean' ? (field.defaultValue ?? false) : ''),
      ]),
    ),
  );
  const [missing, setMissing] = useState<string[]>([]);
  const pending = create.isPending || update.isPending;
  const error = create.error ?? update.error;
  const isNew = !row;
  const editable = (field: ResourceDef['fields'][number]) =>
    !field.readOnly &&
    (isNew ? field.name !== def.parent : field.writable === true && field.name !== def.parent);
  const save = () => {
    const absent = def.fields
      .filter((field) => isNew && field.required && editable(field) && field.kind !== 'boolean')
      .filter((field) => values[field.name] === '' || values[field.name] === undefined)
      .map((field) => field.name);
    setMissing(absent);
    if (absent.length) return;
    if (isNew) create.mutate({ values }, { onSuccess: onDone });
    else if (row && etag) update.mutate({ id: row.id, etag, values }, { onSuccess: onDone });
  };
  return (
    <View
      style={{
        padding: theme.space[5],
        marginTop: theme.space[4],
        borderWidth: 1,
        borderColor: theme.colors.border,
        borderRadius: theme.radius.md,
      }}
    >
      <Text variant="h3">{t(isNew ? 'adm.records.new' : 'adm.records.edit')}</Text>
      {error ? (
        <Banner intent="danger">
          {isApiError(error) && error.isConflict
            ? t('common.conflict.body')
            : isApiError(error)
              ? `${error.title} (${error.code})`
              : t('common.error.generic')}
        </Banner>
      ) : null}
      <View style={{ flexDirection: 'row', flexWrap: 'wrap', columnGap: theme.space[4] }}>
        {def.fields.filter(editable).map((field) => {
          const fieldError = missing.includes(field.name) ? t('common.validation.required') : undefined;
          const width = tablet ? '48%' : '100%';
          if (field.kind === 'boolean')
            return (
              <View key={field.name} style={{ width }}>
                <SwitchRow
                  label={field.label}
                  value={values[field.name] === true}
                  onChange={(value) => setValues((old) => ({ ...old, [field.name]: value }))}
                />
              </View>
            );
          if (field.kind === 'enum' && field.values)
            return (
              <View key={field.name} style={{ width }}>
                <ChoiceGroup
                  label={field.label}
                  value={String(values[field.name] ?? '')}
                  choices={[
                    { value: '', label: t('common.state.notAvailable') },
                    ...field.values.map((value, index) => ({
                      value,
                      label: field.valueLabels?.[index] ?? enumLabel(t, `${def.key}.${field.name}`, value),
                    })),
                  ]}
                  onChange={(value) => setValues((old) => ({ ...old, [field.name]: value }))}
                />
                {fieldError ? <Text variant="caption">{fieldError}</Text> : null}
              </View>
            );
          return (
            <View key={field.name} style={{ width }}>
              <TextInput
                label={field.label}
                value={String(values[field.name] ?? '')}
                onChangeText={(value) => setValues((old) => ({ ...old, [field.name]: value }))}
                editable={!field.readOnly}
                maxLength={field.maxLength}
                keyboardType={
                  field.kind === 'integer' || field.kind === 'decimal' ? 'decimal-pad' : 'default'
                }
                autoCapitalize="none"
                error={fieldError}
              />
            </View>
          );
        })}
      </View>
      <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: theme.space[3] }}>
        <Button loading={pending} disabled={!isNew && !etag} onPress={save}>
          {t('common.action.save')}
        </Button>
        <Button variant="secondary" onPress={onCancel}>
          {t('common.action.cancel')}
        </Button>
      </View>
    </View>
  );
}

function RecordDelete({
  def,
  row,
  etag,
  onDeleted,
}: {
  def: ResourceDef;
  row: RecordRow;
  etag: string | null;
  onDeleted: () => void;
}) {
  const { t } = useI18n();
  const remove = useDeleteRecord(def);
  const [confirm, setConfirm] = useState(false);
  return (
    <View>
      {!confirm ? (
        <Button variant="secondary" onPress={() => setConfirm(true)}>
          {t('common.action.delete')}
        </Button>
      ) : (
        <>
          <Text>{t('adm.records.confirmDelete')}</Text>
          <Button
            loading={remove.isPending}
            disabled={!etag}
            onPress={() => remove.mutate({ id: row.id, etag: etag ?? '' }, { onSuccess: onDeleted })}
          >
            {t('common.action.delete')}
          </Button>
          <Button variant="secondary" onPress={() => setConfirm(false)}>
            {t('common.action.cancel')}
          </Button>
        </>
      )}
      <WorkflowNotice mutation={remove} />
    </View>
  );
}

function LoadingRows({ tablet }: { tablet: boolean }) {
  const { t } = useI18n();
  const theme = useTheme();
  return (
    <View
      accessibilityRole="progressbar"
      accessibilityLabel={t('common.state.loading')}
      style={{ flexDirection: 'row', flexWrap: 'wrap', gap: theme.space[4], marginTop: theme.space[5] }}
    >
      {Array.from({ length: tablet ? 4 : 2 }, (_, index) => (
        <View
          key={index}
          style={{
            height: tablet ? 120 : 96,
            width: tablet ? '48%' : '100%',
            borderRadius: theme.radius.md,
            backgroundColor: theme.colors.surface2,
          }}
        />
      ))}
    </View>
  );
}

function firstText(row: RecordRow, fields: readonly string[]): string | undefined {
  for (const field of fields) if (typeof row[field] === 'string' && row[field]) return row[field] as string;
  return undefined;
}

function isNetworkFailure(error: unknown): boolean {
  return error instanceof TypeError;
}

// The detailed flow screens are below; the core hooks supply their data and mutations for every client.
function HomeworkReview({ route, tablet }: { route: AssessmentRoute; tablet: boolean }) {
  const { t } = useI18n();
  const id = route.params.homeworkId ?? null;
  const mayRead = useCan('assessment.homework.read');
  const mayReadSubmissions = useCan('assessment.homework_submission.read');
  const mayPublish = useCan('assessment.homework.publish_homework');
  const mayApprove = useCan('assessment.homework.approve_homework');
  const mayReject = useCan('assessment.homework.reject_homework');
  const homework = useHomework(mayRead ? id : null);
  const submissions = useHomeworkSubmissions(mayReadSubmissions ? id : null);
  const data = homework.data?.data;
  const studentIds = mayReadSubmissions ? (submissions.data?.items.map((item) => item.studentId) ?? []) : [];
  const names = useStudentNames(studentIds);
  const mutation = useHomeworkStep();
  const [reason, setReason] = useState('');
  const status = data?.status ?? 'draft';
  if (!mayRead) return <Banner>{t('adm.resource.noAccess')}</Banner>;
  if (!id) return <Banner>{t('common.state.notAvailable')}</Banner>;
  return (
    <>
      <QueryFeedback query={homework} hasCachedRows={Boolean(data)} onRetry={() => void homework.refetch()} />
      {homework.isPending ? <LoadingRows tablet={tablet} /> : null}
      {data ? (
        <>
          {data.description ? <Text>{data.description}</Text> : null}
          <Text variant="caption">{enumLabel(t, 'assessment.status', status)}</Text>
          {data.dueAt ? <Text muted>{t('asm.native.due', { date: data.dueAt })}</Text> : null}
          <WorkflowNotice mutation={mutation} />
          {(status === 'draft' && mayPublish) || (status === 'pending' && (mayApprove || mayReject)) ? (
            <TextInput
              label={t('asm.native.approvalNote')}
              value={reason}
              onChangeText={setReason}
              multiline
              style={{ minHeight: 72 }}
            />
          ) : null}
          {status === 'draft' && mayPublish ? (
            <Button
              loading={mutation.isPending}
              disabled={!homework.data?.etag}
              onPress={() =>
                mutation.mutate({ homeworkId: id, step: 'publish', etag: homework.data?.etag ?? null })
              }
            >
              {t('asm.native.publish')}
            </Button>
          ) : null}
          {status === 'pending' && mayApprove ? (
            <Button
              loading={mutation.isPending}
              disabled={!homework.data?.etag}
              onPress={() =>
                mutation.mutate({
                  homeworkId: id,
                  step: 'approve',
                  etag: homework.data?.etag ?? null,
                  reason,
                })
              }
            >
              {t('asm.native.approve')}
            </Button>
          ) : null}
          {status === 'pending' && mayReject ? (
            <Button
              variant="secondary"
              loading={mutation.isPending}
              disabled={!homework.data?.etag}
              onPress={() =>
                mutation.mutate({ homeworkId: id, step: 'reject', etag: homework.data?.etag ?? null, reason })
              }
            >
              {t('asm.native.reject')}
            </Button>
          ) : null}
          {mayReadSubmissions ? (
            <HomeworkSubmissionRows homeworkId={id} query={submissions} names={names} tablet={tablet} />
          ) : null}
        </>
      ) : null}
    </>
  );
}

function HomeworkSubmission({ route, tablet }: { route: AssessmentRoute; tablet: boolean }) {
  const { t } = useI18n();
  const student = useMyStudent();
  const studentId = student.data?.id ?? null;
  const homeworkId = route.params.homeworkId ?? null;
  const mayRead = useCan('assessment.homework.read');
  const mayReadSubmissions = useCan('assessment.homework_submission.read');
  const mayCreate = useCan('assessment.homework_submission.create');
  const homework = useHomework(mayRead ? homeworkId : null);
  const submissions = useHomeworkSubmissions(mayReadSubmissions && studentId ? homeworkId : null);
  const mineCandidate = submissions.data?.items.find((submission) => submission.studentId === studentId);
  const mine = mineCandidate?.id ? (mineCandidate as HomeworkSubmissionData & { id: string }) : undefined;
  const create = useCreateHomeworkSubmission();
  const reply = useReplyToHomeworkFeedback();
  const [studentReply, setStudentReply] = useState('');
  if (!mayRead) return <Banner>{t('adm.resource.noAccess')}</Banner>;
  if (!homeworkId) return <Banner>{t('common.state.notAvailable')}</Banner>;
  return (
    <>
      <QueryFeedback
        query={homework}
        hasCachedRows={Boolean(homework.data)}
        onRetry={() => void homework.refetch()}
      />
      {homework.isPending ? <LoadingRows tablet={tablet} /> : null}
      {homework.data ? (
        <>
          {homework.data.data.description ? <Text>{homework.data.data.description}</Text> : null}
          {homework.data.data.dueAt ? (
            <Text muted>{t('asm.native.due', { date: homework.data.data.dueAt })}</Text>
          ) : null}
          <Banner>{t('asm.native.attachmentsUnavailable')}</Banner>
          {mayReadSubmissions ? (
            <QueryFeedback
              query={submissions}
              hasCachedRows={Boolean(mine)}
              onRetry={() => void submissions.refetch()}
            />
          ) : mayCreate ? (
            <Banner>{t('asm.native.unavailable')}</Banner>
          ) : null}
          <WorkflowNotice mutation={create} />
          {!mine &&
          mayReadSubmissions &&
          !submissions.isPending &&
          !submissions.isError &&
          mayCreate &&
          studentId &&
          homework.data.data.status === 'published' ? (
            <Button loading={create.isPending} onPress={() => create.mutate({ homeworkId, studentId })}>
              {t('asm.native.submit')}
            </Button>
          ) : null}
          {mine ? <HomeworkSubmissionReceipt submission={mine} /> : null}
          {!mine && mayReadSubmissions && !submissions.isPending && !submissions.isError ? (
            <Banner>{t('asm.native.noSubmission')}</Banner>
          ) : null}
          {mine?.teacherComments ? (
            <>
              <Text variant="h3">{t('asm.native.feedback')}</Text>
              <Text>{mine.teacherComments}</Text>
              {!mine.studentReply ? (
                <>
                  <TextInput
                    label={t('asm.native.replyLabel')}
                    value={studentReply}
                    onChangeText={setStudentReply}
                    multiline
                    style={{ minHeight: 88 }}
                  />
                  <Button
                    loading={reply.isPending}
                    disabled={!studentReply.trim() || !mine.id || !mine.etag}
                    onPress={() => {
                      if (!homeworkId || !mine.id || !mine.etag) return;
                      reply.mutate({ homeworkId, submissionId: mine.id, etag: mine.etag, studentReply });
                    }}
                  >
                    {t('asm.native.reply')}
                  </Button>
                </>
              ) : (
                <Text>{mine.studentReply}</Text>
              )}
              <WorkflowNotice mutation={reply} />
            </>
          ) : null}
        </>
      ) : null}
    </>
  );
}

function HomeworkSubmissionReceipt({ submission }: { submission: HomeworkSubmissionData }) {
  const { t } = useI18n();
  return (
    <>
      <Text variant="h3">{t('asm.native.submitted')}</Text>
      <Text>{enumLabel(t, 'assessment.status', submission.status)}</Text>
    </>
  );
}

function HomeworkSubmissionRows({
  homeworkId,
  query,
  names,
  tablet,
}: {
  homeworkId: string;
  query: ReturnType<typeof useHomeworkSubmissions>;
  names: ReadonlyMap<string, string>;
  tablet: boolean;
}) {
  const { t } = useI18n();
  const mayCheck = useCan('assessment.homework_submission.check_homework_submission');
  const check = useCheckHomeworkSubmission();
  const [comments, setComments] = useState<Record<string, string>>({});
  const submissions = query.data?.items ?? [];
  return (
    <>
      <Text variant="h2">{t('asm.native.submissions')}</Text>
      <QueryFeedback
        query={query}
        hasCachedRows={submissions.length > 0}
        onRetry={() => void query.refetch()}
      />
      {query.isPending ? <LoadingRows tablet={tablet} /> : null}
      {!query.isPending && !query.isError && submissions.length === 0 ? (
        <Banner>{t('asm.native.noSubmissions')}</Banner>
      ) : null}
      {submissions.map((submission) => {
        const submissionId = submission.id;
        if (!submissionId) return null;
        return (
          <View key={submissionId}>
            <Text variant="h3">{names.get(submission.studentId) ?? submission.studentId}</Text>
            <Text>{enumLabel(t, 'assessment.status', submission.status)}</Text>
            {submission.studentReply ? <Text>{submission.studentReply}</Text> : null}
            {mayCheck ? (
              <>
                <TextInput
                  label={t('asm.native.comments')}
                  value={comments[submissionId] ?? submission.teacherComments ?? ''}
                  onChangeText={(value) => setComments((current) => ({ ...current, [submissionId]: value }))}
                  multiline
                  style={{ minHeight: 72 }}
                />
                <Button
                  loading={check.isPending}
                  disabled={!submission.etag}
                  onPress={() =>
                    check.mutate({
                      homeworkId,
                      submissionId,
                      etag: submission.etag ?? null,
                      teacherComments: comments[submissionId] ?? submission.teacherComments ?? '',
                    })
                  }
                >
                  {t('asm.native.markChecked')}
                </Button>
              </>
            ) : null}
          </View>
        );
      })}
      <WorkflowNotice mutation={check} />
    </>
  );
}

function AssignmentWorkflow({ row, etag, tablet }: { row: RecordRow; etag: string | null; tablet: boolean }) {
  const { t } = useI18n();
  const status = String(row.status ?? '');
  const step = useAssignmentStep();
  const [reason, setReason] = useState('');
  const mayReadApprovals = useCan('assessment.assignment_approval.read');
  const mayPublish = useCan('assessment.assignment.publish_assignment');
  const mayApprove = useCan('assessment.assignment.approve_assignment');
  const mayReject = useCan('assessment.assignment.reject_assignment');
  const mayFinish = useCan('assessment.assignment.finish_assignment');
  const mayCancel = useCan('assessment.assignment.cancel_assignment');
  const mayReset = useCan('assessment.assignment.reset_assignment');
  const approvalDef = RESOURCES.find((resource) => resource.key === 'assessment.AssignmentApproval');
  const approvals = useInfiniteRecords(approvalDef, {
    parentId: mayReadApprovals ? row.id : undefined,
    filter: 'decision = "pending"',
    size: 100,
  });
  const pendingApprovals = (approvals.data?.pages.flatMap((page) => page.data) ?? []).filter(
    (approval) => approval.decision === 'pending',
  );
  const hasPendingApproval = pendingApprovals.length > 0;
  const readyToPublish = !mayReadApprovals || (approvals.isSuccess && !hasPendingApproval);
  const actions: {
    step: AssignmentStep;
    permission: boolean;
    show: boolean;
    reason?: boolean;
    label: string;
  }[] = [
    {
      step: 'publish',
      permission: mayPublish,
      show: status === 'draft' && readyToPublish,
      label: t('asm.native.publish'),
    },
    {
      step: 'approve',
      permission: mayApprove,
      show: status === 'draft' && mayReadApprovals && hasPendingApproval,
      reason: true,
      label: t('asm.native.approve'),
    },
    {
      step: 'reject',
      permission: mayReject,
      show: status === 'draft' && mayReadApprovals && hasPendingApproval,
      reason: true,
      label: t('asm.native.reject'),
    },
    {
      step: 'finish',
      permission: mayFinish,
      show: ['published', 'ongoing'].includes(status),
      label: t('asm.native.finish'),
    },
    {
      step: 'cancel',
      permission: mayCancel,
      show: ['draft', 'published', 'ongoing'].includes(status),
      reason: true,
      label: t('common.action.cancel'),
    },
    { step: 'reset', permission: mayReset, show: status === 'cancelled', label: t('asm.native.reset') },
  ];
  const visible = actions.filter((action) => action.permission && action.show);
  return (
    <>
      <Text variant="caption">
        {t('asm.native.currentStatus')}: {enumLabel(t, 'assessment.status', status)}
      </Text>
      {status === 'draft' && mayReadApprovals ? (
        <>
          <QueryFeedback
            query={approvals}
            hasCachedRows={pendingApprovals.length > 0}
            onRetry={() => void approvals.refetch()}
          />
          {approvals.isPending ? <LoadingRows tablet={tablet} /> : null}
          {pendingApprovals.map((approval) => (
            <View key={approval.id}>
              <Banner>{t('asm.native.pendingAction')}</Banner>
              {typeof approval.comments === 'string' && approval.comments ? (
                <Text>
                  {t('asm.native.approvalNote')}: {approval.comments}
                </Text>
              ) : null}
            </View>
          ))}
        </>
      ) : null}
      {visible.some((action) => action.reason) ? (
        <TextInput label={t('asm.native.approvalNote')} value={reason} onChangeText={setReason} multiline />
      ) : null}
      {visible.map((action) => (
        <Button
          key={action.step}
          loading={step.isPending}
          disabled={!etag}
          onPress={() =>
            step.mutate(
              {
                assignmentId: row.id,
                step: action.step,
                etag,
                ...(action.reason && reason.trim() ? { reason } : {}),
              },
              {
                onSuccess: () => {
                  setReason('');
                  if (mayReadApprovals) void approvals.refetch();
                },
              },
            )
          }
        >
          {action.label}
        </Button>
      ))}
      <WorkflowNotice mutation={step} />
    </>
  );
}

function AssignmentSubmissionsPanel({ assignmentId, tablet }: { assignmentId: string; tablet: boolean }) {
  const { t } = useI18n();
  const mayRead = useCan('assessment.assignment_submission.read');
  const submissionsQuery = useAssignmentSubmissions(mayRead ? assignmentId : null);
  const submissions = ((submissionsQuery.data?.items ?? []) as AssignmentSubmissionData[]).filter(
    (submission): submission is AssignmentSubmissionData & { id: string } => Boolean(submission.id),
  );
  const names = useStudentNames(mayRead ? submissions.map((submission) => submission.studentId) : []);
  if (!mayRead) return <Banner>{t('adm.resource.noAccess')}</Banner>;
  return (
    <>
      <Text variant="h2">{t('asm.native.submissions')}</Text>
      <QueryFeedback
        query={submissionsQuery}
        hasCachedRows={submissions.length > 0}
        onRetry={() => void submissionsQuery.refetch()}
      />
      {submissionsQuery.isPending ? <LoadingRows tablet={tablet} /> : null}
      {!submissionsQuery.isPending && !submissionsQuery.isError && submissions.length === 0 ? (
        <Banner>{t('asm.native.noSubmissions')}</Banner>
      ) : null}
      {submissions.map((submission) => (
        <AssignmentSubmissionReview
          key={submission.id}
          assignmentId={assignmentId}
          submission={submission}
          studentName={names.get(submission.studentId) ?? submission.studentId}
        />
      ))}
    </>
  );
}

function AssignmentSubmissionReview({
  assignmentId,
  submission,
  studentName,
}: {
  assignmentId: string;
  submission: AssignmentSubmissionData & { id: string };
  studentName: string;
}) {
  const { t } = useI18n();
  const mutation = useAssignmentSubmissionStep();
  const mayAccept = useCan('assessment.assignment_submission.accept_assignment_submission');
  const mayReject = useCan('assessment.assignment_submission.reject_assignment_submission');
  const mayRequest = useCan('assessment.assignment_submission.request_submission_changes');
  const mayGrade = useCan('assessment.assignment_submission.grade_assignment_submission');
  const status = submission.status ?? '';
  const [marks, setMarks] = useState(submission.marksObtained ?? '');
  const [feedback, setFeedback] = useState(submission.feedback ?? '');
  const [reason, setReason] = useState('');
  const submitted = status === 'submitted';
  const run = (step: AssignmentSubmissionStep) =>
    mutation.mutate({
      assignmentId,
      submissionId: submission.id,
      step,
      etag: submission.etag ?? null,
      ...(reason.trim() ? { reason: reason.trim() } : {}),
      ...(step === 'grade' ? { marks: marks.trim(), feedback: feedback.trim() } : {}),
    });
  return (
    <View>
      <Text variant="h3">{studentName}</Text>
      <Text variant="caption">{enumLabel(t, 'assessment.assignment_submission_status', status)}</Text>
      {submission.content ? <Text>{submission.content}</Text> : null}
      {submission.submittedAt ? (
        <Text muted>
          {t('asm.native.submitted')}: {submission.submittedAt}
        </Text>
      ) : null}
      {submitted && (mayReject || mayRequest) ? (
        <TextInput label={t('asm.native.approvalNote')} value={reason} onChangeText={setReason} multiline />
      ) : null}
      {mayGrade && ['submitted', 'accepted'].includes(status) ? (
        <>
          <TextInput
            label={t('asm.native.marks')}
            value={marks}
            onChangeText={setMarks}
            keyboardType="decimal-pad"
          />
          <TextInput label={t('asm.native.feedback')} value={feedback} onChangeText={setFeedback} multiline />
          <Button
            loading={mutation.isPending}
            disabled={!submission.etag || !marks.trim()}
            onPress={() => run('grade')}
          >
            {t('asm.native.saveMarks')}
          </Button>
        </>
      ) : null}
      {mayAccept && submitted ? (
        <Button
          variant="secondary"
          loading={mutation.isPending}
          disabled={!submission.etag}
          onPress={() => run('accept')}
        >
          {t('asm.native.approve')}
        </Button>
      ) : null}
      {mayRequest && submitted ? (
        <Button
          variant="secondary"
          loading={mutation.isPending}
          disabled={!submission.etag || !reason.trim()}
          onPress={() => run('requestChanges')}
        >
          {t('asm.native.requestChanges')}
        </Button>
      ) : null}
      {mayReject && submitted ? (
        <Button
          variant="secondary"
          loading={mutation.isPending}
          disabled={!submission.etag || !reason.trim()}
          onPress={() => run('reject')}
        >
          {t('asm.native.reject')}
        </Button>
      ) : null}
      {submission.marksObtained ? (
        <Text>
          {t('asm.native.marks')}: {submission.marksObtained}
        </Text>
      ) : null}
      {submission.feedback ? (
        <Text>
          {t('asm.native.feedback')}: {submission.feedback}
        </Text>
      ) : null}
      <WorkflowNotice mutation={mutation} />
    </View>
  );
}

function ExamSessionWorkflow({ row, etag }: { row: RecordRow; etag: string | null }) {
  const { t } = useI18n();
  const status = String(row.status ?? '');
  const step = useExamSessionStep();
  const [reason, setReason] = useState('');
  const maySchedule = useCan('assessment.exam_session.schedule_exam_session');
  const mayHold = useCan('assessment.exam_session.mark_exam_session_held');
  const mayComplete = useCan('assessment.exam_session.complete_exam_session');
  const mayCancel = useCan('assessment.exam_session.cancel_exam_session');
  const mayReset = useCan('assessment.exam_session.reset_exam_session');
  const actions: {
    step: ExamSessionStep;
    permission: boolean;
    show: boolean;
    reason?: boolean;
    label: string;
  }[] = [
    { step: 'schedule', permission: maySchedule, show: status === 'draft', label: t('asm.native.schedule') },
    { step: 'markHeld', permission: mayHold, show: status === 'scheduled', label: t('asm.native.markHeld') },
    { step: 'complete', permission: mayComplete, show: status === 'held', label: t('asm.native.complete') },
    {
      step: 'cancel',
      permission: mayCancel,
      show: ['draft', 'scheduled'].includes(status),
      reason: true,
      label: t('common.action.cancel'),
    },
    { step: 'reset', permission: mayReset, show: status === 'cancelled', label: t('asm.native.reset') },
  ];
  const visible = actions.filter((action) => action.permission && action.show);
  return (
    <>
      <Text variant="caption">
        {t('asm.native.currentStatus')}: {enumLabel(t, 'assessment.status', status)}
      </Text>
      {visible.some((action) => action.reason) ? (
        <TextInput label={t('asm.native.approvalNote')} value={reason} onChangeText={setReason} multiline />
      ) : null}
      {visible.map((action) => (
        <Button
          key={action.step}
          loading={step.isPending}
          disabled={!etag}
          onPress={() =>
            step.mutate({
              examSessionId: row.id,
              step: action.step,
              etag,
              ...(action.reason && reason.trim() ? { reason } : {}),
            })
          }
        >
          {action.label}
        </Button>
      ))}
      <WorkflowNotice mutation={step} />
    </>
  );
}

function ExamSessionExamsPanel({
  examSessionId,
  onNavigate,
}: {
  examSessionId: string;
  onNavigate: (route: AssessmentRoute) => void;
}) {
  const { t } = useI18n();
  const mayRead = useCan('assessment.exam.read');
  const def = RESOURCES.find((resource) => resource.key === 'assessment.Exam');
  const examsQuery = useInfiniteRecords(mayRead ? def : undefined, {
    filter: `examSessionId = "${examSessionId}"`,
  });
  const exams = examsQuery.data?.pages.flatMap((page) => page.data) ?? [];
  if (!mayRead) return <Banner>{t('adm.resource.noAccess')}</Banner>;
  return (
    <>
      <Text variant="h2">{t('asm.native.exams')}</Text>
      <QueryFeedback
        query={examsQuery}
        hasCachedRows={exams.length > 0}
        onRetry={() => void examsQuery.refetch()}
      />
      {examsQuery.isPending ? <LoadingRows tablet /> : null}
      {!examsQuery.isPending && !examsQuery.isError && exams.length === 0 ? (
        <Banner>{t('asm.native.noExams')}</Banner>
      ) : null}
      {exams.map((exam) => (
        <Button
          key={exam.id}
          variant="secondary"
          onPress={() => onNavigate({ screenId: 'ASM-09', params: { examId: exam.id } })}
        >
          {firstText(exam, ['name', 'code']) ?? exam.id}
        </Button>
      ))}
    </>
  );
}

function ExamWorkflow({ row, etag }: { row: RecordRow; etag: string | null }) {
  const { t } = useI18n();
  const status = String(row.status ?? '');
  const step = useExamStep();
  const populate = usePopulateExamResults();
  const allocate = useAllocateExamRooms();
  const [reason, setReason] = useState('');
  const [roomIds, setRoomIds] = useState<string[]>([]);
  const maySchedule = useCan('assessment.exam.schedule_exam');
  const mayHold = useCan('assessment.exam.mark_exam_held');
  const mayResultsUpdated = useCan('assessment.exam.mark_exam_results_updated');
  const mayComplete = useCan('assessment.exam.complete_exam');
  const mayCancel = useCan('assessment.exam.cancel_exam');
  const mayReset = useCan('assessment.exam.reset_exam');
  const mayPopulate = useCan('assessment.exam.populate_exam_results');
  const mayAllocate = useCan('assessment.exam.allocate_exam_rooms');
  const mayReadResults = useCan('assessment.exam_result.read');
  const mayReadRooms = useCan('facility.room.read');
  const resultQuery = useExamResults(mayReadResults ? row.id : null);
  const results = resultQuery.data?.items ?? [];
  const roomDef = RESOURCES.find((resource) => resource.key === 'facility.Room');
  const roomsQuery = useInfiniteRecords(mayReadRooms ? roomDef : undefined, { size: 100 });
  const rooms = roomsQuery.data?.pages.flatMap((page) => page.data) ?? [];
  const actions: { step: ExamStep; permission: boolean; show: boolean; reason?: boolean; label: string }[] = [
    { step: 'schedule', permission: maySchedule, show: status === 'draft', label: t('asm.native.schedule') },
    { step: 'markHeld', permission: mayHold, show: status === 'scheduled', label: t('asm.native.markHeld') },
    {
      step: 'resultsUpdated',
      permission: mayResultsUpdated,
      show: status === 'held',
      label: t('asm.native.markUpdated'),
    },
    {
      step: 'complete',
      permission: mayComplete,
      show: ['held', 'result_updated'].includes(status),
      label: t('asm.native.complete'),
    },
    {
      step: 'cancel',
      permission: mayCancel,
      show: ['draft', 'scheduled'].includes(status),
      reason: true,
      label: t('common.action.cancel'),
    },
    { step: 'reset', permission: mayReset, show: status === 'cancelled', label: t('asm.native.reset') },
  ];
  const visible = actions.filter((action) => action.permission && action.show);
  const toggleRoom = (id: string, value: boolean) =>
    setRoomIds((current) =>
      value ? [...current.filter((room) => room !== id), id] : current.filter((room) => room !== id),
    );
  return (
    <>
      <Text variant="caption">
        {t('asm.native.currentStatus')}: {enumLabel(t, 'assessment.status', status)}
      </Text>
      {visible.some((action) => action.reason) ? (
        <TextInput label={t('asm.native.approvalNote')} value={reason} onChangeText={setReason} multiline />
      ) : null}
      {visible.map((action) => (
        <Button
          key={action.step}
          loading={step.isPending}
          disabled={!etag}
          onPress={() =>
            step.mutate({
              examId: row.id,
              step: action.step,
              etag,
              ...(action.reason && reason.trim() ? { reason } : {}),
            })
          }
        >
          {action.label}
        </Button>
      ))}
      {mayPopulate && ['scheduled', 'held'].includes(status) ? (
        <Button loading={populate.isPending} onPress={() => populate.mutate(row.id)}>
          {t('asm.native.populateResults')}
        </Button>
      ) : null}
      {mayAllocate && ['scheduled', 'held'].includes(status) ? (
        <>
          <Text variant="h3">{t('asm.native.roomSelection')}</Text>
          {!mayReadRooms ? <Banner>{t('asm.native.unavailable')}</Banner> : null}
          <QueryFeedback
            query={roomsQuery}
            hasCachedRows={rooms.length > 0}
            onRetry={() => void roomsQuery.refetch()}
          />
          {roomsQuery.isPending ? <LoadingRows tablet /> : null}
          {mayReadRooms && !roomsQuery.isPending && !roomsQuery.isError && rooms.length === 0 ? (
            <Banner>{t('common.state.empty')}</Banner>
          ) : null}
          {rooms.map((room) => (
            <SwitchRow
              key={room.id}
              label={firstText(room, ['name', 'code', 'roomNumber']) ?? room.id}
              value={roomIds.includes(room.id)}
              onChange={(value) => toggleRoom(room.id, value)}
            />
          ))}
          <Button
            loading={allocate.isPending}
            disabled={!roomIds.length || !mayReadRooms}
            onPress={() => allocate.mutate({ examId: row.id, roomIds })}
          >
            {t('asm.native.allocateRooms')}
          </Button>
        </>
      ) : null}
      {resultQuery.isError ? (
        <QueryFeedback
          query={resultQuery}
          hasCachedRows={results.length > 0}
          onRetry={() => void resultQuery.refetch()}
        />
      ) : null}
      <WorkflowNotice mutation={step} />
      <WorkflowNotice mutation={populate} />
      <WorkflowNotice mutation={allocate} />
    </>
  );
}

function MarksheetWorkflow({ row, etag }: { row: RecordRow; etag: string | null }) {
  const { t } = useI18n();
  const status = String(row.status ?? '');
  const mutation = useMarksheetStep();
  const [reason, setReason] = useState('');
  const [visibleToGuardians, setVisibleToGuardians] = useState(row.visibleToGuardians === true);
  const mayValidate = useCan('assessment.marksheet.validate_marksheet');
  const mayPublish = useCan('assessment.marksheet.publish');
  const mayCancel = useCan('assessment.marksheet.cancel_marksheet');
  const mayReset = useCan('assessment.marksheet.reset_marksheet');
  return (
    <>
      <Text variant="caption">
        {t('asm.native.currentStatus')}: {enumLabel(t, 'assessment.status', status)}
      </Text>
      {status === 'draft' && mayValidate ? (
        <Button
          loading={mutation.isPending}
          disabled={!etag}
          onPress={() => mutation.mutate({ marksheetId: row.id, step: 'validate', etag })}
        >
          {t('asm.native.validate')}
        </Button>
      ) : null}
      {status === 'validated' && mayPublish ? (
        <>
          <SwitchRow
            label={t('asm.native.visibleToGuardians')}
            value={visibleToGuardians}
            onChange={setVisibleToGuardians}
          />
          <Button
            loading={mutation.isPending}
            disabled={!etag}
            onPress={() =>
              mutation.mutate({ marksheetId: row.id, step: 'publish', etag, visibleToGuardians })
            }
          >
            {t('asm.native.publish')}
          </Button>
        </>
      ) : null}
      {['draft', 'validated'].includes(status) && mayCancel ? (
        <>
          <TextInput label={t('adm.records.reason')} value={reason} onChangeText={setReason} multiline />
          <Button
            variant="secondary"
            loading={mutation.isPending}
            disabled={!etag}
            onPress={() =>
              mutation.mutate({
                marksheetId: row.id,
                step: 'cancel',
                etag,
                ...(reason.trim() ? { reason } : {}),
              })
            }
          >
            {t('common.action.cancel')}
          </Button>
        </>
      ) : null}
      {status === 'cancelled' && mayReset ? (
        <Button
          loading={mutation.isPending}
          disabled={!etag}
          onPress={() => mutation.mutate({ marksheetId: row.id, step: 'reset', etag })}
        >
          {t('asm.native.reset')}
        </Button>
      ) : null}
      <WorkflowNotice mutation={mutation} />
    </>
  );
}

function ResultTemplatePanel({
  tablet,
  onNavigate,
}: {
  tablet: boolean;
  onNavigate: (route: AssessmentRoute) => void;
}) {
  const { t } = useI18n();
  const mayRead = useCan('assessment.result_template.read');
  const mayGenerate = useCan('assessment.result_template.generate_results');
  const mayCreate = useCan('assessment.result_template.create');
  const def = RESOURCES.find((resource) => resource.key === 'assessment.ResultTemplate');
  const templatesQuery = useInfiniteRecords(mayRead ? def : undefined, { size: tablet ? 50 : 25 });
  const templates = templatesQuery.data?.pages.flatMap((page) => page.data) ?? [];
  const generate = useResultTemplateGenerateResults();
  const submittedOperation = (generate.data as { result?: Operation } | undefined)?.result;
  const operation = useOperation(
    submittedOperation?.operationId && submittedOperation.module ? submittedOperation : null,
  );
  const [creating, setCreating] = useState(false);
  if (!mayRead) return <Banner>{t('adm.resource.noAccess')}</Banner>;
  return (
    <>
      <Text variant="h2">{t('asm.native.generateResults')}</Text>
      {mayCreate && def && !creating ? (
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
            void templatesQuery.refetch();
          }}
          onCancel={() => setCreating(false)}
        />
      ) : null}
      <QueryFeedback
        query={templatesQuery}
        hasCachedRows={templates.length > 0}
        onRetry={() => void templatesQuery.refetch()}
      />
      {templatesQuery.isPending ? <LoadingRows tablet={tablet} /> : null}
      {!templatesQuery.isPending && !templatesQuery.isError && templates.length === 0 ? (
        <Banner>{t('asm.native.noRows')}</Banner>
      ) : null}
      {templates.map((template) => (
        <ResultTemplateCard
          key={template.id}
          row={template}
          def={def}
          tablet={tablet}
          mayGenerate={mayGenerate}
          generate={generate}
          onChange={() => void templatesQuery.refetch()}
          onNavigate={onNavigate}
        />
      ))}
      {generate.isSuccess ? <Banner>{t('asm.native.generationStarted')}</Banner> : null}
      {operation.data ? (
        <View>
          <Text>
            {t('platform.jobs.status')}: {enumLabel(t, 'operation', operation.data.status)}
          </Text>
          {operation.data.progressPercent !== undefined ? (
            <Text>
              {t('platform.jobs.progress')}: {operation.data.progressPercent}%
            </Text>
          ) : null}
          {operation.data.errorMessage ? (
            <Banner intent="danger">{operation.data.errorMessage}</Banner>
          ) : null}
        </View>
      ) : null}
      <QueryFeedback
        query={operation}
        hasCachedRows={Boolean(operation.data)}
        onRetry={() => void operation.refetch()}
      />
      <WorkflowNotice mutation={generate} />
    </>
  );
}

function AssignmentReview({ route, tablet }: { route: AssessmentRoute; tablet: boolean }) {
  return (
    <RecordDetailRoute recordKey="assessment.Assignment" param="assignmentId" route={route} tablet={tablet} />
  );
}

function AssignmentSubmission({ route, tablet }: { route: AssessmentRoute; tablet: boolean }) {
  const { t } = useI18n();
  const theme = useTheme();
  const assignmentId = route.params.assignmentId ?? null;
  const student = useMyStudent();
  const studentId = student.data?.id ?? null;
  const mayReadAssignment = useCan('assessment.assignment.read');
  const mayReadSubmissions = useCan('assessment.assignment_submission.read');
  const mayCreate = useCan('assessment.assignment_submission.create');
  const mayUpdate = useCan('assessment.assignment_submission.update');
  const maySubmit = useCan('assessment.assignment_submission.submit_assignment_submission');
  const assignmentDef = RESOURCES.find((resource) => resource.key === 'assessment.Assignment');
  const submissionDef = RESOURCES.find(
    (resource) => resource.key === 'assessment.AssignmentSubmission',
  ) as ResourceDef;
  const assignment = useRecord(assignmentDef, mayReadAssignment ? assignmentId : null);
  const submissions = useAssignmentSubmissions(mayReadSubmissions ? assignmentId : null);
  const ownCandidate = submissions.data?.items.find((submission) => submission.studentId === studentId);
  const own = ownCandidate?.id ? (ownCandidate as AssignmentSubmissionData & { id: string }) : undefined;
  const create = useCreateRecord(submissionDef);
  const update = useUpdateRecord(submissionDef);
  const submit = useAssignmentSubmissionStep();
  const [content, setContent] = useState('');
  const [editing, setEditing] = useState(false);
  if (!assignmentId) return <Banner>{t('common.state.notAvailable')}</Banner>;
  if (!mayReadAssignment) return <Banner>{t('adm.resource.noAccess')}</Banner>;
  const data = assignment.data?.data as unknown as Assignment | undefined;
  const status = own?.status ?? '';
  const isEditable = Boolean(own && mayUpdate && ['draft', 'change_requested'].includes(status));
  const editableContent = editing ? content : (own?.content ?? '');
  const createSubmission = () => {
    if (!studentId) return;
    create.mutate(
      { values: { assignmentId, studentId, content: content.trim() } },
      {
        onSuccess: () => {
          setContent('');
          void submissions.refetch();
        },
      },
    );
  };
  const saveContent = () => {
    if (!own?.id || !own.etag) return;
    update.mutate(
      { id: own.id, etag: own.etag, values: { content: content.trim() } },
      {
        onSuccess: () => {
          setEditing(false);
          void submissions.refetch();
        },
      },
    );
  };
  return (
    <View style={{ gap: theme.space[4] }}>
      <QueryFeedback
        query={assignment}
        hasCachedRows={Boolean(data)}
        onRetry={() => void assignment.refetch()}
      />
      {assignment.isPending ? <LoadingRows tablet={tablet} /> : null}
      {data ? (
        <>
          <Text variant="h2">{data.title}</Text>
          {data.description ? <Text>{data.description}</Text> : null}
          {data.dueAt ? <Text muted>{t('asm.native.due', { date: data.dueAt })}</Text> : null}
          {data.maxMarks ? (
            <Text muted>
              {t('asm.native.maxMarks')}: {data.maxMarks}
            </Text>
          ) : null}
          <Banner>{t('asm.native.attachmentsUnavailable')}</Banner>
          {mayReadSubmissions ? (
            <QueryFeedback
              query={submissions}
              hasCachedRows={Boolean(own)}
              onRetry={() => void submissions.refetch()}
            />
          ) : mayCreate ? (
            <Banner>{t('asm.native.unavailable')}</Banner>
          ) : null}
          {own ? (
            <>
              <Text variant="h3">{enumLabel(t, 'assessment.assignment_submission_status', status)}</Text>
              {isEditable ? (
                <>
                  <TextInput
                    label={t('asm.native.submit')}
                    value={editableContent}
                    onChangeText={setContent}
                    multiline
                    style={{ minHeight: 120 }}
                  />
                  {editing ? (
                    <Button loading={update.isPending} disabled={!own.etag} onPress={saveContent}>
                      {t('common.action.save')}
                    </Button>
                  ) : (
                    <Button
                      variant="secondary"
                      onPress={() => {
                        setContent(own.content ?? '');
                        setEditing(true);
                      }}
                    >
                      {t('common.action.edit')}
                    </Button>
                  )}
                </>
              ) : own.content ? (
                <Text>{own.content}</Text>
              ) : null}
              {own.feedback ? (
                <Text>
                  {t('asm.native.feedback')}: {own.feedback}
                </Text>
              ) : null}
              {maySubmit && ['draft', 'change_requested'].includes(status) ? (
                <Button
                  loading={submit.isPending}
                  disabled={!own.etag}
                  onPress={() =>
                    submit.mutate({
                      submissionId: own.id,
                      assignmentId,
                      step: 'submit',
                      etag: own.etag ?? null,
                    })
                  }
                >
                  {t('asm.native.submit')}
                </Button>
              ) : null}
            </>
          ) : mayCreate &&
            mayReadSubmissions &&
            !submissions.isPending &&
            !submissions.isError &&
            studentId &&
            ['published', 'ongoing'].includes(data.status ?? '') ? (
            <>
              <TextInput
                label={t('asm.native.submit')}
                value={content}
                onChangeText={setContent}
                multiline
                style={{ minHeight: 120 }}
              />
              <Button loading={create.isPending} disabled={!content.trim()} onPress={createSubmission}>
                {t('asm.native.submit')}
              </Button>
            </>
          ) : !studentId ? (
            <Banner>{t('common.state.notAvailable')}</Banner>
          ) : mayReadSubmissions && !submissions.isPending && !submissions.isError ? (
            <Banner>{t('asm.native.noSubmission')}</Banner>
          ) : null}
          <WorkflowNotice mutation={create} />
          <WorkflowNotice mutation={update} />
          <WorkflowNotice mutation={submit} />
        </>
      ) : null}
    </View>
  );
}

function ExamMarks({ route, tablet }: { route: AssessmentRoute; tablet: boolean }) {
  const { t } = useI18n();
  const theme = useTheme();
  const examId = route.params.examId ?? null;
  const mayReadExam = useCan('assessment.exam.read');
  const mayReadResults = useCan('assessment.exam_result.read');
  const mayRecord = useCan('assessment.exam.record_exam_marks');
  const mayMarkUpdated = useCan('assessment.exam.mark_exam_results_updated');
  const examDef = RESOURCES.find((resource) => resource.key === 'assessment.Exam');
  const exam = useRecord(examDef, mayReadExam ? examId : null);
  const results = useExamResults(mayReadResults ? examId : null);
  const rows = (results.data?.items ?? []) as ExamResult[];
  const students = rows.map((result) => result.studentId);
  const names = useStudentNames(mayReadResults ? students : []);
  const recordMarks = useRecordExamMarks();
  const examStep = useExamStep();
  const [values, setValues] = useState<Record<string, MarkEntry>>({});
  const [submittedStudentIds, setSubmittedStudentIds] = useState<string[]>([]);
  const examData = exam.data?.data as unknown as Exam | undefined;
  if (!examId) return <Banner>{t('common.state.notAvailable')}</Banner>;
  if (!mayReadExam || !mayReadResults) return <Banner>{t('adm.resource.noAccess')}</Banner>;
  const state = examData?.status ?? '';
  const editable = mayRecord && ['held', 'result_updated'].includes(state);
  const editedRows = rows.filter((row) => {
    const value = values[row.studentId];
    if (!value) return false;
    const attendanceStatus = row.attendanceStatus ?? 'present';
    const marks = row.marks ?? '';
    const note = row.note ?? '';
    return (
      value.attendanceStatus !== attendanceStatus ||
      (value.marksObtained ?? marks) !== marks ||
      (value.note ?? note) !== note
    );
  });
  const rowErrors = examMarkRowErrors(recordMarks.error, submittedStudentIds);
  const saveMarks = () => {
    const entries = editedRows.map((row) => {
      const value = values[row.studentId];
      const attendanceStatus = value?.attendanceStatus ?? row.attendanceStatus ?? 'present';
      const marksObtained = value?.marksObtained ?? row.marks ?? undefined;
      const note = value?.note ?? row.note ?? undefined;
      return {
        studentId: row.studentId,
        attendanceStatus: attendanceStatus === 'absent' ? ('absent' as const) : ('present' as const),
        ...(attendanceStatus !== 'absent' && marksObtained?.trim()
          ? { marksObtained: marksObtained.trim() }
          : {}),
        ...(note?.trim() ? { note: note.trim() } : {}),
      };
    });
    if (entries.length === 0) return;
    setSubmittedStudentIds(editedRows.map((row) => row.studentId));
    recordMarks.mutate(
      { examId, entries },
      {
        onSuccess: () => {
          setValues({});
          setSubmittedStudentIds([]);
        },
      },
    );
  };
  return (
    <>
      <QueryFeedback query={exam} hasCachedRows={Boolean(exam.data)} onRetry={() => void exam.refetch()} />
      {exam.isPending ? <LoadingRows tablet={tablet} /> : null}
      {examData ? <Text variant="h2">{examData.name}</Text> : null}
      <QueryFeedback query={results} hasCachedRows={rows.length > 0} onRetry={() => void results.refetch()} />
      {results.isPending ? <LoadingRows tablet={tablet} /> : null}
      {!results.isPending && !results.isError && rows.length === 0 ? (
        <Banner>{t('asm.native.noRows')}</Banner>
      ) : null}
      <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: theme.space[3] }}>
        {rows.map((row) => (
          <ExamMarkRow
            key={row.studentId}
            result={row}
            name={names.get(row.studentId) ?? row.studentId}
            maxMarks={row.maxMarks ?? examData?.totalMarks}
            value={values[row.studentId]}
            tablet={tablet}
            editable={editable}
            errors={rowErrors[row.studentId]}
            onChange={(value) => {
              if (recordMarks.isError) {
                recordMarks.reset();
                setSubmittedStudentIds([]);
              }
              setValues((current) => ({ ...current, [row.studentId]: value }));
            }}
          />
        ))}
      </View>
      {rows.length > 0 && editable ? (
        <Button loading={recordMarks.isPending} disabled={editedRows.length === 0} onPress={saveMarks}>
          {t('asm.native.saveMarks')}
        </Button>
      ) : null}
      {recordMarks.isSuccess ? <Banner>{t('asm.native.markEntrySaved')}</Banner> : null}
      <WorkflowNotice mutation={recordMarks} />
      {exam.data && state === 'held' && mayMarkUpdated ? (
        <Button
          variant="secondary"
          loading={examStep.isPending}
          disabled={!exam.data.etag}
          onPress={() => examStep.mutate({ examId, step: 'resultsUpdated', etag: exam.data?.etag ?? null })}
        >
          {t('asm.native.markUpdated')}
        </Button>
      ) : null}
      <WorkflowNotice mutation={examStep} />
    </>
  );
}

function StudentResults({ route, tablet }: { route: AssessmentRoute; tablet: boolean }) {
  const { t } = useI18n();
  const mayRead = useCan('assessment.marksheet_line.get_student_results');
  const ownStudent = useMyStudent();
  const studentId = route.params.studentId ?? ownStudent.data?.id ?? null;
  const results = useStudentResults(mayRead ? studentId : null);
  const cards = (results.data?.items ?? []) as ResultCard[];
  const names = useStudentNames(studentId && mayRead ? [studentId] : []);
  if (!mayRead) return <Banner>{t('adm.resource.noAccess')}</Banner>;
  if (!studentId) return <Banner>{t('common.state.notAvailable')}</Banner>;
  return (
    <>
      <Text variant="h2">{names.get(studentId) ?? t('asm.native.student')}</Text>
      <QueryFeedback
        query={results}
        hasCachedRows={cards.length > 0}
        onRetry={() => void results.refetch()}
      />
      {results.isPending ? <LoadingRows tablet={tablet} /> : null}
      {!results.isPending && !results.isError && cards.length === 0 ? (
        <Banner>{t('asm.native.noResults')}</Banner>
      ) : null}
      {cards.map((card, index) => (
        <ResultCardView key={`${card.examSessionId}-${index}`} card={card} />
      ))}
    </>
  );
}

function MarksheetDetails({ marksheetId, tablet }: { marksheetId: string; tablet: boolean }) {
  const { t } = useI18n();
  const mayRead = useCan('assessment.marksheet_line.read');
  const linesQuery = useMarksheetLines(mayRead ? marksheetId : null);
  const lines = (linesQuery.data?.items ?? []) as MarksheetLine[];
  const names = useStudentNames(mayRead ? lines.map((line) => line.studentId) : []);
  if (!mayRead) return <Banner>{t('adm.resource.noAccess')}</Banner>;
  return (
    <>
      <Text variant="h2">{t('asm.native.results')}</Text>
      <QueryFeedback
        query={linesQuery}
        hasCachedRows={lines.length > 0}
        onRetry={() => void linesQuery.refetch()}
      />
      {linesQuery.isPending ? <LoadingRows tablet={tablet} /> : null}
      {!linesQuery.isPending && !linesQuery.isError && lines.length === 0 ? (
        <Banner>{t('asm.native.noLines')}</Banner>
      ) : null}
      {lines.map((line) => (
        <View key={line.id}>
          <Text variant="h3">{names.get(line.studentId) ?? line.studentId}</Text>
          {line.totalMarks ? (
            <Text>
              {t('asm.native.marks')}: {line.totalMarks}
            </Text>
          ) : null}
          {line.percentage ? <Text>{line.percentage}%</Text> : null}
          {line.gradeLabel ? (
            <Text>
              {t('asm.native.grade')}: {line.gradeLabel}
            </Text>
          ) : null}
          {line.resultStatus ? <Text>{enumLabel(t, 'assessment.status', line.resultStatus)}</Text> : null}
        </View>
      ))}
    </>
  );
}

function GradingSchemeBands({ gradingSchemeId, tablet }: { gradingSchemeId: string; tablet: boolean }) {
  const { t } = useI18n();
  const mayRead = useCan('assessment.grading_band.read');
  const mayCreate = useCan('assessment.grading_band.create');
  const def = RESOURCES.find((resource) => resource.key === 'assessment.GradingBand');
  const bandsQuery = useGradingBands(mayRead ? gradingSchemeId : null);
  const bands = (bandsQuery.data?.items ?? []) as GradingBand[];
  const [creating, setCreating] = useState(false);
  if (!mayRead) return <Banner>{t('adm.resource.noAccess')}</Banner>;
  return (
    <>
      <Text variant="h2">{t('asm.native.bands')}</Text>
      {mayCreate && def && !creating ? (
        <Button variant="secondary" onPress={() => setCreating(true)}>
          {t('adm.records.new')}
        </Button>
      ) : null}
      {creating && def ? (
        <RecordEditor
          def={def}
          tablet={tablet}
          initialValues={{ gradingSchemeId }}
          onDone={() => {
            setCreating(false);
            void bandsQuery.refetch();
          }}
          onCancel={() => setCreating(false)}
        />
      ) : null}
      <QueryFeedback
        query={bandsQuery}
        hasCachedRows={bands.length > 0}
        onRetry={() => void bandsQuery.refetch()}
      />
      {bandsQuery.isPending ? <LoadingRows tablet={tablet} /> : null}
      {!bandsQuery.isPending && !bandsQuery.isError && bands.length === 0 ? (
        <Banner>{t('asm.native.noBands')}</Banner>
      ) : null}
      {bands.map((band, index) => (
        <EditableGradingBand
          key={band.id ?? `${band.gradeLabel ?? 'band'}-${index}`}
          band={band}
          def={def}
          tablet={tablet}
          onChange={() => void bandsQuery.refetch()}
        />
      ))}
    </>
  );
}

function ExamMarkRow({
  result,
  name,
  maxMarks,
  value,
  tablet,
  editable,
  errors,
  onChange,
}: {
  result: ExamResult;
  name: string;
  maxMarks: string | undefined;
  value: MarkEntry | undefined;
  tablet: boolean;
  editable: boolean;
  errors: readonly string[] | undefined;
  onChange: (value: MarkEntry) => void;
}) {
  const { t } = useI18n();
  const theme = useTheme();
  const attendanceStatus = value?.attendanceStatus ?? result.attendanceStatus ?? 'present';
  const marks = value?.marksObtained ?? result.marks ?? '';
  const note = value?.note ?? result.note ?? '';
  return (
    <View
      style={{
        padding: theme.space[4],
        marginTop: theme.space[3],
        borderWidth: 1,
        borderColor: theme.colors.border,
        borderRadius: theme.radius.md,
        width: tablet ? '48%' : '100%',
      }}
    >
      <Text variant="h3">{name}</Text>
      <ChoiceGroup
        label={t('asm.native.attendance')}
        value={attendanceStatus}
        disabled={!editable}
        choices={[
          { value: 'present', label: t('asm.native.present') },
          { value: 'absent', label: t('asm.native.absent') },
        ]}
        onChange={(next) =>
          onChange({
            studentId: result.studentId,
            attendanceStatus: next as MarkEntry['attendanceStatus'],
            marksObtained: marks,
            note,
          })
        }
      />
      {maxMarks ? (
        <Text muted>
          {t('asm.native.maxMarks')}: {maxMarks}
        </Text>
      ) : null}
      {attendanceStatus === 'present' ? (
        <TextInput
          label={t('asm.native.markValue')}
          value={marks}
          editable={editable}
          keyboardType="decimal-pad"
          onChangeText={(next) =>
            onChange({ studentId: result.studentId, attendanceStatus: 'present', marksObtained: next, note })
          }
        />
      ) : null}
      <TextInput
        label={t('asm.native.note')}
        value={note}
        editable={editable}
        multiline
        onChangeText={(next) =>
          onChange({
            studentId: result.studentId,
            attendanceStatus: attendanceStatus as MarkEntry['attendanceStatus'],
            marksObtained: marks,
            note: next,
          })
        }
      />
      {errors?.map((error, index) => (
        <Banner key={`${result.studentId}-error-${index}`} intent="danger">
          {error}
        </Banner>
      ))}
      {result.gradeLabel ? (
        <Text>
          {t('asm.native.grade')}: {result.gradeLabel}
        </Text>
      ) : null}
      {result.resultStatus ? <Text>{enumLabel(t, 'assessment.status', result.resultStatus)}</Text> : null}
    </View>
  );
}

function ResultCardView({ card }: { card: ResultCard }) {
  const { t } = useI18n();
  const line = card.marksheetLine;
  return (
    <View>
      <Text variant="h3">
        {t('asm.native.examSession')}: {card.examSessionId}
      </Text>
      {line?.totalMarks ? (
        <Text>
          {t('asm.native.marks')}: {line.totalMarks}
        </Text>
      ) : null}
      {line?.percentage ? <Text>{line.percentage}%</Text> : null}
      {line?.gradeLabel ? (
        <Text>
          {t('asm.native.grade')}: {line.gradeLabel}
        </Text>
      ) : null}
      {line?.resultStatus ? <Text>{enumLabel(t, 'assessment.status', line.resultStatus)}</Text> : null}
      {line?.sgpa ? (
        <Text>
          {t('asm.native.sgpa')}: {line.sgpa}
        </Text>
      ) : null}
      {line?.cgpa ? (
        <Text>
          {t('asm.native.cgpa')}: {line.cgpa}
        </Text>
      ) : null}
      {(card.results ?? []).map((result, index) => (
        <Text key={result.id ?? `${result.studentId}-${index}`}>
          {result.marks ?? enumLabel(t, 'attendance', result.attendanceStatus)}
          {result.gradeLabel ? ` · ${result.gradeLabel}` : ''}
        </Text>
      ))}
    </View>
  );
}

function GradingBandRow({ band }: { band: GradingBand }) {
  const { t } = useI18n();
  const theme = useTheme();
  return (
    <View
      style={{
        padding: theme.space[4],
        marginTop: theme.space[3],
        borderWidth: 1,
        borderColor: theme.colors.border,
        borderRadius: theme.radius.md,
      }}
    >
      <Text variant="h3">{band.gradeLabel}</Text>
      <Text>
        {band.minPercent}% – {band.maxPercent}%
      </Text>
      {band.gradePoint ? (
        <Text>
          {t('asm.native.gradePoint')}: {band.gradePoint}
        </Text>
      ) : null}
      {band.description ? <Text>{band.description}</Text> : null}
      <Text>{band.isPass ? t('asm.native.pass') : t('asm.native.fail')}</Text>
    </View>
  );
}

function EditableGradingBand({
  band,
  def,
  tablet,
  onChange,
}: {
  band: GradingBand;
  def: ResourceDef | undefined;
  tablet: boolean;
  onChange: () => void;
}) {
  const { t } = useI18n();
  const mayUpdate = useCan('assessment.grading_band.update');
  const mayDelete = useCan('assessment.grading_band.delete');
  const [editing, setEditing] = useState(false);
  const row = band as unknown as RecordRow;
  return (
    <View>
      {editing && def ? (
        <RecordEditor
          def={def}
          row={row}
          etag={band.etag ?? null}
          tablet={tablet}
          onDone={() => {
            setEditing(false);
            onChange();
          }}
          onCancel={() => setEditing(false)}
        />
      ) : (
        <GradingBandRow band={band} />
      )}
      {def && mayUpdate && !editing ? (
        <Button variant="secondary" onPress={() => setEditing(true)}>
          {t('common.action.edit')}
        </Button>
      ) : null}
      {def && mayDelete ? (
        <RecordDelete def={def} row={row} etag={band.etag ?? null} onDeleted={onChange} />
      ) : null}
    </View>
  );
}

function ResultTemplateCard({
  row,
  def,
  tablet,
  mayGenerate,
  generate,
  onChange,
  onNavigate,
}: {
  row: RecordRow;
  def: ResourceDef | undefined;
  tablet: boolean;
  mayGenerate: boolean;
  generate: ReturnType<typeof useResultTemplateGenerateResults>;
  onChange: () => void;
  onNavigate: (route: AssessmentRoute) => void;
}) {
  const { t } = useI18n();
  const mayUpdate = useCan('assessment.result_template.update');
  const mayDelete = useCan('assessment.result_template.delete');
  const [editing, setEditing] = useState(false);
  const examSessionId = typeof row.examSessionId === 'string' ? row.examSessionId : undefined;
  return (
    <View>
      {editing && def ? (
        <RecordEditor
          def={def}
          row={row}
          etag={row.etag ?? null}
          tablet={tablet}
          onDone={() => {
            setEditing(false);
            onChange();
          }}
          onCancel={() => setEditing(false)}
        />
      ) : (
        <>
          <Text variant="h3">{firstText(row, ['name', 'resultDate']) ?? row.id}</Text>
          {typeof row.resultDate === 'string' ? (
            <Text muted>
              {t('asm.native.resultDate')}: {row.resultDate}
            </Text>
          ) : null}
          {examSessionId ? (
            <Button
              variant="secondary"
              onPress={() => onNavigate({ screenId: 'ASM-08', params: { examSessionId } })}
            >
              {t('asm.native.examSession')}
            </Button>
          ) : null}
        </>
      )}
      {mayGenerate ? (
        <Button loading={generate.isPending} onPress={() => generate.mutate(row.id)}>
          {t('asm.native.generate')}
        </Button>
      ) : null}
      {def && mayUpdate && !editing ? (
        <Button variant="secondary" onPress={() => setEditing(true)}>
          {t('common.action.edit')}
        </Button>
      ) : null}
      {def && mayDelete ? (
        <RecordDelete def={def} row={row} etag={row.etag ?? null} onDeleted={onChange} />
      ) : null}
    </View>
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
