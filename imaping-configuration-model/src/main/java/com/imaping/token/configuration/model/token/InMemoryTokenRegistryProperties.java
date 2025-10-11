package com.imaping.token.configuration.model.token;

import com.fasterxml.jackson.annotation.JsonFilter;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@JsonFilter("InMemoryTokenRegistryProperties")
public class InMemoryTokenRegistryProperties implements Serializable {

    private static final long serialVersionUID = -2600525447128979994L;

    /**
     * Allow the token registry to cache token items for period of time
     * and auto-evict and clean up, removing the need to running a token
     * registry cleaner in the background.
     */
    private boolean cache = true;

    /**
     * The initial capacity of the underlying memory store.
     * The implementation performs internal sizing to accommodate this many elements.
     */
    private int initialCapacity = 1000;

    /**
     * The load factor threshold, used to control resizing.
     * Resizing may be performed when the average number of elements per bin exceeds this threshold.
     */
    private int loadFactor = 1;

    /**
     * The estimated number of concurrently updating threads.
     * The implementation performs internal sizing to try to accommodate this many threads.
     */
    private int concurrency = 20;

}

