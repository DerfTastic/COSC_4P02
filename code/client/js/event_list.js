document.addEventListener("DOMContentLoaded", function () {
    const eventsContainer = document.getElementById("eventsContainer");
    const paginationContainer = document.getElementById("pagination");
    const filterForm = document.getElementById("filterForm");

    let currentPage = 1;
    const eventsPerPage = 5;
    let events = [];
    let filteredEvents = [...events];
    changePage(1);

    function renderEvents(eventList) {
        eventsContainer.innerHTML = "";
        eventList.forEach(event => {
            const ticketSVG = document.createElement("div");
            ticketSVG.classList.add("event-box");
            ticketSVG.innerHTML = `
                <svg id="ticket-svg" style="cursor: pointer;" width="900" height="400" viewBox="0 0 500 200" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="xMidYMid meet">
                    <!-- Ticket Shape -->
                    <path d="M0,40 Q20,40 20,20 H480 Q480,40 500,40 V80 Q480,80 480,100 Q480,120 500,120 V160 Q480,160 480,180 H20 Q20,160 0,160 V120 Q20,120 20,100 Q20,80 0,80 Z" 
                            fill="#eeeef0"/>
                    <!-- Placeholder Image -->
                    <image href="/media/${event.picture}" preserveAspectRatio="none" x="30" y="40" width="200" height="120" fill="#ddd" stroke="#aaa" stroke-width="2"/>
                    <!-- <text x="130" y="110" font-size="14" fill="#666" text-anchor="middle">Image</text> -->

                        <line x1="240" y1="23" x2="240" y2="180" stroke="#415a77" stroke-width="2.5" stroke-dasharray="5,5"/>

                    <foreignObject x="250" y="25" width="230" height="140">
                    <div style="height:100%; display: flex; flex-direction: column; justify-content: stretch; align-items:flex-start; padding: 10px;gap:3px; overflow: hidden;box-sizing: border-box;">
                    <!-- Event Name -->
                    <div style="text-align:left; font-size: 16px; font-weight: bold; color: #1b263b; margin-bottom:5px;">
                        ${event.name}
                        </div>

                    <!-- Event Date -->
                    <div style="text-align:left; font-size: 13px; color: #1b263b;">
                        Date: ${new Date(event.start).toUTCString()}
                        </div>

                    <!-- Event Location -->
                    <div style="text-align:left; font-size: 13px; color: #1b263b;">
                        Location: ${event.location_name}
                    </div>

                    <!-- Event Category -->
                    <div style="text-align:left; font-size: 13px; color: #1b263b;">
                        Category: ${event.category}
                    </div>

                    <!-- Event Organizer -->
                        <div style="text-align:left; font-size: 13px; color: #1b263b;">
                    Organizer: ${event.owner.name}
                    </div>
                </div>
                </foreignObject>
                </svg>
            `;
            ticketSVG.firstElementChild.addEventListener("click", e => {
                window.location.href = `/event?id=${event.id}`
            })
            eventsContainer.appendChild(ticketSVG);
        });
    }

    function renderPagination() {
        paginationContainer.innerHTML = "";
        const totalPages = Math.ceil(filteredEvents.length / eventsPerPage);
        if (totalPages <= 1) return;

        const createButton = (text, page, isActive = false) => {
            const btn = document.createElement("button");
            btn.textContent = text;
            btn.classList.add("page-btn");
            if (isActive) btn.classList.add("active");
            btn.addEventListener("click", () => changePage(page));
            return btn;
        };

        // Always show first page
        paginationContainer.appendChild(createButton(1, 1, currentPage === 1));

        // Ellipses if needed
        if (currentPage > 3) {
            paginationContainer.appendChild(document.createTextNode("..."));
        }

        // Show previous page if it's not 1
        if (currentPage > 2) {
            paginationContainer.appendChild(createButton(currentPage - 1, currentPage - 1));
        }

        // Show the current page
        if (currentPage !== 1 && currentPage !== totalPages) {
            paginationContainer.appendChild(createButton(currentPage, currentPage, true));
        }

        // Show next page if it's not the last one
        if (currentPage < totalPages - 1) {
            paginationContainer.appendChild(createButton(currentPage + 1, currentPage + 1));
        }

        // Ellipses if needed
        if (currentPage < totalPages - 2) {
            paginationContainer.appendChild(document.createTextNode("..."));
        }

        // Always show the last page
        if (totalPages > 1) {
            paginationContainer.appendChild(createButton(totalPages, totalPages, currentPage === totalPages));
        }
    }

    document.getElementById("filterName").value = new URLSearchParams(window.location.search).get("name");
    document.getElementById("filterCategory").value = new URLSearchParams(window.location.search).get("category");
    document.getElementById("filterTag").value = new URLSearchParams(window.location.search).get("tags");
    document.getElementById("filterLocation").value = new URLSearchParams(window.location.search).get("location");

    function changePage(page) {
        const totalPages = Math.ceil(filteredEvents.length / eventsPerPage);
        // if (page < 1 || page > totalPages) return;
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
                const tagFilter = document.getElementById("filterTag").value.split(" ").map(t => t.trim()).filter(t => t.length!=0).map(t => `%${t}%`);
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
                    search.tags = tagFilter;
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
                filteredEvents = [...events];
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
