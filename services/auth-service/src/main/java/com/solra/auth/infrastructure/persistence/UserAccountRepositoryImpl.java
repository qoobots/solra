package com.solra.auth.infrastructure.persistence;

import com.solra.auth.domain.model.*;
import com.solra.auth.domain.repository.UserAccountRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JPA implementation of UserAccountRepository.
 * Maps between domain model and JPA entity.
 */
@Component
public class UserAccountRepositoryImpl implements UserAccountRepository {

    private final UserAccountJpaRepository jpaRepository;

    public UserAccountRepositoryImpl(UserAccountJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<UserAccount> findById(String userId) {
        return jpaRepository.findById(userId).map(this::toDomain);
    }

    @Override
    public Optional<UserAccount> findByPhone(String phone) {
        return jpaRepository.findByPhone(phone).map(this::toDomain);
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(this::toDomain);
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public boolean existsByPhone(String phone) {
        return jpaRepository.existsByPhone(phone);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public UserAccount save(UserAccount account) {
        UserAccountEntity entity = toEntity(account);
        entity = jpaRepository.save(entity);
        return toDomain(entity);
    }

    @Override
    public void delete(String userId) {
        jpaRepository.deleteById(userId);
    }

    // -- Mapping methods --

    private UserAccountEntity toEntity(UserAccount domain) {
        UserAccountEntity entity = new UserAccountEntity();
        entity.setUserId(domain.getUserId());
        entity.setUsername(domain.getUsername());
        entity.setDisplayName(domain.getDisplayName());
        entity.setEmail(domain.getEmail());
        entity.setPhone(domain.getPhone());
        entity.setAvatarUrl(domain.getAvatarUrl());
        entity.setPasswordHash(domain.getPasswordHash());
        entity.setStatus(domain.getStatus().name());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setLastLoginAt(domain.getLastLoginAt());
        entity.setRoles(String.join(",", domain.getRoles()));

        RealNameInfo rn = domain.getRealNameInfo();
        if (rn != null) {
            entity.setRealName(rn.getRealName());
            entity.setIdNumberHash(rn.getIdNumber());
            entity.setBirthDate(rn.getBirthDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
            entity.setRealNameVerified(rn.isVerified());
            entity.setRealNameVerifiedAt(rn.getVerifiedAt());
        }

        return entity;
    }

    private UserAccount toDomain(UserAccountEntity entity) {
        UserAccount domain = new UserAccount();
        copyFields(entity, domain);
        return domain;
    }

    private void copyFields(UserAccountEntity entity, UserAccount domain) {
        // Use reflection helper since domain setters are package-private
        java.lang.reflect.Field[] fields = UserAccount.class.getDeclaredFields();
        try {
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                switch (field.getName()) {
                    case "userId" -> field.set(domain, entity.getUserId());
                    case "username" -> field.set(domain, entity.getUsername());
                    case "displayName" -> field.set(domain, entity.getDisplayName());
                    case "email" -> field.set(domain, entity.getEmail());
                    case "phone" -> field.set(domain, entity.getPhone());
                    case "avatarUrl" -> field.set(domain, entity.getAvatarUrl());
                    case "passwordHash" -> field.set(domain, entity.getPasswordHash());
                    case "status" -> field.set(domain, AccountStatus.valueOf(entity.getStatus()));
                    case "linkedAccounts" -> field.set(domain, new ArrayList<>());
                    case "createdAt" -> field.set(domain, entity.getCreatedAt());
                    case "updatedAt" -> field.set(domain, entity.getUpdatedAt());
                    case "lastLoginAt" -> field.set(domain, entity.getLastLoginAt());
                    case "roles" -> {
                        Set<String> roleSet = entity.getRoles() != null && !entity.getRoles().isEmpty()
                                ? Arrays.stream(entity.getRoles().split(",")).collect(Collectors.toSet())
                                : new HashSet<>();
                        field.set(domain, roleSet);
                    }
                    case "metadata" -> field.set(domain, new HashMap<>());
                    case "realNameInfo" -> {
                        if (entity.getRealName() != null && entity.getIdNumberHash() != null && entity.getBirthDate() != null) {
                            RealNameInfo info = RealNameInfo.create(
                                    entity.getRealName(),
                                    entity.getIdNumberHash(),
                                    LocalDate.parse(entity.getBirthDate(), DateTimeFormatter.ISO_LOCAL_DATE)
                            );
                            if (entity.isRealNameVerified()) {
                                info = info.withVerified(true);
                            }
                            field.set(domain, info);
                        }
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to map entity to domain", e);
        }
    }
}
