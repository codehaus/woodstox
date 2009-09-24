package org.codehaus.staxbind.jsoncount;

import java.util.*;

/**
 * Helper class used to verify that specific sub-class of
 * {@link JsonCountDriver} produces correct results.
 * For now we will use "Json.org" driver as the baseline,
 * assuming that it handles things correctly. Should be safe,
 * since any deviation needs to be investigated anyway.
 */
public class ResultVerifier
{
    public static void checkResults(Class<?> caller, byte[] data, CountResult actResults)
    {
        JsonOrgDriver std = new JsonOrgDriver();
        CountResult expResults = new CountResult();

        try {
            std.read(data, expResults);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        TreeMap<String,int[]> exp = expResults.getCounts();
        TreeMap<String,int[]> act = actResults.getCounts();

        // First: did we see the same fields at least?
        TreeSet<String> expNames = new TreeSet<String>(exp.keySet());
        TreeSet<String> actNames = new TreeSet<String>(act.keySet());

        if (!expNames.equals(actNames)) {
            throw new IllegalStateException("Implementation "+caller.getName()+" broken: different field names, expected: "+expNames+"; got "+actNames);
        }

        // If so, let's verify counts:
        for (Map.Entry<String,int[]> en : exp.entrySet()) {
            String name = en.getKey();
            int expV = en.getValue()[0];
            int actV = exp.get(name)[0];

            if (expV != actV) {
                throw new IllegalStateException("Implementation "+caller.getName()+" broken: field '"+name+"'; expected count "+expV+", actual "+actV);
            }
        }
        // fine, are equal 
    }
}
