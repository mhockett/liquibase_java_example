export dbUrl="jdbc:postgresql://localhost:5432/xxxxxx"
export dbUser="someuser"
export dbPwd="somepassword"
export dbDriver="org.postgresql.Driver"
export changelogSchema=schemaName
export pgDefaultSchema="defaultSchema"

javac -cp 'lib/*' src/TestLiquibaseScope.java
javac -cp 'lib/*' src/TestLiquibaseScope2.java
javac -cp 'lib/*' src/TestLiquibaseUpdate.java
cd src
java -cp '../lib/*:.' -Dliquibase.logLevel=FINE  -Dlogback.configurationFile='../logback.xml'  TestLiquibaseUpdate  ../testChangeset ${changelogSchema} &>updatelog.log
java -cp '../lib/*:.' -Dliquibase.logLevel=FINE -DsearchPath='../testChangeset'  -Dlogback.configurationFile='../logback.xml'  TestLiquibaseScope  ../testChangeset ${changelogSchema} &>scopelog.log
java -cp '../lib/*:.' -Dliquibase.logLevel=FINE -DsearchPath='../testChangeset'  -Dlogback.configurationFile='../logback.xml'  TestLiquibaseScope2  ../testChangeset ${changelogSchema} &>scopelog2.log
