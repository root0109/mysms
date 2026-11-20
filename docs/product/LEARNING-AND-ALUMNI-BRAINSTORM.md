# MySMS learning tools and alumni opportunities — consolidated brainstorm

Research date: 11 November 2026.

Status: product proposals and public-source research, not approved implementation specifications or claims of delivered functionality. Application code, API contracts and database migrations were not changed for this document.

## 1. Direction

Build useful experiences inside MySMS that people have a reason to revisit: teachers prepare and respond to work, students understand and plan their actual schoolwork, families see relevant progress, and alumni contribute opportunities through a durable school relationship.

The four strongest connected products are:

1. **Alumni opportunities:** a permanent-account alumni area with verified membership, job links, optional applications, referrals and useful contributions.
2. **Teacher AI workbench:** generate and adapt teaching material inside existing planning, assessment and reporting screens, with teacher review.
3. **Missed-lesson catch-up:** connect recorded absence and what was actually taught to a manageable, source-linked student queue.
4. **Homework coordination and personal planning:** calculate class workload before publication, coordinate changes between teachers and maintain a student-owned study plan.

These proposals fit the existing software domains. They do not depend on buying hardware, arranging inter-school transport, creating new school enterprises or operating another offline programme.

The public feature catalogues below preserve capabilities found in the requested references, including capabilities for which MySMS already has foundations. Existing capabilities are not counted again as new ideas. Ranking indicates comparative fit and dependency, not permission to remove any already agreed product scope.

## 2. How the two personas were reconciled

**Product Manager:** a feature needs a recognizable reason to return, a clear benefit to a specific role and useful behavior even before a large community forms. Alumni should find opportunities; teachers should avoid copying between tools; students should get help with the work their school actually assigned.

**Senior Developer:** every feature needs an authoritative owner, explicit access rules, manageable dependencies and a complete lifecycle. A generated paragraph, record table or dashboard is insufficient. The experience must handle corrections, withdrawal, retries, failed delivery and stale data.

**Combined decision:** extend existing domains and screens; keep original school records authoritative; create a shared teaching/learning context rather than another disconnected content or task store. Build AI as assistance to these workflows. Describe vendor claims separately from MySMS proposals and observed code.

## 3. Evidence and comparison labels

- **E — Existing narrow capability:** implementation and operation status support the stated function. This is source inspection, not a fresh production or runtime verification.
- **P — Partial:** useful records, APIs or screens exist; the compared end-to-end capability is incomplete or unverified.
- **N — New:** the reviewed source/status/schema does not establish the capability; new product behavior is required.
- **U — Unestablished:** a vendor claim, current availability or implementation detail could not be established from accessible public material.
- **D — Dependency:** a feature requires another unfinished shared capability or an external service. D can accompany P or N.

“Advertised” means the vendor describes a feature; it does not mean this review tested it, verified effectiveness or established all commercial entitlements. Public-site coverage cannot reveal every authenticated feature, customer-specific configuration or unreleased capability.

The code comparison uses the current checkout, including existing uncommitted work. Repository evidence is collected in section 10 so product proposals remain readable.

## 4. Permanent alumni accounts and an alumni job board

This develops the owner's idea. It does not claim that the three learning-tool references provide an alumni product.

### 4.1 The account promise

Keep the same account identity after graduation and offer a verified alumni relationship. A person can remain an alumnus while also holding an independently authorized teacher or guardian role.

“Permanent” means graduation does not arbitrarily expire the account or destroy the person's school connection. It does not preserve active-student privileges. Suspension, account recovery, voluntary closure, retention, school service closure and export still require defined behavior.

The current schema already recognizes alumni as an account routing type and student status. Neither is proof of a working alumni membership or authorization lifecycle.

### 4.2 Complete alumni/job-board feature set

#### Identity and membership

- **AL-01 — Same-account graduation transition.** Retain the IAM account and selected personal history; replace relevant student grants with independently verified alumni grants. Preserve unrelated legitimate roles. P/N; IAM and SIS.
- **AL-02 — Historic alumni claim.** Allow an older graduate to request membership using school/batch evidence, with duplicate detection and private manual review when records do not match. N; SIS membership with IAM identity.
- **AL-03 — Personal recovery contact.** Verify a usable personal contact before school-issued access ends; support changed names, lost addresses and account recovery without exposing another person's school history. P/N/D; IAM and delivery.
- **AL-04 — Durable school and batch membership.** Record institution, batch/year, verification basis, status and visibility preferences separately from current enrollment. N; SIS.
- **AL-05 — Alumni home and optional directory.** Show relevant opportunities, saved items and chosen school/batch updates. Directory participation and each contact field are opt-in. N; member-facing web/mobile.
- **AL-06 — Eligibility-aware access.** Handle graduates who are still minors, appropriate opportunity eligibility and simultaneous parent/teacher roles. Alumni membership alone grants no current-class or child-record access. N; scoped authorization.

#### Post, discover and manage opportunities

- **AL-07 — Short posting wizard.** Share a vacancy with title, organization, relevant location/work mode, opportunity type, closing date and an application method. Avoid requiring a company logo merely to share a link. P/N; careers.
- **AL-08 — External link or in-app application.** A posting explicitly selects its mode. External-link mode shows the destination and does not pretend MySMS received an employer application. In-app mode supports an authorized application and optional policy-defined attachments. N/D; careers and files.
- **AL-09 — Verified contribution attribution.** Show who shared an opportunity, their verified school relationship and the basis of any organization verification. Alumni verification is distinct from employer or vacancy verification. P/N; careers and membership projection.
- **AL-10 — Draft and publication lifecycle.** Support draft, submitted, approved/rejected, open, closed, expired and withdrawn/removed states with reasons and revision history. Review policy is institution-configurable; material edits can require re-review. P/N; careers.
- **AL-11 — Owner-controlled posts.** Derive poster identity from the authenticated account. A poster can edit or close their own authorized posts; moderators exercise separate scoped powers. Generic CRUD permission must not grant control of every posting. P/N; careers.
- **AL-12 — Search, filters and pagination.** Browse approved, eligible, open and unexpired opportunities by role, location, work mode, category and relevant batch/experience filters. Empty results and unavailable records have clear states. P/N; careers.
- **AL-13 — Save and subscribe.** Save vacancies and user-defined searches; opt into a digest or selected alerts; unsubscribe independently from essential account notices. N/D; careers and comms.
- **AL-14 — Expiry and stale-link handling.** Close outdated posts, let posters reconfirm, and accept broken-link reports. A successful URL response is not proof that an employer still accepts applications. N; careers.
- **AL-15 — Report, moderate and appeal.** Complete the report queue, investigation, removal/reinstatement and audit trail. Do not discard evidence when a posting disappears from discovery. P/N; careers.
- **AL-16 — Appropriate audience and sharing.** Support school/batch/approved-community audiences and shareable links whose visibility is explicit. A shared URL never conveys private application access or authentication tokens. N; careers.

#### Applications, referrals and continued contribution

- **AL-17 — Applicant-owned applications.** Derive applicant identity server-side; permit withdrawal and access to one's own history; provide explicitly authorized recruiter/moderator views and private attachment access. P/N/D; careers and files.
- **AL-18 — Honest application tracking.** Separate employer-confirmed statuses from a person's self-reported external application checklist. A link click is neither an application nor a hiring outcome. P/N; careers.
- **AL-19 — Referral offers.** A poster may separately offer a referral with conditions and a bounded request workflow; reveal contact details only through explicit choices. N; careers.
- **AL-20 — Scholarships and deadline links.** Use related opportunity types for verified scholarship, course and application announcements with source, eligibility, expiry and moderation. Preserve type-specific fields instead of calling everything a job. N; careers extension.
- **AL-21 — Ask a graduate.** Publish moderated, topic-tagged career/study questions to opted-in alumni. Answers become searchable school knowledge; student participation follows the school's age/access policy. N; comms, with verified alumni authorship.
- **AL-22 — Practical career stories.** Alumni voluntarily publish dated accounts of a course, application or career path, linked to relevant questions and opportunities. Authors choose visibility and can update or withdraw their contribution. N; comms.
- **AL-23 — Batch participation.** Offer opt-in batch updates, polls, RSVPs and approved archive contributions. Reuse community/event concepts rather than creating a second feed or event engine. P/N; comms.
- **AL-24 — Transparent member outcomes.** Show the school aggregate contribution, fresh-opportunity and report-resolution measures. Show individuals their own saved jobs, requests and applications; do not publicly rank job seekers or contributors. N; careers projections.

### 4.3 Why this can attract repeat use

Alumni get useful opportunities and a simple way to give back. Students can see relevant pathways through approved content. Teachers and staff can also discover eligible vacancies without exposing payroll or HR records. Schools gain an enduring relationship with graduates and evidence that their network provides value.

A useful jobs area does not require a full social network: fresh links, filters, saved items, ownership and a trustworthy publication process can supply value independently. The full job-board behavior above remains the definition of the proposed capability.

### 4.4 Current implementation gaps that matter

The careers module has opening/application/preference records and some state transitions. Public opening list/detail, reporting, applicant email and application-status operations are still marked 501. Its current job shape lacks external application links, review states, expiry and alumni membership/audience.

Generic poster/applicant/person identifiers are writable, and the inspected careers services use tenant-level scope. This is a concrete reason to design owner-specific endpoints and read filtering before granting alumni existing CRUD permissions. Current mandatory résumé/logo references also depend on unfinished file delivery.

## 5. The software-native learning experiences to add

These are MySMS proposals assembled from the research, not claims that every named vendor implements every behavior.

### 5.1 Teacher AI workbench

**Hook:** a teacher can work directly from the class, unit, material and assessment already in MySMS.

Provide contextual actions: draft a unit or lesson; generate questions, answer keys and rubrics; adapt reading difficulty or language; prepare slides/worksheets from authorized material; draft evidence-linked feedback and reports; translate school communications. Let teachers save, edit, compare and discard drafts, then publish through the existing owner workflow.

Keep source references, selected objectives, output language, version, teacher edits and generation status. Generated material is draft content. A provider outage leaves a recoverable request and access to existing material. Publishing must check the current source version and permissions.

**Fit:** P/N/D. Academics owns curriculum/lesson plans; assessment owns official homework, exams and results; LMS owns course teaching material and tutoring. Relative integration effort: medium for contextual text drafting, larger for scanned work, media generation and assessment-grade integration.

### 5.2 Teach me this lesson

**Hook:** a student asks for help from the exact authorized lesson they are studying.

Support a teacher-configured tutor with subject/objective boundaries, age-appropriate instructions, selected materials, worked examples, hints and practice. Students can say “explain differently” or “I still need help.” Teachers see authorized learning interactions and unresolved questions within declared visibility rules.

Offer bounded tutor modes such as practice, revision, language conversation and explanation. Use permitted source context and distinguish unsupported answers. Help settings differ for practice and active assessments; no silent access to private answer keys or unrestricted external tools.

**Fit:** N/D on P LMS content and identity. Relative effort: large; includes session history, access controls, age policy, model cost limits, evaluation and recovery from invalid output. A chat panel alone is incomplete.

### 5.3 What I missed

**Hook:** a returning student opens one queue explaining what was missed and the smallest useful next steps.

When attendance is completed, derive candidate missed lessons, reconcile them with the timetable and teacher-confirmed coverage, then prepare objective-specific work from approved material. Let the teacher prioritize, edit, skip or approve items. Show the student the reason each item appeared, expected effort, completion state and a request-help action.

Keep completed work separate from demonstrated understanding. Attendance correction, a cancelled lesson or a changed objective must update the proposal and history. Present students can receive explicitly teacher-assigned consolidation without an inferred “disengaged” label.

**Fit:** P/N/D. Attendance and timetable inputs exist; exact taught-objective links, recovery records, AI tasks and role-specific queues are new. Relative effort: medium-to-large because the join and correction lifecycle matter as much as generation.

### 5.4 What actually happened in the lesson

**Hook:** one short teacher confirmation improves planning, catch-up and revision together.

After a session, let the teacher confirm covered objectives, remove untaught objectives, link existing resources and add a brief clarification. Reuse this record for catch-up and the class resource page. Permit corrections with a revision reason; never equate scheduled content with taught content.

**Fit:** P/N. Lesson plans and free-text objectives exist; versioned session coverage is new. Relative effort: medium, and it is a shared prerequisite rather than a separate content-authoring system.

### 5.5 A class week that fits

**Hook:** teachers see the combined effect of homework before publishing it.

Add expected effort or a range to each relevant task. Combine official homework, LMS assignments and assessment preparation for the actual roster. Show work due, the assumed study window, overlapping commitments and a transparent overload warning. Let authorized teachers propose a date adjustment and record the accepted change through the task owner.

Count linked representations of one task once. Separate release time, due time, planned study time and exam date. Distinguish collective class-policy limits from individual availability and approved individual extensions.

**Fit:** P/N. Dates, assignments, rosters, timetable and settings exist; per-task effort, policy evaluation, workload projection and negotiation are new. Relative effort: medium, with substantial value before any generative AI is involved.

### 5.6 My next study step

**Hook:** students see when to do their real schoolwork and what to start next.

Import authorized tasks without re-entry, let students choose availability, add personal study tasks, set breaks and generate an explainable plan. Split longer work, respect release/deadline constraints, retain deliberately fixed blocks and replan unfinished work. Say clearly when work cannot fit.

Personal calendar details and actual-effort reports stay private unless the student deliberately shares a bounded view under applicable school/guardian policy. Planning failure does not become an automatic conduct or ability label.

**Fit:** N on P task/calendar records. Relative effort: medium-to-large. Prefer deterministic scheduling and explainable rules; an LLM does not decide authoritative deadlines.

### 5.7 Improve this homework

**Hook:** students get a concrete next action from the teacher's actual feedback.

Show the rubric, feedback and relevant material together; offer an explanation, targeted practice and an authorized resubmission path where the assignment allows it. A student can ask a specific question against the task. Teacher-reviewed clarification can help the whole permitted class.

Retain the original submission, feedback and marks. A new practice attempt or AI explanation never silently changes official results. Submission answers and attachments require real delivery support, not only a receipt.

**Fit:** P/N/D. Assessment submission/checking exists; contextual explanations, revision variants and complete rich submission support need work. Relative effort: medium; larger when automatic grading is included.

### 5.8 Estimate repair and deadline discussion

**Hook:** students can report that a task took much longer than expected, and see a useful response.

Collect optional effort bands and structured reasons; show teachers response counts, spread and missingness. Let teachers clarify instructions, revise future estimates or propose an authorized extension. Publish only appropriately aggregated class findings, with safeguards for very small groups.

This is a proposed extension, not an established StudyTask survey feature. Do not expose individual speed, rank students or infer disability, diligence or wellbeing from the reports.

**Fit:** N. Existing course feedback and guardian threads are not a workload-feedback model. Relative effort: medium.

### 5.9 Task-linked class questions and study circles

**Hook:** community activity helps finish a real task or resolve a specific misunderstanding.

Create authorized question threads attached to a lesson or assignment; allow teacher-approved answers, searchable clarification and student-selected study-circle invitations. Students share only chosen availability windows or tasks. Resolve a question into an explanation, resource or next step and keep its source context.

Reuse the existing community/forum directions and moderation model. Avoid a second general social network or a separate competing assignment store.

**Fit:** P/N/D. Forum/post/task records exist, but member-visible feeds, moderation, notifications and fine-grained audience controls remain incomplete. Relative effort: medium-to-large.

### 5.10 A family learning digest

**Hook:** a guardian sees the few actions relevant to their child: new work, an agreed deadline change, a catch-up item needing help and teacher-approved feedback.

Generate from authorized records with a source link for each statement. Allow preferred language, notification controls and a clear way to ask the responsible teacher. Aggregate all relevant task owners without exposing classmates, private student plans or alumni activity.

This composes the learning features and the already proposed school-community experience. It is not permission to send every draft, AI conversation or personal planning detail to families.

**Fit:** P/N/D. Child relationships and assessment views exist; aggregation, scope-safe summaries and actual delivery require work. Relative effort: medium, dependent on comms.

## 6. Ownership and integration shape

The following is a proposed allocation, not a schema or API change made by this brainstorm.

- **IAM:** account identity, verification, session/recovery behavior, grants and institution-scoped feature eligibility. An alumni routing label is never authority.
- **SIS:** verified alumni membership and its relationship to student history; current student/guardian links. Decide and document revocation and graduation events.
- **Careers:** opportunity drafts/publication, application mode, search, saved jobs, referral requests, reports, applications and moderation outcomes.
- **Academics:** curriculum units, structured objectives/standards and versioned taught-coverage records linked to lesson planning.
- **Assessment:** official homework, exam/result authority, assignment effort metadata, approved deadline changes and revision rules.
- **LMS:** course content, contextual AI drafts/tutors, practice/catch-up activity and study-plan behavior where it composes course work. Preserve explicit links to original assessment tasks.
- **Comms:** actual delivery, preferences/digests and reusable discussion/community experiences. Delivery state and read state are different.
- **Platform:** protected file upload, processing, download and long-running job support. File metadata alone does not make uploaded curriculum, scanned work or résumés usable.
- **People:** profile/contact/consent capabilities where those records are already owned. Alumni directory publication must use an explicit authorized profile projection.

Use existing Java/Spring module boundaries, local transactions, typed contracts and durable outbox/inbox processing. Do not add cross-schema SQL or a distributed transaction to connect these experiences. AI-provider adapters are technical capabilities, not automatically a new domain microservice.

The current dependency graph permits LMS calls to academics, assessment, comms, finance, IAM, master, org and platform; it does not currently permit LMS to synchronously call attendance, timetable or SIS. Automatic catch-up therefore needs reviewed events/projections or an explicitly reviewed contract/dependency change. This decision is part of design, not a convenient direct join.

The existing attendance-completed event includes sheet/date/absent-student identifiers; it does not itself provide the full session, roster, source-version and taught-objective context required by catch-up. Re-completion and late correction require a revision-aware reconciliation strategy. Deduplicating forever by sheet/student would suppress legitimate changes.

For alumni conversion, a transaction can update IAM's own grants/session state atomically. SIS membership changes and IAM changes require durable coordination, retries and an explicit pending state; do not claim one atomic transaction across both modules.

### 6.1 Candidate new domain concepts

Names below are illustrative and owned by the listed domain. They are not implemented tables or accepted schema:

- SIS: alumni membership, membership claim/review and institution/batch affiliation.
- Careers: opportunity revision, application destination/mode, publication review, saved opportunity/search, referral request, moderation report.
- Academics: structured learning objective/standard association and session coverage revision.
- LMS: source-grounded generation request, generated draft/review, tutor configuration/session, recovery item/attempt, study availability/block/plan revision.
- Assessment: task effort estimate, individual extension, deadline-change proposal and authoritative task link.
- Comms: authorized task-linked discussion and digest delivery reference.

Use typed fields and child records for governed facts. Source files and opaque generated documents use protected storage references. Keep model/provider, source revision, publication/teacher decision and job outcome distinct.

### 6.2 Shared dependency work

1. Complete file delivery for curriculum uploads, handwritten work, generated documents, audio and résumés; enforce ownership on all downloads and derivatives.
2. Complete scoped member-facing reads, notifications and outbound delivery rather than assuming the presence of record tables provides them.
3. Build a single generation execution mechanism with tenant budgets, limits, cancellation, retry, provenance and provider failure handling; owners still control publication.
4. Define source ingestion, content rights, versioning and retrieval boundaries. Treat uploaded instructions as source content, not commands that can override system policy or access controls.
5. Add structured objective/task links and current audience/role checks before automatic cross-domain personalization.
6. Evaluate exact-subject/language generation quality, factual errors and accessibility. Automated checks and teacher approval reduce risk; they do not establish guaranteed correctness.
7. Keep personal records out of model requests unless a specific authorized feature requires the minimum necessary fields. A curriculum-only worksheet generator and a student-aware interactive tutor have different data requirements.

## 7. Completeness and acceptance examples

These are behavior tests for the proposed capabilities, not a replacement for detailed implementation specifications.

### Alumni and jobs

- A graduate signs in with the same account after conversion, sees the alumni area and loses current-class authority. A simultaneous legitimate guardian role continues to work only in its own authorized context.
- An older graduate with a changed name can enter private review without exposing the existence or details of other school records.
- A poster cannot submit another user's poster ID, edit someone else's vacancy, read unrelated applications or use a generic list endpoint to bypass ownership filtering.
- An external-link post can be published without uploading a logo or résumé; its destination is explicit and no private profile data is sent in the link.
- An applicant can see/withdraw their own in-app application; a poster can access attachments only under the declared application authorization.
- Expired, rejected and removed postings disappear from discovery while appropriate history and audit remain. Reinstatement and material edits follow review policy.
- School A membership never exposes school B records. Underage membership and opportunity eligibility follow explicit policy; “alumnus” never silently means adult.

### Teaching AI and tutoring

- A teacher generates from an authorized unit, sees source links, edits the draft and publishes through the owner. Another teacher's private sources remain inaccessible.
- Source changes during generation mark the result stale for review. Retry does not duplicate published tasks or charge/count the same completed request twice.
- A provider timeout leaves a visible recoverable state; approved existing resources remain available.
- Student help respects the assignment's practice/assessment rules and cannot retrieve restricted answer keys, classmates' work or another institution's content.
- A generated assessment suggestion never changes a final mark without the authorized grading workflow.

### Catch-up

- A completed attendance sheet plus confirmed coverage creates the correct student's proposed items; daily absence without lesson-level evidence does not fabricate exact missed objectives.
- A cancelled session produces no missed-learning task. Reopened attendance, a corrected mark or revised coverage triggers reconciliation and preserves the reason for change.
- Teacher approval, resource revision and student completion retain provenance. “Completed,” “answered correctly” and “teacher confirmed understanding” remain separate.
- A student can flag a mismatch or ask for help. The family view is limited to the authorized child's published work.

### Workload and planning

- Two representations of one assignment count once; two genuinely different tasks remain separate.
- A newly published deadline/extension invalidates stale plans and updates only affected students after the owner accepts it.
- The planner shows an infeasible workload honestly. Fixed personal blocks, breaks, time zones and task-release windows remain respected.
- A teacher sees appropriate aggregated effort reports, not a student's private event titles or another student's plan.
- One student's individual extension does not change the whole class deadline; an approved class change does not erase individual history.

## 8. Relative fit and product choices

The most bounded extensions are external-link alumni postings, saved opportunities, contextual teacher drafting and per-task effort estimates. Each still needs complete access, lifecycle and exception behavior.

Cross-subject coordination, personal planning, structured taught coverage and automatic catch-up add more data modeling and reconciliation. Rich scanned-work feedback, open-ended student tutoring, accreditation programmes and broad external integrations have larger shared dependencies.

The strongest combined daily experience is: **teacher prepares and publishes once → student gets an accurate task and usable plan → questions/catch-up stay attached → teacher feedback is actionable → family sees the relevant next action.** The strongest long-term community experience is: **student graduates → keeps a verified account → finds and contributes opportunities → remains connected through useful content.**

No delivery dates, staffing costs, learning gains or market-exclusivity claims are established here. Comparative effort is qualitative and assumes the missing shared platform capabilities are completed.

## 9. Decisions to resolve before implementation

- Alumni conversion/claim evidence, opt-in behavior, batch affiliation, minor participation, recovery and school-closure/export behavior.
- Opportunity audiences, permitted job types, external/in-app modes, moderation authority, expiry and employer-verification evidence.
- AI features enabled per institution/role, approved providers, data locations/retention, spending limits, student visibility and teacher review policy.
- Supported curriculum/board objective structures, source ingestion rights and who confirms actual lesson coverage.
- Task-effort conventions, study windows, individual extensions, overload-policy authority and deadline-change approval.
- Default visibility of student plans, effort reports, tutor interactions and guardian summaries.
- Ownership of cross-domain study/recovery projections and reviewed additions to event/SDK dependency contracts.

These are concrete design inputs. They do not mean the researched capabilities have been dropped or that a partial workflow counts as complete.

## 10. Repository evidence and present gaps

### 10.1 Current source/status baseline

The [operation inventory](../../api/operations-status.md) reports 2,049 operations, 1,756 served and 293 returning 501. These are recorded build results, not tests rerun for this brainstorm.

- **RB-01 — Architecture and ownership:** [architecture](../../ARCHITECTURE.md), [Java rules](../../JAVA-CODING-RULES.md), [handoff](../../ROADMAP.md) and [product roles](../../ui/01-product-map.md). Existing domain boundaries, scoped permissions, local transactions and event infrastructure are foundations.
- **RB-02 — Alumni account hint:** [IAM baseline](../../database/migrations/iam/V1_030_001__iam_baseline.sql). Alumni user type is a routing hint, not authorization; account status/expiry/verification fields exist.
- **RB-03 — Graduation and portal accounts:** [student workflow](../../backend/sis/src/main/java/com/mysms/sis/business/StudentWorkflowService.java), [portal account service](../../backend/sis/src/main/java/com/mysms/sis/business/PortalAccountService.java). Exit statuses end placements; active-student account issuance is not alumni conversion.
- **RB-04 — Careers records:** [careers baseline](../../database/migrations/careers/V1_210_001__careers_baseline.sql), [opening controller](../../backend/careers/src/main/java/com/mysms/careers/controller/JobOpeningController.java), [opening service](../../backend/careers/src/main/java/com/mysms/careers/business/JobOpeningService.java), [candidate preferences](../../backend/careers/src/main/java/com/mysms/careers/business/CandidatePreferenceService.java). Opening/application/preference foundations exist. The public list/detail and several custom workflows remain 501; owner-specific behavior must be added.
- **RB-05 — Planning and curriculum:** [academics baseline](../../database/migrations/academics/V1_060_001__academics_baseline.sql), [LMS objective entity](../../backend/lms/src/main/java/com/mysms/lms/domain/LearningObjectiveEntity.java). Curriculum units and free-text objectives exist; they are not a structured standards/mastery graph or verified session-objective history.
- **RB-06 — Official homework and assessment:** [assessment baseline](../../database/migrations/assessment/V1_140_001__assessment_baseline.sql), [homework service](../../backend/assessment/src/main/java/com/mysms/assessment/business/HomeworkService.java), [status](../../ROADMAP.md). Publication/review, submission receipts, checking/replies, exams, results and marksheet flows provide substantial foundations. Homework lacks answer-text support; files depend on unfinished platform delivery.
- **RB-07 — LMS content and assessment foundations:** [LMS baseline](../../database/migrations/lms/V1_150_001__lms_baseline.sql), [quiz controller](../../backend/lms/src/main/java/com/mysms/lms/controller/QuizController.java), [operation inventory](../../api/operations-status.md). Course/chapter/content, question bank, assignments, rubrics, grading records and alerts exist in varying maturity. Start/submit/save quiz attempts, aggregate gradebook and student/teacher dashboards remain 501.
- **RB-08 — Attendance and timetable:** [attendance events](../../backend/attendance/src/main/java/com/mysms/attendance/events/AttendanceEvents.java), [session service](../../backend/timetable/src/main/java/com/mysms/timetable/business/SessionService.java). Completed sheets and published session views are useful inputs. Session generation/batch confirmation are pending; absent IDs are not exact taught-objective evidence.
- **RB-09 — Workload and calendar:** assessment homework has dates but no fine-grained effort/planning model. LMS course workload hours are coarse metadata. Comms tasks and course feedback are different concepts. The operation inventory marks personal calendar/export and several member-facing reads as 501.
- **RB-10 — Shared service dependencies:** file URL/upload completion, in-app notification reads, outbound SendMessage, consent workflows and role switching include 501 operations. Do not promise uploaded-source AI, notification-driven engagement, external SSO or complete role switching merely because tables/contracts exist.
- **RB-11 — Existing related proposals:** [enhancements](../../ROADMAP.md) already proposes structured standards, mastery reporting, SSO, offline support and verifiable credentials. [community brainstorm](SCHOOL-COMMUNITY-NETWORK-BRAINSTORM.md) already covers community membership, alumni spaces, portfolios, editorial publication and supervised opportunities. This document develops software behavior and avoids counting those headings as newly discovered inventions.

### 10.2 Reuse without duplicating authority

Keep assessment homework and LMS assignments as distinct owning concepts. A planner or community thread stores a typed source link and source version, not a second mutable copy of marks, deadlines or submissions. Profile, identity, membership, vacancy and employment verification are separate facts.

The source scan did not establish an existing AI generation/provider execution layer or the specific workload/recovery models proposed here. This is a scoped finding from reviewed code and dependencies, not a claim that every branch or future change has been inspected.

## 11. Toddle — public feature catalogue and MySMS comparison

The [Toddle homepage](https://www.toddleapp.com/) provided the product navigation. The inventory below covers the accessible public product pages and selected official help articles, not an authenticated account or every article in the Help Center. Entries describe advertised capability groups; the text following each status describes MySMS's gap or reuse opportunity.

### 11.1 Curriculum planning

Source: [curriculum planning](https://www.toddleapp.com/product/curriculum-planning/). MySMS comparison: RB-05 and RB-07.

- **TD-001 — Visual curriculum maps. N.** Add a linked course/unit/standard/grade view; existing lists do not provide this experience.
- **TD-002 — Coordinator blueprints and teacher plans. P.** Extend course/lesson records with explicit reusable planning structures and ownership.
- **TD-003 — Configurable, interdisciplinary planning templates. P/N.** Add template fields, permitted variation and multi-subject links.
- **TD-004 — Supplied and school-uploaded standards libraries. N/D.** Add governed framework/version imports and objective mappings.
- **TD-005 — Coverage, pacing and curriculum gaps. N.** Derive from explicit mappings and confirmed coverage, with appropriately scoped aggregation.
- **TD-006 — Simultaneous collaborative planning. N.** Requires collaboration/conflict behavior beyond ordinary record updates.
- **TD-007 — Public curriculum website. N.** Add a separately approved public projection; private planning is not automatically publishable.
- **TD-008 — Shared unit library and adaptation. P.** Course/content foundations exist; discovery, import lineage and adaptation workflows need completion.

### 11.2 Teacher AI

Source: [Toddle AI](https://www.toddleapp.com/ai/). MySMS comparison: RB-05, RB-06 and RB-07.

- **TD-009 — Course, unit and lesson generation. N.** Add source-grounded drafting inside the appropriate authoring owner.
- **TD-010 — Scaffolds, extensions and alternative lesson entry points. N.** Store explicit teacher-selected adaptation and review.
- **TD-011 — Rubric/checklist generation. N on P rubrics.** Generated drafts must become ordinary versioned assessment material.
- **TD-012 — Assessment generation from supplied media/links. N/D.** Add ingestion, rights, source attribution and review.
- **TD-013 — Difficulty, language and contextual adaptation. N.** Preserve original criteria and distinguish a variant from an official published change.
- **TD-014 — Worksheet-to-assignment conversion. N/D.** Add recognition, extraction, editable structure and answer-key verification.
- **TD-015 — Criteria-based AI grading with explanation. N.** Suggestions enter an authorized teacher grading workflow.
- **TD-016 — Handwritten/document/image grading inputs. N/D.** Rich-file processing and faithful evidence extraction are unfinished dependencies.
- **TD-017 — Captions, reply drafts and conversation summaries. N.** Limit each generated artifact to its source audience and editable draft state.
- **TD-018 — Evidence-linked report comments, style and translation. N.** Link claims to authorized evidence and retain teacher approval.

Promotional time-saving statements were not adopted as MySMS guarantees. Multimodal generation and text-only drafting have different implementation effort.

### 11.3 AI tutors

Source: [AI tutors and its FAQ](https://www.toddleapp.com/ai-tutors/). MySMS comparison: RB-07 and RB-10.

- **TD-019 — Teacher-defined tutor objectives. N.** New tutor configuration and assignment lifecycle.
- **TD-020 — Assistance, language and speech settings. N/D.** Separate educational settings from model/provider capability.
- **TD-021 — Teacher-supplied resource grounding. N/D.** Retrieve only permitted source versions.
- **TD-022 — Assigned adaptive tutor activity. N.** New student sessions, attempts and teacher-visible learning context.
- **TD-023 — Text, speech, uploads and equations. N/D.** Requires input-specific clients, processing and access rules.
- **TD-024 — Subject-specific dialogue modes. N.** Support configured mathematical, scientific, literary, coding and language practice.
- **TD-025 — Teacher conversation visibility and content flags. N.** Define permitted readers, review queues and retention.
- **TD-026 — Individual/class reasoning analysis. N.** Any inferred insight needs evidence and uncertainty; it is not a final assessment.

Published limitations include no code execution, teacher-supplied school context and different handling of uploaded versus linked images. Do not infer a coding sandbox or access to every school record. Advertised language breadth does not prove equal quality in each subject/language.

### 11.4 Assessment and gradebook

Source: [assessments and gradebook](https://www.toddleapp.com/product/assessments-gradebook/). MySMS comparison: RB-06 and RB-07.

- **TD-027 — Numeric/letter grading. E/P.** Official grading/result flows exist; complete LMS gradebook parity remains unfinished.
- **TD-028 — Standards-based mastery assessment. N.** Add structured evidence and rules distinct from numeric marks.
- **TD-029 — Mixed question/task formats. P.** Existing question variants need working student delivery and appropriate response interfaces.
- **TD-030 — Rubric/checklist evaluation. P.** Reuse records while completing assignment-linked evaluation.
- **TD-031 — Cloud-document, sketch, audio/video submissions. P/N/D.** Submission rows do not supply rich capture, file access or external adapters.
- **TD-032 — Inline and multimodal feedback. P/N/D.** Text foundations exist; annotations and media feedback are new.
- **TD-033 — Individual, group and multi-class allocation. P.** Complete distribution and exception handling around existing allocation/group structures.
- **TD-034 — Timed/focus-mode assessment and extra-time controls. P/N.** Configure and deliver a complete attempt experience; browser signals are not proof of misconduct.
- **TD-035 — Unified internal/external gradebook. P/D.** Aggregate gradebook and external import/bridge workflows require completion.
- **TD-036 — Reusable school assessment library. P.** Build discovery, copying, version provenance and adaptation on question-bank/content foundations.

### 11.5 Student portfolios

Source: [student portfolios](https://www.toddleapp.com/product/student-portfolios/). MySMS comparison: RB-07, RB-10 and RB-11.

- **TD-037 — Student- and teacher-contributed evidence. N.** Add a portfolio workflow, not another public-profile field.
- **TD-038 — Media, reflection and work-sample entries. N/D.** Reuse protected media with attributable authorship.
- **TD-039 — Outcome-tagged evidence and assessment. N.** Link evidence without conflating it with official grades.
- **TD-040 — Private/student/family sharing choices. N.** Define portfolio-specific audience and revocation.
- **TD-041 — Evidence in progress reports. N.** Add reviewed report references with stable versions.
- **TD-042 — Continuity across school years. N.** Preserve selected evidence and apply current access at year rollover.
- **TD-043 — Creative workbook activities. N.** Add structured student creation/response tools where justified.

Expand the existing MySMS portfolio proposal under one owner. These sources do not establish permanent alumni accounts.

### 11.6 Reports and transcripts

Source: [progress reports and transcripts](https://www.toddleapp.com/product/progress-reports-transcripts/). MySMS comparison: RB-06 and RB-11.

- **TD-044 — Configurable report sections, fields and periods. P.** Result templates exist; broader visual report composition is unestablished.
- **TD-045 — School comment banks. N.** Add governed, reusable comments with teacher adaptation.
- **TD-046 — Student reflection inside reports. N.** Add attributable reflections with publication rules.
- **TD-047 — Combined grades, attendance, behavior and media. P/N.** Extend official report cards through authorized evidence projections.
- **TD-048 — Bulk digital/print family publication. P/D.** Complete rendered documents and delivery around published report-card foundations.
- **TD-049 — Central transcript templates. P/N.** Existing result/GPA data is not a transcript designer.
- **TD-050 — Automatic multi-term transcript population. P.** Define term inclusion, corrections and historical calculation provenance.
- **TD-051 — Previous-school/repeated-year grade import. N.** New provenance-aware import and reconciliation.
- **TD-052 — Live preview and individual/bulk PDFs. N/D.** New transcript rendering, authorized downloads and batch outcomes.

### 11.7 Communication and family experience

Source: [communications hub](https://www.toddleapp.com/product/communications-hub/). MySMS comparison: RB-10 and RB-11.

- **TD-053 — Teacher/student/family direct messaging. P.** Complete conversation access, delivery and client behavior.
- **TD-054 — Class, subject and interest channels. P/N.** Add explicit membership and a usable channel experience.
- **TD-055 — Targeted in-app/email announcements. P/D.** Notice/audience foundations need working delivery and receipts.
- **TD-056 — Two-way family-message translation. N.** Preserve originals and make translation provenance clear.
- **TD-057 — Administrative thread oversight and flags. N.** Define purpose-bound access rather than blanket administrator reading.
- **TD-058 — Unified family app for learning and school information. P.** Existing clients/guardian links need completed member experiences.

### 11.8 Class operations

Source: [class operations](https://www.toddleapp.com/product/class-operations/). MySMS comparison: RB-08, RB-09 and RB-10.

- **TD-059 — Attendance codes, calculations and marking policies. P.** Capture exists; equivalent policy/reminder configuration needs design.
- **TD-060 — Daily/period/AM-PM and activity reconciliation. E/P/N.** Session capture exists; broader attendance reconciliation is additional.
- **TD-061 — Personal timetable and attendance entry from it. E/P.** Reuse the working timetable/capture path; provider schedule import is separate.
- **TD-062 — Weekly, multiweek and alternating cycles. P/U.** Verify supported variants rather than assuming session records deliver every cycle.
- **TD-063 — Substitutions, room changes and notifications. P/N/D.** Session editing exists; coordinated change delivery is incomplete.
- **TD-064 — Combined calendar and workload balancing. P/N.** Build a current cross-owner projection and workload rules.
- **TD-065 — Calendar synchronization and schedule/absence alerts. P/D.** Existing records/preferences are not working sync or delivery.
- **TD-066 — Family attendance and excusal experience. P.** Complete scoped family views and the full request/status journey.

Kiosk and two-way SIS connections are integration references, not prerequisites for native MySMS teaching tools.

### 11.9 Analytics and behavior

Sources: [data analytics](https://www.toddleapp.com/product/data-analytics/) and [behavior management](https://www.toddleapp.com/product/behavior-management/). MySMS comparison: RB-06, RB-08 and RB-10.

- **TD-067 — Cross-domain school dashboards. E/P/N.** Attendance summaries exist; other cohesive views and approved joins remain additional.
- **TD-068 — Plain-language analytical questions and visual answers. N.** Use authorized read models and explain the data behind answers.
- **TD-069 — Attendance/behavior/attainment patterns. N.** Associations require context and cannot establish causes.
- **TD-070 — Scheduled insight delivery. N/D.** Add scoped subscriptions, generation and delivery outcomes.
- **TD-071 — Configurable behavior categories and responses. P.** Discipline foundations exist; generalized configuration is additional.
- **TD-072 — Behavior points, ranges and resets. N.** A separate product-policy decision; do not introduce public child rankings.
- **TD-073 — AI incident writing/history summaries. N.** Restrict sensitive source access and require authorized review.
- **TD-074 — Sensitive-record/family visibility controls. P/U.** Existing institution/guardian controls need record-level parity review.

Behavior-point and automated cross-domain insight features are reference capabilities, not recommended community hooks by default.

### 11.10 Accreditation

Source: [official product overview](https://www.toddleapp.com/product/product-overview/). MySMS comparison: RB-01 and RB-05.

- **TD-075 — Accreditation and custom frameworks. N.** Existing institution accreditation metadata is a different capability.
- **TD-076 — Evidence tagging across learning artifacts. N.** Add purpose-specific evidence links and current entitlement.
- **TD-077 — Framework-organized evidence dashboard. N.** New review/status workflow.
- **TD-078 — Visiting-reviewer access and feedback. N.** New limited external-reviewer lifecycle.

The dedicated accreditation page timed out during research; the overview supplied these claims. Different pages gave different framework counts, so no count is asserted.

### 11.11 Integration catalogue

Source: [integrations](https://www.toddleapp.com/integrations/). MySMS comparison: RB-07 and RB-10. These are advertised connections; exact provider capability and entitlement can vary.

- **TD-079 — Google/Microsoft identity, files and calendars. P/N/D.** MySMS settings/proposals do not establish working adapters.
- **TD-080 — SIS roster, timetable, attendance and grade exchange. N/D.** Provider-specific contracts and reconciliation are separate work.
- **TD-081 — Rostering/identity intermediaries. N/D.** Catalogue examples include Clever, ClassLink, Wonde and Edlink.
- **TD-082 — External learning/content tools. P/D.** Existing external-tool records need real launch, identity and result handling.
- **TD-083 — Originality checking. P/D.** Plagiarism records exist; submission/report/webhook execution remains unfinished.
- **TD-084 — Meeting-provider connections. P/D.** Live-class records do not establish working Zoom/Meet/Teams adapters.

These features are retained as reference scope. Native MySMS adoption does not require integration with every listed vendor.

### 11.12 Onboarding and device use

Sources: [family invitations](https://help.toddleapp.com/en/articles/8611834-how-to-invite-family-members-on-toddle-as-an-educator), [platform/device/file support](https://help.toddleapp.com/en/articles/16556083-platforms-devices-and-file-support-on-toddle), [Toddle Academy](https://help.toddleapp.com/en/articles/8897887-how-do-i-use-toddle-academy). MySMS comparison: RB-03, RB-10 and RB-11.

- **TD-085 — Bulk/individual multilingual invitations and tracking. N/D.** Add complete invitation delivery and state.
- **TD-086 — Approved family joins and printable invitation codes. N.** Require verified child relationships and revocable invitation handling.
- **TD-087 — Multi-child, year-spanning family links and removal/reset. P.** Reuse guardian records; complete invitation/recovery UX.
- **TD-088 — Role apps and in-product media capture. P/N/D.** MySMS app foundations exist; equivalent capture experiences are new.
- **TD-089 — Role-specific product training and practice. N.** An optional in-product academy with progress and assessments is a new adoption capability.

The help articles describe platform-specific limits; native apps, desktop browsers and mobile browsers do not necessarily support identical creation tools.

## 12. Noticed — public feature catalogue and MySMS comparison

Sources reviewed: [FAQ](https://noticedtechnology.com/faq), [homepage](https://noticedtechnology.com/) and [about](https://noticedtechnology.com/about). The following uses short claim labels; the implementation/gap descriptions are MySMS analysis.

### 12.1 Learning and recovery

FAQ-derived claims:

- **NO-001 — Attendance input. E/P.** RB-08 supplies marks/events; derive candidate work rather than declaring a knowledge gap.
- **NO-002 — Timetable-to-curriculum mapping. P/N.** RB-05/RB-08 need explicit session/objective/version links.
- **NO-003 — Objective-specific worksheets. N/D.** New generation/review using approved sources and working file processing.
- **NO-004 — School curriculum ingestion. P/N/D.** Free-text plans do not provide source parsing, lineage or retrieval.
- **NO-005 — Curriculum/tier boundaries. N.** Add explicit constraints and review of out-of-scope output.
- **NO-006 — Automated drift validation. N.** New validation result; do not label it a correctness guarantee.
- **NO-007 — Resources for all students. P/D.** Complete authorized lesson-resource access around existing content.
- **NO-008 — Present-student consolidation. N/U.** The vendor does not explain detection of in-lesson missed learning; MySMS should use explicit teacher/student evidence.
- **NO-009 — Student portal notification. P/N/D.** Member queues and real delivery are incomplete.
- **NO-010 — Responses/completion tracking. P/N.** Existing receipts need rich activity/response support; completion is not mastery.
- **NO-011 — Teacher control. N/U.** Exact vendor edit/review controls are unspecified; MySMS proposes explicit controls.
- **NO-012 — Cohort/school reports. P/N.** Add recovery projections instead of treating attendance summaries as equivalent.
- **NO-013 — Objective/work/completion evidence. N.** New provenance-linked reporting; no assertion of inspection acceptance.

Further claims from the [homepage](https://noticedtechnology.com/):

- **NO-014 — Cross-subject catch-up path. P/N.** Add one authorized queue without copying original assignments.
- **NO-015 — Interactive/adaptive tasks. N/U.** Public descriptions do not establish the adaptation/marking algorithm.
- **NO-016 — Differentiated resources. N/U.** Detailed controls and accessibility conformance are unestablished.
- **NO-017 — Whole-school recovery visibility. N.** Add role-safe aggregate read models with correction history.

Further claims from [about](https://noticedtechnology.com/about):

- **NO-018 — End-of-lesson task creation. N/U.** Reconcile lifecycle triggers; the full vendor timing behavior is unspecified.
- **NO-019 — Teacher analytics portal. P/N.** Existing assessment/attendance screens need the recovery context.
- **NO-020 — Parent progress visibility. P/N.** Add only the authorized child's published recovery items.
- **NO-021 — Automated processing pipeline. P/N.** MySMS events exist; generation, delivery and revision-aware reconciliation are new.

### 12.2 Access, integration and operating model

Source: [FAQ](https://noticedtechnology.com/faq). These items are distinct operating claims; they are not evidence of MySMS certification or integration.

- **NO-022 — Read-only MIS synchronization. N/D.** Vendor names SIMS, Arbor and Bromcom; MySMS can use internal records without those connectors.
- **NO-023 — Google/Microsoft SSO and role views. P/N/D.** Local roles exist; federated SSO is not established.
- **NO-024 — Managed onboarding and validation. P/N.** Curriculum/import quality checks require their own workflow.
- **NO-025 — Data minimization and AI separation. N/U.** Vendor says personal student data stays out of AI requests; MySMS needs its own feature-specific data design.
- **NO-026 — Security/residency/export terms. U.** Vendor describes UK hosting, encryption, certification and contractual disposition; no equivalent MySMS assurance is inferred.
- **NO-027 — Scope-based quote pricing. U.** Commercial context, not a feature to implement or a public price.
- **NO-028 — Founding-cohort availability. U.** Vendor describes supported onboarding; this is not measured effectiveness.

### 12.3 Research limits

The [case-studies page](https://noticedtechnology.com/case-studies) said “Coming soon.” The [help page](https://noticedtechnology.com/help) provided contact support; the [documents page](https://noticedtechnology.com/docs) supplied data-sharing material rather than a full operational manual. No authenticated trial was conducted.

Public material did not establish exact present-student signals, teacher editing/approval detail, adaptation logic, marking rules or measured learning outcomes. Those are unknowns, not assertions that the product lacks them.

For MySMS, the important new capability is the verified chain from attendance and actual coverage to selected work, a student response and a teacher-visible outcome. It must not infer understanding from presence, absence or a completion tick.

## 13. StudyTask and related Studeam — public catalogue and MySMS comparison

### 13.1 Currency and product boundary

The requested [StudyTask page](https://taskntime.org/en/studytask/) explicitly labels the site outdated and points to [studeam.com](https://studeam.com/), which could not be fetched during research. Therefore this is a **legacy advertised feature inventory**, not verified current availability.

StudyTask coordinates institution workload. Studeam is the related individual planner, described as complementary on the [official audience page](https://taskntime.org/para-quien-2/). They are kept separate below. Related pages are included because they explain the individual-planning capability linked by the requested source.

### 13.2 StudyTask: institutional workload

Source: [StudyTask product page](https://taskntime.org/en/studytask/). MySMS comparison: RB-06, RB-08 and RB-09.

- **ST-001 — Daily workload estimates. N.** Add per-task effort and an explainable projection.
- **ST-002 — Homework plus out-of-school study. P/N.** Preserve task types and avoid assuming course hours equal assigned study.
- **ST-003 — Cross-subject teacher coordination. N.** Shared roster-aware planning is new.
- **ST-004 — Quantity and distribution controls. N.** Add policy settings and visible assumptions.
- **ST-005 — Overload diagnosis. N.** Implement transparent constraint warnings, not behavioral diagnosis.
- **ST-006 — Reference-student model with breaks. N.** A policy model is distinct from a personal student plan.
- **ST-007 — Reasonable-time comparison. N.** School-configured budgets require explicit authority and exceptions.
- **ST-008 — Immediate contribution warnings. N.** Evaluate a proposed task before publication.
- **ST-009 — Affected days and excess time. N.** Explain the calculation and uncertain estimates.
- **ST-010 — Attribution to added workload. N.** Use for coordination, not automatic teacher-performance scoring.
- **ST-011 — Teacher/supervisor web access. P/N.** Existing roles/clients need this specific shared view.
- **ST-012 — API embedding claims. N/D/U.** No reviewed public API specification establishes an implementable connector.
- **ST-013 — Fast task-entry claim. P/U.** MySMS can extend existing authoring; the vendor's advertised entry time is not a benchmark result.
- **ST-014 — School and university variants. P/N.** Domain breadth exists; workload rules must respect each calendar/roster model.
- **ST-015 — Demo availability without public pricing. U.** Commercial context, not a delivered MySMS capability.

Additional variants from the [official audience/integration page](https://taskntime.org/para-quien-2/):

- **ST-016 — Projects and exam revision included. P/N.** Add typed planning sources rather than counting due assignments alone.
- **ST-017 — Workload measured in minutes. N.** Add units, estimation source and ranges.
- **ST-018 — Agreed overall limits. N.** Version policy and define override/approval behavior.
- **ST-019 — Slower-student estimation convention. N.** Treat as an explicit policy choice, not an inferred label on individual children.
- **ST-020 — Institution-specific criteria. P/N.** Settings infrastructure exists; workload policy semantics are new.
- **ST-021 — Teacher advising visualization. N.** Add a view with student-selected disclosure of personal constraints.
- **ST-022 — University credit-workload use. P/N/U.** Coarse course workload exists; do not claim validated ECTS compliance.
- **ST-023 — Existing LMS/management embedding. N/D/U.** Provider access and synchronization behavior are unestablished.
- **ST-024 — Combined institutional/personal planning. N.** Keep class policy and each student's private plan separate.
- **ST-025 — Enrollment/retention applications. U.** Public wording is aspirational; do not count this as proven recommendation software.

### 13.3 Studeam: related individual planner

Source: [Studeam features](https://taskntime.org/en/features/). MySMS comparison: RB-09.

- **ST-026 — Dynamic tasks needing attention. P/N.** Aggregate source tasks with precise next-action state.
- **ST-027 — Regular and extra study availability. N.** New private availability model.
- **ST-028 — Multi-session task splitting. N.** Respect release/deadline, effort and session-length rules.
- **ST-029 — Interrupting calendar events. P/N.** Imported/entered events need a usable personal calendar; synchronization is not established.
- **ST-030 — Occasional extra sessions. N.** Preserve student control over exceptional availability.
- **ST-031 — Day, seven-day and month views. P/N.** Existing calendars do not prove the new study-plan views.
- **ST-032 — Phone/large-screen layouts. P/N.** Reuse client kits and add responsive planning interactions.
- **ST-033 — Insufficient-time warnings. N.** Name the unschedulable task and assumptions.
- **ST-034 — Browser use across devices. P.** MySMS client infrastructure exists; this feature's device parity needs delivery.
- **ST-035 — App-style/desktop installation. P/U.** A separate client/distribution requirement, not planning logic.
- **ST-036 — Institutional planner compatibility. N/U.** Vendor compatibility is a claim; MySMS needs its own shared-source model.

Source: [Studeam product page](https://taskntime.org/en/studeam-4/).

- **ST-037 — One-action schedule proposal. N.** New scheduling command with explainable output.
- **ST-038 — Start/end times and breaks. N.** Add study blocks and explicit time-zone handling.
- **ST-039 — Editing and repeated replanning. N.** Preserve chosen blocks and plan revision history.
- **ST-040 — Break, subject and topic settings. P/N.** Existing academic records are not private planner preferences.
- **ST-041 — Effort and deadline inputs. P/N.** Due dates exist; per-task effort does not.
- **ST-042 — Revised estimates during replanning. N.** Record original and remaining effort separately.
- **ST-043 — Actual-versus-estimated time. N.** Use optional self-report, with private defaults and uncertainty.
- **ST-044 — Plan review with an invited adult. N.** Define a bounded sharing grant; do not expose the whole calendar automatically.
- **ST-045 — Shared account across web/apps. E/P/U.** MySMS uses common identity; vendor's present app availability was not tested.
- **ST-046 — Free/ad-free claim. U.** Legacy Studeam commercial claim; not StudyTask pricing or a MySMS commercial decision.

### 13.4 Historical sources and unestablished features

The [planning-method explanation](https://taskntime.org/2021/01/21/planificacion-optima-del-estudio/) describes prioritization, distributed preparation and breaks. Its use of “optimal” is not proof of mathematical optimality or educational impact.

The [2020 product history](https://taskntime.org/2020/06/30/studeam-primer-planificador-app-estudio/) distinguishes the institutional reference model from a plan a real student should follow. The [2019 eThink announcement](https://taskntime.org/en/2019/03/14/ethink-education-partners-with-tasktime-a-time-management-and-workload-solution-provider/) is historical partnership evidence, not a verified maintained Moodle/Totara integration.

The [2021 Android announcement](https://taskntime.org/en/2021/10/18/studeam-is-now-on-android/) and [legacy terms](https://taskntime.org/en/terms-and-conditions/) describe historical access/account behavior. They do not establish current availability or suitable MySMS policy.

Reviewed public sources did not establish a maintained native Google Classroom connector, public API contracts, calendar synchronization/export, institutional report exports, detailed permissions, offline behavior or accessibility conformance. English/Spanish marketing pages do not prove a multilingual product.

Student workload pulses, deadline negotiation, task-linked study circles and estimate repair in section 5 are **MySMS proposals**, not established StudyTask features.

## 14. Consolidation, coverage and cleanup

The source catalogue retains 89 Toddle entries, 28 Noticed entries and 46 StudyTask/related-Studeam entries. Some entries are commercial context or unestablished claims, explicitly marked U; they are not all newly missing MySMS features. The alumni section contains 24 owner-inspired capability proposals.

Repeated concepts are reconciled into the ten MySMS learning experiences in section 5 and the single alumni/jobs area in section 4. Source IDs preserve distinct variants without creating competing owners for assignments, profiles, portfolios, calendars or feeds.

Four independent research contributions were used: GPT-6 Astra for Toddle, GPT-6.1 Terra for Noticed, GPT-6.1 Sol for StudyTask/Studeam, and GPT-6 Sol for alumni/jobs. Each was tasked with the Product Manager and Senior Developer personas. GPT-6 Luna independently reviewed the consolidated document against the source/status evidence and found no actionable corrections. The primary agent reviewed the combined fit, checked source/code details and authored this consolidated file.

Documentation checks passed: catalogue counts and ID sequences, duplicate-ID detection, local file links, empty links, unfinished-marker detection and trailing whitespace. No application builds or runtime tests were run for this documentation-only change.

Research cleanup means one deliverable file, deduplicated concepts, no per-model reports, no downloaded website copies and no temporary research scripts. Existing user changes and earlier brainstorms are preserved. Remaining work is product decisions and detailed implementation/acceptance design followed by implementation and verification; this document does not claim those are complete.
