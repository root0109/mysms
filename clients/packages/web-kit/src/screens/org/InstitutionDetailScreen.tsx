import {
  resourceDef,
  useAllRecords,
  useCan,
  useCreateRecord,
  useI18n,
  useUpdateRecord,
  type RecordRow,
} from '@mysms/core';
import { Banner, Button, Card, Skeleton, Switch, TextInput } from '@mysms/ui-web';
import { useState } from 'react';
import { useParams } from 'react-router';
import { RecordDetailPage } from '../RecordDetailPage';
import { problemText } from '../recordText';
import { RelatedRecords } from '../RelatedRecords';

const ATTRIBUTES = resourceDef('org.InstitutionAttribute');

/** The input kind of an attribute definition's data type; `select` has no options in the contract, so it is text. */
const INPUT_TYPE: Readonly<Record<string, string>> = {
  number: 'number',
  date: 'date',
  email: 'email',
  url: 'url',
  phone: 'tel',
};

/** One attribute the institution's type asks for: its value, saved on its own (create, or update under If-Match). */
function AttributeRow({
  institutionId,
  definition,
  required,
  saved,
  mayCreate,
  mayUpdate,
}: {
  institutionId: string;
  definition: RecordRow;
  required: boolean;
  saved: RecordRow | undefined;
  mayCreate: boolean;
  mayUpdate: boolean;
}) {
  const { t } = useI18n();
  const create = useCreateRecord(ATTRIBUTES as NonNullable<typeof ATTRIBUTES>);
  const update = useUpdateRecord(ATTRIBUTES as NonNullable<typeof ATTRIBUTES>);
  const [value, setValue] = useState(String(saved?.attrValue ?? ''));
  const [missing, setMissing] = useState(false);
  const name = String(definition.name ?? '');
  const editable = saved ? mayUpdate : mayCreate;
  const error = create.error ?? update.error;
  const save = () => {
    setMissing(required && value.trim() === '');
    if (required && value.trim() === '') return;
    if (saved?.etag)
      update.mutate({
        id: saved.id,
        etag: saved.etag,
        values: { attrValue: value },
        parentId: institutionId,
      });
    else
      create.mutate({
        values: { attrKey: name, attrValue: value, attributeDefinitionId: definition.id },
        parentId: institutionId,
      });
  };
  const label = required ? t('org.attributes.required', { name }) : name;
  return (
    <li className="settings-list__row">
      {definition.dataType === 'boolean' ? (
        <Switch
          label={label}
          checked={value === 'true'}
          disabled={!editable}
          onChange={(on) => setValue(String(on))}
        />
      ) : (
        <TextInput
          label={label}
          type={INPUT_TYPE[String(definition.dataType)] ?? 'text'}
          value={value}
          disabled={!editable}
          error={missing ? t('common.validation.required') : (problemText(t, error) ?? undefined)}
          onChange={(event) => setValue(event.target.value)}
        />
      )}
      {editable ? (
        <Button
          variant="secondary"
          loading={create.isPending || update.isPending}
          disabled={value === String(saved?.attrValue ?? '')}
          aria-label={t('org.attributes.saveOne', { name })}
          onClick={save}
        >
          {t('common.action.save')}
        </Button>
      ) : null}
      {create.isSuccess || update.isSuccess ? <span className="hint">{t('adm.records.saved')}</span> : null}
    </li>
  );
}

/**
 * The type-driven attribute form (ADM-03): the attributes the institution's type defines (org.InstitutionTypeAttribute
 * with their org.AttributeDefinition, in the definitions' order), each with the institution's value and an input for
 * its data type; below, every attribute the institution holds, including ones its type does not define.
 */
function AttributesTab({ institution }: { institution: RecordRow }) {
  const { t } = useI18n();
  const typeId = typeof institution.institutionTypeId === 'string' ? institution.institutionTypeId : '';
  const mayValues = useCan('org.institution_attribute.read');
  const mayTypeAttributes = useCan('org.institution_type_attribute.read');
  const mayDefinitions = useCan('org.attribute_definition.read');
  const mayCreate = useCan('org.institution_attribute.create');
  const mayUpdate = useCan('org.institution_attribute.update');
  const formReadable = mayValues && mayTypeAttributes && mayDefinitions && typeId !== '';
  const typeAttributes = useAllRecords('org.InstitutionTypeAttribute', { parentId: typeId }, formReadable);
  const definitions = useAllRecords('org.AttributeDefinition', {}, formReadable);
  const values = useAllRecords('org.InstitutionAttribute', { parentId: institution.id }, formReadable);
  const reads = [typeAttributes, definitions, values];
  const definitionOf = new Map((definitions.data?.items ?? []).map((row) => [row.id, row]));
  const savedOf = new Map(
    (values.data?.items ?? []).map((row) => [String(row.attributeDefinitionId ?? ''), row]),
  );
  const asked = (typeAttributes.data?.items ?? [])
    .map((row) => ({ row, definition: definitionOf.get(String(row.attributeDefinitionId)) }))
    .filter((each): each is { row: RecordRow; definition: RecordRow } => each.definition !== undefined)
    .sort((a, b) => Number(a.definition.sortOrder ?? 0) - Number(b.definition.sortOrder ?? 0));
  return (
    <>
      <Card title={t('org.attributes.form')}>
        {!typeId ? (
          <p className="hint">{t('org.attributes.noType')}</p>
        ) : !formReadable ? (
          <Banner intent="info">{t('adm.resource.noAccess')}</Banner>
        ) : reads.some((read) => read.isPending) ? (
          <Skeleton label={t('common.state.loading')} lines={3} />
        ) : reads.some((read) => read.isError) ? (
          <Banner
            intent="danger"
            action={
              <Button variant="secondary" onClick={() => reads.forEach((read) => void read.refetch())}>
                {t('common.action.retry')}
              </Button>
            }
          >
            {t('common.error.generic')}
          </Banner>
        ) : asked.length === 0 ? (
          <p className="hint">{t('org.attributes.none')}</p>
        ) : (
          <ul className="settings-list">
            {asked.map(({ row, definition }) => (
              <AttributeRow
                key={`${definition.id}:${String(savedOf.get(definition.id)?.etag ?? '')}`}
                institutionId={institution.id}
                definition={definition}
                required={row.isRequired === true}
                saved={savedOf.get(definition.id)}
                mayCreate={mayCreate}
                mayUpdate={mayUpdate}
              />
            ))}
          </ul>
        )}
      </Card>
      <RelatedRecords
        resource="org.InstitutionAttribute"
        title={t('org.attributes.all')}
        parentId={institution.id}
        columns={['attrKey', 'attrValue']}
      />
    </>
  );
}

/**
 * ADM-03 Institution detail & attributes: the institution's fields with edit and delete in its header, the
 * attribute form its type drives, and its departments, academic years and holidays. Another tenant's institution or
 * one outside the caller's scope is not shown (the server answers 404).
 */
export function InstitutionDetailScreen() {
  const { t } = useI18n();
  const { institutionId = '' } = useParams();
  const scope = `institutionId = "${institutionId}"`;
  const presets = { institutionId };
  return (
    <RecordDetailPage
      resource="org.Institution"
      id={institutionId}
      titleField="name"
      back="/app/org/institutions"
      tabs={[
        {
          key: 'attributes',
          label: t('org.institution.tab.attributes'),
          render: (row) => <AttributesTab institution={row} />,
        },
        {
          key: 'departments',
          label: t('screen.ADM04.title'),
          render: () => (
            <RelatedRecords
              resource="org.Department"
              title={t('screen.ADM04.title')}
              scope={scope}
              presets={presets}
            />
          ),
        },
        {
          key: 'years',
          label: t('screen.ADM05.title'),
          render: () => (
            <RelatedRecords
              resource="org.AcademicYear"
              title={t('screen.ADM05.title')}
              scope={scope}
              presets={presets}
              rowLink={(row) => `/app/org/academic-years?year=${row.id}`}
            />
          ),
        },
        {
          key: 'holidays',
          label: t('screen.ADM06.title'),
          render: () => (
            <RelatedRecords
              resource="org.Holiday"
              title={t('screen.ADM06.title')}
              scope={scope}
              presets={presets}
            />
          ),
        },
      ]}
    />
  );
}
