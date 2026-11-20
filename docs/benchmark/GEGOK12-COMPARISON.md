# GegoK12 benchmark: feature-by-feature comparison with MySMS

Status: analysis only, written 2026-11-19. Nothing in MySMS was changed. GegoK12 is the owner's benchmark: MySMS must
have everything GegoK12 has. Items marked **to verify** could not be confirmed from the sources that were readable.

**Evidence legend.**
- `GK:<path>` = file in the GegoK12 repository, `https://github.com/Gego-K12/gegok12/blob/main/<path>` (read through
  raw.githubusercontent.com and the GitHub contents API; GitHub tree pages are robots-blocked).
- `WEB:<path>` = page on `https://gegok12.com/<path>`; `DOC:<path>` = page on `https://docs.gegok12.com/documentation/<path>`.
- MySMS evidence: `table` = `database/migrations/<module>/V*.sql`; `Op` = `api/operations-status.md` operation;
  screen ids (`SIS-02` ...) = `ui/02-navigation-and-screens.md`; ids `NM-*`, `IN-*`, `CP-*`, `ARC-*`, `SEC-*`, `BE-*` = `ROADMAP.md`.

**MySMS status values** (first word of the Status column, used for the counts in §3.20):
- **Has** = record types and operations exist and are served (2,004 of 2,055 operations are served).
- **Partial** = the core is served but a sub-feature GegoK12 ships is missing or answers 501.
- **Contract** = contract only: the operation exists but answers `501 NOT_IMPLEMENTED`.
- **Planned** = not built; a ROADMAP item covers it (id given).
- **Missing** = not built and not planned anywhere in ROADMAP.md or docs/.

Note: "Has" is an API statement. Only 93 of 186 web screens are built (ROADMAP §1), so many "Has" items still need
their screen; the screen id is given so the UI gap can be tracked.

---

## 1. What GegoK12 is

| Aspect | Finding | Source |
|---|---|---|
| Product | Open-source school management system / school ERP for K-12, "API first", web admin + Android apps | WEB:(home), GK:readme.md |
| Vendor | GegoSoft Technologies OPC Pvt Ltd, Madurai, India; product marketed since about 2017 (GoodFirms), open-sourced "since 2021" per site | WEB:(home), goodfirms.co, softwaresuggest.com |
| Licence | Core: MIT (modify, rebrand, resell allowed). Pro modules: commercial, shipped as readable source from a private GitHub repo, licence per domain, no redistribution | GK:LICENSE, WEB:pricing/, DOC:add-on-modules/fee-module |
| Tech stack | PHP 8.4, Laravel (version not stated in README; to verify in composer.json), MySQL, Blade + Vue/Tailwind assets via Laravel Mix (webpack.mix.js, tailwind.config.js), Laravel Sanctum API tokens, Laratrust roles/permissions, Spatie activity log and tags, Laravel Scout + Algolia search, Pusher/laravel-websockets realtime, FCM push (custom brozot/laravel-fcm package), MSG91 and Twilio SMS, SMTP mail, S3 or local storage, Google Maps key, reCAPTCHA, Redis optional | GK:.env.example, GK:readme.md, GK:database/migrations (laratrust_setup_tables, create_websockets_statistics_entries_table, create_activity_log_table) |
| Deployment | Self-hosted LAMP/VPS; visual web installer at `/public/installer` (requirements, database, admin account, install, finish); manual CLI (`composer install`, migrate, `db:seed`); Docker (`docker-compose up`, port 8090); Laravel Herd config | GK:readme.md, GK:Dockerfile, GK:docker-compose.yml, GK:herd.yml, DOC:getting-started |
| Repository | `Gego-K12/gegok12`, v1.1, 1 squashed commit, 53 stars, 93 forks, 11 open issues; 13 route files, ~70 migrations, ~95 models, 48 seeders; `api.md` points to Postman collections for the Parent and Teacher apps | GK:readme.md, GK:app/Providers/RouteServiceProvider.php, GitHub contents API |
| Editions and pricing | **Free core**: 26 modules, unlimited users, no expiry. **12 Pro modules**, one-off per domain, lifetime licence + 5 years of updates: Timetable $200, Transport $200 (driver + conductor apps), Question Paper Generator $200, Video Room $250, and $100 each for Examination, Fee, Media files, Chat room, Certificates, Inventory, Stock, Alumni; all 12 = $1,650. **Managed hosting** $49/month or $490/year (to 2,000 students). **Support**: Community free; Standard $59/month or $299 per 10 tickets (8 business-hour response); Priority $149/month (2 h). **Services**: install $99, data migration $199, custom modules / white-label / reselling / school website quoted. Never charged: per-student fees, apps, extra branches on one domain, data export, rebranding the core | WEB:pricing/, WEB:modules/ |
| Free trial / demo | Demo server with pre-loaded data, credentials sent after a form; guided screen-share walkthrough; "90-day free trial" for your own school | WEB:school-software-demo/ |
| Apps | Parent app (Android, free, `com.gegok12.parent`), Teacher app (Android, free, `com.gegok12.teacherapp`, QR connect), Driver app and Conductor app (Pro, with Transport), student web portal. No iOS app found | WEB:(home), WEB:school-parent-application/, WEB:driver-app/ |
| Target market | Indian K-12 (CBSE, ICSE, IB, state boards, "international"), price-sensitive schools; positions itself against Gibbon, RosarioSIS, openSIS and Fedena; partner/referral programme for resellers | WEB:(home), WEB:open-source-school-erp, WEB:referral-program/ |
| Roles (code) | 13 user groups: SiteAdmin, SiteSubadmin, SchoolAdmin, SchoolSubadmin, Teacher, Student, Parent, Librarian, OldStudent, Receptionist, Accountant, Stock Keeper, Non Teaching. 7 functional roles layered on staff: leave_applier, leave_checker, principal, student_leave_checker, class_coordinator, transport_coordinator, transport_driver. Plus a `privilegeconditions` middleware on admin routes | GK:database/seeders/UsergroupTableSeeder.php, GK:database/seeders/RolesTableSeeder.php, GK:app/Providers/RouteServiceProvider.php |
| Portals (route files) | admin, setting, inventory, stock, librarian, student, teacher, receptionist, accountant, payroll, superadmin (only a commented dashboard), api (parent app), teacherapi (teacher app), addon (exam installer) | GK:app/Providers/RouteServiceProvider.php, GK:routes/*.php |

Observations on code quality (relevant to "everything it has"): GegoK12's open core is smaller than its marketing.
`routes/inventory.php` and `routes/stock.php` are empty in the open repo (Pro), the superadmin portal is a commented-out
dashboard, the fee and exam API routes for the parent app are commented out (fees list route disabled "due to a known
error in the fees add-on"), several destructive actions use GET, and `/cache-clear` and `/checksms` are public routes.
Several module pages linked from WEB:modules/ return 404 (communication, dashboard-and-reports, school-settings,
question-paper-generator). Website claims (GPS tracking, auto timetable, hall tickets, QR asset tracking) are Pro and
could not be checked in code: they are listed as GegoK12 features with "(website claim)".

## 2. GegoK12 feature inventory and 3. comparison with MySMS

Sections 2 and 3 are merged. Features MySMS already has working (100 of 191) are left out, so only the gaps remain: partial, contract-only (501), planned and missing. Each row is one GegoK12 feature (inventory, with GegoK12 evidence) and its MySMS status
(with MySMS evidence). Module grouping follows GegoK12's own (WEB:modules/, DOC:list-of-modules). Free = open core;
Pro = paid add-on.

### 2.0 Module list as GegoK12 sells it

| Group | Free core (26) | Pro (12) | Website-only pages (status unclear) |
|---|---|---|---|
| Students and admissions | Student, Admissions, Promotion, Parent, Discipline, ID card and bus pass | Certificates, Alumni | |
| Classroom and teaching | Class and section, Subject, Homework and assignment, Lesson plan, Class wall | Timetable | Online quiz |
| Exams | | Examination, Question paper generator | |
| Attendance and staff | Attendance, Staff, Leave master, Holiday master, Task | | Leave and payroll (payroll is in the open code) |
| Fees and accounts | Account management (income/expense) | Fee management | |
| Communication | Communication (SMS + push), Notice board, Events and calendar (+ gallery), Telephone directory | Chat room | Emergency notification, Magazine and journal |
| Library, reception, assets | Library, Reception | Transport, Inventory, Stock | Facilities management |
| Digital learning | | Video room, Media files | |
| Platform | Dashboard and reports, Settings, Activity logs | | |

### 3.1 Platform, installation, multi-school, settings

| # | GegoK12 feature | GegoK12 evidence | MySMS status | MySMS evidence |
|---|---|---|---|---|
| G001 | Web installer wizard (requirements check, DB, admin user, install, finish) | GK:readme.md | Missing | DEPLOYMENT.md is a manual runbook; tenants are created by platform staff (PLT-01). No school self-setup wizard |
| G002 | Docker / docker-compose one-command deployment | GK:docker-compose.yml, GK:Dockerfile | Missing | ROADMAP §1: "no CI pipeline, containers or e2e tests exist yet" |
| G003 | Demo data seeders (schools, users, teachers, students, standards, subjects, homework, assignments, lesson plans, notices, holidays, payroll, fee payments, books, grades) | GK:database/seeders/* (48 seeders) | Partial | `platform.ClearDemoData` served (so a demo-data concept exists); a ready demo tenant/seed pack is **to verify** |
| G006 | Add-on marketplace inside the app: catalogue, detail, Razorpay checkout, purchase history, `module:install-*` artisan installers | GK:routes/admin.php (addons), GK:routes/addon.php, GK:routes/api.php `/v2/addons`, DOC:add-on-modules | Missing | Plans exist but no per-module add-on catalogue / feature entitlement per tenant (**to verify** whether `saas_plan` gates modules) |
| G009 | Academic years with status Current / New / Old, status switch | DOC:core-modules/settings, GK:routes/setting.php | Partial | `org.academic_year`, `academic_term` served; `org.ActivateAcademicYear` and `GenerateTerms` answer 501 |
| G011 | General settings, maintenance mode, SEO settings | GK:routes/admin.php (settings) | Partial | `platform.system_setting`, `iam.setting_definition/setting_value` (ADM-15/16). Maintenance mode and SEO: Missing (SEO belongs to CP-4 website, parked) |
| G017 | reCAPTCHA on public forms (admission form, login) | GK:.env.example | Missing | no captcha found in contracts (**to verify** in web client) |
| G018 | E-mail verification link and OTP verification | GK:routes/web.php `/emailverification`, `/verifyotp` | Partial | `iam.RequestVerification` / `ConfirmVerification` served; SMS/e-mail delivery blocked by ARC-2; SMS MFA 501 (SEC-9) |
| G023 | Groups per class with members | GK:routes/admin.php (groups), models Group/GroupMember | Partial | `comms.audience_group` served; `comms.AddGroupMembers` answers 501 |
| G027 | APK download links served by the backend | GK:routes/api.php `/apk/parent`, `/apk/teacher` | Missing | not needed if apps ship through stores (low priority) |

### 3.2 Roles and access

| # | GegoK12 feature | GegoK12 evidence | MySMS status | MySMS evidence |
|---|---|---|---|---|
| G029 | 13 user groups (incl. Librarian, Receptionist, Accountant, Stock Keeper, Non Teaching, OldStudent) each with its own portal | GK:UsergroupTableSeeder.php, RouteServiceProvider.php | Partial | `iam.role`, `permission`, `role_permission`, `user_role`, ADM-10; persona switcher ACC-06. Permission filtering in every module is **SEC-0 (P0)**; ready-made role templates per persona **to verify** |
| G031 | Privilege conditions (per-feature privileges for school sub-admins) | GK:RouteServiceProvider.php `privilegeconditions` | Planned (SEC-0) | `iam.user_permission`, `user_department_access` exist; enforcement in every module is SEC-0 |

### 3.3 Dashboards and reports

| # | GegoK12 feature | GegoK12 evidence | MySMS status | MySMS evidence |
|---|---|---|---|---|
| G032 | Admin dashboard: events, tasks with counts, fee unpaid list, today's absentees (students, staff, teachers) | GK:routes/admin.php (dashboard), setting.php (absentees) | Partial | ADM-01 composed dashboard; ATT-05 defaulters; principal/management dashboards Planned (CP-7) |
| G033 | Birthday list for students and teachers with "send wishes" message | GK:routes/admin.php, receptionist.php, accountant.php | Contract | `people.ListBirthdays`, `people.SendGreetings` answer 501 (SIS-12) |
| G034 | Work-anniversary list with "send message" | GK:routes/receptionist.php, accountant.php | Missing | no operation; would sit next to `people.ListBirthdays` using `hr.staff` join date |
| G035 | Send fee reminders from the dashboard | GK:routes/admin.php (dashboard fees) | Partial | `finance.SendFeeReminders` served (FIN-10); actual SMS/e-mail/push delivery blocked by ARC-2 |
| G036 | Role dashboards for teacher, student, librarian, receptionist, accountant, stock keeper | every portal route file `/dashboard` | Partial | teacher today and parent child summary (ui §1), LMS dashboards; others Planned (CP-7) |
| G037 | Reports: events, fees, holidays (import/export), birthdays, anniversaries, students active / exited / suspended, parents, inventory (current stock, monthly purchases, monthly sales) | GK:routes/admin.php (reports) | Partial | generic `platform.ListReportDefinitions` / `RunReport` and list exports; named canned reports per module **to verify**; CP-7, PERF-4 |
| G039 | Feed page in every portal (latest school activity) | GK:routes/student.php, teacher.php, receptionist.php | Contract | `comms.GetFeed` answers 501 |

### 3.4 Students, parents, discipline, documents (free core)

| # | GegoK12 feature | GegoK12 evidence | MySMS status | MySMS evidence |
|---|---|---|---|---|
| G043 | Marks graph and all-marks view, mark comparison | GK:routes/admin.php, teacher.php | Partial | ASM-12 results; graph/comparison view **to verify** |
| G045 | Send message to a student / teacher and see sent history | GK:routes/admin.php | Contract | `comms.SendMessage` 501; `comms.outbound_message` table exists (ARC-2) |
| G046 | Student export CSV and PDF | GK:routes/admin.php | Partial | CSV via ADM-12 exports; PDF list export **to verify** |
| G051 | Teacher "performance" entries with type and media (photos/video) sent to parents | GK:routes/teacherapi.php (performance), api.php `/v2/performance` | Partial | `sis.student_activity`, `activity_type`; media attachment via `platform.entity_attachment` **to verify** |
| G054 | Student ID card print (and class ID cards) | GK:routes/admin.php (standard link ID card), DOC:core-modules/id-card | Planned (CP-6) | no renderer yet |
| G055 | Staff and teacher ID cards | GK:routes/admin.php (teachers, staff) | Planned (CP-6) | |
| G056 | Bus pass create / show / print | GK:routes/admin.php (students bus pass) | Planned (CP-6, TRN-02 note) | TRN-02 mentions "Bus pass print"; no operation |
| G058 | Old students (OldStudent user group) | GK:UsergroupTableSeeder.php | Partial | student status / alumni concept in careers and people public profile (ACC-12); alumni module see G150 |

### 3.5 Admissions

| # | GegoK12 feature | GegoK12 evidence | MySMS status | MySMS evidence |
|---|---|---|---|---|
| G062 | Grade X-XII extra fields (group, board registration) | DOC:core-modules/settings | Partial | program/stream via `academics.program`; board registration number **to verify** |
| G063 | Admission enquiries (website claim) | WEB:modules/ | Planned (CP-1) | enquiry CRM |

### 3.6 Classes, subjects, lesson plans, holidays

| # | GegoK12 feature | GegoK12 evidence | MySMS status | MySMS evidence |
|---|---|---|---|---|
| G071 | Lesson plans visible to parents per subject, printable | GK:routes/api.php `/v2/lessonplan` | Partial | parent visibility of lesson plans **to verify** |

### 3.7 Homework and assignments

| # | GegoK12 feature | GegoK12 evidence | MySMS status | MySMS evidence |
|---|---|---|---|---|

### 3.8 Attendance

| # | GegoK12 feature | GegoK12 evidence | MySMS status | MySMS evidence |
|---|---|---|---|---|
| G083 | Same-day absence alerts to parents (push/SMS) | WEB:modules/attendance-management/ | Contract | absence notices blocked by ARC-2 (`comms.SendMessage` 501) |

### 3.9 Examinations and assessment (Pro, plus free grade settings)

| # | GegoK12 feature | GegoK12 evidence | MySMS status | MySMS evidence |
|---|---|---|---|---|
| G089 | Bulk mark import from Excel/CSV | WEB:modules/exam-scheduling-and-result-generation/ | Partial | generic import exists; mark-sheet import template **to verify** |
| G091 | Scholastic and non-scholastic (co-scholastic) grades | GK:migrations sc_grade, non_sc_grade, models Scholastic/NonScholastic | Partial | grading schemes are generic; co-scholastic areas (CBSE) **to verify** |
| G092 | School-branded report cards (PDF) with subject scores, grades, remarks | WEB:modules/exam-scheduling-and-result-generation/ | Partial | `result_template`, `marksheet`, `GenerateResults`, `PublishMarksheet` (ASM-11); PDF rendering **to verify** |
| G093 | Hall tickets / admit cards, bulk print | WEB:modules/exam-scheduling-and-result-generation/ (website claim) | Missing | no record type or operation |
| G095 | Exam schedule change notifications to parents | WEB:modules/exam-scheduling-and-result-generation/ | Contract | ARC-2 |
| G096 | Question paper generator from a question bank by subject, chapter, marks weighting (Pro $200) | WEB:pricing/, WEB:modules/ | Partial | `lms.question`, `question_pool`, `question_pattern` exist; printable paper builder Missing |

### 3.10 Timetable (Pro)

| # | GegoK12 feature | GegoK12 evidence | MySMS status | MySMS evidence |
|---|---|---|---|---|
| G099 | Automatic timetable generation from availability and load | WEB:modules/timetable-generator/ (website claim) | Planned (CP-2) | `timetable.GenerateSessions` answers 501 |
| G100 | Clash checks (teacher, room, class) | WEB:modules/timetable-generator/ | Partial | **to verify** in session creation; full solver in CP-2 |
| G101 | Drag-and-drop editing | WEB:modules/timetable-generator/ | Planned (TT-02) | TT-02 builder screen not built |
| G102 | Print / export / share timetable | WEB:modules/timetable-generator/ | Partial | `comms.ExportCalendar` 501; print **to verify** |

### 3.11 Staff, leave, tasks (free core)

| # | GegoK12 feature | GegoK12 evidence | MySMS status | MySMS evidence |
|---|---|---|---|---|
| G109 | Tasks: create for teachers, non-teachers, students; complete, snooze, edit, delete; counts on dashboard | GK:routes/teacherapi.php, all portals | Partial | `comms.task`, `task_assignee`, `CompleteTask`, `SnoozeTask` served; `ClaimTask` 501 (COM-09) |
| G110 | Reminders | GK:migrations reminders | Partial | snooze served; reminder delivery needs ARC-2 |

### 3.12 Payroll (open code; marketed as "Leave and payroll")

| # | GegoK12 feature | GegoK12 evidence | MySMS status | MySMS evidence |
|---|---|---|---|---|
| G115 | Unpaid payroll report, unpaid bank report export | GK:routes/payroll.php (ReportsController) | Partial | lists exist; bank-transfer file/report **to verify** |

### 3.13 Fees (Pro) and accounts (free)

| # | GegoK12 feature | GegoK12 evidence | MySMS status | MySMS evidence |
|---|---|---|---|---|
| G120 | Outstanding-balance / unpaid reports, reminders to the parent app | WEB:modules/, GK:routes/admin.php | Partial | `student_fee_due`, FIN-03, `SendFeeReminders`; delivery ARC-2 |
| G123 | Account management: income and expenditure heads, ledgers, transaction types and accounts | WEB:modules/, DOC:core-modules/account-management (page errored), GK:seeders TransactionTypeTableSeeder, TrasactionAccountTableSeeder | Missing | `finance` is income-side only; ROADMAP IN-3 notes "No expense ledger"; no planned item builds a cash book |

### 3.14 Library (free core)

| # | GegoK12 feature | GegoK12 evidence | MySMS status | MySMS evidence |
|---|---|---|---|---|
| G127 | Return a book | GK:routes/librarian.php `booklending/return` | Contract | `library.ReturnLoan` answers 501 (BE-1) - a basic circulation action |
| G128 | Late fines | WEB:modules/school-library-management/ | Contract | `library.InvoiceLoanFine` 501 |
| G129 | Due-date reminders | WEB:modules/school-library-management/ | Contract | `RequestLoanReturn` 501; delivery ARC-2 |
| G130 | Barcode / QR issue and scanning | WEB:modules/school-library-management/ (website claim) | Partial | `media_unit` copies exist; scanning in LIB-03 **to verify** |
| G132 | Usage reports, top borrowed | WEB:modules/school-library-management/ | Partial | **to verify** (generic reports) |

### 3.15 Reception / front office (free core)

| # | GegoK12 feature | GegoK12 evidence | MySMS status | MySMS evidence |
|---|---|---|---|---|
| G133 | Visitor log with print/view pass | GK:routes/receptionist.php, admin.php | Partial | `frontoffice.visitor_log` (FO-01); `CheckOutVisitor` 501; printable pass **to verify** |
| G137 | Gate passes for students, staff, visitors (website claim) | WEB:modules/reception-module/ | Planned (NM-6) | |
| G138 | Appointments booking with reminders (website claim) | WEB:modules/reception-module/ | Planned (NM-11, PTM only) | general visitor appointments Missing |
| G139 | Host alert when a visitor checks in | WEB:modules/reception-module/ | Missing | depends on ARC-2 |

### 3.16 Communication, notices, events, class wall (free core + Chat Pro)

| # | GegoK12 feature | GegoK12 evidence | MySMS status | MySMS evidence |
|---|---|---|---|---|
| G140 | SMS (MSG91, Twilio), e-mail (SMTP), push (FCM, separate parent and teacher apps) | GK:.env.example, custompackages/brozot/laravel-fcm | Planned (ARC-2) | `comms.notification_channel`, `outbound_message` tables exist; `SendMessage`, `SendTestMessage`, `RetryOutboundMessage` 501; no provider adapters |
| G141 | Targeted messaging by class / year / whole school | WEB:modules/ | Contract | `audience_group`, `notice_audience` served; sending 501 |
| G145 | Calendar view (events + holidays) in every portal and app | GK:routes/*.php `/events` | Contract | `comms.GetMyCalendar`, `ExportCalendar` 501 (COM-03) |
| G146 | Class wall: page categories, pages, posts with attachments, comments, replies, follow, like/dislike, save | GK:routes/admin.php, teacher.php, student.php | Partial | `class_page`, `post`, `post_comment`, `page_category` served; reactions, `FollowClassPage`, `SubmitPost`, `GetFeed` 501; save/bookmark **to verify** |
| G147 | Private conversations (teacher-student, teacher-parent) | GK:routes/teacher.php, student.php | Partial | `comms.conversation` served; `MarkConversationRead`, `HideMessageForMe` 501 |
| G148 | Chat room module, moderated, real time (Pusher / websockets) (Pro) | WEB:modules/private-chat-for-school-admin/, GK:.env.example | Partial | conversations exist; real-time transport **to verify** |
| G149 | In-app notifications list, mark read | GK:routes/*.php `/notifications` | Contract | `ListInAppNotifications`, `MarkNotificationRead`, `MarkAllNotificationsRead` 501 (ACC-09) |
| G152 | Emergency mass alerts with crisis templates, acknowledgement, archive (website claim) | WEB:modules/emergency-notification/ | Contract | COM-06 Broadcast / emergency screen; sending 501 (ARC-2); acknowledgement via `message_receipt` **to verify** |

### 3.17 Transport (Pro, plus free bus pass)

| # | GegoK12 feature | GegoK12 evidence | MySMS status | MySMS evidence |
|---|---|---|---|---|
| G156 | Vehicles, drivers, conductors records, licences/permits | WEB:modules/transport-management/ | Planned (NM-5) | |
| G157 | Live GPS bus tracking for parents, ETA | WEB:modules/transport-management/, WEB:driver-app/ | Planned (NM-5) | Google Maps key in GegoK12 .env |
| G158 | Boarding / alighting attendance on the bus, parent alerts | WEB:modules/transport-management/ | Planned (NM-5) | |
| G159 | Driver app: odometer start/end, distance, trip time, route compliance, speed | WEB:driver-app/ | Planned (NM-5) | |
| G160 | Conductor app: student boarding records | WEB:(home), WEB:conductor-app/ | Planned (NM-5) | |
| G161 | Maintenance and service reminders for buses | WEB:modules/transport-management/ | Planned (NM-5) | **to verify** maintenance in NM-5 scope |
| G162 | Transport coordinator / driver roles | GK:RolesTableSeeder.php | Planned (NM-5) | |

### 3.18 Inventory, stock, facilities, certificates, alumni (Pro / website)

| # | GegoK12 feature | GegoK12 evidence | MySMS status | MySMS evidence |
|---|---|---|---|---|
| G163 | Inventory: assets, location, QR tracking, usage history | WEB:modules/inventory-management/ | Partial | `facility.asset_item`, `room_asset`; QR, movements Planned (NM-4) |
| G164 | Stock: stock in hand, purchases, sales, dead stock, reorder alerts; Stock Keeper portal | WEB:modules/, GK:routes/stock.php (empty in core), GK:routes/admin.php (inventory reports) | Planned (NM-4) | D5 (review) says "not in v1" |
| G165 | Vendors | WEB:modules/inventory-management/ | Planned (NM-4) | |
| G167 | Maintenance work orders, repair requests, preventive schedules, incident reporting | WEB:modules/facilities-management/ (website claim) | Missing | no maintenance/work-order item in ROADMAP |
| G168 | Certificates: transfer, bonafide, conduct, participation, merit, sports, attendance; template editor, numbering, bulk, e-mail, PDF, issuance log | WEB:modules/digital-certificate-generation/, GK:app/Models/StudentCertificate.php | Partial | `sis.IssueBonafideCertificate` (SIS-09) served; the rest Planned (NM-9) |
| G169 | Alumni: records by passing year, directory, reunions, notifications, fundraising, mentoring | WEB:modules/alumni-management/ | Planned (docs/product) | `docs/product/LEARNING-AND-ALUMNI-BRAINSTORM.md`, community network docs; careers job board Has (CAR-01...04) |

### 3.19 Mobile apps

| # | GegoK12 app feature | GegoK12 evidence | MySMS status | MySMS evidence |
|---|---|---|---|---|
| G171 | Parent app: homework pending/finished, submit with files, reply | GK:routes/api.php | Partial | API Has; phone screens for homework **to verify** ("assessment screens" built) |
| G172 | Parent app: assignments submit, completed | GK:routes/api.php | Partial | as above |
| G173 | Parent app: attendance (monthly) | GK:routes/api.php | Partial | API Has (ATT-04); phone screen **to verify** |
| G174 | Parent app: leave apply / edit / delete for child | GK:routes/api.php | Partial | API Has (`CreateStudentLeaveRequest`); phone screen **to verify** |
| G175 | Parent app: notices (active and expired), events, gallery, holidays, magazine | GK:routes/api.php | Partial | APIs Has; calendar 501; phone screens **to verify** |
| G176 | Parent app: notifications / messages, mark read | GK:routes/api.php | Contract | in-app notifications 501; push needs ARC-2 |
| G179 | Parent app: lesson plans by subject | GK:routes/api.php | Partial | see G071 |
| G180 | Parent app: teacher list for the child | GK:routes/api.php `/v2/teachers` | Partial | `teaching_assignment` data; screen **to verify** |
| G181 | Parent app: personal tasks for the child (create, snooze, complete) | GK:routes/api.php | Partial | `comms.task` served; parent screen **to verify** |
| G183 | Teacher app: attendance (session, absent list) | GK:routes/teacherapi.php | Partial | ATT-03 is web; tablet/phone take-attendance **to verify** |
| G185 | Teacher app: lesson plans 4-step create, publish, print | GK:routes/teacherapi.php | Partial | ACA-10 web; phone **to verify** |
| G187 | Teacher app: principal approvals (lesson plan, homework, assignment) | GK:routes/teacherapi.php | Partial | ASM-02 web; phone **to verify** |
| G188 | Teacher app: performance/discipline entries with media | GK:routes/teacherapi.php | Partial | SIS-06 |
| G189 | Teacher app: tasks, notices, events, gallery, holidays, school details | GK:routes/teacherapi.php | Partial | APIs Has (calendar 501) |
| G190 | Teacher app: connect by QR code issued by the school | WEB:(home) | Missing | MySMS uses tenant code + sign-in; QR onboarding not found |

### 3.20 Counts

191 GegoK12 features found (G001-G191: about 150 from the open code, the rest from website and docs claims).

| MySMS status | Count | Meaning |
|---|---:|---|
| Has | 100 | served by the API (screen may still be a placeholder) |
| Partial | 47 | core served; a sub-feature GegoK12 ships is missing, 501 or to verify |
| Contract | 13 | operation exists but answers 501 |
| Planned | 20 | covered by a ROADMAP item (NM-4/5/6/9/11, CP-1/2/6, ARC-2, SEC-0, TT-02, docs/product) |
| Missing | 11 | not built and not planned |

Contract-only items: G033, G039, G045, G083, G095, G127, G128, G129, G141, G145, G149, G152, G176.
Missing items: G001, G002, G006, G017, G027, G034, G093, G123, G139, G167, G190.

---

## 4. Gap list (prioritised, must-have parity first)

Priority: **P0** = GegoK12 ships it in the free core and a school notices on day one; **P1** = in GegoK12's free core or
a common Pro module, needed for a sales demo; **P2** = Pro or website-only claim; **P3** = nice to have.

| Rank | Gap | GegoK12 rows | Today in MySMS | Recommendation |
|---:|---|---|---|---|
| 1 | **Message delivery: SMS, e-mail, push** (MSG91/Twilio-class SMS, SMTP, FCM/APNs) and the 33 comms 501s | G140, G141, G045, G083, G095, G152, G110, G120, G129, G151 | ARC-2 planned; tables exist; `SendMessage` 501 | **P0.** Build ARC-2 next after SEC-0. Ship at least one Indian SMS provider (MSG91 or similar, DLT templates), SMTP and FCM. It unblocks absence alerts, fee reminders, password-reset links, homework and notice pushes. |
| 2 | **In-app notifications inbox** (list, mark read, mark all read) | G149, G176 | 501 | **P0.** Part of ARC-2 but can ship first: the bell icon is on every GegoK12 screen. |
| 3 | **Library return** (and fines, return reminders, lost) | G127, G128, G129 | `ReturnLoan`, `InvoiceLoanFine`, `RequestLoanReturn`, `MarkLoanLost` 501 | **P0.** A library that cannot take a book back fails a demo. Small job. |
| 4 | **Calendar view** (events + holidays + exams per user) | G145 | `GetMyCalendar`, `ExportCalendar` 501 | **P0.** Every GegoK12 portal and app has `/events`. |
| 5 | **Account management: income and expense cash book** | G123 | Missing (finance is fee income only) | **P1.** GegoK12 gives it free. Add expense heads, expense vouchers, income from non-fee sources, day book and simple income-vs-expense report to `finance`. Also feeds IN-3. |
| 6 | **ID cards and bus passes** for students and staff | G054, G055, G056 | CP-6 (P2) | **P1** (raise from P2): free in GegoK12; reuse the NM-9 renderer. |
| 7 | **Certificates** (TC, bonafide, conduct, character, participation, merit) with templates, numbering, bulk, PDF, log | G168 | bonafide only; NM-9 (P2) | **P1.** TC is legally needed in India at every exit. |
| 8 | **Birthdays, greetings and work anniversaries** on dashboards | G033, G034 | `ListBirthdays`, `SendGreetings` 501; anniversaries missing | **P1.** Cheap and visible; add `ListWorkAnniversaries`. |
| 9 | **Feed, class-wall reactions and follow** | G039, G146 | `GetFeed`, reactions, `FollowClassPage`, `SubmitPost` 501 | **P1.** |
| 10 | **Small 501s GegoK12 has working**: `org.ActivateAcademicYear`, `comms.AddGroupMembers`, `frontoffice.CheckOutVisitor`, `comms.MarkConversationRead`, `comms.ClaimTask`, `comms.MarkFeedbackMessage` | G009, G023, G133, G147, G109, G150 | 501 | **P1.** Finish BE-1. |
| 11 | **Hall tickets / admit cards** and **report-card PDF** | G093, G092 | hall ticket missing; report card PDF to verify | **P1.** Add `GenerateHallTickets` on `exam_session`; confirm `result_template` renders a branded PDF. |
| 12 | **School setup wizard, demo tenant, one-command install** | G001, G002, G003 | Missing | **P1.** See §6.2. A guided first-run checklist plus a seeded demo tenant (`ClearDemoData` already exists) is how GegoK12 sells. |
| 13 | **Automatic timetable generator** | G099, G100, G101 | CP-2 (P2); `GenerateSessions` 501; TT-02 not built | **P2** (Pro in GegoK12, $200). |
| 14 | **Transport fleet, GPS, boarding, driver and conductor apps** | G156-G162 | NM-5 (P2) | **P2** (Pro in GegoK12, $200). Keep NM-5 but add conductor role and driver odometer log. |
| 15 | **Inventory and stock** (assets with QR, stock, purchases, sales, vendors, Stock Keeper role) | G163, G164, G165 | NM-4 (P3); D5 says not in v1 | **P2.** GegoK12 sells it ($100 + $100); reconsider D5. |
| 16 | Question paper generator (printable paper from the LMS question bank) | G096 | Partial | P2: a "print paper" builder over `lms.question_pool`. |
| 17 | Gate pass, appointments, host alert at visitor check-in | G137, G138, G139 | NM-6, NM-11 | P2: add general appointments to NM-11. |
| 18 | Alumni module | G169, G058 | docs/product only | P2: give it a ROADMAP id. |
| 19 | Facility maintenance work orders, incidents | G167 | Missing | P3: add to `campus` (Plan A) or NM-4. |
| 20 | Add-on catalogue / per-module entitlement on plans | G006 | Missing (to verify) | P2: needed for any core/Pro packaging (§6.3). |
| 21 | Role templates for GegoK12's 13 personas and 7 functional roles; SEC-0 | G029-G031 | SEC-0 P0 | P0 (already next). |
| 22 | reCAPTCHA (or equivalent) on public admission form and sign-in | G017 | Missing | P2. |
| 23 | Teacher/parent app connect by QR code | G190 | Missing | P2: QR encodes tenant code + invite token. |
| 24 | Phone screens for parent/teacher flows GegoK12's Android apps already have (attendance taking, homework submit, leave apply, notices, calendar, lesson plans, teacher list, approvals) | G171-G189 | many "to verify" | P1: audit `clients/apps/mobile` against the G170-G191 list. |

---

## 5. Product and UX comparison

### 5.1 Where GegoK12 does better

| Area | GegoK12 | MySMS today | Idea |
|---|---|---|---|
| Install and onboarding | Web installer, Docker, Herd, seeders with a full demo school, default admin, video walkthrough | Runbook only (DEPLOYMENT.md), no seed pack, no first-run guide | First-run checklist and demo tenant (§6.2) |
| Demo and sales | Demo server, guided screen-share, 90-day trial, 38 module landing pages, YouTube playlist | none public | Seeded demo tenant per persona; module one-pagers |
| Pricing transparency | One-off prices on the page, 3-year cost comparison, "never charged" list, data-export promise | `saas_plan` exists, no public packaging | §6.3 |
| Notifications | SMS + push + e-mail live; SMS fallback for parents without the app | delivery not built (ARC-2) | Gap 1 |
| Role simplicity | 13 fixed user groups, each with its own short portal; 7 functional roles assigned to staff (principal, leave checker...) | fine-grained permissions, persona switcher; templates to verify | Ship persona role templates (§6.1) |
| Approvals | Principal approves lesson plans, homework, assignments; leave checker approves leave, all from the teacher app | same approvals exist but on separate screens | One "Approvals" inbox (§6.1) |
| Dashboards | Birthdays, anniversaries, unpaid fees, absentees, tasks on every role dashboard | ADM-01 only, CP-7 planned | CP-7 to include these widgets |
| Settings | One Settings area: school details, academic years, admission toggle, holidays, exam rules, grades, promotions, leave master | spread over ADM-03/05/06, ASM-13, HR-03, ADMN-01, SIS-03, ADM-15/16 | School setup hub (§6.1) |
| Free accounting | Income/expense ledgers in free core | missing | Gap 5 |
| Printables | ID cards, bus passes, visitor pass, lesson plan print, payslip download, magazine download | renderer not built | Shared PDF renderer (NM-9/CP-6) |
| Import templates | "Download format" next to every import (teachers, holidays, library cards, promotions, members) | generic import engine; per-entity template to verify | Template download per import |
| Integrations shipped | MSG91, Twilio, FCM, SMTP, S3, Algolia, Pusher, Google Maps, reCAPTCHA | Razorpay (+ PayU/Stripe refs), Zoom, CloudConvert, Turnitin, Google Calendar, LTI | add SMS/push/e-mail adapters; maps with NM-5 |
| Docs | Per-module user docs at docs.gegok12.com (settings, each module, apps) | developer docs only | User help per screen, later |

## 6. Consolidation and other ideas

### 6.1 Module, screen, role and settings consolidation

| # | Idea | Rationale from GegoK12 | Relation to ARCHITECTURE.md §6 |
|---|---|---|---|
| C1 | **Adopt Plan A (9 modules).** GegoK12's own grouping (Students and admissions / Classroom, exams, attendance, timetable / Staff, leave, payroll / Fees and accounts / Communication / Library, reception, transport, inventory / Platform) maps almost one-to-one onto Plan A's students, academics, workforce, finance, comms, campus, platform (+ identity, learning) | a buyer reads the product in these nine-ish groups | Confirms Plan A over Plan B; no tenth module needed for anything GegoK12 has |
| C2 | Put NM-4 inventory/stock, facility maintenance (G167), gate pass (NM-6) and transport fleet (NM-5) all in **campus** | GegoK12 sells "Library, reception, transport, inventory" as one group run by the admin office | Plan A "campus" = library + transport + frontoffice + facility; extend, do not add modules |
| C3 | Put the **expense cash book** (gap 5) inside **finance**, not a new accounting module | GegoK12 "Fees and accounts" is one group | Plan A keeps finance alone |
| C4 | **One navigation per Plan A module** in the web client (9 top-level menu groups instead of 21 module names) | GegoK12's menu is task-shaped | UI follows the merged modules even before the backend merge |
| C5 | **School setup hub**: one Settings area with tabs School profile (ADM-03), Academic years and terms (ADM-05), Holidays (ADM-06), Classes and sections (ACA-04/05), Subjects (ACA-07), Grading (ASM-13), Leave types (HR-03), Fee setup (FIN-01), Admission cycle (ADMN-01), Notification setup (COM-13), Branding (ADM-07) | GegoK12 DOC:core-modules/settings puts all of these in one place | screen-level only |
| C6 | **One Approvals inbox** for principal/HOD/leave checker: lesson plans, homework, assignments, staff leave, student leave, posts, promotions, admissions | GegoK12's functional roles approve across modules | read model in comms or CP-7 `insights` |
| C7 | **Persona role templates** seeded per tenant: School admin, Sub-admin, Principal, Teacher, Class teacher, Accountant, Librarian, Receptionist, Transport coordinator, Driver, Stock keeper, Non-teaching staff, Student, Parent, Alumni | GegoK12 has exactly these groups; schools never edit permissions | part of SEC-0 |
| C8 | Resolve **duplicate concepts** that GegoK12 keeps single: `assessment.assignment` vs `lms.assignment`; `timetable.session` vs `lms.timetable_entry`/`timetable_template`; `lms.certificate` vs NM-9 certificates; `facility.asset_item` vs NM-4 inventory; `library.media` vs `platform.file_object` vs `lms.drive_item` for the media library | GegoK12 has one homework, one assignment, one timetable, one certificate concept | decide one owner per concept; the other becomes a view or link |
| C9 | **Default settings and seed values per tenant**: leave types (Earned, Casual, Sick, Maternity, Quarantine, Study), grade bands (CBSE scholastic and co-scholastic), absence reasons, attendance types (present, absent, late, half day), fee heads, pay categories and payroll items, feedback categories, page categories, notice backgrounds, SMS/e-mail templates | GegoK12 seeds all of these (GK:database/seeders) | platform import of a "school starter pack" |

### 6.2 Onboarding, demo and data import

- **First-run checklist** for a new tenant in the order GegoK12's settings imply: school profile -> academic year ->
  classes and sections -> subjects -> staff import -> student and guardian import -> fee setup -> timetable ->
  invite teachers and parents (QR code, G190) -> send a test SMS/push (`SendTestMessage`, needs ARC-2).
- **Demo tenant**: a seeded CBSE school with one year of data for every persona, reset nightly; `ClearDemoData`
  already lets a real school wipe the sample data after trying it.
- **Import templates**: every import screen offers "download format" (Excel) with validation preview, as GegoK12 does
  for teachers, holidays, library cards, promotions and members; add marks import (G089).
- **Migration service**: GegoK12 sells data migration for $199; MySMS could offer an importer from common Indian ERPs
  (to verify which formats are common).
- **Self-host option**: GegoK12's appeal is "free, self-hosted"; a Docker compose for a single school (monolith `app`
  + PostgreSQL + RabbitMQ) would answer that buyer question (G002).

### 6.3 Packaging and pricing ideas

- Map MySMS into **Core** (everything matching GegoK12's 26 free modules, including accounts, ID cards, calendar,
  notifications) and **add-ons** matching GegoK12's Pro list plus MySMS strengths: Exams and report cards, Fees with
  online payment, Timetable generator, Transport tracking, Inventory and stock, Certificates, Alumni, Video classes,
  LMS, Careers. That needs per-module entitlement on `saas_plan` (G006).
- Publish a "never charged" list (apps, users, data export, leaving) and a 3-year cost example; GegoK12 uses both to
  sell against per-student subscriptions.
- Consider a support-tier table (community / standard / priority) and setup and migration as fixed-price services.

### 6.4 Reports to ship by default (from GegoK12's report routes)

Students active / exited / suspended; parents list; birthdays and anniversaries; events; holidays; fee collection and
unpaid by class; attendance daily / monthly / defaulters; staff absentees; payroll unpaid and bank-transfer list;
library issued / overdue / top borrowed; inventory current stock, monthly purchases and sales; visitor log by day.
Register each as a `platform` report definition so CP-7 dashboards can reuse them.

---

## 7. Sources

GegoK12 website and docs (fetched 2026-11-19):
- https://gegok12.com/ ; https://gegok12.com/pricing/ ; https://gegok12.com/modules/ ; https://gegok12.com/school-software-demo/
- https://gegok12.com/modules/attendance-management/ ; /modules/exam-scheduling-and-result-generation/ ; /modules/transport-management/ ;
  /modules/timetable-generator/ ; /modules/reception-module/ ; /modules/inventory-management/ ; /modules/digital-certificate-generation/ ;
  /modules/emergency-notification/ ; /modules/facilities-management/ ; /modules/video-lecture-room/ ; /modules/alumni-management/ ;
  /modules/school-library-management/ ; /modules/magazine-and-journal-publishing/
- https://gegok12.com/school-parent-application/ ; https://gegok12.com/driver-app/
- https://docs.gegok12.com/documentation (table of contents) ; /documentation/getting-started ; /documentation/core-modules/settings ;
  /documentation/add-on-modules/exam-module ; /documentation/add-on-modules/fee-module

GegoK12 code (https://github.com/Gego-K12/gegok12, branch main, read via raw.githubusercontent.com and api.github.com contents):
- readme.md, api.md, .env.example, app/Providers/RouteServiceProvider.php
- routes/web.php, admin.php, setting.php, teacher.php, student.php, receptionist.php, accountant.php, payroll.php,
  librarian.php, superadmin.php, api.php, teacherapi.php, addon.php, inventory.php (empty), stock.php (empty)
- database/migrations (directory listing), database/seeders (listing), RolesTableSeeder.php, UsergroupTableSeeder.php, app/Models (listing)

Third-party listings used only for vendor background: g2.com, softwaresuggest.com, goodfirms.co, techjockey.com.

**Could not be read** (server errors, 404s or robots): GitHub tree pages (robots-blocked; used the contents API instead);
docs pages for student management, attendance, transport and account management (server error); gegok12.com pages
for fee management, teacher app, online quiz (server error) and communication, dashboard-and-reports, school-settings,
question-paper-generator (404); the recursive git tree (truncated); composer.json/package.json, controllers, views,
lang files and the Pro module repositories (private) were not read within the call budget. The Laravel version, the
language list in `lang/`, and the Pro modules' real behaviour are therefore **to verify**.

MySMS evidence: database/migrations/*/V*.sql, api/operations-status.md, ui/02-navigation-and-screens.md, ROADMAP.md
(§1, §3, §3.10-3.12, ARC-2, BE-1, SEC-0, SEC-11, D5), ARCHITECTURE.md §6, backend/finance (Razorpay), docs/product.
