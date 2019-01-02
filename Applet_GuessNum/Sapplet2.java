package com.taisys.Slimduet.Applet;

import javacard.framework.*;
import uicc.access.FileView;
import uicc.access.UICCSystem;
import uicc.access.UICCException;
//import sim.access.*;
import uicc.system.UICCPlatform;
import uicc.toolkit.*;
//import sim.toolkit.*;
import uicc.usim.toolkit.USATEnvelopeHandler;
import uicc.usim.toolkit.USATEnvelopeHandlerSystem;
import com.taisys.apis.sim.simome.*;
import com.taisys.internal.sim.simome.SIMoMECore;
import com.taisys.internal.sim.cardprofile.UiccProfileConfig;
import com.taisys.seac.IShareableInterface;
import com.taisys.apis.sim.oti.*;


public class Sapplet2 extends Applet implements ToolkitInterface, uicc.toolkit.ToolkitConstants, uicc.usim.toolkit.ToolkitConstants, AppletEvent
{
	final static byte  DEBUG_INS = (byte)0x01;
	
	private final static byte TAG_ALPHA_CR = (byte)(TAG_ALPHA_IDENTIFIER | TAG_SET_CR);
	private final static byte TAG_ITEM_CR = (byte)(TAG_ITEM | TAG_SET_CR);
	public final static byte QUALIFIER_DIGITS = (byte)0x00;
	public final static byte DCS_8_BIT_DATA = (byte)0x04;	
    
	private static ToolkitRegistry reg;
	private static byte idMenu1;
	private static byte[] Menu1, MenuTitle;
	private static byte[] OpenChannelTitle = new byte[] {(byte)'O', (byte)'p', (byte)'e', (byte)'n', (byte)' ', (byte)'C', (byte)'h', (byte)'a', (byte)'n', (byte)'n', (byte)'e', (byte)'l'};
	private static byte[] SendDataTitle = new byte[] {(byte)'S', (byte)'e', (byte)'n', (byte)'d', (byte)' ', (byte)'D', (byte)'a', (byte)'t', (byte)'a'};
	private static byte[] ReceiveDataTitle = new byte[] {(byte)'R', (byte)'e', (byte)'c', (byte)'e', (byte)'i', (byte)'v', (byte)'e', (byte)' ', (byte)'D', (byte)'a', (byte)'t', (byte)'a'};
	private static byte[] CloseChannelTitle = new byte[] {(byte)'C', (byte)'l', (byte)'o', (byte)'s', (byte)'e', (byte)' ', (byte)'C', (byte)'h', (byte)'a', (byte)'n', (byte)'n', (byte)'e', (byte)'l'};
	private static byte[] DataTitle = new byte[] {(byte)'D', (byte)'a', (byte)'t', (byte)'a'};
	private static byte[] DebugTitle = new byte[] {(byte)'D', (byte)'e', (byte)'b', (byte)'u', (byte)'g'};
	private static byte[] largeTmpBuf;  // Temporary buffer (Do not use it while processing SSD and call control)
		
	private static byte currentMenuLevel = (byte)0x00;
	private final static byte ITEM1_ID = (byte)1;
	private final static byte ITEM2_ID = (byte)2;
	private final static byte ITEM3_ID = (byte)3;
	private final static byte ITEM4_ID = (byte)4;
	private final static byte ITEM5_ID = (byte)5;
	private final static byte ITEM6_ID = (byte)6;
	private final static byte ITEM7_ID = (byte)7;
	private final static byte ITEM8_ID = (byte)8;
	private final static byte ITEM9_ID = (byte)9;
	private final static byte ITEM10_ID = (byte)10;
	
	private final static short SIZE_BUFFER_LARGE = (short)256;
	
	static short rcvLen = (short)0;	
	private static byte[] debug, data, showDebug;
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // BIP DATA CONSTANTS
    private static final byte TAG_DATA_DESTINATION_ADDRESS           = (byte) 0x3E;
	public static final byte DEV_ID_ME                               = (byte)0x82;	
    public static final byte TAG_SIM_ME_INTERFACE_TRANSPORT_LEVEL    = (byte)0x3C;
 
    //Default transport layer = 0x03
    private static final byte BEARER_TYPE                       = (byte) 0x03;
    private static final byte BEARER_PARAMETER_PRECEDENCE_CLASS1     = (byte) 0x01;
    private static final byte BEARER_PARAMETER_DELAY_CLASS1          = (byte) 0x01;
    private static final byte BEARER_PARAMETER_RELIABILITY_CLASS1    = (byte) 0x01;
    private static final byte BEARER_PARAMETER_PEAK_THROUGHPUT_CLASS1= (byte) 0x01;
    private static final byte BEARER_PARAMETER_MEAN_THROUGHPUT_CLASS1    = (byte) 0x01;
    // PDP Type = IP
    private static final byte BEARER_PARAMETER_PDP_IP                    = (byte) 0x02;
    // Type of Address IPV4=21,
    private static final byte TYPE_OF_ADDRESS_IPV4      = (byte) 0x21;
    // SIM/ME interface transport level TCP=02
    private static final byte TRANSPORT_PROTOCOL_TYPE   = (byte) 0x02;
    
	private static byte[] portNumber = {(byte)0x00,(byte)0x50};                 // Port 0050
    private static byte[] IPAddress = {(byte)0x24,(byte)0xE3,(byte)0x85,(byte)0x26};
    private static final short BUFFER_SIZE = (short)0x00FF;              // Port 13020
    private static byte channelID = 0x00;
	private static byte[] send_buffer = {(byte)0x47, (byte)0x45,(byte)0x54,(byte)0x20 ,(byte)0x2F ,(byte)0x42 ,(byte)0x49 ,(byte)0x50 ,(byte)0x2F ,(byte)0x20
										,(byte)0x48 ,(byte)0x54 ,(byte)0x54 ,(byte)0x50 ,(byte)0x2F ,(byte)0x31 ,(byte)0x2E ,(byte)0x30 ,(byte)0x0D ,(byte)0x0A 
										,(byte)0x0D ,(byte)0x0A};
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////  
		
	public static void install(byte[] bArray, short bOffset, byte bLength) 
	{
		new Sapplet2().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
		
		reg = ToolkitRegistrySystem.getEntry();
		
		Menu1 = new byte[] {(byte)'S', (byte)'a', (byte)'p', (byte)'p',	(byte)'l', (byte)'e', (byte)'t', (byte)'2'};
		MenuTitle = new byte[] {(byte)'M', (byte)'e', (byte)'n', (byte)'u'};
		//Define the applet Menu Entry
		idMenu1 = reg.initMenuEntry(Menu1, (short) 0, (short) Menu1.length,
				PRO_CMD_SELECT_ITEM, false, (byte) 0, (short) 0);
	
		showDebug = new byte[SIZE_BUFFER_LARGE];
		debug = new byte[SIZE_BUFFER_LARGE];
		data = new byte[SIZE_BUFFER_LARGE];
				
		Util.arrayFillNonAtomic(showDebug, (short) 0, (short) showDebug.length, (byte)0xFF);
		Util.arrayFillNonAtomic(debug, (short) 0, (short) debug.length, (byte)0xFF);
		debug[0] = (byte)'d';
		debug[1] = (byte)'e';
		debug[2] = (byte)'b';
		debug[3] = (byte)'u';
		debug[4] = (byte)'g';
		
		//====Register event
		// Register to get ATR
        reg.setEvent(EVENT_FIRST_COMMAND_AFTER_ATR);
		// Register Status event
		//reg.requestPollInterval((short)30); 
		reg.setEvent(EVENT_EVENT_DOWNLOAD_DATA_AVAILABLE);
		JCSystem.requestObjectDeletion ();
	}
	
	public void process(APDU apdu)
	{
		// Pass selecting AID APDU
		if (selectingApplet())
			return;

		byte res;
		byte[] buf = apdu.getBuffer();
		short len = buf[ISO7816.OFFSET_LC];
		byte INS = buf[ISO7816.OFFSET_INS];
		
		apdu.setIncomingAndReceive();	
		//Command with data part
		switch (buf[ISO7816.OFFSET_INS])
		{
			case DEBUG_INS:
				// short tmp1 = 119;
				Util.setShort(buf, (short)0, channelID);
				apdu.setOutgoingAndSend((short)0, (short)8);
				return;
			default:
				ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
		
		JCSystem.requestObjectDeletion ();
	}
	
	public void processToolkit(short event) throws ToolkitException{
		switch(event){
			case EVENT_MENU_SELECTION:
				do{
					sendMenu();
					if (processSelectItem()==false)
						currentMenuLevel = 0;
				}while(currentMenuLevel >= (byte)0);
				break;
			
			case EVENT_EVENT_DOWNLOAD_DATA_AVAILABLE:
				ProactiveHandler prohandler = ProactiveHandlerSystem.getTheHandler();	
				ProactiveResponseHandler proRspHandler = ProactiveResponseHandlerSystem.getTheHandler();
				receiveData(prohandler, proRspHandler);
				break;
			default:
				break;
		}
	}
	
	private void sendMenu(){
		ProactiveHandler prohandler = ProactiveHandlerSystem.getTheHandler();	
		prohandler.init(PRO_CMD_SELECT_ITEM, (byte) 0, DEV_ID_TERMINAL);
	
		switch(currentMenuLevel){
			case (byte)0:	//Menu
				prohandler.appendTLV(TAG_ALPHA_CR, MenuTitle ,(short)0, (short)MenuTitle.length);	
				prohandler.appendTLV(TAG_ITEM_CR, ITEM3_ID, OpenChannelTitle, (short)0, (short)OpenChannelTitle.length);	//Open Channel
				prohandler.appendTLV(TAG_ITEM_CR, ITEM4_ID, SendDataTitle, (short)0, (short)SendDataTitle.length);	//Send Data
				prohandler.appendTLV(TAG_ITEM_CR, ITEM5_ID, ReceiveDataTitle, (short)0, (short)ReceiveDataTitle.length);	//Receive Data
				prohandler.appendTLV(TAG_ITEM_CR, ITEM6_ID, CloseChannelTitle, (short)0, (short)CloseChannelTitle.length);	//Close Channel
				prohandler.appendTLV(TAG_ITEM_CR, ITEM7_ID, DataTitle, (short)0, (short)DataTitle.length);	//Display Data
				prohandler.appendTLV(TAG_ITEM_CR, ITEM8_ID, DebugTitle, (short)0, (short)DebugTitle.length);	//Debug Data
				break;
			case (byte)8:	//Debug		
				prohandler.appendTLV(TAG_ITEM_CR, ITEM10_ID, showDebug, (short)0, (short)10);
				currentMenuLevel = (byte)0;
				break;
			default:
				break;
		}
		prohandler.send();
		return;
	}
	
	private boolean processSelectItem(){
		ProactiveHandler prohandler = ProactiveHandlerSystem.getTheHandler();	
		
		ProactiveResponseHandler proRspHandler = ProactiveResponseHandlerSystem.getTheHandler();
		byte item = proRspHandler.getItemIdentifier();		
		boolean result = true;
		
		switch(currentMenuLevel){
			case (byte)0:
				switch(item){
					case ITEM3_ID:	//Open Channel
						openChannel(prohandler, proRspHandler);
						break;
					case ITEM4_ID:	//Send Data
						sendData(prohandler);
						break;
					case ITEM5_ID:	//Receive Data
						receiveData(prohandler, proRspHandler);
						break;
					case ITEM6_ID:	//Close Data
						closeChannel(prohandler);
						break;
					case ITEM7_ID:	//Display Data
						displayData(prohandler);
						// currentMenuLevel = (byte)7;
						break;
					case ITEM8_ID:	//Debug
						currentMenuLevel = (byte)8;
						break;
				}
				break;
			default:
				result = false;
				break;
		}
		return result;
	}
	
	public static void openChannel(ProactiveHandler phdr, ProactiveResponseHandler rhdr){
		phdr.init(PRO_CMD_OPEN_CHANNEL, (byte)0x03, DEV_ID_ME);
		phdr.appendTLV((byte)TAG_BEARER_DESCRIPTION, (byte)BEARER_TYPE);
		phdr.appendTLV((byte)TAG_BUFFER_SIZE, (byte)(BUFFER_SIZE>>(byte)8), (byte)BUFFER_SIZE);
		phdr.appendTLV((byte)TAG_SIM_ME_INTERFACE_TRANSPORT_LEVEL, TRANSPORT_PROTOCOL_TYPE, portNumber, (short)0, (short)portNumber.length);
		phdr.appendTLV((byte)TAG_DATA_DESTINATION_ADDRESS, TYPE_OF_ADDRESS_IPV4, IPAddress, (short)0, (short)IPAddress.length);
		phdr.send();
		
		channelID = rhdr.getChannelIdentifier();
		channelID += (byte)0x20;
		rcvLen = (short)0;
	}
	
	public static void sendData(ProactiveHandler phdr){		
		phdr.init(PRO_CMD_SEND_DATA, (byte)0x01, channelID);
		phdr.appendTLV((byte)TAG_CHANNEL_DATA, send_buffer, (short)0, (short)send_buffer.length);
		phdr.send();
	}
	
	public static void receiveData(ProactiveHandler phdr, ProactiveResponseHandler rhdr){
		// showDebug[1] = (byte)0x01
		phdr.init(PRO_CMD_RECEIVE_DATA, (byte)0, channelID);
		phdr.appendTLV((byte)TAG_CHANNEL_DATA_LENGTH, (byte)0xFF);
		phdr.send();
		short dataLen = rhdr.findAndCopyValue((byte)TAG_CHANNEL_DATA, data, (short)0);
					
		if (rcvLen==(short)0 && dataLen>0)
		{
			short i = (short)0;
			for (i=(short)0; i<dataLen; i++)
			{
				if (data[i]==(byte)0xAA && data[(short)(i+1)]==(byte)0xBB && data[(short)(i+2)]==(byte)0xDD) 
					break;
			}
			Util.arrayCopyNonAtomic(data, (short)i, debug, (short)0, (short)(dataLen-i));
			rcvLen = (short)(dataLen-i);
		}
		else if ( dataLen>(short)0)
		{
			Util.arrayCopyNonAtomic(data, (short)0, debug, (short)rcvLen, (short)dataLen);
			rcvLen += dataLen;
		}
	}
		
	public static void closeChannel(ProactiveHandler phdr){
		
		phdr.init(PRO_CMD_CLOSE_CHANNEL, (byte)0, channelID);
		phdr.send();
	}	
	
	public static void displayData(ProactiveHandler phdr){
		phdr.initDisplayText((byte)0x01, (byte)0x04, debug, (short)0, (short)rcvLen);
		phdr.send(); 
	}
			
	public void uninstall(){
	}
}
