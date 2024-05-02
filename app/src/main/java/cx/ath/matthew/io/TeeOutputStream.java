/*
 * Java Tee Stream Library
 *
 * Copyright (c) Matthew Johnson 2005
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * 
 * To Contact the author, please email src@matthew.ath.cx
 *
 */

package cx.ath.matthew.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * Class to copy a stream to another stream or file as it is being sent through a stream pipe
 * E.g.
 * <pre>
 *    PrintWriter r = new PrintWriter(new TeeOutputStream(new FileOutputStream("file"), new File("otherfile")));
 * </pre>
 */
public class TeeOutputStream extends FilterOutputStream
{
   private File f;
   private OutputStream out;
   private OutputStream fos;
   /**
    * Create a new TeeOutputStream on the given OutputStream
    * and copy the stream to the other OuputStream.
    * @param os Writes to this OutputStream
    * @param tos Write to this OutputStream
    */
   public TeeOutputStream(OutputStream os, OutputStream tos) throws IOException
   {
      super(os);
      this.out = os;
      this.fos = tos;
   }
   /**
    * Create a new TeeOutputStream on the given OutputStream
    * and copy the stream to the given File.
    * @param os Writes to this OutputStream
    * @param f Write to this File
    * @param append Append to file not overwrite
    */
   public TeeOutputStream(OutputStream os, File f, boolean append) throws IOException
   {
      super(os);
      this.out = os;
      this.fos = new FileOutputStream(f, append);
   }
   /**
    * Create a new TeeOutputStream on the given OutputStream
    * and copy the stream to the given File.
    * @param os Writes to this OutputStream
    * @param f Write to this File
    */
   public TeeOutputStream(OutputStream os, File f) throws IOException
   {
      super(os);
      this.out = os;
      this.fos = new FileOutputStream(f);
   }
   /**
    * Create a new TeeOutputStream on the given OutputStream
    * and copy the stream to the given File.
    * @param os Writes to this OutputStream
    * @param f Write to this File
    * @param append Append to file not overwrite
    */
   public TeeOutputStream(OutputStream os, String f, boolean append) throws IOException
   {
      this(os, new File(f), append);
   }
   /**
    * Create a new TeeOutputStream on the given OutputStream
    * and copy the stream to the given File.
    * @param os Writes to this OutputStream
    * @param f Write to this File
    */
   public TeeOutputStream(OutputStream os, String f) throws IOException
   {
      this(os, new File(f));
   }
   public void close() throws IOException
   {
      out.close();
      fos.close();
   }
   public void flush() throws IOException
   {
      fos.flush();
      out.flush();
   }
   public void write(int b) throws IOException
   {
      fos.write(b);
      out.write(b);
   }
   public void write(byte[] b) throws IOException
   {
      fos.write(b);
      out.write(b);
   }
   public void write(byte[] b, int off, int len) throws IOException
   {  
      fos.write(b, off, len);
      out.write(b, off, len);
   }

   public void finalize()
   {
      try {
         close();
      } catch (Exception e) {}
   }
}



