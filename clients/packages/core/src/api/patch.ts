/**
 * A PATCH body (JAVA-CODING-RULES §7.2): the fields the change names are sent and the rest are left out, so the
 * server keeps them; a named field without a value (undefined) is sent as null, which clears it.
 */
export function patchOf<T extends object>(changes: T): T {
  return Object.fromEntries(
    Object.entries(changes).map(([key, value]) => [key, value === undefined ? null : value]),
  ) as T;
}
