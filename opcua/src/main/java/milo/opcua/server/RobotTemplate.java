package milo.opcua.server;

import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

public class RobotTemplate {
    private UaVariableNode location;
    private UaVariableNode nextLocation;
    private UaVariableNode stop;
    private UaVariableNode batteryLevel;
    private UaVariableNode carryingProduct;
    private UaVariableNode carriedProduct; // Added CarriedProduct node
    private UaVariableNode target;
    private UaVariableNode priority;
    private UaVariableNode collisionDetected;
    private UaVariableNode unavailablePathway;

    public RobotTemplate(UaVariableNode location, UaVariableNode nextLocation, UaVariableNode stop,
                         UaVariableNode batteryLevel, UaVariableNode carryingProduct, UaVariableNode carriedProduct, UaVariableNode target,
                         UaVariableNode priority, UaVariableNode collisionDetected, UaVariableNode unavailablePathway) {
        this.location = location;
        this.nextLocation = nextLocation;
        this.stop = stop;
        this.batteryLevel = batteryLevel;
        this.carryingProduct = carryingProduct;
        this.carriedProduct = carriedProduct; // Initialize CarriedProduct
        this.target = target;
        this.priority = priority;
        this.collisionDetected = collisionDetected;
        this.unavailablePathway = unavailablePathway;
    }

    public String getLocation() {
        return (String) location.getValue().getValue().getValue();
    }

    public String getNextLocation() {
        return (String) nextLocation.getValue().getValue().getValue();
    }

    public boolean isStop() {
        return (boolean) stop.getValue().getValue().getValue();
    }

    public int getBatteryLevel() {
        return (int) batteryLevel.getValue().getValue().getValue();
    }

    public boolean isCarryingProduct() {
        return (boolean) carryingProduct.getValue().getValue().getValue();
    }

    public String getCarriedProduct() {
        return (String) carriedProduct.getValue().getValue().getValue();
    }

    public String getTarget() {
        return (String) target.getValue().getValue().getValue();
    }

    public void setLocation(String value) {
        location.setValue(new DataValue(new Variant(value)));
    }

    public void setNextLocation(String value) {
        nextLocation.setValue(new DataValue(new Variant(value)));
    }

    public void setStop(boolean value) {
        stop.setValue(new DataValue(new Variant(value)));
    }

    public void setBatteryLevel(int value) {
        batteryLevel.setValue(new DataValue(new Variant(value)));
    }

    public void setCarryingProduct(boolean value) {
        carryingProduct.setValue(new DataValue(new Variant(value)));
    }

    public void setCarriedProduct(String value) {
        carriedProduct.setValue(new DataValue(new Variant(value)));
    }

    public void setTarget(String value) {
        target.setValue(new DataValue(new Variant(value)));
    }

    public int getPriority() {
        return (int) priority.getValue().getValue().getValue();
    }

    public void setPriority(int value) {
        priority.setValue(new DataValue(new Variant(value)));
    }

    public boolean isCollisionDetected() {
        return (boolean) collisionDetected.getValue().getValue().getValue();
    }

    public void setCollisionDetected(boolean value) {
        collisionDetected.setValue(new DataValue(new Variant(value)));
    }

    public String getUnavailablePathway() {
        return (String) unavailablePathway.getValue().getValue().getValue();
    }

    public void setUnavailablePathway(String value) {
        unavailablePathway.setValue(new DataValue(new Variant(value)));
    }

    static public void checkForCollisions() {
        for (int i = 0; i < CustomNamespace.robots.size(); i++) {
            RobotTemplate robotA = CustomNamespace.robots.get(i);
            for (int j = i + 1; j < CustomNamespace.robots.size(); j++) {
                RobotTemplate robotB = CustomNamespace.robots.get(j);

                // Existing collision detection: NextLocation equals NextLocation
                if (!robotA.getNextLocation().isEmpty() && robotA.getNextLocation().equals(robotB.getNextLocation())) {
                    // Collision detected
                    System.out.println("Potential collision detected between Robot " + (i + 1) + " and Robot " + (j + 1) + " at location " + robotA.getNextLocation());
                    // Determine which robot has lower priority
                    RobotTemplate lowerPriorityRobot = robotA.getPriority() > robotB.getPriority() ? robotA : robotB;
                    // Send collision information to the robot
                    lowerPriorityRobot.setCollisionDetected(true);
                    lowerPriorityRobot.setUnavailablePathway(robotA.getNextLocation());
                    lowerPriorityRobot.setStop(true);
                }

                // New collision detection: NextLocation equals another robot's Location without priority check
                if (!robotA.getNextLocation().isEmpty() && robotA.getNextLocation().equals(robotB.getLocation())) {
                    // Collision detected
                    System.out.println("Collision detected: Robot " + (i + 1) + " NextLocation equals Robot " + (j + 1) + " Location at " + robotA.getNextLocation());
                    // RobotA should reroute without priority consideration
                    robotA.setCollisionDetected(true);
                    robotA.setUnavailablePathway(robotB.getLocation());  // Set the pathway occupied by robotB as unavailable
                    robotA.setStop(true);
                }

                // Check if robotB's NextLocation equals robotA's current Location
                if (!robotB.getNextLocation().isEmpty() && robotB.getNextLocation().equals(robotA.getLocation())) {
                    // Collision detected
                    System.out.println("Collision detected: Robot " + (j + 1) + " NextLocation equals Robot " + (i + 1) + " Location at " + robotB.getNextLocation());
                    // RobotB should reroute without priority consideration
                    robotB.setCollisionDetected(true);
                    robotB.setUnavailablePathway(robotB.getNextLocation());
                    robotB.setStop(true);
                }
            }
        }
    }
}