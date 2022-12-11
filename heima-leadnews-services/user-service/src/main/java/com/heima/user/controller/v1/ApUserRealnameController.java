package com.heima.user.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.pojos.AuthDTO;
import com.heima.user.service.ApUserRealnameService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(value = "app用户实名认证API",tags = "app用户实名认证API")
@RestController
@RequestMapping("/api/v1/auth")
public class ApUserRealnameController {
    @Autowired
    private ApUserRealnameService userRealnameService;
    @ApiOperation("根据状态查询实名认证列表")
    @PostMapping("/list")
    public ResponseResult loadListByStatus(@RequestBody AuthDTO dto){
        return userRealnameService.loadListByStatus(dto);
    }
}