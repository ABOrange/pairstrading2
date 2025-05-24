/**
 * 配對交易機器人儀表板 - 總覽頁面 JavaScript
 */

// 頁面載入完成後執行初始化
$(document).ready(function() {
    // 初始化事件監聽
    initEventListeners();
    
    // 初始加載數據
    refreshPage();
});

/**
 * 初始化事件監聽
 */
function initEventListeners() {
    // 刷新總覽
    $('#refreshOverviewBtn').on('click', function() {
        refreshPage();
        resetRefreshCounter();
    });
}

/**
 * 刷新頁面數據 (覆蓋全局 refreshPage 函數)
 */
function refreshPage() {
    refreshOverview();
    
    // 獲取當前窗口大小
    $.ajax({
        url: '/api/trading-config/window-size',
        type: 'GET',
        success: function(response) {
            if (response.status === 'success') {
                $('#windowSize').text(response.windowSize);
            }
        }
    });
}

/**
 * 刷新總覽數據
 */
function refreshOverview() {
    // 獲取交易信號報告
    $.ajax({
        url: '/api/charts/signal-report',
        type: 'GET',
        success: function(response) {
            if (response.status === 'success') {
                $('#signalReport').text(response.report || '暫無數據');
            }
        }
    });
    
    // 獲取最新價格
    $.ajax({
        url: '/api/market/prices',
        type: 'GET',
        success: function(response) {
            if (response.status === 'success' && response.prices) {
                const prices = response.prices;
                
                // 輸出調試信息，幫助診斷問題
                console.log("API 返回的價格數據:", prices);
                console.log("資產1標題:", $('#asset1Price').parent().find('.card-title').text().trim());
                console.log("資產2標題:", $('#asset2Price').parent().find('.card-title').text().trim());
                
                // 更新頁面上的價格顯示
                for (const [symbol, price] of Object.entries(prices)) {
                    // 直接使用 trim() 移除可能的空白字符
                    const symbolTrimmed = symbol.trim();
                    const asset1Title = $('#asset1Price').parent().find('.card-title').text().trim();
                    const asset2Title = $('#asset2Price').parent().find('.card-title').text().trim();
                    
                    // 輸出每次比較的信息
                    console.log(`比較: "${symbolTrimmed}" vs 資產1: "${asset1Title}" 和 資產2: "${asset2Title}"`);
                    
                    if (symbolTrimmed === asset1Title) {
                        const priceDisplay = parseFloat(price).toFixed(2);
                        console.log(`更新資產1(${asset1Title})價格為: ${priceDisplay}`);
                        $('#asset1Price').text(priceDisplay);
                    }
                    
                    if (symbolTrimmed === asset2Title) {
                        const priceDisplay = parseFloat(price).toFixed(2);
                        console.log(`更新資產2(${asset2Title})價格為: ${priceDisplay}`);
                        $('#asset2Price').text(priceDisplay);
                    }
                }
                
                // 添加一個後備機制，直接由資產名稱匹配
                if (prices['BTCUSDT'] && $('#asset1Price').parent().find('.card-title').text().includes('BTC')) {
                    $('#asset1Price').text(parseFloat(prices['BTCUSDT']).toFixed(2));
                    console.log("使用後備機制更新BTC價格");
                }
                
                if (prices['ETHUSDT'] && $('#asset2Price').parent().find('.card-title').text().includes('ETH')) {
                    $('#asset2Price').text(parseFloat(prices['ETHUSDT']).toFixed(2));
                    console.log("使用後備機制更新ETH價格");
                }
            } else {
                console.error("獲取價格失敗:", response);
            }
        },
        error: function(error) {
            console.error("價格API錯誤:", error);
        }
    });
    
    // 獲取持倉數據
    $.ajax({
        url: '/api/market/positions',
        type: 'GET',
        success: function(response) {
            if (response.status === 'success') {
                const positionsTableBody = $('#positionsTableBody');
                positionsTableBody.empty();
                
                if (response.positions && response.positions.length > 0) {
                    // 過濾出有持倉的數據
                    const activePositions = response.positions.filter(p => 
                        parseFloat(p.positionAmt) !== 0);
                    
                    if (activePositions.length > 0) {
                        activePositions.forEach(function(position) {
                            const row = $('<tr></tr>');
                            row.append($('<td></td>').text(position.symbol));
                            row.append($('<td></t:"0.0","markPrice":"0.00000000","unRealizedProfit":"0.00000000","liquidationPrice":"0","leverage":"5","maxNotionalValue":"3000000","marginType":"cross","isolatedMargin":"0.00000000","isAutoAddMargin":"false","positionSide":"BOTH","notional":"0","isolatedWallet":"0","updateTime":0,"isolated":false,"adlQuantile":0},{"symbol":"EPICUSDT","positionAmt":"0.0","entryPrice":"0.0","breakEvenPrice":"0.0","markPrice":"0.00000000","unRealizedProfit":"0.00000000","liquidationPrice":"0","leverage":"5","maxNotionalValue":"250000","marginType":"cross","isolatedMargin":"0.00000000","isAutoAddMargin":"false","positionSide":"BOTH","notional":"0","isolatedWallet":"0","updateTime":0,"isolated":false,"adlQuantile":0},{"symbol":"POLUSDT","positionAmt":"0","entryPrice":"0.0","breakEvenPrice":"0.0","markPrice":"0.00000000","unRealizedProfit":"0.00000000","liquidationPrice":"0","leverage":"5","maxNotionalValue":"750000","marginType":"cross","isolatedMard>').text(parseFloat(position.positionAmt) > 0 ? '多' : '空'));
                            row.append($('<td></td>').text(Math.abs(position.positionAmt)));

                            // 根據盈虧添加顏色
                            const profitTd = $('<td></td>').text(parseFloat(position.unrealizedProfit).toFixed(2));
                            if (parseFloat(position.unrealizedProfit) > 0) {
                                profitTd.addClass('text-success');
                            } else if (parseFloat(position.unrealizedProfit) < 0) {
                                profitTd.addClass('text-danger');
                            }
                            row.append(profitTd);

                            // Unrealized Profit Percentage with "-" check
                            const percentageTd = $('<td></td>').text(position.unrealizedProfitPercentage);
                            if (position.unrealizedProfitPercentage.includes('-')) {
                                percentageTd.addClass('text-danger'); // Negative percentage
                            } else {
                                percentageTd.addClass('text-success'); // Positive percentage
                            }
                            row.append(percentageTd);
                            
                            positionsTableBody.append(row);
                        });
                    } else {
                        positionsTableBody.append('<tr><td colspan="4">暫無持倉</td></tr>');
                    }
                } else {
                    positionsTableBody.append('<tr><td colspan="4">暫無持倉</td></tr>');
                }
            }
        }
    });
}