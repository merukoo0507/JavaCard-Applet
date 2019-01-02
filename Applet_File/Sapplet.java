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
	final static byte  debugIns = (byte)0x01;
	final static byte  CREATE_FILE = (byte)0xE0;
	final static byte  SELECT_FILE = (byte)0xA4;
	final static byte  READ_BINARY = (byte)0xB0;
	final static byte  READ_RECORD = (byte)0xB2;
	final static byte  UPDATE_BINARY = (byte)0xD6;
	final static byte  UPDATE_RECORD = (byte)0xDC;
	    		
	private final static short SSD_TOTAL_FILE_NUM = (short)256;
	
	private static short[] ssdFidList;
	private static SSDfileSystem[] ssdFS;
	private static short ssdFilenum;
	private short fileIndex;
	private static byte[] debug;
	
	public static void install(byte[] bArray, short bOffset, byte bLength) 
	{
		new Sapplet().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
		
		ssdFilenum = 0;
		ssdFidList = new short[SSD_TOTAL_FILE_NUM];
		ssdFS = new SSDfileSystem[SSD_TOTAL_FILE_NUM];
		debug = new byte[256];
		
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
		
		switch (INS)
		{
			case debugIns:
				short tmp1 = 119;
				Util.setShort(buf, (short)0, tmp1);
				apdu.setOutgoingAndSend((short)0, (short)8);
				return;
			case READ_BINARY:
				ssdFS[fileIndex].readBinary(buf[ISO7816.OFFSET_P1], buf, ISO7816.OFFSET_CDATA, buf[ISO7816.OFFSET_LC]);
				apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, buf[ISO7816.OFFSET_LC]);
				return;
			case READ_RECORD:
				ssdFS[fileIndex].readRecord(buf[ISO7816.OFFSET_P1], buf[ISO7816.OFFSET_P2], (short)0, buf, ISO7816.OFFSET_CDATA, buf[ISO7816.OFFSET_LC]);
				apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, buf[ISO7816.OFFSET_LC]);
				return;
		}			
		apdu.setIncomingAndReceive();	
		//Command with data part
		switch (buf[ISO7816.OFFSET_INS])
		{
			case CREATE_FILE:
				createSSDfile(buf);
				break;
			case SELECT_FILE:
				short fileID = (short)0;
				fileID = Util.getShort(buf, (short)ISO7816.OFFSET_CDATA);
				fileIndex = getSSDfidIndex(fileID, (short)0x00);
				if (fileIndex==(short)0xFFFF)
					ISOException.throwIt(ISO7816.SW_DATA_INVALID);
				break;
			case UPDATE_BINARY:
				ssdFS[fileIndex].updateBinary(buf[ISO7816.OFFSET_P1], buf, ISO7816.OFFSET_CDATA, buf[ISO7816.OFFSET_LC]);
				break;
			case UPDATE_RECORD:
				ssdFS[fileIndex].updateRecord(buf[ISO7816.OFFSET_P1], buf[ISO7816.OFFSET_P2], (short)0, buf, ISO7816.OFFSET_CDATA, buf[ISO7816.OFFSET_LC]);
				break;
			default:
				ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
		
		JCSystem.requestObjectDeletion ();
	}
	
	//====Create SSD Files
	private void createSSDfile(byte[] apduBuf){
		byte apduLen = (byte)(apduBuf[ISO7816.OFFSET_LC] + ISO7816.OFFSET_LC);
		byte parseOfs = (byte)(ISO7816.OFFSET_CDATA + (byte)2);	// Skip the FCP tag
		byte tagLen = (byte)0;
		byte fileAttribute = (byte)0;
		byte requiredDataNum = (byte)0;
		short fileID = (short)0;
		short fileTotalSize = (short)0;
		short fileRecordLen = (short)0;
		byte failCreateFile = (byte)0x00;
		
		fileIndex = (short)0xFFFF;
		//====Parse tags (Skip the FCP tag)
		do{
			switch(apduBuf[parseOfs++]){  // Point to length byte
				case (byte)0x80:  // Total file size
					tagLen = apduBuf[parseOfs++];  // Point to value byte
					
					fileTotalSize = Util.getShort(apduBuf, parseOfs);
					
					requiredDataNum++;
					break;
				case (byte)0x82:  // File descriptor
					tagLen = apduBuf[parseOfs++];  // Point to value byte
					
					if(tagLen == (byte)0x02)  // Transparent
						fileAttribute = (byte)0;
					else{                     // Linear fixed
						fileAttribute = (byte)1;
						
						fileRecordLen = Util.getShort(apduBuf, (short)(parseOfs+2));
					}
					
					requiredDataNum++;
					break;
				case (byte)0x83:  // File ID
					tagLen = apduBuf[parseOfs++];  // Point to value byte
					
					fileID = Util.getShort(apduBuf, parseOfs);
					fileIndex = getSSDfidIndex(fileID, (short)0x01);
					if (fileIndex==(short)0xFFFF)
					{
						failCreateFile = (byte)0x01;
						break;
					}
					requiredDataNum++;
					break;
				default:
					tagLen = apduBuf[parseOfs++];  // Point to value byte
					break;
			}
			// If get enough information, then break
			if(requiredDataNum == (byte)3)	
				break;
			// Point to the next tag
			parseOfs += tagLen;
		}while(parseOfs<apduLen);
		
		//====Create file
		if(ssdFS[fileIndex] == null && failCreateFile!=0x01){
			if(fileAttribute == (byte)0)  // Transparent
				ssdFS[fileIndex] = new SSDfileSystem(fileID, fileAttribute, fileTotalSize);
			else						  // Linear fixed
				ssdFS[fileIndex] = new SSDfileSystem(fileID, fileAttribute, fileTotalSize, fileRecordLen);
			ssdFilenum++;
			//Util.setShort(debug, (short)0, fileIndex);
		}
		
		// Release unused previous objects
		JCSystem.requestObjectDeletion();
	}
	
	//====Get SSD File Index
	private short getSSDfidIndex(short fid, short creatFile){
		short cnt = (short)0;
		
		for(cnt=(short)0; cnt<ssdFilenum; cnt++){
			if(fid == ssdFidList[cnt])
				return cnt;
		}
		if (creatFile==(short)0x01 && cnt<SSD_TOTAL_FILE_NUM)
		{
			ssdFidList[cnt] = fid;
			return cnt;
		}
		else
			return (short)0xFFFF;
	}
		
	public void uninstall(){
	}
}
