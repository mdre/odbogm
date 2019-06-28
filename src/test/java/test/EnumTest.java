package test;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public enum EnumTest {
    UNO,
    DOS,
    TRES,
    OTRO_MAS;


    @Override
    public String toString() {
        return "Nombre del EnumTest: " + name();
    }
    
}
