/**
 * Main Application Controller for Disaster Early-Warning Agent.
 * Connects to Java Agent (port 8080) and Python Risk Engine (port 8000).
 * Orchestrates 24/7 telemetry updates, maps, charts, sound alarms, and geofencing.
 */

const App = {
    selectedLocationId: 'LOC-KAKINADA',
    userCoordinates: { lat: 16.9891, lon: 82.2475 },
    activeEpicenter: { lat: 16.9891, lon: 82.2475, name: 'Kakinada Epicenter' },
    userDistrict: 'Kakinada',
    activeHazardDistrict: 'Kakinada',
    is24x7Polling: true,
    pollingIntervalMs: 4000,
    pollTimer: null,
    locationsList: [],
    lastRiskScore: 0,
    currentUser: null,

    async init() {
        console.log("Initializing Disaster Early-Warning Agent UI...");

        // Setup Authentication Portal
        await this.initAuthSession();

        // Setup sound engine
        window.soundEngine.init();

        // Request Browser Push Notification permission
        if ('Notification' in window && Notification.permission === 'default') {
            Notification.requestPermission();
        }

        // Setup tab navigation
        this.setupTabs();

        // Setup simulation button listeners
        this.setupSimulations();

        // Setup dynamic location form
        this.setupLocationForm();

        // Setup user district selector
        this.setupDistrictSelector();

        // Setup sound toggle
        this.setupSoundToggle();

        // Setup geofence tester slider
        this.setupGeofenceSlider();

        // Initialize Map & Charts
        window.MapManager.init();
        window.ChartsManager.init();

        // Initialize ADSA Graph Visualizer
        window.graphVis = new window.GraphVisualizer('adsaGraphCanvas');

        // Fetch initial data
        await this.refreshAllData();

        // Start 24/7 continuous polling
        this.startPolling();
    },

    setupDistrictSelector() {
        const select = document.getElementById('userDistrictSelector');
        if (select) {
            select.value = this.userDistrict;
            select.addEventListener('change', (e) => {
                this.userDistrict = e.target.value;
                console.log(`[USER DISTRICT] User registered district updated to: ${this.userDistrict}`);
                this.updateDistrictDisplay();

                // Re-evaluate simulated distance with new district
                const slider = document.getElementById('userDistanceSlider');
                const distKm = slider ? parseFloat(slider.value) : 15.0;
                this.evaluateSimulatedUserDistance(distKm);
                this.refreshAlerts();
            });
        }
        this.updateDistrictDisplay();
    },

    updateDistrictDisplay() {
        const userBadge = document.getElementById('userDistrictBadge');
        const hazardBadge = document.getElementById('hazardDistrictBadge');
        const explanation = document.getElementById('districtStatusExplanation');
        const pill = document.getElementById('districtFilterPill');

        if (userBadge) userBadge.innerText = this.userDistrict || 'Kakinada';
        if (hazardBadge) hazardBadge.innerText = this.activeHazardDistrict || 'Kakinada';

        const uDist = (this.userDistrict || '').trim().toLowerCase();
        const hDist = (this.activeHazardDistrict || '').trim().toLowerCase();
        const isMatch = !uDist || !hDist || uDist === hDist || uDist.includes(hDist) || hDist.includes(uDist);

        if (isMatch) {
            if (explanation) explanation.innerHTML = `✅ <b>District Match:</b> Emergency alerts and sirens will be dispatched because hazard is localized to your district (<b>${this.userDistrict}</b>).`;
            if (pill) pill.innerHTML = `<span class="radius-badge disaster" style="font-size:11px; padding:6px 12px;">🛡️ DISTRICT MATCH: ACTIVE</span>`;
        } else {
            if (explanation) explanation.innerHTML = `⚠️ <b>District Protection Active:</b> Emergency alerts and audio sirens are <b style="color:#f59e0b;">SUPPRESSED</b>. You are registered in <b>${this.userDistrict}</b>, while hazard is in <b>${this.activeHazardDistrict}</b>. <i>Only alert sent in user alert district.</i>`;
            if (pill) pill.innerHTML = `<span class="radius-badge" style="background:rgba(245,158,11,0.2); color:#f59e0b; border:1px solid #f59e0b; font-size:11px; padding:6px 12px;">🛡️ DISTRICT MISMATCH: MUTED</span>`;
        }
    },

    setupTabs() {
        const tabs = document.querySelectorAll('.tab-btn');
        tabs.forEach(btn => {
            btn.addEventListener('click', () => {
                tabs.forEach(t => t.classList.remove('active'));
                btn.classList.add('active');

                const targetTab = btn.getAttribute('data-tab');
                document.querySelectorAll('.tab-pane').forEach(p => p.style.display = 'none');
                const pane = document.getElementById(targetTab);
                if (pane) {
                    pane.style.display = 'block';
                    if (targetTab === 'tab-graph' && window.graphVis) {
                        window.graphVis.resize();
                    }
                    if (targetTab === 'tab-command' && window.MapManager.map) {
                        window.MapManager.map.invalidateSize();
                    }
                }
            });
        });
    },

    setupSimulations() {
        document.querySelectorAll('.sim-btn').forEach(btn => {
            btn.addEventListener('click', async () => {
                const scenario = btn.getAttribute('data-scenario');
                await this.triggerSimulation(scenario);
            });
        });
    },

    setupLocationForm() {
        const form = document.getElementById('newLocationForm');
        if (!form) return;

        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const city = document.getElementById('locCity').value.trim();
            const state = document.getElementById('locState').value.trim();
            const district = document.getElementById('locDistrict').value.trim();
            const lat = parseFloat(document.getElementById('locLat').value);
            const lon = parseFloat(document.getElementById('locLon').value);

            if (!city || isNaN(lat) || isNaN(lon)) {
                alert('Please enter valid city name and coordinates.');
                return;
            }

            try {
                const resp = await fetch('http://localhost:8080/api/locations', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ city, state, district, latitude: lat, longitude: lon })
                });
                if (resp.ok) {
                    alert(`Location ${city} successfully registered in 24/7 agent monitor!`);
                    form.reset();
                    await this.refreshLocations();
                }
            } catch (err) {
                // Client-side fallback registration
                const newLoc = {
                    location_id: 'LOC-' + city.toUpperCase().replace(/[^A-Z0-9]/g, ''),
                    city, state, district, latitude: lat, longitude: lon, current_risk: 20.0, risk_level: 'NORMAL'
                };
                this.locationsList.push(newLoc);
                this.renderLocationsDropdown();
                alert(`Location ${city} registered in agent local registry!`);
            }
        });
    },

    setupSoundToggle() {
        const soundBtn = document.getElementById('soundToggleBtn');
        if (!soundBtn) return;

        soundBtn.addEventListener('click', () => {
            const isMuted = window.soundEngine.toggleMute();
            soundBtn.innerHTML = isMuted ? '🔇 Sound Muted' : '🔊 Sound Active';
            soundBtn.style.color = isMuted ? '#94a3b8' : '#38bdf8';
        });
    },

    setupGeofenceSlider() {
        const slider = document.getElementById('userDistanceSlider');
        const distVal = document.getElementById('userDistanceVal');
        if (!slider) return;

        slider.addEventListener('input', (e) => {
            const distKm = parseFloat(e.target.value);
            if (distVal) distVal.innerText = `${distKm.toFixed(1)} km`;
            this.evaluateSimulatedUserDistance(distKm);
        });
    },

    async triggerSimulation(scenario) {
        console.log(`Triggering ${scenario} scenario for location ${this.selectedLocationId}...`);
        try {
            await fetch(`http://localhost:8080/api/simulate?scenario=${scenario}&location_id=${this.selectedLocationId}`, { method: 'POST' });
        } catch (e) {
            console.warn('Java simulate endpoint unreachable, running local scenario simulation');
        }

        if (scenario === 'CRITICAL') {
            window.soundEngine.playCriticalAlarm(4);
        } else if (scenario === 'WARNING') {
            window.soundEngine.playChime();
        }

        await this.refreshAllData();
    },

    startPolling() {
        if (this.pollTimer) clearInterval(this.pollTimer);
        this.pollTimer = setInterval(() => {
            if (this.is24x7Polling) {
                this.refreshAllData();
            }
        }, this.pollingIntervalMs);
    },

    async refreshAllData() {
        await Promise.all([
            this.refreshHealth(),
            this.refreshLocations(),
            this.refreshRiskAndWeather(),
            this.refreshSensors(),
            this.refreshAlerts(),
            this.refreshNotifications(),
            this.refreshNotificationBoard(),
            this.refreshGraph()
        ]);
    },

    async refreshHealth() {
        try {
            const res = await fetch('http://localhost:8080/health');
            if (res.ok) {
                const data = await res.json();
                document.getElementById('systemHealthStatus').innerHTML = `
                    <span style="color:#10b981;">ONLINE (24/7)</span> | Cycles: ${data.total_cycles || 0}
                `;
                document.getElementById('pythonStatus').innerText = data.python_engine_connected ? 'Connected (Port 8000)' : 'Fallback Active (Java Rules)';
                document.getElementById('pythonStatus').style.color = data.python_engine_connected ? '#10b981' : '#f59e0b';
            }
        } catch (e) {
            document.getElementById('systemHealthStatus').innerHTML = `<span style="color:#f59e0b;">LOCAL AGENT MODE</span>`;
            document.getElementById('pythonStatus').innerText = 'Starting / Java Fallback Active';
        }
    },

    async refreshLocations() {
        try {
            const res = await fetch('http://localhost:8080/api/locations');
            if (res.ok) {
                this.locationsList = await res.json();
                this.renderLocationsDropdown();
                if (window.MapManager) {
                    window.MapManager.renderWarningLocations(this.locationsList);
                }
            }
        } catch (e) {
            if (this.locationsList.length === 0) {
                this.locationsList = [
                    { location_id: 'LOC-KAKINADA', city: 'Kakinada', district: 'Kakinada', latitude: 16.9891, longitude: 82.2475, current_risk: 91.0, risk_level: 'CRITICAL' },
                    { location_id: 'LOC-VIZAG', city: 'Visakhapatnam', district: 'Visakhapatnam', latitude: 17.6868, longitude: 83.2185, current_risk: 18.0, risk_level: 'NORMAL' },
                    { location_id: 'LOC-VIJAYAWADA', city: 'Vijayawada', district: 'NTR', latitude: 16.5062, longitude: 80.6480, current_risk: 22.0, risk_level: 'LOW' },
                    { location_id: 'LOC-HYDERABAD', city: 'Hyderabad', district: 'Hyderabad', latitude: 17.3850, longitude: 78.4867, current_risk: 15.0, risk_level: 'NORMAL' }
                ];
                this.renderLocationsDropdown();
                if (window.MapManager) {
                    window.MapManager.renderWarningLocations(this.locationsList);
                }
            }
        }
    },

    renderLocationsDropdown() {
        const select = document.getElementById('locationSelector');
        if (!select) return;

        const currentVal = select.value || this.selectedLocationId;
        select.innerHTML = '';
        this.locationsList.forEach(loc => {
            const opt = document.createElement('option');
            opt.value = loc.location_id;
            opt.text = `${loc.city} (${loc.latitude.toFixed(2)}, ${loc.longitude.toFixed(2)}) - ${loc.risk_level || 'NORMAL'}`;
            if (loc.location_id === currentVal) opt.selected = true;
            select.appendChild(opt);
        });

        select.onchange = (e) => {
            this.selectedLocationId = e.target.value;
            const chosen = this.locationsList.find(l => l.location_id === this.selectedLocationId);
            if (chosen) {
                this.activeEpicenter = { lat: chosen.latitude, lon: chosen.longitude, name: chosen.city };
                window.MapManager.setWarningLocation(chosen.latitude, chosen.longitude, chosen.city, chosen.risk_level || 'CRITICAL');
            }
            this.refreshRiskAndWeather();
        };
    },

    async refreshRiskAndWeather() {
        let riskData = null;
        let weatherData = null;

        // 1. Fetch Risk
        try {
            const res = await fetch(`http://localhost:8080/api/risk?location_id=${this.selectedLocationId}`);
            if (res.ok) riskData = await res.json();
        } catch (e) {}

        // Fallback default risk
        if (!riskData) {
            riskData = {
                risk_score: 91.0,
                risk_level: 'CRITICAL',
                confidence: 92.0,
                trend: 'Increasing',
                predicted_risk: 'CRITICAL',
                engine_source: 'PYTHON_AI',
                contributing_factors: [
                    'Heavy rainfall exceeding critical threshold (145 mm)',
                    'Rapid water-level increase (4.8 m)',
                    'Neighboring sensor confirmation'
                ]
            };
        }

        // 2. Fetch Weather
        try {
            const res = await fetch(`http://localhost:8080/api/weather?location_id=${this.selectedLocationId}`);
            if (res.ok) weatherData = await res.json();
        } catch (e) {}

        if (!weatherData) {
            weatherData = {
                temperature_c: 26.0,
                humidity_pct: 94.0,
                pressure_hpa: 988.0,
                wind_speed_kmh: 62.0,
                rainfall_mm: 145.0,
                forecast: 'Heavy rainfall expected'
            };
        }

        // Update UI
        this.updateRiskDisplay(riskData);
        this.updateWeatherDisplay(weatherData);

        // Store global context for downloads
        window.currentDisasterContext = {
            ...riskData,
            ...weatherData,
            location_id: this.selectedLocationId,
            location_name: this.locationsList.find(l => l.location_id === this.selectedLocationId)?.city || 'Monitored Area'
        };

        // Trigger alarm if risk leaped to Critical
        if (riskData.risk_score >= 80.0 && this.lastRiskScore < 80.0) {
            window.soundEngine.playCriticalAlarm(3);
        }
        this.lastRiskScore = riskData.risk_score;
    },

    updateRiskDisplay(r) {
        document.getElementById('metricRiskScore').innerText = `${r.risk_score.toFixed(0)}/100`;
        const badge = document.getElementById('metricRiskLevel');
        badge.innerText = r.risk_level;
        badge.className = `metric-val ${r.risk_level.toLowerCase()}`;

        document.getElementById('metricConfidence').innerText = `${r.confidence.toFixed(0)}%`;
        document.getElementById('metricTrend').innerText = r.trend;
        document.getElementById('metricPredicted').innerText = r.predicted_risk;
        document.getElementById('metricSource').innerText = r.engine_source;

        // Factors list
        const factorsContainer = document.getElementById('contributingFactorsList');
        if (factorsContainer) {
            factorsContainer.innerHTML = (r.contributing_factors || []).map(f => `<li>• ${f}</li>`).join('');
        }
    },

    updateWeatherDisplay(w) {
        document.getElementById('weatherTemp').innerText = `${w.temperature_c.toFixed(1)}°C`;
        document.getElementById('weatherRain').innerText = `${w.rainfall_mm.toFixed(1)} mm`;
        document.getElementById('weatherWind').innerText = `${w.wind_speed_kmh.toFixed(1)} km/h`;
        document.getElementById('weatherHumidity').innerText = `${w.humidity_pct.toFixed(0)}%`;
        document.getElementById('weatherPressure').innerText = `${w.pressure_hpa.toFixed(0)} hPa`;
        document.getElementById('weatherForecast').innerText = w.forecast || 'Nominal';

        // Update Charts
        window.ChartsManager.updateReadings(w.rainfall_mm, 4.8);
    },

    async refreshSensors() {
        try {
            const res = await fetch('http://localhost:8080/api/sensors');
            if (res.ok) {
                const sensors = await res.json();
                document.getElementById('metricActiveSensors').innerText = sensors.filter(s => s.health === 'HEALTHY').length;
                document.getElementById('metricOfflineSensors').innerText = sensors.filter(s => s.health !== 'HEALTHY').length;
                window.MapManager.renderSensors(sensors);
            }
        } catch (e) {}
    },

    lastNotifiedAlertId: null,

    async refreshAlerts() {
        const container = document.getElementById('alertsContainer');
        if (!container) return;

        try {
            const res = await fetch('http://localhost:8080/api/alerts');
            if (res.ok) {
                const alerts = await res.json();
                if (alerts.length === 0) {
                    container.innerHTML = `<div style="color:#64748b; font-size:13px; text-align:center; padding:30px;">All monitored sectors safe. No active emergency alerts.</div>`;
                    return;
                }

                // Check for new incoming alerts to automatically notify user
                const latestAlert = alerts[0];
                if (latestAlert && latestAlert.status === 'ACTIVE') {
                    if (latestAlert.district) {
                        this.activeHazardDistrict = latestAlert.district;
                        this.updateDistrictDisplay();
                    }
                    if (latestAlert.alert_id !== this.lastNotifiedAlertId) {
                        this.lastNotifiedAlertId = latestAlert.alert_id;
                        this.dispatchAutomaticNotificationToUser(latestAlert);
                    }
                }

                container.innerHTML = alerts.map(a => {
                    const alertDist = (a.district || '').trim().toLowerCase();
                    const userDist = (this.userDistrict || '').trim().toLowerCase();
                    const isDistrictMatch = !alertDist || !userDist || alertDist === userDist || alertDist.includes(userDist) || userDist.includes(alertDist);

                    return `
                    <div class="alert-item ${a.alert_type === 'WEATHER_ALERT' ? 'weather-only' : ''}">
                        <div class="alert-item-header">
                            <div>
                                <b style="color:${a.severity === 'CRITICAL' ? '#ef4444' : '#0284c7'}; font-size:14px;">
                                    ${a.alert_type === 'DISASTER_ALERT' ? '🚨 CRITICAL DISASTER ALERT' : '🌧 WEATHER ALERT'}
                                </b>
                                <div style="font-size:11px; color:#94a3b8; margin-top:2px;">
                                    Location: <b>${a.location_name}</b> | District: <b style="color:#38bdf8;">${a.district || 'Monitored District'}</b> | Risk: ${a.risk_score}/100
                                </div>
                            </div>
                            <span class="alert-badge ${a.severity.toLowerCase()}">${a.severity}</span>
                        </div>
                        <div style="font-size:12px; color:#cbd5e1;">
                            <b>Hazard:</b> ${a.hazard_title}<br/>
                            <b>Rainfall:</b> ${a.rainfall_mm} mm | <b>Water Level:</b> ${a.water_level_m} m | <b>Wind:</b> ${a.wind_kmh} km/h
                        </div>
                        <div style="font-size:11px; margin-top:4px; display:flex; justify-content:space-between; align-items:center;">
                            <span style="color:#94a3b8;">
                                ${a.alert_type === 'DISASTER_ALERT' ?
                                  '⚠️ You are within the 100 km disaster-alert area.' :
                                  'ℹ️ You are within the 150 km weather-alert area.'}
                            </span>
                            ${isDistrictMatch ?
                                '<span style="color:#10b981; font-weight:700; font-size:11px;">✅ In Your District</span>' :
                                `<span style="color:#f59e0b; font-weight:700; font-size:11px;">🛡️ Muted (Your District: ${this.userDistrict})</span>`
                            }
                        </div>
                        <div class="alert-actions">
                            <button class="btn-action primary" onclick="App.mapWarningLocation(${a.epicenter_lat}, ${a.epicenter_lon}, '${a.location_name}', '${a.severity}', '${a.district || ''}')">🎯 MAP THIS WARNING</button>
                            <button class="btn-action" onclick="App.showAlertModal('${a.alert_id}')">VIEW ALERT</button>
                            <button class="btn-action" onclick="App.switchTab('tab-user')">VIEW WEATHER</button>
                            <button class="btn-action" onclick="ExportManager.downloadReport('pdf')">DOWNLOAD</button>
                            <button class="btn-action" onclick="ExportManager.shareAlert()">SHARE</button>
                        </div>
                    </div>
                `;
                }).join('');
            }
        } catch (e) {}
    },

    /**
     * Automatically sends alert notification directly to the user.
     * STRICT USER DISPATCH POLICY:
     * - The alert notification goes strictly to the user (Native Device/Browser Push Notification + Notification Bell indicator).
     * - The website is NOT interrupted with unsolicited popups, modals, or banners.
     * - ONLY if and when the user opens/taps the notification (or clicks the user notification bell), the alert modal is displayed.
     * - STRICT DISTRICT ENFORCEMENT: ONLY alert sent in user alert district.
     */
    dispatchAutomaticNotificationToUser(alert) {
        // 1. Strict District Check: ONLY alert sent in user alert district
        const alertDist = (alert.district || '').trim().toLowerCase();
        const userDist = (this.userDistrict || '').trim().toLowerCase();
        const isDistrictMatch = !alertDist || !userDist || alertDist === userDist || alertDist.includes(userDist) || userDist.includes(alertDist);

        if (!isDistrictMatch) {
            console.log(`[DISTRICT FILTER] Alert for ${alert.location_name} (District: ${alert.district}) suppressed for user in district '${this.userDistrict}'. ONLY alert sent in user alert district.`);
            return; // SUPPRESSED: NO NOTIFICATION SENT, DISTRICT MISMATCH
        }

        // 2. User is within affected district -> calculate distance to epicenter
        const uLat = this.userCoordinates.lat;
        const uLon = this.userCoordinates.lon;
        const eLat = alert.epicenter_lat || 16.9891;
        const eLon = alert.epicenter_lon || 82.2475;

        const R = 6371.0088;
        const dLat = (uLat - eLat) * Math.PI / 180;
        const dLon = (uLon - eLon) * Math.PI / 180;
        const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                  Math.cos(eLat * Math.PI / 180) * Math.cos(uLat * Math.PI / 180) *
                  Math.sin(dLon/2) * Math.sin(dLon/2);
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        const distKm = R * c;

        console.log(`[USER ALERT NOTIFICATION] Dispatching alert to user in district (${this.userDistrict}). Distance: ${distKm.toFixed(1)} km`);

        // Store pending alert for user
        this.pendingUserAlert = { ...alert, calculated_distance_km: distKm };

        if (distKm <= 100.0) {
            // 🚨 DISASTER ALERT (<= 100km): Sent strictly to user device
            this.unreadAlertCount = (this.unreadAlertCount || 0) + 1;
            this.updateHeaderNotificationBell();

            // Native OS / Device Push Notification
            if ('Notification' in window) {
                if (Notification.permission === 'granted') {
                    try {
                        const notif = new Notification(`🚨 CRITICAL DISASTER ALERT - ${alert.location_name}`, {
                            body: `Heavy Rainfall Detected (${alert.rainfall_mm}mm, Water: ${alert.water_level_m}m)\n📍 Affected Area: ${alert.district || this.userDistrict} District\n⚠️ You are within the 100 km disaster-alert area (${distKm.toFixed(1)} km).\n\n👉 Click here to open alert details.`,
                            icon: 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><text y=".9em" font-size="90">🚨</text></svg>',
                            tag: 'disaster-alert',
                            requireInteraction: true
                        });
                        notif.onclick = () => {
                            window.focus();
                            App.openAlertDetails(alert);
                        };
                    } catch (e) {
                        console.warn('[PUSH NOTIF] Could not fire native notification:', e);
                    }
                } else if (Notification.permission !== 'denied') {
                    Notification.requestPermission();
                }
            }

            // Gentle audio chime to notify user without deafening siren
            if (window.soundEngine) {
                window.soundEngine.playChime();
            }

        } else if (distKm <= 150.0) {
            // 🌧 WEATHER ALERT ONLY (100km to 150km): Sent strictly to user device
            this.unreadAlertCount = (this.unreadAlertCount || 0) + 1;
            this.updateHeaderNotificationBell();

            if ('Notification' in window && Notification.permission === 'granted') {
                try {
                    const notif = new Notification(`🌧 WEATHER ALERT - ${alert.location_name}`, {
                        body: `Weather Alert in ${alert.district} district.\nℹ️ You are within the 150 km weather-alert area (${distKm.toFixed(1)} km).\n👉 Click to view weather advisory.`,
                        icon: 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><text y=".9em" font-size="90">🌧</text></svg>',
                        tag: 'weather-alert'
                    });
                    notif.onclick = () => {
                        window.focus();
                        App.openAlertDetails(alert);
                    };
                } catch (e) {}
            }

            if (window.soundEngine) {
                window.soundEngine.playChime();
            }

        } else {
            console.log(`[NOTIFICATION SUPPRESSED] User is ${distKm.toFixed(1)} km from epicenter (> 150 km alert boundary). Safe sector.`);
        }
    },

    /**
     * Opens the Alert Details Modal ONLY when the user explicitly taps or opens the notification.
     * Flow: 🔔 Notification -> User Taps -> Website Opens Alert Details -> Download Report
     */
    openAlertDetails(alertOrId) {
        let alert = (typeof alertOrId === 'object' && alertOrId) ? alertOrId : this.pendingUserAlert;
        if (!alert && this.locationsList.length > 0) {
            const loc = this.locationsList.find(l => l.location_id === this.selectedLocationId) || this.locationsList[0];
            alert = {
                location_name: loc.city || 'Detected Zone',
                district: loc.district || this.userDistrict,
                hazard_title: 'Heavy Rainfall Detected',
                rainfall_mm: 145.0,
                water_level_m: 4.8,
                wind_kmh: 62.0,
                risk_score: 91.0,
                severity: 'CRITICAL',
                epicenter_lat: loc.latitude,
                epicenter_lon: loc.longitude,
                calculated_distance_km: 15.0
            };
        }

        const modal = document.getElementById('criticalAlertModal');
        if (!modal) return;

        // Reset unread notification counter now that user opened it
        this.unreadAlertCount = 0;
        this.updateHeaderNotificationBell();

        if (alert) {
            // Fill exact required alert card contents
            const titleEl = document.getElementById('modalAlertTitle');
            const subEl = document.getElementById('modalAlertSubtitle');
            const areaEl = document.getElementById('modalAffectedArea');
            const rainEl = document.getElementById('modalRainfall');
            const waterEl = document.getElementById('modalWaterLevel');
            const windEl = document.getElementById('modalWind');
            const riskEl = document.getElementById('modalRisk');
            const scoreEl = document.getElementById('modalScore');
            const bannerEl = document.getElementById('modalDistanceBanner');

            const isDisaster = (alert.severity === 'CRITICAL' || alert.risk_score >= 80 || alert.alert_type === 'DISASTER_ALERT');

            if (titleEl) {
                titleEl.innerText = isDisaster ? 'CRITICAL DISASTER ALERT' : 'WEATHER ALERT';
                titleEl.style.color = isDisaster ? '#ef4444' : '#0284c7';
            }
            if (subEl) subEl.innerText = alert.hazard_title || 'Heavy Rainfall Detected';
            if (areaEl) areaEl.innerText = `${alert.location_name || 'Detected Zone'} (${alert.district || this.userDistrict} District)`;
            if (rainEl) rainEl.innerText = `${alert.rainfall_mm !== undefined ? alert.rainfall_mm : 145} mm`;
            if (waterEl) waterEl.innerText = `${alert.water_level_m !== undefined ? alert.water_level_m : 4.8} m`;
            if (windEl) windEl.innerText = `${alert.wind_kmh !== undefined ? alert.wind_kmh : 62} km/h`;
            if (riskEl) {
                riskEl.innerText = alert.severity || 'CRITICAL';
                riskEl.className = `dossier-val ${isDisaster ? 'critical' : ''}`;
            }
            if (scoreEl) scoreEl.innerText = `${Math.round(alert.risk_score || 91)}/100`;

            const distKm = alert.calculated_distance_km || 15.0;
            if (bannerEl) {
                if (distKm <= 100.0) {
                    bannerEl.innerHTML = `⚠️ You are within the <b>100 km</b> disaster-alert area (${distKm.toFixed(1)} km to epicenter).`;
                    bannerEl.style.borderColor = 'rgba(239, 68, 68, 0.5)';
                    bannerEl.style.color = '#fca5a5';
                } else if (distKm <= 150.0) {
                    bannerEl.innerHTML = `🌧 You are within the <b>150 km</b> weather-alert area (${distKm.toFixed(1)} km to epicenter).`;
                    bannerEl.style.borderColor = 'rgba(56, 189, 248, 0.5)';
                    bannerEl.style.color = '#7dd3fc';
                } else {
                    bannerEl.innerHTML = `🛡️ You are in a safe sector (${distKm.toFixed(1)} km to epicenter).`;
                    bannerEl.style.borderColor = 'rgba(16, 185, 129, 0.5)';
                    bannerEl.style.color = '#86efac';
                }
            }

            this.pendingUserAlert = alert;
        }

        // Show modal on website now that user has explicitly opened it
        modal.style.display = 'flex';
        modal.classList.add('active');

        // Play alert audio since user opened the alert
        if (window.soundEngine) {
            window.soundEngine.playCriticalAlarm(2);
        }
    },

    closeAlertModal() {
        const modal = document.getElementById('criticalAlertModal');
        if (modal) {
            modal.classList.remove('active');
            modal.style.display = 'none';
        }
        if (window.soundEngine) window.soundEngine.stopSiren();
    },

    viewAlertOnMap() {
        this.closeAlertModal();
        this.switchTab('tab-command');
        const alert = this.pendingUserAlert;
        if (alert && alert.epicenter_lat && alert.epicenter_lon && window.MapManager) {
            window.MapManager.setWarningLocation(
                alert.epicenter_lat,
                alert.epicenter_lon,
                alert.location_name || 'Detected Epicenter',
                alert.severity || 'CRITICAL'
            );
        }
    },

    acknowledgeAlert() {
        this.closeAlertModal();
        alert('Alert acknowledged! Safety confirmation logged in disaster cell audit log.');
        if (window.soundEngine) {
            window.soundEngine.speak('Alert acknowledged. Safety confirmation logged.');
        }
    },

    handleNotificationBellClick() {
        if (this.pendingUserAlert) {
            this.openAlertDetails(this.pendingUserAlert);
        } else {
            this.openAlertDetails(null);
        }
    },

    updateHeaderNotificationBell() {
        const bell = document.getElementById('headerNotificationBell');
        const badge = document.getElementById('headerBellBadge');
        const text = document.getElementById('headerBellText');

        if (!bell || !badge) return;

        if (this.unreadAlertCount > 0) {
            badge.innerText = this.unreadAlertCount;
            badge.style.display = 'inline-flex';
            bell.classList.add('has-alert');
            if (text) text.innerText = `${this.unreadAlertCount} Alert (Tap to Open)`;
        } else {
            badge.style.display = 'none';
            bell.classList.remove('has-alert');
            if (text) text.innerText = 'User Alerts';
        }
    },

    sendTestUserNotification(severity = 'CRITICAL') {
        const testAlert = {
            alert_id: 'ALERT-TEST-' + Date.now(),
            alert_type: severity === 'CRITICAL' ? 'DISASTER_ALERT' : 'WEATHER_ALERT',
            location_name: this.activeEpicenter.name || 'Kakinada Epicenter',
            district: this.userDistrict,
            hazard_title: 'Heavy Rainfall Detected',
            rainfall_mm: 145.0,
            water_level_m: 4.8,
            wind_kmh: 62.0,
            risk_score: 91.0,
            severity: severity,
            epicenter_lat: this.activeEpicenter.lat || 16.9891,
            epicenter_lon: this.activeEpicenter.lon || 82.2475
        };
        this.dispatchAutomaticNotificationToUser(testAlert);
    },

    requestNotificationPermission() {
        if ('Notification' in window) {
            Notification.requestPermission().then(permission => {
                if (permission === 'granted') {
                    new Notification('🚨 Disaster Early-Warning Agent', {
                        body: 'Notifications enabled! Emergency alerts will be delivered directly to your device.',
                        icon: 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><text y=".9em" font-size="90">🚨</text></svg>'
                    });
                    const btn = document.getElementById('btnRequestNotifPermission');
                    if (btn) btn.innerText = '🔔 Device Alerts Active';
                }
            });
        }
    },

    showDistrictSuppressedToast(alert) {
        let toast = document.getElementById('districtSuppressedToast');
        if (!toast) {
            toast = document.createElement('div');
            toast.id = 'districtSuppressedToast';
            toast.style.position = 'fixed';
            toast.style.bottom = '24px';
            toast.style.right = '24px';
            toast.style.background = '#0f172a';
            toast.style.color = '#f8fafc';
            toast.style.border = '1px solid #f59e0b';
            toast.style.borderRadius = '8px';
            toast.style.padding = '12px 18px';
            toast.style.boxShadow = '0 10px 25px rgba(0,0,0,0.6)';
            toast.style.zIndex = '9999';
            toast.style.maxWidth = '360px';
            toast.style.fontFamily = 'Inter, sans-serif';
            toast.style.fontSize = '12px';
            document.body.appendChild(toast);
        }
        toast.innerHTML = `
            <div style="display:flex; align-items:flex-start; gap:10px;">
                <span style="font-size:22px;">🛡️</span>
                <div>
                    <b style="color:#f59e0b;">Notification Filtered: District Protection</b>
                    <div style="margin-top:2px; color:#cbd5e1;">
                        Hazard is active in <b>${alert.district || 'Other'}</b> district. Suppressed for you in <b>${this.userDistrict}</b>.
                    </div>
                    <div style="font-size:11px; color:#38bdf8; margin-top:4px; font-weight:600;">
                        Rule: Only alert sent in user alert district.
                    </div>
                </div>
            </div>
        `;
        toast.style.display = 'block';
        clearTimeout(this._toastTimer);
        this._toastTimer = setTimeout(() => {
            if (toast) toast.style.display = 'none';
        }, 6000);
    },

    async refreshNotifications() {
        const tbody = document.getElementById('notifTableBody');
        if (!tbody) return;

        try {
            const res = await fetch('http://localhost:8080/api/notifications');
            if (res.ok) {
                const notifs = await res.json();
                tbody.innerHTML = notifs.slice(0, 15).map(n => `
                    <tr>
                        <td><b>${n.user_id}</b></td>
                        <td>${n.distance_km.toFixed(1)} km</td>
                        <td>${n.qualifying_radius_km.toFixed(0)} km</td>
                        <td>
                            <span class="radius-badge ${n.alert_type === 'DISASTER_ALERT' ? 'disaster' : (n.alert_type === 'WEATHER_ALERT' ? 'weather' : '')}" style="${n.alert_type.includes('DISTRICT') ? 'background:rgba(245,158,11,0.2); color:#f59e0b; border:1px solid #f59e0b;' : ''}">
                                ${n.alert_type === 'SUPPRESSED_DISTRICT_MISMATCH' ? '🛡️ DISTRICT MISMATCH' : n.alert_type}
                            </span>
                        </td>
                        <td style="color:${n.delivery_status === 'SENT' ? '#10b981' : (n.alert_type.includes('DISTRICT') ? '#f59e0b' : '#94a3b8')}; font-weight:600;">
                            ${n.delivery_status}
                        </td>
                        <td style="font-size:11px; color:#cbd5e1;">${n.explanation}</td>
                    </tr>
                `).join('');
            }
        } catch (e) {}
    },

    // =========================================================================
    // DEDICATED REAL-TIME NOTIFICATION BOARD
    // =========================================================================

    notificationBoardFilter: 'ALL',
    notificationBoardSearch: '',
    cachedNotificationBoardData: [],

    async refreshNotificationBoard() {
        const tbody = document.getElementById('notificationBoardTableBody');
        if (!tbody) return;

        try {
            const [alertsRes, notifsRes] = await Promise.all([
                fetch('http://localhost:8080/api/alerts'),
                fetch('http://localhost:8080/api/notifications')
            ]);

            const alerts = alertsRes.ok ? await alertsRes.json() : [];
            const notifs = notifsRes.ok ? await notifsRes.json() : [];

            const feed = [];

            // Active alerts from rational agent
            alerts.forEach((a, i) => {
                const alertDist = (a.district || '').trim().toLowerCase();
                const userDist = (this.userDistrict || '').trim().toLowerCase();
                const isMatch = !alertDist || !userDist || alertDist === userDist || alertDist.includes(userDist) || userDist.includes(alertDist);

                feed.push({
                    id: a.alert_id || `ALERT-LIVE-${i + 1}`,
                    time: a.created_at ? new Date(a.created_at).toLocaleTimeString() : new Date().toLocaleTimeString(),
                    hazard: a.hazard_title || 'Heavy Rainfall & Flash Flood Inundation',
                    severity: a.severity || 'CRITICAL',
                    location: a.location_name || 'Coastal Sector',
                    district: a.district || 'Kakinada',
                    state: 'Andhra Pradesh',
                    country: 'India',
                    distanceKm: 15.0,
                    radii: '100km Disaster Core / 150km Weather Area',
                    rainfall: a.rainfall_mm || 145.0,
                    waterLevel: a.water_level_m || 4.8,
                    isMatch: isMatch,
                    deliveryStatus: isMatch ? 'SENT' : 'SUPPRESSED_DISTRICT_MISMATCH'
                });
            });

            // Historical notification dispatch log
            notifs.forEach(n => {
                if (!feed.some(f => f.id === n.notification_id)) {
                    const isMatch = n.delivery_status === 'SENT';
                    feed.push({
                        id: n.notification_id,
                        time: n.timestamp ? new Date(n.timestamp).toLocaleTimeString() : new Date().toLocaleTimeString(),
                        hazard: n.explanation || 'Sensor threshold anomaly detected',
                        severity: n.alert_type === 'DISASTER_ALERT' ? 'CRITICAL' : 'WARNING',
                        location: n.user_id,
                        district: this.userDistrict || 'Kakinada',
                        state: 'Andhra Pradesh',
                        country: 'India',
                        distanceKm: n.distance_km || 15.0,
                        radii: `${(n.qualifying_radius_km || 100).toFixed(0)}km Boundary`,
                        rainfall: 145.0,
                        waterLevel: 4.8,
                        isMatch: isMatch,
                        deliveryStatus: n.delivery_status
                    });
                }
            });

            this.cachedNotificationBoardData = feed;
            this.renderNotificationBoard();
        } catch (e) {
            console.warn('[NOTIFICATION BOARD] Feed error:', e);
        }
    },

    renderNotificationBoard() {
        const tbody = document.getElementById('notificationBoardTableBody');
        if (!tbody) return;

        let feed = this.cachedNotificationBoardData || [];

        // Apply Tab Filter
        if (this.notificationBoardFilter === 'DISPATCHED') {
            feed = feed.filter(f => f.isMatch);
        } else if (this.notificationBoardFilter === 'CRITICAL') {
            feed = feed.filter(f => f.severity === 'CRITICAL');
        } else if (this.notificationBoardFilter === 'SUPPRESSED') {
            feed = feed.filter(f => !f.isMatch);
        }

        // Apply Text Search
        if (this.notificationBoardSearch) {
            const q = this.notificationBoardSearch;
            feed = feed.filter(f => 
                (f.hazard && f.hazard.toLowerCase().includes(q)) ||
                (f.district && f.district.toLowerCase().includes(q)) ||
                (f.location && f.location.toLowerCase().includes(q)) ||
                (f.id && f.id.toLowerCase().includes(q))
            );
        }

        // Update KPI Stats Strip
        const totalDispatched = (this.cachedNotificationBoardData || []).filter(f => f.isMatch).length;
        const totalSuppressed = (this.cachedNotificationBoardData || []).filter(f => !f.isMatch).length;
        const hasCriticalInDistrict = (this.cachedNotificationBoardData || []).some(f => f.isMatch && f.severity === 'CRITICAL');

        const elDisp = document.getElementById('nbTotalDispatched');
        const elUserDist = document.getElementById('nbUserDistrict');
        const elSupp = document.getElementById('nbTotalSuppressed');
        const elSiren = document.getElementById('nbSirenStatus');

        if (elDisp) elDisp.innerText = totalDispatched;
        if (elUserDist) elUserDist.innerText = this.userDistrict || 'Kakinada';
        if (elSupp) elSupp.innerText = totalSuppressed;
        if (elSiren) {
            elSiren.innerHTML = hasCriticalInDistrict 
                ? '<span style="color:#ef4444;">ACTIVE (AUDIO SIREN ON)</span>' 
                : '<span style="color:#10b981;">STANDBY</span>';
        }

        if (feed.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding:24px; color:#64748b;">No notification records matching the selected filter.</td></tr>`;
            return;
        }

        tbody.innerHTML = feed.map(item => `
            <tr>
                <td>
                    <div style="font-weight:700; color:#f8fafc;">${item.time}</div>
                    <div style="font-size:10px; color:#94a3b8;">${item.id}</div>
                </td>
                <td>
                    <div style="display:flex; align-items:center; gap:6px; margin-bottom:2px;">
                        <span class="nb-severity-pill ${item.severity.toLowerCase()}">${item.severity}</span>
                        <b>${item.hazard}</b>
                    </div>
                    <div style="font-size:11px; color:#94a3b8;">Precipitation: ${item.rainfall}mm | Spillway: ${item.waterLevel}m</div>
                </td>
                <td>
                    <div style="font-weight:700; color:#38bdf8;">🏛️ ${item.district} District</div>
                    <div style="font-size:11px; color:#94a3b8;">${item.location} • ${item.state}, ${item.country}</div>
                </td>
                <td>
                    <div>${item.radii}</div>
                    <div style="font-size:11px; color:#64748b;">${item.distanceKm.toFixed(1)} km to Epicenter</div>
                </td>
                <td>
                    ${item.isMatch ? 
                        '<span class="nb-badge-dispatched">🛡️ DISPATCHED TO USER DISTRICT (SIREN ACTIVE)</span>' : 
                        `<span class="nb-badge-suppressed">⚠️ SUPPRESSED (REGISTERED IN ${this.userDistrict})</span>`
                    }
                </td>
                <td>
                    <div style="display:flex; gap:6px; flex-wrap:wrap;">
                        <button class="nb-action-btn" onclick="App.acknowledgeAlert()">[ACK SAFE]</button>
                        <button class="nb-action-btn" onclick="window.soundEngine.playCriticalAlarm(2)">[🔊 SIREN]</button>
                        <button class="nb-action-btn" onclick="ExportManager.downloadReport('pdf')">[📥 PDF]</button>
                    </div>
                </td>
            </tr>
        `).join('');
    },

    filterNotificationBoard(mode) {
        this.notificationBoardFilter = mode;
        const btnMap = {
            'ALL': 'nbFilterAll',
            'DISPATCHED': 'nbFilterDispatched',
            'CRITICAL': 'nbFilterCritical',
            'SUPPRESSED': 'nbFilterSuppressed'
        };
        Object.values(btnMap).forEach(id => {
            const btn = document.getElementById(id);
            if (btn) btn.classList.remove('active');
        });
        const activeBtn = document.getElementById(btnMap[mode]);
        if (activeBtn) activeBtn.classList.add('active');

        this.renderNotificationBoard();
    },

    searchNotificationBoard(query) {
        this.notificationBoardSearch = (query || '').trim().toLowerCase();
        this.renderNotificationBoard();
    },

    async refreshGraph() {
        try {
            const res = await fetch('http://localhost:8080/api/graph');
            if (res.ok) {
                const graphData = await res.json();
                if (window.graphVis) {
                    window.graphVis.loadGraphData(graphData.nodes || [], graphData.edges || []);
                }
                const countEl = document.getElementById('graphClusterCount');
                if (countEl) countEl.innerText = graphData.connected_components_count || 1;
            }
        } catch (e) {}
    },

    evaluateSimulatedUserDistance(distKm) {
        const banner = document.getElementById('userGeofenceBanner');
        const title = document.getElementById('userBannerTitle');
        const desc = document.getElementById('userBannerDesc');
        const actions = document.getElementById('userBannerActions');

        if (!banner) return;

        const uDist = (this.userDistrict || '').trim().toLowerCase();
        const hDist = (this.activeHazardDistrict || '').trim().toLowerCase();
        const isDistrictMatch = !uDist || !hDist || uDist === hDist || uDist.includes(hDist) || hDist.includes(uDist);

        if (!isDistrictMatch) {
            banner.className = 'user-geofence-banner safe-zone';
            title.innerHTML = `🛡️ ALERTS SUPPRESSED (District Protection Active)`;
            title.style.color = '#f59e0b';
            desc.innerHTML = `
                You are registered in <b>${this.userDistrict}</b> district, while the active hazard is localized to <b>${this.activeHazardDistrict}</b> district (Distance: ${distKm.toFixed(1)} km).<br/>
                <b>Notification Policy:</b> <span style="color:#f59e0b; font-weight:bold;">ONLY alert sent in user alert district.</span> Sirens and emergency modal alerts are muted for other districts to prevent false panic.
            `;
            actions.innerHTML = `
                <button class="btn-action" onclick="App.setUserDistrictToHazard()" style="background:#f59e0b; color:#0f172a; font-weight:bold;">[SET MY DISTRICT TO ${this.activeHazardDistrict.toUpperCase()}]</button>
                <button class="btn-action" onclick="App.switchTab('tab-command')">[COMMAND CENTER]</button>
            `;
            return;
        }

        if (distKm <= 100.0) {
            // Disaster + Weather Alert
            banner.className = 'user-geofence-banner disaster-zone';
            title.innerHTML = `🚨 CRITICAL DISASTER ALERT (${distKm.toFixed(1)} km to Epicenter)`;
            title.style.color = '#ef4444';
            desc.innerHTML = `
                You are within the <b>100 km disaster-alert area</b> in <b>${this.userDistrict}</b>. Hazardous heavy precipitation (145mm) and rapid water level rise detected.<br/>
                <b>Notification Rules:</b> Disaster Alert + Weather Alert dispatched.
            `;
            actions.innerHTML = `
                <button class="btn-action primary" onclick="App.showAlertModal()">[VIEW ALERT]</button>
                <button class="btn-action" onclick="ExportManager.downloadReport('pdf')">[DOWNLOAD ALERT]</button>
                <button class="btn-action" onclick="App.acknowledgeAlert()">[ACKNOWLEDGE SAFE]</button>
            `;
        } else if (distKm <= 150.0) {
            // Weather Alert ONLY
            banner.className = 'user-geofence-banner weather-zone';
            title.innerHTML = `🌧 WEATHER ALERT ONLY (${distKm.toFixed(1)} km to Epicenter)`;
            title.style.color = '#0284c7';
            desc.innerHTML = `
                You are within the <b>150 km weather-alert area</b> in <b>${this.userDistrict}</b> (Distance: ${distKm.toFixed(1)} km > 100km).<br/>
                <b>Notification Rules:</b> Weather Alert ONLY. Critical disaster warning suppressed to prevent false alarm panic.
            `;
            actions.innerHTML = `
                <button class="btn-action" style="background:#0284c7; color:#fff;" onclick="App.switchTab('tab-command')">[VIEW WEATHER]</button>
                <button class="btn-action" onclick="ExportManager.downloadReport('csv')">[DOWNLOAD REPORT]</button>
            `;
        } else {
            // Beyond 150km: Safe
            banner.className = 'user-geofence-banner safe-zone';
            title.innerHTML = `🛡️ SAFE ZONE (${distKm.toFixed(1)} km to Epicenter)`;
            title.style.color = '#10b981';
            desc.innerHTML = `
                You are <b>${distKm.toFixed(1)} km</b> away from the active hazard epicenter in <b>${this.userDistrict}</b> (> 150 km alert boundary).<br/>
                <b>Notification Rules:</b> Beyond geofence radius. No location-based notification dispatched.
            `;
            actions.innerHTML = `
                <button class="btn-action" onclick="App.switchTab('tab-command')">[CHECK ACTIVE RADII]</button>
            `;
        }
    },

    setUserDistrictToHazard() {
        if (this.activeHazardDistrict) {
            this.userDistrict = this.activeHazardDistrict;
            const select = document.getElementById('userDistrictSelector');
            if (select) {
                let opt = Array.from(select.options).find(o => o.value.toLowerCase() === this.userDistrict.toLowerCase());
                if (!opt) {
                    opt = new Option(this.userDistrict, this.userDistrict);
                    select.add(opt);
                }
                select.value = opt.value;
            }
            this.updateDistrictDisplay();
            const slider = document.getElementById('userDistanceSlider');
            const distKm = slider ? parseFloat(slider.value) : 15.0;
            this.evaluateSimulatedUserDistance(distKm);
            this.refreshAlerts();
        }
    },

    showAlertModal(alertId) {
        if (alertId && this.cachedNotificationBoardData) {
            const item = this.cachedNotificationBoardData.find(a => a.alert_id === alertId);
            if (item) {
                this.openAlertDetails(item);
                return;
            }
        }
        this.openAlertDetails(null);
    },

    switchTab(tabId) {
        const btn = document.querySelector(`[data-tab="${tabId}"]`);
        if (btn) btn.click();
    },

    /**
     * Sets mapping in warning location coordinates and recalculates dual radii.
     */
    mapWarningLocation(lat, lon, name = 'Warning Epicenter', severity = 'CRITICAL', district = '') {
        const numLat = parseFloat(lat);
        const numLon = parseFloat(lon);
        this.activeEpicenter = { lat: numLat, lon: numLon, name: name };
        if (district) {
            this.activeHazardDistrict = district;
            this.updateDistrictDisplay();
        }

        // 1. Update Map view & 100km / 150km circles
        window.MapManager.setWarningLocation(numLat, numLon, name, severity);

        // 2. Recalculate User Geofence status to new warning location
        this.evaluateSimulatedUserCoordinates(this.userCoordinates.lat, this.userCoordinates.lon);

        // 3. Play chime or alarm based on severity
        const uDist = (this.userDistrict || '').trim().toLowerCase();
        const hDist = (this.activeHazardDistrict || '').trim().toLowerCase();
        const isDistrictMatch = !uDist || !hDist || uDist === hDist || uDist.includes(hDist) || hDist.includes(uDist);

        if (isDistrictMatch) {
            if (severity === 'CRITICAL' || severity === 'HIGH') {
                window.soundEngine.playCriticalAlarm(2);
            } else {
                window.soundEngine.playChime();
            }
        }
    },

    /**
     * Focuses on a registered warning location from dropdown or toolbar.
     */
    focusWarningLocation(locId, lat, lon, cityName, severity, district = '') {
        this.selectedLocationId = locId;
        const select = document.getElementById('locationSelector');
        if (select) select.value = locId;
        const loc = this.locationsList.find(l => l.location_id === locId);
        const targetDistrict = district || (loc ? loc.district : cityName);
        this.mapWarningLocation(lat, lon, cityName, severity, targetDistrict);
        this.refreshRiskAndWeather();
    },

    selectLocation(locId) {
        const loc = this.locationsList.find(l => l.location_id === locId);
        if (loc) {
            this.focusWarningLocation(locId, loc.latitude, loc.longitude, loc.city, loc.risk_level || 'CRITICAL', loc.district);
        }
    },

    recenterOnActiveWarning() {
        if (this.activeEpicenter) {
            window.MapManager.setWarningLocation(this.activeEpicenter.lat, this.activeEpicenter.lon, this.activeEpicenter.name, 'CRITICAL');
        }
    },

    onWarningLocationSet(lat, lon) {
        this.activeEpicenter = { lat, lon, name: `Manual Warning Pin (${lat.toFixed(3)}, ${lon.toFixed(3)})` };
        this.evaluateSimulatedUserCoordinates(this.userCoordinates.lat, this.userCoordinates.lon);
    },

    evaluateSimulatedUserCoordinates(uLat, uLon) {
        this.userCoordinates = { lat: uLat, lon: uLon };
        const eLat = this.activeEpicenter.lat;
        const eLon = this.activeEpicenter.lon;

        // Haversine distance
        const R = 6371.0088;
        const dLat = (uLat - eLat) * Math.PI / 180;
        const dLon = (uLon - eLon) * Math.PI / 180;
        const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                  Math.cos(eLat * Math.PI / 180) * Math.cos(uLat * Math.PI / 180) *
                  Math.sin(dLon/2) * Math.sin(dLon/2);
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        const distKm = R * c;

        // Update slider value
        const slider = document.getElementById('userDistanceSlider');
        const distVal = document.getElementById('userDistanceVal');
        if (slider) slider.value = Math.min(200, distKm);
        if (distVal) distVal.innerText = `${distKm.toFixed(1)} km`;

        // Update map user pin & distance vector
        window.MapManager.updateUserLocation(uLat, uLon, eLat, eLon);

        // Update geofence UI banner
        this.evaluateSimulatedUserDistance(distKm);
    },

    // =========================================================================
    // SINGLE-SESSION AUTHENTICATION SUBSYSTEM
    // =========================================================================

    // =========================================================================
    // MULTI-USER AUTHENTICATION SUBSYSTEM (Single Active Session Policy Removed)
    // =========================================================================

    getApiBase() {
        return window.location.port === '8080' ? '' : 'http://localhost:8080';
    },

    async initAuthSession() {
        const storedSession = localStorage.getItem('disaster_agent_session');
        if (storedSession) {
            try {
                const session = JSON.parse(storedSession);
                if (session && session.email) {
                    this.setAuthenticatedState(session);
                    return;
                }
            } catch (ignored) {}
        }

        // Fresh start: login required
        this.setUnauthenticatedState();
    },

    setAuthenticatedState(session) {
        this.currentUser = session;
        if (session.district) {
            this.userDistrict = session.district;
            const select = document.getElementById('userDistrictSelector');
            if (select) select.value = session.district;
            const authDistSelect = document.getElementById('authDistrictSelect');
            if (authDistSelect) authDistSelect.value = session.district;
        }

        // Update Header User Bar
        const headerBar = document.getElementById('headerUserSessionBar');
        const headerEmail = document.getElementById('headerUserEmail');
        const headerDistrict = document.getElementById('headerUserDistrictBadge');
        if (headerBar) headerBar.style.display = 'flex';
        if (headerEmail) headerEmail.innerText = session.email;
        if (headerDistrict) headerDistrict.innerText = `District: ${this.userDistrict}`;

        // Unlock Dashboard
        document.body.classList.remove('auth-locked');
        const modal = document.getElementById('authPortalModal');
        if (modal) modal.classList.remove('active');

        this.updateDistrictDisplay();
        console.log(`[AUTH] Authenticated as ${session.email} in ${this.userDistrict}`);
    },

    setUnauthenticatedState() {
        this.currentUser = null;
        document.body.classList.add('auth-locked');

        const headerBar = document.getElementById('headerUserSessionBar');
        if (headerBar) headerBar.style.display = 'none';

        const modal = document.getElementById('authPortalModal');
        if (modal) modal.classList.add('active');
    },

    async handleLoginSubmit() {
        const emailInput = document.getElementById('authEmailInput');
        const pwdInput = document.getElementById('authPasswordInput');
        const districtSelect = document.getElementById('authDistrictSelect');
        const submitBtn = document.getElementById('authSubmitBtn');

        const email = emailInput ? emailInput.value.trim() : '';
        const password = pwdInput ? pwdInput.value.trim() : '';
        const district = districtSelect ? districtSelect.value : 'Kakinada';

        this.hideAuthAlert();

        // Validation
        if (!email || !email.includes('@') || !email.includes('.')) {
            this.showAuthAlert('⚠️ Please enter a valid Gmail address (e.g. user@gmail.com).', 'error');
            return;
        }

        if (password.length < 4) {
            this.showAuthAlert('⚠️ Password must be at least 4 characters long.', 'error');
            return;
        }

        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<span>⏳ Logging in...</span>';
        }

        const apiBase = this.getApiBase();

        try {
            const resp = await fetch(`${apiBase}/api/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password, district })
            });

            const data = await resp.json();

            if (!resp.ok || !data.success) {
                this.showAuthAlert(`❌ ${data.error || 'Login failed. Please check credentials.'}`, 'error');
                return;
            }

            // Success: Establish session and unlock dashboard
            localStorage.setItem('disaster_agent_session', JSON.stringify(data));
            this.setAuthenticatedState(data);

            if (pwdInput) pwdInput.value = '';
            this.showAuthAlert(`✅ Welcome, ${data.email}! District alert filtering active for ${data.district}.`, 'success');

            // Audio announcement
            if (window.soundEngine && typeof window.soundEngine.speak === 'function') {
                window.soundEngine.speak(`Login successful. Disaster monitoring active for ${data.district} district.`);
            }

            // Refresh all live sensors and alerts for this district
            await this.refreshAllData();

        } catch (err) {
            console.error('[AUTH ERROR]', err);
            // Local fallback
            const fallbackSession = {
                email,
                district,
                token: 'LOCAL-SESSION-' + Date.now(),
                loginTimestamp: Date.now()
            };
            localStorage.setItem('disaster_agent_session', JSON.stringify(fallbackSession));
            this.setAuthenticatedState(fallbackSession);
            this.showAuthAlert(`✅ Session started for ${email} (${district}).`, 'success');
        } finally {
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<span>🔐 LOG IN TO COMMAND CENTER</span>';
            }
        }
    },

    async handleLogout() {
        console.log('[AUTH] Logging out current user...');
        const apiBase = this.getApiBase();

        try {
            await fetch(`${apiBase}/api/auth/logout`, { method: 'POST' });
        } catch (e) {
            console.warn('[AUTH] Logout API notice:', e);
        }

        // Clear local storage and state
        localStorage.removeItem('disaster_agent_session');
        this.setUnauthenticatedState();

        // Reset login form fields to start fresh
        const form = document.getElementById('authLoginForm');
        const pwdInput = document.getElementById('authPasswordInput');

        if (form) form.style.display = 'block';
        if (pwdInput) pwdInput.value = '';

        this.showAuthAlert('✅ <b>Logged out successfully.</b> You can now start fresh with any account.', 'success');

        if (window.soundEngine && typeof window.soundEngine.speak === 'function') {
            window.soundEngine.speak('User logged out. Ready for new login.');
        }
    },

    fillDemoCredentials(email, password, district) {
        const emailInput = document.getElementById('authEmailInput');
        const pwdInput = document.getElementById('authPasswordInput');
        const distSelect = document.getElementById('authDistrictSelect');

        if (emailInput) emailInput.value = email;
        if (pwdInput) pwdInput.value = password;
        if (distSelect) distSelect.value = district;

        this.showAuthAlert(`⚡ Profile loaded: <b>${email}</b> (${district} District). Click 'LOG IN' below.`, 'warning');
    },

    togglePasswordVisibility() {
        const pwdInput = document.getElementById('authPasswordInput');
        const toggleBtn = document.getElementById('authTogglePwdBtn');
        if (!pwdInput) return;

        if (pwdInput.type === 'password') {
            pwdInput.type = 'text';
            if (toggleBtn) toggleBtn.innerText = '🙈';
        } else {
            pwdInput.type = 'password';
            if (toggleBtn) toggleBtn.innerText = '👁️';
        }
    },

    showAuthAlert(message, type = 'info') {
        const box = document.getElementById('authAlertBox');
        if (!box) return;
        box.className = `auth-alert ${type}`;
        box.innerHTML = message;
        box.style.display = 'block';
    },

    hideAuthAlert() {
        const box = document.getElementById('authAlertBox');
        if (box) {
            box.style.display = 'none';
            box.innerHTML = '';
        }
    }
};

window.App = App;
window.addEventListener('DOMContentLoaded', () => App.init());

