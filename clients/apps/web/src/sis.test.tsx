import { SELF_SERVICE, SIS_OFFICE, SIS_PROMOTER, fakeSis, schoolAdmin, sisFixture } from '@mysms/fixtures';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { signedIn } from './test/signedIn';

const f = sisFixture;
const office = { ...schoolAdmin, permissions: [...SELF_SERVICE, ...SIS_OFFICE] };
const studentPath = `/app/sis/students/${f.studentId}`;
const bodyOf = (calls: { path: string; body: string }[], suffix: string) =>
  JSON.parse(calls.filter((c) => c.path.endsWith(suffix)).at(-1)?.body ?? '{}') as Record<string, unknown>;

describe('SIS-02 Student 360°', () => {
  it('shows the student and lets the office change the status, issue a certificate and open the portal account', async () => {
    const { api } = await signedIn(studentPath, office, fakeSis());
    expect(await screen.findByRole('heading', { name: 'Diya Menon' })).toBeInTheDocument();
    expect(screen.getByText(/Admission number A-104/)).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'Profile', selected: true })).toBeInTheDocument();
    expect(screen.getByText('Open incidents')).toBeInTheDocument();

    // an exit needs its date before it can be sent
    const status = screen.getByRole('form', { name: 'Change status' });
    await userEvent.selectOptions(within(status).getByLabelText('New status'), 'Withdrawn');
    expect(within(status).getByRole('button', { name: 'Change status' })).toBeDisabled();
    fireEvent.change(within(status).getByLabelText('Exit date'), { target: { value: '2026-04-01' } });
    await userEvent.type(within(status).getByLabelText('Exit reason (optional)'), 'Moved away');
    await userEvent.click(within(status).getByRole('button', { name: 'Change status' }));
    expect(await screen.findByText('The status is now Withdrawn.')).toBeInTheDocument();
    expect(bodyOf(api.calls, ':changeStatus')).toEqual({
      status: 'withdrawn',
      etag: '"1"',
      exitDate: '2026-04-01',
      exitReason: 'Moved away',
    });

    // a certificate is issued to a current student only: the server's sentence is shown
    const bonafide = screen.getByRole('form', { name: 'Bonafide certificate' });
    await userEvent.click(within(bonafide).getByRole('button', { name: 'Issue bonafide certificate' }));
    expect(await screen.findByRole('alert')).toBeInTheDocument();
    await userEvent.selectOptions(within(status).getByLabelText('New status'), 'Active');
    await userEvent.click(within(status).getByRole('button', { name: 'Change status' }));
    expect(await screen.findByText('The status is now Active.')).toBeInTheDocument();
    await userEvent.type(within(bonafide).getByLabelText('Purpose (optional)'), 'Passport');
    await userEvent.click(within(bonafide).getByRole('button', { name: 'Issue bonafide certificate' }));
    expect(await screen.findByText('Bonafide certificate BC/2026/000001 is issued.')).toBeInTheDocument();
    expect(bodyOf(api.calls, ':issueBonafideCertificate')).toEqual({ purpose: 'Passport' });

    await userEvent.click(screen.getByRole('button', { name: 'Create portal account' }));
    expect(await screen.findByText('The portal account is created.')).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.queryByRole('button', { name: 'Create portal account' })).not.toBeInTheDocument(),
    );
  });

  it('lists the guardians with their names and records the incident steps', async () => {
    const { api } = await signedIn(studentPath, office, fakeSis());
    await userEvent.click(await screen.findByRole('tab', { name: 'Guardians' }));
    expect(await screen.findByText('Lakshmi Menon')).toBeInTheDocument();
    expect(screen.getByText(/Primary contact/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Create portal account for Lakshmi Menon' }));
    expect(await screen.findByText('The portal account is created.')).toBeInTheDocument();
    expect(api.calls.some((c) => c.path === `/v1/sis/guardians/${f.guardianId}:createPortalAccount`)).toBe(
      true,
    );
    // the guardian cards are read again: the account shows, so it is not offered twice
    await waitFor(() =>
      expect(
        screen.queryByRole('button', { name: 'Create portal account for Lakshmi Menon' }),
      ).not.toBeInTheDocument(),
    );

    await userEvent.click(screen.getByRole('tab', { name: 'Discipline' }));
    expect(await screen.findByText('Left the lab without permission.')).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText('What was done (optional)'), 'Met the parents');
    await userEvent.click(screen.getByRole('button', { name: 'Mark action taken' }));
    expect(await screen.findByText('The action is recorded.')).toBeInTheDocument();
    expect(bodyOf(api.calls, ':markActionTaken')).toEqual({ etag: '"1"', response: 'Met the parents' });
    await userEvent.click(await screen.findByRole('button', { name: 'Mark seen by guardian' }));
    expect(await screen.findByText('The guardian’s acknowledgement is recorded.')).toBeInTheDocument();
    expect(bodyOf(api.calls, ':markSeen')).toEqual({ etag: '"2"' });
  });

  it('offers no steps to a reader and tells an unknown student apart', async () => {
    const reader = {
      ...schoolAdmin,
      permissions: [...SELF_SERVICE, 'sis.student.read', 'sis.student.get_student_summary'],
    };
    await signedIn(studentPath, reader, fakeSis());
    expect(await screen.findByRole('heading', { name: 'Diya Menon' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Change status' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Create portal account' })).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('tab', { name: 'Health' }));
    expect(screen.getByText('Your role may not see this part of the record.')).toBeInTheDocument();
  });

  it('says so when the student cannot be seen or the role may not open summaries', async () => {
    const first = await signedIn('/app/sis/students/01960000-0000-7000-8000-00000000beef', office, fakeSis());
    expect(
      await screen.findByText('This student does not exist or is not visible to you.'),
    ).toBeInTheDocument();
    first.unmount();
    await signedIn(
      studentPath,
      { ...schoolAdmin, permissions: [...SELF_SERVICE, 'sis.student.read'] },
      fakeSis(),
    );
    expect(await screen.findByText('Your role may not open student summaries.')).toBeInTheDocument();
  });
});

describe('SIS-03 Enrolment & promotion', () => {
  it('proposes promotions for a class, decides them and applies the approved ones', async () => {
    const promoter = {
      ...schoolAdmin,
      permissions: [...SELF_SERVICE, ...SIS_PROMOTER, 'sis.student.read', 'people.person.read'],
    };
    const { api } = await signedIn('/app/sis/student-enrollments', promoter, fakeSis());
    expect(await screen.findByRole('heading', { name: 'Promotion' })).toBeInTheDocument();
    const select = screen
      .getByRole('heading', { name: '1. Choose the class and where it goes' })
      .closest('section') as HTMLElement;
    await waitFor(() =>
      expect(within(select).getByRole('option', { name: 'Grade 5 A' })).toBeInTheDocument(),
    );
    await userEvent.selectOptions(within(select).getByLabelText('Class'), 'Grade 5 A');
    await userEvent.selectOptions(within(select).getByLabelText('Next academic year'), '2026-27');
    await userEvent.selectOptions(within(select).getByLabelText('Next grade'), 'Grade 6');
    await userEvent.click(within(select).getByRole('button', { name: 'Propose promotions' }));
    expect(bodyOf(api.calls, ':propose')).toEqual({
      fromClassSectionId: f.classSectionId,
      toAcademicYearId: f.nextYear,
      toGradeLevelId: f.nextGrade,
    });

    const review = (await screen.findByRole('heading', { name: '2. Review and 3. apply' })).closest(
      'section',
    ) as HTMLElement;
    expect(await within(review).findByText('Diya Menon')).toBeInTheDocument();
    expect(within(review).getByText('Arjun Das')).toBeInTheDocument();
    expect(within(review).getByRole('button', { name: 'Apply approved promotions (0)' })).toBeDisabled();
    await userEvent.click(
      within(review).getByRole('button', { name: 'Approve the promotion of Diya Menon' }),
    );
    await userEvent.type(
      within(review).getByLabelText('Reason for Arjun Das (optional)'),
      'Repeats the year',
    );
    await userEvent.click(within(review).getByRole('button', { name: 'Reject the promotion of Arjun Das' }));
    await waitFor(() =>
      expect(within(review).getByText('0 to decide, 1 approved, 2 in all.')).toBeInTheDocument(),
    );
    expect(bodyOf(api.calls, ':reject')).toEqual({ etag: '"1"', reason: 'Repeats the year' });
    await userEvent.click(within(review).getByRole('button', { name: 'Apply approved promotions (1)' }));
    expect(await within(review).findByText('1 students are placed in the next year.')).toBeInTheDocument();
    expect(bodyOf(api.calls, ':apply')).toEqual({ promotionIds: [expect.any(String)] });
    // the enrolments themselves stay in the list manager below the wizard
    expect(screen.getByRole('heading', { name: 'Enrolment & promotion' })).toBeInTheDocument();
  });

  it('lets an approver who may not propose approve all and shows every refusal', async () => {
    const approver = {
      ...schoolAdmin,
      permissions: [
        ...SELF_SERVICE,
        'sis.promotion.read',
        'sis.promotion.approve_promotion',
        'org.institution.read',
        'sis.student.read',
        'people.person.read',
      ],
    };
    await signedIn(
      '/app/sis/student-enrollments',
      approver,
      fakeSis({ proposed: true, refuseClassmate: true }),
    );
    expect(
      screen.queryByRole('heading', { name: '1. Choose the class and where it goes' }),
    ).not.toBeInTheDocument();
    const review = (await screen.findByRole('heading', { name: '2. Review and 3. apply' })).closest(
      'section',
    ) as HTMLElement;
    expect(await within(review).findByText('Diya Menon')).toBeInTheDocument();
    expect(
      within(review).queryByRole('button', { name: /Apply approved promotions/ }),
    ).not.toBeInTheDocument();
    expect(within(review).queryByRole('button', { name: /Reject the promotion/ })).not.toBeInTheDocument();
    await userEvent.click(within(review).getByRole('button', { name: 'Approve all proposed' }));
    expect(
      await within(review).findByText(
        '1 of 2 promotions could not be approved. The promotion cannot be changed this way in its current state.',
      ),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(within(review).getByText('1 to decide, 1 approved, 2 in all.')).toBeInTheDocument(),
    );
  });

  it('lets a role that only applies apply what was approved', async () => {
    const applier = {
      ...schoolAdmin,
      permissions: [
        ...SELF_SERVICE,
        'sis.promotion.read',
        'sis.promotion.apply_promotions',
        'org.institution.read',
      ],
    };
    await signedIn('/app/sis/student-enrollments', applier, fakeSis({ proposed: true }));
    const review = (await screen.findByRole('heading', { name: '2. Review and 3. apply' })).closest(
      'section',
    ) as HTMLElement;
    expect(await within(review).findByText('2 to decide, 0 approved, 2 in all.')).toBeInTheDocument();
    expect(within(review).queryByRole('button', { name: 'Approve all proposed' })).not.toBeInTheDocument();
    expect(within(review).getByRole('button', { name: 'Apply approved promotions (0)' })).toBeDisabled();
  });

  it('shows only the enrolments to a role that may not promote', async () => {
    await signedIn(
      '/app/sis/student-enrollments',
      { ...schoolAdmin, permissions: [...SELF_SERVICE, 'sis.student_enrollment.read'] },
      fakeSis(),
    );
    expect(await screen.findByRole('heading', { name: 'Enrolment & promotion' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Promotion' })).not.toBeInTheDocument();
  });
});
