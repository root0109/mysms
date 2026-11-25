import {
  fieldIn,
  resourceDef,
  useAllRecords,
  useCan,
  useCreatePortalAccount,
  useCreateRecord,
  useI18n,
  usePersonNames,
  usePersonsNamed,
  useRecord,
  type RecordRow,
} from '@mysms/core';
import { Banner, Button, Card, DataTable, SelectField, Switch, TextInput } from '@mysms/ui-web';
import { useState } from 'react';
import { Link } from 'react-router';
import { UUID } from '../ids';
import { RecordDetailPage } from '../RecordDetailPage';
import { problemText } from '../recordText';
import { ListWithDetail } from '../ListWithDetail';

const LINKS = resourceDef('sis.StudentGuardian');

/**
 * Links the guardian to a student found by name (people.ListPersons → sis.ListStudents), with the relationship and
 * what the guardian may do for the student (sis.CreateStudentGuardian under the student). The links a guardian
 * already has are kept by student — the contract lists them on the student's page (SIS-02), not by guardian.
 */
function LinkStudent({ guardian }: { guardian: RecordRow }) {
  const { t } = useI18n();
  const mayPersons = useCan('people.person.read');
  const mayStudents = useCan('sis.student.read');
  const mayTypes = useCan('sis.relationship_type.read');
  const [search, setSearch] = useState('');
  const persons = usePersonsNamed(search, mayPersons && mayStudents);
  const personIds = persons.data ?? [];
  const students = useAllRecords(
    'sis.Student',
    { filter: fieldIn('personId', personIds) },
    personIds.length > 0,
  );
  const names = usePersonNames(personIds, mayPersons);
  const types = useAllRecords('sis.RelationshipType', {}, mayTypes);
  const link = useCreateRecord(LINKS as NonNullable<typeof LINKS>);
  const [student, setStudent] = useState<RecordRow | null>(null);
  const [relationshipTypeId, setRelationshipTypeId] = useState('');
  const [flags, setFlags] = useState({
    isPrimaryContact: false,
    receivesCommunication: true,
    canPickUp: false,
  });
  if (!mayPersons || !mayStudents) return <Banner intent="info">{t('sis.guardians.noLookup')}</Banner>;
  const nameOf = (row: RecordRow) => names.get(String(row.personId)) ?? String(row.admissionNumber ?? '');
  return (
    <Card title={t('sis.guardians.link')}>
      <p className="hint">{t('sis.guardians.linkHint')}</p>
      {link.isSuccess && student ? (
        <Banner intent="success">
          {t('sis.guardians.linked', { student: nameOf(student) })}{' '}
          <Link to={`/app/sis/students/${student.id}`}>{t('sis.guardians.openStudent')}</Link>
        </Banner>
      ) : null}
      {link.error ? <Banner intent="danger">{problemText(t, link.error)}</Banner> : null}
      <TextInput
        label={t('sis.guardians.findStudent')}
        type="search"
        value={search}
        hint={t('sis.guardians.findHint')}
        onChange={(event) => {
          setSearch(event.target.value);
          setStudent(null);
        }}
      />
      {personIds.length > 0 || persons.isSuccess ? (
        <DataTable
          caption={t('common.list.count', { count: students.data?.items.length ?? 0 })}
          columns={[
            { key: 'name', header: t('sis.guardians.student'), render: nameOf },
            {
              key: 'number',
              header: t('sis.guardians.admissionNumber'),
              render: (row) => String(row.admissionNumber ?? '—'),
            },
            {
              key: 'pick',
              header: t('adm.resource.rowActions'),
              render: (row) => (
                <Button
                  variant={student?.id === row.id ? 'primary' : 'secondary'}
                  aria-pressed={student?.id === row.id}
                  aria-label={t('sis.guardians.choose', { student: nameOf(row) })}
                  onClick={() => {
                    link.reset();
                    setStudent(row);
                  }}
                >
                  {t('sis.guardians.chooseShort')}
                </Button>
              ),
            },
          ]}
          rows={students.data?.items ?? []}
          rowKey={(row) => row.id}
          loading={students.isPending && personIds.length > 0}
          loadingLabel={t('common.state.loading')}
          emptyText={t('sis.guardians.noStudents')}
        />
      ) : null}
      {student ? (
        <form
          className="form"
          onSubmit={(event) => {
            event.preventDefault();
            link.mutate({
              parentId: student.id,
              values: {
                guardianId: guardian.id,
                ...(relationshipTypeId ? { relationshipTypeId } : {}),
                ...flags,
                status: 'active',
              },
            });
          }}
        >
          <SelectField
            label={t('sis.guardians.relationship')}
            value={relationshipTypeId}
            options={[
              { value: '', label: '—' },
              ...(types.data?.items ?? [])
                .filter((row) => row.isActive !== false)
                .map((row) => ({ value: row.id, label: String(row.name) })),
            ]}
            onChange={(event) => setRelationshipTypeId(event.target.value)}
          />
          <Switch
            label={t('sis.guardians.primary')}
            checked={flags.isPrimaryContact}
            onChange={(on) => setFlags((old) => ({ ...old, isPrimaryContact: on }))}
          />
          <Switch
            label={t('sis.guardians.communication')}
            checked={flags.receivesCommunication}
            onChange={(on) => setFlags((old) => ({ ...old, receivesCommunication: on }))}
          />
          <Switch
            label={t('sis.guardians.pickUp')}
            checked={flags.canPickUp}
            onChange={(on) => setFlags((old) => ({ ...old, canPickUp: on }))}
          />
          <div className="form__actions">
            <Button type="submit" loading={link.isPending} disabled={link.isSuccess}>
              {t('sis.guardians.linkTo', { student: nameOf(student) })}
            </Button>
          </div>
        </form>
      ) : null}
    </Card>
  );
}

/** One guardian: their record under their person's name, their login account and linking them to students. */
function GuardianDetail({ guardianId }: { guardianId: string }) {
  const { t } = useI18n();
  const mayPersons = useCan('people.person.read');
  const mayAccount = useCan('sis.guardian.create_portal_account');
  const mayLink = useCan('sis.student_guardian.create');
  const mayRead = useCan('sis.guardian.read');
  const read = useRecord(resourceDef('sis.Guardian'), mayRead && UUID.test(guardianId) ? guardianId : null);
  const personId = read.data?.data.personId;
  const names = usePersonNames(typeof personId === 'string' ? [personId] : [], mayPersons);
  const account = useCreatePortalAccount();
  return (
    <>
      {account.isSuccess ? <Banner intent="success">{t('sis.guardians.accountMade')}</Banner> : null}
      {account.error ? <Banner intent="danger">{problemText(t, account.error)}</Banner> : null}
      <RecordDetailPage
        resource="sis.Guardian"
        id={guardianId}
        titleField="personId"
        title={typeof personId === 'string' ? names.get(personId) : undefined}
        back="/app/sis/guardians"
        headerActions={(row) =>
          mayAccount && !row.userId ? (
            <Button
              variant="secondary"
              loading={account.isPending}
              onClick={() => account.mutate({ who: 'guardian', id: row.id })}
            >
              {t('sis.guardians.makeAccount')}
            </Button>
          ) : null
        }
        tabs={[
          {
            key: 'link',
            label: t('sis.guardians.link'),
            when: mayLink,
            render: (row) => <LinkStudent guardian={row} />,
          },
        ]}
      />
    </>
  );
}

/**
 * SIS-04 Guardians: the tenant's guardians (list manager: search, create, edit, delete); a row opens the guardian in
 * place (`?guardian=` in the address) with their name, their login account and linking them to students with the
 * relationship type.
 */
export function GuardiansScreen() {
  return (
    <ListWithDetail
      screenId="SIS-04"
      param="guardian"
      detail={(guardianId) => <GuardianDetail key={guardianId} guardianId={guardianId} />}
    />
  );
}
