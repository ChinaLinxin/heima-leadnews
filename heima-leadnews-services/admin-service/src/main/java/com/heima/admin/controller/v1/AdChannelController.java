package com.heima.admin.controller.v1;

import com.heima.admin.service.AdChannelService;
import com.heima.model.admin.dtos.ChannelDTO;
import com.heima.model.admin.pojos.AdChannel;
import com.heima.model.common.dtos.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @ApiOperation("频道新增")
    @PostMapping("/save")
    public ResponseResult insert(@RequestBody AdChannel channel) {
        return channelService.insert(channel);
    }

    @ApiOperation("频道修改")
    @PostMapping("/update")
    public ResponseResult update(@RequestBody AdChannel adChannel) {
        return channelService.update(adChannel);
    }

    @ApiOperation("根据频道ID删除")
    @GetMapping("/del/{id}")
    public ResponseResult deleteById(@PathVariable("id") Integer id) {
        return channelService.deleteById(id);
    }

    @ApiOperation("查询全部频道")
    @GetMapping("/channels")
    public ResponseResult findAll() {
        List<AdChannel> list = channelService.list();
        return ResponseResult.okResult(list);
    }
}