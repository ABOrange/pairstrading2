package andy.crypto.pairstrading.bot.pairstrading.service.impl;

import andy.crypto.pairstrading.bot.bean.PairsTradingServiceValueBean;
import andy.crypto.pairstrading.bot.bean.TradingConfigBean;
import andy.crypto.pairstrading.bot.pairstrading.model.CandlestickData;
import andy.crypto.pairstrading.bot.pairstrading.service.BinanceApiService;
import andy.crypto.pairstrading.bot.pairstrading.service.MarketDataService;
import andy.crypto.pairstrading.bot.pairstrading.util.ConsoleChartUtil;
import andy.crypto.pairstrading.bot.pairstrading.util.StatisticalUtils;
import andy.crypto.pairstrading.bot.pairstrading.util.StatisticalUtils.OrthogonalRegressionResult;
import andy.crypto.pairstrading.bot.service.TradingConfigService;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 市場數據服務實現類
 */
@Slf4j
@Service
public class MarketDataServiceImpl implements MarketDataService {

    @Autowired
    private BinanceApiService binanceApiService;
    
    @Autowired
    @Lazy
    private TradingConfigService tradingConfigService;
    
    // 標記是否為第一次運行
    private static boolean firstRun = true;
    
    /**
     * 從配置服務加載配置
     */
    private TradingConfigBean loadConfig() {
        return tradingConfigService.getTradingConfigBean();
    }

    @Override
    public PairsTradingServiceValueBean fetchMarketData() {
        return fetchMarketData(null, null);
    }

    @Override
    public PairsTradingServiceValueBean fetchMarketData(String asset1, String asset2) {
        // 加載最新配置
        TradingConfigBean tradingConfigBean = loadConfig();
        String targetAsset1 = asset1;
        String targetAsset2 = asset2;

        if(StringUtils.isBlank(asset1)) {
            targetAsset1 = tradingConfigBean.getAsset1();
        }
        if(StringUtils.isBlank(asset2)) {
            targetAsset2 = tradingConfigBean.getAsset2();
        }

        
        log.info("獲取 {} 和 {} 的市場數據", targetAsset1, targetAsset2);
        
        try {
            // 只在第一次運行時顯示配置信息
            if (firstRun) {
                log.info("控制台圖表顯示: {}", tradingConfigBean.isConsoleChartEnabled() ? "啟用" : "禁用");
                log.info("控制台信號報告顯示: {}", tradingConfigBean.isConsoleSignalEnabled() ? "啟用" : "禁用");
                log.info("倉位大小設定: {} {}", tradingConfigBean.getPositionSize(), tradingConfigBean.isAmountBased() ? "USDT (金額)" : "合約數量");
                log.info("合約槓桿倍率: {}x", tradingConfigBean.getLeverage());
                log.info("窗口大小設定: {} 個資料點", tradingConfigBean.getWindowSize());
                firstRun = false;
            }
            
            // 獲取足夠長時間的K線數據來計算相關性和回歸係數
            // 獲取至少windowSize*2的數據點，確保有足夠的數據進行分析
            int requiredDataPoints = tradingConfigBean.getWindowSize() * 2;
            log.info("正在獲取 {} 個資料點用於分析 (窗口大小: {})", requiredDataPoints, tradingConfigBean.getWindowSize());
            
            List<CandlestickData> asset1Data = binanceApiService.getCandlestickData(targetAsset1, "1h", requiredDataPoints);
            List<CandlestickData> asset2Data = binanceApiService.getCandlestickData(targetAsset2, "1h", requiredDataPoints);
            
            log.info("獲取到 {} 的K線數據 {} 條", targetAsset1, asset1Data.size());
            log.info("獲取到 {} 的K線數據 {} 條", targetAsset2, asset2Data.size());
            
            // 檢查數據是否足夠
            if (asset1Data.size() < tradingConfigBean.getWindowSize() || asset2Data.size() < tradingConfigBean.getWindowSize()) {
                log.warn("歷史數據不足以進行分析，需要至少 {} 個資料點，但只獲取到 {} 和 {} 個",
                        tradingConfigBean.getWindowSize(), asset1Data.size(), asset2Data.size());
                return null;
            }
            
            // 計算最新價格
            BigDecimal asset1Price = asset1Data.get(asset1Data.size() - 1).getClose();
            BigDecimal asset2Price = asset2Data.get(asset2Data.size() - 1).getClose();
            
            log.info("{} 最新價格: {}", targetAsset1, asset1Price);
            log.info("{} 最新價格: {}", targetAsset2, asset2Price);
            
            // 轉換為價格序列用於進一步分析
            double[] asset1Prices = asset1Data.stream()
                    .mapToDouble(data -> data.getClose().doubleValue())
                    .toArray();
            
            double[] asset2Prices = asset2Data.stream()
                    .mapToDouble(data -> data.getClose().doubleValue())
                    .toArray();
            
            // 更新價格數據
            PairsTradingServiceValueBean result = updatePriceData(asset1Prices, asset2Prices, asset1Data);
            if (result != null) {
                log.info("平穩性檢定: {}", result.isStationaryTest() ? "通過" : "未通過");
            }
            return result;
        } catch (Exception e) {
            log.error("獲取市場數據失敗", e);
        }
        return null;
    }

    @Override
    public PairsTradingServiceValueBean updatePriceData(double[] asset1Prices, double[] asset2Prices, List<CandlestickData> asset1Data) {
        PairsTradingServiceValueBean updatePriceData = new PairsTradingServiceValueBean();
        TradingConfigBean tradingConfigBean = loadConfig();
        // 1. 資料與窗口檢查
        if (asset1Prices == null || asset2Prices == null) {
            log.warn("價格陣列不可為 null"); return null;
        }
        if (tradingConfigBean.getWindowSize() < 2) {
            log.warn("windowSize 必須 >= 2，目前 = {}", tradingConfigBean.getWindowSize()); return null;
        }
        if (asset1Prices.length < tradingConfigBean.getWindowSize() || asset2Prices.length < tradingConfigBean.getWindowSize()) {
            log.warn("價格數據不足：窗口大小 = {}, 資產1 點數 = {}, 資產2 點數 = {}",
                    tradingConfigBean.getWindowSize(), asset1Prices.length, asset2Prices.length);
            return null;
        }

        int len = tradingConfigBean.getWindowSize();
        double[] window1 = Arrays.copyOfRange(asset1Prices, asset1Prices.length - len, asset1Prices.length);
        double[] window2 = Arrays.copyOfRange(asset2Prices, asset2Prices.length - len, asset2Prices.length);

        // 2. 使用統計工具類計算相關性
        updatePriceData.setCorrelation(StatisticalUtils.pineCorrelation(window1, window2));
        log.info("TradingView 價格相關性: {}", updatePriceData.getCorrelation());

        // 3. 使用統計工具類計算對稱迴歸（Orthogonal Regression）
        OrthogonalRegressionResult regressionResult = StatisticalUtils.computeOrthogonalRegression(window2, window1);
        
        // 更新回歸參數
        updatePriceData.setAlpha(regressionResult.getAlpha());
        updatePriceData.setBeta(regressionResult.getBeta());
        log.debug("Orthogonal 回歸: α = {}, β = {}", updatePriceData.getAlpha(), updatePriceData.getBeta());

        // 4. 獲取殘差序列並更新價差歷史
        updatePriceData.getSpreadHistory().clear();
        double[] residuals = regressionResult.getResiduals();
        for (double residual : residuals) {
            updatePriceData.getSpreadHistory().add(residual);
        }

        // 5. ADF 平穩性檢定
        boolean isStationary = StatisticalUtils.performADFTest(updatePriceData.getSpreadHistory());
        updatePriceData.setStationaryTest(isStationary);
        if (!isStationary) {
            log.warn("殘差序列未通過 ADF 平穩性檢定，但將繼續提供數據並標記檢定結果");
        }

        // 6. 獲取統計量
        updatePriceData.setSpreadMean(regressionResult.getMean());
        updatePriceData.setSpreadStd(regressionResult.getStd());
        
        // 7. 確保標準差不為零（保護措施）
        double minStd = updatePriceData.getSpreadStd() * 0.01;
        if (updatePriceData.getSpreadStd() < minStd) {
            log.warn("正交殘差標準差 {} 太小，調整為 {}", updatePriceData.getSpreadStd(), minStd);
            updatePriceData.setSpreadStd(minStd);;
        }

        // 8. 計算 Z 分數
        updatePriceData.getZScoreHistory().clear();
        for (double s : updatePriceData.getSpreadHistory()) {
            updatePriceData.getZScoreHistory().add((s - updatePriceData.getSpreadMean()) / updatePriceData.getSpreadStd());
        }

        // 9. 更新 lastZScore、spread
        updatePriceData.setLastZScore(updatePriceData.getZScoreHistory().get(len - 1));
        updatePriceData.setSpread(updatePriceData.getSpreadHistory().get(len - 1));

        // 10. 時間對齊
        updatePriceData.getTimeHistory().clear();
        if (asset1Data != null && asset1Data.size() >= len) {
            int offset = asset1Data.size() - len;
            for (int i = 0; i < len; i++) {
                updatePriceData.getTimeHistory().add(asset1Data.get(offset + i).getCloseTime());
            }
        } else {
            long now = System.currentTimeMillis();
            long step = 15 * 60 * 1000L;
            for (int i = 0; i < len; i++){
                updatePriceData.getTimeHistory().add(now - (len - i - 1) * step);
            }
        }

        // 最後日誌
        log.info("最新殘差 = {}，Z-score = {}，均值 = {}，標準差 = {}",
                updatePriceData.getSpread(), updatePriceData.getLastZScore(), updatePriceData.getSpreadMean(), updatePriceData.getSpreadStd());
        return updatePriceData;
    }

    @Override
    public List<Double> getZScoreHistory(String asset1, String asset2) {
        PairsTradingServiceValueBean pairsTradingServiceValueBean = fetchMarketData(asset1, asset2);
        if (pairsTradingServiceValueBean == null) {
            log.warn("無法獲取市場數據,返回空的Z分數歷史");
            return new ArrayList<>();
        }
        return new ArrayList<>(pairsTradingServiceValueBean.getZScoreHistory());
    }
    
    @Override
    public List<Double> getSpreadHistory(String asset1, String asset2) {
        PairsTradingServiceValueBean pairsTradingServiceValueBean = fetchMarketData(asset1, asset2);
        if (pairsTradingServiceValueBean == null) {
            log.warn("無法獲取市場數據,返回空的價差歷史");
            return new ArrayList<>();
        }
        return new ArrayList<>(pairsTradingServiceValueBean.getSpreadHistory());
    }
    
    @Override
    public List<Long> getTimeHistory(String asset1, String asset2) {
        PairsTradingServiceValueBean pairsTradingServiceValueBean = fetchMarketData(asset1, asset2);
        if (pairsTradingServiceValueBean == null) {
            log.warn("無法獲取市場數據,返回空的時間歷史");
            return new ArrayList<>();
        }
        return new ArrayList<>(pairsTradingServiceValueBean.getTimeHistory());
    }
    
    @Override
    public List<String> getAvailableTradingPairs() {
        try {
            return binanceApiService.getAvailableFuturesPairs();
        } catch (Exception e) {
            log.error("獲取可用交易對列表失敗", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public String getZScoreChart(String asset1, String asset2) {
        // 加載最新配置
        TradingConfigBean tradingConfigBean = loadConfig();

        PairsTradingServiceValueBean pairsTradingServiceValueBean = fetchMarketData(asset1, asset2);
        
        if (pairsTradingServiceValueBean == null) {
            return "無法獲取市場數據,無法生成圖表";
        }
        
        if (pairsTradingServiceValueBean.getZScoreHistory().size() < tradingConfigBean.getWindowSize()) {
            return "歷史Z分數數據不足，無法生成圖表";
        }
        
        if (pairsTradingServiceValueBean.getLastZScoreChart().isEmpty()) {
            pairsTradingServiceValueBean.setLastZScoreChart(ConsoleChartUtil.drawZScoreChart(pairsTradingServiceValueBean.getZScoreHistory(), tradingConfigBean.getEntryThreshold(), tradingConfigBean.getExitThreshold()));
        }
        
        // 添加平穩性檢定結果到圖表描述
        String stationaryStatus = pairsTradingServiceValueBean.isStationaryTest() ? "通過" : "未通過";
        if (!pairsTradingServiceValueBean.getLastZScoreChart().isEmpty()) {
            return pairsTradingServiceValueBean.getLastZScoreChart() + "\n平穩性檢定: " + stationaryStatus;
        }
        
        return pairsTradingServiceValueBean.getLastZScoreChart();
    }
}
