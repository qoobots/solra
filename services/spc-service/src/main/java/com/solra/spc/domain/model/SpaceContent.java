package com.solra.spc.domain.model;

import java.util.List;

/**
 * SpaceContent 值对象 — 空间内容清单。
 */
public class SpaceContent {
    private String sceneFileUrl;
    private List<SpaceAsset> assets;
    private List<SpaceModule> modules;
    private String entryPoint;

    public long totalAssetBytes() {
        return assets == null ? 0 : assets.stream().mapToLong(SpaceAsset::getSizeBytes).sum();
    }

    public String getSceneFileUrl() { return sceneFileUrl; }
    public void setSceneFileUrl(String sceneFileUrl) { this.sceneFileUrl = sceneFileUrl; }
    public List<SpaceAsset> getAssets() { return assets; }
    public void setAssets(List<SpaceAsset> assets) { this.assets = assets; }
    public List<SpaceModule> getModules() { return modules; }
    public void setModules(List<SpaceModule> modules) { this.modules = modules; }
    public String getEntryPoint() { return entryPoint; }
    public void setEntryPoint(String entryPoint) { this.entryPoint = entryPoint; }
}
