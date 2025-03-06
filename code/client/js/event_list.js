document.addEventListener("DOMContentLoaded", function () {
    const eventsContainer = document.getElementById("eventsContainer");
    const loadMoreButton = document.getElementById("loadMore");
    const filterForm = document.getElementById("filterForm");

    let events = [
        { title: "Live Concert", location: "New York", category: "Concert", tags: ["Music", "Live"], price: 50, date: "2024-06-15" },
        { title: "Broadway Show", location: "Los Angeles", category: "Theater", tags: ["Drama", "Stage"], price: 75, date: "2024-07-20" },
        { title: "Comedy Night", location: "Chicago", category: "Comedy", tags: ["Stand-up", "Humor"], price: 30, date: "2024-08-10" },
        { title: "Food Festival", location: "San Francisco", category: "Festival", tags: ["Food", "Tasting"], price: 20, date: "2024-09-05" },
        { title: "Tech Conference", location: "Seattle", category: "Conference", tags: ["Technology", "Networking"], price: 100, date: "2024-10-12" },
        { title: "Art Exhibition", location: "Miami", category: "Exhibition", tags: ["Art", "Gallery"], price: 25, date: "2024-11-18" },
        { title: "Jazz Festival", location: "New Orleans", category: "Festival", tags: ["Music", "Jazz"], price: 60, date: "2024-12-22" },
        { title: "Book Fair", location: "Boston", category: "Fair", tags: ["Books", "Literature"], price: 15, date: "2025-01-08" },
        { title: "Gaming Expo", location: "Las Vegas", category: "Expo", tags: ["Gaming", "Esports"], price: 90, date: "2025-02-20" },
        { title: "Opera Night", location: "Washington D.C.", category: "Theater", tags: ["Opera", "Classical"], price: 80, date: "2025-03-14" },
        { title: "Jazz Under the Stars", location: "New York City", category: "Music", tags: ["Jazz", "Outdoor"], price: 50, date: "2025-04-01" }
    ];

    let displayedEvents = 0;
    const eventsPerPage = 10;
    let filteredEvents = [...events];

    function renderEvents(eventList) {
        eventsContainer.innerHTML = "";
        eventList.forEach(event => {
            const ticketSVG = document.createElement("div");
            ticketSVG.innerHTML = `
                <svg width="600" height="300" viewBox="0 0 400 200" xmlns="http://www.w3.org/2000/svg">
                    <path d="M0,40 Q20,40 20,20 H380 Q380,40 400,40 V80 Q380,80 380,100 Q380,120 400,120 V160 Q380,160 380,180 H20 Q20,160 0,160 V120 Q20,120 20,100 Q20,80 0,80 Z" 
                          fill="white" stroke="#ccc" stroke-width="2"/>
                    <text x="40" y="60" font-size="22" font-family="Arial, sans-serif" font-weight="bold" fill="#333">${event.title}</text>
                    <text x="40" y="80" font-size="16" font-family="Arial, sans-serif" fill="#555">Date: ${event.date}</text>
                    <text x="40" y="100" font-size="16" font-family="Arial, sans-serif" fill="#555">Location: ${event.location}</text>
                    <text x="40" y="120" font-size="14" font-family="Arial, sans-serif" fill="#777">Category: ${event.category}</text>
                    <text x="40" y="140" font-size="14" font-family="Arial, sans-serif" fill="#777">Tags: ${event.tags.join(", ")}</text>
                    <text x="40" y="160" font-size="14" font-family="Arial, sans-serif" fill="#777">Price: $${event.price}</text>
                </svg>
            `;
            eventsContainer.appendChild(ticketSVG);
        });
    }

    function applyFilters() {
        const locationFilter = document.getElementById("filterLocation").value.toLowerCase();
        const categoryFilter = document.getElementById("filterCategory").value.toLowerCase();
        const tagFilter = document.getElementById("filterTag").value.toLowerCase();
        const minPrice = parseFloat(document.getElementById("minPrice").value) || 0;
        const maxPrice = parseFloat(document.getElementById("maxPrice").value) || Infinity;
        const startDate = document.getElementById("startDate").value;
        const endDate = document.getElementById("endDate").value;
        const sortBy = document.getElementById("sort").value;
        const sortOrder = document.getElementById("order").value;

        filteredEvents = events.filter(event => {
            return (
                (locationFilter === "" || event.location.toLowerCase().includes(locationFilter)) &&
                (categoryFilter === "" || event.category.toLowerCase().includes(categoryFilter)) &&
                (tagFilter === "" || event.tags.some(tag => tag.toLowerCase().includes(tagFilter))) &&
                (event.price >= minPrice && event.price <= maxPrice) &&
                (startDate === "" || new Date(event.date) >= new Date(startDate)) &&
                (endDate === "" || new Date(event.date) <= new Date(endDate))
            );
        });

        // Sorting
        filteredEvents.sort((a, b) => {
            if (sortBy === "price") return sortOrder === "asc" ? a.price - b.price : b.price - a.price;
            if (sortBy === "date") return sortOrder === "asc" ? new Date(a.date) - new Date(b.date) : new Date(b.date) - new Date(a.date);
            if (sortBy === "location") return sortOrder === "asc" ? a.location.localeCompare(b.location) : b.location.localeCompare(a.location);
            if (sortBy === "category") return sortOrder === "asc" ? a.category.localeCompare(b.category) : b.category.localeCompare(a.category);
        });

        displayedEvents = 0;
        loadMoreEvents();
    }

    function loadMoreEvents() {
        const newEvents = filteredEvents.slice(0, displayedEvents + eventsPerPage);
        renderEvents(newEvents);
        displayedEvents = newEvents.length;
        loadMoreButton.style.display = displayedEvents >= filteredEvents.length ? "none" : "block";
    }

    filterForm.addEventListener("input", applyFilters);
    loadMoreButton.addEventListener("click", loadMoreEvents);

    loadMoreEvents();
});
