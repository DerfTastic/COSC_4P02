function assert(condition, message) {
    if (!condition) {
        throw message || "Assertion failed";
    }
}

async function run_all(){    
    {
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
    
    {
        await api.user.register("Leo", "leo@gmail.com", "supersecret");
        const leo_session = await api.user.login("leo@gmail.com", "supersecret");
        const leo_sessions = await api.user.list_sessions(leo_session);
        assert(leo_sessions.length == 1);
        const leo_info = await api.user.all_userinfo(leo_session);
        assert(leo_info.admin == false);
        assert(leo_info.name == "Leo");
        assert(leo_info.email == "leo@gmail.com");
    }

    {
        const admin_session = await api.user.login("admin@localhost", "admin");
        const admin_info = await api.user.all_userinfo(admin_session);
        assert(admin_info.admin == true);
        await api.admin.get_server_logs(admin_info);
    }
}

