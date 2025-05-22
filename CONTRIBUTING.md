# 貢獻指南

感謝您對「加密貨幣配對交易機器人」專案的關注！我們非常歡迎社區成員的貢獻，使這個專案變得更好。以下是參與貢獻的指南，希望能幫助您順利開始。

## 授權聲明

請注意，本專案採用 [Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0) 授權協議](LICENSE)。任何提交到本專案的貢獻都將遵循相同的授權條款，即允許分享和修改，但禁止用於商業目的。請確保您了解並接受這一點。

## 貢獻流程

### 問題報告

如果您發現Bug或有新功能請求：

1. 在提交新問題前，請先檢查現有的Issues，避免重複
2. 使用Issue模板填寫報告，提供盡可能詳細的信息，包括：
   - 問題的清晰描述
   - 重現步驟（如適用）
   - 預期行為與實際行為
   - 環境信息（如Java版本、操作系統等）
   - 相關日誌或錯誤信息截圖

### 代碼貢獻

如果您希望貢獻代碼：

1. Fork本儲存庫到您的GitHub帳戶
2. 基於`master`分支創建新的功能分支：`git checkout -b feature/your-feature-name`
3. 提交您的更改：`git commit -m 'Add some feature'`
4. 推送到您的Fork：`git push origin feature/your-feature-name`
5. 創建Pull Request，描述您所做的更改及其目的

### Pull Request審核

提交PR後，請耐心等待審核。審核過程中可能會要求您：
- 修正代碼風格問題
- 添加測試覆蓋
- 澄清或解釋特定的實現決策
- 更新文檔以反映您的更改

## 開發環境設置

### 系統要求

- Java 17或更高版本
- Maven 3.6或更高版本
- 幣安API密鑰（建議使用測試網）

### 環境配置步驟

1. 克隆儲存庫：
   ```bash
   git clone https://github.com/yourusername/pairstrading.git
   cd pairstrading
   ```

2. 編譯專案：
   ```bash
   mvn clean install
   ```

3. 運行測試：
   ```bash
   mvn test
   ```

4. 啟動應用程序：
   ```bash
   mvn spring-boot:run
   ```

## 代碼風格指南

本專案遵循以下代碼風格規範：

### Java編碼規範

- 遵循Google Java風格指南
- 使用4個空格縮進（不使用Tab）
- 方法和類應有明確的Javadoc註釋
- 變量和方法名使用camelCase
- 類名使用PascalCase
- 常量使用UPPER_SNAKE_CASE
- 保持單一職責原則和其他SOLID設計原則

### 提交信息規範

提交信息應遵循以下格式：
```
<類型>: <簡短描述>

<詳細描述>

<關聯的Issue編號>
```

類型可以是：
- `feat`：新功能
- `fix`：錯誤修復
- `docs`：文檔更改
- `style`：不影響代碼含義的更改（空格、格式等）
- `refactor`：既不修復錯誤也不添加功能的代碼更改
- `perf`：提高性能的代碼更改
- `test`：添加或修正測試
- `chore`：對構建過程或輔助工具的更改

## 測試要求

- 所有新功能必須包含適當的單元測試
- 修復Bug時應該添加測試來防止回歸
- 測試覆蓋率應保持在80%以上
- 運行測試：`mvn test`

## 文檔要求

- 重要功能應有相應的API文檔
- 更新代碼時請同步更新相關文檔
- 添加新配置選項時，確保在README或相關配置文檔中解釋其用途

## 分支管理策略

- `master`分支保持穩定，隨時可發布
- 功能開發在`feature/*`分支上進行
- Bug修復在`fix/*`分支上進行
- 發布準備在`release/*`分支上進行

## 行為準則

我們期望所有參與者尊重彼此，共同創造一個積極、包容的社區環境：

- 使用友善、包容的語言
- 尊重不同的觀點和經驗
- 接受建設性的批評
- 專注於對社區最有利的事情
- 對其他社區成員表示同理心

## 聯繫方式

如有任何問題或建議，請通過以下方式聯繫我們：

- 創建GitHub Issue
- 發送電子郵件到：andy19910823@gmail.com

感謝您的貢獻！
