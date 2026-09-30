/**
 * Location-Aware Geospatial Command Map & Google Maps Engine for Disaster Early-Warning Agent.
 * Renders:
 * - Google Maps Hybrid & Road Layers with Country, State, District, and City typography
 * - Dynamic Epicenter with Pulsing Hazard Ring
 * - EXACT 100 KM DISASTER ALERT RADIUS (Red Circle)
 * - EXACT 150 KM WEATHER ALERT RADIUS (Blue/Amber Circle)
 * - Prominent District & State Boundary Badges (Kakinada, Visakhapatnam, NTR, Hyderabad, Guntur, East Godavari)
 * - IoT Sensor Network Markers with Real-time Telemetry Popups
 * - Boundary Verification Markers (100km, 100.1km, 150km, 150.1km)
 * - Seamless 3D Photorealistic Interactive Earth Globe toggle
 */

const MapManager = {
    map: null,
    baseLayers: {},
    overlayLayers: {},
    layerControl: null,
    currentMapType: 'GOOGLE_HYBRID', // 'GLOBE', 'GOOGLE_HYBRID', 'GOOGLE_ROAD', 'GOOGLE_TERRAIN', 'SATELLITE', 'DARK'
    epicenterMarker: null,
    disasterCircle: null,
    weatherCircle: null,
    impactCoreCircle: null,
    sensorMarkersGroup: null,
    boundaryMarkersGroup: null,
    warningLocationsGroup: null,
    districtBadgesGroup: null,
    userMarker: null,
    distanceLine: null,
    clickMode: 'SET_WARNING', // 'SET_WARNING' or 'SET_USER'
    globeVisualizer: null,

    init() {
        if (typeof L === 'undefined') {
            console.warn('Leaflet not loaded yet');
            return;
        }

        const defaultLat = 16.9891; // Default Kakinada, Andhra Pradesh, India
        const defaultLon = 82.2475;

        const mapEl = document.getElementById('disasterMap');
        if (!mapEl) return;

        // Leaflet Map with strict world bounds and minimum zoom to prevent infinite horizontal repeating strips
        this.map = L.map('disasterMap', {
            center: [defaultLat, defaultLon],
            zoom: 8,
            minZoom: 3,
            maxZoom: 19,
            maxBounds: [[-85, -180], [85, 180]],
            maxBoundsViscosity: 1.0,
            worldCopyJump: false,
            zoomControl: true
        });

        // 1. Google Maps Satellite Hybrid (Satellite Imagery + Country, State, District, City, Roads Labels)
        this.baseLayers.googleHybrid = L.tileLayer('https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}', {
            attribution: '&copy; Google Maps (Satellite & Administrative Labels)',
            maxZoom: 20,
            noWrap: true
        });

        // 2. Google Maps Standard Road Map (With Country, State, District, Road Hierarchy)
        this.baseLayers.googleRoad = L.tileLayer('https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', {
            attribution: '&copy; Google Maps (Road & Administrative Boundaries)',
            maxZoom: 20,
            noWrap: true
        });

        // 3. Google Maps Terrain Map (Physical elevation + administrative borders & district names)
        this.baseLayers.googleTerrain = L.tileLayer('https://mt1.google.com/vt/lyrs=p&x={x}&y={y}&z={z}', {
            attribution: '&copy; Google Maps Terrain',
            maxZoom: 20,
            noWrap: true
        });

        // 4. Real-World Esri Satellite Imagery
        this.baseLayers.satellite = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
            attribution: '&copy; Esri World Imagery',
            maxZoom: 19,
            noWrap: true
        });

        // 5. Esri District Boundaries and Places Overlay
        this.baseLayers.satelliteLabels = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}', {
            attribution: 'Boundaries &copy; Esri',
            maxZoom: 19,
            noWrap: true
        });

        // 6. Tactical Dark Matter Map
        this.baseLayers.dark = L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
            attribution: '&copy; OpenStreetMap &copy; CARTO',
            maxZoom: 19,
            noWrap: true
        });

        // Activate Google Hybrid as default base layer (gives real satellite + country, state, district names)
        this.baseLayers.googleHybrid.addTo(this.map);

        this.sensorMarkersGroup = L.layerGroup().addTo(this.map);
        this.boundaryMarkersGroup = L.layerGroup().addTo(this.map);
        this.warningLocationsGroup = L.layerGroup().addTo(this.map);
        this.districtBadgesGroup = L.layerGroup().addTo(this.map);

        // Add Leaflet Layer Control in top-right
        const baseMaps = {
            "🛰️ Google Maps Hybrid": this.baseLayers.googleHybrid,
            "🗺️ Google Maps Standard": this.baseLayers.googleRoad,
            "🏔️ Google Maps Terrain": this.baseLayers.googleTerrain,
            "🌍 Esri Satellite": this.baseLayers.satellite,
            "🌙 Tactical Dark Map": this.baseLayers.dark
        };
        const overlayMaps = {
            "🏷️ Satellite Labels & Borders": this.baseLayers.satelliteLabels,
            "🏛️ District & State Borders": this.districtBadgesGroup,
            "📡 Sensor Network": this.sensorMarkersGroup,
            "📐 100km / 150km Dual Radii": this.boundaryMarkersGroup,
            "📍 Warning Locations": this.warningLocationsGroup
        };
        this.layerControl = L.control.layers(baseMaps, overlayMaps, { position: 'topright' }).addTo(this.map);

        // Initialize prominent District & State Labels
        this.renderDistrictBoundariesAndLabels();

        // Click to set warning location or user position
        this.map.on('click', (e) => {
            const { lat, lng } = e.latlng;
            if (this.clickMode === 'SET_WARNING') {
                this.setWarningLocation(lat, lng, `Warning Zone (${lat.toFixed(3)}, ${lng.toFixed(3)})`, 'CRITICAL');
                if (window.App && typeof window.App.onWarningLocationSet === 'function') {
                    window.App.onWarningLocationSet(lat, lng);
                }
            } else if (this.clickMode === 'SET_USER') {
                this.updateUserLocation(lat, lng, this.currentEpicenterLat || defaultLat, this.currentEpicenterLon || defaultLon);
                if (window.App && typeof window.App.evaluateSimulatedUserCoordinates === 'function') {
                    window.App.evaluateSimulatedUserCoordinates(lat, lng);
                }
            }
        });

        this.setWarningLocation(defaultLat, defaultLon, 'Kakinada Warning Epicenter', 'CRITICAL');
        this.renderBoundaryTestPins(defaultLat, defaultLon);

        // Initialize 3D Earth Globe
        this.initGlobeVisualizer();
    },

    initGlobeVisualizer() {
        if (typeof DisasterGlobeVisualizer !== 'undefined') {
            try {
                this.globeVisualizer = new DisasterGlobeVisualizer('globeContainer');
            } catch (err) {
                console.warn('[GLOBE] 3D Globe initialization notice:', err);
            }
        }
    },

    /**
     * Renders prominent Country, State, and District labels & boundaries for key monitoring zones.
     */
    renderDistrictBoundariesAndLabels() {
        if (!this.districtBadgesGroup) return;
        this.districtBadgesGroup.clearLayers();

        const districts = [
            {
                name: 'Kakinada District',
                state: 'Andhra Pradesh',
                country: 'India',
                lat: 16.9891,
                lon: 82.2475,
                color: '#ef4444',
                desc: 'Active Disaster Monitoring Zone'
            },
            {
                name: 'Visakhapatnam District',
                state: 'Andhra Pradesh',
                country: 'India',
                lat: 17.6868,
                lon: 83.2185,
                color: '#38bdf8',
                desc: 'Coastal Deepwater Sector'
            },
            {
                name: 'NTR District (Vijayawada)',
                state: 'Andhra Pradesh',
                country: 'India',
                lat: 16.5062,
                lon: 80.6480,
                color: '#a855f7',
                desc: 'Krishna River Basin'
            },
            {
                name: 'Hyderabad District',
                state: 'Telangana',
                country: 'India',
                lat: 17.3850,
                lon: 78.4867,
                color: '#10b981',
                desc: 'Musi River Floodplain'
            },
            {
                name: 'Guntur District',
                state: 'Andhra Pradesh',
                country: 'India',
                lat: 16.3067,
                lon: 80.4365,
                color: '#f59e0b',
                desc: 'Coastal Inundation Zone'
            },
            {
                name: 'East Godavari District',
                state: 'Andhra Pradesh',
                country: 'India',
                lat: 17.0005,
                lon: 81.7820,
                color: '#ec4899',
                desc: 'Godavari Delta Plains'
            }
        ];

        districts.forEach(d => {
            // District demarcation boundary circle
            const boundary = L.circle([d.lat, d.lon], {
                radius: 20000, // 20km district core zone
                color: d.color,
                weight: 1.5,
                dashArray: '4, 4',
                fillColor: d.color,
                fillOpacity: 0.08
            });

            // Prominent District Label Tag
            const labelHtml = `
                <div class="district-map-badge" style="border-left: 3px solid ${d.color};">
                    <span class="dmb-country">🇮🇳 ${d.country} • ${d.state}</span>
                    <span class="dmb-name">🏛️ ${d.name}</span>
                </div>
            `;

            const labelIcon = L.divIcon({
                html: labelHtml,
                className: 'custom-district-badge-icon',
                iconSize: [180, 36],
                iconAnchor: [90, 18]
            });

            const marker = L.marker([d.lat, d.lon], { icon: labelIcon });
            marker.bindPopup(`
                <div style="font-family:sans-serif; color:#0f172a; min-width:180px;">
                    <h4 style="margin:0 0 4px 0; color:${d.color}; font-size:13px;">🏛️ ${d.name}</h4>
                    <div style="font-size:11px; color:#475569;"><b>State:</b> ${d.state}</div>
                    <div style="font-size:11px; color:#475569;"><b>Country:</b> ${d.country}</div>
                    <div style="font-size:11px; color:#64748b; margin-top:4px;">${d.desc}</div>
                </div>
            `);

            this.districtBadgesGroup.addLayer(boundary);
            this.districtBadgesGroup.addLayer(marker);
        });
    },

    /**
     * Seamlessly toggles mapping mode between:
     * 'GLOBE' (3D Interactive Photorealistic Earth Globe)
     * 'GOOGLE_HYBRID' (Google Satellite Imagery with official District, State, Country Labels)
     * 'GOOGLE_ROAD' (Google Maps Vector Road with detailed Typography)
     * 'GOOGLE_TERRAIN' (Google Topography)
     * 'SATELLITE' (Esri World Imagery)
     * 'DARK' (Tactical Dark Map)
     */
    setMapType(type) {
        this.currentMapType = type;

        const mapWrapper = document.getElementById('disasterMap');
        const globeContainer = document.getElementById('globeContainer');

        if (type === 'GLOBE') {
            if (mapWrapper) mapWrapper.style.display = 'none';
            if (globeContainer) {
                globeContainer.style.display = 'block';
                if (this.globeVisualizer) {
                    this.globeVisualizer.resize();
                    this.globeVisualizer.updateEpicenter(
                        this.currentEpicenterLat || 16.9891,
                        this.currentEpicenterLon || 82.2475,
                        'Active Epicenter',
                        'CRITICAL'
                    );
                    this.globeVisualizer.focusOnCoordinates(
                        this.currentEpicenterLat || 16.9891,
                        this.currentEpicenterLon || 82.2475
                    );
                }
            }
        } else {
            if (globeContainer) globeContainer.style.display = 'none';
            if (mapWrapper) {
                mapWrapper.style.display = 'block';
                if (this.map) {
                    this.map.invalidateSize();
                }
            }

            if (!this.map) return;

            // Remove previous base layers
            Object.values(this.baseLayers).forEach(layer => {
                if (this.map.hasLayer(layer)) this.map.removeLayer(layer);
            });

            if (type === 'GOOGLE_HYBRID') {
                this.baseLayers.googleHybrid.addTo(this.map);
            } else if (type === 'GOOGLE_ROAD') {
                this.baseLayers.googleRoad.addTo(this.map);
            } else if (type === 'GOOGLE_TERRAIN') {
                this.baseLayers.googleTerrain.addTo(this.map);
            } else if (type === 'SATELLITE') {
                this.baseLayers.satellite.addTo(this.map);
                this.baseLayers.satelliteLabels.addTo(this.map);
            } else if (type === 'DARK') {
                this.baseLayers.dark.addTo(this.map);
            }
        }

        // Update toolbar active buttons
        const btnIds = ['btnMapGlobe', 'btnMapGoogleHybrid', 'btnMapGoogleRoad', 'btnMapSatellite', 'btnMapDark'];
        btnIds.forEach(id => {
            const btn = document.getElementById(id);
            if (btn) btn.classList.remove('primary');
        });

        const activeMap = {
            'GLOBE': 'btnMapGlobe',
            'GOOGLE_HYBRID': 'btnMapGoogleHybrid',
            'GOOGLE_ROAD': 'btnMapGoogleRoad',
            'SATELLITE': 'btnMapSatellite',
            'DARK': 'btnMapDark'
        };

        const activeBtn = document.getElementById(activeMap[type]);
        if (activeBtn) activeBtn.classList.add('primary');

        console.log(`[MAP] Switched mapping view to ${type}`);
    },

    setClickMode(mode) {
        this.clickMode = mode;
        const statusEl = document.getElementById('mapClickModeStatus');
        if (statusEl) {
            statusEl.innerText = mode === 'SET_WARNING' ? '🎯 Click map to set Warning Epicenter' : '📍 Click map to set Your Location';
            statusEl.style.color = mode === 'SET_WARNING' ? '#ef4444' : '#22c55e';
        }
    },

    setWarningLocation(lat, lon, locationName = 'Warning Location', severity = 'CRITICAL') {
        this.currentEpicenterLat = lat;
        this.currentEpicenterLon = lon;

        if (this.globeVisualizer) {
            this.globeVisualizer.updateEpicenter(lat, lon, locationName, severity);
        }

        if (!this.map) return;

        // Remove old epicenter & radius circles
        if (this.epicenterMarker) this.map.removeLayer(this.epicenterMarker);
        if (this.disasterCircle) this.map.removeLayer(this.disasterCircle);
        if (this.weatherCircle) this.map.removeLayer(this.weatherCircle);
        if (this.impactCoreCircle) this.map.removeLayer(this.impactCoreCircle);

        const isCritical = (severity === 'CRITICAL' || severity === 'HIGH');
        const color = isCritical ? '#ef4444' : '#f59e0b';

        // 1. Epicenter Icon with Pulsing Beacon
        const epicenterHtml = `
            <div class="pulse-marker" style="--pulse-color: ${color};">
                <div class="pulse-ring"></div>
                <div class="pulse-core">🚨</div>
            </div>
        `;
        const epicenterIcon = L.divIcon({
            html: epicenterHtml,
            className: 'custom-epicenter-icon',
            iconSize: [40, 40],
            iconAnchor: [20, 20]
        });

        this.epicenterMarker = L.marker([lat, lon], { icon: epicenterIcon, zIndexOffset: 1000 }).addTo(this.map);
        this.epicenterMarker.bindPopup(`
            <div style="font-family:sans-serif; color:#0f172a; min-width:200px;">
                <h4 style="margin:0 0 6px 0; color:#ef4444; font-size:14px;">🚨 Active Warning Location</h4>
                <div><b>Location:</b> ${locationName}</div>
                <div><b>Coordinates:</b> ${lat.toFixed(4)}°N, ${lon.toFixed(4)}°E</div>
                <div><b>Severity:</b> <span style="color:${color};font-weight:bold;text-transform:uppercase;">${severity}</span></div>
                <hr style="margin:6px 0; border:0; border-top:1px solid #e2e8f0;"/>
                <div style="font-size:11px; color:#64748b;">
                  • 0–100 km: <b>Disaster Alert Area</b><br/>
                  • 100–150 km: <b>Weather Advisory Area</b>
                </div>
            </div>
        `).openPopup();

        // 2. High Impact Core (0-25 km)
        this.impactCoreCircle = L.circle([lat, lon], {
            radius: 25000, // 25 km
            color: '#dc2626',
            weight: 1.5,
            fillColor: '#ef4444',
            fillOpacity: 0.22
        }).addTo(this.map);

        // 3. Exact 100 KM DISASTER ALERT RADIUS (Red Dashed)
        this.disasterCircle = L.circle([lat, lon], {
            radius: 100000, // 100,000 meters = 100 km
            color: '#ef4444',
            weight: 3,
            dashArray: '8, 8',
            fillColor: '#ef4444',
            fillOpacity: 0.10
        }).addTo(this.map);
        this.disasterCircle.bindTooltip('<b>100 KM DISASTER ALERT RADIUS</b><br/>Disaster Warning + Weather Alert Area', {
            permanent: false,
            direction: 'top',
            className: 'radius-tooltip-disaster'
        });

        // 4. Exact 150 KM WEATHER ALERT RADIUS (Blue Dashed)
        this.weatherCircle = L.circle([lat, lon], {
            radius: 150000, // 150,000 meters = 150 km
            color: '#0284c7',
            weight: 2.5,
            dashArray: '5, 5',
            fillColor: '#38bdf8',
            fillOpacity: 0.05
        }).addTo(this.map);
        this.weatherCircle.bindTooltip('<b>150 KM WEATHER ALERT RADIUS</b><br/>Weather Advisory ONLY (Disaster Warning Suppressed)', {
            permanent: false,
            direction: 'bottom',
            className: 'radius-tooltip-weather'
        });

        // Animated camera flyTo
        this.map.flyTo([lat, lon], 8, {
            animate: true,
            duration: 1.2
        });

        // Update state indicator banner
        const stateText = document.getElementById('mapStateText');
        if (stateText) {
            stateText.innerHTML = `<span style="color:${color}; font-weight:bold;">${severity} HAZARD MAPPING ACTIVE:</span> ${locationName} (${lat.toFixed(3)}, ${lon.toFixed(3)})`;
        }
    },

    renderSensors(sensorsList) {
        if (!this.sensorMarkersGroup) return;
        this.sensorMarkersGroup.clearLayers();

        if (!sensorsList || !sensorsList.length) return;

        sensorsList.forEach(sensor => {
            const isAnomaly = sensor.anomaly_detected || sensor.is_anomaly;
            const isDegraded = sensor.health_status === 'DEGRADED' || sensor.health_status === 'OFFLINE';
            let markerColor = '#10b981';
            let iconText = '🟢';

            if (isAnomaly) {
                markerColor = '#ef4444';
                iconText = '🔴';
            } else if (isDegraded) {
                markerColor = '#f59e0b';
                iconText = '🟡';
            }

            const sensorIcon = L.divIcon({
                html: `<div class="sensor-node-dot" style="background:${markerColor};" title="${sensor.sensor_name}"><span>${iconText}</span></div>`,
                className: 'custom-sensor-icon',
                iconSize: [24, 24],
                iconAnchor: [12, 12]
            });

            const marker = L.marker([sensor.latitude, sensor.longitude], { icon: sensorIcon });
            marker.bindPopup(`
                <div style="font-family:sans-serif; color:#0f172a; min-width:180px;">
                    <h4 style="margin:0 0 4px 0; color:#1e293b; font-size:13px;">📡 ${sensor.sensor_name}</h4>
                    <div style="font-size:11px; color:#64748b;">ID: <code>${sensor.sensor_id}</code></div>
                    <div style="font-size:11px; margin-top:4px;"><b>Type:</b> ${sensor.sensor_type}</div>
                    <div style="font-size:12px; margin-top:2px;"><b>Reading:</b> <b style="color:${markerColor};">${sensor.current_value !== undefined ? sensor.current_value : '--'} ${sensor.unit || ''}</b></div>
                    <div style="font-size:11px;"><b>Reliability:</b> ${(sensor.reliability_score * 100).toFixed(0)}%</div>
                    <div style="font-size:11px;"><b>Health:</b> ${sensor.health_status}</div>
                </div>
            `);
            this.sensorMarkersGroup.addLayer(marker);
        });
    },

    renderBoundaryTestPins(lat, lon) {
        if (!this.boundaryMarkersGroup) return;
        this.boundaryMarkersGroup.clearLayers();

        // Offsets approx (1 deg ~ 111km)
        const d100 = 100 / 111.0;
        const d100_1 = 100.1 / 111.0;
        const d150 = 150 / 111.0;
        const d150_1 = 150.1 / 111.0;

        const testPoints = [
            { lat: lat + d100, lon: lon, lbl: '100.0 km (Disaster Boundary)', status: 'DISASTER_ZONE' },
            { lat: lat, lon: lon + d100_1, lbl: '100.1 km (Weather Boundary Entry)', status: 'WEATHER_ZONE' },
            { lat: lat - d150, lon: lon, lbl: '150.0 km (Weather Boundary Edge)', status: 'WEATHER_ZONE' },
            { lat: lat, lon: lon - d150_1, lbl: '150.1 km (Safe Boundary Beyond)', status: 'SAFE_ZONE' }
        ];

        testPoints.forEach(p => {
            const color = p.status === 'DISASTER_ZONE' ? '#ef4444' : (p.status === 'WEATHER_ZONE' ? '#0284c7' : '#10b981');
            const pinIcon = L.divIcon({
                html: `<div style="background:${color}; color:#fff; font-size:10px; font-weight:bold; padding:2px 6px; border-radius:4px; white-space:nowrap; border:1px solid #fff; box-shadow:0 2px 4px rgba(0,0,0,0.5);">📍 ${p.lbl}</div>`,
                className: 'boundary-test-pin',
                iconSize: [120, 24],
                iconAnchor: [60, 24]
            });
            const m = L.marker([p.lat, p.lon], { icon: pinIcon });
            m.bindPopup(`<b>${p.lbl}</b><br/>Classification: <b>${p.status}</b>`);
            this.boundaryMarkersGroup.addLayer(m);
        });
    },

    updateUserLocation(userLat, userLon, epicenterLat, epicenterLon) {
        if (!this.map) return;

        if (this.userMarker) this.map.removeLayer(this.userMarker);
        if (this.distanceLine) this.map.removeLayer(this.distanceLine);

        const userIcon = L.divIcon({
            html: `<div style="background:#22c55e; width:16px; height:16px; border-radius:50%; border:2px solid #fff; box-shadow:0 0 10px #22c55e;"></div>`,
            className: 'custom-user-icon',
            iconSize: [16, 16],
            iconAnchor: [8, 8]
        });

        this.userMarker = L.marker([userLat, userLon], { icon: userIcon, zIndexOffset: 999 }).addTo(this.map);
        this.userMarker.bindPopup(`<b>👤 Simulated User Location</b><br/>Lat: ${userLat.toFixed(4)}, Lon: ${userLon.toFixed(4)}`).openPopup();

        this.distanceLine = L.polyline([[epicenterLat, epicenterLon], [userLat, userLon]], {
            color: '#38bdf8',
            weight: 2,
            dashArray: '6, 6'
        }).addTo(this.map);
    }
};

window.MapManager = MapManager;
