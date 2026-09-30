/**
 * Real-time Chart.js Dashboard Visualizations.
 * Renders:
 * 1. Rainfall Telemetry & Threshold Warning Line
 * 2. Water Level River Hydrograph with Critical Spillway Marks
 * 3. Multi-Hazard Sensor Distribution Breakdown
 */

const ChartsManager = {
    rainfallChart: null,
    waterLevelChart: null,
    riskFactorsChart: null,

    init() {
        if (typeof Chart === 'undefined') {
            console.warn('Chart.js not loaded yet');
            return;
        }

        // Global Chart Defaults for dark theme
        Chart.defaults.color = '#94a3b8';
        Chart.defaults.borderColor = '#1e293b';
        Chart.defaults.font.family = "'Inter', -apple-system, sans-serif";

        this.initRainfallChart();
        this.initWaterLevelChart();
    },

    initRainfallChart() {
        const ctx = document.getElementById('rainfallChartCanvas');
        if (!ctx) return;

        const labels = ['-50m', '-40m', '-30m', '-20m', '-10m', 'Now'];
        const data = [15, 22, 35, 68, 110, 145];

        this.rainfallChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [
                    {
                        label: 'Rainfall (mm)',
                        data: data,
                        borderColor: '#38bdf8',
                        backgroundColor: 'rgba(56, 189, 248, 0.15)',
                        fill: true,
                        tension: 0.35,
                        borderWidth: 3,
                        pointBackgroundColor: '#38bdf8',
                        pointRadius: 4
                    },
                    {
                        label: 'Warning Threshold (70mm)',
                        data: [70, 70, 70, 70, 70, 70],
                        borderColor: '#f59e0b',
                        borderDash: [5, 5],
                        borderWidth: 1.5,
                        pointRadius: 0,
                        fill: false
                    },
                    {
                        label: 'Critical Threshold (120mm)',
                        data: [120, 120, 120, 120, 120, 120],
                        borderColor: '#ef4444',
                        borderDash: [5, 5],
                        borderWidth: 2,
                        pointRadius: 0,
                        fill: false
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'top', labels: { boxWidth: 12, font: { size: 11 } } },
                    tooltip: { mode: 'index', intersect: false }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        max: 160,
                        title: { display: true, text: 'Accumulation (mm)', color: '#64748b' },
                        grid: { color: 'rgba(255, 255, 255, 0.05)' }
                    },
                    x: {
                        grid: { display: false }
                    }
                }
            }
        });
    },

    initWaterLevelChart() {
        const ctx = document.getElementById('waterLevelChartCanvas');
        if (!ctx) return;

        const labels = ['-50m', '-40m', '-30m', '-20m', '-10m', 'Now'];
        const data = [1.2, 1.4, 2.1, 3.2, 4.1, 4.8];

        this.waterLevelChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [
                    {
                        label: 'Water Level (m)',
                        data: data,
                        borderColor: '#a855f7',
                        backgroundColor: 'rgba(168, 85, 247, 0.15)',
                        fill: true,
                        tension: 0.35,
                        borderWidth: 3,
                        pointBackgroundColor: '#a855f7',
                        pointRadius: 4
                    },
                    {
                        label: 'Spillway Warning (3.5m)',
                        data: [3.5, 3.5, 3.5, 3.5, 3.5, 3.5],
                        borderColor: '#f59e0b',
                        borderDash: [5, 5],
                        borderWidth: 1.5,
                        pointRadius: 0,
                        fill: false
                    },
                    {
                        label: 'Danger Mark (4.5m)',
                        data: [4.5, 4.5, 4.5, 4.5, 4.5, 4.5],
                        borderColor: '#ef4444',
                        borderDash: [5, 5],
                        borderWidth: 2,
                        pointRadius: 0,
                        fill: false
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'top', labels: { boxWidth: 12, font: { size: 11 } } },
                    tooltip: { mode: 'index', intersect: false }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        max: 6.0,
                        title: { display: true, text: 'Stage Height (meters)', color: '#64748b' },
                        grid: { color: 'rgba(255, 255, 255, 0.05)' }
                    },
                    x: {
                        grid: { display: false }
                    }
                }
            }
        });
    },

    updateReadings(rainfallVal, waterLevelVal) {
        if (this.rainfallChart) {
            const data = this.rainfallChart.data.datasets[0].data;
            data.shift();
            data.push(rainfallVal);
            this.rainfallChart.update();
        }
        if (this.waterLevelChart) {
            const data = this.waterLevelChart.data.datasets[0].data;
            data.shift();
            data.push(waterLevelVal);
            this.waterLevelChart.update();
        }
    }
};

window.ChartsManager = ChartsManager;
