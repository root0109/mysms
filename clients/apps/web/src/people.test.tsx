import { PEOPLE_OFFICE, SELF_SERVICE, fakePeople, peopleFixture, schoolAdmin } from '@mysms/fixtures';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { signedIn } from './test/signedIn';

const office = { ...schoolAdmin, permissions: [...SELF_SERVICE, ...PEOPLE_OFFICE] };

describe('SIS-04 Guardians', () => {
  it('opens a guardian under their name, creates their login and links them to a student found by name', async () => {
    const { api } = await signedIn(
      `/app/sis/guardians?guardian=${peopleFixture.guardianId}`,
      office,
      fakePeople(),
    );
    expect(await screen.findByRole('heading', { name: 'Meera Sharma' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Create login account' }));
    expect(await screen.findByText("The guardian's login account was created.")).toBeInTheDocument();

    await userEvent.click(screen.getByRole('radio', { name: 'Link to a student' }));
    await userEvent.type(screen.getByRole('searchbox', { name: "Student's name" }), 'Priya');
    await userEvent.click(await screen.findByRole('button', { name: 'Choose Priya Sharma' }));
    const relationship = screen.getByRole('combobox', { name: 'Relationship' });
    // an inactive relationship type is not offered
    expect(screen.queryByRole('option', { name: 'Retired' })).not.toBeInTheDocument();
    await screen.findByRole('option', { name: 'Mother' });
    await userEvent.selectOptions(relationship, peopleFixture.motherTypeId);
    await userEvent.click(screen.getByRole('switch', { name: 'Primary contact' }));
    await userEvent.click(screen.getByRole('button', { name: 'Link to Priya Sharma' }));
    expect(await screen.findByText('Linked to Priya Sharma.')).toBeInTheDocument();
    const link = api.calls.find((c) => c.method === 'POST' && c.path.endsWith('/guardians'));
    expect(link?.path).toBe(`/v1/sis/students/${peopleFixture.priyaId}/guardians`);
    expect(JSON.parse(link?.body ?? '{}')).toMatchObject({
      guardianId: peopleFixture.guardianId,
      relationshipTypeId: peopleFixture.motherTypeId,
      isPrimaryContact: true,
      receivesCommunication: true,
      canPickUp: false,
    });
  });

  it('lists the guardians and opens one in place', async () => {
    await signedIn('/app/sis/guardians', office, fakePeople());
    const open = await screen.findByRole('link', { name: /^Open/ });
    expect(open).toHaveAttribute('href', `/app/sis/guardians?guardian=${peopleFixture.guardianId}`);
  });
});

describe('SIS-08 Documents', () => {
  it('finds a person, lists their documents and adds one by uploading its file', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 200 }));
    const { api, router } = await signedIn('/app/people/person-documents', office, fakePeople());
    await userEvent.type(await screen.findByRole('searchbox', { name: 'Name' }), 'Priya');
    await userEvent.click(await screen.findByRole('button', { name: 'Priya Sharma' }));
    await waitFor(() => expect(router.state.location.search).toBe(`?person=${peopleFixture.priyaPersonId}`));
    expect(await screen.findByText('Birth certificate')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Documents of Priya Sharma' })).toBeInTheDocument();
    // the file is asked for once the document has a name
    expect(screen.getByRole('button', { name: 'Upload the file' })).toBeDisabled();
    await userEvent.type(screen.getByLabelText('Document name'), 'Transfer certificate');
    await userEvent.upload(
      screen.getByLabelText('Upload the file'),
      new File(['tc!'], 'tc.pdf', { type: 'application/pdf' }),
    );
    expect(await screen.findByText('The document was added.')).toBeInTheDocument();
    expect(await screen.findByText('Transfer certificate')).toBeInTheDocument();
    const created = api.calls.find((c) => c.method === 'POST' && c.path.endsWith('/documents'));
    expect(created?.path).toBe(`/v1/people/persons/${peopleFixture.priyaPersonId}/documents`);
    expect(JSON.parse(created?.body ?? '{}')).toMatchObject({
      documentType: 'certificate',
      name: 'Transfer certificate',
      fileId: peopleFixture.fileId,
    });
  });

  it('tells a role without the permission so', async () => {
    await signedIn(
      '/app/people/person-documents',
      { ...schoolAdmin, permissions: [...SELF_SERVICE] },
      fakePeople(),
    );
    expect(await screen.findByText('Your role may not list these records.')).toBeInTheDocument();
  });
});
