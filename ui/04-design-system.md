# 04 · Design system

One visual and interaction language for all six clients. Ships as two packages: `@mysms/tokens` (platform-neutral design tokens) and the component libraries `@mysms/ui-web` / `@mysms/ui-native` built on them (05 §1).

## 1. Principles

1. **Calm utility.** School staff live in these screens for hours: quiet surfaces, strong hierarchy, dense-but-breathable tables, colour reserved for status and action.
2. **One language, native feel.** Same tokens, iconography, terminology and patterns everywhere; platform idioms (navigation transitions, pickers, haptics) stay native.
3. **Tenant-themed, never tenant-forked.** Institutions restyle through tokens only (§2).
4. **Status is systematic.** Every workflow state maps to one of six semantic intents (§2.2) — no ad-hoc colours.
5. **Accessible and localised by default** (§6, §7) — they are acceptance criteria, not polish.

## 2. Design tokens

Tokens are defined once (JSON), exported as CSS variables (web) and a typed theme object (React Native). Naming: `--sms-<group>-<name>`.

### 2.1 Base palette (default theme)

Neutral default; tenants override brand tokens (§2.4).

| Token | Light | Dark | Use |
|---|---|---|---|
| `color.primary` | #4F46E5 | #818CF8 | Brand actions, active nav, focus |
| `color.on-primary` | #FFFFFF | #1E1B4B | Text/icons on primary |
| `color.primary-container` | #EEF2FF | #312E81 | Selected rows, chips |
| `color.secondary` | #0D9488 | #2DD4BF | Secondary emphasis (learning surfaces) |
| `color.bg` | #F8FAFC | #0B1220 | App background |
| `color.surface` | #FFFFFF | #111A2C | Cards, sheets, tables |
| `color.surface-2` | #F1F5F9 | #1A2538 | Table headers, wells |
| `color.border` | #E2E8F0 | #27334A | Hairlines, inputs |
| `color.text` | #0F172A | #E6EAF2 | Primary text |
| `color.text-muted` | #64748B | #94A3B8 | Secondary text |

### 2.2 Semantic intents

| Intent | Light | Use |
|---|---|---|
| `intent.success` | #16A34A | Present, paid, published, passed, approved |
| `intent.warning` | #D97706 | Late, pending, due soon, draft-needs-action |
| `intent.danger` | #DC2626 | Absent, overdue, rejected, failed, destructive actions |
| `intent.info` | #0284C7 | Informational, scheduled, in-progress |
| `intent.neutral` | #64748B | Inactive, archived, not applicable |
| `intent.special` | #9333EA | LMS release/locks, evaluations, badges |

Each intent ships `*, on-*, container, on-container` variants; status chips always use container pairs (AA contrast guaranteed by the token build, §6).

### 2.3 Type, space, shape, elevation, motion

- **Fonts**: `Inter` (UI) + `JetBrains Mono` (code/exercise); Devanagari and other scripts fall back via the system stack; all numerals tabular in tables.
- **Type scale** (`font.*`): display 32/40 · h1 24/32 · h2 20/28 · h3 16/24 semibold · body 14/22 · body-lg 16/24 · caption 12/16 · mono 13/20. Phones add +1 px to body sizes.
- **Spacing**: 4-px grid, steps `0,2,4,8,12,16,20,24,32,40,48,64`. Screen gutters: 16 (phone) / 24 (tablet) / 32 (web).
- **Radius**: `sm 6 · md 10 · lg 16 · full`. Cards md, inputs sm, sheets lg.
- **Elevation**: 0 flat · 1 card · 2 sticky/toolbar · 3 overlay · 4 toast. Dark mode raises surface tone instead of shadow.
- **Motion**: 120 ms (micro) / 200 ms (overlay) / 300 ms (page) with standard easing; respects `prefers-reduced-motion`.
- **Breakpoints (web)**: `sm 640 · md 960 · lg 1280 · xl 1440+`; table→card collapse at `sm`.

### 2.4 Tenant theming

- Theme source: the tenant's theme record (`platform.theme`, selected per institution; token rows per consolidation R4) fetched with branding at shell boot and cached with its ETag.
- Overridable: `color.primary*`, `color.secondary*`, logo, favicon, login artwork, radius (one notch), font (from an allow-list). **Never** overridable: semantic intents, spacing, type scale, focus ring.
- The token pipeline validates every override for AA contrast at build/apply time and falls back to the default pair when a tenant value fails (§6).
- Dark mode: automatic from OS, user-overridable (ACC-07); tenants supply optional dark variants, else derived.

## 3. Layout and density

- **W**: top bar 56 px, left nav 264 px (collapsible to 72), content max 1440 px. Tables default **compact** (40 px rows) for back-office personas, **comfortable** (48 px) elsewhere; user-switchable, persisted per device.
- **S/P/T**: bottom tabs 56 px + safe areas; cards full-bleed with 16 px gutters; one primary action per screen as a bottom bar when forms scroll.
- **B**: two-pane split (360 px list + fluid detail) in landscape; single pane portrait. Touch targets ≥ 44 px everywhere.

## 4. Interaction patterns

| Pattern | Rule |
|---|---|
| **Forms** | Sectioned; labels above fields; inline validation on blur + submit; server 422 field errors land on the exact field (05 §6); dirty-leave guard; long forms (admission) are steppers with per-step save; `validateOnly` precheck on the final step |
| **Tables/lists** | Toolbar = search + filter chips + saved views; server sort/filter only on declared fields; sticky header; row click = detail, row kebab = actions; bulk-select bar appears on selection; export button where a report operation exists |
| **Pickers** | Typeahead pickers for people/students/courses (server search, recent-first); never free-text ids |
| **Dialogs vs drawers vs pages** | Confirm/destructive = dialog (danger button right, types the consequence); short create/edit = right drawer (W) / sheet (mobile); complex = page |
| **Destructive actions** | Always named ("Cancel invoice", never bare "Delete"); soft-deletes say what remains (audit, history); blocked deletes (409 references) explain and link the blockers |
| **Status & workflow** | Status chip (intent colour) + allowed transitions as explicit buttons from the contract's `:verb` operations; disallowed transitions are hidden; every workflow screen shows "who/when" from audit fields |
| **Approval queues** | One shared pattern (ASM-02, HR-04, ADMN-02, LMS enrolment requests, comms posts): list with filters → item preview → approve/reject with required comment on reject |
| **Toasts & progress** | Mutation success = toast (with undo only where a true inverse operation exists); long-running = progress panel bound to the operation id; failed background work also lands in ACC-09 |
| **Wizard** | Promotion (SIS-03), payslip run (HR-07), results (ASM-11), checkout (F4): steps = select → preview (server-computed) → apply → outcome report with per-row errors |
| **Offline (B/T)** | Global status chip (synced / syncing / queued n / conflicts n); per-item queued badge; conflict tray per 05 §7.5 |
| **Numbers & money** | `Money` component: tenant currency, locale grouping (Indian lakh/crore under `en-IN`), tabular numerals, negative = danger; percentages one decimal |
| **Dates & times** | Institution timezone for school data, viewer-local for LMS with timezone hint when they differ ("09:00 IST"); relative stamps under 7 days, absolute after |
| **Empty states** | Illustration (from the shared set) + one sentence + primary action; first-run empties may link to setup docs |

## 5. Component inventory

`@mysms/ui-web` and `@mysms/ui-native` export the same names and props wherever the platform allows.

**Primitives (28).** Button (primary/secondary/ghost/danger; loading), IconButton, SegmentedControl, Tabs, Chip/StatusChip, Badge, Avatar(+Group), Tooltip, Card, Divider, Accordion, Dialog, Drawer/Sheet, Popover, Menu, Toast, Banner/Callout, ProgressBar/Ring, Skeleton, Spinner(inline only), TextInput, TextArea, NumberInput, Select/Combobox(async), MultiSelect, DatePicker/RangePicker, TimePicker, FileUpload (2-step, progress, retry), Switch/Checkbox/Radio, Stepper, Pagination, Breadcrumbs, EmptyState, ErrorState, OfflineState, SearchInput, Rating, ColorSwatch.

**Data (6).** DataTable (server paging/sort/filter, column prefs, sticky, selection, virtualised), DataCardList (phone mirror of DataTable), DefinitionList, Timeline/ActivityFeed, StatCard, ChartFrame (consistent axes/legends/empty for the few analytics screens).

**Domain (24).**
- `PersonCard` / `PersonPicker` / `StudentPicker` (card = avatar + name + sub-line; the only way people render)
- `InstitutionSwitcher`, `PersonaSwitcher`, `ChildSwitcher`, `AcademicYearPicker`
- `AttendanceGrid` (roster × status cycling, reason popover) — ATT-03
- `TimetableGrid` (week × period; session cards) — TT-02/03
- `CalendarView` (month/week/agenda; occurrence actions) — COM-03
- `FeeSummaryCard`, `DueLine`, `ReceiptView` — F4
- `MarksGrid` (students × components, keyboard-first, row errors) — ASM-10
- `GradebookGrid` (virtualised students × items; cell states: ungraded/draft/published/excused/reassigned) — LMS-31
- `RubricGrid` (view + pick modes) — LMS-32/46
- `QuizPlayer` (timer, palette, autosave indicator; renderers per question type: choice, multi, true/false, short, open, numeric, fill-blanks, match, hotspot, pattern) — LMS-08
- `LessonRenderer` (block content: rich text, media, embeds, downloads) + `LessonEditor` (same blocks, editable) — LMS-06/42
- `OutlineTree` (chapters/items; drag, visibility windows, locks) — LMS-22/41
- `ReleaseRuleBuilder` (condition rows + targets) — LMS-29
- `ForumThread` (nested replies, votes, moderation affordances) — LMS-26
- `AnnotationLayer` (highlight/note on documents & e-books) — LMS-13
- `ChatThread` — COM-05
- `NoticeCard`, `AudiencePicker` (institutions/roles/sections/cohorts) — COM-02, COM-06
- `ConsentPrompt` (purpose, policy text, grant/decline) — F15
- `UploadManager` (global queue: uploads + queued offline writes)
- `OperationProgress` (binds to an operation id)
- `SyncStatusChip` + `ConflictTray` (B/T)
- `PermissionGate` (renders children only when `Me.permissions` allows; the primitive behind README rule 3)

Each component spec (props, states, a11y notes) lives beside its code with its tests. *Planned:* a Storybook story per component as the design-review surface (not set up yet).

## 6. Accessibility — WCAG 2.2 AA

- Contrast ≥ 4.5:1 text / 3:1 UI; enforced by the token build, including tenant overrides (§2.4).
- Full keyboard support on W (visible 2 px focus ring `color.primary`, logical order, skip-to-content; grids are arrow-key navigable with row/col headers announced).
- Screen readers: every control labelled; status chips expose text, not colour; toasts are live regions; icons decorative unless the only label.
- Colour never the sole signal (attendance states pair icon + letter + colour).
- Touch targets ≥ 44×44; text scales to 200 % / OS font scaling without loss; reduced motion honoured.
- Media: captions field supported for lesson video; e-book reader exposes font size/contrast controls.
- Each screen spec's definition of done includes an axe (web) / accessibility-inspector (RN) pass with zero criticals (06 §4).

## 7. Internationalisation

- All strings through the shared i18n catalogue (`@mysms/i18n`, ICU MessageFormat); **no literals in components**. Keys namespaced `module.screen.element`.
- Ship `en` as source-of-truth; `hi` as the first translation target; catalogue structure supports any locale the tenant enables.
- Locale resolution: user (`Me.user.locale`) → tenant default → `en`. Numbers, dates, calendars and currency formatting via `Intl` with the resolved locale; money always with explicit currency code/symbol from `master.currency`.
- RTL-ready from day one: logical CSS properties / RN `I18nManager`; icons with direction semantics flip.
- Text expansion budget +35 %; no text in images; dates/names never concatenated into sentences (full ICU patterns).
- Enum labels (statuses, types) come from the i18n catalogue keyed by the API's enum value — the API value is never shown raw.

## 8. Content style and terminology

Tone: plain, specific, calm; sentence case everywhere; no jargon in user-facing errors ("Couldn't save — this roll number is already used in 5-A").

Canonical glossary (UI copy must use these consistently):

| Term | Means | Not |
|---|---|---|
| **Student** | The person learning — in school, higher education and online learning alike | "Student", "Pupil", "Member" |
| **Teacher** | The person teaching — class sections, HE courses and LMS cohorts alike | "Teacher", "Faculty", "Tutor", "Trainer" |
| Class section | A K-12 class group (Grade 5-A) | "Standard", "Division" |
| Batch | HE intake group | |
| Cohort | A teacher-led LMS class | "Batch" (LMS), "Class" |
| Course (Academics) | HE program course | |
| Course (Learning) | LMS course | "Training" |
| Guardian | Parent/guardian | "Parent" only in P app branding |
| Homework | K-12 daily work | "Assignment" |
| Assignment | Graded submission work (official or LMS) | |
| Due / Invoice / Receipt | Fee lifecycle states | "Bill" |
| Notice | Published announcement | "Circular" |
| Marks / Grade | Exam score / letter result | |
| Publish | Make visible to its audience | "Activate" |

Contract identifiers and UI copy use the same words: *student* and *teacher*, everywhere.

Error-message pattern: what happened → why (if known) → what to do next. Every `Problem.code` has a copy entry; unknown codes fall back to `title` + retry.

## 9. Configurability and personalisation

MySMS serves very different people — a platform operator, a school owner, a bursar, a class teacher, a parent on a phone, a ten-year-old on a tablet — so the interface adapts at several levels. **Configurable never means forked:** every option is a token, a flag or a layout setting over the same components, so each option stays accessible, tested and translatable.

| Level | Who decides | What can change | Where it is stored |
|---|---|---|---|
| Platform | Platform operators (C) | Product-wide defaults: the setting catalogue and each setting's default, plan → module availability, feature flags, master data | `iam.setting_definition` (catalogue, maintained by migrations), `platform.saas_plan`, `platform.tenant_subscription`, `platform.system_setting`, `master.*` |
| Tenant, institution, department, class section | School owners, administrators, department heads (within their own units) | Any catalogue setting allowed at that scope — theme, density, text size, language, time zone, date format, landing screen, e-mail digest, session timeout — optionally **locked** so narrower scopes and users cannot override it; branding (§2.4); terminology per institution type | `iam.setting_value` (one value per setting and scope; screen ADM-16), `platform.theme` and `theme_token` |
| Role / persona | Derived from permissions | Navigation, home dashboard and actions show only what the role can do (`PermissionGate`, 05 §5); each persona has its own default home (02 §1) | Computed from the token's permissions; no storage |
| User | Each person | The user-editable catalogue settings (screen ACC-14); pinned navigation and shortcuts, saved table views and dashboard cards | `iam.setting_value` at user scope; device-only items are labelled as such |

Notification delivery preferences are managed separately on ACC-10 in `comms.notification_preference_set` and
`comms.notification_preference`. They are grouped by role set and must not be treated as general interface settings.

Rules:

1. **Every setting has a good default.** A user who never opens settings gets the catalogue default, overridden by whatever their school, department or class has set.
2. **Precedence, top-down:** user → primary class section → primary department → institution → tenant → catalogue default — the most specific value wins, **except** that a value locked at a scope wins over everything narrower (a school that requires its brand theme locks `ui.theme` at tenant scope; kiosks lock reduced motion). The settings screen shows for each row where its value comes from and whether it is locked. Primary department and class section are the user's role grants marked primary; the institution is the one selected in the shell (`X-Institution-Id`) or the user's primary institution grant.
3. **Reversible and previewed.** Every personalisation shows a live preview and has *Reset to default* (removes the user's own value so the inherited one applies again); tenant theme changes are previewed in the console before publishing and must pass the AA contrast check (§6).
4. **Never hides obligations.** Preferences can reorder and hide optional cards and columns, never required information: fee dues, safeguarding notices, attendance alerts, consent requests and system messages stay visible.
5. **Same everywhere.** Settings follow the user across web, phone and tablet; device-only settings (text size from the OS, offline storage) are labelled as such.
6. **Fast.** Effective settings arrive with the session (`iam.GetMe` plus `iam.ListMySettings`) and are applied before the shell renders, so it never flashes the default theme or layout.
7. **Validated at the source.** The server checks every value against the catalogue (type, allowed values, range, the scopes it may be set at, whether users may change it) and a scoped administrator can set values only on the units of their own grants.
8. **Administrator settings stay with administrators.** A setting users may not change (session timeout, for example) is not shown to them at all: `ListMySettings`, `ListSettingDefinitions` and `GetSettingDefinition` return it only to callers who administer settings (`iam.setting_value.read`) at one of the scopes it may be set at, and answer 404 for it otherwise, as do `SetMySetting` and `ResetMySetting`. Only a tenant-wide administrator sees which unit a value comes from (`sourceScopeId`); everyone else sees only the scope type.

Delivery: the setting catalogue, values at every scope, locks and the caller's effective settings are served by iam (`iam.ListSettingDefinitions`, `iam.*SettingValue`, `iam.ListMySettings`, `iam.SetMySetting`, `iam.ResetMySetting`); the web clients apply theme, density, text size, reduced motion and language at boot and offer ACC-14 and ADM-16; the mobile and tablet apps apply theme, text size and language and offer ACC-14 (on devices reduced motion follows the operating system and density concerns web tables). Saved table views, dashboard card arrangement and pinned navigation are the next client increment.
