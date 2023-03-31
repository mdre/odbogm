package test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.odbogm.annotations.Entity;
import net.odbogm.annotations.Ignore;
import net.odbogm.annotations.OnlyAdd;

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
    
    @OnlyAdd(attribute = "lsve")
    private List<SimpleVertex> lsveOnlyAdd = new ArrayList<>();
    @OnlyAdd
    private List<SimpleVertex> onlyAdd = new ArrayList<>();
    
    
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

    public List<SimpleVertex> getLsveOnlyAdd() {
        return lsveOnlyAdd;
    }

    public List<SimpleVertex> getOnlyAdd() {
        return onlyAdd;
    }

}
