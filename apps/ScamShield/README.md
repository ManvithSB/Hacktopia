# ScamShield 🛡️

ScamShield is an Android application designed to help users identify potentially fraudulent SMS messages. It analyzes message content and provides a risk assessment to help users recognize suspicious messages.

## Features

* Analyze SMS messages for potential scam indicators.
* Display a risk assessment and relevant warnings.
* Use rule-based detection and, when configured and available, AI-assisted analysis.
* Provide a user interface with English, Hindi, and Kannada language options.
* Connect an Android frontend to a Python backend for message analysis.

*Note: The features and language support described here should be verified against the current implementation.*

## How It Works

The project uses a frontend and backend that run separately during development.

1. **Android frontend:** The user interacts with the ScamShield application.
2. **Backend request:** The app sends the message for analysis to the Python backend.
3. **Rule-based detection:** The backend checks for suspicious patterns and indicators.
4. **AI-assisted analysis:** The backend can use the Groq API when configured and available.
5. **Result:** The app displays the analysis returned by the backend.

If AI analysis fails, the backend may use its rule-based fallback. The result is an assessment, not a guarantee that a message is safe or fraudulent.

## Technology Stack

* **Frontend:** Android, Kotlin, and Jetpack Compose (verify against the project files).
* **Backend:** Python and FastAPI.
* **AI service:** Groq API, when configured.
* **Development tools:** Android Studio and Python.

## Project Setup

### Prerequisites

* Android Studio and a compatible Android SDK.
* Python installed on your computer.
* The ScamShield Android project.
* The backend project and its required Python dependencies.
* A Groq API key if AI-assisted analysis requires one.

### 1. Start the Backend

Open a terminal and navigate to the backend project directory.

For the current local development setup:

```powershell
cd D:\Hacktopia
python -m uvicorn backend.main:app --reload --host 0.0.0.0 --port 8000
```

Keep this terminal running while testing the Android application.

To check whether the API documentation is available, open:

`http://127.0.0.1:8000/docs`

### 2. Run the Android Frontend

1. Open the `ScamSheildapp` project in Android Studio.
2. Wait for Gradle sync to finish.
3. Select an Android emulator or connect a physical Android phone.
4. Click **Run ▶** to build and launch the app.
5. Keep the backend terminal running while testing features that require server analysis.

### 3. Connect the Frontend to the Backend

The Android app must use a URL that can reach the backend.

* **Android emulator:** `10.0.2.2` commonly maps to the development computer's localhost.
* **Physical Android phone:** Use the computer's local network IP address, for example `http://192.168.1.10:8000`, replacing the example IP with the actual address.
* For a physical phone, the computer and phone generally need to be on the same Wi-Fi network.
* Ensure the computer's firewall permits the required connection.

The URL shown above is an example. Configure the actual backend URL in the app's existing network configuration.

### 4. Configure AI Access

If the backend uses Groq, configure the API key using the method supported by the backend configuration. Keep secrets in environment variables or another appropriate secret-management mechanism.

**Never commit API keys, passwords, or private credentials to GitHub.**

## Testing

Test the application with harmless example messages, such as:

* "Congratulations! You have won a prize. Click this example link to claim it."
* "Your account needs verification. Please check through the official application."
* "Your appointment is confirmed for tomorrow."

These are illustrative examples only. Actual classifications depend on the implemented detection rules, AI availability, and message content.

## Troubleshooting

### Backend unavailable

* Confirm the backend terminal is still running.
* Check that the backend URL configured in the Android app is correct.
* For a physical phone, use the computer's LAN IP instead of `localhost` or `10.0.2.2`.
* Check the Wi-Fi connection and firewall settings.

### AI analysis fails

* Verify that the required API key is configured.
* Check the backend terminal for errors.
* Confirm that the backend's rule-based fallback is working as expected.

### Android build fails

* Wait for Gradle sync to complete.
* Check the error details in Android Studio.
* Verify that the installed Android SDK and required dependencies are available.

## Limitations and Privacy

ScamShield cannot guarantee that every scam will be detected or that every message classified as safe is trustworthy. Verify suspicious requests through official channels, and never share OTPs, passwords, or banking credentials in response to unexpected messages.

Review the actual backend implementation to determine what message data is transmitted, processed, or logged. Do not assume messages remain on the device if they are sent to a backend service.

## Development Workflow

Check the current changes:

```bash
git status
```

Stage the intended changes:

```bash
git add README.md
```

Review staged changes:

```bash
git diff --cached
```

Commit the README:

```bash
git commit -m "Add ScamShield project README"
```

Push the commit to the configured remote when ready:

```bash
git push
```

Check the configured remote and branch before pushing if the repository setup is uncertain.

## Project Status

ScamShield is an ongoing student project. Setup instructions and feature descriptions should be updated as the implementation evolves.
