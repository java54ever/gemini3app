import streamlit as st
import requests
import pandas as pd
from datetime import datetime

# --- Configuration ---
st.set_page_config(page_title="Météo France Preview", page_icon="🇫🇷")

# --- Helper Functions ---
def get_coordinates(city_name):
    """Finds lat/lon for a city name using Open-Meteo Geocoding."""
    url = "https://geocoding-api.open-meteo.com/v1/search"
    params = {"name": city_name, "count": 1, "language": "fr", "format": "json"}
    try:
        response = requests.get(url, params=params)
        data = response.json()
        if "results" in data:
            return data["results"][0]
        else:
            return None
    except Exception:
        return None

def get_weather_icon(code):
    """Maps WMO weather codes to emojis."""
    if code == 0: return "☀️ Clear"
    if code in [1, 2, 3]: return "⛅ Cloudy"
    if code in [45, 48]: return "🌫️ Fog"
    if code in [51, 53, 55]: return "🌧️ Drizzle"
    if code in [61, 63, 65]: return "☔ Rain"
    if code in [71, 73, 75]: return "❄️ Snow"
    if code in [95, 96, 99]: return "⚡ Thunderstorm"
    return "🌡️"

def get_meteo_france_data(lat, lon):
    """Fetches hourly data specifically from the Meteo-France AROME model."""
    url = "https://api.open-meteo.com/v1/forecast"
    params = {
        "latitude": lat,
        "longitude": lon,
        "hourly": "temperature_2m,relative_humidity_2m,weather_code,precipitation",
        "timezone": "auto",
        "forecast_days": 1,
        # UPDATED: The correct internal ID for the 1.3km model
        "models": "meteofrance_arome_france"
    }
    response = requests.get(url, params=params)
    return response.json()

# --- App Interface ---
st.title("🇫🇷 Météo-France Hourly Preview")
st.markdown("Data Source: **Météo-France AROME** (High Resolution 1.3km)")

# 1. User Input
city = st.text_input("Enter a city in France:", "Paris")

if city:
    # 2. Get Coordinates
    location = get_coordinates(city)
    
    if location:
        lat = location['latitude']
        lon = location['longitude']
        region = location.get('admin1', '')
        country = location.get('country', '')
        
        st.success(f"📍 **{location['name']}** ({region}, {country})")
        
        # 3. Fetch Data
        with st.spinner('Loading AROME data...'):
            data = get_meteo_france_data(lat, lon)
        
        if "hourly" in data:
            hourly = data["hourly"]
            
            # Create DataFrame
            df = pd.DataFrame({
                "Time": pd.to_datetime(hourly["time"]),
                "Temp (°C)": hourly["temperature_2m"],
                "Rain (mm)": hourly["precipitation"],
                "Humidity (%)": hourly["relative_humidity_2m"],
                "Code": hourly["weather_code"]
            })
            
            # Add Condition Icons
            df["Condition"] = df["Code"].apply(get_weather_icon)
            
            # 4. Display Current Weather (Approximate)
            current_hour_index = datetime.now().hour
            # Bounds check to prevent crash at midnight or if data is short
            if 0 <= current_hour_index < len(df):
                current = df.iloc[current_hour_index]
                
                # Metrics Row
                col1, col2, col3 = st.columns(3)
                col1.metric("Temperature", f"{current['Temp (°C)']}°C")
                col2.metric("Sky", current['Condition'])
                col3.metric("Rain", f"{current['Rain (mm)']} mm")
            
            # 5. Charts
            st.subheader("Temperature Trend (24h)")
            # Create a copy for the chart with datetime index
            chart_data = df.set_index("Time")[["Temp (°C)"]]
            st.line_chart(chart_data)

            # 6. Detailed Table
            st.subheader("Hour-by-Hour Detail")
            
            # Format time for display (HH:MM)
            display_df = df.copy()
            display_df["Time"] = display_df["Time"].dt.strftime('%H:%M')
            
            # Select and reorder columns
            final_cols = ["Time", "Condition", "Temp (°C)", "Rain (mm)", "Humidity (%)"]
            st.dataframe(
                display_df[final_cols], 
                use_container_width=True, 
                hide_index=True
            )
            
        else:
            st.error("Error: Could not retrieve data. The API might be busy or the model name is incorrect.")
            st.write(data) # Print error detail if any
    else:
        st.warning("City not found. Please check the spelling.")