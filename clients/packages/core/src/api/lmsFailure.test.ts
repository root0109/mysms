import type { MessageKey } from '@mysms/i18n';
import { describe, expect, it } from 'vitest';
import { lmsFailureText } from './lmsFailure';
import { ApiError } from './problem';

/** The catalogue key itself stands for its sentence, so each case shows which sentence was chosen. */
const t = (key: MessageKey) => key;
const failure = (status: number, code: string, detail?: string, messages: string[] = []) =>
  new ApiError({
    status,
    code,
    title: code,
    detail,
    errors: messages.map((message) => ({ field: 'f', message })),
  });

describe('lmsFailureText', () => {
  it('gives a code with a sentence of its own that sentence, before the status rules', () => {
    expect(lmsFailureText(t, failure(409, 'VIDEO_NOT_WATCHED', 'Watch it first.'))).toBe(
      'lms.error.videoNotWatched',
    );
    expect(lmsFailureText(t, failure(404, 'NOT_FOUND'))).toBe('common.state.notAvailable');
  });

  it('reads the status of any other refusal', () => {
    expect(lmsFailureText(t, failure(412, 'PRECONDITION_FAILED'))).toBe('common.conflict.body');
    expect(lmsFailureText(t, failure(409, 'COHORT_NOT_OPEN', 'The cohort is closed.'))).toBe(
      'The cohort is closed.',
    );
    expect(lmsFailureText(t, failure(503, 'UNAVAILABLE'))).toBe('common.error.generic');
    expect(lmsFailureText(t, failure(422, 'VALIDATION_FAILED', undefined, ['Too long.', 'Required.']))).toBe(
      'Too long. Required.',
    );
    expect(lmsFailureText(t, failure(422, 'VALIDATION_FAILED'))).toBe('common.validation.pattern');
    expect(lmsFailureText(t, failure(403, 'PERMISSION_DENIED'))).toBe('lms.common.forbidden');
    expect(lmsFailureText(t, failure(501, 'NOT_IMPLEMENTED'))).toBe('lms.common.notServed');
    expect(lmsFailureText(t, failure(500, 'INTERNAL'))).toBe('common.error.generic');
  });

  it('reads anything that is not a refusal as the generic sentence', () => {
    expect(lmsFailureText(t, new Error('offline'))).toBe('common.error.generic');
    expect(lmsFailureText(t, undefined)).toBe('common.error.generic');
  });
});
