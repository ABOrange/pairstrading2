package andy.crypto.pairstrading.bot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import andy.crypto.pairstrading.bot.repository.ApiConfigRepository;
import andy.crypto.pairstrading.bot.service.ApiConfigService;
import lombok.extern.slf4j.Slf4j;

/**
 * API配置初始化類
 * 用於在應用啟動時檢查API配置狀態
 */
@Component
@Slf4j
public class ApiConfigInitializer implements CommandLineRunner {

    @Autowired(required = false)
    private ApiConfigService apiConfigService;
    
    @Autowired(required = false)
    private ApiConfigRepository apiConfigRepository;
    
    @Autowired(required = false)
    private Environment environment;

    @Override
    public void run(String... args) throws Exception {
        checkBinanceApiKeys();
    }
    
    /**
     * 檢查幣安API金鑰是否已設定
     */
    private void checkBinanceApiKeys() {
        if (apiConfigRepository == null) {
            log.warn("ApiConfigRepository未設置，無法檢查API金鑰狀態");
            return;
        }
        
        try {
            // 檢查API金鑰是否存在
            boolean apiKeyExists = apiConfigRepository.findByConfigKey(ApiConfigService.BINANCE_API_KEY).isPresent();
            boolean secretKeyExists = apiConfigRepository.findByConfigKey(ApiConfigService.BINANCE_SECRET_KEY).isPresent();
            
            if (!apiKeyExists || !secretKeyExists) {
                log.warn("資料庫中缺少幣安API配置，請使用Web介面設定API金鑰");
            } else {
                log.info("幣安API配置已存在於資料庫中");
            }
        } catch (Exception e) {
            log.error("檢查API金鑰時發生錯誤", e);
        }
    }
}
