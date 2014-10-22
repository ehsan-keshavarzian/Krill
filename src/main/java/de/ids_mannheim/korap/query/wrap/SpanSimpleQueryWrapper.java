package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

public class SpanSimpleQueryWrapper extends SpanQueryWrapper {
    private SpanQuery query;

    public SpanSimpleQueryWrapper (String field, String term) {
	this.isNull = false;
	this.query = new SpanTermQuery(new Term(field, term));
    };

    public SpanSimpleQueryWrapper (SpanQuery query) {
	this.isNull = false;
	this.query = query;
    };

    public SpanQuery toQuery () {
	return this.query;
    };
};