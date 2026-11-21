-- MySMS PostgreSQL schema. Module: privacy — Privacy and Consent Centre
-- (docs/design/PRIVACY-AND-CONSENT-CENTRE.md §5). Until the first release this baseline is edited in place.

CREATE SCHEMA IF NOT EXISTS privacy;
COMMENT ON SCHEMA privacy IS 'Privacy and Consent Centre: the purpose catalogue, each tenant''s purpose settings, notices, consent records and evidence, the effective consent state, rights requests, retention and breaches.';

-- === Global catalogue (no tenant_id; maintained by the platform through this baseline) ===

CREATE TABLE privacy.purpose (
    id uuid NOT NULL,
    code varchar(40) NOT NULL,
    scope_kind varchar(12) NOT NULL,
    fiduciary varchar(10) NOT NULL DEFAULT 'tenant',
    forbidden_for_minors boolean NOT NULL DEFAULT false,
    majority_grace varchar(20) NOT NULL DEFAULT 'none',
    default_valid_months integer,
    external_code varchar(80),
    status varchar(12) NOT NULL DEFAULT 'active',
    version integer NOT NULL DEFAULT 1,
    CONSTRAINT pk_purpose PRIMARY KEY (id),
    CONSTRAINT uq_purpose_code UNIQUE (code),
    CONSTRAINT ck_purpose_scope_kind CHECK (scope_kind IN ('width', 'tenant')),
    CONSTRAINT ck_purpose_fiduciary CHECK (fiduciary IN ('tenant', 'platform')),
    CONSTRAINT ck_purpose_majority_grace CHECK (majority_grace IN ('none', 'keep_published', 'keep_narrow_widths')),
    CONSTRAINT ck_purpose_default_valid_months CHECK (default_valid_months IS NULL OR default_valid_months > 0),
    CONSTRAINT ck_purpose_status CHECK (status IN ('active', 'deprecated', 'retired')),
    CONSTRAINT ck_purpose_version CHECK (version > 0)
);
COMMENT ON TABLE privacy.purpose IS 'Why personal data is processed: the platform-wide purpose catalogue. A tenant switches a purpose on in privacy.tenant_purpose; consents are given per purpose.';
COMMENT ON COLUMN privacy.purpose.id IS 'Stable id, the same in every environment: md5(''mysms.purpose:'' || code)::uuid.';
COMMENT ON COLUMN privacy.purpose.code IS 'Purpose code, e.g. photo_video_publication.';
COMMENT ON COLUMN privacy.purpose.scope_kind IS 'width: a consent names how far the processing reaches (class < institution < inter_school < public); tenant: one decision for the whole tenant.';
COMMENT ON COLUMN privacy.purpose.fiduciary IS 'Who decides the processing: the tenant (the school) or the platform (MySMS on its own account).';
COMMENT ON COLUMN privacy.purpose.forbidden_for_minors IS 'Never allowed for a minor, not even with a guardian''s consent (DPDP Act s.9(3)).';
COMMENT ON COLUMN privacy.purpose.majority_grace IS 'What a guardian''s consent still covers once the student turns 18: none, keep_published (items already published stay), keep_narrow_widths.';
COMMENT ON COLUMN privacy.purpose.default_valid_months IS 'How long a consent lasts unless the notice says otherwise; NULL = until withdrawn.';
COMMENT ON COLUMN privacy.purpose.external_code IS 'Code of the purpose at a consent manager (future).';
COMMENT ON COLUMN privacy.purpose.status IS 'active; deprecated (no new consents, existing ones still count); retired.';
COMMENT ON COLUMN privacy.purpose.version IS 'Incremented on a material change of the purpose.';

-- The purposes of the design's enforcement points (§4.1, §8). Forbidden for minors: tracking, behavioural monitoring
-- and targeted advertising (DPDP Act s.9(3)), and video or biometric monitoring until counsel approves (§8).
INSERT INTO privacy.purpose (id, code, scope_kind, fiduciary, forbidden_for_minors, majority_grace) VALUES
    (md5('mysms.purpose:' || 'behavioural_analytics')::uuid, 'behavioural_analytics', 'tenant', 'tenant', true, 'none'),
    (md5('mysms.purpose:' || 'biometric_data')::uuid, 'biometric_data', 'tenant', 'tenant', false, 'none'),
    (md5('mysms.purpose:' || 'biometric_or_video_monitoring')::uuid, 'biometric_or_video_monitoring', 'tenant', 'tenant', true, 'none'),
    (md5('mysms.purpose:' || 'community_naming')::uuid, 'community_naming', 'width', 'tenant', false, 'keep_published'),
    (md5('mysms.purpose:' || 'community_participation')::uuid, 'community_participation', 'tenant', 'tenant', false, 'none'),
    (md5('mysms.purpose:' || 'health_record_keeping')::uuid, 'health_record_keeping', 'tenant', 'tenant', false, 'none'),
    (md5('mysms.purpose:' || 'learning_analytics')::uuid, 'learning_analytics', 'tenant', 'tenant', false, 'none'),
    (md5('mysms.purpose:' || 'marketing_comms')::uuid, 'marketing_comms', 'tenant', 'tenant', false, 'none'),
    (md5('mysms.purpose:' || 'photo_video_publication')::uuid, 'photo_video_publication', 'width', 'tenant', false, 'keep_published'),
    (md5('mysms.purpose:' || 'product_analytics')::uuid, 'product_analytics', 'tenant', 'platform', true, 'none'),
    (md5('mysms.purpose:' || 'public_profile')::uuid, 'public_profile', 'tenant', 'tenant', false, 'none'),
    (md5('mysms.purpose:' || 'sms_whatsapp_messages')::uuid, 'sms_whatsapp_messages', 'tenant', 'tenant', false, 'none'),
    (md5('mysms.purpose:' || 'targeted_advertising')::uuid, 'targeted_advertising', 'tenant', 'tenant', true, 'none'),
    (md5('mysms.purpose:' || 'third_party_ai_processing')::uuid, 'third_party_ai_processing', 'tenant', 'tenant', false, 'none'),
    (md5('mysms.purpose:' || 'third_party_payment')::uuid, 'third_party_payment', 'tenant', 'tenant', false, 'none'),
    (md5('mysms.purpose:' || 'third_party_sharing')::uuid, 'third_party_sharing', 'tenant', 'tenant', false, 'none'),
    (md5('mysms.purpose:' || 'transport_live_location')::uuid, 'transport_live_location', 'tenant', 'tenant', false, 'none');

-- === Tenant settings and notices ===

CREATE TABLE privacy.notice (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    code varchar(60) NOT NULL,
    title varchar(200) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_notice PRIMARY KEY (id),
    CONSTRAINT uq_notice_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_notice_code CHECK (code ~ '^[a-z][a-z0-9_]{1,59}$')
);
COMMENT ON TABLE privacy.notice IS 'A privacy notice of a tenant: the text shown when consent is asked for one purpose or a bundle of purposes. Its wording lives in versions; set by tenant administrators only.';
COMMENT ON COLUMN privacy.notice.id IS 'Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).';
COMMENT ON COLUMN privacy.notice.tenant_id IS 'Owning tenant (customer organisation). Row-level security key.';
COMMENT ON COLUMN privacy.notice.code IS 'Code of the notice within the tenant, e.g. community_photos; set at creation and never changed.';
COMMENT ON COLUMN privacy.notice.title IS 'Title shown to tenant administrators.';
COMMENT ON COLUMN privacy.notice.created_at IS 'Row creation time.';
COMMENT ON COLUMN privacy.notice.created_by IS 'User account that created the row (audit reference to iam.user_account; not FK-constrained).';
COMMENT ON COLUMN privacy.notice.updated_at IS 'Last modification time (set by the application on every update).';
COMMENT ON COLUMN privacy.notice.updated_by IS 'User account that last modified the row (audit reference; not FK-constrained).';
COMMENT ON COLUMN privacy.notice.deleted_at IS 'Soft-delete timestamp (NULL = live row).';
COMMENT ON COLUMN privacy.notice.row_version IS 'Optimistic-lock version, incremented on every update; used as the ETag.';
CREATE UNIQUE INDEX uq_notice_code ON privacy.notice (tenant_id, code) WHERE deleted_at IS NULL;

CREATE TABLE privacy.notice_version (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    notice_id uuid NOT NULL,
    version integer NOT NULL,
    status varchar(10) NOT NULL DEFAULT 'draft',
    requires_reconsent boolean NOT NULL DEFAULT false,
    purpose_codes varchar(40)[] NOT NULL,
    grievance_contact jsonb NOT NULL,
    published_at timestamptz,
    superseded_by_id uuid,
    document_file_id uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_notice_version PRIMARY KEY (id),
    CONSTRAINT uq_notice_version_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT uq_notice_version_number UNIQUE (tenant_id, notice_id, version),
    CONSTRAINT fk_notice_version_notice_id FOREIGN KEY (notice_id, tenant_id) REFERENCES privacy.notice (id, tenant_id),
    CONSTRAINT fk_notice_version_superseded_by_id FOREIGN KEY (superseded_by_id, tenant_id)
        REFERENCES privacy.notice_version (id, tenant_id),
    CONSTRAINT ck_notice_version_version CHECK (version > 0),
    CONSTRAINT ck_notice_version_status CHECK (status IN ('draft', 'active', 'retired')),
    CONSTRAINT ck_notice_version_purpose_codes CHECK (cardinality(purpose_codes) > 0)
);
COMMENT ON TABLE privacy.notice_version IS 'One version of a notice: draft while it is written, active once published (one per notice), retired when a newer version is published. Published versions never change and are never deleted.';
COMMENT ON COLUMN privacy.notice_version.id IS 'Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).';
COMMENT ON COLUMN privacy.notice_version.tenant_id IS 'Owning tenant (customer organisation). Row-level security key.';
COMMENT ON COLUMN privacy.notice_version.notice_id IS 'The notice this is a version of (privacy.notice).';
COMMENT ON COLUMN privacy.notice_version.version IS 'Version number within the notice: 1, 2, 3 in creation order.';
COMMENT ON COLUMN privacy.notice_version.status IS 'draft (editable); active (published, shown); retired (superseded by a newer published version).';
COMMENT ON COLUMN privacy.notice_version.requires_reconsent IS 'Whether consents given under earlier versions must be asked for again once this version is published.';
COMMENT ON COLUMN privacy.notice_version.purpose_codes IS 'Codes of the catalogue purposes (privacy.purpose.code) the version asks consent for.';
COMMENT ON COLUMN privacy.notice_version.grievance_contact IS 'The school''s grievance officer named by the version: name, role, email, phone.';
COMMENT ON COLUMN privacy.notice_version.published_at IS 'When the version was published (NULL while a draft).';
COMMENT ON COLUMN privacy.notice_version.superseded_by_id IS 'The version that retired this one.';
COMMENT ON COLUMN privacy.notice_version.document_file_id IS 'Optional signed PDF of the version (logical reference to platform.file_object).';
COMMENT ON COLUMN privacy.notice_version.created_at IS 'Row creation time.';
COMMENT ON COLUMN privacy.notice_version.created_by IS 'User account that created the row (audit reference to iam.user_account; not FK-constrained).';
COMMENT ON COLUMN privacy.notice_version.updated_at IS 'Last modification time (set by the application on every update).';
COMMENT ON COLUMN privacy.notice_version.updated_by IS 'User account that last modified the row (audit reference; not FK-constrained).';
COMMENT ON COLUMN privacy.notice_version.row_version IS 'Optimistic-lock version, incremented on every update; used as the ETag.';
CREATE UNIQUE INDEX uq_notice_version_active ON privacy.notice_version (tenant_id, notice_id) WHERE status = 'active';
CREATE INDEX ix_notice_version_notice_id ON privacy.notice_version (notice_id);
CREATE INDEX ix_notice_version_superseded_by_id ON privacy.notice_version (superseded_by_id);

CREATE TABLE privacy.notice_translation (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    notice_version_id uuid NOT NULL,
    language varchar(10) NOT NULL,
    body_markdown text NOT NULL,
    summary_for_children text,
    content_sha256 char(64) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_notice_translation PRIMARY KEY (id),
    CONSTRAINT fk_notice_translation_notice_version_id FOREIGN KEY (notice_version_id, tenant_id)
        REFERENCES privacy.notice_version (id, tenant_id),
    CONSTRAINT ck_notice_translation_language CHECK (language ~ '^[a-z]{2,3}(-[A-Z]{2})?$'),
    CONSTRAINT ck_notice_translation_content_sha256 CHECK (content_sha256 ~ '^[0-9a-f]{64}$')
);
COMMENT ON TABLE privacy.notice_translation IS 'The wording of a notice version in one language; changes only while its version is a draft.';
COMMENT ON COLUMN privacy.notice_translation.id IS 'Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).';
COMMENT ON COLUMN privacy.notice_translation.tenant_id IS 'Owning tenant (customer organisation). Row-level security key.';
COMMENT ON COLUMN privacy.notice_translation.notice_version_id IS 'The version this is a translation of (privacy.notice_version).';
COMMENT ON COLUMN privacy.notice_translation.language IS 'BCP 47 language code: en, hi, ta, mr-IN ...; one live translation per language and version.';
COMMENT ON COLUMN privacy.notice_translation.body_markdown IS 'The notice in Markdown: the data processed, the purposes, how to withdraw and how to complain.';
COMMENT ON COLUMN privacy.notice_translation.summary_for_children IS 'Short plain version shown to a minor alongside the notice.';
COMMENT ON COLUMN privacy.notice_translation.content_sha256 IS 'Lowercase hex SHA-256 of what is shown (body, a blank line, the summary), computed by the application and copied into consent evidence.';
COMMENT ON COLUMN privacy.notice_translation.created_at IS 'Row creation time.';
COMMENT ON COLUMN privacy.notice_translation.created_by IS 'User account that created the row (audit reference to iam.user_account; not FK-constrained).';
COMMENT ON COLUMN privacy.notice_translation.updated_at IS 'Last modification time (set by the application on every update).';
COMMENT ON COLUMN privacy.notice_translation.updated_by IS 'User account that last modified the row (audit reference; not FK-constrained).';
COMMENT ON COLUMN privacy.notice_translation.deleted_at IS 'Soft-delete timestamp (NULL = live row).';
COMMENT ON COLUMN privacy.notice_translation.row_version IS 'Optimistic-lock version, incremented on every update; used as the ETag.';
CREATE UNIQUE INDEX uq_notice_translation_language ON privacy.notice_translation (tenant_id, notice_version_id, language)
    WHERE deleted_at IS NULL;
CREATE INDEX ix_notice_translation_notice_version_id ON privacy.notice_translation (notice_version_id);

CREATE TABLE privacy.tenant_purpose (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    purpose_id uuid NOT NULL,
    enabled boolean NOT NULL DEFAULT false,
    max_scope varchar(20),
    notice_id uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_tenant_purpose PRIMARY KEY (id),
    CONSTRAINT uq_tenant_purpose UNIQUE (tenant_id, purpose_id),
    CONSTRAINT uq_tenant_purpose_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT fk_tenant_purpose_purpose_id FOREIGN KEY (purpose_id) REFERENCES privacy.purpose (id),
    CONSTRAINT fk_tenant_purpose_notice_id FOREIGN KEY (notice_id, tenant_id) REFERENCES privacy.notice (id, tenant_id),
    CONSTRAINT ck_tenant_purpose_max_scope CHECK (max_scope IN ('class', 'institution', 'inter_school', 'public'))
);
COMMENT ON TABLE privacy.tenant_purpose IS 'A tenant''s setting of one catalogue purpose; no row = switched off. Set by tenant administrators only.';
COMMENT ON COLUMN privacy.tenant_purpose.id IS 'Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).';
COMMENT ON COLUMN privacy.tenant_purpose.tenant_id IS 'Owning tenant (customer organisation). Row-level security key.';
COMMENT ON COLUMN privacy.tenant_purpose.purpose_id IS 'The catalogue purpose (privacy.purpose).';
COMMENT ON COLUMN privacy.tenant_purpose.enabled IS 'Whether the tenant processes data for the purpose; off until switched on with an active notice.';
COMMENT ON COLUMN privacy.tenant_purpose.max_scope IS 'The widest width the tenant allows for a width purpose (e.g. never inter_school); NULL = no ceiling, or a tenant-scoped purpose.';
COMMENT ON COLUMN privacy.tenant_purpose.notice_id IS 'The notice shown when asking for consent to the purpose (privacy.notice); its active version must list the purpose while the purpose is enabled.';
COMMENT ON COLUMN privacy.tenant_purpose.created_at IS 'Row creation time.';
COMMENT ON COLUMN privacy.tenant_purpose.created_by IS 'User account that created the row (audit reference to iam.user_account; not FK-constrained).';
COMMENT ON COLUMN privacy.tenant_purpose.updated_at IS 'Last update time.';
COMMENT ON COLUMN privacy.tenant_purpose.updated_by IS 'User account that last updated the row (audit reference to iam.user_account; not FK-constrained).';
COMMENT ON COLUMN privacy.tenant_purpose.row_version IS 'Optimistic-locking version.';

-- === Subjects and guardian links: copies kept from people's and sis's events (design §5.3) ===

CREATE TABLE privacy.subject (
    tenant_id uuid NOT NULL,
    person_id uuid NOT NULL,
    status varchar(8) NOT NULL DEFAULT 'unknown',
    birth_date date,
    majority_date date,
    student_ids uuid[] NOT NULL DEFAULT '{}',
    user_ids uuid[] NOT NULL DEFAULT '{}',
    institution_ids uuid[] NOT NULL DEFAULT '{}',
    student_records jsonb NOT NULL DEFAULT '{}',
    person_row_version integer NOT NULL DEFAULT 0,
    state_version bigint NOT NULL DEFAULT 0,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_subject PRIMARY KEY (tenant_id, person_id),
    CONSTRAINT ck_subject_status CHECK (status IN ('minor', 'adult', 'unknown')),
    CONSTRAINT ck_subject_birth_date CHECK ((birth_date IS NULL) = (status = 'unknown')),
    CONSTRAINT ck_subject_majority_date CHECK ((birth_date IS NULL) = (majority_date IS NULL)),
    CONSTRAINT ck_subject_person_row_version CHECK (person_row_version >= 0),
    CONSTRAINT ck_subject_state_version CHECK (state_version >= 0)
);
COMMENT ON TABLE privacy.subject IS 'A data principal known to privacy: one person of a tenant, kept from people.person events (birth date, majority) and sis.student events (student records, accounts, institutions); privacy never reads another schema. A row may exist before its person event arrives (status unknown).';
COMMENT ON COLUMN privacy.subject.tenant_id IS 'Owning tenant (customer organisation). Row-level security key.';
COMMENT ON COLUMN privacy.subject.person_id IS 'The person (people.person.id; logical, maintained from its events).';
COMMENT ON COLUMN privacy.subject.status IS 'minor (majority_date after today), adult (majority_date reached) or unknown (no birth date known; treated as a minor).';
COMMENT ON COLUMN privacy.subject.birth_date IS 'Date of birth from the person''s events; kept only to compute the age of majority. NULL = unknown or the person was deleted.';
COMMENT ON COLUMN privacy.subject.majority_date IS 'birth_date plus the age of majority (mysms.privacy.age-of-majority): the day the person becomes an adult.';
COMMENT ON COLUMN privacy.subject.student_ids IS 'The live student records of the person (sis.student.id), derived from student_records.';
COMMENT ON COLUMN privacy.subject.user_ids IS 'Login accounts of those student records (iam.user_account.id), derived from student_records.';
COMMENT ON COLUMN privacy.subject.institution_ids IS 'Institutions of those student records (org.institution.id), derived from student_records.';
COMMENT ON COLUMN privacy.subject.student_records IS 'Each sis.student record ever seen for the person, by id: {userId, institutionId, rowVersion, live}. An event changes an entry only to a newer row version; a deleted record, or one moved to another person, stays as a tombstone (live false) so a late event cannot bring it back.';
COMMENT ON COLUMN privacy.subject.person_row_version IS 'Row version of the people.person row the birth date reflects (0 = no person event yet); an older person event is ignored.';
COMMENT ON COLUMN privacy.subject.state_version IS 'Monotonic version of the person''s effective consent state (privacy.consent_state.state_version): incremented under the row lock whenever a recompute changes one of their states, so consumers drop stale privacy.consent_state.changed events.';
COMMENT ON COLUMN privacy.subject.updated_at IS 'When an event last changed the row.';
CREATE INDEX ix_subject_majority ON privacy.subject (tenant_id, majority_date) WHERE status <> 'adult';
CREATE INDEX ix_subject_student_records ON privacy.subject USING gin (student_records);
COMMENT ON INDEX privacy.ix_subject_majority IS 'The minors who come of age by a date (the age-of-majority transition).';
COMMENT ON INDEX privacy.ix_subject_student_records IS 'The subjects that hold an entry of a student record, found when its event arrives (containment by key).';

CREATE TABLE privacy.guardian_link (
    tenant_id uuid NOT NULL,
    link_id uuid NOT NULL,
    student_person_id uuid NOT NULL,
    guardian_person_id uuid NOT NULL,
    guardian_user_id uuid,
    is_legal_guardian boolean NOT NULL,
    has_custody boolean NOT NULL,
    status varchar(10) NOT NULL,
    source_row_version integer NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_guardian_link PRIMARY KEY (tenant_id, link_id),
    CONSTRAINT ck_guardian_link_status CHECK (status IN ('active', 'inactive')),
    CONSTRAINT ck_guardian_link_source_row_version CHECK (source_row_version >= 1)
);
COMMENT ON TABLE privacy.guardian_link IS 'Who is whose guardian, kept from sis.student_guardian events: only an active link of a legal guardian lets its guardian give consent for a minor. A deleted link stays, inactive, so a late event cannot bring it back.';
COMMENT ON COLUMN privacy.guardian_link.tenant_id IS 'Owning tenant (customer organisation). Row-level security key.';
COMMENT ON COLUMN privacy.guardian_link.link_id IS 'The link (sis.student_guardian.id; logical, maintained from its events).';
COMMENT ON COLUMN privacy.guardian_link.student_person_id IS 'Person of the student (people.person.id, through sis.student).';
COMMENT ON COLUMN privacy.guardian_link.guardian_person_id IS 'Person of the guardian (people.person.id, through sis.guardian).';
COMMENT ON COLUMN privacy.guardian_link.guardian_user_id IS 'Login account of the guardian (iam.user_account.id) when the link''s event was published; NULL when it had none.';
COMMENT ON COLUMN privacy.guardian_link.is_legal_guardian IS 'Whether the guardian is the student''s legal guardian.';
COMMENT ON COLUMN privacy.guardian_link.has_custody IS 'Whether the guardian has custody of the student.';
COMMENT ON COLUMN privacy.guardian_link.status IS 'active, or inactive (set inactive in sis, or the link was deleted).';
COMMENT ON COLUMN privacy.guardian_link.source_row_version IS 'Row version of the sis.student_guardian row this copy reflects; an older event is ignored.';
COMMENT ON COLUMN privacy.guardian_link.updated_at IS 'When an event last changed the row.';
CREATE INDEX ix_guardian_link_student ON privacy.guardian_link (tenant_id, student_person_id) WHERE status = 'active';
CREATE INDEX ix_guardian_link_guardian ON privacy.guardian_link (tenant_id, guardian_person_id) WHERE status = 'active';
COMMENT ON INDEX privacy.ix_guardian_link_student IS 'The active guardians of a student.';
COMMENT ON INDEX privacy.ix_guardian_link_guardian IS 'The students an active guardian is linked to.';

-- === Consent records, their evidence and the effective consent state (design §5.4) ===

CREATE TABLE privacy.consent_record (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    subject_person_id uuid NOT NULL,
    giver_person_id uuid NOT NULL,
    giver_capacity varchar(10) NOT NULL,
    purpose_code varchar(40) NOT NULL,
    scope varchar(20) NOT NULL,
    decision varchar(8) NOT NULL,
    notice_version_id uuid NOT NULL,
    notice_language varchar(10) NOT NULL,
    valid_from timestamptz NOT NULL,
    valid_until date,
    withdrawn_at timestamptz,
    superseded_by_id uuid,
    channel varchar(20) NOT NULL,
    verification_status varchar(12) NOT NULL,
    verification_method varchar(30),
    verified_by_user_id uuid,
    verified_at timestamptz,
    recorded_by_user_id uuid,
    evidence_file_id uuid,
    external_artefact_id varchar(120),
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_consent_record PRIMARY KEY (id),
    CONSTRAINT uq_consent_record_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT fk_consent_record_subject_person_id FOREIGN KEY (tenant_id, subject_person_id)
        REFERENCES privacy.subject (tenant_id, person_id),
    CONSTRAINT fk_consent_record_purpose_code FOREIGN KEY (purpose_code) REFERENCES privacy.purpose (code),
    CONSTRAINT fk_consent_record_notice_version_id FOREIGN KEY (notice_version_id, tenant_id)
        REFERENCES privacy.notice_version (id, tenant_id),
    CONSTRAINT fk_consent_record_superseded_by_id FOREIGN KEY (superseded_by_id, tenant_id)
        REFERENCES privacy.consent_record (id, tenant_id) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT ck_consent_record_capacity CHECK (giver_capacity IN ('self', 'guardian', 'nominee')),
    CONSTRAINT ck_consent_record_scope CHECK (scope IN ('class', 'institution', 'inter_school', 'public', 'tenant')),
    CONSTRAINT ck_consent_record_decision CHECK (decision IN ('granted', 'refused')),
    CONSTRAINT ck_consent_record_notice_language CHECK (notice_language ~ '^[a-z]{2,3}(-[A-Z]{2})?$'),
    CONSTRAINT ck_consent_record_channel CHECK (channel IN ('web', 'mobile_app', 'paper_form', 'in_person', 'email',
        'consent_manager', 'import')),
    CONSTRAINT ck_consent_record_verification_status CHECK (verification_status IN ('verified', 'unverified', 'rejected')),
    CONSTRAINT ck_consent_record_verification_method CHECK (verification_method IN ('existing_record',
        'in_person_staff_check', 'digilocker_token', 'otp_on_registered_mobile', 'paper_form_countersigned')),
    CONSTRAINT ck_consent_record_verified CHECK (verification_status <> 'verified'
        OR (verified_at IS NOT NULL AND verification_method IS NOT NULL)),
    CONSTRAINT ck_consent_record_self CHECK (giver_capacity <> 'self' OR giver_person_id = subject_person_id),
    CONSTRAINT ck_consent_record_paper CHECK (channel <> 'paper_form'
        OR (evidence_file_id IS NOT NULL AND recorded_by_user_id IS NOT NULL)),
    CONSTRAINT ck_consent_record_verifier CHECK (verified_by_user_id IS NULL
        OR verified_by_user_id IS DISTINCT FROM recorded_by_user_id),
    CONSTRAINT ck_consent_record_withdrawn CHECK (withdrawn_at IS NULL OR withdrawn_at >= valid_from),
    CONSTRAINT ck_consent_record_superseded CHECK (superseded_by_id IS NULL OR superseded_by_id <> id),
    CONSTRAINT ck_consent_record_row_version CHECK (row_version >= 0)
);
COMMENT ON TABLE privacy.consent_record IS 'One consent decision: one giver''s granted or refused for one subject, purpose and width, under the notice version shown. The decision of a row never changes: a newer decision of the same giver supersedes it, a withdrawal ends it, valid_until lets it lapse; rows are never deleted.';
COMMENT ON COLUMN privacy.consent_record.id IS 'Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).';
COMMENT ON COLUMN privacy.consent_record.tenant_id IS 'Owning tenant (customer organisation). Row-level security key.';
COMMENT ON COLUMN privacy.consent_record.subject_person_id IS 'The person the decision is about (privacy.subject.person_id; people.person.id, logical).';
COMMENT ON COLUMN privacy.consent_record.giver_person_id IS 'The person who decided: the subject (self) or a legal guardian (people.person.id, logical); always the caller''s own person on self-service.';
COMMENT ON COLUMN privacy.consent_record.giver_capacity IS 'In which capacity the giver decided: self (an adult subject), guardian (a legal guardian of a minor), nominee (future).';
COMMENT ON COLUMN privacy.consent_record.purpose_code IS 'The catalogue purpose decided on (privacy.purpose.code).';
COMMENT ON COLUMN privacy.consent_record.scope IS 'The width the decision is for: class < institution < inter_school < public for a width purpose; tenant for a tenant-scoped one. A grant covers every narrower width, a refusal every wider one.';
COMMENT ON COLUMN privacy.consent_record.decision IS 'granted or refused; fixed for the row.';
COMMENT ON COLUMN privacy.consent_record.notice_version_id IS 'The active notice version shown when deciding (privacy.notice_version).';
COMMENT ON COLUMN privacy.consent_record.notice_language IS 'The language of the translation shown (BCP 47).';
COMMENT ON COLUMN privacy.consent_record.valid_from IS 'When the decision was recorded.';
COMMENT ON COLUMN privacy.consent_record.valid_until IS 'Last day the decision counts (from privacy.purpose.default_valid_months); NULL = until withdrawn or superseded.';
COMMENT ON COLUMN privacy.consent_record.withdrawn_at IS 'When the giver withdrew the decision; NULL = not withdrawn.';
COMMENT ON COLUMN privacy.consent_record.superseded_by_id IS 'The newer decision of the same giver that replaced this one (privacy.consent_record); NULL = not superseded.';
COMMENT ON COLUMN privacy.consent_record.channel IS 'How the decision was given: web, mobile_app, paper_form, in_person, email, consent_manager, import.';
COMMENT ON COLUMN privacy.consent_record.verification_status IS 'Whether the giver was established as entitled to decide: verified (counts), unverified (a paper form awaiting a second staff member; does not count), rejected.';
COMMENT ON COLUMN privacy.consent_record.verification_method IS 'How the giver was established: existing_record (identity and link already held by the school), in_person_staff_check, digilocker_token, otp_on_registered_mobile, paper_form_countersigned.';
COMMENT ON COLUMN privacy.consent_record.verified_by_user_id IS 'The staff member who verified a recorded paper form (iam.user_account.id, logical); never the one who recorded it.';
COMMENT ON COLUMN privacy.consent_record.verified_at IS 'When the record became verified.';
COMMENT ON COLUMN privacy.consent_record.recorded_by_user_id IS 'The staff member who recorded a paper form on the giver''s behalf (iam.user_account.id, logical); NULL on self-service.';
COMMENT ON COLUMN privacy.consent_record.evidence_file_id IS 'The scanned signed form (platform.file_object, logical); required for a paper form.';
COMMENT ON COLUMN privacy.consent_record.external_artefact_id IS 'Id of the consent artefact at a consent manager (future).';
COMMENT ON COLUMN privacy.consent_record.created_at IS 'Row creation time.';
COMMENT ON COLUMN privacy.consent_record.created_by IS 'User account that created the row (audit reference to iam.user_account; not FK-constrained).';
COMMENT ON COLUMN privacy.consent_record.updated_at IS 'Last modification time (set by the application on every update).';
COMMENT ON COLUMN privacy.consent_record.updated_by IS 'User account that last modified the row (audit reference; not FK-constrained).';
COMMENT ON COLUMN privacy.consent_record.row_version IS 'Optimistic-lock version, incremented on every update; used as the ETag.';
CREATE UNIQUE INDEX ux_consent_record_live ON privacy.consent_record
    (tenant_id, subject_person_id, purpose_code, giver_person_id)
    WHERE withdrawn_at IS NULL AND superseded_by_id IS NULL;
CREATE INDEX ix_consent_record_subject ON privacy.consent_record (tenant_id, subject_person_id, purpose_code);
CREATE INDEX ix_consent_record_giver ON privacy.consent_record (tenant_id, giver_person_id);
CREATE INDEX ix_consent_record_expiry ON privacy.consent_record (tenant_id, valid_until)
    WHERE withdrawn_at IS NULL AND superseded_by_id IS NULL AND valid_until IS NOT NULL;
CREATE INDEX ix_consent_record_notice_version_id ON privacy.consent_record (notice_version_id);
CREATE INDEX ix_consent_record_superseded_by_id ON privacy.consent_record (superseded_by_id);
CREATE INDEX ix_consent_record_purpose_code ON privacy.consent_record (purpose_code);
COMMENT ON INDEX privacy.ux_consent_record_live IS 'One live decision per giver, subject and purpose: a newer decision supersedes the live one.';
COMMENT ON INDEX privacy.ix_consent_record_subject IS 'Every decision about a subject and purpose (recompute, staff lists).';
COMMENT ON INDEX privacy.ix_consent_record_giver IS 'The decisions a giver made (the consent centre).';
COMMENT ON INDEX privacy.ix_consent_record_expiry IS 'The live decisions that lapse by a date (the daily expiry run).';

CREATE TABLE privacy.consent_evidence (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    consent_record_id uuid NOT NULL,
    event varchar(20) NOT NULL,
    actor_user_id uuid,
    actor_person_id uuid,
    on_behalf_of_capacity varchar(10),
    channel varchar(20) NOT NULL,
    notice_content_sha256 char(64),
    ip_address inet,
    user_agent_hash char(64),
    impersonator_user_id uuid,
    occurred_at timestamptz NOT NULL,
    previous_hash char(64),
    entry_hash char(64) NOT NULL,
    CONSTRAINT pk_consent_evidence PRIMARY KEY (id, occurred_at),
    CONSTRAINT fk_consent_evidence_consent_record_id FOREIGN KEY (consent_record_id, tenant_id)
        REFERENCES privacy.consent_record (id, tenant_id),
    CONSTRAINT ck_consent_evidence_event CHECK (event IN ('recorded', 'verified', 'rejected', 'withdrawn', 'superseded',
        'expired', 'majority_lapsed')),
    CONSTRAINT ck_consent_evidence_capacity CHECK (on_behalf_of_capacity IN ('self', 'guardian', 'nominee')),
    CONSTRAINT ck_consent_evidence_channel CHECK (channel IN ('web', 'mobile_app', 'paper_form', 'in_person', 'email',
        'consent_manager', 'import', 'system')),
    CONSTRAINT ck_consent_evidence_notice_content_sha256 CHECK (notice_content_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_consent_evidence_user_agent_hash CHECK (user_agent_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_consent_evidence_previous_hash CHECK (previous_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_consent_evidence_entry_hash CHECK (entry_hash ~ '^[0-9a-f]{64}$')
) PARTITION BY RANGE (occurred_at);
CREATE TABLE privacy.consent_evidence_default PARTITION OF privacy.consent_evidence DEFAULT;
COMMENT ON TABLE privacy.consent_evidence_default IS 'Default partition of privacy.consent_evidence. Monthly partitions are created ahead of time by the application maintenance job, so this partition should stay empty.';
COMMENT ON TABLE privacy.consent_evidence IS 'Append-only ledger of everything that happened to consent records: who, in which capacity, through which channel, from where, under which notice wording. The application only inserts and reads it (no statement updates or deletes a row); each tenant''s entries form a SHA-256 hash chain (previous_hash → entry_hash) so a changed or removed entry shows. Monthly partitions on occurred_at.';
COMMENT ON COLUMN privacy.consent_evidence.id IS 'Entry id (UUIDv7 generated by the application).';
COMMENT ON COLUMN privacy.consent_evidence.tenant_id IS 'Owning tenant (customer organisation). Row-level security key.';
COMMENT ON COLUMN privacy.consent_evidence.consent_record_id IS 'The consent record the entry is about (privacy.consent_record).';
COMMENT ON COLUMN privacy.consent_evidence.event IS 'What happened: recorded, verified, rejected, withdrawn, superseded, expired, majority_lapsed.';
COMMENT ON COLUMN privacy.consent_evidence.actor_user_id IS 'The account that acted (iam.user_account.id, logical); NULL for the system.';
COMMENT ON COLUMN privacy.consent_evidence.actor_person_id IS 'The person that acted (people.person.id, logical), when the account has one.';
COMMENT ON COLUMN privacy.consent_evidence.on_behalf_of_capacity IS 'The capacity of the giver the action was for: self, guardian, nominee; NULL for a staff step such as a verification.';
COMMENT ON COLUMN privacy.consent_evidence.channel IS 'Through which channel the step happened: web, mobile_app, paper_form, in_person, email, consent_manager, import, system.';
COMMENT ON COLUMN privacy.consent_evidence.notice_content_sha256 IS 'privacy.notice_translation.content_sha256 of the wording shown when the decision was recorded.';
COMMENT ON COLUMN privacy.consent_evidence.ip_address IS 'The client address the request came from, when the call came over REST.';
COMMENT ON COLUMN privacy.consent_evidence.user_agent_hash IS 'Lowercase hex SHA-256 of the client''s User-Agent; the agent itself is not kept.';
COMMENT ON COLUMN privacy.consent_evidence.impersonator_user_id IS 'The administrator acting as the user (AuthContext.impersonatorId), never hidden.';
COMMENT ON COLUMN privacy.consent_evidence.occurred_at IS 'When the step happened; strictly increasing within a tenant (privacy.evidence_chain), so it orders the chain.';
COMMENT ON COLUMN privacy.consent_evidence.previous_hash IS 'entry_hash of the tenant''s previous entry; NULL for the tenant''s first entry.';
COMMENT ON COLUMN privacy.consent_evidence.entry_hash IS 'Lowercase hex SHA-256 of the entry''s canonical text (its fields in a fixed order, then previous_hash), computed by the application.';
CREATE INDEX ix_consent_evidence_consent_record_id ON privacy.consent_evidence (tenant_id, consent_record_id);
CREATE INDEX ix_consent_evidence_chain ON privacy.consent_evidence (tenant_id, occurred_at);
COMMENT ON INDEX privacy.ix_consent_evidence_consent_record_id IS 'The history of one consent record.';
COMMENT ON INDEX privacy.ix_consent_evidence_chain IS 'A tenant''s entries in chain order (verification of the hash chain).';

CREATE TABLE privacy.evidence_chain (
    tenant_id uuid NOT NULL,
    last_entry_hash char(64),
    last_occurred_at timestamptz,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_evidence_chain PRIMARY KEY (tenant_id),
    CONSTRAINT ck_evidence_chain_last_entry_hash CHECK (last_entry_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_evidence_chain_head CHECK ((last_entry_hash IS NULL) = (last_occurred_at IS NULL)),
    CONSTRAINT ck_evidence_chain_row_version CHECK (row_version >= 0)
);
COMMENT ON TABLE privacy.evidence_chain IS 'The head of each tenant''s consent evidence hash chain. A writer locks the tenant''s row (SELECT … FOR UPDATE) for the rest of its transaction, so the tenant''s entries are appended one at a time, each naming its predecessor.';
COMMENT ON COLUMN privacy.evidence_chain.tenant_id IS 'Owning tenant (customer organisation). Row-level security key.';
COMMENT ON COLUMN privacy.evidence_chain.last_entry_hash IS 'entry_hash of the tenant''s latest privacy.consent_evidence entry; NULL before the first one.';
COMMENT ON COLUMN privacy.evidence_chain.last_occurred_at IS 'occurred_at of that entry; the next entry occurs strictly later.';
COMMENT ON COLUMN privacy.evidence_chain.row_version IS 'Number of entries appended to the chain.';

CREATE TABLE privacy.consent_state (
    tenant_id uuid NOT NULL,
    subject_person_id uuid NOT NULL,
    purpose_code varchar(40) NOT NULL,
    scope varchar(20) NOT NULL,
    result varchar(14) NOT NULL,
    reason varchar(30) NOT NULL,
    state_version bigint NOT NULL,
    computed_at timestamptz NOT NULL,
    CONSTRAINT pk_consent_state PRIMARY KEY (tenant_id, subject_person_id, purpose_code, scope),
    CONSTRAINT fk_consent_state_subject_person_id FOREIGN KEY (tenant_id, subject_person_id)
        REFERENCES privacy.subject (tenant_id, person_id),
    CONSTRAINT fk_consent_state_purpose_code FOREIGN KEY (purpose_code) REFERENCES privacy.purpose (code),
    CONSTRAINT ck_consent_state_scope CHECK (scope IN ('class', 'institution', 'inter_school', 'public', 'tenant')),
    CONSTRAINT ck_consent_state_result CHECK (result IN ('allowed', 'denied')),
    CONSTRAINT ck_consent_state_reason CHECK (reason IN ('CONSENT', 'REFUSED', 'NARROWER_GRANT', 'NOT_ANSWERED', 'EXPIRED',
        'RECONSENT_REQUIRED', 'FORBIDDEN_FOR_MINOR', 'PURPOSE_DISABLED')),
    CONSTRAINT ck_consent_state_consistent CHECK ((result = 'allowed') = (reason = 'CONSENT')),
    CONSTRAINT ck_consent_state_state_version CHECK (state_version > 0)
);
COMMENT ON TABLE privacy.consent_state IS 'The effective consent answer per subject, purpose and width (design §4.2), rewritten in the transaction of every change that affects it and published as privacy.consent_state.changed; the only table the decision path reads. No row = never computed, which counts as denied (NOT_ANSWERED).';
COMMENT ON COLUMN privacy.consent_state.tenant_id IS 'Owning tenant (customer organisation). Row-level security key.';
COMMENT ON COLUMN privacy.consent_state.subject_person_id IS 'The person the answer is about (privacy.subject.person_id).';
COMMENT ON COLUMN privacy.consent_state.purpose_code IS 'The catalogue purpose (privacy.purpose.code).';
COMMENT ON COLUMN privacy.consent_state.scope IS 'The width answered: class, institution, inter_school, public (width purposes) or tenant.';
COMMENT ON COLUMN privacy.consent_state.result IS 'allowed or denied.';
COMMENT ON COLUMN privacy.consent_state.reason IS 'Why: CONSENT (allowed), REFUSED, NARROWER_GRANT, NOT_ANSWERED, EXPIRED, RECONSENT_REQUIRED, FORBIDDEN_FOR_MINOR, PURPOSE_DISABLED.';
COMMENT ON COLUMN privacy.consent_state.state_version IS 'privacy.subject.state_version when the answer was computed: monotonic per subject, so consumers drop stale events.';
COMMENT ON COLUMN privacy.consent_state.computed_at IS 'When the answer was computed.';
CREATE INDEX ix_consent_state_allowed ON privacy.consent_state (tenant_id, purpose_code, scope) WHERE result = 'allowed';
CREATE INDEX ix_consent_state_purpose_code ON privacy.consent_state (purpose_code);
COMMENT ON INDEX privacy.ix_consent_state_allowed IS 'The subjects a purpose is allowed for at a width.';

-- === Monthly partitions of consent_evidence (2026-2027; later months by the maintenance job) ===

CREATE TABLE privacy.consent_evidence_2026_01 PARTITION OF privacy.consent_evidence FOR VALUES FROM ('2026-01-01 00:00:00+00') TO ('2026-02-01 00:00:00+00');
CREATE TABLE privacy.consent_evidence_2026_02 PARTITION OF privacy.consent_evidence FOR VALUES FROM ('2026-02-01 00:00:00+00') TO ('2026-03-01 00:00:00+00');
CREATE TABLE privacy.consent_evidence_2026_03 PARTITION OF privacy.consent_evidence FOR VALUES FROM ('2026-03-01 00:00:00+00') TO ('2026-04-01 00:00:00+00');
CREATE TABLE privacy.consent_evidence_2026_04 PARTITION OF privacy.consent_evidence FOR VALUES FROM ('2026-04-01 00:00:00+00') TO ('2026-05-01 00:00:00+00');
CREATE TABLE privacy.consent_evidence_2026_05 PARTITION OF privacy.consent_evidence FOR VALUES FROM ('2026-05-01 00:00:00+00') TO ('2026-06-01 00:00:00+00');
CREATE TABLE privacy.consent_evidence_2026_06 PARTITION OF privacy.consent_evidence FOR VALUES FROM ('2026-06-01 00:00:00+00') TO ('2026-07-01 00:00:00+00');
CREATE TABLE privacy.consent_evidence_2026_07 PARTITION OF privacy.consent_evidence FOR VALUES FROM ('2026-07-01 00:00:00+00') TO ('2026-08-01 00:00:00+00');
CREATE TABLE privacy.consent_evidence_2026_08 PARTITION OF privacy.consent_evidence FOR VALUES FROM ('2026-08-01 00:00:00+00') TO ('2026-09-01 00:00:00+00');
CREATE TABLE privacy.consent_evidence_2026_09 PARTITION OF privacy.consent_evidence FOR VALUES FROM ('2026-09-01 00:00:00+00') TO ('2026-10-01 00:00:00+00');
CREATE TABLE privacy.consent_evidence_2026_10 PARTITION OF privacy.consent_evidence FOR VALUES FROM ('2026-10-01 00:00:00+00') TO ('2026-11-01 00:00:00+00');
CREATE TABLE privacy.consent_evidence_2026_11 PARTITION OF privacy.consent_evidence FOR VALUES FROM ('2026-11-01 00:00:00+00') TO ('2026-12-01 00:00:00+00');
CREATE TABLE privacy.consent_evidence_2026_12 PARTITION OF privacy.consent_evidence FOR VALUES FROM ('2026-12-01 00:00:00+00') TO ('2027-01-01 00:00:00+00');
CREATE TABLE privacy.consent_evidence_2027_01 PARTITION OF privacy.consent_evidence FOR VALUES FROM ('2027-01-01 00:00:00+00') TO ('2027-02-01 00:00:00+00');
CREATE TABLE privacy.consent_evidence_2027_02 PARTITION OF privacy.consent_evidence FOR VALUES FROM ('2027-02-01 00:00:00+00') TO ('2027-03-01 00:00:00+00');
CREATE TABLE privacy.consent_evidence_2027_03 PARTITION OF privacy.consent_evidence FOR VALUES FROM ('2027-03-01 00:00:00+00') TO ('2027-04-01 00:00:00+00');
CREATE TABLE privacy.consent_evidence_2027_04 PARTITION OF privacy.consent_evidence FOR VALUES FROM ('2027-04-01 00:00:00+00') TO ('2027-05-01 00:00:00+00');
CREATE TABLE privacy.consent_evidence_2027_05 PARTITION OF privacy.consent_evidence FOR VALUES FROM ('2027-05-01 00:00:00+00') TO ('2027-06-01 00:00:00+00');
CREATE TABLE privacy.consent_evidence_2027_06 PARTITION OF privacy.consent_evidence FOR VALUES FROM ('2027-06-01 00:00:00+00') TO ('2027-07-01 00:00:00+00');
CREATE TABLE privacy.consent_evidence_2027_07 PARTITION OF privacy.consent_evidence FOR VALUES FROM ('2027-07-01 00:00:00+00') TO ('2027-08-01 00:00:00+00');
CREATE TABLE privacy.consent_evidence_2027_08 PARTITION OF privacy.consent_evidence FOR VALUES FROM ('2027-08-01 00:00:00+00') TO ('2027-09-01 00:00:00+00');
CREATE TABLE privacy.consent_evidence_2027_09 PARTITION OF privacy.consent_evidence FOR VALUES FROM ('2027-09-01 00:00:00+00') TO ('2027-10-01 00:00:00+00');
CREATE TABLE privacy.consent_evidence_2027_10 PARTITION OF privacy.consent_evidence FOR VALUES FROM ('2027-10-01 00:00:00+00') TO ('2027-11-01 00:00:00+00');
CREATE TABLE privacy.consent_evidence_2027_11 PARTITION OF privacy.consent_evidence FOR VALUES FROM ('2027-11-01 00:00:00+00') TO ('2027-12-01 00:00:00+00');
CREATE TABLE privacy.consent_evidence_2027_12 PARTITION OF privacy.consent_evidence FOR VALUES FROM ('2027-12-01 00:00:00+00') TO ('2028-01-01 00:00:00+00');

-- === P1-13: transactional outbox / inbox ===

CREATE TABLE privacy.outbox_event (
    id bigint GENERATED ALWAYS AS IDENTITY,
    tenant_id uuid NOT NULL,
    event_type varchar(120) NOT NULL,
    aggregate_type varchar(80) NOT NULL,
    aggregate_id uuid NOT NULL,
    payload text NOT NULL,
    occurred_at timestamptz NOT NULL DEFAULT now(),
    published_at timestamptz,
    publish_error text,
    CONSTRAINT pk_privacy_outbox_event PRIMARY KEY (id)
);
COMMENT ON TABLE privacy.outbox_event IS 'Transactional outbox of the privacy module: events written in the same transaction as the change, published to the bus by a relay, then pruned. Ordered by the identity column.';
COMMENT ON COLUMN privacy.outbox_event.id IS 'Monotonic sequence; publish order.';
COMMENT ON COLUMN privacy.outbox_event.tenant_id IS 'Owning tenant (customer organisation). Row-level security key.';
COMMENT ON COLUMN privacy.outbox_event.event_type IS 'Event name (module.entity.verb).';
COMMENT ON COLUMN privacy.outbox_event.aggregate_type IS 'Entity kind the event is about.';
COMMENT ON COLUMN privacy.outbox_event.aggregate_id IS 'Entity id the event is about.';
COMMENT ON COLUMN privacy.outbox_event.payload IS 'Serialised event (opaque message envelope).';
COMMENT ON COLUMN privacy.outbox_event.occurred_at IS 'When the change committed.';
COMMENT ON COLUMN privacy.outbox_event.published_at IS 'When the relay published it (NULL = pending).';
COMMENT ON COLUMN privacy.outbox_event.publish_error IS 'Why the relay could not turn the row into an event (set aside, not published); NULL otherwise.';
CREATE INDEX ix_privacy_outbox_event_pending ON privacy.outbox_event (id) WHERE published_at IS NULL AND publish_error IS NULL;
CREATE TABLE privacy.inbox_event (
    event_id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    event_type varchar(120) NOT NULL,
    received_at timestamptz NOT NULL DEFAULT now(),
    processed_at timestamptz,
    processing_error text,
    CONSTRAINT pk_privacy_inbox_event PRIMARY KEY (tenant_id, event_id)
);
COMMENT ON TABLE privacy.inbox_event IS 'Inbox of the privacy module: ids of consumed events, kept so redeliveries are processed exactly once.';
COMMENT ON COLUMN privacy.inbox_event.event_id IS 'Global id of the consumed event; recorded per tenant.';
COMMENT ON COLUMN privacy.inbox_event.tenant_id IS 'Owning tenant (customer organisation). Row-level security key.';
COMMENT ON COLUMN privacy.inbox_event.event_type IS 'Event name.';
COMMENT ON COLUMN privacy.inbox_event.received_at IS 'First delivery time.';
COMMENT ON COLUMN privacy.inbox_event.processed_at IS 'When handling committed (NULL = in flight).';
COMMENT ON COLUMN privacy.inbox_event.processing_error IS 'Last handling error, if any.';
CREATE INDEX ix_privacy_inbox_event_processed ON privacy.inbox_event (processed_at) WHERE processed_at IS NOT NULL;

-- === P0-03: row-level security (tenant isolation). The application role is not the table owner and sets app.tenant_id per transaction; a platform-admin role with BYPASSRLS serves cross-tenant operations ===

ALTER TABLE privacy.consent_evidence ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.consent_evidence FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.consent_evidence USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
ALTER TABLE privacy.consent_record ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.consent_record FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.consent_record USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
ALTER TABLE privacy.consent_state ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.consent_state FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.consent_state USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
ALTER TABLE privacy.evidence_chain ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.evidence_chain FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.evidence_chain USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
ALTER TABLE privacy.guardian_link ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.guardian_link FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.guardian_link USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
ALTER TABLE privacy.inbox_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.inbox_event FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.inbox_event USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
ALTER TABLE privacy.notice ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.notice FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.notice USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
ALTER TABLE privacy.notice_translation ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.notice_translation FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.notice_translation USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
ALTER TABLE privacy.notice_version ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.notice_version FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.notice_version USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
ALTER TABLE privacy.outbox_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.outbox_event FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.outbox_event USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
ALTER TABLE privacy.subject ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.subject FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.subject USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
ALTER TABLE privacy.tenant_purpose ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.tenant_purpose FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.tenant_purpose USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

-- === Module-local infrastructure (distributed deployment: no cross-schema access) ===
CREATE TABLE privacy.idempotency_key (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    idempotency_key uuid NOT NULL,
    user_id uuid,
    operation_name varchar(120) NOT NULL,
    request_hash varchar(64),
    response_status smallint,
    response_body text,
    locked_at timestamptz,
    completed_at timestamptz,
    expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_privacy_idempotency_key PRIMARY KEY (id),
    CONSTRAINT uq_privacy_idempotency_key_1 UNIQUE (tenant_id, idempotency_key)
);
COMMENT ON TABLE privacy.idempotency_key IS 'Module-local idempotency store of the privacy module: stored outcome of a write carrying an Idempotency-Key; replays return the stored response. Rows expire after 24h (cleanup job).';
COMMENT ON COLUMN privacy.idempotency_key.id IS 'Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).';
COMMENT ON COLUMN privacy.idempotency_key.tenant_id IS 'Owning tenant (customer organisation). Row-level security key.';
COMMENT ON COLUMN privacy.idempotency_key.idempotency_key IS 'Key sent by the client.';
COMMENT ON COLUMN privacy.idempotency_key.user_id IS 'Caller.';
COMMENT ON COLUMN privacy.idempotency_key.operation_name IS 'Operation the key was used on.';
COMMENT ON COLUMN privacy.idempotency_key.request_hash IS 'Hash of the request body; a replay with a different body is rejected.';
COMMENT ON COLUMN privacy.idempotency_key.response_status IS 'Stored HTTP status.';
COMMENT ON COLUMN privacy.idempotency_key.response_body IS 'Stored response body.';
COMMENT ON COLUMN privacy.idempotency_key.locked_at IS 'Set while the first attempt is still executing.';
COMMENT ON COLUMN privacy.idempotency_key.completed_at IS 'When the stored response was written.';
COMMENT ON COLUMN privacy.idempotency_key.expires_at IS 'When the row may be purged.';
COMMENT ON COLUMN privacy.idempotency_key.created_at IS 'Row creation time.';
COMMENT ON COLUMN privacy.idempotency_key.created_by IS 'User account that created the row (audit reference to iam.user_account; not FK-constrained).';
COMMENT ON COLUMN privacy.idempotency_key.updated_at IS 'Last modification time (set by the application on every update).';
COMMENT ON COLUMN privacy.idempotency_key.updated_by IS 'User account that last modified the row (audit reference; not FK-constrained).';
COMMENT ON COLUMN privacy.idempotency_key.deleted_at IS 'Soft-delete timestamp (NULL = live row).';
COMMENT ON COLUMN privacy.idempotency_key.row_version IS 'Optimistic-lock version, incremented on every update; used as the ETag.';
ALTER TABLE privacy.idempotency_key ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.idempotency_key FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.idempotency_key USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
CREATE TABLE privacy.operation (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    operation_name varchar(120) NOT NULL,
    resource_type varchar(80),
    resource_id uuid,
    status varchar(20) NOT NULL DEFAULT 'running',
    progress_percent smallint,
    error_code varchar(60),
    error_message varchar(500),
    result_entity_type varchar(80),
    result_entity_id uuid,
    started_by_user_id uuid,
    started_at timestamptz NOT NULL DEFAULT now(),
    finished_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_privacy_operation PRIMARY KEY (id),
    CONSTRAINT ck_privacy_operation_status CHECK (status IN ('running', 'succeeded', 'failed', 'cancelled'))
);
COMMENT ON TABLE privacy.operation IS 'Module-local long-running operation record of the privacy module, returned by its async API calls; polled by clients until it finishes.';
COMMENT ON COLUMN privacy.operation.id IS 'Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).';
COMMENT ON COLUMN privacy.operation.tenant_id IS 'Owning tenant (customer organisation). Row-level security key.';
COMMENT ON COLUMN privacy.operation.operation_name IS 'Operation id as exposed by the API (module.Method).';
COMMENT ON COLUMN privacy.operation.resource_type IS 'Kind of resource being worked on.';
COMMENT ON COLUMN privacy.operation.resource_id IS 'Id of that resource.';
COMMENT ON COLUMN privacy.operation.status IS 'running / succeeded / failed / cancelled.';
COMMENT ON COLUMN privacy.operation.progress_percent IS 'Progress, when the job reports it.';
COMMENT ON COLUMN privacy.operation.error_code IS 'Machine error code when failed.';
COMMENT ON COLUMN privacy.operation.error_message IS 'Human error summary when failed.';
COMMENT ON COLUMN privacy.operation.result_entity_type IS 'Kind of the produced record.';
COMMENT ON COLUMN privacy.operation.result_entity_id IS 'Id of the produced record.';
COMMENT ON COLUMN privacy.operation.started_by_user_id IS 'User who started it.';
COMMENT ON COLUMN privacy.operation.started_at IS 'Start time.';
COMMENT ON COLUMN privacy.operation.finished_at IS 'End time.';
COMMENT ON COLUMN privacy.operation.created_at IS 'Row creation time.';
COMMENT ON COLUMN privacy.operation.created_by IS 'User account that created the row (audit reference to iam.user_account; not FK-constrained).';
COMMENT ON COLUMN privacy.operation.updated_at IS 'Last modification time (set by the application on every update).';
COMMENT ON COLUMN privacy.operation.updated_by IS 'User account that last modified the row (audit reference; not FK-constrained).';
COMMENT ON COLUMN privacy.operation.deleted_at IS 'Soft-delete timestamp (NULL = live row).';
COMMENT ON COLUMN privacy.operation.row_version IS 'Optimistic-lock version, incremented on every update; used as the ETag.';
ALTER TABLE privacy.operation ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.operation FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.operation USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

-- === Indexes on logical (cross-module) reference columns ===
CREATE INDEX ix_tenant_purpose_purpose_id ON privacy.tenant_purpose (purpose_id);
CREATE INDEX ix_tenant_purpose_notice_id ON privacy.tenant_purpose (notice_id);
CREATE INDEX ix_notice_version_document_file_id ON privacy.notice_version (document_file_id);
CREATE INDEX ix_consent_record_evidence_file_id ON privacy.consent_record (evidence_file_id);
CREATE INDEX ix_consent_record_recorded_by_user_id ON privacy.consent_record (recorded_by_user_id);
CREATE INDEX ix_consent_record_verified_by_user_id ON privacy.consent_record (verified_by_user_id);
