function assert(condition, message) {
    if (!condition) {
        throw message || "Assertion failed";
    }
}

class Account{
    /** @type{string} */ email
    /** @type{string} */ password
    
    /** @type{string[]} */ sessions = []

    /** @type{integer[]} */ events = []
}

/** @type{Account[]} */
const admin_accounts = [];
/** @type{Account[]} */
const organizer_accounts = [];
/** @type{Array<Account>} */
const accounts = [];

/**
 * @type{(function (): Promis<void>)[]}
 */
const meows = [];

let api_calls = 0;
/**
 * @template T
 * @param {T[]} param 
 * @param {number} index 
 * @returns {T}
 */
function remove(param, index){
    return param.splice(index, 1)[0];
};

/**
 * @template T
 * @param {T[]} param  
 * @returns {T}
 */
function removeRandom(param){
    if(param.length==0)return null;
    return remove(param, Math.floor(Math.random()*param.length));
}

/**
 * @param {T[]} param  
 * @returns {T}
 * @template T
 */
function getRandom(param){
    if(param.length==0)return null;
    const index = Math.floor(Math.random()*param.length);
    return param[index];
}

/**
 * @param {T[]} param  
 * @param {function(T): Promise<R>} cb
 * @template T
 * @template R
 * @returns R
 */
async function withRandom(param, cb){
    var random = removeRandom(param);
    if(random===null)return;
    const ret = await cb(random);
    param.push(random);
    return ret;
}

/**
 * @param {Account[]} list
 * @param {function(Account, string): Promise} cb 
 * @returns {Promise}
 */
async function withRandomSession(list, cb){
    await withRandom(list, async account => {
        await withRandom(account.sessions, async session => {
            await cb(account, session);
        });
    });
}


async function withRandomAccountSession(cb){
    withRandomSession(accounts, cb);
}

async function withRandomOrganizerSession(cb){
    withRandomSession(organizer_accounts, cb);
}

async function withRandomAdminSession(cb){
    withRandomSession(admin_accounts, cb);
}

meows.push(async function() {
    let account = getRandom(accounts);
    if(account==null)return;
    account.sessions.push(await api.user.login(account.email, account.password));
    api_calls += 1;
});
meows.push(async function() {
    let account = getRandom(admin_accounts);
    if(account==null)return;
    account.sessions.push(await api.user.login(account.email, account.password));
    api_calls += 1;
});
meows.push(async function() {
    let account = getRandom(organizer_accounts);
    if(account==null)return;
    account.sessions.push(await api.user.login(account.email, account.password));
    api_calls += 1;
});

meows.push(async function() {
    while(true){
        let account = getRandom(getRandom([accounts, admin_accounts, organizer_accounts]));
        if(account==null)return;
        while(account.sessions.length > 1){
            var session = removeRandom(account.sessions);
            var id = utility.get_id_from_session(session);
            await api.user.invalidate_session(id, session);
            api_calls += 1;
        }   
    }
});

meows.push(async function() {
    const account = new Account();
    account.name = chance.name();
    account.email = chance.email();
    account.password = chance.string();
    account.events = [];
    account.sessions = [];
    for(var j = 0; j < 10; j ++){
        try{
            await api.user.register(account.name, account.email, account.password);
            break;
        }catch(e){}
        email = chance.email();
        api_calls += 1;
    }
    account.sessions.push(await api.user.login(account.email, account.password)); 
    accounts.push(account);
    api_calls += 1;
});

meows.push(async function(){
    const account = getRandom(getRandom([accounts, admin_accounts, organizer_accounts]));
    if(account==null)return;
    await withRandom(account.sessions, async session => {
        await api.user.list_sessions(session);    
        api_calls += 1;
    });
});

meows.push(async function(){
    const list = getRandom([accounts, admin_accounts, organizer_accounts]);
    if(list.length<=1)return;
    const account = removeRandom(list);
    if(account==null)return;
    if(account.email==="admin@localhost"){
        list.push(account);
        return;
    }
    const session = getRandom(account.sessions);
    await api.user.delete_account(account.email, account.password, session);
    api_calls += 1;
});

meows.push(async function(){
    const account = getRandom(getRandom([accounts, admin_accounts, organizer_accounts]));
    if(account==null)return;
    const session = getRandom(account.sessions);
    await api.user.all_userinfo(session);
    api_calls += 1;
});

// organizer stuff
meows.push(async function(){
    const account = removeRandom(accounts);
    if(account==null)return;
    const session = getRandom(account.sessions);
    await api.organizer.convert_to_organizer_account(session);
    organizer_accounts.push(account);
    api_calls += 1;
});

// search
meows.push(async function(){
    const account = getRandom(organizer_accounts);
    if(account==null)return;
    const session = getRandom(account.sessions);
    const search = new Search();
    search.owning = chance.bool();
    search.draft = chance.bool();
    for(let i = 0; i < 4 && chance.bool(); i ++){
        search.tags.push({
            category: chance.bool(),
            tag: getRandom(["loud", "edm"])
        })
    }
    search.location_lat = chance.longitude();
    search.location_long = chance.latitude();
    if(chance.bool())
        search.date_end = chance.timestamp()*1000;
    if(chance.bool())
        search.date_start = chance.timestamp()*1000;

    if(chance.bool())
        search.max_duration = chance.integer({min: 15*60*1000, min: 12*60*60*1000});
    if(chance.bool())
        search.min_duration = chance.integer({min: 15*60*1000, min: 12*60*60*1000});
    
    if(Math.random()<.2)
        search.min_duration = chance.integer({min: 1, min: 500});

    if(Math.random()<.2)
        search.organizer_fuzzy = `%${chance.character()}%`;

    if(Math.random()<.2)
        search.name_fuzzy = `%${chance.character()}%`;

    if(Math.random()<.2)
        search.location = `%${chance.character()}%`;

    if(Math.random()<.1)
        search.distance = Math.random()*10000-5000;

    if(chance.bool()){
        search.sort_by = getRandom(["MinPrice", "MaxPrice", "TicketsAvailable", "Closest", "StartTime", "MinDuration", "MaxDuration"]);
    }
    
    await api.search.search_events(search, session);
    api_calls += 1;
});

// events
meows.push(async function(){
    //TODO set picture
});

const tags = ["bannana", "meow", "nya", "bwaaaaaa", "edm", "loud", "test"];

meows.push(async function(){
    const account = getRandom(organizer_accounts);
    if(account==null)return;
    const session = getRandom(account.sessions);
    const event = getRandom(account.events);
    if(event==null)return;
    await api.events.add_event_tag(event, getRandom(tags), chance.bool(), session);
    api_calls += 1;
});

meows.push(async function(){
    const account = getRandom(organizer_accounts);
    if(account==null)return;
    const session = getRandom(account.sessions);
    const event = getRandom(account.events);
    if(event==null)return;
    await api.events.delete_event_tag(event, getRandom(tags), chance.bool(), session);
    api_calls += 1;
});

meows.push(async function(){
    const account = getRandom(organizer_accounts);
    if(account==null)return;
    const session = getRandom(account.sessions);
    const event = getRandom(account.events);
    if(event==null)return;
    await api.events.set_draft(event, chance.bool(), session);
    api_calls += 1;
});

meows.push(async function(){
    const account = getRandom(organizer_accounts);
    if(account==null)return;
    const session = getRandom(account.sessions);
    const event = removeRandom(account.events);
    if(event==null)return;
    await api.events.delete_event(event, session);
    api_calls += 1;
});

meows.push(async function(){
    const account = getRandom(organizer_accounts);
    if(account==null)return;
    const session = getRandom(account.sessions);
    const event_id = getRandom(account.events);
    if(event_id==null)return;
    const event = new UpdateOrganizerEvent();
    event.id = event_id;

    event.name = chance.sentence({ words: 5 });
    event.description = chance.paragraph();
    event.metadata = {background: chance.color({format: 'hex'})};

    event.available_total_tickets = chance.bool()?undefined:chance.integer({ min: 50, max: 200 });
    
    event.location_name = chance.address();
    event.location_long = chance.longitude();
    event.location_lat = chance.latitude();

    event.start = chance.bool()?undefined:chance.timestamp()*1000;
    event.duration = chance.bool()?undefined:chance.integer({min: 15*60*1000, min: 12*60*60*1000});

    await api.events.update_event(event, session);
    api_calls += 1;
});

meows.push(async function(){
    const account = getRandom(organizer_accounts);
    if(account==null)return;
    const session = getRandom(account.sessions);
    const event = await api.events.create_event(session);
    account.events.push(event);
    api_calls += 1;
});

meows.push(async function(){
    const account = getRandom(organizer_accounts);
    if(account==null)return;
    const session = getRandom(account.sessions);
    const event = getRandom(account.events);
    if(event==null)return;
    await api.events.get_event(event, session);
    api_calls += 1;
});

// admin
meows.push(async function(){
    const account = getRandom(admin_accounts);
    const session = getRandom(account.sessions);
    await api.admin.get_log_level(session);
    api_calls += 1;
});

meows.push(async function(){
    const account = getRandom(admin_accounts);
    const session = getRandom(account.sessions);
    await api.admin.get_log_level(session);
    api_calls += 1;
});

meows.push(async function(){
    const account = getRandom(admin_accounts);
    const session = getRandom(account.sessions);
    var level = await api.admin.get_log_level(session);
    await api.admin.set_log_level(level, session);
    api_calls += 2;
});

meows.push(async function(){
    const account = getRandom(admin_accounts);
    const session = getRandom(account.sessions);
    await api.admin.get_server_statistics(session);
    api_calls += 1;
});

meows.push(async function(){
    const account = getRandom(admin_accounts);
    const session = getRandom(account.sessions);
    await api.admin.get_server_logs(session);
    api_calls += 1;
});

meows.push(async function(){
    const admin = getRandom(admin_accounts);
    if(accounts.length<=1)return;
    const account = removeRandom(accounts);
    const session = getRandom(admin.sessions);
    await api.admin.set_account_admin(true, account.email, session);
    admin_accounts.push(account);
    api_calls += 1;
});

meows.push(async function(){
    if(admin_accounts.length<=1)return;
    const account = removeRandom(admin_accounts);
    if(account.email==="admin@localhost"){
        admin_accounts.push(account);
        return;
    }
    const session = getRandom(account.sessions);
    await api.admin.set_account_admin(false, account.email, session);
    accounts.push(account);
    api_calls += 1;
});

meows.push(async function(){
    const list = getRandom([accounts, admin_accounts, organizer_accounts]);
    if(list.length<=1)return;
    const account = removeRandom(list);
    if(account.email==="admin@localhost"){
        list.push(account);
        return;
    }
    const admin = getRandom(admin_accounts);
    const session = getRandom(admin.sessions);
    await api.admin.delete_other_account(account.email, session);
    accounts.push(account);
    api_calls += 1;
});


function stuffy(){
    for(let i = 0; i < 100; i ++){
        (async() => {while(true)try{await getRandom(meows)()}catch(e){}})();
    }
} 


async function start(){
    const account = new Account();
    account.email = "admin@localhost";
    account.password = "admin";
    account.events = [];
    account.sessions = [];
    account.sessions.push(await api.user.login(account.email, account.password));
    admin_accounts.push(account);
    api_calls += 1;

    stuffy();
}

start();