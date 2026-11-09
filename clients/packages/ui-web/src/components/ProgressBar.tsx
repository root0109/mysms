export interface ProgressBarProps {
  label: string;
  /** 0–100; undefined renders an indeterminate bar. */
  value?: number | undefined;
}

export function ProgressBar({ label, value }: ProgressBarProps) {
  const clamped = value === undefined ? undefined : Math.max(0, Math.min(100, Math.round(value)));
  return (
    <div
      className="sms-progress"
      role="progressbar"
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={clamped}
    >
      <span
        className="sms-progress__bar"
        style={clamped === undefined ? undefined : { inlineSize: `${clamped}%` }}
      />
    </div>
  );
}
