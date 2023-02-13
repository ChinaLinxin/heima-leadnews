package com.heima.wemedia.config;

import com.heima.common.constants.message.PublishArticleConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 声明定时发布文章
 * 所需的 所有交换机  队列 及 绑定关系
 **/
@Configuration
public class DeclarePubArticleRabbitConfig {
    /**
     * 延时交换机
     * RabbitMQ需要安装延迟插件
     */
    @Bean
    public DirectExchange delayExchange() {
        return ExchangeBuilder.directExchange(PublishArticleConstants.DELAY_DIRECT_EXCHANGE)
                .delayed()
                .durable(true)
                .build();
    }

    /**
     * 声明发布文章队列
     *
     * @return
     */
    @Bean
    public Queue publishArticleQueue() {
        return new Queue(PublishArticleConstants.PUBLISH_ARTICLE_QUEUE, true);
    }

    /**
     * 绑定 延迟交换机 + 发布文章队列
     *
     * @return
     */
    @Bean
    public Binding bindingDeadQueue() {
        return BindingBuilder.bind(publishArticleQueue()).to(delayExchange()).with(PublishArticleConstants.PUBLISH_ARTICLE_ROUTE_KEY);
    }
}