package textsegmentation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

public class Dataset {

	public static void readReferenceFile(String filename, String extension,
			String extension2) {
		Scanner scan = null;
		int k = -1;
		ArrayList<ArrayList<String>> sentences = new ArrayList<ArrayList<String>>();
		try {
			scan = new Scanner(new File(filename));
			while (scan.hasNextLine()) {
				String line = scan.nextLine();
				if (line.equals("==========")) {
					k++;
					sentences.add(new ArrayList<String>());
				} else {
					sentences.get(k).add(line.trim());
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
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
		pw2.println(k);
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
