package andy.crypto.pairstrading.bot.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import andy.crypto.pairstrading.bot.bean.TradingConfigBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import andy.crypto.pairstrading.bot.entity.TradingConfig;
import andy.crypto.pairstrading.bot.repository.TradingConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 交易配置服務類
 */
@Service
@Slf4j
public class TradingConfigService {

    @Autowired
    private TradingConfigRepository tradingConfigRepository;

    // 配置鍵常量
    // 是否開啟交易
    public static final String TRADING_ENABLE = "trading.enable";
    
    // 資產配置
    public static final String ASSET_1 = "trading.pair.asset1";
    public static final String ASSET_2 = "trading.pair.asset2";
    
    // 窗口大小
    public static final String WINDOW_SIZE = "trading.window.size";
    
    // 閾值設定
    public static final String ENTRY_THRESHOLD = "trading.threshold.entry";
    public static final String EXIT_THRESHOLD = "trading.threshold.exit";
    
    // 倉位設定
    public static final String POSITION_SIZE = "trading.position.size";
    public static final String AMOUNT_BASED = "trading.position.amount_based";
    public static final String LEVERAGE = "trading.position.leverage";
    
    // 控制台設定
    public static final String CONSOLE_CHART = "trading.console.chart";
    public static final String CONSOLE_SIGNAL = "trading.console.signal";
    
    // 類別常量
    public static final String CATEGORY_GENERAL = "general";
    public static final String CATEGORY_PAIR = "pair";
    public static final String CATEGORY_WINDOW = "window";
    public static final String CATEGORY_THRESHOLD = "threshold";
    public static final String CATEGORY_POSITION = "position";
    public static final String CATEGORY_CONSOLE = "console";
    
    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        try {
            log.info("TradingConfigService 已初始化");
        } catch (Exception e) {
            log.error("初始化 TradingConfigService 時發生錯誤", e);
        }
    }


    public TradingConfigBean getTradingConfigBean(){
        TradingConfigBean result = new TradingConfigBean();
        result.setAsset1(getAsset1());
        result.setAsset2(getAsset2());
        result.setWindowSize(getWindowSize());
        result.setEntryThreshold(getEntryThreshold());
        result.setExitThreshold(getExitThreshold());
        result.setPositionSize(getPositionSize());
        result.setLeverage(getLeverage());
        result.setAmountBased(isAmountBased());
        result.setPositionSize(getPositionSize());
        result.setTradingEnabled(isTradingEnabled());
        result.setConsoleChartEnabled(isConsoleChartEnabled());
        result.setConsoleSignalEnabled(isConsoleSignalEnabled());
        return result;
    }

    /**
     * 獲取配置值
     * 
     * @param configKey 配置鍵
     * @return 配置值，如果不存在則返回null
     */
    public String getConfigValue(String configKey) {
        return tradingConfigRepository.findByConfigKey(configKey)
                .map(TradingConfig::getConfigValue)
                .orElse(null);
    }
    
    /**
     * 獲取布爾型配置值
     * 
     * @param configKey 配置鍵
     * @param defaultValue 默認值
     * @return 布爾值
     */
    public boolean getBooleanValue(String configKey, boolean defaultValue) {
        String value = getConfigValue(configKey);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
    
    /**
     * 獲取整數型配置值
     * 
     * @param configKey 配置鍵
     * @param defaultValue 默認值
     * @return 整數值
     */
    public int getIntValue(String configKey, int defaultValue) {
        String value = getConfigValue(configKey);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 獲取雙精度型配置值
     * 
     * @param configKey 配置鍵
     * @param defaultValue 默認值
     * @return 雙精度值
     */
    public double getDoubleValue(String configKey, double defaultValue) {
        String value = getConfigValue(configKey);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 獲取BigDecimal型配置值
     * 
     * @param configKey 配置鍵
     * @param defaultValue 默認值
     * @return BigDecimal值
     */
    public BigDecimal getBigDecimalValue(String configKey, BigDecimal defaultValue) {
        String value = getConfigValue(configKey);
        if (value == null) {
            return defaultValue;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 保存或更新配置
     * 
     * @param configKey 配置鍵
     * @param configValue 配置值
     * @param description 描述
     * @param category 類別
     */
    public void saveOrUpdateConfig(String configKey, String configValue, String description, String category) {
        TradingConfig config = tradingConfigRepository.findByConfigKey(configKey)
                .orElse(new TradingConfig(configKey, null, description, category));
        
        config.setConfigValue(configValue);
        tradingConfigRepository.save(config);
        log.debug("保存交易配置: {} = {}", configKey, configValue);
    }
    
    /**
     * 獲取所有配置
     * 
     * @return 所有配置的列表
     */
    public List<TradingConfig> getAllConfigs() {
        return tradingConfigRepository.findAll();
    }
    
    /**
     * 根據類別獲取配置
     * 
     * @param category 類別
     * @return 該類別的配置列表
     */
    public List<TradingConfig> getConfigsByCategory(String category) {
        return tradingConfigRepository.findByCategory(category);
    }
    
    /**
     * 獲取所有配置並按類別分組
     * 
     * @return 按類別分組的配置映射
     */
    public Map<String, List<TradingConfig>> getConfigsByCategories() {
        List<TradingConfig> allConfigs = tradingConfigRepository.findAll();
        return allConfigs.stream()
                .collect(Collectors.groupingBy(TradingConfig::getCategory));
    }
    
    /**
     * 交易是否啟用
     * 
     * @return 是否啟用
     */
    public boolean isTradingEnabled() {
        return getBooleanValue(TRADING_ENABLE, false);
    }
    
    /**
     * 獲取第一個資產
     * 
     * @return 資產名稱
     */
    public String getAsset1() {
        return getConfigValue(ASSET_1);
    }
    
    /**
     * 獲取第二個資產
     * 
     * @return 資產名稱
     */
    public String getAsset2() {
        return getConfigValue(ASSET_2);
    }
    
    /**
     * 獲取窗口大小
     * 
     * @return 窗口大小
     */
    public int getWindowSize() {
        return getIntValue(WINDOW_SIZE, 700);
    }
    
    /**
     * 獲取入場閾值
     * 
     * @return 入場閾值
     */
    public double getEntryThreshold() {
        return getDoubleValue(ENTRY_THRESHOLD, 2.0);
    }
    
    /**
     * 獲取出場閾值
     * 
     * @return 出場閾值
     */
    public double getExitThreshold() {
        return getDoubleValue(EXIT_THRESHOLD, 0.5);
    }
    
    /**
     * 獲取倉位大小
     * 
     * @return 倉位大小
     */
    public BigDecimal getPositionSize() {
        return getBigDecimalValue(POSITION_SIZE, new BigDecimal("0.01"));
    }
    
    /**
     * 是否基於金額計價
     * 
     * @return 是否基於金額計價
     */
    public boolean isAmountBased() {
        return getBooleanValue(AMOUNT_BASED, false);
    }
    
    /**
     * 獲取槓桿倍率
     * 
     * @return 槓桿倍率
     */
    public int getLeverage() {
        return getIntValue(LEVERAGE, 5);
    }
    
    /**
     * 是否顯示控制台圖表
     * 
     * @return 是否顯示控制台圖表
     */
    public boolean isConsoleChartEnabled() {
        return getBooleanValue(CONSOLE_CHART, true);
    }
    
    /**
     * 是否顯示控制台信號
     * 
     * @return 是否顯示控制台信號
     */
    public boolean isConsoleSignalEnabled() {
        return getBooleanValue(CONSOLE_SIGNAL, true);
    }
}
