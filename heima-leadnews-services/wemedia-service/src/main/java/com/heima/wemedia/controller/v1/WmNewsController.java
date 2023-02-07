package com.heima.wemedia.controller.v1;

import com.heima.common.constants.wemedia.WemediaConstants;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.NewsAuthDTO;
import com.heima.model.wemedia.dtos.WmNewsDTO;
import com.heima.model.wemedia.dtos.WmNewsPageReqDTO;
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

    @ApiOperation(value = "发表文章", notes = "发表文章，保存草稿，修改文章 共用的方法")
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

    @ApiOperation(value = ".", notes = "enable 上架: 1 下架: 0")
    @PostMapping("/down_or_up")
    public ResponseResult downOrUp(@RequestBody WmNewsDTO dto) {
        return wmNewsService.downOrUp(dto);
    }
    /**
     * 查询文章列表
     * @param dto
     * @return
     * 1、按照创建时间降序
     * 2、后端查询的文章不应该包含草稿
     */
    @ApiOperation(value = "查询自媒体文章列表",notes = "返回值带作者信息，主要运营管理端调用")
    @PostMapping("/list_vo")
    public ResponseResult findList(@RequestBody NewsAuthDTO dto) {
        return wmNewsService.findList(dto);
    }

    /**
     * 查询文章详情
     * @param id
     * @return
     */
    @ApiOperation(value = "查询自媒体文章详情",notes = "返回值带作者信息，主要运营管理端调用")
    @GetMapping("/one_vo/{id}")
    public ResponseResult findWmNewsVo(@PathVariable("id") Integer id) {
        return wmNewsService.findWmNewsVo(id);
    }

    /**
     * 文章审核成功
     * @param dto
     * @return
     */
    @ApiOperation("人工审核通过 状态:4")
    @PostMapping("/auth_pass")
    public ResponseResult authPass(@RequestBody NewsAuthDTO dto) {
        return wmNewsService.updateStatus(WemediaConstants.WM_NEWS_AUTH_PASS,dto);
    }
    /**
     * 文章审核失败
     * @param dto
     * @return
     */
    @ApiOperation("人工审核失败 状态:2")
    @PostMapping("/auth_fail")
    public ResponseResult authFail(@RequestBody NewsAuthDTO dto) {
        return wmNewsService.updateStatus(WemediaConstants.WM_NEWS_AUTH_FAIL,dto);
    }
}
