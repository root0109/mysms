import { useI18n } from '@mysms/core';
import { RelatedRecords } from '../RelatedRecords';

/**
 * ACA-04 Grade levels & sections: the two catalogues a class section is made of (a class is a grade and a section),
 * side by side on one page, each with its own create, edit and delete.
 */
export function GradeLevelsScreen() {
  const { t } = useI18n();
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.ACA04.title')}</h1>
        <p className="page__subtitle">{t('aca.grades.intro')}</p>
      </header>
      <div className="page__grid">
        <RelatedRecords
          resource="academics.GradeLevel"
          title={t('aca.grades.levels')}
          columns={['name', 'slug', 'sortOrder', 'isActive']}
        />
        <RelatedRecords
          resource="academics.Section"
          title={t('aca.grades.sections')}
          columns={['name', 'code', 'sortOrder', 'isActive']}
        />
      </div>
    </div>
  );
}
