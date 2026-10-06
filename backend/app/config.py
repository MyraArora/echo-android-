import os
from dotenv import load_dotenv

load_dotenv()

class Settings:
    PROJECT_ID: str = os.getenv("GCP_PROJECT_ID", "your-firebase-project-id")
    AUDIO_BUCKET_NAME: str = os.getenv("AUDIO_BUCKET_NAME", "cognitive-decline-audio-uploads")

settings = Settings()
