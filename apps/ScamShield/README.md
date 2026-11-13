# ScamShield

ScamShield is an Android application designed to help users identify potentially fraudulent messages. It analyzes message content and provides risk assessments to help users recognize suspicious activity.

## Features

- Analyze messages for potential scam indicators.
- Display risk levels, scores, recommendations, and explanations.
- Rule-based scam detection.
- English, Hindi, and Kannada language support.
- Android frontend connected to a Python FastAPI backend.
- Backend OCR endpoint for extracting text from images.

## How It Works

1. **Message Input:** The user provides a message for analysis.
2. **Backend Request:** The Android application sends the message to the Python backend.
3. **Risk Analysis:** The backend evaluates suspicious patterns and indicators.
4. **Risk Assessment:** The application displays the risk level, score, recommendation, and explanation.

## Technology Stack

- **Frontend:** Android, Kotlin, Jetpack Compose.
- **Backend:** Python, FastAPI.
- **OCR:** Tesseract OCR, pytesseract, Pillow.
- **Development Tools:** Android Studio, Python.

## Project Setup

### Prerequisites

- Android Studio and a compatible Android SDK.
- Python installed on your computer.
- Git and this repository.

### 1. Start the Backend

Open a terminal at the repository root and install dependencies:

```powershell
python -m pip install -r backend\requirements.txt
```

Start the backend:

```powershell
python -m uvicorn backend.main:app --host 0.0.0.0 --port 8000
```

Keep this terminal running.

To verify that the backend is working, open:

`http://127.0.0.1:8000/docs`

### 2. Run the Android Application

1. Open `apps\ScamShield` in Android Studio.
2. Wait for Gradle sync to complete.
3. Start an Android emulator or connect a physical device.
4. Click **Run** to build and launch the application.

### 3. Connect Android to the Backend

For an Android emulator, open another terminal and run:

```powershell
adb reverse tcp:8000 tcp:8000
```

The application uses:

`http://localhost:8000`

ADB reverse forwards this connection to the backend running on your computer.

Repeat the ADB reverse command if the emulator or ADB connection restarts.

For physical Android devices, configure the backend URL to use your computer's LAN IP address and ensure both devices are on the same network.

**Note:** The current HTTP configuration is intended for local development. A deployed application should use HTTPS.

### 4. OCR Setup (Optional)

The backend provides an image-analysis endpoint at:

`POST /analyze/image`

To use OCR, install Tesseract OCR on your computer in addition to the Python dependencies.

The Android screen currently supports text-based analysis; image upload is available through the backend endpoint.

## Testing

You can test the application using harmless example messages:

- "Congratulations! You have won a prize. Click this example link to claim it."
- "Your account needs verification. Please check through the official application."
- "Your appointment is confirmed for tomorrow."

These are illustrative examples. Actual classifications depend on the implemented detection rules and message content.

## Troubleshooting

### Backend Unavailable

- Ensure the FastAPI backend is running.
- Check that port 8000 is available.
- Verify the backend URL configured in the Android application.
- Run `adb reverse tcp:8000 tcp:8000` for emulator connectivity.

### Android Build Failure

- Wait for Gradle sync to complete.
- Check the build output in Android Studio.
- Verify that the required Android SDK components are installed.

## Limitations and Privacy

ScamShield cannot guarantee that every scam will be detected or that every message classified as safe is trustworthy.

Always verify suspicious requests through official channels. Never share OTPs, passwords, or banking credentials in response to unexpected messages.

Message analysis involves sending message content to the configured backend. Review the backend implementation and deployment configuration to understand how data is processed and logged.

## Project Status

ScamShield is an ongoing student project. Features and setup instructions may evolve as development continues.
