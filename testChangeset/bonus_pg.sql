-- run to create sample table for postgres
create schema if not exists scott;
create table if not exists scott.bonus (
                       ename character varying,
                       job character varying,
                       sal numeric,
                       comm numeric);