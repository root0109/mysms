export * from './data';
export { fakeApi, json, problem, type Handler } from './fakeApi';
export { onePage, pageOf } from './paging';
export { fakeBackend, type FakeBackendOptions } from './handlers';
export { fakeRecords, type FakeRecordType } from './records';
export {
  ATTENDANCE_TEACHER,
  attendanceFixture,
  fakeAttendance,
  type FakeAttendanceOptions,
} from './attendance';
export {
  HOMEWORK_REVIEWER,
  HOMEWORK_TEACHER,
  fakeHomework,
  homeworkFixture,
  type FakeHomeworkOptions,
} from './homework';
export { TIMETABLE_TEACHER, fakeTimetable, timetableFixture } from './timetable';
export { LEAVE_APPROVER, LEAVE_STAFF, fakeLeave, leaveFixture } from './leave';
export { HR_OFFICER, fakeStaff, staffFixture } from './staff';
export { PAYROLL_OFFICER, PAYSLIP_READER, fakePayroll, payrollFixture } from './payroll';
export { FEE_FAMILY, FEE_OFFICE, fakeFinance, financeFixture } from './finance';
export { SIS_OFFICE, SIS_PROMOTER, fakeSis, sisFixture } from './sis';
export {
  ADMISSION_APPLICANT,
  ADMISSION_OFFICE,
  admissionFixture,
  fakeAdmission,
  type FakeAdmissionOptions,
} from './admission';
export { ACCOUNT_ADMIN, fakeIam, fakeJwt, iamFixture, type FakeIamOptions } from './iam';
export { PLATFORM_ADMIN, fakePlatform, platformFixture } from './platform';
export {
  LMS_ADMIN,
  LMS_AUTHOR,
  LMS_STUDENT,
  LMS_TEACHER,
  fakeLms,
  lmsFixture,
  type FakeLmsOptions,
} from './lms';
