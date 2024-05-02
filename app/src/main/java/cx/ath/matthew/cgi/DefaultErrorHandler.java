/*
 * Java CGI Library
 *
 * Copyright (c) Matthew Johnson 2004
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

package cx.ath.matthew.cgi;

/**
 * Interface to handle exceptions in the CGI.
 */
public class DefaultErrorHandler implements CGIErrorHandler
{
   /**
    * This is called if an exception is not caught in the CGI.
    * It should handle printing the error message nicely to the user,
    * and then exit gracefully.
    */
   public void print(boolean headers_sent, Exception e)
   {
      if (!headers_sent) {
         System.out.println("Content-type: text/html");
         System.out.println("");
         System.out.println("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">");
         System.out.println("<HTML><HEAD>");
         System.out.println("<TITLE>Exception in CGI</TITLE>");
         System.out.println("</HEAD><BODY>");
      }
      System.out.println("<HR>");
      System.out.println("<H1>"+e.getClass().toString()+"</H1>");
      System.out.println("<P>");
      System.out.println("Exception Message: "+e.getMessage());
      System.out.println("</P>");
      System.out.println("<P>");
      System.out.println("Stack Trace:");
      System.out.println("</P>");
      System.out.println("<PRE>");
      e.printStackTrace(System.out);
      System.out.println("</PRE>");
      System.out.println("<HR>");
      if (!headers_sent) {
         System.out.println("</BODY></HTML>");
      }
      System.exit(1);
   }
}
