# MySMS Classroom Voice Assistant: Product and Device Brainstorm

**Prepared:** 10 November 2026  
**Concept:** A teacher-controlled, voice-first assistant connected to MySMS that handles short, repetitive classroom administration tasks.  
**Status:** Product concept for later validation; no device, firmware, or voice integration has been built.

## The product idea

Give each participating classroom a small, secure voice terminal paired to that school's MySMS tenant and classroom. A teacher can ask for a bounded task—such as starting attendance, checking who is absent, or reading the next period's timetable—and MySMS performs the task through the same permissions and workflows used by the teacher app.

The device should feel like an **administrative remote control for MySMS**, not an unrestricted chatbot. It should use voice to select a known action, query an authorized MySMS record, speak a short result, and ask for confirmation when it is about to change official data.

The product promise should be about teacher time saved, fewer missed records, and more accessible hands-free workflows. Do not promise that voice recognition is perfect or that AI makes attendance decisions.

## Best first use case: attendance

Attendance is a strong first workflow because the repository already has attendance sheets, records, state transitions, and `TakeAttendanceForSession` / `RecordAttendance` operations listed as served. The current attendance module uses a timetable session to identify the class, and each sheet starts with expected students present. This means the voice feature can focus on a teacher confirming exceptions, rather than asking an AI model to infer each student's identity from room audio.

### Example interaction

1. Teacher taps the device or says its wake phrase after local wake-word detection.
2. Device confirms teacher/class context: “Grade 8B, Mathematics, period 2. Start attendance?”
3. Teacher confirms. MySMS opens or retrieves the matching attendance sheet for the active session.
4. Teacher says, “Absent: Asha Patel, Rohan Das.”
5. Assistant matches each name against the authorized roster. If two students match, it asks a clarifying question and does not guess.
6. Assistant reads back the proposed exceptions: “I have Asha Patel and Rohan Das absent. Mark these two?”
7. Teacher confirms. MySMS writes through the normal attendance operation, with the teacher's identity, device ID, time, class, and resulting records in the audit trail.
8. If the teacher asks, “Who is absent?”, the assistant fetches the current sheet and gives the authorized answer. It should say which sheet/date/session it used.

This design avoids using facial recognition, voice biometrics, or automated student roll-call identity inference. The teacher is the person making the attendance decision; the system transcribes, verifies the roster names, reads back the proposed update, and records the confirmed action.

## Suggested product boundaries

### Phase 1: safe, read-heavy, short actions

- Start attendance for the current timetable session.
- Mark teacher-confirmed absent students, with roster matching and read-back confirmation.
- Read the absent list for a selected/current class, after teacher authentication.
- Ask for the next period, classroom, or current class roster count.
- Read a short class announcement or approved notice.
- Create a reminder or draft a message for the teacher to approve in the app.

### Phase 2: draft and confirm actions

- Create a homework draft with a class, subject, and due date; require the teacher to review/approve it in the app.
- Query class-level progress or missing assignments using scoped reports.
- Draft parent communications, never send bulk messages from an ambiguous voice command.
- Allow a teacher to update a record only when the same MySMS workflow, permission, and audit policy supports it.

### Avoid in the first release

- Student identity from face, voiceprint, or continuous room listening.
- Automatic discipline, health, counselling, or risk classifications.
- Fee, payroll, marks, or official exam-result changes by voice alone.
- Open-ended answers that make up a policy, timetable, grade, or student record.
- Sending a notice to parents without a clear audience, preview, and confirmation.
- Reading sensitive information aloud when the teacher is not authenticated or the class context is unclear.

## Architecture concept

```text
Teacher
  │ push-to-talk / local wake phrase
  ▼
Classroom device
  ├─ local wake-word detection; physical mic mute and visible capture light
  ├─ short-lived paired-device credential; encrypted local queue for offline work
  ├─ audio capture only after explicit activation
  └─ device health, update and remote-revoke channel
        │ encrypted request; no long-lived tenant-wide secret
        ▼
Voice gateway / intent service
  ├─ speech-to-text, language and confidence checks
  ├─ allowlisted intent schema; no arbitrary SQL or raw service credentials
  ├─ teacher/session/context validation
  ├─ normal MySMS authorization and API call
  ├─ read-back/confirmation policy for write actions
  └─ privacy-safe audit record; raw audio discarded by default
        │ authenticated API call
        ▼
MySMS IAM + attendance / timetable / SIS APIs
        │
        └─ response → short speech response / device display
```

The platform should treat the device as a constrained client, not as a trusted administrator. A device token must be scoped to one tenant and classroom, have a short expiry/rotation policy, and be revocable if a school reports a lost or reassigned unit. Teacher permissions still come from MySMS IAM. Do not put a long-lived administrator token in device firmware.

## Hardware choices

| Option | What it proves | Advantages | Limits and risks | Best use |
|---|---|---|---|---|
| Teacher phone or existing tablet with push-to-talk | Voice UX, attendance API, language handling, teacher acceptance | Cheapest and fastest; no custom hardware procurement; existing client can authenticate the teacher | Not hands-free; teacher devices vary; classroom acoustics still matter | First functional pilot |
| Existing smart speaker with an approved MySMS integration/skill | Whether a commercial voice ecosystem can invoke a MySMS task | Fast demonstration if platform and account eligibility fit | Amazon controls voice processing and device experience; certification, account linking, privacy terms, regional model/data behavior, and support must be checked; the startup does not control firmware or update lifecycle | Limited pilot after product/legal review |
| MySMS custom screenless voice terminal | Device management, classroom UX, privacy controls, offline behavior, and hardware economics | Own the capture controls and school-focused experience; can pair to one room and support offline queueing | Requires electronics design, firmware security, manufacturing, quality assurance, certification checks, inventory, warranty, and field support | Only after voice workflow and willingness-to-pay are validated |

Amazon distinguishes Alexa Built-in and Works with Alexa programmes and requires certification for the corresponding branding/experience. Alexa Built-in products must meet security, certification, and customer-experience requirements; third-party speakers can also depend on the Alexa app. Do not market a custom MySMS device as an Amazon Alexa device or assume access to Alexa APIs until Amazon confirms the commercial/technical path. [Alexa Built-in overview](https://developer.amazon.com/en-US/alexa/branding/alexa-guidelines/brand-guidelines/smart-speakers-playbook/2-overview), [Alexa device programmes](https://developer.amazon.com/en-US/alexa/devices)

### Recommended path

1. Prototype the teacher flows in a phone/tablet using push-to-talk.
2. Run a small pilot in real classrooms and measure recognition accuracy, task completion, teacher trust, and time saved.
3. Test a supported third-party speaker only if Amazon confirms the account-linking, product, data, certification, and commercial requirements.
4. Build a custom device only if the experience needs to be hands-free and the paid software economics can pay for the installed hardware over a defined service term.

## India language and cloud-region decision

The voice stack needs a per-pilot language plan. Google's current Cloud Speech-to-Text V2 language table lists Indian English (`en-IN`) and Hindi (`hi-IN`) Chirp 2 support under Singapore (`asia-southeast1`); its current Mumbai (`asia-south1`) entries are more limited. The table and launch readiness can change. For MySMS, that creates a real product decision:

- **Singapore speech endpoint:** may offer the needed model/language set, but classroom audio leaves India for processing. Confirm data-processing terms, school/parent notices, contractual requirements, latency, and customer acceptance before using it.
- **Mumbai model/endpoint:** prefer when the required language/model is available and meets accuracy needs; verify the live locations API before committing.
- **On-device or self-hosted speech recognition in Mumbai:** gives more control over processing location and offline behavior, but increases hardware/compute cost, model maintenance, and engineering burden. This is not included in the cloud estimate.
- **Narrow pilot:** begin with supported English commands or push-to-talk and explicit teacher correction, while validating Hindi/regional-language demand using real recordings with lawful consent and minimal retention.

Do not confuse a model that supports a language globally with a model that is available at the desired regional endpoint. [Google Speech-to-Text language/region table](https://docs.cloud.google.com/speech-to-text/docs/speech-to-text-supported-languages), [Speech-to-Text regional availability](https://docs.cloud.google.com/speech-to-text/docs/locations)

## Privacy, safety, and trust requirements

Classroom audio can include children's voices and personal information. The assistant should be designed so that a teacher can explain exactly when it listens and what happens to the data.

- Use local wake-word detection or push-to-talk. Do not stream room audio continuously to the cloud.
- Include a physical microphone mute switch and an unmistakable capture indicator. Make mute state visible from across the room.
- Delete raw audio after transcription unless a school has explicitly approved a separate, time-limited diagnostic process. Keep transcripts only when needed for the record/audit, and minimize what is stored.
- Do not use voiceprints to identify teachers or students. Authenticate the teacher through the MySMS app, a short-lived session, a tap/QR/NFC handoff, or another school-approved method.
- Read back a proposed attendance change and require a teacher confirmation before writing. A low-confidence name match must lead to clarification, not the nearest match.
- Bind every request to the tenant, school, classroom, teacher, timetable session, and current permission. Re-check authorization server-side before executing.
- Use role and class permissions for queries. A device in one classroom must not fetch another class's student list merely because a voice command names it.
- Avoid reading sensitive data aloud unless the teacher is authenticated and explicitly asks. Keep responses brief and avoid unnecessary personal details.
- Provide offline behavior carefully: allow an encrypted, bounded attendance draft queue, show “not synced” clearly, and use idempotency keys so a retry cannot duplicate attendance.
- Provide remote disable/revoke, signed software updates, encrypted transport/storage, a vulnerability response route, and an inventory/ownership record for every school device.
- Agree with each school on the purpose, lawful roles, notices, retention, processor terms, user access, incident escalation, and deletion/return process. Have Indian privacy counsel map the DPDP Act and phased 2025 Rules to the school use case. This is a compliance design input, not a legal conclusion. [MeitY DPDP Rules](https://www.meity.gov.in/documents/act-and-policies/digital-personal-data-protection-rules-2025-gDOxUjMtQWa?pageTitle=Digital-Personal-Data-Protection-Rules-2025%3B), [DPDP Act](https://www.meity.gov.in/static/uploads/2024/02/Digital-Personal-Data-Protection-Act-2023.pdf)

## Voice workload and cost drivers

For a 1,000-device fleet, 22 school days/month, and 15-second utterances:

| Calls per device per day | Audio minutes/month | Approx. speech + text reasoning + spoken-response cost/month* |
|---:|---:|---:|
| 5 | 27,500 | About ₹0.9 lakh |
| 20 | 110,000 | About ₹3.6 lakh |

\*Planning arithmetic using published Google Speech-to-Text no-data-logging, Neural2 Text-to-Speech, and Gemini Flash-Lite rates; see [FUNDING-AND-CLOUD-COST-ESTIMATE.md](FUNDING-AND-CLOUD-COST-ESTIMATE.md). It excludes a custom self-hosted speech model, Amazon Alexa charges/terms, connectivity, tax, and provider-specific rounding/quotas. Confirm model language and endpoint first. Costs scale with actual minutes and response length.

The most useful measurements from a pilot are not just requests per second. Capture commands per class/day, average and p95 audio seconds, retry rate, failed-name correction rate, language mix, speech endpoint, transcription/LLM/TTS spend per school, and teacher-confirmed completion rate.

## Rollout and business model

Assume 50,000 students across 100 schools as a planning example. If there is one device in each of 1,000 classrooms, the installed device allowance in the cost estimate is ₹10,000–18,000 per room, or ₹1.0–1.8 crore for the full fleet before custom design/validation and continuing repairs.

### A staged rollout

| Stage | Scope | Advancement evidence |
|---|---|---|
| 0: problem validation | Interview teachers and school administrators; observe attendance and notification workflows; test teacher phone/tablet push-to-talk | Measurable time saved, clear data permissions, supported classroom language, and willingness to pay |
| 1: functional pilot | 3–5 schools, a small group of teachers, no custom device commitment | Attendance is correct after confirmation; no cross-tenant/class access; users understand capture/mute controls |
| 2: field pilot | 10–20 schools, perhaps 50–100 devices; school Wi-Fi preferred | Device uptime, correction rates, per-school support load, cloud cost, teacher weekly retention, renewal/paid-pilot intent |
| 3: paid deployment | Expand through school groups with contracted support and device terms | Signed annual contracts, reference schools, predictable implementation and support cost |
| 4: 1,000-device rollout | Procure/install across roughly 100 schools only after step 3 evidence | Three-year device funding, replacement reserve, language/data plan, support staffing, and unit economics approved |

### Free-device options

- **Device included with a three-year SaaS contract:** school pays an annual subscription; device cost is amortized within the service price.
- **Refundable deposit or lease-to-own:** lowers initial hardware risk and discourages abandoned devices.
- **Sponsored classroom:** CSR/corporate/education partners underwrite devices under written branding, privacy, and service terms; do not treat sponsorship as guaranteed recurring revenue.
- **School-group bulk contract:** reduce installation and support costs with one implementation owner and minimum school commitment.
- **Software-only tier:** schools can use the app without the hardware, protecting access and avoiding hardware capex for schools that do not need a speaker.

At the estimate's cost assumptions, 50,000 students and one device per 50 students yield a variable cloud/AI/device allocation of roughly ₹140–360 per student/year before staff, field operations, sales, taxes, repairs, and payment fees. Test annual price points with actual schools. The ₹300/₹600/₹900 scenarios in the companion funding document are hypotheses, not verified market prices.

## Success metrics for the pilot

### Reliability and safety

- Attendance update precision after teacher confirmation: target at least 99.5% of accepted changes match the teacher's intended student/session.
- No action executes on low confidence or ambiguous student identity; count clarification and cancellation rates.
- 100% of write actions have a teacher, device, tenant, class/session, confirmation, and audit record.
- Measure classroom network availability, offline queue age, successful sync rate, and duplicate prevention.
- Zero retained raw audio by default; verify the device mute indicator and remote revocation in school acceptance tests.

### Teacher value

- Minutes to complete attendance before and after the assistant.
- Percentage of pilot teachers who use it in at least three school days per week after week four.
- Teacher satisfaction, correction burden, and task completion time compared with the app.
- Number of schools that convert from free trial to paid annual contract and reasons for non-conversion.

### Unit economics

- Cloud and speech/LLM cost per classroom/month and per active teacher.
- Device installed cost, defect/return rate, field service calls, and replacement cost.
- Onboarding hours and support tickets per school.
- Renewal value and contract duration compared with device acquisition cost.

Set targets with pilot schools before the field trial. The numeric examples above are proposed gates, not results already achieved.

## Risks and unresolved questions

1. **Speech language versus region:** the needed India-accent language model may require a non-India endpoint under the current published support matrix.
2. **Classroom noise and names:** accents, fan noise, multiple speakers, similar names, and regional pronunciations can create false matches. Human read-back is essential.
3. **Authentication in a room:** a shared device cannot assume that the nearest voice belongs to the teacher. Use an authenticated teacher session and a clear lock/timeout policy.
4. **School connectivity:** Wi-Fi quality and power availability vary. The device needs graceful offline behavior; cellular backup affects economics and operational support.
5. **Vendor dependency:** Amazon branding/API, Google's model endpoints, model pricing, and supported languages can change. Keep a provider abstraction and a phone/tablet fallback.
6. **Product readiness:** the repository has a complete attendance contract/operation set listed as served, while a material share of other MySMS operations remains unimplemented. Keep the first assistant scope inside flows that are actually implemented and tested end-to-end.
7. **Device liability and servicing:** a free speaker creates repair, replacement, security update, collection, and e-waste obligations. Contract for ownership, warranties, returns, and end-of-service.
8. **Data governance:** student data must not become general model training data or a transcript archive. Define roles, purpose, retention, notices, subprocessors, and deletion with schools before pilot recordings.
9. **Commercial viability:** voice usage and hardware can erase a low per-student subscription margin. Cap utterance length, use local wake-word detection, minimize LLM calls, and meter usage by school.

## Build later: staged specification outline

When product work begins, keep the first specification small and testable:

1. Attendance assistant user stories and permission matrix.
2. Device identity, teacher pairing, session expiry, remote revoke, and room assignment lifecycle.
3. Command schema and explicit allowlist; transcript-to-intent confidence and clarification behavior.
4. Speech provider/model choice by language and approved region.
5. Human-confirmation rules for each write operation.
6. Privacy notices, retention/deletion, audit fields, subprocessors, and security incident process.
7. Offline behavior, idempotent sync, and timetable/roster freshness.
8. Device bill of materials, sourcing options, enclosure/thermal/acoustic requirements, factory test, update signing, and support process.
9. School pilot protocol, acceptance criteria, teacher training, support SLA, and stop conditions.
10. Commercial model covering hardware funding, subscription floor, replacement term, connectivity, and renewal.

## Sources checked

- [MySMS attendance API status](../api/operations-status.md)
- [MySMS backend status](../STATUS.md)
- [MySMS architecture](../ARCHITECTURE.md)
- [MySMS deployment guide](../DEPLOYMENT.md)
- [Google Speech-to-Text regional language support](https://docs.cloud.google.com/speech-to-text/docs/speech-to-text-supported-languages)
- [Google Speech-to-Text locations](https://docs.cloud.google.com/speech-to-text/docs/locations)
- [Google Cloud Speech-to-Text pricing](https://cloud.google.com/speech-to-text/pricing)
- [Google Cloud Text-to-Speech pricing](https://cloud.google.com/text-to-speech/pricing)
- [Amazon Alexa Built-in programme overview](https://developer.amazon.com/en-US/alexa/branding/alexa-guidelines/brand-guidelines/smart-speakers-playbook/2-overview)
- [Amazon Alexa device programmes](https://developer.amazon.com/en-US/alexa/devices)
- [India DPDP Rules 2025](https://www.meity.gov.in/documents/act-and-policies/digital-personal-data-protection-rules-2025-gDOxUjMtQWa?pageTitle=Digital-Personal-Data-Protection-Rules-2025%3B)
- [India DoT Equipment Type Approval](https://eservices.dot.gov.in/equipment-type-approval-eta)
