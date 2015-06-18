package de.ids_mannheim.korap.query;

import static de.ids_mannheim.korap.TestSimple.getJSONQuery;
import static org.junit.Assert.assertEquals;

import org.apache.lucene.search.spans.SpanQuery;
import org.junit.Test;

import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.util.QueryException;

public class TestSpanRelationQueryJSON {

    @Test
    public void testMatchRelationSource () throws QueryException {
        //
        String filepath = getClass().getResource(
                "/queries/relation/match-source.json").getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(#[1,2]spanSegment(spanRelation(tokens:>:mate/d:HEAD), <tokens:c:s />))",
                sq.toString());
    }


    @Test
    public void testMatchRelationTarget () throws QueryException {
        //
        String filepath = getClass().getResource(
                "/queries/relation/match-target.json").getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(#[1,2]spanSegment(spanRelation(tokens:<:mate/d:HEAD), <tokens:c:vp />))",
                sq.toString());
    }


    @Test
    public void testMatchRelationSourceAndTarget () throws QueryException {
        //
        String filepath = getClass().getResource(
                "/queries/relation/match-source-and-target.json").getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(#[1,2]spanSegment(focus(#2: spanSegment(spanRelation(tokens:>:mate/d:HEAD), <tokens:c:s />)), <tokens:c:vp />))",
                sq.toString());
    }


    @Test
    public void testMatchOperandWithProperty () throws QueryException {
        //
        String filepath = getClass().getResource(
                "/queries/relation/operand-with-property.json").getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(#[1,2]spanSegment(focus(#2: spanSegment(spanRelation(tokens:>:mate/d:HEAD), "
                        + "spanElementWithAttribute(<tokens:c:s />, spanAttribute(tokens:@root)))), <tokens:c:vp />))",
                sq.toString());
    }


    @Test
    public void testMatchOperandWithAttribute () throws QueryException {
        //
        String filepath = getClass().getResource(
                "/queries/relation/operand-with-attribute.json").getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(#[1,2]spanSegment(focus(#2: spanSegment(spanRelation(tokens:>:mate/d:HEAD), "
                        + "spanElementWithAttribute(<tokens:c:s />, spanAttribute(tokens:type:top)))), <tokens:c:vp />))",
                sq.toString());
    }


    @Test
    public void testMatchRelationOnly () throws QueryException {
        //
        String filepath = getClass().getResource(
                "/queries/relation/relation-only.json").getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals("focus(#[1,2]spanRelation(tokens:>:mate/d:HEAD))",
                sq.toString());
    }

    @Test
    public void testFocusSource () throws QueryException {
        //
        String filepath = getClass().getResource(
                "/queries/relation/focus-source.json").getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(1: focus(#[1,2]spanSegment(spanRelation(tokens:<:mate/d:HEAD), {1: <tokens:c:np />})))",
                sq.toString());
    }

    @Test
    public void testFocusTarget() throws QueryException {
        String filepath = getClass().getResource(
                "/queries/relation/focus-target.json").getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(2: focus(#[1,2]spanSegment(focus(#2: spanSegment(spanRelation("
                        + "tokens:>:mate/d:HEAD), {1: <tokens:c:s />})), {2: <tokens:c:np />})))",
                sq.toString());
    }

    @Test
    public void testFocusEmptyTarget() throws QueryException {
        String filepath = getClass().getResource(
                "/queries/relation/focus-empty-target.json").getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(2: focus(#[1,2]spanSegment({2: target:spanRelation(tokens:>:mate/d:HEAD)}, {1: <tokens:c:s />})))",
                sq.toString());
    }

    @Test
    public void testFocusEmptyBoth() throws QueryException {
        String filepath = getClass().getResource(
                "/queries/relation/focus-empty-both.json").getFile();
        SpanQueryWrapper sqwi = getJSONQuery(filepath);
        SpanQuery sq = sqwi.toQuery();
        assertEquals(
                "focus(2: focus(#[1,2]{1: source:{2: target:spanRelation(tokens:>:mate/d:HEAD)}}))",
                sq.toString());
    }
}