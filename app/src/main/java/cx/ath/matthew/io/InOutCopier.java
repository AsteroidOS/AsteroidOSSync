/*
 * Java Exec Pipe Library
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Copies from an input stream to an output stream using a Thread.
 * example:
 *
 * <pre>
 * InputStream a = getInputStream();
 * OutputStream b = getOutputStream();
 * InOutCopier copier = new InOutCopier(a, b);
 * copier.start();
 * &lt;do stuff that writes to the inputstream&gt;
 * </pre>
 */
public class InOutCopier extends Thread
{
   private static final int BUFSIZE=1024;
   private static final int POLLTIME=100;
   private BufferedInputStream is;
   private OutputStream os;
   private boolean enable;
   /**
    * Create a copier from an inputstream to an outputstream
    * @param is The stream to copy from
    * @param os the stream to copy to
    */
   public InOutCopier(InputStream is, OutputStream os) throws IOException
   {
      this.is = new BufferedInputStream(is);
      this.os = os;
      this.enable = true;
   }
   /**
    * Force close the stream without waiting for EOF on the source
    */
   public void close()
   {
      enable = false;
      interrupt();
   }
   /**
    * Flush the outputstream
    */
   public void flush() throws IOException
   {
      os.flush();
   }
   /** Start the thread and wait to make sure its really started */
   public synchronized void start()
   {
      super.start();
      try {
         wait();
      } catch (InterruptedException Ie) {}
   }
   /**
    * Copies from the inputstream to the outputstream
    * until EOF on the inputstream or explicitly closed
    * @see #close()
    */
   public void run() 
   {
      byte[] buf = new byte[BUFSIZE];
      synchronized (this) {
         notifyAll();
      }
      while (enable)
         try {
            int n = is.read(buf);
            if (0 > n)
               break;
            if (0 < n) {
               os.write(buf, 0, (n> BUFSIZE? BUFSIZE:n));
               os.flush();
            }
         } catch (IOException IOe) { 
            break;
         }
      try { os.close(); } catch (IOException IOe) {}
   }
}

