import { useI18n, type MyStudent } from '@mysms/core';
import { ChoiceGroup } from '@mysms/ui-native';

/**
 * Which of the account's students a "my …" view is about (P: one of the children). Shown only when there is a
 * choice; a student reading their own view sees nothing.
 */
export function StudentChoice({
  students,
  value,
  onChange,
}: {
  students: readonly MyStudent[];
  value: string;
  onChange: (studentId: string) => void;
}) {
  const { t } = useI18n();
  if (students.length < 2) return null;
  return (
    <ChoiceGroup
      label={t('native.whose.label')}
      choices={students.map((student) => ({
        value: student.id,
        label: student.name ?? t('aca.class.unnamed'),
      }))}
      value={value}
      onChange={onChange}
    />
  );
}
