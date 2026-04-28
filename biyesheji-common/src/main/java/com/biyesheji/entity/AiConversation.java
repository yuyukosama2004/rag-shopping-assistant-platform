package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_ai_conversation")
public class AiConversation implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String role;
    private String content;
    private String recommendations;
    private LocalDateTime createdAt;
}
