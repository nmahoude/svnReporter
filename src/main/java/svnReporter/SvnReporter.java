package svnReporter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author nmahoude
 *
 *
 *         Based on work by Chris West
 */
public class SvnReporter {

	public static void main(String[] args) {
		try {
			List<String> lines = readLines(args[0]); // Read the lines into an array.
			String strCSV = convertToCSV(lines); // for storing the content of the CSV.
			System.out.print(strCSV); // Output the CSV to stdout.
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static List<String> readLines(String filename) throws IOException {
		BufferedReader bufferedReader = null;
		if (filename.equals("--stdin")) {
			bufferedReader = new BufferedReader(new InputStreamReader(System.in));
		} else {
			FileReader fileReader = new FileReader(filename);
			bufferedReader = new BufferedReader(fileReader);
		}
		List<String> lines = new ArrayList<String>();
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			lines.add(line);
		}
		bufferedReader.close();
		return lines;
	}

	private static String convertToCSV(List<String> lines) {
		Pattern patBlankLine = Pattern.compile("^\\s*$");
		Pattern patHeader = Pattern.compile("^r(\\d+) \\| (.+?) \\| (\\d{4}-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d) .+? \\| (\\d+) lines?$");

		Boolean isBlank = true;
		String rnum, user, date, text;
		Long count;
		String line, ret = ""; // "\"Revision Number\",\"User\",\"Date/Time\",\"SVN Message\"";
		for (Iterator<String> i = lines.iterator(); i.hasNext();) {
			line = i.next();
			if (Pattern.matches("^-{10,}$", line) && i.hasNext()) {
				line = i.next();
				Matcher header = patHeader.matcher(line);
				if (header.matches()) {
					rnum = header.group(1);
					user = header.group(2);
					date = header.group(3);
					count = Long.parseLong(header.group(4));

					String moduleName = null;
					// Read the changed path until the blank line.
					while (i.hasNext() && !patBlankLine.matcher(line = i.next()).matches()) {
						// read changed path to autodetect module
						moduleName = extractModuleFrom(line, moduleName);
					}
					

					// Gather all of the text.
					text = "";
					while (i.hasNext() && count-- > 0) {
						String nextLine = i.next();
						text += nextLine + (count > 0 ? ", " : "");
					}
					
					
					
					// Add the line.
					ret += rnum + ";" + clean(user) + ";" + clean(date) + ";" + clean(text) + ";" + (moduleName  != null ? moduleName : "?")+ System.getProperty("line.separator");
				}
			}
		}
		return ret;
	}

	private static String extractModuleFrom(String line, String oldModuleName) {
		if (oldModuleName != null) return oldModuleName;
		
		Pattern pattern = Pattern.compile("(.*)/(.*)/(trunk|branches|tags|release)(.*)");
		Matcher matcher = pattern.matcher(line);
		if (matcher.matches()) {
//			System.err.println("Found a trunk, branch or tag : "+changedPath);
			String moduleName = matcher.group(2);
//			System.err.println("    -> "+moduleName);
			return moduleName;
		} else {
//			System.err.println("Not found in "+changedPath);
		}

		return oldModuleName;
	}

	private static String clean(String input) {
		return input.replaceAll(";", ",").replaceAll("\n", " ").trim();
	}

	private static String quote(String input) {
		return '"' + input.replaceAll("\"", "\"\"") + '"';
	}
}
