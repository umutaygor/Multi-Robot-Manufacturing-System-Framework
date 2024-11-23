package milo.opcua.server;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.ExecutionException;

public class RobotControlUI extends JFrame {
    private int numRobots = 8; // Default number of robots
    private OpcUaClient client;

    // Arrays to hold UI components for each robot
    private JLabel[] locationLabels;
    private JLabel[] nextLocationLabels;
    private JCheckBox[] stopCheckboxes;
    private JLabel[] batteryLevelLabels;
    private JCheckBox[] carryingProductCheckboxes;
    private JTextField[] targetTextFields;
    private JTextField[] priorityTextFields;

    private JCheckBox conveyor1ProducedCheckbox;
    private JCheckBox conveyor2ProducedCheckbox;
    private JCheckBox conveyor5ProducedCheckbox;
    private JCheckBox conveyor7ProducedCheckbox;

    private JTextArea pathwayPropertiesTextArea;
    private JTextArea idlePropertiesTextArea;
    private JTextArea outputConveyorPropertiesTextArea;

    // UI component to adjust Robot Quantity
    private JTextField robotQuantityField;

    // Main panels
    private JPanel robotPanel;

    public RobotControlUI(OpcUaClient client) {
        this.client = client;
        initComponents();
        startRefreshThread();
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Robot Control UI");
        setPreferredSize(new Dimension(1000, 600));

        JPanel mainPanel = new JPanel(new BorderLayout());

        // Create a panel for Robot Quantity at the top
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel robotQuantityLabel = new JLabel("Robot Quantity: ");
        robotQuantityField = new JTextField(String.valueOf(numRobots), 5);
        topPanel.add(robotQuantityLabel);
        topPanel.add(robotQuantityField);
        JButton updateQuantityButton = new JButton("Update");
        topPanel.add(updateQuantityButton);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Add action listener to update button
        updateQuantityButton.addActionListener(e -> updateRobotQuantity());

        // Create tabbed pane for better organization
        JTabbedPane tabbedPane = new JTabbedPane();

        // Robots Panel
        robotPanel = new JPanel(new GridBagLayout());
        robotPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Robots", TitledBorder.LEFT, TitledBorder.TOP));
        createRobotPanelComponents();

        // Scroll pane for robots panel
        JScrollPane robotScrollPane = new JScrollPane(robotPanel);
        tabbedPane.addTab("Robots", robotScrollPane);

        // Conveyors Panel
        JPanel conveyorPanel = new JPanel(new GridLayout(4, 2));
        conveyorPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Conveyors", TitledBorder.LEFT, TitledBorder.TOP));

        conveyor1ProducedCheckbox = new JCheckBox("Conveyor 1 Produced");
        conveyor2ProducedCheckbox = new JCheckBox("Conveyor 2 Produced");
        conveyor5ProducedCheckbox = new JCheckBox("Conveyor 5 Produced");
        conveyor7ProducedCheckbox = new JCheckBox("Conveyor 7 Produced");

        conveyor1ProducedCheckbox.addActionListener(e -> setConveyorProduced("Conveyor1", conveyor1ProducedCheckbox.isSelected()));

        conveyor2ProducedCheckbox.addActionListener(e -> setConveyorProduced("Conveyor2", conveyor2ProducedCheckbox.isSelected()));

        conveyor5ProducedCheckbox.addActionListener(e -> setConveyorProduced("Conveyor5", conveyor5ProducedCheckbox.isSelected()));

        conveyor7ProducedCheckbox.addActionListener(e -> setConveyorProduced("Conveyor7", conveyor7ProducedCheckbox.isSelected()));

        conveyorPanel.add(conveyor1ProducedCheckbox);
        conveyorPanel.add(conveyor2ProducedCheckbox);
        conveyorPanel.add(conveyor5ProducedCheckbox);
        conveyorPanel.add(conveyor7ProducedCheckbox);

        tabbedPane.addTab("Conveyors", conveyorPanel);

        // Properties Panel
        JPanel propertiesPanel = new JPanel(new GridLayout(3, 1));
        propertiesPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Properties", TitledBorder.LEFT, TitledBorder.TOP));

        pathwayPropertiesTextArea = new JTextArea();
        idlePropertiesTextArea = new JTextArea();
        outputConveyorPropertiesTextArea = new JTextArea();

        pathwayPropertiesTextArea.setEditable(false);
        idlePropertiesTextArea.setEditable(false);
        outputConveyorPropertiesTextArea.setEditable(false);

        propertiesPanel.add(createTitledPanel("Pathway Properties", pathwayPropertiesTextArea));
        propertiesPanel.add(createTitledPanel("Idle Properties", idlePropertiesTextArea));
        propertiesPanel.add(createTitledPanel("Output Conveyor Properties", outputConveyorPropertiesTextArea));

        tabbedPane.addTab("Properties", new JScrollPane(propertiesPanel));

        // Add TabbedPane to Main Panel
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        add(mainPanel);
        pack();
        setLocationRelativeTo(null);
    }

    private void createRobotPanelComponents() {
        robotPanel.removeAll();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Add padding around components
        gbc.insets = new Insets(5, 5, 5, 5); // Insets(top, left, bottom, right)

        // Header Labels
        String[] headers = {"Robot", "Location", "Next Location", "Stop", "Battery Level", "Carrying Product", "Target", "Priority"};
        for (int i = 0; i < headers.length; i++) {
            gbc.gridx = i;
            gbc.gridy = 0;
            JLabel headerLabel = new JLabel(headers[i], SwingConstants.CENTER);
            headerLabel.setFont(new Font("Arial", Font.BOLD, 12));
            robotPanel.add(headerLabel, gbc);
        }

        // Initialize arrays based on numRobots
        locationLabels = new JLabel[numRobots];
        nextLocationLabels = new JLabel[numRobots];
        stopCheckboxes = new JCheckBox[numRobots];
        batteryLevelLabels = new JLabel[numRobots];
        carryingProductCheckboxes = new JCheckBox[numRobots];
        targetTextFields = new JTextField[numRobots];
        priorityTextFields = new JTextField[numRobots];

        // Robot details
        for (int i = 0; i < numRobots; i++) {
            int robotNumber = i + 1;

            gbc.gridy = i + 1;
            gbc.fill = GridBagConstraints.BOTH;

            // Robot Name
            gbc.gridx = 0;
            JLabel robotLabel = new JLabel("Robot " + robotNumber, SwingConstants.CENTER);
            robotPanel.add(robotLabel, gbc);

            // Location Label
            gbc.gridx = 1;
            locationLabels[i] = new JLabel("", SwingConstants.CENTER);
            robotPanel.add(locationLabels[i], gbc);

            // Next Location Label
            gbc.gridx = 2;
            nextLocationLabels[i] = new JLabel("", SwingConstants.CENTER);
            robotPanel.add(nextLocationLabels[i], gbc);

            // Stop Checkbox
            gbc.gridx = 3;
            stopCheckboxes[i] = new JCheckBox();
            robotPanel.add(stopCheckboxes[i], gbc);

            // Battery Level Label
            gbc.gridx = 4;
            batteryLevelLabels[i] = new JLabel("", SwingConstants.CENTER);
            robotPanel.add(batteryLevelLabels[i], gbc);

            // Carrying Product Checkbox
            gbc.gridx = 5;
            carryingProductCheckboxes[i] = new JCheckBox();
            carryingProductCheckboxes[i].setEnabled(false); // Read-only
            robotPanel.add(carryingProductCheckboxes[i], gbc);

            // Target TextField
            gbc.gridx = 6;
            targetTextFields[i] = new JTextField(10);
            robotPanel.add(targetTextFields[i], gbc);

            // Priority TextField
            gbc.gridx = 7;
            priorityTextFields[i] = new JTextField(5);
            robotPanel.add(priorityTextFields[i], gbc);

            // Action Listeners
            int index = i;
            stopCheckboxes[i].addActionListener(e -> setRobotStop(robotNumber, stopCheckboxes[index].isSelected()));

            targetTextFields[i].addActionListener(e -> setRobotTarget(robotNumber, targetTextFields[index].getText()));

            priorityTextFields[i].getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    updatePriority(robotNumber);
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    updatePriority(robotNumber);
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    updatePriority(robotNumber);
                }
            });
        }

        robotPanel.revalidate();
        robotPanel.repaint();
    }

    private JPanel createTitledPanel(String title, JTextArea textArea) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void startRefreshThread() {
        Thread refreshThread = new Thread(() -> {
            while (true) {
                refreshData();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        refreshThread.start();
    }

    private void refreshData() {
        try {
            for (int i = 0; i < numRobots; i++) {
                int robotNumber = i + 1;
                String locationNodeId = robotNumber + "-location";
                String nextLocationNodeId = robotNumber + "-nextLocation";
                String stopNodeId = robotNumber + "-stop";
                String batteryLevelNodeId = robotNumber + "-batteryLevel";
                String carryingProductNodeId = robotNumber + "-carryingProduct";
                String targetNodeId = robotNumber + "-target";
                String priorityNodeId = robotNumber + "-priority";

                String location = readStringValue(locationNodeId);
                String nextLocation = readStringValue(nextLocationNodeId);
                boolean stop = readBooleanValue(stopNodeId);
                int batteryLevel = readIntValue(batteryLevelNodeId);
                boolean carryingProduct = readBooleanValue(carryingProductNodeId);
                String target = readStringValue(targetNodeId);
                int priority = readIntValue(priorityNodeId);

                locationLabels[i].setText(location);
                nextLocationLabels[i].setText(nextLocation);
                stopCheckboxes[i].setSelected(stop);
                batteryLevelLabels[i].setText(batteryLevel + "%");
                carryingProductCheckboxes[i].setSelected(carryingProduct);
                targetTextFields[i].setText(target);
                priorityTextFields[i].setText(String.valueOf(priority));
            }

            boolean conveyor1Produced = readBooleanValue("33-unique-identifier");
            boolean conveyor2Produced = readBooleanValue("34-unique-identifier");
            boolean conveyor5Produced = readBooleanValue("65-unique-identifier");
            boolean conveyor7Produced = readBooleanValue("66-unique-identifier");

            conveyor1ProducedCheckbox.setSelected(conveyor1Produced);
            conveyor2ProducedCheckbox.setSelected(conveyor2Produced);
            conveyor5ProducedCheckbox.setSelected(conveyor5Produced);
            conveyor7ProducedCheckbox.setSelected(conveyor7Produced);

            String pathwayProperties = readStringValue("22-unique-identifier");
            String idleProperties = readStringValue("23-unique-identifier");
            String outputConveyorProperties = readStringValue("67-unique-identifier");

            pathwayPropertiesTextArea.setText(pathwayProperties);
            idlePropertiesTextArea.setText(idleProperties);
            outputConveyorPropertiesTextArea.setText(outputConveyorProperties);

            // Update robot quantity field based on server value
            int serverRobotQuantity = readIntValue("RobotQuantity-unique-identifier");
            if (serverRobotQuantity != numRobots) {
                numRobots = serverRobotQuantity;
                robotQuantityField.setText(String.valueOf(numRobots));
                createRobotPanelComponents();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String readStringValue(String nodeId) throws ExecutionException, InterruptedException {
        NodeId node = new NodeId(2, nodeId);
        DataValue value = client.readValue(0, TimestampsToReturn.Neither, node).get();
        return (String) value.getValue().getValue();
    }

    private boolean readBooleanValue(String nodeId) throws ExecutionException, InterruptedException {
        NodeId node = new NodeId(2, nodeId);
        DataValue value = client.readValue(0, TimestampsToReturn.Neither, node).get();
        return (boolean) value.getValue().getValue();
    }

    private int readIntValue(String nodeId) throws ExecutionException, InterruptedException {
        NodeId node = new NodeId(2, nodeId);
        DataValue value = client.readValue(0, TimestampsToReturn.Neither, node).get();
        return ((Number) value.getValue().getValue()).intValue();
    }

    private void setRobotStop(int robotNumber, boolean stop) {
        String nodeId = robotNumber + "-stop";
        writeValue(nodeId, stop);
    }

    private void setRobotTarget(int robotNumber, String target) {
        String nodeId = robotNumber + "-target";
        writeValue(nodeId, target);
    }

    private void updatePriority(int robotNumber) {
        String priorityText = priorityTextFields[robotNumber - 1].getText();
        try {
            int priority = Integer.parseInt(priorityText);
            String nodeId = robotNumber + "-priority";
            writeValue(nodeId, priority);
        } catch (NumberFormatException e) {
            // Invalid input, ignore
        }
    }

    private void setConveyorProduced(String conveyor, boolean produced) {
        String nodeId = "";
        switch (conveyor) {
            case "Conveyor1":
                nodeId = "33-unique-identifier";
                break;
            case "Conveyor2":
                nodeId = "34-unique-identifier";
                break;
            case "Conveyor5":
                nodeId = "65-unique-identifier";
                break;
            case "Conveyor7":
                nodeId = "66-unique-identifier";
                break;
        }
        writeValue(nodeId, produced);
    }

    private void writeValue(String nodeId, Object value) {
        try {
            NodeId node = new NodeId(2, nodeId);
            DataValue dataValue = new DataValue(new Variant(value));
            client.writeValue(node, dataValue).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateRobotQuantity() {
        try {
            int newQuantity = Integer.parseInt(robotQuantityField.getText());
            if (newQuantity <= 0) {
                JOptionPane.showMessageDialog(this, "Robot quantity must be a positive integer.");
                return;
            }
            numRobots = newQuantity;
            writeValue("RobotQuantity-unique-identifier", numRobots);
            createRobotPanelComponents();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid robot quantity. Please enter a valid integer.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                OpcUaClient client = OpcUaClient.create("opc.tcp://localhost:4840");
                client.connect().get();
                RobotControlUI ui = new RobotControlUI(client);
                ui.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}