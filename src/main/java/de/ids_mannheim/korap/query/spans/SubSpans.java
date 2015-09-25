package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.SpanSubspanQuery;

/**
 * Enumeration of SubSpans, which are parts of another Spans. The
 * SubSpans are specified with a start offset relative to the original
 * span and a length. If the length is unspecified or 0, the end
 * position of the subspans is the same as that of the original spans.
 * 
 * @author margaretha
 * 
 */
public class SubSpans extends SimpleSpans {

    private int startOffset, length;


    /**
     * Constructs SubSpans for the given {@link SpanSubspanQuery}
     * specifiying the start offset and the length of the subspans.
     * 
     * @param subspanQuery
     *            a SpanSubspanQuery
     * @param context
     * @param acceptDocs
     * @param termContexts
     * @throws IOException
     */
    public SubSpans (SpanSubspanQuery subspanQuery,
                     LeafReaderContext context, Bits acceptDocs,
                     Map<Term, TermContext> termContexts) throws IOException {
        super(subspanQuery, context, acceptDocs, termContexts);
        this.startOffset = subspanQuery.getStartOffset();
        this.length = subspanQuery.getLength();
        hasMoreSpans = firstSpans.next();
    }


    @Override
    public boolean next () throws IOException {
        isStartEnumeration = false;
        return advance();
    }


    /**
     * Advances the SubSpans to the next match.
     * 
     * @return <code>true</code> if a match is found,
     *         <code>false</code> otherwise.
     * @throws IOException
     */
    private boolean advance () throws IOException {
        while (hasMoreSpans) {
            if (findMatch()) {
                hasMoreSpans = firstSpans.next();
                return true;
            }
            hasMoreSpans = firstSpans.next();
        }
        return false;
    }


    /**
     * Sets the properties of the current match/subspan.
     * 
     * @throws IOException
     */
    public boolean findMatch () throws IOException {
        if (this.startOffset < 0) {
            matchStartPosition = firstSpans.end() + startOffset;
            if (matchStartPosition < firstSpans.start()) {
                matchStartPosition = firstSpans.start();
            }
        }
        else {
            matchStartPosition = firstSpans.start() + startOffset;
            if (matchStartPosition >= firstSpans.end()) {
                return false;
            }
        }

        if (this.length > 0) {
            matchEndPosition = matchStartPosition + this.length;
            if (matchEndPosition > firstSpans.end()) {
                matchEndPosition = firstSpans.end();
            }
        }
        else {
            matchEndPosition = firstSpans.end();
        }
        matchPayload = firstSpans.getPayload();
        matchDocNumber = firstSpans.doc();
        return true;
    }


    @Override
    public boolean skipTo (int target) throws IOException {
        if (hasMoreSpans && (firstSpans.doc() < target)) {
            if (!firstSpans.skipTo(target)) {
                hasMoreSpans = false;
                return false;
            }
        }
        matchPayload.clear();
        return advance();
    }


    @Override
    public long cost () {
        return firstSpans.cost() + 1;
    }

}
