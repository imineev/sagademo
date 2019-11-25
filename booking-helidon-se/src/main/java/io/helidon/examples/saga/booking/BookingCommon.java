package io.helidon.examples.saga.booking;

import io.helidon.messaging.MessageWithConnectionAndSession;
import io.helidon.messaging.MessagingClient;
import io.helidon.messaging.ProcessingMessagingService;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Message;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletionStage;

abstract class BookingCommon {
    MessagingClient messagingClient;
    Properties configProps;

    public BookingCommon() throws Exception {
        setConfig();
        messagingClient = MessagingClient.build(createConfig());
        setupMessaging();
    }

    public Properties setConfig() throws Exception {
        configProps = new Properties();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("microprofile-config.properties");
        configProps.load(inputStream);
        return configProps;
    }

    private Config createConfig() throws Exception {
        return new Config() {
            public <T> T getValue(String propertyName, Class<T> propertyType) {
                return (T) configProps.get(propertyName);
            }
            public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
                return Optional.empty();
            }
            public Iterable<String> getPropertyNames() {
                return configProps.stringPropertyNames();
            }
            public Iterable<ConfigSource> getConfigSources() {
                return null;
            }
        };
    }

    void setupMessaging()  {
        ProcessingMessagingService processingMessagingService = (message, connection, session) -> {
            MessageWithConnectionAndSession messageWithConnectionAndSession =
                    (MessageWithConnectionAndSession) message.unwrap(javax.jms.Message.class);
            javax.jms.Message jmsMessage = messageWithConnectionAndSession.getPayload();
            String action = jmsMessage.getStringProperty("action");
            String sagaId = jmsMessage.getStringProperty("sagaid");
            Boolean isfailtest = jmsMessage.getBooleanProperty("failtest");
            System.out.println("Booking Service incoming jmsMessage " +
                    "sagaid:" + sagaId + " action:" + action + " isfailtest:" + isfailtest);
            String replyMessageAction = processIncomingMessage(connection, action, isfailtest);
            return getReplyMessage(session, sagaId, replyMessageAction);
        };
        messagingClient.incomingoutgoing( processingMessagingService,
                BookingService.serviceName, BookingService.serviceName,
                Acknowledgment.Strategy.NONE,  true);
        System.out.println("Waiting for booking requests...");
    }

    abstract String processIncomingMessage(Connection connection, String action, boolean isFailTest) throws SQLException;

    abstract void updateDataInReactionToMessage(Connection connection, String sagacompletesuccess) throws SQLException;

    Message getReplyMessage(Session session, String sagaId, String replyMessageAction) {
        try {
            TextMessage textMessage = session.createTextMessage(
                    "booking result for " + BookingService.serviceName + " replyMessageAction:" + replyMessageAction);
            textMessage.setStringProperty("action", replyMessageAction);
            textMessage.setStringProperty("sagaid", sagaId);
            textMessage.setStringProperty("service", BookingService.serviceName);
            return new Message() {
                @Override
                public Object getPayload() {
                    return "booking result";
                }

                @Override
                public CompletionStage<Void> ack() {
                    return null;
                }

                @Override
                public Object unwrap(Class unwrapType) {
                    return textMessage;
                }
            };
        } catch (JMSException e) {
            e.printStackTrace();
            return null; //todo
        }
    }

}

