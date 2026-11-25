import { describe, expect, it } from 'vitest';
import { ApiError } from '../api/problem';
import {
  availableSteps,
  closeOutcomes,
  deadlineTone,
  queueFilter,
  responseRequired,
  retryable,
  rightsFailureKey,
  submitBody,
} from './privacyRights';

const NOW = new Date('2026-11-24T06:00:00Z');
const inDays = (days: number) => new Date(NOW.getTime() + days * 86_400_000).toISOString();

describe('privacy rights requests (ACC-15, ADM-19)', () => {
  it('sends a correction its record and a grievance its category, and no other type either', () => {
    const correction = {
      module: 'people',
      recordType: 'person',
      fields: [{ field: 'surname', requestedValue: 'Rao' }],
    };
    expect(
      submitBody({
        type: 'access',
        subjectPersonId: 's',
        description: ' What? ',
        correction,
        grievanceCategory: 'other',
      }),
    ).toEqual({ type: 'access', subjectPersonId: 's', description: 'What?' });
    expect(
      submitBody({ type: 'correction', subjectPersonId: 's', description: 'Fix', correction }),
    ).toMatchObject({
      correction,
    });
    expect(
      submitBody({
        type: 'grievance',
        subjectPersonId: 's',
        description: 'Bad',
        grievanceCategory: 'consent',
      }),
    ).toMatchObject({ grievanceCategory: 'consent' });
  });

  it('offers the steps the state allows, an access request fulfilled with its summary', () => {
    expect(availableSteps({ status: 'submitted', type: 'nomination' })).toEqual([
      'verifyIdentity',
      'reject',
      'extend',
    ]);
    expect(availableSteps({ status: 'verified', type: 'correction' })).toEqual([
      'assign',
      'reject',
      'extend',
    ]);
    expect(availableSteps({ status: 'in_progress', type: 'access' })[0]).toBe('fulfilAccess');
    expect(availableSteps({ status: 'closed', type: 'grievance' })).toEqual([]);
    expect(availableSteps({ status: 'in_progress', type: 'erasure' })[0]).toBe('startFulfilment');
    expect(availableSteps({ status: 'in_progress', type: 'export', tasks: [{ module: 'sis' }] })).toEqual([
      'assign',
      'extend',
    ]);
    expect(availableSteps({ status: 'in_progress', type: 'withdraw_all' })[0]).toBe('fulfilWithdrawAll');
    expect(availableSteps({ status: 'action_required', type: 'export' })).toEqual(['extend']);
    expect(retryable({ status: 'action_required' }, { status: 'failed' })).toBe(true);
    expect(retryable({ status: 'in_progress' }, { status: 'dispatched' })).toBe(true);
    expect(retryable({ status: 'in_progress' }, { status: 'done' })).toBe(false);
    expect(retryable({ status: 'closed' }, { status: 'failed' })).toBe(false);
    expect(closeOutcomes('access')).toEqual(['declined']);
    expect(closeOutcomes('nomination')).toEqual(['fulfilled', 'declined']);
    expect(responseRequired('grievance', 'fulfilled')).toBe(true);
    expect(responseRequired('correction', 'declined')).toBe(true);
    expect(responseRequired('correction', 'fulfilled')).toBe(false);
  });

  it('colours a deadline: overdue, due within a week, on track, closed', () => {
    expect(deadlineTone({ dueAt: inDays(-1), overdue: true }, NOW)).toBe('overdue');
    expect(deadlineTone({ dueAt: inDays(3), overdue: false }, NOW)).toBe('dueSoon');
    expect(deadlineTone({ dueAt: inDays(20), overdue: false }, NOW)).toBe('onTrack');
    expect(deadlineTone({ dueAt: inDays(-5), overdue: false, closedAt: inDays(-6) }, NOW)).toBe('closed');
    expect(queueFilter('open')).toContain('closed');
    expect(queueFilter('all')).toBeUndefined();
  });

  it('names a refused step by its reason, a conflict of interest apart from a moved-on state', () => {
    const error = (status: number, code: string) => new ApiError({ status, code, title: code });
    expect(rightsFailureKey(error(409, 'RIGHTS_REQUEST_CONFLICT_OF_INTEREST'))).toBe(
      'privacy.rights.error.conflictOfInterest',
    );
    expect(rightsFailureKey(error(409, 'INVALID_STATE_TRANSITION'))).toBe('privacy.rights.error.state');
    expect(rightsFailureKey(error(412, 'PRECONDITION_FAILED'))).toBe('common.conflict.body');
    expect(rightsFailureKey(new Error('network'))).toBe('common.error.generic');
  });
});
