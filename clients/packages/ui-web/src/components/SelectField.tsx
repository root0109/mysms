import { useId, type ComponentPropsWithRef } from 'react';

export interface SelectOption {
  value: string;
  label: string;
}

export interface SelectFieldProps extends Omit<ComponentPropsWithRef<'select'>, 'id' | 'children'> {
  label: string;
  options: readonly SelectOption[];
  hint?: string;
  error?: string | undefined;
  id?: string;
}

/** A native select with a visible label, hint and error, styled like TextInput (ui/04 §5). */
export function SelectField({ label, options, hint, error, id, className, ...rest }: SelectFieldProps) {
  const autoId = useId();
  const selectId = id ?? autoId;
  const hintId = hint ? `${selectId}-hint` : undefined;
  const errorId = error ? `${selectId}-error` : undefined;
  const describedBy = [hintId, errorId].filter(Boolean).join(' ') || undefined;
  return (
    <div
      className={['sms-field', error ? 'sms-field--invalid' : '', className ?? ''].filter(Boolean).join(' ')}
    >
      <label className="sms-field__label" htmlFor={selectId}>
        {label}
      </label>
      <select
        {...rest}
        id={selectId}
        className="sms-field__input sms-field__select"
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
      >
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      {hint ? (
        <p id={hintId} className="sms-field__hint">
          {hint}
        </p>
      ) : null}
      {error ? (
        <p id={errorId} className="sms-field__error" role="alert">
          {error}
        </p>
      ) : null}
    </div>
  );
}
