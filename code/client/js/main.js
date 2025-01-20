console.log("Hello, World! From Javascript");


async function execute_sql(sql){
    const response = await fetch("/api/sql", {
       method: "POST",
       body: sql
    });
    return await response.text();  
}

var session = "";

async function login(email, password) {
    const loginData = {
        email: email,
        password: password
    };

    try {
        const response = await fetch('/api/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(loginData)
        });

        if (response.ok) {
            const result = await response.text();
            session = result;
            console.log('Login successful:', result);
            return result;
        } else {
            session = "";
            const error = await response.text();
            console.error('Login failed:', error);
            throw new Error(`Login failed: ${error}`);
        }
    } catch (err) {
        session = "";
        console.error('An error occurred while logging in:', err);
        throw err;
    }
}

async function test(){
    const response = await fetch('/api/test', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-UserAPIToken': session,
        }
    });

    if (response.ok) {
        const result = await response.json();
        return result;
    } else {
        const error = await response.text();
        console.error('Login failed:', error);
    }
}