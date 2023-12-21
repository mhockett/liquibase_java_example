

import liquibase.command.CommandFailedException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.SearchPathResourceAccessor;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.resource.ResourceAccessor;
import liquibase.Scope;
import liquibase.Scope.ScopedRunner;
import liquibase.command.CommandScope;
import liquibase.command.core.UpdateCommandStep;
import liquibase.command.core.helpers.DbUrlConnectionArgumentsCommandStep;
import liquibase.resource.DirectoryResourceAccessor;
import liquibase.command.core.UpdateSqlCommandStep;
import liquibase.command.CommandResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.DriverManager;
import java.sql.Connection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.io.File;
import java.io.OutputStream;
import java.io.IOException;
import java.sql.SQLException;

/*
  Demo of running liquibase with Scope.CommandScope
 */

public class TestLiquibaseScope   {
    private static final Logger logger = (Logger) LoggerFactory.getLogger(TestLiquibaseScope.class);
    static String changelog;
    static String upgradeSchema;
    public static void main(String args[]) throws java.sql.SQLException, java.io.IOException {
        // get changelog name from args
        changelog = args[0];
        if (args.length > 1) {
            upgradeSchema = args[1];
        } else {
            upgradeSchema = "SCOTT";
        }
        System.out.println("Starting test for CommandScope method " + upgradeSchema + " " + changelog);
        TestLiquibaseScope obj = new TestLiquibaseScope();
        obj.TestLiquibaseScope();

    }

    public void TestLiquibaseScope() throws  IOException,  SQLException {
        System.out.println("Start test liquibase");
        File changelogDir = new File(changelog);
        if ( !changelogDir.exists() || !changelogDir.isDirectory() || changelogDir.isHidden()){
            throw new RuntimeException("Repo is not a valid folder, you entered ${changelogDir.toString()}");
        }
        File changelogFile = new File(changelog + File.separator + "changelog.xml");
        if ( !changelogFile.exists() || !changelogFile.isFile() || changelogFile.isHidden()){
            throw new RuntimeException("Changelog is not a valid file, you entered ${changelogFile.toString()}");
        }
        // logger
        OutputStream lbLogger = Files.newOutputStream(Paths.get( "CommandScopeLogs.log"));
        // get connection
        java.util.Properties props = new java.util.Properties();
        props.setProperty("user", System.getenv("dbUser"));
        props.setProperty("password", System.getenv("dbPwd"));
        String url = System.getenv("dbUrl");
        //creating connection to Oracle database using JDBC
        Connection myConnection = null;
        try {
            myConnection = DriverManager.getConnection(url, props);
            myConnection.setAutoCommit(true);
        }
        catch(Exception ae){
            logger.debug("Database connection has exception . " + ae.getMessage());
        }

        logger.debug("Database connection is set up. ");
        // test the connection
        String checkConn = "select to_char(sysdate,'YYYY-MON-DD HH24:MI:SS') from dual";
        if ( System.getenv("dbDriver").contains("postgres")){
            checkConn = "select now()";
        }
        Statement stmt = null;
        ResultSet rs = null ;
        String dateNow = null;
        try {
            stmt = myConnection.createStatement();
            rs = stmt.executeQuery(checkConn);
            while (rs.next()) {
                if (rs.getString(1) != null) {
                    dateNow = rs.getString(1);
                }
            }
            logger.debug("Test connection good " + dateNow);
        }
        catch(SQLException sq){
            throw new RuntimeException("Error in trying to test connection: " + sq.getMessage());
        }
        logger.debug("Step 1 set up connection is complete");

        makeItRerunnable(myConnection);
        boolean foundResults1 = showResults(myConnection);
        // create liquibase database object
        String defaultSchema = "SCOTT";
        if ( System.getenv("dbDriver").contains("postgres")){
            defaultSchema = System.getenv("pgDefaultSchema");

        }
        Database lbDatabase = null;
        try {

            lbDatabase = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(myConnection));
            lbDatabase.setDefaultSchemaName(defaultSchema);
            lbDatabase.setLiquibaseCatalogName(upgradeSchema);
            lbDatabase.setLiquibaseSchemaName((String) upgradeSchema);
            lbDatabase.setAutoCommit(true);

        } catch (DatabaseException ae) {
            throw new RuntimeException("Error in trying to create database object for changelog: "  + ae.getMessage());
        }
        logger.debug("Step 2 Database is set up.");
       // set up resource accessor and search patch
        ClassLoader classLoader = TestLiquibaseScope.class.getClassLoader();
        ResourceAccessor clOpener = new SearchPathResourceAccessor(
                new DirectoryResourceAccessor(Paths.get(changelogDir.toString())),
                        new ClassLoaderResourceAccessor(classLoader) );
        logger.debug("Set up class loader is complete");
        // set up map for scope
        final String changesetName = changelogFile.getName();
        final java.util.Map<String, Object> config = new java.util.HashMap<>();
        config.put(Scope.Attr.database.name(), lbDatabase);
        config.put(Scope.Attr.resourceAccessor.name(), clOpener);
        Scope.ScopedRunner scopedRunner;
        logger.debug("Map is set up");
        try {
            liquibase.database.Database finalLbDatabase = lbDatabase;
              Scope.child(config,  (ScopedRunner) ()  -> {
                logger.debug("Start inside the scope to process update for " + changesetName);

                CommandScope updateCommand = new CommandScope(UpdateSqlCommandStep.COMMAND_NAME);
                updateCommand.addArgumentValue(DbUrlConnectionArgumentsCommandStep.DATABASE_ARG, finalLbDatabase);
                updateCommand.addArgumentValue(UpdateCommandStep.CHANGELOG_FILE_ARG, changesetName);
                updateCommand.setOutput(lbLogger);
                logger.debug( "Before execute");
                CommandResults result = updateCommand.execute();
                if (result.getResults() != null) {
                    java.util.Set set2 = result.getResults().entrySet();
                    java.util.Iterator i2 = set2.iterator();
                    while (i2.hasNext()) {
                        java.util.Map.Entry param = (java.util.Map.Entry) i2.next();
                        logger.debug( "Results Entry set " + param.getKey() + " value " + param.getValue());
                    }
                }

             });
        }
         catch( CommandFailedException af ){
           logger.debug("Error executing command failed execution liquibase " + af.getStackTrace());
        }
        catch (Exception ee) {
            logger.debug("Error executing liquibase " + ee.getMessage());
        }
        logger.debug("Complete the test");

        boolean foundResults = showResults(myConnection);
        if ( foundResults ){
            System.out.println("SUCCESS");
        }
        else {
            System.out.println("FAILURE");
        }

    }

    private boolean showResults(Connection connection){
        boolean foundResult = false;
        Statement stmt = null;
        ResultSet rs = null ;
        String changesetId = null;
        String job = null;
        try {
            stmt = connection.createStatement();
            rs = stmt.executeQuery("select id from " + upgradeSchema + ".databasechangelog where id = 'changeset.sql' and author = 'scott'");
            while (rs.next()) {
                if (rs.getString(1) != null) {
                   changesetId = rs.getString(1);
                   logger.debug("Found the changeset in the databasechangelog " + changesetId);
                }
            }

            rs = stmt.executeQuery("select job from scott.bonus where ename = 'ME'");
            while (rs.next()) {
                if (rs.getString(1) != null) {
                    job = rs.getString(1);
                    logger.debug("Found the job in bonus " + job);
                }
            }
            if ( (job != null &&!job.isEmpty() && job.equals("CIO")) &&
                    (changesetId != null && !changesetId.isEmpty() && changesetId.equals("changeset.sql"))){
                foundResult = true;
            }
        }
        catch(SQLException sq){
            throw new RuntimeException("Error in trying to test if results are in the database: " + sq.getMessage());
        }
        System.out.println("Found results? " + foundResult + " Job=" + job + " ChangesetId="+ changesetId);
        return foundResult;
    }
    private void makeItRerunnable(Connection connection){

        Statement stmt = null;
        ResultSet rs = null ;
        String dateNow = null;
        try {
            stmt = connection.createStatement();
            stmt.executeUpdate("delete from scott.bonus where ename = 'ME'");
            logger.debug("Removed the bonus record we create");
        }
        catch(SQLException sq){
            throw new RuntimeException("Error in trying delete the bonus record: " + sq.getMessage());
        }
        try {
            stmt = connection.createStatement();
            stmt.executeUpdate("delete from " + upgradeSchema + ".databasechangelog where id = 'changeset.sql' and author = 'scott'");
            logger.debug("Remove the databasechangelog record we created");
        }
        catch(SQLException sq){
            throw new RuntimeException("Error in trying delete the bonus record: " + sq.getMessage());
        }
        try {
            stmt = connection.createStatement();
            stmt.executeUpdate("commit");
            logger.debug("Commit the deletes");
        } catch (SQLException sq) {
            throw new RuntimeException("Error in trying commit of the bonus record: " + sq.getMessage());
        }

    }


}
