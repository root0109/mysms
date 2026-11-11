import '@testing-library/jest-dom/vitest';
import { cleanup, configure } from '@testing-library/react';
import { afterEach } from 'vitest';
// React Router builds internal requests with jsdom AbortSignals, while Node's Request requires Node AbortSignals.
// The web screens under test do not depend on request-level abort, so strip this test-only signal mismatch.
const NodeRequest = globalThis.Request;
globalThis.Request = class CompatibleRequest extends NodeRequest {
  constructor(input: RequestInfo | URL, init?: RequestInit) {
    const { signal: _signal, ...options } = init ?? {};
    super(input, options);
  }
};

// Integration tests sign in and render whole screens; under a fully parallel CI run on few cores the
// default 1 s wait is too tight, so allow 5 s per wait (timing never decides a test's outcome).
configure({ asyncUtilTimeout: 5000 });
afterEach(() => cleanup());
