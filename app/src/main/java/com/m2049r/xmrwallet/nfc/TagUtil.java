package com.m2049r.xmrwallet.nfc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;

import jonelo.jacksum.JacksumAPI;
import jonelo.jacksum.algorithm.AbstractChecksum;
import jonelo.jacksum.ui.ExitStatus;
import jonelo.sugar.util.ExitException;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcA;
import android.util.Log;


/**
 *
 * @author jianjunchu
 *
 */
public class TagUtil {

    private static final int TAGUTIL_TYPE_ULTRALIGHT = 1;
    private static final int TAGUTIL_TYPE_CLASSIC = 2;
    private static final int TAGUTIL_NfcA = 3;
    private static final byte PAGE_ADDR_AUTH0 = 42;
    private static final byte PAGE_ADDR_AUTH1 = 43;

    //	private static android.nfc.Tag tag;

    private static String uid;
    private static String finalPage;

    private int tagType;

    private byte[] secretKey;
    private byte[] ivDefault = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };// default iv
    private byte[] random = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 };// radmon number
    public byte[] getRandom() {
        return random;
    }

    public void setRandom(byte[] random) {
        this.random = random;
    }

    private boolean authorised = true;
    private String ERR_MSG;

    private static NfcA nfcA=null;

    public TagUtil(String u,int type)
    {
        uid=u;
        tagType = type;
    }

    /**
     * get the TagUtil obj, throw exception for not supported nfc tag.
     * @param intent
     * @param isCheckSUM: add a checksum flag at the end of the command (for some cellphone use MTK chips, we should set this param true).
     * How to know mtk chip:  by getprop() method
     *  if  ro.mediatek.gemini_support=true
     *  then
     *      it is  mtk
     * @return
     * @throws Exception:  will throw this exception for unsupported nfc tag
     */
    public static TagUtil selectTag(Intent intent,boolean isCheckSUM) throws Exception
    {
        String action = intent.getAction();
        int type=0;
        if (isSupportedAction(action)) {
            // get TAG in the intent
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String[] tagTypes = tagFromIntent.getTechList();// 支持的类型集合
            String tagType = null;
            for(int i=0;i<tagFromIntent.getTechList().length;i++)
            {
                if(type>0)
                    continue;
                if (tagTypes != null && tagTypes.length > 0) {
                    tagType = tagFromIntent.getTechList()[i];
                }
                if ("android.nfc.tech.NfcA".equals(tagType)) {
                    try{
                        getTagUID_NfcA(tagFromIntent,isCheckSUM);
                        type=TAGUTIL_NfcA;

                    }catch(Exception ex)
                    {
                        throw ex;
                    }
                }
                else
                {
                    throw new Exception("unsupported action "+action +" only support ACTION_TECH_DISCOVERED or ACTION_TAG_DISCOVERED or ACTION_NDEF_DISCOVERED");
                }
            }
            tagFromIntent = null;
            TagUtil tagUtil = new TagUtil(uid,type);
            return tagUtil;
//		if(checkTag(type))
//		{
//			TagUtil tagUtil = new TagUtil(uid,type);
//			return tagUtil;
//		}
//		else
//			throw new Exception ("illegal tag");
        }
        return null;
    }

    /**
     * read one page( four bytes in a page),
     * @param intent
     * @param addr:  page address
     * @param isCheckSum:
     * @return 4 bytes array
     * @throws AuthenticationException
     * @throws Exception
     */
    public byte[] readOnePage(Intent intent,byte addr,boolean isCheckSum) throws AuthenticationException,Exception
    {
        if(tagType==TagUtil.TAGUTIL_NfcA)
            return readOnePage_NfcA( intent, addr,isCheckSum);
        else
            return null;
    }


    private byte[] readOnePage_NfcA(Intent intent,byte addr, boolean isCheckSum) throws AuthenticationException,Exception
    {
        String action = intent.getAction();
        byte[] result = null;
        if (isSupportedAction(action)) {
            //Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            //NfcA mfc = NfcA.get(tagFromIntent);
            try {
                if(authorised){
                    //mfc.connect();
                    //accreditation(mfc,secretKey);//认证
                    byte[] data0 = new byte[2];
                    byte[] dataWithCheckSum = new byte[4];
                    data0[0] = 0x30;
                    data0[1] = addr;
                    byte[] data1;
                    if(isCheckSum)
                    {
                        byte[] checkSum = getCheckSum(data0);
                        dataWithCheckSum[0]=data0[0];
                        dataWithCheckSum[1]=data0[1];
                        dataWithCheckSum[2]=checkSum[0];
                        dataWithCheckSum[3]=checkSum[1];
                        data1 = nfcA.transceive(dataWithCheckSum);// read 4 pages one time
                    }
                    else
                        data1 = nfcA.transceive(data0);// read 4 pages one time

                    result = new byte[4];
                    if(data1.length<16)
                        throw new AuthenticationException("please authenticate first!");
                    else
                        System.arraycopy(data1, 0, result, 0, 4);// get the first page
                }else{
                    throw new AuthenticationException("Authenticate First!");
                }
            } catch (Exception e) {
                throw e;
            }
//			finally {
//				try {
//					mfc.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
        }
        return result;
    }

    /**
     * read four pages（ 4 bytes in a page）
     * @param intent
     * @param addr:
     * @param isCheckSum:
     * @return 16 bytes array
     * @throws AuthenticationException
     * @throws Exception
     */
    public byte[] readFourPage(Intent intent,byte addr, boolean isCheckSum) throws AuthenticationException,Exception
    {
        if(tagType==TagUtil.TAGUTIL_NfcA)
            return readFourPage_NfcA( intent, addr,isCheckSum);
        else
            return null;
    }


    private byte[] readFourPage_NfcA(Intent intent,byte addr, boolean isCheckSum) throws AuthenticationException,Exception
    {
        String action = intent.getAction();
        byte[] result = null;
        if (isSupportedAction(action)) {
            //Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            //NfcA mfc = NfcA.get(tagFromIntent);
            try {
                if(authorised){
                    //mfc.connect();
                    //accreditation(mfc,secretKey);
                    byte[] data0 = new byte[2];
                    byte[] dataWithCheckSum = new byte[4];
                    data0[0] = 0x30;
                    data0[1] = addr;
                    byte[] data1;
                    if(isCheckSum)
                    {
                        byte[] checkSum = getCheckSum(data0);
                        dataWithCheckSum[0]=data0[0];
                        dataWithCheckSum[1]=data0[1];
                        dataWithCheckSum[2]=checkSum[0];
                        dataWithCheckSum[3]=checkSum[1];
                        result = nfcA.transceive(dataWithCheckSum);// 每次读出来的数据为4page的数据
                    }
                    else
                        result = nfcA.transceive(data0);// 每次读出来的数据为4page的数据
                }else{
                    throw new AuthenticationException("please authenticate first!");
                }
            } catch (Exception e) {
                throw e;
            }
//			finally {
//				try {
//					mfc.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
        }
        return result;
    }

    /**
     * write a page（4 bytes in a page）
     * @param intent
     * @param addr
     * @param contents  4 bytes array
     * @param isCheckSum:
     * @return true for success， false for failed
     * @throws AuthenticationException
     * @throws Exception
     */
    public boolean writeTag(Intent intent, byte addr, byte[] contents, boolean isCheckSum) throws AuthenticationException, Exception
    {
        if(tagType==TagUtil.TAGUTIL_NfcA)
            return writeAble( intent, addr,contents, isCheckSum);
        else
            return false;
    }

    private boolean writeAble(Intent intent, byte addr, byte contents[], boolean isCheckSum)
            throws AuthenticationException, Exception
    {
        boolean res = false;
        if((new Integer(addr)).intValue() < 4)
            throw new AuthenticationException("page no should bigger than 4");
        try
        {
            byte newByteArray[] = appendByteArray(contents);
            int pageNum = newByteArray.length / 4;
            byte array[] = new byte[4];
            for(int i = 0; i < pageNum; i++)
            {
                array[0] = newByteArray[0 + 4 * i];
                array[1] = newByteArray[1 + 4 * i];
                array[2] = newByteArray[2 + 4 * i];
                array[3] = newByteArray[3 + 4 * i];
                try
                {
                    writeTag_NfcA(intent, (byte)(addr + i), array, false);
                }
                catch(Exception e)
                {
                    throw new Exception((new StringBuilder()).append("write page ").append(addr + i).append(" failed").toString());
                }
            }

            res = true;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            Log.e("xxx", e.getMessage());
        }
        return res;
    }

    private boolean writeTag_NfcA(Intent intent, byte addr, byte[] contents, boolean isCheckSum) throws AuthenticationException, Exception
    {
        boolean result = false;
        String action = intent.getAction();
        if (isSupportedAction(action)) {
//			Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
//			NfcA mfc = NfcA.get(tagFromIntent);

            try {
                if(authorised){
                    if(contents != null && contents.length== 4){//判断输入的数据
                        //mfc.connect();
                        //accreditation(mfc,secretKey);//认证
                        byte[] data2 = new byte[6];
                        byte[] dataWithCheckSum= new byte[8];
                        data2[0] = (byte) 0xA2;
                        data2[1] = addr;
                        data2[2] = contents[0];
                        data2[3] = contents[1];
                        data2[4] = contents[2];
                        data2[5] = contents[3];
                        byte[] data3;
                        if(isCheckSum)
                        {
                            byte[] checkSum = getCheckSum(data2);
                            dataWithCheckSum[0]=data2[0];
                            dataWithCheckSum[1]=data2[1];
                            dataWithCheckSum[2]=data2[2];
                            dataWithCheckSum[3]=data2[3];
                            dataWithCheckSum[4]=data2[4];
                            dataWithCheckSum[5]=data2[5];
                            dataWithCheckSum[6]=checkSum[0];
                            dataWithCheckSum[7]=checkSum[1];
                            data3 = nfcA.transceive(dataWithCheckSum);// 每次读出来的数据为4page的数据
                        }
                        else
                            data3 = nfcA.transceive(data2);
                        result=true;
                    }else{
                        throw new AuthenticationException("contents must be four bytes");
                    }
                }
                else
                {
                    throw new AuthenticationException("please authenticate first!");
                }
            } catch (Exception e) {
                throw e;
            }
//			finally {
//				try {
//					mfc.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
        }
        return result;
    }

    private static boolean isSupportedAction(String action) {
        return NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) || NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action);
    }


    /**
     * 认证
     * @param intent
     * @param key 秘钥 16 个字符（字母和数字）。
     * @param isCheckSum: 是否增加校验位
     * @return
     * @throws AuthenticationException
     */
    public boolean authentication(Intent intent, String key, boolean isCheckSum) throws AuthenticationException, Exception
    {
        String dexString = this.bytesToHexString(key.getBytes());
        return authentication_internal(intent,dexString,isCheckSum);
    }

    /**
     * 认证
     * @param intent
     * @param key 秘钥 16 个字符（字母和数字）。
     * @param isCheckSum: 是否增加校验位
     * @return
     * @throws AuthenticationException
     */
    public boolean authentication(Intent intent, byte[] key, boolean isCheckSum) throws AuthenticationException, Exception
    {
        String dexString = this.bytesToHexString(key);
        return authentication_internal(intent,dexString,isCheckSum);
    }
    /**
     * 认证
     * @param intent
     * @param key 秘钥 16 个字节， 用   32  个 16 进制字符的字符串表示。
     * @param isCheckSum: 是否增加校验位
     * @return
     * @throws AuthenticationException
     */
    public boolean authentication_internal(Intent intent, String key, boolean isCheckSum) throws AuthenticationException, Exception
    {
        if(tagType==TagUtil.TAGUTIL_NfcA)
            return authentication_NfcA( intent, key,isCheckSum);
        else
            throw new Exception("unknow tag Type"+ tagType+". or SelectTag first.");
    }

    private boolean authentication_NfcA(Intent intent, String key, boolean isCheckSum) throws AuthenticationException
    {
        boolean result = false;
        String action = intent.getAction();
        if (isSupportedAction(action)) {
//			Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
//			NfcA mfc = NfcA.get(tagFromIntent);
            try {
                //Log.e("aaa",key);
                if(key != null && key.length() == 32){//判断输入的数据
                    byte[] data = new byte[24];
                    byte[] binaryKey = hexStringToBytes(key);
                    System.arraycopy(binaryKey, 0, data, 0, 16);
                    System.arraycopy(binaryKey, 0, data, 16, 8);
                    //mfc.connect();
                    accreditation(nfcA,data,isCheckSum);//认证
                    authorised=true;
                    secretKey = data;
                    return true;
                }else{
                    ERR_MSG = "key must be 32 hex chars,current key is "+key;
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            finally
            {
//				try {
//					mfc.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
            }
        }else
        {
            ERR_MSG=action+ " is not support"+ ", action must be on of ACTION_TECH_DISCOVERED or ACTION_TAG_DISCOVERED";
            return false;
        }
    }

    /**
     * 从第 0 页开始，读取指定页数的数据，返回一个字节数组(1 页 4 个字节)
     * @param intent:
     * @param pageNums:  指定页数
     * @param isCheckSum: 是否增加校验位
     * @return
     * @throws Exception
     */
    public byte[] readAllPages(Intent intent,int pageNums,boolean isCheckSum) throws Exception{

        if(tagType==TagUtil.TAGUTIL_NfcA)
            return readAllPages_NfcA( intent, pageNums,isCheckSum);

        else
            return null;
    }


    private byte[] readAllPages_NfcA(Intent intent, int pageNums,boolean isCheckSum) throws Exception{
//			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
//			NfcA mfc = NfcA.get(tag);
        int byteNum =pageNums*4; // 4 bytes a page
        byte[] result= new byte[byteNum];
        try {
            if(authorised){
                //mfc.connect();
                for (int i = 0x04; i < byteNum/4; i++) {
                    byte[] data0 = new byte[2];
                    byte[] dataWithCheckSum = new byte[4];
                    data0[0] = 0x30;
                    data0[1] = (byte) i;

                    byte[] data1;
                    if(isCheckSum)
                    {
                        byte[] checkSum = getCheckSum(data0);
                        dataWithCheckSum[0]=data0[0];
                        dataWithCheckSum[1]=data0[1];
                        dataWithCheckSum[2]=checkSum[0];
                        dataWithCheckSum[3]=checkSum[1];
                        data1 = nfcA.transceive(dataWithCheckSum);// 每次读出来的数据为4page的数据
                    }
                    else
                        data1 = nfcA.transceive(data0);// 4 pages
                    if(data1.length>=4)
                        System.arraycopy(data1, 0, result, 4*i, 4);// get one page
                    else
                        throw new Exception("read the" +i +"th page failed! "+data1.length +"bytes was read");
                }
                return result;
            }else{
                throw new AuthenticationException("please authenticate first!");
            }
        } catch (Exception e) {
            throw e;
        }
//			finally {
//				try {
//					mfc.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
    }

    /**
     * 改变秘钥
     * @param intent
     * @param newKey 新的秘钥
     * @param isCheckSum: 是否增加校验位
     * @return
     * @throws AuthenticationException
     * @throws Exception
     */
    public boolean writeNewKey(Intent intent,String newKey,boolean isCheckSum) throws AuthenticationException,Exception
    {
        if(newKey != null && newKey.length() == 32){
            if(tagType==TagUtil.TAGUTIL_NfcA)
                return writeNewKey_NfcA( intent,newKey,isCheckSum);
            else
                return false;
        }else
            throw new Exception("key must be 32 hex chars");
    }


    // 写入密钥
    private boolean writeNewKey_NfcA(Intent intent,String newKey, boolean isCheckSum)  throws Exception{
        boolean result = false;
        String action = intent.getAction();
        if (isSupportedAction(action)) {
//			Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
//			NfcA mfc = NfcA.get(tagFromIntent);
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
                    data1[1] = (byte) 0x2C;
                    System.arraycopy(dataY, 8, data1, 2, 4);

                    byte[] data2 = new byte[6];
                    byte[] data2WithCheckSum= new byte[8];
                    data2[0] = (byte) 0xA2;
                    data2[1] = (byte) 0x2D;
                    System.arraycopy(dataY, 12, data2, 2, 4);

                    byte[] data3 = new byte[6];
                    byte[] data3WithCheckSum= new byte[8];
                    data3[0] = (byte) 0xA2;
                    data3[1] = (byte) 0x2E;
                    System.arraycopy(dataY, 0, data3, 2, 4);

                    byte[] data4 = new byte[6];
                    byte[] data4WithCheckSum= new byte[8];
                    data4[0] = (byte) 0xA2;
                    data4[1] = (byte) 0x2F;
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
//			finally {
//				try {
//					mfc.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
        }
        return result;
    }

//    /**
//     * 设置芯片的新密码（适用于FJ8216 和   NXP216 芯片   ），在调用本方法前，要先通过 authentication216 方法进行认证。
//     * @param intent
//     * @param newPWD 四个字节的  byte 数组,新的密码
//     * @param PACK 两个字节的  byte 数组,用于设置验证成功后的返回值。
//     * @param isCheckSum
//     * @return
//     * @throws AuthenticationException
//     * @throws Exception
//     */
//    public boolean writePWD216(Intent intent,byte[] newPWD,byte[] PACK, boolean isCheckSum) throws AuthenticationException,Exception
//    {
//        if(newPWD != null && newPWD.length == 4 && PACK.length==2){
//            if(tagType==TagUtil.TAGUTIL_NfcA)
//                return writeNewKey216_NfcA( intent,newPWD,PACK,isCheckSum);
//            else if (tagType==TagUtil.TAGUTIL_TYPE_ULTRALIGHT)
//                return writeNewKey216_MifareUltraLight( intent,newPWD,PACK,isCheckSum);
//            else if (tagType==TagUtil.TAGUTIL_TYPE_CLASSIC)
//                return writeNewKey216_MifareClassic( intent,newPWD,PACK,isCheckSum);
//            else
//                return false;
//        }else
//            throw new Exception("new PWD must be 4 bytes and PACK must be 2 bytes");
//    }
//
//    private boolean writeNewKey216_MifareUltraLight(Intent intent, byte[] newPWD,byte[] PACK,boolean isCheckSum) throws Exception{
//        byte[] oldE6 = readOnePage(intent, (byte)0XE6, isCheckSum);
//        byte[] newE6 = new byte[4];
//        newE6[0]=PACK[0];
//        newE6[1]=PACK[1];
//        newE6[2]=oldE6[2];
//        newE6[3]=oldE6[3];
//        boolean result1 = writeTag(intent, (byte)0XE5, newPWD, isCheckSum);
//        boolean result2 = writeTag(intent, (byte)0XE6, newE6, isCheckSum);
//        return result1 && result2 ;
//    }
//
//    private boolean writeNewKey216_MifareClassic(Intent intent, byte[] newPWD,byte[] PACK,boolean isCheckSum) throws Exception{
//        throw new Exception("unimplemented");
//    }
//
//    private boolean writeNewKey216_NfcA(Intent intent, byte[] newPWD,byte[] PACK,
//                                        boolean isCheckSum) throws Exception {
//        byte[] oldE6 = readOnePage(intent, (byte)0XE6, isCheckSum);
//        byte[] newE6 = new byte[4];
//        newE6[0]=PACK[0];
//        newE6[1]=PACK[1];
//        newE6[2]=oldE6[2];
//        newE6[3]=oldE6[3];
//        boolean result1 = writeTag(intent, (byte)0XE5, newPWD, isCheckSum);
//        boolean result2 = writeTag(intent, (byte)0XE6, newE6, isCheckSum);
//        return result1 && result2 ;
//    }

    /**
     * 设置一个开始页面地址和访问方式， 该页面地址后的页面都需要认证后才可访问。访问方式有两种，0为读写访问，1 为写访问
     * @param intent
     * @param addr: 访问地址，
     * @param access: 如果设置为  0, 读写操作都需要授权. 如果设置为1 ,只有写操作需要授权。
     * @param isCheckSum: 是否增加校验位
     * @return
     * @throws Exception
     */
    public boolean setAccess(Intent intent,byte addr, int access,boolean isCheckSum) throws Exception
    {
        if(tagType==TagUtil.TAGUTIL_NfcA)
            return setAccess_NfcA( intent,addr, access,isCheckSum);
        else
            throw new Exception("unknow tag Type"+ tagType+". or SelectTag first.");
    }


    private boolean setAccess_NfcA(Intent intent, byte addr, int access, boolean isCheckSum ) throws Exception {
        if(addr< (byte)3 || addr > 0x30)
            throw new Exception("address must between 03h and 30h");
        else
        {
            int value = (int)addr<<24;
            access = access << 24;
            boolean result1 = writeTag(intent, (byte)0X2A, this.getBytesArray(value), isCheckSum);
            boolean result2 = writeTag(intent, (byte)0X2B, this.getBytesArray(access), isCheckSum);
            if(result1 && result2)
                return true;
            else
                return false;
        }
    }

    /**
     * 锁定加锁位
     * @param intent
     * @param isCheckSum 是否在命令后自动增加校验位。
     * @return
     * @throws Exception
     */
    public boolean lockLockingbits(Intent intent, boolean isCheckSum) throws Exception
    {
        if(tagType==TagUtil.TAGUTIL_NfcA)
            return lockLockingbits_NfcA( intent,isCheckSum);
        else
            throw new Exception("unknow tag Type"+ tagType+". or SelectTag first.");
    }
    private boolean lockLockingbits_NfcA(Intent intent,boolean isCheckSum)  throws Exception {
        byte[] contents1= new byte[4];
        contents1[0]=(byte)0;
        contents1[1]=(byte)0;
        contents1[2]=(byte)7;
        contents1[3]=(byte)0;

        byte[] contents2= new byte[4];
        contents2[0]=(byte)17;
        contents2[1]=(byte)15;
        contents2[2]=(byte)0;
        contents2[3]=(byte)0;

        if (writeTag(intent, (byte)2, contents1,isCheckSum) && writeTag(intent, (byte)40, contents2,isCheckSum))
            return true;
        else
            return false;
    }

    /**
     * 锁定一个页面范围
     * @param intent
     * @param addr1 要锁定的开始页面
     * @param addr2 要锁定的结束页面
     * @param isCheckSum 是否在命令后自动增加校验位。
     * @return
     * @throws Exception
     */
    public boolean lockPage(Intent intent,byte addr1 ,byte addr2, boolean isCheckSum) throws Exception
    {
        if(tagType==TagUtil.TAGUTIL_NfcA)
            return lockPage_NfcA( intent,addr1, addr2, isCheckSum);
        else
            throw new Exception("unknow tag Type"+ tagType+". or SelectTag first.");
    }

    private boolean lockPage_NfcA(Intent intent, byte startAddr1, byte endAddr, boolean isCheckSum)  throws Exception {
        boolean result = false;

        if(startAddr1>endAddr)
        {
            throw new Exception ("endAdddr must greater than or equal to startAddr");
        }

        if(startAddr1<3 || endAddr >47)
        {
            throw new Exception ("startAddr and endAdddr must between [3,47]");
        }

        if(endAddr>15)
        {
            if(lockPage_NfcA_Part1(intent,startAddr1,(byte)15, isCheckSum)  &&	lockPage_NfcA_Part2(intent,(byte)16,endAddr, isCheckSum))
                result = true;
        }
        else
        {
            if(lockPage_NfcA_Part1(intent,startAddr1,(byte)15, isCheckSum))
                result=true;
        }
        return result;
    }

    /**
     * 锁定为在第 40 页， 可以锁定的范围是 16 到 47 页面。
     * lock in page 40, lock address between 16 and 47
     * @param intent
     * @param startAddr
     * @param endAddr
     * @param isCheckSum 是否在命令后自动增加校验位。
     */
    private boolean lockPage_NfcA_Part2(Intent intent, byte startAddr, byte endAddr, boolean isCheckSum) throws Exception{
        byte[] contents = new byte[4];
        int value=0;
        int totalValue=0;
        for(int j=startAddr;j<=endAddr;j++)
        {
            if(j<=39)
            {
                if(j%4>0)
                    continue;
                int i=(j)/4;
                switch (i)
                {
                    case 4:
                        value=2^25;
                        break;
                    case 5:
                        value=2^26;
                        break;
                    case 6:
                        value=2^27;
                        break;
                    case 7:
                        value=2^29;
                        break;
                    case 8:
                        value=2^30;
                        break;
                    case 9:
                        value=2^31;
                        break;
                    default:
                        break;
                }
            }else
            {
                switch (j)
                {
                    case 41:
                        value=2^20;
                        break;
                    case 42:
                        value=2^21;
                        break;
                    case 43:
                        value=2^22;
                        break;
                    case 44:
                        value=2^23;
                        break;
                    case 40:
                    case 45:
                    case 46:
                    case 47:
                    default:

                }
            }
            totalValue+=value;
        }
        contents = getBytesArray(totalValue);
        return writeTag(intent, (byte)40, contents, isCheckSum);
    }

    private byte[] getBytesArray(int value) {
        byte[] contents = new byte[4];
        contents[0] = (byte)(value >>> 24);
        contents[1] = (byte)(value >>> 16);
        contents[2] = (byte)(value >>> 8);
        contents[3] = (byte)(value );
        return contents;
    }

    /**
     * 锁定位在第 2 页面， 可以锁定的地址范围是 3 到 15
     * lock in page 2, lock address between 3 and 15
     * @param startAddr
     * @param endAddr
     * @param isCheckSum 是否在命令后自动增加校验位。
     */
    private boolean lockPage_NfcA_Part1(Intent intent, byte startAddr, byte endAddr, boolean isCheckSum) throws Exception{
        byte[] contents = new byte[4];
        int value=0;
        int totalValue=0;
        for(int i=startAddr;i<=endAddr;i++)
        {
            switch (i)
            {
                case 3:
                    value=2^8;
                    break;
                case 4:
                    value=2^12;
                    break;
                case 5:
                    value=2^13;
                    break;
                case 6:
                    value=2^14;
                    break;
                case 7:
                    value=2^15;
                    break;
                case 8:
                    value=2^0;
                    break;
                case 9:
                    value=2^1;
                    break;
                case 10:
                    value=2^2;
                    break;
                case 11:
                    value=2^3;
                    break;
                case 12:
                    value=2^4;
                    break;
                case 13:
                    value=2^5;
                    break;
                case 14:
                    value=2^6;
                    break;
                case 15:
                    value=2^7;
                    break;
                default:
            }
            totalValue+=value;
        }
        contents[0] = (byte)(totalValue >>> 24);
        contents[1] = (byte)(totalValue >>> 16);
        contents[2] = (byte)(totalValue >>> 8);
        contents[3] = (byte)(totalValue );
        return writeTag(intent, (byte)2, contents, isCheckSum);
    }

    /**
     * 锁定所有页面
     * @param intent
     * @param isCheckSum 是否在命令后自动增加校验位。
     * @return
     * @throws Exception
     */
    public boolean lockPageAll(Intent intent, boolean isCheckSum) throws Exception
    {
        boolean result = false;
        switch(tagType)
        {
            case TagUtil.TAGUTIL_NfcA:
                result= lockPageAll_NfcA(intent, isCheckSum);
                break;
            case TagUtil.TAGUTIL_TYPE_ULTRALIGHT:
                result= lockPageAll_MifareUltraLight(intent);
                break;
            case TagUtil.TAGUTIL_TYPE_CLASSIC:
                result= lockPageAll_MifareClassic(intent);
                break;
            default:
                throw new Exception("unknow tag Type"+ tagType+". or SelectTag first.");
        }return result;
    }


    private boolean lockPageAll_NfcA(Intent intent, boolean isCheckSum) throws Exception
    {
        byte[] contents1= new byte[4];
        contents1[0]=(byte)0;
        contents1[1]=(byte)0;
        contents1[2]=(byte)255;
        contents1[3]=(byte)255;

        byte[] contents2= new byte[4];
        contents2[0]=(byte)255;
        contents2[1]=(byte)255;
        contents2[2]=(byte)0;
        contents2[3]=(byte)0;

        if (writeTag(intent, (byte)2, contents1, isCheckSum) && writeTag(intent, (byte)40, contents2, isCheckSum))
            return true;
        else
            return false;
    }

    private boolean lockPageAll_MifareUltraLight(Intent intent) throws Exception
    {
        throw new Exception("unimplemented");
    }

    private boolean lockPageAll_MifareClassic(Intent intent) throws Exception
    {
        throw new Exception("unimplemented");
    }

    /**
     * 获取标签类型，目前可支持的标签类型包括  NFCA 和  UltraLight
     * @return
     * @throws AuthenticationException
     */
    public int getTagType() throws AuthenticationException
    {
        return tagType;
    }

    private void accreditation(NfcA mfc,byte[] secretKeys,boolean isCheckSum) throws Exception {
        byte[] iv = ivDefault;

        byte[] command0 = new byte[2];// 发送认证指令的参数
        byte[] command0WithCheckSum = new byte[4];// 发送认证指令的参数(with check sum)

        byte[] command1 = null;// 发送认证后，卡片返回的密文1
        byte[] command1WithCheckSum = null;// 发送认证后，卡片返回的密文1

        byte[] command2 = null;// 密文1去掉数组中的第1个数据,取出有效数组

        byte[] command3 = null;// 密文1 解密后的数据
        byte[] command4 = null;// command2 加密
        byte[] command5 = null;// command3 循环左移得到的数据
        byte[] command6 = null;// 使用command5 和 command4 第二次加密后的数据RNDB
        byte[] command7 = null;//
        byte[] command8 = null;//
        byte[] command9 = null;//
        byte[] command10 = null;//
        byte[] command11 = null;//

        command0[0] = (byte) 0x1A; // 命令位
        command0[1] = (byte) 0x00; // 标志位
        if(isCheckSum)
        {
            byte[] checkSum = getCheckSum(command0);
            command0WithCheckSum[0]=command0[0];
            command0WithCheckSum[1]=command0[1];
            command0WithCheckSum[2]=checkSum[0];
            command0WithCheckSum[3]=checkSum[1];
            command1WithCheckSum = mfc.transceive(command0WithCheckSum);// 11 bytes
            if(command1WithCheckSum.length != 11)
            {
                String str="";
                for (int i = 0 ; i<command1WithCheckSum.length;i++)
                {
                    str = str+" byte"+i+"="+command1WithCheckSum[i]+"  ";
                }
                throw new Exception("length of response is not 11 bytes. the response bytes is: "+str);
            }
            command1= new byte[9];
            System.arraycopy(command1WithCheckSum, 0, command1, 0, 9);
            Log.i("authen","first send end");
            Log.i("authen","first send response" +bytesToHexString(command1));
        }
        else
            command1 = mfc.transceive(command0);

        command2 = new byte[8];
        if(command1.length != 9)
        {
            String str="";
            for (int i = 0 ; i<command1.length;i++)
            {
                str = str+" byte"+i+"="+command1[i]+"  ";
            }
            throw new Exception("length of response is not 9 bytes. the response bytes is: "+str);
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

        if(isCheckSum)
        {
            byte[] command8WithCheckSum= new byte[19];
            byte[] checkSum = getCheckSum(command8);
            for(int i=0;i<17;i++)
            {
                command8WithCheckSum[i]=command8[i];
            }
            command8WithCheckSum[17]=checkSum[0];
            command8WithCheckSum[18]=checkSum[1];
            Log.i("authen","sencond send:"+bytesToHexString(command8WithCheckSum));
            command9 = mfc.transceive(command8WithCheckSum);//
            Log.i("authen","sencond send end");
        }
        else
            command9 = mfc.transceive(command8);
        command10 = new byte[8];
        System.arraycopy(command9, 1, command10, 0, 8);
        iv = command6;
        command11 = ThreeDES.decode(command10, iv, secretKeys);
    }

    /**
     * 16进制字符串转化为字节数组
     */
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
        if(i == -1){
            throw new NumberFormatException();
        }else{
            return (byte)i;
        }
    }

    public static String StringtoHexString(String str)
    {
        String hexString = "0123456789ABCDEF";
        byte bytes[] = str.getBytes();
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for(int i = 0; i < bytes.length; i++)
        {
            sb.append(hexString.charAt((bytes[i] & 240) >> 4));
            sb.append(hexString.charAt((bytes[i] & 15) >> 0));
        }

        return sb.toString();
    }

    public static String hexStringToString(String bytes)
    {
        String hexString = "0123456789ABCDEF";
        ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length() / 2);
        for(int i = 0; i < bytes.length(); i += 2)
            baos.write(hexString.indexOf(bytes.charAt(i)) << 4 | hexString.indexOf(bytes.charAt(i + 1)));

        return new String(baos.toByteArray());
    }

    public static byte[] StringtoBytes(String str)
    {
        String hexstr = StringtoHexString(str);
        byte byte1[] = hexStringToBytes(hexstr);
        return byte1;
    }

    private byte[] appendByteArray(byte byteArray[])
    {
        int length = byteArray.length;
        int m = length % 4;
        byte newByteArray[];
        if(m == 0)
            newByteArray = new byte[length];
        else
            newByteArray = new byte[length + (4 - m)];
        System.arraycopy(byteArray, 0, newByteArray, 0, length);
        return newByteArray;
    }

    /**
     * 字节数组转化为16进制字符串
     */
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

    private static void getTagUID_NfcA(Tag tag,boolean isCheckSum) throws Exception
    {
//		byte[] datau = tag.getId();
//		uid=bytesToHexString(datau);

        nfcA = NfcA.get(tag);
        try {
            String metaInfo = "";
            nfcA.connect();
            byte[] datas = new byte[2];
            byte[] datasWithCheckSum = new byte[4];
            datas[0] = 0x30;
            datas[1] = 0x00;
            byte[] datar;
            if(isCheckSum)
            {
                byte[] checkSum = getCheckSum(datas);
                datasWithCheckSum[0]=datas[0];
                datasWithCheckSum[1]=datas[1];
                datasWithCheckSum[2]=checkSum[0];
                datasWithCheckSum[3]=checkSum[1];
                datar = nfcA.transceive(datasWithCheckSum);// 每次读出来的数据为4page的数据
            }
            else
                datar = nfcA.transceive(datas);// 每次读出来的数据为4page的数据
            byte[] datau = new byte[7];//uid号
            System.arraycopy(datar, 0, datau, 0, 3);// 去4page中的第1page数据
            System.arraycopy(datar, 4, datau, 3, 4);// 去4page中的第1page数据
            uid=bytesToHexString(datau);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            throw e;
        }
//		finally
//		{
//			try {
//				mfc.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
    }

    private static void getFinalPage_NfcA(Tag tag,boolean isCheckSum) throws Exception
    {
        try {
            //mfc.connect();
            byte[] datas = new byte[2];
            byte[] datasWithChcekSum = new byte[4];

            datas[0] = 0x30;
            datas[1] = (byte)0xFF;
            byte[] checkSum;

            if(isCheckSum)
            {
                checkSum = getCheckSum(datas);
                datasWithChcekSum[0]=datas[0];
                datasWithChcekSum[1]=datas[1];
                datasWithChcekSum[2]=checkSum[0];
                datasWithChcekSum[3]=checkSum[1];
            }

            if(nfcA==null)
            {
                nfcA = NfcA.get(tag);
                nfcA.connect();
            }

            byte[] datar;

            if(isCheckSum)
                datar = nfcA.transceive(datasWithChcekSum);
            else
                datar = nfcA.transceive(datas);

            byte[] datau = new byte[4];//uid号
            System.arraycopy(datar, 0, datau, 0, 3);// 4page中的第1page数据
            finalPage=bytesToHexString(datau);
        }
        catch(Exception e)
        {
            throw e;
        }
//		finally
//		{
//			try {
//				mfc.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
    }

    public static String getUid() {
        return uid;
    }


    /**
     * 访问时需要认证的页面地址范围
     * @param intent
     * @return 返回一个开始地址。访问该地址后的所有页面，都需要认证。 如果返回值大于等于 48 ，则 访问（读或写）所有页面都不需要认证。如果返回值小于48，说明访问（读或写）返回值地址到  48  之间的页面时，需要认证。
     * @throws Exception
     */
    public int getAuthenticationAddr(Intent intent,boolean isCheckSum) throws Exception
    {
        byte[] result = readOnePage(intent, (byte)PAGE_ADDR_AUTH0,isCheckSum);
        int r = result[0];
        return r;
    }

    /**
     * 如果访问时,有地址需要认证。该方法可以获得认证的种类
     * @param intent
     * @param isCheckSum 是否在命令后自动增加校验位。
     * @return  如果返回值=0 ，则读写都需要认证。如果返回值=1，只有写需要认证
     * @throws Exception
     */
    public int getAuthenticationType(Intent intent,boolean isCheckSum) throws Exception
    {
        byte[] result = readOnePage(intent, (byte)PAGE_ADDR_AUTH1,isCheckSum);
        int r = result[0];
        return r;
    }

    /**
     * 关闭连接
     */
    public void close()
    {
        try {
            if(nfcA!=null)
                nfcA.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] getCheckSum(byte[] byteAyyay) throws Exception {
        AbstractChecksum checksum=null;
        try {
            String checksumArg="crc:16,1021,c6c6,true,true,0";
            checksum = JacksumAPI.getChecksumInstance(checksumArg,false);
        } catch (NoSuchAlgorithmException nsae) {
            throw new ExitException(nsae.getMessage()+"\nUse -a <code> to specify a valid one.\nFor help and a list of all supported algorithms use -h.\nExit.", ExitStatus.PARAMETER);
        }
        checksum.setEncoding(AbstractChecksum.HEX);
        //byte[] byteAyyay = hexStringToBytes(string);
        checksum.update(byteAyyay);
        String hexValue = checksum.getHexValue();
        //String resultStr =checksum.toString();//d97c 02a8
        byte[] result = reverse(hexStringToBytes(hexValue));
        return result;
    }

    /**
     * 使用命令
     * java -jar aofei_nfc.jar
     * 获取版本号
     * @param args
     */
    public static void main(String[] args)
    {
        System.out.println("2.2.0");
    }

    private static byte[] reverse(byte[] bytes)
    {
        byte[] result= new byte[bytes.length];

        for(int i=0;i<bytes.length;i++)
        {
            result[i]=bytes[bytes.length-i-1];
        }
        return result;
    }

    /**
     * 	获取计数器的值
     * @return
     * @throws Exception
     */
    public int getCount(Intent intent,boolean isCheckSum) throws Exception
    {
        if(tagType==TagUtil.TAGUTIL_NfcA)
            return getCount_NfcA( intent,isCheckSum);
        else
            throw new Exception("unsupport tag type:"+ tagType);
    }

    private int getCount_NfcA(Intent intent,boolean isCheckSum) throws Exception
    {
        byte[] data0 = new byte[2];
        byte[] dataWithCheckSum = new byte[4];
        byte[] result = new byte[3];
        data0[0] = (byte)0x39;
        data0[1] = (byte)0x00;// FJ 39 00
        byte[] data1;
        if(isCheckSum)
        {
            byte[] checkSum = getCheckSum(data0);
            dataWithCheckSum[0]=data0[0];
            dataWithCheckSum[1]=data0[1];
            dataWithCheckSum[2]=checkSum[0];
            dataWithCheckSum[3]=checkSum[1];
            result = nfcA.transceive(dataWithCheckSum);// 每次读出来的数据为4page的数据
        }
        else
            result = nfcA.transceive(data0);// 每次读出来的数据为4page的数据

        int count;
        if(result.length==1)
        {
            count = (int)result[0];
        }else if(result.length==2)
        {
            count = 256*(int)result[1]+(int)result[0];
        }else
            count = 256*256*((int)result[2])+256*(int)result[1]+(int)result[0];
        return count;
    }


    /**
     * 使计数器功能生效或失效
     * @param intent
     * @param isCount true 生效， false 失效
     * @param isCheckSum 是否在命令后自动增加校验位。
     * @throws AuthenticationException
     * @throws Exception
     */
    public void enableCounter(Intent intent,boolean isCount,boolean isCheckSum) throws AuthenticationException, Exception
    {
        if(isCount)
        {
            byte[] bytes = readOnePage(intent, (byte)0xE4, isCheckSum);//E4 for ntag 216
            byte accessByte = bytes[0];
            byte newAccessByte = (byte) (accessByte | (byte)16);
            bytes[0] = newAccessByte;
            writeTag(intent, (byte)0xE4, bytes, isCheckSum);
            //writeTag(intent, (byte)228, bytes, isCheckSum);
        }
        else
        {
            byte[] bytes = readOnePage(intent, (byte)0xE4, isCheckSum);//E4 for ntag 216
            byte accessByte = bytes[0];
            byte newAccessByte = (byte) (accessByte & (byte)(255-16));
            bytes[0] = newAccessByte;
            this.writeTag(intent, (byte)0xE4, bytes, isCheckSum);
        }

    }


    /**
     * 	取芯片的CID值，读页地址FFh，得到的16字节数据的前四个字节做为CID值返回。
     * @param intent
     * @param isCheckSum
     * @return
     * @throws Exception
     * @throws AuthenticationException
     */
    public byte[] getCID(Intent intent, boolean isCheckSum) throws AuthenticationException, Exception
    {
        byte page = (byte)0xFF;
        byte[] result = this.readOnePage(intent, page, isCheckSum);
        return result;
    }



    /**
     * 三重认证第一步，向芯片发送认证指令，取得8个字节的RNDB’
     * @param intent
     * @return 8 bytes,response from chip
     * @throws Exception
     */
    public byte[] authStep1(Intent intent, boolean isCheckSum) throws Exception
    {
        byte[] command0 = new byte[2];// 发送认证指令的参数
        byte[] command0WithCheckSum = new byte[4];// 发送认证指令的参数(with check sum)

        byte[] command1 = null;// 发送认证后，卡片返回的密文
        byte[] command1WithCheckSum = null;// 发送认证后，卡片返回的密文

        byte[] command2 = null;// 由command1去掉数组中的第1个数据,取出有效数组

        command0[0] = (byte) 0x1A; // 命令位
        command0[1] = (byte) 0x00; // 标志位
        if(isCheckSum)
        {
            byte[] checkSum = getCheckSum(command0);
            command0WithCheckSum[0]=command0[0];
            command0WithCheckSum[1]=command0[1];
            command0WithCheckSum[2]=checkSum[0];
            command0WithCheckSum[3]=checkSum[1];
            command1WithCheckSum = nfcA.transceive(command0WithCheckSum);// 11 bytes
            if(command1WithCheckSum.length != 11)
            {
                String str="";
                for (int i = 0 ; i<command1WithCheckSum.length;i++)
                {
                    str = str+" byte"+i+"="+command1WithCheckSum[i]+"  ";
                }
                throw new Exception("length of response is not 11 bytes. the response bytes is: "+str);
            }
            command1= new byte[9];
            System.arraycopy(command1WithCheckSum, 0, command1, 0, 9);
            Log.i("authen","first send end");
            Log.i("authen","first send response" +bytesToHexString(command1));
        }
        else
            command1 = nfcA.transceive(command0);

        command2 = new byte[8];
        if(command1.length != 9)
        {
            String str="";
            for (int i = 0 ; i<command1.length;i++)
            {
                str = str+" byte"+i+"="+command1[i]+"  ";
            }
            throw new Exception("length of response is not 9 bytes. the response bytes is: "+str);
        }
        System.arraycopy(command1, 1, command2, 0, 8);
        return command2;
    }

    /**
     * 三重认证第二步，向芯片发送由服务器返回的RNDAB，得到芯片返回的RNDA”
     * @param intent
     * @param RANAB 16 bytes
     * @param isCheckSum
     * @return 8 bytes,response from chip
     * @throws Exception
     */
    public byte[] authStep2(Intent intent,byte[] RANAB, boolean isCheckSum) throws Exception
    {

        byte[] command8 = new byte[17];
        byte[] command9 = null;//
        byte[] command10 = null;//

        command8[0] = (byte) 0xAF;
        System.arraycopy(RANAB, 0, command8, 1, 16);

        if(isCheckSum)
        {
            byte[] command8WithCheckSum= new byte[19];
            byte[] checkSum = getCheckSum(command8);
            for(int i=0;i<17;i++)
            {
                command8WithCheckSum[i]=command8[i];
            }
            command8WithCheckSum[17]=checkSum[0];
            command8WithCheckSum[18]=checkSum[1];
            Log.i("authen","sencond send:"+bytesToHexString(command8WithCheckSum));
            command9 = nfcA.transceive(command8WithCheckSum);//
            Log.i("authen","sencond send end");
        }
        else
            command9 = nfcA.transceive(command8);
        command10 = new byte[8];
        if(command9.length==9)
        {
            System.arraycopy(command9, 1, command10, 0, 8);
            return command10;
        }
        else
        {
            throw new Exception("ERROR RNDA”:"+this.bytesToHexString(command9));
        }

    }
}
