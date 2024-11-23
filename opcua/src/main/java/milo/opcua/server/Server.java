package milo.opcua.server;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfigBuilder;
import org.eclipse.milo.opcua.sdk.server.identity.AnonymousIdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.CompositeValidator;
import org.eclipse.milo.opcua.sdk.server.util.HostnameUtil;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;
import org.eclipse.milo.opcua.stack.server.EndpointConfiguration;
import org.eclipse.milo.opcua.stack.server.security.ServerCertificateValidator;

import javafx.application.Application;

import javax.swing.*;
import java.security.cert.X509Certificate;
import java.util.List;

import static java.util.Collections.singleton;
import static org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText.english;

public class Server {

    private static final int PORT = 4840;
    private static final String SERVER_NAME = "Simple OPC UA Server";

    public static void main(final String[] args) throws Exception {
        // Start the OPC UA server
        OpcUaServer server = startOpcUaServer();

        // Wait for the server to fully initialize
        Thread.sleep(2000);

        // Create the OPC UA client
        OpcUaClient client = OpcUaClient.create("opc.tcp://localhost:4840");
        client.connect().get();

        // Start the UI in a separate thread
        SwingUtilities.invokeLater(() -> {
            RobotControlUI ui = new RobotControlUI(client);
            ui.setVisible(true);
        });

        // Start the container
        Container.startContainer();

        // Wait indefinitely
        Thread.sleep(Long.MAX_VALUE);
    }




    private static OpcUaServer startOpcUaServer() throws Exception {
        final OpcUaServerConfigBuilder builder = new OpcUaServerConfigBuilder();

        builder.setIdentityValidator(new CompositeValidator(
                AnonymousIdentityValidator.INSTANCE
        ));
        final EndpointConfiguration.Builder endpointBuilder = new EndpointConfiguration.Builder();
        endpointBuilder.addTokenPolicies(
                OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS
        );
        endpointBuilder.setSecurityPolicy(SecurityPolicy.None);
        endpointBuilder.setBindPort(PORT);
        builder.setEndpoints(singleton(endpointBuilder.build()));
        builder.setApplicationName(english(SERVER_NAME));
        builder.setApplicationUri("urn:" + HostnameUtil.getHostname() + ":" + PORT + "/" + SERVER_NAME);
        builder.setBuildInfo(new BuildInfo("", "", "", "", "", new DateTime()));
        builder.setCertificateManager(new DefaultCertificateManager());

        builder.setCertificateValidator(new ServerCertificateValidator() {
            @Override
            public void validateCertificateChain(List<X509Certificate> list, String s) throws UaException {
            }

            @Override
            public void validateCertificateChain(List<X509Certificate> list) throws UaException {
            }
        });

        final OpcUaServer server = new OpcUaServer(builder.build());

        // register namespace
        server.getAddressSpaceManager().register(new CustomNamespace(server, CustomNamespace.URI));

        // start it up
        server.startup().get();

        System.out.println("server = " + server.getConfig().getBuildInfo());

        return server;
    }
}
