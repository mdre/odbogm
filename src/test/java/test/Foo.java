package test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.annotations.Entity;
import net.odbogm.annotations.Ignore;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
@Entity(name = "FooNode")
public class Foo implements InterfaceTest {
    @Ignore
    private final static Logger LOGGER = Logger.getLogger(Foo.class .getName());
    static {
        LOGGER.setLevel(Level.INFO);
    }
    private String text;
    private List<SimpleVertex> lsve = new ArrayList<>();
    
    public Foo() {
    }

    public Foo(String text) {
        this.text = text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public List<SimpleVertex> getLsve() {
        return lsve;
    }

    public void add(SimpleVertex sv) {
        lsve.add(sv);
    }

    @Override
    public void foo() {
    }

}
