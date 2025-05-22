package andy.crypto.pairstrading.bot.pairstrading.service.impl;

import andy.crypto.pairstrading.bot.bean.PairsTradingServiceValueBean;
import andy.crypto.pairstrading.bot.bean.TradingConfigBean;
import andy.crypto.pairstrading.bot.pairstrading.model.OrderResponse;
import andy.crypto.pairstrading.bot.pairstrading.model.PositionInfo;
import andy.crypto.pairstrading.bot.pairstrading.service.BinanceApiService;
import andy.crypto.pairstrading.bot.pairstrading.service.MarketDataService;
import andy.crypto.pairstrading.bot.pairstrading.service.PositionHistoryService;
import andy.crypto.pairstrading.bot.pairstrading.service.TradingExecutionService;
import andy.crypto.pairstrading.bot.service.TradingConfigService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 交易執行服務實現類
 */
@Slf4j
@Service
public class TradingExecutionServiceImpl implements TradingExecutionService {

    @Autowired
    private BinanceApiService binanceApiService;
    
    @Autowired
    private MarketDataService marketDataService;
    
    @Autowired
    private PositionHistoryService positionHistoryService;
    
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
    public void executeTrade(boolean isBuy) {
        // 加載最新配置
        TradingConfigBean tradingConfigBean = loadConfig();

        if (!tradingConfigBean.isTradingEnabled()) {
            log.info("交易功能已禁用，模擬交易模式");
            return;
        }
        
        log.info("執行交易操作");
        PairsTradingServiceValueBean pairsTradingServiceValueBean = marketDataService.fetchMarketData();
        
        try {
            // 檢查是否有倉位
            java.util.List<PositionInfo> positions = binanceApiService.getPositionInfo(null);
            boolean hasPositions = positions.stream().anyMatch(p -> p.getSymbol().equals(tradingConfigBean.getAsset1()) || p.getSymbol().equals(tradingConfigBean.getAsset2()));
            
            // 根據Z分數決定交易方向
            if (Math.abs(pairsTradingServiceValueBean.getLastZScore()) < tradingConfigBean.getExitThreshold() && hasPositions) {
                // 平倉所有倉位前先記錄持倉情況
                for (PositionInfo position : positions) {
                    positionHistoryService.recordClosePosition(
                            position, 
                            "Z分數(" + String.format("%.2f", pairsTradingServiceValueBean.getLastZScore()) + ")在出場閾值內",
                            pairsTradingServiceValueBean.getLastZScore()
                    );
                }
                
                // 平倉所有倉位
                log.info("平倉所有倉位");
                binanceApiService.closeAllPositions(tradingConfigBean.getAsset1());
                binanceApiService.closeAllPositions(tradingConfigBean.getAsset2());
            } else if (pairsTradingServiceValueBean.getLastZScore() > tradingConfigBean.getEntryThreshold()) {
                // 做空資產1，做多資產2
                log.info("執行配對交易: 做空 {} 做多 {}", tradingConfigBean.getAsset1(), tradingConfigBean.getAsset2());
                
                // 執行做空資產1，做多資產2的配對交易
                executePairedPositions(tradingConfigBean.getAsset1(), tradingConfigBean.getAsset2(), false, true,
                        "Z分數(" + String.format("%.2f", pairsTradingServiceValueBean.getLastZScore()) + ")超過入場閾值");
                
            } else if (pairsTradingServiceValueBean.getLastZScore() < -tradingConfigBean.getEntryThreshold()) {
                // 做多資產1，做空資產2
                log.info("執行配對交易: 做多 {} 做空 {}", tradingConfigBean.getAsset1(), tradingConfigBean.getAsset2());
                
                // 執行做多資產1，做空資產2的配對交易
                executePairedPositions(tradingConfigBean.getAsset1(), tradingConfigBean.getAsset2(), true, false,
                        "Z分數(" + String.format("%.2f", pairsTradingServiceValueBean.getLastZScore()) + ")低於負入場閾值");
            }
        } catch (Exception e) {
            log.error("執行交易失敗", e);
        }
    }
    
    @Override
    public void executePairedPositions(String asset1, String asset2, boolean isLongAsset1, boolean isLongAsset2, String reason) {
        try {
            PairsTradingServiceValueBean pairsTradingServiceValueBean = marketDataService.fetchMarketData(asset1, asset2);

            // 計算交易數量
            BigDecimal asset1Quantity = calculateQuantity(asset1, pairsTradingServiceValueBean);
            BigDecimal asset2Quantity = calculateQuantity(asset2, pairsTradingServiceValueBean);
            
            // 處理資產1交易
            String asset1Side = isLongAsset1 ? "BUY" : "SELL";
            OrderResponse asset1Order = binanceApiService.placeOrder(
                    asset1, asset1Side, "BOTH", "MARKET", asset1Quantity, null);
            log.info("{} {} 訂單ID: {}, 數量: {}", isLongAsset1 ? "做多" : "做空", asset1, 
                    asset1Order.getOrderId(), asset1Quantity);
            
            // 記錄資產1倉位
            PositionInfo asset1Position = PositionInfo.builder()
                    .symbol(asset1)
                    .positionSide("BOTH")
                    .entryPrice(asset1Order.getAvgPrice() != null && asset1Order.getAvgPrice().compareTo(BigDecimal.ZERO) > 0 ?
                            asset1Order.getAvgPrice() : asset1Order.getPrice())
                    .positionAmt(isLongAsset1 ? asset1Order.getOrigQty() : asset1Order.getOrigQty().negate())
                    .updateTime(System.currentTimeMillis())
                    .build();
            positionHistoryService.recordOpenPosition(asset1Position, reason, pairsTradingServiceValueBean.getLastZScore());
            
            // 處理資產2交易
            String asset2Side = isLongAsset2 ? "BUY" : "SELL";
            OrderResponse asset2Order = binanceApiService.placeOrder(
                    asset2, asset2Side, "BOTH", "MARKET", asset2Quantity, null);
            log.info("{} {} 訂單ID: {}, 數量: {}", isLongAsset2 ? "做多" : "做空", asset2, 
                    asset2Order.getOrderId(), asset2Quantity);
            
            // 記錄資產2倉位
            PositionInfo asset2Position = PositionInfo.builder()
                    .symbol(asset2)
                    .positionSide("BOTH")
                    .entryPrice(asset2Order.getAvgPrice() != null && asset2Order.getAvgPrice().compareTo(BigDecimal.ZERO) > 0 ?
                            asset2Order.getAvgPrice() : asset2Order.getPrice())
                    .positionAmt(isLongAsset2 ? asset2Order.getOrigQty() : asset2Order.getOrigQty().negate())
                    .updateTime(System.currentTimeMillis())
                    .build();
            positionHistoryService.recordOpenPosition(asset2Position, reason, pairsTradingServiceValueBean.getLastZScore());
            
            // 記錄訂單信息
            log.info("{}訂單: {}", isLongAsset1 ? "做多" : "做空", asset1Order);
            log.info("{}訂單: {}", isLongAsset2 ? "做多" : "做空", asset2Order);
        } catch (Exception e) {
            log.error("執行配對交易失敗: {} 和 {}", asset1, asset2, e);
        }
    }
    
    @Override
    public BigDecimal calculateQuantity(String symbol, PairsTradingServiceValueBean pairsTradingServiceValueBean) {
        // 加載最新配置
        TradingConfigBean tradingConfigBean = loadConfig();
        try {
            // 動態調整倉位大小系數
            // 1. 根據相關性強度 (相關性越高，倉位越大)
            // 2. 根據Z分數偏離入場閾值的程度 (偏離越大，倉位越大)
            // 3. 限制最大倉位不超過配置倉位大小
            double corrFactor = Math.min(1.0, Math.abs(pairsTradingServiceValueBean.getCorrelation()));
            
            // Z分數強度係數: 根據Z分數與入場閾值的比例計算 (最低0.5，最高1.0)
            double zScoreFactor = 0.5 + Math.min(0.5, Math.abs(pairsTradingServiceValueBean.getLastZScore()) / tradingConfigBean.getEntryThreshold() * 0.5);
            
            // 組合系數，至少保留50%倉位大小
            double positionSizeFactor = 0.5 + (corrFactor * zScoreFactor * 0.5);
            
            // 調整後的倉位大小
            BigDecimal adjustedPositionSize = tradingConfigBean.getPositionSize().multiply(new BigDecimal(positionSizeFactor));
            
            log.info("倉位動態調整: 相關性({}), Z分數強度({}), 調整係數({}), 原始倉位({}), 調整後倉位({})",
                    String.format("%.2f", pairsTradingServiceValueBean.getCorrelation()),
                    String.format("%.2f", Math.abs(pairsTradingServiceValueBean.getLastZScore()) / tradingConfigBean.getEntryThreshold()),
                    String.format("%.2f", positionSizeFactor),
                    tradingConfigBean.getPositionSize(),
                    adjustedPositionSize);
            
            if (tradingConfigBean.isAmountBased()) {
                // 基於金額計價，需要獲取當前價格並轉換為數量
                BigDecimal currentPrice = binanceApiService.getLatestPrice(symbol);
                if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    log.error("獲取 {} 價格失敗或價格為零", symbol);
                    return adjustedPositionSize; // 如果價格獲取失敗，直接使用調整後的倉位大小
                }
                
                // 計算數量 = 交易金額 / 當前價格
                BigDecimal quantity = adjustedPositionSize.divide(currentPrice, 8, RoundingMode.DOWN);
                
                // 調整數量精度
                quantity = binanceApiService.adjustQuantityToPrecision(symbol, quantity);
                
                log.info("{} 交易金額: {} USDT, 當前價格: {}, 計算數量: {}", 
                       symbol, adjustedPositionSize, currentPrice, quantity);
                
                return quantity;
            } else {
                // 基於數量計價，使用調整後的數量
                return binanceApiService.adjustQuantityToPrecision(symbol, adjustedPositionSize);
            }
        } catch (Exception e) {
            log.error("計算交易數量失敗: {}", symbol, e);
            // 出錯時使用原始數值，但確保符合精度要求
            return binanceApiService.adjustQuantityToPrecision(symbol, tradingConfigBean.getPositionSize());
        }
    }
    
    @Override
    public void setLeverageForSymbols() {
        // 加載最新配置
        TradingConfigBean tradingConfigBean = loadConfig();
        
        if (!tradingConfigBean.isTradingEnabled()) {
            log.info("交易功能已禁用，跳過設定槓桿倍率");
            return;
        }

        try {
            // 設定第一個資產的槓桿倍率
            boolean success1 = binanceApiService.setLeverage(tradingConfigBean.getAsset1(), tradingConfigBean.getLeverage());
            if (success1) {
                log.info("成功設定 {} 槓桿倍率為 {}x", tradingConfigBean.getAsset1(), tradingConfigBean.getLeverage());
            } else {
                log.warn("設定 {} 槓桿倍率失敗", tradingConfigBean.getAsset1());
            }

            // 設定第二個資產的槓桿倍率
            boolean success2 = binanceApiService.setLeverage(tradingConfigBean.getAsset2(), tradingConfigBean.getLeverage());
            if (success2) {
                log.info("成功設定 {} 槓桿倍率為 {}x", tradingConfigBean.getAsset2(), tradingConfigBean.getLeverage());
            } else {
                log.warn("設定 {} 槓桿倍率失敗", tradingConfigBean.getAsset2());
            }
        } catch (Exception e) {
            log.error("設定槓桿倍率時發生錯誤", e);
        }
    }
}
