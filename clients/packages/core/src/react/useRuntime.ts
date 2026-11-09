import { useContext } from 'react';
import type { Runtime } from '../runtime';
import { RuntimeContext } from './runtimeContext';

export function useRuntime(): Runtime {
  const runtime = useContext(RuntimeContext);
  if (!runtime) throw new Error('useRuntime must be used inside <MySmsProvider>');
  return runtime;
}
