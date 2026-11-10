-- One attendance sheet per timetable session (flow F3: attendance.TakeAttendanceForSession gets or creates it).
-- The rule was only implied by the workflow; two callers who resolved different registers for the same session could
-- each create a sheet. The partial unique index makes the database refuse the second one, whoever writes it.

CREATE UNIQUE INDEX uq_attendance_sheet_session ON attendance.attendance_sheet (tenant_id, timetable_session_id) WHERE deleted_at IS NULL AND timetable_session_id IS NOT NULL;
