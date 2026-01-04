/**
 * REST API client for simulation control.
 */
class ApiClient {
    constructor(baseUrl = '') {
        this.baseUrl = baseUrl;
    }

    /**
     * Creates a new simulation.
     * @param {string} name - Simulation name
     * @param {Object} arena - Arena configuration
     * @returns {Promise<Object>} Created simulation
     */
    async createSimulation(name, arena) {
        const response = await fetch(`${this.baseUrl}/api/simulation`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ name, arena })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to create simulation');
        }

        return response.json();
    }

    /**
     * Lists all simulations.
     * @returns {Promise<Object>} Map of simulations
     */
    async listSimulations() {
        const response = await fetch(`${this.baseUrl}/api/simulation`);

        if (!response.ok) {
            throw new Error('Failed to list simulations');
        }

        return response.json();
    }

    /**
     * Gets a simulation by ID.
     * @param {string} id - Simulation ID
     * @returns {Promise<Object>} Simulation details
     */
    async getSimulation(id) {
        const response = await fetch(`${this.baseUrl}/api/simulation/${id}`);

        if (!response.ok) {
            throw new Error('Failed to get simulation');
        }

        return response.json();
    }

    /**
     * Starts a simulation.
     * @param {string} id - Simulation ID
     * @returns {Promise<Object>} Result
     */
    async startSimulation(id) {
        const response = await fetch(`${this.baseUrl}/api/simulation/${id}/start`, {
            method: 'POST'
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to start simulation');
        }

        return response.json();
    }

    /**
     * Stops a simulation.
     * @param {string} id - Simulation ID
     * @returns {Promise<Object>} Result
     */
    async stopSimulation(id) {
        const response = await fetch(`${this.baseUrl}/api/simulation/${id}/stop`, {
            method: 'POST'
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to stop simulation');
        }

        return response.json();
    }

    /**
     * Steps a simulation (single tick).
     * @param {string} id - Simulation ID
     * @returns {Promise<Object>} Result
     */
    async stepSimulation(id) {
        const response = await fetch(`${this.baseUrl}/api/simulation/${id}/step`, {
            method: 'POST'
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to step simulation');
        }

        return response.json();
    }

    /**
     * Resets a simulation.
     * @param {string} id - Simulation ID
     * @returns {Promise<Object>} Result
     */
    async resetSimulation(id) {
        const response = await fetch(`${this.baseUrl}/api/simulation/${id}/reset`, {
            method: 'POST'
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to reset simulation');
        }

        return response.json();
    }

    /**
     * Deletes a simulation.
     * @param {string} id - Simulation ID
     * @returns {Promise<Object>} Result
     */
    async deleteSimulation(id) {
        const response = await fetch(`${this.baseUrl}/api/simulation/${id}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to delete simulation');
        }

        return response.json();
    }

    /**
     * Gets simulation status.
     * @param {string} id - Simulation ID
     * @returns {Promise<Object>} Status
     */
    async getStatus(id) {
        const response = await fetch(`${this.baseUrl}/api/simulation/${id}/status`);

        if (!response.ok) {
            throw new Error('Failed to get status');
        }

        return response.json();
    }

    /**
     * Adds a vehicle to a simulation.
     * @param {string} simulationId - Simulation ID
     * @param {string} speciesId - Species ID
     * @param {number} x - X position
     * @param {number} y - Y position
     * @param {number} angle - Angle in radians
     * @returns {Promise<Object>} Created vehicle
     */
    async addVehicle(simulationId, speciesId, x, y, angle) {
        const response = await fetch(`${this.baseUrl}/api/simulation/${simulationId}/vehicles`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ speciesId, x, y, angle })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to add vehicle');
        }

        return response.json();
    }

    /**
     * Adds a static object to a simulation.
     * @param {string} simulationId - Simulation ID
     * @param {Object} object - Object configuration
     * @returns {Promise<Object>} Created object
     */
    async addStaticObject(simulationId, object) {
        const response = await fetch(`${this.baseUrl}/api/simulation/${simulationId}/objects`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(object)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to add object');
        }

        return response.json();
    }
}
