package de.ids_mannheim.korap.query;

import java.io.IOException;

import java.util.Set;
import java.util.Map;

import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;

import de.ids_mannheim.korap.query.spans.MatchModifyClassSpans;
import de.ids_mannheim.korap.query.SpanClassQuery;

/**
 * Shrinks spans to a classed span.
 */
public class SpanMatchModifyClassQuery extends SpanClassQuery {
    private boolean divide = false;

    public SpanMatchModifyClassQuery (SpanQuery highlight, byte number, boolean divide) {
	super(highlight, number);
	this.divide = divide;
    };

    public SpanMatchModifyClassQuery (SpanQuery highlight, boolean divide) {
	this(highlight, (byte) 0, divide);
    };

    public SpanMatchModifyClassQuery (SpanQuery highlight, byte number) {
	this(highlight, number, false);
    };

    public SpanMatchModifyClassQuery (SpanQuery highlight) {
	this(highlight, (byte) 0, false);
    };

    @Override
    public String toString (String field) {
	StringBuffer buffer = new StringBuffer();
	if (divide) {
	    buffer.append("split(");
	}
	else {
	    buffer.append("shrink(");
	};
	buffer.append((int) this.number).append(": ");
        buffer.append(this.highlight.toString());
	buffer.append(')');

	buffer.append(ToStringUtils.boost(getBoost()));
	return buffer.toString();
    };

    @Override
    public Spans getSpans (final AtomicReaderContext context, Bits acceptDocs, Map<Term,TermContext> termContexts) throws IOException {
	return (Spans) new MatchModifyClassSpans(this.highlight, context, acceptDocs, termContexts, number, divide);
    };

    @Override
    public Query rewrite (IndexReader reader) throws IOException {
	SpanMatchModifyClassQuery clone = null;
	SpanQuery query = (SpanQuery) this.highlight.rewrite(reader);

	if (query != this.highlight) {
	    if (clone == null)
		clone = this.clone();
	    clone.highlight = query;
	};

	if (clone != null)
	    return clone;

	return this;
    };

    @Override
    public SpanMatchModifyClassQuery clone() {
	SpanMatchModifyClassQuery spanMatchQuery = new SpanMatchModifyClassQuery(
	    (SpanQuery) this.highlight.clone(),
	    this.number,
	    this.divide
        );
	spanMatchQuery.setBoost(getBoost());
	return spanMatchQuery;
    };


    /** Returns true iff <code>o</code> is equal to this. */
    @Override
    public boolean equals(Object o) {
	if (this == o) return true;
	if (!(o instanceof SpanMatchModifyClassQuery)) return false;
	
	final SpanMatchModifyClassQuery spanMatchModifyClassQuery = (SpanMatchModifyClassQuery) o;
	
	if (!highlight.equals(spanMatchModifyClassQuery.highlight)) return false;
	if (this.number != spanMatchModifyClassQuery.number) return false;
	if (this.divide != spanMatchModifyClassQuery.divide) return false;
	return getBoost() == spanMatchModifyClassQuery.getBoost();
    };


    // I don't know what I am doing here
    @Override
    public int hashCode() {
	int result = 1;
	result = highlight.hashCode();
	result += number + 33_333;
	result += divide ? 1 : 0;
	result ^= (result << 15) | (result >>> 18);
	result += Float.floatToRawIntBits(getBoost());
	return result;
    };
};