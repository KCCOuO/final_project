# 72-Hour Emergency Surgery Prediction System

本專案為「急診手術需求預測系統」(72-Hour Surgical Prediction)，提供一站式的資料前處理、機器學習模型訓練與病患預測工作流程。透過分析病患入院時的臨床資料，預測其未來 72 小時內是否需要接受手術治療，以輔助醫療人員進行更精準的醫療決策與資源分配。

## 系統架構與技術選型

本系統採多層次架構設計，結合現代化桌面端技術與強大的機器學習引擎：

- **核心語言**：Java 22 (建議 Java 17+) / Python 3
- **機器學習引擎**：Weka 3.8.6 (Java) / Scikit-learn (Python)
- **桌面使用者介面**：JavaFX 17 (Java) / Tkinter (Python)
- **後端與依賴管理**：Spring Boot 3.2.3, Maven
- **資料處理**：Pandas, NumPy (Python)

## 核心功能模組

### 1. 資料前處理管線 (Data Preprocessing Pipeline)
由 Python 撰寫的資料處理模組（包含 CLI 與 GUI 版本 `data_preprocessing_gui.py`），提供強大且穩健的臨床資料標準化流程：
- 支援 CSV 與 Excel 格式匯入。
- 自動清理缺失值、標準化資料格式、移除不必要欄位（避免未來資訊 Data Leakage）。
- 針對重要指標進行二元數值轉換（N/Y）。
- 針對目標科別（如 ESUR）進行自動篩選，並以 80/20 比例進行 Stratified Train/Test Split 分層抽樣分割。

### 2. 機器學習與模型評估 (Weka ML Engine)
系統基於 Java/Spring Boot 與 Weka 引擎，支援多種經典演算法：
- **決策樹 (J48 / C4.5)**：高可解釋性，臨床常用首選。
- **隨機森林 (Random Forest)**：具備高度預測穩定性與泛化能力。
- **邏輯迴歸 (Logistic Regression)**：可分析臨床特徵權重。
- **模型評估**：提供 10-fold 交叉驗證 (Cross-validation)、訓練集評估與百分比拆分測試。專注於臨床上最關鍵的 **Recall (召回率)** 指標，確保能有效偵測出需要手術的高風險病患。

### 3. 模型預測與防護機制
- **單筆與批次預測**：支援透過介面手動輸入單筆病患資料進行預測，或匯入大量 CSV 進行批次預測，並提供模型信心度 (Confidence)。
- **Anti-OOM 保護**：自動偵測並移除高基數字串欄位（如病患 ID），防止記憶體溢出。
- **模型持久化**：支援匯出與載入訓練好的模型 (.zip)，解決 Weka 反序列化目標特徵遺失的問題。

## 執行與使用方式

### 資料前處理 (Python)
確保已安裝必要的 Python 套件：
```bash
pip install pandas numpy scikit-learn
```
啟動前處理 GUI 介面：
```bash
python data_preprocessing_gui.py
```

### 系統主程式 (Java)
使用 Maven 進行編譯與打包：
```bash
mvn clean install
```
透過 Spring Boot 或 JavaFX Maven Plugin 啟動：
```bash
mvn spring-boot:run
```
或執行編譯後的 JAR 檔啟動桌面應用程式。

## 專案目錄結構

- `/src/main/java/com/medical/ml/`：Java 主程式與核心邏輯（Service, ML, UI Controllers）。
- `/dataset/`：資料集存放目錄（包含原始資料與處理後資料）。
- `data_preprocessing_pipeline.py`：Python 前處理腳本（CLI）。
- `data_preprocessing_gui.py`：Python 前處理視覺化介面（GUI）。
- `pom.xml`：Maven 專案設定檔。

## 臨床應用聲明
本系統定位為決策輔助工具，預測結果（包含信心度）應視為臨床參考指標之一，**不可取代專業醫師的最終臨床診斷與決策**。
