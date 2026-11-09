/** Query keys follow the API: [module, resource, params] (ui/05 §4). */
export type QueryKey = readonly [module: string, resource: string, params?: unknown];

export const queryKey = (module: string, resource: string, params?: unknown): QueryKey =>
  params === undefined ? [module, resource] : [module, resource, params];
