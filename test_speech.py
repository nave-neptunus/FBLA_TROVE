import os
import urllib.request
import json
import ssl

ssl._create_default_https_context = ssl._create_unverified_context

api_key = os.environ.get('GOOGLE_PLACES_API_KEY')
if not api_key:
    if os.path.exists('.env'):
        with open('.env', 'r') as f:
            for line in f:
                if line.startswith('GOOGLE_PLACES_API_KEY='):
                    api_key = line.split('=', 1)[1].strip()
                    break
    api_key = os.environ.get('GOOGLE_PLACES_API_KEY')

if not api_key:
    print("NO_KEY")
    exit(0)

# Create a dummy payload
payload = {
    "config": {
        "encoding": "LINEAR16",
        "sampleRateHertz": 16000,
        "languageCode": "en-US"
    },
    "audio": {
        "content": "UklGRjIAAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YRAAAABWAAAA" # tiny empty wav
    }
}

req = urllib.request.Request(
    f'https://speech.googleapis.com/v1/speech:recognize?key={api_key}',
    data=json.dumps(payload).encode('utf-8'),
    headers={'Content-Type': 'application/json'}
)

try:
    with urllib.request.urlopen(req) as response:
        result = response.read()
        print("SUCCESS:", result.decode('utf-8'))
except Exception as e:
    print("ERROR:", e)
    if hasattr(e, 'read'):
        print(e.read().decode('utf-8'))
