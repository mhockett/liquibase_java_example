/*
  Custom Liquibase Update class using CommandScope
 */

import liquibase.changelog.ChangeLogParameters;
import liquibase.database.Database;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ResourceAccessor;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class LiqUpdateCustom {

    private liquibase.UpdateSummaryEnum showSummary;
    private final liquibase.UpdateSummaryOutputEnum showSummaryOutput = liquibase.UpdateSummaryOutputEnum.LOG;
    protected Database database;
    private final String changeLogFile;
    private final ResourceAccessor resourceAccessor;

    public LiqUpdateCustom(String changeLogFileIn, ResourceAccessor resourceAccessor,
                           Database database) throws LiquibaseException {
        this.changeLogFile = changeLogFileIn;
        this.resourceAccessor = resourceAccessor;
        this.database = database;
    }

    @SuppressWarnings("rawtypes")
    private void myScope(liquibase.Scope.ScopedRunner scopedRunner) throws LiquibaseException {
        Map<String, Object> scopeObjects = new HashMap<>();
        scopeObjects.put(liquibase.Scope.Attr.database.name(), database);
        scopeObjects.put(liquibase.Scope.Attr.resourceAccessor.name(), resourceAccessor);

        try {
            liquibase.Scope.child(scopeObjects, scopedRunner);
        } catch (Exception e) {
            if (e instanceof LiquibaseException) {
                throw (LiquibaseException) e;
            } else {
                throw new LiquibaseException(e);
            }
        }
    }

    public void myUpdate(String changeLogFile, OutputStream lbLogger, ChangeLogParameters liqParams) throws LiquibaseException {
        String labelExpression = null;
        String contexts = null;
        myScope(() -> {
            liquibase.command.CommandScope updateCommand = new liquibase.command.CommandScope(liquibase.command.core.UpdateCommandStep.COMMAND_NAME);
            updateCommand.addArgumentValue(liquibase.command.core.helpers.DbUrlConnectionArgumentsCommandStep.DATABASE_ARG, database);
            updateCommand.addArgumentValue(liquibase.command.core.UpdateToTagCommandStep.CHANGELOG_FILE_ARG, changeLogFile);
            updateCommand.addArgumentValue(liquibase.command.core.UpdateToTagCommandStep.CONTEXTS_ARG, contexts);
            updateCommand.addArgumentValue(liquibase.command.core.UpdateToTagCommandStep.LABEL_FILTER_ARG, labelExpression);
            updateCommand.addArgumentValue(liquibase.command.core.helpers.ShowSummaryArgument.SHOW_SUMMARY, showSummary);
            updateCommand.addArgumentValue(liquibase.command.core.helpers.ShowSummaryArgument.SHOW_SUMMARY_OUTPUT, showSummaryOutput);
            updateCommand.addArgumentValue(liquibase.command.core.helpers.DatabaseChangelogCommandStep.CHANGELOG_PARAMETERS, liqParams);
            updateCommand.setOutput(lbLogger);
            updateCommand.execute();
        });
    }
}

