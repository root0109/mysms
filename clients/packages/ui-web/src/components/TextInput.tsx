import { useId, type ComponentPropsWithRef } from 'react';

export interface TextInputProps extends Omit<ComponentPropsWithRef<'input'>, 'id'> {
  label: string;
  hint?: string;
  /** Field error from validation or the server's problem+json `errors[]`. */
  error?: string | undefined;
  id?: string;
}

export function TextInput({ label, hint, error, id, className, ...rest }: TextInputProps) {
  const autoId = useId();
  const inputId = id ?? autoId;
  const hintId = hint ? `${inputId}-hint` : undefined;
  const errorId = error ? `${inputId}-error` : undefined;
  const describedBy = [hintId, errorId].filter(Boolean).join(' ') || undefined;
  return (
    <div
      className={['sms-field', error ? 'sms-field--invalid' : '', className ?? ''].filter(Boolean).join(' ')}
    >
      <label className="sms-field__label" htmlFor={inputId}>
        {label}
      </label>
      <input
        {...rest}
        id={inputId}
        className="sms-field__input"
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
      />
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
