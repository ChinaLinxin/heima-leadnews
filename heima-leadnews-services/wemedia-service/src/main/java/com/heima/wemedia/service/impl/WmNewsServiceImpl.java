package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.message.NewsAutoScanConstants;
import com.heima.common.constants.wemedia.WemediaConstants;
import com.heima.common.exception.CustException;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.threadlocal.WmThreadLocalUtils;
import com.heima.model.wemedia.dtos.NewsAuthDTO;
import com.heima.model.wemedia.dtos.WmNewsDTO;
import com.heima.model.wemedia.dtos.WmNewsPageReqDTO;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.model.wemedia.vos.WmNewsVO;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Linxin
 */
@Service
@Slf4j
public class WmNewsServiceImpl extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {

    @Value("${file.oss.web-site}")
    private String webSite;

    @Resource
    RabbitTemplate rabbitTemplate;

    @Override
    public ResponseResult findList(WmNewsPageReqDTO dto) {
        // 1.參數校驗
        if (dto == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        dto.checkParam();
        // 2.封装条件
        LambdaQueryWrapper<WmNews> queryWrapper = new LambdaQueryWrapper<>();
        // 模糊查询文章标题
        queryWrapper.like(StringUtils.isNotBlank(dto.getKeyword()), WmNews::getTitle, dto.getKeyword());
        // 频道id
        queryWrapper.eq(dto.getChannelId() != null, WmNews::getChannelId, dto.getChannelId());
        // 文章状态
        queryWrapper.eq(dto.getStatus() != null, WmNews::getStatus, dto.getStatus());
        // 发布时间 >= 开始时间
        queryWrapper.ge(dto.getBeginPubDate() != null, WmNews::getPublishTime, dto.getBeginPubDate());
        // 发布时间 <= 开始时间
        queryWrapper.le(dto.getEndPubDate() != null, WmNews::getPublishTime, dto.getEndPubDate());
        // 登录用户
        WmUser user = WmThreadLocalUtils.getUser();
        if (user == null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        queryWrapper.eq(WmNews::getUserId, user.getId());
        queryWrapper.orderByDesc(WmNews::getCreatedTime);
        // 分页条件构造
        Page<WmNews> page = new Page<>(dto.getPage(), dto.getSize());
        // 3.执行查询
        IPage<WmNews> pageResult = this.page(page, queryWrapper);
        PageResponseResult result = new PageResponseResult(dto.getPage(), dto.getSize(), pageResult.getTotal(), pageResult.getRecords());
        // 处理文章图片
        result.setHost(webSite);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult submitNews(WmNewsDTO dto) {
        // 1.参数校验
        if (dto == null || dto.getContent() == null || dto.getTitle() == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID, "文章内容或标题不能为空");
        }
        WmUser user = WmThreadLocalUtils.getUser();
        if (user == null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        // 2.保存文章或修改文章
        WmNews wmNews = new WmNews();
        BeanUtils.copyProperties(dto, wmNews);
        if (WemediaConstants.WM_NEWS_TYPE_AUTO.equals(dto.getType())) {
            wmNews.setType(null);
        }
        if (!CollectionUtils.isEmpty(dto.getImages())) {
            wmNews.setImages(imagesToStr(dto.getImages()));
        }
        // 发表文章的用户
        wmNews.setUserId(user.getId());
        // 2.保存或修改 wmnews
        saveWmNews(wmNews);
        if (WemediaConstants.WM_NEWS_DRAFT_STATUS.equals(dto.getStatus())) {
            // 如果是草稿，不用保存关联关系
            return ResponseResult.okResult();
        }
        // 3. 判断如果是提交待审核状态保存素材和文章的关联关系
        // 3.1   解析出 内容当中所引用的素材列表
        List<String> urlList = parseContentImages(dto.getContent());
        // 3.2   保存文章内容 和 素材的关联关系
        if (!CollectionUtils.isEmpty(urlList)) {
            saveRelativeInfo(urlList, wmNews.getId(), WemediaConstants.WM_CONTENT_REFERENCE);
        }
        // 3.3 保存文章封面 和 素材的关联关系
        saveRelativeInfoForCover(urlList, wmNews, dto);

        rabbitTemplate.convertAndSend(NewsAutoScanConstants.WM_NEWS_AUTO_SCAN_QUEUE,wmNews.getId());
        log.info("成功发送 待审核消息 ==> 队列:{}, 文章id:{}",NewsAutoScanConstants.WM_NEWS_AUTO_SCAN_QUEUE,wmNews.getId());

        return ResponseResult.okResult();
    }

    @Override
    public ResponseResult findNewsById(Integer id) {
        // 1.参数校验
        if (id == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmNews wmNews = this.getById(id);
        if (wmNews == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "文章存不存在哦~");
        }
        ResponseResult responseResult = ResponseResult.okResult(wmNews);
        responseResult.setHost(webSite);
        return responseResult;
    }

    @Override
    public ResponseResult delNews(Integer id) {
        // 校验参数
        if (id == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmUser user = WmThreadLocalUtils.getUser();
        if (user == null || user.getId() == null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        WmNews wmNews = this.getById(id);
        if (wmNews == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "文章不存在");
        }
        // 判断当前文章状态 status==9 && enable == 1
        if (WemediaConstants.WM_NEWS_PUBLISH_STATUS.equals(wmNews.getStatus()) && WemediaConstants.WM_NEWS_UP.equals(wmNews.getEnable())) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW, "文章已发布，不能删除");
        }
        // 去除素材与文章的关系
        wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId, wmNews.getId()));
        // 删除文章
        this.removeById(id);

        return ResponseResult.okResult();
    }

    @Override
    public ResponseResult downOrUp(WmNewsDTO dto) {
        // 参数校验
        if (dto == null || dto.getId() == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        Short enable = dto.getEnable();
        if (enable == null || (!WemediaConstants.WM_NEWS_UP.equals(enable) && !WemediaConstants.WM_NEWS_DOWN.equals(enable))) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID, "上下架状态错误");
        }
        // 查询文章
        WmNews wmNews = this.getById(dto.getId());
        if (wmNews == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "文章不存在");
        }
        // 判断文章是否发布
        if (!WemediaConstants.WM_NEWS_PUBLISH_STATUS.equals(wmNews.getStatus())) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW, "当前文章不是发布状态，不能上下架");
        }
        update(Wrappers.<WmNews>lambdaUpdate().eq(WmNews::getId,dto.getId())
                .set(WmNews::getEnable,dto.getEnable()));

        return ResponseResult.okResult();
    }

    @Resource
    private WmNewsMaterialMapper wmNewsMaterialMapper;

    private void saveWmNews(WmNews wmNews) {
        // 1.补全参数
        wmNews.setCreatedTime(new Date());
        wmNews.setSubmitedTime(new Date());
        // 2.上下架
        wmNews.setEnable(WemediaConstants.WM_NEWS_UP);
        if (wmNews.getId() == null) {
            this.save(wmNews);
        } else {
            wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId, wmNews.getId()));
            updateById(wmNews);
        }
    }

    /**
     * 将图片集合 转为字符串
     *
     * @param images
     * @return
     */
    private String imagesToStr(List<String> images) {
        return images.stream().map(url -> url.replaceAll(webSite, ""))
                .collect(Collectors.joining(","));
    }

    private List<String> parseContentImages(String content) {
        List<Map> contentMapList = JSON.parseArray(content, Map.class);
        return contentMapList.stream()
                // 筛选 type 为 image 的数据
                .filter(m -> WemediaConstants.WM_NEWS_TYPE_IMAGE.equals(m.get("type")))
                // 获取 type为image 这个map对象 中的value的值 得到素材全路径
                .map(m -> m.get("value").toString())
                // 替换掉 前缀路径website
                .map(url -> url.replaceAll(webSite, ""))
                // 去除重复的素材路径
                .distinct()
                // 将素材收集到集合中
                .collect(Collectors.toList());
    }

    @Resource
    private WmMaterialMapper wmMaterialMapper;

    /**
     * 保存关联关系的核心方法
     *
     * @param urlList 素材路径集合
     * @param newsId  文章id
     * @param type    类型:   0 内容引用      1  封面引用
     */
    public void saveRelativeInfo(List<String> urlList, Integer newsId, Short type) {
        // 1. 根据素材路径url集合   查询出对应的素材id列表
//        List<WmMaterial> wmMaterials = wmMaterialMapper.selectList(Wrappers.<WmMaterial>lambdaQuery()
//                .eq(WmMaterial::getUserId, WmThreadLocalUtils.getUser().getId())
//                .in(WmMaterial::getUrl, urlList)
//                .select(WmMaterial::getId)
//        );
        List<Integer> ids = wmMaterialMapper.selectRelationsIds(urlList, WmThreadLocalUtils.getUser().getId());
        // 2. 判断 id列表  长度  是否小于 素材列表长度  如果小于 说明缺失素材
        if (CollectionUtils.isEmpty(ids) || ids.size() < urlList.size()) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "素材缺失，保存关联关系失败");
        }
        // 3. 根据id列表  newsId  type  保存关联关系 到 wm_news_material
        wmNewsMaterialMapper.saveRelations(ids, newsId, type);
//        for (WmMaterial wmMaterial : wmMaterials) {
//            WmNewsMaterial wmNewsMaterial = new WmNewsMaterial();
//            wmNewsMaterial.setMaterialId(wmMaterial.getId());
//            wmNewsMaterial.setNewsId(newsId);
//            wmNewsMaterial.setType(type);
//            wmNewsMaterialMapper.insert(wmNewsMaterial);
//        }
    }

    /**
     * 保存封面 和 素材的关联关系
     *
     * @param urlList
     * @param wmNews
     * @param dto
     */
    private void saveRelativeInfoForCover(List<String> urlList, WmNews wmNews, WmNewsDTO dto) {
        // 1. 获取dto中 传入的封面集合
        List<String> images = dto.getImages();
        // 2. 判断dto中的type属性是否为 -1
        if (WemediaConstants.WM_NEWS_TYPE_AUTO.equals(dto.getType())) {
            //     2.1  如果是-1  需要自动生成封面
            //     2.2  根据内容中素材列表生成封面
            int size = urlList.size();
            if (size > 0 && size <= 2) {
                //         如果内容素材数量大于 0   小于 等于2   生成单图封面
                images = urlList.stream().limit(1).collect(Collectors.toList());
                wmNews.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);
            } else if (size > 2) {
                //         如果内容素材数量大于 0   大于  2   生成多图封面
                images = urlList.stream().limit(3).collect(Collectors.toList());
                wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
            } else {
                //         如果内容素材数量为 0      生成无图封面
                wmNews.setType(WemediaConstants.WM_NEWS_NONE_IMAGE);
            }
            //         重新修改wmNews
            if (!CollectionUtils.isEmpty(images)) {
                wmNews.setImages(imagesToStr(images));
            }
            updateById(wmNews);
        }
        // 3. 批量保存 封面 和 素材的关联关系
        if (!CollectionUtils.isEmpty(images)) {
            // 如果封面不是自动生成的，是前端直接传过来的 会有前缀路径 记得替换掉
            images = images.stream().map(url -> url.replaceAll(webSite, "")).collect(Collectors.toList());
            saveRelativeInfo(images, wmNews.getId(), WemediaConstants.WM_IMAGE_REFERENCE);
        }
    }

    @Resource
    WmNewsMapper wmNewsMapper;
    /**
     * 查询文章列表
     * @param dto
     * @return
     */
    @Override
    public ResponseResult findList(NewsAuthDTO dto) {
        //1.检查参数
        dto.checkParam();
        //记录当前页
        int currentPage = dto.getPage();
        //设置起始页
        dto.setPage((dto.getPage()-1)*dto.getSize());
        if(StringUtils.isNotBlank(dto.getTitle())){
            dto.setTitle("%"+dto.getTitle()+"%");
        }

        //2.分页查询
        List<WmNewsVO> wmNewsVoList = wmNewsMapper.findListAndPage(dto);
        //统计多少条数据
        long count = wmNewsMapper.findListCount(dto);

        //3.结果返回
        ResponseResult result = new PageResponseResult(currentPage, dto.getSize(), count, wmNewsVoList);
        result.setHost(webSite);
        return result;
    }

    @Resource
    WmUserMapper wmUserMapper;
    /**
     * 查询文章详情
     * @param id
     * @return
     */
    @Override
    public ResponseResult findWmNewsVo(Integer id) {
        //1参数检查
        if(id == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.查询文章信息
        WmNews wmNews = getById(id);
        if(wmNews == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        //3.查询作者
        WmUser wmUser = null;
        if(wmNews.getUserId() != null){
            wmUser = wmUserMapper.selectById(wmNews.getUserId());
        }

        //4.封装vo信息返回
        WmNewsVO wmNewsVo = new WmNewsVO();
        BeanUtils.copyProperties(wmNews,wmNewsVo);
        if(wmUser != null){
            wmNewsVo.setAuthorName(wmUser.getName());
        }
        ResponseResult responseResult = ResponseResult.okResult(wmNewsVo);
        responseResult.setHost(webSite);
        return responseResult;
    }

    /**
     * 自媒体文章人工审核
     * @param status 2  审核失败  4 审核成功
     * @param dto
     * @return
     */
    @Override
    public ResponseResult updateStatus(Short status, NewsAuthDTO dto) {
        //1.参数检查
        if(dto == null || dto.getId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.查询文章
        WmNews wmNews = getById(dto.getId());
        if(wmNews == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        // 检查文章状态 不能为9  已发布
        if (wmNews.getStatus().equals(WmNews.Status.PUBLISHED.getCode())) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW,"文章已发布");
        }
        //3.修改文章状态
        wmNews.setStatus(status);
        if(StringUtils.isNotBlank(dto.getMsg())){
            wmNews.setReason(dto.getMsg());
        }
        updateById(wmNews);

        // TODO 通知定时发布文章

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
