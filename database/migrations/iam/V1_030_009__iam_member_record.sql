-- iam.member_record: the staff records (hr.staff) and student records (sis.student) of each login account and person,
-- kept from hr's and sis's events (against the DAG, events only), so that iam.GetMe answers staffIds and studentIds
-- from iam's own schema — without calling hr or sis on every sign-in, and without the caller needing the permission
-- to read staff or students. Events can arrive late and more than once: a row keeps the source row version it last
-- applied and ignores older ones, and a deleted record stays as a tombstone (is_deleted) so a late "created" or
-- "updated" event cannot bring it back.
CREATE TABLE iam.member_record (
    tenant_id uuid NOT NULL,
    record_id uuid NOT NULL,
    record_type varchar(20) NOT NULL,
    user_id uuid,
    person_id uuid,
    institution_id uuid,
    is_deleted boolean NOT NULL DEFAULT false,
    source_version integer NOT NULL,
    synced_at timestamptz NOT NULL,
    CONSTRAINT pk_iam_member_record PRIMARY KEY (tenant_id, record_id),
    CONSTRAINT ck_member_record_record_type CHECK (record_type IN ('staff', 'student')),
    CONSTRAINT ck_member_record_source_version CHECK (source_version >= 1)
);
COMMENT ON TABLE iam.member_record IS 'Staff and student records of login accounts and persons, kept from hr.staff and sis.student events so iam.GetMe can name them (Me.staffIds, Me.studentIds) from its own schema.';
COMMENT ON COLUMN iam.member_record.tenant_id IS 'Owning tenant (customer organisation). Row-level security key.';
COMMENT ON COLUMN iam.member_record.record_id IS 'hr.staff.id or sis.student.id (logical, maintained from their events).';
COMMENT ON COLUMN iam.member_record.record_type IS 'staff (hr.staff) or student (sis.student).';
COMMENT ON COLUMN iam.member_record.user_id IS 'Login account of the record (iam.user_account.id); NULL when it has none.';
COMMENT ON COLUMN iam.member_record.person_id IS 'Person of the record (people.person.id).';
COMMENT ON COLUMN iam.member_record.institution_id IS 'Institution of the record (org.institution.id).';
COMMENT ON COLUMN iam.member_record.is_deleted IS 'The record was deleted in its module; kept so a late event cannot restore it.';
COMMENT ON COLUMN iam.member_record.source_version IS 'Row version of the hr.staff / sis.student row this entry reflects; an event with an older version is ignored.';
COMMENT ON COLUMN iam.member_record.synced_at IS 'When the row was last changed by an event.';
CREATE INDEX ix_member_record_user ON iam.member_record (tenant_id, user_id) WHERE NOT is_deleted AND user_id IS NOT NULL;
CREATE INDEX ix_member_record_person ON iam.member_record (tenant_id, person_id) WHERE NOT is_deleted AND person_id IS NOT NULL;
COMMENT ON INDEX iam.ix_member_record_user IS 'iam.GetMe: the records of the signed-in account.';
COMMENT ON INDEX iam.ix_member_record_person IS 'iam.GetMe: the records of the signed-in person.';
ALTER TABLE iam.member_record ENABLE ROW LEVEL SECURITY;
ALTER TABLE iam.member_record FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON iam.member_record USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
