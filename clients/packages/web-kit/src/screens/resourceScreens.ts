/**
 * The inventory screens (ui/02 §3) whose job is to manage one record type the backend serves standard operations
 * for: each gets its own route, heading, navigation entry, columns and filter, on the shared list manager
 * (04 §5). Screens that need more than a list manager (dashboards, wizards, grids, "my …" views) are built on their own.
 */
/** The task groups of ui/02 §2.1: daily work first, every catalogue and configuration screen in Setup, last. */
import type { PermissionCode } from '@mysms/core';

export type NavGroupId =
  'people' | 'academics' | 'assessment' | 'learning' | 'money' | 'communication' | 'services' | 'setup';

export interface ResourceScreenDef {
  /** Inventory id, e.g. `ADM-02`. */
  id: string;
  /** `<module>.<Resource>` from api/standard-resources.json. */
  resource: string;
  group: NavGroupId;
  /** Contract field names shown as columns (default: chosen from the record type's fields). */
  columns?: readonly string[];
  /** An enum field offered as a filter (default: the record type's status field, when it has one). */
  filter?: string;
  /** The inventory screen that works on one row of this list (e.g. `ATT-03` for a sheet); rows link to it. */
  detail?: string;
  /** The permission that puts the screen in the navigation, when listing the record type is not enough. */
  navPermission?: PermissionCode;
  /** Something each row can start besides its editor; the tools are named in `ResourceScreen`. */
  tool?:
    | 'takeAttendance'
    | 'cycleTimeline'
    | 'homeworkBoard'
    | 'homeworkApproval'
    | 'assignment'
    | 'examSession'
    | 'exam'
    | 'marksheet'
    | 'generateResults'
    | 'clearDemoData';
}

export const NAV_GROUPS: readonly NavGroupId[] = [
  'people',
  'academics',
  'assessment',
  'learning',
  'money',
  'communication',
  'services',
  'setup',
];

export const RESOURCE_SCREENS: readonly ResourceScreenDef[] = [
  { id: 'SIS-01', resource: 'sis.Student', group: 'people', detail: 'SIS-02' },
  { id: 'SIS-04', resource: 'sis.Guardian', group: 'people' },
  { id: 'ADMN-01', resource: 'admission.AdmissionCycle', group: 'people', tool: 'cycleTimeline' },
  { id: 'ADMN-02', resource: 'admission.Application', group: 'people', detail: 'ADMN-03' },
  { id: 'SIS-03', resource: 'sis.StudentEnrollment', group: 'people' },
  { id: 'SIS-06', resource: 'sis.DisciplineIncident', group: 'people' },
  { id: 'SIS-05', resource: 'sis.StudentHealthRecord', group: 'people' },
  { id: 'SIS-07', resource: 'sis.StudentActivity', group: 'people' },
  { id: 'ACA-01', resource: 'academics.Program', group: 'setup' },
  { id: 'ACA-02', resource: 'academics.Course', group: 'setup' },
  { id: 'ACA-03', resource: 'academics.Batch', group: 'setup' },
  { id: 'ACA-04', resource: 'academics.GradeLevel', group: 'setup' },
  { id: 'ACA-05', resource: 'academics.ClassSection', group: 'academics', detail: 'ACA-06' },
  { id: 'ACA-07', resource: 'academics.Subject', group: 'academics' },
  { id: 'ACA-08', resource: 'academics.TeachingAssignment', group: 'academics' },
  { id: 'ACA-09', resource: 'academics.CurriculumUnit', group: 'academics' },
  { id: 'ACA-10', resource: 'academics.LessonPlan', group: 'academics' },
  { id: 'SIS-10', resource: 'sis.SubjectRegistration', group: 'academics' },
  { id: 'TT-01', resource: 'timetable.Period', group: 'setup' },
  { id: 'TT-02', resource: 'timetable.Session', group: 'academics', tool: 'takeAttendance' },
  { id: 'ATT-01', resource: 'attendance.AttendanceRegister', group: 'academics' },
  { id: 'ATT-02', resource: 'attendance.AttendanceSheet', group: 'academics', detail: 'ATT-03' },
  { id: 'ATT-06', resource: 'time_attendance.StaffAttendance', group: 'people' },
  {
    id: 'ASM-01',
    resource: 'assessment.Homework',
    group: 'assessment',
    detail: 'ASM-03',
    tool: 'homeworkBoard',
  },
  // the same list opened on the homework that waits for review (its route carries ?status=pending)
  {
    id: 'ASM-02',
    resource: 'assessment.Homework',
    group: 'assessment',
    detail: 'ASM-03',
    navPermission: 'assessment.homework.approve_homework',
    filter: 'status',
    tool: 'homeworkApproval',
  },
  {
    id: 'ASM-05',
    resource: 'assessment.Assignment',
    group: 'assessment',
    detail: 'ASM-06',
    tool: 'assignment',
  },
  { id: 'ASM-08', resource: 'assessment.ExamSession', group: 'assessment', tool: 'examSession' },
  { id: 'ASM-09', resource: 'assessment.Exam', group: 'assessment', detail: 'ASM-10', tool: 'exam' },
  { id: 'ASM-11', resource: 'assessment.Marksheet', group: 'assessment', tool: 'marksheet' },
  { id: 'LMS-43', resource: 'lms.Question', group: 'learning' },
  { id: 'LMS-27', resource: 'lms.LiveClass', group: 'learning' },
  { id: 'LMS-26', resource: 'lms.Forum', group: 'learning' },
  { id: 'LMS-15', resource: 'lms.Certificate', group: 'learning' },
  { id: 'LMS-16', resource: 'lms.CertificateRequest', group: 'learning' },
  { id: 'LMS-14', resource: 'lms.Annotation', group: 'learning' },
  { id: 'LMS-49', resource: 'lms.ExternalTool', group: 'setup' },
  { id: 'LMS-52', resource: 'lms.PlagiarismAccount', group: 'setup' },
  { id: 'LMS-55', resource: 'lms.Mentorship', group: 'learning' },
  { id: 'FIN-09', resource: 'finance.Coupon', group: 'setup' },
  { id: 'HR-01', resource: 'hr.Staff', group: 'people' },
  { id: 'HR-03', resource: 'hr.LeaveType', group: 'setup' },
  { id: 'LIB-01', resource: 'library.Media', group: 'services', detail: 'LIB-02' },
  { id: 'LIB-03', resource: 'library.Loan', group: 'services' },
  { id: 'LIB-04', resource: 'library.LibraryCard', group: 'services' },
  { id: 'LIB-05', resource: 'library.Reservation', group: 'services' },
  { id: 'LIB-06', resource: 'library.PurchaseRequest', group: 'services' },
  { id: 'TRN-01', resource: 'transport.Route', group: 'setup' },
  { id: 'TRN-02', resource: 'transport.StudentTransport', group: 'services' },
  { id: 'FO-01', resource: 'frontoffice.VisitorLog', group: 'services' },
  { id: 'FO-02', resource: 'frontoffice.CallLog', group: 'services' },
  { id: 'FO-03', resource: 'frontoffice.PostalRecord', group: 'services' },
  { id: 'FO-04', resource: 'frontoffice.DirectoryContact', group: 'services' },
  { id: 'COM-04', resource: 'comms.Event', group: 'communication' },
  { id: 'COM-06', resource: 'comms.OutboundMessage', group: 'communication' },
  { id: 'COM-07', resource: 'comms.FeedbackThread', group: 'communication' },
  { id: 'COM-08', resource: 'comms.ClassPage', group: 'communication' },
  { id: 'COM-09', resource: 'comms.Task', group: 'communication' },
  { id: 'COM-10', resource: 'comms.AudienceGroup', group: 'communication' },
  { id: 'COM-11', resource: 'comms.MailboxMessage', group: 'communication' },
  { id: 'COM-12', resource: 'comms.Magazine', group: 'communication' },
  { id: 'ADM-02', resource: 'org.Institution', group: 'setup', detail: 'ADM-03' },
  { id: 'ADM-04', resource: 'org.Department', group: 'setup' },
  { id: 'ADM-05', resource: 'org.AcademicYear', group: 'setup' },
  { id: 'ADM-06', resource: 'org.Holiday', group: 'setup' },
  { id: 'ADM-10', resource: 'iam.Role', group: 'setup' },
  { id: 'LMS-34', resource: 'lms.ItemOverride', group: 'learning' },
];
