package ch07.guaranteedmessaging;

import javax.jms.*;
import javax.naming.*;

public class JMSSender {
	private QueueConnection connection = null;
	private QueueSession session = null;
	private QueueSender sender = null;

	public static void main(String[] args) {
		try {
			JMSSender app = new JMSSender();
			app.sendMessageGroup();
			System.exit(0);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public JMSSender() {
		try {
			//connect to the jms provider and create the
			//connection, session, and sender
			Context ctx = new InitialContext();
			QueueConnectionFactory factory = (QueueConnectionFactory) ctx.lookup("queueConnectionFactory");
			connection = factory.createQueueConnection();
			connection.start();
			session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue queue = (Queue) ctx.lookup("TEST.Q");
			sender = session.createSender(queue);
		} catch (Exception jmse) {
			jmse.printStackTrace();
		}
	}

	public void sendMessageGroup() throws JMSException {
		//send the messages as a group
		sendSequenceMarker("START_SEQUENCE");
		sendMessage("First Message");
		sendMessage("Second Message");
		sendMessage("Third Message");
		sendSequenceMarker("END_SEQUENCE");
		connection.close();
	}

	//send a simple text message within the group of messages
	private void sendMessage(String text) throws JMSException {
		TextMessage msg = session.createTextMessage(text);
		msg.setStringProperty("JMSXGroupID", "GROUP1");
		sender.send(msg);
	}

	//send an empty payload message containing the sequence marker
	private void sendSequenceMarker(String marker) throws JMSException {
		BytesMessage msg = session.createBytesMessage();
		msg.setStringProperty("SequenceMarker", marker);
		msg.setStringProperty("JMSXGroupID", "GROUP1");
		sender.send(msg);
	}
}