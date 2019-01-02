package com.taisys.Slimduet.Applet;

import javacard.framework.*;

/*
 * @Author: Nelson
 * 
 */

public class SSDfileSystem{
	protected short file_id;
	protected byte file_attribute;  // 0 = transparent ; 1 = linear fixed
	protected short file_size;
	private byte[] file_data;
	protected short recordLength;
	protected short recordNumber;
	
	SSDfileSystem(short file_id,byte file_attribute,short file_size){
		this.file_id = file_id;
		this.file_attribute = file_attribute;
		this.file_size = file_size;
		file_data = new byte[file_size];
		Util.arrayFillNonAtomic(file_data, (short)0, file_size, (byte)0xFF);
	}
	SSDfileSystem(short file_id,byte file_attribute,short file_size,short recordLength){
		this.file_id = file_id;
		this.file_attribute = file_attribute;
		this.file_size = file_size;
		this.recordLength = recordLength;
		this.recordNumber = (short)(file_size / recordLength);
		file_data = new byte[file_size];
		Util.arrayFillNonAtomic(file_data, (short)0, file_size, (byte)0xFF);
	}
	public short getFileItem(byte[] itemData, short itemOfs, short itemNum){ 
		short count;
		short tempOfs = (short)0;
		for(count=(short)0; count<itemNum; count++){
			tempOfs += (short)((short)((short)file_data[tempOfs]&0x00FF) + (short)1);
		}
		if(tempOfs >= file_size){
			ISOException.throwIt(ISO7816.SW_DATA_INVALID);
			
			return (short)0x00FF;
		}
		Util.arrayCopyNonAtomic(file_data, (short)(tempOfs + (short)1), itemData, itemOfs, file_data[tempOfs]);
	
		return (short)((short)file_data[tempOfs] & (short)0x00ff);
	}
	public short getFileItem(short recNumber, byte mode, short recOffset, byte[] resp, short respOffset){ 
		short count;
		short tempOfs = (short)0;
		if((short)(file_size / recordLength) < recNumber){
			ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
			
			return (short)0x00FF;
		}
		if(recNumber > 0){
			recNumber--;
			for(count=(short)0; count < recNumber; count++){
			tempOfs += recordLength;
			}
			Util.arrayCopyNonAtomic(file_data, (short)(tempOfs + recOffset), resp, respOffset, (short)(recordLength - recOffset));
		}
		return recordLength;
	}
	public void readBinary(short fileOffset, byte[] resp, short respOffset, short respLength){ 
		if((short)(fileOffset + respLength) > file_size){
			ISOException.throwIt(ISO7816.SW_LAST_COMMAND_EXPECTED );
			
			return;
		}
		Util.arrayCopyNonAtomic(file_data, fileOffset, resp, respOffset, respLength);
	}
	public void readRecord(short recNumber, byte mode, short recOffset, byte[] resp, short respOffset, short respLength){ 
		if((short)(file_size / recordLength) < recNumber){
			ISOException.throwIt(ISO7816.SW_LAST_COMMAND_EXPECTED );
			
			return;
		}
		if(recNumber > 0){
			recNumber--; 
			Util.arrayCopyNonAtomic(file_data, (short)(recNumber * recordLength + recOffset), resp, respOffset, respLength);
		}
	}
	public short updateBinary(short fileOffset, byte[] data, short dataOffset, short dataLength){ 
		if((short)(fileOffset + dataLength) > file_size){
			ISOException.throwIt(ISO7816.SW_LAST_COMMAND_EXPECTED );
			
			return (short)0x00FF;
		}
		Util.arrayCopyNonAtomic(data, dataOffset, file_data, fileOffset, dataLength);
		return dataLength;
	}
	public short updateRecord(short recNumber, byte mode, short recOffset, byte[] data, short dataOffset, short dataLength){
		if((short)(file_size / recordLength) < recNumber){
			ISOException.throwIt(ISO7816.SW_LAST_COMMAND_EXPECTED );
			
			return (short)0x00FF;
		}
		if (dataLength!=recordLength)
		{
			ISOException.throwIt((short)(ISO7816.SW_WRONG_LENGTH+recordLength));
		}
		if(recNumber > 0){
			recNumber--;
			Util.arrayCopyNonAtomic(data, dataOffset, file_data, (short)(recNumber * recordLength + recOffset), dataLength);
		}
		return dataLength;
	}
	
}