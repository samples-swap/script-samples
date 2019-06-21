/*
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is mozilla.org code.
 *
 * The Initial Developer of the Original Code is
 * Bluesoft Consultoria em Informatica Ltda.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 */
package br.com.bluesoft.bee.util;

import java.util.regex.Matcher
import java.util.regex.Pattern


public class StringUtil {

	static def splitString(str) {
		def arr = []
		str.eachLine {
			def line = it.trim()
			if(line != "")
				arr << line
		}

		return arr
	}

	static boolean compare(String str1, String str2) {
		def arr1 = splitString("$str1")
		def arr2 = splitString("$str2")

		return arr1 == arr2
	}
	
	static deleteSchemaNameFromUserTypeText(userTypeText) {
		Pattern regex = Pattern.compile("\\\".*\\.\\\"(.*)\\\"");
		Matcher regexMatcher = regex.matcher(userTypeText);
		def newUserTypeText = regexMatcher.replaceFirst('$1');
	}
}
