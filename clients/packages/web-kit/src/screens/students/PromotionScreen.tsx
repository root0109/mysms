import {
  useAcademicYears,
  useApplyPromotions,
  useCan,
  useClassSectionOptions,
  useGradeLevels,
  useI18n,
  useInstitutions,
  usePromotionStep,
  useOpenPromotions,
  usePromotions,
  useProposePromotions,
  useSections,
  useStudentNames,
  type Promotion,
  type PromotionStep,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import {
  Banner,
  Button,
  Card,
  DataTable,
  SelectField,
  Skeleton,
  StatusChip,
  TextInput,
  type StatusChipProps,
} from '@mysms/ui-web';
import { useState } from 'react';
import { ResourceScreen } from '../ResourceScreen';
import { RESOURCE_SCREENS } from '../resourceScreens';
import { sisFailure } from './sisFailure';

const STATUS: Readonly<Record<string, { label: MessageKey; intent: StatusChipProps['intent'] }>> = {
  proposed: { label: 'sis.promotion.status.proposed', intent: 'warning' },
  approved: { label: 'sis.promotion.status.approved', intent: 'info' },
  rejected: { label: 'sis.promotion.status.rejected', intent: 'danger' },
  applied: { label: 'sis.promotion.status.applied', intent: 'success' },
};
const ENROLMENTS = RESOURCE_SCREENS.find((screen) => screen.id === 'SIS-03');

/**
 * SIS-03 Enrolment & promotion: the year-end promotion wizard (ui/04 "Wizard": select → review → apply → outcome)
 * over the class's students, then the enrolments themselves in the list manager. Proposing writes a proposal for
 * every student placed in the class (a student who has one keeps it); each proposal is approved or rejected; applying
 * ends the approved students' placements and opens next year's, all or none. Each step appears for the role that
 * holds its permission.
 */
export function PromotionScreen() {
  const { t } = useI18n();
  const mayPropose = useCan('sis.promotion.propose_promotions');
  const mayApply = useCan('sis.promotion.apply_promotions');
  const mayApprove = useCan('sis.promotion.approve_promotion');
  const mayReject = useCan('sis.promotion.reject_promotion');
  const mayRead = useCan('sis.promotion.read');
  const mayStudents = useCan('sis.student.read');
  const mayPersons = useCan('people.person.read');
  const mayNames = mayStudents && mayPersons;
  // proposing is the wizard's first step; deciding and applying are each their own role's (an approver may never propose)
  const wizard = mayPropose && mayRead;
  const mayDecide = mayRead && (mayApprove || mayReject);
  const mayApplyAll = mayRead && mayApply;
  const mayReview = mayDecide || mayApplyAll;

  const institutions = useInstitutions(wizard || mayReview);
  const [institutionId, setInstitutionId] = useState('');
  const institution = institutionId || institutions.data?.items[0]?.id || '';
  const classes = useClassSectionOptions(institution || null, wizard);
  const years = useAcademicYears(wizard);
  const grades = useGradeLevels(institution || null, wizard);
  const sections = useSections(institution || null, wizard);
  const [fromClass, setFromClass] = useState('');
  const [toYear, setToYear] = useState('');
  const [toGrade, setToGrade] = useState('');
  const [toSection, setToSection] = useState('');
  const [ids, setIds] = useState<string[]>([]);
  const [reasons, setReasons] = useState<Record<string, string>>({});
  const [outcome, setOutcome] = useState<number | null>(null);
  const propose = useProposePromotions();
  const step = usePromotionStep();
  const apply = useApplyPromotions();
  const promotions = usePromotions(ids);
  // before (or without) a proposal of the caller's own, the review lists what waits in the institution
  const open = useOpenPromotions(institution || null, mayReview && ids.length === 0);
  const rows = (ids.length > 0 ? promotions.data : open.data?.items) ?? [];
  const loading = ids.length > 0 ? promotions.isPending : open.isPending;
  const [bulk, setBulk] = useState<{ approved: number; failed: number; reason: string } | null>(null);
  const [bulkRunning, setBulkRunning] = useState(false);
  const names = useStudentNames(mayNames ? rows.map((row) => row.studentId) : []);
  const approved = rows.filter((row) => row.status === 'approved');
  const proposed = rows.filter((row) => row.status === 'proposed');
  const yearOptions = (years.data?.items ?? []).filter((year) => year.institutionId === institution);
  const error = propose.error ?? step.error ?? apply.error;

  const request = (promotion: Promotion, taken: PromotionStep) => ({
    promotionId: promotion.id ?? '',
    step: taken,
    etag: promotion.etag ?? null,
    ...(reasons[promotion.id ?? ''] ? { reason: reasons[promotion.id ?? ''] } : {}),
  });
  const act = (promotion: Promotion, taken: PromotionStep) => {
    setBulk(null);
    step.mutate(request(promotion, taken));
  };
  /** Approves every proposal and reports each refusal, not only the last one to settle. */
  const approveAll = async () => {
    setBulk(null);
    setBulkRunning(true);
    const results = await Promise.allSettled(
      proposed.map((promotion) => step.mutateAsync(request(promotion, 'approve'))),
    );
    const failures = results.filter(
      (result): result is PromiseRejectedResult => result.status === 'rejected',
    );
    setBulkRunning(false);
    setBulk({
      approved: results.length - failures.length,
      failed: failures.length,
      reason: failures[0] ? sisFailure(t, failures[0].reason) : '',
    });
  };

  return (
    <div className="page">
      {wizard || mayReview ? (
        <>
          <header className="page__header">
            <h1 className="page__title">{t('sis.promotion.title')}</h1>
            <p className="page__subtitle">{t('sis.promotion.intro')}</p>
          </header>
          {error && !bulk ? <Banner intent="danger">{sisFailure(t, error)}</Banner> : null}
          {(institutions.data?.items.length ?? 0) > 1 ? (
            <SelectField
              label={t('sis.promotion.select.institution')}
              options={(institutions.data?.items ?? []).map((item) => ({
                value: item.id ?? '',
                label: item.name,
              }))}
              value={institution}
              onChange={(event) => {
                setInstitutionId(event.target.value);
                setIds([]);
              }}
            />
          ) : null}
          {wizard ? (
            <Card title={t('sis.promotion.select.title')}>
              {institutions.isPending ? (
                <Skeleton label={t('common.state.loading')} lines={3} />
              ) : (
                <form
                  className="form"
                  onSubmit={(event) => {
                    event.preventDefault();
                    if (!fromClass || !toYear || !toGrade) return;
                    setOutcome(null);
                    propose.mutate(
                      {
                        fromClassSectionId: fromClass,
                        toAcademicYearId: toYear,
                        toGradeLevelId: toGrade,
                        ...(toSection ? { toSectionId: toSection } : {}),
                      },
                      {
                        onSuccess: (list) => setIds(list.items.flatMap((item) => (item.id ? [item.id] : []))),
                      },
                    );
                  }}
                >
                  <SelectField
                    label={t('sis.promotion.select.fromClass')}
                    options={[
                      { value: '', label: t('sis.promotion.select.choose') },
                      ...(classes.data ?? []).map((option) => ({ value: option.id, label: option.label })),
                    ]}
                    value={fromClass}
                    onChange={(event) => setFromClass(event.target.value)}
                  />
                  <SelectField
                    label={t('sis.promotion.select.toYear')}
                    options={[
                      { value: '', label: t('sis.promotion.select.choose') },
                      ...yearOptions.map((year) => ({ value: year.id ?? '', label: year.name })),
                    ]}
                    value={toYear}
                    onChange={(event) => setToYear(event.target.value)}
                  />
                  <SelectField
                    label={t('sis.promotion.select.toGrade')}
                    options={[
                      { value: '', label: t('sis.promotion.select.choose') },
                      ...(grades.data ?? []).map((grade) => ({ value: grade.id ?? '', label: grade.name })),
                    ]}
                    value={toGrade}
                    onChange={(event) => setToGrade(event.target.value)}
                  />
                  <SelectField
                    label={t('sis.promotion.select.toSection')}
                    options={[
                      { value: '', label: t('sis.promotion.select.noSection') },
                      ...(sections.data ?? []).map((section) => ({
                        value: section.id ?? '',
                        label: section.name,
                      })),
                    ]}
                    value={toSection}
                    onChange={(event) => setToSection(event.target.value)}
                  />
                  <div className="form__actions">
                    <Button
                      type="submit"
                      loading={propose.isPending}
                      disabled={!fromClass || !toYear || !toGrade}
                    >
                      {t('sis.promotion.select.propose')}
                    </Button>
                  </div>
                </form>
              )}
            </Card>
          ) : null}

          {mayReview ? (
            <Card title={t('sis.promotion.review.title')}>
              {ids.length === 0 ? <p className="hint">{t('sis.promotion.review.openIntro')}</p> : null}
              <p className="hint">
                {t('sis.promotion.review.counts', {
                  proposed: proposed.length,
                  approved: approved.length,
                  total: rows.length,
                })}
              </p>
              {mayApprove && proposed.length > 0 ? (
                <div className="page__actions">
                  <Button
                    variant="secondary"
                    loading={bulkRunning}
                    disabled={step.isPending || bulkRunning}
                    onClick={() => void approveAll()}
                  >
                    {t('sis.promotion.review.approveAll')}
                  </Button>
                </div>
              ) : null}
              {bulk && bulk.failed > 0 ? (
                <Banner intent="danger">
                  {t('sis.promotion.review.bulkFailed', {
                    failed: bulk.failed,
                    total: bulk.failed + bulk.approved,
                    reason: bulk.reason,
                  })}
                </Banner>
              ) : null}
              {bulk && bulk.failed === 0 ? (
                <Banner intent="success">
                  {t('sis.promotion.review.bulkDone', { count: bulk.approved })}
                </Banner>
              ) : null}
              <DataTable<Promotion>
                caption={t('sis.promotion.review.title')}
                columns={[
                  {
                    key: 'student',
                    header: t('sis.promotion.review.student'),
                    render: (row) => names.get(row.studentId) ?? t('sis.promotion.review.someone'),
                  },
                  {
                    key: 'status',
                    header: t('sis.promotion.review.status'),
                    render: (row) => {
                      const state = STATUS[row.status ?? 'proposed'];
                      return (
                        <StatusChip
                          intent={state?.intent}
                          label={t(state?.label ?? 'sis.promotion.status.proposed')}
                        />
                      );
                    },
                  },
                  {
                    key: 'decide',
                    header: t('sis.promotion.review.decide'),
                    render: (row) => {
                      if (row.status !== 'proposed') return null;
                      const name = names.get(row.studentId) ?? t('sis.promotion.review.someone');
                      const busy = step.isPending && step.variables?.promotionId === row.id;
                      return (
                        <div className="page__actions">
                          {mayReject ? (
                            <TextInput
                              label={t('sis.promotion.review.reasonFor', { name })}
                              value={reasons[row.id ?? ''] ?? ''}
                              onChange={(event) =>
                                setReasons({ ...reasons, [row.id ?? '']: event.target.value })
                              }
                            />
                          ) : null}
                          {mayApprove ? (
                            <Button
                              aria-label={t('sis.promotion.review.approveFor', { name })}
                              loading={busy && step.variables?.step === 'approve'}
                              disabled={step.isPending}
                              onClick={() => act(row, 'approve')}
                            >
                              {t('sis.promotion.review.approve')}
                            </Button>
                          ) : null}
                          {mayReject ? (
                            <Button
                              variant="secondary"
                              aria-label={t('sis.promotion.review.rejectFor', { name })}
                              loading={busy && step.variables?.step === 'reject'}
                              disabled={step.isPending}
                              onClick={() => act(row, 'reject')}
                            >
                              {t('sis.promotion.review.reject')}
                            </Button>
                          ) : null}
                        </div>
                      );
                    },
                  },
                ]}
                rows={rows}
                rowKey={(row) => row.id ?? ''}
                loading={loading}
                loadingLabel={t('common.state.loading')}
                emptyText={t('sis.promotion.review.none')}
              />
              {mayApplyAll ? (
                <div className="page__actions">
                  <Button
                    loading={apply.isPending}
                    disabled={approved.length === 0 || apply.isPending}
                    onClick={() => {
                      const applying = approved.flatMap((row) => (row.id ? [row.id] : []));
                      setOutcome(null);
                      apply.mutate(applying, { onSuccess: () => setOutcome(applying.length) });
                    }}
                  >
                    {t('sis.promotion.apply.button', { count: approved.length })}
                  </Button>
                </div>
              ) : null}
              {outcome !== null && !apply.error ? (
                <Banner intent="success">{t('sis.promotion.apply.done', { count: outcome })}</Banner>
              ) : null}
            </Card>
          ) : null}
        </>
      ) : null}
      {ENROLMENTS ? <ResourceScreen screen={ENROLMENTS} /> : null}
    </div>
  );
}
