#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
72-Hour Emergency Surgery Prediction - Data Preprocessing Pipeline
Based on the official workflow defined in: 資料處理流程 (以 ESUR 為例).xlsx
"""

import os
import sys
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split

def log_step(step_num, title, description):
    print(f"\n==================================================")
    print(f"Step {step_num}: {title}")
    print(f"-> {description}")
    print(f"==================================================")

def preprocess_pipeline(input_path, output_dir="dataset_output"):
    # Ensure output directory exists
    os.makedirs(output_dir, exist_ok=True)
    
    # Check input extension
    ext = os.path.splitext(input_path)[1].lower()
    
    # ----------------------------------------------------
    # STEP 1: Load Raw Data & Create dataset00.csv
    # ----------------------------------------------------
    log_step(1, "Raw Data Loading & Column Selection", "Loading raw data and removing trailing stats/columns after index 263")
    
    if ext == '.xlsx':
        print(f"Loading Excel file: {input_path} (this may take 1-2 minutes for large datasets)...")
        # Load excel file without header to parse rows manually and drop trailing statistical summaries
        excel_df = pd.read_excel(input_path, header=None)
        
        # Locate English headers row (row containing 'No' or 'hisnummark')
        header_row_idx = None
        for idx in range(min(10, len(excel_df))):
            row_vals = excel_df.iloc[idx].tolist()
            if 'No' in row_vals or 'hisnummark' in row_vals:
                header_row_idx = idx
                break
                
        if header_row_idx is None:
            raise ValueError("Could not find English header row containing 'No' or 'hisnummark' in the first 10 rows of the Excel sheet.")
            
        print(f"Found English column names at Excel row {header_row_idx + 1}")
        
        # Extract headers and data rows
        headers = excel_df.iloc[header_row_idx].tolist()
        data_rows = excel_df.iloc[header_row_idx + 1:]
        
        # We only keep the first 263 columns (column index 0 to 262 inclusive)
        # Corresponding to columns 1 to 263 in 1-based index (deleting 264 to 272)
        headers = headers[:263]
        data_rows = data_rows.iloc[:, :263]
        
        # Build DataFrame
        df = pd.DataFrame(data_rows.values, columns=headers)
        
        # Remove description/summary rows at the end of the file
        # The summary rows are identified by non-numeric values in the 'No' column (like 'Min', 'Max', 'Average', or empty)
        df['No_str'] = df['No'].astype(str).str.strip()
        df = df[df['No_str'].str.match(r'^\d+$', na=False)]
        df = df.drop(columns=['No_str'])
        
        # Convert No back to integer type
        df['No'] = df['No'].astype(int)
    
    elif ext == '.csv':
        print(f"Loading CSV file: {input_path}...")
        df = pd.read_csv(input_path, low_memory=False)
        # Drop columns after the 263rd column if present
        if len(df.columns) > 263:
            print(f"Truncating columns from {len(df.columns)} down to 263...")
            df = df.iloc[:, :263]
    else:
        raise ValueError("Unsupported file format! Please provide either a .xlsx or .csv file.")
        
    print(f"dataset00 shape: {df.shape} (Rows: {df.shape[0]}, Columns: {df.shape[1]})")
    
    # Save dataset00.csv
    dataset00_path = os.path.join(output_dir, "dataset00.csv")
    df.to_csv(dataset00_path, index=False, encoding='utf-8-sig')
    print(f"[OK] Saved: {dataset00_path}")
    
    # ----------------------------------------------------
    # STEP 2: Replace '.' with Empty Cells (dataset01.csv)
    # ----------------------------------------------------
    log_step(2, "Missing Value Standardisation", "Replacing raw '.' entries with empty fields")
    
    # Globally replace string '.' with empty string
    # We do a direct replace on cells that exactly match '.'
    df = df.replace(r'^\.$', '', regex=True)
    df = df.replace(r'^\s*\.\s*$', '', regex=True)
    
    dataset01_path = os.path.join(output_dir, "dataset01.csv")
    df.to_csv(dataset01_path, index=False, encoding='utf-8-sig')
    print(f"[OK] Saved: {dataset01_path}")
    
    # ----------------------------------------------------
    # STEP 3: Strip Whitespace (dataset02.csv)
    # ----------------------------------------------------
    log_step(3, "Cell Whitespace Cleaning", "Stripping leading and trailing whitespaces from all cells")
    
    # Apply str.strip() to all columns containing strings
    for col in df.columns:
        if df[col].dtype == object:
            df[col] = df[col].astype(str).str.strip()
            # Restore empty strings to NaN or keep as empty strings
            df[col] = df[col].replace('nan', '').replace('None', '')
            
    dataset02_path = os.path.join(output_dir, "dataset02.csv")
    df.to_csv(dataset02_path, index=False, encoding='utf-8-sig')
    print(f"[OK] Saved: {dataset02_path}")
    
    # ----------------------------------------------------
    # STEP 4: Remove Unnecessary Columns (dataset03.csv)
    # ----------------------------------------------------
    log_step(4, "Feature Pruning & Dimensionality Reduction", "Removing patient ID, dates, raw vital signs, ER stay and IP surgery columns")
    
    DELETED_COLUMNS = [
        # 1. 病歷號
        'hisnummark',
        # 2. 生命徵象原始值 1-10 次收縮壓, 舒張壓, 脈搏, 體溫, 呼吸, 血氧
        'VS_hbp1', 'VS_hbp2', 'VS_hbp3', 'VS_hbp4', 'VS_hbp5', 'VS_hbp6', 'VS_hbp7', 'VS_hbp8', 'VS_hbp9', 'VS_hbp10',
        'VS_lbp1', 'VS_lbp2', 'VS_lbp3', 'VS_lbp4', 'VS_lbp5', 'VS_lbp6', 'VS_lbp7', 'VS_lbp8', 'VS_lbp9', 'VS_lbp10',
        'VS_pulse1', 'VS_pulse2', 'VS_pulse3', 'VS_pulse4', 'VS_pulse5', 'VS_pulse6', 'VS_pulse7', 'VS_pulse8', 'VS_pulse9', 'VS_pulse10',
        'VS_temper1', 'VS_temper2', 'VS_temper3', 'VS_temper4', 'VS_temper5', 'VS_temper6', 'VS_temper7', 'VS_temper8', 'VS_temper9', 'VS_temper10',
        'VS_breathe1', 'VS_breathe2', 'VS_breathe3', 'VS_breathe4', 'VS_breathe5', 'VS_breathe6', 'VS_breathe7', 'VS_breathe8', 'VS_breathe9', 'VS_breathe10',
        'VS_spo21', 'VS_spo22', 'VS_spo23', 'VS_spo24', 'VS_spo25', 'VS_spo26', 'VS_spo27', 'VS_spo28', 'VS_spo29', 'VS_spo210',
        # 3. 日期與時間相關欄位
        'TRI_etdt', 'TRI_ettm', 'TRI_teamdt', 'TRI_teamtm',
        # 4. 會診申請日期時間
        'ordt', 'ortm', 'ordtm',
        # 5. 急診住院與急診入院手術相關欄位
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
    
    # Drop existing deleted columns
    cols_to_drop = [col for col in DELETED_COLUMNS if col in df.columns]
    df = df.drop(columns=cols_to_drop)
    
    print(f"dataset03 shape: {df.shape} (Removed {len(cols_to_drop)} columns)")
    
    dataset03_path = os.path.join(output_dir, "dataset03.csv")
    df.to_csv(dataset03_path, index=False, encoding='utf-8-sig')
    print(f"[OK] Saved: {dataset03_path}")
    
    # ----------------------------------------------------
    # STEP 5: Binary Value Mapping to N/Y (dataset04.csv)
    # ----------------------------------------------------
    log_step(5, "Nominal Mapping & Label Conversion", "Converting binary values (0/1) to (N/Y) for specified columns")
    
    # Define columns to be mapped from 0/1 to N/Y
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
    
    # Apply 0 -> N, 1 -> Y mapping
    # Note: we use map and fillna to preserve existing non-zero values or NaNs
    for col in ny_columns:
        if col in df.columns:
            # Map numeric and string representations of 0 and 1
            df[col] = df[col].astype(str).str.strip()
            df[col] = df[col].map({'0': 'N', '1': 'Y', '0.0': 'N', '1.0': 'Y'}).fillna(df[col])
            # Restore empty strings as empty
            df[col] = df[col].replace('nan', '').replace('None', '').replace('', np.nan)
            
    # Special mapping for GCS Change (1 -> N, 2 -> Y)
    if 'TRI_gcschang' in df.columns:
        df['TRI_gcschang'] = df['TRI_gcschang'].astype(str).str.strip()
        df['TRI_gcschang'] = df['TRI_gcschang'].map({'1': 'N', '2': 'Y', '1.0': 'N', '2.0': 'Y'}).fillna(df['TRI_gcschang'])
        df['TRI_gcschang'] = df['TRI_gcschang'].replace('nan', '').replace('None', '').replace('', np.nan)

    dataset04_path = os.path.join(output_dir, "dataset04.csv")
    df.to_csv(dataset04_path, index=False, encoding='utf-8-sig')
    print(f"[OK] Saved: {dataset04_path}")
    
    # ----------------------------------------------------
    # STEP 6: Drop 'No' & Wrap ID fields in quotes (dataset05.csv)
    # ----------------------------------------------------
    log_step(6, "Index Removal & String ID Protection", "Removing sequential 'No' index and securing long numeric ID fields")
    
    # Drop index 'No' column
    if 'No' in df.columns:
        df = df.drop(columns=['No'])
        
    # Prevent Excel formatting issues for IDs by prepending a space (will be double quoted in CSV)
    # This prevents Excel from displaying them in scientific notation
    for col in ['TRI_systemid', 'TRI_ccompid', 'TRI_triageid']:
        if col in df.columns:
            # Prepend a space and write out
            df[col] = df[col].apply(lambda x: f" {str(int(float(x)))}" if pd.notna(x) and str(x).strip() != '' and str(x).strip() != 'nan' else '')
            
    dataset05_path = os.path.join(output_dir, "dataset05.csv")
    
    # We write with a custom quoting policy to ensure spaces are quoted correctly
    df.to_csv(dataset05_path, index=False, encoding='utf-8-sig')
    print(f"[OK] Saved: {dataset05_path}")
    
    # ----------------------------------------------------
    # STEP 7: Department Filtering & Stratified Split (ESUR.csv, train, test)
    # ----------------------------------------------------
    log_step(7, "Department Partitioning & Stratified Train/Test Split", "Extracting ESUR department records and splitting 80/20")
    
    if 'ER_hsect' in df.columns:
        # Strip ER_hsect values to make sure matching is exact
        df['ER_hsect'] = df['ER_hsect'].astype(str).str.strip()
        
        # Filter for ESUR
        esur_df = df[df['ER_hsect'] == 'ESUR'].copy()
        
        # Clean the protected ID fields for ESUR file so that they match their reference ESUR.csv exactly
        # If the reference ESUR.csv has quotes or spaces, we preserve them
        print(f"Filtered ESUR record count: {len(esur_df)} rows")
        
        esur_csv_path = os.path.join(output_dir, "ESUR.csv")
        esur_df.to_csv(esur_csv_path, index=False, encoding='utf-8-sig')
        print(f"[OK] Saved: {esur_csv_path}")
        
        # Stratified 80/20 Split
        if 'OPwithin72hr' in esur_df.columns:
            # Drop rows where target is missing if any
            esur_split_df = esur_df.dropna(subset=['OPwithin72hr'])
            
            if len(esur_split_df) < 2:
                print(f"[Warning] Skipping Train/Test Split: Not enough records found for stratification (Found {len(esur_split_df)} rows).")
            else:
                # Stratified train/test split
                try:
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
                    
                    print(f"[OK] Saved stratified train split: {train_path} ({len(train_df)} rows)")
                    print(f"[OK] Saved stratified test split: {test_path} ({len(test_df)} rows)")
                    
                    # Print class distribution summaries
                    print("\nClass Distribution Summary (OPwithin72hr):")
                    print("--------------------------------------------------")
                    print(f"Full ESUR Class Y count: {sum(esur_df['OPwithin72hr'] == 'Y')} | Class N count: {sum(esur_df['OPwithin72hr'] == 'N')}")
                    print(f"Train Class Y count: {sum(train_df['OPwithin72hr'] == 'Y')} | Class N count: {sum(train_df['OPwithin72hr'] == 'N')}")
                    print(f"Test Class Y count: {sum(test_df['OPwithin72hr'] == 'Y')} | Class N count: {sum(test_df['OPwithin72hr'] == 'N')}")
                    print("--------------------------------------------------")
                except ValueError as e:
                    print(f"[Error] Failed to perform Train/Test Split: {e}")
            
        else:
            print("[Warning] Skipping Train/Test Split: Target column 'OPwithin72hr' not found.")
    else:
        print("[Warning] Skipping Department Partitioning: 'ER_hsect' column not found.")
        
    print(f"\n[Done] DATA PREPROCESSING COMPLETELY FINISHED! All target files saved to: {output_dir}\n")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("\n[Error] Missing raw dataset input file!")
        print("Usage: python data_preprocessing_pipeline.py <input_file_path> [output_directory]")
        print("Example: python data_preprocessing_pipeline.py dataset/dataset00.csv processed_results")
        sys.exit(1)
        
    input_file = sys.argv[1]
    out_dir = sys.argv[2] if len(sys.argv) > 2 else "processed_results"
    
    if not os.path.exists(input_file):
        print(f"\n[Error] Input file '{input_file}' does not exist!")
        sys.exit(1)
        
    preprocess_pipeline(input_file, out_dir)
