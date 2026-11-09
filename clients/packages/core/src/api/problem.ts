/** A field-level validation error from problem+json `errors[]`. */
export interface FieldError {
  field: string;
  message: string;
  code?: string;
}

interface ProblemBody {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  code?: string;
  traceId?: string;
  errors?: FieldError[];
}

const isRecord = (v: unknown): v is Record<string, unknown> => typeof v === 'object' && v !== null;

/** Seconds from a `Retry-After` header (delta-seconds or HTTP date); undefined when absent or unparsable. */
export function parseRetryAfter(value: string | null, now: number = Date.now()): number | undefined {
  if (!value) return undefined;
  const trimmed = value.trim();
  if (/^\d+$/.test(trimmed)) return Number(trimmed);
  const at = Date.parse(trimmed);
  if (Number.isNaN(at)) return undefined;
  return Math.max(0, Math.ceil((at - now) / 1000));
}

/**
 * Every failed API call surfaces as an ApiError built from the RFC 9457 problem body (ui/05 §6).
 * `traceId` is always logged with the error so support can find the server-side trace.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly title: string;
  readonly detail: string | undefined;
  readonly errors: readonly FieldError[];
  readonly traceId: string | undefined;
  readonly retryAfterSeconds: number | undefined;

  constructor(init: {
    status: number;
    code: string;
    title: string;
    detail?: string | undefined;
    errors?: readonly FieldError[];
    traceId?: string | undefined;
    retryAfterSeconds?: number | undefined;
  }) {
    super(init.detail ? `${init.title}: ${init.detail}` : init.title);
    this.name = 'ApiError';
    this.status = init.status;
    this.code = init.code;
    this.title = init.title;
    this.detail = init.detail;
    this.errors = init.errors ?? [];
    this.traceId = init.traceId;
    this.retryAfterSeconds = init.retryAfterSeconds;
  }

  /** Builds the error from a response and its parsed problem body (undefined when the body is not JSON). */
  static fromResponse(response: Response, body: unknown): ApiError {
    const p: ProblemBody = isRecord(body) ? (body as ProblemBody) : {};
    const errors = Array.isArray(p.errors)
      ? p.errors.filter(
          (e): e is FieldError => isRecord(e) && typeof e.field === 'string' && typeof e.message === 'string',
        )
      : [];
    return new ApiError({
      status: typeof p.status === 'number' ? p.status : response.status,
      code: typeof p.code === 'string' && p.code ? p.code : `HTTP_${response.status}`,
      title:
        typeof p.title === 'string' && p.title ? p.title : response.statusText || `HTTP ${response.status}`,
      detail: typeof p.detail === 'string' ? p.detail : undefined,
      errors,
      traceId: typeof p.traceId === 'string' ? p.traceId : (response.headers.get('X-Trace-Id') ?? undefined),
      retryAfterSeconds: parseRetryAfter(response.headers.get('Retry-After')),
    });
  }

  /** 412 (stale If-Match) and 409 (state conflict) are both handled by the conflict dialog (ui/05 §6). */
  get isConflict(): boolean {
    return this.status === 412 || this.status === 409;
  }

  /** The API hides invisible rows as 404; screens render 403 and 404 the same way (ui/05 §6). */
  get isNotAvailable(): boolean {
    return this.status === 403 || this.status === 404;
  }

  /** Field errors keyed by field name, for mapping onto form inputs. */
  fieldErrors(): Record<string, string> {
    const out: Record<string, string> = {};
    for (const e of this.errors) if (!(e.field in out)) out[e.field] = e.message;
    return out;
  }
}

export const isApiError = (e: unknown): e is ApiError => e instanceof ApiError;
