package com.solra.saf.domain.model;

/**
 * ReportCategory — classification of user reports.
 * Covers: SAF-003 (用户举报→审核→处理闭环).
 */
public enum ReportCategory {
    /** Illegal or regulated content */
    ILLEGAL_CONTENT,
    /** NSFW / pornography */
    NSFW,
    /** Hate speech or discriminatory content */
    HATE_SPEECH,
    /** Harassment or bullying */
    HARASSMENT,
    /** Spam or scam content */
    SPAM,
    /** Copyright / IP infringement */
    COPYRIGHT,
    /** Minor safety concerns */
    MINOR_SAFETY,
    /** Other (user-provided reason) */
    OTHER
}
