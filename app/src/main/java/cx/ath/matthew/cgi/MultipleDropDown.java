/*
 * Java CGI Library
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

/*
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package cx.ath.matthew.cgi;

import java.util.List;

/**
 * @author Agent
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class MultipleDropDown extends DropDown {

	/**
	 * @param name
	 * @param label
	 * @param values
	 * @param defval
	 * @param indexed
	 */
	public MultipleDropDown(String name, String label, String[] values,
			String defval, boolean indexed) {
		super(name, label, values, defval, indexed);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 * @param label
	 * @param values
	 * @param defval
	 * @param indexed
	 */
	public MultipleDropDown(String name, String label, String[] values,
			int defval, boolean indexed) {
		super(name, label, values, defval, indexed);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 * @param label
	 * @param values
	 * @param defval
	 * @param indexed
	 */
	public MultipleDropDown(String name, String label, List values,
			String defval, boolean indexed) {
		super(name, label, values, defval, indexed);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 * @param label
	 * @param values
	 * @param defval
	 * @param indexed
	 */
	public MultipleDropDown(String name, String label, List values, int defval,
			boolean indexed) {
		super(name, label, values, defval, indexed);
		// TODO Auto-generated constructor stub
	}
	
	protected String print()
	   {
	      String s = "";
	      s += "<select name='"+name+"' multiple='multiple' size='"+values.length+"'>\n";
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
