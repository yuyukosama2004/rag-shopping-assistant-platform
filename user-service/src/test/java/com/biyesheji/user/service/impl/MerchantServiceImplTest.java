package com.biyesheji.user.service.impl;

import com.biyesheji.constant.ResultCode;
import com.biyesheji.constant.UserRole;
import com.biyesheji.dto.OwnerInitializeDTO;
import com.biyesheji.dto.StoreSettingUpdateDTO;
import com.biyesheji.entity.StoreSetting;
import com.biyesheji.entity.User;
import com.biyesheji.exception.BizException;
import com.biyesheji.user.mapper.StoreSettingMapper;
import com.biyesheji.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private StoreSettingMapper storeSettingMapper;
    @InjectMocks
    private MerchantServiceImpl merchantService;

    @Test
    void initializeCreatesTheOnlyOwnerAndStore() {
        ReflectionTestUtils.setField(merchantService, "ownerInitToken", "one-time-token");
        when(userMapper.selectCount(any())).thenReturn(0L, 0L);

        OwnerInitializeDTO dto = new OwnerInitializeDTO();
        dto.setUsername("owner");
        dto.setPassword("secure-password");
        dto.setStoreName("示例店铺");
        dto.setServicePhone("13800000000");
        merchantService.initializeOwner("one-time-token", dto);

        ArgumentCaptor<User> ownerCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(ownerCaptor.capture());
        assertEquals(UserRole.OWNER, ownerCaptor.getValue().getRole());
        assertEquals(1, ownerCaptor.getValue().getStatus());
        ArgumentCaptor<StoreSetting> storeCaptor = ArgumentCaptor.forClass(StoreSetting.class);
        verify(storeSettingMapper).insert(storeCaptor.capture());
        assertEquals(StoreSetting.SINGLE_STORE_ID, storeCaptor.getValue().getId());
        assertEquals("示例店铺", storeCaptor.getValue().getStoreName());
    }

    @Test
    void initializeRejectsInvalidTokenBeforeWriting() {
        ReflectionTestUtils.setField(merchantService, "ownerInitToken", "one-time-token");
        BizException exception = assertThrows(BizException.class,
                () -> merchantService.initializeOwner("wrong-token", new OwnerInitializeDTO()));

        assertEquals(ResultCode.FORBIDDEN, exception.getCode());
        verify(userMapper, never()).insert(any(User.class));
        verify(storeSettingMapper, never()).insert(any(StoreSetting.class));
    }

    @Test
    void customerCannotUpdateStoreSetting() {
        User customer = new User();
        customer.setRole(UserRole.CUSTOMER);
        customer.setStatus(1);
        when(userMapper.selectById(1L)).thenReturn(customer);

        BizException exception = assertThrows(BizException.class,
                () -> merchantService.updateStoreSetting(1L, new StoreSettingUpdateDTO()));

        assertEquals(ResultCode.FORBIDDEN, exception.getCode());
        verify(storeSettingMapper, never()).updateById(any(StoreSetting.class));
    }
}
