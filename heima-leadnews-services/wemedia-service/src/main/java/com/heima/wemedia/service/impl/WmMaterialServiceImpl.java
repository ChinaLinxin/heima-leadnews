package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.exception.CustException;
import com.heima.file.service.FileStorageService;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.threadlocal.WmThreadLocalUtils;
import com.heima.model.wemedia.dtos.WmMaterialDTO;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmMaterialService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author Linxin
 */
@Service
@Slf4j
public class WmMaterialServiceImpl extends ServiceImpl<WmMaterialMapper, WmMaterial> implements WmMaterialService {

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${file.oss.prefix}")
    private String prefix;

    @Value("${file.oss.web-site}")
    private String webSite;

    @Override
    public ResponseResult uploadPicture(MultipartFile multipartFile) {
        // 1.参数校验
        if (multipartFile == null || multipartFile.getSize() == 0) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID, "请上传正确的文件");
        }
        // 获取当前线程中的用户ID
        WmUser wmUser = WmThreadLocalUtils.getUser();
        if (wmUser == null) {
            CustException.cust(AppHttpCodeEnum.NO_OPERATOR_AUTH);
        }
        String originalFilename = multipartFile.getOriginalFilename();
        if (!checkFileSuffix(originalFilename)) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID, "请上传正确的素材格式[jpg,jpeg,png,gif]");
        }
        // 2 上传到oss
        String fileId = null;
        try {
            String fileName = UUID.randomUUID().toString().replaceAll("-", "");
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            fileId = fileStorageService.store(prefix, fileName + suffix, multipartFile.getInputStream());
            log.info("阿里云OSS 文件 fileId: {}", fileId);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("阿里云文件上传失败 uploadPicture error: {}", e);
            CustException.cust(AppHttpCodeEnum.SERVER_ERROR, "服务器繁忙请稍后重试");
        }
        // 3 封装数据并保持到素材库中
        WmMaterial wmMaterial = new WmMaterial();
        wmMaterial.setIsCollection((short) 0);
        wmMaterial.setType((short) 0);
        wmMaterial.setCreatedTime(new Date());
        // 设置文件id
        wmMaterial.setUrl(fileId);
        wmMaterial.setUserId(wmUser.getId());
        save(wmMaterial);
        // 前端显示
        wmMaterial.setUrl(webSite + fileId);
        // 4 返回结果
        return ResponseResult.okResult(wmMaterial);
    }

    @Resource
    private WmNewsMaterialMapper wmNewsMaterialMapper;

    @Override
    public ResponseResult findList(WmMaterialDTO dto) {
        // 1.检查参数
        dto.checkParam();
        // 2.根据参数进行条件查询
        LambdaQueryWrapper<WmMaterial> queryWrapper = new LambdaQueryWrapper<>();
        if (dto.getIsCollection() != null && dto.getIsCollection() == 1) {
            queryWrapper.eq(WmMaterial::getIsCollection, dto.getIsCollection());
        }
        // 查询当前登录用户的素材
        WmUser wmUser = WmThreadLocalUtils.getUser();
        if (wmUser == null) {
            CustException.cust(AppHttpCodeEnum.NO_OPERATOR_AUTH);
        }
        queryWrapper.eq(WmMaterial::getUserId, wmUser.getId());
        // 时间倒序
        queryWrapper.orderByDesc(WmMaterial::getCreatedTime);
        IPage<WmMaterial> pageParam = new Page<>(dto.getPage(), dto.getSize());
        IPage<WmMaterial> pageResult = this.page(pageParam, queryWrapper);
        List<WmMaterial> records = pageParam.getRecords();
        for (WmMaterial record : records) {
            record.setUrl(webSite + record.getUrl());
        }
        // 封装结果
        return new PageResponseResult(dto.getPage(), dto.getSize(), pageResult.getTotal(), pageResult.getRecords());
    }

    @Override
    public ResponseResult delPicture(Integer id) {
        // 1.参数校验
        if (id == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        // 2.业务处理
        WmMaterial wmMaterial = this.getById(id);
        if (wmMaterial == null) {
            // 素材不存在
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        LambdaQueryWrapper<WmNewsMaterial> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmNewsMaterial::getMaterialId, id);
        Integer count = wmNewsMaterialMapper.selectCount(queryWrapper);
        if (count > 0) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW);
        }
        this.removeById(id);
        fileStorageService.delete(wmMaterial.getUrl());
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }


    @Override
    public ResponseResult updateStatus(Integer id, Short type) {
        //1.检查参数
        if(id == null){
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.更新状态
        WmMaterial material = getById(id);
        if (material == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"素材信息不存在");
        }
        //获取当前用户信息
        Integer uid = WmThreadLocalUtils.getUser().getId();
        if(!material.getUserId().equals(uid)){
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW,"只允许收藏自己上传的素材");
        }
        material.setIsCollection(type);
        updateById(material);
//        update(Wrappers.<WmMaterial>lambdaUpdate()  // 如果只想修改指定字段 可以使用此方法
//                .set(WmMaterial::getIsCollection,type)
//                .eq(WmMaterial::getId,id)
//                .eq(WmMaterial::getUserId,uid));
        return ResponseResult.okResult();
    }

    /**
     * 检查文件格式 目前仅支持 jpg  jpeg  png  gif 图片的上传
     *
     * @param path
     * @return
     */
    private boolean checkFileSuffix(String path) {
        if (StringUtils.isBlank(path)) return false;
        List<String> allowSuffix = Arrays.asList("jpg", "jpeg", "png", "gif");
        boolean isAllow = false;
        for (String suffix : allowSuffix) {
            if (path.endsWith(suffix)) {
                isAllow = true;
                break;
            }
        }
        return isAllow;
    }
}
