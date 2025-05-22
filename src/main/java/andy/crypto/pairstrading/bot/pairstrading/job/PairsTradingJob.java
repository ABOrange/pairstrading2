package andy.crypto.pairstrading.bot.pairstrading.job;

import andy.crypto.pairstrading.bot.bean.PairsTradingServiceValueBean;
import andy.crypto.pairstrading.bot.pairstrading.config.BinanceConfig;
import andy.crypto.pairstrading.bot.pairstrading.service.LogService;
import andy.crypto.pairstrading.bot.pairstrading.service.PairsTradingService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class PairsTradingJob extends QuartzJobBean {

    @Autowired
    private PairsTradingService pairsTradingService;
    
    @Autowired
    private LogService logService;
    
    @Autowired
    private BinanceConfig binanceConfig;

    @Override
    public void executeInternal(JobExecutionContext context) throws JobExecutionException {
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.info("配對交易排程任務執行中... 執行時間: {}", currentTime);
        logService.addLog("配對交易排程任務執行中... 執行時間: " + currentTime, "INFO");
        
        // 檢查API金鑰是否已設定
        if (binanceConfig == null || binanceConfig.getApiKey() == null || binanceConfig.getApiKey().isEmpty() || 
            binanceConfig.getSecretKey() == null || binanceConfig.getSecretKey().isEmpty()) {
            String errorMsg = "API金鑰尚未設定，請在設定頁面配置API金鑰後再執行任務";
            log.error(errorMsg);
            logService.addLog(errorMsg, "ERROR");
            return; // 直接返回，不執行後續操作
        }
        
        try {
            // 1. 獲取市場數據
            logService.addLog("開始獲取市場數據", "INFO");
            PairsTradingServiceValueBean pairsTradingServiceValueBean =pairsTradingService.fetchMarketData();
            
            // 2. 分析配對資產相關性
            logService.addLog("開始分析配對資產相關性", "INFO");
            pairsTradingService.analyzeCorrelation(pairsTradingServiceValueBean);
            
            // 3. 計算交易信號
            logService.addLog("開始計算交易信號", "INFO");
            boolean hasSignal = pairsTradingService.calculateSignal(pairsTradingServiceValueBean);
            
            // 4. 如果有信號，執行交易
            if (hasSignal) {
                logService.addLog("檢測到交易信號，開始執行交易操作", "INFO");
                pairsTradingService.executeTrade(true);
                log.info("交易信號檢測並執行完成");
                logService.addLog("交易信號檢測並執行完成", "INFO");
            } else {
                log.info("未檢測到交易信號，不執行交易操作");
                logService.addLog("未檢測到交易信號，不執行交易操作", "INFO");
            }
        } catch (Exception e) {
            log.error("執行配對交易任務時發生錯誤", e);
            logService.addLog("執行配對交易任務時發生錯誤: " + e.getMessage(), "ERROR");
            throw new JobExecutionException(e);
        }
    }
}
