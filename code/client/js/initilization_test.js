function assert(condition, message) {
    if (!condition) {
        throw message || "Assertion failed";
    }
}

class Account{
    /** @type{string} */ name
    /** @type{string} */ email
    /** @type{string} */ password
    /** @type{Session} */ session
}

function log(e){
    console.log(e);
    if(typeof e == "object")
        document.getElementById("console").innerText += JSON.stringify(e, null, 2)+"\n";
    else
        document.getElementById("console").innerText += e+"\n";
}

var time = {};

function timeStart(e){
    time[e] = new Date();
    log("Started: " + e);
}

function timeEnd(e){
    const start = time[e];
    const end = new Date();
    log("Ended: " + e + " Took: " + (end.getTime()-start.getTime()) + "ms");
}

document.addEventListener("DOMContentLoaded", e => run_all());

async function run_all(){  
    const pre_stats = await api.admin.get_server_statistics(await api.user.login("admin@localhost", "admin"));
    timeStart("");
    const start = new Date();

    const user_count = 500;
    const organizer_count = 100;
    const admin_count = 5;  
    // test_register_1();
    // test_register_2();
    // test_default_admin_account();

    timeStart("Creating Admin Users");
    const admins = await Promise.all(generate_users(admin_count).values());
    timeEnd("Creating Admin Users");


    timeStart("Creating Regular Users");
    const users = await Promise.all(generate_users(user_count).values());
    timeEnd("Creating Regular Users");
    
    timeStart("Creating Organizer Users");
    const organizers = await Promise.all(generate_users(organizer_count).map(async e => {
        const user = await e;
        await api.organizer.convert_to_organizer_account(user.session);
        return user;
    }).values());
    timeEnd("Creating Organizer Users");

    
    timeStart("Creating Events");
    const meows = [];
    for(let i = 0; i < 600; i ++){
        var session = organizers[chance.integer({ min: 0, max: organizers.length-1 })].session;
        meows.push(create_example_event(session, i));
    }
    await Promise.all(meows.values());
    timeEnd("Creating Events");


    const end = new Date();
    const diff = (end.getTime()-start.getTime())/1000.0;
    console.log(end.getTime());
    console.log(start.getTime());
    console.log(diff);
    timeEnd("");
    const post_stats = await api.admin.get_server_statistics(await api.user.login("admin@localhost", "admin"));
    log("Requests Made: " + (post_stats.total_requests_handled-pre_stats.total_requests_handled));
    log("Requests Per Second: " + (post_stats.total_requests_handled-pre_stats.total_requests_handled)/diff);
    log("Db Queries Made: " + (post_stats.total_db_statements_executed-pre_stats.total_db_statements_executed));
    log("Db Queries Per Second: " + (post_stats.total_db_statements_executed-pre_stats.total_db_statements_executed)/diff);
    log("Pre Test Statistics");
    log(pre_stats);
    log("Post Test Statistics");
    log(post_stats);
}

/**
 * @param {Account} account 
 */
async function make_organizer(account){
    const session = await api.user.login(account.email, account.password);
    await api.organizer.convert_to_organizer_account(session);
}

/**
 * 
 * @param {number} count 
 * @returns {Promise<Account>[]}
 */
function generate_users(count){
    var users = []
    for(let i = 0; i < count; i++){
        const fut = (async _ => {
            const name = chance.name();
            let email = chance.email();
            const password = chance.string();
            for(var j = 0; j < 10; j ++){
                try{
                    await api.user.register(name, email, password);
                    break;
                }catch(e){}
                email = chance.email();
            }
            const session = await api.user.login(email, password); 
            return {
                name,
                email,
                password,
                session,
            }
        })();
        users.push(fut);
    }
    return users;
}

async function test_register_1(){
    await api.user.register("Yui", "yui@gmail.com", "password");
    const _ = await api.user.login("yui@gmail.com", "password");
    const yui_session = await api.user.login("yui@gmail.com", "password");
    const yui_sessions = await api.user.list_sessions(yui_session);
    assert(yui_sessions.length == 2);
    const yui_info = await api.user.all_userinfo(yui_session);
    assert(yui_info.admin == false);
    assert(yui_info.name == "Yui");
    assert(yui_info.email == "yui@gmail.com");
}

async function test_register_2(){
    await api.user.register("Leo", "leo@gmail.com", "supersecret");
    const leo_session = await api.user.login("leo@gmail.com", "supersecret");
    const leo_sessions = await api.user.list_sessions(leo_session);
    assert(leo_sessions.length == 1);
    const leo_info = await api.user.all_userinfo(leo_session);
    assert(leo_info.admin == false);
    assert(leo_info.name == "Leo");
    assert(leo_info.email == "leo@gmail.com");
}

async function test_default_admin_account(){
    const admin_session = await api.user.login("admin@localhost", "admin");
    const admin_info = await api.user.all_userinfo(admin_session);
    assert(admin_info.admin == true);
    await api.admin.get_server_logs(admin_session);
}

async function create_example_event(org_session, iteration){
    const picture_promise = (async () => await (await fetch(`https://picsum.photos/200?random=${iteration}`)).blob())();

    const event_id = await api.events.create_event(org_session);

    const categories = ["concert", "sports", "fundraiser"];
    const tags = ["fun", "thrilling", "outdoors", "indoors", "nature", "rock", "loud", "quiet", "cats", "dogs", "educational", "calm", "intense"];

    const category = categories[chance.integer({ min: 0, max: categories.length-1 })];
    await api.events.add_event_tag(event_id, category, true, org_session);

    for(let i = 0; i < tags.length; i ++){
        if(chance.bool())
            await api.events.add_event_tag(event_id, tags[i], false, org_session);
    }

    const data = {
        id: event_id,
        name: chance.sentence({ words: 5 }),
        description: chance.paragraph() ,
        metadata: {background: chance.color({format: 'hex'})},
    
        available_total_tickets: chance.bool()?undefined:chance.integer({ min: 50, max: 200 }),
        
        location_name: chance.address(),
        location_long: chance.longitude(),
        location_lat: chance.latitude()
    };
    await api.events.update_event(data, org_session);

    const picture =  await picture_promise;
    await api.events.set_picture(event_id, picture, org_session);


    // getting the event with a session that we own should work;
    await api.events.get_event(event_id, org_session);

    // not publically accessable
    // let failed = false;
    // try{
    //     await api.events.get_event(event_id);
    // }catch(e){
    //     failed = true;
    // }
    // assert(failed);

    await api.events.set_draft(event_id, false, org_session);

    // now that the event isn't a draft its public so anyone (even without an account) can view it
    const result = await api.events.get_event(event_id);

}
