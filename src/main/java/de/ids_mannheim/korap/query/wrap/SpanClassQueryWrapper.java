package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.spans.SpanQuery;

import de.ids_mannheim.korap.query.SpanClassQuery;
import de.ids_mannheim.korap.util.QueryException;


// TODO: If this.subquery.isNegative(), it may be an Expansion!
// SpanExpansionQuery(x, y.negative, min, max. direction???, classNumber, true)

public class SpanClassQueryWrapper extends SpanQueryWrapper {
    private SpanQueryWrapper subquery;

    public SpanClassQueryWrapper (SpanQueryWrapper subquery, byte number) {
        this.subquery = subquery;
        this.number   = number;
        if (number != (byte) 0)
            this.hasClass = true;
		this.min = subquery.min;
        this.max=subquery.max;
    };

    public SpanClassQueryWrapper (SpanQueryWrapper subquery, short number) {
        this(subquery, (byte) number);
    };

    public SpanClassQueryWrapper (SpanQueryWrapper subquery, int number) {
        this(subquery, (byte) number);
    };

    public SpanClassQueryWrapper (SpanQueryWrapper subquery) {
        this(subquery, (byte) 0);
    };

    public boolean isEmpty () {
        return this.subquery.isEmpty();
    };

    public boolean isOptional () {
        return this.subquery.isOptional();
    };

    public boolean isNull () {
        return this.subquery.isNull();
    };

    public boolean isNegative () {
        return this.subquery.isNegative();
    };

    public SpanQuery toQuery () throws QueryException {
        if (this.subquery.isNull())
            return (SpanQuery) null;

        SpanQuery sq = (SpanQuery) this.subquery.toQuery();

        if (sq == null) return (SpanQuery) null;
	
        if (this.number == (byte) 0) {
            return new SpanClassQuery(sq);
        };
        return new SpanClassQuery(sq, (byte) this.number);
    };
};
