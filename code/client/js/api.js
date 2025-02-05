const apiRoot = "/api";

class AllUserInfo {
    /** @type{number} */id
    /** @type{string} */name
    /** @type{string} */email
    /** @type{string} */bio
    /** @type{boolean} */admin
    /** @type{boolean} */has_analytics
}

class Log {
    /** @type{string} */level_s
    /** @type{number} */level_i
    /** @type{string} */message
    /** @type{number} */millis
    /** @type{number} */sequenceNumber
    /** @type{string} */sourceClassName
    /** @type{string} */sourceMethodName
    /** @type{string} */thrown
}

class OrganizerEventTag {
    /** @type{string} */tag
    /** @type{boolean} */category
}

class EventTicket {
    /** @type{number} */id
    /** @type{number} */event_id
    /** @type{string} */name
    /** @type{number} */price
}

class OrganizerEvent {
    /** @type{number} */id
    /** @type{number} */organizer_id
    /** @type{string} */name
    /** @type{string} */description
    /** @type{number} */picture
    /** @type{object} */metadata
    /** @type{boolean} */draft

    /** @type{number} */available_total_tickets

    /** @type{string} */location_name
    /** @type{number} */location_lat
    /** @type{number} */location_long
}

class AllOrganizerEvent{
    /** @type{OrganizerEvent} */ event
    /** @type{OrganizerEventTag[]} */ tags
    
}

class UpdateOrganizerEvent{
    /** @type{number} */id
    /** @type{string} */name
    /** @type{string} */description
    /** @type{object} */metadata
    
    /** @type{number} */available_total_tickets

    /** @type{string} */location_name
    /** @type{number} */location_lat
    /** @type{number} */location_long
}

class RouteStat{
    /** @type{number} */average_response_time
    /** @type{number} */requests_handled
    /** @type{Object.<number, number>} */code_breakdown
}

class ServerStatistics{
    /** @type{Object.<string, RouteStat>} */route_stats
    /** @type{number} */total_requests_handled
    /** @type{number} */prepared_statements_executed
    /** @type{number} */total_db_statements_executed
}

class Search{
    /** @type{number} */ date_start
    /** @type{number} */ date_end
    /** @type{number} */ max_duration
    /** @type{number} */ min_duration
    /** @type{OrganizerEventTag[]} */ tags
    /** @type{string} */ organizer_fuzzy
    /** @type{string} */ name_fizzy
    /** @type{number} */ organizer_exact
    /** @type{number} */ distance
    /** @type{number} */ location_lat
    /** @type{number} */ location_long
    /** @type{string} */ location
    /** @type{number} */ offset
    /** @type{number} */ limit
}

// actually just a string but shhhh
/** @type{string} */
class Session { }

const api = {
    /**
     * @param {string} route 
     * @param {RequestInit} data 
     * @param {string} error 
     * @returns {Promise<Response>}
     */
    api_call: async function (route, data, error) {
        var response;
        try {
            response = await fetch(`${apiRoot}${route}`, data);
        } catch (e) {
            console.log(e);
            throw { error, code: -1 };
        }

        if (response.ok) {
            return response;
        } else if (response.status >= 400 && response.status < 500) {
            throw { error: await response.text(), code: response.status };
        } else {
            throw { error, code: response.status };
        }
    },

    admin: {
        /**
         * @param {string} sql 
         * @param {Session} session 
         * @returns {Promise<string>}
         */
        execute_sql: async function (sql, session) {
            return await (await api.api_call(
                `/execute_sql`,
                {
                    method: 'POST',
                    headers: {
                        'X-UserAPIToken': session
                    },
                    body: sql,
                },
                "An error occured while executing sql"
            )).text();
        },

        /**
         * @param {Session} session 
         * @returns {Promise<Log[]>}
         */
        get_server_logs: async function (session) {
            return await (await api.api_call(
                `/get_server_logs`,
                {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                },
                "An error occured while getting the server logs"
            )).json();
        },
        
        /**
         * @param {Session} session 
         * @returns {Promise<ServerStatistics>}
         */
        get_server_statistics: async function(session) {
            return await (await api.api_call(
                `/get_server_statistics`,
                {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                },
                "An error occured while getting the route statistics"
            )).json();
        },
        
        /**
         * @param {string} email 
         * @param {Session} session 
         * @returns {Promise<>}
         */
        delete_other_account: async function(email, session) {
            await api.api_call(
                `/delete_other_account/${encodeURI(email)}`,
                {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                },
                "An error occured while deleting that account"
            )
        },

        /**
         * @param {boolean} admin 
         * @param {string} email 
         * @param {Session} session 
         * @returns {Promise<>}
         */
        set_account_admin: async function(admin, email, session) {
            await api.api_call(
                `/set_account_admin/${encodeURI(admin)}/${encodeURI(email)}`,
                {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                },
                "An error occured while setting that account as admin"
            )
        },

        /**
         * @param {string} level 
         * @param {Session} session 
         * @returns {Promise<>}
         */
        set_log_level: async function(level, session) {
            await api.api_call(
                `/set_log_level/${encodeURI(level)}`,
                {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                },
                "An error occured while setting the log level"
            )
        },

        /**
         * @param {Session} session 
         * @returns {Promise<string>}
         */
        get_log_level: async function(session) {
            return await (await api.api_call(
                `/get_log_level`,
                {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                },
                "An error occured while getting the log level"
            )).text();
        },

        /**
         * @param {Session} session 
         * @returns {Promise<string[]>}
         */
        get_log_levels: async function(session) {
            return await (await api.api_call(
                `/get_log_levels`,
                {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                },
                "An error occured while getting the log levels"
            )).json();
        }
    },

    events: {
        /**
         * @param {Session} session 
         * @returns {Promise<number>}
         */
        create_event: async function (session) {
            return await (await api.api_call(
                '/create_event',
                {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    }
                },
                "An error occured while creating event"
            )).json();
        },

        /**
         * @param {number|string} id 
         * @param {Session?} session 
         * @returns {Promise<AllOrganizerEvent>}
         */
        get_event: async function(id, session){
            return await (await api.api_call(
                `/get_event/${encodeURI(id)}`,
                {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    }
                },
                "An error occured while getting event"
            )).json();
        },

        /**
         * @param {UpdateOrganizerEvent} update 
         * @param {Session} session 
         * @returns {Promise<>}
         */
        update_event: async function(update, session){
            await api.api_call(
                '/update_event',
                {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                    body: JSON.stringify(update),
                },
                "An error occured while updating event"
            );
        },

        /**
         * @param {number|string} id 
         * @param {Session} session 
         * @returns {Promise<>}
         */
        delete_event: async function(id, session){
            await api.api_call(
                `/delete_event/${encodeURI(id)}`,
                {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    }
                },
                "An error occured while deleting event"
            );
        },

        /**
         * @param {number|string} id 
         * @param {string} tag 
         * @param {boolean} category 
         * @param {Session} session 
         * @returns {Promise<>}
         */
        add_event_tag: async function(id, tag, category, session){
            await api.api_call(
                `/add_event_tag/${encodeURI(id)}/${encodeURI(tag)}/${encodeURI(category)}`,
                {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    }
                },
                "An error occured while adding event tag"
            );
        },

        /**
         * @param {number|string} id 
         * @param {string} tag 
         * @param {boolean} category 
         * @param {Session} session 
         * @returns {Promise<>}
         */
        delete_event_tag: async function(id, tag, category, session){
            await api.api_call(
                `/delete_event_tag/${encodeURI(id)}/${encodeURI(tag)}/${encodeURI(category)}`,
                {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    }
                },
                "An error occured while deleting event tag"
            );
        },

        /**
         * @param {number|string} id 
         * @param {boolean} draft 
         * @param {Session} session 
         * @returns {Promise<>}
         */
        set_draft: async function(id, draft, session){
            await api.api_call(
                `/set_draft/${encodeURI(id)}/${encodeURI(draft)}`,
                {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    }
                },
                "An error occured while setting event draft"
            );
        },

        /**
         * @param {number|string} id 
         * @param {Blob} data 
         * @param {Session} session 
         * @returns {Promise<>}
         */
        set_picture: async function(id, data, session){
            await api.api_call(
                `/set_picture/${encodeURI(id)}/`,
                {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                    body: data,
                },
                "An error occured while setting event picture"
            );
        }
    },

    /**
     * @param {Search} search 
     * @param {Session} session 
     * @returns {Promise<AllOrganizerEvent[]>}
     */
    search: {
        search_events: async function(search, session){
            return await (await api.api_call(
                '/search_events',
                {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                    body: JSON.stringify(search),
                },
                "An error occured while searching"
            )).json();
        }
    },

    organizer: {
        /**
         * @param {Session} session 
         * @returns {Promise<>}
         */
        convert_to_organizer_account: async function (session) {
            await api.api_call(
                '/convert_to_organizer_account',
                {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    }
                },
                "An error occured while converting account to organizer"
            );
        }
    },

    user: {
        /**
         * @param {Session} session 
         * @returns {Promise<AllUserInfo>}
         */
        all_userinfo: async function (session) {
            return await (await api.api_call(
                `/all_userinfo`,
                {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    }
                },
                "An error occured while fetching userinfo"
            )).json();
        },

        /**
         * @param {string} email 
         * @param {string} password 
         * @returns {Promise<Session>}
         */
        login: async function (email, password) {
            return await (await api.api_call(
                `/login`,
                {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        email: email,
                        password: password
                    })
                },
                "An error occured while loggin in"
            )).text();
        },

        /**
         * @param {string} name 
         * @param {string} email 
         * @param {string} password 
         * @returns {Promise}
         */
        register: async function (name, email, password) {
            await api.api_call(
                `/register`,
                {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ name, email, password })
                },
                "An error occured while registering"
            );
        },

        /**
         * @param {Session} session 
         * @returns {Promise<Session[]>}
         */
        list_sessions: async function (session) {
            return await (await api.api_call(
                `/list_sessions`,
                {
                    method: 'GET',
                    headers: { 'X-UserAPIToken': session }
                },
                "An error occured while fetching sessions"
            )).json();
        },

        /**
         * @param {number|string} sessionId 
         * @param {Session} session 
         * @returns {Promise}
         */
        invalidate_session: async function (sessionId, session) {
            await api.api_call(
                `/invalidate_session/${sessionId}`,
                {
                    method: 'DELETE',
                    headers: { 'X-UserAPIToken': session }
                },
                "An error occured while invalidating session"
            );
        },

        /**
         * @param {string} email 
         * @param {string} password 
         * @param {Session} session 
         * @returns {Promise}
         */
        delete_account: async function (email, password, session) {
            await api.api_call(
                `/delete_account/${sessionId}`,
                {
                    method: 'DELETE',
                    headers: { 'X-UserAPIToken': session },
                    body: JSON.stringify({ email, password })
                },
                "An error occured while invalidating session"
            );
        },
    },
}


const cookies = {
    /**
     * 
     * @param {string} token 
     * @param {number} days
     */
    setSession: function (token, days = 30) {
        const expires = new Date();
        expires.setTime(expires.getTime() + days * 24 * 60 * 60 * 1000);
        document.cookie = `sessionToken=${encodeURIComponent(token)};expires=${expires.toUTCString()};path=/;Secure`;
    },

    /**
     * @returns {Session}
     */
    getSession: function () {
        const cookies = document.cookie.split('; ');
        for (const cookie of cookies) {
            const [key, value] = cookie.split('=');
            if (key === 'sessionToken') {
                return decodeURIComponent(value);
            }
        }
        return "";
    },

    deleteSessionToken: function () {
        document.cookie = `sessionToken=;expires=Thu, 01 Jan 1970 00:00:00 UTC;path=/;Secure`;
    },
}

const utility = {
    logout: function () {
        cookies.deleteSessionToken();
        window.location.href = '/login';
    },
    /**
     * @param {number|string} id 
     * @returns {boolean}
     */
    is_session_id_current: function (id) {
        if (cookies.getSession() == null || cookies.getSession().length == 0) return false;
        const curr_id = cookies.getSession().substring(cookies.getSession().length - 8, cookies.getSession().length);
        return parseInt(curr_id, 16) == id;
    },
    require_logged_in: function () {
        if (cookies.getSession() == null || cookies.getSession().length == 0) {
            window.location.href = "/login";
        }
    }

}


const page = {

    login: {
        /**
         * @param {string} email 
         * @param {string} password 
         * @returns {Promise}
         */
        login: async function (email, password) {
            try {
                cookies.deleteSessionToken();
                cookies.setSession(await api.user.login(email, password));
                window.location.href = '/account';
            } catch ({ error, code }) {
                alert(error);
            }
        },
    },

    register: {
        /**
         * @param {string} name 
         * @param {string} email 
         * @param {string} password 
         * @returns {Promise}
         */
        register: async function (name, email, password) {
            try {
                await api.user.register(name, email, password);
            } catch ({ error, code }) {
                alert(error);
            }
        },
    },

    account: {
        /**
         * @returns {Promise<AllUserInfo>}
         */
        all_userinfo: async function () {
            try {
                return await api.user.all_userinfo(cookies.getSession());
            } catch ({ error, code }) {
                if (code == 401) {
                    utility.logout();
                } else {
                    alert(error);
                }
            }
        },

        /**
         * @param {Element}
         * @param {number|string} id 
         * @returns {Promise}
         */
        remove_session: async function (element, id) {
            try {
                await api.user.invalidate_session(id, cookies.getSession());
                element.parentElement.outerHTML = "";
                if (utility.is_session_id_current(id)) {
                    utility.logout();
                }
            } catch (e) {
                alert(e);
            }
        },

        /**
         * @returns {Promise<Session[]>}
         */
        list_sessions: async function () {
            try {
                return await api.user.list_sessions(cookies.getSession());
            } catch ({ error, code }) {
                if (code == 401) {
                    utility.logout();
                } else {
                    alert(error);
                }
            }
        },

        /**
         * @param {number|string} sessionId 
         * @returns {Promise}
         */
        invalidate_session: async function (sessionId) {
            try {
                await api.user.invalidate_session(sessionId, cookies.getSession());
            } catch ({ error, code }) {
                if (code == 401) {
                    utility.logout();
                } else {
                    alert(error);
                }
            }
        },

        /**
         * @param {string} email 
         * @param {string} password 
         * @returns {Promise}
         */
        delete_account: async function (email, password) {
            try {
                await api.user.delete_account(email, password, cookies.getSession());
            } catch ({ error, code }) {
                if (code == 401) {
                    utility.logout();
                } else {
                    alert(error);
                }
            }
        },
    },


    awaiting_handlebar_templates: 0,
    awaiting_html_templates: 0,

    check_for_handlers: function () {
        if (this.awaiting_handlebar_templates == 0 && this.awaiting_html_templates == 0
            || this.awaiting_handlebar_templates == 0 && this.awaiting_html_templates == -1
            || this.awaiting_handlebar_templates == -1 && this.awaiting_html_templates == 0
        ) {
            document.dispatchEvent(new Event("dynamic_content_finished"));
        }
        if (this.awaiting_handlebar_templates == 0) {
            this.awaiting_handlebar_templates = -1;
            document.dispatchEvent(new Event("handlebar_templates_finished"))
        }
        if (this.awaiting_html_templates == 0) {
            this.awaiting_html_templates = -1;
            document.dispatchEvent(new Event("html_templates_finished"))
        }
    },

    /**
     * @param {Element} item 
     */
    load_dynamic_content: function (item) {
        if (item == null) return;
        for (let e of item.querySelectorAll("[type='text/x-html-template']")) {
            this.awaiting_html_templates++;
            (async _ => {
                try {
                    const result = await fetch(e.getAttribute("src"));
                    e.innerHTML = await result.text();
                } catch (err) {
                    e.innerHTML = JSON.stringify(err);
                }

                page.initialize_content(e.nextElementSibling);
                page.load_dynamic_content(e.nextElementSibling);
                this.awaiting_html_templates--;
                page.check_for_handlers();
            })();
        }
        for (let e of item.querySelectorAll("template[type='text/x-handlebars-template']")) {
            this.awaiting_handlebar_templates++;
            (async _ => {
                try {
                    const result = await eval(e.getAttribute("src"));
                    var template = Handlebars.compile(e.innerHTML);
                    var html = template(result);
                    e.nextElementSibling.innerHTML = html;
                } catch (err) {
                    e.nextElementSibling.innerHTML = JSON.stringify(err);
                }

                page.initialize_content(e.nextElementSibling);
                page.load_dynamic_content(e.nextElementSibling);
                this.awaiting_handlebar_templates--;
                page.check_for_handlers();
            })();
        }
        page.check_for_handlers();
    },

    /**
     * @param {Element} item 
     */
    initialize_content: (item) => {
        if (item == null) return;
        for (let e of item.querySelectorAll("template[type='text/x-handlebars-template']")) {
            try {
                var template = Handlebars.compile(e.innerHTML);
                var html = template({});
                e.nextElementSibling.innerHTML = html;
            } catch (err) {
                e.nextElementSibling.innerHTML = JSON.stringify(err);
            }
        }

        for (let e of item.querySelectorAll("div[onclick]")) {
            e.addEventListener("click", ev => {
                eval(e.getAttribute("onclick"));
            })
        }
    }
};


document.addEventListener('handlebar_templates_finished', () => {
    console.log("Handlebar templates finished loading");
});

document.addEventListener('html_templates_finished', () => {
    console.log("HTML templates finished loading");
});

document.addEventListener('dynamic_content_finished', () => {
    console.log("Dynamic content finished loading");
    document.body.style = "";
});
