package com.biyesheji.order.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.constant.OrderStatus;
import com.biyesheji.dto.RefundProcessDTO;
import com.biyesheji.dto.RefundRequestDTO;
import com.biyesheji.entity.Order;
import com.biyesheji.entity.OrderOperation;
import com.biyesheji.entity.RefundRecord;
import com.biyesheji.exception.BizException;
import com.biyesheji.order.mapper.OrderMapper;
import com.biyesheji.order.mapper.OrderOperationMapper;
import com.biyesheji.order.mapper.RefundRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundService {
    private final RefundRecordMapper refundRecordMapper;
    private final OrderMapper orderMapper;
    private final OrderOperationMapper orderOperationMapper;

    @Transactional
    public RefundRecord request(Long userId, String orderNo, RefundRequestDTO dto) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null || !userId.equals(order.getUserId())) throw new BizException(404, "订单不存在");
        if (order.getPayTime() == null || !refundable(order.getStatus())) throw new BizException(400, "当前订单不可申请退款");
        if (dto.getAmount().compareTo(order.getTotalAmount()) > 0) throw new BizException(400, "退款金额不能超过订单实付金额");
        if (refundRecordMapper.selectCount(new LambdaQueryWrapper<RefundRecord>().eq(RefundRecord::getOrderNo, orderNo).eq(RefundRecord::getStatus, "PENDING")) > 0) {
            throw new BizException(400, "该订单已有待处理退款申请");
        }
        RefundRecord record = new RefundRecord();
        record.setId(IdUtil.getSnowflake().nextId());
        record.setOrderNo(orderNo);
        record.setUserId(userId);
        record.setAmount(dto.getAmount());
        record.setReason(dto.getReason().trim());
        record.setStatus("PENDING");
        refundRecordMapper.insert(record);
        operation(orderNo, userId, "CUSTOMER_REFUND_REQUEST", "退款申请已提交");
        return record;
    }

    public List<RefundRecord> listForCustomer(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null || !userId.equals(order.getUserId())) throw new BizException(404, "订单不存在");
        return refundRecordMapper.selectList(new LambdaQueryWrapper<RefundRecord>().eq(RefundRecord::getOrderNo, orderNo).orderByDesc(RefundRecord::getCreatedAt));
    }

    public Page<RefundRecord> pageForMerchant(int pageNum, int pageSize, String status) {
        LambdaQueryWrapper<RefundRecord> query = new LambdaQueryWrapper<RefundRecord>().orderByDesc(RefundRecord::getCreatedAt);
        if (status != null && !status.isBlank()) query.eq(RefundRecord::getStatus, status);
        return refundRecordMapper.selectPage(new Page<>(pageNum, pageSize), query);
    }

    @Transactional
    public void approve(Long operatorId, Long id, RefundProcessDTO dto) {
        process(operatorId, id, dto, "APPROVED", "MERCHANT_APPROVE_REFUND", "商家已确认线下退款");
    }

    @Transactional
    public void reject(Long operatorId, Long id, RefundProcessDTO dto) {
        process(operatorId, id, dto, "REJECTED", "MERCHANT_REJECT_REFUND", "商家已拒绝退款申请");
    }

    private void process(Long operatorId, Long id, RefundProcessDTO dto, String status, String action, String note) {
        RefundRecord current = refundRecordMapper.selectById(id);
        if (current == null) throw new BizException(404, "退款记录不存在");
        RefundRecord update = new RefundRecord();
        update.setStatus(status);
        update.setMerchantNote(dto.getMerchantNote() == null ? null : dto.getMerchantNote().trim());
        update.setProcessedBy(operatorId);
        update.setProcessedAt(LocalDateTime.now());
        int rows = refundRecordMapper.update(update, new LambdaUpdateWrapper<RefundRecord>().eq(RefundRecord::getId, id).eq(RefundRecord::getStatus, "PENDING"));
        if (rows == 0) throw new BizException(400, "退款申请已处理");
        operation(current.getOrderNo(), operatorId, action, note);
    }

    private boolean refundable(Integer status) {
        return status != null && (status == OrderStatus.PAID.getCode() || status == OrderStatus.PROCESSING.getCode()
                || status == OrderStatus.SHIPPED.getCode() || status == OrderStatus.COMPLETED.getCode());
    }

    private void operation(String orderNo, Long operatorId, String action, String note) {
        OrderOperation operation = new OrderOperation();
        operation.setId(IdUtil.getSnowflake().nextId());
        operation.setOrderNo(orderNo);
        operation.setOperatorId(operatorId);
        operation.setAction(action);
        operation.setNote(note);
        orderOperationMapper.insert(operation);
    }
}
