document.addEventListener("DOMContentLoaded", function () {
    const eventsContainer = document.getElementById("eventsContainer");
    const paginationContainer = document.getElementById("pagination");
    const filterForm = document.getElementById("filterForm");

    let currentPage = 1;
    const eventsPerPage = 5;

    function renderEvents(eventList) {
        eventsContainer.innerHTML = "";
        eventList.forEach(event => {
            const ticketSVG = document.createElement("div");
            ticketSVG.classList.add("event-box");
            ticketSVG.innerHTML = `
                <svg style="cursor: pointer;" width="900" height="400" viewBox="0 0 500 200" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="xMidYMid meet">
                    <!-- Ticket Shape -->
                    <path d="M0,40 Q20,40 20,20 H480 Q480,40 500,40 V80 Q480,80 480,100 Q480,120 500,120 V160 Q480,160 480,180 H20 Q20,160 0,160 V120 Q20,120 20,100 Q20,80 0,80 Z" 
                          fill="white" stroke="#ccc" stroke-width="2"/>
                    <!-- Placeholder Image -->
                    <image href="/media/${event.picture}" x="40" y="40" width="180" height="120" fill="#ddd" stroke="#aaa" stroke-width="2"/>
                    <!-- <text x="130" y="110" font-size="14" font-family="Arial, sans-serif" fill="#666" text-anchor="middle">Image</text> -->

                     <line x1="240" y1="30" x2="240" y2="180" stroke="#ccc" stroke-width="3" stroke-dasharray="5,5"/>

                    <!-- Event Details -->
                    <text x="250" y="50" font-size="15" font-family="Arial, sans-serif" font-weight="bold" fill="#333">${event.name}</text>
                    <text x="250" y="80" font-size="16" font-family="Arial, sans-serif" fill="#555">Date: ${new Date(event.start).toUTCString()}</text>
                    <text x="250" y="100" font-size="16" font-family="Arial, sans-serif" fill="#555">Location: ${event.location_name}</text>
                    <text x="250" y="120" font-size="14" font-family="Arial, sans-serif" fill="#777">Category: ${event.category}</text>
                    <text x="250" y="140" font-size="14" font-family="Arial, sans-serif" fill="#777">Tags: ${event.tags.join(", ")}</text>
                    <text x="250" y="160" font-size="14" font-family="Arial, sans-serif" fill="#777">Organizer: ${event.owner.name}</text>
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
        const totalPages = Math.ceil(events.length / eventsPerPage);

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
        const totalPages = Math.ceil(events.length / eventsPerPage);
        // if (page < 1 || page > totalPages) return;
        currentPage = page;
        const start = (currentPage - 1) * eventsPerPage;
        const end = start + eventsPerPage;
        renderEvents(events.slice(start, end));
        renderPagination();
    }

    let count = 1;
    let doing = false;
    async function applyFilters() {
        if(doing)return count++;
        doing = true;
        let mycount;
        try{
            do{
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
                if(nameFilter.length > 0){
                    search.name_fuzzy = `%${nameFilter}%`
                }
                if(locationFilter.length > 0){
                    search.location = `%${locationFilter}%`
                }
                if(categoryFilter.length > 0){
                    search.category_fuzzy = `%${categoryFilter}%`
                }
                if(tagFilter.length > 0){
                    search.tags = [tagFilter];
                }
                if(startDate.length>0){
                    search.date_start = new Date(startDate).getTime()
                }
                if(endDate.length>0){
                    search.date_end = new Date(endDate).getTime() + 8.64e+7;
                }
                search.sort_by = sortBy;
                
                currentPage = 1;
                events = await api.search.search_events_with_owner(search);
                changePage(1);
            }while(mycount!=count);
        }catch(e){
            console.log(e);
        }

        doing = false;
    }

    filterForm.addEventListener("input", applyFilters);
    applyFilters();
});
