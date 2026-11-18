import {
  useCan,
  useDeleteSalaryTemplate,
  useDeleteTemplateComponent,
  useI18n,
  useInstitutions,
  usePayCategories,
  usePayComponents,
  useSalaryTemplates,
  useSaveSalaryTemplate,
  useSaveTemplateComponent,
  useTemplateComponents,
  type SalaryTemplate,
  type SalaryTemplateComponent,
} from '@mysms/core';
import {
  Banner,
  Button,
  Card,
  DataTable,
  SelectField,
  StatusChip,
  Switch,
  TextInput,
  type Column,
} from '@mysms/ui-web';
import { useState } from 'react';
import { componentTypeLabel, failureText, revisionOf } from './format';
import { RetryBanner } from './shared';

/** A template component's amount or rate (numeric(14,4)). */
const VALUE = /^\d{1,10}(\.\d{1,4})?$/;

/**
 * HR-05 Salary templates: the reusable salary structures of the caller's institutions and, for the one chosen, its
 * components — each a pay component with its calculation basis (a pay category) and amount or rate. Components are
 * added, changed and removed here; the server refuses (409) to remove a component or a template a staff salary
 * still uses. Components are listed in the order they were added: the contract keeps no order of its own for them.
 */
export function SalaryTemplatesScreen() {
  const { t } = useI18n();
  const mayRead = useCan('payroll.salary_template.read');
  const mayCreate = useCan('payroll.salary_template.create');
  const mayUpdate = useCan('payroll.salary_template.update');
  const mayDelete = useCan('payroll.salary_template.delete');
  const mayReadComponents = useCan('payroll.salary_template_component.read');
  const mayAddComponent = useCan('payroll.salary_template_component.create');
  const mayChangeComponent = useCan('payroll.salary_template_component.update');
  const mayRemoveComponent = useCan('payroll.salary_template_component.delete');
  const mayCatalogues = useCan('payroll.pay_component.read');
  const mayCategories = useCan('payroll.pay_category.read');
  const mayInstitutions = useCan('org.institution.read');

  const templates = useSalaryTemplates(mayRead);
  const institutions = useInstitutions(mayRead && mayInstitutions);
  const payComponents = usePayComponents(mayRead && mayCatalogues);
  const categories = usePayCategories(mayRead && mayCategories);
  const [chosenId, setChosenId] = useState<string | null>(null);
  const chosen = (templates.data?.items ?? []).find((template) => template.id === chosenId);
  const components = useTemplateComponents(mayReadComponents && chosen ? (chosen.id ?? null) : null);
  const saveTemplate = useSaveSalaryTemplate();
  const deleteTemplate = useDeleteSalaryTemplate();
  const saveComponent = useSaveTemplateComponent();
  const deleteComponent = useDeleteTemplateComponent();

  const [newInstitution, setNewInstitution] = useState('');
  const [newName, setNewName] = useState('');
  const [rename, setRename] = useState<{ id: string; name: string; isActive: boolean } | null>(null);
  const [editing, setEditing] = useState<SalaryTemplateComponent | null>(null);
  const [componentId, setComponentId] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [value, setValue] = useState('');
  const [notice, setNotice] = useState<string | null>(null);

  const title = t('screen.HR05.title');
  if (!mayRead)
    return (
      <div className="page">
        <header className="page__header">
          <h1 className="page__title">{title}</h1>
        </header>
        <Banner intent="info">{t('pay.templates.noAccess')}</Banner>
      </div>
    );

  const institutionName = new Map((institutions.data?.items ?? []).map((item) => [item.id, item.name]));
  const componentOf = new Map((payComponents.data?.items ?? []).map((item) => [item.id, item]));
  const categoryName = new Map((categories.data?.items ?? []).map((item) => [item.id, item.name]));
  const error =
    saveTemplate.error ?? deleteTemplate.error ?? saveComponent.error ?? deleteComponent.error ?? null;
  const used = new Set((components.data?.items ?? []).map((item) => item.payComponentId));
  const offered = (payComponents.data?.items ?? []).filter(
    (item) => !used.has(item.id ?? '') || item.id === editing?.payComponentId,
  );
  const pickedComponent = componentId || offered[0]?.id || '';
  const pickedCategory = categoryId || categories.data?.items[0]?.id || '';
  const valueOk = value.trim() === '' || VALUE.test(value.trim());
  const firstInstitution = newInstitution || institutions.data?.items[0]?.id || '';
  const resetComponentForm = () => {
    setEditing(null);
    setComponentId('');
    setCategoryId('');
    setValue('');
  };
  const done = (message: string) => () => setNotice(message);
  const clearOutcome = () => {
    setNotice(null);
    saveTemplate.reset();
    deleteTemplate.reset();
    saveComponent.reset();
    deleteComponent.reset();
  };

  const templateColumns: Column<SalaryTemplate>[] = [
    { key: 'name', header: t('pay.templates.name'), render: (row) => row.name },
    {
      key: 'institution',
      header: t('pay.common.institution'),
      render: (row) => institutionName.get(row.institutionId) ?? row.institutionId,
    },
    {
      key: 'active',
      header: t('pay.templates.active'),
      render: (row) => (
        <StatusChip
          intent={row.isActive === false ? 'neutral' : 'success'}
          label={t(row.isActive === false ? 'pay.templates.inactive' : 'pay.templates.inUse')}
        />
      ),
    },
  ];
  const componentColumns: Column<SalaryTemplateComponent>[] = [
    {
      key: 'component',
      header: t('pay.payslip.component'),
      render: (row) => componentOf.get(row.payComponentId)?.name ?? row.payComponentId,
    },
    {
      key: 'type',
      header: t('pay.payslip.type'),
      render: (row) => componentTypeLabel(t, componentOf.get(row.payComponentId)?.componentType),
    },
    {
      key: 'category',
      header: t('pay.templates.category'),
      render: (row) => categoryName.get(row.payCategoryId) ?? row.payCategoryId,
    },
    { key: 'value', header: t('pay.templates.value'), align: 'end', render: (row) => row.value ?? '' },
    {
      key: 'actions',
      header: t('pay.common.actions'),
      render: (row) => {
        const name = componentOf.get(row.payComponentId)?.name ?? '';
        return (
          <div className="record-actions__buttons">
            {mayChangeComponent ? (
              <Button
                variant="ghost"
                aria-label={t('pay.templates.editComponent', { name })}
                onClick={() => {
                  clearOutcome();
                  setEditing(row);
                  setComponentId(row.payComponentId);
                  setCategoryId(row.payCategoryId);
                  setValue(row.value ?? '');
                }}
              >
                {t('pay.common.edit')}
              </Button>
            ) : null}
            {mayRemoveComponent ? (
              <Button
                variant="ghost"
                aria-label={t('pay.templates.removeComponent', { name })}
                disabled={deleteComponent.isPending || !row.etag}
                onClick={() => {
                  clearOutcome();
                  deleteComponent.mutate(
                    { salaryTemplateId: chosen?.id ?? '', id: row.id ?? '', etag: row.etag ?? '' },
                    { onSuccess: done(t('pay.templates.done.componentRemoved')) },
                  );
                }}
              >
                {t('pay.common.remove')}
              </Button>
            ) : null}
          </div>
        );
      },
    },
  ];

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{title}</h1>
        <p className="page__subtitle">{t('pay.templates.intro')}</p>
      </header>
      {error ? <Banner intent="danger">{failureText(t, error)}</Banner> : null}
      {notice && !error ? <Banner intent="success">{notice}</Banner> : null}
      <div className="page__grid">
        <Card title={t('pay.templates.list')}>
          {templates.isError ? (
            <RetryBanner onRetry={() => void templates.refetch()} />
          ) : (
            <DataTable
              caption={t('pay.templates.list')}
              columns={templateColumns}
              rows={templates.data?.items}
              rowKey={(row) => row.id ?? row.name}
              loading={templates.isPending}
              loadingLabel={t('common.state.loading')}
              emptyText={t(mayCreate ? 'pay.templates.noneCreate' : 'pay.templates.none')}
              onRowClick={(row) => {
                clearOutcome();
                resetComponentForm();
                setRename(null);
                setChosenId(row.id ?? null);
              }}
            />
          )}
          {mayCreate ? (
            mayInstitutions ? (
              <form
                className="form"
                onSubmit={(event) => {
                  event.preventDefault();
                  if (!newName.trim() || !firstInstitution) return;
                  clearOutcome();
                  saveTemplate.mutate(
                    { institutionId: firstInstitution, name: newName, isActive: true },
                    {
                      onSuccess: (created) => {
                        setNewName('');
                        setChosenId(created.id ?? null);
                        setNotice(t('pay.templates.done.created'));
                      },
                    },
                  );
                }}
              >
                <SelectField
                  label={t('pay.common.institution')}
                  options={(institutions.data?.items ?? []).map((item) => ({
                    value: item.id ?? '',
                    label: item.name,
                  }))}
                  value={firstInstitution}
                  onChange={(event) => setNewInstitution(event.target.value)}
                />
                <TextInput
                  label={t('pay.templates.newName')}
                  maxLength={120}
                  value={newName}
                  onChange={(event) => setNewName(event.target.value)}
                />
                <div className="page__actions">
                  <Button
                    type="submit"
                    loading={saveTemplate.isPending}
                    disabled={!newName.trim() || !firstInstitution}
                  >
                    {t('pay.templates.create')}
                  </Button>
                </div>
              </form>
            ) : (
              <p className="hint">{t('pay.common.noInstitutionLookup')}</p>
            )
          ) : null}
        </Card>

        {chosen ? (
          <Card title={t('pay.templates.detail', { name: chosen.name })}>
            {mayUpdate ? (
              <form
                className="form"
                onSubmit={(event) => {
                  event.preventDefault();
                  const draft = rename ?? {
                    id: chosen.id ?? '',
                    name: chosen.name,
                    isActive: chosen.isActive !== false,
                  };
                  const revision = revisionOf(chosen);
                  if (!draft.name.trim() || !revision) return;
                  clearOutcome();
                  saveTemplate.mutate(
                    {
                      ...revision,
                      institutionId: chosen.institutionId,
                      name: draft.name,
                      isActive: draft.isActive,
                    },
                    {
                      onSuccess: () => {
                        setRename(null);
                        setNotice(t('pay.templates.done.saved'));
                      },
                    },
                  );
                }}
              >
                <TextInput
                  label={t('pay.templates.name')}
                  maxLength={120}
                  value={rename?.name ?? chosen.name}
                  onChange={(event) =>
                    setRename({
                      id: chosen.id ?? '',
                      isActive: rename?.isActive ?? chosen.isActive !== false,
                      name: event.target.value,
                    })
                  }
                />
                <Switch
                  label={t('pay.templates.active')}
                  checked={rename?.isActive ?? chosen.isActive !== false}
                  onChange={(checked) =>
                    setRename({ id: chosen.id ?? '', name: rename?.name ?? chosen.name, isActive: checked })
                  }
                />
                <div className="page__actions">
                  {mayDelete ? (
                    <Button
                      variant="danger"
                      type="button"
                      loading={deleteTemplate.isPending}
                      disabled={!chosen.etag}
                      onClick={() => {
                        clearOutcome();
                        deleteTemplate.mutate(
                          { id: chosen.id ?? '', etag: chosen.etag ?? '' },
                          {
                            onSuccess: () => {
                              setChosenId(null);
                              setNotice(t('pay.templates.done.deleted'));
                            },
                          },
                        );
                      }}
                    >
                      {t('pay.templates.delete')}
                    </Button>
                  ) : null}
                  <Button
                    type="submit"
                    loading={saveTemplate.isPending}
                    disabled={rename === null || !chosen.etag}
                  >
                    {t('pay.templates.saveTemplate')}
                  </Button>
                </div>
              </form>
            ) : null}

            <h2 className="sms-card__title">{t('pay.templates.components')}</h2>
            {!mayReadComponents ? (
              <p className="hint">{t('pay.templates.noComponentAccess')}</p>
            ) : components.isError ? (
              <RetryBanner onRetry={() => void components.refetch()} />
            ) : (
              <DataTable
                caption={t('pay.templates.componentsOf', { name: chosen.name })}
                columns={componentColumns}
                rows={components.data?.items}
                rowKey={(row) => row.id ?? row.payComponentId}
                loading={components.isPending}
                loadingLabel={t('common.state.loading')}
                emptyText={t('pay.templates.noComponents')}
                density="compact"
              />
            )}
            <p className="hint">{t('pay.templates.order')}</p>
            {(editing ? mayChangeComponent : mayAddComponent) && mayReadComponents ? (
              !mayCatalogues || !mayCategories ? (
                <p className="hint">{t('pay.templates.noCatalogues')}</p>
              ) : (
                <form
                  className="form"
                  onSubmit={(event) => {
                    event.preventDefault();
                    const revision = revisionOf(editing);
                    if (!pickedComponent || !pickedCategory || !valueOk || !revision) return;
                    clearOutcome();
                    saveComponent.mutate(
                      {
                        ...revision,
                        salaryTemplateId: chosen.id ?? '',
                        payComponentId: pickedComponent,
                        payCategoryId: pickedCategory,
                        value,
                      },
                      {
                        onSuccess: () => {
                          setNotice(
                            t(
                              editing
                                ? 'pay.templates.done.componentSaved'
                                : 'pay.templates.done.componentAdded',
                            ),
                          );
                          resetComponentForm();
                        },
                      },
                    );
                  }}
                >
                  <h3 className="sms-card__title">
                    {t(editing ? 'pay.templates.changeComponent' : 'pay.templates.addComponent')}
                  </h3>
                  {offered.length === 0 && !editing ? (
                    <p className="hint">{t('pay.templates.allAdded')}</p>
                  ) : (
                    <>
                      <SelectField
                        label={t('pay.payslip.component')}
                        options={offered.map((item) => ({
                          value: item.id ?? '',
                          label: `${item.name} (${componentTypeLabel(t, item.componentType)})`,
                        }))}
                        value={pickedComponent}
                        onChange={(event) => setComponentId(event.target.value)}
                      />
                      <SelectField
                        label={t('pay.templates.category')}
                        options={(categories.data?.items ?? []).map((item) => ({
                          value: item.id ?? '',
                          label: item.name,
                        }))}
                        value={pickedCategory}
                        onChange={(event) => setCategoryId(event.target.value)}
                      />
                      <TextInput
                        label={t('pay.templates.valueOptional')}
                        inputMode="decimal"
                        value={value}
                        error={valueOk ? undefined : t('pay.templates.badValue')}
                        onChange={(event) => setValue(event.target.value)}
                      />
                      <div className="page__actions">
                        {editing ? (
                          <Button variant="secondary" type="button" onClick={resetComponentForm}>
                            {t('pay.common.cancelEdit')}
                          </Button>
                        ) : null}
                        <Button
                          type="submit"
                          loading={saveComponent.isPending}
                          disabled={!valueOk || !pickedComponent}
                        >
                          {t(editing ? 'pay.templates.saveComponent' : 'pay.templates.add')}
                        </Button>
                      </div>
                    </>
                  )}
                </form>
              )
            ) : null}
          </Card>
        ) : null}
      </div>
    </div>
  );
}
