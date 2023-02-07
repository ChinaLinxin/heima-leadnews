package com.heima.wemedia.listen;
import com.heima.common.constants.message.NewsAutoScanConstants;
import com.heima.wemedia.service.WmNewsAutoScanService;
import com.heima.wemedia.service.WmNewsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.IOException;
@Component
@Slf4j
public class WemediaNewsAutoListener {
    @Autowired
    WmNewsAutoScanService wmNewsAutoScanService;
    @Autowired
    WmNewsService wmNewsService;
    /**
     * queues: 监听指定队列
     * queuesToDeclare: 声明并监听指定队列
     * bindings: 声明队列  交换机  并通过路由绑定
     */
    @RabbitListener(queuesToDeclare = {@Queue(name = NewsAutoScanConstants.WM_NEWS_AUTO_SCAN_QUEUE)})
    public void newsAutoScanHandler(String newsId){
        log.info("接收到 自动审核 消息===> {}",newsId);
        // 自动审核
        wmNewsAutoScanService.autoScanWmNews(Integer.valueOf(newsId));
    }
}