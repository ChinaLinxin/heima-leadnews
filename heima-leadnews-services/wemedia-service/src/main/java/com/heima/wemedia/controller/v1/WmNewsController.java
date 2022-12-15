package com.heima.wemedia.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmNewsDTO;
import com.heima.model.wemedia.dtos.WmNewsPageReqDTO;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.wemedia.service.WmNewsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author Linxin
 */
@Api(value = "自媒体文章列表管理", tags = "自媒体文章列表管理")
@RestController
@RequestMapping("/api/v1/news")
public class WmNewsController {

    @Autowired
    private WmNewsService wmNewsService;

    @ApiOperation("根据条件查询文章列表")
    @PostMapping("/list")
    public ResponseResult findList(@RequestBody WmNewsPageReqDTO dto) {
        return wmNewsService.findList(dto);
    }

    @ApiOperation(value = "发表文章",notes = "发表文章，保存草稿，修改文章 共用的方法")
    @PostMapping("/submit")
    public ResponseResult submit(@RequestBody WmNewsDTO dto) {
        return wmNewsService.submitNews(dto);
    }

    @ApiOperation("根据id查询文章")
    @GetMapping("/one/{id}")
    public ResponseResult findNewsById(@PathVariable("id") Integer id) {
        return wmNewsService.findNewsById(id);
    }

    @ApiOperation("根据id删除文章")
    @GetMapping("/del_news/{id}")
    public ResponseResult delNews(@PathVariable("id") Integer id) {
        return wmNewsService.delNews(id);
    }

    @ApiOperation(value = "自媒体文章上架或下架",notes = "enable 上架: 1 下架: 0")
    @PostMapping("/down_or_up")
    public ResponseResult downOrUp(@RequestBody WmNewsDTO dto) {
        return wmNewsService.downOrUp(dto);
    }
}
