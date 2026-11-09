import { resolveLink, type ClientKind, type RouteMatch } from '@mysms/core';
import { useEffect, useState } from 'react';
import { Linking } from 'react-native';

/**
 * Deep links (ARCHITECTURE §4): the screen this app carries, else the web URL, else nothing (polite dead end).
 * Links carry no tokens; the app signs in and loads the record through the API.
 */
export function useDeepLinks(client: ClientKind, webOrigin: string): RouteMatch | null {
  const [match, setMatch] = useState<RouteMatch | null>(null);
  useEffect(() => {
    const handle = (url: string | null) => {
      if (!url) return;
      const r = resolveLink(url, client, webOrigin);
      if (r.kind === 'screen') setMatch(r.match);
      else if (r.kind === 'web-fallback') void Linking.openURL(r.webUrl);
    };
    void Linking.getInitialURL().then(handle);
    const sub = Linking.addEventListener('url', (e) => handle(e.url));
    return () => sub.remove();
  }, [client, webOrigin]);
  return match;
}
