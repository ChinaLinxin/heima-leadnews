package com.heima.admin.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.admin.mapper.AdChannelMapper;
import com.heima.admin.service.AdChannelService;
import com.heima.model.admin.dtos.ChannelDTO;
import com.heima.model.admin.pojos.AdChannel;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class AdChannelServiceImpl extends ServiceImpl<AdChannelMapper, AdChannel> implements AdChannelService {
      @Override
    public ResponseResult findByNameAndPage(ChannelDTO dto) {
        // 1. 校验参数
        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        dto.checkParam(); // 检查分页
        // 2. 封装条件 执行查询
        LambdaQueryWrapper<AdChannel> queryWrapper = Wrappers.<AdChannel>lambdaQuery();

        // 频道名称
        if (StringUtils.isNotBlank(dto.getName())) {
            queryWrapper.like(AdChannel::getName, dto.getName());
        }
        // 状态
        if (dto.getStatus() != null) {
            queryWrapper.eq(AdChannel::getStatus, dto.getStatus());
        }
        // 序号升序
        queryWrapper.orderByAsc(AdChannel::getOrd);
        // 分页
        Page<AdChannel> pageReq = new Page<>(dto.getPage(), dto.getSize());
        // 执行查询
        IPage<AdChannel> pageResult = this.page(pageReq, queryWrapper);
        // 3. 封装响应结果
        PageResponseResult pageResponseResult = new PageResponseResult(dto.getPage(), dto.getSize(), pageResult.getTotal(), pageResult.getRecords());
        return pageResponseResult;
    }

    /**
     * 新增
     * @param adChannel
     * @return
     */
    @Override
    public ResponseResult insert(AdChannel adChannel) {
        // 1 参数校验
        if (adChannel == null || StringUtils.isBlank(adChannel.getName())) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        // 2 判断该频道是否存在

        int count = this.count(Wrappers.<AdChannel>lambdaQuery()
                .eq(AdChannel::getName, adChannel.getName()));
        if(count > 0){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST,"该频道已存在");
        }
        // 3 执行新增
        adChannel.setCreatedTime(new Date());
        save(adChannel);
        // 4 返回结果
        return ResponseResult.okResult();
    }

    /**
     * 修改频道及频道状态
     * @param adChannel
     * @return
     */
    @Override
    public ResponseResult update(AdChannel adChannel) {
        //1 参数校验
        if (adChannel == null || adChannel.getId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2 执行修改
        AdChannel channel = getById(adChannel.getId());
        if (channel == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"频道信息不存在");
        }
        //3. 校验名称唯一性
        if(StringUtils.isNotBlank(adChannel.getName())
                &&
                !adChannel.getName().equals(channel.getName())){
            int count = this.count(Wrappers.<AdChannel>lambdaQuery()
                    .eq(AdChannel::getName, adChannel.getName()));
            if(count > 0){
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST,"该频道已存在");
            }
        }
        updateById(adChannel);
        //4 返回结果
        return ResponseResult.okResult();
    }

    @Override
    public ResponseResult deleteById(Integer id) {

        //1.检查参数
        if(id == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.判断当前频道是否存在 和 是否有效
        AdChannel adChannel = getById(id);
        if(adChannel==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        // 启用状态下不能删除
        if (adChannel.getStatus()) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        //3.删除频道
        removeById(id);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}