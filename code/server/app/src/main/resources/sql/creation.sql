
create table users(
    id INTEGER primary key not null,
    full_name TEXT not null,
    email TEXT not null unique,
    pass TEXT not null,
    admin BOOLEAN not null,
    organizer BOOLEAN not null,
    bio TEXT,
    disp_phone_number TEXT,
    disp_email TEXT,
    picture INTEGER,
    banner INTEGER
);

CREATE UNIQUE INDEX user_email_idx ON users(email);
CREATE INDEX user_full_name_idx ON users(full_name);
CREATE INDEX user_pass_idx ON users(pass);
CREATE INDEX user_admin_idx ON users(admin);
CREATE INDEX user_organizer_idx ON users(organizer);
CREATE INDEX user_disp_phone_number_idx ON users(disp_phone_number);
CREATE INDEX user_disp_email_idx ON users(disp_email);

create table events(
    id INTEGER primary key not null,
    owner_id INTEGER not null,
    name TEXT not null,
    description TEXT not null,
    start INTEGER,
    duration INTEGER,
    release_time INTEGER,
    category TEXT not null,
    type TEXT not null,
    picture INTEGER,
    metadata TEXT,
    available_total_tickets INTEGER,
    draft BOOLEAN not null,

    location_name TEXT,
    location_lat REAL,
    location_long REAL,

     FOREIGN KEY (owner_id)
           REFERENCES users (id)
              ON DELETE CASCADE
              ON UPDATE NO ACTION
);

CREATE TRIGGER event_owner_is_organizer
BEFORE INSERT ON events
BEGIN
    SELECT RAISE(FAIL, "user is not organizer")
    FROM users WHERE id = NEW.owner_id AND organizer=false;
END;


CREATE INDEX event_organizer_id_idx ON events(owner_id);
CREATE INDEX event_name_idx ON events(name);
CREATE INDEX event_category_idx ON events(category);
CREATE INDEX event_type_idx ON events(type);
CREATE INDEX event_location_name_idx ON events(location_name) WHERE location_name IS NOT NULL;
CREATE INDEX event_location_lat_idx ON events(location_lat) WHERE location_lat IS NOT NULL;
CREATE INDEX event_location_long_idx ON events(location_long) WHERE location_long IS NOT NULL;
CREATE INDEX event_draft_idx ON events(draft);
CREATE INDEX event_start_idx ON events(start) WHERE start IS NOT NULL;
CREATE INDEX event_duration_idx ON events(duration) WHERE duration IS NOT NULL;

create table event_tags(
    tag TEXT not null,
    event_id INTEGER not null,
    PRIMARY KEY(tag, event_id),
    FOREIGN KEY (event_id)
       REFERENCES events (id)
          ON DELETE CASCADE
          ON UPDATE NO ACTION
);

CREATE INDEX event_tags_event_id_idx ON event_tags(event_id);
CREATE INDEX event_tags_tag_idx ON event_tags(tag);


create table tickets(
    id INTEGER primary key not null,
    event_id INTEGER,
    name TEXT not null,
    price INTEGER not null,
    available_tickets INTEGER,

     FOREIGN KEY (event_id)
           REFERENCES events (id)
              ON DELETE SET NULL
              ON UPDATE NO ACTION
);

CREATE INDEX tickets_event_id_idx ON tickets(event_id) WHERE event_id IS NOT NULL;
CREATE INDEX tickets_price_idx ON tickets(price);
CREATE INDEX tickets_available_tickets_idx ON tickets(available_tickets) WHERE available_tickets IS NOT NULL;

create table purchased_tickets(
    id INTEGER primary key not null,
    user_id INTEGER,
    ticket_id INTEGER,

    payment_id INTEGER NOT NULL,

    purchase_price INTEGER NOT NULL,

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

CREATE INDEX purchased_tickets_user_id_idx ON purchased_tickets(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX purchased_ticket_id_id_idx ON purchased_tickets(ticket_id) WHERE ticket_id IS NOT NULL;
CREATE INDEX purchased_payment_id_id_idx ON purchased_tickets(payment_id) WHERE payment_id IS NOT NULL;


create table payments(
    id INTEGER primary key not null,
    user_id INTEGER,
    receipt TEXT NOT NULL,
    amount INTEGER NOT NULL,
    payment_date INTEGER NOT NULL,
    FOREIGN KEY (user_id)
        REFERENCES users (id)
            ON DELETE SET NULL
            ON UPDATE NO ACTION
);

CREATE INDEX payments_user_id_idx ON payments(user_id);

create table sessions(
    id INTEGER primary key not null,
    token TEXT unique,
    user_id INTEGER not null,
    expiration DATE not null,
    agent TEXT,
    ip TEXT,


    FOREIGN KEY (user_id)
        REFERENCES users (id)
            ON DELETE CASCADE
            ON UPDATE NO ACTION
);

CREATE INDEX sessions_user_id_idx ON sessions(user_id);
CREATE UNIQUE INDEX sessions_token_idx ON sessions(token) WHERE token IS NOT NULL;
CREATE INDEX sessions_expiration_idx ON sessions(expiration);

insert into users values(null, 'Admin', 'admin@localhost', 'ff8edf427da86f50c08fc4ad89396b358c266e4b3966ddf56b540a2f8e470b40', true, true, null, null, null, null, null);