package milo.opcua.server;

import jade.core.Agent;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.TickerBehaviour;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RobotAgent extends Agent {
    private double calculateDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    protected void setup() {
        System.out.println("Agent " + getLocalName() + " started.");
        ParallelBehaviour parallelBehaviour = new ParallelBehaviour();
        parallelBehaviour.addSubBehaviour(receiverBehaviour);
        addBehaviour(parallelBehaviour);
    }


    TickerBehaviour receiverBehaviour = new TickerBehaviour(this, 500) {
        @Override
        public void onTick() {
            // Print robot locations
            for (int i = 0; i < CustomNamespace.robots.size(); i++) {
                RobotTemplate robot = CustomNamespace.robots.get(i);
                System.out.println("Robot " + (i + 1) + " Location: " + robot.getLocation() + ", Next Location: " + robot.getNextLocation());
            }

            // Check for collisions
            RobotTemplate.checkForCollisions();

            // Resume robots if they were previously stopped and their path is clear
            for (RobotTemplate robot : CustomNamespace.robots) {
                resumeRobotIfPossible(robot);
            }

            // Handle conveyor production and set robot targets
            checkConveyorProduction();

            // Check if robots have picked up products from conveyors
            for (RobotTemplate robot : CustomNamespace.robots) {
                checkProductPickup(robot);
            }

            // Check if robots need to go to drop-off locations, return to idle locations, or seek new targets
            for (RobotTemplate robot : CustomNamespace.robots) {
                checkAndSetNewTarget(robot);
            }
        }
    };

    private void resumeRobotIfPossible(RobotTemplate robot) {
        if (robot.isStop()) {
            boolean canResume = true;
            for (RobotTemplate otherRobot : CustomNamespace.robots) {
                if (otherRobot != robot && otherRobot.getNextLocation().equals(robot.getLocation())) {
                    canResume = false;
                    break;
                }
            }
            if (canResume) {
                System.out.println("Resuming robot.");
                robot.setStop(false);
            }
        }
    }

    private void checkConveyorProduction() {
        if ((boolean) CustomNamespace.conveyor1Produced.getValue().getValue().getValue()) {
            setRobotTarget("InputConveyor");
        }
        if ((boolean) CustomNamespace.conveyor2Produced.getValue().getValue().getValue()) {
            setRobotTarget("InputConveyor #2");
        }
        if ((boolean) CustomNamespace.conveyor5Produced.getValue().getValue().getValue()) {
            setRobotTarget("InputConveyor #3");
        }
        if ((boolean) CustomNamespace.conveyor7Produced.getValue().getValue().getValue()) {
            setRobotTarget("InputConveyor #4");
        }
    }

    private void setRobotTarget(String target) {
        // First, check if any robot is already assigned to this target
        for (RobotTemplate robot : CustomNamespace.robots) {
            if (robot.getTarget().equals(target) && !robot.getLocation().startsWith("Idle Location")) {
                return; // Only return if the robot is actively pursuing this target
            }
        }

        try {
            // Parse input conveyor locations and idle locations
            JSONParser parser = new JSONParser();
            JSONArray inputConveyorLocations = (JSONArray) parser.parse(new FileReader("Input_Conveyor_Info.json"));
            JSONArray idleLocations = (JSONArray) parser.parse(new FileReader("IdleInfo.json"));

            // Find the target conveyor coordinates
            JSONObject targetConveyor = null;
            int conveyorIndex = -1;

            if (target.equals("InputConveyor")) conveyorIndex = 0;
            else if (target.equals("InputConveyor #2")) conveyorIndex = 1;
            else if (target.equals("InputConveyor #3")) conveyorIndex = 2;
            else if (target.equals("InputConveyor #4")) conveyorIndex = 3;

            if (conveyorIndex >= 0 && conveyorIndex < inputConveyorLocations.size()) {
                targetConveyor = (JSONObject) inputConveyorLocations.get(conveyorIndex);
            }

            if (targetConveyor == null) return;

            // Find closest available robot
            RobotTemplate closestRobot = null;
            double minDistance = Double.MAX_VALUE;

            for (RobotTemplate robot : CustomNamespace.robots) {
                if (robot.getTarget().isEmpty() && !robot.isCarryingProduct()) {
                    int robotIndex = CustomNamespace.robots.indexOf(robot);
                    JSONObject robotLocation = (JSONObject) idleLocations.get(robotIndex);

                    double distance = calculateDistance(
                            ((Number) robotLocation.get("X")).doubleValue(),
                            ((Number) robotLocation.get("Y")).doubleValue(),
                            ((Number) targetConveyor.get("X")).doubleValue(),
                            ((Number) targetConveyor.get("Y")).doubleValue()
                    );

                    if (distance < minDistance) {
                        minDistance = distance;
                        closestRobot = robot;
                    }
                }
            }

            // Assign target to closest robot if one was found
            if (closestRobot != null) {
                closestRobot.setTarget(target);
                System.out.println("Set target " + target + " for closest robot at distance " + minDistance);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkProductPickup(RobotTemplate robot) {
        String location = robot.getLocation();
        boolean isCarryingProduct = robot.isCarryingProduct();

        if (location.equals("InputConveyor")) {
            boolean conveyor1Produced = (boolean) CustomNamespace.conveyor1Produced.getValue().getValue().getValue();
            System.out.println(conveyor1Produced);
            if (conveyor1Produced) {

                CustomNamespace.conveyor1Produced.setValue(new DataValue(new Variant(false)));

            }
        } else if (location.equals("InputConveyor #2")) {
            boolean conveyor2Produced = (boolean) CustomNamespace.conveyor2Produced.getValue().getValue().getValue();
            if (conveyor2Produced) {
                robot.setCarryingProduct(true);
                CustomNamespace.conveyor2Produced.setValue(new DataValue(new Variant(false)));

            }
        } else if (location.equals("InputConveyor #3")) {
            boolean conveyor5Produced = (boolean) CustomNamespace.conveyor5Produced.getValue().getValue().getValue();
            if (conveyor5Produced) {
                robot.setCarryingProduct(true);
                CustomNamespace.conveyor5Produced.setValue(new DataValue(new Variant(false)));

            }
        } else if (location.equals("InputConveyor #4")) {
            boolean conveyor7Produced = (boolean) CustomNamespace.conveyor7Produced.getValue().getValue().getValue();
            if (conveyor7Produced) {
                robot.setCarryingProduct(true);
                CustomNamespace.conveyor7Produced.setValue(new DataValue(new Variant(false)));

            }
        }
    }


    private boolean dropOffConveyorNamesContains(String location) {
        try {
            String outputConveyorPropertiesString = CustomNamespace.output_conveyor_Properties.getValue().getValue().getValue().toString();
            JSONParser parser = new JSONParser();
            JSONArray outputConveyorsArray = (JSONArray) parser.parse(outputConveyorPropertiesString);
            for (Object o : outputConveyorsArray) {
                JSONObject conveyor = (JSONObject) o;
                String name = (String) conveyor.get("Name");
                if (name.equals(location)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    private void checkAndSetNewTarget(RobotTemplate robot) {
        boolean isCarryingProduct = robot.isCarryingProduct();
        String currentTarget = robot.getTarget();
        String currentLocation = robot.getLocation();
        int robotIndex = CustomNamespace.robots.indexOf(robot);

        // When robot is carrying a product and needs a drop-off location
        if (isCarryingProduct && (currentTarget.equals("InputConveyor") ||
                currentTarget.equals("InputConveyor #2") ||
                currentTarget.equals("InputConveyor #3") ||
                currentTarget.equals("InputConveyor #4") ||
                currentTarget.isEmpty())) {

            try {
                // Get the JSON string from output_conveyor_Properties
                String outputConveyorPropertiesString = CustomNamespace.output_conveyor_Properties.getValue().getValue().getValue().toString();
                // Parse the JSON string into a JSONArray
                JSONParser parser = new JSONParser();
                JSONArray outputConveyorsArray = (JSONArray) parser.parse(outputConveyorPropertiesString);
                // Extract Names into a List
                List<String> dropOffConveyors = new ArrayList<>();
                for (Object o : outputConveyorsArray) {
                    JSONObject conveyor = (JSONObject) o;
                    String name = (String) conveyor.get("Name");
                    dropOffConveyors.add(name);
                }
                // Randomly select one conveyor name
                if (!dropOffConveyors.isEmpty()) {
                    Random rand = new Random();
                    String dropOffTarget = dropOffConveyors.get(rand.nextInt(dropOffConveyors.size()));
                    robot.setTarget(dropOffTarget);
                    System.out.println("Set drop-off target " + dropOffTarget + " for robot");
                } else {
                    System.out.println("No drop-off conveyors available.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        // When robot has dropped off product and needs to return to idle location
        else if (!isCarryingProduct && dropOffConveyorNamesContains(currentLocation)) {
            // Set target to corresponding idle location
            String idleLocation = "Idle Location" + (robotIndex == 0 ? "" : " #" + (robotIndex + 1));
            robot.setTarget(idleLocation);
            System.out.println("Robot returning to idle location: " + idleLocation);
        }
        // When robot has reached its idle location
        else if (currentLocation.startsWith("Idle Location")) {
            if (currentTarget.startsWith("Idle Location")) {
                robot.setTarget("");
                System.out.println("Robot reached idle location. Ready for new tasks.");
                checkConveyorProduction();
            }
        }
    }
}