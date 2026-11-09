/** Only same-app paths may be a post-sign-in destination (no open redirects). */
export function safeNext(next: string | null | undefined, fallback = '/app'): string {
  if (
    !next ||
    !next.startsWith('/app') ||
    next.startsWith('//') ||
    next.includes('\\') ||
    next.startsWith('/app/auth/')
  )
    return fallback;
  return next;
}
