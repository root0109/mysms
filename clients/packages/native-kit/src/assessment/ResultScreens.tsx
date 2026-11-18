/** Results: marksheets and their templates (ASM-11), a student's report card (ASM-12), grading bands (ASM-13). */
import {
  RESOURCES,
  useGradingBands,
  useInfiniteRecords,
  useMarksheetLines,
  useMarksheetStep,
  useMyStudentIds,
  useResultTemplateGenerateResults,
  useStudentNames,
  useStudentResults,
  useOperation,
  useCan,
  useI18n,
  type GradingBand,
  type MarksheetLine,
  type ResultCard,
  type Operation,
  type RecordRow,
  type ResourceDef,
} from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import { Banner, Button, SwitchRow, Text, TextInput, useTheme } from '@mysms/ui-native';
import { useState } from 'react';
import { View } from 'react-native';
import type { AssessmentRoute } from '../assessmentRoutes';
import { QueryFeedback, WorkflowNotice, RecordEditor, RecordDelete, LoadingRows } from '../records';
import { firstText } from '../recordText';

export function MarksheetWorkflow({ row, etag }: { row: RecordRow; etag: string | undefined }) {
  const { t } = useI18n();
  const status = String(row.status ?? '');
  const mutation = useMarksheetStep();
  const [reason, setReason] = useState('');
  const [visibleToGuardians, setVisibleToGuardians] = useState(row.visibleToGuardians === true);
  const mayValidate = useCan('assessment.marksheet.validate_marksheet');
  const mayPublish = useCan('assessment.marksheet.publish');
  const mayCancel = useCan('assessment.marksheet.cancel_marksheet');
  const mayReset = useCan('assessment.marksheet.reset_marksheet');
  // a validated marksheet is published once (publishing again is refused), so it is not offered again
  const published = typeof row.publishedAt === 'string' && row.publishedAt !== '';
  return (
    <>
      <Text variant="caption">
        {t('asm.native.currentStatus')}: {enumLabel(t, 'assessment.status', status)}
      </Text>
      {status === 'draft' && mayValidate ? (
        <Button
          loading={mutation.isPending}
          disabled={!etag}
          onPress={() => etag && mutation.mutate({ marksheetId: row.id, step: 'validate', etag })}
        >
          {t('asm.native.validate')}
        </Button>
      ) : null}
      {status === 'validated' && mayPublish && !published ? (
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
              etag && mutation.mutate({ marksheetId: row.id, step: 'publish', etag, visibleToGuardians })
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
              etag &&
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
          onPress={() => etag && mutation.mutate({ marksheetId: row.id, step: 'reset', etag })}
        >
          {t('asm.native.reset')}
        </Button>
      ) : null}
      <WorkflowNotice mutation={mutation} />
    </>
  );
}

export function ResultTemplatePanel({
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

export function StudentResults({ route, tablet }: { route: AssessmentRoute; tablet: boolean }) {
  const { t } = useI18n();
  const mayRead = useCan('assessment.marksheet_line.get_student_results');
  const ownStudentId = useMyStudentIds()[0];
  const studentId = route.params.studentId ?? ownStudentId ?? null;
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

export function MarksheetDetails({ marksheetId, tablet }: { marksheetId: string; tablet: boolean }) {
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

export function GradingSchemeBands({
  gradingSchemeId,
  tablet,
}: {
  gradingSchemeId: string;
  tablet: boolean;
}) {
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
      {mayGenerate && row.status === 'draft' ? (
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
