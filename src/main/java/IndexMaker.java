
/**
 * Authors : Aditya Kumar and Surinder Singh
 * 
 * Course : CSC 483/583: Text Retrieval 
 * 
 * This program is used for creating an Apache Lucene Index. 
 * Run it before running the Watson file unless the index is already present.
 * Also, change the location of the wikifiles according to your own system. 
 * 
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

public class IndexMaker {

	public static IndexWriter indexWriter;

	/**
	 * This function goes throw a text file line by line trying to parse them. It is
	 * able to distinguish between file headings, categories and body Also
	 * initializing the analyzer here
	 * 
	 * @param collection
	 * @throws IOException
	 */
	public static void indexCollectionFile(File collection) throws IOException {
		EnglishAnalyzer analyzer = new EnglishAnalyzer();
		FSDirectory index = FSDirectory.open(Paths.get("wiki_index.lucene"));
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		indexWriter = new IndexWriter(index, config);
		try (Scanner inputScanner = new Scanner(collection)) {
			String wikiPage = "";
			String currentTitle = "";
			String categories = "";

			while (inputScanner.hasNextLine()) {
				String line = inputScanner.nextLine().trim().replace("\n", "");
				// doc start
				if (line.startsWith("[[") && line.endsWith("]]") && line.indexOf(":") == -1) {
					if (!wikiPage.trim().equals("") || !wikiPage.startsWith("#REDIRECT")) {
						addToIndex(currentTitle, wikiPage, categories);
					}
					wikiPage = "";
					currentTitle = line.replace("[[", "").replace("]]", "");
					categories = "";
					// categories line
				} else if (line.startsWith("CATEGORIES: ")) {
					categories = line.substring(11);
					// references or subsection headings- need to be skipped
				} else if (line.equals("") || (line.startsWith("=") && line.endsWith("=")) || line.startsWith("|")) {
					continue;
				} else {
					wikiPage += parseLine(line);
				}
			}
			if (!wikiPage.equals("")) {
				addToIndex(currentTitle, wikiPage, categories);
			}
			inputScanner.close();
			indexWriter.close();

		} catch (FileNotFoundException e) {
			System.out.println("Can't read the file: " + collection.getAbsolutePath());
			e.printStackTrace();

		}

	}

	/**
	 * This is used for parsing the lines in the body of a wiki page. We tried
	 * getting rid of the [tpl], links references and file tags.
	 * 
	 * @param line
	 * @return
	 */
	private static String parseLine(String line) {
		String[] splitLine = line.trim().split(" ");
		ArrayList<String> result = new ArrayList<>();
		boolean copy = true;
		int tplcount = 0;
		for (String item : splitLine) {
			if (item.indexOf("[tpl]") != -1) {
				tplcount++;
				if (tplcount == 1)
					result.add(item.substring(0, item.indexOf("[tpl]")) + " ");
				copy = false;
			}
			if (item.indexOf("[/tpl]") != -1) {
				tplcount--;
				String extraPart = item.substring(item.indexOf("[/tpl]") + 6);
				if (extraPart.indexOf("[tpl]") == -1)
					result.add(" " + extraPart);
				if (tplcount == 0)
					copy = true;
				continue;
			}
			if (item.indexOf("http") != -1 || item.indexOf("|") != -1 || item.indexOf("[[") != -1
					|| item.indexOf("]]") != -1) {
				continue;
			}
			if (copy) {
				result.add(item);
			}
		}
		return String.join(" ", result) + " ";
	}

	/**
	 * This function is used for adding a wiki doc to the index. We save the title,
	 * the category and filtered content.
	 * 
	 * @param currentTitle
	 * @param wikiPage
	 * @param categories
	 */
	private static void addToIndex(String currentTitle, String wikiPage, String categories) {
		Document doc = new Document();
		doc.add(new StringField("TITLE", currentTitle, Field.Store.YES));
		doc.add(new TextField("CATEGORY", categories, Field.Store.YES));
		doc.add(new TextField("CONTENT", wikiPage, Field.Store.YES));
		try {
			indexWriter.addDocument(doc);
		} catch (IOException e) {
			System.out.println("UNABLE TO ADD WIKIPAGE: " + currentTitle);
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		// change this based on your system
		String dirPath = "/Users/adityakumar/Downloads/wiki-subset-20140602";
		File dir = new File(dirPath);
		// go through all the files in the directory
		for (File file : dir.listFiles()) {
			if (file.isFile())
				indexCollectionFile(file);
		}
	}

}
