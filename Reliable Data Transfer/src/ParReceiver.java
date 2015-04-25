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

    public void run() {
	byte nextPacketExpected = 0;
	Packet packetReceived = new Packet();
	Packet packetToSend = new Packet();
	packetToSend.ack = 1;
	packetToSend.seq = 0;

	System.out.println("Ready to receive: ");
	
	while(true) {
		System.out.println("Waiting for: Seq:"+packetToSend.seq+" and ACK:"+packetToSend.ack);
	    int event = waitForEvent();
	    if(EVENT_PACKET_ARRIVAL == event) {
			packetReceived = receiveFromLossyChannel();
			System.out.println("Received: Seq:" + packetReceived.seq +" Ack:" + packetReceived.ack);
			deliverMessage(packetReceived);
			
			boolean checkedBuffer = false;
			while(!checkedBuffer){
				if(packetReceived.seq == nextPacketExpected){
					deliverMessageToFile(packetReceived);
					checkedBuffer = true;
				}
				else{
					System.out.println("Packet received but was out of order...");
					System.out.println("Expecting: "+nextPacketExpected);
					System.out.println("Got: "+packetReceived.seq);
					if(!(packetBuf.containsKey(nextPacketExpected))){
						packetBuf.put(packetReceived.seq, packetReceived);
						checkedBuffer = true;
					}
					else{
						deliverMessageToFile(packetBuf.get(nextPacketExpected));
						packetBuf.remove(nextPacketExpected);
					}
					
				}
			}
			
			
			
			packetToSend.seq = packetReceived.seq;
			packetToSend.ack = increment(packetReceived.ack);
			nextPacketExpected = increment(nextPacketExpected);
			String msg = "Seq:" + packetToSend.seq + "Ack:"+packetToSend.ack;
			byte[] msgToSend = msg.getBytes();
			int payloadLength = 0;
			for(int i = 0; i < msgToSend.length; i++){
				packetToSend.payload[i] = msgToSend[i];
				payloadLength++;
			}
			packetToSend.length = payloadLength;
			
		    sendToLossyChannel(packetToSend);
		    m_wakeup = false;
	    }
	    
	}
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
			bw.close();
		
		}catch (Exception e) {}
    }

    public static void main(String args[]) throws Exception { 
	LossyChannel lc = new LossyChannel(RECEIVER_PORT, SENDER_PORT);
	//lc.userDefinedLossRate = Integer.parseInt(args[0]);
	ParReceiver receiver = new ParReceiver(lc);
	lc.setTransportLayer(receiver);
	receiver.run();
    } 
}  
