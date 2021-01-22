
cd data

# Get the subject area mappings from [wlo-metadata-mappings](https://github.com/yovisto/wlo-metadata-mappings).
wget https://raw.githubusercontent.com/yovisto/wlo-metadata-mappings/main/subjectAreas/subjectAreasMapping.ttl

# Get the keyword mappings from [wlo-metadata-mappings](https://github.com/yovisto/wlo-metadata-mappings).
wget https://github.com/yovisto/wlo-metadata-mappings/raw/main/keywords/keywordMapping.ttl

# Get and unzip the normdata file from [wlo-metadata-mappings](https://github.com/yovisto/wlo-metadata-mappings)
wget https://github.com/yovisto/wlo-metadata-mappings/raw/main/normdata/normdata.ttl.zip
unzip normdata.ttl.zip

# Retrieve the discipline description from [oeh-metadata-vocabs](https://github.com/openeduhub/oeh-metadata-vocabs).
wget https://raw.githubusercontent.com/openeduhub/oeh-metadata-vocabs/master/discipline.ttl

# Retrieve the Schlagwortverzeichnis (keywords) from [oeh-metadata-eaf-schlagwortverzeichnis](https://github.com/openeduhub/oeh-metadata-eaf-schlagwortverzeichnis)
wget https://raw.githubusercontent.com/openeduhub/oeh-metadata-eaf-schlagwortverzeichnis/main/data/eaf-graph-by-subject-all.ttl

# Retrieve the Sachgebietssystematik from [oeh-metadata-eaf-sachgebietssystematiken](https://github.com/openeduhub/oeh-metadata-eaf-sachgebietssystematiken)
wget https://raw.githubusercontent.com/openeduhub/oeh-metadata-eaf-sachgebietssystematiken/master/eaf-sachgebietssystematik-all.ttl

# Copy all downloaded files to the Virtuoso data loading directory ```docker/virtuoso/data/toLoad```. 
cp subjectAreasMapping.ttl ../docker/virtuoso/data/toLoad
cp keywordMapping.ttl ../docker/virtuoso/data/toLoad
cp normdata.ttl ../docker/virtuoso/data/toLoad
cp discipline.ttl ../docker/virtuoso/data/toLoad
cp eaf-graph-by-subject-all.ttl ../docker/virtuoso/data/toLoad
cp eaf-sachgebietssystematik-all.ttl ../docker/virtuoso/data/toLoad

cd ..