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


public class Sapplet extends Applet implements ToolkitInterface, uicc.toolkit.ToolkitConstants, uicc.usim.toolkit.ToolkitConstants, AppletEvent
{
	final static byte  DEBUG_INS = (byte)0x01;
	
	private final static byte TAG_ALPHA_CR = (byte)(TAG_ALPHA_IDENTIFIER | TAG_SET_CR);
	private final static byte TAG_ITEM_CR = (byte)(TAG_ITEM | TAG_SET_CR);
	public final static byte QUALIFIER_DIGITS = (byte)0x00;
	public final static byte DCS_8_BIT_DATA = (byte)0x04;	
	
	private final static short SIZE_BUFFER_INPUT = (short)32;
	private final static short SIZE_BUFFER_LARGE = (short)256;
	
	private static ToolkitRegistry reg;
	private static byte idMenu1;
	private static byte[] Menu1, MenuTitle;
	private static byte[] GuessNumTitle = new byte[] {(byte)'G', (byte)'u', (byte)'e', (byte)'s', (byte)'s',(byte)' ', 
													  (byte)'n', (byte)'u', (byte)'m', (byte)'b', (byte)'e', (byte)'r'};
	private static byte[] InputNumTitle = new byte[] {(byte)'I', (byte)'n', (byte)'p', (byte)'u', (byte)'t', (byte)' ', 
													  (byte)'a', (byte)' ', (byte)'n', (byte)'u', (byte)'m', (byte)'(', 
													  (byte)'5', (byte)' ', (byte)'d', (byte)'i', (byte)'g', (byte)'i', 
													  (byte)'t', (byte)'s', (byte)')'};
    private static byte[] ErrorInputNumTitle = new byte[] {(byte)'E', (byte)'r', (byte)'r', (byte)'o', (byte)'r', (byte)'!', 
														   (byte)'P', (byte)'l', (byte)'e', (byte)'a', (byte)'s', (byte)'e', 
														   (byte)' ', (byte)'I', (byte)'n', (byte)'p', (byte)'u', (byte)'t', 
														   (byte)' ', (byte)'a', (byte)'g', (byte)'a', (byte)'i', (byte)'n', 
														   (byte)'.'};
	private static byte[] ResultTitle = new byte[] {(byte)'R', (byte)'e', (byte)'s', (byte)'u', (byte)'l', (byte)'t'};
	private static byte[] BingoTitle = new byte[] {(byte)'B', (byte)'i', (byte)'n', (byte)'g', (byte)'o', (byte)'!'};
	private static byte[] FailTitle = new byte[] {(byte)'F', (byte)'a', (byte)'i', (byte)'l', (byte)'!'};
	private static byte[] AnswerOfGuessNumTitle = new byte[] {(byte)'A', (byte)'n', (byte)'s', (byte)'w', (byte)'e', (byte)'r'};
	private static byte[] DebugTitle = new byte[] {(byte)'D', (byte)'e', (byte)'b', (byte)'u', (byte)'g'};
	
	//====Guess Number
	private byte[] GuessNum = new byte[] {(byte)'-', (byte)'1', (byte)'2', (byte)'3', (byte)'4'};
	private short GuessNumLen = (short)5;
	
	//Enter Call Control
	private static boolean displayStkByShortCode;	
	
	private static byte[] largeTmpBuf;
	private static byte[] inputBuf;
	private static byte[] debugBuf;
	
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
		
	public static void install(byte[] bArray, short bOffset, byte bLength) 
	{
		new Sapplet().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
		
		reg = ToolkitRegistrySystem.getEntry();
		
		Menu1 = new byte[] {(byte)'S', (byte)'a', (byte)'p', (byte)'p',	(byte)'l', (byte)'e', (byte)'t'};
		MenuTitle = new byte[] {(byte)'M', (byte)'e', (byte)'n', (byte)'u'};
		
		//Define the applet Menu Entry
		idMenu1 = reg.initMenuEntry(Menu1, (short) 0, (short) Menu1.length,
				PRO_CMD_SELECT_ITEM, false, (byte) 0, (short) 0);
		
		inputBuf = new byte[SIZE_BUFFER_INPUT];
		debugBuf = new byte[SIZE_BUFFER_LARGE];
		largeTmpBuf = new byte[SIZE_BUFFER_LARGE];
				
		Util.arrayFillNonAtomic(largeTmpBuf, (short) 0, (short) largeTmpBuf.length, (byte)0x00);
		Util.arrayFillNonAtomic(debugBuf, (short) 0, (short) debugBuf.length, (byte)0xFF);
		debugBuf[0] = (byte)'d';
		debugBuf[1] = (byte)'e';
		debugBuf[2] = (byte)'b';
		debugBuf[3] = (byte)'u';
		debugBuf[4] = (byte)'g';
		
		//====Register event
		// Register to get ATR
        reg.setEvent(EVENT_FIRST_COMMAND_AFTER_ATR);
		// Register to call control
		reg.setEvent(EVENT_CALL_CONTROL_BY_NAA);
		// Register Status event
		//reg.requestPollInterval((short)30); 
		
		JCSystem.requestObjectDeletion ();
	}
	
	public void process(APDU apdu)
	{
		// Pass selecting AID APDU
		if (selectingApplet())
			return;

		byte res;
		byte[] buf = apdu.getBuffer();
		
		apdu.setIncomingAndReceive();	
		//Command with data part
		switch (buf[ISO7816.OFFSET_INS])
		{
			//for debug
			case DEBUG_INS:
				short tmp1 = 119;
				Util.setShort(buf, (short)0, tmp1);
				apdu.setOutgoingAndSend((short)0, (short)8);
				return;
			default:
				ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
		
		JCSystem.requestObjectDeletion ();
	}
	
	public void processToolkit(short event) throws ToolkitException{
		// Only one applet can register the event at a time(Even the applet is not in selectable state).
		switch(event){
			case EVENT_CALL_CONTROL_BY_NAA:  
			
				EnvelopeHandler envHdlr = EnvelopeHandlerSystem.getTheHandler();
				short addrLen = (short)0;
								
				displayStkByShortCode = false;
				
				addrLen = envHdlr.findAndCopyValue(TAG_ADDRESS, largeTmpBuf, (short)0);
				addrLen -= (short)1;  // Subtract TON/NPI byte
									
				if(!(addrLen<=(short)5 && checkShortCode(addrLen)))
					break;
					
				if(displayStkByShortCode == false)
					break;
			case EVENT_MENU_SELECTION:
				do{
					sendMenu();
					if (processSelectItem()==false)
						currentMenuLevel = 0;
				}while(currentMenuLevel >= (byte)0);
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
				prohandler.appendTLV(TAG_ITEM_CR, ITEM1_ID, GuessNumTitle, (short)0, (short)GuessNumTitle.length);	//Guess Number
				prohandler.appendTLV(TAG_ITEM_CR, ITEM2_ID, AnswerOfGuessNumTitle, (short)0, (short)AnswerOfGuessNumTitle.length);	//Answer
				prohandler.appendTLV(TAG_ITEM_CR, ITEM8_ID, DebugTitle, (short)0, (short)DebugTitle.length);	//Display debug data
				break;
			case (byte)1:	//Guess Bingo
				prohandler.appendTLV(TAG_ALPHA_CR, GuessNumTitle ,(short)0, (short)GuessNumTitle.length);	
				prohandler.appendTLV(TAG_ITEM_CR, ITEM1_ID, BingoTitle, (short)0, (short)BingoTitle.length);
				currentMenuLevel = (byte)0;
				break;
			case (byte)2:	//Guess fail
				prohandler.appendTLV(TAG_ALPHA_CR, GuessNumTitle ,(short)0, (short)GuessNumTitle.length);	
				prohandler.appendTLV(TAG_ITEM_CR, ITEM1_ID, FailTitle, (short)0, (short)FailTitle.length);
				currentMenuLevel = (byte)0;
				break;
			case (byte)3:	//Answer Of Guess Num
				prohandler.appendTLV(TAG_ALPHA_CR, AnswerOfGuessNumTitle ,(short)0, (short)AnswerOfGuessNumTitle.length);	
				prohandler.appendTLV(TAG_ITEM_CR, ITEM10_ID, GuessNum, (short)0, (short)GuessNum.length);
				currentMenuLevel = (byte)0;
				break;
			case (byte)4:	//InputNum
				prohandler.appendTLV(TAG_ALPHA_CR, MenuTitle ,(short)0, (short)MenuTitle.length);	
				prohandler.appendTLV(TAG_ITEM_CR, ITEM1_ID, GuessNumTitle, (short)0, (short)GuessNumTitle.length);	//Guess Number
				currentMenuLevel = (byte)0;
				break;
			case (byte)8:	//Debug		
				prohandler.appendTLV(TAG_ITEM_CR, ITEM10_ID, debugBuf, (short)0, (short)10);
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
					case ITEM1_ID:	//Input Number						
						InputGuessNumber();
						break;
					case ITEM2_ID:	//Answer
						currentMenuLevel = (byte)3;
						break;
					case ITEM8_ID:	//Debug
						currentMenuLevel = (byte)8;
						break;
				}
				break;
			case (byte)1:
				currentMenuLevel = (byte)1;
				break;
			case (byte)2:
				currentMenuLevel = (byte)2;
				break;
			default:
				result = false;
				break;
		}
		return result;
	}
	
	private void InputGuessNumber()
	{		
		short cnt = (short)0;
		short itemLen = (short)5;
		
		getInput(InputNumTitle, (short)0, itemLen, inputBuf, (short)0);
		
		// (GuessNum==-1234)
		if(itemLen==GuessNumLen && (Util.arrayCompare(inputBuf, (short)0, GuessNum, (short)0, GuessNumLen)==0) )	
			currentMenuLevel = 1;						
		else 		
			currentMenuLevel = 2;
	}
	
	public static short getInput(byte[] displayMsg, short inputMinLen, short inputMaxLen, byte[] textBuf, short textBufOfs)
	{
		ProactiveHandler prohandler = ProactiveHandlerSystem.getTheHandler();
		ProactiveResponseHandler proRspHandler = ProactiveResponseHandlerSystem.getTheHandler();
		
		prohandler.initGetInput(QUALIFIER_DIGITS, DCS_8_BIT_DATA, displayMsg, (short)0, (short)displayMsg.length, inputMinLen, inputMaxLen);
		prohandler.send();		
		inputMinLen = proRspHandler.copyTextString(textBuf, textBufOfs);
		return inputMinLen;
	}
	
	private boolean checkShortCode(short addrLen){
		EnvelopeResponseHandler envResHdlr = EnvelopeResponseHandlerSystem.getTheHandler();
		
		// To stop mobile calling
		//We need to call postAsBERTLV before send. 
		//As we call send, then we can't call postAsBERTLV.

		envResHdlr.postAsBERTLV(true, (byte)1);
		
		if(largeTmpBuf[1]==(byte)0x22 && largeTmpBuf[2]==(byte)0x22)
		{	
			//Guess Number
			currentMenuLevel = (byte)4;
		}
		else if(largeTmpBuf[1]==(byte)0x33 && largeTmpBuf[2]==(byte)0x33)
		{	
			//ShowGuessNum	
			currentMenuLevel = (byte)3;
		}
		else{
			// Release unused previous objects
			JCSystem.requestObjectDeletion();			
			return false;
		}
		
		// Release unused previous objects
		displayStkByShortCode = true;
		JCSystem.requestObjectDeletion();		
		return true;
	}
		
	public void uninstall(){
	}
}
