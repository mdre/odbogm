package net.odbogm;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

/**
 *
 * @author jbertinetti
 */
public class DbManagerTest {
    
    private final DbManager dbm = new DbManager();
    
    
    @Test
    @Ignore
    public void generateDBSQL() throws Exception {
        dbm.generateDBSQL("test.sql", new String[]{"test"});
    }
    
    /*
     * Tests that DBManager ignores the @RID field.
     */
    @Test
    public void dbManagerIgnoreRid() throws Exception {
        List<String> l = dbm.generateDBSQL("test");
        l.forEach(s -> assertFalse(s.contains("rid STRING")));
    }
    
    /*
     * Tests that edge classes are defined correctly.
     */
    @Test
    public void edgeClass() throws Exception {
        List<String> l = dbm.generateDBSQL("test");
        assertTrue(l.stream().anyMatch(
                s -> s.contains("create class EdgeAttrib extends E")));
        assertTrue(l.stream().anyMatch(s -> s.contains(
                "create class SimpleVertexEx_ohmSVE extends EdgeAttrib")));
        //las demás deben seguir como antes
        assertTrue(l.stream().anyMatch(s -> s.contains(
                "create class SimpleVertex extends V")));
    }
    
    /*
     * Tests that SIDs are taken into account.
     */
    @Test
    public void sids() throws Exception {
        List<String> l = dbm.generateDBSQL("test");
        assertTrue(l.stream().anyMatch(
                s -> s.contains("create class UserSID extends V")));
        assertTrue(l.stream().anyMatch(
                s -> s.contains("create class GroupSID extends V")));
        assertTrue(l.stream().anyMatch(
                s -> s.contains("create class SObject extends V")));
    }
    
    /*
     * Tests that it respects the custom names of entity classes.
     */
    @Test
    public void entityName() throws Exception {
        List<String> l = dbm.generateDBSQL("test");
        assertTrue(l.stream().anyMatch(
                s -> s.contains("create class FooNode extends V")));
        assertTrue(l.stream().anyMatch(
                s -> s.contains("create class FooNode_lsve extends E")));
        assertTrue(l.stream().noneMatch(
                s -> s.contains("create class Foo extends V")));
    }
    
    /*
     * Tests that inherited relationships are taken into account.
     */
    @Test
    public void inheritedRelations() throws Exception {
        List<String> l = dbm.generateDBSQL("test");
        assertTrue(l.stream().anyMatch(
                s -> s.contains("create class SSimpleVertex___owner extends SObject___owner")));
        assertTrue(l.stream().anyMatch(
                s -> s.contains("create class SVExChild_looptest extends SimpleVertexEx_looptest")));
        assertTrue(l.stream().anyMatch(
                s -> s.contains("create class SVExChild_lSV extends SimpleVertexEx_lSV")));
        assertTrue(l.stream().anyMatch(
                s -> s.contains("create class SVExChild_hmSV extends SimpleVertexEx_hmSV")));
    }
    
    /*
     * Tests that inherited maps of edge classes are taken into account.
     */
    @Test
    public void inheritedMapEdge() throws Exception {
        List<String> l = dbm.generateDBSQL("test");
        assertTrue(l.stream().anyMatch(
                s -> s.contains("create class SVExChild_ohmSVE extends EdgeAttrib")));
    }
    
    /*
     * Tests that DBManager ignores the @Version field.
     */
    @Test
    public void dbManagerIgnoreVersionField() throws Exception {
        List<String> l = dbm.generateDBSQL("test");
        l.forEach(s -> assertFalse(s.contains("version INTEGER")));
    }
    
}
