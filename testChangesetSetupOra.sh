export dbUrl="jdbc:oracle:thin:@localhost:1521/xxxxx"
export dbUser="someuser"
export dbPwd="somepassword"
export dbDriver="oracle.jdbc.OracleDriver"
export changelogSchema=schemaName

javac -cp 'lib/*' src/LiquibaseScopeExample.java
cd src
java -cp '../lib/*:.' -Dliquibase.logLevel=FINE  -Dlogback.configurationFile='../logback.xml'  LiquibaseScopeExample  ../testChangeset ${changelogSchema} &>updatelog.log
