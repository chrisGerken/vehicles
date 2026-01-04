# Vehicle Simulation Design Document

**Implementation Status:** ✅ Core engine and minimal viable product complete (January 2025)

## Implementation Summary

This design document describes the complete vision for the vehicle simulation system. The **current implementation** includes:

**✅ Fully Implemented:**
- All core business object models (Arena, Vehicle, Species, Receptor, Neurode, Connection, etc.)
- Wave-based neural network evaluation engine
- Differential drive physics with collision detection
- Embedded Jetty 11 server with REST API and WebSocket
- Frontend with Canvas rendering and controls
- 6 Braitenberg species in base package (base.vsp)
- Single simulation management via SimulationService

**⏳ Designed But Not Implemented:**
- Multi-simulation concurrent execution (SimulationManager)
- Persistence layer (save/load to .vsim files)
- Package management API (load/save .vsp files dynamically)
- Repository layer for data storage
- Thread pool per simulation
- Advanced metrics and monitoring

**File Count:**
- 36 Java source files (models, engine, servlets, utilities)
- 4 JavaScript files (api-client, websocket-client, renderer, main)
- 1 species package (base.vsp with 6 species)

**Key Implementation Files:**
- `Application.java` - Main entry point, starts Jetty
- `SimulationEngine.java` - Core tick orchestration
- `NeuralNetworkEvaluator.java` - Wave-based network evaluation
- `PhysicsEngine.java` - Differential drive kinematics
- `Receptor.java` - Capacitor-based light sensing
- `SimulationServlet.java` - REST API endpoints
- `SimulationWebSocket.java` - Real-time state broadcasting
- `frontend/js/renderer.js` - Canvas visualization

---

# Simulation Business Objects

## Arena
**Properties:**
- `width: double` - Width in simulation units
- `height: double` - Height in simulation units
- `wrapEastWest: boolean` - Whether east/west edges wrap (toroidal)
- `wrapNorthSouth: boolean` - Whether north/south edges wrap (toroidal)
- `backgroundColor: Color` - Background color for rendering

**Methods:**
- `normalizePosition(x, y): Point` - Wraps coordinates if wrapping enabled
- `getObjectsInRadius(x, y, radius): List<SimulationObject>` - Spatial query
- `checkCollision(vehicle): boolean` - Check if vehicle hits wall or object

## SimulationObject (Abstract)
**Properties:**
- `id: String` - Unique identifier
- `x: double` - X coordinate
- `y: double` - Y coordinate
- `colorName: String` - Reference to named color definition
- `brightness: double` - Brightness level (0.0 to 1.0)
- `radius: double` - Collision radius (0 for point sources)

**Methods:**
- `getBoundingBox(): Rectangle` - For collision detection
- `distanceTo(other: SimulationObject): double` - Distance calculation
- `getColor(): Color` - Resolves color name to RGB color

## StaticSimulationObject
**Inherits from:** SimulationObject

**Properties:**
- `type: ObjectType` - POINT or WALL
- `x2: double` - End X coordinate (for WALL type only)
- `y2: double` - End Y coordinate (for WALL type only)
- `emitsBrightness: boolean` - Whether it's a light source

**Types:**
- `POINT`: Point light source at (x, y) with specified radius (can be 0)
- `WALL`: Line segment from (x, y) to (x2, y2)

**Additional Notes:**
- Immutable position after creation
- Can be used as obstacles or light sources for receptors
- Point sources emit brightness; walls typically used as obstacles
- Walls can be packaged and reused across simulations

## Vehicle
**Inherits from:** SimulationObject

**Properties:**
- `angle: double` - Heading in radians (0 to 2π)
- `leftMotorSpeed: double` - Left wheel speed (-1.0 to 1.0)
- `rightMotorSpeed: double` - Right wheel speed (-1.0 to 1.0)
- `wheelBase: double` - Distance between left and right wheels
- `maxSpeed: double` - Maximum forward speed
- `species: Species` - Reference to species definition
- `colorName: String` - Vehicle-specific color (optional, overrides species color)
- `receptors: List<Receptor>` - Sensor array (defined by species)
- `neuralNetwork: NeuralNetworkInstance` - Instance of species neural network

**Methods:**
- `updateMotorSpeeds()` - Called after neural network evaluation
- `updatePosition(deltaTime)` - Physics integration
- `sense(): Map<Receptor, double>` - Read all receptor values
- `getEffectiveColor(): String` - Returns vehicle color if set, otherwise species color

**Additional Notes:**
- If species has a color, vehicle inherits it unless vehicle-specific color is set
- If species has no color, vehicle MUST have its own color specified

## Species
**Properties:**
- `id: String` - Unique identifier
- `name: String` - Human-readable name
- `colorName: String` - Default color for vehicles (optional)
- `receptorDefinitions: List<ReceptorDefinition>` - Template for receptors
- `neuralNetworkTemplate: NeuralNetworkTemplate` - Graph structure
- `vehicleRadius: double` - Size of vehicles of this species
- `wheelBase: double` - Wheel separation
- `maxSpeed: double` - Maximum speed
- `sourcePackage: String` - Reference to reusable package (optional)
- `editable: boolean` - Whether this species can be modified (false for package references)

**Methods:**
- `createVehicle(x, y, angle): Vehicle` - Instantiate a new vehicle
- `createVehicle(x, y, angle, colorName): Vehicle` - Instantiate with specific color
- `clone(): Species` - For mutations/variations

**Additional Notes:**
- Species can be defined inline in a simulation or referenced from a reusable package
- If colorName is not set, each vehicle must specify its own color
- Package-referenced species are read-only; clone to create editable variant

## Receptor
**Properties:**
- `id: String` - Unique identifier
- `angleFrom: double` - Start angle relative to vehicle heading (0 to 2π)
- `angleTo: double` - End angle relative to vehicle heading (0 to 2π)
- `maxRange: double` - Maximum detection distance
- `sensitivity: double` - Sensitivity multiplier
- `colorFilter: String` - Only detects objects of this color (named color)
- `accumulatedLight: double` - Current accumulated light (capacitor charge)
- `threshold: double` - Light threshold for firing
- `willFireNextTick: boolean` - Whether receptor will fire on next clock tick

**Methods:**
- `accumulateLight(vehicle, arena): void` - Accumulates light from matching-color objects
- `checkThreshold(): void` - Checks if threshold exceeded, sets willFireNextTick, reduces charge
- `reset(): void` - Clears fire state for new tick

**Notes:**
- Detects brightness only from objects matching colorFilter
- Accumulates light: closer objects contribute more (brightness × (1 - distance/maxRange) × sensitivity)
- When accumulatedLight >= threshold: willFireNextTick = true, accumulatedLight -= threshold
- Fires once per threshold crossing, then charge reduced by threshold amount
- Acts as capacitor, accumulating light over time until threshold is reached

## Neurode
**Properties:**
- `id: String` - Unique identifier within network
- `type: NeurodeType` - INPUT, HIDDEN, OUTPUT
- `threshold: int` - Number of firing excitatory connections required to fire
- `firedPreviousTick: boolean` - State from previous clock tick
- `willFireNextTick: boolean` - Computed state for next clock tick
- `inputConnections: List<Connection>` - Incoming connections
- `outputConnections: List<Connection>` - Outgoing connections

**Methods:**
- `evaluate(): void` - Computes willFireNextTick based on previous tick states
- `advanceTick(): void` - Moves willFireNextTick to firedPreviousTick for new tick

**Types:**
- `INPUT`: Connected to receptor or constant
- `HIDDEN`: Internal processing
- `OUTPUT`: Controls left/right motor

**Firing Logic:**
- Count how many EXCITER input connections fired on previous tick
- If count >= threshold AND no INHIBITOR connections fired on previous tick: willFireNextTick = true
- Otherwise: willFireNextTick = false
- No accumulation across ticks; each tick is independent evaluation

## Connection
**Properties:**
- `id: String` - Unique identifier
- `fromNeurode: String` - Source neurode ID
- `toNeurode: String` - Destination neurode ID
- `type: ConnectionType` - INHIBITOR or EXCITER
- `weight: double` - Connection strength (0.0 to 1.0)

**Types:**
- `INHIBITOR`: If source fires, prevents target from firing this tick
- `EXCITER`: If source fires, contributes to target activation

## ColorDefinition
**Properties:**
- `name: String` - Color name (e.g., "red", "blue", "phototrope-color")
- `r: int` - Red component (0-255)
- `g: int` - Green component (0-255)
- `b: int` - Blue component (0-255)

**Methods:**
- `toRGB(): Color` - Returns RGB color object
- `toHex(): String` - Returns hex color string (#RRGGBB)

**Additional Notes:**
- Color definitions can be shared across simulations and packages
- Named colors allow easy reference and consistent theming

## Simulation
**Properties:**
- `id: String` - Unique simulation identifier
- `name: String` - Human-readable simulation name
- `arena: Arena` - The simulation arena
- `colorDefinitions: Map<String, ColorDefinition>` - Named colors for this simulation
- `species: List<Species>` - Species defined in or referenced by this simulation
- `vehicles: List<Vehicle>` - Active vehicles
- `staticObjects: List<StaticSimulationObject>` - Static objects
- `collisionBehavior: CollisionBehavior` - Collision handling configuration
- `threadCount: int` - Number of threads for parallel vehicle processing
- `ticksPerSecond: int` - Target simulation speed
- `deltaTime: double` - Time per tick in simulation units
- `networkIterations: int` - Number of iterations for neural network evaluation
- `clock: Clock` - Simulation clock
- `running: boolean` - Whether simulation is currently running

**Methods:**
- `start(): void` - Start or resume simulation
- `stop(): void` - Pause simulation
- `step(): void` - Execute single tick
- `reset(): void` - Reset to initial state
- `addVehicle(vehicle): void` - Add vehicle to simulation
- `removeVehicle(id): void` - Remove vehicle from simulation
- `save(): void` - Persist simulation configuration to file

**Additional Notes:**
- Each simulation runs independently with its own thread pool
- Multiple simulations can run concurrently
- Simulations can reference species from reusable packages

## CollisionBehavior
**Properties:**
- `defaultBehavior: CollisionMode` - Default collision handling (NONE, BREAK, BOUNCE)
- `colorSpecificBehaviors: Map<String, CollisionMode>` - Per-color collision rules

**Collision Modes:**
- `NONE`: No collision detection; vehicles pass through objects (break at non-wrapping boundaries)
- `BREAK`: Vehicles break (stop/destroy) when colliding with objects
- `BOUNCE`: Vehicles bounce off objects

**Methods:**
- `getBehaviorForColor(colorName): CollisionMode` - Returns collision mode for specific color
- `setColorBehavior(colorName, mode): void` - Configure per-color collision

**Example:**
```
defaultBehavior: BOUNCE
colorSpecificBehaviors: {
  "blue": BREAK,     // Break blue lights
  "red": NONE        // Pass through red vehicles
}
```

## ReusablePackage
**Properties:**
- `id: String` - Unique package identifier
- `name: String` - Package name
- `version: String` - Package version
- `description: String` - Package description
- `colorDefinitions: Map<String, ColorDefinition>` - Named colors
- `species: List<Species>` - Species definitions
- `staticObjects: List<StaticSimulationObject>` - Reusable static object templates
- `filePath: String` - Path to package file

**Methods:**
- `save(): void` - Write package to file
- `load(filePath): ReusablePackage` - Load package from file
- `export(filePath): void` - Export package for sharing

**Additional Notes:**
- Base package includes 6 original Braitenberg species
- Packages are JSON files with .vsp (Vehicle Simulation Package) extension
- Simulations reference package species by package:species pattern
- Package contents are read-only; clone to create editable variants

## Clock
**Properties:**
- `currentTick: long` - Current simulation tick
- `ticksPerSecond: int` - Target simulation speed
- `deltaTime: double` - Time per tick in simulation units

**Methods:**
- `tick()` - Advance simulation by one step
- `reset()` - Reset to tick 0

## User Interface
- Graphical display of the arena
- Shows simulation with vehicles and static objects over time

---

# System Architecture

## Backend (Java)
**Responsibilities:**
- Multi-simulation management (multiple independent simulations)
- Simulation engine (core logic for each simulation)
- Physics calculations (vehicle movement, collisions)
- Neural network processing (neurode firing, signal propagation)
- Multi-threaded execution (configurable threads per simulation)
- State management and persistence
- WebSocket server for real-time updates
- REST API for configuration and control
- Package management (loading, saving, sharing species/objects)
- Embedded Jetty web server

**Key Components:**
- `Application`: Main entry point, starts/stops Jetty server
- `SimulationManager`: Manages multiple concurrent simulations
- `SimulationEngine`: Main orchestrator for single simulation, clock management
- `Arena`: World management, spatial indexing for collision detection
- `VehicleProcessor`: Thread pool for parallel vehicle updates (per simulation)
- `NeuralNetworkEvaluator`: Processes neurode graphs with wave-based evaluation
- `PhysicsEngine`: Movement, collision detection with configurable behavior
- `WebSocketServer`: Broadcasts simulation state to connected clients
- `APIController`: REST endpoints for control and configuration
- `PackageRepository`: Loads and manages reusable packages
- `PersistenceService`: Saves/loads simulations and packages

**Multi-Simulation Architecture:**
- Each simulation runs as independent long-running process
- Simulations have unique IDs used in all API calls
- Each simulation has its own thread pool (configurable size)
- SimulationManager coordinates multiple simulations
- WebSocket connections subscribe to specific simulation IDs
- Multiple browser tabs can run different simulations

**Concurrency Strategy:**
- Each clock tick processes vehicles in parallel using simulation's thread pool
- Vehicles read from shared state (arena, other objects)
- Write updates to local buffers, then merge at end of tick
- Lock-free or fine-grained locking for performance
- Thread count configurable per simulation (default: CPU cores)

## Frontend (JavaScript)
**Responsibilities:**
- Canvas/WebGL rendering of arena and objects
- User controls (play/pause, speed, step-through)
- Configuration UI (create species, set parameters)
- WebSocket client for receiving simulation updates
- Data visualization (charts, statistics)
- Export/save functionality

**Key Components:**
- `Renderer`: Canvas-based drawing of simulation state
- `WebSocketClient`: Receives and queues state updates
- `UIController`: Handles user interactions
- `ConfigurationPanel`: Species editor, simulation parameters
- `StatisticsPanel`: Charts and metrics

## Communication Protocol

### WebSocket (Real-time Updates)

**Client → Server (Subscribe):**
```json
{
  "type": "subscribe",
  "simulationId": "sim_abc123"
}
```

**Server → Client:**
```json
{
  "type": "state_update",
  "simulationId": "sim_abc123",
  "tick": 1234,
  "vehicles": [
    {"id": "v1", "x": 100.5, "y": 200.3, "angle": 1.57, "colorName": "red", "brightness": 0.8}
  ],
  "staticObjects": [
    {"id": "s1", "x": 50, "y": 50, "colorName": "blue", "brightness": 1.0, "type": "POINT"}
  ]
}
```

### REST API (Control & Configuration)

**Implementation Note:** The current implementation uses `/api/simulation/*` (singular) and implements a subset of the designed endpoints. See "API Endpoints Implemented" section in README.md for the complete list of working endpoints.

**Designed API (Full Vision):**

**Note:** All simulation-specific endpoints include `{simulationId}` path parameter

#### Simulation Management

**POST /api/simulations** (⏳ Designed endpoint pattern; implemented as `/api/simulation`)
- Creates a new simulation
- Body:
```json
{
  "name": "My Simulation",
  "arena": {"width": 1000, "height": 1000, "wrapEastWest": true, "wrapNorthSouth": false},
  "threadCount": 4,
  "ticksPerSecond": 30
}
```
- Response: `{"id": "sim_abc123", "name": "My Simulation"}`

**GET /api/simulations**
- Lists all simulations
- Response: `[{"id": "sim_1", "name": "My Simulation", "status": "running"}, ...]`

**GET /api/simulations/{simulationId}**
- Gets full simulation configuration
- Response: Complete simulation definition

**DELETE /api/simulations/{simulationId}**
- Deletes simulation (must be stopped)
- Response: `{"deleted": true}`

**POST /api/simulations/{simulationId}/save**
- Saves simulation configuration to file
- Body: `{"filePath": "/path/to/save.vsim"}` (optional)
- Response: `{"saved": true, "filePath": "/data/simulations/sim_abc123.vsim"}`

**POST /api/simulations/load**
- Loads simulation from file
- Body: `{"filePath": "/path/to/simulation.vsim"}`
- Response: `{"id": "sim_xyz789", "name": "Loaded Simulation"}`

#### Simulation Control

**POST /api/simulations/{simulationId}/start**
- Starts or resumes simulation
- Response: `{"status": "running", "tick": 123}`

**POST /api/simulations/{simulationId}/stop**
- Pauses simulation
- Response: `{"status": "paused", "tick": 123}`

**POST /api/simulations/{simulationId}/reset**
- Resets simulation to initial state
- Body: `{"keepEntities": false}` (optional)
- Response: `{"status": "reset", "tick": 0}`

**POST /api/simulations/{simulationId}/step**
- Advances simulation by one tick
- Response: `{"status": "paused", "tick": 124}`

**GET /api/simulations/{simulationId}/status**
- Gets current simulation state
- Response:
```json
{
  "status": "running",
  "tick": 12345,
  "vehicleCount": 100,
  "ticksPerSecond": 30,
  "actualTPS": 29.8,
  "threadCount": 4
}
```

**PUT /api/simulations/{simulationId}/threadCount**
- Updates thread count (can be done while running)
- Body: `{"threadCount": 8}`
- Response: `{"threadCount": 8}`

#### Arena Configuration

**GET /api/simulations/{simulationId}/arena**
- Gets arena configuration
- Response:
```json
{
  "width": 1000,
  "height": 1000,
  "wrapEastWest": true,
  "wrapNorthSouth": false,
  "backgroundColor": "#000000"
}
```

**PUT /api/simulations/{simulationId}/arena**
- Updates arena configuration (requires stopped simulation)
- Body: Same format as GET response

#### Color Definitions

**POST /api/simulations/{simulationId}/colors**
- Creates a named color
- Body: `{"name": "red", "r": 255, "g": 0, "b": 0}`
- Response: `{"name": "red", "hex": "#FF0000"}`

**GET /api/simulations/{simulationId}/colors**
- Lists all color definitions
- Response: `[{"name": "red", "r": 255, "g": 0, "b": 0}, ...]`

**DELETE /api/simulations/{simulationId}/colors/{colorName}**
- Deletes color definition (fails if in use)
- Response: `{"deleted": true}`

#### Species Management

**POST /api/simulations/{simulationId}/species**
- Creates a new species
- Body:
```json
{
  "name": "Phototrope",
  "colorName": "red",
  "vehicleRadius": 5,
  "wheelBase": 8,
  "maxSpeed": 50,
  "receptors": [
    {"id": "left", "angleFrom": 0.785, "angleTo": 1.57, "maxRange": 200, "sensitivity": 1.0, "colorFilter": "white", "threshold": 10.0},
    {"id": "right", "angleFrom": -1.57, "angleTo": -0.785, "maxRange": 200, "sensitivity": 1.0, "colorFilter": "white", "threshold": 10.0}
  ],
  "neuralNetwork": {
    "neurodes": [
      {"id": "input_left", "type": "INPUT", "threshold": 1},
      {"id": "input_right", "type": "INPUT", "threshold": 1},
      {"id": "output_left_motor", "type": "OUTPUT", "threshold": 1},
      {"id": "output_right_motor", "type": "OUTPUT", "threshold": 1}
    ],
    "connections": [
      {"from": "input_left", "to": "output_right_motor", "type": "EXCITER", "weight": 1.0},
      {"from": "input_right", "to": "output_left_motor", "type": "EXCITER", "weight": 1.0}
    ]
  }
}
```
- Response: `{"id": "species_abc123", "name": "Phototrope"}`

**POST /api/simulations/{simulationId}/species/reference**
- References species from package
- Body: `{"packageId": "base", "speciesId": "phototrope"}`
- Response: `{"id": "species_ref_123", "name": "Phototrope", "sourcePackage": "base"}`

**GET /api/simulations/{simulationId}/species**
- Lists all species (both defined and referenced)
- Response: `[{"id": "species_1", "name": "Phototrope", "sourcePackage": "base"}, ...]`

**GET /api/simulations/{simulationId}/species/{id}**
- Gets species details
- Response: Full species definition

**PUT /api/simulations/{simulationId}/species/{id}**
- Updates species (only for non-package species)
- Body: Same format as POST
- Response: Updated species

**DELETE /api/simulations/{simulationId}/species/{id}**
- Deletes species (fails if vehicles exist or if package reference)
- Response: `{"deleted": true}`

#### Vehicle Management

**POST /api/simulations/{simulationId}/vehicles**
- Adds vehicle(s) to simulation
- Body:
```json
{
  "speciesId": "species_abc123",
  "vehicles": [
    {"x": 100, "y": 100, "angle": 0},
    {"x": 200, "y": 200, "angle": 1.57, "colorName": "blue"}
  ]
}
```
- Response: `{"created": ["v_1", "v_2"]}`
- Note: colorName is optional if species has a color

**GET /api/simulations/{simulationId}/vehicles**
- Lists all vehicles (lightweight)
- Response:
```json
[
  {"id": "v_1", "speciesId": "species_abc123", "x": 105.3, "y": 102.1, "angle": 0.1, "colorName": "red"},
  ...
]
```

**GET /api/simulations/{simulationId}/vehicles/{id}**
- Gets detailed vehicle state including neural network state
- Response:
```json
{
  "id": "v_1",
  "speciesId": "species_abc123",
  "x": 105.3,
  "y": 102.1,
  "angle": 0.1,
  "colorName": "red",
  "leftMotorSpeed": 0.8,
  "rightMotorSpeed": 0.9,
  "receptorStates": {
    "left": {"accumulatedLight": 15.3, "willFireNextTick": true},
    "right": {"accumulatedLight": 8.7, "willFireNextTick": false}
  },
  "neurodeStates": {
    "input_left": {"firedPreviousTick": true, "willFireNextTick": true},
    "output_right_motor": {"firedPreviousTick": true, "willFireNextTick": false}
  }
}
```

**DELETE /api/simulations/{simulationId}/vehicles/{id}**
- Removes vehicle from simulation
- Response: `{"deleted": true}`

#### Static Objects

**POST /api/simulations/{simulationId}/objects**
- Adds static object to arena
- Body (Point):
```json
{
  "type": "POINT",
  "x": 500,
  "y": 500,
  "radius": 20,
  "colorName": "white",
  "brightness": 1.0,
  "emitsBrightness": true
}
```
- Body (Wall):
```json
{
  "type": "WALL",
  "x": 100,
  "y": 100,
  "x2": 200,
  "y2": 100,
  "colorName": "gray",
  "brightness": 0.0,
  "emitsBrightness": false
}
```
- Response: `{"id": "obj_xyz789"}`

**POST /api/simulations/{simulationId}/objects/reference**
- References static object from package
- Body: `{"packageId": "base", "objectId": "light_source"}`
- Response: `{"id": "obj_ref_123", "sourcePackage": "base"}`

**GET /api/simulations/{simulationId}/objects**
- Lists all static objects
- Response: Array of objects

**DELETE /api/simulations/{simulationId}/objects/{id}**
- Removes static object
- Response: `{"deleted": true}`

#### Package Management

**POST /api/packages**
- Creates a new reusable package
- Body:
```json
{
  "name": "My Species Pack",
  "version": "1.0.0",
  "description": "Custom vehicle species"
}
```
- Response: `{"id": "pkg_abc123", "name": "My Species Pack"}`

**POST /api/packages/load**
- Loads package from file
- Body: `{"filePath": "/path/to/package.vsp"}`
- Response: `{"id": "pkg_xyz789", "name": "Loaded Package"}`

**GET /api/packages**
- Lists all loaded packages
- Response: `[{"id": "base", "name": "Base Package", "version": "1.0.0"}, ...]`

**GET /api/packages/{packageId}**
- Gets full package contents
- Response: Complete package definition with species and objects

**POST /api/packages/{packageId}/save**
- Saves package to file
- Body: `{"filePath": "/path/to/save.vsp"}` (optional)
- Response: `{"saved": true, "filePath": "/data/packages/pkg_abc123.vsp"}`

**POST /api/packages/{packageId}/species**
- Adds species to package
- Body: Same as species creation
- Response: `{"id": "species_xyz", "packageId": "pkg_abc123"}`

**POST /api/packages/{packageId}/objects**
- Adds static object template to package
- Body: Same as object creation
- Response: `{"id": "obj_xyz", "packageId": "pkg_abc123"}`

**POST /api/packages/{packageId}/colors**
- Adds color definition to package
- Body: `{"name": "custom-red", "r": 200, "g": 0, "b": 0}`
- Response: `{"name": "custom-red", "packageId": "pkg_abc123"}`

**DELETE /api/packages/{packageId}**
- Unloads package (fails if referenced by simulations)
- Response: `{"deleted": true}`

#### Collision Behavior

**GET /api/simulations/{simulationId}/collision-behavior**
- Gets collision configuration
- Response:
```json
{
  "defaultBehavior": "BOUNCE",
  "colorSpecificBehaviors": {
    "blue": "BREAK",
    "red": "NONE"
  }
}
```

**PUT /api/simulations/{simulationId}/collision-behavior**
- Updates collision configuration
- Body: Same format as GET response

---

# Technology Stack

## Backend (Java)

**Runtime:**
- Java 11 or higher (for var keyword, modern APIs)
- Embedded Jetty web server (no external server required)

**Build Tool:**
- Maven 3.6+
- Standard directory structure: `src/main/java`, `src/main/resources`, `src/test/java`
- Creates executable JAR with embedded Jetty

**Web Server:**
- Embedded Eclipse Jetty 11.x
- Started/stopped programmatically from Java application
- No WAR deployment needed

**Dependencies (pom.xml):**
```xml
<dependencies>
  <!-- Embedded Jetty Server -->
  <dependency>
    <groupId>org.eclipse.jetty</groupId>
    <artifactId>jetty-server</artifactId>
    <version>11.0.18</version>
  </dependency>

  <dependency>
    <groupId>org.eclipse.jetty</groupId>
    <artifactId>jetty-servlet</artifactId>
    <version>11.0.18</version>
  </dependency>

  <!-- Jetty WebSocket -->
  <dependency>
    <groupId>org.eclipse.jetty.websocket</groupId>
    <artifactId>websocket-jetty-server</artifactId>
    <version>11.0.18</version>
  </dependency>

  <!-- JSON Processing -->
  <dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
  </dependency>

  <!-- Logging -->
  <dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.9</version>
  </dependency>
  <dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.4.11</version>
  </dependency>

  <!-- Testing -->
  <dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.1</version>
    <scope>test</scope>
  </dependency>
</dependencies>

<build>
  <plugins>
    <!-- Create executable JAR -->
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-jar-plugin</artifactId>
      <version>3.3.0</version>
      <configuration>
        <archive>
          <manifest>
            <mainClass>com.gerkenip.vehicles.Application</mainClass>
          </manifest>
        </archive>
      </configuration>
    </plugin>

    <!-- Include dependencies -->
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-shade-plugin</artifactId>
      <version>3.5.1</version>
      <executions>
        <execution>
          <phase>package</phase>
          <goals>
            <goal>shade</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

**Key Libraries:**
- **Gson** - JSON serialization/deserialization
- **Jetty WebSocket** - For real-time communication
- **java.util.concurrent** - Thread pools, concurrent collections
- **SLF4J + Logback** - Logging

**Application Entry Point:**
```java
public class Application {
    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");

        // Register servlets
        context.addServlet(SimulationServlet.class, "/api/simulations/*");
        context.addServlet(PackageServlet.class, "/api/packages/*");

        // Configure WebSocket
        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
            wsContainer.addMapping("/ws", SimulationWebSocket.class);
        });

        // Serve static files
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setResourceBase("frontend");
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { resourceHandler, context });

        server.setHandler(handlers);
        server.start();
        server.join();
    }
}
```

## Frontend (JavaScript)

**Core:**
- Vanilla JavaScript (ES6+) or TypeScript
- HTML5 Canvas for rendering
- WebSocket API (native browser support)

**Optional Libraries:**
- **PixiJS** or **Three.js** - If you need advanced rendering (particles, effects)
- **Chart.js** - For statistics/metrics visualization
- No heavy frameworks required (React/Vue/Angular not needed)

**Build Tools:**
- None required (can serve static files directly)
- Optional: Webpack/Vite for bundling if using TypeScript or modules

**Browser Requirements:**
- Modern browsers with Canvas and WebSocket support
- Chrome, Firefox, Safari, Edge (all recent versions)

## Development Environment

**Recommended IDE:**
- IntelliJ IDEA (Community or Ultimate)
- Eclipse with Maven plugin
- VS Code with Java extensions

**Running Locally:**
```bash
# Build and run
mvn clean package
java -jar target/vehicles.jar

# The application will:
# 1. Start embedded Jetty server on port 8080
# 2. Load base package with default species
# 3. Restore previously saved simulations (if any)
# 4. Serve frontend from /frontend directory
# 5. Expose REST API at /api/*
# 6. Provide WebSocket endpoint at /ws

# Access at http://localhost:8080
```

---

# Project Structure

## Backend (Maven/Java)

**Implementation Note:** The actual implementation includes core components but omits repository, persistence, and some service layers. See below for what's actually implemented.

**Designed Project Structure:**

```
vehicles/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── gerkenip/
│   │   │           └── vehicles/
│   │   │               ├── Application.java          # ✅ Main entry point (starts Jetty)
│   │   │               │
│   │   │               ├── model/                    # ✅ Business objects (all implemented)
│   │   │               │   ├── Arena.java
│   │   │               │   ├── SimulationObject.java
│   │   │               │   ├── StaticSimulationObject.java
│   │   │               │   ├── Vehicle.java
│   │   │               │   ├── Species.java
│   │   │               │   ├── Receptor.java
│   │   │               │   ├── ReceptorDefinition.java
│   │   │               │   ├── Neurode.java
│   │   │               │   ├── NeuralNetwork.java
│   │   │               │   ├── Connection.java
│   │   │               │   ├── ColorDefinition.java
│   │   │               │   ├── Simulation.java
│   │   │               │   ├── CollisionBehavior.java
│   │   │               │   ├── NeurodeType.java (enum)
│   │   │               │   ├── ConnectionType.java (enum)
│   │   │               │   ├── CollisionMode.java (enum)
│   │   │               │   └── ObjectType.java (enum)
│   │   │               │
│   │   │               ├── engine/                   # ✅ Simulation engine (core implemented)
│   │   │               │   ├── SimulationEngine.java         # ✅
│   │   │               │   ├── PhysicsEngine.java            # ✅
│   │   │               │   ├── NeuralNetworkEvaluator.java   # ✅
│   │   │               │   ├── StateListener.java            # ✅
│   │   │               │   ├── SimulationState.java          # ✅
│   │   │               │   └── CollisionBehavior.java        # ✅
│   │   │               │
│   │   │               ├── servlet/                  # ⚠️ HTTP endpoints (partial)
│   │   │               │   ├── BaseServlet.java              # ✅
│   │   │               │   └── SimulationServlet.java        # ✅
│   │   │               │
│   │   │               ├── websocket/                # ✅ WebSocket endpoint
│   │   │               │   └── SimulationWebSocket.java
│   │   │               │
│   │   │               ├── service/                  # ⚠️ Business logic (partial)
│   │   │               │   └── SimulationService.java        # ✅
│   │   │               │
│   │   │               └── util/                     # ✅ Utilities (all implemented)
│   │   │                   ├── JsonUtil.java
│   │   │                   ├── MathUtil.java
│   │   │                   ├── ValidationUtil.java
│   │   │                   ├── IdGenerator.java
│   │   │                   ├── SimulationException.java
│   │   │                   ├── ValidationException.java
│   │   │                   ├── ResourceNotFoundException.java
│   │   │                   ├── SimulationStateException.java
│   │   │                   └── PackageBuilder.java
│   │   │
│   │   └── resources/
│   │       └── packages/                             # ✅ Base species packages
│   │           └── base.vsp                          # ✅ 6 Braitenberg species
│   │
│   └── test/                                         # ⏳ Unit tests (not implemented)
│       └── java/
│           └── com/
│               └── gerkenip/
│                   └── vehicles/
│                       ├── model/
│                       │   ├── VehicleTest.java
│                       │   └── ReceptorTest.java
│                       ├── engine/
│                       │   ├── PhysicsEngineTest.java
│                       │   └── NeuralNetworkEvaluatorTest.java
│                       └── util/
│                           └── MathUtilTest.java
│
├── data/                                             # Runtime data (created at runtime)
│   ├── simulations/                                  # Saved simulation files (.vsim)
│   └── packages/                                     # User-created packages (.vsp)
│
└── target/                                           # Build output (generated)
    └── vehicles.jar                                  # Executable JAR with embedded Jetty
```

## Frontend (JavaScript)

**Implementation Note:** Core rendering and controls implemented. Configuration panels and statistics not yet built.

```
frontend/
├── index.html                              # ✅ Main page (exists from skeleton)
├── css/
│   └── style.css                          # ✅ Styles (exists from skeleton)
├── js/
│   ├── main.js                            # ✅ Entry point with controls
│   ├── websocket-client.js                # ✅ WebSocket with auto-reconnect
│   ├── api-client.js                      # ✅ REST API wrapper
│   └── renderer.js                        # ✅ Canvas rendering with effects
└── assets/
    └── (UI assets from skeleton)
```

**Note:** No web.xml needed! Servlets and WebSockets are registered programmatically in Application.java using Jetty's API.

---

# Physics and Movement

## Vehicle Kinematics (Differential Drive)

The vehicle uses a **differential drive** model with two independently controlled front wheels:

**Given:**
- `vL` = left wheel velocity (motorSpeed × maxSpeed)
- `vR` = right wheel velocity (motorSpeed × maxSpeed)
- `L` = wheelBase (distance between left and right wheels)
- `dt` = deltaTime

**Calculations per tick:**
```
linearVelocity = (vL + vR) / 2
angularVelocity = (vR - vL) / L

angle = angle + angularVelocity × dt
x = x + linearVelocity × cos(angle) × dt
y = y + linearVelocity × sin(angle) × dt
```

**Behavior:**
- Both motors forward at same speed → straight line
- Left faster than right → turns right
- Right faster than left → turns left
- Motors opposite directions → spins in place
- Negative speeds → reverse

## Collision Detection

**Wall Collisions:**
- If wrapping disabled: vehicle breaks at boundary (regardless of collision mode)
- If wrapping enabled: position wraps to opposite side (toroidal arena)

**Object Collisions:**
- Collision behavior determined by CollisionBehavior configuration
- Can be configured globally (defaultBehavior) or per-color (colorSpecificBehaviors)
- Collision detection based on vehicle color vs object color

**Collision Modes:**

**NONE:**
- No collision detection; vehicles pass through objects
- Vehicles still break at non-wrapping arena boundaries

**BREAK:**
- When collision detected: vehicle stops permanently (breaks)
- Vehicle removed from simulation or marked as broken

**BOUNCE:**
- When collision detected: vehicle bounces off object
- Reverse direction: `angle = angle + π`
- Optional: reduce speed or reverse motors briefly

**Per-Color Configuration Example:**
```
defaultBehavior: BOUNCE
colorSpecificBehaviors: {
  "blue": BREAK,     // Break when hitting blue objects
  "red": NONE        // Pass through red vehicles
}
```

**Collision Detection for Walls:**
- Line segment (wall) to circle (vehicle) collision detection
- Calculate distance from vehicle center to line segment
- Collision if distance < vehicle radius

**Optimization:**
- Use spatial partitioning (grid or quadtree) for large simulations
- Only check nearby objects, not all N vehicles
- Pre-filter by color if collision mode is NONE for that color

## Receptor Sensing (Capacitor Model)

Each receptor acts as a **capacitor** that accumulates light over time and fires when a threshold is reached.

**Capacitor Algorithm (per tick):**
```
1. For each SimulationObject in arena:
   a. Check if object.colorName matches receptor.colorFilter
      - If no match, skip this object (color filtering)

   b. Calculate relative angle from vehicle to object
   c. Normalize to vehicle's reference frame (subtract vehicle.angle)
   d. Check if angle is within [receptor.angleFrom, receptor.angleTo]

   e. If yes, calculate distance
   f. If distance < receptor.maxRange:
      contribution = object.brightness × (1 - distance/maxRange) × receptor.sensitivity
      receptor.accumulatedLight += contribution

2. Check if threshold exceeded:
   if receptor.accumulatedLight >= receptor.threshold:
      receptor.willFireNextTick = true
      receptor.accumulatedLight -= receptor.threshold
   else:
      receptor.willFireNextTick = false

3. On next tick, receptor fires if willFireNextTick == true
```

**Key Properties:**
- **accumulatedLight**: Accumulates over multiple ticks (persists between ticks)
- **threshold**: Light level required to fire
- **colorFilter**: Only detects objects of matching color name
- **willFireNextTick**: Boolean flag for next tick's INPUT neurode

**Example:**
```
Receptor threshold: 20.0
Tick 1: Detect light +5.0 → accumulatedLight = 5.0 (no fire)
Tick 2: Detect light +7.0 → accumulatedLight = 12.0 (no fire)
Tick 3: Detect light +10.0 → accumulatedLight = 22.0 (≥ 20.0)
        → willFireNextTick = true
        → accumulatedLight = 22.0 - 20.0 = 2.0 (charge reduced by threshold)
Tick 4: Receptor fires (INPUT neurode = true)
        Detect light +3.0 → accumulatedLight = 5.0 (no fire this tick)
```

**Color Filtering:**
- Receptor only "sees" objects with colorName matching colorFilter
- Different receptors can detect different colors
- Example: left receptor detects "red", right receptor detects "blue"

**Optimization:**
- Use spatial queries to get only nearby objects
- Pre-filter by color before distance/angle checks
- Skip objects with non-matching colors entirely

## Constants and Units

**Simulation Units:**
- 1 unit = arbitrary distance (could represent 1 cm, 1 m, etc.)
- Angles in radians (0 to 2π)
- Time: deltaTime per tick (e.g., 0.1 units = 100ms)

**Suggested Defaults:**
- Arena: 1000 × 1000 units
- Vehicle radius: 5 units
- Wheel base: 8 units
- Max speed: 50 units/second
- Receptor max range: 200 units
- Delta time: 0.1 seconds per tick (10 ticks/second)

---

# Neural Network Evaluation

## Network Structure

Each vehicle has a neural network instance based on its species template:

**Input Neurodes:**
- One per receptor
- One for constant "always on" (bias)
- Set to true/false based on receptor threshold

**Output Neurodes:**
- `LEFT_MOTOR`: Controls left wheel speed
- `RIGHT_MOTOR`: Controls right wheel speed
- Firing = motor on (positive speed)
- Not firing = motor off (zero or negative speed)

**Hidden Neurodes:**
- Arbitrary graph structure
- Can form loops and cycles
- Provide computation and memory

## Evaluation Algorithm (Wave-Based Synchronous Model)

The network is evaluated once per clock tick using a **wave-based synchronous update** model where signals propagate through the network in discrete waves.

**Key Principle:** All neurodes use the **previous tick's firing states** to compute **next tick's firing states**. This creates synchronized waves of activation propagating through the network.

```
1. Read sensor inputs (happens at end of previous tick)
   - For each receptor:
     - receptor.accumulateLight(vehicle, arena)
     - receptor.checkThreshold()
     - receptor fires on NEXT tick if willFireNextTick == true

2. Advance tick (synchronous state transition)
   - For each neurode (including INPUTs from receptors):
     - neurode.firedPreviousTick = neurode.willFireNextTick
     - This creates the "wave" - all neurons update simultaneously

3. Evaluate network for next tick (all neurodes in parallel)
   - For each neurode (HIDDEN and OUTPUT types):
     a. Count firing EXCITER connections from previous tick:
        exciters = 0
        for each input connection c:
          if c.type == EXCITER AND c.fromNeurode.firedPreviousTick == true:
            exciters++

     b. Check for INHIBITOR veto:
        inhibited = false
        for each input connection c:
          if c.type == INHIBITOR AND c.fromNeurode.firedPreviousTick == true:
            inhibited = true
            break

     c. Determine if neurode will fire next tick:
        if inhibited:
          neurode.willFireNextTick = false
        else if exciters >= neurode.threshold:
          neurode.willFireNextTick = true
        else:
          neurode.willFireNextTick = false

4. Read output neurodes (use current tick's firing state)
   - leftMotorSpeed = output_left_motor.firedPreviousTick ? 1.0 : 0.0
   - rightMotorSpeed = output_right_motor.firedPreviousTick ? 1.0 : 0.0

5. Apply motor speeds to vehicle
   - vehicle.leftMotorSpeed = leftMotorSpeed
   - vehicle.rightMotorSpeed = rightMotorSpeed
```

**Wave Propagation Example:**
```
Network: receptor → hidden → output_motor

Tick 1:
  - Receptor accumulates light, exceeds threshold
  - receptor.willFireNextTick = true

Tick 2 (advance):
  - receptor.firedPreviousTick = true (WAVE 1)
  - hidden evaluates: sees receptor fired previous tick
  - hidden.willFireNextTick = true

Tick 3 (advance):
  - receptor.firedPreviousTick = false (light consumed)
  - hidden.firedPreviousTick = true (WAVE 2)
  - output_motor evaluates: sees hidden fired previous tick
  - output_motor.willFireNextTick = true

Tick 4 (advance):
  - hidden.firedPreviousTick = false
  - output_motor.firedPreviousTick = true (WAVE 3)
  - Motors activate!

Signal took 3 ticks to propagate from receptor to motor
```

## Handling Cycles and Loops

**Wave-Based Model Naturally Handles Cycles:**

The wave-based synchronous model handles cycles elegantly because:
- All neurodes update simultaneously using previous tick states
- No need for iteration loops or convergence detection
- Cycles create feedback with 1-tick delay per cycle
- Predictable, deterministic behavior

**Cycle Example:**
```
Network: A → B → C → A (3-neurode cycle)

Tick 1: A fires
Tick 2: B fires (saw A fire previous tick)
Tick 3: C fires (saw B fire previous tick)
Tick 4: A fires again (saw C fire previous tick) - cycle complete!

Signal circulates through cycle continuously with 3-tick period
```

**Self-Loop Example:**
```
Network: A → A (self-connection)

If A fires on tick N:
  - On tick N+1: A evaluates and sees itself fired on tick N
  - If threshold = 1 and connection is EXCITER: A fires again on tick N+1
  - Creates oscillation or sustained firing

This provides memory and temporal dynamics!
```

**Benefits:**
- No iteration limit needed
- Fully parallelizable (all neurodes evaluated independently)
- Predictable performance (O(1) per tick, not O(iterations))
- Natural feedback loops and memory

## Neurode Firing Mode

**Integer Threshold Counting:**
- Neurode counts how many EXCITER connections fired on previous tick
- If count >= threshold AND no INHIBITOR fired: neurode fires next tick
- No accumulation across ticks; each tick is independent evaluation
- Simple, deterministic, predictable

**Example:**
```
Neurode X has threshold = 2
Input connections:
  - A → X (EXCITER)
  - B → X (EXCITER)
  - C → X (EXCITER)
  - D → X (INHIBITOR)

Tick 1: A fires, B does not fire
  → exciters = 1 (< threshold 2) → X does not fire next tick

Tick 2: A fires, B fires, C does not fire
  → exciters = 2 (>= threshold 2) → X fires next tick

Tick 3: A fires, B fires, D fires
  → exciters = 2, but INHIBITOR fired → X does not fire next tick
```

## Neural Network Representation

**JSON Format (for API and storage):**
```json
{
  "species_id": "species_1",
  "neurodes": [
    {"id": "input_left", "type": "INPUT", "threshold": 1},
    {"id": "input_right", "type": "INPUT", "threshold": 1},
    {"id": "bias", "type": "INPUT", "threshold": 0},
    {"id": "hidden_1", "type": "HIDDEN", "threshold": 2},
    {"id": "output_left_motor", "type": "OUTPUT", "threshold": 1},
    {"id": "output_right_motor", "type": "OUTPUT", "threshold": 1}
  ],
  "connections": [
    {"id": "c1", "from": "input_left", "to": "output_right_motor", "type": "EXCITER", "weight": 1.0},
    {"id": "c2", "from": "input_right", "to": "output_left_motor", "type": "EXCITER", "weight": 1.0},
    {"id": "c3", "from": "bias", "to": "output_left_motor", "type": "EXCITER", "weight": 1.0},
    {"id": "c4", "from": "hidden_1", "to": "hidden_1", "type": "EXCITER", "weight": 1.0}
  ]
}
```

**Notes:**
- Thresholds are integers (count of excitatory connections)
- Weights are present but not currently used (reserved for future analog mode)
- INPUT neurodes for receptors have threshold >= 1 (typically 1)
- Bias INPUT has threshold 0 (always fires)

## Future Extensions

- **Analog outputs:** Use weighted sum instead of binary counting for motor control
- **Weighted thresholds:** Use connection weights in threshold calculation
- **Learning:** Adjust weights based on fitness/rewards (genetic algorithm, reinforcement learning)
- **Neurotransmitter types:** Multiple signal types beyond excite/inhibit
- **Plasticity:** Connections strengthen/weaken during simulation
- **Variable receptor decay:** Add decay rate to receptor capacitors

---

# Simulation Lifecycle

## Initialization

**1. Configuration Phase**
- User defines arena parameters (size, wrapping)
- User creates species (receptors, neural network)
- User places static objects (lights, obstacles)
- User spawns vehicles (position, angle, species)

**2. Validation**
- Verify neural networks have required INPUT/OUTPUT neurodes
- Check for valid connections (no dangling references)
- Ensure vehicles are within arena bounds

**3. Backend Initialization**
- Create thread pool for vehicle processing
- Initialize spatial index for collision detection
- Allocate network evaluation buffers
- Start WebSocket server

**4. Frontend Connection**
- Connect to WebSocket
- Subscribe to state updates
- Initialize canvas/renderer
- Load configuration into UI

## Execution Loop

Each tick follows this sequence:

```
while (simulation.running):
  1. Increment clock tick

  2. SENSE PHASE (parallelizable per vehicle)
     - For each vehicle:
       - Query arena for nearby objects
       - Evaluate each receptor
       - Update input neurodes

  3. THINK PHASE (parallelizable per vehicle)
     - For each vehicle:
       - Evaluate neural network (fixed iterations)
       - Read output neurodes
       - Set motor speeds

  4. ACT PHASE (parallelizable per vehicle)
     - For each vehicle:
       - Calculate new position/angle from motor speeds
       - Check for collisions
       - Update position (or handle collision)

  5. BROADCAST PHASE
     - Serialize current state to JSON
     - Send via WebSocket to all connected clients
     - (Optional: throttle to max FPS, e.g., 30 updates/sec)

  6. SLEEP/THROTTLE
     - Sleep to maintain target ticksPerSecond
     - Or run as fast as possible for batch mode
```

## Control Commands

**User Actions:**

**Start/Resume**
- Begin execution loop
- Resume from current tick

**Pause**
- Stop execution loop
- Maintain current state
- Allow inspection

**Step**
- Execute exactly one tick
- Useful for debugging

**Reset**
- Clear all vehicles
- Reset clock to 0
- Keep arena and species definitions
- Or: reload from initial configuration

**Speed Control**
- Adjust ticksPerSecond
- Set to 0 for "as fast as possible"
- Frontend may run slower if it can't keep up

**Add/Remove Entities**
- Dynamically add/remove vehicles
- Add/remove static objects
- Can be done while paused or running

## Persistence and Export

**Save Simulation State**
```json
{
  "version": "1.0",
  "tick": 12345,
  "arena": { ... },
  "species": [ ... ],
  "vehicles": [ ... ],
  "staticObjects": [ ... ]
}
```

**Load Simulation State**
- Restore exact state from JSON
- Continue from saved tick
- Useful for checkpointing long runs

**Export Data**
- CSV export of vehicle trajectories
- Statistics over time (avg speed, collisions, etc.)
- Neural network dumps for analysis

## Performance Considerations

**Batching Updates**
- Don't send WebSocket update every tick if ticks are very fast
- Throttle to reasonable FPS (e.g., 30-60)
- Backend can run at 100+ ticks/sec while frontend renders at 30 FPS

**Large-Scale Simulations**
- For 1000+ vehicles:
  - Use spatial partitioning aggressively
  - Consider reducing broadcast detail (send only visible vehicles)
  - Implement level-of-detail rendering
  - Send differential updates instead of full state

**Thread Pool Sizing**
- Use available CPU cores (Runtime.availableProcessors())
- Test optimal pool size for your workload
- Too many threads → overhead; too few → underutilized

## Monitoring and Debugging

**Metrics to Track:**
- Ticks per second (actual vs target)
- Time per phase (sense, think, act)
- Number of active vehicles
- Collision count
- Network evaluation time

**Debug Features:**
- Visualize receptor cones
- Show neurode firing states
- Display vehicle paths/trails
- Highlight active connections in neural network
- Inspector panel for selected vehicle

---

# Example Species Definitions

## Braitenberg Vehicles

Classic Braitenberg vehicles demonstrate how simple neural connections produce complex emergent behaviors. These make excellent starting points for testing.

### 1. Phototrope (Love/Attraction)

**Behavior:** Approaches light sources

**Receptor Configuration:**
- Left sensor: 45° to 135° (left-front quarter)
- Right sensor: -135° to -45° (right-front quarter)

**Neural Network:**
- Left sensor → Right motor (crossed excitation)
- Right sensor → Left motor (crossed excitation)

**Why it works:**
- Light on left → right motor speeds up → vehicle turns left toward light
- Light on right → left motor speeds up → vehicle turns right toward light
- Result: Vehicle curves toward and slows near light sources

**JSON Definition:**
```json
{
  "name": "Phototrope",
  "vehicleRadius": 5,
  "wheelBase": 8,
  "maxSpeed": 50,
  "receptors": [
    {"id": "left", "angleFrom": 0.785, "angleTo": 2.356, "maxRange": 200, "sensitivity": 1.0},
    {"id": "right", "angleFrom": -2.356, "angleTo": -0.785, "maxRange": 200, "sensitivity": 1.0}
  ],
  "neuralNetwork": {
    "neurodes": [
      {"id": "input_left", "type": "INPUT", "threshold": 0.1},
      {"id": "input_right", "type": "INPUT", "threshold": 0.1},
      {"id": "output_left_motor", "type": "OUTPUT"},
      {"id": "output_right_motor", "type": "OUTPUT"}
    ],
    "connections": [
      {"from": "input_left", "to": "output_right_motor", "type": "EXCITER", "weight": 1.0},
      {"from": "input_right", "to": "output_left_motor", "type": "EXCITER", "weight": 1.0}
    ]
  }
}
```

### 2. Photophobe (Fear/Avoidance)

**Behavior:** Avoids light sources, retreats into darkness

**Receptor Configuration:**
- Same as Phototrope (left and right sensors)

**Neural Network:**
- Left sensor → Left motor (parallel excitation)
- Right sensor → Right motor (parallel excitation)

**Why it works:**
- Light on left → left motor speeds up → vehicle turns right away from light
- Light on right → right motor speeds up → vehicle turns left away from light
- Result: Vehicle turns away from light sources

**JSON Definition:**
```json
{
  "name": "Photophobe",
  "vehicleRadius": 5,
  "wheelBase": 8,
  "maxSpeed": 50,
  "receptors": [
    {"id": "left", "angleFrom": 0.785, "angleTo": 2.356, "maxRange": 200, "sensitivity": 1.0},
    {"id": "right", "angleFrom": -2.356, "angleTo": -0.785, "maxRange": 200, "sensitivity": 1.0}
  ],
  "neuralNetwork": {
    "neurodes": [
      {"id": "input_left", "type": "INPUT", "threshold": 0.1},
      {"id": "input_right", "type": "INPUT", "threshold": 0.1},
      {"id": "output_left_motor", "type": "OUTPUT"},
      {"id": "output_right_motor", "type": "OUTPUT"}
    ],
    "connections": [
      {"from": "input_left", "to": "output_left_motor", "type": "EXCITER", "weight": 1.0},
      {"from": "input_right", "to": "output_right_motor", "type": "EXCITER", "weight": 1.0}
    ]
  }
}
```

### 3. Explorer (Obstacle Avoider)

**Behavior:** Moves forward, turns away from obstacles

**Receptor Configuration:**
- Left-front: -30° to 30°
- Right-front: 30° to 90°
- Left-front: -90° to -30°

**Neural Network:**
- Front sensors inhibit opposite motors when obstacle detected
- Bias neurode keeps motors running by default

**JSON Definition:**
```json
{
  "name": "Explorer",
  "vehicleRadius": 5,
  "wheelBase": 8,
  "maxSpeed": 40,
  "receptors": [
    {"id": "front", "angleFrom": -0.524, "angleTo": 0.524, "maxRange": 100, "sensitivity": 1.0},
    {"id": "left", "angleFrom": 0.524, "angleTo": 1.571, "maxRange": 100, "sensitivity": 1.0},
    {"id": "right", "angleFrom": -1.571, "angleTo": -0.524, "maxRange": 100, "sensitivity": 1.0}
  ],
  "neuralNetwork": {
    "neurodes": [
      {"id": "input_front", "type": "INPUT", "threshold": 0.3},
      {"id": "input_left", "type": "INPUT", "threshold": 0.3},
      {"id": "input_right", "type": "INPUT", "threshold": 0.3},
      {"id": "bias", "type": "INPUT", "threshold": 0.0},
      {"id": "output_left_motor", "type": "OUTPUT"},
      {"id": "output_right_motor", "type": "OUTPUT"}
    ],
    "connections": [
      {"from": "bias", "to": "output_left_motor", "type": "EXCITER", "weight": 1.0},
      {"from": "bias", "to": "output_right_motor", "type": "EXCITER", "weight": 1.0},
      {"from": "input_front", "to": "output_left_motor", "type": "INHIBITOR", "weight": 1.0},
      {"from": "input_front", "to": "output_right_motor", "type": "INHIBITOR", "weight": 1.0},
      {"from": "input_left", "to": "output_left_motor", "type": "INHIBITOR", "weight": 1.0},
      {"from": "input_right", "to": "output_right_motor", "type": "INHIBITOR", "weight": 1.0}
    ]
  }
}
```

### 4. Aggressive (Pursuer)

**Behavior:** Charges at objects, aggressive approach

**Receptor Configuration:**
- Narrow forward sensors

**Neural Network:**
- Crossed inhibitory connections
- When target on left, inhibits left motor → turns left aggressively
- Opposite of photophobe (uses inhibition instead of excitation)

**JSON Definition:**
```json
{
  "name": "Aggressive",
  "vehicleRadius": 5,
  "wheelBase": 8,
  "maxSpeed": 60,
  "receptors": [
    {"id": "left", "angleFrom": 0.0, "angleTo": 0.785, "maxRange": 150, "sensitivity": 1.0},
    {"id": "right", "angleFrom": -0.785, "angleTo": 0.0, "maxRange": 150, "sensitivity": 1.0}
  ],
  "neuralNetwork": {
    "neurodes": [
      {"id": "input_left", "type": "INPUT", "threshold": 0.2},
      {"id": "input_right", "type": "INPUT", "threshold": 0.2},
      {"id": "bias", "type": "INPUT", "threshold": 0.0},
      {"id": "output_left_motor", "type": "OUTPUT"},
      {"id": "output_right_motor", "type": "OUTPUT"}
    ],
    "connections": [
      {"from": "bias", "to": "output_left_motor", "type": "EXCITER", "weight": 1.0},
      {"from": "bias", "to": "output_right_motor", "type": "EXCITER", "weight": 1.0},
      {"from": "input_left", "to": "output_right_motor", "type": "INHIBITOR", "weight": 1.0},
      {"from": "input_right", "to": "output_left_motor", "type": "INHIBITOR", "weight": 1.0}
    ]
  }
}
```

## Testing Scenarios

**Scenario 1: Single Light Source**
- Arena: 1000×1000, no wrapping
- One bright static object at center (500, 500, brightness=1.0)
- Spawn multiple Phototropes around edges
- Expected: All converge toward light

**Scenario 2: Light and Dark**
- Two lights on left side, dark area on right
- Spawn Phototropes on right → should move left
- Spawn Photophobes on left → should move right

**Scenario 3: Obstacle Course**
- Random static objects (low brightness obstacles)
- Spawn Explorers
- Expected: Navigate around obstacles without getting stuck

**Scenario 4: Predator-Prey**
- Bright "prey" vehicles (Photophobes trying to escape)
- Dark "predator" vehicles (Aggressive chasers)
- Interesting emergent dynamics

---

# Validation Rules and Constraints

## Arena Validation

**Width and Height:**
- Minimum: 100 units
- Maximum: 10000 units
- Must be positive numbers
- Error: "Arena dimensions must be between 100 and 10000 units"

**Wrapping:**
- Both `wrapEastWest` and `wrapNorthSouth` are boolean
- No validation needed (default to false)

**Background Color:**
- Must be valid hex color (#RRGGBB format)
- Pattern: `^#[0-9A-Fa-f]{6}$`
- Error: "Invalid color format. Use #RRGGBB"

## Vehicle Validation

**Position (x, y):**
- Must be within arena bounds: `0 <= x < arena.width`, `0 <= y < arena.height`
- Error: "Vehicle position must be within arena bounds"

**Angle:**
- Must be in radians: `0 <= angle < 2π` (0 to 6.283185307)
- Automatically normalized to this range if outside
- Error: "Invalid angle, must be 0 to 2π radians"

**Motor Speeds:**
- Range: `-1.0 <= speed <= 1.0`
- Set by neural network, validated before physics update
- Error: "Motor speed must be between -1.0 and 1.0"

**Radius:**
- Minimum: 1 unit
- Maximum: 100 units
- Must be positive
- Error: "Vehicle radius must be between 1 and 100 units"

**Wheel Base:**
- Minimum: 2 units
- Maximum: 200 units
- Must be positive
- Error: "Wheel base must be between 2 and 200 units"

**Max Speed:**
- Minimum: 1 unit/second
- Maximum: 1000 units/second
- Must be positive
- Error: "Max speed must be between 1 and 1000 units/second"

## Species Validation

**Name:**
- Required, non-empty
- Maximum length: 100 characters
- Pattern: `^[a-zA-Z0-9_\-\s]+$` (alphanumeric, underscore, hyphen, space)
- Error: "Species name is required and must be 1-100 characters"

**Receptors:**
- Minimum: 0 receptors (vehicle is blind)
- Maximum: 20 receptors
- Each receptor must have unique ID
- Error: "Species can have 0-20 receptors with unique IDs"

**Neural Network:**
- Must have at least 2 OUTPUT neurodes (left_motor, right_motor)
- INPUT neurodes must match receptor count + 1 (for bias)
- All neurodes must have unique IDs
- Error: "Neural network must have required OUTPUT neurodes and valid structure"

## Receptor Validation

**ID:**
- Required, non-empty
- Maximum length: 50 characters
- Must be unique within species
- Pattern: `^[a-zA-Z0-9_]+$`
- Error: "Receptor ID is required and must be unique"

**Angle Range:**
- `angleFrom` and `angleTo` must be in range: `-2π <= angle <= 2π`
- `angleTo` can be less than `angleFrom` (wraps around)
- At least one of them must be different
- Error: "Receptor angles must be between -2π and 2π"

**Max Range:**
- Minimum: 10 units
- Maximum: 1000 units
- Must be positive
- Error: "Receptor max range must be between 10 and 1000 units"

**Sensitivity:**
- Minimum: 0.0 (completely insensitive)
- Maximum: 10.0 (highly sensitive)
- Error: "Receptor sensitivity must be between 0.0 and 10.0"

## Neural Network Validation

**Neurode ID:**
- Required, non-empty
- Maximum length: 50 characters
- Must be unique within network
- Pattern: `^[a-zA-Z0-9_]+$`
- Error: "Neurode ID is required and must be unique within network"

**Neurode Type:**
- Must be one of: INPUT, HIDDEN, OUTPUT
- Error: "Invalid neurode type. Must be INPUT, HIDDEN, or OUTPUT"

**Threshold:**
- Minimum: 0.0
- Maximum: 100.0
- Error: "Neurode threshold must be between 0.0 and 100.0"

**Decay Rate:**
- Minimum: 0.0 (no decay)
- Maximum: 1.0 (complete decay)
- Error: "Decay rate must be between 0.0 and 1.0"

**Required Output Neurodes:**
- Must have neurode with ID "output_left_motor" or matching pattern
- Must have neurode with ID "output_right_motor" or matching pattern
- Error: "Neural network must have output_left_motor and output_right_motor neurodes"

**Maximum Neurodes:**
- Total neurodes: 1000 (prevents excessive complexity)
- Error: "Neural network cannot exceed 1000 neurodes"

## Connection Validation

**Connection ID:**
- Required, non-empty
- Maximum length: 50 characters
- Must be unique within network
- Pattern: `^[a-zA-Z0-9_]+$`
- Error: "Connection ID is required and must be unique"

**From/To Neurode:**
- Must reference existing neurode IDs in the network
- Cannot be empty
- Error: "Connection must reference valid neurode IDs"

**Connection Type:**
- Must be one of: EXCITER, INHIBITOR
- Error: "Invalid connection type. Must be EXCITER or INHIBITOR"

**Weight:**
- Minimum: 0.0
- Maximum: 10.0
- Error: "Connection weight must be between 0.0 and 10.0"

**Circular Reference Detection:**
- Self-loops are allowed (neurode → itself)
- No validation needed for cycles (they're intentional)

**Maximum Connections:**
- Total connections: 5000 (prevents excessive complexity)
- Error: "Neural network cannot exceed 5000 connections"

## Static Object Validation

**Position (x, y):**
- Must be within arena bounds
- Error: "Static object position must be within arena bounds"

**Radius:**
- Minimum: 1 unit
- Maximum: 500 units
- Error: "Static object radius must be between 1 and 500 units"

**Brightness:**
- Minimum: 0.0 (completely dark)
- Maximum: 1.0 (maximum brightness)
- Error: "Brightness must be between 0.0 and 1.0"

**Color:**
- Must be valid hex color (#RRGGBB format)
- Pattern: `^#[0-9A-Fa-f]{6}$`
- Error: "Invalid color format. Use #RRGGBB"

## Simulation Configuration Validation

**Ticks Per Second:**
- Minimum: 1 TPS
- Maximum: 1000 TPS
- Value 0 means "as fast as possible"
- Error: "Ticks per second must be 0 (unlimited) or 1-1000"

**Delta Time:**
- Minimum: 0.001 seconds
- Maximum: 1.0 seconds
- Error: "Delta time must be between 0.001 and 1.0 seconds"

**Network Iterations:**
- Minimum: 1 iteration
- Maximum: 100 iterations
- Error: "Network iterations must be between 1 and 100"

**Broadcast Throttle FPS:**
- Minimum: 1 FPS
- Maximum: 120 FPS
- Value 0 means "send every tick"
- Error: "Broadcast FPS must be 0 (every tick) or 1-120"

## Simulation Limits

**Maximum Vehicles:**
- Hard limit: 10,000 vehicles per simulation
- Recommended: 1,000 vehicles for smooth performance
- Error: "Cannot exceed 10,000 vehicles in simulation"

**Maximum Static Objects:**
- Hard limit: 1,000 static objects
- Error: "Cannot exceed 1,000 static objects in simulation"

**Maximum Species:**
- Hard limit: 100 species
- Error: "Cannot exceed 100 species definitions"

## API Request Validation

**JSON Parsing:**
- Must be valid JSON
- Error: "Invalid JSON format"

**Required Fields:**
- Check for presence of required fields before processing
- Error: "Missing required field: {fieldName}"

**Field Type Checking:**
- Verify types match expected (number, string, boolean, array, object)
- Error: "Invalid type for field {fieldName}. Expected {expectedType}"

**ID Format:**
- Server-generated IDs: UUID format or sequential
- Client cannot specify IDs for creation (generated server-side)
- Pattern for UUIDs: `^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$`

## Validation Implementation Strategy

**Backend Validation (Java):**
```java
public class ValidationUtil {
    public static void validateArena(Arena arena) {
        if (arena.getWidth() < 100 || arena.getWidth() > 10000) {
            throw new ValidationException("Arena width must be between 100 and 10000");
        }
        // ... more validations
    }

    public static void validateVehicle(Vehicle vehicle, Arena arena) {
        if (vehicle.getX() < 0 || vehicle.getX() >= arena.getWidth()) {
            throw new ValidationException("Vehicle X position out of arena bounds");
        }
        // ... more validations
    }
}
```

**Validation Timing:**
- **On creation:** Validate all parameters before creating entities
- **On update:** Validate changed parameters
- **Before simulation tick:** Quick sanity check on critical values
- **After neural network evaluation:** Clamp motor speeds to valid range

**Error Response Format:**
```json
{
  "error": true,
  "message": "Validation failed",
  "details": [
    {"field": "width", "error": "Arena width must be between 100 and 10000"},
    {"field": "receptors[0].maxRange", "error": "Receptor max range must be between 10 and 1000"}
  ]
}
```

---

# Error Handling Strategy

## Exception Hierarchy

**Base Exception:**
```java
public class SimulationException extends RuntimeException {
    private final int httpStatusCode;
    private final String errorCode;

    public SimulationException(String message, int httpStatusCode, String errorCode) {
        super(message);
        this.httpStatusCode = httpStatusCode;
        this.errorCode = errorCode;
    }
}
```

**Specific Exceptions:**
- `ValidationException` (400 Bad Request) - Invalid input data
- `ResourceNotFoundException` (404 Not Found) - Entity not found
- `SimulationStateException` (409 Conflict) - Invalid state for operation
- `ResourceLimitException` (429 Too Many Requests) - Exceeded limits
- `InternalSimulationException` (500 Internal Server Error) - Unexpected errors

## HTTP Status Codes

**Success Codes:**
- `200 OK` - Successful GET, PUT, or DELETE
- `201 Created` - Successful POST (entity created)
- `204 No Content` - Successful DELETE with no response body

**Client Error Codes:**
- `400 Bad Request` - Validation failure, malformed JSON
- `404 Not Found` - Resource doesn't exist
- `405 Method Not Allowed` - Wrong HTTP method for endpoint
- `409 Conflict` - Operation conflicts with current state (e.g., start already running simulation)
- `422 Unprocessable Entity` - Valid JSON but semantically incorrect
- `429 Too Many Requests` - Exceeded rate limits or entity limits

**Server Error Codes:**
- `500 Internal Server Error` - Unexpected error in simulation engine
- `503 Service Unavailable` - Server overloaded or shutting down

## Error Response Structure

**Standard Error Response:**
```json
{
  "error": true,
  "errorCode": "VALIDATION_ERROR",
  "message": "Validation failed for vehicle creation",
  "details": [
    {
      "field": "x",
      "value": 12000,
      "error": "Vehicle position must be within arena bounds (0-1000)"
    }
  ],
  "timestamp": "2025-01-04T10:30:45.123Z",
  "path": "/api/vehicles"
}
```

**Error Codes:**
- `VALIDATION_ERROR` - Input validation failed
- `NOT_FOUND` - Resource not found
- `ALREADY_EXISTS` - Duplicate resource
- `INVALID_STATE` - Operation not allowed in current state
- `LIMIT_EXCEEDED` - Too many entities
- `NEURAL_NETWORK_ERROR` - Neural network evaluation failed
- `PHYSICS_ERROR` - Physics calculation failed
- `INTERNAL_ERROR` - Unexpected server error

## Servlet Error Handling

**Base Servlet Pattern:**
```java
protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
    try {
        // Parse request
        String json = readRequestBody(req);

        // Validate and process
        Object result = processRequest(json);

        // Send success response
        sendJsonResponse(resp, 201, result);

    } catch (ValidationException e) {
        sendErrorResponse(resp, 400, "VALIDATION_ERROR", e.getMessage(), e.getDetails());
    } catch (ResourceNotFoundException e) {
        sendErrorResponse(resp, 404, "NOT_FOUND", e.getMessage(), null);
    } catch (SimulationStateException e) {
        sendErrorResponse(resp, 409, "INVALID_STATE", e.getMessage(), null);
    } catch (JsonSyntaxException e) {
        sendErrorResponse(resp, 400, "INVALID_JSON", "Malformed JSON", null);
    } catch (Exception e) {
        logger.error("Unexpected error", e);
        sendErrorResponse(resp, 500, "INTERNAL_ERROR", "An unexpected error occurred", null);
    }
}
```

## Simulation Engine Error Handling

**Vehicle Processing Errors:**
```java
// Option 1: Skip failed vehicles
for (Vehicle vehicle : vehicles) {
    try {
        processVehicle(vehicle);
    } catch (Exception e) {
        logger.error("Error processing vehicle {}: {}", vehicle.getId(), e.getMessage());
        // Vehicle remains in previous state
        metrics.incrementFailedVehicleCount();
    }
}

// Option 2: Stop simulation on critical error
try {
    processAllVehicles();
} catch (PhysicsException e) {
    logger.error("Critical physics error, stopping simulation", e);
    simulationEngine.stop();
    notifyClients("Simulation stopped due to error: " + e.getMessage());
}
```

**Neural Network Errors:**
- If network evaluation fails: freeze vehicle motors at last known state
- Log error with vehicle and species details
- Continue simulation (don't crash entire system for one bad network)
- Optionally: mark vehicle as "failed" and stop updating it

**Collision Detection Errors:**
- If spatial index fails: fall back to brute-force collision check
- Log warning about performance degradation
- Continue simulation

## WebSocket Error Handling

**Connection Errors:**
```java
@OnError
public void onError(Session session, Throwable throwable) {
    logger.error("WebSocket error for session {}: {}", session.getId(), throwable.getMessage());
    try {
        session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Server error"));
    } catch (IOException e) {
        logger.error("Failed to close session", e);
    }
}
```

**Broadcast Errors:**
- If broadcast to client fails: remove that client from subscribers
- Don't block other clients
- Log error and continue

**Message Serialization Errors:**
- If state serialization fails: log error, skip that broadcast tick
- Simulation continues running
- Try again next tick

## Recovery Strategies

**Automatic Recovery:**

1. **Invalid Vehicle State:**
   - If vehicle position becomes NaN or infinite: reset to last valid position
   - If motor speeds are invalid: clamp to valid range [-1.0, 1.0]
   - If vehicle exits arena (when wrapping disabled): move back to boundary

2. **Neural Network Instability:**
   - If neurode charge becomes infinite: reset to 0
   - If network oscillates (detected via state history): dampen or reset

3. **Resource Exhaustion:**
   - If thread pool is saturated: queue new ticks until threads available
   - If memory is low: trigger GC, reduce broadcast frequency
   - If too many vehicles: warn and refuse new additions

**Manual Recovery (User Actions):**
- Stop simulation
- Step through tick-by-tick to debug
- Remove problematic vehicles or species
- Reset simulation to known good state
- Export/import simulation state for debugging

## Logging Strategy

**Log Levels:**

**ERROR:** Critical failures requiring attention
- Servlet exceptions
- Simulation engine crashes
- WebSocket connection failures
- Validation failures (sampled, not every one)

**WARN:** Recoverable issues
- Vehicle processing failures
- Spatial index degradation
- Client disconnections
- Performance degradation (slow ticks)

**INFO:** Normal operations
- Simulation start/stop
- Species creation
- Vehicle count milestones (1000, 5000, 10000)
- Configuration changes

**DEBUG:** Detailed diagnostic info
- Individual vehicle updates
- Neural network evaluations
- Physics calculations
- API requests/responses

**Log Format:**
```
2025-01-04 10:30:45.123 [sim-worker-3] ERROR c.g.v.engine.VehicleProcessor - Error processing vehicle v_abc123: Neural network evaluation failed
```

## Client-Side Error Handling

**WebSocket Connection Loss:**
```javascript
ws.onclose = function(event) {
    console.error("WebSocket closed:", event.code, event.reason);
    // Attempt reconnect with exponential backoff
    setTimeout(reconnect, reconnectDelay);
    reconnectDelay = Math.min(reconnectDelay * 2, 30000);

    // Show user notification
    showError("Connection lost. Reconnecting...");
};
```

**API Request Failures:**
```javascript
async function createSpecies(speciesData) {
    try {
        const response = await fetch('/api/species', {
            method: 'POST',
            body: JSON.stringify(speciesData)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message);
        }

        return await response.json();
    } catch (error) {
        console.error("Failed to create species:", error);
        showError(`Failed to create species: ${error.message}`);
        throw error;
    }
}
```

**Rendering Errors:**
- If canvas rendering fails: log error, skip frame, try again next frame
- If data is malformed: use previous frame's data
- Show warning indicator in UI

## Monitoring and Alerts

**Health Checks:**
- Endpoint: `GET /api/health`
- Returns simulation status, tick rate, error counts
- Can be polled by monitoring systems

**Metrics to Track:**
- Errors per minute (by type)
- Failed vehicle updates
- WebSocket disconnections
- Average tick duration
- Memory usage

**Alert Thresholds:**
- Error rate > 10/minute → WARN
- Error rate > 100/minute → CRITICAL
- Tick duration > 2× target → WARN
- Memory usage > 80% → WARN

## Graceful Degradation

**When Performance Suffers:**
1. Reduce broadcast frequency (60 FPS → 30 FPS → 10 FPS)
2. Send simplified state (positions only, no neurode states)
3. Reduce simulation tick rate
4. Warn user about performance issues

**When Errors Accumulate:**
1. Increase logging detail temporarily
2. Sample and report error patterns to user
3. Suggest actions (reduce vehicles, simplify networks)
4. Offer simulation export for offline debugging

**Failsafe Mechanisms:**
- Maximum simulation runtime (prevent infinite loops)
- Watchdog thread to detect stuck simulation
- Emergency stop button (kills all threads immediately)
- State persistence before potentially dangerous operations

---

# Implementation Notes (January 2025)

## Completed Implementation

This section documents the actual implementation completed in January 2025, distinguishing it from the full design vision above.

### Core Engine Implementation

**Neural Network Evaluation (NeuralNetworkEvaluator.java:97)**
- Wave-based synchronous model fully implemented
- Handles cycles, self-loops, and feedback naturally
- Three-phase evaluation: setInputs → advanceAll → evaluateAll → readOutputs
- No iteration limits needed - O(1) per tick regardless of network complexity

**Physics Engine (PhysicsEngine.java:83)**
- Differential drive kinematics with exact formulas from design
- Position wrapping for toroidal arenas
- Collision detection for walls and objects
- Configurable collision modes (NONE, BREAK, BOUNCE)

**Receptor Sensing (Receptor.java:75)**
- Capacitor-based light accumulation algorithm
- Color filtering (only detects matching colorName)
- Angular field of view checking with wraparound support
- Distance-based attenuation: `brightness × (1 - distance/maxRange) × sensitivity`
- Threshold crossing with partial discharge

**Simulation Engine (SimulationEngine.java:116)**
- Complete tick orchestration: SENSE → ADVANCE → THINK → ACT → BROADCAST
- State broadcasting via StateListener pattern
- Start/stop/step/reset controls
- Thread-safe operation

### Web Layer Implementation

**Embedded Jetty Server (Application.java:18)**
- Programmatic servlet and WebSocket registration
- No web.xml required
- Static file serving from `frontend/` directory
- Graceful shutdown hook

**REST API (SimulationServlet.java:32)**
- 12 endpoints implemented (see README.md)
- JSON request/response with Gson
- Comprehensive error handling with proper HTTP codes
- Base servlet pattern for common functionality

**WebSocket Streaming (SimulationWebSocket.java:33)**
- Subscribe pattern: clients subscribe to simulation IDs
- Real-time state broadcasting on each tick
- Automatic cleanup on disconnect
- Error handling and reconnection support

**Service Layer (SimulationService.java:17)**
- Singleton pattern with thread-safe ConcurrentHashMap storage
- Complete business logic for simulation lifecycle
- Vehicle and static object management
- Validation before all operations

### Frontend Implementation

**JavaScript Modules:**
- `api-client.js` - Fetch-based REST wrapper
- `websocket-client.js` - WebSocket with exponential backoff reconnect
- `renderer.js` - Canvas rendering with:
  - Coordinate transformation (arena → canvas)
  - Vehicle rendering with direction indicators
  - Light sources with glow effects
  - Sensor FOV visualization
  - Tick counter display
- `main.js` - Application controller with UI event handling

### Species Package

**Base Package (base.vsp)**
- 6 complete Braitenberg species with full neural networks
- JSON format matching design specification
- PackageBuilder.java utility for programmatic generation

**Species Included:**
1. Phototrope - Crossed excitatory (approaches light)
2. Photophobe - Parallel excitatory (flees light)
3. Explorer - Bias + inhibitory (obstacle avoidance)
4. Aggressive - Crossed inhibitory (charges targets)
5. Coward - Rear sensors + parallel (flees from behind)
6. Paranoid - 4-direction sensors + complex inhibition

### Technology Stack Used

**Backend:**
- Java 11+ language features
- Jetty 11.0.18 (embedded server)
- Gson 2.10.1 (JSON)
- SLF4J 2.0.9 + Logback 1.4.11 (logging)
- Jakarta Servlet API 5.0.0 (not javax)
- Maven 3.6+ (build)
- Maven Shade Plugin 3.5.1 (executable JAR)

**Frontend:**
- Vanilla JavaScript ES6+
- HTML5 Canvas API
- WebSocket API (native browser)
- No frameworks or build tools

## Key Design Decisions

### Simplifications from Original Design

1. **Single Simulation**: Simplified from multi-simulation architecture
   - No SimulationManager
   - No per-simulation thread pools (sequential processing)
   - Easier to reason about, sufficient for MVP

2. **No Persistence**: Removed file-based save/load
   - No SimulationRepository/PackageRepository
   - No .vsim/.vsp file loading via API
   - Species package exists but loaded programmatically, not via API

3. **Minimal Service Layer**: Consolidated services
   - Single SimulationService instead of multiple service classes
   - Direct model manipulation where appropriate
   - Less abstraction, clearer code paths

4. **No Advanced Features**:
   - No statistics/metrics tracking
   - No thread pool configuration
   - No health checks endpoint
   - No rate limiting
   - No advanced error recovery

### Architectural Patterns Used

1. **Singleton Pattern**: SimulationService for global state management
2. **Observer Pattern**: StateListener for WebSocket broadcasting
3. **Strategy Pattern**: CollisionBehavior for configurable collision modes
4. **Template Method**: BaseServlet for common servlet functionality
5. **DTO Pattern**: SimulationState/VehicleState for WebSocket payloads

### Notable Implementation Details

**Wave-Based Neural Network:**
- Each neurode has `firedPreviousTick` and `willFireNextTick`
- `advanceTick()` moves will → fired for ALL neurodes simultaneously
- This creates synchronized wave propagation
- Natural 1-tick delay per connection (enables cycles)
- No recursion or iteration needed

**Capacitor Receptors:**
- Light accumulates across multiple ticks
- Threshold crossing reduces charge by threshold amount (not to zero)
- Enables multiple firings from single bright object
- Creates realistic "charging" behavior

**Collision Detection:**
- Per-color configuration in CollisionBehavior
- Point-to-point for vehicles vs static objects
- Point-to-line for vehicles vs walls
- Early exit on collision for performance

**WebSocket Protocol:**
- Subscribe message: `{"type": "subscribe", "simulationId": "..."}`
- State update: `{"type": "state_update", "tick": N, "vehicles": [...], "staticObjects": [...]}`
- Error message: `{"type": "error", "message": "..."}`
- Unsubscribe on disconnect

## Testing Approach

**Manual Testing Recommended:**
1. Build and run server
2. Create simulation via API
3. Add light source at arena center
4. Add vehicles from species (requires loading base.vsp programmatically)
5. Start simulation
6. Verify:
   - Phototropes approach light
   - Photophobes flee from light
   - Explorers navigate obstacles
   - WebSocket updates arrive in real-time
   - Start/stop/step controls work

**Unit Tests:**
- Not included in MVP
- Design provides comprehensive test scenarios (see validation section)
- Integration tests could verify:
  - Neural network evaluation with known inputs
  - Physics calculations with known vehicle states
  - API endpoints return correct HTTP codes
  - WebSocket subscription and broadcasting

## Future Expansion Path

To implement remaining designed features:

1. **Add Multi-Simulation Support**:
   - Create SimulationManager
   - Move SimulationService storage to manager
   - Update servlets to route by simulation ID
   - Add thread pools per simulation

2. **Add Persistence**:
   - Implement SimulationRepository
   - Add save/load endpoints
   - Create JSON serializers for full state

3. **Add Package Management**:
   - Implement PackageRepository
   - Add PackageServlet
   - Support loading .vsp files via API
   - Enable package sharing

4. **Add Advanced Features**:
   - Metrics collection and reporting
   - Health check endpoint
   - Statistics visualization in frontend
   - Configuration panels for species/arena

5. **Add Testing**:
   - Unit tests for core algorithms
   - Integration tests for API
   - End-to-end tests for full scenarios

## Conclusion

The minimal viable implementation provides a fully functional vehicle simulation demonstrating emergent Braitenberg behaviors. All core algorithms (neural networks, physics, sensing) are complete and match the design specification. The web layer provides real-time visualization and control. The system is ready for expansion following the patterns established in the design document.

