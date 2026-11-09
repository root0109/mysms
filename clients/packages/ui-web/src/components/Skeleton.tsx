export interface SkeletonProps {
  /** Accessible name of the loading region, e.g. the translated "Loading…". */
  label: string;
  lines?: number;
}

/** Loading state: a skeleton matching the final layout, never a content spinner (ui/02 §1.4). */
export function Skeleton({ label, lines = 3 }: SkeletonProps) {
  return (
    <div className="sms-skeleton" role="status" aria-label={label} aria-busy="true">
      {Array.from({ length: lines }, (_, i) => (
        <span key={i} className="sms-skeleton__line" />
      ))}
    </div>
  );
}
