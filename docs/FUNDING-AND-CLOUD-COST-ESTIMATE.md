# MySMS: Google Cloud Cost and India Funding Estimate

**Prepared:** 10 November 2026  
**Purpose:** Planning estimate for a 50,000-student, 1,000-teacher launch, with 1 TiB of user-file storage and an optional classroom voice-device programme.  
**Status:** Investor-planning model, not a Google quote, vendor quotation, audited forecast, or guarantee of performance.

## Executive estimate

| Budget | Planning range | Working midpoint |
|---|---:|---:|
| Google Cloud, software platform only, monthly at target scale, before tax | **₹2.1–6.5 lakh/month** | **₹4.3 lakh/month** |
| Google Cloud, software platform only, annual run-rate, before tax | **₹25.7–77.2 lakh/year** | **₹51.5 lakh/year** |
| Cloud with 1,000 voice devices at 5–20 short commands per device per school day | **Add ₹0.9–3.6 lakh/month** | Depends on usage and speech-region choice |
| 18-month company runway, software-first and a 100-device field pilot | **₹4.6–8.7 crore** | Raise in milestone-based tranches |
| 18-month runway, with all 1,000 classroom devices provided at launch | **₹6.0–11.5 crore** | Includes an optional cellular-connectivity reserve |

For an initial fundraise, a reasonable planning target is **₹6–9 crore for 18 months**, with a 100-device pilot and evidence gates before mass procurement. If the business commits to buying and installing 1,000 devices before product-market fit, plan for **₹8–12 crore**. The higher figure includes working capital and uncertainty around device sourcing, support, connectivity, and sales timing.

The cloud bill is not the main startup cost. A small product and implementation team, school onboarding, support, security work, and any free hardware will cost more than the core cloud services. SMS, payment-gateway fees, third-party email, and telecom plans are outside the Google Cloud estimate and should be budgeted separately.

## 1. What this estimate assumes

These are explicit modelling inputs. Change them when school pilots provide real usage data.

| Input | Planning assumption |
|---|---|
| Student population | 50,000 enrolled student accounts |
| Teachers | 1,000; assumed to correspond to roughly 1,000 classrooms for the device scenario |
| School footprint | 100 schools averaging 500 students and 10 teachers each; only a convenient sizing assumption |
| Activity | 10,000–15,000 daily active students on school days, plus teachers, guardians, and administrators |
| Peak load | 1,500–3,000 concurrent users in the base design envelope; a timetable/attendance bell-time spike must be measured |
| API volume | Approximately 20 million requests/month in the working case; range depends on client polling, photos, learning content, and notification patterns |
| Files | 1 TiB of Cloud Storage Standard data in one Mumbai region; this is file/object storage, not database capacity |
| Database | PostgreSQL on Cloud SQL, initially sized for a single logical database; budget includes a high-availability primary, 500 GiB SSD planning capacity, backups, and a read replica envelope |
| Availability | Mumbai (`asia-south1`) primary deployment, regional Cloud SQL HA, plus a limited cross-region recovery allowance; no multi-region active-active system |
| Runtime | Cloud Run for a containerized modular monolith and background jobs; not 21 separately operated production microservices |
| Event bus | Managed Pub/Sub for this estimate. The repository currently defaults to RabbitMQ and has a Pub/Sub adapter, so choosing Pub/Sub is a deployment decision, not an existing production fact. |
| Billing month | 730 hours; 1 TiB = 1,024 GiB; USD is converted at a rounded planning rate of **₹95 per USD** |

The RBI page showed ₹93.8579 per USD as of 9 November 2026. ₹95 is used here as a rounded planning conversion, not a forecast or locked exchange rate. [RBI current rates](https://www.rbi.org.in/home.aspx/webservice/home.aspx/scripts/Scripts/scripts/FS_PressRelease.aspx?fn=2759)

### Important limitations

- The repository describes 21 Java/Spring modules and PostgreSQL. It also describes a monolith option and a 21-service deployment option. This model prices the modular-monolith route first because it reduces operational overhead while keeping the module boundaries. The existing maintainability review specifically flags that a pool size of 10 for each of 21 modules can expose up to 210 database connections in the monolith. Set measured, low per-module pools and database connection limits before a production load; the estimate does not assume the current defaults are safe.
- The project status reports 1,709 of 2,049 API operations served, with the remaining 340 returning `501 NOT_IMPLEMENTED`. Attendance has 33 operations listed as served, but the broader platform is not feature-complete. Hosting prices do not fund implementation of unfinished product workflows, integrations, support processes, or production security work. See [STATUS.md](../STATUS.md), [api/operations-status.md](../api/operations-status.md), and [docs/MAINTAINABILITY-REVIEW.md](MAINTAINABILITY-REVIEW.md).
- There is no production traffic history, database benchmark, measured file-download volume, recovery objective, or provider quote in the repository. The ranges below are budget envelopes. They must be replaced by a live Google Cloud Pricing Calculator estimate and a controlled load/cost pilot before an investor or procurement commitment.

## 2. Recommended first production shape

1. **Cloud Run:** one or a small number of container services for the modular monolith, plus separately scaled workers/jobs where useful. Set minimum instances only where startup latency needs it. Configure autoscaling ceilings so a traffic spike cannot create an uncontrolled bill.
2. **Cloud SQL for PostgreSQL:** regional high-availability instance in Mumbai, private connectivity, automated backups and point-in-time recovery. Begin with one production database. Add a read replica only after query/CPU observations show a need; add a Delhi recovery replica only when the recovery objective justifies its cost.
3. **Cloud Storage:** Mumbai regional bucket for uploaded documents and learning files, with explicit lifecycle, retention, versioning, and deletion policies. Do not place student files in a public bucket.
4. **Pub/Sub or RabbitMQ:** use Pub/Sub if managed operations and Google Cloud IAM are preferred. If retaining RabbitMQ, price broker compute, disk, HA, patching, and on-call work separately. Do not count a free local/in-process test bus as production infrastructure.
5. **Static clients and edge:** host the web clients behind a managed HTTPS load balancer/CDN as needed. Mobile app distribution, push notifications, domain registration, and customer support are separate operational items.
6. **Security and observability:** Secret Manager/KMS, least-privilege service identities, audit logs, alerts, backup/restore drills, and budget alerts. Keep log volume and retention deliberate; verbose request/body logs can expose student data and create avoidable spend.

Cloud SQL supports PostgreSQL in Mumbai. The cost model uses a Mumbai primary, but any second-region recovery design and voice/AI region must be separately assessed. [Cloud SQL region availability](https://docs.cloud.google.com/sql/docs/postgres/region-availability-overview)

## 3. Monthly Google Cloud budget at target scale

All amounts below are **USD list-price planning envelopes converted at ₹95/USD**, before taxes, credits, negotiated discounts, and invoice-specific currency adjustments. The line items intentionally use ranges because the right CPU, memory, connection pools, replication, and retention values are not known yet.

| Cloud area | Sizing included in the envelope | USD/month | INR/month at ₹95/USD |
|---|---|---:|---:|
| Cloud Run application and jobs | Java modular monolith; a small warm baseline plus active request/job time and autoscaling headroom | $350–1,000 | ₹33,000–95,000 |
| Cloud SQL PostgreSQL primary | 8–16 vCPU / 32–64 GiB HA planning envelope, 500 GiB SSD, database backups; exact Mumbai SKU and edition to be set in calculator | $1,000–2,500 | ₹95,000–2,37,500 |
| Cloud Storage objects | 1 TiB regional Standard plus a limited version/backup allowance and normal object operations | $50–140 | ₹4,750–13,300 |
| Networking, HTTPS load balancing, CDN/WAF and internet egress | 1–5 TiB monthly internet delivery, one main public entry point, moderate request volume; egress is the largest traffic-sensitive item | $200–900 | ₹19,000–85,500 |
| Pub/Sub | Events published and consumed by the MySMS modules, modest retention, same-region consumers | $10–100 | ₹950–9,500 |
| Logging and monitoring | Controlled application log volume, metrics, alerts, dashboards, and normal retention | $50–250 | ₹4,750–23,750 |
| Secrets, keys, DNS, registry, builds, and small managed services | Basic production service dependencies; excludes engineering labour | $50–200 | ₹4,750–19,000 |
| Recovery and extra replica allowance | Additional database replica/backup capacity and limited second-region recovery resources | $250–800 | ₹23,750–76,000 |
| **Subtotal** |  | **$1,960–5,890** | **₹1,86,200–5,59,550** |
| 15% operating contingency | Headroom for early sizing mistakes and normal usage fluctuation; does not cover a major traffic or product-scope change |  | **₹27,930–83,933** |
| **Budget envelope** | Rounded after contingency |  | **₹2.1–6.5 lakh/month** |

The midpoint is approximately **₹4.3 lakh/month** before tax. It is not a prediction of the invoice; it is the midpoint of this explicit planning envelope.

### Annual run-rate

| Case | Monthly before tax | 12-month steady-state run-rate before tax | With an illustrative 18% tax cash-flow reserve* |
|---|---:|---:|---:|
| Low | ₹2.1 lakh | ₹25.7 lakh | ₹30.3 lakh/year |
| Planning midpoint | ₹4.3 lakh | ₹51.5 lakh | ₹60.8 lakh/year |
| High | ₹6.5 lakh | ₹77.2 lakh | ₹91.1 lakh/year |

\*The 18% column is a cash-planning stress case only, **not a tax conclusion**. Have an Indian CA confirm the actual Google invoice treatment, GST registration/return requirements, reverse-charge treatment if applicable, and available input tax credit. Do not treat recoverable GST as permanent cost, but do plan for payment timing.

This annual table is a **steady-state run-rate multiplied by 12**. If student onboarding ramps gradually, the first-year cloud bill may be lower. If the product launches with all 50,000 accounts active from month one, the full-year figure is more appropriate. Conversely, video uploads, heavy lesson streaming, high log retention, a larger database, or a second full production region can exceed the high case.

## 4. Where the cloud numbers come from

Google's prices are regional and can change. These are the main unit-price anchors used in the model:

- Cloud Run charges for allocated CPU, memory, and requests under request-based billing; its published example for 10 million requests with 400 ms average latency and 20-way concurrency is $13.69 in Belgium. Mumbai is a Tier 1 Cloud Run region, but the Java platform's instance baseline, concurrency, min instances, memory, and database access will dominate the real service configuration. [Cloud Run pricing](https://cloud.google.com/run/pricing)
- Cloud SQL is billed for CPU, memory, storage, backups, and networking. HA compute is priced separately from single-zone compute, and prices depend on selected edition, machine series, and region. The planning envelope is deliberately sized as an HA database rather than an inexpensive development instance. Use the live selector for **Mumbai**, then input the chosen CPU/RAM/SSD/backups and any replica. [Cloud SQL pricing](https://cloud.google.com/sql/pricing)
- Cloud Storage Standard regional storage is billed per GiB-hour; the published page lists a Standard rate of about $0.000030137 per GiB-hour for its currently selected regional example, or about $22 per month for 1 TiB at 730 hours. A second copy, object versions, soft delete, and operations increase the total. The model reserves $50–140 rather than claiming 1 TiB is the whole storage bill. [Cloud Storage pricing](https://cloud.google.com/storage/pricing)
- Premium-tier outbound internet transfer is charged by volume. Google's published Asia schedule is $0.12/GiB for the first TiB and $0.11/GiB for the next 1–10 TiB. On that schedule, 1 TiB is about $123 and 5 TiB about $573 before any CDN-specific charges or other network components. Cloud Run notes that external load-balancer requests use internet egress billing. [VPC network pricing](https://cloud.google.com/vpc/network-pricing), [egress price schedule](https://cloud.google.com/vpc/pricing-announce)
- Pub/Sub provides the first 10 GiB/month of basic message throughput free and then charges $40/TiB for throughput in all regions; message retention can add storage cost. [Pub/Sub pricing](https://cloud.google.com/pubsub/pricing)
- Cloud Logging includes the first 50 GiB/project/month and then charges $0.50/GiB for log ingestion, with extra retention charges beyond the default period. [Cloud Logging pricing](https://cloud.google.com/logging)

The table should therefore be read as a workload budget, not as a vendor-quoted bill. In particular, the Cloud SQL and Cloud Run lines must be validated against the actual selected Mumbai SKUs, billing mode, instance floor, and measured concurrency.

## 5. Optional voice-device recurring cloud cost

The voice estimate below assumes a **locally detected wake word or push-to-talk**, followed by a bounded speech clip. It assumes one device in each of 1,000 classrooms, 22 school days/month, 15 seconds of audio per command, a 150-character spoken response, and simple text-only intent handling. Speech processing is priced at the published Speech-to-Text rate for the no-data-logging Standard option. The actual supported model/region must be confirmed before selecting a provider.

| Voice usage | Formula | Speech minutes/month | Approximate AI cloud/month |
|---|---|---:|---:|
| Light classroom use | 1,000 devices × 5 commands/day × 22 days × 15 sec | 27,500 | **About ₹0.9 lakh** |
| Regular classroom use | 1,000 devices × 20 commands/day × 22 days × 15 sec | 110,000 | **About ₹3.6 lakh** |

Approximate breakdown:

- Speech-to-Text without data logging: $0.024/minute after the small monthly free allotment; about $660/month at 5 commands/day/device and $2,640/month at 20 commands/day/device.
- Neural2 Text-to-Speech: $16 per million characters after the first million free characters; at 150 response characters/command, about $248/month and $1,040/month respectively.
- Gemini 2.5 Flash-Lite text: modelled at 2,000 input and 200 output tokens per command using published rates; approximately $31/month and $123/month. Use deterministic API commands for attendance and roster reads; do not send a full student roster to a general-purpose model for action selection.

These are API-price calculations, not a promise that a chosen model is regionally available, suitable for a noisy classroom, or eligible for the required data-processing terms. If the assistant streams or transcribes much longer utterances, speech costs rise roughly with audio minutes. Local wake-word detection is essential to avoid paying to transmit room audio while nobody is addressing the device. [Speech-to-Text pricing](https://cloud.google.com/speech-to-text/pricing), [Text-to-Speech pricing](https://cloud.google.com/text-to-speech/pricing), [Gemini model pricing](https://cloud.google.com/gemini-enterprise-agent-platform/generative-ai/pricing)

### A specific India-region issue to resolve

Google's current Speech-to-Text V2 supported-language table lists `en-IN` and `hi-IN` Chirp 2 support in `asia-southeast1` (Singapore), while the `asia-south1` (Mumbai) entries shown in that table are more limited. The location/model matrix changes over time. Before handling children's classroom audio, confirm the current exact model, language, endpoint, data-processing location, and contract terms. If the needed accuracy/language set is only available through a non-India endpoint, choose knowingly between a legal-reviewed regional endpoint, an on-device/self-hosted model in India, or a narrower English-only pilot. The cloud estimate does **not** include a GPU cluster for self-hosted speech recognition. [Speech-to-Text supported languages by region](https://docs.cloud.google.com/speech-to-text/docs/speech-to-text-supported-languages), [regional availability](https://docs.cloud.google.com/speech-to-text/docs/locations)

## 6. Non-Google costs that can change the monthly bill

| Item | Budget treatment |
|---|---|
| SMS and OTP | Not part of Google Cloud. Use a vendor quote and include India Principal Entity, header, content-template and consent setup where applicable. As a planning sensitivity only, 200,000 SMS/month at an assumed ₹0.15–0.30 per message would be ₹30,000–60,000/month before any vendor/platform charges; replace the assumed unit price with a DLT-compliant provider quote. [TRAI advice to senders](https://www.trai.gov.in/advice-to-senders) |
| Email | Not included; select a transactional provider, forecast messages and attachments, and configure bounce/complaint management. |
| Payment gateway | Not included. Model the vendor's actual percentage/fixed fee against collected payment volume and decide whether the school or MySMS bears it. Fee collections are not MySMS revenue. |
| Cellular data for devices | Base assumes schools provide reliable Wi-Fi. If each of 1,000 devices needs its own managed SIM, reserve **₹1.5–4 lakh/month** as a placeholder at ₹150–400/device/month; obtain M2M/IoT plan quotes and include coverage and support. |
| Domain, support desk, CRM, product analytics, app-store accounts | Keep as separate SaaS/vendor lines; small individually, material when multiplied across staff and schools. |
| Device replacement and logistics | No recurring hardware repair cost is in the cloud table. Hold spare stock, a warranty/replacement reserve, shipping, and field-technician capacity. |
| Insurance and working capital | Not included as a fixed quote. Consider cyber, product/public-liability, directors' and officers' cover, and delayed school payment cycles with a broker/CA. |

## 7. Free classroom-device economics

The following is a **bottom-up planning allowance**, not a supplier quote. It assumes a purpose-built screenless unit with a compute module, secure device identity, Wi-Fi, microphone array, speaker, mute control, status light, power supply, enclosure, assembly, and quality checks.

| Deployment | Device and installation planning cost | Funding implication |
|---|---:|---|
| 100-device field pilot | ₹10,000–18,000 installed/device | ₹10–18 lakh; allow more for prototype engineering and spares |
| 1,000-device rollout | ₹10,000–18,000 installed/device | ₹1.0–1.8 crore upfront; add prototype/tooling, compliance, and a separate repair reserve |
| Custom hardware design, prototypes, firmware, test fixtures, production validation | ₹35–75 lakh initial allowance | One-time engineering envelope; obtain an electronics design/manufacturing partner estimate |
| Cellular fallback, if needed | ₹150–400/device/month placeholder | ₹1.5–4 lakh/month for 1,000 devices |

Before importing or selling a wireless product, confirm whether the finished device/model and its RF modules need Indian approvals. The Department of Telecommunications describes an ETA process for applicable devices operating in de-licensed bands and lists a ₹10,000 fee per product/model in its current service information; laboratory reports and manufacturer documents are separate. BIS applicability depends on the product category and components, so do not assume that an approval for a radio module covers the finished product. [DoT Equipment Type Approval](https://eservices.dot.gov.in/equipment-type-approval-eta)

Free at the classroom does not mean free to the startup. Treat the device as customer-acquisition cost or a three-year service asset. Require an active software contract, named school owner, return/replacement terms, and a measurable adoption commitment. Consider hardware sponsorship or a school-group contract rather than shipping a device to every room before confirming retention and usage.

## 8. India company setup and compliance budget

This is a workstream checklist, not legal or tax advice.

- **Entity:** If equity fundraising is intended, ask a startup CA/CS and company lawyer to compare a private limited company with an LLP for the cap table, ESOPs, founder vesting, investor instruments, and compliance load. A private limited company is the usual planning assumption for the funding model, not a legal requirement stated here.
- **Incorporation:** MCA's SPICe+ FAQ says companies with authorised capital up to ₹15 lakh qualify for zero MCA filing fee, while state stamp duty still applies; PAN/TAN charges and professional assistance are separate. Reserve **₹30,000–₹1 lakh** for incorporation support and documentation until a local CA/CS quotes for the chosen state and capital structure. [MCA SPICe+ FAQ](https://www.mca.gov.in/Ministry/pdf/SPICe%2Band_linked_filings_FAQs_V3_13%20Jan_2022_updated.pdf)
- **DPIIT Startup recognition:** The Ministry says it does not charge for recognition. Recognition is eligibility-based and is not a grant or automatic tax exemption. [Startup India recognition](https://www.startupindia.gov.in/content/sih/en/startupgov/startup_recognition_page.html)
- **Seed funding:** The Startup India Seed Fund Scheme lists grants up to ₹20 lakh for proof of concept/prototype/product trials and debt/convertible support up to ₹50 lakh for eligible market entry/scaling, disbursed through selected incubators. Eligibility, incorporation age, current calls, incubator selection, and permitted use must be checked; do not count it as committed money in the runway. [SISFS portal](https://seedfund.startupindia.gov.in/), [scheme overview](https://www.startupindia.gov.in/content/dam/invest-india/Templates/public/Startup_India-Way_Ahead.pdf)
- **Annual finance/compliance:** Include company secretary/ROC filings, statutory audit, bookkeeping, payroll/TDS, GST returns if registered, contracts, and fundraising diligence. Reserve **₹2–6 lakh/year** for ordinary accounting/ROC/audit support and **₹10–30 lakh** for first-year privacy, security, employment, school-contract, fundraising, and product-counsel work. These are planning ranges, not regulated fees or professional quotes.
- **Student data:** The DPDP Act and 2025 Rules affect how a school platform handles children's personal data. The Rules have staged commencement dates; map the effective requirements at launch with counsel. Design for purpose limitation, minimum collection, access control, deletion/retention, incident response, school/data-processor terms, and parental/student notices as legally appropriate. This estimate includes a legal/security allowance but not a formal audit or certification programme. [MeitY DPDP Rules page](https://www.meity.gov.in/documents/act-and-policies/digital-personal-data-protection-rules-2025-gDOxUjMtQWa?pageTitle=Digital-Personal-Data-Protection-Rules-2025%3B), [DPDP Act text](https://www.meity.gov.in/static/uploads/2024/02/Digital-Personal-Data-Protection-Act-2023.pdf)
- **Bulk messages:** TRAI requires senders of bulk communications to register as a Principal Entity and follow applicable header, content-template, and consent requirements. [TRAI sender requirements](https://www.trai.gov.in/advice-to-senders)
- **Hardware compliance:** Budget for RF testing, product safety/EMC assessment, manufacturing quality systems, packaging, and warranty logistics after the exact design and sourcing country are selected. Fees and applicability differ by device design, and import duties cannot be estimated without tariff classification.

The MCA's zero-filing-fee concession does not make incorporation free overall. State stamp duty, PAN/TAN, legal/accounting help, and ongoing filings remain.

## 9. Eighteen-month company cash plan

This runway is sized for a small team trying to complete the product, sign schools, support early deployments, and reach roughly 50,000 enrolled student accounts. Fully loaded monthly payroll and operational inputs below are assumptions to be replaced with actual offers and quotes.

| Workstream | Planning assumption | 18-month cash range |
|---|---|---:|
| People | 10–12 FTE, founders included, average fully loaded cost ₹1.2–1.8 lakh/FTE/month | ₹2.16–3.24 crore |
| Cloud and AI ramp | Cloud bill grows with active schools; includes initial speech/LLM testing, excludes non-cloud SMS/email and device SIMs | ₹0.54–1.62 crore |
| Sales, onboarding, travel, training, customer support | School demonstrations, data migration, teacher training, implementation travel, and support coverage | ₹0.54–1.08 crore |
| Product and hardware R&D | Device industrial/electrical design, prototypes, firmware/device-management work, test fixtures, language/UX trials | ₹0.35–0.75 crore |
| Legal, finance, privacy, security, and compliance | Incorporation plus contracts, DPDP readiness, security review, fundraising diligence, device-certification assessment | ₹0.15–0.35 crore |
| 100-device pilot procurement and installation | Installed unit allowance, limited spares, shipping and deployment | ₹0.12–0.20 crore |
| **Subtotal** |  | **₹3.86–7.24 crore** |
| 20% reserve | Sales-cycle delays, FX, cloud variance, device defects, GST timing, and scope risk | **₹0.77–1.45 crore** |
| **18-month software-first raise need** |  | **₹4.6–8.7 crore** |

For 1,000 devices immediately, replace the pilot's ₹0.12–0.20 crore with ₹1.0–1.8 crore installed device spend. That produces roughly **₹5.7–10.6 crore** before optional cellular data; a cellular reserve of ₹0.27–0.72 crore over 18 months takes it to about **₹6.0–11.3 crore**. Round the fundraise planning range to **₹6–11.5 crore**, or **₹8–12 crore** if a larger working-capital and product-support cushion is required.

### Sensible financing sequence

1. **Validation tranche:** fund the core teacher/student workflows and 5–10 paid or co-funded school pilots. Use tablets/phones or a small number of off-the-shelf units to test the voice interaction. Do not manufacture 1,000 custom devices before reliability, language, privacy, and willingness-to-pay are known.
2. **Pre-seed/seed round:** target 12–18 months of runway against measurable milestones: active schools, weekly teacher use, attendance completion time, renewal intent, support cost per school, and platform availability.
3. **Device expansion:** fund 100 devices only after an operational pilot; expand toward 1,000 only after device uptime, replacement rate, per-class usage, and software gross contribution are measured. Seek sponsor/CSR or school-group underwriting for the free-device portion where appropriate.
4. **Non-dilutive options:** investigate eligible incubator programmes and grants, including SISFS, but treat them as upside only. Startup India lists equity, debt, grants, and incubators as different financing paths; equity/debt/grants have different control, repayment, and permitted-use consequences. [Startup India funding overview](https://www.startupindia.gov.in/content/sih/en/looking_for_funding.html)

## 10. Subscription sensitivity and device viability

The prices below are **hypotheses to test**, not observed market rates or recommendations. They assume 50,000 paid students and 100 schools.

| Hypothetical annual platform price | Annual recurring revenue at 50,000 paid students | Approx. annual contract per 500-student school |
|---:|---:|---:|
| ₹300/student/year | ₹1.5 crore | ₹1.5 lakh |
| ₹600/student/year | ₹3.0 crore | ₹3.0 lakh |
| ₹900/student/year | ₹4.5 crore | ₹4.5 lakh |

At 1,000 installed devices for 50,000 students, the device count is one per 50 students. A ₹10,000–18,000 installed device amortized over three years is about **₹67–120 per student/year** before repairs. The software-only cloud estimate is approximately **₹51–154 per student/year** at target scale, before taxes. The voice budget adds approximately **₹22–86 per student/year** at 5–20 short commands/day/device. Together, cloud, AI, and three-year device amortization represent roughly **₹140–360/student/year** before school onboarding, support, payment fees, sales, tax, and central team costs.

This sensitivity shows why a ₹300 price could be tight if the startup absorbs free devices and high voice usage. The business needs measured willingness-to-pay and a policy such as a minimum annual contract, paid device deposit, lease, sponsor, or capped AI usage. A 20% discount, a 60% paid-account rate, or device replacement within three years materially weakens the economics.

## 11. Cost controls and decision gates

- Create separate GCP billing budgets/alerts for production, staging, and development. A budget alert does not itself cap spending; add quotas and maximum instance counts.
- Before signing a full-scale cloud contract, build the live calculator case with the selected Mumbai Cloud Run/Cloud SQL SKUs, HA, SSD, backups, Pub/Sub, CDN, load balancer, and egress. Recheck prices on the day of procurement.
- Run an anonymized pilot with realistic timetable/attendance spikes, database pools, and school-day traffic. Record p50/p95 response time, database CPU/connections, requests, logs GiB, file egress, and costs per active school/student. This is a recommended future validation step; no performance test has been run for this estimate.
- Check that each module's connection pool is small enough for the database, particularly if deploying the 21 modules in one process or multiple Cloud Run instances.
- Use push notifications/in-app notices for routine low-risk events; reserve SMS for high-value/urgent delivery and budget it from actual sends.
- Cache or pre-generate repeated learning assets. Uploaded video and file downloads can make egress cost exceed the storage line by a wide margin.
- Keep audio ephemeral, locally trigger speech capture, cap clip length, and review regional language-model availability before enabling the feature in schools.

## 12. Official source list

Prices and India programme rules change. The following official links were checked for this model on 10 November 2026:

- [Cloud Run pricing](https://cloud.google.com/run/pricing)
- [Cloud SQL pricing](https://cloud.google.com/sql/pricing)
- [Cloud SQL PostgreSQL region availability](https://docs.cloud.google.com/sql/docs/postgres/region-availability-overview)
- [Cloud Storage pricing](https://cloud.google.com/storage/pricing)
- [Google Cloud network pricing](https://cloud.google.com/vpc/network-pricing)
- [Premium egress price schedule](https://cloud.google.com/vpc/pricing-announce)
- [Pub/Sub pricing](https://cloud.google.com/pubsub/pricing)
- [Cloud Logging pricing](https://cloud.google.com/logging)
- [Speech-to-Text pricing](https://cloud.google.com/speech-to-text/pricing)
- [Speech-to-Text supported languages by region](https://docs.cloud.google.com/speech-to-text/docs/speech-to-text-supported-languages)
- [Text-to-Speech pricing](https://cloud.google.com/text-to-speech/pricing)
- [Gemini Enterprise Agent Platform pricing](https://cloud.google.com/gemini-enterprise-agent-platform/generative-ai/pricing)
- [RBI exchange rates](https://www.rbi.org.in/home.aspx/webservice/home.aspx/scripts/Scripts/scripts/FS_PressRelease.aspx?fn=2759)
- [MCA SPICe+ fees FAQ](https://www.mca.gov.in/Ministry/pdf/SPICe%2Band_linked_filings_FAQs_V3_13%20Jan_2022_updated.pdf)
- [Startup India recognition](https://www.startupindia.gov.in/content/sih/en/startupgov/startup_recognition_page.html)
- [Startup India Seed Fund Scheme](https://seedfund.startupindia.gov.in/)
- [MeitY DPDP Rules](https://www.meity.gov.in/documents/act-and-policies/digital-personal-data-protection-rules-2025-gDOxUjMtQWa?pageTitle=Digital-Personal-Data-Protection-Rules-2025%3B)
- [TRAI requirements for bulk senders](https://www.trai.gov.in/advice-to-senders)
- [DoT Equipment Type Approval](https://eservices.dot.gov.in/equipment-type-approval-eta)
