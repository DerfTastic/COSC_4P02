"""
    generate_events.py

    ! The format this generates the JSON in is now outdated.

    This file was the original script created to create the sample events
    that looks like real events and aren't just gibberish. It uses the GPT-4o
    API calls to create full-sentence descriptions.
    
    Keep in mind these .json files are just so we can initialize the database for testing
    purposes during development, they are not intended to be on the actual server or client.
"""

import json
import traceback
import random as rng
from openai import OpenAI
import webbrowser # just to test photos
import os

maxEvents = 200

# Output file
filename = "sample_events.json"
f = open(filename, "w")

# Parameters for event generation

# Categories sorted by how often they occur (first = most events in this category)
categories = ['Music', 'Sports', 'Family', 'Comedy', 'STEM', 'Theater', 'Workshop', 'Community/Culture', 'Film']
# types of events for each category (also pictures)
types = [["Concert", "Meet and greet"],              # Music
         ["Sports game", "Player meet and greet"],   # Sports
         ["Sports practice", "Summer Daycamp", "School play", "Magic show", "Birthday Party"],  # Family
         ["Stand-up show"],                          # Comedy
         ["Conference", "Tradeshow", "Exhibition"],  # STEM
         ["Premiere Show", "Rehearsal", "Play"],     # Theater
         ["Workshop", "Project fair"],               # Workshop
         ["Festival", "Town hall", "Support group"], # Community/Culture
         ["Film festival"]                           # Film
         ]
# The title and name of a given event will be written in its corresponding writing tone,
# which matches the type of event it is. (eg. Kids' event -> "Fun or Silly" tone) 
category_writing_tones = ["artistic",                # Music
                          "informative",             # Sports
                          "fun or silly",            # Family
                          "punny",                   # Comedy
                          "professional or inspiring", # STEM
                          "riveting or Vibrant",     # Theater
                          "crafty and fun",          # Workshop
                          "wholesome and non-chalant", # Community/Culture
                          "elegant and captivating"  # Film
                         ]
# Cities
cities = [
{"name": "St. Catharines, Ontario",     "lat": 43.1583, "lon": -79.2458},
{"name": "New York City, New York",     "lat": 40.712,  "lon": -74.006},
{"name": "Los Angeles, California",     "lat": 34.05,   "lon": -118.25},
{"name": "Toronto, Ontario",            "lat": 43.742,  "lon": -76.373},
{"name": "Chicago, Illinois",           "lat": 41.882,  "lon": -87.628},
{"name": "Houston, Texas",              "lat": 29.763,  "lon": -95.383},
{"name": "London, UK",                  "lat": 51.507,  "lon":  -0.128},
{"name": "Philadelphia, Pennsylvania",  "lat": 39.953,  "lon": -75.164},
{"name": "Calgary, Alberta",            "lat": 51.05,   "lon": -114.067},
{"name": "San Diego, California",       "lat": 32.715,  "lon": -117.163},
{"name": "Ottawa, Ontario",             "lat": 45.425,  "lon":  -75.695}
]

# Ticket types
ticket_type_names = ["Free", "VIP", "General Admission", "Other"]

# Images
urlJPGFolder = "/images/"
imgfileext = ".jpg"


# Additional Setup of libraries

# We will ask GPT-4o-mini to generate the event attributes that are harder to
# just randomize in parts, like getting a good description
AImodel = "gpt-4o-mini"
client = OpenAI(
    api_key = os.environ.get("OPENAI_API_KEY")
)

# Function defintions

# article - Returns "a" or "an" depending on whether the given noun starts with a vowel or not
def article(noun):
    if noun.lower()[0] in ['a', 'e', 'i', 'o', 'u']:
        return "an"
    else:
        return "a"

def AIResponse(systemPrompt, userPromptList, responseFormat):
    msglist = []
    msglist.append({"role": "system", "content": systemPrompt})
    for prompt in userPromptList:
        msglist.append({"role": "user", "content": prompt})

    response = client.responses.create(
      model=AImodel,
      store=True,
      input=msglist,
      text=responseFormat
    )
    return json.loads(response.output_text)


def AIDescPrompt(category, typename, cityname):
    fmtstring = "Write a short description for {0} {1} {2} event in {3} {4} tone that's aimed at its demographic in the city of {5}."
    return fmtstring.format(
        article(typename), typename.lower(),
        category.lower(),                 
        article(category_writing_tones[i_cat]), category_writing_tones[i_cat].lower(),
        cityname)

def getImageURL(typename):
    return urlJPGFolder + typename.lower() + imgfileext

def strlistconcat(thelist):
    return ", ".join(list(map(lambda x:x.center(len(x)+2, '"'), thelist))[:-1]) + " and \"" + thelist[-1] + "\"" if len(thelist) > 1 else '"' + thelist[0] + '"'


# Main Code start
"""
for i in types:
    for j in i:
        webbrowser.open(getImageURL(j));
"""

numOfEventsToGen = 0 # Initialize

# Ask user how many events to generate
validUserResponse = False
while(not validUserResponse):
    response = input("\033[33;1mHow many events do you want to generate? (1-{0}): \033[0m".format(maxEvents))
    try:
        numOfEventsToGen = int(response)
    except ValueError:
        print("\x1b[31;1mNot an integer, try again.\x1b[0m");
    else:
        print(numOfEventsToGen)
        if not (1 <= numOfEventsToGen <= maxEvents):
            print("\x1b[31;1mMust be between 1 and {0}\x1b[0m".format(maxEvents))
        else:
            validUserResponse = True

"""
# Testing
for e in range(0, numOfEventsToGen):
    # Generate category
    i_cat = rng.randrange(len(categories))
    category = categories[i_cat]
    # Generate type
    typename = rng.choice(types[i_cat])
    # Generate city
    city = rng.choice(cities)
    
    print( AIDescPrompt(category, typename, city["name"]) )

"""

# Generation of events

rng.seed() # Randomise seed

f.write("[")
print("[")

for e in range(0, numOfEventsToGen):
    
    # Generate category
    i_cat = rng.randrange(len(categories))
    category = categories[i_cat]
    # Generate type
    typename = rng.choice(types[i_cat])
    # Generate city
    city = rng.choice(cities)

    # Picture (based of type)
    imgURL = getImageURL(typename)

    # AI generated attributes:  description, name, tags, location within city, ticket types, number of total tickets
    resp = AIResponse(
        # System message setup
        "You think of creative and/or sensible details for 10-20 events by a given event organizer that will be displayed on a online event ticketing platform." +
"You will be provided with a type of event, its category, and the writing tone your description and title should be, as well as the city that the event is occuring in." +
"and your goal will be to come up with a short 1-sentence description in the desired writing tone, then the title in that tone, then a list of tags for the event, then the hypothetical name of the venue (i.e. name of the building or park), and finally a list of the different ticket tiers attendees can buy." +
"For each step, just provide the description and title (both in their desired writing tone) in their corresponding fields both as their own strings," +
"then provide a list of strings for the tags field, then provide the venue name in the venue field." + 
"Also provide the event's ticket details, including a list of 1-3 ticket type names " +
"(choices are {0})".format(strlistconcat(ticket_type_names)) + 
"and their prices (floats with .99 at the end) in the 'ticket_types' field, " +
"as well as the total number of event tickets to sell (total_ticket_availability field).",
        # Questions
        [   AIDescPrompt(category, typename, city["name"]),
            "Now write the name for that event",
            "What's good list of tags for this event?",
        ],
        # Format
        {"format": {
            "type": "json_schema",
            "name": "event_details",
            "schema": {
                "type": "object",
                "properties": {
                    "description": { "type": "string" },
                    "name": { "type": "string" },
                    "tags": { "type": "array", "items": { "type": "string" } },
                    "venue": { "type": "string" },
                    "ticket_types": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "name": { "type": "string" },
                                "price": { "type": "number" }
                            },
                            "required": ["name", "price"],
                            "additionalProperties": False
                        }
                    },
                    "total_ticket_availability": { "type": "number" }
                },
                "required": ["description", "name", "tags", "venue", "ticket_types", "total_ticket_availability"],
                "additionalProperties": False
            },
            "strict": True
        }}
    )

    # Loading AI generated attributes into variables
    event_name = resp["name"]
    event_desc = resp["description"]
    event_tags = resp["tags"].lower()
    event_location_name = resp["venue"] + " in " + city["name"]
    # Ticket stuff
    event_total_tickets = resp["total_ticket_availability"]
    event_ticket_types = resp["ticket_types"]
    if len(event_ticket_types) > 1 and event_total_tickets > 30:
        event_ticket_types[0]["available_tickets"] = event_total_tickets/10 # 10% of tickets are tier 1 (highest)
        if len(event_ticket_types) > 2:
            event_ticket_types[1]["available_tickets"] = event_total_tickets/5 # 20% of tickets are tier 2 (second-highest)
    

    # Insert full response into dictionary
    event = {
        "name": event_name,
        "description": event_desc,
        "category": category,
        "type": typename,
        "tags": event_tags,
        "picture": imgURL,
        # Optional:
        "location_name": event_location_name,
        "location_lat": city["lat"],
        "location_long": city["lon"],
        "available_total_tickets": event_total_tickets,
        "tickets": event_ticket_types
    }

    event_json_str = json.dumps(event, indent=4)

    print(event_json_str + ((", \x1b[31m({0}/{1} events generated)\x1b[0m".format(e+1, numOfEventsToGen)) if (e < numOfEventsToGen-1) else ""))    
    f.write(event_json_str + (",\n" if (e < numOfEventsToGen-1) else ""))

f.write("\n]")
print("]")
print("\x1b[32mAll {0} events generated!\x1b[0m".format(numOfEventsToGen))
f.close()
print("Saved in" + filename + ".")
