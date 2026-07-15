package com.biyesheji.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biyesheji.entity.AiRequestLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.Map;

@Mapper
public interface AiRequestLogMapper extends BaseMapper<AiRequestLog> {
    @Select("SELECT COALESCE(SUM(estimated_cost), 0) FROM t_ai_request_log " +
            "WHERE deleted = 0 AND created_at >= CURDATE()")
    BigDecimal todayCost();

    @Select("SELECT COUNT(*) AS total, " +
            "SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS success, " +
            "SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed, " +
            "COALESCE(SUM(estimated_cost), 0) AS estimatedCost, " +
            "COALESCE(AVG(duration_ms), 0) AS averageDurationMs " +
            "FROM t_ai_request_log WHERE deleted = 0 AND created_at >= CURDATE()")
    Map<String, Object> todaySummary();
}
