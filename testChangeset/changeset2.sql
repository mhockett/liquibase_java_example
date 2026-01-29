--liquibase formatted sql
--changeset scott:changeset2.sql   splitStatements:true  stripComments:true    logicalFilePath:changeset2.sql
-- **** *********************************************************
-- insert into scott bonus new values with parameters should be stripped out 
-- **** *********************************************************
insert into scott.bonus ( ename, job, sal, comm)
values (  '${ENAME}', '${JOB}', 100000, 10000 );

