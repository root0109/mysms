import {
  useCan,
  useCreateGradingBand,
  useCreateGradingScheme,
  useDeleteGradingBand,
  useDeleteGradingScheme,
  useGradingBands,
  useGradingSchemes,
  useI18n,
  useUpdateGradingBand,
  useUpdateGradingScheme,
  type GradingBand,
  type GradingBandDraft,
  type GradingScheme,
  type GradingSchemeDraft,
} from '@mysms/core';
import {
  Banner,
  Button,
  Card,
  DataTable,
  Dialog,
  SelectField,
  Skeleton,
  Switch,
  TextInput,
  type Column,
} from '@mysms/ui-web';
import { useEffect, useState } from 'react';
import { assessmentErrorText as message } from './assessmentText';
import { OfflineBanner } from './assessmentUi';

type SchemeForm = {
  institutionId: string;
  academicYearId: string;
  name: string;
  schemeType: 'scholastic' | 'co_scholastic';
  gradingMethod: 'percentage_band' | 'cbse' | 'pass_fail' | 'descriptive';
  isActive: boolean;
  isTemplate: boolean;
};

type BandForm = {
  minPercent: string;
  maxPercent: string;
  gradeLabel: string;
  gradePoint: string;
  description: string;
  isPass: boolean;
};

const EMPTY_SCHEME: SchemeForm = {
  institutionId: '',
  academicYearId: '',
  name: '',
  schemeType: 'scholastic',
  gradingMethod: 'percentage_band',
  isActive: true,
  isTemplate: false,
};
const EMPTY_BAND: BandForm = {
  minPercent: '',
  maxPercent: '',
  gradeLabel: '',
  gradePoint: '',
  description: '',
  isPass: true,
};

function GradingBandManager({ scheme }: { scheme: GradingScheme }) {
  const { t } = useI18n();
  const mayRead = useCan('assessment.grading_band.read');
  const mayCreate = useCan('assessment.grading_band.create');
  const mayUpdate = useCan('assessment.grading_band.update');
  const mayDelete = useCan('assessment.grading_band.delete');
  const bands = useGradingBands(mayRead ? (scheme.id ?? null) : null);
  const create = useCreateGradingBand();
  const update = useUpdateGradingBand();
  const remove = useDeleteGradingBand();
  const [selectedId, setSelectedId] = useState('');
  const [form, setForm] = useState<BandForm>(EMPTY_BAND);
  const [deleteTarget, setDeleteTarget] = useState<GradingBand | null>(null);
  const [failure, setFailure] = useState<unknown>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const selected = bands.data?.items.find((item) => item.id === selectedId);
  useEffect(() => {
    setForm(
      selected
        ? {
            minPercent: selected.minPercent,
            maxPercent: selected.maxPercent,
            gradeLabel: selected.gradeLabel,
            gradePoint: selected.gradePoint ?? '',
            description: selected.description ?? '',
            isPass: selected.isPass !== false,
          }
        : EMPTY_BAND,
    );
  }, [selected]);
  if (!mayRead)
    return (
      <Card title={t('asm.web.bands')}>
        <p className="hint">{t('adm.records.forbidden')}</p>
      </Card>
    );
  if (bands.isPending) return <Skeleton label={t('common.state.loading')} lines={4} />;
  if (bands.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void bands.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {message(bands.error, t)}
      </Banner>
    );
  const select = (item: GradingBand | null) => {
    setSelectedId(item?.id ?? '');
    setFailure(null);
    setNotice(null);
  };
  const save = async () => {
    setFailure(null);
    setNotice(null);
    try {
      if (selected?.id) {
        if (!selected.etag) throw new Error('The grading band has no entity tag.');
        await update.mutateAsync({
          gradingBandId: selected.id,
          etag: selected.etag,
          gradingBandUpdate: {
            minPercent: form.minPercent,
            maxPercent: form.maxPercent,
            gradeLabel: form.gradeLabel,
            ...(form.gradePoint.trim() ? { gradePoint: form.gradePoint.trim() } : {}),
            ...(form.description.trim() ? { description: form.description.trim() } : {}),
            isPass: form.isPass,
          },
        });
      } else {
        const draft: GradingBandDraft = {
          gradingSchemeId: scheme.id,
          minPercent: form.minPercent,
          maxPercent: form.maxPercent,
          gradeLabel: form.gradeLabel,
          ...(form.gradePoint.trim() ? { gradePoint: form.gradePoint.trim() } : {}),
          ...(form.description.trim() ? { description: form.description.trim() } : {}),
          isPass: form.isPass,
        };
        await create.mutateAsync(draft);
      }
      setNotice(t('asm.web.saved'));
      await bands.refetch();
      if (!selected) select(null);
    } catch (error) {
      setFailure(error);
    }
  };
  const removeBand = async () => {
    if (!deleteTarget?.id || !deleteTarget.etag) return;
    setFailure(null);
    try {
      await remove.mutateAsync({ gradingBandId: deleteTarget.id, etag: deleteTarget.etag });
      setDeleteTarget(null);
      select(null);
      setNotice(t('asm.web.deleted'));
      await bands.refetch();
    } catch (error) {
      setFailure(error);
    }
  };
  const rows = bands.data?.items ?? [];
  const columns: Column<GradingBand>[] = [
    { key: 'grade', header: t('asm.web.gradeLabel'), render: (item) => item.gradeLabel },
    { key: 'min', header: t('asm.web.minPercent'), align: 'end', render: (item) => item.minPercent },
    { key: 'max', header: t('asm.web.maxPercent'), align: 'end', render: (item) => item.maxPercent },
    {
      key: 'point',
      header: t('asm.native.gradePoint'),
      align: 'end',
      render: (item) => item.gradePoint ?? t('common.state.notAvailable'),
    },
    {
      key: 'pass',
      header: t('asm.web.isPass'),
      render: (item) => (item.isPass ? t('asm.native.pass') : t('asm.native.fail')),
    },
    {
      key: 'actions',
      header: t('adm.records.actions'),
      render: (item) => (
        <>
          {mayUpdate ? (
            <Button variant="ghost" onClick={() => select(item)}>
              {t('common.action.edit')}
            </Button>
          ) : null}
          {mayDelete ? (
            <Button variant="danger" disabled={!item.etag} onClick={() => setDeleteTarget(item)}>
              {t('common.action.delete')}
            </Button>
          ) : null}
        </>
      ),
    },
  ];
  const busy = create.isPending || update.isPending || remove.isPending;
  return (
    <>
      <Card
        title={t('asm.web.bands')}
        actions={
          mayCreate ? (
            <Button variant="secondary" onClick={() => select(null)}>
              {t('asm.web.newBand')}
            </Button>
          ) : null
        }
      >
        <DataTable
          caption={t('asm.web.bands')}
          rows={rows}
          rowKey={(item) => item.id ?? item.gradeLabel}
          loading={false}
          loadingLabel={t('common.state.loading')}
          emptyText={t('asm.web.noBands')}
          columns={columns}
        />
      </Card>
      {selected || (selectedId === '' && mayCreate) ? (
        <Card title={selected ? selected.gradeLabel : t('asm.web.newBand')}>
          <div className="page__form">
            <TextInput
              label={t('asm.web.minPercent')}
              type="number"
              inputMode="decimal"
              required
              value={form.minPercent}
              onChange={(event) => setForm((value) => ({ ...value, minPercent: event.target.value }))}
            />
            <TextInput
              label={t('asm.web.maxPercent')}
              type="number"
              inputMode="decimal"
              required
              value={form.maxPercent}
              onChange={(event) => setForm((value) => ({ ...value, maxPercent: event.target.value }))}
            />
            <TextInput
              label={t('asm.web.gradeLabel')}
              required
              value={form.gradeLabel}
              onChange={(event) => setForm((value) => ({ ...value, gradeLabel: event.target.value }))}
            />
            <TextInput
              label={t('asm.native.gradePoint')}
              type="number"
              inputMode="decimal"
              value={form.gradePoint}
              onChange={(event) => setForm((value) => ({ ...value, gradePoint: event.target.value }))}
            />
            <TextInput
              label={t('asm.web.description')}
              value={form.description}
              onChange={(event) => setForm((value) => ({ ...value, description: event.target.value }))}
            />
            <Switch
              label={t('asm.web.isPass')}
              checked={form.isPass}
              onChange={(isPass) => setForm((value) => ({ ...value, isPass }))}
            />
          </div>
          <div className="page__actions">
            <Button
              loading={busy}
              disabled={
                !form.minPercent ||
                !form.maxPercent ||
                !form.gradeLabel.trim() ||
                (!selected && !mayCreate) ||
                (selected && !mayUpdate)
              }
              onClick={() => void save()}
            >
              {selected ? t('common.action.save') : t('asm.web.saveBand')}
            </Button>
            {selected ? (
              <Button variant="ghost" onClick={() => select(null)}>
                {t('common.action.cancel')}
              </Button>
            ) : null}
          </div>
        </Card>
      ) : null}
      {notice ? <Banner intent="success">{notice}</Banner> : null}
      {failure ? <Banner intent="danger">{message(failure, t)}</Banner> : null}
      <Dialog
        open={deleteTarget !== null}
        title={t('asm.web.deleteBand')}
        onClose={() => setDeleteTarget(null)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setDeleteTarget(null)}>
              {t('common.action.cancel')}
            </Button>
            <Button variant="danger" loading={remove.isPending} onClick={() => void removeBand()}>
              {t('common.action.delete')}
            </Button>
          </>
        }
      >
        <p>{t('adm.records.confirmDelete')}</p>
      </Dialog>
    </>
  );
}

export function GradingSchemesScreen() {
  const { t } = useI18n();
  const mayRead = useCan('assessment.grading_scheme.read');
  const mayCreate = useCan('assessment.grading_scheme.create');
  const mayUpdate = useCan('assessment.grading_scheme.update');
  const mayDelete = useCan('assessment.grading_scheme.delete');
  const schemes = useGradingSchemes(mayRead);
  const create = useCreateGradingScheme();
  const update = useUpdateGradingScheme();
  const remove = useDeleteGradingScheme();
  const [selectedId, setSelectedId] = useState('');
  const [form, setForm] = useState<SchemeForm>(EMPTY_SCHEME);
  const [deleteTarget, setDeleteTarget] = useState<GradingScheme | null>(null);
  const [failure, setFailure] = useState<unknown>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const selected = schemes.data?.items.find((item) => item.id === selectedId);
  useEffect(() => {
    setForm(
      selected
        ? {
            institutionId: selected.institutionId,
            academicYearId: selected.academicYearId ?? '',
            name: selected.name,
            schemeType: selected.schemeType,
            gradingMethod: selected.gradingMethod ?? 'percentage_band',
            isActive: selected.isActive !== false,
            isTemplate: selected.isTemplate === true,
          }
        : EMPTY_SCHEME,
    );
  }, [selected]);
  if (!mayRead)
    return (
      <Card title={t('screen.ASM13.title')}>
        <p className="hint">{t('asm.web.noAccess')}</p>
      </Card>
    );
  if (schemes.isPending) return <Skeleton label={t('common.state.loading')} lines={7} />;
  if (schemes.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void schemes.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {message(schemes.error, t)}
      </Banner>
    );
  const rows = schemes.data?.items ?? [];
  const select = (item: GradingScheme | null) => {
    setSelectedId(item?.id ?? '');
    setFailure(null);
    setNotice(null);
  };
  const save = async () => {
    setFailure(null);
    setNotice(null);
    try {
      if (selected?.id) {
        if (!selected.etag) throw new Error('The grading scheme has no entity tag.');
        await update.mutateAsync({
          gradingSchemeId: selected.id,
          etag: selected.etag,
          gradingSchemeUpdate: {
            institutionId: form.institutionId,
            academicYearId: form.academicYearId || undefined,
            name: form.name,
            schemeType: form.schemeType,
            gradingMethod: form.gradingMethod,
            isActive: form.isActive,
            isTemplate: form.isTemplate,
          },
        });
      } else {
        const draft: GradingSchemeDraft = {
          institutionId: form.institutionId,
          ...(form.academicYearId.trim() ? { academicYearId: form.academicYearId.trim() } : {}),
          name: form.name.trim(),
          schemeType: form.schemeType,
          gradingMethod: form.gradingMethod,
          isActive: form.isActive,
          isTemplate: form.isTemplate,
        };
        await create.mutateAsync(draft);
      }
      setNotice(t('asm.web.saved'));
      await schemes.refetch();
      if (!selected) select(null);
    } catch (error) {
      setFailure(error);
    }
  };
  const removeScheme = async () => {
    if (!deleteTarget?.id || !deleteTarget.etag) return;
    setFailure(null);
    try {
      await remove.mutateAsync({ gradingSchemeId: deleteTarget.id, etag: deleteTarget.etag });
      setDeleteTarget(null);
      select(null);
      setNotice(t('asm.web.deleted'));
      await schemes.refetch();
    } catch (error) {
      setFailure(error);
    }
  };
  const columns: Column<GradingScheme>[] = [
    { key: 'name', header: t('asm.web.schemeName'), render: (item) => item.name },
    {
      key: 'type',
      header: t('asm.web.schemeType'),
      render: (item) => t(('enum.assessment.gradingSchemeType.' + item.schemeType) as never),
    },
    {
      key: 'method',
      header: t('asm.web.gradingMethod'),
      render: (item) =>
        t(('enum.assessment.gradingMethod.' + (item.gradingMethod ?? 'percentage_band')) as never),
    },
    {
      key: 'active',
      header: t('asm.native.active'),
      render: (item) => (item.isActive === false ? t('asm.native.inactive') : t('asm.native.active')),
    },
    {
      key: 'template',
      header: t('asm.web.template'),
      render: (item) => (item.isTemplate ? t('adm.records.yes') : t('adm.records.no')),
    },
    {
      key: 'actions',
      header: t('adm.records.actions'),
      render: (item) => (
        <>
          <Button variant="ghost" onClick={() => select(item)}>
            {t('common.action.edit')}
          </Button>
          {mayDelete ? (
            <Button variant="danger" disabled={!item.etag} onClick={() => setDeleteTarget(item)}>
              {t('common.action.delete')}
            </Button>
          ) : null}
        </>
      ),
    },
  ];
  const busy = create.isPending || update.isPending || remove.isPending;
  return (
    <div className="page">
      <OfflineBanner />
      <header className="page__header">
        <h1 className="page__title">{t('screen.ASM13.title')}</h1>
        <p className="page__subtitle">{t('asm.web.gradeScheme')}</p>
      </header>
      <Card
        title={t('screen.ASM13.title')}
        actions={mayCreate ? <Button onClick={() => select(null)}>{t('asm.web.newScheme')}</Button> : null}
      >
        <DataTable
          caption={t('screen.ASM13.title')}
          rows={rows}
          rowKey={(item) => item.id ?? item.name}
          loading={false}
          loadingLabel={t('common.state.loading')}
          emptyText={t('asm.web.noSchemes')}
          columns={columns}
          onRowClick={select}
        />
      </Card>
      {selected || (selectedId === '' && mayCreate) ? (
        <Card title={selected ? selected.name : t('asm.web.newScheme')}>
          <div className="page__form">
            <TextInput
              label={t('asm.web.schemeName')}
              required
              value={form.name}
              onChange={(event) => setForm((value) => ({ ...value, name: event.target.value }))}
            />
            <TextInput
              label={t('asm.web.institution')}
              required
              value={form.institutionId}
              onChange={(event) => setForm((value) => ({ ...value, institutionId: event.target.value }))}
            />
            <TextInput
              label={t('asm.web.academicYear')}
              value={form.academicYearId}
              onChange={(event) => setForm((value) => ({ ...value, academicYearId: event.target.value }))}
            />
            <SelectField
              label={t('asm.web.schemeType')}
              value={form.schemeType}
              onChange={(event) =>
                setForm((value) => ({ ...value, schemeType: event.target.value as SchemeForm['schemeType'] }))
              }
              options={[
                { value: 'scholastic', label: t('enum.assessment.gradingSchemeType.scholastic') },
                { value: 'co_scholastic', label: t('enum.assessment.gradingSchemeType.coScholastic') },
              ]}
            />
            <SelectField
              label={t('asm.web.gradingMethod')}
              value={form.gradingMethod}
              onChange={(event) =>
                setForm((value) => ({
                  ...value,
                  gradingMethod: event.target.value as SchemeForm['gradingMethod'],
                }))
              }
              options={[
                { value: 'percentage_band', label: t('enum.assessment.gradingMethod.percentageBand') },
                { value: 'cbse', label: t('enum.assessment.gradingMethod.cbse') },
                { value: 'pass_fail', label: t('enum.assessment.gradingMethod.passFail') },
                { value: 'descriptive', label: t('enum.assessment.gradingMethod.descriptive') },
              ]}
            />
            <Switch
              label={t('asm.native.active')}
              checked={form.isActive}
              onChange={(isActive) => setForm((value) => ({ ...value, isActive }))}
            />
            <Switch
              label={t('asm.web.template')}
              checked={form.isTemplate}
              onChange={(isTemplate) => setForm((value) => ({ ...value, isTemplate }))}
            />
          </div>
          <div className="page__actions">
            <Button
              loading={busy}
              disabled={
                !form.name.trim() ||
                !form.institutionId.trim() ||
                (!selected && !mayCreate) ||
                (selected && !mayUpdate)
              }
              onClick={() => void save()}
            >
              {selected ? t('common.action.save') : t('asm.web.saveScheme')}
            </Button>
            {selected ? (
              <Button variant="ghost" onClick={() => select(null)}>
                {t('common.action.cancel')}
              </Button>
            ) : null}
          </div>
        </Card>
      ) : null}
      {selected ? <GradingBandManager scheme={selected} /> : null}
      {notice ? <Banner intent="success">{notice}</Banner> : null}
      {failure ? <Banner intent="danger">{message(failure, t)}</Banner> : null}
      <Dialog
        open={deleteTarget !== null}
        title={t('asm.web.deleteScheme')}
        onClose={() => setDeleteTarget(null)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setDeleteTarget(null)}>
              {t('common.action.cancel')}
            </Button>
            <Button variant="danger" loading={remove.isPending} onClick={() => void removeScheme()}>
              {t('common.action.delete')}
            </Button>
          </>
        }
      >
        <p>{t('adm.records.confirmDelete')}</p>
      </Dialog>
    </div>
  );
}
