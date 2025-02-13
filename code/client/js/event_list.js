document.addEventListener("DOMContentLoaded", function () {
    const eventsContainer = document.getElementById("eventsContainer");
    const loadMoreButton = document.getElementById("loadMore");
    const filterForm = document.getElementById("filterForm");
    
    let allEvents = [];
    let displayedEvents = 0;
    const eventsPerPage = 10;

    function fetchEvents() {
        fetch("/api/events")
            .then(response => response.json())
            .then(data => {
                allEvents = data;
                applyFilters();
            })
            .catch(error => console.error("Error fetching events:", error));
    }

    function applyFilters() {
        let filteredEvents = allEvents.filter(event => {
            const locationMatch = event.location.toLowerCase().includes(document.getElementById("filterLocation").value.toLowerCase());
            const categoryMatch = event.category.toLowerCase().includes(document.getElementById("filterCategory").value.toLowerCase());
            const typeMatch = event.type.toLowerCase().includes(document.getElementById("filterType").value.toLowerCase());
            const tagMatch = event.tags.some(tag => tag.toLowerCase().includes(document.getElementById("filterTag").value.toLowerCase()));
            
            const minPrice = parseInt(document.getElementById("minPrice").value, 10) || 0;
            const maxPrice = parseInt(document.getElementById("maxPrice").value, 10) || 500;
            const priceMatch = event.price >= minPrice && event.price <= maxPrice;
            
            const startDate = new Date(document.getElementById("startDate").value);
            const endDate = new Date(document.getElementById("endDate").value);
            const eventDate = new Date(event.date);
            const dateMatch = (!document.getElementById("startDate").value || eventDate >= startDate) &&
                              (!document.getElementById("endDate").value || eventDate <= endDate);
            
            return locationMatch && categoryMatch && typeMatch && tagMatch && priceMatch && dateMatch;
        });

        displayEvents(filteredEvents);
    }

    function displayEvents(events) {
        eventsContainer.innerHTML = "";
        const eventsToShow = events.slice(0, displayedEvents + eventsPerPage);
        displayedEvents = eventsToShow.length;
        
        eventsToShow.forEach(event => {
            const eventElement = document.createElement("div");
            eventElement.classList.add("event-box");
            eventElement.innerHTML = `
                <img src="${event.image}" alt="${event.title}">
                <h3>${event.title}</h3>
                <p><strong>Location:</strong> ${event.location}</p>
                <p><strong>Category:</strong> ${event.category}</p>
                <p><strong>Type:</strong> ${event.type}</p>
                <p><strong>Tags:</strong> ${event.tags.join(", ")}</p>
                <p><strong>Price:</strong> $${event.price}</p>
                <p><strong>Date:</strong> ${event.date}</p>
            `;
            eventsContainer.appendChild(eventElement);
        });

        loadMoreButton.style.display = events.length > displayedEvents ? "block" : "none";
    }

    loadMoreButton.addEventListener("click", function () {
        displayedEvents += eventsPerPage;
        applyFilters();
    });

    filterForm.addEventListener("input", applyFilters);
    
    fetchEvents();
});
