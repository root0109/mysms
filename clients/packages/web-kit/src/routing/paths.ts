/** Screens reachable without a session. */
export const PUBLIC_SCREENS: ReadonlySet<string> = new Set(['ACC-01', 'ACC-02', 'ACC-03']);

/** `/sis/students/{studentId}` → `/app/sis/students/:studentId` (a literal `:verb` mid-segment stays literal). */
export const toRouterPath = (route: string): string =>
  '/app' + route.replace(/\{([A-Za-z][A-Za-z0-9]*)\}/g, ':$1');
