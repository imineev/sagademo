package io.helidon.examples.saga.travelagency;


import io.helidon.messaging.IncomingMessagingService;
import io.helidon.messaging.MessageWithConnectionAndSession;
import io.helidon.messaging.MessagingClient;
import oracle.jdbc.pool.OracleDataSource;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;

import javax.jms.Message;
import javax.jms.Session;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Optional;

abstract class TravelAgencyCommon {
    MessagingClient messagingClient;
    String sagaid;
    static String sagaState = null;
    String eventticketsstate = "unknown";
    String hotelstate = "unknown";
    String flightstate = "unknown";
    static String sagaId;
    Connection connection;

    TravelAgencyCommon(String sagaid) throws Exception  {
        setTravelAgencyConnection();
        messagingClient = MessagingClient.build(createConfig());
        this.sagaid = sagaid;
    }

    private void setTravelAgencyConnection() throws SQLException {
        OracleDataSource dataSource = new OracleDataSource();
        dataSource.setURL(TravelAgencyService.url);
        dataSource.setUser(TravelAgencyService.user);
        dataSource.setPassword(TravelAgencyService.password);
        connection = dataSource.getConnection();
    }

    private Config createConfig() {
        return new Config() {
            HashMap<String, String> values = new HashMap<>();
            String connectorName = "aq";
            private void createValues() {
                values.put("mp.messaging.connector."+connectorName+".classname",
                        "io.helidon.messaging.jms.connector.JMSConnector");
                createValuesForChannel(TravelAgencyService.eventtickets, "mp.messaging.incoming.");
                createValuesForChannel(TravelAgencyService.eventtickets, "mp.messaging.outgoing.");
                createValuesForChannel(TravelAgencyService.hotel, "mp.messaging.incoming.");
                createValuesForChannel(TravelAgencyService.hotel, "mp.messaging.outgoing.");
                createValuesForChannel(TravelAgencyService.flight, "mp.messaging.incoming.");
                createValuesForChannel(TravelAgencyService.flight, "mp.messaging.outgoing.");
            }

            private void createValuesForChannel(String channelname, String incomingoroutgoingprefix) {
                values.put(incomingoroutgoingprefix + channelname + ".connector", connectorName);
                values.put(incomingoroutgoingprefix + channelname + ".url", TravelAgencyService.url);
                values.put(incomingoroutgoingprefix + channelname + ".user", TravelAgencyService.user);
                values.put(incomingoroutgoingprefix + channelname + ".password", TravelAgencyService.password);
                values.put(incomingoroutgoingprefix + channelname + ".queue", channelname + "queue");
                if (incomingoroutgoingprefix.equals("mp.messaging.incoming."))
                    values.put(incomingoroutgoingprefix + channelname + ".selector",
                            "action = '" + TravelAgencyService.BOOKINGSUCCESS + "' OR " +
                            "action = '" + TravelAgencyService.BOOKINGFAIL + "' OR " +
                            //the following are not actually necessary for compensation in db case...
                            "action = '" + TravelAgencyService.SAGACOMPLETESUCCESS + "' OR " +
                            "action = '" + TravelAgencyService.SAGACOMPLETEFAIL + "' OR " +
                            "action = '" + TravelAgencyService.SAGACOMPENSATESUCCESS + "' OR " +
                            "action = '" + TravelAgencyService.SAGACOMPENSATEFAIL + "' "
                            );
            }

            @Override
            public <T> T getValue(String propertyName, Class<T> propertyType) {
                createValues();
                return (T) values.get(propertyName);
            }

            @Override
            public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
                createValues();
                return Optional.empty();
            }

            @Override
            public Iterable<String> getPropertyNames() {
                createValues();
                return values.keySet();
            }

            @Override
            public Iterable<ConfigSource> getConfigSources() {
                return null;
            }
        };

    }


    void setupMessaging() {
        IncomingMessagingService incomingMessagingService =
                new IncomingMessagingService() {
                    @Override
                    public void onIncoming(org.eclipse.microprofile.reactive.messaging.Message message, Connection connection, Session session) {
                        MessageWithConnectionAndSession messageWithConnectionAndSession = (MessageWithConnectionAndSession) message.unwrap(Message.class);
                        try {
                            Message jmsMessage = messageWithConnectionAndSession.getPayload();
                            String sagaid = jmsMessage.getStringProperty("sagaid");
                            String service = jmsMessage.getStringProperty("service");
                            String action = jmsMessage.getStringProperty("action");
                            System.out.println("AQ IncomingMessagingService.onIncoming " +
                                    "sagaid:" + sagaid + "message:" + message +
                                    " bookingService:" + service + " action/reply:" + action);
                            switch (service) {
                                case TravelAgencyService.EVENTTICKETS:
                                    eventticketsstate = action;
                                    break;
                                case TravelAgencyService.HOTEL:
                                    hotelstate = action;
                                    break;
                                case TravelAgencyService.FLIGHT:
                                    flightstate = action;
                                    break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
        messagingClient.incoming(incomingMessagingService, TravelAgencyService.eventtickets,
                null, true);
        messagingClient.incoming(incomingMessagingService, TravelAgencyService.hotel,
                null, true);
        messagingClient.incoming(incomingMessagingService, TravelAgencyService.flight,
                null, true);
    }


    abstract void updateDataInReactionToMessage(Connection connection, String service, String action) throws SQLException;

    abstract String processTripBookingRequest();

    abstract boolean beginSaga() throws SQLException;

    abstract void sendMessageToBookingService(String bookingService, String action);

    boolean allParticipantsReplySuccessfully(String success, String fail, int secondsToWait) {
        long timeMillis = System.currentTimeMillis();
        while (System.currentTimeMillis() - timeMillis < secondsToWait * 1000) {
            try {
                System.out.println("Participant reply status... " +
                        " eventticketsstate:" + eventticketsstate +
                        " hotelstate:" + hotelstate +
                        " flightstate:" + flightstate);
                    if(success.equals(eventticketsstate)
                            && success.equals(hotelstate)
                            && success.equals(flightstate)) {
                        return true;
                    } else if(fail.equals(eventticketsstate)
                            && fail.equals(hotelstate)
                            && fail.equals(flightstate)) {
                        return false;
                    }
                Thread.sleep(1 * 1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }



}
