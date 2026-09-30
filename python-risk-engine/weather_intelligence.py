"""
Python Weather Intelligence Module.
Fetches real-time meteorological observations and multi-day forecasts via Open-Meteo API
with zero API key required, supported universally for any coordinates globally.
Includes deterministic fallback simulation for offline resiliency.
"""

import requests
import math
from datetime import datetime
from typing import Dict, Any, Optional

OPEN_METEO_BASE_URL = "https://api.open-meteo.com/v1/forecast"


def get_weather_for_coordinates(lat: float, lon: float, location_id: str = "LOC-DYNAMIC") -> Dict[str, Any]:
    """Fetches real-time weather and forecast data from Open-Meteo API with offline fallback."""
    try:
        url = (
            f"{OPEN_METEO_BASE_URL}?latitude={lat:.4f}&longitude={lon:.4f}"
            "&current=temperature_2m,relative_humidity_2m,surface_pressure,wind_speed_10m,wind_direction_10m,precipitation"
            "&hourly=temperature_2m,precipitation&forecast_days=2"
        )
        resp = requests.get(url, timeout=3.5, headers={"User-Agent": "DisasterEarlyWarningAgent/1.0"})
        if resp.status_code == 200:
            data = resp.json()
            curr = data.get("current", {})
            rain = float(curr.get("precipitation", 0.0))
            wind = float(curr.get("wind_speed_10m", 15.0))
            temp = float(curr.get("temperature_2m", 28.0))
            humid = float(curr.get("relative_humidity_2m", 80.0))
            pressure = float(curr.get("surface_pressure", 1010.0))

            forecast_str = "Precipitation Warning: Heavy Influx Expected" if rain > 50 else (
                "Moderate rain showers forecast" if rain > 15 else "Partly Cloudy"
            )

            return {
                "location_id": location_id,
                "provider": "Open-Meteo",
                "temperature_c": temp,
                "humidity_pct": humid,
                "pressure_hpa": pressure,
                "wind_speed_kmh": wind,
                "rainfall_mm": rain,
                "forecast_summary": forecast_str,
                "severe_warning": "High Gale & Rain Warning" if (wind > 55 or rain > 70) else None,
                "recorded_at": datetime.utcnow().isoformat() + "Z",
                "is_fallback": False
            }
    except Exception as e:
        pass

    # Resilient simulated fallback
    return get_simulated_fallback(lat, lon, location_id)


def get_simulated_fallback(lat: float, lon: float, location_id: str) -> Dict[str, Any]:
    """Generates realistic deterministic meteorological observations with current timestamps."""
    now = datetime.utcnow()
    hour = now.hour
    temp = 27.0 + 3.5 * math.sin(math.radians(hour * 15.0))
    humidity = 82.0 + 8.0 * math.cos(math.radians(hour * 15.0))
    wind = 22.0 + 8.0 * abs(math.sin(lat + lon))
    pressure = 1009.0 - 4.0 * abs(math.cos(lat))
    rain = 18.0

    return {
        "location_id": location_id,
        "provider": "SimulatedMeteorologicalFallback",
        "temperature_c": round(temp, 1),
        "humidity_pct": round(humidity, 1),
        "pressure_hpa": round(pressure, 1),
        "wind_speed_kmh": round(wind, 1),
        "rainfall_mm": rain,
        "forecast_summary": "Active Monsoon Circulation - Rain Anticipated",
        "severe_warning": None,
        "recorded_at": now.isoformat() + "Z",
        "is_fallback": True
    }
