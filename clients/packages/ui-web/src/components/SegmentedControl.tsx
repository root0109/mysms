import { useId } from 'react';
import type { Intent } from '@mysms/tokens';

export interface SegmentedOption<Value extends string> {
  value: Value;
  label: string;
  /** Colours the segment when it is the chosen one; the label always carries the meaning (ui/04 §6). */
  intent?: Intent;
}

export interface SegmentedControlProps<Value extends string> {
  /** Accessible name of the group, e.g. the student a row of marks belongs to. */
  label: string;
  options: readonly SegmentedOption<Value>[];
  /** The chosen value; undefined while nothing is chosen yet. */
  value: Value | undefined;
  onChange: (value: Value) => void;
  disabled?: boolean;
}

/**
 * One choice out of a few, all visible at once and chosen with a single tap (ui/04 §5): a group of native radio
 * buttons drawn as adjoining segments, so arrow keys, focus and form semantics are the browser's own.
 */
export function SegmentedControl<Value extends string>({
  label,
  options,
  value,
  onChange,
  disabled,
}: SegmentedControlProps<Value>) {
  const name = useId();
  return (
    <div className="sms-segmented" role="radiogroup" aria-label={label}>
      {options.map((option) => (
        <label
          key={option.value}
          className={[
            'sms-segmented__option',
            option.value === value
              ? `sms-segmented__option--on sms-segmented__option--${option.intent ?? 'neutral'}`
              : '',
          ]
            .filter(Boolean)
            .join(' ')}
        >
          <input
            type="radio"
            className="sms-segmented__input"
            name={name}
            value={option.value}
            checked={option.value === value}
            disabled={disabled}
            onChange={() => onChange(option.value)}
          />
          <span>{option.label}</span>
        </label>
      ))}
    </div>
  );
}
