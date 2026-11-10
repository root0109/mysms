-- timetable.GetTimetable reads the sessions of one teacher, class, batch or room over a range of days, in start
-- order. The single-column indexes of the baseline find the teacher or the class but then sort every session they
-- ever had; these lead with the subject of the view and end with the start, so a week is one short index range.
CREATE INDEX ix_session_staff_start ON timetable.session (tenant_id, staff_id, start_at) WHERE deleted_at IS NULL AND staff_id IS NOT NULL;
CREATE INDEX ix_session_class_section_start ON timetable.session (tenant_id, class_section_id, start_at) WHERE deleted_at IS NULL AND class_section_id IS NOT NULL;
CREATE INDEX ix_session_batch_start ON timetable.session (tenant_id, batch_id, start_at) WHERE deleted_at IS NULL AND batch_id IS NOT NULL;
CREATE INDEX ix_session_room_start ON timetable.session (tenant_id, room_id, start_at) WHERE deleted_at IS NULL AND room_id IS NOT NULL;
