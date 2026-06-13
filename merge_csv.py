"""
Simple CSV merge script: combines EMED_train.csv and ESUR_train.csv
into one merged_data.csv by appending rows (both have identical headers).
"""

import csv
import os

dataset_dir = r"D:\programFile\IDEA\IntelliJ IDEA 2025.2.1\code\72hourPrediction\dataset"

file1 = os.path.join(dataset_dir, "EMED_train.csv")
file2 = os.path.join(dataset_dir, "ESUR_train.csv")
output = os.path.join(dataset_dir, "merged_data.csv")

count = 0

with open(output, "w", newline="", encoding="utf-8-sig") as fout:
    writer = None
    for path in [file1, file2]:
        with open(path, "r", newline="", encoding="utf-8-sig") as fin:
            reader = csv.DictReader(fin)
            if writer is None:
                fieldnames = reader.fieldnames
                writer = csv.DictWriter(fout, fieldnames=fieldnames)
                writer.writeheader()
                print(f"Header columns: {len(fieldnames)}")
            for row in reader:
                writer.writerow(row)
                count += 1
        print(f"  Done reading: {os.path.basename(path)}")

print(f"\n[OK] merged_data.csv written with {count} data rows -> {output}")
