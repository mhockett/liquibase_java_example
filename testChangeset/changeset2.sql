--liquibase formatted sql
--changeset scott:changeset2.sql   splitStatements:true  stripComments:false    logicalFilePath:changeset2.sql

insert into bonus ( ename, job, sal, comm)
values (  '${ENAME}', '${JOB}', 100000, 10000 );

