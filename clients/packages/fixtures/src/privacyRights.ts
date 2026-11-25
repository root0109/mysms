import type { iam, privacy } from '@mysms/sdk';
import { SELF_SERVICE } from './data';
import { json, problem, type Handler } from './fakeApi';
import { onePage } from './paging';

const id = (suffix: string) => `01960000-0000-7000-8000-${suffix.padStart(12, '0')}`;

/**
 * Rights requests of one school: Ravi, an adult student (the signed-in requester), and the school's privacy officer
 * (the staff queue). Ravi has one access request in progress and one grievance closed with the school's answer; the
 * queue also holds a correction a guardian submitted, due tomorrow, and the guardian's requests about their child: an
 * erasure not yet started, a withdraw-all, and an export waiting for action after its sis task failed five times.
 */
export const rightsFixture = {
  raviPersonId: id('8a1'),
  guardianPersonId: id('8a2'),
  childPersonId: id('8a3'),
  accessId: id('8b1'),
  grievanceId: id('8b2'),
  correctionId: id('8b3'),
  erasureId: id('8b4'),
  withdrawAllId: id('8b5'),
  exportId: id('8b6'),
  failedTaskId: id('8f3'),
  officerUserId: id('8c1'),
  grievanceAnswer: 'We have changed how photos are shared with parents.',
  now: '2026-11-24T06:00:00Z',
} as const;

/** What every signed-in person holds to use their own requests and nominee (iam baseline: applies to all roles). */
export const RIGHTS_SELF_SERVICE = [
  'privacy.my_request.create',
  'privacy.my_request.read',
  'privacy.my_nominee.read',
  'privacy.my_nominee.set',
  'privacy.my_nominee.revoke',
] as const;

/** What the school's privacy officer holds for the queue (ADM-19), tenant-wide. */
export const RIGHTS_STAFF = [
  'privacy.rights_request.read',
  'privacy.rights_request.verify_identity',
  'privacy.rights_request.assign',
  'privacy.rights_request.extend',
  'privacy.rights_request.reject',
  'privacy.rights_request.close',
  'privacy.rights_request.fulfil',
  'privacy.rights_request.retry_task',
] as const;

/** Ravi, an adult student: his own requests and nominee. */
export const rightsRequester: iam.Me = {
  user: {
    id: id('8d1'),
    personId: rightsFixture.raviPersonId,
    username: 'ravi.k',
    email: 'ravi@student.example',
    locale: 'en-IN',
    timezone: 'Asia/Kolkata',
    termsAcceptedAt: '2026-06-01T08:00:00Z',
  },
  permissions: [...SELF_SERVICE, ...RIGHTS_SELF_SERVICE],
  roles: [{ roleCode: 'student', scopeType: 'tenant' }],
};

/** The school's privacy officer. */
export const rightsOfficer: iam.Me = {
  user: {
    id: rightsFixture.officerUserId,
    username: 'dpo.anita',
    email: 'dpo@school.example',
    locale: 'en-IN',
    timezone: 'Asia/Kolkata',
    termsAcceptedAt: '2026-06-01T08:00:00Z',
  },
  permissions: [...SELF_SERVICE, ...RIGHTS_STAFF],
  roles: [{ roleCode: 'privacy_officer', scopeType: 'tenant' }],
};

type Row = privacy.RightsRequest & { rowVersion: number };

const SUMMARY: privacy.AccessSummary = {
  subjectPersonId: rightsFixture.raviPersonId,
  subjectStatus: 'adult',
  subjectRoles: ['student'],
  generatedAt: '2026-11-24T06:00:00Z',
  activities: [
    {
      activityCode: 'comms.operational_notice',
      module: 'comms',
      purposeCode: 'operational_communication',
      legalBasis: 'legitimate_use',
      dataCategories: ['contact'],
      recipients: [],
      crossBorder: false,
    },
  ],
};

/**
 * The rights requests and the nominee, in memory, with the server's rules for the slices ACC-15 and ADM-19 use: a
 * correction names its record and a grievance its category (422 on the field otherwise); only the requester's own
 * requests are theirs (404); each staff step leaves only the states the design allows (409), under the request's etag
 * (412); a grievance and a declined correction are answered (422 on response); an access request is fulfilled with
 * its summary, never closed as fulfilled; the nominee is named, replaced and revoked under its etag.
 */
export function fakeRightsRequests(): Record<string, Handler> {
  const f = rightsFixture;
  let sequence = 6;
  const due = (days: number) => new Date(Date.parse(f.now) + days * 86_400_000).toISOString();
  const rows = new Map<string, Row>([
    [
      f.accessId,
      {
        id: f.accessId,
        number: 'PRV-2026-000001',
        type: 'access',
        subjectPersonId: f.raviPersonId,
        requesterPersonId: f.raviPersonId,
        requesterCapacity: 'self',
        status: 'in_progress',
        identityCheck: 'signed_in_session',
        description: 'What does the school hold about me?',
        assigneeUserId: f.officerUserId,
        submittedAt: due(-20),
        dueAt: due(10),
        overdue: false,
        rowVersion: 2,
      },
    ],
    [
      f.grievanceId,
      {
        id: f.grievanceId,
        number: 'PRV-2026-000002',
        type: 'grievance',
        grievanceCategory: 'consent',
        subjectPersonId: f.raviPersonId,
        requesterPersonId: f.raviPersonId,
        requesterCapacity: 'self',
        status: 'closed',
        identityCheck: 'signed_in_session',
        description: 'My photo was shared without asking.',
        submittedAt: due(-40),
        dueAt: due(-10),
        overdue: false,
        closedAt: due(-12),
        outcomeCode: 'fulfilled',
        outcomeNote: 'Spoke to the class teacher.',
        response: f.grievanceAnswer,
        rowVersion: 3,
      },
    ],
    [
      f.correctionId,
      {
        id: f.correctionId,
        number: 'PRV-2026-000003',
        type: 'correction',
        correction: {
          module: 'sis',
          recordType: 'student',
          fields: [{ field: 'date of birth', requestedValue: '2012-04-09' }],
        },
        subjectPersonId: f.childPersonId,
        requesterPersonId: f.guardianPersonId,
        requesterCapacity: 'guardian',
        status: 'verified',
        identityCheck: 'signed_in_session',
        description: "My daughter's date of birth is wrong.",
        submittedAt: due(-29),
        dueAt: due(1),
        overdue: false,
        rowVersion: 1,
      },
    ],
    ...(
      [
        [f.erasureId, 4, 'erasure', 'in_progress', 20],
        [f.withdrawAllId, 5, 'withdraw_all', 'in_progress', 22],
        [f.exportId, 6, 'export', 'action_required', 25],
      ] as const
    ).map(([requestId, n, type, status, days]): [string, Row] => [
      requestId,
      {
        id: requestId,
        number: `PRV-2026-${String(n).padStart(6, '0')}`,
        type,
        subjectPersonId: f.childPersonId,
        requesterPersonId: f.guardianPersonId,
        requesterCapacity: 'guardian',
        status,
        identityCheck: 'signed_in_session',
        description: 'Please act on what the school holds about my daughter.',
        assigneeUserId: f.officerUserId,
        submittedAt: due(-5),
        dueAt: due(days),
        overdue: false,
        rowVersion: 2,
        ...(type === 'export'
          ? {
              tasks: (
                [
                  [id('8f1'), 'iam', 'done', 1, undefined],
                  [id('8f2'), 'people', 'done', 1, undefined],
                  [f.failedTaskId, 'sis', 'failed', 5, 'STORAGE_DOWN'],
                ] as const
              ).map(([taskId, module, taskStatus, attempts, lastErrorCode]) => ({
                id: taskId,
                rightsRequestId: requestId,
                module,
                action: 'export' as const,
                wave: 1,
                status: taskStatus,
                attempts,
                dispatchedAt: due(-4),
                completedAt: due(-3),
                ...(lastErrorCode ? { lastErrorCode } : {}),
              })),
            }
          : {}),
      },
    ]),
  ]);
  let nominee: (privacy.MyNominee & { rowVersion: number }) | null = null;
  const out = ({ rowVersion, ...row }: Row): privacy.RightsRequest => ({ ...row, etag: `"${rowVersion}"` });
  /** The requester's view: never the assignee, the identity check, the extension reason, the staff's note. */
  const mine = (row: Row): privacy.MyRightsRequest => {
    const shown: Record<string, unknown> = { ...out(row) };
    for (const hidden of [
      'requesterPersonId',
      'identityCheck',
      'assigneeUserId',
      'extendedReason',
      'outcomeNote',
      'etag',
    ])
      delete shown[hidden];
    return shown as unknown as privacy.MyRightsRequest;
  };
  const field = (name: string) =>
    problem(422, 'VALIDATION_FAILED', 'Validation failed', { errors: [{ field: name, message: 'invalid' }] });
  const ownOf = (requestId: string | undefined) => {
    const row = requestId ? rows.get(requestId) : undefined;
    return row && row.requesterPersonId === f.raviPersonId ? row : undefined;
  };
  /** One staff step: the states it leaves, then the change; 404, 409 and 412 as the server answers them. */
  const step =
    (
      allowed: readonly string[],
      change: (row: Row, body: Record<string, unknown>, params: Record<string, string>) => Response | void,
    ): Handler =>
    async (req, params) => {
      const row = rows.get(params.rightsRequestId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      const body = (await req.json()) as Record<string, unknown>;
      if (body.etag !== undefined && body.etag !== `"${row.rowVersion}"`)
        return problem(412, 'PRECONDITION_FAILED');
      if (!allowed.includes(row.status as string)) return problem(409, 'INVALID_STATE_TRANSITION');
      const refused = change(row, body, params);
      if (refused) return refused;
      row.rowVersion += 1;
      return json(out(row));
    };
  return {
    'GET /v1/privacy/my/requests': (req) =>
      onePage(
        req,
        [...rows.values()]
          .filter((r) => r.requesterPersonId === f.raviPersonId)
          .reverse()
          .map(mine),
      ),
    'GET /v1/privacy/my/requests/{rightsRequestId}': (_req, params) => {
      const row = ownOf(params.rightsRequestId);
      return row ? json(mine(row)) : problem(404, 'NOT_FOUND');
    },
    'GET /v1/privacy/my/requests/{rightsRequestId}/access-summary': (_req, params) => {
      const row = ownOf(params.rightsRequestId);
      return row?.type === 'access' && row.outcomeCode === 'fulfilled'
        ? json(SUMMARY)
        : problem(404, 'NOT_FOUND');
    },
    'POST /v1/privacy/my/requests': async (req) => {
      const body = (await req.json()) as privacy.SubmitMyRequestRequest;
      if (body.subjectPersonId !== f.raviPersonId) return problem(404, 'NOT_FOUND');
      if ((body.type === 'correction') !== (body.correction !== undefined)) return field('correction');
      if ((body.type === 'grievance') !== (body.grievanceCategory !== undefined))
        return field('grievanceCategory');
      sequence += 1;
      const row: Row = {
        id: id(`8b${sequence}`),
        number: `PRV-2026-${String(sequence).padStart(6, '0')}`,
        type: body.type,
        subjectPersonId: body.subjectPersonId,
        requesterPersonId: f.raviPersonId,
        requesterCapacity: 'self',
        status: 'verified',
        identityCheck: 'signed_in_session',
        description: body.description,
        submittedAt: f.now,
        dueAt: due(30),
        overdue: false,
        rowVersion: 1,
        ...(body.correction ? { correction: body.correction } : {}),
        ...(body.grievanceCategory ? { grievanceCategory: body.grievanceCategory } : {}),
      };
      rows.set(row.id as string, row);
      return json(mine(row), { status: 201 });
    },
    'GET /v1/privacy/rights-requests': (req) => {
      const filter = new URL(req.url).searchParams.get('filter') ?? '';
      const open = filter.includes('closed');
      const all = [...rows.values()]
        .filter((r) => !open || (r.status !== 'closed' && r.status !== 'rejected'))
        .sort((a, b) => Date.parse(a.dueAt ?? '') - Date.parse(b.dueAt ?? ''));
      return onePage(req, all.map(out));
    },
    'GET /v1/privacy/rights-requests/{rightsRequestId}/access-summary': (_req, params) => {
      const row = rows.get(params.rightsRequestId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      return row.type === 'access' ? json(SUMMARY) : problem(409, 'INVALID_STATE_TRANSITION');
    },
    'POST /v1/privacy/rights-requests/{rightsRequestId}:verifyIdentity': step(['submitted'], (row, body) => {
      row.status = 'verified';
      row.identityCheck = body.identityCheck as Row['identityCheck'];
    }),
    'POST /v1/privacy/rights-requests/{rightsRequestId}:assign': step(
      ['verified', 'in_progress'],
      (row, body) => {
        row.status = 'in_progress';
        row.assigneeUserId = body.assigneeUserId as string;
      },
    ),
    'POST /v1/privacy/rights-requests/{rightsRequestId}:extend': step(
      ['submitted', 'verified', 'in_progress', 'action_required'],
      (row, body) => {
        const later = new Date(body.dueAt as string);
        if (later.getTime() <= Date.parse(row.dueAt ?? '')) return field('dueAt');
        row.dueAt = later.toISOString();
        row.extendedReason = body.reason as string;
      },
    ),
    'POST /v1/privacy/rights-requests/{rightsRequestId}:reject': step(
      ['submitted', 'verified'],
      (row, body) => {
        row.status = 'rejected';
        row.outcomeCode = body.reasonCode as Row['outcomeCode'];
        row.closedAt = f.now;
      },
    ),
    'POST /v1/privacy/rights-requests/{rightsRequestId}:close': step(['in_progress'], (row, body) => {
      const outcome = body.outcomeCode as Row['outcomeCode'];
      if (row.tasks?.length) return problem(409, 'INVALID_STATE_TRANSITION');
      if (row.type === 'access' && outcome !== 'declined') return problem(409, 'INVALID_STATE_TRANSITION');
      const owed = row.type === 'grievance' || (row.type === 'correction' && outcome === 'declined');
      if (owed && !body.response) return field('response');
      row.status = 'closed';
      row.outcomeCode = outcome;
      row.closedAt = f.now;
      if (body.note) row.outcomeNote = body.note as string;
      if (body.response) row.response = body.response as string;
    }),
    'POST /v1/privacy/rights-requests/{rightsRequestId}:fulfilAccess': step(['in_progress'], (row) => {
      if (row.type !== 'access') return problem(409, 'INVALID_STATE_TRANSITION');
      row.status = 'closed';
      row.outcomeCode = 'fulfilled';
      row.closedAt = f.now;
    }),
    'POST /v1/privacy/rights-requests/{rightsRequestId}:startFulfilment': step(['in_progress'], (row) => {
      if ((row.type !== 'erasure' && row.type !== 'export') || row.tasks?.length)
        return problem(409, 'INVALID_STATE_TRANSITION');
      const action = row.type === 'erasure' ? 'erase' : 'export';
      row.tasks = (['iam', 'people', 'sis'] as const).map((module, index) => {
        const first = module === 'iam' || action === 'export';
        return {
          id: id(`8f${sequence + index + 4}`),
          rightsRequestId: row.id,
          module,
          action,
          wave: first ? 1 : 2,
          status: first ? 'dispatched' : 'pending',
          attempts: first ? 1 : 0,
          ...(first ? { dispatchedAt: f.now } : {}),
        };
      });
    }),
    'POST /v1/privacy/rights-requests/{rightsRequestId}:fulfilWithdrawAll': step(['in_progress'], (row) => {
      if (row.type !== 'withdraw_all') return problem(409, 'INVALID_STATE_TRANSITION');
      row.status = 'closed';
      row.outcomeCode = 'fulfilled';
      row.closedAt = f.now;
    }),
    'POST /v1/privacy/rights-requests/{rightsRequestId}/tasks/{taskId}:retry': step(
      ['in_progress', 'action_required'],
      (row, _body, params) => {
        const task = row.tasks?.find((t) => t.id === params.taskId);
        if (!task) return problem(404, 'NOT_FOUND');
        if (task.status !== 'failed' && task.status !== 'dispatched')
          return problem(409, 'INVALID_STATE_TRANSITION');
        task.status = 'dispatched';
        task.attempts = (task.attempts ?? 0) + 1;
        task.dispatchedAt = f.now;
        delete task.completedAt;
        row.status = 'in_progress';
      },
    ),
    'GET /v1/privacy/my/nominee': () => {
      if (!nominee) return problem(404, 'NOT_FOUND');
      const { rowVersion, ...shown } = nominee;
      return json({ ...shown, etag: `"${rowVersion}"` });
    },
    'POST /v1/privacy/my/nominee:set': async (req) => {
      const body = (await req.json()) as privacy.SetMyNomineeRequest;
      if (nominee && body.etag !== undefined && body.etag !== `"${nominee.rowVersion}"`)
        return problem(412, 'PRECONDITION_FAILED');
      const mask = (value: string) => (value.length <= 8 ? '••••' : `••••${value.slice(-4)}`);
      sequence += 1;
      nominee = {
        id: id(`8e${sequence}`),
        nomineeNameMasked: mask(body.nomineeName),
        nomineeContactMasked: mask(body.nomineeContact),
        createdAt: f.now,
        etag: '"1"',
        rowVersion: 1,
      };
      const { rowVersion, ...shown } = nominee;
      return json({ ...shown, etag: `"${rowVersion}"` });
    },
    'POST /v1/privacy/my/nominee:revoke': async (req) => {
      const body = (await req.json()) as privacy.RevokeMyNomineeRequest;
      if (!nominee) return problem(404, 'NOT_FOUND');
      if (body.etag !== `"${nominee.rowVersion}"`) return problem(412, 'PRECONDITION_FAILED');
      nominee = null;
      return new Response(null, { status: 204 });
    },
  };
}
