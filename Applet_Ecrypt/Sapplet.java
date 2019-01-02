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


public class Sapplet extends Applet implements uicc.toolkit.ToolkitConstants, uicc.usim.toolkit.ToolkitConstants, AppletEvent
{
	final static byte  DEBUG_INS = (byte)0x01;
	private final static short SIZE_BUFFER_LARGE = (short)256;
    static byte[] debug = new byte[SIZE_BUFFER_LARGE];
		
	public static void install(byte[] bArray, short bOffset, byte bLength) 
	{
		new Sapplet().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
		Util.arrayFillNonAtomic(debug, (short) 0, (short) debug.length, (byte)0xFF);
		Encryption.iniEncryption();
		JCSystem.requestObjectDeletion();
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
				//Util.setShort(buf, (short)0, (short)3);
				//apdu.setOutgoingAndSend((short)0, (short)8);
				return;
			case Encryption.INS_SET_DES_KEY:	
				//SET_DES_KEY:0x20
				Encryption.setDesKey(apdu);
				break;
			case Encryption.INS_SET_DES_ICV:	
				//SET_DES_ICV:0x21
				//ICV must have 8 bits.
				Encryption.setDesICV(apdu);
				break;
			case Encryption.INS_DO_DES_CIPHER:
				//DO_DES_CIPHER:0x22
				//P1(0:ENCRYPT, 1: DECRYPT)
				//P2(0:Ecb, 1: Cbc)		
				Util.arrayCopyNonAtomic(buf, ISO7816.OFFSET_CDATA, Encryption.EcryptData, (short)0, len);
				Encryption.doDesCipher(apdu, len);
				break;
			default:
				ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
		
		JCSystem.requestObjectDeletion ();
	}
		
	public void uninstall(){
	}
}
