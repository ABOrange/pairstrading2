package andy.crypto.pairstrading.bot.pairstrading.service.impl;

import andy.crypto.pairstrading.bot.entity.TradingConfig;
import andy.crypto.pairstrading.bot.pairstrading.service.BinanceApiService;
import andy.crypto.pairstrading.bot.pairstrading.service.TradingPairManagementService;
import andy.crypto.pairstrading.bot.repository.TradingConfigRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 交易對管理服務實現類
 */
@Slf4j
@Service
public class TradingPairManagementServiceImpl implements TradingPairManagementService {

    @Autowired
    private TradingConfigRepository tradingConfigRepository;
    
    @Autowired
    private BinanceApiService binanceApiService;
    
    // 交易對組合配置鍵前綴
    private static final String PAIR_COMBINATION_PREFIX = "trading.pair.combination.";
    
    @Override
    public List<String> getSavedTradingPairCombinations() {
        try {
            List<TradingConfig> configs = tradingConfigRepository.findByConfigKeyStartingWith(PAIR_COMBINATION_PREFIX);
            return configs.stream()
                    .map(TradingConfig::getConfigValue)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("獲取已保存的交易對組合失敗", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public boolean saveTradingPairCombination(String pairCombination) {
        try {
            // 驗證交易對組合
            if (!validateTradingPairCombination(pairCombination)) {
                return false;
            }
            
            // 生成唯一鍵
            String configKey = PAIR_COMBINATION_PREFIX + System.currentTimeMillis();
            
            // 保存到資料庫
            TradingConfig config = new TradingConfig(
                    configKey, 
                    pairCombination,
                    "交易對組合: " + pairCombination,
                    "PairCombination"
            );
            tradingConfigRepository.save(config);
            
            log.info("成功保存交易對組合: {}", pairCombination);
            return true;
        } catch (Exception e) {
            log.error("保存交易對組合失敗", e);
            return false;
        }
    }
    
    @Override
    public boolean deleteTradingPairCombination(String pairCombination) {
        try {
            List<TradingConfig> configs = tradingConfigRepository.findByConfigValue(pairCombination);
            if (configs.isEmpty()) {
                log.warn("未找到要刪除的交易對組合: {}", pairCombination);
                return false;
            }
            
            // 刪除所有匹配的配置
            for (TradingConfig config : configs) {
                tradingConfigRepository.delete(config);
            }
            
            log.info("成功刪除交易對組合: {}", pairCombination);
            return true;
        } catch (Exception e) {
            log.error("刪除交易對組合失敗", e);
            return false;
        }
    }
    
    @Override
    public boolean validateTradingPairCombination(String pairCombination) {
        // 基本格式驗證
        if (pairCombination == null || pairCombination.trim().isEmpty() || !pairCombination.contains(",")) {
            log.warn("交易對組合格式不正確: {}", pairCombination);
            return false;
        }
        
        // 拆分交易對並驗證
        String[] pairs = pairCombination.split(",");
        if (pairs.length != 2) {
            log.warn("交易對組合必須包含兩個交易對: {}", pairCombination);
            return false;
        }
        
        // 驗證交易對是否可用
        List<String> availablePairs;
        try {
            availablePairs = binanceApiService.getAvailableFuturesPairs();
            if (!availablePairs.contains(pairs[0]) || !availablePairs.contains(pairs[1])) {
                log.warn("交易對組合中包含不可用的交易對: {}", pairCombination);
                return false;
            }
        } catch (Exception e) {
            log.error("驗證交易對組合時獲取可用交易對列表失敗", e);
            return false;
        }
        
        return true;
    }
}
