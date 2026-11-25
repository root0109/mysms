/**
 * The cohort screens the phone (S) and tablet (B) carry (ui/02 §3): cohorts, roster, schedule, forums, live classes,
 * announcements.
 */
export const COHORT_SCREEN_IDS = [
  'LMS-15',
  'LMS-20',
  'LMS-21',
  'LMS-22',
  'LMS-23',
  'LMS-25',
  'LMS-26',
  'LMS-27',
  'LMS-28',
] as const;

/** The coursework screens the phone and tablet carry: quizzes, assignments, groups, grades and the drive. */
export const COURSEWORK_SCREEN_IDS = [
  'LMS-08',
  'LMS-09',
  'LMS-10',
  'LMS-11',
  'LMS-31',
  'LMS-32',
  'LMS-33',
  'LMS-54',
] as const;

/** The learning screens the phone and tablet carry: the catalogue, my learning, the course, lessons, e-books, notes. */
export const LEARNING_SCREEN_IDS = [
  'LMS-01',
  'LMS-02',
  'LMS-04',
  'LMS-05',
  'LMS-06',
  'LMS-07',
  'LMS-13',
  'LMS-14',
] as const;

/** Every LMS screen the phone and tablet carry; the rest stay on the web. */
export const LMS_SCREEN_IDS = [
  ...LEARNING_SCREEN_IDS,
  ...COHORT_SCREEN_IDS,
  ...COURSEWORK_SCREEN_IDS,
] as const;

export type LearningScreenId = (typeof LEARNING_SCREEN_IDS)[number];
export type CohortScreenId = (typeof COHORT_SCREEN_IDS)[number];
export type CourseworkScreenId = (typeof COURSEWORK_SCREEN_IDS)[number];
export type LmsScreenId = (typeof LMS_SCREEN_IDS)[number];

/** An LMS screen and its deep-link parameters; narrowed to one workspace's screens where that workspace renders it. */
export interface LmsRoute<Id extends LmsScreenId = LmsScreenId> {
  screenId: Id;
  params: Record<string, string>;
}

export function isLmsScreenId(value: string): value is LmsScreenId {
  return (LMS_SCREEN_IDS as readonly string[]).includes(value);
}

/** Whether the route belongs to the given workspace's screens. */
export function routeOf<Id extends LmsScreenId>(ids: readonly Id[], route: LmsRoute): route is LmsRoute<Id> {
  return (ids as readonly LmsScreenId[]).includes(route.screenId);
}
