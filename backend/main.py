from fastapi import FastAPI
import uuid
from backend.schemas import AnalyzeRequest, AnalyzeResponse

app = FastAPI(title="ScamShield Backend", description="Hackatopia 2K26 FT-01")

@app.get("/health")
def health_check():
    return {"status": "ok"}

@app.post("/analyze", response_model=AnalyzeResponse)
def analyze(request: AnalyzeRequest):
    coverage = {
        "message": "checked" if request.message else "not_provided",
        "url": "checked" if request.url else "not_provided",
        "qr": "checked" if request.qr_payload else "not_provided",
        "payment": "checked" if (request.payee or request.amount is not None) else "not_provided"
    }

    # Run Message Shield if message is provided
    if request.message:
        from backend.engine.message_shield import analyze_message
        msg_result = analyze_message(request.message)
        score = msg_result["score"]
        signals = msg_result["signals"]
        reasons = msg_result["reasons"]
        risk_level = msg_result["risk_level"]
        recommendation = msg_result["recommendation"]
    else:
        score = 0
        signals = []
        reasons = ["No message provided."]
        risk_level = "SAFE"
        recommendation = "PROCEED"

    return AnalyzeResponse(
        interaction_id=str(uuid.uuid4()),
        risk_level=risk_level,
        score=score,
        signals=signals,
        reasons=reasons,
        recommendation=recommendation,
        coverage=coverage
    )
