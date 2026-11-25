# Privacy — brief for legal review (DPDP Act 2023 and DPDP Rules 2025)

Status: **open questions for counsel; nothing here is legal advice.** Prepared from public sources (cited at the
end) to settle the "to verify" items of [`PRIVACY-AND-CONSENT-CENTRE.md`](PRIVACY-AND-CONSENT-CENTRE.md) §5.1
and §8 and of the processing register the privacy baseline seeds (ROADMAP SEC-11). Each item says what MySMS does
today, what the sources say, and the exact question. Where a source is a commentary rather than the bare text, it is
marked as such; counsel should read the Gazette text.

## 1. Timeline that frames every answer

- The Rules were notified on 13–14 November 2025. Rules 1, 2 and 17–21 apply from publication; Rule 4 (Consent
  Managers) twelve months later (November 2026); Rules 3, 5–16, 22 and 23 — notice, security, breach reporting,
  retention, **children's verifiable consent (Rule 10), the Fourth-Schedule exemptions (Rule 12), cross-border
  transfer (Rule 15)** — eighteen months later, i.e. from **13 May 2027** (commentaries; date counting to confirm).
- Section 7 (legitimate uses) applies from 13 May 2027 as well (commentary).

**Q1.** Confirm the commencement dates MySMS should design to (13 May 2027 for Rules 3, 5–16, 22, 23 and for
sections 5–10 of the Act?).

## 2. The legal-basis values MySMS records

The register's `legal_basis` takes: `consent`, `legitimate_use`, `legitimate_use_voluntary`, `legal_obligation`,
`education_exemption`, `medical_emergency`, `employment`, `contract`.

- The Act has two grounds only: consent (s.6) and "certain legitimate uses" (s.7) (s.4). Section 7 is a closed list:
  (a) personal data the principal provided voluntarily for a specified purpose; (b) State subsidies, benefits,
  services, certificates, licences, permits; (c) State functions; (d) a legal obligation to disclose information to
  the State; (e) a judgment, decree or order; (f) a medical emergency; (g) an epidemic or public-health threat;
  (h) disaster or breakdown of public order; (i) employment.
- So `contract` has no counterpart in the Act; `legal_obligation` corresponds only to s.7(d) (disclosure **to the
  State**) and s.7(e); `education_exemption` is not a ground at all but an exemption from s.9(1) and s.9(3) for
  certain processing (Rule 12, Fourth Schedule); a general `legitimate_use` "for safety or operations" has no s.7
  anchor.

**Q2.** Map each value to an Act ground, or tell us which to drop: (a) is `contract` (a parent paying fees through a
payment gateway) consent, or s.7(a) voluntary provision? (b) is `legal_obligation` right for health notes a school
keeps (it is not a disclosure to the State)? (c) should `legitimate_use` be replaced by the specific s.7 clause per
activity (7(a), 7(f), 7(h)…)? (d) should `education_exemption` become a separate attribute ("exempt from s.9(1)/9(3)
under Fourth Schedule Part A item 3") alongside a real ground?

## 3. Activities marked "to verify"

| Activity | MySMS today | What the sources say | Question |
|---|---|---|---|
| `lms.learning_analytics` (engagement scores, recommendations) | consent; minors' basis "education exemption, to verify" | Fourth Schedule Part A item 3: an educational institution is exempt from s.9(1) and 9(3) where "processing is restricted to tracking and behavioural monitoring (a) for the educational activities of such institution; or (b) in the interests of safety of children enrolled". | **Q3.** For students under 18, is learning analytics run by the school "for its educational activities" (so no parental consent and no s.9(3) bar), or does a recommendation engine go beyond that? Is MySMS (a processor for the school) covered when it computes the analytics? |
| `transport.live_location_share` (live bus location to parents) | legitimate_use (safety) | Part A item 5: a Data Fiduciary "engaged by an educational institution… for transport of children" — "restricted to tracking the location of such children, in the interests of their safety, during the course of their travel". Part B item 4: "determination of real-time location of a child… in the interest of her safety and protection or security". | **Q4.** These are exemptions from verifiable parental consent and the tracking bar — which **ground** (s.6 consent or a s.7 use) then applies to the processing itself? Does the exemption cover the school when its own bus is tracked (item 5 names a transporter engaged by the school)? |
| `sis.health_record` (medical conditions, allergies, medication) | legal_obligation | Part A item 1 covers clinical and mental-health establishments and healthcare professionals, not schools. s.7(f) covers a medical emergency only. | **Q5.** What is the ground for a school keeping health notes of a student — verifiable parental consent, or a statutory duty (which law)? Does the school nurse change that? |
| `comms.operational_notice` (timetable changes, closures, fee reminders) | legitimate_use | no s.7 clause for "operations". | **Q6.** Is this s.7(a) (contact details given voluntarily at admission for school communication), or consent? |
| `frontoffice.visitor_log` (visitor photo and ID) | legitimate_use (safety) | no s.7 clause for premises safety; s.7(a) may apply when the visitor hands over the data for entry. | **Q7.** Is s.7(a) the right ground, and does a visitor's ID copy need a separate notice? |
| `admission.application` | legitimate_use_voluntary | s.7(a). Applicants are mostly children: s.9(1) verifiable parental consent applies unless an exemption covers it. | **Q8.** Does s.7(a) displace s.9(1) for an application a parent submits for a child, or is the parent's submission itself the verifiable consent? |
| `finance.payment_gateway` | contract | see Q2(a). | (Q2) |

## 4. Recipients and cross-border transfer

- MySMS records no recipients yet (`processing_activity.recipients` is empty) and `cross_border` is false everywhere.
- `lms.ai_assist_external` sends student work to an external AI model provider; the provider and its hosting country
  are not yet chosen. Rule 15 permits transfer abroad subject to restrictions the Central Government may notify
  (commentary).

**Q9.** For an external AI provider acting as MySMS's processor: what must the notice (s.5) name — the provider, the
country, both? Is a provider hosted outside India acceptable today, and what contract terms does counsel require?
**Q10.** Must payment gateways, SMS/WhatsApp providers and the e-mail provider be named per activity in the notice, or
by category?

- `lms` already sends students' submitted work to an external plagiarism-check provider
  (`lms.SubmitForPlagiarismCheck`, built; the provider is configured per school). privacy's processing register has no
  activity for it, so no legal basis is recorded and no consent is asked.

**Q14.** On what ground may a school send a student's submission (often a minor's) to a plagiarism-check provider
acting as its processor — consent with verifiable parental consent under s.9(1), or another ground — and what must
the notice name (the provider, its country, how long it keeps the work)?

## 5. Retention

- No activity has a retention policy yet (`retention_policy_code` empty); the retention engine is a later slice.

**Q11.** Which retention periods are legal floors for a school in India (admission records, marks, attendance, fee
records, transfer certificates), and when must data be erased once the purpose is served (Rule 8 and its Third
Schedule, per commentary)?

## 6. Decisions MySMS already took that counsel should confirm

- Age of majority 18 (`mysms.privacy.age-of-majority`); a person of unknown age is treated as a minor.
- On the 18th birthday guardians' decisions stop counting; the student decides from then on (design §4.3,
  the majority job, hourly by default).
- Verification of a parent: an existing school record of the person and an active legal-guardian link ("verified,
  existing_record"); a paper form needs a second staff member.
- Forbidden for minors, whatever a guardian says: behavioural analytics, targeted advertising, biometric or video
  monitoring, product analytics.

**Q12.** Is "existing school record + legal-guardian link" sufficient as Rule 10 verification of the parent's identity
and age (Rule 10's text not yet read for this brief)? **Q13.** Is the list of purposes
forbidden for minors right, given the Part A item 3 exemption for educational tracking?

- Access summary (s.11, design §9.6): generated from the processing register — the tenant's register activities that
  may concern the person (for a minor or a person of unknown age, only activities that process minors' data). It does
  not yet tell whether the person is a student, a guardian or staff, so it may list an activity that does not reach
  them.

**Q15.** Does a summary of the activities that *may* concern the principal satisfy s.11(1)(a), or must it list only
the processing actually done with their data (filtered by their role in the school) before the access right commences?

## Sources

- The Digital Personal Data Protection Act, 2023 (Act 22 of 2023), India Code:
  https://www.indiacode.nic.in/bitstream/123456789/22037/2/a2023-22.pdf
- PIB, "DPDP Rules, 2025 Notified" (17 Nov 2025):
  https://static.pib.gov.in/WriteReadData/specificdocs/documents/2025/nov/doc20251117695301.pdf
- Fourth Schedule text (secondary reproduction): https://privacylawhub.com/bare-acts/dpdp-rules-2025/schedule-iv-fourth-schedule-exemptions-from-section-9-1-and-9-3-
- Section 7 summary (commentary): https://www.taxmann.com/post/blog/legitimate-uses-section-7-dpdp-act
- Rules commencement (commentary): https://www.scconline.com/blog/?p=371058 ;
  https://www.proems.in/guides/dpdp-act-schools/
