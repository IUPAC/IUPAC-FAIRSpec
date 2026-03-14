import pandas as pd
from pathlib import Path
from collections import Counter
import re
import itertools
import sys
import numpy as np

# pdata.py

# TODO-this should be broken out by resource path (first path)
GLOBAL_THRESHOLD = 0.3  # fractional frequency to qualify as a common, global path;



def print_column(df,name):
    '''
    Debugging - list column in column format
    '''
    print(f"TEST {name}\n {df[name]}\n")

def set_path_parts_and_initial_candidates(df): 
    '''
    set columns 'path_text_final' and clean the file paths for analysis
    '''       
    # create file paths with the zip files
    df["path_text_final"] = df["parts"].apply(lambda x: " / ".join(x))
    df["path_text_final"] = df["path_text_final"].str.replace(" / ", "/")
    df["path_text_final"] = df["path_text_final"].str.replace(".zip__/", ".zip|")
    df["path_text_final"] = df["path_text_final"].str.replace("../test2/", "")

    # create clean file paths for analysis
    df['parts'] = df['parts'].apply(lambda x: [item for item in x if '.zip__' not in item])
    df["path_text"] = df["parts"].apply(lambda x: " / ".join(x))
    
    #  For each path part, identify initial candidates as a "basic" candidates
    df["initial_candidates"] = df["parts"].apply(
        lambda parts: [part for part in parts if is_basic_candidate(part)]
    )

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
        return None
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
        if (
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

def promote_child_compound_ids(df, multiplier=5, min_children=6):
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
    all_compound_ids = df["compound_id"].dropna().unique().tolist()
    for folder, files in folder_map.items():
        match_count = 0
        folder_matches = []
        
        for f_path in files:
            # check if any compound_id name exists in the filename
            matched_comp = next((c for c in all_compound_ids if c in f_path), None)
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
    local_df = local_df[["compound_name", "tech", "compound_id", "dataset_folder", "important_file"]]
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

    local_df = promote_child_compound_ids(local_df, multiplier=4, min_children=6)
    return (local_df, spec_ids)

def fix_compound_ids(final_df):
    final_df['compound_id'] = final_df['compound_id'].str.replace(' + ', '+', regex=False)         

def generate_final_df(compound_root_df, final_df):
    # output the file paths
    unique_entries_set = set(itertools.chain.from_iterable(final_df['dataset_folders']))
    file_path_keywords = ['1r', 'fid', '1i', '.mol', 'jdf', '.mnova', 'acqus'] + list(unique_entries_set)

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
    for val in final_df['compound_id']:
        val = val.replace(" + ", "+")
    final_df['compound_id'] = final_df['compound_id'].str.replace(' + ', '+', regex=False)               

    get_final_compound_name(final_df, counts_series)

    # writing the pdata file path
    filtered_df_regex = compound_root_df[compound_root_df['compound_id_path'].apply(lambda x: x[-1] == "pdata")]
    filtered_df_regex['my_string'] = filtered_df_regex['compound_id_path'].str.join('/')
    temp_df = filtered_df_regex.copy()
    temp_df['compound_name'] = temp_df['my_string'].str.split('/').str[0]
    path_mapping = temp_df.groupby('compound_name')['my_string'].apply(list).reset_index()
    path_mapping.columns = ['compound_id', 'pdata_path']
    final_df = final_df.merge(path_mapping, on='compound_id', how='left')
    final_df['pdata_path'] = final_df['pdata_path'].apply(lambda x: x if isinstance(x, list) else [])

    # organizing rows of final_df (could probably delete this)
    rows_order = ["compound_name", "compound_id", "dataset_folders", "pdata_path", "useful_files"]
    final_df = final_df[rows_order]
    return final_df

############## end of method definitions


def remove_zip(parts):
    '''
      remove all ".zip__"
    '''

    def fix_a(p):
        if ".zip__" in p.lower():
            return p[:-6]
        return p

    return [fix_a(p) for p in parts]

def find_ids(parts, compound_ids, spec_ids, path):
    last = len(parts) - 1
    if last < 1 or parts[last] != "pdata": 
        compound_ids.append(None)
        spec_ids.append(None)
        return
    compound_id = None
    spec_id = None
    i = 0
    while i < last:
        parent = parts[i]
        child = parts[i + 1]
        if (i + 2 <= last):
            grandchild = parts[i + 2]
            if child != grandchild and child in grandchild:
                i += 1
                continue
        if parent != child and parent in child:

            # jo4c02600_si_002/1H NMR, 13C NMR and 19F NMR Spectra/6c/6c-H/1/pdata
            break
        if i == last - 2:
            if parent != child:
                # jo4c02737_si_002/FID_revised/6m/1H/pdata
                child = parent + "_" + child
            break
        i += 1
    print(f"{parent} \t{child} \t{path}")    
    compound_ids.append(parent)
    spec_ids.append(child)

def checkParts(path_parts, first, last, paths):
    compound_ids = []
    spec_ids = []
    i = first
    while i < last:
        find_ids(path_parts[i], compound_ids, spec_ids, paths[i])
        i += 1
    return (compound_ids, spec_ids)


# Read file paths
filename = "pdata_all.txt"
with open(filename, "r", encoding="utf-8") as f:
    paths = [line.strip() for line in f if line.strip()]
path_parts = [list(map(str, Path(p).parts[1:])) for p in paths]
path_parts2 = [remove_zip(p) for p in path_parts]
first = 0
lp = len(paths)
while first < lp and not ("-------" in paths[first]):
    first += 1
while first < lp:
    #012345678901234567890
    #---------- FILE_LIST_JO4C02089.TXT
    doi = paths[first][21:-4].lower()
    print(f"{doi} {first}")
    last = first + 1
    while last < lp and not ("-------" in paths[last]):
        last += 1
    if last - first > 5:    
        (compound_ids, spec_ids) = checkParts(path_parts2, first + 1, last - 1, paths)
        with open(f'test2/{doi}_compound_id_pdata.txt', "w", encoding="utf-8") as f:
            m = f'{doi} '+';'.join(compound_ids)+'\n'
            f.write(m)
        with open(f'test2/{doi}_data_object_id_pdata.txt', "w", encoding="utf-8") as f:
            m = f'{doi} '+';'.join(spec_ids)+'\n'
            f.write(m)
    first = last


