import { useActivateAcademicYear, useAllRecords, useCan, useGenerateTerms, useI18n } from '@mysms/core';
import { Banner, Button, Card, SelectField } from '@mysms/ui-web';
import { useSearchParams } from 'react-router';
import { ResourceScreen } from '../ResourceScreen';
import { RESOURCE_SCREENS } from '../resourceScreens';
import { problemText } from '../recordText';
import { RelatedRecords } from '../RelatedRecords';

/**
 * ADM-05 Academic years & terms: the years (list manager: create, edit, close, delete) and, for the year chosen
 * above them (`?year=` in the address), its steps — make it the current year (org.ActivateAcademicYear) and write
 * its terms from its term structure (org.GenerateTerms) — and its terms, edited one by one. Each step shows only
 * with its permission; activating is offered while the year is planned.
 */
export function AcademicYearsScreen() {
  const { t, format } = useI18n();
  const [search, setSearch] = useSearchParams();
  const mayRead = useCan('org.academic_year.read');
  const mayActivate = useCan('org.academic_year.activate_academic_year');
  const mayGenerate = useCan('org.academic_year.generate_terms');
  const years = useAllRecords('org.AcademicYear', {}, mayRead);
  const activate = useActivateAcademicYear();
  const generate = useGenerateTerms();
  const asked = search.get('year') ?? '';
  const year = years.data?.items.find((row) => row.id === asked);
  const screen = RESOURCE_SCREENS.find((each) => each.id === 'ADM-05');
  const failure = activate.error ?? generate.error;
  return (
    <div className="page">
      {screen ? <ResourceScreen screen={screen} /> : null}
      {mayRead ? (
        <Card title={t('org.years.terms')}>
          <SelectField
            label={t('org.years.pick')}
            value={year?.id ?? ''}
            options={[
              { value: '', label: t('org.years.choose') },
              ...(years.data?.items ?? []).map((row) => ({
                value: row.id,
                label: `${String(row.name)} (${format.day(String(row.startDate))} – ${format.day(String(row.endDate))})`,
              })),
            ]}
            onChange={(event) => {
              activate.reset();
              generate.reset();
              const chosen = event.target.value;
              setSearch(
                (previous) => {
                  const next = new URLSearchParams(previous);
                  if (chosen) next.set('year', chosen);
                  else next.delete('year');
                  return next;
                },
                { replace: true },
              );
            }}
          />
          {failure ? <Banner intent="danger">{problemText(t, failure)}</Banner> : null}
          {activate.isSuccess ? <Banner intent="success">{t('org.years.activated')}</Banner> : null}
          {generate.isSuccess ? <Banner intent="success">{t('org.years.generated')}</Banner> : null}
          {year ? (
            <div className="page__actions">
              {mayActivate && year.status === 'planned' ? (
                <Button
                  loading={activate.isPending}
                  onClick={() =>
                    activate.mutate({
                      academicYearId: year.id,
                      etag: (year.etag as string | undefined) ?? null,
                    })
                  }
                >
                  {t('org.years.activate')}
                </Button>
              ) : null}
              {mayGenerate ? (
                <Button
                  variant="secondary"
                  loading={generate.isPending}
                  onClick={() => generate.mutate({ academicYearId: year.id })}
                >
                  {t('org.years.generate')}
                </Button>
              ) : null}
            </div>
          ) : null}
        </Card>
      ) : null}
      {year ? (
        <RelatedRecords
          key={year.id}
          resource="org.AcademicTerm"
          title={t('org.years.termsOf', { year: String(year.name) })}
          parentId={year.id}
          columns={['name', 'startDate', 'endDate', 'sequence']}
        />
      ) : null}
    </div>
  );
}
