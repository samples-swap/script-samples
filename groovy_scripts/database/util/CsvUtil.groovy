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
package br.com.bluesoft.bee.util


class CsvUtil {

	static private def parseLine(def line) {
		def result = []
		def matcher = line.split("(?<!\\\\),")
		matcher.each {
			def campo = it.trim().replace("\\,", ",").replace("\\n", "\n")
			if(campo.equals(""))
				result << null
			else
				result << campo
		}

		// add null values at the end of array, because split function remove last delimiters
		def m = line =~ /,*$/
		if(m.size() > 1) {
			for(n in 1..m[0].size()) {
				result << null
			}
		}

		return result
	}

	static def read(def file) {
		def result = []
		file.eachLine( "utf-8", { result << parseLine(it) })
		return result
	}

	static void write(def file, Collection dados) {
		def writer = file.newPrintWriter("utf-8")
		dados.each {
			def linha = writeLine(it)
			writer.println(linha)
		}
		writer.close()
	}

	static String writeLine(row) {
		def linha = new StringBuffer()
		row.each {
			if(it != null)
				linha << ("${it}".replace(",", "\\,").replace("\n", "\\n")) + ",";
			else {
				linha << ","
			}
		}
		linha = linha[0..-2]
		return linha
	}
}
