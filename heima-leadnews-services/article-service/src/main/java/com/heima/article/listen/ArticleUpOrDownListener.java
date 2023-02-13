package com.heima.article.listen;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.service.ApArticleConfigService;
import com.heima.common.constants.message.NewsUpOrDownConstants;
import com.heima.model.article.pojos.ApArticleConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ArticleUpOrDownListener {
    @Autowired
    private ApArticleConfigService apArticleConfigService;

    @RabbitListener(queuesToDeclare = {
            @Queue(value = NewsUpOrDownConstants.NEWS_UP_FOR_ARTICLE_CONFIG_QUEUE)
    })
    public void newsUpHandler(String articleId) {
        log.info("接收到自媒体文章上架消息, 文章id: {}", articleId);
        // 当前消息路由
        try {
            apArticleConfigService.update(Wrappers.<ApArticleConfig>lambdaUpdate()
                    .set(ApArticleConfig::getIsDown, false)
                    .eq(ApArticleConfig::getArticleId, Long.valueOf(articleId)));
            // 手动确认
        } catch (Exception e) {
            e.printStackTrace();
            log.error("article处理 自媒体文章上架消息 失败, 文章id: {}   原因: {}", articleId, e.getMessage());
        }
    }

    @RabbitListener(queuesToDeclare = {
            @Queue(value = NewsUpOrDownConstants.NEWS_DOWN_FOR_ARTICLE_CONFIG_QUEUE)
    })
    public void newsDownHandler(String articleId) {
        log.info("接收到自媒体文章下架消息, 文章id: {}");
        // 当前消息路由
        try {
            apArticleConfigService.update(Wrappers.<ApArticleConfig>lambdaUpdate()
                    .set(ApArticleConfig::getIsDown, true)
                    .eq(ApArticleConfig::getArticleId, Long.valueOf(articleId)));
        } catch (Exception e) {
            e.printStackTrace();
            log.error("article处理 自媒体文章下架消息 失败, 文章id: {}   原因: {}", articleId, e.getMessage());
        }
    }
}