# 🇫🇷 Météo-France AROME Weather Preview

> **A high-precision weather forecasting tool utilizing the Météo-France AROME model (1.3km resolution).**

Most standard weather apps rely on global models (GFS/ECMWF) with ~22km resolution. This project retrieves data specifically from the **French government's high-resolution model**. This ensures superior accuracy for local topography, valleys, and micro-climates in France.

This repository contains **two implementations** of the same tool:
1.  🐍 **Python:** A modern, interactive web dashboard using Streamlit.
2.  ☕ **Java:** A lightweight, zero-dependency desktop application.

---

## 📂 Project Structure

```text
/weather_app
│
├── app.py                # The Python Web Application (Streamlit)
├── MeteoFranceApp.java   # The Java Desktop Application (Swing)
└── README.md             # This documentation
```

---

## 🐍 Option 1: The Python App (Web UI)

The Python version uses **Streamlit** to create a reactive web dashboard with interactive charts and data tables.

### 1. Prerequisites
*   Python 3.8 or newer.

### 2. Installation
Open your terminal in the project folder and install the required libraries:

**For Mac / Linux:**
```bash
pip3 install streamlit requests pandas
```

**For Windows:**
```powershell
python -m pip install streamlit requests pandas
```

### 3. How to Run
Execute the following command:
```bash
streamlit run app.py
```
*Your default web browser will open automatically displaying the app.*

---

## ☕ Option 2: The Java App (Desktop UI)

The Java version is a standalone application. It requires **no external libraries** (no Maven, no Gradle, no JARs). It uses the standard `java.net.http` client and Swing for the GUI.

### 1. Prerequisites
*   JDK 11 or newer.

### 2. Compilation
Open your terminal in the project folder and compile the source code:
```bash
javac MeteoFranceApp.java
```

### 3. How to Run
Start the application:
```bash
java MeteoFranceApp
```
*A window will appear. Enter a city name and click "Get Forecast". The app includes a debug console at the bottom to view raw API logs.*

---

## ⚙️ Technical Documentation

### Data Source
This project uses the **Open-Meteo API** as a gateway to access public meteorological datasets without requiring API keys or complex authentication.

*   **Geocoding Endpoint:** `https://geocoding-api.open-meteo.com/v1/search`
*   **Weather Endpoint:** `https://api.open-meteo.com/v1/forecast`

### The "Magic" Parameter
To ensure we are using the French high-precision model, the API request includes the specific `models` parameter:

```json
"models": "meteofrance_arome_france"
```

*   **Resolution:** 1.3 km
*   **Update Interval:** Hourly
*   **Coverage:** France and bordering regions

### Java Implementation Details
*   **JSON Parsing:** Uses a custom Regex-based parser to avoid adding heavy dependencies like Jackson or Gson.
*   **Concurrency:** Network requests are handled on a background thread to prevent the UI from freezing (Swing Event Dispatch Thread safety).

---

## ⚠️ Troubleshooting

### Python
*   **Error: `pip is not recognized`**: You likely didn't add Python to your PATH during installation. Use `python -m pip ...` instead.
*   **Error: `No module named 'streamlit'`**: Run the install command again and ensure no errors occurred.

### Java
*   **Error: `HTTP 400 Bad Request`**: This usually happens if the URL is malformed. Check the debug logs in the app window. Ensure the model name is exactly `meteofrance_arome_france`.
*   **Error: `class file has wrong version`**: Your `javac` (compiler) version might be newer than your `java` (runtime) version. Update your JDK.

---

## 📜 License & Attribution

*   **Weather Data:** [Météo-France](https://donneespubliques.meteofrance.fr/) via [Open-Meteo](https://open-meteo.com/) (CC BY 4.0).
*   **License:** MIT License. Feel free to use, modify, and distribute this code.
```
