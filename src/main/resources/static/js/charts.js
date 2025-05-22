/**
 * 配對交易機器人儀表板 - 圖表頁面 JavaScript
 */

// 圖表變量
let zScoreChart = null;
let spreadChart = null;

// 頁面載入完成後執行初始化
$(document).ready(function() {
    // 初始化圖表
    initCharts();
    
    // 初始化事件監聽
    initEventListeners();
    
    // 獲取當前窗口大小並設置到輸入框
    loadCurrentWindowSize();
    
    // 初始加載數據
    refreshPage();
});

/**
 * 初始化事件監聽
 */
function initEventListeners() {
    // 刷新圖表
    $('#refreshChartsBtn').on('click', function() {
        refreshPage();
        resetRefreshCounter();
    });
    
    // 設置窗口大小
    $('#applyWindowSizeBtn').on('click', function() {
        applyWindowSize();
    });
    
    // 回車鍵提交窗口大小
    $('#windowSizeInput').on('keypress', function(e) {
        if (e.which === 13) { // 回車鍵
            applyWindowSize();
        }
    });
}

/**
 * 刷新頁面數據 (覆蓋全局 refreshPage 函數)
 */
function refreshPage() {
    refreshCharts();
}

/**
 * 初始化圖表
 */
function initCharts() {
    // 如果圖表已經存在，則先銷毀它們
    if (zScoreChart) {
        zScoreChart.destroy();
    }
    if (spreadChart) {
        spreadChart.destroy();
    }
    
    // 初始化Z分數圖表
    const zScoreCtx = document.getElementById('zScoreChart').getContext('2d');
    zScoreChart = new Chart(zScoreCtx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: 'Z分數',
                data: [],
                borderColor: 'rgba(54, 162, 235, 1)',
                backgroundColor: 'rgba(54, 162, 235, 0.1)',
                borderWidth: 3,           // 增加線條粗細
                pointRadius: 4,           // 增加數據點大小
                pointHoverRadius: 6,      // 增加懸停時數據點大小
                tension: 0.1,
                fill: true
            }, {
                label: '入場閾值 (上限)',
                data: [],
                borderColor: 'rgba(255, 99, 132, 0.9)',  // 增加顏色飽和度
                borderWidth: 2,
                borderDash: [5, 5],
                fill: false
            }, {
                label: '入場閾值 (下限)',
                data: [],
                borderColor: 'rgba(255, 99, 132, 0.9)',  // 增加顏色飽和度
                borderWidth: 2,
                borderDash: [5, 5],
                fill: false
            }, {
                label: '出場閾值 (上限)',
                data: [],
                borderColor: 'rgba(75, 192, 192, 0.9)',  // 增加顏色飽和度
                borderWidth: 2,
                borderDash: [2, 2],
                fill: false
            }, {
                label: '出場閾值 (下限)',
                data: [],
                borderColor: 'rgba(75, 192, 192, 0.9)',  // 增加顏色飽和度
                borderWidth: 2,
                borderDash: [2, 2],
                fill: false
            }, {
                label: '零線',
                data: [],
                borderColor: 'rgba(0, 0, 0, 0.5)',      // 增加可見度
                borderWidth: 1,
                fill: false
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: false,
                    min: -5,  // 擴大Y軸範圍的最小值
                    max: 5,   // 擴大Y軸範圍的最大值
                    ticks: {
                        stepSize: 0.5,  // 減小刻度步長，增加刻度密度
                        font: {
                            size: 12   // 保持刻度標籤的字體大小
                        }
                    },
                    grid: {
                        color: 'rgba(0, 0, 0, 0.1)',  // 使網格線顏色更淺
                        drawBorder: true
                    }
                },
                x: {
                    ticks: {
                        maxRotation: 45,  // 斜角顯示標籤
                        minRotation: 45,
                        autoSkip: true,   // 自動跳過標籤以避免擁擠
                        maxTicksLimit: 8, // 減少標籤數量，避免擁擠
                        font: {
                            size: 11      // 調整字體大小
                        },
                        color: 'rgba(0, 0, 0, 0.7)' // 增加標籤顏色對比度
                    },
                    grid: {
                        display: false    // 隱藏X軸網格線，減少視覺干擾
                    }
                }
            },
            plugins: {
                title: {
                    display: true,
                    text: 'Z分數走勢圖 (窗口大小: 加載中...)'
                },
                tooltip: {
                    mode: 'index',
                    intersect: false
                }
            },
            animation: {
                duration: 500  // 縮短動畫時間，減少視覺干擾
            }
        }
    });
    
    // 初始化價差圖表
    const spreadCtx = document.getElementById('spreadChart').getContext('2d');
    spreadChart = new Chart(spreadCtx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: '價差',
                data: [],
                borderColor: 'rgba(255, 159, 64, 1)',
                backgroundColor: 'rgba(255, 159, 64, 0.1)',
                borderWidth: 3,            // 增加線條粗細
                pointRadius: 4,            // 增加數據點大小
                pointHoverRadius: 6,       // 增加懸停時數據點大小
                tension: 0.1,
                fill: true
            }, {
                label: '均值',
                data: [],
                borderColor: 'rgba(0, 0, 0, 0.6)',     // 增加可見度
                borderWidth: 2,                        // 增加線條粗細
                fill: false
            }, {
                label: '均值 + 1倍標準差',
                data: [],
                borderColor: 'rgba(255, 205, 86, 0.9)', // 增加顏色飽和度
                borderWidth: 2,
                borderDash: [5, 5],
                fill: false
            }, {
                label: '均值 - 1倍標準差',
                data: [],
                borderColor: 'rgba(255, 205, 86, 0.9)', // 增加顏色飽和度
                borderWidth: 2,
                borderDash: [5, 5],
                fill: false
            }, {
                label: '均值 + 2倍標準差',
                data: [],
                borderColor: 'rgba(255, 99, 132, 0.9)', // 增加顏色飽和度
                borderWidth: 2,
                borderDash: [2, 2],
                fill: false
            }, {
                label: '均值 - 2倍標準差',
                data: [],
                borderColor: 'rgba(255, 99, 132, 0.9)', // 增加顏色飽和度
                borderWidth: 2,
                borderDash: [2, 2],
                fill: false
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: false,
                    ticks: {
                        font: {
                            size: 12   // 保持刻度標籤的字體大小
                        },
                        callback: function(value) {
                            return value.toFixed(2);  // 顯示兩位小數
                        }
                    },
                    grid: {
                        color: 'rgba(0, 0, 0, 0.1)',  // 使網格線顏色更淺
                        drawBorder: true
                    }
                },
                x: {
                    ticks: {
                        maxRotation: 45,  // 斜角顯示標籤
                        minRotation: 45,
                        autoSkip: true,   // 自動跳過標籤以避免擁擠
                        maxTicksLimit: 8, // 減少標籤數量，避免擁擠
                        font: {
                            size: 11      // 調整字體大小
                        },
                        color: 'rgba(0, 0, 0, 0.7)' // 增加標籤顏色對比度
                    },
                    grid: {
                        display: false    // 隱藏X軸網格線，減少視覺干擾
                    }
                }
            },
            plugins: {
                title: {
                    display: true,
                    text: '價差走勢圖 (窗口大小: 加載中...)'
                },
                tooltip: {
                    mode: 'index',
                    intersect: false
                }
            },
            animation: {
                duration: 500  // 縮短動畫時間
            }
        }
    });
    
    // 初始化所有tooltips
    initTooltips();
}

// 導出為全局函數
window.initCharts = initCharts;

/**
 * 刷新圖表數據
 */
function refreshCharts() {
    // 獲取Z分數圖表數據
    $.ajax({
        url: '/api/charts/z-score',
        type: 'GET',
        success: function(response) {
            if (response.status === 'success') {
                // 設置ASCII圖表文本（簡化版顯示）
                $('#zScoreText').text(response.chart || '暫無數據');
                
                // 更新Chart.js圖表
                try {
                    if (response.chartData) {
                        let chartData;
                        // 檢查是否已經是對象還是需要解析的字符串
                        if (typeof response.chartData === 'string') {
                            chartData = JSON.parse(response.chartData);
                        } else {
                            chartData = response.chartData;
                        }
                        
                        // 添加調試信息到控制台
                        console.log('Z分數數據解析成功，數據點數量:', chartData.zScores ? chartData.zScores.length : 0);
                        
                        // 檢查圖表實例是否存在
                        if (!zScoreChart) {
                            initCharts(); // 如果不存在則初始化
                        } else {
                            updateZScoreChart(chartData); // 更新現有圖表
                        }
                    }
                } catch (e) {
                    console.error('解析Z分數圖表數據失敗', e);
                    console.log('數據內容:', response.chartData);
                }
            }
        },
        error: function(xhr, status, error) {
            console.error('獲取Z分數圖表數據失敗:', error);
        }
    });
    
    // 獲取價差圖表數據
    $.ajax({
        url: '/api/charts/spread',
        type: 'GET',
        success: function(response) {
            if (response.status === 'success') {
                // 設置ASCII圖表文本（簡化版顯示）
                $('#spreadText').text(response.chart || '暫無數據');
                
                // 更新Chart.js圖表
                try {
                    if (response.chartData) {
                        let chartData;
                        // 檢查是否已經是對象還是需要解析的字符串
                        if (typeof response.chartData === 'string') {
                            chartData = JSON.parse(response.chartData);
                        } else {
                            chartData = response.chartData;
                        }
                        
                        // 添加調試信息到控制台
                        console.log('價差數據解析成功，數據點數量:', chartData.spreads ? chartData.spreads.length : 0);
                        
                        // 檢查圖表實例是否存在
                        if (!spreadChart) {
                            initCharts(); // 如果不存在則初始化
                        } else {
                            updateSpreadChart(chartData); // 更新現有圖表
                        }
                    }
                } catch (e) {
                    console.error('解析價差圖表數據失敗', e);
                    console.log('數據內容:', response.chartData);
                }
            }
        },
        error: function(xhr, status, error) {
            console.error('獲取價差圖表數據失敗:', error);
        }
    });
}

// 導出為全局函數
window.refreshCharts = refreshCharts;

/**
 * 更新Z分數圖表
 * 
 * @param {Object} data 圖表數據
 */
function updateZScoreChart(data) {
    if (!data || !data.zScores || !data.labels) {
        return;
    }
    
    const zScores = data.zScores;
    const labels = data.labels;
    const entryThreshold = data.entryThreshold || 2.0;
    const exitThreshold = data.exitThreshold || 0.5;
    const latestZScore = data.latestZScore || 0;
    const suggestion = data.suggestion || '無建議';
    
    // 確保zScoreChart已初始化
    if (zScoreChart) {
        // 更新數據前先備份當前配置
        const currentOptions = { ...zScoreChart.options };
        
        // 更新數據
        zScoreChart.data.labels = labels;
        zScoreChart.data.datasets[0].data = zScores;
        
        // 更新閾值線
        const entryThresholdLine = Array(labels.length).fill(entryThreshold);
        const entryThresholdLowerLine = Array(labels.length).fill(-entryThreshold);
        const exitThresholdLine = Array(labels.length).fill(exitThreshold);
        const exitThresholdLowerLine = Array(labels.length).fill(-exitThreshold);
        const zeroLine = Array(labels.length).fill(0);
        
        zScoreChart.data.datasets[1].data = entryThresholdLine;
        zScoreChart.data.datasets[2].data = entryThresholdLowerLine;
        zScoreChart.data.datasets[3].data = exitThresholdLine;
        zScoreChart.data.datasets[4].data = exitThresholdLowerLine;
        zScoreChart.data.datasets[5].data = zeroLine;
        
        // 確保縮放設置更適合數據展示
        zScoreChart.options.scales.y.min = -5;
        zScoreChart.options.scales.y.max = 5;
        
        // 調整X軸刻度的可見性，確保時間標籤正確顯示
        if (labels.length > 20) {
            zScoreChart.options.scales.x.ticks.maxTicksLimit = 10;
        } else {
            zScoreChart.options.scales.x.ticks.maxTicksLimit = labels.length;
        }
        
        // 更新圖表標題
        zScoreChart.options.plugins.title.text = `Z分數走勢圖 (窗口大小: ${labels.length})`;
        
        // 更新右上角的Z分數信息
        if ($('#zScoreInfo').length) {
            let zScoreInfoHTML = `最新Z分數: <strong>${latestZScore.toFixed(3)}</strong> | 建議: <strong>${suggestion}</strong>`;
            
            // 如果未通過平穩度檢定，添加警告圖示
            if (window.backtestResult && window.backtestResult.stationaryTest === false) {
                zScoreInfoHTML += ` <i class="fas fa-exclamation-triangle text-warning" style="font-size: 0.9em;" data-bs-toggle="tooltip" data-bs-placement="top" title="未通過ADF 平穩度檢定，可能無法用 Z分數 做可靠的配對交易信號"></i>`;
            }
            
            $('#zScoreInfo').html(zScoreInfoHTML);
            
            // 初始化tooltip
            initTooltips();
        }
        
        // 更新交易建議信息區域
        if ($('#zScoreTips').length) {
            let alertClass = 'alert-info';
            let tradeTip = '';
            
            if (latestZScore > entryThreshold) {
                alertClass = 'alert-danger';
                tradeTip = `<strong>交易信號:</strong> Z分數 (${latestZScore.toFixed(3)}) > 入場閾值 (${entryThreshold})，建議做空資產1，做多資產2`;
            } else if (latestZScore < -entryThreshold) {
                alertClass = 'alert-danger';
                tradeTip = `<strong>交易信號:</strong> Z分數 (${latestZScore.toFixed(3)}) < 負入場閾值 (${-entryThreshold})，建議做多資產1，做空資產2`;
            } else if (Math.abs(latestZScore) < exitThreshold) {
                alertClass = 'alert-success';
                tradeTip = `<strong>交易信號:</strong> Z分數 (${latestZScore.toFixed(3)}) 在出場閾值 (±${exitThreshold}) 內，建議平倉所有倉位`;
            } else {
                alertClass = 'alert-warning';
                tradeTip = `<strong>無交易信號:</strong> Z分數 (${latestZScore.toFixed(3)}) 在觀望區間，建議持倉觀望`;
            }
            
            // 如果未通過平穩度檢定，添加警告訊息
            if (window.backtestResult && window.backtestResult.stationaryTest === false) {
                tradeTip += `<div class="mt-2"><i class="fas fa-exclamation-triangle text-warning"></i> <strong class="text-warning">警告：</strong> 此交易對未通過ADF平穩度檢定，Z分數可能不穩定，交易信號可能不可靠。</div>`;
            }
            
            $('#zScoreTips').removeClass('alert-info alert-warning alert-danger alert-success').addClass(alertClass);
            $('#zScoreTips').html(tradeTip);
        }
        
        // 更新圖表，但不自動縮放
        zScoreChart.update({
            duration: 500,
            easing: 'easeOutQuad'
        });
    }
}

// 導出為全局函數
window.updateZScoreChart = updateZScoreChart;

/**
 * 更新價差圖表
 * 
 * @param {Object} data 圖表數據
 */
function updateSpreadChart(data) {
    if (!data || !data.spreads || !data.labels) {
        return;
    }
    
    const spreads = data.spreads;
    const labels = data.labels;
    const spreadMean = data.spreadMean || 0;
    const spreadStd = data.spreadStd || 1;
    const latestSpread = data.latestSpread || 0;
    
    // 確保spreadChart已初始化
    if (spreadChart) {
        // 更新數據
        spreadChart.data.labels = labels;
        spreadChart.data.datasets[0].data = spreads;
        
        // 更新均值和標準差線
        const meanLine = Array(labels.length).fill(spreadMean);
        const upperStdLine = Array(labels.length).fill(spreadMean + spreadStd);
        const lowerStdLine = Array(labels.length).fill(spreadMean - spreadStd);
        const upper2StdLine = Array(labels.length).fill(spreadMean + 2 * spreadStd);
        const lower2StdLine = Array(labels.length).fill(spreadMean - 2 * spreadStd);
        
        spreadChart.data.datasets[1].data = meanLine;
        spreadChart.data.datasets[2].data = upperStdLine;
        spreadChart.data.datasets[3].data = lowerStdLine;
        spreadChart.data.datasets[4].data = upper2StdLine;
        spreadChart.data.datasets[5].data = lower2StdLine;
        
        // 調整X軸刻度的可見性，確保時間標籤正確顯示
        if (labels.length > 20) {
            spreadChart.options.scales.x.ticks.maxTicksLimit = 10;
        } else {
            spreadChart.options.scales.x.ticks.maxTicksLimit = labels.length;
        }
        
        // 更新圖表標題
        spreadChart.options.plugins.title.text = `價差走勢圖 (窗口大小: ${labels.length})`;
        
        // 更新右上角的價差信息
        if ($('#spreadInfo').length) {
            $('#spreadInfo').html(`當前價差: <strong>${latestSpread.toFixed(2)}</strong> | 均值: <strong>${spreadMean.toFixed(2)}</strong> | 標準差: <strong>${spreadStd.toFixed(2)}</strong>`);
        }
        
        // 更新價差分析信息區域
        if ($('#spreadTips').length) {
            // 計算當前價差的Z分數
            const currentZScore = (latestSpread - spreadMean) / spreadStd;
            
            // 更新價差分析提示
            let spreadAnalysis = `
                <strong>價差分析:</strong><br>
                價差 = 資產1價格 - Beta * 資產2價格<br>
                Z分數 = (當前價差 - 價差均值) / 價差標準差<br>
                當前價差 ${latestSpread.toFixed(2)} 相對於均值 ${spreadMean.toFixed(2)} ${latestSpread > spreadMean ? '偏高' : '偏低'} ${Math.abs(latestSpread - spreadMean).toFixed(2)} (${Math.abs(currentZScore).toFixed(2)} 個標準差)
            `;
            
            $('#spreadTips').html(spreadAnalysis);
        }
        
        // 動態調整Y軸範圍為均值±3倍標準差，確保所有重要數據都能顯示
        const buffer = spreadStd * 3.5; // 使用3.5倍標準差作為緩衝
        const yMin = spreadMean - buffer;
        const yMax = spreadMean + buffer;
        
        spreadChart.options.scales.y.min = yMin;
        spreadChart.options.scales.y.max = yMax;
        
        // 更新圖表
        spreadChart.update();
    }
}

// 導出為全局函數
window.updateSpreadChart = updateSpreadChart;

/**
 * 獲取當前窗口大小並填充表單
 */
function loadCurrentWindowSize() {
    $.ajax({
        url: '/api/trading-config/window-size',
        type: 'GET',
        success: function(response) {
            if (response.status === 'success') {
                // 設置輸入框的值
                $('#windowSizeInput').val(response.windowSize);
                
                // 更新圖表標題
                if (zScoreChart) {
                    zScoreChart.options.plugins.title.text = `Z分數走勢圖 (窗口大小: ${response.windowSize})`;
                    zScoreChart.update();
                }
                
                if (spreadChart) {
                    spreadChart.options.plugins.title.text = `價差走勢圖 (窗口大小: ${response.windowSize})`;
                    spreadChart.update();
                }
            } else {
                // 顯示錯誤消息
                showToast('error', '獲取窗口大小失敗: ' + response.message);
            }
        },
        error: function(xhr, status, error) {
            showToast('error', '獲取窗口大小時出錯: ' + error);
        }
    });
}

/**
 * 應用窗口大小設置
 */
function applyWindowSize() {
    // 獲取輸入值
    const windowSize = parseInt($('#windowSizeInput').val());
    
    // 驗證範圍
    if (isNaN(windowSize) || windowSize < 10 || windowSize > 1000) {
        showToast('error', '窗口大小必須在10到1000之間');
        return;
    }
    
    // 顯示正在處理的提示
    showToast('info', '正在設置窗口大小...');
    
    // 立即更新圖表標題以顯示處理中狀態
    if (zScoreChart) {
        zScoreChart.options.plugins.title.text = `Z分數走勢圖 (窗口大小: ${windowSize} - 處理中...)`;
        zScoreChart.update();
    }
    
    if (spreadChart) {
        spreadChart.options.plugins.title.text = `價差走勢圖 (窗口大小: ${windowSize} - 處理中...)`;
        spreadChart.update();
    }
    
    // 發送設置請求
    $.ajax({
        url: '/api/trading-config/set-window-size',
        type: 'GET',
        data: {
            windowSize: windowSize
        },
        success: function(response) {
            if (response.status === 'success') {
                // 顯示成功消息
                showToast('success', response.message);
                
                // 重新加載圖表
                refreshPage();
            } else {
                // 顯示錯誤消息
                showToast('error', response.message);
                
                // 恢復圖表標題
                if (zScoreChart) {
                    zScoreChart.options.plugins.title.text = `Z分數走勢圖 (窗口大小: 加載中...)`;
                    zScoreChart.update();
                }
                
                if (spreadChart) {
                    spreadChart.options.plugins.title.text = `價差走勢圖 (窗口大小: 加載中...)`;
                    spreadChart.update();
                }
                
                // 重新獲取當前窗口大小
                loadCurrentWindowSize();
            }
        },
        error: function(xhr, status, error) {
            showToast('error', '設置窗口大小時出錯: ' + error);
            
            // 恢復圖表標題
            if (zScoreChart) {
                zScoreChart.options.plugins.title.text = `Z分數走勢圖 (窗口大小: 加載中...)`;
                zScoreChart.update();
            }
            
            if (spreadChart) {
                spreadChart.options.plugins.title.text = `價差走勢圖 (窗口大小: 加載中...)`;
                spreadChart.update();
            }
            
            // 重新獲取當前窗口大小
            loadCurrentWindowSize();
        }
    });
}

/**
 * 初始化 Bootstrap 的 tooltip 功能
 */
function initTooltips() {
    // 尋找並初始化所有帶有 data-bs-toggle="tooltip" 屬性的元素
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
}