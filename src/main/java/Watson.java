
/**
 * Authors : Aditya Kumar and Surinder Singh
 * 
 * Course : CSC 483/583: Text Retrieval 
 * 
 * This is the Watson Program, our submission for the 
 * final project for CSC 583. It takes in a Apache Lucene 
 * index made with approx. 280000 documents, and searches 
 * through it to find answers to 100 Jeopardy questions 
 * that have appeared on the show in past. 
 * 
 * It requires an index to work, so run IndexMaker.java file
 * if index is not ready. 
 * It also requires a txt file with the clues, categories and correct answers
 * to calculate performance metrics like P@1 and MRR. 
 * 
 */

import java.io.FileNotFoundException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;

public class Watson {

	// Class Variables
	static FSDirectory index;
	static EnglishAnalyzer analyzer;

	/**
	 * Initialized the index and analyzer.
	 * 
	 * @param indexLoc
	 */
	public Watson(String indexLoc) {
		try {
			Watson.index = FSDirectory.open(Paths.get("wiki_index.lucene"));
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Unable to load index");
		}
		Watson.analyzer = new EnglishAnalyzer();
	}

	/**
	 * Main Menu
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		@SuppressWarnings("unused")
		Watson w = new Watson("wiki_index.lucene");
		System.out.println("=== I.B.M WATSON (or our attempt at it) ===");
		System.out.println("1. Standard Results");
		System.out.println("2. Better Results");
		System.out.println("3. Custom Query");
		System.out.print("ENTER YOUR CHOICE (1, 2 or 3): \n");
		try (Scanner scanner = new Scanner(System.in)) {
			int option = scanner.nextInt();
			if (option == 3) {
				customQueryManager();
			} else {
				assessSystem(option == 2);
			}
			scanner.close();

		}
	}

	private static void customQueryManager() {
		System.out.println("=== Custom Query Mode ===");
		System.out.println("1. Standard Results");
		System.out.println("2. Better Results");
		System.out.print("ENTER YOUR CHOICE (1, 2): \n");

		try (Scanner queryScanner = new Scanner(System.in)) {
			int option = queryScanner.nextInt();
			queryScanner.nextLine();
			System.out.print("Enter Category: ");
			String category = queryScanner.nextLine();

			System.out.print("Enter Clue:");
			String clue = queryScanner.nextLine();

			System.out.println("\n-- ANSWER --");
			if (option == 1)
				System.out.println(StandardQueryHandler(clue.trim(), category.trim()).get(0));
			else
				System.out.println(ImprovedQueryHandler(clue.trim(), category.trim()).get(0));
		}
	}

	/**
	 * This is actual main of the class. It reads in from a txt with jeopardy
	 * questions, and then calls the function for finding the answer to it based on
	 * user selection. It also calculates the P@1 and MRR metrics to calculate the
	 * performance of the system
	 * 
	 * @param improved
	 */
	private static void assessSystem(boolean improved) {

		double precisionCount = 0;
		double mrr = 0;

		// Can be changed according to local system
		try (Scanner inputScanner = new Scanner(new File("questions.txt"))) {
			while (inputScanner.hasNext()) {
				String category = inputScanner.nextLine();
				String clue = inputScanner.nextLine();
				String actualAnswer = inputScanner.nextLine();
				ArrayList<String> output;

				if (!improved)
					output = StandardQueryHandler(clue.trim(), category.trim());
				else
					output = ImprovedQueryHandler(clue.trim(), category.trim());
				if (actualAnswer.indexOf(output.get(0)) != -1) {
					precisionCount++;
				}
				// 20 potential answers
				for (int i = 0; i < 20; i++) {
					if (actualAnswer.indexOf(output.get(i)) != -1) {

						mrr += 1.0 / (i + 1);
						break;
					}
				}
				System.out.println(category);
				System.out.println(clue);
				System.out.println("ACTUAL: " + actualAnswer + " GOT: " + output.get(0) + "\n");
				inputScanner.nextLine();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		System.out.println("\nP@1: " + precisionCount / 100);
		System.out.println("MRR: " + mrr / 100);

	}

	/**
	 * To get rid of the dialogues in categories
	 * 
	 * @param category
	 * @return
	 */
	private static String categoryProcesser(String category) {
		String result = "";
		for (int i = 0; i < category.length(); i++) {
			if (category.charAt(i) != '(')
				result += category.charAt(i);
			else
				break;
		}
		return result;
	}

	/**
	 * Core implementation. Simply search for the entire clue to get the top 20
	 * alogrithms according to BM25 scoring.
	 * 
	 * @param question
	 * @param category
	 * @return
	 */
	private static ArrayList<String> StandardQueryHandler(String question, String category) {
		String[] searchableFields = { "CONTENT", "CATEGORY" };
		question = question.replaceAll("([^a-zA-Z0-9.,\\s])", "\\\\$1");
		category = categoryProcesser(category).replaceAll("([^a-zA-Z0-9.,\\s])", "\\\\$1");
		ArrayList<String> results = new ArrayList<>();
		try {
			String[] queries = { question, category };
			Query q = MultiFieldQueryParser.parse(queries, searchableFields, analyzer);
			DirectoryReader reader = DirectoryReader.open(index);
			IndexSearcher searcher = new IndexSearcher(reader);
			TopDocs docs = searcher.search(q, 20);
			ScoreDoc[] hits = docs.scoreDocs;
			// sort the matches
			Arrays.sort(hits, new Comparator<ScoreDoc>() {
				@Override
				public int compare(ScoreDoc a, ScoreDoc b) {
					return Float.compare(b.score, a.score);
				}
			});
			for (ScoreDoc hit : hits) {
				int docId = hit.doc;
				Document d = searcher.doc(docId);
				results.add(d.get("TITLE").toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return results;
	}

	/**
	 * Use ChatGPT to enhance the core implementation by re-ordering the fields
	 * according to its own knowledge. It is not allowed to modify the contents
	 * though, just reorder them.
	 * 
	 * @param question
	 * @param category
	 * @return
	 */
	private static ArrayList<String> ImprovedQueryHandler(String question, String category) {
		String[] searchableFields = { "CONTENT", "CATEGORY" };
		String clue = question.replaceAll("([^a-zA-Z0-9.,\\s])", "");
		question = question.replaceAll("([^a-zA-Z0-9.,\\s])", "\\\\$1");
		String ct = categoryProcesser(category).replaceAll("([^a-zA-Z0-9.,\\s])", "");
		category = ct.replaceAll("([^a-zA-Z0-9.,\\s])", "\\\\$1");
		ct = ct.replaceAll("([^a-zA-Z0-9.,\\s])", "");
		ArrayList<String> results = new ArrayList<>();
		try {
			String[] queries = { question, category };
			Query q = MultiFieldQueryParser.parse(queries, searchableFields, analyzer);
			DirectoryReader reader = DirectoryReader.open(index);
			IndexSearcher searcher = new IndexSearcher(reader);
			TopDocs docs = searcher.search(q, 20);
			ScoreDoc[] hits = docs.scoreDocs;
			Arrays.sort(hits, new Comparator<ScoreDoc>() {
				@Override
				public int compare(ScoreDoc a, ScoreDoc b) {
					return Float.compare(b.score, a.score);
				}
			});
			for (ScoreDoc hit : hits) {
				int docId = hit.doc;
				Document d = searcher.doc(docId);
				results.add(d.get("TITLE").toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		String chatGPTOutput = chatGPT("You are an Jeopardy expert, the best that ever lived."
				+ " Your task is to re-arrange the list of potential answers that are provided to you by the user."
				+ " \\n\\nRULES (They can not be broken in any scenario whatsover):"
				+ " \\n 1 - DO NOT ADD OR REMOVE OR MODIFY THE ELEMENTS OF THE PROVIDED LIST."
				+ " EVEN IF YOU KNOW THE CORRECT ANSWER IS NOT IN THE LIST, DONT ADD IT. "
				+ " \\n 2 - The output should be enclosed in []."
				+ " \\n 3 - There should be no extra dialogue or newlines in the ENTIRE provided answer."
				+ " \\n 4 - DO NOT REARRANGE THE LIST IF YOU DONT HAVE A BETTER ORDER. "
				+ " \\n 5 - DO NOT OUTPUT YOUR ANSWER UNTILL YOU HAVE GOT A LIST WITH EXACTLY 20 ITEMS. "
				+ " \\n\\nThe steps are as follows:" + " \\n1 -- Take the the LIST, QUERY and CATEGORY as inputs."
				+ " \\n2 -- REARRANGE the contents of the list according to their relevance to the query."
				+ " REMEMBER TO FOLLOW THE PROVIDED RULES."
				+ " \\n3 -- Output the rearranged list enclosed in [] while following the specified rules."
				+ " \\n\\n The inputs are specified below: " + " \\n **LIST** : " + results + " \\n **QUERY** : " + clue
				+ " \\n **CATEGORY**: " + ct);
		results = new ArrayList<>(Arrays.asList(chatGPTOutput.split(", ")));
		return results;
	}

	/**
	 * Function to connect with ChatGPT since the API is not available for Java.
	 * 
	 * @param prompt
	 * @return
	 */
	public static String chatGPT(String prompt) {
		String url = "https://api.openai.com/v1/chat/completions";
		String apiKey = "sk-proj-fu1J0kHfi1yr3qcDyp1zT3BlbkFJRMkMWwNKjV8Fcllq7HdE";
		String model = "gpt-4-turbo";
		StringBuffer response = new StringBuffer();
		try {
			URL obj = new URL(url);
			HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Authorization", "Bearer " + apiKey);
			connection.setRequestProperty("Content-Type", "application/json");

			// The request body
			String body = "{\"model\": \"" + model + "\", \"messages\": [{\"role\": \"user\", \"content\": \"" + prompt
					+ "\"}], \"temperature\": 0, \"top_p\": 1," + " \"frequency_penalty\":0, \"presence_penalty\":0 }";
			connection.setDoOutput(true);
			OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
			writer.write(body);
			writer.flush();
			writer.close();

			// Response from ChatGPT
			BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line;

			while ((line = br.readLine()) != null) {
				response.append(line);
			}
			br.close();

			// calls the method to extract the message.
			return extractMessageFromJSONResponse(response.toString());

		} catch (IOException e) {
			System.out.println(response);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Extract the list from chatgpt's answer.
	 * 
	 * @param response
	 * @return
	 */
	public static String extractMessageFromJSONResponse(String response) {
		int start = response.indexOf("content") + 12;

		int end = response.indexOf("]", start);
		return response.substring(start, end);

	}

}
