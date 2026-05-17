# Mock Test Data Reference Guide

> For internal testers — last updated: 2026-05-16

## Overview

This document lists all mock data available for testing the CS Agent.
Mock data simulates **external business systems** (Gumtree Platform API, Salesforce CRM)
that the agent depends on. The agent's own data (KB articles, sessions, events) is real.

**Important behavior change:** When an email, ad_id, or conversation_id does not match any
fixture, the system now returns **"not found"** instead of a default fallback. This matches
real production behavior.

---

## Quick Start: Testing a Use Case

| Use Case | Email to Use | Ad ID | Conversation ID |
|----------|-------------|-------|-----------------|
| **UC-A** Ad Status & Visibility | `alice.removed@example.com` | `AD-2001` | — |
| **UC-B** Posting & Editing Guidance | `bob.newuser@example.com` | — | — |
| **UC-C** Messages & Replies | `carol.blocked@example.com` | `AD-2002` | `CONV-003` |
| **UC-D** Account & Login | `dave.locked@example.com` | — | — |
| **UC-D** Account & Login (alt) | `john.doe@example.com` | — | — |
| **UC-E** General Product & Search | `eve.searcher@example.com` | — | — |
| **UC-F** Payment Inquiry | `frank.premium@example.com` | `AD-2003` | — |
| **UC-FP** Correct Deletion Explanation | `grace.pets@example.com` | `AD-2004` | — |
| **UC-G** GDPR / Data Deletion | `john.doe@example.com` | — | — |
| **UC-H** Ad Removal Appeal | `alice.removed@example.com` | `AD-2001` | — |
| **UC-H** Ad Removal Appeal (alt) | `jane.suspended@example.com` | `AD-1002` | — |
| **UC-I** Refund / Payment Dispute | `iris.refund@example.com` | `AD-2005` | — |
| **UC-J** Trust & Safety Report | `jack.reporter@example.com` | `AD-2006` | — |
| **UC-J** Harassment Report | `carol.blocked@example.com` | `AD-2002` | `CONV-005` |
| **UC-K** Technical Issue | `karen.techbug@example.com` | `AD-2007` | — |

---

## Accounts (13 total)

| Email | Status | Type | Fixture File |
|-------|--------|------|-------------|
| `john.doe@example.com` | ACTIVE | PERSONAL | `active_user.json` |
| `jane.suspended@example.com` | SUSPENDED | PERSONAL | `suspended_user.json` |
| `blocked.user@example.com` | BLACKLISTED | PERSONAL | `blacklisted_user.json` |
| `alice.removed@example.com` | ACTIVE | PERSONAL | `alice_ad_query.json` |
| `bob.newuser@example.com` | ACTIVE (new, 0 ads) | PERSONAL | `bob_new_poster.json` |
| `carol.blocked@example.com` | ACTIVE | PERSONAL | `carol_messaging.json` |
| `dave.locked@example.com` | LOCKED | PERSONAL | `dave_locked.json` |
| `eve.searcher@example.com` | ACTIVE | PERSONAL | `eve_general.json` |
| `frank.premium@example.com` | ACTIVE | BUSINESS | `frank_premium.json` |
| `grace.pets@example.com` | ACTIVE | PERSONAL | `grace_pet_seller.json` |
| `iris.refund@example.com` | ACTIVE | PERSONAL | `iris_refund.json` |
| `jack.reporter@example.com` | ACTIVE | PERSONAL | `jack_reporter.json` |
| `karen.techbug@example.com` | ACTIVE | PERSONAL | `karen_tech_issue.json` |

**Edge case:** Any email NOT in this list will return "account not found".

---

## Listings (11 total)

| Ad ID | Title | Status | Seller | Fixture File |
|-------|-------|--------|--------|-------------|
| `AD-1001` | iPhone 15 Pro - Like New | LIVE | john.doe | `live_ad.json` |
| `AD-1002` | Rare Vintage Watch Collection | REMOVED (policy) | jane.suspended | `removed_policy.json` |
| `AD-1003` | Samsung Galaxy S24 Ultra | REMOVED (multi-account) | blocked.user | `removed_multiple_accounts.json` |
| `AD-1004` | Mountain Bike - Barely Used | PROCESSING | john.doe | `processing_ad.json` |
| `AD-2001` | Vintage Leather Sofa | REMOVED (prohibited) | alice.removed | `alice_removed_prohibited.json` |
| `AD-2002` | Handmade Wooden Desk | LIVE | carol.blocked | `carol_listing.json` |
| `AD-2003` | Camera Equipment Bundle | LIVE (featured) | frank.premium | `frank_featured_ad.json` |
| `AD-2004` | Golden Retriever Puppies | REMOVED (pet limit) | grace.pets | `grace_pet_ad.json` |
| `AD-2005` | Concert Tickets - Front Row | LIVE (featured, paid) | iris.refund | `iris_paid_listing.json` |
| `AD-2006` | Suspicious Electronics Lot | REMOVED (scam) | scammer | `scam_listing.json` |
| `AD-2007` | Vintage Record Player | EXPIRED | karen.techbug | `expired_ad.json` |

**Edge case:** Any ad_id NOT in this list will return "listing not found".

---

## Moderation Reviews (10 total)

| Ad ID | Decision | Reason Code | Fixture File |
|-------|----------|-------------|-------------|
| `AD-1002` | REMOVED | MULTIPLE_ACCOUNTS | `multiple_accounts.json` |
| `AD-1005` | REMOVED | PROHIBITED_CONTENT | `prohibited_content.json` |
| `AD-1006` | REMOVED | PET_LIMIT_EXCEEDED | `pet_limit.json` |
| `AD-2001` | REMOVED | PROHIBITED_ITEM | `alice_prohibited_item.json` |
| `AD-2004` | REMOVED | PET_LIMIT_EXCEEDED | `grace_pet_limit.json` |
| `AD-2006` | REMOVED | SCAM_SUSPECT | `scam_suspect_review.json` |
| `AD-3001` | REMOVED | SPAM | `spam_review.json` |
| `AD-3002` | REMOVED | DUPLICATE_AD | `duplicate_ad_review.json` |
| `AD-3003` | REMOVED | MISLEADING_CONTENT | `misleading_content_review.json` |
| `AD-3004` | REMOVED | IMAGE_VIOLATION | `image_violation_review.json` |

**Note:** Moderation review is auto-fetched by `get_customer_context` when listing status is REMOVED.

---

## Message Moderation (5 total)

| Conversation ID | Blocked Count | Reasons | Fixture File |
|-----------------|--------------|---------|-------------|
| `CONV-001` | 0 (clean) | — | `clean.json` |
| `CONV-002` | 2 | PHONE_NUMBER, EXTERNAL_LINK | `blocked_messages.json` |
| `CONV-003` | 3 | PHONE_NUMBER (x2), EXTERNAL_LINK | `carol_blocked_messages.json` |
| `CONV-004` | 2 | SCAM_ATTEMPT, EXTERNAL_LINK | `scam_conversation.json` |
| `CONV-005` | 1 | HARASSMENT | `harassment_messages.json` |

---

## Case Seeds (8 total, loaded on first startup)

| Case ID | Use Case | Subject | Contact |
|---------|----------|---------|---------|
| `CASE-SEED-001` | UC-AD-REMOVAL | Ad removed unfairly | jane.suspended |
| `CASE-SEED-002` | UC-ACCOUNT-ISSUE | Cannot access account | john.doe |
| `CASE-SEED-003` | UC-GDPR-REQUEST | Delete personal data | john.doe |
| `CASE-SEED-004` | UC-I | Refund for ad promotion | iris.refund |
| `CASE-SEED-005` | UC-J | Report suspicious listing | jack.reporter |
| `CASE-SEED-006` | UC-K | App crashes on photo upload | karen.techbug |
| `CASE-SEED-007` | UC-H | Ad wrongly removed | alice.removed |
| `CASE-SEED-008` | UC-J | Harassment in messaging | carol.blocked |

**Note:** Case seeds are only loaded when `mock_cases` table is empty.
To reload: truncate the table and restart the server.

---

## Cross-Reference: Persona Stories

### Alice (UC-A / UC-H)
Active user whose leather sofa ad (`AD-2001`) was removed for "prohibited item".
She believes it was incorrectly flagged and wants to understand why / appeal.
- Account: `alice.removed@example.com` — ACTIVE
- Listing: `AD-2001` — REMOVED
- Moderation: `AD-2001` — PROHIBITED_ITEM
- Case: `CASE-SEED-007` — appeal

### Bob (UC-B)
Brand new user, registered yesterday, no ads posted yet. Needs help posting first ad.
- Account: `bob.newuser@example.com` — ACTIVE, 0 ads

### Carol (UC-C / UC-J)
Active user whose messages keep getting blocked. Also received harassment from a buyer.
- Account: `carol.blocked@example.com` — ACTIVE
- Listing: `AD-2002` — LIVE (the item being discussed)
- Messages: `CONV-003` — 3 blocked messages (phone sharing + external link)
- Messages: `CONV-005` — 1 blocked (harassment from other party)
- Case: `CASE-SEED-008` — harassment report

### Dave (UC-D)
Long-time user whose account got locked. Cannot log in.
- Account: `dave.locked@example.com` — LOCKED

### Eve (UC-E)
Normal user with general product/search questions.
- Account: `eve.searcher@example.com` — ACTIVE

### Frank (UC-F)
Business seller with featured/promoted ads, has payment questions.
- Account: `frank.premium@example.com` — BUSINESS, 12 active ads
- Listing: `AD-2003` — LIVE, featured

### Grace (UC-FP)
Pet seller whose puppy ad was correctly removed for exceeding pet limit.
Needs the deletion explained, not appealed.
- Account: `grace.pets@example.com` — ACTIVE
- Listing: `AD-2004` — REMOVED (pet limit)
- Moderation: `AD-2004` — PET_LIMIT_EXCEEDED

### John (UC-D / UC-G)
Existing active user. Login issues or GDPR deletion request.
- Account: `john.doe@example.com` — ACTIVE
- Listings: `AD-1001` (LIVE), `AD-1004` (PROCESSING)
- Cases: `CASE-SEED-002` (login), `CASE-SEED-003` (GDPR)

### Jane (UC-H)
Suspended user appealing ad removal.
- Account: `jane.suspended@example.com` — SUSPENDED
- Listing: `AD-1002` — REMOVED (policy violation)
- Moderation: `AD-1002` — MULTIPLE_ACCOUNTS
- Case: `CASE-SEED-001` — ad removal appeal

### Iris (UC-I)
User disputing a payment for featured ad promotion.
- Account: `iris.refund@example.com` — ACTIVE
- Listing: `AD-2005` — LIVE, featured, paid promotion
- Case: `CASE-SEED-004` — refund dispute

### Jack (UC-J)
User reporting a suspicious scam listing.
- Account: `jack.reporter@example.com` — ACTIVE
- Reported listing: `AD-2006` — REMOVED (scam suspect)
- Moderation: `AD-2006` — SCAM_SUSPECT
- Case: `CASE-SEED-005` — safety report

### Karen (UC-K)
User experiencing a technical bug (app crash on photo upload).
- Account: `karen.techbug@example.com` — ACTIVE
- Listing: `AD-2007` — EXPIRED
- Case: `CASE-SEED-006` — tech issue

---

## Demo API Endpoints

Inspect loaded mock data at runtime:

```
GET /v1/demo/mock-data/accounts
GET /v1/demo/mock-data/listings
GET /v1/demo/mock-data/moderation_reviews
GET /v1/demo/mock-data/message_moderation
GET /v1/demo/cases
GET /v1/demo/handover-logs
GET /v1/demo/sessions
```
