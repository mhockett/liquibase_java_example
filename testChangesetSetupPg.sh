export dbUrl="jdbc:postgresql://localhost:5432/xxxxxx"
export dbUser="someuser"
export dbPwd="somepassword"
export dbDriver="org.postgresql.Driver"
export changelogSchema=schemaName
export pgDefaultSchema="defaultSchema"

javac -cp 'lib/*' src/TestLiquibaseScope.java
cd src
java -cp '../lib/*:.' -Dliquibase.logLevel=FINE -DsearchPath='../testChangeset'  -Dlogback.configurationFile='../logback.xml'  LiquibaseScopeExample  ../testChangeset ${changelogSchema} &>scopelog.log
