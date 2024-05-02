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

import java.io.FilterOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Class to pipe an OutputStream through a command using stdin/stdout.
 * E.g.
 * <pre>
 *    Writer w = new OutputStreamWriter(new ExecOutputStream(new FileOutputStream("file"), "command"));
 * </pre>
 */
public class ExecOutputStream extends FilterOutputStream
{
   private Process proc;
   private InputStream stdout;
   private OutputStream stdin;
   private InOutCopier copy;

   /**
    * Create a new ExecOutputStream on the given OutputStream
    * using the process to filter the stream.
    * @param os Writes to this OutputStream
    * @param p Filters data through stdin/out on this Process
    */
   public ExecOutputStream(OutputStream os, Process p) throws IOException
   {
      super(os);
      proc = p;
      stdin = p.getOutputStream();
      stdout = p.getInputStream();
      copy = new InOutCopier(stdout, out);
      copy.start();
   }
   /**
    * Create a new ExecOutputStream on the given OutputStream
    * using the process to filter the stream.
    * @param os Writes to this OutputStream
    * @param cmd Creates a Process from this string to filter data through stdin/out 
    */
   public ExecOutputStream(OutputStream os, String cmd) throws IOException
   { this(os, Runtime.getRuntime().exec(cmd)); }
   /**
    * Create a new ExecOutputStream on the given OutputStream
    * using the process to filter the stream.
    * @param os Writes to this OutputStream
    * @param cmd Creates a Process from this string array (command, arg, ...) to filter data through stdin/out 
    */
   public ExecOutputStream(OutputStream os, String[] cmd) throws IOException
   { this(os, Runtime.getRuntime().exec(cmd)); }
   /**
    * Create a new ExecOutputStream on the given OutputStream
    * using the process to filter the stream.
    * @param os Writes to this OutputStream
    * @param cmd Creates a Process from this string to filter data through stdin/out 
    * @param env Setup the environment for the command
    */
   public ExecOutputStream(OutputStream os, String cmd, String[] env) throws IOException
   { this(os, Runtime.getRuntime().exec(cmd, env)); }
   /**
    * Create a new ExecOutputStream on the given OutputStream
    * using the process to filter the stream.
    * @param os Writes to this OutputStream
    * @param cmd Creates a Process from this string array (command, arg, ...) to filter data through stdin/out 
    * @param env Setup the environment for the command
    */
   public ExecOutputStream(OutputStream os, String[] cmd, String[] env) throws IOException
   { this(os, Runtime.getRuntime().exec(cmd, env)); }

   public void close() throws IOException
   {
      stdin.close();
      try {
         proc.waitFor();
      } catch (InterruptedException Ie)  {}
      //copy.close();
      try {
         copy.join();
      } catch (InterruptedException Ie)  {}
      stdout.close();
      out.close();
   }
   public void flush() throws IOException
   {
      stdin.flush();
      copy.flush();
      out.flush();
   }
   public void write(byte[] b) throws IOException
   {
      stdin.write(b);
   }
   public void write(byte[] b, int off, int len) throws IOException
   {
      stdin.write(b, off, len);
   }
   public void write(int b) throws IOException
   {
      stdin.write(b);
   }
   public void finalize()
   {
      try {
         close();
      } catch (Exception e) {}
   }
}

