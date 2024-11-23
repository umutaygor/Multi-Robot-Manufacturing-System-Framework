package milo.opcua.server;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.util.ExtendedProperties;
import jade.util.leap.Properties;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

public class Container {
    public static void startContainer() {


        try {
            Runtime runtime = Runtime.instance();
            Properties properties = new ExtendedProperties();
            properties.setProperty(Profile.GUI, "true");

            Profile profile = new ProfileImpl(properties);
            AgentContainer agentContainer=runtime.createMainContainer(profile);

            AgentController agent1=agentContainer.createNewAgent("RobotAgent",
                    "milo.opcua.server.RobotAgent",new Object[]{});
            agent1.start();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
