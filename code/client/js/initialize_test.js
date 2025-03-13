/** @type{Organizer[]}*/
const everything = [
    {
        name: "Hockey Rink",
        email: "hockey.rink@hotmail.com",

        // optional stuff
        bio: "We are the hokey rink downtown! come visit us for many stuff n things",
        disp_email: "hockey.rink@hotmail.com",
        disp_phone_number: "111-222-1234",
        banner: "https://m.media-amazon.com/images/I/61lo8QevtJL._AC_UF894,1000_QL80_.jpg",
        picture: "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQb4q7IaES8N3PHuqhiio2997LyGArLlwdNQQ&s",
        
        // this is a list of events
        events: [
            {
                name: "The Hockey Team Game",
                description: "Come join us for this very exciting hokey game!!!",
                category: "sport",
                type: "stadium",
                tags: ["Hokey", "Indoors", "Hometown", "Sports", "Ice", "Hotdog"],
                picture: "https://m.media-amazon.com/images/I/61lo8QevtJL._AC_UF894,1000_QL80_.jpg",
                //optional
                metadata: {background_color: "#FF0000"},
                //optional
                location_name: "Icerink downtown St.Catharines",
                //optional
                location_lat: undefined,
                location_long: undefined,
                // optional
                available_total_tickets: 100,
                
                // list of tickets
                tickets: [
                    {
                        name: "General",
                        price: 9.99
                    },
                    {
                        name: "The Big Boxes At The Back",
                        price: 500,
                        available_tickets: 10
                    }
                ]
            }
        ]
    }
]

class Tickets{
    /** @type{string} */name
    /** @type{number} */price
    /** @type{number} */available_tickets
}

class OurEvent{
    /** @type{string} */name
    /** @type{string} */description
    /** @type{string} */type
    /** @type{string} */category
    /** @type{string?} */picture
    /** @type{object?} */metadata

    /** @type{number?} */available_total_tickets

    /** @type{string[]} */tags

    /** @type{string?} */location_name
    /** @type{number?} */location_lat
    /** @type{number?} */location_long

    /** @type{Tickets[]} */tickets
}

class Organizer{
    /** @type{string} */name
    /** @type{string} */email
    /** @type{string?} */bio
    /** @type{string?} */disp_email
    /** @type{string?} */disp_phone_number
    
    /** @type{string} */picture
    /** @type{string} */banner

    /** @type{OurEvent[]} */events
}

create_organizers(everything);

/**
 * @param {Organizer[]} organizers 
 */
async function create_organizers(organizers){
    for(const organizer of organizers){
        (async () => {
            await api.user.register(organizer.name, organizer.email, "password");
            const session = await api.user.login(organizer.email, "password");
            await api.user.update_user({
                bio: organizer.bio,
                disp_email: organizer.disp_email,
                disp_phone_number: organizer.disp_phone_number,
            }, session);
            if(organizer.picture!=undefined&&organizer.picture!=null){
                const pic = await (await fetch(organizer.picture)).blob();
                await api.user.set_user_picture(pic, session);
            }
            if(organizer.banner!=undefined&&organizer.banner!=null){
                const pic = await (await fetch(organizer.banner)).blob();
                await api.user.set_user_banner_picture(pic, session);
            }
            await api.organizer.convert_to_organizer_account(session);
            await create_events(organizer.events, session);
        })();        
    }
}

/**
 * @param {OurEvent[]} events 
 * @param {string} session 
 */
async function create_events(events, session){
    if(events==undefined||events==null)return;
    for(const event of events){
        (async() => {
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
            
            if(event.picture!=undefined&&event.picture!=null){
                const pic = await (await fetch(event.picture)).blob();
                await api.events.set_picture(id, pic, session);
            }
            if(event.tags!=undefined&&event.tags!=null)
                for(const tag of event.tags){
                    await api.events.add_event_tag(id, tag, session);
                }
            await create_tickets(event.tickets, id, session);
            await api.events.set_draft(id, false, session);
        })()
    }
}

/**
 * 
 * @param {Ticket[]} tickets 
 * @param {number} event_id 
 * @param {string} session 
 */
async function create_tickets(tickets, event_id, session){
    if(tickets==undefined||tickets==null)return;
    for(const ticket of tickets){
        (async() => {
            //TODO
        })()
    }
}