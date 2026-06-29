package com.solra.auth.domain.repository;

import com.solra.auth.domain.model.DeviceBinding;

import java.util.Optional;

/**
 * AUTH-005: Repository interface for DeviceBinding aggregate.
 */
public interface DeviceBindingRepository {
    Optional<DeviceBinding> findByUserId(String userId);
    DeviceBinding save(DeviceBinding binding);
    void deleteByUserId(String userId);
}
