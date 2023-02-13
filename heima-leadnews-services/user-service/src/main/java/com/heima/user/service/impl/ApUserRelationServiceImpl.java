package com.heima.user.service.impl;

import com.heima.common.constants.user.UserRelationConstants;
import com.heima.common.exception.CustException;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.threadlocal.AppThreadLocalUtils;
import com.heima.model.user.dtos.UserRelationDTO;
import com.heima.model.user.pojos.ApUser;
import com.heima.user.service.ApUserRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @author Linxin
 */
@Service
public class ApUserRelationServiceImpl implements ApUserRelationService {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Override
    public ResponseResult follow(UserRelationDTO dto) {
        // 1. 校验参数
        if (dto.getAuthorApUserId() == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID, "作者对应的userId不存在");
        }
        Short operation = dto.getOperation();
        if (operation == null || (operation.intValue() != 0 && operation.intValue() != 1)) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID, "关注类型错误");
        }
        ApUser user = AppThreadLocalUtils.getUser();
        if (user == null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        Integer userId = user.getId();
        Integer followId = dto.getAuthorApUserId();
        if (userId.equals(followId)) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW, "不可以自己关注自己");
        }
        Double score = stringRedisTemplate.opsForZSet()
                .score(UserRelationConstants.FOLLOW_LIST + userId, String.valueOf(followId));
        if (operation.intValue() == 0 && score != null) {
            CustException.cust(AppHttpCodeEnum.DATA_EXIST, "您已关注，请勿重复关注");
        }
        try {
            // 2. 判断operation 是0  是1
            if (operation.intValue() == 0) {
                //    没有关注过    zadd  follow:我的id   作者id
                //                             参数1: key  参数2 集合元素  参数3: score
                stringRedisTemplate.opsForZSet().add(UserRelationConstants.FOLLOW_LIST + userId, String.valueOf(followId), System.currentTimeMillis());
                //                zadd  fans:作者id    我的id
                stringRedisTemplate.opsForZSet().add(UserRelationConstants.FANS_LIST + followId, String.valueOf(userId), System.currentTimeMillis());
            } else {
                // 2.2  是1  取关
                //     zrem  follow:我的id   作者id
                stringRedisTemplate.opsForZSet().remove(UserRelationConstants.FOLLOW_LIST + userId, String.valueOf(followId));
                //     zrem  fans:作者id    我的id
                stringRedisTemplate.opsForZSet().remove(UserRelationConstants.FANS_LIST + followId, String.valueOf(userId));
            }
        } catch (Exception e) {
            e.printStackTrace();
            CustException.cust(AppHttpCodeEnum.SERVER_ERROR);
        }
        return ResponseResult.okResult();
    }
}
