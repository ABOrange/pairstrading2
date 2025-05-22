/**
 * 交易對回測詳情頁面 JavaScript
 */

$(document).ready(function() {
    // 初始化頁面
    initPage();
    
    // 初始化 tooltip
    initTooltips();
});

/**
 * 初始化頁面
 */
function initPage() {
    // 檢查 stationaryTest 屬性，如果為 false，在信號強度後添加警告圖示
    if (window.backtestResult && window.backtestResult.stationaryTest === false) {
        // 找到信號強度的元素
        const ratingElement = $('.alert p:contains("信號強度:")');
        if (ratingElement.length > 0) {
            // 獲取當前的信號強度文本
            const ratingText = ratingElement.html();
            
            // 在信號強度後添加警告圖示，包含 tooltip
            const newRatingText = ratingText.replace('</span>', ' <i class="fas fa-exclamation-triangle text-warning" style="font-size: 0.9em;" data-bs-toggle="tooltip" data-bs-placement="top" title="未通過ADF 平穩度檢定，可能無法用 Z分數 做可靠的配對交易信號"></i></span>');
            
            // 更新元素內容
            ratingElement.html(newRatingText);
            
            // 重新初始化 tooltip
            initTooltips();
        }
    }
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
