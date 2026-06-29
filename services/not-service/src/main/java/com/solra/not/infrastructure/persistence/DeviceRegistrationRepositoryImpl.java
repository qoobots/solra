package com.solra.not.infrastructure.persistence;

import com.solra.not.domain.model.*;
import com.solra.not.domain.repository.DeviceRegistrationRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class DeviceRegistrationRepositoryImpl implements DeviceRegistrationRepository {

    private final DeviceRegistrationJpaRepository jpaRepo;

    public DeviceRegistrationRepositoryImpl(DeviceRegistrationJpaRepository jpaRepo) { this.jpaRepo = jpaRepo; }

    @Override
    public DeviceRegistration save(DeviceRegistration device) {
        DeviceRegistrationEntity e = toEntity(device);
        DeviceRegistrationEntity saved = jpaRepo.save(e);
        return toDomain(saved);
    }

    @Override
    public List<DeviceRegistration> findByUserId(String userId) {
        return jpaRepo.findByUserId(userId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<DeviceRegistration> findByDeviceToken(String deviceToken) {
        return jpaRepo.findByDeviceToken(deviceToken).map(this::toDomain);
    }

    @Override
    public void updateStatus(String registrationId, String status) {
        jpaRepo.findById(registrationId).ifPresent(e -> { e.setStatus(status); jpaRepo.save(e); });
    }

    @Override
    public void delete(String registrationId) {
        jpaRepo.deleteById(registrationId);
    }

    DeviceRegistration toDomain(DeviceRegistrationEntity e) {
        DeviceRegistration d = new DeviceRegistration();
        d.setRegistrationId(e.getRegistrationId());
        d.setUserId(e.getUserId());
        d.setDeviceToken(e.getDeviceToken());
        d.setPlatform(Platform.valueOf(e.getPlatform()));
        d.setPushProvider(PushProvider.valueOf(e.getPushProvider()));
        d.setDeviceName(e.getDeviceName());
        d.setAppVersion(e.getAppVersion());
        d.setOsVersion(e.getOsVersion());
        d.setStatus(DeviceStatus.valueOf(e.getStatus()));
        d.setCreatedAt(e.getCreatedAt());
        d.setLastUsedAt(e.getLastUsedAt());
        return d;
    }

    DeviceRegistrationEntity toEntity(DeviceRegistration d) {
        DeviceRegistrationEntity e = new DeviceRegistrationEntity();
        e.setRegistrationId(d.getRegistrationId());
        e.setUserId(d.getUserId());
        e.setDeviceToken(d.getDeviceToken());
        e.setPlatform(d.getPlatform().name());
        e.setPushProvider(d.getPushProvider().name());
        e.setDeviceName(d.getDeviceName());
        e.setAppVersion(d.getAppVersion());
        e.setOsVersion(d.getOsVersion());
        e.setStatus(d.getStatus().name());
        e.setCreatedAt(d.getCreatedAt());
        e.setLastUsedAt(d.getLastUsedAt());
        return e;
    }
}
