import { CohortScreen } from './CohortScreen';
import { CourseworkScreen } from './CourseworkScreen';
import { LearningScreen } from './LearningScreen';
import {
  COHORT_SCREEN_IDS,
  COURSEWORK_SCREEN_IDS,
  LEARNING_SCREEN_IDS,
  routeOf,
  type LmsRoute,
} from './lmsRoutes';

export interface LmsScreenProps {
  route: LmsRoute;
  tablet: boolean;
  /** Opens another LMS screen; Back returns here. */
  onNavigate: (route: LmsRoute) => void;
  /** Leaves the screen: the previous LMS screen, or home (or sign-in, for a guest). */
  onBack: () => void;
  /** For a guest browsing the catalogue: the way to sign in, offered where enrolling needs a session. */
  onSignIn?: () => void;
}

/**
 * The LMS on phones (S) and the tablet (B), the LMS's main device: one entry point for every LMS deep link and home
 * button, handing the route to the workspace that renders its screen (a Facade over the learning, cohort and
 * coursework workspaces).
 */
export function LmsScreen({ route, tablet, onNavigate, onBack, onSignIn }: LmsScreenProps) {
  if (routeOf(LEARNING_SCREEN_IDS, route))
    return (
      <LearningScreen
        route={route}
        tablet={tablet}
        onNavigate={onNavigate}
        onBack={onBack}
        {...(onSignIn ? { onSignIn } : {})}
      />
    );
  if (routeOf(COHORT_SCREEN_IDS, route))
    return <CohortScreen route={route} tablet={tablet} onRoute={onNavigate} onBack={onBack} />;
  if (routeOf(COURSEWORK_SCREEN_IDS, route))
    return <CourseworkScreen route={route} tablet={tablet} onBack={onBack} />;
  return null;
}
