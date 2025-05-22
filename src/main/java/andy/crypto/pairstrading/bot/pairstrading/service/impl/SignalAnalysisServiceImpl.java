package andy.crypto.pairstrading.bot.pairstrading.service.impl;

import andy.crypto.pairstrading.bot.bean.PairsTradingServiceValueBean;
import andy.crypto.pairstrading.bot.bean.TradingConfigBean;
import andy.crypto.pairstrading.bot.pairstrading.model.PositionInfo;
import andy.crypto.pairstrading.bot.pairstrading.service.BinanceApiService;
import andy.crypto.pairstrading.bot.pairstrading.service.MarketDataService;
import andy.crypto.pairstrading.bot.pairstrading.service.SignalAnalysisService;
import andy.crypto.pairstrading.bot.pairstrading.util.ConsoleChartUtil;
import andy.crypto.pairstrading.bot.service.TradingConfigService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 信號分析服務實現類
 */
@Slf4j
@Service
public class SignalAnalysisServiceImpl implements SignalAnalysisService {

    @Autowired
    private BinanceApiService binanceApiService;
    
    @Autowired
    private MarketDataService marketDataService;
    
    @Autowired
    @Lazy
    private TradingConfigService tradingConfigService;
    
    /**
     * 從配置服務加載配置
     */
    private TradingConfigBean loadConfig() {
        return tradingConfigService.getTradingConfigBean();
    }

    @Override
    public void analyzeCorrelation(PairsTradingServiceValueBean pairsTradingServiceValueBean) {
        // 加載最新配置
        TradingConfigBean tradingConfigBean = loadConfig();
        
        log.info("分析 {} 和 {} 的相關性", tradingConfigBean.getAsset1(), tradingConfigBean.getAsset2());
        
        try {
            if (pairsTradingServiceValueBean.getSpreadHistory().size() < tradingConfigBean.getWindowSize()) {
                log.warn("歷史價差數據不足，無法分析相關性");
                return;
            }
            
            // 計算相關性並更新相關數據
            log.info("資產相關性: {}", pairsTradingServiceValueBean.getCorrelation());
            log.info("回歸模型: Y = {} + {} * X", pairsTradingServiceValueBean.getAlpha(), pairsTradingServiceValueBean.getBeta());
            log.info("回歸係數 (Beta): {}", pairsTradingServiceValueBean.getBeta());
            log.info("回歸截距 (Alpha): {}", pairsTradingServiceValueBean.getAlpha());
            log.info("價差平均值: {}", pairsTradingServiceValueBean.getSpreadMean());
            log.info("價差標準差: {}", pairsTradingServiceValueBean.getSpreadStd());
            log.info("當前價差: {}", pairsTradingServiceValueBean.getSpread());
            log.info("當前Z分數: {}", pairsTradingServiceValueBean.getLastZScore());
            
            // 繪製Z分數圖表
            if (pairsTradingServiceValueBean.getZScoreHistory().size() >= tradingConfigBean.getWindowSize()) {
                pairsTradingServiceValueBean.setLastZScoreChart(ConsoleChartUtil.drawZScoreChart(pairsTradingServiceValueBean.getZScoreHistory(), tradingConfigBean.getEntryThreshold(), tradingConfigBean.getExitThreshold()));
                if (tradingConfigBean.isConsoleChartEnabled()) {
                    log.info("\nZ分數圖表:\n{}", pairsTradingServiceValueBean.getLastZScoreChart());
                }
            }
            
            // 繪製價差圖表
            if (pairsTradingServiceValueBean.getSpreadHistory().size() >= tradingConfigBean.getWindowSize()) {
                pairsTradingServiceValueBean.setLastSpreadChart(ConsoleChartUtil.drawSpreadChart(pairsTradingServiceValueBean.getSpreadHistory(), pairsTradingServiceValueBean.getSpreadMean(), pairsTradingServiceValueBean.getSpreadStd()));
                if (tradingConfigBean.isConsoleChartEnabled()) {
                    log.info("\n價差圖表:\n{}", pairsTradingServiceValueBean.getLastSpreadChart());
                }
            }
        } catch (Exception e) {
            log.error("分析相關性失敗", e);
        }
    }

    @Override
    public boolean calculateSignal(PairsTradingServiceValueBean pairsTradingServiceValueBean) {
        // 加載最新配置
        TradingConfigBean tradingConfigBean = loadConfig();
        
        log.info("計算交易信號");
        
        try {
            if (pairsTradingServiceValueBean.getSpreadHistory().size() < tradingConfigBean.getWindowSize()) {
                log.warn("歷史數據不足，無法計算交易信號");
                return false;
            }
            
            // 檢查相關性是否足夠強
            if (Math.abs(pairsTradingServiceValueBean.getCorrelation()) < 0.7) {
                log.info("資產相關性不足 ({}), 放棄交易", pairsTradingServiceValueBean.getCorrelation());
                return false;
            }
            
            // 檢查是否已有倉位
            List<PositionInfo> positions = binanceApiService.getPositionInfo(null);
            boolean hasPositionAsset1 = positions.stream().anyMatch(p -> p.getSymbol().equals(tradingConfigBean.getAsset1()));
            boolean hasPositionAsset2 = positions.stream().anyMatch(p -> p.getSymbol().equals(tradingConfigBean.getAsset2()));
            boolean hasPositions = hasPositionAsset1 || hasPositionAsset2;
            
            Map<String, Boolean> longPositions = new HashMap<>();
            Map<String, Boolean> shortPositions = new HashMap<>();
            
            for (PositionInfo position : positions) {
                if (position.isLongPosition()) {
                    longPositions.put(position.getSymbol(), true);
                } else if (position.isShortPosition()) {
                    shortPositions.put(position.getSymbol(), true);
                }
            }
            
            // 生成入場信號報告 (使用包含alpha和beta參數的新版方法)
            pairsTradingServiceValueBean.setLastSignalReport(ConsoleChartUtil.generateSignalReport(
                    pairsTradingServiceValueBean.getLastZScore(), tradingConfigBean.getEntryThreshold(), tradingConfigBean.getExitThreshold(),
                    tradingConfigBean.getAsset1(), tradingConfigBean.getAsset2(), pairsTradingServiceValueBean.getCorrelation(), hasPositions, pairsTradingServiceValueBean.getAlpha(), pairsTradingServiceValueBean.getBeta()));
            if (tradingConfigBean.isConsoleSignalEnabled()) {
                log.info("\n入場信號報告:\n{}", pairsTradingServiceValueBean.getLastSignalReport());
            }
            
            // 在控制台中輸出一個明顯的判斷結果
            if (tradingConfigBean.isConsoleSignalEnabled()) {
                if (pairsTradingServiceValueBean.getLastZScore() > tradingConfigBean.getEntryThreshold()) {
                    log.info("\n☛ ☛ ☛ 做空 {} 做多 {} ☚ ☚ ☚", tradingConfigBean.getAsset1(), tradingConfigBean.getAsset2());
                } else if (pairsTradingServiceValueBean.getLastZScore() < -tradingConfigBean.getEntryThreshold()) {
                    log.info("\n☛ ☛ ☛ 做多 {} 做空 {} ☚ ☚ ☚", tradingConfigBean.getAsset1(), tradingConfigBean.getAsset2());
                } else if (Math.abs(pairsTradingServiceValueBean.getLastZScore()) < tradingConfigBean.getExitThreshold() && hasPositions) {
                    log.info("\n☛ ☛ ☛ 平倉所有倉位 ☚ ☚ ☚");
                } else {
                    log.info("\n☛ ☛ ☛ 沒有交易信號 - 持倉觀望 ☚ ☚ ☚");
                }
            }
            
            // 交易信號邏輯
            // 1. 如果Z分數大於入場閾值，做空資產1，做多資產2
            // 2. 如果Z分數小於負入場閾值，做多資產1，做空資產2
            // 3. 如果Z分數在出場閾值內，平倉所有倉位
            
            if (Math.abs(pairsTradingServiceValueBean.getLastZScore()) < tradingConfigBean.getExitThreshold()) {
                // 出場信號
                if (hasPositionAsset1 || hasPositionAsset2) {
                    log.info("Z分數 ({}) 在出場閾值 ({}) 內，平倉所有倉位", pairsTradingServiceValueBean.getLastZScore(), tradingConfigBean.getExitThreshold());
                    return true;
                }
            } else if (pairsTradingServiceValueBean.getLastZScore() > tradingConfigBean.getEntryThreshold()) {
                // 資產1價格相對高估，做空資產1，做多資產2
                if (!shortPositions.getOrDefault(tradingConfigBean.getAsset1(), false) && !longPositions.getOrDefault(tradingConfigBean.getAsset2(), false)) {
                    log.info("Z分數 ({}) 大於入場閾值 ({}), 做空 {} 做多 {}", pairsTradingServiceValueBean.getLastZScore(), tradingConfigBean.getEntryThreshold(), tradingConfigBean.getAsset1(), tradingConfigBean.getAsset2());
                    return true;
                }
            } else if (pairsTradingServiceValueBean.getLastZScore() < -tradingConfigBean.getEntryThreshold()) {
                // 資產1價格相對低估，做多資產1，做空資產2
                if (!longPositions.getOrDefault(tradingConfigBean.getAsset1(), false) && !shortPositions.getOrDefault(tradingConfigBean.getAsset2(), false)) {
                    log.info("Z分數 ({}) 小於負入場閾值 ({}), 做多 {} 做空 {}", pairsTradingServiceValueBean.getLastZScore(), -tradingConfigBean.getEntryThreshold(), tradingConfigBean.getAsset1(), tradingConfigBean.getAsset2());
                    return true;
                }
            }
            
            log.info("沒有新的交易信號");
            return false;
        } catch (Exception e) {
            log.error("計算交易信號失敗", e);
            return false;
        }
    }

    @Override
    public String getSignalReport() {
        // 加載最新配置
        TradingConfigBean tradingConfigBean = loadConfig();
        PairsTradingServiceValueBean pairsTradingServiceValueBean = marketDataService.fetchMarketData();
        if (pairsTradingServiceValueBean.getZScoreHistory().size() < tradingConfigBean.getWindowSize()) {
            return "歷史數據不足，無法生成信號報告";
        }
        
        // 檢查是否已有倉位
        List<PositionInfo> positions;
        boolean hasPositions = false;
        
        try {
            positions = binanceApiService.getPositionInfo(null);
            hasPositions = positions.stream().anyMatch(p -> p.getSymbol().equals(tradingConfigBean.getAsset1()) || p.getSymbol().equals(tradingConfigBean.getAsset2()));
        } catch (Exception e) {
            log.error("獲取倉位信息失敗", e);
        }
        
        if (pairsTradingServiceValueBean.getLastSignalReport().isEmpty()) {
            pairsTradingServiceValueBean.setLastSignalReport(ConsoleChartUtil.generateSignalReport(
                    pairsTradingServiceValueBean.getLastZScore(), tradingConfigBean.getEntryThreshold(), tradingConfigBean.getExitThreshold(),
                    tradingConfigBean.getAsset1(), tradingConfigBean.getAsset2(), pairsTradingServiceValueBean.getCorrelation(), hasPositions, pairsTradingServiceValueBean.getAlpha(), pairsTradingServiceValueBean.getBeta()));
        }
        
        return pairsTradingServiceValueBean.getLastSignalReport();
    }
    
    @Override
    public boolean setWindowSize(int newWindowSize) {
        // 驗證窗口大小合理性（至少10，最多1000）
        if (newWindowSize < 10 || newWindowSize > 1000) {
            log.warn("嘗試設置不合理的窗口大小: {}, 有效範圍: 10-1000", newWindowSize);
            return false;
        }
        
        // 加載最新配置
        TradingConfigBean tradingConfigBean = loadConfig();
        
        log.info("更新窗口大小: {} -> {}", tradingConfigBean.getWindowSize(), newWindowSize);
        
        // 更新窗口大小到配置資料庫
        tradingConfigService.saveOrUpdateConfig(
                TradingConfigService.WINDOW_SIZE,
                String.valueOf(newWindowSize),
                "用於計算的歷史數據點數量",
                TradingConfigService.CATEGORY_WINDOW);
        
        try {
            return true;
        } catch (Exception e) {
            log.error("設置窗口大小後更新數據失敗", e);
            return false;
        }
    }

    @Override
    public int getWindowSize() {
        TradingConfigBean tradingConfigBean = loadConfig();
        return tradingConfigBean.getWindowSize();
    }
}
