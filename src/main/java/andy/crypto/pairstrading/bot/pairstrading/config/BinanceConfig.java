package andy.crypto.pairstrading.bot.pairstrading.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import andy.crypto.pairstrading.bot.service.ApiConfigService;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "binance")
@org.springframework.context.annotation.Lazy
public class BinanceConfig {
    
    private String apiKey;
    private String secretKey;
    private String baseUrl = "https://fapi.binance.com"; // 永續合約的API基礎URL
    private int connectionTimeout = 10000;
    private int readTimeout = 10000;
    private boolean testnetEnabled = false; // 是否使用測試網絡
    
    // 正式網路和測試網路的基礎URL常數
    private static final String PROD_BASE_URL = "https://fapi.binance.com";
    private static final String TEST_BASE_URL = "https://testnet.binancefuture.com";
    
    // ApiConfigService由後處理器設置
    private ApiConfigService apiConfigService;
    
    /**
     * 初始化後從數據庫加載API金鑰
     */
    @PostConstruct
    public void init() {
        try {
            log.info("已讀取幣安配置，但尚未從資料庫載入API金鑰");
            
            // 初始化先不從資料庫讀取，避免依賴注入問題
            // API金鑰將在ApiConfigService設置後載入
            
            // 根據測試網絡設置自動設定基礎URL
            updateBaseUrlBasedOnTestnetSetting();
        } catch (Exception e) {
            log.error("初始化API配置時發生錯誤", e);
        }
    }
    
    /**
     * 根據測試網絡設置更新基礎URL
     */
    public void updateBaseUrlBasedOnTestnetSetting() {
        if (testnetEnabled) {
            this.baseUrl = TEST_BASE_URL;
            log.info("已啟用幣安永續合約測試網絡: {}", baseUrl);
        } else {
            this.baseUrl = PROD_BASE_URL;
            log.info("已啟用幣安永續合約正式網絡: {}", baseUrl);
        }
    }
    
    /**
     * 設置測試網絡啟用狀態並更新基礎URL
     * @param testnetEnabled 是否啟用測試網絡
     */
    public void setTestnetEnabled(boolean testnetEnabled) {
        this.testnetEnabled = testnetEnabled;
        updateBaseUrlBasedOnTestnetSetting();
    }
    
    /**
     * 設置API配置服務
     * 由服務在初始化後手動呼叫，避免依賴循環
     */
    public void setApiConfigService(ApiConfigService apiConfigService) {
        this.apiConfigService = apiConfigService;
        this.loadApiKeysFromDatabase();
    }
    
    /**
     * 從資料庫載入API金鑰和測試網絡設置
     */
    public void loadApiKeysFromDatabase() {
        if (apiConfigService == null) {
            log.warn("ApiConfigService未設置，無法從資料庫載入API金鑰");
            return;
        }
        
        try {
            // 從資料庫獲取API金鑰
            String dbApiKey = apiConfigService.getBinanceApiKey();
            String dbSecretKey = apiConfigService.getBinanceSecretKey();
            boolean dbTestnetEnabled = apiConfigService.isTestnetEnabled();
            
            if (dbApiKey != null && !dbApiKey.isEmpty()) {
                this.apiKey = dbApiKey;
                log.info("已從資料庫載入幣安API金鑰");
            }
            
            if (dbSecretKey != null && !dbSecretKey.isEmpty()) {
                this.secretKey = dbSecretKey;
                log.info("已從資料庫載入幣安API秘鑰");
            }
            
            // 設置測試網絡啟用狀態，這將自動更新基礎URL
            this.testnetEnabled = dbTestnetEnabled;
            updateBaseUrlBasedOnTestnetSetting();
            log.info("已從資料庫載入幣安測試網絡設置: {}", dbTestnetEnabled);
        } catch (Exception e) {
            log.error("從資料庫載入API金鑰時發生錯誤", e);
        }
    }
    
    /**
     * 提供幣安API金鑰Bean
     * 這不是必需的，但提供來與舊版相容
     */
    @Bean
    @Primary
    public String binanceApiKey() {
        if (apiConfigService != null) {
            return apiConfigService.getBinanceApiKey();
        }
        return this.apiKey;
    }
    
    /**
     * 提供幣安API秘鑰Bean
     * 這不是必需的，但提供來與舊版相容
     */
    @Bean
    @Primary
    public String binanceSecretKey() {
        if (apiConfigService != null) {
            return apiConfigService.getBinanceSecretKey();
        }
        return this.secretKey;
    }
}
