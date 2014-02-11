package de.ids_mannheim.korap.search;

import java.util.*;
import java.io.*;

import de.ids_mannheim.korap.KorapSearch;
import de.ids_mannheim.korap.KorapQuery;
import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapFilter;
import de.ids_mannheim.korap.KorapResult;
import java.nio.file.Files;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestKorapSearch {
    @Test
    public void searchCount () {
	KorapSearch ks = new KorapSearch(
	    new KorapQuery("field1").seg("a").with("b")
        );
	// Count:
	ks.setCount(30);
	assertEquals(ks.getCount(), 30);
	ks.setCount(20);
	assertEquals(ks.getCount(), 20);
	ks.setCount(-50);
	assertEquals(ks.getCount(), 20);
	ks.setCount(500);
	assertEquals(ks.getCount(), ks.getCountMax());
    };

    @Test
    public void searchStartIndex () {
	KorapSearch ks = new KorapSearch(
	    new KorapQuery("field1").seg("a").with("b")
        );
	// startIndex
	ks.setStartIndex(5);
	assertEquals(ks.getStartIndex(), 5);
	ks.setStartIndex(1);
	assertEquals(ks.getStartIndex(), 1);
	ks.setStartIndex(0);
	assertEquals(ks.getStartIndex(), 0);
	ks.setStartIndex(70);
	assertEquals(ks.getStartIndex(), 70);
	ks.setStartIndex(-5);
	assertEquals(ks.getStartIndex(), 0);
    };

    @Test
    public void searchQuery () {
	KorapSearch ks = new KorapSearch(
	    new KorapQuery("field1").seg("a").with("b")
        );
	// query
	assertEquals(ks.getQuery().toString(), "spanSegment(field1:a, field1:b)");
    };

    @Test
    public void searchIndex () throws IOException {

	// Construct index
	KorapIndex ki = new KorapIndex();
	// Indexing test files
	for (String i : new String[] {"00001", "00002", "00003", "00004", "00005", "00006", "02439"}) {
	    ki.addDocFile(
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	KorapSearch ks = new KorapSearch(
	    new KorapQuery("tokens").seg("s:Buchstaben")
	);
	ks.getCollection().filter(
            new KorapFilter().and("textClass", "reisen")
        );
	ks.setCount(3);
	ks.setStartIndex(5);
	ks.leftContext.setLength(1);
	ks.rightContext.setLength(1);
	KorapResult kr = ks.run(ki);
	assertEquals(6, kr.totalResults());
	assertEquals(kr.getMatch(0).getSnippetBrackets(), "... dem [Buchstaben] A ...");
    };

    @Test
    public void searchJSON () throws IOException {

	// Construct index
	KorapIndex ki = new KorapIndex();
	// Indexing test files
	for (String i : new String[] {"00001", "00002", "00003", "00004", "00005", "00006", "02439"}) {
	    ki.addDocFile(
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	String json = getString(getClass().getResource("/queries/metaquery3.json").getFile());

	KorapResult kr = new KorapSearch(json).run(ki);
	
	//System.out.println(kr.getQuery().toString());	
	
	assertEquals(66, kr.getTotalResults());
	assertEquals(5, kr.getItemsPerPage());
	assertEquals(5, kr.getStartIndex());
	assertEquals("... a: A ist [der klangreichste] der V ...", kr.getMatch(0).getSnippetBrackets());
    };

    @Test
    public void searchJSON2 () throws IOException {

	// Construct index
	KorapIndex ki = new KorapIndex();
	// Indexing test files
	for (String i : new String[] {"00001", "00002", "00003", "00004", "00005", "00006", "02035-substring", "02439", "05663-unbalanced", "07452-deep"}) {
	    ki.addDocFile(
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	String json = getString(getClass().getResource("/queries/metaquery4.json").getFile());

	KorapSearch ks = new KorapSearch(json);
	KorapResult kr = ks.run(ki);
	assertEquals(2, kr.getTotalResults());

	json = getString(getClass().getResource("/queries/metaquery5.json").getFile());
	ks = new KorapSearch(json);
	kr = ks.run(ki);
	assertEquals(2, kr.getTotalResults());

	json = getString(getClass().getResource("/queries/metaquery6.json").getFile());
	ks = new KorapSearch(json);
	kr = ks.run(ki);
	assertEquals(1, kr.getTotalResults());
    };


    @Test
    public void searchJSONFailure () throws IOException {

	// Construct index
	KorapIndex ki = new KorapIndex();
	// Indexing test files
	for (String i : new String[] {"00001", "00002", "00003", "00004", "00005", "00006", "02439"}) {
	    ki.addDocFile(
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	KorapResult kr = new KorapSearch("{ query").run(ki);

	assertEquals(0, kr.getTotalResults());
	assertNotNull(kr.getError());
    };



    @Test
    public void searchJSONindexboundary () throws IOException {

	// Construct index
	KorapIndex ki = new KorapIndex();
	// Indexing test files
	for (String i : new String[] {"00001", "00002", "00003", "00004", "00005", "00006", "02439"}) {
	    ki.addDocFile(
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	String json = getString(getClass().getResource("/queries/bsp-fail1.json").getFile());

	KorapResult kr = new KorapSearch(json).run(ki);
	assertEquals(0, kr.getStartIndex());
	assertEquals(0, kr.getTotalResults());
	assertEquals(25, kr.getItemsPerPage());
    };

    @Test
    public void searchJSONindexboundary2 () throws IOException {

	// Construct index
	KorapIndex ki = new KorapIndex();
	// Indexing test files
	for (String i : new String[] {"00001", "00002", "00003", "00004", "00005", "00006", "02439"}) {
	    ki.addDocFile(
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	String json = getString(getClass().getResource("/queries/bsp-fail2.json").getFile());

	KorapResult kr = new KorapSearch(json).run(ki);
	assertEquals(50, kr.getItemsPerPage());
	assertEquals(49950, kr.getStartIndex());
	assertEquals(0, kr.getTotalResults());
    };


    @Test
    public void searchJSONcontext () throws IOException {

	// Construct index
	KorapIndex ki = new KorapIndex();
	// Indexing test files
	for (String i : new String[] {"00001", "00002", "00003", "00004", "00005", "00006", "02439"}) {
	    ki.addDocFile(
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	String json = getString(getClass().getResource("/queries/bsp-context.json").getFile());

	KorapSearch ks = new KorapSearch(json);
	KorapResult kr = ks.run(ki);
	assertEquals(10, kr.getTotalResults());
	assertEquals("A bzw. a ist der erste Buchstabe des lateinischen [Alphabets] und ein Vokal. Der Buchstabe A hat in deutschen Texten eine durchschnittliche Häufigkeit  ...", kr.getMatch(0).getSnippetBrackets());

	ks.setCount(5);
	ks.setStartPage(2);
	kr = ks.run(ki);
	assertEquals(10, kr.getTotalResults());
	assertEquals(5, kr.getStartIndex());
	assertEquals(5, kr.getItemsPerPage());


	json = getString(getClass().getResource("/queries/bsp-context-2.json").getFile());

	kr = new KorapSearch(json).run(ki);
	assertEquals(-1, kr.getTotalResults());
	assertEquals("... lls seit den Griechen beibehalten worden. 3. Bedeutungen in der Biologie steht A für das Nukleosid Adenosin steht A die Base Adenin steht A für die Aminosäure Alanin in der Informatik steht a für den dezimalen [Wert] 97 sowohl im ASCII- als auch im Unicode-Zeichensatz steht A für den dezimalen Wert 65 sowohl im ASCII- als auch im Unicode-Zeichensatz als Kfz-Kennzeichen steht A in Deutschland für Augsburg. in Österreich auf ...", kr.getMatch(0).getSnippetBrackets());
    };

    @Test
    public void searchJSONstartPage () throws IOException {

	// Construct index
	KorapIndex ki = new KorapIndex();
	// Indexing test files
	for (String i : new String[] {"00001", "00002", "00003", "00004", "00005", "00006", "02439"}) {
	    ki.addDocFile(
	      getClass().getResource("/wiki/" + i + ".json.gz").getFile(), true
            );
	};
	ki.commit();

	String json = getString(getClass().getResource("/queries/bsp-paging.json").getFile());

	KorapSearch ks = new KorapSearch(json);
	KorapResult kr = ks.run(ki);
	assertEquals(10, kr.getTotalResults());
	assertEquals(5, kr.getStartIndex());
	assertEquals(5, kr.getItemsPerPage());

	json = getString(getClass().getResource("/queries/bsp-cutoff.json").getFile());
	ks = ks = new KorapSearch(json);

	kr = ks.run(ki);
	assertEquals(-1, kr.getTotalResults());
	assertEquals(2, kr.getStartIndex());
	assertEquals(2, kr.getItemsPerPage());

    };


    public static String getString (String path) {
	StringBuilder contentBuilder = new StringBuilder();
	try {
	    BufferedReader in = new BufferedReader(new FileReader(path));
	    String str;
	    while ((str = in.readLine()) != null) {
		contentBuilder.append(str);
	    };
	    in.close();
	} catch (IOException e) {
	    fail(e.getMessage());
	}
	return contentBuilder.toString();
    };
};
