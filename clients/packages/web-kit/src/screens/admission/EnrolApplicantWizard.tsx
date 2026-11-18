import {
  buildPath,
  screenById,
  useCan,
  useClassSectionOptions,
  useEnrollApplicant,
  useI18n,
  type Application,
  type EnrollApplicantResult,
} from '@mysms/core';
import {
  Banner,
  Button,
  Dialog,
  SegmentedControl,
  SelectField,
  StatusChip,
  Switch,
  TextInput,
} from '@mysms/ui-web';
import { useState } from 'react';
import { Link } from 'react-router';
import { failureText } from './status';

type Placement = 'classSection' | 'batch';
type Stage = 'placement' | 'accounts' | 'review';

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
/** The roll numbers sis keeps are short codes. */
const MAX_ROLL = 20;

/**
 * ADMN-04 Enrol applicant, from ADMN-03: where the student is placed — a class section (K-12), chosen by its name
 * among the institution's classes when the role may read them, or a batch of the application's course (higher
 * education), prefilled with what the office set on the application — and the roll number; whether the student and
 * the guardians get portal accounts; then admission.EnrollApplicant. The answer is the student (linked to SIS-02)
 * and, when accounts were asked for, how that operation ended. An interrupted enrolment (network, 5xx) is sent again
 * with the same Idempotency-Key and the server completes what was left; a refused one is corrected and sent as a new
 * attempt.
 */
export function EnrolApplicantWizard({
  application,
  onClose,
}: {
  application: Application;
  onClose: () => void;
}) {
  const { t } = useI18n();
  const enrol = useEnrollApplicant();
  const [stage, setStage] = useState<Stage>('placement');
  const [placement, setPlacement] = useState<Placement>(
    application.batchId || (application.courseId && !application.sectionId) ? 'batch' : 'classSection',
  );
  const [target, setTarget] = useState(
    (placement === 'batch' ? application.batchId : application.sectionId) ?? '',
  );
  const [rollNumber, setRollNumber] = useState('');
  const [accounts, setAccounts] = useState(true);
  const [result, setResult] = useState<EnrollApplicantResult | null>(null);
  const student = screenById('SIS-02')?.routes[0];
  // the institution's classes by name, when the role may read the three catalogues that name them
  const mayClasses = useCan('academics.class_section.read');
  const mayGrades = useCan('academics.grade_level.read');
  const maySections = useCan('academics.section.read');
  const mayNameClasses = mayClasses && mayGrades && maySections;
  const classes = useClassSectionOptions(application.institutionId, mayNameClasses);
  const targetOk = UUID.test(target.trim());
  const yearMissing = placement === 'classSection' && !application.academicYearId;
  const courseMissing = placement === 'batch' && !application.courseId;
  const placementOk = targetOk && !yearMissing && !courseMissing;

  const send = () =>
    enrol.mutate(
      {
        applicationId: application.id ?? '',
        ...(placement === 'classSection' ? { classSectionId: target.trim() } : { batchId: target.trim() }),
        rollNumber,
        createPortalAccounts: accounts,
      },
      { onSuccess: setResult },
    );

  const footer = result ? (
    <Button onClick={onClose}>{t('common.action.close')}</Button>
  ) : (
    <>
      <Button variant="ghost" onClick={onClose} disabled={enrol.isPending}>
        {t('common.action.cancel')}
      </Button>
      {stage !== 'placement' ? (
        <Button
          variant="secondary"
          disabled={enrol.isPending}
          onClick={() => setStage(stage === 'review' ? 'accounts' : 'placement')}
        >
          {t('common.action.back')}
        </Button>
      ) : null}
      {stage === 'review' ? (
        <Button loading={enrol.isPending} onClick={send}>
          {t('admission.enrol.submit')}
        </Button>
      ) : (
        <Button
          disabled={stage === 'placement' && !placementOk}
          onClick={() => setStage(stage === 'placement' ? 'accounts' : 'review')}
        >
          {t('admission.enrol.next')}
        </Button>
      )}
    </>
  );

  return (
    <Dialog open title={t('screen.ADMN04.title')} onClose={onClose} footer={footer}>
      <p className="hint">
        {application.fullName} · {application.applicationNumber}
      </p>
      {result ? (
        <div className="form">
          <Banner intent="success">{t('admission.enrol.done')}</Banner>
          {student ? (
            <Link to={`/app${buildPath(student, { studentId: result.studentId })}`}>
              {t('admission.enrol.openStudent')}
            </Link>
          ) : null}
          {result.operation ? (
            <p>
              {t('admission.enrol.accounts')}{' '}
              <StatusChip
                intent={
                  result.operation.status === 'succeeded'
                    ? 'success'
                    : result.operation.status === 'failed'
                      ? 'danger'
                      : 'info'
                }
                label={t(`admission.operation.${result.operation.status}`)}
              />
              {result.operation.status === 'failed' ? (
                <span className="hint"> {t('admission.enrol.accountsFailed')}</span>
              ) : null}
            </p>
          ) : null}
        </div>
      ) : stage === 'placement' ? (
        <div className="form">
          <SegmentedControl<Placement>
            label={t('admission.enrol.placement')}
            value={placement}
            onChange={(next) => {
              setPlacement(next);
              setTarget((next === 'batch' ? application.batchId : application.sectionId) ?? '');
            }}
            options={[
              { value: 'classSection', label: t('admission.enrol.classSection') },
              { value: 'batch', label: t('admission.enrol.batch') },
            ]}
          />
          {placement === 'classSection' && classes.data && classes.data.length > 0 ? (
            <SelectField
              label={t('admission.enrol.classSectionChoice')}
              value={target}
              options={[
                { value: '', label: t('admission.enrol.chooseClass') },
                ...classes.data.map((option) => ({ value: option.id, label: option.label })),
              ]}
              onChange={(event) => setTarget(event.target.value)}
            />
          ) : (
            <TextInput
              label={t(placement === 'batch' ? 'admission.enrol.batchId' : 'admission.enrol.classSectionId')}
              hint={t('admission.enrol.idHint')}
              value={target}
              error={target.trim() && !targetOk ? t('common.validation.pattern') : undefined}
              onChange={(event) => setTarget(event.target.value)}
            />
          )}
          {yearMissing ? <Banner intent="warning">{t('admission.enrol.noYear')}</Banner> : null}
          {courseMissing ? <Banner intent="warning">{t('admission.enrol.noCourse')}</Banner> : null}
          <TextInput
            label={t('admission.enrol.rollNumber')}
            maxLength={MAX_ROLL}
            value={rollNumber}
            onChange={(event) => setRollNumber(event.target.value)}
          />
        </div>
      ) : stage === 'accounts' ? (
        <div className="form">
          <Switch
            label={t('admission.enrol.createAccounts')}
            hint={t('admission.enrol.createAccountsHint')}
            checked={accounts}
            onChange={setAccounts}
          />
        </div>
      ) : (
        <div className="form">
          <dl className="facts">
            <dt>{t(placement === 'batch' ? 'admission.enrol.batchId' : 'admission.enrol.classSectionId')}</dt>
            <dd>{classes.data?.find((option) => option.id === target.trim())?.label ?? target.trim()}</dd>
            <dt>{t('admission.enrol.rollNumber')}</dt>
            <dd>{rollNumber.trim() || '—'}</dd>
            <dt>{t('admission.enrol.createAccounts')}</dt>
            <dd>{t(accounts ? 'admission.common.yes' : 'admission.common.no')}</dd>
          </dl>
          <p className="hint">{t('admission.enrol.reviewHint')}</p>
          {enrol.error ? <Banner intent="danger">{failureText(t, enrol.error)}</Banner> : null}
        </div>
      )}
    </Dialog>
  );
}
