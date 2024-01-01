/*
  Demo of running liquibase with Scope.CommandScope
 */


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
import liquibase.changelog.ChangeLogParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.io.FileInputStream;


public class LiquibaseScopeExample {
    private static final org.slf4j.Logger logger = (org.slf4j.Logger) org.slf4j.LoggerFactory.getLogger(LiquibaseScopeExample.class);
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
        System.out.println("Starting test for CommandScope method with Parameters" + upgradeSchema + " " + changelog);
        LiquibaseScopeExample obj = new LiquibaseScopeExample();
        obj.LiquibaseScopeExample();

    }

    public void LiquibaseScopeExample() throws java.io.IOException  {
        System.out.println("Start test liquibase Scope 3 test changeset with parameters");
        File changelogDir = new File(changelog);
        if (!changelogDir.exists() || !changelogDir.isDirectory() || changelogDir.isHidden()) {
            throw new RuntimeException("Repo is not a valid folder, you entered ${changelogDir.toString()}");
        }
        File changelogFile = new File(changelog + File.separator + "changelog.xml");
        if (!changelogFile.exists() || !changelogFile.isFile() || changelogFile.isHidden()) {
            throw new RuntimeException("Changelog is not a valid file, you entered ${changelogFile.toString()}");
        }
        // logger
        java.io.OutputStream lbLogger = java.nio.file.Files.newOutputStream(java.nio.file.Paths.get("CommandScopeLogParam.log"));
        // get connection details from system properties 
        Properties props = new Properties();
        props.setProperty("user", System.getenv("dbUser"));
        props.setProperty("password", System.getenv("dbPwd"));
        String url = System.getenv("dbUrl");

        //creating connection to database using JDBC
        java.sql.Connection myConnection = null;
        myConnection = getMyConnection(props, url);

        // get liquibase params
        String enameParam = null;
        String jobParam = null;
        Properties liqProperties = new Properties();
        try {
            liqProperties.load(new FileInputStream(changelog + java.io.File.separator + "liquibase.properties"));
        } catch (IOException e) {
            throw new RuntimeException("Error reading and loading changelog properties " + e.getMessage());
        }
        enameParam = liqProperties.getProperty("ENAME");
        jobParam = liqProperties.getProperty("JOB");

        // make the test rerunnable
        makeItRerunnable(myConnection);
        boolean foundResults1 = showResults(myConnection, enameParam, jobParam);
        boolean foundResults2 = showResults(myConnection, "ME", "CIO");
        if ( !foundResults1 && !foundResults2){
            logger.debug("Records were removed for example");
        }
        // create liquibase database object
        String defaultSchema = "SCOTT";
        if (System.getenv("dbDriver").contains("postgres")) {
            defaultSchema = System.getenv("pgDefaultSchema");
        }
        liquibase.database.Database lbDatabase = getLbDatabase(myConnection, defaultSchema);

        // test the liquibase database connection
        try {
            testLiqConnection(lbDatabase);
        } catch (liquibase.exception.DatabaseException ae) {
            throw new RuntimeException("Error in trying to test Liq database object connection: " + ae.getMessage());
        }

        logger.debug("Liquibase Database is set up.");
        // load into liquibase changelog property object
        ChangeLogParameters lqParameters = getChangelogParameters(lbDatabase, liqProperties);

        // set up resource accessor and search patch
        ClassLoader classLoader = LiquibaseScopeExample.class.getClassLoader();
        liquibase.resource.ResourceAccessor clOpener = new liquibase.resource.SearchPathResourceAccessor(
                new liquibase.resource.DirectoryResourceAccessor(java.nio.file.Paths.get(changelogDir.toString())),
                new liquibase.resource.ClassLoaderResourceAccessor(classLoader));
        logger.debug("Set up class loader is complete");
        // set up map for scope
        final String changesetName = changelogFile.getName();

        try {
            LiqUpdateCustom newLiquibase = new LiqUpdateCustom(changesetName, clOpener, lbDatabase );
            newLiquibase.myUpdate(changesetName, lbLogger,   lqParameters);
        } catch (liquibase.exception.LiquibaseException ae) {
            logger.debug("Error executing liquibase " + ae.getMessage());
        } catch (Exception ee) {
            logger.debug("Error executing liquibase " + ee.getMessage());
        }
        logger.debug("Complete the test");

        foundResults1 = showResults(myConnection, enameParam, jobParam );
        foundResults2 = showResults(myConnection, "ME", "CIO");
        if (foundResults1 && foundResults2) {
            System.out.println("SUCCESS");
        } else {
            System.out.println("FAILURE");
        }

        if (lbDatabase != null) {
            try {
                lbDatabase.close();
            } catch (Exception e) {
                logger.error("banner connection closure error " + e.getMessage());
            }
        }
    }

    private static liquibase.database.Database getLbDatabase(java.sql.Connection myConnection, String defaultSchema) {

        liquibase.database.Database lbDatabase = null;
        try {

            lbDatabase = liquibase.database.DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new liquibase.database.jvm.JdbcConnection(myConnection));
            lbDatabase.setDefaultSchemaName(defaultSchema);
            lbDatabase.setDefaultCatalogName(defaultSchema);
            lbDatabase.setLiquibaseCatalogName(upgradeSchema);
            lbDatabase.setLiquibaseSchemaName((String) upgradeSchema);
            lbDatabase.setConnection(new liquibase.database.jvm.JdbcConnection(myConnection));
            liquibase.database.core.DatabaseUtils.initializeDatabase(defaultSchema, defaultSchema , lbDatabase);
        } catch (liquibase.exception.DatabaseException ae) {
            throw new RuntimeException("Error in trying to create database object for changelog: "  + ae.getMessage());
        }
        return lbDatabase;
    }
    private ChangeLogParameters getChangelogParameters(Database dbliquibase, Properties liqParams) {
        ChangeLogParameters lbParams = new ChangeLogParameters(dbliquibase);
        lbParams.set("ENAME", liqParams.getProperty("ENAME"));
        lbParams.set("JOB", liqParams.getProperty("JOB"));
        return lbParams;
    }

    private static java.sql.Connection getMyConnection(java.util.Properties props, String url) {
        java.sql.Connection myConnection = null;
        try {
            myConnection = java.sql.DriverManager.getConnection(url, props);
            myConnection.setAutoCommit(true);
        } catch (Exception ae) {
            logger.debug("Database connection has exception . " + ae.getMessage());
        }

        logger.debug("Database connection is set up. ");
        // test the connection
        String checkConn = "select to_char(sysdate,'YYYY-MON-DD HH24:MI:SS') from dual";
        if (System.getenv("dbDriver").contains("postgres")) {
            checkConn = "select now()";
        }
        java.sql.Statement stmt = null;
        java.sql.ResultSet rs = null;
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
        } catch (java.sql.SQLException sq) {
            throw new RuntimeException("Error in trying to test connection: " + sq.getMessage());
        }
        logger.debug("Step 1 set up connection is complete");
        return myConnection;
    }

    private static void testLiqConnection(liquibase.database.Database lbDatabase) throws liquibase.exception.DatabaseException {
        // test the connection
        liquibase.database.jvm.JdbcConnection myLiqConnection = (liquibase.database.jvm.JdbcConnection) lbDatabase.getConnection();
        String checkConn = "select to_char(sysdate,'YYYY-MON-DD HH24:MI:SS') from dual";
        if (System.getenv("dbDriver").contains("postgres")) {
            checkConn = "select now()";
        }
        java.sql.PreparedStatement stmt = myLiqConnection.prepareStatement(checkConn);
        java.sql.ResultSet rs = null;
        String dateNow = null;
        try {
            rs = stmt.executeQuery();
            while (rs.next()) {
                if (rs.getString(1) != null) {
                    dateNow = rs.getString(1);
                }
            }
            logger.debug("Test Liq database connection connection good " + dateNow);
        } catch (java.sql.SQLException sq) {
            throw new RuntimeException("Error in trying to test Liq connection connection: " + sq.getMessage());
        }
        logger.debug("Step 1a test Liq connection is complete");
    }

    private boolean showResults(java.sql.Connection connection, String enameParam, String jobParam) {
        boolean foundResult = false;
        java.sql.Statement stmt = null;
        java.sql.ResultSet rs = null;
        String changesetId = null;
        String job = null;
        try {
            stmt = connection.createStatement();
            rs = stmt.executeQuery("select id from " + upgradeSchema + ".databasechangelog where id = 'changeset2.sql' and author = 'scott'");
            while (rs.next()) {
                if (rs.getString(1) != null) {
                    changesetId = rs.getString(1);
                    logger.debug("Found the changeset in the databasechangelog " + changesetId);
                }
            }

            PreparedStatement tableQuery = null;
            rs = null;
            tableQuery  = connection.prepareStatement("select job from scott.bonus where ename = ?");
            tableQuery.setString(1,enameParam);
            rs = tableQuery.executeQuery();
            while (rs.next()) {
                if (rs.getString(1) != null) {
                    job = rs.getString(1);
                    logger.debug("Found the job in bonus " + job);
                }
            }
            if ((job != null && !job.isEmpty() && job.equals(jobParam)) &&
                    (changesetId != null && !changesetId.isEmpty() && changesetId.equals("changeset2.sql"))) {
                foundResult = true;
            }
        } catch (java.sql.SQLException sq) {
            throw new RuntimeException("Error in trying to test if results are in the database: " + sq.getMessage());
        }
        System.out.println("Found results? " + foundResult + " Job=" + job + " ChangesetId=" + changesetId);
        return foundResult;
    }

    private void makeItRerunnable(java.sql.Connection connection) {

        java.sql.Statement stmt = null;
        java.sql.ResultSet rs = null;
        String dateNow = null;
        try {
            stmt = connection.createStatement();
            stmt.executeUpdate("delete from scott.bonus");
            logger.debug("Removed the bonus record we create");
        } catch (java.sql.SQLException sq) {
            throw new RuntimeException("Error in trying delete the bonus record: " + sq.getMessage());
        }
        try {
            stmt = connection.createStatement();
            stmt.executeUpdate("delete from " + upgradeSchema + ".databasechangelog where   author = 'scott'");
            logger.debug("Remove the databasechangelog record we created");
        } catch (java.sql.SQLException sq) {
            throw new RuntimeException("Error in trying delete the bonus record: " + sq.getMessage());
        }
        try {
            stmt = connection.createStatement();
            stmt.executeUpdate("commit");
            logger.debug("Commit the deletes");
        } catch (java.sql.SQLException sq) {
            throw new RuntimeException("Error in trying commit of the bonus record: " + sq.getMessage());
        }

    }


}

class LiqUpdateCustom   {

    private liquibase.UpdateSummaryEnum showSummary ;
    private liquibase.UpdateSummaryOutputEnum showSummaryOutput = liquibase.UpdateSummaryOutputEnum.LOG;
    protected liquibase.database.Database database;
    private liquibase.changelog.DatabaseChangeLog databaseChangeLog;
    private String changeLogFile;
    private final liquibase.resource.ResourceAccessor resourceAccessor;
    public LiqUpdateCustom(String changeLogFileIn, liquibase.resource.ResourceAccessor resourceAccessor,
                           liquibase.database.Database database) throws liquibase.exception.LiquibaseException {
        this.changeLogFile = changeLogFileIn;
        this.resourceAccessor = resourceAccessor;
        this.database = database;
    }

    private void myScope(liquibase.Scope.ScopedRunner scopedRunner) throws liquibase.exception.LiquibaseException {
        java.util.Map<String, Object> scopeObjects = new java.util.HashMap<>();
        scopeObjects.put(liquibase.Scope.Attr.database.name(), database );
        scopeObjects.put(liquibase.Scope.Attr.resourceAccessor.name(), resourceAccessor );

        try {
            liquibase.Scope.child(scopeObjects, scopedRunner);
        } catch (Exception e) {
            if (e instanceof liquibase.exception.LiquibaseException) {
                throw (liquibase.exception.LiquibaseException) e;
            } else {
                throw new liquibase.exception.LiquibaseException(e);
            }
        }
    }


    public void myUpdate(String changeLogFile,  java.io.OutputStream lbLogger , ChangeLogParameters liqParams ) throws liquibase.exception.LiquibaseException {

        String labelExpression = null;
        String contexts = null;
        myScope(() -> {
            liquibase.command.CommandScope updateCommand = new liquibase.command.CommandScope(liquibase.command.core.UpdateCommandStep.COMMAND_NAME);
            updateCommand.addArgumentValue(liquibase.command.core.helpers.DbUrlConnectionArgumentsCommandStep.DATABASE_ARG, database);
            updateCommand.addArgumentValue(liquibase.command.core.UpdateToTagCommandStep.CHANGELOG_FILE_ARG, changeLogFile);
            updateCommand.addArgumentValue(liquibase.command.core.UpdateToTagCommandStep.CONTEXTS_ARG, contexts != null ? contexts.toString() : null);
            updateCommand.addArgumentValue(liquibase.command.core.UpdateToTagCommandStep.LABEL_FILTER_ARG, labelExpression );
            updateCommand.addArgumentValue(liquibase.command.core.helpers.ShowSummaryArgument.SHOW_SUMMARY , showSummary);
            updateCommand.addArgumentValue(liquibase.command.core.helpers.ShowSummaryArgument.SHOW_SUMMARY_OUTPUT, showSummaryOutput);
            updateCommand.addArgumentValue(liquibase.command.core.helpers.DatabaseChangelogCommandStep.CHANGELOG_PARAMETERS, liqParams);
            updateCommand.setOutput(lbLogger);
            updateCommand.execute();
        });
    }
}
