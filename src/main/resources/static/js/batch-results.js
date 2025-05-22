$(document).ready(function() {
    // 儲存所有結果數據
    const allResults = {};
    
    // 初始化 tooltips
    initTooltips();
    
    // 初始化，加載所有交易對的回測數據
    loadAllPairsData();
    
    // 搜索功能
    $('#searchBtn').on('click', function() {
        const searchText = $('#searchInput').val().toLowerCase();
        filterTableRows(searchText);
    });
    
    $('#searchInput').on('keyup', function(e) {
        if (e.keyCode === 13) {
            const searchText = $(this).val().toLowerCase();
            filterTableRows(searchText);
        }
    });
    
    // 過濾按鈕
    $('.btn-group button[data-filter]').on('click', function() {
        const filter = $(this).data('filter');
        
        // 設置按鈕高亮
        $('.btn-group button[data-filter]').removeClass('active');
        $(this).addClass('active');
        
        filterTableByType(filter);
    });
    
    // 排序按鈕
    $('.btn-group button[data-sort]').on('click', function() {
        const sort = $(this).data('sort');
        
        // 設置按鈕高亮
        $('.btn-group button[data-sort]').removeClass('active');
        $(this).addClass('active');
        
        sortTableRows(sort);
    });
    
    // 查看詳情按鈕
    $(document).on('click', '.view-detail-btn', function() {
        const combination = $(this).data('combination');
        if (combination) {
            const pairs = combination.split(',');
            if (pairs.length === 2) {
                window.location.href = '/backtest/details?symbol1=' + pairs[0] + '&symbol2=' + pairs[1];
            }
        }
    });
    
    // 刪除交易對組合按鈕
    $(document).on('click', '.delete-pair-btn', function() {
        const combination = $(this).data('combination');
        
        if (confirm('確定要刪除交易對組合 ' + combination + ' 嗎？')) {
            $.ajax({
                url: '/backtest/api/delete-combination',
                type: 'GET',
                data: {
                    combination: combination
                },
                success: function(response) {
                    if (response.status === 'success') {
                        // 重新加載頁面
                        window.location.reload();
                    } else {
                        // 顯示錯誤消息
                        $('#toastTitle').text('錯誤');
                        $('#toastMessage').text(response.message);
                        var toast = new bootstrap.Toast(document.getElementById('resultToast'));
                        toast.show();
                    }
                },
                error: function(xhr, status, error) {
                    // 顯示錯誤消息
                    $('#toastTitle').text('錯誤');
                    $('#toastMessage').text('刪除交易對組合失敗：' + error);
                    var toast = new bootstrap.Toast(document.getElementById('resultToast'));
                    toast.show();
                }
            });
        }
    });
    
    // 重新整理所有數據按鈕
    $('#refreshAllBtn').on('click', function() {
        // 顯示處理中提示
        $('#toastTitle').text('處理中');
        $('#toastMessage').text('正在重新整理所有回測數據...');
        var toast = new bootstrap.Toast(document.getElementById('resultToast'));
        toast.show();
        
        // 重置數據
        $('.loading-data').html('<div class="spinner-border spinner-border-sm text-primary" role="status"><span class="visually-hidden">Loading...</span></div>');
        
        // 重新加載數據
        loadAllPairsData();
    });
    
    // 加載所有交易對的回測數據
    function loadAllPairsData() {
        $('.result-row').each(function() {
            const combination = $(this).find('.view-detail-btn').data('combination');
            if (combination) {
                loadPairData(combination);
            }
        });
    }
    
    // 加載單個交易對的回測數據
    function loadPairData(combination) {
        const pairs = combination.split(',');
        if (pairs.length !== 2) return;
        
        $.ajax({
            url: '/backtest/api/backtest-pair',
            type: 'GET',
            data: {
                symbol1: pairs[0],
                symbol2: pairs[1],
                // 不指定 days 參數，讓後端使用系統窗口大小計算
                // interval: "1h" // 預設使用1h間隔
            },
            success: function(response) {
                if (response.status === 'success') {
                    // 輸出結果至 console 以檢查 stationaryTest 屬性
                    console.log('API response for ' + combination + ':', response.result);
                    
                    // 保存結果數據
                    allResults[combination] = response.result;
                    
                    // 更新表格數據
                    updateTableData(combination, response.result);
                    
                    // 更新統計和推薦
                    updateStatisticsAndRecommendations();
                    
                    // 初始化 tooltip
                    initTooltips();
                } else {
                    // 更新表格顯示錯誤
                    $('td.loading-data[data-combination="' + combination + '"]').html('<span class="text-danger">加載失敗</span>');
                }
            },
            error: function(xhr, status, error) {
                // 更新表格顯示錯誤
                $('td.loading-data[data-combination="' + combination + '"]').html('<span class="text-danger">加載失敗</span>');
            }
        });
    }
    
    // 更新表格數據
    function updateTableData(combination, result) {
        // 更新相關性
        $('td.loading-data[data-combination="' + combination + '"][data-type="correlation"]').html(
            '<span class="' + getCorrelationClass(result.correlation) + '">' + result.correlation.toFixed(4) + '</span>'
        );
        
        // 更新Z分數 (支持zScore或zscore命名)
        $('td.loading-data[data-combination="' + combination + '"][data-type="zscore"]').html(
            '<span class="' + getZScoreClass(result.zScore || result.zscore) + '">' + 
            (result.zScore !== undefined ? result.zScore.toFixed(4) : 
             result.zscore !== undefined ? result.zscore.toFixed(4) : '0.0000') + '</span>'
        );
        
        // 更新信號
        $('td.loading-data[data-combination="' + combination + '"][data-type="signal"]').html(
            '<span class="' + getSignalClass(result.signalType) + '">' + getSignalTypeText(result.signalType, result.asset1, result.asset2) + '</span>'
        );
        
        // 更新強度
        let ratingHtml = '<span class="' + getRatingClass(result.signalRating) + '">' + result.signalRating;
        
        // 使用黃色的小圖示驚嘆號
        if (result.stationaryTest === false) {
            ratingHtml += ' <i class="fas fa-exclamation-triangle text-warning" style="font-size: 0.9em;" data-bs-toggle="tooltip" data-bs-placement="top" title="未通過ADF 平穩度檢定，可能無法用 Z分數 做可靠的配對交易信號"></i>';
        }
        
        ratingHtml += '</span>';
        
        $('td.loading-data[data-combination="' + combination + '"][data-type="rating"]').html(ratingHtml);
        
        // 更新價差
        $('td.loading-data[data-combination="' + combination + '"][data-type="spread"]').html(
            result.spread.toFixed(2) + ' / ' + result.spreadMean.toFixed(2)
        );
        
        // 更新套利次數
        $('td.loading-data[data-combination="' + combination + '"][data-type="arbitrage"]').html(
            '<span class="' + getArbitrageClass(result.arbitrageCount) + '">' + 
            (result.arbitrageCount !== undefined ? result.arbitrageCount : '0') + '</span>'
        );
        
        // 更新爆倉次數
        $('td.loading-data[data-combination="' + combination + '"][data-type="liquidation"]').html(
            '<span class="' + getLiquidationClass(result.liquidationCount) + '">' + 
            (result.liquidationCount !== undefined ? result.liquidationCount : '0') + '</span>'
        );
    }
    
    // 更新統計和推薦
    function updateStatisticsAndRecommendations() {
        // 檢查是否所有數據都已加載
        const allLoaded = $('.loading-data .spinner-border').length === 0;
        
        if (allLoaded) {
            // 計算統計數據
            const stats = calculateStatistics();
            
            // 更新統計區域
            updateStatistics(stats);
            
            // 更新推薦區域
            updateRecommendations(stats);
        }
    }
    
    // 計算統計數據
    function calculateStatistics() {
        const stats = {
            total: Object.keys(allResults).length,
            highCorrelation: 0,
            strongSignal: 0,
            hasSignal: 0,
            avgCorrelation: 0,
            avgZScore: 0,
            bestPairs: []
        };
        
        let totalCorr = 0;
        let totalZScoreAbs = 0;
        
        // 計算各類統計
        for (const combination in allResults) {
            const result = allResults[combination];
            
            // 高相關性 (>0.7)
            if (Math.abs(result.correlation) > 0.7) {
                stats.highCorrelation++;
            }
            
            // 強信號 (Z分數 > 2.0 或 < -2.0)
            if (Math.abs(result.zScore || result.zscore || 0) > 2.0) {
                stats.strongSignal++;
            }
            
            // 有信號 (不是NO_SIGNAL)
            if (result.signalType !== 'NO_SIGNAL') {
                stats.hasSignal++;
            }
            
            // 累計相關性和Z分數
            totalCorr += Math.abs(result.correlation);
            totalZScoreAbs += Math.abs(result.zScore || result.zscore || 0);
            
            // 添加到候選推薦列表
            stats.bestPairs.push({
                combination: combination,
                asset1: result.asset1,
                asset2: result.asset2,
                correlation: Math.abs(result.correlation),
                zScore: Math.abs(result.zScore || result.zscore || 0), // 統一處理 zScore 或 zscore
                signalType: result.signalType,
                signalRating: result.signalRating,
                stationaryTest: result.stationaryTest, // 保存平穩度檢定結果
                score: Math.abs(result.correlation) * Math.abs(result.zScore || result.zscore || 0)  // 綜合得分
            });
        }
        
        // 計算均值
        if (stats.total > 0) {
            stats.avgCorrelation = totalCorr / stats.total;
            stats.avgZScore = totalZScoreAbs / stats.total;
        }
        
        // 按綜合得分排序最佳交易對
        stats.bestPairs.sort((a, b) => b.score - a.score);
        
        return stats;
    }
    
    // 更新統計顯示
    function updateStatistics(stats) {
        const statsHtml = `
            <div class="row">
                <div class="col-md-6">
                    <ul class="list-group">
                        <li class="list-group-item d-flex justify-content-between align-items-center">
                            總回測交易對
                            <span class="badge bg-primary rounded-pill">${stats.total}</span>
                        </li>
                        <li class="list-group-item d-flex justify-content-between align-items-center">
                            高相關性交易對 (>0.7)
                            <span class="badge bg-success rounded-pill">${stats.highCorrelation}</span>
                        </li>
                        <li class="list-group-item d-flex justify-content-between align-items-center">
                            強信號交易對
                            <span class="badge bg-danger rounded-pill">${stats.strongSignal}</span>
                        </li>
                    </ul>
                </div>
                <div class="col-md-6">
                    <ul class="list-group">
                        <li class="list-group-item d-flex justify-content-between align-items-center">
                            有交易信號的交易對
                            <span class="badge bg-warning rounded-pill">${stats.hasSignal}</span>
                        </li>
                        <li class="list-group-item d-flex justify-content-between align-items-center">
                            平均相關性
                            <span class="badge bg-info rounded-pill">${stats.avgCorrelation.toFixed(4)}</span>
                        </li>
                        <li class="list-group-item d-flex justify-content-between align-items-center">
                            平均|Z分數|
                            <span class="badge bg-info rounded-pill">${stats.avgZScore.toFixed(4)}</span>
                        </li>
                    </ul>
                </div>
            </div>
        `;
        
        $('#statistics').html(statsHtml);
    }
    
    // 更新推薦顯示
    function updateRecommendations(stats) {
        // 最多顯示5個推薦
        const topPairs = stats.bestPairs.slice(0, 5);
        
        if (topPairs.length === 0) {
            $('#recommendations').html('<p>沒有足夠的數據進行推薦</p>');
            return;
        }
        
        let recommendHtml = '<div class="list-group">';
        
        topPairs.forEach((pair, index) => {
            const signalClass = getSignalClass(pair.signalType);
            const ratingClass = getRatingClass(pair.signalRating);
            
            recommendHtml += `
                <a href="/backtest/details?symbol1=${pair.asset1}&symbol2=${pair.asset2}" class="list-group-item list-group-item-action">
                    <div class="d-flex w-100 justify-content-between">
                        <h5 class="mb-1">${index + 1}. ${pair.asset1} + ${pair.asset2}</h5>
                        <small class="${ratingClass}">
                            ${pair.signalRating}
                            ${pair.stationaryTest === false ? 
                              '<i class="fas fa-exclamation-triangle text-warning" style="font-size: 0.9em;" data-bs-toggle="tooltip" data-bs-placement="top" title="未通過ADF 平穩度檢定，可能無法用 Z分數 做可靠的配對交易信號"></i>' : 
                              ''}
                        </small>
                    </div>
                    <p class="mb-1">
                        相關性: <strong>${pair.correlation.toFixed(4)}</strong> | 
                        |Z分數|: <strong>${isNaN(pair.zScore) ? '0.0000' : pair.zScore.toFixed(4)}</strong>
                    </p>
                    <small class="${signalClass}">${getSignalTypeText(pair.signalType, pair.asset1, pair.asset2)}</small>
                </a>
            `;
        });
        
        recommendHtml += '</div>';
        $('#recommendations').html(recommendHtml);
        
        // 初始化推薦卡片中的 tooltip
        initTooltips();
    }
    
    // 表格行過濾器
    function filterTableRows(searchText) {
        $('.result-row').each(function() {
            const rowText = $(this).text().toLowerCase();
            if (rowText.includes(searchText)) {
                $(this).show();
            } else {
                $(this).hide();
            }
        });
    }
    
    // 按類型過濾表格
    function filterTableByType(filterType) {
        $('.result-row').show(); // 先顯示所有行
        
        switch(filterType) {
            case 'high-corr':
                $('.result-row').each(function() {
                    const combination = $(this).find('.view-detail-btn').data('combination');
                    if (allResults[combination] && Math.abs(allResults[combination].correlation) <= 0.7) {
                        $(this).hide();
                    }
                });
                break;
            case 'strong-signal':
                $('.result-row').each(function() {
                    const combination = $(this).find('.view-detail-btn').data('combination');
                    if (allResults[combination] && Math.abs(allResults[combination].zScore) <= 2.0) {
                        $(this).hide();
                    }
                });
                break;
            case 'has-signal':
                $('.result-row').each(function() {
                    const combination = $(this).find('.view-detail-btn').data('combination');
                    if (allResults[combination] && allResults[combination].signalType === 'NO_SIGNAL') {
                        $(this).hide();
                    }
                });
                break;
            case 'all':
            default:
                // 顯示所有行
                break;
        }
    }
    
    // 排序表格行
    function sortTableRows(sortType) {
        const rows = $('#batchResultTable tbody tr').get();
        
        rows.sort(function(a, b) {
            const combinationA = $(a).find('.view-detail-btn').data('combination');
            const combinationB = $(b).find('.view-detail-btn').data('combination');
            const resultA = allResults[combinationA];
            const resultB = allResults[combinationB];
            
            // 檢查是否有結果數據
            if (!resultA && !resultB) return 0;
            if (!resultA) return 1;
            if (!resultB) return -1;
            
            switch(sortType) {
                case 'corr-desc':
                    return Math.abs(resultB.correlation) - Math.abs(resultA.correlation);
                case 'zscore-abs-desc':
                    return Math.abs(resultB.zScore) - Math.abs(resultA.zScore);
                case 'pair-asc':
                    return combinationA.localeCompare(combinationB);
                default:
                    return 0;
            }
        });
        
        // 重新添加排序後的行到表格
        $.each(rows, function(index, row) {
            $('#batchResultTable tbody').append(row);
        });
    }
    
    // 獲取相關性對應的CSS類
    function getCorrelationClass(correlation) {
        const abs = Math.abs(correlation);
        if (abs > 0.9) return 'text-success fw-bold';
        if (abs > 0.8) return 'text-success';
        if (abs > 0.7) return 'text-success';
        if (abs > 0.5) return 'text-warning';
        return 'text-danger';
    }
    
    // 獲取Z分數對應的CSS類
    function getZScoreClass(zScore) {
        const abs = Math.abs(zScore);
        if (abs > 3.0) return 'text-danger fw-bold';
        if (abs > 2.5) return 'text-danger';
        if (abs > 2.0) return 'text-warning fw-bold';
        if (abs > 1.5) return 'text-warning';
        if (abs > 1.0) return 'text-info';
        return 'text-muted';
    }
    
    // 獲取信號類型對應的CSS類
    function getSignalClass(signalType) {
        switch (signalType) {
            case 'LONG_ASSET1_SHORT_ASSET2':
            case 'SHORT_ASSET1_LONG_ASSET2':
                return 'text-danger';
            case 'CLOSE_POSITIONS':
                return 'text-success';
            case 'NO_SIGNAL':
            default:
                return 'text-muted';
        }
    }
    
    // 獲取信號強度對應的CSS類
    function getRatingClass(rating) {
        switch (rating) {
            case '極強':
                return 'text-danger fw-bold';
            case '很強':
                return 'text-danger';
            case '強':
                return 'text-warning fw-bold';
            case '中等':
                return 'text-warning';
            case '弱':
                return 'text-info';
            case '很弱':
            default:
                return 'text-muted';
        }
    }
    
    // 獲取信號類型文字描述
    function getSignalTypeText(signalType, asset1, asset2) {
        switch (signalType) {
            case 'LONG_ASSET1_SHORT_ASSET2':
                return '做多 ' + asset1 + ', 做空 ' + asset2;
            case 'SHORT_ASSET1_LONG_ASSET2':
                return '做空 ' + asset1 + ', 做多 ' + asset2;
            case 'CLOSE_POSITIONS':
                return '平倉';
            case 'NO_SIGNAL':
            default:
                return '觀望';
        }
    }

    // 獲取套利次數對應的CSS類
    function getArbitrageClass(count) {
        if (count > 10) return 'text-success fw-bold';
        if (count > 5) return 'text-success';
        if (count > 0) return 'text-info';
        return 'text-muted';
    }
    
    // 獲取爆倉次數對應的CSS類
    function getLiquidationClass(count) {
        if (count > 5) return 'text-danger fw-bold';
        if (count > 2) return 'text-danger';
        if (count > 0) return 'text-warning';
        return 'text-success';
    }
    
    // 初始化 Bootstrap 的 tooltip 功能
    function initTooltips() {
        // 尋找並初始化所有帶有 data-bs-toggle="tooltip" 屬性的元素
        const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
        tooltipTriggerList.map(function (tooltipTriggerEl) {
            return new bootstrap.Tooltip(tooltipTriggerEl);
        });
    }
});