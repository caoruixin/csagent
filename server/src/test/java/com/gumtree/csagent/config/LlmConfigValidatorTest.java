package com.gumtree.csagent.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sprint §C0: validate the fail-fast LLM config diagnostic surface. We want:
 * <ul>
 *   <li>Missing primary key → FATAL.</li>
 *   <li>Unresolved env-var placeholder for primary key → FATAL.</li>
 *   <li>Malformed primary base URL → FATAL.</li>
 *   <li>Default Kimi base URL ({@code https://api.moonshot.ai/v1}) → WARN
 *       (historically returned 401 in this repo; flag but don't fail).</li>
 *   <li>Working Kimi base URL ({@code https://api.moonshot.cn/v1}) → no warn.</li>
 *   <li>Fallback (DeepSeek) issues → WARN, not FATAL.</li>
 *   <li>{@link LlmConfigValidator#describe} never returns the raw key.</li>
 * </ul>
 */
class LlmConfigValidatorTest {

    private LlmProperties propsWithDefaults() {
        LlmProperties props = new LlmProperties();
        props.getKimi().setApiKey("sk-test-kimi");
        props.getKimi().setBaseUrl("https://api.moonshot.cn/v1");
        props.getKimi().setModel("kimi-k2.6");
        props.getDeepseek().setApiKey("sk-test-deepseek");
        props.getDeepseek().setBaseUrl("https://api.deepseek.com/v1");
        props.getDeepseek().setModel("deepseek-v4-pro");
        return props;
    }

    @Test
    void validate_workingConfig_hasNoFatalsOrWarnings() {
        List<LlmConfigValidator.Diagnostic> diagnostics =
                LlmConfigValidator.validate(propsWithDefaults());
        assertTrue(diagnostics.stream().noneMatch(LlmConfigValidator.Diagnostic::isFatal));
        assertTrue(diagnostics.stream().noneMatch(LlmConfigValidator.Diagnostic::isWarn));
    }

    @Test
    void validate_missingPrimaryKey_isFatal() {
        LlmProperties props = propsWithDefaults();
        props.getKimi().setApiKey("");
        List<LlmConfigValidator.Diagnostic> diagnostics = LlmConfigValidator.validate(props);
        assertTrue(diagnostics.stream()
                .anyMatch(d -> d.isFatal() && "kimi".equals(d.provider())
                        && "api_key_blank".equals(d.code())));
    }

    @Test
    void validate_placeholderPrimaryKey_isFatal() {
        LlmProperties props = propsWithDefaults();
        props.getKimi().setApiKey("${KIMI_API_KEY}");
        List<LlmConfigValidator.Diagnostic> diagnostics = LlmConfigValidator.validate(props);
        assertTrue(diagnostics.stream()
                .anyMatch(d -> d.isFatal() && "api_key_placeholder".equals(d.code())));
    }

    @Test
    void validate_malformedPrimaryUrl_isFatal() {
        LlmProperties props = propsWithDefaults();
        props.getKimi().setBaseUrl("not-a-url");
        List<LlmConfigValidator.Diagnostic> diagnostics = LlmConfigValidator.validate(props);
        assertTrue(diagnostics.stream()
                .anyMatch(d -> d.isFatal() && "base_url_malformed".equals(d.code())));
    }

    @Test
    void validate_blankPrimaryUrl_isFatal() {
        LlmProperties props = propsWithDefaults();
        props.getKimi().setBaseUrl("");
        List<LlmConfigValidator.Diagnostic> diagnostics = LlmConfigValidator.validate(props);
        assertTrue(diagnostics.stream()
                .anyMatch(d -> d.isFatal() && "base_url_blank".equals(d.code())));
    }

    @Test
    void validate_kimiDefaultEndpoint_isWarnNotFatal() {
        LlmProperties props = propsWithDefaults();
        props.getKimi().setBaseUrl(LlmConfigValidator.KIMI_DEFAULT_ENDPOINT);
        List<LlmConfigValidator.Diagnostic> diagnostics = LlmConfigValidator.validate(props);
        assertTrue(diagnostics.stream().noneMatch(LlmConfigValidator.Diagnostic::isFatal));
        assertTrue(diagnostics.stream()
                .anyMatch(d -> d.isWarn() && "kimi".equals(d.provider())
                        && "base_url_default_warning".equals(d.code())),
                "Default Kimi endpoint must produce a warn diagnostic that mentions the working endpoint.");
    }

    @Test
    void validate_kimiWorkingEndpoint_hasNoBaseUrlWarn() {
        LlmProperties props = propsWithDefaults();
        props.getKimi().setBaseUrl(LlmConfigValidator.KIMI_WORKING_ENDPOINT);
        List<LlmConfigValidator.Diagnostic> diagnostics = LlmConfigValidator.validate(props);
        assertTrue(diagnostics.stream()
                .noneMatch(d -> "base_url_default_warning".equals(d.code())));
    }

    @Test
    void validate_missingFallbackKey_isWarnNotFatal() {
        LlmProperties props = propsWithDefaults();
        props.getDeepseek().setApiKey("");
        List<LlmConfigValidator.Diagnostic> diagnostics = LlmConfigValidator.validate(props);
        assertTrue(diagnostics.stream().noneMatch(LlmConfigValidator.Diagnostic::isFatal));
        assertTrue(diagnostics.stream()
                .anyMatch(d -> d.isWarn() && "deepseek".equals(d.provider())));
    }

    @Test
    void validateOrThrow_workingConfig_doesNotThrow() {
        assertDoesNotThrow(() -> LlmConfigValidator.validateOrThrow(propsWithDefaults()));
    }

    @Test
    void validateOrThrow_brokenPrimary_throws() {
        LlmProperties props = propsWithDefaults();
        props.getKimi().setApiKey("");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> LlmConfigValidator.validateOrThrow(props));
        assertTrue(ex.getMessage().contains("kimi"),
                "Diagnostic must mention the failing provider: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("api_key_blank"),
                "Diagnostic must mention the failure code: " + ex.getMessage());
    }

    @Test
    void describe_neverReturnsKeyContent() {
        LlmProperties props = propsWithDefaults();
        props.getKimi().setApiKey("sk-VERY-SECRET-DO-NOT-LEAK-12345");
        String description = LlmConfigValidator.describe(props);
        assertFalse(description.contains("sk-VERY-SECRET"),
                "describe() must NEVER include raw key content. Got: " + description);
        assertTrue(description.contains("present"),
                "describe() should report key state (present|placeholder|blank). Got: " + description);
    }

    @Test
    void describeKeyState_classifiesKeyShape() {
        assertEquals("blank", LlmConfigValidator.describeKeyState(null));
        assertEquals("blank", LlmConfigValidator.describeKeyState(""));
        assertEquals("placeholder", LlmConfigValidator.describeKeyState("${KIMI_API_KEY}"));
        assertEquals("placeholder", LlmConfigValidator.describeKeyState("your-api-key-here"));
        assertEquals("present", LlmConfigValidator.describeKeyState("sk-real-key"));
    }

    @Test
    void looksLikePlaceholder_recognisesEnvVarSyntax() {
        assertTrue(LlmConfigValidator.looksLikePlaceholder("${KIMI_API_KEY}"));
        assertTrue(LlmConfigValidator.looksLikePlaceholder("changeme"));
        assertFalse(LlmConfigValidator.looksLikePlaceholder("sk-real-key"));
        assertFalse(LlmConfigValidator.looksLikePlaceholder(null));
    }

    @Test
    void isHttpUrl_acceptsHttpAndHttps() {
        assertTrue(LlmConfigValidator.isHttpUrl("https://api.moonshot.cn/v1"));
        assertTrue(LlmConfigValidator.isHttpUrl("http://localhost:19999"));
        assertFalse(LlmConfigValidator.isHttpUrl("not-a-url"));
        assertFalse(LlmConfigValidator.isHttpUrl(""));
        assertFalse(LlmConfigValidator.isHttpUrl(null));
        assertFalse(LlmConfigValidator.isHttpUrl("ftp://example.com"));
    }
}
