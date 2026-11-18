import {
  percent,
  useCan,
  useCatalogHome,
  useCourseOutline,
  useCourseSyllabus,
  useCoursePage,
  useEnrollInCourse,
  useI18n,
  useLmsCategories,
  useLmsStatistics,
  usePublicLmsSettings,
  useSession,
  type LmsCourse,
} from '@mysms/core';
import { Banner, Button, Card, ProgressBar, SelectField, Skeleton, TextInput } from '@mysms/ui-web';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router';
import { ActionError, LmsPage, LmsState } from './lmsUi';

/** One course on a catalogue card: its title, introduction and how many have enrolled. */
function CourseCard({ course }: { course: LmsCourse }) {
  const { t, format } = useI18n();
  return (
    <Card title={course.title}>
      {course.shortIntroduction ? <p>{course.shortIntroduction}</p> : null}
      <p className="page__subtitle">
        {t('lms.catalog.enrolled', { count: format.number(course.enrollmentCount ?? 0) })}
      </p>
      <Link to={`/app/lms/courses/${course.id ?? ''}/catalog`}>{t('lms.catalog.open')}</Link>
    </Card>
  );
}

/**
 * LMS-01 Catalogue home: the school's listed courses — searchable by words and narrowed to a category, a page at a
 * time — its featured banners, categories, cohorts open for enrolment and public figures. Readable without a session
 * when the deployment names its school; the words, category and page live in the address.
 */
export function CatalogHomeScreen() {
  const { t, format } = useI18n();
  const [search, setSearch] = useSearchParams();
  const categoryId = search.get('category') ?? '';
  const query = search.get('q') ?? '';
  const page = Math.max(1, Number(search.get('page')) || 1);
  const home = useCatalogHome({ categoryId, query, page });
  const categories = useLmsCategories();
  const statistics = useLmsStatistics();
  const settings = usePublicLmsSettings();
  const title = t('screen.LMS01.title');
  const go = (changes: Record<string, string>) =>
    setSearch((current) => {
      const next = new URLSearchParams(current);
      for (const [key, value] of Object.entries(changes)) {
        if (value) next.set(key, value);
        else next.delete(key);
      }
      return next;
    });

  const state = LmsState({
    title,
    allowed: settings.data?.showCourses !== false,
    loading: home.isPending,
    error: home.error,
    retry: () => void home.refetch(),
  });
  if (state) return state;
  const data = home.data;
  const courses = data?.courses ?? [];
  return (
    <LmsPage
      title={title}
      subtitle={
        statistics.data
          ? t('lms.catalog.figures', {
              courses: format.number(statistics.data.publishedCourses),
              students: format.number(statistics.data.students),
            })
          : undefined
      }
    >
      {(data?.featured ?? []).length > 0 ? (
        <Card title={t('lms.catalog.featured')}>
          <ul>
            {(data?.featured ?? []).map((banner) => (
              <li key={banner.id}>
                <Link to={`/app/lms/courses/${banner.courseId ?? ''}/catalog`}>{t('lms.catalog.open')}</Link>
              </li>
            ))}
          </ul>
        </Card>
      ) : null}
      <Card>
        <div className="form">
          <TextInput
            label={t('lms.catalog.search')}
            defaultValue={query}
            onKeyDown={(event) => {
              if (event.key === 'Enter') go({ q: event.currentTarget.value, page: '' });
            }}
          />
          <SelectField
            label={t('lms.catalog.category')}
            value={categoryId}
            options={[
              { value: '', label: t('lms.catalog.allCategories') },
              ...(categories.data?.items ?? []).map((c) => ({ value: c.id ?? '', label: c.name })),
            ]}
            onChange={(event) => go({ category: event.target.value, page: '' })}
          />
        </div>
      </Card>
      {courses.length === 0 ? (
        <Banner intent="info">{t('lms.catalog.empty')}</Banner>
      ) : (
        <div className="lms-cards">
          {courses.map((course) => (
            <CourseCard key={course.id} course={course} />
          ))}
        </div>
      )}
      <div className="lms-toolbar">
        {page > 1 ? (
          <Button variant="secondary" onClick={() => go({ page: String(page - 1) })}>
            {t('lms.catalog.previous')}
          </Button>
        ) : null}
        {data?.paging?.next ? (
          <Button variant="secondary" onClick={() => go({ page: String(page + 1) })}>
            {t('lms.catalog.next')}
          </Button>
        ) : null}
      </div>
      {(data?.openCohorts ?? []).length > 0 ? (
        <Card title={t('lms.catalog.openCohorts')}>
          <ul>
            {(data?.openCohorts ?? []).map((cohort) => (
              <li key={cohort.id}>
                {t('lms.catalog.cohort', { title: cohort.title, from: format.day(cohort.startDate) })}
              </li>
            ))}
          </ul>
        </Card>
      ) : null}
      {settings.data?.contactUsEmail ? (
        <p className="page__subtitle">{t('lms.catalog.contact', { email: settings.data.contactUsEmail })}</p>
      ) : null}
    </LmsPage>
  );
}

/**
 * LMS-02 Course page: a published course as the public sees it — introduction, outline, previews, authors,
 * testimonials and reviews, and its syllabus for a signed-in reader — and, for a signed-in reader who may, enrolling (LMS-03's free case). A paid course or one
 * taught in cohorts says why it cannot be joined here.
 */
export function CoursePageScreen() {
  const { courseId = '' } = useParams();
  const { t } = useI18n();
  const navigate = useNavigate();
  const signedIn = useSession((s) => s.status === 'signedIn');
  const mayEnrol = useCan('lms.course.enroll_in_course');
  const page = useCoursePage(courseId);
  const outline = useCourseOutline(courseId);
  const enrol = useEnrollInCourse();
  const maySyllabus = useCan('lms.course.get_course_syllabus');
  const syllabus = useCourseSyllabus();
  const title = page.data?.course.title ?? t('screen.LMS02.title');
  const state = LmsState({
    title,
    allowed: true,
    loading: page.isPending,
    error: page.error,
    retry: () => void page.refetch(),
  });
  if (state) return state;
  const data = page.data;
  if (!data) return null;
  const enrolled = Boolean(outline.data?.enrollmentId);
  return (
    <LmsPage
      title={title}
      subtitle={data.course.shortIntroduction}
      actions={
        enrolled ? (
          <Link to={`/app/lms/courses/${courseId}`}>{t('lms.course.continue')}</Link>
        ) : signedIn && mayEnrol ? (
          <Button
            loading={enrol.isPending}
            onClick={() =>
              enrol.mutate(courseId, { onSuccess: () => void navigate(`/app/lms/courses/${courseId}`) })
            }
          >
            {t('lms.course.enrol')}
          </Button>
        ) : !signedIn ? (
          <Link to="/app/auth/login">{t('lms.course.signInToEnrol')}</Link>
        ) : null
      }
    >
      <ActionError error={enrol.error ?? syllabus.error} />
      {signedIn && maySyllabus ? (
        <Button
          variant="secondary"
          loading={syllabus.isPending}
          onClick={() =>
            syllabus.mutate(courseId, {
              onSuccess: (link) => void window.open(link.url, '_blank', 'noopener'),
            })
          }
        >
          {t('lms.course.syllabus')}
        </Button>
      ) : null}
      {data.course.isPaid ? <Banner intent="info">{t('lms.course.paid')}</Banner> : null}
      {data.course.description ? <Card>{data.course.description}</Card> : null}
      <Card title={t('lms.course.outline')}>
        {outline.isPending ? (
          <Skeleton label={t('common.state.loading')} lines={3} />
        ) : (
          <ol>
            {(outline.data?.chapters ?? []).map((chapter) => (
              <li key={chapter.chapterId}>
                {chapter.title}
                <ul>
                  {chapter.lessons.map((lesson) => (
                    <li key={lesson.lessonId}>
                      {lesson.includeInPreview ? (
                        <Link to={`/app/lms/lessons/${lesson.lessonId}`}>{lesson.title}</Link>
                      ) : (
                        lesson.title
                      )}
                    </li>
                  ))}
                </ul>
              </li>
            ))}
          </ol>
        )}
        {enrolled ? (
          <ProgressBar label={t('lms.course.progress')} value={percent(outline.data?.progressPercent)} />
        ) : null}
      </Card>
      {data.authors.length > 0 ? (
        <Card title={t('lms.course.authors')}>
          <ul>
            {data.authors.map((author) => (
              <li key={author.id}>
                {author.name}
                {author.qualification ? ` · ${author.qualification}` : ''}
              </li>
            ))}
          </ul>
        </Card>
      ) : null}
      {data.testimonials.length > 0 ? (
        <Card title={t('lms.course.testimonials')}>
          {data.testimonials.map((testimonial) => (
            <blockquote key={testimonial.id}>{testimonial.quote}</blockquote>
          ))}
        </Card>
      ) : null}
      <Card title={t('lms.course.reviews')}>
        {data.reviews.length === 0 ? (
          <p>{t('lms.course.noReviews')}</p>
        ) : (
          <ul>
            {data.reviews.map((review) => (
              <li key={review.id}>
                {t('lms.course.rating', { rating: review.rating })} {review.review ?? ''}
              </li>
            ))}
          </ul>
        )}
      </Card>
    </LmsPage>
  );
}
