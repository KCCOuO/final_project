# 72 小時手術預測 - 資料前處理工具 (Standalone)

這個工具旨在將原始的醫療資料 (`dataset00.csv`) 轉換為符合 Weka 訓練格式的資料集，並自動完成科別拆分與訓練/測試集抽樣。

## 功能特點
- **自動化 N/Y 轉換**：將 40+ 個二元欄位（0/1）轉換為類別標籤（N/Y）。
- **ID 安全處理**：為 `TRI_systemid` 等 ID 加上引號，防止被誤認為數值。
- **資料清洗**：自動移除序號、時間戳記與無用的未來資訊。
- **分層抽樣 (Stratified Sampling)**：確保訓練集與測試集的手術比例與原始資料一致。
- **科別拆分**：自動生成 `ESUR` (外科) 與 `EMED` (內科) 的獨立檔案。

## 使用步驟

### 1. 準備環境
確保您的電腦已安裝 Python 3.8+。在終端機執行以下指令安裝套件：
```bash
pip install -r requirements.txt
```

### 2. 準備資料
將您的原始資料命名為 `dataset00.csv` 並放在與 `preprocess.py` 相同的資料夾中。

### 3. 執行處理
```bash
python preprocess.py
```

### 4. 取得結果
處理完成後，結果會儲存在 `processed_results` 資料夾中，包含：
- `EMED_train.csv` / `EMED_test.csv`
- `ESUR_train.csv` / `ESUR_test.csv`
- 以及彙整後的 `EMED.csv` / `ESUR.csv`

## 注意事項
- 如果原始 CSV 檔案極大，請確保電腦有足夠的 RAM。
- 如果欄位名稱發生變動，請在 `preprocess.py` 中的 `ny_columns` 進行相應修改。
