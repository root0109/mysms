import '@testing-library/jest-dom/vitest';
import { cleanup, configure } from '@testing-library/react';
import { afterEach } from 'vitest';

// Integration tests sign in and render whole screens; under a fully parallel CI run on few cores the
// default 1 s wait is too tight, so allow 5 s per wait (timing never decides a test's outcome).
configure({ asyncUtilTimeout: 5000 });
afterEach(() => cleanup());
