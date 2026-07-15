package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_ai_knowledge")
public class AiKnowledge extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String category;
    private String title;
    private String content;
    private Integer status;
    private Integer sortOrder;
}
