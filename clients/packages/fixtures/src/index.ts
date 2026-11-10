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
