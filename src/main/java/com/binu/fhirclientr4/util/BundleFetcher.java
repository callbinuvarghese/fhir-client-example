package com.binu.fhirclientr4.util;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Objects;

/**
 * Helpful class that, given an {@link IGenericClient} and a {@link Bundle}, can iterate through all of the 'next' links
 * of the original Bundle and will return an aggregate Bundle containing all of the results that matched the search.
 */
public class BundleFetcher {
    private static final Logger LOG = LoggerFactory.getLogger(BundleFetcher.class);

    private final IGenericClient client;
    private final Bundle startingBundle;

    /**
     * Constructor.
     * @param theClient the client
     * @param theStartingBundle the bundle to start with
     * @throws NullPointerException if any of the parameters are {@code null}
     */
    public BundleFetcher (final IGenericClient theClient, final Bundle theStartingBundle) {
        Objects.requireNonNull(theClient, "theClient cannot be null");
        Objects.requireNonNull(theStartingBundle, "theStartingBundle cannot be null");
        client = theClient;
        startingBundle = theStartingBundle;
    }

    /**
     * Iterates through and calls all of the 'next' links and aggregates all of the resources into one bundle.
     * @return an aggregated bundle of all of the resources that matched the search
     */
    public Bundle fetchAll () {
        Bundle aggregatedBundle = startingBundle.copy();

        // Manually copying over all of the Patient entries into the aggregate bundle to avoid potential
        // loss-of-data bugs from the copy() function since Patient information is the most important information of
        // the bundle at this point.
        aggregatedBundle.getEntry().clear();
        aggregatedBundle.getEntry().addAll(startingBundle.getEntry());

        Bundle partialBundle = startingBundle;
        LOG.debug("Starting bundle search matched {} total resources(s) in the search and {} resource(s) "
                + "are in this bundle.", startingBundle.getTotal(), startingBundle.getEntry().size());

        // Call the 'next' link on each returned partial bundle and add the patients to the aggregate bundle
        while (partialBundle.getLink(Bundle.LINK_NEXT) != null) {
            partialBundle = client.loadPage().next(partialBundle).execute();
            LOG.debug("Got the next bundle. This 'next' bundle had {} resources(s) in it.",
                    partialBundle.getEntry().size());
            aggregatedBundle.getEntry().addAll(partialBundle.getEntry());
        }

        // Just a check to see if counts are off
        if (aggregatedBundle.getTotal() != aggregatedBundle.getEntry().size()) {
            LOG.error("Counts didn't match! Expected {} resource(s) but the bundle only had {} resource(s)!",
                    aggregatedBundle.getTotal(),
                    aggregatedBundle.getEntry().size());
        }

        // Clear links off of this bundle since they aren't really valid at this point
        aggregatedBundle.getLink().clear();

        return aggregatedBundle;
    }
}
