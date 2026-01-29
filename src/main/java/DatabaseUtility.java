/*
  Database utility class for connection and Liquibase database operations
 */

import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseUtility {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseUtility.class);

    /**
     * Creates and tests a JDBC database connection
     */
    public static Connection createConnection(Properties props, String url) {
        Connection myConnection;
        try {
            myConnection = DriverManager.getConnection(url, props);
            myConnection.setAutoCommit(true);
        } catch (Exception ae) {
            throw new RuntimeException("Database connection has exception: " + ae.getMessage(), ae);
        }

        logger.debug("Database connection is set up.");

        // Test the connection
        testConnection(myConnection);
        logger.debug("Step 1 set up connection is complete");
        return myConnection;
    }

    /**
     * Tests a JDBC connection by executing a simple query
     */
    public static void testConnection(Connection connection) {
        String checkConn = "select to_char(sysdate,'YYYY-MON-DD HH24:MI:SS') from dual";
        if (System.getenv("dbDriver").contains("postgres")) {
            checkConn = "select now()";
        }

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(checkConn)) {
            String dateNow = null;
            while (rs.next()) {
                if (rs.getString(1) != null) {
                    dateNow = rs.getString(1);
                }
            }
            logger.debug("Test connection good " + dateNow);
        } catch (SQLException sq) {
            throw new RuntimeException("Error in trying to test connection: " + sq.getMessage(), sq);
        }
    }

    /**
     * Creates a Liquibase Database object from a JDBC connection
     */
    public static Database createLiquibaseDatabase(Connection connection, String defaultSchema, String upgradeSchema) {
        Database lbDatabase;
        try {
            lbDatabase = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            lbDatabase.setDefaultSchemaName(defaultSchema);
            lbDatabase.setDefaultCatalogName(defaultSchema);
            lbDatabase.setLiquibaseCatalogName(upgradeSchema);
            lbDatabase.setLiquibaseSchemaName(upgradeSchema);
            lbDatabase.setConnection(new JdbcConnection(connection));
            liquibase.database.core.DatabaseUtils.initializeDatabase(defaultSchema, defaultSchema, lbDatabase);
        } catch (DatabaseException ae) {
            throw new RuntimeException("Error in trying to create database object for changelog: " + ae.getMessage(), ae);
        }
        return lbDatabase;
    }

    /**
     * Tests a Liquibase database connection
     */
    public static void testLiquibaseConnection(Database lbDatabase) throws DatabaseException {
        JdbcConnection myLiqConnection = (JdbcConnection) lbDatabase.getConnection();
        String checkConn = "select to_char(sysdate,'YYYY-MON-DD HH24:MI:SS') from dual";
        if (System.getenv("dbDriver").contains("postgres")) {
            checkConn = "select now()";
        }

        try {
            PreparedStatement stmt = myLiqConnection.prepareStatement(checkConn);
            ResultSet rs = stmt.executeQuery();
            String dateNow = null;
            while (rs.next()) {
                if (rs.getString(1) != null) {
                    dateNow = rs.getString(1);
                }
            }
            logger.debug("Test Liq database connection good " + dateNow);
        } catch (SQLException sq) {
            throw new RuntimeException("Error in trying to test Liq connection: " + sq.getMessage(), sq);
        }
        logger.debug("Step 1a test Liq connection is complete");
    }

    /**
     * Queries the database to verify changelog execution results
     */
    public static boolean verifyResults(Connection connection, String upgradeSchema, String enameParam, String jobParam) {
        boolean foundResult = false;
        String changesetId = null;
        String job = null;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("select id from " + upgradeSchema +
                     ".databasechangelog where id = 'changeset2.sql' and author = 'scott'")) {

            while (rs.next()) {
                if (rs.getString(1) != null) {
                    changesetId = rs.getString(1);
                    logger.debug("Found the changeset in the databasechangelog " + changesetId);
                }
            }
        } catch (SQLException sq) {
            throw new RuntimeException("Error querying databasechangelog: " + sq.getMessage(), sq);
        }

        try (PreparedStatement tableQuery = connection.prepareStatement("select job from scott.bonus where ename = ?")) {
            tableQuery.setString(1, enameParam);
            try (ResultSet rs = tableQuery.executeQuery()) {
                while (rs.next()) {
                    if (rs.getString(1) != null) {
                        job = rs.getString(1);
                        logger.debug("Found the job in bonus " + job);
                    }
                }
            }
        } catch (SQLException sq) {
            throw new RuntimeException("Error querying bonus table: " + sq.getMessage(), sq);
        }

        if ((job != null && job.equals(jobParam)) &&
                (changesetId != null && changesetId.equals("changeset2.sql"))) {
            foundResult = true;
        }

        System.out.println("Found results? " + foundResult + " Job=" + job + " ChangesetId=" + changesetId);
        return foundResult;
    }

    /**
     * Cleans up test data to make the demo rerunnable
     */
    public static void cleanupTestData(Connection connection, String upgradeSchema) {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("delete from scott.bonus");
            logger.debug("Removed the bonus record we create");
        } catch (SQLException sq) {
            throw new RuntimeException("Error in trying delete the bonus record: " + sq.getMessage(), sq);
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("delete from " + upgradeSchema + ".databasechangelog where author = 'scott'");
            logger.debug("Remove the databasechangelog record we created");
        } catch (SQLException sq) {
            logger.debug("Error in trying delete the changelog records: " + sq.getMessage());
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("commit");
            logger.debug("Commit the deletes");
        } catch (SQLException sq) {
            throw new RuntimeException("Error in trying commit of the bonus record: " + sq.getMessage(), sq);
        }
    }

    /**
     * Safely closes a Liquibase database connection
     */
    public static void closeLiquibaseDatabase(Database database) {
        if (database != null) {
            try {
                database.close();
            } catch (Exception e) {
                logger.error("Database connection closure error: " + e.getMessage(), e);
            }
        }
    }
}

