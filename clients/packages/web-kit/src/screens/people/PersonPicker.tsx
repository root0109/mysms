import { useI18n, usePersonNames, usePersonsNamed } from '@mysms/core';
import { Button, TextInput } from '@mysms/ui-web';
import { useState } from 'react';

/**
 * Finds a person by name (people.ListPersons, two letters at least) and hands the chosen one to the page; the
 * person's record is never shown here, only their name.
 */
export function PersonPicker({ onPick }: { onPick: (personId: string, name: string) => void }) {
  const { t } = useI18n();
  const [search, setSearch] = useState('');
  const found = usePersonsNamed(search);
  const names = usePersonNames(found.data ?? []);
  return (
    <div className="form">
      <TextInput
        label={t('people.picker.search')}
        type="search"
        value={search}
        hint={t('people.picker.hint')}
        onChange={(event) => setSearch(event.target.value)}
      />
      {found.isSuccess && found.data.length === 0 ? <p className="hint">{t('people.picker.none')}</p> : null}
      {found.isError ? <p className="hint">{t('common.error.generic')}</p> : null}
      <ul className="chip-list" aria-label={t('people.picker.results')}>
        {(found.data ?? []).map((personId) => {
          const name = names.get(personId) ?? t('people.picker.unnamed');
          return (
            <li key={personId}>
              <Button variant="secondary" onClick={() => onPick(personId, name)}>
                {name}
              </Button>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
