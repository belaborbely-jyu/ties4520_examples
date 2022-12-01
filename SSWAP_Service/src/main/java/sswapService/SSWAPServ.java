package sswapService;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;

import info.sswap.api.model.RIG;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPObject;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.api.model.SSWAPProperty;
import info.sswap.api.model.SSWAPSubject;
import info.sswap.api.servlet.MapsTo;

public class SSWAPServ extends MapsTo {

    private RIG rigGraph;
    private HashMap<String, String> subjectHashMap = new HashMap<String, String>();
    private SSWAPObject object;
    private SSWAPSubject subject;
    private int objectCount = 0;

    /**
     * Types and predicates created in the Resource Invocation Graph (RIG) document.
     *
     * @param rig document within which to get/create the types and predicates
     */
    @Override
    protected void initializeRequest(RIG rig) {
        // if we need to check service parameters we could start here
        System.out.println("--- in service...");
        rigGraph = rig;
    }


    @Override
    protected void mapsTo(SSWAPSubject translatedSubject) throws Exception {

        object = translatedSubject.getObject();
        subject = translatedSubject;

        Iterator<SSWAPProperty> iterator = translatedSubject.getProperties().iterator();

        while (iterator.hasNext()) {
            SSWAPProperty property = iterator.next();

            SSWAPPredicate predicate = rigGraph.getPredicate(property.getURI());
            String lookupValue = getStrValue(translatedSubject, predicate);

            if (lookupValue == null) lookupValue = "";
            subjectHashMap.put(getStrName(property.getURI()), lookupValue);
        }

        doServiceLogic();
    }


    /**
     * Sets an object of a property
     *
     * @param objProperty the name of the value
     * @param objValue    the value
     */
    public void setObjectProperty(String objProperty, String objValue) {
        Iterator<SSWAPProperty> iterator = object.getProperties().iterator();

        while (iterator.hasNext()) {
            SSWAPProperty property = iterator.next();
            SSWAPPredicate predicate = rigGraph.getPredicate(property.getURI());
            if (getStrName(property.getURI()).equals(objProperty)) {
                object.setProperty(predicate, objValue);
                break;
            }
        }
    }


    /**
     * Imitates logic of the service. Converts values of request properties to the values of result properties. The case of 2 results (objects).
     */
    public void doServiceLogic() {


        objectCount = 1;

        for (int i = 0; i < 3; i++) {
            String resultProperty = "resultProperty_" + (i + 1);
            String resultValue = subjectHashMap.get("requestProperty_" + (i + 1)) + " - converted to result (object-" + objectCount + ")";
            setObjectProperty(resultProperty, resultValue);
        }

        objectCount = 2;

        System.out.println("---set first Object...");
        /**
         * Creating new empty object result...
         */
        SSWAPObject sswapObject = null;
        sswapObject = assignObject(subject);
        Iterator<SSWAPProperty> iterator = object.getProperties().iterator();
        while (iterator.hasNext()) {
            SSWAPProperty property = iterator.next();
            SSWAPPredicate predicate = rigGraph.getPredicate(property.getURI());
            sswapObject.addProperty(predicate, "");
        }
        object = sswapObject;
        subject.addObject(object);

        System.out.println("---added new Object...");


        for (int i = 0; i < 3; i++) {
            String resultProperty = "resultProperty_" + (i + 1);
            String resultValue = subjectHashMap.get("requestProperty_" + (i + 1)) + " - converted to result (object-" + objectCount + ")";
            setObjectProperty(resultProperty, resultValue);
        }

        System.out.println("---set second Object...");
    }


    /**
     * Returns the string value for a property instance on an individual.
     * If more than one property instance exists, only one is (arbitrarily) chosen.
     *
     * @param sswapIndividual the individual with the property
     * @param sswapPredicate  the URI identifying the property (predicate)
     * @return the value as a string; null on any failure
     */
    private String getStrValue(SSWAPIndividual sswapIndividual, SSWAPPredicate sswapPredicate) {

        String value = null;
        SSWAPProperty sswapProperty = sswapIndividual.getProperty(sswapPredicate);

        if (sswapProperty != null) {
            value = sswapProperty.getValue().asString();

            if (value.isEmpty()) {
                value = null;
            }
        }

        return value;
    }


    /**
     * Gets the name of the property
     *
     * @param uri uri of the property
     * @return name of the property
     */
    private String getStrName(URI uri) {
        String[] parts = uri.toString().split("#");
        return parts[1];
    }


}

