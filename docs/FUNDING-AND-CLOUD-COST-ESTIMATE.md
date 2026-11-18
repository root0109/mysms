# MySMS: Google Cloud Cost, N-Year Model and India Funding Plan

**Prepared:** 10 November 2026 (revision 2 of the same day's draft)  
**Scope:** Multi-tenant school management + LMS SaaS, launch target **50,000 students, 1,000 teachers, 1 TiB of files**, hosted in Google Cloud Mumbai (`asia-south1`); optional classroom voice device.  
**Status:** Investor-planning model. It is not a Google quote, a CA/legal opinion, an audited forecast or a performance guarantee. Every price below was read from the linked source on 10 November 2026. Anything not verifiable is marked **assumption** with its reasoning.  
**Currency:** USD converted at **₹94.00 per USD**, rounded up from the RBI/FBIL reference rate of **₹93.8579 on 9 November 2026** ([RBI](https://www.rbi.org.in/home.aspx)). 1 lakh = ₹1,00,000; 1 crore = ₹1,00,00,000 = 100 lakh. 1 TiB = 1,024 GiB; billing month = 730 hours = 2,628,000 seconds.

---

## 1. Executive summary: the ask first

| Item | Number | Basis |
|---|---|---|
| **Total to raise before Series A** | **₹11.5 crore (~US$1.22 M)** in two tranches | Cash need of ₹6.8–8.2 crore to March 2029 (§6.4) plus a 40–70% buffer |
| Tranche 1: pre-seed (angels / angel networks + SISFS) | **₹3.0 crore**, close by March 2027 | Funds build-out, 5–10 paid pilots for the April 2027 academic year |
| Tranche 2: seed (institutional) | **₹8.5 crore (~US$0.9 M)**, close Oct–Dec 2027 | Funds the Jan–Mar 2028 sales season and the AY 2028-29 rollout to 50,000 students |
| Series A (only if gates in §10 are met) | ₹30–45 crore (US$3.2–4.8 M), H2 2029 | Base-case cumulative trough is −₹12.2 crore in FY2030-31 (§5) |
| **Google Cloud bill at 50,000 students** (recommended configuration, all environments, before GST) | **₹1.46 lakh/month = ₹17.5 lakh/year = ₹35 per student per year** (US$1,551/month) | §4; range ₹0.98 lakh (lean) to ₹3.27 lakh (safe) per month |
| Cloud as share of total spend at 50,000 students | **~3%** | FY2028-29 cloud ₹17.5 lakh vs COGS + opex ₹6.0 crore (§5) |
| Fully loaded cost to serve one student | **~₹134/year** (cloud ₹35 + messaging ₹6 + customer success ₹93) | §8 |
| Price needed for a viable business | **≥ ₹400 per student per year** (≈ ₹2 lakh per 500-student school), with ≥ 150,000 students | At ₹250–300 the base case does not break even within 5 years (§5.3) |
| Base-case break-even | **FY2031-32 (year 5)** at 500,000 students, revenue ₹24.3 crore | §5 |
| Gross margin | 70% at 50,000 students, 78–79% at 150,000+ | Investor benchmark: 81% median subscription gross margin ([Benchmarkit 2025](https://www.hibob.com/wp-content/uploads/2025-SaaS-Performance-Metrics-Benchmarks.pdf)) |

**Three facts an investor should take away.**

1. **The cloud is not the problem.** At the founder's target scale the platform costs about ₹35 per student per year on Google Cloud list prices. People (engineering, implementation, customer success, sales, founders, G&A staff) are ~84% of FY2028-29 spend in the base model; payroll is 75% of the 24-month plan (§6.4).
2. **50,000 students is a launch milestone, not a business.** At ₹400–463 per student, 50,000 students yield ₹2.0–2.3 crore of revenue a year, which cannot carry a 22–27-person company. The conservative scenario (stuck at 50,000) never breaks even (§5). The plan therefore requires ~150,000 students by FY2029-30.
3. **Pricing is the biggest risk.** Market price signals for Indian school ERPs are ₹100–300 per student per year (community estimates, §8.3). MySMS must win at ≈ ₹400 with an ERP + LMS + apps bundle, or reduce support cost per school.

---

## 2. Assumptions (change these when pilots produce data)

| # | Input | Value | Source / reasoning |
|---|---|---|---|
| A1 | Students | 50,000 at target; scenarios in §5 | Founder requirement |
| A2 | Teachers | 1,000 | Founder requirement. This is 1 teacher per 50 students; real schools will create more staff accounts. Cloud impact is negligible (+2–3k users). |
| A3 | Schools | 100 × 500 students | **Assumption** for sizing; CBSE/state-board private schools commonly run 300–1,500 students |
| A4 | Guardians | 75,000 accounts (1.5 per student) | **Assumption** |
| A5 | Daily active users (school day) | 15,000 students + 10,000 guardians + 1,000 teachers + 300 administrators = **26,300** | **Assumption** (30% student, 13% guardian DAU) |
| A6 | API calls per DAU per day | student 40, guardian 20, teacher 150, administrator 300 | **Assumption**; timetable, homework, LMS, attendance, marks screens |
| A7 | School days / weekend activity | 22 school days/month; 8 weekend days at 20% of school-day traffic | **Assumption** |
| A8 | Files | 1 TiB Standard storage in Mumbai, +85 GiB new uploads/month | Founder requirement; growth **assumption** (1 TiB/12) |
| A9 | Runtime | Modular monolith (all 21 modules in one JVM) on Cloud Run; managed Pub/Sub event bus | Repository supports both monolith and 21 services ([ARCHITECTURE.md](../ARCHITECTURE.md)); the maintainability review recommends monolith-first ([MAINTAINABILITY-REVIEW.md](MAINTAINABILITY-REVIEW.md)) |
| A10 | Region | Mumbai `asia-south1`; disaster-recovery copies in Delhi `asia-south2` where stated | Data residency and latency for Indian schools |
| A11 | Price per student | ₹400/year base, +5%/year; pilots at 50%; implementation fee ₹25,000 per new school | **Assumption**; sensitivity at ₹250/₹300/₹600 in §5.3 |
| A12 | School churn | 10% of students per year | **Assumption**; sensitivity at 20% in §8 |
| A13 | Collections | Annual contracts invoiced April–June; 50% collected in Q1 of the academic year, 30% Q2, 20% Q3 | **Assumption** reflecting termly payment by many schools |
| A14 | Sales season | Contracts signed January–March for the academic year starting April (CBSE) or June (several state boards) | **Assumption** based on Indian academic calendar |
| A15 | FX | ₹94.00/USD | RBI/FBIL ₹93.8579 on 9 Nov 2026 |

Product state that the money must also fund: 1,709 of 2,049 API operations are served and 340 still return `501 NOT_IMPLEMENTED`; 88 of 186 inventory screens are built ([STATUS.md](../STATUS.md)). There is no production traffic history and no load test; every capacity figure below must be replaced by measurements from the pilot (gate G2, §10).

---

## 3. Workload sizing (how the configurations were derived)

### 3.1 Requests

| Quantity | Formula | Result |
|---|---|---|
| Requests per school day | 15,000×40 + 10,000×20 + 1,000×150 + 300×300 | 1,040,000 |
| Requests per month | 1,040,000×22 + 1,040,000×0.2×8 | 24.5 M → **25 M** used |
| Average over the month | 25 M ÷ (30×86,400 s) | 9.6 req/s |
| Average in school hours | 0.8 × 1,040,000 ÷ (8 h × 3,600 s) | 28.9 req/s |
| Morning attendance peak | 5 × school-hours average (**assumption**) | ~150 req/s |
| **Design peak** | 2 × attendance peak | **300 req/s** |

### 3.2 Database

The monolith calls co-located modules over loopback HTTP/gRPC ([ARCHITECTURE.md](../ARCHITECTURE.md) §1), and every module unit of work begins a transaction, binds the tenant (`set_config`), runs its SQL and commits.

| Quantity | Formula | Result |
|---|---|---|
| Module operations per external request | **assumption** (e.g. attendance → sis, academics, timetable) | 3 |
| SQL statements per module operation | BEGIN + set_config + 2 queries + COMMIT | 5 |
| Statements/s at design peak | 300 × 3 × 5 | **4,500/s** |
| Statements/s at attendance peak | 150 × 3 × 5 | 2,250/s |
| Concurrent active statements (Little's law) | 4,500/s × 2 ms average (**assumption**, indexed point/range queries) | ~9 |
| CPU needed | **Assumption:** 2,000–5,000 simple indexed statements/s per vCPU (validate with `pgbench` on the chosen tier) | 4 vCPU = 8,000–20,000/s capacity → 23–56% busy at design peak; 2 vCPU is adequate only for the real (150 req/s) peak |
| Year-1 data size | attendance 50,000 × 220 days ≈ 11 M rows ≈ 5 GiB with indexes; audit ~2.5 M writes/month × 1 KB ≈ 28 GiB/yr; other modules 10–20 GiB; outbox/inbox purged weekly | **50–80 GiB** (assumption) |
| RAM | hot set (current-year rosters, timetables, attendance) < 10 GiB | 16 GiB is comfortable; 8 GiB is tight |

**Connections — a hard limit the draft missed.** Google documents that each Cloud Run instance can open at most **100 connections** to a Cloud SQL database through the built-in Cloud SQL connection ([Cloud SQL from Cloud Run](https://docs.cloud.google.com/sql/docs/postgres/connect-run)). The repository default of 10 connections per module × 21 module pools = **210 per process** ([DEPLOYMENT.md](../DEPLOYMENT.md), [MAINTAINABILITY-REVIEW.md](MAINTAINABILITY-REVIEW.md)), so the monolith as configured today would exceed that limit on Cloud Run. Required production profile:

| Setting | Value | Total |
|---|---|---|
| `MYSMS_<MODULE>_DB_POOL_SIZE` in the API service | 3 | 21 × 3 = 63 per instance |
| API service max instances | 6 | 6 × 63 = 378 |
| Worker service (outbox relays, jobs) | 1 instance × 21 × 2 | 42 |
| PostgreSQL `max_connections` | set explicitly to ~500 | ~420 used at worst case |

Run the 21 outbox relays (500 ms polling, [DEPLOYMENT.md](../DEPLOYMENT.md)) **only** in the worker service. If they run in every API instance, idle polling multiplies with instance count, and with request-based billing the CPU is throttled between requests, so relays would stall.

### 3.3 Network egress (what users download)

| Traffic | Formula | GiB/month |
|---|---|---:|
| API JSON (gzip) | 25 M requests × 8 KB (**assumption**) | 186 |
| Web app static assets | 2,000 web DAU × 0.5 MB × 30 days (mostly cached) | 28 |
| Files (homework, LMS PDFs, images, report cards) | (15,000×3 MB + 10,000×1 MB + 1,000×10 MB) = 65 GB/school day; × (22 + 8×0.2) days | 1,429 |
| **Total** | | **1,643 GiB ≈ 1.6 TiB** |

Hosted video changes this completely: one 50 MB lesson video per active student per week adds ~3,035 GiB/month (15,000 × 50 MB × 4.345 weeks). Link video from YouTube/Vimeo or a dedicated video service until a paying customer funds it (sensitivity in §4.6).

---

## 4. Monthly Google Cloud bill (three configurations)

### 4.1 Unit prices used (Mumbai `asia-south1`, list, USD)

Google's pricing pages render regional prices with JavaScript; the static page shows Iowa (`us-central1`). Mumbai prices were therefore taken from (a) the open-source mirror of the **Cloud Billing Catalog** generated on 5 Nov 2026 ([Cyclenerd pricing.yml](https://github.com/Cyclenerd/google-cloud-pricing-cost-calculator)), (b) per-region Cloud SQL listings ([Spare Cores](https://sparecores.com/database/gcp/db-n1-standard-16), [Bytebase dbcost](https://www.bytebase.com/dbcost/cloudsql/instance/db-standard-1.md)), and (c) Google pages where the price is not regional.

| Service / SKU | Mumbai price | How it was established |
|---|---|---|
| Cloud SQL Enterprise vCPU / memory | $0.0496 per vCPU-h; $0.0084 per GiB-h | Solved from two Mumbai listings: db-n1-standard-1 (1 vCPU, 3.75 GiB) = $0.0811/h and db-n1-standard-16 (16 vCPU, 60 GiB) = $1.2976/h ([1](https://sparecores.com/database/gcp/db-n1-standard-1), [2](https://sparecores.com/database/gcp/db-n1-standard-16)). Check: 0.0496 + 3.75×0.0084 = 0.0811 ✓; 16×0.0496 + 60×0.0084 = 1.2976 ✓. ≈ 1.20 × Iowa ($0.0413 / $0.007, [Cloud SQL pricing](https://cloud.google.com/sql/pricing)). |
| Cloud SQL Enterprise **HA** vCPU / memory | $0.0991; $0.0168 | Same listings, HA rows: $0.1621/h and $2.5936/h. Check: 0.0991 + 3.75×0.0168 = 0.1621 ✓; 16×0.0991 + 60×0.0168 = 2.5936 ✓ |
| Cloud SQL Enterprise Plus vCPU / memory (HA ×2) | $0.0645; $0.01092 (HA $0.1289; $0.02184) | **Derived:** Iowa Enterprise Plus ÷ Enterprise ratio × Mumbai Enterprise; cross-checked against Mumbai db-standard-1 Enterprise Plus = $77/month (model gives $76.98) ([Bytebase](https://www.bytebase.com/dbcost/cloudsql/instance/db-standard-1.md)) |
| Cloud SQL CUD | −25% (1 year), −52% (3 years) on vCPU and memory; **not** on storage or network | [Cloud SQL pricing](https://cloud.google.com/sql/pricing): $0.0413 → $0.030975 / $0.019824 |
| Cloud SQL SSD / HA SSD / backups | $0.204 / $0.408 / $0.096 per GiB-month | **Derived (assumption):** Iowa $0.17 / $0.34 / $0.08 ([Cloud SQL pricing](https://cloud.google.com/sql/pricing)) × 1.2, the measured Mumbai/Iowa ratio for Cloud SQL compute and for Compute Engine PD-SSD ($0.204 vs $0.17, [Billing Catalog mirror](https://github.com/Cyclenerd/google-cloud-pricing-cost-calculator)). Mumbai Cloud SQL storage SKUs were not publicly readable today. |
| Cloud Run instance-based | $0.000018 per vCPU-s; $0.000002 per GiB-s | Mumbai is listed under **Tier 1** pricing ([Cloud Run pricing](https://cloud.google.com/run/pricing)) |
| Cloud Run request-based | active $0.000024 per vCPU-s, $0.0000025 per GiB-s; idle minimum instance $0.0000025 per vCPU-s and per GiB-s; $0.40 per million requests; 2 M requests free | [Cloud Run pricing](https://cloud.google.com/run/pricing) |
| Cloud Run CUD | −17% (1-year Cloud Run CUD); Compute Flexible CUD −28% (1 yr) / −46% (3 yr) on instance-based | Same page: $0.000018 → $0.00001494 / $0.00001296 / $0.00000972 |
| Internet egress, Premium Tier, to Asia | first 1 GiB free; $0.12/GiB to 1 TiB; $0.11/GiB 1–10 TiB; $0.085/GiB above | [Network pricing](https://cloud.google.com/vpc/network-pricing); Mumbai identical in [Billing Catalog mirror](https://github.com/Cyclenerd/google-cloud-pricing-cost-calculator) |
| External Application Load Balancer | $0.025/h for first 5 forwarding rules; $0.008/GiB inbound and outbound data processed | [Network pricing](https://cloud.google.com/vpc/network-pricing) (Iowa shown; Mumbai **assumed equal**) |
| Cloud Armor Standard | $5/policy/month, $1/rule/month, $0.75 per million requests | [Network pricing — Cloud Armor](https://cloud.google.com/vpc/network-pricing) |
| Cloud CDN (Asia) | $0.09/GiB cache egress (0–10 TiB), $0.0075 per 10,000 lookups | [Network pricing — Cloud CDN](https://cloud.google.com/vpc/network-pricing) |
| Cloud Storage Standard, Mumbai | $0.023/GiB-month; Class A $0.005, Class B $0.0004 per 1,000 ops | [Billing Catalog mirror](https://github.com/Cyclenerd/google-cloud-pricing-cost-calculator); [Cloud Storage pricing](https://cloud.google.com/storage/pricing) |
| Cloud Storage Coldline, Delhi | $0.006/GiB-month; Class A $0.02 per 1,000 | Same |
| Bucket-to-bucket transfer Asia → Asia | $0.08/GiB | [Cloud Storage pricing](https://cloud.google.com/storage/pricing) |
| Pub/Sub | 10 GiB/month free, then $40/TiB, all regions | [Pub/Sub pricing](https://cloud.google.com/pubsub/pricing) |
| Cloud Logging | first 50 GiB/project/month free, then $0.50/GiB (includes 30 days); $0.01/GiB-month beyond 30 days | [Observability pricing](https://cloud.google.com/stackdriver/pricing) |
| Cloud Monitoring | first 150 MiB free, then $0.258/MiB; Prometheus samples $0.06/million | Same |
| Secret Manager | $0.06 per active secret version/month (6 free); $0.03 per 10,000 accesses | [Secret Manager pricing](https://cloud.google.com/secret-manager/pricing) |
| Artifact Registry | 0.5 GiB free, then $0.10/GiB-month | [Artifact Registry pricing](https://cloud.google.com/artifact-registry/pricing) |
| Cloud Build | 2,500 free build-minutes/month (e2-standard-2), then $0.006/min | [Cloud Build pricing](https://cloud.google.com/build/pricing) |
| Cloud NAT, Mumbai | $0.0448/h per gateway; $0.045/GiB processed | [Billing Catalog mirror](https://github.com/Cyclenerd/google-cloud-pricing-cost-calculator) |
| Not priced | Memorystore (not recommended at launch, page did not render today); Cloud DNS (≈$1/month budgeted inside "tooling" as an **assumption**); Google Cloud Customer Care plans | Re-check before buying |

### 4.2 The three configurations

| | Lean | **Recommended** | Safe |
|---|---|---|---|
| When to use | Pilot year; ≤ 30,000 students; accepts a single region | **Launch at 50,000 students** | Contractual uptime/RPO demands, or 150,000+ students |
| Cloud Run API | 2 always-on 2 vCPU/4 GiB instances, instance-based billing, relays in-process | Request-based billing, min 2 instances, max 6, concurrency 80; separate worker (1 vCPU/2 GiB, instance-based) for relays and jobs | 3 always-on API instances + 2 workers, instance-based |
| Cloud SQL | Enterprise, HA, 2 vCPU/8 GiB, 100 GiB SSD | Enterprise, HA, **4 vCPU/16 GiB**, 250 GiB SSD; read replica added only when CPU > 60% (+$172.47/month) | Enterprise, HA, 8 vCPU/32 GiB, 500 GiB; 4 vCPU/16 GiB read replica; 4 vCPU/16 GiB **Delhi** DR replica |
| Files backup | Soft delete only | Daily copy to Delhi Coldline | Daily copy to Delhi Coldline |
| Edge | Global ALB + Cloud Armor Standard | + Cloud NAT static egress IP (SMS/payment-gateway allow-lists) | Same as recommended |
| Logging retention | 30 days | 30 days in Logging + export to Coldline for 1 year (DPDP log retention, §7.4) | 90 days in Logging |
| Staging / dev | Staging scheduled 10 h × 22 days | Staging scheduled + small dev DB | Staging always-on + dev + load-test allowance |

### 4.3 Monthly bill, side by side (USD list, before GST)

| Line | Lean | **Recommended** | Safe |
|---|---:|---:|---:|
| Cloud Run (API + workers) | 259.14 | **297.37** | 546.16 |
| Cloud SQL (primary, storage, backups, replicas) | 293.20 | **606.80** | 1,911.48 |
| Internet egress, 1,643 GiB | 190.85 | **190.85** | 190.85 |
| Load balancer (rule + data processing) | 32.45 | **32.45** | 32.45 |
| Cloud Armor Standard | 33.75 | **33.75** | 33.75 |
| Cloud Storage 1 TiB + soft delete + operations | 25.52 | **25.52** | 25.52 |
| Object backup copy (Delhi Coldline) | 0.00 | **14.74** | 14.74 |
| Pub/Sub (allowance) | 5.00 | **5.00** | 5.00 |
| Cloud Logging | 5.00 | **25.00** | 53.00 |
| Cloud Monitoring | 5.00 | **15.00** | 30.00 |
| Secret Manager, Artifact Registry, Cloud Build, DNS | 17.00 | **17.00** | 17.00 |
| Cloud NAT | 0.00 | **33.15** | 33.15 |
| **Production subtotal** | **866.91** | **1,296.64** | **2,893.11** |
| Staging + development | 81.61 | **113.53** | 273.39 |
| 10% usage contingency | 94.85 | **141.02** | 316.65 |
| **Total, USD/month** | **1,043.37** | **1,551.18** | **3,483.16** |
| **Total, INR/month** | **₹98,077** | **₹1,45,811** | **₹3,27,417** |
| **Annual, INR** | **₹11.77 lakh** | **₹17.50 lakh** | **₹39.29 lakh** |
| Per student per year (÷ 50,000) | ₹23.5 | **₹35.0** | ₹78.6 |
| GST cash outflow at 18% (recoverable as ITC, §4.7) | ₹1,15,731/month gross | ₹1,72,057/month gross | ₹3,86,352/month gross |

Subtotals and totals are computed from unrounded line items, so a column of the rounded figures above can differ by up to $0.02.

### 4.4 Line-item calculations

**Cloud SQL (Mumbai Enterprise HA $0.0991/vCPU-h, $0.0168/GiB-h; non-HA $0.0496, $0.0084; 730 h)**

| Item | Formula | USD/month |
|---|---|---:|
| Lean primary 2 vCPU/8 GiB HA | (2×0.0991 + 8×0.0168) × 730 | 242.80 |
| Lean storage + backups | 100×0.408 + 100×0.096 | 50.40 |
| **Recommended primary 4 vCPU/16 GiB HA** | (4×0.0991 + 16×0.0168) × 730 | **485.60** |
| Recommended storage + backups | 250×0.408 + 200×0.096 | 121.20 |
| Optional read replica 2 vCPU/8 GiB + 250 GiB | (2×0.0496 + 8×0.0084) × 730 + 250×0.204 | 172.47 |
| Safe primary 8 vCPU/32 GiB HA | (8×0.0991 + 32×0.0168) × 730 | 971.19 |
| Safe read replica and Delhi DR replica, each 4 vCPU/16 GiB + 500 GiB | (4×0.0496 + 16×0.0084) × 730 + 500×0.204 | 344.94 each |
| Safe HA storage + backups + WAL to Delhi | 500×0.408 + 400×0.096 + 100 GiB×0.08 (**assumption**: Asia inter-region rate) | 250.40 |
| Reference: Enterprise Plus 8 vCPU/64 GiB HA | (8×0.1289 + 64×0.02184) × 730 | 1,773.14 |

**Committed-use discounts on Cloud SQL compute (storage excluded):**

| Configuration | Compute in CUD scope | 1-year saving (25%) | 3-year saving (52%) |
|---|---:|---:|---:|
| Lean | $242.80 | $60.70 (₹5,706)/month | $126.25 (₹11,868)/month |
| Recommended | $485.60 | $121.40 (₹11,412)/month | $252.51 (₹23,736)/month |
| Safe (primary + 2 replicas) | $1,457.08 | $364.27 (₹34,241)/month | $757.68 (₹71,222)/month |

Buy a 1-year CUD only after 3 months of stable production sizing; a 3-year CUD only after Series A. Sustained-use discounts do not apply to Cloud SQL or Cloud Run.

**Why the draft's Cloud SQL envelope was oversized.** The draft budgeted 8–16 vCPU / 32–64 GiB HA at $1,000–2,500/month. The workload in §3.2 needs ~4 vCPU at a 2× design peak; 8 vCPU/32 GiB HA costs $971.19 for compute alone and 16 vCPU/64 GiB HA would cost 2 × that. That size is the *safe* configuration for ~150,000 students, not the launch configuration. Enterprise Plus is justified only if a customer contract requires its higher availability tier; at 8 vCPU/64 GiB HA it is 3.65× the recommended primary.

**Cloud Run (Tier 1)**

| Item | Formula | USD/month |
|---|---|---:|
| One always-on API instance, 2 vCPU/4 GiB, instance-based | (2×0.000018 + 4×0.000002) × 2,628,000 s | 115.63 |
| One always-on worker, 1 vCPU/2 GiB | (1×0.000018 + 2×0.000002) × 2,628,000 | 57.82 |
| Lean | 2 × 115.63 + burst (2 extra instances × 4 h × 22 days × $0.1584/h) | 259.14 |
| Recommended API, request-based: active | 2 instances × 252 active h (10 h × 22 school days + 4 h × 8 weekend days) × $0.2088/h [(2×0.000024 + 4×0.0000025)×3,600] | 105.24 |
| Recommended API: idle minimum instances | 2 × (730 − 252) h × $0.054/h [(2+4)×0.0000025×3,600] | 51.62 |
| Recommended API: peak burst | 4 instances × 4 h × 22 days × $0.2088/h | 73.50 |
| Recommended API: requests | (25 M − 2 M free) × $0.40/M | 9.20 |
| **Recommended total** | 239.56 + worker 57.82 | **297.37** |
| Safe | 3 × 115.63 + 6 × 4 h × 22 × 0.1584 + 2 × 57.82 | 546.16 |

The ~$5/month Cloud Run free tier is ignored (conservative). JVM sizing: 4 GiB per instance allows ~2.5–3 GiB heap plus metaspace for 21 Spring contexts (**assumption**; the full Maven build alone peaks at ~4.5 GB per [DEPLOYMENT.md](../DEPLOYMENT.md), runtime will be lower — measure). Java start-up for 21 modules will be tens of seconds, which is why minimum instances are kept warm.

**When an alternative is cheaper**

| Option (Mumbai) | 2 always-on 2 vCPU workers | Notes |
|---|---:|---|
| Cloud Run instance-based, 2 vCPU/4 GiB | $231.26 | No cluster to operate; per-second scaling |
| GKE Autopilot, 2 vCPU/4 GiB pods | $190.42 | Iowa $0.0445/vCPU-h, $0.0049225/GiB-h ([GKE pricing](https://cloud.google.com/kubernetes-engine/pricing)) × 1.2 Mumbai ratio (**assumption**); $0.10/h cluster fee is offset by the $74.40/month free-tier credit for one Autopilot cluster |
| Compute Engine 2 × e2-standard-2 (2 vCPU/8 GiB) | $117.51 | $0.08048/h in Mumbai ([Billing Catalog mirror](https://github.com/Cyclenerd/google-cloud-pricing-cost-calculator)); you patch, scale, deploy and secure the VMs |

At launch the difference between Cloud Run and VMs is ~$114/month (~₹10,700), less than one engineer-day. Move to GKE Autopilot when the steady state exceeds ~8–10 always-on instances or when the 21 modules are deployed as separate services; consider Compute Engine only for RabbitMQ if Pub/Sub is rejected.

**Usage lines (same in all three configurations)**

| Item | Formula | USD/month |
|---|---|---:|
| Egress 1,643 GiB | 1,023 × 0.12 + 619 × 0.11 | 190.85 |
| Load balancer | 0.025 × 730 + (1,643 + 132 inbound) GiB × 0.008 | 32.45 |
| Cloud Armor | 5 + 10 × 1 + 25 × 0.75 | 33.75 |
| Cloud Storage | 1,024 × 0.023 + 2% soft-deleted bytes + 100,000 Class A × $0.005/1,000 + 2.5 M Class B × $0.0004/1,000 | 25.52 |
| Delhi Coldline copy (recommended, safe) | 1,024 × 0.006 + 85 GiB/month × 0.08 transfer + 90,000 Class A × $0.02/1,000; plus one-time $81.92 initial copy | 14.74 |
| Pub/Sub | (~5 M domain events + 5 M audit events) × 2 KB = 20 GB published, plus delivery to ~2 subscribers (publish and delivery bytes are both billed) = 60 GB ≈ 56 GiB → (56 − 10) × $40/1,024 = $1.80; **$5 allowance** for retention/backlog | 5.00 |
| Logging | Lean: ≤ 50 GiB inside the free tier, $5 allowance. Recommended: (100 − 50 free) GiB × 0.50 = 25.00; 1-year export to Coldline adds ≈ $7 (inside contingency). Safe (**assumption** ~150 GiB, 90-day): (150 − 50) × 0.50 + 300 GiB × $0.01 retention beyond 30 days = 53.00 | 5 / 25 / 53 |
| Cloud NAT | 0.0448 × 730 + 10 GiB × 0.045 | 33.15 |
| Tooling | Secrets ~$2 + Artifact Registry ~$2 + Cloud Build (4,400 − 2,500) min × $0.006 ≈ $11 + DNS ~$1 (**assumption**) + rounding | 17.00 |
| Monitoring | **Assumption:** custom application metrics and ~20 alert policies at $0.35 per metric reference (alerting charges take effect 1 September 2027 per the pricing page; budgeted from launch, conservative) | 5 / 15 / 30 |

**Staging and development (often omitted):** staging Cloud SQL 2 vCPU/8 GiB non-HA scheduled 220 h/month = $36.61 + 50 GiB SSD $10.20 + backups $4.80 = $51.61; Cloud Run staging ~$20; misc ~$10 → lean $81.61. Recommended adds a 1 vCPU/3.75 GiB dev database scheduled 220 h ($17.84 + 20 GiB $4.08 = $21.92) and dev Cloud Run $10 → $113.53. Safe runs the staging database 24×7 ($121.47 compute + $15.00 storage and backups = $136.47), always-on staging Cloud Run ~$40, misc ~$15, the same dev database ($21.92) and dev Cloud Run ($10), plus a $50 load-test allowance → $273.39. Developers keep using Testcontainers locally, as the repository already does.

### 4.5 Google for Startups credits

| Tier | Credit | Eligibility |
|---|---|---|
| Start | US$2,000 | Founded in the past 2 years, working MVP, plans to raise |
| Scale (non-AI) | Year 1: 100% of usage up to US$100,000; Year 2: 20% of usage up to a further US$100,000 | **Verifiable pre-seed to Series A funding from a known investor**; founded within 3–5 years; company domain must match the applicant's email |
| Scale AI | Year 1: up to US$250,000; Year 2: 20% up to US$100,000 | AI-first startups |

Source: [Google for Startups Cloud Program](https://cloud.google.com/startup). Educational institutions are excluded, but MySMS (a company selling to schools) is not an educational institution. **Realistic value to MySMS:** year-1 cloud ≈ ₹8.9 lakh (fully covered) + 20% of year-2 ≈ ₹3.5 lakh ≈ **₹12.4 lakh (~US$13,000)**, because the platform cannot consume more. Credits are upside; they are not in the cash plan, and they must not be used to justify oversizing.

### 4.6 Egress sensitivity

| Monthly egress | Formula | USD | INR |
|---|---|---:|---:|
| 1 TiB | 1,023 × 0.12 | 122.76 | ₹11,539 |
| **1,643 GiB (base)** | 1,023 × 0.12 + 619 × 0.11 | **190.85** | **₹17,940** |
| Base + weekly 50 MB video per active student (+3,035 GiB) | 1,023 × 0.12 + 3,654 × 0.11 | 524.70 | ₹49,322 |
| 5 TiB | 1,023 × 0.12 + 4,096 × 0.11 | 573.32 | ₹53,892 |
| 10 TiB | 1,023 × 0.12 + 9,216 × 0.11 | 1,136.52 | ₹1,06,833 |

Serving shared LMS content through Cloud CDN at $0.09/GiB (Asia) instead of Premium egress at $0.11–0.12 saves ~18–25% on cacheable bytes; private per-student files gain little from a CDN.

### 4.7 GST and TDS on the cloud invoice

* **GST.** Google Cloud India Private Limited charges **18% GST** to customers with an Indian billing address; customers contracted with Google Asia Pacific are charged **0% India GST with a valid GSTIN** (18% without one) ([Google Cloud tax overview](https://docs.cloud.google.com/billing/docs/resources/vat-overview)). The page does not describe reverse charge; whether MySMS must self-assess GST on an import of services from Google Asia Pacific is a question for the CA. In both cases GST on the cloud bill is creditable as input tax credit (ITC) against the 18% output GST on MySMS's own SaaS invoices (§7.3), so it is a **cash-timing** item (₹26,246/month at the recommended size), not a cost.
* **TDS.** Customers of Google Cloud India deduct TDS at the prescribed rate, file the certificate quarterly (deadlines 15 Aug, 15 Nov, 15 Feb, 15 Jun) and Google credits the amount to the billing account within 7 business days of validation ([Google Cloud India TDS](https://docs.cloud.google.com/billing/docs/resources/tds)). The page cites the Income-tax Act, 2025; confirm the applicable section and rate with the CA.

### 4.8 Non-Google running costs at 50,000 students

| Item | Formula | INR/month (before 18% GST) |
|---|---|---:|
| SMS (OTP, fee reminders, urgent alerts) | 60,000 × ₹0.17 ([MSG91 India rates](https://msg91.com/in/pricing/sms), 60k–4.5 lakh slab) | ₹10,200 |
| WhatsApp utility templates (absence alerts, receipts) | 100,000 × ₹0.1323 (India utility rate, [Gallabox rate card of Meta prices](https://docs.gallabox.com/pricing-and-billing/whatsapp-pricing/rate-card.md)); utility messages inside an open 24-hour service window are free ([Meta](https://developers.facebook.com/docs/whatsapp/pricing)) | ₹13,230 |
| Transactional e-mail | 200,000 × $0.10/1,000 × ₹94 ([Amazon SES](https://aws.amazon.com/ses/pricing/)) | ₹1,880 |
| **Total** | | **₹25,310 (₹6.07 per student per year)** |

Push notifications are the default channel (**assumption:** no per-message charge for mobile push). SMS requires Principal Entity, header, content-template and consent registration on the DLT platforms ([TRAI](https://www.trai.gov.in/advice-to-senders)). Payment-gateway fees are paid on fee collections by schools/parents and are not MySMS revenue or cost.

### 4.9 Optional classroom voice assistant (1,000 devices)

Assumes local wake-word or push-to-talk, 15-second clips, 150-character spoken replies, 2,000 input + 200 output tokens per command, 22 school days.

| Usage | Commands/month | Speech-to-Text V2 Standard ($0.016/min) | TTS Neural2 ($16/M chars, 1 M free) | Gemini 2.5 Flash-Lite ($0.10/M in, $0.40/M out) | Total |
|---|---:|---:|---:|---:|---:|
| 5 commands/device/day | 110,000 (27,500 min) | $440.00 | $248.00 | $30.80 | **$718.80 = ₹67,567** |
| 20 commands/device/day | 440,000 (110,000 min) | $1,760.00 | $1,040.00 | $123.20 | **$2,923.20 = ₹2,74,781** |
| 20/day with Standard TTS voices ($4/M, 4 M free) | 440,000 | $1,760.00 | $248.00 | $123.20 | $2,131.20 = ₹2,00,333 |

Sources: [Speech-to-Text pricing](https://cloud.google.com/speech-to-text/pricing) (V2 Standard is $0.016/min for the first 500,000 minutes; the draft used the V1 "without data logging" rate of $0.024), [Text-to-Speech pricing](https://cloud.google.com/text-to-speech/pricing), [Gemini pricing](https://cloud.google.com/gemini-enterprise-agent-platform/generative-ai/pricing). **Region issue:** the supported-languages table lists `en-IN` and `hi-IN` Chirp 2 in `asia-southeast1` (Singapore) and `europe-west4`, and Chirp 3 in `eu`; no `asia-south1` rows were visible ([languages by region](https://docs.cloud.google.com/speech-to-text/docs/speech-to-text-supported-languages)). Classroom audio would be processed outside India unless an on-device or self-hosted model is used.

Hardware (unchanged from the draft and still an **assumption**, no supplier quote): ₹10,000–18,000 installed per device; 100-device pilot ₹10–18 lakh; 1,000 devices ₹1.0–1.8 crore; custom design/firmware/validation ₹35–75 lakh. Wireless products in de-licensed bands use DoT Equipment Type Approval by self-declaration; the page lists a fee of ₹10,000 ([DoT ETA](https://eservices.dot.gov.in/equipment-type-approval-eta)). Keep the voice device **out of the core raise**; fund it from a CSR sponsor or a school-group contract after gate G4.

---

## 5. N-year model (FY = April–March; Y1 = FY2027-28)

### 5.1 Formulas (extend to any year *n*)

| Quantity | Formula |
|---|---|
| Students | Sₙ (scenario input) ; schools = Sₙ ÷ 500 |
| New schools | (Sₙ − Sₙ₋₁ × (1 − 0.10)) ÷ 500 |
| Price | Pₙ = 400 × 1.05^(n−2) for n ≥ 2; P₁ = 200 (pilot at 50%) |
| Revenue | Sₙ × Pₙ + new schools × ₹25,000 (no fee in Y1) |
| Cash collected | 0.9 × Revenueₙ + 0.1 × Revenueₙ₋₁ |
| Cloud (₹/yr) | 12 × 94 × (700 + 0.017 × Sₙ) — fitted to §4: $700 fixed (non-prod, HA floor, tooling) + $0.017 per student-month; reproduces $1,550/month at 50,000 |
| Messaging | ₹6 × Sₙ |
| Customer success | ⌈schools ÷ 20⌉ FTE in Y1–Y2, ⌈schools ÷ 30⌉ from Y3; ₹8 lakh CTC × 1.08 loading × 1.08^(n−1) |
| COGS | cloud + messaging + customer success |
| R&D | engineering FTE × ₹23 lakh × 1.08 × 1.08^(n−1); engineering FTE Y1–Y5 (**assumption**): conservative 9/10/10/10/10, base 9/12/16/20/24, aggressive 10/16/24/32/40 |
| Sales & marketing | ⌈new schools ÷ 15⌉ reps × ₹7.2 lakh × 1.08 × 1.08^(n−1) + head of sales ₹30 lakh × 1.08 × 1.08^(n−1) + ₹10 lakh brand + ₹15,000 × new schools |
| G&A | ₹15 lakh × 1.2^(n−1) (CA/CS/audit/legal/compliance) + ₹1.14 lakh × headcount (co-working + tools) + n G&A staff × ₹8 lakh × 1.08 × 1.08^(n−1) + founders 2 × (₹12 lakh in Y1–Y2, ₹30 lakh from Y3) × 1.08 |
| Heads | engineering FTE + customer-success FTE + sales reps (minimum 1) + head of sales + 2 founders + n G&A staff |
| EBITDA | Revenue − COGS − R&D − S&M − G&A |
| Cash flow | Cash collected − COGS − R&D − S&M − G&A |

Salary benchmarks behind the ₹23 lakh blended engineering cost: Indian product companies pay backend engineers ₹20–32 lakh at 3 years and ₹22–40 lakh at 5 years; Java seniors (8–11 yrs) ₹38–62 lakh; Bangalore average ₹22–32 lakh, Pune ₹18–27 lakh ([Instahyre, May 2026](https://resources.instahyre.com/blog/backend-developer-salary-in-india/)). Customer-success managers commonly earn ₹8–22 lakh ([Wisemonk, Oct 2026](https://www.wisemonk.io/blogs/best-cities-india-startup-customer-success-hiring.md)). Domestic school field-sales pay is an **assumption** (₹6 lakh fixed + 20% variable); published references cover SaaS SDRs selling globally at ₹7–15 lakh and cross-industry account executives at a ₹1.8–6.9 lakh base ([eChai / PayScale](https://echai.ventures/gtm/gtm-roles-and-paths/what-does-a-gtm-career-actually-pay-in-india-over-ten-years)). Loading of 8% for group health insurance, gratuity provision and payroll tools is an **assumption** (CTC already includes employer PF).

### 5.2 Scenarios at ₹400 per student

**Conservative** — growth stalls at 50,000 students. First EBITDA-positive year: none within 5 years; deepest cumulative cash: **−₹18.84 crore** (keeps falling).

| Year | Students (schools) | Price ₹ | Revenue ₹ cr | Cloud ₹ lakh (₹/student) | COGS ₹ cr | Gross margin | Heads | R&D ₹ cr | S&M ₹ cr | G&A ₹ cr | EBITDA ₹ cr | Cash flow ₹ cr | Cumulative ₹ cr |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Y1 FY2027-28 | 5,000 (10) | 200 | 0.10 | 8.9 (177) | 0.18 | −78% | 15 | 2.24 | 0.52 | 0.67 | −3.50 | −3.51 | −3.51 |
| Y2 FY2028-29 | 30,000 (60) | 400 | 1.33 | 13.6 (45) | 0.43 | 67% | 22 | 2.68 | 0.86 | 0.88 | −3.53 | −3.65 | −7.16 |
| Y3 FY2029-30 | 50,000 (100) | 420 | 2.21 | 17.5 (35) | 0.61 | 73% | 24 | 2.90 | 0.91 | 1.44 | −3.64 | −3.73 | −10.89 |
| Y4 FY2030-31 | 50,000 (100) | 441 | 2.23 | 17.5 (35) | 0.64 | 71% | 22 | 3.13 | 0.62 | 1.59 | −3.75 | −3.76 | −14.64 |
| Y5 FY2031-32 | 50,000 (100) | 463 | 2.34 | 17.5 (35) | 0.68 | 71% | 23 | 3.38 | 0.66 | 1.81 | −4.18 | −4.20 | −18.84 |

If this scenario materialises, the company must shrink to ~6–8 people (revenue ₹2.3 crore − COGS ₹0.7 crore leaves ~₹1.6 crore for all other costs) or be sold.

**Base** — 50k → 150k → 300k → 500k. First EBITDA-positive year: **Y5**; deepest cumulative cash: **−₹12.23 crore** (end of Y4).

| Year | Students (schools) | Price ₹ | Revenue ₹ cr | Cloud ₹ lakh (₹/student) | COGS ₹ cr | Gross margin | Heads | R&D ₹ cr | S&M ₹ cr | G&A ₹ cr | EBITDA ₹ cr | Cash flow ₹ cr | Cumulative ₹ cr |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Y1 FY2027-28 | 5,000 (10) | 200 | 0.10 | 8.9 (177) | 0.18 | −78% | 15 | 2.24 | 0.52 | 0.67 | −3.50 | −3.51 | −3.51 |
| Y2 FY2028-29 | 50,000 (100) | 400 | 2.23 | 17.5 (35) | 0.67 | 70% | 29 | 3.22 | 1.17 | 0.96 | −3.79 | −4.01 | −7.51 |
| Y3 FY2029-30 | 150,000 (300) | 420 | 6.83 | 36.7 (24) | 1.46 | 79% | 46 | 4.64 | 2.06 | 1.69 | −3.03 | −3.49 | −11.00 |
| Y4 FY2030-31 | 300,000 (600) | 441 | 14.05 | 65.4 (22) | 3.01 | 79% | 69 | 6.26 | 3.16 | 2.13 | −0.50 | −1.22 | −12.23 |
| Y5 FY2031-32 | 500,000 (1,000) | 463 | 24.30 | 103.8 (21) | 5.33 | 78% | 97 | 8.11 | 4.51 | 2.65 | 3.69 | 2.67 | −9.56 |

**Aggressive** — 100k → 300k → 600k → 1M. First EBITDA-positive year: **Y4**; deepest cumulative cash: **−₹11.04 crore**.

| Year | Students (schools) | Price ₹ | Revenue ₹ cr | Cloud ₹ lakh (₹/student) | COGS ₹ cr | Gross margin | Heads | R&D ₹ cr | S&M ₹ cr | G&A ₹ cr | EBITDA ₹ cr | Cash flow ₹ cr | Cumulative ₹ cr |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Y1 FY2027-28 | 10,000 (20) | 200 | 0.20 | 9.8 (98) | 0.19 | 5% | 17 | 2.48 | 0.61 | 0.69 | −3.77 | −3.79 | −3.79 |
| Y2 FY2028-29 | 100,000 (200) | 400 | 4.46 | 27.1 (27) | 1.26 | 72% | 44 | 4.29 | 1.81 | 1.13 | −4.04 | −4.47 | −8.26 |
| Y3 FY2029-30 | 300,000 (600) | 420 | 13.65 | 65.4 (22) | 2.85 | 79% | 78 | 6.95 | 3.65 | 2.06 | −1.86 | −2.78 | −11.04 |
| Y4 FY2030-31 | 600,000 (1,200) | 441 | 28.11 | 123.0 (20) | 5.94 | 79% | 123 | 10.01 | 5.81 | 2.74 | 3.60 | 2.15 | −8.88 |
| Y5 FY2031-32 | 1,000,000 (2,000) | 463 | 48.61 | 199.7 (20) | 10.47 | 78% | 177 | 13.52 | 8.48 | 3.56 | 12.57 | 10.52 | 1.64 |

Cloud cost per student falls from ₹35 to ~₹20 as the fixed floor is spread; at 300,000+ students the database moves to the *safe* configuration or larger, which the linear formula absorbs (check: at 300,000 students the formula gives $5,800/month; a bottom-up estimate with a 16 vCPU/64 GiB HA primary, ~9.7 TiB egress and ~6 TiB of files gives ≈ $5,500).

### 5.3 Price sensitivity (same volumes, team and costs)

| Price ₹/student/yr | Scenario | Y2 gross margin | Y5 gross margin | Y5 revenue ₹ cr | Y5 EBITDA ₹ cr | First EBITDA-positive year | Deepest cumulative cash ₹ cr |
|---:|---|---:|---:|---:|---:|---|---:|
| 250 | Conservative | 50% | 54% | 1.47 | −5.05 | none in 5 yrs | −21.72 |
| 250 | Base | 55% | 66% | 15.62 | −4.99 | none in 5 yrs | −25.48 |
| 250 | Aggressive | 57% | 66% | 31.24 | −4.79 | none in 5 yrs | −30.21 |
| 300 | Conservative | 58% | 62% | 1.76 | −4.76 | none in 5 yrs | −20.76 |
| 300 | Base | 61% | 71% | 18.51 | −2.09 | none in 5 yrs | −20.17 |
| 300 | Aggressive | 63% | 72% | 37.03 | 0.99 | Y5 | −19.60 |
| **400** | **Base** | **70%** | **78%** | **24.30** | **3.69** | **Y5** | **−12.23** |
| 400 | Aggressive | 72% | 78% | 48.61 | 12.57 | Y4 | −11.04 |
| 600 | Base | 79% | 85% | 35.88 | 15.27 | Y3 | −7.12 |
| 600 | Aggressive | 80% | 85% | 71.76 | 35.72 | Y3 | −6.36 |

(₹400 and ₹600 conservative rows: −₹4.18 / −₹3.03 crore Y5 EBITDA, no break-even.) The model is far more sensitive to **price** and **support cost per school** than to cloud cost: doubling the cloud bill at 50,000 students moves gross margin by ~8 points; cutting price from ₹400 to ₹300 moves it by ~9 points and pushes break-even out of the 5-year window.

---

## 6. Funding plan

### 6.1 Market context (sourced)

| Fact | Value | Source |
|---|---|---|
| Indian tech funding 2025 | US$10.5 B, −17% vs 2024; seed-stage US$1.1 B, −30% vs 2024; early-stage US$3.9 B, +7% | [Tracxn via The Week, 18 Dec 2025](https://www.theweek.in/wire-updates/business/2025/12/18/dcm91-biz-tech-startups-funding.html) |
| Median seed ticket, H1 2025 | US$1 M (215 deals); growth-stage (Series A/B) median US$8 M | [Inc42 H1 2025 Funding Report](https://asset.inc42.com/2025/07/H1-2025-Funding-Report_v6.pdf) |
| Indian edtech funding | US$4.3 B in 2021 (368 rounds) → **US$214 M in the first eight months of 2026 (36 rounds)**; median round US$1.1 M | [Tracxn via Entrepreneur India, 4 Sep 2026](https://india.entrepreneur.com/?p=99335) |
| Typical seed dilution | 15–20% | [eChai](https://echai.ventures/startingup/seed-fundraising/how-do-i-actually-value-a-pre-revenue-startup-and-where-does-the-seed-number-come-from) |
| Founder stake after Series A (examples) | 29–54% combined across ten startups | [Entrackr](https://entrackr.com/report/startup-founders-lose-grip-amid-heavy-series-a-dilutions-10492468) |

Implication: position MySMS as **B2B vertical SaaS (school operations)**, not consumer edtech; show revenue per school, retention and gross margin, not downloads.

### 6.2 Recommended raise

| Round | Amount | Timing | Instrument / investors | Uses | Milestones to unlock the next round |
|---|---|---|---|---|---|
| Pre-seed | **₹3.0 crore** (~US$0.32 M); **assumption:** 10–15% dilution | Close Jan–Mar 2027 | CCPS or iSAFE from angels/angel networks; SISFS grant (≤ ₹20 lakh) via an incubator | Close the pilot-scope gaps among the 340 unimplemented operations, DPDP/DPA templates, 5–10 pilot schools live for April 2027 | ≥ 5 schools live; ≥ 60% of teachers taking attendance in-app weekly; ≥ 30 signed LOIs/contracts for AY 2028-29 at ≥ ₹350/student |
| Seed | **₹8.5 crore** (~US$0.9 M, just under the US$1 M median seed ticket); 15–20% dilution | Close Oct–Dec 2027 | Institutional seed fund; Google for Startups Scale credits after close | Sales team (6 reps + head), implementation team, ISO 27001, AY 2028-29 rollout | 50,000 students live; ₹2 crore ARR; GRR ≥ 88%; support ≤ 1 FTE per 20 schools; ≥ 99.9% measured uptime |
| Series A | ₹30–45 crore (US$3.2–4.8 M); **assumption:** 20–25% | H2 2029 | VC Series A | Scale to 300,000+ students, second region, platform features | 150,000 students; ₹6–7 crore ARR; NRR ≥ 100%; CAC payback ≤ 12 months |

Illustrative founder ownership (**assumption**): 100% → 90.0% after a 10% ESOP pool → 79.2% after 12% pre-seed → 65.3% after 17.5% seed → 49.0–52.3% after 20–25% Series A.

### 6.3 Non-dilutive and adjacent money (upside only; not in the cash plan)

| Programme | Amount | Conditions / status | Source |
|---|---|---|---|
| Startup India Seed Fund Scheme (SISFS) | Grant ≤ ₹20 lakh (proof of concept, prototype, trials); convertible/debt ≤ ₹50 lakh (market entry, scaling) | DPIIT-recognised, incorporated ≤ 2 years at application, via an approved incubator; ₹945 crore outlay | [India Science & Technology portal](https://www.indiascienceandtechnology.gov.in/funding-opportunities/startups/startup-india-seed-fund-scheme-sisfs) |
| Karnataka ELEVATE | Grant up to ₹50 lakh | 25th round opened 25 May 2026; state registration rules to be confirmed | [NewsBytes, 20 May 2026](https://www.newsbytesapp.com/news/business/karnatakas-elevate-opens-25th-round-may-25-offering-50l-grants/tldr) |
| MeitY SAMRIDH | Matching funding up to ₹40 lakh alongside private investment, via accelerators | 373 startups accelerated, ₹93.75 crore disbursed (Feb 2026) | [Tribune / ANI](https://www.tribuneindia.com/news/business/samridh-scheme-boosts-373-indian-startups-as-government-disburses-over-rs-93-crore-in-funding/) |
| MeitY TIDE 2.0, Kerala (KSUM), Telangana (T-Hub), Maharashtra state schemes | Not verified today | Check current calls before counting | — |
| Google for Startups Cloud | ~₹12 lakh of usable value (§4.5) | Requires verifiable funding | [Google](https://cloud.google.com/startup) |
| CSR for classroom devices | Companies with net profit ≥ ₹5 crore (or net worth ≥ ₹500 crore or turnover ≥ ₹1,000 crore) must spend ≥ 2% of 3-year average net profit; Schedule VII includes promoting education | Sponsor-funded devices, not MySMS revenue | [TaxGuru on s.135](https://taxguru.in/company-law/section-135-corporate-social-responsibility-companies-act2013.html) |
| Revenue-based financing | **Assumption:** available only after 6–12 months of recurring collections; school revenue is seasonal (April–June) which limits monthly-repayment products | Consider after FY2028-29 collections | — |

### 6.4 24-month cash plan (M1 = January 2027; ₹ lakh; GST excluded)

**Team plan**

| Role | CTC ₹ lakh/yr | Loaded ₹/month | Starts | Heads |
|---|---:|---:|---|---:|
| Founder – CEO (sales, schools) | 12 | 1,08,000 | M1 | 1 |
| Founder – CTO | 12 | 1,08,000 | M1 | 1 |
| Senior backend engineer (Java) | 30 | 2,70,000 | M1 | 1 |
| Backend engineer (Java) | 22 | 1,98,000 | M1 | 1 |
| Backend engineer (Java) | 22 | 1,98,000 | M4 | 1 |
| Frontend engineer (React) | 20 | 1,80,000 | M1 | 1 |
| Mobile engineer (React Native) | 22 | 1,98,000 | M1 | 1 |
| QA automation engineer | 12 | 1,08,000 | M2 | 1 |
| Product designer (0.5 FTE contract) | 8 | 72,000 | M1–M12 | 0.5 |
| Product designer | 16 | 1,44,000 | M13 | 1 |
| DevOps / SRE engineer | 25 | 2,25,000 | M7 | 1 |
| Implementation / customer success #1 | 8 | 72,000 | M3 | 1 |
| Implementation / customer success #2 | 8 | 72,000 | M10 | 1 |
| Implementation / customer success #3–#5 | 24 (3 × 8) | 2,16,000 | M14 | 3 |
| Support L1 | 10 (2 × 5) | 90,000 | M15 | 2 |
| Field sales executive #1 (fixed + 20% variable) | 7.2 | 64,800 | M1 | 1 |
| Field sales executives #2–#3 | 14.4 | 1,29,600 | M7 | 2 |
| Field sales executives #4–#6 | 21.6 | 1,94,400 | M10 | 3 |
| Head of sales | 30 | 2,70,000 | M9 | 1 |
| Backend engineer (Java) #4 | 22 | 1,98,000 | M13 | 1 |
| Frontend engineer #2 | 20 | 1,80,000 | M13 | 1 |
| Finance / HR / operations executive | 8 | 72,000 | M7 | 1 |

Loaded = CTC ÷ 12 × 1.08. Engineering CTCs sit inside the Instahyre product-company bands quoted in §5.1; a Bengaluru team costs ~20% more than Pune at the same level (city-average midpoints ₹27 vs ₹22.5 lakh from ranges ₹22–32 vs ₹18–27 lakh).

**Other spend assumptions:** hiring + laptop ₹1.4 lakh per new hire; tools (Workspace, GitHub, CRM, helpdesk) ₹2,500 per head per month; co-working ₹10,000 per seat for 70% of heads (hybrid); sales & marketing programmes (travel, demos, events) ₹1.5 lakh/month to M12 and ₹3 lakh/month after; CA/CS/bookkeeping ₹40,000/month; one-offs: incorporation, trademark, founder and ESOP documents ₹3 lakh (M1), DPDP counsel and school MSA/DPA ₹8 lakh (M2–M6), pen test web + API + Android + iOS ₹3.3 lakh (M8, M20), ISO 27001 ₹3 lakh (M9) and ₹0.7 lakh surveillance (M21), seed legal ₹5 lakh (M10), cyber insurance ₹1.5 lakh (M12, **assumption**). Messaging: ₹3,000/month M4–M15 (pilot), ₹25,310/month (§4.8) from M16. Cloud: $400/month to M3, $800 (lean pilot) M4–M12, $1,000 M13–M15, $1,551 (recommended) from M16. Collections: pilot fees ₹10 lakh (5,000 × ₹200) in May–June 2027; implementation fees 90 × ₹25,000, half at signing (Jan–Mar 2028), half at go-live; ₹2 crore of annual subscriptions collected 50/30/20 over three quarters from April 2028.

**Monthly burn**

| M | Month | Heads | Payroll | Cloud | Other spend* | Total spend | Collections | Net | Cumulative |
|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | Jan 2027 | 7.5 | 11.99 | 0.38 | 16.81 | 29.18 | 0.00 | −29.18 | −29.18 |
| 2 | Feb 2027 | 8.5 | 13.07 | 0.38 | 5.71 | 19.15 | 0.00 | −19.15 | −48.33 |
| 3 | Mar 2027 | 9.5 | 13.79 | 0.38 | 5.80 | 19.97 | 0.00 | −19.97 | −68.29 |
| 4 | Apr 2027 | 10.5 | 15.77 | 0.75 | 5.93 | 22.45 | 0.00 | −22.45 | −90.74 |
| 5 | May 2027 | 10.5 | 15.77 | 0.75 | 4.53 | 21.05 | 5.00 | −16.05 | −106.79 |
| 6 | Jun 2027 | 10.5 | 15.77 | 0.75 | 4.53 | 21.05 | 5.00 | −16.05 | −122.84 |
| 7 | Jul 2027 | 14.5 | 20.03 | 0.75 | 8.91 | 29.69 | 0.00 | −29.69 | −152.53 |
| 8 | Aug 2027 | 14.5 | 20.03 | 0.75 | 6.61 | 27.39 | 0.00 | −27.39 | −179.92 |
| 9 | Sep 2027 | 15.5 | 22.73 | 0.75 | 7.80 | 31.29 | 0.00 | −31.29 | −211.21 |
| 10 | Oct 2027 | 19.5 | 25.40 | 0.75 | 14.38 | 40.53 | 0.00 | −40.53 | −251.75 |
| 11 | Nov 2027 | 19.5 | 25.40 | 0.75 | 3.78 | 29.93 | 0.00 | −29.93 | −281.68 |
| 12 | Dec 2027 | 19.5 | 25.40 | 0.75 | 5.28 | 31.43 | 0.00 | −31.43 | −313.11 |
| 13 | Jan 2028 | 22.0 | 29.90 | 0.94 | 9.72 | 40.56 | 3.75 | −36.81 | −349.92 |
| 14 | Feb 2028 | 25.0 | 32.06 | 0.94 | 10.01 | 43.00 | 3.75 | −39.25 | −389.17 |
| 15 | Mar 2028 | 27.0 | 32.96 | 0.94 | 8.79 | 42.69 | 3.75 | −38.94 | −428.11 |
| 16 | Apr 2028 | 27.0 | 32.96 | 1.46 | 6.22 | 40.63 | 38.96 | −1.68 | −429.79 |
| 17 | May 2028 | 27.0 | 32.96 | 1.46 | 6.22 | 40.63 | 38.96 | −1.68 | −431.47 |
| 18 | Jun 2028 | 27.0 | 32.96 | 1.46 | 6.22 | 40.63 | 33.33 | −7.30 | −438.77 |
| 19 | Jul 2028 | 27.0 | 32.96 | 1.46 | 6.22 | 40.63 | 20.00 | −20.63 | −459.40 |
| 20 | Aug 2028 | 27.0 | 32.96 | 1.46 | 9.52 | 43.93 | 20.00 | −23.93 | −483.33 |
| 21 | Sep 2028 | 27.0 | 32.96 | 1.46 | 6.92 | 41.33 | 20.00 | −21.33 | −504.67 |
| 22 | Oct 2028 | 27.0 | 32.96 | 1.46 | 6.22 | 40.63 | 13.33 | −27.30 | −531.97 |
| 23 | Nov 2028 | 27.0 | 32.96 | 1.46 | 6.22 | 40.63 | 13.33 | −27.30 | −559.27 |
| 24 | Dec 2028 | 27.0 | 32.96 | 1.46 | 6.22 | 40.63 | 13.33 | −27.30 | −586.57 |

\*Other spend = hiring + laptops + messaging + tools + co-working + sales & marketing programmes + legal/finance/security.

**Quarterly view**

| Quarter | Heads (end) | Payroll | Hiring + laptops | Cloud | Messaging | Tools + co-working | S&M programmes | Legal, finance, security | Total spend | Collections | Net | Cumulative |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Q1 Jan–Mar 2027 | 9.5 | 38.84 | 14.00 | 1.13 | 0.00 | 2.42 | 4.50 | 7.40 | 68.29 | 0.00 | −68.29 | −68.29 |
| Q2 Apr–Jun 2027 | 10.5 | 47.30 | 1.40 | 2.26 | 0.09 | 2.99 | 4.50 | 6.00 | 64.54 | 10.00 | −54.54 | −122.84 |
| Q3 Jul–Sep 2027 | 15.5 | 62.80 | 7.00 | 2.26 | 0.09 | 4.23 | 4.50 | 7.50 | 88.38 | 0.00 | −88.38 | −211.21 |
| Q4 Oct–Dec 2027 | 19.5 | 76.19 | 5.60 | 2.26 | 0.09 | 5.56 | 4.50 | 7.70 | 101.90 | 0.00 | −101.90 | −313.11 |
| Q5 Jan–Mar 2028 | 27.0 | 94.91 | 11.20 | 2.82 | 0.09 | 7.03 | 9.00 | 1.20 | 126.25 | 11.25 | −115.00 | −428.11 |
| Q6 Apr–Jun 2028 | 27.0 | 98.87 | 0.00 | 4.37 | 0.76 | 7.70 | 9.00 | 1.20 | 121.90 | 111.25 | −10.65 | −438.77 |
| Q7 Jul–Sep 2028 | 27.0 | 98.87 | 0.00 | 4.37 | 0.76 | 7.70 | 9.00 | 5.20 | 125.90 | 60.00 | −65.90 | −504.67 |
| Q8 Oct–Dec 2028 | 27.0 | 98.87 | 0.00 | 4.37 | 0.76 | 7.70 | 9.00 | 1.20 | 121.90 | 40.00 | −81.90 | −586.57 |
| **24 months** | | **616.68** | **39.20** | **23.84** | **2.64** | **45.31** | **54.00** | **37.40** | **819.07** | **232.50** | **−586.57** | |

**Reconciliation to the raise.** Bottom-up net burn: ₹4.39 crore by June 2028 (18 months), ₹5.87 crore by December 2028 (24 months). January–March 2029 is a selling quarter with no subscription collections: ~₹1.22 crore of spend less ~₹0.25 crore of implementation-fee advances adds ≈ ₹0.97 crore, giving ≈ ₹6.8 crore by March 2029. The top-down base model gives ₹7.51 crore for FY2027-28 + FY2028-29, plus ₹0.68 crore for January–March 2027 = ₹8.19 crore (it carries a larger FY2028-29 team). Raising **₹11.5 crore** therefore leaves a 40–70% buffer (11.5 ÷ 8.19 = 1.40; 11.5 ÷ 6.84 = 1.68). It does **not** cover a full one-season slip (one extra year of burn ≈ ₹4–5 crore); if the January–March 2028 season under-delivers, cut hiring from M13 immediately (decision gate G3). The ₹3.0 crore pre-seed covers cumulative burn to November 2027 (₹2.82 crore); burn reaches ₹3.13 crore at the end of December 2027, so a December seed close needs a ~₹13 lakh bridge (e.g. the SISFS grant) or an earlier first seed drawdown.

**Cloud is 2.9% of 24-month spend** (₹23.84 lakh of ₹819 lakh). The draft's 18-month cloud line of ₹0.54–1.62 crore is replaced by **₹0.15 crore** (sum of Q1–Q6 cloud).

---

## 7. Starting and running the company in India

This section is a checklist with sourced facts; it is not legal or tax advice. Engage a startup CA/CS and a technology lawyer before incorporation.

### 7.1 Entity and structure

| Decision | Recommendation | Why |
|---|---|---|
| Pvt Ltd vs LLP | **Private limited company** | Institutional investors subscribe to shares/CCPS and ESOPs require a company (**assumption**: standard Indian VC practice); an India-focused B2B SaaS should "incorporate a Pvt Ltd in India" ([eChai](https://echai.ventures/startingup/seed-fundraising/where-and-how-should-i-incorporate-to-raise-and-does-the-delaware-flip-still-make-sense-for-indian-founders)). DPIIT recognition is open to Pvt Ltd, LLP, partnership and cooperative society ([Startup India](https://www.startupindia.gov.in/content/sih/en/startupgov/startup_recognition_page.html)). |
| Delaware / Singapore parent | **Do not flip.** Revisit only if > 20% of revenue comes from outside India or a US listing is planned | Flip structures add ODI filings, a 400% net-worth ceiling, Form FC, annual performance reports and transfer-pricing exposure ([eChai](https://echai.ventures/startingup/seed-fundraising/where-and-how-should-i-incorporate-to-raise-and-does-the-delaware-flip-still-make-sense-for-indian-founders)). Coming back is expensive: Groww paid **US$159.4 M (₹1,340 crore)** in US exit tax under IRC §367 to re-domicile to India ([Treelife](https://treelife.in/case-studies/how-growws-160-million-delaware-tax-bill-became-indias-most-expensive-startup-lesson/)). |
| Incorporation cost | **₹7,000–25,000** for 2 directors and ₹1 lakh authorised capital | SPICe+ filing fee is zero up to ₹15 lakh authorised capital; name reservation ₹1,000; DSC ₹1,500–2,500 per director; Karnataka stamp duty ~₹1,000–2,000; PAN/TAN allotted free; professional fees ₹5,000–15,000 ([Patron Accounting](https://www.patronaccounting.com/blog/private-limited-company-registration-cost-breakdown-government-fees)) |
| Trademark | ₹4,500 TM-A e-filing fee for a start-up/small enterprise (₹9,000 for others) | [S.S. Rana](https://ssrana.in/ip-laws/trademarks-in-india/tm-filing-fees-forms-india/); file "MySMS" in software (class 9) and SaaS (class 42) before launch; check availability first |
| ESOP pool | 10% before pre-seed (**assumption**) | Needed to hire senior engineers below the Instahyre product-company bands |
| Founder agreement | Vesting (4 years, 1-year cliff), IP assignment to the company, roles, deadlock clause (**assumption**: standard terms) | Investors will require it at pre-seed |

### 7.2 DPIIT recognition and tax position

| Item | Position on 10 Nov 2026 | Source |
|---|---|---|
| DPIIT recognition | Free; startup status for 10 years from incorporation; turnover < ₹200 crore in every year | [Startup India](https://www.startupindia.gov.in/content/sih/en/startupgov/startup_recognition_page.html) |
| Tax holiday (old s.80-IAC) | 100% of eligible profits for any 3 consecutive years out of the first 10; incorporated before **1 April 2030**; turnover ≤ ₹100 crore; requires a separate **Inter-Ministerial Board** certificate (DPIIT recognition alone is not enough) | [TaxAJ](https://www.taxaj.com/learn/section-80-iac-extended-to-startups-incorporated-up-to-april-2030/) |
| Same holiday under the Income-tax Act, 2025 | Clause 140 of the Income-tax Bill, 2025 is the counterpart of s.80-IAC with substantially identical conditions | [TaxTMI](https://www.taxtmi.com/tmi_notes?id=1599); confirm enacted section number with the CA |
| Angel tax (s.56(2)(viib)) | Abolished from FY2025-26 (1 April 2025) | [India Briefing](https://india-briefing.com/news/abolishing-the-angel-tax-in-india-applicable-for-fy-2025-26-35289.html). Valuation reports are still needed for FEMA pricing if any investor is foreign (**assumption**: CA to confirm). |
| Practical value of 80-IAC to MySMS | Low in years 1–4 (no taxable profit in the base case); potentially material in FY2031-32 onward | §5 |

### 7.3 GST, TDS and registrations

| Item | Position |
|---|---|
| GST registration | Register voluntarily from incorporation so GST on cloud, laptops, co-working and tools is recoverable as ITC (**assumption**: CA to confirm threshold applicability). |
| GST on MySMS invoices to schools | **18% applies; the education exemption does not cover a SaaS vendor.** The services *to* a school exempt under entry 66(b) of Notification 12/2017-CT(Rate) are transport, catering, security/cleaning/housekeeping, services relating to admission or conduct of examinations, and online educational journals ([Logitax summary](https://logitax.in/resources/gst_on_various_services_by_or_to_educational_institutions)). The Gujarat AAAR (7 Sep 2021, *Educational Initiatives Pvt Ltd*) denied the exemption to an assessment service conducted by the vendor rather than the school ([ruling](https://busy.in/caselaw/educational-initiative-pvt-ltd-appellate-authority-for-advance-ruling-gujrat)). Most schools (trusts/societies) cannot claim ITC, so **quote "₹400 + GST"** — a GST-inclusive ₹400 is only ₹339 of revenue. |
| TDS on MySMS receipts | Schools may deduct TDS on payments to MySMS; this is a cash-timing item recoverable against income tax (section and rate under the Income-tax Act, 2025 to be confirmed by the CA). |
| EPF | Applies at 20+ employees; 12% employer + 12% employee, mandatory up to the ₹15,000/month wage ceiling ([Labour Law Reporter](https://labourlawreporter.com/epf.asp)). Plan for it from M13 (headcount 22). |
| Labour codes | The four labour codes took effect on **21 November 2025**; a single wage definition now drives PF, gratuity and leave encashment; fixed-term employees earn gratuity after 1 year; final wages within 2 working days of exit; appointment letters for all staff ([KPMG](https://kpmg.com/in/en/blogs/2025/12/implementation-of-labour-codes-what-changes-and-road-ahead.html)). The 8% loading in §6.4 covers gratuity provision. |
| ESI, professional tax, Shops & Establishments | Required depending on state and pay levels; ESI is unlikely to cover engineering salaries (above the wage ceiling); amounts not verified today — obtain from the payroll provider. |

### 7.4 Children's data: DPDP Act 2023 and Rules 2025

| Item | Position | Source |
|---|---|---|
| Rules notified | 14 November 2025 | [BDO](https://www.bdo.in/en-gb/news/2025/digital-personal-data-protection-rules-2025-%E2%80%94-check-provisions,-implementation-plan,-penalties), [SAM & Co alert](https://www.amsshardul.com/wp-content/uploads/2025/11/Regulatory-Alert-Enforcement-of-DPDP-Act-and-Notification-of-DPDP-Rules.pdf) |
| Phasing | Board immediately; Consent Manager registration from **14 Nov 2026**; notice and consent, breach reporting, security safeguards, **verifiable consent for children**, SDF obligations, data-principal rights and cross-border rules from **14 May 2027** — i.e. during the first pilot academic year | SAM & Co alert |
| Child | Anyone under 18; verifiable parent/guardian consent required; no tracking, behavioural monitoring or targeted advertising of children (s.9) | [DPDP Act](https://www.meity.gov.in/static/uploads/2024/02/Digital-Personal-Data-Protection-Act-2023.pdf) |
| Schools' exemption | Fourth Schedule exempts educational institutions from s.9 obligations when processing children's data for educational activities and safety; a B2B platform acting **solely as a data processor** for the school shifts the Rule 10 consent duty to the school as data fiduciary | [LiveLaw](https://www.livelaw.in/articles/dpdp-child-data-rules-demand-edtech-platforms-555317) |
| Design consequence for MySMS | Contract as **data processor** under a Data Processing Agreement with each school; keep any parent-facing B2C feature (marketing, paid add-ons) out until verifiable-consent flows (Rule 10, e.g. DigiLocker token) are built | — |
| Log retention | All data fiduciaries must retain personal data, traffic data and processing logs for at least **one year** for Seventh-Schedule purposes; grievances resolved within 90 days | SAM & Co alert. Cost: exporting ~100 GiB/month of logs to Coldline for a year ≈ $7/month. |
| Significant Data Fiduciary | Possible designation by volume/sensitivity; then DPO based in India, independent data auditor, periodic DPIA and audits | DPDP Act s.10 |
| Maximum penalties | ₹250 crore (security safeguards); ₹200 crore (breach notification); ₹200 crore (children's obligations); ₹150 crore (SDF obligations); ₹50 crore (any other provision) | DPDP Act Schedule |

### 7.5 Security certification and insurance

| Item | Cost | Source |
|---|---|---|
| ISO 27001, 10–50 employees, year 1 | ₹1.7–3 lakh (consulting ₹1–2 lakh + certification audit ₹0.7–1 lakh); surveillance ₹60,000–80,000/yr | [TCSA](https://www.tcsa.in/resources/iso-27001-cost-india-2026) (vendor figures) |
| SOC 2 Type II | ₹3–5 lakh (vendor price) / ₹5–10 lakh market | Same |
| Pen test web + API + Android + iOS | ₹3.3 lakh + taxes (vendor plan); market ₹50,000–3 lakh per scope | [Cybersecify](https://cybersecify.pages.dev/blog/penetration-testing-cost-india-2026/) |
| Cyber insurance | ₹1.5 lakh/yr | **Assumption**; get broker quotes |

ISO 27001 is the more common ask from Indian school groups (**assumption**); SOC 2 becomes relevant for international customers. Both are scheduled in §6.4.

### 7.6 How schools buy

* Market: **14,66,682** recognised schools, of which **3,41,689** (23.3%) are private unaided; private-unaided schools hold ~40% of enrolment (UDISE+ 2025-26, [analysis](https://educationforallinindia.com/private-schools-cross-the-40-enrolment-mark-the-government-to-private-shift-in-udise-2025-26/)). Even 1,000 schools is < 0.3% of the private-unaided segment.
* Buyers are trusts/societies (owner/chairman decides; principal influences). Budgets are set before the academic year; selling happens January–March for April (CBSE) or June starts (**assumption** based on the academic calendar). Payment is annual or termly; DSO of 60–150 days is planned in §5 and §6.4.
* Government schools buy by tender and are out of scope for the plan.

---

## 8. Unit economics (base case, FY2028-29: 50,000 students, 100 schools)

| Metric | Calculation | Value |
|---|---|---|
| Price | | ₹400 + GST per student per year; ₹2.0 lakh per 500-student school |
| Cloud | ₹17.48 lakh ÷ 50,000 | ₹35.0 |
| Messaging | §4.8 | ₹6.0 |
| Customer success | 5 FTE × ₹8 lakh × 1.08 × 1.08 ÷ 50,000 | ₹93.3 |
| **Cost to serve per student** | | **₹134.3 / year** |
| Gross margin (incl. implementation fees) | 1 − ₹0.67 crore ÷ ₹2.23 crore | **70%** (benchmark median 81% subscription GM, [Benchmarkit](https://www.hibob.com/wp-content/uploads/2025-SaaS-Performance-Metrics-Benchmarks.pdf)) |
| CAC per new school | S&M ₹1.17 crore ÷ 91 new schools | ₹1.29 lakh |
| Gross profit per school per year | ₹2.0 lakh × 70% | ₹1.40 lakh |
| **CAC payback** | 1.29 ÷ 1.40 × 12 | **11 months** (rule of thumb: ~12 months is good, Benchmarkit) |
| LTV at 10% annual churn | ₹1.40 lakh ÷ 0.10 | ₹14.0 lakh → LTV/CAC 10.9 |
| LTV at 20% annual churn | ₹1.40 lakh ÷ 0.20 | ₹7.0 lakh → LTV/CAC 5.4 |
| New-ARR efficiency | S&M ₹1.17 crore ÷ new ARR (91 × ₹2.0 lakh = ₹1.82 crore) | ₹0.64 per ₹1 of new ARR vs benchmark median $2.00 per $1 (Benchmarkit) — **the plan's 15 schools per rep per year is optimistic; validate in the 2028 season** |
| Support sensitivity | 1 CS per 10 schools instead of 20 | Gross margin falls from 70% to 49% |

### 8.3 Comparable prices (Indian school ERPs)

| Product | Price signal | Status of figure |
|---|---|---|
| Teachmint | ₹100–300 per student per year | Community estimate, not published ([Fuzen comparison](https://www.fuzen.io/posts/school-management-software-comparison-fuzen-vs-entab-fedena-teachmint)) |
| Fedena Cloud | ~₹4,999/month up to 500 students (≈ ₹120/student/yr); ₹60,000–1,10,000/yr mid-size | "Approximately", same source; G2 lists Fedena Pro Plus at US$1,299/yr ([G2](https://g2.com/products/fedena/pricing)) |
| Entab | Implementation ₹75,000–2,50,000; licence ₹1.5–8 lakh/yr | Estimate from school-admin forums (Fuzen) |
| Vidyalaya, SchoolLog, eSkooly | ₹1.5–4 lakh/yr; ₹30,000–1,50,000/yr; ₹2,500–5,000/month | Quote-based / approximate (Fuzen) |

None of these vendors publishes verifiable per-student prices; treat the table as directional. MySMS's ₹400 is at the top of the signals and must be earned with LMS + apps + implementation + compliance (DPDP-ready, ISO 27001).

---

## 9. Risks and sensitivities

| Risk | Effect | Mitigation |
|---|---|---|
| Price pressure (₹250–300) | Base never breaks even in 5 years; trough −₹20–25 crore (§5.3) | Per-school minimum (₹1.5 lakh), modular pricing (ERP vs LMS), 3-year contracts with 5% escalator |
| Missed sales season | One season slip ≈ ₹4–5 crore extra burn | Hiring gate G3; sign LOIs before October; keep seed buffer |
| Support cost per school | 1 CS per 10 schools → GM 49% | Self-serve onboarding, data-migration tooling, school-admin training videos |
| Product completeness | 340 operations return 501; 98 of 186 screens are placeholders | Scope the pilot to attendance, timetable, homework, fees, communication; close those operations first |
| DPDP non-compliance | Penalties up to ₹200–250 crore; school contracts lost | Processor-only model, DPA, breach runbook, 1-year log retention, ISO 27001 by M9 |
| Connection exhaustion | Monolith with 210 pooled connections exceeds Cloud Run's 100-per-instance limit | Production pool profile in §3.2 before any pilot |
| Video/egress growth | +₹31,400/month per weekly video per student (§4.6) | External video hosting; per-school storage/egress quotas |
| FX | Cloud is ~3% of spend; ±5% FX moves annual cost by < ₹1 lakh | Pay Google Cloud India in INR |
| Edtech funding climate | Edtech rounds fell from 368 (2021) to 36 (Jan–Aug 2026) | Position as vertical B2B SaaS; show retention and margin |
| Key-person risk | Founder-CTO holds the architecture | Senior Java hire at M1; documented runbooks; ESOP |

---

## 10. Decision gates

| Gate | When | Pass criteria | If it fails |
|---|---|---|---|
| G1 Pre-seed close | Mar 2027 | ₹3 crore committed; 5+ pilot schools signed | Founders-only build; apply for SISFS/ELEVATE; delay hires |
| G2 Pilot evidence | Sep 2027 | p95 API < 500 ms at measured peak; DB CPU < 60%; weekly teacher usage ≥ 60%; support tickets per school per month measured; real requests/egress per student replace §3 | Re-size cloud (§4) and fix product before seed |
| G3 Sales season | Mar 2028 | ≥ 70 schools / 35,000 students signed at ≥ ₹350 | Freeze hiring from M13; re-plan to conservative case |
| G4 Voice device | After G3 | Paid or sponsored demand for ≥ 100 rooms; language/region decision made | Keep voice on teacher phones only |
| G5 Series A readiness | H2 2029 | 150,000 students; ₹6–7 crore ARR; NRR ≥ 100%; GM ≥ 75% | Bridge round or cost reduction to break-even at current scale |
| Cloud CUD | 3 months after stable production | Sizing unchanged for 90 days | Stay on demand |

Cost controls that remain from the draft: separate budgets and alerts per environment (alerts do not cap spend — also set max instances and quotas); no student data in public buckets; deliberate log volume (no request bodies); signed URLs for files; local wake-word and ephemeral audio for any voice feature.

---

## 11. Changes from the previous draft

| # | Previous draft | This revision | Why |
|---|---|---|---|
| 1 | Cloud budget ₹2.1–6.5 lakh/month (midpoint ₹4.3 lakh) | **₹0.98 / 1.46 / 3.27 lakh/month** (lean / recommended / safe), every line computed | Recomputed from Mumbai list prices and a stated workload |
| 2 | Cloud SQL 8–16 vCPU/32–64 GiB HA at $1,000–2,500 | **4 vCPU/16 GiB HA, $606.80 with storage and backups**; 8 vCPU/32 GiB is the *safe* option | Workload needs ~4 vCPU at 2× design peak (§3.2) |
| 3 | Regional prices "to be set in the calculator"; FX ₹95 | Exact Mumbai SKUs (Cloud SQL, Cloud Run Tier 1, egress, storage, NAT); FX ₹94 from RBI ₹93.8579 | The draft gave no Mumbai figures |
| 4 | Connection pool flagged as a concern only | **Hard limit identified:** 100 connections per Cloud Run instance vs 210 configured; production profile given | Google Cloud SQL–Cloud Run documentation |
| 5 | Staging, dev, CI/CD, Cloud Armor, NAT, object backup, log retention not costed | Each costed (non-prod $81–273/month) | Missing lines |
| 6 | No egress method; 1–5 TiB assumed | Bottom-up 1,643 GiB with formula and video sensitivity | Required for a defensible number |
| 7 | Recovery/replica allowance $250–800 as a lump | Explicit read replica (+$172.47) and Delhi DR replica ($344.94) | Transparency |
| 8 | CUD, Google for Startups credits, GST/TDS on cloud not quantified | CUD savings tabled; credits tiers and realistic value (~₹12 lakh); GST 0%/18% by contracting entity, ITC; TDS process | Sourced from Google billing docs |
| 9 | Speech-to-Text at $0.024/min (V1, no data logging) | **V2 Standard $0.016/min**; voice cloud ₹0.68–2.75 lakh/month instead of ₹0.9–3.6 lakh | Current V2 price |
| 10 | 18-month cloud line ₹0.54–1.62 crore | **₹0.15 crore** (2.9% of spend) | Follows from corrections 1–2 |
| 11 | No N-year model | Formula-driven 5-year model, 3 scenarios, 4 price points, break-even and cumulative cash | Founder requirement |
| 12 | 18-month raise ₹4.6–8.7 crore as one envelope | **₹11.5 crore in two tranches** (₹3 crore pre-seed, ₹8.5 crore seed) with a monthly 24-month burn table, roster and milestones; Series A sized from the trough | Investor-grade plan |
| 13 | Price points ₹300/₹600/₹900 untested | Market signals ₹100–300 sourced; ₹400 base; shown that ₹250–300 never breaks even | Price is the dominant risk |
| 14 | India setup: entity, DPIIT, SISFS only | Added angel-tax abolition, 80-IAC to 2030 and its Income-tax Act 2025 counterpart, GST 18% on SaaS to schools (exemption does not apply), labour codes, EPF, trademark, Delaware-flip cost, DPDP phasing to 14 May 2027 and penalties, ISO 27001/SOC 2/pen-test costs, UDISE+ market size | Requirement 6 |
| 15 | Voice hardware in the main raise options (₹8–12 crore with devices) | Voice device moved out of the core raise; funded by CSR/school groups after gate G4 | Hardware before product-market fit adds ₹1–1.8 crore of risk |

---

## 12. Sources (all opened 10 November 2026)

**Google Cloud pricing and documentation**
- [Cloud SQL pricing](https://cloud.google.com/sql/pricing) · [Cloud SQL quotas](https://docs.cloud.google.com/sql/docs/postgres/quotas) · [Connect from Cloud Run (100-connection limit)](https://docs.cloud.google.com/sql/docs/postgres/connect-run)
- Mumbai Cloud SQL listings: [Spare Cores db-n1-standard-1](https://sparecores.com/database/gcp/db-n1-standard-1), [Spare Cores db-n1-standard-16](https://sparecores.com/database/gcp/db-n1-standard-16), [Bytebase db-standard-1](https://www.bytebase.com/dbcost/cloudsql/instance/db-standard-1.md), [Bytebase db-highmem-64](https://www.bytebase.com/dbcost/cloudsql/instance/db-highmem-64.md)
- [Cloud Billing Catalog mirror, generated 5 Nov 2026 (Cyclenerd)](https://github.com/Cyclenerd/google-cloud-pricing-cost-calculator)
- [Cloud Run pricing](https://cloud.google.com/run/pricing) · [Cloud Storage pricing](https://cloud.google.com/storage/pricing) · [Network pricing (egress, Load Balancing, Cloud Armor, CDN)](https://cloud.google.com/vpc/network-pricing) · [Pub/Sub pricing](https://cloud.google.com/pubsub/pricing) · [Observability pricing](https://cloud.google.com/stackdriver/pricing) · [Secret Manager pricing](https://cloud.google.com/secret-manager/pricing) · [Artifact Registry pricing](https://cloud.google.com/artifact-registry/pricing) · [Cloud Build pricing](https://cloud.google.com/build/pricing) · [GKE pricing](https://cloud.google.com/kubernetes-engine/pricing)
- [Speech-to-Text pricing](https://cloud.google.com/speech-to-text/pricing) · [Speech-to-Text languages by region](https://docs.cloud.google.com/speech-to-text/docs/speech-to-text-supported-languages) · [Text-to-Speech pricing](https://cloud.google.com/text-to-speech/pricing) · [Gemini pricing](https://cloud.google.com/gemini-enterprise-agent-platform/generative-ai/pricing)
- [Google for Startups Cloud Program](https://cloud.google.com/startup) · [Taxes by country (India GST)](https://docs.cloud.google.com/billing/docs/resources/vat-overview) · [Google Cloud India TDS](https://docs.cloud.google.com/billing/docs/resources/tds)

**FX, market and funding**
- [RBI reference rate](https://www.rbi.org.in/home.aspx)
- [Tracxn 2025 via The Week](https://www.theweek.in/wire-updates/business/2025/12/18/dcm91-biz-tech-startups-funding.html) · [Inc42 H1 2025 Funding Report](https://asset.inc42.com/2025/07/H1-2025-Funding-Report_v6.pdf) · [Tracxn edtech via Entrepreneur India](https://india.entrepreneur.com/?p=99335) · [eChai on seed valuation](https://echai.ventures/startingup/seed-fundraising/how-do-i-actually-value-a-pre-revenue-startup-and-where-does-the-seed-number-come-from) · [Entrackr on Series A dilution](https://entrackr.com/report/startup-founders-lose-grip-amid-heavy-series-a-dilutions-10492468)
- [SISFS](https://www.indiascienceandtechnology.gov.in/funding-opportunities/startups/startup-india-seed-fund-scheme-sisfs) · [Karnataka ELEVATE](https://www.newsbytesapp.com/news/business/karnatakas-elevate-opens-25th-round-may-25-offering-50l-grants/tldr) · [SAMRIDH](https://www.tribuneindia.com/news/business/samridh-scheme-boosts-373-indian-startups-as-government-disburses-over-rs-93-crore-in-funding/) · [CSR s.135](https://taxguru.in/company-law/section-135-corporate-social-responsibility-companies-act2013.html)
- [Benchmarkit 2025 SaaS benchmarks](https://www.hibob.com/wp-content/uploads/2025-SaaS-Performance-Metrics-Benchmarks.pdf) · [UDISE+ 2025-26 analysis](https://educationforallinindia.com/private-schools-cross-the-40-enrolment-mark-the-government-to-private-shift-in-udise-2025-26/)
- [Fuzen ERP comparison](https://www.fuzen.io/posts/school-management-software-comparison-fuzen-vs-entab-fedena-teachmint) · [G2 Fedena pricing](https://g2.com/products/fedena/pricing)

**Salaries and operating costs**
- [Instahyre backend salaries, May 2026](https://resources.instahyre.com/blog/backend-developer-salary-in-india/) · [Wisemonk customer success](https://www.wisemonk.io/blogs/best-cities-india-startup-customer-success-hiring.md) · [eChai GTM pay](https://echai.ventures/gtm/gtm-roles-and-paths/what-does-a-gtm-career-actually-pay-in-india-over-ten-years)
- [MSG91 SMS pricing](https://msg91.com/in/pricing/sms) · [WhatsApp pricing (Meta)](https://developers.facebook.com/docs/whatsapp/pricing) · [WhatsApp India rate card (Gallabox)](https://docs.gallabox.com/pricing-and-billing/whatsapp-pricing/rate-card.md) · [Amazon SES pricing](https://aws.amazon.com/ses/pricing/) · [TRAI sender requirements](https://www.trai.gov.in/advice-to-senders)
- [TCSA ISO 27001 / SOC 2 costs](https://www.tcsa.in/resources/iso-27001-cost-india-2026) · [Cybersecify pen-test costs](https://cybersecify.pages.dev/blog/penetration-testing-cost-india-2026/) · [DoT ETA](https://eservices.dot.gov.in/equipment-type-approval-eta)

**Company law, tax and data protection**
- [Startup India recognition](https://www.startupindia.gov.in/content/sih/en/startupgov/startup_recognition_page.html) · [80-IAC extension to 2030 (TaxAJ)](https://www.taxaj.com/learn/section-80-iac-extended-to-startups-incorporated-up-to-april-2030/) · [Income-tax Bill 2025 clause 140 (TaxTMI)](https://www.taxtmi.com/tmi_notes?id=1599) · [Angel tax abolition (India Briefing)](https://india-briefing.com/news/abolishing-the-angel-tax-in-india-applicable-for-fy-2025-26-35289.html)
- [GST services to educational institutions (Logitax)](https://logitax.in/resources/gst_on_various_services_by_or_to_educational_institutions) · [Gujarat AAAR, Educational Initiatives](https://busy.in/caselaw/educational-initiative-pvt-ltd-appellate-authority-for-advance-ruling-gujrat)
- [Incorporation costs (Patron Accounting)](https://www.patronaccounting.com/blog/private-limited-company-registration-cost-breakdown-government-fees) · [Trademark fees (S.S. Rana)](https://ssrana.in/ip-laws/trademarks-in-india/tm-filing-fees-forms-india/) · [EPF (Labour Law Reporter)](https://labourlawreporter.com/epf.asp) · [Labour codes (KPMG)](https://kpmg.com/in/en/blogs/2025/12/implementation-of-labour-codes-what-changes-and-road-ahead.html)
- [Delaware flip analysis (eChai)](https://echai.ventures/startingup/seed-fundraising/where-and-how-should-i-incorporate-to-raise-and-does-the-delaware-flip-still-make-sense-for-indian-founders) · [Groww exit tax (Treelife)](https://treelife.in/case-studies/how-growws-160-million-delaware-tax-bill-became-indias-most-expensive-startup-lesson/)
- [DPDP Act 2023](https://www.meity.gov.in/static/uploads/2024/02/Digital-Personal-Data-Protection-Act-2023.pdf) · [DPDP Rules alert (SAM & Co)](https://www.amsshardul.com/wp-content/uploads/2025/11/Regulatory-Alert-Enforcement-of-DPDP-Act-and-Notification-of-DPDP-Rules.pdf) · [DPDP Rules (BDO)](https://www.bdo.in/en-gb/news/2025/digital-personal-data-protection-rules-2025-%E2%80%94-check-provisions,-implementation-plan,-penalties) · [Children's data and ed-tech (LiveLaw)](https://www.livelaw.in/articles/dpdp-child-data-rules-demand-edtech-platforms-555317)

**Repository context**
- [ARCHITECTURE.md](../ARCHITECTURE.md) · [DEPLOYMENT.md](../DEPLOYMENT.md) · [STATUS.md](../STATUS.md) · [MAINTAINABILITY-REVIEW.md](MAINTAINABILITY-REVIEW.md) · [CLASSROOM-VOICE-ASSISTANT-BRAINSTORM.md](CLASSROOM-VOICE-ASSISTANT-BRAINSTORM.md)
