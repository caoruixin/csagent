---
title: A/B testing policy — GrowthBook configuration
doc_tier: runbook
status: current
implementation_status: not_started
source_of_truth: GrowthBook Dashboard (external system) + this file (rollout policy)
last_reviewed: 2026-05-10
review_cadence: every 3-5 sprints
supersedes: []
superseded_by: null
notes: >
  Describes the GrowthBook configuration and progressive-rollout policy
  intended to gate csagent traffic in production. The csagent server
  and UI in this repo do not currently import GrowthBook SDKs, fetch
  feature payloads, or read a `cs_bot_enabled` flag — see PRODUCTION_GAP
  note below. The GrowthBook configuration described here lives on the
  Gumtree main-site stack (referenced URLs target gum-site-prod /
  gum-site-stage), not on this repository.
---

# A/B testing policy

> **Status (CURRENT for the rollout agreement, PRODUCTION_GAP for csagent integration)**
>
> The GrowthBook configuration, traffic-allocation strategy, and
> progressive-rollout cadence described below are the agreed policy
> for production. They are not implemented in this repo: a grep of
> `server/` and `ui/` for `growthbook`, `GrowthBook`, or
> `cs_bot_enabled` returns no matches today. When csagent is wired
> into a production deployment, the integration must follow this
> policy; until then the server runs unconditionally on whoever hits
> the local endpoint.

## GrowthBook 配置总结

  流量分配策略

  ┌─────────┬──────────────────────────────────────────────────────────────────────────────┐
  │  维度   │                                     配置                                     │
  ├─────────┼──────────────────────────────────────────────────────────────────────────────┤
  │ 用户标  │ Cookie gt_gb_exp_uid，UUID v4，有效期 365 天，HttpOnly                       │
  │ 识      │                                                                              │
  ├─────────┼──────────────────────────────────────────────────────────────────────────────┤
  │ Hash    │ gbUserPseudoId（伪 ID，保证同一用户始终落入同一桶）                          │
  │ 属性    │                                                                              │
  ├─────────┼──────────────────────────────────────────────────────────────────────────────┤
  │ 分组权  │ 由 GrowthBook Dashboard 管理，frontend 通过 CDN 拉取，每 1 分钟刷新一次      │
  │ 重      │                                                                              │
  ├─────────┼──────────────────────────────────────────────────────────────────────────────┤
  │ Feature │ prod: growthbook.gum-site-prod.gumtree.cloud/api/features/sdk-rNrJeo3IHlP3UC │
  │  数据   │ Ostaging:                                                                    │
  │ URL     │ growthbook.gum-site-stage.gumtree.cloud/api/features/sdk-cGG3ekqdIkb9ErAL    │
  ├─────────┼──────────────────────────────────────────────────────────────────────────────┤
  │ 覆盖机  │ Query param / HTTP header / Cookie gt_gb_exp_ovr，格式：KEY:A|KEY2:B         │
  │ 制      │                                                                              │   
  └─────────┴──────────────────────────────────────────────────────────────────────────────┘

## Go/No-Go 阈值                                          
- 代码层面没有硬编码阈值——所有 coverage %、variant weights、rollout % 均在 GrowthBook Dashboard 上配置并通过 CDN payload 下发。前端只负责：
                                                                                               
  1. 拉取 feature payload（BFF 侧，1 分钟缓存）                                                
  2. 用 gbUserPseudoId hash 分桶
  3. 触发 GA4 experiment_viewed 事件上报 experimentId + variationId                            
                                                                                               
  实验变体                                                                                     
                                                                                               
  定义在 packages/shared/src/model/experiment.ts，支持 A / B / C / D 四个 variant，以及 true / 
  false 开关型。                               
                                                                                               
  当前共有约 30 个 client-side flags + 18 个 server-side flags 在管理中（见 GrowthBookFeature /
   GrowthBookServerSideFeatures enum）。
                                                                                               
  ---                                                    
  结论：前端代码仅实现 SDK 接入和 tracking，go/no-go 决策阈值完全托管在 GrowthBook  Dashboard
- 开发阶段100%打开，发布prod 环境前，10% 20% 50% 100% gradually roll out