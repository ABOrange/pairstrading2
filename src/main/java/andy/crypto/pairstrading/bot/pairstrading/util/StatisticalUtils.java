package andy.crypto.pairstrading.bot.pairstrading.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.util.List;
import java.util.Arrays;

/**
 * 統計相關工具類
 * 提供常用的統計分析方法
 */
@Slf4j
public class StatisticalUtils {

    /**
     * 模擬 TradingView ta.correlation 的母體相關性計算
     *
     * @param x 序列X
     * @param y 序列Y
     * @return 相關性係數
     */
    public static double pineCorrelation(double[] x, double[] y) {
        if (x == null || y == null || x.length != y.length || x.length < 2) {
            throw new IllegalArgumentException("輸入序列無效或長度不匹配");
        }
        
        int n = x.length;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0, sumY2 = 0;
        for (int i = 0; i < n; i++) {
            sumX += x[i]; 
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumX2 += x[i] * x[i]; 
            sumY2 += y[i] * y[i];
        }
        double meanX = sumX / n, meanY = sumY / n;
        double cov = sumXY / n - meanX * meanY;
        double varX = sumX2 / n - meanX * meanX;
        double varY = sumY2 / n - meanY * meanY;
        return cov / Math.sqrt(varX * varY);
    }
    
    /**
     * 使用對稱迴歸（Orthogonal Regression）計算參數
     * 
     * @param xValues X序列值
     * @param yValues Y序列值
     * @return 包含alpha、beta和相關參數的結果對象
     */
    public static OrthogonalRegressionResult computeOrthogonalRegression(double[] xValues, double[] yValues) {
        if (xValues == null || yValues == null || xValues.length != yValues.length || xValues.length < 2) {
            throw new IllegalArgumentException("輸入序列無效或長度不匹配");
        }
        
        int len = xValues.length;
        
        // 1. 計算 X, Y 的平均
        double meanX = 0, meanY = 0;
        for (int i = 0; i < len; i++) {
            meanX += xValues[i];
            meanY += yValues[i];
        }
        meanX /= len;
        meanY /= len;
        
        // 2. 建資料矩陣並去中心化
        RealMatrix data = new Array2DRowRealMatrix(len, 2);
        for (int i = 0; i < len; i++) {
            data.setEntry(i, 0, xValues[i] - meanX);
            data.setEntry(i, 1, yValues[i] - meanY);
        }
        
        // 3. 計算協方差矩陣並做 SVD
        RealMatrix cov = data.transpose().multiply(data);
        SingularValueDecomposition svd = new SingularValueDecomposition(cov);
        RealMatrix V = svd.getV();
        
        // 4. 取第一主成分向量
        double vx = V.getEntry(0, 0);
        double vy = V.getEntry(1, 0);
        
        // 5. 計算斜率 β 與截距 α
        double beta = vy / vx;
        double alpha = meanY - beta * meanX;
        
        // 6. 計算正交殘差（每點到直線的垂直距離）
        double[] residuals = new double[len];
        double norm = Math.sqrt(1 + beta * beta);
        for (int i = 0; i < len; i++) {
            double y = yValues[i];
            double x = xValues[i];
            residuals[i] = (y - (alpha + beta * x)) / norm;
        }
        
        // 7. 計算殘差統計量
        double mean = 0.0; // 直接假設平均值為0
        double sumSq = Arrays.stream(residuals).map(d -> d * d).sum();
        double std = Math.sqrt(sumSq / len);
        
        return new OrthogonalRegressionResult(alpha, beta, residuals, mean, std);
    }
    
    /**
     * 手動實作 ADF 平穩性檢定
     * 用於判斷時間序列是否具有平穩性
     * 
     * @param series 輸入的時間序列數據
     * @param significanceLevel 顯著性水平，默認為0.05
     * @return 如果序列通過平穩性檢定，則返回true；否則返回false
     */
    public static boolean performADFTest(List<Double> series, double significanceLevel) {
        int n = series.size();
        if (n < 3) {
            log.warn("ADF 檢定需要至少 3 個樣本，當前 = {}", n);
            return false;
        }
        
        // 轉換為基本數據類型數組以提高效率
        double[] y = series.stream().mapToDouble(Double::doubleValue).toArray();
        
        // 建立迴歸模型: Δy_t = α + βy_{t-1} + ε
        SimpleRegression adfReg = new SimpleRegression(true);
        for (int i = 1; i < n; i++) {
            adfReg.addData(y[i - 1], y[i] - y[i - 1]);
        }
        
        // 計算t統計量
        double slope = adfReg.getSlope();
        double standardError = adfReg.getSlopeStdErr();
        double tStat = slope / standardError;
        
        // 計算自由度和p值
        int degreesOfFreedom = n - 3;
        double pValue = new TDistribution(degreesOfFreedom).cumulativeProbability(tStat);
        
        // 記錄詳細的檢定結果
        log.debug("ADF檢定結果: 斜率={}, t統計量={}, p值={}, 顯著性水平={}", 
                 slope, tStat, pValue, significanceLevel);
        
        // 如果p值小於顯著性水平，則拒絕原假設（存在單位根），即時間序列是平穩的
        return pValue < significanceLevel;
    }
    
    /**
     * 使用預設顯著性水平0.05進行ADF檢定
     */
    public static boolean performADFTest(List<Double> series) {
        return performADFTest(series, 0.05);
    }
    
    /**
     * 正交迴歸結果類
     * 封裝正交迴歸分析的結果
     */
    public static class OrthogonalRegressionResult {
        private final double alpha;        // 迴歸截距
        private final double beta;         // 迴歸斜率
        private final double[] residuals;  // 殘差序列
        private final double mean;         // 殘差平均值
        private final double std;          // 殘差標準差
        
        public OrthogonalRegressionResult(double alpha, double beta, double[] residuals, double mean, double std) {
            this.alpha = alpha;
            this.beta = beta;
            this.residuals = residuals;
            this.mean = mean;
            this.std = std;
        }
        
        public double getAlpha() {
            return alpha;
        }
        
        public double getBeta() {
            return beta;
        }
        
        public double[] getResiduals() {
            return residuals;
        }
        
        public double getMean() {
            return mean;
        }
        
        public double getStd() {
            return std;
        }
        
        /**
         * 計算給定價格對的Z分數
         * 
         * @param x X值
         * @param y Y值
         * @return Z分數
         */
        public double calculateZScore(double x, double y) {
            double residual = (y - (alpha + beta * x)) / Math.sqrt(1 + beta * beta);
            return (residual - mean) / std;
        }
    }
}
