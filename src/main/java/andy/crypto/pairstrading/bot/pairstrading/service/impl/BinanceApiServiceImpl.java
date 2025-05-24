package andy.crypto.pairstrading.bot.pairstrading.service.impl;

import andy.crypto.pairstrading.bot.pairstrading.config.BinanceConfig;
import andy.crypto.pairstrading.bot.pairstrading.exception.BinanceApiException;
import andy.crypto.pairstrading.bot.pairstrading.model.CandlestickData;
import andy.crypto.pairstrading.bot.pairstrading.model.OrderResponse;
import andy.crypto.pairstrading.bot.pairstrading.model.PositionInfo;
import andy.crypto.pairstrading.bot.pairstrading.model.PriceFilter;
import andy.crypto.pairstrading.bot.pairstrading.model.SymbolInfo;
import andy.crypto.pairstrading.bot.pairstrading.service.BinanceApiService;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 幣安永續合約API服務實現類
 */
@Slf4j
@Service
public class BinanceApiServiceImpl implements BinanceApiService {

    private final BinanceConfig binanceConfig;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;

    @Autowired
    public BinanceApiServiceImpl(BinanceConfig binanceConfig, ObjectMapper objectMapper) {
        this.binanceConfig = binanceConfig;
        this.objectMapper = objectMapper;

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(org.apache.hc.core5.util.Timeout.ofMilliseconds(binanceConfig.getConnectionTimeout()))
                .setResponseTimeout(org.apache.hc.core5.util.Timeout.ofMilliseconds(binanceConfig.getReadTimeout()))
                .build();

        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    @Override
    public Map<String, BigDecimal> getAccountBalance() {
        try {
            String endpoint = "/fapi/v2/balance";
            String response = callApi(endpoint, null, "GET", true);

            List<Map<String, Object>> balances = objectMapper.readValue(response, new TypeReference<List<Map<String, Object>>>() {});

            Map<String, BigDecimal> balanceMap = new HashMap<>();
            for (Map<String, Object> balance : balances) {
                String asset = (String) balance.get("asset");
                BigDecimal availableBalance = new BigDecimal(balance.get("availableBalance").toString());
                balanceMap.put(asset, availableBalance);
            }

            return balanceMap;
        } catch (Exception e) {
            log.error("獲取帳戶餘額失敗", e);
            throw new BinanceApiException("獲取帳戶餘額失敗: " + e.getMessage(), e);
        }
    }

    @Override
    public BigDecimal getLatestPrice(String symbol) {
        try {
            String endpoint = "/fapi/v1/ticker/price";
            Map<String, Object> params = new HashMap<>();
            params.put("symbol", symbol);

            String response = callApi(endpoint, params, "GET", false);
            Map<String, Object> priceData = objectMapper.readValue(response, Map.class);

            return new BigDecimal(priceData.get("price").toString());
        } catch (Exception e) {
            log.error("獲取最新價格失敗: {}", symbol, e);
            throw new BinanceApiException("獲取最新價格失敗: " + e.getMessage(), e);
        }
    }

    @Override
    public List<CandlestickData> getCandlestickData(String symbol, String interval, Integer limit) {
        try {
            String endpoint = "/fapi/v1/klines";
            Map<String, Object> params = new HashMap<>();
            params.put("symbol", symbol);
            params.put("interval", interval);

            if (limit != null && limit > 0) {
                params.put("limit", limit);
            }

            String response = callApi(endpoint, params, "GET", false);
            List<List<Object>> rawData = objectMapper.readValue(response, new TypeReference<List<List<Object>>>() {});

            List<CandlestickData> candlesticks = new ArrayList<>();
            for (List<Object> candle : rawData) {
                CandlestickData candlestick = CandlestickData.builder()
                        .openTime(Long.parseLong(candle.get(0).toString()))
                        .open(new BigDecimal(candle.get(1).toString()))
                        .high(new BigDecimal(candle.get(2).toString()))
                        .low(new BigDecimal(candle.get(3).toString()))
                        .close(new BigDecimal(candle.get(4).toString()))
                        .volume(new BigDecimal(candle.get(5).toString()))
                        .closeTime(Long.parseLong(candle.get(6).toString()))
                        .quoteAssetVolume(new BigDecimal(candle.get(7).toString()))
                        .numberOfTrades(Integer.parseInt(candle.get(8).toString()))
                        .takerBuyBaseAssetVolume(new BigDecimal(candle.get(9).toString()))
                        .takerBuyQuoteAssetVolume(new BigDecimal(candle.get(10).toString()))
                        .build();

                candlesticks.add(candlestick);
            }

            return candlesticks;
        } catch (Exception e) {
            log.error("獲取K線數據失敗: {}", symbol, e);
            throw new BinanceApiException("獲取K線數據失敗: " + e.getMessage(), e);
        }
    }

    @Override
    public SymbolInfo getSymbolInfo(String symbol) {
        try {
            String endpoint = "/fapi/v1/exchangeInfo";
            String response = callApi(endpoint, null, "GET", false);

            JsonNode root = objectMapper.readTree(response);
            JsonNode symbols = root.get("symbols");

            for (JsonNode symbolNode : symbols) {
                if (symbol.equals(symbolNode.get("symbol").asText())) {
                    return SymbolInfo.builder()
                            .symbol(symbolNode.get("symbol").asText())
                            .pair(symbolNode.get("pair").asText())
                            .contractType(symbolNode.get("contractType").asText())
                            .deliveryDate(symbolNode.get("deliveryDate").asLong())
                            .onboardDate(symbolNode.get("onboardDate").asLong())
                            .status(symbolNode.get("status").asText())
                            .maintMarginPercent(new BigDecimal(symbolNode.get("maintMarginPercent").asText()))
                            .requiredMarginPercent(new BigDecimal(symbolNode.get("requiredMarginPercent").asText()))
                            .baseAsset(symbolNode.get("baseAsset").asText())
                            .quoteAsset(symbolNode.get("quoteAsset").asText())
                            .marginAsset(symbolNode.get("marginAsset").asText())
                            .pricePrecision(symbolNode.get("pricePrecision").asInt())
                            .quantityPrecision(symbolNode.get("quantityPrecision").asInt())
                            .baseAssetPrecision(new BigDecimal(symbolNode.get("baseAssetPrecision").asText()))
                            .quotePrecision(new BigDecimal(symbolNode.get("quotePrecision").asText()))
                            .build();
                }
            }

            throw new BinanceApiException("未找到交易對資訊: " + symbol);
        } catch (BinanceApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("獲取交易對資訊失敗: {}", symbol, e);
            throw new BinanceApiException("獲取交易對資訊失敗: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PositionInfo> getPositionInfo(String symbol) {
        try {
            String endpoint = "/fapi/v2/positionRisk";
            Map<String, Object> params = new HashMap<>();

            if (symbol != null && !symbol.isEmpty()) {
                params.put("symbol", symbol);
            }

            String response = callApi(endpoint, params, "GET", true);
            List<Map<String, Object>> positions = objectMapper.readValue(response, new TypeReference<List<Map<String, Object>>>() {});

            List<PositionInfo> positionInfoList = new ArrayList<>();
            for (Map<String, Object> position : positions) {
                // 只返回有實際倉位的資訊
                BigDecimal positionAmt = new BigDecimal(position.get("positionAmt").toString());
                if (positionAmt.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }

                PositionInfo positionInfo = PositionInfo.builder()
                        .symbol((String) position.get("symbol"))
                        .positionSide((String) position.get("positionSide"))
                        .entryPrice(new BigDecimal(position.get("entryPrice").toString()))
                        .markPrice(new BigDecimal(position.get("markPrice").toString()))
                        .positionAmt(positionAmt)
                        .unrealizedProfit(new BigDecimal(position.get("unRealizedProfit").toString()))
                        .unrealizedProfitPercentage(calculateUnrealizedProfitPercentage(
                                new BigDecimal(position.get("unRealizedProfit").toString()),
                                new BigDecimal(position.get("markPrice").toString()),
                                new BigDecimal(position.get("positionAmt").toString()),
                                new BigDecimal(position.get("leverage").toString())
                        ))
                        .liquidationPrice(new BigDecimal(position.get("liquidationPrice").toString()))
                        .leverage(new BigDecimal(position.get("leverage").toString()))
                        .isolated("ISOLATED".equals(position.get("marginType")))
                        .updateTime(Long.parseLong(position.get("updateTime").toString()))
                        .build();

                positionInfoList.add(positionInfo);
                System.out.println("~~~~~~~~POSITION"+positionInfoList);
            }

            return positionInfoList;
        } catch (Exception e) {
            log.error("獲取持倉信息失敗", e);
            throw new BinanceApiException("獲取持倉信息失敗: " + e.getMessage(), e);
        }
    }

    @Override
    public OrderResponse placeOrder(String symbol, String side, String positionSide, String type, BigDecimal quantity, BigDecimal price) {
        try {
            String endpoint = "/fapi/v1/order";
            Map<String, Object> params = new HashMap<>();
            params.put("symbol", symbol);
            params.put("side", side);

            if (positionSide != null && !positionSide.isEmpty()) {
                params.put("positionSide", positionSide);
            }

            params.put("type", type);
            params.put("quantity", quantity.toString());

            if (price != null && !"MARKET".equals(type)) {
                params.put("price", price.toString());
                params.put("timeInForce", "GTC"); // 除非被取消，否則訂單將一直有效
            }

            params.put("newClientOrderId", "pairs_trading_" + System.currentTimeMillis());

            String response = callApi(endpoint, params, "POST", true);
            return objectMapper.readValue(response, OrderResponse.class);
        } catch (Exception e) {
            log.error("下單失敗: {}", symbol, e);
            throw new BinanceApiException("下單失敗: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean cancelOrder(String symbol, Long orderId) {
        try {
            // 先檢查訂單是否存在且可以取消
            List<OrderResponse> openOrders = getOpenOrders(symbol);
            boolean orderExists = false;

            for (OrderResponse order : openOrders) {
                if (order.getOrderId().equals(orderId)) {
                    orderExists = true;
                    log.info("找到要取消的訂單: ID={}, 狀態={}", orderId, order.getStatus());
                    break;
                }
            }

            if (!orderExists) {
                log.warn("找不到要取消的訂單: 交易對={}, 訂單ID={}", symbol, orderId);
                return false;
            }

            // 執行取消訂單操作
            String endpoint = "/fapi/v1/order";
            Map<String, Object> params = new HashMap<>();
            params.put("symbol", symbol);
            params.put("orderId", orderId);

            String response = callApi(endpoint, params, "DELETE", true);
            OrderResponse cancelResponse = objectMapper.readValue(response, OrderResponse.class);

            return cancelResponse != null && cancelResponse.getOrderId() != null;
        } catch (Exception e) {
            log.error("取消訂單失敗: {}, orderId: {}", symbol, orderId, e);
            throw new BinanceApiException("取消訂單失敗: " + e.getMessage(), e);
        }
    }

    @Override
    public List<OrderResponse> getOpenOrders(String symbol) {
        try {
            String endpoint = "/fapi/v1/openOrders";
            Map<String, Object> params = new HashMap<>();

            if (symbol != null && !symbol.isEmpty()) {
                params.put("symbol", symbol);
            }

            String response = callApi(endpoint, params, "GET", true);
            return objectMapper.readValue(response, new TypeReference<List<OrderResponse>>() {});
        } catch (Exception e) {
            log.error("獲取未完成訂單失敗", e);
            throw new BinanceApiException("獲取未完成訂單失敗: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean closeAllPositions(String symbol) {
        try {
            List<PositionInfo> positions = getPositionInfo(symbol);

            for (PositionInfo position : positions) {
                String side = position.isLongPosition() ? "SELL" : "BUY";
                String positionSide = position.getPositionSide();
                BigDecimal quantity = position.getAbsolutePositionSize();

                placeOrder(position.getSymbol(), side, positionSide, "MARKET", quantity, null);
            }

            return true;
        } catch (Exception e) {
            log.error("關閉所有倉位失敗: {}", symbol, e);
            throw new BinanceApiException("關閉所有倉位失敗: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> getAvailableFuturesPairs() {
        try {
            String endpoint = "/fapi/v1/exchangeInfo";
            String response = callApi(endpoint, null, "GET", false);

            JsonNode root = objectMapper.readTree(response);
            JsonNode symbols = root.get("symbols");

            List<String> availablePairs = new ArrayList<>();

            for (JsonNode symbolNode : symbols) {
                // 只返回正在交易中的交易對
                if ("TRADING".equals(symbolNode.get("status").asText())) {
                    availablePairs.add(symbolNode.get("symbol").asText());
                }
            }

            // 按英文字母順序排序
            Collections.sort(availablePairs);

            return availablePairs;
        } catch (Exception e) {
            log.error("獲取可用交易對失敗", e);
            throw new BinanceApiException("獲取可用交易對失敗: " + e.getMessage(), e);
        }
    }

    @Override
    public PriceFilter getPriceFilter(String symbol) {
        try {
            String endpoint = "/fapi/v1/exchangeInfo";
            String response = callApi(endpoint, null, "GET", false);

            JsonNode root = objectMapper.readTree(response);
            JsonNode symbols = root.get("symbols");

            for (JsonNode symbolNode : symbols) {
                if (symbol.equals(symbolNode.get("symbol").asText())) {
                    JsonNode filters = symbolNode.get("filters");

                    // 查找價格過濾器
                    for (JsonNode filter : filters) {
                        if ("PRICE_FILTER".equals(filter.get("filterType").asText())) {
                            return PriceFilter.builder()
                                    .minPrice(new BigDecimal(filter.get("minPrice").asText()))
                                    .maxPrice(new BigDecimal(filter.get("maxPrice").asText()))
                                    .tickSize(new BigDecimal(filter.get("tickSize").asText()))
                                    .build();
                        }
                    }
                }
            }

            throw new BinanceApiException("未找到交易對價格過濾器: " + symbol);
        } catch (Exception e) {
            log.error("獲取價格過濾器失敗: {}", symbol, e);
            throw new BinanceApiException("獲取價格過濾器失敗: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean setLeverage(String symbol, int leverage) {
        try {
            String endpoint = "/fapi/v1/leverage";
            Map<String, Object> params = new HashMap<>();
            params.put("symbol", symbol);
            params.put("leverage", leverage);

            String response = callApi(endpoint, params, "POST", true);

            log.info("設定 {} 槓桿倍率為 {}, 回應: {}", symbol, leverage, response);

            // 檢查回應是否包含正確的槓桿值
            JsonNode root = objectMapper.readTree(response);
            int returnedLeverage = root.get("leverage").asInt();

            return returnedLeverage == leverage;
        } catch (Exception e) {
            log.error("設定槓桿倍率失敗: {}, 槓桿: {}", symbol, leverage, e);
            return false;
        }
    }

    @Override
    public BigDecimal adjustPriceToTickSize(String symbol, BigDecimal price) {
        try {
            PriceFilter priceFilter = getPriceFilter(symbol);
            BigDecimal tickSize = priceFilter.getTickSize();

            // 如果價格已經是有效價格，則直接返回
            if (price.remainder(tickSize).compareTo(BigDecimal.ZERO) == 0) {
                return price;
            }

            // 調整價格為符合步長的值 (向下取整)
            // 計算公式: price - (price % tickSize)
            // 在Java中，可以使用 divide 和 multiply 來實現
            return price.divide(tickSize, 0, BigDecimal.ROUND_DOWN).multiply(tickSize);
        } catch (Exception e) {
            log.error("調整價格失敗: {}", symbol, e);
            throw new BinanceApiException("調整價格失敗: " + e.getMessage(), e);
        }
    }

    @Override
    public BigDecimal adjustQuantityToPrecision(String symbol, BigDecimal quantity) {
        try {
            SymbolInfo symbolInfo = getSymbolInfo(symbol);
            int quantityPrecision = symbolInfo.getQuantityPrecision();

            // 調整數量到指定精度，使用ROUND_UP確保不會因精度調整導致名義價值低於最小要求
            return quantity.setScale(quantityPrecision, BigDecimal.ROUND_UP);
        } catch (Exception e) {
            log.error("調整數量失敗: {}", symbol, e);
            throw new BinanceApiException("調整數量失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 調用幣安API
     *
     * @param endpoint API端點
     * @param params 參數
     * @param method HTTP方法
     * @param needSignature 是否需要簽名
     * @return API回應
     */
    private String callApi(String endpoint, Map<String, Object> params, String method, boolean needSignature) throws IOException {
        // 檢查API金鑰是否已設定（如果需要簽名）
        if (needSignature) {
            if (binanceConfig.getApiKey() == null || binanceConfig.getApiKey().isEmpty() ||
                    binanceConfig.getSecretKey() == null || binanceConfig.getSecretKey().isEmpty()) {
                throw new BinanceApiException("API金鑰或秘鑰未配置，請在設定頁面配置API金鑰");
            }
        }

        String url = binanceConfig.getBaseUrl() + endpoint;
        HttpUriRequestBase request;

        // 構建查詢參數
        StringBuilder queryString = new StringBuilder();
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (queryString.length() > 0) {
                    queryString.append("&");
                }
                queryString.append(entry.getKey()).append("=").append(entry.getValue());
            }
        }

        // 添加時間戳和簽名
        if (needSignature) {
            if (queryString.length() > 0) {
                queryString.append("&");
            }

            long timestamp = System.currentTimeMillis();
            queryString.append("timestamp=").append(timestamp);

            // 生成簽名
            String signature = generateSignature(queryString.toString(), binanceConfig.getSecretKey());
            queryString.append("&signature=").append(signature);
        }

        // 根據HTTP方法創建請求
        if ("GET".equals(method)) {
            if (queryString.length() > 0) {
                url = url + "?" + queryString;
            }
            request = new HttpGet(url);
        } else if ("POST".equals(method)) {
            request = new HttpPost(url);
            if (queryString.length() > 0) {
                ((HttpPost) request).setEntity(new StringEntity(queryString.toString(), ContentType.APPLICATION_FORM_URLENCODED));
            }
        } else if ("DELETE".equals(method)) {
            if (queryString.length() > 0) {
                url = url + "?" + queryString;
            }
            request = new HttpDelete(url);
        } else {
            throw new BinanceApiException("不支持的HTTP方法: " + method);
        }

        // 添加API密鑰到頭部（如果已設定）
        if (binanceConfig.getApiKey() != null && !binanceConfig.getApiKey().isEmpty()) {
            request.addHeader("X-MBX-APIKEY", binanceConfig.getApiKey());
        }

        // 執行請求
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody;
            try {
                responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            } catch (ParseException e) {
                log.error("解析API回應失敗", e);
                throw new BinanceApiException("解析API回應失敗: " + e.getMessage(), e);
            }

            int statusCode = response.getCode();
            if (statusCode != 200) {
                log.error("API請求失敗，狀態碼: {}, 回應: {}", statusCode, responseBody);
                throw new BinanceApiException("API請求失敗，狀態碼: " + statusCode + ", 回應: " + responseBody);
            }
            return responseBody;
        }
    }

    /**
     * 生成HMAC SHA256簽名
     *
     * @param data 要簽名的數據
     * @param key 密鑰
     * @return 簽名
     */
    private String generateSignature(String data, String key) {
        try {
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKeySpec);

            byte[] hash = hmacSha256.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("生成簽名失敗", e);
            throw new BinanceApiException("生成簽名失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 將位元組轉換為十六進制字串
     *
     * @param bytes 位元組
     * @return 十六進制字串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String calculateUnrealizedProfitPercentage(
            BigDecimal unrealizedProfit,
            BigDecimal markPrice,
            BigDecimal positionAmt,
            BigDecimal leverage
    ) {
        // Avoid division by zero
        if (markPrice.equals(BigDecimal.ZERO) || leverage.equals(BigDecimal.ZERO)) {
            return "0.00%";
        }

        // Calculate Notional Value
        BigDecimal notionalValue = markPrice.multiply(positionAmt.abs());

        // Calculate Initial Margin
        BigDecimal initialMargin = notionalValue.divide(leverage, 4, RoundingMode.HALF_UP);

        // Calculate the Profit Rate
        BigDecimal profitRate = unrealizedProfit.divide(initialMargin, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        // Format to 2 decimal places with % symbol
        return String.format("%.2f%%", profitRate);
    }

}
