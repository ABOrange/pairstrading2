package andy.crypto.pairstrading.bot.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import andy.crypto.pairstrading.bot.entity.ApiConfig;

/**
 * API配置儲存庫介面
 */
@Repository
public interface ApiConfigRepository extends JpaRepository<ApiConfig, String> {
    
    /**
     * 根據配置鍵查找配置
     * 
     * @param configKey 配置鍵
     * @return 配置值
     */
    Optional<ApiConfig> findByConfigKey(String configKey);
}
