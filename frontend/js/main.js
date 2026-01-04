/**
 * Main application entry point.
 * Initializes API client, WebSocket, renderer, and UI controls.
 */

let apiClient;
let wsClient;
let renderer;
let currentSimulationId = null;
let isRunning = false;

// Initialize on page load
document.addEventListener('DOMContentLoaded', async () => {
    console.log('Initializing Vehicle Simulation...');

    // Initialize components
    apiClient = new ApiClient();
    wsClient = new WebSocketClient();

    const canvas = document.getElementById('simulationCanvas');
    renderer = new Renderer(canvas);

    // Setup WebSocket callbacks
    wsClient.onStateUpdate((message) => {
        renderer.render({
            tick: message.tick,
            vehicles: message.vehicles,
            staticObjects: message.staticObjects
        });
    });

    wsClient.onConnection((connected) => {
        updateConnectionStatus(connected);
    });

    wsClient.onError((error) => {
        console.error('WebSocket error:', error);
        showError('WebSocket error: ' + error.message);
    });

    // Connect WebSocket
    const wsUrl = `ws://${window.location.host}/ws`;
    wsClient.connect(wsUrl);

    // Setup UI event listeners
    setupEventListeners();

    // Try to create default simulation
    try {
        await createDefaultSimulation();
    } catch (error) {
        console.error('Error creating default simulation:', error);
        showError('Failed to create simulation: ' + error.message);
    }
});

/**
 * Sets up event listeners for UI controls.
 */
function setupEventListeners() {
    // Control buttons
    document.getElementById('btnStart').addEventListener('click', handleStart);
    document.getElementById('btnStop').addEventListener('click', handleStop);
    document.getElementById('btnStep').addEventListener('click', handleStep);
    document.getElementById('btnReset').addEventListener('click', handleReset);

    // View options
    document.getElementById('chkSensorRays').addEventListener('change', (e) => {
        if (renderer) {
            renderer.showSensorRays = e.target.checked;
        }
    });

    document.getElementById('chkVehicleIds').addEventListener('change', (e) => {
        if (renderer) {
            renderer.showVehicleIds = e.target.checked;
        }
    });
}

/**
 * Creates a default simulation with sample content.
 */
async function createDefaultSimulation() {
    showStatus('Creating simulation...');

    // Create simulation
    const result = await apiClient.createSimulation('Demo Simulation', {
        width: 1000,
        height: 1000,
        wrapEastWest: false,
        wrapNorthSouth: false
    });

    currentSimulationId = result.id;
    console.log('Created simulation:', currentSimulationId);

    // Set arena dimensions in renderer
    renderer.setArenaDimensions(1000, 1000);

    // Subscribe to updates
    if (wsClient.isConnected()) {
        wsClient.subscribe(currentSimulationId);
    }

    // Add a light source in the center
    await apiClient.addStaticObject(currentSimulationId, {
        type: 'POINT',
        x: 500,
        y: 500,
        radius: 20,
        colorName: 'white',
        brightness: 1.0,
        emitsBrightness: true
    });

    showStatus('Simulation created. Add species via code to add vehicles.');
    updateControlButtons(false);
}

/**
 * Handles start button click.
 */
async function handleStart() {
    if (!currentSimulationId) {
        showError('No simulation loaded');
        return;
    }

    try {
        showStatus('Starting simulation...');
        await apiClient.startSimulation(currentSimulationId);
        isRunning = true;
        updateControlButtons(true);
        showStatus('Simulation running');
    } catch (error) {
        showError('Failed to start: ' + error.message);
    }
}

/**
 * Handles stop button click.
 */
async function handleStop() {
    if (!currentSimulationId) {
        showError('No simulation loaded');
        return;
    }

    try {
        showStatus('Stopping simulation...');
        await apiClient.stopSimulation(currentSimulationId);
        isRunning = false;
        updateControlButtons(false);
        showStatus('Simulation stopped');
    } catch (error) {
        showError('Failed to stop: ' + error.message);
    }
}

/**
 * Handles step button click.
 */
async function handleStep() {
    if (!currentSimulationId) {
        showError('No simulation loaded');
        return;
    }

    if (isRunning) {
        showError('Stop simulation before stepping');
        return;
    }

    try {
        await apiClient.stepSimulation(currentSimulationId);
        showStatus('Stepped 1 tick');
    } catch (error) {
        showError('Failed to step: ' + error.message);
    }
}

/**
 * Handles reset button click.
 */
async function handleReset() {
    if (!currentSimulationId) {
        showError('No simulation loaded');
        return;
    }

    if (isRunning) {
        showError('Stop simulation before resetting');
        return;
    }

    try {
        showStatus('Resetting simulation...');
        await apiClient.resetSimulation(currentSimulationId);
        showStatus('Simulation reset');
    } catch (error) {
        showError('Failed to reset: ' + error.message);
    }
}

/**
 * Updates control button states.
 * @param {boolean} running - Whether simulation is running
 */
function updateControlButtons(running) {
    document.getElementById('btnStart').disabled = running;
    document.getElementById('btnStop').disabled = !running;
    document.getElementById('btnStep').disabled = running;
    document.getElementById('btnReset').disabled = running;
}

/**
 * Updates WebSocket connection status indicator.
 * @param {boolean} connected - Connection status
 */
function updateConnectionStatus(connected) {
    const indicator = document.getElementById('connectionStatus');
    if (indicator) {
        indicator.textContent = connected ? 'Connected' : 'Disconnected';
        indicator.className = connected ? 'status-connected' : 'status-disconnected';
    }

    // Re-subscribe if reconnected
    if (connected && currentSimulationId) {
        wsClient.subscribe(currentSimulationId);
    }
}

/**
 * Shows status message.
 * @param {string} message - Status message
 */
function showStatus(message) {
    const statusEl = document.getElementById('statusMessage');
    if (statusEl) {
        statusEl.textContent = message;
        statusEl.className = 'status-message';
    }
    console.log('Status:', message);
}

/**
 * Shows error message.
 * @param {string} message - Error message
 */
function showError(message) {
    const statusEl = document.getElementById('statusMessage');
    if (statusEl) {
        statusEl.textContent = message;
        statusEl.className = 'status-message error';
    }
    console.error('Error:', message);
}
