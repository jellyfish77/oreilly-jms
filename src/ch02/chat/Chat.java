package ch02.chat;

import java.io.*;
import javax.jms.*;
import javax.naming.*;
import java.util.Properties;

/* Connects to the topic and receives and delivers messages */
public class Chat implements javax.jms.MessageListener {
	private TopicSession pubSession;
	private TopicPublisher publisher;
	private TopicConnection connection;
	private String username;
	
	/* Constructor used to Initialize Chat */
	/* Connects to the topic and set up the TopicPublisher and TopicSubscribers for 
	 * delivering and receiving messages.
	 */
	public Chat(String topicFactory, String topicName, String username)	throws Exception {

		// Initialize environment of InitalContext from jndi.properties file
		// InitialContext will load (and merge) all jndi.properties files in root of classpath
		InitialContext ctx = new InitialContext();
		
		// Look up a JMS connection factory object   
		TopicConnectionFactory conFactory = (TopicConnectionFactory)ctx.lookup(topicFactory);
		
		// Create a JNDI connection to JMS Provider
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
	
	public Chat(InitialContext ctx, String topicFactory, String topicName, String username)	throws Exception {	
		
		// Look up the Topic ConnectionFactory object in the messaging server’s naming service.
		// This is an administered object configured by JMS messaging server administrator, used
		// to manufacture connections to a message server.
		TopicConnectionFactory conFactory = (TopicConnectionFactory)ctx.lookup(topicFactory);
		
		// Create a JNDI connection to JMS Provider
		// The TopicConnection represents a connection to the message server.
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
		
		// Start the JMS connection, turning the inbound flow of messages “on,” allowing messages to be
		// received by the client.
		// Messages start to flow in from the topic as soon as start() is invoked.
		connection.start();
		// The stop() method blocks the flow of inbound messages until the start() method is invoked again.
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
		// This should be done when a client is finished using the TopicConnection; closing
		// the connection conserves resources on the client and server.
		// Closing a TopicConnection closes all the objects associated with the connection,
		// including the TopicSession, TopicPublisher, and TopicSubscriber.
		connection.close();
	}
		/* Bootstrap the chat client and provide a command-line interface */
	public static void main(String [] args) {
		try {
			Chat chat;
						
			if (args.length!=3) {
				// use default arguments
				System.out.println("Factory, Topic, or username missing, using defaults...");
				Properties env = new Properties();			
				env.put(Context.INITIAL_CONTEXT_FACTORY,"com.sun.enterprise.naming.SerialInitContextFactory");
				env.put(Context.PROVIDER_URL, "tcp://localhost:4848");
				InitialContext ctx = new InitialContext(env);
				chat = new Chat(ctx, args[0],args[1],args[2]);
				
				//chat = new Chat("TopicConnectionFactory","testTopic",""); // Glassfish JMS objects
				//chat = new Chat("OttoActiveMQTopicConnectionFactory","testTopic","");
			} else {
				// use arguments specified							
				//args[0]=topicFactory; args[1]=topicName; args[2]=username
				//InitialContext ctx = new InitialContext();
				//chat = new Chat();
				chat = new Chat(args[0],args[1],args[2]);
			}
			
			// Read text typed at the command line and pass it to the Chat instance using
			// the instance’s writeMessage() method
			BufferedReader commandLine = new java.io.BufferedReader(new InputStreamReader(System.in));
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
