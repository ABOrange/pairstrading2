package andy.crypto.pairstrading.bot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import andy.crypto.pairstrading.bot.entity.TradingConfig;

/**
 * 交易配置儲存庫介面
 */
@Repository
public interface TradingConfigRepository extends JpaRepository<TradingConfig, String> {
    
    /**
     * 根據配置鍵查找配置
     * 
     * @param configKey 配置鍵
     * @return 配置值
     */
    Optional<TradingConfig> findByConfigKey(String configKey);
    
    /**
     * 根據類別查找配置
     * 
     * @param category 配置類別
     * @return 配置列表
     */
    List<TradingConfig> findByCategory(String category);
    
    /**
     * 根據配置鍵前綴查找配置
     * 
     * @param keyPrefix 配置鍵前綴
     * @return 配置列表
     */
    List<TradingConfig> findByConfigKeyStartingWith(String keyPrefix);
    
    /**
     * 根據配置值查找配置
     * 
     * @param configValue 配置值
     * @return 配置列表
     */
    List<TradingConfig> findByConfigValue(String configValue);
}
