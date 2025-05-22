package andy.crypto.pairstrading.bot.pairstrading.service;

import java.util.List;

/**
 * 日誌服務介面
 * 用於管理和提供系統日誌數據
 */
public interface LogService {

    /**
     * 記錄新的系統日誌
     * 
     * @param logMessage 日誌訊息
     * @param logLevel 日誌等級
     */
    void addLog(String logMessage, String logLevel);
    
    /**
     * 取得最新的系統日誌
     * 
     * @param limit 最大數量限制
     * @return 日誌列表
     */
    List<String> getLatestLogs(int limit);
    
    /**
     * 清除所有日誌
     */
    void clearLogs();
    
    /**
     * 獲取最近的交易操作日誌
     * 
     * @param limit 最大數量限制
     * @return 交易操作日誌列表
     */
    List<String> getLatestTradeLogs(int limit);
}
