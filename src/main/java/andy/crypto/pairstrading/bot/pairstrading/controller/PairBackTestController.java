package andy.crypto.pairstrading.bot.pairstrading.controller;

import andy.crypto.pairstrading.bot.bean.PairsTradingServiceValueBean;
import andy.crypto.pairstrading.bot.pairstrading.model.PairsTradingResult;
import andy.crypto.pairstrading.bot.pairstrading.service.PairsTradingService;
import andy.crypto.pairstrading.bot.pairstrading.service.LogService;
import andy.crypto.pairstrading.bot.service.TradingConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 交易對回測分析控制器
 * 用於提供交易對回測分析相關的API和頁面
 */
@Slf4j
@Controller
@RequestMapping("/backtest")
public class PairBackTestController {

    @Autowired
    private PairsTradingService pairsTradingService;
    
    @Autowired
    private LogService logService;
    
    @Autowired
    private TradingConfigService tradingConfigService;
    
    /**
     * 回測分析頁面
     */
    @GetMapping("")
    public String backTestPage(Model model) {
        try {
            // 獲取所有可用交易對
            List<String> allPairs = pairsTradingService.getAvailableTradingPairs();
            
            // 過濾出只以USDT結尾的交易對
            List<String> availablePairs = allPairs.stream()
                    .filter(pair -> pair.endsWith("USDT"))
                    .collect(Collectors.toList());
            
            model.addAttribute("availablePairs", availablePairs);
            
            // 獲取已保存的交易對組合
            List<String> savedPairCombinations = pairsTradingService.getSavedTradingPairCombinations();
            model.addAttribute("savedPairCombinations", savedPairCombinations);
            
            // 設置當前窗口大小
            int windowSize = tradingConfigService.getTradingConfigBean().getWindowSize();
            model.addAttribute("windowSize", windowSize);
            
            model.addAttribute("activeTab", "backtest");
            return "backtest/index";
        } catch (Exception e) {
            log.error("載入回測分析頁面失敗", e);
            model.addAttribute("error", "載入頁面數據失敗: " + e.getMessage());
            model.addAttribute("activeTab", "backtest");
            return "backtest/index";
        }
    }
    
    /**
     * 獲取所有可用交易對 (API)
     */
    @GetMapping("/api/available-pairs")
    @ResponseBody
    public Map<String, Object> getAvailablePairs() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<String> allPairs = pairsTradingService.getAvailableTradingPairs();
            
            // 過濾出只以USDT結尾的交易對
            List<String> availablePairs = allPairs.stream()
                    .filter(pair -> pair.endsWith("USDT"))
                    .collect(Collectors.toList());
                    
            result.put("pairs", availablePairs);
            result.put("status", "success");
        } catch (Exception e) {
            log.error("獲取可用交易對失敗", e);
            result.put("status", "error");
            result.put("message", "獲取可用交易對失敗: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 獲取已保存的交易對組合 (API)
     */
    @GetMapping("/api/saved-combinations")
    @ResponseBody
    public Map<String, Object> getSavedCombinations() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<String> savedCombinations = pairsTradingService.getSavedTradingPairCombinations();
            result.put("combinations", savedCombinations);
            result.put("status", "success");
        } catch (Exception e) {
            log.error("獲取已保存的交易對組合失敗", e);
            result.put("status", "error");
            result.put("message", "獲取已保存的交易對組合失敗: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 保存交易對組合 (API)
     */
    @GetMapping("/api/save-combination")
    @ResponseBody
    public Map<String, Object> saveCombination(@RequestParam String symbol1, @RequestParam String symbol2) {
        Map<String, Object> result = new HashMap<>();
        try {
            String pairCombination = symbol1 + "," + symbol2;
            boolean success = pairsTradingService.saveTradingPairCombination(pairCombination);
            
            if (success) {
                result.put("status", "success");
                result.put("message", "成功保存交易對組合: " + pairCombination);
                logService.addLog("保存交易對組合: " + pairCombination, "INFO");
            } else {
                result.put("status", "error");
                result.put("message", "保存交易對組合失敗");
            }
        } catch (Exception e) {
            log.error("保存交易對組合失敗", e);
            result.put("status", "error");
            result.put("message", "保存交易對組合失敗: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 刪除交易對組合 (API)
     */
    @GetMapping("/api/delete-combination")
    @ResponseBody
    public Map<String, Object> deleteCombination(@RequestParam String combination) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = pairsTradingService.deleteTradingPairCombination(combination);
            
            if (success) {
                result.put("status", "success");
                result.put("message", "成功刪除交易對組合: " + combination);
                logService.addLog("刪除交易對組合: " + combination, "INFO");
            } else {
                result.put("status", "error");
                result.put("message", "刪除交易對組合失敗");
            }
        } catch (Exception e) {
            log.error("刪除交易對組合失敗", e);
            result.put("status", "error");
            result.put("message", "刪除交易對組合失敗: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 執行單個交易對回測 (API)
     */
    @GetMapping("/api/backtest-pair")
    @ResponseBody
    public Map<String, Object> backTestPair(@RequestParam String symbol1, @RequestParam String symbol2, 
                                         @RequestParam(required = false) Integer days,
                                         @RequestParam(defaultValue = "1h") String interval) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 如果未指定回測天數，使用系統設置的窗口大小（單位1h）
            if (days == null) {
                days = tradingConfigService.getTradingConfigBean().getWindowSize();
            }else{
                days = days * 24;
            }
            
            // 記錄操作日誌
            logService.addLog("執行交易對回測: " + symbol1 + "," + symbol2 + " (天數: " + days + ", 間隔: " + interval + ")", "INFO");
            
            // 執行回測（使用指定的時間間隔）
            PairsTradingResult backTestResult = pairsTradingService.backTestPairCombination(symbol1, symbol2, days, interval);
            
            if (backTestResult != null) {
                result.put("result", backTestResult);
                result.put("status", "success");
                result.put("message", "成功回測交易對: " + symbol1 + "," + symbol2 + " (間隔: " + interval + ")");
            } else {
                result.put("status", "error");
                result.put("message", "回測失敗，無法獲取結果");
            }
        } catch (Exception e) {
            log.error("執行交易對回測失敗", e);
            result.put("status", "error");
            result.put("message", "執行回測失敗: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 獲取交易對Z分數圖表 (API)
     */
    @GetMapping("/api/zscore-chart")
    @ResponseBody
    public Map<String, Object> getZScoreChart(@RequestParam String symbol1, @RequestParam String symbol2) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 獲取Z分數圖表
            String zScoreChart = pairsTradingService.getBackTestZScoreChart(symbol1, symbol2);
            
            result.put("chart", zScoreChart);
            result.put("status", "success");
        } catch (Exception e) {
            log.error("獲取Z分數圖表失敗", e);
            result.put("status", "error");
            result.put("message", "獲取Z分數圖表失敗: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 獲取交易對價差圖表 (API)
     */
    @GetMapping("/api/spread-chart")
    @ResponseBody
    public Map<String, Object> getSpreadChart(@RequestParam String symbol1, @RequestParam String symbol2) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 獲取價差圖表
            String spreadChart = pairsTradingService.getBackTestSpreadChart(symbol1, symbol2);
            
            result.put("chart", spreadChart);
            result.put("status", "success");
        } catch (Exception e) {
            log.error("獲取價差圖表失敗", e);
            result.put("status", "error");
            result.put("message", "獲取價差圖表失敗: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 執行批量交易對回測 (API)
     */
    @GetMapping("/api/batch-backtest")
    @ResponseBody
    public Map<String, Object> batchBackTest(@RequestParam(required = false) Integer hour,
                                          @RequestParam(defaultValue = "1h") String interval) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 如果未指定回測天數，使用系統設置的窗口大小（單位1h）
            if (hour == null) {
                hour = tradingConfigService.getTradingConfigBean().getWindowSize();
                if (hour <= 0) hour = 1; // 確保至少為1天
            }
            
            // 記錄操作日誌
            logService.addLog("執行批量交易對回測 (天數: " + hour + ", 間隔: " + interval + ")", "INFO");
            
            // 執行回測（使用指定的時間間隔）
            Map<String, PairsTradingResult> backTestResults = pairsTradingService.backTestAllSavedPairCombinations(hour, interval);
            
            if (backTestResults != null && !backTestResults.isEmpty()) {
                result.put("results", backTestResults);
                result.put("status", "success");
                result.put("message", "成功回測 " + backTestResults.size() + " 個交易對組合 (間隔: " + interval + ")");
            } else {
                result.put("status", "warning");
                result.put("message", "批量回測完成，但沒有獲取到有效結果");
            }
        } catch (Exception e) {
            log.error("執行批量交易對回測失敗", e);
            result.put("status", "error");
            result.put("message", "執行批量回測失敗: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 交易對回測詳情頁面
     */
    @GetMapping("/details")
    public String backTestDetails(@RequestParam String symbol1, @RequestParam String symbol2, 
                                @RequestParam(required = false) Integer days,
                                @RequestParam(defaultValue = "1h") String interval,
                                Model model) {
        try {
            // 如果未指定回測天數，使用系統設置的窗口大小（單位1h）
            if (days == null) {
                days = tradingConfigService.getTradingConfigBean().getWindowSize();

            }
            
            // 執行回測並獲取結果（使用指定的時間間隔）
            PairsTradingResult result = pairsTradingService.backTestPairCombination(symbol1, symbol2, days, interval);
            
            if (result != null) {
                // 從 API 同步使用的數據獲取最新 Z 分數，確保與 API 返回的值一致

                
                model.addAttribute("result", result);
                model.addAttribute("symbol1", symbol1);
                model.addAttribute("symbol2", symbol2);
                model.addAttribute("interval", interval);
                model.addAttribute("days", days);
                
                // 添加系統配置信息
                model.addAttribute("windowSize", tradingConfigService.getTradingConfigBean().getWindowSize());
                model.addAttribute("entryThreshold", tradingConfigService.getEntryThreshold());
                model.addAttribute("exitThreshold", tradingConfigService.getExitThreshold());
            } else {
                model.addAttribute("error", "無法獲取回測結果");
            }
            
            model.addAttribute("activeTab", "backtest");
            return "backtest/details";
        } catch (Exception e) {
            log.error("載入回測詳情頁面失敗", e);
            model.addAttribute("error", "載入頁面數據失敗: " + e.getMessage());
            model.addAttribute("activeTab", "backtest");
            return "backtest/details";
        }
    }
    
    /**
     * 批量回測結果頁面
     */
    @GetMapping("/batch-results")
    public String batchBackTestResults(Model model) {
        try {
            // 獲取所有已保存的交易對組合
            List<String> savedCombinations = pairsTradingService.getSavedTradingPairCombinations();
            model.addAttribute("combinations", savedCombinations);
            
            // 添加系統配置信息
            model.addAttribute("windowSize", tradingConfigService.getTradingConfigBean().getWindowSize());
            model.addAttribute("entryThreshold", tradingConfigService.getEntryThreshold());
            model.addAttribute("exitThreshold", tradingConfigService.getExitThreshold());
            model.addAttribute("leverage", tradingConfigService.getTradingConfigBean().getLeverage());
            
            model.addAttribute("activeTab", "backtest");
            return "backtest/batch-results";
        } catch (Exception e) {
            log.error("載入批量回測結果頁面失敗", e);
            model.addAttribute("error", "載入頁面數據失敗: " + e.getMessage());
            model.addAttribute("activeTab", "backtest");
            return "backtest/batch-results";
        }
    }
}
