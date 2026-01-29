# Liquibase Java Example (Gradle)

Demo for Liquibase  using Gradle build system

## Requirements

- **JDK 17** (recommended)
- Gradle is included via Gradle Wrapper (no separate installation needed)

## Overview

This demo includes the Java program `LiquibaseScopeExample.java` which runs Liquibase for a changelog using the Liquibase 4.25.1 ScopeCommand API.

The demo runs on Oracle and PostgreSQL databases.
## Building the Project

Use the Gradle Wrapper to clean and build:

```bash
./gradlew clean build
```

Or individually:

```bash
./gradlew clean
./gradlew build
```

## Setup


1. Verify your Oracle database has the `scott.bonus` demo table and run the script `testChangeset/bonus_oracle.sql` to create it if needed.
The script also creates the schema used for the databasechangelog table if it does not exist, and the table and index. 

2. You can use `testChangeset/bonus_pg.sql` to create the table on your PostgreSQL database along with the schema for the databasechangelog table.

3. Configure your database connection by passing properties to Gradle tasks to run the tests 

   **For Oracle:**
   ```bash
   ./gradlew runOracle -PdbUrl="jdbc:oracle:thin:@localhost:1521/xxxxx" \
     -PdbUser="someuser" \
     -PdbPwd="somepassword" \
     -PchangelogSchema="schemaName"
   ```

   **For PostgreSQL:**
   ```bash
   ./gradlew runPostgres -PdbUrl="jdbc:postgresql://localhost:5432/xxxxxx" \
     -PdbUser="someuser" \
     -PdbPwd="somepassword" \
     -PchangelogSchema="schemaName" \
     -PpgDefaultSchema="defaultSchema"
   ```

   Alternatively, you can modify the default values in `build.gradle` file in the `runOracle` or `runPostgres` task definitions.

 

## How It Works

1. The Java demo program is **rerunnable**. The script deletes the `databasechangelog` row and the `scott.bonus` rows before running
2. The demo prints results at the end:
   - Queries the `databasechangelog` for the row that should be created
   - Queries for the row that should be inserted into `scott.bonus`
   - Prints **SUCCESS** or **FAILURE** based on the results



## Project Structure

```
liquibase_java_example/
├── build.gradle              # Gradle build configuration
├── settings.gradle           # Gradle settings
├── gradlew                   # Gradle wrapper script (Unix)
├── gradlew.bat              # Gradle wrapper script (Windows)
├── gradle/
│   └── wrapper/             # Gradle wrapper files
├── src/
│   └── main/
│       └── java/
│           └── LiquibaseScopeExample.java
├── testChangeset/
│   ├── bonus_pg.sql
│   ├── changelog.xml
│   ├── changeset.sql
│   ├── changeset2.sql
│   └── liquibase.properties
└── logback.xml              # Logging configuration
```

## Dependencies

All dependencies are managed by Gradle (see `build.gradle`):
- Liquibase Core 4.29.2 or 5.0.1.  Toggle between the two version in `build.gradle` to test compatibility and time comparison.
- Oracle JDBC Driver (ojdbc11)
- PostgreSQL JDBC Driver 42.7.4
- SLF4J & Logback for logging
- Apache Commons utilities
