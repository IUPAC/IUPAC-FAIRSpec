package com.integratedgraphics.extract;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Hashtable;
import java.util.Map;

import com.integratedgraphics.util.Util;

public class Extractor {

	public void extractFile(File ifsExtractScript, File targetDir) throws IOException {
		extractStream(ifsExtractScript.toURI().toURL().openStream(), targetDir);
	}

	public void extractStream(InputStream is, File targetDir) throws IOException {
		byte[] bytes = Util.getLimitedStreamBytes(is, 0, null, true);
		String script = new String(bytes);
		parseScript(script);
	}

	Map<String, Object> htExtract = new Hashtable<>();

	private void parseScript(String script) throws IOException {
		BufferedReader reader = new BufferedReader(new StringReader(script));
		String line;
		while ((line = reader.readLine()) != null) {
			
		}
		reader.close();
	}
	
	
	
	

}
