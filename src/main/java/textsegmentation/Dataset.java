package textsegmentation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Dataset {

	public static void readReferenceFile(String filename, String extension,
			String extension2) {
		String content = null;
		try {
			content = new String(Files.readAllBytes(Paths.get(filename)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] segsplits = content.split("==========");
		ArrayList<String> segmentations = new ArrayList<String>(
				segsplits.length);
		for (int i = 0; i < segsplits.length; i++) {
			if (!segsplits[i].trim().equals(""))
				segmentations.add(segsplits[i].trim());
		}
		ArrayList<ArrayList<String>> sentences = new ArrayList<ArrayList<String>>();
		for (int i = 0; i < segmentations.size(); i++) {
			String[] sentencesplit = segmentations.get(i).split("\\.");
			ArrayList<String> segsentences = new ArrayList<String>();
			for (int j = 0; j < sentencesplit.length; j++) {
				if (!sentencesplit[j].trim().equals("")
						&& sentencesplit[j].trim().length() > 1) {
					// System.out.println(sentencesplit[j].trim());
					segsentences.add(sentencesplit[j].trim());
				}
			}
			sentences.add(segsentences);
		}

		PrintWriter pw = null;
		PrintWriter pw2 = null;
		try {
			pw = new PrintWriter(filename + "." + extension);
			pw2 = new PrintWriter(filename + "." + extension2);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		pw2.println(segmentations.size());
		for (int i = 0; i < sentences.size(); i++) {
			for (int j = 0; j < sentences.get(i).size(); j++) {
				if (j != sentences.get(i).size() - 1)
					pw.print("0");
				else
					pw.print("1");
				pw2.println((sentences.get(i).get(j) + ".").replace("\n", ""));
			}
		}
		pw.close();
		pw2.close();
	}

	public static void main(String[] args) {
		String d = "9-11";
		Path dir = Paths.get(d);
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
			for (Path file : stream) {
				if (file.getFileName().toString().endsWith(".ref")) {
					readReferenceFile(
							file.getParent().toString() + "/"
									+ file.getFileName(), "out", "text");
					System.out.println(file.getFileName());
					System.out.println("============");
				}
			}
		} catch (IOException | DirectoryIteratorException x) {

			System.err.println(x);
		}
	}
}
