package com.heima.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.admin.mapper.AdUserMapper;
import com.heima.admin.service.AdUserService;
import com.heima.common.exception.CustException;
import com.heima.model.admin.dtos.AdUserDTO;
import com.heima.model.admin.pojos.AdUser;
import com.heima.model.admin.vos.AdUserVO;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.utils.common.AppJwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class AdUserServiceImpl extends ServiceImpl<AdUserMapper, AdUser> implements AdUserService {
    /**
     * admin 登录
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult login(AdUserDTO dto) {
        //1 参数校验
        if (StringUtils.isBlank(dto.getName()) || StringUtils.isBlank(dto.getPassword())) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID, "参数错误");
        }
        //2 根据用户名查询用户信息
        AdUser adUser = getOne(Wrappers.<AdUser>lambdaQuery()
                .eq(AdUser::getName, dto.getName())
        );
        if (adUser == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "用户名或密码错误");
        }
        if (9 != adUser.getStatus().intValue()) {
            CustException.cust(AppHttpCodeEnum.LOGIN_STATUS_ERROR, "用户状态异常，请联系管理员");
        }
        //3 获取数据库密码和盐， 匹配密码
        String dbPwd = adUser.getPassword(); // 数据库密码（加密）
        String salt = adUser.getSalt();
        // 用户输入密码（加密后）
        String newPwd = DigestUtils.md5DigestAsHex((dto.getPassword() + salt).getBytes());
        if (!dbPwd.equals(newPwd)) {
            CustException.cust(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR, "用户名或密码错误");
        }
        //4 修改登录时间
        adUser.setLoginTime(new Date());
        updateById(adUser);
        //5 颁发token jwt 令牌
        String token = AppJwtUtil.getToken(adUser.getId().longValue());
        // 用户信息返回 VO
        AdUserVO userVO = new AdUserVO();
        BeanUtils.copyProperties(adUser, userVO);
        //6 返回结果（jwt）
        Map map = new HashMap();
        map.put("token", token);
        map.put("user", userVO);
        return ResponseResult.okResult(map);
    }
}