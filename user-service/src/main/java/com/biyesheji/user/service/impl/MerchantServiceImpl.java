package com.biyesheji.user.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biyesheji.constant.ResultCode;
import com.biyesheji.constant.UserRole;
import com.biyesheji.dto.OwnerInitializeDTO;
import com.biyesheji.dto.StoreSettingUpdateDTO;
import com.biyesheji.dto.StaffCreateDTO;
import com.biyesheji.dto.StaffStatusUpdateDTO;
import com.biyesheji.entity.StoreSetting;
import com.biyesheji.entity.MerchantAuditLog;
import com.biyesheji.entity.User;
import com.biyesheji.exception.BizException;
import com.biyesheji.user.mapper.StoreSettingMapper;
import com.biyesheji.user.mapper.MerchantAuditLogMapper;
import com.biyesheji.user.mapper.UserMapper;
import com.biyesheji.user.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final UserMapper userMapper;
    private final StoreSettingMapper storeSettingMapper;
    private final MerchantAuditLogMapper merchantAuditLogMapper;

    @Value("${merchant.owner-init-token:}")
    private String ownerInitToken;

    @Override
    @Transactional
    public synchronized void initializeOwner(String initToken, OwnerInitializeDTO dto) {
        if (!StringUtils.hasText(ownerInitToken)
                || !StringUtils.hasText(initToken)
                || !MessageDigest.isEqual(ownerInitToken.getBytes(StandardCharsets.UTF_8), initToken.getBytes(StandardCharsets.UTF_8))) {
            throw new BizException(ResultCode.FORBIDDEN, "店主初始化令牌无效");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, UserRole.OWNER)) > 0) {
            throw new BizException(ResultCode.FORBIDDEN, "店主已初始化");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())) > 0) {
            throw new BizException(ResultCode.USER_EXISTS, "用户名已存在");
        }

        User owner = new User();
        owner.setUsername(dto.getUsername());
        owner.setPassword(BCrypt.hashpw(dto.getPassword()));
        owner.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        owner.setEmail(dto.getServiceEmail());
        owner.setPhone(dto.getServicePhone());
        owner.setRole(UserRole.OWNER);
        owner.setStatus(1);
        userMapper.insert(owner);

        StoreSetting setting = new StoreSetting();
        setting.setId(StoreSetting.SINGLE_STORE_ID);
        setting.setStoreName(dto.getStoreName());
        setting.setServicePhone(dto.getServicePhone());
        setting.setServiceEmail(dto.getServiceEmail());
        setting.setBusinessStatus(1);
        storeSettingMapper.insert(setting);
        recordAudit(owner.getId(), "INITIALIZE_STORE", "STORE_SETTING", setting.getId());
    }

    @Override
    public StoreSetting getStoreSetting() {
        return storeSettingMapper.selectById(StoreSetting.SINGLE_STORE_ID);
    }

    @Override
    public StoreSetting getMerchantStoreSetting(Long userId) {
        requireOwner(userId);
        return getStoreSetting();
    }

    @Override
    @Transactional
    public StoreSetting updateStoreSetting(Long userId, StoreSettingUpdateDTO dto) {
        requireOwner(userId);
        StoreSetting setting = getStoreSetting();
        if (setting == null) {
            throw new BizException(ResultCode.NOT_FOUND, "店铺尚未初始化");
        }
        setting.setStoreName(dto.getStoreName());
        setting.setLogo(dto.getLogo());
        setting.setServicePhone(dto.getServicePhone());
        setting.setServiceEmail(dto.getServiceEmail());
        setting.setAddress(dto.getAddress());
        setting.setBusinessStatus(dto.getBusinessStatus() == null ? 1 : dto.getBusinessStatus());
        setting.setShippingNotice(dto.getShippingNotice());
        setting.setAfterSalesNotice(dto.getAfterSalesNotice());
        storeSettingMapper.updateById(setting);
        recordAudit(userId, "UPDATE_STORE_SETTING", "STORE_SETTING", setting.getId());
        return setting;
    }

    @Override
    public List<User> listStaff(Long userId) {
        requireOwner(userId);
        List<User> staff = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, UserRole.STAFF)
                .orderByDesc(User::getCreatedAt));
        staff.forEach(user -> user.setPassword(null));
        return staff;
    }

    @Override
    @Transactional
    public User createStaff(Long userId, StaffCreateDTO dto) {
        requireOwner(userId);
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())) > 0) {
            throw new BizException(ResultCode.USER_EXISTS, "用户名已存在");
        }
        User staff = new User();
        staff.setUsername(dto.getUsername());
        staff.setPassword(BCrypt.hashpw(dto.getPassword()));
        staff.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        staff.setPhone(dto.getPhone());
        staff.setEmail(dto.getEmail());
        staff.setRole(UserRole.STAFF);
        staff.setStatus(1);
        userMapper.insert(staff);
        recordAudit(userId, "CREATE_STAFF", "USER", staff.getId());
        staff.setPassword(null);
        return staff;
    }

    @Override
    @Transactional
    public User updateStaffStatus(Long userId, Long staffId, StaffStatusUpdateDTO dto) {
        requireOwner(userId);
        User staff = userMapper.selectById(staffId);
        if (staff == null || !Integer.valueOf(UserRole.STAFF).equals(staff.getRole())) {
            throw new BizException(ResultCode.USER_NOT_FOUND, "店员不存在");
        }
        staff.setStatus(dto.getStatus());
        userMapper.updateById(staff);
        recordAudit(userId, "UPDATE_STAFF_STATUS", "USER", staffId);
        staff.setPassword(null);
        return staff;
    }

    private void requireOwner(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() == 0 || !Integer.valueOf(UserRole.OWNER).equals(user.getRole())) {
            throw new BizException(ResultCode.FORBIDDEN, "仅店主可执行此操作");
        }
    }

    private void recordAudit(Long operatorId, String action, String resourceType, Long resourceId) {
        MerchantAuditLog log = new MerchantAuditLog();
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setResult("SUCCESS");
        merchantAuditLogMapper.insert(log);
    }
}
