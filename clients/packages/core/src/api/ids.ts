import { v7 as uuidv7 } from 'uuid';

/** A fresh Idempotency-Key (UUIDv7) for one logical attempt; retries of that attempt reuse it (ui/05 §6). */
export const newIdempotencyKey = (): string => uuidv7();
