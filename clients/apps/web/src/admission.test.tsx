import {
  ADMISSION_APPLICANT,
  ADMISSION_OFFICE,
  SELF_SERVICE,
  admissionFixture,
  fakeAdmission,
  schoolAdmin,
} from '@mysms/fixtures';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { signedIn } from './test/signedIn';

const f = admissionFixture;
const office = { ...schoolAdmin, permissions: [...SELF_SERVICE, ...ADMISSION_OFFICE] };
const applicant = { ...schoolAdmin, permissions: [...SELF_SERVICE, ...ADMISSION_APPLICANT] };
const nobody = { ...schoolAdmin, permissions: [...SELF_SERVICE] };
const detail = (id: string) => `/app/admission/applications/${id}`;
const card = (name: string) => screen.getByRole('heading', { name }).closest('section') as HTMLElement;

describe('ADMN-01 Admission cycles', () => {
  it('heads the cycles with their timeline and shows where each cycle stands, with its form while open', async () => {
    await signedIn('/app/admission/admission-cycles', office, fakeAdmission());
    const timeline = await screen.findByRole('region', { name: 'How an admission cycle runs' });
    expect(
      within(timeline)
        .getAllByRole('listitem')
        .map((stage) => stage.firstChild?.textContent),
    ).toEqual(['Draft', 'Confirmed', 'Open for applications', 'Admission', 'Closed']);
    const open = (await screen.findByText('Grade 1, 2026–27')).closest('tr') as HTMLElement;
    expect(within(open).getByText('Open for applications').closest('li')).toHaveAttribute(
      'aria-current',
      'step',
    );
    expect(within(open).getByRole('link', { name: 'Application form' })).toHaveAttribute(
      'href',
      `/app/admission/apply/${f.openCycleId}`,
    );
    const draft = screen.getByText('Grade 6, 2026–27').closest('tr') as HTMLElement;
    expect(within(draft).queryByRole('link', { name: 'Application form' })).not.toBeInTheDocument();
  });
});

describe('ADMN-02 Applications pipeline', () => {
  it('shows the applications by status and as a table, for one cycle', async () => {
    const { router } = await signedIn('/app/admission/applications', office, fakeAdmission());
    const submitted = await screen.findByRole('region', { name: 'Handed in' });
    expect(within(submitted).getByRole('link', { name: 'Asha Rao' })).toHaveAttribute(
      'href',
      detail(f.submittedId),
    );
    expect(
      within(screen.getByRole('region', { name: 'Admitted' })).getByText('Diya Shah'),
    ).toBeInTheDocument();
    expect(within(screen.getByRole('region', { name: 'Enrolled' })).getByText('None')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('radio', { name: 'Table' }));
    const table = await screen.findByRole('table', { name: 'Applications pipeline' });
    expect(within(table).getAllByRole('row')).toHaveLength(6);
    expect(router.state.location.search).toContain('view=table');
    await userEvent.selectOptions(screen.getByLabelText('Admission cycle'), 'Grade 6, 2026–27');
    expect(await screen.findByText('No applications yet.')).toBeInTheDocument();
  });

  it('tells a role without access so', async () => {
    await signedIn('/app/admission/applications', nobody, fakeAdmission());
    expect(await screen.findByText('Your role may not open admission applications.')).toBeInTheDocument();
  });
});

describe('ADMN-03 Application detail', () => {
  it('shows the whole form and takes the steps its state allows, keeping a reason', async () => {
    const { api } = await signedIn(detail(f.submittedId), office, fakeAdmission());
    expect(await screen.findByText('Little Steps Play School')).toBeInTheDocument();
    expect(within(card('Parents and guardians')).getByText('Meera Rao')).toBeInTheDocument();
    expect(within(card('Emergency contacts')).getByText('+919800000002')).toBeInTheDocument();
    const decision = card('Decision');
    expect(
      within(decision)
        .getAllByRole('button')
        .map((button) => button.textContent),
    ).toEqual(['Mark complete', 'Put on hold', 'Reject', 'Withdraw']);

    await userEvent.type(within(decision).getByLabelText(/Reason/), 'Birth certificate missing');
    await userEvent.click(within(decision).getByRole('button', { name: 'Put on hold' }));
    expect(await screen.findByText('The application is on hold.')).toBeInTheDocument();
    const post = api.calls.find((c) => c.path.endsWith(':markPending'));
    expect(JSON.parse(post?.body ?? '{}')).toEqual({ etag: '"1"', reason: 'Birth certificate missing' });
    await waitFor(() =>
      expect(within(card('Decision')).getByRole('button', { name: 'Mark complete' })).toBeInTheDocument(),
    );
    expect(within(card('Decision')).queryByRole('button', { name: 'Put on hold' })).not.toBeInTheDocument();
  });

  it('enrols an admitted applicant through the wizard and links the student', async () => {
    const { api } = await signedIn(detail(f.admittedId), office, fakeAdmission());
    await userEvent.click(await screen.findByRole('button', { name: 'Enrol as student' }));
    const wizard = await screen.findByRole('dialog', { name: 'Enrol applicant' });
    expect(within(wizard).getByLabelText('Class section id')).toHaveValue(f.classSectionId);
    await userEvent.type(within(wizard).getByLabelText('Roll number (optional)'), '12');
    await userEvent.click(within(wizard).getByRole('button', { name: 'Next' }));
    expect(within(wizard).getByRole('switch', { name: 'Create portal accounts' })).toBeChecked();
    await userEvent.click(within(wizard).getByRole('button', { name: 'Next' }));
    await userEvent.click(within(wizard).getByRole('button', { name: 'Enrol' }));
    expect(await within(wizard).findByText('The applicant is enrolled.')).toBeInTheDocument();
    expect(within(wizard).getByRole('link', { name: 'Open the student' })).toHaveAttribute(
      'href',
      `/app/sis/students/${f.studentId}`,
    );
    expect(within(wizard).getByText('Created')).toBeInTheDocument();
    const post = api.calls.find((c) => c.path.endsWith(':enroll'));
    expect(post?.headers.get('Idempotency-Key')).toBeTruthy();
    expect(JSON.parse(post?.body ?? '{}')).toEqual({
      classSectionId: f.classSectionId,
      rollNumber: '12',
      createPortalAccounts: true,
    });
  });

  it('offers the institution’s classes by name when the role may read them', async () => {
    const reader = {
      ...office,
      permissions: [
        ...office.permissions,
        'academics.class_section.read',
        'academics.grade_level.read',
        'academics.section.read',
      ],
    };
    const { api } = await signedIn(detail(f.admittedId), reader, fakeAdmission());
    await userEvent.click(await screen.findByRole('button', { name: 'Enrol as student' }));
    const wizard = await screen.findByRole('dialog', { name: 'Enrol applicant' });
    const choice = await within(wizard).findByLabelText('Class');
    expect(choice).toHaveValue(f.classSectionId);
    await userEvent.selectOptions(choice, 'Grade 1 B');
    await userEvent.click(within(wizard).getByRole('button', { name: 'Next' }));
    await userEvent.click(within(wizard).getByRole('button', { name: 'Next' }));
    expect(within(wizard).getByText('Grade 1 B')).toBeInTheDocument();
    await userEvent.click(within(wizard).getByRole('button', { name: 'Enrol' }));
    expect(await within(wizard).findByText('The applicant is enrolled.')).toBeInTheDocument();
    const post = api.calls.find((c) => c.path.endsWith(':enroll'));
    expect(JSON.parse(post?.body ?? '{}')).toMatchObject({ classSectionId: f.otherClassSectionId });
  });

  it('says what went wrong when the student records service is down, and keeps the wizard to try again', async () => {
    await signedIn(detail(f.admittedId), office, fakeAdmission({ enrolmentUnavailable: true }));
    await userEvent.click(await screen.findByRole('button', { name: 'Enrol as student' }));
    const wizard = await screen.findByRole('dialog', { name: 'Enrol applicant' });
    await userEvent.click(within(wizard).getByRole('button', { name: 'Next' }));
    await userEvent.click(within(wizard).getByRole('button', { name: 'Next' }));
    await userEvent.click(within(wizard).getByRole('button', { name: 'Enrol' }));
    expect(
      await within(wizard).findByText(/The student records service could not answer/),
    ).toBeInTheDocument();
    expect(within(wizard).getByRole('button', { name: 'Enrol' })).toBeEnabled();
  });
});

describe('ADMN-05 Application form', () => {
  it('lets a signed-in applicant fill the form step by step and hand it in', async () => {
    // a new applicant: no draft of theirs yet
    const { api } = await signedIn(
      `/app/admission/apply/${f.openCycleId}`,
      applicant,
      fakeAdmission({ withoutDraft: true }),
    );
    expect(
      await screen.findByRole('heading', { name: /Application form · Grade 1, 2026–27/ }),
    ).toBeInTheDocument();
    const step = card('Applicant');
    expect(within(step).getByRole('button', { name: 'Save and continue' })).toBeDisabled();
    await userEvent.type(within(step).getByLabelText(/Full name/), 'Nila Bose');
    fireEvent.change(within(step).getByLabelText(/Date of birth/), { target: { value: '2020-02-02' } });
    await userEvent.click(within(step).getByRole('button', { name: 'Save and continue' }));

    const parents = await screen.findByRole('heading', { name: 'Parents and guardians' });
    const guardians = parents.closest('section') as HTMLElement;
    expect(within(guardians).getByRole('button', { name: 'Continue' })).toBeDisabled();
    await userEvent.type(within(guardians).getByLabelText('Name'), 'Arun Bose');
    await userEvent.selectOptions(within(guardians).getByLabelText('Relationship'), 'Father');
    await userEvent.click(within(guardians).getByRole('button', { name: 'Add parent or guardian' }));
    expect(await within(guardians).findByText('Arun Bose')).toBeInTheDocument();
    await userEvent.click(within(guardians).getByRole('button', { name: 'Continue' }));

    const contacts = card('Emergency contacts');
    await userEvent.type(within(contacts).getByLabelText('Phone number'), '+919800000003');
    await userEvent.type(within(contacts).getByLabelText(/Who is this/), 'Aunt');
    await userEvent.click(within(contacts).getByRole('button', { name: 'Add contact' }));
    expect(await within(contacts).findByText('+919800000003')).toBeInTheDocument();
    await userEvent.click(within(contacts).getByRole('button', { name: 'Continue' }));

    await userEvent.click(
      within(card('Review and hand in')).getByRole('button', { name: 'Hand in the application' }),
    );
    expect(await screen.findByText(/Your application number is W-\d{8}-/)).toBeInTheDocument();
    const created = api.calls.find((c) => c.method === 'POST' && c.path === '/v1/admission/applications');
    expect(JSON.parse(created?.body ?? '{}')).toMatchObject({
      institutionId: f.institutionId,
      admissionCycleId: f.openCycleId,
      fullName: 'Nila Bose',
      dateOfBirth: '2020-02-02',
      gender: 'female',
    });
    expect(api.calls.some((c) => c.path.endsWith(':submit'))).toBe(true);
  });

  it('opens the applicant’s saved draft again', async () => {
    await signedIn(`/app/admission/apply/${f.openCycleId}`, applicant, fakeAdmission());
    expect(await screen.findByText(/Your saved application is open again/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Full name/)).toHaveValue('Isha Nair');
  });

  it('refuses a cycle that does not take applications', async () => {
    await signedIn(`/app/admission/apply/${f.draftCycleId}`, applicant, fakeAdmission());
    expect(
      await screen.findByText('This admission cycle does not take applications now.'),
    ).toBeInTheDocument();
  });
});
