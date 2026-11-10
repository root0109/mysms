import { act, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import {
  AppShell,
  Banner,
  Button,
  Card,
  DataTable,
  Dialog,
  ProgressBar,
  SegmentedControl,
  SelectField,
  Skeleton,
  StatusChip,
  Switch,
  TextArea,
  TextInput,
} from './index';
import { axeViolations } from './test/axe';

describe('Button', () => {
  it('is a non-submitting button by default and blocks clicks while loading', async () => {
    const onClick = vi.fn();
    const { rerender, container } = render(<Button onClick={onClick}>Save</Button>);
    const button = screen.getByRole('button', { name: 'Save' });
    expect(button).toHaveAttribute('type', 'button');
    expect(button).toHaveClass('sms-button--primary');
    await userEvent.click(button);
    expect(onClick).toHaveBeenCalledTimes(1);
    rerender(
      <Button onClick={onClick} loading variant="danger">
        Save
      </Button>,
    );
    expect(button).toBeDisabled();
    expect(button).toHaveAttribute('aria-busy', 'true');
    expect(await axeViolations(container)).toEqual([]);
  });
});

describe('TextInput', () => {
  it('labels the input and wires hint and error for assistive tech', async () => {
    const { container } = render(<TextInput label="Name" hint="As on the certificate" error="Required" />);
    const input = screen.getByLabelText('Name');
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveAccessibleDescription('As on the certificate Required');
    expect(screen.getByRole('alert')).toHaveTextContent('Required');
    expect(await axeViolations(container)).toEqual([]);
  });

  it('has no description or invalid state when clean', () => {
    render(<TextInput label="Code" id="code" />);
    const input = screen.getByLabelText('Code');
    expect(input).toHaveAttribute('id', 'code');
    expect(input).not.toHaveAttribute('aria-invalid');
    expect(input).not.toHaveAttribute('aria-describedby');
  });
});

describe('StatusChip, Banner, Skeleton, ProgressBar, Card', () => {
  it('render accessible, colour-independent states', async () => {
    const { container } = render(
      <div>
        <StatusChip intent="success" label="Paid" />
        <StatusChip label="Archived" />
        <StatusChip intent={'bogus' as never} label="Unknown" />
        <Banner intent="danger" title="Failed" action={<Button>Retry</Button>}>
          Could not load.
        </Banner>
        <Banner>Info only</Banner>
        <Skeleton label="Loading…" lines={2} />
        <ProgressBar label="Generating" value={142} />
        <ProgressBar label="Waiting" />
        <Card title="Summary" actions={<Button variant="ghost">Edit</Button>}>
          Body
        </Card>
        <Card>Plain</Card>
      </div>,
    );
    expect(screen.getByText('Paid')).toHaveClass('sms-chip--success');
    expect(screen.getByText('Archived')).toHaveClass('sms-chip--neutral');
    expect(screen.getByText('Unknown')).toHaveClass('sms-chip--neutral');
    expect(screen.getByRole('alert')).toHaveTextContent('FailedCould not load.');
    expect(screen.getAllByRole('status')[0]).toHaveTextContent('Info only');
    expect(screen.getByRole('status', { name: 'Loading…' })).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('progressbar', { name: 'Generating' })).toHaveAttribute('aria-valuenow', '100');
    expect(screen.getByRole('progressbar', { name: 'Waiting' })).not.toHaveAttribute('aria-valuenow');
    expect(screen.getByRole('heading', { name: 'Summary' })).toBeInTheDocument();
    expect(await axeViolations(container)).toEqual([]);
  });
});

describe('DataTable', () => {
  const columns = [
    { key: 'name', header: 'Name', render: (r: { id: string; name: string; n: number }) => r.name },
    {
      key: 'n',
      header: 'Count',
      align: 'end' as const,
      render: (r: { id: string; name: string; n: number }) => r.n,
    },
  ];

  it('renders rows with scoped headers and supports keyboard row activation', async () => {
    const onRowClick = vi.fn();
    const { container } = render(
      <DataTable
        caption="People"
        columns={columns}
        rows={[{ id: '1', name: 'Asha', n: 3 }]}
        rowKey={(r) => r.id}
        loadingLabel="Loading"
        emptyText="None"
        onRowClick={onRowClick}
        density="comfortable"
      />,
    );
    expect(screen.getByRole('table', { name: 'People' })).toHaveClass('sms-table--comfortable');
    expect(screen.getAllByRole('columnheader').map((h) => h.getAttribute('scope'))).toEqual(['col', 'col']);
    const row = screen.getByRole('row', { name: /Asha/ });
    fireEvent.keyDown(row, { key: 'Enter' });
    fireEvent.keyDown(row, { key: 'x' });
    await userEvent.click(row);
    expect(onRowClick).toHaveBeenCalledTimes(2);
    expect(await axeViolations(container)).toEqual([]);
  });

  it('shows loading and empty states', () => {
    const { rerender } = render(
      <DataTable
        caption="People"
        columns={columns}
        rows={undefined}
        rowKey={(r) => r.id}
        loading
        loadingLabel="Loading"
        emptyText="None"
      />,
    );
    expect(screen.getByRole('status', { name: 'Loading' })).toBeInTheDocument();
    rerender(
      <DataTable
        caption="People"
        columns={columns}
        rows={[]}
        rowKey={(r) => r.id}
        loadingLabel="Loading"
        emptyText="None"
      />,
    );
    expect(screen.getByRole('cell', { name: 'None' })).toHaveAttribute('colspan', '2');
  });
});

describe('Dialog', () => {
  function Harness() {
    const [open, setOpen] = useState(false);
    return (
      <>
        <Button onClick={() => setOpen(true)}>Open</Button>
        <Dialog
          open={open}
          title="Edit"
          onClose={() => setOpen(false)}
          footer={<Button onClick={() => setOpen(false)}>Done</Button>}
        >
          <TextInput label="Name" />
        </Dialog>
      </>
    );
  }

  it('moves focus in, traps Tab, closes on Escape and restores focus', async () => {
    render(<Harness />);
    const opener = screen.getByRole('button', { name: 'Open' });
    await userEvent.click(opener);
    const dialog = screen.getByRole('dialog', { name: 'Edit' });
    expect(dialog).toHaveAttribute('aria-modal', 'true');
    const input = screen.getByLabelText('Name');
    const done = screen.getByRole('button', { name: 'Done' });
    expect(input).toHaveFocus();
    act(() => done.focus());
    fireEvent.keyDown(document, { key: 'Tab' });
    expect(input).toHaveFocus();
    fireEvent.keyDown(document, { key: 'Tab', shiftKey: true });
    expect(done).toHaveFocus();
    fireEvent.keyDown(document, { key: 'Enter' });
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    fireEvent.keyDown(document, { key: 'Escape' });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(opener).toHaveFocus();
  });

  it('focuses the panel when it has nothing focusable', () => {
    render(
      <Dialog open title="Note" onClose={() => undefined}>
        Read only
      </Dialog>,
    );
    expect(screen.getByRole('dialog')).toHaveFocus();
    fireEvent.keyDown(document, { key: 'Tab' });
    expect(screen.getByRole('dialog')).toHaveFocus();
  });
});

describe('AppShell', () => {
  it('provides a skip link, labelled navigation, one main region and the environment band', async () => {
    const { container } = render(
      <AppShell
        brand="MySMS"
        nav={<a href="/app">Home</a>}
        navLabel="Main"
        actions={<Button>Sign out</Button>}
        skipLinkLabel="Skip to content"
        environmentBand="Platform console"
      >
        <h1>Home</h1>
      </AppShell>,
    );
    expect(screen.getByRole('link', { name: 'Skip to content' })).toHaveAttribute('href', '#main');
    expect(screen.getByRole('navigation', { name: 'Main' })).toBeInTheDocument();
    expect(screen.getByRole('main')).toHaveAttribute('id', 'main');
    expect(screen.getByText('Platform console')).toHaveClass('sms-shell__band');
    expect(await axeViolations(container)).toEqual([]);
  });
});

describe('SelectField', () => {
  it('labels the select, lists the options and reports changes', async () => {
    const onChange = vi.fn();
    const { container } = render(
      <SelectField
        label="Theme"
        hint="Applies at once"
        value="light"
        onChange={(e) => onChange(e.target.value)}
        options={[
          { value: 'light', label: 'Light' },
          { value: 'dark', label: 'Dark' },
        ]}
      />,
    );
    const select = screen.getByRole('combobox', { name: 'Theme' });
    expect(select).toHaveValue('light');
    expect(screen.getAllByRole('option')).toHaveLength(2);
    expect(select).toHaveAccessibleDescription('Applies at once');
    await userEvent.selectOptions(select, 'dark');
    expect(onChange).toHaveBeenCalledWith('dark');
    expect(await axeViolations(container)).toEqual([]);
  });

  it('marks an error', () => {
    render(<SelectField label="Locale" options={[{ value: 'en', label: 'English' }]} error="Pick one" />);
    expect(screen.getByRole('combobox', { name: 'Locale' })).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByRole('alert')).toHaveTextContent('Pick one');
  });
});

describe('Switch', () => {
  it('is a labelled switch that toggles and can be disabled', async () => {
    function Harness() {
      const [on, setOn] = useState(false);
      return <Switch label="Reduce motion" hint="Fewer animations" checked={on} onChange={setOn} />;
    }
    const { container } = render(<Harness />);
    const toggle = screen.getByRole('switch', { name: 'Reduce motion' });
    expect(toggle).toHaveAttribute('aria-checked', 'false');
    await userEvent.click(toggle);
    expect(toggle).toHaveAttribute('aria-checked', 'true');
    expect(toggle).toHaveClass('sms-switch--on');
    expect(toggle).toHaveAccessibleDescription('Fewer animations');
    render(<Switch label="Digest" checked={false} onChange={() => undefined} error="Locked now" />);
    expect(screen.getByRole('switch', { name: 'Digest' })).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByRole('alert')).toHaveTextContent('Locked now');
    expect(await axeViolations(container)).toEqual([]);
    const onChange = vi.fn();
    render(<Switch label="Locked" checked onChange={onChange} disabled />);
    const locked = screen.getByRole('switch', { name: 'Locked' });
    expect(locked).toBeDisabled();
    fireEvent.click(locked);
    expect(onChange).not.toHaveBeenCalled();
  });
});

describe('TextArea', () => {
  it('labels the text area and wires its error', async () => {
    const { container } = render(<TextArea label="About me" error="Too long" defaultValue="Hi" />);
    const area = screen.getByRole('textbox', { name: 'About me' });
    expect(area).toHaveValue('Hi');
    expect(area).toHaveAttribute('rows', '4');
    expect(area).toHaveAttribute('aria-invalid', 'true');
    expect(await axeViolations(container)).toEqual([]);
  });
});

describe('SegmentedControl', () => {
  it('is a named radio group: one tap chooses, arrow keys move, a disabled group does not change', async () => {
    function Harness({ disabled }: { disabled?: boolean }) {
      const [value, setValue] = useState<'present' | 'absent' | undefined>(undefined);
      return (
        <SegmentedControl
          label="Asha Rao"
          value={value}
          onChange={setValue}
          disabled={disabled ?? false}
          options={[
            { value: 'present', label: 'Present', intent: 'success' },
            { value: 'absent', label: 'Absent', intent: 'danger' },
          ]}
        />
      );
    }
    const { container, rerender } = render(<Harness />);
    const group = screen.getByRole('radiogroup', { name: 'Asha Rao' });
    expect(group).toBeInTheDocument();
    const present = screen.getByRole('radio', { name: 'Present' });
    const absent = screen.getByRole('radio', { name: 'Absent' });
    expect(present).not.toBeChecked();
    expect(absent).not.toBeChecked();
    await userEvent.click(absent);
    expect(absent).toBeChecked();
    expect(absent.closest('label')).toHaveClass('sms-segmented__option--danger');
    await userEvent.keyboard('{ArrowLeft}');
    expect(present).toBeChecked();
    expect(present.closest('label')).toHaveClass('sms-segmented__option--success');
    expect(await axeViolations(container)).toEqual([]);
    rerender(<Harness disabled />);
    await userEvent.click(absent);
    expect(present).toBeChecked();
    expect(absent).toBeDisabled();
  });
});
