import pandas as pd
from pathlib import Path
from collections import Counter
import re
import itertools
import sys
import numpy as np

pd.options.mode.chained_assignment = 'raise'

# compound_id_fay.py
# 2026.04.28 FN -- added __strip_repeated_tokens to remove common tokens across compound IDs, keeping only lowest-frequency tokens; added __choose_suitable_token to choose between tokens of equal frequency based on regex pattern
# 2026.04.11 BH -- fixed mnova problem when file is in zip file top directory
# 2026.04.05 BH -- refactored, re-organized; working automation tested on jo4c02622
# 2026.04.02 BH -- rewrote final assignment of paths to avoid search for compound ID in paths, causing error with "1r" found in "S1r" in jo4c02622 
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

IGNORE_FOLDERS_LC = {
    # lower-case here
    "used_from", "startingmaterial"
}

IGNORE_SUBSTRINGS_LC = {
    # lower-case here
    "zip", ".doc"
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
KNOWN_DATASET_FILES = ['acqus', '1r', 'fid', '1i', '.mol', '.jdf', '.mnova']

# NOTE: 1h keyword is removed because of frequent use of 1h also as a compound_id number
EXPERIMENT_KEYWORDS_LC = ("cosy", "hsqc", "hmbc", "dept", "jmod", "noesy")

EXPERIMENT_KEYWORDS_UC = ("1H", "13C", "19F")

COMP_EXPERIMENT_KEYWORDS = (" 1H")


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

# Token separators: characters to treat as delimiters when tokenizing compound IDs
TOKEN_SEPARATORS = ('-', '_', '.', ' ', '\\', '[', ']', ',', ';', ':','(', ')')



######### all methods are static methods (they do not reference global values, only their parameters)
######### "__" are utility methods called by the main functions

# def __add_map_item(map, compound_id, dataset_folder, item):    
# def __add_parent_folder_column(df, final_df, ignore_parents=None):
# def __contains_experiment_keyword(name):
# def __contains_experiment_keyword_lc(name):
# def __contains_experiment_keyword_uc(name):
# def __has_data_object_extension(name):
# def __has_structure_extension(name):
# def __has_structure_or_data_object_extension(name):
# def __is_basic_candidate(folder):
# def __is_candidate(folder, global_folders):
# def __is_data_object(token):
# def __is_pure_experiment_folder(name):
# def __print_column(df,name):
# def __print_df(name, df):

# def add_compound_ids_from_experiment_parents(df, final_df, path_parts_zip):
# def add_useful_files(compound_root_df, final_df):
# def create_data_frame(paths):
# def generate_compound_map_df(df, compound_root_df):
# def generate_df_for_tsv(df, final_df):
# def get_data_frame_for_compound_paths(df):
# def get_final_compound_name(final_df, counts_series):
# def get_global_folders(paths, all_initial_candidates, candidate_freq):
# def map_dataset_folders(compound_root_df, final_df):
# def map_dataset_folders_for_compound_id(df, compound_id):
# def prune_child_compound_ids(df, multiplier=5, min_children=6):
# def update_df_for_global_folders(df, global_folders, candidate_freq):

def __print_column(df, name):
    '''
    Debugging - list column in column format
    '''
    #print(f"df[{name}]\n {df[name]}\n")

def __print_df(name, df):
    '''
    Debugging - print df
    '''
    #print(f"df {name}\n {df.to_string()}\n")


def __is_data_object(token):
    """
    return true if a token is one of DATA_OBJECT_NAMES (pdata, acqu, procpar)
    
    (no longer checking for actual files, as they have been stripped out)
    """
    return (
        token in DATA_OBJECT_NAMES
      #  or any(token.endswith(ext) for ext in FILE_EXTENSIONS)
    )

def __is_ignore_folder(folder):
    if folder.lower() in (x for x in IGNORE_FOLDERS_LC):
        return False
    if any(sub in folder.lower() for sub in IGNORE_SUBSTRINGS_LC):
        return False

def __is_basic_candidate(folder):
    """
    Not a defined data object name (pdata, procpar, acqu)
    and not something to be ignored
    """
    f = folder.strip()
    if __is_data_object(f):
        return False
    if (__is_ignore_folder(f)):
        return False
    return True

def create_data_frame(paths):
    '''
    create a data frame with columns 'parts', 'path_text', 'path_text_final', and 'initial_candidates'
    '''       
    path_parts = [list(map(str, Path(p).parts[1:])) for p in paths]
    df = pd.DataFrame({"parts": path_parts})

    # create file paths with the zip files
    df['path_text_final'] = paths
    df['path_text_final'] = df['path_text_final'].str.replace(".zip__/", ".zip|")

    def lambda_add_pdata_dir(parts):
        if len(parts) >= 2 and parts[-1] == "pdata":
            # Check if the second from last is a digit (as a string)
            if not str(parts[-2]).isdigit():
                parts.insert(-1, "0")
        return parts

    # create clean file paths for analysis
    # NOTE BH: It's not clear to me why we remove the zip paths. Sometimes those are 
    # the only place the compound numbers are present. For example:
    # jo5c00774_si_002/Supplementary compound and compunational data ver2/FID for publication/13C NMR data of compound 7.zip__/Feb15-2025-kshi2-7/11/pdata
    df['parts'] = df['parts'].apply(lambda parts: lambda_add_pdata_dir(parts))
    df['parts'] = df['parts'].apply(lambda x: [item.replace('acqu', 'pdata') for item in x])
    #df['parts'] = df['parts'].apply(lambda x: [item.replace('.zip__', '').replace('.ZIP__', '') for item in x])
    df['path_text'] = df['parts'].apply(lambda x: " / ".join(x))
    
    #  For each path part, identify initial candidates as a "basic" candidates
    def lambda_get_candidate_parts(parts):
        if parts[-1] == "pdata":
            parts = parts[:-2]
        return [part for part in parts if __is_basic_candidate(part)]

    df['initial_candidates'] = df['parts'].apply(
        lambda parts: lambda_get_candidate_parts(parts)
    )
    df['nonduplicated_candidates'] = df['initial_candidates'].apply(
        lambda parts: [p for p in parts if parts.count(p) == 1]
    )

    #__print_column(df, "parts")
    return (df, path_parts)
    
def get_global_folders(paths, all_initial_candidates, candidate_freq):

    # also add all-unique top directories in each zip file

    path_parts0 = [Path(p).parts[0] for p in paths]
    path_parts1 = [Path(p).parts[1] for p in paths]
    df_parts = pd.DataFrame({"p0":path_parts0, "p1": path_parts1})
    df_parts = df_parts.groupby("p0")['p1'].unique()
    # but what if there is just one compound per file?



    gf = {
        folder for folder, count in candidate_freq.items()
        if count > 1 and count / overall_path_count >= GLOBAL_THRESHOLD
    }


    return gf

def update_df_for_global_folders(df, global_folders, candidate_freq):
    '''
    '''
    # re-extract candidates after removing globals
    df['compound_id_candidates'] = df['initial_candidates'].apply(
        lambda parts: [p for p in parts if __is_candidate(p, global_folders)]
    )
    #__print_column(df,"compound_id_candidates")

    #__print_df("df", df)

    # identify each file path with a compound_id

    def private_parent_of_structure_or_data_object(parts):
        '''
        Return folder that contains a .jdf (or similar) file that could contain
        a compound_id number or spec ID (1H, 13C, etc.) 
        '''
        for i in range(len(parts) - 1):
            parent = parts[i]
            child = parts[i + 1]
            if __has_structure_or_data_object_extension(child):
                return parent
        return None

    def private_clean_parent_of_experiment(parts, candidates, global_folders):
        '''
            Return the parent folder of an experiment folder such as "HMQC".

            This rather unreliable. There are many different NMR experiments;
            only some are checked here.
        '''
        for parent, child in zip(parts, parts[1:]):
            #print(f"parent={parent} child={child}")
            if  (
                parent in candidates
                and __is_candidate(parent, global_folders)
                and not __contains_experiment_keyword(parent)
                and (__contains_experiment_keyword(child) or __is_data_object(child))
            ):
                return parent
        return None


    def lambda_choose_weak_label(row, order, freq, globals, min_freq=1, max_frac=0.5):
        '''
            Identify compound_id candidate folders (instructions with structure or data object files and parent files of experiments)
        '''
        parts = row['parts']
        candidates = row['compound_id_candidates']
        parent = private_clean_parent_of_experiment(parts, candidates, globals)
        #print(f"CWL\n parts={parts}\n cand={candidates}\n parent={parent}")#\n freq={freq}")
        if not parent:
            parent = private_parent_of_structure_or_data_object(parts)
        #print(f"CWL\n parent={parent}")#\n freq={freq}")
        if parent != None and not parent in globals: 
            return parent
        
        #print(f"req {freq}")

        # frequency-based heuristic
        ranked = sorted(candidates, key=lambda x: (row['parts'].index(x), -freq[x]))
        for c in ranked:
            if __has_structure_or_data_object_extension(c):
                return c
            if not __is_candidate(c, globals):
                continue
            if __is_data_object(c):
                continue
            if __is_pure_experiment_folder(c):
                continue
            if (freq[c] >= min_freq and freq[c] / overall_path_count <= max_frac):
                return c
        return None


    df['compound_id_label'] = df.apply(lambda_choose_weak_label, freq=candidate_freq, globals=global_folders, axis='columns', order=1)
    #__print_column(df,"compound_id_label")

    # filter the original DataFrame for rows where the count is greater than or equal to the average 
    #class_name_average = df['compound_id_label'].value_counts().mean()
    #group = df.groupby('compound_id_label')['compound_id_label']
    #occurrence_per_row = group.transform('count')
    #    print(df[df['compound_id_label'] == '2a'])

    #                       parts  ... compound_id_label
    # 61   [2a, 2a 13C, 0, pdata]  ...                2a
    # 62    [2a, 2a 1H, 0, pdata]  ...                2a
    # 63           [2a, 2a.mnova]  ...                2a
    # 265  [2a, 2a 13C, 0, pdata]  ...                2a
    # 266   [2a, 2a 1H, 0, pdata]  ...                2a
    # 267          [2a, 2a.mnova]  ...                2a
    # 469  [2a, 2a 13C, 0, pdata]  ...                2a
    # 470   [2a, 2a 1H, 0, pdata]  ...                2a
    # 471          [2a, 2a.mnova]  ...                2a

    # this does nothing == the right hand side is a negative number    
    # df = df[occurrence_per_row >= class_name_average - (class_name_average/.75)]

    #__print_column(df, "path_text")
    #__print_column(df, "compound_id_label")

    # add to df a count of parents before compound_id_label
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

    df['parent_count'] = df.apply(
        lambda row: lambda_get_parent_count(row, 'path_text','compound_id_label'), axis='columns'
    )
    return df

def get_data_frame_for_compound_paths(df):
    '''
     Create a new data frame for "compound_id_column" s, "compound_id_path" p[s:] and "identified_compound_id" p[s].
    '''
    valid_labels = set(df['compound_id_label'].dropna())
    dfzip = zip(df['parts'], df['parent_count'], df['path_text_final'])

    #print(list(df['parts']))

    records = [
        {"compound_id_column": s, "compound_id_path": p[s:], "identified_compound_id": p[s], "path_text_final": t}
        for p, s, t in dfzip
        if s is not None and s < len(p) and p[s] in valid_labels
    ]
    #print(f"records={records}\n")
    return pd.DataFrame(records)

def __has_structure_extension(name):
    '''
      Is one of the typical extensions, such as ".mol", ".cdxml", or ".cdx"
    '''
    return any(name.endswith(ext) for ext in STRUCTURE_EXTENSIONS)

def __has_data_object_extension(name):
    '''
      Is one of the typical extensions, such as ".mnova", ".jdx", ".jdf"
      This is not guarranteed, particularly for jdx. 
    '''
    return any(name.endswith(ext) for ext in DATA_OBJECT_EXTENSIONS)

def __has_structure_or_data_object_extension(name):
    return __has_structure_extension(name) or __has_data_object_extension(name)

def __contains_experiment_keyword_uc(name):
    """
    Test for one of the known NMR experiment types. 
    This is rather ad hoc and problematic.
    """
    return any(k in name for k in EXPERIMENT_KEYWORDS_UC)

def __contains_experiment_keyword_lc(name):
    """
    Test for one of the known NMR experiment types. 
    This is rather ad hoc and problematic.
    """
    n = name.lower()
    return any(k in n for k in EXPERIMENT_KEYWORDS_LC)

def __contains_experiment_keyword(name):
    """
    Test for one of the known NMR experiment types. 
    This is rather ad hoc and problematic.
    """
    n = name.lower()
    if (name == n):
       return __contains_experiment_keyword_lc(name)
    return __contains_experiment_keyword_uc(name)

def __is_pure_experiment_folder(name):
    """
    Returns true if the folder name represents ONLY an experiment, 
    not a compound_id-experiment hybrid.
    For example,"3ag 13C" will give "3ag" and return false, 
    but "(13C)" wil give "()" and return true
    """
    name = name.strip()
    n = name.lower()
    islc = False
    if name == n:
        islc = True
        if not __contains_experiment_keyword_lc(n):
            return False
        cleaned = n
        test = EXPERIMENT_KEYWORDS_LC
    else:
        if not __contains_experiment_keyword_uc(name):
            return False
        cleaned = name
        test = EXPERIMENT_KEYWORDS_LC
    for k in test:
        cleaned = cleaned.replace(k, "")
    cleaned = re.sub(r"[^a-z0-9]+", "", cleaned)
    return len(cleaned) == 0   
  
def map_dataset_folders_for_compound_id(df, compound_id):
    '''
    Map the dataset folders to the correct compound_id.

    @return a set of 
    '''
    mask = df['parts'].apply(lambda path: compound_id in path)
    relevant_rows = df[mask]
    idx = 0
    for row in relevant_rows['parts']:
      idx=0
      for i in row:
        if i != compound_id:
          idx += 1
        else:
          final_idx = idx
          break
      break
    folders = {
        row['parts'][final_idx+1] 
        for _, row in relevant_rows.iterrows() 
        if len(row['parts']) > 2
    }
    return folders

def __add_map_item(map, compound_id, dataset_folder, item):    
    '''
        add an item to map[compound_id][dataset_folder][], creating parents if necessary
    '''
    if compound_id not in map:
        map[compound_id] = {}
    if dataset_folder not in map[compound_id]:
        map[compound_id][dataset_folder] = []
    if item not in map[compound_id][dataset_folder]:
        map[compound_id][dataset_folder].append(item)

def __get_useful_file_maps(compound_root_df):    
    useful_file_map = {}
    path_map = {}
    cmpd_id_col_map = {}


    
    #             compound_id_path identified_compound_id                         path_text_final
    # 0     [S1a, S1a 13C, 0, pdata]                    S1a   jo4c02622_si_001/S1/S1a/S1a 13C/pdata
    # 1      [S1a, S1a 1H, 0, pdata]                    S1a    jo4c02622_si_001/S1/S1a/S1a 1H/pdata
    # 2             [S1a, S1a.mnova]                    S1a       jo4c02622_si_001/S1/S1a/S1a.mnova
    # 3     [S1b, S1b 13C, 0, pdata]                    S1b   jo4c02622_si_001/S1/S1b/S1b 13C/pdata
    # 4      [S1b, S1b 1H, 0, pdata]                    S1b    jo4c02622_si_001/S1/S1b/S1b 1H/pdata

    for _, row in compound_root_df.iterrows():
        cmpd_id_col = row['compound_id_column']
        path_parts = row['compound_id_path']
        compound_id = row['identified_compound_id']
        path_text_final = row['path_text_final']
        match len(path_parts):
            case 1:
                dataset_folder = path_parts[0]
            case _:
                dataset_folder = str(path_parts[1])            
        for file_index in range(len(path_parts)):
            filename = path_parts[file_index]              
            if filename.lower().endswith(USEFUL_EXTENSIONS):
                __add_map_item(useful_file_map, compound_id, dataset_folder, filename)
                __add_map_item(path_map, compound_id, dataset_folder, path_text_final)
                __add_map_item(cmpd_id_col_map, compound_id, dataset_folder, cmpd_id_col)
    return (useful_file_map, path_map, cmpd_id_col_map)

def __choose_suitable_token(token1, token2, pattern=r"\d[a-zA-Z]*"):
    '''
    Compares two tokens and decides which one looks more similar to a compound name using regex.
    Prefers tokens that start with a digit followed by letters (e.g., "3ag", "18b")
    '''
    match1 = re.search(pattern, token1)
    match2 = re.search(pattern, token2)
    
    if match1 and not match2:
        return token1
    elif match2 and not match1:
        return token2
    
    return token1


def __strip_repeated_tokens(compound_ids, min_token_length=2):
    '''
    Removes common tokens across compound IDs by keeping only lowest-frequency tokens.
    Replaces separators (-, _, ., /) with / and tokenizes.
    Returns list in same order as input to maintain 1-to-1 mapping.
    
    param min_token_length: minimum length of token to consider
    '''
    # Quick exit if everything is None
    if not any(id is not None for id in compound_ids):
        return compound_ids
        
    token_counts = Counter()
    modified_compound_ids = []
    
    # Process original list to preserve the exact indexing of None values
    for compound_id in compound_ids:
        if compound_id is None:
            modified_compound_ids.append(None)
            continue
            
        modified_id = compound_id
        # Use centralized TOKEN_SEPARATORS for consistent tokenization
        for sep in TOKEN_SEPARATORS:
            modified_id = modified_id.replace(sep, '/')
        # Remove quotes and other non-alphanumeric punctuation
        modified_id = modified_id.replace("'", '').replace('"', '')
        
        modified_compound_ids.append(modified_id)

        # Filtering tokens by stripping whitespace, removing empty, and filtering short tokens and pure numbers
        compound_tokens = [
            t.strip() for t in modified_id.split('/') 
        ]
        token_counts.update(set(compound_tokens))
           
    # Reconstruct the IDs keeping only the lowest-frequency tokens
    result = []
    for compound_id in modified_compound_ids:
        if compound_id is None:
            result.append(None)
            continue
            
        tokens = [
            t.strip() for t in compound_id.split('/') 
            #if t.strip() and len(t.strip()) >= min_token_length and not t.strip().isdigit()
        ]
        
        if not tokens:
            result.append(compound_id)
            continue
            
        min_frequency = min(token_counts.get(t, 0) for t in tokens)
        kept_tokens = [t for t in tokens if token_counts.get(t, 0) == min_frequency]
        
        current_best_token = kept_tokens[0]

        if len(kept_tokens) > 1:
            for i in range(1, len(kept_tokens)):
                current_best_token = __choose_suitable_token(current_best_token, kept_tokens[i])
        
        # Filter out experiment/data object keywords using existing constants
        is_experiment = any(current_best_token.startswith(keyword.lower()) for keyword in EXPERIMENT_KEYWORDS_LC)
        is_data_obj = any(current_best_token.startswith(keyword.lower()) for keyword in DATA_OBJECT_NAMES)
        if is_experiment or is_data_obj or current_best_token.lower().startswith("neo"):
            result.append(None)
            continue
        
        if(current_best_token.startswith("COSY") or  
           current_best_token.startswith("HMBC") or 
           current_best_token.startswith("Neo") or 
           current_best_token.startswith("NMR")):
            continue
        current_best_token = current_best_token.replace("compound",'')
        current_best_token = current_best_token.replace("MHBC",'')
        
        result.append(current_best_token)
            
    return result

def add_useful_files(compound_root_df, final_df):
    '''
        Deprecated.
        Collect files of interest. Deprecated. Most of these have been pruned away.
    '''
    # under the format: {compound_id: {dataset_folder: [files]}}
 
    (useful_file_map, path_map, cmpd_id_col_map) = __get_useful_file_maps(compound_root_df)

    # map the useful files to the correct compound_ids
    final_df['useful_files'] = final_df['compound_id'].map(lambda x: useful_file_map.get(x, {}))
    # map paths to the correct compound_ids
    final_df['path_text_final'] = final_df['compound_id'].map(lambda x: path_map.get(x, {}))
    final_df['compound_id_column'] = final_df['compound_id'].map(lambda x: cmpd_id_col_map.get(x, {}))

def map_dataset_folders(compound_root_df, final_df):
    '''
      Check if there are dataset folders within compound_id folders
      Return a DataFrame with columns "compound_id" and "dataset_folders"
    '''

    # problem here with mnova files, no "dataset" folders
    dataset_folder_map = {}
    path_map = {}
    cmpd_id_col_map = {}
    for _, row in compound_root_df.iterrows():
        path_parts = row['compound_id_path']
        compound_id = row['identified_compound_id']
        path_text_final = row['path_text_final']
        cmpd_id_col = row['compound_id_column']
        # check if there is a folder inside the compound_id folder
        if len(path_parts) <= 2:
            path_parts = [path_parts[0],path_parts[0],path_parts[0]]
        folder_name = path_parts[1]
        if compound_id not in dataset_folder_map:
            dataset_folder_map[compound_id] = set()
            dataset_folder_map[compound_id].add(folder_name)
            path_map[compound_id] = path_text_final
            cmpd_id_col_map[compound_id] = cmpd_id_col

    final_df['dataset_folders'] = final_df['compound_id'].map(
        lambda x: list(dataset_folder_map.get(x, []))
    )
    final_df['path_text_final'] = final_df['compound_id'].map(
        lambda x: path_map.get(x)
    )
    final_df['compound_id_column'] = final_df['compound_id'].map(
        lambda x: cmpd_id_col_map.get(x)
    )

def __is_candidate(folder, global_folders):
    '''
      Check:
       (a) that it is not a common global folder
       (b) that it is not one of the folders to ignore, and
       (c) that a candidate is NOT a defined data object (pdata, acqu, procar)

    '''
    if folder in global_folders:
        return False
    if __is_ignore_folder(folder):
        return False
    if __is_data_object(folder):
        return False
    return True

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
        max_token = 0
        for value, count in counts_series.items():
            if value in compound_id_folder:
                if count > max_count:
                    max_token = value
                    max_count = count
        tokens_in_compound_id_folder_name.append(max_token)

    # identify unique tokens
    tokens_in_compound_id_folder_name_set = set(tokens_in_compound_id_folder_name)

    # finds if a token is occuring a lot, and if it is, it's likely not part of the compound_id number
    final_df['compound_name'] = final_df['compound_id']
    for token_found in tokens_in_compound_id_folder_name_set:
        word_freq = tokens_in_compound_id_folder_name.count(token_found)
        if word_freq > len(tokens_in_compound_id_folder_name)/1.5:
            # removing "." from ".zip"
            final_df['compound_name'] = final_df['compound_name'].astype(str).str.replace(token_found, "")
            final_df['compound_name'] = final_df['compound_name'].astype(str).str.replace("of ", "")
            final_df['compound_name'] = final_df['compound_name'].astype(str).str.strip(".")

# abandoned -- fails when "1r" found in "S1r" jo4c02622    
# def lambda_find_full_important_file_path(row, paths_list):
#     '''
#     find the paths that end with an important file name that also have a compound id and a dataset folder
#     ARGH But "1r" will be found in "11r" or "S1r"

#     '''
#     for path in paths_list:
#         if (path.endswith(str(row['important_file'])) and 
#             str(row['compound_id']) in path and 
#             (str(row['dataset_folder']) in path or str(row['dataset_folder']) == "NA")
#         ):
#             return path
#     return None

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
    child_counts = df.groupby("compound_id")['dataset_folder'].nunique()

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
        parent_rows = df[df['compound_id'] == parent]

        # each dataset_folder becomes a new compound_id
        for child, group in parent_rows.groupby("dataset_folder"):
            if child == "NA" or pd.isna(child):
                continue
            child_group = group.copy()
            child_group['compound_id'] = child
            child_group['compound_name'] = child
            child_group['dataobject_ID'] = child_group['compound_name'] + " " + child_group['dataset_folder']
            new_rows.append(child_group)

    # remove original parent compound_id rows
    df = df[~df['compound_id'].isin(suspicious)]

    # add corrected child-compound_id rows
    if new_rows:
        df = pd.concat([df] + new_rows, ignore_index=True)
    print(f"Final identified compound_ids:\n {df['compound_id'].unique()} \n\n Final number of identified compound_ids: {df['compound_id'].nunique()}\n")
    return df


# def add_representations(df, paths_list):
#     '''
#         df will be df_for_TSV
#         Add a folder of png or images to compound_ids (ex: a PDF file in "HRMS For Publication")

#         @return possibly new df
#     '''
#     new_rows = []
#     folder_map = {}

#     # group files by their parent folder
#     for p in paths_list:
#         path_obj = Path(p)
#         parent = str(path_obj.parent)
#         if parent not in folder_map:
#             folder_map[parent] = []
#         folder_map[parent].append(path_obj.name.strip())

#     # check how many files in this specific folder match a known compound_id
#     compound_ids = df['compound_id'].dropna().unique().tolist()
#     for folder, files in folder_map.items():
#         match_count = 0
#         folder_matches = []
        
#         for f_path in files:
#             # check if any compound_id name exists in the filename
#             matched_comp = next((c for c in compound_ids if c in f_path), None)
#             if matched_comp and Path(f_path).suffix.lower() in FILE_EXTENSIONS_FOR_ADD_IMAGE:
#                 match_count += 1
#                 folder_matches.append((f_path, matched_comp))
                
#         # if more than 50% of files in this folder are relevant

#         if len(files) > 0 and (match_count / len(files)) > 0.5:

#             df = df[df['compound_id'] != folder[2:len(folder)-1]].reset_index(drop=True)
            
#             # NOTE: There is a current error in if the pdf name includes a compound_id in there, 
#             # even if the compound_id is not the intended compound_id
#             # HOWEVER: the row turns out funky and would be easy to not include
#             for f_path, comp in folder_matches:
#                 x = df[df['compound_id'] == comp]
#                 template = x.iloc[0].to_dict()
#                 template['important_file'] = Path(f_path).name
#                 template['matched_path'] = str(f_path)
#                 template['dataobject_id'] = "HRMS" # could change later
#                 template['dataset_folder'] = "NA"

#                 # trying to fix the error here
#                 if template['matched_path'] != "":
#                     new_rows.append(template)
#                 else:
#                     continue

#     # adds new rows
#     if new_rows:
#         df = pd.concat([df, pd.DataFrame(new_rows)], ignore_index=True)
#     return df

def generate_df_for_tsv(df, final_df):
    '''
    Create the data frame for ${doi}_final_output.tsv

    @return (new final_df, spec_ids, len(spec_ids))
    '''
    # creating the spectral ID column

    # expanding the final_df to df_for_TSV
    # NOTE: the difference in df_for_TSV is that each useful_file has it's own line and file path while final_df is one row per compound_id

    local_df = (
        final_df.assign(useful_files=final_df['useful_files'].map(lambda d: list(d.items())))
        .explode('useful_files')
        .assign(Key=lambda x: x['useful_files'].str.get(0),
                Value=lambda x: x['useful_files'].str.get(1))
        .drop(columns='pdata_path')
        .drop(columns='useful_files')
        .drop(columns='path_text_final')
        .reset_index(drop=True)
    )

    local2_df = (
        final_df.assign(path_text_final=final_df['path_text_final'].map(lambda d: list(d.items())))
        .explode('path_text_final')
        .assign(Key=lambda x: x['path_text_final'].str.get(0),
                Value=lambda x: x['path_text_final'].str.get(1).str.get(-1))
        .drop(columns='pdata_path')
        .drop(columns='useful_files')
        .drop(columns='compound_name')
        .drop(columns='dataset_folders')
        .drop(columns='path_text_final')
        .reset_index(drop=True)
    )

    local3_df = (
        final_df.assign(compound_id_column=final_df['compound_id_column'].map(lambda d: list(d.items())))
        .explode('compound_id_column')
        .assign(Key=lambda x: x['compound_id_column'].str.get(0),
                Value=lambda x: x['compound_id_column'].str.get(1).str.get(-1))
        .drop(columns='pdata_path')
        .drop(columns='useful_files')
        .drop(columns='compound_name')
        .drop(columns='dataset_folders')
        .drop(columns='path_text_final')
        .reset_index(drop=True)
    )

    #__print_df("local2_df", local2_df)
    # local2_df
    #     compound_id      Key                                  Value
    # 0           S1a  S1a 13C  jo4c02622_si_001/S1/S1a/S1a 13C/pdata
    # 1           S1a   S1a 1H   jo4c02622_si_001/S1/S1a/S1a 1H/pdata
    # 2           S1a       NA      jo4c02622_si_001/S1/S1a/S1a.mnova

    local_df = local_df.rename(columns={'Key': 'dataset_folder', 'Value': 'important_file'})
    local_df = local_df[["compound_name", 
        # "tech", 
        "compound_id", "dataset_folder", "important_file"]]

    def lambda_get_spec_id_from_dataset_folder(row):
        #print(f"{row}")
        cn = row['compound_name']

        for key in COMP_EXPERIMENT_KEYWORDS:
            if (cn.endswith(key)): 
                row['dataset_folder'] = cn
                cn = cn.replace(key,"")
                row['compound_name'] = cn
                break
        df = str(row['dataset_folder']) 
        if cn in df:
            return df.replace(" ", "_")
        return cn + "_" + df

    local_df['dataobject_id'] = local_df.apply(
         lambda row: lambda_get_spec_id_from_dataset_folder(row), axis='columns'
    )
       
    spec_ids = local_df['dataobject_id'].unique().tolist()

    local_df = local_df.explode('important_file')  

    # BH 2026.04.02 abandoning the search for path business
    # # create the two different path_text
    # df['path_text'] = df['path_text'].apply(lambda x: x[13:])
    # paths_list = df['path_text'].tolist()
    # paths_list_final = df['path_text_final'].tolist()

    # print("paths_list")
    # print(paths_list)

    # print("paths_list_final")
    # print(paths_list_final)

    # # creates column for matched_path which doesn't include the zip files (filtered out later)
    # local_df['matched_path'] = local_df.apply(
    #     lambda row: lambda_find_full_important_file_path(row, paths_list), axis='columns'
    # )

    # fixed dataobject_id for compound_ids with JDF files (now trying to do all extension files)
    mask = (local_df['important_file'].str.contains(".")) & (local_df['dataobject_id'].str.contains("NA"))
    local_df.loc[mask, "dataobject_id"] = local_df[mask].apply(
        lambda x: x['compound_id'] + x['important_file'].replace(x['compound_id'], "").replace("_", ""), 
        axis='columns'
    )

    # deprecated
    # local_df = add_representations(local_df, paths_list)

    local_df['path'] = local2_df['Value']
    local_df['cmpd_id_col'] = local3_df['Value']

    # # add the full file paths with zip files to local_df
    # local_df['full_matched_path'] = local_df.apply(
    #     lambda row: lambda_find_full_important_file_path(row, paths_list_final), axis='columns'
    # )

    # # drop matched_path (not needed for intended output)
    # local_df = local_df.drop('matched_path', axis='columns')

    # remove identified compound_id that contains way more folders than other identified compound_ids    
    local_df = prune_child_compound_ids(local_df, multiplier=4, min_children=6)
    
    local_df['path'] = local_df['path'].str.replace("pdata","")


    rows_order = ["cmpd_id_col", "compound_id", "compound_name", "dataobject_id", "path"]
    return (local_df[rows_order], spec_ids, len(spec_ids))

def generate_compound_map_df(df, compound_root_df):
    '''
    create a dataframe by unique compound id 
    that includes compound id column and arrays of values by dataset_folder
    '''

    # creating the final_df, includes all of the information with each compound_id label once
    local_df = pd.DataFrame(df['compound_id_label'].unique(), columns=['compound_id'])

    __print_df("local_df", local_df)

    #        compound_id
    # 0          S1a
    # 1          S1b
    # 2          S1c
    # 3          S1d
    # 4          S1e
    # 5          S1f
    # 6          S1g
    # 7          S1h
    # 8          S1i

    # creates dataset_folders column which includes the dataset folders for each identified compound_id
    map_dataset_folders(compound_root_df, local_df)

    __print_df("local_df-1", local_df)

    #    compound_id dataset_folders                         path_text_final
    # 0          S1a       [S1a 13C]   jo4c02622_si_001/S1/S1a/S1a 13C/pdata
    # 1          S1b       [S1b 13C]   jo4c02622_si_001/S1/S1b/S1b 13C/pdata
    # 2          S1c       [S1c 13C]   jo4c02622_si_001/S1/S1c/S1c 13C/pdata
    # 3          S1d       [S1d 13C]   jo4c02622_si_001/S1/S1d/S1d 13C/pdata

    add_useful_files(compound_root_df, local_df)

    __print_df("local_df-2", local_df)

    #    compound_id dataset_folders                                                                                                                                              path_text_final                                                       useful_files
    # 0          S1a       [S1a 13C]      {'S1a 13C': ['jo4c02622_si_001/S1/S1a/S1a 13C/pdata'], 'S1a 1H': ['jo4c02622_si_001/S1/S1a/S1a 1H/pdata'], 'NA': ['jo4c02622_si_001/S1/S1a/S1a.mnova']}   {'S1a 13C': ['pdata'], 'S1a 1H': ['pdata'], 'NA': ['S1a.mnova']}
    # 1          S1b       [S1b 13C]      {'S1b 13C': ['jo4c02622_si_001/S1/S1b/S1b 13C/pdata'], 'S1b 1H': ['jo4c02622_si_001/S1/S1b/S1b 1H/pdata'], 'NA': ['jo4c02622_si_001/S1/S1b/S1b.mnova']}   {'S1b 13C': ['pdata'], 'S1b 1H': ['pdata'], 'NA': ['S1b.mnova']}
    # 2          S1c       [S1c 13C]      {'S1c 13C': ['jo4c02622_si_001/S1/S1c/S1c 13C/pdata'], 'S1c 1H': ['jo4c02622_si_001/S1/S1c/S1c 1H/pdata'], 'NA': ['jo4c02622_si_001/S1/S1c/S1c.mnova']}   {'S1c 13C': ['pdata'], 'S1c 1H': ['pdata'], 'NA': ['S1c.mnova']}


    # tidy up the ' + ' business
    local_df['compound_id'] = local_df['compound_id'].str.replace(' + ', '+', regex=False)         

    # Strip repeated tokens to remove repeated tokens
    compound_id_list = local_df['compound_id'].dropna().tolist()
    stripped_ids = __strip_repeated_tokens(compound_id_list)
    
    # Create mapping from original to stripped IDs (1-to-1, preserving order)
    id_mapping = {}
    for original_id, stripped_id in zip(compound_id_list, stripped_ids):
        if stripped_id is not None and stripped_id.strip():
            id_mapping[original_id] = stripped_id
        else:
            id_mapping[original_id] = original_id  
    local_df['compound_id'] = local_df['compound_id'].map(id_mapping).fillna(local_df['compound_id'])

    # output the file paths
    unique_entries_set = set(itertools.chain.from_iterable(local_df['dataset_folders']))
    file_path_keywords = KNOWN_DATASET_FILES + list(unique_entries_set)

    filtered_df_regex = compound_root_df[compound_root_df['compound_id_path'].apply(lambda x: any(k in x for k in file_path_keywords))]

    filtered_df_regex = compound_root_df.loc[
        compound_root_df['compound_id_path'].str[-1].eq("pdata")
    ].copy()

    filtered_df_regex['my_string'] = filtered_df_regex['compound_id_path'].str.join("/")

    #print(f"compound_root_df={compound_root_df}")

    def lambda_path_to_pdata(path):
        '''
            finding pdata paths
        '''
        if "pdata" in path:
            idx = path.index("pdata")
            if idx > 0: # this goes back TWO paths or ONE path? set to ONE here
                return path[:idx]
        return path


    compound_root_df['compound_id_path_to_pdata'] = compound_root_df['compound_id_path'].apply(lambda_path_to_pdata)

    #__print_column(compound_root_df,'compound_id_path') 

    #__print_column(compound_root_df,'compound_id_path_to_pdata') # nan nan nan

    long_string = "\n".join(compound_root_df['compound_id_path_to_pdata'].apply(lambda x: " ".join(x)))
    long_string = long_string.replace(" + ", "+")

    # cleaning the compound_id paths + counting tokens to get "compound_name"
    tokens = re.split(r'[ \n\.\/]+', long_string)
    tokens = [t for t in tokens if t]
    counts_series = pd.Series(tokens).value_counts()

    #print(f"tokens={tokens}")
    #print(f"counts_series={counts_series}")
    for val in local_df['compound_id']:
        val = str(val).replace(" + ", "+")
    local_df['compound_id'] = local_df['compound_id'].str.replace(' + ', '+', regex=False)               
    local_df = local_df.dropna()

    
    get_final_compound_name(local_df, counts_series)

    #print(f"line954 {local_df.to_string()}")
    
    # writing the pdata file path
    filtered_df_regex = compound_root_df[compound_root_df['compound_id_path'].apply(lambda x: x[-1] == "pdata")].copy()
    filtered_df_regex['my_string'] = filtered_df_regex['compound_id_path'].str.join('/')
    temp_df = filtered_df_regex.copy()
    temp_df['compound_name'] = temp_df['my_string'].str.split('/').str[0]
    path_mapping = temp_df.groupby('compound_name')['my_string'].apply(list).reset_index()
    path_mapping.columns = ['compound_id', 'pdata_path']

    local_df = local_df.merge(path_mapping, on='compound_id', how='left')
    local_df['pdata_path'] = local_df['pdata_path'].apply(lambda x: x if isinstance(x, list) else [])


    # organizing rows of df (could probably delete this)
    rows_order = ["compound_id_column", "compound_name", "compound_id", "dataset_folders", "pdata_path", "useful_files", "path_text_final"]

    #__print_df("local_df", local_df)
    local_df = local_df[rows_order]


    return local_df

def __add_parent_folder_column(df, final_df, ignore_parents=None):
    '''
    Adds a parent folder if the experiment folder is identified as a compound_id
    '''
    if ignore_parents is None:
        ignore_parents = IGNORE_PARENTS
    parent_map = {}
    for _, row in df.iterrows():
        compound_id = row['compound_id_label']
        parts = row['parts']
        slashes = row['parent_count']

        if compound_id is None or slashes is None:
            continue
        parent_idx = slashes - 1
        if parent_idx < 0 or parent_idx >= len(parts):
            continue
        parent = parts[parent_idx]
        if parent in ignore_parents or parent == compound_id:
            continue
        parent_map.setdefault(compound_id, []).append(parent)

    def private_map_choose_parent(compound_id):
        '''
        Choose most common parent for a given compound_id

        '''
        parents = parent_map.get(compound_id, [])
        if not parents:
            return None
        return Counter(parents).most_common(1)[0][0]

    final_df['parent_folder'] = final_df['compound_id'].map(private_map_choose_parent)

def add_compound_ids_from_experiment_parents(df, final_df, path_parts_zip):
    '''    
    drop columns with identified compound_ids that match columns identified as dataset folders
    @return new final_df
    '''
    all_compound_ids = set(final_df['compound_id'].dropna().unique())

    final_df['__is_match'] = final_df.apply(
        lambda row: any(folder in all_compound_ids for folder in row['dataset_folders']) 
        if isinstance(row['dataset_folders'], list) else False, 
        axis='columns'
    )

    temp_df = final_df[~final_df['__is_match']]

    if len(temp_df) == 0:
        return final_df

    final_df = temp_df.drop(columns=['__is_match'])

    def lambda_set_is_experiment(row):
        '''
        Operates on final_df to give final_df['__is_experiment'] column.

        If an identified compound_id has dataset folders = pdata for now 
        '''
        return isinstance(row['dataset_folders'], list) and 'pdata' in row['dataset_folders']

    final_df['__is_experiment'] = final_df.apply(lambda_set_is_experiment, axis='columns')

    # call the second check functions

    __add_parent_folder_column(df, final_df)
    compound_ids_to_be_added = set(final_df[final_df['__is_experiment'] == True]['parent_folder'])
    final_df = final_df[~final_df['__is_experiment']]
    final_df = final_df.drop(columns=['__is_experiment'])
    try:
        final_df = final_df.drop(columns=['parent_folder'])
    except:
        print("error in Python dropping parent folder!")
        
    def private_add_compound_id(df, final_df, compound_id, path_parts_zip):
        '''
            Add a compound_id to final_df from scratch. 
            If another function identifies a compound_id that was not originally identified, 
            build this compound_id info

            @return a new final_df
        '''

        dataset_folders = map_dataset_folders_for_compound_id(df, compound_id)
        pdata_paths = []
        df_bruker = df[df['parts'].apply(lambda x: x[-1] == "pdata")]
        for parts in df_bruker['parts']:
            idx = 0
            for i in parts:
                idx += 1
                if i == compound_id:
                    new_list = parts[idx-1:]
                    new_path = '/'.join(new_list)
                    pdata_paths.append(new_path)

        # print(f"compoundID {compound_id}")

        new_data = []
        new_paths = []
        new_col = []
        this_col = -1
        this_path = 0
        for parts, path in path_parts_zip:
            if compound_id in parts:
                idx = parts.index(compound_id)
                if parts[idx] == compound_id:
                    print(f"found {idx}")
                    new_data.append([parts[idx]] + parts[idx+1:])
                    new_paths.append(path)
                    new_col.append(idx)
                    this_path = path
                    this_col = idx

        if this_col < 0:
            return final_df

        compound_id_df = pd.DataFrame({"compound_id_column":new_col, "path_text_final": new_paths, "compound_id_path": new_data, "identified_compound_id": compound_id})

        #__print_df("compound_id_df", compound_id_df)
 
        #__print_df("cid", compound_id_df)
        (useful_file_map, _, _) = __get_useful_file_maps(compound_id_df)

        #print(f"{compound_id} ufm {useful_file_map}")

        new_row = {
            "compound_id_column": this_col,
            "compound_name": compound_id,
            "compound_id": compound_id,
            "dataset_folders": sorted(set(dataset_folders)),
            "pdata_path": sorted(set(pdata_paths)),
            "useful_files": useful_file_map[compound_id],
            "path_final_text": this_path
        }

        #print(f"new row {new_row}")
        #__print_df("final_df33", final_df)
        return pd.concat([final_df, pd.DataFrame([new_row])],ignore_index=True)

    if compound_ids_to_be_added:
        print(f"compound_ids to be added (but skipped!) {compound_ids_to_be_added}")

    # for compound_id in compound_ids_to_be_added:
    #     final_df = private_add_compound_id(df, final_df, compound_id, path_parts_zip)
    return final_df

# def generate_NMR_technique_column(final_df):
#     '''
#     Create the "tech" column in final_df for pdata records 

#     not called -- Deprecated -- that's just Bruker, not others; no longer necessary 
#     '''
#     # NOTE: This column is not finalized/working 100%
#     final_df['tech'] = "NA"
#     mask = final_df['pdata_path'].apply(lambda row: len(row) > 0 and ("pdata" in row[0]))
#     final_df.loc[mask, "tech'] = "NMR"

def get_missing_files(df, tsv_df):
    paths = list(tsv_df['path'].dropna())

    # Filter using a list comprehension for the boolean mask
    mask = [not any(sub in str(val) for sub in paths) for val in df['path_text_final'].dropna()]
    df_local = df[mask]
    return list(df_local['path_text_final'])


############## end of method definitions


# Read file paths and set global variables
filename = f"{dataset_DOI}_file_list.txt"
with open(filename, "r", encoding="utf-8") as f:
    paths = [str(line.strip()) for line in f if line.strip()]



(df, path_parts) = create_data_frame(paths)

path_parts_zip = zip(path_parts, paths)

overall_path_count = len(df)

__print_column(df, "nonduplicated_candidates")
# set the initial set of possible candidates
all_initial_candidates = df['initial_candidates'].explode().tolist()
print(f"\n\nAll initial compound_id candidates: \n {set(all_initial_candidates)}\n")

###### frequency analysis for global folders

# get the global folders
nonduplicated_candidates = df['nonduplicated_candidates'].explode().tolist()
global_folders = get_global_folders(paths, nonduplicated_candidates, Counter(nonduplicated_candidates))
print(f"Identified global folders: \n {global_folders}\n")

# update for global folders
candidate_freq = Counter(all_initial_candidates)
df = update_df_for_global_folders(df, global_folders, candidate_freq)

# create compound_root_df, includes all of the filepaths after compound_ids
# this assumes that all the important information is after the compound_id 
compound_root_df = get_data_frame_for_compound_paths(df)

__print_df("crd", compound_root_df)

# compound_root_df
#      compound_id_column           compound_id_path identified_compound_id                         path_text_final
# 0                     1   [S1a, S1a 13C, 0, pdata]                    S1a   jo4c02622_si_001/S1/S1a/S1a 13C/pdata
# 1                     1    [S1a, S1a 1H, 0, pdata]                    S1a    jo4c02622_si_001/S1/S1a/S1a 1H/pdata
# 2                     1           [S1a, S1a.mnova]                    S1a       jo4c02622_si_001/S1/S1a/S1a.mnova
# 3                     1   [S1b, S1b 13C, 0, pdata]                    S1b   jo4c02622_si_001/S1/S1b/S1b 13C/pdata

final_df = generate_compound_map_df(df, compound_root_df)

__print_df("final_df", final_df)

#                     compound_id_column                         compound_name compound_id dataset_folders                                       pdata_path                                                                useful_files                                                                                                                                                                                                                                                              path_text_final
# 0                   {'S1a 13C': [1], 'S1a 1H': [1], 'NA': [1]}           S1a         S1a       [S1a 13C]        [S1a/S1a 13C/0/pdata, S1a/S1a 1H/0/pdata]       {'S1a 13C': ['pdata'], 'S1a 1H': ['pdata'], 'NA': ['S1a.mnova']}                                                                                                                           {'S1a 13C': ['jo4c02622_si_001/S1/S1a/S1a 13C/pdata'], 'S1a 1H': ['jo4c02622_si_001/S1/S1a/S1a 1H/pdata'], 'NA': ['jo4c02622_si_001/S1/S1a/S1a.mnova']}
# 1                   {'S1b 13C': [1], 'S1b 1H': [1], 'NA': [1]}           S1b         S1b       [S1b 13C]        [S1b/S1b 13C/0/pdata, S1b/S1b 1H/0/pdata]       {'S1b 13C': ['pdata'], 'S1b 1H': ['pdata'], 'NA': ['S1b.mnova']}                                                                                                                           {'S1b 13C': ['jo4c02622_si_001/S1/S1b/S1b 13C/pdata'], 'S1b 1H': ['jo4c02622_si_001/S1/S1b/S1b 1H/pdata'], 'NA': ['jo4c02622_si_001/S1/S1b/S1b.mnova']}
# 2                   {'S1c 13C': [1], 'S1c 1H': [1], 'NA': [1]}           S1c         S1c       [S1c 13C]        [S1c/S1c 13C/0/pdata, S1c/S1c 1H/0/pdata]       {'S1c 13C': ['pdata'], 'S1c 1H': ['pdata'], 'NA': ['S1c.mnova']}                                                                                                                           {'S1c 13C': ['jo4c02622_si_001/S1/S1c/S1c 13C/pdata'], 'S1c 1H': ['jo4c02622_si_001/S1/S1c/S1c 1H/pdata'], 'NA': ['jo4c02622_si_001/S1/S1c/S1c.mnova']}


# final check for compound IDs from experiment parent. Necessary?
final_df = add_compound_ids_from_experiment_parents(df, final_df, path_parts_zip)

# get the final compound IDs and write them out
final_compound_names = final_df['compound_name'].unique().tolist()
final_compound_ids = final_df['compound_id'].unique().tolist()
final_compound_id_count = len(final_compound_ids)

(tsv_df, final_spec_ids, final_spec_id_count) = generate_df_for_tsv(df, final_df)

__print_df("tsv_df", tsv_df)

missing_files = get_missing_files(df, tsv_df)
missing_files_count = len(missing_files)
if missing_files_count > 0:
    print(f"{missing_files_count} missing files:\n{'\n'.join(missing_files)}")

########### write files

# write <doi>_compound_id_list.txt and <doi>_compound_id_count
# compound_id_count is a file with length the number of compound_ids

with open(f'{dataset_DOI}_compound_name_list.txt', "w", encoding="utf-8") as f:
    f.write(f'{dataset_DOI} '+';'.join(list(map(str, final_compound_names)))+'\n')
with open(f'{dataset_DOI}_compound_id_list.txt', "w", encoding="utf-8") as f:
    f.write(f'{dataset_DOI} '+';'.join(list(map(str, final_compound_ids)))+'\n')
with open(f'{dataset_DOI}_compound_id_count', "w", encoding="utf-8") as f:
    f.write("0" * final_compound_id_count)


# debugging only?
# write the final csv data frame now, before generating df_final for TSV
# final_df.to_csv(f'{dataset_DOI}_output.csv', index=False)

# NOTE deprecated: generate_NMR_technique_column(final_df)

# create the df for the TSV file along with the list of unique spec_ids

#__print_df("final_df", final_df)

# final_df
#    compound_id dataset_folders                         path_text_final
# 0          S1a       [S1a 13C]   jo4c02622_si_001/S1/S1a/S1a 13C/pdata
# 1          S1b       [S1b 13C]   jo4c02622_si_001/S1/S1b/S1b 13C/pdata
# 2          S1c       [S1c 13C]   jo4c02622_si_001/S1/S1c/S1c 13C/pdata
# 3          S1d       [S1d 13C]   jo4c02622_si_001/S1/S1d/S1d 13C/pdata

#     compound_name compound_id dataobject_id                  path
# 0             S1a         S1a    S1a_13C   jo4c02622_si_001/S1/S1a/S1a 13C/
# 1             S1a         S1a     S1a_1H    jo4c02622_si_001/S1/S1a/S1a 1H/
# 2             S1a         S1a  S1a.mnova  jo4c02622_si_001/S1/S1a/S1a.mnova
# 3             S1b         S1b    S1b_13C   jo4c02622_si_001/S1/S1b/S1b 13C/
# 4             S1b         S1b     S1b_1H    jo4c02622_si_001/S1/S1b/S1b 1H/


tsv_df.to_csv(f'{dataset_DOI}_final_output.tsv', index=False, sep='\t')
# write <doi>_spec_id_list.txt and <doi>_spec_id_count
with open(f'{dataset_DOI}_data_object_id_list.txt', "w", encoding="utf-8") as f:
    f.write(f'{dataset_DOI} '+';'.join(list(map(str, final_spec_ids)))+'\n')
with open(f'{dataset_DOI}_data_object_id_count', "w", encoding="utf-8") as f:
    f.write("0" * final_spec_id_count)

with open(f'{dataset_DOI}_missing_files_list.txt', "w", encoding="utf-8") as f:
    f.write(f'{dataset_DOI} '+';'.join(missing_files)+'\n')
with open(f'{dataset_DOI}_missing_files_list.tsv', "w", encoding="utf-8") as f:
    f.write('\n'.join(missing_files)+'\n')
with open(f'{dataset_DOI}_missing_files_count', "w", encoding="utf-8") as f:
    f.write("0" * missing_files_count)

print(f"created {dataset_DOI}_compound_id_list.txt")
print(f"created {dataset_DOI}_compound_name_list.txt")
print(f"created {dataset_DOI}_data_object_id_list.txt")
print(f"created {dataset_DOI}_final_output.tsv")
if missing_files_count > 0:
    print(f"created {dataset_DOI}_missing_files_list.txt")
    print(f"created {dataset_DOI}_missing_files_list.tsv")

print((f"{dataset_DOI} final compound ID count: {final_compound_id_count}\n"
        f"{dataset_DOI} final data object ID count: {final_spec_id_count}\n"
        f"{dataset_DOI} missing files count: {missing_files_count}"))


