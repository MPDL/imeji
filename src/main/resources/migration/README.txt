Unless otherwise explicitly stated in the subdirectories, this README file is useful hint 
on how to perform migration of imeji data in the Jena triple store.


imeji implements a migration utility, based on a SPARQL update (specifications). 


IMPORTANT NOTE: The SPARQL query updates should be written in a file called “migration.txt”.

To perform migration: 

a) stop the imeji instance 
b) Put the file "migration.txt" in the jena tdb home directory (see Imeji_properties, property “imeji.tdb.path”)
c) start the imeji instance
d) after successfull start and report on successfull migration, rename the file "migration.txt" to another name (e.g. migration_1.txt) 
   so that migration is not performed again next time your imeji instance restarts (alternatively, just remove the file).
   

 

