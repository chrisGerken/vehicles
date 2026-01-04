/**
 * Canvas renderer for simulation visualization.
 */
class Renderer {
    constructor(canvas) {
        this.canvas = canvas;
        this.ctx = canvas.getContext('2d');
        this.arenaWidth = 1000;
        this.arenaHeight = 1000;
        this.scale = 1.0;
        this.offsetX = 0;
        this.offsetY = 0;
        this.showSensorRays = true;
        this.showVehicleIds = false;

        this.resize();
        window.addEventListener('resize', () => this.resize());
    }

    /**
     * Resizes canvas to fit container.
     */
    resize() {
        const container = this.canvas.parentElement;
        const rect = container.getBoundingClientRect();

        this.canvas.width = rect.width;
        this.canvas.height = rect.height;

        // Calculate scale to fit arena in canvas with padding
        const padding = 40;
        const scaleX = (this.canvas.width - padding * 2) / this.arenaWidth;
        const scaleY = (this.canvas.height - padding * 2) / this.arenaHeight;
        this.scale = Math.min(scaleX, scaleY);

        // Center arena
        this.offsetX = (this.canvas.width - this.arenaWidth * this.scale) / 2;
        this.offsetY = (this.canvas.height - this.arenaHeight * this.scale) / 2;
    }

    /**
     * Sets arena dimensions.
     * @param {number} width - Arena width
     * @param {number} height - Arena height
     */
    setArenaDimensions(width, height) {
        this.arenaWidth = width;
        this.arenaHeight = height;
        this.resize();
    }

    /**
     * Converts arena coordinates to canvas coordinates.
     * @param {number} x - Arena X
     * @param {number} y - Arena Y
     * @returns {Object} Canvas coordinates {x, y}
     */
    toCanvasCoords(x, y) {
        return {
            x: this.offsetX + x * this.scale,
            y: this.offsetY + y * this.scale
        };
    }

    /**
     * Converts canvas coordinates to arena coordinates.
     * @param {number} canvasX - Canvas X
     * @param {number} canvasY - Canvas Y
     * @returns {Object} Arena coordinates {x, y}
     */
    toArenaCoords(canvasX, canvasY) {
        return {
            x: (canvasX - this.offsetX) / this.scale,
            y: (canvasY - this.offsetY) / this.scale
        };
    }

    /**
     * Clears the canvas.
     */
    clear() {
        this.ctx.fillStyle = '#1a1a1a';
        this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);

        // Draw arena border
        const topLeft = this.toCanvasCoords(0, 0);
        const bottomRight = this.toCanvasCoords(this.arenaWidth, this.arenaHeight);

        this.ctx.strokeStyle = '#444';
        this.ctx.lineWidth = 2;
        this.ctx.strokeRect(
            topLeft.x,
            topLeft.y,
            bottomRight.x - topLeft.x,
            bottomRight.y - topLeft.y
        );
    }

    /**
     * Renders the simulation state.
     * @param {Object} state - Simulation state from WebSocket
     */
    render(state) {
        this.clear();

        // Render static objects first (they're in the background)
        if (state.staticObjects) {
            for (const obj of state.staticObjects) {
                this.drawStaticObject(obj);
            }
        }

        // Render vehicles
        if (state.vehicles) {
            for (const vehicle of state.vehicles) {
                this.drawVehicle(vehicle);
            }
        }

        // Draw tick counter
        this.drawTickCounter(state.tick);
    }

    /**
     * Draws a static object (point light or wall).
     * @param {Object} obj - Static object
     */
    drawStaticObject(obj) {
        const pos = this.toCanvasCoords(obj.x, obj.y);

        if (obj.type === 'POINT') {
            // Draw point light with glow
            const radius = obj.radius * this.scale;

            // Glow effect
            if (obj.brightness > 0) {
                const gradient = this.ctx.createRadialGradient(
                    pos.x, pos.y, radius * 0.3,
                    pos.x, pos.y, radius * 2
                );
                gradient.addColorStop(0, this.hexToRgba(obj.color, obj.brightness * 0.3));
                gradient.addColorStop(1, this.hexToRgba(obj.color, 0));

                this.ctx.fillStyle = gradient;
                this.ctx.beginPath();
                this.ctx.arc(pos.x, pos.y, radius * 2, 0, Math.PI * 2);
                this.ctx.fill();
            }

            // Core
            this.ctx.fillStyle = obj.color;
            this.ctx.beginPath();
            this.ctx.arc(pos.x, pos.y, radius, 0, Math.PI * 2);
            this.ctx.fill();

        } else if (obj.type === 'WALL') {
            // Draw wall
            const pos2 = this.toCanvasCoords(obj.x2, obj.y2);

            this.ctx.strokeStyle = obj.color;
            this.ctx.lineWidth = 3;
            this.ctx.beginPath();
            this.ctx.moveTo(pos.x, pos.y);
            this.ctx.lineTo(pos2.x, pos2.y);
            this.ctx.stroke();
        }
    }

    /**
     * Draws a vehicle.
     * @param {Object} vehicle - Vehicle state
     */
    drawVehicle(vehicle) {
        const pos = this.toCanvasCoords(vehicle.x, vehicle.y);
        const radius = vehicle.radius * this.scale;

        this.ctx.save();
        this.ctx.translate(pos.x, pos.y);
        this.ctx.rotate(vehicle.angle);

        // Draw vehicle body
        this.ctx.fillStyle = vehicle.color;
        this.ctx.beginPath();
        this.ctx.arc(0, 0, radius, 0, Math.PI * 2);
        this.ctx.fill();

        // Draw direction indicator
        this.ctx.strokeStyle = '#ffffff';
        this.ctx.lineWidth = 2;
        this.ctx.beginPath();
        this.ctx.moveTo(0, 0);
        this.ctx.lineTo(radius, 0);
        this.ctx.stroke();

        // Draw sensor rays if enabled
        if (this.showSensorRays && vehicle.receptors) {
            for (const receptor of vehicle.receptors) {
                this.drawReceptorFOV(receptor, radius);
            }
        }

        this.ctx.restore();

        // Draw vehicle ID if enabled
        if (this.showVehicleIds) {
            this.ctx.fillStyle = '#ffffff';
            this.ctx.font = '10px monospace';
            this.ctx.textAlign = 'center';
            this.ctx.fillText(vehicle.id, pos.x, pos.y + radius + 12);
        }
    }

    /**
     * Draws a receptor's field of view.
     * @param {Object} receptor - Receptor configuration
     * @param {number} bodyRadius - Vehicle body radius
     */
    drawReceptorFOV(receptor, bodyRadius) {
        const maxRange = receptor.maxRange * this.scale;

        this.ctx.strokeStyle = this.hexToRgba(receptor.colorFilter || '#ffffff', 0.3);
        this.ctx.lineWidth = 1;
        this.ctx.beginPath();
        this.ctx.moveTo(0, 0);
        this.ctx.arc(0, 0, maxRange, receptor.angleFrom, receptor.angleTo);
        this.ctx.lineTo(0, 0);
        this.ctx.stroke();
    }

    /**
     * Draws the tick counter.
     * @param {number} tick - Current tick
     */
    drawTickCounter(tick) {
        this.ctx.fillStyle = '#ffffff';
        this.ctx.font = '16px monospace';
        this.ctx.textAlign = 'left';
        this.ctx.fillText(`Tick: ${tick}`, 10, 25);
    }

    /**
     * Converts hex color to rgba string.
     * @param {string} hex - Hex color (#RRGGBB)
     * @param {number} alpha - Alpha value (0-1)
     * @returns {string} rgba() string
     */
    hexToRgba(hex, alpha) {
        const r = parseInt(hex.slice(1, 3), 16);
        const g = parseInt(hex.slice(3, 5), 16);
        const b = parseInt(hex.slice(5, 7), 16);
        return `rgba(${r}, ${g}, ${b}, ${alpha})`;
    }

    /**
     * Toggles sensor ray visualization.
     */
    toggleSensorRays() {
        this.showSensorRays = !this.showSensorRays;
    }

    /**
     * Toggles vehicle ID labels.
     */
    toggleVehicleIds() {
        this.showVehicleIds = !this.showVehicleIds;
    }
}
