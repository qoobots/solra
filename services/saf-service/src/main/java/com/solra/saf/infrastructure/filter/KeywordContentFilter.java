package com.solra.saf.infrastructure.filter;

import com.solra.saf.domain.model.*;
import com.solra.saf.domain.service.ContentFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Keyword + regex based text content filter (SAF-002 primary implementation).
 * This is the H1 prototype filter; production will add AI model filtering.
 */
@Component
public class KeywordContentFilter implements ContentFilter {

    private static final Logger log = LoggerFactory.getLogger(KeywordContentFilter.class);

    private static final Set<ContentType> SUPPORTED_TYPES = EnumSet.of(
            ContentType.TEXT, ContentType.AVATAR_SPEECH,
            ContentType.SPACE_NAME, ContentType.SPACE_DESCRIPTION,
            ContentType.USER_PROFILE
    );

    // Sensitive keyword patterns (production: load from config/database)
    private static final Map<Pattern, PolicyViolation.Category> BLOCKED_PATTERNS = new LinkedHashMap<>();

    static {
        // Build blocked patterns — these are sample rules; production uses a comprehensive library
        addPattern("(?i)\\b(self.?harm|suicide|kill.?myself|end.?my.?life)\\b", PolicyViolation.Category.SELF_HARM);
        addPattern("(?i)\\b(hack|exploit|malware|ransomware|phishing)\\b", PolicyViolation.Category.ILLEGAL);
        addPattern("(?i)(\\d{15,19})", PolicyViolation.Category.PERSONAL_INFO); // Card number pattern
        addPattern("(?i)\\b(spam|scam|fraud|ponzi)\\b", PolicyViolation.Category.FRAUD);
        addPattern("(?i)\\b(abuse|harass|bully|threaten)\\b", PolicyViolation.Category.HARASSMENT);
    }

    private static void addPattern(String regex, PolicyViolation.Category category) {
        BLOCKED_PATTERNS.put(Pattern.compile(regex), category);
    }

    @Override
    public FilterResult filter(String content) {
        if (content == null || content.isBlank()) {
            return FilterResult.pass(SafetyScore.safe("keyword-v1"));
        }

        List<PolicyViolation> violations = new ArrayList<>();
        String lowerContent = content.toLowerCase();

        for (Map.Entry<Pattern, PolicyViolation.Category> entry : BLOCKED_PATTERNS.entrySet()) {
            var matcher = entry.getKey().matcher(lowerContent);
            if (matcher.find()) {
                PolicyViolation.Category category = entry.getValue();
                PolicyViolation.Severity severity = switch (category) {
                    case ILLEGAL, SELF_HARM, MINOR_SAFETY -> PolicyViolation.Severity.CRITICAL;
                    case NSFW, VIOLENCE, PERSONAL_INFO -> PolicyViolation.Severity.HIGH;
                    case HATE_SPEECH, FRAUD, HARASSMENT -> PolicyViolation.Severity.MEDIUM;
                    default -> PolicyViolation.Severity.LOW;
                };

                violations.add(PolicyViolation.detected(
                        "KW-" + UUID.randomUUID().toString().substring(0, 8),
                        category.name(),
                        category,
                        severity,
                        "Content matches blocked pattern in category: " + category,
                        matcher.group()
                ));
            }
        }

        if (!violations.isEmpty()) {
            // Check if any violation is critical — block immediately
            boolean hasCritical = violations.stream()
                    .anyMatch(v -> v.getSeverity() == PolicyViolation.Severity.CRITICAL);

            float score = hasCritical ? 0.0f : 0.2f;
            List<SafetyScore.CategoryScore> catScores = violations.stream()
                    .map(v -> new SafetyScore.CategoryScore(v.getCategory().name(), 0.1f))
                    .distinct()
                    .toList();

            log.warn("Content blocked: {} violations, score={}", violations.size(), score);
            return FilterResult.reject(SafetyScore.unsafe(score, catScores, "keyword-v1"), violations);
        }

        return FilterResult.pass(SafetyScore.safe("keyword-v1"));
    }

    @Override
    public boolean supports(ContentType contentType) {
        return SUPPORTED_TYPES.contains(contentType);
    }
}
