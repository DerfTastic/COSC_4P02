function assert(condition, message) {
    if (!condition) {
        throw message || "Assertion failed";
    }
}

async function run_all(){    
    test_register_1();
    test_register_2();
    test_default_admin_account();
    create_example_event();
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

async function create_example_event(){
    await api.user.register("Organizer", "organizer@very.important.com", "verysecret");
    const org_session = await api.user.login("organizer@very.important.com", "verysecret");

    await api.organizer.convert_to_organizer_account(org_session);

    const event_id = await api.events.create_event(org_session);
    await api.events.add_event_tag(event_id, "concert", true, org_session);
    await api.events.add_event_tag(event_id, "large", false, org_session);
    await api.events.add_event_tag(event_id, "important", false, org_session);

    const data = {
        id: event_id,
        name: "EXTREME CONCERT",
        description: "INSANE RAVE TIME FUN CONCERT WEEE WOOOO BABABBBABAAAAAA!!!!!!!!!!!",
        metadata: {background: "red"}, // random data
        location_name: "Brock University",
        location_long: 43.119688153309305,
        location_lat: -79.2476323921286
    };
    await api.events.update_event(data, org_session);

    const picture =  await (await fetch("https://images.pexels.com/photos/1763075/pexels-photo-1763075.jpeg?cs=srgb&dl=pexels-sebastian-ervi-866902-1763075.jpg&fm=jpg")).blob();
    await api.events.set_picture(event_id, picture, org_session);


    // getting the event with a session that we own should work;
    await api.events.get_event(event_id, org_session);

    // not publically accessable
    let failed = false;
    try{
        await api.events.get_event(event_id);
    }catch(e){
        failed = true;
    }
    assert(failed);

    await api.events.set_draft(event_id, false, org_session);

    const result = await api.events.get_event(event_id);
    
    //TODO more testing
    assert(result.tags.length == 3);
}
