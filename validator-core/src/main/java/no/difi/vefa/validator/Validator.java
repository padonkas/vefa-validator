package no.difi.vefa.validator;

import no.difi.vefa.validator.api.*;
import no.difi.vefa.validator.source.RepositorySource;
import no.difi.xsd.vefa.validator._1.PackageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Validator containing an instance of validation configuration and validation artifacts.
 *
 * Validator is thread safe and should normally be created only once in a program.
 */
public class Validator implements Closeable {

    /**
     * Logger
     */
    private static Logger logger = LoggerFactory.getLogger(Validator.class);

    /**
     * Config
     */
    private Properties properties;

    /**
     * Source
     */
    private Source source;

    /**
     * Current validator instance.
     */
    private ValidatorInstance validatorInstance;

    /**
     * Constructor
     */
    Validator() {
        // No action
    }

    /**
     * Validate file.
     *
     * @param file File to validate.
     * @return Validation result.
     * @throws IOException
     */
    public Validation validate(File file) throws IOException {
        return validate(file.toPath());
    }

    /**
     * Validate file.
     *
     * @param file File to validate.
     * @return Validation result.
     * @throws IOException
     */
    public Validation validate(Path file) throws IOException {
        InputStream inputStream = Files.newInputStream(file);
        Validation validation = validate(inputStream);
        inputStream.close();
        return validation;
    }

    /**
     * Validate content of stream.
     *
     * @param inputStream Stream containing content.
     * @return Validation result.
     */
    public Validation validate(InputStream inputStream) {
        return new Validation(this.validatorInstance, inputStream);
    }

    /**
     * List of packages supported by validator.
     *
     * @return List of packages.
     */
    public List<PackageType> getPackages() {
        return this.validatorInstance.getPackages();
    }

    /**
     * Set configuration for validator.
     *
     * @param properties Configuration
     */
    void setProperties(Properties properties) {
        this.properties = properties;
    }

    /**
     * Set source for validator.
     *
     * @param source Source
     */
    void setSource(Source source) {
        this.source = source;
    }

    /**
     * Creates a new instance of ValidatorInstance for use.
     *
     * @throws ValidatorException
     */
    void load(Class<? extends Checker>[] checkerImpls, Class<? extends Renderer>[] rendererImpls, Declaration... declarationImpls) throws ValidatorException {
        try {
            // Make sure to default to repository source if no source is set.
            if (source == null)
                source = RepositorySource.forProduction();

            // Create a new instance based on source.
            validatorInstance = new ValidatorInstance(source, properties, checkerImpls, rendererImpls, declarationImpls);
        } catch (ValidatorException e) {
            logger.error(e.getMessage(), e);

            // Exceptions during running is not a problem, but exception before the first validator is created is a problem.
            if (validatorInstance == null)
                throw new ValidatorException("Unable to load validator.", e);
        }
    }

    @Override
    public void close() {
        if (validatorInstance != null) {
            validatorInstance.close();
            validatorInstance = null;
        }
    }
}
