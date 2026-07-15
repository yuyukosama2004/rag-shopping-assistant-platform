package com.biyesheji.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biyesheji.entity.AiIndexTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AiIndexTaskMapper extends BaseMapper<AiIndexTask> {
    @Update("UPDATE t_ai_index_task SET status = 'RUNNING', attempts = #{attempts}, updated_at = NOW() " +
            "WHERE id = #{id} AND status = 'PENDING' AND deleted = 0")
    int claim(@Param("id") Long id, @Param("attempts") int attempts);
}
