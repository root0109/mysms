import { isApiError, useCan, useI18n, useRubricBundle, type RubricCell } from '@mysms/core';
import { Banner, Button, Card, Skeleton, StatusChip } from '@mysms/ui-web';
import { useParams } from 'react-router';
import { UUID } from '../ids';

/**
 * LMS-46 Rubric builder: the rubric's grid — criteria down, levels across, each cell's wording — read whole. The
 * rows and cells themselves are standard records; their editors live on the generic record forms, and this grid is
 * where the whole is inspected.
 */
export function RubricBuilderScreen() {
  const { t } = useI18n();
  const { rubricId: param = '' } = useParams();
  const mayRead = useCan('lms.rubric.read');
  const rubricId = mayRead && UUID.test(param) ? param : null;
  const bundle = useRubricBundle(rubricId);
  const title = t('screen.LMS46.title');
  if (!mayRead)
    return (
      <Card title={title}>
        <p className="hint">{t('lms.common.forbidden')}</p>
      </Card>
    );
  if (rubricId === null || (isApiError(bundle.error) && bundle.error.isNotAvailable))
    return (
      <Card title={title}>
        <p>{t('common.state.notAvailable')}</p>
      </Card>
    );
  if (bundle.isPending) return <Skeleton label={t('common.state.loading')} lines={8} />;
  if (bundle.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void bundle.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    );
  const { rubric, criteria, levels, cells } = bundle.data;
  const cellOf = (criterionId: string | undefined, levelId: string | undefined): RubricCell | undefined =>
    cells.find((cell) => cell.rubricCriterionId === criterionId && cell.rubricLevelId === levelId);
  return (
    <Card title={rubric.data.title ?? title}>
      {rubric.data.publishedAt !== undefined && rubric.data.publishedAt !== null && (
        <StatusChip intent="info" label={t('lms.builder.published')} />
      )}
      {criteria.length === 0 ? (
        <p className="hint">{t('lms.rubric.empty')}</p>
      ) : (
        <table>
          <caption>{rubric.data.title ?? title}</caption>
          <thead>
            <tr>
              <th scope="col">{t('lms.rubric.criterion')}</th>
              {levels.map((level) => (
                <th scope="col" key={level.id}>
                  {level.label}
                  {level.scorePercent !== undefined && level.scorePercent !== null && (
                    <span className="hint"> {level.scorePercent}%</span>
                  )}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {criteria.map((criterion) => (
              <tr key={criterion.id}>
                <th scope="row">
                  {criterion.name}
                  {criterion.weightPercent !== undefined && criterion.weightPercent !== null && (
                    <span className="hint"> {criterion.weightPercent}%</span>
                  )}
                </th>
                {levels.map((level) => (
                  <td key={level.id}>{cellOf(criterion.id, level.id)?.description ?? ''}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </Card>
  );
}
