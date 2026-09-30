"""
Disaster Early-Warning Agent - Python Risk Engine Service.
High-throughput REST microservice providing:
- Multi-criteria risk classification & prediction
- Fused sensor & ADSA graph evaluation
- Real-time weather intelligence
- PDF, CSV, JSON, HTML report exports
Runs on port 8000.
"""

from fastapi import FastAPI, HTTPException, Response, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import Dict, Any, List, Optional
import uvicorn
import logging
from datetime import datetime

from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
import os

from risk_model import RiskModel
from export_engine import generate_pdf_report, generate_csv_report, generate_html_dossier
from weather_intelligence import get_weather_for_coordinates

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")
logger = logging.getLogger("PythonRiskEngine")

app = FastAPI(
    title="Disaster Early-Warning Python Risk Engine",
    description="AI Risk Fusion and Classification Microservice",
    version="1.0.0"
)

# Enable CORS for Java backend and web dashboard
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

FRONTEND_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "frontend"))
if os.path.exists(FRONTEND_DIR):
    app.mount("/static", StaticFiles(directory=FRONTEND_DIR), name="static")

@app.get("/dashboard")
@app.get("/app")
def serve_dashboard():
    index_file = os.path.join(FRONTEND_DIR, "index.html")
    if os.path.exists(index_file):
        return FileResponse(index_file)
    return {"message": "Frontend index.html not found"}


class RiskEvaluationRequest(BaseModel):
    location_id: str = "LOC-KAKINADA"
    sensors: Dict[str, float] = Field(default_factory=dict)
    neighbor_confirmation: Dict[str, Any] = Field(default_factory=dict)
    weather: Dict[str, Any] = Field(default_factory=dict)


@app.get("/")
@app.get("/health")
def health():
    return {
        "status": "UP",
        "service": "Python Disaster Risk Engine",
        "timestamp": datetime.utcnow().isoformat() + "Z",
        "engine": "FastAPI + Multi-Criteria Risk Classification",
        "prototype_levels": {
            "0-20": "NORMAL",
            "21-40": "LOW",
            "41-60": "MODERATE",
            "61-80": "HIGH",
            "81-100": "CRITICAL"
        },
        "disclaimer": "Academic prototype risk levels. Not official disaster standards."
    }


@app.post("/api/risk/evaluate")
def evaluate_risk(payload: RiskEvaluationRequest):
    """
    Evaluates multi-hazard risk score, category, trend, and contributing factors
    based on sensor telemetry, ADSA neighbor confirmations, and weather signals.
    """
    logger.info(f"Received risk evaluation request for location: {payload.location_id}")
    result = RiskModel.evaluate(payload.dict())
    return result


@app.get("/api/weather/live")
def get_live_weather(
    lat: float = Query(16.9891, description="Latitude"),
    lon: float = Query(82.2475, description="Longitude"),
    location_id: str = Query("LOC-KAKINADA", description="Location identifier")
):
    """Fetches real-time weather and forecast data for any geographic coordinate."""
    return get_weather_for_coordinates(lat, lon, location_id)


@app.get("/api/export/pdf")
def export_pdf(
    location_id: str = "LOC-KAKINADA",
    location_name: str = "Kakinada, Andhra Pradesh",
    risk_score: float = 91.0,
    risk_level: str = "CRITICAL",
    rainfall_mm: float = 145.0,
    water_level_m: float = 4.8,
    wind_kmh: float = 62.0,
    humidity_pct: float = 94.0
):
    """Generates an emergency warning dossier PDF with ReportLab."""
    sample_data = {
        "location_id": location_id,
        "location_name": location_name,
        "hazard_title": "Heavy Rainfall & Inundation Emergency",
        "risk_score": risk_score,
        "risk_level": risk_level,
        "confidence": 92.0,
        "trend": "Increasing",
        "predicted_risk": "CRITICAL",
        "rainfall_mm": rainfall_mm,
        "water_level_m": water_level_m,
        "wind_kmh": wind_kmh,
        "humidity_pct": humidity_pct,
        "contributing_factors": [
            "Heavy rainfall exceeding critical thresholds",
            "Rapid water-level increase approaching flood mark",
            "Neighboring sensor confirmation across 3 adjacent stations"
        ]
    }
    pdf_bytes = generate_pdf_report(sample_data)
    return Response(
        content=pdf_bytes,
        media_type="application/pdf",
        headers={"Content-Disposition": f"attachment; filename=disaster_report_{location_id}.pdf"}
    )


@app.get("/api/export/csv")
def export_csv(
    location_id: str = "LOC-KAKINADA",
    location_name: str = "Kakinada",
    risk_score: float = 91.0,
    risk_level: str = "CRITICAL"
):
    """Generates CSV audit report."""
    sample_data = {
        "location_name": location_name,
        "risk_score": risk_score,
        "risk_level": risk_level,
        "confidence": 92.0,
        "trend": "Increasing",
        "rainfall_mm": 145.0,
        "water_level_m": 4.8,
        "wind_kmh": 62.0,
        "humidity_pct": 94.0,
        "contributing_factors": ["Heavy rainfall", "Rapid water-level rise", "Neighboring confirmation"]
    }
    csv_str = generate_csv_report(sample_data)
    return Response(
        content=csv_str,
        media_type="text/csv",
        headers={"Content-Disposition": f"attachment; filename=disaster_report_{location_id}.csv"}
    )


@app.get("/api/export/html")
def export_html(
    location_id: str = "LOC-KAKINADA",
    location_name: str = "Kakinada, Andhra Pradesh",
    risk_score: float = 91.0,
    risk_level: str = "CRITICAL"
):
    """Returns a printable HTML emergency dossier."""
    sample_data = {
        "location_name": location_name,
        "risk_score": risk_score,
        "risk_level": risk_level,
        "confidence": 92.0,
        "trend": "Increasing",
        "rainfall_mm": 145.0,
        "water_level_m": 4.8,
        "wind_kmh": 62.0,
        "humidity_pct": 94.0,
        "contributing_factors": ["Heavy rainfall", "Rapid water-level rise", "Neighboring confirmation"]
    }
    html_str = generate_html_dossier(sample_data)
    return Response(content=html_str, media_type="text/html")


if __name__ == "__main__":
    logger.info("Starting Python Risk Engine FastAPI service on port 8000...")
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=False)
