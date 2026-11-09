import { createContext } from 'react';
import type { Runtime } from '../runtime';

export const RuntimeContext = createContext<Runtime | null>(null);
