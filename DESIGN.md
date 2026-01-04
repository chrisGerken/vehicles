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
- `color: Color` - RGB color
- `brightness: double` - Brightness level (0.0 to 1.0)
- `radius: double` - Collision radius

**Methods:**
- `getBoundingBox(): Rectangle` - For collision detection
- `distanceTo(other: SimulationObject): double` - Distance calculation

## StaticSimulationObject
**Inherits from:** SimulationObject

**Properties:**
- `shape: Shape` - Circle, rectangle, or polygon
- `emitsBrightness: boolean` - Whether it's a light source

**Additional Notes:**
- Immutable position after creation
- Can be used as obstacles or light sources for receptors

## Vehicle
**Inherits from:** SimulationObject

**Properties:**
- `angle: double` - Heading in radians (0 to 2π)
- `leftMotorSpeed: double` - Left wheel speed (-1.0 to 1.0)
- `rightMotorSpeed: double` - Right wheel speed (-1.0 to 1.0)
- `wheelBase: double` - Distance between left and right wheels
- `maxSpeed: double` - Maximum forward speed
- `species: Species` - Reference to species definition
- `receptors: List<Receptor>` - Sensor array (defined by species)
- `neuralNetwork: NeuralNetworkInstance` - Instance of species neural network

**Methods:**
- `updateMotorSpeeds()` - Called after neural network evaluation
- `updatePosition(deltaTime)` - Physics integration
- `sense(): Map<Receptor, double>` - Read all receptor values

## Species
**Properties:**
- `id: String` - Unique identifier
- `name: String` - Human-readable name
- `receptorDefinitions: List<ReceptorDefinition>` - Template for receptors
- `neuralNetworkTemplate: NeuralNetworkTemplate` - Graph structure
- `vehicleRadius: double` - Size of vehicles of this species
- `wheelBase: double` - Wheel separation
- `maxSpeed: double` - Maximum speed

**Methods:**
- `createVehicle(x, y, angle): Vehicle` - Instantiate a new vehicle
- `clone(): Species` - For mutations/variations

## Receptor
**Properties:**
- `id: String` - Unique identifier
- `angleFrom: double` - Start angle relative to vehicle heading (0 to 2π)
- `angleTo: double` - End angle relative to vehicle heading (0 to 2π)
- `maxRange: double` - Maximum detection distance
- `sensitivity: double` - Sensitivity multiplier

**Methods:**
- `sense(vehicle, arena): double` - Returns 0.0 to 1.0 based on detected brightness

**Notes:**
- Detects brightness of objects within angular range
- Returns weighted sum of brightness * (1 - distance/maxRange)

## Neurode
**Properties:**
- `id: String` - Unique identifier within network
- `type: NeurodeType` - INPUT, HIDDEN, OUTPUT
- `threshold: double` - Activation threshold (optional, default 0.0)
- `currentCharge: double` - Accumulated input (for threshold-based firing)
- `decayRate: double` - How quickly charge decays (0.0 to 1.0)
- `inputConnections: List<Connection>` - Incoming connections
- `outputConnections: List<Connection>` - Outgoing connections

**Methods:**
- `evaluate(inputs: Map<String, boolean>): boolean` - Returns whether neurode fires
- `reset()` - Clear charge for new tick

**Types:**
- `INPUT`: Connected to receptor or constant
- `HIDDEN`: Internal processing
- `OUTPUT`: Controls left/right motor

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
- Simulation engine (core logic)
- Physics calculations (vehicle movement, collisions)
- Neural network processing (neurode firing, signal propagation)
- Multi-threaded execution (parallel vehicle processing)
- State management and history
- WebSocket server for real-time updates
- REST API for configuration and control

**Key Components:**
- `SimulationEngine`: Main orchestrator, clock management
- `Arena`: World management, spatial indexing for collision detection
- `VehicleProcessor`: Thread pool for parallel vehicle updates
- `NeuralNetworkEvaluator`: Processes neurode graphs
- `PhysicsEngine`: Movement, collision detection
- `WebSocketServer`: Broadcasts simulation state to connected clients
- `APIController`: REST endpoints for control and configuration

**Concurrency Strategy:**
- Each clock tick processes vehicles in parallel using thread pool
- Vehicles read from shared state (arena, other objects)
- Write updates to local buffers, then merge at end of tick
- Lock-free or fine-grained locking for performance

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
**Server → Client:**
```json
{
  "type": "state_update",
  "tick": 1234,
  "vehicles": [
    {"id": "v1", "x": 100.5, "y": 200.3, "angle": 1.57, "color": "#FF0000", "brightness": 0.8}
  ],
  "staticObjects": [
    {"id": "s1", "x": 50, "y": 50, "color": "#0000FF", "brightness": 1.0}
  ]
}
```

### REST API (Control & Configuration)

#### Simulation Control

**POST /api/simulation/start**
- Starts or resumes simulation
- Response: `{"status": "running", "tick": 123}`

**POST /api/simulation/stop**
- Pauses simulation
- Response: `{"status": "paused", "tick": 123}`

**POST /api/simulation/reset**
- Resets simulation to initial state
- Body: `{"keepEntities": false}` (optional)
- Response: `{"status": "reset", "tick": 0}`

**POST /api/simulation/step**
- Advances simulation by one tick
- Response: `{"status": "paused", "tick": 124}`

**GET /api/simulation/status**
- Gets current simulation state
- Response:
```json
{
  "status": "running",
  "tick": 12345,
  "vehicleCount": 100,
  "ticksPerSecond": 30,
  "actualTPS": 29.8
}
```

#### Arena Configuration

**GET /api/arena**
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

**PUT /api/arena**
- Updates arena configuration (requires stopped simulation)
- Body: Same format as GET response

#### Species Management

**POST /api/species**
- Creates a new species
- Body:
```json
{
  "name": "Phototrope",
  "vehicleRadius": 5,
  "wheelBase": 8,
  "maxSpeed": 50,
  "receptors": [
    {"id": "left", "angleFrom": 0.785, "angleTo": 1.57, "maxRange": 200, "sensitivity": 1.0},
    {"id": "right", "angleFrom": -1.57, "angleTo": -0.785, "maxRange": 200, "sensitivity": 1.0}
  ],
  "neuralNetwork": {
    "neurodes": [
      {"id": "input_left", "type": "INPUT", "threshold": 0.5},
      {"id": "input_right", "type": "INPUT", "threshold": 0.5},
      {"id": "output_left", "type": "OUTPUT"},
      {"id": "output_right", "type": "OUTPUT"}
    ],
    "connections": [
      {"from": "input_left", "to": "output_right", "type": "EXCITER", "weight": 1.0},
      {"from": "input_right", "to": "output_left", "type": "EXCITER", "weight": 1.0}
    ]
  }
}
```
- Response: `{"id": "species_abc123", "name": "Phototrope"}`

**GET /api/species**
- Lists all species
- Response: `[{"id": "species_1", "name": "Phototrope"}, ...]`

**GET /api/species/{id}**
- Gets species details
- Response: Full species definition

**DELETE /api/species/{id}**
- Deletes species (fails if vehicles exist)
- Response: `{"deleted": true}`

#### Vehicle Management

**POST /api/vehicles**
- Adds vehicle(s) to simulation
- Body:
```json
{
  "speciesId": "species_abc123",
  "vehicles": [
    {"x": 100, "y": 100, "angle": 0},
    {"x": 200, "y": 200, "angle": 1.57}
  ]
}
```
- Response: `{"created": ["v_1", "v_2"]}`

**GET /api/vehicles**
- Lists all vehicles (lightweight)
- Response:
```json
[
  {"id": "v_1", "speciesId": "species_abc123", "x": 105.3, "y": 102.1, "angle": 0.1},
  ...
]
```

**GET /api/vehicles/{id}**
- Gets detailed vehicle state including neural network state
- Response:
```json
{
  "id": "v_1",
  "speciesId": "species_abc123",
  "x": 105.3,
  "y": 102.1,
  "angle": 0.1,
  "leftMotorSpeed": 0.8,
  "rightMotorSpeed": 0.9,
  "receptorValues": {"left": 0.3, "right": 0.7},
  "neurodeStates": {"input_left": true, "output_right": true, ...}
}
```

**DELETE /api/vehicles/{id}**
- Removes vehicle from simulation
- Response: `{"deleted": true}`

#### Static Objects

**POST /api/objects**
- Adds static object to arena
- Body:
```json
{
  "x": 500,
  "y": 500,
  "shape": "circle",
  "radius": 20,
  "color": "#FFFFFF",
  "brightness": 1.0,
  "emitsBrightness": true
}
```
- Response: `{"id": "obj_xyz789"}`

**GET /api/objects**
- Lists all static objects
- Response: Array of objects

**DELETE /api/objects/{id}**
- Removes static object
- Response: `{"deleted": true}`

#### Configuration

**GET /api/configuration**
- Gets simulation parameters
- Response:
```json
{
  "ticksPerSecond": 30,
  "deltaTime": 0.1,
  "networkIterations": 10,
  "broadcastThrottleFPS": 30
}
```

**PUT /api/configuration**
- Updates simulation parameters
- Body: Same format as GET response

---

# Technology Stack

## Backend (Java)

**Runtime:**
- Java 11 or higher (for var keyword, modern APIs)
- Servlet API 4.0+ (javax.servlet or jakarta.servlet)

**Build Tool:**
- Maven 3.6+
- Standard directory structure: `src/main/java`, `src/main/resources`, `src/test/java`

**Web Server:**
- Apache Tomcat 9.x or 10.x
- Or Jetty 11.x (embedded or standalone)

**Dependencies (pom.xml):**
```xml
<dependencies>
  <!-- Servlet API -->
  <dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>4.0.1</version>
    <scope>provided</scope>
  </dependency>

  <!-- WebSocket API -->
  <dependency>
    <groupId>javax.websocket</groupId>
    <artifactId>javax.websocket-api</artifactId>
    <version>1.1</version>
    <scope>provided</scope>
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
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.13.2</version>
    <scope>test</scope>
  </dependency>
</dependencies>
```

**Key Libraries:**
- **Gson** - JSON serialization/deserialization
- **Java WebSocket API** - For real-time communication
- **java.util.concurrent** - Thread pools, concurrent collections
- **SLF4J + Logback** - Logging

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
# Backend
cd backend
mvn clean package
mvn tomcat7:run
# Or deploy target/vehicle-sim.war to Tomcat webapps/

# Frontend
cd frontend
python3 -m http.server 8080
# Or use any static file server
```

---

# Project Structure

## Backend (Maven/Java)

```
vehicle-simulation/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── vehiclesim/
│   │   │           ├── model/              # Business objects
│   │   │           │   ├── Arena.java
│   │   │           │   ├── SimulationObject.java
│   │   │           │   ├── StaticSimulationObject.java
│   │   │           │   ├── Vehicle.java
│   │   │           │   ├── Species.java
│   │   │           │   ├── Receptor.java
│   │   │           │   ├── Neurode.java
│   │   │           │   ├── Connection.java
│   │   │           │   └── NeurodeType.java (enum)
│   │   │           │
│   │   │           ├── engine/             # Simulation engine
│   │   │           │   ├── SimulationEngine.java
│   │   │           │   ├── Clock.java
│   │   │           │   ├── VehicleProcessor.java
│   │   │           │   ├── PhysicsEngine.java
│   │   │           │   ├── NeuralNetworkEvaluator.java
│   │   │           │   └── SpatialIndex.java
│   │   │           │
│   │   │           ├── servlet/            # HTTP endpoints
│   │   │           │   ├── SimulationControlServlet.java
│   │   │           │   ├── ArenaServlet.java
│   │   │           │   ├── SpeciesServlet.java
│   │   │           │   ├── VehicleServlet.java
│   │   │           │   ├── StaticObjectServlet.java
│   │   │           │   └── ConfigurationServlet.java
│   │   │           │
│   │   │           ├── websocket/          # WebSocket endpoint
│   │   │           │   └── SimulationWebSocket.java
│   │   │           │
│   │   │           ├── service/            # Business logic
│   │   │           │   ├── SimulationService.java
│   │   │           │   ├── SpeciesService.java
│   │   │           │   └── VehicleService.java
│   │   │           │
│   │   │           ├── repository/         # Data storage (in-memory for now)
│   │   │           │   ├── SpeciesRepository.java
│   │   │           │   ├── VehicleRepository.java
│   │   │           │   └── StaticObjectRepository.java
│   │   │           │
│   │   │           └── util/               # Utilities
│   │   │               ├── JsonUtil.java
│   │   │               ├── MathUtil.java
│   │   │               └── ValidationUtil.java
│   │   │
│   │   ├── resources/
│   │   │   ├── logback.xml               # Logging configuration
│   │   │   └── simulation.properties     # Default parameters
│   │   │
│   │   └── webapp/
│   │       └── WEB-INF/
│   │           └── web.xml                # Servlet mappings
│   │
│   └── test/
│       └── java/
│           └── com/
│               └── vehiclesim/
│                   ├── model/
│                   │   └── VehicleTest.java
│                   ├── engine/
│                   │   ├── PhysicsEngineTest.java
│                   │   └── NeuralNetworkEvaluatorTest.java
│                   └── util/
│                       └── MathUtilTest.java
│
└── target/                                 # Build output (generated)
    └── vehicle-simulation.war
```

## Frontend (JavaScript)

```
frontend/
├── index.html                              # Main page
├── css/
│   └── style.css                          # Styles
├── js/
│   ├── main.js                            # Entry point
│   ├── websocket-client.js                # WebSocket handling
│   ├── api-client.js                      # REST API calls
│   ├── renderer.js                        # Canvas rendering
│   ├── ui-controller.js                   # UI event handlers
│   ├── config-panel.js                    # Species/arena configuration UI
│   └── stats-panel.js                     # Statistics display
├── lib/                                    # Optional third-party libraries
│   └── chart.min.js                       # If using Chart.js
└── assets/
    └── icons/                              # UI icons if needed
```

## Servlet Mapping (web.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee
         http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
         version="4.0">

  <display-name>Vehicle Simulation</display-name>

  <!-- Simulation Control -->
  <servlet>
    <servlet-name>SimulationControlServlet</servlet-name>
    <servlet-class>com.vehiclesim.servlet.SimulationControlServlet</servlet-class>
  </servlet>
  <servlet-mapping>
    <servlet-name>SimulationControlServlet</servlet-name>
    <url-pattern>/api/simulation/*</url-pattern>
  </servlet-mapping>

  <!-- Arena Configuration -->
  <servlet>
    <servlet-name>ArenaServlet</servlet-name>
    <servlet-class>com.vehiclesim.servlet.ArenaServlet</servlet-class>
  </servlet>
  <servlet-mapping>
    <servlet-name>ArenaServlet</servlet-name>
    <url-pattern>/api/arena</url-pattern>
  </servlet-mapping>

  <!-- Species Management -->
  <servlet>
    <servlet-name>SpeciesServlet</servlet-name>
    <servlet-class>com.vehiclesim.servlet.SpeciesServlet</servlet-class>
  </servlet>
  <servlet-mapping>
    <servlet-name>SpeciesServlet</servlet-name>
    <url-pattern>/api/species/*</url-pattern>
  </servlet-mapping>

  <!-- Vehicle Management -->
  <servlet>
    <servlet-name>VehicleServlet</servlet-name>
    <servlet-class>com.vehiclesim.servlet.VehicleServlet</servlet-class>
  </servlet>
  <servlet-mapping>
    <servlet-name>VehicleServlet</servlet-name>
    <url-pattern>/api/vehicles/*</url-pattern>
  </servlet-mapping>

  <!-- Static Objects -->
  <servlet>
    <servlet-name>StaticObjectServlet</servlet-name>
    <servlet-class>com.vehiclesim.servlet.StaticObjectServlet</servlet-class>
  </servlet>
  <servlet-mapping>
    <servlet-name>StaticObjectServlet</servlet-name>
    <url-pattern>/api/objects/*</url-pattern>
  </servlet-mapping>

  <!-- Configuration -->
  <servlet>
    <servlet-name>ConfigurationServlet</servlet-name>
    <servlet-class>com.vehiclesim.servlet.ConfigurationServlet</servlet-class>
  </servlet>
  <servlet-mapping>
    <servlet-name>ConfigurationServlet</servlet-name>
    <url-pattern>/api/configuration</url-pattern>
  </servlet-mapping>

</web-app>
```

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
- If wrapping disabled: vehicle stops at boundary
- If wrapping enabled: position wraps to opposite side

**Object Collisions:**
- Simple circle-circle collision for now
- When collision detected:
  - Vehicle stops (velocity = 0)
  - Option: bounce back slightly
  - Option: trigger "collision event" for future fitness functions

**Optimization:**
- Use spatial partitioning (grid or quadtree) for large simulations
- Only check nearby objects, not all N vehicles

## Receptor Sensing

Each receptor detects brightness in an angular wedge:

**Algorithm:**
```
For each SimulationObject in arena:
  1. Calculate relative angle from vehicle to object
  2. Normalize to vehicle's reference frame (subtract vehicle.angle)
  3. Check if angle is within [receptor.angleFrom, receptor.angleTo]
  4. If yes, calculate distance
  5. If distance < receptor.maxRange:
     contribution = object.brightness × (1 - distance/maxRange) × receptor.sensitivity
  6. Sum all contributions
  7. Clamp result to [0.0, 1.0]
```

**Example:**
- Vehicle at (100, 100) facing east (angle = 0)
- Receptor: angleFrom = -π/4, angleTo = π/4 (45° cone ahead)
- Bright object at (150, 110)
- Object is within cone and range → receptor returns value > 0

**Optimization:**
- Use spatial queries to get only nearby objects
- Pre-filter by distance before angular checks

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

## Evaluation Algorithm

The network is evaluated once per clock tick using a **synchronous update** model:

```
1. Read sensor inputs
   - For each receptor:
     - sensorValue = receptor.sense(vehicle, arena)
     - if sensorValue > threshold: set INPUT neurode to TRUE
     - else: set INPUT neurode to FALSE

2. Evaluate network (fixed number of iterations to handle loops)
   - For iteration 1 to maxIterations (e.g., 10):
     a. For each neurode in topological order (or all if cyclic):
        - Check all input connections
        - If any INHIBITOR connection has firing source:
          → neurode does not fire
        - Else:
          → count firing EXCITER connections
          → if threshold mode:
            - add weights to currentCharge
            - apply decay: currentCharge *= (1 - decayRate)
            - if currentCharge >= threshold: neurode fires
          → if simple mode:
            - if any EXCITER fired: neurode fires

     b. Update neurode states for next iteration

3. Read output neurodes
   - leftMotorSpeed = LEFT_MOTOR.firing ? 1.0 : 0.0
   - rightMotorSpeed = RIGHT_MOTOR.firing ? 1.0 : 0.0
   - (Alternative: use charge value as analog output)

4. Apply motor speeds to vehicle
   - vehicle.leftMotorSpeed = leftMotorSpeed
   - vehicle.rightMotorSpeed = rightMotorSpeed
```

## Handling Cycles and Loops

**Challenge:** Neurodes can form cycles (A → B → C → A), creating feedback loops.

**Solution Options:**

### Option 1: Fixed Iterations (Recommended)
- Run network evaluation for fixed number of steps (e.g., 10)
- Allows signals to propagate through loops
- Predictable performance
- Simple to implement and parallelize

### Option 2: Convergence Detection
- Run until network state stops changing
- More accurate but unpredictable timing
- Add max iteration limit as safety

### Option 3: Async Update
- Update neurodes in random order
- More biologically realistic
- Harder to parallelize

**Recommendation:** Use Option 1 (fixed iterations) for simplicity and performance.

## Neurode Firing Modes

### Simple Binary Mode
- Neurode fires if any EXCITER is active AND no INHIBITOR is active
- No threshold, no accumulation
- Immediate response

### Threshold/Capacitor Mode
- Neurode accumulates charge from EXCITER connections (weighted)
- Charge decays each iteration: `charge *= (1 - decayRate)`
- Fires when `charge >= threshold`
- Provides temporal dynamics and memory

**Example:**
```
threshold = 2.0
decayRate = 0.3

Tick 1: exciter fires (weight 1.0) → charge = 1.0 (below threshold, no fire)
Tick 2: exciter fires (weight 1.0) → charge = 1.0×0.7 + 1.0 = 1.7 (no fire)
Tick 3: exciter fires (weight 1.0) → charge = 1.7×0.7 + 1.0 = 2.19 (FIRES!)
Tick 4: no input → charge = 2.19×0.7 = 1.53 (no fire)
```

## Neural Network Representation

**JSON Format (for API and storage):**
```json
{
  "species_id": "species_1",
  "neurodes": [
    {"id": "input_1", "type": "INPUT", "threshold": 0.5},
    {"id": "hidden_1", "type": "HIDDEN", "threshold": 1.5, "decayRate": 0.2},
    {"id": "output_left", "type": "OUTPUT", "threshold": 1.0}
  ],
  "connections": [
    {"from": "input_1", "to": "hidden_1", "type": "EXCITER", "weight": 1.0},
    {"from": "hidden_1", "to": "output_left", "type": "EXCITER", "weight": 0.8},
    {"from": "hidden_1", "to": "hidden_1", "type": "EXCITER", "weight": 0.3}
  ]
}
```

## Future Extensions

- **Analog outputs:** Use charge level (0.0 to 1.0) instead of binary for motor control
- **Learning:** Adjust weights based on fitness/rewards (genetic algorithm, reinforcement learning)
- **Neurotransmitter types:** Multiple signal types beyond excite/inhibit
- **Plasticity:** Connections strengthen/weaken during simulation

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

