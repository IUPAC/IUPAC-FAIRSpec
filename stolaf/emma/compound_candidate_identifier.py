import pandas as pd
from pathlib import Path
from collections import Counter, defaultdict
import numpy as np
import re

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline

# Read file paths

with open("file_list_3tx95x6sq.txt", "r") as f:
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

IGNORE_FOLDERS = {
    "pdata", "fid", "ser", "used_from",
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
    "13c", "1h", "cosy", "hsqc", "hmbc", "dept", "jmod", "noesy", "13c jmod"
]

FILE_EXTENSIONS = {
    ".txt", ".par", ".fid", ".ser", ".json", ".xml", ".temp", ".png", ".info"
}


# Initial candidate extraction (loose)

def is_file_like(name):
    n = name.lower()
    return (
        n in FILE_LIKE_NAMES
        or any(n.endswith(ext) for ext in FILE_EXTENSIONS)
    )

  
def contains_experiment_token(name):
    n = name.lower()
    return any(k in n for k in EXPERIMENT_KEYWORDS)
  
#def contains_experiment_token_twice_same(name):
#    n = name.lower()
#    pattern = r"\b(" + "|".join(EXPERIMENT_KEYWORDS) + r")\b"
#   matches = re.findall(pattern, n)
#    print(len(matches))
#    print(matches)
##    if len(matches) == len(set(matches)):
#      return False
#    else:
#      return len(matches) >= 2
    #return any(k in n for k in EXPERIMENT_KEYWORDS)
  
"""
def is_pure_experiment_folder(name):
  
    #Folder is mostly an experiment name (not a compound).
  
    n = name.lower()

    return (
        contains_experiment_token(n) and
        not contains_experiment_token_twice_same(n)
        #and len(n.split()) <= 3
    )
"""
   
def is_pure_experiment_folder(name):
    """
    True if the folder name represents ONLY an experiment,
    not a compound-experiment hybrid.
    """
    n = name.lower().strip()

    # Must contain an experiment token
    if not contains_experiment_token(n):
        return False

    # Remove experiment tokens and junk
    cleaned = n
    for k in EXPERIMENT_KEYWORDS:
        cleaned = cleaned.replace(k, "")

    cleaned = re.sub(r"[^a-z0-9]+", "", cleaned)

    # If nothing meaningful remains, it's a pure experiment folder
    return len(cleaned) == 0   
   

# im adding this

 
  

  
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
  

def is_basic_candidate(folder):
    f = folder.strip()
    if is_file_like(f):
        return False
    if f.lower() in (x.lower() for x in IGNORE_FOLDERS):
        return False
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
    if is_file_like(folder):
        return False
    if folder.lower() in (x.lower() for x in IGNORE_FOLDERS):
        return False
    if folder in GLOBAL_FOLDERS:
        return False
    return True

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


def get_relative_positions(parts):
    if "pdata" not in parts:
        return {}
    
    idx = parts.index("pdata")
    # Creates a dictionary: {folder_name: relative_offset}
    return {parts[i]: i - idx for i in range(len(parts))}

df["structural_candidate"] = df["parts"].apply(get_relative_positions)

# this is from something else
df["path_token_counts"] = df["parts"].apply(path_token_counts)


def choose_deepest_adjacent_compound(parts, compound_candidates):
    """
    If two compound candidates appear next to each other in the path,
    choose the second one and invalidate the first.
    
     I JUST FLIPPED IT TO a, b RETURNING
    """
    candidate_set = set(compound_candidates)
    
    print("candidate set")
    print(candidate_set)

    for i in range(len(parts) - 1):
        a, b = parts[i], parts[i + 1]

        if a in candidate_set and b in candidate_set:
            # want to print the frequency of the labels maybe
          #  print(
              #  f"[RULE] Adjacent compound folders detected: "
              #  f"'{a}' -> '{b}'. "
              #  f"Selecting '{b}', invalidating '{a}'."
          #  )
            return b, a  # chosen, invalidated

    return None, None





# ==================== TEST ====================
print("~~~~~~~~~~~~~~~~STRUCTUAL DATA WITH PDATA FOLDER~~~~~~~~~~~~~~~~")

#print("=============df structural candidate========")
#print(df["structural_candidate"])

#print("=============best_structural candidate========")
#print(df["structural_candidate"])

print("=============df structural position========")
#with pd.option_context('display.max_rows', None, 'display.max_columns', None):
#  print(df["structural_positions"])
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




def choose_weak_label(row, order, min_freq=3, max_frac=0.5):
    parts = row["parts"]
    candidates = row["compound_candidates"]
    
    #print("+++++++++++++++++++++++++++")
    #print(parts)
    #print(candidates)
    
    #chosen, invalidated = choose_deepest_adjacent_compound(parts, candidates)
    #if chosen:
    #  return chosen
    
    # STRONG RULE: clean parent of experiment wins
    parent = clean_parent_of_experiment(parts, candidates)
    if parent and not is_file_like(parent):
        return parent
        

    
    """
    #print(order)
    if order == 2:
      chosen, invalidated = choose_deepest_adjacent_compound(parts, candidates)
      if chosen:
          return chosen
  
      # STRONG RULE: clean parent of experiment wins
      parent = clean_parent_of_experiment(parts, candidates)
      if parent and not is_file_like(parent):
          return parent
    elif order == 1:
      # STRONG RULE: clean parent of experiment wins
      parent = clean_parent_of_experiment(parts, candidates)
      if parent and not is_file_like(parent):
          return parent
        
      chosen, invalidated = choose_deepest_adjacent_compound(parts, candidates)
      if chosen:
        return chosen

    """

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
        if is_pure_experiment_folder(c):
            continue
        if (
            candidate_freq[c] >= min_freq and
            candidate_freq[c] / TOTAL_PATHS <= max_frac 
            #and not contains_experiment_token(c)
        ):
            return c
        # I'm trying to add this chunk
      #  elif (
      #      candidate_freq[c] >= min_freq and
      #      candidate_freq[c] / TOTAL_PATHS <= max_frac and
      #      contains_experiment_token(c) and
      #      not contains_experiment_token_twice_same(c)
      #  ):
      #      return c

    return None

print("HELLOOooooooo")

#df["compound_label1"] = df.apply(choose_weak_label, axis=1, order=1)

#print(df["compound_label1"])


#df["compound_label2"] = df.apply(choose_weak_label, axis=1, order=2)

#print(df["compound_label2"])


df["compound_label"] = df.apply(choose_weak_label, axis=1, order=1)

print(df["compound_label"])

#df["compound_label"] = df["compound_label1"]


labeled_df = df.dropna(subset=["compound_label"]).copy()

print(f"\nWeakly labeled rows: {len(labeled_df)} / {TOTAL_PATHS}")
print("Top labels:")
print(labeled_df["compound_label"].value_counts().head())


#print("where are we here")
#order_1 = labeled_df["compound_label1"].value_counts().head()
#order_2 = labeled_df["compound_label2"].value_counts().head()

#print(order_1)
#print(order_2)

# ==================== TEST ====================
print("~~~~~~~~~~~~~~~~Weak Labeling~~~~~~~~~~~~~~~~")

print("=============df compound label========")
print(df["compound_label"])

print("=============labeled df========")
print(labeled_df)

# ==============================================




n_classes = labeled_df["compound_label"].nunique()


# ==================== TEST ====================
print("~~~~~~~~~~~~~~~~TRAIN ML MODEL~~~~~~~~~~~~~~~~")

print("=============n classes========")
print(n_classes)

print("=============n class names========")
print(labeled_df["compound_label"].unique())

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


print(labeled_df["compound_label"].unique())

print(labeled_df["compound_label"])



# this logic is not sound proof at all

to_remove = [
    '1H', '13C Jmod', 'HSQC', 'COSY', '13C'
]

# Keep rows where the value is NOT in the list
df_filtered = labeled_df[~labeled_df["compound_label"].isin(to_remove)]


print(df_filtered)
print(df_filtered["compound_label"])
print(df_filtered["compound_label"].unique())



"""
compound_contenders = df_filtered["compound_label"].unique()
print(paths)

print(df)


for row in df["parts"]:
  print(row)
  for compound_contender in compound_contenders:
    if compound_contender in row:
      print(compound_contender)
      
      
print(get_relative_positions(df["parts"]))

print(df["structural_candidate"])

for i in df["structural_candidate"]:
  if i != {}:
    print(i)

"""

#print(candidate_freq)
