

import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.SearchPathResourceAccessor;
import liquibase.integration.commandline.CommandLineUtils;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.resource.ResourceAccessor;
import liquibase.resource.DirectoryResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.sql.Connection;
import java.nio.file.Paths;
import java.sql.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/*
  Demo of running liquibase with Liquibase.update

*/

public class TestLiquibaseUpdate {
    private static final Logger logger = (Logger) LoggerFactory.getLogger(TestLiquibaseUpdate.class);
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
        System.out.println("Starting test liquibase.update process " + upgradeSchema + " " + changelog);
        TestLiquibaseUpdate obj = new TestLiquibaseUpdate();
        obj.TestLiquibaseUpdate();


    }

    public void TestLiquibaseUpdate() throws IOException, SQLException {
        System.out.println("Start test liquibase");
        File changelogDir = new File(changelog);
        if (!changelogDir.exists() || !changelogDir.isDirectory() || changelogDir.isHidden()) {
            throw new RuntimeException("Repo is not a valid folder, you entered ${changelogDir.toString()}");
        }
        File changelogFile = new File(changelog + File.separator + "changelog.xml");
        if (!changelogFile.exists() || !changelogFile.isFile() || changelogFile.isHidden()) {
            throw new RuntimeException("Changelog is not a valid file, you entered ${changelogFile.toString()}");
        }
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
        catch(Exception ae) {
            logger.debug("Database connection has exception . " + ae.getMessage());
        }

        String checkConn = "select to_char(sysdate,'YYYY-MON-DD HH24:MI:SS') from dual";
        if ( System.getenv("dbDriver").contains("postgres")){
            checkConn = "select now()";
        }

        logger.debug("Database connection is set up. ");
        // test the connection
        Statement stmt = null;
        ResultSet rs = null;
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
        } catch (SQLException sq) {
            throw new RuntimeException("Error in trying to test connection: " + sq.getMessage());
        }
        logger.debug("Step 1 set up connection is complete");

        makeItRerunnable(myConnection);
        boolean foundResults1 = showResults(myConnection);

        // set up resource accessor and search patch
        ClassLoader classLoader = TestLiquibaseUpdate.class.getClassLoader();
        ResourceAccessor clOpener = new SearchPathResourceAccessor(
                new DirectoryResourceAccessor(Paths.get(changelogDir.toString())),
                new ClassLoaderResourceAccessor(classLoader));
        logger.debug("Set up class loader is complete");

        // set up liquibase object using command line utils
        Database lbDatabase = null;
        String liquibaseSchemaName = null;
        String liquibaseCatalogName = upgradeSchema;
        String defaultSchema = "SCOTT";
        if ( System.getenv("dbDriver").contains("postgres")){
            defaultSchema = System.getenv("pgDefaultSchema");
            liquibaseCatalogName = upgradeSchema;
        }
        try {
            lbDatabase = CommandLineUtils.createDatabaseObject(
                    clOpener, // resourceAccessor
                    System.getenv("dbUrl"), // url
                    System.getenv("dbUser"), // username
                    System.getenv("dbPwd"), // password
                    System.getenv("dbDriver"), // driver
                    (String) null, // defaultCatalogName
                    defaultSchema, // schema.toUpperCase(), // defaultSchemaName
                    false, // outputDefaultCatalog
                    false, // outputDefaultSchema
                    (String) null, // databaseClass
                    (String) null, // driverPropertiesFile
                    (String) null, // propertyProviderClass
                    upgradeSchema, // liquibaseCatalogName
                    (String) liquibaseSchemaName, // liquibaseSchemaName
                    (String) null, // databaseChangeLogTableName
                    (String) null  // databaseChangeLogLockTableName
            );
        } catch (DatabaseException ae) {
            logger.debug("Error in trying to create database object for " + ae.getMessage());
            logger.error("Error executing liquibase, database exception thrown: " + ae.getMessage());
        }
        Liquibase newLiquibase = null;
        newLiquibase = new Liquibase(changelogFile.getName(), clOpener, lbDatabase);
        try {
            newLiquibase.update("");
        } catch (Exception ee) {
            logger.debug("Error executing liquibase " + ee.getMessage());
        }
        logger.debug("Complete the test");

        boolean foundResults = showResults(myConnection);
        if (foundResults) {
            System.out.println("SUCCESS");
        } else {
            System.out.println("FAILURE");
        }

    }

    private boolean showResults(Connection connection) {
        boolean foundResult = false;
        Statement stmt = null;
        ResultSet rs = null;
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
            if ((job != null && !job.isEmpty() && job.equals("CIO")) &&
                    (changesetId != null && !changesetId.isEmpty() && changesetId.equals("changeset.sql"))) {
                foundResult = true;
            }
        } catch (SQLException sq) {
            throw new RuntimeException("Error in trying to test if results are in the database: " + sq.getMessage());
        }
        System.out.println("Found results? " + foundResult + " Job=" + job + " ChangesetId=" + changesetId);
        return foundResult;
    }

    private void makeItRerunnable(Connection connection) {

        Statement stmt = null;
        ResultSet rs = null;
        String dateNow = null;
        try {
            stmt = connection.createStatement();
            stmt.executeUpdate("delete from scott.bonus where ename = 'ME'");
            logger.debug("Removed the bonus record we create");
        } catch (SQLException sq) {
            throw new RuntimeException("Error in trying delete the bonus record: " + sq.getMessage());
        }
        try {
            stmt = connection.createStatement();
            stmt.executeUpdate("delete from " + upgradeSchema + ".databasechangelog where id = 'changeset.sql' and author = 'scott'");
            logger.debug("Remove the databasechangelog record we created");
        } catch (SQLException sq) {
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
