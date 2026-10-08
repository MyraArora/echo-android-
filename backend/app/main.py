from fastapi import FastAPI, HTTPException, Status
from fastapi.middleware.cors import CORSMiddleware
import firebase_admin
from firebase_admin import credentials, firestore
from pydantic import BaseModel
from app.config import settings

# Initialize Firebase Admin SDK using Google Default Credentials (auto-authenticated in Cloud Run)
if not firebase_admin._apps:
    firebase_admin.initialize_app()

db = firestore.client()

app = FastAPI(
    title="Cognitive Decline Tracking Engine API",
    version="1.0.0",
    description="Backend API for speech extraction and longitudinal analysis."
)

# Allow cross-origin requests for local testing & Web Dashboard integration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class ProcessSessionRequest(BaseModel):
    user_id: str
    session_id: str

@app.get("/")
def health_check():
    """Simple status check to verify deployment on Cloud Run."""
    return {"status": "online", "service": "cognitive-decline-api"}

@app.post("/api/v1/process-session", status_code=Status.HTTP_202_ACCEPTED)
def process_session(payload: ProcessSessionRequest):
    """
    Triggered by Android app after audio upload to GCS.
    Registers the job in Firestore for processing.
    """
    session_ref = db.collection("users").document(payload.user_id).collection("sessions").document(payload.session_id)
    doc = session_ref.get()

    if not doc.exists:
        raise HTTPException(status_code=404, detail="Session record not found in Firestore.")

    # Mark status as processing (Phase 2 will handle feature extraction steps here)
    session_ref.update({
        "status": "PROCESSING",
        "updated_at": firestore.SERVER_TIMESTAMP
    })

    return {
        "message": f"Session {payload.session_id} accepted for processing.",
        "user_id": payload.user_id,
        "status": "PROCESSING"
    }
