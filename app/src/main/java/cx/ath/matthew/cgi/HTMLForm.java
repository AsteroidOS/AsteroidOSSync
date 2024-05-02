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
import java.util.Vector;


/**
 * Class to manage drawing HTML forms
 */
public class HTMLForm
{
   private String target;
   private String submitlabel;
   private String tableclass;
   private Vector fields;
   private boolean post = true;
   
    /**
    * @param target The module to submit to
    */
   public HTMLForm(String target)
   {
      this(target, "Submit", null);
   }
  
   /**
    * @param target The module to submit to
    * @param submitlabel The string to display on the submit button
    */
   public HTMLForm(String target, String submitlabel)
   {
      this(target, submitlabel, null);
   }

   /**
    * @param target The module to submit to
    * @param submitlabel The string to display on the submit button
    * @param tableclass The class= parameter for the generated table
    */
   public HTMLForm(String target, String submitlabel, String tableclass)
   {
      this.target = target;
      this.submitlabel = submitlabel;
      this.tableclass = tableclass;
      fields = new Vector();
   }

   /**
    * Add a field to be displayed in the form.
    *
    * @param field A Field subclass.
    */
   public void addField(Field field)
   {
      fields.add(field);
   }

   /**
    * Set GET method rather than POST
    * @param enable Enable/Disable GET
    */
   public void setGET(boolean enable)
   {
      post = !enable;
   }
   
   /**
    * Shows the form.
    * @param cgi The CGI instance that is handling output
    */
   public void display(CGI cgi)
   {
      try {
         cgi.out("<form action='"+CGITools.escapeChar(target,'"')+"' method='"+
               (post?"post":"get")+"'>");
         if (null == tableclass)
            cgi.out("<table>");
         else
            cgi.out("<table class='"+tableclass+"'>");
         
         Iterator i = fields.iterator();
         while (i.hasNext()) {
            Field f = (Field) i.next();
          if (f instanceof NewTable) {
             cgi.out(f.print());
          }
            if (!(f instanceof HiddenField) && !(f instanceof SubmitButton) && !(f instanceof NewTable)) {
               cgi.out("   <tr>");
               cgi.out("      <td>"+f.label+"</td>");
               cgi.out("      <td>"+f.print()+"</td>");
               cgi.out("   </tr>");
            }
         }
         cgi.out("   <tr>");
         cgi.out("      <td colspan='2' style='text-align:center;'>");
         i = fields.iterator();
         while (i.hasNext()) {
            Field f = (Field) i.next();
            if (f instanceof HiddenField || f instanceof SubmitButton) {
               cgi.out("         "+f.print());
            }
         }      
         cgi.out("         <input type='submit' name='submit' value='"+CGITools.escapeChar(submitlabel,'\'')+"' />");
         cgi.out("      </td>");
         cgi.out("   </tr>");
         cgi.out("</table>");
         cgi.out("</form>");
      } catch (CGIInvalidContentFormatException CGIICFe) {}
   }
}



