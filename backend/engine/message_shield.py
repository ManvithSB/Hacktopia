import re
from typing import List, Tuple, Dict, Any

SIGNAL_KEYWORDS = {
    "urgent_language": [
        "urgent", "immediately", "act now", "today", "within 24 hours", 
        "तुरंत", "अभी", "तुरंत करें", "ತಕ್ಷಣ", "ಈಗಲೇ"
    ],
    "account_threat": [
        "account will be blocked", "account will be suspended", "account suspended", "account deactivated", 
        "kyc failure will block account", "account बंद", "खाता बंद", "खाता ब्लॉक", 
        "ಖಾತೆ ಬ್ಲಾಕ್", "ಖಾತೆ ಸ್ಥಗಿತ"
    ],
    "kyc_pressure": [
        "update kyc", "verify kyc", "kyc expired", "complete kyc", 
        "kyc अपडेट", "kyc सत्यापित", "kyc ನವೀಕರಿಸಿ", "kyc ಪರಿಶೀಲಿಸಿ"
    ],
    "impersonation": [
        "bank official", "bank officer", "customer care", "rbi", 
        "government official", "police", "अधिकारी", "बैंक अधिकारी", "ಬ್ಯಾಂಕ್ ಅಧಿಕಾರಿ"
    ],
    "credential_request": [
        "otp", "pin", "upi pin", "password", "cvv", "card details", 
        "otp भेजें", "pin साझा करें", "otp ಕಳುಹಿಸಿ", "pin ಹಂಚಿಕೊಳ್ಳಿ"
    ],
    "payment_pressure": [
        "pay now", "send money", "transfer money", "pay ₹", "collect request", 
        "payment required", "पैसे भेजें", "भुगतान करें", "ಹಣ ಕಳುಹಿಸಿ", "ಪಾವತಿ ಮಾಡಿ"
    ],
    "suspicious_intent": [
        "verify account by sending", "pay processing fee", "processing fee",
        "send money to unlock account", "scan and pay to receive refund",
        "to verify"
    ],
    "lottery_prize_scam": [
        "lottery", "prize won", "congratulations", "reward claim", 
        "lucky draw", "लॉटरी", "इनाम", "पुरस्कार", "ಲಾಟರಿ", "ಬಹುಮಾನ"
    ],
    "refund_reward_scam": [
        "refund available", "cashback claim", "reward pending", "refund verification", 
        "cashback पाने के लिए", "ಮರುಪಾವತಿ ಪಡೆಯಲು", "ಕ್ಯಾಶ್ಬ್ಯಾಕ್ ಪಡೆಯಲು"
    ]
}

SIGNAL_WEIGHTS = {
    "urgent_language": 15,
    "account_threat": 25,
    "kyc_pressure": 15,
    "impersonation": 20,
    "credential_request": 35,
    "payment_pressure": 20,
    "suspicious_intent": 25,
    "lottery_prize_scam": 25,
    "refund_reward_scam": 15
}

SIGNAL_REASONS = {
    "urgent_language": "The message creates urgency and pressures the user to act quickly.",
    "account_threat": "The message threatens account blocking or suspension.",
    "kyc_pressure": "The message pressures the user to complete or update KYC.",
    "impersonation": "The message appears to impersonate an authority, bank or official service.",
    "credential_request": "The message asks for sensitive credentials such as OTP, PIN or card details.",
    "payment_pressure": "The message pressures the user to make a payment or transfer money.",
    "suspicious_intent": "The message links a financial action to verification, unlocking, refund or reward.",
    "lottery_prize_scam": "The message uses prize or lottery language commonly associated with scams.",
    "refund_reward_scam": "The message uses refund, cashback or reward claims to encourage action."
}

def normalize_text(text: str) -> str:
    """Normalizes whitespace and common punctuation, converts to lowercase."""
    text = text.lower()
    # Replace multiple spaces with a single space
    text = re.sub(r'\s+', ' ', text)
    # Remove common punctuation but keep unicode characters for Hindi/Kannada
    # We will just strip leading/trailing spaces, maybe remove basic punctuation
    text = re.sub(r'[^\w\s₹]', '', text)
    return text.strip()

def detect_signals(text: str) -> List[str]:
    """Detects signals based on deterministic keyword matching."""
    normalized = normalize_text(text)
    detected = set()
    for signal, keywords in SIGNAL_KEYWORDS.items():
        for kw in keywords:
            kw_norm = normalize_text(kw)
            if kw_norm in normalized:
                detected.add(signal)
                break  # If one keyword matches for a signal, no need to check others
    return list(detected)

def score_message(signals: List[str]) -> int:
    """Calculates risk score from unique signals to prevent duplicates inflating it."""
    return sum(SIGNAL_WEIGHTS.get(signal, 0) for signal in signals)

def map_score_to_risk(score: int) -> Tuple[str, str]:
    """Maps the score to a risk level and recommendation."""
    if score < 30:
        return "SAFE", "PROCEED"
    elif score < 60:
        return "SUSPICIOUS", "VERIFY"
    else:
        return "HIGH_RISK", "STOP"

def generate_reasons(signals: List[str]) -> List[str]:
    """Generates human-readable reasons for detected signals."""
    return [SIGNAL_REASONS[signal] for signal in signals if signal in SIGNAL_REASONS]

def analyze_message(message: str) -> Dict[str, Any]:
    """Main entrypoint for message shield analysis."""
    if not message or not message.strip():
        return {
            "score": 0,
            "signals": [],
            "reasons": [],
            "risk_level": "SAFE",
            "recommendation": "PROCEED"
        }
    
    signals = detect_signals(message)
    score = score_message(signals)
    risk_level, recommendation = map_score_to_risk(score)
    reasons = generate_reasons(signals)
    
    return {
        "score": score,
        "signals": signals,
        "reasons": reasons,
        "risk_level": risk_level,
        "recommendation": recommendation
    }
