package andy.crypto.pairstrading.bot.pairstrading.service.impl;

import andy.crypto.pairstrading.bot.pairstrading.model.PositionHistory;
import andy.crypto.pairstrading.bot.pairstrading.model.PositionInfo;
import andy.crypto.pairstrading.bot.pairstrading.repository.PositionHistoryRepository;
import andy.crypto.pairstrading.bot.pairstrading.service.PositionHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 倉位歷史服務實現類 - 使用H2數據庫持久化
 */
@Slf4j
@Service
public class PositionHistoryServiceImpl implements PositionHistoryService {

    private final PositionHistoryRepository positionHistoryRepository;
    
    @Autowired
    public PositionHistoryServiceImpl(PositionHistoryRepository positionHistoryRepository) {
        this.positionHistoryRepository = positionHistoryRepository;
        log.info("初始化倉位歷史服務，使用H2數據庫存儲");
    }
    
    @Override
    @Transactional
    public void recordOpenPosition(PositionInfo position, String reason, Double zScore) {
        log.info("記錄開倉操作: {} {}", position.getSymbol(), position.getPositionSide());
        
        // 防止空值
        String symbol = position.getSymbol() != null ? position.getSymbol() : "UNKNOWN";
        String positionSide = position.getPositionSide() != null ? position.getPositionSide() : "BOTH";
        BigDecimal entryPrice = position.getEntryPrice() != null ? position.getEntryPrice() : BigDecimal.ZERO;
        BigDecimal positionAmt = position.getPositionAmt() != null ? position.getPositionAmt() : BigDecimal.ZERO;
        String safeReason = reason != null ? reason : "系統自動開倉";
        
        PositionHistory history = PositionHistory.builder()
                .symbol(symbol)
                .positionSide(positionSide)
                .entryPrice(entryPrice)
                .positionAmt(positionAmt)
                .action("OPEN")
                .timestamp(System.currentTimeMillis())
                .reason(safeReason)
                .zScore(zScore)  // zScore可以是null
                .build();
        
        positionHistoryRepository.save(history);
        log.debug("已記錄開倉操作並保存到數據庫");
    }

    @Override
    @Transactional
    public void recordClosePosition(PositionInfo position, String reason, Double zScore) {
        log.info("記錄平倉操作: {} {}", position.getSymbol(), position.getPositionSide());
        
        // 防止空值
        String symbol = position.getSymbol() != null ? position.getSymbol() : "UNKNOWN";
        String positionSide = position.getPositionSide() != null ? position.getPositionSide() : "BOTH";
        BigDecimal entryPrice = position.getEntryPrice() != null ? position.getEntryPrice() : BigDecimal.ZERO;
        BigDecimal markPrice = position.getMarkPrice() != null ? position.getMarkPrice() : BigDecimal.ZERO; // 使用當前標記價格作為退出價格
        BigDecimal positionAmt = position.getPositionAmt() != null ? position.getPositionAmt() : BigDecimal.ZERO;
        String safeReason = reason != null ? reason : "系統自動平倉";
        
        PositionHistory history = PositionHistory.builder()
                .symbol(symbol)
                .positionSide(positionSide)
                .entryPrice(entryPrice)
                .exitPrice(markPrice) // 設置退出價格
                .positionAmt(positionAmt)
                .action("CLOSE")
                .timestamp(System.currentTimeMillis())
                .reason(safeReason)
                .zScore(zScore)  // zScore可以是null
                .build();
        
        // 計算並設置平倉損益
        history.calculateAndSetProfit(markPrice);
        
        // 輸出調試信息
        log.debug("平倉損益計算: 交易對={}, 入場價={}, 平倉價={}, 倉位數量={}, 損益={}", 
                symbol, entryPrice, markPrice, positionAmt, history.getRealizedProfit());
        
        positionHistoryRepository.save(history);
        log.debug("已記錄平倉操作並保存到數據庫，損益: {}", history.getRealizedProfit());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PositionHistory> getPositionHistory(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            // 返回所有歷史記錄，按時間降序排序
            log.debug("查詢所有倉位歷史記錄");
            return positionHistoryRepository.findAllByOrderByTimestampDesc();
        } else {
            // 過濾並返回指定交易對的歷史記錄，按時間降序排序
            log.debug("查詢交易對 {} 的倉位歷史記錄", symbol);
            return positionHistoryRepository.findBySymbolOrderByTimestampDesc(symbol);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PositionHistory> getRecentPositionHistory(String symbol, int limit) {
        if (symbol == null || symbol.isEmpty()) {
            // 返回最近的n條記錄
            log.debug("查詢最近 {} 條倉位歷史記錄", limit);
            return positionHistoryRepository.findRecentPositionHistory(limit);
        } else {
            // 返回指定交易對的最近n條記錄
            log.debug("查詢交易對 {} 的最近 {} 條倉位歷史記錄", symbol, limit);
            return positionHistoryRepository.findRecentBySymbol(symbol, limit);
        }
    }

    @Override
    @Transactional
    public void clearHistory() {
        log.info("清空倉位歷史記錄數據庫");
        positionHistoryRepository.deleteAll();
    }
}
