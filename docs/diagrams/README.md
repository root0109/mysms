# Diagrams

draw.io files (uncompressed XML); open with [draw.io / diagrams.net](https://app.diagrams.net) or the draw.io editor extension.

**Rule: a diagram changes in the same commit as the code it describes.**

| File | Shows | Audience |
|---|---|---|
| [architecture/system-context.drawio](architecture/system-context.drawio) | Users, web and mobile clients, the backend, PostgreSQL, the event bus and every external system integrated in code | Product, everyone |
| [architecture/modules-and-dependencies.drawio](architecture/modules-and-dependencies.drawio) | Page 1: build modules and allowed Maven dependencies (conformance `ModuleBoundaryRule`). Page 2: the 26 module-to-module gRPC edges, one module per schema | Developers |
| [architecture/deployment.drawio](architecture/deployment.drawio) | Page 1: the monolith. Page 2: one service per module. PostgreSQL primary and replica, database roles, broker | Developers, operations |
| [architecture/request-lifecycle-sequence.drawio](architecture/request-lifecycle-sequence.drawio) | REST and gRPC call: edge filters / interceptors in order, idempotency, `AppTransactionManager` transaction with tenant binding, outbox write, RLS | Developers |
| [architecture/events-outbox-sequence.drawio](architecture/events-outbox-sequence.drawio) | Outbox write, relay pass, bus, inbox exactly once, retries and dead letters | Developers, operations |
| [architecture/background-jobs-sequence.drawio](architecture/background-jobs-sequence.drawio) | Per-tenant scheduled job: enrolment, lease claim, run, completion and back-off (`com.mysms.core.jobs`) | Developers |

The ER diagrams of the database live in [`database/erd/`](../../database/erd/README.md).
