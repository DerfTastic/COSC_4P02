

class Ticket {
    /** @type{string} */name
    /** @type{number} */price
    /** @type{number?} */available_tickets
}

class OurEvent {
    /** @type{string} */name
    /** @type{string} */description
    /** @type{"concert"|"conference"|"workshop"|"festival"} */type
    /** @type{"sport"|"music"|"technology"|"education"|"art"} */category
    /** @type{string?} */picture
    /** @type{object?} */metadata

    /** @type{number?} */start
    /** @type{number?} */duration

    /** @type{number?} */available_total_tickets

    /** @type{string[]} */tags

    /** @type{string?} */location_name
    /** @type{number?} */location_lat
    /** @type{number?} */location_long

    /** @type{Ticket[]} */tickets
}

class Organizer {
    /** @type{string} */name
    /** @type{string} */email
    /** @type{string?} */bio
    /** @type{string?} */disp_email
    /** @type{string?} */disp_phone_number

    /** @type{string} */picture
    /** @type{string} */banner

    /** @type{OurEvent[]} */events
}

async function delete_database_stuff(){
    try{
        await api.admin.execute_sql("delete from events");
        await api.admin.execute_sql("delete from users where id != 1");
        alert("done");
    }catch({error, code}){
        alert(error)
    }
}

async function makie(){
    try{
        await create_organizers(await (await fetch("/js/sample_events.json")).json());
    }catch(e){
        console.log(e);
        if(e.error != undefined){
            alert(e.error);
        }else{
            alert(e);
        }
    }
    alert("Finished");
}

async function create_all_user_defined(){
    try{
        const json = JSON.parse(document.getElementById("json").value);
        await Promise.all([
            create_organizers(json)
            // create_organizers(joycelyn),
            // create_organizers(taylor),
            // create_organizers(sinatra),
            // create_organizers(ben),
            // create_organizers(parker),
            // create_organizers(everything)
        ]);
    }catch(e){
        console.log(e);
        alert(e);
    }
    alert("Finished");
}

let tracker = 0;
function random_image_url(){
    tracker++;
    return `https://picsum.photos/1366/768?random=${tracker}`;
}

async function create_random() {
    /** @type{Organizer[]}*/   
    var list = [];
    const orgamizers = 30;
    
    for(let i = 0; i < orgamizers; i ++){
        const organizer = new Organizer();
        organizer.name = chance.name();
        organizer.email = chance.email();
        organizer.bio = chance.paragraph({ sentences: chance.integer({ min: 2, max: 5 }) });
        if(chance.bool())
            organizer.disp_email = chance.email();
        if(chance.bool())
            organizer.disp_phone_number = chance.phone();

        organizer.picture = random_image_url();
        organizer.banner = random_image_url();

        organizer.events = [];
        const events = chance.integer({ min: 2, max: 5 });
        for(let e = 0; e < events; e ++){
            const event = new OurEvent();

            event.name = chance.sentence({ words: chance.integer({ min: 2, max: 5 }) });
            event.description = chance.paragraph({ sentences: chance.integer({ min: 2, max: 5 }) });
            event.category = ["sport", "music", "technology", "education", "art"][chance.integer({min: 0, max: 4})];
            event.type = ["concert", "conference", "workshop", "festival"][chance.integer({min: 0, max: 3})];
        
            event.picture = random_image_url();

            event.start = chance.timestamp()*1000;
            event.duration = chance.integer({ min: 1000*60*5, max: 1000*60*60*12 });

            if(true||chance.bool())
                event.available_total_tickets = chance.integer({ min: 50, max: 500 });

            if(true||chance.bool({likelihood: 70})){
                event.location_name = chance.address();
                if(chance.bool({likelihood: 70})){
                    event.location_lat = chance.latitude();
                    event.location_long = chance.longitude();
                }
            }
            
            event.tags = [];
            const tags = chance.integer({ min: 2, max: 10 });
            for(let t = 0; t < tags; t ++){
                let tag;
                do{
                    tag = chance.word();
                }while(event.tags.includes(tag));
                event.tags.push(tag)
            }

            event.tickets = [];
            const tickets = chance.integer({ min: 1, max: 4 });
            for(let t = 0; t < tickets; t ++){
                const ticket = new Ticket();
                ticket.name = chance.sentence({ words: chance.integer({ min: 2, max: 5 }) });
                ticket.price = chance.integer({min: 0, max: 500});
                if(chance.bool())
                    ticket.available_tickets = chance.integer({min: 10, max: 500});

                event.tickets.push(ticket);
            }

            organizer.events.push(event);
        }
        list.push(organizer);
    }

    await create_organizers(list);
    alert("Finished");
}

/**
 * @param {Organizer[]} organizers 
 */
async function create_organizers(organizers) {
    await Promise.all(organizers.map(async(organizer) => {
        await api.user.register(organizer.name, organizer.email, "password");
        const session = await api.user.login(organizer.email, "password");
        await api.user.update_user({
            bio: organizer.bio,
            disp_email: organizer.disp_email,
            disp_phone_number: organizer.disp_phone_number,
        }, session);
        if (organizer.picture != undefined && organizer.picture != null) {
            const pic = await (await fetch(organizer.picture)).blob();
            await api.user.set_user_picture(pic, session);
        }
        if (organizer.banner != undefined && organizer.banner != null) {
            const pic = await (await fetch(organizer.banner)).blob();
            await api.user.set_user_banner_picture(pic, session);
        }
        await api.organizer.convert_to_organizer_account(session);
        await create_events(organizer.events, session);
    }));
}

/**
 * @param {OurEvent[]} events 
 * @param {string} session 
 */
async function create_events(events, session) {
    if (events == undefined || events == null) return;
    await Promise.all(events.map(async(event) => {
        const id = await api.events.create_event(session);
        await api.events.update_event(id, {
            available_total_tickets: event.available_total_tickets,
            category: event.category,
            description: event.description,
            duration: event.duration,
            location_lat: event.location_lat,
            location_long: event.location_long,
            location_name: event.location_name,
            metadata: event.metadata,
            name: event.name,
            start: event.start,
            type: event.type
        }, session);

        if (event.picture != undefined && event.picture != null) {
            const pic = await (await fetch(event.picture)).blob();
            await api.events.set_picture(id, pic, session);
        }
        if (event.tags != undefined && event.tags != null)
            for (const tag of event.tags) {
                await api.events.add_event_tag(id, tag, session);
            }
        await create_tickets(event.tickets, id, session);
        await api.events.set_draft(id, false, session);
    }));
}

/**
 * 
 * @param {Ticket[]} tickets 
 * @param {number} event_id 
 * @param {string} session 
 */
async function create_tickets(tickets, event_id, session) {
    if (tickets == undefined || tickets == null) return;
    await Promise.all(tickets.map(async(ticket) => {
        const id = await api.tickets.create_ticket(event_id, session);
        await api.tickets.update_ticket(id, {
              name: ticket.name,
              price: ticket.price,
              available_tickets: ticket.available_tickets
        }, session);
    }));
}