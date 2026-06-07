# ADR-0009 — Defer Kubernetes for production; use Render's managed orchestration

**Status:** Accepted
**Date:** 2026-06-07
**Decision makers:** Vishal Bhapkar
**Relates to:** [ADR-0008](0008-docker.md)

---

## Context

Once we've adopted Docker (ADR-0008), the next question on the "real production app" path is: do we orchestrate containers ourselves with Kubernetes? Or do we let a PaaS handle it?

Kubernetes is the de facto orchestration platform for containerized production workloads at large companies. Common interview topic. Strong learning value. But also: famously complex to set up, expensive to run, and overkill for many real-world apps.

We have one service to deploy (the Spring Boot backend; frontend is a static Vercel deploy that's not a container). Render's free tier already handles:
- Pulling the Docker image
- Running the container
- Restarting on crash
- Health checking via `/actuator/health`
- HTTPS termination
- DNS

These are the things Kubernetes also provides (Deployments + Services + Ingress + Probes). For a single service, the value-add of Kubernetes over Render is zero.

That said, Vishal has explicitly stated learning goals around production / orchestration. Skipping Kubernetes entirely would miss a legitimate interview-relevant topic.

## Decision

**Production:** No Kubernetes. Render's managed container service handles orchestration for our single backend. Frontend remains on Vercel's static hosting (not a container at all).

**Learning side-quest (optional, anytime):** We will write Kubernetes manifests for the same backend image and demonstrate running it on **local minikube or kind** (Kubernetes In Docker). This produces:
- `infra/k8s/` directory with `Deployment`, `Service`, `ConfigMap`, `Secret`, `Ingress`, and `HorizontalPodAutoscaler` manifests
- A runbook entry: "How to run Daily on local Kubernetes"
- A `docs/learnings/kubernetes.md` study note covering the concepts and the common interview questions

The side-quest does **not** deploy to any cloud Kubernetes (no GKE / EKS / AKS spend). It's purely a local learning artifact, parallel to the Render production path.

## Consequences

### Positive
- **Right-sized for the workload.** No YAML proliferation, no etcd to babysit, no Helm chart to maintain. Render handles the boring parts.
- **Free.** Cloud Kubernetes (even GKE Autopilot) starts at ~$72/month minimum (control plane). Render's free tier is $0.
- **Faster time to deploy.** `docker compose up` locally → `git push` to deploy. No `kubectl apply -f ...` ceremony.
- **Learning path preserved.** Vishal still gets Kubernetes exposure via the local minikube side-quest, on his own schedule, without ops overhead in the critical path.
- **Easy to upgrade later.** If we ever genuinely need Kubernetes (e.g., we hit scale where multiple service instances + service mesh + autoscaling become real concerns), the Docker image already exists; the migration is writing manifests, not refactoring code.

### Negative
- **"Kubernetes in production" not on the resume.** A literal recruiter scanning for "K8s" as a keyword would not see it in Daily's deployment. Mitigated by: (a) the side-quest manifests are on the repo; (b) interview answers can speak honestly to "I evaluated K8s for this project and explicitly chose a PaaS because the orchestration value didn't justify the operational cost."
- **Render lock-in for orchestration.** If we ever leave Render, we have to set up the equivalent elsewhere (App Engine, ECS, Fly.io, K8s). Mitigated because we own the Docker image; the migration is reconfiguring deploy, not rewriting.
- **No experience operating a real K8s cluster.** Side-quest only covers application-level concerns. Cluster-level concerns (node pools, etcd, control plane, network policies) we wouldn't touch.

## Alternatives considered

### Alternative A — Self-managed Kubernetes on a single VPS (k3s / microk8s)
**Rejected** because:
- Cluster maintenance is real work (upgrades, certs, monitoring of the cluster itself).
- One-node "cluster" provides no actual orchestration benefit over Docker Compose.
- VPS hosting cost ($5–10/month) vs. Render free.

### Alternative B — Managed cloud Kubernetes (GKE / EKS / AKS) on free credits
**Rejected** because:
- Free credits are time-limited (90 days, 12 months). The "free forever" requirement disqualifies all of them.
- Even with credits, you pay for control plane + node compute. Going over the free quota silently bills.

### Alternative C — Docker Swarm
**Rejected** because:
- Largely abandoned. Docker company no longer prioritizes Swarm in favor of Kubernetes.
- No interview market for Swarm-specific knowledge.

### Alternative D — Nomad
**Rejected** because:
- Excellent product but smaller industry footprint than Kubernetes.
- Skips both the Render simplicity and the K8s learning value.

### Alternative E — Fly.io
**Rejected** because:
- Conceptually closer to Render than K8s — managed container orchestration.
- Free tier exists but smaller and more confusing than Render's.
- We already have Render set up from the demo project, saves setup.

### Alternative F — Just deploy K8s anyway because of learning
**Rejected** because:
- Conflates "I want to learn K8s" with "K8s is right for this project." The learning need is solved by the local side-quest; the production need is solved by Render. Don't pay ops complexity for learning that can happen offline.

## What the side-quest covers (when we do it)

Topics to study and document in `docs/learnings/kubernetes.md`:

1. **Core resources:**
   - `Pod` — the smallest deployable unit
   - `Deployment` — declarative pod management with rollout / rollback
   - `Service` (ClusterIP, NodePort, LoadBalancer) — networking
   - `Ingress` — HTTP routing into the cluster
   - `ConfigMap` and `Secret` — config / credentials
   - `Namespace` — isolation
2. **Operational concepts:**
   - Liveness / readiness / startup probes
   - Resource requests and limits
   - HorizontalPodAutoscaler (HPA)
   - Rolling vs. recreate deployment strategies
3. **Stateful workloads:**
   - StatefulSet vs. Deployment
   - Persistent volumes, PVCs
   - StorageClass, dynamic provisioning
4. **Networking:**
   - Service mesh basics (Istio / Linkerd) — concepts, when warranted
   - Network policies
5. **Practical:**
   - `kubectl` commands cheat sheet
   - `minikube` / `kind` for local
   - Helm basics (templating, releases)
6. **Common interview questions:**
   - "What's the difference between Deployment and StatefulSet?"
   - "How does a Service find Pods?"
   - "What's the difference between liveness and readiness probes?"
   - "How do you roll back a failed deployment?"
   - "Why might you use a service mesh?"

This produces a portfolio artifact (the manifests) and an interview-ready knowledge base, without committing to ops complexity in production.

## When we'd revisit this decision

We would graduate to real Kubernetes if **any of these** become true:
- Multiple backend services (~3+) with internal service-to-service traffic
- Need for service mesh (mTLS, traffic shaping, advanced routing)
- Need for cluster-level features Render doesn't offer (network policies, custom storage classes, dedicated nodes)
- Team grows to 5+ engineers and we need consistent deploy / config patterns across many apps
- Daily achieves scale where Render's pricing tiers no longer make sense

None of these are remotely imminent. If any becomes true, we write a new ADR superseding this one.

## References

- [Kubernetes documentation](https://kubernetes.io/docs/home/)
- [Render docs — Docker deployments](https://render.com/docs/docker)
- [minikube](https://minikube.sigs.k8s.io/docs/) and [kind](https://kind.sigs.k8s.io/) for local K8s
- "Production Kubernetes" (Josh Rosso et al.) — the textbook on running real K8s
