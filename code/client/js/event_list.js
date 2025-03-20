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
            const ticketSVG = document.createElement("div");
            ticketSVG.classList.add("event-box");
            ticketSVG.innerHTML = `
                
                <svg width="900" height="400" viewBox="0 0 500 200" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="xMidYMid meet">
                    <!-- Ticket Shape -->
                    <path d="M0,40 Q20,40 20,20 H480 Q480,40 500,40 V80 Q480,80 480,100 Q480,120 500,120 V160 Q480,160 480,180 H20 Q20,160 0,160 V120 Q20,120 20,100 Q20,80 0,80 Z" 
                          fill="white" stroke="#ccc" stroke-width="2"/>
                    <!-- Placeholder Image -->
                    <rect x="40" y="40" width="180" height="120" fill="#ddd" stroke="#aaa" stroke-width="2"/>
                    <text x="130" y="110" font-size="14" font-family="Arial, sans-serif" fill="#666" text-anchor="middle">Image</text>

                     <line x1="240" y1="30" x2="240" y2="180" stroke="#ccc" stroke-width="3" stroke-dasharray="5,5"/>

                    <!-- Event Details -->
                    <text x="250" y="50" font-size="15" font-family="Arial, sans-serif" font-weight="bold" fill="#333">${event.title}</text>
                    <text x="250" y="80" font-size="16" font-family="Arial, sans-serif" fill="#555">Date: ${event.date}</text>
                    <text x="250" y="100" font-size="16" font-family="Arial, sans-serif" fill="#555">Location: ${event.location}</text>
                    <text x="250" y="120" font-size="14" font-family="Arial, sans-serif" fill="#777">Category: ${event.category}</text>
                    <text x="250" y="140" font-size="14" font-family="Arial, sans-serif" fill="#777">Tags: ${event.tags.join(", ")}</text>
                    <text x="250" y="160" font-size="14" font-family="Arial, sans-serif" fill="#777">Price: $${event.price}</text>
                </svg>
            `;
            eventsContainer.appendChild(ticketSVG);
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
        const locationFilter = document.getElementById("filterLocation").value.toLowerCase().trim();
        const categoryFilter = document.getElementById("filterCategory").value.toLowerCase().trim();
        const tagFilter = document.getElementById("filterTag").value.toLowerCase().trim();
        const minPrice = parseFloat(document.getElementById("minPrice").value) || 0;
        const maxPrice = parseFloat(document.getElementById("maxPrice").value) || Number.MAX_VALUE;
        const startDate = document.getElementById("startDate").value;
        const endDate = document.getElementById("endDate").value;
        const sortBy = document.getElementById("sort").value;
        const order = document.getElementById("order").value;

        filteredEvents = events.filter(event => {
            const eventDate = new Date(event.date);
            const isLocationMatch = locationFilter === "" || event.location.toLowerCase().includes(locationFilter);
            const isCategoryMatch = categoryFilter === "" || event.category.toLowerCase().includes(categoryFilter);
            const isTagMatch = tagFilter === "" || event.tags.some(tag => tag.toLowerCase().includes(tagFilter));
            const isPriceMatch = event.price >= minPrice && event.price <= maxPrice;
            const isDateMatch = (!startDate || eventDate >= new Date(startDate)) && (!endDate || eventDate <= new Date(endDate));

            return isLocationMatch && isCategoryMatch && isTagMatch && isPriceMatch && isDateMatch;
        });

        filteredEvents.sort((a, b) => {
            let valueA = a[sortBy];
            let valueB = b[sortBy];

            if (sortBy === "date") {
                valueA = new Date(a.date);
                valueB = new Date(b.date);
            } else if (sortBy === "price") {
                valueA = parseFloat(a.price);
                valueB = parseFloat(b.price);
            }

            if (order === "asc") {
                return valueA > valueB ? 1 : -1;
            } else {
                return valueA < valueB ? 1 : -1;
            }
        });

        currentPage = 1;
        changePage(1);
    }

    filterForm.addEventListener("input", applyFilters);
    changePage(1);
});
