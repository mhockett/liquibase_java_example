--liquibase formatted sql
--changeset scott:changeset.sql   splitStatements:true  stripComments:false    logicalFilePath:changeset.sql
-- **** *********************************************************
-- insert into scott bonus new values for CIO should not be stripped out 
-- **** *********************************************************
  insert into scott.bonus ( ename, job, sal, comm)
  values (  'ME', 'CIO', 100000, 10000 );
