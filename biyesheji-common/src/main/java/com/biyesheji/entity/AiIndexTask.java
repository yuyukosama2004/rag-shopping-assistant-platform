package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_ai_index_task")
public class AiIndexTask extends BaseEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long productId;
    private String operation;
    private LocalDateTime productUpdatedAt;
    private String status;
    private Integer attempts;
    private String errorMessage;
    private LocalDateTime processedAt;
}
