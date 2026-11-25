import { describe, expect, it } from 'vitest';
import { ApiError } from '../api/problem';
import {
  belowFloor,
  boardClock,
  breachChanges,
  breachFilter,
  breachOpen,
  breachSteps,
  governanceFailureKey,
  legalHoldBody,
  legalHoldFilter,
  retentionChanges,
} from './privacyGovernance';

describe('privacy governance (ADM-20, ADM-21, ADM-22)', () => {
  it("reads the Board's clock: report sent, not a breach, overdue, within a day, running", () => {
    expect(boardClock({ boardReportSentAt: '2026-11-20T00:00:00Z', overdue: false })).toBe('reported');
    expect(boardClock({ status: 'not_a_breach', overdue: false })).toBe('notABreach');
    expect(boardClock({ status: 'contained', overdue: true, hoursRemaining: -8 })).toBe('overdue');
    expect(boardClock({ status: 'reported', overdue: false, hoursRemaining: 12 })).toBe('dueSoon');
    expect(boardClock({ status: 'reported', overdue: false, hoursRemaining: 60 })).toBe('running');
  });

  it('offers the steps a breach status allows, closing only after the detailed report', () => {
    expect(breachSteps({ status: 'reported' })).toEqual(['triage']);
    expect(breachSteps({ status: 'triaged' })).toEqual(['contain', 'markNotABreach']);
    expect(breachSteps({ status: 'contained' })).toEqual(['recordNotification']);
    expect(breachSteps({ status: 'principals_notified' })).toEqual(['recordNotification']);
    expect(breachSteps({ status: 'principals_notified', boardReportSentAt: '2026-11-22T00:00:00Z' })).toEqual(
      ['close', 'recordNotification'],
    );
    expect(breachSteps({ status: 'closed', closedAt: '2026-11-23T00:00:00Z' })).toEqual([]);
    expect(breachOpen({})).toBe(true);
    expect(breachOpen({ closedAt: '2026-11-23T00:00:00Z' })).toBe(false);
    expect(breachFilter('open')).toContain('not_a_breach');
    expect(breachFilter('overdue')).toBe(breachFilter('open'));
    expect(breachFilter('all')).toBeUndefined();
  });

  it('sends only the breach fields that changed, never an emptied one', () => {
    const breach = {
      detectedAt: '2026-11-20T00:00:00Z',
      becameAwareAt: '2026-11-20T01:00:00Z',
      description: 'Marks list sent to the wrong group.',
      estimatedPrincipals: 38,
    };
    expect(
      breachChanges(breach, { description: breach.description, estimatedPrincipals: 40, mitigation: '' }),
    ).toEqual({ estimatedPrincipals: 40 });
  });

  it('sends a hold only the targets of its scope', () => {
    const draft = {
      reasonCode: 'litigation' as const,
      reviewBy: '2027-01-31',
      subjectPersonId: ' p1 ',
      categoryCode: 'finance',
      module: 'sis',
      entityType: 'student',
      entityIds: 'a1, a2\na3',
    };
    expect(legalHoldBody({ ...draft, scope: 'subject' })).toEqual({
      reasonCode: 'litigation',
      scope: 'subject',
      reviewBy: '2027-01-31',
      subjectPersonId: 'p1',
    });
    expect(legalHoldBody({ ...draft, scope: 'category' })).toMatchObject({ categoryCode: 'finance' });
    expect(legalHoldBody({ ...draft, scope: 'category' })).not.toHaveProperty('subjectPersonId');
    expect(legalHoldBody({ ...draft, scope: 'records' })).toMatchObject({
      module: 'sis',
      entityType: 'student',
      entityIds: ['a1', 'a2', 'a3'],
    });
    expect(legalHoldFilter('active')).toBe('active = true');
    expect(legalHoldFilter('all')).toBeUndefined();
  });

  it('keeps a retention period at or above its floor and sends only what changed', () => {
    const policy = { periodDays: 365, minimumPeriodDays: 180, action: 'delete' as const };
    expect(belowFloor(policy, 179)).toBe(true);
    expect(belowFloor(policy, 180)).toBe(false);
    expect(belowFloor(policy, Number('12.5'))).toBe(true);
    expect(retentionChanges(policy, 365, 'delete')).toEqual({});
    expect(retentionChanges(policy, 400, 'delete')).toEqual({ periodDays: 400 });
    expect(retentionChanges(policy, 365, 'archive')).toEqual({ action: 'archive' });
  });

  it('names a refused step by its reason', () => {
    const error = (status: number, code: string) => new ApiError({ status, code, title: code });
    expect(governanceFailureKey(error(409, 'INVALID_STATE_TRANSITION'))).toBe(
      'privacy.governance.error.state',
    );
    expect(governanceFailureKey(error(412, 'PRECONDITION_FAILED'))).toBe('common.conflict.body');
    expect(governanceFailureKey(error(422, 'VALIDATION_FAILED'))).toBe('privacy.governance.error.invalid');
    expect(governanceFailureKey(new Error('network'))).toBe('common.error.generic');
  });
});
