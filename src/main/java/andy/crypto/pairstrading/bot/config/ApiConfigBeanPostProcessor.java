package andy.crypto.pairstrading.bot.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import andy.crypto.pairstrading.bot.pairstrading.config.BinanceConfig;
import andy.crypto.pairstrading.bot.service.ApiConfigService;
import lombok.extern.slf4j.Slf4j;

/**
 * Bean後置處理器，用於處理API配置相關的依賴注入
 */
@Component
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE) // 確保這個處理器在所有其他Bean初始化後運行
public class ApiConfigBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 處理 BinanceConfig bean，確保它能與 ApiConfigService 正確連接
        if (bean instanceof BinanceConfig) {
            try {
                BinanceConfig binanceConfig = (BinanceConfig) bean;
                
                // 延遲獲取 ApiConfigService，避免循環依賴
                ApiConfigService apiConfigService = applicationContext.getBean(ApiConfigService.class);
                
                if (apiConfigService != null) {
                    binanceConfig.setApiConfigService(apiConfigService);
                    log.info("成功設置 ApiConfigService 到 BinanceConfig");
                }
            } catch (Exception e) {
                log.warn("設置 ApiConfigService 到 BinanceConfig 時發生錯誤: {}", e.getMessage());
            }
        }
        
        return bean;
    }
}
