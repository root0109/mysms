/**
 * A tiny in-memory API for tests: routes are "METHOD /path" with `{param}` placeholders; every handled
 * request is recorded so tests can assert on headers, bodies and order.
 */
export type Handler = (req: Request, params: Record<string, string>) => Response | Promise<Response>;

export const json = (body: unknown, init: ResponseInit = {}): Response =>
  new Response(JSON.stringify(body), {
    status: init.status ?? 200,
    headers: { 'Content-Type': 'application/json', ...(init.headers as Record<string, string> | undefined) },
  });

export const problem = (
  status: number,
  code: string,
  title = 'Error',
  extra: Record<string, unknown> = {},
): Response =>
  new Response(JSON.stringify({ type: 'about:blank', title, status, code, ...extra }), {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  });

export function fakeApi(routes: Record<string, Handler>) {
  const calls: { method: string; path: string; search: string; headers: Headers; body: string }[] = [];
  const compiled = Object.entries(routes).map(([key, handler]) => {
    const [method, pattern] = key.split(' ') as [string, string];
    const names: string[] = [];
    const regex = new RegExp(
      '^' +
        pattern.replace(/[.*+?^$()|[\]\\]/g, '\\$&').replace(/\\?\{(\w+)\\?\}/g, (_m, n: string) => {
          names.push(n);
          return '([^/]+)';
        }) +
        '$',
    );
    return { method, regex, names, handler };
  });
  const fetch = async (request: Request): Promise<Response> => {
    const url = new URL(request.url);
    const path = decodeURIComponent(url.pathname);
    calls.push({
      method: request.method,
      path,
      search: url.search,
      headers: new Headers(request.headers),
      body: await request.clone().text(),
    });
    for (const r of compiled) {
      if (r.method !== request.method) continue;
      const m = r.regex.exec(path);
      if (!m) continue;
      const params: Record<string, string> = {};
      r.names.forEach((n, i) => (params[n] = m[i + 1] ?? ''));
      return r.handler(request, params);
    }
    return problem(404, 'NOT_FOUND', `no fake route for ${request.method} ${path}`);
  };
  return { fetch, calls };
}
