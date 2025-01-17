insert into users values(null, 'leo', 'leo@gmail.com', '', '', null, null);
insert into users values(null, 'yui', 'yui@gmail.com', '', '', null, null);

insert into organizers values(null, false, 1);
insert into organizers values(null, true, 500);

insert into users values(null, 'little', 'guy@yahoo.com', '', '', 1, null);
insert into users values(null, 'big', 'guy@gmail.com', '', '', 2, null);

insert into events values(null, 1, "Taylor Swift Concert", "", null, null, "Somewhere", null, null);
insert into events values(null, 1, "Taylor Swift Concert2", "", null, null, "Somewhere else", null, null);
insert into events values(null, 2, "Taylor Swift Concert2", "", null, null, "Somewhere else", null, null);
insert into events values(null, 2, "ROM", "", null, null, "Toronto", null, null);

insert into tickets values(null, 1, "General", 5, 100);
insert into tickets values(null, 1, "Student", 4, 100);

insert into tickets values(null, 2, "General", 6, null);
insert into tickets values(null, 2, "Students", 6, null);

insert into tickets values(null, 4, "General", 4, null);
insert into tickets values(null, 4, "Students", 3, null);
insert into tickets values(null, 4, "Old Ppl", 3, null);