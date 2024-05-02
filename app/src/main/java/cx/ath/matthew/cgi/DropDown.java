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

import java.util.List;

public class DropDown extends Field
{
   Object[] values;
   Object defval;
   boolean indexed = false;
   /**
    * Create a new DropDown list.
    *
    * @param name The HTML field name.
    * @param label The label to display
    * @param values The values for the drop down list
    * @param defval If this parameter is set then this element will be selected by default.
    * @param indexed If this is set to true, then indexes will be returned, rather than values.
    */
   public DropDown(String name, String label, Object[] values, Object defval, boolean indexed)
   {
      this.name = name;
      this.label = label;
      this.values = values;
      this.indexed = indexed;
      this.defval = defval;
   }
   /**
    * Create a new DropDown list.
    *
    * @param name The HTML field name.
    * @param label The label to display
    * @param values The values for the drop down list
    * @param defval If this parameter is set then this element will be selected by default.
    * @param indexed If this is set to true, then indexes will be returned, rather than values.
    */
   public DropDown(String name, String label, Object[] values, int defval, boolean indexed)
   {
      this.name = name;
      this.label = label;
      this.values = values;
      if (defval < 0)
         this.defval = null;
      else
         this.defval = values[defval];
      this.indexed = indexed;
   }
   /**
    * Create a new DropDown list.
    *
    * @param name The HTML field name.
    * @param label The label to display
    * @param values The values for the drop down list
    * @param defval If this parameter is set then this element will be selected by default.
    * @param indexed If this is set to true, then indexes will be returned, rather than values.
    */
   public DropDown(String name, String label, List values, Object defval, boolean indexed)
   {
      this.name = name;
      this.label = label;
      this.values = (Object[]) values.toArray(new Object[] {});
      this.defval = defval;
      this.indexed = indexed;
   }
   /**
    * Create a new DropDown list.
    *
    * @param name The HTML field name.
    * @param label The label to display
    * @param values The values for the drop down list
    * @param defval If this parameter is set then this element will be selected by default.
    * @param indexed If this is set to true, then indexes will be returned, rather than values.
    */
   public DropDown(String name, String label, List values, int defval, boolean indexed)
   {
      this.name = name;
      this.label = label;
      this.values = (Object[]) values.toArray(new Object[] {});
      if (defval < 0)
         this.defval = null;
      else
         this.defval = values.get(defval);
      this.indexed = indexed;
   }
   protected String print()
   {
      String s = "";
      s += "<select name='"+name+"'>\n";
      for (int i=0; i<values.length; i++) {
         if (indexed)
            s += "   <option value='"+i+"'";
         else
            s += "   <option";
         if (values[i].equals(defval))
            s += " selected='selected'>"+values[i]+"</option>\n";
         else
            s += ">"+values[i]+"</option>\n";
      }
      s += "</select>\n";
      return s;
   }
}


