import type { RecordRow } from '@mysms/core';

/** An institution and the institutions under it. */
export interface Branch {
  row: RecordRow;
  children: Branch[];
}

/**
 * The institutions as a tree by their parent: a row whose parent the caller cannot see (out of their scope) is a
 * root of its own, so the tree shows exactly the rows the server listed, each once. Siblings are in name order.
 */
export function institutionTree(rows: readonly RecordRow[]): Branch[] {
  const ids = new Set(rows.map((row) => row.id));
  const byParent = new Map<string, RecordRow[]>();
  for (const row of rows) {
    const parent = typeof row.parentInstitutionId === 'string' ? row.parentInstitutionId : '';
    const key = ids.has(parent) && parent !== row.id ? parent : '';
    byParent.set(key, [...(byParent.get(key) ?? []), row]);
  }
  const placed = new Set<string>();
  const grow = (parent: string): Branch[] =>
    (byParent.get(parent) ?? [])
      .filter((row) => !placed.has(row.id))
      .sort((a, b) => String(a.name ?? '').localeCompare(String(b.name ?? '')))
      .map((row) => {
        placed.add(row.id);
        return { row, children: grow(row.id) };
      });
  const roots = grow('');
  // rows whose parents form a loop are never reached from a root: each is shown as a root rather than lost
  for (const row of rows) {
    if (placed.has(row.id)) continue;
    placed.add(row.id);
    roots.push({ row, children: grow(row.id) });
  }
  return roots;
}
