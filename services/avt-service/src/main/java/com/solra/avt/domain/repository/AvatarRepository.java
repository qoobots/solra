package com.solra.avt.domain.repository;

import com.solra.avt.domain.model.Avatar;
import java.util.Optional;

public interface AvatarRepository {
    Optional<Avatar> findById(String avatarId);
    void save(Avatar avatar);
}
