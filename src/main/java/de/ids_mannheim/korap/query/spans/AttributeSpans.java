package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanAttributeQuery;
import org.apache.lucene.search.spans.TermSpans;

/**
 * Span enumeration of attributes which are term spans with special payload
 * assignment referring to another span (e.g. element/relation span) to which an
 * attribute span belongs.
 * 
 * The class is basically a wrapper of Lucene {@link TermSpans} with additional
 * functionality regarding element/relation reference. Element/relation id is
 * annotated ascendingly starting from the left side.
 * 
 * The enumeration is ordered firstly by the start position of the attribute and
 * secondly by the element/relation id descendingly. This order helps to match
 * element and attributes faster. 
 * 
 * @author margaretha
 * */
public class AttributeSpans extends SimpleSpans {

    private List<CandidateAttributeSpan> candidateList;
    private int currentDoc, currentPosition;
    private short spanId;
    private boolean isFinish;
    private int elementEnd;

    protected Logger logger = LoggerFactory.getLogger(AttributeSpans.class);

    /**
     * Constructs Attributespans based on the specified SpanAttributeQuery.
     * 
     * @param spanAttributeQuery a spanAttributeQuery
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @throws IOException
     */
    public AttributeSpans(SpanAttributeQuery spanAttributeQuery,
            AtomicReaderContext context, Bits acceptDocs,
            Map<Term, TermContext> termContexts) throws IOException {
        super(spanAttributeQuery, context, acceptDocs, termContexts);
        candidateList = new ArrayList<>();
        hasMoreSpans = firstSpans.next();
        if (hasMoreSpans) {
            currentDoc = firstSpans.doc();
            currentPosition = firstSpans.start();
        }
    }

    @Override
    public boolean next() throws IOException {
        isStartEnumeration = false;
        matchPayload.clear();
        return advance();
    }

    /**
     * Moves to the next match by checking the candidate match list or setting
     * the list first when it is empty.
     * 
     * @return true if a match is found
     * @throws IOException
     */
    private boolean advance() throws IOException {
        while (hasMoreSpans || !candidateList.isEmpty()) {
            if (!candidateList.isEmpty()) {
                // set the current match from the first CandidateAttributeSpan
                // in the candidate list
                CandidateAttributeSpan cs = candidateList.get(0);
                this.matchDocNumber = cs.getDoc();
                this.matchStartPosition = cs.getStart();
                this.matchEndPosition = cs.getEnd();
                this.setSpanId(cs.getSpanId());
                this.setElementEnd(cs.getElementEnd());
                candidateList.remove(0);
                return true;
            } else {
                setCandidateList();
                currentDoc = firstSpans.doc();
                currentPosition = firstSpans.start();
            }
        }
        return false;
    }

    /**
     * Collects all the attributes in the same start position and sort them by
     * element/relation Id in a reverse order (the ones with the bigger
     * element/relation Id first).
     * 
     * @throws IOException
     */
    private void setCandidateList() throws IOException {

        while (hasMoreSpans && firstSpans.doc() == currentDoc
                && firstSpans.start() == currentPosition) {

            candidateList.add(createCandidateSpan());
            hasMoreSpans = firstSpans.next();
        }

        Collections.sort(candidateList);
        Collections.reverse(candidateList);
    }

    /**
     * Creates a CandidateAttributeSpan based on the child span and set the
     * spanId and elementEnd from its payloads.
     * 
     * @param firstSpans an AttributeSpans
     * @return a CandidateAttributeSpan
     * @throws IOException
     */
    private CandidateAttributeSpan createCandidateSpan() throws IOException {
        List<byte[]> payload = (List<byte[]>) firstSpans.getPayload();
        ByteBuffer wrapper = ByteBuffer.wrap(payload.get(0));

        short spanId = wrapper.getShort(0);
        int elementEnd = -1;
        if (payload.get(0).length == 6) {
            elementEnd = wrapper.getInt(2);
        }
        return new CandidateAttributeSpan(firstSpans, spanId, elementEnd);
    }

    /**
     * Return the span id to which an attribute span belongs, for instance a
     * relation id or an element id.
     * 
     * @return a span id, for instance a relation id or an element id
     */
    public short getSpanId() {
        return this.spanId;
    }

    /**
     * Sets the span id to which an attribute span belongs, for instance a
     * relation id or an element id.
     * 
     * @param spanId the span id to which an attribute span belongs, for
     *        instance a relation id or an element id.
     */
    public void setSpanId(short spanId) {
        this.spanId = spanId;
    }

    /**
     * Returns the end position of the element to which an attribute span
     * belongs.
     * 
     * @return an element end position
     */
    public int getElementEnd() {
        return elementEnd;
    }

    /**
     * Sets the end position of the element to which an attribute span belongs.
     * 
     * @param elementEnd the end position of the element to which an attribute
     *        span belongs.
     */
    public void setElementEnd(int elementEnd) {
        this.elementEnd = elementEnd;
    }

    /**
     * Tells if the enumeration of the AttributeSpans has come to an end.
     * 
     * @return true if the enumeration has finished.
     */
    public boolean isFinish() {
        return isFinish;
    }

    /**
     * Sets true if the enumeration of the AttributeSpans has come to an end.
     * 
     * @param isFinish <code>true</code> if the enumeration of the
     *        AttributeSpans has come to an end, <code>false</code> otherwise.
     */
    public void setFinish(boolean isFinish) {
        this.isFinish = isFinish;
    }

    @Override
    public boolean skipTo(int target) throws IOException {
        if (hasMoreSpans && (firstSpans.doc() < target)) {
            if (!firstSpans.skipTo(target)) {
                candidateList.clear();
                return false;
            }
        }
        setCandidateList();
        matchPayload.clear();
        isStartEnumeration = false;
        return advance();
    }

    @Override
    public long cost() {
        return firstSpans.cost();
    }

    /**
     * CandidateAttributeSpan contains information about an Attribute span. All
     * attribute spans occurring in an identical position are collected as
     * CandidateAttributeSpans. The list of these CandidateAttributeSpans are
     * sorted based on the span ids to which the attributes belong to. The
     * attributes with smaller spanIds come first on the list.
     * 
     * */
    class CandidateAttributeSpan extends CandidateSpans implements
            Comparable<CandidateSpans> {

        private short spanId;
        private int elementEnd;

        /**
         * Construct a CandidateAttributeSpan based on the given span, spanId,
         * and elementEnd.
         * 
         * @param span an AttributeSpans
         * @param spanId the element or relation span id to which the current
         *        state of the specified AttributeSpans belongs to.
         * @param elementEnd the end position of the element or relation span to
         *        which the current state of the specified AttributeSpans
         *        belongs to.
         * @throws IOException
         */
        public CandidateAttributeSpan(Spans span, short spanId, int elementEnd)
                throws IOException {
            super(span);
            setSpanId(spanId);
            setElementEnd(elementEnd);
        }

        public void setSpanId(short spanId) {
            this.spanId = spanId;
        }

        public short getSpanId() {
            return spanId;
        }

        public int getElementEnd() {
            return elementEnd;
        }

        public void setElementEnd(int elementEnd) {
            this.elementEnd = elementEnd;
        }

        @Override
        public int compareTo(CandidateSpans o) {
            CandidateAttributeSpan cs = (CandidateAttributeSpan) o;
            if (this.spanId == cs.spanId)
                return 0;
            else if (this.spanId > cs.spanId)
                return 1;
            return -1;
        }
    }
}
