package com.solra.grw.domain.repository;

import com.solra.grw.domain.model.Achievement;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 成就定义仓储接口。
 */
public interface AchievementRepository {
    /** 获取全部成就定义 */
    List<Achievement> findAllDefinitions();
    /** 按编码查找 */
    Optional<Achievement> findByCode(String code);
    /** 按分类查找 */
    List<Achievement> findByCategory(Achievement.Category category);
    /** 保存成就定义 */
    Achievement save(Achievement achievement);
    /** 获取总定义数 */
    long count();
}
