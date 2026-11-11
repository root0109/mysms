export const ASSESSMENT_SCREEN_IDS = [
  'ASM-01',
  'ASM-02',
  'ASM-03',
  'ASM-04',
  'ASM-05',
  'ASM-06',
  'ASM-07',
  'ASM-08',
  'ASM-09',
  'ASM-10',
  'ASM-11',
  'ASM-12',
  'ASM-13',
] as const;

export type AssessmentScreenId = (typeof ASSESSMENT_SCREEN_IDS)[number];

export interface AssessmentRoute {
  screenId: AssessmentScreenId;
  params: Record<string, string>;
}

export function isAssessmentScreenId(value: string): value is AssessmentScreenId {
  return (ASSESSMENT_SCREEN_IDS as readonly string[]).includes(value);
}
