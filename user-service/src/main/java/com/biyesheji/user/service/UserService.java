package com.biyesheji.user.service;

import com.biyesheji.dto.RegisterDTO;
import com.biyesheji.entity.User;
import com.biyesheji.vo.LoginVO;
import com.biyesheji.user.vo.AccountDataExportVO;

public interface UserService {
    LoginVO login(String username, String password);
    User register(RegisterDTO dto);
    User getById(Long userId);
    User updateInfo(Long userId, User user);
    LoginVO refreshToken(String refreshToken);
    void logout(Long userId);
    AccountDataExportVO exportAccountData(Long userId);
    void deleteAccount(Long userId, String password, String confirmation);
}
