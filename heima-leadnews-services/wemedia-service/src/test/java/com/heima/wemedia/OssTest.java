package com.heima.wemedia;

import com.heima.file.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;

/**
 * @author Linxin
 */
@SpringBootTest
public class OssTest {
    @Autowired
    FileStorageService fileStorageService;

    @Value("${file.oss.web-site}")
    String webSite;

    @Test
    public void testFileUpload() throws Exception {

        FileInputStream inputStream = new FileInputStream(new File("C:\\Users\\20656\\Pictures\\Saved Pictures\\15383340a19d5e66858afec909e8376a--143226042.jpg"));

        String wemedia = fileStorageService.store("upload", "aaa1.jpg", inputStream);
        System.out.println(webSite + wemedia);

        // 删除文件
        //fileStorageService.delete("wemedia/2020/12/20201227/aaa1.jpg");
    }
}
