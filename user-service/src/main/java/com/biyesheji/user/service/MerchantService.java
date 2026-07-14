package com.biyesheji.user.service;

import com.biyesheji.dto.OwnerInitializeDTO;
import com.biyesheji.dto.StoreSettingUpdateDTO;
import com.biyesheji.dto.StaffCreateDTO;
import com.biyesheji.dto.StaffStatusUpdateDTO;
import com.biyesheji.dto.StaffPasswordResetDTO;
import com.biyesheji.entity.StoreSetting;
import com.biyesheji.entity.User;

import java.util.List;

public interface MerchantService {
    void initializeOwner(String initToken, OwnerInitializeDTO dto);
    StoreSetting getStoreSetting();
    StoreSetting getMerchantStoreSetting(Long userId);
    StoreSetting updateStoreSetting(Long userId, StoreSettingUpdateDTO dto);
    List<User> listStaff(Long userId);
    User createStaff(Long userId, StaffCreateDTO dto);
    User updateStaffStatus(Long userId, Long staffId, StaffStatusUpdateDTO dto);
    User resetStaffPassword(Long userId, Long staffId, StaffPasswordResetDTO dto);
}
