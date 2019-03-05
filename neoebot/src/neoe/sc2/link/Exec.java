package neoe.sc2.link;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Exec {

	private class StreamGobbler extends Thread {
		InputStream is;
		private PrintWriter out;
		String type;

		private StreamGobbler(InputStream is, String type) {
			this.is = is;
			this.type = type;
			this.out = new PrintWriter(System.out);
		}

		@Override
		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line = null;
				while ((line = br.readLine()) != null)
					out.println(type + "> " + line);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	List<String> sb;

	private String workDir;

	public Exec(String workDir) {
		this.workDir = workDir;
	}

	public void addArg(String s) {
		sb.add(s);
	}

	public void addArg(String s1, String s2) {
		sb.add(s1);
		sb.add(s2);
	}

	public Process execute() throws Exception {
		Process p = new ProcessBuilder().command(sb).directory(new File(workDir)).start();
		StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), "stderr");
		StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), "stdout");
		outputGobbler.start();
		errorGobbler.start();
		return p;
	}

	public void setCmd(String executable) {
		sb = new ArrayList<>();
		sb.add(executable);
	}

}
