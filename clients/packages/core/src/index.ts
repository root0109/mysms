// Public surface of @mysms/core. Apps import from here only.
export { ApiError, isApiError, parseRetryAfter, type FieldError } from './api/problem';
export { MAX_QUERY_RETRIES, isRetryableError, retryDelay, shouldRetryQuery } from './api/retry';
export { newIdempotencyKey } from './api/ids';
export { call, callWithEtag, toApiError, type WithEtag } from './api/unwrap';
export {
  createApiClient,
  createTransport,
  defaultFetch,
  isAuthPath,
  type ApiClass,
  type ApiClient,
  type ApiClientConfig,
  type FetchFn,
  type TokenSource,
  type TransportConfig,
} from './api/client';
export {
  REFRESH_SKEW_MS,
  createSession,
  memoryVault,
  type AuthTransport,
  type EffectiveSetting,
  type LoginRequest,
  type LoginResult,
  type Me,
  type Session,
  type SessionState,
  type SessionStatus,
  type TokenPair,
  type TokenVault,
} from './auth/session';
export { createRuntime, type Runtime, type RuntimeConfig } from './runtime';
export { STALE_TIME, createQueryClient } from './query/queryClient';
export { queryKey, type QueryKey } from './query/keys';
export { can, canAll, canAny } from './permissions/can';
export { PERMISSION_CODES, type PermissionCode } from './generated/permissions';
export { OPERATION_MODULES, type OperationModule } from './generated/operations';
export { SCREENS } from './generated/screens';
export {
  buildPath,
  matchRoute,
  resolveLink,
  routerPaths,
  screenById,
  toRoutePath,
  type LinkResolution,
  type RouteMatch,
} from './routes/resolve';
export type { ClientKind, ScreenDef } from './routes/types';
export { parseQuery, pathOf, searchOf, type QueryLookup } from './routes/url';
export {
  DONE_STATUSES,
  cancelOperation,
  getOperation,
  isDone,
  isOperationModule,
  pollInterval,
  type Operation,
} from './operations/operations';
export { MySmsProvider } from './react/context';
export { useRuntime } from './react/useRuntime';
export { PermissionGate } from './react/PermissionGate';
export { useI18n, type I18n } from './hooks/i18n';
export { useCan, useSession } from './hooks/session';
export { useCompleteMfa, useSignIn, useSignOut } from './hooks/auth';
export { useCancelOperation, useJobCentre, useOperation, type JobCentreParams } from './hooks/operations';
export {
  useCountries,
  useCountry,
  useCreateCountry,
  useDeleteCountry,
  useInfiniteCountries,
  useUpdateCountry,
  type Country,
  type CountryCreate,
  type CountryUpdate,
  type ListParams,
} from './hooks/master';
export {
  DEFAULT_PREFERENCES,
  explicitSetting,
  preferencesFrom,
  settingValue,
  type UiPreferences,
} from './settings/preferences';
export {
  useCreateSettingValue,
  useDeleteSettingValue,
  useInfiniteSettingValues,
  useMySettings,
  usePreferences,
  useReloadMySettings,
  useResetMySetting,
  useSetMySetting,
  useSettingDefinitions,
  useSettingValues,
  useUpdateSettingValue,
  type SettingDefinition,
  type SettingValue,
  type SettingValueCreate,
  type SettingValueUpdate,
} from './hooks/settings';
export { useMyPerson, useUpdateMyPerson, type MyPersonUpdate, type Person } from './hooks/profile';
export {
  useChangePassword,
  useDisableMfa,
  useEndSession,
  useEnrollMfa,
  useMfaCredentials,
  useMySessions,
  useRegenerateRecoveryCodes,
  useVerifyMfaEnrollment,
  type MfaCredential,
  type MfaEnrollment,
  type UserSession,
} from './hooks/security';
export {
  useNotificationPreferenceSets,
  useNotificationPreferences,
  useUpdateNotificationPreference,
  type NotificationPreference,
  type NotificationPreferenceSet,
  type NotificationPreferenceUpdate,
} from './hooks/notifications';
export {
  ATTENDANCE_STATUSES,
  nextDay,
  useAbsenceReasons,
  useAttendanceMarks,
  useAttendanceRegister,
  useAttendanceSheet,
  useAttendanceSheetStep,
  useAttendanceSummary,
  useClassRoster,
  useClassSectionOptions,
  useInstitutions,
  useRecordAttendance,
  useStudent,
  useStudentNames,
  useTakeAttendanceForSession,
  type AbsenceReason,
  type AttendanceGrouping,
  type AttendanceMark,
  type AttendanceRecord,
  type AttendanceRegister,
  type AttendanceSheet,
  type AttendanceStatus,
  type AttendanceSummary,
  type AttendanceSummaryParams,
  type ClassSectionOption,
  type RosterEntry,
} from './hooks/attendance';
export {
  LEAVE_DAY_PARTS,
  academicYearOn,
  cancellation,
  leaveDays,
  useAcademicYears,
  useApplyForLeave,
  useLeaveApplications,
  useLeavePosition,
  useLeaveStep,
  useLeaveTypes,
  usePendingLeave,
  useStaffNames,
  type AcademicYear,
  type LeaveApplication,
  type LeaveDayPart,
  type LeavePosition,
  type LeaveRequest,
  type LeaveStep,
  type LeaveType,
} from './hooks/leave';
export {
  isTimetableDay,
  localDay,
  shiftDay,
  timetableDays,
  useMyStaff,
  useRoomNames,
  useSubjectNames,
  useTimetable,
  weekOf,
  type TimetableDay,
  type TimetableSession,
  type TimetableSubject,
} from './hooks/timetable';
export {
  useCheckHomeworkSubmission,
  useHomework,
  useHomeworkApprovals,
  useHomeworkStep,
  useHomeworkSubmissions,
  type Homework,
  type HomeworkApproval,
  type HomeworkStep,
  type HomeworkSubmission,
} from './hooks/homework';
export {
  settingCategoryLabel,
  settingLabel,
  settingScopeLabel,
  settingSourceLabel,
  settingValueLabel,
} from './settings/labels';
export { RESOURCES } from './generated/resources';
export { RECORD_PERMISSIONS } from './generated/recordPermissions';
export type { ActionDef, FieldDef, OperationRef, RecordPage, RecordRow, ResourceDef } from './records/types';
export {
  actionAllowed,
  allOf,
  createRecord,
  deleteRecord,
  getRecord,
  listRecords,
  MAX_RANGE_DAYS,
  rangeFilter,
  runAction,
  toPayload,
  type RecordListParams,
} from './records/api';
export {
  useCreateRecord,
  useDeleteRecord,
  useInfiniteRecords,
  useRecord,
  useRecordAction,
  useUpdateRecord,
} from './records/hooks';
