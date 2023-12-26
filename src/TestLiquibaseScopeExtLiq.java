/*
  Demo of running liquibase with Scope.CommandScope
 */

public class TestLiquibaseScopeExtLiq {
    private static final org.slf4j.Logger logger = (org.slf4j.Logger) org.slf4j.LoggerFactory.getLogger(TestLiquibaseScopeExtLiq.class);
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
        TestLiquibaseScopeExtLiq obj = new TestLiquibaseScopeExtLiq();
        obj.TestLiquibaseScopeExtLiq();

    }

    public void TestLiquibaseScopeExtLiq() throws  java.io.IOException,  java.sql.SQLException {
        System.out.println("Start test liquibase Scope 2 test");
        java.io.File changelogDir = new java.io.File(changelog);
        if ( !changelogDir.exists() || !changelogDir.isDirectory() || changelogDir.isHidden()){
            throw new RuntimeException("Repo is not a valid folder, you entered ${changelogDir.toString()}");
        }
        java.io.File changelogFile = new java.io.File(changelog + java.io.File.separator + "changelog.xml");
        if ( !changelogFile.exists() || !changelogFile.isFile() || changelogFile.isHidden()){
            throw new RuntimeException("Changelog is not a valid file, you entered ${changelogFile.toString()}");
        }
        // logger
        java.io.OutputStream lbLogger = java.nio.file.Files.newOutputStream(java.nio.file.Paths.get( "CommandScopeLogsw.log"));
        // get connection details from system properties 
        java.util.Properties props = new java.util.Properties();
        props.setProperty("user", System.getenv("dbUser"));
        props.setProperty("password", System.getenv("dbPwd"));
        String url = System.getenv("dbUrl");
        //creating connection to Oracle database using JDBC
        java.sql.Connection myConnection = null;
        myConnection = getMyConnection(props, url);

        // make the test rerunnable
        makeItRerunnable(myConnection);
        boolean foundResults1 = showResults(myConnection);
        // create liquibase database object
        String defaultSchema = "SCOTT";
        if ( System.getenv("dbDriver").contains("postgres")){
            defaultSchema = System.getenv("pgDefaultSchema");
        }
        liquibase.database.Database lbDatabase = null;
        try {

            lbDatabase = liquibase.database.DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new liquibase.database.jvm.JdbcConnection(myConnection));
            lbDatabase.setConnection(new liquibase.database.jvm.JdbcConnection( myConnection));

        } catch (liquibase.exception.DatabaseException ae) {
            throw new RuntimeException("Error in trying to create database object for changelog: "  + ae.getMessage());
        }
        // test the liquibase database connection
        try {
            testLiqConnection(lbDatabase);
        }catch( liquibase.exception.DatabaseException ae){
            throw new RuntimeException("Error in trying to test Liq database object connection: "  + ae.getMessage());
        }

        logger.debug("Step 2 Database is set up.");
       // set up resource accessor and search patch
        ClassLoader classLoader = TestLiquibaseScopeExtLiq.class.getClassLoader();
        liquibase.resource.ResourceAccessor clOpener = new liquibase.resource.SearchPathResourceAccessor(
                new liquibase.resource.DirectoryResourceAccessor(java.nio.file.Paths.get(changelogDir.toString())),
                        new liquibase.resource.ClassLoaderResourceAccessor(classLoader) );
        logger.debug("Set up class loader is complete");
        // set up map for scope
        final String changesetName = changelogFile.getName();

        try {
            LiqUpdate newLiquibase = new LiqUpdate(changesetName, clOpener, lbDatabase.getConnection());
            newLiquibase.myUpdate(changesetName, lbLogger ,defaultSchema,  upgradeSchema );
        }
        catch(liquibase.exception.LiquibaseException ae){
            logger.debug("Error executing liquibase " + ae.getMessage());
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

    private static java.sql.Connection getMyConnection(java.util.Properties props, String url) {
        java.sql.Connection myConnection = null;
        try {
            myConnection = java.sql.DriverManager.getConnection(url, props);
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
        java.sql.Statement stmt = null;
        java.sql.ResultSet rs = null ;
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
        catch(java.sql.SQLException sq){
            throw new RuntimeException("Error in trying to test connection: " + sq.getMessage());
        }
        logger.debug("Step 1 set up connection is complete");
        return myConnection;
    }

    private static void testLiqConnection(liquibase.database.Database lbDatabase) throws liquibase.exception.DatabaseException {
        // test the connection
        liquibase.database.jvm.JdbcConnection myLiqConnection = (liquibase.database.jvm.JdbcConnection) lbDatabase.getConnection();
        String checkConn = "select to_char(sysdate,'YYYY-MON-DD HH24:MI:SS') from dual";
        if ( System.getenv("dbDriver").contains("postgres")){
            checkConn = "select now()";
        }
        java.sql.PreparedStatement stmt = myLiqConnection.prepareStatement(checkConn);
        java.sql.ResultSet rs = null ;
        String dateNow = null;
        try {
            rs = stmt.executeQuery();
            while (rs.next()) {
                if (rs.getString(1) != null) {
                    dateNow = rs.getString(1);
                }
            }
            logger.debug("Test Liq database connection connection good " + dateNow);
        }
        catch(java.sql.SQLException sq){
            throw new RuntimeException("Error in trying to test Liq connection connection: " + sq.getMessage());
        }
        logger.debug("Step 1a test Liq connection is complete");
    }

    private boolean showResults(java.sql.Connection connection){
        boolean foundResult = false;
        java.sql.Statement stmt = null;
        java.sql.ResultSet rs = null ;
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
        catch(java.sql.SQLException sq){
            throw new RuntimeException("Error in trying to test if results are in the database: " + sq.getMessage());
        }
        System.out.println("Found results? " + foundResult + " Job=" + job + " ChangesetId="+ changesetId);
        return foundResult;
    }
    private void makeItRerunnable(java.sql.Connection connection){

        java.sql.Statement stmt = null;
        java.sql.ResultSet rs = null ;
        String dateNow = null;
        try {
            stmt = connection.createStatement();
            stmt.executeUpdate("delete from scott.bonus where ename = 'ME'");
            logger.debug("Removed the bonus record we create");
        }
        catch(java.sql.SQLException sq){
            throw new RuntimeException("Error in trying delete the bonus record: " + sq.getMessage());
        }
        try {
            stmt = connection.createStatement();
            stmt.executeUpdate("delete from " + upgradeSchema + ".databasechangelog where id = 'changeset.sql' and author = 'scott'");
            logger.debug("Remove the databasechangelog record we created");
        }
        catch(java.sql.SQLException sq){
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

class LiqUpdate extends  liquibase.Liquibase {

    private liquibase.UpdateSummaryEnum showSummary;
    private liquibase.UpdateSummaryOutputEnum showSummaryOutput;
    public LiqUpdate(String changeLogFile, liquibase.resource.ResourceAccessor resourceAccessor, liquibase.database.DatabaseConnection conn) throws liquibase.exception.LiquibaseException {
        super(changeLogFile, resourceAccessor, conn);
    }

    private void myScope(liquibase.Scope.ScopedRunner scopedRunner) throws liquibase.exception.LiquibaseException {
        java.util.Map<String, Object> scopeObjects = new java.util.HashMap<>();
        scopeObjects.put(liquibase.Scope.Attr.database.name(), getDatabase());
        scopeObjects.put(liquibase.Scope.Attr.resourceAccessor.name(), getResourceAccessor());

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


    public void myUpdate(String changeLogFile,  java.io.OutputStream lbLogger, String defaultSchema, String upgradeSchema ) throws liquibase.exception.LiquibaseException {
        getDatabase().setDefaultSchemaName(defaultSchema);
        getDatabase().setLiquibaseCatalogName(upgradeSchema);
        getDatabase().setLiquibaseSchemaName((String) upgradeSchema);
        String labelExpression = null;
        String tag = null;
        String contexts = null;
        getDatabase().setAutoCommit(true);
        myScope(() -> {
            liquibase.command.CommandScope updateCommand = new liquibase.command.CommandScope(liquibase.command.core.UpdateToTagCommandStep.COMMAND_NAME);
            updateCommand.addArgumentValue(liquibase.command.core.helpers.DbUrlConnectionArgumentsCommandStep.DATABASE_ARG, getDatabase());
            updateCommand.addArgumentValue(liquibase.command.core.UpdateToTagCommandStep.CHANGELOG_FILE_ARG, changeLogFile);
            updateCommand.addArgumentValue(liquibase.command.core.UpdateToTagCommandStep.CONTEXTS_ARG, contexts != null ? contexts.toString() : null);
            updateCommand.addArgumentValue(liquibase.command.core.UpdateToTagCommandStep.LABEL_FILTER_ARG, labelExpression );
            updateCommand.addArgumentValue(liquibase.command.core.helpers.ShowSummaryArgument.SHOW_SUMMARY , showSummary);
            updateCommand.addArgumentValue(liquibase.command.core.helpers.ShowSummaryArgument.SHOW_SUMMARY_OUTPUT, showSummaryOutput);
            updateCommand.addArgumentValue(liquibase.command.core.UpdateToTagCommandStep.TAG_ARG, "Test");
            updateCommand.setOutput(lbLogger);
            updateCommand.execute();
        });
    }
}
