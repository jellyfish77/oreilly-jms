package ch07.guaranteedmessaging;

import java.util.ArrayList;
import java.util.List;

import javax.jms.*;
import javax.naming.*;

public class JMSReceiver implements MessageListener {
	private List<String> messageBuffer = new ArrayList<String>();

	public JMSReceiver() {
		try {
			Context ctx = new InitialContext();
			QueueConnectionFactory factory = (QueueConnectionFactory) ctx.lookup("queueConnectionFactory");
			QueueConnection connection = factory.createQueueConnection();
			connection.start();
			QueueSession session = connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
			Queue queue = (Queue) ctx.lookup("TEST.Q");
			QueueReceiver receiver = session.createReceiver(queue);
			receiver.setMessageListener(this);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void onMessage(Message message) {
		try {
			if (message.propertyExists("SequenceMarker")) {
				String marker = message.getStringProperty("SequenceMarker");
				//if we are starting a message group, clear out the message buffer
				if (marker.equals("START_SEQUENCE")) {
					//since the messages are delivered and acknowledged as a group, any
					//failures will result in the first sequence message being marked as
					//being redelivered - we don't care about the others
					if (message.getJMSRedelivered()) {
						processCompensatingTransaction();
					}
					messageBuffer.clear();
				}
				//if we are ending the message group, process the message and
				//acknowledge that all messages as having been delivered
				if (marker.equals("END_SEQUENCE")) {
					//process the message
					System.out.println("Messages: ");
					for (String msg : messageBuffer) {
						System.out.println(msg);
					}
					//acknowledge that all messages have been received
					message.acknowledge();
				}
			}
			//save the message contents if it is a non-marker message
			if (message instanceof TextMessage) {
				TextMessage msg = (TextMessage) message;
				processInterimMessage(msg.getText());
			}
			//wait for the next message
			System.out.println("waiting for messages...");
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	public void processCompensatingTransaction() {
		//reverse the processing from the prior message set
		messageBuffer.clear();
	}

	public void processInterimMessage(String msg) {
		//process the interim message
		messageBuffer.add(msg);
	}

	public static void main(String argv[]) {
		new JMSReceiver();
	}
}