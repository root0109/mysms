import type { TokenVault } from '@mysms/core';
import * as SecureStore from 'expo-secure-store';

/** Refresh token in the platform keystore (Keychain / Keystore), never in plain storage (ui/05 §3). */
export function secureStoreVault(key = 'mysms.refreshToken'): TokenVault {
  return {
    load: () => SecureStore.getItemAsync(key),
    save: (token) =>
      SecureStore.setItemAsync(key, token, {
        keychainAccessible: SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY,
      }),
    clear: () => SecureStore.deleteItemAsync(key),
  };
}
