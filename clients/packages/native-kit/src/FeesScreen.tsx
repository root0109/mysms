import {
  isPayable,
  moneyText,
  useCan,
  useCurrencyCode,
  useFamilyStudents,
  useFeeStatement,
  useI18n,
  type FamilyStudent,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Banner, Button, Screen, Text, useTheme } from '@mysms/ui-native';
import { useState } from 'react';
import { Linking, View } from 'react-native';

const STATUS: Readonly<Record<string, MessageKey>> = {
  draft: 'fin.status.draft',
  invoiced: 'fin.status.invoiced',
  posted: 'fin.status.posted',
  partially_paid: 'fin.status.partiallyPaid',
  paid: 'fin.status.paid',
  cancelled: 'fin.status.cancelled',
  initiated: 'fin.status.initiated',
  received: 'fin.status.received',
  failed: 'fin.status.failed',
  refunded: 'fin.status.refunded',
};

/**
 * FIN-08 Fee statement and FIN-07 Pay fees on the parent's and the student's phone: the fees of the student or of a
 * chosen child — owed, paid and outstanding, the invoices and payments — as the server worked them out (sis confirms
 * the family relation). Paying opens the web's pay screen (FIN-07): the gateway's payment window needs its native SDK,
 * which the apps do not carry yet, so the phone hands the payment to the browser and shows it here once the gateway's
 * notification has recorded it.
 */
export function FeesScreen({
  webOrigin,
  tablet = false,
  onBack,
}: {
  webOrigin: string;
  tablet?: boolean;
  onBack: () => void;
}) {
  const { t, format, formatLocale } = useI18n();
  const theme = useTheme();
  const mayRead = useCan('finance.invoice.get_fee_statement');
  const mayPay = useCan('finance.payment.start_checkout');
  const family = useFamilyStudents();
  const [chosen, setChosen] = useState('');
  const studentId = chosen || family.students[0]?.id || '';
  const statement = useFeeStatement(mayRead && studentId ? studentId : null);
  const code = useCurrencyCode(statement.data?.currencyId);
  const money = (amount: string | undefined) => moneyText(format, formatLocale, amount, code);
  const status = (value: string | undefined) =>
    STATUS[value ?? ''] ? t(STATUS[value ?? ''] as MessageKey) : '';
  const label = (student: FamilyStudent) =>
    student.self ? t('fin.family.me') : (student.name ?? student.id);
  const section = { marginTop: theme.space[7] };
  const item = { marginTop: theme.space[4] };
  const open = (statement.data?.invoices ?? []).filter(isPayable);

  return (
    <Screen tablet={tablet}>
      <Text variant="h1" accessibilityRole="header">
        {t('screen.FIN08.title')}
      </Text>
      {!mayRead ? (
        <Banner>{t('fin.common.noAccess')}</Banner>
      ) : family.isPending ? (
        <Text muted>{t('common.state.loading')}</Text>
      ) : family.students.length === 0 ? (
        <Banner>{t('fin.family.none')}</Banner>
      ) : (
        <>
          {family.students.length > 1 ? (
            <View style={section}>
              {family.students.map((student) => (
                <View key={student.id} style={item}>
                  <Button
                    variant={student.id === studentId ? 'primary' : 'secondary'}
                    onPress={() => setChosen(student.id)}
                  >
                    {label(student)}
                  </Button>
                </View>
              ))}
            </View>
          ) : null}
          {statement.isPending ? (
            <Text muted>{t('common.state.loading')}</Text>
          ) : statement.isError ? (
            <>
              <Banner intent="danger">{t('common.error.generic')}</Banner>
              <Button variant="secondary" onPress={() => void statement.refetch()}>
                {t('common.action.retry')}
              </Button>
            </>
          ) : (
            <View style={section}>
              <Text>{`${t('fin.statement.totalDue')}: ${money(statement.data.totalDue)}`}</Text>
              <Text>{`${t('fin.statement.totalPaid')}: ${money(statement.data.totalPaid)}`}</Text>
              <Text style={{ fontWeight: '600' }}>
                {`${t('fin.statement.balance')}: ${money(statement.data.balance)}`}
              </Text>
              <Text variant="h2" accessibilityRole="header" style={section}>
                {t('fin.statement.invoices')}
              </Text>
              {(statement.data.invoices ?? []).length === 0 ? (
                <Text muted>{t('fin.invoice.none')}</Text>
              ) : null}
              {(statement.data.invoices ?? []).map((invoice) => (
                <View key={invoice.id} style={item}>
                  <Text style={{ fontWeight: '600' }}>{invoice.invoiceNumber}</Text>
                  <Text muted>
                    {`${format.day(invoice.invoiceDate)} · ${money(invoice.total)} · ${status(invoice.status)}`}
                  </Text>
                </View>
              ))}
              <Text variant="h2" accessibilityRole="header" style={section}>
                {t('fin.statement.payments')}
              </Text>
              {(statement.data.payments ?? []).length === 0 ? (
                <Text muted>{t('fin.payment.none')}</Text>
              ) : null}
              {(statement.data.payments ?? []).map((payment) => (
                <View key={payment.id} style={item}>
                  <Text>{`${money(payment.amount)} · ${status(payment.status)}`}</Text>
                  <Text muted>{payment.receiptNumber ?? payment.gatewayPaymentId ?? ''}</Text>
                </View>
              ))}
              {mayPay && open.length > 0 ? (
                <View style={section}>
                  <Text muted>{t('fin.native.payInBrowser')}</Text>
                  <Button onPress={() => void Linking.openURL(`${webOrigin}/app/finance/pay`)}>
                    {t('fin.native.pay')}
                  </Button>
                </View>
              ) : null}
            </View>
          )}
        </>
      )}
      <View style={section}>
        <Button variant="secondary" onPress={onBack}>
          {t('common.action.back')}
        </Button>
      </View>
    </Screen>
  );
}
