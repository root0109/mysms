import { useId, type ComponentPropsWithRef } from 'react';

export interface TextAreaProps extends Omit<ComponentPropsWithRef<'textarea'>, 'id'> {
  label: string;
  hint?: string;
  error?: string | undefined;
  id?: string;
}

/** Multi-line text with a visible label, hint and error (ui/04 §5). */
export function TextArea({ label, hint, error, id, className, rows = 4, ...rest }: TextAreaProps) {
  const autoId = useId();
  const areaId = id ?? autoId;
  const hintId = hint ? `${areaId}-hint` : undefined;
  const errorId = error ? `${areaId}-error` : undefined;
  const describedBy = [hintId, errorId].filter(Boolean).join(' ') || undefined;
  return (
    <div
      className={['sms-field', error ? 'sms-field--invalid' : '', className ?? ''].filter(Boolean).join(' ')}
    >
      <label className="sms-field__label" htmlFor={areaId}>
        {label}
      </label>
      <textarea
        {...rest}
        rows={rows}
        id={areaId}
        className="sms-field__input sms-field__textarea"
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
