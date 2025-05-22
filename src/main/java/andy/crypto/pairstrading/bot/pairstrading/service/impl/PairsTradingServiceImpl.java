package andy.crypto.pairstrading.bot.pairstrading.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import andy.crypto.pairstrading.bot.bean.PairsTradingServiceValueBean;
import andy.crypto.pairstrading.bot.pairstrading.model.PairsTradingResult;
import andy.crypto.pairstrading.bot.pairstrading.service.*;

import java.util.List;
import java.util.Map;

/**
 * 配對交易服務實現類 - 門面模式
 */
@Slf4j
@Service
public class PairsTradingServiceImpl implements PairsTradingService {

    @Autowired
    private MarketDataService marketDataService;
    
    @Autowired
    private SignalAnalysisService signalAnalysisService;
    
    @Autowired
    private TradingExecutionService tradingExecutionService;
    
    @Autowired
    private BackTestingService backTestingService;
    
    @Autowired
    private TradingPairManagementService tradingPairManagementService;

    @Override
    public PairsTradingServiceValueBean fetchMarketData() {
        return marketDataService.fetchMarketData();
    }

    @Override
    public PairsTradingServiceValueBean fetchMarketData(String asset1, String asset2) {
        return marketDataService.fetchMarketData(asset1, asset2);
    }

    @Override
    public void analyzeCorrelation(PairsTradingServiceValueBean pairsTradingServiceValueBean) {
        signalAnalysisService.analyzeCorrelation(pairsTradingServiceValueBean);
    }

    @Override
    public boolean calculateSignal(PairsTradingServiceValueBean pairsTradingServiceValueBean) {
        return signalAnalysisService.calculateSignal(pairsTradingServiceValueBean);
    }

    @Override
    public void executeTrade(boolean isBuy) {
        tradingExecutionService.executeTrade(isBuy);
    }
    
    @Override
    public String getZScoreChart(String asset1, String asset2) {
        return marketDataService.getZScoreChart(asset1, asset2);
    }

    @Override
    public String getSignalReport() {
        return signalAnalysisService.getSignalReport();
    }

    @Override
    public boolean setWindowSize(int newWindowSize) {
        return signalAnalysisService.setWindowSize(newWindowSize);
    }

    @Override
    public List<Double> getZScoreHistory(String asset1, String asset2) {
        return marketDataService.getZScoreHistory(asset1, asset2);
    }

    @Override
    public List<Double> getSpreadHistory(String asset1, String asset2) {
        return marketDataService.getSpreadHistory(asset1, asset2);
    }

    @Override
    public List<Long> getTimeHistory(String asset1, String asset2) {
        return marketDataService.getTimeHistory(asset1, asset2);
    }

    @Override
    public List<String> getAvailableTradingPairs() {
        return marketDataService.getAvailableTradingPairs();
    }

    @Override
    public List<String> getSavedTradingPairCombinations() {
        return tradingPairManagementService.getSavedTradingPairCombinations();
    }

    @Override
    public boolean saveTradingPairCombination(String pairCombination) {
        return tradingPairManagementService.saveTradingPairCombination(pairCombination);
    }

    @Override
    public boolean deleteTradingPairCombination(String pairCombination) {
        return tradingPairManagementService.deleteTradingPairCombination(pairCombination);
    }

    @Override
    public PairsTradingResult backTestPairCombination(String symbol1, String symbol2, Integer backTestDays) {
        return backTestingService.backTestPairCombination(symbol1, symbol2, backTestDays);
    }

    @Override
    public PairsTradingResult backTestPairCombination(String symbol1, String symbol2, Integer backTestDays, String interval) {
        return backTestingService.backTestPairCombination(symbol1, symbol2, backTestDays, interval);
    }

    @Override
    public Map<String, PairsTradingResult> batchBackTestPairCombinations(List<String> pairCombinations, Integer backTestDays, String interval) {
        return backTestingService.batchBackTestPairCombinations(pairCombinations, backTestDays, interval);
    }

    @Override
    public Map<String, PairsTradingResult> backTestAllSavedPairCombinations(Integer backTestDays, String interval) {
        return backTestingService.backTestAllSavedPairCombinations(backTestDays, interval);
    }

    @Override
    public String getBackTestZScoreChart(String symbol1, String symbol2) {
        return backTestingService.getBackTestZScoreChart(symbol1, symbol2);
    }

    @Override
    public String getBackTestSpreadChart(String symbol1, String symbol2) {
        return backTestingService.getBackTestSpreadChart(symbol1, symbol2);
    }

    @Override
    public int getWindowSize() {
        return signalAnalysisService.getWindowSize();
    }
}
