set serveroutput on size unlimited
set feedbacin on term on echo on
DECLARE
lv_user VARCHAR2(50);
lv_pass VARCHAR2(100) := 'u_pick_it';
DEFAULT_TABLESPACE VARCHAR2(100) := 'DEVELOPMENT';
TEMP_TABLESPACE VARCHAR2(100) :='TEMP';
upgrade_schema varchar2(50) :=   'upgradedemo';

CURSOR user_check_C IS
SELECT 'Y'
FROM dba_users    WHERE username       = upper(lv_user);
user_exists varchar2(1):='N';

table_exists varchar2(1);
cursor table_exists_c ( tablename varchar2 ) is
select 'Y' from dba_tables where owner = upper(lv_user)
                             and table_name = upper(tablename);

index_exists varchar2(1);
index_sql varchar2(300);
cursor index_exists_c ( owner varchar2 ) is
select 'Y' from dba_indexes WHERE INDEX_name = 'DATABASECHANGELOG_INDEX'
                              AND table_name = 'DATABASECHANGELOG' AND table_owner  = upper(owner)
                              AND owner = upper(owner);



PROCEDURE run_command(command varchar2) is
BEGIN
	    dbms_output.put_line('command to run ' || command);
EXECUTE IMMEDIATE command;
EXCEPTION
	    WHEN OTHERS THEN
	        dbms_output.put_line('Exception with command ' || command || ' error: ' || SQLERRM);
END run_command;

BEGIN


lv_user := upper(upgrade_schema);
OPEN user_check_C;
FETCH user_check_C
    INTO user_exists;
if user_check_C%notfound then
        user_exists := 'N';
end if;
CLOSE user_check_C;

IF user_exists = 'N' THEN
        run_command('create user '|| lv_user ||' identified by  "'||lv_pass || '"'  ) ;

ELSE
        dbms_output.put_line ('User ' || lv_user || ' creation skipped since the user already exists.');

END IF;
    run_command('grant create any context to ' || lv_user);
    run_command('grant create session to ' || lv_user);
     -- ROLES
    run_command(  'GRANT CONNECT TO ' || lv_user);
    run_command(  'ALTER USER '  || lv_user || '  DEFAULT ROLE CONNECT');
    run_command(  'GRANT UNLIMITED TABLESPACE TO ' || lv_user);
    run_command(  'grant create any table to ' || lv_user);

    table_exists := 'N';
open table_exists_c('DATABASECHANGELOG');
fetch table_exists_c into table_exists;
if table_exists_c%notfound then
      table_exists := 'N';
END IF;
close table_exists_c;


dbms_output.put_line('create table ' || table_exists );
    if table_exists = 'N' then
       dbms_output.put_line('Create table begin');
       run_command( 'CREATE TABLE ' || lv_user || '.DATABASECHANGELOG ' ||
                 '  (	ID VARCHAR2(255 CHAR) NOT NULL ENABLE, ' ||
                   '     AUTHOR VARCHAR2(255 CHAR) NOT NULL ENABLE,' ||
                  '      FILENAME VARCHAR2(255 CHAR) NOT NULL ENABLE,' ||
                  '      DATEEXECUTED TIMESTAMP (6) NOT NULL ENABLE,' ||
                  '      ORDEREXECUTED NUMBER(*,0) NOT NULL ENABLE,' ||
                  '      EXECTYPE VARCHAR2(10 CHAR) NOT NULL ENABLE,' ||
                  '      MD5SUM VARCHAR2(35 CHAR),' ||
                 '       DESCRIPTION VARCHAR2(255 CHAR),' ||
                  '      COMMENTS VARCHAR2(255 CHAR),' ||
                 '       TAG VARCHAR2(255 CHAR),' ||
                 '       LIQUIBASE VARCHAR2(20 CHAR),' ||
                 '       CONTEXTS VARCHAR2(255 CHAR),' ||
                 '       LABELS VARCHAR2(255 CHAR),' ||
                '        DEPLOYMENT_ID VARCHAR2(10 CHAR)' ||
                '   ) SEGMENT CREATION IMMEDIATE' ||
                '     PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255' ||
                '   NOCOMPRESS LOGGING' ||
                 '    STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645' ||
                 '    PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1' ||
                 '    BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)' ||
                 '    TABLESPACE DEVELOPMENT ');
       dbms_output.put_line('Created table');

       dbms_output.put_line('Create index begin');
       run_command( 'create  index ' || lv_user || '.DATABASECHANGELOG_INDEX_' || upper(lv_user) || '  on ' ||
         lv_user ||  '.databasechangelog ( author, id, filename)');
        dbms_output.put_line('Created index');
end if;
dbms_output.put_line('Owner before checking if index exists ' || lv_user);
index_exists := 'N';
index_sql := 'select  ''Y''  from dba_indexes where index_name = ' || '''' ||  'DATABASECHANGELOG_INDEX_'
      || upper(lv_user)
      || '''' || ' and table_owner = ''' || lv_user || '''' ;
dbms_output.put_line('index sql: ' || index_sql);
BEGIN
EXECUTE IMMEDIATE index_sql INTO index_exists;
EXCEPTION WHEN NO_DATA_FOUND THEN
        index_exists := 'N';
END;

dbms_output.put_line('Index exists ' || index_exists);
if index_exists = 'N' then
     dbms_output.put_line('Create index begin');
execute immediate 'create  index  ' || lv_user || '.DATABASECHANGELOG_INDEX_' || upper(lv_user) || ' on ' ||
                  lv_user ||  '.databasechangelog ( author, id, filename)';
dbms_output.put_line('Created index');
end if;

table_exists := 'N';
open table_exists_c('DATABASECHANGELOGLOCK');
fetch table_exists_c into table_exists;
close table_exists_c;
if table_exists = 'N' then
          dbms_output.put_line('Create lock table');
           run_command( 'CREATE TABLE ' || lv_user || '.DATABASECHANGELOGLOCK ' ||
          '  (	ID NUMBER(*,0) NOT NULL ENABLE, ' ||
           '  LOCKED NUMBER(1,0) NOT NULL ENABLE, ' ||
           '  LOCKGRANTED TIMESTAMP (6), ' ||
           '  LOCKEDBY VARCHAR2(255 CHAR), ' ||
           '  CONSTRAINT PK_DATABASECHANGELOGLOCK PRIMARY KEY (ID) ' ||
           '      USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS ' ||
           '      STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645 ' ||
           '      PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 ' ||
            '     BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT) ' ||
            '     TABLESPACE DEVELOPMENT  ENABLE ' ||
       ' ) SEGMENT CREATION IMMEDIATE ' ||
         ' PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 ' ||
        ' NOCOMPRESS LOGGING ' ||
         ' STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645 ' ||
         ' PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 ' ||
         ' BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT) ' ||
         ' TABLESPACE DEVELOPMENT ') ;
          dbms_output.put_line('Created lock table');
end if;

END;
/
