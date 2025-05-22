package andy.crypto.pairstrading.bot.pairstrading.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 數據庫配置類
 * 負責檢查H2數據庫是否存在，不存在則創建目錄和數據庫文件
 */
@Slf4j
@Configuration
public class DatabaseConfig {
    
    private final Environment env;
    private final DataSource dataSource;
    
    @Autowired
    public DatabaseConfig(Environment env, DataSource dataSource) {
        this.env = env;
        this.dataSource = dataSource;
        ensureDatabaseDirectoryExists();
        log.info("數據庫配置初始化完成");
    }
    
    /**
     * 確保數據庫目錄存在
     */
    private void ensureDatabaseDirectoryExists() {
        try {
            // 從數據庫URL中解析出路徑
            String dbUrl = env.getProperty("spring.datasource.url");
            if (dbUrl != null && dbUrl.contains("jdbc:h2:file:")) {
                // 提取文件路徑部分
                String filePath = dbUrl.replace("jdbc:h2:file:", "");
                // 移除任何額外的參數
                if (filePath.contains(";")) {
                    filePath = filePath.substring(0, filePath.indexOf(';'));
                }
                
                // 獲取目錄路徑
                File dbFile = new File(filePath);
                File dbDir = dbFile.getParentFile();
                
                // 創建目錄（如果不存在）
                if (dbDir != null && !dbDir.exists()) {
                    log.info("創建數據庫目錄: {}", dbDir.getAbsolutePath());
                    Files.createDirectories(Paths.get(dbDir.getAbsolutePath()));
                }
                
                log.info("數據庫將保存在: {}", dbFile.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("初始化數據庫目錄時發生錯誤", e);
        }
    }
}
