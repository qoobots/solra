package com.solra.saf.domain.model;

/**
 * Content types that can be reviewed.
 */
public enum ContentType {
    TEXT, IMAGE, AUDIO, VIDEO,
    AVATAR_SPEECH,      // Virtual avatar speech (SAF-002)
    SPACE_NAME, SPACE_DESCRIPTION,
    USER_PROFILE
}
