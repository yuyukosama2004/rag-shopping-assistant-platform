package com.biyesheji.user.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.biyesheji.entity.Address;
import com.biyesheji.exception.BizException;
import com.biyesheji.user.mapper.AddressMapper;
import com.biyesheji.user.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;

    @Override
    @Transactional
    public Address add(Long userId, Address address) {
        address.setId(IdUtil.getSnowflake().nextId());
        address.setUserId(userId);
        // 如果这是第一个地址，设为默认
        Long count = addressMapper.selectCount(
                new LambdaQueryWrapper<Address>().eq(Address::getUserId, userId)
        );
        address.setIsDefault(count == 0 ? 1 : 0);
        addressMapper.insert(address);
        return address;
    }

    @Override
    public Address update(Long userId, Address address) {
        Address db = addressMapper.selectById(address.getId());
        if (db == null || !db.getUserId().equals(userId)) {
            throw new BizException("地址不存在");
        }
        if (address.getReceiverName() != null) db.setReceiverName(address.getReceiverName());
        if (address.getReceiverPhone() != null) db.setReceiverPhone(address.getReceiverPhone());
        if (address.getProvince() != null) db.setProvince(address.getProvince());
        if (address.getCity() != null) db.setCity(address.getCity());
        if (address.getDistrict() != null) db.setDistrict(address.getDistrict());
        if (address.getDetail() != null) db.setDetail(address.getDetail());
        addressMapper.updateById(db);
        return db;
    }

    @Override
    public void delete(Long userId, Long addressId) {
        Address db = addressMapper.selectById(addressId);
        if (db == null || !db.getUserId().equals(userId)) {
            throw new BizException("地址不存在");
        }
        addressMapper.deleteById(addressId);
    }

    @Override
    public Address getById(Long id) {
        return addressMapper.selectById(id);
    }

    @Override
    public List<Address> listByUserId(Long userId) {
        return addressMapper.selectList(
                new LambdaQueryWrapper<Address>()
                        .eq(Address::getUserId, userId)
                        .orderByDesc(Address::getIsDefault)
                        .orderByDesc(Address::getUpdatedAt)
        );
    }

    @Override
    @Transactional
    public void setDefault(Long userId, Long addressId) {
        // 先校验目标地址归属于该用户
        Address target = addressMapper.selectById(addressId);
        if (target == null || !target.getUserId().equals(userId)) {
            throw new BizException("地址不存在");
        }
        // 先取消该用户所有默认地址
        addressMapper.update(null,
                new LambdaUpdateWrapper<Address>()
                        .eq(Address::getUserId, userId)
                        .set(Address::getIsDefault, 0)
        );
        // 再设目标地址为默认
        Address address = new Address();
        address.setId(addressId);
        address.setIsDefault(1);
        addressMapper.updateById(address);
    }
}
