package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.heima.aliyun.GreenImageScan;
import com.heima.aliyun.GreenTextScan;
import com.heima.common.constants.message.PublishArticleConstants;
import com.heima.common.exception.CustException;
import com.heima.feigns.AdminFeign;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.utils.common.SensitiveWordUtil;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Linxin
 */
@Service
@Slf4j
public class WmNewsAutoScanServiceImpl implements WmNewsAutoScanService {
    @Value("${file.oss.web-site}")
    String webSite;
    @Autowired
    GreenImageScan greenImageScan;
    @Autowired
    GreenTextScan greenTextScan;
    @Autowired
    AdminFeign adminFeign;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Resource
    private WmNewsMapper wmNewsMapper;

    /**
     * 自动审核方法
     *
     * @param wmNewsId
     */
    @Override
    public void autoScanWmNews(Integer wmNewsId) {
        log.info(" 自动审核发布方法 被调用   当前审核发布的文章id==> {}", wmNewsId);
        //1. 根据文章id 远程调用feign查询文章
        if (wmNewsId == null) {
            log.error("自动审核文章失败    文章id为空");
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmNews wmNews = wmNewsMapper.selectById(wmNewsId);
        if (wmNews == null) {
            log.error("自动审核文章失败    未查询自媒体文章信息  wmNewsId:{}", wmNewsId);
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        // 2. 判断文章状态是否为待审核状态
        Short status = wmNews.getStatus();
        if (status.shortValue() == WmNews.Status.SUBMIT.getCode()) {
            // 抽取出文章中 所有的文本内容 和 所有的图片url集合    Map<String,Object>  content 内容   images List<String>
            Map<String, Object> contentAndImageResult = handleTextAndImages(wmNews);
            // 3.1  敏感词审核    失败   修改文章状态(2)
            boolean isSensivice = handleSensitive((String) contentAndImageResult.get("content"), wmNews);
            if (!isSensivice) return;
            log.info(" 自管理敏感词审核通过  =======   ");

            // 3.2  阿里云的文本审核   失败  状态2  不确定 状态3
            boolean isTextScan = handleTextScan((String) contentAndImageResult.get("content"), wmNews);
            if (!isTextScan) return;
            log.info(" 阿里云内容审核通过  =======   ");
            // 3.3  阿里云的图片审核   失败  状态2  不确定 状态3
            Object images = contentAndImageResult.get("images");
            if (images != null) {
                boolean isImageScan = handleImageScan((List<String>) images, wmNews);
                if (!isImageScan) return;
                log.info(" 阿里云图片审核通过  =======   ");
            }
            // 4. 判断文章发布时间是否大于当前时间   状态 8
            updateWmNews(wmNews, WmNews.Status.SUCCESS.getCode(), "审核成功");

            // 5. 通知定时发布文章
            // 发布时间
            long publishTime = wmNews.getPublishTime().getTime();
            // 当前时间
            long nowTime = new Date().getTime();
            long remainTime = publishTime - nowTime;
            // 发布文章
            rabbitTemplate.convertAndSend(PublishArticleConstants.DELAY_DIRECT_EXCHANGE
                    , PublishArticleConstants.PUBLISH_ARTICLE_ROUTE_KEY
                    , wmNews.getId()
                    , (message) -> {                              // 延时消息 必设置
                        message.getMessageProperties().setHeader("x-delay", remainTime <= 0 ? 0 : remainTime);
                        return message;
                    }
            );
            log.info("立即发布文章通知成功发送，文章id : {}", wmNews.getId());
        }
    }

    /**
     * 阿里云图片审核
     *
     * @param images 待审核的图片列表
     * @return
     */
    private boolean handleImageScan(List<String> images, WmNews wmNews) {
        boolean flag = true;
        try {
            Map map = greenImageScan.imageUrlScan(images);
            String suggestion = (String) map.get("suggestion");
            switch (suggestion) {
                case "block":
                    updateWmNews(wmNews, WmNews.Status.FAIL.getCode(), "图片中有违规内容，审核失败");
                    flag = false;
                    break;
                case "review":
                    updateWmNews(wmNews, WmNews.Status.ADMIN_AUTH.getCode(), "图片中有不确定内容，转为人工审核");
                    flag = false;
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("阿里云图片审核出现异常 , 原因:{}", e.getMessage());
            updateWmNews(wmNews, WmNews.Status.ADMIN_AUTH.getCode(), "阿里云内容服务异常，转为人工审核");
            flag = false;
        }
        //return flag;
        return true;
    }

    /**
     * 阿里云文本
     *
     * @param content block: 状态2    review: 状态3    异常: 状态3
     * @param wmNews
     * @return
     */
    private boolean handleTextScan(String content, WmNews wmNews) {
        boolean flag = true;
        try {
            Map map = greenTextScan.greenTextScan(content);
            String suggestion = (String) map.get("suggestion");
            switch (suggestion) {
                case "block":
                    updateWmNews(wmNews, WmNews.Status.FAIL.getCode(), "文本中有违规内容，审核失败");
                    flag = false;
                    break;
                case "review":
                    updateWmNews(wmNews, WmNews.Status.ADMIN_AUTH.getCode(), "文本中有不确定内容，转为人工审核");
                    flag = false;
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("阿里云文本审核出现异常 , 原因:{}", e.getMessage());
            updateWmNews(wmNews, WmNews.Status.ADMIN_AUTH.getCode(), "阿里云内容服务异常，转为人工审核");
            flag = false;
        }
        //return flag;
        return true;
    }

    /**
     * 基于DFA 检测内容是否包含敏感词
     *
     * @param content
     * @param wmNews
     * @return
     */
    private boolean handleSensitive(String content, WmNews wmNews) {
        boolean flag = true;
        // 1. 查询出数据库中的敏感词
        ResponseResult<List<String>> allSensitivesResult = adminFeign.sensitives();
        if (allSensitivesResult.getCode().intValue() != 0) {
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR, allSensitivesResult.getErrorMessage());
        }
        List<String> allSensitives = allSensitivesResult.getData();
        // 2. 将敏感词集合转发DFA数据模型
        SensitiveWordUtil.initMap(allSensitives);
        // 3. 检测敏感词
        Map<String, Integer> resultMap = SensitiveWordUtil.matchWords(content);
        if (resultMap != null && resultMap.size() > 0) {
            // 将文章状态改为2
            updateWmNews(wmNews, WmNews.Status.FAIL.getCode(), "内容中包含敏感词: " + resultMap);
            flag = false;
        }
        return flag;
    }


    /**
     * 修改文章状态
     *
     * @param wmNews
     * @param status
     * @param reason
     */
    private void updateWmNews(WmNews wmNews, short status, String reason) {
        wmNews.setStatus(status);
        wmNews.setReason(reason);
        wmNewsMapper.updateById(wmNews);
    }

    /**
     * 抽取 文章中所有 文本内容  及 所有图片路径
     *
     * @param wmNews content  type:text     title
     * @return
     */
    private Map<String, Object> handleTextAndImages(WmNews wmNews) {
        String contentJson = wmNews.getContent(); // [{},{},{}]
        if (StringUtils.isBlank(contentJson)) {
            log.error("自动审核文章失败    文章内容为空");
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID, "文章内容为空");
        }
        List<Map> contentMaps = JSONArray.parseArray(contentJson, Map.class);
        // 1. 抽取文章中所有文本     家乡很美   _hmtt_   国家伟大
        String content = contentMaps.stream()
                .filter(map -> "text".equals(map.get("type")))
                .map(map -> (String) map.get("value"))
                .collect(Collectors.joining("_hmtt_"));
        content = content + "_hmtt_" + wmNews.getTitle();

        // 2. 抽取文章中所有图片   content :  全路径       images :  文件名称  + 访问前缀
        List<String> imageList = contentMaps.stream()
                .filter(map -> "image".equals(map.get("type")))
                .map(map -> (String) map.get("value"))
                .collect(Collectors.toList());
        if (StringUtils.isNotBlank(wmNews.getImages())) {
            // 按照 逗号 切割封面字符串  得到数组   基于数组得到stream   将每一条数据都拼接一个前缀 收集成集合
            List<String> urls = Arrays.stream(wmNews.getImages().split(","))
                    .map(url -> webSite + url)
                    .collect(Collectors.toList());
            imageList.addAll(urls);
        }
        // 3. 去重
        imageList = imageList.stream().distinct().collect(Collectors.toList());

        Map result = new HashMap();
        result.put("content", content);
        result.put("images", imageList);
        return result;
    }
}
