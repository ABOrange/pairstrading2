package andy.crypto.pairstrading.bot.pairstrading.controller;

import andy.crypto.pairstrading.bot.bean.PairsTradingServiceValueBean;
import andy.crypto.pairstrading.bot.pairstrading.model.CandlestickData;
import andy.crypto.pairstrading.bot.pairstrading.model.PositionInfo;
import andy.crypto.pairstrading.bot.pairstrading.service.BinanceApiService;
import andy.crypto.pairstrading.bot.pairstrading.service.PairsTradingService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
public class JobController {

    @Autowired
    private Scheduler scheduler;
    
    @Autowired
    private PairsTradingService pairsTradingService;
    
    @Autowired
    private BinanceApiService binanceApiService;
    
    @Value("${trading.pair.asset1:BNBUSDT}")
    private String asset1;

    @Value("${trading.pair.asset2:SOLUSDT}")
    private String asset2;

    /**
     * 手動觸發配對交易任務
     */
    @GetMapping("/job/run-manually")
    public String runTradingJobManually() {
        log.info("手動觸發配對交易任務");
        
        try {
            // 執行服務層邏輯
            PairsTradingServiceValueBean pairsTradingServiceValueBean = pairsTradingService.fetchMarketData();
            pairsTradingService.analyzeCorrelation(pairsTradingServiceValueBean);
            boolean hasSignal = pairsTradingService.calculateSignal(pairsTradingServiceValueBean);
            
            if (hasSignal) {
                pairsTradingService.executeTrade(true);
                return "配對交易任務已手動觸發並執行完成，檢測到交易信號並執行交易";
            } else {
                return "配對交易任務已手動觸發並執行完成，未檢測到交易信號";
            }
        } catch (Exception e) {
            log.error("手動觸發任務失敗", e);
            return "執行失敗: " + e.getMessage();
        }
    }
    
    /**
     * 檢查排程任務狀態
     */
    @GetMapping("/job/status")
    public String getJobStatus() {
        try {
            JobKey jobKey = new JobKey("pairsTradingJob", "pairsTradingGroup");
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
            
            if (jobDetail == null) {
                return "排程任務不存在";
            }
            
            Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey("pairsTradingTrigger", "pairsTradingGroup"));
            Trigger.TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
            
            return "排程任務狀態: " + triggerState.name();
        } catch (SchedulerException e) {
            log.error("獲取任務狀態失敗", e);
            return "獲取狀態失敗: " + e.getMessage();
        }
    }
    
    /**
     * 測試幣安API連接
     */
    @GetMapping("/binance/test-connection")
    public Map<String, Object> testBinanceConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 獲取帳戶餘額
            Map<String, BigDecimal> balances = binanceApiService.getAccountBalance();
            result.put("balances", balances);
            
            // 獲取配對資產最新價格
            BigDecimal asset1Price = binanceApiService.getLatestPrice(asset1);
            BigDecimal asset2Price = binanceApiService.getLatestPrice(asset2);
            
            Map<String, BigDecimal> prices = new HashMap<>();
            prices.put(asset1, asset1Price);
            prices.put(asset2, asset2Price);
            result.put("prices", prices);
            
            result.put("status", "success");
            result.put("message", "成功連接幣安API");
        } catch (Exception e) {
            log.error("測試幣安API連接失敗", e);
            result.put("status", "error");
            result.put("message", "連接失敗: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 獲取K線數據
     */
    @GetMapping("/binance/klines")
    public Map<String, Object> getKlines(@RequestParam String symbol, 
                                        @RequestParam(defaultValue = "1h") String interval,
                                        @RequestParam(defaultValue = "30") Integer limit) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<CandlestickData> klines = binanceApiService.getCandlestickData(symbol, interval, limit);
            result.put("klines", klines);
            result.put("status", "success");
        } catch (Exception e) {
            log.error("獲取K線數據失敗", e);
            result.put("status", "error");
            result.put("message", "獲取K線數據失敗: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 獲取持倉信息
     */
    @GetMapping("/binance/positions")
    public Map<String, Object> getPositions(@RequestParam(required = false) String symbol) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<PositionInfo> positions = binanceApiService.getPositionInfo(symbol);
            result.put("positions", positions);
            result.put("status", "success");
        } catch (Exception e) {
            log.error("獲取持倉信息失敗", e);
            result.put("status", "error");
            result.put("message", "獲取持倉信息失敗: " + e.getMessage());
        }
        
        return result;
    }
}
