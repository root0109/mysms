# MySMS data dictionary

> **Stale.** Covers 466 tables of 21 schemas; the baselines now have 514 tables in 22 schemas (privacy and later
> additions are missing). The generator is not in the repository; regenerate once a Java generator exists in the
> build (ROADMAP DOC-1). The baselines are the source of truth.

Every table, column and relationship in the MySMS database — 466 tables across 21 module schemas. Generated from the Flyway migrations in `migrations/` (same folder).

Column legend: **PK** primary key · **FK** foreign key inside the module (`→ target`, on-delete rule; *tenant-composite* = the FK also carries `tenant_id`, so the link can never cross tenants) · **logical** reference into another module — no foreign key; validated through the owner's API and listed in `logical-references.md` · **UQ** unique · **NN** not null. Partial unique indexes (business keys that ignore soft-deleted rows) and checks are listed under each table. Standard columns (`id`, `tenant_id`, audit set, `row_version`) are described once under *Conventions* in the README and listed in every table.

## `master` — Master Data

Master Data: Global, tenant-independent master tables (geography, currency) shared by every module.

6 tables: [`city`](#mastercity), [`country`](#mastercountry), [`currency`](#mastercurrency), [`inbox_event`](#masterinbox_event), [`outbox_event`](#masteroutbox_event), [`state_province`](#masterstate_province)

<a id="mastercity"></a>
### master.city

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `country_id` | uuid | Y |  | FK → `master.country` · RESTRICT |  |
| 3 | `state_province_id` | uuid |  |  | FK → `master.state_province` · RESTRICT |  |
| 4 | `name` | varchar(?) | Y |  |  |  |
| 5 | `is_active` | bool | Y | TRUE |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `org.institution.city_id`
- ← `people.person_address.city_id`

<a id="mastercountry"></a>
### master.country

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `iso2` | bpchar(?) |  |  | UQ |  |
| 3 | `iso3` | bpchar(?) |  |  |  |  |
| 4 | `name` | varchar(?) | Y |  |  |  |
| 5 | `short_name` | varchar(?) |  |  |  |  |
| 6 | `phone_prefix` | varchar(?) |  |  |  |  |
| 7 | `sort_order` | int4 |  |  |  |  |
| 8 | `is_active` | bool | Y | TRUE |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Relationships**

- ← `master.city.country_id` — referenced by (1:N)
- ← `master.state_province.country_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `admission.application.country_id`
- ← `careers.job_opening.country_id`
- ← `lms.settings_primary_country.country_id`
- ← `org.institution.country_id`
- ← `people.person.nationality_country_id`
- ← `people.person_address.country_id`
- ← `people.public_profile.country_id`
- ← `platform.tenant.country_id`

<a id="mastercurrency"></a>
### master.currency

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `code` | bpchar(?) | Y |  | UQ |  |
| 3 | `name` | varchar(?) | Y |  |  |  |
| 4 | `symbol` | varchar(?) |  |  |  |  |
| 5 | `minor_units` | int2 | Y | 2 |  |  |
| 6 | `is_active` | bool | Y | TRUE |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `finance.fee_head.currency_id`
- ← `finance.invoice.currency_id`
- ← `finance.payment.currency_id`
- ← `finance.payment_allocation.currency_id`
- ← `finance.payment_gateway_account.currency_id`
- ← `finance.student_fee_due.currency_id`
- ← `lms.cohort.currency_id`
- ← `lms.course.currency_id`
- ← `lms.settings.default_currency_id`
- ← `org.institution.currency_id`
- ← `platform.saas_plan.currency_id`
- ← `platform.tenant.default_currency_id`
- ← `platform.tenant_subscription.currency_id`

<a id="masterinbox_event"></a>
### master.inbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `event_id` | uuid | Y |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  | PK |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `received_at` | timestamptz | Y | now() |  |  |
| 5 | `processed_at` | timestamptz |  |  |  |  |
| 6 | `processing_error` | text |  |  |  |  |

<a id="masteroutbox_event"></a>
### master.outbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | int8 |  |  | PK |  |
| 2 | `tenant_id` | uuid |  |  |  |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `aggregate_type` | varchar(?) | Y |  |  |  |
| 5 | `aggregate_id` | uuid | Y |  |  |  |
| 6 | `payload` | text | Y |  |  |  |
| 7 | `occurred_at` | timestamptz | Y | now() |  |  |
| 8 | `published_at` | timestamptz |  |  |  |  |
| 9 | `publish_error` | text |  |  |  | Why the relay set the row aside (not published) |

<a id="masterstate_province"></a>
### master.state_province

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `country_id` | uuid | Y |  | FK → `master.country` · RESTRICT |  |
| 3 | `code` | varchar(?) |  |  |  |  |
| 4 | `name` | varchar(?) | Y |  |  |  |
| 5 | `is_active` | bool | Y | TRUE |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_state_province_1` (`country_id`, `name`) WHERE deleted_at IS NULL
- unique index `uq_state_province_code` (`country_id`, `code`) WHERE code IS NOT NULL AND deleted_at IS NULL

**Relationships**

- ← `master.city.state_province_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `admission.application.state_province_id`
- ← `org.institution.state_province_id`
- ← `people.person_address.state_province_id`

## `platform` — Platform & Tenancy

Platform & Tenancy: SaaS tenancy, subscription plans, shared files, tags, notes, audit trail and global settings.

25 tables: [`audit_log`](#platformaudit_log), [`deletion_log`](#platformdeletion_log), [`entity_attachment`](#platformentity_attachment), [`entity_note`](#platformentity_note), [`file_object`](#platformfile_object), [`idempotency_key`](#platformidempotency_key), [`import_batch`](#platformimport_batch), [`import_job`](#platformimport_job), [`import_row_error`](#platformimport_row_error), [`inbox_event`](#platforminbox_event), [`operation`](#platformoperation), [`outbox_event`](#platformoutbox_event), [`reserved_keyword`](#platformreserved_keyword), [`saas_plan`](#platformsaas_plan), [`sales_enquiry`](#platformsales_enquiry), [`storage_quota`](#platformstorage_quota), [`system_setting`](#platformsystem_setting), [`tag`](#platformtag), [`tag_assignment`](#platformtag_assignment), [`tenant`](#platformtenant), [`tenant_domain`](#platformtenant_domain), [`tenant_subscription`](#platformtenant_subscription), [`theme`](#platformtheme), [`theme_token`](#platformtheme_token), [`upload_policy`](#platformupload_policy)

<a id="platformaudit_log"></a>
### platform.audit_log

*Partitioned by RANGE (occurred_at); monthly partitions plus a default partition.*

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid |  |  | FK → `platform.tenant` · RESTRICT |  |
| 3 | `log_name` | varchar(?) |  |  |  |  |
| 4 | `description` | text | Y |  |  |  |
| 5 | `event` | varchar(?) |  |  |  |  |
| 6 | `subject_type` | varchar(?) |  |  |  |  |
| 7 | `subject_id` | uuid |  |  |  |  |
| 8 | `causer_type` | varchar(?) |  |  |  |  |
| 9 | `causer_id` | uuid |  |  |  |  |
| 10 | `properties` | jsonb |  |  |  |  |
| 11 | `batch_id` | uuid |  |  |  |  |
| 12 | `ip_address` | inet |  |  |  |  |
| 13 | `occurred_at` | timestamptz | Y | now() | PK |  |

<a id="platformdeletion_log"></a>
### platform.deletion_log

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  | FK → `platform.tenant` · RESTRICT |  |
| 3 | `entity_type` | varchar(?) | Y |  |  |  |
| 4 | `entity_id` | uuid | Y |  |  |  |
| 5 | `cohort_id` | uuid |  |  | logical → `lms.cohort` (cross-module, no FK) |  |
| 6 | `deleted_at_ts` | timestamptz | Y | now() |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

<a id="platformentity_attachment"></a>
### platform.entity_attachment

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  | FK → `platform.tenant` · RESTRICT |  |
| 3 | `entity_type` | varchar(?) | Y |  |  |  |
| 4 | `entity_id` | uuid | Y |  |  |  |
| 5 | `file_id` | uuid | Y |  | FK → `platform.file_object` · CASCADE · tenant-composite |  |
| 6 | `sort_order` | int4 | Y | 0 |  |  |
| 7 | `purpose` | varchar(?) | Y | 'attachment' |  |  |
| 8 | `title` | varchar(?) |  |  |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_entity_attachment_1` (`entity_type`, `entity_id`, `file_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_entity_attachment_purpose`: purpose IN ('attachment', 'resource', 'grading_key', 'review_feedback', 'submission', 'inline_media')

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `lms.plagiarism_report.entity_attachment_id`

<a id="platformentity_note"></a>
### platform.entity_note

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  | FK → `platform.tenant` · RESTRICT |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `entity_type` | varchar(?) | Y |  |  |  |
| 5 | `entity_id` | uuid | Y |  |  |  |
| 6 | `note_text` | text | Y |  |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

<a id="platformfile_object"></a>
### platform.file_object

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  | FK → `platform.tenant` · RESTRICT |  |
| 3 | `storage_disk` | varchar(?) | Y |  |  |  |
| 4 | `storage_key` | text | Y |  |  |  |
| 5 | `storage_generation` | int8 |  |  |  |  |
| 6 | `file_name` | varchar(?) | Y |  |  |  |
| 7 | `original_name` | varchar(?) |  |  |  |  |
| 8 | `mime_type` | varchar(?) |  |  |  |  |
| 9 | `size_bytes` | int8 | Y |  |  |  |
| 10 | `checksum_sha256` | bpchar(?) |  |  |  |  |
| 11 | `owner_entity_type` | varchar(?) |  |  |  |  |
| 12 | `owner_entity_id` | uuid |  |  |  |  |
| 13 | `collection_name` | varchar(?) |  |  |  |  |
| 14 | `sort_order` | int4 |  |  |  |  |
| 15 | `file_kind` | varchar(?) |  |  |  |  |
| 16 | `is_shared` | bool | Y | FALSE |  |  |
| 17 | `processing_status` | varchar(?) | Y | 'none' |  |  |
| 18 | `processing_attempts` | int4 | Y | 0 |  |  |
| 19 | `processing_error` | text |  |  |  |  |
| 20 | `created_at` | timestamptz | Y | now() |  |  |
| 21 | `created_by` | uuid |  |  |  |  |
| 22 | `updated_at` | timestamptz | Y | now() |  |  |
| 23 | `updated_by` | uuid |  |  |  |  |
| 24 | `deleted_at` | timestamptz |  |  |  |  |
| 25 | `row_version` | int4 | Y | 0 |  |  |
| 26 | `status` | varchar(?) | Y | 'pending' |  |  |
| 27 | `scanned_at` | timestamptz |  |  |  |  |
| 28 | `uploaded_by_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |

**Uniqueness**

- UNIQUE (`storage_disk`, `storage_key`)
- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_file_object_size_bytes_rule`: size_bytes >= 0
- `ck_file_object_file_kind`: file_kind IN ('document', 'presentation', 'spreadsheet', 'pdf', 'image', 'audio', 'video', 'archive', 'package', 'ebook', 'other')
- `ck_file_object_processing_status`: processing_status IN ('none', 'pending', 'in_progress', 'succeeded', 'failed')
- `ck_file_object_status`: status IN ('pending', 'available', 'quarantined', 'deleted')

**Relationships**

- ← `platform.entity_attachment.file_id` — referenced by (1:N)
- ← `platform.theme.logo_file_id` — referenced by (1:N)
- ← `platform.theme.thumbnail_alt_file_id` — referenced by (1:N)
- ← `platform.theme.thumbnail_file_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `academics.program.image_file_id`
- ← `admission.application.photo_file_id`
- ← `admission.application_guardian.photo_file_id`
- ← `assessment.assignment.attachment_file_id`
- ← `assessment.homework.attachment_file_id`
- ← `careers.job_application.resume_file_id`
- ← `careers.job_opening.company_logo_file_id`
- ← `comms.class_page.cover_image_file_id`
- ← `comms.class_page_attachment.file_id`
- ← `comms.event.image_file_id`
- ← `comms.event_gallery_item.file_id`
- ← `comms.feedback_message.attachment_file_id`
- ← `comms.magazine.cover_image_file_id`
- ← `comms.magazine.document_file_id`
- ← `comms.notice.attachment_file_id`
- ← `comms.notice_background.image_file_id`
- ← `comms.post_comment.attachment_file_id`
- ← `finance.invoice.document_file_id`
- ← `frontoffice.postal_record.attachment_file_id`
- ← `library.media.cover_image_file_id`
- ← `lms.annotation.file_id`
- ← `lms.annotation.media_file_id`
- ← `lms.assignment_submission.attachment_file_id`
- ← `lms.badge.image_file_id`
- ← `lms.catalog_banner.image_file_id`
- ← `lms.chapter.scorm_package_file_id`
- ← `lms.cohort.intro_video_file_id`
- ← `lms.cohort.meta_image_file_id`
- ← `lms.content_item.file_id`
- ← `lms.content_view_session.file_id`
- ← `lms.course.image_file_id`
- ← `lms.course_author_profile.photo_file_id`
- ← `lms.course_preview.file_id`
- ← `lms.course_testimonial.photo_file_id`
- ← `lms.drive_item.file_id`
- ← `lms.ebook.file_id`
- ← `lms.ebook.source_file_id`
- ← `lms.import_export_job.package_file_id`
- ← `lms.plagiarism_report.file_id`
- ← `lms.question_hotspot.image_file_id`
- ← `lms.related_material_rank.file_id`
- ← `lms.settings.meta_image_file_id`
- ← `org.institution.logo_file_id`
- ← `org.institution.signature_file_id`
- ← `payroll.payroll_payment.attachment_file_id`
- ← `payroll.payslip.document_file_id`
- ← `people.consent_record.evidence_file_id`
- ← `people.person.photo_file_id`
- ← `people.person_document.file_id`
- ← `people.public_profile.cover_image_file_id`
- ← `sis.discipline_incident.evidence_file_id`

<a id="platformidempotency_key"></a>
### platform.idempotency_key

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `idempotency_key` | uuid | Y |  |  |  |
| 4 | `user_id` | uuid |  |  |  |  |
| 5 | `operation_name` | varchar(?) | Y |  |  |  |
| 6 | `request_hash` | varchar(?) |  |  |  |  |
| 7 | `response_status` | int2 |  |  |  |  |
| 8 | `response_body` | text |  |  |  |  |
| 9 | `locked_at` | timestamptz |  |  |  |  |
| 10 | `completed_at` | timestamptz |  |  |  |  |
| 11 | `expires_at` | timestamptz | Y |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `idempotency_key`)

<a id="platformimport_batch"></a>
### platform.import_batch

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK | Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback). |
| 2 | `tenant_id` | uuid | Y |  |  | Owning tenant (customer organisation). Row-level security key. |
| 3 | `import_job_id` | uuid | Y |  | FK → `platform.import_job` · RESTRICT · tenant-composite | The import. |
| 4 | `batch_number` | int4 | Y |  |  | 1-based position of the batch in the import. |
| 5 | `module_code` | varchar(40) | Y |  |  | The module that answered (the owner of the entity type). |
| 6 | `created_count` | int4 | Y |  |  | Rows of the batch the module created. |
| 7 | `error_count` | int4 | Y |  |  | Row errors of the batch the module reported. |
| 8 | `created_at` | timestamptz | Y | now() |  | Row creation time (when the answer arrived). |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`import_job_id`, `batch_number`) — a repeated answer to a batch is ignored

**Checks**

- `ck_import_batch_counts`: batch_number >= 1 AND created_count >= 0 AND error_count >= 0

<a id="platformimport_job"></a>
### platform.import_job

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK | Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback). |
| 2 | `tenant_id` | uuid | Y |  |  | Owning tenant (customer organisation). Row-level security key. |
| 3 | `operation_id` | uuid | Y |  | FK → `platform.operation` · RESTRICT · tenant-composite | The platform operation the import answers (one import per operation). |
| 4 | `entity_type` | varchar(40) | Y |  |  | What is imported. |
| 5 | `institution_id` | uuid |  |  | → `org.institution` (logical) | Target institution; NULL for tenant-level records (guardians, tenant holidays). |
| 6 | `file_id` | uuid | Y |  | FK → `platform.file_object` · RESTRICT · tenant-composite | The uploaded CSV file. |
| 7 | `dry_run` | bool | Y | false |  | Validated only: nothing was sent to the owning module. |
| 8 | `row_count` | int4 | Y | 0 |  | Data rows in the file. |
| 9 | `batch_count` | int4 | Y | 0 |  | Batches of valid rows sent to the owning module. |
| 10 | `applied_batch_count` | int4 | Y | 0 |  | Batches the owning module has answered; the import is finished when it reaches batch_count. |
| 11 | `created_count` | int4 | Y | 0 |  | Rows the owning module created. |
| 12 | `error_count` | int4 | Y | 0 |  | Row errors recorded (platform.import_row_error). |
| 13 | `created_at` | timestamptz | Y | now() |  |  |
| 14 | `created_by` | uuid |  |  |  |  |
| 15 | `updated_at` | timestamptz | Y | now() |  |  |
| 16 | `updated_by` | uuid |  |  |  |  |
| 17 | `deleted_at` | timestamptz |  |  |  |  |
| 18 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- UNIQUE (`operation_id`)

**Checks**

- `ck_import_job_entity_type`: entity_type IN ('students', 'guardians', 'staff', 'library_media', 'holidays', 'promotions')
- `ck_import_job_counts`: row_count >= 0 AND batch_count >= 0 AND applied_batch_count BETWEEN 0 AND batch_count AND created_count BETWEEN 0 AND row_count AND error_count >= 0

<a id="platformimport_row_error"></a>
### platform.import_row_error

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK | Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback). |
| 2 | `tenant_id` | uuid | Y |  |  | Owning tenant (customer organisation). Row-level security key. |
| 3 | `import_job_id` | uuid | Y |  | FK → `platform.import_job` · RESTRICT · tenant-composite | The import. |
| 4 | `batch_number` | int4 |  |  |  | The batch whose module refused the row; NULL when the platform's template check did. |
| 5 | `row_number` | int4 | Y |  |  | Record number of the row in the file (the header row is 1, as a spreadsheet numbers rows). |
| 6 | `field` | varchar(80) |  |  |  | The column at fault; NULL for the row as a whole. |
| 7 | `message` | varchar(500) | Y |  |  | Why the row was refused (names fields, never cell values). |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_import_row_error_row`: row_number >= 1 AND (batch_number IS NULL OR batch_number >= 1)

<a id="platforminbox_event"></a>
### platform.inbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `event_id` | uuid | Y |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  | PK |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `received_at` | timestamptz | Y | now() |  |  |
| 5 | `processed_at` | timestamptz |  |  |  |  |
| 6 | `processing_error` | text |  |  |  |  |

<a id="platformoperation"></a>
### platform.operation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `operation_name` | varchar(?) | Y |  |  |  |
| 4 | `resource_type` | varchar(?) |  |  |  |  |
| 5 | `resource_id` | uuid |  |  |  |  |
| 6 | `status` | varchar(?) | Y | 'running' |  |  |
| 7 | `progress_percent` | int2 |  |  |  |  |
| 8 | `error_code` | varchar(?) |  |  |  |  |
| 9 | `error_message` | varchar(?) |  |  |  |  |
| 10 | `result_entity_type` | varchar(?) |  |  |  |  |
| 11 | `result_entity_id` | uuid |  |  |  |  |
| 12 | `started_by_user_id` | uuid |  |  |  |  |
| 13 | `started_at` | timestamptz | Y | now() |  |  |
| 14 | `finished_at` | timestamptz |  |  |  |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_operation_status`: status IN ('running', 'succeeded', 'failed', 'cancelled')

<a id="platformoutbox_event"></a>
### platform.outbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | int8 |  |  | PK |  |
| 2 | `tenant_id` | uuid |  |  |  |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `aggregate_type` | varchar(?) | Y |  |  |  |
| 5 | `aggregate_id` | uuid | Y |  |  |  |
| 6 | `payload` | text | Y |  |  |  |
| 7 | `occurred_at` | timestamptz | Y | now() |  |  |
| 8 | `published_at` | timestamptz |  |  |  |  |
| 9 | `publish_error` | text |  |  |  | Why the relay set the row aside (not published) |

<a id="platformreserved_keyword"></a>
### platform.reserved_keyword

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `keyword` | varchar(?) | Y |  | UQ |  |
| 3 | `created_at` | timestamptz | Y | now() |  |  |
| 4 | `created_by` | uuid |  |  |  |  |
| 5 | `updated_at` | timestamptz | Y | now() |  |  |
| 6 | `updated_by` | uuid |  |  |  |  |
| 7 | `deleted_at` | timestamptz |  |  |  |  |
| 8 | `row_version` | int4 | Y | 0 |  |  |

<a id="platformsaas_plan"></a>
### platform.saas_plan

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `code` | varchar(?) | Y |  | UQ |  |
| 3 | `display_name` | varchar(?) | Y |  |  |  |
| 4 | `billing_cycle_months` | int2 | Y |  |  |  |
| 5 | `price` | numeric(?,?) | Y | 0 |  |  |
| 6 | `currency_id` | uuid |  |  | logical → `master.currency` (cross-module, no FK) |  |
| 7 | `sort_order` | int4 | Y | 1 |  |  |
| 8 | `is_active` | bool | Y | TRUE |  |  |
| 9 | `max_members` | int4 |  |  |  |  |
| 10 | `max_events` | int4 |  |  |  |  |
| 11 | `max_folders` | int4 |  |  |  |  |
| 12 | `max_files` | int4 |  |  |  |  |
| 13 | `max_videos` | int4 |  |  |  |  |
| 14 | `max_audios` | int4 |  |  |  |  |
| 15 | `max_bulletins` | int4 |  |  |  |  |
| 16 | `max_groups` | int4 |  |  |  |  |
| 17 | `created_at` | timestamptz | Y | now() |  |  |
| 18 | `created_by` | uuid |  |  |  |  |
| 19 | `updated_at` | timestamptz | Y | now() |  |  |
| 20 | `updated_by` | uuid |  |  |  |  |
| 21 | `deleted_at` | timestamptz |  |  |  |  |
| 22 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_saas_plan_billing_cycle_months_rule`: billing_cycle_months > 0

**Relationships**

- ← `platform.tenant_subscription.plan_id` — referenced by (1:N)

<a id="platformsales_enquiry"></a>
### platform.sales_enquiry

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `contact_name` | varchar(?) | Y |  |  |  |
| 3 | `email` | varchar(?) | Y |  |  |  |
| 4 | `phone` | varchar(?) | Y |  |  |  |
| 5 | `organisation_name` | varchar(?) | Y |  |  |  |
| 6 | `designation` | varchar(?) | Y |  |  |  |
| 7 | `channel` | varchar(?) |  |  |  |  |
| 8 | `message` | text |  |  |  |  |
| 9 | `status` | varchar(?) | Y | 'new' |  |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_sales_enquiry_status`: status IN ('new', 'contacted', 'qualified', 'converted', 'closed')

<a id="platformstorage_quota"></a>
### platform.storage_quota

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  | FK → `platform.tenant` · RESTRICT |  |
| 3 | `user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 4 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 5 | `quota_bytes` | int8 | Y |  |  |  |
| 6 | `used_bytes` | int8 | Y | 0 |  |  |
| 7 | `is_active` | bool | Y | TRUE |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_storage_quota_1`: num_nonnulls(user_id, institution_id) = 1

<a id="platformsystem_setting"></a>
### platform.system_setting

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `setting_key` | varchar(?) | Y |  | UQ |  |
| 3 | `name` | varchar(?) | Y |  |  |  |
| 4 | `description` | text |  |  |  |  |
| 5 | `value` | text |  |  |  |  |
| 6 | `input_type` | varchar(?) | Y |  |  |  |
| 7 | `is_active` | bool | Y | TRUE |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

<a id="platformtag"></a>
### platform.tag

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  | FK → `platform.tenant` · RESTRICT |  |
| 3 | `tag_type` | varchar(?) | Y | 'general' |  |  |
| 4 | `name` | varchar(?) | Y |  |  |  |
| 5 | `slug` | varchar(?) | Y |  |  |  |
| 6 | `sort_order` | int4 |  |  |  |  |
| 7 | `code` | varchar(?) |  |  |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_tag_1` (`tenant_id`, `tag_type`, `slug`) WHERE deleted_at IS NULL
- unique index `uq_tag_code` (`tenant_id`, `tag_type`, `code`) WHERE code IS NOT NULL AND deleted_at IS NULL

**Relationships**

- ← `platform.tag_assignment.tag_id` — referenced by (1:N)

<a id="platformtag_assignment"></a>
### platform.tag_assignment

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  | FK → `platform.tenant` · RESTRICT |  |
| 3 | `tag_id` | uuid | Y |  | FK → `platform.tag` · CASCADE · tenant-composite |  |
| 4 | `entity_type` | varchar(?) | Y |  |  |  |
| 5 | `entity_id` | uuid | Y |  |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_tag_assignment_1` (`tag_id`, `entity_type`, `entity_id`) WHERE deleted_at IS NULL

<a id="platformtenant"></a>
### platform.tenant

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `code` | varchar(?) | Y |  | UQ |  |
| 3 | `name` | varchar(?) | Y |  |  |  |
| 4 | `legal_name` | varchar(?) |  |  |  |  |
| 5 | `status` | varchar(?) | Y | 'active' |  |  |
| 6 | `primary_email` | varchar(?) |  |  |  |  |
| 7 | `primary_phone` | varchar(?) |  |  |  |  |
| 8 | `country_id` | uuid |  |  | logical → `master.country` (cross-module, no FK) |  |
| 9 | `default_currency_id` | uuid |  |  | logical → `master.currency` (cross-module, no FK) |  |
| 10 | `timezone` | varchar(?) | Y | 'UTC' |  |  |
| 11 | `locale` | varchar(?) | Y | 'en' |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_tenant_status`: status IN ('active', 'suspended', 'closed')

<a id="platformtenant_domain"></a>
### platform.tenant_domain

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  | UQ |  |
| 3 | `host` | varchar(?) | Y |  | UQ |  |
| 4 | `is_primary` | bool | Y | FALSE |  |  |
| 5 | `is_verified` | bool | Y | FALSE |  |  |
| 6 | `verified_at` | timestamptz |  |  |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_tenant_domain_primary` (`tenant_id`) WHERE is_primary AND deleted_at IS NULL

<a id="platformtenant_subscription"></a>
### platform.tenant_subscription

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  | FK → `platform.tenant` · RESTRICT |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `plan_id` | uuid |  |  | FK → `platform.saas_plan` · RESTRICT |  |
| 5 | `requested_by_user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 6 | `status` | varchar(?) | Y | 'pending' |  |  |
| 7 | `start_date` | date |  |  |  |  |
| 8 | `end_date` | date |  |  |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |
| 15 | `max_members` | int4 |  |  |  |  |
| 16 | `max_events` | int4 |  |  |  |  |
| 17 | `max_folders` | int4 |  |  |  |  |
| 18 | `max_files` | int4 |  |  |  |  |
| 19 | `max_videos` | int4 |  |  |  |  |
| 20 | `max_audios` | int4 |  |  |  |  |
| 21 | `max_bulletins` | int4 |  |  |  |  |
| 22 | `max_groups` | int4 |  |  |  |  |
| 23 | `price` | numeric(?,?) |  |  |  |  |
| 24 | `currency_id` | uuid |  |  | logical → `master.currency` (cross-module, no FK) |  |
| 25 | `billing_cycle_months` | int4 |  |  |  |  |
| 26 | `gateway_name` | varchar(?) |  |  |  |  |
| 27 | `gateway_txn_ref` | varchar(?) |  |  |  |  |
| 28 | `amount_paid` | numeric(?,?) |  |  |  |  |
| 29 | `payment_mode` | varchar(?) |  |  |  |  |
| 30 | `gateway_status` | varchar(?) |  |  |  |  |
| 31 | `gateway_error` | varchar(?) |  |  |  |  |
| 32 | `paid_at` | timestamptz |  |  |  |  |
| 33 | `payer_name` | varchar(?) |  |  |  |  |
| 34 | `payer_email` | varchar(?) |  |  |  |  |
| 35 | `payer_phone` | varchar(?) |  |  |  |  |
| 36 | `gateway_raw_response` | text |  |  |  |  |

**Checks**

- `ck_tenant_subscription_status`: status IN ('pending', 'active', 'cancelled', 'expired')

<a id="platformtheme"></a>
### platform.theme

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  | FK → `platform.tenant` · RESTRICT |  |
| 3 | `name` | varchar(?) | Y |  |  |  |
| 4 | `description` | text |  |  |  |  |
| 5 | `thumbnail_file_id` | uuid |  |  | FK → `platform.file_object` · SET NULL · tenant-composite |  |
| 6 | `thumbnail_alt_file_id` | uuid |  |  | FK → `platform.file_object` · SET NULL · tenant-composite |  |
| 7 | `logo_file_id` | uuid |  |  | FK → `platform.file_object` · SET NULL · tenant-composite | Logo of the tenant's branding |
| 8 | `is_default` | bool | Y | false |  | The tenant's own theme (public branding) |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- UNIQUE (`tenant_id`) WHERE is_default AND deleted_at IS NULL — one default theme per tenant

**Relationships**

- ← `platform.theme_token.theme_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `org.institution.theme_id`

<a id="platformtheme_token"></a>
### platform.theme_token

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `theme_id` | uuid | Y |  | FK → `platform.theme` · CASCADE · tenant-composite |  |
| 4 | `token_key` | varchar(?) | Y |  |  |  |
| 5 | `value` | varchar(?) | Y |  |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_theme_token_1` (`theme_id`, `token_key`) WHERE deleted_at IS NULL

<a id="platformupload_policy"></a>
### platform.upload_policy

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  | FK → `platform.tenant` · RESTRICT |  |
| 3 | `role_id` | uuid |  |  | logical → `iam.role` (cross-module, no FK) |  |
| 4 | `module_code` | varchar(?) | Y |  |  |  |
| 5 | `operation_code` | varchar(?) |  |  |  |  |
| 6 | `max_size_bytes` | int8 |  |  |  |  |
| 7 | `allowed_extensions` | varchar(?) |  |  |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `ix_upload_policy_1` (`tenant_id`, `COALESCE(role_id, CAST('00000000-0000-0000-0000-000000000000' AS uuid))`, `module_code`, `COALESCE(operation_code, '')`) WHERE deleted_at IS NULL

## `iam` — Identity & Access

Identity & Access: Login accounts, roles, permissions, scoped role grants, tokens and devices. A person (people.person) may own one account per tenant.

21 tables: [`auth_token`](#iamauth_token), [`idempotency_key`](#iamidempotency_key), [`inbox_event`](#iaminbox_event), [`legacy_credential`](#iamlegacy_credential), [`member_record`](#iammember_record), [`mfa_credential`](#iammfa_credential), [`operation`](#iamoperation), [`outbox_event`](#iamoutbox_event), [`permission`](#iampermission), [`role`](#iamrole), [`role_permission`](#iamrole_permission), [`setting_definition`](#iamsetting_definition), [`setting_value`](#iamsetting_value), [`tenant_directory`](#iamtenant_directory), [`user_account`](#iamuser_account), [`user_department_access`](#iamuser_department_access), [`user_device`](#iamuser_device), [`user_permission`](#iamuser_permission), [`user_preference`](#iamuser_preference), [`user_role`](#iamuser_role), [`user_session`](#iamuser_session)

<a id="iamauth_token"></a>
### iam.auth_token

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `user_id` | uuid | Y |  | FK → `iam.user_account` · CASCADE · tenant-composite |  |
| 4 | `token_type` | varchar(?) | Y |  |  |  |
| 5 | `name` | varchar(?) |  |  |  |  |
| 6 | `token_hash` | bpchar(?) | Y |  | UQ |  |
| 7 | `sent_to` | varchar(?) |  |  |  |  |
| 8 | `ip_address` | inet |  |  |  |  |
| 9 | `expires_at` | timestamptz |  |  |  |  |
| 10 | `last_used_at` | timestamptz |  |  |  |  |
| 11 | `revoked_at` | timestamptz |  |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |
| 18 | `scopes` | varchar(?)[] |  |  |  |  |

**Checks**

- `ck_auth_token_token_type`: token_type IN ('api', 'remember_me', 'one_time_login', 'email_verification', 'mobile_verification', 'password_reset', 'refresh')

<a id="iamidempotency_key"></a>
### iam.idempotency_key

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `idempotency_key` | uuid | Y |  |  |  |
| 4 | `user_id` | uuid |  |  |  |  |
| 5 | `operation_name` | varchar(?) | Y |  |  |  |
| 6 | `request_hash` | varchar(?) |  |  |  |  |
| 7 | `response_status` | int2 |  |  |  |  |
| 8 | `response_body` | text |  |  |  |  |
| 9 | `locked_at` | timestamptz |  |  |  |  |
| 10 | `completed_at` | timestamptz |  |  |  |  |
| 11 | `expires_at` | timestamptz | Y |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `idempotency_key`)

<a id="iaminbox_event"></a>
### iam.inbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `event_id` | uuid | Y |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  | PK |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `received_at` | timestamptz | Y | now() |  |  |
| 5 | `processed_at` | timestamptz |  |  |  |  |
| 6 | `processing_error` | text |  |  |  |  |

<a id="iamlegacy_credential"></a>
### iam.legacy_credential

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `user_id` | uuid | Y |  | FK → `iam.user_account` · CASCADE · tenant-composite · UQ |  |
| 4 | `algorithm` | varchar(?) | Y |  |  |  |
| 5 | `password_hash` | text | Y |  |  |  |
| 6 | `salt` | varchar(?) |  |  |  |  |
| 7 | `migrated_at` | timestamptz |  |  |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_legacy_credential_algorithm`: algorithm IN ('md5', 'sha1', 'sha256', 'bcrypt', 'phpass')

<a id="iammember_record"></a>
### iam.member_record

Staff and student records of login accounts and persons, kept from `hr.staff` and `sis.student` events so `iam.GetMe` can name them (`Me.staffIds`, `Me.studentIds`) from its own schema.

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `tenant_id` | uuid | Y |  | PK | Owning tenant. Row-level security key. |
| 2 | `record_id` | uuid | Y |  | PK | `hr.staff.id` or `sis.student.id` (kept from their events) |
| 3 | `record_type` | varchar(20) | Y |  |  | staff or student |
| 4 | `user_id` | uuid |  |  |  | Login account of the record |
| 5 | `person_id` | uuid |  |  |  | Person of the record |
| 6 | `institution_id` | uuid |  |  |  | Institution of the record |
| 7 | `is_deleted` | bool | Y | false |  | Deleted in its module; kept so a late event cannot restore it |
| 8 | `source_version` | int4 | Y |  |  | Source row version this entry reflects; older events are ignored |
| 9 | `synced_at` | timestamptz | Y |  |  | When an event last changed the row |

**Checks**

- `ck_member_record_record_type`: record_type IN ('staff', 'student')
- `ck_member_record_source_version`: source_version >= 1

<a id="iammfa_credential"></a>
### iam.mfa_credential

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `user_id` | uuid | Y |  | FK → `iam.user_account` · CASCADE · tenant-composite |  |
| 4 | `credential_type` | varchar(?) | Y |  |  |  |
| 5 | `label` | varchar(?) |  |  |  |  |
| 6 | `secret_enc` | text | Y |  |  |  |
| 7 | `verified_at` | timestamptz |  |  |  |  |
| 8 | `last_used_at` | timestamptz |  |  |  |  |
| 9 | `is_active` | bool | Y | TRUE |  |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_mfa_credential_type`: credential_type IN ('totp', 'sms', 'email', 'recovery_codes')

<a id="iamoperation"></a>
### iam.operation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `operation_name` | varchar(?) | Y |  |  |  |
| 4 | `resource_type` | varchar(?) |  |  |  |  |
| 5 | `resource_id` | uuid |  |  |  |  |
| 6 | `status` | varchar(?) | Y | 'running' |  |  |
| 7 | `progress_percent` | int2 |  |  |  |  |
| 8 | `error_code` | varchar(?) |  |  |  |  |
| 9 | `error_message` | varchar(?) |  |  |  |  |
| 10 | `result_entity_type` | varchar(?) |  |  |  |  |
| 11 | `result_entity_id` | uuid |  |  |  |  |
| 12 | `started_by_user_id` | uuid |  |  |  |  |
| 13 | `started_at` | timestamptz | Y | now() |  |  |
| 14 | `finished_at` | timestamptz |  |  |  |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_iam_operation_status`: status IN ('running', 'succeeded', 'failed', 'cancelled')

<a id="iamoutbox_event"></a>
### iam.outbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | int8 |  |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `aggregate_type` | varchar(?) | Y |  |  |  |
| 5 | `aggregate_id` | uuid | Y |  |  |  |
| 6 | `payload` | text | Y |  |  |  |
| 7 | `occurred_at` | timestamptz | Y | now() |  |  |
| 8 | `published_at` | timestamptz |  |  |  |  |
| 9 | `publish_error` | text |  |  |  | Why the relay set the row aside (not published) |

<a id="iampermission"></a>
### iam.permission

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `code` | varchar(?) | Y |  | UQ |  |
| 3 | `display_name` | varchar(?) |  |  |  |  |
| 4 | `description` | text |  |  |  |  |
| 5 | `module_code` | varchar(?) |  |  |  |  |
| 6 | `is_navigation` | bool | Y | FALSE |  |  |
| 7 | `is_modifiable` | bool | Y | TRUE |  |  |
| 8 | `applies_to_all_roles` | bool | Y | FALSE |  |  |
| 9 | `is_object_level` | bool | Y | FALSE |  |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |

**Relationships**

- ← `iam.role_permission.permission_id` — referenced by (1:N)
- ← `iam.user_permission.permission_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `comms.notification_rule.permission_id`

<a id="iamrole"></a>
### iam.role

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `code` | varchar(?) | Y |  |  |  |
| 4 | `display_name` | varchar(?) |  |  |  |  |
| 5 | `description` | text |  |  |  |  |
| 6 | `is_system` | bool | Y | FALSE |  |  |
| 7 | `is_active` | bool | Y | TRUE |  |  |
| 8 | `authority_level` | int4 |  |  |  |  |
| 9 | `parent_role_id` | uuid |  |  | FK → `iam.role` · SET NULL · tenant-composite |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_role_1` (`tenant_id`, `code`) WHERE deleted_at IS NULL

**Relationships**

- ← `iam.role.parent_role_id` — referenced by (1:N)
- ← `iam.role_permission.role_id` — referenced by (1:N)
- ← `iam.user_account.last_login_role_id` — referenced by (1:N)
- ← `iam.user_role.role_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `comms.notice_audience.role_id`
- ← `comms.notification_preference_set.role_id`
- ← `comms.notification_rule.role_id`
- ← `lms.cohort_teacher.role_id`
- ← `lms.external_tool_role.role_id`
- ← `platform.upload_policy.role_id`

<a id="iamrole_permission"></a>
### iam.role_permission

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `role_id` | uuid | Y |  | FK → `iam.role` · CASCADE · tenant-composite |  |
| 4 | `permission_id` | uuid | Y |  | FK → `iam.permission` · CASCADE |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_role_permission_1` (`role_id`, `permission_id`) WHERE deleted_at IS NULL

<a id="iamsetting_definition"></a>
### iam.setting_definition

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `setting_key` | varchar(?) | Y |  | UQ |  |
| 3 | `category` | varchar(?) | Y |  |  |  |
| 4 | `value_type` | varchar(?) | Y |  |  |  |
| 5 | `allowed_values` | varchar(?)[] |  |  |  |  |
| 6 | `min_value` | int4 |  |  |  |  |
| 7 | `max_value` | int4 |  |  |  |  |
| 8 | `default_value` | varchar(?) |  |  |  |  |
| 9 | `allowed_scopes` | varchar(?)[] | Y |  |  |  |
| 10 | `user_editable` | bool | Y | TRUE |  |  |
| 11 | `description` | text |  |  |  |  |
| 12 | `sort_order` | int4 | Y | 0 |  |  |
| 13 | `created_at` | timestamptz | Y | now() |  |  |
| 14 | `created_by` | uuid |  |  |  |  |
| 15 | `updated_at` | timestamptz | Y | now() |  |  |
| 16 | `updated_by` | uuid |  |  |  |  |
| 17 | `deleted_at` | timestamptz |  |  |  |  |
| 18 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_setting_definition_value_type`: value_type IN ('string', 'boolean', 'integer', 'choice')
- `ck_setting_definition_choice`: (value_type = 'choice') = (allowed_values IS NOT NULL)
- `ck_setting_definition_range`: min_value IS NULL OR max_value IS NULL OR min_value <= max_value
- `ck_setting_definition_scopes`: allowed_scopes <@ CAST(ARRAY['tenant', 'institution', 'department', 'class_section', 'user'] AS varchar(20)[]) AND cardinality(allowed_scopes) > 0

**Relationships**

- ← `iam.setting_value.setting_key` — referenced by (1:N)

<a id="iamsetting_value"></a>
### iam.setting_value

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `scope_type` | varchar(?) | Y |  |  |  |
| 4 | `scope_id` | uuid |  |  |  |  |
| 5 | `setting_key` | varchar(?) | Y |  | FK → `iam.setting_definition` · RESTRICT |  |
| 6 | `value` | varchar(?) | Y |  |  |  |
| 7 | `is_locked` | bool | Y | FALSE |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_setting_value_scope_key` (`tenant_id`, `scope_type`, `scope_id`, `setting_key`) WHERE deleted_at IS NULL

**Checks**

- `ck_setting_value_scope_type`: scope_type IN ('tenant', 'institution', 'department', 'class_section', 'user')
- `ck_setting_value_scope_id`: (scope_type = 'tenant') = (scope_id IS NULL)
- `ck_setting_value_user_unlocked`: NOT(scope_type = 'user' AND is_locked)

<a id="iamtenant_directory"></a>
### iam.tenant_directory

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `tenant_id` | uuid | Y |  | PK |  |
| 2 | `tenant_code` | varchar(?) | Y |  | UQ |  |
| 3 | `status` | varchar(?) | Y | 'active' |  |  |
| 4 | `synced_at` | timestamptz | Y | now() |  |  |
| 5 | `source_version` | int4 | Y | 0 |  | platform.tenant row version last applied; older events are ignored |

**Checks**

- `ck_tenant_directory_status`: status IN ('active', 'suspended', 'closed')

<a id="iamuser_account"></a>
### iam.user_account

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `person_id` | uuid |  |  | logical → `people.person` (cross-module, no FK) |  |
| 4 | `username` | varchar(?) |  |  |  |  |
| 5 | `email` | varchar(?) |  |  |  |  |
| 6 | `mobile_no` | varchar(?) |  |  |  |  |
| 7 | `password_hash` | text |  |  |  |  |
| 8 | `status` | varchar(?) | Y | 'active' |  |  |
| 9 | `email_verified_at` | timestamptz |  |  |  |  |
| 10 | `mobile_verified_at` | timestamptz |  |  |  |  |
| 11 | `must_reset_password` | bool | Y | FALSE |  |  |
| 12 | `referred_by_user_id` | uuid |  |  | FK → `iam.user_account` · SET NULL · tenant-composite |  |
| 13 | `terms_accepted_at` | timestamptz |  |  |  |  |
| 14 | `locale` | varchar(?) |  |  |  |  |
| 15 | `timezone` | varchar(?) |  |  |  |  |
| 16 | `last_login_at` | timestamptz |  |  |  |  |
| 17 | `account_expires_at` | timestamptz |  |  |  |  |
| 18 | `failed_login_count` | int4 | Y | 0 |  |  |
| 19 | `last_login_role_id` | uuid |  |  | FK → `iam.role` · SET NULL · tenant-composite |  |
| 20 | `registered_at` | timestamptz |  |  |  |  |
| 21 | `joined_on` | date |  |  |  |  |
| 22 | `external_ref` | varchar(?) |  |  |  |  |
| 23 | `signature` | text |  |  |  |  |
| 24 | `use_signature` | bool | Y | FALSE |  |  |
| 25 | `created_at` | timestamptz | Y | now() |  |  |
| 26 | `created_by` | uuid |  |  |  |  |
| 27 | `updated_at` | timestamptz | Y | now() |  |  |
| 28 | `updated_by` | uuid |  |  |  |  |
| 29 | `deleted_at` | timestamptz |  |  |  |  |
| 30 | `row_version` | int4 | Y | 0 |  |  |
| 31 | `user_type` | varchar(?) |  |  |  |  |
| 32 | `locked_until` | timestamptz |  |  |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `ix_user_account_1` (`tenant_id`, `lower(username)`) WHERE username IS NOT NULL AND deleted_at IS NULL
- unique index `ix_user_account_2` (`tenant_id`, `person_id`) WHERE person_id IS NOT NULL AND deleted_at IS NULL
- unique index `ix_user_account_3` (`tenant_id`, `lower(email)`) WHERE email IS NOT NULL AND deleted_at IS NULL
- unique index `ix_user_account_4` (`tenant_id`, `mobile_no`) WHERE mobile_no IS NOT NULL

**Checks**

- `ck_user_account_status`: status IN ('active', 'inactive', 'locked', 'exited')
- `ck_user_account_1`: username IS NOT NULL OR email IS NOT NULL OR mobile_no IS NOT NULL
- `ck_user_account_user_type`: user_type IN ('admin', 'teacher', 'student', 'guardian', 'staff', 'librarian', 'accountant', 'alumni')

**Relationships**

- ← `iam.auth_token.user_id` — referenced by (1:N)
- ← `iam.legacy_credential.user_id` — referenced by (1:1)
- ← `iam.mfa_credential.user_id` — referenced by (1:N)
- ← `iam.user_account.referred_by_user_id` — referenced by (1:N)
- ← `iam.user_department_access.user_id` — referenced by (1:N)
- ← `iam.user_device.user_id` — referenced by (1:N)
- ← `iam.user_permission.user_id` — referenced by (1:N)
- ← `iam.user_preference.user_id` — referenced by (1:N)
- ← `iam.user_role.user_id` — referenced by (1:N)
- ← `iam.user_session.user_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `academics.lesson_plan_approval.decided_by_user_id`
- ← `assessment.assignment_approval.decided_by_user_id`
- ← `assessment.assignment_submission.graded_by_user_id`
- ← `assessment.exam_result.evaluated_by_user_id`
- ← `assessment.homework_approval.decided_by_user_id`
- ← `assessment.homework_submission.checked_by_user_id`
- ← `assessment.marksheet.generated_by_user_id`
- ← `assessment.marksheet.published_by_user_id`
- ← `assessment.result_change_log.changed_by_user_id`
- ← `attendance.attendance_record.recorded_by_user_id`
- ← `attendance.staff_attendance.recorded_by_user_id`
- ← `careers.job_application.applicant_user_id`
- ← `careers.job_opening.posted_by_user_id`
- ← `comms.audience_group_member.user_id`
- ← `comms.calendar_export.user_id`
- ← `comms.class_page.owner_user_id`
- ← `comms.class_page_member.user_id`
- ← `comms.content_view.viewer_user_id`
- ← `comms.conversation_message.sender_user_id`
- ← `comms.conversation_participant.user_id`
- ← `comms.event.owner_user_id`
- ← `comms.event_attendee.user_id`
- ← `comms.event_occurrence.user_id`
- ← `comms.feedback_message.sender_user_id`
- ← `comms.feedback_thread.assigned_to_user_id`
- ← `comms.in_app_notification.recipient_user_id`
- ← `comms.mailbox_message.owner_user_id`
- ← `comms.message_receipt.user_id`
- ← `comms.notification_preference_set.user_id`
- ← `comms.outbound_message.recipient_user_id`
- ← `comms.post.author_user_id`
- ← `comms.post_comment.author_user_id`
- ← `comms.reaction.user_id`
- ← `comms.task.owner_user_id`
- ← `comms.task_assignee.claimed_by_user_id`
- ← `comms.task_assignee.user_id`
- ← `finance.fee_concession.approved_by_user_id`
- ← `finance.invoice.bill_to_user_id`
- ← `finance.payment.payer_user_id`
- ← `finance.payment.received_by_user_id`
- ← `finance.refund.refunded_by_user_id`
- ← `frontoffice.call_log.logged_by_user_id`
- ← `frontoffice.postal_record.logged_by_user_id`
- ← `hr.leave_application.decided_by_user_id`
- ← `hr.staff.user_id`
- ← `library.loan.borrower_user_id`
- ← `library.loan.issued_by_user_id`
- ← `library.reservation.requested_by_user_id`
- ← `lms.access_log.user_id`
- ← `lms.annotation.user_id`
- ← `lms.annotation_share.shared_with_user_id`
- ← `lms.assignment_group_member.user_id`
- ← `lms.assignment_submission.evaluator_user_id`
- ← `lms.assignment_submission.user_id`
- ← `lms.badge_award.user_id`
- ← `lms.bookmark.user_id`
- ← `lms.certificate.user_id`
- ← `lms.certificate_evaluation.evaluator_user_id`
- ← `lms.certificate_evaluation.user_id`
- ← `lms.certificate_request.evaluator_user_id`
- ← `lms.certificate_request.user_id`
- ← `lms.cohort_enrollment.graded_by`
- ← `lms.cohort_enrollment.user_id`
- ← `lms.cohort_feedback.user_id`
- ← `lms.cohort_teacher.user_id`
- ← `lms.conferencing_account.owner_user_id`
- ← `lms.content_author.user_id`
- ← `lms.content_item_rating.user_id`
- ← `lms.content_view_session.user_id`
- ← `lms.course_interest.user_id`
- ← `lms.course_mentor.user_id`
- ← `lms.course_review.user_id`
- ← `lms.course_teacher.user_id`
- ← `lms.drive_item.owner_user_id`
- ← `lms.drive_item_share.user_id`
- ← `lms.enrollment.user_id`
- ← `lms.enrollment_request.decided_by`
- ← `lms.enrollment_request.user_id`
- ← `lms.evaluator.user_id`
- ← `lms.exercise_submission.user_id`
- ← `lms.forum_post.abuse_confirmed_by`
- ← `lms.forum_post.author_user_id`
- ← `lms.forum_post_report.reported_by`
- ← `lms.forum_post_vote.user_id`
- ← `lms.forum_read_state.user_id`
- ← `lms.forum_topic.individual_user_id`
- ← `lms.grade_change_log.changed_by`
- ← `lms.grade_change_log.student_user_id`
- ← `lms.gradebook_entry.graded_by`
- ← `lms.gradebook_entry.user_id`
- ← `lms.import_export_job.requested_by`
- ← `lms.item_override.user_id`
- ← `lms.learning_program_member.user_id`
- ← `lms.lesson_progress.user_id`
- ← `lms.live_class.host_user_id`
- ← `lms.live_class_participant.user_id`
- ← `lms.mentorship.mentee_user_id`
- ← `lms.mentorship.mentor_user_id`
- ← `lms.peer_review.reviewee_user_id`
- ← `lms.peer_review.reviewer_user_id`
- ← `lms.plagiarism_account.default_teacher_user_id`
- ← `lms.plagiarism_user_link.user_id`
- ← `lms.quiz_answer.graded_by`
- ← `lms.quiz_submission.user_id`
- ← `lms.reassignment_log.reassigned_by`
- ← `lms.reassignment_log.user_id`
- ← `lms.related_material_rank.user_id`
- ← `lms.release_grant.user_id`
- ← `lms.rubric_assessment.user_id`
- ← `lms.student_alert.sent_by`
- ← `lms.student_alert.user_id`
- ← `lms.video_watch.user_id`
- ← `payroll.disbursement_account.managed_by_user_id`
- ← `people.consent_record.recorded_by_user_id`
- ← `platform.file_object.uploaded_by_user_id`
- ← `platform.storage_quota.user_id`
- ← `platform.tenant_subscription.requested_by_user_id`
- ← `sis.discipline_incident.reported_by_user_id`
- ← `sis.guardian.user_id`
- ← `sis.student.user_id`
- ← `sis.student_leave_request.decided_by_user_id`
- ← `sis.student_leave_request.requested_by_user_id`

<a id="iamuser_department_access"></a>
### iam.user_department_access

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `user_id` | uuid | Y |  | FK → `iam.user_account` · CASCADE · tenant-composite |  |
| 4 | `department_id` | uuid | Y |  | logical → `org.department` (cross-module, no FK) |  |
| 5 | `is_primary` | bool | Y | FALSE |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_user_department_access_1` (`user_id`, `department_id`) WHERE deleted_at IS NULL

<a id="iamuser_device"></a>
### iam.user_device

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `user_id` | uuid | Y |  | FK → `iam.user_account` · CASCADE · tenant-composite |  |
| 4 | `device_identifier` | text | Y |  |  |  |
| 5 | `platform` | varchar(?) | Y |  |  |  |
| 6 | `push_token` | text |  |  |  |  |
| 7 | `last_seen_at` | timestamptz |  |  |  |  |
| 8 | `is_active` | bool | Y | TRUE |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_user_device_1` (`user_id`, `device_identifier`) WHERE deleted_at IS NULL

**Checks**

- `ck_user_device_platform`: platform IN ('android', 'ios', 'web')

**Relationships**

- ← `iam.user_session.device_id` — referenced by (1:N)

<a id="iamuser_permission"></a>
### iam.user_permission

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `user_id` | uuid | Y |  | FK → `iam.user_account` · CASCADE · tenant-composite |  |
| 4 | `permission_id` | uuid | Y |  | FK → `iam.permission` · CASCADE |  |
| 5 | `effect` | varchar(?) | Y | 'grant' |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_user_permission_1` (`user_id`, `permission_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_user_permission_effect`: effect IN ('grant', 'deny')

<a id="iamuser_preference"></a>
### iam.user_preference

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `user_id` | uuid | Y |  | FK → `iam.user_account` · CASCADE · tenant-composite |  |
| 4 | `pref_key` | varchar(?) | Y |  |  |  |
| 5 | `pref_value` | varchar(?) |  |  |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_user_preference_1` (`user_id`, `pref_key`) WHERE deleted_at IS NULL

<a id="iamuser_role"></a>
### iam.user_role

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `user_id` | uuid | Y |  | FK → `iam.user_account` · CASCADE · tenant-composite |  |
| 4 | `role_id` | uuid | Y |  | FK → `iam.role` · CASCADE · tenant-composite |  |
| 5 | `scope_type` | varchar(?) | Y | 'tenant' |  |  |
| 6 | `scope_id` | uuid |  |  |  |  |
| 7 | `valid_from` | date |  |  |  |  |
| 8 | `valid_to` | date |  |  |  |  |
| 9 | `is_active` | bool | Y | TRUE |  |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |
| 16 | `is_primary` | bool | Y | FALSE |  |  |

**Uniqueness**

- unique index `uq_user_role_assignment` (`user_id`, `role_id`, `scope_type`, `scope_id`) WHERE deleted_at IS NULL
- unique index `uq_user_role_primary` (`tenant_id`, `user_id`, `scope_type`) WHERE is_primary AND deleted_at IS NULL

**Checks**

- `ck_user_role_scope_type`: scope_type IN ('tenant', 'institution', 'department', 'class_section', 'course', 'lms_course', 'lms_cohort')
- `ck_user_role_1`: (scope_type = 'tenant') = (scope_id IS NULL)

<a id="iamuser_session"></a>
### iam.user_session

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `user_id` | uuid | Y |  | FK → `iam.user_account` · CASCADE · tenant-composite |  |
| 4 | `session_ref` | varchar(?) | Y |  | UQ |  |
| 5 | `device_id` | uuid |  |  | FK → `iam.user_device` · SET NULL · tenant-composite |  |
| 6 | `ip_address` | inet |  |  |  |  |
| 7 | `user_agent` | varchar(?) |  |  |  |  |
| 8 | `started_at` | timestamptz | Y | now() |  |  |
| 9 | `last_seen_at` | timestamptz |  |  |  |  |
| 10 | `ended_at` | timestamptz |  |  |  |  |
| 11 | `impersonator_user_id` | uuid |  |  |  | Administrator acting as the user in this session (iam.StartImpersonation); NULL for the user's own sign-in. |
| 12 | `active_role_id` | uuid |  |  |  | Role the session works in after iam.SwitchRole; NULL = every role the user holds. |
| 13 | `active_scope_id` | uuid |  |  |  | Scope unit of the active role's grant; NULL = every grant of the active role. |
| 14 | `created_at` | timestamptz | Y | now() |  |  |
| 15 | `created_by` | uuid |  |  |  |  |
| 16 | `updated_at` | timestamptz | Y | now() |  |  |
| 17 | `updated_by` | uuid |  |  |  |  |
| 18 | `deleted_at` | timestamptz |  |  |  |  |
| 19 | `row_version` | int4 | Y | 0 |  |  |

## `org` — Organisation & Calendar

Organisation & Calendar: Institutions (schools, colleges, campuses), their departments, academic years and terms.

13 tables: [`academic_term`](#orgacademic_term), [`academic_year`](#orgacademic_year), [`attribute_definition`](#orgattribute_definition), [`department`](#orgdepartment), [`holiday`](#orgholiday), [`idempotency_key`](#orgidempotency_key), [`inbox_event`](#orginbox_event), [`institution`](#orginstitution), [`institution_attribute`](#orginstitution_attribute), [`institution_type`](#orginstitution_type), [`institution_type_attribute`](#orginstitution_type_attribute), [`operation`](#orgoperation), [`outbox_event`](#orgoutbox_event)

<a id="orgacademic_term"></a>
### org.academic_term

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `academic_year_id` | uuid | Y |  | FK → `org.academic_year` · CASCADE · tenant-composite |  |
| 4 | `parent_term_id` | uuid |  |  | FK → `org.academic_term` · SET NULL · tenant-composite |  |
| 5 | `name` | varchar(?) | Y |  |  |  |
| 6 | `start_date` | date | Y |  |  |  |
| 7 | `end_date` | date | Y |  |  |  |
| 8 | `sequence` | int2 |  |  |  |  |
| 9 | `code` | varchar(?) |  |  |  |  |
| 10 | `is_active` | bool | Y | TRUE |  |  |
| 11 | `created_at` | timestamptz | Y | now() |  |  |
| 12 | `created_by` | uuid |  |  |  |  |
| 13 | `updated_at` | timestamptz | Y | now() |  |  |
| 14 | `updated_by` | uuid |  |  |  |  |
| 15 | `deleted_at` | timestamptz |  |  |  |  |
| 16 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_academic_term_name` (`academic_year_id`, `parent_term_id`, `name`) WHERE deleted_at IS NULL
- unique index `uq_academic_term_start` (`academic_year_id`, `parent_term_id`, `start_date`) WHERE deleted_at IS NULL
- unique index `uq_academic_term_code` (`academic_year_id`, `code`) WHERE code IS NOT NULL AND deleted_at IS NULL

**Checks**

- `ck_academic_term_1`: end_date >= start_date

**Relationships**

- ← `org.academic_term.parent_term_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `admission.admission_cycle.academic_term_id`
- ← `assessment.exam_session.academic_term_id`
- ← `lms.cohort.academic_term_id`
- ← `sis.program_enrollment.academic_term_id`
- ← `sis.subject_registration.academic_term_id`

<a id="orgacademic_year"></a>
### org.academic_year

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | FK → `org.institution` · RESTRICT · tenant-composite |  |
| 4 | `name` | varchar(?) | Y |  |  |  |
| 5 | `description` | text |  |  |  |  |
| 6 | `start_date` | date | Y |  |  |  |
| 7 | `end_date` | date | Y |  |  |  |
| 8 | `term_structure` | varchar(?) |  |  |  |  |
| 9 | `status` | varchar(?) | Y | 'planned' |  |  |
| 10 | `is_current` | bool | Y | FALSE |  |  |
| 11 | `created_at` | timestamptz | Y | now() |  |  |
| 12 | `created_by` | uuid |  |  |  |  |
| 13 | `updated_at` | timestamptz | Y | now() |  |  |
| 14 | `updated_by` | uuid |  |  |  |  |
| 15 | `deleted_at` | timestamptz |  |  |  |  |
| 16 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `ix_academic_year_1` (`institution_id`) WHERE is_current AND deleted_at IS NULL
- unique index `uq_academic_year_1` (`institution_id`, `name`) WHERE deleted_at IS NULL

**Checks**

- `ck_academic_year_term_structure`: term_structure IN ('two_semesters', 'two_semesters_quarterly', 'two_semesters_final', 'three_semesters', 'four_quarters', 'final_year', 'other')
- `ck_academic_year_status`: status IN ('planned', 'active', 'closed')
- `ck_academic_year_1`: end_date >= start_date

**Relationships**

- ← `org.academic_term.academic_year_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `academics.class_section.academic_year_id`
- ← `admission.admission_cycle.academic_year_id`
- ← `admission.application.academic_year_id`
- ← `assessment.assignment.academic_year_id`
- ← `assessment.exam_session.academic_year_id`
- ← `assessment.grading_scheme.academic_year_id`
- ← `assessment.homework.academic_year_id`
- ← `attendance.attendance_sheet.academic_year_id`
- ← `comms.class_page.academic_year_id`
- ← `comms.event.academic_year_id`
- ← `comms.magazine.academic_year_id`
- ← `comms.mailbox_message.academic_year_id`
- ← `comms.notice.academic_year_id`
- ← `comms.outbound_message.academic_year_id`
- ← `comms.page_category.academic_year_id`
- ← `comms.post.academic_year_id`
- ← `comms.task.academic_year_id`
- ← `finance.fee_concession.academic_year_id`
- ← `finance.fee_structure.academic_year_id`
- ← `finance.student_fee_due.academic_year_id`
- ← `frontoffice.call_log.academic_year_id`
- ← `frontoffice.postal_record.academic_year_id`
- ← `frontoffice.visitor_log.academic_year_id`
- ← `hr.leave_application.academic_year_id`
- ← `hr.leave_balance.academic_year_id`
- ← `hr.leave_type.academic_year_id`
- ← `library.media.acquired_academic_year_id`
- ← `lms.cohort.academic_year_id`
- ← `sis.discipline_incident.academic_year_id`
- ← `sis.program_enrollment.academic_year_id`
- ← `sis.promotion.from_academic_year_id`
- ← `sis.promotion.to_academic_year_id`
- ← `sis.student_enrollment.academic_year_id`
- ← `sis.student_health_record.academic_year_id`
- ← `sis.student_leave_request.academic_year_id`
- ← `sis.subject_registration.academic_year_id`
- ← `transport.route.academic_year_id`
- ← `transport.student_transport.academic_year_id`

<a id="orgattribute_definition"></a>
### org.attribute_definition

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `name` | varchar(?) | Y |  |  |  |
| 4 | `data_type` | varchar(?) | Y | 'text' |  |  |
| 5 | `sort_order` | int4 | Y | 0 |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_attribute_definition_1` (`tenant_id`, `name`) WHERE deleted_at IS NULL

**Checks**

- `ck_attribute_definition_data_type`: data_type IN ('text', 'number', 'date', 'boolean', 'email', 'url', 'phone', 'select')

**Relationships**

- ← `org.institution_attribute.attribute_definition_id` — referenced by (1:N)
- ← `org.institution_type_attribute.attribute_definition_id` — referenced by (1:N)

<a id="orgdepartment"></a>
### org.department

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | FK → `org.institution` · RESTRICT · tenant-composite |  |
| 4 | `parent_department_id` | uuid |  |  | FK → `org.department` · SET NULL · tenant-composite |  |
| 5 | `code` | varchar(?) | Y |  |  |  |
| 6 | `name` | varchar(?) | Y |  |  |  |
| 7 | `is_active` | bool | Y | TRUE |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_department_1` (`institution_id`, `code`) WHERE deleted_at IS NULL

**Relationships**

- ← `org.department.parent_department_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `academics.course.department_id`
- ← `academics.program.department_id`
- ← `academics.subject.department_id`
- ← `hr.staff.primary_department_id`
- ← `hr.staff_department.department_id`
- ← `iam.user_department_access.department_id`

<a id="orgholiday"></a>
### org.holiday

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | FK → `org.institution` · CASCADE · tenant-composite |  |
| 4 | `title` | varchar(?) | Y |  |  |  |
| 5 | `start_date` | date | Y |  |  |  |
| 6 | `end_date` | date | Y |  |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_holiday_slot` (`tenant_id`, `institution_id`, `start_date`, `title`) WHERE deleted_at IS NULL

**Checks**

- `ck_holiday_1`: end_date >= start_date

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `lms.cohort_holiday.holiday_id`

<a id="orgidempotency_key"></a>
### org.idempotency_key

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `idempotency_key` | uuid | Y |  |  |  |
| 4 | `user_id` | uuid |  |  |  |  |
| 5 | `operation_name` | varchar(?) | Y |  |  |  |
| 6 | `request_hash` | varchar(?) |  |  |  |  |
| 7 | `response_status` | int2 |  |  |  |  |
| 8 | `response_body` | text |  |  |  |  |
| 9 | `locked_at` | timestamptz |  |  |  |  |
| 10 | `completed_at` | timestamptz |  |  |  |  |
| 11 | `expires_at` | timestamptz | Y |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `idempotency_key`)

<a id="orginbox_event"></a>
### org.inbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `event_id` | uuid | Y |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  | PK |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `received_at` | timestamptz | Y | now() |  |  |
| 5 | `processed_at` | timestamptz |  |  |  |  |
| 6 | `processing_error` | text |  |  |  |  |

<a id="orginstitution"></a>
### org.institution

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `parent_institution_id` | uuid |  |  | FK → `org.institution` · SET NULL · tenant-composite |  |
| 4 | `code` | varchar(?) | Y |  |  |  |
| 5 | `name` | varchar(?) | Y |  |  |  |
| 6 | `slug` | varchar(?) |  |  |  |  |
| 7 | `institution_type` | varchar(?) | Y | 'school' |  |  |
| 8 | `email` | varchar(?) |  |  |  |  |
| 9 | `phone` | varchar(?) |  |  |  |  |
| 10 | `address_line1` | varchar(?) |  |  |  |  |
| 11 | `address_line2` | varchar(?) |  |  |  |  |
| 12 | `city_id` | uuid |  |  | logical → `master.city` (cross-module, no FK) |  |
| 13 | `state_province_id` | uuid |  |  | logical → `master.state_province` (cross-module, no FK) |  |
| 14 | `country_id` | uuid |  |  | logical → `master.country` (cross-module, no FK) |  |
| 15 | `postal_code` | varchar(?) |  |  |  |  |
| 16 | `timezone` | varchar(?) |  |  |  |  |
| 17 | `currency_id` | uuid |  |  | logical → `master.currency` (cross-module, no FK) |  |
| 18 | `logo_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 19 | `signature_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 20 | `accreditation` | text |  |  |  |  |
| 21 | `approval_authority` | text |  |  |  |  |
| 22 | `status` | varchar(?) | Y | 'active' |  |  |
| 23 | `institution_type_id` | uuid |  |  | FK → `org.institution_type` · SET NULL · tenant-composite |  |
| 24 | `theme_id` | uuid |  |  | logical → `platform.theme` (cross-module, no FK) |  |
| 25 | `created_at` | timestamptz | Y | now() |  |  |
| 26 | `created_by` | uuid |  |  |  |  |
| 27 | `updated_at` | timestamptz | Y | now() |  |  |
| 28 | `updated_by` | uuid |  |  |  |  |
| 29 | `deleted_at` | timestamptz |  |  |  |  |
| 30 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_institution_1` (`tenant_id`, `code`) WHERE deleted_at IS NULL
- unique index `uq_institution_2` (`tenant_id`, `slug`) WHERE deleted_at IS NULL

**Checks**

- `ck_institution_institution_type`: institution_type IN ('school', 'college', 'university', 'coaching_centre', 'training_centre')
- `ck_institution_status`: status IN ('active', 'inactive')

**Relationships**

- ← `org.academic_year.institution_id` — referenced by (1:1)
- ← `org.department.institution_id` — referenced by (1:N)
- ← `org.holiday.institution_id` — referenced by (1:N)
- ← `org.institution.parent_institution_id` — referenced by (1:N)
- ← `org.institution_attribute.institution_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `academics.class_section.institution_id`
- ← `academics.course.institution_id`
- ← `academics.grade_level.institution_id`
- ← `academics.program.institution_id`
- ← `academics.section.institution_id`
- ← `academics.subject.institution_id`
- ← `admission.admission_cycle.institution_id`
- ← `admission.application.institution_id`
- ← `admission.number_sequence.institution_id`
- ← `assessment.assignment.institution_id`
- ← `assessment.exam_session.institution_id`
- ← `assessment.grading_scheme.institution_id`
- ← `assessment.homework.institution_id`
- ← `attendance.attendance_register.institution_id`
- ← `attendance.attendance_type.institution_id`
- ← `comms.audience_group.institution_id`
- ← `comms.class_page.institution_id`
- ← `comms.content_view.institution_id`
- ← `comms.conversation.institution_id`
- ← `comms.event.institution_id`
- ← `comms.event_institution.institution_id`
- ← `comms.feedback_thread.institution_id`
- ← `comms.magazine.institution_id`
- ← `comms.mailbox_message.institution_id`
- ← `comms.message_template.institution_id`
- ← `comms.notice.institution_id`
- ← `comms.notice_audience.institution_id`
- ← `comms.notice_background.institution_id`
- ← `comms.outbound_message.institution_id`
- ← `comms.page_category.institution_id`
- ← `comms.post.institution_id`
- ← `comms.task.institution_id`
- ← `facility.asset_item.institution_id`
- ← `facility.facility.institution_id`
- ← `facility.room.institution_id`
- ← `finance.fee_concession.institution_id`
- ← `finance.fee_head.institution_id`
- ← `finance.fee_plan.institution_id`
- ← `finance.fee_structure.institution_id`
- ← `finance.invoice.institution_id`
- ← `finance.number_sequence.institution_id`
- ← `finance.payment.institution_id`
- ← `finance.payment_gateway_account.institution_id`
- ← `finance.student_fee_due.institution_id`
- ← `frontoffice.call_log.institution_id`
- ← `frontoffice.directory_contact.institution_id`
- ← `frontoffice.postal_record.institution_id`
- ← `frontoffice.visitor_log.institution_id`
- ← `hr.leave_application.institution_id`
- ← `hr.leave_type.institution_id`
- ← `hr.number_sequence.institution_id`
- ← `hr.staff.institution_id`
- ← `library.library_card.institution_id`
- ← `library.media.institution_id`
- ← `library.media_category.institution_id`
- ← `library.number_sequence.institution_id`
- ← `lms.assignment.institution_id`
- ← `lms.cohort.institution_id`
- ← `lms.course.institution_id`
- ← `lms.discussion_prompt.institution_id`
- ← `lms.ebook.institution_id`
- ← `lms.external_tool_institution.institution_id`
- ← `lms.external_tool_resource.institution_id`
- ← `lms.import_export_job.institution_id`
- ← `lms.number_sequence.institution_id`
- ← `lms.participation_activity.institution_id`
- ← `lms.quiz.institution_id`
- ← `lms.rubric.institution_id`
- ← `lms.web_resource.institution_id`
- ← `payroll.disbursement_account.institution_id`
- ← `payroll.number_sequence.institution_id`
- ← `payroll.payroll_payment.institution_id`
- ← `payroll.payslip.institution_id`
- ← `payroll.salary_template.institution_id`
- ← `people.person_document.institution_id`
- ← `platform.entity_note.institution_id`
- ← `platform.storage_quota.institution_id`
- ← `platform.tenant_subscription.institution_id`
- ← `sis.discipline_incident.institution_id`
- ← `sis.number_sequence.institution_id`
- ← `sis.promotion.institution_id`
- ← `sis.student.institution_id`
- ← `sis.student_category.institution_id`
- ← `timetable.period.institution_id`
- ← `timetable.session.institution_id`
- ← `transport.route.institution_id`

<a id="orginstitution_attribute"></a>
### org.institution_attribute

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | FK → `org.institution` · CASCADE · tenant-composite |  |
| 4 | `attr_key` | varchar(?) | Y |  |  |  |
| 5 | `attr_value` | text |  |  |  |  |
| 6 | `attribute_definition_id` | uuid |  |  | FK → `org.attribute_definition` · SET NULL · tenant-composite |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_institution_attribute_1` (`institution_id`, `attr_key`) WHERE deleted_at IS NULL

<a id="orginstitution_type"></a>
### org.institution_type

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `name` | varchar(?) | Y |  |  |  |
| 4 | `created_at` | timestamptz | Y | now() |  |  |
| 5 | `created_by` | uuid |  |  |  |  |
| 6 | `updated_at` | timestamptz | Y | now() |  |  |
| 7 | `updated_by` | uuid |  |  |  |  |
| 8 | `deleted_at` | timestamptz |  |  |  |  |
| 9 | `row_version` | int4 | Y | 0 |  |  |
| 10 | `code` | varchar(?) | Y |  |  |  |
| 11 | `sort_order` | int4 | Y | 0 |  |  |
| 12 | `is_active` | bool | Y | TRUE |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_institution_type_scope_code` (`tenant_id`, `code`) WHERE deleted_at IS NULL
- unique index `uq_institution_type_1` (`tenant_id`, `name`) WHERE deleted_at IS NULL

**Relationships**

- ← `org.institution.institution_type_id` — referenced by (1:N)
- ← `org.institution_type_attribute.institution_type_id` — referenced by (1:N)

<a id="orginstitution_type_attribute"></a>
### org.institution_type_attribute

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_type_id` | uuid | Y |  | FK → `org.institution_type` · CASCADE · tenant-composite |  |
| 4 | `attribute_definition_id` | uuid | Y |  | FK → `org.attribute_definition` · CASCADE · tenant-composite |  |
| 5 | `is_required` | bool | Y | FALSE |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_institution_type_attribute_1` (`institution_type_id`, `attribute_definition_id`) WHERE deleted_at IS NULL

<a id="orgoperation"></a>
### org.operation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `operation_name` | varchar(?) | Y |  |  |  |
| 4 | `resource_type` | varchar(?) |  |  |  |  |
| 5 | `resource_id` | uuid |  |  |  |  |
| 6 | `status` | varchar(?) | Y | 'running' |  |  |
| 7 | `progress_percent` | int2 |  |  |  |  |
| 8 | `error_code` | varchar(?) |  |  |  |  |
| 9 | `error_message` | varchar(?) |  |  |  |  |
| 10 | `result_entity_type` | varchar(?) |  |  |  |  |
| 11 | `result_entity_id` | uuid |  |  |  |  |
| 12 | `started_by_user_id` | uuid |  |  |  |  |
| 13 | `started_at` | timestamptz | Y | now() |  |  |
| 14 | `finished_at` | timestamptz |  |  |  |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_org_operation_status`: status IN ('running', 'succeeded', 'failed', 'cancelled')

<a id="orgoutbox_event"></a>
### org.outbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | int8 |  |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `aggregate_type` | varchar(?) | Y |  |  |  |
| 5 | `aggregate_id` | uuid | Y |  |  |  |
| 6 | `payload` | text | Y |  |  |  |
| 7 | `occurred_at` | timestamptz | Y | now() |  |  |
| 8 | `published_at` | timestamptz |  |  |  |  |
| 9 | `publish_error` | text |  |  |  | Why the relay set the row aside (not published) |

## `people` — People & Profiles

People & Profiles: Every human known to the system (students, guardians, staff, applicants, students) and their personal, contact, education and experience records.

18 tables: [`certification`](#peoplecertification), [`consent_record`](#peopleconsent_record), [`education_history`](#peopleeducation_history), [`emergency_contact`](#peopleemergency_contact), [`honorific`](#peoplehonorific), [`idempotency_key`](#peopleidempotency_key), [`inbox_event`](#peopleinbox_event), [`operation`](#peopleoperation), [`outbox_event`](#peopleoutbox_event), [`person`](#peopleperson), [`person_address`](#peopleperson_address), [`person_document`](#peopleperson_document), [`person_skill`](#peopleperson_skill), [`person_social_link`](#peopleperson_social_link), [`public_profile`](#peoplepublic_profile), [`qualification`](#peoplequalification), [`skill`](#peopleskill), [`work_experience`](#peoplework_experience)

<a id="peoplecertification"></a>
### people.certification

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `person_id` | uuid | Y |  | FK → `people.person` · CASCADE · tenant-composite |  |
| 4 | `name` | varchar(?) | Y |  |  |  |
| 5 | `issuing_organization` | varchar(?) | Y |  |  |  |
| 6 | `issue_date` | date | Y |  |  |  |
| 7 | `expires` | bool | Y | FALSE |  |  |
| 8 | `expiration_date` | date |  |  |  |  |
| 9 | `description` | text |  |  |  |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |

<a id="peopleconsent_record"></a>
### people.consent_record

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `person_id` | uuid | Y |  | FK → `people.person` · RESTRICT · tenant-composite |  |
| 4 | `given_by_person_id` | uuid | Y |  | FK → `people.person` · RESTRICT · tenant-composite |  |
| 5 | `purpose` | varchar(?) | Y |  |  |  |
| 6 | `policy_version` | varchar(?) | Y |  |  |  |
| 7 | `granted_at` | timestamptz | Y |  |  |  |
| 8 | `withdrawn_at` | timestamptz |  |  |  |  |
| 9 | `channel` | varchar(?) | Y |  |  |  |
| 10 | `recorded_by_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 11 | `evidence_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 12 | `ip_address` | inet |  |  |  |  |
| 13 | `created_at` | timestamptz | Y | now() |  |  |
| 14 | `created_by` | uuid |  |  |  |  |
| 15 | `updated_at` | timestamptz | Y | now() |  |  |
| 16 | `updated_by` | uuid |  |  |  |  |
| 17 | `deleted_at` | timestamptz |  |  |  |  |
| 18 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `ix_consent_record_1` (`person_id`, `purpose`) WHERE withdrawn_at IS NULL AND deleted_at IS NULL

**Checks**

- `ck_consent_record_purpose`: purpose IN ('data_processing', 'photo_video_publication', 'public_profile', 'marketing', 'third_party_sharing', 'sms_whatsapp_messages', 'biometric_data')
- `ck_consent_record_channel`: channel IN ('web', 'mobile_app', 'paper_form', 'email', 'in_person')
- `ck_consent_record_1`: withdrawn_at IS NULL OR withdrawn_at >= granted_at

<a id="peopleeducation_history"></a>
### people.education_history

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `person_id` | uuid | Y |  | FK → `people.person` · CASCADE · tenant-composite |  |
| 4 | `qualification_id` | uuid |  |  | FK → `people.qualification` · RESTRICT · tenant-composite |  |
| 5 | `institution_name` | varchar(?) | Y |  |  |  |
| 6 | `location` | varchar(?) |  |  |  |  |
| 7 | `degree_type` | varchar(?) |  |  |  |  |
| 8 | `major` | varchar(?) |  |  |  |  |
| 9 | `grade_type` | varchar(?) |  |  |  |  |
| 10 | `grade` | varchar(?) |  |  |  |  |
| 11 | `start_date` | date |  |  |  |  |
| 12 | `end_date` | date |  |  |  |  |
| 13 | `certification` | varchar(?) |  |  |  |  |
| 14 | `minor` | varchar(?) |  |  |  |  |
| 15 | `study_mode` | varchar(?) |  |  |  |  |
| 16 | `sort_order` | int4 | Y | 0 |  |  |
| 17 | `created_at` | timestamptz | Y | now() |  |  |
| 18 | `created_by` | uuid |  |  |  |  |
| 19 | `updated_at` | timestamptz | Y | now() |  |  |
| 20 | `updated_by` | uuid |  |  |  |  |
| 21 | `deleted_at` | timestamptz |  |  |  |  |
| 22 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_education_history_grade_type`: grade_type IN ('percentage', 'score', 'letter_grade', 'uk_class', 'french_mention', 'cgpa_4', 'cgpa_10')
- `ck_education_history_study_mode`: study_mode IN ('full_time', 'part_time', 'correspondence', 'online')

<a id="peopleemergency_contact"></a>
### people.emergency_contact

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `person_id` | uuid | Y |  | FK → `people.person` · CASCADE · tenant-composite |  |
| 4 | `contact_person_id` | uuid |  |  | FK → `people.person` · SET NULL · tenant-composite |  |
| 5 | `contact_name` | varchar(?) |  |  |  |  |
| 6 | `relationship` | varchar(?) |  |  |  |  |
| 7 | `phone` | varchar(?) |  |  |  |  |
| 8 | `priority` | int2 | Y | 1 |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_emergency_contact_1`: contact_person_id IS NOT NULL OR (contact_name IS NOT NULL AND phone IS NOT NULL)

<a id="peoplehonorific"></a>
### people.honorific

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `name` | varchar(?) | Y |  |  |  |
| 4 | `shortcut` | varchar(?) |  |  |  |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |
| 11 | `code` | varchar(?) | Y |  |  |  |
| 12 | `sort_order` | int4 | Y | 0 |  |  |
| 13 | `is_active` | bool | Y | TRUE |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_honorific_scope_code` (`tenant_id`, `code`) WHERE deleted_at IS NULL

**Relationships**

- ← `people.person.honorific_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `admission.application.honorific_id`

<a id="peopleidempotency_key"></a>
### people.idempotency_key

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `idempotency_key` | uuid | Y |  |  |  |
| 4 | `user_id` | uuid |  |  |  |  |
| 5 | `operation_name` | varchar(?) | Y |  |  |  |
| 6 | `request_hash` | varchar(?) |  |  |  |  |
| 7 | `response_status` | int2 |  |  |  |  |
| 8 | `response_body` | text |  |  |  |  |
| 9 | `locked_at` | timestamptz |  |  |  |  |
| 10 | `completed_at` | timestamptz |  |  |  |  |
| 11 | `expires_at` | timestamptz | Y |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `idempotency_key`)

<a id="peopleinbox_event"></a>
### people.inbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `event_id` | uuid | Y |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  | PK |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `received_at` | timestamptz | Y | now() |  |  |
| 5 | `processed_at` | timestamptz |  |  |  |  |
| 6 | `processing_error` | text |  |  |  |  |

<a id="peopleoperation"></a>
### people.operation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `operation_name` | varchar(?) | Y |  |  |  |
| 4 | `resource_type` | varchar(?) |  |  |  |  |
| 5 | `resource_id` | uuid |  |  |  |  |
| 6 | `status` | varchar(?) | Y | 'running' |  |  |
| 7 | `progress_percent` | int2 |  |  |  |  |
| 8 | `error_code` | varchar(?) |  |  |  |  |
| 9 | `error_message` | varchar(?) |  |  |  |  |
| 10 | `result_entity_type` | varchar(?) |  |  |  |  |
| 11 | `result_entity_id` | uuid |  |  |  |  |
| 12 | `started_by_user_id` | uuid |  |  |  |  |
| 13 | `started_at` | timestamptz | Y | now() |  |  |
| 14 | `finished_at` | timestamptz |  |  |  |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_people_operation_status`: status IN ('running', 'succeeded', 'failed', 'cancelled')

<a id="peopleoutbox_event"></a>
### people.outbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | int8 |  |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `aggregate_type` | varchar(?) | Y |  |  |  |
| 5 | `aggregate_id` | uuid | Y |  |  |  |
| 6 | `payload` | text | Y |  |  |  |
| 7 | `occurred_at` | timestamptz | Y | now() |  |  |
| 8 | `published_at` | timestamptz |  |  |  |  |
| 9 | `publish_error` | text |  |  |  | Why the relay set the row aside (not published) |

<a id="peopleperson"></a>
### people.person

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `honorific_id` | uuid |  |  | FK → `people.honorific` · SET NULL · tenant-composite |  |
| 4 | `first_name` | varchar(?) |  |  |  |  |
| 5 | `middle_name` | varchar(?) |  |  |  |  |
| 6 | `last_name` | varchar(?) |  |  |  |  |
| 7 | `display_name` | varchar(?) | Y |  |  |  |
| 8 | `date_of_birth` | date |  |  |  |  |
| 9 | `gender` | varchar(?) |  |  |  |  |
| 10 | `blood_group` | varchar(?) |  |  |  |  |
| 11 | `marital_status` | varchar(?) |  |  |  |  |
| 12 | `birth_place` | varchar(?) |  |  |  |  |
| 13 | `native_place` | varchar(?) |  |  |  |  |
| 14 | `nationality_country_id` | uuid |  |  | logical → `master.country` (cross-module, no FK) |  |
| 15 | `mother_tongue` | varchar(?) |  |  |  |  |
| 16 | `religion` | varchar(?) |  |  |  |  |
| 17 | `social_category` | varchar(?) |  |  |  |  |
| 18 | `social_sub_category` | varchar(?) |  |  |  |  |
| 19 | `national_id_type` | varchar(?) |  |  |  |  |
| 20 | `national_id_enc` | text |  |  |  |  |
| 21 | `visa_info` | varchar(?) |  |  |  |  |
| 22 | `primary_email` | varchar(?) |  |  |  |  |
| 23 | `primary_phone` | varchar(?) |  |  |  |  |
| 24 | `alternate_phone` | varchar(?) |  |  |  |  |
| 25 | `photo_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 26 | `remarks` | text |  |  |  |  |
| 27 | `work_phone` | varchar(?) |  |  |  |  |
| 28 | `website_url` | varchar(?) |  |  |  |  |
| 29 | `about_me` | text |  |  |  |  |
| 30 | `created_at` | timestamptz | Y | now() |  |  |
| 31 | `created_by` | uuid |  |  |  |  |
| 32 | `updated_at` | timestamptz | Y | now() |  |  |
| 33 | `updated_by` | uuid |  |  |  |  |
| 34 | `deleted_at` | timestamptz |  |  |  |  |
| 35 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_person_gender`: gender IN ('male', 'female', 'other', 'undisclosed')
- `ck_person_blood_group`: blood_group IN ('A+', 'A-', 'B+', 'B-', 'O+', 'O-', 'AB+', 'AB-', 'A1+', 'A1-', 'B1+', 'B1-', 'A1B+', 'A1B-')
- `ck_person_marital_status`: marital_status IN ('single', 'married', 'divorced', 'widowed')

**Relationships**

- ← `people.certification.person_id` — referenced by (1:N)
- ← `people.consent_record.given_by_person_id` — referenced by (1:N)
- ← `people.consent_record.person_id` — referenced by (1:N)
- ← `people.education_history.person_id` — referenced by (1:N)
- ← `people.emergency_contact.contact_person_id` — referenced by (1:N)
- ← `people.emergency_contact.person_id` — referenced by (1:N)
- ← `people.person_address.person_id` — referenced by (1:N)
- ← `people.person_document.person_id` — referenced by (1:N)
- ← `people.person_skill.person_id` — referenced by (1:N)
- ← `people.person_social_link.person_id` — referenced by (1:N)
- ← `people.public_profile.person_id` — referenced by (1:1)
- ← `people.work_experience.person_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `admission.application.applicant_person_id`
- ← `careers.candidate_preference.person_id`
- ← `finance.payment.payer_person_id`
- ← `hr.staff.person_id`
- ← `iam.user_account.person_id`
- ← `library.library_card.person_id`
- ← `library.purchase_request.requested_by_person_id`
- ← `library.reservation.requested_by_person_id`
- ← `sis.guardian.person_id`
- ← `sis.student.person_id`

<a id="peopleperson_address"></a>
### people.person_address

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `person_id` | uuid | Y |  | FK → `people.person` · CASCADE · tenant-composite |  |
| 4 | `address_type` | varchar(?) | Y |  |  |  |
| 5 | `line1` | varchar(?) |  |  |  |  |
| 6 | `line2` | varchar(?) |  |  |  |  |
| 7 | `city_id` | uuid |  |  | logical → `master.city` (cross-module, no FK) |  |
| 8 | `city_name` | varchar(?) |  |  |  |  |
| 9 | `state_province_id` | uuid |  |  | logical → `master.state_province` (cross-module, no FK) |  |
| 10 | `country_id` | uuid |  |  | logical → `master.country` (cross-module, no FK) |  |
| 11 | `postal_code` | varchar(?) |  |  |  |  |
| 12 | `is_primary` | bool | Y | FALSE |  |  |
| 13 | `state_name` | varchar(?) |  |  |  |  |
| 14 | `sort_order` | int4 | Y | 0 |  |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_person_address_address_type`: address_type IN ('permanent', 'communication', 'official', 'billing', 'other')

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `finance.payment.billing_address_id`

<a id="peopleperson_document"></a>
### people.person_document

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `person_id` | uuid | Y |  | FK → `people.person` · CASCADE · tenant-composite |  |
| 4 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 5 | `document_type` | varchar(?) | Y |  |  |  |
| 6 | `name` | varchar(?) | Y |  |  |  |
| 7 | `file_id` | uuid | Y |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 8 | `document_version` | int4 | Y | 1 |  |  |
| 9 | `status` | varchar(?) | Y | 'active' |  |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_person_document_1` (`person_id`, `name`, `document_version`) WHERE deleted_at IS NULL

**Checks**

- `ck_person_document_document_type`: document_type IN ('certificate', 'id_proof', 'other')
- `ck_person_document_status`: status IN ('active', 'archived')

<a id="peopleperson_skill"></a>
### people.person_skill

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `person_id` | uuid | Y |  | FK → `people.person` · CASCADE · tenant-composite |  |
| 4 | `skill_id` | uuid | Y |  | FK → `people.skill` · RESTRICT · tenant-composite |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_person_skill_1` (`person_id`, `skill_id`) WHERE deleted_at IS NULL

<a id="peopleperson_social_link"></a>
### people.person_social_link

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `person_id` | uuid | Y |  | FK → `people.person` · CASCADE · tenant-composite |  |
| 4 | `network` | varchar(?) | Y |  |  |  |
| 5 | `handle` | varchar(?) | Y |  |  |  |
| 6 | `sort_order` | int4 | Y | 0 |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_person_social_link_1` (`person_id`, `network`, `handle`) WHERE deleted_at IS NULL

**Checks**

- `ck_person_social_link_network`: network IN ('facebook', 'twitter', 'linkedin', 'instagram', 'youtube', 'skype', 'whatsapp', 'telegram', 'github', 'website', 'other')

<a id="peoplepublic_profile"></a>
### people.public_profile

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `person_id` | uuid | Y |  | FK → `people.person` · CASCADE · tenant-composite |  |
| 4 | `headline` | varchar(?) |  |  |  |  |
| 5 | `profession` | varchar(?) |  |  |  |  |
| 6 | `persona_category` | varchar(?) |  |  |  |  |
| 7 | `cover_image_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 8 | `city` | varchar(?) |  |  |  |  |
| 9 | `country_id` | uuid |  |  | logical → `master.country` (cross-module, no FK) |  |
| 10 | `college` | varchar(?) |  |  |  |  |
| 11 | `branch` | varchar(?) |  |  |  |  |
| 12 | `linkedin_url` | varchar(?) |  |  |  |  |
| 13 | `github_url` | varchar(?) |  |  |  |  |
| 14 | `twitter_url` | varchar(?) |  |  |  |  |
| 15 | `medium_url` | varchar(?) |  |  |  |  |
| 16 | `hide_private_info` | bool | Y | FALSE |  |  |
| 17 | `created_at` | timestamptz | Y | now() |  |  |
| 18 | `created_by` | uuid |  |  |  |  |
| 19 | `updated_at` | timestamptz | Y | now() |  |  |
| 20 | `updated_by` | uuid |  |  |  |  |
| 21 | `deleted_at` | timestamptz |  |  |  |  |
| 22 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_public_profile_person_id` (`person_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_public_profile_persona_category`: persona_category IN ('business_owner', 'manager', 'employee', 'student', 'freelancer', 'other')

<a id="peoplequalification"></a>
### people.qualification

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `name` | varchar(?) | Y |  |  |  |
| 4 | `level` | varchar(?) | Y | 'other' |  |  |
| 5 | `is_active` | bool | Y | TRUE |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |
| 12 | `code` | varchar(?) | Y |  |  |  |
| 13 | `sort_order` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_qualification_scope_code` (`tenant_id`, `code`) WHERE deleted_at IS NULL

**Checks**

- `ck_qualification_level`: level IN ('undergraduate', 'postgraduate', 'teacher_training', 'doctorate', 'diploma', 'school', 'other')

**Relationships**

- ← `people.education_history.qualification_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `admission.application_guardian.qualification_id`
- ← `hr.staff.highest_qualification_id`
- ← `sis.guardian.qualification_id`

<a id="peopleskill"></a>
### people.skill

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `name` | varchar(?) | Y |  |  |  |
| 4 | `created_at` | timestamptz | Y | now() |  |  |
| 5 | `created_by` | uuid |  |  |  |  |
| 6 | `updated_at` | timestamptz | Y | now() |  |  |
| 7 | `updated_by` | uuid |  |  |  |  |
| 8 | `deleted_at` | timestamptz |  |  |  |  |
| 9 | `row_version` | int4 | Y | 0 |  |  |
| 10 | `code` | varchar(?) | Y |  |  |  |
| 11 | `sort_order` | int4 | Y | 0 |  |  |
| 12 | `is_active` | bool | Y | TRUE |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_skill_scope_code` (`tenant_id`, `code`) WHERE deleted_at IS NULL
- unique index `uq_skill_1` (`tenant_id`, `name`) WHERE deleted_at IS NULL

**Relationships**

- ← `people.person_skill.skill_id` — referenced by (1:N)

<a id="peoplework_experience"></a>
### people.work_experience

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `person_id` | uuid | Y |  | FK → `people.person` · CASCADE · tenant-composite |  |
| 4 | `experience_type` | varchar(?) | Y | 'job' |  |  |
| 5 | `title` | varchar(?) | Y |  |  |  |
| 6 | `company` | varchar(?) | Y |  |  |  |
| 7 | `location` | varchar(?) | Y |  |  |  |
| 8 | `from_date` | date | Y |  |  |  |
| 9 | `to_date` | date |  |  |  |  |
| 10 | `is_current` | bool | Y | FALSE |  |  |
| 11 | `description` | text |  |  |  |  |
| 12 | `company_website` | varchar(?) |  |  |  |  |
| 13 | `industry` | varchar(?) |  |  |  |  |
| 14 | `responsibilities` | text |  |  |  |  |
| 15 | `sort_order` | int4 | Y | 0 |  |  |
| 16 | `created_at` | timestamptz | Y | now() |  |  |
| 17 | `created_by` | uuid |  |  |  |  |
| 18 | `updated_at` | timestamptz | Y | now() |  |  |
| 19 | `updated_by` | uuid |  |  |  |  |
| 20 | `deleted_at` | timestamptz |  |  |  |  |
| 21 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_work_experience_experience_type`: experience_type IN ('job', 'internship')

<a id="peoplegreeting_template"></a>
### people.greeting_template

Which comms message templates a greeting may name, kept from `comms.message_template` events, so `SendGreetings` refuses a template comms would not deliver.

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `tenant_id` | uuid | Y |  | PK | Owning tenant. Row-level security key. |
| 2 | `template_id` | uuid | Y |  | PK | `comms.message_template.id` (kept from its events) |
| 3 | `institution_id` | uuid |  |  |  | Institution of the template; NULL for a tenant-wide one |
| 4 | `is_active` | bool | Y |  |  | Whether comms uses the template |
| 5 | `is_deleted` | bool | Y | false |  | Deleted in its module; kept so a late event cannot restore it |
| 6 | `source_version` | int4 | Y |  |  | Source row version this entry reflects; older events are ignored |
| 7 | `synced_at` | timestamptz | Y |  |  | When an event last changed the row |

**Checks**

- `ck_greeting_template_source_version`: source_version >= 1

## `academics` — Academic Structure

Academic Structure: Programs, courses, batches, grade levels, sections, class sections, subjects and their offerings, teaching assignments, curriculum units and lesson plans. Serves both K-12 (grade/section) and higher education (program/course/batch).

18 tables: [`batch`](#academicsbatch), [`class_section`](#academicsclass_section), [`class_subject`](#academicsclass_subject), [`course`](#academicscourse), [`course_subject`](#academicscourse_subject), [`curriculum_unit`](#academicscurriculum_unit), [`grade_level`](#academicsgrade_level), [`idempotency_key`](#academicsidempotency_key), [`inbox_event`](#academicsinbox_event), [`lesson_plan`](#academicslesson_plan), [`lesson_plan_approval`](#academicslesson_plan_approval), [`operation`](#academicsoperation), [`outbox_event`](#academicsoutbox_event), [`program`](#academicsprogram), [`program_level`](#academicsprogram_level), [`section`](#academicssection), [`subject`](#academicssubject), [`teaching_assignment`](#academicsteaching_assignment)

<a id="academicsbatch"></a>
### academics.batch

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `course_id` | uuid | Y |  | FK → `academics.course` · RESTRICT · tenant-composite |  |
| 4 | `code` | varchar(?) | Y |  |  |  |
| 5 | `name` | varchar(?) | Y |  |  |  |
| 6 | `start_date` | date | Y |  |  |  |
| 7 | `end_date` | date | Y |  |  |  |
| 8 | `is_active` | bool | Y | TRUE |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_batch_1` (`tenant_id`, `code`) WHERE deleted_at IS NULL

**Checks**

- `ck_batch_1`: end_date >= start_date

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `admission.application.batch_id`
- ← `assessment.assignment.batch_id`
- ← `assessment.exam_session.batch_id`
- ← `attendance.attendance_register.batch_id`
- ← `facility.room.default_batch_id`
- ← `lms.cohort.batch_id`
- ← `sis.program_enrollment.batch_id`
- ← `sis.subject_registration.batch_id`
- ← `timetable.session.batch_id`

<a id="academicsclass_section"></a>
### academics.class_section

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `academic_year_id` | uuid | Y |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `grade_level_id` | uuid | Y |  | FK → `academics.grade_level` · RESTRICT · tenant-composite |  |
| 6 | `section_id` | uuid | Y |  | FK → `academics.section` · RESTRICT · tenant-composite |  |
| 7 | `class_teacher_staff_id` | uuid |  |  | logical → `hr.staff` (cross-module, no FK) |  |
| 8 | `capacity` | int4 |  |  |  |  |
| 9 | `stream` | varchar(?) |  |  |  |  |
| 10 | `is_active` | bool | Y | TRUE |  |  |
| 11 | `created_at` | timestamptz | Y | now() |  |  |
| 12 | `created_by` | uuid |  |  |  |  |
| 13 | `updated_at` | timestamptz | Y | now() |  |  |
| 14 | `updated_by` | uuid |  |  |  |  |
| 15 | `deleted_at` | timestamptz |  |  |  |  |
| 16 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_class_section_1` (`academic_year_id`, `grade_level_id`, `section_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_class_section_capacity_rule`: capacity > 0

**Relationships**

- ← `academics.class_subject.class_section_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `assessment.assignment.class_section_id`
- ← `assessment.exam.class_section_id`
- ← `assessment.homework.class_section_id`
- ← `attendance.attendance_register.class_section_id`
- ← `comms.audience_group.class_section_id`
- ← `comms.event.class_section_id`
- ← `comms.notice.class_section_id`
- ← `comms.post_audience_class.class_section_id`
- ← `comms.task_assignee.class_section_id`
- ← `facility.room.default_class_section_id`
- ← `hr.leave_application.class_section_id`
- ← `sis.discipline_incident.class_section_id`
- ← `sis.student_enrollment.class_section_id`
- ← `timetable.session.class_section_id`

<a id="academicsclass_subject"></a>
### academics.class_subject

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `class_section_id` | uuid | Y |  | FK → `academics.class_section` · CASCADE · tenant-composite |  |
| 4 | `subject_id` | uuid | Y |  | FK → `academics.subject` · RESTRICT · tenant-composite |  |
| 5 | `offering_type` | varchar(?) | Y | 'core' |  |  |
| 6 | `is_active` | bool | Y | TRUE |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_class_subject_1` (`class_section_id`, `subject_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_class_subject_offering_type`: offering_type IN ('core', 'elective', 'exam_only')

**Relationships**

- ← `academics.curriculum_unit.class_subject_id` — referenced by (1:N)
- ← `academics.teaching_assignment.class_subject_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `lms.cohort.class_subject_id`

<a id="academicscourse"></a>
### academics.course

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `program_id` | uuid |  |  | FK → `academics.program` · RESTRICT · tenant-composite |  |
| 5 | `department_id` | uuid |  |  | logical → `org.department` (cross-module, no FK) |  |
| 6 | `parent_course_id` | uuid |  |  | FK → `academics.course` · SET NULL · tenant-composite |  |
| 7 | `code` | varchar(?) | Y |  |  |  |
| 8 | `name` | varchar(?) | Y |  |  |  |
| 9 | `evaluation_type` | varchar(?) | Y | 'normal' |  |  |
| 10 | `min_unit_load` | numeric(?,?) |  |  |  |  |
| 11 | `max_unit_load` | numeric(?,?) |  |  |  |  |
| 12 | `is_active` | bool | Y | TRUE |  |  |
| 13 | `created_at` | timestamptz | Y | now() |  |  |
| 14 | `created_by` | uuid |  |  |  |  |
| 15 | `updated_at` | timestamptz | Y | now() |  |  |
| 16 | `updated_by` | uuid |  |  |  |  |
| 17 | `deleted_at` | timestamptz |  |  |  |  |
| 18 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_course_1` (`institution_id`, `code`) WHERE deleted_at IS NULL

**Checks**

- `ck_course_evaluation_type`: evaluation_type IN ('normal', 'gpa', 'cwa', 'cce')

**Relationships**

- ← `academics.batch.course_id` — referenced by (1:N)
- ← `academics.course.parent_course_id` — referenced by (1:N)
- ← `academics.course_subject.course_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `admission.admission_cycle.course_id`
- ← `admission.admission_cycle_fee.course_id`
- ← `admission.application.course_id`
- ← `assessment.assignment.course_id`
- ← `assessment.exam_session.course_id`
- ← `attendance.attendance_register.course_id`
- ← `facility.room.default_course_id`
- ← `finance.course_fee_default.course_id`
- ← `library.media_course.course_id`
- ← `library.purchase_request_course.course_id`
- ← `sis.program_enrollment.course_id`
- ← `sis.subject_registration.course_id`
- ← `timetable.session.course_id`

<a id="academicscourse_subject"></a>
### academics.course_subject

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `course_id` | uuid | Y |  | FK → `academics.course` · CASCADE · tenant-composite |  |
| 4 | `subject_id` | uuid | Y |  | FK → `academics.subject` · RESTRICT · tenant-composite |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |
| 11 | `term_number` | int2 |  |  |  |  |
| 12 | `credits` | numeric(?,?) |  |  |  |  |
| 13 | `subject_category` | varchar(?) |  |  |  |  |
| 14 | `max_marks` | numeric(?,?) |  |  |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- UNIQUE (`id`, `subject_id`)
- unique index `uq_course_subject_slot` (`course_id`, `subject_id`, `COALESCE(term_number, 0)`) WHERE deleted_at IS NULL

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `sis.subject_registration_line.course_subject_id`

<a id="academicscurriculum_unit"></a>
### academics.curriculum_unit

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `class_subject_id` | uuid | Y |  | FK → `academics.class_subject` · RESTRICT · tenant-composite |  |
| 4 | `name` | varchar(?) | Y |  |  |  |
| 5 | `sequence` | int4 |  |  |  |  |
| 6 | `status` | varchar(?) | Y | 'active' |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_curriculum_unit_status`: status IN ('active', 'inactive')

**Relationships**

- ← `academics.lesson_plan.curriculum_unit_id` — referenced by (1:N)

<a id="academicsgrade_level"></a>
### academics.grade_level

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `name` | varchar(?) | Y |  |  |  |
| 5 | `slug` | varchar(?) | Y |  |  |  |
| 6 | `sort_order` | int4 |  |  |  |  |
| 7 | `is_active` | bool | Y | TRUE |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_grade_level_1` (`institution_id`, `slug`) WHERE deleted_at IS NULL

**Relationships**

- ← `academics.class_section.grade_level_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `admission.admission_cycle.grade_level_id`
- ← `admission.application.grade_level_id`
- ← `assessment.exam_session.grade_level_id`
- ← `finance.fee_structure.grade_level_id`
- ← `sis.promotion.from_grade_level_id`
- ← `sis.promotion.to_grade_level_id`

<a id="academicsidempotency_key"></a>
### academics.idempotency_key

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `idempotency_key` | uuid | Y |  |  |  |
| 4 | `user_id` | uuid |  |  |  |  |
| 5 | `operation_name` | varchar(?) | Y |  |  |  |
| 6 | `request_hash` | varchar(?) |  |  |  |  |
| 7 | `response_status` | int2 |  |  |  |  |
| 8 | `response_body` | text |  |  |  |  |
| 9 | `locked_at` | timestamptz |  |  |  |  |
| 10 | `completed_at` | timestamptz |  |  |  |  |
| 11 | `expires_at` | timestamptz | Y |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `idempotency_key`)

<a id="academicsinbox_event"></a>
### academics.inbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `event_id` | uuid | Y |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  | PK |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `received_at` | timestamptz | Y | now() |  |  |
| 5 | `processed_at` | timestamptz |  |  |  |  |
| 6 | `processing_error` | text |  |  |  |  |

<a id="academicslesson_plan"></a>
### academics.lesson_plan

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `teaching_assignment_id` | uuid | Y |  | FK → `academics.teaching_assignment` · RESTRICT · tenant-composite |  |
| 4 | `curriculum_unit_id` | uuid |  |  | FK → `academics.curriculum_unit` · SET NULL · tenant-composite |  |
| 5 | `unit_no` | varchar(?) | Y |  |  |  |
| 6 | `unit_name` | varchar(?) | Y |  |  |  |
| 7 | `title` | varchar(?) | Y |  |  |  |
| 8 | `planned_duration_minutes` | int4 | Y |  |  |  |
| 9 | `description` | text | Y |  |  |  |
| 10 | `objective` | text |  |  |  |  |
| 11 | `materials_required` | text |  |  |  |  |
| 12 | `introduction` | text |  |  |  |  |
| 13 | `procedure_text` | text |  |  |  |  |
| 14 | `conclusion` | text |  |  |  |  |
| 15 | `assessment_text` | text |  |  |  |  |
| 16 | `modification` | text |  |  |  |  |
| 17 | `notes` | text |  |  |  |  |
| 18 | `status` | varchar(?) | Y | 'draft' |  |  |
| 19 | `is_published` | bool | Y | FALSE |  |  |
| 20 | `published_at` | timestamptz |  |  |  |  |
| 21 | `start_date` | date |  |  |  |  |
| 22 | `end_date` | date |  |  |  |  |
| 23 | `created_at` | timestamptz | Y | now() |  |  |
| 24 | `created_by` | uuid |  |  |  |  |
| 25 | `updated_at` | timestamptz | Y | now() |  |  |
| 26 | `updated_by` | uuid |  |  |  |  |
| 27 | `deleted_at` | timestamptz |  |  |  |  |
| 28 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_lesson_plan_status`: status IN ('draft', 'pending', 'approved', 'rejected', 'archived', 'cancelled')

**Relationships**

- ← `academics.lesson_plan_approval.lesson_plan_id` — referenced by (1:N)

<a id="academicslesson_plan_approval"></a>
### academics.lesson_plan_approval

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `lesson_plan_id` | uuid | Y |  | FK → `academics.lesson_plan` · CASCADE · tenant-composite |  |
| 4 | `decision` | varchar(?) | Y | 'pending' |  |  |
| 5 | `comments` | text | Y |  |  |  |
| 6 | `decided_by_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 7 | `decided_at` | timestamptz |  |  |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_lesson_plan_approval_decision`: decision IN ('pending', 'approved', 'rejected')

<a id="academicsoperation"></a>
### academics.operation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `operation_name` | varchar(?) | Y |  |  |  |
| 4 | `resource_type` | varchar(?) |  |  |  |  |
| 5 | `resource_id` | uuid |  |  |  |  |
| 6 | `status` | varchar(?) | Y | 'running' |  |  |
| 7 | `progress_percent` | int2 |  |  |  |  |
| 8 | `error_code` | varchar(?) |  |  |  |  |
| 9 | `error_message` | varchar(?) |  |  |  |  |
| 10 | `result_entity_type` | varchar(?) |  |  |  |  |
| 11 | `result_entity_id` | uuid |  |  |  |  |
| 12 | `started_by_user_id` | uuid |  |  |  |  |
| 13 | `started_at` | timestamptz | Y | now() |  |  |
| 14 | `finished_at` | timestamptz |  |  |  |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_academics_operation_status`: status IN ('running', 'succeeded', 'failed', 'cancelled')

<a id="academicsoutbox_event"></a>
### academics.outbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | int8 |  |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `aggregate_type` | varchar(?) | Y |  |  |  |
| 5 | `aggregate_id` | uuid | Y |  |  |  |
| 6 | `payload` | text | Y |  |  |  |
| 7 | `occurred_at` | timestamptz | Y | now() |  |  |
| 8 | `published_at` | timestamptz |  |  |  |  |
| 9 | `publish_error` | text |  |  |  | Why the relay set the row aside (not published) |

<a id="academicsprogram"></a>
### academics.program

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `department_id` | uuid |  |  | logical → `org.department` (cross-module, no FK) |  |
| 5 | `program_level_id` | uuid | Y |  | FK → `academics.program_level` · RESTRICT · tenant-composite |  |
| 6 | `code` | varchar(?) | Y |  |  |  |
| 7 | `name` | varchar(?) | Y |  |  |  |
| 8 | `min_unit_load` | numeric(?,?) |  |  |  |  |
| 9 | `max_unit_load` | numeric(?,?) |  |  |  |  |
| 10 | `image_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 11 | `is_active` | bool | Y | TRUE |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_program_1` (`institution_id`, `code`) WHERE deleted_at IS NULL

**Relationships**

- ← `academics.course.program_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `admission.admission_cycle.program_id`
- ← `admission.application.program_id`
- ← `finance.fee_structure.program_id`
- ← `lms.cohort.program_id`

<a id="academicsprogram_level"></a>
### academics.program_level

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `name` | varchar(?) | Y |  |  |  |
| 4 | `created_at` | timestamptz | Y | now() |  |  |
| 5 | `created_by` | uuid |  |  |  |  |
| 6 | `updated_at` | timestamptz | Y | now() |  |  |
| 7 | `updated_by` | uuid |  |  |  |  |
| 8 | `deleted_at` | timestamptz |  |  |  |  |
| 9 | `row_version` | int4 | Y | 0 |  |  |
| 10 | `code` | varchar(?) | Y |  |  |  |
| 11 | `sort_order` | int4 | Y | 0 |  |  |
| 12 | `is_active` | bool | Y | TRUE |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_program_level_scope_code` (`tenant_id`, `code`) WHERE deleted_at IS NULL
- unique index `uq_program_level_1` (`tenant_id`, `name`) WHERE deleted_at IS NULL

**Relationships**

- ← `academics.program.program_level_id` — referenced by (1:N)

<a id="academicssection"></a>
### academics.section

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `name` | varchar(?) | Y |  |  |  |
| 5 | `is_active` | bool | Y | TRUE |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |
| 12 | `code` | varchar(?) | Y |  |  |  |
| 13 | `sort_order` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_section_scope_code` (`institution_id`, `code`) WHERE deleted_at IS NULL
- unique index `uq_section_1` (`institution_id`, `name`) WHERE deleted_at IS NULL

**Relationships**

- ← `academics.class_section.section_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `admission.application.section_id`
- ← `sis.promotion.from_section_id`
- ← `sis.promotion.to_section_id`

<a id="academicssubject"></a>
### academics.subject

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `department_id` | uuid |  |  | logical → `org.department` (cross-module, no FK) |  |
| 5 | `code` | varchar(?) |  |  |  |  |
| 6 | `name` | varchar(?) | Y |  |  |  |
| 7 | `delivery_type` | varchar(?) | Y | 'theory' |  |  |
| 8 | `default_category` | varchar(?) | Y | 'compulsory' |  |  |
| 9 | `grade_weightage` | numeric(?,?) |  |  |  |  |
| 10 | `is_active` | bool | Y | TRUE |  |  |
| 11 | `created_at` | timestamptz | Y | now() |  |  |
| 12 | `created_by` | uuid |  |  |  |  |
| 13 | `updated_at` | timestamptz | Y | now() |  |  |
| 14 | `updated_by` | uuid |  |  |  |  |
| 15 | `deleted_at` | timestamptz |  |  |  |  |
| 16 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `ix_subject_1` (`institution_id`, `code`) WHERE code IS NOT NULL AND deleted_at IS NULL

**Checks**

- `ck_subject_delivery_type`: delivery_type IN ('theory', 'practical', 'both', 'other')
- `ck_subject_default_category`: default_category IN ('compulsory', 'elective')

**Relationships**

- ← `academics.class_subject.subject_id` — referenced by (1:N)
- ← `academics.course_subject.subject_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `assessment.assignment_subject.subject_id`
- ← `assessment.exam.subject_id`
- ← `assessment.homework.subject_id`
- ← `attendance.attendance_register.subject_id`
- ← `academics.teacher_subject.subject_id`
- ← `library.media_subject.subject_id`
- ← `library.purchase_request_subject.subject_id`
- ← `sis.program_enrollment_subject.subject_id`
- ← `sis.subject_registration_line.subject_id`
- ← `timetable.session.subject_id`

<a id="academicsteacher_subject"></a>
### academics.teacher_subject

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `staff_id` | uuid | Y |  | logical → `hr.staff` (cross-module, no FK; archived on `hr.staff.deleted`) |  |
| 4 | `subject_id` | uuid | Y |  | FK → `academics.subject` · RESTRICT · tenant-composite |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_teacher_subject_1` (`staff_id`, `subject_id`) WHERE deleted_at IS NULL

<a id="academicsteaching_assignment"></a>
### academics.teaching_assignment

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `class_subject_id` | uuid | Y |  | FK → `academics.class_subject` · RESTRICT · tenant-composite |  |
| 4 | `staff_id` | uuid | Y |  | logical → `hr.staff` (cross-module, no FK) |  |
| 5 | `periods_per_week` | int4 |  |  |  |  |
| 6 | `periods_remaining` | int4 |  |  |  |  |
| 7 | `teacher_role` | varchar(?) |  |  |  |  |
| 8 | `is_active` | bool | Y | TRUE |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_teaching_assignment_1` (`class_subject_id`, `staff_id`) WHERE deleted_at IS NULL

**Relationships**

- ← `academics.lesson_plan.teaching_assignment_id` — referenced by (1:N)

## `sis` — Student Information

Student Information: Student and guardian records, placement and registration per year, promotions, health, siblings, conduct and co-curricular activity.

22 tables: [`activity_type`](#sisactivity_type), [`discipline_incident`](#sisdiscipline_incident), [`guardian`](#sisguardian), [`idempotency_key`](#sisidempotency_key), [`inbox_event`](#sisinbox_event), [`number_sequence`](#sisnumber_sequence), [`operation`](#sisoperation), [`outbox_event`](#sisoutbox_event), [`program_enrollment`](#sisprogram_enrollment), [`program_enrollment_subject`](#sisprogram_enrollment_subject), [`promotion`](#sispromotion), [`relationship_type`](#sisrelationship_type), [`student`](#sisstudent), [`student_activity`](#sisstudent_activity), [`student_category`](#sisstudent_category), [`student_enrollment`](#sisstudent_enrollment), [`student_guardian`](#sisstudent_guardian), [`student_health_record`](#sisstudent_health_record), [`student_leave_request`](#sisstudent_leave_request), [`student_sibling`](#sisstudent_sibling), [`subject_registration`](#sissubject_registration), [`subject_registration_line`](#sissubject_registration_line)

<a id="sisactivity_type"></a>
### sis.activity_type

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `name` | varchar(?) | Y |  |  |  |
| 4 | `is_active` | bool | Y | TRUE |  |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |
| 11 | `code` | varchar(?) | Y |  |  |  |
| 12 | `sort_order` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_activity_type_scope_code` (`tenant_id`, `code`) WHERE deleted_at IS NULL
- unique index `uq_activity_type_1` (`tenant_id`, `name`) WHERE deleted_at IS NULL

**Relationships**

- ← `sis.student_activity.activity_type_id` — referenced by (1:N)

<a id="sisdiscipline_incident"></a>
### sis.discipline_incident

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `academic_year_id` | uuid | Y |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `class_section_id` | uuid |  |  | logical → `academics.class_section` (cross-module, no FK) |  |
| 6 | `student_id` | uuid | Y |  | FK → `sis.student` · RESTRICT · tenant-composite |  |
| 7 | `incident_at` | timestamptz | Y |  |  |  |
| 8 | `reported_by_user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 9 | `incident_type` | varchar(?) | Y | 'discipline' |  |  |
| 10 | `details` | text | Y |  |  |  |
| 11 | `response` | text |  |  |  |  |
| 12 | `action_taken` | bool | Y | FALSE |  |  |
| 13 | `evidence_media_type` | varchar(?) |  |  |  |  |
| 14 | `evidence_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 15 | `notify_guardians` | bool | Y | FALSE |  |  |
| 16 | `seen_by_guardian` | bool | Y | FALSE |  |  |
| 17 | `created_at` | timestamptz | Y | now() |  |  |
| 18 | `created_by` | uuid |  |  |  |  |
| 19 | `updated_at` | timestamptz | Y | now() |  |  |
| 20 | `updated_by` | uuid |  |  |  |  |
| 21 | `deleted_at` | timestamptz |  |  |  |  |
| 22 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_discipline_incident_incident_type`: incident_type IN ('discipline', 'performance', 'other')
- `ck_discipline_incident_evidence_media_type`: evidence_media_type IN ('video', 'audio', 'image')

<a id="sisguardian"></a>
### sis.guardian

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `person_id` | uuid | Y |  | logical → `people.person` (cross-module, no FK) · UQ |  |
| 4 | `user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) · UQ |  |
| 5 | `profession` | varchar(?) |  |  |  |  |
| 6 | `sub_occupation` | varchar(?) |  |  |  |  |
| 7 | `designation` | varchar(?) |  |  |  |  |
| 8 | `organization_name` | varchar(?) |  |  |  |  |
| 9 | `annual_income` | numeric(?,?) |  |  |  |  |
| 10 | `qualification_id` | uuid |  |  | logical → `people.qualification` (cross-module, no FK) |  |
| 11 | `mobile` | varchar(?) |  |  |  |  |
| 12 | `email` | varchar(?) |  |  |  |  |
| 13 | `created_at` | timestamptz | Y | now() |  |  |
| 14 | `created_by` | uuid |  |  |  |  |
| 15 | `updated_at` | timestamptz | Y | now() |  |  |
| 16 | `updated_by` | uuid |  |  |  |  |
| 17 | `deleted_at` | timestamptz |  |  |  |  |
| 18 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_guardian_person_id` (`person_id`) WHERE deleted_at IS NULL
- unique index `uq_guardian_user_id` (`user_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_guardian_profession`: profession IN ('admin', 'business', 'central_government', 'state_government', 'private_sector', 'home_maker', 'teacher', 'librarian', 'other')

**Relationships**

- ← `sis.student_guardian.guardian_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `comms.feedback_thread.guardian_id`

<a id="sisidempotency_key"></a>
### sis.idempotency_key

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `idempotency_key` | uuid | Y |  |  |  |
| 4 | `user_id` | uuid |  |  |  |  |
| 5 | `operation_name` | varchar(?) | Y |  |  |  |
| 6 | `request_hash` | varchar(?) |  |  |  |  |
| 7 | `response_status` | int2 |  |  |  |  |
| 8 | `response_body` | text |  |  |  |  |
| 9 | `locked_at` | timestamptz |  |  |  |  |
| 10 | `completed_at` | timestamptz |  |  |  |  |
| 11 | `expires_at` | timestamptz | Y |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `idempotency_key`)

<a id="sisinbox_event"></a>
### sis.inbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `event_id` | uuid | Y |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  | PK |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `received_at` | timestamptz | Y | now() |  |  |
| 5 | `processed_at` | timestamptz |  |  |  |  |
| 6 | `processing_error` | text |  |  |  |  |

<a id="sisnumber_sequence"></a>
### sis.number_sequence

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `sequence_code` | varchar(?) | Y |  |  |  |
| 5 | `period_key` | varchar(?) | Y | '' |  |  |
| 6 | `prefix` | varchar(?) |  |  |  |  |
| 7 | `padding` | int2 | Y | 6 |  |  |
| 8 | `next_value` | int8 | Y | 1 |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `institution_id`, `sequence_code`, `period_key`) (NULLS NOT DISTINCT)

<a id="sisoperation"></a>
### sis.operation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `operation_name` | varchar(?) | Y |  |  |  |
| 4 | `resource_type` | varchar(?) |  |  |  |  |
| 5 | `resource_id` | uuid |  |  |  |  |
| 6 | `status` | varchar(?) | Y | 'running' |  |  |
| 7 | `progress_percent` | int2 |  |  |  |  |
| 8 | `error_code` | varchar(?) |  |  |  |  |
| 9 | `error_message` | varchar(?) |  |  |  |  |
| 10 | `result_entity_type` | varchar(?) |  |  |  |  |
| 11 | `result_entity_id` | uuid |  |  |  |  |
| 12 | `started_by_user_id` | uuid |  |  |  |  |
| 13 | `started_at` | timestamptz | Y | now() |  |  |
| 14 | `finished_at` | timestamptz |  |  |  |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_sis_operation_status`: status IN ('running', 'succeeded', 'failed', 'cancelled')

<a id="sisoutbox_event"></a>
### sis.outbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | int8 |  |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `aggregate_type` | varchar(?) | Y |  |  |  |
| 5 | `aggregate_id` | uuid | Y |  |  |  |
| 6 | `payload` | text | Y |  |  |  |
| 7 | `occurred_at` | timestamptz | Y | now() |  |  |
| 8 | `published_at` | timestamptz |  |  |  |  |
| 9 | `publish_error` | text |  |  |  | Why the relay set the row aside (not published) |

<a id="sisprogram_enrollment"></a>
### sis.program_enrollment

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `student_id` | uuid | Y |  | FK → `sis.student` · RESTRICT · tenant-composite |  |
| 4 | `course_id` | uuid | Y |  | logical → `academics.course` (cross-module, no FK) |  |
| 5 | `batch_id` | uuid |  |  | logical → `academics.batch` (cross-module, no FK) |  |
| 6 | `academic_year_id` | uuid |  |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 7 | `academic_term_id` | uuid |  |  | logical → `org.academic_term` (cross-module, no FK) |  |
| 8 | `roll_number` | varchar(?) |  |  |  |  |
| 9 | `status` | varchar(?) | Y | 'running' |  |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `ix_program_enrollment_1` (`course_id`, `batch_id`, `roll_number`) WHERE roll_number IS NOT NULL AND deleted_at IS NULL
- unique index `uq_program_enrollment_1` (`student_id`, `course_id`, `batch_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_program_enrollment_status`: status IN ('running', 'finished', 'dropped')

**Relationships**

- ← `sis.program_enrollment_subject.program_enrollment_id` — referenced by (1:N)
- ← `sis.subject_registration.program_enrollment_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `finance.program_enrollment_fee.program_enrollment_id`
- ← `finance.student_fee_due.program_enrollment_id`

<a id="sisprogram_enrollment_subject"></a>
### sis.program_enrollment_subject

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `program_enrollment_id` | uuid | Y |  | FK → `sis.program_enrollment` · CASCADE · tenant-composite |  |
| 4 | `subject_id` | uuid | Y |  | logical → `academics.subject` (cross-module, no FK) |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_program_enrollment_subject_1` (`program_enrollment_id`, `subject_id`) WHERE deleted_at IS NULL

<a id="sispromotion"></a>
### sis.promotion

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `student_id` | uuid | Y |  | FK → `sis.student` · RESTRICT · tenant-composite |  |
| 4 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 5 | `from_academic_year_id` | uuid | Y |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 6 | `from_grade_level_id` | uuid | Y |  | logical → `academics.grade_level` (cross-module, no FK) |  |
| 7 | `from_section_id` | uuid |  |  | logical → `academics.section` (cross-module, no FK) |  |
| 8 | `to_academic_year_id` | uuid |  |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 9 | `to_grade_level_id` | uuid |  |  | logical → `academics.grade_level` (cross-module, no FK) |  |
| 10 | `to_section_id` | uuid |  |  | logical → `academics.section` (cross-module, no FK) |  |
| 11 | `based_on_exam_session_id` | uuid |  |  | logical → `assessment.exam_session` (cross-module, no FK) |  |
| 12 | `remarks` | text |  |  |  |  |
| 13 | `status` | varchar(?) | Y | 'proposed' |  |  |
| 14 | `created_at` | timestamptz | Y | now() |  |  |
| 15 | `created_by` | uuid |  |  |  |  |
| 16 | `updated_at` | timestamptz | Y | now() |  |  |
| 17 | `updated_by` | uuid |  |  |  |  |
| 18 | `deleted_at` | timestamptz |  |  |  |  |
| 19 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_promotion_once` (`tenant_id`, `student_id`, `from_academic_year_id`) WHERE status <> 'rejected' AND deleted_at IS NULL

**Checks**

- `ck_promotion_status`: status IN ('proposed', 'approved', 'applied', 'rejected')

<a id="sisrelationship_type"></a>
### sis.relationship_type

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `name` | varchar(?) | Y |  |  |  |
| 4 | `created_at` | timestamptz | Y | now() |  |  |
| 5 | `created_by` | uuid |  |  |  |  |
| 6 | `updated_at` | timestamptz | Y | now() |  |  |
| 7 | `updated_by` | uuid |  |  |  |  |
| 8 | `deleted_at` | timestamptz |  |  |  |  |
| 9 | `row_version` | int4 | Y | 0 |  |  |
| 10 | `code` | varchar(?) | Y |  |  |  |
| 11 | `sort_order` | int4 | Y | 0 |  |  |
| 12 | `is_active` | bool | Y | TRUE |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_relationship_type_scope_code` (`tenant_id`, `code`) WHERE deleted_at IS NULL
- unique index `uq_relationship_type_1` (`tenant_id`, `name`) WHERE deleted_at IS NULL

**Relationships**

- ← `sis.student_guardian.relationship_type_id` — referenced by (1:N)

<a id="sisstudent"></a>
### sis.student

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `person_id` | uuid | Y |  | logical → `people.person` (cross-module, no FK) |  |
| 4 | `user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 6 | `admission_number` | varchar(?) |  |  |  |  |
| 7 | `admission_date` | date |  |  |  |  |
| 8 | `joining_date` | date |  |  |  |  |
| 9 | `student_category_id` | uuid |  |  | FK → `sis.student_category` · SET NULL · tenant-composite |  |
| 10 | `government_student_id` | varchar(?) |  |  |  |  |
| 11 | `bonafide_certificate_number` | varchar(?) |  |  |  |  |
| 12 | `status` | varchar(?) | Y | 'active' |  |  |
| 13 | `exit_date` | date |  |  |  |  |
| 14 | `exit_reason` | text |  |  |  |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `ix_student_1` (`institution_id`, `admission_number`) WHERE admission_number IS NOT NULL AND deleted_at IS NULL
- unique index `uq_student_1` (`institution_id`, `person_id`) WHERE deleted_at IS NULL
- unique index `uq_student_2` (`institution_id`, `user_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_student_status`: status IN ('active', 'inactive', 'alumni', 'withdrawn', 'transferred', 'graduated')

**Relationships**

- ← `sis.discipline_incident.student_id` — referenced by (1:N)
- ← `sis.program_enrollment.student_id` — referenced by (1:N)
- ← `sis.promotion.student_id` — referenced by (1:N)
- ← `sis.student_activity.student_id` — referenced by (1:N)
- ← `sis.student_enrollment.student_id` — referenced by (1:N)
- ← `sis.student_guardian.student_id` — referenced by (1:N)
- ← `sis.student_health_record.student_id` — referenced by (1:N)
- ← `sis.student_leave_request.student_id` — referenced by (1:N)
- ← `sis.student_sibling.sibling_student_id` — referenced by (1:N)
- ← `sis.student_sibling.student_id` — referenced by (1:N)
- ← `sis.subject_registration.student_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `admission.application.student_id`
- ← `assessment.assignment_allocation.student_id`
- ← `assessment.assignment_submission.student_id`
- ← `assessment.exam_result.student_id`
- ← `assessment.homework_submission.student_id`
- ← `assessment.marksheet_line.student_id`
- ← `attendance.attendance_record.student_id`
- ← `comms.content_view.student_id`
- ← `comms.feedback_thread.student_id`
- ← `comms.mailbox_message.about_student_id`
- ← `finance.fee_concession.student_id`
- ← `finance.invoice.student_id`
- ← `finance.student_fee_due.student_id`
- ← `frontoffice.visitor_log.student_id`
- ← `library.library_card.student_id`
- ← `timetable.session_student.student_id`
- ← `transport.student_transport.student_id`

<a id="sisstudent_activity"></a>
### sis.student_activity

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `student_id` | uuid | Y |  | FK → `sis.student` · RESTRICT · tenant-composite |  |
| 4 | `staff_id` | uuid |  |  | logical → `hr.staff` (cross-module, no FK) |  |
| 5 | `activity_type_id` | uuid |  |  | FK → `sis.activity_type` · RESTRICT · tenant-composite |  |
| 6 | `activity_date` | date |  |  |  |  |
| 7 | `description` | text |  |  |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

<a id="sisstudent_category"></a>
### sis.student_category

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `code` | varchar(?) | Y |  |  |  |
| 5 | `name` | varchar(?) | Y |  |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |
| 12 | `sort_order` | int4 | Y | 0 |  |  |
| 13 | `is_active` | bool | Y | TRUE |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_student_category_scope_code` (`institution_id`, `code`) WHERE deleted_at IS NULL

**Relationships**

- ← `sis.student.student_category_id` — referenced by (1:N)

<a id="sisstudent_enrollment"></a>
### sis.student_enrollment

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `student_id` | uuid | Y |  | FK → `sis.student` · RESTRICT · tenant-composite |  |
| 4 | `academic_year_id` | uuid | Y |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `class_section_id` | uuid |  |  | logical → `academics.class_section` (cross-module, no FK) |  |
| 6 | `roll_number` | varchar(?) |  |  |  |  |
| 7 | `id_card_number` | varchar(?) |  |  |  |  |
| 8 | `board_registration_number` | varchar(?) |  |  |  |  |
| 9 | `enrollment_status` | varchar(?) | Y | 'enrolled' |  |  |
| 10 | `result_status` | varchar(?) | Y | 'pending' |  |  |
| 11 | `enrolled_on` | date |  |  |  |  |
| 12 | `left_on` | date |  |  |  |  |
| 13 | `created_at` | timestamptz | Y | now() |  |  |
| 14 | `created_by` | uuid |  |  |  |  |
| 15 | `updated_at` | timestamptz | Y | now() |  |  |
| 16 | `updated_by` | uuid |  |  |  |  |
| 17 | `deleted_at` | timestamptz |  |  |  |  |
| 18 | `row_version` | int4 | Y | 0 |  |  |
| 19 | `effective_from` | date |  |  |  |  |
| 20 | `effective_to` | date |  |  |  |  |
| 21 | `previous_enrollment_id` | uuid |  |  | FK → `sis.student_enrollment` · SET NULL · tenant-composite |  |
| 22 | `change_reason` | varchar(?) | Y | 'initial' |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `ix_student_enrollment_1` (`class_section_id`, `roll_number`) WHERE roll_number IS NOT NULL AND deleted_at IS NULL
- unique index `uq_student_enrollment_open` (`tenant_id`, `student_id`, `academic_year_id`) WHERE deleted_at IS NULL AND effective_to IS NULL

**Checks**

- `ck_student_enrollment_enrollment_status`: enrollment_status IN ('enrolled', 'promoted', 'detained', 'left', 'completed')
- `ck_student_enrollment_result_status`: result_status IN ('pending', 'pass', 'fail')
- `ck_student_enrollment_change_reason`: change_reason IN ('initial', 'promotion', 'transfer_in', 'section_change', 're_admission', 'correction')
- `ck_student_enrollment_effective`: effective_to IS NULL OR effective_from IS NULL OR effective_to >= effective_from

**Relationships**

- ← `sis.student_enrollment.previous_enrollment_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `finance.student_fee_due.student_enrollment_id`

<a id="sisstudent_guardian"></a>
### sis.student_guardian

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `student_id` | uuid | Y |  | FK → `sis.student` · CASCADE · tenant-composite |  |
| 4 | `guardian_id` | uuid | Y |  | FK → `sis.guardian` · CASCADE · tenant-composite |  |
| 5 | `relationship_type_id` | uuid |  |  | FK → `sis.relationship_type` · RESTRICT · tenant-composite |  |
| 6 | `is_primary_contact` | bool | Y | FALSE |  |  |
| 7 | `receives_communication` | bool | Y | TRUE |  |  |
| 8 | `can_pick_up` | bool | Y | TRUE |  |  |
| 9 | `status` | varchar(?) | Y | 'active' |  |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |
| 16 | `is_legal_guardian` | bool | Y | FALSE |  |  |
| 17 | `has_custody` | bool | Y | TRUE |  |  |

**Uniqueness**

- unique index `uq_student_guardian_1` (`student_id`, `guardian_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_student_guardian_status`: status IN ('active', 'inactive')

<a id="sisstudent_health_record"></a>
### sis.student_health_record

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `student_id` | uuid | Y |  | FK → `sis.student` · RESTRICT · tenant-composite |  |
| 4 | `academic_year_id` | uuid |  |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `height_cm` | numeric(?,?) |  |  |  |  |
| 6 | `weight_kg` | numeric(?,?) |  |  |  |  |
| 7 | `medical_conditions` | text |  |  |  |  |
| 8 | `medication_needs` | text |  |  |  |  |
| 9 | `medication_allergies` | text |  |  |  |  |
| 10 | `food_allergies` | text |  |  |  |  |
| 11 | `other_allergies` | text |  |  |  |  |
| 12 | `other_medical_info` | text |  |  |  |  |
| 13 | `created_at` | timestamptz | Y | now() |  |  |
| 14 | `created_by` | uuid |  |  |  |  |
| 15 | `updated_at` | timestamptz | Y | now() |  |  |
| 16 | `updated_by` | uuid |  |  |  |  |
| 17 | `deleted_at` | timestamptz |  |  |  |  |
| 18 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_student_health_record_1` (`student_id`, `academic_year_id`) WHERE deleted_at IS NULL

<a id="sisstudent_leave_request"></a>
### sis.student_leave_request

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `student_id` | uuid | Y |  | FK → `sis.student` · CASCADE · tenant-composite |  |
| 4 | `academic_year_id` | uuid |  |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `from_date` | date | Y |  |  |  |
| 6 | `to_date` | date | Y |  |  |  |
| 7 | `day_part` | varchar(?) | Y | 'full_day' |  |  |
| 8 | `reason` | varchar(?) |  |  |  |  |
| 9 | `status` | varchar(?) | Y | 'pending' |  |  |
| 10 | `requested_by_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 11 | `decided_by_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 12 | `decided_at` | timestamptz |  |  |  |  |
| 13 | `decision_note` | varchar(?) |  |  |  |  |
| 14 | `created_at` | timestamptz | Y | now() |  |  |
| 15 | `created_by` | uuid |  |  |  |  |
| 16 | `updated_at` | timestamptz | Y | now() |  |  |
| 17 | `updated_by` | uuid |  |  |  |  |
| 18 | `deleted_at` | timestamptz |  |  |  |  |
| 19 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_student_leave_request_status`: status IN ('pending', 'approved', 'rejected', 'cancelled')
- `ck_student_leave_request_day_part`: day_part IN ('full_day', 'first_half', 'second_half')
- `ck_student_leave_request_dates`: to_date >= from_date

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `attendance.attendance_record.leave_request_id`

<a id="sisstudent_sibling"></a>
### sis.student_sibling

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `student_id` | uuid | Y |  | FK → `sis.student` · CASCADE · tenant-composite |  |
| 4 | `sibling_student_id` | uuid |  |  | FK → `sis.student` · SET NULL · tenant-composite |  |
| 5 | `sibling_name` | varchar(?) |  |  |  |  |
| 6 | `details` | text |  |  |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_student_sibling_pair` (`student_id`, `sibling_student_id`) WHERE sibling_student_id IS NOT NULL AND deleted_at IS NULL

**Checks**

- `ck_student_sibling_not_self`: sibling_student_id IS NULL OR sibling_student_id <> student_id

<a id="sissubject_registration"></a>
### sis.subject_registration

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `student_id` | uuid | Y |  | FK → `sis.student` · RESTRICT · tenant-composite |  |
| 4 | `course_id` | uuid | Y |  | logical → `academics.course` (cross-module, no FK) |  |
| 5 | `batch_id` | uuid |  |  | logical → `academics.batch` (cross-module, no FK) |  |
| 6 | `reference_no` | varchar(?) |  |  |  |  |
| 7 | `status` | varchar(?) | Y | 'draft' |  |  |
| 8 | `min_unit_load` | numeric(?,?) |  |  |  |  |
| 9 | `max_unit_load` | numeric(?,?) |  |  |  |  |
| 10 | `read_by_student` | bool | Y | FALSE |  |  |
| 11 | `read_by_guardian` | bool | Y | FALSE |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |
| 18 | `academic_year_id` | uuid | Y |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 19 | `academic_term_id` | uuid |  |  | logical → `org.academic_term` (cross-module, no FK) |  |
| 20 | `program_enrollment_id` | uuid | Y |  | FK → `sis.program_enrollment` · RESTRICT · tenant-composite |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_subject_registration_slot` (`tenant_id`, `program_enrollment_id`, `academic_year_id`, `academic_term_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_subject_registration_status`: status IN ('draft', 'submitted', 'approved', 'rejected')

**Relationships**

- ← `sis.subject_registration_line.subject_registration_id` — referenced by (1:N)

<a id="sissubject_registration_line"></a>
### sis.subject_registration_line

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `subject_registration_id` | uuid | Y |  | FK → `sis.subject_registration` · CASCADE · tenant-composite |  |
| 4 | `subject_id` | uuid | Y |  | logical → `academics.subject` (cross-module, no FK) |  |
| 5 | `selection_type` | varchar(?) | Y |  |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |
| 12 | `course_subject_id` | uuid | Y |  | logical → `academics.course_subject` (cross-module, no FK) |  |

**Uniqueness**

- unique index `uq_subject_registration_line_1` (`subject_registration_id`, `subject_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_subject_registration_line_selection_type`: selection_type IN ('compulsory', 'elective')

## `hr` — Workforce / HR

Workforce / HR: Staff records, designations, departments, subject competencies, leave types and leave applications.

11 tables: [`designation`](#hrdesignation), [`idempotency_key`](#hridempotency_key), [`inbox_event`](#hrinbox_event), [`leave_application`](#hrleave_application), [`leave_balance`](#hrleave_balance), [`leave_type`](#hrleave_type), [`number_sequence`](#hrnumber_sequence), [`operation`](#hroperation), [`outbox_event`](#hroutbox_event), [`staff`](#hrstaff), [`staff_department`](#hrstaff_department)

<a id="hrdesignation"></a>
### hr.designation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `code` | varchar(?) | Y |  |  |  |
| 4 | `name` | varchar(?) | Y |  |  |  |
| 5 | `staff_category` | varchar(?) | Y |  |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |
| 12 | `sort_order` | int4 | Y | 0 |  |  |
| 13 | `is_active` | bool | Y | TRUE |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_designation_scope_code` (`tenant_id`, `code`) WHERE deleted_at IS NULL

**Checks**

- `ck_designation_staff_category`: staff_category IN ('teaching', 'non_teaching')

**Relationships**

- ← `hr.staff.designation_id` — referenced by (1:N)

<a id="hridempotency_key"></a>
### hr.idempotency_key

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `idempotency_key` | uuid | Y |  |  |  |
| 4 | `user_id` | uuid |  |  |  |  |
| 5 | `operation_name` | varchar(?) | Y |  |  |  |
| 6 | `request_hash` | varchar(?) |  |  |  |  |
| 7 | `response_status` | int2 |  |  |  |  |
| 8 | `response_body` | text |  |  |  |  |
| 9 | `locked_at` | timestamptz |  |  |  |  |
| 10 | `completed_at` | timestamptz |  |  |  |  |
| 11 | `expires_at` | timestamptz | Y |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `idempotency_key`)

<a id="hrinbox_event"></a>
### hr.inbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `event_id` | uuid | Y |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  | PK |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `received_at` | timestamptz | Y | now() |  |  |
| 5 | `processed_at` | timestamptz |  |  |  |  |
| 6 | `processing_error` | text |  |  |  |  |

<a id="hrleave_application"></a>
### hr.leave_application

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `academic_year_id` | uuid | Y |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `staff_id` | uuid | Y |  | FK → `hr.staff` · RESTRICT · tenant-composite |  |
| 6 | `leave_type_id` | uuid |  |  | FK → `hr.leave_type` · RESTRICT · tenant-composite |  |
| 7 | `absence_reason_id` | uuid |  |  | logical → `attendance.absence_reason` (cross-module, no FK) |  |
| 8 | `class_section_id` | uuid |  |  | logical → `academics.class_section` (cross-module, no FK) |  |
| 9 | `from_at` | timestamptz | Y |  |  |  |
| 10 | `to_at` | timestamptz | Y |  |  |  |
| 11 | `day_part` | varchar(?) | Y | 'full_day' |  |  |
| 12 | `remarks` | text |  |  |  |  |
| 13 | `status` | varchar(?) | Y | 'pending' |  |  |
| 14 | `decided_by_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 15 | `decided_on` | date |  |  |  |  |
| 16 | `decision_comments` | text |  |  |  |  |
| 17 | `created_at` | timestamptz | Y | now() |  |  |
| 18 | `created_by` | uuid |  |  |  |  |
| 19 | `updated_at` | timestamptz | Y | now() |  |  |
| 20 | `updated_by` | uuid |  |  |  |  |
| 21 | `deleted_at` | timestamptz |  |  |  |  |
| 22 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_leave_application_day_part`: day_part IN ('full_day', 'forenoon', 'afternoon')
- `ck_leave_application_status`: status IN ('pending', 'approved', 'rejected', 'cancelled', 'completed')
- `ck_leave_application_1`: to_at >= from_at

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `attendance.staff_attendance.leave_application_id`

<a id="hrleave_balance"></a>
### hr.leave_balance

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `staff_id` | uuid | Y |  | FK → `hr.staff` · CASCADE · tenant-composite |  |
| 4 | `leave_type_id` | uuid | Y |  | FK → `hr.leave_type` · RESTRICT · tenant-composite |  |
| 5 | `academic_year_id` | uuid |  |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 6 | `entitled_days` | numeric(?,?) | Y |  |  |  |
| 7 | `used_days` | numeric(?,?) | Y | 0 |  |  |
| 8 | `carried_forward_days` | numeric(?,?) | Y | 0 |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_leave_balance_1` (`staff_id`, `leave_type_id`, `academic_year_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_leave_balance_rule`: entitled_days >= 0 AND used_days >= 0 AND carried_forward_days >= 0

<a id="hrleave_type"></a>
### hr.leave_type

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `academic_year_id` | uuid |  |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `name` | varchar(?) | Y |  |  |  |
| 6 | `max_days_per_year` | numeric(?,?) | Y |  |  |  |
| 7 | `is_active` | bool | Y | TRUE |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |
| 14 | `code` | varchar(?) | Y |  |  |  |
| 15 | `sort_order` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_leave_type_scope_code` (`institution_id`, `code`) WHERE deleted_at IS NULL

**Relationships**

- ← `hr.leave_application.leave_type_id` — referenced by (1:N)
- ← `hr.leave_balance.leave_type_id` — referenced by (1:N)

<a id="hrnumber_sequence"></a>
### hr.number_sequence

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `sequence_code` | varchar(?) | Y |  |  |  |
| 5 | `period_key` | varchar(?) | Y | '' |  |  |
| 6 | `prefix` | varchar(?) |  |  |  |  |
| 7 | `padding` | int2 | Y | 6 |  |  |
| 8 | `next_value` | int8 | Y | 1 |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `institution_id`, `sequence_code`, `period_key`) (NULLS NOT DISTINCT)

<a id="hroperation"></a>
### hr.operation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `operation_name` | varchar(?) | Y |  |  |  |
| 4 | `resource_type` | varchar(?) |  |  |  |  |
| 5 | `resource_id` | uuid |  |  |  |  |
| 6 | `status` | varchar(?) | Y | 'running' |  |  |
| 7 | `progress_percent` | int2 |  |  |  |  |
| 8 | `error_code` | varchar(?) |  |  |  |  |
| 9 | `error_message` | varchar(?) |  |  |  |  |
| 10 | `result_entity_type` | varchar(?) |  |  |  |  |
| 11 | `result_entity_id` | uuid |  |  |  |  |
| 12 | `started_by_user_id` | uuid |  |  |  |  |
| 13 | `started_at` | timestamptz | Y | now() |  |  |
| 14 | `finished_at` | timestamptz |  |  |  |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_hr_operation_status`: status IN ('running', 'succeeded', 'failed', 'cancelled')

<a id="hroutbox_event"></a>
### hr.outbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | int8 |  |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `aggregate_type` | varchar(?) | Y |  |  |  |
| 5 | `aggregate_id` | uuid | Y |  |  |  |
| 6 | `payload` | text | Y |  |  |  |
| 7 | `occurred_at` | timestamptz | Y | now() |  |  |
| 8 | `published_at` | timestamptz |  |  |  |  |
| 9 | `publish_error` | text |  |  |  | Why the relay set the row aside (not published) |

<a id="hrstaff"></a>
### hr.staff

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `person_id` | uuid | Y |  | logical → `people.person` (cross-module, no FK) |  |
| 4 | `user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 6 | `employee_code` | varchar(?) |  |  |  |  |
| 7 | `designation_id` | uuid |  |  | FK → `hr.designation` · RESTRICT · tenant-composite |  |
| 8 | `sub_designation` | varchar(?) |  |  |  |  |
| 9 | `employment_type` | varchar(?) | Y | 'full_time' |  |  |
| 10 | `joining_date` | date |  |  |  |  |
| 11 | `reporting_to_staff_id` | uuid |  |  | FK → `hr.staff` · SET NULL · tenant-composite |  |
| 12 | `primary_department_id` | uuid |  |  | logical → `org.department` (cross-module, no FK) |  |
| 13 | `highest_qualification_id` | uuid |  |  | logical → `people.qualification` (cross-module, no FK) |  |
| 14 | `qualification_detail` | varchar(?) |  |  |  |  |
| 15 | `specialization` | varchar(?) |  |  |  |  |
| 16 | `interests` | text |  |  |  |  |
| 17 | `max_periods_per_week` | int4 |  |  |  |  |
| 18 | `status` | varchar(?) | Y | 'active' |  |  |
| 19 | `created_at` | timestamptz | Y | now() |  |  |
| 20 | `created_by` | uuid |  |  |  |  |
| 21 | `updated_at` | timestamptz | Y | now() |  |  |
| 22 | `updated_by` | uuid |  |  |  |  |
| 23 | `deleted_at` | timestamptz |  |  |  |  |
| 24 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `ix_staff_1` (`institution_id`, `employee_code`) WHERE employee_code IS NOT NULL AND deleted_at IS NULL
- unique index `uq_staff_1` (`institution_id`, `person_id`) WHERE deleted_at IS NULL
- unique index `uq_staff_2` (`institution_id`, `user_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_staff_employment_type`: employment_type IN ('full_time', 'part_time', 'contract', 'visiting')
- `ck_staff_status`: status IN ('active', 'inactive', 'exited')

**Relationships**

- ← `hr.leave_application.staff_id` — referenced by (1:N)
- ← `hr.leave_balance.staff_id` — referenced by (1:N)
- ← `hr.staff.reporting_to_staff_id` — referenced by (1:N)
- ← `hr.staff_department.staff_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `academics.class_section.class_teacher_staff_id`
- ← `academics.teaching_assignment.staff_id`
- ← `assessment.assignment.assigned_by_staff_id`
- ← `assessment.assignment.reviewer_staff_id`
- ← `assessment.exam_invigilator.staff_id`
- ← `assessment.homework.assigned_by_staff_id`
- ← `attendance.attendance_sheet.taken_by_staff_id`
- ← `attendance.staff_attendance.staff_id`
- ← `frontoffice.visitor_log.host_staff_id`
- ← `library.library_card.staff_id`
- ← `payroll.payroll_payment.staff_id`
- ← `payroll.payslip.staff_id`
- ← `payroll.staff_bank_account.staff_id`
- ← `payroll.staff_salary.staff_id`
- ← `sis.student_activity.staff_id`
- ← `timetable.session.staff_id`

<a id="hrstaff_department"></a>
### hr.staff_department

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `staff_id` | uuid | Y |  | FK → `hr.staff` · CASCADE · tenant-composite |  |
| 4 | `department_id` | uuid | Y |  | logical → `org.department` (cross-module, no FK) |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_staff_department_1` (`staff_id`, `department_id`) WHERE deleted_at IS NULL

## `payroll` — Payroll

Payroll: Pay components, salary templates, staff salary structures, payslips and salary disbursements.

17 tables: [`disbursement_account`](#payrolldisbursement_account), [`idempotency_key`](#payrollidempotency_key), [`inbox_event`](#payrollinbox_event), [`number_sequence`](#payrollnumber_sequence), [`operation`](#payrolloperation), [`outbox_event`](#payrolloutbox_event), [`pay_category`](#payrollpay_category), [`pay_component`](#payrollpay_component), [`payment_type`](#payrollpayment_type), [`payroll_payment`](#payrollpayroll_payment), [`payslip`](#payrollpayslip), [`payslip_line`](#payrollpayslip_line), [`salary_template`](#payrollsalary_template), [`salary_template_component`](#payrollsalary_template_component), [`staff_bank_account`](#payrollstaff_bank_account), [`staff_salary`](#payrollstaff_salary), [`staff_salary_component`](#payrollstaff_salary_component)

<a id="payrolldisbursement_account"></a>
### payroll.disbursement_account

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `managed_by_user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `name` | varchar(?) | Y |  |  |  |
| 6 | `account_key` | varchar(?) | Y |  |  |  |
| 7 | `account_number_enc` | text | Y |  |  |  |
| 8 | `bank_routing_code` | varchar(?) | Y |  |  |  |
| 9 | `branch_name` | varchar(?) |  |  |  |  |
| 10 | `extra_reference` | varchar(?) |  |  |  |  |
| 11 | `created_at` | timestamptz | Y | now() |  |  |
| 12 | `created_by` | uuid |  |  |  |  |
| 13 | `updated_at` | timestamptz | Y | now() |  |  |
| 14 | `updated_by` | uuid |  |  |  |  |
| 15 | `deleted_at` | timestamptz |  |  |  |  |
| 16 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_disbursement_account_1` (`institution_id`, `account_key`) WHERE deleted_at IS NULL

**Relationships**

- ← `payroll.payroll_payment.disbursement_account_id` — referenced by (1:N)

<a id="payrollidempotency_key"></a>
### payroll.idempotency_key

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `idempotency_key` | uuid | Y |  |  |  |
| 4 | `user_id` | uuid |  |  |  |  |
| 5 | `operation_name` | varchar(?) | Y |  |  |  |
| 6 | `request_hash` | varchar(?) |  |  |  |  |
| 7 | `response_status` | int2 |  |  |  |  |
| 8 | `response_body` | text |  |  |  |  |
| 9 | `locked_at` | timestamptz |  |  |  |  |
| 10 | `completed_at` | timestamptz |  |  |  |  |
| 11 | `expires_at` | timestamptz | Y |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `idempotency_key`)

<a id="payrollinbox_event"></a>
### payroll.inbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `event_id` | uuid | Y |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  | PK |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `received_at` | timestamptz | Y | now() |  |  |
| 5 | `processed_at` | timestamptz |  |  |  |  |
| 6 | `processing_error` | text |  |  |  |  |

<a id="payrollnumber_sequence"></a>
### payroll.number_sequence

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `sequence_code` | varchar(?) | Y |  |  |  |
| 5 | `period_key` | varchar(?) | Y | '' |  |  |
| 6 | `prefix` | varchar(?) |  |  |  |  |
| 7 | `padding` | int2 | Y | 6 |  |  |
| 8 | `next_value` | int8 | Y | 1 |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `institution_id`, `sequence_code`, `period_key`) (NULLS NOT DISTINCT)

<a id="payrolloperation"></a>
### payroll.operation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `operation_name` | varchar(?) | Y |  |  |  |
| 4 | `resource_type` | varchar(?) |  |  |  |  |
| 5 | `resource_id` | uuid |  |  |  |  |
| 6 | `status` | varchar(?) | Y | 'running' |  |  |
| 7 | `progress_percent` | int2 |  |  |  |  |
| 8 | `error_code` | varchar(?) |  |  |  |  |
| 9 | `error_message` | varchar(?) |  |  |  |  |
| 10 | `result_entity_type` | varchar(?) |  |  |  |  |
| 11 | `result_entity_id` | uuid |  |  |  |  |
| 12 | `started_by_user_id` | uuid |  |  |  |  |
| 13 | `started_at` | timestamptz | Y | now() |  |  |
| 14 | `finished_at` | timestamptz |  |  |  |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |
| 21 | `result` | text |  |  |  | Outcome of a finished operation as a JSON object (counts, skipped items); NULL while it runs and when it has none. |

**Checks**

- `ck_payroll_operation_status`: status IN ('running', 'succeeded', 'failed', 'cancelled')

<a id="payrolloutbox_event"></a>
### payroll.outbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | int8 |  |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `aggregate_type` | varchar(?) | Y |  |  |  |
| 5 | `aggregate_id` | uuid | Y |  |  |  |
| 6 | `payload` | text | Y |  |  |  |
| 7 | `occurred_at` | timestamptz | Y | now() |  |  |
| 8 | `published_at` | timestamptz |  |  |  |  |
| 9 | `publish_error` | text |  |  |  | Why the relay set the row aside (not published) |

<a id="payrollpay_category"></a>
### payroll.pay_category

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `name` | varchar(?) | Y |  |  |  |
| 4 | `created_at` | timestamptz | Y | now() |  |  |
| 5 | `created_by` | uuid |  |  |  |  |
| 6 | `updated_at` | timestamptz | Y | now() |  |  |
| 7 | `updated_by` | uuid |  |  |  |  |
| 8 | `deleted_at` | timestamptz |  |  |  |  |
| 9 | `row_version` | int4 | Y | 0 |  |  |
| 10 | `code` | varchar(?) | Y |  |  |  |
| 11 | `sort_order` | int4 | Y | 0 |  |  |
| 12 | `is_active` | bool | Y | TRUE |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_pay_category_scope_code` (`tenant_id`, `code`) WHERE deleted_at IS NULL
- unique index `uq_pay_category_1` (`tenant_id`, `name`) WHERE deleted_at IS NULL

**Relationships**

- ← `payroll.salary_template_component.pay_category_id` — referenced by (1:N)

<a id="payrollpay_component"></a>
### payroll.pay_component

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `code` | varchar(?) | Y |  |  |  |
| 4 | `name` | varchar(?) | Y |  |  |  |
| 5 | `component_type` | varchar(?) | Y |  |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |
| 12 | `sort_order` | int4 | Y | 0 |  |  |
| 13 | `is_active` | bool | Y | TRUE |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_pay_component_scope_code` (`tenant_id`, `code`) WHERE deleted_at IS NULL

**Checks**

- `ck_pay_component_component_type`: component_type IN ('earning', 'deduction', 'defined')

**Relationships**

- ← `payroll.salary_template_component.pay_component_id` — referenced by (1:N)

<a id="payrollpayment_type"></a>
### payroll.payment_type

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `name` | varchar(?) | Y |  |  |  |
| 4 | `created_at` | timestamptz | Y | now() |  |  |
| 5 | `created_by` | uuid |  |  |  |  |
| 6 | `updated_at` | timestamptz | Y | now() |  |  |
| 7 | `updated_by` | uuid |  |  |  |  |
| 8 | `deleted_at` | timestamptz |  |  |  |  |
| 9 | `row_version` | int4 | Y | 0 |  |  |
| 10 | `code` | varchar(?) | Y |  |  |  |
| 11 | `sort_order` | int4 | Y | 0 |  |  |
| 12 | `is_active` | bool | Y | TRUE |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_payment_type_scope_code` (`tenant_id`, `code`) WHERE deleted_at IS NULL
- unique index `uq_payment_type_1` (`tenant_id`, `name`) WHERE deleted_at IS NULL

**Relationships**

- ← `payroll.payroll_payment.payment_type_id` — referenced by (1:N)

<a id="payrollpayroll_payment"></a>
### payroll.payroll_payment

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `transaction_number` | varchar(?) | Y |  |  |  |
| 5 | `payment_type_id` | uuid |  |  | FK → `payroll.payment_type` · RESTRICT · tenant-composite |  |
| 6 | `disbursement_account_id` | uuid |  |  | FK → `payroll.disbursement_account` · RESTRICT · tenant-composite |  |
| 7 | `staff_id` | uuid | Y |  | logical → `hr.staff` (cross-module, no FK) |  |
| 8 | `payslip_id` | uuid |  |  | FK → `payroll.payslip` · RESTRICT · tenant-composite |  |
| 9 | `transaction_date` | date | Y |  |  |  |
| 10 | `amount` | numeric(?,?) | Y |  |  |  |
| 11 | `payment_method` | varchar(?) | Y |  |  |  |
| 12 | `transaction_details` | text |  |  |  |  |
| 13 | `reference_number` | varchar(?) |  |  |  |  |
| 14 | `attachment_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 15 | `remarks` | text |  |  |  |  |
| 16 | `created_at` | timestamptz | Y | now() |  |  |
| 17 | `created_by` | uuid |  |  |  |  |
| 18 | `updated_at` | timestamptz | Y | now() |  |  |
| 19 | `updated_by` | uuid |  |  |  |  |
| 20 | `deleted_at` | timestamptz |  |  |  |  |
| 21 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`institution_id`, `transaction_number`)

**Checks**

- `ck_payroll_payment_amount_rule`: amount > 0
- `ck_payroll_payment_payment_method`: payment_method IN ('cash', 'cheque', 'bank_transfer', 'other')

<a id="payrollpayslip"></a>
### payroll.payslip

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `payslip_number` | varchar(?) | Y |  |  |  |
| 5 | `staff_id` | uuid | Y |  | logical → `hr.staff` (cross-module, no FK) |  |
| 6 | `staff_salary_id` | uuid | Y |  | FK → `payroll.staff_salary` · RESTRICT · tenant-composite |  |
| 7 | `period_start` | date | Y |  |  |  |
| 8 | `period_end` | date | Y |  |  |  |
| 9 | `pay_percentage` | numeric(?,?) | Y | 100 |  |  |
| 10 | `leave_days` | numeric(?,?) | Y | 0 |  |  |
| 11 | `late_count` | int4 |  |  |  |  |
| 12 | `leave_deduction` | numeric(?,?) | Y | 0 |  |  |
| 13 | `status` | varchar(?) | Y | 'unpaid' |  |  |
| 14 | `remarks` | text |  |  |  |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |
| 21 | `gross_amount` | numeric(?,?) |  |  |  |  |
| 22 | `deduction_total` | numeric(?,?) |  |  |  |  |
| 23 | `net_amount` | numeric(?,?) |  |  |  |  |
| 24 | `document_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |

**Uniqueness**

- UNIQUE (`institution_id`, `payslip_number`)
- UNIQUE (`id`, `tenant_id`)
- unique index `uq_payslip_period` (`tenant_id`, `staff_id`, `period_start`, `period_end`) WHERE deleted_at IS NULL AND status <> 'cancelled'

**Checks**

- `ck_payslip_status`: status IN ('unpaid', 'paid', 'cancelled')
- `ck_payslip_1`: period_end >= period_start

**Relationships**

- ← `payroll.payroll_payment.payslip_id` — referenced by (1:N)
- ← `payroll.payslip_line.payslip_id` — referenced by (1:N)

<a id="payrollpayslip_line"></a>
### payroll.payslip_line

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `payslip_id` | uuid | Y |  | FK → `payroll.payslip` · CASCADE · tenant-composite |  |
| 4 | `staff_salary_component_id` | uuid | Y |  | FK → `payroll.staff_salary_component` · RESTRICT · tenant-composite |  |
| 5 | `amount` | numeric(?,?) | Y |  |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |
| 12 | `component_code` | varchar(?) |  |  |  |  |
| 13 | `component_name` | varchar(?) |  |  |  |  |
| 14 | `component_type` | varchar(?) |  |  |  |  |

**Uniqueness**

- unique index `uq_payslip_line_1` (`payslip_id`, `staff_salary_component_id`) WHERE deleted_at IS NULL

<a id="payrollsalary_template"></a>
### payroll.salary_template

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `name` | varchar(?) | Y |  |  |  |
| 5 | `is_active` | bool | Y | TRUE |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_salary_template_1` (`institution_id`, `name`) WHERE deleted_at IS NULL

**Relationships**

- ← `payroll.salary_template_component.salary_template_id` — referenced by (1:N)
- ← `payroll.staff_salary.salary_template_id` — referenced by (1:N)

<a id="payrollsalary_template_component"></a>
### payroll.salary_template_component

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `salary_template_id` | uuid | Y |  | FK → `payroll.salary_template` · CASCADE · tenant-composite |  |
| 4 | `pay_component_id` | uuid | Y |  | FK → `payroll.pay_component` · RESTRICT · tenant-composite |  |
| 5 | `pay_category_id` | uuid | Y |  | FK → `payroll.pay_category` · RESTRICT · tenant-composite |  |
| 6 | `value` | numeric(?,?) |  |  |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_salary_template_component_1` (`salary_template_id`, `pay_component_id`) WHERE deleted_at IS NULL

**Relationships**

- ← `payroll.staff_salary_component.salary_template_component_id` — referenced by (1:N)

<a id="payrollstaff_bank_account"></a>
### payroll.staff_bank_account

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `staff_id` | uuid | Y |  | logical → `hr.staff` (cross-module, no FK) · UQ |  |
| 4 | `account_holder_name` | varchar(?) | Y |  |  |  |
| 5 | `account_number_enc` | text | Y |  |  |  |
| 6 | `account_number_masked` | varchar(?) |  |  |  |  |
| 7 | `ifsc_code` | varchar(?) |  |  |  |  |
| 8 | `bank_name` | varchar(?) |  |  |  |  |
| 9 | `branch_name` | varchar(?) |  |  |  |  |
| 10 | `is_primary` | bool | Y | TRUE |  |  |
| 11 | `is_active` | bool | Y | TRUE |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_staff_bank_account_primary` (`staff_id`) WHERE is_primary AND deleted_at IS NULL

<a id="payrollstaff_salary"></a>
### payroll.staff_salary

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `staff_id` | uuid | Y |  | logical → `hr.staff` (cross-module, no FK) |  |
| 4 | `salary_template_id` | uuid | Y |  | FK → `payroll.salary_template` · RESTRICT · tenant-composite |  |
| 5 | `gross_salary` | numeric(?,?) | Y |  |  |  |
| 6 | `effective_date` | date | Y |  |  |  |
| 7 | `remarks` | text |  |  |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_staff_salary_1` (`staff_id`, `effective_date`) WHERE deleted_at IS NULL

**Relationships**

- ← `payroll.payslip.staff_salary_id` — referenced by (1:N)
- ← `payroll.staff_salary_component.staff_salary_id` — referenced by (1:N)

<a id="payrollstaff_salary_component"></a>
### payroll.staff_salary_component

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `staff_salary_id` | uuid | Y |  | FK → `payroll.staff_salary` · CASCADE · tenant-composite |  |
| 4 | `salary_template_component_id` | uuid | Y |  | FK → `payroll.salary_template_component` · RESTRICT · tenant-composite |  |
| 5 | `amount` | numeric(?,?) | Y |  |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_staff_salary_component_1` (`staff_salary_id`, `salary_template_component_id`) WHERE deleted_at IS NULL

**Relationships**

- ← `payroll.payslip_line.staff_salary_component_id` — referenced by (1:N)

## `admission` — Admissions

Admissions: Admission cycles, fees, applications with applicant, guardian and emergency-contact details, through to student creation.

10 tables: [`admission_cycle`](#admissionadmission_cycle), [`admission_cycle_fee`](#admissionadmission_cycle_fee), [`application`](#admissionapplication), [`application_emergency_contact`](#admissionapplication_emergency_contact), [`application_guardian`](#admissionapplication_guardian), [`idempotency_key`](#admissionidempotency_key), [`inbox_event`](#admissioninbox_event), [`number_sequence`](#admissionnumber_sequence), [`operation`](#admissionoperation), [`outbox_event`](#admissionoutbox_event)

<a id="admissionadmission_cycle"></a>
### admission.admission_cycle

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `name` | varchar(?) | Y |  |  |  |
| 5 | `admission_basis` | varchar(?) | Y | 'course' |  |  |
| 6 | `program_id` | uuid |  |  | logical → `academics.program` (cross-module, no FK) |  |
| 7 | `course_id` | uuid |  |  | logical → `academics.course` (cross-module, no FK) |  |
| 8 | `grade_level_id` | uuid |  |  | logical → `academics.grade_level` (cross-module, no FK) |  |
| 9 | `academic_year_id` | uuid |  |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 10 | `academic_term_id` | uuid |  |  | logical → `org.academic_term` (cross-module, no FK) |  |
| 11 | `start_date` | date | Y |  |  |  |
| 12 | `end_date` | date |  |  |  |  |
| 13 | `min_applications` | int4 |  |  |  |  |
| 14 | `max_applications` | int4 |  |  |  |  |
| 15 | `minimum_age_years` | int4 |  |  |  |  |
| 16 | `application_fee_head_id` | uuid |  |  | logical → `finance.fee_head` (cross-module, no FK) |  |
| 17 | `status` | varchar(?) | Y | 'draft' |  |  |
| 18 | `created_at` | timestamptz | Y | now() |  |  |
| 19 | `created_by` | uuid |  |  |  |  |
| 20 | `updated_at` | timestamptz | Y | now() |  |  |
| 21 | `updated_by` | uuid |  |  |  |  |
| 22 | `deleted_at` | timestamptz |  |  |  |  |
| 23 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_admission_cycle_admission_basis`: admission_basis IN ('program', 'course', 'grade_level')
- `ck_admission_cycle_status`: status IN ('draft', 'confirmed', 'open', 'admission', 'closed', 'cancelled')
- `ck_admission_cycle_1`: end_date IS NULL OR end_date >= start_date

**Relationships**

- ← `admission.admission_cycle_fee.admission_cycle_id` — referenced by (1:N)
- ← `admission.application.admission_cycle_id` — referenced by (1:N)

<a id="admissionadmission_cycle_fee"></a>
### admission.admission_cycle_fee

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `admission_cycle_id` | uuid | Y |  | FK → `admission.admission_cycle` · CASCADE · tenant-composite |  |
| 4 | `course_id` | uuid | Y |  | logical → `academics.course` (cross-module, no FK) |  |
| 5 | `fee_head_id` | uuid |  |  | logical → `finance.fee_head` (cross-module, no FK) |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_admission_cycle_fee_1` (`admission_cycle_id`, `course_id`) WHERE deleted_at IS NULL

<a id="admissionapplication"></a>
### admission.application

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `admission_cycle_id` | uuid |  |  | FK → `admission.admission_cycle` · RESTRICT · tenant-composite |  |
| 5 | `application_number` | varchar(?) | Y |  |  |  |
| 6 | `application_date` | timestamptz | Y | now() |  |  |
| 7 | `admission_date` | date |  |  |  |  |
| 8 | `academic_year_id` | uuid |  |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 9 | `grade_level_id` | uuid |  |  | logical → `academics.grade_level` (cross-module, no FK) |  |
| 10 | `section_id` | uuid |  |  | logical → `academics.section` (cross-module, no FK) |  |
| 11 | `program_id` | uuid |  |  | logical → `academics.program` (cross-module, no FK) |  |
| 12 | `course_id` | uuid |  |  | logical → `academics.course` (cross-module, no FK) |  |
| 13 | `batch_id` | uuid |  |  | logical → `academics.batch` (cross-module, no FK) |  |
| 14 | `applicant_person_id` | uuid |  |  | logical → `people.person` (cross-module, no FK) |  |
| 15 | `student_id` | uuid |  |  | logical → `sis.student` (cross-module, no FK) |  |
| 16 | `is_existing_student` | bool | Y | FALSE |  |  |
| 17 | `honorific_id` | uuid |  |  | logical → `people.honorific` (cross-module, no FK) |  |
| 18 | `full_name` | varchar(?) | Y |  |  |  |
| 19 | `first_name` | varchar(?) |  |  |  |  |
| 20 | `middle_name` | varchar(?) |  |  |  |  |
| 21 | `last_name` | varchar(?) |  |  |  |  |
| 22 | `date_of_birth` | date | Y |  |  |  |
| 23 | `gender` | varchar(?) | Y |  |  |  |
| 24 | `birth_place` | varchar(?) |  |  |  |  |
| 25 | `nationality` | varchar(?) |  |  |  |  |
| 26 | `religion` | varchar(?) |  |  |  |  |
| 27 | `social_category` | varchar(?) |  |  |  |  |
| 28 | `mother_tongue` | varchar(?) |  |  |  |  |
| 29 | `identification_marks` | varchar(?) |  |  |  |  |
| 30 | `national_id_enc` | text |  |  |  |  |
| 31 | `blood_group` | varchar(?) |  |  |  |  |
| 32 | `height_cm` | numeric(?,?) |  |  |  |  |
| 33 | `weight_kg` | numeric(?,?) |  |  |  |  |
| 34 | `photo_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 35 | `email` | varchar(?) |  |  |  |  |
| 36 | `phone` | varchar(?) |  |  |  |  |
| 37 | `mobile` | varchar(?) |  |  |  |  |
| 38 | `address_line1` | varchar(?) |  |  |  |  |
| 39 | `address_line2` | varchar(?) |  |  |  |  |
| 40 | `city_name` | varchar(?) |  |  |  |  |
| 41 | `postal_code` | varchar(?) |  |  |  |  |
| 42 | `state_province_id` | uuid |  |  | logical → `master.state_province` (cross-module, no FK) |  |
| 43 | `country_id` | uuid |  |  | logical → `master.country` (cross-module, no FK) |  |
| 44 | `permanent_address` | text |  |  |  |  |
| 45 | `communication_address` | text |  |  |  |  |
| 46 | `previous_institution` | varchar(?) |  |  |  |  |
| 47 | `previous_course` | varchar(?) |  |  |  |  |
| 48 | `previous_result` | varchar(?) |  |  |  |  |
| 49 | `previous_board` | varchar(?) |  |  |  |  |
| 50 | `board_registration_number` | varchar(?) |  |  |  |  |
| 51 | `reason_for_leaving` | text |  |  |  |  |
| 52 | `language_choice` | varchar(?) |  |  |  |  |
| 53 | `stream_choice` | text |  |  |  |  |
| 54 | `has_siblings` | bool |  |  |  |  |
| 55 | `has_medical_history` | bool |  |  |  |  |
| 56 | `medical_details` | text |  |  |  |  |
| 57 | `has_extracurricular` | bool |  |  |  |  |
| 58 | `extracurricular_details` | text |  |  |  |  |
| 59 | `transport_mode` | varchar(?) |  |  |  |  |
| 60 | `transport_details` | text |  |  |  |  |
| 61 | `family_business` | varchar(?) |  |  |  |  |
| 62 | `family_income` | numeric(?,?) |  |  |  |  |
| 63 | `application_fee` | numeric(?,?) |  |  |  |  |
| 64 | `discount_percent` | numeric(?,?) |  |  |  |  |
| 65 | `fee_plan_id` | uuid |  |  | logical → `finance.fee_plan` (cross-module, no FK) |  |
| 66 | `fee_start_date` | date |  |  |  |  |
| 67 | `payment_due_date` | date |  |  |  |  |
| 68 | `payment_status` | varchar(?) | Y | 'unpaid' |  |  |
| 69 | `status` | varchar(?) | Y | 'draft' |  |  |
| 70 | `remarks` | text |  |  |  |  |
| 71 | `payment_status_note` | varchar(?) |  |  |  |  |
| 72 | `created_at` | timestamptz | Y | now() |  |  |
| 73 | `created_by` | uuid |  |  |  |  |
| 74 | `updated_at` | timestamptz | Y | now() |  |  |
| 75 | `updated_by` | uuid |  |  |  |  |
| 76 | `deleted_at` | timestamptz |  |  |  |  |
| 77 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_application_1` (`institution_id`, `application_number`) WHERE deleted_at IS NULL

**Checks**

- `ck_application_gender`: gender IN ('male', 'female', 'other')
- `ck_application_previous_board`: previous_board IN ('cbse', 'state_board', 'icse', 'matriculation', 'anglo_indian', 'other')
- `ck_application_transport_mode`: transport_mode IN ('auto', 'car', 'city_bus', 'cycle', 'rickshaw', 'school_bus', 'taxi', 'walking')
- `ck_application_payment_status`: payment_status IN ('unpaid', 'partial', 'paid', 'waived')
- `ck_application_status`: status IN ('draft', 'submitted', 'pending', 'confirmed', 'approved', 'rejected', 'admitted', 'cancelled', 'done')

**Relationships**

- ← `admission.application_emergency_contact.application_id` — referenced by (1:N)
- ← `admission.application_guardian.application_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `finance.payment.admission_application_id`

<a id="admissionapplication_emergency_contact"></a>
### admission.application_emergency_contact

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `application_id` | uuid | Y |  | FK → `admission.application` · CASCADE · tenant-composite |  |
| 4 | `priority` | int2 | Y |  |  |  |
| 5 | `contact_number` | varchar(?) | Y |  |  |  |
| 6 | `relationship` | varchar(?) | Y |  |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_application_emergency_contact_1` (`application_id`, `priority`) WHERE deleted_at IS NULL

<a id="admissionapplication_guardian"></a>
### admission.application_guardian

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `application_id` | uuid | Y |  | FK → `admission.application` · CASCADE · tenant-composite |  |
| 4 | `relationship` | varchar(?) | Y |  |  |  |
| 5 | `name` | varchar(?) | Y |  |  |  |
| 6 | `qualification_id` | uuid |  |  | logical → `people.qualification` (cross-module, no FK) |  |
| 7 | `designation` | varchar(?) |  |  |  |  |
| 8 | `occupation` | varchar(?) |  |  |  |  |
| 9 | `organisation` | varchar(?) |  |  |  |  |
| 10 | `annual_income` | numeric(?,?) |  |  |  |  |
| 11 | `mobile` | varchar(?) |  |  |  |  |
| 12 | `email` | varchar(?) |  |  |  |  |
| 13 | `national_id_enc` | text |  |  |  |  |
| 14 | `photo_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_application_guardian_1` (`application_id`, `relationship`) WHERE deleted_at IS NULL

**Checks**

- `ck_application_guardian_relationship`: relationship IN ('father', 'mother', 'guardian')

<a id="admissionidempotency_key"></a>
### admission.idempotency_key

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `idempotency_key` | uuid | Y |  |  |  |
| 4 | `user_id` | uuid |  |  |  |  |
| 5 | `operation_name` | varchar(?) | Y |  |  |  |
| 6 | `request_hash` | varchar(?) |  |  |  |  |
| 7 | `response_status` | int2 |  |  |  |  |
| 8 | `response_body` | text |  |  |  |  |
| 9 | `locked_at` | timestamptz |  |  |  |  |
| 10 | `completed_at` | timestamptz |  |  |  |  |
| 11 | `expires_at` | timestamptz | Y |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `idempotency_key`)

<a id="admissioninbox_event"></a>
### admission.inbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `event_id` | uuid | Y |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  | PK |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `received_at` | timestamptz | Y | now() |  |  |
| 5 | `processed_at` | timestamptz |  |  |  |  |
| 6 | `processing_error` | text |  |  |  |  |

<a id="admissionnumber_sequence"></a>
### admission.number_sequence

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `sequence_code` | varchar(?) | Y |  |  |  |
| 5 | `period_key` | varchar(?) | Y | '' |  |  |
| 6 | `prefix` | varchar(?) |  |  |  |  |
| 7 | `padding` | int2 | Y | 6 |  |  |
| 8 | `next_value` | int8 | Y | 1 |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `institution_id`, `sequence_code`, `period_key`) (NULLS NOT DISTINCT)

<a id="admissionoperation"></a>
### admission.operation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `operation_name` | varchar(?) | Y |  |  |  |
| 4 | `resource_type` | varchar(?) |  |  |  |  |
| 5 | `resource_id` | uuid |  |  |  |  |
| 6 | `status` | varchar(?) | Y | 'running' |  |  |
| 7 | `progress_percent` | int2 |  |  |  |  |
| 8 | `error_code` | varchar(?) |  |  |  |  |
| 9 | `error_message` | varchar(?) |  |  |  |  |
| 10 | `result_entity_type` | varchar(?) |  |  |  |  |
| 11 | `result_entity_id` | uuid |  |  |  |  |
| 12 | `started_by_user_id` | uuid |  |  |  |  |
| 13 | `started_at` | timestamptz | Y | now() |  |  |
| 14 | `finished_at` | timestamptz |  |  |  |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_admission_operation_status`: status IN ('running', 'succeeded', 'failed', 'cancelled')

<a id="admissionoutbox_event"></a>
### admission.outbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | int8 |  |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `aggregate_type` | varchar(?) | Y |  |  |  |
| 5 | `aggregate_id` | uuid | Y |  |  |  |
| 6 | `payload` | text | Y |  |  |  |
| 7 | `occurred_at` | timestamptz | Y | now() |  |  |
| 8 | `published_at` | timestamptz |  |  |  |  |
| 9 | `publish_error` | text |  |  |  | Why the relay set the row aside (not published) |

## `attendance` — Attendance

Attendance: Attendance registers, sheets (one per register/session/date) and per-student records, with configurable attendance types and absence reasons.

10 tables: [`absence_reason`](#attendanceabsence_reason), [`attendance_record`](#attendanceattendance_record), [`attendance_register`](#attendanceattendance_register), [`attendance_sheet`](#attendanceattendance_sheet), [`attendance_type`](#attendanceattendance_type), [`idempotency_key`](#attendanceidempotency_key), [`inbox_event`](#attendanceinbox_event), [`operation`](#attendanceoperation), [`outbox_event`](#attendanceoutbox_event), [`staff_attendance`](#attendancestaff_attendance)

<a id="attendanceabsence_reason"></a>
### attendance.absence_reason

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `title` | varchar(?) | Y |  |  |  |
| 4 | `is_active` | bool | Y | TRUE |  |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |
| 11 | `code` | varchar(?) | Y |  |  |  |
| 12 | `sort_order` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_absence_reason_scope_code` (`tenant_id`, `code`) WHERE deleted_at IS NULL
- unique index `uq_absence_reason_1` (`tenant_id`, `title`) WHERE deleted_at IS NULL

**Relationships**

- ← `attendance.attendance_record.absence_reason_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `hr.leave_application.absence_reason_id`

<a id="attendanceattendance_record"></a>
### attendance.attendance_record

*Partitioned by RANGE (attendance_date); monthly partitions plus a default partition.*

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `sheet_id` | uuid | Y |  | FK → `attendance.attendance_sheet` · RESTRICT |  |
| 4 | `student_id` | uuid | Y |  | logical → `sis.student` (cross-module, no FK) |  |
| 5 | `status` | varchar(?) | Y |  |  |  |
| 6 | `attendance_type_id` | uuid |  |  | FK → `attendance.attendance_type` · RESTRICT · tenant-composite |  |
| 7 | `absence_reason_id` | uuid |  |  | FK → `attendance.absence_reason` · RESTRICT · tenant-composite |  |
| 8 | `remark` | varchar(?) |  |  |  |  |
| 9 | `recorded_by_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 10 | `attendance_date` | date | Y |  | PK |  |
| 11 | `created_at` | timestamptz | Y | now() |  |  |
| 12 | `created_by` | uuid |  |  |  |  |
| 13 | `updated_at` | timestamptz | Y | now() |  |  |
| 14 | `updated_by` | uuid |  |  |  |  |
| 15 | `deleted_at` | timestamptz |  |  |  |  |
| 16 | `row_version` | int4 | Y | 0 |  |  |
| 17 | `leave_request_id` | uuid |  |  | logical → `sis.student_leave_request` (cross-module, no FK) |  |

**Uniqueness**

- unique index `uq_attendance_record_1` (`sheet_id`, `student_id`, `attendance_date`) WHERE deleted_at IS NULL

**Checks**

- `ck_attendance_record_status`: status IN ('present', 'absent', 'late', 'excused')

<a id="attendanceattendance_register"></a>
### attendance.attendance_register

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `code` | varchar(?) | Y |  |  |  |
| 5 | `name` | varchar(?) | Y |  |  |  |
| 6 | `register_scope` | varchar(?) | Y |  |  |  |
| 7 | `class_section_id` | uuid |  |  | logical → `academics.class_section` (cross-module, no FK) |  |
| 8 | `course_id` | uuid |  |  | logical → `academics.course` (cross-module, no FK) |  |
| 9 | `batch_id` | uuid |  |  | logical → `academics.batch` (cross-module, no FK) |  |
| 10 | `subject_id` | uuid |  |  | logical → `academics.subject` (cross-module, no FK) |  |
| 11 | `is_active` | bool | Y | TRUE |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_attendance_register_1` (`institution_id`, `code`) WHERE deleted_at IS NULL

**Checks**

- `ck_attendance_register_register_scope`: register_scope IN ('institution', 'class_section', 'course_batch', 'subject')
- `ck_attendance_register_1`: (register_scope = 'institution' AND class_section_id IS NULL AND course_id IS NULL AND batch_id IS NULL AND subject_id IS NULL) OR (register_scope = 'class_section' AND class_section_id IS NOT NULL AND course_id IS NULL AND batch_id IS NULL) OR (register_scope = 'course_batch' AND course_id IS NOT NULL AND batch_id IS NOT NULL AND class_section_id IS NULL) OR (register_scope = 'subject' AND subject_id IS NOT NULL AND class_section_id IS NULL AND course_id IS NULL AND batch_id IS NULL)

**Relationships**

- ← `attendance.attendance_sheet.register_id` — referenced by (1:N)

<a id="attendanceattendance_sheet"></a>
### attendance.attendance_sheet

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `register_id` | uuid | Y |  | FK → `attendance.attendance_register` · RESTRICT · tenant-composite |  |
| 4 | `academic_year_id` | uuid |  |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `attendance_date` | date | Y |  |  |  |
| 6 | `timetable_session_id` | uuid |  |  | logical → `timetable.session` (cross-module, no FK) |  |
| 7 | `day_part` | varchar(?) | Y | 'full_day' |  |  |
| 8 | `reference` | varchar(?) |  |  |  |  |
| 9 | `taken_by_staff_id` | uuid |  |  | logical → `hr.staff` (cross-module, no FK) |  |
| 10 | `status` | varchar(?) | Y | 'draft' |  |  |
| 11 | `created_at` | timestamptz | Y | now() |  |  |
| 12 | `created_by` | uuid |  |  |  |  |
| 13 | `updated_at` | timestamptz | Y | now() |  |  |
| 14 | `updated_by` | uuid |  |  |  |  |
| 15 | `deleted_at` | timestamptz |  |  |  |  |
| 16 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `attendance_date`)
- UNIQUE (`id`, `tenant_id`)
- unique index `ix_attendance_sheet_1` (`register_id`, `attendance_date`, `day_part`, `COALESCE(timetable_session_id, CAST('00000000-0000-0000-0000-000000000000' AS uuid))`) WHERE deleted_at IS NULL
- unique index `uq_attendance_sheet_session` (`tenant_id`, `timetable_session_id`) WHERE deleted_at IS NULL AND timetable_session_id IS NOT NULL — one sheet per timetable session

**Checks**

- `ck_attendance_sheet_day_part`: day_part IN ('full_day', 'forenoon', 'afternoon')
- `ck_attendance_sheet_status`: status IN ('draft', 'in_progress', 'done', 'cancelled')

**Relationships**

- ← `attendance.attendance_record.sheet_id` — referenced by (1:N)
- ← `attendance.attendance_record.sheet_id` — referenced by (1:N)

<a id="attendanceattendance_type"></a>
### attendance.attendance_type

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `name` | varchar(?) | Y |  |  |  |
| 5 | `counts_present` | bool | Y | FALSE |  |  |
| 6 | `counts_absent` | bool | Y | FALSE |  |  |
| 7 | `counts_excused` | bool | Y | FALSE |  |  |
| 8 | `counts_late` | bool | Y | FALSE |  |  |
| 9 | `is_active` | bool | Y | TRUE |  |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |
| 16 | `code` | varchar(?) | Y |  |  |  |
| 17 | `sort_order` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_attendance_type_scope_code` (`institution_id`, `code`) WHERE deleted_at IS NULL
- unique index `uq_attendance_type_1` (`institution_id`, `name`) WHERE deleted_at IS NULL

**Relationships**

- ← `attendance.attendance_record.attendance_type_id` — referenced by (1:N)

<a id="attendanceidempotency_key"></a>
### attendance.idempotency_key

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `idempotency_key` | uuid | Y |  |  |  |
| 4 | `user_id` | uuid |  |  |  |  |
| 5 | `operation_name` | varchar(?) | Y |  |  |  |
| 6 | `request_hash` | varchar(?) |  |  |  |  |
| 7 | `response_status` | int2 |  |  |  |  |
| 8 | `response_body` | text |  |  |  |  |
| 9 | `locked_at` | timestamptz |  |  |  |  |
| 10 | `completed_at` | timestamptz |  |  |  |  |
| 11 | `expires_at` | timestamptz | Y |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `idempotency_key`)

<a id="attendanceinbox_event"></a>
### attendance.inbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `event_id` | uuid | Y |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  | PK |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `received_at` | timestamptz | Y | now() |  |  |
| 5 | `processed_at` | timestamptz |  |  |  |  |
| 6 | `processing_error` | text |  |  |  |  |

<a id="attendanceoperation"></a>
### attendance.operation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `operation_name` | varchar(?) | Y |  |  |  |
| 4 | `resource_type` | varchar(?) |  |  |  |  |
| 5 | `resource_id` | uuid |  |  |  |  |
| 6 | `status` | varchar(?) | Y | 'running' |  |  |
| 7 | `progress_percent` | int2 |  |  |  |  |
| 8 | `error_code` | varchar(?) |  |  |  |  |
| 9 | `error_message` | varchar(?) |  |  |  |  |
| 10 | `result_entity_type` | varchar(?) |  |  |  |  |
| 11 | `result_entity_id` | uuid |  |  |  |  |
| 12 | `started_by_user_id` | uuid |  |  |  |  |
| 13 | `started_at` | timestamptz | Y | now() |  |  |
| 14 | `finished_at` | timestamptz |  |  |  |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_attendance_operation_status`: status IN ('running', 'succeeded', 'failed', 'cancelled')

<a id="attendanceoutbox_event"></a>
### attendance.outbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | int8 |  |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `aggregate_type` | varchar(?) | Y |  |  |  |
| 5 | `aggregate_id` | uuid | Y |  |  |  |
| 6 | `payload` | text | Y |  |  |  |
| 7 | `occurred_at` | timestamptz | Y | now() |  |  |
| 8 | `published_at` | timestamptz |  |  |  |  |
| 9 | `publish_error` | text |  |  |  | Why the relay set the row aside (not published) |

<a id="attendancestaff_attendance"></a>
### attendance.staff_attendance

*Partitioned by RANGE (attendance_date); monthly partitions plus a default partition.*

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `staff_id` | uuid | Y |  | logical → `hr.staff` (cross-module, no FK) |  |
| 4 | `attendance_date` | date | Y |  | PK |  |
| 5 | `status` | varchar(?) | Y |  |  |  |
| 6 | `check_in_at` | timestamptz |  |  |  |  |
| 7 | `check_out_at` | timestamptz |  |  |  |  |
| 8 | `source` | varchar(?) | Y | 'manual' |  |  |
| 9 | `leave_application_id` | uuid |  |  | logical → `hr.leave_application` (cross-module, no FK) |  |
| 10 | `recorded_by_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 11 | `remark` | varchar(?) |  |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_staff_attendance_1` (`staff_id`, `attendance_date`) WHERE deleted_at IS NULL

**Checks**

- `ck_staff_attendance_status`: status IN ('present', 'absent', 'half_day', 'late', 'on_leave', 'holiday')
- `ck_staff_attendance_source`: source IN ('manual', 'biometric', 'import')

## `facility` — Rooms & Facilities

Rooms & Facilities: Rooms (classrooms, labs, exam halls), the facilities they offer and the assets they hold.

9 tables: [`asset_item`](#facilityasset_item), [`facility`](#facilityfacility), [`idempotency_key`](#facilityidempotency_key), [`inbox_event`](#facilityinbox_event), [`operation`](#facilityoperation), [`outbox_event`](#facilityoutbox_event), [`room`](#facilityroom), [`room_asset`](#facilityroom_asset), [`room_facility`](#facilityroom_facility)

<a id="facilityasset_item"></a>
### facility.asset_item

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `code` | varchar(?) |  |  |  |  |
| 5 | `name` | varchar(?) | Y |  |  |  |
| 6 | `unit_of_measure` | varchar(?) | Y | 'unit' |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |
| 13 | `sort_order` | int4 | Y | 0 |  |  |
| 14 | `is_active` | bool | Y | TRUE |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `ix_asset_item_1` (`institution_id`, `code`) WHERE code IS NOT NULL
- unique index `uq_asset_item_scope_code` (`institution_id`, `code`) WHERE deleted_at IS NULL

**Relationships**

- ← `facility.room_asset.asset_item_id` — referenced by (1:N)

<a id="facilityfacility"></a>
### facility.facility

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `code` | varchar(?) | Y |  |  |  |
| 5 | `name` | varchar(?) | Y |  |  |  |
| 6 | `is_active` | bool | Y | TRUE |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |
| 13 | `sort_order` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_facility_scope_code` (`institution_id`, `code`) WHERE deleted_at IS NULL

**Relationships**

- ← `facility.room_facility.facility_id` — referenced by (1:N)

<a id="facilityidempotency_key"></a>
### facility.idempotency_key

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `idempotency_key` | uuid | Y |  |  |  |
| 4 | `user_id` | uuid |  |  |  |  |
| 5 | `operation_name` | varchar(?) | Y |  |  |  |
| 6 | `request_hash` | varchar(?) |  |  |  |  |
| 7 | `response_status` | int2 |  |  |  |  |
| 8 | `response_body` | text |  |  |  |  |
| 9 | `locked_at` | timestamptz |  |  |  |  |
| 10 | `completed_at` | timestamptz |  |  |  |  |
| 11 | `expires_at` | timestamptz | Y |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `idempotency_key`)

<a id="facilityinbox_event"></a>
### facility.inbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `event_id` | uuid | Y |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  | PK |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `received_at` | timestamptz | Y | now() |  |  |
| 5 | `processed_at` | timestamptz |  |  |  |  |
| 6 | `processing_error` | text |  |  |  |  |

<a id="facilityoperation"></a>
### facility.operation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `operation_name` | varchar(?) | Y |  |  |  |
| 4 | `resource_type` | varchar(?) |  |  |  |  |
| 5 | `resource_id` | uuid |  |  |  |  |
| 6 | `status` | varchar(?) | Y | 'running' |  |  |
| 7 | `progress_percent` | int2 |  |  |  |  |
| 8 | `error_code` | varchar(?) |  |  |  |  |
| 9 | `error_message` | varchar(?) |  |  |  |  |
| 10 | `result_entity_type` | varchar(?) |  |  |  |  |
| 11 | `result_entity_id` | uuid |  |  |  |  |
| 12 | `started_by_user_id` | uuid |  |  |  |  |
| 13 | `started_at` | timestamptz | Y | now() |  |  |
| 14 | `finished_at` | timestamptz |  |  |  |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_facility_operation_status`: status IN ('running', 'succeeded', 'failed', 'cancelled')

<a id="facilityoutbox_event"></a>
### facility.outbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | int8 |  |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `aggregate_type` | varchar(?) | Y |  |  |  |
| 5 | `aggregate_id` | uuid | Y |  |  |  |
| 6 | `payload` | text | Y |  |  |  |
| 7 | `occurred_at` | timestamptz | Y | now() |  |  |
| 8 | `published_at` | timestamptz |  |  |  |  |
| 9 | `publish_error` | text |  |  |  | Why the relay set the row aside (not published) |

<a id="facilityroom"></a>
### facility.room

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `code` | varchar(?) | Y |  |  |  |
| 5 | `name` | varchar(?) | Y |  |  |  |
| 6 | `room_type` | varchar(?) | Y | 'classroom' |  |  |
| 7 | `capacity` | int4 | Y |  |  |  |
| 8 | `default_course_id` | uuid |  |  | logical → `academics.course` (cross-module, no FK) |  |
| 9 | `default_batch_id` | uuid |  |  | logical → `academics.batch` (cross-module, no FK) |  |
| 10 | `default_class_section_id` | uuid |  |  | logical → `academics.class_section` (cross-module, no FK) |  |
| 11 | `is_active` | bool | Y | TRUE |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_room_1` (`institution_id`, `code`) WHERE deleted_at IS NULL

**Checks**

- `ck_room_room_type`: room_type IN ('classroom', 'laboratory', 'hall', 'exam_hall', 'library', 'other')
- `ck_room_capacity_rule`: capacity > 0

**Relationships**

- ← `facility.room_asset.room_id` — referenced by (1:N)
- ← `facility.room_facility.room_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `assessment.exam_result.room_id`
- ← `timetable.session.room_id`

<a id="facilityroom_asset"></a>
### facility.room_asset

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `room_id` | uuid | Y |  | FK → `facility.room` · CASCADE · tenant-composite |  |
| 4 | `asset_item_id` | uuid | Y |  | FK → `facility.asset_item` · RESTRICT · tenant-composite |  |
| 5 | `tag_code` | varchar(?) |  |  |  |  |
| 6 | `quantity` | numeric(?,?) | Y |  |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_room_asset_quantity_rule`: quantity > 0

<a id="facilityroom_facility"></a>
### facility.room_facility

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `room_id` | uuid | Y |  | FK → `facility.room` · CASCADE · tenant-composite |  |
| 4 | `facility_id` | uuid | Y |  | FK → `facility.facility` · RESTRICT · tenant-composite |  |
| 5 | `quantity` | numeric(?,?) | Y |  |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_room_facility_1` (`room_id`, `facility_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_room_facility_quantity_rule`: quantity > 0

## `timetable` — Timetable

Timetable: Period definitions and scheduled teaching sessions.

8 tables: [`idempotency_key`](#timetableidempotency_key), [`inbox_event`](#timetableinbox_event), [`institution_zone`](#timetableinstitution_zone), [`operation`](#timetableoperation), [`outbox_event`](#timetableoutbox_event), [`period`](#timetableperiod), [`session`](#timetablesession), [`session_student`](#timetablesession_student)

<a id="timetableidempotency_key"></a>
### timetable.idempotency_key

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `idempotency_key` | uuid | Y |  |  |  |
| 4 | `user_id` | uuid |  |  |  |  |
| 5 | `operation_name` | varchar(?) | Y |  |  |  |
| 6 | `request_hash` | varchar(?) |  |  |  |  |
| 7 | `response_status` | int2 |  |  |  |  |
| 8 | `response_body` | text |  |  |  |  |
| 9 | `locked_at` | timestamptz |  |  |  |  |
| 10 | `completed_at` | timestamptz |  |  |  |  |
| 11 | `expires_at` | timestamptz | Y |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `idempotency_key`)

<a id="timetableinbox_event"></a>
### timetable.inbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `event_id` | uuid | Y |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  | PK |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `received_at` | timestamptz | Y | now() |  |  |
| 5 | `processed_at` | timestamptz |  |  |  |  |
| 6 | `processing_error` | text |  |  |  |  |

<a id="timetableinstitution_zone"></a>
### timetable.institution_zone

Each school's own time zone, kept from `org.institution` created/updated/deleted events (timetable never calls org), so `GenerateSessions` places periods in the school's zone.

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `tenant_id` | uuid | Y |  | PK | Owning tenant. Row-level security key. |
| 2 | `institution_id` | uuid | Y |  | PK | `org.institution.id` (kept from its events) |
| 3 | `time_zone` | varchar(64) |  |  |  | IANA zone of the school; NULL while it keeps the tenant's |
| 4 | `is_deleted` | bool | Y | false |  | Deleted in org; kept so a late event cannot restore it |
| 5 | `source_version` | int4 | Y |  |  | Source row version this entry reflects; older events are ignored |
| 6 | `synced_at` | timestamptz | Y |  |  | When an event last changed the row |

**Checks**

- `ck_institution_zone_source_version`: source_version >= 1

<a id="timetableoperation"></a>
### timetable.operation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `operation_name` | varchar(?) | Y |  |  |  |
| 4 | `resource_type` | varchar(?) |  |  |  |  |
| 5 | `resource_id` | uuid |  |  |  |  |
| 6 | `status` | varchar(?) | Y | 'running' |  |  |
| 7 | `progress_percent` | int2 |  |  |  |  |
| 8 | `error_code` | varchar(?) |  |  |  |  |
| 9 | `error_message` | varchar(?) |  |  |  |  |
| 10 | `result_entity_type` | varchar(?) |  |  |  |  |
| 11 | `result_entity_id` | uuid |  |  |  |  |
| 12 | `started_by_user_id` | uuid |  |  |  |  |
| 13 | `started_at` | timestamptz | Y | now() |  |  |
| 14 | `finished_at` | timestamptz |  |  |  |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_timetable_operation_status`: status IN ('running', 'succeeded', 'failed', 'cancelled')

<a id="timetableoutbox_event"></a>
### timetable.outbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | int8 |  |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `aggregate_type` | varchar(?) | Y |  |  |  |
| 5 | `aggregate_id` | uuid | Y |  |  |  |
| 6 | `payload` | text | Y |  |  |  |
| 7 | `occurred_at` | timestamptz | Y | now() |  |  |
| 8 | `published_at` | timestamptz |  |  |  |  |
| 9 | `publish_error` | text |  |  |  | Why the relay set the row aside (not published) |

<a id="timetableperiod"></a>
### timetable.period

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `name` | varchar(?) | Y |  |  |  |
| 5 | `start_time` | time | Y |  |  |  |
| 6 | `duration_minutes` | numeric(?,?) |  |  |  |  |
| 7 | `sequence` | int4 |  |  |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_period_1` (`institution_id`, `name`) WHERE deleted_at IS NULL

**Checks**

- `ck_period_duration_minutes_rule`: duration_minutes > 0

**Relationships**

- ← `timetable.session.period_id` — referenced by (1:N)

<a id="timetablesession"></a>
### timetable.session

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `period_id` | uuid |  |  | FK → `timetable.period` · SET NULL · tenant-composite |  |
| 5 | `start_at` | timestamptz | Y |  |  |  |
| 6 | `end_at` | timestamptz | Y |  |  |  |
| 7 | `weekday` | int2 |  |  |  |  |
| 8 | `class_section_id` | uuid |  |  | logical → `academics.class_section` (cross-module, no FK) |  |
| 9 | `course_id` | uuid |  |  | logical → `academics.course` (cross-module, no FK) |  |
| 10 | `batch_id` | uuid |  |  | logical → `academics.batch` (cross-module, no FK) |  |
| 11 | `subject_id` | uuid |  |  | logical → `academics.subject` (cross-module, no FK) |  |
| 12 | `staff_id` | uuid |  |  | logical → `hr.staff` (cross-module, no FK) |  |
| 13 | `room_id` | uuid |  |  | logical → `facility.room` (cross-module, no FK) |  |
| 14 | `title` | varchar(?) |  |  |  |  |
| 15 | `session_type` | varchar(?) |  |  |  |  |
| 16 | `color` | int2 |  |  |  |  |
| 17 | `status` | varchar(?) | Y | 'draft' |  |  |
| 18 | `created_at` | timestamptz | Y | now() |  |  |
| 19 | `created_by` | uuid |  |  |  |  |
| 20 | `updated_at` | timestamptz | Y | now() |  |  |
| 21 | `updated_by` | uuid |  |  |  |  |
| 22 | `deleted_at` | timestamptz |  |  |  |  |
| 23 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_session_weekday_rule`: weekday BETWEEN 1 AND 7
- `ck_session_status`: status IN ('draft', 'confirmed', 'done', 'cancelled')
- `ck_session_1`: end_at > start_at
- `ck_session_2`: class_section_id IS NOT NULL OR (course_id IS NOT NULL AND batch_id IS NOT NULL)
- indexes `ix_session_staff_start`, `ix_session_class_section_start`, `ix_session_batch_start`, `ix_session_room_start` (`tenant_id`, the subject, `start_at`) WHERE deleted_at IS NULL AND the subject IS NOT NULL — the timetable view of a teacher, class, batch or room over a range of days

**Relationships**

- ← `timetable.session_student.session_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `attendance.attendance_sheet.timetable_session_id`

<a id="timetablesession_student"></a>
### timetable.session_student

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `session_id` | uuid | Y |  | FK → `timetable.session` · CASCADE · tenant-composite |  |
| 4 | `student_id` | uuid | Y |  | logical → `sis.student` (cross-module, no FK) |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_session_student_1` (`session_id`, `student_id`) WHERE deleted_at IS NULL

## `assessment` — Assessment & Results

Assessment & Results: Homework, assignments, exams, grading schemes, marksheets and results for institutional (official) assessment.

25 tables: [`assignment`](#assessmentassignment), [`assignment_allocation`](#assessmentassignment_allocation), [`assignment_approval`](#assessmentassignment_approval), [`assignment_subject`](#assessmentassignment_subject), [`assignment_submission`](#assessmentassignment_submission), [`assignment_type`](#assessmentassignment_type), [`exam`](#assessmentexam), [`exam_invigilator`](#assessmentexam_invigilator), [`exam_result`](#assessmentexam_result), [`exam_session`](#assessmentexam_session), [`exam_type`](#assessmentexam_type), [`grading_band`](#assessmentgrading_band), [`grading_scheme`](#assessmentgrading_scheme), [`homework`](#assessmenthomework), [`homework_approval`](#assessmenthomework_approval), [`homework_submission`](#assessmenthomework_submission), [`idempotency_key`](#assessmentidempotency_key), [`inbox_event`](#assessmentinbox_event), [`marksheet`](#assessmentmarksheet), [`marksheet_line`](#assessmentmarksheet_line), [`operation`](#assessmentoperation), [`outbox_event`](#assessmentoutbox_event), [`result_change_log`](#assessmentresult_change_log), [`result_template`](#assessmentresult_template), [`result_template_band`](#assessmentresult_template_band)

<a id="assessmentassignment"></a>
### assessment.assignment

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `academic_year_id` | uuid |  |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `title` | varchar(?) | Y |  |  |  |
| 6 | `description` | text |  |  |  |  |
| 7 | `assignment_type_id` | uuid |  |  | FK → `assessment.assignment_type` · RESTRICT · tenant-composite |  |
| 8 | `class_section_id` | uuid |  |  | logical → `academics.class_section` (cross-module, no FK) |  |
| 9 | `course_id` | uuid |  |  | logical → `academics.course` (cross-module, no FK) |  |
| 10 | `batch_id` | uuid |  |  | logical → `academics.batch` (cross-module, no FK) |  |
| 11 | `assigned_by_staff_id` | uuid |  |  | logical → `hr.staff` (cross-module, no FK) |  |
| 12 | `reviewer_staff_id` | uuid |  |  | logical → `hr.staff` (cross-module, no FK) |  |
| 13 | `max_marks` | numeric(?,?) |  |  |  |  |
| 14 | `issued_on` | timestamptz |  |  |  |  |
| 15 | `due_at` | timestamptz |  |  |  |  |
| 16 | `attachment_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 17 | `status` | varchar(?) | Y | 'draft' |  |  |
| 18 | `created_at` | timestamptz | Y | now() |  |  |
| 19 | `created_by` | uuid |  |  |  |  |
| 20 | `updated_at` | timestamptz | Y | now() |  |  |
| 21 | `updated_by` | uuid |  |  |  |  |
| 22 | `deleted_at` | timestamptz |  |  |  |  |
| 23 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_assignment_status`: status IN ('draft', 'published', 'ongoing', 'completed', 'cancelled')
- `ck_assignment_1`: class_section_id IS NOT NULL OR course_id IS NOT NULL

**Relationships**

- ← `assessment.assignment_allocation.assignment_id` — referenced by (1:N)
- ← `assessment.assignment_approval.assignment_id` — referenced by (1:N)
- ← `assessment.assignment_subject.assignment_id` — referenced by (1:N)
- ← `assessment.assignment_submission.assignment_id` — referenced by (1:N)

<a id="assessmentassignment_allocation"></a>
### assessment.assignment_allocation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `assignment_id` | uuid | Y |  | FK → `assessment.assignment` · CASCADE · tenant-composite |  |
| 4 | `student_id` | uuid | Y |  | logical → `sis.student` (cross-module, no FK) |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_assignment_allocation_1` (`assignment_id`, `student_id`) WHERE deleted_at IS NULL

<a id="assessmentassignment_approval"></a>
### assessment.assignment_approval

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `assignment_id` | uuid | Y |  | FK → `assessment.assignment` · CASCADE · tenant-composite |  |
| 4 | `decision` | varchar(?) | Y | 'pending' |  |  |
| 5 | `comments` | text |  |  |  |  |
| 6 | `decided_by_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 7 | `decided_at` | timestamptz |  |  |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_assignment_approval_decision`: decision IN ('pending', 'approved', 'rejected')

<a id="assessmentassignment_subject"></a>
### assessment.assignment_subject

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `assignment_id` | uuid | Y |  | FK → `assessment.assignment` · CASCADE · tenant-composite |  |
| 4 | `subject_id` | uuid | Y |  | logical → `academics.subject` (cross-module, no FK) |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_assignment_subject_1` (`assignment_id`, `subject_id`) WHERE deleted_at IS NULL

<a id="assessmentassignment_submission"></a>
### assessment.assignment_submission

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `assignment_id` | uuid | Y |  | FK → `assessment.assignment` · RESTRICT · tenant-composite |  |
| 4 | `student_id` | uuid | Y |  | logical → `sis.student` (cross-module, no FK) |  |
| 5 | `content` | text |  |  |  |  |
| 6 | `submitted_at` | timestamptz | Y |  |  |  |
| 7 | `status` | varchar(?) | Y | 'submitted' |  |  |
| 8 | `marks_obtained` | numeric(?,?) |  |  |  |  |
| 9 | `graded_by_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 10 | `graded_on` | date |  |  |  |  |
| 11 | `feedback` | text |  |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_assignment_submission_1` (`assignment_id`, `student_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_assignment_submission_status`: status IN ('draft', 'submitted', 'change_requested', 'accepted', 'rejected', 'graded', 'cancelled')

<a id="assessmentassignment_type"></a>
### assessment.assignment_type

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `code` | varchar(?) | Y |  |  |  |
| 4 | `name` | varchar(?) | Y |  |  |  |
| 5 | `grading_basis` | varchar(?) | Y | 'submission' |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |
| 12 | `sort_order` | int4 | Y | 0 |  |  |
| 13 | `is_active` | bool | Y | TRUE |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_assignment_type_scope_code` (`tenant_id`, `code`) WHERE deleted_at IS NULL

**Checks**

- `ck_assignment_type_grading_basis`: grading_basis IN ('submission', 'attendance')

**Relationships**

- ← `assessment.assignment.assignment_type_id` — referenced by (1:N)

<a id="assessmentexam"></a>
### assessment.exam

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `exam_session_id` | uuid | Y |  | FK → `assessment.exam_session` · RESTRICT · tenant-composite |  |
| 4 | `subject_id` | uuid | Y |  | logical → `academics.subject` (cross-module, no FK) |  |
| 5 | `class_section_id` | uuid |  |  | logical → `academics.class_section` (cross-module, no FK) |  |
| 6 | `code` | varchar(?) | Y |  |  |  |
| 7 | `name` | varchar(?) | Y |  |  |  |
| 8 | `start_at` | timestamptz | Y |  |  |  |
| 9 | `end_at` | timestamptz | Y |  |  |  |
| 10 | `total_marks` | numeric(?,?) | Y |  |  |  |
| 11 | `pass_marks` | numeric(?,?) | Y |  |  |  |
| 12 | `status` | varchar(?) | Y | 'draft' |  |  |
| 13 | `results_entered` | bool | Y | FALSE |  |  |
| 14 | `notes` | text |  |  |  |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_exam_1` (`exam_session_id`, `code`) WHERE deleted_at IS NULL

**Checks**

- `ck_exam_total_marks_rule`: total_marks > 0
- `ck_exam_status`: status IN ('draft', 'scheduled', 'held', 'result_updated', 'cancelled', 'done')
- `ck_exam_1`: end_at > start_at
- `ck_exam_2`: pass_marks <= total_marks

**Relationships**

- ← `assessment.exam_invigilator.exam_id` — referenced by (1:N)
- ← `assessment.exam_result.exam_id` — referenced by (1:N)

<a id="assessmentexam_invigilator"></a>
### assessment.exam_invigilator

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `exam_id` | uuid | Y |  | FK → `assessment.exam` · CASCADE · tenant-composite |  |
| 4 | `staff_id` | uuid | Y |  | logical → `hr.staff` (cross-module, no FK) |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_exam_invigilator_1` (`exam_id`, `staff_id`) WHERE deleted_at IS NULL

<a id="assessmentexam_result"></a>
### assessment.exam_result

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `exam_id` | uuid | Y |  | FK → `assessment.exam` · RESTRICT · tenant-composite |  |
| 4 | `student_id` | uuid | Y |  | logical → `sis.student` (cross-module, no FK) |  |
| 5 | `room_id` | uuid |  |  | logical → `facility.room` (cross-module, no FK) |  |
| 6 | `attendance_status` | varchar(?) | Y | 'expected' |  |  |
| 7 | `marks` | numeric(?,?) |  |  |  |  |
| 8 | `max_marks` | numeric(?,?) |  |  |  |  |
| 9 | `grade_label` | varchar(?) |  |  |  |  |
| 10 | `grade_point` | numeric(?,?) |  |  |  |  |
| 11 | `credits` | numeric(?,?) |  |  |  |  |
| 12 | `result_status` | varchar(?) |  |  |  |  |
| 13 | `note` | varchar(?) |  |  |  |  |
| 14 | `marksheet_line_id` | uuid |  |  | FK → `assessment.marksheet_line` · SET NULL · tenant-composite |  |
| 15 | `evaluated_by_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 16 | `evaluated_at` | timestamptz |  |  |  |  |
| 17 | `created_at` | timestamptz | Y | now() |  |  |
| 18 | `created_by` | uuid |  |  |  |  |
| 19 | `updated_at` | timestamptz | Y | now() |  |  |
| 20 | `updated_by` | uuid |  |  |  |  |
| 21 | `deleted_at` | timestamptz |  |  |  |  |
| 22 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_exam_result_1` (`exam_id`, `student_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_exam_result_attendance_status`: attendance_status IN ('expected', 'present', 'absent', 'exempted')
- `ck_exam_result_result_status`: result_status IN ('pass', 'fail', 'absent', 'withheld', 'pending')

**Relationships**

- ← `assessment.result_change_log.exam_result_id` — referenced by (1:N)

<a id="assessmentexam_session"></a>
### assessment.exam_session

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `academic_year_id` | uuid |  |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `academic_term_id` | uuid |  |  | logical → `org.academic_term` (cross-module, no FK) |  |
| 6 | `exam_type_id` | uuid | Y |  | FK → `assessment.exam_type` · RESTRICT · tenant-composite |  |
| 7 | `code` | varchar(?) | Y |  |  |  |
| 8 | `name` | varchar(?) | Y |  |  |  |
| 9 | `grade_level_id` | uuid |  |  | logical → `academics.grade_level` (cross-module, no FK) |  |
| 10 | `course_id` | uuid |  |  | logical → `academics.course` (cross-module, no FK) |  |
| 11 | `batch_id` | uuid |  |  | logical → `academics.batch` (cross-module, no FK) |  |
| 12 | `evaluation_type` | varchar(?) | Y | 'marks' |  |  |
| 13 | `start_date` | date | Y |  |  |  |
| 14 | `end_date` | date | Y |  |  |  |
| 15 | `venue` | varchar(?) |  |  |  |  |
| 16 | `status` | varchar(?) | Y | 'draft' |  |  |
| 17 | `created_at` | timestamptz | Y | now() |  |  |
| 18 | `created_by` | uuid |  |  |  |  |
| 19 | `updated_at` | timestamptz | Y | now() |  |  |
| 20 | `updated_by` | uuid |  |  |  |  |
| 21 | `deleted_at` | timestamptz |  |  |  |  |
| 22 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_exam_session_1` (`institution_id`, `code`) WHERE deleted_at IS NULL

**Checks**

- `ck_exam_session_evaluation_type`: evaluation_type IN ('marks', 'grade')
- `ck_exam_session_status`: status IN ('draft', 'scheduled', 'held', 'cancelled', 'done')
- `ck_exam_session_1`: end_date >= start_date
- `ck_exam_session_2`: grade_level_id IS NOT NULL OR (course_id IS NOT NULL AND batch_id IS NOT NULL)

**Relationships**

- ← `assessment.exam.exam_session_id` — referenced by (1:N)
- ← `assessment.marksheet.exam_session_id` — referenced by (1:N)
- ← `assessment.result_template.exam_session_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `sis.promotion.based_on_exam_session_id`

<a id="assessmentexam_type"></a>
### assessment.exam_type

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `code` | varchar(?) | Y |  |  |  |
| 4 | `name` | varchar(?) | Y |  |  |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |
| 11 | `sort_order` | int4 | Y | 0 |  |  |
| 12 | `is_active` | bool | Y | TRUE |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_exam_type_scope_code` (`tenant_id`, `code`) WHERE deleted_at IS NULL

**Relationships**

- ← `assessment.exam_session.exam_type_id` — referenced by (1:N)

<a id="assessmentgrading_band"></a>
### assessment.grading_band

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `grading_scheme_id` | uuid | Y |  | FK → `assessment.grading_scheme` · CASCADE · tenant-composite |  |
| 4 | `min_percent` | numeric(?,?) | Y |  |  |  |
| 5 | `max_percent` | numeric(?,?) | Y |  |  |  |
| 6 | `grade_label` | varchar(?) | Y |  |  |  |
| 7 | `grade_point` | numeric(?,?) |  |  |  |  |
| 8 | `is_pass` | bool | Y | TRUE |  |  |
| 9 | `description` | text |  |  |  |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_grading_band_1` (`grading_scheme_id`, `grade_label`) WHERE deleted_at IS NULL

**Checks**

- `ck_grading_band_1`: min_percent >= 0 AND max_percent <= 100 AND max_percent >= min_percent

**Relationships**

- ← `assessment.result_template_band.grading_band_id` — referenced by (1:N)

<a id="assessmentgrading_scheme"></a>
### assessment.grading_scheme

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `academic_year_id` | uuid |  |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `name` | varchar(?) | Y |  |  |  |
| 6 | `scheme_type` | varchar(?) | Y |  |  |  |
| 7 | `grading_method` | varchar(?) | Y | 'percentage_band' |  |  |
| 8 | `criteria` | text |  |  |  |  |
| 9 | `is_active` | bool | Y | TRUE |  |  |
| 10 | `is_template` | bool | Y | FALSE |  |  |
| 11 | `created_at` | timestamptz | Y | now() |  |  |
| 12 | `created_by` | uuid |  |  |  |  |
| 13 | `updated_at` | timestamptz | Y | now() |  |  |
| 14 | `updated_by` | uuid |  |  |  |  |
| 15 | `deleted_at` | timestamptz |  |  |  |  |
| 16 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `ix_grading_scheme_1` (`institution_id`, `COALESCE(academic_year_id, CAST('00000000-0000-0000-0000-000000000000' AS uuid))`, `scheme_type`, `name`) WHERE deleted_at IS NULL

**Checks**

- `ck_grading_scheme_scheme_type`: scheme_type IN ('scholastic', 'co_scholastic')
- `ck_grading_scheme_grading_method`: grading_method IN ('percentage_band', 'cbse', 'pass_fail', 'descriptive')

**Relationships**

- ← `assessment.grading_band.grading_scheme_id` — referenced by (1:N)
- ← `assessment.result_template.grading_scheme_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `lms.cohort.grading_scheme_id`
- ← `lms.course.grading_scheme_id`

<a id="assessmenthomework"></a>
### assessment.homework

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `academic_year_id` | uuid | Y |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `class_section_id` | uuid | Y |  | logical → `academics.class_section` (cross-module, no FK) |  |
| 6 | `subject_id` | uuid |  |  | logical → `academics.subject` (cross-module, no FK) |  |
| 7 | `assigned_by_staff_id` | uuid |  |  | logical → `hr.staff` (cross-module, no FK) |  |
| 8 | `description` | text |  |  |  |  |
| 9 | `attachment_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 10 | `assigned_on` | timestamptz |  |  |  |  |
| 11 | `due_at` | timestamptz |  |  |  |  |
| 12 | `status` | varchar(?) | Y | 'draft' |  |  |
| 13 | `created_at` | timestamptz | Y | now() |  |  |
| 14 | `created_by` | uuid |  |  |  |  |
| 15 | `updated_at` | timestamptz | Y | now() |  |  |
| 16 | `updated_by` | uuid |  |  |  |  |
| 17 | `deleted_at` | timestamptz |  |  |  |  |
| 18 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_homework_status`: status IN ('draft', 'pending', 'published')
- index `ix_homework_pending` (`tenant_id`, `institution_id`) WHERE status = 'pending' AND deleted_at IS NULL — the approval queue

**Relationships**

- ← `assessment.homework_approval.homework_id` — referenced by (1:N)
- ← `assessment.homework_submission.homework_id` — referenced by (1:N)

<a id="assessmenthomework_approval"></a>
### assessment.homework_approval

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `homework_id` | uuid | Y |  | FK → `assessment.homework` · CASCADE · tenant-composite |  |
| 4 | `decision` | varchar(?) | Y | 'pending' |  |  |
| 5 | `comments` | text |  |  |  |  |
| 6 | `decided_by_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 7 | `decided_at` | timestamptz |  |  |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_homework_approval_decision`: decision IN ('pending', 'approved', 'rejected')
- unique index `uq_homework_approval_pending` (`homework_id`) WHERE decision = 'pending' AND deleted_at IS NULL — one open review per homework

<a id="assessmenthomework_submission"></a>
### assessment.homework_submission

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `homework_id` | uuid | Y |  | FK → `assessment.homework` · RESTRICT · tenant-composite |  |
| 4 | `student_id` | uuid | Y |  | logical → `sis.student` (cross-module, no FK) |  |
| 5 | `submitted_on` | date |  |  |  |  |
| 6 | `status` | varchar(?) | Y | 'unchecked' |  |  |
| 7 | `checked_by_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 8 | `checked_on` | date |  |  |  |  |
| 9 | `teacher_comments` | text |  |  |  |  |
| 10 | `student_reply` | text |  |  |  |  |
| 11 | `created_at` | timestamptz | Y | now() |  |  |
| 12 | `created_by` | uuid |  |  |  |  |
| 13 | `updated_at` | timestamptz | Y | now() |  |  |
| 14 | `updated_by` | uuid |  |  |  |  |
| 15 | `deleted_at` | timestamptz |  |  |  |  |
| 16 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_homework_submission_1` (`homework_id`, `student_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_homework_submission_status`: status IN ('unchecked', 'checked')

<a id="assessmentidempotency_key"></a>
### assessment.idempotency_key

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `idempotency_key` | uuid | Y |  |  |  |
| 4 | `user_id` | uuid |  |  |  |  |
| 5 | `operation_name` | varchar(?) | Y |  |  |  |
| 6 | `request_hash` | varchar(?) |  |  |  |  |
| 7 | `response_status` | int2 |  |  |  |  |
| 8 | `response_body` | text |  |  |  |  |
| 9 | `locked_at` | timestamptz |  |  |  |  |
| 10 | `completed_at` | timestamptz |  |  |  |  |
| 11 | `expires_at` | timestamptz | Y |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `idempotency_key`)

<a id="assessmentinbox_event"></a>
### assessment.inbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `event_id` | uuid | Y |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  | PK |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `received_at` | timestamptz | Y | now() |  |  |
| 5 | `processed_at` | timestamptz |  |  |  |  |
| 6 | `processing_error` | text |  |  |  |  |

<a id="assessmentmarksheet"></a>
### assessment.marksheet

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `exam_session_id` | uuid | Y |  | FK → `assessment.exam_session` · RESTRICT · tenant-composite |  |
| 4 | `result_template_id` | uuid | Y |  | FK → `assessment.result_template` · RESTRICT · tenant-composite |  |
| 5 | `name` | varchar(?) | Y |  |  |  |
| 6 | `generated_on` | date | Y |  |  |  |
| 7 | `generated_by_user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 8 | `status` | varchar(?) | Y | 'draft' |  |  |
| 9 | `total_passed` | int4 |  |  |  |  |
| 10 | `total_failed` | int4 |  |  |  |  |
| 11 | `created_at` | timestamptz | Y | now() |  |  |
| 12 | `created_by` | uuid |  |  |  |  |
| 13 | `updated_at` | timestamptz | Y | now() |  |  |
| 14 | `updated_by` | uuid |  |  |  |  |
| 15 | `deleted_at` | timestamptz |  |  |  |  |
| 16 | `row_version` | int4 | Y | 0 |  |  |
| 17 | `published_at` | timestamptz |  |  |  |  |
| 18 | `published_by_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 19 | `visible_to_guardians` | bool | Y | FALSE |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_marksheet_status`: status IN ('draft', 'validated', 'cancelled')

**Relationships**

- ← `assessment.marksheet_line.marksheet_id` — referenced by (1:N)

<a id="assessmentmarksheet_line"></a>
### assessment.marksheet_line

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `marksheet_id` | uuid | Y |  | FK → `assessment.marksheet` · RESTRICT · tenant-composite |  |
| 4 | `student_id` | uuid | Y |  | logical → `sis.student` (cross-module, no FK) |  |
| 5 | `total_marks` | numeric(?,?) |  |  |  |  |
| 6 | `percentage` | numeric(?,?) |  |  |  |  |
| 7 | `grade_label` | varchar(?) |  |  |  |  |
| 8 | `result_status` | varchar(?) |  |  |  |  |
| 9 | `generated_on` | date | Y |  |  |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |
| 16 | `credits_attempted` | numeric(?,?) |  |  |  |  |
| 17 | `credits_earned` | numeric(?,?) |  |  |  |  |
| 18 | `grade_points` | numeric(?,?) |  |  |  |  |
| 19 | `sgpa` | numeric(?,?) |  |  |  |  |
| 20 | `cgpa` | numeric(?,?) |  |  |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_marksheet_line_1` (`marksheet_id`, `student_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_marksheet_line_result_status`: result_status IN ('pass', 'fail')

**Relationships**

- ← `assessment.exam_result.marksheet_line_id` — referenced by (1:N)

<a id="assessmentoperation"></a>
### assessment.operation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `operation_name` | varchar(?) | Y |  |  |  |
| 4 | `resource_type` | varchar(?) |  |  |  |  |
| 5 | `resource_id` | uuid |  |  |  |  |
| 6 | `status` | varchar(?) | Y | 'running' |  |  |
| 7 | `progress_percent` | int2 |  |  |  |  |
| 8 | `error_code` | varchar(?) |  |  |  |  |
| 9 | `error_message` | varchar(?) |  |  |  |  |
| 10 | `result_entity_type` | varchar(?) |  |  |  |  |
| 11 | `result_entity_id` | uuid |  |  |  |  |
| 12 | `started_by_user_id` | uuid |  |  |  |  |
| 13 | `started_at` | timestamptz | Y | now() |  |  |
| 14 | `finished_at` | timestamptz |  |  |  |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_assessment_operation_status`: status IN ('running', 'succeeded', 'failed', 'cancelled')

<a id="assessmentoutbox_event"></a>
### assessment.outbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | int8 |  |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `aggregate_type` | varchar(?) | Y |  |  |  |
| 5 | `aggregate_id` | uuid | Y |  |  |  |
| 6 | `payload` | text | Y |  |  |  |
| 7 | `occurred_at` | timestamptz | Y | now() |  |  |
| 8 | `published_at` | timestamptz |  |  |  |  |
| 9 | `publish_error` | text |  |  |  | Why the relay set the row aside (not published) |

<a id="assessmentresult_change_log"></a>
### assessment.result_change_log

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `exam_result_id` | uuid | Y |  | FK → `assessment.exam_result` · RESTRICT · tenant-composite |  |
| 4 | `changed_by_user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `changed_at` | timestamptz | Y | now() |  |  |
| 6 | `old_marks` | numeric(?,?) |  |  |  |  |
| 7 | `new_marks` | numeric(?,?) |  |  |  |  |
| 8 | `old_result_status` | varchar(?) |  |  |  |  |
| 9 | `new_result_status` | varchar(?) |  |  |  |  |
| 10 | `old_grade_label` | varchar(?) |  |  |  |  |
| 11 | `new_grade_label` | varchar(?) |  |  |  |  |
| 12 | `reason` | varchar(?) | Y |  |  |  |

<a id="assessmentresult_template"></a>
### assessment.result_template

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `exam_session_id` | uuid | Y |  | FK → `assessment.exam_session` · CASCADE · tenant-composite |  |
| 4 | `grading_scheme_id` | uuid |  |  | FK → `assessment.grading_scheme` · RESTRICT · tenant-composite |  |
| 5 | `name` | varchar(?) | Y |  |  |  |
| 6 | `result_date` | date | Y |  |  |  |
| 7 | `status` | varchar(?) | Y | 'draft' |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_result_template_status`: status IN ('draft', 'result_generated')

**Relationships**

- ← `assessment.marksheet.result_template_id` — referenced by (1:N)
- ← `assessment.result_template_band.result_template_id` — referenced by (1:N)

<a id="assessmentresult_template_band"></a>
### assessment.result_template_band

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `result_template_id` | uuid | Y |  | FK → `assessment.result_template` · CASCADE · tenant-composite |  |
| 4 | `grading_band_id` | uuid | Y |  | FK → `assessment.grading_band` · RESTRICT · tenant-composite |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_result_template_band_1` (`result_template_id`, `grading_band_id`) WHERE deleted_at IS NULL

## `lms` — Learning Management

Learning Management: Online courses (chapters, lessons, SCORM), enrolments and progress, cohorts with live classes and timetables, quizzes, assignments, programming exercises, learning programs, evaluations, certificates and badges. LMS participation is not school attendance and LMS grades are not official results.

133 tables: [`access_log`](#lmsaccess_log), [`annotation`](#lmsannotation), [`annotation_share`](#lmsannotation_share), [`assignment`](#lmsassignment), [`assignment_group`](#lmsassignment_group), [`assignment_group_member`](#lmsassignment_group_member), [`assignment_submission`](#lmsassignment_submission), [`badge`](#lmsbadge), [`badge_award`](#lmsbadge_award), [`bookmark`](#lmsbookmark), [`catalog_banner`](#lmscatalog_banner), [`category`](#lmscategory), [`certificate`](#lmscertificate), [`certificate_evaluation`](#lmscertificate_evaluation), [`certificate_request`](#lmscertificate_request), [`chapter`](#lmschapter), [`cohort`](#lmscohort), [`cohort_course`](#lmscohort_course), [`cohort_enrollment`](#lmscohort_enrollment), [`cohort_feedback`](#lmscohort_feedback), [`cohort_group`](#lmscohort_group), [`cohort_holiday`](#lmscohort_holiday), [`cohort_schedule_slot`](#lmscohort_schedule_slot), [`cohort_session`](#lmscohort_session), [`cohort_teacher`](#lmscohort_teacher), [`conferencing_account`](#lmsconferencing_account), [`conferencing_token`](#lmsconferencing_token), [`content_author`](#lmscontent_author), [`content_item`](#lmscontent_item), [`content_item_rating`](#lmscontent_item_rating), [`content_view_session`](#lmscontent_view_session), [`course`](#lmscourse), [`course_author_profile`](#lmscourse_author_profile), [`course_interest`](#lmscourse_interest), [`course_mentor`](#lmscourse_mentor), [`course_preview`](#lmscourse_preview), [`course_review`](#lmscourse_review), [`course_teacher`](#lmscourse_teacher), [`course_testimonial`](#lmscourse_testimonial), [`discussion_prompt`](#lmsdiscussion_prompt), [`drive_item`](#lmsdrive_item), [`drive_item_share`](#lmsdrive_item_share), [`ebook`](#lmsebook), [`enrollment`](#lmsenrollment), [`enrollment_request`](#lmsenrollment_request), [`evaluator`](#lmsevaluator), [`evaluator_availability`](#lmsevaluator_availability), [`exercise_submission`](#lmsexercise_submission), [`exercise_test_case`](#lmsexercise_test_case), [`exercise_test_result`](#lmsexercise_test_result), [`external_tool`](#lmsexternal_tool), [`external_tool_institution`](#lmsexternal_tool_institution), [`external_tool_link`](#lmsexternal_tool_link), [`external_tool_option`](#lmsexternal_tool_option), [`external_tool_resource`](#lmsexternal_tool_resource), [`external_tool_role`](#lmsexternal_tool_role), [`external_tool_setting`](#lmsexternal_tool_setting), [`forum`](#lmsforum), [`forum_post`](#lmsforum_post), [`forum_post_report`](#lmsforum_post_report), [`forum_post_vote`](#lmsforum_post_vote), [`forum_read_state`](#lmsforum_read_state), [`forum_report_category`](#lmsforum_report_category), [`forum_section`](#lmsforum_section), [`forum_topic`](#lmsforum_topic), [`grade_change_log`](#lmsgrade_change_log), [`gradebook_entry`](#lmsgradebook_entry), [`grading_category`](#lmsgrading_category), [`idempotency_key`](#lmsidempotency_key), [`import_export_job`](#lmsimport_export_job), [`inbox_event`](#lmsinbox_event), [`item_override`](#lmsitem_override), [`learning_objective`](#lmslearning_objective), [`learning_program`](#lmslearning_program), [`learning_program_course`](#lmslearning_program_course), [`learning_program_member`](#lmslearning_program_member), [`lesson`](#lmslesson), [`lesson_activity`](#lmslesson_activity), [`lesson_progress`](#lmslesson_progress), [`live_class`](#lmslive_class), [`live_class_participant`](#lmslive_class_participant), [`mentorship`](#lmsmentorship), [`number_sequence`](#lmsnumber_sequence), [`operation`](#lmsoperation), [`outbox_event`](#lmsoutbox_event), [`participation_activity`](#lmsparticipation_activity), [`peer_review`](#lmspeer_review), [`plagiarism_account`](#lmsplagiarism_account), [`plagiarism_cohort_link`](#lmsplagiarism_cohort_link), [`plagiarism_report`](#lmsplagiarism_report), [`plagiarism_user_link`](#lmsplagiarism_user_link), [`programming_exercise`](#lmsprogramming_exercise), [`question`](#lmsquestion), [`question_accepted_answer`](#lmsquestion_accepted_answer), [`question_blank`](#lmsquestion_blank), [`question_blank_answer`](#lmsquestion_blank_answer), [`question_hotspot`](#lmsquestion_hotspot), [`question_hotspot_region`](#lmsquestion_hotspot_region), [`question_match_pair`](#lmsquestion_match_pair), [`question_option`](#lmsquestion_option), [`question_pattern`](#lmsquestion_pattern), [`question_pool`](#lmsquestion_pool), [`question_pool_item`](#lmsquestion_pool_item), [`quiz`](#lmsquiz), [`quiz_answer`](#lmsquiz_answer), [`quiz_question`](#lmsquiz_question), [`quiz_question_history`](#lmsquiz_question_history), [`quiz_submission`](#lmsquiz_submission), [`quiz_violation`](#lmsquiz_violation), [`reassignment_log`](#lmsreassignment_log), [`related_course`](#lmsrelated_course), [`related_material`](#lmsrelated_material), [`related_material_rank`](#lmsrelated_material_rank), [`related_material_rank_item`](#lmsrelated_material_rank_item), [`release_grant`](#lmsrelease_grant), [`release_rule`](#lmsrelease_rule), [`release_rule_condition`](#lmsrelease_rule_condition), [`release_rule_target`](#lmsrelease_rule_target), [`resource_category`](#lmsresource_category), [`rubric`](#lmsrubric), [`rubric_assessment`](#lmsrubric_assessment), [`rubric_cell`](#lmsrubric_cell), [`rubric_criterion`](#lmsrubric_criterion), [`rubric_level`](#lmsrubric_level), [`settings`](#lmssettings), [`settings_primary_country`](#lmssettings_primary_country), [`settings_sidebar_item`](#lmssettings_sidebar_item), [`student_alert`](#lmsstudent_alert), [`timetable_entry`](#lmstimetable_entry), [`timetable_legend`](#lmstimetable_legend), [`timetable_template`](#lmstimetable_template), [`video_watch`](#lmsvideo_watch), [`web_resource`](#lmsweb_resource)

<a id="lmsaccess_log"></a>
### lms.access_log

*Partitioned by RANGE (accessed_at); monthly partitions plus a default partition.*

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 4 | `accessed_at` | timestamptz | Y |  | PK |  |
| 5 | `operation` | varchar(?) |  |  |  |  |
| 6 | `url` | varchar(?) |  |  |  |  |
| 7 | `params` | text |  |  |  |  |
| 8 | `is_async` | bool | Y | FALSE |  |  |
| 9 | `session_ref` | varchar(?) |  |  |  |  |
| 10 | `cohort_id` | uuid |  |  | FK → `lms.cohort` · SET NULL · tenant-composite |  |
| 11 | `role_name` | varchar(?) |  |  |  |  |
| 12 | `course_week` | varchar(?) |  |  |  |  |
| 13 | `analytics_link_ref` | varchar(?) |  |  |  |  |
| 14 | `user_agent` | varchar(?) |  |  |  |  |
| 15 | `referer` | varchar(?) |  |  |  |  |
| 16 | `ip_address` | inet |  |  |  |  |

<a id="lmsannotation"></a>
### lms.annotation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 4 | `content_item_id` | uuid |  |  | FK → `lms.content_item` · CASCADE · tenant-composite |  |
| 5 | `cohort_id` | uuid |  |  | FK → `lms.cohort` · CASCADE · tenant-composite |  |
| 6 | `ebook_id` | uuid |  |  | FK → `lms.ebook` · CASCADE · tenant-composite |  |
| 7 | `file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 8 | `annotation_type` | varchar(?) | Y |  |  |  |
| 9 | `annotation_subtype` | varchar(?) |  |  |  |  |
| 10 | `page_number` | varchar(?) |  |  |  |  |
| 11 | `start_char_pos` | varchar(?) |  |  |  |  |
| 12 | `char_count` | varchar(?) |  |  |  |  |
| 13 | `location` | text |  |  |  |  |
| 14 | `annotated_text` | text |  |  |  |  |
| 15 | `title` | varchar(?) |  |  |  |  |
| 16 | `description` | text |  |  |  |  |
| 17 | `embed_code` | text |  |  |  |  |
| 18 | `url` | varchar(?) |  |  |  |  |
| 19 | `media_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 20 | `visibility` | varchar(?) | Y | 'private' |  |  |
| 21 | `forum_topic_id` | uuid |  |  | FK → `lms.forum_topic` · SET NULL · tenant-composite |  |
| 22 | `created_at` | timestamptz | Y | now() |  |  |
| 23 | `created_by` | uuid |  |  |  |  |
| 24 | `updated_at` | timestamptz | Y | now() |  |  |
| 25 | `updated_by` | uuid |  |  |  |  |
| 26 | `deleted_at` | timestamptz |  |  |  |  |
| 27 | `row_version` | int4 | Y | 0 |  |  |
| 28 | `lesson_id` | uuid |  |  | FK → `lms.lesson` · CASCADE · tenant-composite |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_annotation_annotation_type`: annotation_type IN ('highlight', 'note', 'link', 'media', 'embed', 'bookmark', 'question')
- `ck_annotation_visibility`: visibility IN ('private', 'shared_with_users', 'cohort')
- `ck_annotation_lesson_anchor`: lesson_id IS NULL OR num_nonnulls(content_item_id, ebook_id, file_id) = 0

**Relationships**

- ← `lms.annotation_share.annotation_id` — referenced by (1:N)

<a id="lmsannotation_share"></a>
### lms.annotation_share

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `annotation_id` | uuid | Y |  | FK → `lms.annotation` · CASCADE · tenant-composite |  |
| 4 | `shared_with_user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `shared_at` | timestamptz | Y | now() |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_annotation_share_1` (`annotation_id`, `shared_with_user_id`) WHERE deleted_at IS NULL

<a id="lmsassignment"></a>
### lms.assignment

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `title` | varchar(?) | Y |  |  |  |
| 4 | `question` | text | Y |  |  |  |
| 5 | `submission_type` | varchar(?) | Y |  |  |  |
| 6 | `course_id` | uuid |  |  | FK → `lms.course` · SET NULL · tenant-composite |  |
| 7 | `is_graded` | bool | Y | TRUE |  |  |
| 8 | `show_model_answer` | bool | Y | FALSE |  |  |
| 9 | `model_answer` | text |  |  |  |  |
| 10 | `scheduling_enabled` | bool | Y | FALSE |  |  |
| 11 | `schedule_start` | timestamptz |  |  |  |  |
| 12 | `schedule_end` | timestamptz |  |  |  |  |
| 13 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 14 | `submission_mode` | varchar(?) | Y | 'individual' |  |  |
| 15 | `min_group_size` | int4 |  |  |  |  |
| 16 | `max_group_size` | int4 |  |  |  |  |
| 17 | `peer_review_points` | numeric(?,?) |  |  |  |  |
| 18 | `uses_inline_authoring` | bool | Y | FALSE |  |  |
| 19 | `is_in_repository` | bool | Y | FALSE |  |  |
| 20 | `created_at` | timestamptz | Y | now() |  |  |
| 21 | `created_by` | uuid |  |  |  |  |
| 22 | `updated_at` | timestamptz | Y | now() |  |  |
| 23 | `updated_by` | uuid |  |  |  |  |
| 24 | `deleted_at` | timestamptz |  |  |  |  |
| 25 | `row_version` | int4 | Y | 0 |  |  |
| 26 | `version` | int4 | Y | 1 |  |  |
| 27 | `superseded_by_id` | uuid |  |  | FK → `lms.assignment` · SET NULL · tenant-composite |  |
| 28 | `published_at` | timestamptz |  |  |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_assignment_submission_type`: submission_type IN ('document', 'pdf', 'url', 'image', 'text')
- `ck_assignment_submission_mode`: submission_mode IN ('individual', 'group')
- `ck_assignment_1`: min_group_size IS NULL OR max_group_size IS NULL OR max_group_size >= min_group_size

**Relationships**

- ← `lms.assignment.superseded_by_id` — referenced by (1:N)
- ← `lms.assignment_submission.assignment_id` — referenced by (1:N)
- ← `lms.content_author.assignment_id` — referenced by (1:N)
- ← `lms.content_item.assignment_id` — referenced by (1:N)
- ← `lms.lesson_activity.assignment_id` — referenced by (1:N)
- ← `lms.timetable_entry.assignment_id` — referenced by (1:N)

<a id="lmsassignment_group"></a>
### lms.assignment_group

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `content_item_id` | uuid | Y |  | FK → `lms.content_item` · CASCADE · tenant-composite |  |
| 4 | `cohort_id` | uuid | Y |  | FK → `lms.cohort` · CASCADE · tenant-composite |  |
| 5 | `name` | varchar(?) | Y |  |  |  |
| 6 | `points` | numeric(?,?) |  |  |  |  |
| 7 | `is_active` | bool | Y | TRUE |  |  |
| 8 | `discussion_topic_id` | uuid |  |  | FK → `lms.forum_topic` · SET NULL · tenant-composite |  |
| 9 | `is_reassigned` | bool | Y | FALSE |  |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_assignment_group_1` (`content_item_id`, `name`) WHERE deleted_at IS NULL

**Relationships**

- ← `lms.assignment_group_member.assignment_group_id` — referenced by (1:N)
- ← `lms.assignment_submission.group_id` — referenced by (1:N)
- ← `lms.gradebook_entry.group_id` — referenced by (1:N)
- ← `lms.item_override.group_id` — referenced by (1:N)
- ← `lms.peer_review.assignment_group_id` — referenced by (1:N)
- ← `lms.reassignment_log.group_id` — referenced by (1:N)
- ← `lms.rubric_assessment.group_id` — referenced by (1:N)

<a id="lmsassignment_group_member"></a>
### lms.assignment_group_member

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `assignment_group_id` | uuid | Y |  | FK → `lms.assignment_group` · CASCADE · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `content_item_id` | uuid | Y |  | FK → `lms.content_item` · CASCADE · tenant-composite |  |
| 6 | `status` | varchar(?) | Y | 'active' |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `ix_assignment_group_member_1` (`content_item_id`, `user_id`) WHERE status = 'active' AND deleted_at IS NULL
- unique index `ix_assignment_group_member_2` (`assignment_group_id`, `user_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_assignment_group_member_status`: status IN ('requested', 'active', 'rejected', 'left', 'removed')

<a id="lmsassignment_submission"></a>
### lms.assignment_submission

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `assignment_id` | uuid | Y |  | FK → `lms.assignment` · RESTRICT · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `lesson_id` | uuid |  |  | FK → `lms.lesson` · SET NULL · tenant-composite |  |
| 6 | `course_id` | uuid |  |  | FK → `lms.course` · SET NULL · tenant-composite |  |
| 7 | `submission_type` | varchar(?) |  |  |  |  |
| 8 | `question_snapshot` | text |  |  |  |  |
| 9 | `answer` | text |  |  |  |  |
| 10 | `attachment_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 11 | `status` | varchar(?) | Y | 'not_graded' |  |  |
| 12 | `evaluator_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 13 | `evaluator_comments` | text |  |  |  |  |
| 14 | `content_item_id` | uuid |  |  | FK → `lms.content_item` · SET NULL · tenant-composite |  |
| 15 | `group_id` | uuid |  |  | FK → `lms.assignment_group` · SET NULL · tenant-composite |  |
| 16 | `title` | varchar(?) |  |  |  |  |
| 17 | `submitted_at` | timestamptz |  |  |  |  |
| 18 | `is_final` | bool | Y | TRUE |  |  |
| 19 | `finalized_at` | timestamptz |  |  |  |  |
| 20 | `created_at` | timestamptz | Y | now() |  |  |
| 21 | `created_by` | uuid |  |  |  |  |
| 22 | `updated_at` | timestamptz | Y | now() |  |  |
| 23 | `updated_by` | uuid |  |  |  |  |
| 24 | `deleted_at` | timestamptz |  |  |  |  |
| 25 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_assignment_submission_submission_type`: submission_type IN ('document', 'pdf', 'url', 'image', 'text')
- `ck_assignment_submission_status`: status IN ('not_graded', 'pass', 'fail', 'not_applicable')

<a id="lmsbadge"></a>
### lms.badge

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `title` | varchar(?) | Y |  |  |  |
| 4 | `description` | text | Y |  |  |  |
| 5 | `image_file_id` | uuid | Y |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 6 | `target_entity` | varchar(?) | Y |  |  |  |
| 7 | `trigger_event` | varchar(?) | Y |  |  |  |
| 8 | `condition_expr` | text | Y |  |  |  |
| 9 | `field_to_check` | varchar(?) |  |  |  |  |
| 10 | `user_field` | varchar(?) | Y |  |  |  |
| 11 | `grant_only_once` | bool | Y | FALSE |  |  |
| 12 | `is_enabled` | bool | Y | TRUE |  |  |
| 13 | `created_at` | timestamptz | Y | now() |  |  |
| 14 | `created_by` | uuid |  |  |  |  |
| 15 | `updated_at` | timestamptz | Y | now() |  |  |
| 16 | `updated_by` | uuid |  |  |  |  |
| 17 | `deleted_at` | timestamptz |  |  |  |  |
| 18 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_badge_1` (`tenant_id`, `title`) WHERE deleted_at IS NULL

**Checks**

- `ck_badge_trigger_event`: trigger_event IN ('new', 'value_change', 'manual')

**Relationships**

- ← `lms.badge_award.badge_id` — referenced by (1:N)

<a id="lmsbadge_award"></a>
### lms.badge_award

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `badge_id` | uuid | Y |  | FK → `lms.badge` · RESTRICT · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `issued_on` | date | Y |  |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

<a id="lmsbookmark"></a>
### lms.bookmark

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 4 | `title` | varchar(?) | Y |  |  |  |
| 5 | `url` | varchar(?) | Y |  |  |  |
| 6 | `module_code` | varchar(?) |  |  |  |  |
| 7 | `cohort_id` | uuid |  |  | FK → `lms.cohort` · CASCADE · tenant-composite |  |
| 8 | `saved_at` | timestamptz | Y | now() |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

<a id="lmscatalog_banner"></a>
### lms.catalog_banner

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `course_id` | uuid | Y |  | FK → `lms.course` · CASCADE · tenant-composite |  |
| 4 | `placement` | varchar(?) | Y |  |  |  |
| 5 | `image_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 6 | `sort_order` | int4 | Y | 0 |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_catalog_banner_placement`: placement IN ('featured', 'showcase')

<a id="lmscategory"></a>
### lms.category

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `name` | varchar(?) | Y |  |  |  |
| 4 | `created_at` | timestamptz | Y | now() |  |  |
| 5 | `created_by` | uuid |  |  |  |  |
| 6 | `updated_at` | timestamptz | Y | now() |  |  |
| 7 | `updated_by` | uuid |  |  |  |  |
| 8 | `deleted_at` | timestamptz |  |  |  |  |
| 9 | `row_version` | int4 | Y | 0 |  |  |
| 10 | `code` | varchar(?) | Y |  |  |  |
| 11 | `sort_order` | int4 | Y | 0 |  |  |
| 12 | `is_active` | bool | Y | TRUE |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_category_scope_code` (`tenant_id`, `code`) WHERE deleted_at IS NULL
- unique index `uq_category_1` (`tenant_id`, `name`) WHERE deleted_at IS NULL

**Relationships**

- ← `lms.cohort.category_id` — referenced by (1:N)
- ← `lms.course.category_id` — referenced by (1:N)

<a id="lmscertificate"></a>
### lms.certificate

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 4 | `course_id` | uuid |  |  | FK → `lms.course` · RESTRICT · tenant-composite |  |
| 5 | `cohort_id` | uuid |  |  | FK → `lms.cohort` · RESTRICT · tenant-composite |  |
| 6 | `evaluator_id` | uuid |  |  | FK → `lms.evaluator` · SET NULL · tenant-composite |  |
| 7 | `certificate_number` | varchar(?) |  |  |  |  |
| 8 | `issue_date` | date | Y |  |  |  |
| 9 | `expiry_date` | date |  |  |  |  |
| 10 | `template_code` | varchar(?) | Y |  |  |  |
| 11 | `is_published` | bool | Y | FALSE |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_certificate_1`: course_id IS NOT NULL OR cohort_id IS NOT NULL

**Relationships**

- ← `lms.enrollment.certificate_id` — referenced by (1:N)

<a id="lmscertificate_evaluation"></a>
### lms.certificate_evaluation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `course_id` | uuid | Y |  | FK → `lms.course` · RESTRICT · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `cohort_id` | uuid |  |  | FK → `lms.cohort` · SET NULL · tenant-composite |  |
| 6 | `certificate_request_id` | uuid |  |  | FK → `lms.certificate_request` · SET NULL · tenant-composite |  |
| 7 | `evaluator_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 8 | `evaluation_date` | date | Y |  |  |  |
| 9 | `start_time` | time | Y |  |  |  |
| 10 | `end_time` | time |  |  |  |  |
| 11 | `rating` | numeric(?,?) |  |  |  |  |
| 12 | `summary` | text |  |  |  |  |
| 13 | `status` | varchar(?) | Y | 'pending' |  |  |
| 14 | `created_at` | timestamptz | Y | now() |  |  |
| 15 | `created_by` | uuid |  |  |  |  |
| 16 | `updated_at` | timestamptz | Y | now() |  |  |
| 17 | `updated_by` | uuid |  |  |  |  |
| 18 | `deleted_at` | timestamptz |  |  |  |  |
| 19 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_certificate_evaluation_status`: status IN ('pending', 'in_progress', 'pass', 'fail')

<a id="lmscertificate_request"></a>
### lms.certificate_request

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `course_id` | uuid | Y |  | FK → `lms.course` · RESTRICT · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `cohort_id` | uuid |  |  | FK → `lms.cohort` · SET NULL · tenant-composite |  |
| 6 | `evaluator_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 7 | `request_date` | date | Y |  |  |  |
| 8 | `weekday` | varchar(?) |  |  |  |  |
| 9 | `start_time` | time | Y |  |  |  |
| 10 | `end_time` | time | Y |  |  |  |
| 11 | `timezone` | varchar(?) |  |  |  |  |
| 12 | `meeting_link_enc` | text |  |  |  |  |
| 13 | `status` | varchar(?) | Y | 'upcoming' |  |  |
| 14 | `created_at` | timestamptz | Y | now() |  |  |
| 15 | `created_by` | uuid |  |  |  |  |
| 16 | `updated_at` | timestamptz | Y | now() |  |  |
| 17 | `updated_by` | uuid |  |  |  |  |
| 18 | `deleted_at` | timestamptz |  |  |  |  |
| 19 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_certificate_request_weekday`: weekday IN ('monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday')
- `ck_certificate_request_status`: status IN ('upcoming', 'completed', 'cancelled')

**Relationships**

- ← `lms.certificate_evaluation.certificate_request_id` — referenced by (1:N)

<a id="lmschapter"></a>
### lms.chapter

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `course_id` | uuid | Y |  | FK → `lms.course` · CASCADE · tenant-composite |  |
| 4 | `title` | varchar(?) | Y |  |  |  |
| 5 | `sort_order` | int4 | Y |  |  |  |
| 6 | `is_scorm_package` | bool | Y | FALSE |  |  |
| 7 | `scorm_package_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 8 | `scorm_package_path` | text |  |  |  |  |
| 9 | `scorm_manifest_file` | text |  |  |  |  |
| 10 | `scorm_launch_file` | text |  |  |  |  |
| 11 | `cohort_id` | uuid |  |  | FK → `lms.cohort` · CASCADE · tenant-composite |  |
| 12 | `parent_chapter_id` | uuid |  |  | FK → `lms.chapter` · CASCADE · tenant-composite |  |
| 13 | `description` | text |  |  |  |  |
| 14 | `chapter_number` | int4 |  |  |  |  |
| 15 | `start_day` | int4 |  |  |  |  |
| 16 | `start_time` | time |  |  |  |  |
| 17 | `due_day` | int4 |  |  |  |  |
| 18 | `due_time` | time |  |  |  |  |
| 19 | `starts_at` | timestamptz |  |  |  |  |
| 20 | `ends_at` | timestamptz |  |  |  |  |
| 21 | `allow_delete` | bool | Y | TRUE |  |  |
| 22 | `allow_edit_details` | bool | Y | TRUE |  |  |
| 23 | `allow_edit_dates` | bool | Y | TRUE |  |  |
| 24 | `copied_from_id` | uuid |  |  | FK → `lms.chapter` · SET NULL · tenant-composite |  |
| 25 | `is_original` | bool | Y | TRUE |  |  |
| 26 | `created_at` | timestamptz | Y | now() |  |  |
| 27 | `created_by` | uuid |  |  |  |  |
| 28 | `updated_at` | timestamptz | Y | now() |  |  |
| 29 | `updated_by` | uuid |  |  |  |  |
| 30 | `deleted_at` | timestamptz |  |  |  |  |
| 31 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- UNIQUE (`id`, `course_id`)
- unique index `ix_chapter_1` (`course_id`, `COALESCE(cohort_id, CAST('00000000-0000-0000-0000-000000000000' AS uuid))`, `COALESCE(parent_chapter_id, CAST('00000000-0000-0000-0000-000000000000' AS uuid))`, `sort_order`) WHERE deleted_at IS NULL

**Checks**

- `ck_chapter_1`: NOT(cohort_id IS NULL AND copied_from_id IS NOT NULL)

**Relationships**

- ← `lms.chapter.copied_from_id` — referenced by (1:N)
- ← `lms.chapter.parent_chapter_id` — referenced by (1:N)
- ← `lms.content_item.chapter_id` — referenced by (1:N)
- ← `lms.import_export_job.target_chapter_id` — referenced by (1:N)
- ← `lms.learning_objective.chapter_id` — referenced by (1:N)
- ← `lms.lesson.chapter_id` — referenced by (1:N)
- ← `lms.lesson_progress.chapter_id` — referenced by (1:N)
- ← `lms.release_grant.chapter_id` — referenced by (1:N)
- ← `lms.release_rule_target.chapter_id` — referenced by (1:N)

<a id="lmscohort"></a>
### lms.cohort

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `title` | varchar(?) | Y |  |  |  |
| 5 | `description` | text |  |  |  |  |
| 6 | `details` | text |  |  |  |  |
| 7 | `category_id` | uuid |  |  | FK → `lms.category` · SET NULL · tenant-composite |  |
| 8 | `medium` | varchar(?) | Y | 'online' |  |  |
| 9 | `start_date` | date | Y |  |  |  |
| 10 | `end_date` | date | Y |  |  |  |
| 11 | `start_time` | time |  |  |  |  |
| 12 | `end_time` | time |  |  |  |  |
| 13 | `timezone` | varchar(?) | Y |  |  |  |
| 14 | `seat_count` | int4 |  |  |  |  |
| 15 | `is_published` | bool | Y | FALSE |  |  |
| 16 | `allow_self_enrollment` | bool | Y | FALSE |  |  |
| 17 | `allow_future_access` | bool | Y | TRUE |  |  |
| 18 | `is_paid` | bool | Y | FALSE |  |  |
| 19 | `price` | numeric(?,?) |  |  |  |  |
| 20 | `currency_id` | uuid |  |  | logical → `master.currency` (cross-module, no FK) |  |
| 21 | `price_usd` | numeric(?,?) |  |  |  |  |
| 22 | `show_live_class` | bool | Y | FALSE |  |  |
| 23 | `evaluation_enabled` | bool | Y | FALSE |  |  |
| 24 | `evaluation_end_date` | date |  |  |  |  |
| 25 | `certification_enabled` | bool | Y | FALSE |  |  |
| 26 | `conferencing_provider` | varchar(?) |  |  |  |  |
| 27 | `conferencing_account_id` | uuid |  |  | FK → `lms.conferencing_account` · SET NULL · tenant-composite |  |
| 28 | `timetable_template_id` | uuid |  |  | FK → `lms.timetable_template` · SET NULL · tenant-composite |  |
| 29 | `confirmation_template_id` | uuid |  |  | logical → `comms.message_template` (cross-module, no FK) |  |
| 30 | `meta_image_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 31 | `intro_video_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 32 | `custom_component` | text |  |  |  |  |
| 33 | `custom_script` | text |  |  |  |  |
| 34 | `publish_notification_sent` | bool | Y | FALSE |  |  |
| 35 | `code` | varchar(?) |  |  |  |  |
| 36 | `academic_term_id` | uuid |  |  | logical → `org.academic_term` (cross-module, no FK) |  |
| 37 | `program_id` | uuid |  |  | logical → `academics.program` (cross-module, no FK) |  |
| 38 | `external_ref` | varchar(?) |  |  |  |  |
| 39 | `lifecycle_status` | varchar(?) | Y | 'draft' |  |  |
| 40 | `staff_access_from` | timestamptz |  |  |  |  |
| 41 | `staff_access_until` | timestamptz |  |  |  |  |
| 42 | `student_access_from` | timestamptz |  |  |  |  |
| 43 | `student_access_until` | timestamptz |  |  |  |  |
| 44 | `has_schedule` | bool | Y | FALSE |  |  |
| 45 | `staff_can_edit_schedule` | bool | Y | FALSE |  |  |
| 46 | `grade_display` | varchar(?) | Y | 'points' |  |  |
| 47 | `grading_scheme_id` | uuid |  |  | logical → `assessment.grading_scheme` (cross-module, no FK) |  |
| 48 | `enrollment_opens_at` | timestamptz |  |  |  |  |
| 49 | `enrollment_closes_at` | timestamptz |  |  |  |  |
| 50 | `created_at` | timestamptz | Y | now() |  |  |
| 51 | `created_by` | uuid |  |  |  |  |
| 52 | `updated_at` | timestamptz | Y | now() |  |  |
| 53 | `updated_by` | uuid |  |  |  |  |
| 54 | `deleted_at` | timestamptz |  |  |  |  |
| 55 | `row_version` | int4 | Y | 0 |  |  |
| 56 | `class_subject_id` | uuid |  |  | logical → `academics.class_subject` (cross-module, no FK) |  |
| 57 | `batch_id` | uuid |  |  | logical → `academics.batch` (cross-module, no FK) |  |
| 58 | `academic_year_id` | uuid |  |  | logical → `org.academic_year` (cross-module, no FK) |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_cohort_medium`: medium IN ('online', 'offline')
- `ck_cohort_conferencing_provider`: conferencing_provider IN ('zoom', 'google_meet')
- `ck_cohort_lifecycle_status`: lifecycle_status IN ('draft', 'active', 'suspended', 'completed', 'archived')
- `ck_cohort_grade_display`: grade_display IN ('points', 'percentage')
- `ck_cohort_1`: end_date >= start_date

**Relationships**

- ← `lms.access_log.cohort_id` — referenced by (1:N)
- ← `lms.annotation.cohort_id` — referenced by (1:N)
- ← `lms.assignment_group.cohort_id` — referenced by (1:N)
- ← `lms.bookmark.cohort_id` — referenced by (1:N)
- ← `lms.certificate.cohort_id` — referenced by (1:N)
- ← `lms.certificate_evaluation.cohort_id` — referenced by (1:N)
- ← `lms.certificate_request.cohort_id` — referenced by (1:N)
- ← `lms.chapter.cohort_id` — referenced by (1:N)
- ← `lms.cohort_course.cohort_id` — referenced by (1:N)
- ← `lms.cohort_enrollment.cohort_id` — referenced by (1:N)
- ← `lms.cohort_feedback.cohort_id` — referenced by (1:N)
- ← `lms.cohort_group.cohort_id` — referenced by (1:N)
- ← `lms.cohort_holiday.cohort_id` — referenced by (1:N)
- ← `lms.cohort_schedule_slot.cohort_id` — referenced by (1:N)
- ← `lms.cohort_session.cohort_id` — referenced by (1:N)
- ← `lms.cohort_teacher.cohort_id` — referenced by (1:N)
- ← `lms.content_item.cohort_id` — referenced by (1:N)
- ← `lms.content_item_rating.cohort_id` — referenced by (1:N)
- ← `lms.content_view_session.cohort_id` — referenced by (1:N)
- ← `lms.enrollment.source_cohort_id` — referenced by (1:N)
- ← `lms.enrollment_request.cohort_id` — referenced by (1:N)
- ← `lms.forum.cohort_id` — referenced by (1:N)
- ← `lms.gradebook_entry.cohort_id` — referenced by (1:N)
- ← `lms.grading_category.cohort_id` — referenced by (1:N)
- ← `lms.import_export_job.cohort_id` — referenced by (1:N)
- ← `lms.live_class.cohort_id` — referenced by (1:N)
- ← `lms.plagiarism_cohort_link.cohort_id` — referenced by (1:N)
- ← `lms.quiz_submission.cohort_id` — referenced by (1:N)
- ← `lms.release_rule.cohort_id` — referenced by (1:N)
- ← `lms.student_alert.cohort_id` — referenced by (1:N)
- ← `lms.timetable_entry.cohort_id` — referenced by (1:N)
- ← `lms.timetable_legend.cohort_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `comms.calendar_export.cohort_id`
- ← `comms.event.cohort_id`
- ← `comms.event_occurrence.cohort_id`
- ← `comms.in_app_notification.cohort_id`
- ← `comms.notice.cohort_id`
- ← `comms.notification_cohort_setting.cohort_id`
- ← `finance.coupon_scope.lms_cohort_id`
- ← `finance.payment.lms_cohort_id`
- ← `platform.deletion_log.cohort_id`

<a id="lmscohort_course"></a>
### lms.cohort_course

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `cohort_id` | uuid | Y |  | FK → `lms.cohort` · CASCADE · tenant-composite |  |
| 4 | `course_id` | uuid | Y |  | FK → `lms.course` · RESTRICT · tenant-composite |  |
| 5 | `evaluator_id` | uuid |  |  | FK → `lms.evaluator` · SET NULL · tenant-composite |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_cohort_course_1` (`cohort_id`, `course_id`) WHERE deleted_at IS NULL

<a id="lmscohort_enrollment"></a>
### lms.cohort_enrollment

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `cohort_id` | uuid | Y |  | FK → `lms.cohort` · RESTRICT · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `payment_id` | uuid |  |  | logical → `finance.payment` (cross-module, no FK) |  |
| 6 | `acquisition_source_id` | uuid |  |  | logical → `finance.acquisition_source` (cross-module, no FK) |  |
| 7 | `confirmation_email_sent` | bool | Y | FALSE |  |  |
| 8 | `is_active` | bool | Y | TRUE |  |  |
| 9 | `is_self_enrolled` | bool | Y | FALSE |  |  |
| 10 | `final_grade` | varchar(?) |  |  |  |  |
| 11 | `final_gpa` | numeric(?,?) |  |  |  |  |
| 12 | `graded_by` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 13 | `graded_at` | timestamptz |  |  |  |  |
| 14 | `grade_published` | bool | Y | FALSE |  |  |
| 15 | `grade_published_at` | timestamptz |  |  |  |  |
| 16 | `teacher_comment` | text |  |  |  |  |
| 17 | `has_accommodation` | bool | Y | FALSE |  |  |
| 18 | `accommodation_ref` | varchar(?) |  |  |  |  |
| 19 | `survey_status` | int2 |  |  |  |  |
| 20 | `removed_at` | timestamptz |  |  |  |  |
| 21 | `removal_reason` | varchar(?) |  |  |  |  |
| 22 | `created_at` | timestamptz | Y | now() |  |  |
| 23 | `created_by` | uuid |  |  |  |  |
| 24 | `updated_at` | timestamptz | Y | now() |  |  |
| 25 | `updated_by` | uuid |  |  |  |  |
| 26 | `deleted_at` | timestamptz |  |  |  |  |
| 27 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `ix_cohort_enrollment_1` (`cohort_id`, `user_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_cohort_enrollment_removal_reason`: removal_reason IN ('manual', 'account_deactivated')

<a id="lmscohort_feedback"></a>
### lms.cohort_feedback

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `cohort_id` | uuid | Y |  | FK → `lms.cohort` · CASCADE · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `feedback` | text | Y |  |  |  |
| 6 | `content_rating` | numeric(?,?) |  |  |  |  |
| 7 | `teacher_rating` | numeric(?,?) |  |  |  |  |
| 8 | `value_rating` | numeric(?,?) |  |  |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_cohort_feedback_1` (`cohort_id`, `user_id`) WHERE deleted_at IS NULL

<a id="lmscohort_group"></a>
### lms.cohort_group

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `cohort_id` | uuid | Y |  | FK → `lms.cohort` · CASCADE · tenant-composite |  |
| 4 | `name` | varchar(?) | Y |  |  |  |
| 5 | `description` | text |  |  |  |  |
| 6 | `is_disabled` | bool | Y | FALSE |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_cohort_group_1` (`cohort_id`, `name`) WHERE deleted_at IS NULL

**Relationships**

- ← `lms.forum_section.cohort_group_id` — referenced by (1:N)

<a id="lmscohort_holiday"></a>
### lms.cohort_holiday

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `cohort_id` | uuid | Y |  | FK → `lms.cohort` · CASCADE · tenant-composite |  |
| 4 | `holiday_id` | uuid |  |  | logical → `org.holiday` (cross-module, no FK) |  |
| 5 | `title` | varchar(?) | Y |  |  |  |
| 6 | `start_date` | date | Y |  |  |  |
| 7 | `end_date` | date | Y |  |  |  |
| 8 | `is_included` | bool | Y | TRUE |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_cohort_holiday_1`: end_date >= start_date

<a id="lmscohort_schedule_slot"></a>
### lms.cohort_schedule_slot

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `cohort_id` | uuid | Y |  | FK → `lms.cohort` · CASCADE · tenant-composite |  |
| 4 | `slot_type` | varchar(?) | Y | 'class_meeting' |  |  |
| 5 | `cohort_teacher_id` | uuid |  |  | FK → `lms.cohort_teacher` · CASCADE · tenant-composite |  |
| 6 | `weekday` | varchar(?) | Y |  |  |  |
| 7 | `start_time` | time | Y |  |  |  |
| 8 | `end_time` | time | Y |  |  |  |
| 9 | `venue` | varchar(?) |  |  |  |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_cohort_schedule_slot_slot_type`: slot_type IN ('class_meeting', 'office_hours')
- `ck_cohort_schedule_slot_weekday`: weekday IN ('monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday')
- `ck_cohort_schedule_slot_1`: end_time > start_time
- `ck_cohort_schedule_slot_2`: (slot_type = 'office_hours') = (cohort_teacher_id IS NOT NULL)

<a id="lmscohort_session"></a>
### lms.cohort_session

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `cohort_id` | uuid | Y |  | FK → `lms.cohort` · CASCADE · tenant-composite |  |
| 4 | `starts_at` | timestamptz | Y |  |  |  |
| 5 | `ends_at` | timestamptz | Y |  |  |  |
| 6 | `description` | text |  |  |  |  |
| 7 | `venue` | varchar(?) |  |  |  |  |
| 8 | `is_cancelled` | bool | Y | FALSE |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_cohort_session_1`: ends_at > starts_at

<a id="lmscohort_teacher"></a>
### lms.cohort_teacher

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `cohort_id` | uuid | Y |  | FK → `lms.cohort` · CASCADE · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `role_id` | uuid |  |  | logical → `iam.role` (cross-module, no FK) |  |
| 6 | `role_label` | varchar(?) |  |  |  |  |
| 7 | `is_active` | bool | Y | TRUE |  |  |
| 8 | `has_office_hours` | bool | Y | FALSE |  |  |
| 9 | `removed_at` | timestamptz |  |  |  |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `ix_cohort_teacher_1` (`cohort_id`, `user_id`) WHERE deleted_at IS NULL

**Relationships**

- ← `lms.cohort_schedule_slot.cohort_teacher_id` — referenced by (1:N)

<a id="lmsconferencing_account"></a>
### lms.conferencing_account

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `provider` | varchar(?) | Y |  |  |  |
| 4 | `account_name` | varchar(?) | Y |  |  |  |
| 5 | `owner_user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 6 | `is_enabled` | bool | Y | TRUE |  |  |
| 7 | `external_account_id` | varchar(?) |  |  |  |  |
| 8 | `client_id` | varchar(?) |  |  |  |  |
| 9 | `client_secret_enc` | text |  |  |  |  |
| 10 | `external_calendar_id` | varchar(?) |  |  |  |  |
| 11 | `created_at` | timestamptz | Y | now() |  |  |
| 12 | `created_by` | uuid |  |  |  |  |
| 13 | `updated_at` | timestamptz | Y | now() |  |  |
| 14 | `updated_by` | uuid |  |  |  |  |
| 15 | `deleted_at` | timestamptz |  |  |  |  |
| 16 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_conferencing_account_1` (`tenant_id`, `account_name`) WHERE deleted_at IS NULL

**Checks**

- `ck_conferencing_account_provider`: provider IN ('zoom', 'google_meet')

**Relationships**

- ← `lms.cohort.conferencing_account_id` — referenced by (1:N)
- ← `lms.conferencing_token.conferencing_account_id` — referenced by (1:N)
- ← `lms.live_class.conferencing_account_id` — referenced by (1:N)

<a id="lmsconferencing_token"></a>
### lms.conferencing_token

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `conferencing_account_id` | uuid | Y |  | FK → `lms.conferencing_account` · CASCADE · tenant-composite |  |
| 4 | `token_type` | varchar(?) | Y |  |  |  |
| 5 | `token_enc` | text | Y |  |  |  |
| 6 | `expires_at` | timestamptz |  |  |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`conferencing_account_id`, `token_type`)

**Checks**

- `ck_conferencing_token_type`: token_type IN ('access', 'refresh')

<a id="lmscontent_author"></a>
### lms.content_author

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 4 | `quiz_id` | uuid |  |  | FK → `lms.quiz` · CASCADE · tenant-composite |  |
| 5 | `question_id` | uuid |  |  | FK → `lms.question` · CASCADE · tenant-composite |  |
| 6 | `assignment_id` | uuid |  |  | FK → `lms.assignment` · CASCADE · tenant-composite |  |
| 7 | `exercise_id` | uuid |  |  | FK → `lms.programming_exercise` · CASCADE · tenant-composite |  |
| 8 | `learning_program_id` | uuid |  |  | FK → `lms.learning_program` · CASCADE · tenant-composite |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_content_author_1`: num_nonnulls(quiz_id, question_id, assignment_id, exercise_id, learning_program_id) = 1

<a id="lmscontent_item"></a>
### lms.content_item

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `chapter_id` | uuid |  |  | FK → `lms.chapter` · CASCADE |  |
| 4 | `course_id` | uuid |  |  | FK → `lms.course` · CASCADE · tenant-composite |  |
| 5 | `cohort_id` | uuid |  |  | FK → `lms.cohort` · CASCADE · tenant-composite |  |
| 6 | `item_kind` | varchar(?) | Y |  |  |  |
| 7 | `lesson_id` | uuid |  |  | FK → `lms.lesson` · CASCADE · tenant-composite |  |
| 8 | `file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 9 | `assignment_id` | uuid |  |  | FK → `lms.assignment` · RESTRICT · tenant-composite |  |
| 10 | `quiz_id` | uuid |  |  | FK → `lms.quiz` · RESTRICT · tenant-composite |  |
| 11 | `discussion_prompt_id` | uuid |  |  | FK → `lms.discussion_prompt` · RESTRICT · tenant-composite |  |
| 12 | `participation_activity_id` | uuid |  |  | FK → `lms.participation_activity` · RESTRICT · tenant-composite |  |
| 13 | `web_resource_id` | uuid |  |  | FK → `lms.web_resource` · RESTRICT · tenant-composite |  |
| 14 | `ebook_id` | uuid |  |  | FK → `lms.ebook` · RESTRICT · tenant-composite |  |
| 15 | `external_tool_resource_id` | uuid |  |  | FK → `lms.external_tool_resource` · RESTRICT · tenant-composite |  |
| 16 | `live_class_id` | uuid |  |  | FK → `lms.live_class` · CASCADE · tenant-composite |  |
| 17 | `link_url` | varchar(?) |  |  |  |  |
| 18 | `title` | varchar(?) |  |  |  |  |
| 19 | `sort_order` | int4 | Y | 0 |  |  |
| 20 | `is_active` | bool | Y | TRUE |  |  |
| 21 | `is_always_visible` | bool | Y | TRUE |  |  |
| 22 | `visible_from` | timestamptz |  |  |  |  |
| 23 | `visible_until` | timestamptz |  |  |  |  |
| 24 | `visible_until_cohort_end` | bool | Y | FALSE |  |  |
| 25 | `attempt_from` | timestamptz |  |  |  |  |
| 26 | `due_at` | timestamptz |  |  |  |  |
| 27 | `attempt_until_cohort_end` | bool | Y | FALSE |  |  |
| 28 | `visible_start_day` | int4 |  |  |  |  |
| 29 | `visible_start_time` | time |  |  |  |  |
| 30 | `visible_end_day` | int4 |  |  |  |  |
| 31 | `visible_end_time` | time |  |  |  |  |
| 32 | `attempt_start_day` | int4 |  |  |  |  |
| 33 | `attempt_start_time` | time |  |  |  |  |
| 34 | `attempt_end_day` | int4 |  |  |  |  |
| 35 | `attempt_end_time` | time |  |  |  |  |
| 36 | `late_submission_allowed` | bool | Y | FALSE |  |  |
| 37 | `late_submission_days` | int4 |  |  |  |  |
| 38 | `is_graded` | bool | Y | FALSE |  |  |
| 39 | `points` | numeric(?,?) |  |  |  |  |
| 40 | `weight_percent` | numeric(?,?) |  |  |  |  |
| 41 | `grading_category_id` | uuid |  |  | FK → `lms.grading_category` · SET NULL · tenant-composite |  |
| 42 | `rubric_id` | uuid |  |  | FK → `lms.rubric` · SET NULL · tenant-composite |  |
| 43 | `plagiarism_check_required` | bool | Y | FALSE |  |  |
| 44 | `show_in_calendar` | bool | Y | TRUE |  |  |
| 45 | `allow_delete` | bool | Y | TRUE |  |  |
| 46 | `allow_edit_details` | bool | Y | TRUE |  |  |
| 47 | `allow_edit_dates` | bool | Y | TRUE |  |  |
| 48 | `allow_edit_grading` | bool | Y | TRUE |  |  |
| 49 | `copied_from_id` | uuid |  |  | FK → `lms.content_item` · SET NULL · tenant-composite |  |
| 50 | `is_original` | bool | Y | TRUE |  |  |
| 51 | `view_count` | int8 | Y | 0 |  |  |
| 52 | `like_count` | int8 | Y | 0 |  |  |
| 53 | `rating_total` | numeric(?,?) | Y | 0 |  |  |
| 54 | `rating_count` | int4 | Y | 0 |  |  |
| 55 | `created_at` | timestamptz | Y | now() |  |  |
| 56 | `created_by` | uuid |  |  |  |  |
| 57 | `updated_at` | timestamptz | Y | now() |  |  |
| 58 | `updated_by` | uuid |  |  |  |  |
| 59 | `deleted_at` | timestamptz |  |  |  |  |
| 60 | `row_version` | int4 | Y | 0 |  |  |
| 61 | `exercise_id` | uuid |  |  | FK → `lms.programming_exercise` · CASCADE · tenant-composite |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_content_item_item_kind`: item_kind IN ('lesson', 'file', 'assignment', 'quiz', 'discussion', 'participation', 'web_resource', 'ebook', 'external_tool', 'live_class', 'link', 'exercise')
- `ck_content_item_1`: num_nonnulls(lesson_id, file_id, assignment_id, quiz_id, discussion_prompt_id, participation_activity_id, web_resource_id, ebook_id, external_tool_resource_id, live_class_id, link_url, exercise_id) = 1
- `ck_content_item_2`: (item_kind = 'lesson') = (lesson_id IS NOT NULL) AND (item_kind = 'file') = (file_id IS NOT NULL) AND (item_kind = 'assignment') = (assignment_id IS NOT NULL) AND (item_kind = 'quiz') = (quiz_id IS NOT NULL) AND (item_kind = 'discussion') = (discussion_prompt_id IS NOT NULL) AND (item_kind = 'participation') = (participation_activity_id IS NOT NULL) AND (item_kind = 'web_resource') = (web_resource_id IS NOT NULL) AND (item_kind = 'ebook') = (ebook_id IS NOT NULL) AND (item_kind = 'external_tool') = (external_tool_resource_id IS NOT NULL) AND (item_kind = 'live_class') = (live_class_id IS NOT NULL) AND (item_kind = 'link') = (link_url IS NOT NULL) AND (item_kind = 'exercise') = (exercise_id IS NOT NULL)
- `ck_content_item_placement`: (chapter_id IS NOT NULL AND course_id IS NOT NULL) OR cohort_id IS NOT NULL

**Relationships**

- ← `lms.annotation.content_item_id` — referenced by (1:N)
- ← `lms.assignment_group.content_item_id` — referenced by (1:N)
- ← `lms.assignment_group_member.content_item_id` — referenced by (1:N)
- ← `lms.assignment_submission.content_item_id` — referenced by (1:N)
- ← `lms.content_item.copied_from_id` — referenced by (1:N)
- ← `lms.content_item_rating.content_item_id` — referenced by (1:N)
- ← `lms.content_view_session.content_item_id` — referenced by (1:N)
- ← `lms.forum_topic.content_item_id` — referenced by (1:N)
- ← `lms.gradebook_entry.content_item_id` — referenced by (1:N)
- ← `lms.item_override.content_item_id` — referenced by (1:N)
- ← `lms.quiz_submission.content_item_id` — referenced by (1:N)
- ← `lms.reassignment_log.content_item_id` — referenced by (1:N)
- ← `lms.release_grant.content_item_id` — referenced by (1:N)
- ← `lms.release_rule.source_content_item_id` — referenced by (1:N)
- ← `lms.release_rule_target.content_item_id` — referenced by (1:N)
- ← `lms.rubric_assessment.content_item_id` — referenced by (1:N)
- ← `lms.student_alert.content_item_id` — referenced by (1:N)

<a id="lmscontent_item_rating"></a>
### lms.content_item_rating

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `content_item_id` | uuid | Y |  | FK → `lms.content_item` · CASCADE · tenant-composite |  |
| 4 | `cohort_id` | uuid |  |  | FK → `lms.cohort` · CASCADE · tenant-composite |  |
| 5 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 6 | `is_like` | bool |  |  |  |  |
| 7 | `rating` | int4 |  |  |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_content_item_rating_1` (`content_item_id`, `user_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_content_item_rating_rating_rule`: rating BETWEEN 1 AND 5

<a id="lmscontent_view_session"></a>
### lms.content_view_session

*Partitioned by RANGE (started_at); monthly partitions plus a default partition.*

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 4 | `content_item_id` | uuid |  |  | FK → `lms.content_item` · CASCADE · tenant-composite |  |
| 5 | `file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 6 | `cohort_id` | uuid |  |  | FK → `lms.cohort` · CASCADE · tenant-composite |  |
| 7 | `started_at` | timestamptz | Y |  | PK |  |
| 8 | `ended_at` | timestamptz |  |  |  |  |
| 9 | `location` | text |  |  |  |  |
| 10 | `session_ref` | varchar(?) |  |  |  |  |
| 11 | `created_at` | timestamptz | Y | now() |  |  |
| 12 | `created_by` | uuid |  |  |  |  |
| 13 | `updated_at` | timestamptz | Y | now() |  |  |
| 14 | `updated_by` | uuid |  |  |  |  |
| 15 | `deleted_at` | timestamptz |  |  |  |  |
| 16 | `row_version` | int4 | Y | 0 |  |  |

<a id="lmscourse"></a>
### lms.course

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `title` | varchar(?) | Y |  |  |  |
| 5 | `short_introduction` | text |  |  |  |  |
| 6 | `description` | text |  |  |  |  |
| 7 | `image_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 8 | `video_url` | varchar(?) |  |  |  |  |
| 9 | `category_id` | uuid |  |  | FK → `lms.category` · SET NULL · tenant-composite |  |
| 10 | `status` | varchar(?) | Y | 'in_progress' |  |  |
| 11 | `is_published` | bool | Y | FALSE |  |  |
| 12 | `published_on` | date |  |  |  |  |
| 13 | `is_upcoming` | bool | Y | FALSE |  |  |
| 14 | `is_featured` | bool | Y | FALSE |  |  |
| 15 | `disable_self_learning` | bool | Y | FALSE |  |  |
| 16 | `enforce_lesson_order` | bool | Y | FALSE |  |  |
| 17 | `certification_enabled` | bool | Y | FALSE |  |  |
| 18 | `paid_certificate` | bool | Y | FALSE |  |  |
| 19 | `default_evaluator_id` | uuid |  |  | FK → `lms.evaluator` · SET NULL · tenant-composite |  |
| 20 | `is_paid` | bool | Y | FALSE |  |  |
| 21 | `price` | numeric(?,?) |  |  |  |  |
| 22 | `currency_id` | uuid |  |  | logical → `master.currency` (cross-module, no FK) |  |
| 23 | `price_usd` | numeric(?,?) |  |  |  |  |
| 24 | `card_gradient` | varchar(?) |  |  |  |  |
| 25 | `timezone` | varchar(?) |  |  |  |  |
| 26 | `publish_notification_sent` | bool | Y | FALSE |  |  |
| 27 | `enrollment_count` | int4 | Y | 0 |  |  |
| 28 | `lesson_count` | int4 | Y | 0 |  |  |
| 29 | `average_rating` | numeric(?,?) |  |  |  |  |
| 30 | `code` | varchar(?) |  |  |  |  |
| 31 | `state_standard_code` | varchar(?) |  |  |  |  |
| 32 | `credits` | numeric(?,?) |  |  |  |  |
| 33 | `prerequisites` | text |  |  |  |  |
| 34 | `corequisites` | text |  |  |  |  |
| 35 | `effective_from` | date |  |  |  |  |
| 36 | `lifecycle_status` | varchar(?) | Y | 'draft' |  |  |
| 37 | `deactivated_on` | date |  |  |  |  |
| 38 | `version_major` | int4 | Y | 1 |  |  |
| 39 | `version_minor` | int4 | Y | 0 |  |  |
| 40 | `previous_version_id` | uuid |  |  | FK → `lms.course` · SET NULL · tenant-composite |  |
| 41 | `root_course_id` | uuid |  |  | FK → `lms.course` · SET NULL · tenant-composite |  |
| 42 | `author_comments` | text |  |  |  |  |
| 43 | `external_ref` | varchar(?) |  |  |  |  |
| 44 | `nominal_duration` | int4 |  |  |  |  |
| 45 | `delivery_modality` | varchar(?) |  |  |  |  |
| 46 | `is_shared` | bool | Y | FALSE |  |  |
| 47 | `content_updated_at` | timestamptz |  |  |  |  |
| 48 | `grade_display` | varchar(?) | Y | 'points' |  |  |
| 49 | `grading_scheme_id` | uuid |  |  | logical → `assessment.grading_scheme` (cross-module, no FK) |  |
| 50 | `is_in_catalog` | bool | Y | FALSE |  |  |
| 51 | `catalog_description` | text |  |  |  |  |
| 52 | `catalog_visible_from` | timestamptz |  |  |  |  |
| 53 | `catalog_visible_until` | timestamptz |  |  |  |  |
| 54 | `outline` | text |  |  |  |  |
| 55 | `workload_hours` | int4 |  |  |  |  |
| 56 | `catalog_status` | varchar(?) | Y | 'draft' |  |  |
| 57 | `created_at` | timestamptz | Y | now() |  |  |
| 58 | `created_by` | uuid |  |  |  |  |
| 59 | `updated_at` | timestamptz | Y | now() |  |  |
| 60 | `updated_by` | uuid |  |  |  |  |
| 61 | `deleted_at` | timestamptz |  |  |  |  |
| 62 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `ix_course_1` (`tenant_id`, `code`, `version_major`, `version_minor`) WHERE code IS NOT NULL AND deleted_at IS NULL

**Checks**

- `ck_course_status`: status IN ('in_progress', 'under_review', 'approved')
- `ck_course_card_gradient`: card_gradient IN ('red', 'blue', 'green', 'amber', 'cyan', 'orange', 'pink', 'purple', 'teal', 'violet', 'yellow', 'gray')
- `ck_course_lifecycle_status`: lifecycle_status IN ('draft', 'published', 'deactivated')
- `ck_course_delivery_modality`: delivery_modality IN ('traditional', 'online', 'hybrid')
- `ck_course_grade_display`: grade_display IN ('points', 'percentage')
- `ck_course_catalog_status`: catalog_status IN ('draft', 'published', 'archived')
- `ck_course_1`: NOT is_paid OR (price IS NOT NULL AND currency_id IS NOT NULL)

**Relationships**

- ← `lms.assignment.course_id` — referenced by (1:N)
- ← `lms.assignment_submission.course_id` — referenced by (1:N)
- ← `lms.catalog_banner.course_id` — referenced by (1:N)
- ← `lms.certificate.course_id` — referenced by (1:N)
- ← `lms.certificate_evaluation.course_id` — referenced by (1:N)
- ← `lms.certificate_request.course_id` — referenced by (1:N)
- ← `lms.chapter.course_id` — referenced by (1:N)
- ← `lms.cohort_course.course_id` — referenced by (1:N)
- ← `lms.content_item.course_id` — referenced by (1:N)
- ← `lms.course.previous_version_id` — referenced by (1:N)
- ← `lms.course.root_course_id` — referenced by (1:N)
- ← `lms.course_author_profile.course_id` — referenced by (1:N)
- ← `lms.course_interest.course_id` — referenced by (1:N)
- ← `lms.course_mentor.course_id` — referenced by (1:N)
- ← `lms.course_preview.course_id` — referenced by (1:N)
- ← `lms.course_review.course_id` — referenced by (1:N)
- ← `lms.course_teacher.course_id` — referenced by (1:N)
- ← `lms.course_testimonial.course_id` — referenced by (1:N)
- ← `lms.drive_item.course_id` — referenced by (1:N)
- ← `lms.enrollment.course_id` — referenced by (1:N)
- ← `lms.enrollment_request.course_id` — referenced by (1:N)
- ← `lms.grading_category.course_id` — referenced by (1:N)
- ← `lms.import_export_job.course_id` — referenced by (1:N)
- ← `lms.learning_objective.course_id` — referenced by (1:N)
- ← `lms.learning_program_course.course_id` — referenced by (1:N)
- ← `lms.lesson_progress.course_id` — referenced by (1:N)
- ← `lms.quiz.course_id` — referenced by (1:N)
- ← `lms.quiz_submission.course_id` — referenced by (1:N)
- ← `lms.related_course.course_id` — referenced by (1:N)
- ← `lms.related_course.related_course_id` — referenced by (1:N)
- ← `lms.related_material_rank.course_id` — referenced by (1:N)
- ← `lms.release_rule.course_id` — referenced by (1:N)
- ← `lms.student_alert.course_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `finance.coupon_scope.lms_course_id`
- ← `finance.payment.lms_course_id`

<a id="lmscourse_author_profile"></a>
### lms.course_author_profile

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `course_id` | uuid | Y |  | FK → `lms.course` · CASCADE · tenant-composite |  |
| 4 | `name` | varchar(?) | Y |  |  |  |
| 5 | `bio` | text |  |  |  |  |
| 6 | `photo_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 7 | `qualification` | varchar(?) |  |  |  |  |
| 8 | `sort_order` | int4 | Y | 0 |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Relationships**

- ← `lms.related_course.author_profile_id` — referenced by (1:N)

<a id="lmscourse_interest"></a>
### lms.course_interest

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `course_id` | uuid | Y |  | FK → `lms.course` · CASCADE · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `email_sent` | bool | Y | FALSE |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_course_interest_1` (`course_id`, `user_id`) WHERE deleted_at IS NULL

<a id="lmscourse_mentor"></a>
### lms.course_mentor

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `course_id` | uuid | Y |  | FK → `lms.course` · CASCADE · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `ix_course_mentor_1` (`course_id`, `user_id`) WHERE deleted_at IS NULL

<a id="lmscourse_preview"></a>
### lms.course_preview

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `course_id` | uuid | Y |  | FK → `lms.course` · CASCADE · tenant-composite |  |
| 4 | `preview_kind` | varchar(?) | Y |  |  |  |
| 5 | `file_id` | uuid | Y |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 6 | `icon` | varchar(?) |  |  |  |  |
| 7 | `title` | varchar(?) |  |  |  |  |
| 8 | `description` | text |  |  |  |  |
| 9 | `sort_order` | int4 | Y | 0 |  |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_course_preview_preview_kind`: preview_kind IN ('material', 'video')

<a id="lmscourse_review"></a>
### lms.course_review

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `course_id` | uuid | Y |  | FK → `lms.course` · CASCADE · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `rating` | numeric(?,?) | Y |  |  |  |
| 6 | `review` | text |  |  |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_course_review_1` (`course_id`, `user_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_course_review_rating_rule`: rating BETWEEN 0 AND 5

<a id="lmscourse_teacher"></a>
### lms.course_teacher

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `course_id` | uuid | Y |  | FK → `lms.course` · CASCADE · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `sort_order` | int4 | Y | 0 |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `ix_course_teacher_1` (`course_id`, `user_id`) WHERE deleted_at IS NULL

<a id="lmscourse_testimonial"></a>
### lms.course_testimonial

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `course_id` | uuid | Y |  | FK → `lms.course` · CASCADE · tenant-composite |  |
| 4 | `title` | varchar(?) |  |  |  |  |
| 5 | `quote` | text |  |  |  |  |
| 6 | `photo_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 7 | `current_position` | varchar(?) |  |  |  |  |
| 8 | `sort_order` | int4 | Y | 0 |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

<a id="lmsdiscussion_prompt"></a>
### lms.discussion_prompt

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `title` | varchar(?) | Y |  |  |  |
| 5 | `description` | text |  |  |  |  |
| 6 | `is_in_repository` | bool | Y | FALSE |  |  |
| 7 | `uses_inline_authoring` | bool | Y | FALSE |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Relationships**

- ← `lms.content_item.discussion_prompt_id` — referenced by (1:N)
- ← `lms.forum_topic.discussion_prompt_id` — referenced by (1:N)

<a id="lmsdrive_item"></a>
### lms.drive_item

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `owner_user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 4 | `file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 5 | `item_type` | varchar(?) |  |  |  |  |
| 6 | `icon` | varchar(?) |  |  |  |  |
| 7 | `title` | varchar(?) | Y |  |  |  |
| 8 | `description` | text |  |  |  |  |
| 9 | `keywords` | varchar(?) |  |  |  |  |
| 10 | `size_bytes` | int8 |  |  |  |  |
| 11 | `link_url` | varchar(?) |  |  |  |  |
| 12 | `uploaded_at` | timestamptz | Y | now() |  |  |
| 13 | `course_id` | uuid |  |  | FK → `lms.course` · SET NULL · tenant-composite |  |
| 14 | `is_shared` | bool | Y | FALSE |  |  |
| 15 | `like_count` | int4 | Y | 0 |  |  |
| 16 | `dislike_count` | int4 | Y | 0 |  |  |
| 17 | `created_at` | timestamptz | Y | now() |  |  |
| 18 | `created_by` | uuid |  |  |  |  |
| 19 | `updated_at` | timestamptz | Y | now() |  |  |
| 20 | `updated_by` | uuid |  |  |  |  |
| 21 | `deleted_at` | timestamptz |  |  |  |  |
| 22 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_drive_item_1`: file_id IS NOT NULL OR link_url IS NOT NULL

**Relationships**

- ← `lms.drive_item_share.drive_item_id` — referenced by (1:N)

<a id="lmsdrive_item_share"></a>
### lms.drive_item_share

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `drive_item_id` | uuid | Y |  | FK → `lms.drive_item` · CASCADE · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `reaction` | varchar(?) |  |  |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_drive_item_share_1` (`drive_item_id`, `user_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_drive_item_share_reaction`: reaction IN ('like', 'dislike')

<a id="lmsebook"></a>
### lms.ebook

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `title` | varchar(?) | Y |  |  |  |
| 5 | `description` | text |  |  |  |  |
| 6 | `ebook_type` | varchar(?) | Y | 'document' |  |  |
| 7 | `file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 8 | `source_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 9 | `conversion_job_ref` | varchar(?) |  |  |  |  |
| 10 | `conversion_output_url` | varchar(?) |  |  |  | Converted file's URL a finished job named, until it is stored. |
| 11 | `conversion_error` | varchar(?) |  |  |  | Why the last conversion job failed. |
| 12 | `show_related_materials` | bool | Y | FALSE |  |  |
| 13 | `related_material_seed` | text |  |  |  |  |
| 14 | `category_id` | uuid |  |  | FK → `lms.resource_category` · SET NULL · tenant-composite |  |
| 15 | `is_in_repository` | bool | Y | FALSE |  |  |
| 16 | `created_at` | timestamptz | Y | now() |  |  |
| 17 | `created_by` | uuid |  |  |  |  |
| 18 | `updated_at` | timestamptz | Y | now() |  |  |
| 19 | `updated_by` | uuid |  |  |  |  |
| 20 | `deleted_at` | timestamptz |  |  |  |  |
| 21 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_ebook_ebook_type`: ebook_type IN ('document', 'presentation', 'pdf', 'epub', 'other')

**Relationships**

- ← `lms.annotation.ebook_id` — referenced by (1:N)
- ← `lms.content_item.ebook_id` — referenced by (1:N)
- ← `lms.related_material.ebook_id` — referenced by (1:N)
- ← `lms.related_material_rank.ebook_id` — referenced by (1:N)

<a id="lmsenrollment"></a>
### lms.enrollment

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `course_id` | uuid | Y |  | FK → `lms.course` · RESTRICT · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `member_type` | varchar(?) | Y | 'student' |  |  |
| 6 | `member_role` | varchar(?) | Y | 'member' |  |  |
| 7 | `progress_percent` | numeric(?,?) | Y | 0 |  |  |
| 8 | `current_lesson_id` | uuid |  |  | FK → `lms.lesson` · SET NULL · tenant-composite |  |
| 9 | `source_cohort_id` | uuid |  |  | FK → `lms.cohort` · SET NULL · tenant-composite |  |
| 10 | `payment_id` | uuid |  |  | logical → `finance.payment` (cross-module, no FK) |  |
| 11 | `purchased_certificate` | bool | Y | FALSE |  |  |
| 12 | `certificate_id` | uuid |  |  | FK → `lms.certificate` · SET NULL · tenant-composite |  |
| 13 | `created_at` | timestamptz | Y | now() |  |  |
| 14 | `created_by` | uuid |  |  |  |  |
| 15 | `updated_at` | timestamptz | Y | now() |  |  |
| 16 | `updated_by` | uuid |  |  |  |  |
| 17 | `deleted_at` | timestamptz |  |  |  |  |
| 18 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `ix_enrollment_1` (`course_id`, `user_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_enrollment_member_type`: member_type IN ('student', 'mentor', 'staff')
- `ck_enrollment_member_role`: member_role IN ('member', 'admin')

<a id="lmsenrollment_request"></a>
### lms.enrollment_request

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `course_id` | uuid | Y |  | FK → `lms.course` · CASCADE · tenant-composite |  |
| 4 | `cohort_id` | uuid |  |  | FK → `lms.cohort` · SET NULL · tenant-composite |  |
| 5 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 6 | `requested_at` | timestamptz | Y | now() |  |  |
| 7 | `status` | varchar(?) | Y | 'pending' |  |  |
| 8 | `decided_by` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 9 | `decided_at` | timestamptz |  |  |  |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `ix_enrollment_request_1` (`course_id`, `user_id`) WHERE status = 'pending' AND deleted_at IS NULL

**Checks**

- `ck_enrollment_request_status`: status IN ('pending', 'approved', 'rejected', 'cancelled')

<a id="lmsevaluator"></a>
### lms.evaluator

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 4 | `unavailable_from` | date |  |  |  |  |
| 5 | `unavailable_to` | date |  |  |  |  |
| 6 | `calendar_provider` | varchar(30) |  |  |  | Calendar provider the evaluator linked (google); NULL = none |
| 7 | `calendar_state` | varchar(64) |  |  |  | OAuth state nonce of the newest link attempt |
| 8 | `calendar_requested_at` | timestamptz |  |  |  | When the newest link attempt started |
| 9 | `calendar_connected_at` | timestamptz |  |  |  | When the calendar was linked; NULL = not (yet) linked |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_evaluator_1` (`tenant_id`, `user_id`) WHERE deleted_at IS NULL

**Relationships**

- ← `lms.certificate.evaluator_id` — referenced by (1:N)
- ← `lms.cohort_course.evaluator_id` — referenced by (1:N)
- ← `lms.course.default_evaluator_id` — referenced by (1:N)
- ← `lms.evaluator_availability.evaluator_id` — referenced by (1:N)

<a id="lmsevaluator_availability"></a>
### lms.evaluator_availability

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `evaluator_id` | uuid | Y |  | FK → `lms.evaluator` · CASCADE · tenant-composite |  |
| 4 | `weekday` | varchar(?) | Y |  |  |  |
| 5 | `start_time` | time | Y |  |  |  |
| 6 | `end_time` | time | Y |  |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_evaluator_availability_weekday`: weekday IN ('monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday')
- `ck_evaluator_availability_1`: end_time > start_time

<a id="lmsexercise_submission"></a>
### lms.exercise_submission

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `exercise_id` | uuid | Y |  | FK → `lms.programming_exercise` · RESTRICT · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `code` | text | Y |  |  |  |
| 6 | `is_full_code` | bool | Y | FALSE |  |  |
| 7 | `status` | varchar(?) |  |  |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_exercise_submission_status`: status IN ('passed', 'failed')

**Relationships**

- ← `lms.exercise_test_result.exercise_submission_id` — referenced by (1:N)

<a id="lmsexercise_test_case"></a>
### lms.exercise_test_case

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `exercise_id` | uuid | Y |  | FK → `lms.programming_exercise` · CASCADE · tenant-composite |  |
| 4 | `sort_order` | int4 | Y | 0 |  |  |
| 5 | `input` | text |  |  |  |  |
| 6 | `expected_output` | text | Y |  |  |  |
| 7 | `is_hidden` | bool | Y | TRUE |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Relationships**

- ← `lms.exercise_test_result.test_case_id` — referenced by (1:N)

<a id="lmsexercise_test_result"></a>
### lms.exercise_test_result

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `exercise_submission_id` | uuid | Y |  | FK → `lms.exercise_submission` · CASCADE · tenant-composite |  |
| 4 | `test_case_id` | uuid |  |  | FK → `lms.exercise_test_case` · SET NULL · tenant-composite |  |
| 5 | `input` | text |  |  |  |  |
| 6 | `expected_output` | text |  |  |  |  |
| 7 | `actual_output` | text |  |  |  |  |
| 8 | `status` | varchar(?) | Y |  |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_exercise_test_result_status`: status IN ('passed', 'failed')

<a id="lmsexternal_tool"></a>
### lms.external_tool

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `title` | varchar(?) | Y |  |  |  |
| 4 | `description` | text |  |  |  |  |
| 5 | `is_included` | bool | Y | FALSE |  |  |
| 6 | `is_paid` | bool | Y | FALSE |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Relationships**

- ← `lms.external_tool_institution.external_tool_id` — referenced by (1:N)
- ← `lms.external_tool_link.external_tool_id` — referenced by (1:N)
- ← `lms.external_tool_option.external_tool_id` — referenced by (1:N)
- ← `lms.external_tool_role.external_tool_id` — referenced by (1:N)
- ← `lms.external_tool_setting.external_tool_id` — referenced by (1:N)

<a id="lmsexternal_tool_institution"></a>
### lms.external_tool_institution

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `external_tool_id` | uuid | Y |  | FK → `lms.external_tool` · CASCADE · tenant-composite |  |
| 4 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_external_tool_institution_1` (`external_tool_id`, `institution_id`) WHERE deleted_at IS NULL

<a id="lmsexternal_tool_link"></a>
### lms.external_tool_link

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `external_tool_resource_id` | uuid | Y |  | FK → `lms.external_tool_resource` · CASCADE · tenant-composite |  |
| 4 | `external_tool_id` | uuid |  |  | FK → `lms.external_tool` · SET NULL · tenant-composite |  |
| 5 | `link_ref` | varchar(?) | Y |  |  |  |
| 6 | `link_title` | varchar(?) |  |  |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

<a id="lmsexternal_tool_option"></a>
### lms.external_tool_option

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `external_tool_id` | uuid | Y |  | FK → `lms.external_tool` · CASCADE · tenant-composite |  |
| 4 | `option_kind` | varchar(?) | Y |  |  |  |
| 5 | `option_value` | varchar(?) | Y |  |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_external_tool_option_1` (`external_tool_id`, `option_kind`, `option_value`) WHERE deleted_at IS NULL

**Checks**

- `ck_external_tool_option_option_kind`: option_kind IN ('tool_type', 'placement')

<a id="lmsexternal_tool_resource"></a>
### lms.external_tool_resource

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `title` | varchar(?) | Y |  |  |  |
| 5 | `description` | text |  |  |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Relationships**

- ← `lms.content_item.external_tool_resource_id` — referenced by (1:N)
- ← `lms.external_tool_link.external_tool_resource_id` — referenced by (1:N)

<a id="lmsexternal_tool_role"></a>
### lms.external_tool_role

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `external_tool_id` | uuid | Y |  | FK → `lms.external_tool` · CASCADE · tenant-composite |  |
| 4 | `role_id` | uuid | Y |  | logical → `iam.role` (cross-module, no FK) |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_external_tool_role_1` (`external_tool_id`, `role_id`) WHERE deleted_at IS NULL

<a id="lmsexternal_tool_setting"></a>
### lms.external_tool_setting

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `external_tool_id` | uuid | Y |  | FK → `lms.external_tool` · CASCADE · tenant-composite |  |
| 4 | `setting_key` | varchar(?) | Y |  |  |  |
| 5 | `setting_value` | text |  |  |  |  |
| 6 | `secret_value_enc` | text |  |  |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_external_tool_setting_1` (`external_tool_id`, `setting_key`) WHERE deleted_at IS NULL

<a id="lmsforum"></a>
### lms.forum

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `cohort_id` | uuid | Y |  | FK → `lms.cohort` · CASCADE · tenant-composite |  |
| 4 | `title` | varchar(?) | Y |  |  |  |
| 5 | `description` | text |  |  |  |  |
| 6 | `early_access_from` | timestamptz |  |  |  |  |
| 7 | `opens_at` | timestamptz |  |  |  |  |
| 8 | `closes_at` | timestamptz |  |  |  |  |
| 9 | `late_access_until` | timestamptz |  |  |  |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Relationships**

- ← `lms.forum_section.forum_id` — referenced by (1:N)

<a id="lmsforum_post"></a>
### lms.forum_post

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `forum_topic_id` | uuid | Y |  | FK → `lms.forum_topic` · CASCADE · tenant-composite |  |
| 4 | `parent_post_id` | uuid |  |  | FK → `lms.forum_post` · CASCADE · tenant-composite |  |
| 5 | `title` | varchar(?) |  |  |  |  |
| 6 | `body` | text |  |  |  |  |
| 7 | `author_user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 8 | `posted_at` | timestamptz | Y | now() |  |  |
| 9 | `is_draft` | bool | Y | FALSE |  |  |
| 10 | `is_teacher_post` | bool | Y | FALSE |  |  |
| 11 | `substantive_flag` | int2 |  |  |  |  |
| 12 | `is_hidden` | bool | Y | FALSE |  |  |
| 13 | `is_hidden_by_parent` | bool | Y | FALSE |  |  |
| 14 | `is_flagged` | bool | Y | FALSE |  |  |
| 15 | `show_signature` | bool | Y | FALSE |  |  |
| 16 | `uses_inline_authoring` | bool | Y | FALSE |  |  |
| 17 | `is_abusive` | bool | Y | FALSE |  |  |
| 18 | `abuse_reported` | bool | Y | FALSE |  |  |
| 19 | `abuse_report_count` | int4 | Y | 0 |  |  |
| 20 | `abuse_confirmed_by` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 21 | `abuse_confirmed_at` | timestamptz |  |  |  |  |
| 22 | `like_count` | int4 | Y | 0 |  |  |
| 23 | `created_at` | timestamptz | Y | now() |  |  |
| 24 | `created_by` | uuid |  |  |  |  |
| 25 | `updated_at` | timestamptz | Y | now() |  |  |
| 26 | `updated_by` | uuid |  |  |  |  |
| 27 | `deleted_at` | timestamptz |  |  |  |  |
| 28 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Relationships**

- ← `lms.forum_post.parent_post_id` — referenced by (1:N)
- ← `lms.forum_post_report.forum_post_id` — referenced by (1:N)
- ← `lms.forum_post_vote.forum_post_id` — referenced by (1:N)
- ← `lms.forum_read_state.forum_post_id` — referenced by (1:N)
- ← `lms.forum_topic.last_post_id` — referenced by (1:N)

<a id="lmsforum_post_report"></a>
### lms.forum_post_report

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `forum_post_id` | uuid | Y |  | FK → `lms.forum_post` · CASCADE · tenant-composite |  |
| 4 | `report_category_id` | uuid |  |  | FK → `lms.forum_report_category` · SET NULL · tenant-composite |  |
| 5 | `description` | text |  |  |  |  |
| 6 | `reported_by` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 7 | `reported_at` | timestamptz | Y | now() |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

<a id="lmsforum_post_vote"></a>
### lms.forum_post_vote

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `forum_post_id` | uuid | Y |  | FK → `lms.forum_post` · CASCADE · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `is_like` | bool | Y |  |  |  |
| 6 | `voted_at` | timestamptz | Y | now() |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_forum_post_vote_1` (`forum_post_id`, `user_id`) WHERE deleted_at IS NULL

<a id="lmsforum_read_state"></a>
### lms.forum_read_state

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 4 | `forum_topic_id` | uuid | Y |  | FK → `lms.forum_topic` · CASCADE · tenant-composite |  |
| 5 | `forum_post_id` | uuid |  |  | FK → `lms.forum_post` · CASCADE · tenant-composite |  |
| 6 | `read_at` | timestamptz | Y | now() |  |  |
| 7 | `is_flagged` | bool | Y | FALSE |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `ix_forum_read_state_1` (`user_id`, `forum_topic_id`, `COALESCE(forum_post_id, CAST('00000000-0000-0000-0000-000000000000' AS uuid))`) WHERE deleted_at IS NULL

<a id="lmsforum_report_category"></a>
### lms.forum_report_category

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `title` | varchar(?) | Y |  |  |  |
| 4 | `created_at` | timestamptz | Y | now() |  |  |
| 5 | `created_by` | uuid |  |  |  |  |
| 6 | `updated_at` | timestamptz | Y | now() |  |  |
| 7 | `updated_by` | uuid |  |  |  |  |
| 8 | `deleted_at` | timestamptz |  |  |  |  |
| 9 | `row_version` | int4 | Y | 0 |  |  |
| 10 | `code` | varchar(?) | Y |  |  |  |
| 11 | `sort_order` | int4 | Y | 0 |  |  |
| 12 | `is_active` | bool | Y | TRUE |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_forum_report_category_scope_code` (`tenant_id`, `code`) WHERE deleted_at IS NULL
- unique index `uq_forum_report_category_1` (`tenant_id`, `title`) WHERE deleted_at IS NULL

**Relationships**

- ← `lms.forum_post_report.report_category_id` — referenced by (1:N)

<a id="lmsforum_section"></a>
### lms.forum_section

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `forum_id` | uuid | Y |  | FK → `lms.forum` · CASCADE · tenant-composite |  |
| 4 | `title` | varchar(?) | Y |  |  |  |
| 5 | `description` | text |  |  |  |  |
| 6 | `section_type` | varchar(?) | Y | 'general' |  |  |
| 7 | `cohort_group_id` | uuid |  |  | FK → `lms.cohort_group` · SET NULL · tenant-composite |  |
| 8 | `sort_order` | int4 | Y | 0 |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_forum_section_section_type`: section_type IN ('general', 'question_answer', 'individual', 'group', 'announcement', 'assignment', 'discussion_prompt', 'mentor', 'other')

**Relationships**

- ← `lms.forum_topic.forum_section_id` — referenced by (1:N)

<a id="lmsforum_topic"></a>
### lms.forum_topic

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `forum_section_id` | uuid |  |  | FK → `lms.forum_section` · CASCADE · tenant-composite |  |
| 4 | `title` | varchar(?) | Y |  |  |  |
| 5 | `description` | text |  |  |  |  |
| 6 | `topic_type` | varchar(?) | Y | 'discussion' |  |  |
| 7 | `opens_at` | timestamptz |  |  |  |  |
| 8 | `closes_at` | timestamptz |  |  |  |  |
| 9 | `content_item_id` | uuid |  |  | FK → `lms.content_item` · SET NULL · tenant-composite |  |
| 10 | `discussion_prompt_id` | uuid |  |  | FK → `lms.discussion_prompt` · SET NULL · tenant-composite |  |
| 11 | `linked_entity_type` | varchar(?) |  |  |  |  |
| 12 | `linked_entity_id` | uuid |  |  |  |  |
| 13 | `individual_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 14 | `is_graded` | bool | Y | FALSE |  |  |
| 15 | `points_possible` | numeric(?,?) |  |  |  |  |
| 16 | `grade_weight_percent` | numeric(?,?) |  |  |  |  |
| 17 | `grading_due_at` | timestamptz |  |  |  |  |
| 18 | `is_grading_completed` | bool | Y | FALSE |  |  |
| 19 | `is_hidden` | bool | Y | FALSE |  |  |
| 20 | `is_teacher_topic` | bool | Y | FALSE |  |  |
| 21 | `is_system_generated` | bool | Y | FALSE |  |  |
| 22 | `last_post_id` | uuid |  |  | FK → `lms.forum_post` · SET NULL · tenant-composite |  |
| 23 | `created_at` | timestamptz | Y | now() |  |  |
| 24 | `created_by` | uuid |  |  |  |  |
| 25 | `updated_at` | timestamptz | Y | now() |  |  |
| 26 | `updated_by` | uuid |  |  |  |  |
| 27 | `deleted_at` | timestamptz |  |  |  |  |
| 28 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_forum_topic_topic_type`: topic_type IN ('discussion', 'question', 'announcement', 'assignment', 'discussion_prompt', 'individual', 'group', 'other')
- `ck_forum_topic_1`: (linked_entity_type IS NULL) = (linked_entity_id IS NULL)

**Relationships**

- ← `lms.annotation.forum_topic_id` — referenced by (1:N)
- ← `lms.assignment_group.discussion_topic_id` — referenced by (1:N)
- ← `lms.forum_post.forum_topic_id` — referenced by (1:N)
- ← `lms.forum_read_state.forum_topic_id` — referenced by (1:N)
- ← `lms.gradebook_entry.forum_topic_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `comms.notice.discussion_topic_id`
- ← `comms.notice_version.discussion_topic_id`

<a id="lmsgrade_change_log"></a>
### lms.grade_change_log

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `gradebook_entry_id` | uuid | Y |  | FK → `lms.gradebook_entry` · RESTRICT · tenant-composite |  |
| 4 | `points` | numeric(?,?) |  |  |  |  |
| 5 | `auto_points` | numeric(?,?) |  |  |  |  |
| 6 | `manual_points` | numeric(?,?) |  |  |  |  |
| 7 | `comment` | text |  |  |  |  |
| 8 | `was_published` | bool |  |  |  |  |
| 9 | `published_at` | timestamptz |  |  |  |  |
| 10 | `changed_by` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 11 | `changed_at` | timestamptz | Y |  |  |  |
| 12 | `student_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 13 | `created_at` | timestamptz | Y | now() |  |  |
| 14 | `created_by` | uuid |  |  |  |  |
| 15 | `updated_at` | timestamptz | Y | now() |  |  |
| 16 | `updated_by` | uuid |  |  |  |  |
| 17 | `deleted_at` | timestamptz |  |  |  |  |
| 18 | `row_version` | int4 | Y | 0 |  |  |

<a id="lmsgradebook_entry"></a>
### lms.gradebook_entry

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `cohort_id` | uuid | Y |  | FK → `lms.cohort` · RESTRICT · tenant-composite |  |
| 4 | `content_item_id` | uuid | Y |  | FK → `lms.content_item` · RESTRICT · tenant-composite |  |
| 5 | `user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 6 | `group_id` | uuid |  |  | FK → `lms.assignment_group` · RESTRICT · tenant-composite |  |
| 7 | `quiz_submission_id` | uuid |  |  | FK → `lms.quiz_submission` · SET NULL · tenant-composite |  |
| 8 | `forum_topic_id` | uuid |  |  | FK → `lms.forum_topic` · SET NULL · tenant-composite |  |
| 9 | `points` | numeric(?,?) |  |  |  |  |
| 10 | `auto_points` | numeric(?,?) |  |  |  |  |
| 11 | `manual_points` | numeric(?,?) |  |  |  |  |
| 12 | `peer_points` | numeric(?,?) |  |  |  |  |
| 13 | `feedback` | text |  |  |  |  |
| 14 | `teacher_comment` | text |  |  |  |  |
| 15 | `status` | varchar(?) | Y | 'not_submitted' |  |  |
| 16 | `graded_by` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 17 | `graded_at` | timestamptz |  |  |  |  |
| 18 | `is_published` | bool | Y | FALSE |  |  |
| 19 | `published_at` | timestamptz |  |  |  |  |
| 20 | `created_at` | timestamptz | Y | now() |  |  |
| 21 | `created_by` | uuid |  |  |  |  |
| 22 | `updated_at` | timestamptz | Y | now() |  |  |
| 23 | `updated_by` | uuid |  |  |  |  |
| 24 | `deleted_at` | timestamptz |  |  |  |  |
| 25 | `row_version` | int4 | Y | 0 |  |  |
| 26 | `points_possible` | numeric(?,?) |  |  |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `ix_gradebook_entry_1` (`content_item_id`, `COALESCE(user_id, group_id)`) WHERE deleted_at IS NULL

**Checks**

- `ck_gradebook_entry_status`: status IN ('not_submitted', 'submitted', 'graded', 'excused', 'reassigned')
- `ck_gradebook_entry_1`: num_nonnulls(user_id, group_id) = 1

**Relationships**

- ← `lms.grade_change_log.gradebook_entry_id` — referenced by (1:N)

<a id="lmsgrading_category"></a>
### lms.grading_category

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `course_id` | uuid |  |  | FK → `lms.course` · CASCADE · tenant-composite |  |
| 4 | `cohort_id` | uuid |  |  | FK → `lms.cohort` · CASCADE · tenant-composite |  |
| 5 | `name` | varchar(?) | Y |  |  |  |
| 6 | `weight_percent` | numeric(?,?) |  |  |  |  |
| 7 | `equal_item_weighting` | bool | Y | FALSE |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_grading_category_1`: NOT(course_id IS NOT NULL AND cohort_id IS NOT NULL)

**Relationships**

- ← `lms.content_item.grading_category_id` — referenced by (1:N)

<a id="lmsidempotency_key"></a>
### lms.idempotency_key

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `idempotency_key` | uuid | Y |  |  |  |
| 4 | `user_id` | uuid |  |  |  |  |
| 5 | `operation_name` | varchar(?) | Y |  |  |  |
| 6 | `request_hash` | varchar(?) |  |  |  |  |
| 7 | `response_status` | int2 |  |  |  |  |
| 8 | `response_body` | text |  |  |  |  |
| 9 | `locked_at` | timestamptz |  |  |  |  |
| 10 | `completed_at` | timestamptz |  |  |  |  |
| 11 | `expires_at` | timestamptz | Y |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `idempotency_key`)

<a id="lmsimport_export_job"></a>
### lms.import_export_job

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `direction` | varchar(?) | Y |  |  |  |
| 5 | `package_format` | varchar(?) | Y |  |  |  |
| 6 | `scope` | varchar(?) | Y |  |  |  |
| 7 | `course_id` | uuid |  |  | FK → `lms.course` · SET NULL · tenant-composite |  |
| 8 | `cohort_id` | uuid |  |  | FK → `lms.cohort` · SET NULL · tenant-composite |  |
| 9 | `target_chapter_id` | uuid |  |  | FK → `lms.chapter` · SET NULL · tenant-composite |  |
| 10 | `add_to_repository` | bool | Y | FALSE |  |  |
| 11 | `package_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 12 | `requested_by` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 13 | `requested_on` | date | Y |  |  |  |
| 14 | `started_on` | date |  |  |  |  |
| 15 | `status` | varchar(?) | Y | 'pending' |  |  |
| 16 | `unarchive_status` | varchar(?) |  |  |  |  |
| 17 | `error_log` | text |  |  |  |  |
| 18 | `cohort_code` | varchar(?) |  |  |  |  |
| 19 | `cohort_start_date` | date |  |  |  |  |
| 20 | `course_code` | varchar(?) |  |  |  |  |
| 21 | `course_version` | varchar(?) |  |  |  |  |
| 22 | `course_title` | varchar(?) |  |  |  |  |
| 23 | `created_at` | timestamptz | Y | now() |  |  |
| 24 | `created_by` | uuid |  |  |  |  |
| 25 | `updated_at` | timestamptz | Y | now() |  |  |
| 26 | `updated_by` | uuid |  |  |  |  |
| 27 | `deleted_at` | timestamptz |  |  |  |  |
| 28 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_import_export_job_direction`: direction IN ('import', 'export')
- `ck_import_export_job_package_format`: package_format IN ('native', 'common_cartridge', 'cohort_archive')
- `ck_import_export_job_scope`: scope IN ('course', 'cohort')
- `ck_import_export_job_status`: status IN ('pending', 'in_progress', 'complete', 'error', 'cancelled')
- `ck_import_export_job_unarchive_status`: unarchive_status IN ('pending', 'in_progress', 'complete', 'error')
- `ck_import_export_job_1`: NOT(course_id IS NOT NULL AND cohort_id IS NOT NULL)

<a id="lmsinbox_event"></a>
### lms.inbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `event_id` | uuid | Y |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  | PK |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `received_at` | timestamptz | Y | now() |  |  |
| 5 | `processed_at` | timestamptz |  |  |  |  |
| 6 | `processing_error` | text |  |  |  |  |

<a id="lmsitem_override"></a>
### lms.item_override

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `content_item_id` | uuid | Y |  | FK → `lms.content_item` · CASCADE · tenant-composite |  |
| 4 | `user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `group_id` | uuid |  |  | FK → `lms.assignment_group` · CASCADE · tenant-composite |  |
| 6 | `available_from` | timestamptz |  |  |  |  |
| 7 | `due_at` | timestamptz |  |  |  |  |
| 8 | `extra_attempts` | int4 |  |  |  |  |
| 9 | `extra_time_minutes` | int4 |  |  |  |  |
| 10 | `reason` | varchar(?) |  |  |  |  |
| 11 | `created_at` | timestamptz | Y | now() |  |  |
| 12 | `created_by` | uuid |  |  |  |  |
| 13 | `updated_at` | timestamptz | Y | now() |  |  |
| 14 | `updated_by` | uuid |  |  |  |  |
| 15 | `deleted_at` | timestamptz |  |  |  |  |
| 16 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `ix_item_override_1` (`content_item_id`, `COALESCE(user_id, group_id)`) WHERE deleted_at IS NULL

**Checks**

- `ck_item_override_1`: num_nonnulls(user_id, group_id) = 1

<a id="lmslearning_objective"></a>
### lms.learning_objective

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `course_id` | uuid |  |  | FK → `lms.course` · CASCADE · tenant-composite |  |
| 4 | `chapter_id` | uuid |  |  | FK → `lms.chapter` · CASCADE · tenant-composite |  |
| 5 | `objective` | text | Y |  |  |  |
| 6 | `sort_order` | int4 | Y | 0 |  |  |
| 7 | `copied_from_id` | uuid |  |  | FK → `lms.learning_objective` · SET NULL · tenant-composite |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_learning_objective_1`: num_nonnulls(course_id, chapter_id) = 1

**Relationships**

- ← `lms.learning_objective.copied_from_id` — referenced by (1:N)
- ← `lms.quiz_answer.learning_objective_id` — referenced by (1:N)

<a id="lmslearning_program"></a>
### lms.learning_program

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `title` | varchar(?) | Y |  |  |  |
| 4 | `is_published` | bool | Y | FALSE |  |  |
| 5 | `enforce_course_order` | bool | Y | FALSE |  |  |
| 6 | `course_count` | int4 | Y | 0 |  |  |
| 7 | `member_count` | int4 | Y | 0 |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_learning_program_1` (`tenant_id`, `title`) WHERE deleted_at IS NULL

**Relationships**

- ← `lms.content_author.learning_program_id` — referenced by (1:N)
- ← `lms.learning_program_course.learning_program_id` — referenced by (1:N)
- ← `lms.learning_program_member.learning_program_id` — referenced by (1:N)

<a id="lmslearning_program_course"></a>
### lms.learning_program_course

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `learning_program_id` | uuid | Y |  | FK → `lms.learning_program` · CASCADE · tenant-composite |  |
| 4 | `course_id` | uuid | Y |  | FK → `lms.course` · RESTRICT · tenant-composite |  |
| 5 | `sort_order` | int4 | Y | 0 |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_learning_program_course_1` (`learning_program_id`, `course_id`) WHERE deleted_at IS NULL

<a id="lmslearning_program_member"></a>
### lms.learning_program_member

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `learning_program_id` | uuid | Y |  | FK → `lms.learning_program` · CASCADE · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `progress_percent` | numeric(?,?) | Y | 0 |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `ix_learning_program_member_1` (`learning_program_id`, `user_id`) WHERE deleted_at IS NULL

<a id="lmslesson"></a>
### lms.lesson

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `chapter_id` | uuid | Y |  | FK → `lms.chapter` · CASCADE · tenant-composite |  |
| 4 | `title` | varchar(?) | Y |  |  |  |
| 5 | `sort_order` | int4 | Y |  |  |  |
| 6 | `include_in_preview` | bool | Y | FALSE |  |  |
| 7 | `body` | text |  |  |  |  |
| 8 | `content` | jsonb |  |  |  |  |
| 9 | `teacher_notes` | text |  |  |  |  |
| 10 | `teacher_content` | jsonb |  |  |  |  |
| 11 | `youtube_video_id` | varchar(?) |  |  |  |  |
| 12 | `embedded_question` | text |  |  |  |  |
| 13 | `file_type` | varchar(?) |  |  |  |  |
| 14 | `is_scorm_package` | bool | Y | FALSE |  |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |
| 21 | `version` | int4 | Y | 1 |  |  |
| 22 | `superseded_by_id` | uuid |  |  | FK → `lms.lesson` · SET NULL · tenant-composite |  |
| 23 | `published_at` | timestamptz |  |  |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_lesson_slot` (`chapter_id`, `sort_order`) WHERE deleted_at IS NULL AND superseded_by_id IS NULL

**Checks**

- `ck_lesson_file_type`: file_type IN ('image', 'document', 'pdf')

**Relationships**

- ← `lms.annotation.lesson_id` — referenced by (1:N)
- ← `lms.assignment_submission.lesson_id` — referenced by (1:N)
- ← `lms.content_item.lesson_id` — referenced by (1:N)
- ← `lms.enrollment.current_lesson_id` — referenced by (1:N)
- ← `lms.lesson.superseded_by_id` — referenced by (1:N)
- ← `lms.lesson_activity.lesson_id` — referenced by (1:N)
- ← `lms.lesson_progress.lesson_id` — referenced by (1:N)
- ← `lms.quiz.lesson_id` — referenced by (1:N)
- ← `lms.timetable_entry.lesson_id` — referenced by (1:N)
- ← `lms.video_watch.lesson_id` — referenced by (1:N)

<a id="lmslesson_activity"></a>
### lms.lesson_activity

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `lesson_id` | uuid | Y |  | FK → `lms.lesson` · CASCADE · tenant-composite |  |
| 4 | `quiz_id` | uuid |  |  | FK → `lms.quiz` · CASCADE · tenant-composite |  |
| 5 | `assignment_id` | uuid |  |  | FK → `lms.assignment` · CASCADE · tenant-composite |  |
| 6 | `exercise_id` | uuid |  |  | FK → `lms.programming_exercise` · CASCADE · tenant-composite |  |
| 7 | `teacher_only` | bool | Y | FALSE |  |  |
| 8 | `sort_order` | int4 | Y | 0 |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_lesson_activity_1`: num_nonnulls(quiz_id, assignment_id, exercise_id) = 1

<a id="lmslesson_note"></a>
### lms.lesson_note

A student's own highlight or note on a lesson; private to its writer (lms.ListLessonNotes and the other note
operations answer the writer's rows only).

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `lesson_id` | uuid | Y |  | FK → `lms.lesson` · CASCADE · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) | Writer |
| 5 | `color` | varchar(20) | Y | 'yellow' |  | red, blue, green, yellow, purple |
| 6 | `highlighted_text` | text |  |  |  |  |
| 7 | `note` | text |  |  |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_lesson_note_color`: color IN ('red', 'blue', 'green', 'yellow', 'purple')
- `ck_lesson_note_1`: highlighted_text IS NOT NULL OR note IS NOT NULL

<a id="lmslesson_progress"></a>
### lms.lesson_progress

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 4 | `course_id` | uuid | Y |  | FK → `lms.course` · CASCADE · tenant-composite |  |
| 5 | `chapter_id` | uuid | Y |  | FK → `lms.chapter` · CASCADE · tenant-composite |  |
| 6 | `lesson_id` | uuid |  |  | FK → `lms.lesson` · CASCADE · tenant-composite |  |
| 7 | `status` | varchar(?) | Y |  |  |  |
| 8 | `is_scorm_chapter` | bool | Y | FALSE |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |
| 15 | `scorm_suspend_data` | text |  |  |  |  |
| 16 | `scorm_location` | varchar(?) |  |  |  |  |
| 17 | `scorm_score` | numeric(?,?) |  |  |  |  |
| 18 | `scorm_total_time_seconds` | int4 |  |  |  |  |

**Uniqueness**

- unique index `ix_lesson_progress_1` (`user_id`, `chapter_id`, `COALESCE(lesson_id, CAST('00000000-0000-0000-0000-000000000000' AS uuid))`)

**Checks**

- `ck_lesson_progress_status`: status IN ('complete', 'partially_complete', 'incomplete')

<a id="lmslive_class"></a>
### lms.live_class

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `cohort_id` | uuid |  |  | FK → `lms.cohort` · CASCADE · tenant-composite |  |
| 4 | `title` | varchar(?) | Y |  |  |  |
| 5 | `description` | text |  |  |  |  |
| 6 | `class_date` | date | Y |  |  |  |
| 7 | `start_time` | time | Y |  |  |  |
| 8 | `duration_minutes` | int4 | Y |  |  |  |
| 9 | `timezone` | varchar(?) | Y |  |  |  |
| 10 | `host_user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 11 | `conferencing_provider` | varchar(?) | Y |  |  |  |
| 12 | `conferencing_account_id` | uuid |  |  | FK → `lms.conferencing_account` · SET NULL · tenant-composite |  |
| 13 | `meeting_id` | varchar(?) |  |  |  |  |
| 14 | `meeting_uuid` | varchar(?) |  |  |  |  |
| 15 | `start_url_enc` | text |  |  |  |  |
| 16 | `join_url` | text |  |  |  |  |
| 17 | `meeting_password_enc` | text |  |  |  |  |
| 18 | `auto_recording` | varchar(?) | Y | 'none' |  |  |
| 19 | `calendar_event_id` | uuid |  |  | logical → `comms.event` (cross-module, no FK) |  |
| 20 | `attendee_count` | int4 |  |  |  |  |
| 21 | `invite_status` | varchar(?) | Y | 'pending' |  |  |
| 22 | `invite_attempts` | int4 | Y | 0 |  |  |
| 23 | `moderator_password_enc` | text |  |  |  |  |
| 24 | `welcome_message` | text |  |  |  |  |
| 25 | `provider_created_ref` | varchar(?) |  |  |  |  |
| 26 | `recording_url` | varchar(?) |  |  |  |  |
| 27 | `invitee_count` | int4 |  |  |  |  |
| 28 | `prep_minutes` | int4 |  |  |  |  |
| 29 | `extension_minutes` | int4 |  |  |  |  |
| 30 | `show_in_calendar` | bool | Y | TRUE |  |  |
| 31 | `host_joined` | bool | Y | FALSE |  |  |
| 32 | `is_active` | bool | Y | TRUE |  |  |
| 33 | `ended_at` | timestamptz |  |  |  |  |
| 34 | `created_at` | timestamptz | Y | now() |  |  |
| 35 | `created_by` | uuid |  |  |  |  |
| 36 | `updated_at` | timestamptz | Y | now() |  |  |
| 37 | `updated_by` | uuid |  |  |  |  |
| 38 | `deleted_at` | timestamptz |  |  |  |  |
| 39 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_live_class_duration_minutes_rule`: duration_minutes > 0
- `ck_live_class_conferencing_provider`: conferencing_provider IN ('zoom', 'google_meet', 'bigbluebutton', 'other')
- `ck_live_class_auto_recording`: auto_recording IN ('none', 'local', 'cloud')
- `ck_live_class_invite_status`: invite_status IN ('pending', 'sent', 'retrying', 'failed')

**Relationships**

- ← `lms.content_item.live_class_id` — referenced by (1:N)
- ← `lms.live_class_participant.live_class_id` — referenced by (1:N)
- ← `lms.timetable_entry.live_class_id` — referenced by (1:N)

<a id="lmslive_class_participant"></a>
### lms.live_class_participant

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `live_class_id` | uuid | Y |  | FK → `lms.live_class` · CASCADE · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `joined_at` | timestamptz | Y |  |  |  |
| 6 | `left_at` | timestamptz |  |  |  |  |
| 7 | `duration_minutes` | int4 |  |  |  |  |
| 8 | `join_url_enc` | text |  |  |  |  |
| 9 | `is_presenter` | bool | Y | FALSE |  |  |
| 10 | `attended` | bool | Y | FALSE |  |  |
| 11 | `invite_status` | varchar(?) |  |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_live_class_participant_1`: left_at >= joined_at
- `ck_live_class_participant_times`: left_at IS NULL OR left_at >= joined_at

<a id="lmsmentorship"></a>
### lms.mentorship

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `mentor_user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 4 | `mentee_user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `valid_from` | date |  |  |  |  |
| 6 | `valid_to` | date |  |  |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `ix_mentorship_1` (`mentor_user_id`, `mentee_user_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_mentorship_1`: mentor_user_id <> mentee_user_id

<a id="lmsnumber_sequence"></a>
### lms.number_sequence

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `sequence_code` | varchar(?) | Y |  |  |  |
| 5 | `period_key` | varchar(?) | Y | '' |  |  |
| 6 | `prefix` | varchar(?) |  |  |  |  |
| 7 | `padding` | int2 | Y | 6 |  |  |
| 8 | `next_value` | int8 | Y | 1 |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `institution_id`, `sequence_code`, `period_key`) (NULLS NOT DISTINCT)

<a id="lmsoperation"></a>
### lms.operation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `operation_name` | varchar(?) | Y |  |  |  |
| 4 | `resource_type` | varchar(?) |  |  |  |  |
| 5 | `resource_id` | uuid |  |  |  |  |
| 6 | `status` | varchar(?) | Y | 'running' |  |  |
| 7 | `progress_percent` | int2 |  |  |  |  |
| 8 | `error_code` | varchar(?) |  |  |  |  |
| 9 | `error_message` | varchar(?) |  |  |  |  |
| 10 | `result_entity_type` | varchar(?) |  |  |  |  |
| 11 | `result_entity_id` | uuid |  |  |  |  |
| 12 | `started_by_user_id` | uuid |  |  |  |  |
| 13 | `started_at` | timestamptz | Y | now() |  |  |
| 14 | `finished_at` | timestamptz |  |  |  |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |
| 21 | `result` | text |  |  |  | Outcome of a finished operation as a JSON object (counts, skipped items); NULL while it runs and when it has none. |

**Checks**

- `ck_lms_operation_status`: status IN ('running', 'succeeded', 'failed', 'cancelled')

<a id="lmsoutbox_event"></a>
### lms.outbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | int8 |  |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `aggregate_type` | varchar(?) | Y |  |  |  |
| 5 | `aggregate_id` | uuid | Y |  |  |  |
| 6 | `payload` | text | Y |  |  |  |
| 7 | `occurred_at` | timestamptz | Y | now() |  |  |
| 8 | `published_at` | timestamptz |  |  |  |  |
| 9 | `publish_error` | text |  |  |  | Why the relay set the row aside (not published) |

<a id="lmsparticipation_activity"></a>
### lms.participation_activity

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `title` | varchar(?) | Y |  |  |  |
| 5 | `description` | text |  |  |  |  |
| 6 | `is_in_repository` | bool | Y | FALSE |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Relationships**

- ← `lms.content_item.participation_activity_id` — referenced by (1:N)

<a id="lmspeer_review"></a>
### lms.peer_review

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `assignment_group_id` | uuid | Y |  | FK → `lms.assignment_group` · CASCADE · tenant-composite |  |
| 4 | `reviewer_user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `reviewee_user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 6 | `points` | numeric(?,?) |  |  |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_peer_review_1` (`assignment_group_id`, `reviewer_user_id`, `reviewee_user_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_peer_review_1`: reviewer_user_id <> reviewee_user_id

<a id="lmsplagiarism_account"></a>
### lms.plagiarism_account

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  | UQ |  |
| 3 | `provider` | varchar(?) | Y | 'turnitin' |  |  |
| 4 | `account_ref` | varchar(?) | Y |  |  |  |
| 5 | `sub_account_ref` | varchar(?) |  |  |  |  |
| 6 | `api_url` | varchar(?) |  |  |  |  |
| 7 | `shared_secret_enc` | text |  |  |  |  |
| 8 | `is_default` | bool | Y | FALSE |  |  |
| 9 | `default_teacher_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 10 | `default_teacher_email` | varchar(?) |  |  |  |  |
| 11 | `default_teacher_first_name` | varchar(?) |  |  |  |  |
| 12 | `default_teacher_last_name` | varchar(?) |  |  |  |  |
| 13 | `created_at` | timestamptz | Y | now() |  |  |
| 14 | `created_by` | uuid |  |  |  |  |
| 15 | `updated_at` | timestamptz | Y | now() |  |  |
| 16 | `updated_by` | uuid |  |  |  |  |
| 17 | `deleted_at` | timestamptz |  |  |  |  |
| 18 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `ix_plagiarism_account_1` (`tenant_id`) WHERE is_default AND deleted_at IS NULL

**Relationships**

- ← `lms.plagiarism_cohort_link.plagiarism_account_id` — referenced by (1:N)
- ← `lms.plagiarism_report.plagiarism_account_id` — referenced by (1:N)
- ← `lms.plagiarism_user_link.plagiarism_account_id` — referenced by (1:N)

<a id="lmsplagiarism_cohort_link"></a>
### lms.plagiarism_cohort_link

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `cohort_id` | uuid | Y |  | FK → `lms.cohort` · CASCADE · tenant-composite |  |
| 4 | `plagiarism_account_id` | uuid | Y |  | FK → `lms.plagiarism_account` · RESTRICT · tenant-composite |  |
| 5 | `account_ref` | varchar(?) |  |  |  |  |
| 6 | `provider_class_ref` | varchar(?) |  |  |  |  |
| 7 | `provider_class_title` | varchar(?) |  |  |  |  |
| 8 | `provider_class_end_at` | timestamptz |  |  |  |  |
| 9 | `provider_assignment_ref` | varchar(?) |  |  |  |  |
| 10 | `provider_assignment_title` | varchar(?) |  |  |  |  |
| 11 | `provider_assignment_start_at` | timestamptz |  |  |  |  |
| 12 | `provider_assignment_due_at` | timestamptz |  |  |  |  |
| 13 | `provider_assignment_instructions` | text |  |  |  |  |
| 14 | `student_report_access` | bool | Y | FALSE |  |  |
| 15 | `teacher_email` | varchar(?) |  |  |  |  |
| 16 | `teacher_first_name` | varchar(?) |  |  |  |  |
| 17 | `teacher_last_name` | varchar(?) |  |  |  |  |
| 18 | `class_created` | bool | Y | FALSE |  |  |
| 19 | `assignment_created` | bool | Y | FALSE |  |  |
| 20 | `created_at` | timestamptz | Y | now() |  |  |
| 21 | `created_by` | uuid |  |  |  |  |
| 22 | `updated_at` | timestamptz | Y | now() |  |  |
| 23 | `updated_by` | uuid |  |  |  |  |
| 24 | `deleted_at` | timestamptz |  |  |  |  |
| 25 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_plagiarism_cohort_link_1` (`cohort_id`, `plagiarism_account_id`) WHERE deleted_at IS NULL

<a id="lmsplagiarism_report"></a>
### lms.plagiarism_report

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `plagiarism_account_id` | uuid |  |  | FK → `lms.plagiarism_account` · SET NULL · tenant-composite |  |
| 4 | `entity_attachment_id` | uuid |  |  | logical → `platform.entity_attachment` (cross-module, no FK) |  |
| 5 | `file_id` | uuid | Y |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 6 | `item_kind` | varchar(?) |  |  |  |  |
| 7 | `provider_submission_ref` | varchar(?) |  |  |  |  |
| 8 | `provider_object_ref` | varchar(?) |  |  |  |  |
| 9 | `submitted_at` | timestamptz |  |  |  |  |
| 10 | `provider_submitted_at` | timestamptz |  |  |  |  |
| 11 | `status` | varchar(?) |  |  |  |  |
| 12 | `similarity_score` | varchar(?) |  |  |  |  |
| 13 | `report_url` | varchar(?) |  |  |  |  |
| 14 | `tolerance` | numeric(?,?) |  |  |  |  |
| 15 | `grademark_status` | varchar(?) |  |  |  |  |
| 16 | `error_code` | varchar(?) |  |  |  |  |
| 17 | `error_text` | text |  |  |  |  |
| 18 | `last_checked_at` | timestamptz |  |  |  |  |
| 19 | `received_at` | timestamptz |  |  |  |  |
| 20 | `created_at` | timestamptz | Y | now() |  |  |
| 21 | `created_by` | uuid |  |  |  |  |
| 22 | `updated_at` | timestamptz | Y | now() |  |  |
| 23 | `updated_by` | uuid |  |  |  |  |
| 24 | `deleted_at` | timestamptz |  |  |  |  |
| 25 | `row_version` | int4 | Y | 0 |  |  |
| 26 | `entity_type` | varchar(?) |  |  |  | Kind of record the checked attachment belongs to; decides who may open the report |
| 27 | `entity_id` | uuid |  |  | logical → the lms record named by `entity_type` (no FK) |  |

**Checks**

- `ck_plagiarism_report_item_kind`: item_kind IN ('assignment', 'discussion', 'other')

<a id="lmsplagiarism_user_link"></a>
### lms.plagiarism_user_link

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 4 | `plagiarism_account_id` | uuid | Y |  | FK → `lms.plagiarism_account` · CASCADE · tenant-composite |  |
| 5 | `account_ref` | varchar(?) |  |  |  |  |
| 6 | `provider_user_ref` | varchar(?) | Y |  |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_plagiarism_user_link_1` (`user_id`, `plagiarism_account_id`) WHERE deleted_at IS NULL

<a id="lmsprogramming_exercise"></a>
### lms.programming_exercise

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `title` | varchar(?) | Y |  |  |  |
| 4 | `problem_statement` | text | Y |  |  |  |
| 5 | `language` | varchar(?) | Y |  |  |  |
| 6 | `starter_code` | text |  |  |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_programming_exercise_language`: language IN ('python', 'javascript', 'rust', 'go')

**Relationships**

- ← `lms.content_author.exercise_id` — referenced by (1:N)
- ← `lms.content_item.exercise_id` — referenced by (1:N)
- ← `lms.exercise_submission.exercise_id` — referenced by (1:N)
- ← `lms.exercise_test_case.exercise_id` — referenced by (1:N)
- ← `lms.lesson_activity.exercise_id` — referenced by (1:N)
- ← `lms.timetable_entry.exercise_id` — referenced by (1:N)

<a id="lmsquestion"></a>
### lms.question

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `question_code` | varchar(?) |  |  |  |  |
| 4 | `question_text` | text |  |  |  |  |
| 5 | `question_type` | varchar(?) | Y | 'choices' |  |  |
| 6 | `allows_multiple` | bool | Y | FALSE |  |  |
| 7 | `marks` | numeric(?,?) |  |  |  |  |
| 8 | `title` | varchar(?) |  |  |  |  |
| 9 | `shuffle_options` | bool | Y | FALSE |  |  |
| 10 | `option_label_style` | varchar(?) |  |  |  |  |
| 11 | `max_selections` | int4 |  |  |  |  |
| 12 | `difficulty` | varchar(?) |  |  |  |  |
| 13 | `general_feedback` | text |  |  |  |  |
| 14 | `correct_feedback` | text |  |  |  |  |
| 15 | `incorrect_feedback` | text |  |  |  |  |
| 16 | `model_answer` | text |  |  |  |  |
| 17 | `true_false_answer` | bool |  |  |  |  |
| 18 | `numeric_answer` | varchar(?) |  |  |  |  |
| 19 | `numeric_tolerance` | varchar(?) |  |  |  |  |
| 20 | `numeric_is_range` | bool | Y | FALSE |  |  |
| 21 | `case_sensitive` | bool | Y | FALSE |  |  |
| 22 | `allow_superscript` | bool | Y | FALSE |  |  |
| 23 | `allow_subscript` | bool | Y | FALSE |  |  |
| 24 | `is_retired` | bool | Y | FALSE |  |  |
| 25 | `created_at` | timestamptz | Y | now() |  |  |
| 26 | `created_by` | uuid |  |  |  |  |
| 27 | `updated_at` | timestamptz | Y | now() |  |  |
| 28 | `updated_by` | uuid |  |  |  |  |
| 29 | `deleted_at` | timestamptz |  |  |  |  |
| 30 | `row_version` | int4 | Y | 0 |  |  |
| 31 | `version` | int4 | Y | 1 |  |  |
| 32 | `superseded_by_id` | uuid |  |  | FK → `lms.question` · SET NULL · tenant-composite |  |
| 33 | `published_at` | timestamptz |  |  |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `ix_question_1` (`tenant_id`, `question_code`) WHERE question_code IS NOT NULL

**Checks**

- `ck_question_question_type`: question_type IN ('choices', 'user_input', 'open_ended', 'true_false', 'matching', 'fill_blank', 'numeric', 'hotspot', 'pattern')

**Relationships**

- ← `lms.content_author.question_id` — referenced by (1:N)
- ← `lms.question.superseded_by_id` — referenced by (1:N)
- ← `lms.question_accepted_answer.question_id` — referenced by (1:N)
- ← `lms.question_blank.question_id` — referenced by (1:N)
- ← `lms.question_hotspot.question_id` — referenced by (1:N)
- ← `lms.question_match_pair.question_id` — referenced by (1:N)
- ← `lms.question_option.question_id` — referenced by (1:N)
- ← `lms.question_pattern.question_id` — referenced by (1:N)
- ← `lms.question_pool_item.question_id` — referenced by (1:N)
- ← `lms.quiz_answer.question_id` — referenced by (1:N)
- ← `lms.quiz_question.question_id` — referenced by (1:N)
- ← `lms.quiz_question_history.question_id` — referenced by (1:N)

<a id="lmsquestion_accepted_answer"></a>
### lms.question_accepted_answer

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `question_id` | uuid | Y |  | FK → `lms.question` · CASCADE · tenant-composite |  |
| 4 | `sort_order` | int2 | Y |  |  |  |
| 5 | `answer_text` | text | Y |  |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`question_id`, `sort_order`) — deferrable

<a id="lmsquestion_blank"></a>
### lms.question_blank

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `question_id` | uuid | Y |  | FK → `lms.question` · CASCADE · tenant-composite |  |
| 4 | `blank_text` | varchar(?) | Y |  |  |  |
| 5 | `sort_order` | int4 | Y | 0 |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Relationships**

- ← `lms.question_blank_answer.question_blank_id` — referenced by (1:N)

<a id="lmsquestion_blank_answer"></a>
### lms.question_blank_answer

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `question_blank_id` | uuid | Y |  | FK → `lms.question_blank` · CASCADE · tenant-composite |  |
| 4 | `answer_text` | varchar(?) | Y |  |  |  |
| 5 | `feedback` | text |  |  |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

<a id="lmsquestion_hotspot"></a>
### lms.question_hotspot

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `question_id` | uuid | Y |  | FK → `lms.question` · CASCADE · tenant-composite |  |
| 4 | `hotspot_type` | varchar(?) | Y |  |  |  |
| 5 | `image_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Relationships**

- ← `lms.question_hotspot_region.question_hotspot_id` — referenced by (1:N)

<a id="lmsquestion_hotspot_region"></a>
### lms.question_hotspot_region

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `question_hotspot_id` | uuid | Y |  | FK → `lms.question_hotspot` · CASCADE · tenant-composite |  |
| 4 | `label` | varchar(?) |  |  |  |  |
| 5 | `shape` | varchar(?) | Y |  |  |  |
| 6 | `coordinates` | varchar(?) | Y |  |  |  |
| 7 | `is_correct` | bool | Y | FALSE |  |  |
| 8 | `feedback` | text |  |  |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

<a id="lmsquestion_match_pair"></a>
### lms.question_match_pair

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `question_id` | uuid | Y |  | FK → `lms.question` · CASCADE · tenant-composite |  |
| 4 | `left_text` | varchar(?) | Y |  |  |  |
| 5 | `right_text` | varchar(?) | Y |  |  |  |
| 6 | `sort_order` | int4 | Y | 0 |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

<a id="lmsquestion_option"></a>
### lms.question_option

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `question_id` | uuid | Y |  | FK → `lms.question` · CASCADE · tenant-composite |  |
| 4 | `sort_order` | int2 | Y |  |  |  |
| 5 | `option_text` | text | Y |  |  |  |
| 6 | `is_correct` | bool | Y | FALSE |  |  |
| 7 | `explanation` | text |  |  |  |  |
| 8 | `label` | varchar(?) |  |  |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`question_id`, `sort_order`) — deferrable

<a id="lmsquestion_pattern"></a>
### lms.question_pattern

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `question_id` | uuid | Y |  | FK → `lms.question` · CASCADE · tenant-composite |  |
| 4 | `model_answer` | text | Y |  |  |  |
| 5 | `pattern` | varchar(?) | Y |  |  |  |
| 6 | `sort_order` | int4 | Y | 0 |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

<a id="lmsquestion_pool"></a>
### lms.question_pool

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `title` | varchar(?) | Y |  |  |  |
| 4 | `description` | text |  |  |  |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Relationships**

- ← `lms.question_pool_item.question_pool_id` — referenced by (1:N)

<a id="lmsquestion_pool_item"></a>
### lms.question_pool_item

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `question_pool_id` | uuid | Y |  | FK → `lms.question_pool` · CASCADE · tenant-composite |  |
| 4 | `question_id` | uuid | Y |  | FK → `lms.question` · CASCADE · tenant-composite |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_question_pool_item_1` (`question_pool_id`, `question_id`) WHERE deleted_at IS NULL

<a id="lmsquiz"></a>
### lms.quiz

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `title` | varchar(?) | Y |  |  |  |
| 4 | `course_id` | uuid |  |  | FK → `lms.course` · SET NULL · tenant-composite |  |
| 5 | `lesson_id` | uuid |  |  | FK → `lms.lesson` · SET NULL · tenant-composite |  |
| 6 | `max_attempts` | int4 |  |  |  |  |
| 7 | `passing_percentage` | int4 | Y |  |  |  |
| 8 | `total_marks` | numeric(?,?) | Y |  |  |  |
| 9 | `show_answers` | bool | Y | TRUE |  |  |
| 10 | `show_submission_history` | bool | Y | FALSE |  |  |
| 11 | `shuffle_questions` | bool | Y | FALSE |  |  |
| 12 | `limit_questions_to` | int4 |  |  |  |  |
| 13 | `duration_minutes` | int4 |  |  |  |  |
| 14 | `proctoring_enabled` | bool | Y | FALSE |  |  |
| 15 | `max_violations` | int4 |  |  |  |  |
| 16 | `negative_marking_enabled` | bool | Y | FALSE |  |  |
| 17 | `negative_marks` | numeric(?,?) |  |  |  |  |
| 18 | `scheduling_enabled` | bool | Y | FALSE |  |  |
| 19 | `schedule_start` | timestamptz |  |  |  |  |
| 20 | `schedule_end` | timestamptz |  |  |  |  |
| 21 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 22 | `description` | text |  |  |  |  |
| 23 | `is_auto_graded` | bool | Y | TRUE |  |  |
| 24 | `is_adaptive` | bool | Y | FALSE |  |  |
| 25 | `equal_question_weighting` | bool | Y | FALSE |  |  |
| 26 | `marks_per_question` | numeric(?,?) |  |  |  |  |
| 27 | `allow_pause` | bool | Y | FALSE |  |  |
| 28 | `attempt_scoring` | varchar(?) | Y | 'highest' |  |  |
| 29 | `drop_lowest_attempt` | bool | Y | FALSE |  |  |
| 30 | `draws_from_pool` | bool | Y | FALSE |  |  |
| 31 | `show_feedback` | bool | Y | TRUE |  |  |
| 32 | `is_in_repository` | bool | Y | FALSE |  |  |
| 33 | `created_at` | timestamptz | Y | now() |  |  |
| 34 | `created_by` | uuid |  |  |  |  |
| 35 | `updated_at` | timestamptz | Y | now() |  |  |
| 36 | `updated_by` | uuid |  |  |  |  |
| 37 | `deleted_at` | timestamptz |  |  |  |  |
| 38 | `row_version` | int4 | Y | 0 |  |  |
| 39 | `version` | int4 | Y | 1 |  |  |
| 40 | `superseded_by_id` | uuid |  |  | FK → `lms.quiz` · SET NULL · tenant-composite |  |
| 41 | `published_at` | timestamptz |  |  |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_quiz_passing_percentage_rule`: passing_percentage BETWEEN 0 AND 100
- `ck_quiz_attempt_scoring`: attempt_scoring IN ('highest', 'last', 'average')

**Relationships**

- ← `lms.content_author.quiz_id` — referenced by (1:N)
- ← `lms.content_item.quiz_id` — referenced by (1:N)
- ← `lms.lesson_activity.quiz_id` — referenced by (1:N)
- ← `lms.quiz.superseded_by_id` — referenced by (1:N)
- ← `lms.quiz_question.quiz_id` — referenced by (1:N)
- ← `lms.quiz_question_history.quiz_id` — referenced by (1:N)
- ← `lms.quiz_submission.quiz_id` — referenced by (1:N)
- ← `lms.timetable_entry.quiz_id` — referenced by (1:N)

<a id="lmsquiz_answer"></a>
### lms.quiz_answer

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `quiz_submission_id` | uuid | Y |  | FK → `lms.quiz_submission` · CASCADE · tenant-composite |  |
| 4 | `question_id` | uuid |  |  | FK → `lms.question` · SET NULL · tenant-composite |  |
| 5 | `question_text` | text |  |  |  |  |
| 6 | `question_type` | varchar(?) |  |  |  |  |
| 7 | `answer` | text |  |  |  |  |
| 8 | `is_correct` | bool |  |  |  |  |
| 9 | `marks_awarded` | numeric(?,?) |  |  |  |  |
| 10 | `marks_possible` | numeric(?,?) |  |  |  |  |
| 11 | `sort_order` | int4 |  |  |  |  |
| 12 | `feedback` | text |  |  |  |  |
| 13 | `graded_by` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 14 | `learning_objective_id` | uuid |  |  | FK → `lms.learning_objective` · SET NULL · tenant-composite |  |
| 15 | `time_spent_ms` | int8 |  |  |  |  |
| 16 | `created_at` | timestamptz | Y | now() |  |  |
| 17 | `created_by` | uuid |  |  |  |  |
| 18 | `updated_at` | timestamptz | Y | now() |  |  |
| 19 | `updated_by` | uuid |  |  |  |  |
| 20 | `deleted_at` | timestamptz |  |  |  |  |
| 21 | `row_version` | int4 | Y | 0 |  |  |

<a id="lmsquiz_question"></a>
### lms.quiz_question

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `quiz_id` | uuid | Y |  | FK → `lms.quiz` · CASCADE · tenant-composite |  |
| 4 | `question_id` | uuid | Y |  | FK → `lms.question` · RESTRICT · tenant-composite |  |
| 5 | `marks` | numeric(?,?) | Y |  |  |  |
| 6 | `sort_order` | int4 | Y | 0 |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_quiz_question_1` (`quiz_id`, `question_id`) WHERE deleted_at IS NULL

<a id="lmsquiz_question_history"></a>
### lms.quiz_question_history

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `quiz_id` | uuid | Y |  | FK → `lms.quiz` · CASCADE · tenant-composite |  |
| 4 | `question_id` | uuid | Y |  | FK → `lms.question` · RESTRICT · tenant-composite |  |
| 5 | `marks` | numeric(?,?) |  |  |  |  |
| 6 | `sort_order` | int4 |  |  |  |  |
| 7 | `superseded_at` | timestamptz | Y | now() |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

<a id="lmsquiz_submission"></a>
### lms.quiz_submission

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `quiz_id` | uuid | Y |  | FK → `lms.quiz` · RESTRICT · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `course_id` | uuid |  |  | FK → `lms.course` · SET NULL · tenant-composite |  |
| 6 | `score` | numeric(?,?) |  |  |  |  |
| 7 | `score_out_of` | numeric(?,?) |  |  |  |  |
| 8 | `percentage` | numeric(?,?) |  |  |  |  |
| 9 | `passing_percentage` | numeric(?,?) |  |  |  |  |
| 10 | `violation_count` | int4 | Y | 0 |  |  |
| 11 | `submission_reason` | varchar(?) |  |  |  |  |
| 12 | `content_item_id` | uuid |  |  | FK → `lms.content_item` · SET NULL · tenant-composite |  |
| 13 | `cohort_id` | uuid |  |  | FK → `lms.cohort` · CASCADE · tenant-composite |  |
| 14 | `status` | varchar(?) | Y | 'submitted' |  |  |
| 15 | `started_at` | timestamptz |  |  |  |  |
| 16 | `submitted_at` | timestamptz |  |  |  |  |
| 17 | `time_taken_seconds` | int4 |  |  |  |  |
| 18 | `answered_count` | int4 |  |  |  |  |
| 19 | `correct_count` | int4 |  |  |  |  |
| 20 | `incorrect_count` | int4 |  |  |  |  |
| 21 | `is_reassigned` | bool | Y | FALSE |  |  |
| 22 | `reassigned_from_id` | uuid |  |  | FK → `lms.quiz_submission` · SET NULL · tenant-composite |  |
| 23 | `teacher_comment` | text |  |  |  |  |
| 24 | `created_at` | timestamptz | Y | now() |  |  |
| 25 | `created_by` | uuid |  |  |  |  |
| 26 | `updated_at` | timestamptz | Y | now() |  |  |
| 27 | `updated_by` | uuid |  |  |  |  |
| 28 | `deleted_at` | timestamptz |  |  |  |  |
| 29 | `row_version` | int4 | Y | 0 |  |  |
| 30 | `attempt_number` | int4 | Y | 1 |  |  |
| 31 | `resumed_at` | timestamptz |  |  |  | When the running attempt last started or resumed (its timer runs from here; NULL: from started_at). |
| 32 | `last_client_sequence` | int8 |  |  |  | Highest client sequence of the autosaves kept; a save with a lower or equal sequence is ignored as stale. |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_quiz_submission_attempt` (`quiz_id`, `user_id`, `content_item_id`, `attempt_number`) WHERE deleted_at IS NULL

**Checks**

- `ck_quiz_submission_status`: status IN ('assigned', 'in_progress', 'paused', 'submitted', 'graded')

**Relationships**

- ← `lms.gradebook_entry.quiz_submission_id` — referenced by (1:N)
- ← `lms.quiz_answer.quiz_submission_id` — referenced by (1:N)
- ← `lms.quiz_submission.reassigned_from_id` — referenced by (1:N)
- ← `lms.quiz_violation.quiz_submission_id` — referenced by (1:N)
- ← `lms.reassignment_log.quiz_submission_id` — referenced by (1:N)

<a id="lmsquiz_violation"></a>
### lms.quiz_violation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `quiz_submission_id` | uuid | Y |  | FK → `lms.quiz_submission` · CASCADE · tenant-composite |  |
| 4 | `event_type` | varchar(?) | Y |  |  |  |
| 5 | `severity` | varchar(?) | Y |  |  |  |
| 6 | `occurred_at` | timestamptz | Y | now() |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_quiz_violation_event_type`: event_type IN ('tab_switch', 'no_face', 'multiple_faces', 'focus_loss', 'camera_disconnect')
- `ck_quiz_violation_severity`: severity IN ('violation', 'warning')

<a id="lmsreassignment_log"></a>
### lms.reassignment_log

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `content_item_id` | uuid | Y |  | FK → `lms.content_item` · RESTRICT · tenant-composite |  |
| 4 | `user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `group_id` | uuid |  |  | FK → `lms.assignment_group` · CASCADE · tenant-composite |  |
| 6 | `quiz_submission_id` | uuid |  |  | FK → `lms.quiz_submission` · SET NULL · tenant-composite |  |
| 7 | `comment` | text |  |  |  |  |
| 8 | `reassigned_by` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 9 | `reassigned_at` | timestamptz | Y |  |  |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_reassignment_log_1`: num_nonnulls(user_id, group_id) = 1

<a id="lmsrelated_course"></a>
### lms.related_course

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `course_id` | uuid | Y |  | FK → `lms.course` · CASCADE · tenant-composite |  |
| 4 | `related_course_id` | uuid | Y |  | FK → `lms.course` · CASCADE · tenant-composite |  |
| 5 | `sort_order` | int4 | Y | 0 |  |  |
| 6 | `author_profile_id` | uuid |  |  | FK → `lms.course_author_profile` · SET NULL · tenant-composite |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_related_course_1` (`course_id`, `related_course_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_related_course_1`: related_course_id <> course_id

<a id="lmsrelated_material"></a>
### lms.related_material

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `ebook_id` | uuid | Y |  | FK → `lms.ebook` · CASCADE · tenant-composite |  |
| 4 | `title` | varchar(?) |  |  |  |  |
| 5 | `url` | varchar(?) | Y |  |  |  |
| 6 | `summary` | text |  |  |  |  |
| 7 | `source` | varchar(?) |  |  |  |  |
| 8 | `media_type` | varchar(?) |  |  |  |  |
| 9 | `content_length` | int8 |  |  |  |  |
| 10 | `content_length_band` | varchar(?) |  |  |  |  |
| 11 | `video_length_seconds` | int8 |  |  |  |  |
| 12 | `video_length_band` | varchar(?) |  |  |  |  |
| 13 | `initial_rank` | int4 |  |  |  |  |
| 14 | `interactivity_level` | int4 |  |  |  |  |
| 15 | `interactivity_band` | varchar(?) |  |  |  |  |
| 16 | `conceptual_level` | varchar(?) |  |  |  |  |
| 17 | `language_complexity` | numeric(?,?) |  |  |  |  |
| 18 | `language_complexity_band` | varchar(?) |  |  |  |  |
| 19 | `created_at` | timestamptz | Y | now() |  |  |
| 20 | `created_by` | uuid |  |  |  |  |
| 21 | `updated_at` | timestamptz | Y | now() |  |  |
| 22 | `updated_by` | uuid |  |  |  |  |
| 23 | `deleted_at` | timestamptz |  |  |  |  |
| 24 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Relationships**

- ← `lms.related_material_rank_item.related_material_id` — referenced by (1:N)

<a id="lmsrelated_material_rank"></a>
### lms.related_material_rank

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 4 | `ebook_id` | uuid | Y |  | FK → `lms.ebook` · CASCADE · tenant-composite |  |
| 5 | `course_id` | uuid |  |  | FK → `lms.course` · CASCADE · tenant-composite |  |
| 6 | `file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 7 | `subject_tags` | varchar(?) |  |  |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `ix_related_material_rank_1` (`user_id`, `ebook_id`, `COALESCE(course_id, CAST('00000000-0000-0000-0000-000000000000' AS uuid))`, `COALESCE(file_id, CAST('00000000-0000-0000-0000-000000000000' AS uuid))`) WHERE deleted_at IS NULL

**Relationships**

- ← `lms.related_material_rank_item.related_material_rank_id` — referenced by (1:N)

<a id="lmsrelated_material_rank_item"></a>
### lms.related_material_rank_item

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `related_material_rank_id` | uuid | Y |  | FK → `lms.related_material_rank` · CASCADE · tenant-composite |  |
| 4 | `related_material_id` | uuid | Y |  | FK → `lms.related_material` · CASCADE · tenant-composite |  |
| 5 | `position` | int2 | Y |  |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`related_material_rank_id`, `position`) — deferrable
- unique index `uq_related_material_rank_item_2` (`related_material_rank_id`, `related_material_id`) WHERE deleted_at IS NULL

<a id="lmsrelease_grant"></a>
### lms.release_grant

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `content_item_id` | uuid | Y |  | FK → `lms.content_item` · CASCADE · tenant-composite |  |
| 4 | `chapter_id` | uuid |  |  | FK → `lms.chapter` · CASCADE · tenant-composite |  |
| 5 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 6 | `is_released` | bool | Y | FALSE |  |  |
| 7 | `access_granted` | bool | Y | FALSE |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_release_grant_1` (`content_item_id`, `user_id`) WHERE deleted_at IS NULL

<a id="lmsrelease_rule"></a>
### lms.release_rule

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `course_id` | uuid |  |  | FK → `lms.course` · CASCADE · tenant-composite |  |
| 4 | `cohort_id` | uuid |  |  | FK → `lms.cohort` · CASCADE · tenant-composite |  |
| 5 | `source_content_item_id` | uuid | Y |  | FK → `lms.content_item` · CASCADE · tenant-composite |  |
| 6 | `title` | varchar(?) |  |  |  |  |
| 7 | `stop_if_true` | bool | Y | FALSE |  |  |
| 8 | `is_disabled` | bool | Y | FALSE |  |  |
| 9 | `reassign_source` | bool | Y | FALSE |  |  |
| 10 | `sort_order` | int4 | Y | 0 |  |  |
| 11 | `copied_from_id` | uuid |  |  | FK → `lms.release_rule` · SET NULL · tenant-composite |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_release_rule_1`: num_nonnulls(course_id, cohort_id) = 1

**Relationships**

- ← `lms.release_rule.copied_from_id` — referenced by (1:N)
- ← `lms.release_rule_condition.release_rule_id` — referenced by (1:N)
- ← `lms.release_rule_target.release_rule_id` — referenced by (1:N)

<a id="lmsrelease_rule_condition"></a>
### lms.release_rule_condition

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `release_rule_id` | uuid | Y |  | FK → `lms.release_rule` · CASCADE · tenant-composite |  |
| 4 | `operator` | varchar(?) | Y |  |  |  |
| 5 | `value` | varchar(?) |  |  |  |  |
| 6 | `value2` | varchar(?) |  |  |  |  |
| 7 | `sort_order` | int4 | Y | 0 |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |
| 14 | `condition_type` | varchar(?) | Y |  |  |  |

**Checks**

- `ck_release_rule_condition_type`: condition_type IN ('completed', 'not_completed', 'submitted', 'not_submitted', 'score_at_least', 'score_below', 'viewed', 'date_after', 'date_before', 'attempts_at_least')

<a id="lmsrelease_rule_target"></a>
### lms.release_rule_target

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `release_rule_id` | uuid | Y |  | FK → `lms.release_rule` · CASCADE · tenant-composite |  |
| 4 | `content_item_id` | uuid |  |  | FK → `lms.content_item` · CASCADE · tenant-composite |  |
| 5 | `chapter_id` | uuid |  |  | FK → `lms.chapter` · CASCADE · tenant-composite |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_release_rule_target_1`: num_nonnulls(content_item_id, chapter_id) = 1

<a id="lmsresource_category"></a>
### lms.resource_category

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `name` | varchar(?) | Y |  |  |  |
| 4 | `created_at` | timestamptz | Y | now() |  |  |
| 5 | `created_by` | uuid |  |  |  |  |
| 6 | `updated_at` | timestamptz | Y | now() |  |  |
| 7 | `updated_by` | uuid |  |  |  |  |
| 8 | `deleted_at` | timestamptz |  |  |  |  |
| 9 | `row_version` | int4 | Y | 0 |  |  |
| 10 | `code` | varchar(?) | Y |  |  |  |
| 11 | `sort_order` | int4 | Y | 0 |  |  |
| 12 | `is_active` | bool | Y | TRUE |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_resource_category_scope_code` (`tenant_id`, `code`) WHERE deleted_at IS NULL
- unique index `uq_resource_category_1` (`tenant_id`, `name`) WHERE deleted_at IS NULL

**Relationships**

- ← `lms.ebook.category_id` — referenced by (1:N)
- ← `lms.web_resource.category_id` — referenced by (1:N)

<a id="lmsrubric"></a>
### lms.rubric

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `title` | varchar(?) | Y |  |  |  |
| 5 | `is_default` | bool | Y | FALSE |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |
| 12 | `version` | int4 | Y | 1 |  |  |
| 13 | `superseded_by_id` | uuid |  |  | FK → `lms.rubric` · SET NULL · tenant-composite |  |
| 14 | `published_at` | timestamptz |  |  |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Relationships**

- ← `lms.content_item.rubric_id` — referenced by (1:N)
- ← `lms.rubric.superseded_by_id` — referenced by (1:N)
- ← `lms.rubric_cell.rubric_id` — referenced by (1:N)
- ← `lms.rubric_criterion.rubric_id` — referenced by (1:N)
- ← `lms.rubric_level.rubric_id` — referenced by (1:N)

<a id="lmsrubric_assessment"></a>
### lms.rubric_assessment

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `content_item_id` | uuid | Y |  | FK → `lms.content_item` · RESTRICT · tenant-composite |  |
| 4 | `user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `group_id` | uuid |  |  | FK → `lms.assignment_group` · CASCADE · tenant-composite |  |
| 6 | `rubric_cell_id` | uuid | Y |  | FK → `lms.rubric_cell` · RESTRICT · tenant-composite |  |
| 7 | `comment` | text |  |  |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |
| 14 | `rubric_version` | int4 |  |  |  |  |
| 15 | `points_awarded` | numeric(?,?) |  |  |  |  |

**Uniqueness**

- unique index `ix_rubric_assessment_1` (`content_item_id`, `COALESCE(user_id, group_id)`, `rubric_cell_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_rubric_assessment_1`: num_nonnulls(user_id, group_id) = 1

<a id="lmsrubric_cell"></a>
### lms.rubric_cell

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `rubric_id` | uuid | Y |  | FK → `lms.rubric` · CASCADE · tenant-composite |  |
| 4 | `rubric_criterion_id` | uuid | Y |  | FK → `lms.rubric_criterion` · CASCADE · tenant-composite |  |
| 5 | `rubric_level_id` | uuid | Y |  | FK → `lms.rubric_level` · CASCADE · tenant-composite |  |
| 6 | `description` | text |  |  |  |  |
| 7 | `sort_order` | int4 | Y | 0 |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_rubric_cell_1` (`rubric_criterion_id`, `rubric_level_id`) WHERE deleted_at IS NULL

**Relationships**

- ← `lms.rubric_assessment.rubric_cell_id` — referenced by (1:N)

<a id="lmsrubric_criterion"></a>
### lms.rubric_criterion

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `rubric_id` | uuid | Y |  | FK → `lms.rubric` · CASCADE · tenant-composite |  |
| 4 | `name` | varchar(?) | Y |  |  |  |
| 5 | `group_name` | varchar(?) |  |  |  |  |
| 6 | `weight_percent` | numeric(?,?) |  |  |  |  |
| 7 | `group_weight_percent` | numeric(?,?) |  |  |  |  |
| 8 | `sort_order` | int4 | Y | 0 |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Relationships**

- ← `lms.rubric_cell.rubric_criterion_id` — referenced by (1:N)

<a id="lmsrubric_level"></a>
### lms.rubric_level

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `rubric_id` | uuid | Y |  | FK → `lms.rubric` · CASCADE · tenant-composite |  |
| 4 | `label` | varchar(?) | Y |  |  |  |
| 5 | `description` | text |  |  |  |  |
| 6 | `score_percent` | numeric(?,?) |  |  |  |  |
| 7 | `sort_order` | int4 | Y | 0 |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Relationships**

- ← `lms.rubric_cell.rubric_level_id` — referenced by (1:N)

<a id="lmssettings"></a>
### lms.settings

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  | UQ |  |
| 3 | `livecode_url` | varchar(?) |  |  |  |  |
| 4 | `default_currency_id` | uuid |  |  | logical → `master.currency` (cross-module, no FK) |  |
| 5 | `apply_gst` | bool | Y | FALSE |  |  |
| 6 | `show_usd_equivalent` | bool | Y | FALSE |  |  |
| 7 | `apply_rounding` | bool | Y | FALSE |  |  |
| 8 | `payment_gateway` | varchar(?) |  |  |  |  |
| 9 | `identify_user_category` | bool | Y | FALSE |  |  |
| 10 | `persona_captured` | bool | Y | FALSE |  |  |
| 11 | `lms_is_default_home` | bool | Y | FALSE |  |  |
| 12 | `send_calendar_invite_for_evaluations` | bool | Y | FALSE |  |  |
| 13 | `allow_guest_access` | bool | Y | TRUE |  |  |
| 14 | `disable_signup` | bool | Y | TRUE |  |  |
| 15 | `disable_pwa` | bool | Y | FALSE |  |  |
| 16 | `allow_job_posting` | bool | Y | TRUE |  |  |
| 17 | `certified_members_page` | bool | Y | FALSE |  |  |
| 18 | `prevent_skipping_videos` | bool | Y | FALSE |  |  |
| 19 | `lesson_dwell_seconds` | int4 | Y | 30 |  |  |
| 20 | `enforce_video_completion` | bool | Y | TRUE |  |  |
| 21 | `enforce_quiz_completion` | bool | Y | TRUE |  |  |
| 22 | `enforce_quiz_passing` | bool | Y | TRUE |  |  |
| 23 | `enforce_open_ended_quiz_passing` | bool | Y | TRUE |  |  |
| 24 | `enforce_assignment_completion` | bool | Y | TRUE |  |  |
| 25 | `enforce_assignment_passing` | bool | Y | FALSE |  |  |
| 26 | `enforce_exercise_completion` | bool | Y | FALSE |  |  |
| 27 | `enforce_exercise_passing` | bool | Y | FALSE |  |  |
| 28 | `show_courses` | bool | Y | TRUE |  |  |
| 29 | `show_students` | bool | Y | TRUE |  |  |
| 30 | `show_assessments` | bool | Y | TRUE |  |  |
| 31 | `show_live_class` | bool | Y | TRUE |  |  |
| 32 | `show_discussions` | bool | Y | TRUE |  |  |
| 33 | `show_emails` | bool | Y | TRUE |  |  |
| 34 | `show_dashboard` | bool | Y | TRUE |  |  |
| 35 | `show_day_view` | bool | Y | FALSE |  |  |
| 36 | `text_direction` | varchar(?) | Y | 'auto' |  |  |
| 37 | `notify_published_courses` | varchar(?) |  |  |  |  |
| 38 | `notify_published_cohorts` | varchar(?) |  |  |  |  |
| 39 | `send_payment_reminders_cohort` | bool | Y | FALSE |  |  |
| 40 | `send_payment_reminders_course` | bool | Y | FALSE |  |  |
| 41 | `mentor_request_template_id` | uuid |  |  | logical → `comms.message_template` (cross-module, no FK) |  |
| 42 | `mentor_status_template_id` | uuid |  |  | logical → `comms.message_template` (cross-module, no FK) |  |
| 43 | `cohort_confirmation_template_id` | uuid |  |  | logical → `comms.message_template` (cross-module, no FK) |  |
| 44 | `certificate_template_id` | uuid |  |  | logical → `comms.message_template` (cross-module, no FK) |  |
| 45 | `payment_reminder_template_id` | uuid |  |  | logical → `comms.message_template` (cross-module, no FK) |  |
| 46 | `custom_signup_content` | text |  |  |  |  |
| 47 | `meta_description` | text |  |  |  |  |
| 48 | `meta_keywords` | text |  |  |  |  |
| 49 | `meta_image_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 50 | `contact_us_email` | varchar(?) |  |  |  |  |
| 51 | `contact_us_url` | varchar(?) |  |  |  |  |
| 52 | `unsplash_access_key_enc` | text |  |  |  |  |
| 53 | `demo_data_present` | bool | Y | FALSE |  |  |
| 54 | `created_at` | timestamptz | Y | now() |  |  |
| 55 | `created_by` | uuid |  |  |  |  |
| 56 | `updated_at` | timestamptz | Y | now() |  |  |
| 57 | `updated_by` | uuid |  |  |  |  |
| 58 | `deleted_at` | timestamptz |  |  |  |  |
| 59 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_settings_1` (`tenant_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_settings_text_direction`: text_direction IN ('auto', 'ltr', 'rtl')
- `ck_settings_notify_published_courses`: notify_published_courses IN ('email', 'in_app')
- `ck_settings_notify_published_cohorts`: notify_published_cohorts IN ('email', 'in_app')

**Relationships**

- ← `lms.settings_primary_country.settings_id` — referenced by (1:N)
- ← `lms.settings_sidebar_item.settings_id` — referenced by (1:N)

<a id="lmssettings_primary_country"></a>
### lms.settings_primary_country

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `settings_id` | uuid | Y |  | FK → `lms.settings` · CASCADE · tenant-composite |  |
| 4 | `country_id` | uuid | Y |  | logical → `master.country` (cross-module, no FK) |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_settings_primary_country_1` (`settings_id`, `country_id`) WHERE deleted_at IS NULL

<a id="lmssettings_sidebar_item"></a>
### lms.settings_sidebar_item

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `settings_id` | uuid | Y |  | FK → `lms.settings` · CASCADE · tenant-composite |  |
| 4 | `sort_order` | int4 | Y | 0 |  |  |
| 5 | `item_type` | varchar(?) | Y |  |  |  |
| 6 | `item_key` | varchar(?) |  |  |  |  |
| 7 | `title` | varchar(?) |  |  |  |  |
| 8 | `icon` | varchar(?) |  |  |  |  |
| 9 | `web_page_route` | varchar(?) |  |  |  |  |
| 10 | `route` | varchar(?) |  |  |  |  |
| 11 | `url` | varchar(?) |  |  |  |  |
| 12 | `is_standard` | bool | Y | FALSE |  |  |
| 13 | `is_hidden` | bool | Y | FALSE |  |  |
| 14 | `open_in_new_window` | bool | Y | TRUE |  |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_settings_sidebar_item_item_type`: item_type IN ('built_in', 'web_page', 'route', 'external')

<a id="lmsstudent_alert"></a>
### lms.student_alert

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 4 | `cohort_id` | uuid |  |  | FK → `lms.cohort` · CASCADE · tenant-composite |  |
| 5 | `course_id` | uuid |  |  | FK → `lms.course` · SET NULL · tenant-composite |  |
| 6 | `metric_code` | varchar(?) |  |  |  |  |
| 7 | `content_item_id` | uuid |  |  | FK → `lms.content_item` · SET NULL · tenant-composite |  |
| 8 | `sent_at` | timestamptz | Y | now() |  |  |
| 9 | `sent_by` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 10 | `subject` | varchar(?) |  |  |  |  |
| 11 | `message` | text |  |  |  |  |
| 12 | `details` | text |  |  |  |  |
| 13 | `read_at` | timestamptz |  |  |  |  |
| 14 | `created_at` | timestamptz | Y | now() |  |  |
| 15 | `created_by` | uuid |  |  |  |  |
| 16 | `updated_at` | timestamptz | Y | now() |  |  |
| 17 | `updated_by` | uuid |  |  |  |  |
| 18 | `deleted_at` | timestamptz |  |  |  |  |
| 19 | `row_version` | int4 | Y | 0 |  |  |

<a id="lmstimetable_entry"></a>
### lms.timetable_entry

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `cohort_id` | uuid |  |  | FK → `lms.cohort` · CASCADE · tenant-composite |  |
| 4 | `timetable_template_id` | uuid |  |  | FK → `lms.timetable_template` · CASCADE · tenant-composite |  |
| 5 | `lesson_id` | uuid |  |  | FK → `lms.lesson` · CASCADE · tenant-composite |  |
| 6 | `quiz_id` | uuid |  |  | FK → `lms.quiz` · CASCADE · tenant-composite |  |
| 7 | `assignment_id` | uuid |  |  | FK → `lms.assignment` · CASCADE · tenant-composite |  |
| 8 | `exercise_id` | uuid |  |  | FK → `lms.programming_exercise` · CASCADE · tenant-composite |  |
| 9 | `live_class_id` | uuid |  |  | FK → `lms.live_class` · CASCADE · tenant-composite |  |
| 10 | `entry_date` | date |  |  |  |  |
| 11 | `day_number` | int4 |  |  |  |  |
| 12 | `start_time` | time |  |  |  |  |
| 13 | `end_time` | time |  |  |  |  |
| 14 | `duration_minutes` | int4 |  |  |  |  |
| 15 | `is_milestone` | bool | Y | FALSE |  |  |
| 16 | `created_at` | timestamptz | Y | now() |  |  |
| 17 | `created_by` | uuid |  |  |  |  |
| 18 | `updated_at` | timestamptz | Y | now() |  |  |
| 19 | `updated_by` | uuid |  |  |  |  |
| 20 | `deleted_at` | timestamptz |  |  |  |  |
| 21 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_timetable_entry_1`: num_nonnulls(cohort_id, timetable_template_id) = 1
- `ck_timetable_entry_2`: num_nonnulls(lesson_id, quiz_id, assignment_id, exercise_id, live_class_id) = 1

<a id="lmstimetable_legend"></a>
### lms.timetable_legend

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `cohort_id` | uuid |  |  | FK → `lms.cohort` · CASCADE · tenant-composite |  |
| 4 | `timetable_template_id` | uuid |  |  | FK → `lms.timetable_template` · CASCADE · tenant-composite |  |
| 5 | `item_kind` | varchar(?) | Y |  |  |  |
| 6 | `color` | varchar(?) | Y |  |  |  |
| 7 | `label` | varchar(?) | Y |  |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_timetable_legend_item_kind`: item_kind IN ('lesson', 'quiz', 'assignment', 'exercise', 'live_class')
- `ck_timetable_legend_1`: num_nonnulls(cohort_id, timetable_template_id) = 1

<a id="lmstimetable_template"></a>
### lms.timetable_template

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `title` | varchar(?) | Y |  |  |  |
| 4 | `created_at` | timestamptz | Y | now() |  |  |
| 5 | `created_by` | uuid |  |  |  |  |
| 6 | `updated_at` | timestamptz | Y | now() |  |  |
| 7 | `updated_by` | uuid |  |  |  |  |
| 8 | `deleted_at` | timestamptz |  |  |  |  |
| 9 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Relationships**

- ← `lms.cohort.timetable_template_id` — referenced by (1:N)
- ← `lms.timetable_entry.timetable_template_id` — referenced by (1:N)
- ← `lms.timetable_legend.timetable_template_id` — referenced by (1:N)

<a id="lmsvideo_watch"></a>
### lms.video_watch

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `lesson_id` | uuid | Y |  | FK → `lms.lesson` · CASCADE · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `video_source` | varchar(?) | Y |  |  |  |
| 6 | `watch_seconds` | numeric(?,?) | Y |  |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_video_watch_1` (`lesson_id`, `user_id`, `video_source`) WHERE deleted_at IS NULL

<a id="lmsweb_resource"></a>
### lms.web_resource

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `title` | varchar(?) | Y |  |  |  |
| 5 | `description` | text |  |  |  |  |
| 6 | `resource_type` | varchar(?) | Y |  |  |  |
| 7 | `url` | varchar(?) |  |  |  |  |
| 8 | `launch_file` | varchar(?) |  |  |  |  |
| 9 | `window_features` | varchar(?) |  |  |  |  |
| 10 | `category_id` | uuid |  |  | FK → `lms.resource_category` · SET NULL · tenant-composite |  |
| 11 | `is_in_repository` | bool | Y | FALSE |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_web_resource_resource_type`: resource_type IN ('url', 'package')

**Relationships**

- ← `lms.content_item.web_resource_id` — referenced by (1:N)

## `finance` — Fees & Payments

Fees & Payments: Fee heads, fee plans with installments, student dues, invoices, coupons and payments (student fees, LMS purchases, admissions). Separate from SaaS subscriptions and payroll.

24 tables: [`acquisition_source`](#financeacquisition_source), [`coupon`](#financecoupon), [`coupon_scope`](#financecoupon_scope), [`course_fee_default`](#financecourse_fee_default), [`fee_concession`](#financefee_concession), [`fee_head`](#financefee_head), [`fee_plan`](#financefee_plan), [`fee_plan_installment`](#financefee_plan_installment), [`fee_plan_installment_component`](#financefee_plan_installment_component), [`fee_structure`](#financefee_structure), [`gateway_event`](#financegateway_event), [`idempotency_key`](#financeidempotency_key), [`inbox_event`](#financeinbox_event), [`invoice`](#financeinvoice), [`invoice_line`](#financeinvoice_line), [`number_sequence`](#financenumber_sequence), [`operation`](#financeoperation), [`outbox_event`](#financeoutbox_event), [`payment`](#financepayment), [`payment_allocation`](#financepayment_allocation), [`payment_gateway_account`](#financepayment_gateway_account), [`program_enrollment_fee`](#financeprogram_enrollment_fee), [`refund`](#financerefund), [`student_fee_due`](#financestudent_fee_due)

<a id="financeacquisition_source"></a>
### finance.acquisition_source

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `name` | varchar(?) | Y |  |  |  |
| 4 | `created_at` | timestamptz | Y | now() |  |  |
| 5 | `created_by` | uuid |  |  |  |  |
| 6 | `updated_at` | timestamptz | Y | now() |  |  |
| 7 | `updated_by` | uuid |  |  |  |  |
| 8 | `deleted_at` | timestamptz |  |  |  |  |
| 9 | `row_version` | int4 | Y | 0 |  |  |
| 10 | `code` | varchar(?) | Y |  |  |  |
| 11 | `sort_order` | int4 | Y | 0 |  |  |
| 12 | `is_active` | bool | Y | TRUE |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_acquisition_source_scope_code` (`tenant_id`, `code`) WHERE deleted_at IS NULL
- unique index `uq_acquisition_source_1` (`tenant_id`, `name`) WHERE deleted_at IS NULL

**Relationships**

- ← `finance.payment.acquisition_source_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `lms.cohort_enrollment.acquisition_source_id`

<a id="financecoupon"></a>
### finance.coupon

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `code` | varchar(?) | Y |  |  |  |
| 4 | `discount_type` | varchar(?) | Y |  |  |  |
| 5 | `percentage_discount` | numeric(?,?) |  |  |  |  |
| 6 | `fixed_discount` | numeric(?,?) |  |  |  |  |
| 7 | `expires_on` | date |  |  |  |  |
| 8 | `usage_limit` | int4 |  |  |  |  |
| 9 | `redemption_count` | int4 | Y | 0 |  |  |
| 10 | `is_enabled` | bool | Y | TRUE |  |  |
| 11 | `created_at` | timestamptz | Y | now() |  |  |
| 12 | `created_by` | uuid |  |  |  |  |
| 13 | `updated_at` | timestamptz | Y | now() |  |  |
| 14 | `updated_by` | uuid |  |  |  |  |
| 15 | `deleted_at` | timestamptz |  |  |  |  |
| 16 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_coupon_1` (`tenant_id`, `code`) WHERE deleted_at IS NULL

**Checks**

- `ck_coupon_discount_type`: discount_type IN ('percentage', 'fixed_amount')
- `ck_coupon_1`: (discount_type = 'percentage' AND percentage_discount IS NOT NULL) OR (discount_type = 'fixed_amount' AND fixed_discount IS NOT NULL)
- `ck_coupon_redemption_rule`: usage_limit IS NULL OR redemption_count <= usage_limit

**Relationships**

- ← `finance.coupon_scope.coupon_id` — referenced by (1:N)
- ← `finance.payment.coupon_id` — referenced by (1:N)

<a id="financecoupon_scope"></a>
### finance.coupon_scope

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `coupon_id` | uuid | Y |  | FK → `finance.coupon` · CASCADE · tenant-composite |  |
| 4 | `lms_course_id` | uuid |  |  | logical → `lms.course` (cross-module, no FK) |  |
| 5 | `lms_cohort_id` | uuid |  |  | logical → `lms.cohort` (cross-module, no FK) |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_coupon_scope_1`: num_nonnulls(lms_course_id, lms_cohort_id) = 1

<a id="financecourse_fee_default"></a>
### finance.course_fee_default

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `course_id` | uuid | Y |  | logical → `academics.course` (cross-module, no FK) · UQ |  |
| 4 | `fee_plan_id` | uuid | Y |  | FK → `finance.fee_plan` · RESTRICT · tenant-composite |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_course_fee_default_1` (`course_id`) WHERE deleted_at IS NULL

<a id="financefee_concession"></a>
### finance.fee_concession

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `student_id` | uuid | Y |  | logical → `sis.student` (cross-module, no FK) |  |
| 5 | `academic_year_id` | uuid | Y |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 6 | `fee_head_id` | uuid |  |  | FK → `finance.fee_head` · SET NULL · tenant-composite |  |
| 7 | `concession_type` | varchar(?) | Y |  |  |  |
| 8 | `concession_value` | numeric(?,?) | Y |  |  |  |
| 9 | `reason` | varchar(?) |  |  |  |  |
| 10 | `approved_by_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 11 | `status` | varchar(?) | Y | 'active' |  |  |
| 12 | `valid_from` | date |  |  |  |  |
| 13 | `valid_until` | date |  |  |  |  |
| 14 | `created_at` | timestamptz | Y | now() |  |  |
| 15 | `created_by` | uuid |  |  |  |  |
| 16 | `updated_at` | timestamptz | Y | now() |  |  |
| 17 | `updated_by` | uuid |  |  |  |  |
| 18 | `deleted_at` | timestamptz |  |  |  |  |
| 19 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_fee_concession_type`: concession_type IN ('percent', 'amount')
- `ck_fee_concession_status`: status IN ('active', 'revoked')
- `ck_fee_concession_value_rule`: concession_value >= 0
- `ck_fee_concession_percent_rule`: concession_type <> 'percent' OR concession_value <= 100

**Relationships**

- ← `finance.student_fee_due.concession_id` — referenced by (1:N)

<a id="financefee_head"></a>
### finance.fee_head

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `code` | varchar(?) | Y |  |  |  |
| 5 | `name` | varchar(?) | Y |  |  |  |
| 6 | `description` | text |  |  |  |  |
| 7 | `default_amount` | numeric(?,?) |  |  |  |  |
| 8 | `currency_id` | uuid |  |  | logical → `master.currency` (cross-module, no FK) |  |
| 9 | `ledger_account_code` | varchar(?) |  |  |  |  |
| 10 | `is_active` | bool | Y | TRUE |  |  |
| 11 | `created_at` | timestamptz | Y | now() |  |  |
| 12 | `created_by` | uuid |  |  |  |  |
| 13 | `updated_at` | timestamptz | Y | now() |  |  |
| 14 | `updated_by` | uuid |  |  |  |  |
| 15 | `deleted_at` | timestamptz |  |  |  |  |
| 16 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_fee_head_1` (`tenant_id`, `code`) WHERE deleted_at IS NULL

**Relationships**

- ← `finance.fee_concession.fee_head_id` — referenced by (1:N)
- ← `finance.fee_plan_installment_component.fee_head_id` — referenced by (1:N)
- ← `finance.invoice_line.fee_head_id` — referenced by (1:N)
- ← `finance.program_enrollment_fee.fee_head_id` — referenced by (1:N)
- ← `finance.student_fee_due.fee_head_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `admission.admission_cycle.application_fee_head_id`
- ← `admission.admission_cycle_fee.fee_head_id`

<a id="financefee_plan"></a>
### finance.fee_plan

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `code` | varchar(?) | Y |  |  |  |
| 5 | `name` | varchar(?) | Y |  |  |  |
| 6 | `schedule_basis` | varchar(?) | Y | 'fixed_days' |  |  |
| 7 | `offset_days` | int4 |  |  |  |  |
| 8 | `offset_direction` | varchar(?) |  |  |  |  |
| 9 | `discount_percent` | numeric(?,?) |  |  |  |  |
| 10 | `notes` | text |  |  |  |  |
| 11 | `is_active` | bool | Y | TRUE |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_fee_plan_1` (`institution_id`, `code`) WHERE deleted_at IS NULL

**Checks**

- `ck_fee_plan_schedule_basis`: schedule_basis IN ('fixed_days', 'fixed_date')
- `ck_fee_plan_offset_direction`: offset_direction IN ('before', 'after')

**Relationships**

- ← `finance.course_fee_default.fee_plan_id` — referenced by (1:N)
- ← `finance.fee_plan_installment.fee_plan_id` — referenced by (1:N)
- ← `finance.fee_structure.fee_plan_id` — referenced by (1:N)
- ← `finance.program_enrollment_fee.fee_plan_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `admission.application.fee_plan_id`

<a id="financefee_plan_installment"></a>
### finance.fee_plan_installment

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `fee_plan_id` | uuid | Y |  | FK → `finance.fee_plan` · CASCADE · tenant-composite |  |
| 4 | `sequence` | int4 | Y |  |  |  |
| 5 | `due_days` | int4 |  |  |  |  |
| 6 | `due_date` | date |  |  |  |  |
| 7 | `share_percent` | numeric(?,?) | Y |  |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`fee_plan_id`, `sequence`) — deferrable
- UNIQUE (`id`, `tenant_id`)

**Relationships**

- ← `finance.fee_plan_installment_component.installment_id` — referenced by (1:N)
- ← `finance.student_fee_due.installment_id` — referenced by (1:N)

<a id="financefee_plan_installment_component"></a>
### finance.fee_plan_installment_component

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `installment_id` | uuid | Y |  | FK → `finance.fee_plan_installment` · CASCADE · tenant-composite |  |
| 4 | `fee_head_id` | uuid | Y |  | FK → `finance.fee_head` · RESTRICT · tenant-composite |  |
| 5 | `sequence` | int4 |  |  |  |  |
| 6 | `share_percent` | numeric(?,?) | Y |  |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_fee_plan_installment_component_1` (`installment_id`, `fee_head_id`) WHERE deleted_at IS NULL

<a id="financefee_structure"></a>
### finance.fee_structure

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `academic_year_id` | uuid | Y |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `grade_level_id` | uuid |  |  | logical → `academics.grade_level` (cross-module, no FK) |  |
| 6 | `program_id` | uuid |  |  | logical → `academics.program` (cross-module, no FK) |  |
| 7 | `fee_plan_id` | uuid | Y |  | FK → `finance.fee_plan` · RESTRICT · tenant-composite |  |
| 8 | `name` | varchar(?) | Y |  |  |  |
| 9 | `status` | varchar(?) | Y | 'draft' |  |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_fee_structure_scope` (`tenant_id`, `academic_year_id`, `grade_level_id`, `program_id`, `fee_plan_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_fee_structure_status`: status IN ('draft', 'published', 'archived')

**Relationships**

- ← `finance.student_fee_due.fee_structure_id` — referenced by (1:N)

<a id="financegateway_event"></a>
### finance.gateway_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `gateway_account_id` | uuid |  |  | FK → `finance.payment_gateway_account` · SET NULL · tenant-composite |  |
| 4 | `gateway_name` | varchar(?) | Y |  |  |  |
| 5 | `gateway_event_id` | varchar(?) |  |  |  |  |
| 6 | `event_type` | varchar(?) | Y |  |  |  |
| 7 | `payment_id` | uuid |  |  | FK → `finance.payment` · RESTRICT · tenant-composite |  |
| 8 | `raw_payload` | text |  |  |  |  |
| 9 | `received_at` | timestamptz | Y | now() |  |  |
| 10 | `processed_at` | timestamptz |  |  |  |  |
| 11 | `processing_error` | text |  |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_gateway_event_dedupe` (`tenant_id`, `gateway_name`, `gateway_event_id`) WHERE gateway_event_id IS NOT NULL

<a id="financeidempotency_key"></a>
### finance.idempotency_key

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `idempotency_key` | uuid | Y |  |  |  |
| 4 | `user_id` | uuid |  |  |  |  |
| 5 | `operation_name` | varchar(?) | Y |  |  |  |
| 6 | `request_hash` | varchar(?) |  |  |  |  |
| 7 | `response_status` | int2 |  |  |  |  |
| 8 | `response_body` | text |  |  |  |  |
| 9 | `locked_at` | timestamptz |  |  |  |  |
| 10 | `completed_at` | timestamptz |  |  |  |  |
| 11 | `expires_at` | timestamptz | Y |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `idempotency_key`)

<a id="financeinbox_event"></a>
### finance.inbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `event_id` | uuid | Y |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  | PK |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `received_at` | timestamptz | Y | now() |  |  |
| 5 | `processed_at` | timestamptz |  |  |  |  |
| 6 | `processing_error` | text |  |  |  |  |

<a id="financeinvoice"></a>
### finance.invoice

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `invoice_number` | varchar(?) | Y |  |  |  |
| 5 | `student_id` | uuid |  |  | logical → `sis.student` (cross-module, no FK) |  |
| 6 | `bill_to_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 7 | `bill_to_name` | varchar(?) | Y |  |  |  |
| 8 | `invoice_date` | date | Y |  |  |  |
| 9 | `due_date` | date |  |  |  |  |
| 10 | `currency_id` | uuid | Y |  | logical → `master.currency` (cross-module, no FK) |  |
| 11 | `subtotal` | numeric(?,?) | Y | 0 |  |  |
| 12 | `discount_total` | numeric(?,?) | Y | 0 |  |  |
| 13 | `tax_total` | numeric(?,?) | Y | 0 |  |  |
| 14 | `total` | numeric(?,?) | Y | 0 |  |  |
| 15 | `amount_paid` | numeric(?,?) | Y | 0 |  |  |
| 16 | `status` | varchar(?) | Y | 'draft' |  |  |
| 17 | `created_at` | timestamptz | Y | now() |  |  |
| 18 | `created_by` | uuid |  |  |  |  |
| 19 | `updated_at` | timestamptz | Y | now() |  |  |
| 20 | `updated_by` | uuid |  |  |  |  |
| 21 | `deleted_at` | timestamptz |  |  |  |  |
| 22 | `row_version` | int4 | Y | 0 |  |  |
| 23 | `invoice_type` | varchar(?) | Y | 'invoice' |  |  |
| 24 | `credit_for_invoice_id` | uuid |  |  | FK → `finance.invoice` · RESTRICT · tenant-composite |  |
| 25 | `posted_at` | timestamptz |  |  |  |  |
| 26 | `cancelled_at` | timestamptz |  |  |  |  |
| 27 | `document_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `invoice_number`)
- UNIQUE (`id`, `tenant_id`)
- UNIQUE (`id`, `currency_id`)

**Checks**

- `ck_invoice_status`: status IN ('draft', 'posted', 'partially_paid', 'paid', 'cancelled')
- `ck_invoice_1`: student_id IS NOT NULL OR bill_to_user_id IS NOT NULL
- `ck_invoice_type`: invoice_type IN ('invoice', 'credit_note')
- `ck_invoice_credit`: invoice_type <> 'credit_note' OR credit_for_invoice_id IS NOT NULL
- `ck_invoice_totals`: subtotal >= 0 AND discount_total >= 0 AND tax_total >= 0 AND total >= 0 AND amount_paid >= 0
- `ck_invoice_no_self_credit`: credit_for_invoice_id IS NULL OR credit_for_invoice_id <> id

**Relationships**

- ← `finance.invoice.credit_for_invoice_id` — referenced by (1:N)
- ← `finance.invoice_line.invoice_id` — referenced by (1:N)
- ← `finance.payment.invoice_id` — referenced by (1:N)
- ← `finance.payment_allocation.invoice_id` — referenced by (1:N)
- ← `finance.payment_allocation.invoice_id` — referenced by (1:N)
- ← `finance.student_fee_due.invoice_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `library.loan.invoice_id`

<a id="financeinvoice_line"></a>
### finance.invoice_line

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `invoice_id` | uuid | Y |  | FK → `finance.invoice` · CASCADE · tenant-composite |  |
| 4 | `fee_head_id` | uuid |  |  | FK → `finance.fee_head` · RESTRICT · tenant-composite |  |
| 5 | `description` | varchar(?) | Y |  |  |  |
| 6 | `quantity` | numeric(?,?) | Y | 1 |  |  |
| 7 | `unit_price` | numeric(?,?) | Y |  |  |  |
| 8 | `discount_amount` | numeric(?,?) | Y | 0 |  |  |
| 9 | `tax_amount` | numeric(?,?) | Y | 0 |  |  |
| 10 | `line_total` | numeric(?,?) | Y |  |  |  |
| 11 | `created_at` | timestamptz | Y | now() |  |  |
| 12 | `created_by` | uuid |  |  |  |  |
| 13 | `updated_at` | timestamptz | Y | now() |  |  |
| 14 | `updated_by` | uuid |  |  |  |  |
| 15 | `deleted_at` | timestamptz |  |  |  |  |
| 16 | `row_version` | int4 | Y | 0 |  |  |
| 17 | `tax_rate_percent` | numeric(?,?) |  |  |  |  |
| 18 | `tax_code` | varchar(?) |  |  |  |  |

**Checks**

- `ck_invoice_line_total_rule`: line_total = quantity * unit_price - discount_amount + tax_amount

<a id="financenumber_sequence"></a>
### finance.number_sequence

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `sequence_code` | varchar(?) | Y |  |  |  |
| 5 | `period_key` | varchar(?) | Y | '' |  |  |
| 6 | `prefix` | varchar(?) |  |  |  |  |
| 7 | `padding` | int2 | Y | 6 |  |  |
| 8 | `next_value` | int8 | Y | 1 |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `institution_id`, `sequence_code`, `period_key`) (NULLS NOT DISTINCT)

<a id="financeoperation"></a>
### finance.operation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `operation_name` | varchar(?) | Y |  |  |  |
| 4 | `resource_type` | varchar(?) |  |  |  |  |
| 5 | `resource_id` | uuid |  |  |  |  |
| 6 | `status` | varchar(?) | Y | 'running' |  |  |
| 7 | `progress_percent` | int2 |  |  |  |  |
| 8 | `error_code` | varchar(?) |  |  |  |  |
| 9 | `error_message` | varchar(?) |  |  |  |  |
| 10 | `result_entity_type` | varchar(?) |  |  |  |  |
| 11 | `result_entity_id` | uuid |  |  |  |  |
| 12 | `started_by_user_id` | uuid |  |  |  |  |
| 13 | `started_at` | timestamptz | Y | now() |  |  |
| 14 | `finished_at` | timestamptz |  |  |  |  |
| 15 | `result` | text |  |  |  | Outcome of a finished operation as a JSON object (counts, skipped items). |
| 16 | `created_at` | timestamptz | Y | now() |  |  |
| 17 | `created_by` | uuid |  |  |  |  |
| 18 | `updated_at` | timestamptz | Y | now() |  |  |
| 19 | `updated_by` | uuid |  |  |  |  |
| 20 | `deleted_at` | timestamptz |  |  |  |  |
| 21 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_finance_operation_status`: status IN ('running', 'succeeded', 'failed', 'cancelled')

<a id="financeoutbox_event"></a>
### finance.outbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | int8 |  |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `aggregate_type` | varchar(?) | Y |  |  |  |
| 5 | `aggregate_id` | uuid | Y |  |  |  |
| 6 | `payload` | text | Y |  |  |  |
| 7 | `occurred_at` | timestamptz | Y | now() |  |  |
| 8 | `published_at` | timestamptz |  |  |  |  |
| 9 | `publish_error` | text |  |  |  | Why the relay set the row aside (not published) |

<a id="financepayment"></a>
### finance.payment

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `payer_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `purpose` | varchar(?) | Y |  |  |  |
| 6 | `invoice_id` | uuid |  |  | FK → `finance.invoice` · RESTRICT · tenant-composite |  |
| 7 | `lms_course_id` | uuid |  |  | logical → `lms.course` (cross-module, no FK) |  |
| 8 | `lms_cohort_id` | uuid |  |  | logical → `lms.cohort` (cross-module, no FK) |  |
| 9 | `admission_application_id` | uuid |  |  | logical → `admission.application` (cross-module, no FK) |  |
| 10 | `gateway_name` | varchar(?) |  |  |  |  |
| 11 | `gateway_order_id` | varchar(?) |  |  |  |  |
| 12 | `gateway_payment_id` | varchar(?) |  |  |  |  |
| 13 | `original_amount` | numeric(?,?) |  |  |  |  |
| 14 | `discount_amount` | numeric(?,?) | Y | 0 |  |  |
| 15 | `amount` | numeric(?,?) | Y |  |  |  |
| 16 | `amount_with_tax` | numeric(?,?) |  |  |  |  |
| 17 | `currency_id` | uuid | Y |  | logical → `master.currency` (cross-module, no FK) |  |
| 18 | `coupon_id` | uuid |  |  | FK → `finance.coupon` · SET NULL · tenant-composite |  |
| 19 | `coupon_code` | varchar(?) |  |  |  |  |
| 20 | `billing_name` | varchar(?) | Y |  |  |  |
| 21 | `billing_address_id` | uuid |  |  | logical → `people.person_address` (cross-module, no FK) |  |
| 22 | `tax_registration_number` | varchar(?) |  |  |  |  |
| 23 | `tax_identity_enc` | text |  |  |  |  |
| 24 | `acquisition_source_id` | uuid |  |  | FK → `finance.acquisition_source` · SET NULL · tenant-composite |  |
| 25 | `payer_consent` | bool | Y | FALSE |  |  |
| 26 | `status` | varchar(?) | Y | 'initiated' |  |  |
| 27 | `received_at` | timestamptz |  |  |  |  |
| 28 | `created_at` | timestamptz | Y | now() |  |  |
| 29 | `created_by` | uuid |  |  |  |  |
| 30 | `updated_at` | timestamptz | Y | now() |  |  |
| 31 | `updated_by` | uuid |  |  |  |  |
| 32 | `deleted_at` | timestamptz |  |  |  |  |
| 33 | `row_version` | int4 | Y | 0 |  |  |
| 34 | `payer_person_id` | uuid |  |  | logical → `people.person` (cross-module, no FK) |  |
| 35 | `payer_name` | varchar(?) |  |  |  |  |
| 36 | `payment_method` | varchar(?) | Y | 'gateway' |  |  |
| 37 | `receipt_number` | varchar(?) |  |  |  |  |
| 38 | `instrument_reference` | varchar(?) |  |  |  |  |
| 39 | `instrument_date` | date |  |  |  |  |
| 40 | `bank_name` | varchar(?) |  |  |  |  |
| 41 | `gateway_account_id` | uuid |  |  | FK → `finance.payment_gateway_account` · RESTRICT · tenant-composite |  |
| 42 | `received_by_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 43 | `gateway_currency` | varchar(?) |  |  |  | ISO code of the order opened at the gateway. |
| 44 | `gateway_amount_minor` | int8 |  |  |  | Order amount at the gateway, in minor units. |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- UNIQUE (`id`, `currency_id`)
- unique index `uq_payment_gateway_txn` (`tenant_id`, `gateway_name`, `gateway_payment_id`) WHERE gateway_payment_id IS NOT NULL
- unique index `uq_payment_receipt` (`tenant_id`, `receipt_number`) WHERE receipt_number IS NOT NULL

**Checks**

- `ck_payment_purpose`: purpose IN ('invoice', 'fees', 'lms_course', 'lms_cohort', 'lms_certificate', 'admission')
- `ck_payment_amount_rule`: amount >= 0
- `ck_payment_status`: status IN ('initiated', 'received', 'failed', 'refunded')
- `ck_payment_2`: purpose NOT IN ('lms_course', 'lms_certificate') OR lms_course_id IS NOT NULL
- `ck_payment_3`: purpose <> 'lms_cohort' OR lms_cohort_id IS NOT NULL
- `ck_payment_4`: purpose <> 'admission' OR admission_application_id IS NOT NULL
- `ck_payment_method`: payment_method IN ('gateway', 'cash', 'cheque', 'dd', 'bank_transfer', 'card_pos', 'adjustment')
- `ck_payment_payer`: payer_user_id IS NOT NULL OR payer_person_id IS NOT NULL OR payer_name IS NOT NULL
- `ck_payment_invoice_purpose`: purpose <> 'invoice' OR invoice_id IS NOT NULL

**Relationships**

- ← `finance.gateway_event.payment_id` — referenced by (1:N)
- ← `finance.payment_allocation.payment_id` — referenced by (1:N)
- ← `finance.payment_allocation.payment_id` — referenced by (1:N)
- ← `finance.refund.payment_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `lms.cohort_enrollment.payment_id`
- ← `lms.enrollment.payment_id`

<a id="financepayment_allocation"></a>
### finance.payment_allocation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `payment_id` | uuid | Y |  | FK → `finance.payment` · NO ACTION |  |
| 4 | `invoice_id` | uuid |  |  | FK → `finance.invoice` · NO ACTION |  |
| 5 | `student_fee_due_id` | uuid |  |  | FK → `finance.student_fee_due` · NO ACTION |  |
| 6 | `amount` | numeric(?,?) | Y |  |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |
| 13 | `currency_id` | uuid | Y |  | logical → `master.currency` (cross-module, no FK) |  |

**Checks**

- `ck_payment_allocation_amount_rule`: amount > 0
- `ck_payment_allocation_target`: (invoice_id IS NOT NULL) <> (student_fee_due_id IS NOT NULL)

<a id="financepayment_gateway_account"></a>
### finance.payment_gateway_account

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `gateway_name` | varchar(?) | Y |  |  |  |
| 5 | `display_name` | varchar(?) | Y |  |  |  |
| 6 | `merchant_id` | varchar(?) |  |  |  |  |
| 7 | `key_id` | varchar(?) |  |  |  |  |
| 8 | `key_secret_enc` | text |  |  |  |  |
| 9 | `webhook_secret_enc` | text |  |  |  |  |
| 10 | `currency_id` | uuid |  |  | logical → `master.currency` (cross-module, no FK) |  |
| 11 | `is_default` | bool | Y | FALSE |  |  |
| 12 | `is_active` | bool | Y | TRUE |  |  |
| 13 | `created_at` | timestamptz | Y | now() |  |  |
| 14 | `created_by` | uuid |  |  |  |  |
| 15 | `updated_at` | timestamptz | Y | now() |  |  |
| 16 | `updated_by` | uuid |  |  |  |  |
| 17 | `deleted_at` | timestamptz |  |  |  |  |
| 18 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_payment_gateway_account_1` (`tenant_id`, `gateway_name`, `display_name`) WHERE deleted_at IS NULL
- unique index `uq_payment_gateway_account_default` (`tenant_id`, `COALESCE(institution_id, CAST('00000000-0000-0000-0000-000000000000' AS uuid))`) WHERE is_default AND deleted_at IS NULL

**Relationships**

- ← `finance.gateway_event.gateway_account_id` — referenced by (1:N)
- ← `finance.payment.gateway_account_id` — referenced by (1:N)

<a id="financeprogram_enrollment_fee"></a>
### finance.program_enrollment_fee

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `program_enrollment_id` | uuid | Y |  | logical → `sis.program_enrollment` (cross-module, no FK) · UQ |  |
| 4 | `fee_plan_id` | uuid |  |  | FK → `finance.fee_plan` · RESTRICT · tenant-composite |  |
| 5 | `fee_head_id` | uuid |  |  | FK → `finance.fee_head` · SET NULL · tenant-composite |  |
| 6 | `fee_start_date` | date |  |  |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_program_enrollment_fee_1` (`program_enrollment_id`) WHERE deleted_at IS NULL

<a id="financerefund"></a>
### finance.refund

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `payment_id` | uuid | Y |  | FK → `finance.payment` · RESTRICT · tenant-composite |  |
| 4 | `amount` | numeric(?,?) | Y |  |  |  |
| 5 | `reason` | varchar(?) |  |  |  |  |
| 6 | `status` | varchar(?) | Y | 'initiated' |  |  |
| 7 | `gateway_refund_id` | varchar(?) |  |  |  |  |
| 8 | `refund_method` | varchar(?) | Y | 'gateway' |  |  |
| 9 | `refunded_by_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 10 | `refunded_at` | timestamptz |  |  |  |  |
| 11 | `request_key` | uuid |  |  |  | Idempotency-Key of the request that opened the refund. |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_refund_gateway` (`tenant_id`, `gateway_refund_id`) WHERE gateway_refund_id IS NOT NULL
- unique index `uq_refund_request` (`tenant_id`, `payment_id`, `request_key`) WHERE request_key IS NOT NULL

**Checks**

- `ck_refund_amount_rule`: amount > 0
- `ck_refund_status`: status IN ('initiated', 'processed', 'failed', 'cancelled')
- `ck_refund_method`: refund_method IN ('gateway', 'cash', 'cheque', 'bank_transfer', 'adjustment')

<a id="financestudent_fee_due"></a>
### finance.student_fee_due

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `student_id` | uuid | Y |  | logical → `sis.student` (cross-module, no FK) |  |
| 5 | `program_enrollment_id` | uuid |  |  | logical → `sis.program_enrollment` (cross-module, no FK) |  |
| 6 | `student_enrollment_id` | uuid |  |  | logical → `sis.student_enrollment` (cross-module, no FK) |  |
| 7 | `installment_id` | uuid |  |  | FK → `finance.fee_plan_installment` · RESTRICT · tenant-composite |  |
| 8 | `fee_head_id` | uuid |  |  | FK → `finance.fee_head` · RESTRICT · tenant-composite |  |
| 9 | `due_date` | date |  |  |  |  |
| 10 | `amount` | numeric(?,?) | Y |  |  |  |
| 11 | `currency_id` | uuid | Y |  | logical → `master.currency` (cross-module, no FK) |  |
| 12 | `discount_percent` | numeric(?,?) |  |  |  |  |
| 13 | `fee_factor` | numeric(?,?) |  |  |  |  |
| 14 | `status` | varchar(?) | Y | 'draft' |  |  |
| 15 | `invoice_id` | uuid |  |  | FK → `finance.invoice` · RESTRICT · tenant-composite |  |
| 16 | `created_at` | timestamptz | Y | now() |  |  |
| 17 | `created_by` | uuid |  |  |  |  |
| 18 | `updated_at` | timestamptz | Y | now() |  |  |
| 19 | `updated_by` | uuid |  |  |  |  |
| 20 | `deleted_at` | timestamptz |  |  |  |  |
| 21 | `row_version` | int4 | Y | 0 |  |  |
| 22 | `academic_year_id` | uuid | Y |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 23 | `fee_structure_id` | uuid |  |  | FK → `finance.fee_structure` · SET NULL · tenant-composite |  |
| 24 | `concession_id` | uuid |  |  | FK → `finance.fee_concession` · SET NULL · tenant-composite |  |
| 25 | `discount_amount` | numeric(?,?) | Y | 0 |  |  |
| 26 | `net_amount` | numeric(?,?) |  |  |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- UNIQUE (`id`, `currency_id`)
- unique index `uq_student_fee_due_slot` (`tenant_id`, `student_id`, `academic_year_id`, `installment_id`, `fee_head_id`) WHERE deleted_at IS NULL AND status <> 'cancelled' AND (installment_id IS NOT NULL OR fee_head_id IS NOT NULL)

**Checks**

- `ck_student_fee_due_amount_rule`: amount >= 0
- `ck_student_fee_due_status`: status IN ('draft', 'invoiced', 'cancelled')
- `ck_student_fee_due_discount_rule`: discount_amount >= 0 AND discount_amount <= amount
- `ck_student_fee_due_net_rule`: net_amount IS NULL OR net_amount = amount - discount_amount

**Relationships**

- ← `finance.payment_allocation.student_fee_due_id` — referenced by (1:N)
- ← `finance.payment_allocation.student_fee_due_id` — referenced by (1:N)

<a id="financestaff_record"></a>
### finance.staff_record

The login account and institution of each staff record, kept from `hr.staff` events, so `RaiseChargeInvoice` bills an account only when it is staff of the charge's institution.

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `tenant_id` | uuid | Y |  | PK | Owning tenant. Row-level security key. |
| 2 | `staff_id` | uuid | Y |  | PK | `hr.staff.id` (kept from its events) |
| 3 | `user_id` | uuid |  |  |  | Login account of the record |
| 4 | `institution_id` | uuid |  |  |  | Institution of the record |
| 5 | `is_deleted` | bool | Y | false |  | Deleted in its module; kept so a late event cannot restore it |
| 6 | `source_version` | int4 | Y |  |  | Source row version this entry reflects; older events are ignored |
| 7 | `synced_at` | timestamptz | Y |  |  | When an event last changed the row |

**Checks**

- `ck_staff_record_source_version`: source_version >= 1

## `library` — Library

Library: Catalogue (media titles), copies, library cards, circulation (loans, returns, fines), reservations and purchase requests.

22 tables: [`author`](#libraryauthor), [`idempotency_key`](#libraryidempotency_key), [`inbox_event`](#libraryinbox_event), [`library_card`](#librarylibrary_card), [`library_card_type`](#librarylibrary_card_type), [`loan`](#libraryloan), [`media`](#librarymedia), [`media_author`](#librarymedia_author), [`media_category`](#librarymedia_category), [`media_course`](#librarymedia_course), [`media_publisher`](#librarymedia_publisher), [`media_subject`](#librarymedia_subject), [`media_type`](#librarymedia_type), [`media_unit`](#librarymedia_unit), [`number_sequence`](#librarynumber_sequence), [`operation`](#libraryoperation), [`outbox_event`](#libraryoutbox_event), [`publisher`](#librarypublisher), [`purchase_request`](#librarypurchase_request), [`purchase_request_course`](#librarypurchase_request_course), [`purchase_request_subject`](#librarypurchase_request_subject), [`reservation`](#libraryreservation)

<a id="libraryauthor"></a>
### library.author

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `name` | varchar(?) | Y |  |  |  |
| 4 | `created_at` | timestamptz | Y | now() |  |  |
| 5 | `created_by` | uuid |  |  |  |  |
| 6 | `updated_at` | timestamptz | Y | now() |  |  |
| 7 | `updated_by` | uuid |  |  |  |  |
| 8 | `deleted_at` | timestamptz |  |  |  |  |
| 9 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Relationships**

- ← `library.media_author.author_id` — referenced by (1:N)

<a id="libraryidempotency_key"></a>
### library.idempotency_key

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `idempotency_key` | uuid | Y |  |  |  |
| 4 | `user_id` | uuid |  |  |  |  |
| 5 | `operation_name` | varchar(?) | Y |  |  |  |
| 6 | `request_hash` | varchar(?) |  |  |  |  |
| 7 | `response_status` | int2 |  |  |  |  |
| 8 | `response_body` | text |  |  |  |  |
| 9 | `locked_at` | timestamptz |  |  |  |  |
| 10 | `completed_at` | timestamptz |  |  |  |  |
| 11 | `expires_at` | timestamptz | Y |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `idempotency_key`)

<a id="libraryinbox_event"></a>
### library.inbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `event_id` | uuid | Y |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  | PK |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `received_at` | timestamptz | Y | now() |  |  |
| 5 | `processed_at` | timestamptz |  |  |  |  |
| 6 | `processing_error` | text |  |  |  |  |

<a id="librarylibrary_card"></a>
### library.library_card

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `card_number` | varchar(?) | Y |  |  |  |
| 5 | `card_type_id` | uuid | Y |  | FK → `library.library_card_type` · RESTRICT · tenant-composite |  |
| 6 | `holder_type` | varchar(?) | Y |  |  |  |
| 7 | `person_id` | uuid | Y |  | logical → `people.person` (cross-module, no FK) |  |
| 8 | `student_id` | uuid |  |  | logical → `sis.student` (cross-module, no FK) |  |
| 9 | `staff_id` | uuid |  |  | logical → `hr.staff` (cross-module, no FK) |  |
| 10 | `max_items_override` | int4 |  |  |  |  |
| 11 | `issue_date` | date | Y |  |  |  |
| 12 | `expiry_date` | date |  |  |  |  |
| 13 | `status` | varchar(?) | Y | 'active' |  |  |
| 14 | `created_at` | timestamptz | Y | now() |  |  |
| 15 | `created_by` | uuid |  |  |  |  |
| 16 | `updated_at` | timestamptz | Y | now() |  |  |
| 17 | `updated_by` | uuid |  |  |  |  |
| 18 | `deleted_at` | timestamptz |  |  |  |  |
| 19 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`institution_id`, `card_number`)
- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_library_card_holder_type`: holder_type IN ('student', 'staff')
- `ck_library_card_status`: status IN ('active', 'inactive', 'expired', 'lost')
- `ck_library_card_1`: (holder_type = 'student' AND student_id IS NOT NULL AND staff_id IS NULL) OR (holder_type = 'staff' AND staff_id IS NOT NULL AND student_id IS NULL)

**Relationships**

- ← `library.loan.library_card_id` — referenced by (1:N)

<a id="librarylibrary_card_type"></a>
### library.library_card_type

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `name` | varchar(?) | Y |  |  |  |
| 4 | `max_items` | int4 | Y |  |  |  |
| 5 | `loan_days` | int4 | Y |  |  |  |
| 6 | `fine_per_day` | numeric(?,?) | Y | 0 |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |
| 13 | `code` | varchar(?) | Y |  |  |  |
| 14 | `sort_order` | int4 | Y | 0 |  |  |
| 15 | `is_active` | bool | Y | TRUE |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_library_card_type_scope_code` (`tenant_id`, `code`) WHERE deleted_at IS NULL
- unique index `uq_library_card_type_1` (`tenant_id`, `name`) WHERE deleted_at IS NULL

**Checks**

- `ck_library_card_type_max_items_rule`: max_items > 0
- `ck_library_card_type_loan_days_rule`: loan_days > 0

**Relationships**

- ← `library.library_card.card_type_id` — referenced by (1:N)

<a id="libraryloan"></a>
### library.loan

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `media_unit_id` | uuid | Y |  | FK → `library.media_unit` · RESTRICT |  |
| 4 | `media_id` | uuid | Y |  | FK → `library.media` · RESTRICT · tenant-composite |  |
| 5 | `library_card_id` | uuid | Y |  | FK → `library.library_card` · RESTRICT · tenant-composite |  |
| 6 | `borrower_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 7 | `issued_on` | date |  |  |  |  |
| 8 | `due_on` | date |  |  |  |  |
| 9 | `returned_on` | date |  |  |  |  |
| 10 | `fine_amount` | numeric(?,?) | Y | 0 |  |  |
| 11 | `invoice_id` | uuid |  |  | logical → `finance.invoice` (cross-module, no FK) |  |
| 12 | `issued_by_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 13 | `status` | varchar(?) | Y | 'issued' |  |  |
| 14 | `created_at` | timestamptz | Y | now() |  |  |
| 15 | `created_by` | uuid |  |  |  |  |
| 16 | `updated_at` | timestamptz | Y | now() |  |  |
| 17 | `updated_by` | uuid |  |  |  |  |
| 18 | `deleted_at` | timestamptz |  |  |  |  |
| 19 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_loan_open_unit` (`media_unit_id`) WHERE status IN ('reserved', 'issued', 'return_pending') AND deleted_at IS NULL

**Checks**

- `ck_loan_status`: status IN ('draft', 'reserved', 'issued', 'return_pending', 'returned', 'lost', 'cancelled')
- `ck_loan_1`: due_on IS NULL OR issued_on IS NULL OR due_on >= issued_on
- `ck_loan_2`: status IN ('draft', 'reserved') OR issued_on IS NOT NULL

<a id="librarymedia"></a>
### library.media

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `acquired_academic_year_id` | uuid |  |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `title` | varchar(?) | Y |  |  |  |
| 6 | `isbn` | varchar(?) |  |  |  |  |
| 7 | `internal_code` | varchar(?) |  |  |  |  |
| 8 | `edition` | varchar(?) |  |  |  |  |
| 9 | `description` | text |  |  |  |  |
| 10 | `media_type_id` | uuid |  |  | FK → `library.media_type` · RESTRICT · tenant-composite |  |
| 11 | `media_category_id` | uuid |  |  | FK → `library.media_category` · RESTRICT · tenant-composite |  |
| 12 | `cover_image_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 13 | `is_active` | bool | Y | TRUE |  |  |
| 14 | `created_at` | timestamptz | Y | now() |  |  |
| 15 | `created_by` | uuid |  |  |  |  |
| 16 | `updated_at` | timestamptz | Y | now() |  |  |
| 17 | `updated_by` | uuid |  |  |  |  |
| 18 | `deleted_at` | timestamptz |  |  |  |  |
| 19 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `ix_media_1` (`institution_id`, `isbn`) WHERE isbn IS NOT NULL AND deleted_at IS NULL
- unique index `ix_media_2` (`institution_id`, `internal_code`) WHERE internal_code IS NOT NULL AND deleted_at IS NULL

**Relationships**

- ← `library.loan.media_id` — referenced by (1:N)
- ← `library.media_author.media_id` — referenced by (1:N)
- ← `library.media_course.media_id` — referenced by (1:N)
- ← `library.media_publisher.media_id` — referenced by (1:N)
- ← `library.media_subject.media_id` — referenced by (1:N)
- ← `library.media_unit.media_id` — referenced by (1:N)
- ← `library.reservation.media_id` — referenced by (1:N)

<a id="librarymedia_author"></a>
### library.media_author

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `media_id` | uuid | Y |  | FK → `library.media` · CASCADE · tenant-composite |  |
| 4 | `author_id` | uuid | Y |  | FK → `library.author` · RESTRICT · tenant-composite |  |
| 5 | `sort_order` | int4 | Y | 0 |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_media_author_1` (`media_id`, `author_id`) WHERE deleted_at IS NULL

<a id="librarymedia_category"></a>
### library.media_category

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `name` | varchar(?) | Y |  |  |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |
| 11 | `code` | varchar(?) | Y |  |  |  |
| 12 | `sort_order` | int4 | Y | 0 |  |  |
| 13 | `is_active` | bool | Y | TRUE |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_media_category_scope_code` (`institution_id`, `code`) WHERE deleted_at IS NULL
- unique index `uq_media_category_1` (`institution_id`, `name`) WHERE deleted_at IS NULL

**Relationships**

- ← `library.media.media_category_id` — referenced by (1:N)

<a id="librarymedia_course"></a>
### library.media_course

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `media_id` | uuid | Y |  | FK → `library.media` · CASCADE · tenant-composite |  |
| 4 | `course_id` | uuid | Y |  | logical → `academics.course` (cross-module, no FK) |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_media_course_1` (`media_id`, `course_id`) WHERE deleted_at IS NULL

<a id="librarymedia_publisher"></a>
### library.media_publisher

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `media_id` | uuid | Y |  | FK → `library.media` · CASCADE · tenant-composite |  |
| 4 | `publisher_id` | uuid | Y |  | FK → `library.publisher` · RESTRICT · tenant-composite |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_media_publisher_1` (`media_id`, `publisher_id`) WHERE deleted_at IS NULL

<a id="librarymedia_subject"></a>
### library.media_subject

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `media_id` | uuid | Y |  | FK → `library.media` · CASCADE · tenant-composite |  |
| 4 | `subject_id` | uuid | Y |  | logical → `academics.subject` (cross-module, no FK) |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_media_subject_1` (`media_id`, `subject_id`) WHERE deleted_at IS NULL

<a id="librarymedia_type"></a>
### library.media_type

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `code` | varchar(?) | Y |  |  |  |
| 4 | `name` | varchar(?) | Y |  |  |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |
| 11 | `sort_order` | int4 | Y | 0 |  |  |
| 12 | `is_active` | bool | Y | TRUE |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_media_type_scope_code` (`tenant_id`, `code`) WHERE deleted_at IS NULL

**Relationships**

- ← `library.media.media_type_id` — referenced by (1:N)
- ← `library.purchase_request.media_type_id` — referenced by (1:N)

<a id="librarymedia_unit"></a>
### library.media_unit

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `media_id` | uuid | Y |  | FK → `library.media` · CASCADE · tenant-composite |  |
| 4 | `accession_number` | varchar(?) | Y |  |  |  |
| 5 | `barcode` | varchar(?) |  |  |  |  |
| 6 | `status` | varchar(?) | Y | 'available' |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`media_id`, `accession_number`)
- UNIQUE (`id`, `media_id`)
- unique index `ix_media_unit_1` (`tenant_id`, `barcode`) WHERE barcode IS NOT NULL

**Checks**

- `ck_media_unit_status`: status IN ('available', 'issued', 'reserved', 'lost', 'damaged', 'withdrawn')

**Relationships**

- ← `library.loan.media_unit_id` — referenced by (1:1)

<a id="librarynumber_sequence"></a>
### library.number_sequence

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `sequence_code` | varchar(?) | Y |  |  |  |
| 5 | `period_key` | varchar(?) | Y | '' |  |  |
| 6 | `prefix` | varchar(?) |  |  |  |  |
| 7 | `padding` | int2 | Y | 6 |  |  |
| 8 | `next_value` | int8 | Y | 1 |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `institution_id`, `sequence_code`, `period_key`) (NULLS NOT DISTINCT)

<a id="libraryoperation"></a>
### library.operation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `operation_name` | varchar(?) | Y |  |  |  |
| 4 | `resource_type` | varchar(?) |  |  |  |  |
| 5 | `resource_id` | uuid |  |  |  |  |
| 6 | `status` | varchar(?) | Y | 'running' |  |  |
| 7 | `progress_percent` | int2 |  |  |  |  |
| 8 | `error_code` | varchar(?) |  |  |  |  |
| 9 | `error_message` | varchar(?) |  |  |  |  |
| 10 | `result_entity_type` | varchar(?) |  |  |  |  |
| 11 | `result_entity_id` | uuid |  |  |  |  |
| 12 | `started_by_user_id` | uuid |  |  |  |  |
| 13 | `started_at` | timestamptz | Y | now() |  |  |
| 14 | `finished_at` | timestamptz |  |  |  |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_library_operation_status`: status IN ('running', 'succeeded', 'failed', 'cancelled')

<a id="libraryoutbox_event"></a>
### library.outbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | int8 |  |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `aggregate_type` | varchar(?) | Y |  |  |  |
| 5 | `aggregate_id` | uuid | Y |  |  |  |
| 6 | `payload` | text | Y |  |  |  |
| 7 | `occurred_at` | timestamptz | Y | now() |  |  |
| 8 | `published_at` | timestamptz |  |  |  |  |
| 9 | `publish_error` | text |  |  |  | Why the relay set the row aside (not published) |

<a id="librarypublisher"></a>
### library.publisher

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `name` | varchar(?) | Y |  |  |  |
| 4 | `created_at` | timestamptz | Y | now() |  |  |
| 5 | `created_by` | uuid |  |  |  |  |
| 6 | `updated_at` | timestamptz | Y | now() |  |  |
| 7 | `updated_by` | uuid |  |  |  |  |
| 8 | `deleted_at` | timestamptz |  |  |  |  |
| 9 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Relationships**

- ← `library.media_publisher.publisher_id` — referenced by (1:N)

<a id="librarypurchase_request"></a>
### library.purchase_request

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `request_number` | varchar(?) |  |  |  |  |
| 4 | `title` | varchar(?) | Y |  |  |  |
| 5 | `author_text` | varchar(?) | Y |  |  |  |
| 6 | `edition` | varchar(?) |  |  |  |  |
| 7 | `publisher_text` | varchar(?) |  |  |  |  |
| 8 | `media_type_id` | uuid |  |  | FK → `library.media_type` · RESTRICT · tenant-composite |  |
| 9 | `requested_by_person_id` | uuid |  |  | logical → `people.person` (cross-module, no FK) |  |
| 10 | `request_date` | date |  |  |  |  |
| 11 | `status` | varchar(?) | Y | 'draft' |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_purchase_request_status`: status IN ('draft', 'requested', 'accepted', 'rejected')

**Relationships**

- ← `library.purchase_request_course.purchase_request_id` — referenced by (1:N)
- ← `library.purchase_request_subject.purchase_request_id` — referenced by (1:N)

<a id="librarypurchase_request_course"></a>
### library.purchase_request_course

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `purchase_request_id` | uuid | Y |  | FK → `library.purchase_request` · CASCADE · tenant-composite |  |
| 4 | `course_id` | uuid | Y |  | logical → `academics.course` (cross-module, no FK) |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_purchase_request_course_1` (`purchase_request_id`, `course_id`) WHERE deleted_at IS NULL

<a id="librarypurchase_request_subject"></a>
### library.purchase_request_subject

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `purchase_request_id` | uuid | Y |  | FK → `library.purchase_request` · CASCADE · tenant-composite |  |
| 4 | `subject_id` | uuid | Y |  | logical → `academics.subject` (cross-module, no FK) |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_purchase_request_subject_1` (`purchase_request_id`, `subject_id`) WHERE deleted_at IS NULL

<a id="libraryreservation"></a>
### library.reservation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `media_id` | uuid | Y |  | FK → `library.media` · RESTRICT · tenant-composite |  |
| 4 | `reference` | varchar(?) |  |  |  |  |
| 5 | `requested_by_person_id` | uuid |  |  | logical → `people.person` (cross-module, no FK) |  |
| 6 | `requested_by_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 7 | `date_from` | date | Y |  |  |  |
| 8 | `date_to` | date | Y |  |  |  |
| 9 | `status` | varchar(?) | Y | 'requested' |  |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_reservation_open` (`media_id`, `COALESCE(requested_by_person_id, requested_by_user_id)`) WHERE status IN ('requested', 'accepted') AND deleted_at IS NULL

**Checks**

- `ck_reservation_status`: status IN ('requested', 'accepted', 'rejected', 'fulfilled', 'cancelled')
- `ck_reservation_1`: date_to >= date_from

## `transport` — Transport

Transport: Routes, stops and the transport arrangement of each student per year.

7 tables: [`idempotency_key`](#transportidempotency_key), [`inbox_event`](#transportinbox_event), [`operation`](#transportoperation), [`outbox_event`](#transportoutbox_event), [`route`](#transportroute), [`route_stop`](#transportroute_stop), [`student_transport`](#transportstudent_transport)

<a id="transportidempotency_key"></a>
### transport.idempotency_key

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `idempotency_key` | uuid | Y |  |  |  |
| 4 | `user_id` | uuid |  |  |  |  |
| 5 | `operation_name` | varchar(?) | Y |  |  |  |
| 6 | `request_hash` | varchar(?) |  |  |  |  |
| 7 | `response_status` | int2 |  |  |  |  |
| 8 | `response_body` | text |  |  |  |  |
| 9 | `locked_at` | timestamptz |  |  |  |  |
| 10 | `completed_at` | timestamptz |  |  |  |  |
| 11 | `expires_at` | timestamptz | Y |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `idempotency_key`)

<a id="transportinbox_event"></a>
### transport.inbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `event_id` | uuid | Y |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  | PK |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `received_at` | timestamptz | Y | now() |  |  |
| 5 | `processed_at` | timestamptz |  |  |  |  |
| 6 | `processing_error` | text |  |  |  |  |

<a id="transportoperation"></a>
### transport.operation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `operation_name` | varchar(?) | Y |  |  |  |
| 4 | `resource_type` | varchar(?) |  |  |  |  |
| 5 | `resource_id` | uuid |  |  |  |  |
| 6 | `status` | varchar(?) | Y | 'running' |  |  |
| 7 | `progress_percent` | int2 |  |  |  |  |
| 8 | `error_code` | varchar(?) |  |  |  |  |
| 9 | `error_message` | varchar(?) |  |  |  |  |
| 10 | `result_entity_type` | varchar(?) |  |  |  |  |
| 11 | `result_entity_id` | uuid |  |  |  |  |
| 12 | `started_by_user_id` | uuid |  |  |  |  |
| 13 | `started_at` | timestamptz | Y | now() |  |  |
| 14 | `finished_at` | timestamptz |  |  |  |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_transport_operation_status`: status IN ('running', 'succeeded', 'failed', 'cancelled')

<a id="transportoutbox_event"></a>
### transport.outbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | int8 |  |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `aggregate_type` | varchar(?) | Y |  |  |  |
| 5 | `aggregate_id` | uuid | Y |  |  |  |
| 6 | `payload` | text | Y |  |  |  |
| 7 | `occurred_at` | timestamptz | Y | now() |  |  |
| 8 | `published_at` | timestamptz |  |  |  |  |
| 9 | `publish_error` | text |  |  |  | Why the relay set the row aside (not published) |

<a id="transportroute"></a>
### transport.route

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `academic_year_id` | uuid |  |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `name` | varchar(?) | Y |  |  |  |
| 6 | `vehicle_number` | varchar(?) | Y |  |  |  |
| 7 | `start_time` | time | Y |  |  |  |
| 8 | `end_time` | time | Y |  |  |  |
| 9 | `is_active` | bool | Y | TRUE |  |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Relationships**

- ← `transport.route_stop.route_id` — referenced by (1:N)
- ← `transport.student_transport.route_id` — referenced by (1:N)

<a id="transportroute_stop"></a>
### transport.route_stop

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `route_id` | uuid | Y |  | FK → `transport.route` · CASCADE · tenant-composite |  |
| 4 | `sequence` | int4 | Y |  |  |  |
| 5 | `stop_name` | varchar(?) | Y |  |  |  |
| 6 | `pickup_time` | time |  |  |  |  |
| 7 | `drop_time` | time |  |  |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`route_id`, `sequence`) — deferrable
- UNIQUE (`id`, `tenant_id`)

**Relationships**

- ← `transport.student_transport.route_stop_id` — referenced by (1:N)

<a id="transportstudent_transport"></a>
### transport.student_transport

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `student_id` | uuid | Y |  | logical → `sis.student` (cross-module, no FK) |  |
| 4 | `academic_year_id` | uuid | Y |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `transport_mode` | varchar(?) | Y |  |  |  |
| 6 | `route_id` | uuid |  |  | FK → `transport.route` · SET NULL · tenant-composite |  |
| 7 | `route_stop_id` | uuid |  |  | FK → `transport.route_stop` · SET NULL · tenant-composite |  |
| 8 | `bus_pass_issued` | bool | Y | FALSE |  |  |
| 9 | `details` | text |  |  |  |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_student_transport_1` (`student_id`, `academic_year_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_student_transport_transport_mode`: transport_mode IN ('auto', 'car', 'city_bus', 'cycle', 'rickshaw', 'school_bus', 'taxi', 'walking')

## `comms` — Communication & Community

Communication & Community: Message templates, outbound notifications/reminders, internal mailbox, in-app notifications, notices, events, magazines, chat, feedback threads, groups, tasks, class pages and social posts.

46 tables: [`audience_group`](#commsaudience_group), [`audience_group_member`](#commsaudience_group_member), [`calendar_export`](#commscalendar_export), [`class_page`](#commsclass_page), [`class_page_attachment`](#commsclass_page_attachment), [`class_page_member`](#commsclass_page_member), [`content_view`](#commscontent_view), [`conversation`](#commsconversation), [`conversation_message`](#commsconversation_message), [`conversation_participant`](#commsconversation_participant), [`event`](#commsevent), [`event_attendee`](#commsevent_attendee), [`event_gallery_item`](#commsevent_gallery_item), [`event_institution`](#commsevent_institution), [`event_occurrence`](#commsevent_occurrence), [`feedback_message`](#commsfeedback_message), [`feedback_thread`](#commsfeedback_thread), [`idempotency_key`](#commsidempotency_key), [`in_app_notification`](#commsin_app_notification), [`inbox_event`](#commsinbox_event), [`magazine`](#commsmagazine), [`mailbox_message`](#commsmailbox_message), [`message_receipt`](#commsmessage_receipt), [`message_template`](#commsmessage_template), [`notice`](#commsnotice), [`notice_audience`](#commsnotice_audience), [`notice_background`](#commsnotice_background), [`notice_version`](#commsnotice_version), [`notification_channel`](#commsnotification_channel), [`notification_channel_property`](#commsnotification_channel_property), [`notification_cohort_setting`](#commsnotification_cohort_setting), [`notification_preference`](#commsnotification_preference), [`notification_preference_set`](#commsnotification_preference_set), [`notification_rule`](#commsnotification_rule), [`notification_type`](#commsnotification_type), [`notification_type_property`](#commsnotification_type_property), [`operation`](#commsoperation), [`outbound_message`](#commsoutbound_message), [`outbox_event`](#commsoutbox_event), [`page_category`](#commspage_category), [`post`](#commspost), [`post_audience_class`](#commspost_audience_class), [`post_comment`](#commspost_comment), [`reaction`](#commsreaction), [`task`](#commstask), [`task_assignee`](#commstask_assignee)

<a id="commsaudience_group"></a>
### comms.audience_group

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `class_section_id` | uuid |  |  | logical → `academics.class_section` (cross-module, no FK) |  |
| 5 | `name` | varchar(?) | Y |  |  |  |
| 6 | `group_scope` | varchar(?) | Y | 'class' |  |  |
| 7 | `is_active` | bool | Y | TRUE |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_audience_group_group_scope`: group_scope IN ('global', 'class')

**Relationships**

- ← `comms.audience_group_member.group_id` — referenced by (1:N)
- ← `comms.task_assignee.group_id` — referenced by (1:N)

<a id="commsaudience_group_member"></a>
### comms.audience_group_member

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `group_id` | uuid | Y |  | FK → `comms.audience_group` · CASCADE · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `member_type` | varchar(?) | Y | 'student' |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_audience_group_member_1` (`group_id`, `user_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_audience_group_member_member_type`: member_type IN ('student', 'teacher', 'non_teaching')

<a id="commscalendar_export"></a>
### comms.calendar_export

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 4 | `provider` | varchar(?) | Y |  |  |  |
| 5 | `cohort_id` | uuid |  |  | logical → `lms.cohort` (cross-module, no FK) |  |
| 6 | `entity_type` | varchar(?) | Y |  |  |  |
| 7 | `entity_id` | uuid | Y |  |  |  |
| 8 | `property_name` | varchar(?) |  |  |  |  |
| 9 | `property_value` | varchar(?) |  |  |  |  |
| 10 | `is_deleted` | bool | Y | FALSE |  |  |
| 11 | `last_exported_at` | timestamptz |  |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_calendar_export_1` (`user_id`, `provider`, `entity_type`, `entity_id`, `property_name`) WHERE deleted_at IS NULL

**Checks**

- `ck_calendar_export_provider`: provider IN ('google', 'microsoft', 'ics')

<a id="commsclass_page"></a>
### comms.class_page

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `academic_year_id` | uuid |  |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `page_category_id` | uuid | Y |  | FK → `comms.page_category` · RESTRICT · tenant-composite |  |
| 6 | `name` | varchar(?) | Y |  |  |  |
| 7 | `description` | text | Y |  |  |  |
| 8 | `cover_image_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 9 | `owner_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 10 | `is_active` | bool | Y | TRUE |  |  |
| 11 | `created_at` | timestamptz | Y | now() |  |  |
| 12 | `created_by` | uuid |  |  |  |  |
| 13 | `updated_at` | timestamptz | Y | now() |  |  |
| 14 | `updated_by` | uuid |  |  |  |  |
| 15 | `deleted_at` | timestamptz |  |  |  |  |
| 16 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Relationships**

- ← `comms.class_page_attachment.class_page_id` — referenced by (1:N)
- ← `comms.class_page_member.class_page_id` — referenced by (1:N)
- ← `comms.post.class_page_id` — referenced by (1:N)

<a id="commsclass_page_attachment"></a>
### comms.class_page_attachment

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `class_page_id` | uuid | Y |  | FK → `comms.class_page` · CASCADE · tenant-composite |  |
| 4 | `file_id` | uuid | Y |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 5 | `is_active` | bool | Y | TRUE |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

<a id="commsclass_page_member"></a>
### comms.class_page_member

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `class_page_id` | uuid | Y |  | FK → `comms.class_page` · CASCADE · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `is_following` | bool | Y | FALSE |  |  |
| 6 | `reaction` | varchar(?) |  |  |  |  |
| 7 | `is_active` | bool | Y | TRUE |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_class_page_member_1` (`class_page_id`, `user_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_class_page_member_reaction`: reaction IN ('like', 'dislike')

<a id="commscontent_view"></a>
### comms.content_view

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `student_id` | uuid | Y |  | logical → `sis.student` (cross-module, no FK) |  |
| 5 | `viewer_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 6 | `content_type` | varchar(?) | Y |  |  |  |
| 7 | `content_id` | uuid | Y |  |  |  |
| 8 | `viewed_at` | timestamptz |  |  |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_content_view_content_type`: content_type IN ('image', 'video', 'assignment', 'homework')

<a id="commsconversation"></a>
### comms.conversation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `conversation_type` | varchar(?) | Y |  |  |  |
| 5 | `title` | varchar(?) |  |  |  |  |
| 6 | `last_message_at` | timestamptz |  |  |  |  |
| 7 | `is_active` | bool | Y | TRUE |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_conversation_conversation_type`: conversation_type IN ('direct', 'group')

**Relationships**

- ← `comms.conversation_message.conversation_id` — referenced by (1:N)
- ← `comms.conversation_participant.conversation_id` — referenced by (1:N)

<a id="commsconversation_message"></a>
### comms.conversation_message

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `conversation_id` | uuid | Y |  | FK → `comms.conversation` · CASCADE · tenant-composite |  |
| 4 | `sender_user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `body` | text | Y |  |  |  |
| 6 | `deleted_by_sender` | bool | Y | FALSE |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Relationships**

- ← `comms.message_receipt.message_id` — referenced by (1:N)

<a id="commsconversation_participant"></a>
### comms.conversation_participant

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `conversation_id` | uuid | Y |  | FK → `comms.conversation` · CASCADE · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `last_read_at` | timestamptz |  |  |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_conversation_participant_1` (`conversation_id`, `user_id`) WHERE deleted_at IS NULL

<a id="commsevent"></a>
### comms.event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `academic_year_id` | uuid |  |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `audience` | varchar(?) | Y | 'school' |  |  |
| 6 | `class_section_id` | uuid |  |  | logical → `academics.class_section` (cross-module, no FK) |  |
| 7 | `audience_label` | varchar(?) |  |  |  |  |
| 8 | `title` | varchar(?) | Y |  |  |  |
| 9 | `description` | text |  |  |  |  |
| 10 | `category` | varchar(?) | Y | 'other' |  |  |
| 11 | `location` | text |  |  |  |  |
| 12 | `organised_by` | text |  |  |  |  |
| 13 | `image_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 14 | `color` | varchar(?) |  |  |  |  |
| 15 | `start_at` | timestamptz |  |  |  |  |
| 16 | `end_at` | timestamptz |  |  |  |  |
| 17 | `is_all_day` | bool | Y | TRUE |  |  |
| 18 | `repeat_count` | int4 | Y | 0 |  |  |
| 19 | `repeat_interval` | int4 | Y | 0 |  |  |
| 20 | `repeat_unit` | varchar(?) |  |  |  |  |
| 21 | `url` | varchar(?) |  |  |  |  |
| 22 | `status` | varchar(?) | Y | 'inactive' |  |  |
| 23 | `cohort_id` | uuid |  |  | logical → `lms.cohort` (cross-module, no FK) |  |
| 24 | `owner_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 25 | `recurrence_rule` | varchar(?) |  |  |  |  |
| 26 | `for_all_members` | bool | Y | TRUE |  |  |
| 27 | `staff_can_edit` | bool | Y | FALSE |  |  |
| 28 | `importance` | varchar(?) |  |  |  |  |
| 29 | `created_at` | timestamptz | Y | now() |  |  |
| 30 | `created_by` | uuid |  |  |  |  |
| 31 | `updated_at` | timestamptz | Y | now() |  |  |
| 32 | `updated_by` | uuid |  |  |  |  |
| 33 | `deleted_at` | timestamptz |  |  |  |  |
| 34 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_event_audience`: audience IN ('school', 'class', 'alumni', 'tenant', 'cohort', 'personal')
- `ck_event_category`: category IN ('cultural', 'education', 'exam', 'holiday', 'meeting', 'live_class', 'other')
- `ck_event_repeat_unit`: repeat_unit IN ('day', 'week', 'month', 'year')
- `ck_event_status`: status IN ('active', 'inactive')
- `ck_event_importance`: importance IN ('high', 'medium', 'low')
- `ck_event_1`: (audience = 'cohort') = (cohort_id IS NOT NULL)
- `ck_event_2`: (audience = 'personal') = (owner_user_id IS NOT NULL)

**Relationships**

- ← `comms.event_attendee.event_id` — referenced by (1:N)
- ← `comms.event_gallery_item.event_id` — referenced by (1:N)
- ← `comms.event_institution.event_id` — referenced by (1:N)
- ← `comms.event_occurrence.event_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `lms.live_class.calendar_event_id`

<a id="commsevent_attendee"></a>
### comms.event_attendee

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `event_id` | uuid | Y |  | FK → `comms.event` · CASCADE · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_event_attendee_1` (`event_id`, `user_id`) WHERE deleted_at IS NULL

<a id="commsevent_gallery_item"></a>
### comms.event_gallery_item

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `event_id` | uuid | Y |  | FK → `comms.event` · CASCADE · tenant-composite |  |
| 4 | `file_id` | uuid | Y |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 5 | `sort_order` | int4 | Y | 0 |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

<a id="commsevent_institution"></a>
### comms.event_institution

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `event_id` | uuid | Y |  | FK → `comms.event` · CASCADE · tenant-composite |  |
| 4 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_event_institution_1` (`event_id`, `institution_id`) WHERE deleted_at IS NULL

<a id="commsevent_occurrence"></a>
### comms.event_occurrence

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `event_id` | uuid | Y |  | FK → `comms.event` · CASCADE · tenant-composite |  |
| 4 | `user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `cohort_id` | uuid |  |  | logical → `lms.cohort` (cross-module, no FK) |  |
| 6 | `title` | varchar(?) |  |  |  |  |
| 7 | `color` | varchar(?) |  |  |  |  |
| 8 | `description` | text |  |  |  |  |
| 9 | `start_at` | timestamptz | Y |  |  |  |
| 10 | `end_at` | timestamptz |  |  |  |  |
| 11 | `is_cancelled` | bool | Y | FALSE |  |  |
| 12 | `response_status` | varchar(?) |  |  |  |  |
| 13 | `importance` | varchar(?) |  |  |  |  |
| 14 | `created_at` | timestamptz | Y | now() |  |  |
| 15 | `created_by` | uuid |  |  |  |  |
| 16 | `updated_at` | timestamptz | Y | now() |  |  |
| 17 | `updated_by` | uuid |  |  |  |  |
| 18 | `deleted_at` | timestamptz |  |  |  |  |
| 19 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_event_occurrence_response_status`: response_status IN ('needs_action', 'accepted', 'tentative', 'declined', 'completed')
- `ck_event_occurrence_importance`: importance IN ('high', 'medium', 'low')

<a id="commsfeedback_message"></a>
### comms.feedback_message

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `thread_id` | uuid | Y |  | FK → `comms.feedback_thread` · CASCADE · tenant-composite |  |
| 4 | `sender_user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `category` | varchar(?) | Y |  |  |  |
| 6 | `message` | text | Y |  |  |  |
| 7 | `attachment_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 8 | `review_status` | varchar(?) | Y | 'new' |  |  |
| 9 | `deleted_by_sender` | bool | Y | FALSE |  |  |
| 10 | `deleted_by_receiver` | bool | Y | FALSE |  |  |
| 11 | `created_at` | timestamptz | Y | now() |  |  |
| 12 | `created_by` | uuid |  |  |  |  |
| 13 | `updated_at` | timestamptz | Y | now() |  |  |
| 14 | `updated_by` | uuid |  |  |  |  |
| 15 | `deleted_at` | timestamptz |  |  |  |  |
| 16 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_feedback_message_category`: category IN ('app_feedback_or_bug', 'student_profile', 'complaint', 'suggestion', 'other')
- `ck_feedback_message_review_status`: review_status IN ('new', 'seen', 'action_taken')

<a id="commsfeedback_thread"></a>
### comms.feedback_thread

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `guardian_id` | uuid | Y |  | logical → `sis.guardian` (cross-module, no FK) |  |
| 5 | `student_id` | uuid | Y |  | logical → `sis.student` (cross-module, no FK) |  |
| 6 | `assigned_to_user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 7 | `status` | varchar(?) | Y | 'open' |  |  |
| 8 | `created_at` | timestamptz | Y | now() |  |  |
| 9 | `created_by` | uuid |  |  |  |  |
| 10 | `updated_at` | timestamptz | Y | now() |  |  |
| 11 | `updated_by` | uuid |  |  |  |  |
| 12 | `deleted_at` | timestamptz |  |  |  |  |
| 13 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_feedback_thread_status`: status IN ('open', 'closed')

**Relationships**

- ← `comms.feedback_message.thread_id` — referenced by (1:N)

<a id="commsidempotency_key"></a>
### comms.idempotency_key

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `idempotency_key` | uuid | Y |  |  |  |
| 4 | `user_id` | uuid |  |  |  |  |
| 5 | `operation_name` | varchar(?) | Y |  |  |  |
| 6 | `request_hash` | varchar(?) |  |  |  |  |
| 7 | `response_status` | int2 |  |  |  |  |
| 8 | `response_body` | text |  |  |  |  |
| 9 | `locked_at` | timestamptz |  |  |  |  |
| 10 | `completed_at` | timestamptz |  |  |  |  |
| 11 | `expires_at` | timestamptz | Y |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `idempotency_key`)

<a id="commsin_app_notification"></a>
### comms.in_app_notification

*Partitioned by RANGE (created_at); monthly partitions plus a default partition.*

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `recipient_user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 4 | `read_at` | timestamptz |  |  |  |  |
| 5 | `created_at` | timestamptz | Y | now() | PK |  |
| 6 | `notification_type_id` | uuid | Y |  | FK → `comms.notification_type` · RESTRICT |  |
| 7 | `title` | varchar(?) |  |  |  |  |
| 8 | `body` | text |  |  |  |  |
| 9 | `sms_text` | varchar(?) |  |  |  |  |
| 10 | `action_url` | varchar(?) |  |  |  |  |
| 11 | `cohort_id` | uuid |  |  | logical → `lms.cohort` (cross-module, no FK) |  |
| 12 | `email_status` | varchar(?) | Y | 'not_required' |  |  |
| 13 | `sms_status` | varchar(?) | Y | 'not_required' |  |  |
| 14 | `popup_status` | varchar(?) | Y | 'not_required' |  |  |
| 15 | `digest_status` | varchar(?) | Y | 'not_required' |  |  |
| 16 | `archived_at` | timestamptz |  |  |  |  |
| 17 | `entity_type` | varchar(?) |  |  |  |  |
| 18 | `entity_id` | uuid |  |  |  |  |
| 19 | `updated_at` | timestamptz | Y | now() |  |  |

**Checks**

- `ck_in_app_notification_email_status`: email_status IN ('not_required', 'pending', 'sent', 'failed')
- `ck_in_app_notification_sms_status`: sms_status IN ('not_required', 'pending', 'sent', 'failed')
- `ck_in_app_notification_popup_status`: popup_status IN ('not_required', 'pending', 'shown')
- `ck_in_app_notification_digest_status`: digest_status IN ('not_required', 'pending', 'sent')

<a id="commsinbox_event"></a>
### comms.inbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `event_id` | uuid | Y |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  | PK |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `received_at` | timestamptz | Y | now() |  |  |
| 5 | `processed_at` | timestamptz |  |  |  |  |
| 6 | `processing_error` | text |  |  |  |  |

<a id="commsmagazine"></a>
### comms.magazine

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `academic_year_id` | uuid | Y |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `title` | varchar(?) |  |  |  |  |
| 6 | `publication_year` | int4 |  |  |  |  |
| 7 | `cover_image_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 8 | `document_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

<a id="commsmailbox_message"></a>
### comms.mailbox_message

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `academic_year_id` | uuid |  |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `owner_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 6 | `about_student_id` | uuid |  |  | logical → `sis.student` (cross-module, no FK) |  |
| 7 | `folder` | varchar(?) | Y |  |  |  |
| 8 | `from_address` | varchar(?) |  |  |  |  |
| 9 | `from_display` | varchar(?) |  |  |  |  |
| 10 | `to_addresses` | text |  |  |  |  |
| 11 | `subject` | text |  |  |  |  |
| 12 | `body` | text |  |  |  |  |
| 13 | `delivery_status` | varchar(?) |  |  |  |  |
| 14 | `provider_message_id` | varchar(?) |  |  |  |  |
| 15 | `is_processed` | bool | Y | FALSE |  |  |
| 16 | `processed_at` | timestamptz |  |  |  |  |
| 17 | `dispatched_at` | timestamptz |  |  |  |  |
| 18 | `read_at` | timestamptz |  |  |  |  |
| 19 | `created_at` | timestamptz | Y | now() |  |  |
| 20 | `created_by` | uuid |  |  |  |  |
| 21 | `updated_at` | timestamptz | Y | now() |  |  |
| 22 | `updated_by` | uuid |  |  |  |  |
| 23 | `deleted_at` | timestamptz |  |  |  |  |
| 24 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_mailbox_message_folder`: folder IN ('outbox', 'inbox', 'sent')
- `ck_mailbox_message_delivery_status`: delivery_status IN ('queued', 'delivered', 'failed')

<a id="commsmessage_receipt"></a>
### comms.message_receipt

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `message_id` | uuid | Y |  | FK → `comms.conversation_message` · CASCADE · tenant-composite |  |
| 4 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `seen_at` | timestamptz |  |  |  |  |
| 6 | `deleted_at_by_recipient` | timestamptz |  |  |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_message_receipt_1` (`message_id`, `user_id`) WHERE deleted_at IS NULL

<a id="commsmessage_template"></a>
### comms.message_template

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `channel` | varchar(?) | Y |  |  |  |
| 5 | `code` | varchar(?) |  |  |  |  |
| 6 | `name` | varchar(?) | Y |  |  |  |
| 7 | `subject` | text |  |  |  |  |
| 8 | `body` | text |  |  |  |  |
| 9 | `external_template_id` | varchar(?) |  |  |  |  |
| 10 | `is_active` | bool | Y | TRUE |  |  |
| 11 | `created_at` | timestamptz | Y | now() |  |  |
| 12 | `created_by` | uuid |  |  |  |  |
| 13 | `updated_at` | timestamptz | Y | now() |  |  |
| 14 | `updated_by` | uuid |  |  |  |  |
| 15 | `deleted_at` | timestamptz |  |  |  |  |
| 16 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `ix_message_template_1` (`tenant_id`, `channel`, `code`) WHERE code IS NOT NULL AND deleted_at IS NULL

**Checks**

- `ck_message_template_channel`: channel IN ('email', 'sms', 'push', 'in_app')

**Relationships**

- ← `comms.outbound_message.template_id` — referenced by (1:N)

**Referenced from other modules (logical, no FK — archive instead of deleting; publish the deleted/archived event)**

- ← `lms.cohort.confirmation_template_id`
- ← `lms.settings.certificate_template_id`
- ← `lms.settings.cohort_confirmation_template_id`
- ← `lms.settings.mentor_request_template_id`
- ← `lms.settings.mentor_status_template_id`
- ← `lms.settings.payment_reminder_template_id`

<a id="commsnotice"></a>
### comms.notice

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `academic_year_id` | uuid |  |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `audience` | varchar(?) | Y |  |  |  |
| 6 | `class_section_id` | uuid |  |  | logical → `academics.class_section` (cross-module, no FK) |  |
| 7 | `background_id` | uuid |  |  | FK → `comms.notice_background` · SET NULL · tenant-composite |  |
| 8 | `title` | varchar(?) | Y |  |  |  |
| 9 | `body` | text | Y |  |  |  |
| 10 | `attachment_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 11 | `publish_at` | timestamptz |  |  |  |  |
| 12 | `expire_at` | timestamptz |  |  |  |  |
| 13 | `is_published` | bool | Y | FALSE |  |  |
| 14 | `cohort_id` | uuid |  |  | logical → `lms.cohort` (cross-module, no FK) |  |
| 15 | `show_in_calendar` | bool | Y | FALSE |  |  |
| 16 | `allow_replies` | bool | Y | FALSE |  |  |
| 17 | `discussion_topic_id` | uuid |  |  | logical → `lms.forum_topic` (cross-module, no FK) |  |
| 18 | `is_closed` | bool | Y | FALSE |  |  |
| 19 | `is_deactivated` | bool | Y | FALSE |  |  |
| 20 | `author_comment` | text |  |  |  |  |
| 21 | `created_at` | timestamptz | Y | now() |  |  |
| 22 | `created_by` | uuid |  |  |  |  |
| 23 | `updated_at` | timestamptz | Y | now() |  |  |
| 24 | `updated_by` | uuid |  |  |  |  |
| 25 | `deleted_at` | timestamptz |  |  |  |  |
| 26 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_notice_audience`: audience IN ('school', 'class', 'teachers', 'cohort', 'roles')
- `ck_notice_1`: audience <> 'class' OR class_section_id IS NOT NULL
- `ck_notice_2`: (audience = 'cohort') = (cohort_id IS NOT NULL)

**Relationships**

- ← `comms.notice_audience.notice_id` — referenced by (1:N)
- ← `comms.notice_version.notice_id` — referenced by (1:N)

<a id="commsnotice_audience"></a>
### comms.notice_audience

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `notice_id` | uuid | Y |  | FK → `comms.notice` · CASCADE · tenant-composite |  |
| 4 | `institution_id` | uuid |  |  | logical → `org.institution` (cross-module, no FK) |  |
| 5 | `role_id` | uuid |  |  | logical → `iam.role` (cross-module, no FK) |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `ix_notice_audience_1` (`notice_id`, `COALESCE(institution_id, CAST('00000000-0000-0000-0000-000000000000' AS uuid))`, `COALESCE(role_id, CAST('00000000-0000-0000-0000-000000000000' AS uuid))`) WHERE deleted_at IS NULL

<a id="commsnotice_background"></a>
### comms.notice_background

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `usage` | varchar(?) | Y |  |  |  |
| 5 | `image_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_notice_background_usage`: usage IN ('event', 'notice')

**Relationships**

- ← `comms.notice.background_id` — referenced by (1:N)

<a id="commsnotice_version"></a>
### comms.notice_version

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `notice_id` | uuid | Y |  | FK → `comms.notice` · CASCADE · tenant-composite |  |
| 4 | `body` | text |  |  |  |  |
| 5 | `publish_at` | timestamptz |  |  |  |  |
| 6 | `expire_at` | timestamptz |  |  |  |  |
| 7 | `discussion_topic_id` | uuid |  |  | logical → `lms.forum_topic` (cross-module, no FK) |  |
| 8 | `is_closed` | bool | Y | FALSE |  |  |
| 9 | `is_deactivated` | bool | Y | FALSE |  |  |
| 10 | `author_comment` | text |  |  |  |  |
| 11 | `versioned_at` | timestamptz | Y |  |  |  |
| 12 | `original_created_at` | timestamptz |  |  |  |  |
| 13 | `created_at` | timestamptz | Y | now() |  |  |
| 14 | `created_by` | uuid |  |  |  |  |
| 15 | `updated_at` | timestamptz | Y | now() |  |  |
| 16 | `updated_by` | uuid |  |  |  |  |
| 17 | `deleted_at` | timestamptz |  |  |  |  |
| 18 | `row_version` | int4 | Y | 0 |  |  |

<a id="commsnotification_channel"></a>
### comms.notification_channel

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `code` | varchar(?) | Y |  |  |  |
| 4 | `description` | varchar(?) |  |  |  |  |
| 5 | `is_enabled` | bool | Y | TRUE |  |  |
| 6 | `sort_order` | int4 | Y | 0 |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `ix_notification_channel_1` (`COALESCE(tenant_id, CAST('00000000-0000-0000-0000-000000000000' AS uuid))`, `code`) WHERE deleted_at IS NULL

**Relationships**

- ← `comms.notification_channel_property.notification_channel_id` — referenced by (1:N)

<a id="commsnotification_channel_property"></a>
### comms.notification_channel_property

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `notification_channel_id` | uuid | Y |  | FK → `comms.notification_channel` · CASCADE · tenant-composite |  |
| 4 | `property_key` | varchar(?) | Y |  |  |  |
| 5 | `property_value` | text |  |  |  |  |
| 6 | `secret_value_enc` | text |  |  |  |  |
| 7 | `sort_order` | int4 | Y | 0 |  |  |
| 8 | `is_mandatory` | bool | Y | FALSE |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_notification_channel_property_1` (`notification_channel_id`, `property_key`) WHERE deleted_at IS NULL

<a id="commsnotification_cohort_setting"></a>
### comms.notification_cohort_setting

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `cohort_id` | uuid | Y |  | logical → `lms.cohort` (cross-module, no FK) |  |
| 4 | `notification_type_id` | uuid | Y |  | FK → `comms.notification_type` · CASCADE |  |
| 5 | `popup_enabled` | bool | Y | FALSE |  |  |
| 6 | `created_at` | timestamptz | Y | now() |  |  |
| 7 | `created_by` | uuid |  |  |  |  |
| 8 | `updated_at` | timestamptz | Y | now() |  |  |
| 9 | `updated_by` | uuid |  |  |  |  |
| 10 | `deleted_at` | timestamptz |  |  |  |  |
| 11 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_notification_cohort_setting_1` (`cohort_id`, `notification_type_id`) WHERE deleted_at IS NULL

<a id="commsnotification_preference"></a>
### comms.notification_preference

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `preference_set_id` | uuid | Y |  | FK → `comms.notification_preference_set` · CASCADE · tenant-composite |  |
| 4 | `notification_type_id` | uuid |  |  | FK → `comms.notification_type` · CASCADE |  |
| 5 | `property_name` | varchar(?) | Y |  |  |  |
| 6 | `property_value` | varchar(?) |  |  |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `ix_notification_preference_1` (`preference_set_id`, `COALESCE(notification_type_id, CAST('00000000-0000-0000-0000-000000000000' AS uuid))`, `property_name`) WHERE deleted_at IS NULL

<a id="commsnotification_preference_set"></a>
### comms.notification_preference_set

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 4 | `role_id` | uuid |  |  | logical → `iam.role` (cross-module, no FK) |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `ix_notification_preference_set_1` (`user_id`, `COALESCE(role_id, CAST('00000000-0000-0000-0000-000000000000' AS uuid))`) WHERE deleted_at IS NULL

**Relationships**

- ← `comms.notification_preference.preference_set_id` — referenced by (1:N)

<a id="commsnotification_rule"></a>
### comms.notification_rule

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `role_id` | uuid | Y |  | logical → `iam.role` (cross-module, no FK) |  |
| 4 | `notification_type_id` | uuid | Y |  | FK → `comms.notification_type` · CASCADE |  |
| 5 | `permission_id` | uuid |  |  | logical → `iam.permission` (cross-module, no FK) |  |
| 6 | `is_enabled` | bool | Y | TRUE |  |  |
| 7 | `email_subject_template` | text |  |  |  |  |
| 8 | `email_body_template` | text |  |  |  |  |
| 9 | `sms_template` | text |  |  |  |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `ix_notification_rule_1` (`role_id`, `notification_type_id`, `COALESCE(permission_id, CAST('00000000-0000-0000-0000-000000000000' AS uuid))`) WHERE deleted_at IS NULL

<a id="commsnotification_type"></a>
### comms.notification_type

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `code` | varchar(?) | Y |  | UQ |  |
| 3 | `description` | text | Y |  |  |  |
| 4 | `module_code` | varchar(?) | Y |  |  |  |
| 5 | `role_code` | varchar(?) |  |  |  |  |
| 6 | `is_enabled` | bool | Y | TRUE |  |  |
| 7 | `popup_enabled` | bool | Y | FALSE |  |  |
| 8 | `teacher_popup_enabled` | bool | Y | FALSE |  |  |
| 9 | `valid_parameters` | varchar(?) |  |  |  |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |

**Relationships**

- ← `comms.in_app_notification.notification_type_id` — referenced by (1:N)
- ← `comms.notification_cohort_setting.notification_type_id` — referenced by (1:N)
- ← `comms.notification_preference.notification_type_id` — referenced by (1:N)
- ← `comms.notification_rule.notification_type_id` — referenced by (1:N)
- ← `comms.notification_type_property.notification_type_id` — referenced by (1:N)

<a id="commsnotification_type_property"></a>
### comms.notification_type_property

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `notification_type_id` | uuid | Y |  | FK → `comms.notification_type` · CASCADE |  |
| 3 | `module_code` | varchar(?) |  |  |  |  |
| 4 | `property_value` | varchar(?) | Y |  |  |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

<a id="commsoperation"></a>
### comms.operation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `operation_name` | varchar(?) | Y |  |  |  |
| 4 | `resource_type` | varchar(?) |  |  |  |  |
| 5 | `resource_id` | uuid |  |  |  |  |
| 6 | `status` | varchar(?) | Y | 'running' |  |  |
| 7 | `progress_percent` | int2 |  |  |  |  |
| 8 | `error_code` | varchar(?) |  |  |  |  |
| 9 | `error_message` | varchar(?) |  |  |  |  |
| 10 | `result_entity_type` | varchar(?) |  |  |  |  |
| 11 | `result_entity_id` | uuid |  |  |  |  |
| 12 | `started_by_user_id` | uuid |  |  |  |  |
| 13 | `started_at` | timestamptz | Y | now() |  |  |
| 14 | `finished_at` | timestamptz |  |  |  |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_comms_operation_status`: status IN ('running', 'succeeded', 'failed', 'cancelled')

<a id="commsoutbound_message"></a>
### comms.outbound_message

*Partitioned by RANGE (created_at); monthly partitions plus a default partition.*

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `academic_year_id` | uuid |  |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `message_kind` | varchar(?) | Y |  |  |  |
| 6 | `channel` | varchar(?) | Y |  |  |  |
| 7 | `sender` | varchar(?) | Y |  |  |  |
| 8 | `recipient` | varchar(?) | Y |  |  |  |
| 9 | `recipient_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 10 | `subject` | text |  |  |  |  |
| 11 | `body` | text | Y |  |  |  |
| 12 | `template_id` | uuid |  |  | FK → `comms.message_template` · SET NULL · tenant-composite |  |
| 13 | `related_entity_type` | varchar(?) |  |  |  |  |
| 14 | `related_entity_id` | uuid |  |  |  |  |
| 15 | `status` | varchar(?) | Y | 'queued' |  |  |
| 16 | `provider_response` | text |  |  |  |  |
| 17 | `scheduled_at` | timestamptz |  |  |  |  |
| 18 | `sent_at` | timestamptz |  |  |  |  |
| 19 | `created_at` | timestamptz | Y | now() | PK |  |
| 20 | `created_by` | uuid |  |  |  |  |
| 21 | `updated_at` | timestamptz | Y | now() |  |  |
| 22 | `updated_by` | uuid |  |  |  |  |
| 23 | `deleted_at` | timestamptz |  |  |  |  |
| 24 | `row_version` | int4 | Y | 0 |  |  |
| 25 | `template_params` | varchar(?)[] |  |  |  |  |
| 26 | `action_url` | varchar(?) |  |  |  |  |

**Checks**

- `ck_outbound_message_message_kind`: message_kind IN ('notification', 'reminder', 'broadcast')
- `ck_outbound_message_channel`: channel IN ('sms', 'email', 'push', 'web_push')
- `ck_outbound_message_status`: status IN ('queued', 'processing', 'delivered', 'failed', 'cancelled')

<a id="commsoutbox_event"></a>
### comms.outbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | int8 |  |  | PK |  |
| 2 | `tenant_id` | uuid |  |  |  |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `aggregate_type` | varchar(?) | Y |  |  |  |
| 5 | `aggregate_id` | uuid | Y |  |  |  |
| 6 | `payload` | text | Y |  |  |  |
| 7 | `occurred_at` | timestamptz | Y | now() |  |  |
| 8 | `published_at` | timestamptz |  |  |  |  |
| 9 | `publish_error` | text |  |  |  | Why the relay set the row aside (not published) |

<a id="commspage_category"></a>
### comms.page_category

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `academic_year_id` | uuid |  |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `name` | varchar(?) | Y |  |  |  |
| 6 | `is_active` | bool | Y | TRUE |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |
| 13 | `code` | varchar(?) | Y |  |  |  |
| 14 | `sort_order` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_page_category_scope_code` (`institution_id`, `code`) WHERE deleted_at IS NULL

**Relationships**

- ← `comms.class_page.page_category_id` — referenced by (1:N)

<a id="commspost"></a>
### comms.post

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `academic_year_id` | uuid | Y |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `class_page_id` | uuid |  |  | FK → `comms.class_page` · RESTRICT · tenant-composite |  |
| 6 | `author_user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 7 | `body` | text | Y |  |  |  |
| 8 | `visibility` | varchar(?) | Y | 'all_classes' |  |  |
| 9 | `status` | varchar(?) | Y | 'draft' |  |  |
| 10 | `scheduled_for` | timestamptz |  |  |  |  |
| 11 | `posted_at` | timestamptz |  |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_post_visibility`: visibility IN ('all_classes', 'selected_classes', 'page_members')
- `ck_post_status`: status IN ('draft', 'pending', 'posted', 'cancelled')

**Relationships**

- ← `comms.post_audience_class.post_id` — referenced by (1:N)
- ← `comms.post_comment.post_id` — referenced by (1:N)
- ← `comms.reaction.post_id` — referenced by (1:N)

<a id="commspost_audience_class"></a>
### comms.post_audience_class

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `post_id` | uuid | Y |  | FK → `comms.post` · CASCADE · tenant-composite |  |
| 4 | `class_section_id` | uuid | Y |  | logical → `academics.class_section` (cross-module, no FK) |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_post_audience_class_1` (`post_id`, `class_section_id`) WHERE deleted_at IS NULL

<a id="commspost_comment"></a>
### comms.post_comment

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `post_id` | uuid | Y |  | FK → `comms.post` · CASCADE · tenant-composite |  |
| 4 | `parent_comment_id` | uuid |  |  | FK → `comms.post_comment` · CASCADE · tenant-composite |  |
| 5 | `author_user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 6 | `body` | text |  |  |  |  |
| 7 | `attachment_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 8 | `is_active` | bool | Y | TRUE |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Relationships**

- ← `comms.post_comment.parent_comment_id` — referenced by (1:N)
- ← `comms.reaction.comment_id` — referenced by (1:N)

<a id="commsreaction"></a>
### comms.reaction

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `post_id` | uuid |  |  | FK → `comms.post` · CASCADE · tenant-composite |  |
| 4 | `comment_id` | uuid |  |  | FK → `comms.post_comment` · CASCADE · tenant-composite |  |
| 5 | `user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 6 | `reaction` | varchar(?) |  |  |  |  |
| 7 | `is_saved` | bool | Y | FALSE |  |  |
| 8 | `is_active` | bool | Y | TRUE |  |  |
| 9 | `created_at` | timestamptz | Y | now() |  |  |
| 10 | `created_by` | uuid |  |  |  |  |
| 11 | `updated_at` | timestamptz | Y | now() |  |  |
| 12 | `updated_by` | uuid |  |  |  |  |
| 13 | `deleted_at` | timestamptz |  |  |  |  |
| 14 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_reaction_post` (`post_id`, `user_id`) WHERE post_id IS NOT NULL AND deleted_at IS NULL
- unique index `uq_reaction_comment` (`comment_id`, `user_id`) WHERE comment_id IS NOT NULL AND deleted_at IS NULL

**Checks**

- `ck_reaction_target`: num_nonnulls(post_id, comment_id) = 1
- `ck_reaction_saved_on_post`: comment_id IS NULL OR is_saved = FALSE

<a id="commstask"></a>
### comms.task

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `academic_year_id` | uuid |  |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `owner_user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 6 | `title` | varchar(?) | Y |  |  |  |
| 7 | `task_category` | varchar(?) | Y |  |  |  |
| 8 | `due_at` | timestamptz | Y |  |  |  |
| 9 | `reminder_option` | varchar(?) | Y |  |  |  |
| 10 | `reminder_at` | timestamptz |  |  |  |  |
| 11 | `checklist` | text | Y |  |  |  |
| 12 | `is_completed` | bool | Y | FALSE |  |  |
| 13 | `is_snoozed` | bool | Y | FALSE |  |  |
| 14 | `priority` | varchar(?) | Y | 'normal' |  |  |
| 15 | `assignment_mode` | varchar(?) | Y | 'individual' |  |  |
| 16 | `created_at` | timestamptz | Y | now() |  |  |
| 17 | `created_by` | uuid |  |  |  |  |
| 18 | `updated_at` | timestamptz | Y | now() |  |  |
| 19 | `updated_by` | uuid |  |  |  |  |
| 20 | `deleted_at` | timestamptz |  |  |  |  |
| 21 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_task_task_category`: task_category IN ('self', 'student', 'class', 'group', 'teacher', 'non_teaching')
- `ck_task_reminder_option`: reminder_option IN ('one_hour_before', 'one_day_before', 'two_days_before', 'custom')
- `ck_task_priority`: priority IN ('low', 'normal', 'high')
- `ck_task_assignment_mode`: assignment_mode IN ('individual', 'group_task', 'open')

**Relationships**

- ← `comms.task_assignee.task_id` — referenced by (1:N)

<a id="commstask_assignee"></a>
### comms.task_assignee

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `task_id` | uuid | Y |  | FK → `comms.task` · CASCADE · tenant-composite |  |
| 4 | `assignee_type` | varchar(?) | Y | 'user' |  |  |
| 5 | `user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 6 | `class_section_id` | uuid |  |  | logical → `academics.class_section` (cross-module, no FK) |  |
| 7 | `group_id` | uuid |  |  | FK → `comms.audience_group` · RESTRICT · tenant-composite |  |
| 8 | `claimed_by_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 9 | `status` | varchar(?) | Y | 'pending' |  |  |
| 10 | `created_at` | timestamptz | Y | now() |  |  |
| 11 | `created_by` | uuid |  |  |  |  |
| 12 | `updated_at` | timestamptz | Y | now() |  |  |
| 13 | `updated_by` | uuid |  |  |  |  |
| 14 | `deleted_at` | timestamptz |  |  |  |  |
| 15 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_task_assignee_assignee_type`: assignee_type IN ('user', 'class', 'group')
- `ck_task_assignee_status`: status IN ('pending', 'completed')
- `ck_task_assignee_1`: num_nonnulls(user_id, class_section_id, group_id) = 1

<a id="commsstaff_record"></a>
### comms.staff_record

The login account and institution of each staff record, kept from `hr.staff` events, so mail and conversations reach only accounts of the sender's school.

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `tenant_id` | uuid | Y |  | PK | Owning tenant. Row-level security key. |
| 2 | `staff_id` | uuid | Y |  | PK | `hr.staff.id` (kept from its events) |
| 3 | `user_id` | uuid |  |  |  | Login account of the record |
| 4 | `institution_id` | uuid |  |  |  | Institution of the record |
| 5 | `is_deleted` | bool | Y | false |  | Deleted in its module; kept so a late event cannot restore it |
| 6 | `source_version` | int4 | Y |  |  | Source row version this entry reflects; older events are ignored |
| 7 | `synced_at` | timestamptz | Y |  |  | When an event last changed the row |

**Checks**

- `ck_comms_staff_record_source_version`: source_version >= 1

<a id="commsguardian_link"></a>
### comms.guardian_link

Which guardian record, person and account is linked to which student, kept from `sis.student_guardian` events, so a guardian reaches only the feedback threads opened as them.

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `tenant_id` | uuid | Y |  | PK | Owning tenant. Row-level security key. |
| 2 | `link_id` | uuid | Y |  | PK | `sis.student_guardian.id` (kept from its events) |
| 3 | `student_id` | uuid | Y |  |  | The child (`sis.student.id`) |
| 4 | `guardian_id` | uuid | Y |  |  | `sis.guardian.id`, as feedback threads name it |
| 5 | `guardian_person_id` | uuid | Y |  |  | Person of the guardian |
| 6 | `guardian_user_id` | uuid |  |  |  | Login account of the guardian when last announced |
| 7 | `status` | varchar(20) | Y |  |  | State of the link; only active counts |
| 8 | `is_deleted` | bool | Y | false |  | Deleted in its module; kept so a late event cannot restore it |
| 9 | `source_version` | int4 | Y |  |  | Source row version this entry reflects; older events are ignored |
| 10 | `synced_at` | timestamptz | Y |  |  | When an event last changed the row |

**Checks**

- `ck_comms_guardian_link_source_version`: source_version >= 1

## `frontoffice` — Front Office

Front Office: Visitor log, call log, postal register and the telephone directory.

8 tables: [`call_log`](#frontofficecall_log), [`directory_contact`](#frontofficedirectory_contact), [`idempotency_key`](#frontofficeidempotency_key), [`inbox_event`](#frontofficeinbox_event), [`operation`](#frontofficeoperation), [`outbox_event`](#frontofficeoutbox_event), [`postal_record`](#frontofficepostal_record), [`visitor_log`](#frontofficevisitor_log)

<a id="frontofficecall_log"></a>
### frontoffice.call_log

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `academic_year_id` | uuid |  |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `direction` | varchar(?) | Y |  |  |  |
| 6 | `purpose` | varchar(?) |  |  |  |  |
| 7 | `caller_name` | varchar(?) | Y |  |  |  |
| 8 | `incoming_number` | varchar(?) |  |  |  |  |
| 9 | `outgoing_number` | varchar(?) |  |  |  |  |
| 10 | `call_date` | date | Y |  |  |  |
| 11 | `start_time` | time |  |  |  |  |
| 12 | `end_time` | time |  |  |  |  |
| 13 | `duration_seconds` | int4 |  |  |  |  |
| 14 | `description` | text |  |  |  |  |
| 15 | `logged_by_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 16 | `created_at` | timestamptz | Y | now() |  |  |
| 17 | `created_by` | uuid |  |  |  |  |
| 18 | `updated_at` | timestamptz | Y | now() |  |  |
| 19 | `updated_by` | uuid |  |  |  |  |
| 20 | `deleted_at` | timestamptz |  |  |  |  |
| 21 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_call_log_direction`: direction IN ('incoming', 'outgoing')

<a id="frontofficedirectory_contact"></a>
### frontoffice.directory_contact

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `name` | varchar(?) | Y |  |  |  |
| 5 | `designation` | varchar(?) | Y |  |  |  |
| 6 | `phone` | varchar(?) | Y |  |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

<a id="frontofficeidempotency_key"></a>
### frontoffice.idempotency_key

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `idempotency_key` | uuid | Y |  |  |  |
| 4 | `user_id` | uuid |  |  |  |  |
| 5 | `operation_name` | varchar(?) | Y |  |  |  |
| 6 | `request_hash` | varchar(?) |  |  |  |  |
| 7 | `response_status` | int2 |  |  |  |  |
| 8 | `response_body` | text |  |  |  |  |
| 9 | `locked_at` | timestamptz |  |  |  |  |
| 10 | `completed_at` | timestamptz |  |  |  |  |
| 11 | `expires_at` | timestamptz | Y |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `idempotency_key`)

<a id="frontofficeinbox_event"></a>
### frontoffice.inbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `event_id` | uuid | Y |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  | PK |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `received_at` | timestamptz | Y | now() |  |  |
| 5 | `processed_at` | timestamptz |  |  |  |  |
| 6 | `processing_error` | text |  |  |  |  |

<a id="frontofficeoperation"></a>
### frontoffice.operation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `operation_name` | varchar(?) | Y |  |  |  |
| 4 | `resource_type` | varchar(?) |  |  |  |  |
| 5 | `resource_id` | uuid |  |  |  |  |
| 6 | `status` | varchar(?) | Y | 'running' |  |  |
| 7 | `progress_percent` | int2 |  |  |  |  |
| 8 | `error_code` | varchar(?) |  |  |  |  |
| 9 | `error_message` | varchar(?) |  |  |  |  |
| 10 | `result_entity_type` | varchar(?) |  |  |  |  |
| 11 | `result_entity_id` | uuid |  |  |  |  |
| 12 | `started_by_user_id` | uuid |  |  |  |  |
| 13 | `started_at` | timestamptz | Y | now() |  |  |
| 14 | `finished_at` | timestamptz |  |  |  |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_frontoffice_operation_status`: status IN ('running', 'succeeded', 'failed', 'cancelled')

<a id="frontofficeoutbox_event"></a>
### frontoffice.outbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | int8 |  |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `aggregate_type` | varchar(?) | Y |  |  |  |
| 5 | `aggregate_id` | uuid | Y |  |  |  |
| 6 | `payload` | text | Y |  |  |  |
| 7 | `occurred_at` | timestamptz | Y | now() |  |  |
| 8 | `published_at` | timestamptz |  |  |  |  |
| 9 | `publish_error` | text |  |  |  | Why the relay set the row aside (not published) |

<a id="frontofficepostal_record"></a>
### frontoffice.postal_record

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `academic_year_id` | uuid |  |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `direction` | varchar(?) | Y |  |  |  |
| 6 | `post_type` | varchar(?) | Y |  |  |  |
| 7 | `reference_number` | varchar(?) |  |  |  |  |
| 8 | `is_confidential` | bool | Y | FALSE |  |  |
| 9 | `sender_name` | varchar(?) |  |  |  |  |
| 10 | `sender_address` | text |  |  |  |  |
| 11 | `receiver_name` | varchar(?) |  |  |  |  |
| 12 | `receiver_address` | text |  |  |  |  |
| 13 | `postal_date` | date | Y |  |  |  |
| 14 | `description` | text | Y |  |  |  |
| 15 | `attachment_file_id` | uuid |  |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 16 | `logged_by_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 17 | `created_at` | timestamptz | Y | now() |  |  |
| 18 | `created_by` | uuid |  |  |  |  |
| 19 | `updated_at` | timestamptz | Y | now() |  |  |
| 20 | `updated_by` | uuid |  |  |  |  |
| 21 | `deleted_at` | timestamptz |  |  |  |  |
| 22 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_postal_record_direction`: direction IN ('dispatch', 'receive')

<a id="frontofficevisitor_log"></a>
### frontoffice.visitor_log

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `institution_id` | uuid | Y |  | logical → `org.institution` (cross-module, no FK) |  |
| 4 | `academic_year_id` | uuid |  |  | logical → `org.academic_year` (cross-module, no FK) |  |
| 5 | `visitor_name` | varchar(?) | Y |  |  |  |
| 6 | `visitor_type` | varchar(?) | Y |  |  |  |
| 7 | `email` | varchar(?) |  |  |  |  |
| 8 | `phone` | varchar(?) |  |  |  |  |
| 9 | `company_name` | varchar(?) |  |  |  |  |
| 10 | `address` | text |  |  |  |  |
| 11 | `student_id` | uuid |  |  | logical → `sis.student` (cross-module, no FK) |  |
| 12 | `relation_to_student` | varchar(?) |  |  |  |  |
| 13 | `related_person_name` | varchar(?) |  |  |  |  |
| 14 | `visitor_count` | int4 | Y | 1 |  |  |
| 15 | `purpose` | varchar(?) | Y |  |  |  |
| 16 | `host_staff_id` | uuid |  |  | logical → `hr.staff` (cross-module, no FK) |  |
| 17 | `visit_date` | date | Y |  |  |  |
| 18 | `entry_time` | time |  |  |  |  |
| 19 | `exit_time` | time |  |  |  |  |
| 20 | `remarks` | text |  |  |  |  |
| 21 | `created_at` | timestamptz | Y | now() |  |  |
| 22 | `created_by` | uuid |  |  |  |  |
| 23 | `updated_at` | timestamptz | Y | now() |  |  |
| 24 | `updated_by` | uuid |  |  |  |  |
| 25 | `deleted_at` | timestamptz |  |  |  |  |
| 26 | `row_version` | int4 | Y | 0 |  |  |

## `careers` — Careers

Careers: Job board, applications and candidate career preferences for students and alumni.

11 tables: [`candidate_preference`](#careerscandidate_preference), [`candidate_preferred_function`](#careerscandidate_preferred_function), [`candidate_preferred_industry`](#careerscandidate_preferred_industry), [`function_area`](#careersfunction_area), [`idempotency_key`](#careersidempotency_key), [`inbox_event`](#careersinbox_event), [`industry`](#careersindustry), [`job_application`](#careersjob_application), [`job_opening`](#careersjob_opening), [`operation`](#careersoperation), [`outbox_event`](#careersoutbox_event)

<a id="careerscandidate_preference"></a>
### careers.candidate_preference

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `person_id` | uuid | Y |  | logical → `people.person` (cross-module, no FK) · UQ |  |
| 4 | `open_to` | varchar(?) |  |  |  |  |
| 5 | `preferred_location` | varchar(?) |  |  |  |  |
| 6 | `dream_companies` | text |  |  |  |  |
| 7 | `attire` | varchar(?) |  |  |  |  |
| 8 | `collaboration` | varchar(?) |  |  |  |  |
| 9 | `role_clarity` | varchar(?) |  |  |  |  |
| 10 | `location_preference` | varchar(?) |  |  |  |  |
| 11 | `work_hours` | varchar(?) |  |  |  |  |
| 12 | `company_type` | varchar(?) |  |  |  |  |
| 13 | `created_at` | timestamptz | Y | now() |  |  |
| 14 | `created_by` | uuid |  |  |  |  |
| 15 | `updated_at` | timestamptz | Y | now() |  |  |
| 16 | `updated_by` | uuid |  |  |  |  |
| 17 | `deleted_at` | timestamptz |  |  |  |  |
| 18 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_candidate_preference_person_id` (`person_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_candidate_preference_open_to`: open_to IN ('work', 'hiring')
- `ck_candidate_preference_attire`: attire IN ('casual', 'formal')
- `ck_candidate_preference_collaboration`: collaboration IN ('individual', 'team', 'both')
- `ck_candidate_preference_role_clarity`: role_clarity IN ('clearly_defined', 'unstructured')
- `ck_candidate_preference_location_preference`: location_preference IN ('travel', 'near_home')
- `ck_candidate_preference_work_hours`: work_hours IN ('flexible', 'fixed')
- `ck_candidate_preference_company_type`: company_type IN ('corporate', 'startup')

**Relationships**

- ← `careers.candidate_preferred_function.candidate_preference_id` — referenced by (1:N)
- ← `careers.candidate_preferred_industry.candidate_preference_id` — referenced by (1:N)

<a id="careerscandidate_preferred_function"></a>
### careers.candidate_preferred_function

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `candidate_preference_id` | uuid | Y |  | FK → `careers.candidate_preference` · CASCADE · tenant-composite |  |
| 4 | `function_area_id` | uuid | Y |  | FK → `careers.function_area` · RESTRICT · tenant-composite |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_candidate_preferred_function_1` (`candidate_preference_id`, `function_area_id`) WHERE deleted_at IS NULL

<a id="careerscandidate_preferred_industry"></a>
### careers.candidate_preferred_industry

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `candidate_preference_id` | uuid | Y |  | FK → `careers.candidate_preference` · CASCADE · tenant-composite |  |
| 4 | `industry_id` | uuid | Y |  | FK → `careers.industry` · RESTRICT · tenant-composite |  |
| 5 | `created_at` | timestamptz | Y | now() |  |  |
| 6 | `created_by` | uuid |  |  |  |  |
| 7 | `updated_at` | timestamptz | Y | now() |  |  |
| 8 | `updated_by` | uuid |  |  |  |  |
| 9 | `deleted_at` | timestamptz |  |  |  |  |
| 10 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_candidate_preferred_industry_1` (`candidate_preference_id`, `industry_id`) WHERE deleted_at IS NULL

<a id="careersfunction_area"></a>
### careers.function_area

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `name` | varchar(?) | Y |  |  |  |
| 4 | `created_at` | timestamptz | Y | now() |  |  |
| 5 | `created_by` | uuid |  |  |  |  |
| 6 | `updated_at` | timestamptz | Y | now() |  |  |
| 7 | `updated_by` | uuid |  |  |  |  |
| 8 | `deleted_at` | timestamptz |  |  |  |  |
| 9 | `row_version` | int4 | Y | 0 |  |  |
| 10 | `code` | varchar(?) | Y |  |  |  |
| 11 | `sort_order` | int4 | Y | 0 |  |  |
| 12 | `is_active` | bool | Y | TRUE |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_function_area_scope_code` (`tenant_id`, `code`) WHERE deleted_at IS NULL
- unique index `uq_function_area_1` (`tenant_id`, `name`) WHERE deleted_at IS NULL

**Relationships**

- ← `careers.candidate_preferred_function.function_area_id` — referenced by (1:N)

<a id="careersidempotency_key"></a>
### careers.idempotency_key

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `idempotency_key` | uuid | Y |  |  |  |
| 4 | `user_id` | uuid |  |  |  |  |
| 5 | `operation_name` | varchar(?) | Y |  |  |  |
| 6 | `request_hash` | varchar(?) |  |  |  |  |
| 7 | `response_status` | int2 |  |  |  |  |
| 8 | `response_body` | text |  |  |  |  |
| 9 | `locked_at` | timestamptz |  |  |  |  |
| 10 | `completed_at` | timestamptz |  |  |  |  |
| 11 | `expires_at` | timestamptz | Y |  |  |  |
| 12 | `created_at` | timestamptz | Y | now() |  |  |
| 13 | `created_by` | uuid |  |  |  |  |
| 14 | `updated_at` | timestamptz | Y | now() |  |  |
| 15 | `updated_by` | uuid |  |  |  |  |
| 16 | `deleted_at` | timestamptz |  |  |  |  |
| 17 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`tenant_id`, `idempotency_key`)

<a id="careersinbox_event"></a>
### careers.inbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `event_id` | uuid | Y |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  | PK |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `received_at` | timestamptz | Y | now() |  |  |
| 5 | `processed_at` | timestamptz |  |  |  |  |
| 6 | `processing_error` | text |  |  |  |  |

<a id="careersindustry"></a>
### careers.industry

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `name` | varchar(?) | Y |  |  |  |
| 4 | `created_at` | timestamptz | Y | now() |  |  |
| 5 | `created_by` | uuid |  |  |  |  |
| 6 | `updated_at` | timestamptz | Y | now() |  |  |
| 7 | `updated_by` | uuid |  |  |  |  |
| 8 | `deleted_at` | timestamptz |  |  |  |  |
| 9 | `row_version` | int4 | Y | 0 |  |  |
| 10 | `code` | varchar(?) | Y |  |  |  |
| 11 | `sort_order` | int4 | Y | 0 |  |  |
| 12 | `is_active` | bool | Y | TRUE |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)
- unique index `uq_industry_scope_code` (`tenant_id`, `code`) WHERE deleted_at IS NULL
- unique index `uq_industry_1` (`tenant_id`, `name`) WHERE deleted_at IS NULL

**Relationships**

- ← `careers.candidate_preferred_industry.industry_id` — referenced by (1:N)

<a id="careersjob_application"></a>
### careers.job_application

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `job_opening_id` | uuid | Y |  | FK → `careers.job_opening` · CASCADE · tenant-composite |  |
| 4 | `applicant_user_id` | uuid | Y |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 5 | `resume_file_id` | uuid | Y |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 6 | `status` | varchar(?) | Y | 'submitted' |  |  |
| 7 | `created_at` | timestamptz | Y | now() |  |  |
| 8 | `created_by` | uuid |  |  |  |  |
| 9 | `updated_at` | timestamptz | Y | now() |  |  |
| 10 | `updated_by` | uuid |  |  |  |  |
| 11 | `deleted_at` | timestamptz |  |  |  |  |
| 12 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- unique index `uq_job_application_1` (`job_opening_id`, `applicant_user_id`) WHERE deleted_at IS NULL

**Checks**

- `ck_job_application_status`: status IN ('submitted', 'shortlisted', 'rejected', 'hired', 'withdrawn')

<a id="careersjob_opening"></a>
### careers.job_opening

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `posted_by_user_id` | uuid |  |  | logical → `iam.user_account` (cross-module, no FK) |  |
| 4 | `title` | varchar(?) | Y |  |  |  |
| 5 | `location` | varchar(?) | Y |  |  |  |
| 6 | `country_id` | uuid | Y |  | logical → `master.country` (cross-module, no FK) |  |
| 7 | `employment_type` | varchar(?) | Y |  |  |  |
| 8 | `work_mode` | varchar(?) |  |  |  |  |
| 9 | `status` | varchar(?) | Y | 'open' |  |  |
| 10 | `description` | text | Y |  |  |  |
| 11 | `company_name` | varchar(?) | Y |  |  |  |
| 12 | `company_website` | varchar(?) | Y |  |  |  |
| 13 | `company_email` | varchar(?) | Y |  |  |  |
| 14 | `company_logo_file_id` | uuid | Y |  | logical → `platform.file_object` (cross-module, no FK) |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |

**Uniqueness**

- UNIQUE (`id`, `tenant_id`)

**Checks**

- `ck_job_opening_employment_type`: employment_type IN ('full_time', 'part_time', 'freelance', 'contract')
- `ck_job_opening_work_mode`: work_mode IN ('remote', 'hybrid', 'on_site')
- `ck_job_opening_status`: status IN ('open', 'closed')

**Relationships**

- ← `careers.job_application.job_opening_id` — referenced by (1:N)

<a id="careersoperation"></a>
### careers.operation

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | uuid | Y | gen_random_uuid() | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `operation_name` | varchar(?) | Y |  |  |  |
| 4 | `resource_type` | varchar(?) |  |  |  |  |
| 5 | `resource_id` | uuid |  |  |  |  |
| 6 | `status` | varchar(?) | Y | 'running' |  |  |
| 7 | `progress_percent` | int2 |  |  |  |  |
| 8 | `error_code` | varchar(?) |  |  |  |  |
| 9 | `error_message` | varchar(?) |  |  |  |  |
| 10 | `result_entity_type` | varchar(?) |  |  |  |  |
| 11 | `result_entity_id` | uuid |  |  |  |  |
| 12 | `started_by_user_id` | uuid |  |  |  |  |
| 13 | `started_at` | timestamptz | Y | now() |  |  |
| 14 | `finished_at` | timestamptz |  |  |  |  |
| 15 | `created_at` | timestamptz | Y | now() |  |  |
| 16 | `created_by` | uuid |  |  |  |  |
| 17 | `updated_at` | timestamptz | Y | now() |  |  |
| 18 | `updated_by` | uuid |  |  |  |  |
| 19 | `deleted_at` | timestamptz |  |  |  |  |
| 20 | `row_version` | int4 | Y | 0 |  |  |

**Checks**

- `ck_careers_operation_status`: status IN ('running', 'succeeded', 'failed', 'cancelled')

<a id="careersoutbox_event"></a>
### careers.outbox_event

| # | Column | Type | NN | Default | Key / relationship | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | int8 |  |  | PK |  |
| 2 | `tenant_id` | uuid | Y |  |  |  |
| 3 | `event_type` | varchar(?) | Y |  |  |  |
| 4 | `aggregate_type` | varchar(?) | Y |  |  |  |
| 5 | `aggregate_id` | uuid | Y |  |  |  |
| 6 | `payload` | text | Y |  |  |  |
| 7 | `occurred_at` | timestamptz | Y | now() |  |  |
| 8 | `published_at` | timestamptz |  |  |  |  |
| 9 | `publish_error` | text |  |  |  | Why the relay set the row aside (not published) |
