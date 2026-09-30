/**
 * ADSA Sensor Network Graph Interactive Canvas Visualizer.
 * Displays:
 * - Graph Nodes & Weighted Physical Couplings
 * - Animated BFS (Breadth-First Search) Traversal
 * - Animated DFS (Depth-First Search) Traversal
 * - Connected Components Visual Highlighting
 * - Neighbor Correlation Anomaly Verification (Isolated Glitch vs Hazard Cluster)
 */

class GraphVisualizer {
    constructor(canvasId) {
        this.canvas = document.getElementById(canvasId);
        this.ctx = this.canvas ? this.canvas.getContext('2d') : null;
        this.nodes = [];
        this.edges = [];
        this.animatingNodes = new Set();
        this.activeTraversal = [];
        this.traversalIndex = 0;
        this.timer = null;

        if (this.canvas) {
            this.resize();
            window.addEventListener('resize', () => this.resize());
        }
    }

    resize() {
        if (!this.canvas) return;
        const rect = this.canvas.parentElement.getBoundingClientRect();
        this.canvas.width = rect.width || 600;
        this.canvas.height = 360;
        this.render();
    }

    loadGraphData(nodesData, edgesData) {
        // Lay out nodes aesthetically in a network constellation
        const width = this.canvas.width;
        const height = this.canvas.height;
        const centerX = width / 2;
        const centerY = height / 2;

        const count = nodesData.length || 7;
        this.nodes = nodesData.map((n, i) => {
            const angle = (i / count) * 2 * Math.PI;
            const radius = 110 + (i % 2 === 0 ? 30 : -20);
            return {
                id: n.id || `S-${i}`,
                name: n.name || `Sensor ${i}`,
                type: n.type || 'RAINFALL',
                val: n.val || 20,
                x: centerX + radius * Math.cos(angle),
                y: centerY + radius * Math.sin(angle),
                health: n.health || 'HEALTHY',
                isHighlighted: false
            };
        });

        this.edges = edgesData.map(e => ({
            source: e.source,
            target: e.target,
            weight: e.weight || 0.8,
            coupling: e.coupling || 'PROXIMITY'
        }));

        this.render();
    }

    render() {
        if (!this.ctx || !this.canvas) return;
        const ctx = this.ctx;
        ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

        // 1. Draw Edges
        this.edges.forEach(edge => {
            const src = this.nodes.find(n => n.id === edge.source);
            const tgt = this.nodes.find(n => n.id === edge.target);
            if (src && tgt) {
                ctx.beginPath();
                ctx.moveTo(src.x, src.y);
                ctx.lineTo(tgt.x, tgt.y);
                ctx.strokeStyle = 'rgba(148, 163, 184, 0.25)';
                ctx.lineWidth = Math.max(1, edge.weight * 3.5);
                ctx.stroke();

                // Draw edge weight badge
                const midX = (src.x + tgt.x) / 2;
                const midY = (src.y + tgt.y) / 2;
                ctx.fillStyle = '#64748b';
                ctx.font = '9px monospace';
                ctx.fillText(`w:${edge.weight.toFixed(1)}`, midX - 10, midY);
            }
        });

        // 2. Draw Nodes
        this.nodes.forEach(n => {
            const isAnimated = this.animatingNodes.has(n.id);
            const radius = isAnimated ? 20 : 15;

            // Halo for highlighted or traversal active nodes
            if (isAnimated || n.isHighlighted) {
                ctx.beginPath();
                ctx.arc(n.x, n.y, radius + 8, 0, 2 * Math.PI);
                ctx.fillStyle = 'rgba(56, 189, 248, 0.3)';
                ctx.fill();
            }

            ctx.beginPath();
            ctx.arc(n.x, n.y, radius, 0, 2 * Math.PI);
            ctx.fillStyle = n.type === 'RAINFALL' ? '#38bdf8' :
                           (n.type === 'WATER_LEVEL' ? '#a855f7' :
                           (n.type === 'WIND_SPEED' ? '#f59e0b' : '#10b981'));
            ctx.fill();
            ctx.strokeStyle = '#ffffff';
            ctx.lineWidth = 2;
            ctx.stroke();

            // Label
            ctx.fillStyle = '#f8fafc';
            ctx.font = '10px Inter, sans-serif';
            ctx.textAlign = 'center';
            ctx.fillText(n.id.replace('SNS-', ''), n.x, n.y + radius + 14);
            ctx.font = '9px Inter, sans-serif';
            ctx.fillStyle = '#94a3b8';
            ctx.fillText(`${n.val} ${n.type === 'RAINFALL' ? 'mm' : (n.type === 'WATER_LEVEL' ? 'm' : '')}`, n.x, n.y + radius + 25);
        });
    }

    runBfsAnimation() {
        if (this.nodes.length === 0) return;
        this.stopAnimation();

        // Compute BFS order from first node
        const visited = new Set();
        const queue = [this.nodes[0].id];
        const traversal = [];

        visited.add(this.nodes[0].id);

        while (queue.length > 0) {
            const curr = queue.shift();
            traversal.push(curr);

            this.edges.forEach(e => {
                let neighbor = null;
                if (e.source === curr) neighbor = e.target;
                if (e.target === curr) neighbor = e.source;

                if (neighbor && !visited.has(neighbor)) {
                    visited.add(neighbor);
                    queue.push(neighbor);
                }
            });
        }

        this.startStepAnimation(traversal, 'ADSA BFS (Wave Propagation)');
    }

    runDfsAnimation() {
        if (this.nodes.length === 0) return;
        this.stopAnimation();

        const visited = new Set();
        const traversal = [];

        const dfsRec = (id) => {
            visited.add(id);
            traversal.push(id);
            this.edges.forEach(e => {
                let neighbor = null;
                if (e.source === id) neighbor = e.target;
                if (e.target === id) neighbor = e.source;
                if (neighbor && !visited.has(neighbor)) {
                    dfsRec(neighbor);
                }
            });
        };

        dfsRec(this.nodes[0].id);
        this.startStepAnimation(traversal, 'ADSA DFS (Deep Catchment Chain)');
    }

    startStepAnimation(traversal, modeLabel) {
        this.animatingNodes.clear();
        this.activeTraversal = traversal;
        this.traversalIndex = 0;

        const infoEl = document.getElementById('graphStatusText');
        if (infoEl) infoEl.innerHTML = `<span style="color:#38bdf8;">${modeLabel}:</span> Starting traversal...`;

        this.timer = setInterval(() => {
            if (this.traversalIndex >= this.activeTraversal.length) {
                if (infoEl) infoEl.innerHTML = `<span style="color:#10b981;">${modeLabel} Complete:</span> Visited ${traversal.length} nodes in order: ${traversal.join(' &rarr; ')}`;
                this.stopAnimation();
                return;
            }

            const currId = this.activeTraversal[this.traversalIndex];
            this.animatingNodes.add(currId);
            if (infoEl) {
                infoEl.innerHTML = `<span style="color:#38bdf8;">${modeLabel}:</span> Visiting Node <b>${currId}</b> (Step ${this.traversalIndex + 1}/${traversal.length})`;
            }
            window.soundEngine.playChime();
            this.render();
            this.traversalIndex++;
        }, 700);
    }

    stopAnimation() {
        if (this.timer) {
            clearInterval(this.timer);
            this.timer = null;
        }
    }
}

window.GraphVisualizer = GraphVisualizer;
