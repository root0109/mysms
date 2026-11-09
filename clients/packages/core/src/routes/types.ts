/** W web · S student app · P parent app · T teacher app · B tablet · C platform console (ui/01). */
export type ClientKind = 'W' | 'S' | 'P' | 'T' | 'B' | 'C';

export interface ScreenDef {
  readonly id: string;
  readonly title: string;
  /** Route patterns without the web `/app` prefix, path params in braces: `/sis/students/{studentId}`. */
  readonly routes: readonly string[];
  readonly clients: readonly string[];
  /** For screens without a route of their own (sheets, drawers, wizards): where they appear. */
  readonly placement?: string;
}
