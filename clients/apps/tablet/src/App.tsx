import { createRuntime } from '@mysms/core';
import { NativeApp, secureStoreVault } from '@mysms/native-kit';
import Constants from 'expo-constants';
import { StatusBar } from 'expo-status-bar';
import { Platform } from 'react-native';

const apiBaseUrl = process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';

const runtime = createRuntime({
  baseUrl: apiBaseUrl,
  clientId: `tablet/${Constants.expoConfig?.version ?? '0.0.0'}/${Platform.OS}`,
  vault: secureStoreVault(),
});

export default function App() {
  return (
    <>
      <StatusBar style="auto" />
      <NativeApp
        runtime={runtime}
        client="B"
        tablet
        webOrigin={process.env.EXPO_PUBLIC_WEB_ORIGIN ?? apiBaseUrl}
        defaultTenantCode={process.env.EXPO_PUBLIC_TENANT_CODE ?? ''}
      />
    </>
  );
}
