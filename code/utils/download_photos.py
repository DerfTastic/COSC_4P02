"""
    download_photos.py

    Downloads external profile photos from URLs in a json and then renames them with the images' download destination
    to avoid cross-origin blocking
"""

import json
import sys
import tkinter as tk
from tkinter import filedialog
import random as rnd
import requests
from urllib.parse import urlparse
import os
from pathlib import Path

rnd.seed() # randomise seed

output_img_dir = "./code/client/images/sample_pfps/"
os.makedirs(output_img_dir, exist_ok=True)

root = tk.Tk()
root.withdraw()

input_file_path = filedialog.askopenfilename(title="Download from?", initialdir="./code/client/js/")
# input_file_path = "./code/client/js/sample_events.json"

jdata = {}

with open(input_file_path, "r") as input_file:
    jdata = json.load(input_file)

# Iterate through input file
for i, org in enumerate(jdata):

    response = requests.get(org["picture"])
    response.raise_for_status()

    # Parse path components
    parsed = urlparse(org["picture"])
    parts = parsed.path.strip("/").split("/")

    if len(parts) >= 2:
        subfolder = parts[-2]
        filename = parts[-1]
        save_name = f"{subfolder}_{filename}"
    else:
        save_name = os.path.basename(parsed.path)

    save_path = os.path.join(output_img_dir, save_name)

    # Write the image to the file
    with open(save_path, 'wb') as f:
        f.write(response.content)

    org["banner"] = org["events"][rnd.randint(0, len(org["events"]))-1]["picture"]
    path = Path(save_path)
    client_path = "/" + str(Path(*path.parts[2:])).replace("\\", "/")
    print(client_path)
    org["picture"] = client_path

print("Downloaded images to {0}".format(output_img_dir))


# Write back to the file

file = open("./code/utils/output.json", "w")
file.write(json.dumps(jdata, indent=4))

print("Changed URL names in {0}".format("output.json"))