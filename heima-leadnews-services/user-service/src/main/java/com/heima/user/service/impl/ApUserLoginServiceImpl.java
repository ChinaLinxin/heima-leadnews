package com.heima.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.LoginDTO;
import com.heima.model.user.pojos.ApUser;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.service.ApUserLoginService;
import com.heima.utils.common.AppJwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Linxin
 */
@Service
public class ApUserLoginServiceImpl implements ApUserLoginService {

    @Autowired
    private ApUserMapper apUserMapper;

    @Override
    public ResponseResult login(LoginDTO dto) {
        // 1. 校验参数
        String phone = dto.getPhone();
        String password = dto.getPassword();
        if (StringUtils.isBlank(password) && StringUtils.isBlank(password)) {
            // 游客登陆
            if (dto.getEquipmentId() == null) {
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
            }
            Map<String, Object> map = new HashMap<>();
            String token = AppJwtUtil.getToken(0L);
            map.put("token", token);
            return ResponseResult.okResult(map);
        }
        // 用户登陆
        ApUser apUser = apUserMapper.selectOne(Wrappers.<ApUser>lambdaQuery().eq(ApUser::getPhone, phone));
        if (apUser == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "请检查手机号");
        }
        // 匹配密码
        String dbPassword = apUser.getPassword();
        String userPassword = DigestUtils.md5DigestAsHex((password + apUser.getSalt()).getBytes(StandardCharsets.UTF_8));
        if (!dbPassword.equals(userPassword)) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "手机号或密码错误");
        }
        Map<String, Object> map = new HashMap<>();
        apUser.setPassword("");
        apUser.setSalt("");
        String token = AppJwtUtil.getToken(apUser.getId().longValue());
        map.put("token", token);
        map.put("user", apUser);
        return ResponseResult.okResult(map);
    }
}
