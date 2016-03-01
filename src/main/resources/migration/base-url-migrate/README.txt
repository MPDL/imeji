Files in this folder contain SPARQL queries for each imeji Model respectively, 
and update the URL from old URI to a new URI structure.
Files can be run during imeji instance startup, with automatic migration utility of imeji.
For these purpose:

a) modify the files to contain correct values for the URIs (instead of old.uri.com and new.uri.com)
b) merge the files into single migration.txt file and put it into the /data/tdb directory of imeji
c) start the imeji instance

After completing the queries, imeji instance is started and the JENA database contains the new data.
If your imeji instance runs with the elastic search, make sure you reindex all your data once again.

If your imeji instance should run with URL without "/imeji" in its path, 
then the file "migration_4_imeji_string_from_url_of_data.txt" should also be appended to the migration.txt file from step b) .  

