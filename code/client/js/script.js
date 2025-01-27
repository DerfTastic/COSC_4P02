//TODO move this somewhere?
function toggleMenu() {
    const hamburger = document.querySelector('.hamburger');
    const navLinks = document.querySelector('nav ul');
    if (hamburger && navLinks) {
        hamburger.classList.toggle('active');
        navLinks.classList.toggle('active');
    }
}

const apiRoot = "/api";

const api = {
    api_call: async function (route, data, error) {
        var response;
        try {
            response = await fetch(`${apiRoot}${route}`, data);
        } catch (e) {
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

    test: {
        execute_sql: async function (sql) {
            return await (await api.api_call(
                `/sql`,
                {
                    method: 'GET',
                },
                "An error occured while testing sessions"
            )).text();
        },

        test: async function () {
            return await (await api.api_call(
                `/test`,
                {
                    method: 'GET',
                    headers: { 'X-UserAPIToken': cookies.getSession() }
                },
                "An error occured while testing sessions"
            )).json();
        },
    },

    user: {
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
    setSession: function (token, days = 30) {
        const expires = new Date();
        expires.setTime(expires.getTime() + days * 24 * 60 * 60 * 1000);
        document.cookie = `sessionToken=${encodeURIComponent(token)};expires=${expires.toUTCString()};path=/;Secure`;
    },

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
        login: async function(email, password){
            try {
                cookies.deleteSessionToken();
                cookies.setSession(await api.user.login(email, password));
                window.location.href = '/account';
            } catch (e) {
                alert(e);
            }
        },
    },

    register: {
        register: async function(name, email, password){
            try {
                await api.user.register(name, email, password);
            } catch (e) {
                alert(e);
            }
        },
    },

    account: {
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

        invalidate_session: async function (sessionId) {
            try {
                return await api.user.invalidate_session(sessionId, cookies.getSession());
            } catch ({ error, code }) {
                if (code == 401) {
                    utility.logout();
                } else {
                    alert(error);
                }
            }
        },

        test: async function(){
            try {
                return await api.test.test(cookies.getSession());
            } catch ({ error, code }) {
                if (code == 401) {
                    utility.logout();
                } else {
                    alert(error);
                }
            }
        },

        delete_account: async function(email, password) {
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

    check_for_handlers: function (){
        if(this.awaiting_handlebar_templates==0&&this.awaiting_html_templates==0
            ||this.awaiting_handlebar_templates==0&&this.awaiting_html_templates==-1
            ||this.awaiting_handlebar_templates==-1&&this.awaiting_html_templates==0
        ){
            document.dispatchEvent(new Event("dynamic_content_finished"));
        }
        if(this.awaiting_handlebar_templates==0){
            this.awaiting_handlebar_templates=-1;
            document.dispatchEvent(new Event("handlebar_templates_finished"))
        }
        if(this.awaiting_html_templates==0){
            this.awaiting_html_templates=-1;
            document.dispatchEvent(new Event("html_templates_finished"))
        }
    },

    load_dynamic_content: function (item) {
        item.querySelectorAll("[type='text/x-html-template']").forEach(e => {
            this.awaiting_html_templates++;
            (async _ => {
                const result = await fetch(e.getAttribute("src"));
                e.innerHTML = await result.text();
                this.awaiting_html_templates--;
    
                page.initialize_content(e);
                page.load_dynamic_content(e);
            })();
        });
        item.querySelectorAll("template[type='text/x-handlebars-template']").forEach(e => {
            this.awaiting_handlebar_templates++;
            (async _ => {
                const result = await eval(e.getAttribute("src"));
                var template = Handlebars.compile(e.innerHTML);
                var html = template(result);
                e.nextElementSibling.innerHTML = html;
    
                this.awaiting_handlebar_templates--;
                page.initialize_content(e);
                page.load_dynamic_content(e);
            })();
        });   
        page.check_for_handlers();     
    },

    initialize_content: (item) => {
        for (let e of item.querySelectorAll("template[type='text/x-handlebars-template']")) {
            var template = Handlebars.compile(e.innerHTML);
            var html = template({});
            e.nextElementSibling.innerHTML = html;
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
    document.body.style="";
});

document.addEventListener('DOMContentLoaded', () => {
    document.body.style="display:none";
    page.initialize_content(document);
    page.load_dynamic_content(document);
});

