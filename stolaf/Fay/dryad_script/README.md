# Dryad Script

1. Sign up for a Dryad API account by following this [Dryad API Accounts Instruction](https://github.com/datadryad/dryad-app/blob/main/documentation/apis/api_accounts.md)

2. Import the necessary Python modules

```
pip install requests dotenv
```

3. Create a `.env` file in the same directory with the Python script following this:

```
CLIENT_ID={YOUR_DRYAD_CLIENT_ID}
CLIENT_SECRET={YOUR_DRYAD_CLIENT_SECRET}
PARENT_DIRECTORY={ABSOLUTE_PATH_TO_FOLDER_THAT_STORES_DOWLOADED_FILE_INCLUDING_SLASH}
```
Parent Directory can be: `/Users/XXXXXXXX/Documents/IUPAC-FAIRSpec/c:/temp/dryad/`

5. To get a or multiple dataset(s) with specific `doi:10561/dryad.XXXXXX`, run the script:

`python3 Dryad.py XXXXX1 XXXXXX2 `

6. The script will fetch the data and create a `dataset.zip` file at the local directory