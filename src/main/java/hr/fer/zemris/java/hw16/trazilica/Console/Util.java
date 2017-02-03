package hr.fer.zemris.java.hw16.trazilica.Console;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for various operations with documents and calculating word frequencies.
 * @author labramusic
 *
 */
public class Util {

	/**
	 * Pattern which describes a word.
	 */
	private final static Pattern WORD_PATTERN = Pattern.compile("\\p{L}+", Pattern.UNICODE_CHARACTER_CLASS);

	/**
	 * Number of documents.
	 */
	private static int numOfDocs;

	/**
	 * Maps files to respective frequencies of each word.
	 */
	private static Map<File, Map<String, Integer>> fileWordFreqs;

	/**
	 * Maps words to the number of documents containing them.
	 */
	private static Map<String, Integer> docsContainingWords;

	/**
	 * Returns the dictionary of words from the given folder.
	 * @param articlesDir path to the articles directory
	 * @param stopWords list of stopwords
	 * @return dictionary of words
	 */
	public static Set<String> getDictionary(Path articlesDir, List<String> stopWords) {
		Set<String> dictionary = new HashSet<>();
		File[] articles = articlesDir.toFile().listFiles();

		try {
			for (File article : articles) {
				String document = new String(Files.readAllBytes(article.toPath()));
				List<String> words = getAllWords(document);
				dictionary.addAll(words);
			}
			dictionary.removeAll(stopWords);
		} catch (IOException e) {
			System.out.println("Error reading file: " + e);
			System.exit(0);
		}
		return dictionary;
	}

	/**
	 * Returns the frequencies of files based on the appearance of words they contain and all
	 * of the words from the dictionary.
	 * @param articlesDir path to articles directory
	 * @param dictionary dictionary of words
	 * @return map of file frequencies
	 */
	public static Map<String, List<Double>> getFileFreqs(Path articlesDir, Set<String> dictionary) {
		Map<String, List<Double>> fileFreqs = new HashMap<>();
		File[] articles = articlesDir.toFile().listFiles();
		calculateFrequencies(articles, dictionary);

		for (File article : articles) {
			List<Double> vector = new ArrayList<>(dictionary.size());
			for (String word : dictionary) {
				int tf = fileWordFreqs.get(article).get(word);
				double idf = getInverseDocumentFrequency(word);
				vector.add(tf*idf);
			}
			String fileName = article.getAbsolutePath();
			fileFreqs.put(fileName, vector);
		}

		return fileFreqs;
	}

	/**
	 * Calculates the frequencies of all words in articles and fills appropriate maps.
	 * @param articles the given articles
	 * @param dictionary dictionary of words
	 */
	private static void calculateFrequencies(File[] articles, Set<String> dictionary) {
		try {
			numOfDocs = articles.length;
			fileWordFreqs = new HashMap<>();
			docsContainingWords = new HashMap<>();
			for (File article : articles) {
				Map<String, Integer> wordFreq = new HashMap<>();
				fileWordFreqs.put(article, wordFreq);
				String document = new String(Files.readAllBytes(article.toPath()));
				List<String> words = getAllWords(document);
				for (String word : dictionary) {
					int freq = Collections.frequency(words, word);
					wordFreq.put(word, freq);

					Integer value = docsContainingWords.get(word);
					if (freq>0) {
						docsContainingWords.put(word, value == null ? 1 : value+1);
					} else if (value==null) {
						docsContainingWords.put(word, 0);
					}
				}
			}
		} catch (IOException e) {
			System.out.println("Error opening file: "+e);
			System.exit(0);
		}
	}

	/**
	 * Returns a list of all the words from the given string of text.
	 * @param string text
	 * @return list of words from text
	 */
	private static List<String> getAllWords(String string) {
		List<String> words = new ArrayList<>();
		Matcher m = WORD_PATTERN.matcher(string);
		while (m.find()) {
			words.add(m.group().toLowerCase());
		}
		return words;
	}

	/**
	 * Calculates the inverse document frequency with the given word.
	 * @param word given word
	 * @return inverse document frequency
	 */
	private static double getInverseDocumentFrequency(String word) {
		int docsContaining = docsContainingWords.get(word);
		return Math.log10((double)numOfDocs/docsContaining);
	}

	/**
	 * Returns a vector from the given list of words and the dictionary of words.
	 * @param dictionary dictionary of words
	 * @param words list of words
	 * @return vector calculated from tf and idf
	 */
	public static List<Double> getVector(Set<String> dictionary, List<String> words) {
		List<Double> vector = new ArrayList<>(dictionary.size());
		for (String word : dictionary) {
			int tf = Collections.frequency(words, word);
			double idf = getInverseDocumentFrequency(word);
			vector.add(tf*idf);
		}
		return vector;
	}

	/**
	 * Returns a list of file similarity objects as top results, based on the query bector.
	 * @param queryVector query vector
	 * @param fileFreqs frequencies of files
	 * @return list of file similarity objects
	 */
	public static List<FileSim> getTopResults(List<Double> queryVector,
			Map<String, List<Double>> fileFreqs) {
		List<FileSim> results = new ArrayList<>();
		for (String article : fileFreqs.keySet()) {
			Double similarity = calculateSimilarity(fileFreqs.get(article), queryVector);
			if (similarity > 0) {
				FileSim fs = new FileSim(article, similarity);
				results.add(fs);
			}
		}
		Collections.sort(results);
		if (results.size() > 10) {
			results = results.subList(0, 10);
		}
		return results;
	}

	/**
	 * Calculates the similarity between two vectors as the cosine of the angle between them.
	 * @param fileVector vector from file
	 * @param queryVector vector from query
	 * @return similarity between vectors
	 */
	private static Double calculateSimilarity(List<Double> fileVector, List<Double> queryVector) {
		int vectorSize = fileVector.size();
		double numerator = 0;
		for (int i=0; i<vectorSize; ++i) {
			numerator += (fileVector.get(i)*queryVector.get(i));
		}
		double fileVectorNorm = calculateNorm(fileVector);
		double queryVectorNorm = calculateNorm(queryVector);
		double denominator = fileVectorNorm*queryVectorNorm;
		return numerator/denominator;
	}

	/**
	 * Calculates the norm of a vector.
	 * @param fileVector given vector
	 * @return norm of vector
	 */
	private static double calculateNorm(List<Double> fileVector) {
		double result = 0;
		int vectorSize = fileVector.size();
		for (int i=0; i<vectorSize; ++i) {
			result += (Math.pow(fileVector.get(i), 2));
		}
		return Math.sqrt(result);
	}

	/**
	 * Helping file similarity class which contains a file name and the similarity calculated
	 * from a previous query. 
	 * @author labramusic
	 *
	 */
	public static class FileSim implements Comparable<FileSim> {

		/**
		 * File name.
		 */
		private String fileName;

		/**
		 * Similarity of vectors.
		 */
		private Double similarity;

		/**
		 * Initializes a new file similarity object.
		 * @param fileName file name
		 * @param similarity the similarity
		 */
		public FileSim(String fileName, Double similarity) {
			this.fileName = fileName;
			this.similarity = similarity;
		}

		/**
		 * Gets the file name.
		 *
		 * @return the file name
		 */
		public String getFileName() {
			return fileName;
		}

		/**
		 * Gets the similarity.
		 *
		 * @return the similarity
		 */
		public Double getSimilarity() {
			return similarity;
		}

		@Override
		public int compareTo(FileSim fs) {
			return fs.similarity.compareTo(similarity);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			FileSim other = (FileSim) obj;
			if (fileName == null) {
				if (other.fileName != null)
					return false;
			} else if (!fileName.equals(other.fileName))
				return false;
			return true;
		}

	}

}
