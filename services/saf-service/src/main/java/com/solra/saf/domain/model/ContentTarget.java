package com.solra.saf.domain.model;

/**
 * Value object — the content being reviewed.
 */
public class ContentTarget {

    private String contentId;
    private ContentType contentType;
    private String contentText;
    private String contentUrl;
    private String contentHash;

    private ContentTarget() {}

    public static ContentTarget text(String contentId, String text) {
        ContentTarget t = new ContentTarget();
        t.contentId = contentId;
        t.contentType = ContentType.TEXT;
        t.contentText = text;
        t.contentHash = hashContent(text);
        return t;
    }

    public static ContentTarget avatarSpeech(String contentId, String speechText) {
        ContentTarget t = new ContentTarget();
        t.contentId = contentId;
        t.contentType = ContentType.AVATAR_SPEECH;
        t.contentText = speechText;
        t.contentHash = hashContent(speechText);
        return t;
    }

    public static ContentTarget spaceDescription(String contentId, String desc) {
        ContentTarget t = new ContentTarget();
        t.contentId = contentId;
        t.contentType = ContentType.SPACE_DESCRIPTION;
        t.contentText = desc;
        t.contentHash = hashContent(desc);
        return t;
    }

    private static String hashContent(String content) {
        if (content == null) return "";
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash).substring(0, 32);
        } catch (Exception e) {
            return "";
        }
    }

    // -- Getters --
    public String getContentId() { return contentId; }
    public ContentType getContentType() { return contentType; }
    public String getContentText() { return contentText; }
    public String getContentUrl() { return contentUrl; }
    public String getContentHash() { return contentHash; }
}
