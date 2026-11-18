import {
  isApiError,
  useCan,
  useDepartments,
  useI18n,
  useStaffAssignmentStep,
  useStaffAssignments,
  useSubjectNames,
  type PermissionCode,
  type StaffAssignmentKind,
  type StaffDepartment,
  type StaffSubject,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Banner, Button, Card, SelectField, Skeleton } from '@mysms/ui-web';
import { useState } from 'react';
import { staffProblem } from './staffText';

interface KindText {
  title: MessageKey;
  none: MessageKey;
  add: MessageKey;
  permission: (verb: 'read' | 'create' | 'update' | 'delete') => PermissionCode;
}

const KIND: Readonly<Record<StaffAssignmentKind, KindText>> = {
  departments: {
    title: 'hr.staff.assign.departments',
    none: 'hr.staff.assign.noneDepartments',
    add: 'hr.staff.assign.addDepartment',
    permission: (verb) => `hr.staff_department.${verb}`,
  },
  subjects: {
    title: 'hr.staff.assign.subjects',
    none: 'hr.staff.assign.noneSubjects',
    add: 'hr.staff.assign.addSubject',
    permission: (verb) => `hr.staff_subject.${verb}`,
  },
};

/**
 * One list of a staff member's departments or subjects (hr.staff_department / hr.staff_subject): each row can be
 * re-pointed or removed under its etag, and one more added; every step is offered only with its permission. Names
 * come from the modules that own the departments (org) and subjects (academics).
 */
function AssignmentList({
  kind,
  staffId,
  names,
  options,
}: {
  kind: StaffAssignmentKind;
  staffId: string;
  names: ReadonlyMap<string, string>;
  /** What can be chosen, by id and name. */
  options: readonly { id: string; name: string }[];
}) {
  const { t } = useI18n();
  const text = KIND[kind];
  const mayCreate = useCan(text.permission('create'));
  const mayUpdate = useCan(text.permission('update'));
  const mayDelete = useCan(text.permission('delete'));
  const rows = useStaffAssignments(kind, staffId);
  const step = useStaffAssignmentStep(kind);
  const [adding, setAdding] = useState('');
  const [changes, setChanges] = useState<Record<string, string>>({});
  /** The department or subject a row names. */
  const targetOf = (row: StaffDepartment | StaffSubject) =>
    'departmentId' in row ? row.departmentId : row.subjectId;
  const nameOf = (id: string) => names.get(id) ?? t('hr.staff.assign.unknown', { id });
  const had = new Set((rows.data ?? []).map((row) => targetOf(row)));
  const choices = (except?: string) =>
    options
      .filter((option) => option.id === except || !had.has(option.id))
      .map((option) => ({ value: option.id, label: option.name }));
  const failure =
    step.error && isApiError(step.error) && step.error.status === 409 && !step.error.detail
      ? t('hr.staff.assign.duplicate')
      : step.error
        ? staffProblem(t, step.error)
        : null;

  return (
    <Card title={t(text.title)}>
      {failure ? <Banner intent="danger">{failure}</Banner> : null}
      {rows.isPending ? (
        <Skeleton label={t('common.state.loading')} lines={2} />
      ) : rows.isError ? (
        <Banner
          intent="danger"
          action={
            <Button variant="secondary" onClick={() => void rows.refetch()}>
              {t('common.action.retry')}
            </Button>
          }
        >
          {staffProblem(t, rows.error)}
        </Banner>
      ) : rows.data.length === 0 ? (
        <p className="hint">{t(text.none)}</p>
      ) : (
        <ul className="leave-list">
          {rows.data.map((row) => {
            const id = row.id ?? '';
            const target = targetOf(row);
            const name = nameOf(target);
            const chosen = changes[id] ?? target;
            return (
              <li key={id} className="leave-list__item">
                <span className="leave-list__what">
                  <strong>{name}</strong>
                </span>
                {mayUpdate ? (
                  <>
                    <SelectField
                      label={t('hr.staff.assign.changeFor', { name })}
                      value={chosen}
                      onChange={(event) => setChanges({ ...changes, [id]: event.target.value })}
                      options={choices(target)}
                    />
                    <Button
                      variant="secondary"
                      disabled={step.isPending || chosen === target}
                      onClick={() =>
                        step.mutate(
                          { staffId, action: 'change', rowId: id, etag: row.etag ?? '', targetId: chosen },
                          {
                            onSuccess: () =>
                              setChanges((old) =>
                                Object.fromEntries(Object.entries(old).filter(([key]) => key !== id)),
                              ),
                          },
                        )
                      }
                    >
                      {t('hr.staff.assign.change')}
                    </Button>
                  </>
                ) : null}
                {mayDelete ? (
                  <Button
                    variant="secondary"
                    aria-label={t('hr.staff.assign.removeFor', { name })}
                    disabled={step.isPending}
                    onClick={() =>
                      step.mutate({ staffId, action: 'remove', rowId: id, etag: row.etag ?? '' })
                    }
                  >
                    {t('hr.staff.assign.remove')}
                  </Button>
                ) : null}
              </li>
            );
          })}
        </ul>
      )}
      {mayCreate && choices().length > 0 ? (
        <form
          className="form"
          onSubmit={(event) => {
            event.preventDefault();
            if (!adding) return;
            step.mutate({ staffId, action: 'add', targetId: adding }, { onSuccess: () => setAdding('') });
          }}
        >
          <SelectField
            label={t(text.add)}
            value={adding}
            onChange={(event) => setAdding(event.target.value)}
            options={[{ value: '', label: t('hr.staff.form.none') }, ...choices()]}
          />
          <div className="page__actions">
            <Button
              type="submit"
              disabled={!adding}
              loading={step.isPending && step.variables?.action === 'add'}
            >
              {t('hr.staff.assign.add')}
            </Button>
          </div>
        </form>
      ) : null}
    </Card>
  );
}

/**
 * HR-02 "Departments & subjects": the departments a staff member works in (besides their main one on the profile)
 * and the subjects they teach, each managed with its own permissions.
 */
export function StaffAssignmentsTab({ staffId, institutionId }: { staffId: string; institutionId: string }) {
  const mayDepartments = useCan('hr.staff_department.read');
  const maySubjects = useCan('hr.staff_subject.read');
  const mayDepartmentNames = useCan('org.department.read');
  const maySubjectNames = useCan('academics.subject.read');
  const departments = useDepartments(mayDepartments && mayDepartmentNames);
  const subjectNames = useSubjectNames(maySubjects && maySubjectNames);
  const ownDepartments = (departments.data ?? []).filter((d) => d.institutionId === institutionId);
  return (
    <div className="page__grid">
      {mayDepartments ? (
        <AssignmentList
          kind="departments"
          staffId={staffId}
          names={new Map(ownDepartments.map((d) => [d.id ?? '', d.name]))}
          options={ownDepartments.map((d) => ({ id: d.id ?? '', name: d.name }))}
        />
      ) : null}
      {maySubjects ? (
        <AssignmentList
          kind="subjects"
          staffId={staffId}
          names={subjectNames}
          options={[...subjectNames].map(([id, name]) => ({ id, name }))}
        />
      ) : null}
    </div>
  );
}
