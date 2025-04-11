"""
    edit_events.py

    A quick little script made just to change things easily in the json file "sample_events.json"
    Keep in mind these .json files are just so we can initialize the database for testing
    purposes during development, they are not intended to be on the actual server or client.
"""

import json
import tkinter as tk
from tkinter import filedialog
import random as rnd

jdata = {}

root = tk.Tk()
root.withdraw()

file_path = filedialog.askopenfilename()

with open(file_path, "r") as file:
    jdata = json.load(file)

rnd.seed() # randomise seed

weirdSymbols = ["\u00e2\u20ac\u201c", "\u00e2\u20ac\u201d", "\u00e2\u20ac\u2122", "\u00e2\u20ac\u2018"]
replacements = ["-",                  ", ",                 "'",                  "'"]

occ = 0 # occurances

for org in jdata:
    
##    org["full_name"] = org["name"]
                             
    for event in org["events"]:

        for i, tag in enumerate(event["tags"]):
##            print(tag, end='')
            event["tags"][i] = tag.replace(" ", "").replace(".", "").replace("-", "").lower()
##            print(tag)
        
##        name = event["name"]
##        description = event["description"]
##        category = event["category"]

##        # All prices need to be ints
##        for ticket in event["tickets"]:
####            print(ticket["price"], " -> ", int(ticket["price"]*1000000))
##            ticket["price"] = int(ticket["price"]*1000000)
##
##        # change category names
##        match(event["category"]):
##            case "Science/Technology":
##                print(event["category"], " -> ", "STEM")
##                event["category"]= "STEM"
##            case "DIY":
##                print(event["category"], " -> ", "Workshop")
##                event["category"]= "Workshop"
##            case "Kids'":
##                print(event["category"], " -> ", "Family")
##                event["category"]= "Family"
##            case "Film Screening":
##                print(event["category"], " -> ", "Film")
##                event["category"]= "Film"
##
##        # Change encoding stuffs
##        for i, sym in enumerate(weirdSymbols):
##            if sym in event["description"]:
##                print("\tIt was \x1b[33m", sym, "\x1b[0m in \t", event["description"], end="");
##                print()
##                print("\t\t\t\t\t\t", event["description"].replace(sym, "\x1b[1;4;34m"+replacements[i]+"\x1b[0m"), end="");
##                print()
##                event["description"] = event["description"].replace(sym, replacements[i])
##                occ += 1

##print("\nNumber of occurances: ", occ)       

        

with open("output.json", "w") as file:
    json.dump(jdata, file, indent=4)

print("re-wrote all specified entries to output.json")
