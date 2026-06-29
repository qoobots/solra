package com.solra.grw.domain.model;

/**
 * DecisiveMomentType 枚举 — 决定性时刻的类型。
 * 定义用户旅程中触发不可逆转化的关键事件类型。
 */
public enum DecisiveMomentType {
    /** 首次与虚拟人对话 */
    FIRST_CONVERSATION,

    /** 首次完成空间探索 */
    FIRST_SPACE_EXPLORED,

    /** 首次添加好友 */
    FIRST_FRIEND_ADDED,

    /** 首次分享内容 */
    FIRST_SHARE,

    /** 首次回访 */
    FIRST_RETURN,

    /** 第十次回访 — 形成习惯的标志 */
    TENTH_RETURN,

    /** 首次创建空间 */
    FIRST_SPACE_CREATED;
}
