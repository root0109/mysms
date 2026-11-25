import {
  newIdempotencyKey,
  rightsFailureKey,
  useI18n,
  useMyAccessSummary,
  useMyRequests,
  useSession,
  useSubmitMyRequest,
  type GrievanceCategory,
  type MyRightsRequest,
  type RightsRequestType,
} from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import { Banner, Button, ChoiceGroup, Screen, Text, TextInput } from '@mysms/ui-native';
import { useState } from 'react';
import { View } from 'react-native';

/** The rights asked for on the phone: seeing one's data and complaining; a correction is made on the web. */
const TYPES: readonly RightsRequestType[] = ['access', 'grievance'];
const GRIEVANCE_CATEGORIES: readonly GrievanceCategory[] = [
  'consent',
  'access',
  'correction',
  'data_security',
  'communication',
  'other',
];

/**
 * ACC-15 My privacy requests on the phone and tablet (ui/02; design PRIVACY-AND-CONSENT-CENTRE §9.6, §12): the user
 * asks what the school holds about them or raises a complaint, and follows each request — its deadline, outcome, the
 * school's answer and a fulfilled access request's summary. Corrections and the nominee are managed on the web
 * screen of the same route.
 */
export function MyRequestsScreen({ tablet, onBack }: { tablet: boolean; onBack: () => void }) {
  const { t, format } = useI18n();
  const requests = useMyRequests();
  const submit = useSubmitMyRequest();
  const me = useSession((session) => session.me?.user?.personId);
  const [type, setType] = useState<RightsRequestType>('access');
  const [category, setCategory] = useState<GrievanceCategory>('other');
  const [description, setDescription] = useState('');
  const [key, setKey] = useState(newIdempotencyKey);
  const [sent, setSent] = useState<MyRightsRequest | null>(null);
  const [open, setOpen] = useState<MyRightsRequest | null>(null);

  const send = () => {
    if (!me || !description.trim()) return;
    submit.mutate(
      {
        idempotencyKey: key,
        draft: {
          type,
          subjectPersonId: me,
          description,
          ...(type === 'grievance' ? { grievanceCategory: category } : {}),
        },
      },
      {
        onSuccess: (request) => {
          setSent(request);
          setDescription('');
          setKey(newIdempotencyKey());
        },
      },
    );
  };

  if (open) return <RequestPage tablet={tablet} request={open} onBack={() => setOpen(null)} />;
  return (
    <Screen tablet={tablet}>
      <Text variant="h1" accessibilityRole="header">
        {t('privacy.myRequests.title')}
      </Text>
      <Text muted>{t('privacy.myRequests.intro')}</Text>
      <Button variant="secondary" onPress={onBack}>
        {t('common.action.back')}
      </Button>
      <ChoiceGroup
        label={t('privacy.myRequests.type')}
        choices={TYPES.map((value) => ({ value, label: enumLabel(t, 'rightsRequestType', value) }))}
        value={type}
        onChange={(value) => setType(value as RightsRequestType)}
      />
      {type === 'grievance' ? (
        <ChoiceGroup
          label={t('privacy.myRequests.grievanceCategory')}
          choices={GRIEVANCE_CATEGORIES.map((value) => ({
            value,
            label: enumLabel(t, 'grievanceCategory', value),
          }))}
          value={category}
          onChange={(value) => setCategory(value as GrievanceCategory)}
        />
      ) : null}
      <TextInput
        label={t('privacy.myRequests.description')}
        value={description}
        multiline
        maxLength={4000}
        onChangeText={setDescription}
      />
      {submit.isError ? <Banner intent="danger">{t(rightsFailureKey(submit.error))}</Banner> : null}
      {sent ? (
        <Banner intent="success">
          {t('privacy.myRequests.sent', { number: sent.number, date: format.date(sent.dueAt) })}
        </Banner>
      ) : null}
      <Button onPress={send} disabled={!description.trim() || submit.isPending}>
        {t('privacy.myRequests.send')}
      </Button>
      <Text variant="h2" accessibilityRole="header">
        {t('privacy.myRequests.list')}
      </Text>
      {requests.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
      {requests.isError ? <Banner intent="danger">{t('common.error.generic')}</Banner> : null}
      {requests.data && requests.data.items.length === 0 ? (
        <Text>{t('privacy.myRequests.empty')}</Text>
      ) : null}
      {(requests.data?.items ?? []).map((request) => (
        <View key={request.id}>
          <Text>
            {request.number} · {enumLabel(t, 'rightsRequestType', request.type)} ·{' '}
            {request.outcomeCode
              ? enumLabel(t, 'rightsOutcome', request.outcomeCode)
              : enumLabel(t, 'rightsRequestStatus', request.status)}
          </Text>
          <Button variant="secondary" onPress={() => setOpen(request)}>
            {t('privacy.myRequests.open')}
          </Button>
        </View>
      ))}
    </Screen>
  );
}

function RequestPage({
  tablet,
  request,
  onBack,
}: {
  tablet: boolean;
  request: MyRightsRequest;
  onBack: () => void;
}) {
  const { t, format } = useI18n();
  const fulfilledAccess = request.type === 'access' && request.outcomeCode === 'fulfilled';
  const summary = useMyAccessSummary(fulfilledAccess ? request.id : null);
  return (
    <Screen tablet={tablet}>
      <Text variant="h1" accessibilityRole="header">
        {t('privacy.myRequests.detail', { number: request.number })}
      </Text>
      <Button variant="secondary" onPress={onBack}>
        {t('common.action.back')}
      </Button>
      <Text>{request.description}</Text>
      {request.closedAt ? null : (
        <Text muted>
          {request.overdue
            ? t('privacy.rights.overdue', { date: format.date(request.dueAt) })
            : t('privacy.rights.due', { date: format.date(request.dueAt) })}
        </Text>
      )}
      {request.closedAt ? (
        <>
          <Text variant="h2">{t('privacy.rights.response')}</Text>
          <Text>{request.response ?? t('privacy.rights.noResponse')}</Text>
        </>
      ) : null}
      {summary.data ? (
        <>
          <Text variant="h2">{t('privacy.rights.summary.title')}</Text>
          {(summary.data.activities ?? []).map((activity) => (
            <Text key={activity.activityCode}>
              {enumLabel(t, 'privacyPurpose', activity.purposeCode)} ·{' '}
              {enumLabel(t, 'legalBasis', activity.legalBasis)}
            </Text>
          ))}
        </>
      ) : null}
    </Screen>
  );
}
