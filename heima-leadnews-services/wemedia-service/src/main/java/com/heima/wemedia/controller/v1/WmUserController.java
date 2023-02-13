package com.heima.wemedia.controller.v1;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.wemedia.service.WmUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Description:
 * @Version: V1.0
 */
@Api(value = "自媒体用户API", tags = "自媒体用户API")
@RestController
@RequestMapping("/api/v1/user")
public class WmUserController {
    @Autowired
    private WmUserService wmUserService;

    @ApiOperation("保存自媒体用户信息")
    @PostMapping("/save")
    public ResponseResult<WmUser> save(@RequestBody WmUser wmUser) {
        wmUserService.save(wmUser);
        return ResponseResult.okResult(wmUser);
    }

    @ApiOperation("根据名称查询自媒体用户信息")
    @GetMapping("/findByName/{name}")
    public ResponseResult<WmUser> findByName(@PathVariable("name") String name) {
        return ResponseResult.okResult(wmUserService.getOne(Wrappers.<WmUser>lambdaQuery().eq(WmUser::getName, name)));
    }
}