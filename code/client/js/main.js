console.log("Hello, World! From Javascript");


async function execute_sql(sql){
    const response = await fetch("/api/sql", {
       method: "POST",
       body: sql
    });
    return await response.text();  
}