package com.rover12421.ApkUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.zip.ZipError;

import static com.rover12421.ApkUtil.ZipConstants.*;

public class ApkPseudoEncryption {
	
    private FileChannel ch; // channel to the zipfile
    private FileChannel fc;

    /**
     * 修复zip伪加密状态的Entry
     * @param inZip
     * @param storeZip
     * @throws IOException
     */
    public void FixEncryptedEntry(File inZip, File fixZip) throws IOException {
    	changEntry(inZip, fixZip, true);
	}
    
    /**
     * 修复zip伪加密状态的Entry
     * @param inZip
     * @param storeZip
     * @throws IOException
     */
    public void FixEncryptedEntry(String inZip, String fixZip) throws IOException {
    	FixEncryptedEntry(new File(inZip), new File(fixZip));
    }
    
    /**
     * 修改zip的Entry为伪加密状态
     * @param inZip
     * @param storeZip
     * @throws IOException
     */
    public void ChangToEncryptedEntry(File inZip, File storeZip) throws IOException {
    	changEntry(inZip, storeZip, false);
    }
    
    /**
     * 修改zip的Entry为伪加密状态
     * @param inZip
     * @param storeZip
     * @throws IOException
     */
    public void ChangToEncryptedEntry(String inZip, String storeZip) throws IOException {
    	ChangToEncryptedEntry(new File(inZip), new File(storeZip));
    }
    
    /**
     * 更改zip的Entry为伪加密状态
     * @param inZip
     * @param storeZip
     * @param fix	ture:修复伪加密 false:更改到伪加密
     * @throws IOException
     */
    private void changEntry(File inZip, File storeZip, boolean fix) throws IOException {
    	FileInputStream fis = new FileInputStream(inZip);
		FileOutputStream fos = new FileOutputStream(storeZip);
		
		byte[] buf = new byte[10240];
    	int len;
    	while ((len = fis.read(buf)) != -1) {
			fos.write(buf, 0, len);
		}
    	
    	ch = fis.getChannel();
    	fc = fos.getChannel();
    	
    	changEntry(fix);
    	
    	ch.close();
    	fc.close();
		
		fis.close();
		fos.close();
    }
    
	// Reads zip file central directory. Returns the file position of first
    // CEN header, otherwise returns -1 if an error occured. If zip->msg != NULL
    // then the error was a zip format error and zip->msg has the error text.
    // Always pass in -1 for knownTotal; it's used for a recursive call.
    private void changEntry(boolean fix) throws IOException {
    	END end = findEND();
    	
        if (end.cenlen > end.endpos)
            zerror("invalid END header (bad central directory size)");
        long cenpos = end.endpos - end.cenlen;     // position of CEN table

        // Get position of first local file (LOC) header, taking into
        // account that there may be a stub prefixed to the zip file.
        long locpos = cenpos - end.cenoff;
        if (locpos < 0)
            zerror("invalid END header (bad central directory offset)");

        // read in the CEN and END
        byte[] cen = new byte[(int)(end.cenlen + ENDHDR)];
        if (readFullyAt(cen, 0, cen.length, cenpos) != end.cenlen + ENDHDR) {
            zerror("read CEN tables failed");
        }

        int pos = 0;
        int limit = cen.length - ENDHDR;
        while (pos < limit) {
            if (CENSIG(cen, pos) != CENSIG)
                zerror("invalid CEN header (bad signature)");
            int method = CENHOW(cen, pos);
            int nlen   = CENNAM(cen, pos);
            int elen   = CENEXT(cen, pos);
            int clen   = CENCOM(cen, pos);
            
            if (fix) {
            	if ((CENFLG(cen, pos) & 1) != 0) {
                	byte[] name = Arrays.copyOfRange(cen, pos + CENHDR, pos + CENHDR + nlen);
                	System.out.println("Found the encrypted entry : " + new String(name) + ", fix...");
                	//b[n] & 0xff) | ((b[n + 1] & 0xff) << 8
                	cen[pos+8] &= 0xFE;
//                	cen[pos+8] ^= CENFLG(cen, pos) % 2;
//                	cen[pos+8] ^= cen[pos+8] % 2;
//                    zerror("invalid CEN header (encrypted entry)");
                }
			} else {
				if ((CENFLG(cen, pos) & 1) == 0) {
                	byte[] name = Arrays.copyOfRange(cen, pos + CENHDR, pos + CENHDR + nlen);
                	System.out.println("Chang the entry : " + new String(name) + ", Encrypted...");
                	//b[n] & 0xff) | ((b[n + 1] & 0xff) << 8
                	cen[pos+8] |= 0x1;
//                    zerror("invalid CEN header (encrypted entry)");
                }
			}
            
            
            if (method != METHOD_STORED && method != METHOD_DEFLATED)
                zerror("invalid CEN header (unsupported compression method: " + method + ")");
            if (pos + CENHDR + nlen > limit)
                zerror("invalid CEN header (bad header size)");
            
            // skip ext and comment
            pos += (CENHDR + nlen + elen + clen);
        }
        
        writeFullyAt(cen, 0, cen.length, cenpos);
        
        if (pos + ENDHDR != cen.length) {
            zerror("invalid CEN header (bad header size)");
        }
    }
    
    /**
     * 检查是否是伪加密的zip文件
     * 实际无法判断是真加密还是伪加密,仅用于APK的伪加密检测
     * @return
     * @throws IOException
     */
    public boolean isPseudoEncryption(File zipFile) throws IOException {
    	FileInputStream fis = new FileInputStream(zipFile);
    	ch = fis.getChannel();
    	
    	boolean bRet = isPseudoEncryption();
    	
    	ch.close();
    	fc.close();		
		fis.close();
		
		return bRet;
    }
    
    /**
     * 检查是否是伪加密的zip文件
     * 实际无法判断是真加密还是伪加密,仅用于APK的伪加密检测
     * @return
     * @throws IOException
     */
    public boolean isPseudoEncryption(String zipFile) throws IOException {
    	return isPseudoEncryption(new File(zipFile));
    }
    
    
    /**
     * 检查是否是伪加密的zip文件
     * 实际无法判断是真加密还是伪加密,仅用于APK的伪加密检测
     * @return
     * @throws IOException
     */
    private boolean isPseudoEncryption() throws IOException {
    	END end = findEND();
    	
        if (end.cenlen > end.endpos)
            zerror("invalid END header (bad central directory size)");
        long cenpos = end.endpos - end.cenlen;     // position of CEN table

        // Get position of first local file (LOC) header, taking into
        // account that there may be a stub prefixed to the zip file.
        long locpos = cenpos - end.cenoff;
        if (locpos < 0)
            zerror("invalid END header (bad central directory offset)");

        // read in the CEN and END
        byte[] cen = new byte[(int)(end.cenlen + ENDHDR)];
        if (readFullyAt(cen, 0, cen.length, cenpos) != end.cenlen + ENDHDR) {
            zerror("read CEN tables failed");
        }

        int pos = 0;
        int limit = cen.length - ENDHDR;
        while (pos < limit) {
            if (CENSIG(cen, pos) != CENSIG)
                zerror("invalid CEN header (bad signature)");
            int method = CENHOW(cen, pos);
            int nlen   = CENNAM(cen, pos);
            int elen   = CENEXT(cen, pos);
            int clen   = CENCOM(cen, pos);
            
            if ((CENFLG(cen, pos) & 1) != 0) {
            	return true;
            }
            
            if (method != METHOD_STORED && method != METHOD_DEFLATED)
                zerror("invalid CEN header (unsupported compression method: " + method + ")");
            if (pos + CENHDR + nlen > limit)
                zerror("invalid CEN header (bad header size)");
            
            // skip ext and comment
            pos += (CENHDR + nlen + elen + clen);
        }
        
        return false;
    }
    
    // Reads len bytes of data from the specified offset into buf.
    // Returns the total number of bytes read.
    // Each/every byte read from here (except the cen, which is mapped).
    final long readFullyAt(byte[] buf, int off, long len, long pos)
        throws IOException
    {
        ByteBuffer bb = ByteBuffer.wrap(buf);
        bb.position(off);
        bb.limit((int)(off + len));
        return readFullyAt(bb, pos);
    }

    private final long readFullyAt(ByteBuffer bb, long pos)
        throws IOException
    {
        synchronized(ch) {
            return ch.position(pos).read(bb);
        }
    }
    
    final long writeFullyAt(byte[] buf, int off, long len, long pos)
            throws IOException
        {
            ByteBuffer bb = ByteBuffer.wrap(buf);
            bb.position(off);
            bb.limit((int)(off + len));
            return writeFullyAt(bb, pos);
        }
    
    private final long writeFullyAt(ByteBuffer bb, long pos)
            throws IOException
    {
        synchronized(fc) {
            return fc.position(pos).write(bb);
        }
    }
    
    // Searches for end of central directory (END) header. The contents of
    // the END header will be read and placed in endbuf. Returns the file
    // position of the END header, otherwise returns -1 if the END header
    // was not found or an error occurred.
    private END findEND() throws IOException
    {
        byte[] buf = new byte[READBLOCKSZ];
        long ziplen = ch.size();
        long minHDR = (ziplen - END_MAXLEN) > 0 ? ziplen - END_MAXLEN : 0;
        long minPos = minHDR - (buf.length - ENDHDR);

        for (long pos = ziplen - buf.length; pos >= minPos; pos -= (buf.length - ENDHDR))
        {
            int off = 0;
            if (pos < 0) {
                // Pretend there are some NUL bytes before start of file
                off = (int)-pos;
                Arrays.fill(buf, 0, off, (byte)0);
            }
            int len = buf.length - off;
            if (readFullyAt(buf, off, len, pos + off) != len)
                zerror("zip END header not found");

            // Now scan the block backwards for END header signature
            for (int i = buf.length - ENDHDR; i >= 0; i--) {
                if (buf[i+0] == (byte)'P'    &&
                    buf[i+1] == (byte)'K'    &&
                    buf[i+2] == (byte)'\005' &&
                    buf[i+3] == (byte)'\006' &&
                    (pos + i + ENDHDR + ENDCOM(buf, i) == ziplen)) {
                    // Found END header
                    buf = Arrays.copyOfRange(buf, i, i + ENDHDR);
                    END end = new END();
                    end.endsub = ENDSUB(buf);
                    end.centot = ENDTOT(buf);
                    end.cenlen = ENDSIZ(buf);
                    end.cenoff = ENDOFF(buf);
                    end.comlen = ENDCOM(buf);
                    end.endpos = pos + i;
                    if (end.cenlen == ZIP64_MINVAL ||
                        end.cenoff == ZIP64_MINVAL ||
                        end.centot == ZIP64_MINVAL32)
                    {
                        // need to find the zip64 end;
                        byte[] loc64 = new byte[ZIP64_LOCHDR];
                        if (readFullyAt(loc64, 0, loc64.length, end.endpos - ZIP64_LOCHDR)
                            != loc64.length) {
                            return end;
                        }
                        long end64pos = ZIP64_LOCOFF(loc64);
                        byte[] end64buf = new byte[ZIP64_ENDHDR];
                        if (readFullyAt(end64buf, 0, end64buf.length, end64pos)
                            != end64buf.length) {
                            return end;
                        }
                        // end64 found, re-calcualte everything.
                        end.cenlen = ZIP64_ENDSIZ(end64buf);
                        end.cenoff = ZIP64_ENDOFF(end64buf);
                        end.centot = (int)ZIP64_ENDTOT(end64buf); // assume total < 2g
                        end.endpos = end64pos;
                    }
                    return end;
                }
            }
        }
        zerror("zip END header not found");
        return null; //make compiler happy
    }
    
    static void zerror(String msg) {
        throw new ZipError(msg);
    }
    
 // End of central directory record
    static class END {
        int  disknum;
        int  sdisknum;
        int  endsub;     // endsub
        int  centot;     // 4 bytes
        long cenlen;     // 4 bytes
        long cenoff;     // 4 bytes
        int  comlen;     // comment length
        byte[] comment;

        /* members of Zip64 end of central directory locator */
        int diskNum;
        long endpos;
        int disktot;
        
        @Override
        public String toString() {
        	return "disknum : " + disknum + "\n" +
        			"sdisknum : " + sdisknum + "\n" +
        			"endsub : " + endsub + "\n" +
        			"centot : " + centot + "\n" +
        			"cenlen : " + cenlen + "\n" +
        			"cenoff : " + cenoff + "\n" +
        			"comlen : " + comlen + "\n" +
        			"diskNum : " + diskNum + "\n" +
        			"endpos : " + endpos + "\n" +
        			"disktot : " + disktot;
        }
    }
}
