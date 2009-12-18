package com.basho.riak.client.plain;

import java.io.IOException;
import java.util.List;

import org.json.JSONObject;

import com.basho.riak.client.RiakBucketInfo;
import com.basho.riak.client.RiakClient;
import com.basho.riak.client.RiakConfig;
import com.basho.riak.client.RiakObject;
import com.basho.riak.client.jiak.JiakClient;
import com.basho.riak.client.raw.RawClient;
import com.basho.riak.client.request.RequestMeta;
import com.basho.riak.client.request.RiakWalkSpec;
import com.basho.riak.client.response.BucketResponse;
import com.basho.riak.client.response.FetchResponse;
import com.basho.riak.client.response.HttpResponse;
import com.basho.riak.client.response.RiakIOException;
import com.basho.riak.client.response.RiakResponseException;
import com.basho.riak.client.response.StoreResponse;
import com.basho.riak.client.response.StreamHandler;
import com.basho.riak.client.response.WalkResponse;

/**
 * An adapter from {@link RiakClient} to a slightly less HTTP, more
 * Java-centric, interface. Objects are returned without HTTP specific
 * information and exceptions are thrown on unsuccessful responses.
 */
public class PlainClient {

    private RiakClient impl;

    /** Connect to the Jiak interface using the given configuration. */
    public static PlainClient connectToJiak(RiakConfig config) {
        return new PlainClient(new JiakClient(config));
    }

    /** Connect to the Jiak interface using the given URL. */
    public static PlainClient connectToJiak(String url) {
        return new PlainClient(new JiakClient(url));
    }

    /** Connect to the Raw interface using the given configuration. */
    public static PlainClient connectToRaw(RiakConfig config) {
        return new PlainClient(new RawClient(config));
    }

    /** Connect to the Jiak interface using the given URL. */
    public static PlainClient connectToRaw(String url) {
        return new PlainClient(new RawClient(url));
    }

    /**
     * Object client wraps an existing {@link RiakClient} and adapts its
     * interface
     */
    public PlainClient(RiakClient riakClient) {
        impl = riakClient;
    }

    /**
     * See {@link RiakClient}.setBucketSchema(). In addition, throws
     * {@link RiakPlainResponseException} if the server does not successfully
     * update the bucket schema.
     */
    public void setBucketSchema(String bucket, JSONObject schema, RequestMeta meta) throws RiakPlainIOException,
            RiakPlainResponseException {
        HttpResponse r = null;
        try {
            r = impl.setBucketSchema(bucket, schema, meta);
        } catch (RiakIOException ioe) {
            throw new RiakPlainIOException(ioe);
        }

        if (r.getStatusCode() != 204)
            throw new RiakPlainResponseException(new RiakResponseException(r, r.getBody()));
    }

    public void setBucketSchema(String bucket, JSONObject schema) throws RiakPlainIOException,
            RiakPlainResponseException {
        setBucketSchema(bucket, schema, null);
    }

    /**
     * See {@link RiakClient}.listBucket(). In addition, throws
     * {@link RiakPlainResponseException} if the server does not return the
     * bucket information
     */
    public RiakBucketInfo listBucket(String bucket, RequestMeta meta) throws RiakPlainIOException,
            RiakPlainResponseException {
        BucketResponse r;

        try {
            r = impl.listBucket(bucket, meta);
        } catch (RiakIOException ioe) {
            throw new RiakPlainIOException(ioe);
        } catch (RiakResponseException re) {
            throw new RiakPlainResponseException(re);
        }

        if (r.getStatusCode() != 200)
            throw new RiakPlainResponseException(new RiakResponseException(r, r.getBody()));

        return r.getBucketInfo();
    }

    public RiakBucketInfo listBucket(String bucket) throws RiakPlainIOException, RiakPlainResponseException {
        return listBucket(bucket, null);
    }

    /**
     * See {@link RiakClient}.store(). In addition, throws
     * {@link RiakPlainResponseException} if the server does not succesfully
     * store the object.
     */
    public void store(RiakObject object, RequestMeta meta) throws RiakPlainIOException, RiakPlainResponseException {
        StoreResponse r;
        try {
            r = impl.store(object, meta);
        } catch (RiakIOException ioe) {
            throw new RiakPlainIOException(ioe);
        } catch (RiakResponseException re) {
            throw new RiakPlainResponseException(re);
        }

        if (r.getStatusCode() != 200 && r.getStatusCode() != 204)
            throw new RiakPlainResponseException(new RiakResponseException(r, r.getBody()));

        object.updateMeta(r);
    }

    public void store(RiakObject object) throws RiakPlainIOException, RiakPlainResponseException {
        store(object, null);
    }

    /**
     * See {@link RiakClient}.fetchMeta(). In addition:
     * 
     * 1. Returns null if object doesn't exist.
     * 
     * 2. Throws {@link RiakPlainResponseException} if the server does not
     * return the metadata.
     */
    public RiakObject fetchMeta(String bucket, String key, RequestMeta meta) throws RiakPlainIOException,
            RiakPlainResponseException {
        FetchResponse r;
        try {
            r = impl.fetchMeta(bucket, key, meta);
        } catch (RiakIOException ioe) {
            throw new RiakPlainIOException(ioe);
        } catch (RiakResponseException re) {
            throw new RiakPlainResponseException(re);
        }

        if (r.getStatusCode() == 404)
            return null;

        if (r.getStatusCode() != 200 && r.getStatusCode() != 304)
            throw new RiakPlainResponseException(new RiakResponseException(r, r.getBody()));

        if (r.getStatusCode() == 200 && !r.hasObject())
            throw new RiakPlainResponseException(new RiakResponseException(r, "Failed to parse metadata"));

        return r.getObject();
    }

    public RiakObject fetchMeta(String bucket, String key) throws RiakPlainIOException, RiakPlainResponseException {
        return fetchMeta(bucket, key, null);
    }

    /**
     * See {@link RiakClient}.fetch(). In addition:
     * 
     * 1. Returns null if object doesn't exist.
     * 
     * 2. Throws {@link RiakPlainResponseException} if the server does not
     * return the object.
     */
    public RiakObject fetch(String bucket, String key, RequestMeta meta) throws RiakPlainIOException,
            RiakPlainResponseException {
        FetchResponse r;
        try {
            r = impl.fetch(bucket, key, meta);
        } catch (RiakIOException ioe) {
            throw new RiakPlainIOException(ioe);
        } catch (RiakResponseException re) {
            throw new RiakPlainResponseException(re);
        }

        if (r.getStatusCode() == 404)
            return null;

        if (r.getStatusCode() != 200 && r.getStatusCode() != 304)
            throw new RiakPlainResponseException(new RiakResponseException(r, r.getBody()));

        if (r.getStatusCode() == 200 && !r.hasObject())
            throw new RiakPlainResponseException(new RiakResponseException(r, "Failed to parse object"));

        return r.getObject();
    }

    public RiakObject fetch(String bucket, String key) throws RiakPlainIOException, RiakPlainResponseException {
        return fetch(bucket, key, null);
    }

    public boolean stream(String bucket, String key, StreamHandler handler, RequestMeta meta) throws IOException {
        return impl.stream(bucket, key, handler, meta);
    }

    /**
     * See {@link RiakClient}.delete(). In addition, throws
     * {@link RiakPlainResponseException} if the object was not deleted.
     * Succeeds if object did not previously exist.
     */
    public void delete(String bucket, String key, RequestMeta meta) throws RiakPlainIOException,
            RiakPlainResponseException {
        HttpResponse r;
        try {
            r = impl.delete(bucket, key, meta);
        } catch (RiakIOException ioe) {
            throw new RiakPlainIOException(ioe);
        }

        if (r.getStatusCode() != 204 && r.getStatusCode() != 404)
            throw new RiakPlainResponseException(new RiakResponseException(r, r.getBody()));
    }

    public void delete(String bucket, String key) throws RiakPlainIOException, RiakPlainResponseException {
        delete(bucket, key, null);
    }

    /**
     * See {@link RiakClient}.walk(). In addition, throws
     * {@link RiakPlainResponseException} if the links could not be walked or
     * the result steps were not returned. Returns null if the object doesn't
     * exist.
     */
    public List<? extends List<? extends RiakObject>> walk(String bucket, String key, String walkSpec, RequestMeta meta)
            throws RiakPlainIOException, RiakPlainResponseException {
        WalkResponse r;
        try {
            r = impl.walk(bucket, key, walkSpec, meta);
        } catch (RiakIOException ioe) {
            throw new RiakPlainIOException(ioe);
        } catch (RiakResponseException re) {
            throw new RiakPlainResponseException(re);
        }

        if (r.getStatusCode() == 404)
            return null;

        if (r.getStatusCode() != 200)
            throw new RiakPlainResponseException(new RiakResponseException(r, r.getBody()));

        if (!r.hasSteps())
            throw new RiakPlainResponseException(new RiakResponseException(r, "Failed to parse walk results"));

        return r.getSteps();
    }

    public List<? extends List<? extends RiakObject>> walk(String bucket, String key, String walkSpec)
            throws RiakPlainIOException, RiakPlainResponseException {
        return walk(bucket, key, walkSpec, null);
    }

    public List<? extends List<? extends RiakObject>> walk(String bucket, String key, RiakWalkSpec walkSpec)
            throws RiakPlainIOException, RiakPlainResponseException {
        return walk(bucket, key, walkSpec.toString(), null);
    }
}
