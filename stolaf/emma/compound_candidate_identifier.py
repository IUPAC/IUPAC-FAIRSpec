import pandas as pd
from pathlib import Path
from collections import Counter
import re
import itertools
import sys
import numpy as np

# command line arguments
dataset_DOI = sys.argv[1]

# Read file paths
filename = f"file_list_{dataset_DOI}.txt"

with open(filename, "r") as f:
    paths = [line.strip() for line in f if line.strip()]

path_parts = [list(Path(p).parts) for p in paths]
df = pd.DataFrame({"parts": path_parts})

# create file paths with the zip files
df["path_text_final"] = df["parts"].apply(lambda x: " / ".join(x))
df["path_text_final"] = df["path_text_final"].str.replace(" / ", "/")
df["path_text_final"] = df["path_text_final"].str.replace(".zip../", ".zip|")
df["path_text_final"] = df["path_text_final"].str.replace("../test2/", "")

# create clean file paths for analysis
df['parts'] = df['parts'].apply(lambda x: [item for item in x if '.zip..' not in item])
df["path_text"] = df["parts"].apply(lambda x: " / ".join(x))

TOTAL_PATHS = len(df)

# Domain knowledge
IGNORE_FOLDERS = {
    "pdata", "fid", "ser", "used_from", "StartingMaterial", ".DS_Store", "Catalyst"
}

FILE_LIKE_NAMES = {
    "acqu", "acqus", "acqu2s", "proc", "procs", "specpar", "title", "outd", "shimvalues",
    "scon2", "pulseprogram", "cpdprg2", "precom.output", "fq1list", "vtc_pid_settings", "Catalyst"
}

# NOTE: 1h keyword is removed because of frequent use of 1h also as a compound number
EXPERIMENT_KEYWORDS = [
    "13c",  "cosy", "hsqc", "hmbc", "dept", "jmod", "noesy", "13c jmod"
]

FILE_EXTENSIONS = {
    ".txt", ".par", ".fid", ".ser", ".json", ".xml", ".temp", ".png", ".info"
}

# identify compound candidates
def is_file_like(name):
    n = name.lower()
    return (
        n in FILE_LIKE_NAMES
        or any(n.endswith(ext) for ext in FILE_EXTENSIONS)
    )

def contains_experiment_token(name):
    n = name.lower()
    return any(k in n for k in EXPERIMENT_KEYWORDS)

# true if the folder name represents ONLY an experiment, not a compound-experiment hybrid
def is_pure_experiment_folder(name):
    n = name.lower().strip()
    if not contains_experiment_token(n):
        return False
    cleaned = n
    for k in EXPERIMENT_KEYWORDS:
        cleaned = cleaned.replace(k, "")
    cleaned = re.sub(r"[^a-z0-9]+", "", cleaned)
    return len(cleaned) == 0   
  
def is_basic_candidate(folder):
    f = folder.strip()
    if is_file_like(f):
        return False
    if f.lower() in (x.lower() for x in IGNORE_FOLDERS):
        return False
    return True

# identify initial candidates
df["initial_candidates"] = df["parts"].apply(
    lambda parts: [p for p in parts if is_basic_candidate(p)]
)


# exclude global folders from candidates
all_initial_candidates = [
    c for sublist in df["initial_candidates"] for c in sublist
]

# print initial candidates
print(f"\n\nAll initial compound candidates: \n {set(all_initial_candidates)}\n")

candidate_freq = Counter(all_initial_candidates)
GLOBAL_THRESHOLD = 0.8  # appears in ≥80% of paths

GLOBAL_FOLDERS = {
    folder for folder, count in candidate_freq.items()
    if count / TOTAL_PATHS >= GLOBAL_THRESHOLD
}

# print global folders
print(f"Identified global folders: \n {GLOBAL_FOLDERS}\n")

def is_candidate(folder):
    if is_file_like(folder):
        return False
    if folder.lower() in (x.lower() for x in IGNORE_FOLDERS):
        return False
    if folder in GLOBAL_FOLDERS:
        return False
    return True

# re-extract candidates after removing globals
df["compound_candidates"] = df["parts"].apply(
    lambda parts: [p for p in parts if is_candidate(p)]
)

# return folder that contains a .jdf file and loosely matches the folder name
def parent_of_matching_jdf(parts):
    for i in range(len(parts) - 1):
        parent = parts[i]
        child = parts[i + 1]
        if (child.lower().endswith(".jdf")):
          return parent
    return None

# return the parent folder of an experiment folder
def clean_parent_of_experiment(parts, candidates):
    for parent, child in zip(parts, parts[1:]):
        if (
            parent in candidates
            and is_candidate(parent)
            and not contains_experiment_token(parent)
            and contains_experiment_token(child)
        ):
            return parent
    return None

# identify compound candidate folders (instructions with jdf files and parent files of experiments)
def choose_weak_label(row, order, min_freq=3, max_frac=0.5):
    parts = row["parts"]
    candidates = row["compound_candidates"]
    parent = clean_parent_of_experiment(parts, candidates)
    jdf_parent = parent_of_matching_jdf(parts)

    if parent and not is_file_like(parent):
        return parent

    if jdf_parent != None:
      return jdf_parent
      
    # frequency-based heuristic
    ranked = sorted(candidates, key=lambda x: (row["parts"].index(x), -candidate_freq[x]))

    for c in ranked:
        if is_file_like(c):
            continue
        if is_pure_experiment_folder(c):
            continue
        if (candidate_freq[c] >= min_freq and candidate_freq[c] / TOTAL_PATHS <= max_frac):
            return c
    return None

# count the slashes before identified compound folders
def count_slashes_before(row, path_col, compound_col):
  path = str(row[path_col]).replace('\\', '/')
  compound = str(row[compound_col])
  idx = path.lower().find(compound.lower())
  if idx == -1:
    return None
  return path[:idx].count('/')

# get the contents of each compound and get the filepaths after compounds
def get_filepaths_after_compounds(df):
    valid_labels = set(df["compound_label"].dropna())
    records = [
        {"compound_path": parts[s:], "identified_compound": parts[s]}
        for parts, s in zip(df["parts"], df["slashes_before"])
        if s is not None and s < len(parts) and parts[s] in valid_labels
    ]
    return pd.DataFrame(records)

# identify each file path with a compound
df["compound_label"] = df.apply(choose_weak_label, axis=1, order=1)

# filter the original DataFrame for rows where the count is greater than or equal to the average 
class_name_average = df["compound_label"].value_counts().mean()
occurrence_per_row = df.groupby('compound_label')['compound_label'].transform('count')
df = df[occurrence_per_row >= class_name_average - (class_name_average/.75)]

# add slashes_before column to df
df['slashes_before'] = df.apply(
    lambda row: count_slashes_before(row, 'path_text','compound_label'),
    axis=1
)

# create diff_df, includes all of the filepaths after compounds
diff_df = get_filepaths_after_compounds(df)

# creating the final_df, includes all of the information with each compound label once
final_df = df["compound_label"].unique()
final_df = pd.DataFrame(final_df, columns=['compound'])

# identifying if there are test folders within compound folders
def map_test_folders(diff_df, final_df):
  test_folder_map = {}
  
  for _, row in diff_df.iterrows():
      path_parts = row["compound_path"]
      compound = row["identified_compound"]
      
      # check if there is a folder inside the compound folder
      if len(path_parts) > 2:
          folder_name = path_parts[1]
          
          if compound not in test_folder_map:
              test_folder_map[compound] = set()
          test_folder_map[compound].add(folder_name)
  
  final_df['test_folders'] = final_df['compound'].map(
      lambda x: list(test_folder_map.get(x, []))
  )
  return final_df

# creates test_folders column which includes the test folders for each identified compound
final_df = map_test_folders(diff_df, final_df)

# map the test folders to the correct compound
def map_test_folders_for_compound(diff_df, final_df, target_compound):
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

# collect files of interest
def list_useful_files(diff_df):
    # under the format: {compound: {test_folder: [files]}}
    useful_file_map = {}

    # NOTE: useful_extensions can be altered to find any files of interest
    useful_extensions = ('.mol', '.jdf' ,'.mnova', 'fid', 'pdata', '1r', 'acqu', '2rr', '2ri', '2ir', '2ii', 'ser', 'acqu2s', '.pdf', '.png')

    for _, row in diff_df.iterrows():
        path_parts = row["compound_path"]
        compound = row["identified_compound"]
        if len(path_parts) > 2:
            test_folder = path_parts[1]
            
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
  
# extract the useful files from the diff_df (includes all file paths after the compounds)
useful_files = list_useful_files(diff_df)

# map the useful files to the correct compounds
final_df['useful_files'] = final_df['compound'].map(lambda x: useful_files.get(x, {}))
final_df['compound'] = final_df['compound'].str.replace(' + ', '+', regex=False)         

   
# output the file paths
unique_entries_set = set(itertools.chain.from_iterable(final_df['test_folders']))
FILE_PATH_KEYWORDS = ['1r', 'fid', '1i', '.mol', 'jdf', '.mnova', 'acqus'] + list(unique_entries_set)
filtered_df_regex = diff_df[diff_df['compound_path'].apply(lambda x: any(k in x for k in FILE_PATH_KEYWORDS))]

filtered_df_regex = diff_df.loc[
    diff_df["compound_path"].str[-1].eq("pdata")
].copy()

filtered_df_regex["my_string"] = filtered_df_regex["compound_path"].str.join("/")

# finding pdata paths
def clean_compound_path(path):
    if "pdata" in path:
        idx = path.index("pdata")
        if idx > 0:
            return path[:idx-1]
    return path

diff_df["compound_path_clean"] = diff_df["compound_path"].apply(clean_compound_path)

long_string = "\n".join(diff_df['compound_path_clean'].apply(lambda x: " ".join(x)))
long_string = long_string.replace(" + ", "+")

# cleaning the compound paths + counting tokens to get "compound_name"
tokens = re.split(r'[ \n\.\/]+', long_string)
tokens = [t for t in tokens if t]
counts_series = pd.Series(tokens).value_counts()

for val in final_df['compound']:
  val = val.replace(" + ", "+")
final_df['compound'] = final_df['compound'].str.replace(' + ', '+', regex=False)               

# now to get the just compound names, compare the tokens with the identified folders
# if a compound folder name contains multiple tokens, select the one where it is less frequent
# OR filter out the most common token in the folder name 
tokens_in_compound_folder_name = []
for compound_folder in final_df['compound']:
  max_count = 0
  for value, count in counts_series.items():
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
rows_order = ["compound_name", "compound", "test_folders", "pdata_path", "useful_files"]
final_df = final_df[rows_order]

# drops identified compounds who has test folders that are also identified compounds
all_compounds = set(final_df['compound'].dropna().unique())

final_df['is_match'] = final_df.apply(
    lambda row: any(folder in all_compounds for folder in row['test_folders']) 
    if isinstance(row['test_folders'], list) else False, 
    axis=1
)

final_df = final_df[~final_df['is_match']]
final_df = final_df.drop(columns=['is_match'])

# if an identified compound has test folders = pdata, then change identified compound to the parent folder
def update_to_parent(row):
    if isinstance(row['test_folders'], list) and 'pdata' in row['test_folders']:
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
    test_folders = map_test_folders_for_compound(df, final_df, compound)
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
    useful_files = list_useful_files(compound_df)
    useful_files = useful_files[compound]

    new_row = {
        "compound_name": compound,
        "compound": compound,
        "test_folders": sorted(set(test_folders)),
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
print(f"\nFinal identified compounds:\n {final_df['compound'].unique()} \n\n Final number of identified compounds:{final_df['compound'].nunique()}\n")

# create the final csv after the second run through
final_df.to_csv(f'output_{dataset_DOI}.csv', index=False)

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

df_exploded = df_exploded.rename(columns={'Key': 'test_folder', 'Value': 'important_file'})
df_exploded = df_exploded[["compound_name", "tech", "compound", "test_folder", "important_file"]]
df_exploded["spectra_ID"] = df_exploded["compound_name"] + ' ' + df_exploded["test_folder"]
df_exploded = df_exploded.explode('important_file')  

# create the two different path_text
df['path_text'] = df['path_text'].apply(lambda x: x[13:])
paths_list = df['path_text'].tolist()

paths_list_final = df["path_text_final"].tolist()

def find_matching_path(row, paths_list):
    for path in paths_list:
        if (path.endswith(str(row['important_file'])) and 
            str(row['compound']) in path and 
            str(row['test_folder']) in path):
            return path
        elif (path.endswith(str(row['important_file'])) and
              str(row['compound']) in path and
              str(row['test_folder']) == "NA"):
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
            template["test_folder"] = "NA"

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

    # count number of distinct test folders per compound
    child_counts = df.groupby("compound")["test_folder"].nunique()

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

        # each test_folder becomes a new compound
        for child, group in parent_rows.groupby("test_folder"):
            if child == "NA" or pd.isna(child):
                continue
            child_group = group.copy()
            child_group["compound"] = child
            child_group["compound_name"] = child
            child_group["spectra_ID"] = child_group["compound_name"] + " " + child_group["test_folder"]
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
df_exploded.to_csv(f'final_output_{dataset_DOI}.tsv', index=False, sep='\t')

# print output info to terminal
print(f"Final Output: \n \t final_output_{dataset_DOI}.tsv \n\t output_{dataset_DOI}.csv \n\t file_list_{dataset_DOI}.txt\n")
