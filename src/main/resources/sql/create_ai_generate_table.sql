-- AI生成任务表
CREATE TABLE IF NOT EXISTS ai_generate_task (
    id BIGINT NOT NULL COMMENT '任务ID',
    userId BIGINT NOT NULL COMMENT '用户ID',
    spaceId BIGINT DEFAULT NULL COMMENT '空间ID（null=公共空间，非null=个人/团队空间）',
    modelName VARCHAR(64) NOT NULL DEFAULT 'z-image-turbo' COMMENT '模型名称',
    prompt TEXT NOT NULL COMMENT '正向提示词',
    negativePrompt TEXT COMMENT '负向提示词',
    paramsJson TEXT COMMENT '生成参数JSON（size, promptExtend, seed）',
    status INT NOT NULL DEFAULT 0 COMMENT '状态：0-PENDING, 1-RUNNING, 2-SUCCESS, 3-FAILED',
    errorMessage TEXT COMMENT '错误信息',
    resultCount INT DEFAULT 0 COMMENT '生成图片数量',
    createTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    INDEX idx_user_space_status (userId, spaceId, status),
    INDEX idx_create_time (createTime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI生成任务表';
