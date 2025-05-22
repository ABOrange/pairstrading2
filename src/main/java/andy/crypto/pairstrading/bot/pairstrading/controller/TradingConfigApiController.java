package andy.crypto.pairstrading.bot.pairstrading.controller;

import andy.crypto.pairstrading.bot.bean.PairsTradingServiceValueBean;
import andy.crypto.pairstrading.bot.pairstrading.service.LogService;
import andy.crypto.pairstrading.bot.pairstrading.service.PairsTradingService;
import andy.crypto.pairstrading.bot.service.TradingConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 交易配置 API 控制器
 * 用於提供交易配置相關的 API 端點
 */
@Slf4j
@RestController
@RequestMapping("/api/trading-config")
public class TradingConfigApiController {

    @Autowired
    private PairsTradingService pairsTradingService;
    
    @Autowired
    private TradingConfigService tradingConfigService;
    
    @Autowired
    private LogService logService;
    
    /**
     * 獲取當前窗口大小
     */
    @GetMapping("/window-size")
    public Map<String, Object> getWindowSize() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 獲取當前窗口大小
            int windowSize = tradingConfigService.getTradingConfigBean().getWindowSize();
            result.put("windowSize", windowSize);
            result.put("status", "success");
        } catch (Exception e) {
            log.error("獲取窗口大小失敗", e);
            result.put("status", "error");
            result.put("message", "獲取窗口大小失敗: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 設置窗口大小
     */
    @GetMapping("/set-window-size")
    public Map<String, Object> setWindowSize(@RequestParam int windowSize) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 記錄操作日誌
            logService.addLog("修改窗口大小: " + windowSize, "INFO");
            
            // 設置新的窗口大小
            boolean success = pairsTradingService.setWindowSize(windowSize);
            
            if (success) {
                result.put("message", "窗口大小已更新為 " + windowSize);
                result.put("status", "success");
            } else {
                result.put("message", "窗口大小設置失敗，請確保值在有效範圍內 (10-1000)");
                result.put("status", "error");
            }
        } catch (Exception e) {
            log.error("設置窗口大小失敗", e);
            logService.addLog("設置窗口大小失敗: " + e.getMessage(), "ERROR");
            result.put("status", "error");
            result.put("message", "設置窗口大小失敗: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 手動觸發交易任務
     */
    @GetMapping("/run-job")
    public Map<String, Object> runJob() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 記錄操作日誌
            logService.addLog("手動觸發交易任務", "INFO");
            
            // 執行交易任務
            PairsTradingServiceValueBean pairsTradingServiceValueBean =  pairsTradingService.fetchMarketData();
            pairsTradingService.analyzeCorrelation(pairsTradingServiceValueBean);
            boolean hasSignal = pairsTradingService.calculateSignal(pairsTradingServiceValueBean);
            
            if (hasSignal) {
                pairsTradingService.executeTrade(true);
                String message = "交易任務已執行，檢測到交易信號並執行交易";
                logService.addLog(message, "INFO");
                result.put("message", message);
            } else {
                String message = "交易任務已執行，未檢測到交易信號";
                logService.addLog(message, "INFO");
                result.put("message", message);
            }
            result.put("status", "success");
        } catch (Exception e) {
            log.error("手動觸發交易任務失敗", e);
            logService.addLog("手動觸發交易任務失敗: " + e.getMessage(), "ERROR");
            result.put("status", "error");
            result.put("message", "執行失敗: " + e.getMessage());
        }
        return result;
    }
}