package io.helidon.examples.saga.travelagency;

import oracle.AQ.AQQueueTable;
import oracle.AQ.AQQueueTableProperty;
import oracle.jms.AQjmsDestinationProperty;
import oracle.jms.AQjmsFactory;
import oracle.jms.AQjmsSession;

import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.sql.DataSource;
import java.sql.Connection;

public class TravelAgencyResourceSetup {


    private void createQueuesAndTables(Connection connection, DataSource dataSource) throws Exception {
        connection.createStatement().execute(
                "create table travelagency (sagaid varchar(32), sagastate varchar(32), " +
                        "eventticketsstate varchar(32), hotelstate varchar(32), flightstate varchar(32))");
        createQueue(dataSource, "aquser1", "eventticketsqueue", "text");
        createQueue(dataSource, "aquser1", "hotelqueue", "text");
        createQueue(dataSource, "aquser1", "flightqueue", "text");
    }

    public Object createQueue(DataSource aqDataSource, String queueOwner, String queueName, String msgType) throws Exception {
        System.out.println("create queue queueName:" + queueName);
        QueueConnectionFactory q_cf = AQjmsFactory.getQueueConnectionFactory(aqDataSource);
        QueueConnection q_conn = q_cf.createQueueConnection();
        Session session = q_conn.createQueueSession(true, Session.CLIENT_ACKNOWLEDGE);
        AQQueueTable q_table = null;
        AQQueueTableProperty qt_prop =
                new AQQueueTableProperty(
                        msgType.equals("map") ? "SYS.AQ$_JMS_MAP_MESSAGE":"SYS.AQ$_JMS_TEXT_MESSAGE" )  ;
        q_table = ((AQjmsSession) session).createQueueTable(queueOwner, queueName, qt_prop);
        Queue queue = ((AQjmsSession) session).createQueue(q_table, queueName, new AQjmsDestinationProperty());
        System.out.println("create queue successful for queue:" + queue.toString());
        return "createOrderQueue successful for queue:" + queue.toString();
    }
}
