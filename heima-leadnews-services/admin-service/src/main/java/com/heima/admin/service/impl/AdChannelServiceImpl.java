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
}