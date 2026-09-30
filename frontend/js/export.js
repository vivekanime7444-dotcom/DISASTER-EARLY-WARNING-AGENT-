/**
 * Multi-Format Download & Share Engine.
 * Allows downloading disaster intelligence dossiers and alerts as PDF, CSV, JSON, and HTML.
 * Includes fallback client-side generation when offline.
 */

const ExportManager = {
    /**
     * Downloads an alert report in the requested format (pdf, csv, json, html).
     */
    async downloadReport(format = 'pdf', alertData = null) {
        const data = alertData || window.currentDisasterContext || this.getDefaultData();
        const locId = data.location_id || 'LOC-CURRENT';

        // 1. Try Java backend download endpoint first
        try {
            const javaUrl = `http://localhost:8080/api/download?format=${format}&location_id=${locId}`;
            const res = await fetch(javaUrl);
            if (res.ok) {
                const blob = await res.blob();
                this.triggerFileDownload(blob, `Disaster_Report_${locId}.${format}`);
                return;
            }
        } catch (e) {
            console.warn('Java download endpoint unreachable, checking Python export engine...');
        }

        // 2. Try Python backend export endpoint
        try {
            const pyUrl = `http://localhost:8000/api/export/${format}?location_id=${locId}&risk_score=${data.risk_score || 91}&risk_level=${data.risk_level || 'CRITICAL'}`;
            const res = await fetch(pyUrl);
            if (res.ok) {
                const blob = await res.blob();
                this.triggerFileDownload(blob, `Disaster_Report_${locId}.${format}`);
                return;
            }
        } catch (e) {
            console.warn('Python export endpoint unreachable, generating client-side...');
        }

        // 3. Resilient Client-side Generation Fallback
        if (format === 'json') {
            const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
            this.triggerFileDownload(blob, `Disaster_Report_${locId}.json`);
        } else if (format === 'csv') {
            const csvContent = this.generateClientCsv(data);
            const blob = new Blob([csvContent], { type: 'text/csv' });
            this.triggerFileDownload(blob, `Disaster_Report_${locId}.csv`);
        } else if (format === 'html' || format === 'pdf') {
            // For PDF/HTML in client fallback, open formatted print-ready dossier
            this.openPrintableDossier(data);
        }
    },

    triggerFileDownload(blob, filename) {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    },

    generateClientCsv(data) {
        return `Report_Type,Disaster Early-Warning Intelligence Audit\n` +
               `Generated_At,${new Date().toISOString()}\n` +
               `Location,${data.location_name || 'Monitored Sector'}\n` +
               `Risk_Score,${data.risk_score || 18.0}\n` +
               `Risk_Level,${data.risk_level || 'NORMAL'}\n` +
               `Confidence,${data.confidence || 90}%\n` +
               `Trend,${data.trend || 'STABLE'}\n` +
               `Disaster_Radius_KM,100.0\n` +
               `Weather_Radius_KM,150.0\n` +
               `Rainfall_MM,${data.rainfall_mm || 0}\n` +
               `Water_Level_M,${data.water_level_m || 0}\n` +
               `Wind_KMH,${data.wind_kmh || 0}\n` +
               `Contributing_Factors,"${(data.reasons || []).join('; ')}"\n` +
               `Disclaimer,Academic prototype research tool. Not official government warning.\n`;
    },

    openPrintableDossier(data) {
        const printWin = window.open('', '_blank');
        const color = (data.risk_level === 'CRITICAL' ? '#ef4444' : (data.risk_level === 'HIGH' ? '#f97316' : '#10b981'));
        const reasons = (data.reasons || ['Operational telemetry normal']).map(r => `<li>${r}</li>`).join('');

        printWin.document.write(`
            <!DOCTYPE html>
            <html>
            <head>
                <title>Emergency Warning Dossier - ${data.location_name || 'Monitored Sector'}</title>
                <style>
                    body { font-family: 'Segoe UI', system-ui, sans-serif; padding: 30px; background: #0b0f19; color: #f1f5f9; }
                    .card { max-width: 800px; margin: auto; background: #131b2e; border: 1px solid #1e293b; border-radius: 12px; padding: 24px; }
                    .header { display: flex; justify-content: space-between; border-bottom: 2px solid #1e293b; padding-bottom: 12px; margin-bottom: 20px; }
                    .badge { background: ${color}; color: #fff; padding: 6px 14px; border-radius: 9999px; font-weight: bold; }
                    .grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 20px; }
                    .box { background: #1e293b; padding: 12px; border-radius: 8px; border-left: 4px solid #3b82f6; }
                    .lbl { font-size: 11px; color: #94a3b8; text-transform: uppercase; }
                    .val { font-size: 20px; font-weight: bold; margin-top: 4px; }
                    .print-btn { background: #3b82f6; color: white; border: none; padding: 10px 20px; border-radius: 6px; cursor: pointer; font-size: 14px; margin-top: 15px; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="header">
                        <div>
                            <h2 style="margin:0; color:#ef4444;">🚨 DISASTER & WEATHER REPORT</h2>
                            <div style="font-size:12px; color:#94a3b8;">Location: <b>${data.location_name || 'Monitored Sector'}</b> | Time: ${new Date().toUTCString()}</div>
                        </div>
                        <div class="badge">${data.risk_level || 'NORMAL'} (${data.risk_score || 18}/100)</div>
                    </div>
                    <div class="grid">
                        <div class="box"><div class="lbl">Risk Score</div><div class="val">${data.risk_score || 18}/100</div></div>
                        <div class="box"><div class="lbl">100km Disaster Radius</div><div class="val" style="color:#ef4444;">ACTIVE</div></div>
                        <div class="box"><div class="lbl">150km Weather Radius</div><div class="val" style="color:#3b82f6;">ACTIVE</div></div>
                        <div class="box"><div class="lbl">Rainfall</div><div class="val">${data.rainfall_mm || 0} mm</div></div>
                        <div class="box"><div class="lbl">Water Level</div><div class="val">${data.water_level_m || 0} m</div></div>
                        <div class="box"><div class="lbl">Wind Speed</div><div class="val">${data.wind_kmh || 0} km/h</div></div>
                    </div>
                    <h4>Contributing Factors & Reasoning:</h4>
                    <ul>${reasons}</ul>
                    <button class="print-btn" onclick="window.print()">🖨️ Print / Save as PDF</button>
                    <p style="font-size:11px; color:#64748b; margin-top:20px;">
                        <b>Academic Prototype Notice:</b> This dossier is generated by an automated multi-criteria early-warning system. Thresholds are prototype specifications and do NOT replace official state emergency warnings.
                    </p>
                </div>
            </body>
            </html>
        `);
        printWin.document.close();
    },

    /**
     * Web Share API for sharing alerts where supported.
     */
    async shareAlert(alertData) {
        const data = alertData || window.currentDisasterContext || this.getDefaultData();
        const text = `🚨 ${data.hazard_title || 'Disaster Alert'} in ${data.location_name || 'Local Area'}! Risk Score: ${data.risk_score || 91}/100 (${data.risk_level || 'CRITICAL'}). Stay safe! (Academic prototype warning)`;

        if (navigator.share) {
            try {
                await navigator.share({
                    title: '🚨 Emergency Warning Notice',
                    text: text,
                    url: window.location.href
                });
            } catch (err) {
                if (err.name !== 'AbortError') console.log('Share error:', err);
            }
        } else {
            // Fallback: Copy to clipboard
            await navigator.clipboard.writeText(text);
            alert('Emergency Alert text copied to clipboard for sharing!');
        }
    },

    getDefaultData() {
        return {
            location_id: 'LOC-KAKINADA',
            location_name: 'Kakinada, Andhra Pradesh',
            hazard_title: 'Heavy Rainfall & Flash Flood Emergency',
            risk_score: 91.0,
            risk_level: 'CRITICAL',
            confidence: 92.0,
            trend: 'Increasing',
            predicted_risk: 'CRITICAL',
            rainfall_mm: 145.0,
            water_level_m: 4.8,
            wind_kmh: 62.0,
            humidity_pct: 94.0,
            reasons: [
                'Heavy rainfall exceeding critical threshold (>= 120 mm)',
                'Rapid water-level increase approaching flood mark (4.8 m)',
                'Neighboring sensor confirmation across adjacent stations'
            ]
        };
    }
};

window.ExportManager = ExportManager;
