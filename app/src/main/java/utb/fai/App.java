/*
 * This source file was generated by the Gradle 'init' task
 */
package utb.fai;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.swing.text.html.parser.ParserDelegator;

public class App {

	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("Missing parameters - start URL, debug level, and max depth");
			return;
		}
		LinkedList<URIinfo> foundURIs = new LinkedList<>();
		HashSet<URI> visitedURIs = new HashSet<>();
		HashMap<String, Integer> wordsCount = new HashMap<>();
		int debugLevel = Integer.parseInt(args[1]);
		int maxDepth = Integer.parseInt(args[2]);

		URI uri;
		try {
			//uri = new URI(args[0] + "/");
			uri = new URI(args[0] + "/");
			foundURIs.add(new URIinfo(uri, 0)); // množina nalezených URI
			visitedURIs.add(uri); // všechny již navštívené URI, ignoruju pokud již byla navštívena
			/**
			 * Zde zpracujte dalí parametry - maxDepth a debugLevel
			 */

			ParserCallback callBack = new ParserCallback(visitedURIs, foundURIs,wordsCount,maxDepth,debugLevel); // callback pro zpracování nalezených URI
			ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

			while (!foundURIs.isEmpty()) {
				URIinfo URIinfo = foundURIs.removeFirst();

				callBack.depth = URIinfo.depth; // kontroluje hloubku procházení
				callBack.pageURI = uri = URIinfo.uri;
				System.err.println("Analyzing " + uri);

				URI finalUri = uri;
				executor.submit(() -> {
					try {
						Document doc = Jsoup.connect(finalUri.toString()).timeout(10000).get(); // Increased timeout to 10 seconds
						callBack.parseDocument(doc);
					} catch (Exception e) {
						System.err.println("Error loading page: " + e.getMessage());
					}
				});
			}

			executor.shutdown();
			while (!executor.isTerminated()) {
			}

			callBack.printWordsCount();

		} catch (URISyntaxException e) {
			System.err.println("Invalid URI: " + args[0]);
		} catch (Exception e) {
			System.err.println("Zachycena neoetøená výjimka, konèíme...");
			e.printStackTrace();
		}
	}

}
