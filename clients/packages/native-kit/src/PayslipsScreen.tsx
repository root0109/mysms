import {
  moneyText,
  useCan,
  useCurrencyCode,
  useI18n,
  useInstitutions,
  usePayslipLines,
  usePayslips,
  useSession,
  type Payslip,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Banner, Button, Screen, Text, useTheme } from '@mysms/ui-native';
import { useState } from 'react';
import { View } from 'react-native';

const NO_STAFF: readonly string[] = [];
const STATUS: Readonly<Record<string, MessageKey>> = {
  unpaid: 'pay.payslip.status.unpaid',
  paid: 'pay.payslip.status.paid',
  cancelled: 'pay.payslip.status.cancelled',
};
const TYPE: Readonly<Record<string, MessageKey>> = {
  earning: 'pay.component.type.earning',
  deduction: 'pay.component.type.deduction',
  defined: 'pay.component.type.defined',
};

/**
 * HR-08 on the teacher's phone and tablet: the signed-in staff member's own payslips (iam.GetMe names their staff
 * records), newest month first, and one payslip with its lines and the totals the server worked out. The PDF is not
 * offered because no served operation renders it. Amounts use the kits' money formatting in the school's currency
 * when the role may read it, else plain decimals.
 */
export function PayslipsScreen({ tablet = false, onBack }: { tablet?: boolean; onBack: () => void }) {
  const { t, format, formatLocale } = useI18n();
  const theme = useTheme();
  const mayRead = useCan('payroll.payslip.read');
  const mayLines = useCan('payroll.payslip_line.read');
  const mayInstitutions = useCan('org.institution.read');
  const staffIds = useSession((s) => s.me?.staffIds ?? NO_STAFF);
  const payslips = usePayslips(mayRead ? { staffIds } : null);
  const [openId, setOpenId] = useState<string | null>(null);
  const rows = payslips.data?.items ?? [];
  const opened = rows.find((payslip) => payslip.id === openId) ?? rows[0];
  const lines = usePayslipLines(mayLines && opened ? (opened.id ?? null) : null);
  const institutions = useInstitutions(mayInstitutions && Boolean(opened));
  const currencyId = institutions.data?.items.find((item) => item.id === opened?.institutionId)?.currencyId;
  const currency = useCurrencyCode(currencyId);
  const money = (amount: string | undefined) => moneyText(format, formatLocale, amount, currency);
  const period = (payslip: Payslip) =>
    t('pay.payslip.periodRange', {
      from: format.day(payslip.periodStart),
      to: format.day(payslip.periodEnd),
    });
  const status = (payslip: Payslip) =>
    STATUS[payslip.status ?? ''] ? t(STATUS[payslip.status ?? ''] as MessageKey) : '';
  const section = { marginTop: theme.space[7] };
  const item = { marginTop: theme.space[4] };

  return (
    <Screen tablet={tablet}>
      <Text variant="h1" accessibilityRole="header">
        {t('screen.HR08.title')}
      </Text>
      {!mayRead ? (
        <Banner>{t('pay.mine.noAccess')}</Banner>
      ) : staffIds.length === 0 ? (
        <Banner>{t('pay.mine.notStaff')}</Banner>
      ) : payslips.isError ? (
        <>
          <Banner intent="danger">{t('common.error.generic')}</Banner>
          <Button variant="secondary" onPress={() => void payslips.refetch()}>
            {t('common.action.retry')}
          </Button>
        </>
      ) : payslips.isPending ? (
        <Text muted>{t('common.state.loading')}</Text>
      ) : rows.length === 0 ? (
        <Text muted>{t('pay.mine.none')}</Text>
      ) : (
        <View style={tablet ? { flexDirection: 'row', gap: theme.space[7] } : undefined}>
          <View style={[section, tablet ? { flex: 1 } : null]}>
            {rows.map((payslip) => (
              <View key={payslip.id} style={item}>
                <Button
                  variant={payslip.id === opened?.id ? 'primary' : 'secondary'}
                  onPress={() => setOpenId(payslip.id ?? null)}
                >
                  {`${period(payslip)} · ${status(payslip)} · ${money(payslip.netAmount)}`}
                </Button>
              </View>
            ))}
          </View>
          {opened ? (
            <View style={[section, tablet ? { flex: 1 } : null]}>
              <Text variant="h2" accessibilityRole="header">
                {t('pay.mine.detail', { number: opened.payslipNumber })}
              </Text>
              <Text muted>{`${period(opened)} · ${status(opened)}`}</Text>
              {!mayLines ? (
                <Text muted>{t('pay.payslip.noLineAccess')}</Text>
              ) : lines.isPending ? (
                <Text muted>{t('common.state.loading')}</Text>
              ) : lines.isError ? (
                <Banner intent="danger">{t('common.error.generic')}</Banner>
              ) : (
                (lines.data?.items ?? []).map((line) => (
                  <View key={line.id} style={item}>
                    <Text style={{ fontWeight: '600' }}>
                      {line.componentName ?? line.componentCode ?? ''}
                    </Text>
                    <Text muted>
                      {`${TYPE[line.componentType ?? ''] ? t(TYPE[line.componentType ?? ''] as MessageKey) : ''} · ${money(line.amount)}`}
                    </Text>
                  </View>
                ))
              )}
              <View style={section}>
                <Text>{`${t('pay.payslip.gross')}: ${money(opened.grossAmount)}`}</Text>
                <Text>{`${t('pay.payslip.deductions')}: ${money(opened.deductionTotal)}`}</Text>
                <Text
                  style={{ fontWeight: '600' }}
                >{`${t('pay.payslip.net')}: ${money(opened.netAmount)}`}</Text>
              </View>
              <Text muted>{t('pay.mine.noPdf')}</Text>
            </View>
          ) : null}
        </View>
      )}
      <View style={section}>
        <Button variant="secondary" onPress={onBack}>
          {t('common.action.back')}
        </Button>
      </View>
    </Screen>
  );
}
