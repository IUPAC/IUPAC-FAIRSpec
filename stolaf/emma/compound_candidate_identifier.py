import pandas as pd
from pathlib import Path
from collections import Counter
import re
import itertools
import sys
import numpy as np

# command line arguments
dataset_DOI = sys.argv[1]
#print(f"dataset DOI: {dataset_DOI}")

# Read file paths
filename = f"file_list_{dataset_DOI}.txt"
#print(filename)

with open(filename, "r") as f:
    paths = [line.strip() for line in f if line.strip()]

path_parts = [list(Path(p).parts) for p in paths]

df = pd.DataFrame({"parts": path_parts})
df["path_text"] = df["parts"].apply(lambda x: " / ".join(x))

TOTAL_PATHS = len(df)

# ==================== TEST ====================
#print("~~~~~~~~~~~~~~~~READ FILE PATHS~~~~~~~~~~~~~~~~")
#print("===========df====================")
#print(df)

#print("=============df path text========")
#print(df["path_text"])
# ==============================================


# Domain knowledge

IGNORE_FOLDERS = {
    "pdata", "fid", "ser", "used_from", "StartingMaterial", ".DS_Store"
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

# I am temporarily removing the 1h keyword, still working ok for some reason
# might be working okay because I added the second pass
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
df["initial_candidates2"] = df["initial_candidates"]

# ==================== TEST ====================
#print("~~~~~~~~~~~~~~~~INITIAL CANDIDATE EXTRACTION~~~~~~~~~~~~~~~~")
#print("===========df initial candidates====================")
#print(df["initial_candidates"])

#print("=============df parts========")
#print(df["parts"])
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

#print("\nDetected GLOBAL folders (excluded):")
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
#print("~~~~~~~~~~~~~~~~DETECT AND EXCLUDE GLOBAL FOLDERS~~~~~~~~~~~~~~~~")

#print("=============candidate frequency========")
#print(candidate_freq)

#print("=============GLOBAL FOLDERS========")
#print(GLOBAL_FOLDERS)

#print("=============df compound candidates========")
#print(df["compound_candidates"])
# ==============================================



# Structural data with pdata folder
def get_relative_positions(parts):
    if "pdata" not in parts:
        return {}
    
    idx = parts.index("pdata")
    # Creates a dictionary: {folder_name: relative_offset}
    return {parts[i]: i - idx for i in range(len(parts))}

df["structural_candidate"] = df["parts"].apply(get_relative_positions)


def normalize_name(s):
    """Normalize for loose matching (1i, 1i_, 1i-1 → 1i)"""
    return re.sub(r'[^a-z0-9]', '', s.lower())


def parent_of_matching_jdf(parts, candidates):
    
    #Return candidate folder that contains a .jdf file whose name
    #loosely matches the folder name.
    
    for i in range(len(parts) - 1):
        parent = parts[i]
        child = parts[i + 1]

        if (
            #parent in candidates
            child.lower().endswith(".jdf")
            #and not is_file_like(parent)
        ):
            #parent_norm = normalize_name(parent)
            #jdf_base = normalize_name(child.replace(".jdf", ""))

            # allow 1i / 1i_.jdf, 1i-1.jdf, etc.
            #if any(parent_norm in item for item in jdf_base):
          return parent

    return None


# Weak labeling (safe version)

def clean_parent_of_experiment(parts, candidates):
    for i in range(len(parts) - 1):
        parent = parts[i]
        child = parts[i + 1]
        #if range(len(parts)-i > 2):
        #  grandchild = parts[i + 2]
        #  if special_case_folder(child, grandchild):
        #   return None

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
    #print("LOOOOOKKKKKKKKK")
    
    
    parent = clean_parent_of_experiment(parts, candidates)
    jdf_parent = parent_of_matching_jdf(parts, candidates)
    
    
    #print(candidates)
    #print(parts)
    
    #print("JDF PARENT")
    #print(jdf_parent)
    if parent and not is_file_like(parent):
        return parent

    #jdf_parent = parent_of_matching_jdf(parts, candidates)
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



df["compound_label"] = df.apply(choose_weak_label, axis=1, order=1)

print(df["compound_label"].unique())



labeled_df = df.dropna(subset=["compound_label"]).copy()

#print(f"\nWeakly labeled rows: {len(labeled_df)} / {TOTAL_PATHS}")
#print("Top labels:")
#print(labeled_df["compound_label"].value_counts().head())


# ==================== TEST ====================
#print("~~~~~~~~~~~~~~~~Weak Labeling~~~~~~~~~~~~~~~~")

#print("=============df compound label========")
#print(df["compound_label"])

#print("=============labeled df========")
#print(labeled_df)

# ==============================================




n_classes = labeled_df["compound_label"].nunique()


# ==================== TEST ====================
#print("~~~~~~~~~~~~~~~~TRAIN ML MODEL~~~~~~~~~~~~~~~~")

#print("=============n classes========")
#print(n_classes)

#print("=============n class names========")
#print(labeled_df["compound_label"].unique())

# ==============================================


# Extra thing I'm writing

class_name_counts = labeled_df["compound_label"].value_counts()
class_name_average = class_name_counts.mean()

occurrence_per_row = df.groupby('compound_label')['compound_label'].transform('count')
print("\nOccurrence count per row:")
print(occurrence_per_row)

# Filter the original DataFrame for rows where the count is greater than or equal to the average
print("BEFORE")
print(df["compound_label"].unique())

df = df[occurrence_per_row >= class_name_average- (class_name_average/.75)]
print("AFTER")
print(df["compound_label"].unique())

print("\nFiltered DataFrame (categories with >= average occurrences):")


print(labeled_df["compound_label"].unique())

#print(labeled_df["compound_label"])



# this logic is not sound proof at all

to_remove = [
    '1H', '13C Jmod', 'HSQC', 'COSY', '13C'
]

# Keep rows where the value is NOT in the list
df_filtered = labeled_df[~labeled_df["compound_label"].isin(to_remove)]


#print(df_filtered)
#print(df_filtered["compound_label"])
#print(df_filtered["compound_label"].unique())


#with pd.option_context('display.max_rows', 20, 'display.max_columns', None):
#    print(df_filtered)


# PART TWO STARTS HERE

#print(df["path_text"])

def count_slashes_before(row, path_col, compound_col):
  path = str(row[path_col]).replace('\\', '/')
  compound = str(row[compound_col])
  #print(compound)
  idx = path.lower().find(compound.lower())
  if idx == -1:
    return None
    
  return path[:idx].count('/')


df['slashes_before'] = df.apply(
    lambda row: count_slashes_before(row, 'path_text','compound_label'),
    axis=1
)

#print(df[['path_text', 'compound_label', 'slashes_before']])
    
  
#print(df['slashes_before'].unique())

matches_list = df['path_text'].str.findall(r".*/.*/.*/ 5r")
#print("\nList of matches for each row:")
#print(matches_list)

#print(df['path_text'])

    
pattern = r"([^/]+)\.zip$"

    
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
#print(zipped_filenames)

# trying next step

def get_filepaths_to_compounds(df, path_parts):
    extracted_data = []
    print(df["compound_label"].unique())
    valid_labels = set(df["compound_label"].unique())
    print("valid labels")
    print(valid_labels)
    unique_slashes = df["slashes_before"].unique()
    print(unique_slashes)
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
  
print("BIG TES")
print(df["compound_label"].unique())
new_df = get_filepaths_to_compounds(df, path_parts)
print("new_df")
print(new_df["identified_compound"].unique())








# trying to create the regex

def create_path_pattern(compound_path_list, zipped_list):
    transformed_parts = []
    
    for part in compound_path_list:
        #print(part)
        #print(zipped_list)
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
    #print(base_path)
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
#print(result)

#print(set(result))

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
#print(diff_df)
IMPORTANT_FILES = [
  ".mol", ".jdf"
]
for file_type in diff_df["compound_path"]:
  ##print(file_type[len(file_type)-1])
  if any(keyword in file_type[len(file_type)-1] for keyword in IMPORTANT_FILES):
    file_content.append("\n")
    file_content.append(file_type[0])
    file_content.append("\n")
    file_content.append(file_type[len(file_type)-1])
    file_content.append("\n")
    
    
# creating the df of all of the information

final_df = df["compound_label"].unique()
final_df = pd.DataFrame(final_df, columns=['compound'])
#print(final_df)

#print("identifying")


    
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
  
  # Map the discovered folders back to final_df as a list
  final_df['test_folders'] = final_df['compound'].map(
      lambda x: list(test_folder_map.get(x, []))
  )
  return final_df

final_df = map_test_folders(diff_df, final_df)


def map_test_folders_for_compound(diff_df, final_df, target_compound):
    mask = diff_df["parts"].apply(lambda paths: target_compound in paths)
    relevant_rows = diff_df[mask]
    print("RELEVANT ROWS")
    print(relevant_rows)


    # 2. Extract the folder name (index 1) from path_parts if they are long enough
    # We use a set comprehension for automatic deduplication
    idx = 0
    for row in relevant_rows["parts"]:
      idx=0
      for i in row:
        print(row)
        print(row[idx])
        if i != target_compound:
          idx += 1
        else:
          final_idx = idx
          break

      break
    print(final_idx)
    #relevant_rows['parts'] = relevant_rows['parts'][final_idx]
    print("NEW RELEVANT ROWS")
    print(relevant_rows)
        
    folders = {
        row["parts"][final_idx+1] 
        for _, row in relevant_rows.iterrows() 
        if len(row["parts"]) > 2
    }
    print("FOLDERSSSS")
    print(folders)
    #final_df.loc[final_df['compound'] == target_compound, 'test_folders'] = [list(folders)]
    print(final_df)
    return folders


def list_useful_files(diff_df):
    # Dictionary to store files: {compound: {test_folder: [files]}}
    useful_file_map = {}

    useful_extensions = ('.mol', '.jdf' ,'.mnova', 'fid', 'pdata', '1r')

    for _, row in diff_df.iterrows():
        path_parts = row["compound_path"]
        compound = row["identified_compound"]
        #print("IN THE FUNCTION")
        #print(path_parts)
        #print(compound)
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

#print(final_df[['compound', 'useful_files']])



#print(final_df)   

final_df['compound'] = final_df['compound'].str.replace(' + ', '+', regex=False)         

final_df.to_csv(f'output_{dataset_DOI}.csv', index=False)
    
final_df.to_json(f'output_{dataset_DOI}.json', indent=4)
    


with open(f"extractor_{dataset_DOI}.txt", "w") as file:
  file.writelines(file_content)
  
  
  
  
  
# trying to make it output the file paths

unique_entries_set = set(itertools.chain.from_iterable(final_df['test_folders']))
#print(unique_entries_set)

# take this and filter through this --> then convert the df output back to just file paths
# will have to add more filepaths that don't just start at the compound name

FILE_PATH_KEYWORDS = ['1r', 'fid', '1i', '.mol', 'jdf', '.mnova', 'acqus'] + list(unique_entries_set)



#filtered_df_regex = diff_df[diff_df['compound_path'].str.contains(regex_pattern, na = False)]
filtered_df_regex = diff_df[diff_df['compound_path'].apply(lambda x: any(k in x for k in FILE_PATH_KEYWORDS))]
filtered_df_regex = diff_df[diff_df['compound_path'].apply(lambda x: x[-1] in FILE_PATH_KEYWORDS if x else False)]

#print(filtered_df_regex)


filtered_df_regex['my_string'] = filtered_df_regex['compound_path'].str.join('/')

#print(filtered_df_regex['my_string'] )

filtered_df_regex['my_string'].to_csv('output_string.csv', index=False)


#print(path_parts)


# trying to do something that is wanted to do

# tokens are / . ' ' 


folders_to_remove = set()
for path in diff_df['compound_path']:
    if 'pdata' in path:
        idx = path.index('pdata')
        if idx > 0:
            folders_to_remove.add(path[idx - 1])
            
def global_clean(path_list):
    #if 'pdata' in path_list:
    #    idx = path_list.index('pdata')
    #    path_list = path_list[:max(0, idx - 1)]
    
    for folder in folders_to_remove:
      if folder in path_list:
        return path_list[:path_list.index(folder)]
    return path_list
    #return [folder for folder in path_list if folder not in folders_to_remove]

diff_df['compound_path_clean'] = diff_df['compound_path'].apply(global_clean)



#print(diff_df['compound_path_clean'])
long_string = "\n".join(diff_df['compound_path_clean'].apply(lambda x: " ".join(x)))
long_string = long_string.replace(" + ", "+")
#print(long_string)

tokens = re.split(r'[ \n\.\/]+', long_string)
tokens = [t for t in tokens if t]
##print(tokens)
counts_series = pd.Series(tokens).value_counts()


##print(counts_series)

#with pd.option_context('display.max_rows', None, 
#                       'display.max_columns', None):
#                         print(counts_series)
                         
for val in final_df['compound']:
  val = val.replace(" + ", "+")


final_df['compound'] = final_df['compound'].str.replace(' + ', '+', regex=False)               

# now to get the just compound names, compare the tokens with the identified folders
# if a compound folder name contains multiple tokens, select the one where it is less frequent
# OR filter out the most common token in the folder name 

#print(final_df)

#print(tokens)

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
  if word_freq > len(tokens_in_compound_folder_name)/2:
    #print("THIS IS THE BAD ONE:")
    #print(token_found)
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



path_mapping.columns = ['compound', 'pdata_path']
final_df = final_df.merge(path_mapping, on='compound', how='left')
final_df['pdata_path'] = final_df['pdata_path'].apply(lambda x: x if isinstance(x, list) else [])

print(final_df)

#compound,test_folders,useful_files,compound_name,pdata_path
rows_order = ["compound_name", "compound", "test_folders", "pdata_path", "useful_files"]
final_df = final_df[rows_order]
print(final_df["pdata_path"])

final_df.to_csv(f'output_{dataset_DOI}.csv', index=False)




##print(df["compound_candidates"])




# second run through
# drops identified compounds who has test folders that are also identified compounds

all_compounds = set(final_df['compound'].dropna().unique())

final_df['is_match'] = final_df.apply(
    lambda row: any(folder in all_compounds for folder in row['test_folders']) 
    if isinstance(row['test_folders'], list) else False, 
    axis=1
)

print(final_df[['compound', 'test_folders','is_match']])

final_df = final_df[~final_df['is_match']]
final_df = final_df.drop(columns=['is_match'])

# if an identified compound has test folders = pdata, then change identified compound to the parent folder

print(diff_df['compound_path'])



def update_to_parent(row):
    if isinstance(row['test_folders'], list) and 'pdata' in row['test_folders']:
      return True #Path(row['compound_path']).parent.name

    return False

final_df['is_experiment'] = final_df.apply(update_to_parent, axis=1)


print(final_df['is_experiment'])


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


print(diff_df["identified_compound"].unique)

def add_compound(final_df, diff_df, compound):
    if compound in final_df["compound_name"].values:
        return final_df


    compound_rows = diff_df[diff_df["parts"].apply(lambda x: compound in x)]
  
    print("COMPOUND ROWS")
    print(compound_rows)
    print(diff_df)
    # should probably filter out global folders
    
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
    print(test_folders)
    pdata_paths =[]
    filtered_df_regex = diff_df[diff_df['parts'].apply(lambda x: x[-1] == "pdata")]
    #filtered_df_regex['my_string'] = filtered_df_regex['parts'].str.join('/')
    print(filtered_df_regex)
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
    print(pdata_paths)

    compound_df = pd.DataFrame({"compound_path": new_data, "identified_compound": compound})
    print(compound_df)

    useful_files = list_useful_files(compound_df)
    print(useful_files)
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
print(compounds_to_be_added)


final_df = final_df[~final_df['is_experiment']]
final_df = final_df.drop(columns=['is_experiment'])
final_df = final_df.drop(columns=['parent_folder'])

for compound in compounds_to_be_added:
  final_df = add_compound(final_df, df, compound)

final_df.to_csv(f'output_{dataset_DOI}.csv', index=False)




# converting the file output to the TSV file

print("HIIIII")
with pd.option_context('display.max_rows', None, 'display.max_columns', None):
  print(final_df)


  
