package de.ids_mannheim.korap.server;

import java.io.*;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import javax.ws.rs.WebApplicationException;

import de.ids_mannheim.korap.KorapNode;
import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapSearch;
import de.ids_mannheim.korap.KorapCollection;
import de.ids_mannheim.korap.KorapMatch;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.server.KorapResponse;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.index.MatchCollector;
import de.ids_mannheim.korap.index.collector.MatchCollectorDB;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.sql.Connection;


/**
 * Root resource (exposed at root path)
 * The responses only represent JSON responses, although HTML responses
 * may be handy.
 *
 * @author Nils Diewald
 *
 * Look at http://www.mkyong.com/webservices/jax-rs/json-example-with-jersey-jackson/
 */
@Path("/")
public class Resource {

    private String version;

    // Initiate Logger
    private final static Logger log = LoggerFactory.getLogger(KorapNode.class);

    // Slightly based on String::BooleanSimple
    static Pattern p = Pattern.compile(
        "\\s*(?i:false|no|inactive|disabled|off|n|neg(?:ative)?|not|null|undef)\\s*"
    );

    // Check if a string is meant to represent null
    private static boolean isNull (String value) {
	if (value == null)
	    return true;

	Matcher m = p.matcher(value);
	if (m.matches())
	    return true;

	return false;
    };


    /**
     * Return information on the node, like name etc.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String info () {
	KorapIndex index = KorapNode.getIndex();
	KorapResponse kresp = new KorapResponse(KorapNode.getName(), index.getVersion());
	kresp.setListener(KorapNode.getListener());
	long texts = -1;

	return kresp.setTotalTexts(index.numberOf("documents"))
	    .setMsg("Up and running!")
	    .toJSON();
    };
    

    /**
     * Add new documents to the index
     *
     * @param json JSON-LD string with search and potential meta filters.
     */
    /*
     * Support GZip:
     * oR MAYBE IT'S ALREADY SUPPORTED ....
     * http://stackoverflow.com/questions/19765582/how-to-make-jersey-use-gzip-compression-for-the-response-message-body
    */
    @PUT
    @Path("/index/{textID}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String add (@PathParam("textID") Integer uid,
		       @Context UriInfo uri,
		       String json) {
	/*
	 * See
	 * http://www.mkyong.com/webservices/jax-rs/file-upload-example-in-jersey/
	 */

	// Todo: Parameter for server node

	// Get index
	KorapIndex index = KorapNode.getIndex();
	KorapResponse kresp = new KorapResponse(KorapNode.getName(), index.getVersion());

	if (index == null)
	    return kresp.setError(601, "Unable to find index").toJSON();

	String ID = "Unknown";
	boolean unstaged = false;
	try {
	    FieldDocument fd = index.addDoc(uid, json);
	    ID = fd.getID();
	    unstaged = index.getUnstaged();
	}
	// Set HTTP to ???
	catch (IOException e) {
	    
	    return kresp.setErrstr(e.getLocalizedMessage()).toJSON();
	};

	// Set HTTP to 200
	return kresp.
	    setMsg("Text \"" + ID + "\" was added successfully")
	    .setUnstaged(unstaged)
	    .toJSON();
    };


    // TODO: Commit changes to the index before the server dies!
    /**
     * Commit data changes to the index
     */
    @POST
    @Path("/index")
    @Produces(MediaType.APPLICATION_JSON)
    public String commit () {

	// Get index
	KorapIndex index = KorapNode.getIndex();
	KorapResponse kresp = new KorapResponse(KorapNode.getName(), index.getVersion());

	if (index == null)
	    return kresp.setError(601, "Unable to find index").toJSON();

	if (version == null)
	    version = index.getVersion();

	// There are documents to commit
	try {
	    index.commit();
	}
	catch (IOException e) {
	    // Set HTTP to ???
	    return kresp.setErrstr(e.getMessage()).toJSON();
	};

	// Set HTTP to ???
	return kresp.setMsg("Unstaged data was committed").toJSON();
    };



    /**
     * Find matches in the lucene index based on UIDs and return one match per doc.
     *
     * @param text_id
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String find (String json, @Context UriInfo uri) {

	// Get index
	KorapIndex index = KorapNode.getIndex();

	// Search index
        if (index != null) {
	    KorapSearch ks = new KorapSearch(json);

	    // Get query parameters
	    MultivaluedMap<String,String> qp = uri.getQueryParameters();

	    if (qp.get("uid") != null) {

		// Build Collection based on a list of uids
		List<String> uids = qp.get("uid");
		KorapCollection kc = new KorapCollection();
		kc.filterUIDs(uids.toArray(new String[uids.size()]));

		// TODO: RESTRICT COLLECTION TO ONLY RESPECT SELF DOCS (REPLICATION)

		// Override old collection
		ks.setCollection(kc);

		// Only return the first match per text
		ks.setItemsPerResource(1);

		return ks.run(index).toJSON();
	    };
	    KorapResult kr = new KorapResult();
	    kr.setNode(KorapNode.getName());
	    kr.setError(610, "No UUIDs given");
	    return kr.toJSON();
	};

	return new KorapResponse(
	    KorapNode.getName(),
	    index.getVersion()
        ).setError(601, "Unable to find index").toJSON();
    };


    /**
     * Collect matches and aggregate the UIDs plus matchcount in the database.
     *
     * @param text_id
     */
    @PUT
    @Path("/collect/{resultID}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String collect (String json,
			   @PathParam("resultID") String resultID,
			   @Context UriInfo uri) {

	// Get index
	KorapIndex index = KorapNode.getIndex();

	// No index found
	if (index == null)
  	    return new KorapResponse(
	        KorapNode.getName(),
	        index.getVersion()
            ).setError(601, "Unable to find index").toJSON();

	// Get the database
	try {
	    MatchCollectorDB mc = new MatchCollectorDB(1000, "Res_" + resultID);
	    Connection conn = KorapNode.getDBPool().getConnection();
	    mc.setDBPool("mysql", KorapNode.getDBPool(), conn);

	    // TODO: Only search in self documents (REPLICATION FTW!)

	    KorapSearch ks = new KorapSearch(json);
	    MatchCollector result = index.collect(ks, mc);

	    result.setNode(KorapNode.getName());
	    return result.toJSON();
	}
	catch (SQLException e) {
	    log.error(e.getLocalizedMessage());
	};

	return new KorapResponse(
	    KorapNode.getName(),
	    index.getVersion()
        ).setError("Unable to connect to database").toJSON();
    };





    /* These routes are still wip: */




    /**
     * Search the lucene index.
     *
     * @param json JSON-LD string with search and potential meta filters.
     */
    @POST
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String search (String json) {

	// Get index
	KorapIndex index = KorapNode.getIndex();

	// Search index
        if (index != null) {
            KorapResult kr = new KorapSearch(json).run(index);
	    kr.setNode(KorapNode.getName());
	    return kr.toJSON();
	};

	return new KorapResponse(
	    KorapNode.getName(),
	    index.getVersion()
        ).setError(601, "Unable to find index").toJSON();
    };

    @GET
    @Path("/match/{matchID}")
    @Produces(MediaType.APPLICATION_JSON)
    public String match (@PathParam("matchID") String id,
			 @Context UriInfo uri) {

	// Get index
	KorapIndex index = KorapNode.getIndex();

	// Search index
        if (index != null) {

	    // Get query parameters
	    MultivaluedMap<String,String> qp = uri.getQueryParameters();

	    boolean includeSpans = false,
		includeHighlights = true,
		extendToSentence = false,
		info = false;

	    // Optional query parameter "info" for more information on the match
	    if (!isNull(qp.getFirst("info")))
		info = true;
	    
	    // Optional query parameter "spans" for span information inclusion
	    if (!isNull(qp.getFirst("spans"))) {
		includeSpans = true;
		info = true;
	    };

	    // Optional query parameter "highlights" for highlight information inclusion
	    String highlights = qp.getFirst("highlights");
	    if (highlights != null && isNull(highlights))
		includeHighlights = false;

	    // Optional query parameter "extended" for sentence expansion
	    if (!isNull(qp.getFirst("extended")))
		extendToSentence = true;

	    List<String> foundries = qp.get("foundry");
	    List<String> layers    = qp.get("layer");

            try {		
		// Get match info
                return index.getMatchInfo(
		    id,
		    "tokens",
		    info,
		    foundries,
		    layers,
		    includeSpans,
		    includeHighlights,
		    extendToSentence
		).toJSON();
            }

	    // Nothing found
	    catch (QueryException qe) {
		// Todo: Make KorapMatch rely on KorapResponse!
                KorapMatch km = new KorapMatch();
                km.setError(qe.getMessage());
                return km.toJSON();
            }
	};

	// Response with error message
        KorapMatch km = new KorapMatch();
        km.setError("Index not found");
        return km.toJSON();
    };

    /*
      POST /collect/:result_id
      POST /peek
      POST /?text_id=...
      POST /:text_id/

      PUT /:text_id

      DELETE /:text_id
      DELETE /:corpus_sigle
      DELETE /:corpus_sigle/:doc_sigle
      DELETE /:corpus_sigle/:doc_sigle/:text_sigle
     */

    @POST
    @Path("/collection")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String collection (String json) {

	// Get index
	KorapIndex index = KorapNode.getIndex();

	if (index == null)
	    return "{\"documents\" : -1, error\" : \"No index given\" }";

	return "{}";
    };



    // Interceptor class
    public class GZIPReaderInterceptor implements ReaderInterceptor {
	@Override
	public Object aroundReadFrom(ReaderInterceptorContext context)
	    throws IOException, WebApplicationException {
	    final InputStream originalInputStream = context.getInputStream();
	    context.setInputStream(new GZIPInputStream(originalInputStream));
	    return context.proceed();
	};
    };

    public class GZIPWriterInterceptor implements WriterInterceptor {
	@Override
	public void aroundWriteTo(WriterInterceptorContext context)
	    throws IOException, WebApplicationException {
	    final OutputStream outputStream = context.getOutputStream();
	    context.setOutputStream(new GZIPOutputStream(outputStream));
	    context.proceed();
	};
    };
};
