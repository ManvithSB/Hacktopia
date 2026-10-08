import re
from typing import Optional, List, Dict, Any, Tuple

CORRELATION_WEIGHTS = {
    "amount_mismatch": 20,
    "payee_mismatch": 25,
    "payment_context_mismatch": 10
}

CORRELATION_REASONS = {
    "amount_mismatch": "The payment amount does not match the amount mentioned in the message.",
    "payee_mismatch": "The payment recipient does not match the recipient described in the message.",
    "payment_context_mismatch": "The payment request does not fully match the context described in the message."
}

def extract_amounts(message: str) -> List[float]:
    """
    Extract monetary amounts from the message using deterministic regex.
    Supports INR formats: ₹850, ₹ 850, Rs. 850, Rs 850, INR 850, 850 rupees, 850 रुपये, ₹1,000
    """
    if not message:
        return []
    
    # Matches patterns like ₹850, Rs. 850, Rs 850, INR 850
    # Also handles commas like 1,000
    # (?:₹|rs\.?|inr)\s*([\d,]+(?:\.\d{1,2})?)
    pattern_prefix = r'(?:₹|rs\.?|inr)\s*([\d,]+(?:\.\d+)?)'
    
    # Matches patterns like 850 rupees, 850 रुपये
    pattern_suffix = r'([\d,]+(?:\.\d+)?)\s*(?:rupees|रुपये)'
    
    amounts = []
    
    # Find all prefix matches
    for match in re.finditer(pattern_prefix, message, flags=re.IGNORECASE):
        val_str = match.group(1).replace(',', '')
        try:
            amounts.append(float(val_str))
        except ValueError:
            pass
            
    # Find all suffix matches
    for match in re.finditer(pattern_suffix, message, flags=re.IGNORECASE):
        val_str = match.group(1).replace(',', '')
        try:
            amounts.append(float(val_str))
        except ValueError:
            pass
            
    return amounts

def normalize_payee(payee: str) -> str:
    """Normalize payee strings for comparison."""
    if not payee:
        return ""
    payee = payee.lower()
    # Remove obvious punctuation
    payee = re.sub(r'[^\w\s]', '', payee)
    # Collapse repeated whitespace
    payee = re.sub(r'\s+', ' ', payee)
    return payee.strip()

def extract_payees(message: str) -> List[str]:
    """
    Extract payee candidates from the message using simple patterns.
    Examples:
    "Pay ABC Utilities ₹850" -> ABC Utilities
    "send ₹500 to Rahul" -> Rahul
    "transfer ₹1000 to XYZ" -> XYZ
    "pay to ABC Bank" -> ABC Bank
    "payment to ABC Electricity" -> ABC Electricity
    "payable to ABC Utilities" -> ABC Utilities
    """
    if not message:
        return []
        
    candidates = []
    
    # "send [amount] to [payee]" or "transfer [amount] to [payee]"
    # "pay to [payee]", "payment to [payee]", "payable to [payee]"
    # We will match " to [payee]" preceded by payment words
    to_patterns = [
        r'(?:send|transfer|pay|payment|payable)(?:\s+(?:₹|rs\.?|inr)?\s*[\d,.]+)?\s+to\s+([A-Za-z0-9\s]+?)(?:\s+₹|\s+rs|\.|$)'
    ]
    
    for pat in to_patterns:
        for match in re.finditer(pat, message, flags=re.IGNORECASE):
            candidate = match.group(1).strip()
            # Try to cut off extra stuff if it matched too far
            words = candidate.split()
            # Just take up to 3-4 words as a heuristic payee name to prevent matching the rest of the sentence
            candidate = " ".join(words[:4])
            if candidate:
                candidates.append(candidate)
                
    # "Pay [payee] [amount]"
    pay_amount_pattern = r'(?:pay)\s+([A-Za-z0-9\s]+?)\s+(?:₹|rs\.?|inr)\s*[\d,.]+'
    ignore_words = {"your", "my", "the", "a", "an", "bill", "of", "for"}
    for match in re.finditer(pay_amount_pattern, message, flags=re.IGNORECASE):
        candidate = match.group(1).strip()
        words = candidate.split()
        
        # Check if candidate looks like a generic phrase rather than a payee
        if any(w.lower() in ignore_words for w in words):
            continue
            
        candidate = " ".join(words[:4])
        if candidate:
            candidates.append(candidate)
            
    # Check for implicit context like "electricity bill", "water bill", "traffic fine", "lottery prize"
    context_patterns = [
        r'([a-zA-Z0-9]+)\s+(?:bill|fine|challan)\b',
        r'(?:lottery|prize|kyc|bank)'
    ]
    for pat in context_patterns:
        for match in re.finditer(pat, message, flags=re.IGNORECASE):
            # If it's the group pattern, use group(1), else the whole match
            candidate = match.group(1).strip() if match.groups() else match.group(0).strip()
            if candidate.lower() not in ignore_words:
                candidates.append(candidate)
                
    return candidates

def check_amount_mismatch(message_amounts: List[float], request_amount: Optional[float]) -> bool:
    """Returns True if there is a clear mismatch, False otherwise."""
    if not message_amounts or request_amount is None:
        return False
        
    # If request_amount is exactly one of the extracted amounts, it's not a mismatch
    if any(abs(amt - request_amount) < 0.01 for amt in message_amounts):
        return False
        
    return True

def check_payee_mismatch(message_payees: List[str], request_payee: Optional[str]) -> bool:
    """Returns True if there is a clear mismatch, False otherwise."""
    if not message_payees or not request_payee:
        return False
        
    norm_req = normalize_payee(request_payee)
    if not norm_req:
        return False
        
    for p in message_payees:
        norm_p = normalize_payee(p)
        if not norm_p:
            continue
        # Substring containment
        if norm_req in norm_p or norm_p in norm_req:
            return False
            
    return True

def correlate_context(message: str, payee: Optional[str], amount: Optional[float]) -> Dict[str, Any]:
    """
    Correlates the message context with structured payment data.
    """
    signals = []
    
    message_amounts = extract_amounts(message)
    message_payees = extract_payees(message)
    
    has_amount_mismatch = check_amount_mismatch(message_amounts, amount)
    has_payee_mismatch = check_payee_mismatch(message_payees, payee)
    
    if has_amount_mismatch:
        signals.append("amount_mismatch")
    if has_payee_mismatch:
        signals.append("payee_mismatch")
        
    if has_amount_mismatch and has_payee_mismatch:
        signals.append("payment_context_mismatch")
        
    score = sum(CORRELATION_WEIGHTS.get(sig, 0) for sig in signals)
    reasons = [CORRELATION_REASONS[sig] for sig in signals if sig in CORRELATION_REASONS]
    
    return {
        "correlation_signals": signals,
        "correlation_score": score,
        "correlation_reasons": reasons
    }
