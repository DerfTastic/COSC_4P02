drop table if exists users;
drop table if exists organizers;
drop table if exists events;
drop table if exists tickets;
drop table if exists purchased_tickets;
drop table if exists payments;


create table users(
    id INTEGER primary key not null,
    name TEXT not null,
    email TEXT not null unique,
    pass TEXT not null,
    bio TEXT not null default "",
    organizer_id INTEGER default NULL,

     FOREIGN KEY (organizer_id)
           REFERENCES organizers (id)
              ON DELETE SET NULL
              ON UPDATE NO ACTION
);

create table organizers(
    id INTEGER primary key not null,
    has_analytics BOOLEAN not null
);

create table events(
    id INTEGER primary key not null,
    name TEXT not null,
    description TEXT not null,
    picture INTEGER,

    location_name TEXT,
    location_lat REAL,
    location_long REAL
);

create table tickets(
    id INTEGER primary key not null,
    event_id INTEGER,
    name TEXT not null,
    price NUMERIC not null,
    available_tickets INTEGER,

     FOREIGN KEY (event_id)
           REFERENCES events (id)
              ON DELETE SET NULL
              ON UPDATE NO ACTION
);

create table purchased_tickets(
    user_id INTEGER,
    ticket_id INTEGER,

    payment_id INTEGER NOT NULL,

    purchase_date NUMERIC NOT NULL,
    purchase_price NUMERIC NOT NULL,

    PRIMARY KEY(user_id, ticket_id),

    FOREIGN KEY (user_id)
        REFERENCES users (id)
            ON DELETE SET NULL
            ON UPDATE NO ACTION,
    FOREIGN KEY (ticket_id)
        REFERENCES tickets (id)
            ON DELETE SET NULL
            ON UPDATE NO ACTION,
    FOREIGN KEY (payment_id)
        REFERENCES payments (id)
            ON DELETE RESTRICT
            ON UPDATE RESTRICT
);

create table payments(
    id INTEGER primary key not null,
    user_id INTEGER,
    receipt TEXT NOT NULL,
    amount NUMERIC NOT NULL,
    FOREIGN KEY (user_id)
        REFERENCES users (id)
            ON DELETE SET NULL
            ON UPDATE NO ACTION
);