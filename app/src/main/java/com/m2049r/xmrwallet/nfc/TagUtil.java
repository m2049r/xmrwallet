package com.m2049r.xmrwallet.nfc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;


/**
 * It's an utility class for NFC related operations include Read/Write/Authentication/ChangeKey/Lock/
 */
public class TagUtil {

    private static final int TAGUTIL_TYPE_ULTRALIGHT = 1;
    private static final int TAGUTIL_TYPE_CLASSIC = 2;
    private static final int TAGUTIL_NfcA = 3;
    private static final byte PAGE_ADDR_AUTH0 = (byte) 241;//page no 241
    private static final byte PAGE_ADDR_AUTH1 = (byte) 242;//page no 242

    private static String uid;
    private static String finalPage;

    private int tagType;

    private byte[] ivDefault = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};// default iv
    private byte[] random = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};// radmon number

    public byte[] getRandom() {
        return random;
    }

    public void setRandom(byte[] random) {
        this.random = random;
    }

    private boolean authorised = true;

    private static NfcA nfcA = null;

    public TagUtil(String u, int type) {
        uid = u;
        tagType = type;
    }

    /**
     * get the TagUtil obj, throw exception for not supported nfc tag.
     *
     * @param intent      intent
     * @param isCheckSUM: add a checksum flag at the end of the command (for some cellphone use MTK chip, we should set this param true).
     *                    How to know mtk chip:  by getprop() method
     *                    if  ro.mediatek.gemini_support=true
     *                    then
     *                    it is  mtk
     * @return TagUtil
     * @throws Exception: will throw this exception for unsupported nfc tag
     */
    public static TagUtil selectTag(Intent intent, boolean isCheckSUM) throws Exception {
        String action = intent.getAction();
        int type = 0;
        if (isSupportedAction(action)) {
            // get TAG in the intent
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String[] tagTypes = tagFromIntent.getTechList();
            String tagType = null;
            for (int i = 0; i < tagFromIntent.getTechList().length; i++) {
                if (type > 0)
                    continue;
                if (tagTypes != null && tagTypes.length > 0) {
                    tagType = tagFromIntent.getTechList()[i];
                }
                if ("android.nfc.tech.NfcA".equals(tagType)) {
                    try {
                        getTagUID_NfcA(tagFromIntent, isCheckSUM);
                        type = TAGUTIL_NfcA;

                    } catch (Exception ex) {
                        throw ex;
                    }
                } else {
                    throw new Exception("unsupported action " + action + " only support ACTION_TECH_DISCOVERED or ACTION_TAG_DISCOVERED or ACTION_NDEF_DISCOVERED");
                }
            }
            tagFromIntent = null;
            return new TagUtil(uid, type);
        }
        return null;
    }

    /**
     * read one page( four bytes in a page),
     *
     * @param intent      intent
     * @param addr:       page address
     * @param isCheckSum:
     * @return 4 bytes array
     * @throws AuthenticationException
     * @throws Exception
     */
    public byte[] readOnePage(Intent intent, byte addr, boolean isCheckSum) throws AuthenticationException, Exception {
        if (tagType == TagUtil.TAGUTIL_NfcA)
            return readOnePage_NfcA(intent, addr, isCheckSum);
        else
            return null;
    }


    private byte[] readOnePage_NfcA(Intent intent, byte addr, boolean isCheckSum) throws AuthenticationException, Exception {
        String action = intent.getAction();
        byte[] result = null;
        if (isSupportedAction(action)) {
            try {
                if (authorised) {
                    byte[] data0 = new byte[2];
                    byte[] dataWithCheckSum = new byte[4];
                    data0[0] = 0x30;
                    data0[1] = addr;
                    byte[] data1;
                    if (isCheckSum) {
                        byte[] checkSum = getCheckSum(data0);
                        dataWithCheckSum[0] = data0[0];
                        dataWithCheckSum[1] = data0[1];
                        dataWithCheckSum[2] = checkSum[0];
                        dataWithCheckSum[3] = checkSum[1];
                        data1 = nfcA.transceive(dataWithCheckSum);// read 4 pages one time
                    } else
                        data1 = nfcA.transceive(data0);// read 4 pages one time

                    result = new byte[4];
                    if (data1.length < 16)
                        throw new AuthenticationException("please authenticate first!");
                    else
                        System.arraycopy(data1, 0, result, 0, 4);// get the first page
                } else {
                    throw new AuthenticationException("Authenticate First!");
                }
            } catch (Exception e) {
                throw e;
            }
        }
        return result;
    }

    /**
     * read four pages（ 4 bytes in a page）
     *
     * @param intent      intent
     * @param addr:       start page to read
     * @param isCheckSum:
     * @return 16 bytes array
     * @throws Exception Exception
     */
    public byte[] readFourPage(Intent intent, byte addr, boolean isCheckSum) throws Exception {
        if (tagType == TagUtil.TAGUTIL_NfcA)
            return readFourPage_NfcA(intent, addr, isCheckSum);
        else
            return null;
    }


    private byte[] readFourPage_NfcA(Intent intent, byte addr, boolean isCheckSum) throws Exception {
        String action = intent.getAction();
        byte[] result = null;
        if (isSupportedAction(action)) {
            try {
                if (authorised) {
                    byte[] data0 = new byte[2];
                    byte[] dataWithCheckSum = new byte[4];
                    data0[0] = 0x30;
                    data0[1] = addr;
                    byte[] data1;
                    if (isCheckSum) {
                        byte[] checkSum = getCheckSum(data0);
                        dataWithCheckSum[0] = data0[0];
                        dataWithCheckSum[1] = data0[1];
                        dataWithCheckSum[2] = checkSum[0];
                        dataWithCheckSum[3] = checkSum[1];
                        result = nfcA.transceive(dataWithCheckSum);
                    } else
                        result = nfcA.transceive(data0);
                } else {
                    throw new AuthenticationException("please authenticate first!");
                }
            } catch (Exception e) {
                throw e;
            }
        }
        return result;
    }


    /**
     * @param intent      intent
     * @param startPageNO start page to write
     * @param contents    bytes to write
     * @param isCheckSum  add checksum at the end of command.
     * @return true or false
     * @throws Exception Exception
     */
    public boolean writePages(Intent intent, int startPageNO, byte contents[], boolean isCheckSum)
            throws Exception {
        boolean res = false;
        if (startPageNO < 4 || startPageNO > 239)
            throw new AuthenticationException("page no should between [4,239]");
        try {
            byte newByteArray[] = appendByteArray(contents);
            int pageNum = newByteArray.length / 4;
            byte array[] = new byte[4];
            for (int i = 0; i < pageNum; i++) {
                array[0] = newByteArray[4 * i];
                array[1] = newByteArray[1 + 4 * i];
                array[2] = newByteArray[2 + 4 * i];
                array[3] = newByteArray[3 + 4 * i];
                try {
                    writeOnePage(intent, (startPageNO + i), array, false);
                } catch (Exception e) {
                    throw new Exception((new StringBuilder()).append("write page ").append(startPageNO + i).append(" failed").toString());
                }
            }

            res = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public boolean writeOnePage(Intent intent, int addr, byte[] contents, boolean isCheckSum) throws Exception {
        boolean result = false;
        String action = intent.getAction();
        if (isSupportedAction(action)) {

            try {
                if (authorised) {
                    if (contents != null && contents.length == 4) {
                        byte[] data2 = new byte[6];
                        byte[] dataWithCheckSum = new byte[8];
                        data2[0] = (byte) 0xA2;
                        data2[1] = (byte) addr;
                        data2[2] = contents[0];
                        data2[3] = contents[1];
                        data2[4] = contents[2];
                        data2[5] = contents[3];
                        byte[] data3;
                        if (isCheckSum) {
                            byte[] checkSum = getCheckSum(data2);
                            dataWithCheckSum[0] = data2[0];
                            dataWithCheckSum[1] = data2[1];
                            dataWithCheckSum[2] = data2[2];
                            dataWithCheckSum[3] = data2[3];
                            dataWithCheckSum[4] = data2[4];
                            dataWithCheckSum[5] = data2[5];
                            dataWithCheckSum[6] = checkSum[0];
                            dataWithCheckSum[7] = checkSum[1];
                            data3 = nfcA.transceive(dataWithCheckSum);
                        } else
                            data3 = nfcA.transceive(data2);
                        result = true;
                    } else {
                        throw new AuthenticationException("contents must be four bytes");
                    }
                } else {
                    throw new AuthenticationException("please authenticate first!");
                }
            } catch (Exception e) {
                throw e;
            }
        }
        return result;
    }

    private static boolean isSupportedAction(String action) {
        return NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) || NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action);
    }

    /**
     * authentication,key is byte array.
     *
     * @param intent     intent
     * @param key        byte array, length=16 bytes
     * @param isCheckSum add checksum or not
     * @return true or false
     * @throws Exception Exception
     */
    public boolean authentication(Intent intent, byte[] key, boolean isCheckSum) throws Exception {
        if (tagType == TagUtil.TAGUTIL_NfcA) {
            return authentication_NfcA(intent, key, isCheckSum);
        } else
            throw new Exception("unknow tag Type" + tagType);
    }


    private boolean authentication_NfcA(Intent intent, byte[] binaryKey, boolean isCheckSum) {
        boolean result = false;
        String action = intent.getAction();
        if (isSupportedAction(action)) {
            try {
                byte[] data = new byte[24];
                System.arraycopy(binaryKey, 0, data, 0, 16);
                System.arraycopy(binaryKey, 0, data, 16, 8);
                accreditation(data, isCheckSum);
                authorised = true;
                return true;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * read some pages from the fourth page
     *
     * @param intent:
     * @param pageNums:   how many pages should read.
     * @param isCheckSum:
     * @return byte array
     * @throws Exception Exception
     */
    public byte[] readAllPages(Intent intent, int pageNums, boolean isCheckSum) throws Exception {

        if (tagType == TagUtil.TAGUTIL_NfcA)
            return readAllPages_NfcA(intent, pageNums, isCheckSum);

        else
            return null;
    }


    private byte[] readAllPages_NfcA(Intent intent, int pageNums, boolean isCheckSum) throws Exception {
        int byteNum = pageNums * 4; // 4 bytes a page
        byte[] result = new byte[byteNum];
        try {
            if (authorised) {
                for (int i = 0x04; i < byteNum / 4; i++) {
                    byte[] data0 = new byte[2];
                    byte[] dataWithCheckSum = new byte[4];
                    data0[0] = 0x30;
                    data0[1] = (byte) i;

                    byte[] data1;
                    if (isCheckSum) {
                        byte[] checkSum = getCheckSum(data0);
                        dataWithCheckSum[0] = data0[0];
                        dataWithCheckSum[1] = data0[1];
                        dataWithCheckSum[2] = checkSum[0];
                        dataWithCheckSum[3] = checkSum[1];
                        data1 = nfcA.transceive(dataWithCheckSum);
                    } else
                        data1 = nfcA.transceive(data0);
                    if (data1.length >= 4)
                        System.arraycopy(data1, 0, result, 4 * i, 4);// get one page
                    else
                        throw new Exception("read the" + i + "th page failed! " + data1.length + "bytes was read");
                }
                return result;
            } else {
                throw new AuthenticationException("please authenticate first!");
            }
        } catch (Exception e) {
            throw e;
        }
    }

    public boolean writeNewKey(Intent intent, String newKey, boolean isCheckSum) throws Exception {
        boolean result = false;
        String action = intent.getAction();
        if (isSupportedAction(action)) {
            try {
                if(authorised){
                    String dataString = newKey;
                    //判断输入的数据

                    byte[] dataX = hexStringToBytes(dataString);
                    byte[] dataY = new byte[16];
                    for(int i=0;i<16;i++){
                        dataY[i] = dataX[15-i];
                        System.out.println("mi"+dataY[i]);
                    }
                    byte[] data1 = new byte[6];
                    byte[] data1WithCheckSum= new byte[8];
                    data1[0] = (byte) 0xA2;
                    data1[1] = (byte) 0xF5;
                    System.arraycopy(dataY, 8, data1, 2, 4);

                    byte[] data2 = new byte[6];
                    byte[] data2WithCheckSum= new byte[8];
                    data2[0] = (byte) 0xA2;
                    data2[1] = (byte) 0xF6;
                    System.arraycopy(dataY, 12, data2, 2, 4);

                    byte[] data3 = new byte[6];
                    byte[] data3WithCheckSum= new byte[8];
                    data3[0] = (byte) 0xA2;
                    data3[1] = (byte) 0xF7;
                    System.arraycopy(dataY, 0, data3, 2, 4);

                    byte[] data4 = new byte[6];
                    byte[] data4WithCheckSum= new byte[8];
                    data4[0] = (byte) 0xA2;
                    data4[1] = (byte) 0xF8;
                    System.arraycopy(dataY, 4, data4, 2, 4);

//						mfc.connect();
//						accreditation(mfc,secretKeyDefault);//认证


                    if(isCheckSum)
                    {
                        byte[] checkSum = getCheckSum(data1);
                        for(int i=0;i<6;i++)
                            data1WithCheckSum[i]=data1[i];
                        data1WithCheckSum[6]=checkSum[0];
                        data1WithCheckSum[7]=checkSum[1];
                        nfcA.transceive(data1WithCheckSum);// 每次读出来的数据为4page的数据
                    }
                    else
                        nfcA.transceive(data1);

                    if(isCheckSum)
                    {
                        byte[] checkSum = getCheckSum(data2);
                        for(int i=0;i<6;i++)
                            data2WithCheckSum[i]=data2[i];
                        data2WithCheckSum[6]=checkSum[0];
                        data2WithCheckSum[7]=checkSum[1];
                        nfcA.transceive(data2WithCheckSum);// 每次读出来的数据为4page的数据
                    }
                    else
                        nfcA.transceive(data2);

                    if(isCheckSum)
                    {
                        byte[] checkSum = getCheckSum(data3);
                        for(int i=0;i<6;i++)
                            data3WithCheckSum[i]=data3[i];
                        data3WithCheckSum[6]=checkSum[0];
                        data3WithCheckSum[7]=checkSum[1];
                        nfcA.transceive(data3WithCheckSum);// 每次读出来的数据为4page的数据
                    }
                    else
                        nfcA.transceive(data3);

                    if(isCheckSum)
                    {
                        byte[] checkSum = getCheckSum(data4);
                        for(int i=0;i<6;i++)
                            data4WithCheckSum[i]=data4[i];
                        data4WithCheckSum[6]=checkSum[0];
                        data4WithCheckSum[7]=checkSum[1];
                        nfcA.transceive(data4WithCheckSum);// 每次读出来的数据为4page的数据
                    }
                    else
                        nfcA.transceive(data4);
                    result = true;

                }else{
                    throw new AuthenticationException("please authenticate first!");
                }
            }catch (NumberFormatException e) {
                throw new Exception("new key: "+newKey+" is not correct." +" key must be 32 hex chars");
            } catch (Exception e) {
                throw e;
            }

        }
        return result;
    }


    /**
     * set authenticated access level and the start address after which we need authenticaion for access.
     * access level: 0 for read/write access，1 for write access
     *
     * @param intent         intent
     * @param startAddr:     start address.
     * @param operationType: 0x00 for write operation, 0x80 for read/write operation.
     * @param isCheckSum:
     * @return true or false
     * @throws Exception Exception
     */
    public boolean setAccess(Intent intent, byte startAddr, byte operationType, boolean isCheckSum) throws Exception {
        if (tagType == TagUtil.TAGUTIL_NfcA)
            return setAccess_NfcA(intent, startAddr, operationType, isCheckSum);
        else
            throw new Exception("unknow tag Type " + tagType);
    }


    private boolean setAccess_NfcA(Intent intent, byte startAddr, byte operationType, boolean isCheckSum) throws Exception {

        byte[] authAddrPage = this.readOnePage(intent, PAGE_ADDR_AUTH0, false);
        byte[] opertionTypePage = this.readOnePage(intent, PAGE_ADDR_AUTH1, false);
        authAddrPage[3] = startAddr;
        opertionTypePage[0] = operationType;
        boolean result1 = this.writeOnePage(intent, PAGE_ADDR_AUTH0, authAddrPage, isCheckSum);
        boolean result2 = this.writeOnePage(intent, PAGE_ADDR_AUTH1, opertionTypePage, isCheckSum);
        if (result1 && result2)
            return true;
        else
            return false;
    }

    /**
     * lock the locking bit
     *
     * @param intent     intent
     * @param isCheckSum add checksum at the end of commands
     * @return true or false
     * @throws Exception Exception
     */
    public boolean lockLockingbits(Intent intent, boolean isCheckSum) throws Exception {
        if (tagType == TagUtil.TAGUTIL_NfcA)
            return lockLockingbits_NfcA(intent, isCheckSum);
        else
            throw new Exception("unknow tag Type" + tagType + ". or SelectTag first.");
    }

    private boolean lockLockingbits_NfcA(Intent intent, boolean isCheckSum) throws Exception {
        byte[] contents1 = new byte[4];
        contents1[0] = (byte) 0;
        contents1[1] = (byte) 0;
        contents1[2] = (byte) 7;
        contents1[3] = (byte) 0;

        byte[] contents2 = new byte[4];
        contents2[0] = (byte) 17;
        contents2[1] = (byte) 15;
        contents2[2] = (byte) 0;
        contents2[3] = (byte) 0;

        if (writeOnePage(intent, (byte) 2, contents1, isCheckSum) && writeOnePage(intent, (byte) 40, contents2, isCheckSum))
            return true;
        else
            return false;
    }

    private byte[] getBytesArray(int value) {
        byte[] contents = new byte[4];
        contents[0] = (byte) (value >>> 24);
        contents[1] = (byte) (value >>> 16);
        contents[2] = (byte) (value >>> 8);
        contents[3] = (byte) (value);
        return contents;
    }


    public int getTagType() {
        return tagType;
    }

    private void accreditation(byte[] secretKeys, boolean isCheckSum) throws Exception {
        byte[] iv = ivDefault;

        byte[] command0 = new byte[2];// first command
        byte[] command0WithCheckSum = new byte[4];// first command(with check sum)

        byte[] command1;// first response
        byte[] command1WithCheckSum = null;// first response with check sum

        byte[] command2;// command1 without first byte.

        byte[] command3;// decode command1
        byte[] command4;// command2 encode
        byte[] command5;// command3 left move
        byte[] command6;// RNDB (command5 and command4 encode)
        byte[] command7;
        byte[] command8;
        byte[] command9;
        byte[] command10;
        byte[] command11;

        command0[0] = (byte) 0x1A; // command
        command0[1] = (byte) 0x00; // flag
        if (isCheckSum) {
            byte[] checkSum = getCheckSum(command0);
            command0WithCheckSum[0] = command0[0];
            command0WithCheckSum[1] = command0[1];
            command0WithCheckSum[2] = checkSum[0];
            command0WithCheckSum[3] = checkSum[1];
            command1WithCheckSum = nfcA.transceive(command0WithCheckSum);// 11 bytes
            if (command1WithCheckSum.length != 11) {
                String str = "";
                for (int i = 0; i < command1WithCheckSum.length; i++) {
                    str = str + " byte" + i + "=" + command1WithCheckSum[i] + "  ";
                }
                throw new Exception("length of response is not 11 bytes. the response bytes is: " + str);
            }
            command1 = new byte[9];
            System.arraycopy(command1WithCheckSum, 0, command1, 0, 9);
        } else {
            command1 = nfcA.transceive(command0);
        }
        command2 = new byte[8];
        if (command1.length != 9) {
            String str = "";
            for (int i = 0; i < command1.length; i++) {
                str = str + " byte" + i + "=" + command1[i] + "  ";
            }
            throw new Exception("length of response is not 9 bytes. the response bytes is: " + str);
        }
        System.arraycopy(command1, 1, command2, 0, 8);
        command3 = ThreeDES.decode(command2, iv, secretKeys);
        iv = command2;
        command4 = ThreeDES.encode(random, iv, secretKeys);
        iv = command4;
        command5 = new byte[8];
        System.arraycopy(command3, 1, command5, 0, 7);
        command5[7] = command3[0];
        command6 = ThreeDES.encode(command5, iv, secretKeys);

        command7 = new byte[16];
        System.arraycopy(command4, 0, command7, 0, 8);
        System.arraycopy(command6, 0, command7, 8, 8);
        command8 = new byte[17];

        command8[0] = (byte) 0xAF;
        System.arraycopy(command7, 0, command8, 1, 16);

        if (isCheckSum) {
            byte[] command8WithCheckSum = new byte[19];
            byte[] checkSum = getCheckSum(command8);
            System.arraycopy(command8, 0, command8WithCheckSum, 0, 17);
            command8WithCheckSum[17] = checkSum[0];
            command8WithCheckSum[18] = checkSum[1];
            command9 = nfcA.transceive(command8WithCheckSum);
        } else {
            command9 = nfcA.transceive(command8);
        }
        command10 = new byte[8];
        System.arraycopy(command9, 1, command10, 0, 8);
        iv = command6;
        command11 = ThreeDES.decode(command10, iv, secretKeys);
    }


    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    private static byte charToByte(char c) {
        int i = "0123456789ABCDEF".indexOf(c);
        if (i == -1) {
            throw new NumberFormatException();
        } else {
            return (byte) i;
        }
    }

    private static String StringtoHexString(String str) {
        String hexString = "0123456789ABCDEF";
        byte bytes[] = str.getBytes();
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            sb.append(hexString.charAt((bytes[i] & 240) >> 4));
            sb.append(hexString.charAt((bytes[i] & 15) >> 0));
        }

        return sb.toString();
    }

    public static String hexStringToString(String bytes) {
        String hexString = "0123456789ABCDEF";
        ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length() / 2);
        for (int i = 0; i < bytes.length(); i += 2)
            baos.write(hexString.indexOf(bytes.charAt(i)) << 4 | hexString.indexOf(bytes.charAt(i + 1)));

        return new String(baos.toByteArray());
    }

    public static byte[] StringtoBytes(String str) {
        String hexstr = StringtoHexString(str);
        return hexStringToBytes(hexstr);
    }

    private byte[] appendByteArray(byte byteArray[]) {
        int length = byteArray.length;
        int m = length % 4;
        byte newByteArray[];
        if (m == 0)
            newByteArray = new byte[length];
        else
            newByteArray = new byte[length + (4 - m)];
        System.arraycopy(byteArray, 0, newByteArray, 0, length);
        return newByteArray;
    }


    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    private static void getTagUID_NfcA(Tag tag, boolean isCheckSum) throws Exception {
        nfcA = NfcA.get(tag);
        try {
            nfcA.connect();
            byte[] datas = new byte[2];
            byte[] datasWithCheckSum = new byte[4];
            datas[0] = 0x30;
            datas[1] = 0x00;
            byte[] datar;
            if (isCheckSum) {
                byte[] checkSum = getCheckSum(datas);
                datasWithCheckSum[0] = datas[0];
                datasWithCheckSum[1] = datas[1];
                datasWithCheckSum[2] = checkSum[0];
                datasWithCheckSum[3] = checkSum[1];
                datar = nfcA.transceive(datasWithCheckSum);
            } else
                datar = nfcA.transceive(datas);
            byte[] datau = new byte[7];//uid
            System.arraycopy(datar, 0, datau, 0, 3);
            System.arraycopy(datar, 4, datau, 3, 4);
            uid = bytesToHexString(datau);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static String getUid() {
        return uid;
    }


    public int getAuthenticationAddr(Intent intent, boolean isCheckSum) throws Exception {
        byte[] result = readOnePage(intent, PAGE_ADDR_AUTH0, isCheckSum);
        return result[0];
    }

    public int getAuthenticationType(Intent intent, boolean isCheckSum) throws Exception {
        byte[] result = readOnePage(intent, PAGE_ADDR_AUTH1, isCheckSum);
        return result[0];
    }

    /**
     * close the nfc connection
     */
    public void close() {
        try {
            if (nfcA != null)
                nfcA.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] getCheckSum(byte[] byteAyyay) {
        CRC16 checksum = new CRC16();
        String hexString = Long.toHexString(checksum.getCRC16(byteAyyay));
        return reverse(hexStringToBytes(hexString));
    }


    public static byte[] reverse(byte[] bytes) {
        byte[] result = new byte[bytes.length];

        for (int i = 0; i < bytes.length; i++) {
            result[i] = bytes[bytes.length - i - 1];
        }
        return result;
    }

}
