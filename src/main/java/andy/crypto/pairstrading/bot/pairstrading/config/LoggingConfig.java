package andy.crypto.pairstrading.bot.pairstrading.config;

import andy.crypto.pairstrading.bot.pairstrading.service.LogService;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import jakarta.annotation.PostConstruct;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * 日誌配置
 * 用於捕獲系統日誌並將其發送到自定義的日誌服務
 */
@Configuration
public class LoggingConfig {

    @Autowired
    private LogService logService;
    
    @PostConstruct
    public void init() {
        // 獲取 Logback Logger 上下文
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        // 創建自定義的日誌攔截器
        CustomLogAppender appender = new CustomLogAppender();
        appender.setContext(loggerContext);
        appender.setLogService(logService);
        appender.start();
        
        // 將攔截器添加到根日誌記錄器
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(appender);
        
        // 也添加到我們專案的Logger
        Logger projectLogger = loggerContext.getLogger("andy.crypto.pairstrading");
        projectLogger.addAppender(appender);
    }
    
    /**
     * 自定義日誌攔截器
     * 用於捕獲日誌事件並將其轉發到我們的日誌服務
     */
    private static class CustomLogAppender extends AppenderBase<ILoggingEvent> {
        
        private LogService logService;
        
        public void setLogService(LogService logService) {
            this.logService = logService;
        }
        
        @Override
        protected void append(ILoggingEvent event) {
            if (logService != null) {
                // 將日誌事件發送到我們的日誌服務
                String level = event.getLevel().toString();
                String message = event.getFormattedMessage();
                
                // 僅處理INFO級別及以上的日誌
                if (event.getLevel().isGreaterOrEqual(Level.INFO)) {
                    logService.addLog(message, level);
                }
            }
        }
    }
}
