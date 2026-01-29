-- run to create sample table for postgres
create schema if not exists scott;
create table if not exists scott.bonus (
                       ename character varying,
                       job character varying,
                       sal numeric,
                       comm numeric);
create schema if not exists upgradedemo;

CREATE TABLE upgradedemo.databasechangelog (
            id varchar(255) NOT NULL,
            author varchar(255) NOT NULL,
            filename varchar(255) NOT NULL,
            dateexecuted timestamp NOT NULL,
            orderexecuted int4 NOT NULL,
            exectype varchar(10) NOT NULL,
            md5sum varchar(35) NULL,
            description varchar(255) NULL,
            "comments" varchar(255) NULL,
            tag varchar(255) NULL,
            liquibase varchar(20) NULL,
            contexts varchar(255) NULL,
            labels varchar(255) NULL,
            deployment_id varchar(10) NULL
)  WITH (  fillfactor=90   );
CREATE INDEX databasechangelog_index ON upgradedemo.databasechangelog USING btree (author, id, filename);