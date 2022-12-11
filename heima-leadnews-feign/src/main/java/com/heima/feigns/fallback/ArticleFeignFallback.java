package com.heima.feigns.fallback;

import com.heima.feigns.ArticleFeign;
import com.heima.model.article.pojos.ApAuthor;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ArticleFeignFallback implements FallbackFactory<ArticleFeign> {
    @Override
    public ArticleFeign create(Throwable throwable) {
        return new ArticleFeign() {
            @Override
            public ResponseResult<ApAuthor> findByUserId(Integer userId) {
                log.error("参数 userId : {}", userId);
                log.error("ArticleFeign findByUserId 远程调用出错啦 ~~~ !!!! {} ", throwable.getMessage());
                return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR);
            }

            @Override
            public ResponseResult save(ApAuthor apAuthor) {
                log.error("参数 apAuthor: {}", apAuthor);
                log.error("ArticleFeign save 远程调用出错啦 ~~~ !!!! {} ", throwable.getMessage());
                return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR);
            }
        };
    }
}