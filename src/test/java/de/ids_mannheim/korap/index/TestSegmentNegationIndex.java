package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import org.apache.lucene.search.spans.SpanQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;

import de.ids_mannheim.korap.Krill;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.query.wrap.SpanSegmentQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanSequenceQueryWrapper;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.response.Result;

@RunWith(JUnit4.class)
public class TestSegmentNegationIndex {
    private SpanQuery sq;
    private KrillIndex ki;
    private Result kr;
    private FieldDocument fd;
    private Logger log;


    @Test
    public void testcaseNegation () throws Exception {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc2());
        ki.addDoc(createFieldDoc3());
        ki.commit();
        SpanSegmentQueryWrapper ssqw = new SpanSegmentQueryWrapper("tokens",
                "s:b");
        ssqw.with("s:c");
        SpanSequenceQueryWrapper sqw = new SpanSequenceQueryWrapper("tokens",
                ssqw).append("s:d");

        kr = ki.search(sqw.toQuery(), (short) 10);

        assertEquals("totalResults", kr.getTotalResults(), 2);
        // Match #0
        assertEquals("doc-number", 0, kr.getMatch(0).getLocalDocID());
        assertEquals("StartPos (0)", 4, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 6, kr.getMatch(0).endPos);

        // Match #1 in the other atomic index
        assertEquals("doc-number", 3, kr.getMatch(1).getLocalDocID());
        assertEquals("StartPos (0)", 0, kr.getMatch(1).startPos);
        assertEquals("EndPos (0)", 2, kr.getMatch(1).endPos);

        ssqw = new SpanSegmentQueryWrapper("tokens", "s:b");
        ssqw.without("s:c");
        sqw = new SpanSequenceQueryWrapper("tokens", ssqw).append("s:a");

        kr = ki.search(sqw.toQuery(), (short) 10);

        assertEquals("doc-number", 0, kr.getMatch(0).getLocalDocID());
        assertEquals("StartPos (0)", 2, kr.getMatch(0).startPos);
        assertEquals("EndPos (0)", 4, kr.getMatch(0).endPos);

        assertEquals("doc-number", 1, kr.getMatch(1).getLocalDocID());
        assertEquals("StartPos (1)", 1, kr.getMatch(1).startPos);
        assertEquals("EndPos (1)", 3, kr.getMatch(1).endPos);

        assertEquals("doc-number", 1, kr.getMatch(2).getLocalDocID());
        assertEquals("StartPos (2)", 2, kr.getMatch(2).startPos);
        assertEquals("EndPos (2)", 4, kr.getMatch(2).endPos);

        assertEquals("doc-number", 2, kr.getMatch(3).getLocalDocID());
        assertEquals("StartPos (3)", 1, kr.getMatch(3).startPos);
        assertEquals("EndPos (3)", 3, kr.getMatch(3).endPos);
    }


    @Test
    public void testcaseWarnings () throws Exception {
        ki = new KrillIndex();
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc2());
        ki.addDoc(createFieldDoc3());
        ki.commit();

        kr = ki.search(new Krill(
                "{\"query\" : { \"@type\" : \"koral:token\", \"wrap\" : { \"@type\" : \"koral:term\", \"key\" : \"a\", \"flags\" : [\"caseInsensitive\"], \"layer\" : \"orth\", \"match\" : \"match:eq\" }}}"));
        assertEquals("totalResults", kr.getTotalResults(), 6);
        assertEquals("Warning", kr.hasWarnings(), true);
        assertEquals("Warning text", kr.getWarning(0).getMessage(),
                "Flag is unknown");
        assertEquals("Warning text", kr.getWarning(0).toJsonString(),
                "[748,\"Flag is unknown\",\"caseInsensitive\"]");

        // Negation of segment
        kr = ki.search(new Krill(
                "{\"query\" : { \"@type\" : \"koral:token\", \"wrap\" : { \"@type\" : \"koral:term\", \"key\" : \"a\", \"flags\" : [\"flags:caseInsensitive\"], \"layer\" : \"orth\", \"match\" : \"match:ne\" }}}"));

        assertEquals("totalResults", kr.getTotalResults(), 4);
        assertEquals("Warning", kr.hasWarnings(), true);
        assertEquals("Warning text", kr.getWarning(0).getMessage(),
                "Exclusivity of query is ignored");

        // Flag parameter injection
        kr = ki.search(new Krill(
                "{\"query\" : { \"@type\" : \"koral:token\", \"wrap\" : { \"@type\" : \"koral:term\", \"key\" : \"a\", \"flags\" : [{ \"injection\" : true }], \"layer\" : \"orth\", \"match\" : \"match:ne\" }}}"));

        assertEquals("totalResults", kr.getTotalResults(), 6);
        assertEquals("Warning", kr.hasWarnings(), true);
        assertEquals("Warning text", kr.getWarning(0).getMessage(),
                "Flag is unknown");
        assertEquals("Warning text", kr.getWarning(0).toJsonString(),
                "[748,\"Flag is unknown\"]");
    };


    private FieldDocument createFieldDoc0 () {
        fd = new FieldDocument();
        fd.addString("ID", "doc-0");
        fd.addTV("tokens", "bcbabd", "[(0-1)s:b|i:b|_1$<i>0<i>1]"
                + "[(1-2)s:c|i:c|s:b|_2$<i>1<i>2]"
                + "[(2-3)s:b|i:b|_3$<i>2<i>3|<>:e$<b>64<i>2<i>4<i>4<b>0]"
                + "[(3-4)s:a|i:a|_4$<i>3<i>4|<>:e$<b>64<i>3<i>5<i>5<b>0|"
                + "<>:e2$<b>64<i>3<i>5<i>5<b>0]"
                + "[(4-5)s:b|i:b|s:c|_5$<i>4<i>5]"
                + "[(5-6)s:d|i:d|_6$<i>5<i>6|<>:e2$<b>64<i>5<i>6<i>6<b>0]");
        return fd;
    }


    private FieldDocument createFieldDoc1 () {
        fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV("tokens", "babaa", "[(0-1)s:b|i:b|s:c|_1$<i>0<i>1]"
                + "[(1-2)s:a|i:a|s:b|_2$<i>1<i>2|<>:e$<b>64<i>1<i>3<i>3<b>0]"
                + "[(2-3)s:b|i:b|s:a|_3$<i>2<i>3]"
                + "[(3-4)s:a|i:a|_4$<i>3<i>4]" + "[(4-5)s:a|i:a|_5$<i>4<i>5]");
        return fd;
    }


    private FieldDocument createFieldDoc2 () {
        fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addTV("tokens", "bdb",
                "[(0-1)s:b|i:b|_1$<i>0<i>1]" + "[(1-2)s:d|i:d|s:b|_2$<i>1<i>2]"
                        + "[(2-3)s:b|i:b|s:a|_3$<i>2<i>3]");
        return fd;
    }


    private FieldDocument createFieldDoc3 () {
        fd = new FieldDocument();
        fd.addString("ID", "doc-3");
        fd.addTV("tokens", "bdb", "[(0-1)s:b|i:b|s:c|_1$<i>0<i>1]"
                + "[(1-2)s:d|_2$<i>1<i>2]" + "[(2-3)s:d|i:d|_3$<i>2<i>3]");
        return fd;
    }
}
