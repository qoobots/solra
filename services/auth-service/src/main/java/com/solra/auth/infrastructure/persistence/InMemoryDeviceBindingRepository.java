package com.solra.auth.infrastructure.persistence;

import com.solra.auth.domain.model.DeviceBinding;
import com.solra.auth.domain.repository.DeviceBindingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AUTH-005: In-memory implementation of DeviceBindingRepository.
 * Production should use PostgreSQL or Redis.
 */
@Component
public class InMemoryDeviceBindingRepository implements DeviceBindingRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryDeviceBindingRepository.class);

    private final Map<String, DeviceBinding> bindingsByUserId = new ConcurrentHashMap<>();

    @Override
    public Optional<DeviceBinding> findByUserId(String userId) {
        return Optional.ofNullable(bindingsByUserId.get(userId));
    }

    @Override
    public DeviceBinding save(DeviceBinding binding) {
        bindingsByUserId.put(binding.getUserId(), binding);
        log.debug("DeviceBinding saved: user={} devices={}", binding.getUserId(), binding.deviceCount());
        return binding;
    }

    @Override
    public void deleteByUserId(String userId) {
        bindingsByUserId.remove(userId);
        log.debug("DeviceBinding deleted for user={}", userId);
    }
}
