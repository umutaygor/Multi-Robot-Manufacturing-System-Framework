package milo.opcua.server;

import com.google.common.collect.ImmutableSet;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespace;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.FolderTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

public class CustomNamespace extends ManagedNamespace {
    private static final Logger logger = LoggerFactory.getLogger(CustomNamespace.class);
    public static final String URI = "urn:my:custom:namespace";

    private static final int NUM_ROBOTS = 8;
    public static List<RobotTemplate> robots = new ArrayList<>();

    static UaVariableNode pathwayProperties;
    static UaVariableNode idleProperties;
    static UaVariableNode input_conveyor_Properties;
    static UaVariableNode output_conveyor_Properties;

    static UaVariableNode conveyor1Produced;
    static UaVariableNode conveyor2Produced;
    static UaVariableNode conveyor5Produced;
    static UaVariableNode conveyor7Produced;
    static UaVariableNode conveyor9Produced;

    static UaVariableNode robotQuantity;  // Added variable

    private final SubscriptionModel subscriptionModel;

    public CustomNamespace(final OpcUaServer server, final String uri) throws Exception {
        super(server, uri);
        this.subscriptionModel = new SubscriptionModel(server, this);
        registerItems(getNodeContext());
        startBatteryLevelReduction();
    }

    private void registerItems(final UaNodeContext context) throws Exception {
        System.out.println("Registering items");

        final UaFolderNode folder = new UaFolderNode(
                context,
                newNodeId(1),
                newQualifiedName("FirstFolder"),
                LocalizedText.english("MainFolder"));
        context.getNodeManager().addNode(folder);

        final Optional<UaNode> objectsFolder = context.getServer()
                .getAddressSpaceManager()
                .getManagedNode(Identifiers.ObjectsFolder);
        objectsFolder.ifPresent(node -> ((FolderTypeNode) node).addComponent(folder));

        // Create robots
        for (int i = 1; i <= NUM_ROBOTS; i++) {
            RobotTemplate robot = createRobot(context, i);
            robots.add(robot);
        }

        // Add variables for conveyors and properties
        conveyor1Produced = createUaVariableNode(newNodeId("33-unique-identifier"), AccessLevel.READ_WRITE, AccessLevel.READ_WRITE, Identifiers.Boolean, "Conveyor 1 Produced", "Updating Conveyor 1 Produced", "Get Conveyor 1 Produced");
        conveyor2Produced = createUaVariableNode(newNodeId("34-unique-identifier"), AccessLevel.READ_WRITE, AccessLevel.READ_WRITE, Identifiers.Boolean, "Conveyor 2 Produced", "Updating Conveyor 2 Produced", "Get Conveyor 2 Produced");
        conveyor5Produced = createUaVariableNode(newNodeId("65-unique-identifier"), AccessLevel.READ_WRITE, AccessLevel.READ_WRITE, Identifiers.Boolean, "Conveyor 5 Produced", "Updating Conveyor 5 Produced", "Get Conveyor 5 Produced");
        conveyor7Produced = createUaVariableNode(newNodeId("66-unique-identifier"), AccessLevel.READ_WRITE, AccessLevel.READ_WRITE, Identifiers.Boolean, "Conveyor 7 Produced", "Updating Conveyor 7 Produced", "Get Conveyor 7 Produced");
        conveyor9Produced = createUaVariableNode(newNodeId("100-unique-identifier"), AccessLevel.READ_WRITE, AccessLevel.READ_WRITE, Identifiers.Boolean, "Conveyor 9 Produced", "Updating Conveyor 9 Produced", "Get Conveyor 9 Produced");
        pathwayProperties = createUaVariableNode(newNodeId("22-unique-identifier"), AccessLevel.READ_WRITE, AccessLevel.READ_WRITE, Identifiers.String, "Pathway Properties", "Updating the Pathway Properties", "Get the Pathway Properties");
        idleProperties = createUaVariableNode(newNodeId("23-unique-identifier"), AccessLevel.READ_WRITE, AccessLevel.READ_WRITE, Identifiers.String, "Idle Properties", "Updating the Idle Properties", "Get the Idle Properties");
        output_conveyor_Properties = createUaVariableNode(newNodeId("67-unique-identifier"), AccessLevel.READ_WRITE, AccessLevel.READ_WRITE, Identifiers.String, "Output Conveyor Properties", "Updating Output Conveyor Properties", "Get Output Conveyor Properties");
        input_conveyor_Properties = createUaVariableNode(
                newNodeId("68-unique-identifier"),  // Use a unique identifier
                AccessLevel.READ_WRITE,
                AccessLevel.READ_WRITE,
                Identifiers.String,
                "Input Conveyor Properties",
                "Updating Input Conveyor Properties",
                "Get Input Conveyor Properties"
        );
        // Create RobotQuantity variable
        robotQuantity = createUaVariableNode(newNodeId("RobotQuantity-unique-identifier"), AccessLevel.READ_WRITE, AccessLevel.READ_WRITE, Identifiers.Int32, "RobotQuantity", "Updating RobotQuantity", "Get RobotQuantity");

        // Set initial values for conveyors
        conveyor1Produced.setValue(new DataValue(new Variant(false)));
        conveyor2Produced.setValue(new DataValue(new Variant(false)));
        conveyor5Produced.setValue(new DataValue(new Variant(false)));
        conveyor7Produced.setValue(new DataValue(new Variant(false)));
        conveyor9Produced.setValue(new DataValue(new Variant(false)));

        // Set initial value for RobotQuantity
        robotQuantity.setValue(new DataValue(new Variant(NUM_ROBOTS)));

        // Read initial location properties from JSON files
        JSONParser parser = new JSONParser();
        JSONArray pathwayPropertiesArray = (JSONArray)
                parser.parse(new FileReader("PathwayInfo-trying2.json"));
        String pathwayPropertiesString = pathwayPropertiesArray.toString();
        pathwayProperties.setValue(new DataValue(new Variant(pathwayPropertiesString)));

        JSONArray idlePropertiesArray = (JSONArray) parser.parse(new FileReader("IdleInfo.json"));
        String idlePropertiesString = idlePropertiesArray.toString();
        idleProperties.setValue(new DataValue(new Variant(idlePropertiesString)));

        JSONArray input_conveyor_PropertiesArray = (JSONArray) parser.parse(new FileReader("Input_Conveyor_Info2.json"));
        String input_conveyor_PropertiesString = input_conveyor_PropertiesArray.toString();
        input_conveyor_Properties.setValue(new DataValue(new Variant(input_conveyor_PropertiesString)));

        JSONArray output_conveyor_PropertiesArray = (JSONArray) parser.parse(new FileReader("Output_Conveyor_Info2.json"));
        String output_conveyor_PropertiesString = output_conveyor_PropertiesArray.toString();
        output_conveyor_Properties.setValue(new DataValue(new Variant(output_conveyor_PropertiesString)));

        // Add nodes to folder and context
        addNodeToFolderAndContext(folder, context, conveyor1Produced);
        addNodeToFolderAndContext(folder, context, conveyor2Produced);
        addNodeToFolderAndContext(folder, context, conveyor5Produced);
        addNodeToFolderAndContext(folder, context, conveyor7Produced);
        addNodeToFolderAndContext(folder, context, conveyor9Produced);

        addNodeToFolderAndContext(folder, context, pathwayProperties);
        addNodeToFolderAndContext(folder, context, idleProperties);
        addNodeToFolderAndContext(folder, context, output_conveyor_Properties);
        addNodeToFolderAndContext(folder, context, input_conveyor_Properties);

        addNodeToFolderAndContext(folder, context, robotQuantity); // Add RobotQuantity to folder
    }

    private RobotTemplate createRobot(UaNodeContext context, int robotNumber) {
        String prefix = "Robot " + robotNumber + " ";
        UaVariableNode location = createUaVariableNode(newNodeId(robotNumber + "-location"), AccessLevel.READ_WRITE, AccessLevel.READ_WRITE, Identifiers.String, prefix + "Location", "Updating the " + prefix + "Location", "Get the " + prefix + "Location");
        UaVariableNode nextLocation = createUaVariableNode(newNodeId(robotNumber + "-nextLocation"), AccessLevel.READ_WRITE, AccessLevel.READ_WRITE, Identifiers.String, prefix + "Next Location", "Updating the " + prefix + "Next Location", "Get the " + prefix + "Next Location");
        UaVariableNode stop = createUaVariableNode(newNodeId(robotNumber + "-stop"), AccessLevel.READ_WRITE, AccessLevel.READ_WRITE, Identifiers.Boolean, prefix + "Stop", "Updating the " + prefix + "Stop", "Get the " + prefix + "Stop");
        UaVariableNode batteryLevel = createUaVariableNode(newNodeId(robotNumber + "-batteryLevel"), AccessLevel.READ_WRITE, AccessLevel.READ_WRITE, Identifiers.Int32, prefix + "Battery Level", "Updating the " + prefix + "Battery Level", "Get the " + prefix + "Battery Level");
        UaVariableNode carryingProduct = createUaVariableNode(newNodeId(robotNumber + "-carryingProduct"), AccessLevel.READ_WRITE, AccessLevel.READ_WRITE, Identifiers.Boolean, prefix + "Carrying Product", "Updating the " + prefix + "Carrying Product", "Get the " + prefix + "Carrying Product");
        UaVariableNode carriedProduct = createUaVariableNode(newNodeId(robotNumber + "-carriedProduct"), AccessLevel.READ_WRITE, AccessLevel.READ_WRITE, Identifiers.String, prefix + "Carried Product", "Updating the " + prefix + "Carried Product", "Get the " + prefix + "Carried Product"); // Added CarriedProduct node
        UaVariableNode target = createUaVariableNode(newNodeId(robotNumber + "-target"), AccessLevel.READ_WRITE, AccessLevel.READ_WRITE, Identifiers.String, prefix + "Target", "Updating the " + prefix + "Target", "Get the " + prefix + "Target");
        UaVariableNode priority = createUaVariableNode(newNodeId(robotNumber + "-priority"), AccessLevel.READ_WRITE, AccessLevel.READ_WRITE, Identifiers.Int32, prefix + "Priority", "Updating the " + prefix + "Priority", "Get the " + prefix + "Priority");
        UaVariableNode collisionDetected = createUaVariableNode(newNodeId(robotNumber + "-collisionDetected"), AccessLevel.READ_WRITE, AccessLevel.READ_WRITE, Identifiers.Boolean, prefix + "Collision Detected", "Collision Detected for " + prefix, "Collision Detected for " + prefix);
        UaVariableNode unavailablePathway = createUaVariableNode(newNodeId(robotNumber + "-unavailablePathway"), AccessLevel.READ_WRITE, AccessLevel.READ_WRITE, Identifiers.String, prefix + "Unavailable Pathway", "Unavailable Pathway for " + prefix, "Unavailable Pathway for " + prefix);

        // Set initial values
        location.setValue(new DataValue(new Variant("empty" + robotNumber)));
        nextLocation.setValue(new DataValue(new Variant("")));
        stop.setValue(new DataValue(new Variant(false)));
        batteryLevel.setValue(new DataValue(new Variant(100)));
        carryingProduct.setValue(new DataValue(new Variant(false)));
        carriedProduct.setValue(new DataValue(new Variant(""))); // Initialize CarriedProduct to empty string
        target.setValue(new DataValue(new Variant("")));
        priority.setValue(new DataValue(new Variant(NUM_ROBOTS + 1 - robotNumber)));
        collisionDetected.setValue(new DataValue(new Variant(false)));
        unavailablePathway.setValue(new DataValue(new Variant("")));

        // Add nodes to folder and context
        addNodeToFolderAndContext(context.getNodeManager().getNode(newNodeId(1)).orElseThrow(), context, location);
        addNodeToFolderAndContext(context.getNodeManager().getNode(newNodeId(1)).orElseThrow(), context, nextLocation);
        addNodeToFolderAndContext(context.getNodeManager().getNode(newNodeId(1)).orElseThrow(), context, stop);
        addNodeToFolderAndContext(context.getNodeManager().getNode(newNodeId(1)).orElseThrow(), context, batteryLevel);
        addNodeToFolderAndContext(context.getNodeManager().getNode(newNodeId(1)).orElseThrow(), context, carryingProduct);
        addNodeToFolderAndContext(context.getNodeManager().getNode(newNodeId(1)).orElseThrow(), context, carriedProduct); // Add CarriedProduct node to context
        addNodeToFolderAndContext(context.getNodeManager().getNode(newNodeId(1)).orElseThrow(), context, target);
        addNodeToFolderAndContext(context.getNodeManager().getNode(newNodeId(1)).orElseThrow(), context, priority);
        addNodeToFolderAndContext(context.getNodeManager().getNode(newNodeId(1)).orElseThrow(), context, collisionDetected);
        addNodeToFolderAndContext(context.getNodeManager().getNode(newNodeId(1)).orElseThrow(), context, unavailablePathway);

        return new RobotTemplate(location, nextLocation, stop, batteryLevel, carryingProduct, carriedProduct, target, priority, collisionDetected, unavailablePathway); // Pass carriedProduct to RobotTemplate constructor
    }

    private void addNodeToFolderAndContext(UaNode folder, UaNodeContext context, UaVariableNode node) {
        if (folder instanceof UaFolderNode) {
            ((UaFolderNode) folder).addOrganizes(node);
        }
        context.getNodeManager().addNode(node);
    }

    private void startBatteryLevelReduction() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            for (RobotTemplate robot : robots) {
                int batteryLevel = robot.getBatteryLevel();
                if (batteryLevel > 0) {
                    robot.setBatteryLevel(batteryLevel - 1);
                }
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    protected void onStartup() throws Exception {
        registerItems(getNodeContext());
        System.out.println("URI = " + URI);
    }

    @Override
    public void onDataItemsCreated(final List<DataItem> dataItems) {
        this.subscriptionModel.onDataItemsCreated(dataItems);
    }

    @Override
    public void onDataItemsModified(final List<DataItem> dataItems) {
        this.subscriptionModel.onDataItemsModified(dataItems);
    }

    @Override
    public void onDataItemsDeleted(final List<DataItem> dataItems) {
        this.subscriptionModel.onDataItemsDeleted(dataItems);
    }

    @Override
    public void onMonitoringModeChanged(final List<MonitoredItem> monitoredItems) {
        this.subscriptionModel.onMonitoringModeChanged(monitoredItems);
    }

    public UaVariableNode createUaVariableNode(NodeId nodeId, ImmutableSet<AccessLevel> accessLevel, ImmutableSet<AccessLevel> userAccessLevel, NodeId dataType, String qualifiedName, String displayName, String description) {
        UaVariableNode uaVariableNode = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                .setNodeId(nodeId)
                .setAccessLevel(accessLevel)
                .setUserAccessLevel(userAccessLevel)
                .setDataType(dataType)
                .setBrowseName(newQualifiedName(qualifiedName))
                .setDisplayName(LocalizedText.english(displayName))
                .setDescription(LocalizedText.english(description))
                .build();
        return uaVariableNode;
    }
}