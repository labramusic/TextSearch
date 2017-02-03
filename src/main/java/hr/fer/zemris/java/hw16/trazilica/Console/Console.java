package hr.fer.zemris.java.hw16.trazilica.Console;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Program which acts as a console application for searching similar documents
 * based on a given query. Similiraty is calculated as the similarity of term
 * frequency - inverse document frequency of two files. The dictionary of all
 * words is initialized at the start, with removed stopwords. The shell accepts
 * the following commands: - query: searches for the top 10 results based on
 * similarity - type: accepts the index of a previously returned result as an
 * argument and prints the text of its article on the screen - results: prints
 * the previously returned results on the screen The program expects a single
 * argument - the folder with the articles being searched.
 * 
 * @author labramusic
 *
 */
public class Console {

	/**
	 * Stopwords file path.
	 */
	private final static String stopwordsPath = "hrvatski_stoprijeci.txt";

	/**
	 * Dictionary of words.
	 */
	private static Set<String> dictionary;

	/**
	 * Maps article names to the list of frequencies of words in those articles.
	 */
	private static Map<String, List<Double>> fileFreqs;

	/**
	 * Top results based on query search.
	 */
	private static List<Util.FileSim> topResults;

	/**
	 * The main method.
	 * 
	 * @param args
	 *            command line arguments
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Invalid number of arguments. Path to articles expected.");
			System.exit(0);
		}
		Path articlesDir = null;
		try {
			articlesDir = Paths.get(args[0]);
		} catch (InvalidPathException e) {
			System.out.println("The given argument is not a path.");
			System.exit(0);
		}
		if (!Files.exists(articlesDir) || !Files.isDirectory(articlesDir)) {
			System.out.println("The given folder doesn't exist.");
			System.exit(0);
		}

		List<String> stopWords = null;
		try {
			stopWords = Files.readAllLines(Paths.get(stopwordsPath));
		} catch (IOException e) {
			System.out.println("Error reading file: " + e);
			System.exit(0);
		}

		dictionary = Util.getDictionary(articlesDir, stopWords);
		fileFreqs = Util.getFileFreqs(articlesDir, dictionary);

		System.out.println("Veličina riječnika je " + dictionary.size() + " riječi.");
		System.out.println();

		Scanner sc = new Scanner(System.in);
		while (true) {
			System.out.print("Enter command > ");
			String line = sc.nextLine().trim();

			if (line.startsWith("query")) {
				parseQuery(line);

			} else if (line.equals("results")) {
				parseResults(line);

			} else if (line.startsWith("type")) {
				parseType(line);

			} else if (line.equals("exit")) {
				System.out.println("Did you mean: \"brexit\"?");

			} else if (line.equals("brexit")) {
				break;

			} else {
				System.out.println("Nepoznata naredba.");
			}
			System.out.println();
		}
		sc.close();

	}

	/**
	 * Parses the query command.
	 * 
	 * @param line
	 *            query command
	 */
	private static void parseQuery(String line) {
		String[] args = line.substring(5).trim().split(" +");
		List<String> words = new ArrayList<>();
		for (String arg : args) {
			if (dictionary.contains(arg)) {
				words.add(arg);
			}
		}
		int len = words.size();
		List<Double> queryVector = Util.getVector(dictionary, words);
		topResults = Util.getTopResults(queryVector, fileFreqs);

		System.out.print("Query is: [");
		StringJoiner joiner = new StringJoiner(", ");
		for (int i = 0; i < len; ++i) {
			joiner.add(words.get(i));
		}
		System.out.println(joiner.toString() + "]");
		System.out.println("Najboljih 10 rezultata:");
		printResults();
	}

	/**
	 * Parses the results command.
	 * 
	 * @param line
	 *            results command
	 */
	private static void parseResults(String line) {
		if (topResults == null || topResults.isEmpty()) {
			System.out.println("No previous results found.");
		} else {
			printResults();
		}
	}

	/**
	 * Prints the top results on the screen.
	 */
	private static void printResults() {
		for (int i = 0; i < topResults.size(); ++i) {
			String fileName = topResults.get(i).getFileName();
			Double sim = topResults.get(i).getSimilarity();
			System.out.format("[%2d](%.4f) %s%n", i, sim, fileName);
		}
	}

	/**
	 * Parses the type command.
	 * 
	 * @param line
	 *            type command
	 */
	private static void parseType(String line) {
		line = line.substring(4).trim();
		int index = 0;
		try {
			index = Integer.parseInt(line);
		} catch (NumberFormatException e) {
			System.out.println("Unknown command. Expected argument is index of query result.");
			return;
		}
		if (topResults == null || topResults.size() < index + 1) {
			System.out.println("The requested result is not available.");
			return;
		}

		String fileName = topResults.get(index).getFileName();
		for (int i = 0; i < fileName.length(); ++i)
			System.out.print("-");
		System.out.println();
		System.out.println("Dokument: " + fileName);
		try {
			List<String> lines = Files.readAllLines(Paths.get(fileName));
			for (String l : lines) {
				System.out.println(l);
			}

		} catch (IOException e) {
			System.out.println("Error opening file: " + e);
		}
		for (int i = 0; i < fileName.length(); ++i)
			System.out.print("-");
		System.out.println();
	}

}
