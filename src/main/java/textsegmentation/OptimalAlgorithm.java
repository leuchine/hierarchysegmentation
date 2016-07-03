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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import scpsolver.problems.LPSolution;
import scpsolver.problems.LPWizard;
import scpsolver.problems.LPWizardConstraint;

import com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class OptimalAlgorithm {
	public static String content;
	public static ArrayList<String> sentences;
	public static ArrayList<HashMap<String, Integer>> vectors;
	public static double[][] quality;
	public static double[][] memo;
	public static int[][] trace;
	public static int k = 10;
	public static double[][] termvectors;
	public static String[] terms;
	public static double[][] docvectors;
	public static ArrayList<HashMap<String, Integer>> segmentations;
	public static int k1 = 4;
	public static int k2 = 8;
	public static HashMap<String, int[]> classes;
	public static Statement statement;
	public static ArrayList<ArrayList<String>> vectorclasses;
	public static double[] dbsimilarities;

	public static void readReferenceFile(String filename) {
		content = null;
		try {
			content = new String(Files.readAllBytes(Paths.get(filename)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] segsplits = content.split("==========");
		ArrayList<String> segmentations = new ArrayList<String>(
				segsplits.length);
		k = 0;
		for (int i = 0; i < segsplits.length; i++) {
			if (!segsplits[i].trim().equals("")) {
				segmentations.add(segsplits[i].trim());
				k++;
			}
		}
		sentences = new ArrayList<String>();
		for (int i = 0; i < segmentations.size(); i++) {
			String[] sentencesplit = segmentations.get(i).split("\\.");
			for (int j = 0; j < sentencesplit.length; j++) {
				if (!sentencesplit[j].trim().equals("")
						&& sentencesplit[j].trim().length() > 1) {
					// System.out.println(sentencesplit[j].trim());
					sentences.add(sentencesplit[j].trim());
				}
			}
		}
		StringBuilder cleancontent = new StringBuilder();
		for (int i = 0; i < sentences.size(); i++) {
			cleancontent.append(" " + sentences.get(i) + ".");
		}
		content = cleancontent.toString().trim();
		// System.out.println(content);
	}

	public static void annotate() {
		vectors = new ArrayList<HashMap<String, Integer>>(sentences.size() + 1);
		WordListConverter.constructUselessWordList();
		for (int i = 0; i < sentences.size(); i++) {
			HashMap<String, Integer> vector = new HashMap<String, Integer>();
			Properties props = new Properties();
			props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
			StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
			Annotation document = new Annotation(sentences.get(i));
			pipeline.annotate(document);
			List<CoreMap> sentences = document.get(SentencesAnnotation.class);
			String firstgram = "";
			for (CoreMap sentence : sentences) {
				ArrayList<String> ners = new ArrayList<String>();
				ArrayList<String> words = new ArrayList<String>();
				ArrayList<String> lemmas = new ArrayList<String>();
				for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
					String word = token.get(TextAnnotation.class);
					String ne = token.get(NamedEntityTagAnnotation.class);
					String lemma = token.get(LemmaAnnotation.class);
					if (word.equals("\'") || word.equals(",")
							|| word.equals("\"") || word.equals(";")
							|| WordListConverter.list.containsKey(lemma))
						// || word.replaceAll("[^a-zA-Z ]",
						// "").trim().equals("")
						continue;
					// System.out.println("word: " + word);
					// System.out.println("lemma: " + lemma);
					words.add(word);
					ners.add(ne);
					lemmas.add(lemma);

					if (vector.containsKey(lemma)) {
						vector.put(lemma, vector.get(lemma) + 1);
					} else {
						vector.put(lemma, 1);
					}

					// if (!firstgram.equals("")) {
					// if (vector.containsKey(firstgram + " " + lemma))
					// vector.put(firstgram + " " + lemma,
					// vector.get(firstgram + " " + lemma) + 1);
					// else
					// vector.put(firstgram + " " + lemma, 1);
					// firstgram = lemma;
					// }
					// if (firstgram.equals(""))
					// firstgram = lemma;
				}
				// for (int j = 0; j < ners.size(); j++) {
				// if (!ners.get(j).equals("O")) {
				// String item = "";
				// while (j < ners.size() && (!ners.get(j).equals("O"))) {
				// item += words.get(j) + " ";
				// j++;
				// }
				// j--;
				// if (vector.containsKey(item.trim())) {
				// vector.put(item.trim(), vector.get(item.trim()) + 1);
				// } else {
				// vector.put(item.trim(), 1);
				// }
				// }
				// }
			}
			System.out.println(vector);
			vectors.add(vector);
		}
		HashMap<String, Integer> all = new HashMap<String, Integer>();
		for (int i = 0; i < vectors.size(); i++) {
			Object[] objs = vectors.get(i).keySet().toArray();
			for (int j = 0; j < objs.length; j++) {
				if (!all.containsKey(objs[j]))
					all.put((String) objs[j], 1);
			}
		}
		Object[] objs = all.keySet().toArray();
		terms = new String[objs.length];
		for (int i = 0; i < terms.length; i++) {
			terms[i] = (String) objs[i];
		}
		termvectors = new double[terms.length][vectors.size()];
		for (int i = 0; i < terms.length; i++) {
			// System.out.print(terms[i] + " ");
			for (int j = 0; j < vectors.size(); j++) {
				termvectors[i][j] = vectors.get(j).containsKey(terms[i]) ? 1
						: 0;
				// System.out.print(termvectors[i][j] + " ");
			}
			// System.out.println();
		}

		int maxFactors = 15;
		double featureInit = 0.01;
		double initialLearningRate = 0.01;
		int annealingRate = 100;
		double regularization = 0.50;
		double minImprovement = 0.0000;
		int minEpochs = 10;
		int maxEpochs = 5000;

		// SvdMatrix matrix = SvdMatrix.svd(termvectors, maxFactors,
		// featureInit,
		// initialLearningRate, annealingRate, regularization, null,
		// minImprovement, minEpochs, maxEpochs);
		// docvectors = matrix.rightSingularVectors();
		// for (int i = 0; i < docvectors.length; i++) {
		// System.out.print(i + " ");
		// for (int j = 0; j < docvectors[i].length; j++) {
		// System.out.print(docvectors[i][j] + " ");
		// }
		// System.out.println();
		// }
	}

	public static double[] vectorAddition(double[] v1, double[] v2) {
		double[] sum = new double[v1.length];
		for (int i = 0; i < v1.length; i++) {
			sum[i] = v1[i] + v2[i];
		}
		return sum;
	}

	public static HashMap<String, Integer> vectorAddition(
			HashMap<String, Integer> v1, HashMap<String, Integer> v2) {
		HashMap<String, Integer> union = new HashMap<String, Integer>();
		Object[] o1 = v1.keySet().toArray();
		Object[] o2 = v2.keySet().toArray();
		for (int i = 0; i < o1.length; i++) {
			if (!union.containsKey(o1[i]))
				union.put((String) o1[i], 1);
		}
		for (int i = 0; i < o2.length; i++) {
			if (!union.containsKey(o2[i]))
				union.put((String) o2[i], 1);
		}
		Object[] o3 = union.keySet().toArray();
		HashMap<String, Integer> addition = new HashMap<String, Integer>();
		for (int i = 0; i < o3.length; i++) {
			int s1 = 0;
			int s2 = 0;
			if (v1.containsKey(o3[i]))
				s1 = v1.get(o3[i]);
			if (v2.containsKey(o3[i]))
				s2 = v2.get(o3[i]);
			addition.put((String) o3[i], s1 + s2);
		}
		return addition;
	}

	public static double cosSimilarity(HashMap<String, Integer> v1,
			HashMap<String, Integer> v2) {
		ArrayList<String> intersection = new ArrayList<String>();
		Object[] iterator = v1.keySet().toArray();
		for (int i = 0; i < iterator.length; i++) {
			if (v2.containsKey(iterator[i]))
				intersection.add((String) iterator[i]);
		}
		double innerproduct = 0;
		for (int i = 0; i < intersection.size(); i++) {
			innerproduct += v1.get(intersection.get(i))
					* v2.get(intersection.get(i));
		}
		return innerproduct / (length(v1) * length(v2));
	}

	public static double cosSimilarity(double[] v1, double[] v2) {
		double innerproduct = 0;
		for (int i = 0; i < v1.length; i++) {
			innerproduct += v1[i] * v2[i];
		}
		return innerproduct / (length(v1) * length(v2));
	}

	public static double length(double[] v) {
		double sum = 0;
		for (int i = 0; i < v.length; i++) {
			sum += v[i] * v[i];
		}
		return Math.sqrt(sum);
	}

	public static double length(HashMap<String, Integer> v) {
		Object[] iterator = v.keySet().toArray();
		double sum = 0;
		for (int i = 0; i < iterator.length; i++) {
			sum += Math.pow(v.get(iterator[i]), 2);
		}
		return Math.sqrt(sum);
	}

	public static void DBpediasimilarityscore() throws SQLException {
		vectorclasses = new ArrayList<ArrayList<String>>(vectors.size());
		for (int i = 0; i < vectors.size(); i++) {
			System.out.println("vector " + i);
			ArrayList<String> vectorclass = new ArrayList<String>();
			HashMap<String, Integer> vector = vectors.get(i);
			Object[] objs = vector.keySet().toArray();
			for (int j = 0; j < objs.length; j++) {
				String target = (String) objs[j];
				target.replace(" ", "_");
				try {
					ResultSet rs = statement
							.executeQuery("select classes from entityClasses where entity like '"
									+ target
									+ "' or entity like '"
									+ target
									+ "\\_%'");
					if (rs.next()) {
						String c = rs.getString(1);
						System.out.println(c.split("\\|")[0]);
						vectorclass.add(c.split("\\|")[0]);
					}
				} catch (MySQLSyntaxErrorException ex) {
					ex.printStackTrace();
				}
			}
			vectorclasses.add(vectorclass);
		}
		dbsimilarities = new double[vectors.size() - 1];
		for (int i = 0; i < dbsimilarities.length; i++) {
			ArrayList<String> v1 = vectorclasses.get(i);
			ArrayList<String> v2 = vectorclasses.get(i + 1);
			double sum = 0;
			for (int j = 0; j < v1.size(); j++) {
				for (int k = 0; k < v2.size(); k++) {
					sum += dbpediascore(v1.get(j), v2.get(k));
				}
			}
			if (v1.size() == 0 || v2.size() == 0)
				dbsimilarities[i] = 0;
			else
				dbsimilarities[i] = sum / v1.size() / v2.size();
			System.out.println(dbsimilarities[i]);
		}
	}

	public static double dbpediascore(String c1, String c2) {
		if (!classes.containsKey(c1) || !classes.containsKey(c2))
			return 0;
		int[] id1 = classes.get(c1);
		int[] id2 = classes.get(c2);
		int i = 0, j = 0;
		int common = 0;
		int distance = 0;
		while (i < id1.length && j < id2.length && id1[i] == id2[j]) {
			common++;
			i++;
			j++;
		}
		distance = id1.length - i + id2.length - j;
		int max = id1.length > id2.length ? id1.length : id2.length;
		return (double) common / (max + distance);
	}

	public static void computeQuality() {
		quality = new double[vectors.size()][vectors.size()];
		for (int i = 0; i < vectors.size(); i++) {
			quality[i][i] = 1;
		}
		for (int i = 0; i < vectors.size(); i++) {
			for (int j = i + 1; j < vectors.size(); j++) {
				HashMap<String, Integer> sum = new HashMap<String, Integer>();
				for (int k = i; k <= j; k++) {
					sum = vectorAddition(sum, vectors.get(k));
				}
				// double[] sumt = new double[docvectors[0].length];
				// for (int k = i; k <= j; k++) {
				// sumt = vectorAddition(sumt, docvectors[k]);
				// }
				for (int k = i; k <= j; k++) {
					quality[i][j] += 1 * cosSimilarity(sum, vectors.get(k));// +
																			// 0
																			// *cosSimilarity(sumt,
																			// docvectors[k]);
				}
			}
		}
	}

	public static double optimalalgorithm(int k) {
		memo = new double[vectors.size()][k + 1];
		trace = new int[vectors.size()][k + 1];
		for (int i = 0; i < vectors.size(); i++) {
			memo[i][1] = quality[0][i];
		}
		for (int j = 2; j <= k; j++) {
			for (int i = 0; i < vectors.size(); i++) {
				memo[i][j] = Integer.MIN_VALUE;
				int position = 0;
				for (int x = 0; x <= i - 1; x++) {
					double value = memo[x][j - 1] + quality[x + 1][i];
					if (value > memo[i][j]) {
						memo[i][j] = value;
						position = x + 1;
					}
				}
				trace[i][j] = position;
			}
		}
		return 0;
	}

	public static void constructCluster(int k, String filename) {
		segmentations = new ArrayList<HashMap<String, Integer>>(k);
		String output = "";
		int endIndex = trace.length - 1;
		for (int j = k; j > 1; j--) {
			String append = "";
			HashMap<String, Integer> segmentation = new HashMap<String, Integer>();
			for (int i = trace[endIndex][j]; i < endIndex; i++) {
				segmentation = vectorAddition(segmentation, vectors.get(i));
			}
			segmentations.add(segmentation);
			System.out.print("[" + trace[endIndex][j] + "," + endIndex + "] ");
			for (int i = trace[endIndex][j]; i <= endIndex; i++) {
				if (i != endIndex)
					append += "0";
				else
					append += "1";
			}
			output = append + output;
			endIndex = trace[endIndex][j] - 1;
		}
		System.out.print("[0," + endIndex + "] ");

		HashMap<String, Integer> segmentation = new HashMap<String, Integer>();
		for (int i = 0; i < endIndex; i++) {
			segmentation = vectorAddition(segmentation, vectors.get(i));
		}
		segmentations.add(segmentation);

		String append = "";
		for (int i = 0; i <= endIndex; i++) {
			if (i != endIndex)
				append += "0";
			else
				append += "1";
		}
		output = append + output;
		System.out.println(output);
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(filename + ".optimalout");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		pw.print(output);
		pw.close();
	}

	public static void ilpsolver() {
		LPWizard lpw = new LPWizard();
		lpw.setMinProblem(false);
		for (int i = 0; i < segmentations.size(); i++) {
			for (int j = 0; j < terms.length; j++) {
				if (segmentations.get(i).get(terms[j]) != null)
					lpw.plus("x" + i + j, segmentations.get(i).get(terms[j]));
				else
					lpw.plus("x" + i + j, 0);
			}
		}
		for (int i = 0; i < segmentations.size(); i++) {
			LPWizardConstraint lpc = lpw.addConstraint("c" + i, k1, ">=");
			for (int j = 0; j < terms.length; j++) {
				lpc.plus("x" + i + j, 1);
			}
			lpc.setAllVariablesBoolean();
		}
		for (int i = 0; i < terms.length; i++) {
			LPWizardConstraint lpc = lpw.addConstraint("a" + i, k2, ">=");
			for (int j = 0; j < segmentations.size(); j++) {
				lpc.plus("x" + j + i, 1);
			}
		}
		LPSolution lps = lpw.solve();
		int sum = 0;
		for (int i = 0; i < segmentations.size(); i++) {
			for (int j = 0; j < terms.length; j++) {
				if (lps.getInteger("x" + i + j) == 1) {
					System.out.println("x" + i + j);
					sum++;
				}
			}
		}
		// System.out.println(lps.toString());
		System.out.println(sum);
		System.out.println(segmentations.size());
	}

	public static void main(String[] args) throws FileNotFoundException,
			SQLException {
		Scanner scan = new Scanner(new File("classid.txt"));
		classes = new HashMap<String, int[]>();
		while (scan.hasNextLine()) {
			String[] splits = scan.nextLine().split(" ");
			int[] numbers = new int[splits.length - 1];
			for (int i = 0; i < numbers.length; i++)
				numbers[i] = Integer.parseInt(splits[i + 1]);
			classes.put(splits[0], numbers);
			System.out.println(splits[0]);
		}

//		DBConnection con = new DBConnection();
//		Connection connection = con.getConnection();
//		statement = connection.createStatement();
		String d = "9-11";
		Path dir = Paths.get(d);
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
			for (Path file : stream) {
				if (file.getFileName().toString().equals("0.ref")) {
					readReferenceFile(file.getParent().toString() + "/"
							+ file.getFileName());
					annotate();
//					DBpediasimilarityscore();
					computeQuality();
					optimalalgorithm(k);
					constructCluster(k, file.getParent().toString() + "/"
							+ file.getFileName());
					System.out.println(file.getFileName());
					System.out.println("============");
//					ilpsolver();
				}
			}
		} catch (IOException | DirectoryIteratorException x) {
			System.err.println(x);
		}
		// HashMap<String, Integer> a = new HashMap<String, Integer>();
		// a.put("a", 2);
		// a.put("b", 1);
		//
		// a.put("d", 5);
		// HashMap<String, Integer> b = new HashMap<String, Integer>();
		// b.put("a", 1);
		// b.put("b", 1);
		// System.out.println(vectorAddition(a, b));
	}
}
