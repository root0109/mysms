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
    (md5('mysms.purpose:' || 'admission_processing')::uuid, 'admission_processing', 'tenant', 'tenant', false, 'none'),
    (md5('mysms.purpose:' || 'behavioural_analytics')::uuid, 'behavioural_analytics', 'tenant', 'tenant', true, 'none'),
    (md5('mysms.purpose:' || 'biometric_data')::uuid, 'biometric_data', 'tenant', 'tenant', false, 'none'),
    (md5('mysms.purpose:' || 'biometric_or_video_monitoring')::uuid, 'biometric_or_video_monitoring', 'tenant', 'tenant', true, 'none'),
    (md5('mysms.purpose:' || 'community_naming')::uuid, 'community_naming', 'width', 'tenant', false, 'keep_published'),
    (md5('mysms.purpose:' || 'community_participation')::uuid, 'community_participation', 'tenant', 'tenant', false, 'none'),
    (md5('mysms.purpose:' || 'health_record_keeping')::uuid, 'health_record_keeping', 'tenant', 'tenant', false, 'none'),
    (md5('mysms.purpose:' || 'learning_analytics')::uuid, 'learning_analytics', 'tenant', 'tenant', false, 'none'),
    (md5('mysms.purpose:' || 'marketing_comms')::uuid, 'marketing_comms', 'tenant', 'tenant', false, 'none'),
    (md5('mysms.purpose:' || 'operational_communication')::uuid, 'operational_communication', 'tenant', 'tenant', false, 'none'),
    (md5('mysms.purpose:' || 'photo_video_publication')::uuid, 'photo_video_publication', 'width', 'tenant', false, 'keep_published'),
    (md5('mysms.purpose:' || 'product_analytics')::uuid, 'product_analytics', 'tenant', 'platform', true, 'none'),
    (md5('mysms.purpose:' || 'public_profile')::uuid, 'public_profile', 'tenant', 'tenant', false, 'none'),
    (md5('mysms.purpose:' || 'sms_whatsapp_messages')::uuid, 'sms_whatsapp_messages', 'tenant', 'tenant', false, 'none'),
    (md5('mysms.purpose:' || 'targeted_advertising')::uuid, 'targeted_advertising', 'tenant', 'tenant', true, 'none'),
    (md5('mysms.purpose:' || 'third_party_ai_processing')::uuid, 'third_party_ai_processing', 'tenant', 'tenant', false, 'none'),
    (md5('mysms.purpose:' || 'third_party_payment')::uuid, 'third_party_payment', 'tenant', 'tenant', false, 'none'),
    (md5('mysms.purpose:' || 'third_party_sharing')::uuid, 'third_party_sharing', 'tenant', 'tenant', false, 'none'),
    (md5('mysms.purpose:' || 'transport_live_location')::uuid, 'transport_live_location', 'tenant', 'tenant', false, 'none'),
    (md5('mysms.purpose:' || 'visitor_safety')::uuid, 'visitor_safety', 'tenant', 'tenant', false, 'none');

CREATE TABLE privacy.data_category (
    id uuid NOT NULL,
    code varchar(40) NOT NULL,
    sensitivity varchar(12) NOT NULL,
    description text NOT NULL,
    CONSTRAINT pk_data_category PRIMARY KEY (id),
    CONSTRAINT uq_data_category_code UNIQUE (code),
    CONSTRAINT ck_data_category_sensitivity CHECK (sensitivity IN ('normal', 'sensitive', 'restricted'))
);
COMMENT ON TABLE privacy.data_category IS 'What kind of personal data a processing activity touches: the platform-wide category catalogue, used by notices, the processing register, export, erasure and retention.';
COMMENT ON COLUMN privacy.data_category.id IS 'Stable id, the same in every environment: md5(''mysms.data_category:'' || code)::uuid.';
COMMENT ON COLUMN privacy.data_category.code IS 'Category code, e.g. contact.';
COMMENT ON COLUMN privacy.data_category.sensitivity IS 'normal; sensitive (wider harm if exposed); restricted (health, counselling, biometric: restricted access, erasure needs a second reviewer).';
COMMENT ON COLUMN privacy.data_category.description IS 'What the category holds, in English.';

-- Sensitivity per the owner's decision of 23 Nov 2026 (design §4.1, §5.1).
INSERT INTO privacy.data_category (id, code, sensitivity, description) VALUES
    (md5('mysms.data_category:' || 'academic_record')::uuid, 'academic_record', 'normal', 'Marks, grades, assessments, coursework and learning progress.'),
    (md5('mysms.data_category:' || 'attendance')::uuid, 'attendance', 'normal', 'Presence, absence and lateness records.'),
    (md5('mysms.data_category:' || 'biometric')::uuid, 'biometric', 'restricted', 'Fingerprints, face templates and other biometric identifiers.'),
    (md5('mysms.data_category:' || 'contact')::uuid, 'contact', 'normal', 'Phone numbers, e-mail and postal addresses, messaging handles.'),
    (md5('mysms.data_category:' || 'counselling')::uuid, 'counselling', 'restricted', 'Counselling, welfare and safeguarding notes.'),
    (md5('mysms.data_category:' || 'device_usage')::uuid, 'device_usage', 'normal', 'How the person uses the apps: sessions, clicks, device and browser details.'),
    (md5('mysms.data_category:' || 'financial')::uuid, 'financial', 'sensitive', 'Fees, payments, bank and card references, salaries.'),
    (md5('mysms.data_category:' || 'government_id')::uuid, 'government_id', 'sensitive', 'Government identity numbers and documents (Aadhaar, PAN, passport, APAAR).'),
    (md5('mysms.data_category:' || 'health')::uuid, 'health', 'restricted', 'Medical conditions, allergies, medication, disability and health notes.'),
    (md5('mysms.data_category:' || 'identity')::uuid, 'identity', 'normal', 'Name, date of birth, gender, relations and school identifiers.'),
    (md5('mysms.data_category:' || 'location')::uuid, 'location', 'sensitive', 'Live or recorded location, such as a bus position or a home address on a map.'),
    (md5('mysms.data_category:' || 'photo_media')::uuid, 'photo_media', 'sensitive', 'Photos, videos and voice recordings of the person.');

-- RoPA-lite (design §5.1, §8): what each module does with personal data, for which purpose, on which basis. Seeded
-- here from design §8 as approved by the owner on 23 Nov 2026; the per-module manifests and their conformance check
-- (META-INF/mysms/privacy/<module>.json) come later. Bases marked "to verify" in the design await legal review
-- (ROADMAP SEC-11): lms.learning_analytics, transport.live_location_share, sis.health_record.
CREATE TABLE privacy.processing_activity (
    id uuid NOT NULL,
    module varchar(30) NOT NULL,
    code varchar(60) NOT NULL,
    purpose_id uuid NOT NULL,
    legal_basis varchar(30) NOT NULL,
    education_exemption_clause varchar(60),
    targets_minors boolean NOT NULL,
    audiences varchar(10)[] NOT NULL,
    recipients jsonb NOT NULL DEFAULT '[]',
    cross_border boolean NOT NULL DEFAULT false,
    retention_policy_code varchar(60),
    version integer NOT NULL DEFAULT 1,
    CONSTRAINT pk_processing_activity PRIMARY KEY (id),
    CONSTRAINT uq_processing_activity_code UNIQUE (code),
    CONSTRAINT fk_processing_activity_purpose_id FOREIGN KEY (purpose_id) REFERENCES privacy.purpose (id),
    CONSTRAINT ck_processing_activity_code CHECK (code ~ '^[a-z_]+\.[a-z_]+$' AND code LIKE module || '.%'),
    CONSTRAINT ck_processing_activity_basis CHECK (legal_basis IN ('consent', 'legitimate_use', 'legitimate_use_voluntary',
        'legal_obligation', 'education_exemption', 'medical_emergency', 'employment', 'contract')),
    CONSTRAINT ck_processing_activity_exemption CHECK (legal_basis <> 'education_exemption'
        OR education_exemption_clause IS NOT NULL),
    CONSTRAINT ck_processing_activity_recipients CHECK (jsonb_typeof(recipients) = 'array'),
    CONSTRAINT ck_processing_activity_audiences CHECK (cardinality(audiences) > 0
        AND audiences <@ ARRAY['student', 'guardian', 'staff']::varchar(10)[]),
    CONSTRAINT ck_processing_activity_version CHECK (version > 0)
);
COMMENT ON TABLE privacy.processing_activity IS 'The processing register (RoPA-lite): one row per thing a module does with personal data — its purpose, legal basis, whether it reaches minors, recipients and retention. Activities on the consent basis are checked through privacy-client; the others are recorded only.';
COMMENT ON COLUMN privacy.processing_activity.id IS 'Stable id, the same in every environment: md5(''mysms.processing_activity:'' || code)::uuid.';
COMMENT ON COLUMN privacy.processing_activity.module IS 'The module that processes the data.';
COMMENT ON COLUMN privacy.processing_activity.code IS 'Activity code <module>.<activity>, e.g. comms.marketing_broadcast.';
COMMENT ON COLUMN privacy.processing_activity.purpose_id IS 'The purpose the activity serves (privacy.purpose).';
COMMENT ON COLUMN privacy.processing_activity.legal_basis IS 'consent; legitimate_use (DPDP Act s.7, e.g. safety and operations); legitimate_use_voluntary (s.7(a)); legal_obligation; education_exemption; medical_emergency; employment; contract.';
COMMENT ON COLUMN privacy.processing_activity.education_exemption_clause IS 'The Fourth Schedule clause relied on when the basis is education_exemption.';
COMMENT ON COLUMN privacy.processing_activity.audiences IS 'Whose data the activity processes, by their role at the school: student, guardian, staff (one or more). An access summary lists an activity only for a subject who holds one of these roles.';
COMMENT ON COLUMN privacy.processing_activity.targets_minors IS 'Whether the activity processes data of minors; never true for a purpose forbidden for minors.';
COMMENT ON COLUMN privacy.processing_activity.recipients IS 'Who receives the data: a JSON array of {kind: processor|fiduciary, name, country}; empty while none is named.';
COMMENT ON COLUMN privacy.processing_activity.cross_border IS 'Whether the data leaves India.';
COMMENT ON COLUMN privacy.processing_activity.retention_policy_code IS 'The retention policy that applies (the retention engine, later); NULL while none is set.';
COMMENT ON COLUMN privacy.processing_activity.version IS 'Incremented on a material change of the activity.';
CREATE INDEX ix_processing_activity_purpose_id ON privacy.processing_activity (purpose_id);
COMMENT ON INDEX privacy.ix_processing_activity_purpose_id IS 'The activities of a purpose.';

INSERT INTO privacy.processing_activity (id, module, code, purpose_id, legal_basis, targets_minors, audiences) VALUES
    (md5('mysms.processing_activity:' || 'admission.application')::uuid, 'admission', 'admission.application', md5('mysms.purpose:' || 'admission_processing')::uuid, 'legitimate_use_voluntary', true, ARRAY['student', 'guardian']::varchar(10)[]),
    (md5('mysms.processing_activity:' || 'attendance.biometric_capture')::uuid, 'attendance', 'attendance.biometric_capture', md5('mysms.purpose:' || 'biometric_data')::uuid, 'consent', true, ARRAY['student', 'staff']::varchar(10)[]),
    (md5('mysms.processing_activity:' || 'careers.employer_sharing')::uuid, 'careers', 'careers.employer_sharing', md5('mysms.purpose:' || 'third_party_sharing')::uuid, 'consent', false, ARRAY['student']::varchar(10)[]),
    (md5('mysms.processing_activity:' || 'comms.community_naming')::uuid, 'comms', 'comms.community_naming', md5('mysms.purpose:' || 'community_naming')::uuid, 'consent', true, ARRAY['student']::varchar(10)[]),
    (md5('mysms.processing_activity:' || 'comms.community_participation')::uuid, 'comms', 'comms.community_participation', md5('mysms.purpose:' || 'community_participation')::uuid, 'consent', true, ARRAY['student', 'guardian']::varchar(10)[]),
    (md5('mysms.processing_activity:' || 'comms.community_photo_publication')::uuid, 'comms', 'comms.community_photo_publication', md5('mysms.purpose:' || 'photo_video_publication')::uuid, 'consent', true, ARRAY['student', 'staff']::varchar(10)[]),
    (md5('mysms.processing_activity:' || 'comms.marketing_broadcast')::uuid, 'comms', 'comms.marketing_broadcast', md5('mysms.purpose:' || 'marketing_comms')::uuid, 'consent', false, ARRAY['student', 'guardian']::varchar(10)[]),
    (md5('mysms.processing_activity:' || 'comms.operational_notice')::uuid, 'comms', 'comms.operational_notice', md5('mysms.purpose:' || 'operational_communication')::uuid, 'legitimate_use', false, ARRAY['student', 'guardian', 'staff']::varchar(10)[]),
    (md5('mysms.processing_activity:' || 'comms.whatsapp_optional')::uuid, 'comms', 'comms.whatsapp_optional', md5('mysms.purpose:' || 'sms_whatsapp_messages')::uuid, 'consent', false, ARRAY['student', 'guardian', 'staff']::varchar(10)[]),
    (md5('mysms.processing_activity:' || 'finance.payment_gateway')::uuid, 'finance', 'finance.payment_gateway', md5('mysms.purpose:' || 'third_party_payment')::uuid, 'contract', false, ARRAY['student', 'guardian']::varchar(10)[]),
    (md5('mysms.processing_activity:' || 'frontoffice.visitor_log')::uuid, 'frontoffice', 'frontoffice.visitor_log', md5('mysms.purpose:' || 'visitor_safety')::uuid, 'legitimate_use', false, ARRAY['guardian']::varchar(10)[]),
    (md5('mysms.processing_activity:' || 'lms.ai_assist_external')::uuid, 'lms', 'lms.ai_assist_external', md5('mysms.purpose:' || 'third_party_ai_processing')::uuid, 'consent', true, ARRAY['student', 'staff']::varchar(10)[]),
    (md5('mysms.processing_activity:' || 'lms.learning_analytics')::uuid, 'lms', 'lms.learning_analytics', md5('mysms.purpose:' || 'learning_analytics')::uuid, 'consent', true, ARRAY['student']::varchar(10)[]),
    (md5('mysms.processing_activity:' || 'lms.proctoring_video')::uuid, 'lms', 'lms.proctoring_video', md5('mysms.purpose:' || 'biometric_or_video_monitoring')::uuid, 'consent', false, ARRAY['student']::varchar(10)[]),
    (md5('mysms.processing_activity:' || 'people.public_profile')::uuid, 'people', 'people.public_profile', md5('mysms.purpose:' || 'public_profile')::uuid, 'consent', false, ARRAY['student', 'staff']::varchar(10)[]),
    (md5('mysms.processing_activity:' || 'platform.product_analytics')::uuid, 'platform', 'platform.product_analytics', md5('mysms.purpose:' || 'product_analytics')::uuid, 'consent', false, ARRAY['student', 'guardian', 'staff']::varchar(10)[]),
    (md5('mysms.processing_activity:' || 'sis.health_record')::uuid, 'sis', 'sis.health_record', md5('mysms.purpose:' || 'health_record_keeping')::uuid, 'legal_obligation', true, ARRAY['student']::varchar(10)[]),
    (md5('mysms.processing_activity:' || 'transport.live_location_share')::uuid, 'transport', 'transport.live_location_share', md5('mysms.purpose:' || 'transport_live_location')::uuid, 'legitimate_use', true, ARRAY['student', 'guardian']::varchar(10)[]);

CREATE TABLE privacy.processing_activity_category (
    activity_id uuid NOT NULL,
    category_id uuid NOT NULL,
    CONSTRAINT pk_processing_activity_category PRIMARY KEY (activity_id, category_id),
    CONSTRAINT fk_processing_activity_category_activity_id FOREIGN KEY (activity_id)
        REFERENCES privacy.processing_activity (id),
    CONSTRAINT fk_processing_activity_category_category_id FOREIGN KEY (category_id)
        REFERENCES privacy.data_category (id)
);
COMMENT ON TABLE privacy.processing_activity_category IS 'Which data categories each processing activity touches.';
COMMENT ON COLUMN privacy.processing_activity_category.activity_id IS 'The activity (privacy.processing_activity).';
COMMENT ON COLUMN privacy.processing_activity_category.category_id IS 'A category it touches (privacy.data_category).';
CREATE INDEX ix_processing_activity_category_category_id ON privacy.processing_activity_category (category_id);
COMMENT ON INDEX privacy.ix_processing_activity_category_category_id IS 'The activities that touch a category.';

INSERT INTO privacy.processing_activity_category (activity_id, category_id)
SELECT md5('mysms.processing_activity:' || a)::uuid, md5('mysms.data_category:' || c)::uuid
  FROM (VALUES
    ('admission.application', 'academic_record'), ('admission.application', 'contact'),
    ('admission.application', 'government_id'), ('admission.application', 'identity'),
    ('attendance.biometric_capture', 'attendance'), ('attendance.biometric_capture', 'biometric'),
    ('careers.employer_sharing', 'academic_record'), ('careers.employer_sharing', 'contact'),
    ('careers.employer_sharing', 'identity'),
    ('comms.community_naming', 'identity'),
    ('comms.community_participation', 'contact'), ('comms.community_participation', 'identity'),
    ('comms.community_photo_publication', 'identity'), ('comms.community_photo_publication', 'photo_media'),
    ('comms.marketing_broadcast', 'contact'),
    ('comms.operational_notice', 'contact'),
    ('comms.whatsapp_optional', 'contact'),
    ('finance.payment_gateway', 'contact'), ('finance.payment_gateway', 'financial'),
    ('frontoffice.visitor_log', 'government_id'), ('frontoffice.visitor_log', 'identity'),
    ('frontoffice.visitor_log', 'photo_media'),
    ('lms.ai_assist_external', 'academic_record'),
    ('lms.learning_analytics', 'academic_record'), ('lms.learning_analytics', 'device_usage'),
    ('lms.proctoring_video', 'biometric'), ('lms.proctoring_video', 'photo_media'),
    ('people.public_profile', 'contact'), ('people.public_profile', 'identity'), ('people.public_profile', 'photo_media'),
    ('platform.product_analytics', 'device_usage'),
    ('sis.health_record', 'health'),
    ('transport.live_location_share', 'location')
  ) AS pairs (a, c);

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
    staff_records jsonb NOT NULL DEFAULT '{}',
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
COMMENT ON COLUMN privacy.subject.staff_records IS 'Each hr.staff record ever seen for the person, by id: {userId, institutionId, rowVersion, live}, kept from hr.staff events exactly as student_records. A live entry makes the person staff for the access summary and names their staff login accounts.';
COMMENT ON COLUMN privacy.subject.person_row_version IS 'Row version of the people.person row the birth date reflects (0 = no person event yet); an older person event is ignored.';
COMMENT ON COLUMN privacy.subject.state_version IS 'Monotonic version of the person''s effective consent state (privacy.consent_state.state_version): incremented under the row lock whenever a recompute changes one of their states, so consumers drop stale privacy.consent_state.changed events.';
COMMENT ON COLUMN privacy.subject.updated_at IS 'When an event last changed the row.';
CREATE INDEX ix_subject_majority ON privacy.subject (tenant_id, majority_date) WHERE status <> 'adult';
CREATE INDEX ix_subject_student_records ON privacy.subject USING gin (student_records);
COMMENT ON INDEX privacy.ix_subject_majority IS 'The minors who come of age by a date (the age-of-majority transition).';
COMMENT ON INDEX privacy.ix_subject_student_records IS 'The subjects that hold an entry of a student record, found when its event arrives (containment by key).';
CREATE INDEX ix_subject_staff_records ON privacy.subject USING gin (staff_records);
COMMENT ON INDEX privacy.ix_subject_staff_records IS 'The subjects that hold an entry of a staff record, found when its event arrives (containment by key).';

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

CREATE TABLE privacy.job_schedule (
    job_code varchar(60) NOT NULL,
    tenant_id uuid NOT NULL,
    next_run_at timestamptz NOT NULL,
    lease_owner uuid,
    lease_until timestamptz,
    last_run_at timestamptz,
    failures integer NOT NULL DEFAULT 0,
    last_error varchar(500),
    CONSTRAINT pk_job_schedule PRIMARY KEY (job_code, tenant_id),
    CONSTRAINT ck_job_schedule_lease CHECK ((lease_owner IS NULL) = (lease_until IS NULL)),
    CONSTRAINT ck_job_schedule_failures CHECK (failures >= 0)
);
COMMENT ON TABLE privacy.job_schedule IS 'When each tenant is next due for each scheduled job of the module (core JobSchedule, ROADMAP ARC-4). A tenant is enrolled the first time it has work for the job. Job codes, tenant ids and times only, no personal data, and no row-level security: the job runner claims due rows across tenants, then runs each in its tenant''s own context.';
COMMENT ON COLUMN privacy.job_schedule.job_code IS 'The job (<module>.<name>): privacy.majority, privacy.consent_expiry, privacy.task_sweep (re-dispatches unanswered rights-request tasks) or privacy.retention (the daily retention runs).';
COMMENT ON COLUMN privacy.job_schedule.tenant_id IS 'The tenant the job runs for.';
COMMENT ON COLUMN privacy.job_schedule.next_run_at IS 'When the tenant is next due.';
COMMENT ON COLUMN privacy.job_schedule.lease_owner IS 'The runner (a random id per process) holding the tenant''s run; null while no run is held.';
COMMENT ON COLUMN privacy.job_schedule.lease_until IS 'Until when the lease holds; after it another runner may take the run over.';
COMMENT ON COLUMN privacy.job_schedule.last_run_at IS 'When the last run ended, finished or failed.';
COMMENT ON COLUMN privacy.job_schedule.failures IS 'Failed runs in a row; the retry delay doubles with each.';
COMMENT ON COLUMN privacy.job_schedule.last_error IS 'Why the last run failed (exception class and message, first 500 characters); null after a finished run.';
CREATE INDEX ix_job_schedule_due ON privacy.job_schedule (next_run_at);
COMMENT ON INDEX privacy.ix_job_schedule_due IS 'The due runs, the longest overdue first.';

-- === Rights requests of data principals (design §5.5, §9; P2-1) ===

CREATE TABLE privacy.rights_request (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    number varchar(30) NOT NULL,
    type varchar(14) NOT NULL,
    subject_person_id uuid NOT NULL,
    requester_person_id uuid NOT NULL,
    requester_capacity varchar(10) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'submitted',
    identity_check varchar(20),
    details jsonb NOT NULL DEFAULT '{}',
    assignee_user_id uuid,
    submitted_at timestamptz NOT NULL,
    due_at timestamptz NOT NULL,
    extended_reason text,
    closed_at timestamptz,
    outcome_code varchar(30),
    outcome_note text,
    outcome_response text,
    outcome_file_id uuid,
    access_summary jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_rights_request PRIMARY KEY (id),
    CONSTRAINT uq_rights_request_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT uq_rights_request_number UNIQUE (tenant_id, number),
    CONSTRAINT ck_rights_request_number CHECK (number ~ '^PRV-[0-9]{4}-[0-9]{6,}$'),
    CONSTRAINT ck_rights_request_type CHECK (type IN ('access', 'export', 'correction', 'erasure', 'grievance',
        'nomination', 'withdraw_all')),
    CONSTRAINT ck_rights_request_capacity CHECK (requester_capacity IN ('self', 'guardian', 'nominee')),
    CONSTRAINT ck_rights_request_self CHECK (requester_capacity <> 'self' OR requester_person_id = subject_person_id),
    CONSTRAINT ck_rights_request_status CHECK (status IN ('submitted', 'verified', 'in_progress', 'action_required',
        'rejected', 'closed')),
    CONSTRAINT ck_rights_request_identity_check CHECK (identity_check IN ('signed_in_session', 'in_person', 'document')),
    CONSTRAINT ck_rights_request_verified CHECK (status IN ('submitted', 'rejected') OR identity_check IS NOT NULL),
    CONSTRAINT ck_rights_request_assigned CHECK (status NOT IN ('in_progress', 'action_required')
        OR assignee_user_id IS NOT NULL),
    CONSTRAINT ck_rights_request_due CHECK (due_at > submitted_at),
    CONSTRAINT ck_rights_request_extended CHECK (extended_reason IS NULL OR length(extended_reason) BETWEEN 1 AND 1000),
    CONSTRAINT ck_rights_request_closed CHECK ((status IN ('rejected', 'closed')) = (closed_at IS NOT NULL)),
    CONSTRAINT ck_rights_request_outcome CHECK ((closed_at IS NULL) = (outcome_code IS NULL)),
    CONSTRAINT ck_rights_request_outcome_code CHECK (outcome_code IN ('fulfilled', 'partially_fulfilled', 'declined',
        'identity_not_verified', 'not_entitled', 'duplicate', 'other')),
    CONSTRAINT ck_rights_request_outcome_note CHECK (outcome_note IS NULL OR length(outcome_note) BETWEEN 1 AND 4000),
    CONSTRAINT ck_rights_request_outcome_response CHECK (outcome_response IS NULL
        OR (closed_at IS NOT NULL AND length(outcome_response) BETWEEN 1 AND 4000)),
    CONSTRAINT ck_rights_request_nominee CHECK (requester_capacity <> 'nominee'
        OR identity_check IS NULL OR identity_check IN ('in_person', 'document')),
    CONSTRAINT ck_rights_request_closed_at CHECK (closed_at IS NULL OR closed_at >= submitted_at),
    CONSTRAINT ck_rights_request_access_summary CHECK (access_summary IS NULL
        OR (type = 'access' AND status = 'closed' AND jsonb_typeof(access_summary) = 'object')),
    CONSTRAINT ck_rights_request_access_fulfilled CHECK (type <> 'access' OR outcome_code IS NULL
        OR outcome_code NOT IN ('fulfilled', 'partially_fulfilled') OR access_summary IS NOT NULL),
    CONSTRAINT ck_rights_request_row_version CHECK (row_version >= 0)
);
COMMENT ON TABLE privacy.rights_request IS 'A data principal''s request to exercise a right (access, correction, grievance, nomination, export, erasure, withdraw-all), submitted by the person themself as an adult, by the active legal guardian of a minor, or by the active nominee of an adult (DPDP Act s.14), answered by the school''s privacy staff within the response deadline. Rows are never deleted: a request is evidence of how a right was honoured.';
COMMENT ON COLUMN privacy.rights_request.id IS 'Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).';
COMMENT ON COLUMN privacy.rights_request.tenant_id IS 'Owning tenant (customer organisation). Row-level security key.';
COMMENT ON COLUMN privacy.rights_request.number IS 'The request''s number within the tenant, PRV-<year>-<six digits> (privacy.number_sequence), quoted in every letter about it.';
COMMENT ON COLUMN privacy.rights_request.type IS 'The right exercised: access, correction, grievance, nomination (a nominee''s notice that the principal died or is incapacitated, with proof; closing it as fulfilled lets the nominee act for the principal), export and erasure (fulfilled by one task per module, privacy.rights_request_task), withdraw_all (every live consent the requester gave about the subject, withdrawn by the staff''s fulfilment).';
COMMENT ON COLUMN privacy.rights_request.subject_person_id IS 'The data principal the request is about (people.person.id, logical).';
COMMENT ON COLUMN privacy.rights_request.requester_person_id IS 'The person who submitted it: always the signed-in caller''s own person, never a request field (people.person.id, logical).';
COMMENT ON COLUMN privacy.rights_request.requester_capacity IS 'In which capacity the requester acts: self (an adult for themself), guardian (the active legal guardian of a minor), nominee (the principal''s active nominee, privacy.nominee; always verified by staff in person or by document).';
COMMENT ON COLUMN privacy.rights_request.status IS 'submitted (identity still to verify) → verified → in_progress (assigned) → closed; submitted or verified → rejected. An erasure or export request in progress whose module task failed for the fifth time is action_required until a retry puts it back in progress; once every task answered it is closed with outcome fulfilled or partially_fulfilled. An extension is not a status: it moves due_at.';
COMMENT ON COLUMN privacy.rights_request.identity_check IS 'How the requester''s identity was established: signed_in_session (submitted while signed in), in_person or document (verified by staff); NULL until verified.';
COMMENT ON COLUMN privacy.rights_request.details IS 'What the request asks, typed per request type by the application: {"description": "..."} (the requester''s own words, at most 4000 characters), with "correction": {module, recordType, recordId, fields: [{field, requestedValue}]} for a correction and "grievanceCategory" for a grievance.';
COMMENT ON COLUMN privacy.rights_request.assignee_user_id IS 'The staff member handling the request (iam.user_account.id, logical); required while in_progress.';
COMMENT ON COLUMN privacy.rights_request.submitted_at IS 'When the request was submitted; the response deadline runs from it.';
COMMENT ON COLUMN privacy.rights_request.due_at IS 'The response deadline: submitted_at plus the module''s period for the type (mysms.privacy.rights.sla.<type>), moved later only by an extension. Overdue = still open past it (derived, not stored).';
COMMENT ON COLUMN privacy.rights_request.extended_reason IS 'Why the deadline was last extended (staff text); NULL = never extended. Every extension is also in the audit trail with its reason.';
COMMENT ON COLUMN privacy.rights_request.closed_at IS 'When the request was closed or rejected; NULL while it is open.';
COMMENT ON COLUMN privacy.rights_request.outcome_code IS 'How it ended: fulfilled, partially_fulfilled or declined (closed); identity_not_verified, not_entitled, duplicate or other (rejected). Shown to the requester, who may appeal a rejection as a grievance.';
COMMENT ON COLUMN privacy.rights_request.outcome_note IS 'The staff''s note on closing (staff text, not shown to the requester); NULL = none.';
COMMENT ON COLUMN privacy.rights_request.outcome_response IS 'The school''s answer shown to the requester on closing: required to close a grievance and to decline a correction; NULL = none. Never in an event.';
COMMENT ON COLUMN privacy.rights_request.outcome_file_id IS 'The export package or response letter (platform.file_object, logical); not written yet: the export package is assembled from the export tasks'' parts in a later phase.';
COMMENT ON COLUMN privacy.rights_request.access_summary IS 'The access summary sent in answer to an access request, kept as it was when the request was fulfilled (privacy.FulfilAccessRequest), with its schemaVersion: the tenant''s register activities that may concern the subject with their purposes, legal bases, data categories, recipients, retention and the widths the subject''s consent allowed, generated from privacy''s own register — no other module''s data. Set only on a closed access request, and always on one closed as fulfilled (an access request is fulfilled only with its summary; CloseRightsRequest may only decline it); NULL otherwise. Never in an event.';
COMMENT ON COLUMN privacy.rights_request.created_at IS 'Row creation time.';
COMMENT ON COLUMN privacy.rights_request.created_by IS 'User account that created the row (audit reference to iam.user_account; not FK-constrained).';
COMMENT ON COLUMN privacy.rights_request.updated_at IS 'Last modification time (set by the application on every update).';
COMMENT ON COLUMN privacy.rights_request.updated_by IS 'User account that last modified the row (audit reference; not FK-constrained).';
COMMENT ON COLUMN privacy.rights_request.row_version IS 'Optimistic-lock version, incremented on every update; used as the ETag.';
CREATE INDEX ix_rights_request_queue ON privacy.rights_request (tenant_id, status, due_at) WHERE closed_at IS NULL;
CREATE INDEX ix_rights_request_requester ON privacy.rights_request (tenant_id, requester_person_id, submitted_at);
COMMENT ON INDEX privacy.ix_rights_request_queue IS 'The open requests by status and deadline (the staff queue, overdue requests).';
COMMENT ON INDEX privacy.ix_rights_request_requester IS 'A requester''s own requests, newest first (self-service).';

CREATE TABLE privacy.nominee (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    principal_person_id uuid NOT NULL,
    nominee_person_id uuid,
    nominee_name_enc text NOT NULL,
    nominee_contact_enc text NOT NULL,
    status varchar(10) NOT NULL DEFAULT 'active',
    activated_at timestamptz,
    activated_by_request_id uuid,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_nominee PRIMARY KEY (id),
    CONSTRAINT uq_nominee_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT fk_nominee_principal_person_id FOREIGN KEY (tenant_id, principal_person_id)
        REFERENCES privacy.subject (tenant_id, person_id),
    CONSTRAINT fk_nominee_activated_by_request_id FOREIGN KEY (activated_by_request_id, tenant_id)
        REFERENCES privacy.rights_request (id, tenant_id),
    CONSTRAINT ck_nominee_status CHECK (status IN ('active', 'revoked')),
    CONSTRAINT ck_nominee_revoked CHECK ((status = 'revoked') = (revoked_at IS NOT NULL)),
    CONSTRAINT ck_nominee_activated CHECK ((activated_at IS NULL) = (activated_by_request_id IS NULL)),
    CONSTRAINT ck_nominee_self CHECK (nominee_person_id IS DISTINCT FROM principal_person_id),
    CONSTRAINT ck_nominee_row_version CHECK (row_version >= 0)
);
COMMENT ON TABLE privacy.nominee IS 'The person an adult data principal names to exercise their rights on their death or incapacity (DPDP Act s.14). One active nominee per principal; naming another revokes the one before. The nominee acts for the principal only after staff fulfil the nominee''s nomination request (proof of death or incapacity, verified in person or by document). Rows are never deleted: a nomination is evidence.';
COMMENT ON COLUMN privacy.nominee.id IS 'Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).';
COMMENT ON COLUMN privacy.nominee.tenant_id IS 'Owning tenant (customer organisation). Row-level security key.';
COMMENT ON COLUMN privacy.nominee.principal_person_id IS 'The adult data principal who named the nominee (privacy.subject).';
COMMENT ON COLUMN privacy.nominee.nominee_person_id IS 'The nominee''s person when they are known to the school and sign in (people.person.id, logical); NULL for a nominee outside the school, who reaches the school''s privacy staff in person.';
COMMENT ON COLUMN privacy.nominee.nominee_name_enc IS 'The nominee''s name, encrypted per tenant (core FieldCipher); shown masked.';
COMMENT ON COLUMN privacy.nominee.nominee_contact_enc IS 'The nominee''s phone number or e-mail address, encrypted per tenant (core FieldCipher); shown masked.';
COMMENT ON COLUMN privacy.nominee.status IS 'active (the principal''s current nominee) or revoked (by the principal, or replaced by a newer nominee).';
COMMENT ON COLUMN privacy.nominee.activated_at IS 'When the nominee became able to act for the principal: the closing as fulfilled of their nomination request; NULL until then.';
COMMENT ON COLUMN privacy.nominee.activated_by_request_id IS 'The nomination request whose fulfilment activated the nominee (privacy.rights_request).';
COMMENT ON COLUMN privacy.nominee.revoked_at IS 'When the nomination was revoked or replaced; NULL while active.';
COMMENT ON COLUMN privacy.nominee.created_at IS 'Row creation time.';
COMMENT ON COLUMN privacy.nominee.created_by IS 'User account that created the row (audit reference to iam.user_account; not FK-constrained).';
COMMENT ON COLUMN privacy.nominee.updated_at IS 'Last modification time (set by the application on every update).';
COMMENT ON COLUMN privacy.nominee.updated_by IS 'User account that last modified the row (audit reference; not FK-constrained).';
COMMENT ON COLUMN privacy.nominee.row_version IS 'Optimistic-lock version, incremented on every update; used as the ETag.';
CREATE UNIQUE INDEX uq_nominee_active ON privacy.nominee (tenant_id, principal_person_id) WHERE status = 'active';
CREATE INDEX ix_nominee_nominee_person_id ON privacy.nominee (tenant_id, nominee_person_id) WHERE status = 'active';
CREATE INDEX ix_nominee_activated_by_request_id ON privacy.nominee (activated_by_request_id);
COMMENT ON INDEX privacy.uq_nominee_active IS 'One active nominee per principal.';
COMMENT ON INDEX privacy.ix_nominee_nominee_person_id IS 'The principals a signed-in person is the active nominee of (submitting as a nominee).';
COMMENT ON INDEX privacy.ix_nominee_activated_by_request_id IS 'The nominee a nomination request activated.';

CREATE TABLE privacy.rights_request_task (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    request_id uuid NOT NULL,
    module varchar(30) NOT NULL,
    action varchar(12) NOT NULL,
    wave smallint NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'pending',
    attempts integer NOT NULL DEFAULT 0,
    dispatched_at timestamptz,
    completed_at timestamptz,
    result jsonb,
    last_error_code varchar(60),
    next_attempt_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_rights_request_task PRIMARY KEY (id),
    CONSTRAINT uq_rights_request_task UNIQUE (tenant_id, request_id, module, action),
    CONSTRAINT fk_rights_request_task_request_id FOREIGN KEY (request_id, tenant_id)
        REFERENCES privacy.rights_request (id, tenant_id),
    CONSTRAINT ck_rights_request_task_module CHECK (module ~ '^[a-z][a-z_]{1,29}$'),
    CONSTRAINT ck_rights_request_task_action CHECK (action IN ('erase', 'export')),
    CONSTRAINT ck_rights_request_task_wave CHECK (wave IN (1, 2)),
    CONSTRAINT ck_rights_request_task_status CHECK (status IN ('pending', 'dispatched', 'done', 'partial', 'failed',
        'not_applicable')),
    CONSTRAINT ck_rights_request_task_attempts CHECK ((status = 'pending') = (attempts = 0) AND attempts >= 0),
    CONSTRAINT ck_rights_request_task_dispatched CHECK ((status = 'pending') = (dispatched_at IS NULL)),
    CONSTRAINT ck_rights_request_task_completed CHECK ((status IN ('pending', 'dispatched')) = (completed_at IS NULL)),
    CONSTRAINT ck_rights_request_task_result CHECK (result IS NULL
        OR (completed_at IS NOT NULL AND jsonb_typeof(result) = 'object')),
    CONSTRAINT ck_rights_request_task_next_attempt CHECK (next_attempt_at IS NULL
        OR (status = 'failed' AND attempts < 5)),
    CONSTRAINT ck_rights_request_task_row_version CHECK (row_version >= 0)
);
COMMENT ON TABLE privacy.rights_request_task IS 'One module''s part of fulfilling an erasure or export request (design §9.2): created when the staff start the fulfilment, dispatched to the module as privacy.rights_request.task_assigned (ids only), answered by the module''s <module>.privacy_task.completed event. Rows are never deleted: the results are evidence of how the right was honoured.';
COMMENT ON COLUMN privacy.rights_request_task.id IS 'Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback); the id the module answers with, the same on every re-dispatch.';
COMMENT ON COLUMN privacy.rights_request_task.tenant_id IS 'Owning tenant (customer organisation). Row-level security key.';
COMMENT ON COLUMN privacy.rights_request_task.request_id IS 'The request the task fulfils (privacy.rights_request).';
COMMENT ON COLUMN privacy.rights_request_task.module IS 'The module that carries the task out (a module code of mysms.privacy.erasure.modules).';
COMMENT ON COLUMN privacy.rights_request_task.action IS 'erase (an erasure request) or export (an export request).';
COMMENT ON COLUMN privacy.rights_request_task.wave IS '1: dispatched at once (for an erasure, the modules of mysms.privacy.erasure.first-wave, comms and iam, which stop messages and sign-in; every export task); 2: dispatched once every wave-1 task of the request is done or not applicable.';
COMMENT ON COLUMN privacy.rights_request_task.status IS 'pending (its wave has not started) → dispatched → done, partial (rows kept under a legal hold or a legal-retention policy), failed or not_applicable. A failed answer waits for its next attempt (next_attempt_at) and is dispatched again until the fifth attempt; a retry dispatches a failed or unanswered task again at once.';
COMMENT ON COLUMN privacy.rights_request_task.attempts IS 'How many times the task was dispatched (the first time included); 0 while pending.';
COMMENT ON COLUMN privacy.rights_request_task.dispatched_at IS 'When the task was last dispatched; a task dispatched longer ago than mysms.privacy.erasure.redispatch-after without an answer is dispatched again by the sweep job. NULL while pending.';
COMMENT ON COLUMN privacy.rights_request_task.completed_at IS 'When the module''s answer arrived; NULL while pending or dispatched.';
COMMENT ON COLUMN privacy.rights_request_task.result IS 'The module''s answer, counts and codes only: {"counts": {table: rows}, "heldItems": n, "policyCodes": [...], "fileId": "..."}; NULL until answered. Never names or values of the erased data.';
COMMENT ON COLUMN privacy.rights_request_task.last_error_code IS 'The error code of the module''s last failed answer; NULL while none failed.';
COMMENT ON COLUMN privacy.rights_request_task.next_attempt_at IS 'When a failed task is dispatched again by the sweep job: the failed answer''s time plus mysms.privacy.erasure.retry-delay doubled per attempt after the first, at most mysms.privacy.erasure.redispatch-after. NULL unless the task failed before its fifth attempt; cleared by every dispatch.';
COMMENT ON COLUMN privacy.rights_request_task.created_at IS 'Row creation time.';
COMMENT ON COLUMN privacy.rights_request_task.created_by IS 'User account that started the fulfilment (audit reference to iam.user_account; not FK-constrained).';
COMMENT ON COLUMN privacy.rights_request_task.updated_at IS 'Last modification time (set by the application on every update).';
COMMENT ON COLUMN privacy.rights_request_task.updated_by IS 'User account that last modified the row (audit reference; not FK-constrained); NULL for a change made by a module''s answer or the sweep job.';
COMMENT ON COLUMN privacy.rights_request_task.row_version IS 'Optimistic-lock version, incremented on every update; used as the ETag.';
CREATE INDEX ix_rights_request_task_request_id ON privacy.rights_request_task (request_id, wave, module);
CREATE INDEX ix_rights_request_task_dispatched ON privacy.rights_request_task (tenant_id, dispatched_at)
    WHERE status = 'dispatched';
COMMENT ON INDEX privacy.ix_rights_request_task_request_id IS 'A request''s tasks, wave by wave.';
COMMENT ON INDEX privacy.ix_rights_request_task_dispatched IS 'The tasks waiting for their module''s answer, the longest waiting first (the sweep job).';
CREATE INDEX ix_rights_request_task_next_attempt ON privacy.rights_request_task (tenant_id, next_attempt_at)
    WHERE status = 'failed' AND next_attempt_at IS NOT NULL;
COMMENT ON INDEX privacy.ix_rights_request_task_next_attempt IS 'The failed tasks waiting for their next attempt, the earliest due first (the sweep job).';

-- === Legal holds (design §5.5, §10): what erasure and retention must keep ===

CREATE TABLE privacy.legal_hold (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    reason_code varchar(20) NOT NULL,
    scope varchar(10) NOT NULL,
    subject_person_id uuid,
    category_code varchar(40),
    module varchar(30),
    entity_type varchar(80),
    entity_ids uuid[],
    placed_by_user_id uuid NOT NULL,
    placed_at timestamptz NOT NULL,
    review_by date NOT NULL,
    released_at timestamptz,
    released_by_user_id uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_legal_hold PRIMARY KEY (id),
    CONSTRAINT uq_legal_hold_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT fk_legal_hold_category_code FOREIGN KEY (category_code) REFERENCES privacy.data_category (code),
    CONSTRAINT ck_legal_hold_reason_code CHECK (reason_code IN ('safeguarding', 'litigation', 'regulator', 'audit')),
    CONSTRAINT ck_legal_hold_scope CHECK (scope IN ('subject', 'category', 'records')),
    CONSTRAINT ck_legal_hold_subject CHECK (scope <> 'subject' OR (subject_person_id IS NOT NULL
        AND category_code IS NULL AND module IS NULL AND entity_type IS NULL AND entity_ids IS NULL)),
    CONSTRAINT ck_legal_hold_category CHECK (scope <> 'category' OR (category_code IS NOT NULL
        AND module IS NULL AND entity_type IS NULL AND entity_ids IS NULL)),
    CONSTRAINT ck_legal_hold_records CHECK (scope <> 'records' OR (category_code IS NULL AND module IS NOT NULL
        AND entity_type IS NOT NULL AND cardinality(entity_ids) BETWEEN 1 AND 1000)),
    CONSTRAINT ck_legal_hold_module CHECK (module IS NULL OR module ~ '^[a-z][a-z_]{1,29}$'),
    CONSTRAINT ck_legal_hold_entity_type CHECK (entity_type IS NULL OR length(entity_type) BETWEEN 1 AND 80),
    CONSTRAINT ck_legal_hold_released CHECK ((released_at IS NULL) = (released_by_user_id IS NULL)),
    CONSTRAINT ck_legal_hold_released_at CHECK (released_at IS NULL OR released_at >= placed_at),
    CONSTRAINT ck_legal_hold_row_version CHECK (row_version >= 0)
);
COMMENT ON TABLE privacy.legal_hold IS 'A legal hold placed by the school''s data protection officer or safeguarding lead: a subject''s data, a data category (of a subject or of everyone) or a set of records that no erasure task or retention sweep may remove while the hold is active. Modules ask privacy which holds apply (ListActiveHolds) and keep and count what they cover. Every hold has a review date: holds are reviewed, never forgotten. Rows are never deleted: a hold is evidence.';
COMMENT ON COLUMN privacy.legal_hold.id IS 'Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).';
COMMENT ON COLUMN privacy.legal_hold.tenant_id IS 'Owning tenant (customer organisation). Row-level security key.';
COMMENT ON COLUMN privacy.legal_hold.reason_code IS 'Why the data is held: safeguarding, litigation, regulator (an inquiry or order) or audit.';
COMMENT ON COLUMN privacy.legal_hold.scope IS 'What is held: subject (all the subject''s data), category (one data category, of the subject or of everyone) or records (named records of one module). Fixed once placed.';
COMMENT ON COLUMN privacy.legal_hold.subject_person_id IS 'The data principal whose data is held (people.person.id, logical): required for scope subject; for category and records NULL = everyone''s.';
COMMENT ON COLUMN privacy.legal_hold.category_code IS 'The data category held (privacy.data_category): scope category only.';
COMMENT ON COLUMN privacy.legal_hold.module IS 'The module holding the records (module code): scope records only.';
COMMENT ON COLUMN privacy.legal_hold.entity_type IS 'The kind of record held, as its module names it (e.g. student): scope records only.';
COMMENT ON COLUMN privacy.legal_hold.entity_ids IS 'The records held (1 to 1000 ids of the module''s rows, logical): scope records only.';
COMMENT ON COLUMN privacy.legal_hold.placed_by_user_id IS 'The staff member who placed the hold (iam.user_account.id, logical).';
COMMENT ON COLUMN privacy.legal_hold.placed_at IS 'When the hold was placed.';
COMMENT ON COLUMN privacy.legal_hold.review_by IS 'When the hold must be reviewed (today or later when set); the hold stays active past it, listed as due for review, until released.';
COMMENT ON COLUMN privacy.legal_hold.released_at IS 'When the hold was released; NULL while active.';
COMMENT ON COLUMN privacy.legal_hold.released_by_user_id IS 'The staff member who released the hold (iam.user_account.id, logical); NULL while active.';
COMMENT ON COLUMN privacy.legal_hold.created_at IS 'Row creation time.';
COMMENT ON COLUMN privacy.legal_hold.created_by IS 'User account that created the row (audit reference to iam.user_account; not FK-constrained).';
COMMENT ON COLUMN privacy.legal_hold.updated_at IS 'Last modification time (set by the application on every update).';
COMMENT ON COLUMN privacy.legal_hold.updated_by IS 'User account that last modified the row (audit reference; not FK-constrained).';
COMMENT ON COLUMN privacy.legal_hold.row_version IS 'Optimistic-lock version, incremented on every update; used as the ETag.';
CREATE INDEX ix_legal_hold_active ON privacy.legal_hold (tenant_id, subject_person_id) WHERE released_at IS NULL;
CREATE INDEX ix_legal_hold_review ON privacy.legal_hold (tenant_id, review_by) WHERE released_at IS NULL;
CREATE INDEX ix_legal_hold_category_code ON privacy.legal_hold (category_code);
COMMENT ON INDEX privacy.ix_legal_hold_active IS 'The active holds that may cover a subject''s data (ListActiveHolds).';
COMMENT ON INDEX privacy.ix_legal_hold_review IS 'The active holds by review date (the register''s default order, holds due for review).';
COMMENT ON INDEX privacy.ix_legal_hold_category_code IS 'The holds of a data category (foreign key).';

-- === Breach register (design §5.6, §11; P4) ===

CREATE TABLE privacy.breach (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    number varchar(30) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'reported',
    detected_at timestamptz NOT NULL,
    became_aware_at timestamptz NOT NULL,
    board_report_due_at timestamptz NOT NULL,
    description text NOT NULL,
    categories varchar(40)[] NOT NULL DEFAULT '{}',
    modules varchar(30)[] NOT NULL DEFAULT '{}',
    estimated_principals integer,
    likely_consequences text,
    mitigation text,
    owner_user_id uuid NOT NULL,
    board_report_sent_at timestamptz,
    not_a_breach_reason text,
    closed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_breach PRIMARY KEY (id),
    CONSTRAINT uq_breach_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT uq_breach_number UNIQUE (tenant_id, number),
    CONSTRAINT ck_breach_number CHECK (number ~ '^PRB-[0-9]{4}-[0-9]{6,}$'),
    CONSTRAINT ck_breach_status CHECK (status IN ('reported', 'triaged', 'contained', 'board_initial_sent',
        'board_report_sent', 'principals_notified', 'closed', 'not_a_breach')),
    CONSTRAINT ck_breach_aware CHECK (became_aware_at >= detected_at),
    CONSTRAINT ck_breach_due CHECK (board_report_due_at = became_aware_at + interval '72 hours'),
    CONSTRAINT ck_breach_description CHECK (length(description) BETWEEN 1 AND 4000),
    CONSTRAINT ck_breach_estimated_principals CHECK (estimated_principals IS NULL OR estimated_principals >= 0),
    CONSTRAINT ck_breach_likely_consequences CHECK (likely_consequences IS NULL
        OR length(likely_consequences) BETWEEN 1 AND 4000),
    CONSTRAINT ck_breach_mitigation CHECK (mitigation IS NULL OR length(mitigation) BETWEEN 1 AND 4000),
    CONSTRAINT ck_breach_board_report_sent CHECK (CASE
        WHEN status IN ('board_report_sent', 'closed') THEN board_report_sent_at IS NOT NULL
        WHEN status = 'principals_notified' THEN true
        ELSE board_report_sent_at IS NULL END),
    CONSTRAINT ck_breach_board_report_sent_at CHECK (board_report_sent_at IS NULL
        OR board_report_sent_at >= became_aware_at),
    CONSTRAINT ck_breach_not_a_breach CHECK ((status = 'not_a_breach') = (not_a_breach_reason IS NOT NULL)),
    CONSTRAINT ck_breach_not_a_breach_reason CHECK (not_a_breach_reason IS NULL
        OR length(not_a_breach_reason) BETWEEN 1 AND 1000),
    CONSTRAINT ck_breach_closed CHECK ((status IN ('closed', 'not_a_breach')) = (closed_at IS NOT NULL)),
    CONSTRAINT ck_breach_closed_at CHECK (closed_at IS NULL OR closed_at >= became_aware_at),
    CONSTRAINT ck_breach_row_version CHECK (row_version >= 0)
);
COMMENT ON TABLE privacy.breach IS 'The tenant''s register of personal data breaches (DPDP Rules: intimation to the Data Protection Board and to each affected data principal), kept by the school''s data protection officer: when it happened and when the school became aware, the Board''s 72-hour clock, what was affected in ids and counts, and each notice sent (privacy.breach_notification). Rows are never deleted: the register is evidence of how a breach was handled.';
COMMENT ON COLUMN privacy.breach.id IS 'Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).';
COMMENT ON COLUMN privacy.breach.tenant_id IS 'Owning tenant (customer organisation). Row-level security key.';
COMMENT ON COLUMN privacy.breach.number IS 'The breach''s number within the tenant, PRB-<year>-<six digits> (privacy.number_sequence), quoted in every notice about it.';
COMMENT ON COLUMN privacy.breach.status IS 'reported → triaged → contained → board_initial_sent → board_report_sent → principals_notified → closed (the principals may also be told before the Board''s detailed report); triaged → not_a_breach. Notices are recorded in privacy.breach_notification and move the status where they apply.';
COMMENT ON COLUMN privacy.breach.detected_at IS 'When the breach was detected (or, when known, happened).';
COMMENT ON COLUMN privacy.breach.became_aware_at IS 'When the school became aware of the breach: the start of the Board''s clock (which instant counts is to be verified with counsel). Fixed once reported.';
COMMENT ON COLUMN privacy.breach.board_report_due_at IS 'became_aware_at plus 72 hours: when the detailed report to the Board is due. Overdue and the hours remaining are derived at read time, never stored.';
COMMENT ON COLUMN privacy.breach.description IS 'What happened, in ids and counts only (record ids, table names, numbers of rows or people) — never a name, contact or other personal data; never in an event.';
COMMENT ON COLUMN privacy.breach.categories IS 'Codes of the data categories affected (privacy.data_category.code; checked by the application); empty while unknown.';
COMMENT ON COLUMN privacy.breach.modules IS 'The MySMS modules whose data is affected (module codes); empty while unknown.';
COMMENT ON COLUMN privacy.breach.estimated_principals IS 'How many data principals are estimated to be affected; NULL while unknown.';
COMMENT ON COLUMN privacy.breach.likely_consequences IS 'The likely consequences for the principals (staff text, ids and counts only; at most 4000 characters); NULL while not assessed. Never in an event.';
COMMENT ON COLUMN privacy.breach.mitigation IS 'What was done or is planned to contain the breach and protect the principals (staff text, ids and counts only); NULL while none. Never in an event.';
COMMENT ON COLUMN privacy.breach.owner_user_id IS 'The staff member handling the breach (iam.user_account.id, logical): the reporter until reassigned.';
COMMENT ON COLUMN privacy.breach.board_report_sent_at IS 'When the detailed report to the Board was sent (its breach_notification''s sent_at); NULL until then.';
COMMENT ON COLUMN privacy.breach.not_a_breach_reason IS 'Why triage found it was not a personal data breach (staff text, at most 1000 characters); set only with status not_a_breach. Never in an event.';
COMMENT ON COLUMN privacy.breach.closed_at IS 'When the breach was closed or found not to be a breach; NULL while open.';
COMMENT ON COLUMN privacy.breach.created_at IS 'Row creation time.';
COMMENT ON COLUMN privacy.breach.created_by IS 'User account that created the row (audit reference to iam.user_account; not FK-constrained).';
COMMENT ON COLUMN privacy.breach.updated_at IS 'Last modification time (set by the application on every update).';
COMMENT ON COLUMN privacy.breach.updated_by IS 'User account that last modified the row (audit reference; not FK-constrained).';
COMMENT ON COLUMN privacy.breach.row_version IS 'Optimistic-lock version, incremented on every update; used as the ETag.';
CREATE INDEX ix_breach_open ON privacy.breach (tenant_id, board_report_due_at) WHERE closed_at IS NULL;
COMMENT ON INDEX privacy.ix_breach_open IS 'The open breaches by the Board''s deadline (the register''s default order, the clocks).';

CREATE TABLE privacy.breach_notification (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    breach_id uuid NOT NULL,
    audience varchar(12) NOT NULL,
    kind varchar(10) NOT NULL,
    sent_at timestamptz NOT NULL,
    document_file_id uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_breach_notification PRIMARY KEY (id),
    CONSTRAINT fk_breach_notification_breach_id FOREIGN KEY (breach_id, tenant_id)
        REFERENCES privacy.breach (id, tenant_id),
    CONSTRAINT ck_breach_notification_audience CHECK (audience IN ('board', 'principals', 'processor', 'tenant')),
    CONSTRAINT ck_breach_notification_kind CHECK (kind IN ('initial', 'detailed', 'update')),
    CONSTRAINT ck_breach_notification_row_version CHECK (row_version >= 0)
);
COMMENT ON TABLE privacy.breach_notification IS 'A notice about a breach the school sent: to the Data Protection Board, the affected principals, a processor or (when MySMS as processor tells the school) the tenant. Recorded by the data protection officer after sending it outside MySMS: delivery through comms is not built yet. Append-only: rows are never changed or deleted.';
COMMENT ON COLUMN privacy.breach_notification.id IS 'Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).';
COMMENT ON COLUMN privacy.breach_notification.tenant_id IS 'Owning tenant (customer organisation). Row-level security key.';
COMMENT ON COLUMN privacy.breach_notification.breach_id IS 'The breach the notice is about (privacy.breach).';
COMMENT ON COLUMN privacy.breach_notification.audience IS 'Who was told: board (the Data Protection Board), principals (the affected data principals), processor, tenant.';
COMMENT ON COLUMN privacy.breach_notification.kind IS 'initial (intimation without delay), detailed (the Board''s report within 72 hours, or the principals'' full notice), update.';
COMMENT ON COLUMN privacy.breach_notification.sent_at IS 'When the notice was sent; not in the future and not before the school became aware of the breach.';
COMMENT ON COLUMN privacy.breach_notification.document_file_id IS 'The notice as sent (platform.file_object, logical); NULL when not attached.';
COMMENT ON COLUMN privacy.breach_notification.created_at IS 'Row creation time.';
COMMENT ON COLUMN privacy.breach_notification.created_by IS 'User account that recorded the notice (audit reference to iam.user_account; not FK-constrained).';
COMMENT ON COLUMN privacy.breach_notification.updated_at IS 'Last modification time; equals created_at (rows are never changed).';
COMMENT ON COLUMN privacy.breach_notification.updated_by IS 'User account that last modified the row (audit reference; not FK-constrained).';
COMMENT ON COLUMN privacy.breach_notification.row_version IS 'Optimistic-lock version; always 1 (rows are never changed).';
CREATE INDEX ix_breach_notification_breach_id ON privacy.breach_notification (breach_id, sent_at);
COMMENT ON INDEX privacy.ix_breach_notification_breach_id IS 'A breach''s notices in the order they were sent.';

CREATE TABLE privacy.number_sequence (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    sequence_code varchar(40) NOT NULL,
    period_key varchar(20) NOT NULL DEFAULT '',
    prefix varchar(20),
    padding smallint NOT NULL DEFAULT 6,
    next_value bigint NOT NULL DEFAULT 1,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_privacy_number_sequence PRIMARY KEY (id),
    CONSTRAINT uq_privacy_number_sequence_1 UNIQUE (tenant_id, sequence_code, period_key)
);
COMMENT ON TABLE privacy.number_sequence IS 'Module-local gap-aware number source of the privacy module (rights request and breach numbers), one counter per tenant, sequence code and period: privacy''s data is tenant-level, so no counter belongs to an institution. Read with SELECT ... FOR UPDATE inside the issuing transaction; never shared with another module.';
COMMENT ON COLUMN privacy.number_sequence.id IS 'Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).';
COMMENT ON COLUMN privacy.number_sequence.tenant_id IS 'Owning tenant (customer organisation). Row-level security key.';
COMMENT ON COLUMN privacy.number_sequence.sequence_code IS 'Which number series of this module (rights_request, breach).';
COMMENT ON COLUMN privacy.number_sequence.period_key IS 'Reset period (the calendar year of submission or report, such as 2026; empty = never resets).';
COMMENT ON COLUMN privacy.number_sequence.prefix IS 'Rendered number prefix.';
COMMENT ON COLUMN privacy.number_sequence.padding IS 'Zero padding width of the numeric part.';
COMMENT ON COLUMN privacy.number_sequence.next_value IS 'Next number to issue.';
COMMENT ON COLUMN privacy.number_sequence.created_at IS 'Row creation time.';
COMMENT ON COLUMN privacy.number_sequence.created_by IS 'User account that created the row (audit reference to iam.user_account; not FK-constrained).';
COMMENT ON COLUMN privacy.number_sequence.updated_at IS 'Last modification time (set by the application on every update).';
COMMENT ON COLUMN privacy.number_sequence.updated_by IS 'User account that last modified the row (audit reference; not FK-constrained).';
COMMENT ON COLUMN privacy.number_sequence.deleted_at IS 'Soft-delete timestamp (NULL = live row).';
COMMENT ON COLUMN privacy.number_sequence.row_version IS 'Optimistic-lock version, incremented on every update; used as the ETag.';

-- === Retention (design §5.6, §10): the platform's policies, each tenant's setting, the daily runs ===

CREATE TABLE privacy.retention_policy (
    id uuid NOT NULL,
    code varchar(60) NOT NULL,
    category_code varchar(40) NOT NULL,
    module varchar(30) NOT NULL,
    trigger_code varchar(20) NOT NULL,
    period_days integer NOT NULL,
    minimum_period_days integer NOT NULL,
    action varchar(10) NOT NULL,
    notify_before_days integer,
    legal_reference text NOT NULL,
    CONSTRAINT pk_retention_policy PRIMARY KEY (id),
    CONSTRAINT uq_retention_policy_code UNIQUE (code),
    CONSTRAINT fk_retention_policy_category_code FOREIGN KEY (category_code) REFERENCES privacy.data_category (code),
    CONSTRAINT ck_retention_policy_module CHECK (module ~ '^[a-z][a-z_]{1,29}$'),
    CONSTRAINT ck_retention_policy_trigger_code CHECK (trigger_code IN ('after_leaving', 'after_record_date',
        'after_inactivity', 'after_purpose_end')),
    CONSTRAINT ck_retention_policy_floor CHECK (minimum_period_days >= 1 AND period_days >= minimum_period_days),
    CONSTRAINT ck_retention_policy_action CHECK (action IN ('delete', 'anonymise', 'archive')),
    CONSTRAINT ck_retention_policy_notify_before CHECK (notify_before_days IS NULL OR notify_before_days >= 1)
);
COMMENT ON TABLE privacy.retention_policy IS 'The platform''s retention catalogue: how long each kind of personal data is kept after its trigger and what its module does with it then, with the legal floor below which no tenant may go. Seeded by this baseline; a tenant''s own period and action live in privacy.tenant_retention_policy. Every period, floor and reference is conservative and TO VERIFY with counsel and the owner (design §10, brief Q11).';
COMMENT ON COLUMN privacy.retention_policy.id IS 'Stable id, the same in every environment: md5(''mysms.retention_policy:'' || code)::uuid.';
COMMENT ON COLUMN privacy.retention_policy.code IS 'Policy code <module>.<what>, e.g. attendance.after_leaving.';
COMMENT ON COLUMN privacy.retention_policy.category_code IS 'The data category kept (privacy.data_category).';
COMMENT ON COLUMN privacy.retention_policy.module IS 'The module holding the data, which sweeps it on privacy.retention.due.';
COMMENT ON COLUMN privacy.retention_policy.trigger_code IS 'When the period starts: after_leaving (the person left the school), after_record_date (the record''s own date), after_inactivity (the account''s last use), after_purpose_end (consent withdrawn or the purpose ended).';
COMMENT ON COLUMN privacy.retention_policy.period_days IS 'How many days the data is kept after the trigger by default (to verify).';
COMMENT ON COLUMN privacy.retention_policy.minimum_period_days IS 'The legal floor in days: no tenant period may be shorter (to verify).';
COMMENT ON COLUMN privacy.retention_policy.action IS 'What the module does with data past the period by default: delete, anonymise or archive.';
COMMENT ON COLUMN privacy.retention_policy.notify_before_days IS 'How many days before the sweep the person is told (DPDP Rules: erasure after inactivity, to verify); NULL = not told. The notice through comms is not built yet.';
COMMENT ON COLUMN privacy.retention_policy.legal_reference IS 'The law or rule the period and floor come from, as understood today (to verify with counsel).';

-- Conservative defaults, every one TO VERIFY with counsel and the owner (design §10, brief Q11).
CREATE INDEX ix_retention_policy_category_code ON privacy.retention_policy (category_code);
COMMENT ON INDEX privacy.ix_retention_policy_category_code IS 'The policies of a data category (foreign key).';

INSERT INTO privacy.retention_policy (id, code, category_code, module, trigger_code, period_days, minimum_period_days, action, notify_before_days, legal_reference) VALUES
    (md5('mysms.retention_policy:' || 'admission.applicant_not_admitted')::uuid, 'admission.applicant_not_admitted', 'identity', 'admission', 'after_record_date', 365, 90, 'delete', NULL, 'No statutory period known; one admission cycle (to verify).'),
    (md5('mysms.retention_policy:' || 'assessment.after_leaving')::uuid, 'assessment.after_leaving', 'academic_record', 'assessment', 'after_leaving', 3653, 1826, 'archive', NULL, 'Board and state record-keeping rules for marks and certificates (to verify).'),
    (md5('mysms.retention_policy:' || 'attendance.after_leaving')::uuid, 'attendance.after_leaving', 'attendance', 'attendance', 'after_leaving', 1826, 1095, 'anonymise', NULL, 'State education rules for attendance registers (to verify).'),
    (md5('mysms.retention_policy:' || 'comms.media_after_withdrawal')::uuid, 'comms.media_after_withdrawal', 'photo_media', 'comms', 'after_purpose_end', 30, 7, 'delete', NULL, 'DPDP Act s.8(7): erase once consent is withdrawn (to verify).'),
    (md5('mysms.retention_policy:' || 'finance.after_leaving')::uuid, 'finance.after_leaving', 'financial', 'finance', 'after_leaving', 2922, 2922, 'archive', NULL, 'Books of account kept eight years (Income-tax Act, Companies Act; to verify).'),
    (md5('mysms.retention_policy:' || 'frontoffice.visitor_log')::uuid, 'frontoffice.visitor_log', 'identity', 'frontoffice', 'after_record_date', 90, 30, 'delete', NULL, 'No statutory period known; safeguarding practice (to verify).'),
    (md5('mysms.retention_policy:' || 'iam.after_inactivity')::uuid, 'iam.after_inactivity', 'identity', 'iam', 'after_inactivity', 1095, 365, 'anonymise', 2, 'DPDP Rules: erasure after a period of inactivity, with 48 hours'' notice (to verify).'),
    (md5('mysms.retention_policy:' || 'payroll.after_leaving')::uuid, 'payroll.after_leaving', 'financial', 'payroll', 'after_leaving', 2922, 2922, 'archive', NULL, 'Wage and tax records kept eight years (to verify).'),
    (md5('mysms.retention_policy:' || 'platform.access_log')::uuid, 'platform.access_log', 'device_usage', 'platform', 'after_record_date', 365, 365, 'delete', NULL, 'DPDP Rules: logs kept at least one year for security (to verify).'),
    (md5('mysms.retention_policy:' || 'sis.health_after_leaving')::uuid, 'sis.health_after_leaving', 'health', 'sis', 'after_leaving', 365, 30, 'delete', NULL, 'No statutory period known; restricted data kept the shortest (to verify).'),
    (md5('mysms.retention_policy:' || 'time_attendance.after_leaving')::uuid, 'time_attendance.after_leaving', 'attendance', 'time_attendance', 'after_leaving', 1826, 1095, 'archive', NULL, 'Staff attendance backs the wage records (payroll.after_leaving): archived, never anonymised (to verify).');

CREATE TABLE privacy.tenant_retention_policy (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    policy_id uuid NOT NULL,
    period_days integer NOT NULL,
    action varchar(10) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_tenant_retention_policy PRIMARY KEY (id),
    CONSTRAINT uq_tenant_retention_policy UNIQUE (tenant_id, policy_id),
    CONSTRAINT fk_tenant_retention_policy_policy_id FOREIGN KEY (policy_id) REFERENCES privacy.retention_policy (id),
    CONSTRAINT ck_tenant_retention_policy_period CHECK (period_days >= 1),
    CONSTRAINT ck_tenant_retention_policy_action CHECK (action IN ('delete', 'anonymise', 'archive')),
    CONSTRAINT ck_tenant_retention_policy_row_version CHECK (row_version >= 0)
);
COMMENT ON TABLE privacy.tenant_retention_policy IS 'A tenant''s own setting of one catalogue retention policy; no row = the catalogue''s period and action. Set by tenant administrators only, never below the policy''s legal floor (checked by the application: 422 RETENTION_BELOW_LEGAL_FLOOR).';
COMMENT ON COLUMN privacy.tenant_retention_policy.id IS 'Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).';
COMMENT ON COLUMN privacy.tenant_retention_policy.tenant_id IS 'Owning tenant (customer organisation). Row-level security key.';
COMMENT ON COLUMN privacy.tenant_retention_policy.policy_id IS 'The catalogue policy (privacy.retention_policy).';
COMMENT ON COLUMN privacy.tenant_retention_policy.period_days IS 'The tenant''s period in days: at least the policy''s minimum_period_days.';
COMMENT ON COLUMN privacy.tenant_retention_policy.action IS 'The tenant''s action for data past the period: delete, anonymise or archive.';
COMMENT ON COLUMN privacy.tenant_retention_policy.created_at IS 'Row creation time.';
COMMENT ON COLUMN privacy.tenant_retention_policy.created_by IS 'User account that created the row (audit reference to iam.user_account; not FK-constrained).';
COMMENT ON COLUMN privacy.tenant_retention_policy.updated_at IS 'Last modification time (set by the application on every update).';
COMMENT ON COLUMN privacy.tenant_retention_policy.updated_by IS 'User account that last modified the row (audit reference; not FK-constrained).';
COMMENT ON COLUMN privacy.tenant_retention_policy.row_version IS 'Optimistic-lock version, incremented on every update; used as the ETag (0 = never set).';
CREATE INDEX ix_tenant_retention_policy_policy_id ON privacy.tenant_retention_policy (policy_id);
COMMENT ON INDEX privacy.ix_tenant_retention_policy_policy_id IS 'The tenants'' settings of a policy (foreign key).';

CREATE TABLE privacy.retention_run (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    policy_id uuid NOT NULL,
    module varchar(30) NOT NULL,
    action varchar(10) NOT NULL,
    cutoff timestamptz NOT NULL,
    status varchar(10) NOT NULL DEFAULT 'dispatched',
    affected integer,
    held integer,
    started_at timestamptz NOT NULL,
    completed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_retention_run PRIMARY KEY (id),
    CONSTRAINT uq_retention_run UNIQUE (tenant_id, policy_id, cutoff),
    CONSTRAINT fk_retention_run_policy_id FOREIGN KEY (policy_id) REFERENCES privacy.retention_policy (id),
    CONSTRAINT ck_retention_run_module CHECK (module ~ '^[a-z][a-z_]{1,29}$'),
    CONSTRAINT ck_retention_run_action CHECK (action IN ('delete', 'anonymise', 'archive')),
    CONSTRAINT ck_retention_run_status CHECK (status IN ('dispatched', 'done', 'partial', 'failed')),
    CONSTRAINT ck_retention_run_completed CHECK ((status = 'dispatched') = (completed_at IS NULL)),
    CONSTRAINT ck_retention_run_counts CHECK ((affected IS NULL OR affected >= 0) AND (held IS NULL OR held >= 0)),
    CONSTRAINT ck_retention_run_cutoff CHECK (cutoff < started_at),
    CONSTRAINT ck_retention_run_row_version CHECK (row_version >= 0)
);
COMMENT ON TABLE privacy.retention_run IS 'One daily run of a retention policy for a tenant: the cutoff privacy sent to the module holding the data (privacy.retention.due) and, once the module reports, what it removed and kept under holds. One run per policy and cutoff day. Counts only; rows are never deleted.';
COMMENT ON COLUMN privacy.retention_run.id IS 'Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback); the id the module answers with.';
COMMENT ON COLUMN privacy.retention_run.tenant_id IS 'Owning tenant (customer organisation). Row-level security key.';
COMMENT ON COLUMN privacy.retention_run.policy_id IS 'The policy run (privacy.retention_policy).';
COMMENT ON COLUMN privacy.retention_run.module IS 'The module asked to sweep (the policy''s module).';
COMMENT ON COLUMN privacy.retention_run.action IS 'What the module does with data older than the cutoff: the tenant''s action when set, the catalogue''s otherwise, at the time of the run.';
COMMENT ON COLUMN privacy.retention_run.cutoff IS 'Data whose trigger lies before this instant (the start of the run''s UTC day minus the period) is past its period.';
COMMENT ON COLUMN privacy.retention_run.status IS 'dispatched (waiting for the module) → done, partial (rows kept under holds) or failed. Module sweeps are not built yet: a run stays dispatched until its module reports.';
COMMENT ON COLUMN privacy.retention_run.affected IS 'How many rows the module removed; NULL until it reports.';
COMMENT ON COLUMN privacy.retention_run.held IS 'How many rows the module kept under a legal hold; NULL until it reports.';
COMMENT ON COLUMN privacy.retention_run.started_at IS 'When the run was dispatched.';
COMMENT ON COLUMN privacy.retention_run.completed_at IS 'When the module reported; NULL while dispatched.';
COMMENT ON COLUMN privacy.retention_run.created_at IS 'Row creation time.';
COMMENT ON COLUMN privacy.retention_run.created_by IS 'NULL: runs are created by the scheduled retention job (audit reference to iam.user_account; not FK-constrained).';
COMMENT ON COLUMN privacy.retention_run.updated_at IS 'Last modification time (set by the application on every update).';
COMMENT ON COLUMN privacy.retention_run.updated_by IS 'User account that last modified the row (audit reference; not FK-constrained).';
COMMENT ON COLUMN privacy.retention_run.row_version IS 'Optimistic-lock version, incremented on every update; used as the ETag.';
CREATE INDEX ix_retention_run_started ON privacy.retention_run (tenant_id, started_at);
CREATE INDEX ix_retention_run_policy_id ON privacy.retention_run (policy_id);
COMMENT ON INDEX privacy.ix_retention_run_started IS 'The tenant''s runs, newest first (ListRetentionRuns).';
COMMENT ON INDEX privacy.ix_retention_run_policy_id IS 'The runs of a policy (foreign key).';

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

ALTER TABLE privacy.breach ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.breach FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.breach USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
ALTER TABLE privacy.breach_notification ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.breach_notification FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.breach_notification USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
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
ALTER TABLE privacy.legal_hold ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.legal_hold FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.legal_hold USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
ALTER TABLE privacy.notice ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.notice FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.notice USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
ALTER TABLE privacy.notice_translation ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.notice_translation FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.notice_translation USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
ALTER TABLE privacy.notice_version ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.notice_version FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.notice_version USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
ALTER TABLE privacy.nominee ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.nominee FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.nominee USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
ALTER TABLE privacy.number_sequence ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.number_sequence FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.number_sequence USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
ALTER TABLE privacy.outbox_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.outbox_event FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.outbox_event USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
ALTER TABLE privacy.rights_request ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.rights_request FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.rights_request USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
ALTER TABLE privacy.rights_request_task ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.rights_request_task FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.rights_request_task USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
ALTER TABLE privacy.retention_run ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.retention_run FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.retention_run USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
ALTER TABLE privacy.subject ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.subject FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.subject USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
ALTER TABLE privacy.tenant_purpose ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.tenant_purpose FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.tenant_purpose USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
ALTER TABLE privacy.tenant_retention_policy ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.tenant_retention_policy FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.tenant_retention_policy USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

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
CREATE INDEX ix_rights_request_subject_person_id ON privacy.rights_request (subject_person_id);
CREATE INDEX ix_rights_request_assignee_user_id ON privacy.rights_request (assignee_user_id);
CREATE INDEX ix_rights_request_outcome_file_id ON privacy.rights_request (outcome_file_id);
CREATE INDEX ix_breach_owner_user_id ON privacy.breach (owner_user_id);
CREATE INDEX ix_breach_notification_document_file_id ON privacy.breach_notification (document_file_id);
CREATE INDEX ix_legal_hold_subject_person_id ON privacy.legal_hold (subject_person_id);
CREATE INDEX ix_legal_hold_placed_by_user_id ON privacy.legal_hold (placed_by_user_id);
CREATE INDEX ix_legal_hold_released_by_user_id ON privacy.legal_hold (released_by_user_id);
