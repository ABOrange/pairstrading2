package andy.crypto.pairstrading.bot.pairstrading.service;

import andy.crypto.pairstrading.bot.pairstrading.model.PositionHistory;
import andy.crypto.pairstrading.bot.pairstrading.model.PositionInfo;
import java.util.List;

/**
 * 倉位歷史服務介面
 */
public interface PositionHistoryService {
    
    /**
     * 記錄開倉操作
     * @param position 倉位信息
     * @param reason 開倉原因
     * @param zScore 操作時的Z分數
     */
    void recordOpenPosition(PositionInfo position, String reason, Double zScore);
    
    /**
     * 記錄平倉操作
     * @param position 倉位信息
     * @param reason 平倉原因
     * @param zScore 操作時的Z分數
     */
    void recordClosePosition(PositionInfo position, String reason, Double zScore);
    
    /**
     * 獲取指定交易對的歷史倉位記錄
     * @param symbol 交易對符號，如果為null則返回所有記錄
     * @return 歷史倉位記錄列表
     */
    List<PositionHistory> getPositionHistory(String symbol);
    
    /**
     * 獲取指定交易對的最近 n 條歷史倉位記錄
     * @param symbol 交易對符號，如果為null則返回所有記錄
     * @param limit 限制返回的記錄數量
     * @return 歷史倉位記錄列表
     */
    List<PositionHistory> getRecentPositionHistory(String symbol, int limit);
    
    /**
     * 清空歷史記錄
     */
    void clearHistory();
}
