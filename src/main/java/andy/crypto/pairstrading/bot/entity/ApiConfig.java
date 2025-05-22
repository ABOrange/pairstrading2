package andy.crypto.pairstrading.bot.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用於存儲API配置的實體類
 */
@Entity
@Table(name = "api_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiConfig {

    @Id
    private String configKey;
    
    private String configValue;
    
    private String description;
}
