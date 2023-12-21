Readme for changeset 4.25.1 demo

Tested on JDK 8

The demo includes two demo scripts :

1. TestLiquibaseScope.java which will run liquibase for a changelog using the Liquibase 4.25.1 ScopeCommand

1. TestLiquibaseUpdate.java which will run liquibase for changelog using liquibase.update 

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

5. The two java programs are rerunnable.  Each deletes the databasechangelog row and the scott.bonus row. 
6. The two programs print results at the end.  Each queries the the databasechangelog for the row that should be created.
It also queries for the row that should be inserted into scott.bonus.  It prints out a success or failure. 

From the version that runs the update you will see :

```

08:03:21.627 [main] INFO liquibase.command.CommandScope - Command execution complete
08:03:21.628 [main] DEBUG TestLiquibaseUpdate - Complete the test
08:03:21.632 [main] DEBUG TestLiquibaseUpdate - Found the changeset in the databasechangelog changeset.sql
08:03:21.633 [main] DEBUG TestLiquibaseUpdate - Found the job in bonus CIO
Found results? true Job=CIO ChangesetId=changeset.sql
SUCCESS   
   
```
   
From the version that runs the scope, you will see :

```text

08:13:37.769 [main] INFO liquibase.command.CommandScope - Command execution complete
08:13:37.769 [main] DEBUG TestLiquibaseScope - Results Entry set defaultChangeExecListener value liquibase.changelog.visitor.DefaultChangeExecListener@1c93f6e1
08:13:37.769 [main] DEBUG TestLiquibaseScope - Results Entry set statusCode value 0
08:13:37.772 [main] DEBUG TestLiquibaseScope - Results Entry set updateReport value UpdateReportParameters(changelogArgValue=changelog.xml, jdbcUrl=jdbc:oracle:thin:@localhost:1521/BAN83, tag=null, commandTitle=Update Sql, success=true, databaseInfo=DatabaseInfo(databaseType=Oracle, version=Oracle Database 19c Enterprise Edition Release 19.0.0.0.0 - Production), runtimeInfo=RuntimeInfo(systemUsername=mhockett, hostname=C02D1A9SMD6M, os=Mac OS X, interfaceType=null, startTime=null, updateDuration=null, liquibaseVersion=4.25.1, javaVersion=1.8.0_282), operationInfo=OperationInfo(command=null, operationOutcome=success, operationOutcomeErrorMsg=null, exception=null, updateSummaryMsg=null, rowsAffected=0), customData=CustomData(customDataFile=null, fileContents=null), changesetInfo=ChangesetInfo(changesetCount=1, changesetInfoList=[IndividualChangesetInfo(index=1, changesetAuthor=scott, changesetId=changeset.sql, changelogFile=changeset.sql, comment=null, success=true, changesetOutcome=EXECUTED, errorMsg=null, labels=, contexts=null, attributes=[], generatedSql=[insert into scott.bonus ( ename, job, sal, comm)
  values (  'ME', 'CIO', 100000, 10000 );])]), date=Thu Dec 21 08:13:33 EST 2023)
08:13:37.772 [main] DEBUG TestLiquibaseScope - Complete the test
Found results? false Job=null ChangesetId=null
FAILURE


```
 
 