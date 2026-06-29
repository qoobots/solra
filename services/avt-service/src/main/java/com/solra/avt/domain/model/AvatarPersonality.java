package com.solra.avt.domain.model;

import java.util.List;
import java.util.Map;

public class AvatarPersonality {
    private String personaId;
    private String tone;
    private List<String> traits;
    private String backgroundStory;
    private Map<String, String> customAttributes;

    public String getPersonaId() { return personaId; }
    public void setPersonaId(String personaId) { this.personaId = personaId; }
    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }
    public List<String> getTraits() { return traits; }
    public void setTraits(List<String> traits) { this.traits = traits; }
    public String getBackgroundStory() { return backgroundStory; }
    public void setBackgroundStory(String backgroundStory) { this.backgroundStory = backgroundStory; }
    public Map<String, String> getCustomAttributes() { return customAttributes; }
    public void setCustomAttributes(Map<String, String> customAttributes) { this.customAttributes = customAttributes; }
}
