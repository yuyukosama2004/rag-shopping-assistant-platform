package com.biyesheji.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录返回
 */
@Data
public class LoginVO implements Serializable {
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String username;
    private String nickname;
    private Long expiresIn;

    public static LoginVO of(String accessToken, String refreshToken, Long userId,
                              String username, String nickname, Long expiresIn) {
        LoginVO vo = new LoginVO();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setUserId(userId);
        vo.setUsername(username);
        vo.setNickname(nickname);
        vo.setExpiresIn(expiresIn);
        return vo;
    }
}
