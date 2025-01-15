drop table if exists person;
create table person (
    id  integer primary key not null,
    name string
);
insert into person values(null, 'leo');
insert into person values(null, 'yui');