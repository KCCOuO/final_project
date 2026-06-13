import base64
import json
import urllib.request

mermaid_code = """graph TD
    classDef main fill:#e1f5fe,stroke:#039be5,stroke-width:2px,color:#000
    classDef sub fill:#fff,stroke:#b3e5fc,stroke-width:1px,color:#333
    classDef data fill:#f3e5f5,stroke:#8e24aa,stroke-width:2px,color:#000
    classDef model fill:#e8f5e9,stroke:#43a047,stroke-width:2px,color:#000

    RawData[(Raw Hospital Data CSV)]:::data

    subgraph Phase 1: Data Preprocessing
        direction LR
        Merge[CSV Loading & Schema Detection]:::sub
        AntiOOM[Anti-OOM: Remove High Cardinality]:::sub
        MissingValues[Data Imputation: ReplaceMissingValues]:::sub
        Merge --> AntiOOM --> MissingValues
    end

    subgraph Phase 2: Model Training
        direction LR
        WekaEngine[Weka ML Engine Integration]:::sub
        Compare[Algorithm Comparison: RF, J48, etc.]:::sub
        Eval[10-Fold CV & Recall R1 Evaluation]:::sub
        WekaEngine --> Compare --> Eval
    end

    subgraph Phase 3: Clinical Prediction
        direction LR
        Load[Model Loading & Parsing]:::sub
        Batch[Batch Prediction]:::main
        Single[Single Patient Form]:::main
        Load --> Batch
        Load --> Single
    end
    
    TrainedModel((Trained Predictive Model .zip)):::model
    OutBatch[Output Results CSV\\nwith Confidence %]:::data
    OutSingle[GUI Popup\\nRisk & Confidence %]:::data

    RawData --> Merge
    MissingValues --> WekaEngine
    Eval -.->|Export Model Archive| TrainedModel
    TrainedModel -.-> Load
    Batch --> OutBatch
    Single --> OutSingle
"""

state = {
    "code": mermaid_code,
    "mermaid": {"theme": "default"}
}
json_state = json.dumps(state)
b64_state = base64.urlsafe_b64encode(json_state.encode('utf-8')).decode('utf-8')
url = f"https://mermaid.ink/img/{b64_state}"
output_path = r"D:\programFile\IDEA\IntelliJ IDEA 2025.2.1\code\0519\architecture_english_block.png"

try:
    print("Downloading image from mermaid.ink...")
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req) as response, open(output_path, 'wb') as out_file:
        out_file.write(response.read())
    print(f"SUCCESS: Image saved to {output_path}")
except Exception as e:
    print(f"FAILED: {e}")
