from fastapi import FastAPI, UploadFile, File, Form, HTTPException
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
        from backend.engine.message_shield import analyze_message, map_score_to_risk
        from backend.engine.correlation import correlate_context
        
        msg_result = analyze_message(request.message)
        
        # Base Message Shield results
        score = msg_result["score"]
        signals = msg_result["signals"]
        reasons = msg_result["reasons"]
        
        # Context Correlation
        # Only check payment mismatch if payment data is provided
        if request.payee or request.amount is not None:
            corr_result = correlate_context(request.message, request.payee, request.amount)
            score += corr_result["correlation_score"]
            signals.extend(corr_result["correlation_signals"])
            
        risk_level, recommendation = map_score_to_risk(score)
        
        # Multilingual Explanation Layer
        from backend.engine.explanations import get_reason
        reasons = [get_reason(sig, request.language) for sig in signals]
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

@app.post("/analyze/image")
async def analyze_image(
    image: UploadFile = File(...),
    language: str = Form("en")
):
    # Validate image format
    allowed_types = ["image/png", "image/jpeg", "image/webp"]
    if image.content_type not in allowed_types:
        raise HTTPException(status_code=400, detail="Unsupported image format. Allowed formats: PNG, JPEG, WEBP.")
    
    # Read image bytes and size limit (10 MB)
    MAX_SIZE = 10 * 1024 * 1024
    image_bytes = await image.read()
    if not image_bytes:
        raise HTTPException(status_code=400, detail="Empty image uploaded.")
    if len(image_bytes) > MAX_SIZE:
        raise HTTPException(status_code=400, detail="Image is too large. Maximum allowed size is 10 MB.")
    
    # OCR processing
    from backend.engine.ocr import extract_text_from_image
    try:
        extracted_text = extract_text_from_image(image_bytes, language)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))
    except Exception:
        raise HTTPException(status_code=500, detail="OCR processing failed.")
    
    # Run Message Shield on extracted text
    from backend.engine.message_shield import analyze_message, map_score_to_risk
    msg_result = analyze_message(extracted_text)
    score = msg_result["score"]
    signals = msg_result["signals"]
    
    # No payment context in this endpoint, so skip correlation
    risk_level, recommendation = map_score_to_risk(score)
    
    # Multilingual explanations
    from backend.engine.explanations import get_reason
    reasons = [get_reason(sig, language) for sig in signals]
    
    coverage = {
        "message": "checked_via_ocr",
        "url": "not_provided",
        "qr": "not_provided",
        "payment": "not_provided"
    }
    
    return {
        "interaction_id": str(uuid.uuid4()),
        "risk_level": risk_level,
        "score": score,
        "signals": signals,
        "reasons": reasons,
        "recommendation": recommendation,
        "coverage": coverage,
        "input_source": "image",
        "extracted_text": extracted_text
    }
