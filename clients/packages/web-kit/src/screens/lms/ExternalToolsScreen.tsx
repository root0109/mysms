import {
  lmsFailureText,
  useActivateExternalTool,
  useCan,
  useExternalToolLinks,
  useExternalToolResources,
  useExternalTools,
  useI18n,
  useLaunchExternalTool,
  type ExternalTool,
  type LtiLaunch,
} from '@mysms/core';
import { Banner, Button, Card, DataTable, Dialog, Skeleton, StatusChip, TextInput } from '@mysms/ui-web';
import { useEffect, useRef, useState } from 'react';

/**
 * LMS-49 External tools (LTI): the tenant's tool registry with paid tools activated by their licence key, the
 * placed resources with their links, and a signed LTI 1.1 launch posted to the tool in a new tab. LTI 1.3 waits on
 * its platform dependency; until then launches are 1.1 only.
 */
export function ExternalToolsScreen() {
  const { t } = useI18n();
  const mayRead = useCan('lms.external_tool.read');
  const tools = useExternalTools();
  const title = t('screen.LMS49.title');
  if (!mayRead)
    return (
      <Card title={title}>
        <p className="hint">{t('lms.common.forbidden')}</p>
      </Card>
    );
  if (tools.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;
  if (tools.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void tools.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    );
  return <ToolsPanel tools={tools.data.items} />;
}

function ToolsPanel({ tools }: { tools: ExternalTool[] }) {
  const { t } = useI18n();
  const mayActivate = useCan('lms.external_tool.activate_external_tool');
  const [activating, setActivating] = useState<ExternalTool | null>(null);
  return (
    <Card title={t('screen.LMS49.title')}>
      <DataTable
        caption={t('screen.LMS49.title')}
        columns={[
          { key: 'title', header: t('lms.tools.tool'), render: (row: ExternalTool) => row.title ?? '' },
          {
            key: 'state',
            header: t('lms.tools.state'),
            render: (row: ExternalTool) =>
              row.isPaid && !row.isIncluded ? (
                <StatusChip intent="warning" label={t('lms.tools.needsLicence')} />
              ) : (
                <StatusChip intent="success" label={t('lms.tools.enabled')} />
              ),
          },
          {
            key: 'actions',
            header: t('lms.bank.actions'),
            render: (row: ExternalTool) =>
              mayActivate && row.isPaid && !row.isIncluded ? (
                <Button variant="secondary" onClick={() => setActivating(row)}>
                  {t('lms.tools.activate')}
                </Button>
              ) : null,
          },
        ]}
        rows={tools}
        rowKey={(row) => row.id ?? ''}
        loadingLabel={t('common.state.loading')}
        emptyText={t('common.state.empty')}
      />
      <ResourcesPanel />
      <ActivateDialog tool={activating} onClose={() => setActivating(null)} />
    </Card>
  );
}

function ActivateDialog({ tool, onClose }: { tool: ExternalTool | null; onClose: () => void }) {
  const { t } = useI18n();
  const activate = useActivateExternalTool();
  const [key, setKey] = useState('');
  return (
    <Dialog open={tool !== undefined && tool !== null} title={t('lms.tools.activate')} onClose={onClose}>
      <p>{tool?.title}</p>
      <TextInput
        label={t('lms.tools.licenceKey')}
        value={key}
        onChange={(event) => setKey(event.target.value)}
      />
      {activate.isError && <Banner intent="danger">{lmsFailureText(t, activate.error)}</Banner>}
      <Button
        onClick={() =>
          tool?.id && activate.mutate({ externalToolId: tool.id, activationKey: key }, { onSuccess: onClose })
        }
        disabled={activate.isPending || key.trim() === ''}
      >
        {t('lms.tools.activate')}
      </Button>
    </Dialog>
  );
}

/** The placed resources with their links; a link launches in a new tab through the signed form. */
function ResourcesPanel() {
  const { t } = useI18n();
  const mayResources = useCan('lms.external_tool_resource.read');
  const resources = useExternalToolResources();
  const [selected, setSelected] = useState<string | null>(null);
  if (!mayResources) return null;
  const rows = resources.data?.items ?? [];
  return (
    <section aria-label={t('lms.tools.resources')}>
      <h2>{t('lms.tools.resources')}</h2>
      {rows.length === 0 ? (
        <p className="hint">{t('common.state.empty')}</p>
      ) : (
        <ul>
          {rows.map((resource) => (
            <li key={resource.id}>
              <Button
                variant={selected === resource.id ? 'primary' : 'secondary'}
                onClick={() => setSelected(resource.id ?? null)}
              >
                {resource.title}
              </Button>
            </li>
          ))}
        </ul>
      )}
      {selected !== undefined && selected !== null && <LinksPanel externalToolResourceId={selected} />}
    </section>
  );
}

function LinksPanel({ externalToolResourceId }: { externalToolResourceId: string }) {
  const { t } = useI18n();
  const mayLinks = useCan('lms.external_tool_link.read');
  const mayLaunch = useCan('lms.external_tool_link.launch_external_tool');
  const links = useExternalToolLinks(mayLinks ? externalToolResourceId : null);
  const launch = useLaunchExternalTool();
  const [pending, setPending] = useState<LtiLaunch | null>(null);
  if (!mayLinks) return null;
  if (links.isPending) return <Skeleton label={t('common.state.loading')} lines={2} />;
  if (links.isError) return <Banner intent="danger">{t('common.error.generic')}</Banner>;
  return (
    <>
      {launch.isError && <Banner intent="danger">{lmsFailureText(t, launch.error)}</Banner>}
      <ul>
        {links.data.items.map((link) => (
          <li key={link.id}>
            {link.linkTitle ?? link.linkRef}
            {mayLaunch && link.id && (
              <Button
                variant="secondary"
                onClick={() =>
                  launch.mutate(
                    { externalToolResourceId, externalToolLinkId: link.id as string },
                    { onSuccess: (form) => setPending(form) },
                  )
                }
                disabled={launch.isPending}
              >
                {t('lms.tools.launch')}
              </Button>
            )}
          </li>
        ))}
        {links.data.items.length === 0 && <li className="hint">{t('common.state.empty')}</li>}
      </ul>
      {pending && <LaunchForm launch={pending} onDone={() => setPending(null)} />}
    </>
  );
}

/** Posts the signed launch form to the tool in a new tab; the signature expires quickly, so it posts at once. */
function LaunchForm({ launch, onDone }: { launch: LtiLaunch; onDone: () => void }) {
  const form = useRef<HTMLFormElement>(null);
  useEffect(() => {
    form.current?.submit();
    onDone();
  }, [onDone]);
  return (
    <form ref={form} action={launch.launchUrl ?? ''} method="post" target="_blank" hidden>
      {Object.entries(launch.formFields ?? {}).map(([name, value]) => (
        <input key={name} type="hidden" name={name} value={String(value)} />
      ))}
    </form>
  );
}
