# SCHEMA VALIDATION SCRIPT

1. Install the library `check-jsonschema`

```
pip install check-jsonschema
```

2. Open the file `/IUPAC_FAIRSPEC/dev-linux/validation/` and change the `DATA_DIRECTORY` to the absolute path of the folder that stores the ouput folders of the extraction. The output folder should be categorized

Example:

```
DATA_DIRECTORY="/Users/xxxxx/Documents/IUPAC-FAIRSpec/c:/temp/"
```

The directory should follow this folder structure:

```
DATA_DIRECTORY
    |__________ dryad/
                |__________ mcvdnckbb_out/
    |__________ acs/
                |__________ acs.orglett.0c01043_out/
    |__________ icl
                |__________ 10.14469_hpc_10386/
```

3. From the `/IUPAC_FAIRSPEC` folder, run the following command in the terminal

```
bash dev-linux/validation/valid_schema.sh
```

4. If the validation is successful, it will generate a file named `<DOI>_schema_valid.txt` in the output folder. Otherwise, a file named `<DOI>_schema_valid_error.txt` will be generated.