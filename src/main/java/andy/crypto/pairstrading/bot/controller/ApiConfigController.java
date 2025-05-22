package andy.crypto.pairstrading.bot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import andy.crypto.pairstrading.bot.entity.ApiConfig;
import andy.crypto.pairstrading.bot.pairstrading.config.BinanceConfig;
import andy.crypto.pairstrading.bot.repository.ApiConfigRepository;
import andy.crypto.pairstrading.bot.service.ApiConfigService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API配置控制器，用於管理API金鑰
 */
@RestController
@RequestMapping("/api/config")
@Slf4j
public class ApiConfigController {

    @Autowired
    private ApiConfigService apiConfigService;
    
    @Autowired
    private ApiConfigRepository apiConfigRepository;
    
    @Autowired
    private BinanceConfig binanceConfig;

    /**
     * 獲取API配置列表（不顯示完整的API金鑰和秘鑰）
     */
    @GetMapping("/list")
    public ResponseEntity<List<ApiConfig>> listConfigs() {
        List<ApiConfig> configs = apiConfigRepository.findAll();
        
        // 安全處理：不返回完整的API金鑰和秘鑰
        for (ApiConfig config : configs) {
            String value = config.getConfigValue();
            if (value != null && value.length() > 8) {
                config.setConfigValue(value.substring(0, 4) + "..." + value.substring(value.length() - 4));
            }
        }
        
        return ResponseEntity.ok(configs);
    }

    /**
     * 更新幣安API金鑰
     */
    @PostMapping("/update-binance-keys")
    public ResponseEntity<Map<String, String>> updateBinanceKeys(
            @RequestParam String apiKey,
            @RequestParam String secretKey) {
        
        // 保存到資料庫
        apiConfigService.saveOrUpdateConfig(ApiConfigService.BINANCE_API_KEY, apiKey, "幣安API金鑰");
        apiConfigService.saveOrUpdateConfig(ApiConfigService.BINANCE_SECRET_KEY, secretKey, "幣安API秘鑰");
        
        // 更新當前配置
        binanceConfig.setApiKey(apiKey);
        binanceConfig.setSecretKey(secretKey);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "幣安API金鑰已更新");
        
        log.info("幣安API金鑰已更新");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 獲取當前使用的API金鑰資訊（不顯示完整金鑰）
     */
    @GetMapping("/current-keys")
    public ResponseEntity<Map<String, String>> getCurrentKeys() {
        Map<String, String> response = new HashMap<>();
        
        // 獲取當前配置中的金鑰（僅顯示部分內容）
        String apiKey = binanceConfig.getApiKey();
        String secretKey = binanceConfig.getSecretKey();
        
        if (apiKey != null && apiKey.length() > 8) {
            response.put("apiKey", apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4));
        } else {
            response.put("apiKey", "未設置");
        }
        
        if (secretKey != null && secretKey.length() > 8) {
            response.put("secretKey", secretKey.substring(0, 4) + "..." + secretKey.substring(secretKey.length() - 4));
        } else {
            response.put("secretKey", "未設置");
        }
        
        response.put("baseUrl", binanceConfig.getBaseUrl());
        response.put("testnetEnabled", String.valueOf(binanceConfig.isTestnetEnabled()));
        
        return ResponseEntity.ok(response);
    }
}
