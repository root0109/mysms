import type { iam, privacy } from '@mysms/sdk';
import { SELF_SERVICE } from './data';
import { json, problem, type Handler } from './fakeApi';
import { onePage } from './paging';

const id = (suffix: string) => `01960000-0000-7000-8000-${suffix.padStart(12, '0')}`;
const HOUR = 3_600_000;
const DAY = 24 * HOUR;

/**
 * The school's privacy governance registers: two open breaches (one just reported with twelve hours left on the
 * Board's clock, one contained and past it), an active legal hold due for review and a released one, two retention
 * policies the school has not set yet and their last runs.
 */
export const governanceFixture = {
  reportedBreachId: id('9a1'),
  containedBreachId: id('9a2'),
  activeHoldId: id('9b1'),
  releasedHoldId: id('9b2'),
  heldPersonId: id('9b3'),
  attendancePolicyId: id('9c1'),
  admissionPolicyId: id('9c2'),
  officerUserId: id('9d1'),
} as const;

/** What the school's data protection officer holds for the breach register, holds and retention (ADM-20 to ADM-22). */
export const PRIVACY_GOVERNANCE = [
  'privacy.breach.read',
  'privacy.breach.report',
  'privacy.breach.update',
  'privacy.breach.triage',
  'privacy.breach.contain',
  'privacy.breach.record_notification',
  'privacy.breach.close',
  'privacy.breach.mark_not_a_breach',
  'privacy.legal_hold.read',
  'privacy.legal_hold.place',
  'privacy.legal_hold.update',
  'privacy.legal_hold.release',
  'privacy.retention_policy.read',
  'privacy.retention_policy.update',
  'privacy.retention_run.read',
] as const;

/** The school's data protection officer. */
export const privacyOfficer: iam.Me = {
  user: {
    id: governanceFixture.officerUserId,
    username: 'dpo.meera',
    email: 'dpo@school.example',
    locale: 'en-IN',
    timezone: 'Asia/Kolkata',
    termsAcceptedAt: '2026-06-01T08:00:00Z',
  },
  permissions: [...SELF_SERVICE, ...PRIVACY_GOVERNANCE],
  roles: [{ roleCode: 'privacy_officer', scopeType: 'tenant' }],
};

type Stored<T> = { -readonly [K in keyof T]: T[K] } & { id: string; rowVersion: number };
const fieldProblem = (field: string, message: string) =>
  problem(422, 'VALIDATION_FAILED', 'Validation failed', { errors: [{ field, message }] });
const stateProblem = (detail: string) => problem(409, 'INVALID_STATE_TRANSITION', 'Conflict', { detail });
const stale = (req: Request, row: { rowVersion: number }) =>
  req.headers.get('If-Match') !== `"${row.rowVersion}"`;
const iso = (time: number) => new Date(time).toISOString();
const today = () => new Date().toISOString().slice(0, 10);

/**
 * The breach register, legal holds and retention settings in memory, with the server's rules for the slices ADM-20
 * to ADM-22 use: a breach's Board clock (72 hours from awareness) and overdue are derived at read time; each step
 * leaves only the states the design allows (409) under the breach's etag (412); notices move the status as the
 * server does; an open breach is edited under If-Match, a closed one never (409); a hold names exactly the targets
 * of its scope (422 on the field), is reviewed today or later (422) and changes no more once released (409); a
 * retention period never goes below the policy's floor (422), saved under the row's ETag ("0" while never set).
 */
export function fakePrivacyGovernance(): Record<string, Handler> {
  const f = governanceFixture;
  const now = Date.now();
  let sequence = 2;
  const breaches = new Map<string, Stored<privacy.Breach>>([
    [
      f.reportedBreachId,
      {
        id: f.reportedBreachId,
        number: 'PRB-2026-000001',
        status: 'reported',
        detectedAt: iso(now - 61 * HOUR),
        becameAwareAt: iso(now - 60 * HOUR),
        boardReportDueAt: iso(now + 12 * HOUR),
        description: 'A teacher e-mailed the class marks list to the wrong parents group.',
        categories: ['assessment'],
        modules: ['assessment'],
        estimatedPrincipals: 38,
        notifications: [],
        rowVersion: 1,
      },
    ],
    [
      f.containedBreachId,
      {
        id: f.containedBreachId,
        number: 'PRB-2026-000002',
        status: 'contained',
        detectedAt: iso(now - 81 * HOUR),
        becameAwareAt: iso(now - 80 * HOUR),
        boardReportDueAt: iso(now - 8 * HOUR),
        description: 'A lost laptop held a copy of the admission register.',
        categories: ['contact'],
        modules: ['admission'],
        notifications: [],
        rowVersion: 3,
      },
    ],
  ]);
  /** The breach as the server shows it now: the Board clock derived, until the detailed report or not a breach. */
  const breachOut = ({ rowVersion, ...row }: Stored<privacy.Breach>): privacy.Breach => {
    const shown: privacy.Breach = { ...row, etag: `"${rowVersion}"` };
    if (row.boardReportSentAt || row.status === 'not_a_breach') return { ...shown, overdue: false };
    const left = Date.parse(row.boardReportDueAt ?? '') - Date.now();
    return { ...shown, overdue: left < 0, hoursRemaining: Math.floor(left / HOUR) };
  };
  /** One step on a breach: its etag (412), the states it leaves (409), then the change. */
  const breachStep =
    (
      allowed: (row: Stored<privacy.Breach>) => boolean,
      change: (row: Stored<privacy.Breach>, body: Record<string, unknown>) => Response | void,
    ): Handler =>
    async (req, params) => {
      const row = breaches.get(params.breachId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      const body = (await req.json()) as Record<string, unknown>;
      if (body.etag !== undefined && body.etag !== `"${row.rowVersion}"`)
        return problem(412, 'PRECONDITION_FAILED');
      if (!allowed(row)) return stateProblem('The breach has moved on.');
      const refused = change(row, body);
      if (refused) return refused;
      row.rowVersion += 1;
      return json(breachOut(row));
    };
  const intimated = (status: string | undefined) =>
    status === 'board_initial_sent' || status === 'board_report_sent' || status === 'principals_notified';

  const holds = new Map<string, Stored<privacy.LegalHold>>([
    [
      f.activeHoldId,
      {
        id: f.activeHoldId,
        reasonCode: 'litigation',
        scope: 'subject',
        subjectPersonId: f.heldPersonId,
        reviewBy: iso(now - DAY).slice(0, 10),
        status: 'active',
        placedByUserId: f.officerUserId,
        placedAt: iso(now - 90 * DAY),
        rowVersion: 1,
      },
    ],
    [
      f.releasedHoldId,
      {
        id: f.releasedHoldId,
        reasonCode: 'audit',
        scope: 'category',
        categoryCode: 'finance',
        reviewBy: iso(now - 30 * DAY).slice(0, 10),
        status: 'released',
        placedByUserId: f.officerUserId,
        placedAt: iso(now - 200 * DAY),
        releasedAt: iso(now - 31 * DAY),
        releasedByUserId: f.officerUserId,
        rowVersion: 2,
      },
    ],
  ]);
  const holdOut = ({ rowVersion, ...row }: Stored<privacy.LegalHold>): privacy.LegalHold => ({
    ...row,
    reviewDue: !row.releasedAt && row.reviewBy <= today(),
    etag: `"${rowVersion}"`,
  });
  /** The targets a hold's scope requires, and those it does not allow (the subject is allowed with any scope). */
  const targetError = (hold: privacy.LegalHold): Response | undefined => {
    const needs: Record<string, (keyof privacy.LegalHold)[]> = {
      subject: ['subjectPersonId'],
      category: ['categoryCode'],
      records: ['module', 'entityType', 'entityIds'],
    };
    const required = needs[hold.scope] ?? [];
    for (const field of required)
      if (!hold[field]) return fieldProblem(field, 'is required for this scope of hold');
    for (const field of ['categoryCode', 'module', 'entityType', 'entityIds'] as const)
      if (hold[field] !== undefined && !required.includes(field))
        return fieldProblem(field, 'does not apply to this scope of hold');
    return undefined;
  };

  const policies = new Map<string, Stored<privacy.RetentionPolicy>>(
    (
      [
        [
          f.attendancePolicyId,
          'attendance_records',
          'attendance',
          'attendance',
          'after_leaving',
          1095,
          1095,
          'archive',
        ],
        [
          f.admissionPolicyId,
          'unsuccessful_applications',
          'admission',
          'admission',
          'after_record_date',
          365,
          180,
          'delete',
        ],
      ] as const
    ).map(([policyId, code, categoryCode, module, trigger, periodDays, minimumPeriodDays, action]) => [
      policyId,
      {
        id: policyId,
        code,
        categoryCode,
        module,
        trigger,
        periodDays,
        minimumPeriodDays,
        action,
        notifyBeforeDays: 30,
        overridden: false,
        rowVersion: 0,
      },
    ]),
  );
  const policyOut = ({ rowVersion, ...row }: Stored<privacy.RetentionPolicy>): privacy.RetentionPolicy => ({
    ...row,
    etag: `"${rowVersion}"`,
  });
  const runs: privacy.RetentionRun[] = [
    {
      id: id('9e1'),
      policyCode: 'unsuccessful_applications',
      categoryCode: 'admission',
      module: 'admission',
      action: 'delete',
      cutoff: iso(now - 366 * DAY),
      status: 'partial',
      affected: 12,
      held: 1,
      startedAt: iso(now - DAY),
      completedAt: iso(now - DAY + HOUR),
    },
    {
      id: id('9e2'),
      policyCode: 'attendance_records',
      categoryCode: 'attendance',
      module: 'attendance',
      action: 'archive',
      cutoff: iso(now - 1096 * DAY),
      status: 'done',
      affected: 0,
      held: 0,
      startedAt: iso(now - 2 * DAY),
      completedAt: iso(now - 2 * DAY + HOUR),
    },
  ];

  return {
    'GET /v1/privacy/breaches': (req) => {
      const filter = new URL(req.url).searchParams.get('filter') ?? '';
      const open = filter.includes('closed');
      const all = [...breaches.values()]
        .filter((b) => !open || !b.closedAt)
        .sort((a, b) => Date.parse(a.boardReportDueAt ?? '') - Date.parse(b.boardReportDueAt ?? ''));
      return onePage(req, all.map(breachOut));
    },
    'GET /v1/privacy/breaches/{breachId}': (_req, params) => {
      const row = breaches.get(params.breachId ?? '');
      return row ? json(breachOut(row)) : problem(404, 'NOT_FOUND');
    },
    'POST /v1/privacy/breaches': async (req) => {
      const body = (await req.json()) as privacy.Breach;
      if (!body.description?.trim()) return fieldProblem('description', 'must not be blank');
      const detected = Date.parse(body.detectedAt);
      const aware = Date.parse(body.becameAwareAt);
      if (Number.isNaN(detected) || detected > Date.now())
        return fieldProblem('detectedAt', 'must not be in the future');
      if (Number.isNaN(aware) || aware > Date.now())
        return fieldProblem('becameAwareAt', 'must not be in the future');
      if (aware < detected)
        return fieldProblem('becameAwareAt', 'must not be earlier than when the breach was detected');
      sequence += 1;
      const row: Stored<privacy.Breach> = {
        ...body,
        id: id(`9a${sequence}`),
        number: `PRB-2026-${String(sequence).padStart(6, '0')}`,
        status: 'reported',
        boardReportDueAt: iso(aware + 72 * HOUR),
        notifications: [],
        rowVersion: 1,
      };
      breaches.set(row.id, row);
      return json(breachOut(row), { status: 201 });
    },
    'PATCH /v1/privacy/breaches/{breachId}': async (req, params) => {
      const row = breaches.get(params.breachId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      if (stale(req, row)) return problem(412, 'PRECONDITION_FAILED');
      if (row.closedAt) return stateProblem('A closed breach cannot be changed.');
      const body = (await req.json()) as Record<string, unknown>;
      const mask = (new URL(req.url).searchParams.get('updateMask') ?? '').split(',').filter(Boolean);
      const target = row as unknown as Record<string, unknown>;
      for (const field of mask) target[field] = body[field];
      row.rowVersion += 1;
      return json(breachOut(row));
    },
    'POST /v1/privacy/breaches/{breachId}:triage': breachStep(
      (b) => b.status === 'reported',
      (b) => {
        b.status = 'triaged';
      },
    ),
    'POST /v1/privacy/breaches/{breachId}:contain': breachStep(
      (b) => b.status === 'triaged',
      (b) => {
        b.status = 'contained';
      },
    ),
    'POST /v1/privacy/breaches/{breachId}:markNotABreach': breachStep(
      (b) => b.status === 'triaged',
      (b, body) => {
        if (!String(body.reason ?? '').trim()) return fieldProblem('reason', 'must not be blank');
        b.status = 'not_a_breach';
        b.notABreachReason = body.reason as string;
        b.closedAt = iso(Date.now());
      },
    ),
    'POST /v1/privacy/breaches/{breachId}:close': breachStep(
      (b) => b.status === 'principals_notified' && Boolean(b.boardReportSentAt),
      (b) => {
        b.status = 'closed';
        b.closedAt = iso(Date.now());
      },
    ),
    'POST /v1/privacy/breaches/{breachId}:recordNotification': breachStep(
      (b) => !b.closedAt,
      (b, body) => {
        const audience = body.audience as privacy.BreachNotificationAudienceEnum;
        const kind = body.kind as privacy.BreachNotificationKindEnum;
        const sentAt = Date.parse(body.sentAt as string);
        if (Number.isNaN(sentAt) || sentAt > Date.now())
          return fieldProblem('sentAt', 'must not be in the future');
        if (audience === 'board' && kind === 'initial') {
          if (b.status !== 'contained')
            return stateProblem("The Board's initial intimation is recorded once, for a contained breach.");
          b.status = 'board_initial_sent';
        } else if (audience === 'board') {
          if (!intimated(b.status)) return stateProblem("The Board's report follows its initial intimation.");
          if (kind === 'detailed') {
            if (b.boardReportSentAt) return stateProblem("The Board's detailed report is already recorded.");
            b.boardReportSentAt = iso(sentAt);
            if (b.status === 'board_initial_sent') b.status = 'board_report_sent';
          }
        } else if (audience === 'principals') {
          if (!intimated(b.status))
            return stateProblem('The principals are told once the Board has been intimated.');
          b.status = 'principals_notified';
        }
        sequence += 1;
        b.notifications = [
          ...(b.notifications ?? []),
          { id: id(`9f${sequence}`), audience, kind, sentAt: iso(sentAt) },
        ];
      },
    ),

    'GET /v1/privacy/legal-holds': (req) => {
      const active = (new URL(req.url).searchParams.get('filter') ?? '').includes('active = true');
      const all = [...holds.values()]
        .filter((h) => !active || !h.releasedAt)
        .sort((a, b) => a.reviewBy.localeCompare(b.reviewBy));
      return onePage(req, all.map(holdOut));
    },
    'GET /v1/privacy/legal-holds/{legalHoldId}': (_req, params) => {
      const row = holds.get(params.legalHoldId ?? '');
      return row ? json(holdOut(row)) : problem(404, 'NOT_FOUND');
    },
    'POST /v1/privacy/legal-holds': async (req) => {
      const body = (await req.json()) as privacy.LegalHold;
      const refused = targetError(body);
      if (refused) return refused;
      if (!body.reviewBy || body.reviewBy < today())
        return fieldProblem('reviewBy', 'must be today or later');
      sequence += 1;
      const row: Stored<privacy.LegalHold> = {
        ...body,
        id: id(`9b${sequence + 3}`),
        status: 'active',
        placedByUserId: f.officerUserId,
        placedAt: iso(Date.now()),
        rowVersion: 1,
      };
      holds.set(row.id, row);
      return json(holdOut(row), { status: 201 });
    },
    'PATCH /v1/privacy/legal-holds/{legalHoldId}': async (req, params) => {
      const row = holds.get(params.legalHoldId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      if (stale(req, row)) return problem(412, 'PRECONDITION_FAILED');
      if (row.releasedAt) return stateProblem('A released hold changes no more.');
      const body = (await req.json()) as privacy.LegalHoldUpdate;
      if (!body.reviewBy || body.reviewBy < today())
        return fieldProblem('reviewBy', 'must be today or later');
      row.reviewBy = body.reviewBy;
      row.rowVersion += 1;
      return json(holdOut(row));
    },
    'POST /v1/privacy/legal-holds/{legalHoldId}:release': async (req, params) => {
      const row = holds.get(params.legalHoldId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      const body = (await req.json()) as privacy.ReleaseLegalHoldRequest;
      if (body.etag !== `"${row.rowVersion}"`) return problem(412, 'PRECONDITION_FAILED');
      if (row.releasedAt) return stateProblem('The hold is already released.');
      row.status = 'released';
      row.releasedAt = iso(Date.now());
      row.releasedByUserId = f.officerUserId;
      row.rowVersion += 1;
      return json(holdOut(row));
    },

    'GET /v1/privacy/retention-policies': (req) => onePage(req, [...policies.values()].map(policyOut)),
    'GET /v1/privacy/retention-policies/{retentionPolicyId}': (_req, params) => {
      const row = policies.get(params.retentionPolicyId ?? '');
      return row ? json(policyOut(row)) : problem(404, 'NOT_FOUND');
    },
    'PATCH /v1/privacy/retention-policies/{retentionPolicyId}': async (req, params) => {
      const row = policies.get(params.retentionPolicyId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      if (stale(req, row)) return problem(412, 'PRECONDITION_FAILED');
      const body = (await req.json()) as privacy.RetentionPolicyUpdate;
      const mask = (new URL(req.url).searchParams.get('updateMask') ?? '').split(',');
      const periodDays = mask.includes('periodDays') ? body.periodDays : row.periodDays;
      if (periodDays === undefined || periodDays < (row.minimumPeriodDays ?? 0))
        return fieldProblem('periodDays', "must not be shorter than the policy's legal floor");
      row.periodDays = periodDays;
      if (mask.includes('action') && body.action) row.action = body.action;
      row.overridden = true;
      row.rowVersion += 1;
      return json(policyOut(row));
    },
    'GET /v1/privacy/retention-runs': (req) => onePage(req, runs),
  };
}
