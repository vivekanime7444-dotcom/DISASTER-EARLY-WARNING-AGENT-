"""
Multi-criteria Disaster Risk Classification & Prediction Engine.
Fuses physical sensor telemetry, ADSA spatial neighbor correlation, and weather intelligence.

PROTOTYPE THRESHOLDS (Academic Research):
  0–20  : NORMAL
  21–40 : LOW
  41–60 : MODERATE
  61–80 : HIGH
  81–100: CRITICAL
NOTE: Clearly distinguished as academic prototype thresholds, not official government disaster standards.
"""

from typing import Dict, Any, List, Tuple
import math

class RiskModel:
    @staticmethod
    def classify_risk_level(score: float) -> str:
        if score <= 20.0:
            return "NORMAL"
        elif score <= 40.0:
            return "LOW"
        elif score <= 60.0:
            return "MODERATE"
        elif score <= 80.0:
            return "HIGH"
        else:
            return "CRITICAL"

    @classmethod
    def evaluate(cls, payload: Dict[str, Any]) -> Dict[str, Any]:
        sensors = payload.get("sensors", {})
        neighbor = payload.get("neighbor_confirmation", {})
        weather = payload.get("weather", {})
        location_id = payload.get("location_id", "LOC-UNKNOWN")

        rainfall = float(sensors.get("rainfall", 0.0))
        water_level = float(sensors.get("water_level", 0.0))
        wind_speed = float(sensors.get("wind_speed", 0.0))
        humidity = float(sensors.get("humidity", 0.0))
        pressure = float(sensors.get("pressure", 1013.0))
        soil_moisture = float(sensors.get("soil_moisture", 0.0))
        temperature = float(sensors.get("temperature", 28.0))

        # Weather forecast blending
        forecast_rain = float(weather.get("rainfall", 0.0))
        effective_rain = max(rainfall, (rainfall * 0.4 + forecast_rain * 0.6))

        contributing_factors: List[str] = []
        score_components: List[float] = []

        is_neighbor_supported = bool(neighbor.get("supported", False))
        confirming_count = int(neighbor.get("confirming_count", 0))

        # 1. Heavy Rainfall + Water Level + Neighbor Confirmation (Primary Flash Flood Rule)
        if effective_rain >= 120.0 and water_level >= 4.0:
            if is_neighbor_supported:
                score_components.append(92.0)
                contributing_factors.append("Heavy rainfall (>= 120 mm) exceeding critical threshold")
                contributing_factors.append(f"Rapid water-level increase ({water_level:.1f} m) approaching river crest")
                contributing_factors.append(f"Neighboring sensor confirmation ({confirming_count} adjacent telemetry stations verified)")
            else:
                score_components.append(48.0)
                contributing_factors.append(f"Elevated rainfall reading ({effective_rain:.1f} mm), but unconfirmed by neighboring sensors (potential isolated anomaly)")
        elif effective_rain >= 70.0 or water_level >= 3.2:
            score_components.append(58.0)
            if effective_rain >= 70.0:
                contributing_factors.append(f"Substantial precipitation recorded ({effective_rain:.1f} mm)")
            if water_level >= 3.2:
                contributing_factors.append(f"Water level in warning danger zone ({water_level:.1f} m)")

        # 2. Cyclone / Gale Wind / Barometric Drop Rule
        if pressure <= 985.0 and wind_speed >= 60.0:
            score_components.append(88.0)
            contributing_factors.append(f"Severe barometric pressure drop ({pressure:.1f} hPa) coupled with tropical gale winds ({wind_speed:.1f} km/h)")
        elif wind_speed >= 50.0:
            score_components.append(45.0)
            contributing_factors.append(f"High velocity surface winds ({wind_speed:.1f} km/h)")

        # 3. High Soil Saturation + Sustained Rainfall (Landslide Risk)
        if effective_rain >= 70.0 and soil_moisture >= 80.0:
            score_components.append(65.0)
            contributing_factors.append(f"Soil moisture hyper-saturation ({soil_moisture:.0f}%) creates high slope instability / landslide vulnerability")

        # 4. Extreme Thermal Index (Heatwave / Wildfire)
        if temperature >= 43.0 and humidity <= 25.0:
            score_components.append(72.0)
            contributing_factors.append(f"Extreme ambient temperature ({temperature:.1f}°C) with dry atmospheric conditions ({humidity:.0f}% RH)")

        # Baseline Nominal Condition
        if not score_components:
            score_components.append(15.0)
            contributing_factors.append("All physical sensor readings and meteorological indicators remain within safe operational baselines")

        # Composite score calculation (highest threat with 15% blending of secondary threats)
        score_components.sort(reverse=True)
        primary_score = score_components[0]
        secondary_blend = sum(score_components[1:]) * 0.08 if len(score_components) > 1 else 0.0
        final_risk_score = round(min(100.0, max(5.0, primary_score + secondary_blend)), 1)

        # Confidence calculation
        base_confidence = 88.0
        if is_neighbor_supported:
            base_confidence += 6.0
        else:
            base_confidence -= 18.0

        if effective_rain > 0 and water_level > 0:
            base_confidence += 4.0

        confidence = round(min(98.0, max(35.0, base_confidence)), 1)

        # Trend and Prediction
        if final_risk_score >= 60.0:
            trend = "Increasing" if effective_rain > 80.0 else "Stable"
            predicted_score = min(100.0, final_risk_score + (12.0 if trend == "Increasing" else 0.0))
        elif final_risk_score >= 35.0:
            trend = "Stable"
            predicted_score = final_risk_score
        else:
            trend = "Stable"
            predicted_score = final_risk_score

        risk_level = cls.classify_risk_level(final_risk_score)
        predicted_risk = cls.classify_risk_level(predicted_score)

        return {
            "risk_score": final_risk_score,
            "risk_level": risk_level,
            "confidence": confidence,
            "trend": trend,
            "predicted_risk": predicted_risk,
            "contributing_factors": contributing_factors,
            "location_id": location_id,
            "engine_source": "PYTHON_AI",
            "prototype_disclaimer": "Academic prototype risk classification (0-20 Normal, 21-40 Low, 41-60 Mod, 61-80 High, 81-100 Critical). Not official government disaster standard."
        }
