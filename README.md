# 急診手術需求預測系統 (72-Hour Surgical Prediction)

使用 **Java 22 + Spring Boot 3.2 + JavaFX 17 + Weka 3.8.6**，根據病患到院時的臨床資料，預測其 **3 天（72 小時）內是否需要手術**。

提供完整的 **資料前處理 → 模型訓練 → 病患預測** 一站式 GUI 工作流程，專為醫護人員設計，介面全繁體中文。

---

## 專案結構

```
72hourPrediction/
├── pom.xml                                         ← Maven 相依設定
├── README.md
└── src/main/
    ├── java/com/medical/ml/
    │   ├── Launcher.java                           ← 程式進入點
    │   ├── JavaFXApplication.java                  ← JavaFX Application 啟動器
    │   ├── MedicalMLApplication.java               ← Spring Boot 啟動設定
    │   ├── PrimaryStageInitializer.java            ← 主視窗初始化 + FXML 載入
    │   ├── StageReadyEvent.java                    ← Spring Event（視窗就緒通知）
    │   │
    │   ├── dto/
    │   │   ├── PreprocessOptions.java              ← 前處理參數 DTO
    │   │   └── PreprocessingStep.java              ← 前處理步驟定義
    │   │
    │   ├── ml/
    │   │   ├── algorithm/
    │   │   │   ├── MLAlgorithm.java                ← 演算法共用介面
    │   │   │   ├── J48Algo.java                    ← C4.5 決策樹
    │   │   │   ├── RandomForestAlgo.java           ← 隨機森林（200 棵樹）
    │   │   │   ├── LogisticAlgo.java               ← 邏輯回歸
    │   │   │   ├── NaiveBayesAlgo.java             ← 樸素貝氏
    │   │   │   ├── BaggingAlgo.java                ← Bagging 集成學習
    │   │   │   └── AdaBoostAlgo.java               ← AdaBoost 集成學習
    │   │   └── factory/
    │   │       └── AlgorithmFactory.java           ← 演算法工廠（統一管理）
    │   │
    │   ├── service/
    │   │   ├── WekaService.java                    ← 核心 Weka 服務（前處理 + 資料分割）
    │   │   ├── SessionState.java                   ← 全域狀態管理（資料 + 模型）
    │   │   ├── ExportService.java                  ← 模型匯出/匯入（.zip 封裝）
    │   │   ├── PredictionService.java              ← 單筆預測服務
    │   │   ├── BatchPredictionService.java         ← 批次預測服務
    │   │   └── LoadModelService.java               ← 模型載入服務
    │   │
    │   ├── ui/controller/
    │   │   ├── MainController.java                 ← 主介面控制器（狀態列）
    │   │   ├── PreprocessTabController.java        ← 資料前處理分頁
    │   │   ├── ClassifyTabController.java          ← 機器學習訓練分頁
    │   │   └── PredictTabController.java           ← 病患預測分頁
    │   │
    │   └── util/
    │       ├── DataLoaderUtil.java                 ← CSV 多檔合併載入工具
    │       └── DataScanner.java                    ← 資料掃描工具
    │
    └── resources/
        ├── application.properties                  ← Spring Boot 設定
        ├── fxml/
        │   ├── layout.fxml                         ← 主介面佈局
        │   ├── tab_preprocess.fxml                 ← 前處理分頁 UI
        │   ├── tab_classify.fxml                   ← 訓練分頁 UI
        │   └── tab_predict.fxml                    ← 預測分頁 UI
        └── styles/
            └── styles.css                          ← 全域樣式表
```

---

## 環境需求

| 工具       | 版本                      |
|-----------|--------------------------|
| Java JDK  | 22（建議 17 以上）         |
| Maven     | 3.6 以上                  |
| Weka      | 3.8.6（由 Maven 自動下載） |
| JavaFX    | 17.0.10（由 Maven 自動下載）|
| Spring Boot | 3.2.3                  |

---

## 建置與執行

```bash
# 1. 編譯專案
mvn clean compile

# 2. 透過 IDE 執行（推薦）
#    執行 com.medical.ml.Launcher 作為主類別
#    JVM 參數建議：-Xmx4g

# 3. 或透過 Maven 打包後執行
mvn package -DskipTests
java -Xmx4g -jar target/medical-ml-desktop-0.0.1-SNAPSHOT.jar
```

---

## 功能總覽

### 📋 資料前處理（Preprocess）

| 功能               | 說明                                                   |
|-------------------|-------------------------------------------------------|
| 多檔合併載入        | 支援同時匯入多個 `.csv` 檔案，自動合併並顯示各檔詳細統計   |
| 智慧型態轉換        | 自動偵測並轉換字串型 → 名目型、數值目標 → 類別目標        |
| 高基數欄位移除      | 自動移除 >100 種值的字串欄（如身分證字號）防止記憶體溢位    |
| 前處理過濾器        | 補齊缺失數據 (ReplaceMissingValues)、類別權重平衡 (SMOTE) |
| 目標欄位選擇        | 可自由選擇任意欄位作為預測目標                            |

### 🧠 機器學習訓練（Classify）

| 模型              | 說明                           | 特色               |
|------------------|-------------------------------|-------------------|
| J48 (C4.5)       | 決策樹分類器                    | 可解釋性高，醫學首選 |
| Random Forest    | 200 棵決策樹集成學習             | 預測穩定，泛化能力強 |
| Logistic Regression | 線性分類器                   | 可查看特徵影響係數   |
| Naive Bayes      | 樸素貝氏分類                    | 高速，作為基準比較   |
| Bagging          | Bootstrap 集成學習             | 降低過擬合          |
| AdaBoost         | 自適應提升學習                   | 強化弱學習器        |

**評估方式：**

| 方式                 | 說明                          |
|---------------------|------------------------------|
| 全資料集訓練          | 用全部資料評估（易過擬合）       |
| K-fold 交叉驗證      | 預設 10 折，最推薦的評估方式     |
| 百分比拆分            | 自訂比例拆分訓練/測試集          |

**模型清單顯示格式：**
```
J48 - Acc: 96.7% | R(0): 99.2% | R(1): 41.0%
```
- `Acc`：整體準確率
- `R(0)`：Class 0 召回率（如「未手術」的偵測率）
- `R(1)`：Class 1 召回率（如「需手術」的偵測率）⚠️ **醫學上最關鍵的指標**

### 🔮 病患預測（Predict）

| 功能                | 說明                                                             |
|--------------------|------------------------------------------------------------------|
| 模型來源選擇         | 下拉選單切換訓練歷史中的任意模型，或從本機載入 `.zip` 模型檔        |
| 多模型對比面板       | 📊 以瀏覽器分頁方式同時檢視多個模型的完整分析報告，一鍵選用          |
| 單筆預測            | 動態產生表單（根據模型欄位自動調整），彈出視窗顯示結果與模型信心度    |
| 批次預測            | 匯入未知 CSV → 自動比對欄位 → 輸出包含預測結果與信心度的 CSV 檔案  |

---

## 模型匯出格式

匯出的 `.zip` 檔案包含：

| 檔案              | 內容                           |
|------------------|-------------------------------|
| `model.model`    | 序列化的 Weka 分類器            |
| `filter.model`   | 序列化的前處理過濾器             |
| `header.arff`    | 訓練資料表頭（欄位結構定義）      |
| `classIndex.txt` | 預測目標欄位索引（防止載入時遺失） |
| `report.txt`     | 原始訓練分析報告（完整保存）      |

---

## 預測輸出

### 單筆預測

```
預測結果: 1
模型信心度: 87.30%
(信心度為模型對此預測的把握程度)
```

### 批次預測 CSV 輸出

| 原始欄位1 | 原始欄位2 | ... | Predicted_OPwithin72hr | Confidence(%) |
|----------|----------|-----|----------------------|---------------|
| 75       | 1        | ... | 1                    | 87.30         |
| 62       | 0        | ... | 0                    | 94.15         |

---

## 防護機制

| 機制                       | 說明                                                        |
|---------------------------|-------------------------------------------------------------|
| Anti-OOM 自動過濾          | 訓練前自動移除 >500 種值的高基數 Nominal 欄位，防止記憶體溢位   |
| Class Index 持久化         | 匯出時強制保存目標索引，解決 Weka 原生反序列化後遺失的問題      |
| 智慧欄位比對               | 批次預測時按欄位名稱配對，容忍來源資料的欄位順序與數量差異       |
| String 屬性安全轉換         | 自動偵測低基數字串轉 Nominal，高基數字串直接移除               |

---

## 注意事項

- ⚠️ **洩漏欄位須排除**：手術時間、術後 ICD-10、住院手術碼等含未來資訊的欄位，須在前處理階段手動移除
- ⚠️ **不平衡資料**：手術案例僅佔約 4.4%，建議使用 SMOTE 或類別權重平衡進行處理
- ⚠️ **本系統僅供學術研究，不可直接用於臨床決策**
- 💡 建議將 JVM 記憶體上限設為 4GB 以上（`-Xmx4g`），以應對大型醫療資料集
