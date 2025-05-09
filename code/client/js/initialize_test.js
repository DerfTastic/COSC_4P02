

class Ticket {
    /** @type{string} */name
    /** @type{number} */price
    /** @type{number?} */total_tickets
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

    /** @type{number?} */release_date

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

            event.start = chance.integer({ min: new Date("2025-01-01").getTime(), max: new Date("2026-01-01").getTime() });
            event.release_date = chance.integer({ min:  -1000*60*60*24*30, max: 0});
            event.duration = chance.integer({ min: 1000*60*5, max: 1000*60*60*12 });

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
                ticket.price = chance.integer({min: 0, max: 50000000});
                if(chance.bool())
                    ticket.total_tickets = chance.integer({min: 10, max: 500});

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
        await api.payment.make_purchase({
            payment: {
                billing: "My House",
                card: "1234567890",
                code: "921",
                exparation: "25/45",
                name: "Dw about it"
            },
            items: [new AccountOrganizerUpgradeOrderItem()]
        }, session);
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
              total_tickets: ticket.total_tickets
        }, session);
    }));
}

async function get_purchaser_ID() {
    var userInfo = await page.account.userinfo();
    var id = userInfo.id;
    for (const ele of document.getElementsByClassName("userID")) {
        ele.innerHTML = id;
    }
}

async function login_as_user(email, pass) {
    var sesh;
    try {
        cookies.deleteSessionToken();
        sesh = await api.user.login(email, pass)
        cookies.setSession(sesh);
    } catch ({ error, code }) {
        alert(error);
    }
    location.reload(); 
    if (utility.is_session_id_current(sesh)) {
        get_purchaser_ID();
    }
}

var createdEventID, createdTicketID;

async function make_event_and_ticket() {
    session = cookies.getSession();
    var eventTicketDiv = document.getElementById("eventTicketDiv");
    try {
        createdEventID = await api.events.create_event(session);
        await api.events.set_draft(createdEventID, false, session);
        createdTicketID = await api.tickets.create_ticket(createdEventID, session);
        await api.tickets.update_ticket(createdTicketID, {
            name: "General Admission",
            price: 10000000,
            total_tickets: 5
        }, session);

        const para = document.createElement("div");
        para.innerHTML = `<p>New event and ticket created. event: <span style="color:lime;">${createdEventID}</span>, ticket: <span style="color:lime;">${createdTicketID}</span></p>`
        eventTicketDiv.appendChild(para);
    } catch(e){
        console.log(e);
        alert(e);
    }
}

async function make_purchases() {
    if (typeof window.createdTicketID === 'undefined') {
        console.log("didn't make an event and ticket :(")
        alert("didn't make an event and ticket :(")
        return
    }
    try {
        var createdItems = [new TicketOrderItem(window.createdTicketID)]

        const receipt_item = await api.payment.make_purchase({
            payment: {
                billing: "My House",
                card: "1234567890",
                code: "921",
                expiration: "25/45",
                name: "Dw about it"
            },
            items: createdItems
        }, session);

        console.log("Receipt:")
        console.log(receipt_item);

        console.log("IDs of purchased_tickets:")
        // Make a QR for each purchased item
        for (var item of receipt_item.items) {
            var ptID = { // PurchasedTicketId
                "id": item.id.pid, 
                "salt": item.id.salt
            }
            console.log(item.id.pid);
            var QRimgsrc = gen_qr(JSON.stringify(ptID), 200);
            appendQR(QRimgsrc, createdEventID, ptID);
        }

    }catch(e){
        console.log(e);
        alert(e);
    }
}

/** Appends a new QR code, event info and purchasedTicketID information 
 * (id and salt) inside the ".content" div
 * @param {String} imgsrc The src attribute of the QR code image
 * @param {integer} eventID The ID of the event associated with this QR code
 * @param {PurchasedTicketId} PTIDinfo The purchased ticket id info
 */
function appendQR(imgsrc, eventID, PTIDinfo) {
    const contentDivs = document.getElementsByClassName("content");
    if (contentDivs.length === 0) {
        console.error("No content div");
        return;
    }
    else if (contentDivs.length > 1) {
        console.error("Too many content divs");
        return;
    }
    const contentDiv = contentDivs[0];

    const newImageEle = document.createElement("img");
    newImageEle.src = imgsrc;
    contentDiv.appendChild(newImageEle.cloneNode(true));
    const newInfoEle = document.createElement("p");
    newInfoEle.innerHTML = "QR for event ID: <span style='color:lime'>" + eventID + "</span>";
    newInfoEle.innerHTML += "\n" + JSON.stringify(PTIDinfo);
    contentDiv.appendChild(newInfoEle.cloneNode(true));
    contentDiv.appendChild(document.createElement("br"));
}