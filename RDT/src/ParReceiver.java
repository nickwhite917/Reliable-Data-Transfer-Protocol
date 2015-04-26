import java.io.*;
import java.util.*;


/*
 * Author: Wenbing Zhao
 * Last Modified: 10/4/2009
 * For EEC484 Project
 */

public class ParReceiver extends TransportLayer{
    public static final int RECEIVER_PORT = 9888;
    public static final int SENDER_PORT = 9887;
    public static Hashtable<Byte,Packet> packetBuf = new Hashtable<Byte,Packet>(){/**
		 * 
		 */
		private static final long serialVersionUID = 4446462988040792573L;

	{
				
	}};
    public ParReceiver(LossyChannel lc) {
	super(lc);
    }


    public void waitFor(int seqWaitingFor){
    	System.out.printf("Entered States %s.\n",seqWaitingFor);
	    int event = waitForEvent();
	    if(EVENT_PACKET_ARRIVAL == event) {
	    	Packet packetReceived = new Packet();
			packetReceived = receiveFromLossyChannel();
			if(packetReceived.seq != (byte)seqWaitingFor){
				Packet packetToSend = new Packet();
				packetToSend.ack = packetReceived.seq;
				sendToLossyChannel(packetToSend);
			}
			else if(packetReceived.seq == (byte)seqWaitingFor){
				deliverMessage(packetReceived);
				deliverMessageToFile(packetReceived);
				Packet packetToSend = new Packet();
				packetToSend.ack = (byte)seqWaitingFor;
				sendToLossyChannel(packetToSend);
				waitFor((int)increment((byte)seqWaitingFor));
			}
	    }	 
    }
    public void run(){
		waitFor(0);
	}
	    
    // To be modified for task#5
    //
    // We simply extract the payload and display it as a string in stdout
    void deliverMessage(Packet packet) {
	byte[] payload = new byte[packet.length];
	for(int i=0; i<payload.length; i++)
	    payload[i] = packet.payload[i];
	String recvd = new String(payload);
	System.out.println("Received "+packet.length+" bytes: "
			   +recvd);
    }
    
    void deliverMessageToFile(Packet packet) {
    	byte[] payload = new byte[packet.length];
    	for(int i=0; i<payload.length; i++)
    	    payload[i] = packet.payload[i];
    	String recvd = new String(payload);
    	
    	File outputFileName =  new File("OUTPUT.txt");
		BufferedWriter bw;
		try{
			bw = new BufferedWriter(new FileWriter(outputFileName, true));
		//PrintWriter writer = new PrintWriter(outputFileName+".txt", "UTF-8");
		//writer.println("Received "+packet.length+" bytes: "+recvd);
		//writer.println("received payload len: "+recvd.length());
		//writer.close();
			bw.write("Received "+packet.length+" bytes: "+recvd+"\n");
			bw.newLine();
			bw.close();
		
		}catch (Exception e) {}
    }

    public static void main(String args[]) throws Exception { 
	LossyChannel lc = new LossyChannel(RECEIVER_PORT, SENDER_PORT);
	//lc.userDefinedLossRate = Integer.parseInt(args[0]);
	ParReceiver receiver = new ParReceiver(lc);
	lc.setTransportLayer(receiver);
	receiver.run();
	//receiver.run();
    } 
}  
