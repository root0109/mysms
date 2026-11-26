/**
 * A row with a PATCH body applied (JAVA-CODING-RULES §7.2), as the server does it: a field the body names is set,
 * one sent as null is cleared, and every field the body leaves out is kept.
 */
export function patched<T extends object>(row: T, changes: object): T {
  const out = { ...row } as Record<string, unknown>;
  for (const [field, value] of Object.entries(changes)) {
    if (value === null) delete out[field];
    else out[field] = value;
  }
  return out as T;
}
