/**
 * WebSocket client for real-time simulation updates.
 */
class WebSocketClient {
    constructor() {
        this.ws = null;
        this.subscribedSimulationId = null;
        this.stateUpdateCallback = null;
        this.connectionCallback = null;
        this.errorCallback = null;
        this.reconnectInterval = null;
        this.reconnectDelay = 3000;
        this.shouldReconnect = false;
    }

    /**
     * Connects to the WebSocket server.
     * @param {string} url - WebSocket URL (ws://localhost:8080/ws)
     */
    connect(url) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            console.log('WebSocket already connected');
            return;
        }

        this.shouldReconnect = true;
        this.ws = new WebSocket(url);

        this.ws.onopen = () => {
            console.log('WebSocket connected');

            // Clear reconnect interval if it exists
            if (this.reconnectInterval) {
                clearInterval(this.reconnectInterval);
                this.reconnectInterval = null;
            }

            // Notify connection callback
            if (this.connectionCallback) {
                this.connectionCallback(true);
            }

            // Re-subscribe if we were subscribed before
            if (this.subscribedSimulationId) {
                this.subscribe(this.subscribedSimulationId);
            }
        };

        this.ws.onmessage = (event) => {
            try {
                const message = JSON.parse(event.data);
                this.handleMessage(message);
            } catch (error) {
                console.error('Error parsing WebSocket message:', error);
            }
        };

        this.ws.onerror = (error) => {
            console.error('WebSocket error:', error);
            if (this.errorCallback) {
                this.errorCallback(error);
            }
        };

        this.ws.onclose = () => {
            console.log('WebSocket disconnected');

            if (this.connectionCallback) {
                this.connectionCallback(false);
            }

            // Attempt to reconnect
            if (this.shouldReconnect && !this.reconnectInterval) {
                console.log(`Reconnecting in ${this.reconnectDelay}ms...`);
                this.reconnectInterval = setInterval(() => {
                    console.log('Attempting to reconnect...');
                    this.connect(url);
                }, this.reconnectDelay);
            }
        };
    }

    /**
     * Disconnects from the WebSocket server.
     */
    disconnect() {
        this.shouldReconnect = false;

        if (this.reconnectInterval) {
            clearInterval(this.reconnectInterval);
            this.reconnectInterval = null;
        }

        if (this.ws) {
            this.ws.close();
            this.ws = null;
        }

        this.subscribedSimulationId = null;
    }

    /**
     * Subscribes to a simulation's state updates.
     * @param {string} simulationId - Simulation ID
     */
    subscribe(simulationId) {
        if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
            console.error('WebSocket not connected');
            return;
        }

        const message = {
            type: 'subscribe',
            simulationId: simulationId
        };

        this.ws.send(JSON.stringify(message));
        this.subscribedSimulationId = simulationId;
        console.log(`Subscribed to simulation: ${simulationId}`);
    }

    /**
     * Handles incoming WebSocket messages.
     * @param {Object} message - Parsed message
     */
    handleMessage(message) {
        switch (message.type) {
            case 'subscribed':
                console.log(`Successfully subscribed to simulation: ${message.simulationId}`);
                break;

            case 'state_update':
                if (this.stateUpdateCallback) {
                    this.stateUpdateCallback(message);
                }
                break;

            case 'error':
                console.error('Server error:', message.message);
                if (this.errorCallback) {
                    this.errorCallback(new Error(message.message));
                }
                break;

            default:
                console.warn('Unknown message type:', message.type);
        }
    }

    /**
     * Sets the callback for state updates.
     * @param {Function} callback - Callback function (receives state update message)
     */
    onStateUpdate(callback) {
        this.stateUpdateCallback = callback;
    }

    /**
     * Sets the callback for connection status changes.
     * @param {Function} callback - Callback function (receives boolean connected status)
     */
    onConnection(callback) {
        this.connectionCallback = callback;
    }

    /**
     * Sets the callback for errors.
     * @param {Function} callback - Callback function (receives Error)
     */
    onError(callback) {
        this.errorCallback = callback;
    }

    /**
     * Returns true if WebSocket is connected.
     * @returns {boolean} Connection status
     */
    isConnected() {
        return this.ws && this.ws.readyState === WebSocket.OPEN;
    }
}
