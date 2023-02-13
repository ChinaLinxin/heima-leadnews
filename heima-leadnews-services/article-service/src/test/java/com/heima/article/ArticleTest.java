package com.heima.article;

import com.heima.article.service.ApArticleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Linxin
 */
@SpringBootTest
public class ArticleTest {

    @Autowired
    ApArticleService apArticleService;

    @Test
    public void publishArticle() {
        apArticleService.publishArticle(6277);
    }
}
