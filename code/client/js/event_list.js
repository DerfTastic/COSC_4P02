document.addEventListener("DOMContentLoaded", function () { 
    const eventsContainer = document.getElementById("eventsContainer");
    const loadMoreButton = document.getElementById("loadMore");
    const filterForm = document.getElementById("filterForm");
    
    let events = [
        { title: "Live Concert", location: "New York", category: "Concert", tags: ["Music", "Live"], price: 50, date: "2024-06-15", image: "/images/concert.jpg" },
        { title: "Broadway Show", location: "Los Angeles", category: "Theater", tags: ["Drama", "Performance"], price: 75, date: "2024-07-20", image: "/images/theater.jpg" },
        { title: "Comedy Night", location: "Chicago", category: "Comedy", tags: ["Stand-up", "Entertainment"], price: 30, date: "2024-08-10", image: "/images/comedy.jpg" },
        { title: "Food Festival", location: "San Francisco", category: "Festival", tags: ["Food", "Tasting"], price: 20, date: "2024-09-05", image: "/images/food.jpg" },
        { title: "Tech Conference", location: "Seattle", category: "Conference", tags: ["Technology", "Networking"], price: 100, date: "2024-10-12", image: "/images/tech.jpg" },
        { title: "Art Exhibition", location: "Miami", category: "Exhibition", tags: ["Art", "Gallery"], price: 25, date: "2024-11-18", image: "/images/art.jpg" },
        { title: "Jazz Festival", location: "New Orleans", category: "Festival", tags: ["Music", "Jazz"], price: 60, date: "2024-12-22", image: "/images/jazz.jpg" },
        { title: "Book Fair", location: "Boston", category: "Fair", tags: ["Books", "Reading"], price: 15, date: "2025-01-08", image: "/images/book.jpg" },
        { title: "Gaming Expo", location: "Las Vegas", category: "Expo", tags: ["Gaming", "Tech"], price: 90, date: "2025-02-20", image: "/images/gaming.jpg" },
        { title: "Opera Night", location: "Washington D.C.", category: "Theater", tags: ["Opera", "Performance"], price: 80, date: "2025-03-14", image: "/images/opera.jpg" },
        { title: "Wine Tasting", location: "Napa Valley", category: "Festival", tags: ["Wine", "Luxury"], price: 40, date: "2025-04-10", image: "/images/wine.jpg" }
    ];

    let displayedEvents = 0;
    const eventsPerPage = 10;
    let filteredEvents = [...events];

    function renderEvents(eventList) {
        eventsContainer.innerHTML = "";
        eventList.forEach(event => {
            const eventBox = document.createElement("div");
            eventBox.classList.add("event-box");
            eventBox.style.backgroundImage = `url('/images/ticket.png')`;
            eventBox.style.backgroundSize = "cover";
            eventBox.style.backgroundPosition = "center";
            eventBox.style.width = "100%";
            eventBox.style.height = "250px";
            eventBox.style.position = "relative";
    
            eventBox.innerHTML = `
                <div class="event-overlay">
                    <h3>${event.title}</h3>
                    <p><strong>Location:</strong> ${event.location}</p>
                    <p><strong>Category:</strong> ${event.category}</p>
                    <p><strong>Tags:</strong> ${event.tags.join(", ")}</p>
                    <p><strong>Price:</strong> $${event.price}</p>
                    <p><strong>Date:</strong> ${event.date}</p>
                </div>
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

    function filterAndSortEvents() {
        filteredEvents = [...events];
        displayedEvents = 0;
        
        const locationFilter = document.getElementById("filterLocation").value.toLowerCase();
        const categoryFilter = document.getElementById("filterCategory").value.toLowerCase();
        const tagFilter = document.getElementById("filterTag").value.toLowerCase();
        const minPrice = document.getElementById("minPrice").value ? parseInt(document.getElementById("minPrice").value) : 0;
        const maxPrice = document.getElementById("maxPrice").value ? parseInt(document.getElementById("maxPrice").value) : 300;
        const startDate = document.getElementById("startDate").value;
        const endDate = document.getElementById("endDate").value;
        const sortBy = document.getElementById("sort").value;
        const order = document.getElementById("order").value;
        
        filteredEvents = filteredEvents.filter(event => {
            return (!locationFilter || event.location.toLowerCase().includes(locationFilter)) &&
                   (!categoryFilter || event.category.toLowerCase().includes(categoryFilter)) &&
                   (!tagFilter || event.tags.some(tag => tag.toLowerCase().includes(tagFilter))) &&
                   (event.price >= minPrice && event.price <= maxPrice) &&
                   (!startDate || new Date(event.date) >= new Date(startDate)) &&
                   (!endDate || new Date(event.date) <= new Date(endDate));
        });
        
        if (sortBy) {
            filteredEvents.sort((a, b) => {
                if (sortBy === "price") {
                    return order === "asc" ? a.price - b.price : b.price - a.price;
                } else if (sortBy === "date") {
                    return order === "asc" ? new Date(a.date) - new Date(b.date) : new Date(b.date) - new Date(a.date);
                } else {
                    return order === "asc" ? a[sortBy].localeCompare(b[sortBy]) : b[sortBy].localeCompare(a[sortBy]);
                }
            });
        }
        
        loadMoreEvents();
    }
    
    filterForm.addEventListener("input", filterAndSortEvents);
    loadMoreButton.addEventListener("click", loadMoreEvents);
    
    loadMoreEvents();
});