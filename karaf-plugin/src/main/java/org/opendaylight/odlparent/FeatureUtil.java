/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.odlparent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.RegEx;
import org.apache.karaf.features.internal.model.Bundle;
import org.apache.karaf.features.internal.model.ConfigFile;
import org.apache.karaf.features.internal.model.Feature;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.model.JaxbUtil;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.ops4j.pax.url.mvn.internal.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FeatureUtil {
    private static final Logger LOG = LoggerFactory.getLogger(FeatureUtil.class);

    private static final Pattern MVN_PATTERN = Pattern.compile("mvn:", Pattern.LITERAL);
    private static final Pattern WRAP_PATTERN = Pattern.compile("wrap:", Pattern.LITERAL);

    @RegEx
    private static final String VERSION_STRIP_PATTERN_STR = "\\$.*$";
    private static final Pattern VERSION_STRIP_PATTERN = Pattern.compile(VERSION_STRIP_PATTERN_STR);

    private FeatureUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Converts the given list of URLs to artifact coordinates.
     *
     * @param urls The URLs.
     * @return The corresponding artifact coordinates.
     * @throws MalformedURLException if a URL is malformed.
     */
    public static List<String> toCoord(final List<URL> urls) throws MalformedURLException {
        List<String> result = new ArrayList<>();
        for (URL url : urls) {
            result.add(toCoord(url));
        }
        LOG.trace("toCoord({}) returns {}", urls, result);
        return result;
    }

    /**
     * Converts the given URL to artifact coordinates.
     *
     * @param url The URL.
     * @return The corresponding artifact coordinates.
     * @throws MalformedURLException if the URL is malformed.
     */
    public static String toCoord(final URL url) throws MalformedURLException {
        String repository = url.toString();
        String unwrappedRepo = WRAP_PATTERN.matcher(repository).replaceFirst("");
        Parser parser = new Parser(unwrappedRepo);
        String coord = MVN_PATTERN.matcher(parser.getGroup()).replaceFirst("") + ":" + parser.getArtifact();
        if (parser.getType() != null) {
            coord = coord + ":" + parser.getType();
        }
        if (parser.getClassifier() != null) {
            coord = coord + ":" + parser.getClassifier();
        }
        coord = coord + ":" + VERSION_STRIP_PATTERN.matcher(parser.getVersion()).replaceAll("");
        LOG.trace("toCoord({}) returns {}", url, coord);
        return coord;
    }

    /**
     * Parses the given repository as URLs and converts them to artifact coordinates.
     *
     * @param repository The repository (list of URLs).
     * @return The corresponding artifact coordinates.
     * @throws MalformedURLException if a URL is malformed.
     */
    public static Set<String> mvnUrlsToCoord(final List<String> repository) throws MalformedURLException {
        Set<String> result = new LinkedHashSet<>();
        for (String url : repository) {
            result.add(toCoord(new URL(url)));
        }
        LOG.trace("mvnUrlsToCoord({}) returns {}", repository, result);
        return result;
    }

    /**
     * Converts the given features' repository to artifact coordinates.
     *
     * @param features The features.
     * @return The corresponding artifact coordinates.
     * @throws MalformedURLException if a URL is malformed.
     */
    public static Set<String> featuresRepositoryToCoords(final Features features) throws MalformedURLException {
        return mvnUrlsToCoord(features.getRepository());
    }

    /**
     * Converts all the given features' repositories to artifact coordinates.
     *
     * @param features The features.
     * @return The corresponding artifact coordinates.
     * @throws MalformedURLException if a URL is malformed.
     */
    public static Set<String> featuresRepositoryToCoords(final Set<Features> features) throws MalformedURLException {
        Set<String> result = new LinkedHashSet<>();
        for (Features feature : features) {
            result.addAll(featuresRepositoryToCoords(feature));
        }
        LOG.trace("featuresRepositoryToCoords({}) returns {}", features, result);
        return result;
    }

    /**
     * Lists the artifact coordinates of the given feature's bundles and configuration files.
     *
     * @param feature The feature.
     * @return The corresponding coordinates.
     * @throws MalformedURLException if a URL is malformed.
     */
    public static Set<String> featureToCoords(final Feature feature) throws MalformedURLException {
        Set<String> result = new LinkedHashSet<>();
        if (feature.getBundle() != null) {
            result.addAll(bundlesToCoords(feature.getBundle()));
        }
        if (feature.getConfigfile() != null) {
            result.addAll(configFilesToCoords(feature.getConfigfile()));
        }
        LOG.trace("featureToCoords({}) returns {}", feature.getName(), result);
        return result;
    }

    /**
     * Lists the artifact coordinates of the given configuration files.
     *
     * @param configfiles The configuration files.
     * @return The corresponding coordinates.
     * @throws MalformedURLException if a URL is malformed.
     */
    public static Set<String> configFilesToCoords(final List<ConfigFile> configfiles) throws MalformedURLException {
        Set<String> result = new LinkedHashSet<>();
        for (ConfigFile configFile : configfiles) {
            result.add(toCoord(new URL(configFile.getLocation())));
        }
        LOG.trace("configFilesToCoords({}) returns {}", configfiles, result);
        return result;
    }

    /**
     * Lists the artifact coordinates of the given bundles.
     *
     * @param bundles The bundles.
     * @return The corresponding coordinates.
     * @throws MalformedURLException if a URL is malformed.
     */
    public static Set<String> bundlesToCoords(final List<Bundle> bundles) throws MalformedURLException {
        Set<String> result = new LinkedHashSet<>();
        for (Bundle bundle : bundles) {
            result.add(toCoord(new URL(bundle.getLocation())));
        }
        LOG.trace("bundlesToCoords({}) returns {}", bundles, result);
        return result;
    }

    /**
     * Extracts all the artifact coordinates for the given features (repositories, bundles, configuration files).
     *
     * @param features The feature.
     * @return The artifact coordinates.
     * @throws MalformedURLException if a URL is malformed.
     */
    public static Set<String> featuresToCoords(final Features features) throws MalformedURLException {
        Set<String> result = new LinkedHashSet<>();
        if (features.getRepository() != null) {
            result.addAll(featuresRepositoryToCoords(features));
        }
        if (features.getFeature() != null) {
            for (Feature feature : features.getFeature()) {
                result.addAll(featureToCoords(feature));
            }
        }
        LOG.trace("featuresToCoords({}) returns {}", features.getName(), result);
        return result;
    }

    /**
     * Extracts all the artifact coordinates for the given set of features (repositories, bundles, configuration
     * files).
     *
     * @param features The features.
     * @return The artifact coordinates.
     * @throws MalformedURLException if a URL is malformed.
     */
    public static Set<String> featuresToCoords(final Set<Features> features) throws MalformedURLException {
        Set<String> result = new LinkedHashSet<>();
        for (Features feature : features) {
            result.addAll(featuresToCoords(feature));
        }
        LOG.trace("featuresToCoords({}) returns {}", features, result);
        return result;
    }

    /**
     * Unmarshal all the features in the given artifacts.
     *
     * @param featureArtifacts The artifacts.
     * @return The features.
     * @throws FileNotFoundException if a file is missing.
     */
    public static Set<Features> readFeatures(final Set<Artifact> featureArtifacts) throws FileNotFoundException {
        Set<Features> result = new LinkedHashSet<>();
        for (Artifact artifact : featureArtifacts) {
            result.add(readFeature(artifact));
        }
        LOG.trace("readFeatures({}) returns {}", featureArtifacts, result);
        return result;
    }

    /**
     * Unmarshal the features in the given artifact.
     *
     * @param artifact The artifact.
     * @return The features.
     * @throws FileNotFoundException if a file is missing.
     */
    public static Features readFeature(final Artifact artifact) throws FileNotFoundException {
        return readFeature(artifact.getFile());
    }

    /**
     * Unmarshal the features in the given file.
     *
     * @param file The file.
     * @return The features.
     * @throws FileNotFoundException if a file is missing.
     */
    public static Features readFeature(final File file) throws FileNotFoundException {
        FileInputStream stream = new FileInputStream(file);
        Features result = JaxbUtil.unmarshal(file.toURI().toString(), stream, false);
        LOG.trace("readFeature({}) returns {} without resolving first", file, result.getName());
        return result;
    }

    /**
     * Unmarshal the features matching the given artifact coordinates.
     *
     * @param aetherUtil The Aether resolver.
     * @param coords The artifact coordinates.
     * @return The features.
     * @throws ArtifactResolutionException if the coordinates can't be resolved.
     * @throws FileNotFoundException if a file is missing.
     */
    public static Features readFeature(final AetherUtil aetherUtil, final String coords)
            throws ArtifactResolutionException, FileNotFoundException {
        Artifact artifact = aetherUtil.resolveArtifact(coords);
        Features result = readFeature(artifact);
        LOG.trace("readFeature({}) returns {} after resolving first", coords, result.getName());
        return result;
    }

    /**
     * Unmarshals all the features starting from the given feature.
     *
     * @param aetherUtil The Aether resolver.
     * @param features The starting features.
     * @param existingCoords The artifact coordinates which have already been unmarshalled.
     * @return The features.
     * @throws MalformedURLException if a URL is malformed.
     * @throws FileNotFoundException if a file is missing.
     * @throws ArtifactResolutionException if artifact coordinates can't be resolved.
     */
    public static Set<Features> findAllFeaturesRecursively(
            final AetherUtil aetherUtil, final Features features, final Set<String> existingCoords)
            throws MalformedURLException, FileNotFoundException, ArtifactResolutionException {
        LOG.debug("findAllFeaturesRecursively({}) starts", features.getName());
        LOG.trace("findAllFeaturesRecursively knows about these coords: {}", existingCoords);
        Set<Features> result = new LinkedHashSet<>();
        Set<String> coords = FeatureUtil.featuresRepositoryToCoords(features);
        for (String coord : coords) {
            if (!existingCoords.contains(coord)) {
                LOG.trace("findAllFeaturesRecursively() going to add {}", coord);
                existingCoords.add(coord);
                Features feature = FeatureUtil.readFeature(aetherUtil, coord);
                result.add(feature);
                LOG.debug("findAllFeaturesRecursively() added {}", coord);
                result.addAll(findAllFeaturesRecursively(aetherUtil, FeatureUtil.readFeature(aetherUtil, coord),
                        existingCoords));
            } else {
                LOG.trace("findAllFeaturesRecursively() skips known {}", coord);
            }
        }
        return result;
    }

    /**
     * Unmarshals all the features starting from the given features.
     *
     * @param aetherUtil The Aether resolver.
     * @param features The starting features.
     * @param existingCoords The artifact coordinates which have already been unmarshalled.
     * @return The features.
     * @throws MalformedURLException if a URL is malformed.
     * @throws FileNotFoundException if a file is missing.
     * @throws ArtifactResolutionException if artifact coordinates can't be resolved.
     */
    public static Set<Features> findAllFeaturesRecursively(
            final AetherUtil aetherUtil, final Set<Features> features, final Set<String> existingCoords)
            throws MalformedURLException, FileNotFoundException, ArtifactResolutionException {
        Set<Features> result = new LinkedHashSet<>();
        for (Features feature : features) {
            result.addAll(findAllFeaturesRecursively(aetherUtil, feature, existingCoords));
        }
        return result;
    }

    /**
     * Unmarshals all the features (including known ones) starting from the given features.
     *
     * @param aetherUtil The Aether resolver.
     * @param features The starting features.
     * @return The features.
     * @throws MalformedURLException if a URL is malformed.
     * @throws FileNotFoundException if a file is missing.
     * @throws ArtifactResolutionException if artifact coordinates can't be resolved.
     */
    public static Set<Features> findAllFeaturesRecursively(final AetherUtil aetherUtil, final Set<Features> features)
            throws MalformedURLException, FileNotFoundException, ArtifactResolutionException {
        return findAllFeaturesRecursively(aetherUtil, features, new LinkedHashSet<String>());
    }

}
