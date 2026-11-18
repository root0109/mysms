/**
 * Screens reachable without a session. ACC-05 is among them because the code in a verification e-mail confirms the
 * address without signing in (iam.ConfirmVerification is public); signed in, the same screen also sends a code. The
 * course catalogue (LMS-01, LMS-02) is read as a guest of the school this deployment names.
 */
export const PUBLIC_SCREENS: ReadonlySet<string> = new Set([
  'ACC-01',
  'ACC-02',
  'ACC-03',
  'ACC-05',
  'LMS-01',
  'LMS-02',
]);

/** `/sis/students/{studentId}` → `/app/sis/students/:studentId` (a literal `:verb` mid-segment stays literal). */
export const toRouterPath = (route: string): string =>
  '/app' + route.replace(/\{([A-Za-z][A-Za-z0-9]*)\}/g, ':$1');
