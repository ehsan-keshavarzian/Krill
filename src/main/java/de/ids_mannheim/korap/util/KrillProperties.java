package de.ids_mannheim.korap.util;

import java.util.*;
import java.io.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Todo: Properties may be loaded twice - although Java may cache automatically
 * 
 * @author diewald, margaretha
 *
 */
public class KrillProperties {

    public static final String defaultPropertiesLocation = "krill.properties";
    public static final String defaultInfoLocation = "krill.info";
    private static Properties prop, info;

    // Logger
    private final static Logger log = LoggerFactory
            .getLogger(KrillProperties.class);

    // Load properties from file
    public static Properties loadDefaultProperties () {
        if (prop != null)
            return prop;

        prop = loadProperties(defaultPropertiesLocation);
        return prop;
    };


    // Load properties from file
    public static Properties loadProperties (String propFile) {
        if (propFile == null)
            return loadDefaultProperties();

        InputStream iFile;
        try {
            iFile = new FileInputStream(propFile);
            prop = new Properties();
            prop.load(iFile);

        }
        catch (IOException t) {
            try {
                iFile = KrillProperties.class.getClassLoader()
                        .getResourceAsStream(propFile);
                if (iFile == null) {
                    log.warn(
                            "Cannot find {}. Please create it using "
                            + "\"src/main/resources/krill.properties.info\" as template.",
                            propFile, propFile);
                    return null;
                };

                prop = new Properties();
                prop.load(iFile);
                iFile.close();
            }
            catch (IOException e) {
                log.error(e.getLocalizedMessage());
                return null;
            };
        };
        return prop;
    };


    // Load version info from file
    public static Properties loadInfo () {
        try {
            info = new Properties();
            InputStream iFile = KrillProperties.class.getClassLoader()
                    .getResourceAsStream(defaultInfoLocation);

            if (iFile == null) {
                log.error("Cannot find {}.", defaultInfoLocation);
                return null;
            };

            info.load(iFile);
            iFile.close();
        }
        catch (IOException e) {
            log.error(e.getLocalizedMessage());
            return null;
        };
        return info;
    };
};
