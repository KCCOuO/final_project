import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
import os
import tkinter as tk
from tkinter import filedialog, messagebox
from tkinter import ttk

# 定義最終 125 個目標欄位的清單與順序 (與 dataset05 一致)
FINAL_COLUMNS = [
    'psex','hage_y','ER_hsect','ER_hicd10_0','ER_hemgtype','ER_hintype',
    'VS_hbp_mean','VS_lbp_mean','VS_pulse_mean','VS_temper_mean','VS_breathe_mean','VS_spo2_mean',
    'VS_hbp_sd','VS_lbp_sd','VS_pulse_sd','VS_temper_sd','VS_breathe_sd','VS_spo2_sd',
    'VS_hbp_var','VS_lbp_var','VS_pulse_var','VS_temper_var','VS_breathe_var','VS_spo2_var',
    'TRI_etgcse','TRI_etgcsv','TRI_etgcsm','TRI_etgcs','TRI_gcschang','TRI_etrts','TRI_hurt',
    'TRI_systemid','TRI_ccompid','TRI_triageid',
    'TRI_gturn1','TRI_gturn2','TRI_gturn3','TRI_gturn4','TRI_gturn5','TRI_gturn6','TRI_gturn7',
    'TRI_gturn8','TRI_gturn9','TRI_gturn10','TRI_gturn11','TRI_gturn12','TRI_gturn13','TRI_gturn14',
    'TRI_teamon_N1','TRI_teamon_N2','TRI_teamon_N3','TRI_teamon_N4','TRI_teamon_N5',
    'TRI_teamon_T1','TRI_teamon_T2','TRI_teamon_T3','TRI_teamon_T4','TRI_teamon_T5',
    'TRI_teamon1','TRI_teamon2','TRI_teamon3','TRI_teamon4','TRI_teamon5','TRI_teamflag',
    'LAB_ALP','LAB_ANTI','LAB_APTT','LAB_ATYLYM','LAB_BAND','LAB_BASO','LAB_BLAST','LAB_BUN',
    'LAB_CREA','LAB_CRPP','LAB_DBIL','LAB_EGFR','LAB_EOSINO','LAB_GGT','LAB_GOT','LAB_GPT',
    'LAB_HCT','LAB_HGB','LAB_INR','LAB_LACT','LAB_LDH','LAB_LYM','LAB_MCB','LAB_MCH','LAB_MCHC',
    'LAB_MCV','LAB_META','LAB_MG','LAB_MONO','LAB_MPV','LAB_MYELO','LAB_NA','LAB_NRBC',
    'LAB_PDW','LAB_PLATE','LAB_PROMYE','LAB_PT','LAB_RBC','LAB_RDW','LAB_SEG','LAB_TBIL','LAB_WBC',
    'PROC_transfusion','PROC_intub','PROC_endo','PROC_chestintub',
    'IMG_chestXray','IMG_Xray','IMG_CT','IMG_MRI','IMG_ECG','IMG_SONO',
    'orcatgy','orfreqn','orfreqn_e','orfreqn_r','orfreqn_n','orclsect','orselect','consult','OPwithin72hr'
]

def run_process(input_path, output_dir, log_func):
    try:
        log_func(f"開始處理資料: {input_path}")
        
        # 1. 讀取原始資料
        df = pd.read_csv(input_path, low_memory=False)
        log_func("讀取完成，正在執行欄位過濾與轉換...")
        
        # 2. 定義需要轉換為 N/Y 的欄位清單
        ny_columns = [
            'PROC_transfusion', 'PROC_intub', 'PROC_endo', 'PROC_chestintub',
            'IMG_chestXray', 'IMG_Xray', 'IMG_CT', 'IMG_MRI', 'IMG_ECG', 'IMG_SONO',
            'consult', 'OPwithin72hr', 'TRI_teamflag'
        ]
        ny_columns += [f'TRI_teamon_N{i}' for i in range(1, 6)]
        ny_columns += [f'TRI_teamon_T{i}' for i in range(1, 6)]
        ny_columns += [f'TRI_teamon{i}' for i in range(1, 6)]
        ny_columns += [f'TRI_gturn{i}' for i in range(1, 15)]
        ny_columns += ['orfreqn_e', 'orfreqn_r', 'orfreqn_n']
        
        for col in ny_columns:
            if col in df.columns:
                df[col] = df[col].map({0: 'N', 1: 'Y', '0': 'N', '1': 'Y'}).fillna(df[col])

        # 3. 特殊邏輯 (GCS/Teamflag)
        # 基於使用者要求：TRI_etgcs 1->N, 2->Y; TRI_gcschang 1->N, 2->Y
        for col in ['TRI_etgcs', 'TRI_gcschang']:
            if col in df.columns:
                df[col] = df[col].map({1: 'N', 2: 'Y', '1': 'N', '2': 'Y'}).fillna(df[col])

        # 4. ID 欄位字串化 (加上引號)
        for col in ['TRI_systemid', 'TRI_ccompid', 'TRI_triageid']:
            if col in df.columns:
                df[col] = df[col].astype(str).apply(lambda x: f'"{x}"' if x != 'nan' and x != '' else x)

        # 5. 嚴格對齊 125 個目標欄位
        # 只保留 FINAL_COLUMNS 中有的欄位，且順序一致
        final_df = pd.DataFrame()
        for col in FINAL_COLUMNS:
            if col in df.columns:
                final_df[col] = df[col]
            else:
                log_func(f"⚠️ 警告: 原始資料中缺少欄位 '{col}'，已填補空值。")
                final_df[col] = np.nan

        # 6. 拆分科別與抽樣
        os.makedirs(output_dir, exist_ok=True)
        for dept in ['ESUR', 'EMED']:
            dept_df = final_df[final_df['ER_hsect'] == dept].copy()
            if not dept_df.empty:
                log_func(f"處理 {dept} 科別... ({len(dept_df)} 筆)")
                
                # 儲存完整的科別 CSV
                dept_csv = os.path.join(output_dir, f'{dept}.csv')
                dept_df.to_csv(dept_csv, index=False, quoting=0)
                
                # 7. 分層抽樣拆分 (80/20)
                if len(dept_df) > 1:
                    train, test = train_test_split(
                        dept_df, test_size=0.2, random_state=42, stratify=dept_df['OPwithin72hr']
                    )
                    train.to_csv(os.path.join(output_dir, f'{dept}_train.csv'), index=False, quoting=0)
                    test.to_csv(os.path.join(output_dir, f'{dept}_test.csv'), index=False, quoting=0)
                    log_func(f"  - {dept} 訓練與測試集生成完畢")
                else:
                    log_func(f"  - {dept} 資料量過少，跳過抽樣拆分")

        log_func("\n🎉 所有處理已按流程完整執行！")
        messagebox.showinfo("完成", f"資料處理成功！\n共生成 125 個欄位。\n儲存路徑：{output_dir}")
    except Exception as e:
        log_func(f"❌ 錯誤: {str(e)}")
        messagebox.showerror("錯誤", f"處理過程中發生錯誤：\n{str(e)}")

        log_func("\n🎉 所有處理已完成！")
        messagebox.showinfo("完成", f"資料處理成功！\n儲存路徑：{output_dir}")
    except Exception as e:
        log_func(f"❌ 錯誤: {str(e)}")
        messagebox.showerror("錯誤", f"處理過程中發生錯誤：\n{str(e)}")

class PreprocessGUI:
    def __init__(self, root):
        self.root = root
        self.root.title("72小時預測資料前處理工具")
        self.root.geometry("500x400")
        
        # UI 配置
        frame = tk.Frame(root, padx=20, pady=20)
        frame.pack(fill=tk.BOTH, expand=True)
        
        tk.Label(frame, text="原始資料檔案 (CSV):").pack(anchor=tk.W)
        self.entry_input = tk.Entry(frame, width=50)
        self.entry_input.pack(fill=tk.X, pady=5)
        tk.Button(frame, text="瀏覽檔案", command=self.browse_file).pack(anchor=tk.E)
        
        tk.Label(frame, text="輸出的資料夾:").pack(anchor=tk.W, pady=(10, 0))
        self.entry_output = tk.Entry(frame, width=50)
        self.entry_output.pack(fill=tk.X, pady=5)
        self.entry_output.insert(0, os.path.join(os.getcwd(), "processed_results"))
        tk.Button(frame, text="選擇資料夾", command=self.browse_dir).pack(anchor=tk.E)
        
        self.btn_run = tk.Button(frame, text="開始執行轉換", command=self.start, bg="#4CAF50", fg="white", height=2)
        self.btn_run.pack(fill=tk.X, pady=20)
        
        tk.Label(frame, text="執行日誌:").pack(anchor=tk.W)
        self.txt_log = tk.Text(frame, height=8, state=tk.DISABLED)
        self.txt_log.pack(fill=tk.BOTH, expand=True)

    def browse_file(self):
        filename = filedialog.askopenfilename(filetypes=[("CSV Files", "*.csv")])
        if filename:
            self.entry_input.delete(0, tk.END)
            self.entry_input.insert(0, filename)

    def browse_dir(self):
        directory = filedialog.askdirectory()
        if directory:
            self.entry_output.delete(0, tk.END)
            self.entry_output.insert(0, directory)

    def log(self, message):
        self.txt_log.config(state=tk.NORMAL)
        self.txt_log.insert(tk.END, message + "\n")
        self.txt_log.see(tk.END)
        self.txt_log.config(state=tk.DISABLED)
        self.root.update_idletasks()

    def start(self):
        input_path = self.entry_input.get()
        output_dir = self.entry_output.get()
        if not input_path or not os.path.exists(input_path):
            messagebox.showwarning("警告", "請選擇正確的輸入檔案路徑")
            return
        
        self.btn_run.config(state=tk.DISABLED)
        run_process(input_path, output_dir, self.log)
        self.btn_run.config(state=tk.NORMAL)

if __name__ == "__main__":
    root = tk.Tk()
    app = PreprocessGUI(root)
    root.mainloop()
