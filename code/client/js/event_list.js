document.addEventListener("DOMContentLoaded", function () {
    const eventsContainer = document.getElementById("eventsContainer");
    const loadMoreButton = document.getElementById("loadMore");
    const filterForm = document.getElementById("filterForm");
    
    let events = [
        { title: "Live Concert", location: "New York", category: "Concert", tags: ["Music", "Live"], price: 50, date: "2024-06-15" },
        { title: "Broadway Show", location: "Los Angeles", category: "Theater", tags: ["Drama", "Stage"], price: 75, date: "2024-07-20" },
        { title: "Comedy Night", location: "Chicago", category: "Comedy", tags: ["Stand-up", "Humor"], price: 30, date: "2024-08-10" }
    ];

    let displayedEvents = 0;
    const eventsPerPage = 10;
    let filteredEvents = [...events];

    function renderEvents(eventList) {
        eventsContainer.innerHTML = "";
        eventList.forEach(event => {
            const ticketSVG = document.createElement("div");
            ticketSVG.innerHTML = `
                <svg width="400" height="200" viewBox="0 0 400 200" xmlns="http://www.w3.org/2000/svg">
                    <path d="M0,40 Q20,40 20,20 H380 Q380,40 400,40 V80 Q380,80 380,100 Q380,120 400,120 V160 Q380,160 380,180 H20 Q20,160 0,160 V120 Q20,120 20,100 Q20,80 0,80 Z" 
                          fill="white" stroke="#ccc" stroke-width="2"/>
                    <text x="40" y="60" font-size="22" font-family="Arial, sans-serif" font-weight="bold" fill="#333">${event.title}</text>
                    <text x="40" y="100" font-size="16" font-family="Arial, sans-serif" fill="#555">Date: ${event.date}</text>
                    <text x="40" y="130" font-size="16" font-family="Arial, sans-serif" fill="#555">Location: ${event.location}</text>
                    <text x="40" y="160" font-size="14" font-family="Arial, sans-serif" fill="#777">Tags: ${event.tags.join(", ")}</text>
                </svg>
            `;
            eventsContainer.appendChild(ticketSVG);
        });
    }

    function loadMoreEvents() {
        const newEvents = filteredEvents.slice(0, displayedEvents + eventsPerPage);
        renderEvents(newEvents);
        displayedEvents = newEvents.length;
        loadMoreButton.style.display = displayedEvents >= filteredEvents.length ? "none" : "block";
    }

    filterForm.addEventListener("input", function () {
        displayedEvents = 0;
        loadMoreEvents();
    });
    loadMoreButton.addEventListener("click", loadMoreEvents);
    
    loadMoreEvents();
});
