/**
 * 3D Photorealistic Interactive Earth Globe for Disaster Early-Warning Agent.
 * Uses Three.js to render a true 3D spherical globe with:
 * - Earth sphere with continental terrain & atmosphere glow
 * - Dynamic 3D Epicenter marker at exact coordinates
 * - 3D Spherical 100km Disaster Radius & 150km Weather Radius rings
 * - 3D IoT Sensor Beacons
 * - Interactive mouse rotation, inertia, zoom, and auto-rotate controls
 */

class DisasterGlobeVisualizer {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.scene = null;
        this.camera = null;
        this.renderer = null;
        this.globe = null;
        this.atmosphere = null;
        this.epicenterMesh = null;
        this.disasterRingMesh = null;
        this.weatherRingMesh = null;
        this.sensorsGroup = null;
        this.labelsGroup = null;

        this.radius = 100; // Base Earth sphere radius
        this.isRotating = true;
        this.isDragging = false;
        this.previousMousePosition = { x: 0, y: 0 };
        this.rotationSpeed = { x: 0, y: 0.002 };
        this.activeLocation = { lat: 16.9891, lon: 82.2475, name: 'Kakinada Epicenter', level: 'CRITICAL' };

        this.init();
    }

    init() {
        if (!this.container || typeof THREE === 'undefined') {
            console.warn('[GLOBE] Three.js not loaded or container not found.');
            return;
        }

        const width = this.container.clientWidth || 800;
        const height = this.container.clientHeight || 520;

        // 1. Scene
        this.scene = new THREE.Scene();

        // 2. Camera
        this.camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 2000);
        this.camera.position.z = 280;

        // 3. WebGL Renderer
        this.renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
        this.renderer.setSize(width, height);
        this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
        this.renderer.shadowMap.enabled = true;
        this.container.innerHTML = '';
        this.container.appendChild(this.renderer.domElement);

        // 4. Lights
        const ambientLight = new THREE.AmbientLight(0xffffff, 0.85);
        this.scene.add(ambientLight);

        const sunLight = new THREE.DirectionalLight(0xffffff, 1.2);
        sunLight.position.set(200, 150, 200);
        this.scene.add(sunLight);

        const backLight = new THREE.DirectionalLight(0x38bdf8, 0.4);
        backLight.position.set(-200, -100, -200);
        this.scene.add(backLight);

        // 5. Earth Sphere
        this.createEarthSphere();

        // 6. Atmosphere Halo
        this.createAtmosphere();

        // 7. Groups for dynamic markers
        this.sensorsGroup = new THREE.Group();
        this.labelsGroup = new THREE.Group();
        this.scene.add(this.sensorsGroup);
        this.scene.add(this.labelsGroup);

        // 8. Event Listeners
        this.setupInteraction();

        // 9. Initial Epicenter
        this.updateEpicenter(this.activeLocation.lat, this.activeLocation.lon, this.activeLocation.name, this.activeLocation.level);
        this.focusOnCoordinates(this.activeLocation.lat, this.activeLocation.lon);

        // 10. Start Animation Loop
        this.animate = this.animate.bind(this);
        requestAnimationFrame(this.animate);

        console.log('[GLOBE] 3D Photorealistic Interactive Globe Initialized.');
    }

    createEarthSphere() {
        const geometry = new THREE.SphereGeometry(this.radius, 64, 64);

        // Generate procedural canvas texture representing Earth Continents with India/Andhra prominently highlighted
        const canvas = document.createElement('canvas');
        canvas.width = 2048;
        canvas.height = 1024;
        const ctx = canvas.getContext('2d');

        // Deep blue ocean background
        const oceanGrad = ctx.createLinearGradient(0, 0, 0, 1024);
        oceanGrad.addColorStop(0, '#0c1e38');
        oceanGrad.addColorStop(0.5, '#08172e');
        oceanGrad.addColorStop(1, '#051024');
        ctx.fillStyle = oceanGrad;
        ctx.fillRect(0, 0, 2048, 1024);

        // Draw longitude & latitude grid lines
        ctx.strokeStyle = 'rgba(56, 189, 248, 0.12)';
        ctx.lineWidth = 1;
        for (let x = 0; x <= 2048; x += 128) {
            ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, 1024); ctx.stroke();
        }
        for (let y = 0; y <= 1024; y += 128) {
            ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(2048, y); ctx.stroke();
        }

        // Draw Equator & Tropics
        ctx.strokeStyle = 'rgba(56, 189, 248, 0.25)';
        ctx.lineWidth = 1.5;
        ctx.beginPath(); ctx.moveTo(0, 512); ctx.lineTo(2048, 512); ctx.stroke();

        // Approximate World Landmass Continents
        ctx.fillStyle = '#1e3a29'; // Terrain Green
        ctx.strokeStyle = '#34d399'; // Borders
        ctx.lineWidth = 1.5;

        const drawLand = (pts) => {
            ctx.beginPath();
            pts.forEach((p, idx) => {
                const px = ((p[1] + 180) / 360) * 2048;
                const py = ((90 - p[0]) / 180) * 1024;
                if (idx === 0) ctx.moveTo(px, py); else ctx.lineTo(px, py);
            });
            ctx.closePath();
            ctx.fill();
            ctx.stroke();
        };

        // India & South Asia (High Detail)
        drawLand([
            [35, 74], [34, 78], [30, 81], [27, 88], [28, 96], [22, 92], [22, 88],
            [16.5, 82.5], [13, 80], [8.5, 77.5], [10, 76], [15, 73.8], [20, 72.8],
            [24, 69], [28, 70], [33, 72]
        ]);

        // Highlight India with country & district fill
        const indX = ((82.25 + 180) / 360) * 2048;
        const indY = ((90 - 17.0) / 180) * 1024;
        ctx.fillStyle = 'rgba(56, 189, 248, 0.35)';
        ctx.beginPath();
        ctx.arc(indX, indY, 28, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#38bdf8';
        ctx.stroke();

        // Label: INDIA & ANDHRA PRADESH
        ctx.fillStyle = '#f8fafc';
        ctx.font = 'bold 18px Inter, sans-serif';
        ctx.fillText('INDIA', indX - 25, indY - 12);
        ctx.fillStyle = '#38bdf8';
        ctx.font = 'bold 12px Inter, sans-serif';
        ctx.fillText('Andhra Pradesh', indX - 42, indY + 14);

        // Asia / Eurasia continent
        drawLand([[70, 40], [72, 140], [60, 160], [40, 140], [35, 120], [20, 110], [10, 105], [5, 100], [20, 75], [30, 60], [40, 50], [55, 30], [65, 30]]);
        // Africa
        drawLand([[35, -5], [37, 10], [30, 32], [12, 44], [-5, 40], [-25, 33], [-34, 18], [-15, 12], [5, 2], [15, -17], [30, -10]]);
        // Europe
        drawLand([[60, 5], [65, 25], [55, 35], [45, 30], [36, -5], [45, -8], [55, -5]]);
        // North America
        drawLand([[70, -160], [70, -80], [50, -55], [30, -80], [20, -105], [30, -120], [55, -135], [65, -168]]);
        // South America
        drawLand([[10, -75], [5, -50], [-10, -35], [-35, -55], [-55, -68], [-35, -72], [-15, -75], [-5, -80]]);
        // Australia
        drawLand([[-12, 130], [-15, 145], [-25, 153], [-38, 145], [-35, 115], [-20, 115]]);

        // Load Earth texture or use high-res canvas
        const texture = new THREE.CanvasTexture(canvas);
        texture.anisotropy = 16;

        // Bump canvas
        const material = new THREE.MeshPhongMaterial({
            map: texture,
            specular: new THREE.Color(0x224466),
            shininess: 25,
            bumpScale: 1.5
        });

        // Try loading real photorealistic NASA Blue Marble texture asynchronously
        const imgLoader = new THREE.TextureLoader();
        imgLoader.load('https://unpkg.com/three-globe/example/img/earth-blue-marble.jpg', (loadedTexture) => {
            material.map = loadedTexture;
            material.needsUpdate = true;
            console.log('[GLOBE] Real NASA satellite Blue Marble Earth texture loaded.');
        }, undefined, () => {
            console.log('[GLOBE] Running with crisp procedural satellite vector map texture.');
        });

        this.globe = new THREE.Mesh(geometry, material);
        this.scene.add(this.globe);
    }

    createAtmosphere() {
        const atmoGeo = new THREE.SphereGeometry(this.radius * 1.035, 48, 48);
        const atmoMat = new THREE.ShaderMaterial({
            vertexShader: `
                varying vec3 vNormal;
                void main() {
                    vNormal = normalize(normalMatrix * normal);
                    gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
                }
            `,
            fragmentShader: `
                varying vec3 vNormal;
                void main() {
                    float intensity = pow(0.65 - dot(vNormal, vec3(0, 0, 1.0)), 2.2);
                    gl_FragColor = vec4(0.22, 0.74, 0.97, 0.4) * intensity;
                }
            `,
            blending: THREE.AdditiveBlending,
            side: THREE.BackSide,
            transparent: true
        });
        this.atmosphere = new THREE.Mesh(atmoGeo, atmoMat);
        this.scene.add(this.atmosphere);
    }

    latLonToVector3(lat, lon, altitude = 0) {
        const phi = (90 - lat) * (Math.PI / 180);
        const theta = (lon + 180) * (Math.PI / 180);
        const r = this.radius + altitude;
        return new THREE.Vector3(
            -(r * Math.sin(phi) * Math.cos(theta)),
            r * Math.cos(phi),
            r * Math.sin(phi) * Math.sin(theta)
        );
    }

    updateEpicenter(lat, lon, name = 'Epicenter', level = 'CRITICAL') {
        this.activeLocation = { lat, lon, name, level };

        // 1. Epicenter Beacon
        if (this.epicenterMarker) this.scene.remove(this.epicenterMarker);
        if (this.disasterRingMesh) this.scene.remove(this.disasterRingMesh);
        if (this.weatherRingMesh) this.scene.remove(this.weatherRingMesh);

        const pos = this.latLonToVector3(lat, lon, 1.5);

        // Glowing Core
        const beaconGeo = new THREE.SphereGeometry(2.5, 16, 16);
        const beaconMat = new THREE.MeshBasicMaterial({
            color: level === 'CRITICAL' ? 0xef4444 : (level === 'WARNING' ? 0xf59e0b : 0x10b981)
        });
        this.epicenterMarker = new THREE.Mesh(beaconGeo, beaconMat);
        this.epicenterMarker.position.copy(pos);
        this.scene.add(this.epicenterMarker);

        // 2. 100 KM DISASTER ALERT RADIUS RING (Spherical Projection)
        // 100km is approx 0.9 degrees on Earth (Earth radius = 6371km, 100km / 6371 * 100 = ~1.57 units)
        const ringGeo100 = new THREE.RingGeometry(5.2, 5.8, 48);
        const ringMat100 = new THREE.MeshBasicMaterial({
            color: 0xef4444,
            side: THREE.DoubleSide,
            transparent: true,
            opacity: 0.85
        });
        this.disasterRingMesh = new THREE.Mesh(ringGeo100, ringMat100);
        this.disasterRingMesh.position.copy(pos);
        this.disasterRingMesh.lookAt(new THREE.Vector3(0, 0, 0));
        this.scene.add(this.disasterRingMesh);

        // 3. 150 KM WEATHER ALERT RADIUS RING
        const ringGeo150 = new THREE.RingGeometry(8.2, 8.8, 48);
        const ringMat150 = new THREE.MeshBasicMaterial({
            color: 0x0284c7,
            side: THREE.DoubleSide,
            transparent: true,
            opacity: 0.7
        });
        this.weatherRingMesh = new THREE.Mesh(ringGeo150, ringMat150);
        this.weatherRingMesh.position.copy(pos);
        this.weatherRingMesh.lookAt(new THREE.Vector3(0, 0, 0));
        this.scene.add(this.weatherRingMesh);

        this.updateGlobeLabels(lat, lon, name);
    }

    updateGlobeLabels(lat, lon, name) {
        // Clear previous sensors
        while (this.sensorsGroup.children.length > 0) {
            this.sensorsGroup.remove(this.sensorsGroup.children[0]);
        }

        // Add 3D IoT Sensor pins around the active region
        const sensorOffsets = [
            { dLat: 0.08, dLon: 0.12, name: 'Port Rain Gauge Alpha', type: 'RAINFALL' },
            { dLat: -0.06, dLon: -0.08, name: 'Sluice River Hydro Node', type: 'WATER_LEVEL' },
            { dLat: 0.14, dLon: -0.10, name: 'Coastal Anemometer', type: 'WIND' },
            { dLat: -0.12, dLon: 0.15, name: 'Town Hall Humidity Node', type: 'HUMIDITY' }
        ];

        sensorOffsets.forEach(s => {
            const sPos = this.latLonToVector3(lat + s.dLat, lon + s.dLon, 1.2);
            const dotGeo = new THREE.SphereGeometry(1.2, 12, 12);
            const dotMat = new THREE.MeshBasicMaterial({ color: 0x10b981 });
            const dot = new THREE.Mesh(dotGeo, dotMat);
            dot.position.copy(sPos);
            this.sensorsGroup.add(dot);
        });
    }

    focusOnCoordinates(lat, lon) {
        // Smoothly rotate the globe so (lat, lon) faces the camera
        const targetRotY = -((lon + 90) * (Math.PI / 180));
        const targetRotX = (lat * (Math.PI / 180));

        if (this.globe) {
            this.globe.rotation.y = targetRotY;
            this.globe.rotation.x = targetRotX;
        }
    }

    setupInteraction() {
        const dom = this.renderer.domElement;

        dom.addEventListener('mousedown', (e) => {
            this.isDragging = true;
            this.isRotating = false; // Pause auto-rotation when user interacts
            this.previousMousePosition = { x: e.clientX, y: e.clientY };
        });

        window.addEventListener('mouseup', () => {
            this.isDragging = false;
        });

        dom.addEventListener('mousemove', (e) => {
            if (!this.isDragging) return;

            const deltaX = e.clientX - this.previousMousePosition.x;
            const deltaY = e.clientY - this.previousMousePosition.y;

            if (this.globe) {
                this.globe.rotation.y += deltaX * 0.005;
                this.globe.rotation.x += deltaY * 0.005;

                // Clamp X rotation to prevent flipping
                this.globe.rotation.x = Math.max(-Math.PI / 2.5, Math.min(Math.PI / 2.5, this.globe.rotation.x));
            }

            this.previousMousePosition = { x: e.clientX, y: e.clientY };
        });

        // Mousewheel Zoom
        dom.addEventListener('wheel', (e) => {
            e.preventDefault();
            this.camera.position.z += e.deltaY * 0.15;
            this.camera.position.z = Math.max(140, Math.min(450, this.camera.position.z));
        }, { passive: false });

        // Touch support for mobile/tablets
        let touchStart = { x: 0, y: 0 };
        dom.addEventListener('touchstart', (e) => {
            if (e.touches.length === 1) {
                this.isDragging = true;
                this.isRotating = false;
                touchStart = { x: e.touches[0].clientX, y: e.touches[0].clientY };
            }
        });

        dom.addEventListener('touchmove', (e) => {
            if (!this.isDragging || e.touches.length !== 1) return;
            const deltaX = e.touches[0].clientX - touchStart.x;
            const deltaY = e.touches[0].clientY - touchStart.y;

            if (this.globe) {
                this.globe.rotation.y += deltaX * 0.005;
                this.globe.rotation.x += deltaY * 0.005;
            }
            touchStart = { x: e.touches[0].clientX, y: e.touches[0].clientY };
        });

        dom.addEventListener('touchend', () => {
            this.isDragging = false;
        });

        // Window resize handler
        window.addEventListener('resize', () => {
            if (!this.container || !this.renderer || !this.camera) return;
            const w = this.container.clientWidth;
            const h = this.container.clientHeight;
            this.camera.aspect = w / h;
            this.camera.updateProjectionMatrix();
            this.renderer.setSize(w, h);
        });
    }

    toggleAutoRotate() {
        this.isRotating = !this.isRotating;
        return this.isRotating;
    }

    animate() {
        requestAnimationFrame(this.animate);

        // Auto spin if enabled and user not dragging
        if (this.isRotating && !this.isDragging && this.globe) {
            this.globe.rotation.y += 0.0018;
        }

        // Pulse the epicenter rings
        if (this.disasterRingMesh) {
            const time = Date.now() * 0.003;
            const s = 1.0 + Math.sin(time) * 0.08;
            this.disasterRingMesh.scale.set(s, s, 1);
        }

        if (this.renderer && this.scene && this.camera) {
            this.renderer.render(this.scene, this.camera);
        }
    }

    resize() {
        if (!this.container || !this.renderer || !this.camera) return;
        const w = this.container.clientWidth;
        const h = this.container.clientHeight;
        this.camera.aspect = w / h;
        this.camera.updateProjectionMatrix();
        this.renderer.setSize(w, h);
    }
}

window.DisasterGlobeVisualizer = DisasterGlobeVisualizer;
