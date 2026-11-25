import { resourceDef, useCan, useCreateRecord, useI18n, usePersonNames } from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Banner, Card, SelectField, TextInput } from '@mysms/ui-web';
import { useState } from 'react';
import { Link, useSearchParams } from 'react-router';
import { FileDownloadButton, FileUploadButton } from '../../files/FileTransfer';
import { UUID } from '../ids';
import { problemText } from '../recordText';
import { RelatedRecords } from '../RelatedRecords';
import { PersonPicker } from './PersonPicker';

const DOCUMENTS = resourceDef('people.PersonDocument');
const KINDS = ['certificate', 'id_proof', 'other'] as const;
const KIND_LABEL: Readonly<Record<(typeof KINDS)[number], MessageKey>> = {
  certificate: 'people.documents.kind.certificate',
  id_proof: 'people.documents.kind.idProof',
  other: 'people.documents.kind.other',
};

/** Adds a document to the person: what it is and its name, then the file, which saves it (people.CreatePersonDocument). */
function AddDocument({ personId }: { personId: string }) {
  const { t } = useI18n();
  const create = useCreateRecord(DOCUMENTS as NonNullable<typeof DOCUMENTS>);
  const [kind, setKind] = useState<(typeof KINDS)[number]>('certificate');
  const [name, setName] = useState('');
  return (
    <Card title={t('people.documents.add')}>
      {create.isSuccess ? <Banner intent="success">{t('people.documents.added')}</Banner> : null}
      {create.error ? <Banner intent="danger">{problemText(t, create.error)}</Banner> : null}
      <div className="form">
        <SelectField
          label={t('people.documents.kind')}
          value={kind}
          options={KINDS.map((value) => ({ value, label: t(KIND_LABEL[value]) }))}
          onChange={(event) => setKind(event.target.value as (typeof KINDS)[number])}
        />
        <TextInput
          label={t('people.documents.name')}
          value={name}
          maxLength={200}
          hint={t('people.documents.nameHint')}
          onChange={(event) => setName(event.target.value)}
        />
        <FileUploadButton
          label={t('people.documents.upload')}
          disabled={name.trim() === '' || create.isPending}
          onUploaded={(file) =>
            create.mutate(
              {
                parentId: personId,
                values: { documentType: kind, name: name.trim(), fileId: file.id, status: 'active' },
              },
              { onSuccess: () => setName('') },
            )
          }
        />
      </div>
    </Card>
  );
}

/**
 * SIS-08 Documents: a person's documents (certificates, identity proofs, others), for a student or anyone else the
 * school keeps a person record of. The person is found by name (`?person=` in the address); their documents are
 * listed with a download each, added by uploading the file, and archived or deleted in their editor. The server
 * lists only documents in the caller's institutions.
 */
export function DocumentsScreen() {
  const { t } = useI18n();
  const [search, setSearch] = useSearchParams();
  const mayRead = useCan('people.person_document.read');
  const mayCreate = useCan('people.person_document.create');
  const mayPersons = useCan('people.person.read');
  const asked = search.get('person') ?? '';
  const personId = UUID.test(asked) ? asked : '';
  const names = usePersonNames(personId ? [personId] : [], mayPersons);
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.SIS08.title')}</h1>
        <p className="page__subtitle">{t('people.documents.intro')}</p>
      </header>
      {!mayRead ? (
        <Banner intent="info">{t('adm.resource.noAccess')}</Banner>
      ) : !personId ? (
        <Card title={t('people.documents.whose')}>
          {mayPersons ? (
            <PersonPicker onPick={(id) => setSearch({ person: id })} />
          ) : (
            <Banner intent="info">{t('sis.guardians.noLookup')}</Banner>
          )}
        </Card>
      ) : (
        <>
          <p>
            <Link to="/app/people/person-documents">{t('people.documents.another')}</Link>
          </p>
          <RelatedRecords
            resource="people.PersonDocument"
            title={t('people.documents.of', { name: names.get(personId) ?? t('people.picker.unnamed') })}
            parentId={personId}
            columns={['name', 'documentType', 'documentVersion', 'status']}
            filterField="status"
            rowTool={(row) =>
              typeof row.fileId === 'string' ? (
                <FileDownloadButton fileId={row.fileId} label={t('people.documents.download')} />
              ) : null
            }
          />
          {mayCreate ? <AddDocument personId={personId} /> : null}
        </>
      )}
    </div>
  );
}
