insert into users values(null, 'leo', 'leo@gmail.com', 'a1a3dc053beadae5e201db681e71bc4a88c6777617edbdffa57314f3191502d9', null, null, null);
insert into users values(null, 'yui', 'yui@gmail.com', 'ba3c1ae8e18507315efe899d6359f5ce9725e1e07ea8e3e1f4d38ffe439c57a2', '', null, null);

insert into organizers values(null, false, 1);
insert into organizers values(null, true, 500);

insert into users values(null, 'little', 'guy@yahoo.com', '4ea5768948eb7165c3147c0b20710c367c877d81d11b278ba6f4a4e140e9ce6d', '', 1, null);
insert into users values(null, 'big', 'guy@gmail.com', '4f29c5b011618801b5050c4f4e26b74b39ea88720458f8c7ec07b42c877376be', '', 2, null);

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