# MySMS Classroom Voice Assistant: Product and Device Brainstorm

**Prepared:** 10 November 2026 (revised the same day with three independent expert brainstorms)  
**Concept:** A teacher-controlled assistant connected to MySMS that takes short, repetitive classroom admin off the teacher: attendance, "who is absent", timetable, homework and parent messages.  
**Status:** Concept for later validation. No device, firmware or voice integration has been built.  
**Conventions:** *Estimate* = our arithmetic or judgement, with the basis stated. *Unverified* = a claim we could not confirm from a source. Everything else carries a source link. This is not legal advice; items marked "counsel" need an Indian lawyer.

---

## 1. Decision summary

1. **Build the assistant, not the box, first.** Ship push-to-talk voice and exception-only attendance in the existing React Native teacher app as a 3-month, zero-hardware pilot. All three experts reached this independently.
2. **Do not copy the Echo look or build on Alexa.** Amazon designs Echo in-house (Lab126) and contract manufacturers build it on Amazon's tooling; there is no supply chain to buy into, a look-alike invites a design-piracy or passing-off claim, and Amazon's business routes are closed to schools (§2).
3. **Attendance is the right wedge but the weakest reason for a voice *device*.** The time saving comes from the *pattern*: everyone pre-marked present, approved leave pre-filled, the teacher states only exceptions, by **roll number**, with a read-back. A tap screen already does this in about 20–30 seconds (estimate), so voice must beat that, not a 3-minute roll call.
4. **The higher-value voice jobs are content capture and queries**: homework and class-diary dictation, "who is absent", "what's next", parent-message drafts. Typing these on a phone while standing in class is slow; asking for them is fast.
5. **Push-to-talk only. No wake word, no ambient capture, no child audio, no biometrics.** This is the cheapest design, the most reliable in a room of 30–60 children, and the one that is defensible under the DPDP children's obligations that begin on **13 May 2027** ([AZB & Partners](https://www.azbpartners.com/bank/update-indias-digital-personal-data-protection-framework-comes-into-effect/)), inside the first full pilot year.
6. **If MySMS ever puts hardware in a room, make it output-first.** Use a networked period/bell/substitution/notice display, or an app on the interactive panel the school already owns, with the teacher's phone as the microphone. A microphone box comes last, if ever.
7. **Speech is cheap; hardware and support are not.** Push-to-talk speech costs roughly ₹300–2,500 per classroom per year depending on vendor and usage (estimate, §9). A device adds roughly ₹1,800–8,800 per classroom per year before one-off design and certification costs (estimate, §9).
8. **Biggest risks:** a wrongly marked absence that reaches a parent; a "listening device in my child's classroom" news story; hardware support overwhelming a software company; teachers simply preferring to tap.

---

## 2. Your two direct questions

### 2.1 Where does Amazon source Echo?

| Fact | Status | Source |
|---|---|---|
| Echo is designed by **Amazon Lab126**, Amazon's hardware R&D subsidiary in Sunnyvale, California (founded 2004; Kindle 2007, Echo 2015, Echo Dot 2016). | Verified | [Wikipedia: Amazon Lab126](https://en.wikipedia.org/wiki/Amazon_Lab126) |
| Assembly is outsourced. **Foxconn** built Echo and Echo Dot at Hengyang, Hunan, China; a 2019 China Labor Watch report on student interns working overtime on those lines made the news. | Verified (2019 reporting) | [CBS News](https://www.cbsnews.com/news/amazon-echo-devices-children-in-china-forced-to-work-overtime-to-make-your-amazon-devices-report-says/), [Vice](https://vice.com/en/article/your-amazon-alexa-might-have-been-made-by-overworked-chinese-schoolchildren) |
| Amazon's first Indian device line (announced Feb 2021) is run by Cloud Network Technology, a Foxconn subsidiary, in Chennai, for **Fire TV Sticks**. | Verified | [The Week](https://www.theweek.in/news/biz-tech/2021/02/16/amazon-to-start-device-manufacturing-including-fire-tv-sticks-in-india.html), [TechRadar](https://www.techradar.com/news/amazon-to-start-device-manufacturing-in-india-with-fire-tv-sticks) |
| In 2019 Foxconn's India head said he planned to add Echo speakers to Indian production "eventually". | Verified as a stated plan | [IndustryWeek](https://www.industryweek.com/supply-chain/article/22028163/apple-iphone-amazon-echo-production-moving-from-china-to-india) |
| Echo speakers are made in India today | **Unverified**; no primary source found by any of the three experts | — |
| Amazon uses its own silicon in current Echo devices (AZ3 in Echo Dot Max / Echo Studio, launched in India 27 May 2026 at ₹10,999 / ₹23,999). | Verified (news) | [Business Today](https://www.businesstoday.in/technology/news/story/amazon-launches-echo-dot-max-and-echo-studio-smart-speakers-in-india-533602-2026-05-27) |

**Plain answer:** Amazon designs Echo itself and contract manufacturers such as Foxconn build it with Amazon's design, tooling, firmware and silicon. There is no white-label Echo, and no factory will sell you "the same one". Other suppliers (e.g. Compal) are **unverified**.

### 2.2 What "getting the same look" means in India

| Lever | What it means for a look-alike | Source |
|---|---|---|
| **Designs Act, 2000, s.22** | Applying a registered design *"or any fraudulent or obvious imitation thereof"* to articles for sale, or importing them, is piracy. Remedies are up to ₹25,000 per contravention (capped at ₹50,000 per design), or damages plus an **injunction**. The injunction is the real risk because it stops shipments. Protection lasts 10 years, extendable by 5. | [S.S. Rana](https://ssrana.in/ip-laws/design-law-india/piracy-registered-design-india/) |
| Whether Amazon holds Indian design registrations for Echo models | **Unverified.** Assume yes, and have an IP attorney search the IP India designs register before any design freeze. | — |
| **Copyright Act, s.15(2)** | Copyright in a registrable but unregistered design lapses after more than 50 industrial reproductions, so copyright is not Amazon's main weapon. | [SCC Online](https://www.scconline.com/blog/post/2024/10/16/from-art-to-industry-decoding-the-clash-between-copyright-and-design-in-indian-law/), [Legal 500](https://www.legal500.com/intelligence/india/intellectual-property/between-art-and-industry-the-copyright-design-crossover) |
| **Trade marks / passing off** | "Echo", "Alexa" and the light-ring signature are Amazon brand territory. A fabric cylinder with a coloured ring sold as "Alexa-like" invites a passing-off claim over get-up. No Indian smart-speaker get-up case was found (**unverified**; counsel). | — |

It is also commercially unwise: Amazon is a potential cloud and marketplace partner, and the "look" is the cheapest part of a smart speaker. The cost sits in far-field audio, certification, fleet management and support.

**What to do instead:**

1. **Original classroom industrial design** from an Indian product-design studio or a Shenzhen ODM's ID team, registered by MySMS under the Designs Act. Wall-mounted above child reach, tamper screws, no fabric, a large mute switch, a status light visible from the back row.
2. **Fork an open reference design for prototypes:** Home Assistant Voice Preview Edition (US$59; ESP32-S3 + XMOS XU316, dual mics, hardware mute that cuts mic power, open firmware), the closest thing to a legal "Echo Dot you can fork" ([CNX Software](https://cnx-software.com/2024/12/20/59-voice-preview-edition-adds-an-offline-smart-speaker-to-your-home-assistant-server)).
3. **ODM reference board plus your own shell**, final assembly at a small Indian EMS (§7.3). Specific vendors **unverified**; get three written quotes.

### 2.3 Can MySMS just use real Echo devices? No.

| Amazon route | Current status | Fit |
|---|---|---|
| **Alexa Built-in (AVS)** on your own hardware | "Alexa Voice Service (AVS) developer tools are no longer generally available for Alexa Built-in" ([Amazon AVS deprecated features](https://developer.amazon.com/docs/alexa-voice-service/deprecated-features.html)) | Closed. *This corrects the earlier draft, which treated Built-in as a possible path.* |
| **Alexa Smart Properties** (fleet-managed Echos) | Verticals are hospitality, senior living, healthcare and "core". Hospitality and senior living are offered in CA, DE, ES, FR, IT, JP, UK and US; healthcare and core are US-only. **No education vertical, and India is not listed** ([Amazon ASP comparison](https://developer.amazon.com/docs/alexa/alexa-smart-properties/product-feature-comparison.html), [ASP overview](https://developer.amazon.com/docs/alexa/alexa-smart-properties/about-alexa-smart-properties.html)) | Closed |
| **Custom skill on consumer Echo** | Possible. India's child threshold in skill policy is under 13. Child-directed skills may not collect personal information or use account linking ([Alexa skill policy](https://developer.amazon.com/en-US/docs/alexa/custom-skills/policy-requirements-for-an-alexa-skill.html), [Kid skills](https://developer.amazon.com/en-US/alexa/alexa-skills-kit/get-deeper/custom-skills/kid-skills)). A teacher-facing skill is not child-directed, but it runs in a children's room, needs account linking, and sends classroom audio and student names through Amazon's consumer cloud. | Two-week demo at most; not a product |
| **Alexa+** | Hindi testing by invitation from 22 June 2026 ([TechCrunch, Jun 2026](https://techcrunch.com/2026/06/22/amazon-is-testing-alexa-in-india-with-hindi-support/)); consumer early-access launch in Hindi and English on 16 Sep 2026, free with Prime, otherwise ₹2,000/month ([TechCrunch, Sep 2026](https://techcrunch.com/2026/09/16/amazon-launches-alexa-in-india-with-hindi-support/)) | Consumer product, not a B2B platform. *The experts' two dates are both correct: testing in June, then launch in September.* |

A 2018 anecdote shows teachers are curious, not that the platform fits: a Maharashtra teacher used an Echo on a power bank and hotspot, and it answered only in English ([About Amazon India](https://www.aboutamazon.in/news/devices/alexa-goes-to-school)).

---

## 3. Where the experts agree, disagree, and where the draft stood

### 3.1 Strong signals: all three experts agreed independently

- No custom hardware before a software pilot proves weekly use and time saved.
- Do not imitate Echo; do not build on Alexa.
- Push-to-talk, teacher voice only, audio discarded after transcription, hardware mute with a light wired to the mic power.
- **Exception-based attendance** (default present) is the core interaction; **roll numbers** beat names; read-back before any write.
- No roll-call mode in which children answer "present": it needs far-field child-speech recognition, invites proxy answers, and captures children's audio.
- No face recognition and no voiceprints. Karnataka dropped a plan to mark attendance by face for over 52 lakh students in 2025 after objections ([Careers360](https://news.careers360.com/karnataka-scrap-facial-recognition-attendance-govt-school-data-privacy-mid-day-meal-ai-app-digital-madhu-bangarappa-education-news)).
- The LLM only emits whitelisted intents. A deterministic resolver checks the roster and permissions and calls MySMS **as the teacher**, never as the device.
- Pluggable speech-to-text: benchmark 2–3 providers on about 200 real, consented teacher utterances.
- The interactive panel and the teacher's phone are better first "devices" than a speaker.

### 3.2 Disagreements and our call

| Topic | Positions | Recommendation |
|---|---|---|
| Highest-value voice job | Opus: attendance by roll number, plus after-class admin dictation. Fable: content capture (homework, diary). Sonnet: the long tail of queries. | Ship all three in the pilot. **Rank capture and queries above attendance-by-voice**; keep attendance as the headline *pattern* (tap or voice). |
| Commit behaviour for attendance | Opus: auto-commit after 5 s unless cancelled. Founder, Sonnet, Fable: explicit confirm. | **Explicit confirm in v1.** A wrong absence can trigger a guardian notification. Consider auto-commit only after the pilot measures wrong-student writes at or below 0.5%. |
| Wake word | Founder's draft allowed a local wake phrase. All three experts: push-to-talk only. | **Push-to-talk only.** A wake phrase in a room of 50 children produces false triggers and a permanent "is it listening?" debate. |
| First room hardware | Opus: networked bell/clock/substitution display. Fable: silent classroom board; the phone is the mic. Sonnet: wall tablet or panel kiosk. | Converge on **output-first**: a panel app where a panel exists, otherwise a cheap display. The teacher's phone stays the mic. |
| Primary ASR | Founder: Google. Fable: Sarvam or self-hosted AI4Bharat. Opus: Sarvam or AWS. Sonnet: benchmark first. | **Benchmark Sarvam, AWS Transcribe and self-hosted IndicConformer.** Google Chirp 3 only if counsel accepts processing outside India (§6.3). |
| Device cost | Founder: ₹10,000–18,000 installed. Opus: ₹3,000–5,000 landed for an ESP32 push-to-talk unit. Fable: ₹4,000–7,000. Sonnet: ₹8,000–15,000 for a tablet or SBC pilot. | These describe **different devices**, and all are estimates. The founder's figure fits an installed tablet/SBC kit. A custom ESP32 unit is cheaper per unit but carries ₹11–45 lakh of one-off cost (§7). |
| WPC ETA for a finished product with a certified module | Opus via GRL: no separate ETA if made in India with the module fitted unmodified. Sonnet via Freyr: a module helps but does not approve the product automatically. Fable: unverified. | **Assume an ETA is needed until a lab confirms in writing.** The GRL route is a reason to assemble in India with a pre-certified module. |
| DPDP dates | Fable: notified 13 Nov 2025, phase III 13 May 2027. Opus and Sonnet: 14 Nov / 14 May. | Notified **13 Nov 2025**, consent managers from **13 Nov 2026**, children's data and most duties from **13 May 2027** ([AZB](https://www.azbpartners.com/bank/update-indias-digital-personal-data-protection-framework-comes-into-effect/), [SCC Online](https://www.scconline.com/blog/post/2025/11/14/meity-notified-digital-personal-data-protection-rules-2025/)). Press reports dated 14 Nov explain the difference. Plan to be compliant by **April 2027**. |

### 3.3 The founder's draft: right, wrong and missing

- **Right, and kept:** "administrative remote control, not a chatbot"; read-back, confirm, clarify-don't-guess; no biometrics; revocable tenant/room-scoped device tokens; teacher permissions from MySMS IAM; offline queue with idempotency keys; the Google region-versus-language trap; evidence-gated rollout; software-only tier; pilot metrics.
- **Wrong or weak:** Alexa Built-in and existing smart speakers listed as viable paths (both closed, §2.3); a wake phrase as default trigger; Google-first speech; names instead of roll numbers; a ₹10,000–18,000 device allowance not tied to a specific device; "free device" treated as a first-class option.
- **Missing:** answers to the two direct questions; teacher time-use evidence; form-factor comparison; use cases mapped to operations that exist; name/noise evidence; certifications; the DPDP education exemption; cost per classroom; the pivots in §12. Full list in §17.

---

## 4. Is a voice device the right form factor?

### 4.1 What teachers lose time on

- **India, government schools:** a NIEPA study (2018) found teachers spent only **19.1%** of annual hours on teaching. **42.6%** went to non-teaching core activities, **31.8%** to non-teaching school-related activities and **6.5%** to other department work ([EducationWorld](https://www.educationworld.in/teachers-spend-only-19-1-per-cent-time-teaching-says-nuepa-report), [Accountability Initiative](https://accountabilityindia.in/blog/relooking-non-teaching-roles-of-government-school-teachers/)). That burden is records, surveys and duplicate reporting, not the minutes of roll call.
- **OECD TALIS 2018:** in an average lesson, about 78% of time goes to teaching, 13% to keeping order and 8% to administration ([Brussels Times](https://www.brusselstimes.com/58954/teachers-spend-less-time-teaching-talis-study-shows)). India's participation in TALIS is **unverified**.
- **Duplication is the real attendance pain:** register, then app, then state portal; Karnataka teachers report OTP failures in the SATS app ([Careers360](https://news.careers360.com/karnataka-scrap-facial-recognition-attendance-govt-school-data-privacy-mid-day-meal-ai-app-digital-madhu-bangarappa-education-news/amp)).
- **The only study of a purpose-built classroom voice assistant** (Merlyn Mind, UC Irvine 2022, ~79 US teachers, 7 weeks): teachers *perceived* more teaching time, but the measured change was not significant; they used voice for display, files, timers and video, **not attendance**; some reported misrecognised accented English ([UCI report](https://www.prometheanworld.com/wp-content/uploads/pdf/UCI-AI-in-Classrooms-Final-Report_-2022_v2.pdf)). Its far-field mics could be disabled so only a handheld remote triggered it ([EdTech Magazine](https://edtechmagazine.com/k12/article/2021/11/new-digital-assistant-education-supports-teachers-classroom)).
- **Attendance arithmetic (estimate):** 40 names × ~4 s ≈ 160 s by roll call vs ~15–30 s to tap 3 exceptions; across 8 periods, ~21 min/day vs ~2–4 min. **The saving comes from the pattern, which MySMS can ship without voice.**

### 4.2 Form-factor comparison

| Option | Attendance speed | Reliability | Privacy/legal | Incremental cost per room | Verdict |
|---|---|---|---|---|---|
| Teacher app, exception tap (default present, leave pre-filled, carry forward) | Fastest (estimate ~20 s) | Highest; visual confirm | Lowest; identity is native | ~₹0 | **Build first** |
| Push-to-talk voice in the teacher app | Fast for 1–5 exceptions; best for capture and queries | Good with roster matching and on-screen confirm | Low; teacher's close-talk voice only | Speech only (§9) | **Build in the pilot** |
| Interactive flat panel (IFP) app | Medium | Good; big shared confirm screen | Low; no new mic | Software only. About 35% of government schools (class 6+) have smart classrooms ([ThePrint](https://theprint.in/india/education/just-35-govt-schools-in-india-have-smart-classrooms-bihar-up-among-worst-performers-centre-to-parliament/2699817)) | **Phase 2** |
| Mounted Android tablet | Medium | Good | Medium (shared device, theft) | ₹8,000–15,000 (estimate) | Only where teachers lack phones or schools ban phones in class |
| RFID/QR cards | Fast at the gate; weak per period | Lost cards, proxy taps, reader upkeep | Low–medium | Reader plus cards | A different problem (gate attendance) |
| Face-recognition camera | Fast when it works | Lighting and pose errors cause wrongful absences | **High**: biometrics of minors. The IFF argues such systems fail Puttaswamy's proportionality test ([IFF](https://internetfreedom.in/privacy-marked-absent/)). Telangana adopted it in 2023 with 20,000 tablets ([Telangana Today](https://telanganatoday.com/telangana-no-roll-calls-as-govt-schools-adopt-ai-facial-recognition)); Karnataka dropped its plan | — | **Do not** |
| Dedicated smart speaker | Slower than tapping beyond ~5 absentees; no visual confirm | Weakest: far-field in noise | Highest perceived risk | Highest (§9) | **Last, if ever** |

**Verdict:** a voice *device* is the wrong first form factor. Voice is a good *input mode*, with the phone or a Bluetooth lapel mic as the microphone and a screen for confirmation. A voice box also cannot verify who is present: any child can say "present" for a friend.

---

## 5. Use cases, ranked, with interaction scripts

### 5.1 Ranking (value × feasibility × MySMS readiness)

Readiness is taken from [operations-status](../api/operations-status.md) as of today. Attendance has 33 of 33 operations served; timetable 23 of 25; comms 202 of 235.

| # | Use case | Value | Voice feasibility | MySMS readiness | Rank |
|---|---|---|---|---|---|
| 1 | Exception attendance (tap and voice), pre-filled from approved leave | High (daily, every period) | High with roll numbers | `TakeAttendanceForSession`, `RecordAttendance` served; `sis.*StudentLeaveRequest` served | **Pilot** |
| 2 | "Who is absent?" / attendance summary | High | High (read-only) | `GetAttendanceSheet`, `SummarizeAttendance` served | **Pilot** |
| 3 | Homework dictation → draft → publish | High (typing is slow) | High (teacher's own words) | `assessment.CreateHomework`, `PublishHomework` served; approval flow exists | **Pilot** |
| 4 | "What's my next period?" / today's timetable | Medium | High | `timetable.GetTimetable` served | **Pilot** |
| 5 | Absence → guardian notification loop | High (parents and safety) | n/a (triggered by #1) | Notices served; **`comms.SendMessage` is 501** | Pilot, if messaging ships |
| 6 | Parent message draft (one guardian), translated, previewed | High | Medium (free text) | Blocked on `comms.SendMessage` | Phase 2 |
| 7 | Class diary / "covered today" | Medium–high | High | **No class-diary operation found**; lesson plans served | Phase 2 (needs model) |
| 8 | Substitution query ("Who covers 7A P3?") | High for coordinators | High | **No substitution operation found** in operations-status | Phase 2 (build substitution first) |
| 9 | Timers, period-end warning | Low–medium | High (local) | None needed | Panel/display phase |
| — | Marks entry, fees, discipline, health remarks, student chatbot, noise "monitoring" by identity | — | — | — | **Never by voice** |

### 5.2 Scripts (push-to-talk; ▸ = teacher holds the button)

**A. Exception attendance with roll numbers and read-back**
```
▸ "Attendance. Absent four, seventeen, twenty-three."      (Hindi: "Absent chaar, satrah, teis")
◂ Screen + voice: "8B, Maths, period 1. Already on approved leave: 9 Kavya, 31 Imran.
   New absent: 4 Aarav Shah, 17 Priya Nair, 23 Ravi Kumar. 37 present. Confirm?"
▸ "Yes"  (or tap Confirm)
◂ "Saved at 9:04. Guardians will be notified per school policy. Say 'undo' until period ends."
```
Ambiguity: `▸ "Absent Aarav"` → `◂ "Two matches: roll 4 Aarav Shah, roll 11 Aarav Singh. Which roll number?"`. The assistant never picks the nearest match.  
Carry forward: `▸ "Same as period one"` shows a diff and asks for confirmation.  
Late: `▸ "Seventeen came late, nine twenty"` records a late mark and needs confirmation.

**B. Who is absent**
```
▸ "Who is absent today?"
◂ "8B today, period 1 sheet submitted 9:04 by you: 5 absent, 2 on approved leave. Names are on screen."
```
By default the assistant speaks counts and shows names on screen. It reads names aloud only on request ("read them"), because a room of children is listening. It always states which sheet, date and session it used.

**C. Timetable / substitution**
```
▸ "What's next?"          ◂ "Period 4 at 10:40: 7A Science, Room 12."
▸ "Any substitution for me today?"   ◂ (needs a substitution model; today: "Not available yet")
```

**D. Homework / class diary capture**
```
▸ "Homework for seven B maths: exercise 4.2, questions 1 to 10, due Friday."
◂ Draft card: Class 7B · Mathematics · "Exercise 4.2, Q1–10" · Due Fri 13 Nov · [Edit] [Publish]
```
Publishing is a tap after review and goes through the existing approval workflow. Diary entries ("Diary: finished chapter 3, quiz Monday") follow the same pattern once a diary entity exists.

**E. Parent message (one guardian, never bulk)**
```
▸ "Message Priya Nair's parents: she missed the unit test, she can take it Monday."
◂ Draft in the teacher's language, plus a translation into the guardian's preferred language,
   with a preview showing the recipient name and channel. [Send] only on tap.
```
Bulk messages, class-wide notices and anything about health or discipline are never sent from voice.

---

## 6. Reliability, then architecture

### 6.1 Indian names and noisy classrooms

- **Expect errors.** On classroom audio Whisper scored 24.46% WER zero-shot (19.14% fine-tuned) on NCTE recordings and 30.15% on another corpus ([arXiv 2409.14494](https://arxiv.org/pdf/2409.14494v2)). On Indian-accented English (Svarah), Whisper-large scored 7.2% vs 20.7–30.0% for commercial systems, worst on entity-heavy prompts ([Svarah](https://ar5iv.labs.arxiv.org/html/2305.15760)). On Hindi (Vistaar), IndicWhisper averaged 13.6% vs 20.0% Azure and 23.9% Google ([Vistaar](https://github.com/AI4Bharat/vistaar)). MySMS classroom rates are **unverified** until the pilot.
- **Shrink to a closed set.** Bias recognition with the session roster (Chirp 3 accepts up to 1,000 bias phrases, [Google](https://docs.cloud.google.com/speech-to-text/docs/models/chirp-3); AWS Transcribe supports streaming custom vocabulary for hi-IN/en-IN, [AWS](https://docs.aws.amazon.com/transcribe/latest/dg/supported-languages.html)), then match in **our own code** against ≤60 candidates using phonetic keys over Latin and native-script transliterations plus per-school aliases.
- **Roll numbers first**: digits are a tiny, well-trained vocabulary; names are read back for confirmation.
- **Close-talk mic**: a phone at 20–30 cm or a Bluetooth lapel mic beats a far-field array 4 m away competing with 50 children (engineering judgement, all three experts).
- **Confidence policy**: low-confidence or duplicate matches → clarifying question; every write → read-back and on-screen diff; undo until period end; corrections logged as a quality metric, never a teacher-performance metric.

### 6.2 Architecture

```text
Teacher phone (RN app)  ── or ──  Panel/display app paired by QR for the period
 │ push-to-talk only (no wake word); ≤10 s clips; VAD; offline queue (SQLite)
 │ teacher's own JWT  (room device holds only a read-only room credential)
 ▼
voice-gateway (Spring Boot, India region)
 ├─ STT adapter: Sarvam | AWS Transcribe | self-hosted IndicConformer (benchmarked)
 ├─ roster-biased recognition → phonetic matcher (≤60 candidates, roll numbers)
 ├─ intent: deterministic grammar first; LLM fallback emits JSON from a closed tool schema
 ├─ policy: permission re-check, class/session binding, confidence and confirmation rules
 ├─ calls existing MySMS REST/gRPC AS THE TEACHER (tenant/institution scoped, idempotency key)
 └─ audit row: teacher, device, intent, diff, confirmation; audio discarded after STT
 ▼
MySMS IAM · attendance · timetable · assessment(homework) · sis(leave) · comms
```

Key decisions:

- **Push-to-talk**: no ambient capture, no child triggers, better signal-to-noise, and a natural moment to bind identity. Espressif's on-device ESP-SR supports only English and Chinese command words ([ESP-SR docs](https://docs.espressif.com/projects/esp-sr/en/latest/esp32s3/speech_command_recognition/README.html)), so on-device Hindi is not an option there.
- **Teacher identity**: best, the teacher's phone is mic and identity. For a shared room device, the teacher claims it per period (QR, NFC/BLE tap or PIN) and the server issues a token scoped to that class and period; a teacher not timetabled in that room is flagged as a possible substitution. Never voice biometrics (biometric data, fails in noise, spoofable).
- **LLM in a cage**: grammar for the top intents (~20 intents may cover 95% of use, an assumption); the LLM is a fallback that emits a whitelisted, schema-validated call, never writes without confirmation and never sends free text to parents without preview. India-hosted LLMs exist (Sarvam lists `sarvam-105b`, [pricing](https://docs.sarvam.ai/api-reference-docs/getting-started/pricing)).
- **Offline**: the tap flow and queued writes work offline; voice does not promise offline; timetable, bell and timers are cached on any room device.
- **Fleet/OTA (device phase only)**: signed A/B firmware, staged 1%→10%→100% rollout, ESP-IDF secure boot + flash encryption or a Mender/RAUC-style Linux updater (tooling is an assumption), per-device certificates, remote mic kill and revoke, audio-free telemetry, per-school asset register.
- **Physical security**: wall mount above 2 m, tamper screws, no exposed USB data port, debug ports disabled, only today's encrypted roster cache on the device.

### 6.3 Speech-to-text options and data residency

| Option | Indian languages | India residency | Price / licence |
|---|---|---|---|
| **Sarvam Saaras v3** | 22 scheduled languages plus Indian English, code-mixing; streaming ([Sarvam](https://www.sarvam.ai/speech-to-text)) | Vendor states all processing in India; VPC and on-prem offered ([Sarvam](https://www.sarvam.ai/speech-to-text)). Verify contractually | ₹0.49/min (₹29.40/h) pay-as-you-go, excl. GST ([pricing](https://docs.sarvam.ai/api-reference-docs/getting-started/pricing)) |
| **AWS Transcribe** | Streaming for hi-IN, en-IN, bn, gu, kn, ml, mr, or, pa, ta, te; custom vocabulary ([AWS](https://docs.aws.amazon.com/transcribe/latest/dg/supported-languages.html)) | Mumbai is not on the streaming exclusion list; **confirm** | Not checked |
| **Google Chirp 3** | en-IN and hi-IN GA; ten more Indian languages in preview ([Google](https://docs.cloud.google.com/speech-to-text/docs/models/chirp-3)) | Listed only in `us` and `eu` multi-regions; **no India region** (checked 10 Nov 2026). The earlier draft's note on Chirp 2 in Singapore was not re-verified | US$0.016/min standard ([pricing](https://cloud.google.com/speech-to-text/pricing)); the earlier draft costed US$0.024/min no-logging |
| **Azure Speech** | Not checked | Central India is a Speech region ([Azure](https://learn.microsoft.com/en-us/azure/ai-services/speech-service/regions)); real-time STT there **unverified** | Not checked |
| **AI4Bharat IndicConformer 600M** | 22 languages; Hindi WER 13.2 on Vaani; card recommends newer Indic-Transcribe ([Hugging Face](https://huggingface.co/ai4bharat/indic-conformer-600m-multilingual)) | Self-hosted wherever you run it | MIT licence; you own GPU operations |
| **Bhashini** | Government stack, 22 scheduled languages ([MoE/PIB](https://education.gov.in/sites/upload_files/mhrd/files/PIB2039811.pdf)) | Government-run | SLA, quotas and commercial terms **unverified** |
| **Whisper (self-hosted)** | Strong on Indian English ([Svarah](https://ar5iv.labs.arxiv.org/html/2305.15760)); regional languages **unverified** | Self-hosted | Open weights |

**Recommendation:** start the pilot on Sarvam (India processing, Indian-language coverage, lowest verified price), with AWS Transcribe (Mumbai, to be confirmed) as the second adapter. Score providers on **roster-match accuracy on our own recordings**, not raw WER. Keep self-hosted IndicConformer as the sovereignty and cost fallback. Vendor contracts must prohibit training on our data, require India processing and require prompt deletion.

---

## 7. Hardware (only after the gates in §11)

### 7.1 Options

| Tier | Example (verified data) | Use |
|---|---|---|
| Demo kit | Espressif ESP32-S3-BOX-3: two mics, 2.4" touchscreen, speaker, mute button; US$49.99 kit ([CNX](https://www.cnx-software.com/2023/09/01/esp32-s3-box-3-devkit-comes-with-2-4-inch-display-dual-microphone-pcie-expansion-connector/)), £61.90 UK retail ([The Pi Hut](https://thepihut.com/collections/adafruit/products/espressif-esp32-s3-box-3)) | Demos only |
| Open reference | Home Assistant Voice PE, US$59 (§2.2) | Fork for a pilot |
| Far-field front end | Seeed ReSpeaker XMOS XVF3800: 4 mics, echo cancellation, beamforming, ~5 m pickup, mute button; US$49.99–54.50 ([CNX](https://www.cnx-software.com/2025/07/29/respeaker-xmos-xvf3800-4-mic-array-board-features-esp32-s3-module-works-over-usb/)) | Only if far-field is ever required |
| Linux SoC / SBC | Rockchip RK3308, built for smart speakers ([CNX](https://www.cnx-software.com/2018/03/09/rockchip-unveils-rk3308-gemini-cortex-a35-processors-for-smart-speakers/), search only); Raspberry Pi CM5 from US$45 at launch ([Raspberry Pi](https://www.raspberrypi.com/news/compute-module-5-on-sale-now/)), prices up Oct 2025 ([CNX](https://www.cnx-software.com/2025/10/02/ddr4-supply-demand-stress-leads-to-raspberry-pi-cm4-cm5-price-increases/)) | RK3308: Shenzhen ODM ecosystem; CM5: easy dev, pricey for a fleet |
| Reference retail | Echo Pop launched in India at ₹4,999 ([About Amazon India](https://www.aboutamazon.in/md/news/devices/amazon-launches-the-all-new-echo-pop-in-india)) | Price anchor schools will compare against |

### 7.2 Indicative cost (all **estimates**, not quotes; ±40%)

| Item | Range | Basis |
|---|---|---|
| ESP32-S3 push-to-talk + display/bell unit (2 MEMS mics, 3 W speaker, small LCD or LED ring, button, hardware mute, moulded wall enclosure) | BOM ₹1,800–3,000; landed ₹3,000–5,000 at 1,000–5,000 units | Opus estimate |
| Same with XMOS-class far-field front end | add ₹1,500–2,500 | Opus estimate; XVF3800 retail above |
| Fuller custom unit (front end, LED ring, small screen, PSU, assembly) | ₹4,000–7,000 per unit at 1,000 units | Fable estimate |
| Tablet/SBC kit with mount and installation | ₹8,000–15,000 (Sonnet); ₹10,000–18,000 installed ([funding estimate](FUNDING-AND-CLOUD-COST-ESTIMATE.md)) | Estimates |
| One-off: industrial design ₹3–10 lakh; tooling ₹5–15 lakh; certification ₹3–8 lakh | **₹11–33 lakh** (Opus); Fable puts tooling plus engineering at ₹15–40 lakh plus "a few lakh" for certification, so **₹11–45 lakh** combined | Estimates; get three ODM/EMS quotes at 100, 1,000 and 5,000 units |

At 1,000 units, the one-off cost adds ₹1,100–4,500 per unit. A 1,000-unit custom device cannot match a ₹4,999 Echo on price or audio quality. That is one more reason to go output-first and reuse existing hardware.

### 7.3 Manufacturing route

Indian EMS majors exist; Dixon has had a 50:50 wireless-audio joint venture with boAt since 2022 ([boAt](https://www.boat-lifestyle.com/en-ae/blogs/news/boat-and-dixon-technologies-form-joint-venture-to-design-and-manufacture-wireless-audio-solutions-in-india)). At 1,000–5,000 units we are below most tier-1 minimums (assumption). The realistic route is a Shenzhen ODM reference design, final assembly at a small Indian EMS (which helps with the WPC module route and "Made in India" procurement), and an Indian ID studio for the shell. Incentive schemes are irrelevant at this volume: IT Hardware PLI 2.0 targets laptops, PCs and servers ([Vajiram & Ravi](https://vajiramandravi.com/current-affairs/?p=35470)), and the ₹22,919 crore ECMS targets components ([PIB](https://static.pib.gov.in/WriteReadData/specificdocs/documents/2025/oct/doc20251027678301.pdf)).

### 7.4 India certifications

- **BIS CRS:** smart speakers (with and without display) are under the Compulsory Registration Scheme, IS 616:2017, mandatory from 1 Apr 2021 ([Enviliance](https://enviliance.com/regions/south-asia/in/report_1515)). This requires testing at a BIS-recognised lab and an Authorised Indian Representative for foreign makers; registration is typically valid two years ([Agile Regulatory](https://www.agileregulatory.com/blogs/bis-crs-registration-of-smart-speakers-with-or-without-display-is-616-2017)). Confirm against the official BIS CRS list. The category for a **display-only** device and separate registration for the power adapter are **unverified**; ask the lab.
- **WPC ETA** (Wi-Fi/BT in de-licensed bands): approval by self-declaration on the Saral Sanchar portal, backed by an accredited RF test report; the certificate is per model. Espressif holds them for its modules ([Espressif ETA example](https://documentation.espressif.com/ETA-SD-20200704087%20ESP32-PICO-V3-ZERO%20WPC.pdf)). DoT lists a ₹10,000 fee per product/model ([DoT ETA](https://eservices.dot.gov.in/equipment-type-approval-eta), as cited in the [funding estimate](FUNDING-AND-CLOUD-COST-ESTIMATE.md)). Whether a finished product needs its own ETA is in dispute (see §3.2); budget for it.
- **TEC MTCTE:** the 2022 exemption list covered handsets, servers, smart watches, smart cameras and PoS devices, not smart speakers ([Khaitan & Co](https://khaitanco.com/thought-leaderships/Mandatory-testing-and-certification-of-telecom-equipment-Regulatory-overlaps-pave-way-for-exemptions)). Applicability to a Wi-Fi-only classroom device is **unverified**; get a written lab opinion.

---

## 8. Privacy, safety and trust (DPDP and minors)

**Timeline.** The DPDP Rules 2025 were notified on 13 Nov 2025. Consent-manager provisions apply from 13 Nov 2026. Children's data, notice/consent standards and most fiduciary duties apply from **13 May 2027** ([AZB](https://www.azbpartners.com/bank/update-indias-digital-personal-data-protection-framework-comes-into-effect/), [SCC Online](https://www.scconline.com/blog/post/2025/11/14/meity-notified-digital-personal-data-protection-rules-2025/)). Penalties reach ₹250 crore for failing to take reasonable security safeguards ([Biometric Update](https://www.biometricupdate.com/202511/india-notifies-its-sweeping-digital-personal-data-protection-rules)).

**The education exemption is narrow and belongs to the school.** The Fourth Schedule exempts an educational institution from verifiable parental consent (s.9(1)) and the tracking/behavioural-monitoring ban (s.9(3)) only where processing is "restricted to tracking and behavioural monitoring for the educational activities of such institution" or "in the interests of safety of children enrolled" ([Fourth Schedule text](https://www.dpdpa.com/schedule/schedule4.html)). Commentators say the scope is contested and should be read narrowly ([MediaNama](https://www.medianama.com/2025/11/223-dpdp-rules-tracking-children-parental-consent/)). Our reading, **to be confirmed by counsel**:

- Attendance processed by the school, with MySMS as its processor, fits.
- Capturing ambient classroom audio that contains children's voices is a different activity and harder to fit.
- MySMS training models on classroom audio would fall outside the exemption.
- The IFF notes the DPDP Act has no special "sensitive" tier for biometrics ([IFF](https://internetfreedom.in/privacy-marked-absent/)). That is not permission: a child's voiceprint or face template is the fastest way to lose a school.

**Design commitments to publish:**

1. The mic is electrically off unless the button is held. A hardware mute cuts mic power, and the light is wired to the mic rail, not to software.
2. Only the teacher's utterance is processed. Audio is discarded after transcription. Transcripts are kept only as structured audit rows (teacher, time, intent, diff). Model-improvement audio requires opt-in **per adult teacher**, never children's audio. Amazon's own property product advertises "voice recordings are never saved" ([ASP comparison](https://developer.amazon.com/docs/alexa/alexa-smart-properties/product-feature-comparison.html)); match that.
3. No voice biometrics, no child-speech recognition, no emotion or attention analytics, no ads, no student chatbot.
4. Names are spoken aloud only on request. Sensitive data (health, discipline, fees, marks) is never read aloud.
5. Processing in India, named sub-processors, vendor no-training clauses; a one-page parent notice in the school's languages; a teacher FAQ; a "what this can never do" sticker on any device.
6. Contract roles: school as fiduciary, MySMS as processor; purpose, retention, deletion, incident escalation and device return (the earlier draft's list stands).
7. **Teacher trust:** no talk-time or usage-per-teacher reports to management in year one; principals cannot listen; no arrival timestamps derived from voice; co-design with a teacher panel. The Merlyn study found involvement in refining the tool reduced fear of replacement ([UCI](https://www.prometheanworld.com/wp-content/uploads/pdf/UCI-AI-in-Classrooms-Final-Report_-2022_v2.pdf)).
8. **Child safety** for any device: low-voltage supply, no exposed cables, wall mount out of reach, no open Bluetooth pairing.

---

## 9. Cost per classroom per year (estimates)

Basis: 220 school days; 50 students per classroom; 1,000 classrooms; 3-year device amortisation; 10% annual replacement; field support ₹500–1,000 per device per year (Fable estimate). Speech at Sarvam's ₹0.49/min, or at Google no-logging US$0.024/min × ₹95 = ₹2.28/min as in the [funding estimate](FUNDING-AND-CLOUD-COST-ESTIMATE.md).

| Configuration | Per classroom/year | Per student/year | Working |
|---|---|---|---|
| Speech only, push-to-talk, 3–5 min/day | ₹323–539 (Sarvam); ₹1,505–2,508 (Google no-logging) | ₹6–50 | 660–1,100 min × ₹0.49 or × ₹2.28 |
| Earlier draft's voice-cloud line (STT + LLM + TTS, 5–20 commands × 15 s/day) | ₹1,080–4,320 if billed 12 months | ₹22–86 | ₹0.9–3.6 lakh/month ÷ 1,000 × 12 |
| Software-only voice add-on (speech + LLM + hosting) | ₹300–1,500 | ₹6–30 | Fable estimate; LLM only on grammar misses |
| Custom ESP32 unit at ₹3,000–6,000 landed, plus speech | ₹2,100–5,100 | ₹42–102 | (₹1,000–2,000 amortisation) + (₹300–600 replacement) + (₹500–1,000 support) + (₹300–1,500 speech) |
| Tablet/SBC kit at ₹10,000–18,000 installed, plus speech | ₹5,100–10,300 | ₹102–206 | (₹3,333–6,000) + (₹1,000–1,800) + (₹500–1,000) + (₹300–1,500) |
| Excluded | One-off ₹11–45 lakh design/tooling/certification; a managed SIM at ₹150–400/device/month = ₹1,800–4,800/yr if Wi-Fi is absent ([funding estimate](FUNDING-AND-CLOUD-COST-ESTIMATE.md)) | | |

Always-on streaming would cost 6 h × ₹29.40 × 220 days = ₹38,808 per classroom per year at Sarvam rates (Opus arithmetic). That is another reason push-to-talk is non-negotiable. The draft's 15-second-per-command assumption is conservative; measure p50/p95 clip length in the pilot.

---

## 10. Business model

| Model | Take |
|---|---|
| **Voice/assistant as a paid MySMS add-on** (per classroom or per teacher per year) | **Yes, from the pilot.** Near-zero cost of goods; sells the time saved, not a gadget |
| Panel-vendor bundle (MySMS app on their IFP) | Explore in phase 2. Merlyn Mind went to market through Promethean panels ([EdTech Magazine](https://edtechmagazine.com/k12/article/2021/11/new-digital-assistant-education-supports-teachers-classroom)) |
| Device leased inside a 3-year subscription; MySMS owns replacement | Only after Gate C. The school never faces a capital purchase |
| Refundable deposit / lease-to-own; school-group bulk contract | Acceptable variants of the lease |
| Free device | **No.** Theft, breakage and support with no perceived value. CSR sponsorship is a one-off, not recurring revenue |
| Software-only tier | Always available; protects schools that do not want hardware |

The ₹300/₹600/₹900 per-student price scenarios in the funding document remain **hypotheses**. Test them with paying pilots. Even the tablet configuration (₹102–206 per student/year, §9) fits under ₹300 only before staff, sales and support costs.

---

## 11. Phased roadmap with gates

**Phase 0 — measure (weeks 0–3).**

- Time-motion baseline in 10 classrooms: seconds per attendance (paper and app), per-period frequency, duplicate entry into state portals, time spent on homework and diary entry.
- Collect about 200 consented teacher utterances (accented English, Hindi–English code-mix, one regional language).
- Get a one-hour IP counsel call and a DPDP counsel read of the Fourth Schedule for school/processor roles.
- **Gate 0:** a measurable admin burden (target: at least 10 min/teacher/day across attendance, homework and diary; estimate to be validated). Counsel sign-off on the push-to-talk, no-audio-retention design.

**Phase 1 — zero-hardware pilot (months 0–3).** Three to five schools, about 30–50 volunteer teachers, English and Hindi.

- Exception-tap attendance: default present, approved leave pre-filled, "same as period 1", undo until period end, offline queue with idempotency keys.
- Push-to-talk assistant in the RN app with about 10 intents: attendance exceptions, who is absent, late, undo, submit, next period, homework draft, diary draft (if the entity exists), reminder.
- Voice gateway with Sarvam and AWS adapters, roster-biased matcher, confirmation UI, audit log.
- Guardian absence notification, if `comms.SendMessage` or the notice path is ready.

**Gate A (end of month 3) — go/no-go for continuing voice:**

| Metric | Go | Kill / rethink |
|---|---|---|
| Wrong-student writes after confirmation | ≤0.5% of accepted changes (the draft's 99.5% precision target) | >2% |
| Cross-tenant or cross-class data exposure | Zero | Any |
| Teachers using voice weekly at week 8 (Fable's gate); stretch: ≥3 days/week (the draft's metric) | ≥40% | <20%: keep tap and capture, drop voice-first plans |
| Median attendance time, voice vs tap | Voice no slower than tap | Voice slower **and** low use |
| Intent success without retry | ≥90% (assumption) | <75% |
| Post-submit correction rate | <1–2% | >5% |
| Homework/diary entries per teacher per week | Rising vs baseline | Flat |
| Attendance submitted within 10 min of bell; guardian alert latency | Improves vs baseline | No change |
| Speech + LLM cost per classroom | Within the §9 range | >2× range |
| Raw audio retained | Zero (verified) | Any |

**Phase 2 — shared surfaces (months 3–6).**

- Panel/kiosk app with QR claim: current period, substitute, homework due, attendance summary, timers.
- Bluetooth lapel mic or clicker.
- Substitution model in timetable; parent-message drafts with translation.
- One regional language chosen by pilot demand.
- **Gate B:** daily panel use in at least 60% of pilot rooms that have panels (assumption); teachers explicitly ask for hands-free; at least one school signs a paid pilot.

**Phase 3 — hardware decision (months 6–12).**

- 50–100 room pilot of an **output-first** display/bell unit with optional push-to-talk, built on off-the-shelf boards in an original enclosure (pilot units, not for sale).
- Three ODM/EMS quotes; written BIS/WPC/MTCTE opinions.
- **Gate C:** ≥98% device uptime in school hours (assumption); support tickets within budget; signed demand for several thousand rooms before custom ID and tooling (Fable).

**Year 2:** custom industrial design, BIS CRS and WPC, assembly by an Indian EMS, sold under a lease-in-subscription model.

---

## 12. Non-obvious pivots (ranked)

1. **"Predict, then confirm" attendance:** open the sheet pre-filled with approved leave and earlier-period absences, then ask "Three on approved leave. Anyone else absent?" Prediction saves the time; voice only confirms (Fable). Leave data already exists (`sis.*StudentLeaveRequest` served).
2. **Absence → guardian notification loop:** "marked absent in period 1" at 9:10, not 4 pm. Parents value this more than teachers value voice; it makes attendance a safety feature (Opus, Sonnet). Needs `comms.SendMessage` (501 today).
3. **Teacher admin copilot on any surface:** one intent layer for phone, panel, chat and later a speaker. A principal's "which classes are below 85% this week?" and after-class dictation of diary, homework, leave and parent notes are where the minutes are (Opus, Sonnet).
4. **"Enter attendance once" for state portals:** export or integrate to state portals and UDISE-style reports, plus a printable register (Opus). State-portal APIs **unverified**; start with SATS (Karnataka).
5. **Output-first classroom display:** networked bell, clock, "Next: Maths, Mrs Rao, Room 12", substitutions, announcements. No microphone, no privacy debate, replaces runner-carried substitution chits, and puts MySMS in every room (Opus, Fable).
6. **Panel app via OEM partners:** the school already bought the screen (all three experts).
7. **One speech server per school:** a mini-PC running an MIT-licensed Indic model serves every room over the LAN; no audio leaves the school (Fable). Feasibility **unverified**; test in phase 2.

---

## 13. Risks that could kill it

1. **A wrong absence reaches a parent** (mitigate: roll numbers, read-back, explicit confirm, undo, alert delay).
2. **A "spy device in my child's classroom" story** (push-to-talk, wired mute light, no audio retention, parent notice, output-first hardware).
3. **Teachers prefer tapping** or feel awkward talking to a device; Merlyn showed no significant measured time saving ([UCI](https://www.prometheanworld.com/wp-content/uploads/pdf/UCI-AI-in-Classrooms-Final-Report_-2022_v2.pdf)). Gate A catches this.
4. **ASR fails on regional names** in the first staff-room demo (benchmark first).
5. **Hardware support swamps a software team**: only 63.5% of Indian schools report internet and 93.6% electricity ([UDISE+ 2024-25 summary](https://educationforallinindia.com/63-percent-of-indian-schools-now-online-yet-over-25000-languish-without-electricity/)); in-classroom coverage is worse (assumption).
6. **DPDP from 13 May 2027** with an unclear fiduciary/processor split or a narrow exemption reading.
7. **Product readiness**: no substitution or class-diary operations, outbound messaging 501; stay inside tested flows ([STATUS](../STATUS.md)).
8. **Vendor dependency** on speech pricing, regions and languages (provider abstraction, tap fallback).
9. **Design-infringement notice** if a device resembles Echo; **device liability** for repair, updates, collection and e-waste.

---

## 14. Open questions

1. Actual minutes per teacher per day on attendance, homework and diary (Phase 0)?
2. Are teachers' phones allowed in class? If not, what is panel/tablet coverage?
3. Are roll numbers stable and known to every teacher?
4. Which provider wins on roster-match accuracy, and will Sarvam/AWS contract for India-only processing and no training?
5. Counsel's view of MySMS as processor under the Fourth Schedule, and of incidental child audio?
6. Guardian alerts immediate, or delayed (e.g. 15 min) to allow corrections?
7. Which state portals (SATS and others) accept imports or APIs?
8. Will panel OEMs allow a third-party app, on what terms?
9. Does a display-only or push-to-talk unit fall under BIS CRS IS 616, and need its own WPC ETA on a certified module?
10. What will schools pay per classroom per year without hardware?

---

## 15. Build-later specification outline

(1) User stories and permission matrix; (2) intent allowlist, grammar, LLM fallback contract, confirmation rules per write; (3) roster matcher (roll numbers, phonetics, transliteration, aliases, duplicates); (4) speech adapters, benchmark protocol, residency terms; (5) identity: teacher token, read-only room credential, QR/NFC claim, expiry, revoke; (6) audit, retention, notices, sub-processors, incidents; (7) offline queue and idempotent sync; (8) missing MySMS capabilities: `comms.SendMessage`, substitution, class diary, guardian-alert policy; (9) panel/display app and pairing; (10) device phase only: BOM, ODM quotes, acoustics, factory test, secure boot/OTA, BIS/WPC/MTCTE, warranty, e-waste; (11) pilot protocol, gates, training, SLA, stop conditions; (12) commercial terms.

---

## 16. How this brainstorm was produced

The founder's draft was the base. Three independent model brainstorms (**Opus 5.5**, **Sonnet**, **Fable 5.2**) answered the same brief without seeing it; this document synthesises all four, keeps consensus, resolves conflicts with a stated call (§3.2) and corrects the draft. Re-checked on 10 Nov 2026: DPDP phase dates (AZB), the Alexa+ India timeline (TechCrunch), AVS status (Amazon), Chirp 3 regions (Google) and MySMS operation status (`api/operations-status.md`). Unverifiable claims stay marked **unverified**; estimates are judgement with a stated basis, not quotes.

---

## 17. Changes from the founder's draft

1. Added answers to "where does Amazon source Echo" (Lab126 design, Foxconn assembly, no verified India Echo production) and "same look" (Designs Act s.22, Copyright s.15(2), passing off), with what to do instead.
2. **Removed** Alexa Built-in and existing smart speakers as paths (AVS tools no longer generally available; Smart Properties has no education vertical or India listing).
3. Trigger changed from wake phrase to **push-to-talk only**.
4. Attendance script now uses **roll numbers with name read-back**, leave pre-fill, carry-forward, undo and explicit confirm.
5. Added teacher time-use evidence and a form-factor comparison: voice is an input mode, not a device.
6. Ranked use cases against **actual MySMS readiness** (no substitution or class-diary operations; `comms.SendMessage` is 501) and added five interaction scripts.
7. Added name/noise evidence and the roster-constrained matcher.
8. Replaced Google-first speech with a residency-aware table (Sarvam and AWS lead; Chirp 3 has no India region).
9. Architecture: teacher token, never device; read-only room device; grammar first, LLM in a closed schema.
10. Added hardware tiers, BOM and one-off **estimates**, ODM/EMS route, BIS CRS / WPC ETA / MTCTE.
11. Corrected the DPDP timeline (13 Nov 2025 / 13 Nov 2026 / 13 May 2027) and added the Fourth Schedule exemption and its limits.
12. Added cost per classroom per year, reconciling the draft's figures with expert estimates.
13. "Free device" downgraded to no; lease-in-subscription only after Gate C.
14. Rollout replaced by Phases 0–3 with **numeric go/no-go gates**.
15. Added pivots, kill risks and open questions; kept the draft's guardrails, offline design, revocation, metrics and spec outline, merged in place.

---

## Sources

**MySMS internal:** [operations status](../api/operations-status.md) · [backend status](../STATUS.md) · [architecture](../ARCHITECTURE.md) · [deployment](../DEPLOYMENT.md) · [funding and cloud cost estimate](FUNDING-AND-CLOUD-COST-ESTIMATE.md)

**Amazon / Echo:** [Lab126](https://en.wikipedia.org/wiki/Amazon_Lab126) · [CBS News 2019](https://www.cbsnews.com/news/amazon-echo-devices-children-in-china-forced-to-work-overtime-to-make-your-amazon-devices-report-says/) · [Vice 2019](https://vice.com/en/article/your-amazon-alexa-might-have-been-made-by-overworked-chinese-schoolchildren) · [ChannelNews (unopened)](https://www.channelnews.com.au/foxconn-compal-cash-in-on-amazons-smart-speakers/) · [The Week 2021](https://www.theweek.in/news/biz-tech/2021/02/16/amazon-to-start-device-manufacturing-including-fire-tv-sticks-in-india.html) · [TechRadar 2021](https://www.techradar.com/news/amazon-to-start-device-manufacturing-in-india-with-fire-tv-sticks) · [IndustryWeek 2019](https://www.industryweek.com/supply-chain/article/22028163/apple-iphone-amazon-echo-production-moving-from-china-to-india) · [Business Today, Echo Dot Max India](https://www.businesstoday.in/technology/news/story/amazon-launches-echo-dot-max-and-echo-studio-smart-speakers-in-india-533602-2026-05-27) · [Echo Pop India](https://www.aboutamazon.in/md/news/devices/amazon-launches-the-all-new-echo-pop-in-india) · [AVS deprecated features](https://developer.amazon.com/docs/alexa-voice-service/deprecated-features.html) · [ASP overview](https://developer.amazon.com/docs/alexa/alexa-smart-properties/about-alexa-smart-properties.html) · [ASP comparison](https://developer.amazon.com/docs/alexa/alexa-smart-properties/product-feature-comparison.html) · [Alexa skill policy](https://developer.amazon.com/en-US/docs/alexa/custom-skills/policy-requirements-for-an-alexa-skill.html) · [Kid skills](https://developer.amazon.com/en-US/alexa/alexa-skills-kit/get-deeper/custom-skills/kid-skills) · [Alexa+ India test, Jun 2026](https://techcrunch.com/2026/06/22/amazon-is-testing-alexa-in-india-with-hindi-support/) · [Alexa+ India launch, Sep 2026](https://techcrunch.com/2026/09/16/amazon-launches-alexa-in-india-with-hindi-support/) · [Alexa goes to school 2018](https://www.aboutamazon.in/news/devices/alexa-goes-to-school) · [Alexa Built-in overview](https://developer.amazon.com/en-US/alexa/branding/alexa-guidelines/brand-guidelines/smart-speakers-playbook/2-overview) · [Alexa device programmes](https://developer.amazon.com/en-US/alexa/devices)

**IP law:** [S.S. Rana, Designs Act s.22](https://ssrana.in/ip-laws/design-law-india/piracy-registered-design-india/) · [SCC Online, Copyright s.15(2)](https://www.scconline.com/blog/post/2024/10/16/from-art-to-industry-decoding-the-clash-between-copyright-and-design-in-indian-law/) · [Legal 500](https://www.legal500.com/intelligence/india/intellectual-property/between-art-and-industry-the-copyright-design-crossover)

**Teachers, schools, form factor:** [EducationWorld, NUEPA](https://www.educationworld.in/teachers-spend-only-19-1-per-cent-time-teaching-says-nuepa-report) · [Accountability Initiative](https://accountabilityindia.in/blog/relooking-non-teaching-roles-of-government-school-teachers/) · [Brussels Times, TALIS](https://www.brusselstimes.com/58954/teachers-spend-less-time-teaching-talis-study-shows) · [UCI Merlyn Mind report](https://www.prometheanworld.com/wp-content/uploads/pdf/UCI-AI-in-Classrooms-Final-Report_-2022_v2.pdf) · [EdTech Magazine](https://edtechmagazine.com/k12/article/2021/11/new-digital-assistant-education-supports-teachers-classroom) · [Careers360, Karnataka](https://news.careers360.com/karnataka-scrap-facial-recognition-attendance-govt-school-data-privacy-mid-day-meal-ai-app-digital-madhu-bangarappa-education-news) · [IFF](https://internetfreedom.in/privacy-marked-absent/) · [Telangana Today](https://telanganatoday.com/telangana-no-roll-calls-as-govt-schools-adopt-ai-facial-recognition) · [ThePrint, smart classrooms](https://theprint.in/india/education/just-35-govt-schools-in-india-have-smart-classrooms-bihar-up-among-worst-performers-centre-to-parliament/2699817) · [UDISE+ 2024-25 summary](https://educationforallinindia.com/63-percent-of-indian-schools-now-online-yet-over-25000-languish-without-electricity/)

**Speech:** [arXiv 2409.14494](https://arxiv.org/pdf/2409.14494v2) · [Svarah](https://ar5iv.labs.arxiv.org/html/2305.15760) · [AI4Bharat Vistaar](https://github.com/AI4Bharat/vistaar) · [IndicConformer](https://huggingface.co/ai4bharat/indic-conformer-600m-multilingual) · [Sarvam STT](https://www.sarvam.ai/speech-to-text) · [Sarvam pricing](https://docs.sarvam.ai/api-reference-docs/getting-started/pricing) · [AWS Transcribe languages](https://docs.aws.amazon.com/transcribe/latest/dg/supported-languages.html) · [Google Chirp 3](https://docs.cloud.google.com/speech-to-text/docs/models/chirp-3) · [Google languages by region](https://docs.cloud.google.com/speech-to-text/docs/speech-to-text-supported-languages) · [Google locations](https://docs.cloud.google.com/speech-to-text/docs/locations) · [Google STT pricing](https://cloud.google.com/speech-to-text/pricing) · [Google TTS pricing](https://cloud.google.com/text-to-speech/pricing) · [Azure Speech regions](https://learn.microsoft.com/en-us/azure/ai-services/speech-service/regions) · [Bhashini (MoE/PIB)](https://education.gov.in/sites/upload_files/mhrd/files/PIB2039811.pdf) · [ESP-SR command words](https://docs.espressif.com/projects/esp-sr/en/latest/esp32s3/speech_command_recognition/README.html)

**Hardware, manufacturing, certification:** [HA Voice PE](https://cnx-software.com/2024/12/20/59-voice-preview-edition-adds-an-offline-smart-speaker-to-your-home-assistant-server) · [ESP32-S3-BOX-3 (CNX)](https://www.cnx-software.com/2023/09/01/esp32-s3-box-3-devkit-comes-with-2-4-inch-display-dual-microphone-pcie-expansion-connector/) · [ESP32-S3-BOX-3 (Pi Hut)](https://thepihut.com/collections/adafruit/products/espressif-esp32-s3-box-3) · [ReSpeaker XVF3800](https://www.cnx-software.com/2025/07/29/respeaker-xmos-xvf3800-4-mic-array-board-features-esp32-s3-module-works-over-usb/) · [RK3308 (search only)](https://www.cnx-software.com/2018/03/09/rockchip-unveils-rk3308-gemini-cortex-a35-processors-for-smart-speakers/) · [Raspberry Pi CM5](https://www.raspberrypi.com/news/compute-module-5-on-sale-now/) · [CM4/CM5 price rise](https://www.cnx-software.com/2025/10/02/ddr4-supply-demand-stress-leads-to-raspberry-pi-cm4-cm5-price-increases/) · [boAt–Dixon JV](https://www.boat-lifestyle.com/en-ae/blogs/news/boat-and-dixon-technologies-form-joint-venture-to-design-and-manufacture-wireless-audio-solutions-in-india) · [PLI 2.0](https://vajiramandravi.com/current-affairs/?p=35470) · [ECMS (PIB)](https://static.pib.gov.in/WriteReadData/specificdocs/documents/2025/oct/doc20251027678301.pdf) · [Enviliance, BIS CRS IS 616](https://enviliance.com/regions/south-asia/in/report_1515) · [Agile Regulatory, BIS CRS](https://www.agileregulatory.com/blogs/bis-crs-registration-of-smart-speakers-with-or-without-display-is-616-2017) · [Espressif WPC ETA certificate](https://documentation.espressif.com/ETA-SD-20200704087%20ESP32-PICO-V3-ZERO%20WPC.pdf) · [GRL, WPC](https://www.graniteriverlabs.com/en-us/market-access-services/country/india/wpc-mark) · [Freyr, WPC ETA](https://www.freyrsolutions.com/blog/wpc-compliance-for-wireless-devices-in-india-understanding-eta-licensing-and-market-entry-requirements.md) · [DoT ETA](https://eservices.dot.gov.in/equipment-type-approval-eta) · [Khaitan, MTCTE](https://khaitanco.com/thought-leaderships/Mandatory-testing-and-certification-of-telecom-equipment-Regulatory-overlaps-pave-way-for-exemptions)

**Privacy / DPDP:** [AZB & Partners, DPDP phases](https://www.azbpartners.com/bank/update-indias-digital-personal-data-protection-framework-comes-into-effect/) · [SCC Online, Rules notified](https://www.scconline.com/blog/post/2025/11/14/meity-notified-digital-personal-data-protection-rules-2025/) · [Biometric Update](https://www.biometricupdate.com/202511/india-notifies-its-sweeping-digital-personal-data-protection-rules) · [Fourth Schedule](https://www.dpdpa.com/schedule/schedule4.html) · [MediaNama](https://www.medianama.com/2025/11/223-dpdp-rules-tracking-children-parental-consent/) · [MeitY DPDP Rules](https://www.meity.gov.in/documents/act-and-policies/digital-personal-data-protection-rules-2025-gDOxUjMtQWa?pageTitle=Digital-Personal-Data-Protection-Rules-2025%3B) · [DPDP Act 2023](https://www.meity.gov.in/static/uploads/2024/02/Digital-Personal-Data-Protection-Act-2023.pdf)
