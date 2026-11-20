# MySMS School Community Network — product brainstorm and requirements

**Date:** 11 November 2026 · **Status:** proposal for discussion; no implementation authorized by this document · **Launch focus:** Indian schools, followed by country-specific expansion.

**Deliverable:** a consolidated product and engineering proposal grounded in the current MySMS codebase and primary-source research.

**Navigation:**

- [Recommendation](#1-recommendation-and-scope), [product goals](#2-product-goals-and-boundaries), [repository baseline](#3-what-the-codebase-already-supports), [product/business options](#4-product-and-business-options).
- [People](#5-people-jobs-and-role-boundaries), [network policy](#6-network-topology-and-authorization-requirements), [membership](#7-membership-onboarding-and-lifecycle), [home](#8-login-home-and-navigation-requirements).
- [Posts/interactions](#9-content-creation-and-interaction-catalog), [stories/media](#10-moments-stories-media-and-memories), [school use cases](#11-school-use-cases-and-domain-boundaries), [LMS](#12-lms-integration-without-loss-of-learning-boundaries).
- [Messaging](#13-messaging-and-relationship-discovery), [alumni](#14-alumni-network-requirements), [external sharing](#15-external-sharing-four-distinct-products), [consent/India rules](#16-consent-permissions-and-privacy-requirements).
- [Safety](#17-safety-moderation-and-appeals), [admin controls](#18-administrator-settings-and-governance-catalog), [notifications/accessibility](#19-notifications-delivery-search-and-accessibility), [global expansion](#20-international-expansion-strategy).
- [Technical design](#21-proposed-technical-design-within-mysms-boundaries), [nonfunctional requirements](#22-security-reliability-and-nonfunctional-requirements), [costs/metrics](#23-sizing-costs-and-sustainable-pricing), [rollout](#24-delivery-sequence-and-release-gates).
- [Acceptance scenarios](#25-acceptance-and-verification-scenarios), [exclusions](#26-alternatives-exclusions-and-avoidable-traps), [distinctive ideas](#27-distinctive-opportunities-worth-testing), [open decisions](#28-open-decisions-and-customer-discovery-backlog).
- [Sources](#29-market-reference-points-and-source-notes), [product decisions and validation](#30-product-decisions-and-validation).

## 1. Recommendation and scope

Build a **verified school community integrated into the MySMS home**, with short posts, photos, school moments, moderated discussion and a separate alumni network. Start with useful school/class updates and bounded student participation. Extend toward clubs, chain collaboration, portfolios and supervised alumni opportunities when schools demonstrate that they can operate the community well.

Borrow the useful interaction patterns from Snapchat and Twitter/X: quick capture, concise updates, stories, replies, topic spaces and timely conversation. Give them school-specific rules: verified relationships, visible audiences, age-sensitive privileges, school ownership, human moderation, accessible alternatives and explicit publication decisions. Do not optimize children's time spent or make essential school work depend on social participation.

The strongest positioning is **"your school life, learning and community in one trusted place."** The defensible advantage is the existing school relationship and workflow context: membership comes from enrollment/employment/guardian links, assignments stay in the LMS, school notices remain authoritative, and graduation can lead to a new alumni relationship. A generic social feed alone offers little differentiation.

### 1.1 Decisions recommended now

1. Community appears on the login home, beneath urgent official items and role-specific tasks. A dedicated Community area supports deeper browsing.
2. The primary network boundary is a verified school institution inside a tenant. A chain may span nested institutions in that same tenant; chain-wide visibility requires explicit policy and participating institutions.
3. A child’s class, club, school, chain and alumni audience are distinct. Joining the school does not expose every member, campus or historic class.
4. Launch with staff publishing and moderated student submissions in selected age groups. Parent participation begins through child-linked audiences and controlled replies.
5. Begin with text and images. Trial moments/stories after expiry, reporting and media revocation work. Short video follows an operational and cost gate.
6. Use chronological/relevance-by-subscription feeds with finite pages and a caught-up state. No behavioral engagement ranking for minors.
7. Give alumni a separately verified, opt-in network. Alumni status alone never grants access to current minors or school operational records.
8. External sharing is off by default. Distinguish an authenticated link, an approved public derivative, a user-confirmed share, and publication to an official school account.
9. School discretion operates inside product safety rules and applicable law. An administrator toggle cannot substitute for required consent or make restricted records public.
10. Extend `comms` first, subject to a capability audit and design review. Preserve the existing module dependency direction; do not introduce another domain service merely because feeds look like a new product.

### 1.2 How to read this document

- **Observed:** directly supported by the current repository scan; a schema or contract is not proof of a complete workflow.
- **Proposed:** recommended behavior to build and validate.
- **Optional:** a viable later capability, subject to a decision and operational capacity.
- **Research gate:** legal, market, integration or usability uncertainty that must be resolved before the dependent release.
- **P0:** required for a safe, useful pilot; **P1:** expansion after pilot evidence; **P2:** later optional exploration; **Excluded:** deliberately outside the proposed product.

Requirements use stable family IDs. Unless otherwise stated, catalog requirements are proposed. Priorities express a delivery sequence, not evidence that an item already exists. This is a broad discovery specification; country law, school policies and actual user research will refine it.

## 2. Product goals and boundaries

The community should make school life visible, give members useful ways to participate and reduce fragmented communication. Its value depends on accurate school relationships, understandable audiences and manageable staff workload.

- **Belonging:** students, teachers, staff, guardians and alumni can contribute through appropriate, verified relationships.
- **Daily usefulness:** school tasks, authoritative notices and community updates coexist without obscuring required actions.
- **Trust:** membership changes, permission withdrawal, reporting and content removal work throughout the content lifecycle.
- **Operational sustainability:** schools can staff review and escalation, support their languages/devices and afford media delivery.
- **Continuity:** class rollover, graduation and alumni participation preserve connection through deliberately different access rights.

The initial scope stays within verified school and same-tenant chain boundaries. Public publication, alumni interaction and country expansion each have explicit release gates. The platform does not become a global student directory or an unrestricted consumer social network.

## 3. What the codebase already supports

### 3.1 Organizational and identity foundation — observed

- [`platform.tenant`](../../database/migrations/platform/V1_020_001__platform_baseline.sql) represents a customer organization: a trust, group, single school or training company. It is the data-isolation boundary.
- [`org.institution`](../../database/migrations/org/V1_040_001__org_baseline.sql) has `tenant_id`, `parent_institution_id`, configurable institution information and local timezone fields. Its comments explicitly describe nested group/campus institutions. A chain within one tenant fits this design; it does not establish federation between tenants.
- [`iam`](../../database/migrations/iam/V1_030_001__iam_baseline.sql) contains accounts, roles, permissions and scoped settings. `iam.member_record` is an event-maintained projection of **staff and student** records for `GetMe`, not a complete community membership or alumni registry.
- SIS, HR, people and academics provide enrollment, staff, person, guardian and class context. Each relationship must be evaluated for its specific purpose; a person’s existence is not proof of community entitlement.
- [`ARCHITECTURE.md`](../../ARCHITECTURE.md) defines tenant predicates plus forced PostgreSQL RLS, module-owned schemas, SDK/event integration, no cross-schema SQL, no cross-module foreign keys and no distributed transactions.

### 3.2 Community and LMS foundation — observed

[`comms`](../../database/migrations/comms/V1_190_001__comms_baseline.sql) already models notices, events, galleries, magazines, conversations/messages, feedback, audience groups, tasks, content views, class/club pages, page members, posts, post audiences, comments, reactions and notification preferences.

Current posts require an institution and academic year. Their visibility values are `all_classes`, `selected_classes` and `page_members`; their states are `draft`, `pending`, `posted` and `cancelled`. These are useful starting points, but do not fully model durable alumni membership, story expiry, public export approval, child media permissions or chain audience rules. `class_page_member` records following/reaction state; it should not be assumed to prove the proposed verified relationship lifecycle.

[`PostService.java`](../../backend/comms/src/main/java/com/mysms/comms/business/PostService.java) has CRUD and approve/cancel transitions with scope checks, audit and outbox publication. [`PostRepository.java`](../../backend/comms/src/main/java/com/mysms/comms/repository/PostRepository.java) applies institution data scope. This is evidence of building blocks, not a verified social authorization implementation covering every class, child, age, consent and audience rule proposed here. Evaluate generic record access as well as new feed endpoints before giving members social permissions.

[`api/operations-status.md`](../../api/operations-status.md) marks `comms.GetFeed`, `comms.SubmitPost`, `comms.FollowClassPage`, `comms.SendMessage` and `comms.ListInAppNotifications` as **501** at the time of the scan. `ListPosts`, class-page CRUD and `ApprovePost` are marked served. Served CRUD does not demonstrate delivery, personalized feed behavior or the proposed safeguards. Recheck the status when implementation begins because the repository is actively changing.

[`lms`](../../database/migrations/lms/V1_150_001__lms_baseline.sql) models courses, course/cohort enrollment, teachers, live classes, discussion prompts, forums/topics/posts, report categories, reports and read state. Learning discussion and graded participation must retain LMS ownership. A social reaction must never become a grade or attendance fact implicitly.

### 3.3 Home experience — observed

- [`web HomeScreen`](../../clients/packages/web-kit/src/screens/HomeScreen.tsx) is currently a welcome card and describes future role-specific dashboards.
- [`native HomeScreen`](../../clients/packages/native-kit/src/HomeScreen.tsx) is a permission-aware list of operational entry points such as timetable, homework, children, leave, payslips, profile and notifications.
- React web, native phone and tablet clients and common core/i18n packages already exist. The proposed community home is new product work, not an existing completed screen.
- Existing deep-link conventions in the architecture support module links without embedding tokens or personal data. Use them as navigation, with authorization at the destination.

### 3.4 Explicit gaps to investigate before reuse

**Early design discovery:** end-user post visibility; anti-spoofing of authors and roles; parent-child audience enforcement; complete submit/approve/edit/remove flows; attachment access; notification delivery and revocation; moderation cases and appeals; consent/permissions ledger; membership revocation; alumnus identities; story expiration; external publication; member-facing clients; accessibility; operational coverage. Later-feature discovery informs the design but does not make alumni, story expiry or public export P0 delivery gates. Do not solve the pilot by simply turning on existing generic CRUD for students.

**Confirmed dependency gaps:** the operation inventory also marks `platform.CreateUploadUrl`, `platform.CompleteUpload`, `platform.GetDownloadUrl` and file-object list/get/delete as 501. `people.ListConsentRecords`, `CreateConsentRecord`, `GetConsentRecord`, `WithdrawConsent` and `GetConsentStatus` are 501. Authorized media and usable consent/withdrawal workflows are prerequisites for the image pilot even though file/consent tables and related attachment records exist.

The current allowed SDK dependencies for `comms` are academics, IAM, org, platform and SIS; people, HR and LMS are not in that list. Consent/employment projections therefore need an approved event/revocation design or an explicit reviewed dependency change. Do not add convenient owner calls that violate the existing graph.

The repository scan establishes design context, not a runtime audit. The observed capabilities and gaps should be revalidated when implementation begins.

## 4. Product and business options

### 4.1 Product A — school bulletin and family window

Staff publish school/class updates, celebrations, event photos and official announcements; families view and acknowledge or react. Lowest participation and moderation complexity. Strong pilot and primary-school fit. Weakness: limited student agency, alumni value and peer collaboration. **P0 launch configuration**, with an explicit route toward richer participation.

### 4.2 Product B — moderated school community

Verified members contribute in school-approved spaces. Student/guardian posts may need approval; age and trust tier determine interaction privileges. This supports school identity and student voice without exposing the institution to an unrestricted social graph. Costs include moderation and appeals. **Recommended core product**, introduced through Product A plus a small student-submission pilot.

### 4.3 Product C — learning community and portfolios

Showcase projects, reading, art, experiments and reflections; connect to a course or learning objective where appropriate. Keep assessment evidence in the LMS; create a separate selected community artifact. Attractive for schools valuing visible learning. Risk: duplicate entry and confusing social applause with assessment. **P1**, with careful consent and authorship.

### 4.4 Product D — chain collaboration network

Teacher practice circles, interschool events, shared magazines, competitions and approved student projects across campuses. Requires explicit participating institutions, accountable moderators and scoped publishing rights. **P1 for selected same-tenant spaces**, after school-level boundaries work. A chain-wide bulletin can arrive earlier than cross-campus student interaction.

### 4.5 Product E — alumni association and opportunity network

Verified batches, reunions, career talks, adult mentorship, opt-in directory and school-sponsored opportunities. Builds continuity after graduation and can generate value without access to current children. Verification and reactivation are major product jobs. **P1 separate alumni area with adult-only directory/contact features and restricted minor bulletins/events; P2 supervised minor bridges**.

### 4.6 Product F — school publication and reputation desk

An editorial workflow turns selected internal moments into approved website/social media assets. School communications teams gain a reusable caption, translated copy, permission checks and destination-specific assets. This is a controlled publication product, not automatic forwarding. **P1 manual approved export; P2 official-account connectors**.

### 4.7 Product G — independent open school social platform

Anyone can discover schools, follow individuals, post publicly or message members. This would undermine the closed-network proposition, greatly expand safeguarding scope and create a separate consumer business. **Excluded** from this roadmap.

### 4.8 Commercial models

- **Bundled community:** basic school/class communication included in MySMS. Promotes one daily home and avoids charging families to receive essential updates. Recommended foundation.
- **School or chain subscription add-on:** advanced moderation workflows, editorial tools, alumni administration, richer archives and chain governance. Charge the institution, not children for visibility.
- **Usage allowance:** transparent storage, processing, media egress and optional delivery allowances with capped overage. Avoid pricing that discourages reporting or safety review.
- **Adult alumni association plan:** school-approved adult networking/event administration; optional school-paid or adult association-paid model after demand validation.
- **Managed publication/moderation services:** separately scoped service with clear human coverage, confidentiality and responsibilities; do not imply software automatically supplies trained school safeguarding staff.
- **Excluded revenue:** selling student data, targeted advertising to children, pay-to-promote student posts, public popularity boosts, or withholding essential school messages behind a parent premium tier.

Pricing and willingness to pay are hypotheses. Research Indian budget/private, premium, government/aided, international and chain schools separately. Do not assume every school has the same devices, language needs, moderation staff or purchasing power.

## 5. People, jobs and role boundaries

### 5.1 Students

Read relevant updates, participate in approved clubs, ask questions, contribute school work, celebrate peers, find help and maintain selected memories. Students need comprehensible audiences and a voice in community rules. Being under 18 in India remains legally relevant even where the product gives older teenagers more agency.

**Proposed experience bands, not statutory age bands:** early years/primary use teacher-mediated contributions and guardian access; middle years get guided submissions and structured replies; older teenagers get more autonomous publishing in approved spaces as policy allows; adults use adult rules while school membership remains scoped. Age unknown means restrictive privileges until verified, not a guessed grade-to-age conversion.

### 5.2 Teachers and teaching assistants

Publish once to appropriate classes, moderate a manageable queue, host subject/club spaces, find colleagues and show learning without exposing marks. Need drafts, delegated coverage, scheduling, consent assistance, quiet hours and a predictable workload. Do not make daily content quotas part of teacher evaluation.

### 5.3 Parents, guardians and carers

See relevant school/class updates, manage their own notification/language settings, grant or withdraw appropriate permissions, support events and contact staff through governed channels. Access comes from an active, authorized child relationship. Do not expose another guardian’s contact details, family arrangements or access rights.

### 5.4 Staff and contractors

Office, librarian, counselor, coach, nurse, transport and support staff may have official publishing or participation rights for their jobs. Safeguarding, medical, employment and transport records remain restricted. Contractor presence is verified, sponsored and time bounded; a contractor is not automatically in every student space.

### 5.5 School/chain leadership

Set governance, approve spaces, delegate moderation, publish official statements, coordinate chains and review aggregate outcomes. Leadership authority over an institution does not automatically justify reading every confidential report or private learning artifact. Separate editorial, technical, safeguarding and data-protection powers.

### 5.6 Alumni and former staff

Maintain a voluntary school relationship, reconnect with opt-in adults, attend reunions, offer verified opportunities and support school initiatives. Former teachers and alumni do not retain current student permissions. A person can hold an alumni role and an independently verified current parent/staff role; privileges remain attributable to the appropriate context.

### 5.7 Guests, service providers and platform support

Guest speakers use narrow invitations or submit through a staff sponsor. No public invitation grants roster access. Platform support uses exceptional, time-limited audited access where required, without joining school communities as a normal member. Anonymous visitors see only deliberately published public derivatives, if any.

## 6. Network topology and authorization requirements

### 6.1 Vocabulary

- **Tenant:** customer/trust/group isolation boundary.
- **Institution:** school, campus or nested unit owned by that tenant.
- **Relationship:** verified student, guardian-child, employment, alumnus, invited sponsor or other approved affiliation, with effective dates and status.
- **Space:** school, class, club, staff room, cohort, chain project, alumni batch or other approved community area.
- **Audience:** an explicit set/rule of allowed viewers for a content version.
- **Follow:** a preference to receive updates from an eligible space; it never creates eligibility.
- **Public derivative:** a separately approved artifact intended to leave the closed network.

### 6.2 Required policy behavior

- **NET-01 [P0]** Every object and operation carries tenant context from trusted authentication, never a client-supplied tenant selector alone.
- **NET-02 [P0]** School membership and role grants are independently checked. A role named “teacher” without a valid school assignment cannot publish as that school’s teacher.
- **NET-03 [P0]** Evaluate read, create, reply, react, moderate, export, download, tag and directory discovery separately.
- **NET-04 [P0]** Class access uses current enrollment, teaching or child-link entitlement plus explicit audience policy. Institution access alone is insufficient.
- **NET-05 [P0]** Guardian access is child-specific. Switching children or roles visibly changes context and never combines privileges into a broader directory.
- **NET-06 [P0]** Audience preview states institution(s), spaces, member categories, age restrictions and whether current/future members can see historical items.
- **NET-07 [P0]** Default to the narrowest relevant audience. “Whole school,” “whole chain,” guardians and alumni are explicit choices with extra review where needed.
- **NET-08 [P0]** After the revocation enforcement barrier completes, a removed parent/child/space relationship grants no subsequent protected reads, writes or media grants. Bound and measure source-change-to-enforcement exposure through ARCH-05/07; higher-risk actions require current evidence, and urgent restrictions use emergency invalidation. Old feed entries and notifications confer no rights.
- **NET-09 [P0]** Deny optional community privileges for missing, ambiguous, expired, suspended or unverified membership. Fail closed when their authorization cannot be established. Essential school access, safety reporting and privacy/complaint routes use their own separately evaluated entitlements; a social restriction must not disable those routes.
- **NET-10 [P0]** Enforce the same rules on lists, detail URLs, search, counts, attachments, thumbnails, notifications, exports, moderation tools and generic resource routes.
- **NET-11 [P0]** A block/mute is a social preference or safety restriction, not a way to suppress essential school notices or erase a safeguarding report. Explain its exact effect.
- **NET-12 [P0]** Explicit denies, safety constraints, jurisdiction requirements and withdrawn permissions cannot be overridden by ordinary space administrators.
- **NET-13 [P1]** Chain spaces name participating institutions and accountable publisher/moderator owners. Parent-institution hierarchy does not silently grant member-to-member access.
- **NET-14 [P1]** Cross-campus competitions expose approved team/project artifacts, not full minor directories or private course membership.
- **NET-15 [P2]** Cross-tenant collaboration requires a separate federation design, school agreements, ownership and revocation plan. Never use privileged database access to assemble it.
- **NET-16 [P0]** Child/staff membership is never conditioned on fee payment status for community safety, complaints or essential school access. Any nonessential suspension needs a documented policy and appeal.
- **NET-17 [P0]** Prohibit cross-tenant contact syncing and global person search. Tenant-scoped accounts are not a global social identity.
- **NET-18 [P0]** A member may belong to multiple institutions/roles; display the authoring identity and school context before publishing.

### 6.3 Policy precedence and history

Proposed resolution: law and platform safety floor → tenant/chain ceiling → institution policy → space policy → content-specific audience/permission → member preference. Lower levels may narrow access; widening beyond an upper-level restriction requires the authorized policy process. Record which policy version produced a decision and provide an administrator-readable explanation without leaking protected facts.

History needs a deliberate setting. For example, a new student joining Class 8B might see this term’s general class updates, while a project group or old trip gallery requires explicit inclusion. An alumnus never inherits a class’s future feed. Changing audience after publication is a new disclosure decision: narrowing takes effect promptly; widening triggers preview and renewed review/permissions where needed.

## 7. Membership, onboarding and lifecycle

- **MEM-01 [P0]** Establish membership from authoritative SIS/HR/guardian records or an approved invitation process, with source and effective dates.
- **MEM-02 [P0]** Invitations are scoped, expiring, single-use where appropriate, rate-limited and revocable. A photographed school QR code cannot itself prove membership.
- **MEM-03 [P0]** Support assisted onboarding, school IDs and secure account recovery for users without personal email. A shared phone number is not a unique person identifier. Existing IAM requires unique sign-in identifiers across accounts; shared family contact details need a separate contact/recovery design and cannot simply be reused as every person's login identifier.
- **MEM-04 [P0]** Present simple role/age/language-appropriate community rules, purposes and privacy information before optional participation.
- **MEM-05 [P0]** Separate essential school access from optional social activation, optional directory participation and external publication permission.
- **MEM-06 [P0]** Honor verified guardian restrictions, custody changes and lawful access limitations through a confidential administrative process. Do not expose reasons in the feed.
- **MEM-07 [P0]** Promotion and academic-year rollover update class relationships without silently expanding old content to the new cohort.
- **MEM-08 [P0]** Transfers, withdrawals, employment termination, leave and suspension have defined effective-time behavior, including pending drafts, moderation duties and access revocation.
- **MEM-09 [P0]** Account deactivation does not delete required evidence automatically; ordinary content visibility, retention and identity display are decided separately.
- **MEM-10 [P0]** Account recovery re-establishes current entitlements and revokes compromised sessions; a recycled phone/SIM must not acquire the prior user’s data.
- **MEM-11 [P1]** Graduation initiates an invitation/eligibility process for alumni rather than automatic permanent access. Under-18 graduates retain minor protections.
- **MEM-12 [P0]** Age-of-majority transitions prompt updated notices and independent choices for any eligible pilot user, including a current student who turns 18. Do not suddenly publish an adult profile or preserve guardian access by habit. Alumni-related transition features follow their later release gate.
- **MEM-13 [P1]** Resolve duplicate records and person merges with verified evidence, an audit trail and re-evaluation of grants; never infer two people are identical from names alone.
- **MEM-14 [P1]** Handle school closure, tenant exit, school transfer to another trust, institution split/merge and deceased-member memorial requests with explicit ownership and archive/export rules.
- **MEM-15 [P0]** Offer opt-out and reactivation without loss of essential notices, homework or reporting access. Quiet/nonposting membership is valid.

## 8. Login home and navigation requirements

### 8.1 Information order

Proposed home sequence: visible school/role/child context → urgent official items → “Today / needs action” → an optional P1 school-moments strip → a finite community preview → upcoming relevant events. The P0 home works without stories. The full community feed is one tap away. Administrators can adjust approved layouts within accessibility and essential-task constraints.

- **HOME-01 [P0]** Student home highlights timetable, due work and relevant official notices before optional social items.
- **HOME-02 [P0]** Teacher home highlights teaching/attendance/work review and moderation tasks, with a quick scoped composer.
- **HOME-03 [P0]** Guardian home labels each child’s school/class and combines tasks only where their origins remain clear; provide child filters.
- **HOME-04 [P0]** Staff home shows job-specific actions and relevant school updates; restrict sensitive operational cards to their owning permissions.
- **HOME-05 [P1]** Alumni home prioritizes alumni events, batch updates and opted-in opportunities rather than a current student feed.
- **HOME-06 [P0]** Official urgent notices have a separate visual treatment, issuer, validity and acknowledgment state. Likes or story views never count as required acknowledgment.
- **HOME-07 [P0]** Community failure or loading must not block timetable, attendance, fees, homework, safety reporting or other operational navigation.
- **HOME-08 [P0]** Provide filters for school, class, clubs and official updates, an accessible composer and persistent report/help entry points.
- **HOME-09 [P0]** Empty home explains relevant next actions without inventing content, exposing other groups or pushing users to invite strangers.
- **HOME-10 [P0]** Restore scroll/filter state safely within the active context; clear protected content on account/child/role/tenant switching and sign-out.
- **HOME-11 [P0]** Mobile/tablet/web support the same entitlement rules. Shared classroom tablets use supervised sessions and automatic return to a safe screen.
- **HOME-12 [P0]** Standard links open owning-module screens with fresh checks. Personal operational summaries do not become social posts.
- **HOME-13 [P1]** Allow a simple “tasks first” preference and disable the moments strip per institution/age group without losing the community’s useful text updates.

### 8.2 Feed principles

Reverse chronological within eligible subscriptions is the pilot default. Official pinning is labeled, bounded and expires. Group repetitive system updates into compact cards. Prefer a finite “you’re caught up” experience and explicit load-more. No autoplay, child popularity charts, streaks, follower counts, outrage ranking or dark patterns requiring frequent return.

Eligibility must be applied before pagination, counts and preview generation. A feed projection can propose candidates; it cannot decide authorization. A new student, guardian-link removal or takedown must change results even while background feed updates are delayed.

## 9. Content, creation and interaction catalog

### 9.1 Posts and publishing

- **POST-01 [P0]** Support concise text updates with accessible expansion for longer content; proposed initial composer guidance is roughly 500 characters, not an arbitrary protocol limit.
- **POST-02 [P0]** Support image posts/carousels with alt text, captions and a low-data variant. Proposed pilot limits: up to 5 images, each bounded by validated upload policy.
- **POST-03 [P0]** Composer clearly shows the author role, school, space and audience; trusted official badges derive from grants, not editable profile text.
- **POST-04 [P0]** Drafts, explicit submission, pending review, approve, return/reject with reason, publish, edit, withdraw, remove and archive have clear states.
- **POST-05 [P0]** Authors see their own pending content and status; pending student submissions are not visible to ordinary members.
- **POST-06 [P0]** Editing published content creates a version; changes to audience, media or meaning require the applicable review again. Approved content cannot be swapped after review.
- **POST-07 [P0]** Scheduled publication checks current author eligibility, audience, permissions and media state when execution occurs. A staff member leaving before publication invalidates the schedule or requires an authorized owner.
- **POST-08 [P0]** Prevent spoofed authorship, official labels and impersonation. Delegated/on-behalf-of publication preserves both actor and represented publisher.
- **POST-09 [P0]** Cancel/retry must not duplicate posts. Warn before discarding unsaved work. Show upload and review failures distinctly.
- **POST-10 [P0]** Allow post-level comments/reactions controls bounded by school/space policy and a clear closed-comments state.
- **POST-11 [P1]** Post templates cover classroom recap, achievement, event recap, club invitation, reading recommendation and student project.
- **POST-12 [P1]** Collaborative/coauthored artifacts preserve credits and permissions; team membership is not a license to publish every participant’s identity.
- **POST-13 [P1]** Teacher-reviewed translation and caption assistance may be offered, showing original text and correction options.
- **POST-14 [P1]** Categories/hashtags are school-scoped, approved and searchable; trending tags do not become a global discovery surface.
- **POST-15 [P1]** Share a selected artifact to another eligible internal space only through an audience check and renewed approval if widened. Prefer reference/link when it preserves access boundaries.
- **POST-16 [P2]** Short video/voice notes require transcripts, processing/moderation limits and separate retention/egress budgets. No public live streaming at launch.

### 9.2 Replies, appreciation and discussion

- **INT-01 [P0]** Allow configured reactions such as appreciate/celebrate/helpful; no public dislike/downvote of children or leaderboard of reactions.
- **INT-02 [P0]** Replies inherit or narrow the parent’s audience and media rules. Deleted/removed parent behavior is defined for the entire thread.
- **INT-03 [P0]** Limit nesting and provide report/hide controls at comment level. Rate-limit bursts, repeated text and repeated targeting of one member.
- **INT-04 [P0]** Mentions search only eligible people in the current space. A mention does not expand access or disclose membership to others.
- **INT-05 [P0]** Protect against dogpiling: temporarily lock a thread, slow replies and hold new comments for review without publicly labeling a child an offender.
- **INT-06 [P0]** Student reaction counts are hidden or private by default; authors receive useful feedback without competitive comparison.
- **INT-07 [P1]** Structured polls support school/club decisions; show who can see individual responses and whether the poll is advisory. Do not advertise anonymity if staff can identify respondents.
- **INT-08 [P1]** Q&A can mark an answer as staff-confirmed; academic graded discussions remain in the LMS.
- **INT-09 [P1]** Parent discussions and staff-only rooms are separate from student spaces. Staff confidential cases are excluded from ordinary staff social rooms.
- **INT-10 [P0]** Reporters can communicate privately with authorized reviewers. Anonymous-to-peers reporting may exist; no promise of anonymity from required safeguarding investigation.

## 10. Moments, stories, media and memories

### 10.1 Stories

- **STORY-01 [P1]** An image/text moment has an explicit audience and expiry. Proposed default: 24 hours, with school-approved alternatives.
- **STORY-02 [P1]** Story capture and submission use the same permission, moderation and media pipeline as posts; “quick” does not bypass review.
- **STORY-03 [P1]** Expired stories disappear from feeds, detail routes, search and new media access even if the expiry worker runs late.
- **STORY-04 [P1]** Show a plain explanation: expiry ends ordinary visibility; authorized incident evidence, retention obligations, backups and already exported copies follow separate rules.
- **STORY-05 [P1]** Support accessible pause, manual navigation, text alternative, reduced motion and no automatic sound/video.
- **STORY-06 [P1]** A story can be reported while visible; a report preserves only necessary evidence under restricted access even after expiry.
- **STORY-07 [P1]** Viewer identities/counts are not a social popularity tool for minors; any necessary administrative access is purpose-limited and disclosed.
- **STORY-08 [P1]** Promoting a story into a permanent highlight/yearbook is a new retention and audience decision requiring the applicable permissions.
- **STORY-09 [P0]** Never promise screenshot prevention or screenshot detection across all devices. Policy controls cannot recall what a permitted viewer has independently captured.

### 10.2 Media handling

- **MEDIA-01 [P0]** Upload into private quarantine; enforce file signature/type, byte/dimension/duration limits, malware checks and safe processing before visibility.
- **MEDIA-02 [P0]** Strip unnecessary EXIF/location and embedded identifying metadata; protect originals and every derivative/thumbnail equally.
- **MEDIA-03 [P0]** Use authenticated media delivery or revocation-aware delivery grants with a documented residual exposure window. A long-lived unrevocable CDN URL does not meet urgent takedown requirements.
- **MEDIA-04 [P0]** Check rights for depicted/identified children and other people, not just the uploader. Use manual subject confirmation/review; no biometric identification is needed.
- **MEDIA-05 [P0]** Unidentified children in a group image prevent public export until reviewed, cropped, redacted or otherwise approved under the required policy. Automated blur assistance is not proof of anonymity.
- **MEDIA-06 [P0]** Prevent media from another tenant, private record or unapproved space being attached by guessing its identifier.
- **MEDIA-07 [P0]** Caption/alt text must not reveal hidden names, phone numbers, health details, grades or location that the picture itself omits.
- **MEDIA-08 [P0]** Apply deletion/withdrawal to originals, derivatives, attachment references, search and caches, with separately documented backups/legal holds.
- **MEDIA-09 [P1]** Offer school albums with explicit attendees/audience and a reviewed cover. Do not automatically tag every attendee.
- **MEDIA-10 [P1]** Selected memory books and personal artifact export need bystander redaction, rights checks and retention choices. A student's own archive entitlement does not entitle them to every class photo.
- **MEDIA-11 [P2]** Video includes captions/transcripts, thumbnail review, modest resolution and duration ceilings, processing status and bounded retry.

## 11. School use cases and domain boundaries

The catalog below describes possible community experiences. It does not transfer ownership of the underlying records into `comms`.

### 11.1 Classroom, learning and clubs

- **USE-01 [P0]** Teacher classroom recap: an accessible summary of what the class explored, scoped to the class and approved linked guardians.
- **USE-02 [P0]** Student work showcase: student/teacher chooses a project artifact, credits creators, checks bystanders and submits for the appropriate audience.
- **USE-03 [P1]** Subject help/Q&A: approved ungraded questions and shared explanations; personal remediation and grading remain private LMS work.
- **USE-04 [P1]** Club/society spaces: robotics, debate, eco-club, drama, music, coding, sports, art and school magazine, each with a responsible adult and membership rules.
- **USE-05 [P1]** Houses and student council: announcements, ideas and supervised debate; official elections require a separately reviewed voting process, not a reaction count.
- **USE-06 [P1]** Reading/library circles: book reviews, reading recommendations and moderated challenges, without exposing an individual’s borrowing history or fines.
- **USE-07 [P1]** Service/volunteering: opportunities, organizer-approved photos and reflections; verified service evidence lives with its authorized owner.
- **USE-08 [P1]** Peer appreciation and quiet contributions: collaborative recognition that includes support staff, effort and service without publishing rankings or inferred character scores.
- **USE-09 [P2]** Digital citizenship activities: audience-selection exercises, credit/copyright practice and restorative community learning, not automated punishment.

### 11.2 Events and school life

- **USE-10 [P0]** Assemblies, annual day, science fairs, sports days, exhibitions and festival recaps link to official events and approved galleries.
- **USE-11 [P1]** Trips/camps: permission-aware delayed recaps, attendance-independent publishing and no public real-time location of children.
- **USE-12 [P1]** Competitions and team achievements: verified official outcomes, team credit, approved names/media; no automatic posting of report-card marks.
- **USE-13 [P1]** School magazine/newsroom: student editors, staff supervision, issue review, accessible reading and separate public editions.
- **USE-14 [P1]** PTA and volunteering: opt-in parent opportunities, school-sponsored groups and event logistics; no universal parent contact directory.
- **USE-15 [P1]** Inclusion and accessibility: text/audio alternatives, sensitive treatment of accommodations and no disability labels in audience-facing cards.
- **USE-16 [P1]** School history and anniversary archives: verified contributions from former members with permission for depicted people and careful treatment of historic minors.
- **USE-17 [P1]** Ask the School: shared, staff-confirmed answers to recurring general questions, with private redirection for individual cases. P0 staff may publish ordinary informational posts without a dedicated Q&A workflow.

### 11.3 Staff, leadership and chain

- **USE-18 [P1]** Staff practice circles: teaching ideas, approved resources, professional learning and internal appreciation, without confidential personnel cases.
- **USE-19 [P1]** Chain teacher exchange: vetted resources and common initiatives across participating campuses; source attribution and licensing are retained.
- **USE-20 [P1]** Interschool events/projects: a bounded collaboration space with named institutional sponsors, explicit participants and an end date.
- **USE-21 [P1]** Principal/leadership updates: trusted publisher, official correction process and structured questions. Complaints are never forced into public threads.
- **USE-22 [P1]** School listening: moderated suggestions and advisory surveys, with clear audience and respondent-identity rules; no sentiment score of individual children.
- **USE-23 [P2]** Approved resource marketplace between staff: library of reusable school-owned material, reviewed separately for licensing and commercial activity.

### 11.4 Explicit sensitive-data boundary

**Never auto-publish:** marks/grades, assessment feedback, exam accommodations, attendance/absence reasons, fee balances, defaulters, financial aid, payroll, health/medical information, counseling, discipline, safeguarding cases, custody restrictions, addresses, phone numbers, bus routes, precise location, identity documents or credentials. A personal authorized home card may summarize some of these through its owning module; that is not community publication.

Exceptional publication, such as a selected public award, requires a deliberately prepared artifact, proper authorization and the applicable rights. A teacher permission to read records is not permission to publish them. Student refusal to appear in publicity must not reduce teaching access, grades or access to required tasks.

## 12. LMS integration without loss of learning boundaries

- **LMS-01 [P0]** Keep lessons, graded discussions, submissions, quizzes, attendance, feedback, gradebooks and completion rules in their owning modules.
- **LMS-02 [P0]** A school student, an online course buyer and a cohort learner are distinct memberships. Buying/enrolling in a course does not grant access to schoolchildren’s communities.
- **LMS-03 [P0]** Home shows authorized assignment/course reminders without placing private content in the shared social timeline or notification preview.
- **LMS-04 [P0]** Community reactions, view time or posting frequency never determine grades, attendance, progression or discipline automatically.
- **LMS-05 [P1]** Offer “propose as showcase” from a learning artifact: select content, remove private feedback/marks, verify authorship and media rights, then submit a distinct community revision.
- **LMS-06 [P1]** A showcase may link to an original only for viewers separately entitled to that original. Hidden course names/rosters are not leaked in fallback cards.
- **LMS-07 [P1]** Course/cohort community spaces inherit only approved membership projections and policies, not a public catalog course’s discoverability.
- **LMS-08 [P1]** Teacher can route a social question to a learning discussion without copying private participant history or grading context across audiences.
- **LMS-09 [P1]** Project groups and clubs can share selected artifacts, with group credits and contribution permissions. Peer assessment remains private to its intended LMS audience.
- **LMS-10 [P1]** Course/cohort expiration, withdrawal, cancellation and subscription expiry have defined community consequences; essential complaint and safety reporting remain available.
- **LMS-11 [P1]** Learning portfolios distinguish private evidence, family sharing, school showcase and public export. Changing one does not silently change the others.
- **LMS-12 [P2]** Teacher professional learning communities can span campuses through explicitly approved spaces; do not use student courses as staff networking areas.

The existing architecture allows LMS → comms dependencies. Home composition should use client-side authorized queries or an explicitly designed aggregation layer. New comms → LMS SDK reads would create a dependency issue and require review; minimal events/read projections may support eligible use cases without reversing the graph.

## 13. Messaging and relationship discovery

Messaging can consume more moderation effort than posts. Existing conversation tables do not justify unrestricted school chat.

- **MSG-01 [P0]** No unrestricted alumni/guest/adult-to-minor direct messages, private calls or contact requests.
- **MSG-02 [P0]** Existing staff-parent or school service communication stays in an approved, institution-governed workflow with appropriate record access and quiet hours.
- **MSG-03 [P0]** School-wide user search, phone contact upload, friend suggestions and address-book harvesting are disabled for the pilot.
- **MSG-04 [P0]** Display name/photo options minimize identifying information; student profiles do not expose personal email, phone, exact birth date, address or public social handles by default.
- **MSG-05 [P1]** Student-to-student messaging, if validated, is a separate release decision with eligible ages, small verified spaces, reporting, retention, supervisory transparency and coercion controls.
- **MSG-06 [P1]** Teacher office hours and structured request forms reduce expectations of constant availability; routing to a deputy is explicit.
- **MSG-07 [P1]** Directory visibility is opt-in or purpose-authorized, field-level and scoped. Being searchable does not permit messaging or export.
- **MSG-08 [P1]** Invitations to groups cannot expand a member’s school/age eligibility. Group owners cannot add arbitrary adults or minors.
- **MSG-09 [P0]** No “disappearing” private child chat in the initial product. Do not promise end-to-end encryption while claiming server-side content moderation; make any future encryption/oversight tradeoff explicit.

## 14. Alumni network requirements

### 14.1 Start with a separate verified community

- **ALUM-01 [P1]** Verify historical affiliation using school records and/or authorized school review; an email domain, name match or old roll number alone is insufficient.
- **ALUM-02 [P1]** Record school, batch/year, verification source and current membership status. Support changed names and incomplete historic records through a private review path.
- **ALUM-03 [P1]** Alumni activation is opt-in with fresh profile/directory choices and contact preferences; do not bulk publish historic student records.
- **ALUM-04 [P1]** Separate adult and minor eligibility. Finishing Class 12 or leaving school is not proof of being 18.
- **Initial minor-alumni policy:** verified minor alumni receive school-curated batch bulletins and approved event information only. Adult directory/contact requests, recruiting and mentorship are unavailable to them until a separate minor-program policy and release are approved. They retain age-appropriate reporting and privacy routes.
- **ALUM-05 [P1]** Batch groups, reunions, school updates, memories and chapter/event subscriptions are bounded by verification and selected profile fields.
- **ALUM-06 [P1]** Directory search uses eligible adult fields chosen by the member, with throttling and no bulk export/scraping.
- **ALUM-07 [P1]** Adult contact requests require acceptance; do not reveal email/phone merely because a request was sent.
- **ALUM-08 [P1]** Handle dormant accounts, re-verification, account recovery, report/ban/appeal and alumni admin succession.
- **ALUM-09 [P1]** Former staff/parents have separate categories and rights; they are not automatically alumni or current staff.

### 14.2 Opportunities and guarded bridges

- **ALUM-10 [P1]** Approved career panels, college pathways, volunteering and school-history contributions can begin through school-hosted groups.
- **ALUM-11 [P1]** Opportunity postings name the verified organization/sponsor, scope, eligibility and review owner; report scams and paid solicitations.
- **ALUM-12 [P2]** Minor mentorship needs an explicitly sponsored, time-bounded program with eligible participants, trained supervision, consent/assent where applicable and a clear reporting route.
- **ALUM-13 [P2]** A mentorship grant does not expose the minor directory, private course work, location, personal contact details or ambient direct messaging.
- **ALUM-14 [P2]** School fundraising, scholarships and donations use an approved finance/payment process, receipts and country-specific review; social posts do not collect money directly at launch.
- **ALUM-15 [P2]** Adult alumni jobs and referrals may link to careers capabilities after ownership and privacy review; commercial recruiting never targets current children through school memberships.

Design the alumni transition now, but ship interactive alumni networking after the pilot. A verified batch bulletin/reunion feature can arrive before directories, mentorship or commercial opportunities.

## 15. External sharing: four distinct products

### 15.1 Private authenticated link

A member shares a MySMS link in WhatsApp or elsewhere. The recipient still needs an authorized account and current membership. The link itself grants no access, and its preview must reveal no protected title, name, image or text. This can support legitimate school coordination with a generic preview such as “School update — sign in to view.” Private content remains protected even when the URL is public.

### 15.2 Approved public derivative

A school editor selects content and prepares a sanitized asset/caption that has permission to leave the network. This is a new publication record with a reviewed audience, destination, content version, assets and permission evidence. It may be downloaded by an authorized publisher, published on a school website or handed to an approved connector.

### 15.3 User-confirmed sharing

An allowed user chooses an approved public artifact and invokes a supported share mechanism. This is a handoff, not proof of a completed external post or its final audience. Users may alter text or redistribute outside the application after export. Administrators can control what MySMS releases, not guarantee what the receiving app or another device does with it.

### 15.4 Official-account publication

An authorized school publisher connects the school-owned destination and publishes an approved artifact with a durable job, provider response and audit history. This needs credentials, provider permissions, account eligibility and operational support. It is independent of user social accounts, private links and internal post approval.

### 15.5 Platform-specific planning — researched limits

- **WhatsApp:** the official help center supports a prefilled-text share link; the user still chooses a contact and sends. Automated business delivery is a separate product with consent/preferences and approved-template constraints described by Meta. Do not promise automatic posting into arbitrary parent groups, Status or Channels. Validate the chosen supported route at implementation. [WhatsApp click to chat](https://faq.whatsapp.com/5913398998672934), [Meta business chat controls](https://about.fb.com/news/2025/04/ways-to-manage-your-businesses-chats-on-whatsapp/).
- **Instagram:** Meta’s published Instagram API collection describes professional-account publishing and account/login-specific restrictions. Do not promise silent publication to arbitrary personal accounts or require every user to connect Instagram. Use approved school-owned accounts where supported; validate current permissions, app review, formats and story eligibility. Direct Meta documentation returned access/rate-limit errors during this research, so exact current route/format support remains a release gate. [Meta’s Instagram API collection](https://www.postman.com/meta/instagram/documentation/6yqw8pt/instagram-api?entity=request-23987686-10bf8bf7-d9b7-4340-9c66-61d3ce683a87).
- **X/Twitter:** current official documentation exposes create-post operations with OAuth authorization and write scopes. Confirm account access, commercial terms, media support and quotas before committing. A manually initiated sharing path can be investigated separately; the old developer web-intent URL redirected to general documentation during research and is not treated as verified current integration support. [X create-post documentation](https://docs.x.com/x-api/posts/create-post).

No platform pricing, quota, guaranteed delivery time or deletion guarantee is assumed here. Recheck provider documentation near implementation and launch.

### 15.6 Export policy and workflow requirements

- **EXT-01 [P0]** External release is disabled by default; control destination, school, age band, content type, requester and authorized publisher separately.
- **EXT-02 [P0]** Private links remain authenticated and generate generic previews. Never expose a private media URL in Open Graph tags or third-party link unfurling.
- **EXT-03 [P1]** An external request selects the exact internal revision and a separate public derivative, not a reusable permission to export all future versions.
- **EXT-04 [P1]** Review author rights, all identifiable/depicted subjects, required permissions, school branding, copyright, location, names in audio/text and background identifiers.
- **EXT-05 [P1]** Unknown subjects or missing/withdrawn permission block release or require a reviewed redaction/crop; a school administrator cannot tick away the missing evidence.
- **EXT-06 [P1]** Approval binds a content/asset hash, audience, caption, destinations, policy version, approver and expiry. Sensitive releases use an independent approver where policy requires it.
- **EXT-07 [P1]** Recheck permissions and eligibility immediately before dispatch, including scheduled posts and queued manual downloads.
- **EXT-08 [P1]** The destination preview explains what becomes public or leaves MySMS, what identifiers remain and that external copies may survive withdrawal.
- **EXT-09 [P1]** Do not export comments, reaction lists, viewers, class rosters, private course links or unrelated student records with an approved image.
- **EXT-10 [P1]** Record request, approve/reject, dispatch/handoff, actor, destination, known provider ID, unknown outcome and recall/removal attempts.
- **EXT-11 [P2]** Official-account credentials are protected, scoped, rotated and revocable, with school approval and separate publisher powers. Offboarding removes connectors and pending jobs.
- **EXT-12 [P2]** Provider timeouts enter an unknown/reconciliation state. Do not retry blindly and duplicate publication; use provider idempotency when available and local job identity throughout.
- **EXT-13 [P1]** Withdrawing permission stops pending releases and triggers supported takedown/recall steps for completed releases; show partial/failed/unknown removal honestly.
- **EXT-14 [P1]** Disable/kill switches apply server-side to downloads, jobs and connectors, while preserving release evidence and ordinary school access.
- **EXT-15 [P0]** Disabling in-app export cannot prevent all screenshots, camera capture or unauthorized copying. Explain boundaries without making false control promises.
- **EXT-16 [P1]** An external export does not automatically import platform comments, identities or follower graphs back into the school network.

## 16. Consent, permissions and privacy requirements

### 16.1 Existing foundation and proposed extension

[`people.consent_record`](../../database/migrations/people/V1_050_001__people_baseline.sql) already records a person, giver, purpose, policy version, timing, channel and optional evidence. Its purposes include photo/video publication, public profile, marketing, third-party sharing and SMS/WhatsApp. [`people.public_profile`](../../database/migrations/people/V1_050_001__people_baseline.sql) is described as a public student/alumni profile. Neither is proof of a complete school social consent or alumni verification workflow.

Reuse the authoritative consent owner where appropriate; add community-specific decision evidence rather than inventing a competing universal consent ledger. A broad current-purpose record may be necessary but insufficient for exact content, destination, depicted people and withdrawal propagation. Public profiles must never activate automatically merely because alumni/social features are enabled.

- **PRIV-01 [P0]** Document data roles and purposes for school operation, optional community, alumni and external promotion, including what MySMS processes for its own purposes.
- **PRIV-02 [P0]** Separate account participation, internal depiction, directory/public profile, external publication, optional marketing and channel delivery choices.
- **PRIV-03 [P0]** Record the subject, authorized giver, verified relationship, notice/policy version, purpose, scope, timing, evidence source, withdrawal and applicable expiry/review.
- **PRIV-04 [P0]** A guardian authorizes only their lawful scope; consent for one child does not cover siblings, classmates or all children in a photograph.
- **PRIV-05 [P0]** Explain purposes and controls in plain regional-language and age-appropriate forms. Keep essential learning and required notices accessible when optional social consent is refused.
- **PRIV-06 [P0]** Support permitted offline/paper evidence with authorized capture and verification; do not treat an imported unchecked spreadsheet as verified consent.
- **PRIV-07 [P0]** Show an easy withdrawal/request route and a useful status of affected internal visibility, scheduled exports and known external copies.
- **PRIV-08 [P0]** Resolve conflicting guardian instructions, objections by a child and lawful restrictions through a private documented process; do not publish dispute details.
- **PRIV-09 [P0]** Data access/correction/erasure requests have an identity-verified workflow, an accountable owner and documented exceptions/holds. Export is subject to other people’s rights.
- **PRIV-10 [P0]** Specify retention separately for drafts, active posts, expired stories, alumni profiles, consent evidence, reports, audit/security logs and backups; no “keep everything forever.”
- **PRIV-11 [P0]** Legal/evidence holds are restricted, reasoned, reviewed and time-bounded where possible; they do not keep the held item visible in the normal feed.
- **PRIV-12 [P0]** Limit vendor and analytics data, redact logs, prohibit student-content model training by default, and publish relevant subprocessor/data-location information.
- **PRIV-13 [P0]** Minimize birth-date/identity-document handling. Prefer existing reliable school records or an approved verification method; do not add mandatory Aadhaar, face recognition or new identity-document collection by default.
- **PRIV-14 [P0]** Make parental supervision visible and understandable to the child. Do not expose private help-seeking or a report automatically to a potentially implicated guardian/staff member.
- **PRIV-15 [P1]** School/tenant exit provides agreed authorized export, deletion/retention evidence and revocation of connectors; alumni continuity requires an explicit continuing owner/arrangement.

### 16.2 India: source-verified baseline and launch review

The DPDP Act defines a child as under 18. Section 9 addresses verifiable parental consent, detrimental processing, tracking/behavioral monitoring and targeted advertising involving children, with specified exemptions. Treat older teenagers as minors for the India policy design; social participation and publicity need purpose-specific review. [DPDP Act, sections 2 and 9](https://www.meity.gov.in/static/uploads/2024/06/2bf1f0e9f04e6fb4f8fef35e82c42aa5.pdf).

The 2025 Rules specify parental-consent mechanisms. Their Fourth Schedule limits the educational-institution tracking/monitoring exemption to educational activities or enrolled children’s safety. **Inference:** do not assume this covers promotional social publication, alumni networking or commercial engagement analysis. [DPDP Rules, rule 10 and Fourth Schedule](https://www.meity.gov.in/static/uploads/2025/11/53450e6e5dc0bfa85ebd78686cadad39.pdf).

Commencement is phased: the notification defers many core duties, including section 9, by eighteen months from publication; the Rules similarly defer most operational provisions. The instruments bear 13 November 2025 dates, implying a May 2027 planning horizon. Confirm the exact legal effective date, corrigenda and later changes before launch; use **April 2027 as a proposed internal readiness target**, not a legal deadline. [Act commencement notification](https://www.meity.gov.in/static/uploads/2025/11/c56ceae6c383460ca69577428d36828b.pdf), [Rules commencement](https://www.meity.gov.in/static/uploads/2025/11/53450e6e5dc0bfa85ebd78686cadad39.pdf).

**Required legal discovery, without assuming the answer:** applicability of intermediary/social-media rules and school safeguarding obligations; mandatory reporting and evidence-handling duties including POCSO-related issues; grievance mechanisms and statutory timelines; sector/board/state policies; copyright/music rights; accessibility; international transfers and vendor processing; retention exceptions; school/MySMS data roles; treatment of guardianship and adults needing lawful support. Keep the resulting legal obligations distinct from configurable school preferences. This product proposal is not a legal opinion.

## 17. Safety, moderation and appeals

### 17.1 Human operating model

Each pilot institution appoints a community owner, editorial approvers, a trained safeguarding lead and deputies, a data/privacy owner and technical escalation contact. The chain names an owner for every shared space. Platform and school responsibilities must be explicit in contracts and runbooks. Software does not supply round-the-clock safeguarding coverage by itself.

- **SAFE-01 [P0]** Publish understandable community rules covering bullying, discrimination, sexual content, grooming, violence/threats, impersonation, doxxing, scams, cheating/academic misuse, copyright and unauthorized imagery.
- **SAFE-02 [P0]** Report controls exist on posts, replies, profiles, spaces and media, including a private route for concerns about staff or administrators.
- **SAFE-03 [P0]** Triage routes distinguish routine editorial issues, conduct concerns, urgent safety concerns, privacy requests and statutory escalation. A post report is not automatically a disciplinary finding.
- **SAFE-04 [P0]** The implicated author/moderator cannot be the sole reviewer. Reporter identities and case details are restricted to authorized need-to-know roles.
- **SAFE-05 [P0]** Provide a practical urgent-help route and clearly state staffed hours/response expectations; do not label the social app an emergency service.
- **SAFE-06 [P0]** Allow immediate restriction of visible content by authorized responders, with evidence preservation and audit independent of the final investigation.
- **SAFE-07 [P0]** Reports survive ordinary story expiry or author deletion under the approved evidence policy; confidential evidence does not remain available to the class.
- **SAFE-08 [P0]** Moderation queue shows owner, severity, age, escalation and deputy coverage; no pending item becomes public merely because an SLA expires.
- **SAFE-09 [P0]** Inform authors/reporters appropriately about outcomes and appeals without revealing protected people, evidence or legal investigations.
- **SAFE-10 [P0]** Offer correction, appeal and independent review. Retain reason codes and policy versions; prevent retaliatory removal of legitimate criticism.
- **SAFE-11 [P0]** Support freeze-posting, lock-thread, slow-mode, pre-review and disable-export controls by institution/space. Reading required notices and reporting remain available.
- **SAFE-12 [P0]** Rate-limit mass mentions, invitation abuse, repeated replies, scraping, bulk downloads and report brigading. A verified member can still act harmfully.
- **SAFE-13 [P0]** Separate moderation, safeguarding and access-control audit permissions. A generic administrator role does not automatically reveal confidential case evidence.
- **SAFE-14 [P1]** Use multilingual automated assistance for triage only after testing relevant Indian languages/code-mixed content. Human reviewers resolve context and contested decisions.
- **SAFE-15 [P0]** No automated guilt, mental-health diagnosis, emotion profiling, student risk score or disciplinary punishment based on feed behavior.
- **SAFE-16 [P0]** Define after-hours/holiday coverage and disable high-risk publishing modes when coverage is absent. Show expectations before users submit.
- **SAFE-17 [P0]** Simulate a mistaken public child image, compromised staff account, bullying thread and report against an administrator before opening participation.

### 17.2 Protect expression as well as safety

Student representation should help draft rules and review whether they are understandable. Separate an unpopular opinion from harm. Schools can moderate behavior without turning the feed into a permanent conduct dossier. Avoid publishing who was sanctioned, ranking “good” students, automatic parent alerts for every rejected draft, or deleting all criticism of school management. Provide a confidential complaint route that remains usable when social posting is restricted.

## 18. Administrator settings and governance catalog

- **CFG-01 [P0]** Enable community by institution and role/age group; retain a platform safety floor that school settings cannot weaken.
- **CFG-02 [P0]** Configure who may create spaces, join, invite, publish, approve, reply, react, download and request public release.
- **CFG-03 [P0]** Set narrow default audiences, historic-content visibility and required review by content type/audience.
- **CFG-04 [P0]** Choose comment modes, moderation owner/deputy, coverage hours, queue thresholds and escalation contacts.
- **CFG-05 [P0]** Control media types, upload limits, storage allowances, retention, allowed attachment types and notification budgets.
- **CFG-06 [P0]** Manage external sharing disabled/enabled separately for private links, approved asset download, user handoff and official connectors.
- **CFG-07 [P0]** Preview effective settings for a specific role/context without impersonating a child or exposing their private data.
- **CFG-08 [P0]** Log setting changes with before/after, actor, policy version and effective time; require appropriate approval for material widening.
- **CFG-09 [P1]** Chain policy can set ceilings/defaults and delegate narrowing to schools. Show inherited versus local settings and conflicts.
- **CFG-10 [P1]** Configure languages, home layout within constraints, approved categories, story duration and institution branding.
- **CFG-11 [P1]** Alumni onboarding, directory fields, batches, verified sponsors and program-specific bridges have separate controls.
- **CFG-12 [P0]** Emergency restrictions apply immediately to active sessions/queued work server-side. Re-enabling does not blindly publish the accumulated backlog.
- **CFG-13 [P0]** School offboarding and staff changes reassign queues, remove publishing powers, revoke destination credentials and address orphaned spaces.
- **CFG-14 [P1]** Offer restrictive preset policies: primary school, secondary guided participation, staff-only, chain publication and alumni adult network. Presets remain reviewable.

## 19. Notifications, delivery, search and accessibility

### 19.1 Notifications and authoritative communication

- **NOTIF-01 [P0]** Separate required official notices from optional social reactions, comments, moments and marketing.
- **NOTIF-02 [P0]** Default optional social delivery to digest/in-app with quiet hours, per-space mute and reasonable caps; no push on every appreciation.
- **NOTIF-03 [P0]** Push/email/SMS/WhatsApp previews contain the minimum safe context. Do not include confidential child records, private body text or protected media URLs.
- **NOTIF-04 [P0]** Recheck eligibility before delivery and when opened; expired/removed/revoked content is not restored by an old notification.
- **NOTIF-05 [P0]** Prevent duplicate digest items and duplicate required notices across multiple child links without obscuring school/child context.
- **NOTIF-06 [P1]** Digests show selected approved text/artifacts and authorized links; a private feed digest does not become a public email gallery.
- **NOTIF-07 [P0]** Delivery failure, acknowledgment, view and reaction are separate states. Never claim a required notice was read because a push was sent.
- **NOTIF-08 [P1]** Optional external channel delivery has separate consent/preferences and provider-supported mechanisms; it cannot replace internal audit or required delivery policy.

### 19.2 Search and discovery

- **SEARCH-01 [P0]** Search only currently entitled content and allowed member fields, applying authorization before results, snippets, counts and suggestions.
- **SEARCH-02 [P0]** Remove restricted/expired/withdrawn content promptly from search exposure; index lag never grants access to stale snippets.
- **SEARCH-03 [P1]** Filter by eligible space, content type, topic, date and verified publisher; support Indic scripts and sensible handling of transliteration.
- **SEARCH-04 [P0]** No global minor discovery, public school roster, cross-tenant people search or bulk directory export.
- **SEARCH-05 [P1]** School FAQ and event discovery support recurring questions without exposing the identity of those who asked privately.

### 19.3 India-first usability and accessibility

- **UX-01 [P0]** Design for low-cost Android devices, modest data plans, intermittent connectivity and shared family/classroom devices; validate these assumptions with pilot schools.
- **UX-02 [P0]** Text-first rendering, small image variants, no autoplay, visible data-saving mode and graceful missing-media behavior.
- **UX-03 [P0]** Keyboard/screen-reader navigation, adequate contrast, scalable text, large tap targets, captions/alt text, reduced motion and accessible report/consent flows.
- **UX-04 [P0]** Begin with English and the languages needed by the chosen pilot. Externalize strings, support Unicode and measure code-mixed content comprehension.
- **UX-05 [P1]** Translation retains the original, labels machine assistance and supports corrections. Human review is required where mistranslation could change safety/consent meaning.
- **UX-06 [P0]** Clear timestamps/timezones, academic year and school context; no ambiguous “today” across institutions when planning global expansion.
- **UX-07 [P0]** Shared-device sign-out clears local protected state; sensitive previews are hidden by default and device notifications disclose minimal detail.
- **UX-08 [P0]** Offline reading is bounded by explicit policy; protected caches have short permitted lifetimes and purge on known revocation/sign-out. Do not promise recall of already viewed offline data.
- **UX-09 [P0]** Offline drafts remain drafts. Reconnect revalidates role, audience, permissions and version before submission/publication.
- **UX-10 [P1]** Consider assisted voice input and audio alternatives with explicit microphone action and transcripts; no ambient classroom recording.
- **UX-11 [P0]** A family with no smartphone gets an agreed alternative for essential school communication; social participation is not a prerequisite for learning.

## 20. International expansion strategy

Keep India-specific legal rules in a versioned jurisdiction policy layer rather than hardcoding one age, identity method or retention period into every product concept. The launch unit is a school/institution with an agreed jurisdiction, contractual data roles, languages, approved vendors and operational owner.

The US COPPA school-consent route is limited to educational contexts and cannot simply authorize unrelated commercial use; evaluate COPPA, student-record rules and state obligations before launch. The UK Children’s Code emphasizes high-privacy defaults and limits on unnecessary geolocation/profiling. These support a restrictive architecture but do not establish worldwide compliance. [FTC COPPA school FAQs](https://www.ftc.gov/business-guidance/resources/complying-coppa-frequently-asked-questions), [ICO Children’s Code standards](https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/childrens-information/childrens-code-guidance-and-resources/age-appropriate-design-a-code-of-practice-for-online-services/code-standards/).

- **GLOBAL-01 [P1 design / per-country release]** Specify applicable ages, age assurance, school/guardian authority, child rights, notices and acceptable verification methods.
- **GLOBAL-02 [P1 design / per-country release]** Specify safeguarding/reporting, grievance/takedown deadlines and appeal procedures with local ownership.
- **GLOBAL-03 [P1 design / per-country release]** Review residency, transfer, subprocessors, security, record-access/deletion and school contracts.
- **GLOBAL-04 [P1 design / per-country release]** Review school calendars, grading/curriculum terminology, languages/RTL, timezone, accessible formats and local emergency pathways.
- **GLOBAL-05 [P1 design / per-country release]** Review social media age/account rules, destination availability, publicity/image rights, copyright/music and marketing law.
- **GLOBAL-06 [P1 design / per-country release]** Assess whether local social-media age restrictions cover this product or provide an education-related exception; never assume an LMS label settles classification.
- **GLOBAL-07 [P2]** Federation between independently controlled schools/countries needs a separate product, legal agreement and security design; country expansion does not imply a global student graph.

## 21. Proposed technical design within MySMS boundaries

This section defines responsibilities and design constraints. It proposes no migrations, source edits or API contract changes in this exercise.

### 21.1 Start inside comms

Use `comms` for community spaces, publication workflows, member-facing feed projections, comments/reactions, routine moderation and external release orchestration. Keep internal components bounded so a later service extraction is possible if traffic, deployment or ownership requires it. Creating a new community service immediately would duplicate existing post/page/notice concepts and add consistency problems before scale has been measured.

Identity remains IAM; institution structure remains org; person/consent ownership remains people; student/guardian relationships remain SIS; employment remains HR; learning remains LMS; file infrastructure and central audit remain platform. A confidential safeguarding case may belong in a separately restricted capability; routine social moderation must not quietly become a general school case-management product.

### 21.2 Conceptual domain objects

The following concepts are requirements for design, not instructions to create a table for each noun:

1. **School relationship:** source owner/record/version, person/account association, institution, relationship type, validity, verification and revocation state.
2. **Community space:** purpose, owner, institution scope, membership policy, age policy, visibility/history policy, moderators and lifecycle.
3. **Space membership:** eligibility evidence, explicit join/follow preferences, contribution privileges, effective dates and removal reason restricted to appropriate staff.
4. **Publication:** stable identity, author/delegation, owning space, type and current ordinary-visibility state.
5. **Publication revision:** immutable text/assets/credits, intended audience, review-relevant metadata and exact version/hash.
6. **Audience policy/revision:** participating institutions and role/relationship rules, exclusions, history behavior and policy version. Snapshot intention for audit while checking current entitlement for every read.
7. **Media asset/variant:** quarantined original, approved derivatives, processing state, subject declarations, metadata, expiry and delivery authorization.
8. **Permission decision:** references to authoritative permission/consent evidence and the content, person, purpose and destination it covers; never a competing unverified consent authority.
9. **Moderation report/case/action/appeal:** protected reporter, owner, severity, evidence, reasons, outcome and restricted review history.
10. **External release package/job/result:** exact approved derivative and caption, destination, actor/approver, dispatch status, known external identity and recall status.
11. **User preference/read state:** follows, mutes, language, digest choice, last-seen marker and personal saves, separated from eligibility and required notice acknowledgments.
12. **Alumni verification/program grant:** historical affiliation, opt-in status, adult/minor policy and tightly bounded mentorship/volunteer grants.

### 21.3 Commands and state machines

Prefer domain commands such as submit, return for changes, approve exact revision, request broader audience, withdraw, report, restrict, appeal, expire and release approved derivative. Generic CRUD remains an administrative tool only where it enforces the same invariants. Setting `status=posted`, `authorUserId`, `isActive`, audience records or attachment IDs through another route must not bypass the intended workflow.

**Publication:** draft → submitted → in review → published; review may return/reject; published may be superseded, withdrawn, restricted or expired. Scheduled is a dispatch condition on an approved revision, not proof of eligibility at dispatch. Reopening a moderation case does not automatically republish its content.

**Moderation:** reported → triaged → restricted/investigating → resolved → appealed/reviewed where applicable. Preserve minimum required evidence independently of visible publication state.

**External release:** prepared → pending approval → approved → queued → dispatched → confirmed/failed/unknown → recall requested → removed/partial/failed/unknown. A timeout may leave external publication successful but unconfirmed. Model uncertainty rather than reporting “failed” and issuing an unsafe duplicate.

**Membership:** candidate/invited → verified/active → suspended/expired/revoked → eligible for a separately verified relationship. Graduation changes relationship eligibility, not the account’s universal trust level.

### 21.4 Authorization decision path

For each action, establish trusted account/tenant context, required capability, active source relationship and freshness, institution/space eligibility, age/safety restrictions, current publication state/expiry, exact audience rules and any applicable permission/consent. Then evaluate action-specific restrictions and produce a scoped response. Record privileged disclosure decisions without putting private content in ordinary logs.

The same decision path protects feed/detail, nested replies, reaction APIs, upload binding, thumbnail/full-media delivery, notifications, search, exports, moderation and administrator record routes. Disallow access before producing a count, snippet or media URL. Owner checks for another module must honor its API’s authorization and the module dependency graph.

### 21.5 Events, projections and revocation

- **ARCH-01 [P0]** No cross-schema SQL, cross-module foreign keys or distributed transactions. Follow module-local JDBC, programmatic transaction, outbox/inbox, audit, idempotency and optimistic-concurrency conventions.
- **ARCH-02 [P0]** Define event contracts for enrollment/class changes, guardian links, employment, account/role status, institution changes, permission withdrawal, publication state and asset state through approved owners.
- **ARCH-03 [P0]** Each relationship projection stores source identity/version and effective time; duplicate/late events cannot resurrect revoked membership or withdrawn permission.
- **ARCH-04 [P0]** Read projections and cached feed candidates are performance aids. They do not override current denies, expiry or takedown state.
- **ARCH-05 [P0]** Establish an explicit revocation barrier/freshness contract before launch. A revocation must not be reported complete while live protected routes still grant access from a stale projection.
- **ARCH-06 [P0]** High-risk actions—external release, audience widening, grants, sensitive report access—require current authoritative evidence or an equivalent verifiable current decision. Fail closed when freshness cannot be established.
- **ARCH-07 [P0]** Ordinary online content access has a documented maximum relationship/deny freshness window and emergency invalidation mechanism. Measure source-change-to-enforcement, not just event delivery.
- **ARCH-08 [P0]** Use reconciliation jobs, lag monitoring and operator repair to detect missing membership/permission updates. Do not assume outbox delivery makes all downstream effects instantaneous or magically exactly once.
- **ARCH-09 [P0]** Event payloads contain minimum identifiers/version/context, not copies of confidential learning, health, fee or safeguarding records.
- **ARCH-10 [P0]** Approval binds a specific revision and expected version; concurrent edits, moderator actions and retries cannot replace approved assets or publish twice.
- **ARCH-11 [P0]** Evolve contracts, permissions, state documentation, generated clients and conformance rules together when implementation is authorized. Do not add broad wildcard social grants.
- **ARCH-12 [P0]** Home composition handles partial module failure independently and does not introduce circular domain dependencies.

The existing graph does not permit comms → people/HR/LMS SDK reads. Define approved owner events/projections and revocation signals first; if a direct authoritative API is necessary, review the graph deliberately. Neither a convenient cross-schema query nor a permanently stale local consent copy is acceptable.

### 21.6 Feed, media and integration infrastructure

Start with a query-based chronological feed over local publications, audience relationships and deny state, using stable seek/cursor pagination. Index by measured tenant/space/status/time access patterns. Filter eligibility before page boundaries and avoid leaking hidden totals. Consider hybrid fanout, partitioning or dedicated search only when measured traffic warrants it.

Uploads use private sessions, quarantine, bounded processing, scanning, metadata stripping and explicit binding to eligible content. Media visibility follows the content’s state and rights. Review authenticated proxy delivery versus short-lived signed grants, including already issued URL revocation; a signed URL is a bearer grant and cannot be called instantly revocable without an actual enforcement mechanism.

Use durable jobs for scanning, thumbnails/transcoding, expiry, notifications, search updates, export dispatch and deletion. Job state has bounded retries, dead-letter/repair visibility and idempotent handlers. A failed worker must not bypass read-time expiry or make quarantine assets public. External-provider failure must not block internal school tasks.

## 22. Security, reliability and nonfunctional requirements

- **NFR-01 [P0]** Reuse trusted authentication/session controls; require stronger authentication for high-impact publishing/moderation/admin functions according to the IAM policy.
- **NFR-02 [P0]** Tenant RLS plus application predicates remain enforced for community data. Add object/relationship authorization within the tenant.
- **NFR-03 [P0]** Validate and sanitize user content; prevent stored XSS, unsafe HTML/links, injection, CSRF where applicable, malicious file processing and cross-tenant object reference abuse.
- **NFR-04 [P0]** Do not fetch arbitrary user-provided URLs from privileged networks. Any future preview fetcher needs SSRF defenses, safe egress and no credential forwarding.
- **NFR-05 [P0]** Encrypt transport and sensitive stored data, protect tokens/keys, separate service identities and minimize privileged support access.
- **NFR-06 [P0]** Prevent enumeration through IDs, member search, error differences, invitation/recovery forms, counts, cached assets and unpublished content.
- **NFR-07 [P0]** Rate limits and quotas apply per actor/space/tenant and to expensive media/search/export actions, with fair retry guidance.
- **NFR-08 [P0]** Audit privileged decisions, policy changes and sensitive report access without copying protected content into general telemetry.
- **NFR-09 [P0]** Backup/restore, deletion, permission withdrawal, credential compromise and mistaken-publication runbooks are exercised before the pilot.
- **NFR-10 [P0]** Feature flags and kill switches operate by institution/capability, with preservation of required school access, incident evidence and privacy requests.
- **NFR-11 [P0]** Define and test request, upload, media-processing and notification limits. Backpressure never auto-publishes unchecked content.
- **NFR-12 [P0]** Monitor availability, first-page latency, relationship freshness, removal propagation, media failures, queue age, notification failures, storage/egress and export uncertainty.
- **NFR-13 [P0]** Accessibility and performance are validated on representative web/mobile/tablet devices and pilot languages, including report/consent flows.
- **NFR-14 [P1]** Maintain compatibility across deployed mobile client versions; server enforcement does not depend on clients having updated UI settings.
- **NFR-15 [P1]** Disaster recovery and tenant export meet contractual objectives measured through drills; define regional and vendor outage fallbacks.

### 22.1 Initial measurable targets — proposed, not promises

Validate with the pilot before contracting these numbers:

- 99.9% monthly authorized feed-read availability, excluding clearly defined planned outages only if the contract allows them.
- Warm first-page API p95 under 800 ms on the agreed load profile; full usable text home p95 under 2.5 seconds on the agreed representative low-end device/network test. Measure API and client experience separately.
- Text submission acknowledgment p95 under 2 seconds excluding review; image processing has its own observed queue target and visible status.
- Read-time expiry denial at the expiry instant in server policy; asynchronous purge completion has a separate measured objective.
- Proposed online source-revocation-to-enforcement objective of 60 seconds or better, with stronger synchronous/current checks for high-risk actions. Finalize the barrier semantics in ARCH-05 before making any “immediate” claim.
- Urgent content takedown objective agreed after an incident drill, including media variants and CDN behavior; never assert universal removal of offline copies.
- Suggested pilot editorial service: ordinary submissions reviewed within one school working day during covered hours; serious safety reports use an explicitly staffed escalation route and target agreed with the safeguarding owner. This is a proposed operating arrangement, not a statement of statutory timelines.

Keep security/correctness objectives separate from latency goals. A fast feed that leaks one class’s content into another class is a failed release.

## 23. Sizing, costs and sustainable pricing

### 23.1 Variables to measure

Track eligible members versus active members; read sessions/pages; staff/student submissions; approved ratio; image/video sizes and variants; retention; media views/cache hit ratio; notification channels; review minutes; reports/escalations; support contacts; language/translation volume; search frequency and provider jobs. Keep usage reporting privacy-preserving and avoid exposing small child cohorts.

### 23.2 Transparent planning example

The following is an illustrative load case, not a forecast or vendor quote:

- A chain of 10 schools with 1,000 students each has 10,000 students. Add an assumed 8,000 guardian accounts and 1,000 staff accounts: 19,000 eligible accounts. Guardians with siblings may reduce account counts.
- Assume 50% daily activity: 9,500 active accounts/day. At 3 opens and 2 feed pages per open: **57,000 page reads/day**.
- Assume 1,000 school/space updates per day across the chain and 2 images per update, each with a 0.5 MB original: **1 GB new originals/day**. At 1.5× total storage including variants: roughly **45 GB per 30-day month** and **540 GB per 360-day retention period**, before backups/evidence. Individual schools may post far less; student publishing could raise this substantially.
- Assume each daily active account loads 20 compressed images of 0.12 MB: **22.8 GB client delivery/day**, roughly **684 GB per 30 days**. CDN cache hit reduces origin load, not necessarily all delivery charges.
- Assume 30% of 1,000 submissions need 45 seconds of review: **225 staff minutes/day**, or **3.75 hours/day**, excluding incident handling. This may be distributed among schools, but must still be funded/staffed.
- Adding 100 clips/day of 10 MB creates **1 GB/day of video originals before variants**; repeated viewing and processing can exceed image costs quickly. This is why video is a separate gate.

Units use decimal MB/GB for this example. Arithmetic is intentionally simple; collect pilot telemetry before forecasting peak concurrency or annual spend.

### 23.3 Cost model

Monthly cost = baseline application/database operations + retained original/variant/backup storage + image/video processing + CDN/media delivery + notifications/provider API charges + optional translation/triage + staff moderation/escalation + onboarding/support/compliance operations.

School price must cover both software and human responsibilities. No rupee price is asserted without current provider quotes and customer willingness-to-pay research. Offer bounded allowances, understandable overage alerts, soft caps and a predictable path to buying capacity. Never block report submission, essential notices or a privacy request because a school hit its social media quota.

### 23.4 Pilot outcome metrics

- **Usefulness:** eligible families who can find required information, notice comprehension, task completion and reported reduction in duplicate communication.
- **Participation quality:** distinct voluntary contributors, representation across activity types, student/guardian usefulness ratings and quiet-user feedback.
- **Trust:** audience-preview comprehension, mistaken disclosure, permission/withdrawal success, opt-outs and corrected identity relationships.
- **Operations:** review minutes per 100 submissions, queue age, report response/resolution, staff after-hours burden and unresolved cases.
- **Economics:** media bytes and delivery per active institution/member, support effort and gross contribution after relevant operating costs.
- **Guardrails:** no decline in essential task completion, no unowned serious report, no unresolved authorization leak, no growth in compulsory/pressured posting.

Do not optimize child screen time, streak completion, follower growth, controversial content or individual popularity. A rise in reports can indicate better access to reporting; interpret with investigation and surveys rather than treating low report counts as proof of safety.

## 24. Delivery sequence and release gates

### Phase 0 — discovery and operating design

Interview leadership, communications staff, teachers, students across ages, guardians, support staff and alumni. Include low-connectivity users and families who do not post. Map current WhatsApp/notice/LMS workflows and baseline staff time. Validate the institution/chain structure against real customer deployment. Agree data roles, rules, moderation coverage, audience matrix, history, consent semantics, retention and the source of revocation.

**Exit gate:** named accountable school owners, understood privacy/safety policies, tested audience prototype, a scoped technical design for missing feed/media/consent workflows and no unresolved critical dependency decision. Research can proceed without collecting real children’s content.

### Phase 1 — P0 school/class community vertical slice

Ship useful role home, verified school/class access, text and approved images after their dependencies are complete, staff publishing, bounded student submissions in selected groups, chronological feed, narrow comments/reactions, report/triage/withdrawal, audit, consent flows, quiet notifications and emergency restrictions. Start with comments off except in covered pilot spaces. No stories or external public publication is required for this phase.

**Exit gate:** complete submit → approve → eligible read → report → restrict/withdraw; direct-route and generic CRUD negative tests; media denial; consent withdrawal; transfer/guardian/staff revocation; accessibility and low-bandwidth usability; trained reviewers and demonstrated incident drill. Text-only subpilot is viable while image gates remain closed.

### Phase 2 — P1 moments and meaningful participation

Add accessible story presentation/expiry, structured Q&A/polls, more student/club participation, event galleries, reviewed portfolio showcases and selected memory artifacts. Expand participation only where the review queue and appeals process work.

**Exit gate:** expiry and evidence behavior are understandable and tested; teacher workload stays within agreed capacity; students can correctly identify audiences; quiet users retain essential access.

### Phase 3 — selected same-tenant chain spaces and alumni

Introduce explicit cross-campus projects/newsrooms and a separate verified alumni pilot. These are separate feature flags and can ship independently. Alumni begins with opt-in batches/events and restricted adult contact discovery. Minor alumni initially receive curated bulletins/event information only, with no adult directory/contact or recruiting access; minor bridges remain closed until separately approved.

**Exit gate:** no unintended widening through hierarchy/history, current-minor isolation, verified lifecycle/age transitions and named chain/alumni owners.

### Phase 4 — approved external publication

Introduce a school editorial desk, reviewed public derivatives and controlled manual handoff. Add one official-account connector only after current API/account/permission verification and a tested withdrawal/unknown-outcome workflow. Choose the first destination based on real school demand and feasible supported APIs, not a universal promise to support every format on all three platforms.

**Exit gate:** exact-version approvals, identifiable-subject permissions, destination rendering, revoked-permission dispatch blocks, credential offboarding, duplicate/unknown-result handling, kill switch and honest recall status.

### Phase 5 — country expansion and optional advanced programs

Apply local policy packs, language/device tests, school contracts and operational coverage. Evaluate supervised mentorship, richer adult alumni opportunities and selected advanced media. Cross-tenant federation, live streaming and open social graphs are not automatic later phases.

### 24.1 Timing and stopping rules

A reasonable **planning hypothesis** is 2–4 weeks of discovery followed by an 8–12 week limited-school observation period once the P0 slice is ready. These are validation windows, not implementation estimates. Delivery effort cannot be credibly fixed until missing authorization, media, consent and client workflows are scoped with the team.

Stop expansion for an authorization/data disclosure, inability to process a serious report, repeated unexplained permission failure, an unowned queue, unsustainable staff load or material interference with required school tasks. Narrow or disable the affected capability, preserve necessary evidence, investigate, repair and rehearse before reopening.

## 25. Acceptance and verification scenarios

These are future implementation acceptance requirements. No tests or source code are added in this documentation task.

1. **ACC-01 — cross-tenant denial [P0]:** a tenant A member cannot list/get/search/preview/download tenant B content using guessed IDs, copied URLs, comment/reaction routes or a stale notification.
2. **ACC-02 — class separation [P0]:** a Class 8A guardian cannot read Class 8B content unless independently entitled; an institution-wide read grant intended for administration is not accidentally issued to guardians.
3. **ACC-03 — multiple children [P0]:** a guardian switches school/child and sees labeled appropriate content, with no exposure of another guardian’s relationship, contact or restriction.
4. **ACC-04 — staff/parent dual role [P0]:** authoring context is visible; parent mode does not publish as staff or expose unrelated staff-room posts.
5. **ACC-05 — invitation leakage [P0]:** a photographed public QR/invitation cannot grant an unverified stranger class access; used/revoked/expired invitations fail safely.
6. **ACC-06 — author spoofing [P0]:** client-supplied author, official badge, moderation status and attachment references cannot impersonate another person or bypass review through generic CRUD.
7. **ACC-07 — pending privacy [P0]:** an unapproved student draft/submission is visible only to the author and authorized reviewers; ordinary search/counts/previews do not reveal it.
8. **ACC-08 — versioned approval [P0]:** approving one image/caption/audience revision does not approve a concurrently edited replacement; optimistic-concurrency conflicts are handled without silent overwrites.
9. **ACC-09 — revocation [P0]:** guardian link removal, transfer, staff exit and account suspension stop protected routes under the defined revocation barrier/freshness objective; late events do not restore rights.
10. **ACC-10 — history/rollover [P0]:** a new class roster does not inherit old class media accidentally; existing users do not gain another campus’s history when the chain changes.
11. **ACC-11 — confidential data boundary [P0]:** authorized home result/fee/health/discipline cards never enter a shared social response, preview, digest or search result.
12. **ACC-12 — independent LMS learner [P0]:** a course buyer with no school relationship cannot discover/join schoolchildren’s class/club spaces.
13. **ACC-13 — media quarantine [P0]:** unscanned or invalid files produce no ordinary-viewable URL/thumbnail; private attachments cannot be rebound by guessing an asset ID.
14. **ACC-14 — withdrawal [P0]:** removing the applicable permission restricts affected media and blocks pending publication; derivatives/caches/jobs have observable cleanup states.
15. **ACC-15 — bystanders [P0/P1 export]:** one child’s permission does not clear a group photo; unknown/minimally redacted subjects do not receive an automatic “safe for public” decision.
16. **ACC-16 — report against staff [P0]:** a pupil can report an adult-authored post to a restricted independent reviewer without exposing their identity to the accused or ordinary class moderators.
17. **ACC-17 — reporting under restriction [P0]:** a suspended/muted user retains the approved safety/complaint route; block/mute does not erase official notices or report evidence.
18. **ACC-18 — queue outage [P0]:** an unstaffed queue does not publish pending content automatically; deputies/escalation and pause controls work.
19. **ACC-19 — provider/community outage [P0]:** failed community queries or social connectors do not block timetable, homework, attendance or essential school actions.
20. **ACC-20 — notification privacy [P0]:** a lock-screen preview reveals no private child text/media; opening a stale link rechecks eligibility and removed/expired state.
21. **ACC-21 — shared-device recovery [P0]:** sign-out clears protected state, old child/tenant views do not flash on account switch, and a recycled phone cannot recover someone else’s school identity.
22. **ACC-22 — offline reconnect [P0]:** a draft created while offline is revalidated after transfer/permission withdrawal; old caches are purged when revocation is learned, with residual offline limits accurately disclosed.
23. **ACC-23 — stories [P1]:** expiry blocks detail and media reads even with a delayed worker; a prior report remains restricted evidence and does not keep the story generally visible.
24. **ACC-24 — alumni transition [P1]:** a graduate receives new opt-in eligibility, loses current class access and remains under minor policy if under 18; adult alumnus status alone cannot message current students.
25. **ACC-25 — context-specific alumni [P1]:** an alumnus who is also a current guardian sees child content through that verified child link only; the alumnus directory cannot discover classmates of the child.
26. **ACC-26 — chain release [P1]:** an approved single-campus item remains single-campus until an explicit reviewed widening; a newly added campus does not automatically see old restricted content.
27. **ACC-27 — external disable [P1/P2]:** server-side policy blocks handoff/download/connector work even from old clients or queued jobs after export is disabled.
28. **ACC-28 — export revision/consent [P1/P2]:** changed caption/media, revoked permission or changed destination stops dispatch until the required approval is renewed.
29. **ACC-29 — external uncertainty [P2]:** a timeout after provider acceptance creates a reconciled/unknown outcome rather than duplicate publication; webhook replay does not duplicate results.
30. **ACC-30 — recall [P1/P2]:** external removal is attempted where supported; partial/unconfirmed removal and prior manual downloads are visible to authorized staff without claiming full recall.
31. **ACC-31 — accessibility/language [P0]:** student/guardian can understand audience, report and consent controls using the pilot languages, screen reader and low-data device; essential tasks remain usable without media.
32. **ACC-32 — abusive input [P0]:** stored scripts, malicious files, unsafe link previews, mass mentions, scraping and credential abuse are blocked or contained with useful nonleaking errors.
33. **ACC-33 — deletion/restore [P0]:** normal deletion, evidence hold, backup restore and tenant exit do not resurrect withdrawn public visibility or lose the required restricted audit trail.
34. **ACC-34 — no forced participation [P0]:** a child/guardian opting out of optional social publishing still receives required learning/notices and can use safety/privacy workflows.
35. **ACC-35 — staff capacity [P0/P1]:** pilot review and reporting loads fit named coverage, or the system narrows participation without penalizing students for unreviewed submissions.

Implementation validation should combine policy/state-machine unit checks, real PostgreSQL/RLS integration checks, adversarial authorization tests over every route, media delivery/withdrawal checks, complete client journeys, representative load/failure tests and supervised usability sessions. Avoid a large suite that merely repeats table CRUD while missing these workflows.

## 26. Alternatives, exclusions and avoidable traps

- **An extra generic feed tab:** easy to demonstrate but weak daily value. Put useful relevant community on the existing home and preserve task navigation.
- **All members can see all content:** simple to explain until a parent sees another class, a campus sees another campus or alumni see children. Use explicit relationships/spaces.
- **Every student needs a public social identity:** unnecessary for younger children and incompatible with narrow participation. Allow mediated contributions.
- **Mandatory posting or social attendance:** creates coercion and invalidates engagement metrics. Recognize voluntary contribution without grading participation.
- **Every post requires the principal:** operational bottleneck. Delegate bounded authority and use stronger review for broader audiences and public release.
- **Every teacher moderates constantly:** unsustainable. Give named coverage, bounded queues, quiet hours, deputies and pause controls.
- **Reaction/follower leaderboards:** likely to amplify status competition and discourage quiet pupils. Exclude popularity mechanics.
- **Private child content with public thumbnails:** fails the boundary despite protected detail APIs. Protect all derivatives/unfurls.
- **A signed form means all future sharing:** confuses purposes and depicted people. Use current applicable evidence and exact-release decisions.
- **Stories vanish everywhere:** impossible to promise for screenshots, exports and backups. Separate ordinary expiry from retention/evidence.
- **Alumni means adult or permanently trusted:** false lifecycle assumption. Verify age/role and create separate access.
- **Public social posting is a universal share API:** provider routes/account requirements differ and change. Offer reviewed derivatives and validate connectors separately.
- **AI moderates the school:** misses local language, context and authority dynamics. Use assistance with accountable human escalation.
- **Rebuild notices, chat, LMS and files inside social:** duplicates owners and weakens records. Compose existing capabilities through approved boundaries.
- **Global launch by changing language:** misses law, school authority, safeguarding, data roles and operating coverage. Use country release gates.

**Excluded from the initial product:** open public/student discovery, anonymous confession feeds, unrestricted adult-minor contact, live location maps, biometric age/face recognition, behavioral advertising, emotion/wellbeing scoring, public student popularity metrics, fee-defaulter shaming, automated disciplinary decisions, public livestreaming and cross-tenant student federation.

## 27. Distinctive opportunities worth testing

1. **School Pulse:** a finite weekly editorial selection across learning, arts, sport, service and staff life, with a coverage view for editors rather than a student ranking.
2. **Class journal:** teacher creates one accessible weekly recap, reusing reviewed artifacts and generating an authorized family digest without duplicate entry.
3. **Ask the School:** searchable verified general answers with routing to the right school owner, reducing repeat parent questions in unmanaged groups.
4. **Contribution portfolio:** student chooses approved highlights and reflections to preserve; no inferred reputation score or compulsory publicity.
5. **Permission-aware memories:** selected events/stories become an opt-in archive or yearbook only through a fresh retention/audience decision.
6. **Chain exchange:** schools share selected teaching practices and student projects without exposing raw rosters or student social graphs.
7. **Alumni return paths:** batch events → school-hosted career panels → adult opportunities → carefully supervised programs, each with a separate permission step.
8. **Editorial publication desk:** approved public assets/captions for school communications teams, with destination previews and documented rights rather than repeated copying from private chats.

Treat these as experiments. Research whether families value authentic everyday learning more than polished publicity, whether students trust school-operated expression, and whether teachers can contribute without another administrative burden.

## 28. Open decisions and customer discovery backlog

### 28.1 Decisions needed before P0

1. What exactly constitutes the first customer’s tenant/chain/institution structure, and does any chain currently span tenants?
2. Which student ages/grades and school types enter the first participation pilot? Who verifies age when grade and age differ?
3. Which relationships authorize reading and posting, and who owns their timely revocation?
4. What can guardians see of student-authored content, rejected drafts and private reporting? What restricted exceptions apply?
5. Do new members see history, and what happens after rollover, transfer, withdrawal and staff exit?
6. What permissions are necessary for internal child media, and what is the practical process for bystanders, objections and conflicting guardian instructions?
7. Who moderates, handles appeals and provides safeguarding coverage during evenings/holidays? What capacity does the school actually have?
8. Which essential notices require acknowledgment or alternate delivery, and how does community coexist without obscuring them?
9. Which languages, devices, shared-phone arrangements and accessibility needs are required by the pilot schools?
10. What retention/deletion/evidence/exit policy can the school explain and operate?
11. What event/projection or reviewed API design meets consent/employment freshness without breaking the module graph?
12. Which existing generic routes/permissions must be narrowed before end users gain community access?

### 28.2 Decisions before later features

13. Is an alumni directory desired, or do reunions/batch updates solve the initial need with less disclosure?
14. Who verifies historic alumni and owns the network after school/tenant ownership changes?
15. Which same-tenant chain spaces are useful enough to justify cross-campus student disclosure?
16. Which external destination and format does the school actually need first, and can its official account use a supported integration?
17. Who approves public release, and what happens when permission is withdrawn after a manual handoff?
18. Can media cost and review workload be supported by the school subscription? Which allowance/pricing model is understandable?
19. Are mentorship, fundraising, jobs or public publishing separate paid/operated products rather than small feed features?
20. Which next country has enough validated demand to fund a local policy/operations launch?

These are discovery inputs and future release decisions. Resolve the relevant questions before implementing or opening each capability.

## 29. Market reference points and source notes

School story feeds and learning portfolios already have established examples. ClassDojo describes class/school updates for connected families, Toddle describes communications and private/family portfolios. The opportunity is therefore not novelty of a school feed. **Inference:** MySMS differentiation should come from integrated workflow context, membership lifecycle, chain governance, India-first usability, alumni continuity and reliable publication permissions. Product pages describe advertised behavior, not independently verified outcomes or a complete competitive evaluation. [ClassDojo class/school stories](https://help.classdojo.com/hc/en-us/articles/211889103-What-is-Class-Story-and-School-Story), [Toddle communications](https://www.toddleapp.com/product/communications-hub/), [Toddle portfolios](https://www.toddleapp.com/product/student-portfolios/).

Primary external sources consulted on 11 November 2026:

- [India DPDP Act, 2023](https://www.meity.gov.in/static/uploads/2024/06/2bf1f0e9f04e6fb4f8fef35e82c42aa5.pdf): child definition and statutory framework.
- [DPDP Rules, 2025 official Gazette PDF](https://www.meity.gov.in/static/uploads/2025/11/53450e6e5dc0bfa85ebd78686cadad39.pdf): commencement, verifiable parental consent and education-related limits. Exact current consolidated text/dates need prelaunch review.
- [DPDP Act commencement notification](https://www.meity.gov.in/static/uploads/2025/11/c56ceae6c383460ca69577428d36828b.pdf): phased application.
- [FTC COPPA FAQs, schools section](https://www.ftc.gov/business-guidance/resources/complying-coppa-frequently-asked-questions): US expansion discovery.
- [ICO Children’s Code](https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/childrens-information/childrens-code-guidance-and-resources/age-appropriate-design-a-code-of-practice-for-online-services/code-standards/): UK expansion discovery.
- [WhatsApp official click-to-chat help](https://faq.whatsapp.com/5913398998672934) and [Meta business messaging controls](https://about.fb.com/news/2025/04/ways-to-manage-your-businesses-chats-on-whatsapp/): user handoff versus business messaging.
- [Meta’s Instagram API collection](https://www.postman.com/meta/instagram/documentation/6yqw8pt/instagram-api?entity=request-23987686-10bf8bf7-d9b7-4340-9c66-61d3ce683a87): published account/login constraints; exact modern endpoint support remains unconfirmed where direct docs were unavailable.
- [X create-post documentation](https://docs.x.com/x-api/posts/create-post): authenticated official publication surface.

Architecture, priorities, example targets, cost assumptions and product recommendations are proposals grounded in the intended school ecosystem and repository findings. External legal/API sources inform constraints; they are not promises that MySMS currently implements them. Revalidate changing law, provider access and commercial terms before any release.

## 30. Product decisions and validation

The recommended direction balances student expression, school operations, technical boundaries and the changing rights of community members.

### 30.1 Governing principles

Use a closed verified community, a useful operational home, explicit audiences, narrow defaults, human-owned moderation, honest story expiry, separate alumni eligibility and controlled external release. Revenue and success metrics should support school usefulness and sustainable operation rather than children's attention or popularity.

Existing tables and contracts are foundations. Release readiness depends on complete authorized workflows, effective withdrawal and a school team capable of operating them.

### 30.2 Tradeoffs and chosen direction

- **Student agency:** early expression gives the community value, while publishing without adequate authorization or coverage creates avoidable harm. **Decision:** staff publishing plus selected moderated student submissions in P0; broaden direct participation only where age policy and coverage support it.
- **Stories at launch:** visual stories can encourage adoption, but require reliable media access, expiry and retention behavior. **Decision:** approved images after P0 media gates, story presentation in P1, text-only pilot permitted while dependencies are unfinished.
- **Alumni timing:** lasting school connection is valuable, but former members require a different access lifecycle. **Decision:** define the transition before launch, ship a separate alumni pilot after current-school rights are reliable.
- **Review strictness:** central approval is operationally simple but may suppress classroom conversation and overload leadership. **Decision:** delegated bounded authority, risk-based review and a stronger gate for wider/public audiences.
- **Home composition:** household summaries can save time but must preserve child, school and role boundaries. **Decision:** summaries are allowed only with obvious source labels and separate authorization; publishing always requires an explicit context.
- **Sharing:** a simple share button is attractive, but release permissions and provider capabilities vary. **Decision:** private authenticated links first; approved public derivatives and connectors are separate phased capabilities.
- **Architecture:** a universal community graph could support more futures; adapting comms offers less duplication. **Decision:** keep the initial bounded community in comms, with explicit relationship/space semantics and preserved module rules; extract only with evidence.

### 30.3 Concrete first product to validate

A user logs in, sees school/role/child context, required actions and a small set of relevant school/class updates. A teacher can publish or submit a reviewed recap once. Selected students can submit work to an accountable reviewer. A guardian can understand why they see an item, choose their language/digest, manage permissions and report a concern. Transfers and withdrawal actually change access. Schools can operate the queue without creating a new full-time burden. Later, the same verified relationship system can support clubs, moments, chain collaboration and an alumni community with deliberately different rights.


