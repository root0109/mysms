import { matchRoute, nextPage, useCan, useGlobalSearch, useI18n, type SearchHit } from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Banner, Button, Card, TextInput } from '@mysms/ui-web';
import { useState } from 'react';
import { Link, useSearchParams } from 'react-router';
import { problemText } from './problems';

const TYPE_LABELS: Readonly<Record<string, MessageKey>> = {
  'platform.file_object': 'plt.search.type.file',
  'platform.tag': 'plt.search.type.tag',
};

/** A hit opens the web screen of its record when the web app has one; otherwise it is shown without a link. */
function HitTitle({ hit }: { hit: SearchHit }) {
  const path = hit.urlPath && matchRoute(hit.urlPath, 'W') ? hit.urlPath : null;
  return path ? <Link to={`/app${path}`}>{hit.title}</Link> : <span>{hit.title}</span>;
}

/**
 * ACC-11 Global search: records the caller may see whose title contains the text (platform.GlobalSearch), page by
 * page, with the query kept in the address so a search can be shared. The platform searches what it owns — files
 * and tags; people, courses and other modules' records are not searchable until those modules publish a search
 * feed (reported as a known limit).
 */
export function GlobalSearchScreen() {
  const { t } = useI18n();
  const maySearch = useCan('platform.tenant.global_search');
  const [params, setParams] = useSearchParams();
  const [text, setText] = useState(params.get('q') ?? '');
  const [page, setPage] = useState(1);
  const query = params.get('q') ?? '';
  const results = useGlobalSearch(query, page);
  const hasNext = nextPage(results.data?.paging) !== undefined;
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.ACC11.title')}</h1>
        <p className="page__subtitle">{t('plt.search.intro')}</p>
      </header>
      {!maySearch ? (
        <Banner intent="info">{t('plt.search.noAccess')}</Banner>
      ) : (
        <>
          <form
            role="search"
            noValidate
            onSubmit={(event) => {
              event.preventDefault();
              setPage(1);
              setParams(text.trim() ? { q: text.trim() } : {});
            }}
          >
            <TextInput
              label={t('plt.search.query')}
              hint={t('plt.search.queryHint')}
              value={text}
              maxLength={100}
              onChange={(event) => setText(event.target.value)}
            />
            <Button type="submit" disabled={text.trim().length < 2}>
              {t('plt.search.submit')}
            </Button>
          </form>
          {query.length < 2 ? null : results.isError ? (
            <Banner intent="danger">{problemText(t, results.error)}</Banner>
          ) : results.isPending ? (
            <p className="hint">{t('common.state.loading')}</p>
          ) : (
            <Card title={t('plt.search.results', { query })}>
              {results.data.data.length === 0 ? (
                <p className="hint">{t('plt.search.none')}</p>
              ) : (
                <ul className="result-list">
                  {results.data.data.map((hit) => {
                    const typeLabel = TYPE_LABELS[hit.entityType];
                    return (
                      <li key={`${hit.entityType}-${hit.entityId}`}>
                        <HitTitle hit={hit} />
                        <span className="hint">
                          {' '}
                          {typeLabel ? t(typeLabel) : hit.entityType}
                          {hit.subtitle ? ` · ${hit.subtitle}` : ''}
                        </span>
                      </li>
                    );
                  })}
                </ul>
              )}
              <div className="form-actions">
                <Button variant="secondary" disabled={page === 1} onClick={() => setPage(page - 1)}>
                  {t('plt.audit.previous')}
                </Button>
                <Button variant="secondary" disabled={!hasNext} onClick={() => setPage(page + 1)}>
                  {t('plt.audit.next')}
                </Button>
              </div>
            </Card>
          )}
        </>
      )}
    </div>
  );
}
