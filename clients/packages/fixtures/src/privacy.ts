import type { iam, privacy } from '@mysms/sdk';
import { SELF_SERVICE } from './data';
import { json, problem, type Handler } from './fakeApi';
import { onePage } from './paging';

const id = (suffix: string) => `01960000-0000-7000-8000-${suffix.padStart(12, '0')}`;

/**
 * A family's consent centre: a parent (the signed-in account) is the legal guardian of two children, Asha and Kabir;
 * the school asks about photos and videos of events (a width purpose, the school allowing up to the whole school),
 * promotional messages (one decision for the whole school) and optional WhatsApp messages (switched on, but its
 * notice is not published yet). Asha, 15, can also sign in herself and comes of age on 4 March 2029.
 */
export const privacyFixture = {
  parentPersonId: id('7a1'),
  ashaPersonId: id('7a2'),
  kabirPersonId: id('7a3'),
  ashaStudentId: id('7b2'),
  kabirStudentId: id('7b3'),
  noticeCode: 'family_notice',
  noticeVersionId: id('7c1'),
  ashaComesOfAge: '2029-03-04',
  photos: 'photo_video_publication',
  marketing: 'marketing_comms',
  whatsapp: 'sms_whatsapp_messages',
  english: 'We publish photos and videos of school events on the school website and in the school app.',
  hindi: 'हम विद्यालय के कार्यक्रमों की फ़ोटो और वीडियो विद्यालय की वेबसाइट और ऐप पर प्रकाशित करते हैं।',
  forChildren: 'The school may show photos of you at school events. Your parents decide if that is okay.',
} as const;

/** What every signed-in person holds to use the consent centre (iam baseline: applies to all roles). */
export const CONSENT_SELF_SERVICE = [
  'privacy.my_consent.read',
  'privacy.my_consent.create',
  'privacy.my_consent.withdraw',
] as const;

/** The parent: their own self-service, the consent centre and their children's cards. */
export const consentParent: iam.Me = {
  user: {
    id: id('7d1'),
    personId: privacyFixture.parentPersonId,
    username: 'parent.meena',
    email: 'meena@family.example',
    locale: 'en-IN',
    timezone: 'Asia/Kolkata',
    termsAcceptedAt: '2026-06-01T08:00:00Z',
  },
  permissions: [...SELF_SERVICE, ...CONSENT_SELF_SERVICE, 'sis.guardian.list_my_children'],
  roles: [{ roleCode: 'guardian', scopeType: 'tenant' }],
};

/** Asha herself, a student under 18. */
export const consentMinorStudent: iam.Me = {
  user: {
    id: id('7d2'),
    personId: privacyFixture.ashaPersonId,
    username: 'asha.r',
    email: 'asha@student.example',
    locale: 'en-IN',
    timezone: 'Asia/Kolkata',
    termsAcceptedAt: '2026-06-01T08:00:00Z',
  },
  permissions: [...SELF_SERVICE, ...CONSENT_SELF_SERVICE],
  roles: [{ roleCode: 'student', scopeType: 'tenant' }],
};

type Scope = privacy.MyConsentAllowedScopesEnum;
/** A live decision of the signed-in account. */
interface Decision {
  id: string;
  subject: string;
  purpose: string;
  scope: Scope;
  decision: 'granted' | 'refused';
  decidedAt: string;
  version: number;
}

const PURPOSES: readonly { code: string; allowedScopes: Scope[]; notice: boolean }[] = [
  { code: privacyFixture.photos, allowedScopes: ['class', 'institution'], notice: true },
  { code: privacyFixture.marketing, allowedScopes: ['tenant'], notice: true },
  { code: privacyFixture.whatsapp, allowedScopes: ['tenant'], notice: false },
];

/**
 * The consent centre of privacy, in memory, with the server's rules for the slice ACC-13 uses: the rows a person
 * sees (their children as guardian, or themself read only as a minor), a decision only for a child of theirs under
 * the active notice version at a width the purpose takes (404 / 422), a newer decision replacing the live one, and
 * withdrawal of one's own decision under its body etag (412). The effect is the parent's own (the only guardian).
 * The notice is served in English and Hindi, English for any other language.
 */
export function fakePrivacy(options: { as?: 'parent' | 'minor' } = {}): Record<string, Handler> {
  const f = privacyFixture;
  const minor = options.as === 'minor';
  const decisions = new Map<string, Decision>();
  let sequence = 0;
  const key = (subject: string, purpose: string) => `${subject}|${purpose}`;
  const row = (subject: string, purpose: (typeof PURPOSES)[number]): privacy.MyConsent => {
    const mine = decisions.get(key(subject, purpose.code));
    const effect = mine?.decision === 'granted' ? mine.scope : undefined;
    const base: privacy.MyConsent = {
      subjectPersonId: subject,
      purposeCode: purpose.code,
      capacity: minor ? 'minor_self' : 'guardian',
      allowedScopes: purpose.allowedScopes,
      ...(purpose.notice ? { noticeCode: f.noticeCode, noticeVersionId: f.noticeVersionId } : {}),
      ...(effect ? { effectiveWidestScope: effect } : {}),
    };
    if (minor) return { ...base, decidesFrom: f.ashaComesOfAge };
    if (!mine) return { ...base, myStatus: 'not_answered' };
    return {
      ...base,
      id: mine.id,
      scope: mine.scope,
      myStatus: mine.decision,
      decidedAt: mine.decidedAt,
      noticeVersion: 1,
      channel: 'web',
      etag: `"${mine.version}"`,
    };
  };
  const subjects = minor ? [f.ashaPersonId] : [f.ashaPersonId, f.kabirPersonId];
  const rows = () => subjects.flatMap((subject) => PURPOSES.map((purpose) => row(subject, purpose)));
  return {
    'GET /v1/privacy/my/consents': (req) => onePage(req, rows()),
    'GET /v1/privacy/my/notices/{noticeCode}': (req, params) => {
      if (params.noticeCode !== f.noticeCode) return problem(404, 'NOT_FOUND');
      const asked = new URL(req.url).searchParams.get('language') ?? '';
      const language = asked === 'hi' ? 'hi' : 'en';
      return json({
        noticeVersionId: f.noticeVersionId,
        noticeCode: f.noticeCode,
        version: 1,
        language,
        purposeCodes: [f.photos, f.marketing],
        requiresReconsent: false,
        grievanceContact: {
          name: 'Ritu Sharma',
          role: 'Grievance officer',
          email: 'grievance@school.example',
        },
        bodyMarkdown: language === 'hi' ? f.hindi : f.english,
        summaryForChildren: f.forChildren,
        contentSha256: '0'.repeat(64),
        publishedAt: '2026-11-01T04:30:00Z',
      } satisfies privacy.MyNotice);
    },
    'POST /v1/privacy/my/consents': async (req) => {
      const body = (await req.json()) as privacy.RecordMyConsentRequest;
      if (minor || !subjects.includes(body.subjectPersonId)) return problem(404, 'NOT_FOUND');
      const purpose = PURPOSES.find((p) => p.code === body.purposeCode);
      if (
        !purpose?.notice ||
        body.noticeVersionId !== f.noticeVersionId ||
        !purpose.allowedScopes.includes(body.scope)
      )
        return problem(422, 'VALIDATION_FAILED');
      const decision: Decision = {
        id: id(`7e${++sequence}`),
        subject: body.subjectPersonId,
        purpose: body.purposeCode,
        scope: body.scope,
        decision: body.decision,
        decidedAt: '2026-11-23T05:00:00Z',
        version: 1,
      };
      decisions.set(key(decision.subject, decision.purpose), decision);
      return json(row(decision.subject, purpose), { status: 201 });
    },
    'POST /v1/privacy/my/consents/{consentRecordId}:withdraw': async (req, params) => {
      const mine = [...decisions.values()].find((d) => d.id === params.consentRecordId);
      if (!mine) return problem(404, 'NOT_FOUND');
      const body = (await req.json()) as privacy.WithdrawMyConsentRequest;
      if (body.etag !== undefined && body.etag !== `"${mine.version}"`)
        return problem(412, 'PRECONDITION_FAILED');
      decisions.delete(key(mine.subject, mine.purpose));
      const purpose = PURPOSES.find((p) => p.code === mine.purpose);
      return purpose ? json(row(mine.subject, purpose)) : problem(404, 'NOT_FOUND');
    },
    'GET /v1/sis/me/children': (req) =>
      onePage(req, [
        {
          student: { id: f.ashaStudentId, personId: f.ashaPersonId, status: 'active' },
          person: { id: f.ashaPersonId, firstName: 'Asha', lastName: 'Rao', displayName: 'Asha Rao' },
        },
        {
          student: { id: f.kabirStudentId, personId: f.kabirPersonId, status: 'active' },
          person: { id: f.kabirPersonId, firstName: 'Kabir', lastName: 'Rao', displayName: 'Kabir Rao' },
        },
      ]),
  };
}

/** What the school's privacy administrator holds (ADM-17, ADM-18). */
export const PRIVACY_ADMIN = [
  'privacy.tenant_purpose.read',
  'privacy.tenant_purpose.update',
  'privacy.notice.read',
  'privacy.notice.create',
  'privacy.notice.update',
  'privacy.notice.delete',
  'privacy.notice_version.read',
  'privacy.notice_version.create',
  'privacy.notice_version.update',
  'privacy.notice_version.publish',
  'privacy.notice_translation.read',
  'privacy.notice_translation.create',
  'privacy.notice_translation.update',
  'privacy.notice_translation.delete',
] as const;

/** A stored row: the contract's fields and the row version its etag carries. */
type Stored<T> = { -readonly [K in keyof T]: T[K] } & { id: string; rowVersion: number };
const out = <T extends object>({ rowVersion, ...row }: Stored<T>) => ({ ...row, etag: `"${rowVersion}"` });
const fieldProblem = (field: string, message: string) =>
  problem(422, 'VALIDATION_FAILED', 'Validation failed', { errors: [{ field, message }] });

/**
 * The school's privacy settings, in memory, with the server's rules for the slice ADM-17 and ADM-18 use: a purpose is
 * switched on only with a notice whose active version lists it and never when MySMS decides it (422 on the field); a
 * width ceiling only for a width purpose; a notice version is edited while a draft and published only with a
 * translation (409), the active one before it retiring; a published version and its wording are frozen (409); a
 * notice in use is not deleted (409); every change is guarded by the row's ETag (412).
 */
export function fakePrivacyAdmin(): Record<string, Handler> {
  let sequence = 0;
  const nextId = () => id(`8${String(++sequence).padStart(3, '0')}`);
  const purposes = new Map<string, Stored<privacy.Purpose>>(
    (
      [
        ['photo_video_publication', 'width', 'tenant'],
        ['marketing_comms', 'tenant', 'tenant'],
        ['product_analytics', 'tenant', 'platform'],
      ] as const
    ).map(([code, scopeKind, fiduciary], index) => {
      const row: Stored<privacy.Purpose> = {
        id: id(`80${index}`),
        code,
        scopeKind,
        fiduciary,
        forbiddenForMinors: fiduciary === 'platform',
        status: 'active',
        enabled: false,
        version: 1,
        rowVersion: 0,
      };
      return [row.id, row];
    }),
  );
  const notices = new Map<string, Stored<privacy.Notice>>();
  const versions = new Map<string, Stored<privacy.NoticeVersion>>();
  const translations = new Map<string, Stored<privacy.NoticeTranslation>>();
  const activeListing = (noticeId: string, code: string) =>
    [...versions.values()].some(
      (v) => v.noticeId === noticeId && v.status === 'active' && v.purposeCodes.includes(code),
    );
  const stale = (req: Request, row: { rowVersion: number }) =>
    req.headers.get('If-Match') !== `"${row.rowVersion}"`;
  const versionOf = (params: Record<string, string>) => {
    const row = versions.get(params.noticeVersionId ?? '');
    return row && row.noticeId === params.noticeId ? row : undefined;
  };
  return {
    'GET /v1/privacy/purposes': (req) => onePage(req, [...purposes.values()].map(out)),
    'PATCH /v1/privacy/purposes/{purposeId}': async (req, params) => {
      const row = purposes.get(params.purposeId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      if (stale(req, row)) return problem(412, 'PRECONDITION_FAILED');
      const body = (await req.json()) as privacy.PurposeUpdate;
      const mask = (new URL(req.url).searchParams.get('updateMask') ?? '').split(',');
      const next = { ...row };
      if (mask.includes('enabled')) next.enabled = body.enabled ?? false;
      if (mask.includes('noticeId')) next.noticeId = body.noticeId;
      if (mask.includes('maxScope')) next.maxScope = body.maxScope;
      if (next.maxScope && next.scopeKind !== 'width')
        return fieldProblem('maxScope', 'applies only to a purpose whose consents name a width');
      if (next.enabled && next.fiduciary === 'platform')
        return fieldProblem('enabled', 'is decided by the platform and cannot be switched on by a school');
      if (next.enabled && (!next.noticeId || !activeListing(next.noticeId, next.code ?? '')))
        return fieldProblem('noticeId', 'must name a notice whose active version lists this purpose');
      next.rowVersion += 1;
      purposes.set(row.id, next);
      return json(out(next));
    },
    'GET /v1/privacy/notices': (req) => onePage(req, [...notices.values()].map(out)),
    'POST /v1/privacy/notices': async (req) => {
      const body = (await req.json()) as privacy.Notice;
      if (!/^[a-z][a-z0-9_]*$/.test(body.code))
        return fieldProblem('code', 'must be lower case letters, digits, _');
      if ([...notices.values()].some((n) => n.code === body.code))
        return problem(409, 'ALREADY_EXISTS', 'Conflict', { detail: 'A notice with this code exists.' });
      const row: Stored<privacy.Notice> = { id: nextId(), code: body.code, title: body.title, rowVersion: 1 };
      notices.set(row.id, row);
      return json(out(row), { status: 201 });
    },
    'PATCH /v1/privacy/notices/{noticeId}': async (req, params) => {
      const row = notices.get(params.noticeId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      if (stale(req, row)) return problem(412, 'PRECONDITION_FAILED');
      const body = (await req.json()) as privacy.NoticeUpdate;
      const next = { ...row, title: body.title ?? row.title, rowVersion: row.rowVersion + 1 };
      notices.set(row.id, next);
      return json(out(next));
    },
    'DELETE /v1/privacy/notices/{noticeId}': (req, params) => {
      const row = notices.get(params.noticeId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      if (stale(req, row)) return problem(412, 'PRECONDITION_FAILED');
      const used =
        [...purposes.values()].some((p) => p.noticeId === row.id) ||
        [...versions.values()].some((v) => v.noticeId === row.id && v.status === 'active');
      if (used)
        return problem(409, 'INVALID_STATE_TRANSITION', 'Conflict', {
          detail: 'The notice has an active version, which is kept as consent evidence.',
        });
      notices.delete(row.id);
      return new Response(null, { status: 204 });
    },
    'GET /v1/privacy/notices/{noticeId}/versions': (req, params) =>
      onePage(
        req,
        [...versions.values()]
          .filter((v) => v.noticeId === params.noticeId)
          .sort((a, b) => (b.version ?? 0) - (a.version ?? 0))
          .map(out),
      ),
    'POST /v1/privacy/notices/{noticeId}/versions': async (req, params) => {
      if (!notices.has(params.noticeId ?? '')) return problem(404, 'NOT_FOUND');
      const body = (await req.json()) as privacy.NoticeVersion;
      if (body.purposeCodes.length === 0) return fieldProblem('purposeCodes', 'size must be at least 1');
      if (!body.grievanceContact.email) return fieldProblem('grievanceContact.email', 'must not be blank');
      const number = [...versions.values()].filter((v) => v.noticeId === params.noticeId).length + 1;
      const row: Stored<privacy.NoticeVersion> = {
        ...body,
        id: nextId(),
        noticeId: params.noticeId,
        version: number,
        status: 'draft',
        rowVersion: 1,
      };
      versions.set(row.id, row);
      return json(out(row), { status: 201 });
    },
    'PATCH /v1/privacy/notices/{noticeId}/versions/{noticeVersionId}': async (req, params) => {
      const row = versionOf(params);
      if (!row) return problem(404, 'NOT_FOUND');
      if (stale(req, row)) return problem(412, 'PRECONDITION_FAILED');
      if (row.status !== 'draft')
        return problem(409, 'INVALID_STATE_TRANSITION', 'Conflict', {
          detail: 'The notice version is published; publish a new version to change it.',
        });
      const body = (await req.json()) as privacy.NoticeVersionUpdate;
      const next = { ...row, ...body, rowVersion: row.rowVersion + 1 };
      versions.set(row.id, next);
      return json(out(next));
    },
    'POST /v1/privacy/notices/{noticeId}/versions/{noticeVersionId}:publish': async (req, params) => {
      const row = versionOf(params);
      if (!row) return problem(404, 'NOT_FOUND');
      const body = (await req.json()) as privacy.PublishNoticeVersionRequest;
      if (body.etag !== undefined && body.etag !== `"${row.rowVersion}"`)
        return problem(412, 'PRECONDITION_FAILED');
      if (![...translations.values()].some((tr) => tr.noticeVersionId === row.id))
        return problem(409, 'INVALID_STATE_TRANSITION', 'Conflict', {
          detail: 'A notice version needs a translation before it is published.',
        });
      for (const other of versions.values())
        if (other.noticeId === row.noticeId && other.status === 'active')
          versions.set(other.id, { ...other, status: 'retired', rowVersion: other.rowVersion + 1 });
      const next = {
        ...row,
        status: 'active' as const,
        publishedAt: '2026-11-23T06:00:00Z',
        rowVersion: row.rowVersion + 1,
      };
      versions.set(row.id, next);
      return json(out(next));
    },
    'GET /v1/privacy/notices/{noticeId}/versions/{noticeVersionId}/translations': (req, params) =>
      onePage(
        req,
        [...translations.values()].filter((tr) => tr.noticeVersionId === params.noticeVersionId).map(out),
      ),
    'POST /v1/privacy/notices/{noticeId}/versions/{noticeVersionId}/translations': async (req, params) => {
      const version = versionOf(params);
      if (!version) return problem(404, 'NOT_FOUND');
      if (version.status !== 'draft') return problem(409, 'INVALID_STATE_TRANSITION');
      const body = (await req.json()) as privacy.NoticeTranslation;
      const row = { ...body, id: nextId(), noticeVersionId: version.id, rowVersion: 1 };
      translations.set(row.id, row);
      return json(out(row), { status: 201 });
    },
  };
}
