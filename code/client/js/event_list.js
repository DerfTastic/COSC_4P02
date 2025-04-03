document.addEventListener("DOMContentLoaded", function () {
    const eventsContainer = document.getElementById("eventsContainer");
    const paginationContainer = document.getElementById("pagination");
    const filterForm = document.getElementById("filterForm");

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

    let count = 1;
    let doing = false;
    async function applyFilters() {
        if (doing) return count++;
        doing = true;
        let mycount;
        try {
            do {
                mycount = ++count
                const nameFilter = document.getElementById("filterName").value.toLowerCase().trim();
                const locationFilter = document.getElementById("filterLocation").value.toLowerCase().trim();
                const categoryFilter = document.getElementById("filterCategory").value.toLowerCase().trim();
                const tagFilter = document.getElementById("filterTag").value.toLowerCase().trim();
                const minPrice = parseFloat(document.getElementById("minPrice").value) || 0;
                const maxPrice = parseFloat(document.getElementById("maxPrice").value) || Number.MAX_VALUE;
                const startDate = document.getElementById("startDate").value;
                const endDate = document.getElementById("endDate").value;
                const sortBy = document.getElementById("sort").value;

                /**
                 * @type{Search}
                 */
                var search = {};
                if (nameFilter.length > 0) {
                    search.name_fuzzy = `%${nameFilter}%`
                }
                if (locationFilter.length > 0) {
                    search.location = `%${locationFilter}%`
                }
                if (categoryFilter.length > 0) {
                    search.category_fuzzy = `%${categoryFilter}%`
                }
                if (tagFilter.length > 0) {
                    search.tags = [tagFilter];
                }
                if (startDate.length > 0) {
                    search.date_start = new Date(startDate).getTime()
                }
                if (endDate.length > 0) {
                    search.date_end = new Date(endDate).getTime() + 8.64e+7;
                }
                search.sort_by = sortBy;

                currentPage = 1;
                events = await api.search.search_events_with_owner(search);
                changePage(1);
            } while (mycount != count);
        } catch (e) {
            console.log(e);
        }

        doing = false;
    }

    filterForm.addEventListener("input", applyFilters);
    applyFilters();
});
