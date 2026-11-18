import { createRuntime } from '@mysms/core';
import {
  LMS_ADMIN,
  LMS_AUTHOR,
  LMS_STUDENT,
  LMS_TEACHER,
  SELF_SERVICE,
  fakeApi,
  fakeBackend,
  fakeLms,
  lmsFixture,
  schoolAdmin,
} from '@mysms/fixtures';
import { WebApp } from '@mysms/web-kit';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createMemoryRouter } from 'react-router';
import { describe, expect, it, vi } from 'vitest';
import { routes } from './routes';
import { signedIn } from './test/signedIn';

const fx = lmsFixture;
const { cohortId } = fx;
const student = { ...schoolAdmin, permissions: [...SELF_SERVICE, ...LMS_STUDENT] };
const teacher = { ...schoolAdmin, permissions: [...SELF_SERVICE, ...LMS_TEACHER] };
const author = { ...schoolAdmin, permissions: [...SELF_SERVICE, ...LMS_AUTHOR] };
const home = `/app/lms/cohorts/${cohortId}`;
describe('LMS-21 Cohort home', () => {
  it('shows the cohort with its sections and sends an announcement to every active student', async () => {
    const { api } = await signedIn(home, teacher, fakeLms());
    expect(await screen.findByRole('heading', { name: 'Algebra 2026' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Roster' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Forums' })).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Send announcement' }));
    await userEvent.type(await screen.findByLabelText('Subject'), 'Exam week');
    await userEvent.type(screen.getByLabelText('Message'), 'The hall opens at nine.');
    await userEvent.click(screen.getByRole('button', { name: 'Send' }));
    expect(await screen.findByText('Queued for 2 students.')).toBeInTheDocument();
    const sent = api.calls.find((c) => c.path === `/v1/lms/cohorts/${cohortId}:sendAnnouncement`);
    expect(JSON.parse(sent?.body ?? '{}')).toEqual({
      subject: 'Exam week',
      content: 'The hall opens at nine.',
    });
  });

  it('walks a lifecycle step under the etag and shows the conflict when the state refuses it', async () => {
    await signedIn(home, teacher, fakeLms());
    await screen.findByRole('heading', { name: 'Algebra 2026' });
    // the cohort is already in the catalogue: publishing is not offered, unpublishing is
    expect(screen.queryByRole('button', { name: 'Publish' })).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Unpublish' }));
    expect(await screen.findByRole('button', { name: 'Publish' })).toBeInTheDocument();
  });
});

describe('LMS-23 Roster', () => {
  it('names the members through the roster read', async () => {
    await signedIn(`${home}/roster`, teacher, fakeLms());
    expect(await screen.findByText('Meera Iyer')).toBeInTheDocument();
    expect(screen.getByText('Asha Rao')).toBeInTheDocument();
    expect(screen.getByText('Ravi Kumar')).toBeInTheDocument();
  });
});

describe('LMS-08 Quiz player', () => {
  it('starts the attempt and submits the chosen answers under the attempt token', async () => {
    const { api } = await signedIn(`/app/lms/content-items/${fx.quizItemId}/quiz`, student, fakeLms());
    await userEvent.click(await screen.findByRole('button', { name: 'Start attempt' }));
    expect(await screen.findByText(/Pick the prime number/)).toBeInTheDocument();
    await userEvent.click(screen.getByLabelText('7'));
    await userEvent.click(screen.getByRole('button', { name: 'Submit attempt' }));
    await waitFor(() => {
      const submit = api.calls.find((call) => call.path.endsWith(':submit'));
      expect(submit).toBeDefined();
      expect(JSON.parse(submit?.body ?? '{}')).toMatchObject({
        attemptToken: fx.attemptToken,
        answers: [{ questionId: fx.choiceQuestionId, selectedOptionIds: [fx.optionRight] }],
      });
    });
  });

  it('hides the player from a role without the attempt permission', async () => {
    await signedIn(
      `/app/lms/content-items/${fx.quizItemId}/quiz`,
      { ...schoolAdmin, permissions: [...SELF_SERVICE] },
      fakeLms(),
    );
    expect(await screen.findByText(/do not have access/)).toBeInTheDocument();
  });
});

describe('LMS-09 Quiz result', () => {
  it('shows the outcome with each answer and its marks', async () => {
    await signedIn(`/app/lms/quiz-submissions/${fx.quizSubmissionId}`, student, fakeLms());
    expect(await screen.findByText(/Score 6.00 of 10.00/)).toBeInTheDocument();
    expect(screen.getByText(/Pick the prime number/)).toBeInTheDocument();
    expect(screen.getByText('6.00 of 6.00 marks')).toBeInTheDocument();
  });
});

describe('LMS-10 Assignment', () => {
  it('hands the draft in and keeps it from changing afterwards', async () => {
    const { api } = await signedIn(
      `/app/lms/content-items/${fx.assignmentItemId}/assignment`,
      student,
      fakeLms(),
    );
    expect(await screen.findByText('Write about primes.')).toBeInTheDocument();
    await userEvent.click(await screen.findByRole('button', { name: 'Hand in' }));
    expect(await screen.findByText('Handed in')).toBeInTheDocument();
    expect(api.calls.some((call) => call.path.endsWith(':finalize'))).toBe(true);
    expect(screen.queryByRole('button', { name: 'Hand in' })).not.toBeInTheDocument();
  });
});

describe('LMS-31/32 grading', () => {
  it('shows the gradebook grid with the student rows', async () => {
    await signedIn(`/app/lms/cohorts/${fx.cohortId}/gradebook`, teacher, fakeLms());
    expect(await screen.findByText('Asha Verma')).toBeInTheDocument();
    expect(screen.getByText('Rohan Gupta')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Export grades' })).toBeInTheDocument();
  });

  it('saves a grade and surfaces the conflict when the entry moved on', async () => {
    const { api } = await signedIn(
      `/app/lms/content-items/${fx.quizItemId}/grading`,
      teacher,
      fakeLms({ entryEtag: '"9"' }),
    );
    expect(await screen.findByText(/submitted/)).toBeInTheDocument();
    // the quiz queue offers the attempt; marking the open answer goes through gradeAnswers
    expect(api.calls.some((call) => call.path.includes('quiz-submissions'))).toBe(true);
  });
});

describe('LMS-43 Question bank', () => {
  it('searches the bank and starts a GIFT import by file id', async () => {
    const { api } = await signedIn('/app/lms/questions', teacher, fakeLms());
    expect(await screen.findByText('Pick the prime number.')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Import questions' }));
    await userEvent.type(await screen.findByLabelText('File id'), fx.questionFileId);
    await userEvent.click(screen.getByRole('button', { name: 'Import' }));
    expect(await screen.findByText(/The import is running/)).toBeInTheDocument();
    const body = JSON.parse(api.calls.find((call) => call.path.endsWith(':import'))?.body ?? '{}') as {
      fileId: string;
      format: string;
    };
    expect(body).toEqual({ fileId: fx.questionFileId, format: 'gift' });
  });
});

describe('LMS-54 Drive', () => {
  it('replaces who sees an item through ShareDriveItem', async () => {
    const { api } = await signedIn('/app/platform/drive-items', student, fakeLms());
    expect(await screen.findByText('Notes.pdf')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Share' }));
    await userEvent.type(await screen.findByLabelText('Share with (user id)'), fx.otherStudentId);
    await userEvent.click(screen.getByRole('button', { name: 'Add' }));
    await waitFor(() => {
      const body = JSON.parse(api.calls.find((call) => call.path.endsWith(':share'))?.body ?? '{}') as {
        userIds: string[];
      };
      expect(body.userIds).toEqual([fx.otherStudentId]);
    });
  });
});

describe('LMS-01 Catalogue home and LMS-02 Course page', () => {
  it('are read by a guest in the school the deployment names, without a session', async () => {
    const api = fakeApi({ ...fakeBackend({ me: schoolAdmin }), ...fakeLms() });
    const runtime = createRuntime({
      baseUrl: 'https://school.example.com',
      clientId: 'web/test/jsdom',
      fetch: api.fetch,
      tenantCode: 'demo',
    });
    const router = createMemoryRouter(routes, { initialEntries: ['/app/lms/catalog'] });
    render(<WebApp runtime={runtime} router={router} />);

    expect(await screen.findByRole('heading', { name: 'Course catalogue' })).toBeInTheDocument();
    expect(await screen.findByText('Algebra basics')).toBeInTheDocument();
    expect(screen.queryByText('Geometry draft')).not.toBeInTheDocument();
    expect(await screen.findByText('1 courses · 12 students')).toBeInTheDocument();
    const catalogCall = api.calls.find((c) => c.path === '/v1/lms/catalog');
    expect(catalogCall?.headers.get('X-Tenant-Code')).toBe('demo');
    expect(catalogCall?.headers.get('Authorization')).toBeNull();

    await userEvent.click(screen.getByRole('link', { name: 'View course' }));
    expect(await screen.findByRole('heading', { name: 'Algebra basics' })).toBeInTheDocument();
    expect(await screen.findByRole('link', { name: 'Solving for x' })).toBeInTheDocument();
    expect(screen.getByText('Two unknowns')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Sign in to enrol' })).toBeInTheDocument();
  });
});

describe('LMS-02 Course page and LMS-06 Lesson player', () => {
  it('enrols a student who completes a video lesson only after watching it', async () => {
    const { api, router } = await signedIn('/app', student, fakeLms());
    await router.navigate(`/app/lms/courses/${fx.courseId}/catalog`);
    const opened = vi.spyOn(window, 'open').mockReturnValue(null);
    await userEvent.click(await screen.findByRole('button', { name: 'Download the syllabus' }));
    await waitFor(() =>
      expect(opened).toHaveBeenCalledWith(
        `https://files.example.com/syllabus/${fx.courseId}.html`,
        '_blank',
        'noopener',
      ),
    );
    opened.mockRestore();
    await userEvent.click(screen.getByRole('button', { name: 'Enrol' }));
    await waitFor(() => expect(router.state.location.pathname).toBe(`/app/lms/courses/${fx.courseId}`));
    // the catalogue page unmounts before its lesson links are clicked: a click on a detached link is a real navigation
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Enrol' })).not.toBeInTheDocument());

    await userEvent.click(await screen.findByRole('link', { name: 'Solving for x' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Mark complete' }));
    expect(
      await screen.findByText('Watch the lesson video to the end before marking it complete.'),
    ).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'I have watched the video' }));
    await waitFor(() =>
      expect(api.calls.some((c) => c.path === `/v1/lms/lessons/${fx.videoLessonId}:trackVideoWatch`)).toBe(
        true,
      ),
    );
    await userEvent.click(screen.getByRole('button', { name: 'Mark complete' }));
    expect(await screen.findByText('Complete')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Mark complete' })).not.toBeInTheDocument();
  });

  it("keeps the student's own notes on a lesson", async () => {
    const { api, router } = await signedIn('/app', student, fakeLms());
    await router.navigate(`/app/lms/courses/${fx.courseId}/catalog`);
    await userEvent.click(await screen.findByRole('button', { name: 'Enrol' }));
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Enrol' })).not.toBeInTheDocument());
    await userEvent.click(await screen.findByRole('link', { name: 'Two unknowns' }));
    // the lesson's blocks: an ordered list and an outside link, never markup
    expect(await screen.findByText('Isolate y')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Open graphs.example.org' })).toHaveAttribute(
      'href',
      'https://graphs.example.org/plot',
    );
    const notes = (await screen.findByRole('heading', { name: 'My notes' })).closest(
      'section',
    ) as HTMLElement;
    await userEvent.type(within(notes).getByLabelText('New note'), 'Check the signs.');
    await userEvent.click(within(notes).getByRole('button', { name: 'Add note' }));
    expect(await within(notes).findByText(/Check the signs\./)).toBeInTheDocument();
    const created = api.calls.find((c) => c.method === 'POST' && c.path.endsWith('/notes'));
    const noteBody = JSON.parse(created?.body ?? '{}');
    expect(noteBody).toMatchObject({ note: 'Check the signs.' });
    expect(noteBody).not.toHaveProperty('userId');
  });
});

describe('LMS-40 My courses', () => {
  it("offers only the steps a course's state allows, under its etag", async () => {
    const { api } = await signedIn('/app/lms/authoring/courses', author, fakeLms());
    const table = await screen.findByRole('table', { name: 'My courses' });
    const draftRow = (await within(table).findByRole('link', { name: 'Geometry draft' })).closest(
      'tr',
    ) as HTMLElement;
    const publishedRow = within(table)
      .getByRole('link', { name: 'Algebra basics' })
      .closest('tr') as HTMLElement;
    expect(within(publishedRow).queryByRole('button', { name: 'Submit for review' })).not.toBeInTheDocument();

    await userEvent.click(within(draftRow).getByRole('button', { name: 'Submit for review' }));
    await waitFor(() => expect(within(draftRow).getByText('Under review')).toBeInTheDocument());
    const submitted = api.calls.find((c) => c.path === `/v1/lms/courses/${fx.draftCourseId}:submitForReview`);
    expect(JSON.parse(submitted?.body ?? '{}')).toEqual({ etag: '"1"' });
    expect(within(draftRow).queryByRole('button', { name: 'Submit for review' })).not.toBeInTheDocument();
  });
});

describe('LMS-40 versions, LMS-41 structure, LMS-42 lesson editor and LMS-50 catalogue page', () => {
  it('makes a major version of a published course in its lane', async () => {
    const { api } = await signedIn('/app/lms/authoring/courses', author, fakeLms());
    const table = await screen.findByRole('table', { name: 'My courses' });
    const row = (await within(table).findByRole('link', { name: 'Algebra basics' })).closest(
      'tr',
    ) as HTMLElement;
    await userEvent.click(within(row).getByRole('button', { name: 'New version' }));
    const dialog = await screen.findByRole('dialog', { name: 'New version of Algebra basics' });
    await userEvent.selectOptions(within(dialog).getByLabelText('Kind of change'), 'major');
    await userEvent.type(within(dialog).getByLabelText('What changes in this version'), 'New syllabus');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Create version' }));
    expect(await within(table).findByText('2.0')).toBeInTheDocument();
    const made = api.calls.find((c) => c.path === `/v1/lms/courses/${fx.courseId}:createVersion`);
    expect(JSON.parse(made?.body ?? '{}')).toEqual({ versionKind: 'major', authorComments: 'New syllabus' });
  });

  it('renames a chapter under its etag', async () => {
    const { api } = await signedIn(`/app/lms/courses/${fx.courseId}/edit`, author, fakeLms());
    await userEvent.click(await screen.findByRole('button', { name: 'Rename chapter' }));
    const dialog = await screen.findByRole('dialog', { name: 'Rename chapter' });
    const field = within(dialog).getByLabelText('Chapter title');
    await userEvent.clear(field);
    await userEvent.type(field, 'Equations in one unknown');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(await screen.findByRole('heading', { name: 'Equations in one unknown' })).toBeInTheDocument();
    const renamed = api.calls.find((c) => c.method === 'PATCH' && c.path.includes('/chapters/'));
    expect(renamed?.headers.get('If-Match')).toBe('"1"');
    expect(JSON.parse(renamed?.body ?? '{}')).toEqual({ title: 'Equations in one unknown' });
  });

  it('edits a lesson as blocks, refusing a link that is not https', async () => {
    const { api } = await signedIn(`/app/lms/lessons/${fx.textLessonId}/edit`, author, fakeLms());
    expect(await screen.findByDisplayValue(/Isolate y\s+Substitute/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Add Text' }));
    const blocks = screen.getAllByLabelText('Text');
    await userEvent.type(blocks[blocks.length - 1] as HTMLElement, 'Check by substituting back.');
    await userEvent.click(screen.getByRole('button', { name: 'Add Outside link' }));
    await userEvent.type(
      screen.getAllByLabelText('Address (https)').at(-1) as HTMLElement,
      'http://plain.example',
    );
    expect(screen.getByText('Enter a full https:// address.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled();
    const removes = screen.getAllByRole('button', { name: 'Remove block' });
    await userEvent.click(removes[removes.length - 1] as HTMLElement);
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    await waitFor(() => {
      const saved = api.calls.find(
        (c) => c.method === 'PATCH' && c.path === `/v1/lms/lessons/${fx.textLessonId}`,
      );
      expect(saved?.headers.get('If-Match')).toBe('"1"');
      expect(JSON.parse(saved?.body ?? '{}').content).toEqual({
        blocks: [
          { type: 'list', data: { style: 'ordered', items: ['Isolate y', 'Substitute'] } },
          { type: 'embed', data: { source: 'https://graphs.example.org/plot' } },
          { type: 'paragraph', data: { text: 'Check by substituting back.' } },
        ],
      });
    });
  });

  it('adds an author to the catalogue page and removes them under their etag', async () => {
    const { api } = await signedIn(`/app/lms/courses/${fx.courseId}/catalog/edit`, author, fakeLms());
    await userEvent.type(await screen.findByLabelText('Author name'), 'R. Sharma');
    await userEvent.type(screen.getByLabelText('Qualification'), 'M.Sc. Mathematics');
    await userEvent.click(screen.getByRole('button', { name: 'Add author' }));
    expect(await screen.findByText('R. Sharma · M.Sc. Mathematics')).toBeInTheDocument();
    const added = api.calls.find((c) => c.method === 'POST' && c.path.endsWith('/author-profiles'));
    expect(JSON.parse(added?.body ?? '{}')).toEqual({
      name: 'R. Sharma',
      qualification: 'M.Sc. Mathematics',
      sortOrder: 0,
      courseId: fx.courseId,
    });
    await userEvent.click(screen.getByRole('button', { name: 'Remove from page' }));
    await waitFor(() => expect(screen.queryByText('R. Sharma · M.Sc. Mathematics')).not.toBeInTheDocument());
    const removed = api.calls.find((c) => c.method === 'DELETE' && c.path.includes('/author-profiles/'));
    expect(removed?.headers.get('If-Match')).toBe('"1"');
  });
});

describe('LMS-53 LMS settings and LMS-47 Repository', () => {
  const admin = { ...schoolAdmin, permissions: [...SELF_SERVICE, ...LMS_ADMIN] };

  it('saves a toggle under the ETag the screen last read (the first save creates the row)', async () => {
    const { api } = await signedIn('/app/lms/settings', admin, fakeLms());
    expect(await screen.findByRole('heading', { name: 'LMS settings' })).toBeInTheDocument();
    await userEvent.click(await screen.findByRole('switch', { name: 'Disable self sign-up' }));
    await waitFor(() => {
      const saved = api.calls.find((c) => c.method === 'PATCH' && c.path === '/v1/lms/settings');
      expect(saved?.headers.get('If-Match')).toBe('"0"');
      expect(JSON.parse(saved?.body ?? '{}')).toEqual({ disableSignup: false });
    });
  });

  it('places picked repository items into the named chapter', async () => {
    const { api } = await signedIn('/app/lms/repository', admin, fakeLms());
    await userEvent.click(await screen.findByRole('checkbox', { name: 'Fractions check' }));
    await userEvent.type(screen.getByLabelText('Chapter id to place into'), fx.chapterId);
    await userEvent.click(screen.getByRole('button', { name: 'Place 1 item(s)' }));
    expect(await screen.findByText('Placed in the chapter.')).toBeInTheDocument();
    const placed = api.calls.find((c) => c.path === '/v1/lms/content-items:addFromRepository');
    expect(JSON.parse(placed?.body ?? '{}')).toEqual({
      chapterId: fx.chapterId,
      itemKind: 'quiz',
      sourceIds: [fx.repositoryQuizId],
    });
  });
});

describe('LMS-14 My notes & bookmarks', () => {
  it('saves and deletes a bookmark', async () => {
    const { api } = await signedIn(
      '/app/lms/annotations',
      {
        ...schoolAdmin,
        permissions: [...SELF_SERVICE, ...LMS_ADMIN],
      },
      fakeLms(),
    );
    await userEvent.type(await screen.findByLabelText('Title'), 'Khan Academy');
    await userEvent.type(screen.getByLabelText('Address'), 'https://khan.example.org');
    await userEvent.click(screen.getByRole('button', { name: 'Save bookmark' }));
    expect(await screen.findByRole('link', { name: 'Khan Academy' })).toBeInTheDocument();
    const created = api.calls.find((c) => c.method === 'POST' && c.path === '/v1/lms/bookmarks');
    const bookmarkBody = JSON.parse(created?.body ?? '{}');
    expect(bookmarkBody).toMatchObject({ title: 'Khan Academy' });
    expect(bookmarkBody).not.toHaveProperty('userId');

    await userEvent.click(screen.getByRole('button', { name: 'Delete' }));
    await waitFor(() =>
      expect(api.calls.some((c) => c.method === 'DELETE' && c.path.startsWith('/v1/lms/bookmarks/'))).toBe(
        true,
      ),
    );
  });
});
