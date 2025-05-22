package andy.crypto.pairstrading.bot.pairstrading.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;

/**
 * 請求映射日誌記錄器
 * 用於在啟動時列出所有可用的URL映射
 */
@Slf4j
@Component
public class RequestMappingLogger implements CommandLineRunner {

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Override
    public void run(String... args) throws Exception {
        log.info("應用程序的URL映射：");
        Map<RequestMappingInfo, HandlerMethod> map = handlerMapping.getHandlerMethods();
        map.forEach((key, value) -> log.info("{} => {}", key, value));
    }
}
