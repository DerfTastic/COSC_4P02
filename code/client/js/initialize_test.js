/** @type{Organizer[]}*/
const taylor = [
    {
        name: "Harmony Productions",
        email: "info@harmonypro.com",
        bio: "Crafting unforgettable musical experiences.",
        disp_email: "contact@harmonypro.com",
        disp_phone_number: "(555) 987-6543",
        picture: "harmonypro.jpg",
        banner: "harmonypro_banner.jpg",
        events: [
            {
                name: "Classical Music Gala",
                description: "An evening of timeless symphonies and orchestral brilliance.",
                type: "concert",
                category: "music",
                tags: ["classical", "orchestra", "live performance"],
                location_name: "Grand Symphony Hall",
                location_lat: 40.7306,
                location_long: -73.9352,
                start: 1691932800000, 
                duration: 86400000,
                tickets: [
                    { name: "Standard Ticket", price: 50, available_tickets: 1000 },
                    { name: "VIP Seating", price: 150, available_tickets: 200 }
                ]
            }
        ]
    },
    {
        name: "TechNext",
        email: "events@technext.com",
        bio: "Pioneering the future of technology through engaging conferences.",
        disp_email: "hello@technext.com",
        disp_phone_number: "(555) 246-1357",
        picture: "technext.jpg",
        banner: "technext_banner.jpg",
        events: [
            {
                name: "Future of AI Summit",
                description: "Exploring the next frontier in artificial intelligence.",
                type: "conference",
                category: "technology",
                tags: ["AI", "machine learning", "future tech"],
                location_name: "Silicon Convention Center",
                location_lat: 37.7749,
                location_long: -122.4194,
                start: 1691328000000, 
                duration: 57600000,
                tickets: [
                    { name: "Standard Pass", price: 400, available_tickets: 800 },
                    { name: "VIP Access", price: 1000, available_tickets: 150 }
                ]
            }
        ]
    },
    {
        name: "Mindful Growth",
        email: "hello@mindfulgrowth.com",
        bio: "Promoting wellness through mindful living and self-development.",
        disp_email: "support@mindfulgrowth.com",
        disp_phone_number: "(555) 678-9012",
        picture: "mindfulgrowth.jpg",
        banner: "mindfulgrowth_banner.jpg",
        events: [
            {
                name: "Mindfulness & Productivity Workshop",
                description: "Learn how mindfulness can improve focus and performance.",
                type: "workshop",
                category: "education",
                tags: ["mindfulness", "productivity", "self-improvement"],
                location_name: "Downtown Conference Center",
                location_lat: 34.0522,
                location_long: -118.2437,
                start: 1690723200000, 
                duration: 43200000,
                tickets: [
                    { name: "General Entry", price: 120, available_tickets: 300 }
                ]
            }
        ]
    },
    {
        name: "Epic Adventures",
        email: "contact@epicadventures.com",
        bio: "Creating thrilling outdoor experiences for adrenaline seekers.",
        disp_email: "info@epicadventures.com",
        disp_phone_number: "(555) 432-1098",
        picture: "epicadventures.jpg",
        banner: "epicadventures_banner.jpg",
        events: [
            {
                name: "Ultimate Rock Climbing Challenge",
                description: "Test your skills on some of the most challenging climbing routes.",
                type: "festival",
                category: "sport",
                tags: ["climbing", "outdoors", "adventure"],
                location_name: "Red Rock Canyon",
                location_lat: 36.1716,
                location_long: -115.1391,
                start: 1686950400000, 
                duration: 43200000,
                tickets: [
                    { name: "Participant Pass", price: 100, available_tickets: 500 },
                    { name: "Spectator Ticket", price: 30, available_tickets: 1000 }
                ]
            }
        ]
    },
    {
        name: "Artistic Visions",
        email: "info@artisticvisions.com",
        bio: "Celebrating creativity through inspiring art events.",
        disp_email: "events@artisticvisions.com",
        disp_phone_number: "(555) 890-1234",
        picture: "artisticvisions.jpg",
        banner: "artisticvisions_banner.jpg",
        events: [
            {
                name: "Modern Art Expo",
                description: "Showcasing cutting-edge contemporary art from around the world.",
                type: "festival",
                category: "art",
                tags: ["modern art", "exhibition", "culture"],
                location_name: "Metropolitan Art Gallery",
                location_lat: 51.5074,
                location_long: -0.1278,
                start: 1686278400000, 
                duration: 86400000,
                tickets: [
                    { name: "General Admission", price: 25, available_tickets: 2000 },
                    { name: "VIP Tour", price: 75, available_tickets: 300 }
                ]
            }
        ]
    }
];


/** @type{Organizer[]}*/
const jacob = [
    {
        name: "Starlight Events",
        email: "info@starlightevents.com",
        bio: "Creating magical experiences under the stars.",
        disp_email: "contact@starlightevents.com",
        disp_phone_number: "(555) 123-4567",
        picture: "starlightevents.jpg",
        banner: "starlightevents_banner.jpg",
        events: [
            {
                name: "Midnight Jazz Festival",
                description: "A soulful night of jazz and fine dining.",
                type: "festival",
                category: "music",
                tags: ["jazz", "live music", "nightlife"],
                location_name: "Riverfront Amphitheater",
                location_lat: 34.0522,
                location_long: -118.2437,
                start: 1685664000000, 
                duration: 43200000,
                tickets: [
                    { name: "Standard Ticket", price: 80, available_tickets: 3000 },
                    { name: "VIP Lounge", price: 250, available_tickets: 500 }
                ]
            }
        ]
    },
    {
        name: "Global Innovators",
        email: "hello@globalinnovators.com",
        bio: "Bringing together the world's brightest minds.",
        disp_email: "events@globalinnovators.com",
        disp_phone_number: "(666) 234-5678",
        picture: "globalinnovators.jpg",
        banner: "globalinnovators_banner.jpg",
        events: [
            {
                name: "Blockchain Revolution Summit",
                description: "Exploring the future of decentralized finance.",
                type: "conference",
                category: "technology",
                tags: ["blockchain", "crypto", "finance"],
                location_name: "Silicon Valley Tech Hub",
                location_lat: 37.7749,
                location_long: -122.4194,
                start: 1684636800000, 
                duration: 86400000,
                tickets: [
                    { name: "General Admission", price: 500, available_tickets: 700 },
                    { name: "VIP Access", price: 1200, available_tickets: 150 }
                ]
            },
            {
                name: "AI & Ethics Symposium",
                description: "A deep dive into the ethical implications of AI.",
                type: "conference",
                category: "technology",
                tags: ["AI", "ethics", "society"],
                location_name: "Innovation Center",
                location_lat: 40.7128,
                location_long: -74.0060,
                start: 1686230400000, 
                duration: 86400000,
                tickets: [
                    { name: "Standard Ticket", price: 300, available_tickets: 500 }
                ]
            }
        ]
    },
    {
        name: "Mindful Living Collective",
        email: "contact@mindfulliving.com",
        bio: "Helping people find balance and wellness.",
        disp_email: "support@mindfulliving.com",
        disp_phone_number: "(777) 345-6789",
        picture: "mindfulliving.jpg",
        banner: "mindfulliving_banner.jpg",
        events: [
            {
                name: "Yoga & Meditation Retreat",
                description: "A weekend retreat to relax and rejuvenate.",
                type: "workshop",
                category: "education",
                tags: ["yoga", "meditation", "wellness"],
                location_name: "Tranquil Mountain Resort",
                location_lat: 45.1234,
                location_long: -75.1234,
                start: 1685731200000, 
                duration: 5400000,
                tickets: [
                    { name: "Full Retreat Pass", price: 600, available_tickets: 100 }
                ]
            }
        ]
    },
    {
        name: "Elite Sports Network",
        email: "sports@elitesports.com",
        bio: "Hosting top-tier sporting events worldwide.",
        disp_email: "contact@elitesports.com",
        disp_phone_number: "(888) 456-7890",
        picture: "elitesports.jpg",
        banner: "elitesports_banner.jpg",
        events: [
            {
                name: "Extreme Adventure Race",
                description: "An intense obstacle course for the daring.",
                type: "festival",
                category: "sport",
                tags: ["adventure", "competition", "fitness"],
                location_name: "Mountain Valley Park",
                location_lat: 39.7392,
                location_long: -104.9903,
                start: 1685030400000, 
                duration: 7200000,
                tickets: [
                    { name: "Participant Ticket", price: 150, available_tickets: 2000 },
                    { name: "Spectator Pass", price: 40, available_tickets: 5000 }
                ]
            }
        ]
    }
];


/** @type{Organizer[]}*/
const sinatra = [
    {
        name: "Epic Gatherings",
        email: "info@epicgatherings.com",
        bio: "Creating unforgettable experiences for all audiences.",
        disp_email: "contact@epicgatherings.com",
        disp_phone_number: "(111) 987-6543",
        picture: "epicgatherings.jpg",
        banner: "epicgatherings_banner.jpg",
        events: [
            {
                name: "Summer Jam Music Fest",
                description: "A weekend of music, food, and fun.",
                type: "festival",
                category: "music",
                tags: ["festival", "live music", "outdoor"],
                location_name: "Sunshine Park",
                location_lat: 35.6895,
                location_long: 139.6917,
                start: 1685001600000, 
                duration: 43200000,
                tickets: [
                    { name: "General Pass", price: 99, available_tickets: 5000 },
                    { name: "VIP Pass", price: 300, available_tickets: 500 }
                ]
            }
        ]
    },
    {
        name: "Think Big Conferences",
        email: "hello@thinkbig.com",
        bio: "Inspiring minds with world-class speakers and panels.",
        disp_email: "events@thinkbig.com",
        disp_phone_number: "(222) 765-4321",
        picture: "thinkbig.jpg",
        banner: "thinkbig_banner.jpg",
        events: [
            {
                name: "Future of AI Summit",
                description: "Discussing the next decade of artificial intelligence.",
                type: "conference",
                category: "technology",
                tags: ["AI", "tech", "innovation"],
                location_name: "Tech Convention Center",
                location_lat: 37.7749,
                location_long: -122.4194,
                start: 1684396800000, 
                duration: 5400000,
                tickets: [
                    { name: "Standard Ticket", price: 400, available_tickets: 800 },
                    { name: "VIP Access", price: 1000, available_tickets: 100 }
                ]
            }
        ]
    },
    {
        name: "Creative Minds Collective",
        email: "contact@creativeminds.com",
        bio: "Celebrating the arts with workshops and showcases.",
        disp_email: "support@creativeminds.com",
        disp_phone_number: "(333) 555-9999",
        picture: "creativeminds.jpg",
        banner: "creativeminds_banner.jpg",
        events: [
            {
                name: "Modern Art Expo",
                description: "A showcase of contemporary and abstract art.",
                type: "festival",
                category: "art",
                tags: ["art", "exhibition", "culture"],
                location_name: "Downtown Art Gallery",
                location_lat: 40.7128,
                location_long: -74.0060,
                start: 1683676800000, 
                duration: 21600000,
                tickets: [
                    { name: "Entry Pass", price: 50, available_tickets: 2000 }
                ]
            },
            {
                name: "Photography Masterclass",
                description: "Learn advanced techniques from top photographers.",
                type: "workshop",
                category: "art",
                tags: ["photography", "training", "art"],
                location_name: "Creative Studio Hub",
                location_lat: 51.5074,
                location_long: -0.1278,
                start: 1685529600000, 
                duration: 57600000,
                tickets: [
                    { name: "Workshop Entry", price: 150, available_tickets: 50 }
                ]
            }
        ]
    },
    {
        name: "Champions League Events",
        email: "sports@championsleague.com",
        bio: "Bringing the most thrilling sports competitions to life.",
        disp_email: "contact@championsleague.com",
        disp_phone_number: "(444) 999-7777",
        picture: "championsleague.jpg",
        banner: "championsleague_banner.jpg",
        events: [
            {
                name: "International Soccer Cup",
                description: "A clash of the world's top football teams.",
                type: "festival",
                category: "sport",
                tags: ["soccer", "sports", "championship"],
                location_name: "National Stadium",
                location_lat: 48.8566,
                location_long: 2.3522,
                start: 1685452800000, 
                duration: 43200000,
                tickets: [
                    { name: "General Admission", price: 75, available_tickets: 10000 },
                    { name: "VIP Lounge", price: 500, available_tickets: 500 }
                ]
            }
        ]
    }
];


/** @type{Organizer[]}*/
const joycelyn = [
    {
        name: "NextGen Events",
        email: "info@nextgenevents.com",
        bio: "Pushing the boundaries of event planning and execution.",
        disp_email: "contact@nextgenevents.com",
        disp_phone_number: "(111) 222-3333",
        picture: "nextgen.jpg",
        banner: "nextgen_banner.jpg",
        events: [
            {
                name: "Future Tech Conference",
                description: "Exploring groundbreaking innovations in technology.",
                type: "conference",
                category: "technology",
                tags: ["AI", "robotics", "innovation"],
                location_name: "Silicon Valley Expo Center",
                location_lat: 37.7749,
                location_long: -122.4194,
                start: 1684358400000, 
                duration: 86400000,
                tickets: [
                    { name: "General Admission", price: 300, available_tickets: 500 },
                    { name: "VIP Experience", price: 800, available_tickets: 100 }
                ]
            }
        ]
    },
    {
        name: "Culture Connect",
        email: "hello@cultureconnect.com",
        bio: "Bringing communities together through arts and culture.",
        disp_email: "events@cultureconnect.com",
        disp_phone_number: "(222) 333-4444",
        picture: "cultureconnect.jpg",
        banner: "cultureconnect_banner.jpg",
        events: [
            {
                name: "Global Film Festival",
                description: "Showcasing films from around the world.",
                type: "festival",
                category: "art",
                tags: ["film", "cinema", "international"],
                location_name: "Sunset Boulevard Theater",
                location_lat: 34.0983,
                location_long: -118.3296,
                start: 1687497600000, 
                duration: 57600000,
                tickets: [
                    { name: "Day Pass", price: 50, available_tickets: 1000 },
                    { name: "VIP All Access", price: 250, available_tickets: 200 }
                ]
            },
            {
                name: "World Music Experience",
                description: "An immersive event celebrating global music styles.",
                type: "concert",
                category: "music",
                tags: ["live music", "world music", "culture"],
                location_name: "City Concert Hall",
                location_lat: 40.7128,
                location_long: -74.0060,
                start: 1688630400000, 
                duration: 28800000,
                tickets: [
                    { name: "Standard", price: 75, available_tickets: 1500 },
                    { name: "VIP", price: 200, available_tickets: 300 }
                ]
            }
        ]
    },
    {
        name: "Elite Sports",
        email: "sports@elitesports.com",
        bio: "Organizing the most thrilling sports competitions worldwide.",
        disp_email: "contact@elitesports.com",
        disp_phone_number: "(333) 444-5555",
        picture: "elitesports.jpg",
        banner: "elitesports_banner.jpg",
        events: [
            {
                name: "Marathon World Series",
                description: "A global marathon competition with elite runners.",
                type: "festival",
                category: "sport",
                tags: ["running", "marathon", "fitness"],
                location_name: "Central Park, NY",
                location_lat: 40.7851,
                location_long: -73.9683,
                start: 1686950400000, 
                duration: 43200000,
                tickets: [
                    { name: "Runner Pass", price: 100, available_tickets: 2000 },
                    { name: "Spectator Pass", price: 30, available_tickets: 5000 }
                ]
            }
        ]
    },
    {
        name: "Innovation Hub",
        email: "info@innovationhub.com",
        bio: "Leading the way in tech and business innovation events.",
        disp_email: "support@innovationhub.com",
        disp_phone_number: "(444) 555-6666",
        picture: "innovationhub.jpg",
        banner: "innovationhub_banner.jpg",
        events: [
            {
                name: "Startup Founders Summit",
                description: "Connecting the brightest minds in startups and entrepreneurship.",
                type: "conference",
                category: "technology",
                tags: ["startups", "business", "networking"],
                location_name: "San Francisco Tech Hub",
                location_lat: 37.7749,
                location_long: -122.4194,
                start: 1686950400000,
                duration: 43200000,
                tickets: [
                    { name: "General Pass", price: 400, available_tickets: 300 },
                    { name: "Investor Access", price: 1200, available_tickets: 50 }
                ]
            }
        ]
    }
];


/** @type{Organizer[]}*/
const parker = [
    {
        name: "EventMasters",
        email: "info@eventmasters.com",
        bio: "Bringing the best events to life with expert planning.",
        disp_email: "contact@eventmasters.com",
        disp_phone_number: "(123) 555-7890",
        picture: "eventmasters.jpg",
        banner: "eventmasters_banner.jpg",
        events: [
            {
                name: "Summer Music Fest",
                description: "An open-air festival featuring top artists.",
                type: "festival",
                category: "music",
                tags: ["festival", "live music", "summer"],
                location_name: "Central Park",
                location_lat: 40.7851,
                location_long: -73.9683,
                start: 1688630400000,
                duration: 28800000,
                tickets: [
                    { name: "General Admission", price: 80, available_tickets: 3000 },
                    { name: "VIP", price: 200, available_tickets: 500 }
                ]
            }
        ]
    },
    {
        name: "InnovateTech",
        email: "hello@innovatetech.com",
        bio: "Leading conferences on the future of technology.",
        disp_email: "events@innovatetech.com",
        disp_phone_number: "(987) 654-3210",
        picture: "innovatetech.jpg",
        banner: "innovatetech_banner.jpg",
        events: [
            {
                name: "Blockchain Revolution",
                description: "Exploring the impact of blockchain technology.",
                type: "conference",
                category: "technology",
                tags: ["blockchain", "crypto", "finance"],
                location_name: "Tech Valley Center",
                location_lat: 37.7749,
                location_long: -122.4194,
                start: 1687497600000,
                duration: 57600000,
                tickets: [
                    { name: "Standard Pass", price: 250, available_tickets: 400 },
                    { name: "VIP Pass", price: 700, available_tickets: 100 }
                ]
            }
        ]
    },
    {
        name: "ArtVision",
        email: "art@artvision.com",
        bio: "Creating stunning art exhibitions and workshops.",
        disp_email: "info@artvision.com",
        disp_phone_number: "(555) 123-4567",
        picture: "artvision.jpg",
        banner: "artvision_banner.jpg",
        events: [
            {
                name: "Street Art Exhibition",
                description: "Showcasing the best urban art from around the world.",
                type: "festival",
                category: "art",
                tags: ["graffiti", "street art", "exhibition"],
                location_name: "Downtown Art District",
                location_lat: 34.0522,
                location_long: -118.2437,
                start: 1684358400000,
                duration: 86400000,
                tickets: [
                    { name: "Entry Pass", price: 25, available_tickets: 1500 }
                ]
            },
            {
                name: "Photography Masterclass",
                description: "Learn from the best photographers in the industry.",
                type: "workshop",
                category: "art",
                tags: ["photography", "training", "art"],
                location_name: "Creative Hub Studio",
                location_lat: 51.5074,
                location_long: -0.1278,
                start: 1685452800000,
                duration: 43200000,
                tickets: [
                    { name: "Workshop Entry", price: 100, available_tickets: 50 }
                ]
            }
        ]
    },
    {
        name: "GameWorld Expo",
        email: "expo@gameworld.com",
        bio: "A paradise for gamers, developers, and industry professionals.",
        disp_email: "support@gameworld.com",
        disp_phone_number: "(777) 888-9999",
        picture: "gameworld.jpg",
        banner: "gameworld_banner.jpg",
        events: [
            {
                name: "Esports Championship 2025",
                description: "The ultimate esports showdown featuring top teams!",
                type: "festival",
                category: "sport",
                tags: ["esports", "gaming", "competition"],
                location_name: "Gaming Arena NYC",
                location_lat: 40.7128,
                location_long: -74.0060,
                start: 1685529600000,
                duration: 57600000,
                tickets: [
                    { name: "Standard Ticket", price: 60, available_tickets: 2000 },
                    { name: "VIP Experience", price: 250, available_tickets: 300 }
                ]
            }
        ]
    }
];


/** @type{Organizer[]}*/
const ben = [
    {
        name: "Live Nation",
        email: "contact@livenation.com",
        bio: "Leading concert promoter bringing top artists to the stage.",
        disp_email: "support@livenation.com",
        disp_phone_number: "(123) 456-7890",
        picture: "livenation.jpg",
        banner: "livenation_banner.jpg",
        events: [
            {
                name: "Rock Legends Night",
                description: "A night of classic rock with legendary bands.",
                type: "concert",
                category: "music",
                tags: ["rock", "live music", "festival"],
                location_name: "Madison Square Garden",
                location_lat: 40.7505,
                location_long: -73.9934,
                start: 1683676800000,
                duration: 21600000,
                tickets: [
                    { name: "General Admission", price: 50, available_tickets: 2000 },
                    { name: "VIP", price: 150, available_tickets: 500 }
                ]
            }
        ]
    },
    {
        name: "Tech Future",
        email: "info@techfuture.com",
        bio: "Hosting cutting-edge technology conferences worldwide.",
        disp_email: "events@techfuture.com",
        disp_phone_number: "(456) 789-0123",
        picture: "techfuture.jpg",
        banner: "techfuture_banner.jpg",
        events: [
            {
                name: "AI Innovations 2025",
                description: "A conference on the latest advancements in AI.",
                type: "conference",
                category: "technology",
                tags: ["AI", "innovation", "conference"],
                location_name: "Silicon Valley Convention Center",
                location_lat: 37.3875,
                location_long: -122.0575,
                start: 1684396800000,
                duration: 5400000,
                tickets: [
                    { name: "Standard Pass", price: 299, available_tickets: 500 },
                    { name: "VIP Pass", price: 799, available_tickets: 100 }
                ]
            },
            {
                name: "Cybersecurity Summit",
                description: "Discussing modern cybersecurity challenges and solutions.",
                type: "conference",
                category: "technology",
                tags: ["cybersecurity", "IT", "network security"],
                location_name: "San Francisco Tech Hub",
                location_lat: 37.7749,
                location_long: -122.4194,
                start: 1685001600000,
                duration: 43200000,
                tickets: [
                    { name: "Basic Pass", price: 199, available_tickets: 300 },
                    { name: "Premium Pass", price: 499, available_tickets: 50 }
                ]
            }
        ]
    },
    {
        name: "ArtSphere",
        email: "hello@artsphere.com",
        bio: "Curating art experiences and workshops for enthusiasts.",
        disp_email: "contact@artsphere.com",
        disp_phone_number: "(789) 012-3456",
        picture: "artsphere.jpg",
        banner: "artsphere_banner.jpg",
        events: [
            {
                name: "Modern Art Showcase",
                description: "An exhibition featuring contemporary artists.",
                type: "festival",
                category: "art",
                tags: ["modern art", "gallery", "exhibition"],
                location_name: "New York Art Hall",
                location_lat: 40.7128,
                location_long: -74.0060,
                start: 1685030400000,
                duration: 7200000,
                tickets: [
                    { name: "Day Pass", price: 30, available_tickets: 1000 },
                    { name: "Weekend Pass", price: 75, available_tickets: 500 }
                ]
            },
            {
                name: "Sculpting Workshop",
                description: "Hands-on session with professional sculptors.",
                type: "workshop",
                category: "art",
                tags: ["sculpting", "handmade", "craft"],
                location_name: "Downtown Art Studio",
                location_lat: 40.7306,
                location_long: -73.9352,
                start: 1685731200000,
                duration: 5400000,
                tickets: [
                    { name: "Standard Entry", price: 50, available_tickets: 30 }
                ]
            }
        ]
    },
    {
        name: "Sports World",
        email: "info@sportsworld.com",
        bio: "Organizing thrilling sporting events worldwide.",
        disp_email: "events@sportsworld.com",
        disp_phone_number: "(222) 333-4444",
        picture: "sportsworld.jpg",
        banner: "sportsworld_banner.jpg",
        events: [
            {
                name: "National Basketball Championship",
                description: "The top teams competing for the national title!",
                type: "festival",
                category: "sport",
                tags: ["basketball", "championship", "sports"],
                location_name: "Staples Center",
                location_lat: 34.0430,
                location_long: -118.2673,
                start: 1686230400000,
                duration: 86400000,
                tickets: [
                    { name: "Regular Seat", price: 75, available_tickets: 5000 },
                    { name: "VIP Courtside", price: 500, available_tickets: 100 }
                ]
            }
        ]
    }
];

/** @type{Organizer[]}*/
const everything = [
    {
        name: "Hockey Rink",
        email: "hockey.rink@hotmail.com",

        // optional stuff
        bio: "We are the hokey rink downtown! come visit us for many stuff n things",
        disp_email: "hockey.rink@hotmail.com",
        disp_phone_number: "111-222-1234",
        banner: "https://m.media-amazon.com/images/I/61lo8QevtJL._AC_UF894,1000_QL80_.jpg",
        picture: "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQb4q7IaES8N3PHuqhiio2997LyGArLlwdNQQ&s",

        // this is a list of events
        events: [
            {
                name: "The Hockey Team Game",
                description: "Come join us for this very exciting hokey game!!!",
                category: "sport",
                type: "stadium",
                tags: ["Hokey", "Indoors", "Hometown", "Sports", "Ice", "Hotdog"],
                picture: "https://m.media-amazon.com/images/I/61lo8QevtJL._AC_UF894,1000_QL80_.jpg",
                //optional
                metadata: { background_color: "#FF0000" },
                //optional
                location_name: "Icerink downtown St.Catharines",
                //optional
                location_lat: undefined,
                location_long: undefined,
                // optional
                available_total_tickets: 100,

                // list of tickets
                tickets: [
                    {
                        name: "General",
                        price: 9.99
                    },
                    {
                        name: "The Big Boxes At The Back",
                        price: 500,
                        available_tickets: 10
                    }
                ]
            },
        ]
    }
]

class Ticket {
    /** @type{string} */name
    /** @type{number} */price
    /** @type{number?} */available_tickets
}

class OurEvent {
    /** @type{string} */name
    /** @type{string} */description
    /** @type{"concert"|"conference"|"workshop"|"festival"} */type
    /** @type{"sport"|"music"|"technology"|"education"|"art"} */category
    /** @type{string?} */picture
    /** @type{object?} */metadata

    /** @type{number?} */start
    /** @type{number?} */duration

    /** @type{number?} */available_total_tickets

    /** @type{string[]} */tags

    /** @type{string?} */location_name
    /** @type{number?} */location_lat
    /** @type{number?} */location_long

    /** @type{Ticket[]} */tickets
}

class Organizer {
    /** @type{string} */name
    /** @type{string} */email
    /** @type{string?} */bio
    /** @type{string?} */disp_email
    /** @type{string?} */disp_phone_number

    /** @type{string} */picture
    /** @type{string} */banner

    /** @type{OurEvent[]} */events
}


async function create_all_user_defined(){
    await Promise.all([
        create_organizers(jacob),
        create_organizers(joycelyn),
        create_organizers(taylor),
        create_organizers(sinatra),
        create_organizers(ben),
        create_organizers(parker),
        create_organizers(everything)
    ]);
    alert("Finished");
}

let tracker = 0;
function random_image_url(){
    tracker++;
    return `https://picsum.photos/200/300?random=${tracker}`;
}

async function create_random() {
    /** @type{Organizer[]}*/   
    var list = [];
    const orgamizers = 30;
    
    for(let i = 0; i < orgamizers; i ++){
        const organizer = new Organizer();
        organizer.name = chance.name();
        organizer.email = chance.email();
        organizer.bio = chance.paragraph({ sentences: chance.integer({ min: 2, max: 5 }) });
        if(chance.bool())
            organizer.disp_email = chance.email();
        if(chance.bool())
            organizer.disp_phone_number = chance.phone();

        organizer.picture = random_image_url();
        organizer.banner = random_image_url();

        organizer.events = [];
        const events = chance.integer({ min: 2, max: 5 });
        for(let e = 0; e < events; e ++){
            const event = new OurEvent();

            event.name = chance.sentence({ words: chance.integer({ min: 2, max: 5 }) });
            event.description = chance.paragraph({ sentences: chance.integer({ min: 2, max: 5 }) });
            event.category = ["sport", "music", "technology", "education", "art"][chance.integer({min: 0, max: 4})];
            event.type = ["concert", "conference", "workshop", "festival"][chance.integer({min: 0, max: 3})];
        
            event.picture = random_image_url();

            event.start = chance.timestamp()*1000;
            event.duration = chance.integer({ min: 1000*60*5, max: 1000*60*60*12 });

            if(chance.bool())
                event.available_total_tickets = chance.integer({ min: 50, max: 500 });

            if(chance.bool({likelihood: 70})){
                event.location_name = chance.address();
                if(chance.bool({likelihood: 70})){
                    event.location_lat = chance.latitude();
                    event.location_long = chance.longitude();
                }
            }
            
            event.tags = [];
            const tags = chance.integer({ min: 2, max: 10 });
            for(let t = 0; t < tags; t ++){
                let tag;
                do{
                    tag = chance.word();
                }while(event.tags.includes(tag));
                event.tags.push(tag)
            }

            event.tickets = [];
            const tickets = chance.integer({ min: 1, max: 4 });
            for(let t = 0; t < tickets; t ++){
                const ticket = new Ticket();
                ticket.name = chance.sentence({ words: chance.integer({ min: 2, max: 5 }) });
                ticket.price = chance.integer({min: 0, max: 500});
                if(chance.bool())
                    ticket.available_tickets = chance.integer({min: 10, max: 500});

                event.tickets.push(ticket);
            }

            organizer.events.push(event);
        }
        list.push(organizer);
    }

    await create_organizers(list);
    alert("Finished");
}

/**
 * @param {Organizer[]} organizers 
 */
async function create_organizers(organizers) {
    await Promise.all(organizers.map(async(organizer) => {
        await api.user.register(organizer.name, organizer.email, "password");
        const session = await api.user.login(organizer.email, "password");
        await api.user.update_user({
            bio: organizer.bio,
            disp_email: organizer.disp_email,
            disp_phone_number: organizer.disp_phone_number,
        }, session);
        if (organizer.picture != undefined && organizer.picture != null) {
            try {
                const pic = await (await fetch(organizer.picture)).blob();
                await api.user.set_user_picture(pic, session);
            } catch (e) { }
        }
        if (organizer.banner != undefined && organizer.banner != null) {
            try {
                const pic = await (await fetch(organizer.banner)).blob();
                await api.user.set_user_banner_picture(pic, session);
            } catch (e) { }
        }
        await api.organizer.convert_to_organizer_account(session);
        await create_events(organizer.events, session);
    }));
}

/**
 * @param {OurEvent[]} events 
 * @param {string} session 
 */
async function create_events(events, session) {
    if (events == undefined || events == null) return;
    await Promise.all(events.map(async(event) => {
        const id = await api.events.create_event(session);
        await api.events.update_event(id, {
            available_total_tickets: event.available_total_tickets,
            category: event.category,
            description: event.description,
            duration: event.duration,
            location_lat: event.location_lat,
            location_long: event.location_long,
            location_name: event.location_name,
            metadata: event.metadata,
            name: event.name,
            start: event.start,
            type: event.type
        }, session);

        if (event.picture != undefined && event.picture != null) {
            try {
                const pic = await (await fetch(event.picture)).blob();
                await api.events.set_picture(id, pic, session);
            } catch (e) { }
        }
        if (event.tags != undefined && event.tags != null)
            for (const tag of event.tags) {
                try{
                    await api.events.add_event_tag(id, tag, session);
                }catch(e){
                    console.log(event.tags);
                }
            }
        await create_tickets(event.tickets, id, session);
        await api.events.set_draft(id, false, session);
    }));
}

/**
 * 
 * @param {Ticket[]} tickets 
 * @param {number} event_id 
 * @param {string} session 
 */
async function create_tickets(tickets, event_id, session) {
    if (tickets == undefined || tickets == null) return;
    await Promise.all(tickets.map(async(ticket) => {
        //TODO
    }));
}