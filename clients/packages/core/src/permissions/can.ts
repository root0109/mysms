import type { PermissionCode } from '../generated/permissions';

type Grants = ReadonlySet<string> | readonly string[] | null | undefined;

const has = (grants: Grants, code: string): boolean =>
  grants
    ? Array.isArray(grants)
      ? grants.includes(code)
      : (grants as ReadonlySet<string>).has(code)
    : false;

/** Checks `Me.permissions` (ui/05 §5). */
export const can = (grants: Grants, code: PermissionCode): boolean => has(grants, code);
export const canAny = (grants: Grants, codes: readonly PermissionCode[]): boolean =>
  codes.some((c) => has(grants, c));
export const canAll = (grants: Grants, codes: readonly PermissionCode[]): boolean =>
  codes.every((c) => has(grants, c));
