package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.exception.CustException;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmUserDTO;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.model.wemedia.vos.WmUserVO;
import com.heima.utils.common.AppJwtUtil;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class WmUserServiceImpl extends ServiceImpl<WmUserMapper, WmUser> implements WmUserService {
    @Override
    public ResponseResult login(WmUserDTO dto) {
        // 1.检查参数
        if (StringUtils.isBlank(dto.getName()) || StringUtils.isBlank(dto.getPassword())) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID, "用户名或密码不能为空");
        }
        // 2.根据用户名查询用户信息
        WmUser wmUser = this.getOne(Wrappers.<WmUser>lambdaQuery().eq(WmUser::getName, dto.getName()));
        if (wmUser == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        // 3.比对密码
        String inputPwd = DigestUtils.md5DigestAsHex((dto.getPassword() + wmUser.getSalt()).getBytes());
        if (!inputPwd.equals(wmUser.getPassword())) {
            CustException.cust(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR);
        }
        // 4.检查用户状态
        if (9 != wmUser.getStatus()) {
            CustException.cust(AppHttpCodeEnum.LOGIN_STATUS_ERROR);
        }
        // 修改最近一次登录时间
        wmUser.setLoginTime(new Date());
        this.updateById(wmUser);
        // 5.颁发token
        String token = AppJwtUtil.getToken(wmUser.getId().longValue());
        // 用户信息脱敏
        WmUserVO wmUserVO = new WmUserVO();
        BeanUtils.copyProperties(wmUser, wmUserVO);

        // 封装参数返回
        Map<String, Object> map = new HashMap<>();
        map.put("token", token);
        map.put("user", wmUserVO);
        return ResponseResult.okResult(map);
    }
}