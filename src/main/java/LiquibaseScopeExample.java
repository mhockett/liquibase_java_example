/*
  Demo of running liquibase with Scope.CommandScope
 */

import liquibase.changelog.ChangeLogParameters;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.resource.ResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.Properties;
import java.text.SimpleDateFormat;

public class LiquibaseScopeExample {
    private static final Logger logger = LoggerFactory.getLogger(LiquibaseScopeExample.class);
    private static String changelog;
    private static String upgradeSchema;

    public static void main(String[] args) throws IOException {
        // get changelog name from args
        changelog = args[0];
        if (args.length > 1) {
            upgradeSchema = args[1];
        } else {
            upgradeSchema = "SCOTT";
        }
        System.out.println("Starting test for CommandScope method with Parameters " + upgradeSchema + " " + changelog);

        LiquibaseScopeExample example = new LiquibaseScopeExample();
        example.run();
    }

    public void run() throws IOException {
        // Print Liquibase version using Liquibase API
        String liquibaseVersion = liquibase.util.LiquibaseUtil.getBuildVersion();
        System.out.println("================================================");
        System.out.println("Liquibase Version: " + liquibaseVersion);
        System.out.println("================================================");

        long startDate  = new java.util.Date().getTime();
        SimpleDateFormat df = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss:SSS");
        System.out.println("Start time is " + df.format(new java.util.Date(startDate)));

        System.out.println("Start test liquibase Scope 3 test changeset with parameters");
        File changelogDir = new File(changelog);
        if (!changelogDir.exists() || !changelogDir.isDirectory() || changelogDir.isHidden()) {
            throw new RuntimeException("Repo is not a valid folder, you entered " + changelogDir);
        }
        File changelogFile = new File(changelog + File.separator + "changelog.xml");
        if (!changelogFile.exists() || !changelogFile.isFile() || changelogFile.isHidden()) {
            throw new RuntimeException("Changelog is not a valid file, you entered " + changelogFile);
        }

        // Setup logger
        OutputStream lbLogger = Files.newOutputStream(Paths.get("CommandScopeLogParam.log"));

        // Get connection details from system properties
        Properties props = new Properties();
        props.setProperty("user", System.getenv("dbUser"));
        props.setProperty("password", System.getenv("dbPwd"));
        String url = System.getenv("dbUrl");

        // Create database connection using utility
        Connection myConnection = DatabaseUtility.createConnection(props, url);

        // Get liquibase params
        Properties liqProperties = new Properties();
        try {
            liqProperties.load(Files.newInputStream(Paths.get(changelog + File.separator + "liquibase.properties")));
        } catch (IOException e) {
            throw new RuntimeException("Error reading and loading changelog properties " + e.getMessage(), e);
        }
        String enameParam = liqProperties.getProperty("ENAME");
        String jobParam = liqProperties.getProperty("JOB");

        // Make the test rerunnable
        DatabaseUtility.cleanupTestData(myConnection, upgradeSchema);
        boolean foundResults1 = DatabaseUtility.verifyResults(myConnection, upgradeSchema, enameParam, jobParam);
        boolean foundResults2 = DatabaseUtility.verifyResults(myConnection, upgradeSchema, "ME", "CIO");
        if (!foundResults1 && !foundResults2) {
            logger.debug("Records were removed for example");
        }

        // Create liquibase database object
        String defaultSchema = "SCOTT";
        if (System.getenv("dbDriver").contains("postgres")) {
            defaultSchema = System.getenv("pgDefaultSchema");
        }
        Database lbDatabase = DatabaseUtility.createLiquibaseDatabase(myConnection, defaultSchema, upgradeSchema);

        // Test the liquibase database connection
        try {
            DatabaseUtility.testLiquibaseConnection(lbDatabase);
        } catch (DatabaseException ae) {
            throw new RuntimeException("Error in trying to test Liq database object connection: " + ae.getMessage(), ae);
        }

        logger.debug("Liquibase Database is set up.");

        // Load into liquibase changelog property object
        ChangeLogParameters lqParameters = createChangelogParameters(lbDatabase, liqProperties);

        // Set up resource accessor and search path
        ClassLoader classLoader = LiquibaseScopeExample.class.getClassLoader();
        ResourceAccessor clOpener = new liquibase.resource.SearchPathResourceAccessor(
                new liquibase.resource.DirectoryResourceAccessor(Paths.get(changelogDir.toString())),
                new liquibase.resource.ClassLoaderResourceAccessor(classLoader));
        logger.debug("Set up class loader is complete");

        // Set up map for scope
        final String changesetName = changelogFile.getName();

        try {
            LiqUpdateCustom newLiquibase = new LiqUpdateCustom(changesetName, clOpener, lbDatabase);
            newLiquibase.myUpdate(changesetName, lbLogger, lqParameters);
        } catch (liquibase.exception.LiquibaseException ae) {
            logger.debug("Error executing liquibase " + ae.getMessage());
        } catch (Exception ee) {
            logger.debug("Error executing liquibase " + ee.getMessage());
        }
        logger.debug("Complete the test");

        foundResults1 = DatabaseUtility.verifyResults(myConnection, upgradeSchema, enameParam, jobParam);
        foundResults2 = DatabaseUtility.verifyResults(myConnection, upgradeSchema, "ME", "CIO");
        if (foundResults1 && foundResults2) {
            System.out.println("SUCCESS");
        } else {
            System.out.println("FAILURE");
        }

        DatabaseUtility.closeLiquibaseDatabase(lbDatabase);

        long endDate  = new java.util.Date().getTime();
        System.out.println("Start time is " + df.format(new java.util.Date(startDate)));
        String duration = getTimeDuration(startDate, endDate);
        System.out.println("Total time to run is " + duration);
    }

    private ChangeLogParameters createChangelogParameters(Database dbliquibase, Properties liqParams) {
        ChangeLogParameters lbParams = new ChangeLogParameters(dbliquibase);
        lbParams.set("ENAME", liqParams.getProperty("ENAME"));
        lbParams.set("JOB", liqParams.getProperty("JOB"));
        return lbParams;
    }

    public static String getTimeDuration(long startDate, long endDate) {


        long diff = endDate - startDate;

        long diffSeconds = diff / 1000 % 60;
        long diffMinutes = diff / (60 * 1000) % 60;
        long diffHours = diff / (60 * 60 * 1000) % 24;
        long diffDays = diff / (24 * 60 * 60 * 1000);
        Object[] testArgs = {String.valueOf(diffDays), String.valueOf(diffHours), String.valueOf(diffMinutes), String.valueOf(diffSeconds)};
        return String.format("%s days, %s hours, %s minutes, %s seconds",
                testArgs);
    }

}
