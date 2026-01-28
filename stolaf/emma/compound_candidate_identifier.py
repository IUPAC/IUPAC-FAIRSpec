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

print(path_parts)

df = pd.DataFrame({"parts": path_parts})

df["path_text_final"] = df["parts"].apply(lambda x: " / ".join(x))
df["path_text_final"] = df["path_text_final"].str.replace(" / ", "/")
df["path_text_final"] = df["path_text_final"].str.replace(".zip../", ".zip|")
df["path_text_final"] = df["path_text_final"].str.replace("../test2/", "")

df['parts'] = df['parts'].apply(lambda x: [item for item in x if '.zip..' not in item])
df["path_text"] = df["parts"].apply(lambda x: " / ".join(x))

print(df['path_text'])
print(df["path_text_final"])

TOTAL_PATHS = len(df)

# Domain knowledge

IGNORE_FOLDERS = {
    "pdata", "fid", "ser", "used_from", "StartingMaterial", ".DS_Store", "Catalyst"
}

FILE_LIKE_NAMES = {
    "acqu", "acqus", "acqu2s", "proc", "procs", "specpar", "title", "outd", "shimvalues",
    "scon2", "pulseprogram", "cpdprg2", "precom.output", "fq1list", "vtc_pid_settings", "Catalyst"
}

# I am temporarily removing the 1h keyword, still working ok for some reason
EXPERIMENT_KEYWORDS = [
    "13c",  "cosy", "hsqc", "hmbc", "dept", "jmod", "noesy", "13c jmod"
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

   
def is_pure_experiment_folder(name):
    # True if the folder name represents ONLY an experiment, not a compound-experiment hybrid
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

df["initial_candidates"] = df["parts"].apply(
    lambda parts: [p for p in parts if is_basic_candidate(p)]
)


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

def parent_of_matching_jdf(parts):
    # Return candidate folder that contains a .jdf file whose name
    # loosely matches the folder name.
    for i in range(len(parts) - 1):
        parent = parts[i]
        child = parts[i + 1]
        if (child.lower().endswith(".jdf")):
          return parent

    return None

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

def choose_weak_label(row, order, min_freq=3, max_frac=0.5):
    parts = row["parts"]
    candidates = row["compound_candidates"]
    parent = clean_parent_of_experiment(parts, candidates)
    jdf_parent = parent_of_matching_jdf(parts)

    if parent and not is_file_like(parent):
        return parent

    if jdf_parent != None:
      print(jdf_parent)
      return jdf_parent
      
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

    return None

def count_slashes_before(row, path_col, compound_col):
  path = str(row[path_col]).replace('\\', '/')
  compound = str(row[compound_col])
  idx = path.lower().find(compound.lower())
  if idx == -1:
    return None
    
  return path[:idx].count('/')


# get the contents of each compound
# get filepaths after compounds
def get_filepaths_after_compounds(df):
    valid_labels = set(df["compound_label"].dropna())
    records = [
        {"compound_path": parts[s:], "identified_compound": parts[s]}
        for parts, s in zip(df["parts"], df["slashes_before"])
        if s is not None and s < len(parts) and parts[s] in valid_labels
    ]
    return pd.DataFrame(records)

df["compound_label"] = df.apply(choose_weak_label, axis=1, order=1)

class_name_average = df["compound_label"].value_counts().mean()
occurrence_per_row = df.groupby('compound_label')['compound_label'].transform('count')

# Filter the original DataFrame for rows where the count is greater than or equal to the average
df = df[occurrence_per_row >= class_name_average - (class_name_average/.75)]


df['slashes_before'] = df.apply(
    lambda row: count_slashes_before(row, 'path_text','compound_label'),
    axis=1
)

diff_df = get_filepaths_after_compounds(df)

# creating the df of all of the information

final_df = df["compound_label"].unique()
final_df = pd.DataFrame(final_df, columns=['compound'])



    
# identifying if there are test folders within compound folders
def map_test_folders(diff_df, final_df):
  test_folder_map = {}
  
  for _, row in diff_df.iterrows():
      path_parts = row["compound_path"]
      compound = row["identified_compound"]
      
      # Check if there is a folder inside the compound folder
      if len(path_parts) > 2:
          folder_name = path_parts[1]
          
          if compound not in test_folder_map:
              test_folder_map[compound] = set()
          test_folder_map[compound].add(folder_name)
  
  final_df['test_folders'] = final_df['compound'].map(
      lambda x: list(test_folder_map.get(x, []))
  )
  return final_df

final_df = map_test_folders(diff_df, final_df)


def map_test_folders_for_compound(diff_df, final_df, target_compound):
    mask = diff_df["parts"].apply(lambda paths: target_compound in paths)
    relevant_rows = diff_df[mask]
    # 2. Extract the folder name (index 1) from path_parts if they are long enough
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


def list_useful_files(diff_df):
    # Dictionary to store files: {compound: {test_folder: [files]}}
    useful_file_map = {}

    useful_extensions = ('.mol', '.jdf' ,'.mnova', 'fid', 'pdata', '1r', 'acqu', '2rr', '2ri', '2ir', '2ii', 'ser', 'acqu2s')

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

# maybe can delete this line
final_df['compound'] = final_df['compound'].str.replace(' + ', '+', regex=False)         

   
# trying to make it output the file paths

unique_entries_set = set(itertools.chain.from_iterable(final_df['test_folders']))


# take this and filter through this --> then convert the df output back to just file paths
# will have to add more filepaths that don't just start at the compound name

FILE_PATH_KEYWORDS = ['1r', 'fid', '1i', '.mol', 'jdf', '.mnova', 'acqus'] + list(unique_entries_set)

print(FILE_PATH_KEYWORDS)

filtered_df_regex = diff_df[diff_df['compound_path'].apply(lambda x: any(k in x for k in FILE_PATH_KEYWORDS))]
#filtered_df_regex = diff_df[diff_df['compound_path'].apply(lambda x: x[-1] in FILE_PATH_KEYWORDS if x else False)]


#filtered_df_regex['my_string'] = filtered_df_regex['compound_path'].str.join('/')

filtered_df_regex = diff_df.loc[
    diff_df["compound_path"].str[-1].eq("pdata")
].copy()

filtered_df_regex["my_string"] = filtered_df_regex["compound_path"].str.join("/")

def clean_compound_path(path):
    if "pdata" in path:
        idx = path.index("pdata")
        if idx > 0:
            return path[:idx-1]
    return path

diff_df["compound_path_clean"] = diff_df["compound_path"].apply(clean_compound_path)



long_string = "\n".join(diff_df['compound_path_clean'].apply(lambda x: " ".join(x)))
long_string = long_string.replace(" + ", "+")


tokens = re.split(r'[ \n\.\/]+', long_string)
tokens = [t for t in tokens if t]

counts_series = pd.Series(tokens).value_counts()

# maybe can delete these two lines?                        
for val in final_df['compound']:
  val = val.replace(" + ", "+")


final_df['compound'] = final_df['compound'].str.replace(' + ', '+', regex=False)               

# now to get the just compound names, compare the tokens with the identified folders
# if a compound folder name contains multiple tokens, select the one where it is less frequent
# OR filter out the most common token in the folder name 


for value, count in counts_series.items():
  print(f"value: {value}, count: {count}")
  

tokens_in_compound_folder_name = []
for compound_folder in final_df['compound']:
  max_count = 0
  for value, count in counts_series.items():
    if value in compound_folder:
      if count > max_count:
        max_token = value
        max_count = count
  tokens_in_compound_folder_name.append(max_token)
  
tokens_in_compound_folder_name_set = set(tokens_in_compound_folder_name)


for token_found in tokens_in_compound_folder_name_set:
  final_df['compound_name'] = final_df['compound']
  # basically i want  a way to find if the token_found is occuring a lot, and if it is, then probably get rid of it
  word_freq = tokens_in_compound_folder_name.count(token_found)
  #print(word_freq)
  if word_freq > len(tokens_in_compound_folder_name)/1.5:
    final_df['compound_name'] = final_df['compound'].str.replace(token_found, "")
    final_df['compound_name'] = final_df['compound_name'].str.replace("of ", "")

# trying to write the pdata file path (there is a warning i'd like to get rid of with these two lines of code)

filtered_df_regex = diff_df[diff_df['compound_path'].apply(lambda x: x[-1] == "pdata")]
filtered_df_regex['my_string'] = filtered_df_regex['compound_path'].str.join('/')

print(filtered_df_regex['my_string'])

#filtered_df_regex = diff_df.loc[diff_df['compound_path'].str.endswith("pdata", na=False)].copy()
#filtered_df_regex['my_string'] = filtered_df_regex['compound_path'].str.join('/')

temp_df = filtered_df_regex.copy()
temp_df['compound_id'] = temp_df['my_string'].str.split('/').str[0]
path_mapping = temp_df.groupby('compound_id')['my_string'].apply(list).reset_index()

#print(final_df['pdata_path'])

path_mapping.columns = ['compound', 'pdata_path']
final_df = final_df.merge(path_mapping, on='compound', how='left')
final_df['pdata_path'] = final_df['pdata_path'].apply(lambda x: x if isinstance(x, list) else [])

print(final_df)

#compound,test_folders,useful_files,compound_name,pdata_path
rows_order = ["compound_name", "compound", "test_folders", "pdata_path", "useful_files"]
final_df = final_df[rows_order]
#print(final_df["pdata_path"])

final_df.to_csv(f'output_{dataset_DOI}.csv', index=False)

# second run through
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


def add_parent_folder_column(final_df, df, ignore_parents=None):
    if ignore_parents is None:
        ignore_parents = {
            ".", "..", "", ".DS_Store",
            "pdata", "fid", "ser", "used_from"
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
          
    for item in new_data:
      print(item)
      
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

def rebuild_compound(final_df, diff_df, compound):
    final_df = final_df[final_df["compound"] != compound]
    return add_compound(final_df, diff_df, compound)  
    

final_df = add_parent_folder_column(final_df, df)

compounds_to_be_added = set(final_df[final_df['is_experiment'] == True]['parent_folder'])



final_df = final_df[~final_df['is_experiment']]
final_df = final_df.drop(columns=['is_experiment'])
final_df = final_df.drop(columns=['parent_folder'])

for compound in compounds_to_be_added:
  final_df = add_compound(final_df, df, compound)

final_df.to_csv(f'output_{dataset_DOI}.csv', index=False)


final_df["tech"] = "NA"
# MIGHT NEED TO CHECK THIS LINE
mask = final_df["pdata_path"].apply(lambda row: len(row) > 0 and ("pdata" in row[0] or ".jdf" in row[0]))
final_df.loc[mask, "tech"] = "NMR"


# creating the spectral ID column
df_to_tsv = final_df

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

df_exploded['matched_path'] = df_exploded.apply(
    lambda row: find_matching_path(row, paths_list), axis=1
)

# fixed spectra_ID for compounds with JDF files (now trying to do all extension files)
mask = (df_exploded["important_file"].str.contains(".")) & (df_exploded["spectra_ID"].str.contains("NA"))
df_exploded.loc[mask, "spectra_ID"] = df_exploded[mask].apply(
    lambda x: x["compound"] + ' ' + x["important_file"].replace(x["compound"], "").replace("_", ""), 
    axis=1
)

#.replace(".jdf", "") this went after the last replace

with pd.option_context('display.max_rows', None, 'display.max_columns', None):
  print(df_exploded["spectra_ID"])
  
  
all_compounds = df_exploded["compound"].unique().tolist()
new_rows = []

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
            
    # if > 50% of files in this folder are relevant
    if len(files) > 0 and (match_count / len(files)) > 0.5:
        df_exploded = df_exploded[df_exploded["compound"] != folder[2:len(folder)-1]].reset_index(drop=True)
        
        for f_path, comp in folder_matches:
            template = df_exploded[df_exploded["compound"] == comp].iloc[0].to_dict()
            template["important_file"] = f_path.name
            template["matched_path"] = str(f_path)
            template["spectra_ID"] = "HRMS" # could change later
            template["test_folder"] = "NA"
            
            new_rows.append(template)

if new_rows:
    df_exploded = pd.concat([df_exploded, pd.DataFrame(new_rows)], ignore_index=True)
    
df_exploded['full_matched_path'] = df_exploded.apply(
    lambda row: find_matching_path(row, paths_list_final), axis=1
)

df_exploded = df_exploded.drop('matched_path', axis=1)

df_exploded.to_csv(f'final_output_{dataset_DOI}.tsv', index=False, sep='\t')


