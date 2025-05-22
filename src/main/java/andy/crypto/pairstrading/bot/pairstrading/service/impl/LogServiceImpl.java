package andy.crypto.pairstrading.bot.pairstrading.service.impl;

import andy.crypto.pairstrading.bot.pairstrading.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 日誌服務實現類
 */
@Slf4j
@Service
public class LogServiceImpl implements LogService {

    private static final int MAX_LOG_SIZE = 1000; // 最大日誌數量
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // 使用線程安全的列表存儲日誌
    private final CopyOnWriteArrayList<LogEntry> systemLogs = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<LogEntry> tradeLogs = new CopyOnWriteArrayList<>();
    
    @Override
    public void addLog(String logMessage, String logLevel) {
        LogEntry logEntry = new LogEntry(
                LocalDateTime.now().format(DATE_FORMATTER),
                logLevel,
                logMessage
        );
        
        // 添加到系統日誌
        systemLogs.add(logEntry);
        
        // 如果是交易相關日誌，也添加到交易日誌
        if (logMessage.contains("交易") || 
            logMessage.contains("倉位") || 
            logMessage.contains("訂單") || 
            logMessage.contains("trade") || 
            logMessage.contains("position") || 
            logMessage.contains("order")) {
            tradeLogs.add(logEntry);
        }
        
        // 如果超過最大日誌數量，移除最舊的日誌
        while (systemLogs.size() > MAX_LOG_SIZE) {
            systemLogs.remove(0);
        }
        
        while (tradeLogs.size() > MAX_LOG_SIZE) {
            tradeLogs.remove(0);
        }
    }
    
    @Override
    public List<String> getLatestLogs(int limit) {
        return systemLogs.stream()
                .sorted((l1, l2) -> l2.timestamp.compareTo(l1.timestamp)) // 按時間降序排序
                .limit(limit)
                .map(LogEntry::toString)
                .collect(Collectors.toList());
    }
    
    @Override
    public void clearLogs() {
        systemLogs.clear();
        tradeLogs.clear();
    }
    
    @Override
    public List<String> getLatestTradeLogs(int limit) {
        return tradeLogs.stream()
                .sorted((l1, l2) -> l2.timestamp.compareTo(l1.timestamp)) // 按時間降序排序
                .limit(limit)
                .map(LogEntry::toString)
                .collect(Collectors.toList());
    }
    
    /**
     * 內部日誌條目類
     */
    private static class LogEntry {
        private final String timestamp;
        private final String level;
        private final String message;
        
        public LogEntry(String timestamp, String level, String message) {
            this.timestamp = timestamp;
            this.level = level;
            this.message = message;
        }
        
        @Override
        public String toString() {
            return "[" + timestamp + "] [" + level + "] " + message;
        }
    }
}
