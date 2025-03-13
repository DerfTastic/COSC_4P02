document.addEventListener("DOMContentLoaded", function () {
    const eventsContainer = document.getElementById("eventsContainer");
    const paginationContainer = document.getElementById("pagination");
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

    let currentPage = 1;
    const eventsPerPage = 5;
    let filteredEvents = [...events];

    function renderEvents(eventList) {
        eventsContainer.innerHTML = "";
        eventList.forEach(event => {
            const eventDiv = document.createElement("div");
            eventDiv.classList.add("event-box");
            eventDiv.innerHTML = `
                <h3>${event.title}</h3>
                <p>Date: ${event.date}</p>
                <p>Location: ${event.location}</p>
                <p>Category: ${event.category}</p>
                <p>Tags: ${event.tags.join(", ")}</p>
                <p>Price: $${event.price}</p>
            `;
            eventsContainer.appendChild(eventDiv);
        });
    }

    function renderPagination() {
        paginationContainer.innerHTML = "";
        const totalPages = Math.ceil(filteredEvents.length / eventsPerPage);
        
        if (totalPages <= 1) return;

        const createButton = (text, page) => {
            const btn = document.createElement("button");
            btn.textContent = text;
            btn.classList.add("page-btn");
            if (page === currentPage) btn.classList.add("active");
            btn.addEventListener("click", () => changePage(page));
            return btn;
        };

        paginationContainer.appendChild(createButton("⏮", 1));
        paginationContainer.appendChild(createButton("◀", Math.max(1, currentPage - 1)));
        
        for (let i = 1; i <= totalPages; i++) {
            paginationContainer.appendChild(createButton(i, i));
        }
        
        paginationContainer.appendChild(createButton("▶", Math.min(totalPages, currentPage + 1)));
        paginationContainer.appendChild(createButton("⏭", totalPages));
    }

    function changePage(page) {
        const totalPages = Math.ceil(filteredEvents.length / eventsPerPage);
        if (page < 1 || page > totalPages) return;
        currentPage = page;
        const start = (currentPage - 1) * eventsPerPage;
        const end = start + eventsPerPage;
        renderEvents(filteredEvents.slice(start, end));
        renderPagination();
    }

    function applyFilters() {
        const locationFilter = document.getElementById("filterLocation").value.toLowerCase();
        const categoryFilter = document.getElementById("filterCategory").value.toLowerCase();
        const tagFilter = document.getElementById("filterTag").value.toLowerCase();

        filteredEvents = events.filter(event =>
            (locationFilter === "" || event.location.toLowerCase().includes(locationFilter)) &&
            (categoryFilter === "" || event.category.toLowerCase().includes(categoryFilter)) &&
            (tagFilter === "" || event.tags.some(tag => tag.toLowerCase().includes(tagFilter)))
        );

        currentPage = 1;
        changePage(1);
    }

    filterForm.addEventListener("input", applyFilters);
    changePage(1);
});
