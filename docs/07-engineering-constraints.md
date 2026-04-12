# 7. Engineering & Integration Constraints

## 7.1 Confirmed (from business requirements)

- Bot runs in **Salesforce Enhanced Chat**, deeply integrated with Salesforce.
- Key integration objects: Messaging Session / Chat, Omni-Channel, Salesforce Case, Salesforce Knowledge.
- Phase 1 requires an FAQ vector store with semantic retrieval and version management.
- Must support pilot / controlled rollout with configurable traffic split.
- Reporting required from day one.

## 7.2 Reasonably Inferred

This is an **external chat channel + custom bot backend + Salesforce record system** architecture, not a native Salesforce bot.

The technical design must cover:

- Bot runtime / orchestration
- Intent routing
- FAQ retrieval (semantic)
- Human handover
- Case creation
- Reporting / event logging

## 7.3 Engineering Constraints (derived from current codebase)

The following constraints are extracted from the existing Gumtree platform repos under `/projects/gums`. Any new bot service should align with these established patterns unless there is an explicit decision to diverge.

### 7.3.1 Languages, Frameworks & Service Shape

| Aspect | Current Platform Standard | Notes |
|--------|--------------------------|-------|
| **Primary language** | Java (8 / 11 / 17) | bapi, capi, buyer, seller, conversations, user-service, gdpr-orchestrator, dpe |
| **Secondary languages** | Kotlin (mobile-apps-bff), Scala 2.13 (livead-search), TypeScript (frontend) | |
| **Backend framework** | Spring Boot (newer services), Spring Framework (legacy) | dpe uses Spring Boot 3.2.5 + Java 17 — the most modern reference |
| **Alt framework** | Ktor 2.3 (Kotlin, mobile-apps-bff) | |
| **Build tools** | Maven (Java services), Gradle (Kotlin services), Yarn (frontend monorepo) | |
| **Service shape** | Containerised microservices, one service per repo | Each service owns its own Helm chart, Dockerfile, Jenkinsfile |

### 7.3.2 Database & Storage

| Technology | Usage |
|-----------|-------|
| **PostgreSQL** | Primary relational DB across all services (via GCP Cloud SQL + cloud-sql-proxy sidecar) |
| **Redis** | Caching layer (bapi, capi, gdpr-orchestrator) |
| **Elasticsearch 8.x** | Full-text search (livead-search via Elastic4s) |
| **MongoDB** | User-service secondary store |
| **Cassandra** | Seller-service secondary store |
| **GCP Cloud Storage** | Object / blob storage |

### 7.3.3 Vector Search & ML Infrastructure

**No existing vector search infrastructure** in the current platform.

- Current search is keyword/faceted via Elasticsearch.
- No references to Pinecone, Weaviate, pgvector, Vertex AI Matching Engine, or similar.
- **Implication for bot project**: vector store infra must be provisioned from scratch. Candidates to evaluate given the GCP-only constraint:
  - Vertex AI Vector Search (managed, native GCP)
  - pgvector on existing Cloud SQL PostgreSQL
  - Self-hosted solution on GKE (e.g., Qdrant, Weaviate)

### 7.3.4 Event Streaming

| Aspect | Detail |
|--------|--------|
| **Broker** | Apache Kafka |
| **Serialisation** | Avro (Confluent Schema Registry) |
| **Additional** | GCP Pub/Sub used by gdpr-orchestrator for async workflows |
| **Credentials** | Kafka API key/secret + Schema Registry key/secret via GCP Secret Manager |

### 7.3.5 Deployment Platform

| Aspect | Detail |
|--------|--------|
| **Cloud provider** | **Google Cloud Platform (100%)** — no AWS / Azure |
| **Primary region** | europe-west4; multi-region with europe-west3 |
| **Container orchestration** | Google Kubernetes Engine (GKE) |
| **Service mesh** | Istio (VirtualService routing, internal ingress gateway) |
| **Container registry** | GCP Artifact Registry (`europe-west4-docker.pkg.dev/gum-host-7491/docker-artifact-repo`) |
| **Serverless option** | Cloud Run (supported by Jenkins pipeline) |
| **Infra-as-code** | Helm charts per service (`/helm-chart` directory) |
| **Scaling** | Horizontal Pod Autoscaler (HPA) configured per service |
| **Health endpoints** | `/internal/health/liveness`, `/internal/health/readiness`, `/internal/health` |

### 7.3.6 CI/CD

| Tool | Role |
|------|------|
| **Jenkins** | Primary CI/CD — custom Gumtree pipeline library (`com.gumtree.jenkins.*`), deploys to GKE / VM / Cloud Run |
| **GitHub Actions** | Secondary — PR checks, SonarCloud, Jira workflows (mobile-apps-bff, frontend, iOS) |
| **SonarCloud** | Static analysis / code quality |
| **Pact** | Consumer-driven contract testing (dedicated `pact-verify` Jenkinsfiles) |

### 7.3.7 Observability

| Layer | Technology |
|-------|-----------|
| **Logging** | Logback + SLF4J, Logstash appender; GCP Cloud Logging enabled on GKE services |
| **Metrics** | Micrometer -> Prometheus (PodMonitoring on `/internal/metrics`); InfluxDB (buyer) |
| **Error tracking** | Sentry / Raven (livead-search) |
| **Tracing** | Not explicitly found — likely via Istio sidecar / GCP Cloud Trace |

### 7.3.8 Secret & Config Management

| Aspect | Detail |
|--------|--------|
| **Secrets** | **GCP Secret Manager** — all services mount secrets as env vars (e.g., `site-gumtree-*`) |
| **Config** | Spring Cloud Config (gdpr-orchestrator); Helm values per environment (prod / staging / nonprod) |
| **RBAC** | Per-service GKE service accounts bound to GCP IAM (`iam.gke.io/gcp-service-account`) |

### 7.3.9 Auth & Internal API Security

| Aspect | Detail |
|--------|--------|
| **Service-to-service** | Istio mTLS (service mesh), network policies |
| **API gateway** | Istio Internal Ingress Gateway |
| **Auth tokens** | AES-encrypted auth tokens (`site-gumtree-auth-token-aes-key` in Secret Manager) |
| **External API contracts** | OpenAPI / Swagger codegen; contract YAML per service |
| **Contract testing** | Pact (consumer-driven) |

### 7.3.10 Repo Structure

**Multi-repo**: each service lives in its own repository. The workspace aggregates them for local analysis. Frontend is a Yarn-based monorepo internally but a separate repo from backend services.

**Implication for bot project**: the bot backend should be a **new standalone repo** (greenfield), following the same conventions — own Helm chart, Jenkinsfile, Dockerfile, contract directory.

---

## 7.4 Summary: Recommended Bot Service Baseline

Based on the most modern service in the platform (**dpe** — Spring Boot 3.2.5, Java 17), the bot service should start from:

| Dimension | Recommendation |
|-----------|---------------|
| Language / Framework | **Java 17 + Spring Boot 3.x** (or Kotlin + Ktor if team prefers) |
| Build tool | Maven (align with majority) or Gradle |
| Database | **PostgreSQL on Cloud SQL** (session state, case data, audit log) |
| Vector store | **TBD** — evaluate pgvector / Vertex AI Vector Search / self-hosted on GKE |
| Event bus | Kafka (Avro) for domain events; Pub/Sub acceptable for async jobs |
| Deployment | GKE via Helm + Jenkins pipeline; Cloud Run as lightweight option |
| Observability | Micrometer + Prometheus + GCP Cloud Logging + Logback |
| Secrets | GCP Secret Manager |
| CI/CD | Jenkins (primary) + GitHub Actions (PR checks, SonarCloud) |
| Repo | New greenfield repo with standard layout: `/server`, `/helm-chart`, `/contract` |
| API style | REST (OpenAPI contract), Pact for consumer-driven testing |
