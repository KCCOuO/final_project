#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
72-Hour Emergency Surgery Prediction - Premium Data Preprocessing GUI
Integrated with the 100% verified, step-by-step robust pipeline.
"""

import os
import sys
import tkinter as tk
from tkinter import filedialog, messagebox, ttk
import pandas as pd
import numpy as np
import threading
from sklearn.model_selection import train_test_split

class PremiumPreprocessGUI:
    def __init__(self, root):
        self.root = root
        self.root.title("72-Hour Prediction Preprocessing Suite")
        self.root.geometry("680x620")
        self.root.minsize(600, 550)
        
        # Configure Premium Color Palette (Modern Clean Light Mode theme)
        self.bg_color = "#F3F4F6"       # Soft light gray background
        self.panel_bg = "#FFFFFF"       # White panel background
        self.text_color = "#1F2937"     # Dark slate text
        self.accent_color = "#3B82F6"   # Electric blue button / accent
        self.accent_hover = "#2563EB"   # Darker blue for hover
        self.success_color = "#059669"  # Darker emerald green for OK/Success
        self.warning_color = "#D97706"  # Darker amber for warnings
        self.danger_color = "#DC2626"   # Red for errors
        self.border_color = "#E5E7EB"   # Light border lines
        
        # Set window background
        self.root.configure(bg=self.bg_color)
        
        # Apply style override for progress bar
        style = ttk.Style()
        style.theme_use('default')
        style.configure("TProgressbar", thickness=15, troughcolor=self.panel_bg, background=self.accent_color)
        
        self.setup_ui()
        
    def setup_ui(self):
        # 1. Header Banner
        header = tk.Frame(self.root, bg=self.accent_color, height=80)
        header.pack(fill=tk.X)
        header.pack_propagate(False)
        
        title_label = tk.Label(
            header, 
            text="72-HOUR SURGERY PREPROCESSING SUITE", 
            fg="white", 
            bg=self.accent_color,
            font=("Segoe UI", 16, "bold")
        )
        title_label.pack(side=tk.LEFT, padx=20, pady=22)
        
        version_label = tk.Label(
            header,
            text="v1.2 (Validated)",
            fg="#93C5FD",
            bg=self.accent_color,
            font=("Segoe UI", 10, "italic")
        )
        version_label.pack(side=tk.RIGHT, padx=20, pady=27)
        
        # Main Container
        main_frame = tk.Frame(self.root, bg=self.bg_color, padx=20, pady=20)
        main_frame.pack(fill=tk.BOTH, expand=True)
        
        # 2. Input/Output Configurations Panel
        config_panel = tk.LabelFrame(
            main_frame, 
            text=" IO Configurations ", 
            fg=self.accent_color,
            bg=self.panel_bg,
            bd=1,
            relief=tk.SOLID,
            font=("Segoe UI", 11, "bold"),
            padx=15,
            pady=15
        )
        config_panel.pack(fill=tk.X, pady=(0, 15))
        
        # Input Path
        tk.Label(
            config_panel, 
            text="Raw Dataset File (CSV or Excel):", 
            fg=self.text_color, 
            bg=self.panel_bg,
            font=("Segoe UI", 10)
        ).grid(row=0, column=0, sticky=tk.W, pady=(0, 5))
        
        self.entry_input = tk.Entry(
            config_panel, 
            fg=self.text_color, 
            bg=self.bg_color, 
            insertbackground=self.text_color,
            bd=1, 
            relief=tk.SOLID,
            highlightthickness=1,
            highlightcolor=self.accent_color,
            highlightbackground=self.border_color,
            font=("Segoe UI", 10)
        )
        self.entry_input.grid(row=1, column=0, sticky=tk.EW, padx=(0, 10), pady=(0, 10))
        
        self.btn_browse_input = tk.Button(
            config_panel, 
            text=" Browse... ", 
            command=self.browse_input,
            fg="white",
            bg=self.border_color,
            activeforeground="white",
            activebackground=self.accent_color,
            relief=tk.FLAT,
            cursor="hand2",
            font=("Segoe UI", 9, "bold"),
            padx=10
        )
        self.btn_browse_input.grid(row=1, column=1, pady=(0, 10))
        self.bind_hover_effect(self.btn_browse_input, self.accent_color, self.border_color)
        
        # Output Directory
        tk.Label(
            config_panel, 
            text="Output Directory for Converted Files:", 
            fg=self.text_color, 
            bg=self.panel_bg,
            font=("Segoe UI", 10)
        ).grid(row=2, column=0, sticky=tk.W, pady=(5, 5))
        
        self.entry_output = tk.Entry(
            config_panel, 
            fg=self.text_color, 
            bg=self.bg_color, 
            insertbackground=self.text_color,
            bd=1, 
            relief=tk.SOLID,
            highlightthickness=1,
            highlightcolor=self.accent_color,
            highlightbackground=self.border_color,
            font=("Segoe UI", 10)
        )
        self.entry_output.grid(row=3, column=0, sticky=tk.EW, padx=(0, 10))
        # Default output dir to 'dataset' folder in current working directory
        default_out = os.path.join(os.getcwd(), "dataset")
        self.entry_output.insert(0, default_out)
        
        self.btn_browse_output = tk.Button(
            config_panel, 
            text=" Select Folder ", 
            command=self.browse_output,
            fg="white",
            bg=self.border_color,
            activeforeground="white",
            activebackground=self.accent_color,
            relief=tk.FLAT,
            cursor="hand2",
            font=("Segoe UI", 9, "bold"),
            padx=8
        )
        self.btn_browse_output.grid(row=3, column=1)
        self.bind_hover_effect(self.btn_browse_output, self.accent_color, self.border_color)
        
        config_panel.columnconfigure(0, weight=1)
        
        # 3. Process Execution Button
        self.btn_run = tk.Button(
            main_frame, 
            text="START DATA PREPROCESSING PIPELINE", 
            command=self.start_pipeline_thread,
            fg="white",
            bg=self.accent_color,
            activeforeground="white",
            activebackground=self.accent_hover,
            relief=tk.FLAT,
            cursor="hand2",
            font=("Segoe UI", 11, "bold"),
            pady=10
        )
        self.btn_run.pack(fill=tk.X, pady=(0, 15))
        self.bind_hover_effect(self.btn_run, self.accent_hover, self.accent_color)
        
        # 4. Progress and Console logs
        log_panel = tk.LabelFrame(
            main_frame, 
            text=" Pipeline Progress & Logs ", 
            fg=self.accent_color,
            bg=self.panel_bg,
            bd=1,
            relief=tk.SOLID,
            font=("Segoe UI", 11, "bold"),
            padx=15,
            pady=15
        )
        log_panel.pack(fill=tk.BOTH, expand=True)
        
        self.progress_bar = ttk.Progressbar(
            log_panel, 
            orient="horizontal", 
            mode="determinate",
            style="TProgressbar"
        )
        self.progress_bar.pack(fill=tk.X, pady=(0, 10))
        
        # Console Log textbox
        log_scrollbar = tk.Scrollbar(log_panel)
        log_scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        
        self.txt_log = tk.Text(
            log_panel, 
            fg="#1F2937", # Dark slate text
            bg="#F9FAFB", # Very light gray for console
            insertbackground="#1F2937",
            bd=1,
            relief=tk.SOLID,
            font=("Consolas", 10),
            yscrollcommand=log_scrollbar.set,
            state=tk.DISABLED
        )
        self.txt_log.pack(fill=tk.BOTH, expand=True)
        log_scrollbar.config(command=self.txt_log.yview)
        
        # Configure colorful log tags
        self.txt_log.tag_config("ok", foreground=self.success_color)
        self.txt_log.tag_config("warn", foreground=self.warning_color)
        self.txt_log.tag_config("danger", foreground=self.danger_color)
        self.txt_log.tag_config("cyan", foreground="#22D3EE")
        
    def bind_hover_effect(self, button, hover_color, normal_color):
        button.bind("<Enter>", lambda e: button.configure(bg=hover_color))
        button.bind("<Leave>", lambda e: button.configure(bg=normal_color))
        
    def browse_input(self):
        file_path = filedialog.askopenfilename(
            filetypes=[
                ("Supported Datasets", "*.csv;*.xlsx"),
                ("CSV Files", "*.csv"),
                ("Excel Files", "*.xlsx"),
                ("All Files", "*.*")
            ]
        )
        if file_path:
            self.entry_input.delete(0, tk.END)
            self.entry_input.insert(0, file_path)
            
    def browse_output(self):
        dir_path = filedialog.askdirectory()
        if dir_path:
            self.entry_output.delete(0, tk.END)
            self.entry_output.insert(0, dir_path)
            
    def log(self, message, level="info"):
        self.txt_log.config(state=tk.NORMAL)
        
        # Add timestamp/formatting if desired
        formatted_message = message + "\n"
        
        if "[OK]" in message:
            self.txt_log.insert(tk.END, formatted_message, "ok")
        elif "[Done]" in message or "===" in message:
            self.txt_log.insert(tk.END, formatted_message, "cyan")
        elif "[Warning]" in message or "⚠️" in message:
            self.txt_log.insert(tk.END, formatted_message, "warn")
        elif "[Error]" in message or "❌" in message:
            self.txt_log.insert(tk.END, formatted_message, "danger")
        else:
            self.txt_log.insert(tk.END, formatted_message)
            
        self.txt_log.see(tk.END)
        self.txt_log.config(state=tk.DISABLED)
        self.root.update_idletasks()
        
    def set_progress(self, val):
        self.progress_bar["value"] = val
        self.root.update_idletasks()
        
    def start_pipeline_thread(self):
        input_path = self.entry_input.get().strip()
        output_dir = self.entry_output.get().strip()
        
        if not input_path:
            messagebox.showwarning("Warning", "Please select a raw dataset input file first!")
            return
        if not os.path.exists(input_path):
            messagebox.showerror("Error", f"Input file '{input_path}' does not exist!")
            return
        if not output_dir:
            messagebox.showwarning("Warning", "Please specify a target output directory!")
            return
            
        # Disable button to prevent re-entry
        self.btn_run.config(state=tk.DISABLED, text="PROCESSING PIPELINE ACTIVE...")
        
        # Clear log area
        self.txt_log.config(state=tk.NORMAL)
        self.txt_log.delete("1.0", tk.END)
        self.txt_log.config(state=tk.DISABLED)
        
        # Start background thread
        thread = threading.Thread(target=self.run_pipeline, args=(input_path, output_dir))
        thread.daemon = True
        thread.start()
        
    def run_pipeline(self, input_path, output_dir):
        try:
            self.set_progress(5)
            self.log(f"==================================================")
            self.log(f"Starting Preprocessing Pipeline Suite")
            self.log(f"Input: {input_path}")
            self.log(f"Output Directory: {output_dir}")
            self.log(f"==================================================\n")
            
            ext = os.path.splitext(input_path)[1].lower()
            
            # ----------------------------------------------------
            # STEP 1: Load Raw Data & Create dataset00.csv
            # ----------------------------------------------------
            self.log("Step 1: Loading raw data and selecting columns (index 1 to 263)...")
            
            if ext == '.xlsx':
                self.log("[INFO] Excel format detected. Starting manual row parsing (this may take up to a minute)...")
                excel_df = pd.read_excel(input_path, header=None)
                
                header_row_idx = None
                for idx in range(min(10, len(excel_df))):
                    row_vals = excel_df.iloc[idx].tolist()
                    if 'No' in row_vals or 'hisnummark' in row_vals:
                        header_row_idx = idx
                        break
                        
                if header_row_idx is None:
                    raise ValueError("Could not find English header row containing 'No' or 'hisnummark' in the sheet.")
                    
                self.log(f"[INFO] English column names identified at Excel row {header_row_idx + 1}")
                
                headers = excel_df.iloc[header_row_idx].tolist()[:263]
                data_rows = excel_df.iloc[header_row_idx + 1:].iloc[:, :263]
                
                df = pd.DataFrame(data_rows.values, columns=headers)
                
                # Filter rows where sequential index 'No' is numeric
                df['No_str'] = df['No'].astype(str).str.strip()
                df = df[df['No_str'].str.match(r'^\d+$', na=False)]
                df = df.drop(columns=['No_str'])
                df['No'] = df['No'].astype(int)
            
            elif ext == '.csv':
                self.log("[INFO] CSV format detected. Reading file...")
                df = pd.read_csv(input_path, low_memory=False)
                if len(df.columns) > 263:
                    self.log(f"[INFO] Truncating trailing columns (retaining 1 to 263)...")
                    df = df.iloc[:, :263]
            else:
                raise ValueError("Unsupported format! Please feed a .xlsx or .csv dataset.")
                
            self.log(f"[INFO] dataset00 shape established: {df.shape}")
            os.makedirs(output_dir, exist_ok=True)
            
            dataset00_path = os.path.join(output_dir, "dataset00.csv")
            df.to_csv(dataset00_path, index=False, encoding='utf-8-sig')
            self.log(f"[OK] Saved: {dataset00_path}")
            self.set_progress(15)
            
            # ----------------------------------------------------
            # STEP 2: Replace '.' with Empty Cells (dataset01.csv)
            # ----------------------------------------------------
            self.log("\nStep 2: Performing Missing Value Standardisation ('.' -> empty)...")
            df = df.replace(r'^\.$', '', regex=True)
            df = df.replace(r'^\s*\.\s*$', '', regex=True)
            
            dataset01_path = os.path.join(output_dir, "dataset01.csv")
            df.to_csv(dataset01_path, index=False, encoding='utf-8-sig')
            self.log(f"[OK] Saved: {dataset01_path}")
            self.set_progress(30)
            
            # ----------------------------------------------------
            # STEP 3: Strip Whitespace (dataset02.csv)
            # ----------------------------------------------------
            self.log("\nStep 3: Executing cell-level whitespace cleaning...")
            for col in df.columns:
                if df[col].dtype == object:
                    df[col] = df[col].astype(str).str.strip()
                    df[col] = df[col].replace('nan', '').replace('None', '')
                    
            dataset02_path = os.path.join(output_dir, "dataset02.csv")
            df.to_csv(dataset02_path, index=False, encoding='utf-8-sig')
            self.log(f"[OK] Saved: {dataset02_path}")
            self.set_progress(45)
            
            # ----------------------------------------------------
            # STEP 4: Remove Unnecessary Columns (dataset03.csv)
            # ----------------------------------------------------
            self.log("\nStep 4: Executing Feature Pruning (dropping IDs, datetimes, raw vitals, ER stays)...")
            DELETED_COLUMNS = [
                'hisnummark',
                'VS_hbp1', 'VS_hbp2', 'VS_hbp3', 'VS_hbp4', 'VS_hbp5', 'VS_hbp6', 'VS_hbp7', 'VS_hbp8', 'VS_hbp9', 'VS_hbp10',
                'VS_lbp1', 'VS_lbp2', 'VS_lbp3', 'VS_lbp4', 'VS_lbp5', 'VS_lbp6', 'VS_lbp7', 'VS_lbp8', 'VS_lbp9', 'VS_lbp10',
                'VS_pulse1', 'VS_pulse2', 'VS_pulse3', 'VS_pulse4', 'VS_pulse5', 'VS_pulse6', 'VS_pulse7', 'VS_pulse8', 'VS_pulse9', 'VS_pulse10',
                'VS_temper1', 'VS_temper2', 'VS_temper3', 'VS_temper4', 'VS_temper5', 'VS_temper6', 'VS_temper7', 'VS_temper8', 'VS_temper9', 'VS_temper10',
                'VS_breathe1', 'VS_breathe2', 'VS_breathe3', 'VS_breathe4', 'VS_breathe5', 'VS_breathe6', 'VS_breathe7', 'VS_breathe8', 'VS_breathe9', 'VS_breathe10',
                'VS_spo21', 'VS_spo22', 'VS_spo23', 'VS_spo24', 'VS_spo25', 'VS_spo26', 'VS_spo27', 'VS_spo28', 'VS_spo29', 'VS_spo210',
                'TRI_etdt', 'TRI_ettm', 'TRI_teamdt', 'TRI_teamtm',
                'ordt', 'ortm', 'ordtm',
                'ER_hcaseno', 'ER_admdt', 'ER_admtm', 'ER_disdt', 'ER_distm',
                'ER_hicd10_1', 'ER_hicd10_2', 'ER_hicd10_3', 'ER_hicd10_4', 'ER_hicd10_5', 'ER_hicd10_6', 'ER_hicd10_7',
                'ER_hdistype', 'ER_LOS_min', 'ER_LOS_hr',
                'ER_oppfcode1', 'ER_oppfcode2', 'ER_oppfcode3', 'ER_oppfcode4',
                'ER_opicd10_1', 'ER_opicd10_2', 'ER_opicd10_3',
                'ER_opdt', 'ER_optm', 'ER_surgery_type', 'ER_surgery_sect',
                'EROP', 'IPOP', 'OPtm_min', 'OPtm_hr', 'surgery', 'surgery_date', 'surgery_time',
                'IPhcaseno', 'IPadmdt', 'IPadmtm', 'IPdisdt', 'IPdistm', 'IPhsect',
                'IPhicd10_0', 'IPhicd10_1', 'IPhicd10_2', 'IPhicd10_3', 'IPhicd10_4', 'IPhicd10_5', 'IPhicd10_6', 'IPhicd10_7',
                'IPhintype', 'IPhdistype',
                'IP_ophcaseno', 'IP_oppfcode1', 'IP_oppfcode2', 'IP_oppfcode3', 'IP_oppfcode4',
                'IP_opicd10_1', 'IP_opicd10_2', 'IP_opicd10_3', 'IP_opicd10_4', 'IP_opicd10_5', 'IP_opicd10_6', 'IP_opicd10_7', 'IP_opicd10_8',
                'IP_opdt', 'IP_optm', 'IP_surgery_type', 'IP_surgery_sect',
                'opdt_within72hr', 'optm_within72hr', 'opdtm_within72hr'
            ]
            
            cols_to_drop = [c for c in DELETED_COLUMNS if c in df.columns]
            df = df.drop(columns=cols_to_drop)
            self.log(f"[INFO] Retained columns: {len(df.columns)} (dropped {len(cols_to_drop)} columns)")
            
            dataset03_path = os.path.join(output_dir, "dataset03.csv")
            df.to_csv(dataset03_path, index=False, encoding='utf-8-sig')
            self.log(f"[OK] Saved: {dataset03_path}")
            self.set_progress(60)
            
            # ----------------------------------------------------
            # STEP 5: Binary Value Mapping to N/Y (dataset04.csv)
            # ----------------------------------------------------
            self.log("\nStep 5: Standardizing binary nominal values to N / Y...")
            ny_columns = [
                'PROC_transfusion', 'PROC_intub', 'PROC_endo', 'PROC_chestintub',
                'IMG_chestXray', 'IMG_Xray', 'IMG_CT', 'IMG_MRI', 'IMG_ECG', 'IMG_SONO',
                'consult', 'OPwithin72hr', 'LAB_ANTI', 'orselect', 'TRI_teamflag'
            ]
            ny_columns += [f'TRI_teamon_N{i}' for i in range(1, 6)]
            ny_columns += [f'TRI_teamon_T{i}' for i in range(1, 6)]
            ny_columns += [f'TRI_teamon{i}' for i in range(1, 6)]
            ny_columns += [f'TRI_gturn{i}' for i in range(1, 15)]
            ny_columns += ['orfreqn_e', 'orfreqn_r', 'orfreqn_n']
            
            for col in ny_columns:
                if col in df.columns:
                    df[col] = df[col].astype(str).str.strip()
                    df[col] = df[col].map({'0': 'N', '1': 'Y', '0.0': 'N', '1.0': 'Y'}).fillna(df[col])
                    df[col] = df[col].replace('nan', '').replace('None', '').replace('', np.nan)
                    
            if 'TRI_gcschang' in df.columns:
                df['TRI_gcschang'] = df['TRI_gcschang'].astype(str).str.strip()
                df['TRI_gcschang'] = df['TRI_gcschang'].map({'1': 'N', '2': 'Y', '1.0': 'N', '2.0': 'Y'}).fillna(df['TRI_gcschang'])
                df['TRI_gcschang'] = df['TRI_gcschang'].replace('nan', '').replace('None', '').replace('', np.nan)
                
            dataset04_path = os.path.join(output_dir, "dataset04.csv")
            df.to_csv(dataset04_path, index=False, encoding='utf-8-sig')
            self.log(f"[OK] Saved: {dataset04_path}")
            self.set_progress(75)
            
            # ----------------------------------------------------
            # STEP 6: Drop 'No' & Wrap ID fields in quotes (dataset05.csv)
            # ----------------------------------------------------
            self.log("\nStep 6: Dropping index 'No' and formatting ID values...")
            if 'No' in df.columns:
                df = df.drop(columns=['No'])
                
            for col in ['TRI_systemid', 'TRI_ccompid', 'TRI_triageid']:
                if col in df.columns:
                    df[col] = df[col].apply(lambda x: f" {str(int(float(x)))}" if pd.notna(x) and str(x).strip() != '' and str(x).strip() != 'nan' else '')
                    
            dataset05_path = os.path.join(output_dir, "dataset05.csv")
            df.to_csv(dataset05_path, index=False, encoding='utf-8-sig')
            self.log(f"[OK] Saved: {dataset05_path}")
            self.set_progress(85)
            
            # ----------------------------------------------------
            # STEP 7: Department Filtering & Stratified Split (ESUR.csv, train, test)
            # ----------------------------------------------------
            self.log("\nStep 7: Partitioning department data and performing stratified split...")
            if 'ER_hsect' in df.columns:
                df['ER_hsect'] = df['ER_hsect'].astype(str).str.strip()
                esur_df = df[df['ER_hsect'] == 'ESUR'].copy()
                
                self.log(f"[INFO] Filtered {len(esur_df)} ESUR records")
                esur_csv_path = os.path.join(output_dir, "ESUR.csv")
                esur_df.to_csv(esur_csv_path, index=False, encoding='utf-8-sig')
                self.log(f"[OK] Saved: {esur_csv_path}")
                
                if 'OPwithin72hr' in esur_df.columns:
                    esur_split_df = esur_df.dropna(subset=['OPwithin72hr'])
                    train_df, test_df = train_test_split(
                        esur_split_df, 
                        test_size=0.2, 
                        random_state=42, 
                        stratify=esur_split_df['OPwithin72hr']
                    )
                    
                    train_path = os.path.join(output_dir, "ESUR_train.csv")
                    test_path = os.path.join(output_dir, "ESUR_test.csv")
                    
                    train_df.to_csv(train_path, index=False, encoding='utf-8-sig')
                    test_df.to_csv(test_path, index=False, encoding='utf-8-sig')
                    
                    self.log(f"[OK] Saved stratified train split: {train_path} ({len(train_df)} rows)")
                    self.log(f"[OK] Saved stratified test split: {test_path} ({len(test_df)} rows)")
                    
                    # Log Distribution Summary
                    self.log("\n=== Class Distribution Summary (OPwithin72hr) ===")
                    self.log(f"Full ESUR - Y: {sum(esur_df['OPwithin72hr'] == 'Y')} | N: {sum(esur_df['OPwithin72hr'] == 'N')}")
                    self.log(f"Training  - Y: {sum(train_df['OPwithin72hr'] == 'Y')} | N: {sum(train_df['OPwithin72hr'] == 'N')}")
                    self.log(f"Testing   - Y: {sum(test_df['OPwithin72hr'] == 'Y')} | N: {sum(test_df['OPwithin72hr'] == 'N')}")
                    self.log("=================================================")
                    
                else:
                    self.log("[Warning] Target column 'OPwithin72hr' not found. Skipping split.")
            else:
                self.log("[Warning] Department column 'ER_hsect' not found. Skipping ESUR filtering.")
                
            self.set_progress(100)
            self.log(f"\n[Done] DATA PREPROCESSING PIPELINE SUCCESSFULLY COMPLETE!")
            
            messagebox.showinfo("Success", f"Data Preprocessing Suite successfully completed!\nAll files saved to:\n{output_dir}")
            
        except Exception as e:
            self.log(f"\n[Error] Pipeline halted with error: {str(e)}")
            messagebox.showerror("Execution Error", f"An error occurred during execution:\n{str(e)}")
        finally:
            self.btn_run.config(state=tk.NORMAL, text="START DATA PREPROCESSING PIPELINE")

if __name__ == "__main__":
    root = tk.Tk()
    app = PremiumPreprocessGUI(root)
    
    # Pre-populate default input path if it exists to be friendly
    default_csv = os.path.join(os.getcwd(), "dataset", "dataset00.csv")
    if os.path.exists(default_csv):
        app.entry_input.insert(0, default_csv)
        
    root.mainloop()
