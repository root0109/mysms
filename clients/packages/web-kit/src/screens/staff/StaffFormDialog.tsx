import {
  STAFF_EMPLOYMENT_TYPES,
  isApiError,
  useCan,
  useCreateStaff,
  useDepartments,
  useDesignations,
  useI18n,
  usePersonNames,
  usePersonsNamed,
  useSession,
  useUpdateStaff,
  type Staff,
  type StaffEmploymentType,
  type StaffFields,
} from '@mysms/core';
import { Banner, Button, Dialog, SelectField, TextInput } from '@mysms/ui-web';
import { useState } from 'react';
import { UUID } from '../ids';
import { EMPLOYMENT_TYPE, staffProblem } from './staffText';

/** The fields of the form, as typed: every value a string, so an empty field clears an optional one. */
type Draft = Record<Exclude<keyof StaffFields, 'maxPeriodsPerWeek' | 'employmentType'>, string> & {
  employmentType: StaffEmploymentType | '';
  maxPeriodsPerWeek: string;
};

const TEXT_FIELDS = [
  'employeeCode',
  'designationId',
  'subDesignation',
  'joiningDate',
  'primaryDepartmentId',
  'qualificationDetail',
  'specialization',
] as const;

function draftOf(staff: Staff | undefined, institutionId: string): Draft {
  return {
    personId: staff?.personId ?? '',
    institutionId: staff?.institutionId ?? institutionId,
    employeeCode: staff?.employeeCode ?? '',
    designationId: staff?.designationId ?? '',
    subDesignation: staff?.subDesignation ?? '',
    employmentType: staff?.employmentType ?? 'full_time',
    joiningDate: staff?.joiningDate ?? '',
    primaryDepartmentId: staff?.primaryDepartmentId ?? '',
    qualificationDetail: staff?.qualificationDetail ?? '',
    specialization: staff?.specialization ?? '',
    maxPeriodsPerWeek: staff?.maxPeriodsPerWeek === undefined ? '' : String(staff.maxPeriodsPerWeek),
  };
}

/** The contract's value of a typed field: trimmed, and undefined when empty (an optional field is cleared). */
const valueOf = (text: string) => (text.trim() === '' ? undefined : text.trim());

/**
 * Adds a staff member (hr.CreateStaff) or edits one (hr.UpdateStaff under the ETag last read, sending only the fields
 * that changed, so another user's change to other fields stays). The person must already be recorded in People:
 * they are found by name when the role may read persons, otherwise by id. The login account and the status are not
 * typed here — they have their own steps on the staff record (HR-02).
 */
export function StaffFormDialog({
  staff,
  etag,
  onClose,
  onSaved,
}: {
  /** The staff member as last read; absent for a new one. */
  staff?: Staff;
  etag?: string | null;
  onClose: () => void;
  onSaved: (saved: Staff) => void;
}) {
  const { t } = useI18n();
  const institutions = useSession((s) => s.me?.institutions) ?? [];
  const mayPersons = useCan('people.person.read');
  const designations = useDesignations(useCan('hr.designation.read'));
  const departments = useDepartments(useCan('org.department.read'));
  const isNew = staff === undefined;
  const [draft, setDraft] = useState<Draft>(() => draftOf(staff, institutions[0]?.id ?? ''));
  const [personSearch, setPersonSearch] = useState('');
  const [checked, setChecked] = useState(false);
  const persons = usePersonsNamed(personSearch, isNew && mayPersons);
  const personNames = usePersonNames(persons.data ?? [], isNew && mayPersons);
  const create = useCreateStaff();
  const update = useUpdateStaff();
  const pending = create.isPending || update.isPending;
  const error = create.error ?? update.error;
  const set = (field: keyof Draft) => (value: string) => setDraft((old) => ({ ...old, [field]: value }));

  const periods = draft.maxPeriodsPerWeek.trim();
  const periodsOk = periods === '' || /^\d{1,3}$/.test(periods);
  const personOk = !isNew || UUID.test(draft.personId.trim());
  const institutionOk = !isNew || draft.institutionId !== '';
  const serverError = (field: string) =>
    isApiError(error) ? error.errors.find((e) => e.field === field)?.message : undefined;

  const fields = (): StaffFields => {
    const typed: StaffFields = {
      personId: draft.personId.trim(),
      institutionId: draft.institutionId,
      employmentType: draft.employmentType || undefined,
      maxPeriodsPerWeek: periods === '' ? undefined : Number(periods),
    };
    for (const field of TEXT_FIELDS) typed[field] = valueOf(draft[field]);
    return typed;
  };

  const submit = () => {
    setChecked(true);
    if (!periodsOk || !personOk || !institutionOk || pending) return;
    const wanted = fields();
    if (isNew) {
      create.mutate(wanted, { onSuccess: onSaved });
      return;
    }
    if (!etag) return;
    const before = draftOf(staff, '');
    const changed = (Object.keys(draft) as (keyof Draft)[]).filter(
      (field) => field !== 'personId' && field !== 'institutionId' && draft[field] !== before[field],
    );
    if (changed.length === 0) {
      onClose();
      return;
    }
    const changes = Object.fromEntries(
      changed.map((field) => [field, wanted[field]]),
    ) as Partial<StaffFields>;
    update.mutate({ staffId: staff.id ?? '', etag, changes }, { onSuccess: onSaved });
  };

  const optional = (label: string) => [{ value: '', label }];
  return (
    <Dialog
      open
      title={t(isNew ? 'hr.staff.form.newTitle' : 'hr.staff.form.editTitle')}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button type="submit" form="staff-form" loading={pending} disabled={!isNew && !etag}>
            {t('common.action.save')}
          </Button>
        </>
      }
    >
      <form
        id="staff-form"
        className="form-grid"
        noValidate
        onSubmit={(event) => {
          event.preventDefault();
          submit();
        }}
      >
        {error ? <Banner intent="danger">{staffProblem(t, error)}</Banner> : null}
        {isNew && mayPersons ? (
          <>
            <TextInput
              label={t('hr.staff.form.personSearch')}
              type="search"
              hint={t('hr.staff.form.personHint')}
              error={
                checked && !personOk && (persons.data?.length ?? 0) === 0
                  ? t('common.validation.required')
                  : undefined
              }
              maxLength={100}
              value={personSearch}
              onChange={(event) => setPersonSearch(event.target.value)}
            />
            {persons.isSuccess && (persons.data?.length ?? 0) === 0 ? (
              <p className="hint">{t('hr.staff.form.personNone')}</p>
            ) : null}
            {(persons.data?.length ?? 0) > 0 ? (
              <SelectField
                label={t('hr.staff.form.person')}
                value={draft.personId}
                error={checked && !personOk ? t('common.validation.required') : undefined}
                onChange={(event) => set('personId')(event.target.value)}
                options={[
                  ...optional(t('hr.staff.form.none')),
                  ...(persons.data ?? []).map((personId) => ({
                    value: personId,
                    label: personNames.get(personId) ?? personId,
                  })),
                ]}
              />
            ) : null}
          </>
        ) : null}
        {isNew && !mayPersons ? (
          <TextInput
            label={t('hr.staff.form.personId')}
            hint={t('hr.staff.form.personIdHint')}
            value={draft.personId}
            error={checked && !personOk ? t('common.validation.pattern') : serverError('personId')}
            onChange={(event) => set('personId')(event.target.value)}
          />
        ) : null}
        {isNew && institutions.length === 0 ? (
          <Banner intent="warning">{t('hr.staff.form.noInstitution')}</Banner>
        ) : null}
        {isNew && institutions.length > 1 ? (
          <SelectField
            label={t('hr.staff.form.institution')}
            value={draft.institutionId}
            onChange={(event) => set('institutionId')(event.target.value)}
            options={institutions.map((institution) => ({
              value: institution.id ?? '',
              label: institution.name,
            }))}
          />
        ) : null}
        <TextInput
          label={t('hr.staff.form.code')}
          hint={t('hr.staff.form.codeHint')}
          maxLength={30}
          value={draft.employeeCode}
          error={serverError('employeeCode')}
          onChange={(event) => set('employeeCode')(event.target.value)}
        />
        {designations.data ? (
          <SelectField
            label={t('hr.staff.form.designation')}
            value={draft.designationId}
            error={serverError('designationId')}
            onChange={(event) => set('designationId')(event.target.value)}
            options={[
              ...optional(t('hr.staff.form.none')),
              ...designations.data.map((designation) => ({
                value: designation.id ?? '',
                label: designation.name,
              })),
            ]}
          />
        ) : null}
        <TextInput
          label={t('hr.staff.form.subDesignation')}
          maxLength={120}
          value={draft.subDesignation}
          onChange={(event) => set('subDesignation')(event.target.value)}
        />
        <SelectField
          label={t('hr.staff.form.employmentType')}
          value={draft.employmentType}
          onChange={(event) => set('employmentType')(event.target.value)}
          options={STAFF_EMPLOYMENT_TYPES.map((type) => ({ value: type, label: t(EMPLOYMENT_TYPE[type]) }))}
        />
        <TextInput
          label={t('hr.staff.form.joiningDate')}
          type="date"
          value={draft.joiningDate}
          onChange={(event) => set('joiningDate')(event.target.value)}
        />
        {departments.data ? (
          <SelectField
            label={t('hr.staff.form.department')}
            value={draft.primaryDepartmentId}
            onChange={(event) => set('primaryDepartmentId')(event.target.value)}
            options={[
              ...optional(t('hr.staff.form.none')),
              ...departments.data
                .filter(
                  (department) => !draft.institutionId || department.institutionId === draft.institutionId,
                )
                .map((department) => ({ value: department.id ?? '', label: department.name })),
            ]}
          />
        ) : null}
        <TextInput
          label={t('hr.staff.form.qualification')}
          maxLength={150}
          value={draft.qualificationDetail}
          onChange={(event) => set('qualificationDetail')(event.target.value)}
        />
        <TextInput
          label={t('hr.staff.form.specialization')}
          maxLength={150}
          value={draft.specialization}
          onChange={(event) => set('specialization')(event.target.value)}
        />
        <TextInput
          label={t('hr.staff.form.maxPeriods')}
          inputMode="numeric"
          value={draft.maxPeriodsPerWeek}
          error={!periodsOk ? t('hr.staff.form.badNumber') : serverError('maxPeriodsPerWeek')}
          onChange={(event) => set('maxPeriodsPerWeek')(event.target.value)}
        />
      </form>
    </Dialog>
  );
}
