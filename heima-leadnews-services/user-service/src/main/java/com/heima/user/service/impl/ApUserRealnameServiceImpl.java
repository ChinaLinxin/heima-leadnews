package com.heima.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.exception.CustomException;
import com.heima.model.admin.pojos.ApUserRealname;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.pojos.AuthDTO;
import com.heima.user.mapper.ApUserRealnameMapper;
import com.heima.user.service.ApUserRealnameService;
import org.springframework.stereotype.Service;

@Service
public class ApUserRealnameServiceImpl extends ServiceImpl<ApUserRealnameMapper, ApUserRealname> implements ApUserRealnameService {
    /**
     * 查询列表
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult loadListByStatus(AuthDTO dto) {
        // 1 参数检查
        if (dto == null) {
            throw new CustomException(AppHttpCodeEnum.PARAM_INVALID);
        }
        dto.checkParam();

        // 2 条件查询
        Page<ApUserRealname> page = new Page<>(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<ApUserRealname> lambdaQueryWrapper = new LambdaQueryWrapper();

        if (dto.getStatus() != null) {
            lambdaQueryWrapper.eq(ApUserRealname::getStatus, dto.getStatus());
        }

        IPage<ApUserRealname> resultPage = page(page, lambdaQueryWrapper);

        // 3 返回结果
        return new PageResponseResult(dto.getPage(), dto.getSize(),
                resultPage.getTotal(), resultPage.getRecords());
    }
}