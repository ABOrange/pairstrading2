package andy.crypto.pairstrading.bot.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import andy.crypto.pairstrading.bot.entity.TradingConfig;
import andy.crypto.pairstrading.bot.repository.TradingConfigRepository;
import andy.crypto.pairstrading.bot.service.TradingConfigService;
import lombok.extern.slf4j.Slf4j;

/**
 * 交易配置控制器
 */
@Controller
@RequestMapping("/settings/trading")
@Slf4j
public class TradingConfigController {

    @Autowired
    private TradingConfigService tradingConfigService;
    
    @Autowired
    private TradingConfigRepository tradingConfigRepository;
    
    /**
     * 獲取所有交易配置
     */
    @GetMapping("/list")
    @ResponseBody
    public ResponseEntity<List<TradingConfig>> listConfigs() {
        List<TradingConfig> configs = tradingConfigRepository.findAll();
        return ResponseEntity.ok(configs);
    }
    
    /**
     * 更新通用設定
     */
    @PostMapping("/update-general")
    @ResponseBody
    public ResponseEntity<Map<String, String>> updateGeneralSettings(
            @RequestParam("tradingEnable") boolean tradingEnable) {
        
        tradingConfigService.saveOrUpdateConfig(
                TradingConfigService.TRADING_ENABLE,
                String.valueOf(tradingEnable),
                "在測試網上啟用實際交易",
                TradingConfigService.CATEGORY_GENERAL);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "通用設定已更新");
        
        log.info("通用設定已更新 - 交易功能: {}", tradingEnable ? "啟用" : "禁用");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 更新交易對設定
     */
    @PostMapping("/update-pair")
    @ResponseBody
    public ResponseEntity<Map<String, String>> updatePairSettings(
            @RequestParam("asset1") String asset1,
            @RequestParam("asset2") String asset2) {
        
        // 驗證資產名稱格式
        if (!asset1.endsWith("USDT") || !asset2.endsWith("USDT")) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "資產名稱必須以USDT結尾");
            return ResponseEntity.badRequest().body(response);
        }
        
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
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "交易對設定已更新");
        
        log.info("交易對設定已更新 - 資產1: {}, 資產2: {}", asset1, asset2);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 更新窗口設定
     */
    @PostMapping("/update-window")
    @ResponseBody
    public ResponseEntity<Map<String, String>> updateWindowSettings(
            @RequestParam("windowSize") int windowSize) {
        
        // 驗證窗口大小合理性
        if (windowSize < 10 || windowSize > 1000) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "窗口大小必須在10-1000之間");
            return ResponseEntity.badRequest().body(response);
        }
        
        tradingConfigService.saveOrUpdateConfig(
                TradingConfigService.WINDOW_SIZE,
                String.valueOf(windowSize),
                "用於計算的歷史數據點數量",
                TradingConfigService.CATEGORY_WINDOW);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "窗口設定已更新");
        
        log.info("窗口設定已更新 - 大小: {}", windowSize);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 更新閾值設定
     */
    @PostMapping("/update-threshold")
    @ResponseBody
    public ResponseEntity<Map<String, String>> updateThresholdSettings(
            @RequestParam("entryThreshold") double entryThreshold,
            @RequestParam("exitThreshold") double exitThreshold) {
        
        // 驗證閾值合理性
        if (entryThreshold <= 0 || exitThreshold <= 0 || exitThreshold >= entryThreshold) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "入場閾值必須大於出場閾值，且兩者均需大於0");
            return ResponseEntity.badRequest().body(response);
        }
        
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
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "閾值設定已更新");
        
        log.info("閾值設定已更新 - 入場: {}, 出場: {}", entryThreshold, exitThreshold);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 更新倉位設定
     */
    @PostMapping("/update-position")
    @ResponseBody
    public ResponseEntity<Map<String, String>> updatePositionSettings(
            @RequestParam("positionSize") BigDecimal positionSize,
            @RequestParam("amountBased") boolean amountBased,
            @RequestParam("leverage") int leverage) {
        
        // 驗證倉位大小和槓桿倍率合理性
        if (positionSize.compareTo(BigDecimal.ZERO) <= 0) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "倉位大小必須大於0");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (leverage < 1 || leverage > 125) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "槓桿倍率必須在1-125之間");
            return ResponseEntity.badRequest().body(response);
        }
        
        tradingConfigService.saveOrUpdateConfig(
                TradingConfigService.POSITION_SIZE,
                positionSize.toString(),
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
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "倉位設定已更新");
        
        log.info("倉位設定已更新 - 大小: {}, 金額計價: {}, 槓桿: {}x", 
                positionSize, amountBased ? "是" : "否", leverage);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 更新控制台設定
     */
    @PostMapping("/update-console")
    @ResponseBody
    public ResponseEntity<Map<String, String>> updateConsoleSettings(
            @RequestParam("consoleChart") boolean consoleChart,
            @RequestParam("consoleSignal") boolean consoleSignal) {
        
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
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "控制台設定已更新");
        
        log.info("控制台設定已更新 - 圖表顯示: {}, 信號報告顯示: {}", 
                consoleChart ? "啟用" : "禁用", consoleSignal ? "啟用" : "禁用");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 重置所有配置為預設值
     */
    @PostMapping("/reset")
    @ResponseBody
    public ResponseEntity<Map<String, String>> resetAllConfigs() {
        // 刪除所有現有配置
        tradingConfigRepository.deleteAll();
        
        // 重新初始化配置 (將觸發 TradingConfigInitializer)
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "所有交易配置已重置為預設值");
        
        log.info("所有交易配置已重置為預設值");
        
        return ResponseEntity.ok(response);
    }
}
