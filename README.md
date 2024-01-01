Readme for changeset 4.25.1 demo

Tested on JDK 8

The demo includes the demo java program LiquibaseScopeExample.java :

1. LiquibaseScopeExample.java which will run liquibase for a changelog using the Liquibase 4.25.1 ScopeCommand  

1. Shell scripts to run using Oracle and another for PG. 

Demos runs on oracle and can run on PG.  
I added the shells but I usually run using my IDE Run. 

1.  Verify your oracle database has the scott.bonus demo table.  

2. You can use the testChangeset/bonus_pg.sql to create the table on your PG database.  

1. Modify the shells testChangesetSetupOra.sh, or if you have PG, modify the testChangesetSetupPg.sh 
   update the variables for your database URL, ID that runs liquibase, password and database driver. 

    ```
    export dbUrl="jdbc:oracle:thin:@localhost:1521/xxxxx"
    export dbUser="someuser"
    export dbPwd="somepassword"
    export dbDriver="oracle.jdbc.OracleDriver"
    export changelogSchema=schemaName
   
   ```    

   testChangesetSetupPg.sh has the additional variable pgDefaultSchema for the search path. 

   ``` 
   export dbUrl="jdbc:postgresql://localhost:5432/xxxxxx"
   export dbUser="someuser"
   export dbPwd="somepassword"
   export dbDriver="org.postgresql.Driver"
   export changelogSchema=schemaName
   export pgDefaultSchema="defaultSchema"

   ```
 
3. Run the shell from the root of the repo. 

  sh testChangesetSetupOra.sh 

  sh testChangesetSetupPg.sh

4. Logs are in the src folder 

   1. scopelog.log has the output from the version that uses scope
   1. updatelog.log has the output from the version that uses update
   1. CommandScopeLogs.log has the output from the scope  

5. The java demo program is rerunnable.  The script deletes the databasechangelog row and the scott.bonus rows. 
6. The demo print results at the end.  Each queries the the databasechangelog for the row that should be created.
It also queries for the row that should be inserted into scott.bonus.  It prints out a success or failure. 

 
