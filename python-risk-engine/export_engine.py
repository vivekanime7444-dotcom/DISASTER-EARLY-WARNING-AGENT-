"""
Comprehensive Multi-Format Export Engine for Disaster Early-Warning Agent.
Generates:
1. PDF (using ReportLab)
2. CSV
3. JSON
4. Printable HTML Dossier
Includes: location, hazard, readings, risk, confidence, trend, weather, contributing factors, timestamps, alert ID, and disclaimer.
"""

import io
import json
import csv
from datetime import datetime
from typing import Dict, Any, Optional

try:
    from reportlab.lib.pagesizes import letter
    from reportlab.lib import colors
    from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
    from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, HRFlowable
    REPORTLAB_AVAILABLE = True
except ImportError:
    REPORTLAB_AVAILABLE = False


def generate_pdf_report(data: Dict[str, Any]) -> bytes:
    """Generates a high-resolution, formatted PDF disaster intelligence report."""
    if not REPORTLAB_AVAILABLE:
        # Fallback to plain text buffer if reportlab is somehow missing
        buffer = io.BytesIO()
        buffer.write(f"PDF GENERATION: {json.dumps(data, indent=2)}".encode('utf-8'))
        return buffer.getvalue()

    buffer = io.BytesIO()
    doc = SimpleDocTemplate(
        buffer,
        pagesize=letter,
        rightMargin=36,
        leftMargin=36,
        topMargin=36,
        bottomMargin=36
    )

    styles = getSampleStyleSheet()
    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Heading1'],
        fontSize=20,
        leading=24,
        textColor=colors.HexColor('#dc2626'),
        spaceAfter=6
    )
    subtitle_style = ParagraphStyle(
        'DocSubtitle',
        parent=styles['Normal'],
        fontSize=11,
        textColor=colors.HexColor('#64748b'),
        spaceAfter=14
    )
    body_bold = ParagraphStyle(
        'BodyBold',
        parent=styles['Normal'],
        fontSize=10,
        leading=14,
        textColor=colors.HexColor('#0f172a'),
        fontName='Helvetica-Bold'
    )
    body_text = ParagraphStyle(
        'BodyText',
        parent=styles['Normal'],
        fontSize=10,
        leading=14,
        textColor=colors.HexColor('#334155')
    )
    disclaimer_style = ParagraphStyle(
        'Disclaimer',
        parent=styles['Italic'],
        fontSize=8,
        leading=11,
        textColor=colors.HexColor('#94a3b8')
    )

    elements = []

    # Title & Metadata
    elements.append(Paragraph("🚨 UNIVERSAL DISASTER EARLY-WARNING INTELLIGENCE DOSSIER", title_style))
    elements.append(Paragraph(f"Generated at: {datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S UTC')} | Mission Operational Reference", subtitle_style))
    elements.append(HRFlowable(width="100%", thickness=1.5, color=colors.HexColor('#cbd5e1'), spaceAfter=14))

    # Key Metrics Table
    risk_level = data.get("risk_level", "NORMAL")
    risk_score = data.get("risk_score", 15.0)
    badge_color = colors.HexColor('#ef4444') if risk_level == "CRITICAL" else (
        colors.HexColor('#f97316') if risk_level == "HIGH" else (
            colors.HexColor('#f59e0b') if risk_level == "MODERATE" else colors.HexColor('#10b981')
        )
    )

    summary_data = [
        [Paragraph("Location / Sector:", body_bold), Paragraph(str(data.get("location_name", "Monitored Region")), body_text)],
        [Paragraph("Hazard Assessment:", body_bold), Paragraph(str(data.get("hazard_title", "Environmental Monitoring")), body_text)],
        [Paragraph("Composite Risk Score:", body_bold), Paragraph(f"<b>{risk_score}/100</b> ({risk_level})", body_bold)],
        [Paragraph("Analysis Confidence:", body_bold), Paragraph(f"{data.get('confidence', 90.0)}%", body_text)],
        [Paragraph("24h Trend:", body_bold), Paragraph(str(data.get("trend", "STABLE")), body_text)],
        [Paragraph("Predicted Risk Category:", body_bold), Paragraph(str(data.get("predicted_risk", risk_level)), body_text)],
        [Paragraph("Disaster Warning Radius:", body_bold), Paragraph("100.0 KM (Disaster + Weather Alert Active)", body_text)],
        [Paragraph("Weather Advisory Radius:", body_bold), Paragraph("150.0 KM (Weather Alert Only Active)", body_text)]
    ]

    summary_table = Table(summary_data, colWidths=[180, 360])
    summary_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), colors.HexColor('#f8fafc')),
        ('BOX', (0, 0), (-1, -1), 1, colors.HexColor('#e2e8f0')),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, colors.HexColor('#e2e8f0')),
        ('PADDING', (0, 0), (-1, -1), 6),
    ]))
    elements.append(summary_table)
    elements.append(Spacer(1, 14))

    # Sensor Telemetry Readings Table
    elements.append(Paragraph("<b>Physical Sensor & Meteorological Telemetry Snapshot</b>", body_bold))
    elements.append(Spacer(1, 6))

    telemetry_data = [
        ["Parameter", "Observed Value", "Unit", "Threshold Status"],
        ["Rainfall (24h Accumulation)", f"{data.get('rainfall_mm', 0.0):.1f}", "mm", "Elevated" if data.get('rainfall_mm', 0.0) >= 70 else "Normal"],
        ["Water Level (Hydro Sluice)", f"{data.get('water_level_m', 0.0):.1f}", "m", "Critical" if data.get('water_level_m', 0.0) >= 4.0 else "Normal"],
        ["Surface Wind Speed", f"{data.get('wind_kmh', 0.0):.1f}", "km/h", "Gale Warning" if data.get('wind_kmh', 0.0) >= 55 else "Nominal"],
        ["Relative Humidity", f"{data.get('humidity_pct', 0.0):.0f}", "%", "High Saturation" if data.get('humidity_pct', 0.0) >= 85 else "Nominal"],
        ["Ambient Temperature", f"{data.get('temperature_c', 28.0):.1f}", "°C", "Nominal"],
    ]

    tel_table = Table(telemetry_data, colWidths=[180, 120, 80, 160])
    tel_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#0f172a')),
        ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
        ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
        ('BOTTOMPADDING', (0, 0), (-1, 0), 6),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, colors.HexColor('#cbd5e1')),
        ('BOX', (0, 0), (-1, -1), 1, colors.HexColor('#94a3b8')),
        ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, colors.HexColor('#f1f5f9')]),
        ('PADDING', (0, 0), (-1, -1), 5),
    ]))
    elements.append(tel_table)
    elements.append(Spacer(1, 14))

    # Contributing Factors
    elements.append(Paragraph("<b>AI Rational Agent Reasoning & Contributing Factors:</b>", body_bold))
    elements.append(Spacer(1, 4))
    factors = data.get("contributing_factors", ["Telemetry within safe limits."])
    for f in factors:
        elements.append(Paragraph(f"• {f}", body_text))

    elements.append(Spacer(1, 18))
    elements.append(HRFlowable(width="100%", thickness=1, color=colors.HexColor('#e2e8f0'), spaceAfter=8))
    elements.append(Paragraph(
        "<b>DISCLAIMER:</b> This early-warning dossier is generated by an academic prototype research system "
        "fusing multi-criteria IoT sensor telemetry, ADSA sensor graph correlation, and Python AI risk classification. "
        "Thresholds (0-20 Normal, 21-40 Low, 41-60 Mod, 61-80 High, 81-100 Critical) are prototype standards "
        "and do NOT replace official National/State Disaster Management Authority emergency notices.",
        disclaimer_style
    ))

    doc.build(elements)
    return buffer.getvalue()


def generate_csv_report(data: Dict[str, Any]) -> str:
    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(["Field", "Value", "Unit / Context"])
    writer.writerow(["Report Title", "Disaster Early-Warning Intelligence Audit", "System Generated"])
    writer.writerow(["Timestamp UTC", datetime.utcnow().isoformat(), "ISO-8601"])
    writer.writerow(["Location", data.get("location_name", "Monitored Sector"), "Geographic Registry"])
    writer.writerow(["Risk Score", data.get("risk_score", 15.0), "0-100 Scale"])
    writer.writerow(["Risk Level", data.get("risk_level", "NORMAL"), "Prototype Classification"])
    writer.writerow(["Confidence", f"{data.get('confidence', 90.0)}%", "Percentage"])
    writer.writerow(["Trend", data.get("trend", "STABLE"), "Directional Gradient"])
    writer.writerow(["Disaster Alert Radius", "100.0", "KM"])
    writer.writerow(["Weather Alert Radius", "150.0", "KM"])
    writer.writerow(["Rainfall", data.get("rainfall_mm", 0.0), "mm"])
    writer.writerow(["Water Level", data.get("water_level_m", 0.0), "m"])
    writer.writerow(["Wind Speed", data.get("wind_kmh", 0.0), "km/h"])
    writer.writerow(["Humidity", data.get("humidity_pct", 0.0), "%"])
    writer.writerow(["Contributing Factors", "; ".join(data.get("contributing_factors", [])), "Expert Reasoning"])
    writer.writerow(["Disclaimer", "Academic prototype research platform. Not official government warning.", "Legal Notice"])
    return output.getvalue()


def generate_html_dossier(data: Dict[str, Any]) -> str:
    risk_level = data.get("risk_level", "NORMAL")
    risk_score = data.get("risk_score", 15.0)
    color = "#ef4444" if risk_level == "CRITICAL" else ("#f97316" if risk_level == "HIGH" else "#10b981")

    factors_html = "".join([f"<li>{f}</li>" for f in data.get("contributing_factors", ["Nominal conditions."])])

    return f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Emergency Warning Dossier - {data.get('location_name', 'Monitored Sector')}</title>
  <style>
    body {{ font-family: 'Segoe UI', system-ui, sans-serif; background: #0b0f19; color: #f1f5f9; padding: 40px; margin: 0; }}
    .container {{ max-width: 840px; margin: 0 auto; background: #131b2e; border: 1px solid #1e293b; border-radius: 16px; padding: 32px; box-shadow: 0 20px 40px rgba(0,0,0,0.5); }}
    .header {{ display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #1e293b; padding-bottom: 16px; }}
    .badge {{ background: {color}; color: #fff; padding: 6px 16px; border-radius: 9999px; font-weight: bold; font-size: 14px; text-transform: uppercase; }}
    .grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; margin: 24px 0; }}
    .card {{ background: #1e293b; padding: 16px; border-radius: 10px; border-left: 4px solid #3b82f6; }}
    .card .val {{ font-size: 24px; font-weight: 700; margin-top: 4px; }}
    .card .lbl {{ font-size: 11px; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.5px; }}
    .factors {{ background: #1e293b; padding: 20px; border-radius: 12px; margin: 20px 0; }}
    .factors ul {{ margin: 8px 0 0 20px; padding: 0; }}
    .factors li {{ margin-bottom: 6px; color: #cbd5e1; }}
    .btn {{ background: #3b82f6; color: #fff; padding: 12px 24px; border: none; border-radius: 8px; cursor: pointer; font-weight: 600; font-size: 14px; }}
    .disclaimer {{ font-size: 11px; color: #64748b; margin-top: 24px; line-height: 1.5; }}
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <div>
        <h2 style="margin:0; color:#ef4444;">🚨 DISASTER & WEATHER REPORT</h2>
        <div style="color:#94a3b8; font-size:13px; margin-top:4px;">Location: <b>{data.get('location_name', 'Monitored Sector')}</b> | Generated: {datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S UTC')}</div>
      </div>
      <div class="badge">{risk_level} ({risk_score}/100)</div>
    </div>

    <div class="grid">
      <div class="card" style="border-left-color: {color};">
        <div class="lbl">Risk Score</div>
        <div class="val">{risk_score}/100</div>
      </div>
      <div class="card">
        <div class="lbl">Confidence</div>
        <div class="val">{data.get('confidence', 90.0)}%</div>
      </div>
      <div class="card">
        <div class="lbl">Trend</div>
        <div class="val">{data.get('trend', 'STABLE')}</div>
      </div>
      <div class="card">
        <div class="lbl">100km Disaster Zone</div>
        <div class="val" style="color:#ef4444;">ACTIVE</div>
      </div>
      <div class="card">
        <div class="lbl">150km Weather Zone</div>
        <div class="val" style="color:#3b82f6;">ACTIVE</div>
      </div>
    </div>

    <div class="grid">
      <div class="card"><div class="lbl">Rainfall</div><div class="val">{data.get('rainfall_mm', 0.0):.1f} mm</div></div>
      <div class="card"><div class="lbl">Water Level</div><div class="val">{data.get('water_level_m', 0.0):.1f} m</div></div>
      <div class="card"><div class="lbl">Wind Speed</div><div class="val">{data.get('wind_kmh', 0.0):.1f} km/h</div></div>
      <div class="card"><div class="lbl">Humidity</div><div class="val">{data.get('humidity_pct', 0.0):.0f}%</div></div>
    </div>

    <div class="factors">
      <h4 style="margin:0; color:#f8fafc;">Key Contributing Factors & Reasoning:</h4>
      <ul>{factors_html}</ul>
    </div>

    <button class="btn" onclick="window.print()">🖨️ Print / Save as PDF</button>

    <div class="disclaimer">
      <b>ACADEMIC PROTOTYPE NOTICE:</b> This warning dossier is created for college research demonstration.
      Thresholds are academic prototypes (0-20 Normal, 21-40 Low, 41-60 Moderate, 61-80 High, 81-100 Critical) and do not represent official disaster standards.
    </div>
  </div>
</body>
</html>
"""
