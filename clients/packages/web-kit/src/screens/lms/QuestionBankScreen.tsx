import {
  lmsFailureText,
  RESOURCES,
  useCan,
  useDeleteRecord,
  useI18n,
  useImportQuestions,
  useQuestionSearch,
  type LmsQuestion,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Banner, Button, Card, DataTable, Dialog, SelectField, TextInput } from '@mysms/ui-web';
import { useState } from 'react';
import { UUID } from '../ids';

const TYPES: ReadonlyArray<{ value: string; label: MessageKey }> = [
  { value: 'choices', label: 'lms.bank.type.choices' },
  { value: 'user_input', label: 'lms.bank.type.userInput' },
  { value: 'open_ended', label: 'lms.bank.type.openEnded' },
  { value: 'true_false', label: 'lms.bank.type.trueFalse' },
  { value: 'matching', label: 'lms.bank.type.matching' },
  { value: 'fill_blank', label: 'lms.bank.type.fillBlank' },
  { value: 'numeric', label: 'lms.bank.type.numeric' },
  { value: 'hotspot', label: 'lms.bank.type.hotspot' },
  { value: 'pattern', label: 'lms.bank.type.pattern' },
];

const QUESTION_DEF = RESOURCES.find((resource) => resource.key === 'lms.Question');

/**
 * LMS-43 Question bank: the tenant's bank searched by text and type, questions retired through their standard
 * delete, and GIFT / CSV files imported through a file already in the file store. The per-type editors live on the
 * generic record form of lms.Question.
 */
export function QuestionBankScreen() {
  const { t } = useI18n();
  const mayRead = useCan('lms.question.search_question_bank');
  const mayImport = useCan('lms.question.import_questions');
  const mayDelete = useCan('lms.question.delete');
  const [query, setQuery] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [importing, setImporting] = useState(false);
  const search = useQuestionSearch(mayRead ? query : '', typeFilter ? [typeFilter] : [], null);
  const remove = useDeleteRecord(QUESTION_DEF as NonNullable<typeof QUESTION_DEF>);
  const title = t('screen.LMS43.title');
  if (!mayRead)
    return (
      <Card title={title}>
        <p className="hint">{t('lms.common.forbidden')}</p>
      </Card>
    );
  return (
    <Card title={title}>
      <div role="search">
        <TextInput
          label={t('lms.bank.search')}
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />
        <SelectField
          label={t('lms.bank.type')}
          value={typeFilter}
          onChange={(event) => setTypeFilter(event.target.value)}
          options={[
            { value: '', label: t('lms.bank.allTypes') },
            ...TYPES.map((type) => ({ value: type.value, label: t(type.label) })),
          ]}
        />
        {mayImport && (
          <Button variant="secondary" onClick={() => setImporting(true)}>
            {t('lms.bank.import')}
          </Button>
        )}
      </div>
      {search.isError && <Banner intent="danger">{t('common.error.generic')}</Banner>}
      {remove.isError && <Banner intent="danger">{lmsFailureText(t, remove.error)}</Banner>}
      <DataTable
        caption={title}
        columns={[
          {
            key: 'question',
            header: t('lms.bank.question'),
            render: (row: LmsQuestion) => row.questionText ?? '',
          },
          {
            key: 'type',
            header: t('lms.bank.type'),
            render: (row: LmsQuestion) => row.questionType ?? '',
          },
          { key: 'marks', header: t('lms.bank.marks'), render: (row: LmsQuestion) => row.marks ?? '' },
          {
            key: 'actions',
            header: t('lms.bank.actions'),
            render: (row: LmsQuestion) =>
              mayDelete && row.id && row.etag ? (
                <Button
                  variant="secondary"
                  onClick={() => remove.mutate({ id: row.id as string, etag: row.etag as string })}
                  disabled={remove.isPending}
                >
                  {t('common.action.delete')}
                </Button>
              ) : null,
          },
        ]}
        rows={search.data?.data}
        rowKey={(row) => row.id ?? ''}
        loading={search.isPending}
        loadingLabel={t('common.state.loading')}
        emptyText={t('lms.bank.empty')}
      />
      <ImportDialog open={importing} onClose={() => setImporting(false)} />
    </Card>
  );
}

/** lms.ImportQuestions: names an uploaded GIFT or CSV file; the import runs as a 202 operation. */
function ImportDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { t } = useI18n();
  const importQuestions = useImportQuestions();
  const [fileId, setFileId] = useState('');
  const [format, setFormat] = useState<'gift' | 'csv'>('gift');
  return (
    <Dialog open={open} title={t('lms.bank.import')} onClose={onClose}>
      <p className="hint">{t('lms.bank.importHint')}</p>
      <TextInput
        label={t('lms.bank.fileId')}
        value={fileId}
        onChange={(event) => setFileId(event.target.value)}
        error={fileId !== '' && !UUID.test(fileId) ? t('common.validation.pattern') : undefined}
      />
      <SelectField
        label={t('lms.bank.format')}
        value={format}
        onChange={(event) => setFormat(event.target.value === 'csv' ? 'csv' : 'gift')}
        options={[
          { value: 'gift', label: t('lms.bank.format.gift') },
          { value: 'csv', label: t('lms.bank.format.csv') },
        ]}
      />
      {importQuestions.isError && <Banner intent="danger">{lmsFailureText(t, importQuestions.error)}</Banner>}
      {importQuestions.isSuccess && <Banner intent="success">{t('lms.bank.importStarted')}</Banner>}
      <Button
        onClick={() => importQuestions.mutate({ fileId, format })}
        disabled={importQuestions.isPending || !UUID.test(fileId)}
      >
        {t('lms.bank.importStart')}
      </Button>
    </Dialog>
  );
}
