package de.ids_mannheim.korap.query.spans;

import java.io.IOException;

import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.util.Bits;


import de.ids_mannheim.korap.query.SimpleSpanQuery;

/**	NextSpans is an enumeration of Span matches, which ensures that  
 * 	a span is immediately followed by another span. 
 * 
 * 	@author margaretha 
 * */
public class NextSpans extends SimpleSpans {	
	
    public NextSpans (SimpleSpanQuery simpleSpanQuery,
  	      AtomicReaderContext context,
  	      Bits acceptDocs,
  	      Map<Term,TermContext> termContexts) throws IOException {
    	this(simpleSpanQuery, context, acceptDocs, termContexts, true);    	
    }
    
    public NextSpans (SimpleSpanQuery simpleSpanQuery,
	      AtomicReaderContext context,
	      Bits acceptDocs,
	      Map<Term,TermContext> termContexts,
	      boolean collectPayloads) throws IOException {		 			
		super(simpleSpanQuery, context, acceptDocs, termContexts,collectPayloads);
	}

    /** Check weather the end position of the current firstspan equals 
     *  the start position of the secondspan. 
  	 * */
	protected int findMatch() {		
		if (firstSpans.end() == secondSpans.start()) {			
			matchDocNumber = firstSpans.doc();
			matchStartPosition = firstSpans.start();
			matchEndPosition = secondSpans.end();	
			return 0;
		}		
		else if (firstSpans.end() > secondSpans.start())
			return 1;
		
		return -1;		
	}	
}
