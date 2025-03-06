document.addEventListener("DOMContentLoaded", function () {
    const eventsContainer = document.getElementById("eventsContainer");
    const loadMoreButton = document.getElementById("loadMore");
    const filterForm = document.getElementById("filterForm");
    
    let events = [
        { title: "Live Concert", location: "New York", category: "Concert", price: 50, date: "2024-06-15" },
        { title: "Broadway Show", location: "Los Angeles", category: "Theater", price: 75, date: "2024-07-20" },
        { title: "Comedy Night", location: "Chicago", category: "Comedy", price: 30, date: "2024-08-10" },
        { title: "Food Festival", location: "San Francisco", category: "Festival", price: 20, date: "2024-09-05" },
        { title: "Tech Conference", location: "Seattle", category: "Conference", price: 100, date: "2024-10-12" }
    ];

    let displayedEvents = 0;
    const eventsPerPage = 10;
    let filteredEvents = [...events];

    function renderEvents(eventList) {
        eventsContainer.innerHTML = "";
        eventList.forEach(event => {
            const eventBox = document.createElement("div");
            eventBox.classList.add("event-box");
            eventBox.innerHTML = `
                <svg width="400" height="200" viewBox="0 0 400 200" xmlns="http://www.w3.org/2000/svg">
                    <path d="M0,40 Q20,40 20,20 H380 Q380,40 400,40 V80 Q380,80 380,100 Q380,120 400,120 V160 Q380,160 380,180 H20 Q20,160 0,160 V120 Q20,120 20,100 Q20,80 0,80 Z" 
                          fill="white" stroke="#ccc" stroke-width="2"/>
                    <text x="40" y="50" font-size="22" font-family="Arial, sans-serif" font-weight="bold" fill="#333">${event.title}</text>
                    <text x="40" y="80" font-size="16" font-family="Arial, sans-serif" fill="#555">Date: ${event.date}</text>
                    <text x="40" y="110" font-size="16" font-family="Arial, sans-serif" fill="#555">Location: ${event.location}</text>
                    <text x="40" y="140" font-size="16" font-family="Arial, sans-serif" fill="#555">Category: ${event.category}</text>
                    <text x="40" y="170" font-size="16" font-family="Arial, sans-serif" fill="#555">Price: $${event.price}</text>
                    <circle cx="200" cy="45" r="2" fill="#bbb"/>
                    <circle cx="200" cy="65" r="2" fill="#bbb"/>
                    <circle cx="200" cy="85" r="2" fill="#bbb"/>
                    <circle cx="200" cy="105" r="2" fill="#bbb"/>
                    <circle cx="200" cy="125" r="2" fill="#bbb"/>
                    <circle cx="200" cy="145" r="2" fill="#bbb"/>
                    <circle cx="200" cy="165" r="2" fill="#bbb"/>
                </svg>
            `;
            eventsContainer.appendChild(eventBox);
        });
    }

    function loadMoreEvents() {
        const newEvents = filteredEvents.slice(0, displayedEvents + eventsPerPage);
        renderEvents(newEvents);
        displayedEvents = newEvents.length;
        loadMoreButton.style.display = displayedEvents >= filteredEvents.length ? "none" : "block";
    }

    filterForm.addEventListener("input", function () {
        loadMoreEvents();
    });

    loadMoreButton.addEventListener("click", loadMoreEvents);
    loadMoreEvents();
});
