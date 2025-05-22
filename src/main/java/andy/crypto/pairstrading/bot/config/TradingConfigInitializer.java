package andy.crypto.pairstrading.bot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import andy.crypto.pairstrading.bot.service.TradingConfigService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 * 交易配置初始化器
 * 負責將YAML配置轉移到資料庫
 */
@Component
@Slf4j
public class TradingConfigInitializer implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private TradingConfigService tradingConfigService;
    
    @Autowired
    private Environment env;
    
    @Value("${trading.enable:false}")
    private boolean tradingEnable;
    
    @Value("${trading.pair.asset1:BNBUSDT}")
    private String asset1;
    
    @Value("${trading.pair.asset2:SOLUSDT}")
    private String asset2;
    
    @Value("${trading.window.size:700}")
    private int windowSize;
    
    @Value("${trading.threshold.entry:2.0}")
    private double entryThreshold;
    
    @Value("${trading.threshold.exit:0.5}")
    private double exitThreshold;
    
    @Value("${trading.position.size:10}")
    private String positionSize;
    
    @Value("${trading.position.amount_based:true}")
    private boolean amountBased;
    
    @Value("${trading.position.leverage:5}")
    private int leverage;
    
    @Value("${trading.console.chart:true}")
    private boolean consoleChart;
    
    @Value("${trading.console.signal:true}")
    private boolean consoleSignal;
    
    private boolean initialized = false;
    
    @Override
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (initialized) {
            return;
        }
        
        try {
            log.info("開始初始化交易配置...");
            
            // 檢查資料庫中是否已存在交易配置
            if (tradingConfigService.getConfigValue(TradingConfigService.TRADING_ENABLE) == null) {
                log.info("資料庫中未找到交易配置，即將初始化預設配置...");
                
                // 初始化一般設定
                tradingConfigService.saveOrUpdateConfig(
                        TradingConfigService.TRADING_ENABLE, 
                        String.valueOf(tradingEnable), 
                        "在測試網上啟用實際交易", 
                        TradingConfigService.CATEGORY_GENERAL);
                
                // 初始化交易對設定
                tradingConfigService.saveOrUpdateConfig(
                        TradingConfigService.ASSET_1, 
                        asset1, 
                        "第一個資產", 
                        TradingConfigService.CATEGORY_PAIR);
                
                tradingConfigService.saveOrUpdateConfig(
                        TradingConfigService.ASSET_2, 
                        asset2, 
                        "第二個資產", 
                        TradingConfigService.CATEGORY_PAIR);
                
                // 初始化窗口設定
                tradingConfigService.saveOrUpdateConfig(
                        TradingConfigService.WINDOW_SIZE, 
                        String.valueOf(windowSize), 
                        "用於計算的歷史數據點數量", 
                        TradingConfigService.CATEGORY_WINDOW);
                
                // 初始化閾值設定
                tradingConfigService.saveOrUpdateConfig(
                        TradingConfigService.ENTRY_THRESHOLD, 
                        String.valueOf(entryThreshold), 
                        "入場閾值 (Z分數)", 
                        TradingConfigService.CATEGORY_THRESHOLD);
                
                tradingConfigService.saveOrUpdateConfig(
                        TradingConfigService.EXIT_THRESHOLD, 
                        String.valueOf(exitThreshold), 
                        "出場閾值 (Z分數)", 
                        TradingConfigService.CATEGORY_THRESHOLD);
                
                // 初始化倉位設定
                tradingConfigService.saveOrUpdateConfig(
                        TradingConfigService.POSITION_SIZE, 
                        positionSize, 
                        "交易倉位大小", 
                        TradingConfigService.CATEGORY_POSITION);
                
                tradingConfigService.saveOrUpdateConfig(
                        TradingConfigService.AMOUNT_BASED, 
                        String.valueOf(amountBased), 
                        "true表示使用金額計價(USDT)，false表示使用合約數量計價", 
                        TradingConfigService.CATEGORY_POSITION);
                
                tradingConfigService.saveOrUpdateConfig(
                        TradingConfigService.LEVERAGE, 
                        String.valueOf(leverage), 
                        "合約槓桿倍率", 
                        TradingConfigService.CATEGORY_POSITION);
                
                // 初始化控制台設定
                tradingConfigService.saveOrUpdateConfig(
                        TradingConfigService.CONSOLE_CHART, 
                        String.valueOf(consoleChart), 
                        "是否在控制台中顯示圖表", 
                        TradingConfigService.CATEGORY_CONSOLE);
                
                tradingConfigService.saveOrUpdateConfig(
                        TradingConfigService.CONSOLE_SIGNAL, 
                        String.valueOf(consoleSignal), 
                        "是否在控制台中顯示信號報告", 
                        TradingConfigService.CATEGORY_CONSOLE);
                        
                log.info("交易配置初始化完成！");
            } else {
                log.info("資料庫中已存在交易配置，跳過初始化步驟");
            }
            
            initialized = true;
        } catch (Exception e) {
            log.error("初始化交易配置時發生錯誤", e);
        }
    }
}
