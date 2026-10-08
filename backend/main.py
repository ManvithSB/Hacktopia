from fastapi import FastAPI
import uuid
from backend.schemas import AnalyzeRequest, AnalyzeResponse

app = FastAPI(title="ScamShield Backend", description="Hackatopia 2K26 FT-01")

@app.get("/health")
def health_check():
    return {"status": "ok"}

@app.post("/analyze", response_model=AnalyzeResponse)
def analyze(request: AnalyzeRequest):
    # Temporary placeholder response as per FIRST MILESTONE requirements.
    # Logic for actual scam detection is NOT implemented yet.
    
    coverage = {
        "message": "checked" if request.message else "not_provided",
        "url": "checked" if request.url else "not_provided",
        "qr": "checked" if request.qr_payload else "not_provided",
        "payment": "checked" if (request.payee or request.amount is not None) else "not_provided"
    }

    return AnalyzeResponse(
        interaction_id=str(uuid.uuid4()),
        risk_level="SAFE",
        score=0,
        signals=[],
        reasons=["Temporary placeholder: Intelligence modules not yet implemented."],
        recommendation="PROCEED",
        coverage=coverage
    )
