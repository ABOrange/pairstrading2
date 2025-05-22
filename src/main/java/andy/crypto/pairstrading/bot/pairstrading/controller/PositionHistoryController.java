package andy.crypto.pairstrading.bot.pairstrading.controller;

import andy.crypto.pairstrading.bot.bean.PairsTradingServiceValueBean;
import andy.crypto.pairstrading.bot.pairstrading.model.PositionHistory;
import andy.crypto.pairstrading.bot.pairstrading.model.PositionInfo;
import andy.crypto.pairstrading.bot.pairstrading.service.BinanceApiService;
import andy.crypto.pairstrading.bot.pairstrading.service.PairsTradingService;
import andy.crypto.pairstrading.bot.pairstrading.service.PositionHistoryService;
import andy.crypto.pairstrading.bot.service.TradingConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 倉位歷史記錄API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/position")
public class PositionHistoryController {

    @Autowired
    private PositionHistoryService positionHistoryService;

    @Autowired
    private BinanceApiService binanceApiService;

    @Autowired
    private TradingConfigService tradingConfigService;

    @Autowired
    private PairsTradingService pairsTradingService;

    @Value("${trading.pair.asset1:BNBUSDT}")
    private String asset1;

    @Value("${trading.pair.asset2:SOLUSDT}")
    private String asset2;

    /**
     * 將 BigDecimal 轉換為字符串，以解決序列化問題
     */
    private List<Map<String, Object>> convertToSerializableFormat(List<PositionHistory> histories) {
        if (histories == null) {
            return new ArrayList<>();
        }

        return histories.stream().map(history -> {
            Map<String, Object> map = new HashMap<>();
            map.put("symbol", history.getSymbol());
            map.put("positionSide", history.getPositionSide());
            map.put("entryPrice", history.getEntryPrice() != null ? history.getEntryPrice().toString() : null);
            map.put("exitPrice", history.getExitPrice() != null ? history.getExitPrice().toString() : null);
            map.put("positionAmt", history.getPositionAmt() != null ? history.getPositionAmt().toString() : null);
            map.put("realizedProfit", history.getRealizedProfit() != null ? history.getRealizedProfit().toString() : null);
            map.put("action", history.getAction());
            map.put("timestamp", history.getTimestamp());
            map.put("formattedTime", history.getFormattedTime());
            map.put("reason", history.getReason());
            map.put("zScore", history.getZScore());
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 獲取所有歷史倉位記錄
     */
    @GetMapping("/history")
    public Map<String, Object> getPositionHistory(
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false, defaultValue = "0") int limit) {

        Map<String, Object> result = new HashMap<>();

        try {
            List<PositionHistory> history;

            if (limit > 0) {
                // 有限制數量，獲取最近的記錄
                history = positionHistoryService.getRecentPositionHistory(symbol, limit);
            } else {
                // 沒有限制，獲取全部記錄
                history = positionHistoryService.getPositionHistory(symbol);
            }

            // 將 PositionHistory 轉換為可序列化的格式
            List<Map<String, Object>> serializableHistory = convertToSerializableFormat(history);

            result.put("history", serializableHistory);
            result.put("count", serializableHistory.size());
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "獲取歷史倉位記錄失敗: " + e.getMessage());
            // 確保即使出錯也返回一個空列表，讓前端有東西可以處理
            result.put("history", new ArrayList<>());
            result.put("count", 0);
        }

        return result;
    }

    /**
     * 獲取每個交易對的最近倉位記錄
     */
    @GetMapping("/recent")
    public Map<String, Object> getRecentPositionHistory(
            @RequestParam(required = false, defaultValue = "10") int limit) {

        Map<String, Object> result = new HashMap<>();

        try {
            // 獲取資產歷史記錄，確保資產代號非空
            String asset1Symbol = (asset1 != null && !asset1.isEmpty()) ? asset1 : "BNBUSDT";
            String asset2Symbol = (asset2 != null && !asset2.isEmpty()) ? asset2 : "SOLUSDT";

            List<PositionHistory> asset1History = positionHistoryService.getRecentPositionHistory(asset1Symbol, limit);
            List<PositionHistory> asset2History = positionHistoryService.getRecentPositionHistory(asset2Symbol, limit);

            // 將 PositionHistory 轉換為可序列化的格式
            List<Map<String, Object>> serializableAsset1History = convertToSerializableFormat(asset1History);
            List<Map<String, Object>> serializableAsset2History = convertToSerializableFormat(asset2History);

            Map<String, List<Map<String, Object>>> historyMap = new HashMap<>();
            historyMap.put(asset1Symbol, serializableAsset1History);
            historyMap.put(asset2Symbol, serializableAsset2History);

            result.put("history", historyMap);
            result.put("asset1", asset1Symbol);
            result.put("asset2", asset2Symbol);
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "獲取最近倉位記錄失敗: " + e.getMessage());

            // 發生錯誤時也返回空數據，確保前端能正常處理
            Map<String, List<Map<String, Object>>> emptyMap = new HashMap<>();
            emptyMap.put(asset1 != null ? asset1 : "BNBUSDT", new ArrayList<>());
            emptyMap.put(asset2 != null ? asset2 : "SOLUSDT", new ArrayList<>());

            result.put("history", emptyMap);
            result.put("asset1", asset1 != null ? asset1 : "BNBUSDT");
            result.put("asset2", asset2 != null ? asset2 : "SOLUSDT");
        }

        return result;
    }

    /**
     * 檢查是否有活躍持倉
     */
    @GetMapping("/check-active")
    public Map<String, Object> checkActivePositions() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 獲取當前交易對配置
            String asset1Symbol = tradingConfigService.getTradingConfigBean().getAsset1();
            String asset2Symbol = tradingConfigService.getTradingConfigBean().getAsset2();

            if (asset1Symbol == null || asset1Symbol.isEmpty()) {
                asset1Symbol = asset1 != null ? asset1 : "BNBUSDT";
            }

            if (asset2Symbol == null || asset2Symbol.isEmpty()) {
                asset2Symbol = asset2 != null ? asset2 : "SOLUSDT";
            }

            // 檢查是否有倉位
            List<PositionInfo> positions = binanceApiService.getPositionInfo(null);
            String finalAsset1Symbol = asset1Symbol;
            String finalAsset2Symbol = asset2Symbol;
            boolean hasPositions = positions.stream()
                    .anyMatch(p -> (p.getSymbol().equals(finalAsset1Symbol) || p.getSymbol().equals(finalAsset2Symbol))
                            && p.getPositionAmt() != null
                            && p.getPositionAmt().abs().compareTo(BigDecimal.ZERO) > 0);

            result.put("hasPositions", hasPositions);
            result.put("status", "success");

            // 添加當前交易對信息，方便前端使用
            result.put("asset1", asset1Symbol);
            result.put("asset2", asset2Symbol);

            log.info("檢查持倉狀態: 資產1={}, 資產2={}, 是否有持倉={}", asset1Symbol, asset2Symbol, hasPositions);
        } catch (Exception e) {
            log.error("檢查持倉狀態失敗", e);
            result.put("status", "error");
            result.put("message", "檢查持倉狀態失敗: " + e.getMessage());
            result.put("hasPositions", false); // 出錯時假設沒有持倉
        }

        return result;
    }

    /**
     * 立即平倉所有持倉
     */
    @PostMapping("/close-all")
    public Map<String, Object> closeAllPositions() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 獲取當前交易對配置
            String asset1Symbol = tradingConfigService.getTradingConfigBean().getAsset1();
            String asset2Symbol = tradingConfigService.getTradingConfigBean().getAsset2();

            if (asset1Symbol == null || asset1Symbol.isEmpty()) {
                asset1Symbol = asset1 != null ? asset1 : "BNBUSDT";
            }

            if (asset2Symbol == null || asset2Symbol.isEmpty()) {
                asset2Symbol = asset2 != null ? asset2 : "SOLUSDT";
            }

            // 檢查是否有倉位
            List<PositionInfo> positions = binanceApiService.getPositionInfo(null);

            if (positions.isEmpty()) {
                result.put("status", "info");
                result.put("message", "沒有需要平倉的持倉");
                return result;
            }



            // 平倉資產1
            log.info("正在平倉 {}", asset1Symbol);
            boolean closeAsset1Success = binanceApiService.closeAllPositions(asset1Symbol);

            // 平倉資產2
            log.info("正在平倉 {}", asset2Symbol);
            boolean closeAsset2Success = binanceApiService.closeAllPositions(asset2Symbol);

            if (closeAsset1Success && closeAsset2Success) {
                log.info("所有倉位平倉成功");
                result.put("status", "success");
                result.put("message", "所有倉位已成功平倉");
                PairsTradingServiceValueBean pairsTradingServiceValueBean =pairsTradingService.fetchMarketData(asset1Symbol,asset2Symbol);
                // 在平倉前先記錄所有相關倉位的歷史
                for (PositionInfo position : positions) {
                    if (position.getSymbol().equals(asset1Symbol) || position.getSymbol().equals(asset2Symbol)) {
                        // 檢查倉位是否有實際數量
                        if (position.getPositionAmt() != null && position.getPositionAmt().abs().compareTo(BigDecimal.ZERO) > 0) {
                            log.info("記錄平倉歷史: {} 倉位數量: {}", position.getSymbol(), position.getPositionAmt());
                            positionHistoryService.recordClosePosition(
                                    position,
                                    "手動平倉所有倉位",
                                    pairsTradingServiceValueBean.getLastZScore()  // Z分數不可用，因為這是手動平倉
                            );
                        }
                    }
                }
            } else {
                log.warn("部分倉位平倉失敗: 資產1平倉{}, 資產2平倉{}",
                        closeAsset1Success ? "成功" : "失敗",
                        closeAsset2Success ? "成功" : "失敗");
                result.put("status", "warning");
                result.put("message", "部分倉位平倉可能未成功，請查看持倉信息確認");
            }


        } catch (Exception e) {
            log.error("平倉所有倉位失敗", e);
            result.put("status", "error");
            result.put("message", "平倉所有倉位失敗: " + e.getMessage());
        }

        return result;
    }

    /**
     * 清空歷史倉位記錄
     */
    @PostMapping("/clear")
    public Map<String, Object> clearPositionHistory() {
        Map<String, Object> result = new HashMap<>();

        try {
            positionHistoryService.clearHistory();
            result.put("status", "success");
            result.put("message", "歷史倉位記錄已清空");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "清空歷史倉位記錄失敗: " + e.getMessage());
        }

        return result;
    }
}