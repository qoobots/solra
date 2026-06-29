package com.solra.crt.interfaces.rest;

import com.solra.crt.application.dto.AssetDTO;
import com.solra.crt.application.dto.PageResult;
import com.solra.crt.application.service.AssetApplicationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 资产 REST 控制器。
 * 对应 Proto SpaceCreationService 中资产 CRUD 相关的 RPC。
 */
@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final AssetApplicationService assetService;

    public AssetController(AssetApplicationService assetService) {
        this.assetService = assetService;
    }

    /**
     * 上传资产。
     * POST /api/v1/assets
     */
    @PostMapping
    public ResponseEntity<AssetDTO> uploadAsset(@RequestBody Map<String, Object> request) {
        String spaceId = (String) request.get("space_id");
        String name = (String) request.get("name");
        String type = (String) request.getOrDefault("type", "MODEL_3D");
        String description = (String) request.get("description");

        AssetDTO asset = assetService.uploadAsset(spaceId, extractUserId(), name, type, description);
        return ResponseEntity.status(HttpStatus.CREATED).body(asset);
    }

    /**
     * 获取资产详情。
     * GET /api/v1/assets/{assetId}
     */
    @GetMapping("/{assetId}")
    public ResponseEntity<AssetDTO> getAsset(@PathVariable String assetId) {
        AssetDTO asset = assetService.getAsset(assetId);
        return ResponseEntity.ok(asset);
    }

    /**
     * 列出资产（分页）。
     * GET /api/v1/assets?space_id=xxx&type=MODEL_3D&page=1&page_size=20
     */
    @GetMapping
    public ResponseEntity<PageResult<AssetDTO>> listAssets(
            @RequestParam("space_id") String spaceId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize) {
        PageResult<AssetDTO> result = assetService.listAssets(spaceId, type, page, pageSize);
        return ResponseEntity.ok(result);
    }

    /**
     * 删除资产。
     * DELETE /api/v1/assets/{assetId}
     */
    @DeleteMapping("/{assetId}")
    public ResponseEntity<Void> deleteAsset(@PathVariable String assetId) {
        assetService.deleteAsset(assetId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 发布资产。
     * POST /api/v1/assets/{assetId}/publish
     */
    @PostMapping("/{assetId}/publish")
    public ResponseEntity<AssetDTO> publishAsset(@PathVariable String assetId) {
        AssetDTO asset = assetService.publishAsset(assetId);
        return ResponseEntity.ok(asset);
    }

    private String extractUserId() {
        // TODO: Extract from JWT token / auth context
        return "default-user";
    }
}
