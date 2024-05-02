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

import java.util.Iterator;
import java.util.Map;

class testcgi extends CGI
{
   protected void cgi(Map POST, Map GET, Map ENV, Map COOKIE, String[] params) throws Exception
   {
      header("Content-type", "text/plain");
      setcookie("testcgi", "You have visited us already");
      out("This is a test CGI program");
      out("These are the params:");
      for (int i=0; i < params.length; i++)
         out("-- "+params[i]);
      
      out("These are the POST vars:");
      Iterator i = POST.keySet().iterator();
      while (i.hasNext()) {
         String s = (String) i.next();
         out("-- "+s+" => "+POST.get(s));
      }
      
      out("These are the GET vars:");
      i = GET.keySet().iterator();
      while (i.hasNext()) {
         String s = (String) i.next();
         out("-- "+s+" => "+GET.get(s));
      }
        
      out("These are the ENV vars:");
      i = ENV.keySet().iterator();
      while (i.hasNext()) {
         String s = (String) i.next();
         out("-- "+s+" => "+ENV.get(s));
      }
      
      out("These are the COOKIEs:");
      i = COOKIE.keySet().iterator();
      while (i.hasNext()) {
         String s = (String) i.next();
         out("-- "+s+" => "+COOKIE.get(s));
      }   
   }

   public static void main(String[] args)
   {
      CGI cgi = new testcgi();
      cgi.doCGI(args);
   }
}
