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
    api_call: async function(route, data, error){
        var response;
        try{
            response = await fetch(`${apiRoot}${route}`, data);
        }catch(e){
            throw error;
        }
    
        if(response.ok){
            return response;
        }else if(response.status>=400&&response.status<500){
            throw await response.text();
        }else{
            throw error;
        }
    },

    test: {
        execute_sql: async function(sql){
            return await (await api.api_call(
                `/sql`,
                {
                    method: 'GET',
                },
                "An error occured while testing sessions"
            )).text();
        },

        test: async function(){
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
        login: async function(email, password) {
            cookies.deleteSessionToken();
            cookies.setSession(await (await api.api_call(
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
            )).text());
        },
        
        register: async function(name, email, password) {
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
        
        list_sessions: async function() {
            return await (await api.api_call(
                `/list_sessions`,
                {
                    method: 'GET',
                    headers: { 'X-UserAPIToken': cookies.getSession() }
                },
                "An error occured while fetching sessions"
            )).json();
        },
        
        invalidate_session: async function(sessionId) {
            await api.api_call(
                `/invalidate_session/${sessionId}`,
                {
                    method: 'DELETE',
                    headers: { 'X-UserAPIToken': cookies.getSession() }
                },
                "An error occured while invalidating session"
            );
        },
        
        delete_account: async function(email, password) {
            await api.api_call(
                `/delete_account/${sessionId}`,
                {
                    method: 'DELETE',
                    headers: { 'X-UserAPIToken': cookies.getSession() },
                    body: JSON.stringify({ email, password })
                },
                "An error occured while invalidating session"
            );
        },
    },
}


const cookies = {
    setSession: function(token, days = 30) {
        const expires = new Date();
        expires.setTime(expires.getTime() + days * 24 * 60 * 60 * 1000);
        document.cookie = `sessionToken=${encodeURIComponent(token)};expires=${expires.toUTCString()};path=/;Secure`;
    },
    
    getSession: function() {
        const cookies = document.cookie.split('; ');
        for (const cookie of cookies) {
            const [key, value] = cookie.split('=');
            if (key === 'sessionToken') {
            return decodeURIComponent(value);
            }
        }
        return "";
    },
    
    deleteSessionToken: function() {
        document.cookie = `sessionToken=;expires=Thu, 01 Jan 1970 00:00:00 UTC;path=/;Secure`;
    },
}



const page = {
    load_dynamic_content: async (item) => {
        item.querySelectorAll("[type='text/x-html-template']").forEach(async e => {
            const result = await fetch(e.getAttribute("src"));
            e.innerHTML = await result.text();

            page.initialize_content(e);
            page.load_dynamic_content(e);
        });
        
        item.querySelectorAll("template[type='text/x-handlebars-template']").forEach(async e => {
            const result = await eval(e.getAttribute("src"));
            var template = Handlebars.compile(e.innerHTML);
            var html = template(result);
            e.nextElementSibling.innerHTML = html;

            page.initialize_content(e);
            page.load_dynamic_content(e);
        });
    },

    initialize_content: (item) => {
        for(let e of item.querySelectorAll("template[type='text/x-handlebars-template']")){
            var template = Handlebars.compile(e.innerHTML);
            var html = template({});
            e.nextElementSibling.innerHTML = html;
        }

        for(let e of item.querySelectorAll("div[onclick]")){
            e.addEventListener("click", ev => {
                eval(e.getAttribute("onclick"));
            })
        }
    }
};

document.addEventListener('DOMContentLoaded', () => {
    page.initialize_content(document);
    page.load_dynamic_content(document);
});
