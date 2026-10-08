from typing import Optional, List, Dict, Any
from pydantic import BaseModel, Field, model_validator
import uuid

class AnalyzeRequest(BaseModel):
    message: Optional[str] = None
    url: Optional[str] = None
    qr_payload: Optional[str] = None
    payee: Optional[str] = None
    amount: Optional[float] = Field(default=None, ge=0.0)
    currency: str = Field(default="INR")
    language: str = Field(default="en")

    @model_validator(mode='after')
    def check_not_empty(self) -> 'AnalyzeRequest':
        if not any([
            self.message, 
            self.url, 
            self.qr_payload, 
            self.payee, 
            self.amount is not None
        ]):
            raise ValueError("Request must contain at least one piece of evidence (message, url, qr_payload, payee, or amount).")
        return self

class AnalyzeResponse(BaseModel):
    interaction_id: str
    risk_level: str
    score: int
    signals: List[str]
    reasons: List[str]
    recommendation: str
    coverage: Dict[str, str]
