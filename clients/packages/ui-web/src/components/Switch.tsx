import { useId } from 'react';

export interface SwitchProps {
  label: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
  hint?: string | undefined;
  /** Shown and announced as the field's error. */
  error?: string | undefined;
  disabled?: boolean;
  id?: string;
}

/** An on/off setting: a button with role="switch" and aria-checked, operable by keyboard (ui/04 §6). */
export function Switch({ label, checked, onChange, hint, error, disabled, id }: SwitchProps) {
  const autoId = useId();
  const switchId = id ?? autoId;
  const hintId = hint ? `${switchId}-hint` : undefined;
  const errorId = error ? `${switchId}-error` : undefined;
  const describedBy = [hintId, errorId].filter(Boolean).join(' ') || undefined;
  return (
    <div className="sms-switch-field">
      <button
        id={switchId}
        type="button"
        role="switch"
        aria-checked={checked}
        aria-describedby={describedBy}
        aria-invalid={error ? true : undefined}
        disabled={disabled}
        className={`sms-switch${checked ? ' sms-switch--on' : ''}`}
        onClick={() => onChange(!checked)}
      >
        <span className="sms-switch__thumb" aria-hidden="true" />
      </button>
      <label className="sms-field__label" htmlFor={switchId}>
        {label}
      </label>
      {hint ? (
        <p id={hintId} className="sms-field__hint sms-switch-field__hint">
          {hint}
        </p>
      ) : null}
      {error ? (
        <p id={errorId} className="sms-field__error sms-switch-field__hint" role="alert">
          {error}
        </p>
      ) : null}
    </div>
  );
}
