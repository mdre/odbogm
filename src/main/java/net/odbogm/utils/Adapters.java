package net.odbogm.utils;

import com.orientechnologies.orient.core.record.impl.ODocument;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Converters from/to certain Java data types to OrientDB data types.
 * 
 * @author jbertinetti
 */
public class Adapters {

    private final static Logger LOGGER = Logger.getLogger(Adapters.class.getName());

    // existing adapters:
    // @TODO: enum fields is a case of adaptable
    public static final Adapter OPOINT = new OPointAdapter();


    /**
     * Converts to/from database value. `fromDb` could receive a null, `toDb` not.
     */
    public interface Adapter {
        Object toDb(Object javaVal);
        Object fromDb(Object dbVal);
        String dbType();
    }


    private static class OPointAdapter implements Adapter {

        @Override
        public Object toDb(Object javaVal) {
            OPoint v = (OPoint)javaVal;
            ODocument p = new ODocument("OPoint");
            p.field("coordinates", Arrays.asList(v.getX(), v.getY()));
            return p;
        }

        @Override
        public Object fromDb(Object dbVal) {
            if (dbVal == null) return null;
            try {
                ODocument p = (ODocument)dbVal;
                List<Double> l = p.getProperty("coordinates");
                return new OPoint(l.get(0), l.get(1));
            } catch (ClassCastException ex) {
                LOGGER.log(Level.SEVERE, "Error converting {0} value to OPoint property.", dbVal);
                return null;
            }
        }

        @Override
        public String dbType() {
            return "EMBEDDED OPoint";
        }

    }

}
