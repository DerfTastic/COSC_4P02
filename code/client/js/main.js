console.log("Hello, World! From Javascript");


async function execute_sql(sql){
    const response = await fetch("/api/sql", {
       method: "POST",
       body: sql
    });
    return await response.text();  
}

var session = "";
const apiRoot = "/api";

async function api_call(route, data, error){
    var response;
    try{
        response = await fetch(`${apiRoot}${route}`, data);
    }catch(e){
        throw error;
    }

    if(response.ok){
        return response;
    }else if(response.status%100==400){
        throw await response.text();
    }else{
        throw error;
    }
}

async function login(email, password) {
    session = "";
    session = await (await api_call(
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
}

async function test(){
    return await (await api_call(
        `/test`,
        {
            method: 'GET',
            headers: { 'X-UserAPIToken': session }
        },
        "An error occured while testing sessions"
    )).json();
}

async function register(name, email, password) {
    await api_call(
        `/register`,
        {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, email, password })
        },
        "An error occured while registering"
    );
}

async function list_sessions() {
    return await (await api_call(
        `/list_sessions`,
        {
            method: 'GET',
            headers: { 'X-UserAPIToken': session }
        },
        "An error occured while fetching sessions"
    )).json();
}

async function invalidate_session(sessionId) {
    await api_call(
        `/invalidate_session/${sessionId}`,
        {
            method: 'DELETE',
            headers: { 'X-UserAPIToken': session }
        },
        "An error occured while invalidating session"
    );
}