package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 34 — extractor-unit-contract tests for the UC-G / UC-H / UC-I /
 * UC-J form-context prefill extension. Mirrors the
 * {@link Sprint71PartialIntakePersistenceTest} extractor-unit-contract
 * shape and is the per-UC complement to that file (which stays the UC-K
 * regression guard at HEAD).
 *
 * <p>Closure contract for each per-UC group: given a
 * {@code session.formContext} JSON that carries the relevant canonical
 * source fields (per {@link FormContextIngestionService}'s shape:
 * first_name / email / topic_subject / description / optional ad_id),
 * the extractor returns a map whose keys are the canonical intake-field
 * names defined in {@link IntakeFieldsRegistry#requiredFieldsFor}. No
 * regex on user-typed content is exercised — the new branches are
 * field-mapping plumbing (per Sprint 34 §8 stanza anti-hardcode self-
 * walk in {@code docs/sprint_objective.md}).
 */
class Sprint34IntakePrefillExtractorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // -------- UC-H — Ad Removal Appeal --------

    @Test
    void ucH_seedsAdIdAndEmailFromFormContext() {
        String formContext = "{\"first_name\":\"Alice\","
                + "\"email\":\"alice@example.com\","
                + "\"topic_subject\":\"Ad Support\","
                + "\"ad_id\":\"AD-2001\","
                + "\"description\":\"I can't see my advert\"}";
        Map<String, String> out = IntakeFieldExtractor.extractFromTurn(
                "UC-H", "I want to appeal the removal", formContext, objectMapper);
        assertEquals("AD-2001", out.get("ad_id_or_listing_url"),
                "form_context.ad_id must seed canonical ad_id_or_listing_url");
        assertEquals("alice@example.com", out.get("registered_email"),
                "form_context.email must seed canonical registered_email");
    }

    @Test
    void ucH_seedsOnlyAdId_whenFormContextEmailMissing() {
        String formContext = "{\"ad_id\":\"AD-9001\"}";
        Map<String, String> out = IntakeFieldExtractor.extractFromTurn(
                "UC-H", "appeal please", formContext, objectMapper);
        assertEquals("AD-9001", out.get("ad_id_or_listing_url"));
        assertNull(out.get("registered_email"),
                "missing form_context.email must leave registered_email unseeded");
    }

    @Test
    void ucH_seedsOnlyEmail_whenFormContextAdIdMissing() {
        String formContext = "{\"email\":\"bob@example.com\"}";
        Map<String, String> out = IntakeFieldExtractor.extractFromTurn(
                "UC-H", "appeal please", formContext, objectMapper);
        assertNull(out.get("ad_id_or_listing_url"),
                "missing form_context.ad_id must leave ad_id_or_listing_url unseeded");
        assertEquals("bob@example.com", out.get("registered_email"));
    }

    @Test
    void ucH_returnsEmpty_whenFormContextHasNeitherAdIdNorEmail() {
        String formContext = "{\"first_name\":\"Carol\","
                + "\"topic_subject\":\"Ad Support\","
                + "\"description\":\"why\"}";
        Map<String, String> out = IntakeFieldExtractor.extractFromTurn(
                "UC-H", "appeal", formContext, objectMapper);
        assertTrue(out.isEmpty(),
                "UC-H must produce no fields when neither ad_id nor email is in form_context");
    }

    @Test
    void ucH_returnsEmpty_whenFormContextIsNull() {
        Map<String, String> out = IntakeFieldExtractor.extractFromTurn(
                "UC-H", "I want to appeal", null, objectMapper);
        assertTrue(out.isEmpty(),
                "UC-H must produce no fields when form_context is null");
    }

    @Test
    void ucH_returnsEmpty_whenFormContextIsBlank() {
        Map<String, String> out = IntakeFieldExtractor.extractFromTurn(
                "UC-H", "I want to appeal", "   ", objectMapper);
        assertTrue(out.isEmpty(),
                "UC-H must produce no fields when form_context is blank");
    }

    @Test
    void ucH_doesNotMineUserMessageForAdId() {
        // The bot must NOT regex the user's typed text for an ad-id-shaped
        // token. The form_context is the source of truth; inline mentions
        // come through the LLM's request_handover.arguments.intake_fields
        // path (persistInlineIntakeFields), not the extractor.
        Map<String, String> out = IntakeFieldExtractor.extractFromTurn(
                "UC-H", "my ad AD-12345 was removed unfairly", null, objectMapper);
        assertTrue(out.isEmpty(),
                "UC-H extractor must not mine user message for ad_id shape");
    }

    @Test
    void ucH_doesNotMineUserMessageForEmail() {
        Map<String, String> out = IntakeFieldExtractor.extractFromTurn(
                "UC-H", "my email is foo@bar.com please appeal", null, objectMapper);
        assertTrue(out.isEmpty(),
                "UC-H extractor must not mine user message for email shape");
    }

    @Test
    void ucH_doesNotOverwriteExistingFields() {
        Map<String, String> existing = new HashMap<>();
        existing.put("ad_id_or_listing_url", "AD-USER-SUPPLIED");
        existing.put("registered_email", "user-supplied@example.com");
        Map<String, String> merged = IntakeFieldExtractor.mergeForUc(
                "UC-H", existing, "appeal",
                "{\"ad_id\":\"AD-FORM\",\"email\":\"form@example.com\"}", objectMapper);
        // existing inline values preserved per the mergeForUc skip-if-existing rule.
        assertEquals("AD-USER-SUPPLIED", merged.get("ad_id_or_listing_url"));
        assertEquals("user-supplied@example.com", merged.get("registered_email"));
    }

    @Test
    void ucH_mergesIntoCanonicalNames_evenIfFormUsesAliasKey() {
        // form_context historically writes "ad_id" (alias) and "email"
        // (alias). After Sprint 34, the merged session.intakeFields map
        // carries the canonical names defined in
        // IntakeFieldsRegistry.requiredFieldsFor("UC-H").
        Map<String, String> merged = IntakeFieldExtractor.mergeForUc(
                "UC-H", new HashMap<>(), "appeal",
                "{\"ad_id\":\"AD-3001\",\"email\":\"x@y.z\"}", objectMapper);
        assertEquals("AD-3001", merged.get("ad_id_or_listing_url"));
        assertEquals("x@y.z", merged.get("registered_email"));
    }

    // -------- UC-G — GDPR / data-action intake --------

    @Test
    void ucG_seedsRegisteredEmailFromFormContext() {
        String formContext = "{\"first_name\":\"Dave\","
                + "\"email\":\"dave@example.com\","
                + "\"topic_subject\":\"Account & Privacy\","
                + "\"description\":\"please delete my account\"}";
        Map<String, String> out = IntakeFieldExtractor.extractFromTurn(
                "UC-G", "delete my account please", formContext, objectMapper);
        assertEquals("dave@example.com", out.get("registered_email"),
                "form_context.email must seed canonical registered_email for UC-G");
        assertNull(out.get("data_request_type"),
                "data_request_type has no form_context source today — must remain unseeded");
    }

    @Test
    void ucG_returnsEmpty_whenFormContextEmailMissing() {
        String formContext = "{\"first_name\":\"Eve\",\"topic_subject\":\"Account & Privacy\"}";
        Map<String, String> out = IntakeFieldExtractor.extractFromTurn(
                "UC-G", "delete", formContext, objectMapper);
        assertTrue(out.isEmpty(),
                "UC-G extractor must produce no fields when form_context.email is absent");
    }

    @Test
    void ucG_returnsEmpty_whenFormContextIsNull() {
        Map<String, String> out = IntakeFieldExtractor.extractFromTurn(
                "UC-G", "GDPR deletion", null, objectMapper);
        assertTrue(out.isEmpty(),
                "UC-G extractor must produce no fields when form_context is null");
    }

    @Test
    void ucG_doesNotMineUserMessageForEmail() {
        Map<String, String> out = IntakeFieldExtractor.extractFromTurn(
                "UC-G", "my email is gdpr@example.com — please delete", null, objectMapper);
        assertTrue(out.isEmpty(),
                "UC-G extractor must not mine user message for email shape");
    }

    // -------- UC-J — Trust & Safety report intake --------

    @Test
    void ucJ_seedsDescriptionFromFormContext() {
        String formContext = "{\"first_name\":\"Frank\","
                + "\"email\":\"frank@example.com\","
                + "\"topic_subject\":\"Trust & Safety\","
                + "\"description\":\"user XYZ is posting spam ads repeatedly\"}";
        Map<String, String> out = IntakeFieldExtractor.extractFromTurn(
                "UC-J", "please review", formContext, objectMapper);
        assertEquals("user XYZ is posting spam ads repeatedly", out.get("description"),
                "form_context.description must seed canonical description for UC-J");
        assertNull(out.get("report_target"),
                "report_target has no form_context source — must remain unseeded");
        assertNull(out.get("report_type"),
                "report_type has no form_context source — must remain unseeded");
    }

    @Test
    void ucJ_returnsEmpty_whenFormContextDescriptionBlank() {
        String formContext = "{\"first_name\":\"Grace\",\"description\":\"\"}";
        Map<String, String> out = IntakeFieldExtractor.extractFromTurn(
                "UC-J", "please review", formContext, objectMapper);
        assertTrue(out.isEmpty(),
                "UC-J extractor must produce no fields when form_context.description is blank");
    }

    @Test
    void ucJ_returnsEmpty_whenFormContextIsNull() {
        Map<String, String> out = IntakeFieldExtractor.extractFromTurn(
                "UC-J", "please review", null, objectMapper);
        assertTrue(out.isEmpty(),
                "UC-J extractor must produce no fields when form_context is null");
    }

    @Test
    void ucJ_doesNotMineUserMessageForDescription() {
        Map<String, String> out = IntakeFieldExtractor.extractFromTurn(
                "UC-J", "the user has been posting spam in messages",
                "{\"first_name\":\"H\"}", objectMapper);
        assertTrue(out.isEmpty(),
                "UC-J extractor must not capture description from user message; "
                        + "only the form_context.description field is read");
    }

    // -------- UC-I — Refund / payment-dispute intake (deliberate no-op) --------

    @Test
    void ucI_isDeliberateNoOp_evenWithFullFormContext() {
        String formContext = "{\"first_name\":\"Ivan\","
                + "\"email\":\"ivan@example.com\","
                + "\"topic_subject\":\"Payments\","
                + "\"description\":\"my refund didn't arrive\"}";
        Map<String, String> out = IntakeFieldExtractor.extractFromTurn(
                "UC-I", "please refund", formContext, objectMapper);
        assertTrue(out.isEmpty(),
                "UC-I extractor is a deliberate no-op today — form_context carries no "
                        + "transaction_reference or dispute_reason source. The branch "
                        + "exists for symmetry; a future sprint that adds a "
                        + "transaction-reference form field can extend it without "
                        + "re-touching the dispatch in extractFromTurn.");
    }

    @Test
    void ucI_isDeliberateNoOp_withNullFormContext() {
        Map<String, String> out = IntakeFieldExtractor.extractFromTurn(
                "UC-I", "refund please", null, objectMapper);
        assertTrue(out.isEmpty(),
                "UC-I extractor must remain a no-op when form_context is null");
    }

    @Test
    void ucI_isDeliberateNoOp_withBlankFormContext() {
        Map<String, String> out = IntakeFieldExtractor.extractFromTurn(
                "UC-I", "refund please", "   ", objectMapper);
        assertTrue(out.isEmpty(),
                "UC-I extractor must remain a no-op when form_context is blank");
    }

    // -------- UC-K regression guard (delegates to existing helper) --------

    @Test
    void ucK_stillCapturesPlatformFromUserReply() {
        // Sanity: Sprint 34 did NOT touch the UC-K helpers. The fixture
        // matches Sprint71PartialIntakePersistenceTest.extractor_capturesPlatformFromUserReply.
        Map<String, String> out = IntakeFieldExtractor.extractFromTurn(
                "UC-K", "I'm using Chrome on Windows 11", null, objectMapper);
        assertEquals("I'm using Chrome on Windows 11", out.get("platform"));
    }

    @Test
    void ucK_stillCapturesReproStepsFromCs066FormDescription() {
        String formContext = "{\"description\":\""
                + "Why am I not getting the option to add my phone number as a "
                + "point of contact when listening an item any more\"}";
        Map<String, String> out = IntakeFieldExtractor.extractFromTurn(
                "UC-K", "thanks", formContext, objectMapper);
        assertNotNull(out.get("repro_steps_or_error_message"),
                "Sprint 34 must NOT regress cs066 UC-K extraction shape");
    }

    @Test
    void ucK_doesNotLeakIntoOtherUcBranches() {
        // Sanity: a UC-K-flavoured fixture passed in with uc="UC-H" must
        // NOT seed UC-K canonical names; the dispatch branches are
        // mutually exclusive on uc identifier.
        Map<String, String> out = IntakeFieldExtractor.extractFromTurn(
                "UC-H", "I'm using Chrome on Windows 11", null, objectMapper);
        assertFalse(out.containsKey("platform"),
                "UC-H branch must not seed UC-K's platform canonical name");
        assertFalse(out.containsKey("repro_steps_or_error_message"),
                "UC-H branch must not seed UC-K's repro_steps canonical name");
    }

    // -------- handlesUc surface --------

    @Test
    void handlesUc_returnsTrueForAllIntakeUcs() {
        assertTrue(IntakeFieldExtractor.handlesUc("UC-G"), "UC-G must be handled");
        assertTrue(IntakeFieldExtractor.handlesUc("UC-H"), "UC-H must be handled");
        assertTrue(IntakeFieldExtractor.handlesUc("UC-I"), "UC-I must be handled (no-op today)");
        assertTrue(IntakeFieldExtractor.handlesUc("UC-J"), "UC-J must be handled");
        assertTrue(IntakeFieldExtractor.handlesUc("UC-K"),
                "UC-K must still be handled after Sprint 34");
    }

    @Test
    void handlesUc_returnsFalseForFaqAndUnknownUcs() {
        assertFalse(IntakeFieldExtractor.handlesUc("UC-A"), "FAQ-path UC-A must not be handled");
        assertFalse(IntakeFieldExtractor.handlesUc("UC-B"), "FAQ-path UC-B must not be handled");
        assertFalse(IntakeFieldExtractor.handlesUc("UC-C"), "FAQ-path UC-C must not be handled");
        assertFalse(IntakeFieldExtractor.handlesUc("UC-D"), "FAQ-path UC-D must not be handled");
        assertFalse(IntakeFieldExtractor.handlesUc("UC-F"), "FAQ-path UC-F must not be handled");
        assertFalse(IntakeFieldExtractor.handlesUc("UC-FP"), "FAQ-path UC-FP must not be handled");
        assertFalse(IntakeFieldExtractor.handlesUc("UC-NONEXISTENT"));
        assertFalse(IntakeFieldExtractor.handlesUc(null), "null UC must not be handled");
    }

    // -------- extractFormContextField helper (direct) --------

    @Test
    void extractFormContextField_returnsValueWhenPresent() {
        String value = IntakeFieldExtractor.extractFormContextField(
                "{\"ad_id\":\"AD-77\",\"email\":\"foo@bar\"}", "ad_id", objectMapper);
        assertEquals("AD-77", value);
    }

    @Test
    void extractFormContextField_returnsNullForMissingField() {
        String value = IntakeFieldExtractor.extractFormContextField(
                "{\"first_name\":\"X\"}", "ad_id", objectMapper);
        assertNull(value);
    }

    @Test
    void extractFormContextField_returnsNullForNullJson() {
        assertNull(IntakeFieldExtractor.extractFormContextField(null, "ad_id", objectMapper));
    }

    @Test
    void extractFormContextField_returnsNullForBlankJson() {
        assertNull(IntakeFieldExtractor.extractFormContextField("   ", "ad_id", objectMapper));
    }

    @Test
    void extractFormContextField_returnsNullForBlankField() {
        assertNull(IntakeFieldExtractor.extractFormContextField(
                "{\"ad_id\":\"\"}", "ad_id", objectMapper),
                "blank string value must be normalised to null");
    }

    @Test
    void extractFormContextField_returnsNullForUnparseableJson() {
        assertNull(IntakeFieldExtractor.extractFormContextField(
                "not json", "ad_id", objectMapper),
                "extractor must tolerate unparseable JSON without throwing");
    }

    @Test
    void extractFormContextField_returnsNullForNonObjectJson() {
        assertNull(IntakeFieldExtractor.extractFormContextField(
                "[\"a\",\"b\"]", "ad_id", objectMapper),
                "array root must be treated as missing field");
    }
}
