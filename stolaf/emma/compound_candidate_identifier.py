import pandas as pd
from pathlib import Path
from collections import Counter, defaultdict
import numpy as np
import re

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline

# Read file paths

with open("file_list_v6wwpzh7x.txt", "r") as f:
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
df = df[occurrence_per_row >= class_name_average- (class_name_average/2)]

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


with pd.option_context('display.max_rows', 20, 'display.max_columns', None):
    print(df_filtered)


# PART TWO STARTS HERE

print(df["path_text"])

def count_slashes_before(row, path_col, compound_col):
  path = str(row[path_col]).replace('\\', '/')
  compound = str(row[compound_col])
  print(compound)
  idx = path.lower().find(compound.lower())
  if idx == -1:
    return None
    
  return path[:idx].count('/')


df['slashes_before'] = df.apply(
    lambda row: count_slashes_before(row, 'path_text','compound_label'),
    axis=1
)

print(df[['path_text', 'compound_label', 'slashes_before']])
    
  
print(df['slashes_before'].unique())

matches_list = df['path_text'].str.findall(r".*/.*/.*/ 5r")
print("\nList of matches for each row:")
print(matches_list)

print(df['path_text'])

#for path in paths:
#  if re.search(r"\.zip$", path):
#    print(path)
    
    
pattern = r"([^/]+)\.zip$"
#zipped_filenames = [re.search(pattern, p).group(1) for p in paths if re.search(pattern, p)]
#print(zipped_filenames)
    
zipped_filenames = []
for p in paths:
    match = re.search(pattern, p)
    if match:
        name = match.group(1)
        zipped_filenames.append(f"{name}")
        if "_" in name:
            zipped_filenames.append(f"{name.replace('_', ' ')}")
        elif " " in name:
            zipped_filenames.append(f"{name.replace(' ', '_')}")

zipped_filenames = list(dict.fromkeys(zipped_filenames))    
print(zipped_filenames)

# trying next step

print(df["slashes_before"])

"""
def get_filepaths_to_compounds():
  for slashes in df["slashes_before"].unique():
    for i, folder in enumerate(path_parts):
      print(f"List {i}: {folder}")
      if len(folder) > slashes:
          print(f"  Index 2: {folder[slashes]}")
          if folder[slashes] in df["compound_label"].unique():
            print( folder[0:slashes+1])
            return folder[0:slashes+1]
"""



def get_filepaths_to_compounds(df, path_parts):
    extracted_data = []
    
    valid_labels = set(df["compound_label"].unique())
    unique_slashes = df["slashes_before"].unique()

    for slashes in unique_slashes:
        for folder in path_parts:
            # check if the folder list is long enough to reach the compound index
            if len(folder) > slashes:
                potential_compound = folder[slashes]
                
                # If this index contains an identified compound
                if potential_compound in valid_labels:
                    full_path = folder[0:slashes+1]
                    extracted_data.append({
                        "compound_path": full_path,
                        "identified_compound": potential_compound
                    })

    # Create the new DataFrame
    new_df = pd.DataFrame(extracted_data)
    return new_df
  
new_df = get_filepaths_to_compounds(df, path_parts)
print(new_df)



# trying to create the regex

def create_path_pattern(compound_path_list, zipped_list):
    transformed_parts = []
    
    for part in compound_path_list:
        print(part)
        print(zipped_list)
        # if the folder name is in our zip list, add .zip extension
        if part == "..":
          continue
        if part == "test2":
          continue
        if part in zipped_list:
            transformed_parts.append(f"{part}.zip|{part}")
        else:
            transformed_parts.append(part)
    
    base_path = "/".join(transformed_parts).strip("/")
    return f"{base_path}/" if base_path else ""
  
  
  
unique_paths = new_df["compound_path"].drop_duplicates()


file_content = [
  "Path to Compounds\n",
]


path_to_compounds = []
for path_list in unique_paths:
    base_path = create_path_pattern(path_list, zipped_filenames)
    print(base_path)
    file_content.append(base_path)
    file_content.append("\n")
    path_to_compounds.append(base_path)


# try to do maybe the estimated path

def mask_filenames(input_list):
    output = []
    for entry in input_list:

        parts = re.split(r'([|/])', entry)
        
        masked_parts = []
        for part in parts:
            if not part:
                continue
            if part in ['|', '/']:
                masked_parts.append(part)
            elif part.endswith('.zip'):
                masked_parts.append('*.zip')
            else:
                masked_parts.append('*')
        
        output.append("".join(masked_parts))
    return output

result = mask_filenames(path_to_compounds)
print(result)

print(set(result))

file_content.append("\nPossible file path: \n")
file_content.append(str(set(result)))


# get the contents of each compound
# get filepaths after compounds
def get_filepaths_after_compounds(df, path_parts):
    extracted_data = []
  
    valid_labels = set(df["compound_label"].unique())
    unique_slashes = df["slashes_before"].unique()

    for slashes in unique_slashes:
        for folder in path_parts:
           
            if len(folder) > slashes:
                potential_compound = folder[slashes]

                if potential_compound in valid_labels:

                    full_path = folder[slashes:]
                    # Store as a dictionary for the new DataFrame
                    extracted_data.append({
                        "compound_path": full_path,
                        "identified_compound": potential_compound
                    })

    new_df = pd.DataFrame(extracted_data)
    return new_df
  
diff_df = get_filepaths_after_compounds(df, path_parts)
print(diff_df)
IMPORTANT_FILES = [
  ".mol", ".jdf"
]
for file_type in diff_df["compound_path"]:
  #print(file_type[len(file_type)-1])
  if any(keyword in file_type[len(file_type)-1] for keyword in IMPORTANT_FILES):
    file_content.append("\n")
    file_content.append(file_type[0])
    file_content.append("\n")
    file_content.append(file_type[len(file_type)-1])
    file_content.append("\n")
    
    
# creating the df of all of the information

final_df = df["compound_label"].unique()
final_df = pd.DataFrame(final_df, columns=['compound'])
print(final_df)

print("identifying")
    
# identifying if there are test folders within compound folders

#print(diff_df["compound_path"])

#for file_path in diff_df["compound_path"]:
#  if len(file_path) > 2:
#    test_folder = file_path[1]
    
test_folder_map = {}

for _, row in diff_df.iterrows():
    path_parts = row["compound_path"]
    compound = row["identified_compound"]
    
    # Check if there is a folder inside the compound folder
    # If compound is at index 3, the test folder is at index 4
    if len(path_parts) > 2:
        folder_name = path_parts[1]
        
        if compound not in test_folder_map:
            test_folder_map[compound] = set()
        test_folder_map[compound].add(folder_name)

# 3. Map the discovered folders back to final_df as a list
final_df['test_folders'] = final_df['compound'].map(
    lambda x: list(test_folder_map.get(x, []))
)





def list_useful_files(diff_df):
    # Dictionary to store files: {compound: {test_folder: [files]}}
    useful_file_map = {}

    useful_extensions = ('.mol', '.jdf' ,'.mnova', 'fid', 'pdata')

    for _, row in diff_df.iterrows():
        path_parts = row["compound_path"]
        compound = row["identified_compound"]
        if len(path_parts) > 2:
            test_folder = path_parts[1]
            
            for file_index in range(len(path_parts)-1):
              filename = path_parts[file_index+1]
              
              # Filter for useful files
              if filename.lower().endswith(useful_extensions):
                  if compound not in useful_file_map:
                      useful_file_map[compound] = {}
                 
                  if test_folder not in useful_file_map[compound]:
                      useful_file_map[compound][test_folder] = []
                  
                  if filename not in useful_file_map[compound][test_folder]:
                    useful_file_map[compound][test_folder].append(filename)
          
                  continue
        else:
          test_folder = "NA"
          for file_index in range(len(path_parts)-1):
            filename = path_parts[file_index+1]
            if filename.lower().endswith(useful_extensions):
                  if compound not in useful_file_map:
                      useful_file_map[compound] = {}
                  if test_folder not in useful_file_map[compound]:
                      useful_file_map[compound][test_folder] = []
                  
                  if filename not in useful_file_map[compound][test_folder]:
                    useful_file_map[compound][test_folder].append(filename)
                    
                  continue
            
            
    return useful_file_map

# Execute and add to your final_df
useful_files = list_useful_files(diff_df)
final_df['useful_files'] = final_df['compound'].map(lambda x: useful_files.get(x, {}))

print(final_df[['compound', 'useful_files']])



print(final_df)   


final_df.to_csv('output_v6wwpzh7x.csv', index=False)
    
final_df.to_json('output_v6wwpzh7x.json', indent=4)
    
    

with open("extractor_v6wwpzh7x.txt", "w") as file:
  file.writelines(file_content)
  

