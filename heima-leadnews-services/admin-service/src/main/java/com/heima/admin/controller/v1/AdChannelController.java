package com.heima.admin.controller.v1;

import com.heima.admin.service.AdChannelService;
import com.heima.model.admin.dtos.ChannelDTO;
import com.heima.model.common.dtos.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/channel")
@Api(value = "频道管理", tags = "频道管理", description = "频道管理API")
public class AdChannelController {

    @Autowired
    private AdChannelService channelService;

    /**
     * 根据名称分页查询频道列表
     *
     * @param dto
     * @return
     */
    @PostMapping("/list")
    @ApiOperation("频道分页列表查询")
    public ResponseResult findByNameAndPage(@RequestBody ChannelDTO dto) {
        return channelService.findByNameAndPage(dto);
    }
}