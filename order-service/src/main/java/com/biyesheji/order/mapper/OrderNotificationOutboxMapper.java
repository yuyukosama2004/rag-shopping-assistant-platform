package com.biyesheji.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biyesheji.entity.OrderNotificationOutbox;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface OrderNotificationOutboxMapper extends BaseMapper<OrderNotificationOutbox> {
    @Update("UPDATE t_order_notification_outbox SET status='RUNNING', attempts=attempts+1 " +
            "WHERE id=#{id} AND status IN ('PENDING','RETRY')")
    int claim(@Param("id") Long id);

    @Update("UPDATE t_order_notification_outbox SET status='RETRY', next_attempt_at=#{now}, " +
            "last_error='发送进程中断，任务已恢复' WHERE status='RUNNING' AND updated_at < #{staleBefore}")
    int recoverStuck(@Param("staleBefore") LocalDateTime staleBefore, @Param("now") LocalDateTime now);
}
