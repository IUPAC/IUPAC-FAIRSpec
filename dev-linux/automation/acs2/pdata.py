from pathlib import Path

# pdata.py

# uses the "pruned" version of the file_list_<doi>.txt collection
# reads pdata_all.txt (from Windows in the working directory:  find "pdata" file_list_*.txt > pdata_all.txt)

# creates a large set of output files of the form

# {doi}_compound_id_pdata.txt
# {doi}_data_object_id_pdata.txt

# an experiment in generating compound ID and spec ID for 81 ACS2 datasets  

# - very preliminary
# - not 100% successful
# - not checking for uniqueness of ids

# 2026.03.14 BH

filename = "pdata_all.txt"

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
        with open(f'{doi}_compound_id_pdata.txt', "w", encoding="utf-8") as f:
            m = f'{doi} '+';'.join(compound_ids)+'\n'
            f.write(m)
        with open(f'{doi}_data_object_id_pdata.txt', "w", encoding="utf-8") as f:
            m = f'{doi} '+';'.join(spec_ids)+'\n'
            f.write(m)
    first = last


