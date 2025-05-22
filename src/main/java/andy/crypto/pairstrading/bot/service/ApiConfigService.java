package andy.crypto.pairstrading.bot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import andy.crypto.pairstrading.bot.entity.ApiConfig;
import andy.crypto.pairstrading.bot.pairstrading.config.BinanceConfig;
import andy.crypto.pairstrading.bot.repository.ApiConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * API配置服務類
 */
@Service
@Slf4j
public class ApiConfigService {

    @Autowired
    private ApiConfigRepository apiConfigRepository;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    // 配置鍵常量
    public static final String BINANCE_API_KEY = "binance.apiKey";
    public static final String BINANCE_SECRET_KEY = "binance.secretKey";
    public static final String BINANCE_TESTNET_ENABLED = "binance.testnetEnabled";
    
    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        try {
            log.info("ApiConfigService 已初始化");
            // 不在這裡設置 BinanceConfig，讓 BeanPostProcessor 負責這個工作
        } catch (Exception e) {
            log.error("初始化 ApiConfigService 時發生錯誤", e);
        }
    }
    
    /**
     * 獲取配置值
     * 
     * @param configKey 配置鍵
     * @return 配置值，如果不存在則返回null
     */
    public String getConfigValue(String configKey) {
        return apiConfigRepository.findByConfigKey(configKey)
                .map(ApiConfig::getConfigValue)
                .orElse(null);
    }
    
    /**
     * 保存或更新配置
     * 
     * @param configKey 配置鍵
     * @param configValue 配置值
     * @param description 描述
     */
    public void saveOrUpdateConfig(String configKey, String configValue, String description) {
        ApiConfig config = apiConfigRepository.findByConfigKey(configKey)
                .orElse(new ApiConfig(configKey, null, description));
        
        config.setConfigValue(configValue);
        apiConfigRepository.save(config);
        log.info("保存配置: {} = {}", configKey, configValue);
        
        // 如果更新的是幣安相關配置，也更新 BinanceConfig
        if (BINANCE_API_KEY.equals(configKey) || BINANCE_SECRET_KEY.equals(configKey)) {
            try {
                BinanceConfig binanceConfig = applicationContext.getBean(BinanceConfig.class);
                binanceConfig.loadApiKeysFromDatabase();
                log.info("已更新 BinanceConfig 中的 API 金鑰");
            } catch (Exception e) {
                log.error("更新 BinanceConfig 時發生錯誤", e);
            }
        } else if (BINANCE_TESTNET_ENABLED.equals(configKey)) {
            try {
                BinanceConfig binanceConfig = applicationContext.getBean(BinanceConfig.class);
                boolean testnetEnabled = Boolean.parseBoolean(configValue);
                binanceConfig.setTestnetEnabled(testnetEnabled);
                log.info("已更新 BinanceConfig 中的測試網絡設置為: {}", testnetEnabled);
            } catch (Exception e) {
                log.error("更新 BinanceConfig 測試網絡設置時發生錯誤", e);
            }
        }
    }
    
    /**
     * 獲取幣安API金鑰
     * 
     * @return 幣安API金鑰
     */
    public String getBinanceApiKey() {
        return getConfigValue(BINANCE_API_KEY);
    }
    
    /**
     * 獲取幣安API秘鑰
     * 
     * @return 幣安API秘鑰
     */
    public String getBinanceSecretKey() {
        return getConfigValue(BINANCE_SECRET_KEY);
    }
    
    /**
     * 獲取幣安測試網絡啟用狀態
     * 
     * @return 測試網絡啟用狀態，默認為true
     */
    public boolean isTestnetEnabled() {
        String value = getConfigValue(BINANCE_TESTNET_ENABLED);
        return value == null ? true : Boolean.parseBoolean(value);
    }
    
    /**
     * 設置幣安測試網絡啟用狀態
     * 
     * @param enabled 是否啟用測試網絡
     */
    public void setTestnetEnabled(boolean enabled) {
        saveOrUpdateConfig(BINANCE_TESTNET_ENABLED, String.valueOf(enabled), "幣安測試網絡啟用狀態");
    }
}
