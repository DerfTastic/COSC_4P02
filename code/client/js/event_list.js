

document.addEventListener("DOMContentLoaded", function () {
    const events = [
        { title: "Event One", desc: "Join us for an amazing time!", img: "logo.png", location: "New York", price: 20, genre: "Music", tags: ["Concert", "Live"] },
        { title: "Event Two", desc: "Don't miss this exciting event.", img: "logo.png", location: "Los Angeles", price: 35, genre: "Comedy", tags: ["Stand-up", "Entertainment"] },
        { title: "Event Three", desc: "A wonderful experience awaits.", img: "logo.png", location: "Chicago", price: 15, genre: "Theater", tags: ["Play", "Drama"] },
        { title: "Event Four", desc: "Experience something great!", img: "logo.png", location: "Miami", price: 50, genre: "Festival", tags: ["Outdoor", "Fun"] },
        { title: "Event 5", desc: "Join us for an amazing time!", img: "logo.png", location: "New York", price: 20, genre: "Music", tags: ["Concert", "Live"] },
        { title: "Event 6", desc: "Don't miss this exciting event.", img: "logo.png", location: "Los Angeles", price: 35, genre: "Comedy", tags: ["Stand-up", "Entertainment"] },
        { title: "Event 7", desc: "A wonderful experience awaits.", img: "logo.png", location: "Chicago", price: 15, genre: "Theater", tags: ["Play", "Drama"] },
        { title: "Event 8", desc: "Experience something great!", img: "logo.png", location: "Miami", price: 50, genre: "Festival", tags: ["Outdoor", "Fun"] },
        { title: "Event 9", desc: "Join us for an amazing time!", img: "logo.png", location: "New York", price: 20, genre: "Music", tags: ["Concert", "Live"] },
        { title: "Event 10", desc: "Don't miss this exciting event.", img: "logo.png", location: "Los Angeles", price: 35, genre: "Comedy", tags: ["Stand-up", "Entertainment"] },
        { title: "Event 11", desc: "A wonderful experience awaits.", img: "logo.png", location: "Chicago", price: 15, genre: "Theater", tags: ["Play", "Drama"] },
        { title: "Event 12", desc: "Experience something great!", img: "logo.png", location: "Miami", price: 50, genre: "Festival", tags: ["Outdoor", "Fun"] },
        { title: "Event 13", desc: "Join us for an amazing time!", img: "logo.png", location: "New York", price: 20, genre: "Music", tags: ["Concert", "Live"] },
        { title: "Event 14", desc: "Don't miss this exciting event.", img: "logo.png", location: "Los Angeles", price: 35, genre: "Comedy", tags: ["Stand-up", "Entertainment"] },
        { title: "Event 15", desc: "A wonderful experience awaits.", img: "logo.png", location: "Chicago", price: 15, genre: "Theater", tags: ["Play", "Drama"] },
        { title: "Event 16", desc: "Experience something great!", img: "logo.png", location: "Miami", price: 50, genre: "Festival", tags: ["Outdoor", "Fun"] },
        { title: "Event 17", desc: "Join us for an amazing time!", img: "logo.png", location: "New York", price: 20, genre: "Music", tags: ["Concert", "Live"] },
        { title: "Event 18", desc: "Don't miss this exciting event.", img: "logo.png", location: "Los Angeles", price: 35, genre: "Comedy", tags: ["Stand-up", "Entertainment"] },
        { title: "Event 19", desc: "A wonderful experience awaits.", img: "logo.png", location: "Chicago", price: 15, genre: "Theater", tags: ["Play", "Drama"] },
        { title: "Event 20", desc: "Experience something great!", img: "logo.png", location: "Miami", price: 50, genre: "Festival", tags: ["Outdoor", "Fun"] },
        { title: "Event 21", desc: "Join us for an amazing time!", img: "logo.png", location: "New York", price: 20, genre: "Music", tags: ["Concert", "Live"] },
        { title: "Event 22", desc: "Don't miss this exciting event.", img: "logo.png", location: "Los Angeles", price: 35, genre: "Comedy", tags: ["Stand-up", "Entertainment"] }
    ];

        let eventsDisplayed = 0;
        const eventsPerLoad = 10;
        const eventsContainer = document.getElementById("eventsContainer");
        const loadMoreButton = document.getElementById("loadMore");
        const sortSelect = document.getElementById("sort");
        const orderSelect = document.getElementById("order");
        const filterForm = document.getElementById("filterForm");
    
        function renderEvents(eventList, append = false) {
            if (!append) {
                eventsContainer.innerHTML = "";
            }
            const fragment = document.createDocumentFragment();
            eventList.forEach(event => {
                const eventDiv = document.createElement("div");
                eventDiv.classList.add("event-box");
                eventDiv.innerHTML = `
                    <img src="${event.img}" alt="${event.title}">
                    <h3>${event.title}</h3>
                    <p>${event.desc}</p>
                    <p><strong>Location:</strong> ${event.location}</p>
                    <p><strong>Price:</strong> $${event.price}</p>
                    <p><strong>Genre:</strong> ${event.genre}</p>
                    <p><strong>Tags:</strong> ${event.tags.join(", ")}</p>
                `;
                fragment.appendChild(eventDiv);
            });
            eventsContainer.appendChild(fragment);
        }
    
        function loadEvents() {
            let filteredEvents = filterEvents(events);
            let eventSubset = filteredEvents.slice(eventsDisplayed, eventsDisplayed + eventsPerLoad);
            renderEvents(eventSubset, true);
            eventsDisplayed += eventsPerLoad;
            if (eventsDisplayed >= filteredEvents.length) {
                loadMoreButton.style.display = "none";
            } else {
                loadMoreButton.style.display = "block";
            }
        }
    
        function sortEvents(eventList) {
            const sortBy = sortSelect.value;
            const order = orderSelect.value === "asc" ? 1 : -1;
            return eventList.sort((a, b) => {
                if (typeof a[sortBy] === "string") {
                    return a[sortBy].localeCompare(b[sortBy]) * order;
                } else {
                    return (a[sortBy] - b[sortBy]) * order;
                }
            });
        }
    
        function filterEvents(eventList) {
            const locationFilter = document.getElementById("filterLocation").value.toLowerCase();
            const genreFilter = document.getElementById("filterGenre").value.toLowerCase();
            const tagFilter = document.getElementById("filterTag").value.toLowerCase();
    
            return eventList.filter(event => {
                return (
                    (locationFilter === "" || event.location.toLowerCase().includes(locationFilter)) &&
                    (genreFilter === "" || event.genre.toLowerCase().includes(genreFilter)) &&
                    (tagFilter === "" || event.tags.some(tag => tag.toLowerCase().includes(tagFilter)))
                );
            });
        }
    
        function updateEvents() {
            eventsDisplayed = 0;
            loadMoreButton.style.display = "block";
            let filteredAndSortedEvents = sortEvents(filterEvents(events));
            eventsContainer.innerHTML = "";
            loadEvents();
        }
    
        filterForm.addEventListener("change", updateEvents);
        sortSelect.addEventListener("change", updateEvents);
        orderSelect.addEventListener("change", updateEvents);
        loadMoreButton.addEventListener("click", loadEvents);
        loadEvents(); // Load initial events
    });