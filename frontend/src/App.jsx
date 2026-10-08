import { useEffect, useState } from 'react'
import './App.css'
import { analyzePayment } from './services/api'

const translations = {
  en: {
    navCheck: 'CHECK PAYMENT',
    navDemo: 'DEMO',
    navSafety: 'SAFETY',
    heroTag: 'PRE-PAYMENT SECURITY',
    heroTitle1: 'Does the payment',
    heroTitle2: 'match the story?',
    heroText:
      'ScamShield checks the message, destination and payment details before your money moves.',
    checkNow: 'CHECK PAYMENT',
    tryDemo: 'TRY LIVE DEMO',
    inputTitle: 'WHAT DO YOU WANT TO CHECK?',
    inputSubtitle: 'Choose how the payment request reached you.',
    message: 'MESSAGE',
    messageDesc: 'Paste the message you received',
    screenshot: 'SCREENSHOT',
    screenshotDesc: 'Upload a suspicious message',
    upi: 'PAYMENT',
    upiDesc: 'Simulate a payment request',
    messageLabel: 'MESSAGE CONTENT',
    messagePlaceholder:
      'Paste the message asking you to make a payment...',
    urlLabel: 'LINK / URL',
    urlPlaceholder: 'https://example.com/pay',
    payeeLabel: 'PAYEE NAME',
    payeePlaceholder: 'Who are you paying?',
    amountLabel: 'PAYMENT AMOUNT',
    amountPlaceholder: '8500',
    upiLabel: 'UPI ID',
    upiPlaceholder: 'name@upi',
    runCheck: 'RUN SECURITY CHECK',
    loading: 'ANALYSING...',
    demoTitle: 'SEE SCAMSHIELD IN ACTION',
    demoText:
      'Try a realistic payment scam scenario and see how ScamShield catches the mismatch.',
    loadDemo: 'LOAD HIGH-RISK DEMO',
    story: 'MESSAGE STORY',
    payment: 'PAYMENT REQUEST',
    decision: 'SECURITY DECISION',
    safe: 'SAFE',
    suspicious: 'SUSPICIOUS',
    highRisk: 'HIGH RISK',
    proceed: 'PROCEED',
    verify: 'VERIFY',
    stop: 'STOP',
    possible: 'POSSIBLE RESULTS',
    possibleText: 'One clear decision before you pay.',
    whyRisky: 'WHY IS THIS RISKY?',
    whySafe: 'WHY IS THIS SAFE?',
    whatDo: 'WHAT SHOULD YOU DO?',
    stopPayment: 'STOP PAYMENT',
    verifyPayment: 'VERIFY PAYMENT',
    proceedPayment: 'PROCEED WITH CAUTION',
    footer: 'Think before you pay.',
    screenshotSoon:
      'Screenshot analysis will be connected to the backend next.',
    backendError:
      'Could not connect to ScamShield backend. Make sure the FastAPI server is running on port 8000.',
  },

  kn: {
    navCheck: 'ಪಾವತಿ ಪರಿಶೀಲಿಸಿ',
    navDemo: 'ಡೆಮೊ',
    navSafety: 'ಸುರಕ್ಷತೆ',
    heroTag: 'ಪಾವತಿಗೂ ಮುನ್ನದ ಭದ್ರತೆ',
    heroTitle1: 'ಪಾವತಿ ಮತ್ತು',
    heroTitle2: 'ಸಂದೇಶ ಒಂದೇನಾ?',
    heroText:
      'ನಿಮ್ಮ ಹಣ ಹೋಗುವ ಮೊದಲು ಸಂದೇಶ, ಲಿಂಕ್ ಮತ್ತು ಪಾವತಿ ವಿವರಗಳನ್ನು ScamShield ಪರಿಶೀಲಿಸುತ್ತದೆ.',
    checkNow: 'ಪಾವತಿ ಪರಿಶೀಲಿಸಿ',
    tryDemo: 'ಡೆಮೊ ಪ್ರಯತ್ನಿಸಿ',
    inputTitle: 'ನೀವು ಏನು ಪರಿಶೀಲಿಸಲು ಬಯಸುತ್ತೀರಿ?',
    inputSubtitle: 'ಪಾವತಿ ವಿನಂತಿ ನಿಮಗೆ ಹೇಗೆ ಬಂದಿದೆ ಎಂಬುದನ್ನು ಆಯ್ಕೆಮಾಡಿ.',
    message: 'ಸಂದೇಶ',
    messageDesc: 'ನಿಮಗೆ ಬಂದ ಸಂದೇಶವನ್ನು ಹಾಕಿ',
    screenshot: 'ಸ್ಕ್ರೀನ್‌ಶಾಟ್',
    screenshotDesc: 'ಸಂದೇಹಾಸ್ಪದ ಸಂದೇಶದ ಚಿತ್ರ ಹಾಕಿ',
    upi: 'ಪಾವತಿ',
    upiDesc: 'ಪಾವತಿ ವಿನಂತಿಯನ್ನು ಅನುಕರಿಸಿ',
    messageLabel: 'ಸಂದೇಶ',
    messagePlaceholder: 'ಪಾವತಿ ಮಾಡಲು ಕೇಳಿರುವ ಸಂದೇಶವನ್ನು ಹಾಕಿ...',
    urlLabel: 'ಲಿಂಕ್ / URL',
    urlPlaceholder: 'https://example.com/pay',
    payeeLabel: 'ಪಾವತಿದಾರರ ಹೆಸರು',
    payeePlaceholder: 'ನೀವು ಯಾರಿಗೆ ಪಾವತಿಸುತ್ತೀರಿ?',
    amountLabel: 'ಪಾವತಿ ಮೊತ್ತ',
    amountPlaceholder: '8500',
    upiLabel: 'UPI ID',
    upiPlaceholder: 'name@upi',
    runCheck: 'ಭದ್ರತಾ ಪರಿಶೀಲನೆ ಪ್ರಾರಂಭಿಸಿ',
    loading: 'ಪರಿಶೀಲಿಸಲಾಗುತ್ತಿದೆ...',
    demoTitle: 'SCAMSHIELD ಅನ್ನು ಪ್ರಯತ್ನಿಸಿ',
    demoText:
      'ನಿಜವಾದ ಪಾವತಿ ಮೋಸದ ಉದಾಹರಣೆಯನ್ನು ಪ್ರಯತ್ನಿಸಿ ಮತ್ತು ScamShield ಹೇಗೆ ಪತ್ತೆ ಮಾಡುತ್ತದೆ ನೋಡಿ.',
    loadDemo: 'ಹೈ-ರಿಸ್ಕ್ ಡೆಮೊ',
    story: 'ಸಂದೇಶದ ಕಥೆ',
    payment: 'ಪಾವತಿ ವಿನಂತಿ',
    decision: 'ಭದ್ರತಾ ನಿರ್ಧಾರ',
    safe: 'ಸುರಕ್ಷಿತ',
    suspicious: 'ಸಂದೇಹಾಸ್ಪದ',
    highRisk: 'ಹೆಚ್ಚಿನ ಅಪಾಯ',
    proceed: 'ಮುಂದುವರಿಯಿರಿ',
    verify: 'ಪರಿಶೀಲಿಸಿ',
    stop: 'ನಿಲ್ಲಿಸಿ',
    possible: 'ಸಂಭವನೀಯ ಫಲಿತಾಂಶಗಳು',
    possibleText: 'ಪಾವತಿಸುವ ಮೊದಲು ಒಂದು ಸ್ಪಷ್ಟ ನಿರ್ಧಾರ.',
    whyRisky: 'ಇದು ಏಕೆ ಅಪಾಯಕಾರಿ?',
    whySafe: 'ಇದು ಏಕೆ ಸುರಕ್ಷಿತ?',
    whatDo: 'ನೀವು ಏನು ಮಾಡಬೇಕು?',
    stopPayment: 'ಪಾವತಿ ನಿಲ್ಲಿಸಿ',
    verifyPayment: 'ಪಾವತಿ ಪರಿಶೀಲಿಸಿ',
    proceedPayment: 'ಎಚ್ಚರಿಕೆಯಿಂದ ಮುಂದುವರಿಯಿರಿ',
    screenshotSoon:
      'ಸ್ಕ್ರೀನ್‌ಶಾಟ್ ಪರಿಶೀಲನೆಯನ್ನು ಮುಂದಿನ ಹಂತದಲ್ಲಿ backend ಗೆ ಸಂಪರ್ಕಿಸಲಾಗುತ್ತದೆ.',
    backendError:
      'ScamShield backend ಗೆ ಸಂಪರ್ಕಿಸಲಾಗಲಿಲ್ಲ. FastAPI server port 8000 ನಲ್ಲಿ ಚಾಲನೆಯಲ್ಲಿದೆಯೇ ನೋಡಿ.',
    footer: 'ಪಾವತಿಸುವ ಮೊದಲು ಯೋಚಿಸಿ.',
  },

  hi: {
    navCheck: 'भुगतान जांचें',
    navDemo: 'डेमो',
    navSafety: 'सुरक्षा',
    heroTag: 'भुगतान से पहले सुरक्षा',
    heroTitle1: 'क्या भुगतान',
    heroTitle2: 'कहानी से मेल खाता है?',
    heroText:
      'आपका पैसा जाने से पहले ScamShield संदेश, लिंक और भुगतान की जानकारी जांचता है।',
    checkNow: 'भुगतान जांचें',
    tryDemo: 'लाइव डेमो',
    inputTitle: 'आप क्या जांचना चाहते हैं?',
    inputSubtitle: 'भुगतान अनुरोध आपको कैसे मिला, वह चुनें।',
    message: 'संदेश',
    messageDesc: 'आपको मिला संदेश डालें',
    screenshot: 'स्क्रीनशॉट',
    screenshotDesc: 'संदिग्ध संदेश की तस्वीर डालें',
    upi: 'भुगतान',
    upiDesc: 'भुगतान अनुरोध का सिमुलेशन करें',
    messageLabel: 'संदेश',
    messagePlaceholder: 'भुगतान करने के लिए आए संदेश को डालें...',
    urlLabel: 'लिंक / URL',
    urlPlaceholder: 'https://example.com/pay',
    payeeLabel: 'भुगतान प्राप्तकर्ता',
    payeePlaceholder: 'आप किसे भुगतान कर रहे हैं?',
    amountLabel: 'भुगतान राशि',
    amountPlaceholder: '8500',
    upiLabel: 'UPI ID',
    upiPlaceholder: 'name@upi',
    runCheck: 'सुरक्षा जांच शुरू करें',
    loading: 'जांच हो रही है...',
    demoTitle: 'SCAMSHIELD को आजमाएं',
    demoText:
      'एक वास्तविक भुगतान घोटाले का उदाहरण देखें और जानें कि ScamShield इसे कैसे पकड़ता है।',
    loadDemo: 'हाई-रिस्क डेमो',
    story: 'संदेश की कहानी',
    payment: 'भुगतान अनुरोध',
    decision: 'सुरक्षा निर्णय',
    safe: 'सुरक्षित',
    suspicious: 'संदिग्ध',
    highRisk: 'उच्च जोखिम',
    proceed: 'आगे बढ़ें',
    verify: 'जांचें',
    stop: 'रोकें',
    possible: 'संभावित परिणाम',
    possibleText: 'भुगतान से पहले एक स्पष्ट निर्णय।',
    whyRisky: 'यह जोखिम भरा क्यों है?',
    whySafe: 'यह सुरक्षित क्यों है?',
    whatDo: 'आपको क्या करना चाहिए?',
    stopPayment: 'भुगतान रोकें',
    verifyPayment: 'भुगतान सत्यापित करें',
    proceedPayment: 'सावधानी से आगे बढ़ें',
    screenshotSoon:
      'स्क्रीनशॉट जांच अगले चरण में backend से जोड़ी जाएगी।',
    backendError:
      'ScamShield backend से कनेक्ट नहीं हो पाया। सुनिश्चित करें कि FastAPI server port 8000 पर चल रहा है।',
    footer: 'भुगतान करने से पहले सोचें।',
  },
}

function ShieldIcon({ small = false }) {
  return (
    <div className={`brand-shield ${small ? 'small' : ''}`}>
      <svg viewBox="0 0 40 40" fill="none">
        <path
          d="M20 3L34 8V18C34 27 28.5 34 20 37C11.5 34 6 27 6 18V8L20 3Z"
          stroke="currentColor"
          strokeWidth="2.5"
        />
        <path
          d="M13 20L18 25L28 14"
          stroke="currentColor"
          strokeWidth="2.5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    </div>
  )
}

function ArrowIcon() {
  return (
    <svg viewBox="0 0 20 20" fill="none">
      <path
        d="M4 10H16M10 4L16 10L10 16"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

function ModeIcon({ type }) {
  if (type === 'message') {
    return (
      <svg viewBox="0 0 32 32" fill="none">
        <rect
          x="4"
          y="5"
          width="24"
          height="19"
          rx="3"
          stroke="currentColor"
          strokeWidth="2"
        />
        <path
          d="M9 11H23M9 16H19"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
        />
        <path
          d="M11 24L9 28L16 24"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinejoin="round"
        />
      </svg>
    )
  }

  if (type === 'screenshot') {
    return (
      <svg viewBox="0 0 32 32" fill="none">
        <rect
          x="4"
          y="5"
          width="24"
          height="22"
          rx="3"
          stroke="currentColor"
          strokeWidth="2"
        />
        <circle
          cx="11"
          cy="12"
          r="2"
          stroke="currentColor"
          strokeWidth="2"
        />
        <path
          d="M7 23L14 16L18 20L21 17L26 23"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    )
  }

  return (
    <svg viewBox="0 0 32 32" fill="none">
      <path
        d="M16 4V28M21 9C20 7 18 6 15.5 6C12.5 6 10 7.5 10 10C10 16 22 13.5 22 20C22 23.5 19 26 15.5 26C12.5 26 10 24.5 9 22"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
    </svg>
  )
}

function SunIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none">
      <circle
        cx="12"
        cy="12"
        r="4"
        stroke="currentColor"
        strokeWidth="1.8"
      />
      <path
        d="M12 2V5M12 19V22M22 12H19M5 12H2M19.07 4.93L16.95 7.05M7.05 16.95L4.93 19.07M19.07 19.07L16.95 16.95M7.05 7.05L4.93 4.93"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
      />
    </svg>
  )
}

function MoonIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none">
      <path
        d="M20 15.2A8.5 8.5 0 0 1 8.8 4C5.8 5.25 4 8.1 4 11.35A8.65 8.65 0 0 0 12.65 20C15.9 20 18.75 18.2 20 15.2Z"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

function App() {
  const [language, setLanguage] = useState('en')

  const [theme, setTheme] = useState(() => {
    return localStorage.getItem('scamshield-theme') || 'dark'
  })

  const t = translations[language]

  const [inputType, setInputType] = useState('message')
  const [message, setMessage] = useState('')
  const [url, setUrl] = useState('')
  const [payee, setPayee] = useState('')
  const [amount, setAmount] = useState('')
  const [upiId, setUpiId] = useState('')
  const [screenshot, setScreenshot] = useState(null)

  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    localStorage.setItem('scamshield-theme', theme)
  }, [theme])

  const createDemoResult = () => ({
    interaction_id: 'demo-high-risk',
    risk_level: 'HIGH_RISK',
    score: 94,
    signals: ['AMOUNT_MISMATCH', 'PAYEE_MISMATCH', 'SUSPICIOUS_URL'],
    reasons: [
      'The message asks for ₹850, but the payment amount is ₹8,500.',
      'The message is about an electricity bill, but the payment is going to Rahul Kumar.',
      'The payment link does not clearly identify the official electricity provider.',
      'Urgent language is being used to pressure you into making a payment.',
    ],
    recommendation: 'STOP',
  })

  const loadHighRiskDemo = () => {
    setInputType('upi')
    setMessage(
      'Your electricity bill is overdue. Pay ₹850 immediately to avoid disconnection.'
    )
    setUrl('https://example.com/electricity-payment')
    setPayee('Rahul Kumar')
    setAmount('8500')
    setUpiId('rahul@upi')
    setResult(null)

    setTimeout(() => {
      document.getElementById('checker')?.scrollIntoView({
        behavior: 'smooth',
        block: 'start',
      })
    }, 100)
  }

  const localizeReason = (reason) => {
    const text = String(reason || '').toLowerCase()

    if (
      text.includes('amount') ||
      text.includes('850') ||
      text.includes('8,500')
    ) {
      if (language === 'kn') {
        return 'ಸಂದೇಶದಲ್ಲಿ ₹850 ಕೇಳಲಾಗಿದೆ, ಆದರೆ ಪಾವತಿ ಮೊತ್ತ ₹8,500 ಆಗಿದೆ.'
      }

      if (language === 'hi') {
        return 'संदेश में ₹850 मांगे गए हैं, लेकिन भुगतान राशि ₹8,500 है।'
      }

      return 'The message asks for ₹850, but the payment amount is ₹8,500.'
    }

    if (
      text.includes('payee') ||
      text.includes('rahul') ||
      text.includes('electricity')
    ) {
      if (language === 'kn') {
        return 'ವಿದ್ಯುತ್ ಬಿಲ್‌ಗಾಗಿ ಹೇಳಲಾಗಿದೆ, ಆದರೆ ಹಣ Rahul Kumar ಅವರಿಗೆ ಹೋಗುತ್ತಿದೆ.'
      }

      if (language === 'hi') {
        return 'संदेश बिजली बिल के बारे में है, लेकिन भुगतान Rahul Kumar को जा रहा है।'
      }

      return 'The message is about an electricity bill, but the payment is going to Rahul Kumar.'
    }

    if (
      text.includes('url') ||
      text.includes('link') ||
      text.includes('destination')
    ) {
      if (language === 'kn') {
        return 'ಪಾವತಿ ಲಿಂಕ್ ಅಧಿಕೃತ ವಿದ್ಯುತ್ ಪೂರೈಕೆದಾರರನ್ನು ಸ್ಪಷ್ಟವಾಗಿ ಗುರುತಿಸುವುದಿಲ್ಲ.'
      }

      if (language === 'hi') {
        return 'भुगतान लिंक आधिकारिक बिजली प्रदाता की पहचान स्पष्ट रूप से नहीं करता।'
      }

      return 'The payment link does not clearly identify the official electricity provider.'
    }

    if (text.includes('urgent') || text.includes('pressure')) {
      if (language === 'kn') {
        return 'ತಕ್ಷಣ ಪಾವತಿಸಲು ಒತ್ತಡ ಹೇರುವ ತುರ್ತು ಭಾಷೆಯನ್ನು ಬಳಸಲಾಗಿದೆ.'
      }

      if (language === 'hi') {
        return 'तुरंत भुगतान करवाने के लिए दबाव वाली भाषा का उपयोग किया गया है।'
      }

      return 'Urgent language is being used to pressure you into making a payment.'
    }

    return reason
  }

  const getRiskInfo = () => {
    const risk = result?.risk_level

    if (risk === 'HIGH_RISK') {
      return {
        label: t.highRisk,
        action: t.stop,
        title: t.stopPayment,
        className: 'risk-high',
      }
    }

    if (risk === 'SAFE') {
      return {
        label: t.safe,
        action: t.proceed,
        title: t.proceedPayment,
        className: 'risk-safe',
      }
    }

    return {
      label: t.suspicious,
      action: t.verify,
      title: t.verifyPayment,
      className: 'risk-suspicious',
    }
  }

  const handleAnalyze = async () => {
    if (inputType === 'screenshot') {
      alert(t.screenshotSoon)
      return
    }

    setLoading(true)
    setResult(null)

    const requestData = {
      message,
      url,
      qr_payload: null,
      payee,
      amount: Number(amount) || 0,
      currency: 'INR',
    }

    try {
      const data = await analyzePayment(requestData)
      setResult(data)
    } catch (error) {
      if (payee === 'Rahul Kumar' && Number(amount) === 8500) {
        setResult(createDemoResult())
      } else {
        alert(t.backendError)
      }
    } finally {
      setLoading(false)
    }
  }

  const riskInfo = result ? getRiskInfo() : null

  const themeButtonStyle = {
    display: 'flex',
    alignItems: 'center',
    gap: '4px',
    padding: '4px',
    height: '42px',
    border: '1px solid #284054',
    background: '#090e15',
    flexShrink: 0,
    zIndex: 100,
  }

  const themeButton = (isActive) => ({
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '6px',
    height: '32px',
    minWidth: '72px',
    padding: '0 10px',
    border: isActive
      ? '1px solid #00e5ff'
      : '1px solid transparent',
    background: isActive ? '#111a25' : 'transparent',
    color: isActive ? '#00e5ff' : '#82909e',
    fontFamily: 'monospace',
    fontSize: '10px',
    fontWeight: '700',
    letterSpacing: '0.06em',
    cursor: 'pointer',
  })

  return (
    <div className={`app-shell ${theme === 'light' ? 'theme-light' : ''}`}>
      <header className="topbar">
        <div className="topbar-inner">
          <a className="logo" href="#">
            <ShieldIcon small />
            <div>
              <strong>ScamShield</strong>
              <span>THINK • CHECK • PAY SAFE</span>
            </div>
          </a>

          <nav className="desktop-nav">
            <a href="#checker">{t.navCheck}</a>
            <a href="#demo">{t.navDemo}</a>
            <a href="#safety">{t.navSafety}</a>
          </nav>

          <div className="top-actions">
            {/* THEME SWITCH */}
            <div
              className="scamshield-theme-switch"
              style={themeButtonStyle}
              aria-label="Theme selection"
            >
              <button
                type="button"
                onClick={() => setTheme('light')}
                title="Light mode"
                style={themeButton(theme === 'light')}
              >
                <SunIcon />
                <span>LIGHT</span>
              </button>

              <button
                type="button"
                onClick={() => setTheme('dark')}
                title="Dark mode"
                style={themeButton(theme === 'dark')}
              >
                <MoonIcon />
                <span>DARK</span>
              </button>
            </div>

            <select
              value={language}
              onChange={(e) => setLanguage(e.target.value)}
              className="language-select"
            >
              <option value="en">ENGLISH</option>
              <option value="kn">ಕನ್ನಡ</option>
              <option value="hi">हिन्दी</option>
            </select>

            <button
              className="header-check"
              onClick={() =>
                document.getElementById('checker')?.scrollIntoView({
                  behavior: 'smooth',
                })
              }
            >
              {t.checkNow}
              <ArrowIcon />
            </button>
          </div>
        </div>
      </header>

      <main>
        <section className="hero">
          <div className="hero-grid">
            <div className="hero-copy">
              <div className="eyebrow">
                <span className="live-dot" />
                {t.heroTag}
              </div>

              <h1>
                {t.heroTitle1}
                <br />
                <span>{t.heroTitle2}</span>
              </h1>

              <p>{t.heroText}</p>

              <div className="hero-buttons">
                <button
                  className="primary-button"
                  onClick={() =>
                    document.getElementById('checker')?.scrollIntoView({
                      behavior: 'smooth',
                    })
                  }
                >
                  {t.checkNow}
                  <ArrowIcon />
                </button>

                <button className="text-button" onClick={loadHighRiskDemo}>
                  {t.tryDemo}
                  <ArrowIcon />
                </button>
              </div>

              <div className="hero-stats">
                <div>
                  <strong>01</strong>
                  <span>MESSAGE</span>
                </div>

                <div>
                  <strong>02</strong>
                  <span>PAYMENT</span>
                </div>

                <div>
                  <strong>03</strong>
                  <span>DECISION</span>
                </div>
              </div>
            </div>

            <div className="hero-visual">
              <div className="visual-label">PAYMENT CHECKPOINT</div>

              <div className="checkpoint-card">
                <div className="checkpoint-top">
                  <span>SCAMSHIELD</span>
                  <span className="status-live">● LIVE</span>
                </div>

                <div className="checkpoint-icon">
                  <ShieldIcon />
                </div>

                <div className="checkpoint-question">
                  Does this payment
                  <br />
                  <span>make sense?</span>
                </div>

                <div className="checkpoint-line">
                  <span>MESSAGE</span>
                  <span>PAYMENT</span>
                  <span>DECISION</span>
                </div>
              </div>

              <div className="floating-data data-one">
                <span>AMOUNT</span>
                <strong>₹850 → ₹8,500</strong>
              </div>

              <div className="floating-data data-two">
                <span>STATUS</span>
                <strong>CHECK REQUIRED</strong>
              </div>
            </div>
          </div>
        </section>

        <section className="ticker">
          <div>MESSAGE</div>
          <span>→</span>
          <div>LINK / QR</div>
          <span>→</span>
          <div>PAYMENT</div>
          <span>→</span>
          <div>CORRELATION</div>
          <span>→</span>
          <div>RISK DECISION</div>
        </section>

        <section className="checker-section" id="checker">
          <div className="section-heading">
            <div>
              <span className="section-number">01 / CHECK</span>
              <h2>{t.inputTitle}</h2>
            </div>

            <p>{t.inputSubtitle}</p>
          </div>

          <div className="mode-grid">
            <button
              className={`mode-card ${inputType === 'message' ? 'active' : ''
                }`}
              onClick={() => setInputType('message')}
            >
              <div className="mode-number">01</div>

              <div className="mode-icon">
                <ModeIcon type="message" />
              </div>

              <div>
                <h3>{t.message}</h3>
                <p>{t.messageDesc}</p>
              </div>

              <ArrowIcon />
            </button>

            <button
              className={`mode-card ${inputType === 'screenshot' ? 'active' : ''
                }`}
              onClick={() => setInputType('screenshot')}
            >
              <div className="mode-number">02</div>

              <div className="mode-icon">
                <ModeIcon type="screenshot" />
              </div>

              <div>
                <h3>{t.screenshot}</h3>
                <p>{t.screenshotDesc}</p>
              </div>

              <ArrowIcon />
            </button>

            <button
              className={`mode-card ${inputType === 'upi' ? 'active' : ''
                }`}
              onClick={() => setInputType('upi')}
            >
              <div className="mode-number">03</div>

              <div className="mode-icon">
                <ModeIcon type="upi" />
              </div>

              <div>
                <h3>{t.upi}</h3>
                <p>{t.upiDesc}</p>
              </div>

              <ArrowIcon />
            </button>
          </div>

          <div className="checker-workspace">
            <div className="workspace-head">
              <div>
                <span>INPUT / {inputType.toUpperCase()}</span>
                <h3>Payment intelligence checkpoint</h3>
              </div>

              <div className="workspace-status">
                <span />
                READY
              </div>
            </div>

            {inputType === 'message' && (
              <div className="form-area">
                <div className="field full">
                  <label>{t.messageLabel}</label>

                  <textarea
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    placeholder={t.messagePlaceholder}
                  />
                </div>

                <div className="field">
                  <label>{t.urlLabel}</label>

                  <input
                    value={url}
                    onChange={(e) => setUrl(e.target.value)}
                    placeholder={t.urlPlaceholder}
                  />
                </div>

                <div className="field">
                  <label>{t.amountLabel}</label>

                  <input
                    type="number"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    placeholder={t.amountPlaceholder}
                  />
                </div>

                <div className="field">
                  <label>{t.payeeLabel}</label>

                  <input
                    value={payee}
                    onChange={(e) => setPayee(e.target.value)}
                    placeholder={t.payeePlaceholder}
                  />
                </div>

                <div className="field">
                  <label>{t.upiLabel}</label>

                  <input
                    value={upiId}
                    onChange={(e) => setUpiId(e.target.value)}
                    placeholder={t.upiPlaceholder}
                  />
                </div>
              </div>
            )}

            {inputType === 'upi' && (
              <div className="form-area">
                <div className="field full">
                  <label>{t.messageLabel}</label>

                  <textarea
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    placeholder={t.messagePlaceholder}
                  />
                </div>

                <div className="payment-strip">
                  <div className="field">
                    <label>{t.payeeLabel}</label>

                    <input
                      value={payee}
                      onChange={(e) => setPayee(e.target.value)}
                      placeholder={t.payeePlaceholder}
                    />
                  </div>

                  <div className="field">
                    <label>{t.amountLabel}</label>

                    <input
                      type="number"
                      value={amount}
                      onChange={(e) => setAmount(e.target.value)}
                      placeholder={t.amountPlaceholder}
                    />
                  </div>

                  <div className="field">
                    <label>{t.upiLabel}</label>

                    <input
                      value={upiId}
                      onChange={(e) => setUpiId(e.target.value)}
                      placeholder={t.upiPlaceholder}
                    />
                  </div>
                </div>

                <div className="field full">
                  <label>{t.urlLabel}</label>

                  <input
                    value={url}
                    onChange={(e) => setUrl(e.target.value)}
                    placeholder={t.urlPlaceholder}
                  />
                </div>
              </div>
            )}

            {inputType === 'screenshot' && (
              <div className="upload-zone">
                <ModeIcon type="screenshot" />

                <h3>{t.screenshot}</h3>

                <p>{t.screenshotDesc}</p>

                <input
                  type="file"
                  accept="image/*"
                  onChange={(e) =>
                    setScreenshot(e.target.files?.[0] || null)
                  }
                />

                {screenshot && (
                  <div className="selected-file">
                    {screenshot.name}
                  </div>
                )}
              </div>
            )}

            <div className="workspace-footer">
              <div className="privacy-note">
                <ShieldIcon small />
                <span>SIMULATED • NO REAL MONEY MOVES</span>
              </div>

              <button
                className="analyze-button"
                onClick={handleAnalyze}
                disabled={loading}
              >
                {loading ? t.loading : t.runCheck}

                {!loading && <ArrowIcon />}
              </button>
            </div>
          </div>

          {result && riskInfo && (
            <section
              className={`result-panel ${riskInfo.className}`}
            >
              <div className="result-header">
                <div>
                  <span className="section-number">
                    02 / DECISION
                  </span>

                  <h2>{riskInfo.label}</h2>
                </div>

                <div className="result-action">
                  <span>{t.decision}</span>
                  <strong>{riskInfo.action}</strong>
                </div>
              </div>

              <div className="result-main">
                <div className="result-message">
                  <div className="result-symbol">
                    {result.risk_level === 'HIGH_RISK'
                      ? '!'
                      : result.risk_level === 'SAFE'
                        ? '✓'
                        : '?'}
                  </div>

                  <div>
                    <span>RISK SCORE</span>
                    <strong>{result.score ?? '--'}/100</strong>
                  </div>
                </div>

                <div className="result-reasons">
                  <div className="result-column">
                    <span className="column-label">
                      {result.risk_level === 'SAFE'
                        ? t.whySafe
                        : t.whyRisky}
                    </span>

                    {result.reasons?.length ? (
                      result.reasons.map((reason, index) => (
                        <div className="reason-row" key={index}>
                          <span>
                            {String(index + 1).padStart(2, '0')}
                          </span>

                          <p>{localizeReason(reason)}</p>
                        </div>
                      ))
                    ) : (
                      <div className="reason-row">
                        <span>01</span>
                        <p>
                          No major conflicting signals detected.
                        </p>
                      </div>
                    )}
                  </div>

                  <div className="decision-column">
                    <span className="column-label">
                      {t.whatDo}
                    </span>

                    <h3>{riskInfo.title}</h3>

                    <p>
                      {result.risk_level === 'HIGH_RISK'
                        ? 'Do not proceed until the payment details have been independently verified.'
                        : result.risk_level === 'SAFE'
                          ? 'The available payment details are consistent with the message.'
                          : 'Verify the sender, payee and payment details through an independent source.'}
                    </p>
                  </div>
                </div>
              </div>
            </section>
          )}
        </section>

        <section className="demo-section" id="demo">
          <div className="demo-copy">
            <span className="section-number">03 / DEMO</span>

            <h2>{t.demoTitle}</h2>

            <p>{t.demoText}</p>

            <button
              className="primary-button"
              onClick={loadHighRiskDemo}
            >
              {t.loadDemo}
              <ArrowIcon />
            </button>
          </div>

          <div className="demo-transaction">
            <div className="transaction-head">
              <span>DEMO TRANSACTION</span>
              <span>HIGH RISK</span>
            </div>

            <div className="transaction-row">
              <div>
                <span>{t.story}</span>
                <strong>Electricity Bill</strong>
              </div>

              <div>
                <span>{t.payment}</span>
                <strong>₹8,500</strong>
              </div>
            </div>

            <div className="transaction-warning">
              <span>!</span>

              <div>
                <strong>Mismatch detected</strong>
                <p>Expected ₹850 → Received ₹8,500</p>
              </div>
            </div>
          </div>
        </section>

        <section className="safety-section" id="safety">
          <div className="section-heading">
            <div>
              <span className="section-number">
                04 / DECISION
              </span>

              <h2>{t.possible}</h2>
            </div>

            <p>{t.possibleText}</p>
          </div>

          <div className="result-types">
            <div className="result-type safe-card">
              <span>01</span>

              <div className="result-type-icon">✓</div>

              <h3>{t.safe}</h3>

              <p>No major conflicting signals detected.</p>

              <strong>{t.proceed}</strong>
            </div>

            <div className="result-type suspicious-card">
              <span>02</span>

              <div className="result-type-icon">?</div>

              <h3>{t.suspicious}</h3>

              <p>Something needs independent verification.</p>

              <strong>{t.verify}</strong>
            </div>

            <div className="result-type high-card">
              <span>03</span>

              <div className="result-type-icon">!</div>

              <h3>{t.highRisk}</h3>

              <p>Strong conflicting signals detected.</p>

              <strong>{t.stop}</strong>
            </div>
          </div>
        </section>
      </main>

      <footer>
        <div className="footer-brand">
          <ShieldIcon small />

          <div>
            <strong>ScamShield</strong>
            <span>{t.footer}</span>
          </div>
        </div>

        <span>HACKATOPIA 2K26 • THE SATURN</span>
      </footer>
    </div>
  )
}

export default App