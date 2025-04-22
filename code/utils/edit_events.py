"""
    edit_events.py

    A quick little script made just to change things easily in the json file "sample_events.json"
    Keep in mind these .json files are just so we can initialize the database for testing
    purposes during development, they are not intended to be on the actual server or client.
"""

import json
import sys
import tkinter as tk
from tkinter import filedialog
import random as rnd
from openai import OpenAI
import requests

# Additional Setup of libraries

# Using GPT-4o-mini to get longer descriptions
AImodel = "gpt-4o-mini"
client = OpenAI(
    api_key=sys.argv[1] # Feed API key as runtime config
)

rnd.seed() # randomise seed


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

def get10Descriptions(descList):
    descListStr = "\n".join([str(d+1)+". "+desc for d, desc in enumerate(descList)])
    return AIResponse(
        # System message setup
        "You make event descriptions for an event-ticketing website. Don't make the description sound similar to each other.",
        # Questions
        ["Make these {0} event descriptions to about 100 words long: \n{1}".format(len(descList), descListStr)],
        # Format
        {"format": {
            "type": "json_schema",
            "name": "longer_descriptions",
            "schema": {
                "type": "object",
                "properties": {
                    "longer_descriptions": { "type": "array", "items": { "type": "string" } },
                },
                "required": ["longer_descriptions"],
                "additionalProperties": False,
            },
            "strict": True
        }}
    )

# Actual program

output_write_file = "./code/utils/output.json"

input_jdata = {}

root = tk.Tk()
root.withdraw()

# input_file_path = filedialog.askopenfilename(title="Edit which file?", initialdir="./code/client/js/")
input_file_path = "./code/client/js/sample_events.json"

with open(input_file_path, "r") as input_file:
    input_jdata = json.load(input_file)

client_event_img_dir = "/images/sample_events_photos/"

# Define the illegal encoding symbols due to unicodes stuff
weirdSymbols = ["\u00e2\u20ac\u201c", "\u00e2\u20ac\u201d", "\u00e2\u20ac\u2122", "\u00e2\u20ac\u2018", "> "]
replacements = ["-",                  ", ",                 "'",                  "'",                  ""]

# Get profile photo urls
x = requests.get('https://randomuser.me/api/?results={0}'.format(len(input_jdata)))
xjson = x.json()
pfpURLs = [xjson["results"][img]["picture"]["large"] for img in range(len(input_jdata))]

# Make output file blank first
file = open(output_write_file, "w")
file.write("")
file.close()
file = open(output_write_file, "a")
file.write("[\n")

# Iterate through input file
for i, org in enumerate(input_jdata):
    print("Organizer " + str(i+1))

    # Set new pfp and banner
    org["picture"] = pfpURLs[i]
    org["banner"] = "https://picsum.photos/1366/768?random={0}".format(rnd.randint(0, 100))

    # Make longer descriptions
    newDescs = []
    chunk_size = 10
    allOldDescs = [ev["description"] for j, ev in enumerate(org["events"])]
    for c in range(0, len(allOldDescs), chunk_size):
        descChunk = allOldDescs[c:c + chunk_size]
        print("Generating descs [{0}-{1}]".format(c+1, c+chunk_size))
        newDescs.extend(get10Descriptions(descChunk)["longer_descriptions"])

    # Even out longer descriptions if AI failed to generate enough
    if len(newDescs) < len(org["events"]):
        for nonMatchingI in range(len(newDescs), len(org["events"])):
            newDescs.append(org["events"][nonMatchingI]["description"])

    # Visualization/Debug showcase generated descriptions next to the old ones
    print("\n".join([item for pair in zip(allOldDescs, newDescs) for item in pair]))
                             
    for j, event in enumerate(org["events"]):

        # Link related photos based on type
        event["picture"] = client_event_img_dir + event["type"].lower() + ".jpg"

        # Put longer generated description
        event["description"] = newDescs[j]

        # Remove > in

        # Change encoding stuffs in that new description
        for k, sym in enumerate(weirdSymbols):
            if sym in event["description"]:
                print("\tIt was \x1b[33m", sym, "\x1b[0m in \t", event["description"])
                print("\t\t\t\t\t\t", event["description"].replace(sym, "\x1b[1;4;34m" + replacements[k] + "\x1b[0m"))
                event["description"] = event["description"].replace(sym, replacements[k])

    file.write(json.dumps(org, indent=4))
    if i < len(input_jdata)-1:
        file.write(",")
    file.write("\n")
    file.close()
    file = open(output_write_file, "a")

file.write("]")
file.close()

# with open(output_write_file, "w") as file:
#     json.dump(input_jdata, file, indent=4)

print("re-wrote all specified entries to {0}".format(output_write_file))
