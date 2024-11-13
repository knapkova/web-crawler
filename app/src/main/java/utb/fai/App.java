package utb.fai;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

public class App {
	private final ConcurrentHashMap<String, Boolean> visitedURIs = new ConcurrentHashMap<>();
	private final Map<String, Integer> wordFrequency = new ConcurrentHashMap<>();
	private final ExecutorService executor = Executors.newFixedThreadPool(10);
	private final int maxDepth;
	private final int debugDepth;

	public App(int maxDepth, int debugDepth) {
		this.maxDepth = maxDepth;
		this.debugDepth = debugDepth;
	}

	public void startCrawling(String startURL) {
		crawl(startURL, 0, debugDepth);
		executor.shutdown();
		try {
			executor.awaitTermination(1, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		printTopWords();
	}

	private void crawl(String url, int depth, int debugDepth) {
		if (depth > maxDepth || visitedURIs.putIfAbsent(url, true) != null) {
			return;
		}

		if (debugDepth == 1) {
			System.err.println("Crawling URL: " + url + " at depth: " + depth);
		}

		try {
			Document doc = Jsoup.connect(url).get();
			Elements scripts = doc.select("script, style");
			scripts.forEach(Element::remove);
			String bodyText = doc.body().text();

			synchronized (this) {
				analyzeText(bodyText);
			}

			Elements links = doc.select("a[href]");
			for (Element link : links) {
				String absUrl = link.absUrl("href");
				if (!absUrl.isEmpty()) {
					executor.submit(() -> crawl(absUrl, depth + 1, debugDepth));
				}
			}
		} catch (IOException e) {
			System.err.println("Failed to fetch URL: " + url + " - " + e.getMessage());
		}
	}

	private void analyzeText(String text) {
		String[] words = text.split("\\s+");
		for (String word : words) {
			if (!word.isEmpty()) {
				wordFrequency.merge(word, 1, Integer::sum);
			}
		}
	}


	private void printTopWords() {
		wordFrequency.entrySet().stream()
				.sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
				.limit(20)
				.forEach(entry -> System.out.println(entry.getKey() + ";" + entry.getValue()));
	}

	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("not enough arguments -> url maxDepth debugDepth");
			return;
		}

		String startURL = args[0];
		int maxDepth =  Integer.parseInt(args[1]);
		int debugDepth =Integer.parseInt(args[2]);

		App crawler = new App(maxDepth, debugDepth);
		crawler.startCrawling(startURL);
	}
}