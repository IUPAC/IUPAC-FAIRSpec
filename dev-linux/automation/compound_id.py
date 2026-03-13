import pandas as pd
from pathlib import Path
from collections import Counter
import re
import itertools
import sys
import numpy as np

# compound_id.py
# 2026.03.12 BH -- refactored for clearer defs
# 2026.03.12 BH -- better reporting
# 2026.03.10 BH -- from compound_candidate_identifier.py
# 2026.03.08 BH -- changed "specpar" to "procpar" 
# 2026.03.05 BH -- changed "zip.." to "zip__" since Windows cannot handle file/directory names ending with '.'

# command line arguments
dataset_DOI = sys.argv[1]

### Domain knowledge

IGNORE_FOLDERS = {
    # lower-case here
    "used_from", "startingmaterial"
}

IGNORE_SUBSTRINGS = {
    # lower-case here
    "zip"
}

# The Bash script running this method will strip out
#  - all files other than pdata in the directory containing pdata
#  - all files other than procpar in a directory containing procpar
#  - all files in the pdata directory
# Note that acqu is here because it is possible to have a Bruker directory with no pdata
# In that case, the first file in the direcory (acqu) is kept

DATA_OBJECT_NAMES = {
    "pdata", "procpar", "acqu"
}

# NOTE: 1h keyword is removed because of frequent use of 1h also as a compound number
EXPERIMENT_KEYWORDS = [
    "13c",  "cosy", "hsqc", "hmbc", "dept", "jmod", "noesy", "13c jmod"
]

STRUCTURE_EXTENSIONS = {
   ".mol", ".cdxml", ".cdx"
}

DATA_OBJECT_EXTENSIONS = {
   ".spc", ".jdf", ".jdx", ".mnova"
}

# FILE_EXTENSIONS = {
#    ".txt", ".par", ".fid", ".ser", ".json", ".xml", ".temp", ".png", ".info", ".spc"
#}

# NOTE: USEFUL_EXTENSIONS can be altered to find any files of interest
USEFUL_EXTENSIONS = ('.mol', 'cdx', 'cdxml', '.jdf' ,'.mnova', 'pdata', 'procpar', 'acqu', '.pdf', '.png')

# TODO-this should be broken out by resource path (first path)
GLOBAL_THRESHOLD = 0.3  # fractional frequency to qualify as a common, global path;



######### static methods

def print_column(df,name):
    '''
    Debugging - list column in column format
    '''
    print(f"TEST {name} {df[name]}\n")

def static_is_data_object(token):
    """
    return true if a token is one of DATA_OBJECT_NAMES (pdata, acqu, procpar)
    
    (no longer checking for actual files, as they have been stripped out)
    """
    return (
        token in DATA_OBJECT_NAMES
      #  or any(token.endswith(ext) for ext in FILE_EXTENSIONS)
    )

def static_has_structure_extension(name):
    '''
      Is one of the typicaal extensions, such as ".mol", ".cdxml", or ".cdx"
    '''
    return any(name.endswith(ext) for ext in STRUCTURE_EXTENSIONS)

def static_has_data_object_extension(name):
    '''
      Is one of the typicaal extensions, such as ".mnova", ".jdx", ".jdf"
      This is not guarranteed, particularly for jdx. 
    '''
    return any(name.endswith(ext) for ext in DATA_OBJECT_EXTENSIONS)

def static_contains_experiment_keyword(name):
    """
    Test for one of the known NMR experiment types. 
    This is rather ad hoc and problematic.
    """
    n = name.lower()
    return any(k in n for k in EXPERIMENT_KEYWORDS)

def static_is_pure_experiment_folder(name):
    """
    Returns true if the folder name represents ONLY an experiment, 
    not a compound-experiment hybrid.
    For example,"3ag 13C" will give "3ag" and return false, 
    but "(13C)" wil give "()" and return true
    """
    n = name.lower().strip()
    if not static_contains_experiment_keyword(n):
        return False
    cleaned = n
    for k in EXPERIMENT_KEYWORDS:
        cleaned = cleaned.replace(k, "")
    cleaned = re.sub(r"[^a-z0-9]+", "", cleaned)
    return len(cleaned) == 0   
  
def static_is_basic_candidate(folder):
    """
    Not a defined data object name (pdata, procpar, acqu)
    and not something to be ignored
    """
    f = folder.strip()
    if static_is_data_object(f):
        return False
    if f.lower() in (x for x in IGNORE_FOLDERS):
        return False
    if any(sub in f.lower() for sub in IGNORE_SUBSTRINGS):
        return False
    return True

def static_parent_of_structure_or_data_object(parts):
    '''
    Return folder that contains a .jdf (or similar) file that could contain
    a compound number or spec ID (1H, 13C, etc.) 
    '''
    for i in range(len(parts) - 1):
        parent = parts[i]
        child = parts[i + 1]
        if static_has_structure_extension(child) or static_has_data_object_extension(child):
          return parent
    return None

def static_get_parent_count(row, path_col, compound_col):
    '''
        Count the number of parents above this path. 
        That is, the number of slashes before identified compound folders.

        Not sure why we do this case-insensitively.

    '''
    path = str(row[path_col]).replace('\\', '/')
    compound = str(row[compound_col])
    idx = path.lower().find("/ " + compound.lower() + " /")
    if idx == -1:
        idx = path.lower().find(compound.lower())
    if idx == -1:
        return None
    else:
        idx += 2
    print(f"count_slash {idx} {path} {path[:idx]} {compound} {compound_col} {path_col}")  
    return path[:idx].count('/')

def static_get_data_frame_for_filepaths_after_compounds(df):
    '''
     Add "compound_path" [p:s] and "identified_compound" p[s] to the data frame.
     
    '''
    valid_labels = set(df["compound_label"].dropna())
    dfzip = zip(df["parts"], df["slashes_before"])
    records = [
        {"compound_path": p[s:], "identified_compound": p[s]}
        for p, s in dfzip
        if s is not None and s < len(p) and p[s] in valid_labels
    ]
    print(f"records={records}\n")
    return pd.DataFrame(records)

def static_map_dataset_folders_for_compound(diff_df, final_df, target_compound):
    '''
    Map the dataset folders to the correct compound.

    @aram diff_df  file path parts AFTER a compound-defining part
    @param final_df
    @param target_compound 
    '''
    mask = diff_df["parts"].apply(lambda paths: target_compound in paths)
    relevant_rows = diff_df[mask]
    idx = 0
    for row in relevant_rows["parts"]:
      idx=0
      for i in row:
        if i != target_compound:
          idx += 1
        else:
          final_idx = idx
          break
      break
    folders = {
        row["parts"][final_idx+1] 
        for _, row in relevant_rows.iterrows() 
        if len(row["parts"]) > 2
    }
    return folders

def static_path_to_pdata(path):
    '''
        finding pdata paths

    '''
    if "pdata" in path:
        idx = path.index("pdata")
        if idx > 0: # this goes back TWO paths or ONE path? set to ONE here
            return path[:idx]
    return path

def static_list_useful_files(diff_df):
    '''
        Deprecated.
        Collect files of interest. Deprecated. Most of these have been pruned away.
    '''
    # under the format: {compound: {dataset_folder: [files]}}
    useful_file_map = {}

    for _, row in diff_df.iterrows():
        path_parts = row["compound_path"]
        compound = row["identified_compound"]
        if len(path_parts) > 2:
            dataset_folder = path_parts[1]
            
            for file_index in range(len(path_parts)-1):
              filename = path_parts[file_index+1]
              
              if filename.lower().endswith(USEFUL_EXTENSIONS):
                  if compound not in useful_file_map:
                      useful_file_map[compound] = {}
                 
                  if dataset_folder not in useful_file_map[compound]:
                      useful_file_map[compound][dataset_folder] = []
                  
                  if filename not in useful_file_map[compound][dataset_folder]:
                    useful_file_map[compound][dataset_folder].append(filename)
                  continue
        else:
          dataset_folder = "NA"
          for file_index in range(len(path_parts)-1):
            filename = path_parts[file_index+1]
            if filename.lower().endswith(USEFUL_EXTENSIONS):
                  if compound not in useful_file_map:
                      useful_file_map[compound] = {}
                  if dataset_folder not in useful_file_map[compound]:
                      useful_file_map[compound][dataset_folder] = []
                  if filename not in useful_file_map[compound][dataset_folder]:
                    useful_file_map[compound][dataset_folder].append(filename)
                  continue
    return useful_file_map

def static_get_global_folders(all_initial_candidates):
    print(f"TEST candidate_freq {candidate_freq}\n")
    return {
        folder for folder, count in candidate_freq.items()
        if count / overall_path_count >= GLOBAL_THRESHOLD
    }

def static_map_dataset_folders(diff_df, final_df):
    '''
      Check if there are dataset folders within compound folders
      Return a DataFrame with columns "compound" and "dataset_folders"
    '''
    dataset_folder_map = {}
    for _, row in diff_df.iterrows():
        path_parts = row["compound_path"]
        compound = row["identified_compound"]
      
      # check if there is a folder inside the compound folder
        if len(path_parts) > 2:
            folder_name = path_parts[1]          
            if compound not in dataset_folder_map:
                dataset_folder_map[compound] = set()
                dataset_folder_map[compound].add(folder_name)
    final_df['dataset_folders'] = final_df['compound'].map(
        lambda x: list(dataset_folder_map.get(x, []))
    )
  
def static_is_candidate(folder, global_folders):
    '''
      Check:
       (a) that it is not a common global folder
       (b) that it is not one of the folders to ignore, and
       (c) that a candidate is NOT a defined data object (pdata, acqu, procar)

    '''
    if folder in global_folders:
        return False
    if folder.lower() in (x for x in IGNORE_FOLDERS):
        return False
    if static_is_data_object(folder):
        return False
    return True

def static_clean_parent_of_experiment(parts, candidates, global_folders):
    '''
        Return the parent folder of an experiment folder such as "HMQC".

        This rather unreliable. There are many different NMR experiments;
        only some are checked here.
    '''
    for parent, child in zip(parts, parts[1:]):
        # print(f"parent={parent} child={child}")
        if (
            parent in candidates
            and static_is_candidate(parent, global_folders)
            and not static_contains_experiment_keyword(parent)
            and (static_contains_experiment_keyword(child) or static_is_data_object(child))
        ):
            return parent
    return None

def static_get_final_compound_name(final_df, counts_series):
    '''
    Get just compound names.
    Compare the tokens with the identified folders.
    If a compound folder name contains multiple tokens, 
    select the one where it is less frequent
    OR filter out the most common token in the folder name 
    '''
    tokens_in_compound_folder_name = []
    for compound_folder in final_df['compound']:
        max_count = 0
        for value, count in counts_series.items():
            print(f"val={value} count={count}")
            if value in compound_folder:
                if count > max_count:
                    max_token = value
                    max_count = count
        tokens_in_compound_folder_name.append(max_token)

    # identify unique tokens
    tokens_in_compound_folder_name_set = set(tokens_in_compound_folder_name)

    # finds if a token is occuring a lot, and if it is, it's likely not part of the compound number
    for token_found in tokens_in_compound_folder_name_set:
        final_df['compound_name'] = final_df['compound']
        word_freq = tokens_in_compound_folder_name.count(token_found)
        if word_freq > len(tokens_in_compound_folder_name)/1.5:
            final_df['compound_name'] = final_df['compound'].str.replace(token_found, "")
            final_df['compound_name'] = final_df['compound_name'].str.replace("of ", "")

def choose_weak_label(row, order, min_freq=3, max_frac=0.5):
    '''
        Identify compound candidate folders (instructions with structure or data object files and parent files of experiments)
        This method is not static, because it depends upon global_folders and candidate_freq 
    '''
    parts = row["parts"]
    candidates = row["compound_candidates"]
    parent = static_clean_parent_of_experiment(parts, candidates, global_folders)
    if parent and not static_is_data_object(parent):
        return parent
    object_parent = static_parent_of_structure_or_data_object(parts)
    if object_parent != None:
      return object_parent
      
    # frequency-based heuristic
    ranked = sorted(candidates, key=lambda x: (row["parts"].index(x), -candidate_freq[x]))
    for c in ranked:
        if static_is_data_object(c):
            continue
        if static_is_pure_experiment_folder(c):
            continue
        if (candidate_freq[c] >= min_freq and candidate_freq[c] / overall_path_count <= max_frac):
            return c
    return None

############## end of method definitions


# Read file paths
filename = f"file_list_{dataset_DOI}.txt"

with open(filename, "r", encoding="utf-8") as f:
    paths = [line.strip() for line in f if line.strip()]

path_parts = [list(map(str, Path(p).parts[1:])) for p in paths]
df = pd.DataFrame({"parts": path_parts})

# create file paths with the zip files
df["path_text_final"] = df["parts"].apply(lambda x: " / ".join(x))
df["path_text_final"] = df["path_text_final"].str.replace(" / ", "/")
df["path_text_final"] = df["path_text_final"].str.replace(".zip__/", ".zip|")
df["path_text_final"] = df["path_text_final"].str.replace("../test2/", "")

# create clean file paths for analysis
df['parts'] = df['parts'].apply(lambda x: [item for item in x if '.zip__' not in item])
df["path_text"] = df["parts"].apply(lambda x: " / ".join(x))

overall_path_count = len(df)

#  For each path part, identify initial candidates as a "basic" candidates
df["initial_candidates"] = df["parts"].apply(
    lambda parts: [part for part in parts if static_is_basic_candidate(part)]
)

all_initial_candidates = df['initial_candidates'].explode().tolist()

# print initial candidates
print(f"\n\nAll initial compound candidates: \n {set(all_initial_candidates)}\n")

###### frequency analysis for global folders

candidate_freq = Counter(all_initial_candidates)

global_folders = static_get_global_folders(all_initial_candidates)

# print global folders
print(f"Identified global folders: \n {global_folders}\n")

# re-extract candidates after removing globals
df["compound_candidates"] = df["parts"].apply(
    lambda parts: [p for p in parts if static_is_candidate(p, global_folders)]
)

#print_column(df,"compound_candidates")

# identify each file path with a compound
df["compound_label"] = df.apply(choose_weak_label, axis=1, order=1)

#print_column(df,"compound_label")

# filter the original DataFrame for rows where the count is greater than or equal to the average 
class_name_average = df["compound_label"].value_counts().mean()
occurrence_per_row = df.groupby('compound_label')['compound_label'].transform('count')
df = df[occurrence_per_row >= class_name_average - (class_name_average/.75)]

#print_column(df, "path_text")
#print_column(df, "compound_label")

# add slashes_before column to df
df['slashes_before'] = df.apply(
    lambda row: static_get_parent_count(row, 'path_text','compound_label'),
    axis=1
)

# create diff_df, includes all of the filepaths after compounds
# this assumes that all the important information is after the compound ID
diff_df = static_get_data_frame_for_filepaths_after_compounds(df)

# creating the final_df, includes all of the information with each compound label once
final_df = pd.DataFrame(df["compound_label"].unique(), columns=['compound'])

# creates dataset_folders column which includes the dataset folders for each identified compound
static_map_dataset_folders(diff_df, final_df)

print(f"diff_df={diff_df}")

# extract the useful files from the diff_df (includes all file paths after the compounds)
useful_files = static_list_useful_files(diff_df)

# map the useful files to the correct compounds
final_df['useful_files'] = final_df['compound'].map(lambda x: useful_files.get(x, {}))
final_df['compound'] = final_df['compound'].str.replace(' + ', '+', regex=False)         

   
# output the file paths
unique_entries_set = set(itertools.chain.from_iterable(final_df['dataset_folders']))
file_path_keywords = ['1r', 'fid', '1i', '.mol', 'jdf', '.mnova', 'acqus'] + list(unique_entries_set)

filtered_df_regex = diff_df[diff_df['compound_path'].apply(lambda x: any(k in x for k in file_path_keywords))]

filtered_df_regex = diff_df.loc[
    diff_df["compound_path"].str[-1].eq("pdata")
].copy()

filtered_df_regex["my_string"] = filtered_df_regex["compound_path"].str.join("/")

#print(f"diff_df={diff_df}")

diff_df["compound_path_to_pdata"] = diff_df["compound_path"].apply(static_path_to_pdata)

#print_column(diff_df,'compound_path') 

#print_column(diff_df,'compound_path_to_pdata') # nan nan nan

long_string = "\n".join(diff_df['compound_path_to_pdata'].apply(lambda x: " ".join(x)))
long_string = long_string.replace(" + ", "+")

# cleaning the compound paths + counting tokens to get "compound_name"
tokens = re.split(r'[ \n\.\/]+', long_string)
tokens = [t for t in tokens if t]
counts_series = pd.Series(tokens).value_counts()

#print(f"tokens={tokens}")
#print(f"counts_series={counts_series}")
for val in final_df['compound']:
  val = val.replace(" + ", "+")
final_df['compound'] = final_df['compound'].str.replace(' + ', '+', regex=False)               

static_get_final_compound_name(final_df, counts_series)

# writing the pdata file path
filtered_df_regex = diff_df[diff_df['compound_path'].apply(lambda x: x[-1] == "pdata")]
filtered_df_regex['my_string'] = filtered_df_regex['compound_path'].str.join('/')
temp_df = filtered_df_regex.copy()
temp_df['compound_id'] = temp_df['my_string'].str.split('/').str[0]
path_mapping = temp_df.groupby('compound_id')['my_string'].apply(list).reset_index()
path_mapping.columns = ['compound', 'pdata_path']
final_df = final_df.merge(path_mapping, on='compound', how='left')
final_df['pdata_path'] = final_df['pdata_path'].apply(lambda x: x if isinstance(x, list) else [])

# organizing rows of final_df (could probably delete this)
rows_order = ["compound_name", "compound", "dataset_folders", "pdata_path", "useful_files"]
final_df = final_df[rows_order]

# drops identified compounds who has dataset folders that are also identified compounds
all_compounds = set(final_df['compound'].dropna().unique())

final_df['is_match'] = final_df.apply(
    lambda row: any(folder in all_compounds for folder in row['dataset_folders']) 
    if isinstance(row['dataset_folders'], list) else False, 
    axis=1
)

final_df = final_df[~final_df['is_match']]
final_df = final_df.drop(columns=['is_match'])

# if an identified compound has dataset folders = pdata, then change identified compound to the parent folder
def update_to_parent(row):
    if isinstance(row['dataset_folders'], list) and 'pdata' in row['dataset_folders']:
      return True 
    return False

final_df['is_experiment'] = final_df.apply(update_to_parent, axis=1)

# adds a parent folder if the experiment folder is identified as a compound
def add_parent_folder_column(final_df, df, ignore_parents=None):
    if ignore_parents is None:
        ignore_parents = {
            ".", "..", "", ".DS_Store", "pdata", "fid", "ser", "used_from"
        }
    parent_map = {}
    for _, row in df.iterrows():
        compound = row["compound_label"]
        parts = row["parts"]
        slashes = row["slashes_before"]

        if compound is None or slashes is None:
            continue
        parent_idx = slashes - 1
        if parent_idx < 0 or parent_idx >= len(parts):
            continue
        parent = parts[parent_idx]
        if parent in ignore_parents or parent == compound:
            continue
        parent_map.setdefault(compound, []).append(parent)

    # choose most common parent per compound
    def choose_parent(compound):
        parents = parent_map.get(compound, [])
        if not parents:
            return None
        return Counter(parents).most_common(1)[0][0]

    final_df["parent_folder"] = final_df["compound"].map(choose_parent)
    return final_df

# add a compound to final_df from scratch (if another function identifies a compound that was not originally identified, build this compound info)
def add_compound(final_df, diff_df, compound):
    if compound in final_df["compound_name"].values:
        return final_df
    compound_rows = diff_df[diff_df["parts"].apply(lambda x: compound in x)]
  
    all_paths = []
    if not compound_rows.empty:
        all_paths = compound_rows["parts"].tolist()
        
    new_data = []
    for sublist in path_parts:
      if compound in sublist:
          idx = sublist.index(compound)
          if sublist[idx] == compound:
            new_sublist = [sublist[idx]] + sublist[idx+1:]
            new_data.append(new_sublist)
          else:
              new_data.append(sublist)
    dataset_folders = static_map_dataset_folders_for_compound(df, final_df, compound)
    pdata_paths =[]
    filtered_df_regex = diff_df[diff_df['parts'].apply(lambda x: x[-1] == "pdata")]
    for row in filtered_df_regex['parts']:
      idx = 0
      for i in row:
        idx += 1
        if i == compound:
          new_list = row[idx-1:]
          new_path = '/'.join(new_list)
          pdata_paths.append(new_path)
          print(pdata_paths)
        continue
    compound_df = pd.DataFrame({"compound_path": new_data, "identified_compound": compound})
    useful_files = static_list_useful_files(compound_df)
    useful_files = useful_files[compound]

    new_row = {
        "compound_name": compound,
        "compound": compound,
        "dataset_folders": sorted(set(dataset_folders)),
        "pdata_path": sorted(set(pdata_paths)),
        "useful_files": useful_files,
    }

    return pd.concat(
        [final_df, pd.DataFrame([new_row])],
        ignore_index=True
    )

# function to rebuild compounds that were incomplete
def rebuild_compound(final_df, diff_df, compound):
    final_df = final_df[final_df["compound"] != compound]
    return add_compound(final_df, diff_df, compound)  
    
# call the second check functions
final_df = add_parent_folder_column(final_df, df)
compounds_to_be_added = set(final_df[final_df['is_experiment'] == True]['parent_folder'])
final_df = final_df[~final_df['is_experiment']]
final_df = final_df.drop(columns=['is_experiment'])
final_df = final_df.drop(columns=['parent_folder'])

for compound in compounds_to_be_added:
  final_df = add_compound(final_df, df, compound)
  
# final identified compounds

final_ids = final_df['compound'].unique().tolist()
final_id_count = final_df['compound'].nunique()

print(f"\nFinal identified compounds: {final_id_count} {final_ids}\n")

# create the final csv after the second run through
final_df.to_csv(f'{dataset_DOI}_output.csv', index=False)

# creating the "tech" column in final_df 
# NOTE: This column is not finalized/working 100%
final_df["tech"] = "NA"
mask = final_df["pdata_path"].apply(lambda row: len(row) > 0 and ("pdata" in row[0]))
final_df.loc[mask, "tech"] = "NMR"

# creating the spectral ID column
df_to_tsv = final_df

# expanding the final_df to df_exploded
# NOTE: the difference in df_exploded is that each useful_file has it's own line and file path while final_df is one row per compound
df_exploded = (
    final_df.assign(useful_files=final_df['useful_files'].map(lambda d: list(d.items())))
    .explode('useful_files')
    .assign(Key=lambda x: x['useful_files'].str.get(0),
            Value=lambda x: x['useful_files'].str.get(1))
    .drop(columns='useful_files')
    .reset_index(drop=True)
)

df_exploded = df_exploded.rename(columns={'Key': 'dataset_folder', 'Value': 'important_file'})
df_exploded = df_exploded[["compound_name", "tech", "compound", "dataset_folder", "important_file"]]
df_exploded["spectra_ID"] = df_exploded["compound_name"] + ' ' + df_exploded["dataset_folder"]

final_specIds = df_exploded["spectra_ID"].unique().tolist()

df_exploded = df_exploded.explode('important_file')  

# create the two different path_text
df['path_text'] = df['path_text'].apply(lambda x: x[13:])
paths_list = df['path_text'].tolist()

paths_list_final = df["path_text_final"].tolist()

def find_matching_path(row, paths_list):
    for path in paths_list:
        if (path.endswith(str(row['important_file'])) and 
            str(row['compound']) in path and 
            str(row['dataset_folder']) in path):
            return path
        elif (path.endswith(str(row['important_file'])) and
              str(row['compound']) in path and
              str(row['dataset_folder']) == "NA"):
              return path
    return None

# creates column for matched_path which doesn't include the zip files (filtered out later)
df_exploded['matched_path'] = df_exploded.apply(
    lambda row: find_matching_path(row, paths_list), axis=1
)

# fixed spectra_ID for compounds with JDF files (now trying to do all extension files)
mask = (df_exploded["important_file"].str.contains(".")) & (df_exploded["spectra_ID"].str.contains("NA"))
df_exploded.loc[mask, "spectra_ID"] = df_exploded[mask].apply(
    lambda x: x["compound"] + ' ' + x["important_file"].replace(x["compound"], "").replace("_", ""), 
    axis=1
)

all_compounds = df_exploded["compound"].unique().tolist()
new_rows = []

# functionality to add a folder of png or images to compounds (ex: "HRMS For Publication")
# group files by their parent folder
folder_map = {}
for p in paths_list:
    path_obj = Path(p)
    parent = str(path_obj.parent)
    if parent not in folder_map:
        folder_map[parent] = []
    folder_map[parent].append(path_obj)

for folder, files in folder_map.items():
    # check how many files in this specific folder match a known compound
    match_count = 0
    folder_matches = []
    
    for f_path in files:
        # check if any compound name exists in the filename
        matched_comp = next((c for c in all_compounds if c in f_path.name), None)
        if matched_comp and f_path.suffix.lower() in ['.pdf', '.png', '.mol', '.cdx', '.cdxml', '.sdf', '.dx', '.jdx']:
            match_count += 1
            folder_matches.append((f_path, matched_comp))
            
    # if more than 50% of files in this folder are relevant
    if len(files) > 0 and (match_count / len(files)) > 0.5:
        df_exploded = df_exploded[df_exploded["compound"] != folder[2:len(folder)-1]].reset_index(drop=True)
        
        # NOTE: There is a current error in if the pdf name includes a compound in there, even if the compound is not the intended compound
        # HOWEVER: the row turns out funky and would be easy to not include
        for f_path, comp in folder_matches:
            template = df_exploded[df_exploded["compound"] == comp].iloc[0].to_dict()
            template["important_file"] = f_path.name
            template["matched_path"] = str(f_path)
            template["spectra_ID"] = "HRMS" # could change later
            template["dataset_folder"] = "NA"

            # trying to fix the error here
            if template["matched_path"] != "":
              new_rows.append(template)
            else:
              continue

# adds new rows
if new_rows:
    df_exploded = pd.concat([df_exploded, pd.DataFrame(new_rows)], ignore_index=True)
    
# add the full file paths with zip files to df_exploded
df_exploded['full_matched_path'] = df_exploded.apply(
    lambda row: find_matching_path(row, paths_list_final), axis=1
)

# drop matched_path (not needed for intended output)
df_exploded = df_exploded.drop('matched_path', axis=1)

# last check: if identified compound contains way more folders than other identified compounds
def promote_child_compounds(df_exploded, multiplier=5, min_children=6):
    df = df_exploded.copy()
    row_counts = df.groupby("compound").size()
    if len(row_counts) == 0:
        return df

    median_rows = row_counts.median()

    # count number of distinct dataset folders per compound
    child_counts = df.groupby("compound")["dataset_folder"].nunique()

    # identify parent-like folders
    suspicious = row_counts[
        (row_counts > median_rows * multiplier) &
        (child_counts >= min_children)
    ].index.tolist()

    if not suspicious:
        return df

    print("Some identified compounds are likely parent folders.")
    print("Promoting children of parent-like compounds:", suspicious)
    print("\n")
    
    new_rows = []
    for parent in suspicious:
        parent_rows = df[df["compound"] == parent]

        # each dataset_folder becomes a new compound
        for child, group in parent_rows.groupby("dataset_folder"):
            if child == "NA" or pd.isna(child):
                continue
            child_group = group.copy()
            child_group["compound"] = child
            child_group["compound_name"] = child
            child_group["spectra_ID"] = child_group["compound_name"] + " " + child_group["dataset_folder"]
            new_rows.append(child_group)

    # remove original parent compound rows
    df = df[~df["compound"].isin(suspicious)]

    # add corrected child-compound rows
    if new_rows:
        df = pd.concat([df] + new_rows, ignore_index=True)
    print(f"Final identified compounds:\n {df['compound'].unique()} \n\n Final number of identified compounds: {df['compound'].nunique()}\n")
    return df

df_exploded = promote_child_compounds(df_exploded, multiplier=4, min_children=6)

# write file to TSV
df_exploded.to_csv(f'{dataset_DOI}_final_output.tsv', index=False, sep='\t')

# write compound list

print(f"final ID count: {final_id_count}")

# write Spectra_id

with open(f'{dataset_DOI}_compound_list.txt', "w", encoding="utf-8") as f:
    f.write(f'{dataset_DOI} '+';'.join(list(map(str, final_ids)))+'\n')

# compound_count is a file with length the number of compounds
with open(f'{dataset_DOI}_compound_count', "w", encoding="utf-8") as f:
    f.write("0" * final_id_count)

with open(f'{dataset_DOI}_spec_list.txt', "w", encoding="utf-8") as f:
    f.write(f'{dataset_DOI} '+';'.join(list(map(str, final_specIds)))+'\n')

# print output info to terminal
print(f"Final Output: \n \t {dataset_DOI}_final_output.tsv \n\t {dataset_DOI}_output.csv \n\t {dataset_DOI}_file_list.txt\n")

