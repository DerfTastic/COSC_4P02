console.log("Hello, World! From Javascript");

//---------- TESTING/DEBUG ------------------- START

async function execute_sql(sql){
    const response = await fetch("/api/sql", {
       method: "POST",
       body: sql
    });
    return await response.text();  
}


async function test(){
    return await (await api_call(
        `/test`,
        {
            method: 'GET',
            headers: { 'X-UserAPIToken': getSession() }
        },
        "An error occured while testing sessions"
    )).json();
}

//---------- TESTING/DEBUG ------------------- END


const apiRoot = "/api";


//---------- SESSION MANAGEMENT ------------------- START

var currentSession = "";
//TODO make this cookie based?
function setSession(session){
    currentSession = session;
}

//TODO make this cookie based?
function getSession(){
    return currentSession;
}

//---------- SESSION MANAGEMENT ------------------- END



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


//---------- USER API ------------------- START

async function login(email, password) {
    setSession("");
    setSession(await (await api_call(
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
            headers: { 'X-UserAPIToken': getSession() }
        },
        "An error occured while fetching sessions"
    )).json();
}

async function invalidate_session(sessionId) {
    await api_call(
        `/invalidate_session/${sessionId}`,
        {
            method: 'DELETE',
            headers: { 'X-UserAPIToken': getSession() }
        },
        "An error occured while invalidating session"
    );
}

async function delete_account(email, password) {
    await api_call(
        `/delete_account/${sessionId}`,
        {
            method: 'DELETE',
            headers: { 'X-UserAPIToken': getSession() },
            body: JSON.stringify({ email, password })
        },
        "An error occured while invalidating session"
    );
}

//---------- USER API ------------------- END