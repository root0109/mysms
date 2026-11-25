import { useCan, useI18n, useSubjectNames } from '@mysms/core';
import { ListWithDetail } from '../ListWithDetail';
import { RecordDetailPage } from '../RecordDetailPage';
import { RelatedRecords } from '../RelatedRecords';

/**
 * ACA-02 Courses & subjects (HE): the courses (list manager); a course opens in place (`?course=`) with its fields
 * and its subjects — which subject, in which term, for how many credits and marks (academics course-subject rows,
 * created, edited and removed under the course).
 */
export function CoursesScreen() {
  const { t } = useI18n();
  const subjectNames = useSubjectNames(useCan('academics.subject.read'));
  return (
    <ListWithDetail
      screenId="ACA-02"
      param="course"
      detail={(courseId) => (
        <RecordDetailPage
          key={courseId}
          resource="academics.Course"
          id={courseId}
          titleField="name"
          back="/app/academics/courses"
          tabs={[
            {
              key: 'subjects',
              label: t('aca.courses.subjects'),
              render: (course) => (
                <RelatedRecords
                  resource="academics.CourseSubject"
                  title={t('aca.courses.subjectsOf', { course: String(course.name ?? '') })}
                  parentId={course.id}
                  names={{ subjectId: subjectNames }}
                  columns={['subjectId', 'termNumber', 'credits', 'maxMarks', 'subjectCategory']}
                />
              ),
            },
          ]}
        />
      )}
    />
  );
}
