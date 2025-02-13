document.addEventListener("DOMContentLoaded", function () {
    const eventsContainer = document.getElementById("eventsContainer");
    const loadMoreButton = document.getElementById("loadMore");
    const filterForm = document.getElementById("filterForm");
    
    let events = [
        { title: "Live Concert", location: "New York", category: "Concert", type: "Music", price: 50, date: "2024-06-15", image: "/images/concert.jpg" },
        { title: "Broadway Show", location: "Los Angeles", category: "Theater", type: "Drama", price: 75, date: "2024-07-20", image: "/images/theater.jpg" }
    ];

    let displayedEvents = 0;
    const eventsPerPage = 10;

    function renderEvents(eventList) {
        eventsContainer.innerHTML = "";
        eventList.forEach(event => {
            const eventBox = document.createElement("div");
            eventBox.classList.add("event-box");
            eventBox.dataset.location = event.location;
            eventBox.dataset.category = event.category;
            eventBox.dataset.type = event.type;
            eventBox.dataset.price = event.price;
            eventBox.dataset.date = event.date;
            
            eventBox.innerHTML = `
                <img src="${event.image}" alt="${event.title}">
                <h3>${event.title}</h3>
                <p>Location: ${event.location}</p>
                <p>Category: ${event.category}</p>
                <p>Type: ${event.type}</p>
                <p>Price: $${event.price}</p>
                <p>Date: ${event.date}</p>
            `;
            eventsContainer.appendChild(eventBox);
        });
    }

    function loadMoreEvents() {
        const newEvents = events.slice(displayedEvents, displayedEvents + eventsPerPage);
        renderEvents(newEvents);
        displayedEvents += newEvents.length;
        if (displayedEvents >= events.length) {
            loadMoreButton.style.display = "none";
        }
    }

    function filterAndSortEvents() {
        let filteredEvents = [...events];
        
        const locationFilter = document.getElementById("filterLocation").value.toLowerCase();
        const categoryFilter = document.getElementById("filterCategory").value.toLowerCase();
        const typeFilter = document.getElementById("filterType").value.toLowerCase();
        const tagFilter = document.getElementById("filterTag").value.toLowerCase();
        const minPrice = parseInt(document.getElementById("minPrice").value) || 0;
        const maxPrice = parseInt(document.getElementById("maxPrice").value) || 500;
        const startDate = document.getElementById("startDate").value;
        const endDate = document.getElementById("endDate").value;
        
        filteredEvents = filteredEvents.filter(event => {
            return (!locationFilter || event.location.toLowerCase().includes(locationFilter)) &&
                   (!categoryFilter || event.category.toLowerCase().includes(categoryFilter)) &&
                   (!typeFilter || event.type.toLowerCase().includes(typeFilter)) &&
                   (!tagFilter || event.title.toLowerCase().includes(tagFilter)) &&
                   (event.price >= minPrice && event.price <= maxPrice) &&
                   (!startDate || new Date(event.date) >= new Date(startDate)) &&
                   (!endDate || new Date(event.date) <= new Date(endDate));
        });
        
        const sortBy = document.getElementById("sort").value;
        const order = document.getElementById("order").value;
        filteredEvents.sort((a, b) => {
            if (sortBy === "price") {
                return order === "asc" ? a.price - b.price : b.price - a.price;
            } else if (sortBy === "date") {
                return order === "asc" ? new Date(a.date) - new Date(b.date) : new Date(b.date) - new Date(a.date);
            } else {
                return order === "asc" ? a[sortBy].localeCompare(b[sortBy]) : b[sortBy].localeCompare(a[sortBy]);
            }
        });
        
        renderEvents(filteredEvents);
    }
    
    filterForm.addEventListener("input", filterAndSortEvents);
    loadMoreButton.addEventListener("click", loadMoreEvents);
    
    loadMoreEvents();
});
