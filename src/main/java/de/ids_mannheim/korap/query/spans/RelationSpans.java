package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.TermSpans;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanRelationQuery;

/**
 * Enumeration of spans denoting relations between two
 * tokens/elements. The start and end of a RelationSpan always denote
 * the start and end of the left-side token/element.
 * 
 * There are 4 types of relations, which is differentiated by the
 * payload length in bytes.
 * <ol>
 * <li>Token to token relation (1 int & 3 short, length: 10)</li>
 * <li>Token to span (2 int & 3 short, length: 14)</li>
 * <li>Span to token (int, byte, int, 3 short, length: 15)</li>
 * <li>Span to Span (3 int & 3 short, length: 18)</li>
 * </ol>
 * Every integer value denotes the start/end position of the
 * start/target of a relation, in this format: (sourceEndPos?,
 * startTargetPos, endTargetPos?). The end position of a token is
 * identical to its start position, and therefore not is saved in a
 * payload.
 * 
 * The short values denote the relation id, left id, and right id. The
 * byte in relation #3 is just a dummy to create a different length
 * from the relation #2.
 * 
 * NOTE: Sorting of the candidate spans can alternatively be done in
 * indexing, instead of here. (first by left positions and then by
 * right positions)
 * 
 * The class number of relation source is always 1 and that of
 * relation target is always 2 regardless of the relation direction.
 * 
 * @author margaretha
 * */
public class RelationSpans extends RelationBaseSpans {

    private int currentDoc, currentPosition;
    private int direction;
    private TermSpans relationTermSpan;

    protected Logger logger = LoggerFactory.getLogger(RelationSpans.class);
    private List<CandidateSpan> candidateList;
    private byte tempSourceNum, tempTargetNum;
    private byte sourceClass, targetClass;

    public static enum PayloadTypeIdentifier {
        TERM_TO_TERM(32), TERM_TO_ELEMENT(33), ELEMENT_TO_TERM(34), ELEMENT_TO_ELEMENT(
                35);

        private byte value;


        private PayloadTypeIdentifier (int value) {
            this.value = (byte) value;
        }
    }


    /**
     * Constructs RelationSpans from the given
     * {@link SpanRelationQuery}.
     * 
     * @param relationSpanQuery
     *            a SpanRelationQuery
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @throws IOException
     */
    public RelationSpans (SpanRelationQuery relationSpanQuery,
                          LeafReaderContext context, Bits acceptDocs,
                          Map<Term, TermContext> termContexts)
            throws IOException {
        super(relationSpanQuery, context, acceptDocs, termContexts);
        direction = relationSpanQuery.getDirection();
        tempSourceNum = relationSpanQuery.getTempSourceNum();
        tempTargetNum = relationSpanQuery.getTempTargetNum();
        sourceClass = relationSpanQuery.getSourceClass();
        targetClass = relationSpanQuery.getTargetClass();

        candidateList = new ArrayList<>();
        relationTermSpan = (TermSpans) firstSpans;
        hasMoreSpans = relationTermSpan.next();
    }


    @Override
    public boolean next () throws IOException {
        isStartEnumeration = false;
        return advance();
    }


    /**
     * Returns true if there is a next match by checking if the
     * CandidateList is
     * not empty and set the first element of the list as the next
     * match.
     * Otherwise, if the RelationSpan has not ended yet, try to set
     * the
     * CandidateList.
     * 
     * @return true if there is a next match.
     * @throws IOException
     */
    private boolean advance () throws IOException {
        while (hasMoreSpans || !candidateList.isEmpty()) {
            if (!candidateList.isEmpty()) {
                CandidateSpan cs = candidateList.get(0);
                this.matchDocNumber = cs.getDoc();
                this.matchStartPosition = cs.getStart();
                this.matchEndPosition = cs.getEnd();
                this.matchPayload = cs.getPayloads();
                this.setRightStart(cs.getRightStart());
                this.setRightEnd(cs.getRightEnd());
                this.spanId = cs.getSpanId(); // relation id
                this.leftId = cs.getLeftId();
                this.rightId = cs.getRightId();
                candidateList.remove(0);
                return true;
            }
            else {
                setCandidateList();
                currentDoc = relationTermSpan.doc();
                currentPosition = relationTermSpan.start();
            }
        }
        return false;
    }


    /**
     * Setting the CandidateList by adding all relationTermSpan whose
     * start
     * position is the same as the current span position, and sort the
     * candidateList.
     * 
     * @throws IOException
     */
    private void setCandidateList () throws IOException {
        while (hasMoreSpans && relationTermSpan.doc() == currentDoc
                && relationTermSpan.start() == currentPosition) {

            CandidateSpan cs = new CandidateSpan(relationTermSpan);
            readPayload(cs);
            setPayload(cs);
            candidateList.add(cs);
            hasMoreSpans = relationTermSpan.next();
        }
        Collections.sort(candidateList);
    }


    /**
     * Identify the relation type of the given
     * {@link CandidateRelationSpan} by
     * checking the length of its payloads, and set some properties of
     * the span
     * based on the payloads.
     * 
     * @param cs
     *            a CandidateRelationSpan
     */
    private void readPayload (CandidateSpan cs) {
        List<byte[]> payload = (List<byte[]>) cs.getPayloads();
        int length = payload.get(0).length;
        ByteBuffer bb = ByteBuffer.allocate(length);
        bb.put(payload.get(0));

        cs.setLeftStart(cs.start);

        int i;
        this.payloadTypeIdentifier = bb.get(0);

        if (payloadTypeIdentifier == PayloadTypeIdentifier.TERM_TO_TERM.value) { // length 11            
            i = bb.getInt(1);
            cs.setLeftEnd(cs.start + 1);
            cs.setRightStart(i);
            cs.setRightEnd(i + 1);
            cs.setLeftId(bb.getShort(5)); // left id
            cs.setRightId(bb.getShort(7)); // right id
            if (length > 9) {
                cs.setSpanId(bb.getShort(9)); // relation id
            }
        }
        else if (payloadTypeIdentifier == PayloadTypeIdentifier.TERM_TO_ELEMENT.value) { // length
            // 15
            cs.setLeftEnd(cs.start + 1);
            cs.setRightStart(bb.getInt(1));
            cs.setRightEnd(bb.getInt(5));
            cs.setLeftId(bb.getShort(9)); // left id
            cs.setRightId(bb.getShort(11)); // right id
            if (length > 13) {
                cs.setSpanId(bb.getShort(13)); // relation id
            }
        }
        else if (payloadTypeIdentifier == PayloadTypeIdentifier.ELEMENT_TO_TERM.value) { // length
            // 15
            cs.setEnd(bb.getInt(1));
            cs.setLeftEnd(cs.end);
            i = bb.getInt(5);
            cs.setRightStart(i);
            cs.setRightEnd(i + 1);
            cs.setLeftId(bb.getShort(9)); // left id
            cs.setRightId(bb.getShort(11)); // right id
            if (length > 13) {
                cs.setSpanId(bb.getShort(13)); // relation id
            }
        }
        else if (payloadTypeIdentifier == PayloadTypeIdentifier.ELEMENT_TO_ELEMENT.value) {
            // length 19
            cs.setEnd(bb.getInt(1));
            cs.setLeftEnd(cs.end);
            cs.setRightStart(bb.getInt(5));
            cs.setRightEnd(bb.getInt(9));
            cs.setLeftId(bb.getShort(13)); // left id
            cs.setRightId(bb.getShort(15)); // right id
            if (length > 17) {
                cs.setSpanId(bb.getShort(17)); // relation id
            }
        }


        // Payload is cleared.
    }


    private void setPayload (CandidateSpan cs) throws IOException {
        ArrayList<byte[]> payload = new ArrayList<byte[]>();
        if (relationTermSpan.isPayloadAvailable()) {
            payload.addAll(relationTermSpan.getPayload());
        }
        if (direction == 0) {
            payload.add(createClassPayload(cs.getLeftStart(), cs.getLeftEnd(),
                    tempSourceNum, false));
            payload.add(createClassPayload(cs.getRightStart(),
                    cs.getRightEnd(), tempTargetNum, false));

            if (sourceClass > 0) {
                payload.add(createClassPayload(cs.getLeftStart(),
                        cs.getLeftEnd(), sourceClass, true));
            }
            if (targetClass > 0) {
                payload.add(createClassPayload(cs.getRightStart(),
                        cs.getRightEnd(), targetClass, true));
            }

        }
        else {
            payload.add(createClassPayload(cs.getRightStart(),
                    cs.getRightEnd(), tempSourceNum, false));
            payload.add(createClassPayload(cs.getLeftStart(), cs.getLeftEnd(),
                    tempTargetNum, false));

            if (sourceClass > 0) {
                payload.add(createClassPayload(cs.getRightStart(),
                        cs.getRightEnd(), sourceClass, true));
            }
            if (targetClass > 0) {
                payload.add(createClassPayload(cs.getLeftStart(),
                        cs.getLeftEnd(), targetClass, true));
            }
        }
        cs.setPayloads(payload);
    }


    private byte[] createClassPayload (int start, int end, byte classNumber,
            boolean keep) {
        ByteBuffer buffer = null;
        if (keep) {
            buffer = ByteBuffer.allocate(10);
        }
        else {
            buffer = ByteBuffer.allocate(11);
        }
        Byte classPTI = 0;
        buffer.put(classPTI);
        buffer.putInt(start);
        buffer.putInt(end);
        buffer.put(classNumber);
        return buffer.array();
    }


    @Override
    public boolean skipTo (int target) throws IOException {
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
    public long cost () {
        return firstSpans.cost();
    }


    /**
     * Returns the right start position of the current RelationSpan.
     * 
     * @return the right start position of the current RelationSpan.
     */
    public int getRightStart () {
        return rightStart;
    }


    /**
     * Sets the right start position of the current RelationSpan.
     * 
     * @param rightStart
     *            the right start position of the current RelationSpan
     */
    public void setRightStart (int rightStart) {
        this.rightStart = rightStart;
    }


    /**
     * Returns the right end position of the current RelationSpan.
     * 
     * @return the right end position of the current RelationSpan.
     */
    public int getRightEnd () {
        return rightEnd;
    }


    /**
     * Sets the right end position of the current RelationSpan.
     * 
     * @param rightEnd
     *            the right end position of the current RelationSpan.
     */
    public void setRightEnd (int rightEnd) {
        this.rightEnd = rightEnd;
    }

}
