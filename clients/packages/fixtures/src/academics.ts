import { attendanceFixture, fakeAttendance } from './attendance';
import type { Handler } from './fakeApi';
import { fakeRecords } from './records';
import { fakeTimetable, timetableFixture } from './timetable';

const id = (suffix: string) => `01960000-0000-7000-8000-${suffix.padStart(12, '0')}`;
const INSTITUTION = attendanceFixture.institutionId;

/**
 * The academic structure around the attendance fixture's class (Grade 5 A): its grade and section, its subjects with
 * their teachers, a course with a subject, a register, homework, and two staff members with a day of staff
 * attendance (one record from the biometric device). Names come from people like everywhere else.
 */
export const academicsFixture = {
  classSectionId: attendanceFixture.classSectionId,
  gradeId: id('b01'),
  sectionId: id('b02'),
  courseId: id('b11'),
  mathsClassSubjectId: id('b21'),
  scienceClassSubjectId: id('b22'),
  teacherId: timetableFixture.staffId,
  clerkId: id('b31'),
  day: '2026-03-02',
} as const;

/** What a school administrator needs for the academics screens and staff attendance. */
export const ACADEMICS_ADMIN = [
  'academics.course.read',
  'academics.course_subject.read',
  'academics.course_subject.create',
  'academics.grade_level.read',
  'academics.grade_level.create',
  'academics.section.read',
  'academics.class_section.read',
  'academics.class_subject.read',
  'academics.teaching_assignment.read',
  'academics.subject.read',
  'sis.student_enrollment.list_class_roster',
  'sis.student.read',
  'people.person.read',
  'hr.staff.read',
  'timetable.session.get_timetable',
  'facility.room.read',
  'attendance.attendance_register.read',
  'assessment.homework.read',
  'time_attendance.staff_attendance.read',
  'time_attendance.staff_attendance.create',
  'time_attendance.staff_attendance.update',
] as const;

/**
 * The attendance and timetable fakes (the class roster and student names, the week of sessions, subject and room
 * names) with the academic records over the generic handlers, staff and persons for names, and staff attendance.
 */
export function fakeAcademics(): Record<string, Handler> {
  const f = academicsFixture;
  const teacherPersonId = id('b41');
  const clerkPersonId = id('b42');
  return {
    ...fakeAttendance(),
    ...fakeTimetable(),
    ...fakeRecords({
      collection: '/v1/people/persons',
      item: '/v1/people/persons/{personId}',
      required: ['displayName'],
      rows: [
        ...attendanceFixture.students.map((student) => ({ id: student.personId, displayName: student.name })),
        { id: teacherPersonId, displayName: 'Kavita Iyer' },
        { id: clerkPersonId, displayName: 'Anil Das' },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/hr/staff',
      item: '/v1/hr/staff/{staffId}',
      required: ['personId'],
      rows: [
        {
          id: f.teacherId,
          personId: teacherPersonId,
          institutionId: INSTITUTION,
          employeeCode: 'T-1',
          status: 'active',
        },
        {
          id: f.clerkId,
          personId: clerkPersonId,
          institutionId: INSTITUTION,
          employeeCode: 'C-1',
          status: 'active',
        },
        {
          id: id('b33'),
          personId: id('b43'),
          institutionId: INSTITUTION,
          employeeCode: 'X-1',
          status: 'exited',
        },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/academics/class-sections',
      item: '/v1/academics/class-sections/{classSectionId}',
      required: ['institutionId'],
      rows: [
        {
          id: f.classSectionId,
          institutionId: INSTITUTION,
          academicYearId: id('b05'),
          gradeLevelId: f.gradeId,
          sectionId: f.sectionId,
          capacity: 40,
          isActive: true,
        },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/academics/grade-levels',
      item: '/v1/academics/grade-levels/{gradeLevelId}',
      required: ['institutionId', 'name', 'slug'],
      rows: [{ id: f.gradeId, institutionId: INSTITUTION, name: 'Grade 5', slug: 'grade-5', sortOrder: 5 }],
    }),
    ...fakeRecords({
      collection: '/v1/academics/sections',
      item: '/v1/academics/sections/{sectionId}',
      required: ['institutionId', 'name'],
      rows: [{ id: f.sectionId, institutionId: INSTITUTION, name: 'A', code: 'A', sortOrder: 1 }],
    }),
    ...fakeRecords({
      collection: '/v1/academics/class-sections/{classSectionId}/subjects',
      item: '/v1/academics/class-sections/{classSectionId}/subjects/{classSubjectId}',
      required: ['subjectId'],
      rows: [
        {
          id: f.mathsClassSubjectId,
          classSectionId: f.classSectionId,
          subjectId: timetableFixture.subjects[0].id,
          offeringType: 'core',
        },
        {
          id: f.scienceClassSubjectId,
          classSectionId: f.classSectionId,
          subjectId: timetableFixture.subjects[1].id,
          offeringType: 'core',
        },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/academics/teaching-assignments',
      item: '/v1/academics/teaching-assignments/{teachingAssignmentId}',
      required: ['classSubjectId', 'staffId'],
      rows: [
        { id: id('b51'), classSubjectId: f.mathsClassSubjectId, staffId: f.teacherId, periodsPerWeek: 6 },
        { id: id('b52'), classSubjectId: f.mathsClassSubjectId, staffId: f.clerkId, isActive: false },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/academics/courses',
      item: '/v1/academics/courses/{courseId}',
      required: ['institutionId', 'code', 'name'],
      rows: [
        { id: f.courseId, institutionId: INSTITUTION, code: 'BSC', name: 'B.Sc. Physics', isActive: true },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/academics/courses/{courseId}/subjects',
      item: '/v1/academics/courses/{courseId}/subjects/{courseSubjectId}',
      required: ['subjectId'],
      rows: [
        {
          id: id('b61'),
          courseId: f.courseId,
          subjectId: timetableFixture.subjects[0].id,
          termNumber: 1,
          credits: '4',
        },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/attendance/attendance-registers',
      item: '/v1/attendance/attendance-registers/{attendanceRegisterId}',
      required: ['institutionId', 'code', 'name', 'registerScope'],
      rows: [
        {
          id: attendanceFixture.registerId,
          institutionId: INSTITUTION,
          code: 'G5A',
          name: 'Grade 5 A daily',
          registerScope: 'class_section',
          classSectionId: f.classSectionId,
        },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/assessment/homeworks',
      item: '/v1/assessment/homeworks/{homeworkId}',
      required: ['classSectionId'],
      rows: [
        {
          id: id('b71'),
          institutionId: INSTITUTION,
          classSectionId: f.classSectionId,
          subjectId: timetableFixture.subjects[1].id,
          dueAt: '2026-03-04T10:00:00Z',
          status: 'published',
        },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/time-attendance/staff-attendance-days',
      item: '/v1/time-attendance/staff-attendance-days/{staffAttendanceId}',
      required: ['staffId', 'attendanceDate', 'status'],
      rows: [
        {
          id: id('b81'),
          staffId: f.clerkId,
          attendanceDate: f.day,
          status: 'present',
          source: 'biometric',
          checkInAt: '2026-03-02T03:05:00Z',
        },
        {
          id: id('b82'),
          staffId: f.teacherId,
          attendanceDate: '2026-03-01',
          status: 'absent',
          source: 'manual',
        },
      ],
    }),
  };
}
