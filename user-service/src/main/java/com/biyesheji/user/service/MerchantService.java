package com.biyesheji.user.service;

import com.biyesheji.dto.OwnerInitializeDTO;
import com.biyesheji.dto.StoreSettingUpdateDTO;
import com.biyesheji.entity.StoreSetting;

public interface MerchantService {
    void initializeOwner(String initToken, OwnerInitializeDTO dto);
    StoreSetting getStoreSetting();
    StoreSetting getMerchantStoreSetting(Long userId);
    StoreSetting updateStoreSetting(Long userId, StoreSettingUpdateDTO dto);
}
