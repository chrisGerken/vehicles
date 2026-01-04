# Vehicle Simulation

A neural network-controlled vehicle simulation with Java backend and JavaScript frontend.

## Overview

This simulation demonstrates emergent behaviors in autonomous vehicles controlled by simple neural networks. Vehicles use light sensors (receptors) to perceive their environment and neural networks to control their motors, producing complex behaviors from simple rules.

**Implementation Status:** ✅ **COMPLETE** - All core components implemented and ready for testing.

## Architecture

- **Backend**: Java 11+ with embedded Jetty 11.0.18 server
- **Frontend**: Vanilla JavaScript with HTML5 Canvas rendering
- **Communication**: WebSocket for real-time state streaming, REST API for control
- **Neural Networks**: Wave-based synchronous evaluation with feedback loops
- **Physics**: Differential drive kinematics with configurable collision detection
- **Species Package**: 6 classic Braitenberg vehicles included

## Prerequisites

- Java 11 or higher
- Maven 3.6+

## Building

```bash
mvn clean package
```

This creates `target/vehicles.jar` (executable JAR with all dependencies via Maven Shade plugin)

## Running

```bash
java -jar target/vehicles.jar
```

The application will:
1. Start embedded Jetty server on port 8080
2. Serve frontend at `http://localhost:8080`
3. Expose REST API at `/api/simulation/*`
4. Provide WebSocket endpoint at `/ws`

Open your browser to `http://localhost:8080`

**Note:** The base species package (`src/main/resources/packages/base.vsp`) contains all 6 Braitenberg species, but to use them you'll need to load the package programmatically and add vehicles to your simulation via the API.

## Project Structure

```
vehicles/
├── pom.xml                           # Maven configuration
├── src/main/java/                    # Java backend source
│   └── com/gerkenip/vehicles/
│       ├── Application.java          # Main entry point (starts Jetty)
│       ├── model/                    # Business objects
│       ├── engine/                   # Simulation engine
│       ├── servlet/                  # HTTP endpoints
│       ├── websocket/                # WebSocket endpoint
│       ├── service/                  # Business logic
│       ├── repository/               # Data storage
│       ├── persistence/              # Save/load functionality
│       └── util/                     # Utilities
├── src/main/resources/               # Configuration files
│   ├── logback.xml                   # Logging configuration
│   └── packages/                     # Base species packages
│       └── base.vsp                  # Base Braitenberg species
├── src/test/java/                    # Unit tests
├── data/                             # Runtime data (created at runtime)
│   ├── simulations/                  # Saved simulation files
│   └── packages/                     # User-created packages
└── frontend/                         # JavaScript frontend
    ├── index.html
    ├── css/
    └── js/
```

## Design Documentation

See [DESIGN.md](DESIGN.md) for detailed design documentation including:
- Business object models
- System architecture
- API specifications
- Physics and neural network algorithms
- Example species (Braitenberg vehicles)

## Implementation Details

### What's Implemented

**Phase 1-3: Core Engine (✅ Complete)**
- All business object models (Arena, Vehicle, Species, Receptor, Neurode, Connection, etc.)
- Wave-based neural network evaluator with support for cycles and feedback loops
- Differential drive physics engine with realistic vehicle kinematics
- Capacitor-based receptor sensing with light accumulation
- Complete simulation engine orchestration
- Collision detection and handling (NONE, BREAK, BOUNCE modes)

**Phase 4-5: Web Layer (✅ Complete)**
- Embedded Jetty 11 server with programmatic servlet registration
- SimulationServlet with REST API endpoints
- WebSocket support for real-time state updates
- SimulationService with thread-safe ConcurrentHashMap storage
- JSON serialization/deserialization with Gson
- Exception handling with proper HTTP status codes

**Phase 6: Frontend (✅ Complete)**
- API client wrapper for REST endpoints
- WebSocket client with auto-reconnect
- Canvas-based renderer with coordinate transformation
- Visual effects (vehicle rendering, light sources with glow, sensor FOV visualization)
- Control panel UI integration (start/stop/step/reset)
- Main application controller

**Phase 7: Species Package (✅ Complete)**
- Base Braitenberg species package (base.vsp) with 6 species:
  1. **Phototrope** - Approaches light (crossed excitatory)
  2. **Photophobe** - Flees from light (parallel excitatory)
  3. **Explorer** - Avoids obstacles (bias + inhibitory)
  4. **Aggressive** - Charges targets (crossed inhibitory)
  5. **Coward** - Flees from behind (rear sensors)
  6. **Paranoid** - Avoids all threats (4-direction sensors)
- PackageBuilder utility for programmatic species creation

**API Endpoints Implemented:**
- `POST /api/simulation` - Create simulation
- `GET /api/simulation` - List all simulations
- `GET /api/simulation/{id}` - Get simulation details
- `POST /api/simulation/{id}/start` - Start simulation
- `POST /api/simulation/{id}/stop` - Stop simulation
- `POST /api/simulation/{id}/step` - Execute single tick
- `POST /api/simulation/{id}/reset` - Reset simulation
- `DELETE /api/simulation/{id}` - Delete simulation
- `GET /api/simulation/{id}/status` - Get status
- `POST /api/simulation/{id}/vehicles` - Add vehicle
- `POST /api/simulation/{id}/objects` - Add static object (POINT or WALL)
- WebSocket at `/ws` - Real-time state updates

### What's Not Yet Implemented

The following features from design.md are **not implemented** in this minimal viable version:
- Multi-simulation management (SimulationManager for concurrent simulations)
- Persistence layer (save/load simulations to files)
- Package management REST API (loading .vsp files via API)
- Advanced collision behavior configuration
- Thread pool configuration per simulation
- Statistics and metrics tracking
- Additional servlets (PackageServlet, ColorServlet)
- Repository layer for data storage
- Integration tests

## Features

### Simulation (Core Features Implemented)
- **Single simulation support**: One simulation instance per server run
- **Single-threaded engine**: Sequential vehicle processing (thread pools designed but not active)
- **Real-time visualization**: WebSocket streaming to browser ✅
- **Manual setup**: Simulations created and configured via API

### Neural Networks
- **Wave-based evaluation**: Signals propagate through network in discrete waves
- **Integer threshold neurodes**: Count excitatory connections to determine firing
- **Feedback loops**: Natural support for cycles and self-connections
- **Excitatory and inhibitory connections**: Complex behaviors from simple rules

### Sensing
- **Capacitor receptors**: Accumulate light over time until threshold reached
- **Color filtering**: Receptors only detect specific colors
- **Angular detection**: Configurable field of view per receptor
- **Distance-based attenuation**: Closer objects contribute more light

### Vehicles & Species
- **Reusable species packages**: Share and reuse vehicle designs
- **Base Braitenberg species**: 6 classic behaviors included (phototrope, photophobe, explorer, aggressive, etc.)
- **Differential drive physics**: Realistic two-motor vehicle dynamics
- **Per-vehicle or per-species colors**: Flexible color assignment

### Collision Detection
- **Configurable per-color**: Different collision behaviors for different colors
- **Multiple modes**: NONE (pass through), BREAK (destroy), BOUNCE (reflect)
- **Wall support**: Static objects can be points or line segments

### API & Integration
- **REST API**: Complete control over simulations, species, vehicles, objects
- **WebSocket streaming**: Real-time state updates
- **JSON configuration**: Human-readable, version-controllable simulation files
- **Embedded Jetty**: No external server required

## License

MIT
