document.addEventListener("DOMContentLoaded", function () {
    const eventsContainer = document.getElementById("eventsContainer");
    const loadMoreButton = document.getElementById("loadMore");
    const filterForm = document.getElementById("filterForm");
    
    let events = [
        { title: "Live Concert", location: "New York", category: "Concert", price: 50, date: "2024-06-15", image: "/images/concert.jpg" },
        { title: "Broadway Show", location: "Los Angeles", category: "Theater", price: 75, date: "2024-07-20", image: "/images/theater.jpg" },
        { title: "Comedy Night", location: "Chicago", category: "Comedy", price: 30, date: "2024-08-10", image: "/images/comedy.jpg" },
        { title: "Food Festival", location: "San Francisco", category: "Festival", price: 20, date: "2024-09-05", image: "/images/food.jpg" },
        { title: "Tech Conference", location: "Seattle", category: "Conference", price: 100, date: "2024-10-12", image: "/images/tech.jpg" },
        { title: "Art Exhibition", location: "Miami", category: "Exhibition", price: 25, date: "2024-11-18", image: "/images/art.jpg" },
        { title: "Jazz Festival", location: "New Orleans", category: "Festival", price: 60, date: "2024-12-22", image: "/images/jazz.jpg" },
        { title: "Book Fair", location: "Boston", category: "Fair", price: 15, date: "2025-01-08", image: "/images/book.jpg" },
        { title: "Gaming Expo", location: "Las Vegas", category: "Expo", price: 90, date: "2025-02-20", image: "/images/gaming.jpg" },
        { title: "Opera Night", location: "Washington D.C.", category: "Theater", price: 80, date: "2025-03-14", image: "/images/opera.jpg" },
        { title: "Wine Tasting", location: "Napa Valley", category: "Festival", price: 40, date: "2025-04-10", image: "/images/wine.jpg" },
        { title: "Circus Show", location: "Dallas", category: "Entertainment", price: 35, date: "2025-05-20", image: "/images/circus.jpg" },
        { title: "Rock Concert", location: "Denver", category: "Concert", price: 70, date: "2025-06-15", image: "/images/rock.jpg" },
        { title: "Fashion Expo", location: "New York", category: "Expo", price: 85, date: "2025-07-30", image: "/images/fashion.jpg" },
        { title: "Film Festival", location: "Los Angeles", category: "Festival", price: 50, date: "2025-08-18", image: "/images/film.jpg" },
        { title: "Magic Show", location: "Orlando", category: "Entertainment", price: 45, date: "2025-09-10", image: "/images/magic.jpg" },
        { title: "Marathon", location: "Boston", category: "Sports", price: 20, date: "2025-10-05", image: "/images/marathon.jpg" },
        { title: "Science Fair", location: "Houston", category: "Fair", price: 30, date: "2025-11-22", image: "/images/science.jpg" },
        { title: "Ballet Performance", location: "San Francisco", category: "Theater", price: 65, date: "2025-12-08", image: "/images/ballet.jpg" }
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
