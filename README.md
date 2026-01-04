# Vehicle Simulation

A neural network-controlled vehicle simulation with Java backend and JavaScript frontend.

## Overview

This simulation demonstrates emergent behaviors in autonomous vehicles controlled by simple neural networks. Vehicles use light sensors (receptors) to perceive their environment and neural networks to control their motors, producing complex behaviors from simple rules.

## Architecture

- **Backend**: Java with embedded Jetty server, WebSocket for real-time updates, REST API for control
- **Frontend**: JavaScript with HTML5 Canvas for rendering
- **Communication**: WebSocket for simulation state streaming, REST for configuration
- **Multi-Simulation**: Run multiple independent simulations concurrently
- **Persistence**: Save/load simulations and reusable species packages

## Prerequisites

- Java 11 or higher
- Maven 3.6+

## Building

```bash
mvn clean package
```

This creates `target/vehicles.jar` (executable JAR with embedded Jetty)

## Running

```bash
java -jar target/vehicles.jar
```

The application will:
1. Start embedded Jetty server on port 8080
2. Load base package with 6 classic Braitenberg species
3. Restore previously saved simulations (if any)
4. Serve frontend at `http://localhost:8080`
5. Expose REST API at `/api/*`
6. Provide WebSocket endpoint at `/ws`

Open your browser to `http://localhost:8080`

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

## Features

### Simulation
- **Multi-simulation support**: Run multiple independent simulations concurrently
- **Multi-threaded engine**: Configurable thread pools for parallel vehicle processing
- **Real-time visualization**: WebSocket streaming to browser
- **Persistence**: Save/load simulations and resume where you left off

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
