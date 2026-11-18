import {
  isApiError,
  THEME_COLOUR_TOKENS,
  THEME_FONTS,
  THEME_RADIUS_STEPS,
  themeTokens,
  useCan,
  useDeleteTheme,
  useI18n,
  useSaveTheme,
  useThemes,
  type Theme,
  type ThemeDraft,
} from '@mysms/core';
import {
  Banner,
  Button,
  Card,
  DataTable,
  Dialog,
  SelectField,
  StatusChip,
  Switch,
  TextArea,
  TextInput,
  type Column,
} from '@mysms/ui-web';
import { useState, type CSSProperties } from 'react';
import { FileDownloadButton, FileUploadButton } from '../../files/FileTransfer';
import { BrandingCard } from './BrandingCard';
import { problemText } from './problems';
import { RetryBanner } from './shared';

const NAME_MAX = 120;

const RADIUS_LABELS = {
  '-1': 'plt.theme.radius.smaller',
  '0': 'plt.theme.radius.same',
  '1': 'plt.theme.radius.larger',
} as const;

/** An empty theme: the platform's defaults until the administrator overrides a token. */
const blank = (): ThemeDraft => ({
  name: '',
  description: '',
  logoFileId: null,
  isDefault: false,
  tokens: {},
});

const draftOf = (theme: Theme): ThemeDraft => ({
  id: theme.id as string,
  etag: theme.etag as string,
  name: theme.name,
  description: theme.description ?? '',
  logoFileId: theme.logoFileId ?? null,
  isDefault: theme.isDefault === true,
  tokens: { ...themeTokens(theme) },
});

/**
 * The live preview of a draft (ui/04 §2.4): the overridable tokens as the web shell's CSS variables on a sample of
 * the shell, so the administrator sees the brand before saving. Values come from the draft, never typed in here.
 */
function TokenPreview({ tokens }: { tokens: Record<string, string> }) {
  const { t } = useI18n();
  const style: Record<string, string> = {};
  for (const [key, value] of Object.entries(tokens)) {
    if (key.startsWith('color.') && !key.endsWith('.dark')) style[`--sms-${key.replace('.', '-')}`] = value;
  }
  if (tokens['font.family']) style.fontFamily = `"${tokens['font.family']}", var(--sms-font-ui, sans-serif)`;
  return (
    <div
      className="theme-preview"
      style={style as CSSProperties}
      aria-label={t('plt.theme.preview')}
      role="group"
    >
      <div className="theme-preview__bar">{t('app.brand.name')}</div>
      <p>{t('plt.theme.previewText')}</p>
      <Button>{t('plt.theme.previewAction')}</Button>
    </div>
  );
}

/**
 * ADM-07 Branding & theme: the tenant's themes (platform.ListThemes), an editor with the tokens a tenant may override
 * (brand colours with dark variants, UI font, corner radius by one notch), the logo uploaded through the file store,
 * the default mark that public branding reads, and a live preview. What the sign-in page shows comes from
 * platform.GetBranding when the school code is known. Editing needs the theme permissions (tenant administrators).
 */
export function ThemesScreen({ tenantCode = '' }: { tenantCode?: string }) {
  const { t } = useI18n();
  const mayCreate = useCan('platform.theme.create');
  const mayUpdate = useCan('platform.theme.update');
  const mayDelete = useCan('platform.theme.delete');
  const themes = useThemes();
  const save = useSaveTheme();
  const remove = useDeleteTheme();
  const [draft, setDraft] = useState<ThemeDraft | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const editable = draft !== null && (draft.id === undefined ? mayCreate : mayUpdate);
  const nameError =
    draft && draft.name.trim().length === 0
      ? t('common.validation.required')
      : draft && draft.name.trim().length > NAME_MAX
        ? t('common.validation.maxLength', { max: NAME_MAX })
        : undefined;
  const change = (patch: Partial<ThemeDraft>) => {
    setNotice(null);
    setDraft((current) => (current ? { ...current, ...patch } : current));
  };
  const setToken = (key: string, value: string) =>
    setDraft((current) => {
      if (!current) return current;
      const tokens = { ...current.tokens };
      if (value) tokens[key] = value;
      else delete tokens[key];
      return { ...current, tokens };
    });

  const columns: Column<Theme>[] = [
    { key: 'name', header: t('plt.theme.name'), render: (theme) => theme.name },
    {
      key: 'default',
      header: t('plt.theme.default'),
      render: (theme) =>
        theme.isDefault ? <StatusChip intent="success" label={t('plt.theme.isDefault')} /> : null,
    },
    {
      key: 'primary',
      header: t('plt.theme.primary'),
      render: (theme) => themeTokens(theme)['color.primary'] ?? t('plt.theme.platformDefault'),
    },
  ];

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.ADM07.title')}</h1>
        <p className="page__subtitle">{t('plt.theme.intro')}</p>
      </header>
      {notice ? <Banner intent="success">{notice}</Banner> : null}
      {themes.isError ? (
        <RetryBanner onRetry={() => void themes.refetch()} />
      ) : (
        <div className="page__grid">
          <Card
            title={t('plt.theme.list')}
            actions={
              mayCreate ? (
                <Button variant="secondary" onClick={() => setDraft(blank())}>
                  {t('plt.theme.new')}
                </Button>
              ) : null
            }
          >
            <DataTable
              caption={t('plt.theme.list')}
              columns={columns}
              rows={themes.data?.items}
              rowKey={(theme) => theme.id ?? theme.name}
              loading={themes.isPending}
              loadingLabel={t('common.state.loading')}
              emptyText={mayCreate ? t('plt.theme.noneCreate') : t('plt.theme.none')}
              onRowClick={(theme) => {
                setNotice(null);
                setDraft(draftOf(theme));
              }}
            />
          </Card>
          {draft ? (
            <Card
              title={draft.id === undefined ? t('plt.theme.new') : t('plt.theme.edit', { name: draft.name })}
            >
              <form
                noValidate
                onSubmit={(event) => {
                  event.preventDefault();
                  if (!editable || nameError) return;
                  save.mutate(draft, {
                    onSuccess: (saved) => {
                      setDraft(draftOf(saved));
                      setNotice(t('plt.theme.saved'));
                    },
                  });
                }}
              >
                <TextInput
                  label={t('plt.theme.name')}
                  value={draft.name}
                  maxLength={NAME_MAX}
                  required
                  disabled={!editable}
                  error={nameError}
                  onChange={(event) => change({ name: event.target.value })}
                />
                <TextArea
                  label={t('plt.theme.description')}
                  value={draft.description}
                  disabled={!editable}
                  onChange={(event) => change({ description: event.target.value })}
                />
                <Switch
                  label={t('plt.theme.makeDefault')}
                  hint={t('plt.theme.defaultHint')}
                  checked={draft.isDefault}
                  disabled={!editable}
                  onChange={(isDefault) => change({ isDefault })}
                />
                <fieldset className="form-section" disabled={!editable}>
                  <legend>{t('plt.theme.colours')}</legend>
                  {THEME_COLOUR_TOKENS.flatMap((key) => [key, `${key}.dark`]).map((key) => (
                    <TextInput
                      key={key}
                      type="color"
                      label={t('plt.theme.token', { token: key })}
                      value={draft.tokens[key] ?? '#000000'}
                      onChange={(event) => setToken(key, event.target.value.toUpperCase())}
                    />
                  ))}
                  <p className="hint">{t('plt.theme.coloursHint')}</p>
                </fieldset>
                <SelectField
                  label={t('plt.theme.font')}
                  value={draft.tokens['font.family'] ?? ''}
                  disabled={!editable}
                  options={[
                    { value: '', label: t('plt.theme.platformDefault') },
                    ...THEME_FONTS.map((font) => ({ value: font, label: font })),
                  ]}
                  onChange={(event) => setToken('font.family', event.target.value)}
                />
                <SelectField
                  label={t('plt.theme.radius')}
                  value={draft.tokens['radius.step'] ?? ''}
                  disabled={!editable}
                  options={[
                    { value: '', label: t('plt.theme.platformDefault') },
                    ...THEME_RADIUS_STEPS.map((step) => ({ value: step, label: t(RADIUS_LABELS[step]) })),
                  ]}
                  onChange={(event) => setToken('radius.step', event.target.value)}
                />
                <div className="form-section">
                  <p>{t('plt.theme.logo')}</p>
                  {draft.logoFileId ? (
                    <>
                      <FileDownloadButton fileId={draft.logoFileId} label={t('plt.theme.viewLogo')} />
                      {editable ? (
                        <Button variant="ghost" onClick={() => change({ logoFileId: null })}>
                          {t('plt.theme.removeLogo')}
                        </Button>
                      ) : null}
                    </>
                  ) : (
                    <p className="hint">{t('plt.theme.noLogo')}</p>
                  )}
                  {editable ? (
                    <FileUploadButton
                      label={t('plt.theme.uploadLogo')}
                      accept="image/png,image/jpeg,image/webp"
                      onUploaded={(file) => change({ logoFileId: file.id ?? null })}
                    />
                  ) : null}
                </div>
                <TokenPreview tokens={draft.tokens} />
                {save.isError ? <Banner intent="danger">{problemText(t, save.error)}</Banner> : null}
                <div className="form-actions">
                  {editable ? (
                    <Button type="submit" loading={save.isPending} disabled={Boolean(nameError)}>
                      {t('common.action.save')}
                    </Button>
                  ) : null}
                  {draft.id !== undefined && mayDelete ? (
                    <Button variant="danger" onClick={() => setConfirmDelete(true)}>
                      {t('plt.theme.delete')}
                    </Button>
                  ) : null}
                  <Button variant="ghost" onClick={() => setDraft(null)}>
                    {t('common.action.close')}
                  </Button>
                </div>
              </form>
            </Card>
          ) : null}
          <BrandingCard tenantCode={tenantCode} />
        </div>
      )}
      <Dialog
        open={confirmDelete}
        title={t('plt.theme.delete')}
        onClose={() => setConfirmDelete(false)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setConfirmDelete(false)}>
              {t('common.action.cancel')}
            </Button>
            <Button
              variant="danger"
              loading={remove.isPending}
              onClick={() => {
                if (draft?.id === undefined || draft.etag === undefined) return;
                remove.mutate(
                  { id: draft.id, etag: draft.etag },
                  {
                    onSuccess: () => {
                      setConfirmDelete(false);
                      setDraft(null);
                      setNotice(t('plt.theme.deleted'));
                    },
                  },
                );
              }}
            >
              {t('plt.theme.delete')}
            </Button>
          </>
        }
      >
        <p>{t('plt.theme.deleteConfirm', { name: draft?.name ?? '' })}</p>
        {remove.isError ? (
          <Banner intent="danger">
            {isApiError(remove.error) && remove.error.status === 412
              ? t('common.conflict.body')
              : problemText(t, remove.error)}
          </Banner>
        ) : null}
      </Dialog>
    </div>
  );
}
