import pandas as pd
from pathlib import Path
from collections import Counter, defaultdict
import numpy as np
import re

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline

# Read file paths

with open("file_listb.txt", "r") as f:
    paths = [line.strip() for line in f if line.strip()]

path_parts = [list(Path(p).parts) for p in paths]

df = pd.DataFrame({"parts": path_parts})
df["path_text"] = df["parts"].apply(lambda x: " / ".join(x))

TOTAL_PATHS = len(df)

# ==================== TEST ====================
print("~~~~~~~~~~~~~~~~READ FILE PATHS~~~~~~~~~~~~~~~~")
print("===========df====================")
print(df)

print("=============df path text========")
print(df["path_text"])
# ==============================================


# Domain knowledge

# NOTE !!!!!!!!!!!!!!!!!!!!!!!
# - Need to figure out how to exclude files that have extensions (def not compound names, etc...)
# - Use the relation of file names with extensions to identify compound names/experiments

IGNORE_FOLDERS = {
    "pdata", "fid", "ser", "used_from",
#    "COSY", "HSQC", "HMBC", "NOESY",
#    "1", "2", "3", "4", "5",
#    "test2", ".", "..",
#    "13C Jmod", "1H", "13C", "DEPT", "JMOD"
}

FILE_LIKE_NAMES = {
    "acqu", "acqus", "acqu2s",
    "proc", "procs",
    "specpar",
    "title",
    "outd",
    "shimvalues",
    "scon2",
    "pulseprogram",
    "cpdprg2",
    "precom.output",
    "fq1list",
    "vtc_pid_settings"
}


EXPERIMENT_KEYWORDS = [
    "13c", "1h", "cosy", "hsqc", "hmbc", "dept", "jmod", "noesy", "13C", "1H"
]

FILE_EXTENSIONS = {
    ".txt", ".par", ".fid", ".ser", ".json", ".xml", ".temp", ".png", ".info"
}

KNOWN_PARAM_FILES = {
    "acqus", "acqu2s", "procpar", "pulseprogram"
}

# Initial candidate extraction (loose)

def is_file_like(name):
    n = name.lower()
    return (
        n in FILE_LIKE_NAMES
        or any(n.endswith(ext) for ext in FILE_EXTENSIONS)
    )

def has_file_extension(name):
    p = Path(name)
    return p.suffix != "" and len(p.suffix) <= 5
  
def contains_experiment_token(name):
    n = name.lower()
    return any(k in n for k in EXPERIMENT_KEYWORDS)
  
def is_pure_experiment_folder(name):
    """
    Folder is mostly an experiment name (not a compound).
    """
    n = name.lower()
    return (
        contains_experiment_token(n)
        and len(n.split()) <= 3
    )
  
def label_in_path(label, path_text):
    label_norm = re.sub(r"[\s\-_]+", "", label.lower())
    path_norm = re.sub(r"[\s\-_]+", "", path_text.lower())
    return label_norm in path_norm
  
def normalize_token(s):
    """Normalize folder text into a comparable token string."""
    return re.sub(r"[^a-z0-9]+", "", s.lower())

def extract_core_tokens(folder):
    """
    Extract meaningful alphanumeric tokens from a folder name.
    """
    return [
        normalize_token(t)
        for t in re.split(r"[\s\-_]+", folder)
        if len(t) >= 2
    ]
    
def path_token_counts(parts):
    counts = Counter()
    for p in parts:
        for t in extract_core_tokens(p):
            counts[t] += 1
    return counts
  
def infer_compound_without_pdata(parts):
    """
    Handles paths like:
    compound / compound EXPERIMENT / acqus
    """

    for i in range(len(parts) - 1):
        current = parts[i]
        next_part = parts[i + 1]

        # If next folder looks like experiment or file
        if contains_experiment_token(next_part) or is_file_like(next_part):
            # Prefer clean parent
            if is_candidate(current) and not contains_experiment_token(current):
                return current

    return None
  
def experiment_parent_candidate(parts):
    for i in range(len(parts) - 1):
        if contains_experiment_token(parts[i + 1]):
            parent = parts[i]
            if is_candidate(parent):
                return parent
    return None
  
def is_basic_candidate(folder):
    f = folder.strip()
    if is_file_like(f):
        return False
    if f.lower() in (x.lower() for x in IGNORE_FOLDERS):
        return False
    #if f.isdigit():
    #    return False
    #if len(f) < 3:
    #    return False
    return True

df["initial_candidates"] = df["parts"].apply(
    lambda parts: [p for p in parts if is_basic_candidate(p)]
)

# ==================== TEST ====================
print("~~~~~~~~~~~~~~~~INITIAL CANDIDATE EXTRACTION~~~~~~~~~~~~~~~~")
print("===========df initial candidates====================")
print(df["initial_candidates"])

print("=============df parts========")
print(df["parts"])
# ==============================================


# Detect and exclude global folders (project-level)

all_initial_candidates = [
    c for sublist in df["initial_candidates"] for c in sublist
]

candidate_freq = Counter(all_initial_candidates)

GLOBAL_THRESHOLD = 0.8  # appears in ≥80% of paths

GLOBAL_FOLDERS = {
    folder for folder, count in candidate_freq.items()
    if count / TOTAL_PATHS >= GLOBAL_THRESHOLD
}

print("\nDetected GLOBAL folders (excluded):")
for g in sorted(GLOBAL_FOLDERS):
    print(" ", g)

def is_candidate(folder):
    if folder in GLOBAL_FOLDERS:
        return False
    return is_basic_candidate(folder)

# Re-extract candidates after removing globals
df["compound_candidates"] = df["parts"].apply(
    lambda parts: [p for p in parts if is_candidate(p)]
)

# ==================== TEST ====================
print("~~~~~~~~~~~~~~~~DETECT AND EXCLUDE GLOBAL FOLDERS~~~~~~~~~~~~~~~~")
#print("===========all initial candidates====================")
#print(all_initial_candidates)

print("=============candidate frequency========")
print(candidate_freq)

print("=============GLOBAL FOLDERS========")
print(GLOBAL_FOLDERS)

print("=============df compound candidates========")
print(df["compound_candidates"])
# ==============================================


# Structural data with pdata folder

# NOTE !!!!!!!!!!!!!!!!!
# Want to add more of these structural anchors

"""
position_counts = defaultdict(Counter)

for parts in df["parts"]:
    if "pdata" in parts:
        idx = parts.index("pdata")
        for d in range(1, 4):
            if idx - d >= 0:
                position_counts[d][parts[idx - d]] += 1

def structural_scores(parts):
    scores = {}
    if "pdata" not in parts:
        return scores
    idx = parts.index("pdata")
    for d in range(1, 4):
        if idx - d >= 0:
            folder = parts[idx - d]
            scores[folder] = position_counts[d][folder]
    return scores

def best_structural_candidate(parts):
    scores = structural_scores(parts)
    #print(scores)
    return max(scores, key=scores.get) if scores else None

df["structural_candidate"] = df["parts"].apply(best_structural_candidate)

"""


def get_relative_positions(parts):
    if "pdata" not in parts:
        return {}
    
    idx = parts.index("pdata")
    # Creates a dictionary: {folder_name: relative_offset}
    return {parts[i]: i - idx for i in range(len(parts))}


# Apply to create a column of position dictionaries
df["structural_positions"] = df["parts"].apply(get_relative_positions)

# adding this line to not change the names of things
df["structural_candidate"] = df["parts"].apply(get_relative_positions)

# this is from something else
df["path_token_counts"] = df["parts"].apply(path_token_counts)



def has_clean_parent(candidate, parts):
    """
    Returns True if a shorter folder exists in the same path
    that is contained within the candidate name.
    """
    cand_norm = normalize_token(candidate)

    for p in parts:
        if p == candidate:
            continue
        p_norm = normalize_token(p)

        if p_norm and p_norm in cand_norm and len(p_norm) < len(cand_norm):
            return True

    return False


def structural_compound_score(row):
    parts = row["parts"]
    positions = row["structural_positions"]
    token_counts = row["path_token_counts"]

    scores = defaultdict(float)

    for i, folder in enumerate(parts):

        if not is_candidate(folder):
            continue

        folder_lower = folder.lower()
        folder_tokens = extract_core_tokens(folder)
        
        for t in folder_tokens:
          if token_counts[t] > 1:
            scores[folder] += 2 * (token_counts[t]-1)
        
        if contains_experiment_token(folder_lower):
            scores[folder] -= 3
            
        # Prefer folders above pdata
        if folder in positions:
            rel = positions[folder]

            if -3 <= rel <= -1:
                scores[folder] += 3 - abs(rel)

        # If next folder looks like experiment → parent is compound
        if i < len(parts) - 1:
            if contains_experiment_token(parts[i + 1]):
                scores[folder] += 2
                
        if len(folder_tokens) > 1:
          for t in folder_tokens:
            if token_counts[t] > 1:
              scores[folder] -= 1
    return scores




# ==================== TEST ====================
print("~~~~~~~~~~~~~~~~STRUCTUAL DATA WITH PDATA FOLDER~~~~~~~~~~~~~~~~")

#print("=============df structural candidate========")
#print(df["structural_candidate"])

#print("=============best_structural candidate========")
#print(df["structural_candidate"])

print("=============df structural position========")
with pd.option_context('display.max_rows', None, 'display.max_columns', None):
  print(df["structural_positions"])
# ==============================================


# Weak labeling (safe version)

def clean_parent_of_experiment(parts, candidates):
    """
    Returns a clean candidate that is the parent of an experiment folder.
    """
    for i in range(len(parts) - 1):
        parent = parts[i]
        child = parts[i + 1]

        if (
            parent in candidates
            and is_candidate(parent)
            and not contains_experiment_token(parent)
            and contains_experiment_token(child)
        ):
            return parent
    return None


""""
def choose_weak_label(row, min_freq=5, max_frac=0.5):
    ranked = sorted(
        row["compound_candidates"],
        key=lambda x: (
			row["parts"].index(x),
            -candidate_freq[x]
        )
    )
    for c in ranked:
        if (
            candidate_freq[c] >= min_freq and
            candidate_freq[c] / TOTAL_PATHS <= max_frac
        ):
            return c
    return None

"""

def choose_weak_label(row, min_freq=3, max_frac=0.5):
    parts = row["parts"]
    candidates = row["compound_candidates"]

    # STRONG RULE: clean parent of experiment wins
    parent = clean_parent_of_experiment(parts, candidates)
    if parent and not is_file_like(parent):
        return parent

    # Otherwise, fall back to frequency-based heuristic
    ranked = sorted(
        candidates,
        key=lambda x: (
            row["parts"].index(x),
            -candidate_freq[x]
        )
    )

    for c in ranked:
        if is_file_like(c):
            continue
        if (
            candidate_freq[c] >= min_freq and
            candidate_freq[c] / TOTAL_PATHS <= max_frac and
            not contains_experiment_token(c)
        ):
            return c

    return None



df["compound_label"] = df.apply(choose_weak_label, axis=1)

labeled_df = df.dropna(subset=["compound_label"]).copy()

print(f"\nWeakly labeled rows: {len(labeled_df)} / {TOTAL_PATHS}")
print("Top labels:")
print(labeled_df["compound_label"].value_counts().head())

# ==================== TEST ====================
print("~~~~~~~~~~~~~~~~Weak Labeling~~~~~~~~~~~~~~~~")

print("=============df compound label========")
print(df["compound_label"])

print("=============labeled df========")
print(labeled_df)

# ==============================================


# Train ML Model (if valid)

n_classes = labeled_df["compound_label"].nunique()

if n_classes < 2:
    raise RuntimeError(
        f"Cannot train ML model: only {n_classes} class found."
    )

pipeline = Pipeline([
    ("tfidf", TfidfVectorizer(
        token_pattern=r"[^/ ]+",
        ngram_range=(1, 2),
        min_df=2
    )),
    ("clf", LogisticRegression(
        max_iter=2000,
        class_weight="balanced"
    ))
])

pipeline.fit(
    labeled_df["path_text"],
    labeled_df["compound_label"]
)


# ==================== TEST ====================
print("~~~~~~~~~~~~~~~~TRAIN ML MODEL~~~~~~~~~~~~~~~~")

print("=============n classes========")
print(n_classes)

print("=============n class names========")
print(labeled_df["compound_label"].unique())

print("=============pipeline========")
print(pipeline)

# ==============================================


# Extra thing I'm writing

class_name_counts = labeled_df["compound_label"].value_counts()
class_name_average = class_name_counts.mean()

occurrence_per_row = df.groupby('compound_label')['compound_label'].transform('count')
print("\nOccurrence count per row:")
print(occurrence_per_row)

# Filter the original DataFrame for rows where the count is greater than or equal to the average
df = df[occurrence_per_row >= class_name_average- (class_name_average/4)]

print("\nFiltered DataFrame (categories with >= average occurrences):")
print(df)

# ML Predictions (Top-K)

"""
def top_k_predictions(model, texts, k=3):
    probs = model.predict_proba(texts)
    classes = model.classes_
    output = []
    for row in probs:
        idx = np.argsort(row)[::-1][:k]
        output.append([(classes[i], float(row[i])) for i in idx])
    return output
"""

def top_k_predictions(model, texts, paths, parts_list, k=5):
    probs = model.predict_proba(texts)
    classes = model.classes_

    output = []
    for row_probs, path_text, parts in zip(probs, paths, parts_list):
        ranked = sorted(
            [(classes[i], float(row_probs[i])) for i in range(len(classes))],
            key=lambda x: x[1],
            reverse=True
        )

        # keep only labels present in this path
        """
        filtered = [
            (c, p) for c, p in ranked
            if label_in_path(c, path_text)
        ][:k]

        """
        
        filtered = []
        for c, p in ranked:
            if not label_in_path(c, path_text):
                continue
        
            # Reject experiment-contaminated labels
            if contains_experiment_token(c):
                # Allow only if no clean parent exists
                if has_clean_parent(c, parts):
                    continue
        
            filtered.append((c, p))
        
            if len(filtered) == k:
                break
      
      
        output.append(filtered)

    return output


df["ml_top_candidates"] = top_k_predictions(
    pipeline,
    df["path_text"],
    df["path_text"],
    df["parts"],
    k=5
)

# ==================== TEST ====================
print("~~~~~~~~~~~~~~~~ML PREDICTIONS (TOP K)~~~~~~~~~~~~~~~~")

print("=============df ml top candidates========")
print(df["ml_top_candidates"])
# ==============================================

# Combind ML + Structure


"""
def final_decision(row):
    structural = row["structural_candidate"]
    ml = row["ml_top_candidates"]

    if structural:
        for c, p in ml:
            #print(c)
            #print(p)
            #print(structural)
            if c == structural:
                return c, p, "ML+STRUCTURE"

    return ml[0][0], ml[0][1], "ML_ONLY"
"""

"""
def final_decision(row):
    ml = row["ml_top_candidates"]
    structural_scores = structural_compound_score(row)

    # Boost ML probabilities using structure
    combined = {}

    for c, p in ml:
        combined[c] = p

    for c, s in structural_scores.items():
        combined[c] = combined.get(c, 0) + 0.15 * s

    # Pick best
    best = max(combined.items(), key=lambda x: x[1])

    source = "ML_ONLY"
    if best[0] in structural_scores:
        source = "ML+RELATIVE_STRUCTURE"

    return best[0], best[1], source
"""


def final_decision(row):
    parts = row["parts"]
    path_text = row["path_text"]

    if "pdata" not in parts:
        inferred = infer_compound_without_pdata(parts)
        if inferred and label_in_path(inferred, path_text):
            return inferred, 0.6, "STRUCTURE_NO_PDATA"

    # Only allow labels that actually appear in the path
    valid_ml = [
        (c, p) for c, p in row["ml_top_candidates"]
        if label_in_path(c, path_text)
    ]

    # Structural candidate must also appear in path
    structural = row["structural_candidate"]
    if isinstance(structural, dict):
        # pick best structural candidate that appears in path
        structural = next(
            (k for k in structural if label_in_path(k, path_text)),
            None
        )

    # Prefer ML + STRUCTURE agreement
    if structural:
        for c, p in valid_ml:
            if c == structural:
                return c, p, "ML+STRUCTURE+PATH"

    # Otherwise best ML candidate that appears in path
    if valid_ml:
        return valid_ml[0][0], valid_ml[0][1], "ML+PATH"

    # Final fallback: best in-path compound candidate
    for c in row["compound_candidates"]:
        if label_in_path(c, path_text):
            return c, 0.05, "FALLBACK_PATH_ONLY"
          
    parent = experiment_parent_candidate(row["parts"])
    if parent and label_in_path(parent, row["path_text"]):
        return parent, 0.15, "STRUCTURE_PARENT_FALLBACK"

    # Nothing valid
    return None, 0.0, "NO_VALID_CANDIDATE"


df[["predicted_compound", "confidence", "source"]] = df.apply(
    lambda r: pd.Series(final_decision(r)),
    axis=1
)

# ==================== TEST ====================
print("~~~~~~~~~~~~~~~~COMBIND ML + STRUCTURE~~~~~~~~~~~~~~~~")

print("=============final thingy========")
print(df[["predicted_compound", "confidence", "source"]])
# ==============================================

# Save Outputs


df_out = df[[
    "path_text",
    "predicted_compound",
    "confidence",
    "source",
    "ml_top_candidates",
    "structural_candidate"
]]

df_out.to_csv("predicted_compoundsb.csv", index=False)
df.to_csv("parsed_pathsb.csv", index=False)

print("\nDone.")
print("Outputs written:")
print(" - parsed_paths.csv")
print(" - predicted_compounds.csv")


print(labeled_df["compound_label"].value_counts().head(20))

print(labeled_df["compound_label"].unique())
# ========================================================== PART 2 =================================================
# ========================================================== PART 2 =================================================
# ========================================================== PART 2 =================================================
# ========================================================== PART 2 =================================================
# ========================================================== PART 2 =================================================
# ========================================================== PART 2 =================================================


for compound in labeled_df["compound_label"].unique():
  print(compound)
  


