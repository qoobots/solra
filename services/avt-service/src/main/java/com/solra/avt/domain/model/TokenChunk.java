package com.solra.avt.domain.model;

/**
 * TokenChunk 值对象 — 流式响应的单个token块。
 */
public class TokenChunk {

    private int sequence;
    private String token;
    private boolean isFinal;

    public TokenChunk() {}

    public TokenChunk(int sequence, String token, boolean isFinal) {
        this.sequence = sequence;
        this.token = token;
        this.isFinal = isFinal;
    }

    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public boolean isFinal() { return isFinal; }
    public void setFinal(boolean isFinal) { this.isFinal = isFinal; }
}
