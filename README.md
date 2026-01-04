# Vehicle Simulation

A neural network-controlled vehicle simulation with Java backend and JavaScript frontend.

## Overview

This simulation demonstrates emergent behaviors in autonomous vehicles controlled by simple neural networks. Vehicles use light sensors (receptors) to perceive their environment and neural networks to control their motors, producing complex behaviors from simple rules.

## Architecture

- **Backend**: Java servlets with Maven, WebSocket for real-time updates, REST API for control
- **Frontend**: JavaScript with HTML5 Canvas for rendering
- **Communication**: WebSocket for simulation state streaming, REST for configuration

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Apache Tomcat 9.x or 10.x (or any servlet container)

## Building

```bash
mvn clean package
```

This creates `target/vehicles.war`

## Running

### Option 1: Using Tomcat Maven Plugin
```bash
mvn tomcat7:run
```

Then open browser to `http://localhost:8080`

### Option 2: Deploy to Tomcat
```bash
cp target/vehicles.war $TOMCAT_HOME/webapps/
$TOMCAT_HOME/bin/catalina.sh run
```

Then open browser to `http://localhost:8080/vehicles`

## Project Structure

```
vehicles/
├── pom.xml                           # Maven configuration
├── src/main/java/                    # Java backend source
│   └── com/gerkenip/vehicles/
│       ├── model/                    # Business objects
│       ├── engine/                   # Simulation engine
│       ├── servlet/                  # HTTP endpoints
│       ├── websocket/                # WebSocket endpoint
│       ├── service/                  # Business logic
│       ├── repository/               # Data storage
│       └── util/                     # Utilities
├── src/main/resources/               # Configuration files
├── src/main/webapp/WEB-INF/          # Web application config
├── src/test/java/                    # Unit tests
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

- Multi-threaded simulation engine for handling thousands of vehicles
- Real-time visualization via WebSocket
- Configurable neural networks with excitatory and inhibitory connections
- Light-based sensing system
- Classic Braitenberg vehicle behaviors (attraction, avoidance, exploration)
- REST API for simulation control and configuration

## License

MIT
