package andy.crypto.pairstrading.bot.pairstrading.controller;

import andy.crypto.pairstrading.bot.bean.PairsTradingServiceValueBean;
import andy.crypto.pairstrading.bot.bean.TradingConfigBean;
import andy.crypto.pairstrading.bot.pairstrading.service.BinanceApiService;
import andy.crypto.pairstrading.bot.pairstrading.service.PairsTradingService;
import andy.crypto.pairstrading.bot.pairstrading.service.impl.PairsTradingServiceImpl;
import andy.crypto.pairstrading.bot.service.TradingConfigService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 圖表數據 API 控制器
 * 用於提供各種交易圖表數據的 API 端點
 */
@Slf4j
@RestController
@RequestMapping("/api/charts")
public class ChartApiController {

    @Autowired
    private PairsTradingService pairsTradingService;
    
    @Autowired
    private TradingConfigService tradingConfigService;
    
    // 預設值僅作為備用，實際使用資料庫中的值
    @Value("${trading.pair.asset1:BNBUSDT}")
    private String asset1Default;

    @Value("${trading.pair.asset2:SOLUSDT}")
    private String asset2Default;
    
    /**
     * 獲取Z分數圖表數據
     * 支援外部傳入資產對參數，若未傳入則使用系統設定的資產對
     * 
     * @param asset1 第一個資產的代碼，可選參數
     * @param asset2 第二個資產的代碼，可選參數
     * @return 包含Z分數圖表數據的Map
     */
    @GetMapping("/z-score")
    public Map<String, Object> getZScoreData(
            @RequestParam(required = false) String asset1,
            @RequestParam(required = false) String asset2) {
        Map<String, Object> data = new HashMap<>();
        try {
            // 若未傳入資產參數，則使用系統設定的資產對
            if (asset1 == null || asset1.isEmpty()) {
                asset1 = tradingConfigService.getAsset1();
                // 如果資料庫中沒有設定，則使用預設值
                if (asset1 == null || asset1.isEmpty()) {
                    asset1 = asset1Default;
                }
            }
            
            if (asset2 == null || asset2.isEmpty()) {
                asset2 = tradingConfigService.getAsset2();
                // 如果資料庫中沒有設定，則使用預設值
                if (asset2 == null || asset2.isEmpty()) {
                    asset2 = asset2Default;
                }
            }
            
            // 獲取Z分數圖表數據作為ASCII圖表（保留向後兼容性）
            String zScoreChart = pairsTradingService.getZScoreChart(asset1, asset2);
            data.put("chart", zScoreChart);
            
            // 直接從原始數據生成JSON數據，用於JS圖表
            String chartJson = generateZScoreChartJson(asset1, asset2);
            data.put("chartData", chartJson);
            
            // 添加資產資訊
            data.put("asset1", asset1);
            data.put("asset2", asset2);
            data.put("status", "success");
        } catch (Exception e) {
            log.error("獲取Z分數圖表數據失敗", e);
            data.put("status", "error");
            data.put("message", "獲取數據失敗: " + e.getMessage());
        }
        return data;
    }
    
    /**
     * 直接從PairsTradingService獲取原始Z分數數據並轉為JSON
     * 不再依賴ASCII圖表的解析
     * 
     * @param asset1 資產1代碼，若為null則使用系統設定
     * @param asset2 資產2代碼，若為null則使用系統設定
     * @return Z分數數據的JSON字符串
     */
    private String generateZScoreChartJson(String asset1, String asset2) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, Object> chartData = new HashMap<>();
            
            // 直接從PairsTradingService獲取原始Z分數數據並轉為JSON
            List<Double> zScores = pairsTradingService.getZScoreHistory(asset1, asset2);
            if (zScores == null || zScores.isEmpty()) {
                return "{}";
            }

            PairsTradingServiceValueBean pairsTradingServiceValueBean = pairsTradingService.fetchMarketData(asset1, asset2);
            TradingConfigBean tradingConfigBean = tradingConfigService.getTradingConfigBean();

            // 獲取閾值配置
            double entryThreshold = 0;
            double exitThreshold = 0;
            try {
                entryThreshold = tradingConfigBean.getEntryThreshold();
                exitThreshold = tradingConfigBean.getExitThreshold();
            } catch (Exception e) {
                log.warn("無法獲取閾值設定，使用默認值");
            }
            
            // 獲取最新Z分數
            double latestZScore = 0;
            try {
                latestZScore = pairsTradingServiceValueBean.getLastZScore();
            } catch (Exception e) {
                log.warn("無法獲取最新Z分數，使用默認值");
                if (!zScores.isEmpty()) {
                    latestZScore = zScores.get(zScores.size() - 1);
                }
            }
            
            // 獲取ADF平穩性檢定結果
            boolean stationaryTest = false;
            try {
                stationaryTest = pairsTradingServiceValueBean.isStationaryTest();
            } catch (Exception e) {
                log.warn("無法獲取平穩性檢定結果，使用默認值");
            }
            
            // 獲取時間戳數據並格式化為可讀時間
            List<String> labels = new ArrayList<>();
            List<Long> timeHistory = pairsTradingService.getTimeHistory(asset1, asset2);
            
            if (timeHistory != null && !timeHistory.isEmpty()) {
                // 使用時間戳作為標籤
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MM-dd HH:mm");
                for (Long time : timeHistory) {
                    labels.add(dateFormat.format(new java.util.Date(time)));
                }
            } else {
                // 如果沒有時間數據，退回到使用序號
                for (int i = 0; i < zScores.size(); i++) {
                    labels.add(String.valueOf(i + 1));
                }
            }
            
            // 組合數據
            chartData.put("zScores", zScores);
            chartData.put("entryThreshold", entryThreshold);
            chartData.put("exitThreshold", exitThreshold);
            chartData.put("latestZScore", latestZScore);
            chartData.put("stationaryTest", stationaryTest);
            chartData.put("labels", labels);
            
            // 使用傳入的資產參數或從資料庫獲取最新的資產設定
            String currentAsset1 = asset1;
            String currentAsset2 = asset2;
            
            // 如果未傳入參數，則從資料庫獲取
            if (currentAsset1 == null || currentAsset1.isEmpty()) {
                currentAsset1 = tradingConfigService.getAsset1();
                // 如果資料庫中沒有設定，則使用預設值
                if (currentAsset1 == null || currentAsset1.isEmpty()) {
                    currentAsset1 = asset1Default;
                }
            }
            
            if (currentAsset2 == null || currentAsset2.isEmpty()) {
                currentAsset2 = tradingConfigService.getAsset2();
                // 如果資料庫中沒有設定，則使用預設值
                if (currentAsset2 == null || currentAsset2.isEmpty()) {
                    currentAsset2 = asset2Default;
                }
            }
            
            // 添加建議
            String suggestion = "無建議";
            if (latestZScore > entryThreshold) {
                suggestion = "做空 " + currentAsset1 + " 做多 " + currentAsset2;
            } else if (latestZScore < -entryThreshold) {
                suggestion = "做多 " + currentAsset1 + " 做空 " + currentAsset2;
            } else if (Math.abs(latestZScore) < exitThreshold) {
                suggestion = "平倉所有倉位";
            } else {
                suggestion = "持倉觀望";
            }
            chartData.put("suggestion", suggestion);
            
            return mapper.writeValueAsString(chartData);
        } catch (JsonProcessingException e) {
            log.error("轉換Z分數數據為JSON時發生錯誤", e);
            return "{}";
        }
    }
    
    /**
     * 獲取價差圖表數據
     * 支援外部傳入資產對參數，若未傳入則使用系統設定的資產對
     * 
     * @param asset1 第一個資產的代碼，可選參數
     * @param asset2 第二個資產的代碼，可選參數
     * @return 包含價差圖表數據的Map
     */
    @GetMapping("/spread")
    public Map<String, Object> getSpreadData(
            @RequestParam(required = false) String asset1,
            @RequestParam(required = false) String asset2) {
        Map<String, Object> data = new HashMap<>();
        try {
            // 若未傳入資產參數，則使用系統設定的資產對
            if (asset1 == null || asset1.isEmpty()) {
                asset1 = tradingConfigService.getAsset1();
                // 如果資料庫中沒有設定，則使用預設值
                if (asset1 == null || asset1.isEmpty()) {
                    asset1 = asset1Default;
                }
            }
            
            if (asset2 == null || asset2.isEmpty()) {
                asset2 = tradingConfigService.getAsset2();
                // 如果資料庫中沒有設定，則使用預設值
                if (asset2 == null || asset2.isEmpty()) {
                    asset2 = asset2Default;
                }
            }
            
            // 獲取價差圖表數據作為ASCII圖表（保留向後兼容性）
            String spreadChart = pairsTradingService.getBackTestSpreadChart(asset1,asset2);
            data.put("chart", spreadChart);
            
            // 直接從原始數據生成JSON數據，用於JS圖表
            String chartJson = generateSpreadChartJson(asset1, asset2);
            data.put("chartData", chartJson);
            
            // 添加資產資訊
            data.put("asset1", asset1);
            data.put("asset2", asset2);
            data.put("status", "success");
        } catch (Exception e) {
            log.error("獲取價差圖表數據失敗", e);
            data.put("status", "error");
            data.put("message", "獲取數據失敗: " + e.getMessage());
        }
        return data;
    }
    
    /**
     * 直接從PairsTradingService獲取原始價差數據並轉為JSON
     * 不再依賴ASCII圖表的解析
     * 
     * @param asset1 資產1代碼，若為null則使用系統設定
     * @param asset2 資產2代碼，若為null則使用系統設定
     * @return 價差數據的JSON字符串
     */
    private String generateSpreadChartJson(String asset1, String asset2) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, Object> chartData = new HashMap<>();
            
            // 從服務中獲取價差歷史數據
            List<Double> spreads = pairsTradingService.getSpreadHistory(asset1, asset2);
            if (spreads == null || spreads.isEmpty()) {
                return "{}";
            }

            PairsTradingServiceValueBean pairsTradingServiceValueBean = pairsTradingService.fetchMarketData(asset1, asset2);

            // 獲取價差均值和標準差
            double spreadMean = 0;
            double spreadStd = 0;
            try {
                spreadMean = pairsTradingServiceValueBean.getSpreadMean();;
                spreadStd = pairsTradingServiceValueBean.getSpreadStd();
            } catch (Exception e) {
                log.warn("無法獲取價差統計信息，使用默認值");
            }
            
            // 獲取最新價差
            double latestSpread = 0;
            try {
                latestSpread = pairsTradingServiceValueBean.getSpread();
            } catch (Exception e) {
                log.warn("無法獲取最新價差，使用默認值");
                if (!spreads.isEmpty()) {
                    latestSpread = spreads.get(spreads.size() - 1);
                }
            }
            
            // 獲取ADF平穩性檢定結果
            boolean stationaryTest = false;
            try {
                stationaryTest = pairsTradingServiceValueBean.isStationaryTest();
            } catch (Exception e) {
                log.warn("無法獲取平穩性檢定結果，使用默認值");
            }
            
            // 獲取時間戳數據並格式化為可讀時間
            List<String> labels = new ArrayList<>();
            List<Long> timeHistory = pairsTradingService.getTimeHistory(asset1, asset2);
            
            if (timeHistory != null && !timeHistory.isEmpty()) {
                // 使用時間戳作為標籤
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MM-dd HH:mm");
                for (Long time : timeHistory) {
                    labels.add(dateFormat.format(new java.util.Date(time)));
                }
            } else {
                // 如果沒有時間數據，退回到使用序號
                for (int i = 0; i < spreads.size(); i++) {
                    labels.add(String.valueOf(i + 1));
                }
            }
            
            // 使用傳入的資產參數或從資料庫獲取
            String currentAsset1 = asset1;
            String currentAsset2 = asset2;
            
            // 如果未傳入參數，則從資料庫獲取
            if (currentAsset1 == null || currentAsset1.isEmpty()) {
                currentAsset1 = tradingConfigService.getAsset1();
                // 如果資料庫中沒有設定，則使用預設值
                if (currentAsset1 == null || currentAsset1.isEmpty()) {
                    currentAsset1 = asset1Default;
                }
            }
            
            if (currentAsset2 == null || currentAsset2.isEmpty()) {
                currentAsset2 = tradingConfigService.getAsset2();
                // 如果資料庫中沒有設定，則使用預設值
                if (currentAsset2 == null || currentAsset2.isEmpty()) {
                    currentAsset2 = asset2Default;
                }
            }
            
            // 組合數據
            chartData.put("spreads", spreads);
            chartData.put("spreadMean", spreadMean);
            chartData.put("spreadStd", spreadStd);
            chartData.put("latestSpread", latestSpread);
            chartData.put("stationaryTest", stationaryTest);
            chartData.put("labels", labels);
            chartData.put("asset1", currentAsset1);
            chartData.put("asset2", currentAsset2);
            
            return mapper.writeValueAsString(chartData);
        } catch (JsonProcessingException e) {
            log.error("轉換價差數據為JSON時發生錯誤", e);
            return "{}";
        }
    }
    
    /**
     * 獲取交易信號報告
     */
    @GetMapping("/signal-report")
    public Map<String, Object> getSignalReport() {
        Map<String, Object> data = new HashMap<>();
        try {
            // 獲取交易信號報告
            String signalReport = pairsTradingService.getSignalReport();
            data.put("report", signalReport);
            data.put("status", "success");
        } catch (Exception e) {
            log.error("獲取交易信號報告失敗", e);
            data.put("status", "error");
            data.put("message", "獲取數據失敗: " + e.getMessage());
        }
        return data;
    }
}