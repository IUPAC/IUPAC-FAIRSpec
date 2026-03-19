import pandas as pd
from pathlib import Path
from collections import Counter
import re
import itertools
import sys
import numpy as np
pd.options.mode.chained_assignment = None

# compound_name.py
# 2026.03.14 BH -- refactoring, commenting
# 2026.03.13 BH -- refactoring, commenting, fixing n/pdata using text.isdigit()
# 2026.03.12 BH -- refactored for clearer defs
# 2026.03.12 BH -- better reporting
# 2026.03.10 BH -- from compound_id_candidate_identifier.py
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

IGNORE_PARENTS = {
    ".", "..", "", ".DS_Store", "pdata", "fid", "ser", "used_from"
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

# NOTE: this is no longer useful -- 1r,fid,i1 are all pruned out already
KNOWN_DATASET_FILES = ['1r', 'fid', '1i', '.mol', 'jdf', '.mnova', 'acqus']

# NOTE: 1h keyword is removed because of frequent use of 1h also as a compound_id number
EXPERIMENT_KEYWORDS = ( "13c",  "cosy", "hsqc", "hmbc", "dept", "jmod", "noesy", "13c jmod")

STRUCTURE_EXTENSIONS = {
   ".mol", ".cdxml", ".cdx"
}

DATA_OBJECT_EXTENSIONS = {
   ".spc", ".jdf", ".jdx", ".mnova"
}

# TODO: why the differences here?
FILE_EXTENSIONS_FOR_ADD_IMAGE = ('.pdf', '.png', '.mol', '.cdx', '.cdxml', '.sdf', '.dx', '.jdx')

# FILE_EXTENSIONS = {
#    ".txt", ".par", ".fid", ".ser", ".json", ".xml", ".temp", ".png", ".info", ".spc"
#}

# NOTE: USEFUL_EXTENSIONS can be altered to find any files of interest
USEFUL_EXTENSIONS = ('.mol', 'cdx', 'cdxml', '.jdf' ,'.mnova', 'pdata', 'procpar', 'acqu', '.pdf', '.png')

# TODO-this should be broken out by resource path (first path)
GLOBAL_THRESHOLD = 0.3  # fractional frequency to qualify as a common, global path;



######### all methods are static methods (they do not reference global values, only their parameters)

# def add_compound_id(df, final_df, compound_id):
# def add_compound_ids_from_experiment_parents(df, final_df):
# def add_parent_folder_column(df, final_df, ignore_parents=None):
# def add_representations(df, paths_list):
# def add_useful_files(compound_root_df, final_df):
# def clean_parent_of_experiment(parts, candidates, global_folders):
# def contains_experiment_keyword(name):
# def generate_df_for_tsv(df, final_df):
# def generate_final_df(df, compound_root_df):
# def generate_NMR_technique_column(final_df):
# def get_data_frame_for_filepaths_after_compound_ids(df):
# def get_final_compound_name(final_df, counts_series):
# def get_global_folders(all_initial_candidates, candidate_freq):
# def has_data_object_extension(name):
# def has_structure_extension(name):
# def has_structure_or_data_object_extension(name):
# def is_basic_candidate(folder):
# def is_candidate(folder, global_folders):
# def is_data_object(token):
# def is_pure_experiment_folder(name):
# def lambda_choose_weak_label(row, order, freq, globals, min_freq=3, max_frac=0.5):
# def lambda_find_full_important_file_path(row, paths_list):
# def lambda_get_parent_count(row, path_col, compound_id_col):
# def lambda_set_is_experiment(row):
# def map_dataset_folders(compound_root_df, final_df):
# def map_dataset_folders_for_compound_id(df, compound_name):
# def parent_of_structure_or_data_object(parts):
# def path_to_pdata(path):
# def print_column(df,name):
# def prune_child_compound_ids(df, multiplier=5, min_children=6):
# def set_path_parts_and_initial_candidates(df): 
# def update_df_for_global_folders(df, global_folders, candidate_freq):

def print_column(df,name):
    '''
    Debugging - list column in column format
    '''
    print(f"TEST {name}\n {df[name]}\n")

def create_data_frame(paths):
    '''
    create a data frame with columns 'parts', 'path_text', 'path_text_final', and 'initial_candidates'
    '''       
    path_parts = [list(map(str, Path(p).parts[1:])) for p in paths]
    df = pd.DataFrame({"parts": path_parts})
    # create file paths with the zip files
    df["path_text_final"] = df["parts"].apply(lambda x: " / ".join(x))
    df["path_text_final"] = df["path_text_final"].str.replace(" / ", "/")
    df["path_text_final"] = df["path_text_final"].str.replace(".zip__/", ".zip|")

    # create clean file paths for analysis
    # NOTE BH: It's not clear to me why we remove the zip paths. Sometimes those are 
    # the only place the compound numbers are present. For example:
    # jo5c00774_si_002/Supplementary compound and compunational data ver2/FID for publication/13C NMR data of compound 7.zip__/Feb15-2025-kshi2-7/11/pdata
    df['parts'] = df['parts'].apply(lambda x: [item for item in x if '.zip__' not in item])
    df["path_text"] = df["parts"].apply(lambda x: " / ".join(x))
    
    #  For each path part, identify initial candidates as a "basic" candidates
    df["initial_candidates"] = df["parts"].apply(
        lambda parts: [part for part in parts if is_basic_candidate(part)]
    )
    return df
    
def is_basic_candidate(folder):
    """
    Not a defined data object name (pdata, procpar, acqu)
    and not something to be ignored
    """
    f = folder.strip()
    if is_data_object(f):
        return False
    if f.lower() in (x for x in IGNORE_FOLDERS):
        return False
    if any(sub in f.lower() for sub in IGNORE_SUBSTRINGS):
        return False
    return True

def is_data_object(token):
    """
    return true if a token is one of DATA_OBJECT_NAMES (pdata, acqu, procpar)
    
    (no longer checking for actual files, as they have been stripped out)
    """
    return (
        token in DATA_OBJECT_NAMES
      #  or any(token.endswith(ext) for ext in FILE_EXTENSIONS)
    )

def has_structure_extension(name):
    '''
      Is one of the typicaal extensions, such as ".mol", ".cdxml", or ".cdx"
    '''
    return any(name.endswith(ext) for ext in STRUCTURE_EXTENSIONS)

def has_data_object_extension(name):
    '''
      Is one of the typical extensions, such as ".mnova", ".jdx", ".jdf"
      This is not guarranteed, particularly for jdx. 
    '''
    return any(name.endswith(ext) for ext in DATA_OBJECT_EXTENSIONS)

def has_structure_or_data_object_extension(name):
    return has_structure_extension(name) or has_data_object_extension(name)

def contains_experiment_keyword(name):
    """
    Test for one of the known NMR experiment types. 
    This is rather ad hoc and problematic.
    """
    n = name.lower()
    return any(k in n for k in EXPERIMENT_KEYWORDS)

def is_pure_experiment_folder(name):
    """
    Returns true if the folder name represents ONLY an experiment, 
    not a compound_id-experiment hybrid.
    For example,"3ag 13C" will give "3ag" and return false, 
    but "(13C)" wil give "()" and return true
    """
    n = name.lower().strip()
    if not contains_experiment_keyword(n):
        return False
    cleaned = n
    for k in EXPERIMENT_KEYWORDS:
        cleaned = cleaned.replace(k, "")
    cleaned = re.sub(r"[^a-z0-9]+", "", cleaned)
    return len(cleaned) == 0   
  
def parent_of_structure_or_data_object(parts):
    '''
    Return folder that contains a .jdf (or similar) file that could contain
    a compound_id number or spec ID (1H, 13C, etc.) 
    '''
    for i in range(len(parts) - 1):
        parent = parts[i]
        child = parts[i + 1]
        if has_structure_or_data_object_extension(child):
          return parent
    return None

def lambda_get_parent_count(row, path_col, compound_id_col):
    '''
        Count the number of parents above this path. 
        That is, the number of slashes before identified compound_id folders.

        Not sure why we do this case-insensitively.

    '''
    path = str(row[path_col]).replace('\\', '/')
    compound_id = str(row[compound_id_col])
    idx = path.lower().find("/ " + compound_id.lower() + " /")
    if idx == -1:
        idx = path.lower().find(compound_id.lower())
    if idx == -1:
        return 0
    else:
        idx += 2
    #print(f"count_slash {idx} {path} {path[:idx]} {compound_id} {compound_id_col} {path_col}")  
    return path[:idx].count('/')

def get_data_frame_for_filepaths_after_compound_ids(df):
    '''
     Add "compound_id_path" [p:s] and "identified_compound_id" p[s] to the data frame.
    '''
    valid_labels = set(df["compound_id_label"].dropna())
    dfzip = zip(df["parts"], df["parent_count"])
    records = [
        {"compound_id_path": p[s:], "identified_compound_id": p[s]}
        for p, s in dfzip
        if s is not None and s < len(p) and p[s] in valid_labels
    ]
    #print(f"records={records}\n")
    return pd.DataFrame(records)

def map_dataset_folders_for_compound_id(df, compound_name):
    '''
    Map the dataset folders to the correct compound_id.

    @return a set of 
    '''
    mask = df["parts"].apply(lambda path: compound_name in path)
    relevant_rows = df[mask]
    idx = 0
    for row in relevant_rows["parts"]:
      idx=0
      for i in row:
        if i != compound_name:
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

def path_to_pdata(path):
    '''
        finding pdata paths

    '''
    if "pdata" in path:
        idx = path.index("pdata")
        if idx > 0: # this goes back TWO paths or ONE path? set to ONE here
            return path[:idx]
    return path

def add_useful_files(compound_root_df, final_df):
    '''
        Deprecated.
        Collect files of interest. Deprecated. Most of these have been pruned away.
    '''
    # under the format: {compound_id: {dataset_folder: [files]}}
    useful_file_map = {}

    for _, row in compound_root_df.iterrows():
        path_parts = row["compound_id_path"]
        compound_id = row["identified_compound_id"]
        if len(path_parts) > 2:
            dataset_folder = path_parts[1]
            
            for file_index in range(len(path_parts)-1):
              filename = path_parts[file_index+1]
              
              if filename.lower().endswith(USEFUL_EXTENSIONS):
                  if compound_id not in useful_file_map:
                      useful_file_map[compound_id] = {}
                 
                  if dataset_folder not in useful_file_map[compound_id]:
                      useful_file_map[compound_id][dataset_folder] = []
                  
                  if filename not in useful_file_map[compound_id][dataset_folder]:
                    useful_file_map[compound_id][dataset_folder].append(filename)
                  continue
        else:
          dataset_folder = "NA"
          for file_index in range(len(path_parts)-1):
            filename = path_parts[file_index+1]
            if filename.lower().endswith(USEFUL_EXTENSIONS):
                  if compound_id not in useful_file_map:
                      useful_file_map[compound_id] = {}
                  if dataset_folder not in useful_file_map[compound_id]:
                      useful_file_map[compound_id][dataset_folder] = []
                  if filename not in useful_file_map[compound_id][dataset_folder]:
                    useful_file_map[compound_id][dataset_folder].append(filename)
                  continue
    # map the useful files to the correct compound_ids
    final_df['useful_files'] = final_df['compound_id'].map(lambda x: useful_file_map.get(x, {}))

def get_global_folders(all_initial_candidates, candidate_freq):
    return {
        folder for folder, count in candidate_freq.items()
        if count > 1 and count / overall_path_count >= GLOBAL_THRESHOLD
    }

def map_dataset_folders(compound_root_df, final_df):
    '''
      Check if there are dataset folders within compound_id folders
      Return a DataFrame with columns "compound_id" and "dataset_folders"
    '''
    dataset_folder_map = {}
    for _, row in compound_root_df.iterrows():
        path_parts = row["compound_id_path"]
        compound_id = row["identified_compound_id"]
      
      # check if there is a folder inside the compound_id folder
        if len(path_parts) > 2:
            folder_name = path_parts[1]          
            if compound_id not in dataset_folder_map:
                dataset_folder_map[compound_id] = set()
                dataset_folder_map[compound_id].add(folder_name)
    final_df['dataset_folders'] = final_df['compound_id'].map(
        lambda x: list(dataset_folder_map.get(x, []))
    )
  
def is_candidate(folder, global_folders):
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
    if is_data_object(folder):
        return False
    return True

def clean_parent_of_experiment(parts, candidates, global_folders):
    '''
        Return the parent folder of an experiment folder such as "HMQC".

        This rather unreliable. There are many different NMR experiments;
        only some are checked here.
    '''
    for parent, child in zip(parts, parts[1:]):
        #print(f"parent={parent} child={child}")
        if  (
            parent in candidates
            and is_candidate(parent, global_folders)
            and not contains_experiment_keyword(parent)
            and (contains_experiment_keyword(child) or is_data_object(child))
        ):
            return parent
    return None

def get_final_compound_name(final_df, counts_series):
    '''
    Get just the final compound_id ids.
    Compare the tokens with the identified folders.
    If a compound_id folder name contains multiple tokens, 
    select the one where it is less frequent
    OR filter out the most common token in the folder name 
    '''
    tokens_in_compound_id_folder_name = []
    for compound_id_folder in final_df['compound_id']:
        max_count = 0
        for value, count in counts_series.items():
            #print(f"val={value} count={count}")
            if value in compound_id_folder:
                if count > max_count:
                    max_token = value
                    max_count = count
        tokens_in_compound_id_folder_name.append(max_token)

    # identify unique tokens
    tokens_in_compound_id_folder_name_set = set(tokens_in_compound_id_folder_name)

    # finds if a token is occuring a lot, and if it is, it's likely not part of the compound_id number
    for token_found in tokens_in_compound_id_folder_name_set:
        final_df['compound_name'] = final_df['compound_id']
        word_freq = tokens_in_compound_id_folder_name.count(token_found)
        if word_freq > len(tokens_in_compound_id_folder_name)/1.5:
            final_df['compound_name'] = final_df['compound_id'].str.replace(token_found, "")
            final_df['compound_name'] = final_df['compound_name'].str.replace("of ", "")

def lambda_choose_weak_label(row, order, freq, globals, min_freq=3, max_frac=0.5):
    '''
        Identify compound_id candidate folders (instructions with structure or data object files and parent files of experiments)
    '''
    parts = row["parts"]
    candidates = row["compound_id_candidates"]
    parent = clean_parent_of_experiment(parts, candidates, globals)
    print(f"CWL\n pars={parts}\n cand={candidates}\n parent={parent}\n freq={freq}")
    if not parent:
        parent = parent_of_structure_or_data_object(parts)
    if parent != None and not parent in globals: 
        return parent
      
    # frequency-based heuristic
    ranked = sorted(candidates, key=lambda x: (row["parts"].index(x), -freq[x]))
    for c in ranked:
        if has_structure_or_data_object_extension(c):
            return c
        if not is_candidate(c, globals):
            continue
        if is_data_object(c):
            continue
        if is_pure_experiment_folder(c):
            continue
        if (freq[c] >= min_freq and freq[c] / overall_path_count <= max_frac):
            return c
    return None

def lambda_set_is_experiment(row):
    '''
    Operates on final_df to give final_df['is_experiment'] column.

    If an identified compound_id has dataset folders = pdata for now 
    '''
    return isinstance(row['dataset_folders'], list) and 'pdata' in row['dataset_folders']


def add_parent_folder_column(df, final_df, ignore_parents=None):
    '''
    Adds a parent folder if the experiment folder is identified as a compound_id

    '''
    if ignore_parents is None:
        ignore_parents = IGNORE_PARENTS
    parent_map = {}
    for _, row in df.iterrows():
        compound_id = row["compound_id_label"]
        parts = row["parts"]
        slashes = row["parent_count"]

        if compound_id is None or slashes is None:
            continue
        parent_idx = slashes - 1
        if parent_idx < 0 or parent_idx >= len(parts):
            continue
        parent = parts[parent_idx]
        if parent in ignore_parents or parent == compound_id:
            continue
        parent_map.setdefault(compound_id, []).append(parent)

    def local_map_choose_parent(compound_id):
        '''
        Choose most common parent for a given compound_id

        '''
        parents = parent_map.get(compound_id, [])
        if not parents:
            return None
        return Counter(parents).most_common(1)[0][0]

    final_df["parent_folder"] = final_df["compound_id"].map(local_map_choose_parent)

def add_compound_id(df, final_df, compound_id):
    '''
        Add a compound_id to final_df from scratch. 
        If another function identifies a compound_id that was not originally identified, 
        build this compound_id info

        @return a new final_df
    '''
    compound_id_rows = df[df["parts"].apply(lambda x: compound_id in x)]

    all_paths = []
    if not compound_id_rows.empty:
        all_paths = compound_id_rows["parts"].tolist()
        
    new_data = []
    for sublist in path_parts:
      if compound_id in sublist:
          idx = sublist.index(compound_id)
          if sublist[idx] == compound_id:
            new_sublist = [sublist[idx]] + sublist[idx+1:]
            new_data.append(new_sublist)
          else:
              new_data.append(sublist)
    dataset_folders = map_dataset_folders_for_compound_id(df, compound_id)
    pdata_paths = []
    filtered_df_regex = df[df['parts'].apply(lambda x: x[-1] == "pdata")]
    for row in filtered_df_regex['parts']:
      idx = 0
      for i in row:
        idx += 1
        if i == compound_id:
          new_list = row[idx-1:]
          new_path = '/'.join(new_list)
          pdata_paths.append(new_path)
          #print(pdata_paths)
        continue
    compound_id_df = pd.DataFrame({"compound_id_path": new_data, "identified_compound_id": compound_id})
    useful_files = list_useful_files(compound_id_df)
    useful_files = useful_files[compound_id]
    new_row = {
        "compound_name": compound_id,
        "compound_id": compound_id,
        "dataset_folders": sorted(set(dataset_folders)),
        "pdata_path": sorted(set(pdata_paths)),
        "useful_files": useful_files
    }
    return pd.concat([final_df, pd.DataFrame([new_row])],ignore_index=True)
    
def lambda_find_full_important_file_path(row, paths_list):
    '''
    find the paths that end with an important file name that also have a compound id and a dataset folder
    '''
    for path in paths_list:
        if (path.endswith(str(row['important_file'])) and 
            str(row['compound_id']) in path and 
            (str(row['dataset_folder']) in path or str(row['dataset_folder']) == "NA")
        ):
            return path
    return None

def prune_child_compound_ids(df, multiplier=5, min_children=6):
    '''
     last check: if identified compound_id contains way more folders than other identified compound_ids

    @return a copy of df
    '''
    df = df.copy()
    row_counts = df.groupby("compound_id").size()
    if len(row_counts) == 0:
        return df

    median_rows = row_counts.median()

    # count number of distinct dataset folders per compound_id
    child_counts = df.groupby("compound_id")["dataset_folder"].nunique()

    # identify parent-like folders
    suspicious = row_counts[
        (row_counts > median_rows * multiplier) &
        (child_counts >= min_children)
    ].index.tolist()

    if not suspicious:
        return df

    print("Some identified compound_ids are likely parent folders.")
    print("Promoting children of parent-like compound_ids:", suspicious)
    print("\n")
    
    new_rows = []
    for parent in suspicious:
        parent_rows = df[df["compound_id"] == parent]

        # each dataset_folder becomes a new compound_id
        for child, group in parent_rows.groupby("dataset_folder"):
            if child == "NA" or pd.isna(child):
                continue
            child_group = group.copy()
            child_group["compound_id"] = child
            child_group["compound_name"] = child
            child_group["spectra_ID"] = child_group["compound_name"] + " " + child_group["dataset_folder"]
            new_rows.append(child_group)

    # remove original parent compound_id rows
    df = df[~df["compound_id"].isin(suspicious)]

    # add corrected child-compound_id rows
    if new_rows:
        df = pd.concat([df] + new_rows, ignore_index=True)
    print(f"Final identified compound_ids:\n {df['compound_id'].unique()} \n\n Final number of identified compound_ids: {df['compound_id'].nunique()}\n")
    return df


def add_representations(df, paths_list):
    '''
        df will be df_for_TSV
        Add a folder of png or images to compound_ids (ex: a PDF file in "HRMS For Publication")

        @return possibly new df
    '''
    new_rows = []
    folder_map = {}

    # group files by their parent folder
    for p in paths_list:
        path_obj = Path(p)
        parent = str(path_obj.parent)
        if parent not in folder_map:
            folder_map[parent] = []
        folder_map[parent].append(path_obj.name.strip())

    # check how many files in this specific folder match a known compound_id
    compound_ids = df["compound_id"].dropna().unique().tolist()
    for folder, files in folder_map.items():
        match_count = 0
        folder_matches = []
        
        for f_path in files:
            # check if any compound_id name exists in the filename
            matched_comp = next((c for c in compound_ids if c in f_path), None)
            if matched_comp and f_path.suffix.lower() in FILE_EXTENSIONS_FOR_ADD_IMAGE:
                match_count += 1
                folder_matches.append((f_path, matched_comp))
                
        # if more than 50% of files in this folder are relevant

        if len(files) > 0 and (match_count / len(files)) > 0.5:

            df = df[df["compound_id"] != folder[2:len(folder)-1]].reset_index(drop=True)
            
            # NOTE: There is a current error in if the pdf name includes a compound_id in there, 
            # even if the compound_id is not the intended compound_id
            # HOWEVER: the row turns out funky and would be easy to not include
            for f_path, comp in folder_matches:
                x = df[df["compound_id"] == comp]
                template = x.iloc[0].to_dict()
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
        df = pd.concat([df, pd.DataFrame(new_rows)], ignore_index=True)
    return df

def generate_df_for_tsv(df, final_df):
    '''
    Create the data frame for ${doi}_final_output.tsv

    @return new final_df, spec_ids, and len(spec_ids)
    '''
    # creating the spectral ID column

    # expanding the final_df to df_for_TSV
    # NOTE: the difference in df_for_TSV is that each useful_file has it's own line and file path while final_df is one row per compound_id
    local_df = (
        final_df.assign(useful_files=final_df['useful_files'].map(lambda d: list(d.items())))
        .explode('useful_files')
        .assign(Key=lambda x: x['useful_files'].str.get(0),
                Value=lambda x: x['useful_files'].str.get(1))
        .drop(columns='useful_files')
        .reset_index(drop=True)
    )

    local_df = local_df.rename(columns={'Key': 'dataset_folder', 'Value': 'important_file'})
    local_df = local_df[["compound_name", 
        # "tech", 
        "compound_id", "dataset_folder", "important_file"]]
    local_df["spectra_ID"] = local_df["compound_name"] + ' ' + local_df["dataset_folder"]

    spec_ids = local_df["spectra_ID"].unique().tolist()

    local_df = local_df.explode('important_file')  

    # create the two different path_text
    df['path_text'] = df['path_text'].apply(lambda x: x[13:])
    paths_list = df['path_text'].tolist()
    paths_list_final = df["path_text_final"].tolist()

    # creates column for matched_path which doesn't include the zip files (filtered out later)
    local_df['matched_path'] = local_df.apply(
        lambda row: lambda_find_full_important_file_path(row, paths_list), axis='columns'
    )

    # fixed spectra_ID for compound_ids with JDF files (now trying to do all extension files)
    mask = (local_df["important_file"].str.contains(".")) & (local_df["spectra_ID"].str.contains("NA"))
    local_df.loc[mask, "spectra_ID"] = local_df[mask].apply(
        lambda x: x["compound_id"] + ' ' + x["important_file"].replace(x["compound_id"], "").replace("_", ""), 
        axis='columns'
    )

    local_df = add_representations(local_df, paths_list)

    # add the full file paths with zip files to local_df
    local_df['full_matched_path'] = local_df.apply(
        lambda row: lambda_find_full_important_file_path(row, paths_list_final), axis='columns'
    )

    # drop matched_path (not needed for intended output)
    local_df = local_df.drop('matched_path', axis='columns')

    # remove identified compound_id that contains way more folders than other identified compound_ids    
    local_df = prune_child_compound_ids(local_df, multiplier=4, min_children=6)
    return (local_df, spec_ids, len(spec_ids))

def generate_final_df(df, compound_root_df):
    # creating the final_df, includes all of the information with each compound_id label once
    df = pd.DataFrame(df["compound_id_label"].unique(), columns=['compound_id'])

    # creates dataset_folders column which includes the dataset folders for each identified compound_id
    map_dataset_folders(compound_root_df, df)

    # extract the useful files from the compound_root_df (includes all file paths after the compound_ids)
    add_useful_files(compound_root_df, df)

    # tidy up the ' + ' business
    df['compound_id'] = df['compound_id'].str.replace(' + ', '+', regex=False)         

    # output the file paths
    unique_entries_set = set(itertools.chain.from_iterable(df['dataset_folders']))
    file_path_keywords = KNOWN_DATASET_FILES + list(unique_entries_set)

    filtered_df_regex = compound_root_df[compound_root_df['compound_id_path'].apply(lambda x: any(k in x for k in file_path_keywords))]

    filtered_df_regex = compound_root_df.loc[
        compound_root_df["compound_id_path"].str[-1].eq("pdata")
    ].copy()

    filtered_df_regex["my_string"] = filtered_df_regex["compound_id_path"].str.join("/")

    #print(f"compound_root_df={compound_root_df}")

    compound_root_df["compound_id_path_to_pdata"] = compound_root_df["compound_id_path"].apply(path_to_pdata)

    #print_column(compound_root_df,'compound_id_path') 

    #print_column(compound_root_df,'compound_id_path_to_pdata') # nan nan nan

    long_string = "\n".join(compound_root_df['compound_id_path_to_pdata'].apply(lambda x: " ".join(x)))
    long_string = long_string.replace(" + ", "+")

    # cleaning the compound_id paths + counting tokens to get "compound_name"
    tokens = re.split(r'[ \n\.\/]+', long_string)
    tokens = [t for t in tokens if t]
    counts_series = pd.Series(tokens).value_counts()

    #print(f"tokens={tokens}")
    #print(f"counts_series={counts_series}")
    for val in df['compound_id']:
        val = val.replace(" + ", "+")
    df['compound_id'] = df['compound_id'].str.replace(' + ', '+', regex=False)               

    get_final_compound_name(df, counts_series)

    # writing the pdata file path
    filtered_df_regex = compound_root_df[compound_root_df['compound_id_path'].apply(lambda x: x[-1] == "pdata")].copy()
    filtered_df_regex['my_string'] = filtered_df_regex['compound_id_path'].str.join('/')
    temp_df = filtered_df_regex.copy()
    temp_df['compound_name'] = temp_df['my_string'].str.split('/').str[0]
    path_mapping = temp_df.groupby('compound_name')['my_string'].apply(list).reset_index()
    path_mapping.columns = ['compound_id', 'pdata_path']
    df = df.merge(path_mapping, on='compound_id', how='left')
    df['pdata_path'] = df['pdata_path'].apply(lambda x: x if isinstance(x, list) else [])

    # organizing rows of df (could probably delete this)
    rows_order = ["compound_name", "compound_id", "dataset_folders", "pdata_path", "useful_files"]
    return df[rows_order]

def add_compound_ids_from_experiment_parents(df, final_df):
    '''    
    drop columns with identified compound_ids that match columns identified as dataset folders
    @return new final_df
    '''
    all_compound_ids = set(final_df['compound_id'].dropna().unique())

    final_df['is_match'] = final_df.apply(
        lambda row: any(folder in all_compound_ids for folder in row['dataset_folders']) 
        if isinstance(row['dataset_folders'], list) else False, 
        axis='columns'
    )

    final_df = final_df[~final_df['is_match']]
    final_df = final_df.drop(columns=['is_match'])

    final_df['is_experiment'] = final_df.apply(lambda_set_is_experiment, axis='columns')

    # call the second check functions

    add_parent_folder_column(df, final_df)
    compound_ids_to_be_added = set(final_df[final_df['is_experiment'] == True]['parent_folder'])
    final_df = final_df[~final_df['is_experiment']]
    final_df = final_df.drop(columns=['is_experiment'])
    final_df = final_df.drop(columns=['parent_folder'])

    for compound_id in compound_ids_to_be_added:
        final_df = add_compound_id(df, final_df, compound_id)
    return final_df

def update_df_for_global_folders(df, global_folders, candidate_freq):
    '''
    '''
    # re-extract candidates after removing globals
    df["compound_id_candidates"] = df["parts"].apply(
        lambda parts: [p for p in parts if is_candidate(p, global_folders)]
    )
    #print_column(df,"compound_id_candidates")

    # identify each file path with a compound_id
    df["compound_id_label"] = df.apply(lambda_choose_weak_label, freq=candidate_freq, globals=global_folders, axis='columns', order=1)
    print_column(df,"compound_id_label")

    # filter the original DataFrame for rows where the count is greater than or equal to the average 
    class_name_average = df["compound_id_label"].value_counts().mean()
    occurrence_per_row = df.groupby('compound_id_label')['compound_id_label'].transform('count')
    df = df[occurrence_per_row >= class_name_average - (class_name_average/.75)]

    #print_column(df, "path_text")
    #print_column(df, "compound_id_label")

    # add to df a count of parents before compound_id_label
    df['parent_count'] = df.apply(
        lambda row: lambda_get_parent_count(row, 'path_text','compound_id_label'), axis="columns"
    )
    return df

def generate_NMR_technique_column(final_df):
    '''
    Create the "tech" column in final_df for pdata records 

    not called -- Deprecated -- that's just Bruker, not others; no longer necessary 
    '''
    # NOTE: This column is not finalized/working 100%
    final_df["tech"] = "NA"
    mask = final_df["pdata_path"].apply(lambda row: len(row) > 0 and ("pdata" in row[0]))
    final_df.loc[mask, "tech"] = "NMR"


############## end of method definitions


# Read file paths and set global variables
filename = f"file_list_{dataset_DOI}.txt"
with open(filename, "r", encoding="utf-8") as f:
    paths = [line.strip() for line in f if line.strip()]

df = create_data_frame(paths)

overall_path_count = len(df)

# set the initial set of possible candidates
all_initial_candidates = df['initial_candidates'].explode().tolist()
print(f"\n\nAll initial compound_id candidates: \n {set(all_initial_candidates)}\n")

###### frequency analysis for global folders

candidate_freq = Counter(all_initial_candidates)
global_folders = get_global_folders(all_initial_candidates, candidate_freq)
print(f"Identified global folders: \n {global_folders}\n")

df = update_df_for_global_folders(df, global_folders, candidate_freq)

# create compound_root_df, includes all of the filepaths after compound_ids
# this assumes that all the important information is after the compound_id 
compound_root_df = get_data_frame_for_filepaths_after_compound_ids(df)
#print(f"compound_root_df={compound_root_df}")
#            compound_id_path   identified_compound_id
# 0         [3aa-1H, procpar]              3aa-1H
# 1   [3aaa-19F.fid, procpar]        3aaa-19F.fid
# 2        [3aaa-1H, procpar]             3aaa-1H
# 3     [3ab-1H.fid, procpar]          3ab-1H.fid
# 4         [3ab-77Se, pdata]            3ab-77Se
# ..                      ...                 ...
# 66          [4-77Se, pdata]              4-77Se
# 67            [5-1H, pdata]                5-1H
# 68          [5-77Se, pdata]              5-77Se
# 69            [6-1H, pdata]                6-1H
# 70          [6-77Se, pdata]              6-77Se

final_df = generate_final_df(df, compound_root_df)

# final check for compound IDs from experiment parent. Necessary?
final_df = add_compound_ids_from_experiment_parents(df, final_df)

# get the final compound IDs and write them out
final_compound_ids = final_df['compound_id'].unique().tolist()
final_compound_id_count = len(final_compound_ids)
# write <doi>_compound_id_list.txt and <doi>_compound_id_count
# compound_id_count is a file with length the number of compound_ids
with open(f'{dataset_DOI}_compound_id_list.txt', "w", encoding="utf-8") as f:
    f.write(f'{dataset_DOI} '+';'.join(list(map(str, final_compound_ids)))+'\n')
with open(f'{dataset_DOI}_compound_id_count', "w", encoding="utf-8") as f:
    f.write("0" * final_compound_id_count)


# write the final csv data frame now, before generating df_final for TSV
final_df.to_csv(f'{dataset_DOI}_output.csv', index=False)

# NOTE deprecated: generate_NMR_technique_column(final_df)

# create the df for the TSV file along with the list of unique spec_ids
(final_df, final_spec_ids, final_spec_id_count) = generate_df_for_tsv(df, final_df)
final_df.to_csv(f'{dataset_DOI}_final_output.tsv', index=False, sep='\t')
# write <doi>_spec_id_list.txt and <doi>_spec_id_count
with open(f'{dataset_DOI}_data_object_id_list.txt', "w", encoding="utf-8") as f:
    f.write(f'{dataset_DOI} '+';'.join(list(map(str, final_spec_ids)))+'\n')
with open(f'{dataset_DOI}_data_object_id_count', "w", encoding="utf-8") as f:
    f.write("0" * final_spec_id_count)

print(f"final compound ID count: {final_compound_id_count}  final data object ID count: {final_spec_id_count}")

