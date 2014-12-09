package de.ids_mannheim.korap.response;

import java.util.*;
import java.io.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import de.ids_mannheim.korap.response.Notifications;
import de.ids_mannheim.korap.response.serialize.KorapResponseDeserializer;

/**
 * Base class for objects meant to be responded by the server.
 *
 * <p>
 * <blockquote><pre>
 *   KorapResponse km = new KorapResponse();
 *   System.out.println(
 *     km.toJsonString()
 *   );
 * </pre></blockquote>
 *
 * @author Nils Diewald
 * @see de.ids_mannheim.korap.response.Notifications
 */
@JsonDeserialize(using = KorapResponseDeserializer.class)
public class KorapResponse extends Notifications {
    ObjectMapper mapper = new ObjectMapper();

    private String version, name, node, listener;
    private String benchmark;
    private boolean timeExceeded = false;

    /**
     * Construct a new KorapResponse object.
     *
     * @return The new KorapResponse object
     */
    public KorapResponse () {};


    /**
     * Set the string representation of the backend's version.
     *
     * @param version The string representation of the backend's version
     * @return KorapResponse object for chaining
     */
    @JsonIgnore
    public KorapResponse setVersion (String version) {
        this.version = version;
        return this;
    };


    /**
     * Get string representation of the backend's version.
     *
     * @return String representation of the backend's version
     */
    @JsonIgnore
    public String getVersion () {
        return this.version;
    };


    /**
     * Set the string representation of the backend's name.
     * All nodes in a cluster should have the same backend name.
     *
     * @param version The string representation of the backend's name
     * @return KorapResponse object for chaining
     */
    @JsonIgnore
    public KorapResponse setName (String name) {
        this.name = name;
        return this;
    };


    /**
     * Get string representation of the backend's name.
     * All nodes in a cluster should have the same backend name.
     *
     * @return String representation of the backend's name
     */
    @JsonIgnore
    public String getName () {
        return this.name;
    };


    /**
     * Set the string representation of the node's name.
     * Each node in a cluster has a unique name.
     *
     * @param version The string representation of the node's name
     * @return KorapResponse object for chaining
     */
    @JsonIgnore
    public KorapResponse setNode (String name) {
        this.node = name;
        return this;
    };


    /**
     * Get string representation of the node's name.
     * Each node in a cluster has a unique name.
     *
     * @return String representation of the node's name
     */
    @JsonIgnore
    public String getNode () {
        return this.node;
    };
    

    /**
     * Set to <tt>true</tt> if time is exceeded
     * based on a timeout.
     *
     * <p>
     * Will add a warning (682) to the output.
     *
     * @param timeout Either <tt>true</tt> or <tt>false</tt>, in case the response
     *        timed out
     * @return KorapResponse object for chaining
     */
    @JsonIgnore
    public KorapResponse setTimeExceeded (boolean timeout) {
        if (timeout)
            this.addWarning(682, "Response time exceeded");
        this.timeExceeded = timeout;
        return this;
    };


    /**
     * Check if the response time was exceeded.
     *
     * @return <tt>true</tt> in case the response had a timeout,
     *         otherwise <tt>false</tt>
     */
    @JsonIgnore
    public boolean hasTimeExceeded () {
        return this.timeExceeded;
    };


    /**
     * Set the benchmark as timestamp differences.
     *
     * @param ts1 Starting time of the benchmark
     * @param ts2 Ending time of the benchmark
     * @return KorapResponse object for chaining
     */
    @JsonIgnore
    public KorapResponse setBenchmark (long ts1, long ts2) {
        this.benchmark =
            (ts2 - ts1) < 100_000_000 ?
            // Store as miliseconds
            (((double) (ts2 - ts1) * 1e-6) + " ms") :
            // Store as seconds
            (((double) (ts2 - ts1) / 1000000000.0) + " s");
        return this;
    };
    

    /**
     * Set the benchmark as a string representation.
     *
     * @param bm String representation of a benchmark
     *           (including trailing time unit)
     * @return KorapResponse for chaining
     */
    @JsonIgnore
    public KorapResponse setBenchmark (String bm) {
        this.benchmark = bm;
        return this;
    };


    /**
     * Get the benchmark time as a string.
     *
     * @return String representation of the benchmark
     *         (including trailing time unit)
     */
    @JsonIgnore
    public String getBenchmark () {
        return this.benchmark;
    };
    

    /**
     * Set the listener URI as a String. This is probably the localhost
     * with an arbitrary port, like
     *
     * <p>
     * <blockquote><pre>
     *   http://localhost:8080/
     * </pre></blockquote>
     *
     * @param listener String representation of the listener URI
     * @return KorapResponse object for chaining
     */
    @JsonIgnore
    public KorapResponse setListener (String listener) {
        this.listener = listener;
        return this;
    };
    

    /**
     * Get the listener URI as a string.
     *
     * @return The listener URI as a string representation
     */
    @JsonIgnore
    public String getListener () {
        return this.listener;
    };


    /**
     * Serialize response as a JsonNode.
     *
     * @return JsonNode representation of the response
     */
    @Override
    public JsonNode toJsonNode () {

        // Get notifications json response
        ObjectNode json = (ObjectNode) super.toJsonNode();

        StringBuilder sb = new StringBuilder();
        if (this.getName() != null) {
            sb.append(this.getName());
            
            if (this.getVersion() != null)
                sb.append("-");
        };

        // No name but version is given
        if (this.getVersion() != null)
            sb.append(this.getVersion());

        if (sb.length() > 0)
            json.put("version", sb.toString());
        
        if (this.timeExceeded)
            json.put("timeExceeded", true);
        
        if (this.getNode() != null)
            json.put("node", this.getNode());

        if (this.getListener() != null)
            json.put("listener", this.getListener());

        if (this.getBenchmark() != null)
            json.put("benchmark", this.getBenchmark());

        return (JsonNode) json;
    };

    /**
     * Serialize response as a JSON string.
     * <p>
     * <blockquote><pre>
     * {
     *   "version" : "Lucene-Backend-0.49.1",
     *   "timeExceeded" : true,
     *   "node" : "Tanja",
     *   "listener" : "http://localhost:8080/",
     *   "benchmark" : "12.3s",
     *   "errors": [
     *     [123, "You are not allowed to serialize these messages"],
     *     [124, "Your request was invalid"]
     *   ],
     *   "messages" : [
     *     [125, "Class is deprecated", "Notifications"]
     *   ]
     * }
     * </pre></blockquote>
     *
     * @return String representation of the response
     */
    public String toJsonString () {
        String msg = "";
        try {
            return mapper.writeValueAsString(this.toJsonNode());
        }
        catch (Exception e) {
            // Bad in case the message contains quotes!
            msg = ", \"" + e.getLocalizedMessage() + "\"";
        };

        return
            "{\"errors\":["+
            "[620, " +
            "\"Unable to generate JSON\"" + msg + "]" +
            "]}";
    };
};