-- MySMS schema smoke test. Run with psql -v ON_ERROR_STOP=1 against a freshly-built database.
-- Positive paths must succeed; each negative test must raise, caught by a DO block.
\set QUIET on
SET client_min_messages = warning;

BEGIN;
-- ---- seeds ----
INSERT INTO master.currency (id, code, name) VALUES ('00000000-0000-0000-0000-00000000c001', 'INR', 'Indian Rupee');
INSERT INTO platform.tenant (id, code, name) VALUES
 ('00000000-0000-0000-0000-0000000000a1', 'alpha', 'Alpha Trust'),
 ('00000000-0000-0000-0000-0000000000b1', 'beta', 'Beta Trust');
INSERT INTO org.institution (id, tenant_id, code, name) VALUES
 ('00000000-0000-0000-0000-0000000001a1', '00000000-0000-0000-0000-0000000000a1', 'alpha-school', 'Alpha School'),
 ('00000000-0000-0000-0000-0000000001b1', '00000000-0000-0000-0000-0000000000b1', 'beta-school', 'Beta School');
INSERT INTO org.academic_year (id, tenant_id, institution_id, name, start_date, end_date) VALUES
 ('00000000-0000-0000-0000-0000000002a1', '00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000001a1', '2026-27', '2026-06-01', '2027-03-31');
INSERT INTO people.person (id, tenant_id, display_name) VALUES
 ('00000000-0000-0000-0000-0000000003a1', '00000000-0000-0000-0000-0000000000a1', 'Asha Student'),
 ('00000000-0000-0000-0000-0000000003a2', '00000000-0000-0000-0000-0000000000a1', 'Asha Parent'),
 ('00000000-0000-0000-0000-0000000003b1', '00000000-0000-0000-0000-0000000000b1', 'Bela Student');
INSERT INTO sis.student (id, tenant_id, person_id, institution_id) VALUES
 ('00000000-0000-0000-0000-0000000004a1', '00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000003a1', '00000000-0000-0000-0000-0000000001a1');
INSERT INTO lms.course (id, tenant_id, title) VALUES
 ('00000000-0000-0000-0000-00000000cca1', '00000000-0000-0000-0000-0000000000a1', 'Maths 101');
INSERT INTO sis.guardian (id, tenant_id, person_id) VALUES
 ('00000000-0000-0000-0000-0000000005a1', '00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000003a2'),
 ('00000000-0000-0000-0000-0000000005b1', '00000000-0000-0000-0000-0000000000b1', '00000000-0000-0000-0000-0000000003b1');

-- ---- 1. cross-tenant link rejected by composite FK (beta guardian on alpha student) ----
DO $$ BEGIN
  INSERT INTO sis.student_guardian (tenant_id, student_id, guardian_id)
  VALUES ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000004a1', '00000000-0000-0000-0000-0000000005b1');
  RAISE EXCEPTION 'SMOKE FAIL: cross-tenant guardian link was accepted';
EXCEPTION WHEN foreign_key_violation THEN NULL; END $$;
-- same-tenant link works
INSERT INTO sis.student_guardian (tenant_id, student_id, guardian_id)
VALUES ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000004a1', '00000000-0000-0000-0000-0000000005a1');

-- ---- 2. duplicate gateway payment rejected ----
INSERT INTO finance.payment (id, tenant_id, purpose, amount, currency_id, billing_name, payer_name, gateway_name, gateway_payment_id, lms_course_id)
VALUES ('00000000-0000-0000-0000-0000000006a1', '00000000-0000-0000-0000-0000000000a1', 'lms_course', 100, '00000000-0000-0000-0000-00000000c001', 'Asha', 'Asha', 'razorpay', 'pay_123', '00000000-0000-0000-0000-00000000cca1');
DO $$ BEGIN
  INSERT INTO finance.payment (tenant_id, purpose, amount, currency_id, billing_name, payer_name, gateway_name, gateway_payment_id, lms_course_id)
  VALUES ('00000000-0000-0000-0000-0000000000a1', 'lms_course', 100, '00000000-0000-0000-0000-00000000c001', 'Asha', 'Asha', 'razorpay', 'pay_123', '00000000-0000-0000-0000-00000000cca1');
  RAISE EXCEPTION 'SMOKE FAIL: duplicate gateway payment accepted';
EXCEPTION WHEN unique_violation THEN NULL; END $$;

-- ---- 3. payment must name a payer ----
DO $$ BEGIN
  INSERT INTO finance.payment (tenant_id, purpose, amount, currency_id, billing_name, lms_course_id)
  VALUES ('00000000-0000-0000-0000-0000000000a1', 'lms_course', 50, '00000000-0000-0000-0000-00000000c001', 'x', '00000000-0000-0000-0000-00000000cca1');
  RAISE EXCEPTION 'SMOKE FAIL: payerless payment accepted';
EXCEPTION WHEN check_violation THEN NULL; END $$;

-- ---- 4. allocation targets exactly one of invoice/due ----
DO $$ BEGIN
  INSERT INTO finance.payment_allocation (tenant_id, payment_id, amount, currency_id)
  VALUES ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000006a1', 10, '00000000-0000-0000-0000-00000000c001');
  RAISE EXCEPTION 'SMOKE FAIL: targetless allocation accepted';
EXCEPTION WHEN check_violation THEN NULL; END $$;

-- ---- 5. duplicate generated fee due rejected (ad-hoc manual dues stay free) ----
INSERT INTO finance.fee_head (id, tenant_id, code, name) VALUES
 ('00000000-0000-0000-0000-000000000fb1', '00000000-0000-0000-0000-0000000000a1', 'TUITION', 'Tuition');
INSERT INTO finance.student_fee_due (id, tenant_id, institution_id, student_id, academic_year_id, fee_head_id, amount, currency_id, status, due_date)
VALUES ('00000000-0000-0000-0000-0000000007a1', '00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000001a1', '00000000-0000-0000-0000-0000000004a1', '00000000-0000-0000-0000-0000000002a1', '00000000-0000-0000-0000-000000000fb1', 500, '00000000-0000-0000-0000-00000000c001', 'draft', '2026-07-01');
DO $$ BEGIN
  INSERT INTO finance.student_fee_due (tenant_id, institution_id, student_id, academic_year_id, fee_head_id, amount, currency_id, status, due_date)
  VALUES ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000001a1', '00000000-0000-0000-0000-0000000004a1', '00000000-0000-0000-0000-0000000002a1', '00000000-0000-0000-0000-000000000fb1', 500, '00000000-0000-0000-0000-00000000c001', 'draft', '2026-07-01');
  RAISE EXCEPTION 'SMOKE FAIL: duplicate generated fee due accepted';
EXCEPTION WHEN unique_violation THEN NULL; END $$;

-- ---- 6. nested terms now allowed; duplicate start under same parent rejected ----
INSERT INTO org.academic_term (id, tenant_id, academic_year_id, name, start_date, end_date)
VALUES ('00000000-0000-0000-0000-0000000008a1', '00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000002a1', 'Semester 1', '2026-06-01', '2026-10-31');
INSERT INTO org.academic_term (tenant_id, academic_year_id, parent_term_id, name, start_date, end_date)
VALUES ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000002a1', '00000000-0000-0000-0000-0000000008a1', 'Quarter 1', '2026-06-01', '2026-08-15');
DO $$ BEGIN
  INSERT INTO org.academic_term (tenant_id, academic_year_id, parent_term_id, name, start_date, end_date)
  VALUES ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000002a1', '00000000-0000-0000-0000-0000000008a1', 'Quarter 1 dup', '2026-06-01', '2026-08-20');
  RAISE EXCEPTION 'SMOKE FAIL: duplicate nested term start accepted';
EXCEPTION WHEN unique_violation THEN NULL; END $$;

-- ---- 7. enrollment history: same year re-placement allowed when prior row is closed ----
INSERT INTO sis.student_enrollment (id, tenant_id, student_id, academic_year_id, enrollment_status, result_status, effective_from, effective_to, change_reason)
VALUES ('00000000-0000-0000-0000-0000000009a1', '00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000004a1', '00000000-0000-0000-0000-0000000002a1', 'enrolled', 'pending', '2026-06-01', '2026-08-01', 'initial');
INSERT INTO sis.student_enrollment (tenant_id, student_id, academic_year_id, enrollment_status, result_status, effective_from, previous_enrollment_id, change_reason)
VALUES ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000004a1', '00000000-0000-0000-0000-0000000002a1', 'enrolled', 'pending', '2026-08-02', '00000000-0000-0000-0000-0000000009a1', 'section_change');
DO $$ BEGIN
  INSERT INTO sis.student_enrollment (tenant_id, student_id, academic_year_id, enrollment_status, result_status, effective_from, change_reason)
  VALUES ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000004a1', '00000000-0000-0000-0000-0000000002a1', 'enrolled', 'pending', '2026-09-01', 'correction');
  RAISE EXCEPTION 'SMOKE FAIL: two open placements in one year accepted';
EXCEPTION WHEN unique_violation THEN NULL; END $$;

-- ---- 8. one open loan per copy ----
INSERT INTO library.library_card_type (id, tenant_id, name, code, max_items, loan_days) VALUES ('00000000-0000-0000-0000-000000000ca1', '00000000-0000-0000-0000-0000000000a1', 'Student', 'STUDENT', 3, 14);
INSERT INTO library.media (id, tenant_id, institution_id, title) VALUES ('00000000-0000-0000-0000-000000000da1', '00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000001a1', 'Algebra');
INSERT INTO library.media_unit (id, tenant_id, media_id, accession_number) VALUES ('00000000-0000-0000-0000-000000000ea1', '00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-000000000da1', 'ACC-1');
INSERT INTO library.library_card (id, tenant_id, institution_id, card_number, card_type_id, holder_type, person_id, student_id, issue_date)
VALUES ('00000000-0000-0000-0000-000000000fa1', '00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000001a1', 'CARD-1', '00000000-0000-0000-0000-000000000ca1', 'student', '00000000-0000-0000-0000-0000000003a1', '00000000-0000-0000-0000-0000000004a1', '2026-06-01');
INSERT INTO library.loan (tenant_id, media_unit_id, media_id, library_card_id, status, issued_on)
VALUES ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-000000000ea1', '00000000-0000-0000-0000-000000000da1', '00000000-0000-0000-0000-000000000fa1', 'issued', '2026-06-02');
DO $$ BEGIN
  INSERT INTO library.loan (tenant_id, media_unit_id, media_id, library_card_id, status, issued_on)
  VALUES ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-000000000ea1', '00000000-0000-0000-0000-000000000da1', '00000000-0000-0000-0000-000000000fa1', 'issued', '2026-06-03');
  RAISE EXCEPTION 'SMOKE FAIL: second open loan on one copy accepted';
EXCEPTION WHEN unique_violation THEN NULL; END $$;

-- ---- 9. partition routing: staff attendance (hrms time_attendance) lands in its month, and its in-module
--         leave link (hr.leave_application, tenant-composite FK) can never point into another tenant ----
INSERT INTO hr.staff (id, tenant_id, person_id, institution_id, employment_type, status)
VALUES ('00000000-0000-0000-0000-000000000aa1', '00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000003a2', '00000000-0000-0000-0000-0000000001a1', 'full_time', 'active');
INSERT INTO time_attendance.staff_attendance (tenant_id, staff_id, attendance_date, status)
VALUES ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-000000000aa1', '2026-07-15', 'present');
DO $$ DECLARE n int; BEGIN
  SELECT count(*) INTO n FROM time_attendance.staff_attendance_2026_07;
  IF n <> 1 THEN RAISE EXCEPTION 'SMOKE FAIL: staff attendance not routed to monthly partition'; END IF;
END $$;
INSERT INTO hr.staff (id, tenant_id, person_id, institution_id, employment_type, status)
VALUES ('00000000-0000-0000-0000-000000000ab1', '00000000-0000-0000-0000-0000000000b1', '00000000-0000-0000-0000-0000000003b1', '00000000-0000-0000-0000-0000000001b1', 'full_time', 'active');
INSERT INTO hr.leave_application (id, tenant_id, institution_id, academic_year_id, staff_id, from_at, to_at, status)
VALUES ('00000000-0000-0000-0000-00000000f1b1', '00000000-0000-0000-0000-0000000000b1', '00000000-0000-0000-0000-0000000001b1', '00000000-0000-0000-0000-0000000002a1', '00000000-0000-0000-0000-000000000ab1', '2026-07-14', '2026-07-16', 'approved');
DO $$ BEGIN
  INSERT INTO time_attendance.staff_attendance (tenant_id, staff_id, attendance_date, status, leave_application_id)
  VALUES ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-000000000aa1', '2026-07-16', 'on_leave', '00000000-0000-0000-0000-00000000f1b1');
  RAISE EXCEPTION 'SMOKE FAIL: staff attendance accepted another tenant''s leave application';
EXCEPTION WHEN foreign_key_violation THEN NULL; END $$;

COMMIT;

-- ---- 10. RLS isolation (needs its own role; outside the seed transaction) ----
DROP ROLE IF EXISTS mysms_app_smoke;
CREATE ROLE mysms_app_smoke LOGIN;
GRANT USAGE ON SCHEMA sis, platform, org, people TO mysms_app_smoke;
GRANT SELECT ON ALL TABLES IN SCHEMA sis, org, people TO mysms_app_smoke;
SET ROLE mysms_app_smoke;
SET app.tenant_id = '00000000-0000-0000-0000-0000000000a1';
DO $$ DECLARE n int; BEGIN
  SELECT count(*) INTO n FROM sis.guardian;
  IF n <> 1 THEN RAISE EXCEPTION 'SMOKE FAIL: RLS returned % guardian rows for tenant alpha (expected 1: beta row must be invisible)', n; END IF;
END $$;
SET app.tenant_id = '00000000-0000-0000-0000-0000000000b1';
DO $$ DECLARE n int; BEGIN
  SELECT count(*) INTO n FROM sis.guardian;
  IF n <> 1 THEN RAISE EXCEPTION 'SMOKE FAIL: RLS returned % guardian rows for tenant beta', n; END IF;
  SELECT count(*) INTO n FROM sis.student;
  IF n <> 0 THEN RAISE EXCEPTION 'SMOKE FAIL: beta can see alpha students'; END IF;
END $$;
RESET ROLE;
DROP OWNED BY mysms_app_smoke;
DROP ROLE mysms_app_smoke;

SELECT 'SMOKE OK' AS result;

-- ---- 11. review regressions ----
-- versioning: a second live version in the same slot is rejected; supersede-then-insert works
INSERT INTO lms.chapter (id, tenant_id, course_id, title, sort_order) VALUES
 ('00000000-0000-0000-0000-000000000c11', '00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-00000000cca1', 'Ch 1', 1);
INSERT INTO lms.lesson (id, tenant_id, title, chapter_id, sort_order, version, published_at) VALUES
 ('00000000-0000-0000-0000-000000000e01', '00000000-0000-0000-0000-0000000000a1', 'L1 v1', '00000000-0000-0000-0000-000000000c11', 1, 1, now());
DO $$ BEGIN
  INSERT INTO lms.lesson (tenant_id, title, chapter_id, sort_order, version)
  VALUES ('00000000-0000-0000-0000-0000000000a1', 'L1 v2 premature', '00000000-0000-0000-0000-000000000c11', 1, 2);
  RAISE EXCEPTION 'SMOKE FAIL: two live versions in one slot accepted';
EXCEPTION WHEN unique_violation THEN NULL; END $$;
BEGIN;
UPDATE lms.lesson SET superseded_by_id = '00000000-0000-0000-0000-000000000e02' WHERE id = '00000000-0000-0000-0000-000000000e01';
INSERT INTO lms.lesson (id, tenant_id, title, chapter_id, sort_order, version) VALUES
 ('00000000-0000-0000-0000-000000000e02', '00000000-0000-0000-0000-0000000000a1', 'L1 v2', '00000000-0000-0000-0000-000000000c11', 1, 2);
COMMIT;
DO $$ DECLARE n int; BEGIN
  SELECT count(*) INTO n FROM lms.lesson WHERE chapter_id = '00000000-0000-0000-0000-000000000c11';
  IF n <> 2 THEN RAISE EXCEPTION 'SMOKE FAIL: supersede-then-insert versioning flow failed'; END IF;
END $$;

-- money: soft-deleted payment must NOT reopen gateway dedupe
UPDATE finance.payment SET deleted_at = now() WHERE id = '00000000-0000-0000-0000-0000000006a1';
DO $$ BEGIN
  INSERT INTO finance.payment (tenant_id, purpose, amount, currency_id, billing_name, payer_name, gateway_name, gateway_payment_id, lms_course_id)
  VALUES ('00000000-0000-0000-0000-0000000000a1', 'lms_course', 100, '00000000-0000-0000-0000-00000000c001', 'Asha', 'Asha', 'razorpay', 'pay_123', '00000000-0000-0000-0000-00000000cca1');
  RAISE EXCEPTION 'SMOKE FAIL: soft-deleted payment reopened gateway dedupe';
EXCEPTION WHEN unique_violation THEN NULL; END $$;
UPDATE finance.payment SET deleted_at = NULL WHERE id = '00000000-0000-0000-0000-0000000006a1';

-- refund dedupe
INSERT INTO finance.refund (tenant_id, payment_id, amount, gateway_refund_id)
VALUES ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000006a1', 10, 'rfnd_1');
DO $$ BEGIN
  INSERT INTO finance.refund (tenant_id, payment_id, amount, gateway_refund_id)
  VALUES ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000006a1', 10, 'rfnd_1');
  RAISE EXCEPTION 'SMOKE FAIL: duplicate gateway refund accepted';
EXCEPTION WHEN unique_violation THEN NULL; END $$;

-- two ad-hoc manual dues in one year are allowed now
INSERT INTO finance.student_fee_due (tenant_id, institution_id, student_id, academic_year_id, amount, currency_id, status, due_date)
VALUES ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000001a1', '00000000-0000-0000-0000-0000000004a1', '00000000-0000-0000-0000-0000000002a1', 100, '00000000-0000-0000-0000-00000000c001', 'draft', '2026-08-01');
INSERT INTO finance.student_fee_due (tenant_id, institution_id, student_id, academic_year_id, amount, currency_id, status, due_date)
VALUES ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000001a1', '00000000-0000-0000-0000-0000000004a1', '00000000-0000-0000-0000-0000000002a1', 200, '00000000-0000-0000-0000-00000000c001', 'draft', '2026-09-01');

-- allocation currency guard: allocation must share the payment currency
INSERT INTO master.currency (id, code, name) VALUES ('00000000-0000-0000-0000-00000000c002', 'USD', 'US Dollar');
DO $$ BEGIN
  INSERT INTO finance.payment_allocation (tenant_id, payment_id, student_fee_due_id, amount, currency_id)
  VALUES ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000006a1', '00000000-0000-0000-0000-0000000007a1', 10, '00000000-0000-0000-0000-00000000c002');
  RAISE EXCEPTION 'SMOKE FAIL: allocation in a different currency than the payment accepted';
EXCEPTION WHEN foreign_key_violation THEN NULL; END $$;

-- evidence protection: deleting a payment with gateway events is blocked
INSERT INTO finance.gateway_event (tenant_id, gateway_name, event_type, payment_id, gateway_event_id)
VALUES ('00000000-0000-0000-0000-0000000000a1', 'razorpay', 'payment.captured', '00000000-0000-0000-0000-0000000006a1', 'evt_1');
DO $$ BEGIN
  DELETE FROM finance.payment WHERE id = '00000000-0000-0000-0000-0000000006a1';
  RAISE EXCEPTION 'SMOKE FAIL: payment with webhook evidence was hard-deletable';
EXCEPTION WHEN foreign_key_violation THEN NULL; END $$;

SELECT 'SMOKE2 OK' AS result;


-- ============================================================================
-- SMOKE3: distributed-deployment invariants (no cross-module coupling)
-- ============================================================================
RESET ROLE; RESET app.tenant_id;

-- 1. ZERO cross-module foreign keys anywhere (modules may live on separate instances). The schemas of one
--    module deploy together, so a foreign key between hrms's own schemas (hr, payroll, time_attendance) is
--    allowed — and only between them (owner decision, reorganisation 2c).
DO $$
DECLARE n int;
BEGIN
  SELECT count(*) INTO n
  FROM pg_constraint con
  JOIN pg_class c  ON c.oid  = con.conrelid
  JOIN pg_namespace ns  ON ns.oid  = c.relnamespace
  JOIN pg_class c2 ON c2.oid = con.confrelid
  JOIN pg_namespace ns2 ON ns2.oid = c2.relnamespace
  WHERE con.contype = 'f'
    AND ns.nspname NOT IN ('pg_catalog','information_schema','public')
    AND ns.nspname <> ns2.nspname
    AND NOT (ns.nspname  IN ('hr','payroll','time_attendance')
         AND ns2.nspname IN ('hr','payroll','time_attendance'));
  IF n <> 0 THEN RAISE EXCEPTION 'SMOKE3 FAIL: % cross-module foreign keys exist', n; END IF;
END $$;

-- 2. Module-local infrastructure tables exist (idempotency/operation in every service schema,
--    number_sequence in exactly the schemas that issue numbers).
DO $$
DECLARE missing text;
BEGIN
  SELECT string_agg(s || '.' || t, ', ') INTO missing FROM (
    SELECT s, t FROM unnest(ARRAY['platform','iam','org','people','academics','sis','hr','payroll',
      'time_attendance','admission','attendance','facility','timetable','assessment','lms','finance','library',
      'transport','comms','frontoffice','careers']) s,
      unnest(ARRAY['idempotency_key','operation']) t
    UNION ALL
    SELECT s, 'number_sequence' FROM unnest(ARRAY['admission','finance','hr','library','lms','payroll','sis']) s
  ) req
  WHERE NOT EXISTS (SELECT 1 FROM information_schema.tables x
                    WHERE x.table_schema = req.s AND x.table_name = req.t);
  IF missing IS NOT NULL THEN RAISE EXCEPTION 'SMOKE3 FAIL: missing module-local tables: %', missing; END IF;
  -- and no stray number source in a module that issues nothing (a shared or orphan sequence is a design error)
  SELECT string_agg(table_schema, ', ') INTO missing FROM information_schema.tables
   WHERE table_name = 'number_sequence'
     AND table_schema NOT IN ('admission','finance','hr','library','lms','payroll','sis');
  IF missing IS NOT NULL THEN RAISE EXCEPTION 'SMOKE3 FAIL: unexpected number_sequence in: %', missing; END IF;
END $$;

-- 3. RLS isolates the new module-local tables (finance idempotency key invisible to another tenant).
CREATE ROLE mysms_smoke3 LOGIN;
GRANT USAGE ON SCHEMA finance TO mysms_smoke3;
GRANT SELECT, INSERT, UPDATE ON finance.idempotency_key, finance.number_sequence TO mysms_smoke3;
SET ROLE mysms_smoke3;
SET app.tenant_id = '00000000-0000-0000-0000-0000000000a1';
INSERT INTO finance.idempotency_key (id, tenant_id, idempotency_key, operation_name, expires_at)
VALUES ('00000000-0000-0000-0000-00000000f0a1', '00000000-0000-0000-0000-0000000000a1',
        '00000000-0000-0000-0000-00000000c0a1', 'finance.RecordPayment', now() + interval '1 day');
SET app.tenant_id = '00000000-0000-0000-0000-0000000000b1';
DO $$
DECLARE n int;
BEGIN
  SELECT count(*) INTO n FROM finance.idempotency_key;
  IF n <> 0 THEN RAISE EXCEPTION 'SMOKE3 FAIL: tenant beta sees % idempotency rows of tenant alpha', n; END IF;
END $$;
RESET ROLE; RESET app.tenant_id;

-- 4. Module-local document numbering: finance issues a number with SELECT ... FOR UPDATE in finance's own schema.
SET ROLE mysms_smoke3;
SET app.tenant_id = '00000000-0000-0000-0000-0000000000a1';
INSERT INTO finance.number_sequence (id, tenant_id, sequence_code, prefix)
VALUES ('00000000-0000-0000-0000-00000000f1a1', '00000000-0000-0000-0000-0000000000a1', 'invoice', 'INV-');
DO $$
DECLARE v bigint;
BEGIN
  SELECT next_value INTO v FROM finance.number_sequence
   WHERE tenant_id = '00000000-0000-0000-0000-0000000000a1' AND sequence_code = 'invoice' AND period_key = ''
   FOR UPDATE;
  UPDATE finance.number_sequence SET next_value = next_value + 1
   WHERE id = '00000000-0000-0000-0000-00000000f1a1';
  IF v <> 1 THEN RAISE EXCEPTION 'SMOKE3 FAIL: first issued number was %', v; END IF;
END $$;
RESET ROLE; RESET app.tenant_id;

REASSIGN OWNED BY mysms_smoke3 TO postgres; DROP OWNED BY mysms_smoke3; DROP ROLE mysms_smoke3;

SELECT 'SMOKE3 OK' AS result;

-- ============================================================================================================
-- SMOKE4 — hierarchical settings (iam.setting_value): scope rules, one value per scope, tenant isolation.
-- ============================================================================================================
DROP ROLE IF EXISTS mysms_smoke4;
CREATE ROLE mysms_smoke4 NOLOGIN;
GRANT USAGE ON SCHEMA iam TO mysms_smoke4;
GRANT SELECT, INSERT ON iam.setting_value TO mysms_smoke4;
GRANT SELECT ON iam.setting_definition TO mysms_smoke4;
SET ROLE mysms_smoke4;
SET app.tenant_id = '00000000-0000-0000-0000-0000000000a1';
INSERT INTO iam.setting_value (tenant_id, scope_type, scope_id, setting_key, value, is_locked)
VALUES ('00000000-0000-0000-0000-0000000000a1', 'tenant', NULL, 'ui.theme', 'light', true);
DO $$
BEGIN
  BEGIN  -- a second tenant-level value for the same key
    INSERT INTO iam.setting_value (tenant_id, scope_type, scope_id, setting_key, value)
    VALUES ('00000000-0000-0000-0000-0000000000a1', 'tenant', NULL, 'ui.theme', 'dark');
    RAISE EXCEPTION 'SMOKE4 FAIL: duplicate tenant-level value accepted';
  EXCEPTION WHEN unique_violation THEN NULL; END;
  BEGIN  -- tenant scope must not name a scope id
    INSERT INTO iam.setting_value (tenant_id, scope_type, scope_id, setting_key, value)
    VALUES ('00000000-0000-0000-0000-0000000000a1', 'tenant', '00000000-0000-0000-0000-0000000000c1', 'ui.density', 'compact');
    RAISE EXCEPTION 'SMOKE4 FAIL: tenant scope with a scope id accepted';
  EXCEPTION WHEN check_violation THEN NULL; END;
  BEGIN  -- a user value can never be locked
    INSERT INTO iam.setting_value (tenant_id, scope_type, scope_id, setting_key, value, is_locked)
    VALUES ('00000000-0000-0000-0000-0000000000a1', 'user', '00000000-0000-0000-0000-0000000000c1', 'ui.density', 'compact', true);
    RAISE EXCEPTION 'SMOKE4 FAIL: locked user value accepted';
  EXCEPTION WHEN check_violation THEN NULL; END;
  BEGIN  -- only catalogue settings
    INSERT INTO iam.setting_value (tenant_id, scope_type, scope_id, setting_key, value)
    VALUES ('00000000-0000-0000-0000-0000000000a1', 'user', '00000000-0000-0000-0000-0000000000c1', 'no.such.setting', 'x');
    RAISE EXCEPTION 'SMOKE4 FAIL: value for an unknown setting accepted';
  EXCEPTION WHEN foreign_key_violation THEN NULL; END;
END $$;
SET app.tenant_id = '00000000-0000-0000-0000-0000000000b2';
DO $$
DECLARE n int;
BEGIN
  SELECT count(*) INTO n FROM iam.setting_value;
  IF n <> 0 THEN RAISE EXCEPTION 'SMOKE4 FAIL: tenant beta sees % setting values of tenant alpha', n; END IF;
  SELECT count(*) INTO n FROM iam.setting_definition;
  IF n < 10 THEN RAISE EXCEPTION 'SMOKE4 FAIL: setting catalogue has % rows', n; END IF;
END $$;
RESET ROLE; RESET app.tenant_id;
REASSIGN OWNED BY mysms_smoke4 TO postgres; DROP OWNED BY mysms_smoke4; DROP ROLE mysms_smoke4;

SELECT 'SMOKE4 OK' AS result;
