package ch02.chat;

import java.io.*;
import javax.jms.*;
import javax.naming.*;

public class Chat implements javax.jms.MessageListener {
	private TopicSession pubSession;
	private TopicPublisher publisher;
	private TopicConnection connection;
	private String username;
	
	/* Constructor used to Initialize Chat */
	public Chat(String topicFactory, String topicName, String username)	throws Exception {

		// Initialize environment of InitalContext from jndi.properties file
		InitialContext ctx = new InitialContext();
		
		// Look up a JMS connection factory 
		//   
		TopicConnectionFactory conFactory = (TopicConnectionFactory)ctx.lookup(topicFactory);
		// Create a JNDI connection
		TopicConnection connection = conFactory.createTopicConnection();
		
		// Create two JMS session objects
		TopicSession pubSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
		TopicSession subSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
		
		// Look up a JMS topic
		Topic chatTopic = (Topic)ctx.lookup(topicName);
		
		// Create a JMS publisher and subscriber. The additional parameters
		// on the createSubscriber are a message selector (null) and a true
		// value for the noLocal flag indicating that messages produced from
		// this publisher should not be consumed by this publisher.
		TopicPublisher publisher = pubSession.createPublisher(chatTopic);
		TopicSubscriber subscriber = subSession.createSubscriber(chatTopic, null, true);
		
		// Set a JMS message listener
		subscriber.setMessageListener(this);
		
		// Initialize the Chat application variables
		this.connection = connection;
		this.pubSession = pubSession;
		this.publisher = publisher;
		this.username = username;
		
		// Start the JMS connection; allows messages to be delivered
		connection.start();
	}
	
	/* Receive Messages From Topic Subscriber */
	public void onMessage(Message message) {
		//System.out.print("Received message from Topic: ");
		try {
			TextMessage textMessage = (TextMessage) message;			
			System.out.println("\n" + textMessage.getText());			
			System.out.print(this.username + "*: ");
		} 
		catch (JMSException jmse) { jmse.printStackTrace(); }		
	}
	
	/* Create and Send Message Using Publisher */
	protected void writeMessage(String text) throws JMSException {
		TextMessage message = pubSession.createTextMessage();
		message.setText(username+": "+text);
		//System.out.println("Publishing message ("+text+") to topic specified...");
		publisher.publish(message);
	}
		/* Close the JMS Connection */
	public void close() throws JMSException {
		connection.close();
	}
		/* Run the Chat Client */
	public static void main(String [] args) {
		try {
			Chat chat;
			if (args.length!=3) {
				// use default arguments
				System.out.println("Factory, Topic, or username missing, using defaults...");
				chat = new Chat("OttoActiveMQTopicConnectionFactory","testTopic","");
			} else {
				// use arguments specified							
				//args[0]=topicFactory; args[1]=topicName; args[2]=username
				chat = new Chat(args[0],args[1],args[2]);
			}
			
			// Read from command line
			BufferedReader commandLine = new
			java.io.BufferedReader(new InputStreamReader(System.in));
			// Loop until the word "exit" is typed
			while(true) {
				System.out.print(chat.username + "*: ");
				String s = commandLine.readLine();
				if (s.equalsIgnoreCase("exit")) {
					chat.close();
					System.exit(0);
				} else
					chat.writeMessage(s);
			}			
		}
		catch (Exception e) { e.printStackTrace(); }
	}
}
