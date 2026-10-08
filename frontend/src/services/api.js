const API_BASE_URL = 'http://127.0.0.1:8000'

export async function analyzePayment(data) {
    const response = await fetch(`${API_BASE_URL}/analyze`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(data),
    })

    if (!response.ok) {
        throw new Error(`Backend error: ${response.status}`)
    }

    return await response.json()
}