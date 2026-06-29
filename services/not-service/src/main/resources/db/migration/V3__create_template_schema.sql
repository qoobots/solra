-- ============================================================
-- V3: 通知模板管理 (NOT-004)
-- 系统通知模板表，支持变量替换、多语言、分类管理
-- ============================================================

CREATE TABLE IF NOT EXISTS notification_templates (
    template_id         VARCHAR(36)     PRIMARY KEY,
    template_code       VARCHAR(64)     NOT NULL UNIQUE,
    name                VARCHAR(128)    NOT NULL,
    type                VARCHAR(32)     NOT NULL,
    default_priority    VARCHAR(16)     NOT NULL DEFAULT 'NORMAL',
    title_template      VARCHAR(256),
    body_template       VARCHAR(1024),
    image_url           VARCHAR(512),
    deep_link_template  VARCHAR(512),
    category            VARCHAR(32),
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    localized_titles    JSONB,
    localized_bodies    JSONB,
    version             INTEGER         NOT NULL DEFAULT 1,
    created_by          VARCHAR(36),
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_templates_code ON notification_templates(template_code);
CREATE INDEX idx_templates_type ON notification_templates(type);
CREATE INDEX idx_templates_category ON notification_templates(category);
CREATE INDEX idx_templates_active ON notification_templates(active);

-- ============================================================
-- 预置 ≥20 种通知场景模板
-- ============================================================

-- 社交类 (6个)
INSERT INTO notification_templates (template_id, template_code, name, type, default_priority, title_template, body_template, category, localized_titles, localized_bodies) VALUES
('tmpl-001', 'follow', '新关注', 'FOLLOW', 'NORMAL',
 '{username} 关注了你', 'TA 正在索拉探索空间，去看看 TA 的空间吧', 'SOCIAL',
 '{"en":"{username} followed you"}',
 '{"en":"{username} is exploring spaces on Solra"}'),

('tmpl-002', 'interaction', '互动通知', 'INTERACTION', 'NORMAL',
 '{username} 与你互动', 'TA 在空间「{space_name}」中与你进行了互动', 'SOCIAL',
 '{"en":"{username} interacted with you"}',
 '{"en":"They interacted with you in space \"{space_name}\""}'),

('tmpl-003', 'session_invite', '空间邀请', 'SESSION_INVITE', 'HIGH',
 '{username} 邀请你加入空间', 'TA 邀请你一起探索「{space_name}」，快来加入吧', 'SOCIAL',
 '{"en":"{username} invites you to a space"}',
 '{"en":"They invite you to explore \"{space_name}\" together"}'),

('tmpl-004', 'session_joined', '好友加入空间', 'SESSION_JOINED', 'LOW',
 '{username} 加入了你的空间', 'TA 正在「{space_name}」中，去打个招呼吧', 'SOCIAL',
 '{"en":"{username} joined your space"}',
 '{"en":"They are in \"{space_name}\", go say hi!"}'),

('tmpl-005', 'friend_request', '好友请求', 'FRIEND_REQUEST', 'NORMAL',
 '{username} 申请添加你为好友', 'TA 想和你在索拉中建立更深度的连接', 'SOCIAL',
 '{"en":"{username} sent you a friend request"}',
 '{"en":"They want to build a deeper connection on Solra"}'),

('tmpl-006', 'space_invite', '空间分享', 'SPACE_INVITE', 'NORMAL',
 '{username} 分享了一个空间给你', '「{space_name}」— 点击探索这个令人惊艳的空间', 'SOCIAL',
 '{"en":"{username} shared a space with you"}',
 '{"en":"\"{space_name}\" — tap to explore this amazing space"}');

-- 创作类 (4个)
INSERT INTO notification_templates (template_id, template_code, name, type, default_priority, title_template, body_template, category, localized_titles, localized_bodies) VALUES
('tmpl-007', 'project_published', '作品发布', 'PROJECT_PUBLISHED', 'HIGH',
 '{username} 发布了新空间「{space_name}」', '去体验 TA 的最新创作吧', 'CREATION',
 '{"en":"{username} published new space \"{space_name}\""}',
 '{"en":"Go experience their latest creation"}'),

('tmpl-008', 'project_liked', '作品被赞', 'PROJECT_LIKED', 'LOW',
 '{username} 喜欢了你的空间', '你的空间「{space_name}」获得了新的喜爱', 'CREATION',
 '{"en":"{username} liked your space"}',
 '{"en":"Your space \"{space_name}\" got a new like"}'),

('tmpl-009', 'asset_uploaded', '资产上传完成', 'ASSET_UPLOADED', 'LOW',
 '资产上传完成', '你上传的 {count} 个资产已处理完毕，可以在编辑器中使用', 'CREATION',
 '{"en":"Assets upload complete"}',
 '{"en":"{count} assets have been processed and are ready in the editor"}'),

('tmpl-010', 'review_result', '审核结果', 'REVIEW_RESULT', 'HIGH',
 '空间「{space_name}」审核结果', '你的空间已通过审核，现已公开发布', 'CREATION',
 '{"en":"Review result for \"{space_name}\""}',
 '{"en":"Your space has been approved and is now publicly available"}');

-- 系统类 (6个)
INSERT INTO notification_templates (template_id, template_code, name, type, default_priority, title_template, body_template, category, localized_titles, localized_bodies) VALUES
('tmpl-011', 'system_announcement', '系统公告', 'SYSTEM_ANNOUNCEMENT', 'HIGH',
 '📢 系统公告', '{body}', 'SYSTEM',
 '{"en":"📢 System Announcement"}',
 '{"en":"{body}"}'),

('tmpl-012', 'system_alert', '系统提醒', 'SYSTEM_ALERT', 'URGENT',
 '⚠️ 系统提醒', '{body}', 'SYSTEM',
 '{"en":"⚠️ System Alert"}',
 '{"en":"{body}"}'),

('tmpl-013', 'achievement', '成就解锁', 'ACHIEVEMENT', 'NORMAL',
 '🎉 成就解锁：{achievement_name}', '恭喜！你解锁了「{achievement_name}」成就', 'SYSTEM',
 '{"en":"🎉 Achievement Unlocked: {achievement_name}"}',
 '{"en":"Congratulations! You unlocked \"{achievement_name}\" achievement"}'),

('tmpl-014', 'faith_level_up', '信仰等级提升', 'FAITH_LEVEL_UP', 'HIGH',
 '🌟 信仰等级提升！', '你在索拉的存在感持续加深，当前信仰等级 Lv.{level}', 'SYSTEM',
 '{"en":"🌟 Faith Level Up!"}',
 '{"en":"Your presence in Solra deepens, current Faith Level: Lv.{level}"}'),

('tmpl-015', 'avatar_remember', '虚拟人的想念', 'INTERACTION', 'HIGH',
 '{avatar_name} 想你了 💭', 'TA 说：「{memory_text}」— 回来看看 TA 吧', 'SYSTEM',
 '{"en":"{avatar_name} misses you 💭"}',
 '{"en":"They said: \"{memory_text}\" — come back and visit"}'),

('tmpl-016', 'welcome_new_user', '新用户欢迎', 'SYSTEM_ANNOUNCEMENT', 'NORMAL',
 '欢迎来到索拉 🌌', '你好 {username}，你的第一个空间已准备就绪，点击探索属于你的世界', 'SYSTEM',
 '{"en":"Welcome to Solra 🌌"}',
 '{"en":"Hello {username}, your first space is ready. Tap to explore your world"}');

-- 商业化 (4个)
INSERT INTO notification_templates (template_id, template_code, name, type, default_priority, title_template, body_template, category, localized_titles, localized_bodies) VALUES
('tmpl-017', 'purchase_success', '购买成功', 'PURCHASE_SUCCESS', 'NORMAL',
 '✅ 购买成功', '你已成功购买「{item_name}」，花费 ¥{amount}', 'COMMERCIAL',
 '{"en":"✅ Purchase Successful"}',
 '{"en":"You purchased \"{item_name}\" for ¥{amount}"}'),

('tmpl-018', 'subscription_expiry', '订阅即将到期', 'SUBSCRIPTION_EXPIRY', 'HIGH',
 '📅 订阅即将到期', '你的 Solra {plan_name} 将在 {days} 天后到期，续费享受不间断体验', 'COMMERCIAL',
 '{"en":"📅 Subscription Expiring Soon"}',
 '{"en":"Your Solra {plan_name} expires in {days} days"}'),

('tmpl-019', 'gift_received', '收到礼物', 'GIFT_RECEIVED', 'NORMAL',
 '🎁 {username} 送了你一份礼物', 'TA 送了你「{gift_name}」，去虚拟物品仓库查看吧', 'COMMERCIAL',
 '{"en":"🎁 {username} sent you a gift"}',
 '{"en":"They sent you \"{gift_name}\", check your inventory"}'),

('tmpl-020', 'creator_earning', '创作者收益', 'PROJECT_PUBLISHED', 'NORMAL',
 '💰 创作者收益到账', '你的空间「{space_name}」本月获得收益 ¥{amount}', 'COMMERCIAL',
 '{"en":"💰 Creator Earnings Received"}',
 '{"en":"Your space \"{space_name}\" earned ¥{amount} this month"}');
