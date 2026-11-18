import {
  USER_ACCOUNT_STATUSES,
  USER_TYPES,
  isApiError,
  useCan,
  useCreateUserAccount,
  useI18n,
  useUserAccounts,
  useUsernameAvailability,
  type UserAccount,
  type UserAccountStatus,
  type UserType,
} from '@mysms/core';
import {
  Banner,
  Button,
  Card,
  DataTable,
  Dialog,
  SelectField,
  StatusChip,
  TextInput,
  type Column,
} from '@mysms/ui-web';
import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router';
import { USER_STATUS, USER_TYPE, userLink } from './userText';

/**
 * ADM-08 Users: the tenant's login accounts, found by login name, e-mail or mobile number and narrowed by status and
 * account type; a row opens the account (ADM-09), where its status, credentials, role grants and impersonation are
 * handled. A role that may create accounts adds one here (without a password: credentials are set on the account).
 */
export function UsersScreen() {
  const { t } = useI18n();
  const mayRead = useCan('iam.user_account.read');
  const mayCreate = useCan('iam.user_account.create');
  const [query, setQuery] = useSearchParams();
  const [search, setSearch] = useState('');
  const [adding, setAdding] = useState(false);
  const status = USER_ACCOUNT_STATUSES.find((value) => value === query.get('status'));
  const userType = USER_TYPES.find((value) => value === query.get('type'));
  const setFilter = (name: 'status' | 'type', value: string) =>
    setQuery(
      (previous) => {
        const next = new URLSearchParams(previous);
        if (value) next.set(name, value);
        else next.delete(name);
        return next;
      },
      { replace: true },
    );
  const text = search.trim();
  const list = useUserAccounts(
    { ...(text ? { search: text } : {}), ...(status ? { status } : {}), ...(userType ? { userType } : {}) },
    mayRead,
  );
  const rows = list.data?.pages.flatMap((page) => page.data) ?? [];
  const title = t('screen.ADM08.title');

  if (!mayRead)
    return (
      <div className="page">
        <header className="page__header">
          <h1 className="page__title">{title}</h1>
        </header>
        <Banner intent="info">{t('adm.users.noAccess')}</Banner>
      </div>
    );

  const columns: Column<UserAccount>[] = [
    {
      key: 'username',
      header: t('adm.users.column.username'),
      render: (row) => <Link to={userLink(row.id ?? '')}>{row.username}</Link>,
    },
    { key: 'email', header: t('adm.users.column.email'), render: (row) => row.email ?? '—' },
    { key: 'mobile', header: t('adm.users.column.mobile'), render: (row) => row.mobileNo ?? '—' },
    {
      key: 'type',
      header: t('adm.users.column.type'),
      render: (row) => (row.userType ? t(USER_TYPE[row.userType]) : '—'),
    },
    {
      key: 'status',
      header: t('adm.users.column.status'),
      render: (row) => {
        const state = row.status ? USER_STATUS[row.status] : undefined;
        return <StatusChip intent={state?.intent} label={state ? t(state.label) : '—'} />;
      },
    },
  ];

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{title}</h1>
        <p className="page__subtitle">{t('adm.users.intro')}</p>
      </header>
      <Card
        title={t('common.list.count', { count: rows.length })}
        actions={mayCreate ? <Button onClick={() => setAdding(true)}>{t('adm.users.add')}</Button> : null}
      >
        <div className="page__grid">
          <TextInput
            label={t('adm.users.search')}
            type="search"
            maxLength={100}
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
          <SelectField
            label={t('adm.users.filter.status')}
            value={status ?? ''}
            onChange={(event) => setFilter('status', event.target.value)}
            options={[
              { value: '', label: t('adm.users.filter.all') },
              ...USER_ACCOUNT_STATUSES.map((value: UserAccountStatus) => ({
                value,
                label: t(USER_STATUS[value].label),
              })),
            ]}
          />
          <SelectField
            label={t('adm.users.filter.type')}
            value={userType ?? ''}
            onChange={(event) => setFilter('type', event.target.value)}
            options={[
              { value: '', label: t('adm.users.filter.all') },
              ...USER_TYPES.map((value: UserType) => ({ value, label: t(USER_TYPE[value]) })),
            ]}
          />
        </div>
        {list.isError ? (
          <Banner
            intent="danger"
            action={
              <Button variant="secondary" onClick={() => void list.refetch()}>
                {t('common.action.retry')}
              </Button>
            }
          >
            {t('common.error.generic')}
          </Banner>
        ) : (
          <DataTable
            caption={title}
            columns={columns}
            rows={rows}
            rowKey={(row) => row.id ?? row.username ?? ''}
            loading={list.isPending}
            loadingLabel={t('common.state.loading')}
            emptyText={t(text || status || userType ? 'adm.users.noMatch' : 'adm.users.empty')}
          />
        )}
        {list.hasNextPage ? (
          <Button
            variant="secondary"
            loading={list.isFetchingNextPage}
            onClick={() => void list.fetchNextPage()}
          >
            {t('common.list.more')}
          </Button>
        ) : null}
      </Card>
      {adding ? <NewUserDialog onClose={() => setAdding(false)} /> : null}
    </div>
  );
}

const EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

/** A new account: a login name (checked as it is typed), e-mail, mobile number and type; no password yet. */
function NewUserDialog({ onClose }: { onClose: () => void }) {
  const { t } = useI18n();
  const navigate = useNavigate();
  const create = useCreateUserAccount();
  const mayCheck = useCan('iam.user_account.create');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [mobileNo, setMobileNo] = useState('');
  const [userType, setUserType] = useState<UserType>('teacher');
  const [touched, setTouched] = useState(false);
  const availability = useUsernameAvailability(username, mayCheck);
  const taken = availability.data?.available === false;
  const serverErrors = isApiError(create.error) ? create.error.fieldErrors() : {};
  const emailInvalid = email.trim() !== '' && !EMAIL.test(email.trim());
  const submit = (event: FormEvent) => {
    event.preventDefault();
    setTouched(true);
    if (!username.trim() || emailInvalid || taken) return;
    create.mutate(
      {
        username: username.trim(),
        userType,
        ...(email.trim() ? { email: email.trim() } : {}),
        ...(mobileNo.trim() ? { mobileNo: mobileNo.trim() } : {}),
      },
      { onSuccess: (created) => navigate(userLink(created.id ?? '')) },
    );
  };
  return (
    <Dialog
      open
      title={t('adm.users.addTitle')}
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button type="submit" form="new-user" loading={create.isPending}>
            {t('adm.users.create')}
          </Button>
        </>
      }
    >
      <form id="new-user" className="form" noValidate onSubmit={submit}>
        <TextInput
          label={t('adm.users.column.username')}
          autoComplete="off"
          maxLength={150}
          value={username}
          hint={
            taken && availability.data?.suggestion
              ? t('adm.users.suggestion', { suggestion: availability.data.suggestion })
              : undefined
          }
          error={
            touched && !username.trim()
              ? t('common.validation.required')
              : taken
                ? t('adm.users.taken')
                : serverErrors.username
          }
          onChange={(e) => setUsername(e.target.value)}
        />
        <TextInput
          label={t('adm.users.column.email')}
          type="email"
          maxLength={320}
          value={email}
          error={emailInvalid ? t('acc.signUp.invalidEmail') : serverErrors.email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <TextInput
          label={t('adm.users.column.mobile')}
          type="tel"
          maxLength={20}
          value={mobileNo}
          error={serverErrors.mobileNo}
          onChange={(e) => setMobileNo(e.target.value)}
        />
        <SelectField
          label={t('adm.users.column.type')}
          value={userType}
          onChange={(e) => setUserType(e.target.value as UserType)}
          options={USER_TYPES.map((value) => ({ value, label: t(USER_TYPE[value]) }))}
        />
        {create.isError && !Object.keys(serverErrors).length ? (
          <Banner intent="danger">
            {t(
              isApiError(create.error) && create.error.status === 409
                ? 'adm.users.duplicate'
                : 'common.error.generic',
            )}
          </Banner>
        ) : null}
      </form>
    </Dialog>
  );
}
