package com.solra.not.domain.repository;

import com.solra.not.domain.model.DeviceRegistration;
import java.util.List;
import java.util.Optional;

/** 设备注册仓储接口 */
public interface DeviceRegistrationRepository {
    DeviceRegistration save(DeviceRegistration device);
    List<DeviceRegistration> findByUserId(String userId);
    Optional<DeviceRegistration> findByDeviceToken(String deviceToken);
    void updateStatus(String registrationId, String status);
    void delete(String registrationId);
}
