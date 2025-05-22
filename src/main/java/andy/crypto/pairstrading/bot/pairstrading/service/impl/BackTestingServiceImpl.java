package andy.crypto.pairstrading.bot.pairstrading.service.impl;

import andy.crypto.pairstrading.bot.bean.PairsTradingServiceValueBean;
import andy.crypto.pairstrading.bot.bean.TradingConfigBean;
import andy.crypto.pairstrading.bot.pairstrading.model.CandlestickData;
import andy.crypto.pairstrading.bot.pairstrading.model.PairsTradingResult;
import andy.crypto.pairstrading.bot.pairstrading.service.BackTestingService;
import andy.crypto.pairstrading.bot.pairstrading.service.BinanceApiService;
import andy.crypto.pairstrading.bot.pairstrading.service.MarketDataService;
import andy.crypto.pairstrading.bot.pairstrading.service.TradingPairManagementService;
import andy.crypto.pairstrading.bot.pairstrading.util.ConsoleChartUtil;
import andy.crypto.pairstrading.bot.service.TradingConfigService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 回測服務實現類
 */
@Slf4j
@Service
public class BackTestingServiceImpl implements BackTestingService {

    @Autowired
    private BinanceApiService binanceApiService;
    
    @Autowired
    private MarketDataService marketDataService;
    
    @Autowired
    private TradingPairManagementService tradingPairManagementService;
    
    @Autowired
    @Lazy
    private TradingConfigService tradingConfigService;
    
    // 回測線程池，用於並行回測多個交易對
    private ExecutorService backTestExecutor = Executors.newFixedThreadPool(4);
    
    /**
     * 從配置服務加載配置
     */
    private TradingConfigBean loadConfig() {
        return tradingConfigService.getTradingConfigBean();
    }
    
    @Override
    public int calculateBackTestTimeLength(Integer backTestDays) {
        log.info("backTestDays:{}",backTestDays);
        TradingConfigBean tradingConfigBean = loadConfig();
        // 如果沒有指定天數或為0，則使用系統窗口大小
        int time = (backTestDays == null || backTestDays <= 0) ? tradingConfigBean.getWindowSize() : backTestDays;
        // 確保至少為1
        return Math.max(1, time);
    }
    
    @Override
    public PairsTradingResult backTestPairCombination(String symbol1, String symbol2, Integer backTestDays) {
        // 使用預設間隔 "1h"
        return backTestPairCombination(symbol1, symbol2, backTestDays, "1h");
    }
    
    @Override
    public PairsTradingResult backTestPairCombination(String symbol1, String symbol2, Integer backTestDays, String interval) {
        String pairKey = symbol1 + "," + symbol2 + "_" + interval;

        
        log.info("開始回測交易對組合: {} (間隔: {})", pairKey, interval);
        
        try {
            // 加載系統配置
            TradingConfigBean tradingConfigBean = loadConfig();
            
            // 計算回測時間長度
            int dataPoints = calculateBackTestTimeLength(backTestDays);
            
            log.info("使用時間間隔: {}, 回測資料點數: {}", interval, dataPoints);
            
            // 獲取歷史K線數據
            List<CandlestickData> asset1Data = binanceApiService.getCandlestickData(symbol1, interval, dataPoints);
            List<CandlestickData> asset2Data = binanceApiService.getCandlestickData(symbol2, interval, dataPoints);
            
            log.info("獲取到 {} 的K線數據 {} 條", symbol1, asset1Data.size());
            log.info("獲取到 {} 的K線數據 {} 條", symbol2, asset2Data.size());
            
            // 檢查數據是否足夠
            if (asset1Data.size() < tradingConfigBean.getWindowSize() || asset2Data.size() < tradingConfigBean.getWindowSize()) {
                log.warn("歷史數據不足以進行分析，需要至少 {} 個資料點，但只獲取到 {} 和 {} 個", 
                        tradingConfigBean.getWindowSize(), asset1Data.size(), asset2Data.size());
                return null;
            }
            
            // 轉換為價格序列用於分析
            double[] asset1Prices = asset1Data.stream()
                    .mapToDouble(data -> data.getClose().doubleValue())
                    .toArray();
            
            double[] asset2Prices = asset2Data.stream()
                    .mapToDouble(data -> data.getClose().doubleValue())
                    .toArray();
            
            // 更新價格數據並計算指標
            PairsTradingServiceValueBean pairsTradingServiceValueBean = marketDataService.updatePriceData(asset1Prices, asset2Prices, asset1Data);
            
            // 計算成功套利次數
            int successfulTradeCount = countSuccessfulTradesInBackTest(pairsTradingServiceValueBean.getZScoreHistory(), tradingConfigBean.getEntryThreshold(), tradingConfigBean.getExitThreshold());
            
            // 計算爆倉次數
            int liquidationCount = countLiquidationsInBackTest(pairsTradingServiceValueBean.getZScoreHistory(), asset1Data, asset2Data, tradingConfigBean.getEntryThreshold(), tradingConfigBean.getExitThreshold(), tradingConfigBean.getLeverage());
            
            // 獲取最新價格
            BigDecimal asset1Price = asset1Data.get(asset1Data.size() - 1).getClose();
            BigDecimal asset2Price = asset2Data.get(asset2Data.size() - 1).getClose();
            
            // 生成圖表
            String zScoreChartStr = ConsoleChartUtil.drawZScoreChart(pairsTradingServiceValueBean.getZScoreHistory(), tradingConfigBean.getEntryThreshold(), tradingConfigBean.getExitThreshold());
            String spreadChartStr = ConsoleChartUtil.drawSpreadChart(pairsTradingServiceValueBean.getSpreadHistory(), pairsTradingServiceValueBean.getSpreadMean(), pairsTradingServiceValueBean.getSpreadStd());

            
            // 使用當前Z分數判斷交易信號類型
            PairsTradingResult.SignalType signalType = determineSignalType(pairsTradingServiceValueBean.getLastZScore(), tradingConfigBean.getEntryThreshold(), tradingConfigBean.getExitThreshold());
            
            // 生成信號強度評級
            String signalRating = evaluateSignalStrength(pairsTradingServiceValueBean.getLastZScore());
            
            // 生成信號報告
            String signalReport = ConsoleChartUtil.generateSignalReport(
                    pairsTradingServiceValueBean.getLastZScore(), tradingConfigBean.getEntryThreshold(), tradingConfigBean.getExitThreshold(),
                    symbol1, symbol2, pairsTradingServiceValueBean.getCorrelation(), false, pairsTradingServiceValueBean.getAlpha(), pairsTradingServiceValueBean.getBeta());
            
            // 創建回測結果
            PairsTradingResult result = PairsTradingResult.builder()
                    .id(UUID.randomUUID().toString())
                    .timestamp(LocalDateTime.now())
                    .asset1(symbol1)
                    .asset2(symbol2)
                    .correlation(pairsTradingServiceValueBean.getCorrelation())
                    .beta(pairsTradingServiceValueBean.getBeta())
                    .spread(pairsTradingServiceValueBean.getSpread())
                    .spreadMean(pairsTradingServiceValueBean.getSpreadMean())
                    .spreadStd(pairsTradingServiceValueBean.getSpreadStd())
                    .zScore(pairsTradingServiceValueBean.getLastZScore())
                    .asset1Price(asset1Price)
                    .asset2Price(asset2Price)
                    .signalType(signalType)
                    .signalRating(signalRating)
                    .zScoreChart(zScoreChartStr)
                    .spreadChart(spreadChartStr)
                    .signalReport(signalReport)
                    .interval(interval)
                    .arbitrageCount(successfulTradeCount)
                    .liquidationCount(liquidationCount)
                    .stationaryTest(pairsTradingServiceValueBean.isStationaryTest())
                    .build();

            
            log.info("完成交易對組合回測: {}，相關性: {}, Z分數: {}, 成功套利次數: {}, 爆倉次數: {}", 
                    pairKey, pairsTradingServiceValueBean.getCorrelation(), pairsTradingServiceValueBean.getLastZScore(), 
                    successfulTradeCount, liquidationCount);
            
            return result;
        } catch (Exception e) {
            log.error("回測交易對組合失敗: {}, 間隔: {}", pairKey, interval, e);
            return null;
        }
    }
    
    /**
     * 根據Z分數判斷交易信號類型
     */
    private PairsTradingResult.SignalType determineSignalType(double zScore, double entryThreshold, double exitThreshold) {
        if (Math.abs(zScore) < exitThreshold) {
            return PairsTradingResult.SignalType.CLOSE_POSITIONS;
        } else if (zScore > entryThreshold) {
            return PairsTradingResult.SignalType.SHORT_ASSET1_LONG_ASSET2;
        } else if (zScore < -entryThreshold) {
            return PairsTradingResult.SignalType.LONG_ASSET1_SHORT_ASSET2;
        } else {
            return PairsTradingResult.SignalType.NO_SIGNAL;
        }
    }
    
    /**
     * 評估信號強度
     */
    private String evaluateSignalStrength(double zScore) {
        double absZScore = Math.abs(zScore);
        if (absZScore > 3.0) {
            return "極強";
        } else if (absZScore > 2.5) {
            return "很強";
        } else if (absZScore > 2.0) {
            return "強";
        } else if (absZScore > 1.5) {
            return "中等";
        } else if (absZScore > 1.0) {
            return "弱";
        } else {
            return "很弱";
        }
    }
    
    @Override
    public int countSuccessfulTradesInBackTest(List<Double> zScoreHistory, double entryThreshold, double exitThreshold) {
        if (zScoreHistory == null || zScoreHistory.isEmpty()) {
            return 0;
        }
        
        int successCount = 0;
        boolean inLongPosition = false;   // 做多資產1，做空資產2
        boolean inShortPosition = false;  // 做空資產1，做多資產2
        double entryZScore = 0.0;
        
        for (Double zScore : zScoreHistory) {
            // 判斷是否應該入場
            if (!inLongPosition && !inShortPosition) {
                if (zScore < -entryThreshold) {
                    // 入場：做多資產1，做空資產2
                    inLongPosition = true;
                    entryZScore = zScore;
                } else if (zScore > entryThreshold) {
                    // 入場：做空資產1，做多資產2
                    inShortPosition = true;
                    entryZScore = zScore;
                }
            } 
            // 判斷是否應該出場
            else if (inLongPosition) {
                if (Math.abs(zScore) < exitThreshold) {
                    // 從做多資產1，做空資產2的倉位出場
                    inLongPosition = false;
                    // 如果Z分數從負值向零移動，表示套利成功
                    if (zScore > entryZScore) {
                        successCount++;
                    }
                }
            } else if (inShortPosition) {
                if (Math.abs(zScore) < exitThreshold) {
                    // 從做空資產1，做多資產2的倉位出場
                    inShortPosition = false;
                    // 如果Z分數從正值向零移動，表示套利成功
                    if (zScore < entryZScore) {
                        successCount++;
                    }
                }
            }
        }
        
        return successCount;
    }
    
    /**
     * 計算回測期間的爆倉次數
     * 當使用槓桿交易時，如果價格波動過大導致虧損超過保證金，就會發生爆倉
     * 根據槓桿倍數動態計算爆倉閾值
     */
    private int countLiquidationsInBackTest(List<Double> zScoreHistory, List<CandlestickData> asset1Data, List<CandlestickData> asset2Data, double entryThreshold, double exitThreshold, int leverage) {
        if (zScoreHistory == null || zScoreHistory.isEmpty() || leverage <= 1) {
            return 0; // 如果不使用槓桿或數據為空，不會發生爆倉
        }
        
        int liquidationCount = 0;
        boolean inLongPosition = false;   // 做多資產1，做空資產2
        boolean inShortPosition = false;  // 做空資產1，做多資產2
        
        // 入場價格及位置
        BigDecimal entryAsset1Price = null;
        BigDecimal entryAsset2Price = null;
        int entryPositionIndex = -1;
        
        // 計算爆倉閾值，考慮槓桿和維持保證金率
        // 維持保證金率通常取1/槓桿的80%左右，例如:
        // 槓桿10倍，維持保證金率約8%
        // 槓桿20倍，維持保證金率約4%
        double maintenanceMarginRate = 0.8 / leverage; // 維持保證金率
        double liquidationThreshold = maintenanceMarginRate; // 當淨虧損達到維持保證金率時觸發爆倉
        
        log.debug("使用槓桿: {}倍，維持保證金率: {}%, 爆倉閾值: {}%", 
                leverage, maintenanceMarginRate * 100, liquidationThreshold * 100);
        
        for (int i = 0; i < zScoreHistory.size(); i++) {
            Double zScore = zScoreHistory.get(i);
            
            // 確保我們有對應的價格數據
            if (i >= asset1Data.size() || i >= asset2Data.size()) {
                break;
            }
            
            BigDecimal asset1Price = asset1Data.get(i).getClose();
            BigDecimal asset2Price = asset2Data.get(i).getClose();
            
            // 判斷是否應該入場
            if (!inLongPosition && !inShortPosition) {
                if (zScore < -entryThreshold) {
                    // 入場：做多資產1，做空資產2
                    inLongPosition = true;
                    entryAsset1Price = asset1Price;
                    entryAsset2Price = asset2Price;
                    entryPositionIndex = i;
                    log.debug("做多入場 (索引: {})：資產1={}, 資產2={}, Z分數={}", 
                            i, entryAsset1Price, entryAsset2Price, zScore);
                } else if (zScore > entryThreshold) {
                    // 入場：做空資產1，做多資產2
                    inShortPosition = true;
                    entryAsset1Price = asset1Price;
                    entryAsset2Price = asset2Price;
                    entryPositionIndex = i;
                    log.debug("做空入場 (索引: {})：資產1={}, 資產2={}, Z分數={}", 
                            i, entryAsset1Price, entryAsset2Price, zScore);
                }
            } 
            // 判斷是否發生爆倉或應該出場
            else if (inLongPosition) {
                try {
                    // 檢查資產1是否下跌過多（做多資產1）或資產2是否上漲過多（做空資產2）
                    double asset1PriceChange = asset1Price.subtract(entryAsset1Price)
                            .divide(entryAsset1Price, 8, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double asset2PriceChange = asset2Price.subtract(entryAsset2Price)
                            .divide(entryAsset2Price, 8, BigDecimal.ROUND_HALF_UP).doubleValue();
                    
                    // 計算綜合變化（考慮兩個資產的變化）
                    // 在做多資產1，做空資產2的情況下:
                    // netChange = asset1的變化率 - asset2的變化率
                    // netChange為負表示虧損 (asset1下跌或asset2上漲)
                    double netChange = asset1PriceChange - asset2PriceChange;
                    
                    // 計算槓桿交易的實際盈虧 (以槓桿倍數放大)
                    double leveragedPnL = netChange * leverage;
                    
                    // 如果淨虧損（絕對值）超過維持保證金率，就會發生爆倉
                    if (netChange < -liquidationThreshold) {
                        // 爆倉：做多資產1，做空資產2，但資產1下跌過多或資產2上漲過多
                        liquidationCount++;
                        inLongPosition = false;
                        log.debug("做多爆倉發生 (索引: {})：淨變化={}%, 槓桿後虧損={}%, 爆倉閾值={}%", 
                                i, netChange * 100, leveragedPnL * 100, liquidationThreshold * 100);
                    } else if (Math.abs(zScore) < exitThreshold) {
                        // 正常出場 (Z分數回歸至出場閾值內)
                        inLongPosition = false;
                        log.debug("做多正常出場 (索引: {})：Z分數={}, 淨變化={}%", i, zScore, netChange * 100);
                    }
                } catch (ArithmeticException e) {
                    log.error("計算價格變化時發生除以零錯誤: 索引={}, 資產1={}, 資產2={}", i, asset1Price, asset2Price);
                    inLongPosition = false;
                }
            } else if (inShortPosition) {
                try {
                    // 檢查資產1是否上漲過多（做空資產1）或資產2是否下跌過多（做多資產2）
                    double asset1PriceChange = asset1Price.subtract(entryAsset1Price)
                            .divide(entryAsset1Price, 8, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double asset2PriceChange = asset2Price.subtract(entryAsset2Price)
                            .divide(entryAsset2Price, 8, BigDecimal.ROUND_HALF_UP).doubleValue();
                    
                    // 計算綜合變化（考慮兩個資產的變化）
                    // 在做空資產1，做多資產2的情況下:
                    // netChange = -(asset1的變化率) + asset2的變化率
                    // netChange為負表示虧損 (asset1上漲或asset2下跌)
                    double netChange = -asset1PriceChange + asset2PriceChange;
                    
                    // 計算槓桿交易的實際盈虧 (以槓桿倍數放大)
                    double leveragedPnL = netChange * leverage;
                    
                    // 如果淨虧損（絕對值）超過維持保證金率，就會發生爆倉
                    if (netChange < -liquidationThreshold) {
                        // 爆倉：做空資產1，做多資產2，但資產1上漲過多或資產2下跌過多
                        liquidationCount++;
                        inShortPosition = false;
                        log.debug("做空爆倉發生 (索引: {})：淨變化={}%, 槓桿後虧損={}%, 爆倉閾值={}%", 
                                i, netChange * 100, leveragedPnL * 100, liquidationThreshold * 100);
                    } else if (Math.abs(zScore) < exitThreshold) {
                        // 正常出場 (Z分數回歸至出場閾值內)
                        inShortPosition = false;
                        log.debug("做空正常出場 (索引: {})：Z分數={}, 淨變化={}%", i, zScore, netChange * 100);
                    }
                } catch (ArithmeticException e) {
                    log.error("計算價格變化時發生除以零錯誤: 索引={}, 資產1={}, 資產2={}", i, asset1Price, asset2Price);
                    inShortPosition = false;
                }
            }
        }
        
        // 如果到了回測結束仍持有倉位，檢查是否會爆倉
        if (inLongPosition || inShortPosition) {
            log.debug("回測結束時仍有未平倉的倉位");
        }
        
        return liquidationCount;
    }
    
    @Override
    public Map<String, PairsTradingResult> batchBackTestPairCombinations(List<String> pairCombinations, Integer backTestDays, String interval) {
        if (pairCombinations == null || pairCombinations.isEmpty()) {
            return new HashMap<>();
        }
        
        // 加載系統配置
        TradingConfigBean tradingConfigBean = loadConfig();

        // 計算回測時間長度
        int timeLength = calculateBackTestTimeLength(backTestDays);

        log.info("開始批量回測 {} 個交易對組合 (間隔: {}, 時間長度: {})", pairCombinations.size(), interval, timeLength);
        
        // 創建結果映射
        Map<String, PairsTradingResult> results = new HashMap<>();
        
        // 創建完成計數器，用於追蹤進度
        final int[] completedCount = {0};
        final int totalCount = pairCombinations.size();
        
        // 使用並行流同時處理多個交易對組合
        List<CompletableFuture<Void>> futures = pairCombinations.stream()
                .map(pairCombination -> CompletableFuture.runAsync(() -> {
                    try {
                        // 驗證交易對組合格式
                        if (!tradingPairManagementService.validateTradingPairCombination(pairCombination)) {
                            log.warn("交易對組合格式不正確，跳過: {}", pairCombination);
                            return;
                        }
                        
                        // 解析交易對
                        String[] pairs = pairCombination.split(",");
                        
                        // 進行回測（使用指定的時間間隔）
                        PairsTradingResult result = backTestPairCombination(pairs[0], pairs[1], timeLength, interval);
                        
                        // 更新結果映射
                        if (result != null) {
                            synchronized (results) {
                                results.put(pairCombination, result);
                            }
                        }
                    } catch (Exception e) {
                        log.error("回測交易對組合失敗: {}, 間隔: {}", pairCombination, interval, e);
                    } finally {
                        // 更新完成計數並記錄進度
                        updateProgressCounter(completedCount, totalCount);
                    }
                }, backTestExecutor))
                .collect(Collectors.toList());
        
        // 等待所有任務完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        log.info("批量回測完成，成功回測 {} 個交易對組合 (間隔: {})", results.size(), interval);
        
        return results;
    }
    
    /**
     * 更新進度計數器並記錄進度
     */
    private void updateProgressCounter(int[] completedCount, int totalCount) {
        synchronized (completedCount) {
            completedCount[0]++;
            if (completedCount[0] % 5 == 0 || completedCount[0] == totalCount) {
                log.info("回測進度: {}/{} ({}%)", 
                        completedCount[0], totalCount, 
                        String.format("%.1f", (double)completedCount[0] / totalCount * 100));
            }
        }
    }
    
    @Override
    public Map<String, PairsTradingResult> backTestAllSavedPairCombinations(Integer backTestDays, String interval) {
        // 計算回測時間長度
        int time = calculateBackTestTimeLength(backTestDays);
        
        // 獲取所有已保存的交易對組合
        List<String> pairCombinations = tradingPairManagementService.getSavedTradingPairCombinations();
        
        if (pairCombinations.isEmpty()) {
            log.warn("沒有找到已保存的交易對組合，無法執行回測");
            return new HashMap<>();
        }
        
        log.info("開始批量回測所有已保存的交易對組合: {} 個 (間隔: {}, 天數: {})", 
                 pairCombinations.size(), interval, time);
        
        // 調用批量回測方法（使用指定的時間間隔）
        return batchBackTestPairCombinations(pairCombinations, time, interval);
    }
    
    @Override
    public String getBackTestZScoreChart(String symbol1, String symbol2) {
        TradingConfigBean tradingConfigBean = loadConfig();
        
        // 嘗試執行回測
        PairsTradingResult result = backTestPairCombination(symbol1, symbol2, tradingConfigBean.getWindowSize());
        if (result != null) {
            return result.getZScoreChart();
        }
        
        return "無法獲取回測Z分數圖表，請先執行回測";
    }
    
    @Override
    public String getBackTestSpreadChart(String symbol1, String symbol2) {
        TradingConfigBean tradingConfigBean = loadConfig();
        
        // 嘗試執行回測
        PairsTradingResult result = backTestPairCombination(symbol1, symbol2, tradingConfigBean.getWindowSize());
        if (result != null) {
            return result.getSpreadChart();
        }
        
        return "無法獲取回測價差圖表，請先執行回測";
    }
}
