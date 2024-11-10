package utb.fai;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Třída ParserCallback je používána parserem DocumentParser,
 * je implementován přímo v JDK a umí parsovat HTML do verze 3.0.
 * Při parsování (analýze) HTML stránky volá tento parser
 * jednotlivé metody třídy ParserCallback, co nám umožuje
 * provádět s částmi HTML stránky naše vlastní akce.
 * 
 * @author Tomá Dulík
 */
class ParserCallback extends HTMLEditorKit.ParserCallback {

    /**
     * pageURI bude obsahovat URI aktuální parsované stránky. Budeme
     * jej vyuívat pro resolving všech URL, které v kódu stránky najdeme
     * - předtím, než nalezené URL uložíme do foundURLs, musíme z něj udělat
     * absolutní URL!
     */
    URI pageURI;

    /**
     * depth bude obsahovat aktuální hloubku zanoření
     */
    int depth, maxDepth;

    /**
     * visitedURLs je množina všech URL, které jsme již navtívili
     * (parsovali). Pokud najdeme na stránce URL, který je v této množině,
     * nebudeme jej u dále parsovat
     */
    HashSet<URI> visitedURIs;

    /**
     * foundURLs jsou všechna nová (zatím nenavštívená) URL, která na stránce
     * najdeme. Poté, co projdeme celou stránku, budeme z tohoto seznamu
     * jednotlivá URL brát a zpracovávat.
     */
    LinkedList<URIinfo> foundURIs;

    /** pokud debugLevel>1, budeme vypisovat debugovací hlášky na std. error */
    int debugLevel = 0;
    HashMap<String, Integer> wordsCount = new HashMap<>();


    ParserCallback(HashSet<URI> visitedURIs, LinkedList<URIinfo> foundURIs,  HashMap<String, Integer> wordsCount, int maxDepth, int debugLevel) {
        this.foundURIs = foundURIs;
        this.visitedURIs = visitedURIs;
        this.debugLevel = debugLevel;
        this.wordsCount = wordsCount;
        this.maxDepth = maxDepth;
    }

    public void parseDocument(Document doc) {
        // Extract and count words from the document
        String text = doc.body().text();
        String[] words = text.split("\\W+");
        for (String word : words) {
            wordsCount.put(word, wordsCount.getOrDefault(word, 0) + 1);
        }

        // Find and process links if depth allows
        if (depth < maxDepth) {
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                String href = link.attr("abs:href");
                try {
                    URI linkUri = new URI(href);
                    if (!visitedURIs.contains(linkUri)) {
                        visitedURIs.add(linkUri);
                        foundURIs.add(new URIinfo(linkUri, depth + 1));
                    }
                } catch (URISyntaxException e) {
                    System.err.println("Invalid link URI: " + href);
                }
            }
        }
    }
    /**
     * metoda handleSimpleTag se volá např. u značky <FRAME>
     */
    public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
        handleStartTag(t, a, pos);
    }

    public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
        URI uri;
        String href = null;
        if (debugLevel > 1)
            System.err.println("handleStartTag: " + t.toString() + ", pos=" + pos + ", attribs=" + a.toString());
        if (depth <= maxDepth) // kontrola zanoření
            if (t == HTML.Tag.A)
                href = (String) a.getAttribute(HTML.Attribute.HREF);
            else if (t == HTML.Tag.FRAME)
                href = (String) a.getAttribute(HTML.Attribute.SRC);
        if (href != null)
            try {
                uri = pageURI.resolve(href);
                if (!uri.isOpaque() && !visitedURIs.contains(uri)) { //opaqeu - mail, tools...
                    visitedURIs.add(uri);
                    foundURIs.add(new URIinfo(uri, depth + 1));
                    if (debugLevel > 0)
                        System.err.println("Adding URI: " + uri.toString());
                }
            } catch (Exception e) {
                System.err.println("Nalezeno nekorektní URI: " + href);
                e.printStackTrace();
            }

    }

    /******************************************************************
     * V metodě handleText bude probíhat veškerá činnost, související se
     * zjiováním četnosti slov v textovém obsahu HTML stránek.
     * IMPLEMENTACE TÉTO METODY JE V TÉTO ÚLOZE VAŠÍM ÚKOLEM !!!!
     * Možný postup:
     * Ve třídě Parser (klidně v její metodě main) si vytvořte vyhledávací tabulku
     * =instanci třídy HashMap<String,Integer> nebo TreeMap<String,Integer>.
     * Do této tabulky si ukládejte dvojice klíč-data, kde
     * klíčem jsou jednotlivá slova z textového obsahu HTML stránek,
     * data typu Integer bude dosavadní počet výskytu daného slova v
     * HTML stránkách.
     *******************************************************************/
    public void handleText(char[] data, int pos) {
       //System.out.println("handleText: " + String.valueOf(data) + ", pos=" + pos);
        String text = String.valueOf(data);
        //System.out.println(text);
        String[] words = text.split("\\W+");
        for (String word : words) {
            if (!word.isEmpty()) {
                word = word.toLowerCase();
                wordsCount.put(word, wordsCount.getOrDefault(word, 0) + 1);
            }
        }

        //hahs mapa -> list -> sort -> print
        /**
         * ...tady bude vaše implementace...
         */
    }
    public void printWordsCount() {
        List<Map.Entry<String, Integer>> sorted = wordsCount.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(20)
                .collect(Collectors.toList());
        for (Map.Entry<String, Integer> entry : sorted) {
            System.out.println(entry.getKey() + ";" + entry.getValue());
        }
    }
}
