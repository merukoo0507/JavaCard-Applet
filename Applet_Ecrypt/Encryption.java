package com.taisys.Slimduet.Applet;

import javacard.framework.*;
import javacard.security.KeyBuilder;
import javacard.security.*;
import javacardx.crypto.*;

public class Encryption{
		
	//====ECB Encryption
	public static final byte INS_SET_DES_KEY              = (byte)0x20;
    public static final byte INS_SET_DES_ICV              = (byte)0x21;
    public static final byte INS_DO_DES_CIPHER            = (byte)0x22;
	
    public static byte[] desKey;
    public static byte[] desICV;
	
    public static Cipher desEcbCipher;
    public static Cipher desCbcCipher;
	
    public static Key tempDesKey2;
    public static Key tempDesKey3;
	
	public static byte desLen;
	public static byte[] EcryptData;
	
	public static void iniEncryption()
	{
		desLen = 32;
		EcryptData = new byte[desLen];
		Encryption.desKey = new byte[] {
				(byte) '1', (byte) '0', (byte) 'D', (byte) '7', (byte) '6', (byte) 'B', (byte) '2', (byte) 'E',
				(byte) '6', (byte) '4', (byte) 'D', (byte) '4', (byte) '1', (byte) '0', (byte) '7', (byte) 'D',
				(byte) 'E', (byte) '1', (byte) '9', (byte) 'C', (byte) '9', (byte) 'B', (byte) 'F', (byte) 'D',
				(byte) 'B', (byte) '6', (byte) 'D', (byte) '5', (byte) '6', (byte) '9', (byte) '8', (byte) '1'};
		
        Encryption.desICV = new byte[8];
		//Create a DES ECB/CBS object instance of the DES algorithm.
        Encryption.desEcbCipher = Cipher.getInstance(Cipher.ALG_DES_ECB_NOPAD, false);
        Encryption.desCbcCipher = Cipher.getInstance(Cipher.ALG_DES_CBC_NOPAD, false);
		//Creates uninitialized TDES cryptographic keys for signature and cipher algorithms. 
        //Encryption.tempDesKey2 = KeyBuilder.buildKey(KeyBuilder.TYPE_DES_TRANSIENT_DESELECT, KeyBuilder.LENGTH_DES3_2KEY, false);
        Encryption.tempDesKey3 = KeyBuilder.buildKey(KeyBuilder.TYPE_DES_TRANSIENT_DESELECT, KeyBuilder.LENGTH_DES3_2KEY, false);
		
	}
	
	//Set the key of TDES Encrypt/Decrypt
    public static void setDesKey(APDU apdu)
    {
        byte[] buffer = apdu.getBuffer();
		short len = buffer[ISO7816.OFFSET_LC];
		
        //Copy the incoming TDES Key value to the global variable 'Encryption.desKey'
		Util.arrayCopy(apdu.getBuffer(), ISO7816.OFFSET_CDATA, Encryption.desKey, (short)0, len);
    }
	
	//Set DES ICV, ICV is the initial vector
	public static void setDesICV(APDU apdu)
	{
        byte[] buffer = apdu.getBuffer();
		short len = buffer[ISO7816.OFFSET_LC];
		Util.arrayCopy(apdu.getBuffer(), ISO7816.OFFSET_CDATA, Encryption.desICV, (short)0, len);
	}

	public static Key getDesKey()
	{
		Key tempDesKey = Encryption.tempDesKey3;
		//Set the 'Encryption.desKey' key data value into the internal representation
		((DESKey)tempDesKey).setKey(Encryption.desKey, (short)0);
		return tempDesKey;
	}

	//DES algorithm encrypt and decrypt
	public static void doDesCipher(APDU apdu, short len)
	{
        byte[] buffer = apdu.getBuffer();
		
		Key key = getDesKey();
		byte mode = buffer[ISO7816.OFFSET_P1] == (byte)0x00 ? Cipher.MODE_ENCRYPT : Cipher.MODE_DECRYPT;
		Cipher cipher = buffer[ISO7816.OFFSET_P2] == (byte)0x00 ? Encryption.desEcbCipher : Encryption.desCbcCipher;
		//Initializes the 'cipher' object with the appropriate Key and algorithm specific parameters.
		//DES algorithms in CBC mode expect a 8-byte parameter value for the initial vector(IV)
		if (cipher == Encryption.desCbcCipher)
		{
			cipher.init(key, mode, Encryption.desICV, (short)0, (short)8);
		}
		else
		{
			cipher.init(key, mode);
		}
		//This method must be invoked to complete a cipher operation. Generates encrypted/decrypted output from all/last input data.
		cipher.doFinal(Encryption.EcryptData, (short)0, len, Encryption.EcryptData, (short)0);
		Util.arrayCopyNonAtomic(Encryption.EcryptData, (short)0, buffer, ISO7816.OFFSET_CDATA, len);
		apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, len);
	}
	
	
}
