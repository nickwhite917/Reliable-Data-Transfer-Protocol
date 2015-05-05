/* Author: Wenbing Zhao
 * Last Modified: 10/4/2009
 * For EEC484 Project
 */

import java.io.*; 
import java.util.*;

public class ParSender extends TransportLayer{ 
	
	//Define ports for sender and receiver communication
    public static final int RECEIVER_PORT = 9888;
    public static final int SENDER_PORT = 9887;
    
    public String filePath;	//String which holds file path for BufferedReader to use
    
    //List which holds the lines of the file to be sent
    public List<String> inputLines = null;
    //Index of the current line being prepared to be sent
    public int inputLinesIndex = 0;
    
    //Initializer, fed a LossyChannel object
    public ParSender(LossyChannel lc) 
    {
    	super(lc);
    }

    /*
	*	Function	:	waitAck 
	*	Purpose		:	- Waits for activation from 'waitForCallFromAbove' state. 
	*					- Is given an 'ack' value and a reference to a packet object.
	*					- Once activated, if timeout occurs, will re-send the packet it was sent.
	*					- If a packet with 'ack' value equal to own 'ack' value, stops timer and sends to 'waitForCallFromAbove' state.
	*	Arguments	:	- byte ack		: ack of the packet that is expected
	*					- Packet sndpkt	: packet that is to be send if timeout occurs
	*	Returns		:	void
    */
   public void waitAck(byte ack,Packet sndpkt){
   	try{
	   //As soon as state is activated, wait for a packet to arrive, or timeout event
	   int event = waitForEvent();
	    if(EVENT_PACKET_ARRIVAL == event) {	//If the event was a packet arrival
	    	Packet packet = new Packet();
	    	packet = receiveFromLossyChannel();//Receive the packet from lossy channel
	    	
	    	if(packet.ack != ack){	//If the ack of the packet is NOT the same as the state's ack
	    		waitAck(ack,sndpkt);//launch the current state again from the beginning
	    	}
	    	if(packet.ack == ack){	//If the ack of the packet is NOT the same as the state's ack 
	    		stopTimer();	//Stop the timer
	    		//System.out.printf(" %d percent complete...",(inputLines.size()/inputLinesIndex));
	    		waitForCallFromAbove(increment(ack));	//Go to next state and increment the expected ack
	    	}
	    }
	    else if(EVENT_TIMEOUT == event){	//If a timeout event occurs
	    	System.out.println("Timeout, resending..."); //Inform the user of a timeout
	    	sendToLossyChannel(sndpkt);	//Resend the packet
	    	startTimer();	//Restart the timer
	    	waitAck(ack,sndpkt); //Enter the same state again with the same packet

	    }
	}
	catch(Exception ex){return;}
   }
   
   /*
	*	Function	:	waitForCallFromAbove 
	*	Purpose		:	- If a packet is received, continue at current state
	*					- Once data is processed, make an outgoing packet and send it
	*					- Then transfer to state waitAck
	*	Arguments	:	- byte sndpktSeq: seq of the packet to be sent
	*	Returns		:	void
   */
   public void waitForCallFromAbove(byte sndpktSeq){
   	try{
		Packet sndpkt = new Packet();	//Initialize a new packet
		sndpkt.seq = sndpktSeq;	//Set the seq to the current state's seq
		try{ //Try to load the packet with the next line of data from the file/user input
			sndpkt.payload = inputLines.get(inputLinesIndex).toString().getBytes();
			sndpkt.length = inputLines.get(inputLinesIndex).toString().length();
		}
		catch(Exception e){
			System.out.printf("Done sending all '%i' message/s! \n",inputLinesIndex);
			return;
		}
		inputLinesIndex++; //Point to next line of data
		
		sendToLossyChannel(sndpkt);
    	startTimer();
    	waitAck(sndpktSeq,sndpkt);//Transfer to the waitAck state
    }
    catch(Exception ex){return;}
   }
   
   /*
  	*	Function	:	run 
  	*	Purpose		:	Gets user input regarding what to send, starts the timer, begins the FSM
  	*	Arguments	:	none
  	*	Returns		:	void
     */
    public void run(){
    	getMessageToSend(); //Get user input, either a file or message.
    	long startTime =0;
    	try{
    	startTime = System.nanoTime();	//Start the timer
    	waitForCallFromAbove((byte)0);	//Begin the finite state machine
    	}
    	catch(Exception e){
    		long stopTime = System.nanoTime(); //Stop the timer
    		System.out.println((double)(stopTime - startTime)/1000000000);
    		return;
    	}
    	long stopTime = System.nanoTime();
    	System.out.println((double)(stopTime - startTime)/1000000000);

    }
    
    /*
	*	Function	:	getMessageToSendGet 
	*	Purpose		:	Gets the message or file-path from user
	*	Arguments	:	none
	*	Returns		:	byte array
    */
    void getMessageToSend() {
    	
	    System.out.println("Enter a message to send (To send a file, enter the complete path) :> ");
	    
		//Assuming the user's input is a file, try to open it...
		String userInputFromConsole = null;
		
		try {
			BufferedReader inFromUser = 
			new BufferedReader(new InputStreamReader(System.in)); 

			//Get user's input
			userInputFromConsole = inFromUser.readLine(); 
			if(null == userInputFromConsole){
				System.exit(1);
			}
			
			BufferedReader br = new BufferedReader(new FileReader(userInputFromConsole));
			filePath = userInputFromConsole;
		    try 
		    {	    	
		        StringBuilder sb = new StringBuilder();
		        String line = br.readLine();
		        inputLines = new ArrayList<String>();
		        inputLines.add(line);
		        while (line != null) {
		            sb.append(line);
		            sb.append(System.lineSeparator());
		            line = br.readLine();
		            inputLines.add(line);
	        }
		        userInputFromConsole = sb.toString();
		    } finally {
		        br.close();
		    }

		    //Tell the user we are sending the file at path 'userInputFromConsole'
		    //System.out.println("Sending: " + userInputFromConsole);
		 	//Return the contents of the file in byte form
		    
		} catch(Exception e) { //Executed when the user enters a message, not a file path
		    inputLines = new ArrayList<String>();
		    inputLines.add(userInputFromConsole);
		}
    }

    /*
	*	Function	:	getLossRate //Get loss rate from user using Scanner
	*	Returns		:	integer //User defined loss rate
	*	Arguments	:	none
    */
    public static int getLossRate(){
    	//Prompt
    	System.out.println("Would you like to change the loss rate? (Enter 0 for No and 1 for Yes) :> ");
    	//Create scanner and take input
	    Scanner in = new Scanner(System.in);
	    String answer = in.nextLine();
	    //If user wants to change rate...
		if(answer.equals("1"))
		{
			System.out.println("Please enter your desired loss rate as a percentage (E.g. 20 for 20%):> ");
		    answer = in.nextLine();	  
		    return Integer.valueOf(answer);
		}
		//Return 0% loss rate if user does not want to change rate
		else{
			return 0;
		}
    }

    /*
	*	Function	:	Main
	*	Returns		:	Void
	*	Arguments	:	String args[] //Command line arguments
    */
    public static void main(String args[]) throws Exception { 
    	//Instantiate lossy channel
		LossyChannel lc = new LossyChannel(SENDER_PORT, RECEIVER_PORT); //Instantiate lossy channel
		//Get packet loss rate from user
		lc.setUserDefinedLossRate(getLossRate());
		//Instantiate and run sender ParSender
		ParSender sender = new ParSender(lc);
		lc.setTransportLayer(sender);
		sender.run();
		System.out.printf("Done sending all messages! \n");
	} 
} 
